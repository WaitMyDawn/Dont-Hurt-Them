package yagen.waitmydawn.dont_hurt_them.server;

import net.minecraft.commands.Commands;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yagen.waitmydawn.dont_hurt_them.DontHurtThem;
import java.util.UUID;

public final class FriendshipCommands {
    public static void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        
        var uuidArgument = Commands.argument("uuid", UuidArgument.uuid())
                .suggests((context, builder) -> {
                    var player = context.getSource().getPlayerOrException();
                    String type = ResourceLocationArgument.getId(context, "type").toString();
                    return SharedSuggestionProvider.suggest(FriendshipService.list(player).entries().stream()
                            .filter(entry -> entry.individual() && FriendshipLifecycle.canRevive(player, type, entry.entityId()))
                            .map(entry -> entry.entityId().toString()), builder);
                })
                .executes(context -> execute(context, null))
                .then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(context -> execute(context, Vec3Argument.getVec3(context, "pos"))));
        dispatcher.register(Commands.literal("dht").requires(source -> source.hasPermission(2))
                .then(Commands.literal("revive")
                        .then(Commands.argument("type", ResourceLocationArgument.id())
                                .suggests((context, builder) -> {
                                    var player = context.getSource().getPlayerOrException();
                                    return SharedSuggestionProvider.suggestResource(FriendshipService.list(player).entries().stream()
                                            .filter(entry -> entry.individual() && FriendshipLifecycle.canRevive(player, entry.typeId(), entry.entityId()))
                                            .map(entry -> ResourceLocation.parse(entry.typeId())).distinct(), builder);
                                }).then(uuidArgument))));
    }

    private static int execute(CommandContext<net.minecraft.commands.CommandSourceStack> context, Vec3 position) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        String type = ResourceLocationArgument.getId(context, "type").toString();
        UUID uuid = UuidArgument.getUuid(context, "uuid");
        try {
            if (position == null) FriendshipLifecycle.revive(player, type, uuid);
            else FriendshipLifecycle.revive(player, type, uuid, context.getSource().getLevel(), position);
            context.getSource().sendSuccess(() -> Component.translatable("message.dont_hurt_them.revived", uuid.toString()), true);
            return 1;
        } catch (IllegalArgumentException error) {
            context.getSource().sendFailure(Component.translatable("message.dont_hurt_them." + error.getMessage()));
            return 0;
        }
    }
    private FriendshipCommands() {}
}
