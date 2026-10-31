package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.tube.ClockworkInletBlock;
import com.comedymonkey.createaddons.content.tube.ClockworkInletBlockEntity;
import com.comedymonkey.createaddons.content.tube.ClockworkReceiverBlock;
import com.comedymonkey.createaddons.content.tube.ClockworkReceiverBlockEntity;
import com.comedymonkey.createaddons.content.tube.ClockworkTubeBlockEntity;
import com.comedymonkey.createaddons.content.tube.TubeLoad;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The Clockwork Tubes' moving parts (design/briefs/clockwork-tubes.md, Motion): the Receiver's ring gear turning with
 * its network, meshed tooth-in-gap with Create's cogs; items passing through the middle of it; the Inlets' gulping
 * flaps; and each tube's item gliding past its glass.
 */
public final class TubeRenderers {
	public static final ModelResourceLocation RECEIVER_COG = model("clockwork_receiver_cog");
	public static final ModelResourceLocation INLET_FLAP = model("clockwork_inlet_flap");

	private TubeRenderers() {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		event.register(RECEIVER_COG);
		event.register(INLET_FLAP);
	}

	/** Turns the pose as the blockstate turns the block model ({@code x} then {@code y}, in degrees), about its centre. */
	private static void orient(PoseStack ms, int x, int y) {
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-y));
		ms.mulPose(Axis.XP.rotationDegrees(-x));
		ms.translate(-0.5, -0.5, -0.5);
	}

	/** The Receiver's model is built with its output up; its blockstate's turn for each facing (the exporter's). */
	static void orientReceiver(PoseStack ms, Direction facing) {
		switch (facing) {
			case UP -> {}
			case DOWN -> orient(ms, 180, 0);
			case NORTH -> orient(ms, 90, 0);
			case SOUTH -> orient(ms, 270, 0);
			case EAST -> orient(ms, 90, 90);
			case WEST -> orient(ms, 90, 270);
		}
	}

	/** The Inlets' model is built with the mouth down. */
	static void orientInlet(PoseStack ms, Direction facing) {
		switch (facing) {
			case DOWN -> {}
			case UP -> orient(ms, 180, 0);
			case NORTH -> orient(ms, 270, 0);
			case SOUTH -> orient(ms, 90, 0);
			case EAST -> orient(ms, 270, 90);
			case WEST -> orient(ms, 270, 270);
		}
	}

	private static double now(Level level) {
		return level == null ? 0 : AnimationTickHolder.getTicks(level) + AnimationTickHolder.getPartialTicks(level);
	}

	private static void item(ItemStack stack, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Level level, int seed) {
		Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, level, seed);
	}

	// ---------- Receiver ----------

	public static class Receiver extends SafeBlockEntityRenderer<ClockworkReceiverBlockEntity> {
		public Receiver(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(ClockworkReceiverBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
								  int overlay) {
			parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
			ItemStack stack = be.getThroat();
			if (stack.isEmpty() || be.getLevel() == null)
				return;
			// its load rising through the throat, through the hole in the cog, and out of the mouth (stamped with the game time)
			float f = be.throatProgress(be.getLevel().getGameTime() + partialTicks);
			ms.pushPose();
			orientReceiver(ms, be.getBlockState().getValue(ClockworkReceiverBlock.FACING));
			ms.translate(0.5, f, 0.5);
			ms.mulPose(Axis.YP.rotationDegrees(f * 180));
			ms.scale(0.375f, 0.375f, 0.375f);
			item(stack, ms, buffer, light, overlay, be.getLevel(), (int) be.getBlockPos().asLong());
			ms.popPose();
		}

		/** The ring gear, turning about its output's axis as Create turns a cogwheel there (its mesh offset included). */
		public static void parts(ClockworkReceiverBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
			Direction facing = be.getBlockState().getValue(ClockworkReceiverBlock.FACING);
			float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), facing.getAxis());
			ms.pushPose();
			orientReceiver(ms, facing);
			// the model's up is the output side: for an output on the negative side of its axis, that's the other way round
			ms.translate(0.5, 0.5, 0.5);
			ms.mulPose(Axis.YP.rotation(facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? angle : -angle));
			ms.translate(-0.5, -0.5, -0.5);
			sink.part(RECEIVER_COG, ms, light);
			ms.popPose();
		}
	}

	// ---------- Inlet ----------

	public static class Inlet extends SafeBlockEntityRenderer<ClockworkInletBlockEntity> {
		public Inlet(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(ClockworkInletBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
								  int overlay) {
			parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
		}

		/** The flap: Trade Bot's lid plate across the mouth, hinged at its north edge, gulping inward. */
		public static void parts(ClockworkInletBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
			double seconds = (now(be.getLevel()) - be.gulpAt) / 20;
			ms.pushPose();
			orientInlet(ms, be.getBlockState().getValue(ClockworkInletBlock.FACING));
			ms.translate(8 / 16f, 1.25f / 16f, 3.5f / 16f);
			// raising it into the hood is a turn the negative way about X
			ms.mulPose(Axis.XP.rotationDegrees(-gulp(seconds)));
			ms.translate(-8 / 16f, -1.25f / 16f, -3.5f / 16f);
			sink.part(INLET_FLAP, ms, light);
			ms.popPose();
		}

		private static final double[][] GULP = {{0, 0}, {0.05, -5}, {0.13, 50}, {0.23, -8}, {0.3, 0}};

		/** The gulp, in degrees into the hood: puff out 5°, snap in 50°, swing back past closed by 8°, settle (0.3 s). */
		static float gulp(double s) {
			if (s <= 0 || s >= GULP[GULP.length - 1][0])
				return 0;
			for (int i = 1; i < GULP.length; i++)
				if (s <= GULP[i][0]) {
					double f = (s - GULP[i - 1][0]) / (GULP[i][0] - GULP[i - 1][0]);
					f = f * f * (3 - 2 * f);
					return (float) (GULP[i - 1][1] + (GULP[i][1] - GULP[i - 1][1]) * f);
				}
			return 0;
		}
	}

	// ---------- Tube ----------

	public static class Tube extends SafeBlockEntityRenderer<ClockworkTubeBlockEntity> {
		public Tube(BlockEntityRendererProvider.Context context) {}

		/** Its item, from the face it came in by, through the middle, to the face it leaves by, turning slowly as it goes. */
		@Override
		protected void renderSafe(ClockworkTubeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
								  int overlay) {
			TubeLoad load = be.getLoad();
			if (load == null)
				return;
			// the server stamps loads with the game time, so it's the clock here too
			float p = be.getLevel() == null ? load.progress : load.progressAt(be.getLevel().getGameTime() + partialTicks);
			Vec3 in = Vec3.atLowerCornerOf(load.from.getNormal()).scale(0.5), out = Vec3.atLowerCornerOf(load.to.getNormal()).scale(0.5);
			Vec3 at = p < 0.5 ? in.scale(1 - p * 2) : out.scale((p - 0.5) * 2);
			ms.pushPose();
			ms.translate(0.5 + at.x, 0.5 + at.y, 0.5 + at.z);
			ms.mulPose(Axis.YP.rotationDegrees(p * 90 + (be.getBlockPos().hashCode() & 63)));
			ms.scale(0.375f, 0.375f, 0.375f);
			item(load.stack, ms, buffer, light, overlay, be.getLevel(), (int) be.getBlockPos().asLong());
			ms.popPose();
		}
	}
}
