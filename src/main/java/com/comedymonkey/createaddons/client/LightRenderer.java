package com.comedymonkey.createaddons.client;

import java.util.EnumMap;
import java.util.Map;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.light.LightBlock;
import com.comedymonkey.createaddons.content.light.LightBlockEntity;
import com.comedymonkey.createaddons.content.light.LightColours;
import com.comedymonkey.createaddons.content.light.LightKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * An Electric Light's lens while it's lit (design/specs/electric-lights.md): drawn full-bright, in the warm undyed glow
 * or its dye's (D13), darker by brightness step (D22). A starving light flickers for a second before it goes dark, and
 * flickers back on when power returns (P3); the High-Bay and Yard Light warm up from dark red to sodium amber over 4 s
 * (P4). Dark, the lens is the unlit glass baked into the block model, so a dark light costs nothing here.
 */
public class LightRenderer extends SafeBlockEntityRenderer<LightBlockEntity> {
	private static final Map<LightKind, ModelResourceLocation> GLOW = new EnumMap<>(LightKind.class), GLASS = new EnumMap<>(LightKind.class);

	static {
		for (LightKind k : LightKind.values()) {
			GLOW.put(k, model(k.id() + "_glow"));
			GLASS.put(k, model(k.id() + "_glass"));
		}
	}

	/** How much darker each brightness step's lens is (Full, Bright, Dim, Glow). */
	private static final float[] STEP_SHADE = {1f, 0.7f, 0.45f, 0.25f};
	/** Ticks a flicker frame lasts (the design's 90 ms). */
	private static final float FRAME = 1.8f;

	public LightRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/light/" + name));
	}

	/** Its lit lens (the coloured light reads where it sits from this). */
	public static ModelResourceLocation glow(LightKind kind) {
		return GLOW.get(kind);
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		GLOW.values().forEach(event::register);
		GLASS.values().forEach(event::register);
	}

	@Override
	protected void renderSafe(LightBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
	}

	/** A lit light has no lens in its block model: it's drawn this far, so a street keeps its lamps (the visual reads this too). */
	@Override
	public int getViewDistance() {
		return 256;
	}

	/** Parts reach a block beyond their own (a Cobra Head leans out over the street). */
	@Override
	public net.minecraft.world.phys.AABB getRenderBoundingBox(LightBlockEntity be) {
		return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(1);
	}

	/** Lit (or flickering) lights have a part to draw: their lens; dark ones show the glass in their block model. */
	public static boolean active(LightBlockEntity be) {
		return be.phase() != LightBlockEntity.Phase.DARK;
	}

	public static void parts(LightBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		if (!active(be) || be.getLevel() == null)
			return;
		LightKind kind = be.kind();
		BlockState state = be.getBlockState();
		long time = be.getLevel().getGameTime();
		boolean lit = lit(be, time, pt);
		ms.pushPose();
		turn(ms, kind, state.hasProperty(LightBlock.FACING) ? state.getValue(LightBlock.FACING) : Direction.UP);
		if (lit)
			sink.part(GLOW.get(kind), ms, LightTexture.FULL_BRIGHT, RenderType.cutout(), tint(be, time, pt));
		else
			sink.part(GLASS.get(kind), ms, light);
		ms.popPose();
	}

	/** Lit this frame: steady, or in a flicker frame that's on (more of them on as it comes back, fewer as it goes). */
	public static boolean lit(LightBlockEntity be, long time, float pt) {
		// ticks since, worked out whole before the part tick is added (game time is too large for a float)
		float t = (float) (time - be.flickerAt()) + pt;
		boolean fading = be.phase() == LightBlockEntity.Phase.FADING;
		if (t < 0 || t >= LightBlockEntity.FLICKER_TICKS)
			return !fading;
		int frame = (int) (t / FRAME);
		float chance = be.flickerOn() ? 0.35f + frame * 0.06f : 0.65f - frame * 0.06f;
		return noise(be.getBlockPos(), frame) < chance;
	}

	private static float noise(BlockPos pos, int frame) {
		long h = pos.asLong() * 0x9E3779B97F4A7C15L + frame * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h & 0xFFFFFF) / (float) 0x1000000;
	}

	/** The lens's colour: its dye's (or warm), warming up from sodium red, darker by brightness step. */
	static int tint(LightBlockEntity be, long time, float pt) {
		int c = warming(be, time, pt);
		float s = STEP_SHADE[be.step()];
		return rgb((c >> 16 & 255) * s, (c >> 8 & 255) * s, (c & 255) * s);
	}

	/** A lit lens's colour for a dye (or none) at a brightness step: what a Light Strip's lit diffuser is tinted. */
	public static int lens(@org.jetbrains.annotations.Nullable net.minecraft.world.item.DyeColor dye, int step) {
		int c = LightColours.lens(dye);
		float s = STEP_SHADE[Math.floorMod(step, STEP_SHADE.length)];
		return rgb((c >> 16 & 255) * s, (c >> 8 & 255) * s, (c & 255) * s);
	}

	/** Its lens's colour at full: its dye's (or warm), warming up from sodium red. */
	public static int warming(LightBlockEntity be, long time, float pt) {
		int c = LightColours.lens(be.dye());
		float w = warmth(be, time, pt);
		return w < 1 ? mix(LightColours.SODIUM, c, w * w * (3 - 2 * w)) : c;
	}

	/** How far through its warm-up it is, 0 to 1 (1 for lights that don't warm, or have). */
	public static float warmth(LightBlockEntity be, long time, float pt) {
		if (!be.kind().warms() || time - be.warmAt() >= LightBlockEntity.WARM_TICKS)
			return 1;
		float w = ((float) (time - be.warmAt()) + pt) / LightBlockEntity.WARM_TICKS;
		return w < 0 ? 1 : Math.min(1, w);
	}

	private static int mix(int a, int b, float f) {
		return rgb(Mth.lerp(f, a >> 16 & 255, b >> 16 & 255), Mth.lerp(f, a >> 8 & 255, b >> 8 & 255), Mth.lerp(f, a & 255, b & 255));
	}

	private static int rgb(float r, float g, float b) {
		return Mth.clamp(Math.round(r), 0, 255) << 16 | Mth.clamp(Math.round(g), 0, 255) << 8 | Mth.clamp(Math.round(b), 0, 255);
	}

	/**
	 * Turns a part to face its way, as the blockstate turns the block model (design/models/electric-lights.export.js):
	 * wall and face lights are built facing south, leaning heads pointing north, the rest don't turn.
	 */
	public static void turn(PoseStack ms, LightKind kind, Direction facing) {
		int x = 0, y = 0;
		switch (kind.mount()) {
			case WALL, ANY -> {
				switch (facing) {
					case WEST -> y = 90;
					case NORTH -> y = 180;
					case EAST -> y = 270;
					case UP -> x = kind.mount() == LightKind.Mount.ANY ? 90 : 0;
					case DOWN -> x = kind.mount() == LightKind.Mount.ANY ? 270 : 0;
					default -> {}
				}
			}
			case TOP_FACING -> {
				switch (facing) {
					case EAST -> y = 90;
					case SOUTH -> y = 180;
					case WEST -> y = 270;
					default -> {}
				}
			}
			default -> {}
		}
		if (x == 0 && y == 0)
			return;
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-y));
		ms.mulPose(Axis.XP.rotationDegrees(-x));
		ms.translate(-0.5, -0.5, -0.5);
	}
}
