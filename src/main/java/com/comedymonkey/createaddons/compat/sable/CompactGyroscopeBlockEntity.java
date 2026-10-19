package com.comedymonkey.createaddons.compat.sable;

import java.util.List;
import java.util.Locale;

import org.joml.Matrix3dc;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps the ship it's on level. The same controller as Gyroscope Stabilizers' Gyroscope (MIT licensed, by raprek):
 * a restoring torque towards world up, plus damping of the ship's spin and an integral term for steady lean, all
 * scaling with the flywheel's speed up to a reference RPM. Torques are applied in the ship's local frame.
 */
public class CompactGyroscopeBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor {
	private static final Vector3d WORLD_UP = new Vector3d(0, 1, 0);
	/** Largest change in angular velocity the restoring term may cause in one step, in rad/s. */
	private static final double MAX_RESTORING_DELTA_OMEGA = 0.5;
	/** The flywheel spins up and down gently rather than snapping to the shaft speed. */
	private static final double CHASE_RATE = 1 / 64.0;
	private static final int BAR_LENGTH = 18;

	public final LerpedFloat effectiveSpeed = LerpedFloat.linear();
	private boolean effectiveSpeedStarted;
	/** Redstone signal, 0 to 15: brakes the flywheel, to a stop at full power. */
	private int brake;
	/** Client: the flywheel's own angle in degrees, since braking lets it fall behind the shaft. */
	private float flywheelAngle;
	private float flywheelAngleLast;

	private final Vector3d currentUpWorld = new Vector3d();
	private final Vector3d errorAxisWorld = new Vector3d();
	private final Vector3d errorAxisLocal = new Vector3d();
	private final Vector3d angularVelocityWorld = new Vector3d();
	private final Vector3d angularVelocityLocal = new Vector3d();
	private final Vector3d restoringImpulse = new Vector3d();
	private final Vector3d dampingImpulse = new Vector3d();
	private final Vector3d deltaOmega = new Vector3d();
	private final Vector3d integral = new Vector3d();

	// Worked out in the physics step, read back on the server tick and sent to clients for the goggles
	private volatile float physicsEffort;
	private volatile float physicsTilt;
	private volatile int physicsSteps;
	private int stepsLastSeen;
	/** Synced: whether it's on a ship, how hard it's pushing (0 to 1) and the ship's tilt in degrees. */
	private boolean onShip;
	private float effort;
	private float tilt;

	public CompactGyroscopeBlockEntity(BlockPos pos, BlockState state) {
		super(AeronauticsContent.COMPACT_GYROSCOPE_BE.get(), pos, state);
	}

	@Override
	public void tick() {
		super.tick();
		if (!effectiveSpeedStarted) {
			effectiveSpeed.startWithValue(getSpeed());
			effectiveSpeedStarted = true;
		}
		if (level != null && !level.isClientSide) {
			int signal = level.getBestNeighborSignal(worldPosition);
			if (signal != brake) {
				brake = signal;
				setChanged();
				sendData();
			}
		}
		// The brake holds the flywheel back from the shaft's speed; the stronger the signal, the slower it spins
		effectiveSpeed.chase(getSpeed() * (15 - brake) / 15f, CHASE_RATE, LerpedFloat.Chaser.EXP);
		effectiveSpeed.tickChaser();
		if (level != null && level.isClientSide) {
			flywheelAngleLast = flywheelAngle;
			// RPM to degrees per tick
			flywheelAngle = (flywheelAngle + effectiveSpeed.getValue() * 0.3f) % 360;
		}
		if (level != null && !level.isClientSide && level.getGameTime() % 10 == 0)
			syncReadings();
	}

	/** The flywheel's angle in radians, smoothed between ticks. */
	public float getFlywheelAngle(float partialTicks) {
		float last = flywheelAngleLast, now = flywheelAngle;
		// Unwrap across the 360 wrap so it doesn't spin backwards for a frame
		if (now - last > 180)
			last += 360;
		else if (last - now > 180)
			last -= 360;
		return (float) Math.toRadians(last + (now - last) * partialTicks);
	}

	/** Passes what the physics step saw on to clients, when it has changed enough to show. */
	private void syncReadings() {
		int steps = physicsSteps;
		boolean nowOnShip = steps != stepsLastSeen;
		stepsLastSeen = steps;
		float nowEffort = nowOnShip ? physicsEffort : 0;
		float nowTilt = nowOnShip ? physicsTilt : 0;
		if (nowOnShip != onShip || Math.abs(nowEffort - effort) > 0.03f || Math.abs(nowTilt - tilt) > 0.5f) {
			onShip = nowOnShip;
			effort = nowEffort;
			tilt = nowTilt;
			sendData();
		}
	}

	/** 0 to 1: how much of its full strength the current flywheel speed gives. */
	private double strengthFraction() {
		return Math.min(Math.abs(effectiveSpeed.getValue()) / CMConfig.SERVER.gyroReferenceRpm(), 1);
	}

