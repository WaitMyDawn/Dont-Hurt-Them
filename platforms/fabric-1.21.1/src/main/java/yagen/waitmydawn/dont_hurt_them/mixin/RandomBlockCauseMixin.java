package yagen.waitmydawn.dont_hurt_them.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;
import yagen.waitmydawn.dont_hurt_them.server.HazardLedger;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class RandomBlockCauseMixin {
    @WrapMethod(method = "randomTick")
    private void dontHurtThem$random(ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        CauseContext.push(HazardLedger.get(level.getServer()).owner(level, pos));
        try { original.call(level, pos, random); }
        finally { CauseContext.pop(); }
    }
}
