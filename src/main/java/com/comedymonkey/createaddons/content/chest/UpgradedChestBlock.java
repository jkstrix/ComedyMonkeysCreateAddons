package com.comedymonkey.createaddons.content.chest;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.Wrenches;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A chest fitted with a chest upgrade, single or one half of a double. Never placed directly: created by using an
 * upgrade on a vanilla chest, and turned back into one when its upgrade is removed. A chest (single or double) holds
 * one upgrade; breaking it drops a chest, and the upgrade if no other half remains to keep it. The body is a block model; the lid is drawn by {@code UpgradedChestRenderer} so it can open.
 */
public class UpgradedChestBlock extends BaseEntityBlock implements IWrenchable, TieredStorageBlock {
	public static final MapCodec<UpgradedChestBlock> CODEC = simpleCodec(UpgradedChestBlock::new);
	// Same property objects as vanilla chests, so ChestBlock.getConnectedDirection works on both
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
	public static final EnumProperty<ChestTier> TIER = EnumProperty.create("tier", ChestTier.class);
	/** While true the block model leaves the lid out and the renderer animates it instead. */
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

	private static final VoxelShape SINGLE = Block.box(1, 0, 1, 15, 14, 15);
	private static final VoxelShape NORTH = Block.box(1, 0, 0, 15, 14, 15);
	private static final VoxelShape SOUTH = Block.box(1, 0, 1, 15, 14, 16);
	private static final VoxelShape WEST = Block.box(0, 0, 1, 15, 14, 15);
	private static final VoxelShape EAST = Block.box(1, 0, 1, 16, 14, 15);

	public UpgradedChestBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(TYPE, ChestType.SINGLE)
			.setValue(TIER, ChestTier.COPPER)
			.setValue(OPEN, false));
	}

	/**
	 * Upgraded chests aren't placed from an item, but Carry On re-places carried blocks through this: it keeps the
	 * carried tier and type and takes only the facing from here (a stray half is then split off by updateShape).
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/**
	 * After any placement (converted, carried, moved...) check the half's pairing and upgrade on the next tick, and
	 * put the static lid back if it arrived open.
	 */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!level.isClientSide && !state.is(oldState.getBlock())) {
			// Placed rather than moved (e.g. put down with Carry On): may join a matching chest next to it
			if (!movedByPiston)
				ChestUpgrades.markJustPlaced(level, pos);
			level.scheduleTick(pos, this, state.getValue(OPEN) ? UpgradedChestBlockEntity.LID_CLOSE_TICKS : 1);
		}
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	/** Netherite ones shrug off explosions. */
	@Override
	public float getExplosionResistance(BlockState state, net.minecraft.world.level.BlockGetter level,
										 net.minecraft.core.BlockPos pos, net.minecraft.world.level.Explosion explosion) {
		return tierOf(state).blastProof() ? 1200 : super.getExplosionResistance(state, level, pos, explosion);
	}

	@Override
	public ChestTier tierOf(BlockState state) {
		return state.getValue(TIER);
	}

	@Override
	public java.util.List<BlockPos> parts(Level level, BlockPos pos, BlockState state) {
		return ChestUpgrades.halves(level, pos, state);
	}

	@Override
	public void setTier(Level level, BlockPos pos, ChestTier tier) {
		ChestUpgrades.setTier(level, pos, tier);
	}

	@Override
	public void revertToVanilla(Level level, BlockPos pos) {
		ChestUpgrades.revertToVanilla(level, pos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TYPE, TIER, OPEN);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (state.getValue(TYPE) == ChestType.SINGLE)
			return SINGLE;
		return switch (ChestBlock.getConnectedDirection(state)) {
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			default -> EAST;
		};
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new UpgradedChestBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide
			? createTickerHelper(type, ModBlockEntities.UPGRADED_CHEST.get(), (l, p, s, be) -> be.clientTick())
			: null;
	}

	/**
	 * Scheduled by the opener counter, after closing and after pairing changes: drops lost viewers, restores the
	 * static lid, and sorts out a half that lost its partner.
	 */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof UpgradedChestBlockEntity chest)
			chest.recheckOpen();
		ChestUpgrades.validateHalf(level, pos);
	}

	/** Keeps a double chest paired only while both halves match (same facing and tier), like vanilla chests. */
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
									 BlockPos pos, BlockPos neighborPos) {
		// Rejoin, like a vanilla chest: a single half whose partner (re)appears beside it, pointing back at it. This
		// is what keeps doubles together when Create puts a contraption back down one block at a time.
		if (state.getValue(TYPE) == ChestType.SINGLE && neighbor.is(this) && direction.getAxis().isHorizontal()
			&& neighbor.getValue(TYPE) != ChestType.SINGLE
			&& neighbor.getValue(FACING) == state.getValue(FACING)
			&& neighbor.getValue(TIER) == state.getValue(TIER)
			&& ChestBlock.getConnectedDirection(neighbor) == direction.getOpposite())
			return state.setValue(TYPE, neighbor.getValue(TYPE).getOpposite());
		if (state.getValue(TYPE) != ChestType.SINGLE && ChestBlock.getConnectedDirection(state) == direction) {
			boolean stillPaired = neighbor.is(this)
				&& neighbor.getValue(TYPE) == state.getValue(TYPE).getOpposite()
				&& neighbor.getValue(FACING) == state.getValue(FACING)
				&& neighbor.getValue(TIER) == state.getValue(TIER);
			if (!stillPaired) {
				// Next tick: re-check the pairing; a half left without the chest's upgrade turns back into a vanilla chest
				level.scheduleTick(pos, this, 1);
				// Another upgraded chest there may be our partner mid-placement (Create puts contraptions down one
				// block at a time, updating each against neighbours that aren't final yet), so like a vanilla chest
				// only split straight away when the neighbour isn't one at all; the scheduled check settles the rest
				if (!neighbor.is(this))
					return state.setValue(TYPE, ChestType.SINGLE);
			}
		}
		return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState rotated = state.rotate(mirror.getRotation(state.getValue(FACING)));
		return mirror == Mirror.NONE ? rotated : rotated.setValue(TYPE, rotated.getValue(TYPE).getOpposite());
	}

	/**
	 * Right-click with a wrench opens the upgrade GUI (sneaking with one still dismantles, as in Create). Sneak +
	 * empty hand is left alone so Carry On can pick the chest up.
	 */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
											  Player player, InteractionHand hand, BlockHitResult hit) {
		if (!Wrenches.isWrench(stack))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
			ChestUpgrades.openUpgradeGui(serverPlayer, pos);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/** Right-click opens the chest. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
											   BlockHitResult hit) {
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (player instanceof ServerPlayer serverPlayer)
			ChestUpgrades.openChest(serverPlayer, level, pos, state);
		return InteractionResult.CONSUME;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		// Moved blocks (Create contraptions, Sable physics objects) keep their contents in their block entity data
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof UpgradedChestBlockEntity be) {
			Containers.dropContents(level, pos, be);
			// The chest's one upgrade stays with the other half if there is one, otherwise it drops
			if (ChestUpgrades.passOnUpgrade(level, pos, state, be))
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
					new ItemStack(state.getValue(TIER).upgradeItem()));
			level.updateNeighbourForOutputSignal(pos, this);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	/** Pick-block gives the chest's own item (with its tier); Ctrl + pick-block in creative also copies the contents. */
	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return UpgradedChestItem.of(state.getValue(TIER));
	}

	/** Placed from its item (creative pick-block): a fresh single chest that holds its own upgrade. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof UpgradedChestBlockEntity chest)
			chest.setHoldsUpgrade(true);
	}
}
