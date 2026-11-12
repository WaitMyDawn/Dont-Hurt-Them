package yagen.waitmydawn.dont_hurt_them.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import yagen.waitmydawn.dont_hurt_them.config.ClientConfigs;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;

public final class ClientSetup implements ClientModInitializer {
    public void onInitializeClient() {
        ClientConfigs.load();
        ClientPlayNetworking.registerGlobalReceiver(FriendshipNetwork.Snapshot.TYPE, (packet, context) -> ClientFriendships.receive(packet));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientFriendships.logout());
        DamageProtection.clientProjectileProtection = ClientFriendships::protectsProjectileTarget;
    }
}
