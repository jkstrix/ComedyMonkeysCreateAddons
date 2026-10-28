package com.comedymonkey.createaddons.content.juggernaut;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Both ends of a Spring Engine (D35-D38). The output end holds the wind, in SU-ticks (one SU for one tick), for both:
 * the winding end adds the stress it's set to take for every tick it turns, lossless, until it's full; the output end
 * turns its shaft at its set speed while there's wind and no redstone signal, offering up to 16,384 SU, and loses only
 * what its network actually draws from it.
 */
public class SpringEngineBlockEntity extends GeneratingKineticBlockEntity {
	/** The winding end's setting is in steps of this many SU (1,024 SU by default, up to 262,144: under 4 minutes to full). */
	static final int STRESS_STEP = 1024;
	public static final int DEFAULT_SPEED = 32;

	/** The output end: the wind held (SU-ticks), and whether a signal has it stopped. */
	private long stored;
	private boolean powered;
	/** Client: how wound, 0..1, as last sent (both ends: the renderer shows it at either porthole). */
	private float shownFill;
	private float lastSentFill = -1;
	private long lastSentStored = -1;
	private int lastComparator = -1;
	/** The winding end: whether it was taking stress last time it told its network. */
	private boolean wasWinding;

	ScrollValueBehaviour speedSetting, stressSetting;

