package com.comedymonkey.createaddons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

class ComedyMonkeysCreateAddonsTest {
	@Test
	void modIsLoaded() {
		assertTrue(ModList.get().isLoaded(ComedyMonkeysCreateAddons.MOD_ID));
	}

	/** Carry On's default blacklist matches mod ids containing "create", which would make none of our blocks carryable. */
	@Test
	void modIdDoesNotContainCreate() {
		assertFalse(ComedyMonkeysCreateAddons.MOD_ID.toLowerCase(Locale.ROOT).contains("create"));
	}

	@Test
	void asResourceIsInTheModsNamespace() {
		ResourceLocation id = ComedyMonkeysCreateAddons.asResource("animal_pen");
		assertEquals(ComedyMonkeysCreateAddons.MOD_ID, id.getNamespace());
		assertEquals("animal_pen", id.getPath());
	}
}
