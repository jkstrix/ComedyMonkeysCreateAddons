package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The front tray of the glass-fronted machine boxes (Auto Harvester, Animal Pens, String Tangler, Stone Infuser): a few of whatever is waiting to
 * be collected, leaning back so they show through the window. One item once there's anything, two from a third of
 * the slots in use, three from two thirds. Positions are in the block's model pixels, front facing north.
 */
final class TrayItems {
	private static final float[] X = {5, 8, 11};
	private static final float Z = 2.7f;
	/** About 3 px across for a flat item. */
	private static final float SCALE = 0.18f;
	/** Lying flat is 90; this stands them up to 30 degrees off the tray, facing the glass. */
	private static final float LEAN = 60;

	private TrayItems() {}

	/** {@code bedY} is the tray's floor, in pixels. Call with the pose already turned to the block's facing. */
	static void render(IItemHandler output, float bedY, Level level, PoseStack ms, MultiBufferSource buffer, int light,
					   int overlay) {
		List<ItemStack> waiting = new ArrayList<>(3);
		int used = 0;
		for (int i = 0; i < output.getSlots(); i++) {
			ItemStack stack = output.getStackInSlot(i);
			if (stack.isEmpty())
				continue;
			used++;
			if (waiting.size() < 3)
				waiting.add(stack);
		}
		if (used == 0)
			return;
		float full = used / (float) output.getSlots();
		draw(waiting, full >= 2 / 3f ? 3 : full >= 1 / 3f ? 2 : 1, bedY, level, ms, buffer, light, overlay);
	}

	/**
	 * The same from a summary, as the Monster Cages and Fish Tanks send their clients: the first few stacks waiting, and
	 * {@code used} of the {@code slots} in use. At most {@code max} items (fewer further off).
	 */
	static void render(List<ItemStack> waiting, int used, int slots, int max, float bedY, Level level, PoseStack ms,
					   MultiBufferSource buffer, int light, int overlay) {
		if (used == 0 || waiting.isEmpty() || max <= 0)
			return;
		float full = used / (float) Math.max(1, slots);
		draw(waiting, Math.min(max, full >= 2 / 3f ? 3 : full >= 1 / 3f ? 2 : 1), bedY, level, ms, buffer, light, overlay);
	}

	/**
	 * The same tray for a machine with a single output slot (the String Tangler, the Stone Infuser): one item once
	 * there's anything, two from a third of a stack, three from two thirds.
	 */
	static void render(ItemStack output, float bedY, Level level, PoseStack ms, MultiBufferSource buffer, int light,
					   int overlay) {
		if (output.isEmpty())
			return;
		float full = output.getCount() / (float) output.getMaxStackSize();
		draw(List.of(output), full >= 2 / 3f ? 3 : full >= 1 / 3f ? 2 : 1, bedY, level, ms, buffer, light, overlay);
	}

	private static void draw(List<ItemStack> waiting, int shown, float bedY, Level level, PoseStack ms,
							 MultiBufferSource buffer, int light, int overlay) {
		var items = Minecraft.getInstance().getItemRenderer();
		for (int i = 0; i < shown; i++) {
			// Fewer different stacks than items to show: the last one again
			ItemStack stack = waiting.get(Math.min(i, waiting.size() - 1));
			ms.pushPose();
			ms.translate(X[i] / 16, (bedY + 0.75f) / 16, Z / 16);
			ms.mulPose(Axis.XP.rotationDegrees(LEAN));
			ms.mulPose(Axis.ZP.rotationDegrees(i * 25 - 25));
			ms.scale(SCALE, SCALE, SCALE);
			items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, level, i);
			ms.popPose();
		}
	}
}
