package com.comedymonkey.createaddons.content.tradebot.master;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.chest.ChestUpgrades;
import com.comedymonkey.createaddons.content.tradebot.board.TradeBoardEntity;
import com.comedymonkey.createaddons.content.tradebot.board.TradeBoards;
import com.comedymonkey.createaddons.content.tradebot.work.ContainerWork;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What a Trade Master knows of his network, read once to plan a trip (design/specs/trade-master.md, "What he knows"):
 * every container with a Trade Board tuned to it within his range of the station and loaded (D40, H7), what each holds,
 * and the gauges on its boards' faces (D41). Boards out of range only lend their gauges to the "out of range" list (P3).
 */
public final class TradeNetwork {
	/** A gauge on one of his boards: Create's place for it, the container it counts, its item, and its amount in items. */
	public record Gauge(FactoryPanelPosition panel, BlockPos container, ItemStack kind, int amount) {}

	/**
	 * A container with one of his boards on it. No gauges: an input (D40). Gauges: an output he fills, and draws from
	 * only above them: nothing is taken from it below a gauge's amount for that item, to sell or to spend (D36).
	 */
	public static final class Store {
		public final BlockPos pos;
		public final List<Gauge> gauges;
		private final KindCounts contents;

		public Store(BlockPos pos, List<Gauge> gauges, List<ItemStack> contents) {
			this.pos = pos;
			this.gauges = List.copyOf(gauges);
			this.contents = KindCounts.of(contents);
		}

		public int count(ItemStack kind) {
			return contents.get(kind);
		}

		/** The most any gauge on it asks for of {@code kind}: what has to stay (D36). */
		public int floor(ItemStack kind) {
			int floor = 0;
			for (Gauge gauge : gauges)
				if (ItemStack.isSameItemSameComponents(gauge.kind(), kind))
					floor = Math.max(floor, gauge.amount());
			return floor;
		}

		/** What he may take of {@code kind}: anything above its floor. */
		public int spare(ItemStack kind) {
			return Math.max(0, count(kind) - floor(kind));
		}

		public boolean input() {
			return gauges.isEmpty();
		}

		/** Each kind it holds, one item each. */
		public List<ItemStack> kinds() {
			return contents.kinds();
		}

		/** Planning: {@code count} of {@code kind} spoken for (taken out, as far as the rest of the plan knows). */
		void reserve(ItemStack kind, int count) {
			contents.add(kind, -count);
		}

		Store copy() {
			Store copy = new Store(pos, gauges, List.of());
			for (ItemStack kind : contents.kinds())
				copy.contents.add(kind, contents.get(kind));
			return copy;
		}
	}

	public final List<Store> stores;
	/** Gauges on boards beyond his range of the station (P3, H7). */
	public final List<Gauge> outOfRange;

	public TradeNetwork(List<Store> stores, List<Gauge> outOfRange) {
		this.stores = List.copyOf(stores);
		this.outOfRange = List.copyOf(outOfRange);
	}

	/** Every gauge in range, on every store. */
	public List<Gauge> gauges() {
		return stores.stream().flatMap(store -> store.gauges.stream()).toList();
	}

	@Nullable
	public Store store(BlockPos pos) {
		for (Store store : stores)
			if (store.pos.equals(pos))
				return store;
		return null;
	}

	/** A copy whose stores can be reserved from while planning, leaving this one as read. */
	TradeNetwork copy() {
		return new TradeNetwork(stores.stream().map(Store::copy).toList(), outOfRange);
	}

	/** His network as it is now, around his station: boards tuned to {@code frequency}, within {@code range} blocks. */
	public static TradeNetwork read(Level level, UUID frequency, BlockPos station, int range) {
		Vec3 centre = Vec3.atCenterOf(station);
		Map<BlockPos, List<Gauge>> gauges = new LinkedHashMap<>();
		List<Gauge> far = new ArrayList<>();
		for (TradeBoardEntity board : TradeBoards.on(level, frequency)) {
			BlockPos container = containerOf(level, board.containerPos());
			List<Gauge> onBoard = gaugesOn(level, board, container);
			if (Vec3.atCenterOf(container).distanceTo(centre) > range) {
				far.addAll(onBoard);
				continue;
			}
			if (!ContainerWork.allHalvesLoaded(level, container) || board.container() == null)
				continue;
			gauges.computeIfAbsent(container, c -> new ArrayList<>()).addAll(onBoard);
		}
		List<Store> stores = new ArrayList<>();
		gauges.forEach((pos, onIt) -> {
			IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler == null)
				return;
			List<ItemStack> contents = new ArrayList<>();
			for (int slot = 0; slot < handler.getSlots(); slot++)
				if (!handler.getStackInSlot(slot).isEmpty())
					contents.add(handler.getStackInSlot(slot).copy());
			stores.add(new Store(pos, onIt, contents));
		});
		return new TradeNetwork(stores, far);
	}

	/** The gauges on the face a board hangs on: the active ones with an item set (a gauge block's up to four, D41). */
	private static List<Gauge> gaugesOn(Level level, TradeBoardEntity board, BlockPos container) {
		BlockPos at = board.getPos();
		if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof FactoryPanelBlockEntity panel))
			return List.of();
		BlockState state = level.getBlockState(at);
		if (!(state.getBlock() instanceof FactoryPanelBlock) || FactoryPanelBlock.connectedDirection(state) != board.getDirection())
			return List.of();
		List<Gauge> gauges = new ArrayList<>();
		for (FactoryPanelBehaviour gauge : panel.panels.values()) {
			ItemStack kind = gauge.getFilter();
			if (!gauge.active || kind.isEmpty() || gauge.getAmount() <= 0)
				continue;
			gauges.add(new Gauge(gauge.getPanelPosition(), container, kind.copyWithCount(1), amount(gauge.getAmount(), gauge.upTo, kind)));
		}
		return gauges;
	}

	/** A gauge's amount in items: as set ("up to"), or in stacks of its item (Create's {@code tickStorageMonitor}). */
	public static int amount(int setting, boolean items, ItemStack kind) {
		return setting * (items ? 1 : kind.getMaxStackSize());
	}

	/** A double chest is one store, however many of its halves have boards: its lowest half stands for it. */
	private static BlockPos containerOf(Level level, BlockPos pos) {
		if (!level.isLoaded(pos))
			return pos;
		return ChestUpgrades.halves(level, pos, level.getBlockState(pos)).stream().min(Comparator.naturalOrder()).orElse(pos);
	}
}
