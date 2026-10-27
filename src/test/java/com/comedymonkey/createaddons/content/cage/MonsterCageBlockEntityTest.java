package com.comedymonkey.createaddons.content.cage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.SyncStagger;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.testutil.NoCopyEntityData;
import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/**
 * The Monster Cage's performance fixes that hold off a level: the monster's type read once per jar, the bowl and tray
 * sent to clients as a summary, the captive left out of its own syncs, the queued sync cleared by any sync, the sync
 * phase, and the Netherite cage's FE through the menu's data slots. (Ticking, drowning, culling and the energy upkeep
 * need a level: see CagePerformanceGameTests.)
 */
class MonsterCageBlockEntityTest {
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

	private static MonsterCageBlockEntity cage(Block block) {
		return new MonsterCageBlockEntity(BlockPos.ZERO, block.defaultBlockState());
	}

	private static MonsterCageBlockEntity cage() {
		return cage(ModBlocks.MONSTER_CAGE.get());
	}

	static ItemStack jar(EntityType<?> type) {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", EntityType.getKey(type).toString());
		ItemStack stack = new ItemStack(type == EntityType.WITHER ? ModItems.NETHERITE_JAR.get() : ModItems.IRON_JAR.get());
		stack.set(DataComponents.ENTITY_DATA, CustomData.of(tag));
		stack.set(DataComponents.MAX_STACK_SIZE, 1);
		return stack;
	}

	/** A cage as a client sees it: it goes by the server's summary of its bowl and tray. */
	static final class ClientCage extends MonsterCageBlockEntity {
		ClientCage() {
			super(BlockPos.ZERO, ModBlocks.MONSTER_CAGE.get().defaultBlockState());
		}

		@Override
		boolean showsSummary() {
			return true;
		}
	}

	// ---------------------------------------------------------------- the monster's type

	/** The jar's data throws if it's copied: reading the type must read it in place. */
	@Test
	void typeOfReadsTheJarWithoutCopyingIt() {
		ItemStack jar = new ItemStack(ModItems.IRON_JAR.get());
		jar.set(DataComponents.ENTITY_DATA, NoCopyEntityData.of("minecraft:zombie"));
		assertSame(EntityType.ZOMBIE, MonsterJarItem.typeOf(jar));
		ItemStack bottle = new ItemStack(ModItems.BOTTLED_ANIMAL.get());
		bottle.set(DataComponents.ENTITY_DATA, NoCopyEntityData.of("minecraft:cod"));
		assertSame(EntityType.COD, BottledAnimalItem.typeOf(bottle));
	}

	@Test
	void typeFollowsTheCaptiveIn() {
		MonsterCageBlockEntity cage = cage();
		assertNull(cage.getMonsterType());
		ReflectionAccess.call(cage, "setCaptive", jar(EntityType.ZOMBIE));
		assertSame(EntityType.ZOMBIE, cage.getMonsterType());
		ReflectionAccess.call(cage, "setCaptive", jar(EntityType.SKELETON));
		assertSame(EntityType.SKELETON, cage.getMonsterType());
		ReflectionAccess.call(cage, "setCaptive", ItemStack.EMPTY);
		assertNull(cage.getMonsterType());
	}

	@Test
	void typeFollowsPonderHordeAndClearing() {
		MonsterCageBlockEntity cage = cage();
		cage.setPonderHorde(jar(EntityType.SPIDER), 12, 0);
		assertSame(EntityType.SPIDER, cage.getMonsterType());
		cage.clearContent();
		assertNull(cage.getMonsterType());
	}

	/** Off a level the jar slot takes the client's path, as the menu's slot sync does on a client. */
	@Test
	void typeFollowsTheClientsJarSlot() {
		MonsterCageBlockEntity cage = cage();
		cage.getCaptiveSlot().setStackInSlot(0, jar(EntityType.HUSK));
		assertSame(EntityType.HUSK, cage.getMonsterType());
		cage.getCaptiveSlot().setStackInSlot(0, ItemStack.EMPTY);
		assertNull(cage.getMonsterType());
	}

	@Test
	void typeFollowsALoad() {
		MonsterCageBlockEntity saved = cage();
		saved.setPonderHorde(jar(EntityType.CREEPER), 3, 0);
		MonsterCageBlockEntity loaded = cage();
		loaded.loadWithComponents(saved.saveWithFullMetadata(registries), registries);
		assertSame(EntityType.CREEPER, loaded.getMonsterType());
		assertEquals(3, loaded.getCount());
	}

