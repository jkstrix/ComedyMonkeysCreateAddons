package com.comedymonkey.createaddons.content.light;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.Wrenches;
import com.comedymonkey.createaddons.content.line.PowerLineBlock;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Light Strip (design/specs/electric-lights.md, P6, P7): a thin lit channel lying on the face of a block, one per face,
 * up to all six in one block, joining its neighbours and turning corners like a Power Line wire (StripLinks). Strips that
 * join are one run: a Power Line touching any piece feeds the whole run, and only the run (LightStripBlockEntity). A dye
 * colours the whole run, a water bucket washes it and the wrench steps its brightness, all of it at once (D24, P26);
 * sneaking, just this block. One block of strips is one light: lit or dark, one colour, one brightness.
 */
public class LightStripBlock extends BaseEntityBlock implements IWrenchable, SimpleWaterloggedBlock, LitBlock {
	public static final MapCodec<LightStripBlock> CODEC = simpleCodec(LightStripBlock::new);
	/** Per direction: a strip lies against that face of the block (the same properties as a Power Line's wires). */
	public static final Map<Direction, BooleanProperty> FACES = PowerLineBlock.FACES;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	/** Light level at full brightness (P1; raised from 10 after a look in game, 2026-10-28). */
	public static final int LEVEL = 12;

