package com.comedymonkey.createaddons.content.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.SettlingImpact;
import com.comedymonkey.createaddons.content.SyncStagger;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The Fish Tank's performance fixes that hold off a level: the creature's type read once per bottle, the trough and
 * net sent to clients as a summary, the bottle kept when it's read again, the queued sync cleared by any sync, and a
 * client not making a creature to know whether it breeds.
 */
class FishTankBlockEntityTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void setUp() {
		TestConfig.loadDefaults();
		TestConfig.reset();
		registries = VanillaRegistries.createLookup();
	}

	@AfterEach
	void reset() {
		TestConfig.reset();
	}

	private static FishTankBlockEntity tank() {
		return new FishTankBlockEntity(BlockPos.ZERO, ModBlocks.FISH_TANK.get().defaultBlockState());
	}

	static ItemStack bottle(EntityType<?> type) {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", EntityType.getKey(type).toString());
		ItemStack stack = new ItemStack(ModItems.BOTTLED_ANIMAL.get());
		stack.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
		return stack;
	}

	/** A tank as a client sees it: it goes by the server's summary of its trough and net. */
	static final class ClientTank extends FishTankBlockEntity {
		ClientTank() {
			super(BlockPos.ZERO, ModBlocks.FISH_TANK.get().defaultBlockState());
		}

		@Override
		boolean showsSummary() {
			return true;
		}
	}

	private static EntityType<?> type(FishTankBlockEntity tank) {
		return (EntityType<?>) ReflectionAccess.get(tank, "creatureType");
	}

	@Test
	void typeFollowsTheCaptive() {
		FishTankBlockEntity tank = tank();
		assertNull(type(tank));
		tank.setPonderSchool(bottle(EntityType.AXOLOTL), 2, 0, 0);
		assertSame(EntityType.AXOLOTL, type(tank));
		assertTrue(tank.isAxolotl());
		tank.setPonderSchool(bottle(EntityType.COD), 2, 0, 0);
		assertSame(EntityType.COD, type(tank));
		assertFalse(tank.isAxolotl());
		tank.clearContent();
		assertNull(type(tank));
		// the bottle slot's client path (off a level)
		tank.getCaptiveSlot().setStackInSlot(0, bottle(EntityType.SALMON));
		assertSame(EntityType.SALMON, type(tank));
		FishTankBlockEntity loaded = tank();
		loaded.loadWithComponents(tank.saveWithFullMetadata(registries), registries);
		assertSame(EntityType.SALMON, type(loaded));
	}

	@Test
	void readKeepsTheSameStackWhenTheBottleIsTheSame() {
		FishTankBlockEntity tank = tank();
		tank.setPonderSchool(bottle(EntityType.COD), 3, 0, 0);
		ItemStack before = tank.getCaptive();
		tank.loadWithComponents(tank.saveWithFullMetadata(registries), registries);
		assertSame(before, tank.getCaptive());
		FishTankBlockEntity other = tank();
		other.setPonderSchool(bottle(EntityType.SALMON), 3, 0, 0);
		tank.loadWithComponents(other.saveWithFullMetadata(registries), registries);
		assertNotSame(before, tank.getCaptive());
		assertSame(EntityType.SALMON, type(tank));
	}

	/** The update tag, read as a client packet by a fresh tank, gives the same food and net without the inventories. */
	@Test
	void foodAndNetSummaryRoundTrips() {
		FishTankBlockEntity server = tank();
		server.setPonderSchool(bottle(EntityType.COD), 2, 0, 0);
		server.getFood().setStackInSlot(1, new ItemStack(ModItems.FISH_FOOD.get(), 40));
		ItemStack[] made = {new ItemStack(Items.COD, 3), new ItemStack(Items.BONE_MEAL, 1), new ItemStack(Items.COD, 64),
			new ItemStack(Items.BONE, 2)};
		for (int i = 0; i < made.length; i++)
			server.getOutput().setStackInSlot(8 - i, made[i].copy());

		CompoundTag tag = server.getUpdateTag(registries);
		assertFalse(tag.contains("Food"));
		assertFalse(tag.contains("Output"));
		ClientTank client = new ClientTank();
		client.handleUpdateTag(tag, registries);
		assertTrue(ItemStack.isSameItemSameComponents(server.getShownFood(), client.getShownFood()));
		assertEquals(40, client.getFoodCount());
		assertEquals(4, client.getTrayUsed());
		assertEquals(3, client.getTray().size());
		// the net shows the first stacks in slot order: slots 5, 6 and 7
		assertTrue(client.getTray().get(0).is(Items.BONE));
		assertTrue(client.getTray().get(1).is(Items.COD));
		assertTrue(client.getTray().get(2).is(Items.BONE_MEAL));
		assertTrue(client.hasOutput());

		server.clearContent();
		client.handleUpdateTag(server.getUpdateTag(registries), registries);
		assertTrue(client.getShownFood().isEmpty());
		assertEquals(0, client.getFoodCount());
		assertFalse(client.hasOutput());
	}

	/** A client goes by the creature's type to know it breeds: no tank in sight makes a creature just to ask. */
	@Test
	void aClientKnowsItBreedsWithoutMakingACreature() {
		ClientTank client = new ClientTank();
		client.setPonderSchool(bottle(EntityType.COD), 2, 0, 0);
		client.getFood().setStackInSlot(0, new ItemStack(ModItems.FISH_FOOD.get(), 8));
		client.getWater().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
		client.setSpeed(64);
		assertTrue(client.isBreeding());
		assertNull(ReflectionAccess.get(client, "sample"), "a creature was made");
		// a bottle whose creature's mod is gone: no type, no breeding
		ItemStack gone = bottle(EntityType.COD);
		CompoundTag tag = new CompoundTag();
		tag.putString("id", "gone_mod:fish");
		gone.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
		client.setPonderSchool(gone, 2, 0, 0);
		assertFalse(client.isBreeding());
	}

	/** A packet that leaves the bottle out puts back the one the server last sent, undoing a client's guessed click. */
	@Test
	void aPacketWithoutTheBottleUndoesAClientsGuess() {
		FishTankBlockEntity tank = tank();
		tank.setPonderSchool(bottle(EntityType.COD), 3, 0, 0);
		CompoundTag full = tank.getUpdateTag(registries);
		ClientTank client = new ClientTank();
		client.handleUpdateTag(full, registries);
		ItemStack sent = client.getCaptive();
		client.getCaptiveSlot().setStackInSlot(0, ItemStack.EMPTY);
		CompoundTag without = full.copy();
		without.remove("Captive");
		without.putBoolean("SameCaptive", true);
		client.handleUpdateTag(without, registries);
		assertSame(sent, client.getCaptive());
		assertTrue(client.hasCreature());
	}

	@Test
	void aClientsSaveLeavesOutItsInventories() {
		ClientTank client = new ClientTank();
		client.getFood().setStackInSlot(0, new ItemStack(ModItems.FISH_FOOD.get(), 9));
		CompoundTag copy = client.saveCustomOnly(registries);
		assertFalse(copy.contains("Food"));
		assertFalse(copy.contains("Output"));
		FishTankBlockEntity server = tank();
		server.getFood().setStackInSlot(0, new ItemStack(ModItems.FISH_FOOD.get(), 9));
		assertTrue(server.saveCustomOnly(registries).contains("Food"));
	}

	/**
	 * The goggles show the impact the server's network holds, settled, not what the client works out it wants: here
	 * the tank wants nothing but the network keeps its load a while longer, and the client shows that load.
	 */
	@Test
	void theGogglesShowTheSettledImpact() {
		FishTankBlockEntity server = tank();
		// young growing: the tank wants its load
		server.setPonderSchool(bottle(EntityType.COD), 0, 2, 0);
		SettlingImpact impact = (SettlingImpact) ReflectionAccess.get(server, "impact");
		float load = server.calculateStressApplied();
		assertTrue(load > 0);
		impact.settle(load);
		server.clearContent();
		impact.settle(0);
		assertEquals(load, impact.applied());
		ClientTank client = new ClientTank();
		client.handleUpdateTag(server.getUpdateTag(registries), registries);
		assertEquals(load, client.calculateStressApplied(), "the client's own guess would be 0");
		for (int tick = 0; tick < SettlingImpact.SETTLE_TICKS; tick++)
			impact.settle(0);
		client.handleUpdateTag(server.getUpdateTag(registries), registries);
		assertEquals(0, client.calculateStressApplied());
		// a tank never settled (Ponder) goes by what it wants
		assertEquals(0, new ClientTank().calculateStressApplied());
	}

	@Test
	void sendDataClearsTheQueuedSync() {
		FishTankBlockEntity tank = tank();
		tank.getFood().setStackInSlot(0, new ItemStack(ModItems.FISH_FOOD.get(), 5));
		assertTrue((Boolean) ReflectionAccess.get(tank, "syncQueued"));
		tank.sendData();
		assertFalse((Boolean) ReflectionAccess.get(tank, "syncQueued"));
	}

	@Test
	void syncPhaseIsByPosition() {
		BlockPos pos = new BlockPos(17, 70, -3);
		FishTankBlockEntity tank = new FishTankBlockEntity(pos, ModBlocks.FISH_TANK.get().defaultBlockState());
		assertEquals(SyncStagger.phase(pos, 10), ReflectionAccess.get(tank, "syncPhase"));
	}

	@Test
	void renderBoxIsTheBlockMadeOnce() {
		FishTankBlockEntity tank = tank();
		assertSame(tank.getRenderBox(), tank.getRenderBox());
		assertEquals(new net.minecraft.world.phys.AABB(BlockPos.ZERO), tank.getRenderBox());
	}
}
