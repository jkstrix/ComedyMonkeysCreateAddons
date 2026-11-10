package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.dripstone.PointedDripstoneGeneratorBlock;
import com.comedymonkey.createaddons.content.dripstone.PointedDripstoneGeneratorBlockEntity;
import com.comedymonkey.createaddons.content.dripstone.PointedDripstoneGeneratorBlockEntity.Phase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.redstone.link.LinkRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the little dripstone cave inside a Pointed Dripstone Generator, behind its glass front
 * (design/briefs/pointed-dripstone-generator.md): water standing on the dripstone ceiling (as high as the tank is full),
 * the four seed stalactites (vanilla's own Pointed Dripstone, scaled), the tip growing on the one whose turn it is, and
 * the trip hammer: its spindle ticks a quarter turn to each stalactite in turn, and at the end of each cycle it winds up,
 * holds, whacks the grown tip, which shudders, drops tumbling and slides into the front tray, then recoils and settles.
 * The lamp on the front frame says why it's stopped: cyan running, amber full, dark red switched off, missing a seed or
 * out of water, unlit with no rotation. Timings are the approved preview's (pointed-dripstone-generator.gen.js).
 */
public class PointedDripstoneGeneratorRenderer extends SafeBlockEntityRenderer<PointedDripstoneGeneratorBlockEntity> {
	private static final ModelResourceLocation SPINDLE = part("pointed_dripstone_generator_spindle");
	private static final ModelResourceLocation ARM = part("pointed_dripstone_generator_arm");
	/** The Cobblestone Generators' lit lenses: the lamp sits in the same place on the same frame. */
	private static final ModelResourceLocation LAMP_RUNNING = part("cobble_generator_lamp_running");
	private static final ModelResourceLocation LAMP_FULL = part("cobble_generator_lamp_full");
	private static final ModelResourceLocation LAMP_OFF = part("cobble_generator_lamp_off");

	/** Model pixels, window north: the spindle's axis, the arm's pivot height, the water, the stalactites. */
	private static final float SPIN_X = 8, SPIN_Z = 7.5f, ARM_Y = 5.95f;
	private static final float WATER_BOTTOM = 12, WATER_TOP = 13.75f, WATER_MIN = 0.25f;
	private static final int SEEDS = PointedDripstoneGeneratorBlockEntity.SEEDS;
	private static final float SEED_TOP = 10.5f, SCALE = 0.2f;
	/** The tray's floor (the chamber floor), and its three places across the front (TrayItems). */
	private static final float TRAY_Y = 2, TRAY_Z = 3.6f;
	private static final float[] TRAY_X = {5, 8, 11};
	/** The hammer's angle from its stalactite (degrees): resting beside it, wound up, touching it, recoiled. */
	private static final float REST = 30, WOUND = 60, CONTACT = 18, RECOIL = 36;

	private static final BlockSprite WATER_STILL = new BlockSprite(ResourceLocation.withDefaultNamespace("block/water_still"));
	/** Plains water colour; the water sprite is grey and tinted in-world. */
	private static final int WATER_COLOUR = 0x3F76E4;

	public PointedDripstoneGeneratorRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	/** The spindle and the arm aren't referenced by any blockstate, so they're loaded explicitly (the lamps are the Cobblestone Generators'). */
	public static void registerModels(ModelEvent.RegisterAdditional event) {
		event.register(SPINDLE);
		event.register(ARM);
	}

	@Override
	protected void renderSafe(PointedDripstoneGeneratorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(PointedDripstoneGeneratorBlock.FACING))
			return;
		Direction front = state.getValue(PointedDripstoneGeneratorBlock.FACING);
		boolean behind = CameraView.of(be, ms).behindGlass(front);
		// The shaft, hammer and lamp are part models (Flywheel draws them when it can); the hammer is inside, seen only
		// through the front glass, the lamp stands proud of the front
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light, !behind);
		if (!behind)
			renderCave(be, front, partialTicks, ms, buffer, light, overlay);
		if (be.hasAntenna())
			LinkRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
	}

	/** The part models: the shaft end, the hammer's spindle and arm, and the lamp (for the renderer, or Flywheel's visual). */
	public static void parts(PointedDripstoneGeneratorBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		parts(be, partialTicks, ms, sink, light, true);
	}

	private static void parts(PointedDripstoneGeneratorBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light,
							  boolean hammer) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(PointedDripstoneGeneratorBlock.FACING))
			return;
		Direction front = state.getValue(PointedDripstoneGeneratorBlock.FACING);
		CobbleGeneratorRenderer.shaft(be, front.getOpposite(), ms, sink, light);
		ms.pushPose();
		turnToFront(ms, front);
		if (hammer)
			hammer(be, partialTicks, ms, sink, light);
		// in the status line's order: switched off, full, needs seed or water, no rotation (unlit)
		ModelResourceLocation lamp = be.isRunning() ? LAMP_RUNNING
			: be.isPowered() ? LAMP_OFF
			: be.isFull() ? LAMP_FULL
			: !be.hasSeed() || be.needsWater() ? LAMP_OFF : null;
		if (lamp != null)
			sink.part(lamp, ms, LightTexture.FULL_BRIGHT);
		ms.popPose();
	}

	/** The block is modelled with its window north; turn to the real front. */
	private static void turnToFront(PoseStack ms, Direction front) {
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
	}

	// ---------------------------------------------------------------- the hammer

	/** The spindle's angle that points the arm (built along +x) at stalactite {@code turn}. */
	private static float aim(int turn) {
		return 45 - 90 * turn;
	}

	private static void hammer(PointedDripstoneGeneratorBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		int turn = be.getTurn();
		float k = be.getPhaseTicks(partialTicks);
		float yaw, tilt = 0;
		switch (be.getPhase()) {
			// to the next stalactite; the first it's already at (it went round to it while the tips were still tiny)
			case INDEX -> yaw = turn == 0 ? aim(0) + REST
				: Mth.lerp(backOut(Math.min(1, k / PointedDripstoneGeneratorBlockEntity.INDEX)), aim(turn - 1), aim(turn)) + REST;
			case KNOCK -> {
				yaw = aim(turn) + knockOffset(k);
				tilt = knockTilt(k);
			}
			// the tips growing: straight after the last knock it ticks round to the first stalactite, before the tips
			// are big enough to be in its way (sweeping past the fourth later would cut through its new tip)
			default -> {
				float grown = be.getGrowth(partialTicks) * be.getGrowTicks();
				yaw = Mth.lerp(backOut(Math.min(1, grown / PointedDripstoneGeneratorBlockEntity.INDEX)), aim(SEEDS - 1), aim(SEEDS)) + REST;
			}
		}
		ms.pushPose();
		ms.translate(SPIN_X / 16, 0, SPIN_Z / 16);
		ms.mulPose(Axis.YP.rotationDegrees(yaw));
		ms.translate(-SPIN_X / 16, 0, -SPIN_Z / 16);
		sink.part(SPINDLE, ms, light);
		ms.translate(SPIN_X / 16, ARM_Y / 16, SPIN_Z / 16);
		ms.mulPose(Axis.ZP.rotationDegrees(tilt));
		ms.translate(-SPIN_X / 16, -ARM_Y / 16, -SPIN_Z / 16);
		sink.part(ARM, ms, light);
		ms.popPose();
	}

	/** Wind-up 10 ticks, hold 3, whack 2 (the hit at 15), recoil 6, settle 6. */
	private static float knockOffset(float k) {
		if (k < 10)
			return Mth.lerp(easeOut(k / 10), REST, WOUND);
		if (k < 13)
			return WOUND;
		if (k < 15)
			return Mth.lerp((k - 13) / 2, WOUND, CONTACT);
		if (k < 21)
			return Mth.lerp(easeOut((k - 15) / 6), CONTACT, RECOIL);
		if (k < 27)
			return Mth.lerp(smooth((k - 21) / 6), RECOIL, REST);
		return REST;
	}

	/** The arm lifts the head a little as it winds up, and chops down into the hit. */
	private static float knockTilt(float k) {
		if (k < 10)
			return Mth.lerp(easeOut(k / 10), 0, 12);
		if (k < 13)
			return 12;
		if (k < 15)
			return Mth.lerp((k - 13) / 2, 12, -4);
		if (k < 21)
			return Mth.lerp(smooth((k - 15) / 6), -4, 0);
		return 0;
	}

	private static float easeOut(float u) {
		return 1 - (1 - u) * (1 - u);
	}

	private static float smooth(float u) {
		u = Mth.clamp(u, 0, 1);
		return u * u * (3 - 2 * u);
	}

	/** A small overshoot past the target, then back. */
	private static float backOut(float u) {
		float c1 = 1.2f, c3 = c1 + 1;
		return 1 + c3 * (u - 1) * (u - 1) * (u - 1) + c1 * (u - 1) * (u - 1);
	}

	// ---------------------------------------------------------------- the cave

	private static void renderCave(PointedDripstoneGeneratorBlockEntity be, Direction front, float partialTicks, PoseStack ms,
								   MultiBufferSource buffer, int light, int overlay) {
		ms.pushPose();
		turnToFront(ms, front);
		// water standing on the ceiling, as high as the tank is full
		float fill = be.getWater().getFluidAmount() / (float) Math.max(1, be.getWater().getCapacity());
		// never thinner than a quarter pixel: a sliver on the ceiling would z-fight with it
		if (fill > 0.01f)
			water(buffer.getBuffer(RenderType.cutout()), ms.last(), light,
				WATER_BOTTOM + WATER_MIN + (WATER_TOP - WATER_BOTTOM - WATER_MIN) * Math.min(1, fill));

		var blocks = Minecraft.getInstance().getBlockRenderer();
		BlockState seed = Blocks.POINTED_DRIPSTONE.defaultBlockState()
			.setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN).setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.FRUSTUM);
		BlockState tip = seed.setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP);
		float[][] at = PointedDripstoneGeneratorBlockEntity.STALACTITES;
		int seeds = be.getSeedCount();
		for (int i = 0; i < at.length && i < seeds; i++) {
			ms.pushPose();
			ms.translate(at[i][0] / 16, SEED_TOP / 16, at[i][1] / 16);
			ms.scale(SCALE, SCALE, SCALE);
			ms.translate(-0.5, -1, -0.5);
			blocks.renderSingleBlock(seed, ms, buffer, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
			ms.popPose();
		}

		// the tips: all four growing together; then, as the hammer goes round, each whole until its knock, shuddering,
		// dropping and sliding to the tray after the hit, and gone once knocked
		for (int i = 0; i < at.length && i < seeds; i++)
			tip(be, i, at[i], partialTicks, ms, buffer, light, blocks, tip);

		// the piece counts at the strike, but it joins the tray once its tip has slid in (as the approved preview)
		ItemStack inTray = be.getOutputHandler().getStackInSlot(0);
		float k = be.getPhaseTicks(partialTicks);
		if (be.getPhase() == Phase.KNOCK && k >= PointedDripstoneGeneratorBlockEntity.HIT && k < 29 && !inTray.isEmpty())
			inTray = inTray.copyWithCount(inTray.getCount() - 1);
		TrayItems.render(inTray, TRAY_Y, be.getLevel(), ms, buffer, light, overlay);
		ms.popPose();
	}

	private static void tip(PointedDripstoneGeneratorBlockEntity be, int i, float[] s, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							int light, net.minecraft.client.renderer.block.BlockRenderDispatcher blocks, BlockState tip) {
		float top = PointedDripstoneGeneratorBlockEntity.TIP_TOP, centre = (top + PointedDripstoneGeneratorBlockEntity.TIP_BOTTOM) / 2;
		int turn = be.getTurn();
		// growing (all four), or whole and waiting for the hammer (those it hasn't reached, and the one it's turning to)
		if (be.getPhase() == Phase.GROW || i > turn || i == turn && be.getPhase() == Phase.INDEX) {
			float g = be.getGrowth(partialTicks);
			if (g <= 0.01f)
				return;
			ms.pushPose();
			ms.translate(s[0] / 16, top / 16, s[1] / 16);
			ms.scale(SCALE * g, SCALE * g, SCALE * g);
			ms.translate(-0.5, -1, -0.5);
			blocks.renderSingleBlock(tip, ms, buffer, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
			ms.popPose();
			return;
		}
		// already knocked off this round
		if (i < turn)
			return;
		float k = be.getPhaseTicks(partialTicks);
		if (k >= 29)
			return;
		float x = 0, y = 0, z = 0, rx = 0, rz = 0;
		if (k >= PointedDripstoneGeneratorBlockEntity.HIT) {
			if (k < 18)
				x = ((int) k) % 2 == 0 ? -0.35f : 0.35f;
			float f = Mth.clamp((k - 18) / 6, 0, 1);
			y = -3.1f * f * f + (k > 24 && k < 27 ? 0.4f * Mth.sin(Mth.PI * (k - 24) / 3) : 0);
			float g = smooth((k - 24) / 5);
			float slot = TRAY_X[0];
			for (float t : TRAY_X)
				if (Math.abs(t - s[0]) < Math.abs(slot - s[0]))
					slot = t;
			x += (slot - s[0]) * g;
			z = (TRAY_Z - s[1]) * g;
			rx = -80 * smooth(f);
			rz = 15 * f;
		}
		ms.pushPose();
		ms.translate((s[0] + x) / 16, (centre + y) / 16, (s[1] + z) / 16);
		ms.mulPose(Axis.ZP.rotationDegrees(rz));
		ms.mulPose(Axis.XP.rotationDegrees(rx));
		ms.scale(SCALE, SCALE, SCALE);
		ms.translate(-0.5, -0.5, -0.5);
		blocks.renderSingleBlock(tip, ms, buffer, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
		ms.popPose();
	}

	/** The water's top and front (the rest is against the walls and the ceiling), x 2-14, z 2-14. */
	private static void water(VertexConsumer vc, PoseStack.Pose pose, int light, float top) {
		TextureAtlasSprite s = WATER_STILL.get();
		float a = 2 / 16f, d = 14 / 16f, b = WATER_BOTTOM / 16, e = top / 16, c = 2 / 16f, f = 14 / 16f;
		// up
		quad(vc, pose, s, light, 0, 1, 0, a, e, c, 2, 2, a, e, f, 2, 14, d, e, f, 14, 14, d, e, c, 14, 2);
		// north (front)
		quad(vc, pose, s, light, 0, 0, -1, d, e, c, 2, 16 - top, d, b, c, 2, 16 - WATER_BOTTOM, a, b, c, 14, 16 - WATER_BOTTOM,
			a, e, c, 14, 16 - top);
	}

	/** Four corners (x, y, z in blocks; u, v in sprite pixels 0-16), counter-clockwise seen from outside. */
	private static void quad(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int light, float nx, float ny, float nz,
							 float... v) {
		for (int i = 0; i < 4; i++) {
			int o = i * 5;
			vc.addVertex(pose, v[o], v[o + 1], v[o + 2])
				.setColor((WATER_COLOUR >> 16) & 0xFF, (WATER_COLOUR >> 8) & 0xFF, WATER_COLOUR & 0xFF, 255)
				.setUv(s.getU(v[o + 3] / 16f), s.getV(v[o + 4] / 16f))
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
		}
	}
}
