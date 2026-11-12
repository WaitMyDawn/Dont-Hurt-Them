package yagen.waitmydawn.dont_hurt_them.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yagen.waitmydawn.dont_hurt_them.server.TickSchedulerOwner;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.HazardLedger;

@Mixin(ServerLevel.class)
public abstract class ScheduledCauseMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void dontHurtThem$bindSchedulers(CallbackInfo callback) {
        ServerLevel level = (ServerLevel) (Object) this;
        // The queue itself has no dimension reference; bind its owning world once during construction.
        ((TickSchedulerOwner) level.getBlockTicks()).dontHurtThem$setLevel(level);
        ((TickSchedulerOwner) level.getFluidTicks()).dontHurtThem$setLevel(level);
    }

    @WrapMethod(method = "tickBlock")
    private void dontHurtThem$block(BlockPos pos, Block block, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        CauseContext.push(HazardLedger.get(level.getServer()).owner(level, pos));
        try { original.call(pos, block); }
        finally { CauseContext.pop(); }
    }

    @WrapMethod(method = "tickFluid")
    private void dontHurtThem$fluid(BlockPos pos, Fluid fluid, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        CauseContext.push(HazardLedger.get(level.getServer()).owner(level, pos));
        try { original.call(pos, fluid); }
        finally { CauseContext.pop(); }
    }
}
