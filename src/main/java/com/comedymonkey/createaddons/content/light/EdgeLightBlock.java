package com.comedymonkey.createaddons.content.light;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.Wrenches;
import com.comedymonkey.createaddons.content.line.WireLink;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An Edge Light (design/specs/electric-lights.md, P6, P7): aluminium trim with a lit chamfer along the edges of a block's
 * faces, picked by where you click (the edge nearest the click, on the face clicked), several edges in one block. Its
 * edges are in its block entity (EdgeLinks). Trim that joins is one run, fed through any piece a Power Line touches and
 * dyed, washed and stepped all at once, as Light Strips are; sneak for one block. One block is one light.
 */
public class EdgeLightBlock extends BaseEntityBlock implements IWrenchable, SimpleWaterloggedBlock, LitBlock {
	public static final MapCodec<EdgeLightBlock> CODEC = simpleCodec(EdgeLightBlock::new);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public EdgeLightBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LightBlock.LIGHT, 0).setValue(LightBlock.COLOR, LightDye.NONE).setValue(WATERLOGGED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LightBlock.LIGHT, LightBlock.COLOR, WATERLOGGED);
	}

	/** The light it casts: as a Light Strip's, 12 at full. */
	public static int lightLevel(BlockState state) {
		return LightStripBlock.lightLevel(state);
	}

	@Override
	public double draw(int step) {
		return CMConfig.SERVER.lightRunFePerTick() * LightKind.STEPS[step];
	}

	// ---------- placing on edges ----------

	/** The face and side of the edge a placement means: the face nearest the click that can hold trim, its nearest edge. */
	public static @Nullable Direction[] edgeFor(BlockPlaceContext ctx) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Vec3 h = ctx.getClickLocation().subtract(Vec3.atLowerCornerOf(pos));
		Direction face = null;
		double best = Double.MAX_VALUE;
		for (Direction f : Direction.values()) {
			double d = distance(h, f);
			if (d < best && supports(level, pos, f)) {
				best = d;
				face = f;
			}
		}
		if (face == null)
			return null;
		Direction side = null;
		best = Double.MAX_VALUE;
		for (Direction s : WireLink.across(face)) {
			double d = distance(h, s);
			if (d < best) {
				best = d;
				side = s;
			}
		}
		return new Direction[] {face, side};
	}

	/** How far a point in the block is from its face d. */
	private static double distance(Vec3 h, Direction d) {
		double v = Math.clamp(h.get(d.getAxis()), 0, 1);
		return d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 - v : v;
	}

	/** Trim can lie against face f if the block on that side has a sturdy face there (an encased line counts). */
	static boolean supports(LevelReader level, BlockPos pos, Direction face) {
		BlockPos support = pos.relative(face);
		return !(level.getBlockState(support).getBlock() instanceof EdgeLightBlock) && LightBlock.sturdy(level, support, face.getOpposite());
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Direction[] edge = edgeFor(ctx);
		if (edge == null || EdgeLinks.has(EdgeLinks.edgesAt(level, pos), edge[0], edge[1]))
			return null;
		BlockState here = level.getBlockState(pos);
		return here.is(this) ? here : defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
	}

	/** Another edge can go in this block if the edge the click means hasn't trim yet. */
	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
		if (!ctx.getItemInHand().is(asItem()))
			return false;
		Direction[] edge = edgeFor(ctx);
		return edge != null && !EdgeLinks.has(EdgeLinks.edgesAt(ctx.getLevel(), ctx.getClickedPos()), edge[0], edge[1]);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		int edges = EdgeLinks.edgesAt(level, pos);
		if (edges == 0)
			return true;
		for (Direction face : Direction.values())
			if (EdgeLinks.onFace(edges, face) && supports(level, pos, face))
				return true;
		return false;
	}

	/** Trim whose block went comes off with it and drops; with none left the block goes. */
	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (level instanceof Level l && !l.isClientSide && l.getBlockEntity(pos) instanceof EdgeLightBlockEntity be
			&& EdgeLinks.onFace(be.edges(), dir) && !supports(level, pos, dir)) {
			int gone = be.removeEdgesOn(dir);
			if (be.edges() == 0)
				return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
			for (int i = 0; i < gone; i++)
				Block.popResource(l, pos, new ItemStack(this));
		}
		return state;
	}

	/** It drops one for each edge it has. */
	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		List<ItemStack> drops = super.getDrops(state, params);
		if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof EdgeLightBlockEntity be)
			for (ItemStack stack : drops)
				if (stack.is(asItem()))
					stack.setCount(Math.max(1, Integer.bitCount(be.edges())));
		return drops;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		boolean gone = !newState.is(this);
		if (gone)
			LightMounts.relinkBehind(level, state, pos);
		super.onRemove(state, level, pos, newState, moving);
		if (gone)
			RunLightBlockEntity.runsChanged();
	}

	// ---------- shape ----------

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		VoxelShape shape = EdgeLinks.shape(EdgeLinks.edgesAt(level, pos));
		return shape.isEmpty() ? Block.box(6, 6, 6, 10, 10, 10) : shape;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	// ---------- using it ----------

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
											  BlockHitResult hit) {
		if (Wrenches.isWrench(stack))
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		return RunLightBlockEntity.dyeRun(stack, level, pos, player);
	}

	/** The wrench steps the whole run's brightness (P26); sneaking it picks the block up, as Create's do. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext ctx) {
		RunLightBlockEntity.stepRun(ctx.getLevel(), ctx.getClickedPos());
		return InteractionResult.SUCCESS;
	}

	// ---------- block entity and ticks ----------

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EdgeLightBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof LightBlockEntity be)
			be.wake();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return !state.getValue(WATERLOGGED);
	}
}
