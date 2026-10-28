package com.comedymonkey.createaddons.content.light;

import com.comedymonkey.createaddons.content.line.PowerLineBlock;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The cable a Pendant Lamp or Chandelier is lowered on (D19): a thin flex, no collision, carrying the lamp's power from the
 * ceiling. The top block has the lamp's rose against the ceiling. It hangs only between the ceiling (or more cable) and
 * more cable or its lamp: take any part away and the whole hanging lamp comes down, the lamp dropping and each block of
 * cable giving its Power Line back.
 */
public class HangingCableBlock extends Block implements SimpleWaterloggedBlock {
	public static final MapCodec<HangingCableBlock> CODEC = simpleCodec(HangingCableBlock::new);
	public static final EnumProperty<Rose> ROSE = EnumProperty.create("rose", Rose.class);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final VoxelShape SHAPE = Block.box(7, 0, 7, 9, 16, 9);

	/** The rose at the top of the cable, against the ceiling: none further down. */
	public enum Rose implements StringRepresentable {
		NONE("none"),
		STEEL("steel"),
		BRASS("brass");

		private final String name;

		Rose(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public HangingCableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ROSE, Rose.NONE).setValue(WATERLOGGED, false));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROSE, WATERLOGGED);
	}

	/** Hanging from the ceiling (or cable above) down to cable or its lamp. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState above = level.getBlockState(pos.above()), below = level.getBlockState(pos.below());
		boolean held = above.getBlock() instanceof HangingCableBlock || LightBlock.sturdy(level, pos.above(), Direction.DOWN);
		boolean holds = below.getBlock() instanceof HangingCableBlock
			|| below.getBlock() instanceof HangingLampBlock && below.getValue(HangingLampBlock.HUNG);
		return held && holds;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (!canSurvive(state, level, pos))
			return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
		return state;
	}

	/** The top of a cable taken away: the wires round its ceiling let the lamp go. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		super.onRemove(state, level, pos, newState, moving);
		if (!level.isClientSide && !newState.is(this) && state.getValue(ROSE) != Rose.NONE)
			PowerLineBlock.refreshAround(level, pos.above());
	}

	/** Picked, it's the Power Line it was lowered with. */
	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(ModItems.COPPER_POWER_LINE.get());
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return !state.getValue(WATERLOGGED);
	}
}
