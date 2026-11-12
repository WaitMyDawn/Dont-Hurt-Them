package yagen.waitmydawn.dont_hurt_them.core;

public record AlbumFilter(boolean individuals, boolean types, boolean favoritesOnly, boolean deadOnly) {
    public boolean matches(FriendshipEntry entry, boolean dead) {
        return (entry.individual() ? individuals : types) && (!favoritesOnly || entry.favorite()) && (!deadOnly || dead);
    }
}
