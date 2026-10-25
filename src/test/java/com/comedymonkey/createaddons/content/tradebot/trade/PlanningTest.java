package com.comedymonkey.createaddons.content.tradebot.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The planning pass's bookkeeping: what his inputs hold, read once, and villagers he couldn't get to. */
class PlanningTest {
	/** The count the planning used to make, slot by slot, to check the snapshot against. */
	private static int slotBySlot(List<? extends IItemHandler> stores, ItemCost goods) {
		int count = 0;
		for (IItemHandler store : stores)
			for (int slot = 0; slot < store.getSlots(); slot++)
				if (goods.test(store.getStackInSlot(slot)))
					count += store.getStackInSlot(slot).getCount();
		return count;
	}

	private static ItemStackHandler store(ItemStack... stacks) {
		ItemStackHandler handler = new ItemStackHandler(54);
		for (int i = 0; i < stacks.length; i++)
			handler.setStackInSlot(i * 3 % 54, stacks[i]); // spread out, with gaps
		return handler;
	}

	@Test
	void theSnapshotCountsWhatTheSlotsHold() {
		ItemStack namedWheat = new ItemStack(Items.WHEAT, 7);
		namedWheat.set(DataComponents.CUSTOM_NAME, Component.literal("Best wheat"));
		List<ItemStackHandler> stores = List.of(
			store(new ItemStack(Items.WHEAT, 64), new ItemStack(Items.STICK, 30), namedWheat),
			store(new ItemStack(Items.WHEAT, 12), new ItemStack(Items.PAPER, 24), new ItemStack(Items.STICK, 64), new ItemStack(Items.STICK, 5)),
			store(),
			store(new ItemStack(Items.EMERALD, 3), namedWheat.copyWithCount(2)));
		InputSnapshot snapshot = InputSnapshot.of(stores);
		ItemCost onlyNamed = new ItemCost(Items.WHEAT.builtInRegistryHolder(), 1,
			DataComponentPredicate.builder().expect(DataComponents.CUSTOM_NAME, Component.literal("Best wheat")).build());
		for (ItemCost goods : List.of(new ItemCost(Items.WHEAT, 20), new ItemCost(Items.STICK, 32), new ItemCost(Items.PAPER, 24),
			new ItemCost(Items.COAL, 15), new ItemCost(Items.EMERALD, 1), onlyNamed)) {
			assertEquals(slotBySlot(stores, goods), snapshot.count(goods), goods.toString());
			assertEquals(slotBySlot(stores, goods) > 0, snapshot.has(goods), goods.toString());
		}
		assertEquals(64 + 7 + 12 + 2, snapshot.count(new ItemCost(Items.WHEAT, 20)), "named wheat is still wheat");
		assertEquals(9, snapshot.count(onlyNamed), "but only the named wheat is named");
	}

	@Test
	void tradeDemandFromTheSnapshotIsWhatSlotBySlotCountingGave() {
		List<ItemStackHandler> stores = List.of(store(new ItemStack(Items.STICK, 64), new ItemStack(Items.WHEAT, 30)),
			store(new ItemStack(Items.STICK, 6), new ItemStack(Items.WHEAT, 33), new ItemStack(Items.COAL, 14)));
		InputSnapshot snapshot = InputSnapshot.of(stores);
		TradeDemand fromSnapshot = new TradeDemand(), fromSlots = new TradeDemand();
		for (TradeDemand demand : List.of(fromSnapshot, fromSlots)) {
			demand.add(new ItemCost(Items.STICK, 32), 32, 3); // 70 sticks: two lots
			demand.add(new ItemCost(Items.WHEAT, 20), 20, 2);
			demand.add(new ItemCost(Items.WHEAT, 20), 20, 2); // a second farmer shares the 63 wheat: three lots in all
			demand.add(new ItemCost(Items.COAL, 15), 15, 4); // 14 coal: none
		}
		fromSnapshot.limit(snapshot::count);
		fromSlots.limit(goods -> slotBySlot(stores, goods));
		for (ItemStack stack : List.of(new ItemStack(Items.STICK, 64), new ItemStack(Items.WHEAT, 64), new ItemStack(Items.COAL, 64)))
			assertEquals(fromSlots.howMany(stack), fromSnapshot.howMany(stack), stack.toString());
		assertEquals(64, fromSnapshot.howMany(new ItemStack(Items.STICK, 64)));
		assertEquals(60, fromSnapshot.howMany(new ItemStack(Items.WHEAT, 64)));
		assertEquals(0, fromSnapshot.howMany(new ItemStack(Items.COAL, 64)));
	}

	@Test
	void noWayToAVillagerInTheSameSeatDoublesTheWaitEachTime() {
		Unreachable unreachable = new Unreachable();
		UUID villager = UUID.randomUUID();
		BlockPos seat = new BlockPos(5, 64, 5);
		long now = 0;
		for (long wait : new long[] { 2400, 4800, 9600, 19200, 19200, 19200 }) {
			unreachable.mark(villager, seat, now, true);
			assertTrue(unreachable.blocked(villager, now + wait - 1), "left alone " + wait + " ticks");
			assertFalse(unreachable.blocked(villager, now + wait), "but no longer than " + wait);
			now += wait;
		}
		unreachable.mark(villager, seat, now, false);
		assertFalse(unreachable.blocked(villager, now + Unreachable.BASE_TICKS), "stuck on the way: just the first wait");
		now += Unreachable.BASE_TICKS;
		unreachable.mark(villager, seat.east(), now, true);
		assertFalse(unreachable.blocked(villager, now + Unreachable.BASE_TICKS), "a new seat starts afresh");
		now += Unreachable.BASE_TICKS;
		unreachable.mark(villager, seat.east(), now, true);
		assertTrue(unreachable.blocked(villager, now + 2L * Unreachable.BASE_TICKS - 1));
		unreachable.clear(villager); // he got to it
		assertFalse(unreachable.blocked(villager, now));
		unreachable.mark(villager, seat.east(), now, true);
		assertFalse(unreachable.blocked(villager, now + Unreachable.BASE_TICKS), "and after getting to it, starts afresh too");
	}

	@Test
	void beingStuckNeverStacksUp() {
		Unreachable unreachable = new Unreachable();
		UUID villager = UUID.randomUUID();
		for (int i = 0; i < 5; i++)
			unreachable.mark(villager, BlockPos.ZERO, i * 10_000L, false);
		assertFalse(unreachable.blocked(villager, 40_000L + Unreachable.BASE_TICKS));
		unreachable.mark(villager, BlockPos.ZERO, 50_000L, true);
		assertFalse(unreachable.blocked(villager, 50_000L + Unreachable.BASE_TICKS), "the first time there's no way at all: the first wait");
	}

	@Test
	void longPastMarksAreForgotten() {
		Unreachable unreachable = new Unreachable();
		for (int i = 0; i < 50; i++)
			unreachable.mark(UUID.randomUUID(), BlockPos.ZERO, 0, true);
		unreachable.mark(UUID.randomUUID(), BlockPos.ZERO, Unreachable.BASE_TICKS + Unreachable.FORGET_TICKS, true);
		assertEquals(1, unreachable.size());
	}
}
