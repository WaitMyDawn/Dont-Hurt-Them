package yagen.waitmydawn.dont_hurt_them.server;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipEntry;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipSerialization;
import yagen.waitmydawn.dont_hurt_them.data.OfflineFriendships;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;

public final class FriendshipService {
    public static FriendshipList list(Player player) {
        FriendshipList list = ((AttachmentTarget) player).getAttachedOrCreate(ModRegistries.FRIENDSHIPS);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) OfflineFriendships.get(level.getServer()).applyRemovals(player.getUUID(), list);
        return list;
    }
    public static boolean holding(Player player) {
        return player.getMainHandItem().is(ModRegistries.PROOF_OF_FRIENDSHIP.get())
                || player.getOffhandItem().is(ModRegistries.PROOF_OF_FRIENDSHIP.get());
    }

    public static void toggle(ServerPlayer player, Entity target) {
        FriendshipList list = list(player);
        FriendshipEntry entry = new FriendshipEntry(target.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString(),
                target.getName().getString(), false, player.level().getServer().overworld().getGameTime());
        boolean removed = list.remove(entry.key());
        if (!removed && !list.put(entry)) { message(player, "full"); return; }
        player.sendSystemMessage(Component.translatable("message.dont_hurt_them." + (removed ? "removed" : "added"), target.getName()), true);
        changed(player);
    }

    public static void action(ServerPlayer player, FriendshipNetwork.Action packet) {
        if (!holding(player)) return;
        FriendshipList list = list(player);
        boolean changed = false;
        switch (packet.action()) {
            case "add_type" -> {
                Identifier id = Identifier.tryParse(packet.key());
                if (id == null || !packet.key().contains(":") || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                    message(player, "invalid_type"); return;
                }
                var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
                if (type == net.minecraft.world.entity.EntityTypes.PLAYER) { message(player, "player_type_forbidden"); return; }
                if (list.contains("type/" + id)) { message(player, "exists"); return; }
                changed = list.put(new FriendshipEntry(null, id.toString(), type.getDescription().getString(), false, player.level().getServer().overworld().getGameTime()));
                if (!changed) message(player, "full");
            }
            case "clear_deceased" -> {
                int removed = clearDeceased(player);
                player.sendSystemMessage(Component.translatable("message.dont_hurt_them.cleared_deceased", removed), false);
                return;
            }
            case "remove" -> changed = list.remove(packet.key());
            case "favorite" -> changed = list.toggleFavorite(packet.key());
            default -> { return; }
        }
        if (changed) changed(player);
    }

    public static int clearDeceased(ServerPlayer player) {
        if (!holding(player)) return 0;
        var list = list(player);
        int removed = 0;
        for (FriendshipEntry entry : list.entries()) {
            if (!entry.individual() || entry.favorite()) continue;
            var record = FriendshipLifecycle.reconcileDeath(player.level().getServer(), entry.entityId());
            if (record != null && record.dead() && list.remove(entry.key())) removed++;
        }
        if (removed > 0) changed(player); else sync(player, false);
        return removed;
    }

    public static void changed(ServerPlayer player) {
        var current = list(player);
        var archive = yagen.waitmydawn.dont_hurt_them.data.FriendshipArchive.get(player.level().getServer());
        for (FriendshipEntry entry : current.entries()) if (entry.individual()) {
            var record = archive.find(entry.entityId());
            if (record != null && record.dead() && FriendshipLifecycle.isKnownNonLiving(record, (net.minecraft.server.level.ServerLevel) player.level())) {
                OfflineFriendships.get(player.level().getServer()).forgetEntity(entry.entityId());
                archive.forget(entry.entityId());
                current.remove(entry.key());
            }
        }
        OfflineFriendships.get(player.level().getServer()).update(player.getUUID(), list(player));
        sync(player, false);
    }

    public static void sync(ServerPlayer player, boolean open) {
        CompoundTag root = FriendshipSerialization.write(list(player));
        root.putLong("clock", player.level().getServer().overworld().getGameTime());
        CompoundTag live = new CompoundTag();
        for (FriendshipEntry entry : list(player).entries()) {
            if (!entry.individual()) continue;
            var death = FriendshipLifecycle.reconcileDeath(player.level().getServer(), entry.entityId());
            if (death != null && death.dead()) { live.put(entry.key(), death.metadata()); continue; }
            for (var level : player.level().getServer().getAllLevels()) {
                Entity entity = level.getEntity(entry.entityId());
                if (entity == null || entity.isRemoved()) continue;
                CompoundTag info = new CompoundTag();
                info.putBoolean("loaded", true);
                info.putBoolean("living", entity instanceof LivingEntity);
                info.putBoolean("renameable", !(entity instanceof Player) && entity.isAlive());
                info.putString("name", entity.getName().getString());
                info.putString("dimension", level.dimension().identifier().toString());
                info.putDouble("x", entity.getX()); info.putDouble("y", entity.getY()); info.putDouble("z", entity.getZ());
                if (entity instanceof LivingEntity living) {
                    info.putFloat("health", living.getHealth()); info.putFloat("max_health", living.getMaxHealth());
                }
                live.put(entry.key(), info);
                break;
            }
        }
        root.put("live", live);
        ServerPlayNetworking.send(player, new FriendshipNetwork.Snapshot(root, open));
    }

    public static Entity findLoaded(ServerPlayer player, java.util.UUID uuid) {
        for (var level : player.level().getServer().getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null && !entity.isRemoved()) return entity;
        }
        return null;
    }

    public static boolean rename(ServerPlayer player, java.util.UUID uuid, String name) {
        if (!holding(player) || !list(player).contains("entity/" + uuid) || name.isBlank() || name.length() > 64
                || name.codePoints().anyMatch(Character::isISOControl)) return false;
        Entity entity = findLoaded(player, uuid);
        if (entity == null || entity instanceof Player || !entity.isAlive()) return false;
        entity.setCustomName(Component.literal(name));
        if (entity instanceof net.minecraft.world.entity.Mob mob) mob.setPersistenceRequired();
        FriendshipEntry old = list(player).entries().stream().filter(entry -> entry.entityId() != null && entry.entityId().equals(uuid)).findFirst().orElseThrow();
        list(player).put(old.withName(name));
        changed(player); FriendshipLifecycle.syncOwners(player.level().getServer(), uuid);
        return true;
    }

    private static void message(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("message.dont_hurt_them." + key), false);
    }
    private FriendshipService() {}
}
