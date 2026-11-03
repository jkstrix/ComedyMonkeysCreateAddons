package com.comedymonkey.createaddons.content.jetpack;

import java.util.EnumMap;
import java.util.Map;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Jetpack Winding Station (jetpack spec D38-D45): the only place a Clockwork Jetpack is wound. Hang one on it, or
 * back into it wearing one (sneak with your back to it, standing on the block in front), and its boom reaches out,
 * fits its socket over the key and winds it from the shaft below, at a Copper Backtank's rate and stress. It faces you
 * when placed: its front, where the hung jetpack's key points, is the side you placed it from.
 */
public class JetpackWindingStationBlock extends HorizontalKineticBlock implements IBE<JetpackWindingStationBlockEntity> {
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL)
			SHAPES.put(facing, Shapes.or(box(facing, 1, 0, 1, 15, 4, 15), box(facing, 5.5, 4, 10.5, 10.5, 7, 15),
				box(facing, 6.25, 7, 12.25, 9.75, 25.4, 15.75)));
	}

	/** A box given facing north (the model's front), turned to face {@code facing} as the blockstate turns the model. */
	private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
		return switch (facing) {
			case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			default -> Block.box(x0, y0, z0, x1, y1, z1);
		};
	}

	public JetpackWindingStationBlock(Properties properties) {
		super(properties);
	}

	/** Its front towards whoever places it (D44). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(HORIZONTAL_FACING));
	}

	/** Hang a Clockwork Jetpack on it (D39). */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
											  BlockHitResult hit) {
		if (!Jetpacks.isJetpack(stack))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return onBlockEntityUseItemOn(level, pos, be -> {
			if (!be.hung().isEmpty())
				return ItemInteractionResult.FAIL;
			if (!level.isClientSide) {
				be.hang(stack.copyWithCount(1));
				stack.shrink(1);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		});
	}

	/**
	 * With an empty hand (D39): one hung on it goes on your back if your Jetpack slot (the chest, without Curios) is free,
	 * or into your inventory if not; with none hung on it, the jetpack you're wearing comes off and hangs on it. Anything
	 * else in hand (a funnel, a block, a wrench) does what it does.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty())
			return InteractionResult.PASS;
		return onBlockEntityUse(level, pos, be -> {
			if (be.hung().isEmpty()) {
				if (Jetpacks.worn(player).isEmpty())
					return InteractionResult.PASS;
				if (!level.isClientSide && player instanceof ServerPlayer server) {
					JetpackFlight.flush(server);
					be.hang(Jetpacks.takeOff(player));
					JetpackFlight.sound(player, "equip");
				}
				return InteractionResult.sidedSuccess(level.isClientSide);
			}
			if (!level.isClientSide) {
				ItemStack jetpack = be.takeDown();
				if (!Jetpacks.putOn(player, jetpack))
					player.getInventory().placeItemBackInInventory(jetpack);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		});
	}

	/** Breaking it drops the hung jetpack, wind and all (D44). */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock()))
			withBlockEntityDo(level, pos, be -> {
				if (!be.hung().isEmpty())
					Block.popResource(level, pos, be.takeDown());
			});
		super.onRemove(state, level, pos, newState, moved);
	}

	/** The shaft comes in from below, like Create's backtank (D40). */
	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == Direction.DOWN;
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return Direction.Axis.Y;
	}

	/** A comparator reads the hung jetpack's wind (D26). */
	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof JetpackWindingStationBlockEntity be ? be.comparatorLevel() : 0;
	}

	@Override
	public Class<JetpackWindingStationBlockEntity> getBlockEntityClass() {
		return JetpackWindingStationBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends JetpackWindingStationBlockEntity> getBlockEntityType() {
		return ModBlockEntities.JETPACK_WINDING_STATION.get();
	}
}
