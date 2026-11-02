package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlock;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** The moving and whole-machine parts of the Electric Motor, Lava Generator and Geothermal Turbine. */
public final class ElectricRenderers {
	public static final ModelResourceLocation GENERATOR_ROTOR = model("lava_generator_rotor");
	public static final ModelResourceLocation DYNAMO_AERIAL = model("clockwork_dynamo_aerial");
	public static final ModelResourceLocation DYNAMO_COMMUTATOR = model("clockwork_dynamo_commutator");
	public static final ModelResourceLocation TURBINE = model("geothermal_turbine");
	public static final ModelResourceLocation TURBINE_GLOW = model("geothermal_turbine_glow");
	public static final ModelResourceLocation GAUGE_LAVA = model("geothermal_turbine_gauge_lava");
	public static final ModelResourceLocation GAUGE_WATER = model("geothermal_turbine_gauge_water");
	public static final ModelResourceLocation TURBINE_ROTOR = model("geothermal_turbine_rotor");
	public static final ModelResourceLocation NEEDLE_ENERGY = model("geothermal_turbine_needle_energy");
	public static final ModelResourceLocation NEEDLE_STEAM = model("geothermal_turbine_needle_steam");
	public static final ModelResourceLocation LAMP_OUTPUT = model("geothermal_turbine_lamp_output");
	public static final ModelResourceLocation LAMP_STALL = model("geothermal_turbine_lamp_stall");

