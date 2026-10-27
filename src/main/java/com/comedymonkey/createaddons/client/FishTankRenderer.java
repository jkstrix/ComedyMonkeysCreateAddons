package com.comedymonkey.createaddons.client;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.tank.FishTankBlock;
import com.comedymonkey.createaddons.content.tank.FishTankBlockEntity;
import com.comedymonkey.createaddons.content.tank.TankCreature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Draws a Fish Tank's insides (design/briefs/fish-tank.md): the water to its level, the creatures swimming in it, the
 * air pump's piston bobbing with the shaft, Fish Food sinking under the feeder, catches waiting in the net, Create's own
 * cogwheel half sunk in the gravel, and a soft glow on the back wall for glow squid. The tank is glass on three sides, so
 * the inside shows unless the camera is behind all three. The solid parts go to Flywheel when it's on; the water and the
 * glow stay here, since drawn by Flywheel they'd hide the fish behind them. The water goes in the sorted translucent
 * sheet the items use (the plain translucent type writes depth before the items are drawn, hiding them), and the glow is
 * additive, as in the preview. Modelled facing north; positions in model pixels.
 */
public class FishTankRenderer extends SafeBlockEntityRenderer<FishTankBlockEntity> {
	public static final ModelResourceLocation PISTON = part("block/fish_tank_pump");
	public static final ModelResourceLocation WATER = part("block/fish_tank_water");
	public static final ModelResourceLocation GLOW = part("block/fish_tank_glow");
	/** Create's own small cogwheel (Flywheel registers its partials as standalone models). */
	private static final ModelResourceLocation COG = ModelResourceLocation.standalone(AllPartialModels.SHAFTLESS_COGWHEEL.modelLocation());
	private static final float FLOOR_Y = 2.6f, NET_Y = 2.95f, MAX_SCALE = 0.26f, MAX_SIZE = 0.4f;
	/** The cogwheel: where it lies, how big, and how it's turned (degrees about X, Y, Z). */
	private static final float COG_X = 5.2f, COG_Y = 2.9f, COG_Z = 7.2f, COG_SCALE = 0.33f;
	private static final float[] COG_ROT = {-28, 20, -12};
	private static final float[][] FOOD = {{3.4f, 3.6f}, {4.2f, 4.4f}, {5.0f, 3.2f}};

