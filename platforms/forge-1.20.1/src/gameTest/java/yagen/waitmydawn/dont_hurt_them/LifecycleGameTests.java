package yagen.waitmydawn.dont_hurt_them;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;
import java.util.function.Consumer;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipEntry;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipArchive;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipSerialization;
import yagen.waitmydawn.dont_hurt_them.data.OfflineFriendships;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipLifecycle;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;

@GameTestHolder(DontHurtThem.MODID)
@PrefixGameTestTemplate(false)
public final class LifecycleGameTests {
    private static ServerPlayer serverPlayer(GameTestHelper helper) {
        return net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "dht_" + UUID.randomUUID().toString().substring(0, 8)));
    }

    private static void befriend(Player player, Entity entity) {
        String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        FriendshipService.list(player).put(new FriendshipEntry(entity.getUUID(), type, entity.getName().getString(), true,
                player.level().getServer().overworld().getGameTime()));
        OfflineFriendships.get(player.level().getServer()).update(player.getUUID(), FriendshipService.list(player));
    }
    private static void kill(Entity entity) { entity.hurt(entity.damageSources().genericKill(), 10000); }

    @GameTest(template = "empty")
    public static void deathSnapshotRestoresHorseIdentityInventoryAndAttributes(GameTestHelper helper) {
        Horse horse = helper.spawn(EntityType.HORSE, 2, 2, 2);
        Player player = TestPlayers.survival(helper);
        horse.setVariant(Variant.BLACK); horse.setTamed(true); horse.setOwnerUUID(player.getUUID());
        horse.setCustomName(Component.literal("Traveler")); horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(45); horse.setHealth(45);
        helper.assertTrue(horse.getSlot(400).set(new ItemStack(Items.SADDLE)), "The horse fixture must contain saddle inventory");
        horse.getPersistentData().putString("test_marker", "irregular-state");
        befriend(player, horse); UUID uuid = horse.getUUID();
        kill(horse);
        var archive = FriendshipArchive.get(helper.getLevel().getServer());
        var record = archive.find(uuid);
        helper.assertTrue(record != null && record.dead() && record.entity().contains("SaddleItem"), "Snapshot must precede death drops");
        helper.assertTrue(record.cause().contains("damage_type") && record.dimension().equals(helper.getLevel().dimension().location().toString()), "Death provenance must be recorded");
        var loaded = FriendshipArchive.load(archive.save(new CompoundTag()));
        helper.assertTrue(loaded.find(uuid).dead() && loaded.find(uuid).entity().contains("SaddleItem"), "Full death record persists through NBT");
        try { FriendshipLifecycle.revive(player, "minecraft:horse", uuid); helper.fail("Loaded corpse must prevent duplicate UUIDs"); }
        catch (IllegalArgumentException expected) { helper.assertTrue(expected.getMessage().equals("uuid_conflict"), "Expected UUID conflict"); }
        horse.discard();
        Horse restored = (Horse) FriendshipLifecycle.revive(player, "minecraft:horse", uuid);
        helper.assertTrue(restored.getUUID().equals(uuid) && restored.getVariant() == Variant.BLACK && restored.getCustomName().getString().equals("Traveler"), "UUID, variant and custom name are restored");
        helper.assertTrue(restored.getSlot(400).get().is(Items.SADDLE) && restored.getOwnerUUID().equals(player.getUUID()), "Inventory and ownership are restored");
        helper.assertTrue(restored.getMaxHealth() == 45 && restored.getHealth() == 45 && restored.getPersistentData().getString("test_marker").equals("irregular-state"), "Attributes, health and mod data are restored");
        helper.assertTrue(!archive.find(uuid).dead() && !FriendshipLifecycle.canRevive(player, "minecraft:horse", uuid), "Successful revival consumes active death status");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cancelledDeathAndUnloadingDoNotCreateTombstones(GameTestHelper helper) {
        var cow = helper.spawn(EntityType.COW, 2, 2, 2);
        Player player = TestPlayers.survival(helper); befriend(player, cow);
        Consumer<LivingDeathEvent> cancel = event -> { if (event.getEntity() == cow) { event.setCanceled(true); cow.setHealth(1); } };
        MinecraftForge.EVENT_BUS.addListener(cancel);
        try { kill(cow); } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        helper.assertTrue(cow.isAlive() && FriendshipArchive.get(helper.getLevel().getServer()).find(cow.getUUID()) == null, "Cancelled death is not a confirmed death");
        cow.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        helper.assertTrue(FriendshipArchive.get(helper.getLevel().getServer()).find(cow.getUUID()) == null, "Unloading is not proof of death");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blockedRevivalRetainsTheDeathRecord(GameTestHelper helper) {
        var sheep = helper.spawn(EntityType.SHEEP, 2, 2, 2);
        Player player = TestPlayers.survival(helper); befriend(player, sheep); UUID uuid = sheep.getUUID();
        kill(sheep); sheep.discard();
        Consumer<EntityJoinLevelEvent> block = event -> { if (event.getEntity().getUUID().equals(uuid)) event.setCanceled(true); };
        MinecraftForge.EVENT_BUS.addListener(block);
        try {
            try { FriendshipLifecycle.revive(player, "minecraft:sheep", uuid); helper.fail("Blocked spawn must fail revival"); }
            catch (IllegalArgumentException expected) { helper.assertTrue(expected.getMessage().equals("spawn_blocked"), "Blocked spawn has a distinct result"); }
        } finally { MinecraftForge.EVENT_BUS.unregister(block); }
        helper.assertTrue(FriendshipLifecycle.canRevive(player, "minecraft:sheep", uuid) && helper.getLevel().getEntity(uuid) == null, "Failure retains the record without a partial entity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void reviveCompletionUsesOnlyThePlayersRecordedDeaths(GameTestHelper helper) throws Exception {
        ServerPlayer player = serverPlayer(helper);
        var cow = helper.spawn(EntityType.COW, 2, 2, 2); befriend(player, cow); kill(cow);
        Player other = TestPlayers.survival(helper);
        var sheep = helper.spawn(EntityType.SHEEP, 3, 2, 2); befriend(other, sheep); kill(sheep);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var source = player.createCommandSourceStack().withPermission(2);
        var types = dispatcher.getCompletionSuggestions(dispatcher.parse("dht revive ", source)).join().getList().stream().map(s -> s.getText()).toList();
        helper.assertTrue(types.contains("minecraft:cow") && !types.contains("minecraft:sheep"), "Type suggestions are scoped to the player's own dead friends");
        var uuids = dispatcher.getCompletionSuggestions(dispatcher.parse("dht revive minecraft:cow ", source)).join().getList().stream().map(s -> s.getText()).toList();
        helper.assertTrue(uuids.contains(cow.getUUID().toString()) && !uuids.contains(sheep.getUUID().toString()), "UUID suggestions follow the selected type and owner");
        helper.assertTrue(!dispatcher.getRoot().getChild("dht").canUse(source.withPermission(0)), "Revival requires cheat permission");
        helper.assertTrue(!FriendshipLifecycle.canRevive(other, "minecraft:cow", cow.getUUID()), "A global archive does not grant another player's access");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void renameRequiresAnOwnedLoadedFriendAndPreservesFriendshipAge(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper);
        var cow = helper.spawn(EntityType.COW, 2, 2, 2); befriend(player, cow);
        long added = FriendshipService.list(player).entries().iterator().next().addedAt();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistries.PROOF_OF_FRIENDSHIP.get()));
        helper.assertTrue(FriendshipService.rename(player, cow.getUUID(), "Dinnerbone"), "A loaded friend may be renamed");
        helper.assertTrue(cow.getCustomName().getString().equals("Dinnerbone") && cow.isPersistenceRequired(), "Rename has name-tag name and persistence semantics");
        helper.assertTrue(FriendshipService.list(player).entries().iterator().next().addedAt() == added, "Rename preserves friendship age");
        var stranger = helper.spawn(EntityType.PIG, 4, 2, 2);
        helper.assertTrue(!FriendshipService.rename(player, stranger.getUUID(), "No") && !FriendshipService.rename(player, cow.getUUID(), "x".repeat(65)), "Foreign targets and invalid names are rejected");
        kill(cow);
        helper.assertTrue(!FriendshipService.rename(player, cow.getUUID(), "No"), "Deceased targets cannot be renamed");
        befriend(player, player);
        helper.assertTrue(!FriendshipService.rename(player, player.getUUID(), "No"), "Name tags do not rewrite player identities");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void friendshipTimestampMigrationPreservesUnknownHistory(GameTestHelper helper) {
        var list = new yagen.waitmydawn.dont_hurt_them.core.FriendshipList();
        list.put(new FriendshipEntry(UUID.randomUUID(), "odd:unrelated", "Friend", true, 1234));
        CompoundTag saved = FriendshipSerialization.write(list);
        helper.assertTrue(FriendshipSerialization.read(saved).entries().iterator().next().addedAt() == 1234, "New timestamps persist");
        saved.getList("entries", 10).getCompound(0).remove("added_at");
        helper.assertTrue(FriendshipSerialization.read(saved).entries().iterator().next().addedAt() == -1, "Legacy history remains unknown rather than fabricated");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void failedEntitySerializerDoesNotBreakDeathOrEnablePartialRevival(GameTestHelper helper) {
        var cow = new net.minecraft.world.entity.animal.Cow(EntityType.COW, helper.getLevel()) {
            @Override public void addAdditionalSaveData(CompoundTag tag) { throw new IllegalStateException("fixture serializer failure"); }
        };
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        cow.setPos(pos.getX(), pos.getY(), pos.getZ()); helper.getLevel().addFreshEntity(cow);
        Player player = TestPlayers.survival(helper); befriend(player, cow); kill(cow);
        var record = FriendshipArchive.get(helper.getLevel().getServer()).find(cow.getUUID());
        helper.assertTrue(!cow.isAlive() && record != null && record.dead() && record.cause().getBoolean("snapshot_failed"), "Confirmed death remains recorded despite serializer failure");
        helper.assertTrue(!FriendshipLifecycle.canRevive(player, "minecraft:cow", cow.getUUID()), "Incomplete snapshots are not advertised as revivable");
        helper.succeed();
    }
}
