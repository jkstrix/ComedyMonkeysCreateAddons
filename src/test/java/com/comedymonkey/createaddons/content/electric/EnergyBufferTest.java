package com.comedymonkey.createaddons.content.electric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.IEnergyStorage;

class EnergyBufferTest {
	private final AtomicInteger changes = new AtomicInteger();

	@BeforeEach
	void resetCounter() {
		changes.set(0);
	}

	private EnergyBuffer buffer(int capacity, int maxReceive, int maxExtract) {
		return new EnergyBuffer(capacity, maxReceive, maxExtract, changes::incrementAndGet);
	}

	// ---------------------------------------------------------------- construction

	@Test
	void startsEmptyWithItsLimits() {
		EnergyBuffer buffer = buffer(1000, 100, 50);
		assertEquals(0, buffer.getEnergyStored());
		assertEquals(1000, buffer.getMaxEnergyStored());
		assertTrue(buffer.canReceive());
		assertTrue(buffer.canExtract());
		assertFalse(buffer.isFull());
		assertEquals(0, changes.get());
	}

	@Test
	void zeroLimitsMeanNoReceiveOrExtract() {
		EnergyBuffer buffer = buffer(1000, 0, 0);
		assertFalse(buffer.canReceive());
		assertFalse(buffer.canExtract());
		assertEquals(0, buffer.receiveEnergy(500, false));
		buffer.setEnergy(500);
		assertEquals(0, buffer.extractEnergy(500, false));
		assertEquals(500, buffer.getEnergyStored());
		assertEquals(0, changes.get());
	}

	// ---------------------------------------------------------------- receiveEnergy

	@Test
	void receiveIsCappedByMaxReceive() {
		EnergyBuffer buffer = buffer(1000, 100, 100);
		assertEquals(100, buffer.receiveEnergy(500, false));
		assertEquals(100, buffer.getEnergyStored());
		assertEquals(1, changes.get());
	}

	@Test
	void receiveIsCappedByRoomLeft() {
		EnergyBuffer buffer = buffer(1000, 1000, 1000);
		buffer.setEnergy(950);
		assertEquals(50, buffer.receiveEnergy(500, false));
		assertEquals(1000, buffer.getEnergyStored());
		assertTrue(buffer.isFull());
	}

	@Test
	void simulatedReceiveChangesNothingAndDoesNotNotify() {
		EnergyBuffer buffer = buffer(1000, 200, 200);
		assertEquals(200, buffer.receiveEnergy(500, true));
		assertEquals(0, buffer.getEnergyStored());
		assertEquals(0, changes.get());
	}

	@Test
	void receivingIntoAFullBufferDoesNotNotify() {
		EnergyBuffer buffer = buffer(1000, 1000, 1000);
		buffer.setEnergy(1000);
		assertEquals(0, buffer.receiveEnergy(10, false));
		assertEquals(0, changes.get());
	}

	@Test
	void zeroOrNegativeReceiveIsIgnored() {
		EnergyBuffer buffer = buffer(1000, 1000, 1000);
		buffer.setEnergy(500);
		assertEquals(0, buffer.receiveEnergy(0, false));
		assertEquals(0, buffer.receiveEnergy(-100, false));
		assertEquals(500, buffer.getEnergyStored());
		assertEquals(0, changes.get());
	}

	// ---------------------------------------------------------------- extractEnergy

	@Test
	void extractIsCappedByMaxExtractAndStored() {
		EnergyBuffer buffer = buffer(1000, 1000, 100);
		buffer.setEnergy(150);
		assertEquals(100, buffer.extractEnergy(500, false));
		assertEquals(50, buffer.getEnergyStored());
		assertEquals(50, buffer.extractEnergy(500, false));
		assertEquals(0, buffer.getEnergyStored());
		assertEquals(2, changes.get());
	}

	@Test
	void simulatedExtractChangesNothingAndDoesNotNotify() {
		EnergyBuffer buffer = buffer(1000, 1000, 1000);
		buffer.setEnergy(300);
		assertEquals(300, buffer.extractEnergy(500, true));
		assertEquals(300, buffer.getEnergyStored());
		assertEquals(0, changes.get());
	}

	@Test
	void extractingFromEmptyOrNonPositiveIsIgnored() {
		EnergyBuffer buffer = buffer(1000, 1000, 1000);
		assertEquals(0, buffer.extractEnergy(100, false));
		buffer.setEnergy(100);
		assertEquals(0, buffer.extractEnergy(0, false));
		assertEquals(0, buffer.extractEnergy(-5, false));
		assertEquals(100, buffer.getEnergyStored());
		assertEquals(0, changes.get());
	}

	// ---------------------------------------------------------------- generate (internal, ignores receive limit)

	@Test
	void generateIgnoresTheReceiveLimit() {
		EnergyBuffer buffer = buffer(1000, 0, 1000);
		assertEquals(700, buffer.generate(700));
		assertEquals(700, buffer.getEnergyStored());
		assertEquals(1, changes.get());
	}

	@Test
	void generateOnlyAddsWhatFits() {
		EnergyBuffer buffer = buffer(1000, 0, 1000);
		buffer.setEnergy(900);
		assertEquals(100, buffer.generate(500));
		assertEquals(1000, buffer.getEnergyStored());
		assertEquals(0, buffer.generate(500));
		assertEquals(1000, buffer.getEnergyStored());
		assertEquals(1, changes.get());
	}

	@Test
	void generatingNothingDoesNotNotify() {
		EnergyBuffer buffer = buffer(1000, 0, 1000);
		assertEquals(0, buffer.generate(0));
		assertEquals(0, changes.get());
	}

