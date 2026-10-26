package com.comedymonkey.createaddons.content.line;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
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
 * A Power Line: thin cable that lies flat on the face of a block it was placed against (floors, walls, ceilings, any
 * of them in one block), clipped down, and folds round inside and outside corners to the next. Power runs along it from
 * anything that gives FE to anything that takes it (see {@link PowerLines}), and up Line Posts into hanging spans.
 */
public class PowerLineBlock extends BaseEntityBlock implements IWrenchable, SimpleWaterloggedBlock, LineBlock {
	public static final MapCodec<PowerLineBlock> CODEC = simpleCodec(p -> new PowerLineBlock(LineTier.COPPER, p));
	/** Per direction: a wire lies against that face of the block. */
	public static final Map<Direction, BooleanProperty> FACES = new EnumMap<>(Direction.class);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	static {
		for (Direction d : Direction.values())
			FACES.put(d, BooleanProperty.create(d.getSerializedName()));
	}

	private final LineTier tier;

	public PowerLineBlock(LineTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
		BlockState state = stateDefinition.any().setValue(WATERLOGGED, false);
		for (BooleanProperty p : FACES.values())
			state = state.setValue(p, false);
		registerDefaultState(state);
	}

	public LineTier tier() {
		return tier;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		FACES.values().forEach(builder::add);
		builder.add(WATERLOGGED);
	}

	public static boolean hasFace(BlockState state, Direction face) {
		return state.getBlock() instanceof PowerLineBlock && state.getValue(FACES.get(face));
	}

	public static int faceCount(BlockState state) {
		int n = 0;
		for (Direction d : Direction.values())
			if (hasFace(state, d))
				n++;
		return n;
	}

	@Override
	public boolean takes(Fitting fitting) {
		return fitting.fitsPowerLines();
	}

	/** A single wire running straight across its face: the run's direction. */
	@Nullable
	@Override
	public Direction sideFor(Fitting fitting, Level level, BlockPos pos, BlockState state) {
		return fitting == Fitting.SPLITTER ? junctionSide(level, pos, state) : straightSide(level, pos, state);
	}

	/** A Splitter sits on a junction: three or four ways on one face; its first branch, or null if it isn't one. */
	@Nullable
	public Direction junctionSide(Level level, BlockPos pos, BlockState state) {
		if (faceCount(state) != 1 || !(level.getBlockEntity(pos) instanceof PowerLineBlockEntity be))
			return null;
		for (Direction face : Direction.values())
			if (hasFace(state, face)) {
				java.util.List<Direction> branches = be.branches();
				return (be.links(face) & WireLink.RISE) == 0 && branches.size() >= 3 ? branches.get(0) : null;
			}
		return null;
	}

	@Override
	public Direction straightSide(Level level, BlockPos pos, BlockState state) {
		if (faceCount(state) != 1 || !(level.getBlockEntity(pos) instanceof PowerLineBlockEntity be))
			return null;
		for (Direction face : Direction.values()) {
			if (!hasFace(state, face))
				continue;
			// (a riser up through the middle leaves no room for a fitting)
			if ((be.links(face) & WireLink.RISE) != 0)
				return null;
			Direction first = null;
			int count = 0;
			for (Direction d : WireLink.across(face))
				if (be.link(face, d).connected()) {
					count++;
					if (first == null)
						first = d;
				}
			if (count == 0)
				return WireLink.across(face).get(0);
			if (count == 2 && be.link(face, first.getOpposite()).connected())
				return first;
			if (count == 1)
				return first;
		}
		return null;
	}