	/** The same jar read again keeps the stack it had, so what's cached against it (the drawn jar's monster) stays. */
	@Test
	void readKeepsTheSameStackWhenTheJarIsTheSame() {
		MonsterCageBlockEntity cage = cage();
		cage.setPonderHorde(jar(EntityType.ZOMBIE), 4, 0);
		ItemStack before = cage.getCaptive();
		CompoundTag tag = cage.saveWithFullMetadata(registries);
		cage.loadWithComponents(tag, registries);
		assertSame(before, cage.getCaptive());
		cage.handleUpdateTag(cage.getUpdateTag(registries), registries);
		assertSame(before, cage.getCaptive());

		MonsterCageBlockEntity other = cage();
		other.setPonderHorde(jar(EntityType.SKELETON), 4, 0);
		cage.loadWithComponents(other.saveWithFullMetadata(registries), registries);
		assertNotSame(before, cage.getCaptive());
		assertSame(EntityType.SKELETON, cage.getMonsterType());
	}

	// ---------------------------------------------------------------- what clients are sent

	private static CompoundTag updateTag(MonsterCageBlockEntity cage) {
		return (CompoundTag) ReflectionAccess.call(cage, "updatePacketTag", registries);
	}

	/** A packet the cage's own sync left the jar out of (what the server sends then). */
	private static CompoundTag withoutJar(CompoundTag full) {
		CompoundTag tag = full.copy();
		tag.remove("Captive");
		tag.putBoolean("SameCaptive", true);
		return tag;
	}

	/**
	 * Building an update packet doesn't count the jar as sent (only a sync, which broadcasts to every client watching,
	 * does: see CagePerformanceGameTests), so a packet built for one player never makes the others miss it.
	 */
	@Test
	void buildingAPacketDoesNotCountTheJarAsSent() {
		MonsterCageBlockEntity cage = cage();
		ReflectionAccess.call(cage, "setCaptive", jar(EntityType.ZOMBIE));
		assertTrue(updateTag(cage).contains("Captive"));
		assertTrue(updateTag(cage).contains("Captive"));
		assertNull(ReflectionAccess.get(cage, "sentCaptive"));
		// and a client first seeing the cage always gets it
		assertTrue(cage.getUpdateTag(registries).contains("Captive"));
	}

	/** A client that has the jar keeps that very stack through packets that leave it out. */
	@Test
	void aClientKeepsTheJarThroughPacketsWithoutIt() {
		MonsterCageBlockEntity cage = cage();
		cage.setPonderHorde(jar(EntityType.ZOMBIE), 5, 0);
		CompoundTag full = cage.getUpdateTag(registries);
		ClientCage client = new ClientCage();
		client.handleUpdateTag(full, registries);
		ItemStack had = client.getCaptive();
		client.handleUpdateTag(withoutJar(full), registries);
		assertSame(had, client.getCaptive());
		assertSame(EntityType.ZOMBIE, client.getMonsterType());
	}

	/**
	 * A click in the jar slot changes a client's jar at once, but the server may refuse it (a cage in lock-down). A
	 * packet without the jar then puts back the one the server last sent, not the client's guess.
	 */
	@Test
	void aPacketWithoutTheJarUndoesAClientsGuess() {
		MonsterCageBlockEntity cage = cage();
		cage.setPonderHorde(jar(EntityType.ZOMBIE), 5, 0);
		CompoundTag full = cage.getUpdateTag(registries);
		ClientCage client = new ClientCage();
		client.handleUpdateTag(full, registries);
		ItemStack sent = client.getCaptive();
		// the client's guess: the jar taken out
		client.getCaptiveSlot().setStackInSlot(0, ItemStack.EMPTY);
		assertTrue(client.getCaptive().isEmpty());
		client.handleUpdateTag(withoutJar(full), registries);
		assertSame(sent, client.getCaptive());
		assertSame(EntityType.ZOMBIE, client.getMonsterType());
		assertTrue(client.hasMonster());
	}

