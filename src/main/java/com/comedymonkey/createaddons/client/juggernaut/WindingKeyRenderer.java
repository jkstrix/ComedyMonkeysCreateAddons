package com.comedymonkey.createaddons.client.juggernaut;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlock;
import com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlockEntity;
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
 * The Winding Key's moving parts: the key, turning with the shaft in its hub, and the ring of runes on its plate, which
 * lights frost-blue once it has some wind in it and flickers as it nears full.
 */
public class WindingKeyRenderer extends SafeBlockEntityRenderer<WindingKeyBlockEntity> {
	public static final ModelResourceLocation KEY = part("winding_key_key");
	public static final ModelResourceLocation RUNES = part("winding_key_runes");

	public WindingKeyRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	/** The key and the runes aren't referenced by any blockstate, so they're loaded explicitly. */
	public static void registerModels(ModelEvent.RegisterAdditional event) {
		event.register(KEY);
		event.register(RUNES);
	}

	@Override
	protected void renderSafe(WindingKeyBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
							  int overlay) {
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
	}

	/** The key and the lit runes are its part models (for this renderer, or Flywheel's visual). */
	public static void parts(WindingKeyBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		Direction facing = be.getBlockState().getValue(WindingKeyBlock.FACING);
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
		sink.part(KEY, ms, light);
		ms.popPose();
		float wind = be.wind();
		long time = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
		// lit from a tenth wound; near full, it flickers like the preview's
		if (wind > 0.1f && (wind < 0.9f || time / 3 % 2 == 0)) {
			ms.pushPose();
			face(ms, facing);
			sink.part(RUNES, ms, LightTexture.FULL_BRIGHT);
			ms.popPose();
		}
	}

	/** Turns the north-facing model to face {@code facing}. */
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
}
