package com.comedymonkey.createaddons.content.light;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A light's remote spot (design/specs/electric-lights.md, P13): the light a Floodlight puts where its beam lands. Invisible,
 * in air or water only, never picked, targeted or broken by a player, and gone when anything is put in its place. It
 * belongs to the light that placed it (LightSpotBlockEntity) and looks every two seconds that it still does, so one left
 * behind (its light broken in a way that didn't take it) goes out by itself.
 */
public class LightSpotBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
	public static final MapCodec<LightSpotBlock> CODEC = simpleCodec(LightSpotBlock::new);
	public static final IntegerProperty LIGHT = IntegerProperty.create("light", 1, 15);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	/** How often a spot looks for the light it belongs to (ticks). */
	private static final int CHECK = 40;

	public LightSpotBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIGHT, 15).setValue(LightBlock.COLOR, LightDye.NONE).setValue(WATERLOGGED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIGHT, LightBlock.COLOR, WATERLOGGED);
	}

	/**
	 * Puts (or keeps, at this light and colour) the spot of the light at {@code owner} at pos: only into air, water, or its
	 * own spot already there. False if there's no room.
	 */
	public static boolean place(Level level, BlockPos pos, BlockPos owner, int light, @Nullable DyeColor dye) {
		if (!level.isLoaded(pos))
			return false;
		BlockState there = level.getBlockState(pos);
		boolean ours = there.getBlock() instanceof LightSpotBlock && owner.equals(owner(level, pos));
		boolean water = ours ? there.getValue(WATERLOGGED) : there.is(Blocks.WATER) && level.getFluidState(pos).isSource();
		if (!ours && !there.isAir() && !water)
			return false;
		BlockState spot = ModBlocks.LIGHT_SPOT.get().defaultBlockState().setValue(LIGHT, Math.max(1, Math.min(15, light)))
			.setValue(LightBlock.COLOR, LightDye.of(dye)).setValue(WATERLOGGED, water);
		if (ours && there == spot)
			return true;
		level.setBlock(pos, spot, Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof LightSpotBlockEntity be)
			be.setOwner(owner);
		return true;
	}

	/** Takes away the spot at pos, if it's the light at {@code owner}'s (leaving its water). */
	public static void remove(Level level, BlockPos pos, BlockPos owner) {
		if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof LightSpotBlock && owner.equals(owner(level, pos)))
			clear(level, pos, level.getBlockState(pos));
	}

	private static void clear(Level level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
	}

	private static @Nullable BlockPos owner(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LightSpotBlockEntity be ? be.owner() : null;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, level, pos, old, moving);
		if (!old.is(this))
			level.scheduleTick(pos, this, CHECK);
	}

	/** Still its light's spot (or its light is out of reach, unloaded): it stays; else it goes out. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockPos owner = owner(level, pos);
		boolean kept = owner != null && (!level.isLoaded(owner)
			|| level.getBlockEntity(owner) instanceof LightBlockEntity light && light.ownsSpot(pos));
		if (kept)
			level.scheduleTick(pos, this, CHECK);
		else
			clear(level, pos, state);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return state;
	}

	/** Any fluid flows in: water fills it as it would air (it stays, waterlogged); anything else takes its place. */
	@Override
	public boolean canPlaceLiquid(@Nullable net.minecraft.world.entity.player.Player player, BlockGetter level, BlockPos pos, BlockState state,
								  net.minecraft.world.level.material.Fluid fluid) {
		return !(state.getValue(WATERLOGGED) && fluid == Fluids.WATER);
	}

	@Override
	public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
		if (fluid.getType() == Fluids.WATER && !state.getValue(WATERLOGGED))
			return SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluid);
		if (!level.isClientSide())
			level.setBlock(pos, fluid.createLegacyBlock(), Block.UPDATE_ALL);
		return true;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LightSpotBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return !state.getValue(WATERLOGGED);
	}
}
