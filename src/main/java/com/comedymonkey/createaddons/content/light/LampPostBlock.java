package com.comedymonkey.createaddons.content.light;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.line.LinePostBlock;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Lamp Post (design/specs/electric-lights.md, D14, D21, P8): a Line Post of cast iron or steel, built up into a pole the
 * same way, standing upright. Its head takes a lamp head on top and, on each side, an arm with a head at its end: right-
 * click the side with a lamp head (the arm takes the post's finish; the head is a light block in the block beyond, see
 * {@link LampHeadBlock}). A small insulator on a bracket under the top ties spans; a cable at its foot feeds the pole, and
 * the pole feeds the lamps fixed to its head. Its sides carry arms, not the Line Post's branch insulators.
 */
public class LampPostBlock extends LinePostBlock {
	private final ArmFinish finish;

	public LampPostBlock(ArmFinish finish, Properties properties) {
		super(properties);
		this.finish = finish;
	}

	public ArmFinish finish() {
		return finish;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return simpleCodec(p -> new LampPostBlock(finish, p));
	}

	/** Only upright: a street lamp's pole stands on a floor. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return context.getClickedFace() == Direction.UP ? super.getStateForPlacement(context) : null;
	}
}