	public LightStripBlock(Properties properties) {
		super(properties);
		BlockState state = stateDefinition.any().setValue(LightBlock.LIGHT, 0).setValue(LightBlock.COLOR, LightDye.NONE)
			.setValue(WATERLOGGED, false);
		for (BooleanProperty p : FACES.values())
			state = state.setValue(p, false);
		registerDefaultState(state);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LightBlock.LIGHT, LightBlock.COLOR, WATERLOGGED);
		FACES.values().forEach(builder::add);
	}

	public static boolean hasFace(BlockState state, Direction face) {
		return state.getBlock() instanceof LightStripBlock && state.getValue(FACES.get(face));
	}

	public static int faceCount(BlockState state) {
		int n = 0;
		for (Direction d : Direction.values())
			if (hasFace(state, d))
				n++;
		return n;
	}

	/** The light it casts: 12 at full, less by brightness step, none when dark. */
	public static int lightLevel(BlockState state) {
		int light = state.getValue(LightBlock.LIGHT);
		return light == 0 ? 0 : Math.max(1, (int) Math.round(LEVEL * LightKind.STEPS[light - 1]));
	}

	@Override
	public double draw(int step) {
		return CMConfig.SERVER.lightRunFePerTick() * LightKind.STEPS[step];
	}

	// ---------- placing on faces ----------

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockState here = level.getBlockState(pos);
		BlockState base = here.is(this) ? here : defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
		// against the face that was clicked first, else the nearest face the player looks towards
		Direction clicked = ctx.getClickedFace().getOpposite();
		if (!hasFace(base, clicked) && supports(level, pos, clicked))
			return base.setValue(FACES.get(clicked), true);
		for (Direction face : ctx.getNearestLookingDirections())
			if (!hasFace(base, face) && supports(level, pos, face))
				return base.setValue(FACES.get(face), true);
		return null;
	}

	/** Another strip can go on a face of this block that hasn't one yet. */
	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
		if (!ctx.getItemInHand().is(asItem()))
			return false;
		for (Direction face : Direction.values())
			if (!hasFace(state, face) && supports(ctx.getLevel(), ctx.getClickedPos(), face))
				return true;
		return false;
	}

	/** A strip can lie against face f if the block on that side has a sturdy face there (an encased line counts, D11). */
	static boolean supports(LevelReader level, BlockPos pos, Direction face) {
		BlockPos support = pos.relative(face);
		BlockState state = level.getBlockState(support);
		return !(state.getBlock() instanceof LightStripBlock) && LightBlock.sturdy(level, support, face.getOpposite());
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		for (Direction face : Direction.values())
			if (hasFace(state, face) && supports(level, pos, face))
				return true;
		return false;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		// a strip whose support went goes with it and drops: the last through the loot table, the others by hand
		if (hasFace(state, dir) && !supports(level, pos, dir)) {
			BlockState left = state.setValue(FACES.get(dir), false);
			if (faceCount(left) == 0 && !state.getValue(WATERLOGGED))
				return Blocks.AIR.defaultBlockState();
			if (level instanceof Level l && !l.isClientSide)
				Block.popResource(l, pos, new ItemStack(this));
			return faceCount(left) == 0 ? Blocks.WATER.defaultBlockState() : left;
		}
		return state;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	/** Strips added or taken away: the runs are worked out again. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, level, pos, old, moving);
		if (!old.is(this) || faces(old) != faces(state)) {
			LightStripBlockEntity.runsChanged();
			LightMounts.relinkBehind(level, state, pos);
			if (old.is(this))
				LightMounts.relinkBehind(level, old, pos);
		}
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!newState.is(this))
			LightStripBlockEntity.runsChanged();
		super.onRemove(state, level, pos, newState, moving);
		if (!newState.is(this))
			LightMounts.relinkBehind(level, state, pos);
	}

	private static int faces(BlockState state) {
		int bits = 0;
		for (Direction d : Direction.values())
			if (hasFace(state, d))
				bits |= 1 << d.ordinal();
		return bits;
	}

	// ---------- shape ----------

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		VoxelShape out = Shapes.empty();
		for (Direction face : Direction.values())
			if (hasFace(state, face))
				out = Shapes.or(out, plate(face));
		return out;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return Shapes.empty();
	}

	/** A strip's outline on face f: a 2 px plate over the face. */
	private static VoxelShape plate(Direction f) {
		return switch (f) {
			case DOWN -> Block.box(0, 0, 0, 16, 2, 16);
			case UP -> Block.box(0, 14, 0, 16, 16, 16);
			case NORTH -> Block.box(0, 0, 0, 16, 16, 2);
			case SOUTH -> Block.box(0, 0, 14, 16, 16, 16);
			case WEST -> Block.box(0, 0, 0, 2, 16, 16);
			case EAST -> Block.box(14, 0, 0, 16, 16, 16);
		};
	}

	// ---------- turning ----------

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		BlockState out = state;
		for (Direction d : Direction.values())
			out = out.setValue(FACES.get(rotation.rotate(d)), state.getValue(FACES.get(d)));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		BlockState out = state;
		for (Direction d : Direction.values())
			out = out.setValue(FACES.get(mirror.mirror(d)), state.getValue(FACES.get(d)));
		return out;
	}

	// ---------- using it ----------

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
											  BlockHitResult hit) {
		if (Wrenches.isWrench(stack))
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		if (!(level.getBlockEntity(pos) instanceof LightStripBlockEntity be) || !player.mayBuild())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		DyeColor dye;
		if (stack.getItem() instanceof DyeItem d)
			dye = d.getDyeColor();
		else if (stack.is(Items.WATER_BUCKET))
			dye = null;
		else
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		// the client can't know the run (it only learns of changes the server makes), so it never uses the item itself
		if (level.isClientSide)
			return ItemInteractionResult.SUCCESS;
		// the whole run (D24), or sneaking, just this block; the dye isn't used and the bucket stays full
		boolean changed = false;
		for (LightStripBlockEntity piece : player.isShiftKeyDown() ? java.util.List.of(be) : be.run())
			if (piece.dye() != dye) {
				changed = true;
				if (!level.isClientSide)
					piece.setDye(dye);
			}
		if (!changed)
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide)
			level.playSound(null, pos, dye != null ? SoundEvents.DYE_USE : SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, dye != null ? 0.7f : 0.5f,
				dye != null ? 1.1f : 1.2f);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/** The wrench steps the whole run's brightness together (P26); sneaking it picks the block up, as Create's do. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext ctx) {
		Level level = ctx.getLevel();
		if (!level.isClientSide && level.getBlockEntity(ctx.getClickedPos()) instanceof LightStripBlockEntity be) {
			int to = (be.step() + 1) % LightKind.STEPS.length;
			be.stepTo(to, true);
			for (LightStripBlockEntity piece : be.run())
				if (piece != be)
					piece.stepTo(to, false);
		}
		return InteractionResult.SUCCESS;
	}

	// ---------- block entity and ticks ----------

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LightStripBlockEntity(pos, state);
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
