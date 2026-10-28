package com.comedymonkey.createaddons.content.light;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.line.PowerLineBlock;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * ceiling. The top block has the lamp's rose against the ceiling; only it hangs from the ceiling, the rest from the cable
 * above. It hangs only down to more cable or its lamp: take any part away and the whole hanging lamp comes down, the lamp
 * dropping and each block of cable giving its Power Line back.
 */
public class HangingCableBlock extends Block implements SimpleWaterloggedBlock, SpecialBlockItemRequirement {
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

	/**
	 * The hung lamp at the foot of the cable whose rose is at pos, if the rose is against {@code ceiling}: a wire meeting the
	 * rose flat on that ceiling, or the line that ceiling is, feeds the lamp down its cable. Null if pos is no such rose.
	 */
	public static @Nullable BlockPos lampUnder(BlockGetter level, BlockPos pos, BlockPos ceiling) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof HangingCableBlock) || state.getValue(ROSE) == Rose.NONE || !pos.above().equals(ceiling))
			return null;
		return foot(level, pos);
	}

	/** The hung lamp at the foot of the cable through pos, or null. (Straight down: never into another chunk.) */
	public static @Nullable BlockPos foot(BlockGetter level, BlockPos pos) {
		BlockPos p = pos.below();
		for (int i = 0; i < HangingLampBlock.MAX_DROP && level.getBlockState(p).getBlock() instanceof HangingCableBlock; i++)
			p = p.below();
		BlockState lamp = level.getBlockState(p);
		return lamp.getBlock() instanceof HangingLampBlock && lamp.getValue(HangingLampBlock.HUNG) ? p : null;
	}

	/** The rose under the ceiling (as a ceiling light hangs: LightBlock.hangsUnder), the rest from cable; down to cable or its lamp. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState above = level.getBlockState(pos.above()), below = level.getBlockState(pos.below());
		boolean held = above.getBlock() instanceof HangingCableBlock
			|| state.getValue(ROSE) != Rose.NONE && LightBlock.hangsUnder(level, pos.above());
		boolean holds = below.getBlock() instanceof HangingCableBlock
			|| below.getBlock() instanceof HangingLampBlock && below.getValue(HangingLampBlock.HUNG);
		return held && holds;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		// it looks again a tick later, so a hanging lamp set down a block at a time (a Create contraption landing, a
		// schematicannon) isn't brought down before its last block is in
		if (!canSurvive(state, level, pos))
			level.scheduleTick(pos, this, 1);
		return state;
	}

	/** Still nothing to hang from, or nothing hanging: it comes down, giving its wire back (the game puts its water back). */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!canSurvive(state, level, pos))
			level.destroyBlock(pos, true);
	}

	/** The top of a cable taken away: the wires meeting its rose and on its ceiling let the lamp go. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		super.onRemove(state, level, pos, newState, moving);
		if (!level.isClientSide && !newState.is(this) && state.getValue(ROSE) != Rose.NONE) {
			PowerLineBlock.refreshAround(level, pos.above());
			PowerLineBlock.refreshAround(level, pos);
		}
	}

	/** Picked, it's the Power Line it was lowered with. */
	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(ModItems.COPPER_POWER_LINE.get());
	}

	/** A schematicannon prints each block of it from a Power Line. */
	@Override
	public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity be) {
		return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ModItems.COPPER_POWER_LINE.get());
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
