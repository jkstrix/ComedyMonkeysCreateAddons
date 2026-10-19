package com.comedymonkey.createaddons.content.incinerator;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Incinerator: a Create-styled furnace that destroys whatever is piped, funnelled or poured into it, items and
 * fluids alike. While it's burning it heats a steam engine boiler above it like a lit Blaze Burner.
 */
public class IncineratorBlock extends BaseEntityBlock implements IWrenchable {
	public static final MapCodec<IncineratorBlock> CODEC = simpleCodec(IncineratorBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Something was burned in the last few seconds. */
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public IncineratorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
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
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new IncineratorBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null
			: createTickerHelper(type, ModBlockEntities.INCINERATOR.get(), (l, p, s, be) -> be.serverTick());
	}

	/** Flames and smoke from the grille and chimney, and a crackle, while it burns. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT))
			return;
		Direction front = state.getValue(FACING);
		double x = pos.getX() + 0.5 + front.getStepX() * 0.52 + (random.nextDouble() - 0.5) * 0.4;
		double y = pos.getY() + 0.25 + random.nextDouble() * 0.3;
		double z = pos.getZ() + 0.5 + front.getStepZ() * 0.52 + (random.nextDouble() - 0.5) * 0.4;
		level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
		if (random.nextInt(3) == 0)
			level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.04, 0);
		if (random.nextInt(10) == 0)
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE,
				SoundSource.BLOCKS, 0.8f, 1.0f, false);
	}
}
