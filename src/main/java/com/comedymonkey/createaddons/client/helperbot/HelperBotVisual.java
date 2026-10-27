package com.comedymonkey.createaddons.client.helperbot;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

import com.comedymonkey.createaddons.client.parts.DistanceGate;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlock;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed Helper Bot as Flywheel instances: his own cube-less skeleton, posed every frame by his animator exactly as
 * his renderer poses the shared model (with a playback of its own, since this runs on Flywheel's workers), then copied
 * onto his instances. The animator itself still ticks on the client tick (his choices and his sounds). His renderer
 * stays for when Flywheel can't draw, for Ponder, the shop screen and the item.
 */
public class HelperBotVisual extends AbstractBlockEntityVisual<HelperBotBlockEntity> implements SimpleDynamicVisual {
	private final HelperBotRig rig = HelperBotRig.skeleton();
	private final ClipPlayback playback = new ClipPlayback();
	private final HelperBotInstances instances;
	private final Matrix4f pose = new Matrix4f();
	@Nullable
	private Direction facing;
	private volatile int light;
	/**
	 * On a ship or a contraption its position is the embedding's, so the world's frustum and Flywheel's distance
	 * limiter say nothing about it: there he's measured from where he really is ({@link DistanceGate}).
	 */
	private final boolean embedded;
	@Nullable
	private Entity owner;
	private boolean ownerSought;

	public HelperBotVisual(VisualizationContext context, HelperBotBlockEntity be, float partialTick) {
		super(context, be, partialTick);
		embedded = context instanceof VisualEmbedding;
		instances = new HelperBotInstances(instancerProvider(), be.dye(), embedded);
		light = computePackedLight();
		draw(partialTick);
	}

	/** As his renderer's culling box: half a block taller than his block, his arms and swivel past its sides. */
	@Override
	public boolean isVisible(FrustumIntersection frustum) {
		float x = visualPos.getX(), y = visualPos.getY(), z = visualPos.getZ();
		return frustum.testAab(x - 0.5f, y - 0.5f, z - 0.5f, x + 1.5f, y + 2.5f, z + 1.5f);
	}

	@Override
	public void beginFrame(DynamicVisual.Context context) {
		// far away he's posed less often, and not at all out of view
		if (embedded) {
			if (!ownerSought) {
				ownerSought = true;
				owner = DistanceGate.contraptionOf(blockEntity);
			}
			if (DistanceGate.skip(context, DistanceGate.worldPosition(blockEntity, owner)))
				return;
		} else if (!isVisible(context.frustum()) || doDistanceLimitThisFrame(context))
			return;
		draw(context.partialTick());
	}

	private void draw(float partialTick) {
		Level level = blockEntity.getLevel();
		if (level == null)
			return;
		instances.dye(blockEntity.dye());
		HelperBotClient.State st = HelperBotClient.state(blockEntity);
		st.animator.pose(rig, st.situation, AnimationTickHolder.getTicks(level) + partialTick, partialTick, playback);
		instances.copy(rig);
		// placed as his renderer places him: 2 px toward what he faces, turned to face it, in entity space
		BlockState state = blockEntity.getBlockState();
		Direction now = state.hasProperty(HelperBotBlock.FACING) ? state.getValue(HelperBotBlock.FACING) : Direction.NORTH;
		boolean turned = now != facing;
		if (turned) {
			facing = now;
			HelperBotInstances.place(pose, visualPos.getX(), visualPos.getY(), visualPos.getZ(), now);
		}
		instances.light(light);
		instances.update(pose, turned);
	}

	@Override
	public void updateLight(float partialTick) {
		// (applied on the next frame: Flywheel runs light updates alongside it)
		light = computePackedLight();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		instances.collect(consumer);
	}

	@Override
	protected void _delete() {
		instances.delete();
	}
}