	public FishTankRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation part(String path) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource(path));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation model : List.of(PISTON, WATER, GLOW))
			event.register(model);
	}

	@Override
	public AABB getRenderBoundingBox(FishTankBlockEntity be) {
		// the block's own box, as by default, but kept on the tank rather than made every frame
		return be.getRenderBox();
	}

	@Override
	protected void renderSafe(FishTankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(FishTankBlock.FACING))
			return;
		Direction front = state.getValue(FishTankBlock.FACING);
		CameraView camera = CameraView.of(be, ms);
		boolean hidden = camera.behindGlass(front) && camera.behindGlass(front.getClockWise())
			&& camera.behindGlass(front.getCounterClockWise());
		// with Flywheel on, its visual has the parts: no need to pose them all for nothing
		if (!PartSink.instanced(be))
			parts(be, partialTicks, ms, PartSink.screen(buffer, state, overlay), light);
		PartSink sink = PartSink.screen(buffer, state, overlay);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		if (state.getValue(FishTankBlock.GLOWING))
			sink.part(GLOW, ms, LightTexture.FULL_BRIGHT, RenderType.eyes(TextureAtlas.LOCATION_BLOCKS));
		if (!hidden) {
			renderFood(be, partialTicks, ms, buffer, light, overlay, camera);
			renderCreatures(be, partialTicks, ms, buffer, light, camera);
			TrayItems.render(be.getTray(), be.getTrayUsed(), be.getOutput().getSlots(), camera.items(3), NET_Y, be.getLevel(), ms,
				buffer, light, overlay);
		}
		// the water last, over everything inside
		if (be.getWater().getFluidAmount() > 0) {
			ms.pushPose();
			ms.translate(0, FLOOR_Y / 16, 0);
			ms.scale(1, be.waterLevel() - FLOOR_Y, 1);
			sink.part(WATER, ms, light, Sheets.translucentCullBlockSheet());
			ms.popPose();
		}
		ms.popPose();
	}

	/**
	 * The part models (for this renderer, or Flywheel's visual): the shaft end, the pump's piston riding up and down
	 * with the shaft, and the cogwheel, a third of its size, tipped into the gravel.
	 */
	public static void parts(FishTankBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(FishTankBlock.FACING))
			return;
		Direction front = state.getValue(FishTankBlock.FACING);
		CobbleGeneratorRenderer.shaft(be, front.getOpposite(), ms, sink, light);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), front.getAxis());
		ms.pushPose();
		ms.translate(0, (0.75f - 0.75f * Mth.cos(angle)) / 16, 0);
		sink.part(PISTON, ms, light);
		ms.popPose();
		ms.translate(COG_X / 16, COG_Y / 16, COG_Z / 16);
		// in Blockbench's order (Z, then Y, then X), as the approved preview tips it
		ms.mulPose(Axis.ZP.rotationDegrees(COG_ROT[2]));
		ms.mulPose(Axis.YP.rotationDegrees(COG_ROT[1]));
		ms.mulPose(Axis.XP.rotationDegrees(COG_ROT[0]));
		ms.scale(COG_SCALE, COG_SCALE, COG_SCALE);
		ms.translate(-0.5, -0.5, -0.5);
		sink.part(COG, ms, light);
		ms.popPose();
	}

	/** Fish Food sinking under the feeder, starting again every few seconds while there's food. */
	private static void renderFood(FishTankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
								   int overlay, CameraView camera) {
		ItemStack food = be.getShownFood();
		var level = be.getLevel();
		int shown = camera.items(FOOD.length);
		if (food.isEmpty() || level == null || shown <= 0)
			return;
		float surface = be.waterLevel();
		float t = ((level.getGameTime() % 80) + partialTicks) / 80f;
		var items = Minecraft.getInstance().getItemRenderer();
		for (int i = 0; i < shown; i++) {
			float y = surface - 0.6f - (t * 4 + i * 0.3f);
			if (y < 4)
				continue;
			ms.pushPose();
			ms.translate(FOOD[i][0] / 16, y / 16, FOOD[i][1] / 16);
			ms.mulPose(Axis.XP.rotationDegrees(90));
			ms.mulPose(Axis.ZP.rotationDegrees(i * 70 + t * 90));
			ms.scale(0.1f, 0.1f, 0.1f);
			items.renderStatic(food, ItemDisplayContext.FIXED, light, overlay, ms, buffer, level, i);
			ms.popPose();
		}
	}

	private static void renderCreatures(FishTankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
										int light, CameraView camera) {
		List<TankCreature> shown = be.getShown();
		if (shown.isEmpty())
			return;
		// fewer further off, none past 32 blocks or in a shadow pass, within the frame's budget
		int drawn = camera.creatures(shown.size());
		if (drawn <= 0)
			return;
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		dispatcher.setRenderShadow(false);
		try {
			for (int i = 0; i < drawn; i++) {
				TankCreature creature = shown.get(i);
				Entity entity = creature.entity();
				float size = entity instanceof Squid ? TankCreature.SQUID_HEIGHT : Math.max(entity.getBbHeight(), entity.getBbWidth());
				float scale = Math.min(MAX_SCALE, MAX_SIZE / size);
				// glow squid glow (their renderer's own light is only asked for when it's left to it)
				int lit = entity instanceof GlowSquid ? LightTexture.pack(15, LightTexture.sky(light)) : light;
				ms.pushPose();
				ms.translate(creature.x(partialTicks) / 16, creature.y(partialTicks) / 16, creature.z(partialTicks) / 16);
				ms.scale(scale, scale, scale);
				dispatcher.render(entity, 0, 0, 0, creature.yaw(partialTicks), partialTicks, ms, buffer, lit);
				ms.popPose();
			}
		} finally {
			dispatcher.setRenderShadow(true);
		}
	}
}
