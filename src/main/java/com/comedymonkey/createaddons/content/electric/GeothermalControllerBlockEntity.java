package com.comedymonkey.createaddons.content.electric;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.SaveMark;
import com.comedymonkey.createaddons.content.line.PowerGrid;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Runs the Geothermal Turbine: checks the 3x3x3 behind it once a second, and while it's formed and has both lava and
 * water, burns them (lava heats water to steam, the steam turns the turbine) for a lot of FE, always at the power level
 * set on it (what its store can't hold is lost). While its chunk isn't running, its line runs it on paper. Pipes and energy pipes
 * reach it through any casing; it pushes power into anything that takes it touching the outside of the turbine.
 */
public class GeothermalControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, PowerGrid.Generator, com.comedymonkey.createaddons.content.line.NetworkReading.Shown {
	public static final int TANK = 16000;
	private static final int BUFFER = 4_000_000;

	private final FluidTank lava = tank(stack -> stack.getFluid().isSame(Fluids.LAVA));
	private final FluidTank water = tank(stack -> stack.getFluid().isSame(Fluids.WATER));
	private final EnergyBuffer energy = new EnergyBuffer(BUFFER, 0, BUFFER, () -> SaveMark.mark(this));
	private final IFluidHandler fluids = new Inlets();
	/** Its power can't be taken from the block while it's on paper (its record is what runs it then). */
	private final IEnergyStorage energyOut = energy.extractOnly(() -> PowerGrid.givesHere(level, worldPosition));
	private boolean formed;
	private boolean running;
	private boolean checkQueued = true;
	/** The power level set on it, in tenths of full output. */
	private int outputTenths = 10;
	/** Its Power Line network's figures, for the goggles. */
	private final com.comedymonkey.createaddons.content.line.NetworkReading reading = new com.comedymonkey.createaddons.content.line.NetworkReading();
	/** Server: the lava and water the Fluid Lines bring it, from each source, for its record. */
	private final PowerGrid.Feeds feeds = new PowerGrid.Feeds(2);
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
	/** Server: what's under each port flange, found when it forms (and looked up again only when it changes). */
	private final List<BlockCapabilityCache<IEnergyStorage, @Nullable Direction>> neighbours = new ArrayList<>();

	public GeothermalControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GEOTHERMAL_CONTROLLER.get(), pos, state);
	}

	private FluidTank tank(java.util.function.Predicate<FluidStack> valid) {
		return new FluidTank(TANK, valid) {
			@Override
			protected void onContentsChanged() {
				SaveMark.mark(GeothermalControllerBlockEntity.this);
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

	// ---------- its power level ----------

	/** Tenths of full output it's set to, 1 to 10 (full unless turned down); lava and water use go with it. */
	public int outputTenths() {
		return Math.clamp(outputTenths, 1, 10);
	}

	/** FE a tick it makes at its level, from the config's full output. */
	public int outputSetting() {
		return (int) Math.round(Math.max(0, CMConfig.SERVER.turbineFePerTick()) * outputTenths() / 10.0);
	}

	/** From its settings screen, checked on the server. */
	public void applyOutput(int tenths) {
		if (Math.clamp(tenths, 1, 10) == outputTenths)
			return;
		outputTenths = Math.clamp(tenths, 1, 10);
		syncQueued = true;
		SaveMark.mark(this);
	}

	/** How much of full output it's set to, 0 to 1: what its lava and water use scale by. */
	private double setShare() {
		return outputTenths() / 10.0;
	}

	// ---------- its record, for running on paper while its chunk isn't running ----------

	@Override
	public PowerGrid.Paper newPaper() {
		return new PowerGrid.Paper("geothermal_turbine", 2);
	}

	@Override
	public void record(PowerGrid.Paper p, boolean refresh) {
		p.set(formed ? outputSetting() : 0, energy.getEnergyStored(), BUFFER);
		p.fuel(0).set(lava.getFluidAmount(), TANK, CMConfig.SERVER.turbineLavaPerSecond() / 20.0 * setShare());
		p.fuel(1).set(water.getFluidAmount(), TANK, CMConfig.SERVER.turbineWaterPerSecond() / 20.0 * setShare());
		if (refresh) {
			feeds.into(p.fuel(0), 0, level);
			feeds.into(p.fuel(1), 1, level);
		}
	}

	/** Back from paper: its store, lava and water move by as much as its record's did (anything else that reached it stays). */
	@Override
	public void resume(double storeChange, double[] fuelChange) {
		energy.setEnergy((int) Math.max(0, Math.min(BUFFER, energy.getEnergyStored() + Math.round(storeChange))));
		move(lava, Fluids.LAVA, fuelChange[0]);
		move(water, Fluids.WATER, fuelChange[1]);
		SaveMark.mark(this);
	}

	private static void move(FluidTank tank, net.minecraft.world.level.material.Fluid fluid, double change) {
		int mb = (int) Math.round(change);
		if (mb > 0)
			tank.fill(new FluidStack(fluid, mb), IFluidHandler.FluidAction.EXECUTE);
		else if (mb < 0)
			tank.drain(-mb, IFluidHandler.FluidAction.EXECUTE);
	}

	@Override
	public void networkReading(int makes, int uses) {
		if (level != null && reading.set(makes, uses, level.getGameTime()))
			syncQueued = true;
	}

	/** Lava and water the Fluid Lines bring its port casings, from each source, for its record. */
	@Override
	public void fed(BlockPos source, net.minecraft.world.level.material.Fluid fluid, int amount) {
		feeds.add(source, fluid.isSame(Fluids.LAVA) ? 0 : fluid.isSame(Fluids.WATER) ? 1 : -1, amount);
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

	/** Running with its store full: what it makes now is lost (its stall lamp and sound). */
	public boolean isStalled() {
		return formed && running && energy.getEnergyStored() >= BUFFER * 0.99;
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
		// running again after being on paper: it takes on what its record came to
		if (level instanceof ServerLevel server)
			PowerGrid.of(server).back(this, server);
		if (checkQueued || level.getGameTime() % 20 == 0) {
			checkQueued = false;
			setFormed(structureIsWhole());
		}
		int make = 0;
		if (formed) {
			// Out to everything touching it first, then make up what was taken (up to full output)
			for (int i = 0; i < neighbours.size() && energy.getEnergyStored() > 0; i++)
				energy.pushTo(neighbours.get(i), energy.getEnergyStored());
			// always at its set level while it has both: what the store can't hold is lost
			if (!lava.isEmpty() && !water.isEmpty())
				make = outputSetting();
			if (make > 0) {
				double share = setShare();
				lavaProgress += CMConfig.SERVER.turbineLavaPerSecond() * share;
				waterProgress += CMConfig.SERVER.turbineWaterPerSecond() * share;
				int lavaBurn = (int) (lavaProgress / 20), waterBurn = (int) (waterProgress / 20);
				lava.drain(lavaBurn, IFluidHandler.FluidAction.EXECUTE);
				water.drain(waterBurn, IFluidHandler.FluidAction.EXECUTE);
				lavaProgress -= lavaBurn * 20;
				waterProgress -= waterBurn * 20;
				energy.generate(Math.min(make, energy.getMaxEnergyStored() - energy.getEnergyStored()));
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
		if (level instanceof ServerLevel server) {
			if (Math.floorMod(worldPosition.hashCode() + server.getGameTime(), 20) == 0)
				feeds.update(server.getGameTime());
			PowerGrid.of(server).note(this, server);
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
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
			BlockPos pos = p.immutable();
			BlockState state = level.getBlockState(pos);
			if (state.hasProperty(GeothermalPartBlock.FORMED) && state.getValue(GeothermalPartBlock.FORMED) != now)
				level.setBlock(pos, state.setValue(GeothermalPartBlock.FORMED, now), 3);
			if (level.getBlockEntity(pos) instanceof GeothermalCasingBlockEntity casing)
				casing.setController(now ? worldPosition : null);
			// Power goes out only through the port flanges under the base: the bottom face of the bottom layer
			if (now && pos.getY() == centre.getY() - 1 && level instanceof ServerLevel server)
				neighbours.add(EnergyBuffer.cacheFor(server, pos, Direction.DOWN));
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
		String key = isStalled() ? "full" : running ? "running" : lava.isEmpty() ? "no_lava" : "no_water";
		lang().translate("gui.goggles.geothermal_turbine." + key, String.format(Locale.ROOT, "%,.0f", output))
			.style(running && !isStalled() ? ChatFormatting.GREEN : ChatFormatting.RED).forGoggles(tooltip, 1);
		lang().translate("gui.goggles.geothermal_turbine.level", outputTenths() * 10).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		reading.addTo(tooltip, level, 1);
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
		tag.putInt("OutputTenths", outputTenths);
		reading.write(tag);
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
		outputTenths = tag.contains("OutputTenths") ? tag.getInt("OutputTenths") : 10;
		reading.read(tag);
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
