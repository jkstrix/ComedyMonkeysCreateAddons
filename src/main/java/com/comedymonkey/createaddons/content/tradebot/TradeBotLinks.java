package com.comedymonkey.createaddons.content.tradebot;

import com.comedymonkey.createaddons.config.CMConfig;
import com.simibubi.create.content.kinetics.belt.BeltBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * What Trade Bot can be linked to (D36, D61): anything that holds items (chests, barrels, vaults, depots, other mods'
 * containers) as a place to take from or deliver to, and Create belts as a place to deliver to only. Picking cycles a
 * block through takes from, delivers to and unlinked.
 */
public final class TradeBotLinks {
	/** Outline and text colours: muted cyan for takes from, his amber for delivers to. */
	public static final int TAKES_COLOUR = 0x6FB7C4, DELIVERS_COLOUR = 0xD99A4A;

	public enum Role { NONE, TAKES, DELIVERS }

	private TradeBotLinks() {}

	public static boolean canLink(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof TradeBotStationBlock)
			return false;
		return outputOnly(level, pos) || level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
	}

	/** Belts carry things away; he puts things on them but never takes from them. */
	public static boolean outputOnly(Level level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof BeltBlock;
	}

	public static boolean inRange(BlockPos station, BlockPos target) {
		int range = CMConfig.SERVER.tradeBotRange();
		return station.distSqr(target) <= (double) range * range;
	}

	/** The role after one more click, given the current role and how many inputs are already taken. */
	public static Role nextRole(Role current, boolean outputOnly, int inputs, int maxInputs) {
		return switch (current) {
			case NONE -> outputOnly || inputs >= maxInputs ? Role.DELIVERS : Role.TAKES;
			case TAKES -> Role.DELIVERS;
			case DELIVERS -> Role.NONE;
		};
	}

	/** "Takes from Chest" / "Delivers to Depot" / "Unlinked Barrel", coloured like the outlines. */
	public static Component describe(Level level, BlockPos pos, Role role) {
		Component name = level.getBlockState(pos).getBlock().getName();
		MutableComponent text = Component.translatable("message.comedymonkeysaddons.trade_bot.link." + role.name().toLowerCase(java.util.Locale.ROOT), name);
		return switch (role) {
			case TAKES -> text.withColor(TAKES_COLOUR);
			case DELIVERS -> text.withColor(DELIVERS_COLOUR);
			case NONE -> text.withStyle(ChatFormatting.GRAY);
		};
	}
}
