package yagen.waitmydawn.dont_hurt_them.core;

import java.util.Objects;
import java.util.UUID;

public record FriendshipEntry(UUID entityId, String typeId, String name, boolean favorite, long addedAt) {
    public FriendshipEntry(UUID entityId, String typeId, String name, boolean favorite) {
        this(entityId, typeId, name, favorite, -1);
    }
    public FriendshipEntry {
        Objects.requireNonNull(typeId);
        Objects.requireNonNull(name);
        if (typeId.isBlank()) throw new IllegalArgumentException("Empty entity type");
    }

    public boolean individual() { return entityId != null; }
    public String key() { return individual() ? "entity/" + entityId : "type/" + typeId; }
    public FriendshipEntry withFavorite(boolean value) {
        return new FriendshipEntry(entityId, typeId, name, value, addedAt);
    }
    public FriendshipEntry withName(String value) { return new FriendshipEntry(entityId, typeId, value, favorite, addedAt); }
    public double friendshipDays(long now) { return addedAt < 0 ? -1 : Math.max(0, now - addedAt) / 24000.0; }
}
