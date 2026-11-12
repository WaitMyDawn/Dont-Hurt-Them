package yagen.waitmydawn.dont_hurt_them.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;

@Mixin(Projectile.class)
public abstract class ProjectileCollisionMixin {
    @Inject(method = "canHitEntity", at = @At("RETURN"), cancellable = true)
    private void dontHurtThem$skipFriend(Entity target, CallbackInfoReturnable<Boolean> callback) {
        // Filtering before ray selection preserves hits behind friends within the same movement step.
        if (callback.getReturnValue() && DamageProtection.skipsProjectileCollision((Projectile) (Object) this, target)) {
            callback.setReturnValue(false);
        }
    }
}
