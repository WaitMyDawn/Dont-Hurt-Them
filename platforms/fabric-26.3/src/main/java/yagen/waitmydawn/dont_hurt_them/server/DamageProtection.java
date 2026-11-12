package yagen.waitmydawn.dont_hurt_them.server;

import yagen.waitmydawn.dont_hurt_them.data.NbtCompat;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.FluidTags;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import yagen.waitmydawn.dont_hurt_them.core.FriendshipList;
import yagen.waitmydawn.dont_hurt_them.data.OfflineFriendships;

public final class DamageProtection {
    public static final String OWNER = "dont_hurt_them_owner";
    private static final String FIRE_OWNER = "dont_hurt_them_fire_owner";
    private static final String FIRE_UNTIL = "dont_hurt_them_fire_until";

    // Client prediction uses only the local shooter's synchronized rules; server decisions remain authoritative.
    public static BiPredicate<Entity, UUID> clientProjectileProtection = (target, owner) -> false;

    public static boolean skipsProjectileCollision(Projectile projectile, Entity target) {
        UUID owner = owner(projectile);
        if (owner == null) return false;
        return target.level().isClientSide() ? clientProjectileProtection.test(target, owner) : protects(target, owner);
    }

    public static UUID owner(Entity entity) { return owner(entity, new HashSet<>()); }

    private static UUID owner(Entity entity, Set<UUID> visited) {
        if (entity == null || !visited.add(entity.getUUID())) return null;
        if (entity instanceof Player) return entity.getUUID();
        if (entity instanceof OwnableEntity ownable && ownable.getOwnerReference() != null) return ownable.getOwnerReference().getUUID();
        Entity parent = null;
        if (entity instanceof Projectile projectile) parent = projectile.getOwner();
        else if (entity instanceof PrimedTnt tnt) parent = tnt.getOwner();
        else if (entity instanceof AreaEffectCloud cloud) parent = cloud.getOwner();
        else if (entity instanceof EvokerFangs fangs) parent = fangs.getOwner();
        UUID owner = owner(parent, visited);
        if (owner != null) return owner;
        return NbtCompat.hasUuid(ModRegistries.data(entity), OWNER) ? NbtCompat.uuid(ModRegistries.data(entity), OWNER) : null;
    }

    public static boolean protects(Entity target, UUID owner) {
        if (owner == null || !(target.level() instanceof ServerLevel level)) return false;
        Player player = level.getServer().getPlayerList().getPlayer(owner);
        FriendshipList list = player != null ? FriendshipService.list(player) : OfflineFriendships.get(level.getServer()).find(owner);
        return list != null && list.protects(target.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
    }

    public static boolean shouldProtect(Entity target, DamageSource source) {
        if (target.level().isClientSide()) return false;
        UUID cause = owner(source.getEntity());
        if (cause == null) cause = owner(source.getDirectEntity());
        if (cause != null) return protects(target, cause);
        if (CauseContext.current() != null && protects(target, CauseContext.current())) return true;
        return protects(target, environmentalOwner(target, source));
    }

    private static UUID environmentalOwner(Entity target, DamageSource source) {
        if (!(target.level() instanceof ServerLevel level)) return null;
        boolean fire = source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA);
        boolean contact = fire || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL);
        if (!contact) return null;
        var box = target.getBoundingBox().inflate(0.05);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - 0.2, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = level.getBlockState(pos);
            boolean matches = fire ? (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CAMPFIRE)
                    || state.is(Blocks.SOUL_CAMPFIRE) || state.getFluidState().is(FluidTags.LAVA))
                    : source.is(DamageTypes.CACTUS) ? state.is(Blocks.CACTUS)
                    : source.is(DamageTypes.SWEET_BERRY_BUSH) ? state.is(Blocks.SWEET_BERRY_BUSH)
                    : source.is(DamageTypes.HOT_FLOOR) ? state.is(Blocks.MAGMA_BLOCK)
                    : source.is(DamageTypes.DROWN) ? state.getFluidState().is(FluidTags.WATER)
                    : source.is(DamageTypes.IN_WALL) ? !state.getCollisionShape(level, pos).isEmpty() : state.is(Blocks.WITHER_ROSE);
            if (!matches) continue;
            UUID cause = HazardLedger.get(level.getServer()).owner(level, pos);
            if (cause != null) {
                if (fire) {
                    NbtCompat.putUuid(ModRegistries.data(target), FIRE_OWNER, cause);
                    ModRegistries.data(target).putLong(FIRE_UNTIL, level.getGameTime() + Math.max(200, target.getRemainingFireTicks()));
                }
                return cause;
            }
        }
        if (source.is(DamageTypes.ON_FIRE) && NbtCompat.hasUuid(ModRegistries.data(target), FIRE_OWNER)
                && level.getGameTime() <= ModRegistries.data(target).getLongOr(FIRE_UNTIL, 0)) {
            return NbtCompat.uuid(ModRegistries.data(target), FIRE_OWNER);
        }
        return null;
    }

    private DamageProtection() {}
}
