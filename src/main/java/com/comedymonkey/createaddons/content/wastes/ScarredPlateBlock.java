package com.comedymonkey.createaddons.content.wastes;

import java.util.Locale;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The Scarred Void-Alloy Plate (D73, D75; the buildings brief's block set, #7): a Void-Alloy Plate whose {@link #FACING}
 * face carries the war's mark ({@link #SCAR}): a chip, a crack, or heat tint. Its other faces are the plate's bond
 * ({@link VoidAlloyBond}). Only the cracked one lights: the seam-coloured pixels beside its crack glow dim while its
 * district is fed. Damage marks the uprising's buildings by the rings' rules without random missing blocks.
 */
public class ScarredPlateBlock extends CityDecorLightBlock implements IWrenchable {
	public static final MapCodec<ScarredPlateBlock> CODEC = simpleCodec(ScarredPlateBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public enum Scar implements StringRepresentable {
		CHIPPED, CRACKED, HEAT;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final EnumProperty<Scar> SCAR = EnumProperty.create("scar", Scar.class);
	/**
	 * What a player's item places, and that a Create Wrench turns it to the next scar (provisional, for the user's word:
	 * progress doc, "D75").
	 */
	public static final Scar ITEM_SCAR = Scar.CHIPPED;

	public ScarredPlateBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(FACING, Direction.NORTH).setValue(SCAR, ITEM_SCAR));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, SCAR, VoidAlloyBond.NS, VoidAlloyBond.EW);
	}

	@Override
	public boolean lightable(BlockState state) {
		return state.getValue(SCAR) == Scar.CRACKED;
	}

	/** Its scar toward the player. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return VoidAlloyBond.at(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context.getClickedPos());
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		VoidAlloyBond.follow(level, pos, state);
	}

	/** A wrench turns it to the next scar (chipped, cracked, heat). */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		if (!level.isClientSide) {
			Scar next = Scar.values()[(state.getValue(SCAR).ordinal() + 1) % Scar.values().length];
			level.setBlock(context.getClickedPos(), state.setValue(SCAR, next).setValue(LIT, false), Block.UPDATE_ALL);
			IWrenchable.playRotateSound(level, context.getClickedPos());
		}
		return InteractionResult.SUCCESS;
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
