package com.comedymonkey.createaddons.content.juggernaut;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Winding Key (D16-D18): a socket plate fixed to a block face with a key standing out of it, turned by a shaft into
 * the key's hub. Fitted to the back of the centre of a T of Brass Blocks and wound, it wakes the Clockwork Juggernaut
 * (see {@link WindingKeyBlockEntity}). {@code facing} is the way the key points, away from the block it's fixed to.
 */
public class WindingKeyBlock extends DirectionalKineticBlock implements IBE<WindingKeyBlockEntity> {
	/** The plate against the block behind and the key's hub, for a key facing north (the model's way round). */
	private static final VoxelShape NORTH = Shapes.or(Block.box(1, 1, 14, 15, 15, 16), Block.box(5, 5, 4, 11, 11, 14),
		Block.box(1, 4.5, 4.5, 15, 11.5, 6.5));

	public WindingKeyBlock(Properties properties) {
		super(properties);
	}

	/** It goes on the face you click, pointing out of it. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return rotated(state.getValue(FACING));
	}

	private static final java.util.Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.class);

	/** The north shape turned to face {@code facing}. */
	private static VoxelShape rotated(Direction facing) {
		return SHAPES.computeIfAbsent(facing, f -> {
			VoxelShape[] out = { Shapes.empty() };
			NORTH.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
				double[] a = turn(f, x0, y0, z0), b = turn(f, x1, y1, z1);
				out[0] = Shapes.or(out[0], Shapes.box(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
					Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])));
			});
			return out[0];
		});
	}

	/** A point of the north-facing model (block units), as it sits for a key facing {@code f}. */
	private static double[] turn(Direction f, double x, double y, double z) {
		return switch (f) {
			case NORTH -> new double[] { x, y, z };
			case SOUTH -> new double[] { 1 - x, y, 1 - z };
			case EAST -> new double[] { 1 - z, y, x };
			case WEST -> new double[] { z, y, 1 - x };
			case UP -> new double[] { x, 1 - z, y };
			case DOWN -> new double[] { x, z, 1 - y };
		};
	}

	@Override
	public Class<WindingKeyBlockEntity> getBlockEntityClass() {
		return WindingKeyBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends WindingKeyBlockEntity> getBlockEntityType() {
		return ModBlockEntities.WINDING_KEY.get();
	}
}
