package yagen.waitmydawn.dont_hurt_them;

import com.electronwill.nightconfig.core.CommentedConfig;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import yagen.waitmydawn.dont_hurt_them.config.ClientConfigs;

@GameTestHolder(DontHurtThem.MODID)
@PrefixGameTestTemplate(false)
public final class ConfigGameTests {
    @GameTest(template = "empty")
    public static void missingClientConfigGeneratesDefault(GameTestHelper helper) {
        var config = CommentedConfig.inMemory();
        helper.assertTrue(!ClientConfigs.SPEC.isCorrect(config), "An absent key must require correction");
        ClientConfigs.SPEC.correct(config);
        helper.assertTrue(Integer.valueOf(4).equals(config.get("cardsPerPage")), "First load must create the default capacity");
        helper.assertTrue(ClientConfigs.SPEC.isCorrect(config), "Generated configuration must pass validation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void validClientCapacitiesSurviveCorrection(GameTestHelper helper) {
        for (int count : new int[] {2, 4, 8}) {
            var config = CommentedConfig.inMemory();
            config.set("cardsPerPage", count);
            ClientConfigs.SPEC.correct(config);
            helper.assertTrue(Integer.valueOf(count).equals(config.get("cardsPerPage")), "Correction must preserve capacity " + count);
            helper.assertTrue(ClientConfigs.SPEC.isCorrect(config), "Supported capacities must pass validation");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidClientCapacitiesRecoverToDefault(GameTestHelper helper) {
        for (Object value : new Object[] {null, -1, 0, 1, 3, 5, 16, "4", 4L, 4.0, true, List.of(4), Map.of("value", 4)}) {
            var config = CommentedConfig.inMemory();
            config.set("cardsPerPage", value);
            helper.assertTrue(!ClientConfigs.SPEC.isCorrect(config), "Unsupported values must require correction: " + value);
            ClientConfigs.SPEC.correct(config);
            helper.assertTrue(Integer.valueOf(4).equals(config.get("cardsPerPage")), "Unsupported values must recover to the default: " + value);
            helper.assertTrue(ClientConfigs.SPEC.isCorrect(config), "Corrected configuration must pass validation");
        }
        helper.succeed();
    }
}
