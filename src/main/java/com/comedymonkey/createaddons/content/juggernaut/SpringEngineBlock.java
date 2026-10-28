package com.comedymonkey.createaddons.content.juggernaut;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Spring Engine (D34-D39): a clock's spring barrel scaled up into a two-block Create machine lying along its
 * shafts. The winding end (behind) takes a shaft and stores the stress it's set to take; the output end (in front) turns
 * a shaft at its set speed and gives up to 16,384 SU, drained by what's actually used. {@code facing} is the way the
 * output shaft points; the winding end is the block behind it.
 */
public class SpringEngineBlock extends HorizontalKineticBlock implements IBE<SpringEngineBlockEntity> {
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	private static final VoxelShape SHAPE = Block.box(0.5, 0, 0, 15.5, 16, 16);

	public enum Part implements StringRepresentable {
		OUTPUT, WINDING;

		@Override
		public String getSerializedName() {
			return this == OUTPUT ? "output" : "winding";
		}
	}

	public SpringEngineBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(PART, Part.OUTPUT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(PART));
	}

	public static boolean isOutput(BlockState state) {
		return state.getValue(PART) == Part.OUTPUT;
	}

	/** Where the other end is: behind the output end, in front of the winding end. */
	public static BlockPos partner(BlockPos pos, BlockState state) {
		Direction facing = state.getValue(HORIZONTAL_FACING);
		return pos.relative(isOutput(state) ? facing.getOpposite() : facing);
	}

	/** The output end, which holds the wind for both. */
	public static BlockPos output(BlockPos pos, BlockState state) {
		return isOutput(state) ? pos : partner(pos, state);
	}

	/** The output end goes where you click, its shaft towards you; the winding end behind it needs room too. */
	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockPos behind = context.getClickedPos().relative(facing.getOpposite());
		Level level = context.getLevel();
		if (!level.getBlockState(behind).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(behind))
			return null;
		return defaultBlockState().setValue(HORIZONTAL_FACING, facing).setValue(PART, Part.OUTPUT);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide && isOutput(state))
			level.setBlock(partner(pos, state), state.setValue(PART, Part.WINDING), 3);
	}

	/** Either end alone falls apart: the other one goes with it. */
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
								  BlockPos neighbourPos) {
		if (neighbourPos.equals(partner(pos, state)) && !(neighbour.is(this) && neighbour.getValue(PART) != state.getValue(PART)
			&& neighbour.getValue(HORIZONTAL_FACING) == state.getValue(HORIZONTAL_FACING)))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	/** Breaking the winding end breaks the output end properly, so the engine drops once, with its wind (D38). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && !isOutput(state) && !player.isCreative()) {
			BlockPos out = partner(pos, state);
			if (level.getBlockState(out).is(this))
				level.destroyBlock(out, true, player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** A wrench picks the whole engine up, wind and all, from either end; it never turns half of it. */
	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		if (isOutput(state))
			return super.onSneakWrenched(state, context);
		BlockPos out = partner(context.getClickedPos(), state);
		Level level = context.getLevel();
		BlockState outState = level.getBlockState(out);
		if (!(level instanceof ServerLevel server) || !outState.is(this))
			return InteractionResult.SUCCESS;
		Player player = context.getPlayer();
		if (player != null && !player.isCreative())
			Block.getDrops(outState, server, out, level.getBlockEntity(out), player, context.getItemInHand())
				.forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
		level.destroyBlock(out, false);
		IWrenchable.playRemoveSound(level, out);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.PASS;
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		Direction facing = state.getValue(HORIZONTAL_FACING);
		return face == (isOutput(state) ? facing : facing.getOpposite());
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING).getAxis();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** D37: a redstone signal on the output end stops it giving. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
		super.neighborChanged(state, level, pos, block, fromPos, moving);
		if (!level.isClientSide && isOutput(state))
			withBlockEntityDo(level, pos, SpringEngineBlockEntity::checkPowered);
	}

	/** D37: a comparator on either end reads how wound it is. */
	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(output(pos, state)) instanceof SpringEngineBlockEntity be ? be.comparatorLevel() : 0;
	}

	@Override
	public Class<SpringEngineBlockEntity> getBlockEntityClass() {
		return SpringEngineBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SpringEngineBlockEntity> getBlockEntityType() {
		return ModBlockEntities.SPRING_ENGINE.get();
	}
}
