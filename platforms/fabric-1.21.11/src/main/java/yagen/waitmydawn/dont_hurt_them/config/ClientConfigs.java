package yagen.waitmydawn.dont_hurt_them.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.loader.api.FabricLoader;
import yagen.waitmydawn.dont_hurt_them.core.ModIdentity;

public final class ClientConfigs {
    public static final AtomicInteger CARDS_PER_PAGE = new AtomicInteger(4);
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve(ModIdentity.CLIENT_CONFIG); }
    public static void load() {
        try { Files.createDirectories(path().getParent()); }
        catch (IOException error) { throw new UncheckedIOException(error); }
        try (var config = CommentedFileConfig.builder(path()).sync().build()) {
            config.load();
            Object raw = config.get("cardsPerPage");
            int value = raw instanceof Integer count && valid(count) ? count : 4;
            CARDS_PER_PAGE.set(value);
            config.set("cardsPerPage", value);
            config.setComment("cardsPerPage", "Number of portrait cards per album page: 2, 4 or 8.");
            config.save();
        }
    }
    private static boolean valid(int count) { return count == 2 || count == 4 || count == 8; }
    public static void setCardsPerPage(int count) {
        if (!valid(count)) throw new IllegalArgumentException("Unsupported album capacity: " + count);
        try (var config = CommentedFileConfig.builder(path()).sync().build()) {
            config.load(); config.set("cardsPerPage", count); config.save(); CARDS_PER_PAGE.set(count);
        }
    }
    private ClientConfigs() {}
}
