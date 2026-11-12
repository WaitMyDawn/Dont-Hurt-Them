package yagen.waitmydawn.dont_hurt_them.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.HazardLedger;
import yagen.waitmydawn.dont_hurt_them.server.TickSchedulerOwner;

@Mixin(LevelTicks.class)
public abstract class TickScheduleMixin<T> implements TickSchedulerOwner {
    @Unique private ServerLevel dontHurtThem$level;
    public void dontHurtThem$setLevel(ServerLevel level) { dontHurtThem$level = level; }

    @Inject(method = "schedule", at = @At("HEAD"))
    private void dontHurtThem$inherit(ScheduledTick<T> tick, CallbackInfo callback) {
        if (dontHurtThem$level != null && CauseContext.current() != null) {
            HazardLedger.get(dontHurtThem$level.getServer()).changed(dontHurtThem$level, tick.pos(),
                    dontHurtThem$level.getBlockState(tick.pos()), CauseContext.current());
        }
    }
}
