package com.comedymonkey.createaddons.compat.jei;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.machine.MachineKind;
import com.comedymonkey.createaddons.content.machine.TwoInputRecipe;
import com.comedymonkey.createaddons.registry.ModItems;

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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Both ingredients (with their counts) on the left, the machine in the middle, the result on the right. */
public class TwoInputCategory implements IRecipeCategory<RecipeHolder<TwoInputRecipe>> {
	private static final Map<MachineKind, RecipeType<RecipeHolder<TwoInputRecipe>>> TYPES = new EnumMap<>(MachineKind.class);
	private static final int WIDTH = 140;
	private static final int HEIGHT = 44;

	private final MachineKind kind;
	private final IDrawable icon;
	private final IDrawableStatic slot;
	private final IDrawableStatic arrow;

	public TwoInputCategory(IGuiHelper gui, MachineKind kind) {
		this.kind = kind;
		icon = gui.createDrawableItemStack(new ItemStack(machine(kind)));
		slot = gui.getSlotDrawable();
		arrow = gui.getRecipeArrow();
	}

	public static RecipeType<RecipeHolder<TwoInputRecipe>> type(MachineKind kind) {
		return TYPES.computeIfAbsent(kind, k -> RecipeType.createRecipeHolderType(ComedyMonkeysCreateAddons.asResource(k.id())));
	}

	private static net.minecraft.world.item.Item machine(MachineKind kind) {
		return kind == MachineKind.TANGLING ? ModItems.STRING_TANGLER.get() : ModItems.STONE_INFUSER.get();
	}

	@Override
	public RecipeType<RecipeHolder<TwoInputRecipe>> getRecipeType() {
		return type(kind);
	}

	@Override
	public Component getTitle() {
		return Component.translatable("block." + ComedyMonkeysCreateAddons.MOD_ID + "."
			+ (kind == MachineKind.TANGLING ? "string_tangler" : "stone_infuser"));
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

	private static java.util.List<ItemStack> stacks(Ingredient ingredient, int count) {
		return Arrays.stream(ingredient.getItems()).map(s -> s.copyWithCount(count)).toList();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<TwoInputRecipe> holder, IFocusGroup focuses) {
		TwoInputRecipe recipe = holder.value();
		builder.addSlot(RecipeIngredientRole.INPUT, 9, 9).addItemStacks(stacks(recipe.ingredient(), recipe.count()));
		builder.addSlot(RecipeIngredientRole.INPUT, 31, 9).addItemStacks(stacks(recipe.extra(), recipe.extraCount()));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 9).addItemStack(recipe.result());
	}

	@Override
	public void draw(RecipeHolder<TwoInputRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics,
					 double mouseX, double mouseY) {
		slot.draw(graphics, 8, 8);
		slot.draw(graphics, 30, 8);
		slot.draw(graphics, 112, 8);
		icon.draw(graphics, 62, 0);
		arrow.draw(graphics, 80, 9);
		// How long one takes at 256 RPM (16x the base speed)
		float seconds = holder.value().processingTime() / 16f / 20f;
		Component time = Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.jei.machine_time",
			String.format(Locale.ROOT, "%.1f", seconds));
		graphics.drawString(Minecraft.getInstance().font, time, WIDTH - Minecraft.getInstance().font.width(time) - 2, 32,
			0x808080, false);
	}
}
