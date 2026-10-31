package com.comedymonkey.createaddons.client.wastes;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.SubstationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The city's sky-cables (D61, D64; the look of design/models/clockwork-wastes-insulator.gen.js): each span a sagging
 * pair of patina'd copper strands 1.2 px across with a hairline gap down its top (copper showing where they rub at the
 * ties), drawn by the insulator that keeps it. While its district is fed a fine starlight line runs in the gap; when the
 * district wakes the 3 px bead runs the span in its 2 s turn and the line lights in behind it. A snapped span hangs about
 * 3 blocks from its tie with a curl, bare copper at the break, swaying a little; it never lights.
 */
public class SkyCableRenderer implements BlockEntityRenderer<DerelictInsulatorBlockEntity> {
	/** The span's sag, as a share of its length (the preview's 6-9 px on spans of 5-8 blocks). Provisional. */
	public static final double SAG = 0.075;
	private static final double W = 1.2 / 16, H = 0.8 / 16, LINE_W = 0.3 / 16, BEAD_L = 3 / 16.0, BEAD_W = 1.4 / 16, BEAD_H = 1.3 / 16;
	/** The snapped span's polyline (s along its heading, y up, px from its tie): the preview's SNAP_PTS. */
	private static final double[][] SNAP = { { 0, 0 }, { 1.5, -0.5 }, { 3, -1.8 }, { 4.3, -4 }, { 5.2, -7 }, { 5.5, -10 }, { 5.6, -20 },
		{ 5.6, -30 }, { 5.5, -40 }, { 5.6, -46 }, { 6.4, -49 }, { 7.6, -50 }, { 8.6, -49.2 } };

	public SkyCableRenderer(BlockEntityRendererProvider.Context context) {}

