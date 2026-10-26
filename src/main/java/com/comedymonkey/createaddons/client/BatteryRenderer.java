package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.electric.BatteryBlock;
import com.comedymonkey.createaddons.content.electric.BatteryBlockEntity;
import com.comedymonkey.createaddons.content.electric.BatteryTowers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * What moves on a Battery and an Industrial Battery (design/models/battery.gen.js, its clips charging, giving, forming
 * and lightning). A Battery: its two tubes lit to its charge in eighths (a dim ember at the foot when empty), a pulse
 * running up them while it charges and down while it gives, and the flow lamp lit for which. An Industrial Battery, from
 * its master: the level column up its front from the plinth to the crown, its pulse, the cyan ripple that climbs it when
 * it forms, and a strike's flash and arcs from the rod to the four bushings. All of it glows: drawn full-bright.
 */
public class BatteryRenderer extends SafeBlockEntityRenderer<BatteryBlockEntity> {
	public static final ModelResourceLocation FILL = model("battery_fill");
	public static final ModelResourceLocation EMBER = model("battery_ember");
	public static final ModelResourceLocation PULSE = model("battery_pulse");
	public static final ModelResourceLocation LAMP_IN = model("battery_lamp_in");
	public static final ModelResourceLocation LAMP_OUT = model("battery_lamp_out");
	public static final ModelResourceLocation LEVEL = model("battery_tower_level");
	public static final ModelResourceLocation LEVEL_EMBER = model("battery_tower_ember");
	public static final ModelResourceLocation TOWER_PULSE = model("battery_tower_pulse");
	public static final ModelResourceLocation RIPPLE = model("battery_tower_ripple");
	public static final ModelResourceLocation ARC = model("battery_arc");

	/** The tubes' fill runs from y 3.5 to 12.5 px. */
	private static final float TUBE_Y0 = 3.5f, TUBE_Y1 = 12.5f;
	/** The tower's level column, from just above the plinth to just under the crown (px, from the master's foot). */
	private static final float COLUMN_Y0 = 3;
	/** A bushing's top, above the top of the tower's top layer, and its distance in from the tower's corners (px). */
	private static final float BUSHING_TOP = 15.55f, BUSHING_IN = 5.5f;
	/** A Lightning Rod's tip, above its foot (px). */
	private static final float ROD_TIP = 16;
	/** Flashes of a strike's arcs, from and to (seconds after it). */
	private static final float[][] ARC_FLASHES = {{0, 0.08f}, {0.12f, 0.25f}, {0.3f, 0.36f}};

