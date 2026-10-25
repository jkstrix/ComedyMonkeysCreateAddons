package com.comedymonkey.createaddons.compat.sable.client;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.compat.sable.FlightInstrumentsBlock;
import com.comedymonkey.createaddons.compat.sable.FlightInstrumentsBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The moving parts of the Flight Instruments, drawn on the panel that leans back 22.5 degrees from the lip
 * (modelled facing north): the altimeter's two hands, the airspeed needle, the attitude ball (sky over ground,
 * tilting with roll and dropping as the nose comes up) under a fixed aircraft symbol, and a little plane on the
 * heading dial pointing the way the ship faces.
 */
public class FlightInstrumentsRenderer extends SafeBlockEntityRenderer<FlightInstrumentsBlockEntity> {
	private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("block/white_concrete");
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
		TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);

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

		/** A point on the panel, {@code layer} hundredths of a pixel above it so stacked shapes don't flicker. */
		private float[] at(float u, float v, int layer) {
			// The panel's front faces north: texture u runs to -x, v runs down the panel to its foot
			return new float[] {(16 - u) / 16f, (2 + LENGTH * (1 - v / 16)) / 16f, (8 - 0.015f * layer) / 16f};
		}

		private void quad(float[] a, float[] b, float[] c, float[] d, int rgb) {
			float[][] pts = {a, b, c, d};
			for (int pass = 0; pass < 2; pass++)
				for (int i = 0; i < 4; i++) {
					float[] p = pts[pass == 0 ? i : 3 - i];
					vc.addVertex(pose, p[0], p[1], p[2])
						.setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255)
						.setUv(u0, v0)
						.setLight(light)
						.setNormal(pose, 0, 0, -1);
				}
		}

		/** A convex polygon (u, v pairs) as a fan of quads from its first point. */
		private void polygon(List<float[]> pts, int rgb, int layer) {
			for (int i = 1; i + 1 < pts.size(); i++) {
				float[] a = pts.get(0), b = pts.get(i), c = pts.get(i + 1);
				quad(at(a[0], a[1], layer), at(b[0], b[1], layer), at(c[0], c[1], layer), at(c[0], c[1], layer), rgb);
			}
		}

		/** Clockwise from straight up as you look at the panel. */
		private static float[] dir(float degrees) {
			float a = (float) Math.toRadians(degrees);
			return new float[] {Mth.sin(a), -Mth.cos(a)};
		}

		void needle(float[] c, float angle, float length, float width, int rgb, int layer) {
			float[] d = dir(angle);
			float px = -d[1] * width, py = d[0] * width;
			float tx = c[0] + d[0] * length, ty = c[1] + d[1] * length;
			quad(at(c[0] + px, c[1] + py, layer), at(c[0] - px, c[1] - py, layer),
				at(tx - px * 0.3f, ty - py * 0.3f, layer), at(tx + px * 0.3f, ty + py * 0.3f, layer), rgb);
			// A hub over the pivot
			float h = width * 1.4f;
			quad(at(c[0] - h, c[1] - h, layer + 1), at(c[0] - h, c[1] + h, layer + 1),
				at(c[0] + h, c[1] + h, layer + 1), at(c[0] + h, c[1] - h, layer + 1), HUB);
		}

		/** The sky disc, the ground clipped from it by the horizon, the horizon line and the aircraft symbol. */
		void attitude(float[] c, float roll, float pitch) {
			int sides = 28;
			List<float[]> disc = new ArrayList<>();
			for (int i = 0; i < sides; i++) {
				float[] d = dir(i * 360f / sides);
				disc.add(new float[] {c[0] + d[0] * RADIUS, c[1] + d[1] * RADIUS});
			}
			polygon(disc, SKY, 1);

			// The horizon tilts against the roll and drops as the nose comes up
			float[] down = dir(180 - roll);
			float drop = Mth.clamp(pitch / 40 * RADIUS, -RADIUS * 0.9f, RADIUS * 0.9f);
			float hx = c[0] + down[0] * drop, hy = c[1] + down[1] * drop;
			List<float[]> ground = new ArrayList<>();
			for (int i = 0; i < sides; i++) {
				float[] a = disc.get(i), b = disc.get((i + 1) % sides);
				float sa = (a[0] - hx) * down[0] + (a[1] - hy) * down[1];
				float sb = (b[0] - hx) * down[0] + (b[1] - hy) * down[1];
				if (sa >= 0)
					ground.add(a);
				if ((sa >= 0) != (sb >= 0)) {
					float t = sa / (sa - sb);
					ground.add(new float[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
				}
			}
			if (ground.size() >= 3)
				polygon(ground, GROUND, 2);

			// Horizon line across the ball where the sky meets the ground
			float[] along = {-down[1], down[0]};
			float half = (float) Math.sqrt(Math.max(0, RADIUS * RADIUS - drop * drop));
			float w = 0.12f;
			quad(at(hx - along[0] * half - down[0] * w, hy - along[1] * half - down[1] * w, 3),
				at(hx - along[0] * half + down[0] * w, hy - along[1] * half + down[1] * w, 3),
				at(hx + along[0] * half + down[0] * w, hy + along[1] * half + down[1] * w, 3),
				at(hx + along[0] * half - down[0] * w, hy + along[1] * half - down[1] * w, 3), LINE);

			// The aircraft, fixed: two black wings and a white centre
			bar(c[0] - 1.9f, c[1] - 0.15f, c[0] - 0.55f, c[1] + 0.15f, WINGS, 4);
			bar(c[0] + 0.55f, c[1] - 0.15f, c[0] + 1.9f, c[1] + 0.15f, WINGS, 4);
			bar(c[0] - 0.35f, c[1] - 0.35f, c[0] + 0.35f, c[1] + 0.35f, CENTRE, 4);
		}

		private void bar(float ua, float va, float ub, float vb, int rgb, int layer) {
			quad(at(ua, va, layer), at(ua, vb, layer), at(ub, vb, layer), at(ub, va, layer), rgb);
		}

		/** A little white plane, nose towards the heading. */
		void plane(float[] c, float heading) {
			float[] f = dir(heading);
			float[] s = {-f[1], f[0]};
			// Fuselage
			quad(at(c[0] + f[0] * 1.3f + s[0] * 0.18f, c[1] + f[1] * 1.3f + s[1] * 0.18f, 2),
				at(c[0] + f[0] * 1.3f - s[0] * 0.18f, c[1] + f[1] * 1.3f - s[1] * 0.18f, 2),
				at(c[0] - f[0] * 1.1f - s[0] * 0.18f, c[1] - f[1] * 1.1f - s[1] * 0.18f, 2),
				at(c[0] - f[0] * 1.1f + s[0] * 0.18f, c[1] - f[1] * 1.1f + s[1] * 0.18f, 2), NEEDLE);
			// Wings and tail
			wing(c, f, s, 0.2f, 1.1f, 0.35f);
			wing(c, f, s, -0.9f, 0.5f, 0.22f);
		}

		private void wing(float[] c, float[] f, float[] s, float at, float span, float chord) {
			float x = c[0] + f[0] * at, y = c[1] + f[1] * at;
			quad(at(x + s[0] * span, y + s[1] * span, 2), at(x - s[0] * span, y - s[1] * span, 2),
				at(x - s[0] * span - f[0] * chord, y - s[1] * span - f[1] * chord, 2),
				at(x + s[0] * span - f[0] * chord, y + s[1] * span - f[1] * chord, 2), NEEDLE);
		}
	}
}
