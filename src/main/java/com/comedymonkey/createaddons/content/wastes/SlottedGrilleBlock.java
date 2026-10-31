package com.comedymonkey.createaddons.content.wastes;

import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The Slotted Grille (D71, D75; the buildings brief's block set, #4): the uprising's window, a void-alloy block whose two
 * faces along its {@link #AXIS} carry three tall chamfered slots cut right through, a dim pane across each slot lit while
 * its district is fed. Its field is in the void-alloy bond ({@link VoidAlloyBond}). Not a full occluder (you see through
 * the slots). It also lies on its side for a fallen tower top (D75): the {@code _lying} and {@code up_} orientations.
 */
public class SlottedGrilleBlock extends CityDecorLightBlock {
	public static final MapCodec<SlottedGrilleBlock> CODEC = simpleCodec(SlottedGrilleBlock::new);

	/** Which way its slots look through and which way they run. */
	public enum Orientation implements StringRepresentable {
		/** Upright (the slots run up), looking along x. */
		X,
		/** Upright, looking along z. */
		Z,
		/** Lying: looking along x, the slots running along z. */
		X_LYING,
		/** Lying: looking along z, the slots running along x. */
		Z_LYING,
		/** Lying on its back: looking up, the slots running along x. */
		UP_X,
		/** Lying on its back: looking up, the slots running along z. */
		UP_Z;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

		/** A quarter turn about y: x and z change places. */
		public Orientation turned() {
			return switch (this) {
				case X -> Z;
				case Z -> X;
				case X_LYING -> Z_LYING;
				case Z_LYING -> X_LYING;
				case UP_X -> UP_Z;
				case UP_Z -> UP_X;
			};
		}
	}

	public static final EnumProperty<Orientation> AXIS = EnumProperty.create("axis", Orientation.class);

	public SlottedGrilleBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(AXIS, Orientation.Z));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(AXIS, VoidAlloyBond.NS, VoidAlloyBond.EW);
	}

	/**
	 * Upright, its slots looking the way the player looks; sneaking, lying ({@link #LYING_WHEN_SNEAKING}): looking out of
	 * the face clicked (on a floor or ceiling, on its back with the slots running the way the player looks).
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction.Axis look = context.getHorizontalDirection().getAxis();
		Orientation o = look == Direction.Axis.X ? Orientation.X : Orientation.Z;
		if (LYING_WHEN_SNEAKING && context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
			Direction.Axis face = context.getClickedFace().getAxis();
			o = switch (face) {
				case X -> Orientation.X_LYING;
				case Z -> Orientation.Z_LYING;
				case Y -> look == Direction.Axis.X ? Orientation.UP_X : Orientation.UP_Z;
			};
		}
		return VoidAlloyBond.at(defaultBlockState().setValue(AXIS, o), context.getClickedPos());
	}

	/** A player sneaking places a grille lying (provisional, for the user's word: progress doc, "D75"). */
	public static final boolean LYING_WHEN_SNEAKING = true;

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		VoidAlloyBond.follow(level, pos, state);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? state.setValue(AXIS, state.getValue(AXIS).turned()) : state;
	}
}
