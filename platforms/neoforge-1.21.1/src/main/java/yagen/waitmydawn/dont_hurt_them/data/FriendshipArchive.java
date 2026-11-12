package yagen.waitmydawn.dont_hurt_them.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-only death snapshots. Clients receive status metadata, never the complete entity archive. */
public final class FriendshipArchive extends SavedData {
    public record DeathRecord(UUID uuid, String type, String dimension, String name, long diedAt, long revivedAt,
                              CompoundTag entity, CompoundTag cause) {
        public boolean dead() { return revivedAt < 0; }
        public CompoundTag metadata() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("dead", dead()); tag.putString("name", name); tag.putString("dimension", dimension);
            tag.putLong("death_tick", diedAt);
            var pos = entity.getList("Pos", Tag.TAG_DOUBLE);
            if (pos.size() == 3) { tag.putDouble("x", pos.getDouble(0)); tag.putDouble("y", pos.getDouble(1)); tag.putDouble("z", pos.getDouble(2)); }
            return tag;
        }
    }
    private final Map<UUID, DeathRecord> records = new HashMap<>();
    public static FriendshipArchive get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(FriendshipArchive::new, FriendshipArchive::load), "dont_hurt_them_archive");
    }
    public DeathRecord find(UUID uuid) { return records.get(uuid); }
    public void record(DeathRecord record) { records.put(record.uuid(), record); setDirty(); }
    public void revived(UUID uuid, long tick) {
        DeathRecord old = records.get(uuid);
        if (old != null) record(new DeathRecord(uuid, old.type(), old.dimension(), old.name(), old.diedAt(), tick, old.entity(), old.cause()));
    }
    public static FriendshipArchive load(CompoundTag root, HolderLookup.Provider registries) {
        FriendshipArchive archive = new FriendshipArchive();
        for (Tag element : root.getList("records", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) element;
            if (!tag.hasUUID("uuid")) continue;
            archive.record(new DeathRecord(tag.getUUID("uuid"), tag.getString("type"), tag.getString("dimension"), tag.getString("name"),
                    tag.getLong("died_at"), tag.contains("revived_at") ? tag.getLong("revived_at") : -1,
                    tag.getCompound("entity"), tag.getCompound("cause")));
        }
        return archive;
    }
    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (DeathRecord record : records.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", record.uuid()); tag.putString("type", record.type()); tag.putString("dimension", record.dimension());
            tag.putString("name", record.name()); tag.putLong("died_at", record.diedAt()); tag.putLong("revived_at", record.revivedAt());
            tag.put("entity", record.entity()); tag.put("cause", record.cause()); list.add(tag);
        }
        root.put("records", list); return root;
    }
}
