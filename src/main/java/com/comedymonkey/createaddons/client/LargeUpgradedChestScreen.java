package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.LargeUpgradedChestMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Screen for the 108-slot diamond double chest (12 x 9), styled like the vanilla chest screen. */
public class LargeUpgradedChestScreen extends AbstractContainerScreen<LargeUpgradedChestMenu> {
	private static final ResourceLocation TEXTURE = ComedyMonkeysCreateAddons.asResource("textures/gui/large_chest.png");
	public static final int WIDTH = LargeUpgradedChestMenu.CHEST_X * 2 + LargeUpgradedChestMenu.COLUMNS * 18 - 2;
	public static final int HEIGHT = LargeUpgradedChestMenu.HOTBAR_Y + 24;

	public LargeUpgradedChestScreen(LargeUpgradedChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = WIDTH;
		imageHeight = HEIGHT;
		inventoryLabelX = LargeUpgradedChestMenu.INVENTORY_X;
		inventoryLabelY = LargeUpgradedChestMenu.INVENTORY_Y - 11;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 512);
	}
}
