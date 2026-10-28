package com.comedymonkey.createaddons.client.line;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.line.Fitting;
import com.comedymonkey.createaddons.content.line.PowerLineBlock;
import com.comedymonkey.createaddons.content.line.PowerLineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The fittings clipped onto a Power Line: a Breaker with its lever (snapping over when flipped), a Meter whose needle
 * swings to the flow, a Splitter on a junction whose dial points at the branch served first (or shows the fork when
 * they're all level). They lie on the wire's face across its run. Plain
 * wire draws nothing here. All of it is part models, which Flywheel draws instead when it's on.
 */
public class PowerLineRenderer implements BlockEntityRenderer<PowerLineBlockEntity> {
	public static final ModelResourceLocation BREAKER = part("breaker"), LEVER = part("breaker_lever"), METER = part("wire_meter"),
		NEEDLE = part("wire_meter_needle"), SPLITTER = part("wire_splitter"), DIAL_EVEN = part("wire_splitter_dial_even"),
		DIAL_NEAREST = part("wire_splitter_dial_nearest"), DIAL_ROBIN = part("wire_splitter_dial_robin"), DIAL_FIRST = part("wire_splitter_dial_first"),
		SWITCH = part("light_switch"), SWITCH_TOGGLE = part("light_switch_toggle");

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/line_fitting/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {BREAKER, LEVER, METER, NEEDLE, SPLITTER, DIAL_EVEN, DIAL_NEAREST, DIAL_ROBIN, DIAL_FIRST, SWITCH, SWITCH_TOGGLE})
			event.register(m);
	}

	public PowerLineRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(PowerLineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		if (!PartSink.instanced(be))
			parts(be, partialTicks, ms, PartSink.screen(buffers, be.getBlockState(), overlay), light);
	}

	public static void parts(PowerLineBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		if (be.encasing() != null || be.fitting() == Fitting.NONE)
			return;
		BlockState state = be.getBlockState();
		Direction run = be.fittingSide(), face = null;
		// a Splitter is turned to point at its first branch
		Direction first = be.fitting() == Fitting.SPLITTER ? be.firstBranch() : null;
		if (first != null)
			run = first;
		for (Direction d : Direction.values())
			if (PowerLineBlock.hasFace(state, d) && d.getAxis() != run.getAxis()
				&& (face == null || be.link(d, run).connected() || be.link(d, run.getOpposite()).connected()))
				face = d;
		if (face == null)
			return;
		ms.pushPose();
		lieOn(ms, face, run);
		switch (be.fitting()) {
			case BREAKER -> {
				sink.part(BREAKER, ms, light);
				float target = be.powered() || !be.leverOn() ? 35 : -35;
				float shown = be.easeShown(target, 0.4f);
				rotated(ms, 8, 3, 8, Axis.ZP, shown, () -> sink.part(LEVER, ms, light));
			}
			case METER -> {
				sink.part(METER, ms, light);
				float target = -90 + 180 * be.meterLevel();
				float shown = be.easeShown(target, 0.12f);
				rotated(ms, 8, 1.75f, 8, Axis.YP, -shown, () -> sink.part(NEEDLE, ms, light));
			}
			case SWITCH -> {
				// the Light Switch's plate over the wire; its toggle tilts along the run, snapping over to the other side
				sink.part(SWITCH, ms, light);
				float shown = be.easeShown(be.leverOn() ? -30 : 30, 0.6f);
				rotated(ms, 8, 2, 8, Axis.ZP, shown, () -> sink.part(SWITCH_TOGGLE, ms, light));
			}
			case SPLITTER -> {
				sink.part(SPLITTER, ms, light);
				sink.part(first != null ? DIAL_FIRST : DIAL_EVEN, ms, light);
			}
			default -> {}
		}
		ms.popPose();
	}

	/** Parts authored on the floor across a wire running along x, laid on {@code face} with the wire running {@code run}. */
	private static void lieOn(PoseStack ms, Direction face, Direction run) {
		Vector3f f = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
		Vector3f r = new Vector3f(run.getStepX(), run.getStepY(), run.getStepZ());
		Vector3f z = new Vector3f(f).cross(r);
		Matrix3f m = new Matrix3f(r.x, r.y, r.z, -f.x, -f.y, -f.z, z.x, z.y, z.z);
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(new Quaternionf().setFromNormalized(m));
		ms.translate(-0.5, -0.5, -0.5);
	}

	private static void rotated(PoseStack ms, float px, float py, float pz, Axis axis, float degrees, Runnable draw) {
		ms.pushPose();
		ms.translate(px / 16, py / 16, pz / 16);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-px / 16, -py / 16, -pz / 16);
		draw.run();
		ms.popPose();
	}
}