	public SpringEngineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SPRING_ENGINE.get(), pos, state);
	}

	private boolean isOutput() {
		return SpringEngineBlock.isOutput(getBlockState());
	}

	public static long capacity() {
		return CMConfig.Server.get(CMConfig.SERVER.springEngineCapacity) * 20;
	}

	private static int maxStress() {
		return CMConfig.Server.get(CMConfig.SERVER.springEngineMaxStress);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		// the settings sit on the drum's side away from the wheels
		CenteredSideValueBoxTransform side = new CenteredSideValueBoxTransform((state, dir) ->
			dir == state.getValue(SpringEngineBlock.HORIZONTAL_FACING).getCounterClockWise());
		if (SpringEngineBlock.isOutput(getBlockState())) {
			speedSetting = new KineticScrollValueBehaviour(CreateLang.translateDirect("kinetics.creative_motor.rotation_speed"), this, side);
			speedSetting.between(-256, 256);
			speedSetting.value = DEFAULT_SPEED;
			speedSetting.withCallback(i -> updateGeneratedRotation());
			behaviours.add(speedSetting);
		} else {
			stressSetting = new ScrollValueBehaviour(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.spring_engine.wind_stress"),
				this, side);
			stressSetting.between(1, Math.max(1, CMConfig.Server.get(CMConfig.SERVER.springEngineMaxWindStress) / STRESS_STEP));
			stressSetting.value = 1024 / STRESS_STEP;
			stressSetting.withFormatter(v -> String.format(Locale.ROOT, "%,d", v * STRESS_STEP));
			stressSetting.withCallback(i -> reportStress());
			behaviours.add(stressSetting);
		}
	}

	// ---- the store (on the output end)

	@Nullable
	private SpringEngineBlockEntity output() {
		if (isOutput())
			return this;
		return level != null && level.getBlockEntity(SpringEngineBlock.partner(worldPosition, getBlockState())) instanceof SpringEngineBlockEntity be
			&& be.isOutput() ? be : null;
	}

	public long stored() {
		return stored;
	}

	/** Game tests and Ponder: set the wind held (output end). */
	public void setStored(long stored) {
		this.stored = Math.max(0, Math.min(capacity(), stored));
		shownFill = fill();
	}

	/** Ponder: show this end wound to {@code fill} (0..1); on the output end it holds that much too. */
	public void showFill(float fill) {
		if (isOutput())
			setStored((long) (fill * capacity()));
		shownFill = fill;
	}

	public float fill() {
		return isOutput() ? (float) ((double) stored / capacity()) : shownFill;
	}

	/** How wound it looks (either end), for the renderer. */
	public float shownFill() {
		return shownFill;
	}

	public int comparatorLevel() {
		return stored <= 0 ? 0 : 1 + (int) Math.floor(fill() * 14.0);
	}

	private boolean running() {
		return isOutput() && stored > 0 && !powered;
	}

	void checkPowered() {
		boolean now = level != null && level.hasNeighborSignal(worldPosition);
		if (now != powered) {
			powered = now;
			updateGeneratedRotation();
			setChanged();
		}
	}

	// ---- kinetics

	@Override
	public float getGeneratedSpeed() {
		if (!running() || speedSetting == null)
			return 0;
		return convertToDirection(speedSetting.getValue(), getBlockState().getValue(SpringEngineBlock.HORIZONTAL_FACING));
	}

	/** The output end offers the engine's most (16,384 SU) at whatever speed it's set to. */
	@Override
	public float calculateAddedStressCapacity() {
		float rpm = speedSetting == null ? 0 : Math.abs(speedSetting.getValue());
		float capacity = isOutput() && rpm > 0 ? maxStress() / rpm : 0;
		lastCapacityProvided = capacity;
		return capacity;
	}

	/** The winding end takes exactly the stress it's set to, whatever its speed, until the spring is full. */
	@Override
	public float calculateStressApplied() {
		float rpm = Math.abs(getTheoreticalSpeed());
		float impact = !isOutput() && winding() && rpm > 0 ? windStress() / rpm : 0;
		lastStressApplied = impact;
		return impact;
	}

	private int windStress() {
		return stressSetting == null ? 0 : stressSetting.getValue() * STRESS_STEP;
	}

	/** The winding end: turning into an engine that has room left. */
	private boolean winding() {
		SpringEngineBlockEntity out = output();
		return out != null && out.stored < capacity();
	}

	private void reportStress() {
		wasWinding = winding();
		if (hasNetwork())
			getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (!isOutput())
			reportStress(); // its stress per RPM depends on its speed
	}

	@Override
	public void initialize() {
		super.initialize();
		if (isOutput()) {
			checkPowered();
			// back after a move or a reload: turning again if it should be
			if (getGeneratedSpeed() != getTheoreticalSpeed() && !hasSource())
				updateGeneratedRotation();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		if (isOutput())
			tickOutput();
		else
			tickWinding();
	}

	private void tickWinding() {
		SpringEngineBlockEntity out = output();
		if (out == null)
			return;
		if (winding() != wasWinding)
			reportStress(); // full, it stops loading its network; with room again, it starts
		if (getSpeed() != 0 && !isOverStressed() && winding()) {
			out.add(windStress());
			if (level.getGameTime() % 10 == 0)
				sound("engine_ratchet", 0.9f + 0.2f * level.random.nextFloat());
		}
		float fill = out.fill();
		if (fill != lastSentFill && (level.getGameTime() % 20 == 0 || Math.abs(fill - lastSentFill) > 0.01f)) {
			lastSentFill = shownFill = fill;
			sendData();
		}
	}

	/** Wind in from the winding end (SU-ticks), lossless, up to full. */
	void add(long amount) {
		boolean wasEmpty = stored <= 0, wasFull = stored >= capacity();
		stored = Math.min(capacity(), stored + amount);
		if (wasEmpty && stored > 0)
			updateGeneratedRotation(); // it can give again
		if (!wasFull && stored >= capacity())
			sound("engine_full", 1);
		setChanged();
	}

	private void tickOutput() {
		if (running() && capacity > 0) {
			// what its network actually draws, its share of it where other engines and motors help turn the same shafts
			float mine = calculateAddedStressCapacity() * Math.abs(getGeneratedSpeed());
			double used = Math.min(stress, capacity) * (mine / capacity);
			stored = Math.max(0, stored - (long) Math.ceil(used));
			if (stored == 0) {
				updateGeneratedRotation(); // run down
				sound("engine_empty", 1);
			}
			if (used > 0 && level.getGameTime() % 20 == 0)
				sound("engine_tick", 1);
			setChanged();
		}
		shownFill = fill();
		// once a second while it changes (goggles read it; a percent of an hour is half a minute of winding), at once on
		// a big step
		if (stored != lastSentStored && (level.getGameTime() % 20 == 0 || Math.abs(shownFill - lastSentFill) > 0.01f || stored == 0)) {
			lastSentFill = shownFill;
			lastSentStored = stored;
			sendData();
		}
		int comparator = comparatorLevel();
		if (comparator != lastComparator) {
			lastComparator = comparator;
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
			BlockPos winding = SpringEngineBlock.partner(worldPosition, getBlockState());
			level.updateNeighbourForOutputSignal(winding, getBlockState().getBlock());
		}
	}

	private void sound(String id, float pitch) {
		SoundEvent sound = ModSounds.juggernaut(id);
		if (sound != null)
			level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.6f, pitch);
	}

	// ---- goggles, saving, the item

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).add(getBlockState().getBlock().getName().copy().append(":")).forGoggles(tooltip);
		SpringEngineBlockEntity out = output();
		long held = out == null ? 0 : out.stored;
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate("gui.goggles.spring_engine.wound", String.format(Locale.ROOT, "%.1f", 100.0 * held / capacity()),
				String.format(Locale.ROOT, "%,d", held / 20 / 60), String.format(Locale.ROOT, "%,d", capacity() / 20 / 60))
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		// at full stress, how long what's in it lasts
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate("gui.goggles.spring_engine.lasts", String.format(Locale.ROOT, "%.1f", held / 20.0 / maxStress() / 60.0),
				String.format(Locale.ROOT, "%,d", maxStress()))
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (isOutput()) {
			tag.putLong("Stored", stored);
			tag.putBoolean("Powered", powered);
		}
		tag.putFloat("Fill", shownFill);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		stored = tag.getLong("Stored");
		powered = tag.getBoolean("Powered");
		shownFill = tag.getFloat("Fill");
	}

	/** D38: picked up or broken, it keeps its wind. */
	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (isOutput() && stored > 0)
			components.set(ModDataComponents.SPRING_WIND.get(), stored);
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		stored = Math.max(0, Math.min(capacity(), input.getOrDefault(ModDataComponents.SPRING_WIND.get(), 0L)));
		shownFill = fill();
	}

	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		super.removeComponentsFromTag(tag);
		tag.remove("Stored");
	}

	public Direction facing() {
		return getBlockState().getValue(SpringEngineBlock.HORIZONTAL_FACING);
	}
}