	/** The update tag, read as a client packet by a fresh cage, gives the same bowl and tray without the inventories. */
	@Test
	void bowlAndTraySummaryRoundTrips() {
		MonsterCageBlockEntity server = cage(ModBlocks.BRASS_MONSTER_CAGE.get());
		server.setPonderHorde(jar(EntityType.BLAZE), 10, 0);
		server.getChow().setStackInSlot(0, new ItemStack(ModItems.MONSTER_CHOW.get(), 64));
		server.getChow().setStackInSlot(2, new ItemStack(ModItems.MONSTER_CHOW.get(), 10));
		ItemStack[] made = {new ItemStack(Items.BLAZE_ROD, 5), new ItemStack(Items.GOLD_INGOT, 2), new ItemStack(Items.BLAZE_ROD, 64),
			new ItemStack(Items.COAL, 1), new ItemStack(Items.BONE, 9)};
		for (int i = 0; i < made.length; i++)
			server.getOutput().setStackInSlot(i * 2, made[i].copy());

		CompoundTag tag = server.getUpdateTag(registries);
		assertFalse(tag.contains("Chow"), "clients aren't sent the chow bin");
		assertFalse(tag.contains("Output"), "clients aren't sent the output");

		ClientCage client = new ClientCage();
		client.handleUpdateTag(tag, registries);
		assertTrue(ItemStack.isSameItemSameComponents(server.getBowlChow(), client.getBowlChow()));
		assertEquals(74, client.getChowCount());
		assertEquals(server.getChowCount(), client.getChowCount());
		assertEquals(5, client.getTrayUsed());
		assertEquals(3, client.getTray().size());
		for (int i = 0; i < 3; i++)
			assertTrue(ItemStack.isSameItemSameComponents(made[i], client.getTray().get(i)), "tray item " + i);
		assertTrue(client.hasOutput());
		assertEquals(server.hasOutput(), client.hasOutput());
		// what the cage does with it reads the same either side
		assertEquals(server.getStatus(), client.getStatus());

		// emptied, and sent again
		server.clearContent();
		client.handleUpdateTag(server.getUpdateTag(registries), registries);
		assertTrue(client.getBowlChow().isEmpty());
		assertEquals(0, client.getChowCount());
		assertEquals(0, client.getTrayUsed());
		assertFalse(client.hasOutput());
	}

	/** On a client the inventories change only through an open menu; the bowl follows them. */
	@Test
	void summaryFollowsAnOpenMenusSlots() {
		ClientCage client = new ClientCage();
		client.getChow().setStackInSlot(1, new ItemStack(ModItems.MONSTER_CHOW.get(), 7));
		assertEquals(7, client.getChowCount());
		client.getOutput().setStackInSlot(4, new ItemStack(Items.ARROW, 3));
		assertEquals(1, client.getTrayUsed());
		assertTrue(client.hasOutput());
	}

	/** A save, unlike a client packet, keeps the inventories whole. */
	@Test
	void savesStillHoldTheInventories() {
		MonsterCageBlockEntity cage = cage();
		cage.getChow().setStackInSlot(0, new ItemStack(ModItems.MONSTER_CHOW.get(), 20));
		cage.getOutput().setStackInSlot(8, new ItemStack(Items.BONE, 3));
		MonsterCageBlockEntity loaded = cage();
		loaded.loadWithComponents(cage.saveWithFullMetadata(registries), registries);
		assertEquals(20, loaded.getChow().getStackInSlot(0).getCount());
		assertEquals(3, loaded.getOutput().getStackInSlot(8).getCount());
	}

	// ---------------------------------------------------------------- syncing

	/** Any sync sends what was queued, so the queue is cleared and the same changes don't go out again. */
	@Test
	void sendDataClearsTheQueuedSync() {
		MonsterCageBlockEntity cage = cage();
		cage.getChow().setStackInSlot(0, new ItemStack(ModItems.MONSTER_CHOW.get(), 5));
		assertTrue((Boolean) ReflectionAccess.get(cage, "syncQueued"), "a change queues a sync");
		cage.sendData();
		assertFalse((Boolean) ReflectionAccess.get(cage, "syncQueued"));
	}

	@Test
	void syncPhaseIsByPosition() {
		for (BlockPos pos : new BlockPos[] {BlockPos.ZERO, new BlockPos(1, 64, 0), new BlockPos(-300, 12, 77)}) {
			MonsterCageBlockEntity cage = new MonsterCageBlockEntity(pos, ModBlocks.MONSTER_CAGE.get().defaultBlockState());
			assertEquals(SyncStagger.phase(pos, 10), ReflectionAccess.get(cage, "syncPhase"));
		}
		assertEquals(10, ReflectionAccess.get(cage(), "SYNC_INTERVAL"));
	}

	// ---------------------------------------------------------------- the Netherite cage's FE