	private ElectricRenderers() {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {GENERATOR_ROTOR, DYNAMO_AERIAL, DYNAMO_COMMUTATOR, TURBINE, TURBINE_GLOW, GAUGE_LAVA, GAUGE_WATER,
			TURBINE_ROTOR, NEEDLE_ENERGY, NEEDLE_STEAM, LAMP_OUTPUT, LAMP_STALL})
			event.register(m);
	}

	/** Turns a model made facing north to face {@code facing}, about the block's centre. */
	private static void face(PoseStack ms, Direction facing) {
		ms.translate(0.5, 0.5, 0.5);
		if (facing == Direction.UP)
			ms.mulPose(Axis.XP.rotationDegrees(90));
		else if (facing == Direction.DOWN)
			ms.mulPose(Axis.XP.rotationDegrees(-90));
		else
			ms.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
	}

	/** The motor's shaft end, turning with the network. */
	public static class Motor extends SafeBlockEntityRenderer<ElectricMotorBlockEntity> {
		public Motor(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(ElectricMotorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
								  int light, int overlay) {
			parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
		}

		/** The shaft end is its only part model (for this renderer, or Flywheel's visual). */
		public static void parts(ElectricMotorBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
			BlockState state = be.getBlockState();
			Direction facing = state.getValue(ElectricMotorBlock.FACING);
			Direction.Axis axis = facing.getAxis();
			float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
			ms.pushPose();
			ms.translate(0.5, 0.5, 0.5);
			ms.mulPose(switch (axis) {
				case X -> Axis.XP.rotation(angle);
				case Y -> Axis.YP.rotation(angle);
				case Z -> Axis.ZP.rotation(angle);
			});
			ms.translate(-0.5, -0.5, -0.5);
			face(ms, facing);
			sink.part(CobbleGeneratorRenderer.SHAFT, ms, light);
			ms.popPose();
		}
	}

	/**
	 * The Clockwork Dynamo's moving parts: its shaft end, the commutator in its top window turning with the shaft (on its
	 * own axis above the shaft's, at the shaft's speed), and the aerial springing up, folding and wobbling (see {@link
	 * com.comedymonkey.createaddons.content.electric.DynamoAerial}).
	 */
	public static class Dynamo extends SafeBlockEntityRenderer<com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity> {
		public Dynamo(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity be, float partialTicks, PoseStack ms,
								  MultiBufferSource buffer, int light, int overlay) {
			parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
		}

		public static void parts(com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity be, float partialTicks, PoseStack ms,
								 PartSink sink, int light) {
			Direction facing = be.getBlockState().getValue(com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlock.FACING);
			Direction.Axis axis = facing.getAxis();
			float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
			// the shaft end, as the motor's
			ms.pushPose();
			ms.translate(0.5, 0.5, 0.5);
			ms.mulPose(switch (axis) {
				case X -> Axis.XP.rotation(angle);
				case Y -> Axis.YP.rotation(angle);
				case Z -> Axis.ZP.rotation(angle);
			});
			ms.translate(-0.5, -0.5, -0.5);
			face(ms, facing);
			sink.part(CobbleGeneratorRenderer.SHAFT, ms, light);
			ms.popPose();

			ms.pushPose();
			face(ms, facing);
			// the commutator turns about the model's Z, which faces away from the shaft's end: the same turn as the
			// shaft's in the world, so for a shaft pointing the positive way along its axis it's the other way round
			ms.pushPose();
			ms.translate(8 / 16f, 11.25f / 16f, 0);
			ms.mulPose(Axis.ZP.rotation(commutatorAngle(angle, facing)));
			ms.translate(-8 / 16f, -11.25f / 16f, 0);
			sink.part(DYNAMO_COMMUTATOR, ms, light);
			ms.popPose();
			// the aerial, hinged at the top of its collar
			// the animation clock (it runs in Ponder too); until the block entity has ticked, the block's state shows the way
			double time = be.getLevel() == null ? 0 : net.createmod.catnip.animation.AnimationTickHolder.getTicks(be.getLevel())
				+ net.createmod.catnip.animation.AnimationTickHolder.getPartialTicks(be.getLevel());
			boolean shown = be.getBlockState().getValue(com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlock.WORKING);
			ms.translate(11.75f / 16f, 14 / 16f, 11.5f / 16f);
			ms.mulPose(Axis.XP.rotationDegrees(be.aerial.pitch(time, shown)));
			ms.mulPose(Axis.ZP.rotationDegrees(be.aerial.roll(time)));
			ms.translate(-11.75f / 16f, -14 / 16f, -11.5f / 16f);
			sink.part(DYNAMO_AERIAL, ms, light);
			ms.popPose();
		}

		/** The commutator's turn about the model's Z for a shaft turned {@code angle} about its world axis. */
		static float commutatorAngle(float angle, Direction facing) {
			return facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? -angle : angle;
		}
	}

	/** The generator's turbine wheel, spinning behind its intake glass while it burns. */
	public static class Generator extends SafeBlockEntityRenderer<LavaGeneratorBlockEntity> {
		public Generator(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(LavaGeneratorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
								  int light, int overlay) {
			parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
		}

		/** The turbine wheel is its only part model (for this renderer, or Flywheel's visual). */
		public static void parts(LavaGeneratorBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
			BlockState state = be.getBlockState();
			ms.pushPose();
			face(ms, state.getValue(LavaGeneratorBlock.FACING));
			ms.translate(8 / 16f, 11.5 / 16f, 4 / 16f);
			ms.mulPose(Axis.ZP.rotationDegrees(be.getRotorAngle(partialTicks)));
			ms.translate(-8 / 16f, -11.5 / 16f, -4 / 16f);
			sink.part(GENERATOR_ROTOR, ms, light);
			ms.popPose();
		}
	}

	/**
	 * The whole formed turbine, drawn round its middle block: the rotor turning behind its window, the firebox glow while
	 * it runs, the lava and water gauges, the energy and steam dials, and the output and stall lamps. Pivots are in the
	 * formed model's pixels (design/models/geothermal-turbine.gen.js).
	 */
	public static class Turbine extends SafeBlockEntityRenderer<GeothermalControllerBlockEntity> {
		/** The rotor turns about X through (y 20, z 2). */
		private static final float ROTOR_Y = 20, ROTOR_Z = 2;
		/** The energy dial on the panel (turns about Z) and the steam dial on the boiler's west end (turns about X). */
		private static final float ENERGY_X = 2, ENERGY_Y = 1, ENERGY_Z = -15.75f, STEAM_X = -13.25f, STEAM_Y = 9, STEAM_Z = 18;
		/** The gauge windows' foot: the fills grow up from here. */
		private static final float GAUGE_FOOT = -3;

		public Turbine(BlockEntityRendererProvider.Context context) {}

		/**
		 * The controller draws the whole turbine, whose other blocks draw nothing, so it's drawn even when the
		 * controller's own section is out of sight while the rest of the turbine is in view.
		 */
		@Override
		public boolean shouldRenderOffScreen(GeothermalControllerBlockEntity be) {
			return true;
		}

		@Override
		public int getViewDistance() {
			return 96;
		}

		@Override
		protected void renderSafe(GeothermalControllerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
								  int light, int overlay) {
			parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
		}

		/** Everything it draws is a part model (for this renderer, or Flywheel's visual). */
		public static void parts(GeothermalControllerBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
			BlockState state = be.getBlockState();
			if (!state.getValue(GeothermalControllerBlock.FORMED) || be.getLevel() == null)
				return;
			Direction facing = state.getValue(GeothermalControllerBlock.FACING);
			// Lit like the air in front of the panel, so the big model isn't dark from sampling its own inside
			int front = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing));
			ms.pushPose();
			Direction back = facing.getOpposite();
			ms.translate(back.getStepX(), 0, back.getStepZ());
			face(ms, facing);
			boolean lit = state.getValue(GeothermalControllerBlock.LIT);
			sink.part(TURBINE, ms, front);
			// The firebox only glows while it burns; off, the grates are dark iron
			if (lit)
				sink.part(TURBINE_GLOW, ms, LightTexture.FULL_BRIGHT);
			turned(ms, sink, TURBINE_ROTOR, front, Axis.XP, -be.rotorAngle(partialTicks), 0, ROTOR_Y, ROTOR_Z);
			// Energy dial sweeps clockwise from lower left (empty) to lower right (full); steam follows the load
			turned(ms, sink, NEEDLE_ENERGY, front, Axis.ZP, -135 + 270 * be.energyFill(), ENERGY_X, ENERGY_Y, ENERGY_Z);
			turned(ms, sink, NEEDLE_STEAM, front, Axis.XP, 70 - 140 * be.load() * Math.min(1, be.rotorSpeed() * 4), STEAM_X, STEAM_Y, STEAM_Z);
			if (lit)
				sink.part(LAMP_OUTPUT, ms, LightTexture.FULL_BRIGHT);
			if (be.isStalled())
				sink.part(LAMP_STALL, ms, LightTexture.FULL_BRIGHT);
			gauge(ms, sink, GAUGE_LAVA, be.lavaFill());
			gauge(ms, sink, GAUGE_WATER, be.waterFill());
			ms.popPose();
		}

		/** A part turned by {@code degrees} about {@code axis} through a pivot given in model pixels. */
		private static void turned(PoseStack ms, PartSink sink, ModelResourceLocation model, int light,
								   Axis axis, float degrees, float x, float y, float z) {
			ms.pushPose();
			ms.translate(x / 16, y / 16, z / 16);
			ms.mulPose(axis.rotationDegrees(degrees));
			ms.translate(-x / 16, -y / 16, -z / 16);
			sink.part(model, ms, light);
			ms.popPose();
		}

		/** A gauge's fill, from the foot of its window up to its level. */
		private static void gauge(PoseStack ms, PartSink sink, ModelResourceLocation model, float fill) {
			if (fill <= 0.005f)
				return;
			ms.pushPose();
			ms.translate(0, GAUGE_FOOT / 16, 0);
			ms.scale(1, Math.min(1, fill), 1);
			ms.translate(0, -GAUGE_FOOT / 16, 0);
			sink.part(model, ms, LightTexture.FULL_BRIGHT);
			ms.popPose();
		}
	}
}
