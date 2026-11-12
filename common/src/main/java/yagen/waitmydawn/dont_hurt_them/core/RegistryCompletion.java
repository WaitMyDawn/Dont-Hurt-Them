package yagen.waitmydawn.dont_hurt_them.core;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Completion state for a supplied registry catalog; platform adapters supply registered IDs and render the popup. */
public final class RegistryCompletion {
    private final List<String> catalog;
    private List<String> matches = List.of();
    private int selection;
    private boolean visible;

    public RegistryCompletion(Collection<String> catalog) {
        this.catalog = catalog.stream().distinct().sorted().toList();
    }

    public void update(String input) {
        String query = input.trim().toLowerCase(Locale.ROOT);
        matches = catalog.stream().filter(id -> id.startsWith(query)
                || (!query.contains(":") && id.substring(id.indexOf(':') + 1).startsWith(query))).toList();
        selection = 0;
        visible = !query.isEmpty() && !matches.isEmpty();
    }

    public List<String> matches() { return matches; }
    public int selection() { return selection; }
    public String selected() { return matches.isEmpty() ? null : matches.get(selection); }
    public boolean visible() { return visible; }
    public void dismiss() { visible = false; }
    public void show() { visible = !matches.isEmpty(); }

    public void move(int delta) {
        if (matches.isEmpty()) return;
        selection = Math.floorMod(selection + delta, matches.size());
        show();
    }

    public void select(int index) {
        if (index >= 0 && index < matches.size()) selection = index;
    }
}
