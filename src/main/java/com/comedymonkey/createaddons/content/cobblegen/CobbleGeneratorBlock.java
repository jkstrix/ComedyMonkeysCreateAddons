package com.comedymonkey.createaddons.content.cobblegen;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * Andesite and Brass Cobblestone, Basalt and Blackstone Generators: the window ({@code facing}) faces the player when placed, a shaft goes
 * into the back (the only side that takes power) and cobblestone comes out
 * at the pace a Mechanical Drill would mine it (see {@link CobbleGeneratorBlockEntity}). It only comes out through
 * item transport (hoppers, chutes, funnels, pipes, arms) from any side; it stops when its buffer is full.
 */
public class CobbleGeneratorBlock extends KineticBlock implements IBE<CobbleGeneratorBlockEntity> {
	/** The front, with the window. The shaft goes into the opposite face. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Held off by a redstone signal (lights a wireless generator's antenna). */
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	private final GeneratorProduct product;
	private final int perCycle;
	private final int capacity;
	private final boolean wireless;

	/** {@code wireless}: has an antenna on top that listens on a Redstone Link frequency. */
	public CobbleGeneratorBlock(Properties properties, GeneratorProduct product, int perCycle, int capacity, boolean wireless) {
		super(properties);
		this.product = product;
		this.perCycle = perCycle;
		this.capacity = capacity;
		this.wireless = wireless;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
	}

	public GeneratorProduct product() {
		return product;
	}

	/** Blocks made each time the drill-equivalent "breaks" a block. */
	public int perCycle() {
		return perCycle;
	}

	/** How many blocks it holds before it stops. */
	public int capacity() {
		return capacity;
	}

	public boolean isWireless() {
		return wireless;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, POWERED));
	}

	/** Window towards the player, like a furnace; power then has to come in from behind. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Only the back takes a shaft. */
	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING).getOpposite();
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

	/** A redstone signal switches it off (no cobblestone, no stress). */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos neighbourPos,
								   boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighbour, neighbourPos, movedByPiston);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof CobbleGeneratorBlockEntity be)
			be.setWiredSignal(level.hasNeighborSignal(pos));
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		// Moved blocks (contraptions, Aeronautics physics objects) keep their cobblestone
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CobbleGeneratorBlockEntity be)
			be.dropContents();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof CobbleGeneratorBlockEntity be ? be.getComparatorOutput() : 0;
	}

	@Override
	public Class<CobbleGeneratorBlockEntity> getBlockEntityClass() {
		return CobbleGeneratorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CobbleGeneratorBlockEntity> getBlockEntityType() {
		return ModBlockEntities.COBBLE_GENERATOR.get();
	}
}
