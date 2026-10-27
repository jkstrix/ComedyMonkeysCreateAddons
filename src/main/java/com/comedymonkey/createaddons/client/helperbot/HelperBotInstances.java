package com.comedymonkey.createaddons.client.helperbot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.part.InstanceTree;
import dev.engine_room.flywheel.lib.model.part.ModelTree;
import dev.engine_room.flywheel.lib.model.part.ModelTrees;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

/**
 * One Helper Bot as Flywheel instances, for {@link HelperBotVisual} (placed) and {@link HelperBotActorVisual} (on a
 * contraption): his model in his dye's texture, bone by bone, with his irises' glowing cubes at full brightness (the
 * same texels as his glow texture, as his renderer draws them), posed by copying a {@link HelperBotRig} over.
 */
final class HelperBotInstances {
	/**
	 * As his renderer's entity render type: cutout, both sides, unmipped, lit as an entity. One per texture, and a
	 * second set for when he's embedded (on a train or a ship): there Flywheel's default light would shade him with
	 * ambient occlusion (down to a fifth, irises and all, in a tunnel or under a station roof), which his renderer
	 * never did. Embedded, he takes the world's light block by block instead (the flat light shader, no occlusion).
	 * Placed in the world nothing changes: the default light does nothing there but his own light.
	 */
	private static final Material PLAIN = build(null, false), PLAIN_EMBEDDED = build(null, true);
	private static final Map<DyeColor, Material> DYED = new EnumMap<>(DyeColor.class), DYED_EMBEDDED = new EnumMap<>(DyeColor.class);
	static {
		for (DyeColor dye : DyeColor.values()) {
			DYED.put(dye, build(dye, false));
			DYED_EMBEDDED.put(dye, build(dye, true));
		}
	}

	/** Where each material's tree of his model comes from (tests build theirs without a running game). */
	static Function<Material, ModelTree> trees = material -> ModelTrees.of(HelperBotModels.LAYER, material);

	private final InstancerProvider provider;
	private InstanceTree tree;
	/** The tree's node for each of the rig's bones, and whether each is drawn this frame (a hidden bone hides all below it). */
	private final InstanceTree[] nodes = new InstanceTree[HelperBotRig.bones()];
	private final boolean[] shown = new boolean[HelperBotRig.bones()];
	/** Everything but the glowing cubes: these take the light where he stands. */
	private final List<TransformedInstance> lit = new ArrayList<>();
	@Nullable
	private DyeColor dye;
	private final boolean embedded;
	private int light = -1;
	/** Something moved since the last update; the whole tree is new (placed from scratch). */
	private boolean dirty, fresh;

	HelperBotInstances(InstancerProvider provider, @Nullable DyeColor dye, boolean embedded) {
		this.provider = provider;
		this.dye = dye;
		this.embedded = embedded;
		build();
	}

	/** His material in this dye, placed or embedded: made once, shared by every visual. */
	static Material material(@Nullable DyeColor dye, boolean embedded) {
		if (embedded)
			return dye == null ? PLAIN_EMBEDDED : DYED_EMBEDDED.get(dye);
		return dye == null ? PLAIN : DYED.get(dye);
	}

	private static Material build(@Nullable DyeColor dye, boolean embedded) {
		SimpleMaterial.Builder builder = SimpleMaterial.builder().texture(HelperBotModels.texture(dye)).cutout(CutoutShaders.ONE_TENTH)
			.backfaceCulling(false).mipmap(false).cardinalLightingMode(CardinalLightingMode.ENTITY).ambientOcclusion(false);
		if (embedded)
			builder.light(LightShaders.FLAT);
		return builder.build();
	}

	private void build() {
		tree = InstanceTree.create(provider, trees.apply(material(dye, embedded)));
		Map<String, InstanceTree> byName = new HashMap<>();
		index(tree, byName);
		lit.clear();
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = byName.get(HelperBotRig.name(i));
			TransformedInstance instance = nodes[i] == null ? null : nodes[i].instance();
			if (instance == null)
				continue;
			if (HelperBotRig.Bones.isGlow(i))
				instance.light(LightTexture.FULL_BRIGHT).setChanged();
			else
				lit.add(instance);
		}
		light = -1;
		dirty = fresh = true;
	}

	private static void index(InstanceTree node, Map<String, InstanceTree> out) {
		for (int i = 0; i < node.childCount(); i++) {
			out.put(node.childName(i), node.child(i));
			index(node.child(i), out);
		}
	}

	/** His colour can't change in place today, but if it ever does (a data packet), he's made again in the new texture. */
	void dye(@Nullable DyeColor dye) {
		if (dye == this.dye)
			return;
		this.dye = dye;
		tree.delete();
		build();
	}

	/**
	 * Takes the rig's pose. Bones are in parent-first order, so a bone hidden by a clip (or squashed to nothing) hides
	 * everything below it, as the model's own drawing does; hidden bones' transforms are taken once they show again.
	 */
	void copy(HelperBotRig rig) {
		for (int i = 0; i < nodes.length; i++) {
			ModelPart part = rig.parts[i];
			int parent = HelperBotRig.parent(i);
			boolean on = part.visible && (parent < 0 || shown[parent]);
			shown[i] = on;
			InstanceTree node = nodes[i];
			if (node == null)
				continue;
			// (a node's visibility is set on everything under it too: its children come after it and set their own)
			if (node.visible() != on) {
				node.visible(on);
				// shown again: placed again too, as whatever is above it may have moved while it was hidden (Flywheel
				// stops at a hidden node, so it wouldn't know)
				if (on)
					node.copyTransform(part);
				dirty = true;
			}
			if (on && (node.xPos() != part.x || node.yPos() != part.y || node.zPos() != part.z || node.xRot() != part.xRot
				|| node.yRot() != part.yRot || node.zRot() != part.zRot || node.xScale() != part.xScale || node.yScale() != part.yScale
				|| node.zScale() != part.zScale)) {
				node.copyTransform(part);
				dirty = true;
			}
		}
	}

	/** The light where he stands, for all but his glowing irises. */
	void light(int light) {
		if (light == this.light)
			return;
		this.light = light;
		for (TransformedInstance instance : lit)
			instance.light(light).setChanged();
	}

	/**
	 * Places the posed tree at {@code pose} (block space to the visual's). Only the bones that moved since last time
	 * (and what hangs from them) are sent again, unless {@code moved} (the whole of him moved) or the tree is new.
	 */
	void update(Matrix4fc pose, boolean moved) {
		if (!dirty && !moved && !fresh)
			return;
		if (moved || fresh)
			tree.updateInstances(pose);
		else
			tree.updateInstancesStatic(pose);
		dirty = fresh = false;
	}

	/**
	 * Where his visuals stand him in block {@code (x, y, z)} (in the visual's space), facing {@code facing}: the same
	 * transform as his renderer's {@code HelperBotModels.draw}, 2 px toward what he faces, in entity space.
	 */
	static Matrix4f place(Matrix4f out, float x, float y, float z, Direction facing) {
		return out.identity().translate(x + 0.5f + facing.getStepX() * 2 / 16f, y, z + 0.5f + facing.getStepZ() * 2 / 16f)
			.rotateY((float) Math.toRadians(180 - facing.toYRot())).scale(-1, -1, 1).translate(0, -1.501f, 0);
	}

	void collect(Consumer<Instance> consumer) {
		tree.traverse(consumer);
	}

	void delete() {
		tree.delete();
	}
}
