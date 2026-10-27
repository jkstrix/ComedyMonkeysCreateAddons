package com.comedymonkey.createaddons.client.helperbot;

import org.joml.Matrix4f;

import com.comedymonkey.createaddons.client.parts.DistanceGate;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlock;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.phys.Vec3;

/**
 * Helper Bot riding a contraption, as Flywheel instances in its embedding: posed every frame like the placed visual,
 * from the animator {@code HelperBotMovement} ticks. Lit as Create lights its own actors: the contraption's own block
 * light here, and the world's light where he is from Flywheel's embedding.
 */
public class HelperBotActorVisual extends ActorVisual {
	private final HelperBotRig rig = HelperBotRig.skeleton();
	private final ClipPlayback playback = new ClipPlayback();
	private final HelperBotInstances instances;
	private final Matrix4f pose = new Matrix4f();
	private final int light;
	private boolean placed;
	/** Create calls every actor every frame: far bots are posed less often, out of render distance not at all. */
	private final DistanceGate gate;

	public HelperBotActorVisual(VisualizationContext visualizationContext, BlockAndTintGetter world, MovementContext context) {
		super(visualizationContext, world, context);
		instances = new HelperBotInstances(instancerProvider, HelperBotClient.state(context).dye, true);
		gate = new DistanceGate(context.localPos.hashCode());
		light = LightTexture.pack(localBlockLight(), 0);
		// placed in the contraption as his renderer places him in his block
		Direction facing = context.state.hasProperty(HelperBotBlock.FACING) ? context.state.getValue(HelperBotBlock.FACING) : Direction.NORTH;
		HelperBotInstances.place(pose, context.localPos.getX(), context.localPos.getY(), context.localPos.getZ(), facing);
		// posed at once (on the tick, without a partial tick), so his first frame isn't his rest pose
		draw(AnimationTickHolder.getTicks(context.world), 0);
	}

	@Override
	public void beginFrame() {
		// where he is: his own spot as the contraption last moved him, else the contraption's
		Vec3 at = context.position != null ? context.position : context.contraption != null && context.contraption.entity != null
			? context.contraption.entity.position() : null;
		if (at != null && !gate.shouldUpdate(at))
			return;
		float partialTick = AnimationTickHolder.getPartialTicks(context.world);
		draw(AnimationTickHolder.getTicks(context.world) + partialTick, partialTick);
	}

	void draw(double now, float partialTick) {
		HelperBotClient.State st = HelperBotClient.state(context);
		st.animator.pose(rig, st.situation, now, partialTick, playback);
		instances.copy(rig);
		instances.light(light);
		instances.update(pose, !placed);
		placed = true;
	}

	@Override
	protected void _delete() {
		instances.delete();
	}
}
