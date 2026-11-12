package yagen.waitmydawn.dont_hurt_them.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipLifecycle;

@Mixin(Entity.class)
public abstract class EntityProtectionMixin {
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void dontHurtThem$protect(DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if (DamageProtection.shouldProtect((Entity) (Object) this, source)) callback.setReturnValue(true);
        else FriendshipLifecycle.prepareNonLiving((Entity) (Object) this, source);
    }
}
