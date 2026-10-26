package com.comedymonkey.createaddons.content.electric;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.SaveMark;
import com.comedymonkey.createaddons.content.line.PowerGrid;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Burns lava flat out while it has any, filling its energy store (what doesn't fit is lost), and pushes that energy out
 * of its side terminals into whatever takes it (an Electric Motor, a Power Line, a battery). While its chunk isn't
 * running, its line runs it on paper from its record ({@link PowerGrid}).
 */
public class LavaGeneratorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, PowerGrid.Generator, com.comedymonkey.createaddons.content.line.NetworkReading.Shown {
	private static final int TANK = 8000;
	private static final int BUFFER = 100_000;
	/** Ticks the window keeps showing lava once the tank runs dry, so a trickle of lava doesn't flicker it. */
	static final int FUELLED_HOLD_TICKS = 40;

	private final FluidTank tank = new FluidTank(TANK, stack -> stack.getFluid().isSame(Fluids.LAVA)) {
		@Override
		protected void onContentsChanged() {
			SaveMark.mark(LavaGeneratorBlockEntity.this);
			syncQueued = true;
		}
	};
	private final EnergyBuffer energy = new EnergyBuffer(BUFFER, 0, BUFFER, () -> SaveMark.mark(this));
	/** What pipes and cables see: its lava can't be drawn out, nor its power taken, while it's on paper. */
	private final net.neoforged.neoforge.energy.IEnergyStorage energyOut = energy.extractOnly(() -> PowerGrid.givesHere(level, worldPosition));
	private final net.neoforged.neoforge.fluids.capability.IFluidHandler fluidsOut = new net.neoforged.neoforge.fluids.capability.IFluidHandler() {
		@Override
		public int getTanks() {
			return tank.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int i) {
			return tank.getFluidInTank(i);
		}

		@Override
		public int getTankCapacity(int i) {
			return tank.getTankCapacity(i);
		}

		@Override
		public boolean isFluidValid(int i, FluidStack stack) {
			return tank.isFluidValid(i, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return tank.fill(resource, action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return PowerGrid.givesHere(level, worldPosition) ? tank.drain(resource, action) : FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return PowerGrid.givesHere(level, worldPosition) ? tank.drain(maxDrain, action) : FluidStack.EMPTY;
		}
	};

	public net.neoforged.neoforge.energy.IEnergyStorage getEnergyOut() {
		return energyOut;
	}

	public net.neoforged.neoforge.fluids.capability.IFluidHandler getFluidsOut() {
		return fluidsOut;
	}

	/** Its Power Line network's figures, for the goggles. */
	private final com.comedymonkey.createaddons.content.line.NetworkReading reading = new com.comedymonkey.createaddons.content.line.NetworkReading();
	/** Server: the lava the Fluid Lines bring it, from each source, for its record. */
	private final PowerGrid.Feeds feeds = new PowerGrid.Feeds(1);
	/** Server: what's on each terminal (clockwise of the front, then the other side), found again when it's turned. */
	@Nullable
	private BlockCapabilityCache<IEnergyStorage, @Nullable Direction> rightTerminal, leftTerminal;
	/** Ticks the tank has been empty, for the window's lava. */
	private int dryTicks;
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
			&& old.getValue(LavaGeneratorBlock.FACING) != state.getValue(LavaGeneratorBlock.FACING)) {
			invalidateCapabilities();
			rightTerminal = leftTerminal = null;
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	public FluidTank getTank() {
		return tank;
	}

	/** Half the difference to a generator whose terminal touches this one's on {@code side}, if it holds less. */
	private void share(Direction side) {
		BlockPos next = worldPosition.relative(side);
		if (level == null || !level.isLoaded(next) || !(level.getBlockEntity(next) instanceof LavaGeneratorBlockEntity other)
			|| !LavaGeneratorBlock.isTerminal(other.getBlockState(), side.getOpposite()))
			return;
		int move = (energy.getEnergyStored() - other.energy.getEnergyStored()) / 2;
		if (move <= 0)
			return;
		move = Math.min(move, other.energy.getMaxEnergyStored() - other.energy.getEnergyStored());
		energy.setEnergy(energy.getEnergyStored() - move);
		other.energy.setEnergy(other.energy.getEnergyStored() + move);
		SaveMark.mark(this);
		SaveMark.mark(other);
	}

	// ---------- its record, for running on paper while its chunk isn't running ----------

	@Override
	public PowerGrid.Paper newPaper() {
		return new PowerGrid.Paper("lava_generator", 1);
	}

	@Override
	public void record(PowerGrid.Paper p, boolean refresh) {
		p.set(CMConfig.SERVER.generatorFePerTick(), energy.getEnergyStored(), BUFFER);
		p.fuel(0).set(tank.getFluidAmount(), TANK, CMConfig.SERVER.generatorLavaPerSecond() / 20.0);
		if (refresh) {
			feeds.into(p.fuel(0), 0, level);
			p.row(row());
		}
	}

	/** The generators whose terminals touch this one's (they share their stores, so they do on paper too). */
	private List<BlockPos> row() {
		List<BlockPos> out = new java.util.ArrayList<>();
		Direction side = getBlockState().getValue(LavaGeneratorBlock.FACING).getClockWise();
		for (Direction d : new Direction[] {side, side.getOpposite()}) {
			BlockPos next = worldPosition.relative(d);
			if (level != null && level.isLoaded(next) && level.getBlockEntity(next) instanceof LavaGeneratorBlockEntity other
				&& LavaGeneratorBlock.isTerminal(other.getBlockState(), d.getOpposite()))
				out.add(next);
		}
		return out;
	}

	/** Back from paper: its store and lava move by as much as its record's did (anything else that reached it stays). */
	@Override
	public void resume(double storeChange, double[] fuelChange) {
		energy.setEnergy((int) Math.max(0, Math.min(BUFFER, energy.getEnergyStored() + Math.round(storeChange))));
		int lava = (int) Math.round(fuelChange[0]);
		if (lava > 0)
			tank.fill(new FluidStack(Fluids.LAVA, lava), FluidTank.FluidAction.EXECUTE);
		else if (lava < 0)
			tank.drain(-lava, FluidTank.FluidAction.EXECUTE);
		SaveMark.mark(this);
	}

	@Override
	public void fed(BlockPos source, net.minecraft.world.level.material.Fluid fluid, int amount) {
		if (fluid.isSame(Fluids.LAVA))
			feeds.add(source, 0, amount);
	}

	@Override
	public void networkReading(int makes, int uses) {
		if (level != null && reading.set(makes, uses, level.getGameTime()))
			syncQueued = true;
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
		// running again after being on paper: it takes on what its record came to
		if (level instanceof ServerLevel server)
			PowerGrid.of(server).back(this, server);
		// Out of both terminals first, then make up what was taken (up to full output)
		BlockState state = getBlockState();
		Direction side = state.getValue(LavaGeneratorBlock.FACING).getClockWise();
		if (rightTerminal == null && level instanceof ServerLevel server) {
			rightTerminal = EnergyBuffer.cacheFor(server, worldPosition, side);
			leftTerminal = EnergyBuffer.cacheFor(server, worldPosition, side.getOpposite());
		}
		energy.pushTo(rightTerminal, energy.getEnergyStored());
		energy.pushTo(leftTerminal, energy.getEnergyStored());
		// generators whose terminals touch even out their stores, so a cable on any one draws on the whole row
		share(side);
		share(side.getOpposite());
		// flat out while there's lava: what the store can't hold is lost
		int make = tank.isEmpty() ? 0 : CMConfig.SERVER.generatorFePerTick();
		if (make > 0) {
			burnProgress += CMConfig.SERVER.generatorLavaPerSecond();
			int burn = (int) (burnProgress / 20);
			if (burn > 0) {
				burnProgress -= burn * 20;
				tank.drain(burn, FluidTank.FluidAction.EXECUTE);
			}
			energy.generate(Math.min(make, energy.getMaxEnergyStored() - energy.getEnergyStored()));
			activeTicks = 40;
		} else if (activeTicks > 0) {
			activeTicks--;
		}
		output = output * 0.95f + make * 0.05f;
		boolean canRun = activeTicks > 0;

		running = canRun;
		// The window shows the lava whenever there's some in the tank (and a moment after it runs dry, so a trickle
		// doesn't flicker it), and the fire while it burns
		dryTicks = tank.isEmpty() ? Math.min(dryTicks + 1, FUELLED_HOLD_TICKS + 1) : 0;
		boolean fuelled = showsLava(state.getValue(LavaGeneratorBlock.FUELLED), !tank.isEmpty(), dryTicks);
		if (state.getValue(LavaGeneratorBlock.LIT) != running || state.getValue(LavaGeneratorBlock.FUELLED) != fuelled)
			level.setBlock(worldPosition, state.setValue(LavaGeneratorBlock.LIT, running).setValue(LavaGeneratorBlock.FUELLED, fuelled), 3);
		if (level.getGameTime() % 10 == 0
			&& (syncQueued || running && level.getGameTime() % 40 == 0
			|| Math.abs(energy.getEnergyStored() - syncedEnergy) > BUFFER / 50)) {
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

	/** The window's lava: on as soon as there's lava, off once the tank has been dry for {@link #FUELLED_HOLD_TICKS}. */
	static boolean showsLava(boolean showing, boolean hasLava, int dryTicks) {
		if (hasLava)
			return true;
		return showing && dryTicks <= FUELLED_HOLD_TICKS;
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
		// it runs flat out while it has lava: a full store means what it makes now is lost
		boolean full = lit && energy.getEnergyStored() >= BUFFER * 0.99;
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate(full ? "gui.goggles.lava_generator.full" : lit ? "gui.goggles.lava_generator.running"
					: "gui.goggles.lava_generator.no_lava",
				String.format(Locale.ROOT, "%,.0f", output))
			.style(lit && !full ? ChatFormatting.GREEN : ChatFormatting.RED)
			.forGoggles(tooltip, 1);
		reading.addTo(tooltip, level, 1);
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
		reading.write(tag);
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
		reading.read(tag);
	}
}
