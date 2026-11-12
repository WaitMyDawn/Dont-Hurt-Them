package yagen.waitmydawn.dont_hurt_them.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;

/** Enforcement-only mirror for owned projectiles whose player has disconnected. Player attachments are authoritative. */
public final class OfflineFriendships extends SavedData {
    private final Map<UUID, FriendshipList> lists = new HashMap<>();

    public static OfflineFriendships get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(OfflineFriendships::new, OfflineFriendships::load, null), "dont_hurt_them_offline");
    }

    private static OfflineFriendships load(CompoundTag root, HolderLookup.Provider registries) {
        OfflineFriendships result = new OfflineFriendships();
        for (String key : root.getAllKeys()) {
            try { result.lists.put(UUID.fromString(key), FriendshipSerialization.read(root.getCompound(key))); }
            catch (IllegalArgumentException ignored) { /* Ignore malformed owner IDs without discarding valid players. */ }
        }
        return result;
    }

    public FriendshipList find(UUID owner) { return lists.get(owner); }
    public boolean tracks(UUID entity) { return lists.values().stream().anyMatch(list -> list.contains("entity/" + entity)); }

    public void update(UUID owner, FriendshipList list) {
        lists.put(owner, FriendshipSerialization.read(FriendshipSerialization.write(list)));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        lists.forEach((id, list) -> root.put(id.toString(), FriendshipSerialization.write(list)));
        return root;
    }
}
