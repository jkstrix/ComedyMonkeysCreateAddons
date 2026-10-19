package com.comedymonkey.createaddons.content.engine;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
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
import net.createmod.catnip.math.VecHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Magma Engine: a heavy lava-burning engine, four times a Portable Engine (by default 64 RPM with 8,192 SU, burning
 * 10 mB of lava a second). The Supercharged one doubles all of that, and its tank. Pipes fill it through the back or
 * the bottom only; a bucket works on any face. The shaft comes out of the middle of the front ({@code facing}).
 */
public class MagmaEngineBlock extends KineticBlock implements IBE<MagmaEngineBlockEntity> {
	/** The side the output shaft comes out of. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Burning (lights the vents). */
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	private final boolean supercharged;

	public MagmaEngineBlock(Properties properties, boolean supercharged) {
		super(properties);
		this.supercharged = supercharged;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	public boolean isSupercharged() {
		return supercharged;
	}

	/** Output speed in RPM, from the config. */
	public int speed() {
		return supercharged ? CMConfig.SERVER.superchargedMagmaEngineSpeed() : CMConfig.SERVER.magmaEngineSpeed();
	}

	/** Stress capacity per RPM, from the config. */
	public double capacityPerRpm() {
		return supercharged ? CMConfig.SERVER.superchargedMagmaEngineCapacity() : CMConfig.SERVER.magmaEngineCapacity();
	}

	/** Lava burned while running, in mB per second, from the config. */
	public int lavaPerSecond() {
		return supercharged ? CMConfig.SERVER.superchargedMagmaEngineLavaPerSecond() : CMConfig.SERVER.magmaEngineLavaPerSecond();
	}

	/** 4 buckets, or 8 supercharged. */
	public int tankCapacity() {
		return supercharged ? 8000 : 4000;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, LIT));
	}

	/** Shaft towards the player, like a Portable Engine; sneak to place it with the shaft pointing away. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction away = context.getHorizontalDirection();
		boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
		return defaultBlockState().setValue(FACING, sneaking ? away : away.getOpposite());
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
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** Lava buckets go straight in. */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		if (FluidUtil.getFluidHandler(stack).isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		// Pipes only connect at the back and bottom, but a bucket can be poured in from any side
		if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, state.getValue(FACING).getOpposite()))
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		return ItemInteractionResult.CONSUME;
	}

	/** A rumble, embers from the vents and smoke from the stacks while it burns. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT))
			return;
		if (random.nextInt(3) == 0) {
			Vec3 stack;
			if (ModList.get().isLoaded("simulated")) {
				// Out of one of the Portable Engine shape's exhaust outlets, which sit at the hatch end
				stack = new Vec3((random.nextBoolean() ? 2.8 : 13.2) / 16.0, 11.5 / 16, 15.2 / 16);
				stack = VecHelper.rotateCentered(stack, -state.getValue(FACING).toYRot(), Direction.Axis.Y);
			} else {
				// Out of the bore on top of one of the three cylinder heads (modelled with the shaft facing north)
				stack = new Vec3((3.5 + 4.5 * random.nextInt(3)) / 16.0, 20.9 / 16, 8 / 16.0);
				stack = VecHelper.rotateCentered(stack, -(state.getValue(FACING).toYRot() + 180), Direction.Axis.Y);
			}
			level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
				pos.getX() + stack.x, pos.getY() + stack.y, pos.getZ() + stack.z, 0, 0.06, 0);
		}
		if (random.nextInt(4) == 0) {
			Direction side = state.getValue(FACING).getClockWise();
			if (random.nextBoolean())
				side = side.getOpposite();
			level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5 + side.getStepX() * 0.52, pos.getY() + 0.5,
				pos.getZ() + 0.5 + side.getStepZ() * 0.52, 0, 0, 0);
		}
		if (random.nextInt(20) == 0)
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_AMBIENT,
				SoundSource.BLOCKS, 0.4f, 0.7f, false);
	}

	@Override
	public Class<MagmaEngineBlockEntity> getBlockEntityClass() {
		return MagmaEngineBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MagmaEngineBlockEntity> getBlockEntityType() {
		return ModBlockEntities.MAGMA_ENGINE.get();
	}
}
