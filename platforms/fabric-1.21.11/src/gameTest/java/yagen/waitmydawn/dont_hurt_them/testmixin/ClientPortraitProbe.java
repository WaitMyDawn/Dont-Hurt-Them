package yagen.waitmydawn.dont_hurt_them.testmixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yagen.waitmydawn.dont_hurt_them.ClientRegression;
import yagen.waitmydawn.dont_hurt_them.client.EntityPortrait;

@Mixin(value = EntityPortrait.class, remap = false)
public abstract class ClientPortraitProbe {
    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private static void dontHurtThem$portrait(CallbackInfo callback) { ClientRegression.portrait(); }
}
