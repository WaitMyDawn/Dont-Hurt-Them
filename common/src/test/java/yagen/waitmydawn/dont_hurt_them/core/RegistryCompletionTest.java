package yagen.waitmydawn.dont_hurt_them.core;

import java.util.List;

public final class RegistryCompletionTest {
    public static void run() {
        RegistryCompletion completion = new RegistryCompletion(List.of(
                "zeta:owl", "alpha:river_ray", "other:river_boat", "alpha:owl", "alpha:owl"));
        completion.update("");
        require(!completion.visible() && completion.matches().size() == 4, "Empty input must not force a popup");
        completion.show();
        require(completion.visible(), "Explicit completion can open the whole catalog");
        completion.update("owl");
        require(completion.matches().equals(List.of("alpha:owl", "zeta:owl")), "Unqualified paths match all supplied namespaces");
        completion.move(-1);
        require(completion.selected().equals("zeta:owl"), "Backward selection wraps");
        completion.move(1);
        require(completion.selected().equals("alpha:owl"), "Forward selection wraps");
        completion.dismiss();
        require(!completion.visible() && completion.selected().equals("alpha:owl"), "Dismissal preserves candidates for reopening");
        completion.update("river_");
        require(completion.matches().equals(List.of("alpha:river_ray", "other:river_boat")), "Path prefixes preserve unrelated registered IDs");
        completion.update("ALPHA:R");
        require(completion.matches().equals(List.of("alpha:river_ray")), "Qualified input is normalized and namespace-specific");
        completion.update("zeta:");
        require(completion.selected().equals("zeta:owl"), "Namespace completion");
        completion.update("unknown:owl");
        completion.move(1);
        require(completion.matches().isEmpty() && completion.selected() == null && !completion.visible(), "Unknown identifiers do not invent candidates");
        System.out.println("Registry completion: all assertions passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
