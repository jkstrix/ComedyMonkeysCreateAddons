package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The heart's Derelict Star Reactor (design/specs/clockwork-wastes.md, P10, P13; brief §6): a 3 x 3 x 3 cradle of four
 * void-alloy arms round a smoky glass sphere, fixed where the city built it. Its blocks are one block of 27
 * {@link #PART}s ({@code x + 3z + 9y}, the controller {@link #CONTROLLER} in the middle of the bottom layer, where the
 * model and block entity live; the four bottom edge-middles are ports a Power Line can meet). A Nether Star put in (by
 * hand, funnel, arm, Deployer or package) wakes all four districts at once and runs the city's lamps for 8 hours; its
 * spare power can be drawn off by a line. A Wrench (a player's sneak-click, or a Deployer holding one) salvages it for
 * two Unknown Devices, leaving a dead ruin ({@link #SALVAGED}) that only then can be broken.
 */
public class DerelictStarReactorBlock extends BaseEntityBlock implements IWrenchable {
	public static final MapCodec<DerelictStarReactorBlock> CODEC = simpleCodec(DerelictStarReactorBlock::new);
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 26);
	public static final BooleanProperty SALVAGED = BooleanProperty.create("salvaged");
	public static final int CONTROLLER = 4;
	private static final float HARDNESS = 5f, RESISTANCE = 9f;
	/** Removing all its blocks at once (one going takes the rest). */
	private static boolean removing;

	public DerelictStarReactorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(PART, CONTROLLER).setValue(SALVAGED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART, SALVAGED);
	}

	public static int part(int x, int y, int z) {
		return x + 3 * z + 9 * y;
	}

	/** Where the controller of the part at {@code pos} is. */
	public static BlockPos controller(BlockState state, BlockPos pos) {
		int p = state.getValue(PART), x = p % 3, z = p / 3 % 3, y = p / 9;
		return pos.offset(1 - x, -y, 1 - z);
	}

	/** Where part {@code p} of the reactor whose controller is at {@code controller} is. */
	public static BlockPos partAt(BlockPos controller, int p) {
		return controller.offset(p % 3 - 1, p / 9, p / 3 % 3 - 1);
	}

	/** The four bottom edge-middles, where a Power Line can meet it. */
	public static boolean isPort(int p) {
		return p == 1 || p == 3 || p == 5 || p == 7;
	}

	@Nullable
	public static DerelictStarReactorBlockEntity controllerAt(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof DerelictStarReactorBlock))
			return null;
		return level.getBlockEntity(controller(state, pos)) instanceof DerelictStarReactorBlockEntity be ? be : null;
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		for (int p = 0; p < 27; p++) {
			BlockPos at = partAt(pos, p);
			if (at.getY() >= context.getLevel().getMaxBuildHeight() || !context.getLevel().getBlockState(at).canBeReplaced(context))
				return null;
		}
		return defaultBlockState();
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		for (int p = 0; p < 27; p++)
			if (p != CONTROLLER)
				level.setBlock(partAt(pos, p), state.setValue(PART, p), Block.UPDATE_ALL);
	}

	/** One part going takes the rest, without their drops (the controller's is the reactor's). */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!newState.is(this) && !removing) {
			BlockPos controller = controller(state, pos);
			if (state.getValue(PART) == CONTROLLER && level.getBlockEntity(pos) instanceof DerelictStarReactorBlockEntity be)
				be.removed();
			removing = true;
			try {
				for (int p = 0; p < 27; p++) {
					BlockPos at = partAt(controller, p);
					if (!at.equals(pos) && level.getBlockState(at).is(this))
						level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				}
			} finally {
				removing = false;
			}
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	/** Broken anywhere (once salvaged), it drops from its controller. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && state.getValue(PART) != CONTROLLER) {
			BlockPos c = controller(state, pos);
			if (level.getBlockState(c).is(this))
				level.destroyBlock(c, !player.isCreative(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return CityLocks.progress(state, player, level, pos, !state.getValue(SALVAGED), HARDNESS);
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		return CityLocks.resistance(!state.getValue(SALVAGED), RESISTANCE);
	}

	// ---------- a star in, or the wrench ----------

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hit) {
		DerelictStarReactorBlockEntity be = controllerAt(level, pos);
		if (be == null)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (StarReactorBlockEntity.isStar(stack)) {
			if (level.isClientSide)
				return ItemInteractionResult.SUCCESS;
			if (be.insertStar(player))
				stack.consume(1, player);
			return ItemInteractionResult.SUCCESS;
		}
		// a Deployer holding a Wrench salvages as a player's sneak-click does (D32)
		if (Salvage.isWrench(stack) && Salvage.isDeployer(player)) {
			if (level instanceof ServerLevel)
				be.salvage(player, stack);
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	/** A player's sneak-click with the Wrench: two Unknown Devices, and no reactor afterwards (P13). */
	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		DerelictStarReactorBlockEntity be = controllerAt(context.getLevel(), context.getClickedPos());
		if (be != null && context.getLevel() instanceof ServerLevel)
			be.salvage(context.getPlayer(), context.getItemInHand());
		return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
	}

	/** A plain wrench click does nothing: it can't turn. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.PASS;
	}

	// ---------- shapes: the ring base, the arms, the sphere ----------

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		int p = state.getValue(PART), x = p % 3, z = p / 3 % 3, y = p / 9;
		if (y == 0)
			return Block.box(0, 0, 0, 16, 6, 16);
		boolean centre = x == 1 && z == 1;
		if (centre)
			return y == 1 ? Block.box(2, 6, 2, 14, 16, 14) : Block.box(2, 0, 2, 14, 6, 14);
		// an arm stands in each edge-middle block, on the side toward the centre
		if (x == 1 || z == 1) {
			double[] b = y == 1 ? new double[] { 6, 0, 4, 10, 16, 8 } : new double[] { 6, 0, 4, 10, 5, 16 };
			if (state.getValue(SALVAGED) && y == 2)
				return Shapes.empty();
			return turned(b, x, z);
		}
		return Shapes.empty();
	}

	/** An arm's box built for the north edge-middle (x 1, z 0), turned to the edge-middle at (x, z). */
	private static VoxelShape turned(double[] b, int x, int z) {
		net.minecraft.core.Direction side = z == 0 ? net.minecraft.core.Direction.NORTH : z == 2 ? net.minecraft.core.Direction.SOUTH
			: x == 0 ? net.minecraft.core.Direction.WEST : net.minecraft.core.Direction.EAST;
		double[] p = SalvageBlock.turn(b[0], b[2], side), q = SalvageBlock.turn(b[3], b[5], side);
		return Block.box(Math.min(p[0], q[0]), b[1], Math.min(p[1], q[1]), Math.max(p[0], q[0]), b[4], Math.max(p[1], q[1]));
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof DerelictStarReactorBlockEntity be)
			be.scheduledTick();
	}

	// ---------- block entities: the controller, and the ports ----------

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		int p = state.getValue(PART);
		if (p == CONTROLLER)
			return new DerelictStarReactorBlockEntity(pos, state);
		return isPort(p) ? new DerelictReactorPortBlockEntity(pos, state) : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide || state.getValue(PART) != CONTROLLER || type != ModWastes.DERELICT_STAR_REACTOR_BE.get())
			return null;
		return (l, p, s, be) -> ((DerelictStarReactorBlockEntity) be).tick();
	}
}
