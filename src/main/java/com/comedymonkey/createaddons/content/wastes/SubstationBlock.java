package com.comedymonkey.createaddons.content.wastes;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import org.joml.Vector3f;

import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A district's Substation (design/specs/clockwork-wastes.md, D96, P8, P50): a makers' steel cabinet two blocks tall
 * with the uprising's charge column clamped into it and a breaker lever on its side. Holding use on either half hauls the
 * breaker over and wakes its district ({@link SubstationBlockEntity}); it takes no power. Placed and broken like a door.
 * Locked (unbreakable, blast-proof, contraption-proof: {@link CityLocks}) until it wakes, crackling with faint starlight;
 * afterwards an ordinary block that breaks into scrap.
 */
public final class SubstationBlock extends HorizontalDirectionalBlock implements EntityBlock, CityLocks.Lockable {
	public static final MapCodec<SubstationBlock> CODEC = simpleCodec(SubstationBlock::new);
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	/** Dormant: nothing but a woken district's Substation can be broken (P21). */
	public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
	/** Its hardness and blast resistance once woken (bedrock's while locked: see CityLocks). */
	public static final float HARDNESS = 5f, RESISTANCE = 9f;

	private static final Map<Direction, VoxelShape> LOWER = SalvageBlock.shapes(new double[][] {
		{ 0.5, 0, 1.5, 15.5, 1, 14.5 }, { 1, 1, 1, 15, 16, 14 }, { 15, 10, 6.5, 17, 16, 9.5 } });
	private static final Map<Direction, VoxelShape> UPPER = SalvageBlock.shapes(new double[][] {
		{ 1, 0, 1, 15, 15, 14 }, { 4, 15, 7, 12, 18, 9 } });

	public SubstationBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER).setValue(LOCKED, true));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, LOCKED);
	}

	public static BlockPos lower(BlockState state, BlockPos pos) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		if (pos.getY() >= context.getLevel().getMaxBuildHeight() - 1 || !context.getLevel().getBlockState(pos.above()).canBeReplaced(context))
			return null;
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	/** A half whose other half is gone goes too. */
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
		if (direction == (lower ? Direction.UP : Direction.DOWN)
			&& (!neighbour.is(this) || neighbour.getValue(HALF) == state.getValue(HALF)))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	/** Broken by its top half, it drops from its bottom one (as a door); in creative, nothing drops. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos below = pos.below();
			if (level.getBlockState(below).is(this))
				level.destroyBlock(below, !player.isCreative(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Broken after waking (P50): its district stays awake, but its own grid is cut for good. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!newState.is(this) && state.getValue(HALF) == DoubleBlockHalf.LOWER && level.getBlockEntity(pos) instanceof SubstationBlockEntity be)
			be.removed();
		// its tray held the first sky-cable span of its district (D61): that span comes down as scrap
		if (!newState.is(this) && state.getValue(HALF) == DoubleBlockHalf.LOWER && level instanceof net.minecraft.server.level.ServerLevel server)
			DerelictInsulatorBlockEntity.anchorRemoved(server, pos);
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	public boolean locked(BlockState state) {
		return state.getValue(LOCKED);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return CityLocks.progress(state, player, level, pos, state.getValue(LOCKED), HARDNESS);
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		return CityLocks.resistance(state.getValue(LOCKED), RESISTANCE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER).get(state.getValue(FACING));
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof SubstationBlockEntity be)
			be.scheduledTick();
	}

	// ---------- the breaker (D96) ----------

	/** A use, held or new, on either half of a dormant one hauls its breaker (no arm swing: it's a steady pull). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(LOCKED) || player.isSpectator())
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.CONSUME;
		return level.getBlockEntity(lower(state, pos)) instanceof SubstationBlockEntity be && be.haul(player) ? InteractionResult.CONSUME
			: InteractionResult.PASS;
	}

	/** Faint starlight, now and then: the uprising's edge tone (visual-language.md). */
	private static final DustParticleOptions STARLIGHT = new DustParticleOptions(new Vector3f(0x7f / 255f, 0x76 / 255f, 0xa8 / 255f), 0.5f);

	/** D96: dormant, it crackles with sparse, faint violet starlight round its cabinet (sound: its low hum, the sound pass). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LOCKED) || random.nextInt(2) != 0)
			return;
		double x = pos.getX() + 0.1 + random.nextDouble() * 0.8, y = pos.getY() + random.nextDouble(), z = pos.getZ() + 0.1 + random.nextDouble() * 0.8;
		level.addParticle(STARLIGHT, x, y, z, 0, 0.02, 0);
	}

	// ---------- comparator: dormant or awake ----------

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(lower(state, pos)) instanceof SubstationBlockEntity be ? be.comparator() : 0;
	}

	// ---------- the block entity ----------

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SubstationBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide || state.getValue(HALF) != DoubleBlockHalf.LOWER || type != ModWastes.SUBSTATION_BE.get())
			return null;
		return (l, p, s, be) -> ((SubstationBlockEntity) be).tick();
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@SuppressWarnings("deprecation") // BlockState.rotate inside mirror() is vanilla's own pattern
	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
