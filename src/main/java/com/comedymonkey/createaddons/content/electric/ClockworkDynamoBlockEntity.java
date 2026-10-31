package com.comedymonkey.createaddons.content.electric;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.FixedStress;
import com.comedymonkey.createaddons.content.SaveMark;
import com.comedymonkey.createaddons.content.line.NetworkReading;
import com.comedymonkey.createaddons.content.line.PowerGrid;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Takes the stress it's set to off its shaft, at any speed, and makes the FE an Electric Motor would draw for that
 * stress, times its efficiency (three quarters by default), so a motor and a dynamo can never run each other. What its
 * store can't hold is lost; it keeps taking its stress all the same, so the shaft network never flickers. Power goes
 * out through its base. While its chunk isn't running, its line runs it on paper from its record, counting only the share
 * of its shaft's power that comes from free sources (water wheels, windmills), so a far motor or engine can't run it for
 * nothing (design/specs/dynamo.md D11, D12).
 */
public class ClockworkDynamoBlockEntity extends KineticBlockEntity implements PowerGrid.Generator, NetworkReading.Shown {
	/** Stress is set in steps of this, as on the motor. */
	public static final int STRESS_STEP = ElectricMotorBlockEntity.STRESS_STEP;
	/** Shaft power sources that cost nothing to run, so they keep a far dynamo going on paper. */
	public static final TagKey<Block> FREE_SOURCES = TagKey.create(Registries.BLOCK, ComedyMonkeysCreateAddons.asResource("free_kinetic_sources"));

	private final EnergyBuffer energy;
	/** What pipes and cables see: power can only be taken out, and none while it's on paper. */
	private final IEnergyStorage energyOut;
	private int stress = STRESS_STEP;
	/** Making power this tick: its shaft turns and isn't overstressed. */
	private boolean working;
	/** Fractions of an FE made on earlier ticks. */
	private double carry;
	/**
	 * Server: its shaft network's free sources (water wheels, windmills) and the share of its output each gives, as last
	 * worked out (about once a second, and at once when it starts). Saved, so its record is right from its first tick.
	 */
	private Map<BlockPos, Double> freeSources = Map.of();
	private int syncedEnergy = -1;
	/** Server: what's against its base, and which way that was when it was looked up. */
	@Nullable
	private BlockCapabilityCache<IEnergyStorage, @Nullable Direction> base;
	@Nullable
	private Direction baseSide;
	/** Its Power Line network's figures, for the goggles. */
	private final NetworkReading reading = new NetworkReading();

	/** Client: the aerial on its top. */
	public final DynamoAerial aerial = new DynamoAerial();
	/** Client: run every client tick (the dynamo's sounds); set by the client setup. */
	public static Consumer<ClockworkDynamoBlockEntity> clientTicker = be -> {};

