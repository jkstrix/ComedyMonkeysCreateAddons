package com.comedymonkey.createaddons.compat.sable;

import java.util.List;
import java.util.Locale;

import org.joml.Vector3d;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.particle.AirParticleData;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A propeller that only turns the ship. Its blades push along their facing exactly like an Aeronautics Andesite
 * Propeller at the same speed (thrust per RPM, scaled by the physics step, the way Sable applies propeller thrust),
 * but only the turning that push would cause about the ship's centre of mass is applied, never the push itself. So
 * on a tail boom it yaws the ship as a tail propeller would, without dragging it sideways. Wind comes off the blades
 * the same way as a propeller's.
 */
public class TorqueRotorBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor {
	/** Airflow per RPM, as the Andesite Propeller; only sets how far and fast the wind particles go. */
	private static final double AIRFLOW_PER_RPM = 0.1;
	/** How far the blades reach from the shaft, in blocks. */
	private static final double BLADE_RADIUS = 0.75;
	private static ParticleOptions wind;

	private final Vector3d push = new Vector3d();
	private final Vector3d arm = new Vector3d();
	private final Vector3d torque = new Vector3d();

	public TorqueRotorBlockEntity(BlockPos pos, BlockState state) {
		super(AeronauticsContent.TORQUE_ROTOR_BE.get(), pos, state);
	}

	/** The blades reach half a block past the block on every side. */
	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition).inflate(0.5);
	}

	private Direction facing() {
		return getBlockState().getValue(DirectionalKineticBlock.FACING);
	}

	/** Speed signed as an Aeronautics propeller signs it: positive blows air out along the facing. */
	private float directionIndependentSpeed() {
		return facing().getAxisDirection().getStep() * getSpeed();
	}

	/** Thrust the blades make at the current speed, signed like {@link #directionIndependentSpeed()}. */
	private double thrust() {
		return CMConfig.SERVER.torqueRotorThrust() * directionIndependentSpeed();
	}

	@Override
	public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
		double thrust = thrust();
		if (thrust == 0)
			return;
		MassData mass = subLevel.getMassTracker();
		if (mass.isInvalid())
			return;
		// The ship is pushed against the airflow, just as a propeller here would push it...
		Direction facing = facing();
		push.set(facing.getStepX(), facing.getStepY(), facing.getStepZ()).mul(-thrust * timeStep);
		// ...but it only gets the turn that push causes about its centre of mass (ship-local frame throughout)
		arm.set(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)
			.sub(mass.getCenterOfMass());
		arm.cross(push, torque);
		handle.applyTorqueImpulse(torque);
	}

	@Override
	public void tick() {
		super.tick();
		if (level != null && level.isClientSide && getSpeed() != 0 && !isVirtual())
			spawnWind();
	}

	/** Air streaming through the blade disc, at the rate and speed an Andesite Propeller's does. */
	private void spawnWind() {
		RandomSource random = level.random;
		double rotationSpeed = Math.abs(getSpeed()) * 0.3;
		double amount = 0.12 * rotationSpeed;
		int count = Math.min((int) (Math.log(amount / 2 + 1) * 2 + random.nextFloat()), 5);
		double speed = Mth.clamp(AIRFLOW_PER_RPM * directionIndependentSpeed() / 20, -5, 5);

		Direction facing = facing();
		Direction.Axis axis = facing.getAxis();
		// Two directions across the blade disc
		Direction.Axis u = axis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
		Direction.Axis v = axis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
		for (int i = 0; i < count; i++) {
			double radius = Math.sqrt(random.nextDouble()) * BLADE_RADIUS;
			double angle = random.nextDouble() * Math.PI * 2;
			double along = -0.06 + speed * random.nextFloat();
			double[] p = {0.5, 0.5, 0.5};
			p[axis.ordinal()] += facing.getAxisDirection().getStep() * along;
			p[u.ordinal()] += Math.cos(angle) * radius;
			p[v.ordinal()] += Math.sin(angle) * radius;
			double fade = speed * Math.exp(-0.2 * Math.abs(along));
			level.addParticle(wind(), worldPosition.getX() + p[0], worldPosition.getY() + p[1], worldPosition.getZ() + p[2],
				facing.getStepX() * fade, facing.getStepY() * fade, facing.getStepZ() * fade);
		}
	}

	/** Aeronautics' own propeller airflow particle when it's installed, Create's air particle otherwise. */
	private static ParticleOptions wind() {
		if (wind == null) {
			try {
				wind = (ParticleOptions) Class.forName("dev.eriksonn.aeronautics.content.particle.PropellerAirParticleData")
					.getConstructor(boolean.class, boolean.class).newInstance(true, false);
			} catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
				wind = new AirParticleData(1, 0.1f);
			}
		}
		return wind;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.torque_rotor").forGoggles(tooltip);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate("gui.goggles.torque_rotor.strength", String.format(Locale.ROOT, "%,.0f", Math.abs(thrust())))
			.style(getSpeed() == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA)
			.forGoggles(tooltip, 1);
		return true;
	}
}
