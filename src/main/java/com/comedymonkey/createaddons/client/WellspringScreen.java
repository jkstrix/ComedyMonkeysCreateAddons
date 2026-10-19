package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity;
import com.comedymonkey.createaddons.content.wellspring.WellspringMenu;
import com.comedymonkey.createaddons.content.wellspring.WellspringSource;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Upgrade GUI: generation and capacity slots on the left, readout in the middle, fluid gauge on the right. */
public class WellspringScreen extends AbstractContainerScreen<WellspringMenu> {
	private static final ResourceLocation TEXTURE = ComedyMonkeysCreateAddons.asResource("textures/gui/wellspring.png");

	// Gauge interior, relative to the GUI origin (must match the texture)
	private static final int GAUGE_X = 132;
	private static final int GAUGE_Y = 18;
	private static final int GAUGE_W = 36;
	private static final int GAUGE_H = 58;
	// Sprites in the texture beside the panel
	private static final int LOCK_U = 176, LOCK_V = 0;
	private static final int GLASS_U = 176, GLASS_V = 16;

	private static final int INFO_X = 68;
	private static final int TEXT = 0x3B2A20;
	private static final int LABEL = 0x6E5E4E;
	private static final int WATER = 0x2A5DB0;
	private static final int LAVA = 0xC0501A;
	private static final int FULL = 0xB02020;

	public WellspringScreen(WellspringMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 176;
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);

		if (isHovering(GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY)) {
			graphics.renderComponentTooltip(font, gaugeTooltip(), mouseX, mouseY);
			return;
		}
		for (Slot slot : menu.slots)
			if (slot instanceof WellspringMenu.UpgradeSlot upgradeSlot && !upgradeSlot.isActive()
				&& isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY))
				graphics.renderTooltip(font, tr("gui.wellspring.locked").withStyle(ChatFormatting.GRAY), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

		// Padlocks over slots beyond the configured limit
		for (Slot slot : menu.slots)
			if (slot instanceof WellspringMenu.UpgradeSlot upgradeSlot && !upgradeSlot.isActive())
				graphics.blit(TEXTURE, leftPos + slot.x, topPos + slot.y, LOCK_U, LOCK_V, 16, 16);

		renderFluid(graphics);
		RenderSystem.enableBlend();
		graphics.blit(TEXTURE, leftPos + GAUGE_X, topPos + GAUGE_Y, GLASS_U, GLASS_V, GAUGE_W, GAUGE_H);
		RenderSystem.disableBlend();
	}

	private void renderFluid(GuiGraphics graphics) {
		WellspringBlockEntity be = menu.getBlockEntity();
		FluidStack fluid = be.getTank().getFluid();
		if (fluid.isEmpty())
			return;

		int height = Math.max(1, Math.round(GAUGE_H * be.getFillFraction()));
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
		TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
		int tint = ext.getTintColor(fluid);
		float a = ((tint >> 24) & 0xFF) / 255f;
		float r = ((tint >> 16) & 0xFF) / 255f;
		float g = ((tint >> 8) & 0xFF) / 255f;
		float b = (tint & 0xFF) / 255f;

		int left = leftPos + GAUGE_X;
		int bottom = topPos + GAUGE_Y + GAUGE_H;
		graphics.enableScissor(left, bottom - height, left + GAUGE_W, bottom);
		RenderSystem.enableBlend();
		for (int y = bottom - 16; y > bottom - height - 16; y -= 16)
			for (int x = left; x < left + GAUGE_W; x += 16)
				graphics.blit(x, y, 0, 16, 16, sprite, r, g, b, a == 0 ? 1 : a);
		RenderSystem.disableBlend();
		graphics.disableScissor();
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
		graphics.drawString(font, tr("gui.wellspring.generation"), 8, 18, LABEL, false);
		graphics.drawString(font, tr("gui.wellspring.capacity"), 8, 50, LABEL, false);

		WellspringBlockEntity be = menu.getBlockEntity();
		int fluidColor = fluidColor(be);

		// Fill percentage, drawn large
		graphics.drawString(font, tr("gui.wellspring.fill"), INFO_X, 18, LABEL, false);
		graphics.pose().pushPose();
		graphics.pose().translate(INFO_X, 29, 0);
		graphics.pose().scale(1.5f, 1.5f, 1);
		graphics.drawString(font, be.getFillPercent() + "%", 0, 0, be.getTank().isEmpty() ? LABEL : fluidColor, false);
		graphics.pose().popPose();

		// Generation rate
		graphics.drawString(font, tr("gui.wellspring.output"), INFO_X, 50, LABEL, false);
		Component rate;
		int rateColor = fluidColor;
		if (be.getGenerationUpgradeCount() == 0) {
			rate = tr("gui.wellspring.no_output");
			rateColor = LABEL;
		} else if (be.getTank().getFluidAmount() >= be.getTank().getCapacity()) {
			rate = tr("gui.wellspring.full");
			rateColor = FULL;
		} else {
			rate = Component.literal(be.getGenerationRate() + " mB/s");
		}
		graphics.drawString(font, rate, INFO_X, 60, rateColor, false);
	}

	private List<Component> gaugeTooltip() {
		WellspringBlockEntity be = menu.getBlockEntity();
		FluidStack fluid = be.getTank().getFluid();
		List<Component> lines = new ArrayList<>();
		if (fluid.isEmpty()) {
			lines.add(tr("gui.wellspring.empty"));
		} else {
			lines.add(fluid.getHoverName());
			lines.add(Component.literal(be.getFillPercent() + "%").withStyle(ChatFormatting.GOLD));
		}
		lines.add(Component.literal(WellspringBlockEntity.mb(be.getTank().getFluidAmount()) + " / "
			+ WellspringBlockEntity.mb(be.getTank().getCapacity()) + " mB").withStyle(ChatFormatting.GRAY));
		return lines;
	}

	private static int fluidColor(WellspringBlockEntity be) {
		WellspringSource source = be.getGenerationSource();
		if (source == WellspringSource.NONE)
			source = WellspringSource.of(be.getTank().getFluid());
		return switch (source) {
			case LAVA -> LAVA;
			case HONEY -> 0xD8901C;
			case CHOCOLATE -> 0x5E3418;
			default -> WATER;
		};
	}

	private static net.minecraft.network.chat.MutableComponent tr(String key) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + "." + key);
	}
}
