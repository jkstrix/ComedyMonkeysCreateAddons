package com.comedymonkey.createaddons.content.wastes;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A broken Create machine from the Clockwork Wastes (D26): decor that comes apart for parts. Sneak-click it with a Create
 * Wrench, or let a Deployer holding one click it, for the full salvage ({@link Salvage}); a plain wrench click turns it
 * as any wrenchable block. Broken any other way it drops one Clockwork Scrap (its {@code blocks/} loot table). Faces
 * the player who places it; its model reaches into the neighbouring space in places (the Deployer's snapped arm), but it
 * collides as its own block only.
 */
public class SalvageBlock extends HorizontalDirectionalBlock implements IWrenchable {
	public static final MapCodec<SalvageBlock> CODEC = simpleCodec(SalvageBlock::new);

	private final Map<Direction, VoxelShape> shapes;

	public SalvageBlock(Properties properties) {
		this(properties, new double[][] { { 0, 0, 0, 16, 16, 16 } });
	}

	/** @param northBoxes its shape facing north, in pixels: {x0, y0, z0, x1, y1, z1} each */
	public SalvageBlock(Properties properties, double[][] northBoxes) {
		super(properties);
		this.shapes = shapes(northBoxes);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING));
	}

	/** A player's sneak-click with the Wrench: the full salvage, not Create's pick-up. */
	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		if (context.getLevel() instanceof ServerLevel level)
			Salvage.salvage(level, context.getClickedPos(), context.getPlayer(), context.getItemInHand());
		return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
	}

	/** A Deployer holding a Wrench salvages as a player's sneak-click does (D32, P43); it can't sneak, so it's caught here. */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hit) {
		if (!Salvage.isWrench(stack) || !Salvage.isDeployer(player))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (level instanceof ServerLevel server)
			Salvage.salvage(server, pos, player, stack);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	static Map<Direction, VoxelShape> shapes(double[][] northBoxes) {
		Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape shape = Shapes.empty();
			for (double[] b : northBoxes) {
				double[] p = turn(b[0], b[2], facing), q = turn(b[3], b[5], facing);
				shape = Shapes.or(shape, Block.box(Math.min(p[0], q[0]), b[1], Math.min(p[1], q[1]), Math.max(p[0], q[0]), b[4], Math.max(p[1], q[1])));
			}
			map.put(facing, shape);
		}
		return map;
	}

	/** A north-facing (x, z) point in pixels, turned about the block's middle to {@code facing} (as the blockstates turn the models). */
	static double[] turn(double x, double z, Direction facing) {
		return switch (facing) {
			case EAST -> new double[] { 16 - z, x };
			case SOUTH -> new double[] { 16 - x, 16 - z };
			case WEST -> new double[] { z, 16 - x };
			default -> new double[] { x, z };
		};
	}
}
