package yagen.waitmydawn.dont_hurt_them.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yagen.waitmydawn.dont_hurt_them.server.DamageProtection;

@Mixin(LivingEntity.class)
public abstract class LivingEffectMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void dontHurtThem$effect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> callback) {
        // Forge 1.20.1's applicability event does not carry the effect source.
        if (!effect.getEffect().isBeneficial() && DamageProtection.protects((LivingEntity) (Object) this, DamageProtection.owner(source))) {
            callback.setReturnValue(false);
        }
    }
}
