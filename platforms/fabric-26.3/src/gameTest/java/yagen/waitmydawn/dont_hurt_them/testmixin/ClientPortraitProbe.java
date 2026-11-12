package yagen.waitmydawn.dont_hurt_them.testmixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yagen.waitmydawn.dont_hurt_them.ClientRegression;
import yagen.waitmydawn.dont_hurt_them.client.EntityPortrait;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;

@Mixin(value = EntityPortrait.class, remap = false)
public abstract class ClientPortraitProbe {
    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private static void dontHurtThem$portrait(GuiGraphicsExtractor graphics, Entity entity, int x, int y, int width,
                                              int height, int mouseX, int mouseY, CallbackInfo callback) {
        ClientRegression.portrait(entity);
    }
}
