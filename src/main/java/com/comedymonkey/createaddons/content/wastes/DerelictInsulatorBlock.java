package com.comedymonkey.createaddons.content.wastes;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Derelict Insulator (D61, D64; approved as previewed in design/models/clockwork-wastes-insulator.gen.js): a three-tier
 * chamfered void-alloy insulator stack for the city's towers and masts, holding its sky-cable spans
 * ({@link DerelictInsulatorBlockEntity}). One block with three mounts by the face it's placed on: on top of a block (a
 * collar whose claws bite a girder), on a wall (a plate and a braced arm), or hanging under one (a socket; the spire's
 * crown-arm ends). Its neck ring is lit while its district is fed (an emissive part in its block model). The city
 * places them and links their spans; a player's has none. Breaks into scrap, its spans with it.
 */
public class DerelictInsulatorBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final MapCodec<DerelictInsulatorBlock> CODEC = simpleCodec(DerelictInsulatorBlock::new);
	public static final EnumProperty<Mount> MOUNT = EnumProperty.create("mount", Mount.class);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	/** Where a span ties on, in block px (the exporter's tie points; x and z are the block's middle for every mount and facing). */
	public static final double TIE_TOP = 12, TIE_WALL = 13.5, TIE_HANGING = 4.4;

	private static final Map<Direction, VoxelShape> WALL_SHAPES = new EnumMap<>(Direction.class);
	private static final VoxelShape TOP_SHAPE = Block.box(5.5, 0, 5.5, 10.5, 14, 10.5), HANGING_SHAPE = Block.box(5.5, 2, 5.5, 10.5, 16, 10.5);

	static {
		// facing west in the model (its wall on the east): the plate and arm from x 4 to the wall, the stack round the middle
		Map<Direction, VoxelShape> north = SalvageBlock.shapes(new double[][] { { 6, 0.5, 4, 10, 14, 16 } });
		// SalvageBlock's boxes are given facing north; the wall mount faces away from its wall, which is behind it
		WALL_SHAPES.putAll(north);
	}

	public DerelictInsulatorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(MOUNT, Mount.TOP).setValue(LIT, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, MOUNT, LIT);
	}

	/** On the face clicked: on a top a standing stack, under a block a hanging one, on a wall's side a wall mount facing out. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (face == Direction.UP)
			return defaultBlockState().setValue(MOUNT, Mount.TOP).setValue(FACING, context.getHorizontalDirection().getOpposite());
		if (face == Direction.DOWN)
			return defaultBlockState().setValue(MOUNT, Mount.HANGING).setValue(FACING, context.getHorizontalDirection().getOpposite());
		return defaultBlockState().setValue(MOUNT, Mount.WALL).setValue(FACING, face);
	}

	/** Where a span ties on, in the block (px / 16). */
	public static Vec3 tie(BlockState state) {
		double y = switch (state.getValue(MOUNT)) {
			case TOP -> TIE_TOP;
			case WALL -> TIE_WALL;
			case HANGING -> TIE_HANGING;
		};
		return new Vec3(0.5, y / 16, 0.5);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(MOUNT)) {
			case TOP -> TOP_SHAPE;
			case HANGING -> HANGING_SHAPE;
			case WALL -> WALL_SHAPES.get(state.getValue(FACING));
		};
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof DerelictInsulatorBlockEntity be)
			be.scheduledTick();
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!newState.is(this) && level.getBlockEntity(pos) instanceof DerelictInsulatorBlockEntity be)
			be.removed();
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DerelictInsulatorBlockEntity(pos, state);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	public enum Mount implements StringRepresentable {
		TOP, WALL, HANGING;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
