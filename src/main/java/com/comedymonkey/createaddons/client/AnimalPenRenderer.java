package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.content.pen.AnimalPenBlock;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlockEntity;
import com.comedymonkey.createaddons.content.pen.PenAnimal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the inside of an Animal Pen behind its glass: the captured animal's own model, small, a few of them walking
 * about on the straw (the newest a baby for a while after it's born), and the food lying in the trough. Also turns
 * the shaft end in the back. The pen is modelled with its front facing north; positions are in its model pixels.
 */
public class AnimalPenRenderer extends SafeBlockEntityRenderer<AnimalPenBlockEntity> {
	/** Top of the straw bedding. */
	private static final float FLOOR_Y = 2.5f;
	/** Largest an animal is drawn, and how tall the biggest may stand, in blocks. */
	private static final float MAX_SCALE = 0.26f, MAX_SIZE = 0.4f;
	/** Where food lies in the trough. */
	private static final float[] FOOD_X = {5.5f, 8, 10.5f};
	private static final float FOOD_Y = 3.1f, FOOD_Z = 12;

	public AnimalPenRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(AnimalPenBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(AnimalPenBlock.FACING))
			return;
		Direction front = state.getValue(AnimalPenBlock.FACING);
		CobbleGeneratorRenderer.renderShaft(be, state, front.getOpposite(), ms, buffer, light, overlay);

		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		renderFood(be, ms, buffer, light, overlay);
		renderAnimals(be, partialTicks, ms, buffer, light);
		ms.popPose();
	}

	private static void renderFood(AnimalPenBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		ItemStack food = be.getFood().getStackInSlot(0);
		if (food.isEmpty())
			return;
		// One, two or three lying in the trough, as it fills up
		int shown = food.getCount() >= 32 ? 3 : food.getCount() >= 8 ? 2 : 1;
		var items = Minecraft.getInstance().getItemRenderer();
		for (int i = 0; i < shown; i++) {
			ms.pushPose();
			ms.translate(FOOD_X[i] / 16, FOOD_Y / 16, FOOD_Z / 16);
			ms.mulPose(Axis.XP.rotationDegrees(90));
			ms.mulPose(Axis.ZP.rotationDegrees(i * 70 - 40));
			ms.scale(0.3f, 0.3f, 0.3f);
			items.renderStatic(food, ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), i);
			ms.popPose();
		}
	}

	private static void renderAnimals(AnimalPenBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
									  int light) {
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		dispatcher.setRenderShadow(false);
		try {
			for (PenAnimal animal : be.getShown()) {
				Entity entity = animal.entity();
				float scale = Math.min(MAX_SCALE, MAX_SIZE / Math.max(entity.getBbHeight(), entity.getBbWidth()));
				ms.pushPose();
				ms.translate(animal.x(partialTicks) / 16, FLOOR_Y / 16, animal.z(partialTicks) / 16);
				ms.scale(scale, scale, scale);
				dispatcher.render(entity, 0, 0, 0, animal.yaw(partialTicks), partialTicks, ms, buffer, light);
				ms.popPose();
			}
		} finally {
			dispatcher.setRenderShadow(true);
		}
	}
}
