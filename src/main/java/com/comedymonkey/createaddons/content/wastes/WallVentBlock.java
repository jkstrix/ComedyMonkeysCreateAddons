package com.comedymonkey.createaddons.content.wastes;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A wall vent (D57, D59, D60; design/models/clockwork-wastes-vent.gen.js, the approved preview): a block in the heart
 * hall's walls, a tall chamfered opening at the floor closed by a louvred hatch, a curled Scuttler behind it. Its
 * {@link #FACING} looks into the hall. Sealed while the heart sleeps (its seam dark); live once the heart wakes (all four
 * districts: the seam lit in the edge tone, the charging contact dim, a dull lens glinting between the louvres); when the
 * Custodian calls, it bursts once, its Scuttler tumbling out, and stays open, spent, its hatch flat on the floor, until
 * the Custodian resets: then it shuts again with a new Scuttler inside (D67; {@link WallVentBlockEntity}). Eight to a hall, two beside each pylon (D60): the city places them.
 * <p>
 * Provisional, waiting for the user: unspent, it's locked (bedrock's hardness, so it can't be broken before its burst);
 * spent, it breaks like a Void-Alloy Plate, into one Clockwork Scrap.
 */
public class WallVentBlock extends HorizontalDirectionalBlock implements EntityBlock, CityLocks.Lockable {
	public static final MapCodec<WallVentBlock> CODEC = simpleCodec(WallVentBlock::new);
	public static final float HARDNESS = 5f, RESISTANCE = 9f;

	/** Its state: sealed (the heart asleep), live (awake, its Scuttler waiting), open (burst, spent for good). */
	public enum State implements StringRepresentable {
		SEALED, LIVE, OPEN;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final EnumProperty<State> STATE = EnumProperty.create("state", State.class);

	public WallVentBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(STATE, State.SEALED));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, STATE);
	}

	/** Facing out of the wall it was placed against, into the room (or toward the placer, on a floor). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
	}

	public static State state(BlockState state) {
		return state.getValue(STATE);
	}

	@Override
	public boolean locked(BlockState state) {
		return state.getValue(STATE) != State.OPEN;
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return CityLocks.progress(state, player, level, pos, locked(state), HARDNESS);
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		return CityLocks.resistance(locked(state), RESISTANCE);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof WallVentBlockEntity be)
			be.scheduledTick();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WallVentBlockEntity(pos, state);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
