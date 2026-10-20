package com.comedymonkey.createaddons.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.LargeUpgradedChestMenu;
import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterMenu;
import com.comedymonkey.createaddons.content.pen.AnimalPenMenu;
import com.comedymonkey.createaddons.content.wellspring.WellspringMenu;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;

import net.minecraft.world.inventory.Slot;

/** The shared layout the menus place their slots by, and the textures drawn to it. */
class MenuLayoutTest {
	@Test
	void inventoryPanelSitsUnderTheMachineWithAGap() {
		assertEquals(94, MenuLayout.inventoryTop(90));
		assertEquals(90 + MenuLayout.GAP + MenuLayout.INVENTORY_HEIGHT, MenuLayout.screenHeight(90));
		assertEquals(0, MenuLayout.inventoryLeft(176));
		// Centred under the 230-wide double chest
		assertEquals(27, MenuLayout.inventoryLeft(LargeUpgradedChestMenu.WIDTH));
	}

	@Test
	void playerSlotsAreInVanillasOrderInsideThePanel() {
		List<Slot> slots = new ArrayList<>();
		MenuLayout.addPlayerInventory(slots::add, null, 27, 190);
		assertEquals(36, slots.size());
		// The three rows of the inventory first (inventory slots 9..35), then the hotbar (0..8)
		assertEquals(9, slots.get(0).getContainerSlot());
		assertEquals(35, slots.get(26).getContainerSlot());
		assertEquals(0, slots.get(27).getContainerSlot());
		assertEquals(27 + 8, slots.get(0).x);
		assertEquals(190 + MenuLayout.INVENTORY_ROWS_Y, slots.get(0).y);
		assertEquals(190 + MenuLayout.INVENTORY_ROWS_Y + 36, slots.get(18).y);
		// The hotbar sits lower, and its last slot's frame stays inside the panel
		Slot last = slots.get(35);
		assertTrue(last.y > slots.get(18).y + 18);
		assertTrue(last.y + 17 < 190 + MenuLayout.INVENTORY_HEIGHT - 1);
		assertTrue(last.x + 17 <= 27 + MenuLayout.INVENTORY_WIDTH - 7);
	}

	/** Each screen's texture has to be at least as big as the screen drawn from it. */
	@Test
	void texturesCoverTheirScreens() throws IOException {
		check("auto_harvester.png", AutoHarvesterMenu.WIDTH, MenuLayout.screenHeight(AutoHarvesterMenu.HEIGHT));
		check("animal_pen.png", AnimalPenMenu.WIDTH, MenuLayout.screenHeight(AnimalPenMenu.HEIGHT));
		check("brass_animal_pen.png", AnimalPenMenu.WIDTH, MenuLayout.screenHeight(AnimalPenMenu.HEIGHT));
		check("wellspring.png", WellspringMenu.WIDTH, MenuLayout.screenHeight(WellspringMenu.HEIGHT));
		check("upgraded_chest.png", UpgradedChestMenu.WIDTH, MenuLayout.screenHeight(UpgradedChestMenu.height(3)));
		check("upgraded_chest_large.png", UpgradedChestMenu.WIDTH, MenuLayout.screenHeight(UpgradedChestMenu.height(6)));
		check("large_chest.png", LargeUpgradedChestMenu.WIDTH, MenuLayout.screenHeight(LargeUpgradedChestMenu.HEIGHT));
	}

	/** The shared sprites are drawn with blit() calls that don't give a texture size, which means 256x256. */
	@Test
	void widgetsAreTheSizeTheirBlitsAssume() throws IOException {
		int[] size = size("widgets.png");
		assertEquals(256, size[0]);
		assertEquals(256, size[1]);
	}

	private static void check(String file, int width, int height) throws IOException {
		int[] size = size(file);
		assertTrue(size[0] >= width && size[1] >= height,
			file + " is " + size[0] + "x" + size[1] + ", the screen needs " + width + "x" + height);
	}

	private static int[] size(String file) throws IOException {
		Path path = ModResourceFiles.find("assets", ComedyMonkeysCreateAddons.MOD_ID, "textures", "gui", file);
		assumeTrue(path != null, "resources not reachable");
		try (InputStream in = Files.newInputStream(path); DataInputStream data = new DataInputStream(in)) {
			data.skipBytes(16); // signature, IHDR length and type
			return new int[] {data.readInt(), data.readInt()};
		}
	}
}
