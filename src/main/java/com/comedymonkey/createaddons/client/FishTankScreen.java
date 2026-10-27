package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.tank.FishTankBlockEntity;
import com.comedymonkey.createaddons.content.tank.FishTankMenu;
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
 * Wrench screen of a Fish Tank, on the Animal Pens' artwork (as the Monster Cages): the Capture Bottle and the Fish Food
 * on the left, a breeding gauge and the water beside them, what's in the net on the right, and the school and what
 * it's doing underneath.
 */
public class FishTankScreen extends AbstractContainerScreen<FishTankMenu> {
	private static final ResourceLocation ANDESITE = ComedyMonkeysCreateAddons.asResource("textures/gui/animal_pen.png");
	private static final ResourceLocation BRASS = ComedyMonkeysCreateAddons.asResource("textures/gui/brass_animal_pen.png");
	private static final int BREED_X = 30, WATER_X = 44, GAUGE_Y = 18, GAUGE_W = 10, GAUGE_H = 54;
	private static final int SCHOOL_Y = 102, STATUS_Y = 112;
	private static final int BREED = 0xFF7A8C4A, BREED_TOP = 0xFF93A85A;
	private static final int WATER = 0xFF2F6A80, WATER_TOP = 0xFF3F8AA6;

	public FishTankScreen(FishTankMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = FishTankMenu.WIDTH;
		imageHeight = MenuLayout.screenHeight(FishTankMenu.HEIGHT);
		inventoryLabelX = GuiKit.inventoryLabelX(FishTankMenu.WIDTH);
		inventoryLabelY = GuiKit.inventoryLabelY(FishTankMenu.HEIGHT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		FishTankBlockEntity be = menu.getBlockEntity();
		if (isHovering(BREED_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY))
			graphics.renderComponentTooltip(font, breedTooltip(be), mouseX, mouseY);
		else if (isHovering(WATER_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY))
			graphics.renderComponentTooltip(font, List.of(tr("water"), Component.literal(String.format(Locale.ROOT, "%,d / %,d mB",
				be.getWater().getFluidAmount(), be.getWater().getCapacity())).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		FishTankBlockEntity be = menu.getBlockEntity();
		graphics.blit(be.isBrass() ? BRASS : ANDESITE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		gauge(graphics, BREED_X, be.isBreeding() ? be.getBreedProgress() : 0, BREED, BREED_TOP);
		gauge(graphics, WATER_X, be.getWater().getFluidAmount() / (float) be.getWater().getCapacity(), WATER, WATER_TOP);
		RenderSystem.enableBlend();
		if (be.getCaptive().isEmpty())
			ghost(graphics, new ItemStack(ModItems.CAPTURE_BOTTLE.get()), FishTankMenu.BOTTLE_X, FishTankMenu.BOTTLE_Y);
		if (be.getShownFood().isEmpty())
			ghost(graphics, new ItemStack(ModItems.FISH_FOOD.get()), FishTankMenu.FOOD_X, FishTankMenu.FOOD_Y);
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
		GuiKit.title(graphics, font, title, FishTankMenu.WIDTH, GuiKit.TITLE);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiKit.INVENTORY_TEXT, false);
		FishTankBlockEntity be = menu.getBlockEntity();
		if (be.hasCreature())
			graphics.drawString(font, be.getSchool(), 8, SCHOOL_Y, GuiKit.TEXT, false);
		graphics.drawString(font, be.getStatus(), 8, be.hasCreature() ? STATUS_Y : SCHOOL_Y,
			be.isBreeding() ? GuiKit.TEXT : GuiKit.BAD, false);
	}

	private List<Component> breedTooltip(FishTankBlockEntity be) {
		List<Component> lines = new ArrayList<>();
		lines.add(be.getStatus());
		if (be.hasCreature()) {
			lines.add(tr("food_hint").withStyle(ChatFormatting.GRAY));
			float seconds = be.secondsPerBreeding();
			if (seconds > 0)
				lines.add(tr("per_breeding", String.format(Locale.ROOT, "%.1f", seconds)).withStyle(ChatFormatting.GRAY));
		}
		int fruitful = be.getFruitfulLevel();
		if (fruitful > 0)
			lines.add(tr("fruitful", Component.translatable("enchantment.level." + fruitful)).withStyle(ChatFormatting.LIGHT_PURPLE));
		return lines;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.fish_tank." + key, args);
	}
}
