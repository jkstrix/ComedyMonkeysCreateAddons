package com.comedymonkey.createaddons.content.light;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Pendant Lamp or Chandelier (design/specs/electric-lights.md, D19): hung under a ceiling on a short flex from a rose,
 * and lowered a block at a time with a Power Line (right-click it with one: up to 16), the gap filled with hanging cable
 * that carries its power (HangingCableBlock). HUNG: it hangs from cable, the rose up at the ceiling; else it's right under
 * the ceiling with its rose. It's fed through the ceiling it hangs from, as any light fixed to a block is (LightMounts).
 */
public class HangingLampBlock extends LightBlock {
	public static final BooleanProperty HUNG = BooleanProperty.create("hung");
	/** The most blocks of cable a lamp is lowered on. */
	public static final int MAX_DROP = 16;

	public HangingLampBlock(LightKind kind, Properties properties) {
		super(kind, properties);
		registerDefaultState(defaultBlockState().setValue(HUNG, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HUNG);
	}

	/** The rose this lamp's cable hangs from (steel for the Pendant, brass for the Chandelier). */
	public HangingCableBlock.Rose rose() {
		return kind() == LightKind.CHANDELIER ? HangingCableBlock.Rose.BRASS : HangingCableBlock.Rose.STEEL;
	}

	/** The block the lamp at pos hangs from: the ceiling above it, or above the top of its cable. */
	public static @Nullable BlockPos ceiling(BlockGetter level, BlockState state, BlockPos pos) {
		if (!state.hasProperty(HUNG))
			return null;
		BlockPos p = pos.above();
		for (int i = 0; i <= MAX_DROP; i++) {
			// (never loads a chunk to look)
			if (level instanceof LevelReader reader && !reader.hasChunkAt(p))
				return null;
			if (!(level.getBlockState(p).getBlock() instanceof HangingCableBlock))
				return p;
			p = p.above();
		}
		return null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(HUNG))
			return level.getBlockState(pos.above()).getBlock() instanceof HangingCableBlock;
		return super.canSurvive(state, level, pos);
	}

	/** A Power Line in hand lowers the lamp a block (D19), leaving cable in its place; the wire is used. */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
											  BlockHitResult hit) {
		if (!stack.is(ModItems.COPPER_POWER_LINE.get()))
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		if (!player.mayBuild() || !canLower(level, state, pos))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (level.isClientSide)
			return ItemInteractionResult.SUCCESS;
		lower(level, pos);
		if (!player.getAbilities().instabuild)
			stack.shrink(1);
		level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6f, 1.2f);
		return ItemInteractionResult.SUCCESS;
	}

	/** Room under it, and short of its longest drop. */
	static boolean canLower(Level level, BlockState state, BlockPos pos) {
		return level.getBlockState(pos.below()).canBeReplaced() && drop(level, state, pos) < MAX_DROP;
	}

	/** Moves the hanging lamp at pos down a block, cable left where it hung; false if it can't go. */
	public static boolean lower(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof HangingLampBlock lamp) || !canLower(level, state, pos))
			return false;
		BlockPos below = pos.below();
		// the lamp moves down with everything it was (colour, brightness, its store)
		CompoundTag data = level.getBlockEntity(pos) instanceof LightBlockEntity be ? be.saveCustomOnly(level.registryAccess()) : null;
		BlockState lowered = state.setValue(HUNG, true).setValue(WATERLOGGED, level.getFluidState(below).getType() == Fluids.WATER);
		level.setBlock(below, lowered, Block.UPDATE_ALL);
		if (data != null && level.getBlockEntity(below) instanceof LightBlockEntity be) {
			be.loadCustomOnly(data, level.registryAccess());
			be.setChanged();
			level.sendBlockUpdated(below, lowered, lowered, Block.UPDATE_ALL);
		}
		level.setBlock(pos, ModBlocks.HANGING_CABLE.get().defaultBlockState()
			.setValue(HangingCableBlock.ROSE, state.getValue(HUNG) ? HangingCableBlock.Rose.NONE : lamp.rose())
			.setValue(HangingCableBlock.WATERLOGGED, state.getValue(WATERLOGGED)), Block.UPDATE_ALL);
		return true;
	}

	/** How many blocks of cable the lamp at pos hangs on. */
	static int drop(BlockGetter level, BlockState state, BlockPos pos) {
		if (!state.getValue(HUNG))
			return 0;
		int n = 0;
		for (BlockPos p = pos.above(); level.getBlockState(p).getBlock() instanceof HangingCableBlock && n <= MAX_DROP; p = p.above())
			n++;
		return n;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
		BlockState state = super.getStateForPlacement(ctx);
		return state == null ? null : state.setValue(HUNG, false);
	}
}
