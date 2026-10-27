package com.comedymonkey.createaddons.content.helperbot;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.client.helperbot.HelperBotClient;
import com.comedymonkey.createaddons.network.HelperBotStatusPayload;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Helper Bot on a contraption, as the Blaze Burner does it: the contraption doesn't draw his block entity; he draws and
 * animates himself here, from the train he's on (in control or not, its schedule, its speed). On anything that isn't
 * a train he rides along as a passenger (D28, D37).
 */
public class HelperBotMovement implements MovementBehaviour {
	/** Whether he's in control of the train, worked out once per contraption (as the Blaze Burner does). */
	private static final String CONDUCTOR = "Conductor";
	/** His last status, also in the carriage's spawn data, so a player who starts watching sees it at once. */
	public static final String STATUS = "HelperBotStatus";
	/** Status bits sent to clients ({@link HelperBotStatusPayload}). */
	public static final int PAUSED = 1, MANUAL = 2, NO_PATH = 4, DERAILED = 8;
	/** Ticks between status checks, and between resends of an unchanged status. */
	private static final int CHECK = 5, RESEND = 100;

	/** Contraption Controls can't switch him off: he isn't doing anything to switch off. */
	@Override
	@Nullable
	public ItemStack canBeDisabledVia(MovementContext context) {
		return null;
	}

	@Override
	public void tick(MovementContext context) {
		if (context.world.isClientSide)
			HelperBotClient.tickOnContraption(context);
		else
			sendStatus(context);
	}

	/** While he drives: tell the players near his train what it's doing that their clients can't see. */
	private static void sendStatus(MovementContext context) {
		long time = context.world.getGameTime();
		if (time % CHECK != 0 || !(context.contraption.entity instanceof CarriageContraptionEntity cce) || !inControl(context))
			return;
		Carriage carriage = cce.getCarriage();
		Train train = carriage == null ? null : carriage.train;
		if (train == null)
			return;
		int flags = status(train);
		boolean changed = !context.data.contains(STATUS) || context.data.getByte(STATUS) != flags;
		if (!changed && time % RESEND != 0)
			return;
		context.data.putByte(STATUS, (byte) flags);
		PacketDistributor.sendToPlayersTrackingEntity(cce, new HelperBotStatusPayload(cce.getId(), context.localPos, (byte) flags));
	}

	/** The status bits for a train a bot drives: what its clients can't see for themselves. */
	public static int status(Train train) {
		boolean scheduled = train.runtime.getSchedule() != null;
		boolean manual = driven(train);
		// a finished schedule is paused too, but no right-click resumes it (Create takes the schedule back instead)
		boolean paused = scheduled && train.runtime.paused && !train.runtime.completed && !manual;
		return (train.derailed ? DERAILED : 0) | (manual ? MANUAL : 0) | (paused ? PAUSED : 0)
			| (scheduled && train.status.navigation ? NO_PATH : 0);
	}

	/** A player at any of the train's controls: Create marks only the carriage they sit in, but pauses the whole train. */
	private static boolean driven(Train train) {
		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity != null && entity.getControllingPlayer().isPresent())
				return true;
		}
		return false;
	}

	@Override
	public boolean disableBlockEntityRendering() {
		return true;
	}

	/** Without Flywheel the contraption's renderer draws him (with it, his visual below does, as Create's actors do). */
	@Override
	@OnlyIn(Dist.CLIENT)
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
		if (!VisualizationManager.supportsVisualization(context.world))
			HelperBotClient.renderOnContraption(context, renderWorld, matrices, buffer);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	@Nullable
	public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext context) {
		return HelperBotClient.actorVisual(visualizationContext, simulationWorld, context);
	}

	/** Directly behind Train Controls facing him on a train: he's driving it. */
	public static boolean inControl(MovementContext context) {
		if (!context.data.contains(CONDUCTOR))
			context.data.putBoolean(CONDUCTOR, determineIfConducting(context));
		return context.data.getBoolean(CONDUCTOR);
	}

	private static boolean determineIfConducting(MovementContext context) {
		if (!(context.contraption instanceof CarriageContraption carriage))
			return false;
		Direction assembly = carriage.getAssemblyDirection();
		for (Direction direction : Iterate.directionsInAxis(assembly.getAxis()))
			if (carriage.inControl(context.localPos, direction))
				return true;
		return false;
	}
}
