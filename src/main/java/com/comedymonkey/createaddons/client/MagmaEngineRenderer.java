package com.comedymonkey.createaddons.client;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The Magma Engine's moving parts, all driven by the shaft's angle so they speed up and stop with it: the shaft stub and
 * the flywheel turning on it, the two pistons pumping in and out of their cylinders half a turn apart, and on the
 * Supercharged engine the supercharger's pulley at twice the shaft's speed. Modelled with the shaft to the north, and
 * turned to the engine's facing by {@link MagmaEngineBlock#modelToWorld}.
 */
public class MagmaEngineRenderer extends SafeBlockEntityRenderer<MagmaEngineBlockEntity> {
	public static final Parts STANDARD = new Parts("magma_engine", false);
	public static final Parts SUPERCHARGED = new Parts("supercharged_magma_engine", true);
	/** How far a piston slides along its cylinder, in blocks. */
	private static final float STROKE = 1 / 16f;
	/** Each cylinder leans out 45 degrees from the shaft: up-left and up-right in the model. */
	private static final float LEAN = Mth.SQRT_OF_TWO / 2;
	/** The supercharger pulley's axis, in the model (pixels). */
	private static final float BLOWER_X = 8, BLOWER_Y = 14.6f;

	/** One engine's moving parts, named {@code <prefix>_flywheel} and so on. */
	public record Parts(ModelResourceLocation flywheel, ModelResourceLocation pistonLeft, ModelResourceLocation pistonRight,
						@Nullable ModelResourceLocation blowerPulley) {
		Parts(String prefix, boolean supercharged) {
			this(part(prefix + "_flywheel"), part(prefix + "_piston_left"), part(prefix + "_piston_right"),
				supercharged ? part(prefix + "_blower_pulley") : null);
		}

		void register(ModelEvent.RegisterAdditional event) {
			event.register(flywheel);
			event.register(pistonLeft);
			event.register(pistonRight);
			if (blowerPulley != null)
				event.register(blowerPulley);
		}
	}

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		STANDARD.register(event);
		SUPERCHARGED.register(event);
	}

	public MagmaEngineRenderer(BlockEntityRendererProvider.Context context) {}

	/**
	 * How far each piston is out of the drum, 0 to 1, at this shaft angle (radians): the left at the top of its stroke
	 * at half a turn, the right at a whole turn.
	 */
	public static float pistonTravel(float angle, boolean left) {
		return 0.5f * (1 - Mth.cos(angle + (left ? 0 : Mth.PI)));
	}

	@Override
	protected void renderSafe(MagmaEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(MagmaEngineBlock.FACING))
			return;
		Direction facing = state.getValue(MagmaEngineBlock.FACING);
		float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), facing.getAxis());
		// The model's +Z runs out of the back; the world angle turns about the axis' positive direction
		float spin = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? -angle : angle;
		Parts parts = be.isSupercharged() ? SUPERCHARGED : STANDARD;
		// One buffer for everything: asking for another render type would finish this one mid-draw
		VertexConsumer vc = buffer.getBuffer(RenderType.solid());

		ms.pushPose();
		ms.mulPose(MagmaEngineBlock.modelToWorld(facing));

		// The shaft stub and the flywheel, turning together about the shaft
		ms.pushPose();
		ms.translate(0.5f, 0.5f, 0);
		ms.mulPose(Axis.ZP.rotation(spin));
		ms.translate(-0.5f, -0.5f, 0);
		render(ms, vc, state, CobbleGeneratorRenderer.SHAFT, light, overlay);
		render(ms, vc, state, parts.flywheel(), light, overlay);
		ms.popPose();

		// The pistons, each sliding along its leaning cylinder
		for (int side = 0; side < 2; side++) {
			boolean left = side == 0;
			float out = STROKE * pistonTravel(angle, left);
			ms.pushPose();
			ms.translate((left ? -LEAN : LEAN) * out, LEAN * out, 0);
			render(ms, vc, state, left ? parts.pistonLeft() : parts.pistonRight(), light, overlay);
			ms.popPose();
		}

		// The supercharger's pulley, belt-driven at twice the shaft's speed
		if (parts.blowerPulley() != null) {
			ms.pushPose();
			ms.translate(BLOWER_X / 16, BLOWER_Y / 16, 0);
			ms.mulPose(Axis.ZP.rotation(2 * spin));
			ms.translate(-BLOWER_X / 16, -BLOWER_Y / 16, 0);
			render(ms, vc, state, parts.blowerPulley(), light, overlay);
			ms.popPose();
		}
		ms.popPose();
	}

	private static void render(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation location,
							   int light, int overlay) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), vc, state, model,
			1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.solid());
	}
}
