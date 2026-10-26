package com.comedymonkey.createaddons.content.electric;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
	/** There's lava in the tank: the window shows it, burning or not. */
	public static final BooleanProperty FUELLED = BooleanProperty.create("fuelled");

	public LavaGeneratorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(FUELLED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT, FUELLED);
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

	/** Broken or replaced: no longer a generator its line can reach from afar. */
	@Override
	protected void onRemove(BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, BlockState newState,
							boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level instanceof net.minecraft.server.level.ServerLevel server)
			com.comedymonkey.createaddons.content.line.PowerGrid.of(server).forget(pos);
		super.onRemove(state, level, pos, newState, movedByPiston);
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

	/**
	 * While it burns, small flames flicker at the lava window on the front (x 3–13, y 4–6 px in the model, behind
	 * glass). Its sounds, lava pops among them, are ElectricSounds.
	 */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT) || random.nextInt(3) != 0)
			return;
		Direction front = state.getValue(FACING), across = front.getClockWise();
		double along = (random.nextDouble() - 0.5) * 0.56;
		Vec3 at = Vec3.atCenterOf(pos).add(front.getStepX() * 0.47 + across.getStepX() * along, -0.19 + random.nextDouble() * 0.08,
			front.getStepZ() * 0.47 + across.getStepZ() * along);
		level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, front.getStepX() * 0.004, 0.006, front.getStepZ() * 0.004);
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
