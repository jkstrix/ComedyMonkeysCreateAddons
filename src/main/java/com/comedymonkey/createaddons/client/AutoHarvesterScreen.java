package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlockEntity;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterMenu;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

/** Wrench screen of the Auto Harvester: planter on the left, harvest in the middle, growth gauge on the right. */
public class AutoHarvesterScreen extends AbstractContainerScreen<AutoHarvesterMenu> {
	private static final ResourceLocation TEXTURE = ComedyMonkeysCreateAddons.asResource("textures/gui/auto_harvester.png");
	private static final ResourceLocation SPROUT = ComedyMonkeysCreateAddons.asResource("block/auto_harvester_crop");

	// Gauge interior, relative to the GUI origin (must match the texture)
	private static final int GAUGE_X = 144, GAUGE_Y = 18, GAUGE_W = 16, GAUGE_H = 54;
	private static final int STATUS_Y = 78;
	private static final int GROWTH = 0xFF587B3A;
	private static final int GROWTH_TOP = 0xFF6E924A;
	private static final int HARVEST = 0xFF9C8A4C;

	public AutoHarvesterScreen(AutoHarvesterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = AutoHarvesterMenu.WIDTH;
		imageHeight = MenuLayout.screenHeight(AutoHarvesterMenu.HEIGHT);
		inventoryLabelX = GuiKit.inventoryLabelX(AutoHarvesterMenu.WIDTH);
		inventoryLabelY = GuiKit.inventoryLabelY(AutoHarvesterMenu.HEIGHT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		if (isHovering(GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY))
			graphics.renderComponentTooltip(font, gaugeTooltip(), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		AutoHarvesterBlockEntity be = menu.getBlockEntity();

		// Growth fills the gauge from the bottom; while it harvests the gauge shows the cut
		boolean harvesting = be.getHarvestProgress(0) >= 0;
		float fill = harvesting ? 1 : be.getGrowth(partialTick);
		int height = Math.round(GAUGE_H * fill);
		if (height > 0) {
			int x = leftPos + GAUGE_X, bottom = topPos + GAUGE_Y + GAUGE_H;
			graphics.fill(x, bottom - height, x + GAUGE_W, bottom, harvesting ? HARVEST : GROWTH);
			graphics.fill(x, bottom - height, x + GAUGE_W, bottom - height + 1, harvesting ? HARVEST : GROWTH_TOP);
		}

		// A faint sprout in the empty planter slot
		if (!be.hasPlant()) {
			TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SPROUT);
			RenderSystem.enableBlend();
			graphics.blit(leftPos + AutoHarvesterMenu.PLANTER_X, topPos + AutoHarvesterMenu.PLANTER_Y, 0, 16, 16, sprite,
				1, 1, 1, 0.3f);
			RenderSystem.disableBlend();
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		GuiKit.title(graphics, font, title, AutoHarvesterMenu.WIDTH, GuiKit.TITLE);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiKit.INVENTORY_TEXT, false);
		graphics.drawString(font, tr("gui.auto_harvester.plant"), 20, 22, GuiKit.LABEL, false);
		AutoHarvesterBlockEntity be = menu.getBlockEntity();
		graphics.drawString(font, be.getStatus(), 8, STATUS_Y, be.isWorking() ? GuiKit.TEXT : GuiKit.BAD, false);
	}

	private List<Component> gaugeTooltip() {
		AutoHarvesterBlockEntity be = menu.getBlockEntity();
		List<Component> lines = new ArrayList<>();
		lines.add(be.getStatus());
		if (be.hasPlant())
			lines.add(Component.literal(String.format(Locale.ROOT, "%.1f ", be.getSecondsPerHarvest()))
				.append(tr("gui.goggles.auto_harvester.per_harvest")).withStyle(ChatFormatting.GRAY));
		int bountiful = be.getBountifulLevel();
		if (bountiful > 0)
			lines.add(tr("gui.goggles.auto_harvester.bountiful", Component.translatable("enchantment.level." + bountiful))
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		return lines;
	}

	private static net.minecraft.network.chat.MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + "." + key, args);
	}
}
