package yagen.waitmydawn.dont_hurt_them.core;

import java.util.UUID;

public final class AlbumRulesTest {
    public static void run() {
        FriendshipEntry entity = new FriendshipEntry(UUID.randomUUID(), "odd:animal", "Friend", false, 100);
        FriendshipEntry type = new FriendshipEntry(null, "odd:another", "Type", false, 100);
        require(new AlbumFilter(true, true, false, false).matches(entity, true), "Default view includes living and deceased individuals");
        require(new AlbumFilter(false, true, false, false).matches(type, false), "Categories use OR logic");
        require(!new AlbumFilter(false, true, false, false).matches(entity, false), "Disabled categories stay hidden");
        require(!new AlbumFilter(true, true, true, false).matches(entity, false), "Favorites are an additional restriction");
        require(new AlbumFilter(true, true, true, true).matches(entity.withFavorite(true), true), "Favorite and death filters compose");
        require(!new AlbumFilter(true, true, false, true).matches(entity, false), "Death-only excludes alive/unloaded entries");
        require(entity.withFavorite(true).addedAt() == 100 && entity.withName("New").addedAt() == 100, "Mutations preserve friendship start");
        require(entity.friendshipDays(24100) == 1.0 && entity.friendshipDays(0) == 0, "Game-time duration is monotonic and nonnegative");
        require(new FriendshipEntry(entity.entityId(), "odd:animal", "Legacy", false).friendshipDays(100000) < 0, "Legacy timestamps stay unknown");
        for (float[] size : new float[][]{{1.4f, 1.6f}, {0.4f, 3.5f}, {8, 2}, {0.2f, 0.2f}}) {
            float scale = PortraitFit.scale(120, 200, size[0], size[1]);
            double diameter = Math.sqrt(Math.pow(Math.max(0.4f, size[1]), 2) + 2 * Math.pow(Math.max(0.4f, size[0]), 2)) * 1.4;
            require(scale > 0 && diameter * scale <= 108.01, "Rotating bodies fit their smaller viewport dimension");
        }
        for (int width : new int[]{240, 320, 464, 800, 1920}) for (int height : new int[]{180, 240, 480, 1080}) for (int capacity : new int[]{2, 4, 8}) {
            AlbumLayout layout = AlbumLayout.create(width, height, capacity);
            require(layout.rows() * layout.columns() == capacity, "Configured capacity is independent of window size");
            require(layout.cardHeight() > layout.cardWidth(), "Cards stay portrait-oriented on dense and wide screens");
            for (int i = 0; i < capacity; i++) {
                require(layout.cardX(i) >= layout.left() && layout.cardY(i) > layout.top() + 40, "Cards do not overlap filter controls");
                require((layout.cardX(i) + layout.cardWidth()) * layout.scale() <= width
                        && (layout.cardY(i) + layout.cardHeight()) * layout.scale() <= height, "Cards fit inside the physical GUI viewport");
                require(layout.cardY(i) + layout.cardHeight() <= layout.top() + layout.panelHeight() - 55, "Cards do not overlap footer controls");
            }
        }
        System.out.println("Album filters, duration and portrait fitting: all assertions passed");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
