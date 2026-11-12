package yagen.waitmydawn.dont_hurt_them.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfigs {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.ConfigValue<Integer> CARDS_PER_PAGE = BUILDER
            .comment("Number of portrait cards per album page: 2, 4 or 8.")
            // NeoForge validates missing keys as null before inserting their defaults.
            .define("cardsPerPage", 4, value -> value instanceof Integer count && (count == 2 || count == 4 || count == 8));

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfigs() {
    }
    public static void setCardsPerPage(int count) { CARDS_PER_PAGE.set(count); SPEC.save(); }
}
