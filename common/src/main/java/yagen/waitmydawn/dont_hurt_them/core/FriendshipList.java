package yagen.waitmydawn.dont_hurt_them.core;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Personal protection rules. Platform adapters own persistence, identity resolution and synchronization. */
public final class FriendshipList {
    public static final int MAX_ENTRIES = 256;
    public static final int INDIVIDUAL_COLOR = 0x55FF55;
    public static final int TYPE_COLOR = 0x5599FF;
    private static final Comparator<FriendshipEntry> ORDER = Comparator
            .comparing(FriendshipEntry::favorite).reversed()
            .thenComparing(entry -> !entry.individual())
            .thenComparing(FriendshipEntry::typeId)
            .thenComparing(FriendshipEntry::name)
            .thenComparing(FriendshipEntry::key);
    private final Map<String, FriendshipEntry> entries = new LinkedHashMap<>();

    public Collection<FriendshipEntry> entries() { return List.copyOf(entries.values()); }
    public List<FriendshipEntry> sorted() { return entries.values().stream().sorted(ORDER).toList(); }
    public boolean contains(String key) { return entries.containsKey(key); }
    public FriendshipEntry get(String key) { return entries.get(key); }

    public boolean put(FriendshipEntry entry) {
        if (entries.size() >= MAX_ENTRIES && !entries.containsKey(entry.key())) return false;
        entries.put(entry.key(), entry);
        return true;
    }

    public boolean remove(String key) { return entries.remove(key) != null; }

    public boolean toggleFavorite(String key) {
        FriendshipEntry entry = entries.get(key);
        if (entry == null) return false;
        entries.put(key, entry.withFavorite(!entry.favorite()));
        return true;
    }

    public int color(UUID entityId, String typeId) {
        if (entityId != null && entries.containsKey("entity/" + entityId)) return INDIVIDUAL_COLOR;
        return entries.containsKey("type/" + typeId) ? TYPE_COLOR : -1;
    }

    public boolean protects(UUID entityId, String typeId) { return color(entityId, typeId) != -1; }
}
