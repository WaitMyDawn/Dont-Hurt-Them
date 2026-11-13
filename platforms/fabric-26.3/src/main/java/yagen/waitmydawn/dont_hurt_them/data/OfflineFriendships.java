package yagen.waitmydawn.dont_hurt_them.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;

/** Enforcement-only mirror for owned projectiles whose player has disconnected. Player attachments are authoritative. */
public final class OfflineFriendships extends SavedData {
    private final Map<UUID, FriendshipList> lists = new HashMap<>();
    private final Map<UUID, Set<UUID>> ownersByEntity = new HashMap<>();
    private final Map<UUID, Set<UUID>> pendingRemovals = new HashMap<>();

    public static OfflineFriendships get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new net.minecraft.world.level.saveddata.SavedDataType<>(net.minecraft.resources.Identifier.withDefaultNamespace("dont_hurt_them_offline"), OfflineFriendships::new, CompoundTag.CODEC.xmap(tag -> OfflineFriendships.load(tag, null), data -> data.save(new CompoundTag(), null)), null));
    }

    private static OfflineFriendships load(CompoundTag root, HolderLookup.Provider registries) {
        OfflineFriendships result = new OfflineFriendships();
        for (String key : root.keySet()) {
            try { result.lists.put(UUID.fromString(key), FriendshipSerialization.read(root.getCompoundOrEmpty(key))); }
            catch (IllegalArgumentException ignored) { /* Ignore malformed owner IDs without discarding valid players. */ }
        }
        CompoundTag pending = root.getCompoundOrEmpty("_pending_removals");
        for (String owner : pending.keySet()) {
            try {
                Set<UUID> ids = new HashSet<>();
                for (String entity : pending.getCompoundOrEmpty(owner).keySet()) {
                    try { ids.add(UUID.fromString(entity)); } catch (IllegalArgumentException ignored) { }
                }
                if (!ids.isEmpty()) result.pendingRemovals.put(UUID.fromString(owner), ids);
            } catch (IllegalArgumentException ignored) { }
        }
        result.lists.forEach(result::index);
        return result;
    }

    public FriendshipList find(UUID owner) { return lists.get(owner); }
    public boolean tracks(UUID entity) { return ownersByEntity.containsKey(entity); }

    private void index(UUID owner, FriendshipList list) {
        for (var entry : list.entries()) if (entry.individual()) ownersByEntity.computeIfAbsent(entry.entityId(), ignored -> new HashSet<>()).add(owner);
    }

    public void forgetEntity(UUID entity) {
        Set<UUID> owners = ownersByEntity.remove(entity);
        if (owners == null) return;
        for (UUID owner : owners) {
            lists.get(owner).remove("entity/" + entity);
            pendingRemovals.computeIfAbsent(owner, ignored -> new HashSet<>()).add(entity);
        }
        setDirty();
    }

    public void applyRemovals(UUID owner, FriendshipList list) {
        Set<UUID> ids = pendingRemovals.remove(owner);
        if (ids == null) return;
        for (UUID id : ids) list.remove("entity/" + id);
        setDirty();
    }

    public void update(UUID owner, FriendshipList list) {
        FriendshipList previous = lists.get(owner);
        if (previous != null) for (var entry : previous.entries()) if (entry.individual()) {
            Set<UUID> owners = ownersByEntity.get(entry.entityId());
            if (owners != null) { owners.remove(owner); if (owners.isEmpty()) ownersByEntity.remove(entry.entityId()); }
        }
        FriendshipList copy = FriendshipSerialization.read(FriendshipSerialization.write(list));
        lists.put(owner, copy);
        index(owner, copy);
        setDirty();
    }

    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        lists.forEach((id, list) -> root.put(id.toString(), FriendshipSerialization.write(list)));
        CompoundTag pending = new CompoundTag();
        pendingRemovals.forEach((owner, ids) -> {
            CompoundTag removals = new CompoundTag();
            ids.forEach(id -> removals.putBoolean(id.toString(), true));
            pending.put(owner.toString(), removals);
        });
        root.put("_pending_removals", pending);
        return root;
    }
}
