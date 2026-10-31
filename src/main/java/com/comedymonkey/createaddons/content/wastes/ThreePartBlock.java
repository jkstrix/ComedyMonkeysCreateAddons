package com.comedymonkey.createaddons.content.wastes;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decor three blocks long, placed and broken as one like a door (D37): the Charging Alcove and Maker Statue standing
 * three tall, the Toppled Maker Statue lying three long. The block placed is the {@code bottom} part; the others follow
 * up the column, or, lying, toward the facing's clockwise side (east for a north-facing one), where the model runs.
 * Breaking any part breaks the rest; it drops once, from the bottom part's loot table (none in creative).
 */
public class ThreePartBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

	private final boolean lying;
	private final Map<Part, Map<Direction, VoxelShape>> shapes = new EnumMap<>(Part.class);

	/**
	 * @param lying whether it lies along the ground instead of standing
	 * @param northBoxes each part's shape facing north, in pixels ({x0, y0, z0, x1, y1, z1}): bottom, middle, top
	 */
	public ThreePartBlock(Properties properties, boolean lying, double[][][] northBoxes) {
		super(properties);
		this.lying = lying;
		for (Part part : Part.values())
			shapes.put(part, SalvageBlock.shapes(northBoxes[part.ordinal()]));
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PART, Part.BOTTOM));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return simpleCodec(p -> new ThreePartBlock(p, lying, new double[][][] { { { 0, 0, 0, 16, 16, 16 } }, { { 0, 0, 0, 16, 16, 16 } }, { { 0, 0, 0, 16, 16, 16 } } }));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}

	/** The way the parts run from the bottom one. */
	public Direction run(Direction facing) {
		return lying ? facing.getClockWise() : Direction.UP;
	}

	/** Where the bottom part of the block at {@code pos} is. */
	public BlockPos bottom(BlockState state, BlockPos pos) {
		return pos.relative(run(state.getValue(FACING)), -state.getValue(PART).ordinal());
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		for (int k = 1; k <= 2; k++) {
			BlockPos next = pos.relative(run(facing), k);
			if (next.getY() >= level.getMaxBuildHeight() || !level.getBlockState(next).canBeReplaced(context))
				return null;
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		Direction run = run(state.getValue(FACING));
		level.setBlock(pos.relative(run, 1), state.setValue(PART, Part.MIDDLE), Block.UPDATE_ALL);
		level.setBlock(pos.relative(run, 2), state.setValue(PART, Part.TOP), Block.UPDATE_ALL);
	}

	/** A part whose neighbour along the run is gone goes too (each part checks the ones it joins). */
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		Direction run = run(state.getValue(FACING));
		Part part = state.getValue(PART);
		boolean up = direction == run && part != Part.TOP, down = direction == run.getOpposite() && part != Part.BOTTOM;
		if (up || down) {
			Part expected = Part.values()[part.ordinal() + (up ? 1 : -1)];
			if (!neighbour.is(this) || neighbour.getValue(PART) != expected || neighbour.getValue(FACING) != state.getValue(FACING))
				return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	/** In creative, breaking any part takes the bottom away first, quietly, so nothing drops (as doors do). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && player.isCreative() && state.getValue(PART) != Part.BOTTOM) {
			BlockPos bottom = bottom(state, pos);
			BlockState there = level.getBlockState(bottom);
			if (there.is(this) && there.getValue(PART) == Part.BOTTOM) {
				level.setBlock(bottom, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, bottom, Block.getId(there));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(PART)).get(state.getValue(FACING));
	}

	/** Turned or mirrored only with its whole run, in a structure (lying, mirroring would put the parts the wrong way). */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return lying ? state : state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	public enum Part implements StringRepresentable {
		BOTTOM, MIDDLE, TOP;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
