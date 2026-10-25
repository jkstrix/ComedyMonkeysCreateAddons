package com.comedymonkey.createaddons.client;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlock;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlockEntity;
import com.comedymonkey.createaddons.content.pen.PenAnimal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Draws the inside of an Animal Pen behind its glass: the captured animal's own model, small, a few of them walking
 * about on the straw (the newest a baby for a while after it's born), the food lying in the trough, and what's
 * waiting to be collected in the tray at the front. Also the milk in the sight glass on each side, and turns the
 * shaft end in the back. The pen is modelled with its front facing north; positions are in its model pixels.
 */
public class AnimalPenRenderer extends SafeBlockEntityRenderer<AnimalPenBlockEntity> {
	/** The milk column: a 1 px tall unit in both side sight glasses, lifted to the glass foot and stretched to the level. */
	public static final ModelResourceLocation MILK =
		ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/animal_pen_milk"));
	/** The sight glass runs from here up, this many pixels. */
	private static final float MILK_Y = 3, MILK_HEIGHT = 10;
	/** Floor of the tray in front, which sits on the straw. */
	private static final float TRAY_Y = 2.6f;
	/** Top of the straw bedding. */
	private static final float FLOOR_Y = 2.5f;
	/** Largest an animal is drawn, and how tall the biggest may stand, in blocks. */
	private static final float MAX_SCALE = 0.26f, MAX_SIZE = 0.4f;
	/** Where food lies in the trough. */
	private static final float[] FOOD_X = {5.5f, 8, 10.5f};
	private static final float FOOD_Y = 3.1f, FOOD_Z = 12;
	/**
	 * How many of the animals are drawn by distance, as they get too small to count: past 16 blocks two, past 32 none.
	 * Coming back they return a block closer in (15 and 31), so standing at the edge doesn't flick them on and off.
	 */
	private static final double FEWER_OUT = 16 * 16, FEWER_IN = 15 * 15, NONE_OUT = 32 * 32, NONE_IN = 31 * 31;
	private static final int ALL = 0, FEWER = 1, NONE = 2;
	/** How many each pen drew last frame (render thread only); weak, so pens that go are forgotten. */
	private final Map<AnimalPenBlockEntity, Integer> detail = new WeakHashMap<>();

	public AnimalPenRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(AnimalPenBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(AnimalPenBlock.FACING))
			return;
		Direction front = state.getValue(AnimalPenBlock.FACING);
		CameraView camera = CameraView.of(be, ms);
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);

		// The inside only shows through the front glass
		if (!camera.behindGlass(front)) {
			ms.pushPose();
			ms.translate(0.5, 0.5, 0.5);
			ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
			ms.translate(-0.5, -0.5, -0.5);
			renderFood(be, ms, buffer, light, overlay);
			renderAnimals(be, partialTicks, ms, buffer, light, detail(be, camera));
			TrayItems.render(be.getOutput(), TRAY_Y, be.getLevel(), ms, buffer, light, overlay);
			ms.popPose();
		}
	}

	/**
	 * The part models: the shaft end and the milk in the side gauges (for this renderer, or Flywheel's visual). The milk
	 * is in the side gauges, so it's always drawn.
	 */
	public static void parts(AnimalPenBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(AnimalPenBlock.FACING))
			return;
		Direction front = state.getValue(AnimalPenBlock.FACING);
		CobbleGeneratorRenderer.shaft(be, front.getOpposite(), ms, sink, light);
		FluidTank milk = be.getMilk();
		if (milk.isEmpty())
			return;
		float level = Math.min(1, milk.getFluidAmount() / (float) milk.getCapacity());
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		ms.translate(0, MILK_Y / 16, 0);
		ms.scale(1, MILK_HEIGHT * level, 1);
		sink.part(MILK, ms, light);
		ms.popPose();
	}

	private static void renderFood(AnimalPenBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		ItemStack food = be.getTroughFood();
		if (food.isEmpty())
			return;
		// One, two or three lying in the trough, as it fills up
		int count = be.getFoodCount();
		int shown = count >= 32 ? 3 : count >= 8 ? 2 : 1;
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

	/**
	 * How many of this pen's animals to draw from where the camera is, going on from what it drew last time. All of
	 * them where the camera isn't known (Ponder, a shadow pass), which doesn't count as last time.
	 */
	private int detail(AnimalPenBlockEntity be, CameraView camera) {
		if (!camera.known())
			return ALL;
		double distanceSqr = camera.distanceSqr();
		Integer last = detail.get(be);
		int was = last == null ? ALL : last;
		int now = distanceSqr > NONE_OUT ? NONE : distanceSqr > FEWER_OUT ? FEWER : ALL;
		// Moving closer, it only steps back up once it's well inside the nearer band
		if (now < was) {
			if (was == NONE && distanceSqr > NONE_IN)
				now = NONE;
			else if (now == ALL && distanceSqr > FEWER_IN)
				now = FEWER;
		}
		if (last == null || now != was)
			detail.put(be, now);
		return now;
	}

	/** The animals; further off, just the first and the last (the youngest, if there's a baby), then none. */
	private static void renderAnimals(AnimalPenBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
									  int light, int detail) {
		List<PenAnimal> shown = be.getShown();
		if (shown.isEmpty() || detail == NONE)
			return;
		boolean fewer = detail == FEWER;
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		dispatcher.setRenderShadow(false);
		try {
			for (int i = 0; i < shown.size(); i++) {
				if (fewer && i != 0 && i != shown.size() - 1)
					continue;
				PenAnimal animal = shown.get(i);
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
