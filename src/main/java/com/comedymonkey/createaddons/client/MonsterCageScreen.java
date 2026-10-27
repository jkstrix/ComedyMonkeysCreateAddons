package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.cage.CageTier;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity;
import com.comedymonkey.createaddons.content.cage.MonsterCageMenu;
import com.comedymonkey.createaddons.registry.ModItems;
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

/**
 * Wrench screen of a Monster Cage, on the Animal Pens' artwork: the jar and the Monster Chow on the left, a growth gauge
 * and a second gauge (water in a Flooded cage, FE in the Netherite one), what it has made on the right, and the horde
 * and what it's doing underneath.
 */
public class MonsterCageScreen extends AbstractContainerScreen<MonsterCageMenu> {
	private static final ResourceLocation ANDESITE = ComedyMonkeysCreateAddons.asResource("textures/gui/animal_pen.png");
	private static final ResourceLocation BRASS = ComedyMonkeysCreateAddons.asResource("textures/gui/brass_animal_pen.png");
	private static final int GROW_X = 30, SECOND_X = 44, GAUGE_Y = 18, GAUGE_W = 10, GAUGE_H = 54;
	private static final int HORDE_Y = 102, STATUS_Y = 112;
	private static final int GROW = 0xFF7A8C4A, GROW_TOP = 0xFF93A85A;
	private static final int WATER = 0xFF2F6A80, WATER_TOP = 0xFF3F8AA6;
	private static final int FE = 0xFF3D82D8, FE_TOP = 0xFF6FB7E8;

	public MonsterCageScreen(MonsterCageMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = MonsterCageMenu.WIDTH;
		imageHeight = MenuLayout.screenHeight(MonsterCageMenu.HEIGHT);
		inventoryLabelX = GuiKit.inventoryLabelX(MonsterCageMenu.WIDTH);
		inventoryLabelY = GuiKit.inventoryLabelY(MonsterCageMenu.HEIGHT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		MonsterCageBlockEntity be = menu.getBlockEntity();
		if (isHovering(GROW_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY))
			graphics.renderComponentTooltip(font, growTooltip(be), mouseX, mouseY);
		else if (isHovering(SECOND_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY))
			graphics.renderComponentTooltip(font, secondTooltip(be), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		MonsterCageBlockEntity be = menu.getBlockEntity();
		graphics.blit(be.getTier() == CageTier.ANDESITE ? ANDESITE : BRASS, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		gauge(graphics, GROW_X, be.isGrowing() ? be.getProgress() : 0, GROW, GROW_TOP);
		if (be.isFlooded())
			gauge(graphics, SECOND_X, be.getWater().getFluidAmount() / (float) be.getWater().getCapacity(), WATER, WATER_TOP);
		else if (be.isNetherite())
			gauge(graphics, SECOND_X, be.getEnergyStored() / (float) Math.max(1, be.getEnergyCapacity()), FE, FE_TOP);
		RenderSystem.enableBlend();
		if (be.getCaptive().isEmpty())
			ghost(graphics, new ItemStack(be.isNetherite() ? ModItems.NETHERITE_JAR.get() : ModItems.IRON_JAR.get()),
				MonsterCageMenu.JAR_X, MonsterCageMenu.JAR_Y);
		if (be.getBowlChow().isEmpty())
			ghost(graphics, new ItemStack(ModItems.MONSTER_CHOW.get()), MonsterCageMenu.CHOW_X, MonsterCageMenu.CHOW_Y);
		RenderSystem.disableBlend();
	}

	private void gauge(GuiGraphics graphics, int gx, float fill, int colour, int top) {
		int height = Math.round(GAUGE_H * Math.min(1, fill));
		if (height <= 0)
			return;
		int x = leftPos + gx, bottom = topPos + GAUGE_Y + GAUGE_H;
		graphics.fill(x, bottom - height, x + GAUGE_W, bottom, colour);
		graphics.fill(x, bottom - height, x + GAUGE_W, bottom - height + 1, top);
	}

	private void ghost(GuiGraphics graphics, ItemStack stack, int x, int y) {
		graphics.renderFakeItem(stack, leftPos + x, topPos + y);
		graphics.fill(RenderType.guiOverlay(), leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xA062645F);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		GuiKit.title(graphics, font, title, MonsterCageMenu.WIDTH, GuiKit.TITLE);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiKit.INVENTORY_TEXT, false);
		MonsterCageBlockEntity be = menu.getBlockEntity();
		if (be.hasMonster())
			graphics.drawString(font, be.getHorde(), 8, HORDE_Y, GuiKit.TEXT, false);
		graphics.drawString(font, be.getStatus(), 8, be.hasMonster() ? STATUS_Y : HORDE_Y,
			be.isGrowing() ? GuiKit.TEXT : GuiKit.BAD, false);
	}

	private List<Component> growTooltip(MonsterCageBlockEntity be) {
		List<Component> lines = new ArrayList<>();
		lines.add(be.getStatus());
		if (be.hasMonster()) {
			lines.add(tr("chow_hint", be.chowCost()).withStyle(ChatFormatting.GRAY));
			float seconds = be.secondsPerMonster();
			if (seconds > 0)
				lines.add(tr("per_monster", be.chowCost(), String.format(Locale.ROOT, "%.1f", seconds)).withStyle(ChatFormatting.GRAY));
		}
		int monstrous = be.getMonstrousLevel();
		if (monstrous > 0)
			lines.add(tr("monstrous", Component.translatable("enchantment.level." + monstrous)).withStyle(ChatFormatting.LIGHT_PURPLE));
		return lines;
	}

	private List<Component> secondTooltip(MonsterCageBlockEntity be) {
		List<Component> lines = new ArrayList<>();
		if (be.isFlooded()) {
			lines.add(tr("water"));
			lines.add(Component.literal(String.format(Locale.ROOT, "%,d / %,d mB", be.getWater().getFluidAmount(),
				be.getWater().getCapacity())).withStyle(ChatFormatting.GRAY));
		} else if (be.isNetherite()) {
			lines.add(tr(be.isFieldUp() ? "field_up" : "field_down"));
			lines.add(Component.literal(String.format(Locale.ROOT, "%,d / %,d FE", be.getEnergyStored(), be.getEnergyCapacity()))
				.withStyle(ChatFormatting.GRAY));
		} else {
			lines.add(tr("no_second_gauge").withStyle(ChatFormatting.GRAY));
		}
		return lines;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.monster_cage." + key, args);
	}
}
