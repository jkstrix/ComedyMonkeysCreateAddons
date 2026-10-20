package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlockEntity;
import com.comedymonkey.createaddons.content.pen.AnimalPenMenu;
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
 * Wrench screen of an Animal Pen: the animal and its food on the left, a breeding gauge and a milk gauge, what it
 * has made on the right, and how many it has and what it's doing underneath.
 */
public class AnimalPenScreen extends AbstractContainerScreen<AnimalPenMenu> {
	private static final ResourceLocation ANDESITE = ComedyMonkeysCreateAddons.asResource("textures/gui/animal_pen.png");
	private static final ResourceLocation BRASS = ComedyMonkeysCreateAddons.asResource("textures/gui/brass_animal_pen.png");

	// Gauge interiors, relative to the GUI origin (must match the textures)
	private static final int BREED_X = 30, MILK_X = 44, GAUGE_Y = 18, GAUGE_W = 10, GAUGE_H = 54;
	private static final int HERD_Y = 102, STATUS_Y = 112;
	private static final int BREED = 0xFFB0745A, BREED_TOP = 0xFFC88A6C;
	private static final int MILK = 0xFFDAD8CE, MILK_TOP = 0xFFF0EEE6;

	public AnimalPenScreen(AnimalPenMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = AnimalPenMenu.WIDTH;
		imageHeight = MenuLayout.screenHeight(AnimalPenMenu.HEIGHT);
		inventoryLabelX = GuiKit.inventoryLabelX(AnimalPenMenu.WIDTH);
		inventoryLabelY = GuiKit.inventoryLabelY(AnimalPenMenu.HEIGHT);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
		AnimalPenBlockEntity be = menu.getBlockEntity();
		if (isHovering(BREED_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY))
			graphics.renderComponentTooltip(font, breedTooltip(be), mouseX, mouseY);
		else if (isHovering(MILK_X, GAUGE_Y, GAUGE_W, GAUGE_H, mouseX, mouseY))
			graphics.renderComponentTooltip(font, milkTooltip(be), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		AnimalPenBlockEntity be = menu.getBlockEntity();
		graphics.blit(be.isBrass() ? BRASS : ANDESITE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
		gauge(graphics, BREED_X, be.isBreeding() ? be.getBreedProgress() : 0, BREED, BREED_TOP);
		if (be.isMilkable())
			gauge(graphics, MILK_X, be.getMilk().getFluidAmount() / (float) be.getMilk().getCapacity(), MILK, MILK_TOP);

		// Faint hints in the empty slots: a Capture Bottle for the animal, and that animal's own food
		RenderSystem.enableBlend();
		if (be.getCaptive().isEmpty())
			ghost(graphics, new ItemStack(ModItems.CAPTURE_BOTTLE.get()), AnimalPenMenu.ANIMAL_X, AnimalPenMenu.ANIMAL_Y);
		ItemStack food = be.getFoodHint();
		if (be.getTroughFood().isEmpty() && !food.isEmpty())
			ghost(graphics, food, AnimalPenMenu.TROUGH_X, AnimalPenMenu.TROUGH_Y);
		RenderSystem.disableBlend();
	}

	private void gauge(GuiGraphics graphics, int gx, float fill, int colour, int top) {
		int height = Math.round(GAUGE_H * fill);
		if (height <= 0)
			return;
		int x = leftPos + gx, bottom = topPos + GAUGE_Y + GAUGE_H;
		graphics.fill(x, bottom - height, x + GAUGE_W, bottom, colour);
		graphics.fill(x, bottom - height, x + GAUGE_W, bottom - height + 1, top);
	}

	private void ghost(GuiGraphics graphics, ItemStack stack, int x, int y) {
		graphics.renderFakeItem(stack, leftPos + x, topPos + y);
		// Dimmed by the slot's own colour drawn over it
		graphics.fill(RenderType.guiOverlay(), leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xA062645F);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		GuiKit.title(graphics, font, title, AnimalPenMenu.WIDTH, GuiKit.TITLE);
		graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiKit.INVENTORY_TEXT, false);
		AnimalPenBlockEntity be = menu.getBlockEntity();
		if (be.hasAnimal())
			graphics.drawString(font, be.getHerd(), 8, HERD_Y, GuiKit.TEXT, false);
		graphics.drawString(font, be.getStatus(), 8, be.hasAnimal() ? STATUS_Y : HERD_Y,
			be.isBreeding() ? GuiKit.TEXT : GuiKit.BAD, false);
	}

	private List<Component> breedTooltip(AnimalPenBlockEntity be) {
		List<Component> lines = new ArrayList<>();
		lines.add(be.getStatus());
		if (be.hasAnimal())
			lines.add(tr("gui.animal_pen.food_hint").withStyle(ChatFormatting.GRAY));
		int grows = be.ticksToNextGrown();
		if (grows > 0)
			lines.add(tr("gui.animal_pen.grow_next", String.format(Locale.ROOT, "%d:%02d", grows / 1200, grows / 20 % 60))
				.withStyle(ChatFormatting.GRAY));
		int fruitful = be.getFruitfulLevel();
		if (fruitful > 0)
			lines.add(tr("gui.animal_pen.fruitful", Component.translatable("enchantment.level." + fruitful))
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		return lines;
	}

	private List<Component> milkTooltip(AnimalPenBlockEntity be) {
		List<Component> lines = new ArrayList<>();
		if (!be.isMilkable()) {
			lines.add(tr("gui.animal_pen.no_milk").withStyle(ChatFormatting.GRAY));
			return lines;
		}
		lines.add(tr("gui.animal_pen.milk"));
		lines.add(Component.literal(String.format(Locale.ROOT, "%,d / %,d mB", be.getMilk().getFluidAmount(),
			be.getMilk().getCapacity())).withStyle(ChatFormatting.GRAY));
		return lines;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + "." + key, args);
	}
}
