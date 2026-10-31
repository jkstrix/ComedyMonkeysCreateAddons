package com.comedymonkey.createaddons.client.wastes.bot;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlock;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlockEntity;
import com.comedymonkey.createaddons.content.wastes.bot.Custodian;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Custodian's four tether cables (D55, D59, D60; the approved preview design/models/clockwork-wastes-heart-hall.gen.js,
 * whose shapes and timings these are, in blocks): makers' copper, 2 px thick, patina'd, the copper worn through in sparse
 * patches on the underside that drags on the floor; a void-alloy plug where each leaves a socket on its back and a
 * void-alloy sleeve where each meets its pylon, on the crown plate's corner facing the dais, just under the fins. The
 * upper sockets run to the pylons behind it, the lower ones to those in front, swinging wide round its sides.
 * <ul>
 * <li>Kneeling asleep: slack, back over its robe plates, down onto the dais behind it, over its edge, along the floor to
 * the pylon's foot and up its corner to the sleeve.</li>
 * <li>Waking: as it rises they lift off the floor into sagging curves, with a small bounce (from 1.9 s of its waking clip).</li>
 * <li>In the fight: hanging curves, swaying a little; while it's shielded two starlight beads crawl along each standing
 * pylon's cable toward it (D67: none on a broken pylon's, as no arc runs from it), 3 s end to end (from 3.2 s of its
 * waking).</li>
 * <li>Kneeling at a reset: the beads stop and the cables settle back onto the floor.</li>
 * <li>Its death: dark and hanging, until 5 s of its death clip, when they unplug from its back and drop.</li>
 * </ul>
 * Each cable's socket end is where the posed model has that socket this frame ({@link BotRenderer}); the curves are worked
 * out afresh every frame.
 */
final class CustodianTethers {
	private CustodianTethers() {}

	/** The cable's materials, from the heart-hall preview (the bots' game export): the cable, void alloy, the bead. */
	static final ResourceLocation TEXTURE = ComedyMonkeysCreateAddons.asResource("textures/entity/custodian_tether.png");
	private static final float TEX_W = 48, TEX_H = 16;
	private static final int CABLE = 0, ALLOY = 16, BEAD = 32;
	private static final double PX = 1 / 16.0;
	/** Segments to a cable (the preview's). */
	static final int N = 22;
	/** Socket k (0-3): its side on its back (-1 its left, +1 its right); 0-1 the upper pair. */
	private static final int[] SIDE = { -1, 1, -1, 1 };

	/** One frame's socket: where the cable leaves it (1.5 px behind the plug's back) and the way its back faces. */
	record Socket(Vec3 p, Vec3 back) {}

	// ---------------------------------------------------------------- which socket each pylon's cable leaves from

	/**
	 * Each of its pylons' socket (0-3) by where the pylon stands from its dais as it kneels there: behind it, an upper
	 * socket; in front, a lower one; left or right, that side's.
	 */
	static int[] sockets(Custodian c, List<BlockPos> pylons) {
		Vec3 dais = daisCentre(c);
		float yaw = c.syncedDais() != null ? c.daisYaw() : c.getYRot();
		double r = yaw * Mth.DEG_TO_RAD;
		double fx = -Math.sin(r), fz = Math.cos(r), rx = -Math.cos(r), rz = -Math.sin(r);
		int[] out = new int[pylons.size()];
		boolean[] taken = new boolean[4];
		for (int i = 0; i < pylons.size(); i++) {
			BlockPos p = pylons.get(i);
			double dx = p.getX() + 0.5 - dais.x, dz = p.getZ() + 0.5 - dais.z;
			int want = (dx * fx + dz * fz < 0 ? 0 : 2) + (dx * rx + dz * rz < 0 ? 0 : 1);
			int k = want;
			for (int n = 0; n < 4 && taken[k]; n++)
				k = (want + n + 1) % 4;
			taken[k] = true;
			out[i] = k;
		}
		return out;
	}

	static Vec3 daisCentre(Custodian c) {
		BlockPos d = c.syncedDais();
		return d == null ? c.position() : Vec3.atBottomCenterOf(d);
	}

