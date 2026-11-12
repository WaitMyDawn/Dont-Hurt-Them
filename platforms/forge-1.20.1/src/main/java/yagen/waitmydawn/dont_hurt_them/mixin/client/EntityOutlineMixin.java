package yagen.waitmydawn.dont_hurt_them.mixin.client;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yagen.waitmydawn.dont_hurt_them.client.ClientFriendships;

@Mixin(Entity.class)
public abstract class EntityOutlineMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void dontHurtThem$glow(CallbackInfoReturnable<Boolean> callback) {
        if (ClientFriendships.color((Entity) (Object) this) != -1) callback.setReturnValue(true);
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void dontHurtThem$color(CallbackInfoReturnable<Integer> callback) {
        int color = ClientFriendships.color((Entity) (Object) this);
        if (color != -1) callback.setReturnValue(color);
    }
}
