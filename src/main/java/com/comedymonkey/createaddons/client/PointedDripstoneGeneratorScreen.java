package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.dripstone.PointedDripstoneGeneratorBlockEntity;
import com.comedymonkey.createaddons.content.dripstone.PointedDripstoneGeneratorMenu;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Wrench screen of a Pointed Dripstone Generator, in the GUI kit on the Animal Pens' layout (as the Fish Tank's): the
 * seed slot on the left, the growth and water gauges beside it, the store in the middle, and what it's doing underneath.
 */
public class PointedDripstoneGeneratorScreen extends AbstractContainerScreen<PointedDripstoneGeneratorMenu> {
	private static final ResourceLocation TEXTURE = ComedyMonkeysCreateAddons.asResource("textures/gui/pointed_dripstone_generator.png");
	private static final int GROW_X = 30, WATER_X = 44, GAUGE_Y = 18, GAUGE_W = 10, GAUGE_H = 54;
	private static final int STATUS_Y = 102, RATE_Y = 112;
	/** Growth in dripstone brown, water in the Fish Tank's blue. */
	private static final int GROW = 0xFF7A5C46, GROW_TOP = 0xFF96745A;
	private static final int WATER = 0xFF2F6A80, WATER_TOP = 0xFF3F8AA6;

	public PointedDripstoneGeneratorScreen(PointedDripstoneGeneratorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = PointedDripstoneGeneratorMenu.WIDTH;
		imageHeight = MenuLayout.screenHeight(PointedDripstoneGeneratorMenu.HEIGHT);
		inventoryLabelX = GuiKit.inventoryLabelX(PointedDripstoneGeneratorMenu.WIDTH);
		inventoryLabelY = GuiKit.inventoryLabelY(PointedDripstoneGeneratorMenu.HEIGHT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		PointedDripstoneGeneratorBlockEntity be = menu.getBlockEntity();
		if (isHovering(GROW_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY))
			graphics.renderComponentTooltip(font, growTooltip(be), mouseX, mouseY);
		else if (isHovering(WATER_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY))
			graphics.renderComponentTooltip(font, List.of(tr("water"), Component.literal(String.format(Locale.ROOT, "%,d / %,d mB",
				be.getWater().getFluidAmount(), be.getWater().getCapacity())).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
		else if (isHovering(PointedDripstoneGeneratorMenu.SEED_X, PointedDripstoneGeneratorMenu.SEED_Y, 16, 16, mouseX, mouseY)
			&& menu.getCarried().isEmpty() && be.getSeedCount() == 0)
			graphics.renderComponentTooltip(font, List.of(tr("seed"), tr("seed_hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		PointedDripstoneGeneratorBlockEntity be = menu.getBlockEntity();
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		gauge(graphics, GROW_X, be.getGrowth(partialTick), GROW, GROW_TOP);
		gauge(graphics, WATER_X, be.getWater().getFluidAmount() / (float) Math.max(1, be.getWater().getCapacity()), WATER, WATER_TOP);
		if (be.getSeedCount() == 0) {
			RenderSystem.enableBlend();
			int x = leftPos + PointedDripstoneGeneratorMenu.SEED_X, y = topPos + PointedDripstoneGeneratorMenu.SEED_Y;
			graphics.renderFakeItem(new ItemStack(Items.POINTED_DRIPSTONE), x, y);
			graphics.fill(RenderType.guiOverlay(), x, y, x + 16, y + 16, 0xA062645F);
			RenderSystem.disableBlend();
		}
	}

	private void gauge(GuiGraphics graphics, int gx, float fill, int colour, int top) {
		int height = Math.round(GAUGE_H * Math.min(1, fill));
		if (height <= 0)
			return;
		int x = leftPos + gx, bottom = topPos + GAUGE_Y + GAUGE_H;
		graphics.fill(x, bottom - height, x + GAUGE_W, bottom, colour);
		graphics.fill(x, bottom - height, x + GAUGE_W, bottom - height + 1, top);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		GuiKit.title(graphics, font, title, PointedDripstoneGeneratorMenu.WIDTH, GuiKit.TITLE);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiKit.INVENTORY_TEXT, false);
		PointedDripstoneGeneratorBlockEntity be = menu.getBlockEntity();
		// scaled down to the panel if a translation runs long
		Component status = be.status();
		int room = PointedDripstoneGeneratorMenu.WIDTH - 16, width = font.width(status);
		float scale = width > room ? room / (float) width : 1;
		graphics.pose().pushPose();
		graphics.pose().translate(8, STATUS_Y + (1 - scale) * 4, 0);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, status, 0, 0, be.isRunning() ? GuiKit.TEXT : GuiKit.BAD, false);
		graphics.pose().popPose();
		graphics.drawString(font, tr("seeds", be.getSeedCount(), PointedDripstoneGeneratorBlockEntity.SEEDS), 8, RATE_Y, GuiKit.LABEL, false);
	}

	private List<Component> growTooltip(PointedDripstoneGeneratorBlockEntity be) {
		List<Component> lines = new ArrayList<>();
		lines.add(be.status());
		float seconds = be.secondsPerPiece();
		if (seconds > 0)
			lines.add(tr("per_piece", String.format(Locale.ROOT, "%.1f", seconds)).withStyle(ChatFormatting.GRAY));
		return lines;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.pointed_dripstone_generator." + key, args);
	}
}
