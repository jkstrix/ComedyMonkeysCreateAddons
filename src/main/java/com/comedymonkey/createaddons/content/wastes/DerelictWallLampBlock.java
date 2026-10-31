package com.comedymonkey.createaddons.content.wastes;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Derelict Wall Lamp (D16, P25; brief §4): a makers' cast-iron bracket on a wall holding the uprising's hooded lens,
 * tipped down and out toward the facing. Lit while its district is fed ({@link DerelictLightBlockEntity}); breaks into
 * scrap. Placed against a wall's face, it points away from it.
 */
public class DerelictWallLampBlock extends HorizontalDirectionalBlock implements EntityBlock, DerelictLightBlockEntity.DerelictLight {
	public static final MapCodec<DerelictWallLampBlock> CODEC = simpleCodec(DerelictWallLampBlock::new);
	public static final BooleanProperty LIT = DerelictStreetLampBlock.LIT;
	private static final Map<Direction, VoxelShape> SHAPES = SalvageBlock.shapes(new double[][] { { 6, 6, 15, 10, 14, 16 }, { 5, 5, 2, 11, 14, 15 } });

	public DerelictWallLampBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	@Override
	public boolean lightable(BlockState state) {
		return true;
	}

	public static int lightLevel(BlockState state) {
		return state.getValue(LIT) ? 15 : 0;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof DerelictLightBlockEntity be)
			be.scheduledTick();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DerelictLightBlockEntity(pos, state);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
