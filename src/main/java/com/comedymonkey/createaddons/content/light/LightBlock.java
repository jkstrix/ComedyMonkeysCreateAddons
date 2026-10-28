package com.comedymonkey.createaddons.content.light;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.Wrenches;
import com.comedymonkey.createaddons.content.light.LightKind.Mount;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One Electric Light (design/specs/electric-lights.md): lit while its block entity has power, at the light level of its
 * brightness step (LIGHT is 0 when dark, else its step + 1; the light it casts follows from it). A dye in hand colours
 * its glow without being used (COLOR, in the state so coloured-light mods can colour its light); a water bucket washes it
 * plain; the wrench steps its brightness; breaking it drops it plain.
 */
public class LightBlock extends BaseEntityBlock implements IWrenchable, SimpleWaterloggedBlock, LitBlock {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	/** Dark (0), or lit at brightness step {@code LIGHT - 1}: Full, Bright, Dim, Glow. */
	public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, LightKind.STEPS.length);
	public static final EnumProperty<LightDye> COLOR = EnumProperty.create("color", LightDye.class);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private final LightKind kind;
	private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

	public LightBlock(LightKind kind, Properties properties) {
		super(properties);
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(FACING, defaultFacing(kind)).setValue(LIGHT, 0).setValue(COLOR, LightDye.NONE)
			.setValue(WATERLOGGED, false));
		for (Direction d : Direction.values())
			shapes.put(d, turned(kind, d));
	}

	public LightKind kind() {
		return kind;
	}

	@Override
	public double draw(int step) {
		return kind.draw(step);
	}

	@Override
	public boolean warms() {
		return kind.warms();
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(p -> new LightBlock(kind, p));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIGHT, COLOR, WATERLOGGED);
	}

	/**
	 * The light a light of this kind casts on the world: its level at its brightness step while lit, else none. Given the
	 * kind, as it's asked for every state while the block is still being made (before it knows its own).
	 */
	public static int lightLevel(LightKind kind, BlockState state) {
		int light = state.getValue(LIGHT);
		return light == 0 ? 0 : kind.level(light - 1);
	}

	/** The way a light that doesn't turn always faces: down for ceiling lights, up for the rest. */
	private static Direction defaultFacing(LightKind kind) {
		return switch (kind.mount()) {
			case CEILING -> Direction.DOWN;
			case WALL, ANY -> Direction.SOUTH;
			case TOP_FACING -> Direction.NORTH;
			default -> Direction.UP;
		};
	}

	// ---------- placing and staying up ----------

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockState base = defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
		switch (kind.mount()) {
			case WALL, ANY -> {
				// on the face clicked if it can hold it there, else the nearest face it can
				if (kind.mount() == Mount.ANY || ctx.getClickedFace().getAxis().isHorizontal()) {
					BlockState s = base.setValue(FACING, ctx.getClickedFace());
					if (s.canSurvive(level, pos))
						return s;
				}
				for (Direction d : ctx.getNearestLookingDirections()) {
					if (kind.mount() == Mount.WALL && !d.getAxis().isHorizontal())
						continue;
					BlockState s = base.setValue(FACING, d.getOpposite());
					if (s.canSurvive(level, pos))
						return s;
				}
				return null;
			}
			case TOP_FACING -> {
				BlockState s = base.setValue(FACING, ctx.getHorizontalDirection());
				return s.canSurvive(level, pos) ? s : null;
			}
			default -> {
				return base.canSurvive(level, pos) ? base : null;
			}
		}
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return switch (kind.mount()) {
			case WALL, ANY -> {
				Direction facing = state.getValue(FACING);
				yield sturdy(level, pos.relative(facing.getOpposite()), facing);
			}
			case CEILING -> encased(level, pos.above()) || canSupportCenter(level, pos.above(), Direction.DOWN);
			case FLOOR, TOP, TOP_FACING -> encased(level, pos.below()) || canSupportCenter(level, pos.below(), Direction.UP);
			case FULL -> true;
		};
	}

	/**
	 * The block at {@code pos} has a sturdy face towards {@code face}, or is a line encased in a block (D11: a light fixed to
	 * the block its feed is hidden in; the line's shape isn't the block's, so the game wouldn't call it sturdy).
	 */
	static boolean sturdy(LevelReader level, BlockPos pos, Direction face) {
		return encased(level, pos) || level.getBlockState(pos).isFaceSturdy(level, pos, face);
	}

	private static boolean encased(LevelReader level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof com.comedymonkey.createaddons.content.line.LineBlockEntity line && line.encasing() != null;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (!canSurvive(state, level, pos))
			return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		return super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	/** The block it's fixed to (on its wall, ceiling or floor), or null for one that's a whole block itself. */
	public static @Nullable BlockPos support(BlockState state, BlockPos pos) {
		if (!(state.getBlock() instanceof LightBlock light))
			return null;
		BlockPos post = LampHeadBlock.post(state, pos);
		if (post != null)
			return post;
		return switch (light.kind.mount()) {
			case WALL, ANY -> pos.relative(state.getValue(FACING).getOpposite());
			case CEILING -> pos.above();
			case FLOOR, TOP, TOP_FACING -> pos.below();
			case FULL -> null;
		};
	}

	/** Placed or taken away: the wires round the block it's fixed to work their links out again (they may feed it through it). */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, level, pos, old, moving);
		if (!old.is(this))
			LightMounts.relinkBehind(level, state, pos);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		super.onRemove(state, level, pos, newState, moving);
		if (!newState.is(this))
			LightMounts.relinkBehind(level, state, pos);
	}

	// ---------- shape ----------

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapes.get(state.getValue(FACING));
	}

	/** Its outline turned to face {@code facing}: wall and face lights are built facing south, leaning heads north. */
	static VoxelShape turned(LightKind kind, Direction facing) {
		VoxelShape built = kind.shape();
		return switch (kind.mount()) {
			case WALL -> facing.getAxis().isHorizontal() ? turn(built, facing) : built;
			case ANY -> turn(built, facing);
			case TOP_FACING -> facing.getAxis().isHorizontal() ? turn(built, facing.getOpposite()) : built;
			default -> built;
		};
	}

	/** A shape built facing south, turned to face {@code to} (as the block model's blockstate rotation turns it). */
	static VoxelShape turn(VoxelShape shape, Direction to) {
		if (to == Direction.SOUTH)
			return shape;
		VoxelShape out = Shapes.empty();
		for (AABB b : shape.toAabbs()) {
			double[] a = map(to, b.minX, b.minY, b.minZ), c = map(to, b.maxX, b.maxY, b.maxZ);
			out = Shapes.or(out, Shapes.box(Math.min(a[0], c[0]), Math.min(a[1], c[1]), Math.min(a[2], c[2]),
				Math.max(a[0], c[0]), Math.max(a[1], c[1]), Math.max(a[2], c[2])));
		}
		return out.optimize();
	}

	private static double[] map(Direction to, double x, double y, double z) {
		return switch (to) {
			case WEST -> new double[] {1 - z, y, x};
			case NORTH -> new double[] {1 - x, y, 1 - z};
			case EAST -> new double[] {z, y, 1 - x};
			case UP -> new double[] {x, z, 1 - y};
			case DOWN -> new double[] {x, 1 - z, y};
			default -> new double[] {x, y, z};
		};
	}

	// ---------- turning with schematics and contraptions ----------

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	// ---------- using it ----------

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
											  BlockHitResult hit) {
		if (Wrenches.isWrench(stack))
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		if (!(level.getBlockEntity(pos) instanceof LightBlockEntity be) || !player.mayBuild())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (stack.getItem() instanceof DyeItem dye) {
			// the dye colours the glow and isn't used up (D5)
			if (be.dye() == dye.getDyeColor())
				return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide) {
				be.setDye(dye.getDyeColor());
				level.playSound(null, pos, net.minecraft.sounds.SoundEvents.DYE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 1.1f);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		if (stack.is(Items.WATER_BUCKET)) {
			// washed plain; the bucket stays full (D24)
			if (be.dye() == null)
				return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide) {
				be.setDye(null);
				level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.2f);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	/** The wrench steps the brightness (D22): Full, Bright, Dim, Glow, and round again. Lights never turn on the wrench. */
	@Override
	public net.minecraft.world.InteractionResult onWrenched(BlockState state, UseOnContext ctx) {
		Level level = ctx.getLevel();
		if (!level.isClientSide && level.getBlockEntity(ctx.getClickedPos()) instanceof LightBlockEntity be)
			be.stepBrightness();
		return net.minecraft.world.InteractionResult.SUCCESS;
	}

	// ---------- block entity and ticks ----------

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LightBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	/** Lights don't tick: a scheduled tick wakes one when its power is due to run out, or to come on (P3, P5). */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof LightBlockEntity be)
			be.wake();
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return !state.getValue(WATERLOGGED) && kind.mount() != Mount.FULL;
	}
}
