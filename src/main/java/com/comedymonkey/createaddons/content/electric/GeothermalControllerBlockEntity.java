package com.comedymonkey.createaddons.content.electric;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModSounds;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Runs the Geothermal Turbine: checks the 3x3x3 behind it once a second, and while it's formed and has both lava and
 * water, burns them (lava heats water to steam, the steam turns the turbine) for a lot of FE. Pipes and energy pipes
 * reach it through any casing; it pushes power into anything that takes it touching the outside of the turbine.
 */
public class GeothermalControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
	public static final int TANK = 16000;
	private static final int BUFFER = 4_000_000;

	private final FluidTank lava = tank(stack -> stack.getFluid().isSame(Fluids.LAVA));
	private final FluidTank water = tank(stack -> stack.getFluid().isSame(Fluids.WATER));
	private final EnergyBuffer energy = new EnergyBuffer(BUFFER, 0, BUFFER, this::setChanged);
	private final IFluidHandler fluids = new Inlets();
	private final IEnergyStorage energyOut = energy.extractOnly();
	private boolean formed;
	private boolean running;
	private boolean checkQueued = true;
	/** Lava and water owed, in twentieths of a millibucket: both are used in proportion to what's made. */
	private double lavaProgress;
	private double waterProgress;
	/** Stays lit this many more ticks after it last made power, so a light load doesn't make it flicker. */
	private int activeTicks;
	/** Recent FE a tick, smoothed, for the goggles. */
	private float output;
	private boolean syncQueued;
	private int syncedEnergy = -1;
	/** The rotor, client side: its angle (degrees) and its speed (0 still to 1 flat out), easing after the output. */
	private float rotorAngle, prevRotorAngle, rotorSpeed;
	/** How far the rotor turns a tick flat out, and the slowest it turns while it's making any power at all. */
	private static final float SPIN_DEGREES = 30, MIN_RUNNING_SPEED = 0.25f;
	/** Set by the client: the turbine's sounds, run each client tick. */
	public static Consumer<GeothermalControllerBlockEntity> clientTicker = be -> {};
	/** Blocks touching the outside of the turbine, and the side of them that faces it. */
	private final List<BlockPos> neighbours = new ArrayList<>();
	private final List<Direction> neighbourSides = new ArrayList<>();

	public GeothermalControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GEOTHERMAL_CONTROLLER.get(), pos, state);
	}

	private FluidTank tank(java.util.function.Predicate<FluidStack> valid) {
		return new FluidTank(TANK, valid) {
			@Override
			protected void onContentsChanged() {
				setChanged();
				syncQueued = true;
			}
		};
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	public boolean isFormed() {
		return formed;
	}

	public IFluidHandler getFluids() {
		return fluids;
	}

	public IEnergyStorage getEnergyOut() {
		return energyOut;
	}

	public float lavaFill() {
		return (float) lava.getFluidAmount() / TANK;
	}

	public float waterFill() {
		return (float) water.getFluidAmount() / TANK;
	}

	public boolean isRunning() {
		return running;
	}

	/** Formed with both lava and water, but not running: the power isn't being taken ("Full"). */
	public boolean isStalled() {
		return formed && !running && !lava.isEmpty() && !water.isEmpty();
	}

	/** How hard it's working, 0 to 1, from the smoothed output. */
	public float load() {
		return Math.min(1, output / CMConfig.SERVER.turbineFePerTick());
	}

	public float energyFill() {
		return (float) energy.getEnergyStored() / BUFFER;
	}

	public float rotorAngle(float partialTicks) {
		float delta = rotorAngle - prevRotorAngle;
		if (delta < 0)
			delta += 360;
		return prevRotorAngle + delta * partialTicks;
	}

	public float rotorSpeed() {
		return rotorSpeed;
	}

	public void queueCheck() {
		checkQueued = true;
	}

	/** The middle block of the turbine. */
	public BlockPos centre() {
		return worldPosition.relative(getBlockState().getValue(GeothermalControllerBlock.FACING).getOpposite());
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(centre()).inflate(1.6);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			tickRotor();
			clientTicker.accept(this);
			return;
		}
		if (checkQueued || level.getGameTime() % 20 == 0) {
			checkQueued = false;
			setFormed(structureIsWhole());
		}
		int make = 0;
		if (formed) {
			// Out to everything touching it first, then make up what was taken (up to full output)
			for (int i = 0; i < neighbours.size() && energy.getEnergyStored() > 0; i++)
				energy.pushTo(level, neighbours.get(i), neighbourSides.get(i), energy.getEnergyStored());
			int fe = CMConfig.SERVER.turbineFePerTick();
			if (!lava.isEmpty() && !water.isEmpty())
				make = Math.min(fe, energy.getMaxEnergyStored() - energy.getEnergyStored());
			if (make > 0) {
				double share = (double) make / fe;
				lavaProgress += CMConfig.SERVER.turbineLavaPerSecond() * share;
				waterProgress += CMConfig.SERVER.turbineWaterPerSecond() * share;
				int lavaBurn = (int) (lavaProgress / 20), waterBurn = (int) (waterProgress / 20);
				lava.drain(lavaBurn, IFluidHandler.FluidAction.EXECUTE);
				water.drain(waterBurn, IFluidHandler.FluidAction.EXECUTE);
				lavaProgress -= lavaBurn * 20;
				waterProgress -= waterBurn * 20;
				energy.generate(make);
				activeTicks = 40;
			}
		}
		if (make == 0 && activeTicks > 0)
			activeTicks--;
		if (!formed)
			activeTicks = 0;
		output = output * 0.95f + make * 0.05f;
		boolean canRun = activeTicks > 0;
		if (canRun != running) {
			running = canRun;
			BlockState state = getBlockState();
			if (state.getValue(GeothermalControllerBlock.LIT) != running)
				level.setBlock(worldPosition, state.setValue(GeothermalControllerBlock.LIT, running), 3);
		}
		if (level.getGameTime() % 10 == 0 && (syncQueued || running && level.getGameTime() % 40 == 0
			|| Math.abs(energy.getEnergyStored() - syncedEnergy) > BUFFER / 100)) {
			syncQueued = false;
			syncedEnergy = energy.getEnergyStored();
			sendData();
		}
	}

	/**
	 * Winds the rotor towards the speed the output asks for: up over about three seconds from rest, and coasting down
	 * a little quicker once the power stops (as in the approved run clip, design/models/geothermal-turbine.gen.js).
	 */
	private void tickRotor() {
		float target = formed && running ? MIN_RUNNING_SPEED + (1 - MIN_RUNNING_SPEED) * load() : 0;
		rotorSpeed += (target - rotorSpeed) * (target > rotorSpeed ? 0.02f : 0.03f);
		if (rotorSpeed < 0.002f && target == 0)
			rotorSpeed = 0;
		prevRotorAngle = rotorAngle;
		rotorAngle = (rotorAngle + rotorSpeed * SPIN_DEGREES) % 360;
	}

	/** Every block of the 3x3x3: the core in the middle, this controller in the front, casings everywhere else. */
	private boolean structureIsWhole() {
		BlockPos centre = centre();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
			if (pos.equals(worldPosition))
				continue;
			BlockState state = level.getBlockState(pos);
			if (!GeothermalPartBlock.isPart(state, !pos.equals(centre)))
				return false;
			// A casing already working for another formed turbine (two built overlapping) can't be shared
			if (level.getBlockEntity(pos) instanceof GeothermalCasingBlockEntity casing && casing.controller() != null
				&& !worldPosition.equals(casing.getController()))
				return false;
		}
		return true;
	}

	/**
	 * Forming or unforming changes what the controller and every casing offer pipes and cables, but NeoForge only
	 * looks again when a block entity comes or goes; caches (Create's pipes, most cable mods) would keep the old
	 * answer, even a dead controller's tanks.
	 */
	private void invalidateStructure() {
		BlockPos centre = centre();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1)))
			level.invalidateCapabilities(pos.immutable());
	}

	private void setFormed(boolean now) {
		// Unchanged, unless it loaded formed and hasn't worked out its neighbours yet
		if (now == formed && (!now || !neighbours.isEmpty()))
			return;
		// Loading formed also lands here (to find its neighbours); only a real change gets a sound
		boolean changed = now != formed;
		formed = now;
		BlockPos centre = centre();
		neighbours.clear();
		neighbourSides.clear();
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
			BlockPos pos = p.immutable();
			BlockState state = level.getBlockState(pos);
			if (state.hasProperty(GeothermalPartBlock.FORMED) && state.getValue(GeothermalPartBlock.FORMED) != now)
				level.setBlock(pos, state.setValue(GeothermalPartBlock.FORMED, now), 3);
			if (level.getBlockEntity(pos) instanceof GeothermalCasingBlockEntity casing)
				casing.setController(now ? worldPosition : null);
			// Power goes out only through the port flanges under the base: the bottom face of the bottom layer
			if (now && pos.getY() == centre.getY() - 1) {
				neighbours.add(pos);
				neighbourSides.add(Direction.DOWN);
			}
		}
		syncQueued = true;
		invalidateStructure();
		if (changed)
			playAtCentre(now ? "form" : "unform");
		if (now)
			ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.TURBINE_FORMED, ItemStack.EMPTY, 0);
	}

	/** The controller's going: free the casings. */
	public void unform() {
		if (formed && level != null) {
			formed = false;
			BlockPos centre = centre();
			for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
				BlockPos pos = p.immutable();
				if (pos.equals(worldPosition))
					continue;
				BlockState state = level.getBlockState(pos);
				if (state.hasProperty(GeothermalPartBlock.FORMED) && state.getValue(GeothermalPartBlock.FORMED))
					level.setBlock(pos, state.setValue(GeothermalPartBlock.FORMED, false), 3);
				if (level.getBlockEntity(pos) instanceof GeothermalCasingBlockEntity casing)
					casing.setController(null);
			}
			invalidateStructure();
			playAtCentre("unform");
		}
	}

	private void playAtCentre(String sound) {
		BlockPos centre = centre();
		level.playSound(null, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5, ModSounds.turbine(sound),
			SoundSource.BLOCKS, 1, 1);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.geothermal_turbine").forGoggles(tooltip);
		if (!formed) {
			lang().translate("gui.goggles.geothermal_turbine.unformed").style(ChatFormatting.RED).forGoggles(tooltip, 1);
			return true;
		}
		fluidLine(tooltip, lava, Fluids.LAVA.getFluidType().getDescription().copy());
		fluidLine(tooltip, water, Fluids.WATER.getFluidType().getDescription().copy());
		lang().text(String.format(Locale.ROOT, "%,d / %,d FE", energy.getEnergyStored(), BUFFER)).style(ChatFormatting.AQUA)
			.forGoggles(tooltip, 1);
		String key = running ? "running" : lava.isEmpty() ? "no_lava" : water.isEmpty() ? "no_water" : "full";
		lang().translate("gui.goggles.geothermal_turbine." + key, String.format(Locale.ROOT, "%,.0f", output))
			.style(running ? ChatFormatting.GREEN : ChatFormatting.RED).forGoggles(tooltip, 1);
		return true;
	}

	private static void fluidLine(List<Component> tooltip, FluidTank tank, Component name) {
		lang().text(String.format(Locale.ROOT, "%,d / %,d mB ", tank.getFluidAmount(), TANK)).style(ChatFormatting.GOLD)
			.add(lang().add(name.copy()).style(ChatFormatting.GRAY)).forGoggles(tooltip, 1);
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("Lava", lava.writeToNBT(registries, new CompoundTag()));
		tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
		tag.putInt("Energy", energy.getEnergyStored());
		tag.putBoolean("Formed", formed);
		tag.putBoolean("Running", running);
		tag.putDouble("LavaProgress", lavaProgress);
		tag.putDouble("WaterProgress", waterProgress);
		tag.putInt("ActiveTicks", activeTicks);
		tag.putFloat("Output", output);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		lava.setFluid(FluidStack.EMPTY);
		lava.readFromNBT(registries, tag.getCompound("Lava"));
		water.setFluid(FluidStack.EMPTY);
		water.readFromNBT(registries, tag.getCompound("Water"));
		energy.setEnergy(tag.getInt("Energy"));
		formed = tag.getBoolean("Formed");
		running = tag.getBoolean("Running");
		lavaProgress = tag.getDouble("LavaProgress");
		waterProgress = tag.getDouble("WaterProgress");
		activeTicks = tag.getInt("ActiveTicks");
		output = tag.getFloat("Output");
		checkQueued = true;
	}

	/** Lava and water in, each to its own tank; nothing comes back out. */
	private class Inlets implements IFluidHandler {
		@Override
		public int getTanks() {
			return 2;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return (tank == 0 ? lava : water).getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return TANK;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return (tank == 0 ? lava : water).isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			// A pipe still holding on to a broken turbine's inlets mustn't pour fluid into nothing
			if (!formed || isRemoved())
				return 0;
			if (lava.isFluidValid(resource))
				return lava.fill(resource, action);
			if (water.isFluidValid(resource))
				return water.fill(resource, action);
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return FluidStack.EMPTY;
		}
	}
}
