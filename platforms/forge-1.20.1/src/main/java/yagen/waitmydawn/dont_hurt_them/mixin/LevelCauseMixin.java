package yagen.waitmydawn.dont_hurt_them.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.HazardLedger;

@Mixin(Level.class)
public abstract class LevelCauseMixin {
    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean dontHurtThem$record(BlockPos pos, BlockState state, int flags, int recursion, Operation<Boolean> original) {
        boolean changed = original.call(pos, state, flags, recursion);
        if (changed && (Object) this instanceof ServerLevel level) {
            HazardLedger.get(level.getServer()).changed(level, pos, state, CauseContext.current());
        }
        return changed;
    }
}