	/** Where its cable plugs into a pylon (bottom block {@code p}): the crown plate's corner toward the dais, 40.75 px up. */
	static Vec3 plugPoint(BlockPos p, Vec3 dais) {
		Vec3 c = Vec3.atBottomCenterOf(p), to = flat(dais.subtract(c));
		return c.add(0, 40.75 * PX, 0).add(to.scale(4.6 * PX));
	}

	// ---------------------------------------------------------------- the shapes (world space, blocks)

	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1e-12 ? new Vec3(1, 0, 0) : f.normalize();
	}

	/** Hanging: a curve that leaves the socket backward and outward and sags on its way to the pylon. */
	static Vec3[] taut(Socket s, Vec3 plug, Vec3 side, double sway) {
		Vec3 p1 = s.p().add(flat(s.back().add(side.scale(0.45))).scale(34 * PX)).add(0, (-12 + sway) * PX, 0);
		Vec3 p2 = plug.add(flat(s.p().subtract(plug)).scale(52 * PX)).add(0, (-24 - sway) * PX, 0);
		List<Vec3> pts = new ArrayList<>();
		for (int k = 0; k <= N * 4; k++) {
			double u = k / (double) (N * 4), a = (1 - u) * (1 - u) * (1 - u), b = 3 * u * (1 - u) * (1 - u), cc = 3 * u * u * (1 - u), d = u * u * u;
			pts.add(s.p().scale(a).add(p1.scale(b)).add(p2.scale(cc)).add(plug.scale(d)));
		}
		return resample(pts);
	}

	/**
	 * Slack on the floor: back over its plates and down onto the dais behind it, across the dais (3 x 3) and over its edge,
	 * along the floor to the pylon's foot and up its corner to the sleeve. {@code fallen}: unplugged, the socket end
	 * lying on the dais.
	 */
	static Vec3[] slack(Socket s, Vec3 plug, BlockPos pylon, Vec3 dais, boolean fallen) {
		double top = dais.y, floor = pylon.getY();
		Vec3 base = Vec3.atBottomCenterOf(pylon), back = s.back();
		Vec3 q1 = new Vec3(s.p().x + back.x * 22 * PX, top + PX, s.p().z + back.z * 22 * PX);
		Vec3 dir = flat(base.subtract(q1));
		double edge = 1e9;
		if (Math.abs(dir.x) > 1e-6)
			edge = Math.min(edge, ((dir.x > 0 ? 1.5 : -1.5) + dais.x - q1.x) / dir.x);
		if (Math.abs(dir.z) > 1e-6)
			edge = Math.min(edge, ((dir.z > 0 ? 1.5 : -1.5) + dais.z - q1.z) / dir.z);
		edge = Math.max(0, edge);
		Vec3 q2 = q1.add(dir.scale(edge)), q3 = q2.add(dir.scale(2.5 * PX));
		Vec3 q4 = new Vec3(q3.x + dir.x * 3 * PX, floor + PX, q3.z + dir.z * 3 * PX);
		Vec3 toP = flat(base.subtract(q4));
		Vec3 foot = new Vec3(base.x, floor + PX, base.z).subtract(toP.scale(7.5 * PX));
		Vec3 collar = base.add(0, 4.4 * PX, 0).subtract(toP.scale(5.6 * PX));
		Vec3 up = base.add(0, 37 * PX, 0).subtract(toP.scale(4.7 * PX));
		List<Vec3> pts = new ArrayList<>();
		if (fallen)
			pts.add(new Vec3(q1.x - back.x * 9 * PX, top + PX, q1.z - back.z * 9 * PX));
		else {
			pts.add(s.p());
			pts.add(s.p().add(back.scale(7 * PX)).add(0, -1.5 * PX, 0));
			pts.add(s.p().add(back.scale(13 * PX)).add(0, -4.5 * PX, 0));
			pts.add(new Vec3(s.p().x + back.x * 18 * PX, Math.max(top + 3 * PX, s.p().y - 12 * PX), s.p().z + back.z * 18 * PX));
		}
		pts.addAll(List.of(q1, q2, q3, q4, foot, collar, up, plug));
		return resample(pts);
	}

	/** A polyline resampled to N + 1 points evenly along its length. */
	static Vec3[] resample(List<Vec3> pts) {
		double[] acc = new double[pts.size()];
		for (int i = 1; i < pts.size(); i++)
			acc[i] = acc[i - 1] + pts.get(i).distanceTo(pts.get(i - 1));
		double total = acc[acc.length - 1];
		Vec3[] out = new Vec3[N + 1];
		int i = 1;
		for (int k = 0; k <= N; k++) {
			double s = k / (double) N * total;
			while (i < acc.length - 1 && acc[i] < s)
				i++;
			double seg = acc[i] - acc[i - 1], f = seg < 1e-9 ? 0 : (s - acc[i - 1]) / seg;
			out[k] = pts.get(i - 1).lerp(pts.get(i), Mth.clamp(f, 0, 1));
		}
		return out;
	}

	private static Vec3[] lerp(Vec3[] a, Vec3[] b, double f) {
		Vec3[] out = new Vec3[a.length];
		for (int i = 0; i < a.length; i++)
			out[i] = a[i].lerp(b[i], f);
		return out;
	}

	/** The preview's spring to 1 (lift, bounce, settle). */
	private static double spring(double t, double t0, double freq, double damp) {
		if (t < t0)
			return 0;
		double u = t - t0, w = 2 * Math.PI * freq / 4;
		return 1 - Math.exp(-damp * u) * (Math.cos(w * u) + damp / w * Math.sin(w * u));
	}

	private static double smooth(double u) {
		return u <= 0 ? 0 : u >= 1 ? 1 : u * u * (3 - 2 * u);
	}

	// ---------------------------------------------------------------- drawing

	/**
	 * Draws its cables, plugs and sleeves, in {@code ms} at its position this frame. {@code sockets}: each socket this
	 * frame (null if the model wasn't posed, then nothing is drawn).
	 */
	static void draw(Custodian c, float partialTick, PoseStack ms, MultiBufferSource buffers, @Nullable Socket[] sockets) {
		List<BlockPos> pylons = c.pylonPositions();
		if (sockets == null || pylons.isEmpty())
			return;
		Vec3 at = c.getPosition(partialTick), dais = daisCentre(c);
		int[] socketOf = sockets(c, pylons);
		Clip clip = c.playback.clip();
		String clipName = clip == null ? "" : clip.name();
		double t = clip == null ? 0 : clip.localTime(c.playback.elapsed(c.clipNow(partialTick)));
		double age = (c.tickCount + partialTick) / 20.0;
		Custodian.Phase phase = c.phase();
		boolean shielded = c.shielded();
		double yaw = Mth.rotLerp(partialTick, c.yBodyRotO, c.yBodyRot) * Mth.DEG_TO_RAD;
		Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		PoseStack.Pose pose = ms.last();
		for (int i = 0; i < pylons.size(); i++) {
			int k = socketOf[i];
			Socket local = sockets[k];
			if (local == null)
				continue;
			BlockPos pylon = pylons.get(i);
			Socket s = new Socket(local.p().add(at), local.back());
			Vec3 plug = plugPoint(pylon, dais), side = right.scale(SIDE[k]);
			Vec3[] pts;
			switch (phase) {
				case DORMANT -> pts = slack(s, plug, pylon, dais, false);
				case WAKING -> {
					double f = clipName.equals("waking") ? spring(t, 1.9, 1.1, 3.2) : 1;
					pts = lerp(slack(s, plug, pylon, dais, false), taut(s, plug, side, 0), f);
				}
				case KNEELING -> {
					double f = clipName.equals("kneel") ? 1 - smooth((t - 0.4) / 1.5) : 0;
					pts = lerp(slack(s, plug, pylon, dais, false), taut(s, plug, side, 0), f);
				}
				case DYING -> {
					double u = Math.max(0, (t - 5) / 0.45);
					double f = u >= 1 ? 1 - 0.05 * Math.exp(-(t - 5.45) * 8) * Math.abs(Math.sin((t - 5.45) * 18)) : u * u;
					pts = lerp(taut(s, plug, side, 0), slack(s, plug, pylon, dais, true), clipName.equals("death") ? f : 0);
				}
				default -> pts = taut(s, plug, side, 1.6 * Math.sin(age / 6 * 2 * Math.PI + k));
			}
			ConduitPylonBlockEntity pylonBe = ConduitPylonBlock.pylonAt(c.level(), pylon);
			boolean beads = beads(phase, clipName, t, shielded, pylonBe != null && pylonBe.isStanding());
			cable(c, vc, pose, pts, at);
			// the plug at the socket end (turned with it), the sleeve on the pylon's crown (turned toward the dais)
			Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
			box(vc, pose, pts[0].subtract(at), right, fwd, 1.75, 1.75, 1.75, 0, ALLOY, light(c, pts[0]));
			Vec3 toDais = flat(dais.subtract(Vec3.atBottomCenterOf(pylon))), across = new Vec3(-toDais.z, 0, toDais.x);
			int lp = light(c, plug);
			box(vc, pose, plug.subtract(at), toDais, across, 2, 1.75, 1.75, 0, ALLOY, lp);
			box(vc, pose, plug.subtract(at), toDais, across, 0.5, 1.25, 1.25, 2.5, ALLOY, lp);
			if (beads)
				for (int b = 0; b < 2; b++) {
					double u = 1 - ((age + b * 1.5 + k * 0.4) / 3 % 1), x = u * N;
					int j = Math.min(N - 1, (int) Math.floor(x));
					Vec3 p = pts[j].lerp(pts[j + 1], x - j), dir = pts[j + 1].subtract(pts[j]);
					Vec3 along = dir.lengthSqr() < 1e-12 ? right : dir.normalize();
					Vec3 ac = flat(along.cross(new Vec3(0, 1, 0)).lengthSqr() < 1e-9 ? right : along.cross(new Vec3(0, 1, 0)));
					box(vc, pose, p.subtract(at), along, ac, 1.6, 1.3, 1.3, 0, BEAD, LightTexture.FULL_BRIGHT);
				}
		}
	}

	/**
	 * Whether beads crawl along a cable: while it's shielded, from 3.2 s of its waking, until 0.3 s into a reset's kneel,
	 * never as it dies, and (D67) only along a standing pylon's cable, as the arcs run only from standing pylons.
	 */
	static boolean beads(Custodian.Phase phase, String clipName, double t, boolean shielded, boolean pylonStanding) {
		if (!shielded || !pylonStanding)
			return false;
		return switch (phase) {
			case DORMANT, DYING -> false;
			case WAKING -> !clipName.equals("waking") || t >= 3.2;
			case KNEELING -> clipName.equals("kneel") && t < 0.3;
			default -> true;
		};
	}

	private static int light(Custodian c, Vec3 world) {
		return LevelRenderer.getLightColor(c.level(), BlockPos.containing(world));
	}

	/**
	 * A cable through {@code pts} (world): each segment a 2 x 2 px bar along it, a little longer than the gap so they
	 * overlap, its worn underside always down (turned about the vertical, then tipped, as the preview's segments are).
	 */
	private static void cable(Custodian c, VertexConsumer vc, PoseStack.Pose pose, Vec3[] pts, Vec3 at) {
		Vec3 lastSide = null;
		for (int j = 0; j < N; j++) {
			Vec3 a = pts[j], b = pts[j + 1], d = b.subtract(a);
			double len = d.length();
			if (len < 1e-6)
				continue;
			Vec3 along = d.scale(1 / len), side = along.cross(new Vec3(0, 1, 0));
			if (side.lengthSqr() < 1e-6)
				side = lastSide != null ? lastSide : new Vec3(0, 0, 1);
			side = side.normalize();
			lastSide = side;
			Vec3 up = side.cross(along).normalize();
			Vec3 from = a.subtract(along.scale(0.35 * PX)).subtract(at);
			double l = len + 0.7 * PX;
			int light = light(c, a.lerp(b, 0.5));
			float u1 = (float) Math.min(16, l / PX);
			Vec3 h = side.scale(PX), v = up.scale(PX), L = along.scale(l);
			// the corners of the bar: from, +L, +-side, +-up
			Vec3 c000 = from.subtract(h).subtract(v), c100 = c000.add(L);
			Vec3 c010 = from.subtract(h).add(v), c110 = c010.add(L);
			Vec3 c001 = from.add(h).subtract(v), c101 = c001.add(L);
			Vec3 c011 = from.add(h).add(v), c111 = c011.add(L);
			// top and both sides: the patina rows; the underside: the worn rows; the ends
			quad(vc, pose, c010, c110, c111, c011, up, CABLE, 0, 6, u1, 8, light);
			quad(vc, pose, c001, c101, c100, c000, up.scale(-1), CABLE, 0, 12, u1, 14, light);
			quad(vc, pose, c011, c111, c101, c001, side, CABLE, 0, 6, u1, 8, light);
			quad(vc, pose, c000, c100, c110, c010, side.scale(-1), CABLE, 0, 6, u1, 8, light);
			quad(vc, pose, c000, c010, c011, c001, along.scale(-1), CABLE, 0, 6, 2, 8, light);
			quad(vc, pose, c101, c111, c110, c100, along, CABLE, 0, 6, 2, 8, light);
		}
	}

	/**
	 * A box centred {@code off} (blocks) along {@code x} from {@code centre}, half-sizes {@code hx}, {@code hy} (up),
	 * {@code hz} (across) in pixels, its faces from the texture region at {@code region}, fitted to their size.
	 */
	private static void box(VertexConsumer vc, PoseStack.Pose pose, Vec3 centre, Vec3 x, Vec3 z, double hx, double hy, double hz, double off,
							int region, int light) {
		Vec3 X = x.normalize(), Y = new Vec3(0, 1, 0), Z = z.normalize();
		Vec3 o = centre.add(X.scale(off * PX));
		Vec3 ex = X.scale(hx * PX), ey = Y.scale(hy * PX), ez = Z.scale(hz * PX);
		float w = (float) Math.min(16, hx * 2), h = (float) Math.min(16, hy * 2), d = (float) Math.min(16, hz * 2);
		Vec3 p000 = o.subtract(ex).subtract(ey).subtract(ez), p100 = o.add(ex).subtract(ey).subtract(ez);
		Vec3 p010 = o.subtract(ex).add(ey).subtract(ez), p110 = o.add(ex).add(ey).subtract(ez);
		Vec3 p001 = o.subtract(ex).subtract(ey).add(ez), p101 = o.add(ex).subtract(ey).add(ez);
		Vec3 p011 = o.subtract(ex).add(ey).add(ez), p111 = o.add(ex).add(ey).add(ez);
		quad(vc, pose, p010, p110, p111, p011, Y, region, 0, 0, w, d, light);
		quad(vc, pose, p001, p101, p100, p000, Y.scale(-1), region, 0, 0, w, d, light);
		quad(vc, pose, p011, p111, p101, p001, Z, region, 0, 0, w, h, light);
		quad(vc, pose, p000, p100, p110, p010, Z.scale(-1), region, 0, 0, w, h, light);
		quad(vc, pose, p000, p010, p011, p001, X.scale(-1), region, 0, 0, d, h, light);
		quad(vc, pose, p101, p111, p110, p100, X, region, 0, 0, d, h, light);
	}

	/** One face, its texture the rectangle (u0, v0)-(u1, v1) in pixels of the region at {@code region}. */
	private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, int region,
							 float u0, float v0, float u1, float v1, int light) {
		float U0 = (region + u0) / TEX_W, U1 = (region + u1) / TEX_W, V0 = v0 / TEX_H, V1 = v1 / TEX_H;
		vertex(vc, pose, a, U0, V0, normal, light);
		vertex(vc, pose, b, U1, V0, normal, light);
		vertex(vc, pose, c, U1, V1, normal, light);
		vertex(vc, pose, d, U0, V1, normal, light);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, Vec3 n, int light) {
		vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(255, 255, 255, 255).setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
	}
}
