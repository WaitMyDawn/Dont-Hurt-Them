package yagen.waitmydawn.dont_hurt_them.core;

import java.util.List;
import java.util.UUID;

public final class FriendshipRulesTest {
    public static void main(String[] args) {
        RegistryCompletionTest.run();
        AlbumRulesTest.run();
        UUID individual = UUID.fromString("43baf85b-8a67-483e-8753-34093a0e8448");
        FriendshipList alice = new FriendshipList(), bob = new FriendshipList();
        FriendshipEntry specific = new FriendshipEntry(individual, "odd:unrelated", "Zed", false);
        FriendshipEntry type = new FriendshipEntry(null, "odd:unrelated", "Type", false);
        alice.put(type); alice.put(specific);
        require(alice.sorted().get(0).equals(specific), "Individuals precede types");
        require(alice.color(individual, type.typeId()) == FriendshipList.INDIVIDUAL_COLOR, "Individual color wins");
        require(alice.color(UUID.randomUUID(), type.typeId()) == FriendshipList.TYPE_COLOR, "Exact type match");
        require(!alice.protects(UUID.randomUUID(), "odd:unrelated_child"), "Registry prefixes do not establish membership");
        require(!bob.protects(individual, type.typeId()), "Lists are isolated by owner");
        alice.toggleFavorite(type.key());
        require(alice.sorted().get(0).key().equals(type.key()), "Favorites override category order");
        alice.remove(specific.key());
        require(alice.color(individual, type.typeId()) == FriendshipList.TYPE_COLOR, "Removing an individual retains type protection");
        alice.remove(type.key());
        require(!alice.protects(individual, type.typeId()), "Removing both rules removes protection");
        require(!alice.toggleFavorite("entity/missing"), "Missing entries are not created by pinning");
        for (String id : List.of("z:alpha", "a:zeta", "a:beta")) alice.put(new FriendshipEntry(null, id, id, false));
        require(alice.sorted().stream().map(FriendshipEntry::typeId).toList().equals(List.of("a:beta", "a:zeta", "z:alpha")), "Lexical registry order");
        FriendshipList full = new FriendshipList();
        for (int i = 0; i < FriendshipList.MAX_ENTRIES; i++) require(full.put(new FriendshipEntry(null, "test:t" + i, "T", false)), "Capacity");
        require(!full.put(new FriendshipEntry(null, "test:overflow", "T", false)), "Bounded list");
        require(full.put(new FriendshipEntry(null, "test:t0", "Updated", true)), "Updates remain possible at capacity");
        full.remove("type/test:t1");
        require(full.put(new FriendshipEntry(null, "test:replacement", "T", false)), "Removal releases capacity");
        System.out.println("Friendship rules: all assertions passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
