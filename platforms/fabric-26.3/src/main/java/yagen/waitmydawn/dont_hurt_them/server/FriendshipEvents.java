package yagen.waitmydawn.dont_hurt_them.server;

import yagen.waitmydawn.dont_hurt_them.data.NbtCompat;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import yagen.waitmydawn.dont_hurt_them.registries.ModRegistries;

public final class FriendshipEvents {
    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!player.getItemInHand(hand).is(ModRegistries.PROOF_OF_FRIENDSHIP.get())) return InteractionResult.PASS;
            if (player instanceof ServerPlayer serverPlayer) {
                if (player.isShiftKeyDown()) FriendshipService.sync(serverPlayer, true);
                else FriendshipService.toggle(serverPlayer, entity);
            }
            return InteractionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!player.isShiftKeyDown() || !player.getItemInHand(hand).is(ModRegistries.PROOF_OF_FRIENDSHIP.get())) return InteractionResult.PASS;
            if (player instanceof ServerPlayer serverPlayer) FriendshipService.sync(serverPlayer, true);
            return InteractionResult.SUCCESS;
        });
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
                DamageProtection.protects(entity, player.getUUID()) ? InteractionResult.FAIL : InteractionResult.PASS);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !DamageProtection.shouldProtect(entity, source));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Player) return;
            var owner = DamageProtection.owner(entity);
            if (owner == null) owner = CauseContext.current();
            if (owner != null) NbtCompat.putUuid(ModRegistries.data(entity), DamageProtection.OWNER, owner);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> FriendshipService.changed(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((previous, next, alive) -> FriendshipService.sync(next, false));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, previous, next) -> FriendshipService.sync(player, false));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % 20 == 0 && FriendshipService.holding(player)) FriendshipService.sync(player, false);
            }
        });
    }
    private FriendshipEvents() {}
}
