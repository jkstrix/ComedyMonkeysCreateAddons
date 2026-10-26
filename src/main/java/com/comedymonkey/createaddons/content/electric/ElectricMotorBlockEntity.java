package com.comedymonkey.createaddons.content.electric;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.SaveMark;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Draws {@code stress / 16} FE a tick (configurable) and, while it has the power, turns at its set speed with its set
 * stress capacity. Short of power it stops, and starts again once it has a reserve stored: 30 seconds' worth, or a
 * quarter of its buffer if that's less (a few seconds at high stress). Each start and stop jolts the whole kinetic
 * network, so a weak supply makes it pause now and then rather than stutter.
 */
public class ElectricMotorBlockEntity extends GeneratingKineticBlockEntity implements com.comedymonkey.createaddons.content.line.NetworkReading.Shown {
	/** Ticks of running it waits to have stored before it starts again after running short. */
	private static final int RESTART_TICKS = 600;
	public static final int MIN_SPEED = 1;
	public static final int MAX_SPEED = 256;
	/** Stress is set in steps of this. */
	public static final int STRESS_STEP = 256;

	private final EnergyBuffer energy;
	private int speed = 64;
	private int stress = 16384;
	private boolean reversed;
	private boolean running;
	/** Fractions of an FE owed from earlier ticks. */
	private double owed;
	private int syncedEnergy = -1;
	/** Client: run every client tick (the motor's sounds); set by the client setup. */
	public static Consumer<ElectricMotorBlockEntity> clientTicker = be -> {};

	/** Its Power Line network's figures, for the goggles. */
	private final com.comedymonkey.createaddons.content.line.NetworkReading reading = new com.comedymonkey.createaddons.content.line.NetworkReading();

	@Override
	public void networkReading(int makes, int uses) {
		if (level != null && reading.set(makes, uses, level.getGameTime()))
			sendData();
	}

	public ElectricMotorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ELECTRIC_MOTOR.get(), pos, state);
		// Two seconds' worth at full stress: Power Lines top it up once a second, and a little gap doesn't stall it
		int buffer = Math.max(100_000, (int) Math.ceil(CMConfig.SERVER.motorMaxStress() * CMConfig.SERVER.motorFePerStress() * 40));
		// No comparator output: its store changing every tick only needs a save, not a comparator update
		energy = new EnergyBuffer(buffer, buffer, 0, () -> SaveMark.mark(this));
	}

	public IEnergyStorage getEnergy() {
		return energy;
	}

	public int getSpeedSetting() {
		return speed;
	}

	public int getStressSetting() {
		return stress;
	}

	public boolean isReversed() {
		return reversed;
	}

	public boolean isRunning() {
		return running;
	}

	/** FE per tick at the current stress setting. */
	public double usagePerTick() {
		return usageFor(stress);
	}

	public static double usageFor(int stress) {
		return stress * CMConfig.SERVER.motorFePerStress();
	}

	public static int maxStress() {
		return CMConfig.SERVER.motorMaxStress();
	}

	/** From the settings screen, checked on the server. */
	public void applySettings(int speed, int stress, boolean reversed) {
		this.speed = Mth.clamp(speed, MIN_SPEED, MAX_SPEED);
		this.stress = Mth.clamp(stress / STRESS_STEP * STRESS_STEP, STRESS_STEP, maxStress());
		this.reversed = reversed;
		setChanged();
		updateGeneratedRotation();
		sendData();
		if (level != null)
			level.playSound(null, worldPosition, ModSounds.motor("adjust"), SoundSource.BLOCKS, 0.6f, 1);
	}

	/**
	 * Picks its turning up again on its first tick, like Create's Creative Motor. Being moved (made into a Sable physics
	 * object, or put down by a contraption) wipes a kinetic block's speed, but leaves this one running, so nothing else
	 * would set it turning again.
	 */
	@Override
	public void initialize() {
		super.initialize();
		// Restart when its speed isn't what it should make (a move wiped it), or a slower source drives it; a plain
		// reload already has its speed back, so it doesn't recompute the whole network for nothing
		float generated = getGeneratedSpeed(), theoretical = getTheoreticalSpeed();
		if (hasSource() ? Math.abs(generated) > Math.abs(theoretical) : generated != theoretical)
			updateGeneratedRotation();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			clientTicker.accept(this);
			return;
		}
		double need = usagePerTick();
		boolean canRun = energy.getEnergyStored() >= (running ? owed + need : Math.max(need * 10, Math.min(energy.getMaxEnergyStored() / 4.0, need * RESTART_TICKS)));
		if (canRun) {
			owed += need;
			int take = (int) owed;
			owed -= energy.consume(take);
		} else {
			owed = 0;
		}
		if (canRun != running) {
			running = canRun;
			updateGeneratedRotation();
			if (running && stress >= maxStress())
				ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.MOTOR_FULL_POWER, ItemStack.EMPTY, 0);
			BlockState state = getBlockState();
			if (state.getValue(ElectricMotorBlock.POWERED) != running)
				level.setBlock(worldPosition, state.setValue(ElectricMotorBlock.POWERED, running), 3);
		}
		// Keep the goggles' charge reading fresh without flooding packets
		if (level.getGameTime() % 10 == 0 && Math.abs(energy.getEnergyStored() - syncedEnergy) > energy.getMaxEnergyStored() / 100) {
			syncedEnergy = energy.getEnergyStored();
			sendData();
		}
	}

	@Override
	public float getGeneratedSpeed() {
		if (!running)
			return 0;
		return convertToDirection(reversed ? -speed : speed, getBlockState().getValue(ElectricMotorBlock.FACING));
	}

	/** Capacity per RPM: the set stress is what it gives at any speed. */
	@Override
	public float calculateAddedStressCapacity() {
		float capacity = running ? (float) stress / speed : 0;
		lastCapacityProvided = capacity;
		return capacity;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.electric_motor").forGoggles(tooltip);
		lang().text(String.format(Locale.ROOT, "%,d / %,d FE", energy.getEnergyStored(), energy.getMaxEnergyStored()))
			.style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		if (running)
			lang().translate("gui.goggles.electric_motor.using", String.format(Locale.ROOT, "%,.0f", usagePerTick()))
				.style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
		else
			lang().translate("gui.goggles.electric_motor.no_power", String.format(Locale.ROOT, "%,.0f", usagePerTick()))
				.style(ChatFormatting.RED).forGoggles(tooltip, 1);
		reading.addTo(tooltip, level, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("Energy", energy.getEnergyStored());
		tag.putInt("SpeedSetting", speed);
		tag.putInt("StressSetting", stress);
		tag.putBoolean("Reversed", reversed);
		tag.putBoolean("Running", running);
		reading.write(tag);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		energy.setEnergy(tag.getInt("Energy"));
		if (tag.contains("SpeedSetting")) {
			// Clamped like the screen's settings: a speed of 0 would give infinite stress capacity, and the most
			// stress may have been lowered in the config since it was saved
			speed = Mth.clamp(tag.getInt("SpeedSetting"), MIN_SPEED, MAX_SPEED);
			stress = Mth.clamp(tag.getInt("StressSetting") / STRESS_STEP * STRESS_STEP, STRESS_STEP, maxStress());
		}
		reversed = tag.getBoolean("Reversed");
		running = tag.getBoolean("Running");
		reading.read(tag);
	}
}
