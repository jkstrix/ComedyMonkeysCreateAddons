package com.comedymonkey.createaddons.content.tradebot.work;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.chest.ChestUpgrades;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlock;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * Where Trade Bot stands to use a container, and how (D5, D6): on a side he can reach, at the container's level or
 * one block below it (a container up on something is a "high" one), at the distance its clip was animated for,
 * facing it. Chests prefer their front, where the lid opens; a chest with something on its lid is used blind.
 */
public record ContainerVisit(BlockPos container, ContainerKind kind, Direction side, BlockPos standBlock, Vec3 stand, float yaw) {

	/** The best way to use {@code container}, nearest him first; null if he can't stand anywhere to reach it. */
	@Nullable
	public static ContainerVisit plan(TradeBotEntity bot, BlockPos container) {
		Level level = bot.level();
		BlockState state = level.getBlockState(container);
		Direction front = isChest(state) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;
		List<ContainerVisit> options = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL)
			for (int below = 0; below <= 1; below++) {
				ContainerVisit visit = at(bot, level, container, state, side, below == 1);
				if (visit != null)
					options.add(visit);
			}
		return options.stream().min(Comparator.comparingDouble(v ->
			v.stand.distanceTo(bot.position()) + (front != null && v.side != front ? 8 : 0) + (v.kind == ContainerKind.CHEST_HIGH ? 4 : 0)))
			.orElse(null);
	}

	private static boolean isChest(BlockState state) {
		return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof UpgradedChestBlock;
	}

	@Nullable
	private static ContainerVisit at(TradeBotEntity bot, Level level, BlockPos container, BlockState state, Direction side, boolean high) {
		ContainerKind kind = ContainerKind.of(state, high);
		if (high && (kind == ContainerKind.DEPOT || kind == ContainerKind.BELT))
			kind = ContainerKind.BLIND; // a raised depot or belt: he can't reach its top, so it's the blind routine
		// something on the lid (on either half of a double chest): it can't open for him to look in
		if (kind == ContainerKind.CHEST && ChestUpgrades.halves(level, container, state).stream().anyMatch(half -> ChestBlock.isChestBlockedAt(level, half)))
			kind = ContainerKind.CHEST_HIGH;
		int dy = high ? -1 : 0;
		Vec3 face = Vec3.atBottomCenterOf(container).add(side.getStepX() * 0.5, 0, side.getStepZ() * 0.5);
		Vec3 point = face.add(side.getStepX() * kind.standoff / 16.0, 0, side.getStepZ() * kind.standoff / 16.0);
		BlockPos standBlock = new BlockPos(BlockPos.containing(point).getX(), container.getY() + dy, BlockPos.containing(point).getZ());
		if (WalkNodeEvaluator.getPathTypeStatic(bot, standBlock) != PathType.WALKABLE)
			return null;
		// the blocks between him and the container must be clear (he leans and reaches across them)
		for (BlockPos between = container.relative(side).above(dy); !between.equals(standBlock); between = between.relative(side)) {
			PathType type = WalkNodeEvaluator.getPathTypeStatic(bot, between);
			if (type != PathType.WALKABLE && type != PathType.OPEN)
				return null;
			if (Math.abs(between.getX() - container.getX()) + Math.abs(between.getZ() - container.getZ()) > 3)
				return null;
		}
		Vec3 stand = new Vec3(point.x, WalkNodeEvaluator.getFloorLevel(level, standBlock), point.z);
		return new ContainerVisit(container, kind, side, standBlock, stand, side.getOpposite().toYRot());
	}
}
