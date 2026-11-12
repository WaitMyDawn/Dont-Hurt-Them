package yagen.waitmydawn.dont_hurt_them.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent provenance for player-authored blocks, fluid/fire propagation and scheduled updates. */
public final class HazardLedger extends SavedData {
    private final Map<String, UUID> owners = new HashMap<>();
    public static HazardLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(HazardLedger::new, HazardLedger::load, null), "dont_hurt_them_causes");
    }
    private static String key(ServerLevel level, BlockPos pos) { return level.dimension().location() + "/" + pos.asLong(); }
    public UUID owner(ServerLevel level, BlockPos pos) { return owners.get(key(level, pos)); }
    public void changed(ServerLevel level, BlockPos pos, BlockState state, UUID owner) {
        String key = key(level, pos);
        UUID previous = state.isAir() || owner == null ? owners.remove(key) : owners.put(key, owner);
        if (previous != null || owner != null) setDirty();
    }
    private static HazardLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        HazardLedger ledger = new HazardLedger();
        for (String key : tag.getAllKeys()) if (tag.hasUUID(key)) ledger.owners.put(key, tag.getUUID(key));
        return ledger;
    }
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        owners.forEach(tag::putUUID);
        return tag;
    }
}
