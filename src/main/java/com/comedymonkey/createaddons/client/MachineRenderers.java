package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.content.machine.TwoInputMachineBlock;
import com.comedymonkey.createaddons.content.machine.TwoInputMachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * What goes on inside the String Tangler and the Stone Infuser, both modelled with the window facing north and
 * the shaft going into the south face. Everything is drawn into one cutout buffer.
 */
public final class MachineRenderers {
	private static final ResourceLocation STRING = ResourceLocation.withDefaultNamespace("block/tripwire");
	private static final ResourceLocation COBBLESTONE = ResourceLocation.withDefaultNamespace("block/cobblestone");
	private static final int STRANDS = 6;

	private MachineRenderers() {}

	private static TextureAtlasSprite sprite(ResourceLocation id) {
		return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
	}

	/** Turns the pose so the model's north is the machine's front, about the block centre. */
	private static void faceFront(PoseStack ms, Direction front) {
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
	}

	/** The shaft stub in the back, spun about the world axis like the shaft it meets. */
	private static void shaft(TwoInputMachineBlockEntity be, BlockState state, Direction front, PoseStack ms,
							  VertexConsumer vc, int light, int overlay) {
		Direction back = front.getOpposite();
		Direction.Axis axis = back.getAxis();
		float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(axis == Direction.Axis.X ? Axis.XP.rotation(angle) : Axis.ZP.rotation(angle));
		ms.mulPose(Axis.YP.rotationDegrees(-(back.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), vc, state,
			Minecraft.getInstance().getModelManager().getModel(CobbleGeneratorRenderer.SHAFT), 1, 1, 1, light, overlay,
			ModelData.EMPTY, RenderType.cutout());
		ms.popPose();
	}

	/** A flat quad between four points (in block units), both sides, with a sprite's u0..u1 v0..v1 (0-16). */
	private static void quad(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int light, int rgb,
							 float[] a, float[] b, float[] c, float[] d, float u0, float v0, float u1, float v1) {
		float[][] pts = {a, b, c, d};
		float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < 4; i++) {
				int k = pass == 0 ? i : 3 - i;
				vc.addVertex(pose, pts[k][0], pts[k][1], pts[k][2])
					.setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255)
					.setUv(s.getU(uv[k][0] / 16f), s.getV(uv[k][1] / 16f))
					.setLight(light)
					.setNormal(pose, 0, 1, 0);
			}
		}
	}

	/** An axis-aligned box in pixels, every face, textured from the matching part of the sprite. */
	private static void cube(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int light,
							 float x0, float y0, float z0, float x1, float y1, float z1) {
		float a = x0 / 16, b = y0 / 16, c = z0 / 16, d = x1 / 16, e = y1 / 16, f = z1 / 16;
		float[] p000 = {a, b, c}, p100 = {d, b, c}, p010 = {a, e, c}, p110 = {d, e, c};
		float[] p001 = {a, b, f}, p101 = {d, b, f}, p011 = {a, e, f}, p111 = {d, e, f};
		int white = 0xFFFFFF;
		quad(vc, pose, s, light, white, p010, p011, p111, p110, x0, z0, x1, z1);           // up
		quad(vc, pose, s, light, white, p000, p100, p101, p001, x0, z0, x1, z1);           // down
		quad(vc, pose, s, light, white, p110, p100, p000, p010, 16 - x1, 16 - y1, 16 - x0, 16 - y0); // north
		quad(vc, pose, s, light, white, p011, p001, p101, p111, x0, 16 - y1, x1, 16 - y0); // south
		quad(vc, pose, s, light, white, p010, p000, p001, p011, z0, 16 - y1, z1, 16 - y0); // west
		quad(vc, pose, s, light, white, p111, p101, p100, p110, 16 - z1, 16 - y1, 16 - z0, 16 - y0); // east
	}

	// ---------------------------------------------------------------- String Tangler

	/**
	 * Six strands of string stretched between the copper frame and a spindle on the shaft axis. While it works they
	 * spin with the shaft and are drawn in and twisted tighter and tighter as the job goes on. Idle with string
	 * loaded, they hang slack and still.
	 */
	public static class Tangler extends SafeBlockEntityRenderer<TwoInputMachineBlockEntity> {
		public Tangler(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(TwoInputMachineBlockEntity be, float partialTicks, PoseStack ms,
								  MultiBufferSource buffer, int light, int overlay) {
			BlockState state = be.getBlockState();
			if (!state.hasProperty(TwoInputMachineBlock.FACING))
				return;
			Direction front = state.getValue(TwoInputMachineBlock.FACING);
			VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
			shaft(be, state, front, ms, vc, light, overlay);

			ms.pushPose();
			faceFront(ms, front);
			PoseStack.Pose pose = ms.last();
			boolean hasString = !be.getStack(TwoInputMachineBlockEntity.MAIN).isEmpty();
			float progress = be.getProgress(partialTicks);
			boolean working = be.isProcessing();
			float spin = working ? KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(),
				front.getAxis()) : 0;
			TextureAtlasSprite string = sprite(STRING);

			if (hasString || working) {
				// The axis runs north-south through the block centre; strands fan out round it
				float outer = Mth.lerp(progress, 6.2f, 3.4f) / 16f;
				float inner = Mth.lerp(progress, 1.2f, 0.4f) / 16f;
				float twist = working ? 0.6f + progress * 3.5f : 0.15f;
				float sag = working ? 0 : 0.8f / 16f;
				for (int i = 0; i < STRANDS; i++) {
					float base = i * Mth.TWO_PI / STRANDS + spin;
					float z0 = (5 + (i % 3) * 3f) / 16f;
					float z1 = (11 - (i % 3) * 3f) / 16f;
					float[] from = point(base, outer, z0, 0);
					float[] to = point(base + twist, inner, z1, sag);
					strand(vc, pose, string, light, from, to);
					// A second strand from the opposite side crossing it, so the tangle reads as a knot
					float[] from2 = point(base + Mth.PI, outer * 0.9f, z1, 0);
					float[] to2 = point(base + Mth.PI - twist, inner, z0, sag);
					strand(vc, pose, string, light, from2, to2);
				}
			}

			ms.popPose();
		}

		private static float[] point(float angle, float radius, float z, float drop) {
			return new float[] {0.5f + Mth.cos(angle) * radius, 0.5f + Mth.sin(angle) * radius - drop, z};
		}

		/** A thin ribbon of string between two points, turned to face across the shaft axis. */
		private static void strand(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, int light,
								   float[] from, float[] to) {
			float dx = to[0] - from[0], dy = to[1] - from[1];
			float len = Mth.sqrt(dx * dx + dy * dy);
			float w = 0.45f / 16f;
			float nx = len < 1e-4f ? w : -dy / len * w, ny = len < 1e-4f ? 0 : dx / len * w;
			float[] a = {from[0] + nx, from[1] + ny, from[2]}, b = {from[0] - nx, from[1] - ny, from[2]};
			float[] c = {to[0] - nx, to[1] - ny, to[2]}, d = {to[0] + nx, to[1] + ny, to[2]};
			quad(vc, pose, s, light, 0xF2F2F2, a, b, c, d, 0, 6, 16, 8);
		}
	}

	// ---------------------------------------------------------------- Stone Infuser

	/**
	 * A block of cobblestone on a stand in the chamber, with the new stone creeping up over it from the bottom as
	 * the job goes on. A finished block waits there until it's taken.
	 */
	public static class Infuser extends SafeBlockEntityRenderer<TwoInputMachineBlockEntity> {
		public Infuser(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(TwoInputMachineBlockEntity be, float partialTicks, PoseStack ms,
								  MultiBufferSource buffer, int light, int overlay) {
			BlockState state = be.getBlockState();
			if (!state.hasProperty(TwoInputMachineBlock.FACING))
				return;
			Direction front = state.getValue(TwoInputMachineBlock.FACING);
			VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
			shaft(be, state, front, ms, vc, light, overlay);

			ms.pushPose();
			faceFront(ms, front);
			PoseStack.Pose pose = ms.last();
			ItemStack output = be.getStack(TwoInputMachineBlockEntity.OUTPUT);
			ItemStack making = be.getMaking();
			boolean hasCobble = !be.getStack(TwoInputMachineBlockEntity.MAIN).isEmpty();
			// A slow turn while it works
			float turn = be.isProcessing() ? AnimationTickHolder.getRenderTime(be.getLevel()) * 1.5f : 0;
			ms.pushPose();
			ms.translate(0.5, 0, 0.5);
			ms.mulPose(Axis.YP.rotationDegrees(turn));
			ms.translate(-0.5, 0, -0.5);
			pose = ms.last();

			if (be.isProcessing() && !making.isEmpty()) {
				float rise = 8 * be.getProgress(partialTicks);
				cube(vc, pose, sprite(COBBLESTONE), light, 4, 3.5f, 4, 12, 11.5f, 12);
				if (rise > 0.05f)
					cube(vc, pose, blockSprite(making), light, 3.9f, 3.4f, 3.9f, 12.1f, 3.5f + rise, 12.1f);
			} else if (!output.isEmpty()) {
				cube(vc, pose, blockSprite(output), light, 4, 3.5f, 4, 12, 11.5f, 12);
			} else if (hasCobble) {
				cube(vc, pose, sprite(COBBLESTONE), light, 4, 3.5f, 4, 12, 11.5f, 12);
			}
			ms.popPose();
			ms.popPose();
		}

		/** The particle (side) sprite of a block item's block, or cobblestone if it isn't one. */
		private static TextureAtlasSprite blockSprite(ItemStack stack) {
			if (stack.getItem() instanceof BlockItem item)
				return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
					.getParticleIcon(item.getBlock().defaultBlockState());
			return sprite(COBBLESTONE);
		}
	}
}
