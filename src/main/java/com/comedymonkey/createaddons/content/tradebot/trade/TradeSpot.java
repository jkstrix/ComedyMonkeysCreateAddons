package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * Where Trade Bot stands to trade with a seated villager (D7): two blocks out from the seat, so there's one block
 * between them, facing the villager. A workstation or any other obstacle in that block is fine (he lobs the goods over
 * it, {@code trade_villager}); if it's open he uses {@code trade_villager_open}. He stands where the clip was animated,
 * 39 px from the seat's centre (the preview villager's middle), so the package lands in the villager's arms.
 */
public record TradeSpot(AbstractVillager villager, BlockPos seat, Direction side, BlockPos standBlock, Vec3 stand, float yaw, boolean open) {
	public static final double SEAT_DISTANCE = 39 / 16.0;

	/** The best place to trade with {@code villager} from, nearest him first, preferring one across a workstation. */
	@Nullable
	public static TradeSpot plan(TradeBotEntity bot, AbstractVillager villager) {
		return plan(bot, villager, java.util.Set.of());
	}

	/** As {@link #plan(TradeBotEntity, AbstractVillager)}, but not from the sides he's already failed to reach. */
	@Nullable
	public static TradeSpot plan(TradeBotEntity bot, AbstractVillager villager, java.util.Set<Direction> except) {
		Level level = bot.level();
		BlockPos seat = seatOf(villager);
		List<TradeSpot> options = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (except.contains(side))
				continue;
			BlockPos standBlock = seat.relative(side, 2);
			BlockPos gap = seat.relative(side);
			// a spot in an unloaded chunk waits for another trip (D55)
			if (!level.isLoaded(standBlock) || !level.isLoaded(gap) || WalkNodeEvaluator.getPathTypeStatic(bot, standBlock) != PathType.WALKABLE)
				continue;
			PathType between = WalkNodeEvaluator.getPathTypeStatic(bot, gap);
			boolean open = between == PathType.WALKABLE || between == PathType.OPEN;
			Vec3 point = Vec3.atBottomCenterOf(seat).add(side.getStepX() * SEAT_DISTANCE, 0, side.getStepZ() * SEAT_DISTANCE);
			double floor = WalkNodeEvaluator.getFloorLevel(level, standBlock);
			Vec3 stand = new Vec3(point.x, floor, point.z);
			// he has to fit there: with a wall just behind the spot, he stands back on the block's centre instead
			if (!fits(bot, stand))
				stand = new Vec3(standBlock.getX() + 0.5, floor, standBlock.getZ() + 0.5);
			if (!fits(bot, stand))
				continue;
			options.add(new TradeSpot(villager, seat, side, standBlock, stand, side.getOpposite().toYRot(), open));
		}
		return options.stream().min(Comparator.comparingDouble(s -> s.stand.distanceTo(bot.position()) + (s.open ? 2 : 0))).orElse(null);
	}

	private static boolean fits(TradeBotEntity bot, Vec3 at) {
		return bot.level().noCollision(bot, bot.getDimensions(bot.getPose()).makeBoundingBox(at).deflate(1e-3));
	}

	/** The seat block the villager sits in (a contraption's seat is wherever the villager is now). */
	public static BlockPos seatOf(AbstractVillager villager) {
		Entity vehicle = villager.getVehicle();
		return vehicle instanceof SeatEntity ? vehicle.blockPosition() : villager.blockPosition();
	}

	/** The villager is still sitting where he planned to trade (a contraption may have carried it off). */
	public boolean villagerInPlace() {
		Vec3 at = villager.position(), centre = Vec3.atBottomCenterOf(seat);
		double dx = at.x - centre.x, dz = at.z - centre.z;
		return dx * dx + dz * dz < 0.5 * 0.5 && Math.abs(at.y - centre.y) < 1;
	}
}