	public BatteryRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {FILL, EMBER, PULSE, LAMP_IN, LAMP_OUT, LEVEL, LEVEL_EMBER, TOWER_PULSE, RIPPLE, ARC})
			event.register(m);
	}

	@Override
	protected void renderSafe(BatteryBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
	}

	/** A tower's master draws the whole tower's moving parts, so it's drawn while any of the tower is in view. */
	@Override
	public boolean shouldRenderOffScreen(BatteryBlockEntity be) {
		return be.isMaster();
	}

	@Override
	public int getViewDistance() {
		return 96;
	}

	@Override
	public AABB getRenderBoundingBox(BatteryBlockEntity be) {
		if (!be.isMaster())
			return new AABB(be.getBlockPos());
		BlockPos c = BatteryTowers.center(be.getBlockPos(), be.towerFacing());
		return new AABB(c.getX() - 1, c.getY(), c.getZ() - 1, c.getX() + 2, c.getY() + be.height() + 2, c.getZ() + 2);
	}

	/** Its parts (for this renderer, or Flywheel's visual): a plain Battery's, or a tower's from its master. */
	public static void parts(BatteryBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		if (be.getLevel() == null)
			return;
		float seconds = (be.getLevel().getGameTime() % 24000 + partialTicks) / 20f;
		if (!be.getBlockState().getValue(BatteryBlock.FORMED)) {
			battery(be, seconds, ms, sink);
			return;
		}
		if (be.isMaster())
			tower(be, partialTicks, seconds, ms, sink);
	}

	private static void battery(BatteryBlockEntity be, float seconds, PoseStack ms, PartSink sink) {
		int full = LightTexture.FULL_BRIGHT;
		float f = Math.round(be.shownFill() * 8) / 8f;
		byte flow = be.shownFlow();
		ms.pushPose();
		face(ms, be.getBlockState().getValue(BatteryBlock.FACING));
		if (f <= 0) {
			sink.part(EMBER, ms, full);
		} else {
			ms.pushPose();
			ms.translate(0, TUBE_Y0 / 16, 0);
			ms.scale(1, f, 1);
			ms.translate(0, -TUBE_Y0 / 16, 0);
			sink.part(FILL, ms, full);
			ms.popPose();
		}
		if (flow != 0 && f > 0) {
			ms.pushPose();
			ms.translate(0, pulseY(seconds, 0.8f, f, flow > 0, TUBE_Y0, TUBE_Y1 - TUBE_Y0, 1) / 16, 0);
			sink.part(PULSE, ms, full);
			ms.popPose();
		}
		if (flow > 0)
			sink.part(LAMP_IN, ms, full);
		else if (flow < 0)
			sink.part(LAMP_OUT, ms, full);
		ms.popPose();
	}

	private static void tower(BatteryBlockEntity be, float partialTicks, float seconds, PoseStack ms, PartSink sink) {
		int full = LightTexture.FULL_BRIGHT;
		int h = be.height();
		float span = (h - 1) * 16 + 13 - COLUMN_Y0;
		float f = be.drawnFill(partialTicks);
		byte flow = be.shownFlow();
		ms.pushPose();
		face(ms, be.towerFacing());
		if (f <= 0.002f) {
			sink.part(LEVEL_EMBER, ms, full);
		} else {
			ms.pushPose();
			ms.translate(0, COLUMN_Y0 / 16, 0);
			ms.scale(1, span * f / 16, 1);
			sink.part(LEVEL, ms, full);
			ms.popPose();
		}
		if (flow != 0 && f > 0.02f) {
			ms.pushPose();
			ms.translate(0, pulseY(seconds, 1.4f, f, flow > 0, COLUMN_Y0, span, 2) / 16, 0);
			sink.part(TOWER_PULSE, ms, full);
			ms.popPose();
		}
		// forming: a ripple of cyan climbs the column from the plinth
		if (be.formAge >= 0) {
			float s = (be.formAge + partialTicks) / 20f;
			if (s >= 0.4f && s < 1.6f) {
				ms.pushPose();
				ms.translate(0, (Math.min(span, (s - 0.4f) / 1.1f * (span + 8)) + COLUMN_Y0 - 8) / 16, 0);
				sink.part(RIPPLE, ms, full);
				ms.popPose();
			}
		}
		// a strike: the column flashes, and arcs jump from the rod's tip to the four bushings
		if (be.strikeAge >= 0) {
			float s = (be.strikeAge + partialTicks) / 20f;
			if (s < 0.25f) {
				ms.pushPose();
				ms.scale(1, (span + 3) / 8, 1);
				sink.part(RIPPLE, ms, full);
				ms.popPose();
			}
			for (int k = 0; k < ARC_FLASHES.length; k++)
				if (s >= ARC_FLASHES[k][0] && s < ARC_FLASHES[k][1])
					arcs(be, h, k, ms, sink);
		}
		ms.popPose();
	}

	/**
	 * Arcs from the rod's tip (on the top block struck) to each bushing, in the master's pixels as the tower faces north
	 * (the tower's x 0-48 is the master's -16 to 32): six jagged steps each, a new shape each flash.
	 */
	private static void arcs(BatteryBlockEntity be, int h, int flash, PoseStack ms, PartSink sink) {
		int at = be.strikeAt;
		float[] tip = {(at % 3) * 16 + 8 - 16, h * 16 + ROD_TIP, (at / 3) * 16 + 8};
		float top = (h - 1) * 16 + 16 + BUSHING_TOP;
		int i = 0;
		for (int ix : new int[] {0, 2})
			for (int iz : new int[] {0, 2}) {
				float[] b = {(ix == 0 ? BUSHING_IN : 48 - BUSHING_IN) - 16, top, iz == 0 ? BUSHING_IN : 48 - BUSHING_IN};
				arc(tip, b, flash * 4 + i++, ms, sink);
			}
	}

	private static void arc(float[] a, float[] b, int seed, PoseStack ms, PartSink sink) {
		float[] last = a;
		for (int i = 1; i <= 6; i++) {
			float s = i / 6f;
			float[] p = i == 6 ? b : new float[] {Mth.lerp(s, a[0], b[0]) + jitter(seed, i, 1), Mth.lerp(s, a[1], b[1]) + jitter(seed, i, 2),
				Mth.lerp(s, a[2], b[2]) + jitter(seed, i, 3)};
			segment(last, p, ms, sink);
			last = p;
		}
	}

	/** Up to 1.5 px either way, the same for the same seed. */
	private static float jitter(int seed, int i, int k) {
		long x = (seed * 73856093L) ^ (i * 19349663L) ^ (k * 83492791L);
		x = (x ^ (x >>> 13)) * 0x5bd1e995L;
		return ((x >>> 8) & 0xffff) / 65535f * 3 - 1.5f;
	}

	/** The arc piece (16 px along x through its foot) laid from p to q. */
	private static void segment(float[] p, float[] q, PoseStack ms, PartSink sink) {
		float dx = q[0] - p[0], dy = q[1] - p[1], dz = q[2] - p[2];
		float flat = Mth.sqrt(dx * dx + dz * dz), len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 0.01f)
			return;
		ms.pushPose();
		ms.translate(p[0] / 16, p[1] / 16, p[2] / 16);
		ms.mulPose(Axis.YP.rotation((float) Math.atan2(-dz, dx)));
		ms.mulPose(Axis.ZP.rotation((float) Math.atan2(dy, flat)));
		ms.scale(len / 16, 1, 1);
		sink.part(ARC, ms, LightTexture.FULL_BRIGHT);
		ms.popPose();
	}

	/**
	 * A pulse running the lit part of a fill (from y0, {@code span} long, lit to {@code f}): up while charging, down while
	 * giving, once a {@code period}; {@code size} its own height (px).
	 */
	static float pulseY(float seconds, float period, float f, boolean up, float y0, float span, float size) {
		float k = (seconds % period) / period, u = up ? k : 1 - k;
		return y0 + u * Math.max(0, span * f - size);
	}

	/** Turns a model made facing north to face {@code facing}, about the block's centre. */
	private static void face(PoseStack ms, Direction facing) {
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
	}
}
