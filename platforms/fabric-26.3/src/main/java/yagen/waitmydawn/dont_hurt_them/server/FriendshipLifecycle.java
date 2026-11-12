package yagen.waitmydawn.dont_hurt_them.server;

import yagen.waitmydawn.dont_hurt_them.data.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipArchive;
import yagen.waitmydawn.dont_hurt_them.data.OfflineFriendships;

public final class FriendshipLifecycle {
    private static final Map<Entity, FriendshipArchive.DeathRecord> PENDING = new WeakHashMap<>();

    public static FriendshipArchive.DeathRecord capture(Entity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || !OfflineFriendships.get(level.getServer()).tracks(entity.getUUID())) return null;
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        CompoundTag cause = new CompoundTag();
        CompoundTag snapshot = new CompoundTag();
        try { snapshot = NbtCompat.save(entity); }
        catch (RuntimeException error) {
            // A broken serializer must not interrupt death or advertise an incomplete snapshot as revivable.
            cause.putBoolean("snapshot_failed", true);
            yagen.waitmydawn.dont_hurt_them.DontHurtThem.LOGGER.warn("Cannot archive entity {} {}: {}", type, entity.getUUID(), error.getClass().getName());
        }
        snapshot.putString("id", type); NbtCompat.putUuid(snapshot, "UUID", entity.getUUID());
        if (source != null) {
            cause.putString("damage", source.getMsgId());
            cause.putString("damage_type", source.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse(""));
            if (source.getEntity() != null) NbtCompat.putUuid(cause, "attacker", source.getEntity().getUUID());
            if (source.getDirectEntity() != null) NbtCompat.putUuid(cause, "direct_entity", source.getDirectEntity().getUUID());
            UUID player = DamageProtection.owner(source.getEntity());
            if (player != null) NbtCompat.putUuid(cause, "player", player);
            if (entity instanceof LivingEntity living) cause.putString("message", living.getCombatTracker().getDeathMessage().getString());
        }
        return new FriendshipArchive.DeathRecord(entity.getUUID(), type, level.dimension().identifier().toString(), entity.getName().getString(),
                level.getServer().overworld().getGameTime(), -1, snapshot, cause);
    }

    public static void prepareNonLiving(Entity entity, DamageSource source) {
        if (entity instanceof LivingEntity || entity.level().isClientSide()) return;
        var record = capture(entity, source);
        if (record != null) PENDING.put(entity, record);
    }

    public static void removed(Entity entity, Entity.RemovalReason reason) {
        if (entity instanceof LivingEntity || reason != Entity.RemovalReason.KILLED || entity.level().isClientSide()) return;
        var record = PENDING.remove(entity);
        commit(entity, record == null ? capture(entity, null) : record);
    }

    public static void commit(Entity entity, FriendshipArchive.DeathRecord record) {
        if (record == null || !(entity.level() instanceof ServerLevel level)) return;
        FriendshipArchive.get(level.getServer()).record(record);
        syncOwners(level.getServer(), entity.getUUID());
    }

    public static void syncOwners(MinecraftServer server, UUID uuid) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (FriendshipService.list(player).contains("entity/" + uuid)) FriendshipService.sync(player, false);
        }
    }

    public static boolean canRevive(Player player, String type, UUID uuid) {
        if (!(player.level() instanceof ServerLevel level) || FriendshipService.list(player).entries().stream()
                .noneMatch(entry -> uuid.equals(entry.entityId()) && type.equals(entry.typeId()))) return false;
        var record = FriendshipArchive.get(level.getServer()).find(uuid);
        return record != null && record.dead() && !record.cause().getBooleanOr("snapshot_failed", false)
                && record.type().equals(type) && !record.type().equals("minecraft:player");
    }

    public static Entity revive(Player player, String type, UUID uuid) {
        if (!canRevive(player, type, uuid)) throw new IllegalArgumentException("no_death_record");
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        var archive = FriendshipArchive.get(server);
        var record = archive.find(uuid);
        Identifier dimension = Identifier.tryParse(record.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) throw new IllegalArgumentException("missing_dimension");
        CompoundTag tag = record.entity().copy();
        var pos = NbtCompat.list(tag, "Pos", Tag.TAG_DOUBLE);
        if (pos.size() != 3) throw new IllegalArgumentException("invalid_snapshot");
        level.getChunk(BlockPos.containing(pos.getDoubleOr(0, 0), pos.getDoubleOr(1, 0), pos.getDoubleOr(2, 0)));
        Entity restored;
        try { restored = NbtCompat.load(tag, level); }
        catch (RuntimeException error) { throw new IllegalArgumentException("invalid_snapshot", error); }
        if (restored == null || restored instanceof Player || !restored.getUUID().equals(uuid)
                || !BuiltInRegistries.ENTITY_TYPE.getKey(restored.getType()).toString().equals(type)) throw new IllegalArgumentException("invalid_snapshot");
        var tree = restored.getSelfAndPassengers().toList();
        if (tree.stream().map(Entity::getUUID).distinct().count() != tree.size()) throw new IllegalArgumentException("invalid_snapshot");
        for (Entity member : tree) {
            for (ServerLevel world : server.getAllLevels()) {
                Entity existing = world.getEntity(member.getUUID());
                if (existing != null) throw new IllegalArgumentException("uuid_conflict");
            }
            if (member instanceof LivingEntity living) {
                living.setHealth(living.getMaxHealth()); living.deathTime = 0; living.hurtTime = 0;
            }
            member.setPose(Pose.STANDING); member.fallDistance = 0; member.clearFire();
        }
        if (!level.tryAddFreshEntityWithPassengers(restored)) throw new IllegalArgumentException("uuid_conflict");
        if (tree.stream().anyMatch(entity -> level.getEntity(entity.getUUID()) != entity)) {
            tree.forEach(Entity::discard);
            throw new IllegalArgumentException("spawn_blocked");
        }
        archive.revived(uuid, server.overworld().getGameTime());
        syncOwners(server, uuid);
        return restored;
    }
    private FriendshipLifecycle() {}
}
