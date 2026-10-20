package com.comedymonkey.createaddons.content.electric;

import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Draws {@code stress / 16} FE a tick (configurable) and, while it has the power, turns at its set speed with its set
 * stress capacity. Short of power it stops, and starts again once it has half a second's worth stored.
 */
public class ElectricMotorBlockEntity extends GeneratingKineticBlockEntity {
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

	public ElectricMotorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ELECTRIC_MOTOR.get(), pos, state);
		// A second's worth at full stress, so a little gap in supply doesn't stall it
		int buffer = Math.max(100_000, (int) Math.ceil(CMConfig.SERVER.motorMaxStress() * CMConfig.SERVER.motorFePerStress() * 20));
		energy = new EnergyBuffer(buffer, buffer, 0, this::setChanged);
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
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		double need = usagePerTick();
		boolean canRun = energy.getEnergyStored() >= (running ? owed + need : need * 10);
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
	}
}
