package com.comedymonkey.createaddons.content.electric;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Lava Generator: burns lava in a small turbine to make Forge Energy. Lava goes in at the middle of the back; power
 * comes out of the terminals in the middle of each side. The front, with its window and turbine intake, faces you.
 */
public class LavaGeneratorBlock extends Block implements IBE<LavaGeneratorBlockEntity>, IWrenchable {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public LavaGeneratorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** The side lava goes in. */
	public static Direction inlet(BlockState state) {
		return state.getValue(FACING).getOpposite();
	}

	/** Power comes out of the two sides. */
	public static boolean isTerminal(BlockState state, Direction side) {
		return side != null && side.getAxis() == state.getValue(FACING).getClockWise().getAxis();
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** Lava buckets pour in from any side. */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		if (FluidUtil.getFluidHandler(stack).isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, inlet(state)))
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		return ItemInteractionResult.CONSUME;
	}

	/** Lava popping at the side vents and a low crackle while it burns. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT))
			return;
		Direction side = state.getValue(FACING).getClockWise();
		if (random.nextBoolean())
			side = side.getOpposite();
		Vec3 vent = Vec3.atCenterOf(pos).add(side.getStepX() * 0.47, -0.22, side.getStepZ() * 0.47);
		if (random.nextInt(8) == 0)
			level.addParticle(ParticleTypes.LAVA, vent.x, vent.y, vent.z, 0, 0, 0);
		if (random.nextInt(3) == 0)
			level.addParticle(ParticleTypes.SMALL_FLAME, vent.x, vent.y, vent.z, side.getStepX() * 0.01, 0.01, side.getStepZ() * 0.01);
		if (random.nextInt(24) == 0)
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_AMBIENT,
				SoundSource.BLOCKS, 0.3f, 0.8f, false);
	}

	@Override
	public Class<LavaGeneratorBlockEntity> getBlockEntityClass() {
		return LavaGeneratorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends LavaGeneratorBlockEntity> getBlockEntityType() {
		return ModBlockEntities.LAVA_GENERATOR.get();
	}
}
