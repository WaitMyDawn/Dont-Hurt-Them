package yagen.waitmydawn.dont_hurt_them.testmixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yagen.waitmydawn.dont_hurt_them.ClientRegression;

@Mixin(EntityRenderer.class)
public abstract class ClientNameProbe {
    @Inject(method = "renderNameTag", at = @At("HEAD"))
    private void dontHurtThem$name(Entity entity, Component label, PoseStack pose, MultiBufferSource buffers, int light, float tick, CallbackInfo callback) {
        ClientRegression.name(entity);
    }
}
