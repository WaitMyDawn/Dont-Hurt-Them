package yagen.waitmydawn.dont_hurt_them.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;

@EventBusSubscriber(modid = DontHurtThem.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
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
