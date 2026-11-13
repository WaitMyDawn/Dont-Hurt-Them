package yagen.waitmydawn.dont_hurt_them.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipSerialization;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;

@EventBusSubscriber(modid = DontHurtThem.MODID, value = Dist.CLIENT)
public final class ClientFriendships {
    private static FriendshipList list = new FriendshipList();
    private static CompoundTag live = new CompoundTag();
    private static long clock, receivedAt;
    public static FriendshipList list() { return list; }
    public static CompoundTag live(String key) { return live.getCompound(key); }
    public static boolean dead(String key) { return live(key).getBoolean("dead"); }
    public static String health(String key) {
        CompoundTag info = live(key);
        return yagen.waitmydawn.dont_hurt_them.core.AlbumHealth.label(info.getBoolean("loaded"),
                info.contains("health", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) ? info.getFloat("health") : Float.NaN,
                info.contains("max_health", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) ? info.getFloat("max_health") : Float.NaN);
    }
    public static long clock() {
        var level = Minecraft.getInstance().level;
        return clock + (level == null ? 0 : Math.max(0, level.getGameTime() - receivedAt));
    }

    public static void receive(FriendshipNetwork.Snapshot packet) {
        list = FriendshipSerialization.read(packet.data());
        live = packet.data().getCompound("live");
        Minecraft mc = Minecraft.getInstance();
        clock = packet.data().getLong("clock"); receivedAt = mc.level == null ? 0 : mc.level.getGameTime();
        if (packet.open() && mc.player != null) mc.setScreen(new FriendshipScreen());
    }

    public static int color(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !entity.level().isClientSide || !FriendshipService.holding(mc.player)) return -1;
        return list.color(entity.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    public static boolean protectsProjectileTarget(Entity entity, java.util.UUID owner) {
        var player = Minecraft.getInstance().player;
        return player != null && player.getUUID().equals(owner)
                && list.protects(entity.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        list = new FriendshipList(); live = new CompoundTag(); clock = 0; receivedAt = 0;
    }

    @SubscribeEvent
    public static void nameTag(net.neoforged.neoforge.client.event.RenderNameTagEvent event) {
        if (EntityPortrait.rendering()) event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
        else if (color(event.getEntity()) != -1) event.setCanRender(net.neoforged.neoforge.common.util.TriState.TRUE);
    }
    private ClientFriendships() {}
}
