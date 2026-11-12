package yagen.waitmydawn.dont_hurt_them.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.HazardLedger;

@Mixin(LevelAccessor.class)
public interface TickScheduleMixin {
    @Inject(method = "createTick", at = @At("RETURN"))
    private <T> void dontHurtThem$inherit(CallbackInfoReturnable<ScheduledTick<T>> callback) {
        if (this instanceof ServerLevel level && CauseContext.current() != null) {
            BlockPos pos = callback.getReturnValue().pos();
            HazardLedger.get(level.getServer()).changed(level, pos, level.getBlockState(pos), CauseContext.current());
        }
    }
}
