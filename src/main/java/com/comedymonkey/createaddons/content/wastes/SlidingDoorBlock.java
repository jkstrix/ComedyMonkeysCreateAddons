package com.comedymonkey.createaddons.content.wastes;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A locked Sliding Door (design/specs/clockwork-wastes.md, P21, P49): two void-alloy leaves, two blocks wide and three
 * tall, the controller at the bottom of column 0 with column 1 on the facing's clockwise side. Locked (unbreakable,
 * blast-proof, contraption-proof) until its district wakes (the heart's: until all four are); then the seam lights and
 * the leaves slide into the walls ({@link SlidingDoorBlockEntity}). Open, it never closes and lets anything through
 * at once, so it can't close on anyone; it's an ordinary block from then on, broken as one like a door.
 */
public class SlidingDoorBlock extends HorizontalDirectionalBlock implements EntityBlock, CityLocks.Lockable {
	public static final MapCodec<SlidingDoorBlock> CODEC = simpleCodec(SlidingDoorBlock::new);
	public static final IntegerProperty COL = IntegerProperty.create("col", 0, 1);
	public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 2);
	/** Woken: its leaves are in the walls (P49). Locked while false. */
	public static final BooleanProperty OPEN = BooleanProperty.create("open");
	/** One of the heart's doors: opens once all four districts are awake. */
	public static final BooleanProperty HEART = CityVaultBlock.HEART;
	public static final float HARDNESS = 6f, RESISTANCE = 9f;
	private static final Map<Direction, VoxelShape> CLOSED = SalvageBlock.shapes(new double[][] { { 0, 0, 6, 16, 16, 10 } });

	public SlidingDoorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(COL, 0).setValue(ROW, 0)
			.setValue(OPEN, false).setValue(HEART, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, COL, ROW, OPEN, HEART);
	}

	/** Where the controller of the door at {@code pos} is. */
	public static BlockPos controller(BlockState state, BlockPos pos) {
		return pos.relative(state.getValue(FACING).getClockWise(), -state.getValue(COL)).below(state.getValue(ROW));
	}

	/** Each part's place from the controller: column toward the facing's clockwise side, row up. */
	public static BlockPos part(BlockPos controller, Direction facing, int col, int row) {
		return controller.relative(facing.getClockWise(), col).above(row);
	}

	public static boolean isController(BlockState state) {
		return state.getValue(COL) == 0 && state.getValue(ROW) == 0;
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		for (int c = 0; c < 2; c++)
			for (int r = 0; r < 3; r++) {
				BlockPos p = part(pos, facing, c, r);
				if (p.getY() >= level.getMaxBuildHeight() || !level.getBlockState(p).canBeReplaced(context))
					return null;
			}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		Direction facing = state.getValue(FACING);
		for (int c = 0; c < 2; c++)
			for (int r = 0; r < 3; r++)
				if (c != 0 || r != 0)
					level.setBlock(part(pos, facing, c, r), state.setValue(COL, c).setValue(ROW, r), Block.UPDATE_ALL);
	}

	/** A part whose neighbour in the door is gone goes too (so the whole door goes as one). */
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		Direction facing = state.getValue(FACING);
		int col = state.getValue(COL), row = state.getValue(ROW);
		int dc = direction == facing.getClockWise() ? 1 : direction == facing.getCounterClockWise() ? -1 : 0;
		int dr = direction == Direction.UP ? 1 : direction == Direction.DOWN ? -1 : 0;
		int c = col + dc, r = row + dr;
		if ((dc != 0 || dr != 0) && c >= 0 && c <= 1 && r >= 0 && r <= 2
			&& (!neighbour.is(this) || neighbour.getValue(COL) != c || neighbour.getValue(ROW) != r || neighbour.getValue(FACING) != facing))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	/** Broken anywhere, it drops from its controller (in creative, nothing drops). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && !isController(state)) {
			BlockPos c = controller(state, pos);
			if (level.getBlockState(c).is(this))
				level.destroyBlock(c, !player.isCreative(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public boolean locked(BlockState state) {
		return !state.getValue(OPEN);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return CityLocks.progress(state, player, level, pos, !state.getValue(OPEN), HARDNESS);
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		return CityLocks.resistance(!state.getValue(OPEN), RESISTANCE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CLOSED.get(state.getValue(FACING));
	}

	/** Open, nothing stops anyone: the leaves are in the walls (and it can never close on anyone, P49). */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(OPEN) ? Shapes.empty() : CLOSED.get(state.getValue(FACING));
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(OPEN);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof SlidingDoorBlockEntity be)
			be.scheduledTick();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return isController(state) ? new SlidingDoorBlockEntity(pos, state) : null;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	/** Mirrored, its columns would swap sides: only turned, never mirrored. */
	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}
}
