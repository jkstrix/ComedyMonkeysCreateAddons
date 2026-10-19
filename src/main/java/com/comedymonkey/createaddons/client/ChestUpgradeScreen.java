package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.chest.ChestUpgradeMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Sneak + right-click GUI of an upgraded chest: just the upgrade slot. Hovering it explains the current tier and
 * whether it can be removed or downgraded.
 */
public class ChestUpgradeScreen extends AbstractContainerScreen<ChestUpgradeMenu> {
	private static final ResourceLocation TEXTURE = ComedyMonkeysCreateAddons.asResource("textures/gui/chest_upgrade.png");
	private static final int TEXT = 0x3B2A20;

	public ChestUpgradeScreen(ChestUpgradeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 50;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		if (menu.getCarried().isEmpty()
			&& isHovering(ChestUpgradeMenu.UPGRADE_X, ChestUpgradeMenu.UPGRADE_Y, 16, 16, mouseX, mouseY))
			graphics.renderComponentTooltip(font, slotTooltip(), mouseX, mouseY);
		else
			renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
	}

	private List<Component> slotTooltip() {
		List<Component> lines = new ArrayList<>();
		ChestTier tier = menu.getTier();
		if (tier == null)
			return lines;
		int multiplier = tier.multiplier();
		lines.add(tier.chestName());
		lines.add(tr("gui.chest_upgrade.stack_size", multiplier).withStyle(ChatFormatting.GOLD));
		lines.add(tr("gui.chest_upgrade.per_slot", 64 * multiplier).withStyle(ChatFormatting.GRAY));
		if (menu.getHalves() > 1)
			lines.add(tr("gui.chest_upgrade.double", menu.getHalves()).withStyle(ChatFormatting.GRAY));

		int lowest = menu.lowestFittingTier();
		if (lowest == ChestUpgradeMenu.FITS_VANILLA) {
			lines.add(tr("gui.chest_upgrade.removable").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			lines.add(tr("gui.chest_upgrade.too_full").withStyle(ChatFormatting.RED));
			lines.add(tr("gui.chest_upgrade.lowest_tier", ChestTier.values()[lowest].chestName())
				.withStyle(ChatFormatting.RED));
		}
		return lines;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + "." + key, args);
	}
}
