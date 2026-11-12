package yagen.waitmydawn.dont_hurt_them.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipEntry;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;

public final class FriendshipSerialization {
    private FriendshipSerialization() {}

    public static CompoundTag write(FriendshipList list) {
        CompoundTag root = new CompoundTag();
        ListTag entries = new ListTag();
        for (FriendshipEntry entry : list.entries()) {
            CompoundTag tag = new CompoundTag();
            if (entry.individual()) tag.putUUID("uuid", entry.entityId());
            tag.putString("type", entry.typeId());
            tag.putString("name", entry.name());
            tag.putBoolean("favorite", entry.favorite());
            tag.putLong("added_at", entry.addedAt());
            entries.add(tag);
        }
        root.putInt("version", 2);
        root.put("entries", entries);
        return root;
    }

    public static FriendshipList read(CompoundTag root) {
        FriendshipList list = new FriendshipList();
        ListTag entries = root.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(entries.size(), FriendshipList.MAX_ENTRIES); i++) {
            CompoundTag tag = entries.getCompound(i);
            if (tag.getString("type").isBlank()) continue;
            list.put(new FriendshipEntry(tag.hasUUID("uuid") ? tag.getUUID("uuid") : null,
                    tag.getString("type"), tag.getString("name"), tag.getBoolean("favorite"),
                    tag.contains("added_at") ? tag.getLong("added_at") : -1));
        }
        return list;
    }
}
