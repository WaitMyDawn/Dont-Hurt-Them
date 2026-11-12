package yagen.waitmydawn.dont_hurt_them;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipEntry;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipSerialization;
import yagen.waitmydawn.dont_hurt_them.data.OfflineFriendships;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;
import yagen.waitmydawn.dont_hurt_them.server.HazardLedger;

@GameTestHolder(DontHurtThem.MODID)
@PrefixGameTestTemplate(false)
public final class FriendshipGameTests {
    private static Player owner(GameTestHelper helper, Entity target, boolean type) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var list = FriendshipService.list(player);
        list.put(new FriendshipEntry(type ? null : target.getUUID(),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString(), "Friend", false));
        OfflineFriendships.get(helper.getLevel().getServer()).update(player.getUUID(), list);
        return player;
    }

    @GameTest(template = "empty")
    public static void personalProtectionAllowsStrangersAndNaturalDamage(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        float health = cow.getHealth();
        cow.hurt(cow.damageSources().playerAttack(owner), 4);
        helper.assertTrue(cow.getHealth() == health, "The owner's direct hit must be blocked");
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        cow.hurt(cow.damageSources().playerAttack(stranger), 3);
        helper.assertTrue(cow.getHealth() < health, "Another player's damage must remain possible");
        cow.invulnerableTime = 0;
        float after = cow.getHealth();
        cow.hurt(cow.damageSources().generic(), 1);
        helper.assertTrue(cow.getHealth() < after, "Unattributed natural damage must remain possible");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void typeRulesProtectDifferentIndividuals(GameTestHelper helper) {
        Cow first = helper.spawn(EntityType.COW, 2, 2, 2), second = helper.spawn(EntityType.COW, 3, 2, 2);
        Player owner = owner(helper, first, true);
        float health = second.getHealth();
        second.hurt(second.damageSources().playerAttack(owner), 4);
        helper.assertTrue(second.getHealth() == health, "Type protection must cover another UUID of the exact type");
        var sheep = helper.spawn(EntityType.SHEEP, 4, 2, 2);
        helper.assertTrue(!DamageProtection.shouldProtect(sheep, sheep.damageSources().playerAttack(owner)), "Other registry types remain unprotected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void burningArrowImpactCancellationPreservesProjectile(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        Arrow arrow = new Arrow(helper.getLevel(), owner, new ItemStack(Items.ARROW), null);
        arrow.igniteForSeconds(20);
        arrow.setDeltaMovement(2, 0, 0);
        var impact = new ProjectileImpactEvent(arrow, new EntityHitResult(cow));
        NeoForge.EVENT_BUS.post(impact);
        helper.assertTrue(impact.isCanceled() && !arrow.isRemoved() && !cow.isOnFire(), "Cancelled friend impacts must preserve the projectile");
        helper.assertTrue(arrow.getDeltaMovement().x == 2, "Cancelling an impact must preserve velocity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tntAndTamedEntitiesRetainTheirOwner(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        PrimedTnt tnt = new PrimedTnt(helper.getLevel(), cow.getX(), cow.getY(), cow.getZ(), owner);
        helper.assertTrue(DamageProtection.shouldProtect(cow, cow.damageSources().explosion(tnt, owner)), "TNT ownership");
        Wolf wolf = helper.spawn(EntityType.WOLF, 4, 2, 2);
        wolf.setOwnerUUID(owner.getUUID());
        helper.assertTrue(DamageProtection.shouldProtect(cow, cow.damageSources().mobAttack(wolf)), "Tamed attacker ownership");
        tnt.getPersistentData().putUUID(DamageProtection.OWNER, owner.getUUID());
        helper.assertTrue(DamageProtection.owner(tnt).equals(owner.getUUID()), "Persistent provenance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void harmfulEffectsAreBlockedButHealingRemainsPossible(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        cow.addEffect(new MobEffectInstance(MobEffects.POISON, 200), owner);
        helper.assertTrue(!cow.hasEffect(MobEffects.POISON), "Player poison must be blocked before losing its source");
        cow.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200), owner);
        helper.assertTrue(cow.hasEffect(MobEffects.REGENERATION), "Beneficial effects remain possible");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void attachmentSurvivesNbtRoundTripWithoutTheItem(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        var list = FriendshipService.list(owner);
        String key = list.entries().iterator().next().key();
        list.toggleFavorite(key);
        CompoundTag save = owner.saveWithoutId(new CompoundTag());
        Player restored = helper.makeMockPlayer(GameType.SURVIVAL);
        restored.load(save);
        helper.assertTrue(restored.getData(ModRegistries.FRIENDSHIPS).contains(key), "Player attachment persists through entity NBT");
        helper.assertTrue(restored.getData(ModRegistries.FRIENDSHIPS).sorted().get(0).favorite(), "Favorite persists");
        helper.assertTrue(restored.getMainHandItem().isEmpty(), "Friendship storage does not depend on holding the tool");
        helper.assertTrue(FriendshipSerialization.read(FriendshipSerialization.write(list)).protects(cow.getUUID(), "minecraft:cow"), "Serializer round trip");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void playerPlacedHazardsKeepProvenance(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        BlockPos pos = cow.blockPosition();
        CauseContext.push(owner.getUUID());
        try { helper.getLevel().setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState()); }
        finally { CauseContext.pop(); }
        helper.assertTrue(owner.getUUID().equals(HazardLedger.get(helper.getLevel().getServer()).owner(helper.getLevel(), pos)), "Block mixin records the player cause");
        float health = cow.getHealth();
        cow.hurt(cow.damageSources().lava(), 4);
        helper.assertTrue(cow.getHealth() == health, "Player-placed lava must not harm a friend");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(HazardLedger.get(helper.getLevel().getServer()).owner(helper.getLevel(), pos) == null, "Removed blocks release their attribution");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nonLivingEntitiesUseTheSameProtection(GameTestHelper helper) {
        var frame = helper.spawn(EntityType.ITEM_FRAME, 2, 2, 2);
        Player owner = owner(helper, frame, false);
        helper.assertTrue(frame.isInvulnerableTo(frame.damageSources().playerAttack(owner)), "Entity invulnerability covers non-living entities");
        helper.assertTrue(!frame.isInvulnerableTo(frame.damageSources().generic()), "Natural damage remains available");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void fluidPropagationRetainsTheOriginalPlayer(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 6, 2, 6);
        Player owner = owner(helper, cow, false);
        BlockPos source = helper.absolutePos(new BlockPos(2, 3, 2));
        CauseContext.push(owner.getUUID());
        try { helper.getLevel().setBlockAndUpdate(source, Blocks.LAVA.defaultBlockState()); }
        finally { CauseContext.pop(); }
        helper.succeedWhen(() -> {
            BlockPos below = source.below();
            helper.assertTrue(helper.getLevel().getFluidState(below).is(net.minecraft.tags.FluidTags.LAVA), "Lava must actually flow before checking provenance");
            helper.assertTrue(owner.getUUID().equals(HazardLedger.get(helper.getLevel().getServer()).owner(helper.getLevel(), below)), "Scheduled fluid propagation retains ownership");
        });
    }

    @GameTest(template = "empty")
    public static void explosionExcludesOnlyTheOwnersFriends(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        PrimedTnt tnt = new PrimedTnt(helper.getLevel(), cow.getX(), cow.getY(), cow.getZ(), owner);
        var explosion = new net.minecraft.world.level.Explosion(helper.getLevel(), tnt, cow.getX(), cow.getY(), cow.getZ(), 3, false,
                net.minecraft.world.level.Explosion.BlockInteraction.KEEP);
        java.util.List<Entity> affected = new java.util.ArrayList<>(java.util.List.of(cow, stranger));
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.ExplosionEvent.Detonate(helper.getLevel(), explosion, affected));
        helper.assertTrue(!affected.contains(cow) && affected.contains(stranger), "Friends are removed from both damage and knockback processing");
        helper.succeed();
    }
}
