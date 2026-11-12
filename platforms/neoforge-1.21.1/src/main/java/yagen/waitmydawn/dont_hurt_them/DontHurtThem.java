package yagen.waitmydawn.dont_hurt_them;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;
import yagen.waitmydawn.dont_hurt_them.core.ModIdentity;
import net.neoforged.bus.api.IEventBus;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;
import yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork;

@Mod(DontHurtThem.MODID)
public final class DontHurtThem {
    public static final String MODID = ModIdentity.ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public DontHurtThem(IEventBus bus, ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,
                yagen.waitmydawn.dont_hurt_them.config.ClientConfigs.SPEC, ModIdentity.CLIENT_CONFIG);
        ModRegistries.ITEMS.register(bus);
        ModRegistries.ATTACHMENTS.register(bus);
        bus.addListener(FriendshipNetwork::register);
        bus.addListener(ModRegistries::creativeTab);
    }
}
