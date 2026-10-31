package com.comedymonkey.createaddons.client.wastes;

import org.joml.FrustumIntersection;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.MachineVisuals;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.client.parts.PartsVisual;
import com.comedymonkey.createaddons.content.wastes.LiftBlockEntity;
import com.comedymonkey.createaddons.content.wastes.LiftCallPlateBlock;
import com.comedymonkey.createaddons.content.wastes.LiftCallPlateBlockEntity;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The city's Lifts, drawn (design/models/clockwork-wastes-lift.gen.js, the approved preview; D40). The drive draws the
 * whole lift as part models in its corner's frame (the shaft from it toward its facing's clockwise side and away from
 * its front, turned as the blockstates turn their models): the head on the rails' tops, one level of the four guide
 * rails per block down to the shaft's floor, and the deck where its drive says it is this frame. Its seams, the deck's
 * edge groove, the control post's lens and the clamps' pips are lit full-bright once the district wakes (the preview's
 * awake clip), dark before. Flywheel draws them (the deck's parts move on the GPU); the renderer draws them only when
 * Flywheel can't. The deck entity has no renderer of its own. A call plate's lens is lit only while its call waits (D62).
 */
public final class LiftRenderers {
	private LiftRenderers() {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/wastes/" + name));
	}

	static final ModelResourceLocation DECK_W = model("lift_deck_w"), DECK_E = model("lift_deck_e"),
		DECK_GLOW_W = model("lift_deck_glow_w"), DECK_GLOW_E = model("lift_deck_glow_e"),
		HEAD_NW = model("lift_head_nw"), HEAD_NE = model("lift_head_ne"), HEAD_SW = model("lift_head_sw"), HEAD_SE = model("lift_head_se"),
		GUIDES_W = model("lift_guides_w"), GUIDES_E = model("lift_guides_e"),
		SEAMS_W = model("lift_guide_seams_w"), SEAMS_E = model("lift_guide_seams_e"), PLATE_LENS = model("lift_call_plate_lens");

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {DECK_W, DECK_E, DECK_GLOW_W, DECK_GLOW_E, HEAD_NW, HEAD_NE, HEAD_SW, HEAD_SE,
			GUIDES_W, GUIDES_E, SEAMS_W, SEAMS_E, PLATE_LENS})
			event.register(m);
	}

	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModWastes.LIFT_BE.get(), c -> new LiftRenderer());
		event.registerBlockEntityRenderer(ModWastes.LIFT_CALL_PLATE_BE.get(), c -> new WastesRenderers.Parts<>(LiftRenderers::plate));
		event.registerEntityRenderer(ModWastes.LIFT_DECK.get(), NoopRenderer::new);
	}

	/** At client setup: Flywheel draws the parts. */
	public static void registerVisuals() {
		SimpleBlockEntityVisualizer.builder(ModWastes.LIFT_BE.get())
			.factory((context, be, partialTick) -> new ShaftVisual(context, be, partialTick))
			.skipVanillaRender(be -> true)
			.apply();
		MachineVisuals.visualWhen(ModWastes.LIFT_CALL_PLATE_BE, LiftRenderers::plate, true, LiftCallPlateBlockEntity::isCalling);
	}

	/** The whole lift, in the drive's frame: its block's corner, turned with its facing about that corner's middle. */
	public static void lift(LiftBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		Level level = be.getLevel();
		if (level == null)
			return;
		BlockPos pos = be.getBlockPos();
		boolean lit = be.isAwake();
		ms.pushPose();
		WastesRenderers.face(ms, be.facing());
		// the head: on the rails' tops, round the shaft's top
		sink.part(HEAD_NW, ms, light);
		ms.pushPose();
		ms.translate(1, 0, 0);
		sink.part(HEAD_NE, ms, light);
		ms.translate(0, 0, 1);
		sink.part(HEAD_SE, ms, light);
		ms.translate(-1, 0, 0);
		sink.part(HEAD_SW, ms, light);
		ms.popPose();
		// the guide rails, a level a block, down to the shaft's floor
		for (int y = be.bottom(); y < pos.getY(); y++) {
			int at = LevelRenderer.getLightColor(level, pos.atY(y));
			ms.pushPose();
			ms.translate(0, y - pos.getY(), 0);
			sink.part(GUIDES_W, ms, at);
			if (lit)
				sink.part(SEAMS_W, ms, LightTexture.FULL_BRIGHT);
			ms.translate(1, 0, 0);
			sink.part(GUIDES_E, ms, at);
			if (lit)
				sink.part(SEAMS_E, ms, LightTexture.FULL_BRIGHT);
			ms.popPose();
		}
		// the deck, where its drive says it is this frame (a tick behind, as entities are drawn)
		double deckY = be.deckY(level.getGameTime() - 1 + pt);
		ms.pushPose();
		ms.translate(0, deckY - pos.getY(), 0);
		AABB shaft = be.shaft();
		int at = LevelRenderer.getLightColor(level, BlockPos.containing(shaft.getCenter().x, deckY + 0.5, shaft.getCenter().z));
		sink.part(DECK_W, ms, at);
		if (lit)
			sink.part(DECK_GLOW_W, ms, LightTexture.FULL_BRIGHT);
		ms.translate(1, 0, 0);
		sink.part(DECK_E, ms, at);
		if (lit)
			sink.part(DECK_GLOW_E, ms, LightTexture.FULL_BRIGHT);
		ms.popPose();
		ms.popPose();
	}

	/** A call plate's lens: dark until it's pressed, lit while its call waits for the deck (D62). */
	public static void plate(LiftCallPlateBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		if (!be.isCalling())
			return;
		ms.pushPose();
		WastesRenderers.face(ms, be.getBlockState().getValue(LiftCallPlateBlock.FACING));
		sink.part(PLATE_LENS, ms, LightTexture.FULL_BRIGHT);
		ms.popPose();
	}

	/** The whole shaft is in view while any of it is: its rails reach down to its floor, its head round its top. */
	static AABB reach(LiftBlockEntity be) {
		AABB s = be.shaft();
		return new AABB(s.minX - 1, be.bottom() - 1, s.minZ - 1, s.maxX + 1, be.getBlockPos().getY() + 2, s.maxZ + 1);
	}

	static final class ShaftVisual extends PartsVisual<LiftBlockEntity> {
		ShaftVisual(dev.engine_room.flywheel.api.visualization.VisualizationContext context, LiftBlockEntity be, float partialTick) {
			super(context, be, partialTick, LiftRenderers::lift);
		}

		@Override
		public boolean isVisible(FrustumIntersection frustum) {
			// the visual's own position is the drive's (relative to the render origin); the shaft reaches from it
			AABB r = reach(blockEntity).move(-blockEntity.getBlockPos().getX() + visualPos.getX(), -blockEntity.getBlockPos().getY() + visualPos.getY(),
				-blockEntity.getBlockPos().getZ() + visualPos.getZ());
			return frustum.testAab((float) r.minX, (float) r.minY, (float) r.minZ, (float) r.maxX, (float) r.maxY, (float) r.maxZ);
		}
	}

	static final class LiftRenderer extends SafeBlockEntityRenderer<LiftBlockEntity> {
		@Override
		protected void renderSafe(LiftBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
			lift(be, pt, ms, PartSink.forRenderer(be, buffer, overlay), light);
		}

		@Override
		public boolean shouldRenderOffScreen(LiftBlockEntity be) {
			return true;
		}

		@Override
		public AABB getRenderBoundingBox(LiftBlockEntity be) {
			return reach(be);
		}

		@Override
		public int getViewDistance() {
			return 128;
		}
	}
}
