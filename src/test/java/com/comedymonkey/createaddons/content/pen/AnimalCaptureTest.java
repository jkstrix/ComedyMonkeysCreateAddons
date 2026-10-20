package com.comedymonkey.createaddons.content.pen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import net.minecraft.core.registries.Registries;

/** What can be tested without a world: the blacklist tag. Catching itself needs real entities. */
class AnimalCaptureTest {
	@Test
	void blacklistIsTheModsEntityTypeTag() {
		assertEquals(Registries.ENTITY_TYPE, AnimalCapture.BLACKLIST.registry());
		assertEquals(ComedyMonkeysCreateAddons.asResource("pen_blacklist"), AnimalCapture.BLACKLIST.location());
	}

	@Test
	void nothingIsNotAnAnimal() {
		assertFalse(AnimalCapture.canCapture(null, null));
	}
}
