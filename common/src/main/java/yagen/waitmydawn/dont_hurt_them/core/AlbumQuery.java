package yagen.waitmydawn.dont_hurt_them.core;

import java.util.Locale;
import java.util.Objects;

public record AlbumQuery(AlbumFilter filter, NonLivingMode nonLivingMode, SearchField searchField, String query) {
    public enum EntityKind { UNKNOWN, LIVING, NON_LIVING }

    public enum NonLivingMode {
        INCLUDE("show_non_living"), EXCLUDE("hide_non_living"), ONLY("only_non_living");

        private final String labelKey;
        NonLivingMode(String labelKey) { this.labelKey = labelKey; }
        public String labelKey() { return labelKey; }
        public NonLivingMode next() { return values()[(ordinal() + 1) % values().length]; }
        public boolean matches(EntityKind kind) {
            // Failed or unavailable entity factories do not establish a nonliving classification.
            return this == INCLUDE || (this == EXCLUDE ? kind != EntityKind.NON_LIVING : kind == EntityKind.NON_LIVING);
        }
    }

    public enum SearchField {
        NAME("search_name"), REGISTRY_NAME("search_registry");

        private final String labelKey;
        SearchField(String labelKey) { this.labelKey = labelKey; }
        public String labelKey() { return labelKey; }
        public SearchField next() { return this == NAME ? REGISTRY_NAME : NAME; }
    }

    public AlbumQuery {
        Objects.requireNonNull(filter);
        Objects.requireNonNull(nonLivingMode);
        Objects.requireNonNull(searchField);
        query = normalize(query);
    }

    public boolean needsName() { return !query.isEmpty() && searchField == SearchField.NAME; }

    public boolean matches(FriendshipEntry entry, boolean dead, EntityKind kind, String displayedName) {
        return filter.matches(entry, dead) && nonLivingMode.matches(kind)
                && (query.isEmpty() || normalize(searchField == SearchField.NAME ? displayedName : entry.typeId()).contains(query));
    }

    private static String normalize(String value) { return value == null ? "" : value.strip().toLowerCase(Locale.ROOT); }
}
