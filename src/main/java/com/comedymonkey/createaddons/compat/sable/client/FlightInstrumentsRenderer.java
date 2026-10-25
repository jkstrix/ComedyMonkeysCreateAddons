package com.comedymonkey.createaddons.compat.sable.client;

import com.comedymonkey.createaddons.client.BlockSprite;
import com.comedymonkey.createaddons.compat.sable.FlightInstrumentsBlock;
import com.comedymonkey.createaddons.compat.sable.FlightInstrumentsBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The moving parts of the Flight Instruments, drawn on the panel that leans back 22.5 degrees from the lip
 * (modelled facing north): the altimeter's two hands, the airspeed needle, the attitude ball (sky over ground,
 * tilting with roll and dropping as the nose comes up) under a fixed aircraft symbol, and a little plane on the
 * heading dial pointing the way the ship faces.
 */
public class FlightInstrumentsRenderer extends SafeBlockEntityRenderer<FlightInstrumentsBlockEntity> {
	private static final BlockSprite WHITE = new BlockSprite(ResourceLocation.withDefaultNamespace("block/white_concrete"));
	/** Gauge centres on the panel texture, in sixteenths (its 64px divided by 4), u to the right and v down. */
	private static final float[][] GAUGES = {{4, 4}, {12, 4}, {4, 12}, {12, 12}};
	private static final float RADIUS = 2.75f;
	/** The panel's length up its lean, in pixels: it rises 14, from the lip to the top of the block. */
	private static final float LENGTH = (float) (14 / Math.cos(Math.toRadians(22.5)));
	/** The ship-hardware palette (design/briefs/flight-instruments.md): muted, never near-white. */
	private static final int SKY = 0x4F7C96, GROUND = 0x634532, LINE = 0xC9C2A8, NEEDLE = 0xC9C2A8, SHORT = 0xD99A4A;
	private static final int HUB = 0x2F343B, WINGS = 0x1F2327, CENTRE = 0xA3873F;

	public FlightInstrumentsRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(FlightInstrumentsBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(FlightInstrumentsBlock.FACING))
			return;
		Direction facing = state.getValue(FlightInstrumentsBlock.FACING);
		FlightInstrumentsBlockEntity.Readings r = FlightInstrumentsBlockEntity.read(be.getLevel(), be.getBlockPos(), facing);
		VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
		TextureAtlasSprite white = WHITE.get();

		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		// Onto the panel: standing 8 deep on the lip and leant back 22.5 degrees about its foot, as in the model
		ms.translate(8 / 16f, 2 / 16f, 8 / 16f);
		ms.mulPose(Axis.XP.rotationDegrees(22.5f));
		ms.translate(-8 / 16f, -2 / 16f, -8 / 16f);
		Panel p = new Panel(vc, ms.last(), white, light);

