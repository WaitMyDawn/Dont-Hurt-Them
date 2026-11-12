package yagen.waitmydawn.dont_hurt_them.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yagen.waitmydawn.dont_hurt_them.client.ClientFriendships;
import yagen.waitmydawn.dont_hurt_them.client.EntityPortrait;

@Mixin(EntityRenderer.class)
public abstract class EntityNameMixin {
    // Amend the final decision so renderer subclass overrides cannot hide friendship names.
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;shouldShowName(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean dontHurtThem$name(boolean original, @Local(argsOnly = true) Entity entity) {
        return !EntityPortrait.rendering() && (original || ClientFriendships.showName(entity));
    }
}
