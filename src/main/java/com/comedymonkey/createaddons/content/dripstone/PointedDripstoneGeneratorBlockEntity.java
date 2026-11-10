package com.comedymonkey.createaddons.content.dripstone;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.FillOnly;
import com.comedymonkey.createaddons.content.cobblegen.TopFrequencySlot;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Grows Pointed Dripstone (design/specs/pointed-dripstone-generator.md). Each cycle the four seed stalactites grow their
 * tips together (taking {@link #waterCost()} of water for each as they start), then the hammer goes round them: for each,
 * its spindle ticks a quarter turn to it ({@link #INDEX} ticks) and it knocks the tip off ({@link #KNOCK} ticks), the piece
 * going into the store on the hit. The round of knocks takes the same time at any speed; the growing is what slows. A
 * cycle of four is 20 s at 256 RPM (5 s a piece, config), in proportion to speed.
 * <p>
 * It stops, with no stress, when it's switched off by redstone, missing a seed, short of water for the next tip, full,
 * or not turning; a part-done cycle waits where it is. The client runs the same cycle from what the server sends, so the
 * window keeps step.
 */
public class PointedDripstoneGeneratorBlockEntity extends KineticBlockEntity implements Clearable, MenuProvider {
	/** Ticks of the spindle's quarter turn to the next stalactite, and of the knock (wind-up to settle), at any speed. */
	public static final int INDEX = 10, KNOCK = 30;
	/** The round of four knocks after the tips have grown. */
	public static final int ROUND = 4 * (INDEX + KNOCK);
	/** The tick of the knock the hammer meets the tip: the piece is made. */
	public static final int HIT = 15;
	/** Seed stalactites it needs, and Pointed Dripstone it holds before it stops. */
	public static final int SEEDS = 4, CAPACITY = 64;
	/** Full, it waits until this many have been taken, so an extractor taking one at a time doesn't flip its stress. */
	private static final int RESTART = 16;
	private static final int SYNC_INTERVAL = 10;

	public enum Phase { INDEX, GROW, KNOCK }

	private final ItemStackHandler seed = new ItemStackHandler(1) {
		@Override
		public int getSlotLimit(int slot) {
			return SEEDS;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.is(Items.POINTED_DRIPSTONE);
		}

		@Override
		protected void onContentsChanged(int slot) {
			changed(true);
		}
	};
	private final FluidTank water = new FluidTank(1000, stack -> stack.is(Fluids.WATER)) {
		@Override
		protected void onContentsChanged() {
			changed(false);
		}

		/** Never a negative fill: one over the capacity (a config lowered under what it holds) takes nothing. */
		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return getFluidAmount() >= getCapacity() ? 0 : super.fill(resource, action);
		}
	};
	/** Set while it empties itself for a block mover: nothing is sent or updated. */
	private boolean quiet;
	private final IFluidHandler waterIn = new FillOnly(water);
	private final IItemHandlerModifiable output = new OutputHandler();

	private int stored;
	private boolean filled;
	private Phase phase = Phase.GROW;
	/** Ticks into an index or knock. */
	private int phaseTicks;
	/** How far the four tips have grown, 0 to 1. */
	private float growth;
	/** The water for these tips has been taken. */
	private boolean paid;
	/**
	 * The stalactite the hammer is at or turning to, 0 to 3, in the round of knocks; while the tips grow it waits by the
	 * last one (3).
	 */
	private int turn = SEEDS - 1;

	private boolean powered, wiredSignal, checkPowerNextTick, reportedIdle, syncQueued;
	private int linkSignal;
	/** Redstone Link receiver, on the Wireless one. No initialiser: it is set from the super constructor. */
	private LinkBehaviour link;

	public PointedDripstoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.POINTED_DRIPSTONE_GENERATOR.get(), pos, state);
		syncTankCapacity();
	}

	/**
	 * The tank holds the config's amount (at least a cycle's water), from the moment it exists: a pipe can fill it
	 * before its first tick. Water over a lowered capacity is cut back to it.
	 */
	private void syncTankCapacity() {
		int capacity = Math.max(cycleWater(), CMConfig.SERVER.dripstoneGeneratorTank());
		if (water.getCapacity() != capacity)
			water.setCapacity(capacity);
		if (water.getFluidAmount() > capacity)
			water.getFluid().setAmount(capacity);
	}

	// ---------------------------------------------------------------- the cycle

	/**
	 * Ticks a whole cycle of four takes at the current speed (0 if stopped): 20 s at 256 RPM by default (5 s a piece), in
	 * proportion to speed, and never shorter than the round of knocks.
	 */
	public int ticksPerCycle() {
		float rpm = Math.abs(getSpeed());
		if (rpm == 0)
			return 0;
		return Math.max(ROUND + 1, Math.round((float) CMConfig.SERVER.dripstoneGeneratorSeconds() * SEEDS * 20 * 256 / rpm));
	}

	/** Ticks the tips take to grow at the current speed (for the renderer: the hammer moves on in the first few). */
	public int getGrowTicks() {
		return growTicks();
	}

	private int growTicks() {
		return Math.max(1, ticksPerCycle() - ROUND);
	}

	/** Water for one Pointed Dripstone. */
	private int waterCost() {
		return CMConfig.SERVER.dripstoneGeneratorWater();
	}

	/** Water the four tips take as they start growing. */
	private int cycleWater() {
		return waterCost() * SEEDS;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (checkPowerNextTick && !level.isClientSide) {
			checkPowerNextTick = false;
			setWiredSignal(level.hasNeighborSignal(worldPosition));
			syncPoweredState();
		}
		syncTankCapacity();
		if (!level.isClientSide) {
			if (stored >= CAPACITY)
				filled = true;
			else if (filled && stored <= CAPACITY - RESTART) {
				filled = false;
				syncQueued = true;
			}
		}
		if (isRunning()) {
			advance();
			if (level.isClientSide)
				drip();
		}
		if (level.isClientSide)
			return;
		if (isIdle() != reportedIdle)
			updateStress();
		if (syncQueued && level.getGameTime() % SYNC_INTERVAL == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** One tick of the cycle, on both sides (only the server takes the water and makes the piece). */
	private void advance() {
		// a Ponder scene has no server: its generator runs the whole cycle itself
		boolean server = !level.isClientSide || ponder;
		switch (phase) {
			case GROW -> {
				if (!paid) {
					// the server takes the water and says so at once; the client waits to hear it (rather than guess
					// from water it may have been sent late)
					if (!server)
						return;
					water.drain(cycleWater(), IFluidHandler.FluidAction.EXECUTE);
					paid = true;
					if (!level.isClientSide)
						sendData();
				}
				growth = Math.min(1, growth + 1f / growTicks());
				// grown: the hammer goes round, starting with the first
				if (growth >= 1) {
					turn = 0;
					enter(Phase.INDEX);
				}
			}
			case INDEX -> {
				if (++phaseTicks >= INDEX)
					enter(Phase.KNOCK);
			}
			case KNOCK -> {
				phaseTicks++;
				if (phaseTicks == HIT)
					hit();
				if (phaseTicks >= KNOCK) {
					if (turn < SEEDS - 1) {
						turn++;
						enter(Phase.INDEX);
					} else {
						enter(Phase.GROW);
					}
				}
			}
		}
	}

	private void enter(Phase next) {
		phase = next;
		phaseTicks = 0;
		if (next == Phase.GROW) {
			growth = 0;
			paid = false;
		}
		if (!level.isClientSide) {
			setChanged();
			sendData();
		}
	}

	/** The hammer meets the tip: the piece goes into the store, with vanilla's Pointed Dripstone break sound and crumbs. */
	private void hit() {
		if (level.isClientSide && !ponder)
			return;
		stored = Math.min(CAPACITY, stored + 1);
		if (ponder)
			return;
		level.playSound(null, worldPosition, SoundEvents.POINTED_DRIPSTONE_BREAK, SoundSource.BLOCKS, 0.5f, 1);
		com.comedymonkey.createaddons.advancement.ModAdvancements.awardNearby(level, worldPosition,
			com.comedymonkey.createaddons.advancement.ModAdvancements.DRIPSTONE_GROWN, ItemStack.EMPTY, 1);
		if (level instanceof ServerLevel server) {
			Vec3 tip = toWorld(tipPosition(turn));
			server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.POINTED_DRIPSTONE.defaultBlockState()),
				tip.x, tip.y, tip.z, 6, 0.04, 0.04, 0.04, 0.05);
		}
		changed(true);
	}

	/** Where each stalactite hangs, in model pixels with the window north (as the renderer and the model). */
	public static final float[][] STALACTITES = {{11, 4.5f}, {11, 10.5f}, {5, 10.5f}, {5, 4.5f}};
	/** The tip's top and full bottom, in pixels. */
	public static final float TIP_TOP = 7.3f, TIP_BOTTOM = 4.1f;

	private static Vec3 tipPosition(int turn) {
		float[] s = STALACTITES[turn];
		return new Vec3(s[0] / 16, (TIP_TOP + TIP_BOTTOM) / 32, s[1] / 16);
	}

	/** A point in the block's model frame (window north, 0-1) in the world, turned to the real front. */
	private Vec3 toWorld(Vec3 local) {
		Direction front = getBlockState().getValue(PointedDripstoneGeneratorBlock.FACING);
		Vec3 turned = VecHelper.rotateCentered(local, -(front.toYRot() + 180), Direction.Axis.Y);
		return Vec3.atLowerCornerOf(worldPosition).add(turned);
	}

	/** Client only: now and then a drop of water falls from one of the growing tips. */
	private void drip() {
		if (phase != Phase.GROW || growth < 0.2f || level.random.nextInt(20) != 0)
			return;
		float[] s = STALACTITES[level.random.nextInt(SEEDS)];
		Vec3 at = toWorld(new Vec3(s[0] / 16, (TIP_TOP - (TIP_TOP - TIP_BOTTOM) * growth) / 16, s[1] / 16));
		level.addParticle(ParticleTypes.DRIPPING_DRIPSTONE_WATER, at.x, at.y, at.z, 0, 0, 0);
	}

	// ---------------------------------------------------------------- state

	public boolean hasSeed() {
		return seed.getStackInSlot(0).getCount() >= SEEDS;
	}

	/** Short of water for the next four tips (tips already growing have had theirs). */
	public boolean needsWater() {
		return phase == Phase.GROW && !paid && water.getFluidAmount() < cycleWater();
	}

	public boolean isFull() {
		return filled || stored >= CAPACITY;
	}

	/** Full: it stops where it is (a knock already under way finishes), until room is made. */
	private boolean waitingForRoom() {
		return isFull() && phase != Phase.KNOCK;
	}

	/** Makes nothing and puts no load on the network. */
	public boolean isIdle() {
		return powered || !hasSeed() || needsWater() || waitingForRoom();
	}

	/** Turning, not overstressed, not idle: the cycle runs. */
	public boolean isRunning() {
		return getSpeed() != 0 && !isOverStressed() && !isIdle();
	}

	public boolean isPowered() {
		return powered;
	}

	public Phase getPhase() {
		return phase;
	}

	public int getTurn() {
		return turn;
	}

	/** Ticks into the index or knock, between ticks while it runs. */
	public float getPhaseTicks(float partialTicks) {
		return phaseTicks + (isRunning() && phase != Phase.GROW ? partialTicks : 0);
	}

	/** How far the tips have grown, 0 to 1 (1 through the round of knocks). */
	public float getGrowth(float partialTicks) {
		return phase == Phase.GROW ? Math.min(1, growth + (isRunning() ? partialTicks / growTicks() : 0)) : 1;
	}

	/** Seed stalactites in place, 0 to 4. */
	public int getSeedCount() {
		return seed.getStackInSlot(0).getCount();
	}

	public FluidTank getWater() {
		return water;
	}

	public int getStored() {
		return stored;
	}

	/** Set by a Ponder scene: no server there, so this generator runs the cycle itself (see {@link #advance()}). */
	private boolean ponder;

	/** Ponder only: the store, the seed and the water. */
	public void setPonderState(int stored, int seeds, int waterAmount) {
		// (game tests use this too, on a real server, where nothing changes)
		ponder = level != null && level.isClientSide;
		this.stored = stored;
		this.filled = stored >= CAPACITY;
		seed.setStackInSlot(0, seeds > 0 ? new ItemStack(Items.POINTED_DRIPSTONE, seeds) : ItemStack.EMPTY);
		water.setFluid(waterAmount > 0 ? new FluidStack(Fluids.WATER, waterAmount) : FluidStack.EMPTY);
	}

	// ---------------------------------------------------------------- the seed

	/** Whether this stack can go into the seed (Pointed Dripstone, while it has fewer than four). */
	boolean seeds(ItemStack stack) {
		if (!stack.is(Items.POINTED_DRIPSTONE) || getSeedCount() >= SEEDS)
			return false;
		// it has to actually go in (a renamed one doesn't stack with plain ones already there)
		return seed.insertItem(0, stack.copyWithCount(1), true).isEmpty();
	}

	/** Puts as many of the stack as the seed still needs into it, taking only what went in (a creative player keeps theirs). */
	void addSeed(Player player, ItemStack stack) {
		int add = Math.min(SEEDS - getSeedCount(), stack.getCount());
		if (add <= 0)
			return;
		int taken = add - seed.insertItem(0, stack.copyWithCount(add), false).getCount();
		if (taken <= 0)
			return;
		if (!player.getAbilities().instabuild)
			stack.shrink(taken);
		level.playSound(null, worldPosition, SoundEvents.POINTED_DRIPSTONE_PLACE, SoundSource.BLOCKS, 0.8f, 1);
	}

	public IItemHandlerModifiable getSeed() {
		return seed;
	}

	// ---------------------------------------------------------------- redstone

	/** From the block's neighbour updates (and once after loading): a wired redstone signal stops it. */
	void setWiredSignal(boolean signal) {
		wiredSignal = signal;
		updatePowered();
	}

	private void setLinkSignal(int strength) {
		linkSignal = strength;
		updatePowered();
	}

	private void updatePowered() {
		boolean now = wiredSignal || linkSignal > 0;
		if (powered == now)
			return;
		powered = now;
		setChanged();
		sendData();
		syncPoweredState();
	}

	/** The antenna lights up while it holds the generator off. */
	private void syncPoweredState() {
		if (level == null || level.isClientSide)
			return;
		BlockState state = getBlockState();
		if (state.hasProperty(PointedDripstoneGeneratorBlock.POWERED) && state.getValue(PointedDripstoneGeneratorBlock.POWERED) != powered)
			level.setBlock(worldPosition, state.setValue(PointedDripstoneGeneratorBlock.POWERED, powered), 3);
	}

	@Override
	public void initialize() {
		super.initialize();
		// Checked on the first tick rather than here, so the block state is not touched while the chunk loads
		checkPowerNextTick = true;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		if (getBlockState().getBlock() instanceof PointedDripstoneGeneratorBlock block && block.isWireless()) {
			link = LinkBehaviour.receiver(this, ValueBoxTransform.Dual.makeSlots(TopFrequencySlot::new), this::setLinkSignal);
			behaviours.add(link);
		}
	}

	public boolean hasAntenna() {
		return link != null;
	}

	// ---------------------------------------------------------------- stress

	/** No stress while idle; the normal impact per RPM otherwise. */
	@Override
	public float calculateStressApplied() {
		float impact = super.calculateStressApplied();
		if (isIdle()) {
			impact = 0;
			lastStressApplied = 0;
		}
		return impact;
	}

	private void updateStress() {
		reportedIdle = isIdle();
		if (hasNetwork())
			getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
		sendData();
	}

	// ---------------------------------------------------------------- the store

	public IItemHandlerModifiable getOutputHandler() {
		return output;
	}

	/** What pipes see of the tank: water in, nothing back out. */
	public IFluidHandler getWaterHandler() {
		return waterIn;
	}

	void dropContents() {
		if (level == null || level.isClientSide)
			return;
		double x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
		Containers.dropItemStack(level, x, y, z, seed.getStackInSlot(0).copy());
		seed.setStackInSlot(0, ItemStack.EMPTY);
		if (stored > 0)
			Containers.dropItemStack(level, x, y, z, new ItemStack(Items.POINTED_DRIPSTONE, stored));
		stored = 0;
	}

	int getComparatorOutput() {
		return stored <= 0 ? 0 : 1 + (int) (14f * stored / CAPACITY);
	}

	/** {@code now}: send it at once (the seed and the store change what the window shows); else with the next batch. */
	private void changed(boolean now) {
		if (quiet)
			return;
		setChanged(); // which also updates the comparators
		if (level == null || level.isClientSide)
			return;
		if (now)
			sendData();
		else
			syncQueued = true;
	}

	/** Called by block movers once they've copied this block entity elsewhere, so nothing is dropped twice. */
	@Override
	public void clearContent() {
		// quietly, as the Wellspring does: the block is mid-move, so no comparator updates and no packets
		quiet = true;
		try {
			stored = 0;
			seed.setStackInSlot(0, ItemStack.EMPTY);
			water.setFluid(FluidStack.EMPTY);
		} finally {
			quiet = false;
		}
	}

	// ---------------------------------------------------------------- goggles and screen

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.pointed_dripstone_generator").forGoggles(tooltip);
		lang().text(stored + " / " + CAPACITY + " ").style(ChatFormatting.GOLD)
			.add(lang().add(Items.POINTED_DRIPSTONE.getDescription().copy()).style(ChatFormatting.GRAY))
			.forGoggles(tooltip, 1);
		lang().text(String.format(Locale.ROOT, "%,d / %,d mB ", water.getFluidAmount(), water.getCapacity())).style(ChatFormatting.AQUA)
			.add(lang().add(Fluids.WATER.getFluidType().getDescription().copy()).style(ChatFormatting.GRAY))
			.forGoggles(tooltip, 1);
		Component status = status();
		lang().add(status.copy()).style(isRunning() ? ChatFormatting.AQUA : ChatFormatting.RED).forGoggles(tooltip, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	/** What it's doing, or why not: the goggles' and the screen's status line. */
	public Component status() {
		String k = ComedyMonkeysCreateAddons.MOD_ID + ".gui.pointed_dripstone_generator.";
		// in the spec's order (D29): switched off, full, needs seed, needs water, needs rotation
		if (powered)
			return Component.translatable(k + "off");
		if (isFull())
			return Component.translatable(k + "full");
		if (!hasSeed())
			return Component.translatable(k + "needs_seed", SEEDS - getSeedCount());
		if (needsWater())
			return Component.translatable(k + "needs_water");
		int ticks = ticksPerCycle();
		if (ticks == 0 || isOverStressed())
			return Component.translatable(k + "needs_rotation");
		return Component.translatable(k + "per_minute", String.format(Locale.ROOT, "%.1f", SEEDS * 1200f / ticks));
	}

	/** Seconds per Pointed Dripstone at the current speed (a quarter of a cycle of four), or 0 if it isn't turning. */
	public float secondsPerPiece() {
		return ticksPerCycle() / 20f / SEEDS;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	@Nullable
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new PointedDripstoneGeneratorMenu(containerId, inventory, this);
	}

	/** The menu finds its block entity from this, however the screen is opened. */
	@Override
	public void writeClientSideData(AbstractContainerMenu menu, net.minecraft.network.RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(worldPosition);
	}

	// ---------------------------------------------------------------- persistence

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("Stored", stored);
		tag.putBoolean("Filled", filled);
		tag.put("Seed", seed.serializeNBT(registries));
		tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
		tag.putString("Phase", phase.name());
		tag.putInt("PhaseTicks", phaseTicks);
		tag.putFloat("Growth", growth);
		tag.putBoolean("Paid", paid);
		tag.putInt("Turn", turn);
		tag.putBoolean("Powered", powered);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		stored = tag.getInt("Stored");
		filled = tag.getBoolean("Filled");
		seed.deserializeNBT(registries, tag.getCompound("Seed"));
		water.readFromNBT(registries, tag.getCompound("Water"));
		syncTankCapacity();
		Phase readPhase;
		try {
			readPhase = Phase.valueOf(tag.getString("Phase"));
		} catch (IllegalArgumentException e) {
			readPhase = Phase.GROW;
		}
		int readTicks = tag.getInt("PhaseTicks"), readTurn = Math.floorMod(tag.getInt("Turn"), SEEDS);
		float readGrowth = tag.getFloat("Growth");
		// The client runs the cycle itself; a packet that's only a tick or two off what it shows (latency jitter) doesn't
		// pull the hammer back, it keeps its own place. A real change of phase or turn always wins.
		boolean inStep = clientPacket && readPhase == phase && readTurn == turn
			&& Math.abs(readTicks - phaseTicks) <= 2 && Math.abs(readGrowth - growth) <= 2f / Math.max(1, growTicks());
		if (!inStep) {
			phase = readPhase;
			phaseTicks = readTicks;
			turn = readTurn;
			growth = readGrowth;
		}
		paid = tag.getBoolean("Paid");
		powered = tag.getBoolean("Powered");
	}

	/** The store out from any side, and into the screen's output slot; nothing goes in. */
	private class OutputHandler implements IItemHandlerModifiable {
		@Override
		public int getSlots() {
			return 1;
		}

		/**
		 * A fresh stack every time, never a kept one: the screen's slot hands what it shows straight to the player (a
		 * number-key swap puts this very object in their hotbar), so a cached stack would end up shared with their
		 * inventory, and duplicated or lost the next time it's shown.
		 */
		@Override
		public ItemStack getStackInSlot(int slot) {
			return stored <= 0 ? ItemStack.EMPTY : new ItemStack(Items.POINTED_DRIPSTONE, stored);
		}

		/** The screen's slot sets what's left after a player takes some. */
		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			stored = stack.is(Items.POINTED_DRIPSTONE) ? Math.min(CAPACITY, stack.getCount()) : 0;
			changed(true);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			int count = Math.min(Math.min(amount, 64), stored);
			if (count <= 0)
				return ItemStack.EMPTY;
			if (!simulate) {
				stored -= count;
				changed(true);
			}
			return new ItemStack(Items.POINTED_DRIPSTONE, count);
		}

		@Override
		public int getSlotLimit(int slot) {
			return CAPACITY;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return false;
		}
	}
}
