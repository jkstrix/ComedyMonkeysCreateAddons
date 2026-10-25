package com.comedymonkey.createaddons.content.pen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.chest.ChestStacks;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The pen's herd bookkeeping, trough, handlers, readouts and saving, on block entities that aren't in any world
 * (so nothing here ticks, breeds or butchers: those need a level).
 */
class AnimalPenBlockEntityTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void setUp() {
		TestConfig.loadDefaults();
		// In case an earlier test class left a value changed
		TestConfig.reset();
		registries = VanillaRegistries.createLookup();
	}

	@AfterEach
	void reset() {
		TestConfig.reset();
	}

	private static AnimalPenBlockEntity pen(boolean brass) {
		AnimalPenBlock block = brass ? ModBlocks.BRASS_ANIMAL_PEN.get() : ModBlocks.ANIMAL_PEN.get();
		return new AnimalPenBlockEntity(BlockPos.ZERO, block.defaultBlockState());
	}

	static ItemStack bottle(EntityType<?> type) {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", EntityType.getKey(type).toString());
		ItemStack stack = new ItemStack(ModItems.BOTTLED_ANIMAL.get());
		stack.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
		return stack;
	}

	private static Component lang(String key) {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate(key).component();
	}

	// ---------------------------------------------------------------- sizes

	@Test
	void capacityIsFixedPerTier() {
		assertEquals(128, pen(false).getCapacity());
		assertEquals(512, pen(true).getCapacity());
	}

	@Test
	void brassIsTheBrassBlock() {
		assertFalse(pen(false).isBrass());
		assertTrue(pen(true).isBrass());
	}

	@Test
	void outputAndMilkTankAreSizedByTier() {
		assertEquals(9, pen(false).getOutput().getSlots());
		assertEquals(18, pen(true).getOutput().getSlots());
		assertEquals(8_000, pen(false).getMilk().getCapacity());
		assertEquals(32_000, pen(true).getMilk().getCapacity());
	}

	@Test
	void troughHoldsFoodForEveryAnimalInNormalStacks() {
		for (boolean brass : new boolean[] {false, true}) {
			AnimalPenBlockEntity pen = pen(brass);
			assertEquals(brass ? 8 : 2, pen.getFood().getSlots());
			assertEquals(64, pen.getFood().getSlotLimit(0));
			assertEquals(pen.getCapacity(), pen.getFood().getSlots() * pen.getFood().getSlotLimit(0));
		}
	}

	@Test
	void foodIsSpreadOverTheTroughsSlots() {
		AnimalPenBlockEntity pen = pen(true);
		pen.setPonderFood(new ItemStack(Items.WHEAT, 200));
		for (int i = 0; i < 3; i++)
			assertEquals(64, pen.getFood().getStackInSlot(i).getCount());
		assertEquals(8, pen.getFood().getStackInSlot(3).getCount());
		assertTrue(pen.getFood().getStackInSlot(4).isEmpty());
		assertSame(Items.WHEAT, pen.getTroughFood().getItem());
		// Each slot hands out a whole normal stack
		assertEquals(64, pen.getFood().extractItem(1, 64, false).getCount());
	}

	@Test
	void unstackableFoodTakesASlotEach() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderFood(new ItemStack(Items.TROPICAL_FISH_BUCKET, 1));
		assertEquals(1, pen.getFood().getStackInSlot(0).getCount());
		assertTrue(pen.getFood().getStackInSlot(1).isEmpty());
	}

	// ---------------------------------------------------------------- the herd

	@Test
	void emptyPenHasNoAnimal() {
		AnimalPenBlockEntity pen = pen(false);
		assertFalse(pen.hasAnimal());
		assertEquals(0, pen.getCount());
		assertEquals(0, pen.getComparatorOutput());
		assertEquals(0, pen.ticksToNextGrown());
		assertEquals(0, pen.getFruitfulLevel());
		assertEquals(lang("gui.animal_pen.empty"), pen.getStatus());
	}

	@Test
	void herdCountsAdultsAndYoung() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 3, 2);
		assertTrue(pen.hasAnimal());
		assertEquals(3, pen.getAdults());
		assertEquals(2, pen.getYoung());
		assertEquals(5, pen.getCount());
		assertSame(EntityType.COW, BottledAnimalItem.typeOf(pen.getCaptive()));
		assertEquals(1, pen.getCaptive().getCount());
	}

	@Test
	void aBottleWithNoAnimalsCountedIsNotAnAnimal() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 0, 0);
		assertFalse(pen.hasAnimal());
	}

	@Test
	void youngGrowUpAfterTheConfiguredTime() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.PIG), 1, 4);
		assertEquals(20 * 1200, pen.ticksToNextGrown());
		TestConfig.set(CMConfig.SERVER.penGrowSeconds, 60);
		pen.setPonderHerd(bottle(EntityType.PIG), 1, 4);
		assertEquals(20 * 60, pen.ticksToNextGrown());
	}

	@Test
	void nothingHasBredYet() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.PIG), 2, 0);
		assertEquals(0, pen.getBreedProgress());
		assertTrue(pen.isWoolReady());
	}

	// ---------------------------------------------------------------- running and the status line

	@Test
	void runsOnlyWhileTurningWithAnAnimal() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setSpeed(32);
		assertFalse(pen.isRunning());
		pen.setPonderHerd(bottle(EntityType.PIG), 2, 0);
		assertTrue(pen.isRunning());
		pen.setSpeed(0);
		assertFalse(pen.isRunning());
	}

	@Test
	void statusSaysWhyItIsNotBreeding() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.PIG), 2, 0);
		assertEquals(lang("gui.animal_pen.idle"), pen.getStatus());

		pen.setSpeed(32);
		// No food in the trough
		assertEquals(lang("gui.animal_pen.hungry"), pen.getStatus());
		assertFalse(pen.isBreeding());

		pen.setPonderHerd(bottle(EntityType.PIG), 0, 1);
		assertEquals(lang("gui.animal_pen.growing"), pen.getStatus());

		pen.setPonderHerd(bottle(EntityType.PIG), pen.getCapacity(), 0);
		assertEquals(lang("gui.animal_pen.full"), pen.getStatus());
	}

	// ---------------------------------------------------------------- comparator

	@Test
	void comparatorShowsHowFullItIs() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 1, 0);
		// Never 0 while there's an animal
		assertEquals(1, pen.getComparatorOutput());
		pen.setPonderHerd(bottle(EntityType.COW), 32, 32);
		assertEquals(8, pen.getComparatorOutput());
		pen.setPonderHerd(bottle(EntityType.COW), 128, 0);
		assertEquals(15, pen.getComparatorOutput());
	}

	@Test
	void comparatorNeverGoesAboveFifteen() {
		// A herd from when pens could be configured to hold more
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 300, 0);
		assertEquals(15, pen.getComparatorOutput());
	}

	// ---------------------------------------------------------------- the screen's animal slot (client-side guess, no level)

	@Test
	void animalSlotTakesOnlyFilledBottles() {
		IItemHandlerModifiable slot = pen(false).getCaptiveSlot();
		assertTrue(slot.isItemValid(0, bottle(EntityType.SHEEP)));
		assertFalse(slot.isItemValid(0, new ItemStack(ModItems.BOTTLED_ANIMAL.get())));
		assertFalse(slot.isItemValid(0, new ItemStack(ModItems.CAPTURE_BOTTLE.get())));
		assertFalse(slot.isItemValid(0, new ItemStack(Items.WHEAT)));
		assertEquals(1, slot.getSlotLimit(0));
	}

	@Test
	void animalSlotHandsOutACopy() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.SHEEP), 2, 0);
		ItemStack shown = pen.getCaptiveSlot().getStackInSlot(0);
		shown.setCount(0);
		assertFalse(pen.getCaptive().isEmpty());
		assertTrue(pen.hasAnimal());
	}

	@Test
	void animalSlotRefusesASecondAnimal() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.SHEEP), 2, 0);
		ItemStack other = bottle(EntityType.COW);
		assertSame(other, pen.getCaptiveSlot().insertItem(0, other, false));
		assertSame(EntityType.SHEEP, BottledAnimalItem.typeOf(pen.getCaptive()));
	}

	@Test
	void simulatedInsertChangesNothing() {
		AnimalPenBlockEntity pen = pen(false);
		ItemStack left = pen.getCaptiveSlot().insertItem(0, bottle(EntityType.COW), true);
		assertTrue(left.isEmpty());
		assertFalse(pen.hasAnimal());
	}

	@Test
	void simulatedExtractChangesNothing() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 3, 0);
		ItemStack out = pen.getCaptiveSlot().extractItem(0, 1, true);
		assertSame(EntityType.COW, BottledAnimalItem.typeOf(out));
		assertEquals(3, pen.getAdults());
		assertTrue(pen.getCaptiveSlot().extractItem(0, 0, false).isEmpty());
	}

	@Test
	void clientGuessPutsOneAnimalInAndTakesTheHerdOut() {
		AnimalPenBlockEntity pen = pen(false);
		pen.getCaptiveSlot().setStackInSlot(0, bottle(EntityType.CHICKEN));
		assertTrue(pen.hasAnimal());
		assertEquals(1, pen.getAdults());
		assertEquals(1, pen.getCaptive().getCount());
		pen.getCaptiveSlot().setStackInSlot(0, ItemStack.EMPTY);
		assertFalse(pen.hasAnimal());
		assertEquals(0, pen.getCount());
	}

	// ---------------------------------------------------------------- automation

	@Test
	void automationTakesOnlyFromTheOutput() {
		AnimalPenBlockEntity pen = pen(false);
		IItemHandler automation = pen.getAutomationHandler();
		// The trough's 2 slots (8 on the Brass pen), then the output
		assertEquals(2 + 9, automation.getSlots());
		assertEquals(8 + 18, pen(true).getAutomationHandler().getSlots());

		pen.setPonderFood(new ItemStack(Items.WHEAT, 20));
		pen.getOutput().setStackInSlot(2, new ItemStack(Items.BEEF, 7));
		assertEquals(20, automation.getStackInSlot(0).getCount());
		assertSame(Items.BEEF, automation.getStackInSlot(2 + 2).getItem());

		// The trough can't be emptied by pipes, the output can
		assertTrue(automation.extractItem(0, 64, false).isEmpty());
		assertEquals(20, pen.getFood().getStackInSlot(0).getCount());
		ItemStack beef = automation.extractItem(2 + 2, 64, false);
		assertEquals(7, beef.getCount());
		assertTrue(pen.getOutput().getStackInSlot(2).isEmpty());
	}

	@Test
	void automationPutsNothingIntoTheOutput() {
		IItemHandler automation = pen(false).getAutomationHandler();
		ItemStack beef = new ItemStack(Items.BEEF, 5);
		int output = pen(false).getFood().getSlots();
		assertSame(beef, automation.insertItem(output, beef, false));
		assertFalse(automation.isItemValid(output, beef));
	}

	@Test
	void pipesTakeMilkButPutNoneIn() {
		Assumptions.assumeTrue(NeoForgeMod.MILK.isBound(), "milk fluid not enabled");
		AnimalPenBlockEntity pen = pen(false);
		IFluidHandler pipes = pen.getMilkHandler();
		FluidStack milk = new FluidStack(NeoForgeMod.MILK.get(), 1000);
		assertEquals(0, pipes.fill(milk.copy(), IFluidHandler.FluidAction.EXECUTE));
		assertFalse(pipes.isFluidValid(0, milk));

		assertEquals(1000, pen.getMilk().fill(milk.copy(), IFluidHandler.FluidAction.EXECUTE));
		assertEquals(250, pipes.drain(250, IFluidHandler.FluidAction.EXECUTE).getAmount());
		assertEquals(750, pen.getMilk().getFluidAmount());
		assertEquals(8_000, pipes.getTankCapacity(0));
	}

	// ---------------------------------------------------------------- moving and saving

	@Test
	void clearingLeavesNothingToDropTwice() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 5, 3);
		pen.setPonderFood(new ItemStack(Items.WHEAT, 30));
		pen.getOutput().setStackInSlot(0, new ItemStack(Items.LEATHER, 4));
		pen.clearContent();
		assertFalse(pen.hasAnimal());
		assertTrue(pen.getCaptive().isEmpty());
		assertEquals(0, pen.getCount());
		assertTrue(pen.getTroughFood().isEmpty());
		assertFalse(pen.hasOutput());
	}

	@Test
	void savesAndLoadsTheHerd() {
		AnimalPenBlockEntity pen = pen(true);
		pen.setPonderHerd(bottle(EntityType.SHEEP), 3, 2);
		// Over several of the trough's slots
		pen.setPonderFood(new ItemStack(Items.WHEAT, 200));
		pen.getOutput().setStackInSlot(4, new ItemStack(Items.MUTTON, 7));
		CompoundTag tag = pen.saveWithoutMetadata(registries);

		AnimalPenBlockEntity loaded = pen(true);
		loaded.loadWithComponents(tag, registries);
		assertSame(EntityType.SHEEP, BottledAnimalItem.typeOf(loaded.getCaptive()));
		assertEquals(3, loaded.getAdults());
		assertEquals(2, loaded.getYoung());
		assertEquals(pen.ticksToNextGrown(), loaded.ticksToNextGrown());
		for (int i = 0; i < 4; i++) {
			assertSame(Items.WHEAT, loaded.getFood().getStackInSlot(i).getItem());
			assertEquals(i < 3 ? 64 : 8, loaded.getFood().getStackInSlot(i).getCount());
		}
		assertSame(Items.MUTTON, loaded.getOutput().getStackInSlot(4).getItem());
		assertEquals(7, loaded.getOutput().getStackInSlot(4).getCount());
		assertTrue(loaded.isWoolReady());
	}

	@Test
	void emptyPenSavesAndLoadsEmpty() {
		CompoundTag tag = pen(false).saveWithoutMetadata(registries);
		AnimalPenBlockEntity loaded = pen(false);
		loaded.loadWithComponents(tag, registries);
		assertFalse(loaded.hasAnimal());
		assertTrue(loaded.getCaptive().isEmpty());
	}

	@Test
	void loadingAnotherPensDataReplacesTheHerd() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 4, 0);
		CompoundTag tag = pen.saveWithoutMetadata(registries);

		AnimalPenBlockEntity other = pen(false);
		other.setPonderHerd(bottle(EntityType.PIG), 9, 9);
		other.loadWithComponents(tag, registries);
		assertSame(EntityType.COW, BottledAnimalItem.typeOf(other.getCaptive()));
		assertEquals(4, other.getAdults());
		assertEquals(0, other.getYoung());
	}

	@Test
	void pensSavedBeforeYoungCountEveryAnimalAsGrown() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 1, 0);
		CompoundTag tag = pen.saveWithoutMetadata(registries);
		tag.remove("Adults");
		tag.remove("Young");
		tag.remove("WoolReady");
		tag.putInt("Count", 7);

		AnimalPenBlockEntity loaded = pen(false);
		loaded.loadWithComponents(tag, registries);
		assertEquals(7, loaded.getAdults());
		assertEquals(0, loaded.getYoung());
		// Sheep in old pens had their wool
		assertTrue(loaded.isWoolReady());
	}

	@Test
	void troughsSavedAsAPlainHandlerStillLoad() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 1, 0);
		CompoundTag tag = pen.saveWithoutMetadata(registries);
		ItemStackHandler old = new ItemStackHandler(1);
		old.setStackInSlot(0, new ItemStack(Items.WHEAT, 12));
		tag.put("Food", old.serializeNBT(registries));

		AnimalPenBlockEntity loaded = pen(false);
		loaded.loadWithComponents(tag, registries);
		assertSame(Items.WHEAT, loaded.getFood().getStackInSlot(0).getItem());
		assertEquals(12, loaded.getFood().getStackInSlot(0).getCount());
	}

	@Test
	void anOldBigStackTroughIsSpreadOverTheNewSlots() {
		// Before the trough had several slots it was one big stack, saved like the upgraded chests' stacks
		AnimalPenBlockEntity pen = pen(true);
		CompoundTag tag = pen.saveWithoutMetadata(registries);
		NonNullList<ItemStack> old = NonNullList.withSize(1, ItemStack.EMPTY);
		old.set(0, new ItemStack(Items.WHEAT, 256));
		tag.put("Food", ChestStacks.save(old, registries));

		AnimalPenBlockEntity loaded = pen(true);
		loaded.loadWithComponents(tag, registries);
		for (int i = 0; i < 4; i++)
			assertEquals(64, loaded.getFood().getStackInSlot(i).getCount());
		assertTrue(loaded.getFood().getStackInSlot(4).isEmpty());
	}

	@Test
	void oldUnstackableFoodBeyondTheTroughIsNotKeptInIt() {
		// An old trough could stack unstackable food (tropical fish buckets) 64 deep; only one fits each slot now,
		// and the rest is put out with the output on the next tick
		AnimalPenBlockEntity pen = pen(false);
		CompoundTag tag = pen.saveWithoutMetadata(registries);
		NonNullList<ItemStack> old = NonNullList.withSize(1, ItemStack.EMPTY);
		old.set(0, new ItemStack(Items.TROPICAL_FISH_BUCKET, 10));
		tag.put("Food", ChestStacks.save(old, registries));

		AnimalPenBlockEntity loaded = pen(false);
		loaded.loadWithComponents(tag, registries);
		assertEquals(1, loaded.getFood().getStackInSlot(0).getCount());
		assertEquals(1, loaded.getFood().getStackInSlot(1).getCount());
	}

	@Test
	void eachLitterIsSavedWithItsOwnTime() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 2, 0);
		CompoundTag tag = pen.saveWithoutMetadata(registries);
		ListTag litters = new ListTag();
		for (int[] litter : new int[][] {{3, 500}, {2, 100}}) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("Count", litter[0]);
			entry.putInt("Ticks", litter[1]);
			litters.add(entry);
		}
		tag.put("Young", litters);

		AnimalPenBlockEntity loaded = pen(false);
		loaded.loadWithComponents(tag, registries);
		assertEquals(5, loaded.getYoung());
		assertEquals(7, loaded.getCount());
		// The soonest of the two
		assertEquals(100, loaded.ticksToNextGrown());
	}

	// ---------------------------------------------------------------- syncing

	/** The bottle carries the whole animal, so the frequent syncs leave it out until it changes; the client keeps it. */
	@Test
	void runningSyncsSendTheCaptiveOnlyWhenItChanges() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 3, 0);
		ownSync(pen);
		CompoundTag first = packet(pen);
		assertTrue(first.contains("Captive"));
		CompoundTag second = packet(pen);
		assertFalse(second.contains("Captive"));
		assertTrue(second.getBoolean("SameCaptive"));

		AnimalPenBlockEntity client = pen(false);
		client.readClient(first, registries);
		client.readClient(second, registries);
		assertSame(EntityType.COW, BottledAnimalItem.typeOf(client.getCaptive()), "the client keeps the animal it was sent");
		assertEquals(3, client.getAdults());

		pen.setPonderHerd(bottle(EntityType.PIG), 2, 0);
		CompoundTag third = packet(pen);
		assertTrue(third.contains("Captive"), "a new animal goes out");
		client.readClient(third, registries);
		assertSame(EntityType.PIG, BottledAnimalItem.typeOf(client.getCaptive()));
	}

	@Test
	void savesAndFullUpdatesAlwaysCarryTheCaptive() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 3, 0);
		packet(pen);
		assertTrue(pen.getUpdateTag(registries).contains("Captive"));
		assertTrue(pen.saveWithoutMetadata(registries).contains("Captive"));
	}

	/** Anything but the pen's own sync (here, a refused break) may be filling in a fresh client copy: it gets the lot. */
	@Test
	void otherUpdatePacketsCarryTheCaptive() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 3, 0);
		ownSync(pen);
		packet(pen);
		com.comedymonkey.createaddons.testutil.ReflectionAccess.set(pen, "ownSyncAt", -10L);
		assertTrue(packet(pen).contains("Captive"));
	}

	@Test
	void theCaptiveIsResentNowAndThen() {
		AnimalPenBlockEntity pen = pen(false);
		pen.setPonderHerd(bottle(EntityType.COW), 3, 0);
		ownSync(pen);
		packet(pen);
		// as if sent long ago
		com.comedymonkey.createaddons.testutil.ReflectionAccess.set(pen, "sentCaptiveAt", -1000L);
		assertTrue(packet(pen).contains("Captive"));
	}

	/** As if the pen had just sent a sync of its own (a pen with no level is at game time 0). */
	private static void ownSync(AnimalPenBlockEntity pen) {
		com.comedymonkey.createaddons.testutil.ReflectionAccess.set(pen, "ownSyncAt", 0L);
	}

	private CompoundTag packet(AnimalPenBlockEntity pen) {
		return (CompoundTag) com.comedymonkey.createaddons.testutil.ReflectionAccess.call(pen, "updatePacketTag", registries);
	}
}