	public ClockworkDynamoBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CLOCKWORK_DYNAMO.get(), pos, state);
		int cap = storeSize();
		energy = new EnergyBuffer(cap, 0, cap, () -> SaveMark.mark(this));
		energyOut = energy.extractOnly(() -> level == null || PowerGrid.givesHere(level, worldPosition));
	}

	/** Two seconds of its most output: Power Lines draw on it once a second. */
	static int storeSize() {
		return Math.max(100_000, (int) Math.ceil(outputFor(maxStress()) * 40));
	}

	/** FE a tick it makes from this much stress: what a motor draws for it, times the efficiency. */
	public static double outputFor(int stress) {
		return stress * CMConfig.SERVER.motorFePerStress() * CMConfig.SERVER.dynamoEfficiency();
	}

	/** The most stress it can be set to take: the motor's most. */
	public static int maxStress() {
		return ElectricMotorBlockEntity.maxStress();
	}

	/** A setting as it's kept: a whole number of steps, between one step and the most. */
	static int clampStress(int stress) {
		return Mth.clamp(stress / STRESS_STEP * STRESS_STEP, STRESS_STEP, Math.max(STRESS_STEP, maxStress()));
	}

	public IEnergyStorage getEnergyOut() {
		return energyOut;
	}

	public EnergyBuffer getEnergy() {
		return energy;
	}

	public int getStressSetting() {
		return stress;
	}

	public boolean isWorking() {
		return working;
	}

	/** Server: the share of its output its free sources give, as last worked out (the most that counts on paper). */
	public float getFreeShare() {
		double share = 0;
		for (double s : freeSources.values())
			share += s;
		return (float) Math.min(1, share);
	}

	/** FE a tick its Power Line network last said it makes (what its goggles show). */
	public int networkMakes() {
		return reading.makes();
	}

	/** FE a tick at its setting, whether or not it's turning now. */
	public double outputPerTick() {
		return outputFor(stress);
	}

	/** From the settings screen, checked on the server. */
	public void applySettings(int stress) {
		int set = clampStress(stress);
		if (set == this.stress)
			return;
		this.stress = set;
		setChanged();
		if (hasNetwork())
			getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
		sendData();
		if (level != null)
			level.playSound(null, worldPosition, ModSounds.dynamo("adjust"), SoundSource.BLOCKS, 0.6f, 1);
	}

	// ---------- stress: its setting at any speed ----------

	@Override
	public float calculateStressApplied() {
		float impact = FixedStress.impact(this, stress, false);
		lastStressApplied = impact;
		return impact;
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (level != null && !level.isClientSide)
			FixedStress.keep(this, FixedStress.impact(this, stress, false));
	}

	// ---------- running ----------

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			// the level's state, not this block entity's copy (Ponder changes the block without telling it), on the
			// animation clock (which runs in Ponder while the game is paused)
			BlockState state = level.getBlockState(worldPosition);
			boolean shown = state.hasProperty(ClockworkDynamoBlock.WORKING) ? state.getValue(ClockworkDynamoBlock.WORKING)
				: getBlockState().getValue(ClockworkDynamoBlock.WORKING);
			aerial.update(shown, stress, net.createmod.catnip.animation.AnimationTickHolder.getTicks(level));
			clientTicker.accept(this);
			return;
		}
		// (game tests: marked away, it stands still as if its chunk weren't running)
		if (PowerGrid.away(level, worldPosition))
			return;
		ServerLevel server = (ServerLevel) level;
		// running again after being on paper: it takes on what its record came to
		PowerGrid.of(server).back(this, server);
		FixedStress.keep(this, FixedStress.impact(this, stress, false));

		// Create gives an overstressed block a speed of 0, so this is also false then. With Create's stress switched off
		// nothing is ever overstressed, so its setting would be free of any supply: it makes nothing (D2: never a gain)
		boolean turning = getSpeed() != 0 && IRotate.StressImpact.isEnabled();
		if (turning) {
			carry += outputPerTick();
			int make = (int) carry;
			carry -= make;
			energy.generate(Math.min(make, energy.getMaxEnergyStored() - energy.getEnergyStored()));
		} else {
			carry = 0;
		}

		// Out of its base into whatever takes it there (looked up again if it's been turned in place)
		Direction side = getBlockState().getValue(ClockworkDynamoBlock.FACING).getOpposite();
		if (base == null || side != baseSide) {
			base = EnergyBuffer.cacheFor(server, worldPosition, side);
			baseSide = side;
		}
		int pushed = energy.pushTo(base, energy.getEnergyStored());
		if (pushed > 0 && server.getGameTime() % 20 == 0)
			awardIfRunsMotor(server, worldPosition.relative(side));

		boolean started = turning && !working;
		if (turning != working) {
			working = turning;
			BlockState state = getBlockState();
			if (state.getValue(ClockworkDynamoBlock.WORKING) != working)
				level.setBlock(worldPosition, state.setValue(ClockworkDynamoBlock.WORKING, working), 3);
			sendData();
		}
		// its free sources: at once when it starts (so its record is right from the start), then about once a second
		if (started || Math.floorMod(worldPosition.hashCode() + server.getGameTime(), 20) == 0) {
			freeSources = working ? freeSources() : Map.of();
			SaveMark.mark(this);
		}
		// Keep the goggles' charge reading fresh without flooding packets
		if (server.getGameTime() % 10 == 0 && Math.abs(energy.getEnergyStored() - syncedEnergy) > energy.getMaxEnergyStored() / 100) {
			syncedEnergy = energy.getEnergyStored();
			sendData();
		}
		PowerGrid.of(server).note(this, server);
	}

	/** A running Electric Motor against its base, powered by it: everyone nearby gets the credit. */
	private void awardIfRunsMotor(ServerLevel server, BlockPos next) {
		if (server.getBlockEntity(next) instanceof ElectricMotorBlockEntity motor && motor.isRunning()
			&& ElectricMotorBlock.isInput(motor.getBlockState(), getBlockState().getValue(ClockworkDynamoBlock.FACING)))
			ModAdvancements.awardNearby(server, next, ModAdvancements.DYNAMO_MOTOR, ItemStack.EMPTY, 0);
	}

	/** Power Lines (once a second): a working dynamo and a running motor on the same network earn the credit too. */
	public static void noteNetwork(net.minecraft.world.level.Level level, Iterable<BlockPos> machines) {
		BlockPos motor = null;
		boolean dynamo = false;
		for (BlockPos p : machines) {
			if (!level.isLoaded(p))
				continue;
			var be = level.getBlockEntity(p);
			if (be instanceof ClockworkDynamoBlockEntity d && d.isWorking())
				dynamo = true;
			else if (be instanceof ElectricMotorBlockEntity m && m.isRunning())
				motor = p;
		}
		if (dynamo && motor != null)
			ModAdvancements.awardNearby(level, motor, ModAdvancements.DYNAMO_MOTOR, ItemStack.EMPTY, 0);
	}

	/**
	 * Its shaft network's free sources and the share of its power each gives. Only loaded sources still in place can be
	 * free; anything else (its chunk unloaded, or gone) counts towards the whole but not as free. Never loads a chunk:
	 * the capacities are the network's own figures, and the whole is at least what Create last told this block.
	 */
	private Map<BlockPos, Double> freeSources() {
		if (!hasNetwork() || level == null)
			return Map.of();
		KineticNetwork network = getOrCreateNetwork();
		Map<BlockPos, Double> free = new LinkedHashMap<>();
		double total = 0;
		for (KineticBlockEntity source : network.sources.keySet()) {
			double cap = network.getActualCapacityOf(source);
			total += cap;
			BlockPos at = source.getBlockPos();
			if (!source.isRemoved() && level.isLoaded(at) && level.getBlockEntity(at) == source && source.getBlockState().is(FREE_SOURCES))
				free.merge(at.immutable(), cap, Double::sum);
		}
		return shares(free, Math.max(total, capacity));
	}

	/** Each free source's capacity as a share of the whole (none of nothing), as a map nothing else changes. */
	static Map<BlockPos, Double> shares(Map<BlockPos, Double> capacities, double total) {
		if (total <= 0)
			return Map.of();
		Map<BlockPos, Double> out = new LinkedHashMap<>();
		capacities.forEach((at, cap) -> {
			if (cap > 0)
				out.put(at, Math.min(1, cap / total));
		});
		return Map.copyOf(out);
	}

	// ---------- its record, for running on paper while its chunk isn't running ----------

	@Override
	public PowerGrid.Paper newPaper() {
		return new PowerGrid.Paper("clockwork_dynamo", 0);
	}

	/**
	 * Its full output while it runs (what its network shows it making), and its free sources for while it's away: each
	 * counts its share only while it isn't running itself ({@link PowerGrid.Paper#away}). Nothing unless it's working.
	 */
	@Override
	public void record(PowerGrid.Paper p, boolean refresh) {
		p.set(working ? (int) Math.floor(outputPerTick()) : 0, energy.getEnergyStored(), energy.getMaxEnergyStored());
		p.away(working ? freeSources : Map.of());
	}

	/** Back from paper: its store moves by as much as its record's did. */
	@Override
	public void resume(double storeChange, double[] fuelChange) {
		energy.setEnergy((int) Math.max(0, Math.min(energy.getMaxEnergyStored(), energy.getEnergyStored() + Math.round(storeChange))));
		SaveMark.mark(this);
	}

	@Override
	public void networkReading(int makes, int uses) {
		if (level != null && reading.set(makes, uses, level.getGameTime()))
			sendData();
	}

	// ---------- goggles and saving ----------

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.clockwork_dynamo").forGoggles(tooltip);
		lang().text(String.format(Locale.ROOT, "%,d / %,d FE", energy.getEnergyStored(), energy.getMaxEnergyStored()))
			.style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		if (working)
			lang().translate("gui.goggles.clockwork_dynamo.making", String.format(Locale.ROOT, "%,.0f", outputPerTick()))
				.style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
		else
			lang().translate(!IRotate.StressImpact.isEnabled() ? "gui.goggles.clockwork_dynamo.stress_off"
					: isOverStressed() ? "gui.goggles.clockwork_dynamo.overstressed" : "gui.goggles.clockwork_dynamo.not_turning")
				.style(ChatFormatting.RED).forGoggles(tooltip, 1);
		lang().translate("gui.goggles.clockwork_dynamo.taking", String.format(Locale.ROOT, "%,d", stress),
				Math.round(CMConfig.SERVER.dynamoEfficiency() * 100))
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
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
		tag.putInt("StressSetting", stress);
		tag.putBoolean("Working", working);
		if (!clientPacket)
			tag.putDouble("Carry", carry);
		if (!clientPacket && !freeSources.isEmpty()) {
			net.minecraft.nbt.ListTag sources = new net.minecraft.nbt.ListTag();
			freeSources.forEach((at, share) -> {
				CompoundTag s = new CompoundTag();
				s.putLong("Pos", at.asLong());
				s.putDouble("Share", share);
				sources.add(s);
			});
			tag.put("FreeSources", sources);
		}
		reading.write(tag);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		energy.setEnergy(tag.getInt("Energy"));
		// Clamped like the screen's settings: the most stress may have been lowered in the config since it was saved
		stress = tag.contains("StressSetting") ? clampStress(tag.getInt("StressSetting")) : STRESS_STEP;
		working = tag.getBoolean("Working");
		if (!clientPacket)
			carry = tag.getDouble("Carry");
		if (!clientPacket) {
			Map<BlockPos, Double> sources = new LinkedHashMap<>();
			for (net.minecraft.nbt.Tag t : tag.getList("FreeSources", net.minecraft.nbt.Tag.TAG_COMPOUND))
				sources.put(BlockPos.of(((CompoundTag) t).getLong("Pos")), ((CompoundTag) t).getDouble("Share"));
			freeSources = Map.copyOf(sources);
		}
		reading.read(tag);
	}
}
