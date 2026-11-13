package yagen.waitmydawn.dont_hurt_them.server;

import yagen.waitmydawn.dont_hurt_them.data.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
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
import java.util.ArrayDeque;
import java.util.UUID;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipArchive;
import yagen.waitmydawn.dont_hurt_them.data.OfflineFriendships;

public final class FriendshipLifecycle {
    private enum LegacyKind { LIVING, NON_LIVING, UNKNOWN }
    private static final Map<ServerLevel, Map<net.minecraft.world.entity.EntityType<?>, LegacyKind>> LEGACY_KINDS = new WeakHashMap<>();
    private static final ThreadLocal<ArrayDeque<UUID>> DEATH_PUBLICATIONS = new ThreadLocal<>();

    public static FriendshipArchive.DeathRecord capture(Entity entity, DamageSource source) {
        if (!(entity instanceof LivingEntity)) return null;
        if (!(entity.level() instanceof ServerLevel level) || !OfflineFriendships.get(level.getServer()).tracks(entity.getUUID())) return null;
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        CompoundTag cause = new CompoundTag();
        cause.putBoolean("living_entity", true);
        CompoundTag snapshot = new CompoundTag();
        try { snapshot = NbtCompat.save(entity); }
        catch (RuntimeException error) {
            // A broken serializer must not interrupt death or advertise an incomplete snapshot as revivable.
            cause.putBoolean("snapshot_failed", true);
            yagen.waitmydawn.dont_hurt_them.DontHurtThem.LOGGER.warn("Cannot archive entity {} {}: {}", type, entity.getUUID(), error.getClass().getName());
        }
        snapshot.remove("Passengers");
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

    public static void removed(Entity entity, Entity.RemovalReason reason) {
        if (entity instanceof LivingEntity || reason != Entity.RemovalReason.KILLED || entity.getRemovalReason() != Entity.RemovalReason.KILLED
                || !(entity.level() instanceof ServerLevel level)) return;
        UUID uuid = entity.getUUID();
        var server = level.getServer();
        if (!OfflineFriendships.get(server).tracks(uuid)) return;
        var owners = server.getPlayerList().getPlayers().stream().filter(owner -> FriendshipService.list(owner).contains("entity/" + uuid)).toList();
        OfflineFriendships.get(server).forgetEntity(uuid);
        FriendshipArchive.get(server).forget(uuid);
        for (ServerPlayer owner : owners) {
            FriendshipService.list(owner).remove("entity/" + uuid);
            FriendshipService.changed(owner);
        }
    }

    public static boolean isKnownNonLiving(FriendshipArchive.DeathRecord record, ServerLevel level) {
        if (record.cause().getBooleanOr("living_entity", false)) return false;
        var id = Identifier.tryParse(record.type());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return false;
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == EntityType.PLAYER || net.minecraft.world.entity.ai.attributes.DefaultAttributes.hasSupplier(type)) return false;
        // An absent attribute declaration is not proof of a nonliving type; inspect a detached factory result for legacy records only.
        var cache = LEGACY_KINDS.computeIfAbsent(level, ignored -> new HashMap<>());
        LegacyKind kind = cache.get(type);
        if (kind == null) {
            try {
                Entity probe = type.create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
                kind = probe == null ? LegacyKind.UNKNOWN : probe instanceof LivingEntity ? LegacyKind.LIVING : LegacyKind.NON_LIVING;
            } catch (RuntimeException error) {
                kind = LegacyKind.UNKNOWN;
                yagen.waitmydawn.dont_hurt_them.DontHurtThem.LOGGER.warn("Cannot classify legacy friendship type {}: {}", record.type(), error.getClass().getName());
            }
            cache.put(type, kind);
        }
        return kind == LegacyKind.NON_LIVING;
    }

    public static void commit(Entity entity, FriendshipArchive.DeathRecord record) {
        if (record == null || !(entity instanceof LivingEntity) || !(entity.level() instanceof ServerLevel level)) return;
        var publications = DEATH_PUBLICATIONS.get();
        if (publications == null) { publications = new ArrayDeque<>(); DEATH_PUBLICATIONS.set(publications); }
        publications.push(record.uuid());
        try {
            FriendshipArchive.get(level.getServer()).record(record);
            syncOwners(level.getServer(), entity.getUUID());
        } finally {
            publications.pop();
            if (publications.isEmpty()) DEATH_PUBLICATIONS.remove();
        }
    }

