package yagen.waitmydawn.dont_hurt_them.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipLifecycle;

@Mixin(Entity.class)
public abstract class EntityDeathMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void dontHurtThem$removed(Entity.RemovalReason reason, CallbackInfo callback) {
        FriendshipLifecycle.removed((Entity) (Object) this, reason);
    }
}
