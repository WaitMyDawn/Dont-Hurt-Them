package yagen.waitmydawn.dont_hurt_them.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;
import java.util.UUID;

@EventBusSubscriber(modid = DontHurtThem.MODID)
public final class FriendshipEvents {
    @SubscribeEvent
    public static void specific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getItemStack().is(ModRegistries.PROOF_OF_FRIENDSHIP.get())) return;
        interact(event.getEntity(), event.getTarget());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void general(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(ModRegistries.PROOF_OF_FRIENDSHIP.get())) return;
        interact(event.getEntity(), event.getTarget());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void interact(Player player, Entity target) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (player.isShiftKeyDown()) FriendshipService.sync(serverPlayer, true);
        else FriendshipService.toggle(serverPlayer, target);
    }

    @SubscribeEvent
    public static void block(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ModRegistries.PROOF_OF_FRIENDSHIP.get()) || !event.getEntity().isShiftKeyDown()) return;
        if (event.getEntity() instanceof ServerPlayer player) FriendshipService.sync(player, true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (DamageProtection.protects(event.getTarget(), event.getEntity().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (DamageProtection.shouldProtect(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void explosion(ExplosionEvent.Detonate event) {
        UUID cause = DamageProtection.owner(event.getExplosion().getDirectSourceEntity());
        if (cause == null) cause = CauseContext.current();
        UUID owner = cause;
        event.getAffectedEntities().removeIf(entity -> DamageProtection.protects(entity, owner));
    }

    @SubscribeEvent
    public static void projectile(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit
                && DamageProtection.skipsProjectileCollision(event.getProjectile(), hit.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void effect(MobEffectEvent.Applicable event) {
        if (!event.getEffectInstance().getEffect().value().isBeneficial()
                && DamageProtection.protects(event.getEntity(), DamageProtection.owner(event.getEffectSource()))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity() instanceof Player) return;
        UUID owner = DamageProtection.owner(event.getEntity());
        if (owner == null) owner = CauseContext.current();
        if (owner != null) event.getEntity().getPersistentData().putUUID(DamageProtection.OWNER, owner);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FriendshipService.changed(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FriendshipService.sync(player, false);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FriendshipService.sync(player, false);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0 && FriendshipService.holding(player)) {
            FriendshipService.sync(player, false);
        }
    }

    private FriendshipEvents() {}
}
