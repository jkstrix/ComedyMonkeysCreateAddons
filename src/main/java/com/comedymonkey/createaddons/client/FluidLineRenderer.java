package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.line.Fitting;
import com.comedymonkey.createaddons.content.line.FluidLineBlock;
import com.comedymonkey.createaddons.content.line.FluidLineBlockEntity;
import com.comedymonkey.createaddons.content.line.LineSide;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A Fluid Line's moving parts: the fluid sliding along each sight slot while the line moves it (out towards the tanks,
 * slow on copper, quick on brass), the filter fluid behind an Intake's or a Filter's mesh, and the in-line fittings: a
 * Valve's wheel turning to its setting, a Meter's needle swinging to its reading, a Splitter's dial clicking round. Lit
 * like the pipe, never full-bright. Everything else is the baked block model, so a plain still line costs nothing here.
 * The fittings are part models ({@link #parts}), which Flywheel draws when it's on; the fluid always comes from here.
 */
public class FluidLineRenderer implements BlockEntityRenderer<FluidLineBlockEntity> {
	/** The slot's fluid sits level with the rim, a quarter pixel above the dark idle strip. */
	private static final float SLOT_Y = 10f / 16, SLOT_X0 = 7.25f / 16, SLOT_X1 = 8.75f / 16;
	/** How far the slot runs in from the face: to the middle on a straight piece, short of the hub on the rest. */
	private static final float SLOT_NEAR = 1.75f, SLOT_NEAR_INTAKE = 5f, SLOT_FAR = 8f, SLOT_FAR_HUB = 3.5f;

	public static final ModelResourceLocation VALVE_BODY = part("valve_body"), VALVE_WHEEL = part("valve_wheel"),
		FILTER = part("filter"), FILTER_GLASS = part("filter_glass"), FLAG = part("flag"), METER = part("meter"), METER_NEEDLE = part("meter_needle"), SPLITTER = part("splitter"),
		DIAL_EVEN = part("splitter_dial_even"), DIAL_NEAREST = part("splitter_dial_nearest"), DIAL_ROBIN = part("splitter_dial_robin");

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/line_fitting/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {VALVE_BODY, VALVE_WHEEL, FILTER, FILTER_GLASS, FLAG, METER, METER_NEEDLE, SPLITTER,
			DIAL_EVEN, DIAL_NEAREST, DIAL_ROBIN})
			event.register(m);
	}

	public FluidLineRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(FluidLineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		FluidStack flowing = be.flowing();
		Fitting fitting = be.fitting();
		if (be.encasing() != null || (flowing.isEmpty() && fitting == Fitting.NONE) || (fitting == Fitting.INTAKE && flowing.isEmpty() && be.filter().isEmpty()))
			return;
		BlockState state = be.getBlockState();
		VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
		// the game time whole (the fill and drain are timed by it), and the slide, which only moves while the fluid does
		float time = (be.getLevel() == null ? 0 : be.getLevel().getGameTime()) + partialTicks;
		float phase = be.slid(time);
		float progress = be.progress(time);
		if (!flowing.isEmpty()) {
			Sprite fluid = Sprite.of(flowing);
			float far = state.getValue(FluidLineBlock.HUB) ? SLOT_FAR_HUB : SLOT_FAR;
			for (Direction d : Direction.Plane.HORIZONTAL) {
				LineSide side = FluidLineBlock.side(state, d);
				if (side == LineSide.NONE)
					continue;
				// an Intake's collar covers the start of its slot, and an in-line fitting the middle of a straight piece
				float near = side == LineSide.INTAKE ? SLOT_NEAR_INTAKE : SLOT_NEAR;
				float slotFar = fitting != Fitting.NONE && fitting != Fitting.INTAKE ? Math.min(far, 4.75f) : far;
				// the part of the slot the fluid has reached (filling) or not yet left (draining): the way in is the first
				// half of its way through the block, from the face to the middle, the way out the second, middle to face
				boolean out = be.flowsOut(d);
				float len = slotFar - near, k = Mth.clamp(out ? progress * 2 - 1 : progress * 2, 0, 1);
				float a = near, b = slotFar;
				if (be.draining()) {
					if (out)
						b = slotFar - len * k;
					else
						a = near + len * k;
				} else if (out)
					a = slotFar - len * k;
				else
					b = near + len * k;
				if (b - a < 0.05f)
					continue;
				ms.pushPose();
				turn(ms, d);
				strip(vc, ms, fluid, SLOT_X0, SLOT_X1, SLOT_Y, a, b, out ? phase : -phase, light, overlay);
				ms.popPose();
			}
		}
		if (fitting == Fitting.INTAKE) {
			// its window shows the fluid it's drawing, running in from the tank (when it moves); idle, the fluid it's set to
			FluidStack shown = flowing.isEmpty() ? be.filter() : flowing;
			if (!shown.isEmpty()) {
				ms.pushPose();
				turn(ms, be.fittingSide());
				// under the Intake's mesh (y 11.5), over its collar (y 11)
				strip(vc, ms, Sprite.of(shown), 7f / 16, 9f / 16, 11.25f / 16, 1.75f, 4.25f, -phase, light, overlay);
				ms.popPose();
			}
			return;
		}
		// the fluid behind a Filter's window
		if (fitting == Fitting.FILTER && !be.filter().isEmpty()) {
			ms.pushPose();
			alongAxis(ms, be.fittingSide().getAxis());
			Sprite fluid = Sprite.of(be.filter());
			flat(vc, ms.last(), fluid, 6f / 16, 7f / 16, 10f / 16, 9f / 16, 11.25f / 16, light, overlay);
			south(vc, ms.last(), fluid, 6f / 16, 7f / 16, 10f / 16, 9f / 16, 11.25f / 16, light, overlay);
			ms.popPose();
		}
		// with Flywheel on, its visual has the parts (and eases them: only one of the two may)
		if (fitting != Fitting.NONE && !PartSink.instanced(be))
			parts(be, partialTicks, ms, PartSink.screen(buffers, state, overlay), light);
		// the Filter's glass always comes from here, last: after its fluid (drawn by Flywheel it went in first and hid the
		// fluid) and after its can (the cutout and translucent buffers are one, so the order is the order drawn)
		if (fitting == Fitting.FILTER) {
			ms.pushPose();
			alongAxis(ms, be.fittingSide().getAxis());
			PartSink.screen(buffers, state, overlay).part(FILTER_GLASS, ms, light, RenderType.translucent());
			ms.popPose();
		}
	}

	/** An in-line fitting round the middle, turned along the straight piece; its moving part eased to its setting. */
	public static void parts(FluidLineBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		if (be.encasing() != null || be.fitting() == Fitting.NONE || be.fitting() == Fitting.INTAKE)
			return;
		ms.pushPose();
		alongAxis(ms, be.fittingSide().getAxis());
		switch (be.fitting()) {
			case VALVE -> {
				sink.part(VALVE_BODY, ms, light);
				// three quarters of a turn from open to shut
				float target = be.powered() ? 270 : (10 - be.valveOpen()) * 27f;
				float shown = be.easeShown(target, 0.25f);
				rotated(ms, 8, 14.5f, 8, Axis.YP, -shown, () -> sink.part(VALVE_WHEEL, ms, light));
			}
			// its glass is drawn by render(), after the fluid behind it
			case FILTER -> sink.part(FILTER, ms, light);
			case METER -> {
				sink.part(METER, ms, light);
				// the needle swings from the left stop (nothing) to the right (the line's whole rate), and settles
				float target = -90 + 180 * be.meterLevel();
				float shown = be.easeShown(target, 0.12f);
				rotated(ms, 8, 14, 9, Axis.ZP, -shown, () -> sink.part(METER_NEEDLE, ms, light));
			}
			case SPLITTER -> {
				sink.part(SPLITTER, ms, light);
				ModelResourceLocation dial = switch (be.splitMode()) {
					case EVEN -> DIAL_EVEN;
					case NEAREST -> DIAL_NEAREST;
					case ROUND_ROBIN -> DIAL_ROBIN;
				};
				float target = be.splitMode().ordinal() * 120f;
				float shown = be.easeShown(target, 0.3f);
				rotated(ms, 8, 11, 8, Axis.YP, -shown, () -> sink.part(dial, ms, light));
			}
			default -> {}
		}
		// a Valve's or Filter's signal flag: hanging while fluid can pass, swung up level while it holds fluid back
		if (be.fitting() == Fitting.VALVE || be.fitting() == Fitting.FILTER) {
			float up = be.easeFlag(be.holding() ? 90 : 0, 0.2f);
			rotated(ms, 8, 9, 4.75f, Axis.ZP, up, () -> sink.part(FLAG, ms, light));
		}
		ms.popPose();
	}

	private static void rotated(PoseStack ms, float px, float py, float pz, Axis axis, float degrees, Runnable draw) {
		ms.pushPose();
		ms.translate(px / 16, py / 16, pz / 16);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-px / 16, -py / 16, -pz / 16);
		draw.run();
		ms.popPose();
	}

	/** Fittings are authored along x: turned for a straight piece along z, or stood up for one along y. */
	static void alongAxis(PoseStack ms, Direction.Axis axis) {
		if (axis == Direction.Axis.X)
			return;
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(axis == Direction.Axis.Z ? Axis.YP.rotationDegrees(90) : Axis.ZP.rotationDegrees(90));
		ms.translate(-0.5, -0.5, -0.5);
	}

	/** Turns the block about its middle as the blockstate turns a part authored facing north to side d. */
	static void turn(PoseStack ms, Direction d) {
		ms.translate(0.5, 0.5, 0.5);
		switch (d) {
			case EAST -> ms.mulPose(Axis.YP.rotationDegrees(-90));
			case SOUTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
			case WEST -> ms.mulPose(Axis.YP.rotationDegrees(90));
			case UP -> ms.mulPose(Axis.XP.rotationDegrees(90));
			case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(-90));
			default -> {}
		}
		ms.translate(-0.5, -0.5, -0.5);
	}

	/**
	 * A strip of fluid lying flat at y, x0-x1 across and from {@code near} to {@code far} px in from the north face, its
	 * texture slid {@code phase} px towards the face (negative: away from it), split where the texture wraps.
	 */
	private static void strip(VertexConsumer vc, PoseStack ms, Sprite fluid, float x0, float x1, float y, float near, float far,
							  float phase, int light, int overlay) {
		PoseStack.Pose pose = ms.last();
		float v0 = fluid.sprite.getV(x0), v1 = fluid.sprite.getV(x1);
		float z = near;
		while (z < far - 0.001f) {
			float u = (((z + phase) % 16) + 16) % 16;
			float len = Math.min(far - z, 16 - u);
			float za = z / 16, zb = (z + len) / 16;
			float ua = fluid.sprite.getU(u / 16), ub = fluid.sprite.getU((u + len) / 16);
			vertex(vc, pose, x0, y, za, ua, v0, fluid.color, light, overlay, 0, 1, 0);
			vertex(vc, pose, x0, y, zb, ub, v0, fluid.color, light, overlay, 0, 1, 0);
			vertex(vc, pose, x1, y, zb, ub, v1, fluid.color, light, overlay, 0, 1, 0);
			vertex(vc, pose, x1, y, za, ua, v1, fluid.color, light, overlay, 0, 1, 0);
			z += len;
		}
	}

	/** A flat upward quad x0-x1, z0-z1 at y (block units), its texture cut from the same place on the sprite. */
	private static void flat(VertexConsumer vc, PoseStack.Pose pose, Sprite s, float x0, float z0, float x1, float z1, float y,
							 int light, int overlay) {
		float u0 = s.sprite.getU(x0), u1 = s.sprite.getU(x1), v0 = s.sprite.getV(z0), v1 = s.sprite.getV(z1);
		vertex(vc, pose, x0, y, z0, u0, v0, s.color, light, overlay, 0, 1, 0);
		vertex(vc, pose, x0, y, z1, u0, v1, s.color, light, overlay, 0, 1, 0);
		vertex(vc, pose, x1, y, z1, u1, v1, s.color, light, overlay, 0, 1, 0);
		vertex(vc, pose, x1, y, z0, u1, v0, s.color, light, overlay, 0, 1, 0);
	}

	/** A quad facing south, x0-x1, y0-y1 at z. */
	private static void south(VertexConsumer vc, PoseStack.Pose pose, Sprite s, float x0, float y0, float x1, float y1, float z,
							  int light, int overlay) {
		float u0 = s.sprite.getU(x0), u1 = s.sprite.getU(x1), v0 = s.sprite.getV(1 - y1), v1 = s.sprite.getV(1 - y0);
		vertex(vc, pose, x0, y1, z, u0, v0, s.color, light, overlay, 0, 0, 1);
		vertex(vc, pose, x0, y0, z, u0, v1, s.color, light, overlay, 0, 0, 1);
		vertex(vc, pose, x1, y0, z, u1, v1, s.color, light, overlay, 0, 0, 1);
		vertex(vc, pose, x1, y1, z, u1, v0, s.color, light, overlay, 0, 0, 1);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color,
							   int light, int overlay, float nx, float ny, float nz) {
		vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
	}

	/** A fluid's still texture and tint. */
	record Sprite(TextureAtlasSprite sprite, int color) {
		static Sprite of(FluidStack fluid) {
			IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
			TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
			return new Sprite(sprite, ext.getTintColor(fluid) | 0xFF000000);
		}
	}
}
