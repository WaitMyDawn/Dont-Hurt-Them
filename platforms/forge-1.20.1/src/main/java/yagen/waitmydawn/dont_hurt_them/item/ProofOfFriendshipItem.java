package yagen.waitmydawn.dont_hurt_them.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (player instanceof ServerPlayer serverPlayer) FriendshipService.sync(serverPlayer, true);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.dont_hurt_them.proof_of_friendship.hint"));
        lines.add(Component.translatable("item.dont_hurt_them.proof_of_friendship.mark"));
    }
}
