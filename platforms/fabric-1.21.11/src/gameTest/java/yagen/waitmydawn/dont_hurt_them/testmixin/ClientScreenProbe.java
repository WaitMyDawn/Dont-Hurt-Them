package yagen.waitmydawn.dont_hurt_them.testmixin;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yagen.waitmydawn.dont_hurt_them.ClientRegression;

@Mixin(Screen.class)
public abstract class ClientScreenProbe {
    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("RETURN"))
    private void dontHurtThem$frame(CallbackInfo callback) {
        ClientRegression.frame((Screen) (Object) this);
    }
}
