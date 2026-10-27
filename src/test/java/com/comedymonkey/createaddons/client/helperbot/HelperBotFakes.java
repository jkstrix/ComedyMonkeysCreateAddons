package com.comedymonkey.createaddons.client.helperbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.part.ModelTree;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

/**
 * Stand-ins for what a running game gives Helper Bot's rendering: a Flywheel instancer that records the instances it
 * hands out (no GPU), his model's tree built straight from the baked layer (as Flywheel's {@code ModelTrees} builds
 * it: a model on every bone with cubes), and a vertex consumer that records what's drawn.
 */
final class HelperBotFakes {
	private HelperBotFakes() {}

	/** A bone's model in the tree: no meshes, just its bone's name. */
	record BoneModel(String bone) implements Model {
		@Override
		public List<ConfiguredMesh> meshes() {
			return List.of();
		}

		@Override
		public Vector4fc boundingSphere() {
			return new Vector4f();
		}
	}

	/** An instance's handle, keeping what Flywheel would be told. */
	static final class Handle implements InstanceHandle {
		boolean visible = true, deleted;
		int changes;

		@Override
		public void setChanged() {
			changes++;
		}

		@Override
		public void setDeleted() {
			deleted = true;
		}

		@Override
		public void setVisible(boolean visible) {
			this.visible = visible;
		}

		@Override
		public boolean isVisible() {
			return visible;
		}
	}

	/** Every instance handed out, by bone, with its handle. */
	static final class Provider implements InstancerProvider {
		final Map<String, TransformedInstance> byBone = new HashMap<>();
		final Map<Instance, Handle> handles = new IdentityHashMap<>();
		final List<TransformedInstance> all = new ArrayList<>();

		@Override
		public <I extends Instance> Instancer<I> instancer(InstanceType<I> type, Model model, int bias) {
			return new Instancer<>() {
				@Override
				public I createInstance() {
					Handle handle = new Handle();
					I instance = type.create(handle);
					handles.put(instance, handle);
					all.add((TransformedInstance) instance);
					byBone.put(((BoneModel) model).bone(), (TransformedInstance) instance);
					return instance;
				}

				@Override
				public void stealInstance(@Nullable I instance) {
					throw new UnsupportedOperationException();
				}
			};
		}

		Handle handle(Instance instance) {
			return handles.get(instance);
		}

		int changes() {
			int n = 0;
			for (Handle h : handles.values())
				n += h.changes;
			return n;
		}
	}

	/** His model's tree from the layer with cubes, as Flywheel's {@code ModelTrees.of(LAYER, material)} has it. */
	static ModelTree tree() {
		ModelPart root = HelperBotModels.layer().bakeRoot();
		Map<String, ModelPart> parts = HelperBotModels.geometry().parts(root, HelperBotModels.ROOT);
		return new ModelTree(null, PartPose.ZERO, Map.of(HelperBotModels.ROOT, node(0, parts)));
	}

	private static ModelTree node(int bone, Map<String, ModelPart> parts) {
		ModelPart part = parts.get(HelperBotRig.name(bone));
		Map<String, ModelTree> children = new HashMap<>();
		for (int i = 0; i < HelperBotRig.bones(); i++)
			if (HelperBotRig.parent(i) == bone)
				children.put(HelperBotRig.name(i), node(i, parts));
		return new ModelTree(part.isEmpty() ? null : new BoneModel(HelperBotRig.name(bone)), part.getInitialPose(), children);
	}

	/** Where a rig's bone ends up, through every bone above it, as his renderer's drawing places it. */
	static Matrix4f boneMatrix(HelperBotRig rig, int bone, Matrix4f start) {
		PoseStack ms = new PoseStack();
		ms.last().pose().set(start);
		rig.root.translateAndRotate(ms);
		List<Integer> chain = new ArrayList<>();
		for (int i = bone; i >= 0; i = HelperBotRig.parent(i))
			chain.add(0, i);
		for (int i : chain)
			rig.parts[i].translateAndRotate(ms);
		return new Matrix4f(ms.last().pose());
	}

	/** Whether a bone is drawn: it and every bone above it shown, as ModelPart.render decides. */
	static boolean drawn(HelperBotRig rig, int bone) {
		for (int i = bone; i >= 0; i = HelperBotRig.parent(i))
			if (!rig.parts[i].visible)
				return false;
		return true;
	}

	/** One recorded vertex: where it is and its packed light. */
	record Vertex(float x, float y, float z, int light) {}

	/** Records every vertex drawn into it. */
	static final class Recorder implements VertexConsumer {
		final List<Vertex> vertices = new ArrayList<>();
		private float x, y, z;
		private boolean open;

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
			open = true;
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			if (open)
				vertices.add(new Vertex(x, y, z, u | v << 16));
			open = false;
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}

		long lit(int light) {
			return vertices.stream().filter(v -> v.light() == light).count();
		}
	}
}