		// Altimeter: long hand per 100 m, short per 1000 m
		double alt = r.altitude();
		p.needle(GAUGES[0], (float) ((alt % 100 + 100) % 100 / 100 * 360), RADIUS * 0.85f, 0.2f, NEEDLE, 2);
		p.needle(GAUGES[0], (float) ((alt % 1000 + 1000) % 1000 / 1000 * 360), RADIUS * 0.55f, 0.3f, SHORT, 3);
		// Airspeed: 0 to 60 m/s over a 300 degree sweep
		p.needle(GAUGES[1], -150 + (float) Mth.clamp(r.airspeed(), 0, 60) * 5, RADIUS * 0.85f, 0.2f, NEEDLE, 2);
		// Attitude ball and the aircraft fixed over it
		p.attitude(GAUGES[2], (float) r.roll(), (float) r.pitch());
		// Heading: a little plane pointing the way the ship faces
		p.plane(GAUGES[3], (float) r.heading());
		ms.popPose();
	}

	/** Draws flat shapes on the panel surface, in panel texture units. */
	private static final class Panel {
		/** Sides of the attitude ball, and its edge as directions from the centre (clockwise from straight up). */
		private static final int SIDES = 28;
		private static final float[] DISC_U = new float[SIDES], DISC_V = new float[SIDES];
		/** A polygon's corners, reused for each one drawn: at most the ball's plus two. Render thread only. */
		private static final float[] POLY_U = new float[SIDES + 2], POLY_V = new float[SIDES + 2];

		static {
			for (int i = 0; i < SIDES; i++) {
				DISC_U[i] = dirU(i * 360f / SIDES);
				DISC_V[i] = dirV(i * 360f / SIDES);
			}
		}

		private final VertexConsumer vc;
		private final PoseStack.Pose pose;
		private final float u0, v0;
		private final int light;

		Panel(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite, int light) {
			this.vc = vc;
			this.pose = pose;
			this.u0 = sprite.getU(0.5f);
			this.v0 = sprite.getV(0.5f);
			this.light = light;
		}

		/** Clockwise from straight up as you look at the panel: the across (u) part of that direction... */
		private static float dirU(float degrees) {
			return Mth.sin((float) Math.toRadians(degrees));
		}

		/** ...and the down (v) part. */
		private static float dirV(float degrees) {
			return -Mth.cos((float) Math.toRadians(degrees));
		}

		/**
		 * A quad through four panel points (u, v), both sides, {@code layer} hundredths of a pixel above the panel so
		 * stacked shapes don't flicker.
		 */
		private void quad(float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd, int layer, int rgb) {
			float z = (8 - 0.015f * layer) / 16f;
			vertex(ua, va, z, rgb);
			vertex(ub, vb, z, rgb);
			vertex(uc, vc, z, rgb);
			vertex(ud, vd, z, rgb);
			// and the back, the other way round
			vertex(ud, vd, z, rgb);
			vertex(uc, vc, z, rgb);
			vertex(ub, vb, z, rgb);
			vertex(ua, va, z, rgb);
		}

		/** The panel's front faces north: texture u runs to -x, v runs down the panel to its foot. */
		private void vertex(float u, float v, float z, int rgb) {
			this.vc.addVertex(pose, (16 - u) / 16f, (2 + LENGTH * (1 - v / 16)) / 16f, z)
				.setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255)
				.setUv(u0, v0)
				.setLight(light)
				.setNormal(pose, 0, 0, -1);
		}

		/** The first {@code n} corners of {@link #POLY_U}, {@link #POLY_V}, a convex polygon, as a fan of quads from the first. */
		private void polygon(int n, int rgb, int layer) {
			for (int i = 1; i + 1 < n; i++)
				quad(POLY_U[0], POLY_V[0], POLY_U[i], POLY_V[i], POLY_U[i + 1], POLY_V[i + 1], POLY_U[i + 1], POLY_V[i + 1],
					layer, rgb);
		}

		void needle(float[] c, float angle, float length, float width, int rgb, int layer) {
			float du = dirU(angle), dv = dirV(angle);
			float px = -dv * width, py = du * width;
			float tx = c[0] + du * length, ty = c[1] + dv * length;
			quad(c[0] + px, c[1] + py, c[0] - px, c[1] - py, tx - px * 0.3f, ty - py * 0.3f, tx + px * 0.3f, ty + py * 0.3f,
				layer, rgb);
			// A hub over the pivot
			float h = width * 1.4f;
			quad(c[0] - h, c[1] - h, c[0] - h, c[1] + h, c[0] + h, c[1] + h, c[0] + h, c[1] - h, layer + 1, HUB);
		}

		/** The sky disc, the ground clipped from it by the horizon, the horizon line and the aircraft symbol. */
		void attitude(float[] c, float roll, float pitch) {
			for (int i = 0; i < SIDES; i++) {
				POLY_U[i] = c[0] + DISC_U[i] * RADIUS;
				POLY_V[i] = c[1] + DISC_V[i] * RADIUS;
			}
			polygon(SIDES, SKY, 1);

			// The horizon tilts against the roll and drops as the nose comes up
			float downU = dirU(180 - roll), downV = dirV(180 - roll);
			float drop = Mth.clamp(pitch / 40 * RADIUS, -RADIUS * 0.9f, RADIUS * 0.9f);
			float hx = c[0] + downU * drop, hy = c[1] + downV * drop;
			int n = 0;
			for (int i = 0; i < SIDES; i++) {
				int j = (i + 1) % SIDES;
				float au = c[0] + DISC_U[i] * RADIUS, av = c[1] + DISC_V[i] * RADIUS;
				float bu = c[0] + DISC_U[j] * RADIUS, bv = c[1] + DISC_V[j] * RADIUS;
				float sa = (au - hx) * downU + (av - hy) * downV;
				float sb = (bu - hx) * downU + (bv - hy) * downV;
				if (sa >= 0) {
					POLY_U[n] = au;
					POLY_V[n++] = av;
				}
				if ((sa >= 0) != (sb >= 0)) {
					float t = sa / (sa - sb);
					POLY_U[n] = au + (bu - au) * t;
					POLY_V[n++] = av + (bv - av) * t;
				}
			}
			if (n >= 3)
				polygon(n, GROUND, 2);

			// Horizon line across the ball where the sky meets the ground
			float alongU = -downV, alongV = downU;
			float half = (float) Math.sqrt(Math.max(0, RADIUS * RADIUS - drop * drop));
			float w = 0.12f;
			quad(hx - alongU * half - downU * w, hy - alongV * half - downV * w,
				hx - alongU * half + downU * w, hy - alongV * half + downV * w,
				hx + alongU * half + downU * w, hy + alongV * half + downV * w,
				hx + alongU * half - downU * w, hy + alongV * half - downV * w, 3, LINE);

			// The aircraft, fixed: two black wings and a white centre
			bar(c[0] - 1.9f, c[1] - 0.15f, c[0] - 0.55f, c[1] + 0.15f, WINGS, 4);
			bar(c[0] + 0.55f, c[1] - 0.15f, c[0] + 1.9f, c[1] + 0.15f, WINGS, 4);
			bar(c[0] - 0.35f, c[1] - 0.35f, c[0] + 0.35f, c[1] + 0.35f, CENTRE, 4);
		}

		private void bar(float ua, float va, float ub, float vb, int rgb, int layer) {
			quad(ua, va, ua, vb, ub, vb, ub, va, layer, rgb);
		}

		/** A little white plane, nose towards the heading. */
		void plane(float[] c, float heading) {
			float fu = dirU(heading), fv = dirV(heading);
			float su = -fv, sv = fu;
			// Fuselage
			quad(c[0] + fu * 1.3f + su * 0.18f, c[1] + fv * 1.3f + sv * 0.18f,
				c[0] + fu * 1.3f - su * 0.18f, c[1] + fv * 1.3f - sv * 0.18f,
				c[0] - fu * 1.1f - su * 0.18f, c[1] - fv * 1.1f - sv * 0.18f,
				c[0] - fu * 1.1f + su * 0.18f, c[1] - fv * 1.1f + sv * 0.18f, 2, NEEDLE);
			// Wings and tail
			wing(c, fu, fv, su, sv, 0.2f, 1.1f, 0.35f);
			wing(c, fu, fv, su, sv, -0.9f, 0.5f, 0.22f);
		}

		private void wing(float[] c, float fu, float fv, float su, float sv, float at, float span, float chord) {
			float x = c[0] + fu * at, y = c[1] + fv * at;
			quad(x + su * span, y + sv * span, x - su * span, y - sv * span,
				x - su * span - fu * chord, y - sv * span - fv * chord,
				x + su * span - fu * chord, y + sv * span - fv * chord, 2, NEEDLE);
		}
	}
}
