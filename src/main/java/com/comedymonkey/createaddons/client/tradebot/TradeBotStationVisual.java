package com.comedymonkey.createaddons.client.tradebot;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.joml.Matrix4f;

import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.part.InstanceTree;
import dev.engine_room.flywheel.lib.model.part.ModelTrees;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * The winding station as Flywheel instances: the station model, and its glowing cubes at full brightness, posed every
 * frame by the same code as {@link TradeBotStationRenderer} (it follows Trade Bot's clip, so it can move any frame), then
 * copied onto the instance trees. There's one station per Trade Bot, so this costs little.
 */
public class TradeBotStationVisual extends AbstractBlockEntityVisual<TradeBotStationBlockEntity> implements SimpleDynamicVisual {
	private static final Material MODEL = material(TradeBotRenderer.TEXTURE, false), GLOW = material(TradeBotRenderer.GLOW, true);

	/** The station's bones, posed as the renderer poses them, and its instance trees, bone by bone. */
	private final ModelPart root;
	private final Map<String, ModelPart> parts;
	private final InstanceTree tree, glowTree;
	private final Map<String, InstanceTree> nodes = new HashMap<>(), glowNodes = new HashMap<>();
	private final Matrix4f pose = new Matrix4f();
	private volatile int light;
	private int lit = -1;
	/** Its own playback of Trade Bot's clip: it runs on Flywheel's workers, so it never touches his. */
	private final com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback playback = new com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback();
	/** On a ship or a contraption its position is the embedding's, so the world's frustum says nothing about it. */
	private final boolean embedded;

	public TradeBotStationVisual(VisualizationContext context, TradeBotStationBlockEntity be, float partialTick) {
		super(context, be, partialTick);
		embedded = context instanceof dev.engine_room.flywheel.api.visualization.VisualEmbedding;
		root = Minecraft.getInstance().getEntityModels().bakeLayer(TradeBotGeometry.STATION_LAYER);
		parts = TradeBotGeometry.get().parts(root, "station");
		tree = InstanceTree.create(instancerProvider(), ModelTrees.of(TradeBotGeometry.STATION_LAYER, MODEL));
		glowTree = InstanceTree.create(instancerProvider(), ModelTrees.of(TradeBotGeometry.STATION_GLOW_LAYER, GLOW));
		index(tree, nodes);
		index(glowTree, glowNodes);
		glowTree.traverse(i -> i.light(LightTexture.FULL_BRIGHT).setChanged());
		light = computePackedLight();
		draw(partialTick);
	}

	/**
	 * As the renderer's entity render type: cutout, both sides, lit as an entity. The glow sits exactly on the model's
	 * faces, so it's drawn a hair in front ({@code over}), whatever order the two are drawn in.
	 */
	private static Material material(net.minecraft.resources.ResourceLocation texture, boolean over) {
		return SimpleMaterial.builder().texture(texture).cutout(CutoutShaders.ONE_TENTH).backfaceCulling(false).mipmap(false)
			.polygonOffset(over).cardinalLightingMode(CardinalLightingMode.ENTITY).build();
	}

	private static void index(InstanceTree node, Map<String, InstanceTree> out) {
		for (int i = 0; i < node.childCount(); i++) {
			out.put(node.childName(i), node.child(i));
			index(node.child(i), out);
		}
	}

	/** The station reaches a block or so past its own (its winder arm, its flag): kept up to date while any of it is in view. */
	@Override
	public boolean isVisible(org.joml.FrustumIntersection frustum) {
		float x = visualPos.getX(), y = visualPos.getY(), z = visualPos.getZ();
		return frustum.testAab(x - 2, y - 1, z - 2, x + 3, y + 3, z + 3);
	}

	@Override
	public void beginFrame(DynamicVisual.Context context) {
		if (!embedded && (!isVisible(context.frustum()) || doDistanceLimitThisFrame(context)))
			return;
		draw(context.partialTick());
	}

	private void draw(float partialTick) {
		TradeBotStationRenderer.pose(parts, root, blockEntity, partialTick, playback);
		for (Map.Entry<String, ModelPart> e : parts.entrySet()) {
			copy(e.getValue(), nodes.get(e.getKey()));
			copy(e.getValue(), glowNodes.get(e.getKey()));
		}
		// placed as the renderer places it: at the dock point, turned to face out, in entity space
		Vec3 dock = TradeBotStationBlock.dockPoint(BlockPos.ZERO, blockEntity.facing());
		pose.identity().translate(visualPos.getX() + (float) dock.x, visualPos.getY() + (float) dock.y, visualPos.getZ() + (float) dock.z)
			.rotateY((float) Math.toRadians(180 - blockEntity.facing().toYRot())).scale(-1, -1, 1).translate(0, -1.501f, 0);
		tree.updateInstances(pose);
		glowTree.updateInstances(pose);
		int now = light;
		if (now != lit) {
			lit = now;
			tree.traverse(i -> i.light(now).setChanged());
		}
	}

	private static void copy(ModelPart from, InstanceTree to) {
		if (to == null)
			return;
		to.copyTransform(from);
		to.visible(from.visible);
		to.skipDraw(from.skipDraw);
	}

	@Override
	public void updateLight(float partialTick) {
		// (applied on the next frame: Flywheel runs light updates alongside it)
		light = computePackedLight();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		tree.traverse(consumer);
	}

	@Override
	protected void _delete() {
		tree.delete();
		glowTree.delete();
	}
}
