package com.comedymonkey.createaddons.client.juggernaut;

import com.comedymonkey.createaddons.content.juggernaut.ClockworkJuggernaut;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The ground shaking under the Juggernaut's spring leap: the camera of a player near where he comes down judders for
 * a moment, strongest close in, and scaled by the player's Screen Effect Scale setting (0 turns it off). Also keeps the
 * landing's freeze from zooming the view.
 */
public final class JuggernautShake {
	/** How far from his landing the ground is felt shaking, in blocks, and for how long, in ticks. */
	private static final double RANGE = 18;
	private static final int TICKS = 16;
	/** Degrees the camera swings at its worst, right under him. */
	private static final float DEGREES = 3;

	private static float strength;
	private static int ticksLeft;

	private JuggernautShake() {}

	public static void register() {
		ClockworkJuggernaut.landShake = JuggernautShake::landed;
		NeoForge.EVENT_BUS.addListener(JuggernautShake::onClientTick);
		NeoForge.EVENT_BUS.addListener(JuggernautShake::onCameraAngles);
		NeoForge.EVENT_BUS.addListener(JuggernautShake::onFov);
	}

	/** Held still by his landing, the view doesn't zoom in as it would for ordinary Slowness. */
	private static void onFov(ComputeFovModifierEvent event) {
		MobEffectInstance slow = event.getPlayer().getEffect(MobEffects.MOVEMENT_SLOWDOWN);
		if (slow != null && slow.getAmplifier() == ClockworkJuggernaut.FREEZE_AMPLIFIER && event.getPlayer().getTicksFrozen() > 0)
			event.setNewFovModifier(1);
	}

	private static void landed(Vec3 at) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		double d = mc.player.position().distanceTo(at);
		if (d > RANGE)
			return;
		strength = Math.max(ticksLeft > 0 ? strength : 0, (float) (1 - d / RANGE));
		ticksLeft = TICKS;
	}

	private static void onClientTick(ClientTickEvent.Post event) {
		if (ticksLeft > 0)
			ticksLeft--;
	}

	private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		if (ticksLeft <= 0)
			return;
		float scale = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
		float t = (float) (TICKS - ticksLeft + event.getPartialTick());
		// a hard jolt that dies away over the shake
		float amount = DEGREES * strength * scale * (1 - t / TICKS) * (1 - t / TICKS);
		event.setPitch(event.getPitch() + amount * Mth.sin(t * 2.9f));
		event.setRoll(event.getRoll() + amount * 0.8f * Mth.sin(t * 2.3f + 1.7f));
		event.setYaw(event.getYaw() + amount * 0.4f * Mth.sin(t * 3.7f + 0.6f));
	}
}