	@Override
	public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
		double fraction = strengthFraction();
		double kp = CMConfig.SERVER.gyroStrength() * fraction;
		double kd = CMConfig.SERVER.gyroDamping() * fraction;
		double ki = CMConfig.SERVER.gyroIntegral() * fraction;
		physicsSteps++;
		if (kp <= 0 && kd <= 0 && ki <= 0) {
			integral.zero();
			physicsEffort = 0;
			physicsTilt = (float) tiltDegrees(subLevel.logicalPose().orientation());
			return;
		}
		MassData mass = subLevel.getMassTracker();
		if (mass.isInvalid())
			return;
		Matrix3dc inverseInertia = mass.getInverseInertiaTensor();
		Quaterniondc orientation = subLevel.logicalPose().orientation();

		handle.getAngularVelocity(angularVelocityWorld);
		orientation.transformInverse(angularVelocityWorld, angularVelocityLocal);
		// Tilt error: the axis to turn about to bring the ship's up back to world up
		orientation.transform(WORLD_UP, currentUpWorld);
		currentUpWorld.cross(WORLD_UP, errorAxisWorld);
		orientation.transformInverse(errorAxisWorld, errorAxisLocal);

		integral.fma(timeStep, errorAxisLocal);
		double limit = CMConfig.SERVER.gyroIntegralLimit();
		double integralLength = integral.length();
		if (integralLength > limit && integralLength > 0)
			integral.mul(limit / integralLength);

		restoringImpulse.set(errorAxisLocal).mul(kp * timeStep).fma(ki * timeStep, integral);
		inverseInertia.transform(restoringImpulse, deltaOmega);
		double restoringDelta = deltaOmega.length();
		if (restoringDelta > MAX_RESTORING_DELTA_OMEGA)
			restoringImpulse.mul(MAX_RESTORING_DELTA_OMEGA / restoringDelta);

		dampingImpulse.set(angularVelocityLocal).mul(-kd * timeStep);
		inverseInertia.transform(dampingImpulse, deltaOmega);
		dampingImpulse.mul(clampingFactor(angularVelocityLocal, deltaOmega));

		restoringImpulse.add(dampingImpulse);
		handle.applyTorqueImpulse(restoringImpulse);

		// For the goggles: the torque applied against what it could apply at full strength. Square-rooted so the
		// small, constant corrections of a ship in flight still show up
		double full = Math.max(CMConfig.SERVER.gyroStrength(), 1) * timeStep;
		physicsEffort = (float) Math.min(1, Math.sqrt(restoringImpulse.length() / full));
		physicsTilt = (float) tiltDegrees(orientation);
	}

	/** How far the ship's up is from world up, in degrees. */
	private static double tiltDegrees(Quaterniondc orientation) {
		Vector3d up = orientation.transform(new Vector3d(WORLD_UP));
		return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, up.dot(WORLD_UP)))));
	}

	/** Scales the damping so it can slow the spin to a stop but never reverse it within a step. */
	private static double clampingFactor(Vector3dc velocity, Vector3dc expectedChange) {
		double k = -velocity.dot(expectedChange);
		double v = velocity.lengthSquared();
		if (k < 0)
			return 0;
		if (10 * k < v)
			return 1 - k / (2 * v);
		if (v < 1.0E-10)
			return v / (k + 1.0E-10);
		return v * (1 - Math.exp(-k / v)) / k;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.compact_gyroscope").forGoggles(tooltip);
		float spin = (float) strengthFraction();
		tooltip.add(Component.literal("     ")
			.append(Component.translatable("comedymonkeysaddons.gui.goggles.compact_gyroscope.spin").withStyle(ChatFormatting.GRAY))
			.append(bar(spin, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN)));
		if (brake > 0)
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
				.translate(brake >= 15 ? "gui.goggles.compact_gyroscope.braked" : "gui.goggles.compact_gyroscope.braking",
					Math.round(brake * 100 / 15f))
				.style(ChatFormatting.RED).forGoggles(tooltip, 1);
		if (!onShip) {
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.compact_gyroscope.not_on_ship")
				.style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
			return true;
		}
		tooltip.add(Component.literal("     ")
			.append(Component.translatable("comedymonkeysaddons.gui.goggles.compact_gyroscope.correcting").withStyle(ChatFormatting.GRAY))
			.append(bar(effort, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.RED)));
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate("gui.goggles.compact_gyroscope.tilt", String.format(Locale.ROOT, "%.1f", tilt))
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("Brake", brake);
		if (clientPacket) {
			tag.putBoolean("OnShip", onShip);
			tag.putFloat("Effort", effort);
			tag.putFloat("Tilt", tilt);
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		brake = tag.getInt("Brake");
		if (clientPacket) {
			onShip = tag.getBoolean("OnShip");
			effort = tag.getFloat("Effort");
			tilt = tag.getFloat("Tilt");
		}
	}

	/** An 18-segment bar in three colour bands. */
	private static MutableComponent bar(float fraction, ChatFormatting low, ChatFormatting mid, ChatFormatting high) {
		int filled = Math.max(0, Math.min(BAR_LENGTH, Math.round(fraction * BAR_LENGTH)));
		int third = BAR_LENGTH / 3;
		return Component.empty()
			.append(segment(Math.min(filled, third), low))
			.append(segment(Math.max(0, Math.min(filled, 2 * third) - third), mid))
			.append(segment(Math.max(0, filled - 2 * third), high))
			.append(segment(BAR_LENGTH - filled, ChatFormatting.DARK_GRAY));
	}

	private static MutableComponent segment(int count, ChatFormatting colour) {
		return count <= 0 ? Component.empty() : Component.literal("|".repeat(count)).withStyle(colour);
	}
}
