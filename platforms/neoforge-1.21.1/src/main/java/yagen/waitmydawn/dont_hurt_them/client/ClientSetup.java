package yagen.waitmydawn.dont_hurt_them.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;

@EventBusSubscriber(modid = DontHurtThem.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            FriendshipNetwork.clientHandler = (packet, context) -> ClientFriendships.receive(packet);
            DamageProtection.clientProjectileProtection = ClientFriendships::protectsProjectileTarget;
        });
    }
    private ClientSetup() {}
}
