package yagen.waitmydawn.dont_hurt_them.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import java.util.function.BiConsumer;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;

public final class FriendshipNetwork {
    // Installed exclusively during client setup; common packet registration must not resolve client classes.
    public static BiConsumer<Snapshot, Object> clientHandler = (packet, context) -> {};

    public record Action(String action, String key) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id("action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = new StreamCodec<>() {
            public Action decode(RegistryFriendlyByteBuf buf) { return new Action(buf.readUtf(24), buf.readUtf(256)); }
            public void encode(RegistryFriendlyByteBuf buf, Action p) { buf.writeUtf(p.action, 24); buf.writeUtf(p.key, 256); }
        };
        public Type<Action> type() { return TYPE; }
    }

    public record Snapshot(CompoundTag data, boolean open) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(id("snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            public Snapshot decode(RegistryFriendlyByteBuf buf) {
                CompoundTag tag = buf.readNbt();
                return new Snapshot(tag == null ? new CompoundTag() : tag, buf.readBoolean());
            }
            public void encode(RegistryFriendlyByteBuf buf, Snapshot p) { buf.writeNbt(p.data); buf.writeBoolean(p.open); }
        };
        public Type<Snapshot> type() { return TYPE; }
    }

    public record Rename(java.util.UUID entity, String name) implements CustomPacketPayload {
        public static final Type<Rename> TYPE = new Type<>(id("rename"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Rename> CODEC = new StreamCodec<>() {
            public Rename decode(RegistryFriendlyByteBuf buf) { return new Rename(buf.readUUID(), buf.readUtf(64)); }
            public void encode(RegistryFriendlyByteBuf buf, Rename packet) { buf.writeUUID(packet.entity); buf.writeUtf(packet.name, 64); }
        };
        public Type<Rename> type() { return TYPE; }
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(Action.TYPE, Action.CODEC);
        PayloadTypeRegistry.playC2S().register(Rename.TYPE, Rename.CODEC);
        PayloadTypeRegistry.playS2C().register(Snapshot.TYPE, Snapshot.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (packet, context) -> FriendshipService.action(context.player(), packet));
        ServerPlayNetworking.registerGlobalReceiver(Rename.TYPE, (packet, context) -> {
            if (!FriendshipService.rename(context.player(), packet.entity(), packet.name())) {
                context.player().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.dont_hurt_them.rename_failed"), false);
            }
        });
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(DontHurtThem.MODID, path); }
    private FriendshipNetwork() {}
}
