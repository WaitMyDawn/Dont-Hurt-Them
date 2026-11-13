package yagen.waitmydawn.dont_hurt_them.core;

import java.util.Locale;

public final class AlbumHealthTest {
    public static void run() {
        require(AlbumHealth.label(true, 1, 1).equals("1.0 / 1.0"), "One-health friends retain their server values");
        require(AlbumHealth.label(true, 0.5f, 1).equals("0.5 / 1.0"), "Fractional damage is preserved");
        require(AlbumHealth.label(true, 7.25f, 45.5f).equals("7.3 / 45.5"), "Custom attributes and fractional health are displayed together");
        require(AlbumHealth.label(true, 0, 45.5f).equals("0.0 / 45.5"), "Zero health is valid before a confirmed death arrives");
        require(AlbumHealth.label(true, 37.5f, 20).equals("37.5 / 20.0"), "Display does not overwrite third-party values with a local clamp");
        require(AlbumHealth.label(false, 1, 1).equals("--"), "Unloaded entries cannot reuse stale health");
        for (float[] pair : new float[][]{{Float.NaN, 1}, {1, Float.NaN}, {Float.POSITIVE_INFINITY, 1}, {1, Float.NEGATIVE_INFINITY}, {-1, 1}, {1, 0}, {1, -1}}) {
            require(AlbumHealth.label(true, pair[0], pair[1]).equals("--"), "Missing, partial and invalid pairs remain unknown");
        }
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            require(AlbumHealth.label(true, 2.5f, 80).equals("2.5 / 80.0"), "Health formatting does not depend on the JVM locale");
        } finally { Locale.setDefault(previous); }
        System.out.println("Album authoritative health pairs: all assertions passed");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
