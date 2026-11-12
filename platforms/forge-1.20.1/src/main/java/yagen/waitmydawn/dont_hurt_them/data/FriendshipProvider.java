package yagen.waitmydawn.dont_hurt_them.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;

@Mod.EventBusSubscriber(modid = DontHurtThem.MODID)
public final class FriendshipProvider implements ICapabilitySerializable<CompoundTag> {
    private final FriendshipList list = new FriendshipList();
    private final LazyOptional<FriendshipList> capability = LazyOptional.of(() -> list);

    public <T> LazyOptional<T> getCapability(Capability<T> requested, Direction side) {
        return requested == ModRegistries.FRIENDSHIPS ? capability.cast() : LazyOptional.empty();
    }
    public CompoundTag serializeNBT() { return FriendshipSerialization.write(list); }
    public void deserializeNBT(CompoundTag tag) {
        for (var entry : list.entries()) list.remove(entry.key());
        for (var entry : FriendshipSerialization.read(tag).entries()) list.put(entry);
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        FriendshipProvider provider = new FriendshipProvider();
        event.addCapability(new ResourceLocation(DontHurtThem.MODID, "friendships"), provider);
        event.addListener(provider.capability::invalidate);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            original.getCapability(ModRegistries.FRIENDSHIPS).ifPresent(previous ->
                    event.getEntity().getCapability(ModRegistries.FRIENDSHIPS).ifPresent(next -> {
                        for (var entry : FriendshipSerialization.read(FriendshipSerialization.write(previous)).entries()) next.put(entry);
                    }));
        } finally { original.invalidateCaps(); }
    }
}
