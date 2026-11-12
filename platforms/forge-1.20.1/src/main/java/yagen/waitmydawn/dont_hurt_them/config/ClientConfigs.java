package yagen.waitmydawn.dont_hurt_them.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfigs {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.ConfigValue<Integer> CARDS_PER_PAGE = BUILDER
            .comment("Number of portrait cards per album page: 2, 4 or 8.")
            // Forge validates missing keys as null before inserting their defaults.
            .define("cardsPerPage", 4, value -> value instanceof Integer count && (count == 2 || count == 4 || count == 8));

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ClientConfigs() {
    }
    public static void setCardsPerPage(int count) { CARDS_PER_PAGE.set(count); SPEC.save(); }
}
