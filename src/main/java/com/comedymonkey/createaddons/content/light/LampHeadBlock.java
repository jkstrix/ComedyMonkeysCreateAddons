package com.comedymonkey.createaddons.content.light;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.line.LinePostBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A street lamp head (Lantern, Cobra, Globe, Yard Light): it stands on top of a block (a Lamp Post's head, say) or, placed
 * against the side of a Lamp Post's head, hangs on an arm there (ARM: the post's finish), in the block beyond the post,
 * facing away from it. The post draws the arm (its branch on that side); the head draws its own hook or cup at the arm's
 * end. It's a light like any other, fed by the pole.
 */
public class LampHeadBlock extends LightBlock {
	public static final EnumProperty<ArmFinish> ARM = EnumProperty.create("arm", ArmFinish.class);
	/** An arm head's outline, as built facing north (the post to the south): the head at the arm's end. */
	private static final VoxelShape ON_ARM = Block.box(3, 0, 3, 13, 15, 16);

	public LampHeadBlock(LightKind kind, Properties properties) {
		super(kind, properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ARM);
	}

	/** The Lamp Post head an arm head hangs from, if it's on one. */
	public static @Nullable BlockPos post(BlockState state, BlockPos pos) {
		return state.hasProperty(ARM) && state.getValue(ARM) != ArmFinish.NONE ? pos.relative(state.getValue(FACING).getOpposite()) : null;
	}

	/** A Lamp Post's head (or a single post), whose side towards {@code side} has no arm yet. */
	private static boolean freeSide(BlockState post, Direction side) {
		return post.getBlock() instanceof LampPostBlock && post.getValue(LinePostBlock.ROLE).isHead()
			&& !post.getValue(LinePostBlock.BRANCHES.get(side));
	}

	/** Against the side of a Lamp Post's head: on an arm there, facing away from the post. Else as any head. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction face = ctx.getClickedFace();
		Level level = ctx.getLevel();
		BlockPos at = ctx.getClickedPos(), post = at.relative(face.getOpposite());
		BlockState postState = level.getBlockState(post);
		if (face.getAxis().isHorizontal() && freeSide(postState, face))
			return defaultBlockState().setValue(ARM, ((LampPostBlock) postState.getBlock()).finish()).setValue(FACING, face)
				.setValue(WATERLOGGED, level.getFluidState(at).getType() == Fluids.WATER);
		BlockState state = super.getStateForPlacement(ctx);
		return state == null ? null : state.setValue(ARM, ArmFinish.NONE);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos post = post(state, pos);
		if (post == null)
			return super.canSurvive(state, level, pos);
		BlockState p = level.getBlockState(post);
		return p.getBlock() instanceof LampPostBlock lamp && lamp.finish() == state.getValue(ARM) && p.getValue(LinePostBlock.ROLE).isHead();
	}

	/** Hung on an arm: the post grows the arm on that side; taken away, the arm goes with it. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, level, pos, old, moving);
		BlockPos post = post(state, pos);
		if (post != null && !level.isClientSide && freeSide(level.getBlockState(post), state.getValue(FACING)))
			level.setBlock(post, level.getBlockState(post).setValue(LinePostBlock.BRANCHES.get(state.getValue(FACING)), true), Block.UPDATE_ALL);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		BlockPos post = post(state, pos);
		super.onRemove(state, level, pos, newState, moving);
		if (post != null && !newState.is(this) && !level.isClientSide) {
			BlockState p = level.getBlockState(post);
			if (p.getBlock() instanceof LampPostBlock && p.getValue(LinePostBlock.BRANCHES.get(state.getValue(FACING))))
				level.setBlock(post, p.setValue(LinePostBlock.BRANCHES.get(state.getValue(FACING)), false), Block.UPDATE_ALL);
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		if (post(state, pos) == null)
			return super.getShape(state, level, pos, ctx);
		return turn(ON_ARM, state.getValue(FACING).getOpposite());
	}
}
