package yagen.waitmydawn.dont_hurt_them;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import yagen.waitmydawn.dont_hurt_them.data.NbtCompat;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipEntry;
import yagen.waitmydawn.dont_hurt_them.data.OfflineFriendships;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;

public final class ProjectileGameTests {
    private static Player shooter(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(0.5, 2, 3.5)));
        return player;
    }

    private static <T extends Mob> T target(GameTestHelper helper, EntityType<T> type, int x) {
        T entity = helper.spawn(type, x, 2, 3);
        entity.setNoAi(true);
        entity.setNoGravity(true);
        return entity;
    }

    private static void befriend(Player player, Entity entity, boolean type) {
        var list = FriendshipService.list(player);
        list.put(new FriendshipEntry(type ? null : entity.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
                entity.getName().getString(), false));
        OfflineFriendships.get(player.level().getServer()).update(player.getUUID(), list);
    }

    private static <T extends Projectile> T launch(GameTestHelper helper, T projectile, double speed) {
        projectile.setPos(helper.absoluteVec(new Vec3(1, 2.5, 3.5)));
        projectile.setNoGravity(true);
        projectile.setDeltaMovement(speed, 0, 0);
        helper.getLevel().addFreshEntity(projectile);
        return projectile;
    }

    private static Arrow arrow(GameTestHelper helper, Player player) {
        Arrow arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), null);
        arrow.setBaseDamage(0.25);
        return arrow;
    }

    private static void assertUntouched(GameTestHelper helper, Mob friend) {
        helper.assertTrue(friend.getHealth() == friend.getMaxHealth() && !friend.isOnFire() && friend.getArrowCount() == 0,
                "Friends must retain health and avoid ignition and embedded arrows");
        helper.assertTrue(friend.getDeltaMovement().lengthSqr() == 0, "Friends must not receive projectile knockback");
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void fastBurningArrowSkipsMultipleFriendsAndHitsRearTarget(GameTestHelper helper) {
        Player player = shooter(helper);
        var first = target(helper, EntityTypes.COW, 2);
        var second = target(helper, EntityTypes.SHEEP, 4);
        var rear = target(helper, EntityTypes.PIG, 6);
        befriend(player, first, false);
        befriend(player, second, false);
        Arrow arrow = launch(helper, arrow(helper, player), 7);
        arrow.igniteForSeconds(20);
        // A single real movement step must resolve the rear hit, rather than skipping the entire ray after cancellation.
        arrow.tick();
        assertUntouched(helper, first);
        assertUntouched(helper, second);
        helper.assertTrue(rear.getHealth() < rear.getMaxHealth() && rear.isOnFire(), "The same movement step must hit the rear target");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void piercingArrowDoesNotSpendPiercingOnFriends(GameTestHelper helper) {
        Player player = shooter(helper);
        var first = target(helper, EntityTypes.COW, 2);
        var second = target(helper, EntityTypes.COW, 3);
        var third = target(helper, EntityTypes.COW, 4);
        var rear = target(helper, EntityTypes.SHEEP, 5);
        var last = target(helper, EntityTypes.PIG, 6);
        befriend(player, first, true);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PIERCING), 1);
        Arrow arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), crossbow);
        arrow.setBaseDamage(0.25);
        launch(helper, arrow, 6.5);
        arrow.tick();
        assertUntouched(helper, first);
        assertUntouched(helper, second);
        assertUntouched(helper, third);
        helper.assertTrue(rear.getHealth() < rear.getMaxHealth() && last.getHealth() < last.getMaxHealth(),
                "The original piercing budget must remain available for non-friends");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void tridentSkipsFriendAndHitsRearTarget(GameTestHelper helper) {
        Player player = shooter(helper);
        var friend = target(helper, EntityTypes.COW, 2);
        var rear = target(helper, EntityTypes.SHEEP, 6);
        befriend(player, friend, false);
        ThrownTrident trident = launch(helper, new ThrownTrident(helper.getLevel(), player, new ItemStack(Items.TRIDENT)), 7);
        trident.tick();
        assertUntouched(helper, friend);
        helper.assertTrue(rear.getHealth() < rear.getMaxHealth() && !trident.isRemoved(), "Tridents must hit the rear target and remain recoverable");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void arrowsStillStopAtBlocksAndCanBePickedUp(GameTestHelper helper) {
        Player player = shooter(helper);
        var friend = target(helper, EntityTypes.COW, 2);
        var rear = target(helper, EntityTypes.SHEEP, 6);
        befriend(player, friend, false);
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.STONE);
        Arrow arrow = launch(helper, arrow(helper, player), 7);
        arrow.tick();
        assertUntouched(helper, friend);
        helper.assertTrue(!arrow.isRemoved() && NbtCompat.save(arrow).getBooleanOr("inGround", false), "The block behind a friend must stop the arrow");
        helper.assertTrue(rear.getHealth() == rear.getMaxHealth(), "Friends must not make projectiles pass through solid blocks");
        for (int tick = 0; tick < 8; tick++) arrow.tick();
        player.setPos(arrow.position());
        arrow.playerTouch(player);
        helper.assertTrue(arrow.isRemoved() && player.getInventory().countItem(Items.ARROW) == 1, "Normal survival arrow pickup must still work");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void tridentKeepsOriginalItemAfterFriendAndBlock(GameTestHelper helper) {
        Player player = shooter(helper);
        var friend = target(helper, EntityTypes.COW, 2);
        befriend(player, friend, false);
        ItemStack stack = new ItemStack(Items.TRIDENT);
        stack.setDamageValue(17);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Recovered spear"));
        helper.setBlock(new BlockPos(6, 2, 3), Blocks.STONE);
        ThrownTrident trident = launch(helper, new ThrownTrident(helper.getLevel(), player, stack), 7);
        trident.tick();
        assertUntouched(helper, friend);
        helper.assertTrue(!trident.isRemoved(), "Crossing a friend must not delete the trident");
        for (int tick = 0; tick < 8; tick++) trident.tick();
        player.setPos(trident.position());
        trident.playerTouch(player);
        helper.assertTrue(trident.isRemoved() && player.getInventory().countItem(Items.TRIDENT) == 1, "The original trident must be picked up once");
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(item -> ItemStack.isSameItemSameComponents(item, stack)),
                "Trident pickup must preserve damage and custom components");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void loyaltyTridentReturnsAfterPassingFriend(GameTestHelper helper) {
        Player player = shooter(helper);
        var friend = target(helper, EntityTypes.COW, 2);
        befriend(player, friend, false);
        ItemStack stack = new ItemStack(Items.TRIDENT);
        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY), 3);
        helper.setBlock(new BlockPos(6, 2, 3), Blocks.STONE);
        ThrownTrident trident = launch(helper, new ThrownTrident(helper.getLevel(), player, stack), 3);
        trident.tick();
        assertUntouched(helper, friend);
        helper.assertTrue(!trident.isRemoved() && !NbtCompat.save(trident).getBooleanOr("DealtDamage", false) && trident.getDeltaMovement().x > 0,
                "Passing a friend must not trigger an early hit or loyalty return");
        boolean returning = false;
        for (int tick = 0; tick < 100 && !trident.isRemoved(); tick++) {
            trident.tick();
            if (trident.isNoPhysics()) returning = true;
            if (returning && trident.getBoundingBox().intersects(player.getBoundingBox())) trident.playerTouch(player);
        }
        helper.assertTrue(returning && trident.isRemoved() && player.getInventory().countItem(Items.TRIDENT) == 1,
                "Loyalty must return and recover the trident after a real block hit; returning=" + returning
                        + ", removed=" + trident.isRemoved() + ", recovered=" + player.getInventory().countItem(Items.TRIDENT));
        helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(item -> ItemStack.isSameItemSameComponents(item, stack)),
                "Returning tridents must retain enchantments");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void otherPlayersAndUnownedProjectilesStillHitFriends(GameTestHelper helper) {
        Player player = shooter(helper);
        Player stranger = shooter(helper);
        var friend = target(helper, EntityTypes.COW, 2);
        befriend(player, friend, false);
        Arrow strangerArrow = launch(helper, arrow(helper, stranger), 3);
        strangerArrow.tick();
        helper.assertTrue(friend.getHealth() < friend.getMaxHealth(), "Another player's projectile must still collide");
        friend.setHealth(friend.getMaxHealth());
        friend.damageCooldownTime = 0;
        Arrow unownedArrow = new Arrow(helper.getLevel(), 0, 0, 0, new ItemStack(Items.ARROW), null);
        unownedArrow.setBaseDamage(0.25);
        launch(helper, unownedArrow, 3).tick();
        helper.assertTrue(friend.getHealth() < friend.getMaxHealth(), "Unowned projectile collisions must remain unchanged");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void snowballSkipsFriendAndHitsRearBlaze(GameTestHelper helper) {
        Player player = shooter(helper);
        var friend = target(helper, EntityTypes.COW, 2);
        var rear = target(helper, EntityTypes.BLAZE, 6);
        befriend(player, friend, false);
        Snowball snowball = launch(helper, new Snowball(helper.getLevel(), player, new ItemStack(Items.SNOWBALL)), 7);
        snowball.tick();
        assertUntouched(helper, friend);
        helper.assertTrue(rear.getHealth() < rear.getMaxHealth(), "The shared projectile filter must preserve a throwable's rear hit");
        helper.succeed();
    }
}
