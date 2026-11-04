package com.comedymonkey.createaddons.content.tradebot.board;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The Trade Boards in each level, by the network they're tuned to (rebuilt as they load and unload, so nothing is
 * saved): what a Trade Master reads to find his chests (design/specs/trade-master.md D40). Also the one place a gauge
 * asks "am I on a board?" (D41).
 */
public final class TradeBoards {
	private static final Map<ResourceKey<Level>, Map<UUID, Set<TradeBoardEntity>>> BY_LEVEL = new ConcurrentHashMap<>();
	/** The client's own player, set by the client at startup (this class also runs on servers, which have none). */
	public static volatile Supplier<Player> clientPlayer = () -> null;

	private TradeBoards() {}

	static void add(TradeBoardEntity board) {
		UUID frequency = board.frequency();
		if (frequency == null)
			return;
		BY_LEVEL.computeIfAbsent(board.level().dimension(), d -> new ConcurrentHashMap<>())
			.computeIfAbsent(frequency, f -> ConcurrentHashMap.newKeySet()).add(board);
	}

	static void remove(TradeBoardEntity board) {
		Map<UUID, Set<TradeBoardEntity>> level = BY_LEVEL.get(board.level().dimension());
		if (level == null)
			return;
		for (Set<TradeBoardEntity> boards : level.values())
			boards.remove(board);
		level.values().removeIf(Set::isEmpty);
	}

	/** Every loaded board on this network in this level. */
	public static Collection<TradeBoardEntity> on(Level level, UUID frequency) {
		Map<UUID, Set<TradeBoardEntity>> byFrequency = BY_LEVEL.get(level.dimension());
		Set<TradeBoardEntity> boards = byFrequency == null ? null : byFrequency.get(frequency);
		return boards == null ? List.of() : List.copyOf(boards);
	}

	/**
	 * The board hanging in {@code pos} on the face that looks out along {@code facing} (a gauge block's space and its
	 * face: a gauge there sits on the board), or null.
	 */
	@Nullable
	public static TradeBoardEntity at(Level level, BlockPos pos, Direction facing) {
		for (TradeBoardEntity board : level.getEntitiesOfClass(TradeBoardEntity.class, new AABB(pos)))
			if (board.isAlive() && board.getPos().equals(pos) && board.getDirection() == facing)
				return board;
		return null;
	}

	/** The client's player is about to put a gauge or a board on a face (clicks then go through boards). */
	public static boolean placingOnBoards() {
		Player player = clientPlayer.get();
		return player != null && (placesOnBoards(player.getMainHandItem()) || placesOnBoards(player.getOffhandItem()));
	}

	static boolean placesOnBoards(ItemStack stack) {
		return stack.getItem() instanceof com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem || stack.is(ModItems.TRADE_BOARD.get());
	}
}
