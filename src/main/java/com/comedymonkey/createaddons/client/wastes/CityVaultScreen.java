package com.comedymonkey.createaddons.client.wastes;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.GuiKit;
import com.comedymonkey.createaddons.client.UpgradedChestScreen;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * A City Vault's screen (D46, D59 option A, design/gui/city-vault.gui.js): the GUI kit's chest panel and inventory panel,
 * its material band void-alloy (a cast bar, lit along its top and left edges, a faint violet fall down its face) with a
 * lit starlight seam along its foot, the title in the starlight core tone. The band is its own texture, drawn over the
 * panel's grey band, never tinted; the body below is the kit's (D60).
 */
public class CityVaultScreen extends UpgradedChestScreen {
	/** The band's inside (174 x 12), from city-vault.gui.js --mod. */
	private static final ResourceLocation BAND = ComedyMonkeysCreateAddons.asResource("textures/gui/city_vault_band.png");
	private static final ResourceLocation PANEL = ComedyMonkeysCreateAddons.asResource("textures/gui/upgraded_chest.png");
	/** The starlight core tone (the world brief's), for the title. */
	public static final int TITLE = 0xB8B0D8;
	static final int BAND_W = UpgradedChestMenu.WIDTH - 2, BAND_H = MenuLayout.HEADER - 2;

	public CityVaultScreen(UpgradedChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(PANEL, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		graphics.blit(BAND, leftPos + 1, topPos + 1, 0, 0, BAND_W, BAND_H, BAND_W, BAND_H);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		GuiKit.title(graphics, font, title, imageWidth, TITLE);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiKit.INVENTORY_TEXT, false);
	}
}
