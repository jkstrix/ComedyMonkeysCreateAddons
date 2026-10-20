package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The 3 or 6-row upgraded chest or barrel: a wood panel whose band takes the tier's colour, and the inventory under it. */
public class UpgradedChestScreen extends AbstractContainerScreen<UpgradedChestMenu> {
	private static final ResourceLocation THREE_ROWS = ComedyMonkeysCreateAddons.asResource("textures/gui/upgraded_chest.png");
	private static final ResourceLocation SIX_ROWS = ComedyMonkeysCreateAddons.asResource("textures/gui/upgraded_chest_large.png");

	private final ResourceLocation texture;
	private final int panelHeight;

	public UpgradedChestScreen(UpgradedChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		int rows = menu.getRowCount();
		texture = rows > 3 ? SIX_ROWS : THREE_ROWS;
		panelHeight = UpgradedChestMenu.height(rows);
		imageWidth = UpgradedChestMenu.WIDTH;
		imageHeight = MenuLayout.screenHeight(panelHeight);
		inventoryLabelX = GuiKit.inventoryLabelX(imageWidth);
		inventoryLabelY = GuiKit.inventoryLabelY(panelHeight);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		if (menu.getTier() != null)
			GuiKit.tintBand(graphics, texture, leftPos, topPos, imageWidth, 256, 256, menu.getTier());
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		GuiKit.title(graphics, font, title, imageWidth, menu.getTier() == null ? GuiKit.TITLE : GuiKit.titleColour(menu.getTier()));
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiKit.INVENTORY_TEXT, false);
	}

	/** Stacks of 100 or more: the count small enough to stay in its slot (shortened from 1,000), the exact one in the tooltip. */
	@Override
	protected void renderSlotContents(GuiGraphics graphics, ItemStack stack, Slot slot, @Nullable String countString) {
		if (countString != null || stack.getCount() < GuiKit.BIG_COUNT) {
			super.renderSlotContents(graphics, stack, slot, countString);
			return;
		}
		super.renderSlotContents(graphics, stack, slot, "");
		GuiKit.drawBigCount(graphics, font, slot.x, slot.y, stack.getCount());
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
		List<Component> lines = super.getTooltipFromContainerItem(stack);
		Component exact = GuiKit.exactCount(stack);
		if (exact != null) {
			lines = new ArrayList<>(lines);
			lines.add(1, exact);
		}
		return lines;
	}
}
