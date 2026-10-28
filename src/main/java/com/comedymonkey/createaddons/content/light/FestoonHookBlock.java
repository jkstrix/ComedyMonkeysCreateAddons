package com.comedymonkey.createaddons.content.light;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.line.LinePostBlock;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Festoon's hook (Festoons): a small iron plate and eye on a wall face, or a bracket on the side of a post (POST), put
 * up by hanging Festoon Lights from it and gone when its last string comes down. It's fixed to the block behind it, so a
 * Power Line on that block (or the post) feeds its run (LightMounts). A dye colours the run's bulbs, the wrench steps
 * their brightness, shears take it down.
 */
public class FestoonHookBlock extends BaseEntityBlock implements IWrenchable, LitBlock {
	public static final MapCodec<FestoonHookBlock> CODEC = simpleCodec(FestoonHookBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty POST = BooleanProperty.create("post");
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		SHAPES.put(Direction.SOUTH, box(6.5, 10.5, 0, 9.5, 13.5, 2));
		SHAPES.put(Direction.NORTH, box(6.5, 10.5, 14, 9.5, 13.5, 16));
		SHAPES.put(Direction.EAST, box(0, 10.5, 6.5, 2, 13.5, 9.5));
		SHAPES.put(Direction.WEST, box(14, 10.5, 6.5, 16, 13.5, 9.5));
	}

	public FestoonHookBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(POST, false)
			.setValue(LightBlock.LIGHT, 0).setValue(LightBlock.COLOR, LightDye.NONE));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POST, LightBlock.LIGHT, LightBlock.COLOR);
	}

	/** Its run's draw is its block entity's (its strings' length): the block alone draws nothing. */
	@Override
	public double draw(int step) {
		return 0;
	}

	/** A hook hangs on a sturdy face, or on any side of a Line Post or Lamp Post. */
	static boolean holds(BlockGetter level, BlockPos support, Direction facing) {
		BlockState state = level.getBlockState(support);
		return state.getBlock() instanceof LinePostBlock || state.isFaceSturdy(level, support, facing);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return holds(level, pos.relative(facing.getOpposite()), facing);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		super.onPlace(state, level, pos, old, moving);
		if (!old.is(this))
			LightMounts.relinkBehind(level, state, pos);
	}

	/** Coming down: its strings come down with it (given back as Festoon Lights), and the wires that fed it let it go. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		boolean gone = !newState.is(this);
		if (gone && !level.isClientSide && level.getBlockEntity(pos) instanceof FestoonBlockEntity be)
			Festoons.cut(level, pos, be);
		super.onRemove(state, level, pos, newState, moving);
		if (gone)
			LightMounts.relinkBehind(level, state, pos);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
											  BlockHitResult hit) {
		if (stack.is(Items.SHEARS)) {
			if (!player.mayBuild())
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide) {
				level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.6f, 1.3f);
				level.destroyBlock(pos, false);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		// (more Festoon Lights hang another string from it: the item's own use)
		if (stack.is(ModItems.FESTOON_LIGHTS.get()))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return RunLightBlockEntity.dyeRun(stack, level, pos, player);
	}

	/** The wrench steps the run's brightness (P26). */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext ctx) {
		RunLightBlockEntity.stepRun(ctx.getLevel(), ctx.getClickedPos());
		return InteractionResult.SUCCESS;
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(ModItems.FESTOON_LIGHTS.get());
	}

	/** Lights don't tick: a scheduled tick wakes its run's piece (to come on, run out, end a flicker). */
	@Override
	protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
		if (level.getBlockEntity(pos) instanceof FestoonBlockEntity be)
			be.wake();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FestoonBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return net.minecraft.world.phys.shapes.Shapes.empty();
	}
}
