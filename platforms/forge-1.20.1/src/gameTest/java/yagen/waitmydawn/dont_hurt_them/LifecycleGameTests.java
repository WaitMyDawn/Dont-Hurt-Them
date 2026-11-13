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
    @GameTest(template = "empty")
    public static void revivalDoesNotArchiveOrRestoreLivingPassengers(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer();
        var horse = helper.spawn(EntityType.HORSE, 2, 2, 2);
        var rider = helper.spawn(EntityType.COW, 3, 2, 2);
        helper.assertTrue(rider.startRiding(horse), "Living rider mounts the horse fixture");
        befriend(owner, horse); befriend(owner, rider);
        UUID riderId = rider.getUUID();
        kill(horse);
        var archive = FriendshipArchive.get(helper.getLevel().getServer());
        helper.assertTrue(!archive.find(horse.getUUID()).entity().contains("Passengers"), "New snapshots exclude every passenger");
        kill(rider); rider.discard(); horse.discard();
        String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.HORSE).toString();
        Entity restored = FriendshipLifecycle.revive(owner, type, horse.getUUID());
        helper.assertTrue(restored.getPassengers().isEmpty() && helper.getLevel().getEntity(riderId) == null, "Only the target horse is restored");
        helper.assertTrue(archive.find(riderId).dead(), "Another friend's death record is not consumed with the mount");
        helper.succeed();
    }



    @GameTest(template = "empty")
    public static void destroyedNonlivingFriendsAreForgottenWithoutDeathSnapshots(GameTestHelper helper) {
        ServerPlayer owner = serverPlayer(helper);
        var frame = EntityType.ITEM_FRAME.create(helper.getLevel());
        if (frame == null) { helper.fail("Cannot create frame fixture"); return; }
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        frame.setPos(pos.getX(), pos.getY(), pos.getZ());
        helper.assertTrue(helper.getLevel().addFreshEntity(frame), "Frame fixture enters the level");
        befriend(owner, frame);
        String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(frame.getType()).toString();
        FriendshipService.list(owner).put(new FriendshipEntry(null, type, "Frames", false, 1));
        Player offlineOwner = helper.makeMockPlayer(); befriend(offlineOwner, frame);
        var savedPlayerList = FriendshipSerialization.read(FriendshipSerialization.write(FriendshipService.list(offlineOwner)));
        helper.assertTrue(FriendshipLifecycle.capture(frame, null) == null, "Nonliving entities never create death snapshots");
        frame.setRemoved(Entity.RemovalReason.KILLED);
        helper.assertTrue(!FriendshipService.list(owner).contains("entity/" + frame.getUUID()), "Destruction removes even a favorite individual frame");
        helper.assertTrue(FriendshipService.list(owner).contains("type/" + type), "Entity-type protection survives individual destruction");
        helper.assertTrue(FriendshipArchive.get(helper.getLevel().getServer()).find(frame.getUUID()) == null, "No nonliving death archive is retained");
        var mirror = OfflineFriendships.get(helper.getLevel().getServer());
        helper.assertTrue(!mirror.find(offlineOwner.getUUID()).contains("entity/" + frame.getUUID()), "Offline enforcement mirror is cleaned");
        mirror.applyRemovals(offlineOwner.getUUID(), savedPlayerList);
        helper.assertTrue(!savedPlayerList.contains("entity/" + frame.getUUID()), "Pending cleanup removes the stale persisted player entry on return");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyNonlivingDeathsCannotBeRevivedAndLivingMiscTypesRemainValid(GameTestHelper helper) {
        ServerPlayer owner = serverPlayer(helper);
        var frame = EntityType.ITEM_FRAME.create(helper.getLevel());
        if (frame == null) { helper.fail("Cannot create legacy frame fixture"); return; }
        befriend(owner, frame);
        String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(frame.getType()).toString();
        var archive = FriendshipArchive.get(helper.getLevel().getServer());
        archive.record(new FriendshipArchive.DeathRecord(frame.getUUID(), type, "minecraft:overworld", "Old frame", 1, -1, new CompoundTag(), new CompoundTag()));
        helper.assertTrue(!FriendshipLifecycle.canRevive(owner, type, frame.getUUID()), "Legacy nonliving death records are not eligible for revival");
        FriendshipService.changed(owner);
        helper.assertTrue(!FriendshipService.list(owner).contains("entity/" + frame.getUUID()) && archive.find(frame.getUUID()) == null, "Legacy nonliving entries and snapshots are removed");
        String armorStand = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ARMOR_STAND).toString();
        helper.assertTrue(!FriendshipLifecycle.isKnownNonLiving(new FriendshipArchive.DeathRecord(UUID.randomUUID(), armorStand, "minecraft:overworld", "Stand", 1, -1, new CompoundTag(), new CompoundTag()), helper.getLevel()), "LivingEntity classification must not use the MISC spawn category");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void revivalRefusesPlayerArguments(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer();
        String playerType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER).toString();
        try { FriendshipLifecycle.revive(owner, playerType, UUID.randomUUID()); helper.fail("Player revival must be rejected before loading a snapshot"); }
        catch (IllegalArgumentException expected) { helper.assertTrue(expected.getMessage().equals("player_revive_forbidden"), "Player revival has a dedicated rejection"); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unloadedGravesStayUnloadedAndExplicitDestinationsCanOverrideThem(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer();
        var cow = helper.spawn(EntityType.COW, 2, 2, 2); befriend(owner, cow); kill(cow); cow.discard();
        var archive = FriendshipArchive.get(helper.getLevel().getServer());
        var old = archive.find(cow.getUUID());
        var snapshot = old.entity().copy();
        net.minecraft.nbt.ListTag far = new net.minecraft.nbt.ListTag();
        far.add(net.minecraft.nbt.DoubleTag.valueOf(24000000.5)); far.add(net.minecraft.nbt.DoubleTag.valueOf(70)); far.add(net.minecraft.nbt.DoubleTag.valueOf(24000000.5));
        snapshot.put("Pos", far);
        archive.record(new FriendshipArchive.DeathRecord(old.uuid(), old.type(), old.dimension(), old.name(), old.diedAt(), -1, snapshot, old.cause()));
        helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(1500000, 1500000) == null, "Distant grave starts unloaded");
        try { FriendshipLifecycle.revive(owner, old.type(), old.uuid()); helper.fail("Unloaded graves must not be loaded by revival"); }
        catch (IllegalArgumentException expected) { helper.assertTrue(expected.getMessage().equals("grave_unloaded"), "Unloaded grave rejection"); }
        helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(1500000, 1500000) == null && archive.find(old.uuid()).dead(), "Failure neither loads the grave nor consumes the record");
        var target = cow.position().add(0, 1, 0);
        Entity restored = FriendshipLifecycle.revive(owner, old.type(), old.uuid(), helper.getLevel(), target);
        helper.assertTrue(restored.position().distanceToSqr(target) < 0.000001, "Explicit destination overrides the unloaded grave");
        helper.assertTrue(!archive.find(old.uuid()).dead(), "Successful override consumes active death status");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void revivalCoordinatesUseVanillaParsingAndRequireThreeComponents(GameTestHelper helper) throws Exception {
        ServerPlayer owner = serverPlayer(helper);
        var cow = helper.spawn(EntityType.COW, 2, 2, 2); befriend(owner, cow); kill(cow); cow.discard();
        owner.setPos(cow.getX(), cow.getY() + 1, cow.getZ());
        var source = owner.createCommandSourceStack().withPermission(2);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        String command = "dht revive minecraft:cow " + cow.getUUID();
        try { dispatcher.execute(command + " ~ ~", source); helper.fail("Two coordinates cannot execute revival"); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
        helper.assertTrue(FriendshipArchive.get(helper.getLevel().getServer()).find(cow.getUUID()).dead(), "Incomplete coordinates preserve the record");
        helper.assertTrue(dispatcher.execute(command + " ~ ~ ~", source) == 1, "Vanilla relative coordinates execute revival");
        var restored = helper.getLevel().getEntity(cow.getUUID());
        helper.assertTrue(restored != null && restored.position().distanceToSqr(source.getPosition()) < 0.000001, "Relative destination uses the command source position");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clearingDeceasedPreservesFavoritesOtherRulesAndSharedArchives(GameTestHelper helper) {
        ServerPlayer owner = serverPlayer(helper);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistries.PROOF_OF_FRIENDSHIP.get()));
        var cow = helper.spawn(EntityType.COW, 2, 2, 2); befriend(owner, cow);
        var sheep = helper.spawn(EntityType.SHEEP, 3, 2, 2); befriend(owner, sheep);
        var pig = helper.spawn(EntityType.PIG, 4, 2, 2); befriend(owner, pig);
        var list = FriendshipService.list(owner);
        list.put(list.get("entity/" + cow.getUUID()).withFavorite(false));
        String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW).toString();
        list.put(new FriendshipEntry(null, type, "Cows", false, 1));
        UUID missing = UUID.randomUUID();
        list.put(new FriendshipEntry(missing, type, "Unloaded", false, 2));
        Player other = helper.makeMockPlayer(); befriend(other, cow);
        kill(cow); kill(sheep);
        helper.assertTrue(FriendshipService.clearDeceased(owner) == 1, "Only the non-favorite confirmed death is removed");
        helper.assertTrue(!list.contains("entity/" + cow.getUUID()) && list.contains("entity/" + sheep.getUUID()), "Favorite deceased friend survives cleanup");
        helper.assertTrue(list.contains("entity/" + pig.getUUID()) && list.contains("type/" + type) && list.contains("entity/" + missing), "Living friends, type rules and unconfirmed missing entities survive");
        helper.assertTrue(FriendshipService.list(other).contains("entity/" + cow.getUUID()) && FriendshipArchive.get(helper.getLevel().getServer()).find(cow.getUUID()).dead(), "Another owner and the shared archive remain intact");
        list.put(list.get("entity/" + sheep.getUUID()).withFavorite(false));
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(FriendshipService.clearDeceased(owner) == 0 && list.contains("entity/" + sheep.getUUID()), "Cleanup requires holding the tool");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyPassengersAreIgnoredBeforeDeserialization(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer();
        var cow = helper.spawn(EntityType.COW, 2, 2, 2); befriend(owner, cow); kill(cow); cow.discard();
        var rider = helper.spawn(EntityType.COW, 4, 2, 2);
        var riderPosition = rider.position();
        float riderHealth = rider.getHealth();
        var archive = FriendshipArchive.get(helper.getLevel().getServer());
        var old = archive.find(cow.getUUID());
        var snapshot = old.entity().copy();
        CompoundTag playerPassenger = new CompoundTag();
        playerPassenger.putString("id", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER).toString());
        CompoundTag livingPassenger = new CompoundTag();
        livingPassenger.putString("id", old.type());
        livingPassenger.putUUID("UUID", rider.getUUID());
        net.minecraft.nbt.ListTag passengers = new net.minecraft.nbt.ListTag();
        passengers.add(playerPassenger); passengers.add(livingPassenger); snapshot.put("Passengers", passengers);
        archive.record(new FriendshipArchive.DeathRecord(old.uuid(), old.type(), old.dimension(), old.name(), old.diedAt(), -1, snapshot, old.cause()));
        Entity restored = FriendshipLifecycle.revive(owner, old.type(), old.uuid());
        helper.assertTrue(restored.getPassengers().isEmpty() && !archive.find(old.uuid()).dead(), "Legacy passenger data cannot block or expand root-only revival");
        helper.assertTrue(helper.getLevel().getEntity(rider.getUUID()) == rider && rider.getHealth() == riderHealth
                && rider.position().equals(riderPosition) && !rider.isPassenger(), "Existing riders are neither duplicated, moved, damaged nor remounted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spontaneousRevivalRequiresALivingEntityWithTheSameUuid(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer();
        var type = EntityType.COW;
        var cow = helper.spawn(type, 2, 2, 2);
        befriend(owner, cow);
        UUID uuid = cow.getUUID();
        String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        var server = helper.getLevel().getServer();
        kill(cow);
        var original = FriendshipLifecycle.reconcileDeath(server, uuid);
        helper.assertTrue(original != null && original.dead(), "A loaded corpse remains confirmed dead");
        cow.discard();
        var unrelated = helper.spawn(type, 4, 2, 2);
        unrelated.setCustomName(cow.getName());
        helper.assertTrue(FriendshipLifecycle.reconcileDeath(server, uuid).dead(), "A missing entity remains dead despite another entity with the same type and name");
        var restored = type.create(helper.getLevel());
        if (restored == null) { helper.fail("Cannot create revival fixture"); return; }
        restored.setUUID(uuid);
        restored.setPos(cow.getX(), cow.getY(), cow.getZ());
        helper.assertTrue(helper.getLevel().addFreshEntity(restored), "The original UUID can return after corpse removal");
        helper.assertTrue(!FriendshipLifecycle.canRevive(owner, typeId, uuid), "The command reconciles spontaneous revival before offering another revival");
        var reconciled = FriendshipArchive.get(server).find(uuid);
        helper.assertTrue(!reconciled.dead() && reconciled.diedAt() == original.diedAt() && reconciled.revivedAt() >= 0, "Revival clears active death status and preserves death history");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void playerFriendshipsRequireIndividualIdentity(GameTestHelper helper) {
        ServerPlayer owner = serverPlayer(helper);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistries.PROOF_OF_FRIENDSHIP.get()));
        String playerType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER).toString();
        FriendshipService.action(owner, new yagen.waitmydawn.dont_hurt_them.network.FriendshipNetwork.Action("add_type", playerType));
        helper.assertTrue(!FriendshipService.list(owner).contains("type/" + playerType), "Server rejects a player-wide rule");
        Player friend = helper.makeMockPlayer();
        Player stranger = helper.makeMockPlayer();
        FriendshipService.toggle(owner, friend);
        helper.assertTrue(FriendshipService.list(owner).protects(friend.getUUID(), playerType), "Right-click adds a specific player UUID");
        var legacy = new yagen.waitmydawn.dont_hurt_them.core.FriendshipList();
        legacy.put(new FriendshipEntry(null, playerType, "Players", true, 1));
        legacy.put(new FriendshipEntry(friend.getUUID(), playerType, friend.getName().getString(), false, 2));
        String cowType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW).toString();
        legacy.put(new FriendshipEntry(null, cowType, "Cows", false, 3));
        var restored = FriendshipSerialization.read(FriendshipSerialization.write(legacy));
        helper.assertTrue(!restored.contains("type/" + playerType), "Legacy player-wide rules are discarded");
        helper.assertTrue(restored.protects(friend.getUUID(), playerType) && !restored.protects(stranger.getUUID(), playerType), "Player rules remain personal after persistence");
        helper.assertTrue(restored.protects(UUID.randomUUID(), cowType), "Unrelated entity types retain type-wide protection");
        FriendshipService.toggle(owner, friend);
        helper.assertTrue(!FriendshipService.list(owner).protects(friend.getUUID(), playerType), "Right-click again removes the specific player");
        helper.succeed();
    }

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
            catch (IllegalArgumentException expected) { helper.assertTrue(expected.getMessage().equals("spawn_blocked"), "Expected spawn_blocked, got " + expected.getMessage()); }
        } finally { MinecraftForge.EVENT_BUS.unregister(block); }
        helper.assertTrue(FriendshipLifecycle.canRevive(player, "minecraft:sheep", uuid) && helper.getLevel().getEntity(uuid) == null, "Failure retains the record without a partial entity");
        var retry = FriendshipLifecycle.revive(player, "minecraft:sheep", uuid);
        helper.assertTrue(retry.getUUID().equals(uuid) && helper.getLevel().getEntity(uuid) == retry
                && !FriendshipLifecycle.canRevive(player, "minecraft:sheep", uuid), "Removing the blocker allows the retained death record to be revived once");
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
