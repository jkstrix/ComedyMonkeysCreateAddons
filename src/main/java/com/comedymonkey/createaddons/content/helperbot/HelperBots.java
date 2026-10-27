package com.comedymonkey.createaddons.content.helperbot;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlock;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Helper Bot (design/specs/helper-bot.md): a robot block who does a Blaze Burner's two Create jobs by the Blaze
 * Burner's own rules. Directly behind Train Controls facing him he conducts a train; beside a Stock Ticker he keeps
 * shop. These are the rules both jobs and his block share.
 */
public final class HelperBots {
	private HelperBots() {}

	/** On a train he conducts through Create's own conductor rules, and draws and animates himself as a contraption actor. */
	public static void bindBlocks() {
		MovingInteractionBehaviour.REGISTRY.register(ModBlocks.HELPER_BOT.get(), new HelperBotConductor());
		MovementBehaviour.REGISTRY.register(ModBlocks.HELPER_BOT.get(), new HelperBotMovement());
	}

	/**
	 * The side Train Controls face him from, if any: controls directly beside him, facing back at him, are the ones he
	 * drives with (Create's {@code CarriageContraption.inControl}, on the ground). Contraption Controls are a kind of
	 * Train Controls in code, but nobody drives from them.
	 */
	@Nullable
	public static Direction controlsSide(BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockState there = level.getBlockState(pos.relative(side));
			if (there.getBlock() instanceof ControlsBlock && !(there.getBlock() instanceof ContraptionControlsBlock) && there.getValue(BlockStateProperties.HORIZONTAL_FACING) == side.getOpposite())
				return side;
		}
		return null;
	}

	/** A Stock Ticker beside him on his own level, the one he keeps shop for (the Blaze Burner's rule). */
	@Nullable
	public static BlockPos stockTicker(BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos there = pos.relative(side);
			if (level.getBlockState(there).getBlock() instanceof StockTickerBlock)
				return there;
		}
		return null;
	}

	/** Whether a Helper Bot is beside this Stock Ticker on its level, keeping shop for it (when the config allows it). */
	public static boolean keepsShopFor(BlockGetter level, BlockPos tickerPos) {
		if (!com.comedymonkey.createaddons.config.CMConfig.SERVER.helperBotShopKeeper.get())
			return false;
		for (Direction side : Direction.Plane.HORIZONTAL)
			if (level.getBlockState(tickerPos.relative(side)).getBlock() instanceof HelperBotBlock)
				return true;
		return false;
	}

	/** An order was placed at this Stock Ticker: every Helper Bot keeping shop for it nods (D35). */
	public static void orderPlacedAt(net.minecraft.world.level.Level level, BlockPos tickerPos) {
		if (!com.comedymonkey.createaddons.config.CMConfig.SERVER.helperBotShopKeeper.get())
			return;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos there = tickerPos.relative(side);
			BlockState state = level.getBlockState(there);
			if (state.getBlock() instanceof HelperBotBlock bot)
				level.blockEvent(there, bot, HelperBotBlock.ORDER, 0);
		}
	}

	/** How far (in blocks) his head and caps reach into the block above. */
	private static final double HEAD_ROOM = 10 / 16.0;

	/**
	 * Anything solid low enough in the block above to meet his head or caps (they reach 9.5 px into it) makes him duck
	 * (D17). Scaffolding and powder snow have no collision shape without someone standing on them, but he'd still poke
	 * through.
	 */
	public static boolean roofAbove(BlockGetter level, BlockPos pos) {
		BlockPos above = pos.above();
		BlockState state = level.getBlockState(above);
		if (state.is(Blocks.SCAFFOLDING) || state.is(Blocks.POWDER_SNOW))
			return true;
		VoxelShape shape = state.getCollisionShape(level, above);
		return !shape.isEmpty() && shape.min(Direction.Axis.Y) < HEAD_ROOM;
	}
}
