package yagen.waitmydawn.dont_hurt_them.network;

import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;

public final class FriendshipNetwork {
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DontHurtThem.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    public static BiConsumer<Snapshot, NetworkEvent.Context> clientHandler = (packet, context) -> {};
    public record Action(String action, String key) {}
    public record Snapshot(CompoundTag data, boolean open) {}
    public record Rename(UUID entity, String name) {}

    public static void register() {
        CHANNEL.messageBuilder(Action.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buf) -> { buf.writeUtf(packet.action(), 24); buf.writeUtf(packet.key(), 256); })
                .decoder(buf -> new Action(buf.readUtf(24), buf.readUtf(256)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) FriendshipService.action(player, packet);
                }).add();
        CHANNEL.messageBuilder(Snapshot.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buf) -> { buf.writeNbt(packet.data()); buf.writeBoolean(packet.open()); })
                .decoder(buf -> { CompoundTag tag = buf.readNbt(); return new Snapshot(tag == null ? new CompoundTag() : tag, buf.readBoolean()); })
                .consumerMainThread((packet, context) -> clientHandler.accept(packet, context.get())).add();
        CHANNEL.messageBuilder(Rename.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buf) -> { buf.writeUUID(packet.entity()); buf.writeUtf(packet.name(), 64); })
                .decoder(buf -> new Rename(buf.readUUID(), buf.readUtf(64)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && !FriendshipService.rename(player, packet.entity(), packet.name())) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.dont_hurt_them.rename_failed"), false);
                    }
                }).add();
    }
    public static void send(ServerPlayer player, Snapshot packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void sendToServer(Object packet) { CHANNEL.sendToServer(packet); }
    private FriendshipNetwork() {}
}
