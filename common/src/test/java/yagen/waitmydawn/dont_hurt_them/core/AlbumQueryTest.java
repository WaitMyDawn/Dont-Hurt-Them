package yagen.waitmydawn.dont_hurt_them.core;

import java.util.Locale;
import java.util.UUID;
import static yagen.waitmydawn.dont_hurt_them.core.AlbumQuery.EntityKind.*;
import static yagen.waitmydawn.dont_hurt_them.core.AlbumQuery.NonLivingMode.*;
import static yagen.waitmydawn.dont_hurt_them.core.AlbumQuery.SearchField.*;

public final class AlbumQueryTest {
    public static void run() {
        AlbumFilter all = new AlbumFilter(true, true, false, false);
        FriendshipEntry entity = new FriendshipEntry(UUID.randomUUID(), "odd:glass_wren", "Old name", true, 100);
        FriendshipEntry type = new FriendshipEntry(null, "other:clockwork", "Stored type", false, 100);
        require(INCLUDE.next() == EXCLUDE && EXCLUDE.next() == ONLY && ONLY.next() == INCLUDE, "Nonliving modes cycle in UI order");
        require(NAME.next() == REGISTRY_NAME && REGISTRY_NAME.next() == NAME, "Search scope cycles");
        for (var kind : AlbumQuery.EntityKind.values()) {
            require(new AlbumQuery(all, INCLUDE, NAME, "").matches(entity, false, kind, null), "Default includes every classification");
            require(new AlbumQuery(all, EXCLUDE, NAME, "").matches(entity, false, kind, null) == (kind != NON_LIVING), "Hide excludes confirmed nonliving only");
            require(new AlbumQuery(all, ONLY, NAME, "").matches(entity, false, kind, null) == (kind == NON_LIVING), "Only requires positive nonliving evidence");
        }
        require(new AlbumQuery(all, INCLUDE, NAME, " 小伙伴 ").matches(entity, false, LIVING, "新名字小伙伴二号"), "Name search supports Chinese substrings and surrounding whitespace");
        require(!new AlbumQuery(all, INCLUDE, NAME, "Old name").matches(entity, false, LIVING, "New name"), "Renamed friends use their displayed name");
        require(new AlbumQuery(all, INCLUDE, NAME, "本地化").matches(type, false, NON_LIVING, "本地化名称"), "Type search uses the localized displayed name");
        require(!new AlbumQuery(all, INCLUDE, NAME, "glass_wren").matches(entity, false, LIVING, "Friend"), "Name scope does not search registry IDs");
        require(new AlbumQuery(all, INCLUDE, REGISTRY_NAME, "ODD:GLASS").matches(entity, false, LIVING, null), "Registry search matches namespace and path case insensitively");
        require(new AlbumQuery(all, INCLUDE, REGISTRY_NAME, "clockwork").matches(type, false, NON_LIVING, "Unrelated"), "Registry search supports path fragments in unrelated namespaces");
        require(!new AlbumQuery(all, INCLUDE, REGISTRY_NAME, "Unrelated").matches(type, false, NON_LIVING, "Unrelated"), "Registry scope does not search names");
        require(!new AlbumQuery(all, INCLUDE, NAME, "missing").matches(entity, false, UNKNOWN, null), "Missing names cannot establish a search match");
        require(!new AlbumQuery(all, INCLUDE, NAME, entity.entityId().toString()).matches(entity, false, LIVING, "Friend"), "UUID is outside the requested search scope");
        require(new AlbumQuery(new AlbumFilter(true, false, true, true), INCLUDE, NAME, "friend").matches(entity, true, LIVING, "Friend"), "Category, favorites, death and search filters compose");
        require(!new AlbumQuery(new AlbumFilter(true, false, true, true), ONLY, NAME, "friend").matches(entity, true, LIVING, "Friend"), "Nonliving mode remains an additional restriction");
        require(!new AlbumQuery(new AlbumFilter(true, false, false, false), INCLUDE, NAME, "name").matches(type, false, NON_LIVING, "Name"), "Search never re-enables a disabled category");
        require(!new AlbumQuery(new AlbumFilter(true, true, true, false), INCLUDE, NAME, "name").matches(type, false, NON_LIVING, "Name"), "Search never re-enables nonfavorite entries");
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            require(new AlbumQuery(all, INCLUDE, NAME, "IRIS").matches(entity, false, LIVING, "Iris"), "Search normalization is independent of the JVM locale");
        } finally { Locale.setDefault(previous); }
        System.out.println("Album nonliving modes and search: all assertions passed");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
