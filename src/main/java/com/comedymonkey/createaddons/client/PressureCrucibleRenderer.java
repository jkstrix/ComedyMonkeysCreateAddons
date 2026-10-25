package com.comedymonkey.createaddons.client;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.crucible.PressureCrucibleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Draws the moving parts of a Pressure Crucible, a square screw press (design/briefs/pressure-crucible.md): the shaft
 * end turning in the back gearbox, the pinion it drives and the big gear and screw that turn against it, the platen
 * riding down the screw onto the contents (it starts at the top, lowers over exactly the time one item takes, then
 * eases back up), the gauge needle climbing with the platen, the bowl's glow for the burner's heat, and the dust and
 * results in the bowl. Modelled with the back to the south and turned to the block's facing. Kinetic blocks normally
 * get a Flywheel visual; without one Flywheel falls back to this renderer, which also covers Ponder and contraptions.
 */
public class PressureCrucibleRenderer extends SafeBlockEntityRenderer<PressureCrucibleBlockEntity> {
	public static final ModelResourceLocation SCREW = model("block/pressure_crucible_screw");
	public static final ModelResourceLocation PINION = model("block/pressure_crucible_pinion");
	public static final ModelResourceLocation RAM = model("block/pressure_crucible_ram");
	public static final ModelResourceLocation NEEDLE = model("block/pressure_crucible_needle");
	public static final ModelResourceLocation SHAFT = model("block/pressure_crucible_shaft");
	public static final ModelResourceLocation HEAT_SMOULDERING = model("block/pressure_crucible_heat_smouldering");
	public static final ModelResourceLocation HEAT_KINDLED = model("block/pressure_crucible_heat_kindled");
	public static final ModelResourceLocation HEAT_SEETHING = model("block/pressure_crucible_heat_seething");

	/** Underside of the platen at the top and bottom of its travel, in blocks. */
	private static final float PLATE_TOP = 11 / 16f;
	private static final float PLATE_BOTTOM = 4.5f / 16;
	/** The screw's axis, the pinion's axis and the gauge's centre, in the model (pixels, back to the south). */
	private static final float SCREW_X = 8, SCREW_Z = 8, PINION_X = 8, PINION_Z = 13.5f, GAUGE_X = 4.5f, GAUGE_Y = 13.75f;
	/** The big gear's radius over the pinion's: the pinion turns this much faster, the other way. */
	private static final float RATIO = 4.25f / 1.25f;
	/** The needle's angle with the platen at the top and at the bottom of its travel (degrees about the dial's face). */
	private static final float NEEDLE_REST = 135, NEEDLE_SWEEP = -270;

