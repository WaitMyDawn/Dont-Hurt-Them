package yagen.waitmydawn.dont_hurt_them.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipLifecycle;

@Mixin(LivingEntity.class)
public abstract class LivingDeathMixin {
    @Shadow protected boolean dead;
    @WrapMethod(method = "die")
    private void dontHurtThem$archive(DamageSource source, Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        boolean wasDead = dead;
        var snapshot = wasDead ? null : FriendshipLifecycle.capture(entity, source);
        original.call(source);
        // The flag is set only after the cancellable death hook, while the snapshot precedes inventory drops.
        if (!wasDead && dead) FriendshipLifecycle.commit(entity, snapshot);
    }
}
