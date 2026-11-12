package yagen.waitmydawn.dont_hurt_them.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;
import yagen.waitmydawn.dont_hurt_them.server.FriendshipService;

public final class ProofOfFriendshipItem extends Item {
    public ProofOfFriendshipItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) FriendshipService.sync(serverPlayer, true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.dont_hurt_them.proof_of_friendship.hint"));
        lines.accept(Component.translatable("item.dont_hurt_them.proof_of_friendship.mark"));
    }
}
