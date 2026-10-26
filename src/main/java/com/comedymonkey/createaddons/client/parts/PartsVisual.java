package com.comedymonkey.createaddons.client.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.joml.FrustumIntersection;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.LightUpdatedVisual;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A machine's model parts as Flywheel instances: every frame its renderer's part method poses them, exactly as it
 * would for the screen, and each part goes to the instance in the same place in the order (a new one only when the
 * part changes there, as when a lamp changes colour). Moving the instances costs a matrix per part, not a re-drawn
 * model, and an instance is only sent to the GPU again when it moved, changed light or was shown or hidden: a part
 * standing still costs a comparison a frame. Parts the method leaves out this frame are hidden. Fluids, items and other things that aren't part models
 * stay with the renderer.
 * <p>
 * Only {@link #beginFrame} (and the constructor) touches the instances: Flywheel runs light updates on its worker
 * threads alongside the frame, so {@link #updateLight} just notes the new light for the next frame.
 */
public class PartsVisual<T extends BlockEntity> extends AbstractBlockEntityVisual<T> implements SimpleDynamicVisual, LightUpdatedVisual {
	/** A renderer's part method: poses its parts about {@code ms} (at the block's corner) into {@code sink}. */
	@FunctionalInterface
	public interface Parts<T> {
		void draw(T be, float partialTicks, PoseStack ms, PartSink sink, int light);
	}

	private final Parts<T> parts;
	/** Whether it has parts to show at all now (a line with no fitting has none): checked first, every frame. */
	private final java.util.function.Predicate<T> active;
	/**
	 * On a Sable ship or a contraption the visual sits in an embedding: its position is relative to that, so the world
	 * frustum and the camera distance say nothing about it, and the ship moves it anyway. It's updated every frame.
	 */
	private final boolean embedded;
	/** The renderer's own view distance, squared: past it, the renderer drew nothing, so the parts go too. */
	private final double viewDistanceSqr;
	private final List<ModelResourceLocation> models = new ArrayList<>();
	private final List<RenderType> types = new ArrayList<>();
	private final List<TransformedInstance> instances = new ArrayList<>();
	/** What each instance was last sent as: its pose, light, and whether it's showing. */
	private final List<org.joml.Matrix4f> poses = new ArrayList<>();
	private final List<int[]> lights = new ArrayList<>();
	private final List<boolean[]> showing = new ArrayList<>();
	private final PoseStack ms = new PoseStack();
	private final PartSink sink = this::place;
	private volatile int light;
	private volatile boolean relit;
	private boolean hidden;
	private int next;

	public PartsVisual(VisualizationContext context, T be, float partialTick, Parts<T> parts) {
		this(context, be, partialTick, parts, b -> true);
	}

	public PartsVisual(VisualizationContext context, T be, float partialTick, Parts<T> parts, java.util.function.Predicate<T> active) {
		super(context, be, partialTick);
		this.parts = parts;
		this.active = active;
		embedded = context instanceof VisualEmbedding;
		BlockEntityRenderer<T> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
		int distance = renderer != null ? renderer.getViewDistance() : 64;
		viewDistanceSqr = (double) distance * distance;
		light = computePackedLight();
		draw(partialTick);
	}

	/**
	 * Parts can reach a block or two beyond their own (the clock's pendulum hangs into the block below, the turbine
	 * fills 3 x 3 x 3 behind its controller), so they keep moving while any block that near is in view.
	 */
	@Override
	public boolean isVisible(FrustumIntersection frustum) {
		float x = visualPos.getX(), y = visualPos.getY(), z = visualPos.getZ();
		return frustum.testAab(x - 2, y - 2, z - 2, x + 3, y + 3, z + 3);
	}

	@Override
	public void beginFrame(DynamicVisual.Context context) {
		if (!active.test(blockEntity)) {
			hideAll();
			return;
		}
		if (!embedded) {
			if (context.camera().getPosition().distanceToSqr(Vec3.atCenterOf(pos)) > viewDistanceSqr) {
				hideAll();
				return;
			}
			if (!relit && (!isVisible(context.frustum()) || doDistanceLimitThisFrame(context)))
				return;
		}
		relit = false;
		draw(context.partialTick());
	}

	@Override
	public void updateLight(float partialTick) {
		light = computePackedLight();
		relit = true;
	}

	private void draw(float partialTick) {
		next = 0;
		hidden = false;
		ms.setIdentity();
		ms.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
		parts.draw(blockEntity, partialTick, ms, sink, light);
		for (int i = next; i < instances.size(); i++)
			hide(i);
	}

	private void hide(int i) {
		if (!showing.get(i)[0])
			return;
		showing.get(i)[0] = false;
		instances.get(i).setVisible(false);
	}

	private void hideAll() {
		if (hidden)
			return;
		hidden = true;
		for (int i = 0; i < instances.size(); i++)
			hide(i);
	}

	private void place(ModelResourceLocation model, PoseStack pose, int partLight, RenderType type) {
		TransformedInstance instance;
		int i = next++;
		if (i < instances.size() && models.get(i).equals(model) && types.get(i) == type) {
			instance = instances.get(i);
			// standing still, same light, already showing: nothing to send
			if (showing.get(i)[0] && lights.get(i)[0] == partLight && poses.get(i).equals(pose.last().pose()))
				return;
		} else {
			instance = instancerProvider().instancer(InstanceTypes.TRANSFORMED, PartModels.get(model, type)).createInstance();
			if (i < instances.size()) {
				instances.get(i).delete();
				instances.set(i, instance);
				models.set(i, model);
				types.set(i, type);
			} else {
				instances.add(instance);
				models.add(model);
				types.add(type);
				poses.add(new org.joml.Matrix4f());
				lights.add(new int[1]);
				showing.add(new boolean[1]);
			}
		}
		poses.get(i).set(pose.last().pose());
		lights.get(i)[0] = partLight;
		showing.get(i)[0] = true;
		instance.setTransform(pose.last());
		instance.light(partLight);
		instance.setVisible(true);
		instance.setChanged();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		instances.forEach(consumer);
	}

	@Override
	protected void _delete() {
		instances.forEach(TransformedInstance::delete);
		instances.clear();
		models.clear();
		types.clear();
		poses.clear();
		lights.clear();
		showing.clear();
	}
}
