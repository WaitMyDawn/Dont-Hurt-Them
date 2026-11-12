package yagen.waitmydawn.dont_hurt_them.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.HazardLedger;

@Mixin(FluidState.class)
public abstract class RandomFluidCauseMixin {
    @WrapMethod(method = "randomTick")
    private void dontHurtThem$random(Level level, BlockPos pos, RandomSource random, Operation<Void> original) {
        var owner = level instanceof ServerLevel server ? HazardLedger.get(server.getServer()).owner(server, pos) : null;
        CauseContext.push(owner);
        try { original.call(level, pos, random); }
        finally { CauseContext.pop(); }
    }
}
