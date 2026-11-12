package yagen.waitmydawn.dont_hurt_them.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipSerialization;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;

public final class ClientFriendships {
    private static FriendshipList list = new FriendshipList();
    private static CompoundTag live = new CompoundTag();
    private static long clock, receivedAt;
    public static FriendshipList list() { return list; }
    public static CompoundTag live(String key) { return live.getCompoundOrEmpty(key); }
    public static boolean dead(String key) { return live(key).getBooleanOr("dead", false); }
    public static long clock() {
        var level = Minecraft.getInstance().level;
        return clock + (level == null ? 0 : Math.max(0, level.getGameTime() - receivedAt));
    }

    public static void receive(FriendshipNetwork.Snapshot packet) {
        list = FriendshipSerialization.read(packet.data());
        live = packet.data().getCompoundOrEmpty("live");
        Minecraft mc = Minecraft.getInstance();
        clock = packet.data().getLongOr("clock", 0); receivedAt = mc.level == null ? 0 : mc.level.getGameTime();
        if (packet.open() && mc.player != null) mc.setScreen(new FriendshipScreen());
    }

    public static int color(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !entity.level().isClientSide() || !FriendshipService.holding(mc.player)) return -1;
        return list.color(entity.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    public static boolean showName(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Minecraft.renderNames() || entity == mc.getCameraEntity()
                || entity.isInvisibleTo(mc.player) || color(entity) == -1) return false;
        double range = entity.isDiscrete() ? 32 : 64;
        return mc.getEntityRenderDispatcher().distanceToSqr(entity) < range * range;
    }

    public static boolean protectsProjectileTarget(Entity entity, java.util.UUID owner) {
        var player = Minecraft.getInstance().player;
        return player != null && player.getUUID().equals(owner)
                && list.protects(entity.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    public static void logout() {
        list = new FriendshipList(); live = new CompoundTag(); clock = 0; receivedAt = 0;
    }

    private ClientFriendships() {}
}
