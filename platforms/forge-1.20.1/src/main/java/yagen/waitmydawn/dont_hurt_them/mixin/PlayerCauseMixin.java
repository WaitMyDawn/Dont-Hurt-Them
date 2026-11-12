package yagen.waitmydawn.dont_hurt_them.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import yagen.waitmydawn.dont_hurt_them.server.CauseContext;

@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerCauseMixin {
    @Shadow @Final protected ServerPlayer player;

    @WrapMethod(method = "useItemOn")
    private InteractionResult dontHurtThem$useOn(ServerPlayer player, Level level, ItemStack stack,
            InteractionHand hand, BlockHitResult hit, Operation<InteractionResult> original) {
        CauseContext.push(player.getUUID());
        try { return original.call(player, level, stack, hand, hit); }
        finally { CauseContext.pop(); }
    }

    @WrapMethod(method = "useItem")
    private InteractionResult dontHurtThem$use(ServerPlayer player, Level level, ItemStack stack,
            InteractionHand hand, Operation<InteractionResult> original) {
        CauseContext.push(player.getUUID());
        try { return original.call(player, level, stack, hand); }
        finally { CauseContext.pop(); }
    }

    @WrapMethod(method = "destroyBlock")
    private boolean dontHurtThem$mine(BlockPos pos, Operation<Boolean> original) {
        CauseContext.push(player.getUUID());
        try { return original.call(pos); }
        finally { CauseContext.pop(); }
    }
}
