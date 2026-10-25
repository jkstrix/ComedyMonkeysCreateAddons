package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlock;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlockEntity;
import com.comedymonkey.createaddons.content.cobblegen.GeneratorProduct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.redstone.link.LinkRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws what goes on inside a Cobblestone Generator, behind its glass front, in 3D (design/briefs/cobble-generator.md):
 * pools of water and lava in the basin (Minecraft's own animated water and lava), and while it runs, water and lava
 * pouring from the nozzles and a block of the product rising between them in step with the drill-paced progress (a
 * batch of four on brass). The drill hangs over it, spinning with the shaft, riding the block's top and jabbing down
 * on every drill hit; stopped, it waits raised. The lamp on the front frame says why: cyan running, amber full, dark
 * red switched off, unlit with no rotation. The lava and the lamp are full-bright. Also turns the shaft end in the back.
 */
public class CobbleGeneratorRenderer extends SafeBlockEntityRenderer<CobbleGeneratorBlockEntity> {
	/** Short shaft stub, modelled going into the north face. */
	public static final ModelResourceLocation SHAFT =
		ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/cobble_generator_shaft"));
	private static final ModelResourceLocation DRILL = part("cobble_generator_drill");
	private static final ModelResourceLocation DRILL_BRASS = part("cobble_generator_drill_brass");
	private static final ModelResourceLocation DRILL_ROD = part("cobble_generator_drill_rod");
	private static final ModelResourceLocation LAMP_RUNNING = part("cobble_generator_lamp_running");
	private static final ModelResourceLocation LAMP_FULL = part("cobble_generator_lamp_full");
	private static final ModelResourceLocation LAMP_OFF = part("cobble_generator_lamp_off");
	/** The drill's spin axis (model pixels), the top of its collar above the tip, and the sleeve its rod runs up into. */
	private static final float DRILL_X = 8, DRILL_Z = 6.5f, COLLAR = 3.5f, COLLAR_BRASS = 3.75f, SLEEVE = 12.25f;
	/** The brass batch: four 2.25 px blocks in a 2 x 2 grid, by their north-west corners. */
	private static final float[][] BATCH = {{5.5f, 4.25f}, {8.25f, 4.25f}, {5.5f, 7}, {8.25f, 7}};

	private static final BlockSprite WATER_STILL = new BlockSprite(ResourceLocation.withDefaultNamespace("block/water_still"));
	private static final BlockSprite WATER_FLOW = new BlockSprite(ResourceLocation.withDefaultNamespace("block/water_flow"));
	private static final BlockSprite LAVA_STILL = new BlockSprite(ResourceLocation.withDefaultNamespace("block/lava_still"));
	private static final BlockSprite LAVA_FLOW = new BlockSprite(ResourceLocation.withDefaultNamespace("block/lava_flow"));
	private static final BlockSprite BLUE_ICE = new BlockSprite(ResourceLocation.withDefaultNamespace("block/blue_ice"));
	/** Plains water colour; the water sprites are grey and tinted in-world. */
	private static final int WATER_COLOUR = 0x3F76E4;
	private static final int WHITE = 0xFFFFFF;

	public CobbleGeneratorRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	/** The shaft stub, the drills, the rod and the lamps aren't referenced by any blockstate, so they're loaded explicitly. */
	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {SHAFT, DRILL, DRILL_BRASS, DRILL_ROD, LAMP_RUNNING,
			LAMP_FULL, LAMP_OFF})
			event.register(m);
	}

	@Override
	protected void renderSafe(CobbleGeneratorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(CobbleGeneratorBlock.FACING))
			return;
		Direction front = state.getValue(CobbleGeneratorBlock.FACING);
		boolean behind = CameraView.of(be, ms).behindGlass(front);
		renderShaft(be, state, front.getOpposite(), ms, buffer, light, overlay);
		VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
		// The chamber and the drill are inside, seen only through the front glass; the lamp stands proud of the front
		if (!behind)
			renderChamber(be, front, ms, vc, light);
		renderDrillAndLamp(be, state, front, partialTicks, ms, vc, light, overlay, !behind);
		// Redstone Link frequency items on top, once an antenna is fitted
		if (be.hasAntenna())
			LinkRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
	}

	/** The shaft end going into {@code back}, turning with the network; shared with the Auto Harvester. */
	static void renderShaft(KineticBlockEntity be, BlockState state, Direction back, PoseStack ms,
									MultiBufferSource buffer, int light, int overlay) {
		Direction.Axis axis = back.getAxis();
		float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		// Spin about the world axis exactly as Create spins shafts, so it lines up with the shaft it meets
		ms.mulPose(axis == Direction.Axis.X ? Axis.XP.rotation(angle) : Axis.ZP.rotation(angle));
		ms.mulPose(Axis.YP.rotationDegrees(-(back.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(),
			buffer.getBuffer(RenderType.solid()), state, Minecraft.getInstance().getModelManager().getModel(SHAFT),
			1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.solid());
		ms.popPose();
	}

	/** The chamber is modelled with the front facing north: lava on the west, water on the east, floor at y = 2. */
	private static void renderChamber(CobbleGeneratorBlockEntity be, Direction front, PoseStack ms, VertexConsumer vc,
									  int light) {
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		PoseStack.Pose pose = ms.last();
		int glow = LightTexture.FULL_BRIGHT;

		GeneratorProduct product = be.product();
		// Lava in the basin, and water beside it (or a block of blue ice, for basalt and blackstone)
		box(vc, pose, LAVA_STILL.get(), WHITE, glow, 2, 2, 2, 6, 3, 13);
		if (product.usesBlueIce())
			box(vc, pose, BLUE_ICE.get(), WHITE, light, 10, 2, 4, 14, 6, 9);
		else
			box(vc, pose, WATER_STILL.get(), WATER_COLOUR, light, 10, 2, 2, 14, 3, 13);

		// The block forming between them: one, or brass's batch of four smaller ones growing together
		float height = be.blockHeight() * be.getFormingFraction();
		if (height > 0.05f) {
			TextureAtlasSprite stone = BlockSprite.of(product.sprite());
			if (be.makesBatch())
				for (float[] at : BATCH)
					box(vc, pose, stone, WHITE, light, at[0], 2, at[1], at[0] + CobbleGeneratorBlockEntity.BATCH_HEIGHT,
						2 + height, at[1] + CobbleGeneratorBlockEntity.BATCH_HEIGHT);
			else
				box(vc, pose, stone, WHITE, light, 6.5f, 2, 5, 9.5f, 2 + height, 8);
		}

		// Pours from the nozzles down into the pools, only while it runs
		if (be.isRunning()) {
			box(vc, pose, LAVA_FLOW.get(), WHITE, glow, 3.5f, 3, 6.5f, 4.5f, 10, 7.5f);
			if (!product.usesBlueIce())
				box(vc, pose, WATER_FLOW.get(), WATER_COLOUR, light, 11.5f, 3, 6.5f, 12.5f, 10, 7.5f);
		}
		ms.popPose();
	}

	/**
	 * The drill over the forming block and the status lamp, both in the chamber's frame (front north). The drill's tip
	 * comes from the block entity (riding the block while it runs, raised when it doesn't) less its jab on each strike;
	 * it spins with the shaft about its own vertical axis, and its rod is stretched from the collar up into the sleeve.
	 * The drill is left out when it can't be seen ({@code drill} false); the lamp stands proud of the front, so never.
	 */
	private static void renderDrillAndLamp(CobbleGeneratorBlockEntity be, BlockState state, Direction front,
										   float partialTicks, PoseStack ms, VertexConsumer vc, int light, int overlay,
										   boolean drill) {
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		if (drill)
			renderDrill(be, state, front, partialTicks, ms, vc, light, overlay);
		ModelResourceLocation lamp = be.isRunning() ? LAMP_RUNNING : be.isFull() ? LAMP_FULL : be.isPowered() ? LAMP_OFF : null;
		if (lamp != null)
			render(ms, vc, state, lamp, LightTexture.FULL_BRIGHT, overlay);
		ms.popPose();
	}

	/** The drill and its rod, in the chamber's frame. */
	private static void renderDrill(CobbleGeneratorBlockEntity be, BlockState state, Direction front, float partialTicks,
									PoseStack ms, VertexConsumer vc, int light, int overlay) {
		boolean brass = be.makesBatch();
		float tip = be.getDrillTip(partialTicks) - be.getDrillJab(partialTicks);
		float spin = be.isRunning()
			? KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), front.getAxis()) : 0;
		ms.pushPose();
		ms.translate(DRILL_X / 16, tip / 16, DRILL_Z / 16);
		ms.mulPose(Axis.YP.rotation(spin));
		ms.translate(-DRILL_X / 16, 0, -DRILL_Z / 16);
		render(ms, vc, state, brass ? DRILL_BRASS : DRILL, light, overlay);
		ms.popPose();
		float rodBottom = tip + (brass ? COLLAR_BRASS : COLLAR);
		if (rodBottom < SLEEVE) {
			ms.pushPose();
			ms.translate(0, rodBottom / 16, 0);
			ms.scale(1, SLEEVE - rodBottom, 1);
			render(ms, vc, state, DRILL_ROD, light, overlay);
			ms.popPose();
		}
	}

	private static void render(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation location,
							   int light, int overlay) {
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), vc, state,
			Minecraft.getInstance().getModelManager().getModel(location), 1, 1, 1, light, overlay, ModelData.EMPTY,
			RenderType.cutout());
	}

	/**
	 * A box in pixel coordinates, every face but the bottom, each face textured from the matching part of the
	 * sprite (as a block model would map it).
	 */
	private static void box(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int rgb, int light,
							float x0, float y0, float z0, float x1, float y1, float z1) {
		float a = x0 / 16, b = y0 / 16, c = z0 / 16, d = x1 / 16, e = y1 / 16, f = z1 / 16;
		// up
		quad(vc, pose, s, rgb, light, 0, 1, 0,
			a, e, c, x0, z0, a, e, f, x0, z1, d, e, f, x1, z1, d, e, c, x1, z0);
		// north (front)
		quad(vc, pose, s, rgb, light, 0, 0, -1,
			d, e, c, 16 - x1, 16 - y1, d, b, c, 16 - x1, 16 - y0, a, b, c, 16 - x0, 16 - y0, a, e, c, 16 - x0, 16 - y1);
		// south
		quad(vc, pose, s, rgb, light, 0, 0, 1,
			a, e, f, x0, 16 - y1, a, b, f, x0, 16 - y0, d, b, f, x1, 16 - y0, d, e, f, x1, 16 - y1);
		// west
		quad(vc, pose, s, rgb, light, -1, 0, 0,
			a, e, c, z0, 16 - y1, a, b, c, z0, 16 - y0, a, b, f, z1, 16 - y0, a, e, f, z1, 16 - y1);
		// east
		quad(vc, pose, s, rgb, light, 1, 0, 0,
			d, e, f, 16 - z1, 16 - y1, d, b, f, 16 - z1, 16 - y0, d, b, c, 16 - z0, 16 - y0, d, e, c, 16 - z0, 16 - y1);
	}

	/** Four corners (x, y, z in blocks; u, v in sprite pixels 0-16), counter-clockwise seen from outside. */
	private static void quad(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int rgb, int light,
							 float nx, float ny, float nz, float... v) {
		for (int i = 0; i < 4; i++) {
			int o = i * 5;
			vc.addVertex(pose, v[o], v[o + 1], v[o + 2])
				.setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255)
				.setUv(s.getU(v[o + 3] / 16f), s.getV(v[o + 4] / 16f))
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
		}
	}
}