	// ---------- placing on faces ----------

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState here = level.getBlockState(pos);
		BlockState base = here.is(this) ? here : defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
		// against the face that was clicked first, else the nearest face the player looks towards
		Direction clicked = context.getClickedFace().getOpposite();
		if (!here.is(this) && !hasFace(base, clicked) && supports(level, pos, clicked))
			return base.setValue(FACES.get(clicked), true);
		for (Direction face : context.getNearestLookingDirections()) {
			if (hasFace(base, face) || !supports(level, pos, face))
				continue;
			return base.setValue(FACES.get(face), true);
		}
		return null;
	}

	/** Another wire can go on a face of this block that hasn't one yet. */
	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		if (!context.getItemInHand().is(asItem()))
			return false;
		// one block holds one colour of wire: another colour isn't added to it (it would take the block's colour over)
		if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LineBlockEntity be)
			|| be.dye() != context.getItemInHand().get(com.comedymonkey.createaddons.registry.ModDataComponents.LINE_DYE.get()))
			return false;
		for (Direction face : Direction.values())
			if (!hasFace(state, face) && supports(context.getLevel(), context.getClickedPos(), face))
				return true;
		return false;
	}

	/** A wire can lie against face f if the block on that side has a sturdy face there. */
	static boolean supports(LevelReader level, BlockPos pos, Direction face) {
		BlockPos support = pos.relative(face);
		BlockState state = level.getBlockState(support);
		return !(state.getBlock() instanceof PowerLineBlock) && state.isFaceSturdy(level, support, face.getOpposite());
	}

	/** One wire of this block as an item, with its dye. */
	private ItemStack wireItem(Level level, BlockPos pos) {
		return LineItem.of(this, level, pos);
	}

	/** Picked, a wire of its colour. */
	@Override
	public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos, Player player) {
		return LineItem.of(this, level, pos);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		for (Direction face : Direction.values())
			if (hasFace(state, face) && supports(level, pos, face))
				return true;
		return false;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
									 BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		// (a hole drilled through the block beside it stays while the block does)
		if (!(neighborState.getBlock() instanceof LineBlock) && level.getBlockEntity(pos) instanceof LineBlockEntity be
			&& !(be.joint(direction) == Joint.DRILLED && level instanceof Level l && drillable(l, neighborPos, direction)))
			be.forgetJoint(direction);
		// a wire whose support went goes with it, and drops: the last one through the loot table (as the block breaks),
		// the others (and the last, if it stood in water) by hand
		if (hasFace(state, direction) && !supports(level, pos, direction)) {
			BlockState left = state.setValue(FACES.get(direction), false);
			if (faceCount(left) == 0 && !state.getValue(WATERLOGGED))
				return Blocks.AIR.defaultBlockState();
			if (level instanceof Level l && !l.isClientSide)
				Block.popResource(l, pos, wireItem(l, pos));
			return faceCount(left) == 0 ? Blocks.WATER.defaultBlockState() : left;
		}
		return state;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moving) {
		super.neighborChanged(state, level, pos, neighbor, from, moving);
		if (level.isClientSide || !(level.getBlockEntity(pos) instanceof PowerLineBlockEntity be))
			return;
		be.relink();
		if (be.fitting() == Fitting.BREAKER)
			be.setPowered(level.hasNeighborSignal(pos));
	}

	/** A wire whose data was reloaded (see LineBlockEntity) works its links out again. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
		if (level.getBlockEntity(pos) instanceof PowerLineBlockEntity be)
			be.relink();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		relinkAround(level, pos);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (state != oldState && level instanceof ServerLevel server) {
			relinkAround(level, pos);
			PowerLines.of(server).changed(pos);
		}
	}

	/**
	 * This wire and every wire that could run to it (beside it, round its corners, and through a wall beside it) work
	 * their links out again.
	 */
	public static void relinkAround(Level level, BlockPos pos) {
		if (level.isClientSide)
			return;
		for (int dx = -1; dx <= 1; dx++)
			for (int dy = -1; dy <= 1; dy++)
				for (int dz = -1; dz <= 1; dz++)
					if (level.isLoaded(pos.offset(dx, dy, dz)) && level.getBlockEntity(pos.offset(dx, dy, dz)) instanceof PowerLineBlockEntity be)
						be.relink();
		for (Direction d : Direction.values())
			if (level.isLoaded(pos.relative(d, 2)) && level.getBlockEntity(pos.relative(d, 2)) instanceof PowerLineBlockEntity be)
				be.relink();
	}

	/**
	 * A block a cable can be threaded through: solid all through, not a line, and not a machine on either side (wires plug
	 * into those).
	 */
	public static boolean drillable(Level level, BlockPos pos, Direction from) {
		BlockState s = level.getBlockState(pos);
		return !(s.getBlock() instanceof LineBlock) && !(s.getBlock() instanceof LinePostBlock) && s.isCollisionShapeFullBlock(level, pos)
			&& level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, from.getOpposite()) == null
			&& level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, from) == null;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LineBlockEntity be) {
			if (be.fitting() != Fitting.NONE)
				Block.popResource(level, pos, new ItemStack(be.fitting().item()));
			if (be.encasing() != null)
				Block.popResource(level, pos, new ItemStack(be.encasing().getBlock()));
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
		if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
			PowerLines.of(server).changed(pos);
			relinkAround(level, pos);
		}
	}

	// ---------- turning (structures, schematics, contraptions) ----------

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

	// ---------- shape ----------

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (level.getBlockEntity(pos) instanceof PowerLineBlockEntity be)
			return be.encasing() != null ? Shapes.block() : be.wireShape();
		VoxelShape out = Shapes.empty();
		for (Direction face : Direction.values())
			if (hasFace(state, face))
				out = Shapes.or(out, plate(face, null));
		return out;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return level.getBlockEntity(pos) instanceof LineBlockEntity be && be.encasing() != null ? Shapes.block() : Shapes.empty();
	}

	@Override
	public VoxelShape lineShape(BlockState state, BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof PowerLineBlockEntity be ? be.wireShape() : Shapes.empty();
	}

	/** A 2 px tall strip lying on face f: the middle 6 px square, or the arm from it to edge d. */
	static VoxelShape plate(Direction f, @Nullable Direction d) {
		double a = 5, b = 11;
		double x0 = a, x1 = b, y0 = a, y1 = b, z0 = a, z1 = b;
		if (d != null) {
			switch (d) {
				case NORTH -> { z0 = 0; z1 = 6; }
				case SOUTH -> { z0 = 10; z1 = 16; }
				case WEST -> { x0 = 0; x1 = 6; }
				case EAST -> { x0 = 10; x1 = 16; }
				case DOWN -> { y0 = 0; y1 = 6; }
				case UP -> { y0 = 10; y1 = 16; }
			}
			// narrow across the run
			if (d.getAxis() != Direction.Axis.X && f.getAxis() != Direction.Axis.X) { x0 = 7; x1 = 9; }
			if (d.getAxis() != Direction.Axis.Y && f.getAxis() != Direction.Axis.Y) { y0 = 7; y1 = 9; }
			if (d.getAxis() != Direction.Axis.Z && f.getAxis() != Direction.Axis.Z) { z0 = 7; z1 = 9; }
		}
		switch (f) {
			case DOWN -> { y0 = 0; y1 = 2; }
			case UP -> { y0 = 14; y1 = 16; }
			case NORTH -> { z0 = 0; z1 = 2; }
			case SOUTH -> { z0 = 14; z1 = 16; }
			case WEST -> { x0 = 0; x1 = 2; }
			case EAST -> { x0 = 14; x1 = 16; }
		}
		return Block.box(x0, y0, z0, x1, y1, z1);
	}

	// ---------- use ----------

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		// another wire onto a free face of this block is placed as usual
		if (stack.is(asItem()) || !(level.getBlockEntity(pos) instanceof LineBlockEntity be))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return LineInteractions.useItem(stack, be, level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof LineBlockEntity be))
			return InteractionResult.PASS;
		return LineInteractions.useEmpty(be, level, pos, player);
	}

	/** A wrench opens a Splitter's priorities; otherwise it takes a fitting off, or works a joint. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LineBlockEntity be))
			return InteractionResult.PASS;
		if (be instanceof PowerLineBlockEntity wire && wire.fitting() == Fitting.SPLITTER) {
			if (context.getLevel() instanceof net.minecraft.server.level.ServerLevel server && context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)
				com.comedymonkey.createaddons.network.SplitterInfoPayload.send(server, player, wire);
			return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
		}
		Direction side = FluidLineBlock.nearestSide(context.getClickedPos(), context.getClickLocation(), context.getClickedFace());
		return LineInteractions.wrench(be, context.getLevel(), context.getPlayer(), side, false);
	}

	/** Sneaking: the encasing comes off, or a Splitter (its wrench opens its screen), or else the wire is picked up. */
	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LineBlockEntity be && be.encasing() != null)
			return LineInteractions.wrench(be, context.getLevel(), context.getPlayer(), context.getClickedFace(), true);
		if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LineBlockEntity be && be.fitting() == Fitting.SPLITTER) {
			if (!context.getLevel().isClientSide)
				be.removeFitting(context.getPlayer());
			return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
		}
		return IWrenchable.super.onSneakWrenched(state, context);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LineBlockEntity be ? be.comparatorSignal() : 0;
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
		return new PowerLineBlockEntity(pos, state);
	}
}
