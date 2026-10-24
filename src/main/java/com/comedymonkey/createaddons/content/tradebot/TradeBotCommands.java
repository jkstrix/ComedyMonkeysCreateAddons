package com.comedymonkey.createaddons.content.tradebot;

import java.util.Comparator;
import java.util.List;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Operator commands for trying Trade Bot out: {@code /tradebot clip <name>} plays one of his clips on the nearest bot (to
 * check the game against the Blockbench preview), {@code /tradebot goto <pos>} sends him there and back, and
 * {@code /tradebot home} calls him home.
 */
public final class TradeBotCommands {
	private static final double REACH = 32;

	private TradeBotCommands() {}

	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("tradebot").requires(source -> source.hasPermission(2))
			.then(Commands.literal("clip").then(Commands.argument("name", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(ClipLibrary.get().all().stream().map(Clip::name), builder))
				.executes(TradeBotCommands::playClip)))
			.then(Commands.literal("goto").then(Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(TradeBotCommands::goTo)))
			.then(Commands.literal("home").executes(TradeBotCommands::home))
			.then(Commands.literal("debug").executes(context -> {
				TradeBotMotion.debug = !TradeBotMotion.debug;
				context.getSource().sendSuccess(() -> Component.literal("Trade Bot route logging " + (TradeBotMotion.debug ? "on" : "off")), false);
				return 1;
			})));
	}

	/** Sends the nearest bot to stand on a block, then home again (for trying out movement before he has jobs). */
	private static int goTo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		TradeBotEntity bot = nearest(context.getSource());
		if (bot == null)
			return 0;
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
		if (!bot.brain.goTo(pos)) {
			context.getSource().sendFailure(Component.translatable("commands.comedymonkeysaddons.tradebot.no_route"));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.translatable("commands.comedymonkeysaddons.tradebot.goto", pos.toShortString()), false);
		return 1;
	}

	private static int home(CommandContext<CommandSourceStack> context) {
		TradeBotEntity bot = nearest(context.getSource());
		if (bot == null)
			return 0;
		bot.goHome();
		context.getSource().sendSuccess(() -> Component.translatable("message.comedymonkeysaddons.trade_bot.heading_home"), false);
		return 1;
	}

	private static TradeBotEntity nearest(CommandSourceStack source) {
		List<TradeBotEntity> bots = source.getLevel().getEntitiesOfClass(TradeBotEntity.class,
			AABB.ofSize(source.getPosition(), REACH * 2, REACH * 2, REACH * 2));
		TradeBotEntity bot = bots.stream().min(Comparator.comparingDouble(b -> b.distanceToSqr(source.getPosition()))).orElse(null);
		if (bot == null)
			source.sendFailure(Component.translatable("commands.comedymonkeysaddons.tradebot.no_bot"));
		return bot;
	}

	private static int playClip(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		Clip clip = ClipLibrary.get().find(StringArgumentType.getString(context, "name"));
		if (clip == null) {
			source.sendFailure(Component.translatable("commands.comedymonkeysaddons.tradebot.no_clip"));
			return 0;
		}
		List<TradeBotEntity> bots = source.getLevel().getEntitiesOfClass(TradeBotEntity.class,
			AABB.ofSize(source.getPosition(), REACH * 2, REACH * 2, REACH * 2));
		TradeBotEntity bot = bots.stream().min(Comparator.comparingDouble(b -> b.distanceToSqr(source.getPosition()))).orElse(null);
		if (bot == null) {
			source.sendFailure(Component.translatable("commands.comedymonkeysaddons.tradebot.no_bot"));
			return 0;
		}
		bot.playClip(clip);
		source.sendSuccess(() -> Component.translatable("commands.comedymonkeysaddons.tradebot.clip", clip.name(), clip.length()), false);
		return 1;
	}
}
