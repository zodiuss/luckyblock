package net.zodiuss.luckyblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.zodiuss.luckyblock.LuckyBlock;
import net.zodiuss.luckyblock.block.LuckyBlocks;
import net.zodiuss.luckyblock.block.custom.LuckyBlockBlock;
import net.zodiuss.luckyblock.component.CustomDropData;
import net.zodiuss.luckyblock.component.ModComponents;
import net.zodiuss.luckyblock.drop.LuckyDropExecutor;
import net.zodiuss.luckyblock.drop.LuckyDropSelector;

public class LuckyCommands {
    /** Loader-agnostic registration: loader passes dispatcher from its event */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(
                Commands.literal("lucky")
                        .then(Commands.literal("drop")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(context -> runDrop(context.getSource(), StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("give")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("drop", StringArgumentType.string())
                                                .executes(context -> runGive(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        StringArgumentType.getString(context, "drop"),
                                                        null
                                                ))
                                                .then(Commands.argument("luck", IntegerArgumentType.integer(LuckyBlockBlock.MIN_LUCK, LuckyBlockBlock.MAX_LUCK))
                                                        .executes(context -> runGive(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "drop"),
                                                                IntegerArgumentType.getInteger(context, "luck")
                                                        )))))));

        LuckyBlock.LOGGER.info("Registered lucky commands");
    }

    /** Backwards compat for fabric direct call */
    @Deprecated
    public static void register() {
        // no-op - loader must call register(dispatcher,...)
        LuckyBlock.LOGGER.warn("LuckyCommands.register() without dispatcher is deprecated - commands not registered");
    }

    private static int runDrop(CommandSourceStack source, String name) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        LuckyDropSelector.find(source.getServer(), name).ifPresentOrElse(
                drop -> LuckyDropExecutor.execute(drop, source.getLevel(), player.blockPosition(), player),
                () -> player.sendSystemMessage(Component.literal("Unknown lucky drop: " + name))
        );

        return 1;
    }

    private static int runGive(CommandSourceStack source, Iterable<ServerPlayer> targets, String dropName, Integer luck) {
        var drop = LuckyDropSelector.find(source.getServer(), dropName);
        if (drop.isEmpty()) {
            source.sendFailure(Component.literal("Unknown lucky drop: " + dropName));
            return 0;
        }

        int resolvedLuck = luck != null ? luck : drop.get().luck();
        int count = 0;

        for (ServerPlayer player : targets) {
            ItemStack stack = new ItemStack(LuckyBlocks.LUCKY_BLOCK);
            stack.set(ModComponents.LUCK, resolvedLuck);
            stack.set(ModComponents.CUSTOM_DROP, CustomDropData.ofId(dropName));

            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }

            count++;
        }

        final int givenCount = count;
        source.sendSuccess(
                () -> Component.literal("Gave lucky block with drop " + dropName + " to " + givenCount + " player(s)"),
                true
        );

        return givenCount;
    }
}
