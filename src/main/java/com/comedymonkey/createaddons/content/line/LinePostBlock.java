package com.comedymonkey.createaddons.content.line;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Line Post: a wooden post with a ceramic insulator on top, for hanging Power Line spans. It has no tier. Posts stack
 * into a pole of any height, pointing out of whatever face they stand on; the top block (the head) carries the
 * insulator and up to four branches, each a crossarm with an insulator of its own. A wire running under the foot joins
 * the head's top insulator. A pole joins the cables on it of one colour (brass feeding copper of its colour).
 */
public class LinePostBlock extends BaseEntityBlock implements IWrenchable, SimpleWaterloggedBlock {
	public static final MapCodec<LinePostBlock> CODEC = simpleCodec(LinePostBlock::new);
	/** The way the post points: out of the face it stands on. */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final EnumProperty<PostRole> ROLE = EnumProperty.create("role", PostRole.class);
	/** Branches on the head, by side in the post's own frame (as if it stood upright). */
	public static final Map<Direction, BooleanProperty> BRANCHES = new EnumMap<>(Direction.class);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	static {
		for (Direction d : Direction.Plane.HORIZONTAL)
			BRANCHES.put(d, BooleanProperty.create(d.getSerializedName()));
	}

	public LinePostBlock(Properties properties) {
		super(properties);
		BlockState state = stateDefinition.any().setValue(FACING, Direction.UP).setValue(ROLE, PostRole.SINGLE).setValue(WATERLOGGED, false);
		for (BooleanProperty p : BRANCHES.values())
			state = state.setValue(p, false);
		registerDefaultState(state);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ROLE, WATERLOGGED);
		BRANCHES.values().forEach(builder::add);
	}

	// ---------- the post's frame ----------

	/** Turns a vector from the post's own frame (standing upright, about the block's middle) into the world's. */
	static Vector3f toWorld(Vector3f local, Direction facing) {
		Matrix3f m = new Matrix3f();
		switch (facing) {
			case DOWN -> m.rotateX((float) Math.toRadians(-180));
			case NORTH -> m.rotateX((float) Math.toRadians(-90));
			case SOUTH -> m.rotateX((float) Math.toRadians(-270));
			case EAST -> m.rotateY((float) Math.toRadians(-90)).rotateX((float) Math.toRadians(-90));
			case WEST -> m.rotateY((float) Math.toRadians(-270)).rotateX((float) Math.toRadians(-90));
			default -> {}
		}
		return m.transform(new Vector3f(local));
	}

	/** The world direction a side of the post's own frame points to. */
	public static Direction worldSide(Direction local, Direction facing) {
		Vector3f v = toWorld(new Vector3f(local.getStepX(), local.getStepY(), local.getStepZ()), facing);
		return Direction.getNearest(v.x, v.y, v.z);
	}

	/** The side of the post's own frame that points to a world direction (null along the post). */
	@Nullable
	public static Direction localSide(Direction world, Direction facing) {
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (worldSide(d, facing) == world)
				return d;
		return null;
	}

	/** Where a span ties onto insulator i of the post at pos, in the world (0: the top; 1-4: the branches N, E, S, W). */
	public static Vec3 tie(BlockState state, BlockPos pos, int insulator) {
		Vector3f local = switch (insulator) {
			case 1 -> new Vector3f(8, 15.1f, 2.5f);
			case 2 -> new Vector3f(13.5f, 15.1f, 8);
			case 3 -> new Vector3f(8, 15.1f, 13.5f);
			case 4 -> new Vector3f(2.5f, 15.1f, 8);
			default -> new Vector3f(8, 16.5f, 8);
		};
		local.sub(8, 8, 8).div(16);
		Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.UP;
		Vector3f w = toWorld(local, facing);
		return Vec3.atCenterOf(pos).add(w.x, w.y, w.z);
	}

	/** The insulators a post has: the top one, and one per branch, if it's a head. */
	public static boolean hasInsulator(BlockState state, int insulator) {
		if (!(state.getBlock() instanceof LinePostBlock) || !state.getValue(ROLE).isHead())
			return false;
		return insulator == 0 || (insulator >= 1 && insulator <= 4 && state.getValue(BRANCHES.get(LinePostBlockEntity.branchSide(insulator))));
	}

	/** The insulator nearest a point (a click), of those the post has. */
	public static int nearestInsulator(BlockState state, BlockPos pos, Vec3 at) {
		int best = 0;
		double bestDist = Double.MAX_VALUE;
		for (int i = 0; i <= 4; i++) {
			if (!hasInsulator(state, i))
				continue;
			double d = tie(state, pos, i).distanceToSqr(at);
			if (d < bestDist) {
				bestDist = d;
				best = i;
			}
		}
		return best;
	}

	/** Walks up a pole from any of its blocks to the head. */
	@Nullable
	public static BlockPos head(LevelReader level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LinePostBlock))
			return null;
		Direction facing = state.getValue(FACING);
		BlockPos p = pos;
		for (int i = 0; i < 256; i++) {
			if (!level.hasChunkAt(p))
				return null;
			BlockState s = level.getBlockState(p);
			if (!(s.getBlock() instanceof LinePostBlock) || s.getValue(FACING) != facing)
				return null;
			if (s.getValue(ROLE).isHead())
				return p;
			p = p.relative(facing);
		}
		return null;
	}

	/** Walks down a pole to its foot. */
	@Nullable
	public static BlockPos foot(LevelReader level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LinePostBlock))
			return null;
		Direction facing = state.getValue(FACING);
		BlockPos p = pos;
		for (int i = 0; i < 256; i++) {
			if (!level.hasChunkAt(p))
				return null;
			BlockState s = level.getBlockState(p);
			if (!(s.getBlock() instanceof LinePostBlock) || s.getValue(FACING) != facing)
				return null;
			if (s.getValue(ROLE).isFoot())
				return p;
			p = p.relative(facing.getOpposite());
		}
		return null;
	}

	// ---------- placing and stacking ----------

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getClickedFace();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockPos below = pos.relative(facing.getOpposite());
		BlockState belowState = level.getBlockState(below);
		// a head with spans hung from it can't be built over: take them down first
		if (belowState.getBlock() instanceof LinePostBlock && belowState.getValue(FACING) == facing
			&& level.getBlockEntity(below) instanceof LinePostBlockEntity post && !post.spans().isEmpty())
			return null;
		BlockState state = defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
		state = withRole(state, level, pos);
		return canSurvive(state, level, pos) ? state : null;
	}

	private static BlockState withRole(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockState below = level.getBlockState(pos.relative(facing.getOpposite())), above = level.getBlockState(pos.relative(facing));
		boolean b = below.getBlock() instanceof LinePostBlock && below.getValue(FACING) == facing;
		boolean a = above.getBlock() instanceof LinePostBlock && above.getValue(FACING) == facing;
		return state.setValue(ROLE, PostRole.of(b, a));
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos support = pos.relative(facing.getOpposite());
		BlockState s = level.getBlockState(support);
		if (s.getBlock() instanceof LinePostBlock && s.getValue(FACING) == facing)
			return true;
		return s.isFaceSturdy(level, support, facing);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
									 BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (!canSurvive(state, level, pos)) {
			if (!state.getValue(WATERLOGGED))
				return Blocks.AIR.defaultBlockState();
			// standing in water the block turns to water, not air, so no loot: the post and its branches drop by hand
			if (level instanceof Level l && !l.isClientSide) {
				int posts = 1;
				for (BooleanProperty b : BRANCHES.values())
					if (state.getValue(b))
						posts++;
				Block.popResource(l, pos, new ItemStack(ModItems.LINE_POST.get(), posts));
			}
			return Blocks.WATER.defaultBlockState();
		}
		return withRole(state, level, pos);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (state != oldState && level instanceof ServerLevel server) {
			PowerLines.of(server).changed(pos);
			// the wires beside the foot see the post (or stop seeing it)
			PowerLineBlock.relinkAround(level, pos);
		}
		// put there by a command or a structure: its place in the pole is worked out next tick
		if (!oldState.is(this) && !level.isClientSide)
			level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
		BlockState next = withRole(state, level, pos);
		if (next != state)
			level.setBlock(pos, next, Block.UPDATE_ALL);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LinePostBlockEntity post)
			for (ItemStack wire : post.takeDownAll())
				Block.popResource(level, pos, wire);
		super.onRemove(state, level, pos, newState, movedByPiston);
		if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
			PowerLines.of(server).changed(pos);
			PowerLineBlock.relinkAround(level, pos);
		}
	}

	// ---------- use ----------

	/**
	 * A Line Post on the side of a head adds a branch there (sneak to place a post instead); shears take down the
	 * spans on the nearest insulator. Power Lines in hand hang spans (see {@link PowerLineItem}).
	 */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		if (stack.is(ModItems.LINE_POST.get()) && !player.isShiftKeyDown() && state.getValue(ROLE).isHead()) {
			Direction local = localSide(hit.getDirection(), state.getValue(FACING));
			if (local == null || state.getValue(BRANCHES.get(local)))
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide) {
				level.setBlock(pos, state.setValue(BRANCHES.get(local), true), Block.UPDATE_ALL);
				if (!player.getAbilities().instabuild)
					stack.shrink(1);
				level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 1.1f);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		if (stack.getItem() instanceof ShearsItem && level.getBlockEntity(pos) instanceof LinePostBlockEntity post) {
			int insulator = nearestInsulator(state, pos, hit.getLocation());
			if (post.spansAt(insulator).isEmpty())
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide) {
				for (ItemStack wire : post.takeDown(insulator))
					if (!player.getInventory().add(wire))
						Block.popResource(level, pos, wire);
				stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
				level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8f, 1.2f);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	/** The wrench takes a branch off (and its spans down), giving the post back. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!state.getValue(ROLE).isHead())
			return InteractionResult.PASS;
		int insulator = nearestInsulator(state, pos, context.getClickLocation());
		if (insulator == 0)
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (level.getBlockEntity(pos) instanceof LinePostBlockEntity post)
			for (ItemStack wire : post.takeDown(insulator))
				give(level, pos, context.getPlayer(), wire);
		level.setBlock(pos, state.setValue(BRANCHES.get(LinePostBlockEntity.branchSide(insulator)), false), Block.UPDATE_ALL);
		give(level, pos, context.getPlayer(), new ItemStack(ModItems.LINE_POST.get()));
		level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1f, 1.1f);
		return InteractionResult.SUCCESS;
	}

	private static void give(Level level, BlockPos pos, @Nullable Player player, ItemStack item) {
		if (player == null || !player.getInventory().add(item))
			Block.popResource(level, pos, item);
	}

	// ---------- turning (structures, schematics, contraptions) ----------

	/** The post turns with its facing; an upright post's branches (on world sides) turn too, a lying one's go with it. */
	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		Direction facing = state.getValue(FACING);
		BlockState out = state.setValue(FACING, rotation.rotate(facing));
		if (facing.getAxis() == Direction.Axis.Y)
			for (Direction d : Direction.Plane.HORIZONTAL)
				out = out.setValue(BRANCHES.get(rotation.rotate(d)), state.getValue(BRANCHES.get(d)));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		Direction facing = state.getValue(FACING);
		BlockState out = state.setValue(FACING, mirror.mirror(facing));
		if (facing.getAxis() == Direction.Axis.Y)
			for (Direction d : Direction.Plane.HORIZONTAL)
				out = out.setValue(BRANCHES.get(mirror.mirror(d)), state.getValue(BRANCHES.get(d)));
		return out;
	}

	// ---------- shape ----------

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		PostRole role = state.getValue(ROLE);
		Direction facing = state.getValue(FACING);
		VoxelShape shape = box(6.5, role.isFoot() ? 1.5 : 0, 6.5, 9.5, role.isHead() ? 10.5 : 16, 9.5, facing);
		if (role.isFoot())
			shape = Shapes.or(shape, box(5, 0, 5, 11, 1.5, 11, facing));
		if (role.isHead()) {
			shape = Shapes.or(shape, box(5.5, 10.5, 5.5, 10.5, 16, 10.5, facing));
			for (Map.Entry<Direction, BooleanProperty> b : BRANCHES.entrySet())
				if (state.getValue(b.getValue()))
					shape = Shapes.or(shape, switch (b.getKey()) {
						case NORTH -> box(6.5, 7, 0.5, 9.5, 15, 6.5, facing);
						case SOUTH -> box(6.5, 7, 9.5, 9.5, 15, 15.5, facing);
						case WEST -> box(0.5, 7, 6.5, 6.5, 15, 9.5, facing);
						default -> box(9.5, 7, 6.5, 15.5, 15, 9.5, facing);
					});
		}
		return shape;
	}

	/** A box in the post's own frame, turned into the world's. */
	private static VoxelShape box(double x0, double y0, double z0, double x1, double y1, double z1, Direction facing) {
		Vector3f a = toWorld(new Vector3f((float) x0 / 16 - 0.5f, (float) y0 / 16 - 0.5f, (float) z0 / 16 - 0.5f), facing);
		Vector3f b = toWorld(new Vector3f((float) x1 / 16 - 0.5f, (float) y1 / 16 - 0.5f, (float) z1 / 16 - 0.5f), facing);
		return Shapes.box(Math.min(a.x, b.x) + 0.5, Math.min(a.y, b.y) + 0.5, Math.min(a.z, b.z) + 0.5,
			Math.max(a.x, b.x) + 0.5, Math.max(a.y, b.y) + 0.5, Math.max(a.z, b.z) + 0.5);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LinePostBlockEntity(pos, state);
	}
}
