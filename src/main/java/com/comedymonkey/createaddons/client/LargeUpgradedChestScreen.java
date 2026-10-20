package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.chest.LargeUpgradedChestMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The 108-slot diamond double chest: 12x9 slots in a wood panel whose band takes the tier's colour. */
public class LargeUpgradedChestScreen extends AbstractContainerScreen<LargeUpgradedChestMenu> {
	private static final ResourceLocation TEXTURE = ComedyMonkeysCreateAddons.asResource("textures/gui/large_chest.png");

	public LargeUpgradedChestScreen(LargeUpgradedChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = LargeUpgradedChestMenu.WIDTH;
		imageHeight = MenuLayout.screenHeight(LargeUpgradedChestMenu.HEIGHT);
		inventoryLabelX = GuiKit.inventoryLabelX(LargeUpgradedChestMenu.WIDTH);
		inventoryLabelY = GuiKit.inventoryLabelY(LargeUpgradedChestMenu.HEIGHT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 512);
		if (menu.getTier() != null)
			GuiKit.tintBand(graphics, TEXTURE, leftPos, topPos, imageWidth, 256, 512, menu.getTier());
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
