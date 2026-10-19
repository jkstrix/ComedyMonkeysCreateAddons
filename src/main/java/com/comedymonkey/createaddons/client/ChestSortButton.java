package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.UpgradedChestView;
import com.comedymonkey.createaddons.network.SortChestPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Adds a small Sort button, an up and a down arrow, to the top right of upgraded chest and barrel screens. */
public final class ChestSortButton {
	private static final int WIDTH = 16;
	private static final int HEIGHT = 12;
	private static final String UP = "↑";
	private static final String DOWN = "↓";

	private ChestSortButton() {}

	public static void onScreenInit(ScreenEvent.Init.Post event) {
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof UpgradedChestView))
			return;
		int x = screen.getGuiLeft() + screen.getXSize() - 7 - WIDTH;
		int y = screen.getGuiTop() + 4;
		Button button = new Button(x, y, WIDTH, HEIGHT, Component.empty(),
				b -> PacketDistributor.sendToServer(new SortChestPayload()), narration -> narration.get()) {
			@Override
			protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
				super.renderWidget(graphics, mouseX, mouseY, partialTick);
				// Two arrows with a gap between them, rather than one cramped glyph
				Font font = Minecraft.getInstance().font;
				int colour = isHoveredOrFocused() ? 0xFFFFA0 : 0xFFFFFF;
				graphics.drawString(font, UP, getX() + 3, getY() + 2, colour, true);
				graphics.drawString(font, DOWN, getX() + getWidth() - 3 - font.width(DOWN), getY() + 2, colour, true);
			}
		};
		button.setTooltip(Tooltip.create(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.sort_chest")));
		event.addListener(button);
	}
}
