package yagen.waitmydawn.dont_hurt_them;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yagen.waitmydawn.dont_hurt_them.core.ModIdentity;

public final class DontHurtThem implements ModInitializer {
    public static final String MODID = ModIdentity.ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        yagen.waitmydawn.dont_hurt_them.registries.ModRegistries.init();
        yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork.register();
        yagen.waitmydawn.dont_hurt_them.server.FriendshipEvents.register();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) ->
                yagen.waitmydawn.dont_hurt_them.server.FriendshipCommands.register(dispatcher));
    }
}
