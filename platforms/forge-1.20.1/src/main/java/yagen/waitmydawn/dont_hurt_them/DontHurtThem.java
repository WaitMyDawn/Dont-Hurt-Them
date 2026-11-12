package yagen.waitmydawn.dont_hurt_them;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import yagen.waitmydawn.dont_hurt_them.core.ModIdentity;
import net.minecraftforge.eventbus.api.IEventBus;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;

@Mod(DontHurtThem.MODID)
public final class DontHurtThem {
    public static final String MODID = ModIdentity.ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public DontHurtThem() {
        IEventBus bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                yagen.waitmydawn.dont_hurt_them.config.ClientConfigs.SPEC, ModIdentity.CLIENT_CONFIG);
        ModRegistries.ITEMS.register(bus);
        bus.addListener(ModRegistries::capabilities);
        FriendshipNetwork.register();
        bus.addListener(ModRegistries::creativeTab);
    }
}
