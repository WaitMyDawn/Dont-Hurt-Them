package yagen.waitmydawn.dont_hurt_them.core;

import java.util.Locale;

public final class AlbumHealth {
    public static String label(boolean loaded, float health, float maximum) {
        // Both values must come from one server snapshot; client entity initialization is not authoritative.
        if (!loaded || !Float.isFinite(health) || !Float.isFinite(maximum) || health < 0 || maximum <= 0) return "--";
        return String.format(Locale.ROOT, "%.1f / %.1f", health, maximum);
    }

    private AlbumHealth() {}
}
