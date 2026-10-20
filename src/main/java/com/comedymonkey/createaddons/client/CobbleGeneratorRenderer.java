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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws what goes on inside a Cobblestone Generator, behind its glass front, in 3D: pools of water and lava in the
 * basin (Minecraft's own animated water and lava), and while it runs, water and lava pouring from the inlet pipes and
 * a block of cobblestone rising between them in step with the drill-paced progress. Stopped, the pours stop; full,
 * a finished block sits there. The lava is full-bright, so it glows. Also turns the shaft end in the back.
 */
public class CobbleGeneratorRenderer extends SafeBlockEntityRenderer<CobbleGeneratorBlockEntity> {
	/** Short shaft stub, modelled going into the north face. */
	public static final ModelResourceLocation SHAFT =
		ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/cobble_generator_shaft"));

	private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
	private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
	private static final ResourceLocation LAVA_STILL = ResourceLocation.withDefaultNamespace("block/lava_still");
	private static final ResourceLocation LAVA_FLOW = ResourceLocation.withDefaultNamespace("block/lava_flow");
	private static final ResourceLocation BLUE_ICE = ResourceLocation.withDefaultNamespace("block/blue_ice");
	/** Plains water colour; the water sprites are grey and tinted in-world. */
	private static final int WATER_COLOUR = 0x3F76E4;
	private static final int WHITE = 0xFFFFFF;

	public CobbleGeneratorRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(CobbleGeneratorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(CobbleGeneratorBlock.FACING))
			return;
		Direction front = state.getValue(CobbleGeneratorBlock.FACING);
		renderShaft(be, state, front.getOpposite(), ms, buffer, light, overlay);
		renderChamber(be, front, ms, buffer.getBuffer(RenderType.cutout()), light);
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
		box(vc, pose, sprite(LAVA_STILL), WHITE, glow, 2, 2, 2, 6, 3, 13);
		if (product.usesBlueIce())
			box(vc, pose, sprite(BLUE_ICE), WHITE, light, 10, 2, 4, 14, 6, 9);
		else
			box(vc, pose, sprite(WATER_STILL), WATER_COLOUR, light, 10, 2, 2, 14, 3, 13);

		// The block forming between them
		float height = 3 * be.getFormingFraction();
		if (height > 0.05f)
			box(vc, pose, sprite(product.sprite()), WHITE, light, 6.5f, 2, 5, 9.5f, 2 + height, 8);

		// Pours from the nozzles down into the pools, only while it runs
		if (be.isRunning()) {
			box(vc, pose, sprite(LAVA_FLOW), WHITE, glow, 3.5f, 3, 6.5f, 4.5f, 10, 7.5f);
			if (!product.usesBlueIce())
				box(vc, pose, sprite(WATER_FLOW), WATER_COLOUR, light, 11.5f, 3, 6.5f, 12.5f, 10, 7.5f);
		}
		ms.popPose();
	}

	private static TextureAtlasSprite sprite(ResourceLocation id) {
		return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
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