	private static TextureAtlasSprite sprite(String name) {
		return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ComedyMonkeysCreateAddons.asResource("block/wastes/" + name));
	}

	@Override
	public void render(DerelictInsulatorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		Level level = be.getLevel();
		if (level == null || be.spans().isEmpty())
			return;
		BlockPos pos = be.getBlockPos();
		Vec3 origin = Vec3.atLowerCornerOf(pos);
		Vec3 a = DerelictInsulatorBlockEntity.tie(level, pos);
		if (a == null)
			return;
		a = a.subtract(origin);
		VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
		Sprites sp = new Sprites();
		double now = level.getGameTime() + partialTicks;
		for (DerelictInsulatorBlockEntity.Span s : be.spans()) {
			if (s.snapped()) {
				snapped(vc, ms.last(), sp, a, s.heading(), now, level, origin);
				continue;
			}
			Vec3 far = DerelictInsulatorBlockEntity.tie(level, s.other());
			if (far == null)
				continue;
			Vec3 b = far.subtract(origin);
			// how far the line has lit: all of it, or behind the bead during its turn after the wake
			double lit = 0;
			if (be.fed()) {
				lit = 1;
				if (be.wakeAt() != SubstationBlockEntity.NEVER) {
					double t = now - be.wakeAt() - s.stage() * DerelictInsulatorBlockEntity.BEAD_TICKS;
					lit = Mth.clamp(t / DerelictInsulatorBlockEntity.BEAD_TICKS, 0, 1);
				}
			}
			span(vc, ms.last(), sp, s.fromHere() ? a : b, s.fromHere() ? b : a, lit, level, origin);
		}
	}

	private static final class Sprites {
		final TextureAtlasSprite side = sprite("span_side"), sideWorn = sprite("span_side_worn"), top = sprite("span_top"),
			topWorn = sprite("span_top_worn"), broken = sprite("span_break"), line = sprite("ring_lit"), bead = sprite("flash");
	}

	/** A span from a (where the bead starts) to b: segments of the strand pair, the lit line up to {@code lit}, the bead at it. */
	private static void span(VertexConsumer vc, PoseStack.Pose pose, Sprites sp, Vec3 a, Vec3 b, double lit, Level level, Vec3 origin) {
		double length = a.distanceTo(b);
		int n = Mth.clamp((int) Math.ceil(length * 4), 16, 128);
		double sag = Math.max(0.25, length * SAG);
		Vec3 prev = a;
		for (int i = 1; i <= n; i++) {
			double t = i / (double) n;
			Vec3 p = at(a, b, sag, t);
			boolean worn = i <= 2 || i > n - 2;
			int l = LevelRenderer.getLightColor(level, BlockPos.containing(prev.add(p).scale(0.5).add(origin)));
			bar(vc, pose, prev, p, W, H, worn ? sp.sideWorn : sp.side, worn ? sp.topWorn : sp.top, l, 0.05 / 16);
			// the lit line in the gap, chunk by chunk behind the bead
			if (lit > 0 && (i - 0.5) / n <= lit)
				strip(vc, pose, prev, p, LINE_W, H / 2 + 0.02 / 16, sp.line);
			prev = p;
		}
		if (lit > 0 && lit < 1) {
			Vec3 c = at(a, b, sag, lit), d = at(a, b, sag, Math.min(1, lit + 0.001)).subtract(at(a, b, sag, Math.max(0, lit - 0.001)));
			Vec3 dir = d.lengthSqr() < 1e-9 ? b.subtract(a).normalize() : d.normalize();
			Vec3 lift = new Vec3(0, (BEAD_H - H) / 2 + 0.25 / 16, 0);
			bar(vc, pose, c.subtract(dir.scale(BEAD_L / 2)).add(lift), c.add(dir.scale(BEAD_L / 2)).add(lift), BEAD_W, BEAD_H, sp.bead, sp.bead,
				LightTexture.FULL_BRIGHT, 0);
		}
	}

	private static Vec3 at(Vec3 a, Vec3 b, double sag, double t) {
		return a.lerp(b, t).subtract(0, 4 * sag * t * (1 - t), 0);
	}

	/** The snapped span: the preview's polyline from the tie along its heading, swaying 2.5 degrees over 4 s. */
	private static void snapped(VertexConsumer vc, PoseStack.Pose pose, Sprites sp, Vec3 a, float heading, double now, Level level, Vec3 origin) {
		double yaw = Math.toRadians(heading);
		Vec3 along = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		double sway = Math.toRadians(2.5 * Math.sin(now / 20 * Math.PI / 2));
		Vec3 prev = a;
		for (int k = 1; k < SNAP.length; k++) {
			double s = SNAP[k][0] / 16, y = SNAP[k][1] / 16;
			// the sway turns the hanging line about the heading's level axis through the tie
			double ys = y * Math.cos(sway), side = y * Math.sin(sway);
			Vec3 p = a.add(along.scale(s)).add(0, ys, 0).add(along.cross(new Vec3(0, 1, 0)).scale(side));
			boolean last = k == SNAP.length - 1, worn = k <= 2;
			int l = LevelRenderer.getLightColor(level, BlockPos.containing(p.add(origin)));
			bar(vc, pose, prev, p, W, H, last ? sp.broken : worn ? sp.sideWorn : sp.side, last ? sp.broken : worn ? sp.topWorn : sp.top, l, 0.05 / 16);
			prev = p;
		}
	}

	/** A bar from p to q, w across and h tall, its top on {@code top} (3 texel rows across: strand, gap, strand), sides on {@code side}. */
	private static void bar(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, Vec3 q, double w, double h, TextureAtlasSprite side, TextureAtlasSprite top,
							int light, double overlap) {
		Vec3 dir = q.subtract(p);
		double len = dir.length();
		if (len < 1e-6)
			return;
		dir = dir.scale(1 / len);
		p = p.subtract(dir.scale(overlap));
		q = q.add(dir.scale(overlap));
		Vec3 across = dir.cross(new Vec3(0, 1, 0));
		if (across.lengthSqr() < 1e-6)
			across = dir.cross(new Vec3(1, 0, 0));
		across = across.normalize();
		Vec3 up = across.cross(dir).normalize();
		Vec3 sx = across.scale(w / 2), sy = up.scale(h / 2);
		float u1 = side.getU((float) Math.min(1, len)), su0 = side.getU(0), sv0 = side.getV(0), sv1 = side.getV((float) h);
		// top (up), bottom, and the two sides
		quad(vc, pose, p.add(sy).subtract(sx), q.add(sy).subtract(sx), q.add(sy).add(sx), p.add(sy).add(sx), up,
			top.getU(0), top.getV(0), top.getU((float) Math.min(1, len)), top.getV(3 / 16f), light);
		quad(vc, pose, p.subtract(sy).add(sx), q.subtract(sy).add(sx), q.subtract(sy).subtract(sx), p.subtract(sy).subtract(sx), up.scale(-1),
			su0, sv0, u1, side.getV((float) w), light);
		quad(vc, pose, p.subtract(sy).subtract(sx), q.subtract(sy).subtract(sx), q.add(sy).subtract(sx), p.add(sy).subtract(sx), across.scale(-1),
			su0, sv0, u1, sv1, light);
		quad(vc, pose, p.add(sy).add(sx), q.add(sy).add(sx), q.subtract(sy).add(sx), p.subtract(sy).add(sx), across, su0, sv0, u1, sv1, light);
	}

	/** A flat full-bright strip on the bar's top: the lit line. */
	private static void strip(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, Vec3 q, double w, double rise, TextureAtlasSprite s) {
		Vec3 dir = q.subtract(p).normalize();
		Vec3 across = dir.cross(new Vec3(0, 1, 0));
		if (across.lengthSqr() < 1e-6)
			across = dir.cross(new Vec3(1, 0, 0));
		across = across.normalize().scale(w / 2);
		Vec3 up = across.cross(dir).normalize();
		Vec3 lift = up.scale(rise);
		quad(vc, pose, p.add(lift).subtract(across), q.add(lift).subtract(across), q.add(lift).add(across), p.add(lift).add(across), up,
			s.getU(0.25f), s.getV(0.25f), s.getU(0.75f), s.getV(0.75f), LightTexture.FULL_BRIGHT);
	}

	private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 n, float u0, float v0, float u1, float v1, int light) {
		vertex(vc, pose, a, u0, v0, n, light);
		vertex(vc, pose, b, u1, v0, n, light);
		vertex(vc, pose, c, u1, v1, n, light);
		vertex(vc, pose, d, u0, v1, n, light);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, Vec3 n, int light) {
		vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
	}

	@Override
	public boolean shouldRenderOffScreen(DerelictInsulatorBlockEntity be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}

	@Override
	public AABB getRenderBoundingBox(DerelictInsulatorBlockEntity be) {
		return be.renderBox();
	}
}