	@Test
	void negativeGenerateNeverRemovesEnergy() {
		EnergyBuffer buffer = buffer(1000, 0, 1000);
		buffer.setEnergy(400);
		buffer.generate(-300);
		assertEquals(400, buffer.getEnergyStored());
		assertEquals(0, changes.get());
	}

	@Test
	void generateDoesNotOverflowAtIntMax() {
		EnergyBuffer buffer = buffer(Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE, buffer.generate(Integer.MAX_VALUE));
		assertEquals(0, buffer.generate(Integer.MAX_VALUE));
		assertEquals(Integer.MAX_VALUE, buffer.getEnergyStored());
		assertTrue(buffer.isFull());
	}

	// ---------------------------------------------------------------- consume (internal, ignores extract limit)

	@Test
	void consumeIgnoresTheExtractLimit() {
		EnergyBuffer buffer = buffer(1000, 1000, 0);
		buffer.setEnergy(800);
		assertEquals(600, buffer.consume(600));
		assertEquals(200, buffer.getEnergyStored());
		assertEquals(1, changes.get());
	}

	@Test
	void consumeOnlyTakesWhatIsStored() {
		EnergyBuffer buffer = buffer(1000, 1000, 0);
		buffer.setEnergy(50);
		assertEquals(50, buffer.consume(500));
		assertEquals(0, buffer.getEnergyStored());
		assertEquals(0, buffer.consume(500));
		assertEquals(1, changes.get());
	}

	@Test
	void negativeConsumeNeverAddsEnergy() {
		EnergyBuffer buffer = buffer(1000, 1000, 0);
		buffer.setEnergy(400);
		buffer.consume(-300);
		assertEquals(400, buffer.getEnergyStored());
		assertEquals(0, changes.get());
	}

	@Test
	void generateThenConsumeConservesEnergy() {
		EnergyBuffer buffer = buffer(10_000, 0, 0);
		int in = 0, out = 0;
		for (int i = 0; i < 50; i++) {
			in += buffer.generate(373);
			out += buffer.consume(211);
		}
		assertEquals(in - out, buffer.getEnergyStored());
		assertTrue(buffer.getEnergyStored() >= 0 && buffer.getEnergyStored() <= buffer.getMaxEnergyStored());
	}

	// ---------------------------------------------------------------- setEnergy / isFull

	@Test
	void setEnergyClampsToRangeAndDoesNotNotify() {
		EnergyBuffer buffer = buffer(1000, 1000, 1000);
		buffer.setEnergy(5000);
		assertEquals(1000, buffer.getEnergyStored());
		buffer.setEnergy(-20);
		assertEquals(0, buffer.getEnergyStored());
		buffer.setEnergy(Integer.MIN_VALUE);
		assertEquals(0, buffer.getEnergyStored());
		buffer.setEnergy(Integer.MAX_VALUE);
		assertEquals(1000, buffer.getEnergyStored());
		assertEquals(0, changes.get());
	}

	@Test
	void isFullOnlyAtCapacity() {
		EnergyBuffer buffer = buffer(1000, 1000, 1000);
		buffer.setEnergy(999);
		assertFalse(buffer.isFull());
		buffer.setEnergy(1000);
		assertTrue(buffer.isFull());
	}

	// ---------------------------------------------------------------- extractOnly view

	@Test
	void extractOnlyViewNeverReceives() {
		EnergyBuffer buffer = buffer(1000, 1000, 1000);
		IEnergyStorage view = buffer.extractOnly();
		assertFalse(view.canReceive());
		assertTrue(view.canExtract());
		assertEquals(0, view.receiveEnergy(500, false));
		assertEquals(0, view.receiveEnergy(500, true));
		assertEquals(0, buffer.getEnergyStored());
		assertEquals(0, changes.get());
	}

	@Test
	void extractOnlyViewExtractsThroughTheBuffer() {
		EnergyBuffer buffer = buffer(1000, 1000, 300);
		buffer.setEnergy(800);
		IEnergyStorage view = buffer.extractOnly();
		assertEquals(800, view.getEnergyStored());
		assertEquals(1000, view.getMaxEnergyStored());
		assertEquals(300, view.extractEnergy(1000, true));
		assertEquals(800, buffer.getEnergyStored());
		assertEquals(0, changes.get());
		assertEquals(300, view.extractEnergy(1000, false));
		assertEquals(500, buffer.getEnergyStored());
		assertEquals(500, view.getEnergyStored());
		assertEquals(1, changes.get());
	}

	@Test
	void extractOnlyViewTracksLaterChanges() {
		EnergyBuffer buffer = buffer(1000, 0, 1000);
		IEnergyStorage view = buffer.extractOnly();
		buffer.generate(250);
		assertEquals(250, view.getEnergyStored());
	}

	// ---------------------------------------------------------------- serialization (inherited from EnergyStorage)

	@Test
	void serializesAsTheStoredAmount() {
		EnergyBuffer buffer = buffer(1000, 1000, 1000);
		buffer.setEnergy(640);
		Tag tag = buffer.serializeNBT(null);
		IntTag intTag = assertInstanceOf(IntTag.class, tag);
		assertEquals(640, intTag.getAsInt());

		EnergyBuffer copy = buffer(1000, 1000, 1000);
		copy.deserializeNBT(null, tag);
		assertEquals(640, copy.getEnergyStored());
	}

	@Test
	void setEnergyRoundTripLosesNothingWithinCapacity() {
		EnergyBuffer buffer = buffer(100_000, 0, 100_000);
		buffer.generate(12_345);
		EnergyBuffer loaded = buffer(100_000, 0, 100_000);
		loaded.setEnergy(buffer.getEnergyStored());
		assertEquals(12_345, loaded.getEnergyStored());
	}
}
