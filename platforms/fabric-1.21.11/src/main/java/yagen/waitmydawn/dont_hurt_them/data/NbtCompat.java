package yagen.waitmydawn.dont_hurt_them.data;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class NbtCompat {
    public static UUID uuid(CompoundTag tag, String key) { return tag.read(key, UUIDUtil.CODEC).orElseThrow(); }
    public static boolean hasUuid(CompoundTag tag, String key) { return tag.read(key, UUIDUtil.CODEC).isPresent(); }
    public static void putUuid(CompoundTag tag, String key, UUID value) { tag.store(key, UUIDUtil.CODEC, value); }
    public static boolean contains(CompoundTag tag, String key, int type) {
        Tag value = tag.get(key);
        return value != null && value.getId() == type;
    }
    public static ListTag list(CompoundTag tag, String key, int type) {
        ListTag result = new ListTag();
        for (Tag element : tag.getListOrEmpty(key)) if (element.getId() == type) result.add(element);
        return result;
    }
    public static CompoundTag save(Entity entity) {
        var problems = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(problems, entity.registryAccess());
        entity.saveWithoutId(output);
        if (!problems.isEmpty()) throw new IllegalStateException(problems.getReport());
        return output.buildResult();
    }
    public static Entity load(CompoundTag tag, ServerLevel level) {
        var problems = new ProblemReporter.Collector();
        Entity entity = EntityType.loadEntityRecursive(TagValueInput.create(problems, level.registryAccess(), tag), level,
                EntitySpawnReason.LOAD, value -> value);
        if (!problems.isEmpty()) throw new IllegalArgumentException(problems.getReport());
        return entity;
    }
    private NbtCompat() {}
}
