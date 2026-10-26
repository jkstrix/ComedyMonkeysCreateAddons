package com.comedymonkey.createaddons.content.electric;

import org.joml.Vector3f;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.mojang.math.Axis;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
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
 * Geothermal Turbine Controller: the middle of the turbine's front. Behind it, a 3x3x3 of Geothermal Casings round a
 * Turbine Core. Formed, it draws the whole machine; running, the machine steams and its vents flicker with flame.
 */
public class GeothermalControllerBlock extends Block implements IBE<GeothermalControllerBlockEntity>, IWrenchable {
	/**
	 * Turned by a wrench, it faces a different 3x3x3: it comes out unformed and unlit (onRemove frees the old casings),
	 * and forms again if the new cube is a turbine.
	 */
	/** On a formed turbine a wrench opens its power level; on an unformed controller it turns it, as before. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (!state.getValue(FORMED))
			return IWrenchable.super.onWrenched(state, context);
		if (context.getLevel().isClientSide)
			com.comedymonkey.createaddons.client.GeothermalTurbineScreen.open(context.getClickedPos());
		return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		return IWrenchable.super.getRotatedBlockState(originalState, targetedFace).setValue(FORMED, false).setValue(LIT, false);
	}

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty FORMED = GeothermalPartBlock.FORMED;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public GeothermalControllerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FORMED, LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** Buckets of lava or water go straight in. */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		if (FluidUtil.getFluidHandler(stack).isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		// the controller offers its tanks to no side (pipes go through the ports below), so ask it without one
		if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, null))
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		return ItemInteractionResult.CONSUME;
	}

	/**
	 * Broken, or turned by a wrench (it then faces a different 3x3x3): free the casings of the turbine it was part of.
	 * The block entity still has the old facing here, so it frees the right ones.
	 */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		boolean turned = state.is(newState.getBlock()) && state.getValue(FACING) != newState.getValue(FACING);
		if ((!state.is(newState.getBlock()) || turned) && level.getBlockEntity(pos) instanceof GeothermalControllerBlockEntity be)
			be.unform();
		// broken: no longer a generator its line can reach from afar
		if (!state.is(newState.getBlock()) && level instanceof net.minecraft.server.level.ServerLevel server)
			com.comedymonkey.createaddons.content.line.PowerGrid.of(server).forget(pos);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/**
	 * Running: steam rising out of the stack on the turbine housing, flames licking out of the firebox grates on both
	 * sides and now and then a pop of lava (its sounds are GeothermalTurbineSounds). Positions are in the formed model's
	 * pixels, about the middle block of the turbine: the stack's mouth is 5 px across round (8, 31, 8), and the grates
	 * are three slots at y -11, -7 and -3, 2 px tall, from z -6 to 22 on the firebox's sides, their glow at x -14.75 and
	 * 30.75 (the flames start just outside it).
	 */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(FORMED) || !state.getValue(LIT))
			return;
		Direction facing = state.getValue(FACING);
		BlockPos centre = pos.relative(facing.getOpposite());
		for (int i = 0; i < 2; i++) {
			Vec3 steam = at(centre, facing, 6 + random.nextFloat() * 4, 31.2f, 6 + random.nextFloat() * 4);
			level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.CLOUD : ParticleTypes.WHITE_SMOKE, steam.x, steam.y, steam.z,
				(random.nextFloat() - 0.5f) * 0.02f, 0.06 + random.nextFloat() * 0.05, (random.nextFloat() - 0.5f) * 0.02f);
		}
		boolean east = random.nextBoolean();
		float ventY = -10 + random.nextInt(3) * 4;
		Vec3 vent = at(centre, facing, east ? 31 : -15, ventY, -6 + random.nextFloat() * 28);
		Vec3 out = at(Vec3.ZERO, facing, east ? 1 : -1, 0, 0);
		if (random.nextInt(2) == 0)
			level.addParticle(ParticleTypes.FLAME, vent.x, vent.y, vent.z, out.x * 0.015, 0.015, out.z * 0.015);
		if (random.nextInt(14) == 0)
			level.addParticle(ParticleTypes.LAVA, vent.x, vent.y, vent.z, 0, 0, 0);
	}

	/** A point given in the formed model's pixels (modelled facing north round the middle block) to world space. */
	private static Vec3 at(BlockPos centre, Direction facing, float x, float y, float z) {
		Vec3 world = at(Vec3.ZERO, facing, x / 16 - 0.5f, y / 16, z / 16 - 0.5f);
		return new Vec3(centre.getX() + 0.5 + world.x, centre.getY() + world.y, centre.getZ() + 0.5 + world.z);
	}

	private static Vec3 at(Vec3 origin, Direction facing, float x, float y, float z) {
		Vector3f v = new Vector3f(x, y, z);
		Axis.YP.rotationDegrees(-(facing.toYRot() + 180)).transform(v);
		return origin.add(v.x, v.y, v.z);
	}

	@Override
	public Class<GeothermalControllerBlockEntity> getBlockEntityClass() {
		return GeothermalControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends GeothermalControllerBlockEntity> getBlockEntityType() {
		return ModBlockEntities.GEOTHERMAL_CONTROLLER.get();
	}
}
