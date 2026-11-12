package yagen.waitmydawn.dont_hurt_them;

import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import yagen.waitmydawn.dont_hurt_them.data.NbtCompat;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipEntry;
import yagen.waitmydawn.dont_hurt_them.data.FriendshipSerialization;
import yagen.waitmydawn.dont_hurt_them.data.OfflineFriendships;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;
import yagen.waitmydawn.dont_hurt_them.server.HazardLedger;

public final class FriendshipGameTests {
    private static Player owner(GameTestHelper helper, Entity target, boolean type) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var list = FriendshipService.list(player);
        list.put(new FriendshipEntry(type ? null : target.getUUID(),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString(), "Friend", false));
        OfflineFriendships.get(helper.getLevel().getServer()).update(player.getUUID(), list);
        return player;
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void personalProtectionAllowsStrangersAndNaturalDamage(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        float health = cow.getHealth();
        cow.hurtServer((net.minecraft.server.level.ServerLevel) cow.level(), cow.damageSources().playerAttack(owner), 4);
        helper.assertTrue(cow.getHealth() == health, "The owner's direct hit must be blocked");
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        cow.hurtServer((net.minecraft.server.level.ServerLevel) cow.level(), cow.damageSources().playerAttack(stranger), 3);
        helper.assertTrue(cow.getHealth() < health, "Another player's damage must remain possible");
        cow.damageCooldownTime = 0;
        float after = cow.getHealth();
        cow.hurtServer((net.minecraft.server.level.ServerLevel) cow.level(), cow.damageSources().generic(), 1);
        helper.assertTrue(cow.getHealth() < after, "Unattributed natural damage must remain possible");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void typeRulesProtectDifferentIndividuals(GameTestHelper helper) {
        Cow first = helper.spawn(EntityTypes.COW, 2, 2, 2), second = helper.spawn(EntityTypes.COW, 3, 2, 2);
        Player owner = owner(helper, first, true);
        float health = second.getHealth();
        second.hurtServer((net.minecraft.server.level.ServerLevel) second.level(), second.damageSources().playerAttack(owner), 4);
        helper.assertTrue(second.getHealth() == health, "Type protection must cover another UUID of the exact type");
        var sheep = helper.spawn(EntityTypes.SHEEP, 4, 2, 2);
        helper.assertTrue(!DamageProtection.shouldProtect(sheep, sheep.damageSources().playerAttack(owner)), "Other registry types remain unprotected");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void burningArrowImpactCancellationPreservesProjectile(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        class ImpactArrow extends Arrow {
            ImpactArrow(Player shooter) { super(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null); }
            void impact() { hitTargetOrDeflectSelf(new EntityHitResult(cow)); }
        }
        ImpactArrow arrow = new ImpactArrow(owner);
        arrow.igniteForSeconds(20); arrow.setDeltaMovement(2, 0, 0);
        arrow.impact();
        helper.assertTrue(!arrow.isRemoved() && !cow.isOnFire() && cow.getHealth() == cow.getMaxHealth(), "Fallback must retain the projectile and skip friend impact effects");
        helper.assertTrue(arrow.getDeltaMovement().x == 2, "Fallback must preserve velocity");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void tntAndTamedEntitiesRetainTheirOwner(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        PrimedTnt tnt = new PrimedTnt(helper.getLevel(), cow.getX(), cow.getY(), cow.getZ(), owner);
        helper.assertTrue(DamageProtection.shouldProtect(cow, cow.damageSources().explosion(tnt, owner)), "TNT ownership");
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 4, 2, 2);
        wolf.setOwner(owner);
        helper.assertTrue(DamageProtection.shouldProtect(cow, cow.damageSources().mobAttack(wolf)), "Tamed attacker ownership");
        NbtCompat.putUuid(ModRegistries.data(tnt), DamageProtection.OWNER, owner.getUUID());
        helper.assertTrue(DamageProtection.owner(tnt).equals(owner.getUUID()), "Persistent provenance");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void harmfulEffectsAreBlockedButHealingRemainsPossible(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        cow.addEffect(new MobEffectInstance(MobEffects.POISON, 200), owner);
        helper.assertTrue(!cow.hasEffect(MobEffects.POISON), "Player poison must be blocked before losing its source");
        cow.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200), owner);
        helper.assertTrue(cow.hasEffect(MobEffects.REGENERATION), "Beneficial effects remain possible");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void attachmentSurvivesNbtRoundTripWithoutTheItem(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        var list = FriendshipService.list(owner);
        String key = list.entries().iterator().next().key();
        list.toggleFavorite(key);
        CompoundTag save = NbtCompat.save(owner);
        Player restored = helper.makeMockPlayer(GameType.SURVIVAL);
        restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, restored.registryAccess(), save));
        helper.assertTrue(FriendshipService.list(restored).contains(key), "Player attachment persists through entity NBT");
        helper.assertTrue(FriendshipService.list(restored).sorted().get(0).favorite(), "Favorite persists");
        helper.assertTrue(restored.getMainHandItem().isEmpty(), "Friendship storage does not depend on holding the tool");
        helper.assertTrue(FriendshipSerialization.read(FriendshipSerialization.write(list)).protects(cow.getUUID(), "minecraft:cow"), "Serializer round trip");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void playerPlacedHazardsKeepProvenance(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        BlockPos pos = cow.blockPosition();
        CauseContext.push(owner.getUUID());
        try { helper.getLevel().setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState()); }
        finally { CauseContext.pop(); }
        helper.assertTrue(owner.getUUID().equals(HazardLedger.get(helper.getLevel().getServer()).owner(helper.getLevel(), pos)), "Block mixin records the player cause");
        float health = cow.getHealth();
        cow.hurtServer((net.minecraft.server.level.ServerLevel) cow.level(), cow.damageSources().lava(), 4);
        helper.assertTrue(cow.getHealth() == health, "Player-placed lava must not harm a friend");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(HazardLedger.get(helper.getLevel().getServer()).owner(helper.getLevel(), pos) == null, "Removed blocks release their attribution");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void nonLivingEntitiesUseTheSameProtection(GameTestHelper helper) {
        var frame = helper.spawn(EntityTypes.ITEM_FRAME, 2, 2, 2);
        Player owner = owner(helper, frame, false);
        helper.assertTrue(!frame.hurtServer(helper.getLevel(), frame.damageSources().playerAttack(owner), 1), "Entity invulnerability covers non-living entities");
        helper.assertTrue(frame.hurtServer(helper.getLevel(), frame.damageSources().generic(), 1), "Natural damage remains available");
        helper.succeed();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 120)
    public void fluidPropagationRetainsTheOriginalPlayer(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, 6, 2, 6);
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

    @GameTest(structure = "fabric-gametest-api-v1:empty")
    public void explosionExcludesOnlyTheOwnersFriends(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Player owner = owner(helper, cow, false);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        PrimedTnt tnt = new PrimedTnt(helper.getLevel(), cow.getX(), cow.getY(), cow.getZ(), owner);
        var explosion = new net.minecraft.world.level.ServerExplosion(helper.getLevel(), tnt, null, null, cow.position(), 3, false,
                net.minecraft.world.level.Explosion.BlockInteraction.KEEP);
        var rear = helper.spawn(EntityTypes.SHEEP, 3, 2, 2);
        explosion.explode();
        helper.assertTrue(cow.getHealth() == cow.getMaxHealth() && cow.getDeltaMovement().lengthSqr() == 0,
                "Friends must be removed from explosion damage and knockback");
        helper.assertTrue(rear.getHealth() < rear.getMaxHealth(), "Non-friends remain affected by the explosion");
        helper.succeed();
    }
}
