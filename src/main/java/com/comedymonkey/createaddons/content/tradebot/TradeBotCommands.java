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
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /tradebot clip <name>}: plays one of his clips on the nearest Trade Bot, to check the game against the
 * Blockbench preview. Operators only.
 */
public final class TradeBotCommands {
	private static final double REACH = 32;

	private TradeBotCommands() {}

	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("tradebot").requires(source -> source.hasPermission(2))
			.then(Commands.literal("clip").then(Commands.argument("name", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(ClipLibrary.get().all().stream().map(Clip::name), builder))
				.executes(TradeBotCommands::playClip))));
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
