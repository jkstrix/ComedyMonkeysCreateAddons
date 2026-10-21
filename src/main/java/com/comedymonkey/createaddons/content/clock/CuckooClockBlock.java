package com.comedymonkey.createaddons.content.clock;

import java.util.EnumMap;
import java.util.Map;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cuckoo Clock: a wall clock two blocks tall. This is the top block, with the case, roof and works, and the shaft
 * into its back; the pendulum and weights hang into the block below, a {@link CuckooClockWeightsBlock} placed with
 * it. While it turns, villagers nearby restock at 8:00 and 12:00, and it chimes and calls the hour (see
 * {@link CuckooClockBlockEntity}).
 */
public class CuckooClockBlock extends KineticBlock implements IBE<CuckooClockBlockEntity> {
	/** The front, with the dial. The shaft goes into the opposite face, usually through the wall it hangs on. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Case and roof, modelled with the front facing north. */
	private static final Map<Direction, VoxelShape> SHAPES = shapes(new double[][] {
		{2.25, 0, 5.25, 13.75, 11.25, 16}, {1, 9.5, 5.25, 15, 16, 16}});

	public CuckooClockBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	/**
	 * Hung on the clicked wall, facing out from it; clicked on a floor or ceiling, it faces the player. The block below
	 * must be free for the weights.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos below = context.getClickedPos().below();
		Level level = context.getLevel();
		if (below.getY() < level.getMinBuildHeight() || !level.getBlockState(below).canBeReplaced(context))
			return null;
		Direction clicked = context.getClickedFace();
		Direction facing = clicked.getAxis().isHorizontal() ? clicked : context.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(FACING, facing);
	}

	/** Puts the weights in below, however the clock got here (placed, moved, Carry On), if there's room. */
	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (level.isClientSide || oldState.is(this))
			return;
		BlockPos below = pos.below();
		BlockState there = level.getBlockState(below);
		if (there.is(ModBlocks.CUCKOO_CLOCK_WEIGHTS.get()))
			return;
		if (!there.canBeReplaced()) {
			level.scheduleTick(pos, this, 1);  // no room for the weights after all: it drops (see tick)
			return;
		}
		level.setBlock(below, ModBlocks.CUCKOO_CLOCK_WEIGHTS.get().defaultBlockState()
			.setValue(CuckooClockWeightsBlock.FACING, state.getValue(FACING)), Block.UPDATE_ALL);
	}

	/**
	 * Keeps the weights turned with the clock (wrenching, structure rotation). If the weights go some other way than a
	 * player breaking them (a drill, an explosion, {@code /setblock}), the clock drops a tick later.
	 */
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
									 BlockPos pos, BlockPos neighbourPos) {
		if (direction == Direction.DOWN) {
			if (!neighbour.is(ModBlocks.CUCKOO_CLOCK_WEIGHTS.get()))
				level.scheduleTick(pos, this, 1);
			else if (neighbour.getValue(CuckooClockWeightsBlock.FACING) != state.getValue(FACING))
				level.setBlock(neighbourPos, neighbour.setValue(CuckooClockWeightsBlock.FACING, state.getValue(FACING)), Block.UPDATE_ALL);
		}
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	/** Without its weights below it the clock comes down, dropping itself. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!level.getBlockState(pos.below()).is(ModBlocks.CUCKOO_CLOCK_WEIGHTS.get()))
			level.destroyBlock(pos, true);
	}

	/** Only the back takes a shaft, in the middle of this block. */
	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING).getOpposite();
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		// the weights below go with it, and drop nothing of their own
		BlockPos below = pos.below();
		if (!level.isClientSide && level.getBlockState(below).is(ModBlocks.CUCKOO_CLOCK_WEIGHTS.get()))
			level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public Class<CuckooClockBlockEntity> getBlockEntityClass() {
		return CuckooClockBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CuckooClockBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CUCKOO_CLOCK.get();
	}

	/**
	 * North-facing boxes (pixels, may reach outside 0..16) turned to each facing the way the blockstate turns the
	 * model: y 90 takes north to east.
	 */
	static Map<Direction, VoxelShape> shapes(double[][] northBoxes) {
		Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape shape = Shapes.empty();
			for (double[] b : northBoxes) {
				double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
				double[] r = switch (facing) {
					case EAST -> new double[] {16 - z2, 16 - z1, x1, x2};
					case SOUTH -> new double[] {16 - x2, 16 - x1, 16 - z2, 16 - z1};
					case WEST -> new double[] {z1, z2, 16 - x2, 16 - x1};
					default -> new double[] {x1, x2, z1, z2};
				};
				shape = Shapes.or(shape, Block.box(r[0], b[1], r[2], r[1], b[4], r[3]));
			}
			map.put(facing, shape.optimize());
		}
		return map;
	}
}
