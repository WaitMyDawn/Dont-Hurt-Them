package yagen.waitmydawn.dont_hurt_them.data;

import yagen.waitmydawn.dont_hurt_them.data.NbtCompat;
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
            if (cause.getBooleanOr("living_entity", false)) tag.putBoolean("living", true);
            var pos = NbtCompat.list(entity, "Pos", Tag.TAG_DOUBLE);
            if (pos.size() == 3) { tag.putDouble("x", pos.getDoubleOr(0, 0)); tag.putDouble("y", pos.getDoubleOr(1, 0)); tag.putDouble("z", pos.getDoubleOr(2, 0)); }
            return tag;
        }
    }
    private final Map<UUID, DeathRecord> records = new HashMap<>();
    public static FriendshipArchive get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedDataType<>("dont_hurt_them_archive", FriendshipArchive::new, CompoundTag.CODEC.xmap(tag -> FriendshipArchive.load(tag, null), data -> data.save(new CompoundTag(), null)), null));
    }
    public DeathRecord find(UUID uuid) { return records.get(uuid); }
    public void forget(UUID uuid) { if (records.remove(uuid) != null) setDirty(); }
    public void record(DeathRecord record) { records.put(record.uuid(), record); setDirty(); }
    public void revived(UUID uuid, long tick) {
        DeathRecord old = records.get(uuid);
        if (old != null) record(new DeathRecord(uuid, old.type(), old.dimension(), old.name(), old.diedAt(), tick, old.entity(), old.cause()));
    }
    public static FriendshipArchive load(CompoundTag root, HolderLookup.Provider registries) {
        FriendshipArchive archive = new FriendshipArchive();
        for (Tag element : NbtCompat.list(root, "records", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) element;
            if (!NbtCompat.hasUuid(tag, "uuid")) continue;
            archive.record(new DeathRecord(NbtCompat.uuid(tag, "uuid"), tag.getStringOr("type", ""), tag.getStringOr("dimension", ""), tag.getStringOr("name", ""),
                    tag.getLongOr("died_at", 0), tag.contains("revived_at") ? tag.getLongOr("revived_at", 0) : -1,
                    tag.getCompoundOrEmpty("entity"), tag.getCompoundOrEmpty("cause")));
        }
        return archive;
    }
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (DeathRecord record : records.values()) {
            CompoundTag tag = new CompoundTag();
            NbtCompat.putUuid(tag, "uuid", record.uuid()); tag.putString("type", record.type()); tag.putString("dimension", record.dimension());
            tag.putString("name", record.name()); tag.putLong("died_at", record.diedAt()); tag.putLong("revived_at", record.revivedAt());
            tag.put("entity", record.entity()); tag.put("cause", record.cause()); list.add(tag);
        }
        root.put("records", list); return root;
    }
}
