package yagen.waitmydawn.dont_hurt_them.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;

@Mixin(Projectile.class)
public abstract class ProjectileImpactMixin {
    @Inject(method = "hitTargetOrDeflectSelf", at = @At("HEAD"), cancellable = true)
    private void dontHurtThem$impact(HitResult hit, CallbackInfoReturnable<ProjectileDeflection> callback) {
        if (hit instanceof EntityHitResult entityHit && DamageProtection.skipsProjectileCollision((Projectile) (Object) this, entityHit.getEntity())) {
            callback.setReturnValue(ProjectileDeflection.NONE);
        }
    }
}
