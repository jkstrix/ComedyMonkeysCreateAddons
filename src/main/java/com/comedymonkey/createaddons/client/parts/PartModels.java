package com.comedymonkey.createaddons.client.parts;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;

/**
 * Flywheel models of the machines' part models, one per part and render type, shared by every visual so all the
 * machines of a kind draw in one batch. Each is drawn with the render type its renderer asks for (cutout for most,
 * solid for the shaft stub), not whatever its model file names, and unshaded: a block entity renderer's parts get no
 * per-face shading, and Flywheel's would darken them (checked against the renderer with A/B screenshots).
 * Forgotten whenever the models are baked again (resource reload), when Flywheel rebuilds its visuals anyway.
 */
public final class PartModels {
	private record Key(ModelResourceLocation part, RenderType type) {}

	// concurrent: Flywheel builds and updates visuals on its worker threads
	private static final Map<Key, Model> MODELS = new ConcurrentHashMap<>();

	private PartModels() {}

	public static Model get(ModelResourceLocation part, RenderType type) {
		return MODELS.computeIfAbsent(new Key(part, type), key ->
			new BakedModelBuilder(Minecraft.getInstance().getModelManager().getModel(key.part()))
				.materialFunc((layer, shaded, ambientOcclusion) -> ModelUtil.getMaterial(key.type(), false, ambientOcclusion))
				.build());
	}

	/** On ModelEvent.BakingCompleted. */
	public static void forget() {
		MODELS.clear();
	}
}