	public PressureCrucibleRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation model(String path) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource(path));
	}

	/** The parts aren't referenced by any blockstate, so they're loaded explicitly. */
	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation part : new ModelResourceLocation[] {SCREW, PINION, RAM, NEEDLE, SHAFT,
			HEAT_SMOULDERING, HEAT_KINDLED, HEAT_SEETHING})
			event.register(part);
	}

	@Override
	protected void renderSafe(PressureCrucibleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);

		// Contents lying in the bowl, under the platen
		ItemStack input = be.getInventory().getStackInSlot(PressureCrucibleBlockEntity.INPUT);
		ItemStack output = be.getInventory().getStackInSlot(PressureCrucibleBlockEntity.OUTPUT);
		int layers = input.isEmpty() ? 0 : Math.min(4, 1 + input.getCount() / 16);
		for (int i = 0; i < layers; i++)
			renderFlat(be, input, ms, buffer, light, overlay, 0.5f + (i % 2 == 0 ? -0.03f : 0.03f),
				0.5f + (i < 2 ? -0.03f : 0.03f), 4.05f / 16 + i * 0.008f, 37 * i);
		if (!output.isEmpty())
			renderFlat(be, output, ms, buffer, light, overlay, 0.5f, 0.5f, 4.05f / 16 + layers * 0.008f + 0.008f, 15);
	}

	/**
	 * The part models: the shaft end, the screw and its gear, the pinion, the platen, the gauge needle and the bowl's
	 * glow (for this renderer, or Flywheel's visual). Cutout, since the gear faces and the dial are round cut-outs.
	 */
	public static void parts(PressureCrucibleBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING))
			return;
		Direction back = state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);

		// Shaft end, turning with the network. The model points south; turn it to the back, then spin it about the
		// world axis exactly as Create spins shafts so it lines up with the shaft it connects to.
		Direction.Axis axis = back.getAxis();
		float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(axis == Direction.Axis.X ? Axis.XP.rotation(angle) : Axis.ZP.rotation(angle));
		ms.mulPose(Axis.YP.rotationDegrees(-back.toYRot()));
		ms.translate(-0.5, -0.5, -0.5);
		sink.part(SHAFT, ms, light);
		ms.popPose();

		// Everything else in the model's frame, turned to the back's direction
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-back.toYRot()));
		ms.translate(-0.5, -0.5, -0.5);

		// The shaft's turn about the model's +Z (out of the back): the world angle runs the other way when the back
		// faces a negative direction
		float spin = back.getAxisDirection() == Direction.AxisDirection.POSITIVE ? angle : -angle;
		// The big gear and the screw turn with the shaft, the pinion that drives them faster and the other way
		spinAbout(ms, sink, SCREW, SCREW_X, SCREW_Z, spin, light);
		spinAbout(ms, sink, PINION, PINION_X, PINION_Z, -RATIO * spin, light);

		// The platen: top of travel when idle, lowering steadily as the current item is pressed
		float plate = be.getPlate(partialTicks);
		ms.pushPose();
		ms.translate(0, PLATE_TOP - plate * (PLATE_TOP - PLATE_BOTTOM), 0);
		sink.part(RAM, ms, light);
		ms.popPose();

		// The gauge needle climbs to the red as the platen comes down
		ms.pushPose();
		ms.translate(GAUGE_X / 16, GAUGE_Y / 16, 0);
		ms.mulPose(Axis.ZP.rotationDegrees(NEEDLE_REST + NEEDLE_SWEEP * plate));
		ms.translate(-GAUGE_X / 16, -GAUGE_Y / 16, 0);
		sink.part(NEEDLE, ms, light);
		ms.popPose();

		// The bowl glows with the burner under it: a faint red when smouldering, full-bright amber or soul blue when lit
		HeatLevel heat = be.getHeatLevel();
		ModelResourceLocation glow = heatModel(heat);
		if (glow != null)
			sink.part(glow, ms, heat.isAtLeast(HeatLevel.KINDLED) ? LightTexture.FULL_BRIGHT : light);
		ms.popPose();
	}

	/** A dying burner still smoulders until it goes out. */
	@Nullable
	private static ModelResourceLocation heatModel(HeatLevel heat) {
		return switch (heat) {
			case SMOULDERING, FADING -> HEAT_SMOULDERING;
			case KINDLED -> HEAT_KINDLED;
			case SEETHING -> HEAT_SEETHING;
			default -> null;
		};
	}

	private static void spinAbout(PoseStack ms, PartSink sink, ModelResourceLocation part, float x, float z, float radians,
								  int light) {
		ms.pushPose();
		ms.translate(x / 16, 0, z / 16);
		ms.mulPose(Axis.YP.rotation(radians));
		ms.translate(-x / 16, 0, -z / 16);
		sink.part(part, ms, light);
		ms.popPose();
	}

	private static void renderFlat(PressureCrucibleBlockEntity be, ItemStack stack, PoseStack ms, MultiBufferSource buffer,
								   int light, int overlay, float x, float z, float y, float spin) {
		ms.pushPose();
		ms.translate(x, y, z);
		ms.mulPose(Axis.YP.rotationDegrees(spin));
		ms.mulPose(Axis.XP.rotationDegrees(90));
		// Small enough to fit under the platen
		ms.scale(0.3f, 0.3f, 0.3f);
		Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms,
			buffer, be.getLevel(), 0);
		ms.popPose();
	}
}