	/**
	 * The FE goes as one data slot: NeoForge sends container data to its clients as var-ints (its
	 * AdvancedContainerSetDataPayload), so a value over 16 bits arrives whole, in one packet.
	 */
	@Test
	void menuCarriesTheWholeFeInOneSlot() {
		MonsterCageBlockEntity cage = cage(ModBlocks.NETHERITE_MONSTER_CAGE.get());
		ReflectionAccess.call(cage.getEnergy(), "set", 200_000);
		MonsterCageMenu menu = new MonsterCageMenu(1, new Inventory(null), cage);
		assertEquals(200_000, menu.getEnergy());
		ContainerData sent = (ContainerData) ReflectionAccess.get(menu, "energy");
		assertEquals(1, sent.getCount());
		for (int fe : new int[] {200_000, 65_536, 100_000_000}) {
			AdvancedContainerSetDataPayload payload = new AdvancedContainerSetDataPayload((byte) 1, (short) 0, fe);
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
			AdvancedContainerSetDataPayload.STREAM_CODEC.encode(buffer, payload);
			SimpleContainerData received = new SimpleContainerData(1);
			received.set(0, AdvancedContainerSetDataPayload.STREAM_CODEC.decode(buffer).value());
			ReflectionAccess.set(menu, "energy", received);
			assertEquals(fe, menu.getEnergy());
		}
	}

	/**
	 * With its field off the cage takes no FE at all (user, 2026-10-27), simulated or not, so the power stays on the
	 * network; it notes each offer, which decides when the field may come back.
	 */
	@Test
	void anOffFieldRefusesFeAndNotesTheOffer() {
		MonsterCageBlockEntity cage = cage(ModBlocks.NETHERITE_MONSTER_CAGE.get());
		assertFalse(cage.isFieldUp());
		assertEquals(0, cage.getEnergy().receiveEnergy(5_000, true));
		assertEquals(0, cage.getEnergy().receiveEnergy(5_000, false));
		assertEquals(0, cage.getEnergyStored());
		assertEquals(10_000L, ReflectionAccess.get(cage, "offeredNow"));
		// a field up takes it as before
		cage.setPonderField(true, -1);
		assertEquals(5_000, cage.getEnergy().receiveEnergy(5_000, false));
		assertEquals(10_000L, ReflectionAccess.get(cage, "offeredNow"), "offers only count while it's off");
	}

	/**
	 * On trial, real FE comes in at most at the field's cost a tick (kept, not drawn); a simulation still answers with
	 * the room, as a Power Line plans a second ahead with one, and a pusher keeps what it couldn't put in.
	 */
	@Test
	void aTrialTakesAtMostTheFieldCostATick() {
		MonsterCageBlockEntity cage = cage(ModBlocks.NETHERITE_MONSTER_CAGE.get());
		ReflectionAccess.set(cage, "startingFor", 0);
		int cost = 2048;
		assertEquals(50_000, cage.getEnergy().receiveEnergy(50_000, true));
		assertEquals(cost, cage.getEnergy().receiveEnergy(50_000, false));
		assertEquals(0, cage.getEnergy().receiveEnergy(50_000, false), "the tick's cost is in");
		assertEquals(cost, cage.getEnergyStored());
		assertEquals(0L, ReflectionAccess.get(cage, "offeredNow"), "on trial, offers aren't noted");
	}

	/** Cages without a field take no FE and note nothing. */
	@Test
	void otherCagesTakeNoFe() {
		MonsterCageBlockEntity cage = cage();
		assertEquals(0, cage.getEnergy().receiveEnergy(5_000, false));
		assertEquals(0L, ReflectionAccess.get(cage, "offeredNow"));
	}

	// ---------------------------------------------------------------- a client's copy

	/** A client's inventories aren't the cage's: a creative pick-block copy of the client's cage leaves them out. */
	@Test
	void aClientsSaveLeavesOutItsInventories() {
		ClientCage client = new ClientCage();
		client.getChow().setStackInSlot(0, new ItemStack(ModItems.MONSTER_CHOW.get(), 9));
		client.getOutput().setStackInSlot(0, new ItemStack(Items.BONE, 2));
		CompoundTag copy = client.saveCustomOnly(registries);
		assertFalse(copy.contains("Chow"));
		assertFalse(copy.contains("Output"));
		MonsterCageBlockEntity server = cage();
		server.getChow().setStackInSlot(0, new ItemStack(ModItems.MONSTER_CHOW.get(), 9));
		assertTrue(server.saveCustomOnly(registries).contains("Chow"));
		// pasted, the copy makes a cage with an empty bin, not a broken one
		MonsterCageBlockEntity pasted = cage();
		pasted.loadWithComponents(copy, registries);
		assertEquals(2, pasted.getChow().getSlots());
		assertTrue(pasted.getChow().getStackInSlot(0).isEmpty());
	}
}
