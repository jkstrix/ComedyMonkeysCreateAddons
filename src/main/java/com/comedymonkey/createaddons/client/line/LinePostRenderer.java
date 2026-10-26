package com.comedymonkey.createaddons.client.line;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.line.LineColors;
import com.comedymonkey.createaddons.content.line.LinePostBlock;
import com.comedymonkey.createaddons.content.line.LinePostBlockEntity;
import com.comedymonkey.createaddons.content.line.LineTier;
import com.comedymonkey.createaddons.content.line.Span;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The spans hung from a Line Post: thin cable sagging gently between two insulators, drawn by the end that owns it.
 * Thinner than the same wire placed on a face: bare copper 0.6 px, brass 0.9 by 0.5, covered (dyed) about 1 px. Lit
 * along its length like the world it passes through.
 */
public class LinePostRenderer implements BlockEntityRenderer<LinePostBlockEntity> {
	private static final int SEGMENTS = 16;

	public LinePostRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(LinePostBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		Level level = be.getLevel();
		if (level == null || be.spans().isEmpty())
			return;
		TextureAtlasSprite body = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
			.apply(ComedyMonkeysCreateAddons.asResource("block/fluid_line/body"));
		VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
		BlockPos pos = be.getBlockPos();
		Vec3 origin = Vec3.atLowerCornerOf(pos);
		for (Span s : be.spans()) {
			if (!s.owner())
				continue;
			Vec3 farTie = com.comedymonkey.createaddons.content.line.SpanHolder.farTie(level, pos.offset(s.offset()), s.to());
			if (farTie == null)
				continue;
			Vec3 a = be.tie(s.from()).subtract(origin), b = farTie.subtract(origin);
			boolean covered = s.dye() != null, brass = s.tier() == LineTier.BRASS;
			float w = (covered ? (brass ? 1.35f : 1.05f) : (brass ? 0.9f : 0.6f)) / 16, h = (covered ? 1.05f : brass ? 0.5f : 0.6f) / 16;
			cable(vc, ms, body, a, b, w, h, LineColors.of(s.tier(), s.dye()) | 0xFF000000, level, origin);
		}
	}

	/** A catenary-ish sag from a to b, as a thin four-sided tube. */
	private static void cable(VertexConsumer vc, PoseStack ms, TextureAtlasSprite sprite, Vec3 a, Vec3 b, float w, float h, int color,
							  Level level, Vec3 origin) {
		double length = a.distanceTo(b);
		double sag = Math.max(0.5, length / 24);
		Vec3 prev = a;
		for (int i = 1; i <= SEGMENTS; i++) {
			double t = i / (double) SEGMENTS;
			Vec3 p = a.lerp(b, t).subtract(0, 4 * sag * t * (1 - t), 0);
			Vec3 mid = prev.add(p).scale(0.5).add(origin);
			int light = LevelRenderer.getLightColor(level, BlockPos.containing(mid));
			segment(vc, ms.last(), sprite, prev, p, w, h, color, light);
			prev = p;
		}
	}

	private static void segment(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite s, Vec3 p, Vec3 q, float w, float h, int color, int light) {
		Vec3 dir = q.subtract(p).normalize();
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		if (side.lengthSqr() < 1e-4)
			side = dir.cross(new Vec3(1, 0, 0));
		side = side.normalize().scale(w / 2);
		Vec3 up = side.cross(dir).normalize().scale(h / 2);
		Vec3[] ring = {up.add(side), up.subtract(side), up.scale(-1).subtract(side), up.scale(-1).add(side)};
		float u0 = s.getU(0.4f), u1 = s.getU(0.6f), v0 = s.getV(0.4f), v1 = s.getV(0.6f);
		for (int k = 0; k < 4; k++) {
			Vec3 c0 = ring[k], c1 = ring[(k + 1) % 4];
			Vec3 n = c0.add(c1).normalize();
			quad(vc, pose, p.add(c0), q.add(c0), q.add(c1), p.add(c1), n, u0, v0, u1, v1, color, light);
		}
	}

	private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 n, float u0, float v0, float u1,
							 float v1, int color, int light) {
		vertex(vc, pose, a, u0, v0, n, color, light);
		vertex(vc, pose, b, u1, v0, n, color, light);
		vertex(vc, pose, c, u1, v1, n, color, light);
		vertex(vc, pose, d, u0, v1, n, color, light);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, Vec3 n, int color, int light) {
		vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
	}

	@Override
	public boolean shouldRenderOffScreen(LinePostBlockEntity be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	@Override
	public AABB getRenderBoundingBox(LinePostBlockEntity be) {
		return be.renderBox();
	}
}
