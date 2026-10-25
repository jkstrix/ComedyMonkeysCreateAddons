package com.comedymonkey.createaddons.compat.sable;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cockpit panel of four dials (altimeter, airspeed, attitude, heading) facing whoever placed it. The ship's
 * forward is taken as the way the pilot faces the panel.
 */
public class FlightInstrumentsBlock extends BaseEntityBlock implements IWrenchable {
	public static final MapCodec<FlightInstrumentsBlock> CODEC = simpleCodec(FlightInstrumentsBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** The console's outline per facing: the lip, then the body stepping back under the leaning panel. */
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape shape = box(facing, 0, 5, 2);
			for (int y = 2; y < 16; y += 2)
				shape = Shapes.or(shape, box(facing, y, 8 + (int) ((y - 2) * Math.tan(Math.toRadians(22.5))), y + 2));
			// the hand rail on the lip, and the glare hood over the panel's top edge (it overhangs the top by under a pixel)
			shape = Shapes.or(shape, box(facing, 2, 5, 4), box(facing, 14, 11, 17));
			SHAPES.put(facing, shape.optimize());
		}
	}

	/** A full-width slab from {@code y0} to {@code y1}, running from {@code depth} back from the front to the back. */
	private static VoxelShape box(Direction facing, int y0, int depth, int y1) {
		return switch (facing) {
			case SOUTH -> Block.box(0, y0, 0, 16, y1, 16 - depth);
			case EAST -> Block.box(0, y0, 0, 16 - depth, y1, 16);
			case WEST -> Block.box(depth, y0, 0, 16, y1, 16);
			default -> Block.box(0, y0, depth, 16, y1, 16);
		};
	}

	public FlightInstrumentsBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FlightInstrumentsBlockEntity(pos, state);
	}
}