    public static void syncOwners(MinecraftServer server, UUID uuid) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (FriendshipService.list(player).contains("entity/" + uuid)) FriendshipService.sync(player, false);
        }
    }

    public static FriendshipArchive.DeathRecord reconcileDeath(MinecraftServer server, UUID uuid) {
        var archive = FriendshipArchive.get(server);
        var record = archive.find(uuid);
        if (record == null || !record.dead()) return record;
        // Keep synchronous notifications inside an active death publication from interpreting the same transition as revival.
        var publications = DEATH_PUBLICATIONS.get();
        if (publications != null && publications.contains(uuid)) return record;
        // Only a loaded, living entity with the same UUID overrides a confirmed death; never load chunks or infer identity from appearance.
        for (var level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null && !entity.isRemoved() && entity.isAlive()) {
                archive.revived(uuid, server.overworld().getGameTime());
                return archive.find(uuid);
            }
        }
        return record;
    }

    private static boolean hasLivingEvidence(FriendshipArchive.DeathRecord record, ServerLevel level) {
        if (record.cause().getBooleanOr("living_entity", false)) return true;
        var id = Identifier.tryParse(record.type());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return false;
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (net.minecraft.world.entity.ai.attributes.DefaultAttributes.hasSupplier(type)) return true;
        var cache = LEGACY_KINDS.get(level);
        return cache != null && cache.get(type) == LegacyKind.LIVING;
    }

    public static boolean canRevive(Player player, String type, UUID uuid) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        var entry = FriendshipService.list(player).get("entity/" + uuid);
        var id = Identifier.tryParse(type);
        if (entry == null || !type.equals(entry.typeId()) || id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id) || BuiltInRegistries.ENTITY_TYPE.getValue(id) == EntityType.PLAYER) return false;
        var record = reconcileDeath(level.getServer(), uuid);
        return record != null && record.dead() && hasLivingEvidence(record, level) && !record.cause().getBooleanOr("snapshot_failed", false)
                && record.type().equals(type);
    }

    public static Entity revive(Player player, String type, UUID uuid) {
        return revive(player, type, uuid, null, null);
    }

    public static Entity revive(Player player, String type, UUID uuid, ServerLevel destination, Vec3 position) {
        var requested = Identifier.tryParse(type);
        if (requested != null && BuiltInRegistries.ENTITY_TYPE.getValue(requested) == EntityType.PLAYER) throw new IllegalArgumentException("player_revive_forbidden");
        if ((destination == null) != (position == null)) throw new IllegalArgumentException("invalid_position");
        if (!canRevive(player, type, uuid)) throw new IllegalArgumentException("no_death_record");
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        var archive = FriendshipArchive.get(server);
        var record = archive.find(uuid);
        Identifier dimension = Identifier.tryParse(record.dimension());
        ServerLevel level = destination != null ? destination : dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) throw new IllegalArgumentException("grave_unloaded");
        if (level.getServer() != server) throw new IllegalArgumentException("invalid_position");
        CompoundTag tag = record.entity().copy();
        // Historical snapshots may contain passengers; discard them before any validation or entity loading.
        tag.remove("Passengers");
        Vec3 original = snapshotPosition(tag);
        Vec3 offset = position == null ? Vec3.ZERO : position.subtract(original);
        var expectedIds = new HashSet<UUID>();
        // Validate only the target identity; an entity factory must not add passengers during loading.
        prepareSnapshot(tag, level, offset, expectedIds);
        Entity restored;
        try { restored = NbtCompat.load(tag, level); }
        catch (RuntimeException error) { throw new IllegalArgumentException("invalid_snapshot", error); }
        if (restored == null || restored instanceof Player || !restored.getUUID().equals(uuid)
                || !BuiltInRegistries.ENTITY_TYPE.getKey(restored.getType()).toString().equals(type)) throw new IllegalArgumentException("invalid_snapshot");
        if (!(restored instanceof LivingEntity)) throw new IllegalArgumentException("non_living_revive_forbidden");
        restored.stopRiding();
        var tree = restored.getSelfAndPassengers().toList();
        if (tree.size() != expectedIds.size() || !tree.stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet()).equals(expectedIds))
            throw new IllegalArgumentException("invalid_snapshot");
        for (Entity member : tree) {
            if (member instanceof Player) throw new IllegalArgumentException("player_revive_forbidden");
            if (!(member instanceof LivingEntity)) throw new IllegalArgumentException("non_living_revive_forbidden");
            requireLoaded(level, member.position());
            for (ServerLevel world : server.getAllLevels()) {
                if (world.getEntity(member.getUUID()) != null) throw new IllegalArgumentException("uuid_conflict");
            }
            if (member instanceof LivingEntity living) {
                living.setHealth(living.getMaxHealth()); living.deathTime = 0; living.hurtTime = 0;
            }
            member.setPose(Pose.STANDING); member.fallDistance = 0; member.clearFire();
        }
        boolean added;
        try { added = level.addFreshEntity(restored); }
        catch (RuntimeException error) { rollback(level, tree); throw new IllegalArgumentException("spawn_blocked", error); }
        // UUID collisions are checked before insertion; cancellation can also return false.
        if (!added) { rollback(level, tree); throw new IllegalArgumentException("spawn_blocked"); }
        if (!restored.getPassengers().isEmpty() || tree.stream().anyMatch(entity -> level.getEntity(entity.getUUID()) != entity)) {
            rollback(level, tree);
            throw new IllegalArgumentException("spawn_blocked");
        }
        archive.revived(uuid, server.overworld().getGameTime());
        syncOwners(server, uuid);
        return restored;
    }

    private static Vec3 snapshotPosition(CompoundTag tag) {
        var pos = NbtCompat.list(tag, "Pos", Tag.TAG_DOUBLE);
        if (pos.size() != 3) throw new IllegalArgumentException("invalid_snapshot");
        Vec3 result = new Vec3(pos.getDoubleOr(0, 0), pos.getDoubleOr(1, 0), pos.getDoubleOr(2, 0));
        if (!Double.isFinite(result.x) || !Double.isFinite(result.y) || !Double.isFinite(result.z)) throw new IllegalArgumentException("invalid_snapshot");
        return result;
    }

    private static void prepareSnapshot(CompoundTag tag, ServerLevel level, Vec3 offset, java.util.Set<UUID> ids) {
        tag.remove("Passengers");
        var id = Identifier.tryParse(tag.getStringOr("id", ""));
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) throw new IllegalArgumentException("invalid_snapshot");
        if (BuiltInRegistries.ENTITY_TYPE.getValue(id) == EntityType.PLAYER) throw new IllegalArgumentException("player_revive_forbidden");
        if (!NbtCompat.hasUuid(tag, "UUID") || !ids.add(NbtCompat.uuid(tag, "UUID"))) throw new IllegalArgumentException("invalid_snapshot");
        Vec3 target = snapshotPosition(tag).add(offset);
        requireLoaded(level, target);
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(target.x)); pos.add(DoubleTag.valueOf(target.y)); pos.add(DoubleTag.valueOf(target.z));
        tag.put("Pos", pos);
    }

    private static void requireLoaded(ServerLevel level, Vec3 position) {
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || !Level.isInSpawnableBounds(BlockPos.containing(position))) throw new IllegalArgumentException("invalid_position");
        BlockPos block = BlockPos.containing(position);
        if (level.getChunkSource().getChunkNow(block.getX() >> 4, block.getZ() >> 4) == null) throw new IllegalArgumentException("grave_unloaded");
    }

    private static void rollback(ServerLevel level, java.util.List<Entity> tree) {
        // Spawn hooks can fail after inserting the target; never discard an unrelated UUID collision.
        for (Entity entity : tree) if (level.getEntity(entity.getUUID()) == entity) entity.discard();
    }
    private FriendshipLifecycle() {}
}
