package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

class UpgradedChestBlockEntityTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void setUpRegistries() {
		registries = VanillaRegistries.createLookup();
	}

	@BeforeEach
	void loadConfig() {
		TestConfig.loadDefaults();
	}

	private static BlockState state(ChestTier tier) {
		Block block = ModBlocks.UPGRADED_CHEST.get();
		return block.defaultBlockState().setValue(UpgradedChestBlock.TIER, tier);
	}

	private static UpgradedChestBlockEntity chest(ChestTier tier) {
		return ModBlockEntities.UPGRADED_CHEST.get().create(BlockPos.ZERO, state(tier));
	}

	private static ItemStack named(Item item, String name, int count) {
		ItemStack stack = new ItemStack(item, count);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
		return stack;
	}

	private static int total(UpgradedStorage storage, Item item) {
		int total = 0;
		for (ItemStack stack : storage.getItems())
			if (stack.is(item))
				total += stack.getCount();
		return total;
	}

	private static UpgradedChestBlockEntity reload(UpgradedChestBlockEntity chest, ChestTier tier) {
		CompoundTag tag = chest.saveWithoutMetadata(registries);
		UpgradedChestBlockEntity loaded = chest(tier);
		loaded.loadWithComponents(tag, registries);
		return loaded;
	}

	@Test
	void slotsFollowTheTier() {
		for (ChestTier tier : ChestTier.values()) {
			UpgradedStorage storage = chest(tier);
			assertEquals(tier, storage.getTier());
			assertEquals(tier.slots(), storage.getContainerSize(), tier.name());
		}
	}

	@Test
	void stackLimitsFollowTheTier() {
		UpgradedStorage gold = chest(ChestTier.GOLD);
		assertEquals(512, gold.getMaxStackSize());
		assertEquals(512, gold.getMaxStackSize(new ItemStack(Items.STONE)));
		assertEquals(128, gold.getMaxStackSize(new ItemStack(Items.ENDER_PEARL)));
		assertEquals(1, gold.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)));
		assertEquals(1024, chest(ChestTier.NETHERITE).getMaxStackSize(new ItemStack(Items.STONE)));
	}

	@Test
	void saveAndLoadKeepOversizedStacks() {
		UpgradedChestBlockEntity chest = chest(ChestTier.GOLD);
		chest.getItems().set(0, new ItemStack(Items.STONE, 500));
		chest.getItems().set(26, named(Items.DIAMOND, "Shiny", 300));

		UpgradedStorage loaded = reload(chest, ChestTier.GOLD);
		assertEquals(27, loaded.getContainerSize());
		for (int i = 0; i < 27; i++)
			assertTrue(ItemStack.matches(chest.getItem(i), loaded.getItem(i)), "slot " + i);
	}

	@Test
	void saveAndLoadKeepAFullNetheriteChest() {
		UpgradedChestBlockEntity chest = chest(ChestTier.NETHERITE);
		for (int i = 0; i < 54; i++)
			chest.getItems().set(i, new ItemStack(Items.STONE, 1024));
		UpgradedStorage loaded = reload(chest, ChestTier.NETHERITE);
		assertEquals(54 * 1024, total(loaded, Items.STONE));
	}

	@Test
	void holdsUpgradeIsSavedAndDefaultsToTrue() {
		UpgradedChestBlockEntity chest = chest(ChestTier.IRON);
		assertTrue(chest.saveWithoutMetadata(registries).getBoolean("HoldsUpgrade"));

		CompoundTag without = chest.saveWithoutMetadata(registries);
		without.putBoolean("HoldsUpgrade", false);
		UpgradedChestBlockEntity notHolding = chest(ChestTier.IRON);
		notHolding.loadWithComponents(without, registries);
		assertFalse(notHolding.saveWithoutMetadata(registries).getBoolean("HoldsUpgrade"));

		// Data from before the flag existed: the chest holds its upgrade
		CompoundTag legacy = chest.saveWithoutMetadata(registries);
		legacy.remove("HoldsUpgrade");
		notHolding.loadWithComponents(legacy, registries);
		assertTrue(notHolding.saveWithoutMetadata(registries).getBoolean("HoldsUpgrade"));
	}

	@Test
	void downgradeCompactsIntoTheSmallerInventory() {
		UpgradedChestBlockEntity chest = chest(ChestTier.DIAMOND);
		NonNullList<ItemStack> items = chest.getItems();
		items.set(5, new ItemStack(Items.STONE, 500));
		items.set(30, new ItemStack(Items.DIRT, 2));
		items.set(53, new ItemStack(Items.DIAMOND, 1));

		chest.setBlockState(state(ChestTier.GOLD));
		UpgradedStorage gold = chest;
		assertEquals(27, gold.getContainerSize());
		assertTrue(gold.getItem(0).is(Items.STONE));
		assertEquals(500, gold.getItem(0).getCount());
		assertTrue(gold.getItem(1).is(Items.DIRT));
		assertTrue(gold.getItem(2).is(Items.DIAMOND));
	}

	@Test
	void upgradeToDiamondKeepsPositions() {
		UpgradedChestBlockEntity chest = chest(ChestTier.GOLD);
		chest.getItems().set(26, new ItemStack(Items.STONE, 300));
		chest.setBlockState(state(ChestTier.DIAMOND));
		assertEquals(54, chest.getContainerSize());
		assertEquals(300, chest.getItem(26).getCount());
	}

	@Test
	void fillResizesToTheTier() {
		UpgradedStorage diamond = chest(ChestTier.DIAMOND);
		NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
		contents.set(26, new ItemStack(Items.STONE, 10));
		diamond.fill(contents);
		assertEquals(54, diamond.getContainerSize());
		assertEquals(10, diamond.getItem(26).getCount());
	}

	@Test
	void loadMakesRoomForSlotsPastTheTier() {
		// Data can arrive before the block state that matches it (e.g. moved by a physics mod)
		UpgradedChestBlockEntity diamond = chest(ChestTier.DIAMOND);
		diamond.getItems().set(40, new ItemStack(Items.STONE, 64));
		UpgradedStorage gold = reload(diamond, ChestTier.GOLD);
		assertEquals(54, gold.getContainerSize());
		assertEquals(64, gold.getItem(40).getCount());
	}

	@Test
	void contentsStayOutOfItemComponents() {
		// The vanilla container component can't hold stacks above 99
		UpgradedChestBlockEntity chest = chest(ChestTier.GOLD);
		chest.getItems().set(0, new ItemStack(Items.STONE, 500));
		assertFalse(chest.collectComponents().has(DataComponents.CONTAINER));
	}

	@Test
	void automationFillsPast64() {
		UpgradedStorage gold = chest(ChestTier.GOLD);
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(gold);
		assertEquals(488, handler.insertItem(0, new ItemStack(Items.STONE, 1000), false).getCount());
		assertEquals(512, gold.getItem(0).getCount());
		assertEquals(64, handler.extractItem(0, 1000, false).getCount());
		assertEquals(448, gold.getItem(0).getCount());
	}

	/**
	 * FLY-01: with Flywheel the open lid is a visual of its own, which Flywheel doesn't make again for a change of block
	 * state, so opening or shutting must have the client make it again; other state changes (a new tier, turning) not.
	 */
	@Test
	void onlyOpeningOrShuttingChangesTheLidShown() {
		BlockState shut = state(ChestTier.GOLD), open = shut.setValue(UpgradedChestBlock.OPEN, true);
		assertTrue(UpgradedChestBlockEntity.lidShownChanged(shut, open));
		assertTrue(UpgradedChestBlockEntity.lidShownChanged(open, shut));
		assertFalse(UpgradedChestBlockEntity.lidShownChanged(shut, shut));
		assertFalse(UpgradedChestBlockEntity.lidShownChanged(open, open.setValue(UpgradedChestBlock.TIER, ChestTier.DIAMOND)));
		assertFalse(UpgradedChestBlockEntity.lidShownChanged(shut, shut.setValue(UpgradedChestBlock.FACING,
			net.minecraft.core.Direction.EAST)));
	}

	/**
	 * A level with nothing in it but which side it's on: made without its constructor (which needs a running server or
	 * client), enough for what a chest asks of its level when only its {@code open} state changes.
	 */
	private static net.minecraft.world.level.Level level(boolean clientSide) {
		try {
			Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
			java.lang.reflect.Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Object unsafe = theUnsafe.get(null);
			Object level = unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, net.minecraft.server.level.ServerLevel.class);
			long side = (long) unsafeClass.getMethod("objectFieldOffset", java.lang.reflect.Field.class)
				.invoke(unsafe, net.minecraft.world.level.Level.class.getDeclaredField("isClientSide"));
			unsafeClass.getMethod("putBoolean", Object.class, long.class, boolean.class).invoke(unsafe, level, side, clientSide);
			net.minecraft.world.level.Level made = (net.minecraft.world.level.Level) level;
			assertEquals(clientSide, made.isClientSide());
			return made;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** Opens or shuts a chest in a level on this side, counting the client hook's calls. */
	private static int lidHookCalls(boolean clientSide, BlockState from, BlockState to) {
		java.util.function.Consumer<UpgradedChestBlockEntity> was = UpgradedChestBlockEntity.clientLidShown;
		int[] calls = {0};
		UpgradedChestBlockEntity chest = ModBlockEntities.UPGRADED_CHEST.get().create(BlockPos.ZERO, from);
		chest.setLevel(level(clientSide));
		UpgradedChestBlockEntity.clientLidShown = be -> {
			assertTrue(be == chest);
			calls[0]++;
		};
		try {
			chest.setBlockState(to);
		} finally {
			UpgradedChestBlockEntity.clientLidShown = was;
		}
		return calls[0];
	}

	/** In a client level, opening and shutting each call the client hook once; on the server, never. */
	@Test
	void lidHookOnTheClientOnly() {
		BlockState shut = state(ChestTier.GOLD), open = shut.setValue(UpgradedChestBlock.OPEN, true);
		assertEquals(1, lidHookCalls(true, shut, open));
		assertEquals(1, lidHookCalls(true, open, shut));
		assertEquals(0, lidHookCalls(false, shut, open));
		assertEquals(0, lidHookCalls(false, open, shut));
		// no change, nothing
		assertEquals(0, lidHookCalls(true, open, open));
	}

	/**
	 * A new tier or turning it doesn't call the client hook, on either side (asked of the check itself: those changes
	 * also drop the chest's automation handlers through its level, which an empty level can't do), nor with no level.
	 */
	@Test
	void lidHookNotForOtherChanges() {
		BlockState shut = state(ChestTier.GOLD), open = shut.setValue(UpgradedChestBlock.OPEN, true);
		for (boolean clientSide : new boolean[] {true, false}) {
			net.minecraft.world.level.Level level = level(clientSide);
			assertEquals(clientSide, UpgradedChestBlockEntity.tellsClient(level, shut, open));
			assertEquals(clientSide, UpgradedChestBlockEntity.tellsClient(level, open, shut));
			for (BlockState from : new BlockState[] {shut, open}) {
				assertFalse(UpgradedChestBlockEntity.tellsClient(level, from, from.setValue(UpgradedChestBlock.TIER, ChestTier.DIAMOND)));
				assertFalse(UpgradedChestBlockEntity.tellsClient(level, from, from.setValue(UpgradedChestBlock.FACING,
					net.minecraft.core.Direction.EAST)));
			}
		}
		assertFalse(UpgradedChestBlockEntity.tellsClient(null, shut, open));
		// and through the chest itself: with no level it can't be on the client
		java.util.function.Consumer<UpgradedChestBlockEntity> was = UpgradedChestBlockEntity.clientLidShown;
		int[] calls = {0};
		UpgradedChestBlockEntity.clientLidShown = be -> calls[0]++;
		try {
			chest(ChestTier.GOLD).setBlockState(open);
		} finally {
			UpgradedChestBlockEntity.clientLidShown = was;
		}
		assertEquals(0, calls[0]);
	}
}
