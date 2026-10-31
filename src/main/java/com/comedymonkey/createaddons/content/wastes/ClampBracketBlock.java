package com.comedymonkey.createaddons.content.wastes;

import java.util.Locale;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Clamp Bracket (D71, D75; the buildings brief's block set, #6): the hero join, void-alloy clamped onto old
 * stonework. {@link #MOUNT} wall: a strap on the face of the block behind it ({@code facing} looks out from that face),
 * two jaws biting the stone and a boss as the load socket; saddle: a top plate over the block below, its jaws hanging
 * on that block's two faces along {@code facing}'s axis (the model reaches into the block below; its outline stays in its
 * own). Structural, so never lit. It needs no wall to stay up (as the Brass Plaque; provisional).
 */
public class ClampBracketBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<ClampBracketBlock> CODEC = simpleCodec(ClampBracketBlock::new);

	public enum Mount implements StringRepresentable {
		WALL, SADDLE;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final EnumProperty<Mount> MOUNT = EnumProperty.create("mount", Mount.class);
	/** Its outline facing north, in pixels (the preview's strap, jaws and boss; the saddle's plate and boss). */
	private static final Map<Direction, VoxelShape> WALL = SalvageBlock.shapes(new double[][] {
		{ 5, 3, 14, 11, 13, 16 }, { 3, 13, 12, 13, 16, 16 }, { 3, 0, 12, 13, 3, 16 }, { 6, 6, 11, 10, 10, 14 } }),
		SADDLE = SalvageBlock.shapes(new double[][] { { 5, 0, 0, 11, 4, 16 }, { 6, 4, 6, 10, 7, 10 } });

	public ClampBracketBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(MOUNT, Mount.WALL));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, MOUNT);
	}

	/** On a wall: a strap on the face clicked. On a top: a saddle, its jaws along the way the player looks. Under: a strap facing the player. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (face.getAxis().isHorizontal())
			return defaultBlockState().setValue(FACING, face);
		if (face == Direction.UP)
			return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(MOUNT, Mount.SADDLE);
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(MOUNT) == Mount.SADDLE ? SADDLE : WALL).get(state.getValue(FACING));
	}
}
