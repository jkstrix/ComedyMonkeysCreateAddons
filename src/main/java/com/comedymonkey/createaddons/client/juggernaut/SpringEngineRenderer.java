package com.comedymonkey.createaddons.client.juggernaut;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlock;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The Spring Engine's moving parts (the approved props' renderer parts): the coil seen through each end's porthole,
 * packed tighter round the shaft the more it's wound; the rune band along the top, lit in four levels; on the winding
 * end the ratchet turning with the shaft in and its pawl clicking over the teeth; on the output end the escape wheel
 * ticking round and the anchor rocking while it gives.
 */
public class SpringEngineRenderer extends SafeBlockEntityRenderer<SpringEngineBlockEntity> {
	private static final ModelResourceLocation[] COIL_FRONT = new ModelResourceLocation[4], COIL_BACK = new ModelResourceLocation[4],
		RUNES = new ModelResourceLocation[4];
	private static final ModelResourceLocation ESCAPE_WHEEL = part("spring_engine_escape_wheel"), ANCHOR = part("spring_engine_anchor"),
		RATCHET = part("spring_engine_ratchet"), PAWL = part("spring_engine_pawl"), SHAFT_FRONT = part("spring_engine_shaft_front"),
		SHAFT_BACK = part("spring_engine_shaft_back");

	static {
		for (int k = 0; k < 4; k++) {
			COIL_FRONT[k] = part("spring_engine_coil_front_" + k);
			COIL_BACK[k] = part("spring_engine_coil_back_" + k);
			RUNES[k] = part("spring_engine_runes_" + (k + 1));
		}
	}

	public SpringEngineRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	/** None of the parts is referenced by a blockstate, so they're loaded explicitly. */
	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (int k = 0; k < 4; k++) {
			event.register(COIL_FRONT[k]);
			event.register(COIL_BACK[k]);
			event.register(RUNES[k]);
		}
		event.register(ESCAPE_WHEEL);
		event.register(ANCHOR);
		event.register(RATCHET);
		event.register(PAWL);
		event.register(SHAFT_FRONT);
		event.register(SHAFT_BACK);
	}

	@Override
	protected void renderSafe(SpringEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
							  int overlay) {
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
	}

	/** Its part models (for this renderer, or Flywheel's visual), posed in the block's north-facing model space. */
	public static void parts(SpringEngineBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		boolean output = SpringEngineBlock.isOutput(be.getBlockState());
		Direction facing = be.facing();
		float fill = be.shownFill();
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		// the coil frame by wind (0 run down .. 3 full); the runes lit to the nearest quarter
		sink.part((output ? COIL_FRONT : COIL_BACK)[Math.min(3, (int) (fill * 4))], ms, light);
		int lit = Math.round(fill * 4);
		if (lit > 0)
			sink.part(RUNES[lit - 1], ms, LightTexture.FULL_BRIGHT);
		// its shaft out of the end, turning with that end, while nothing's fitted there (a shaft or a machine shows its own)
		Direction shaftFace = output ? facing : facing.getOpposite();
		if (be.getLevel() != null && be.getLevel().getBlockState(be.getBlockPos().relative(shaftFace)).isAir()) {
			float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), facing.getAxis());
			ms.pushPose();
			ms.translate(0.5, 0.5, 0);
			ms.mulPose(Axis.ZP.rotation(angle));
			ms.translate(-0.5, -0.5, 0);
			sink.part(output ? SHAFT_FRONT : SHAFT_BACK, ms, light);
			ms.popPose();
		}
		float time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTicks;
		if (output) {
			// the escapement: the wheel steps a tooth (30°) four times a second while it gives, the anchor rocking with it
			boolean giving = be.getSpeed() != 0;
			int beat = giving ? (int) (time / 5) : 0;
			wheel(ms, sink, ESCAPE_WHEEL, 15.75f, 9, 8, beat * 30, light);
			wheel(ms, sink, ANCHOR, 17.25f, 14, 8, giving ? (beat % 2 == 0 ? 12 : -12) : 0, light);
		} else {
			// the ratchet turns with the shaft in; the pawl lifts over each tooth (twelve a turn) and drops
			float angle = (float) Math.toDegrees(KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), facing.getAxis()));
			float tooth = ((angle % 30) + 30) % 30 / 30f;
			wheel(ms, sink, RATCHET, 15.75f, 9, 8, -angle, light);
			wheel(ms, sink, PAWL, 17.25f, 14, 2.25f, be.getSpeed() != 0 ? 8 * (1 - tooth) : 0, light);
		}
		ms.popPose();
	}

	/** A part turned {@code degrees} about the x axis through its pivot (pixels, model space). */
	private static void wheel(PoseStack ms, PartSink sink, ModelResourceLocation part, float px, float py, float pz, float degrees,
							  int light) {
		ms.pushPose();
		ms.translate(px / 16, py / 16, pz / 16);
		ms.mulPose(Axis.XP.rotationDegrees(degrees));
		ms.translate(-px / 16, -py / 16, -pz / 16);
		sink.part(part, ms, light);
		ms.popPose();
	}
}
