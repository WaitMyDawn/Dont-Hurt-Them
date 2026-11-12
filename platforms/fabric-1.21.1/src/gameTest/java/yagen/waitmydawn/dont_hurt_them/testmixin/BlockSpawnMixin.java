package yagen.waitmydawn.dont_hurt_them.testmixin;

import yagen.waitmydawn.dont_hurt_them.SpawnRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class BlockSpawnMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void dontHurtThem$block(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (SpawnRules.BLOCKED.contains(entity.getUUID())) callback.setReturnValue(false);
    }
}
