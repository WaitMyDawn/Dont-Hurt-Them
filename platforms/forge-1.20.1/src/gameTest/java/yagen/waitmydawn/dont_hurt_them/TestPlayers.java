package yagen.waitmydawn.dont_hurt_them;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;

final class TestPlayers {
    static ServerPlayer survival(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "dht_" + UUID.randomUUID().toString().substring(0, 8)));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }
}
