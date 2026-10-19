package com.comedymonkey.createaddons.compat.jei;

import java.util.Arrays;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.crucible.CrucibleRecipe;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.content.processing.recipe.HeatCondition;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Dust (with its count) on the left, the result on the right, the Blaze Burner heat it needs underneath. */
public class CrucibleCategory implements IRecipeCategory<RecipeHolder<CrucibleRecipe>> {
	public static final RecipeType<RecipeHolder<CrucibleRecipe>> TYPE =
		RecipeType.createRecipeHolderType(ComedyMonkeysCreateAddons.asResource("pressure_crucible"));

	private static final int WIDTH = 140;
	private static final int HEIGHT = 50;

	private final IDrawable icon;
	private final IDrawableStatic slot;
	private final IDrawableStatic arrow;
	private final IDrawable burner;

	public CrucibleCategory(IGuiHelper gui) {
		icon = gui.createDrawableItemStack(new ItemStack(ModItems.PRESSURE_CRUCIBLE.get()));
		slot = gui.getSlotDrawable();
		arrow = gui.getRecipeArrow();
		burner = gui.createDrawableItemStack(new ItemStack(
			BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "blaze_burner"))));
	}

	@Override
	public RecipeType<RecipeHolder<CrucibleRecipe>> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("block." + ComedyMonkeysCreateAddons.MOD_ID + ".pressure_crucible");
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrucibleRecipe> holder, IFocusGroup focuses) {
		CrucibleRecipe recipe = holder.value();
		builder.addSlot(RecipeIngredientRole.INPUT, 21, 9)
			.addItemStacks(Arrays.stream(recipe.ingredient().getItems())
				.map(stack -> stack.copyWithCount(recipe.count()))
				.toList());
		builder.addSlot(RecipeIngredientRole.OUTPUT, 103, 9)
			.addItemStack(recipe.result());
	}

	@Override
	public void draw(RecipeHolder<CrucibleRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics,
					 double mouseX, double mouseY) {
		CrucibleRecipe recipe = holder.value();
		slot.draw(graphics, 20, 8);
		slot.draw(graphics, 102, 8);
		arrow.draw(graphics, 58, 9);
		icon.draw(graphics, 62, 0);

		HeatCondition heat = recipe.heat();
		if (heat != HeatCondition.NONE) {
			burner.draw(graphics, 8, 32);
			Component text = Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.heat." + heat.getSerializedName());
			graphics.drawString(Minecraft.getInstance().font, text, 28, 36, heat.getColor(), false);
		}
		// Speed hint: the press time at 256 RPM (16x the base speed)
		float seconds = recipe.processingTime() / 16f / 20f;
		Component time = Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.jei.crucible_time",
			String.format(java.util.Locale.ROOT, "%.1f", seconds));
		graphics.drawString(Minecraft.getInstance().font, time, WIDTH - Minecraft.getInstance().font.width(time) - 2, 36,
			0x808080, false);
	}
}
