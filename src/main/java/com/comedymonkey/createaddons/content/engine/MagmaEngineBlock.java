package com.comedymonkey.createaddons.content.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Magma Engine: a lava-burning V-twin, four times a Portable Engine (by default 64 RPM with 8,192 SU, burning 10 mB of
 * lava a second). The Supercharged one doubles all of that, and its tank. The shaft comes out of the middle of the
 * front ({@code facing}), which can point any way; pipes fill it on every other side, and a bucket works on any face.
 * Its look and effects are modelled with the shaft to the north; {@link #modelToWorld} turns them to the facing.
 */
public class MagmaEngineBlock extends KineticBlock implements IBE<MagmaEngineBlockEntity> {
	/** The side the output shaft comes out of. */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	/** Burning (the firebox and exhausts glow, the pistons pump). */
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

	/** Pipes connect on every side but the front, where the shaft is. */
	public static boolean isInlet(BlockState state, Direction side) {
		return side != null && side != state.getValue(FACING);
	}

	/**
	 * Turns a point in the engine's model (shaft to the north, in block units) to where it is in the world for this
	 * facing, as the blockstate's "x" and "y" turn the model: the matrix for the renderer, the value box and effects.
	 */
	public static Matrix4f modelToWorld(Direction facing) {
		float x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
		float y = facing.getAxis().isHorizontal() ? facing.getOpposite().toYRot() : 0;
		return new Matrix4f().translate(0.5f, 0.5f, 0.5f)
			.rotateY((float) Math.toRadians(-y))
			.rotateX((float) Math.toRadians(-x))
			.translate(-0.5f, -0.5f, -0.5f);
	}

	/** A point in the model (in pixels, shaft to the north) as an offset in the block for this facing. */
	public static Vec3 modelPoint(Direction facing, double x, double y, double z) {
		Vector3f v = modelToWorld(facing).transformPosition(new Vector3f((float) x / 16, (float) y / 16, (float) z / 16));
		return new Vec3(v.x, v.y, v.z);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, LIT));
	}

	/** Shaft towards the player, whichever way they look; sneak to place it with the shaft pointing away. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction away = context.getNearestLookingDirection();
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
		// Pipes connect on every side but the front, but a bucket can be poured in from any side
		if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, state.getValue(FACING).getOpposite()))
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		return ItemInteractionResult.CONSUME;
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
