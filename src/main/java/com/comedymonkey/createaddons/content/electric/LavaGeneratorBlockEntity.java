package com.comedymonkey.createaddons.content.electric;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Burns lava to fill its energy store, and pushes that energy out of its side terminals into whatever takes it
 * (an Electric Motor, a Pipez energy pipe, a battery). It only burns while it has room for the energy.
 */
public class LavaGeneratorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	private static final int TANK = 8000;
	private static final int BUFFER = 100_000;

	private final FluidTank tank = new FluidTank(TANK, stack -> stack.getFluid().isSame(Fluids.LAVA)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
			syncQueued = true;
		}
	};
	private final EnergyBuffer energy = new EnergyBuffer(BUFFER, 0, BUFFER, this::setChanged);
	/** Lava owed, in twentieths of a millibucket: burning is in proportion to what's made. */
	private double burnProgress;
	/** Stays lit this many more ticks after it last made power, so a light load doesn't make it flicker. */
	private int activeTicks;
	/** Recent FE a tick, smoothed, for the goggles. */
	private float output;
	private boolean running;
	private boolean syncQueued;
	private int syncedEnergy = -1;
	/** Client: the turbine wheel's angle and speed, so it spins up and runs down rather than snapping. */
	private float rotorAngle;
	private float rotorAngleLast;
	private float rotorSpeed;
	/** Degrees a tick flat out; at a light load the wheel turns at a quarter of this. */
	private static final float FULL_SPEED = 24;
	/** Client: run every client tick (the generator's sounds); set by the client setup. */
	public static Consumer<LavaGeneratorBlockEntity> clientTicker = be -> {};

	public LavaGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LAVA_GENERATOR.get(), pos, state);
	}

	/** Like vanilla's chest: a wrench turning it moves the sides its pipes and cables connect on, so they look again. */
	@Override
	public void setBlockState(BlockState state) {
		BlockState old = getBlockState();
		super.setBlockState(state);
		if (old.hasProperty(LavaGeneratorBlock.FACING) && state.hasProperty(LavaGeneratorBlock.FACING)
			&& old.getValue(LavaGeneratorBlock.FACING) != state.getValue(LavaGeneratorBlock.FACING))
			invalidateCapabilities();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	public FluidTank getTank() {
		return tank;
	}

	public EnergyBuffer getEnergy() {
		return energy;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			rotorAngleLast = rotorAngle;
			// The wheel follows the load: a quarter speed at a light load, full speed flat out
			float target = isLit() ? FULL_SPEED * (0.25f + 0.75f * load()) : 0;
			rotorSpeed = Mth.approach(rotorSpeed, target, rotorSpeed < target ? 0.6f : 0.3f);
			rotorAngle = (rotorAngle + rotorSpeed) % 360;
			clientTicker.accept(this);
			return;
		}
		// Out of both terminals first, then make up what was taken (up to full output)
		BlockState state = getBlockState();
		Direction side = state.getValue(LavaGeneratorBlock.FACING).getClockWise();
		energy.pushTo(level, worldPosition, side, energy.getEnergyStored());
		energy.pushTo(level, worldPosition, side.getOpposite(), energy.getEnergyStored());
		int fe = CMConfig.SERVER.generatorFePerTick();
		int make = tank.isEmpty() ? 0 : Math.min(fe, energy.getMaxEnergyStored() - energy.getEnergyStored());
		if (make > 0) {
			burnProgress += CMConfig.SERVER.generatorLavaPerSecond() * (double) make / fe;
			int burn = (int) (burnProgress / 20);
			if (burn > 0) {
				burnProgress -= burn * 20;
				tank.drain(burn, FluidTank.FluidAction.EXECUTE);
			}
			energy.generate(make);
			activeTicks = 40;
		} else if (activeTicks > 0) {
			activeTicks--;
		}
		output = output * 0.95f + make * 0.05f;
		boolean canRun = activeTicks > 0;

		running = canRun;
		// The window shows the lava whenever there's some in the tank, and the fire while it burns
		boolean fuelled = !tank.isEmpty();
		if (state.getValue(LavaGeneratorBlock.LIT) != running || state.getValue(LavaGeneratorBlock.FUELLED) != fuelled)
			level.setBlock(worldPosition, state.setValue(LavaGeneratorBlock.LIT, running).setValue(LavaGeneratorBlock.FUELLED, fuelled), 3);
		if (level.getGameTime() % 10 == 0
			&& (syncQueued || running && level.getGameTime() % 40 == 0
			|| Math.abs(energy.getEnergyStored() - syncedEnergy) > BUFFER / 50)) {
			syncQueued = false;
			syncedEnergy = energy.getEnergyStored();
			sendData();
		}
	}

	public boolean isLit() {
		return getBlockState().getValue(LavaGeneratorBlock.LIT);
	}

	/** How much of its full output is being taken, 0 to 1 (smoothed; synced to the client). */
	public float load() {
		return Mth.clamp(output / CMConfig.SERVER.generatorFePerTick(), 0, 1);
	}

	/** Client: the wheel's speed, 0 (still) to 1 (flat out). */
	public float rotorSpeed() {
		return rotorSpeed / FULL_SPEED;
	}

	/** Degrees, smoothed between ticks. */
	public float getRotorAngle(float partialTicks) {
		float last = rotorAngleLast, now = rotorAngle;
		if (now < last)
			now += 360;
		return Mth.lerp(partialTicks, last, now);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		LangBuilder lang = new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
		lang.translate("gui.goggles.lava_generator").forGoggles(tooltip);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.text(String.format(Locale.ROOT, "%,d / %,d mB ", tank.getFluidAmount(), TANK)).style(ChatFormatting.GOLD)
			.add(new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).add(Fluids.LAVA.getFluidType().getDescription().copy())
				.style(ChatFormatting.GRAY))
			.forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.text(String.format(Locale.ROOT, "%,d / %,d FE", energy.getEnergyStored(), BUFFER)).style(ChatFormatting.AQUA)
			.forGoggles(tooltip, 1);
		boolean lit = getBlockState().getValue(LavaGeneratorBlock.LIT);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate(lit ? "gui.goggles.lava_generator.running" : tank.isEmpty()
					? "gui.goggles.lava_generator.no_lava" : "gui.goggles.lava_generator.full",
				String.format(Locale.ROOT, "%,.0f", output))
			.style(lit ? ChatFormatting.GREEN : ChatFormatting.RED)
			.forGoggles(tooltip, 1);
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
		tag.putInt("Energy", energy.getEnergyStored());
		tag.putDouble("BurnProgress", burnProgress);
		tag.putInt("ActiveTicks", activeTicks);
		tag.putFloat("Output", output);
		tag.putBoolean("Running", running);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		tank.setFluid(FluidStack.EMPTY);
		tank.readFromNBT(registries, tag.getCompound("Tank"));
		energy.setEnergy(tag.getInt("Energy"));
		burnProgress = tag.getDouble("BurnProgress");
		activeTicks = tag.getInt("ActiveTicks");
		output = tag.getFloat("Output");
		running = tag.getBoolean("Running");
	}
}
