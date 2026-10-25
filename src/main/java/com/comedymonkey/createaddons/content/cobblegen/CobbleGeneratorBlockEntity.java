package com.comedymonkey.createaddons.content.cobblegen;

import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Makes cobblestone (or basalt, or blackstone) at exactly the pace a Mechanical Drill at the same speed would mine it: every "hit" adds
 * max(1, breakSpeed / hardness) of 10 damage stages, then waits hardness / breakSpeed ticks, with breakSpeed being
 * RPM / 100 (Create's BlockBreakingKineticBlockEntity). For cobblestone that is 10 ticks per block at 256 RPM.
 * The hardness is that of the block being made.
 */
public class CobbleGeneratorBlockEntity extends KineticBlockEntity implements Clearable {
	private static final int SYNC_INTERVAL = 10;

	private final IItemHandler output = new OutputHandler();
	private int stored;
	/**
	 * Filled up, and waiting for a quarter of its store to be taken before it starts again: an extractor taking a little
	 * at a time would otherwise flip it between idle and working every cycle, putting its load on the network and
	 * taking it off again.
	 */
	private boolean filled;
	private int destroyProgress;
	private int ticksUntilNextProgress;
	private boolean syncQueued;
	/** Stopped by a redstone signal: wired, or wireless through the antenna. */
	private boolean powered;
	private boolean wiredSignal;
	private int linkSignal;
	/** Redstone Link receiver, on wireless generators. No initialiser: it is set from the super constructor. */
	private LinkBehaviour link;
	private boolean checkPowerNextTick;
	/** Whether the stress the network last heard about was the idle (zero) one. */
	private boolean reportedIdle;
	/** Client only: the same drill countdown, run locally so the window shows the block forming in step. */
	private int clientProgress;
	private int clientTicksUntilNextProgress;
	/**
	 * Client only: the drill's tip height (pixels) this tick and last, eased toward riding the forming block while it
	 * runs and back up to {@link #DRILL_RAISED} when it stops; and the game time of its last strike, for the jab.
	 */
	private float drillTip = DRILL_RAISED, prevDrillTip = DRILL_RAISED;
	private long lastStrike = Long.MIN_VALUE;

	/** Where the drill's tip waits when it isn't drilling, in pixels from the floor of the block. */
	public static final float DRILL_RAISED = 8.75f;
	/** The forming block's full height: one 3 px block, or the brass batch's 2.25 px blocks. */
	public static final float BLOCK_HEIGHT = 3, BATCH_HEIGHT = 2.25f;

	public CobbleGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.COBBLE_GENERATOR.get(), pos, state);
	}

	public GeneratorProduct product() {
		return getBlockState().getBlock() instanceof CobbleGeneratorBlock b ? b.product() : GeneratorProduct.COBBLESTONE;
	}

	private float hardness() {
		return product().hardness();
	}

	private ItemStack productStack(int count) {
		return new ItemStack(product().item(), count);
	}

	private int capacity() {
		return getBlockState().getBlock() instanceof CobbleGeneratorBlock b ? b.capacity() : 64;
	}

	private int perCycle() {
		return getBlockState().getBlock() instanceof CobbleGeneratorBlock b ? b.perCycle() : 1;
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
		if (level.isClientSide) {
			if (isRunning()) {
				clientWork();
				steam();
			}
			animateDrill();
			return;
		}

		if (stored >= capacity())
			filled = true;
		else if (filled && stored <= capacity() - Math.max(perCycle(), capacity() / 4)) {
			filled = false;
			syncQueued = true;
		}
		if (getSpeed() != 0 && !isOverStressed() && !isIdle())
			work();
		// Full or switched off by redstone: stop loading the network, and pick the load back up when that ends
		if (isIdle() != reportedIdle)
			updateStress();

		if (syncQueued && level.getGameTime() % SYNC_INTERVAL == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** One tick of Create's drill logic, against an imaginary block of the product. */
	private void work() {
		if (ticksUntilNextProgress-- > 0)
			return;
		float hardness = hardness();
		float breakSpeed = Math.abs(getSpeed() / 100f);
		destroyProgress += Mth.clamp((int) (breakSpeed / hardness), 1, 10 - destroyProgress);
		if (destroyProgress >= 10) {
			destroyProgress = 0;
			ticksUntilNextProgress = 0;
			stored = Math.min(capacity(), stored + perCycle());
			level.playSound(null, worldPosition, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.15f, 0.9f);
			onContentsChanged();
			return;
		}
		ticksUntilNextProgress = (int) (hardness / breakSpeed);
	}

	private void clientWork() {
		if (clientTicksUntilNextProgress-- > 0)
			return;
		float hardness = hardness();
		float breakSpeed = Math.abs(getSpeed() / 100f);
		clientProgress += Mth.clamp((int) (breakSpeed / hardness), 1, 10 - clientProgress);
		// every hit is a strike of the drill
		lastStrike = level.getGameTime();
		if (clientProgress >= 10) {
			clientProgress = 0;
			clientTicksUntilNextProgress = 0;
			return;
		}
		clientTicksUntilNextProgress = (int) (hardness / breakSpeed);
	}

	/** Client only: the drill rides the top of the forming block while it runs, and waits raised otherwise. */
	private void animateDrill() {
		prevDrillTip = drillTip;
		float target = isRunning() ? 2 + blockHeight() * getFormingFraction() + 0.25f : DRILL_RAISED;
		// it follows the block down quickly, and lifts away more slowly when it stops or a block is done
		drillTip += (target - drillTip) * (target < drillTip ? 0.6f : 0.25f);
	}

	/** Brass makes a batch of 16, drawn as four smaller blocks. */
	public boolean makesBatch() {
		return perCycle() > 1;
	}

	public float blockHeight() {
		return makesBatch() ? BATCH_HEIGHT : BLOCK_HEIGHT;
	}

	/** The drill's tip height in pixels, between ticks. */
	public float getDrillTip(float partialTicks) {
		return Mth.lerp(partialTicks, prevDrillTip, drillTip);
	}

	/** How far down the drill's jab is (pixels): a quick 0.75 px stab and back over 3 ticks after each strike. */
	public float getDrillJab(float partialTicks) {
		if (level == null || !isRunning())
			return 0;
		float since = level.getGameTime() - lastStrike + partialTicks;
		return since < 0 || since > 3 ? 0 : 0.75f * Mth.sin(Mth.PI * since / 3);
	}

	/** Turning, not overstressed, not switched off and with room for more: the pours run. */
	public boolean isRunning() {
		return getSpeed() != 0 && !isOverStressed() && !isIdle();
	}

	/** Full or switched off: makes nothing and puts no load on the network. */
	public boolean isIdle() {
		return powered || filled || stored >= capacity();
	}

	public boolean isPowered() {
		return powered;
	}

	/** From the block's neighbour updates (and once after loading): a wired redstone signal stops the generator. */
	void setWiredSignal(boolean signal) {
		wiredSignal = signal;
		updatePowered();
	}

	/** From the antenna's Redstone Link: any signal on its frequency stops the generator. */
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
		if (state.hasProperty(CobbleGeneratorBlock.POWERED) && state.getValue(CobbleGeneratorBlock.POWERED) != powered)
			level.setBlock(worldPosition, state.setValue(CobbleGeneratorBlock.POWERED, powered), 3);
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
		if (getBlockState().getBlock() instanceof CobbleGeneratorBlock block && block.isWireless()) {
			link = createLink();
			behaviours.add(link);
		}
	}

	private LinkBehaviour createLink() {
		return LinkBehaviour.receiver(this, ValueBoxTransform.Dual.makeSlots(TopFrequencySlot::new), this::setLinkSignal);
	}

	public boolean hasAntenna() {
		return link != null;
	}



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

	public boolean isFull() {
		return filled || stored >= capacity();
	}

	/** Ponder only: how much it holds and whether redstone has switched it off. */
	public void setPonderState(int stored, boolean powered) {
		this.stored = stored;
		this.filled = stored >= capacity();
		this.powered = powered;
	}

	/** How far the cobblestone block forming in the chamber has grown, 0 to 1 (1 while full). */
	public float getFormingFraction() {
		if (isFull())
			return 1;
		int progress = level != null && level.isClientSide ? clientProgress : destroyProgress;
		return progress / 10f;
	}

	/** Now and then a puff of steam where the water meets the lava (client only). */
	private void steam() {
		if (level.random.nextInt(8) != 0)
			return;
		Vec3 local = new Vec3(0.5 + (level.random.nextDouble() - 0.5) * 0.2, 0.32, 0.4);
		Direction front = getBlockState().getValue(CobbleGeneratorBlock.FACING);
		// chamber coordinates are modelled with the front facing north; turn them to the actual front
		Vec3 turned = VecHelper.rotateCentered(local, -(front.toYRot() + 180), Direction.Axis.Y);
		level.addParticle(ParticleTypes.WHITE_SMOKE, worldPosition.getX() + turned.x, worldPosition.getY() + turned.y,
			worldPosition.getZ() + turned.z, 0, 0.012, 0);
	}

	/** Ticks one cycle takes at the current speed (0 if stopped), for the goggles. */
	private int ticksPerCycle() {
		if (getSpeed() == 0)
			return 0;
		float hardness = hardness();
		float breakSpeed = Math.abs(getSpeed() / 100f);
		int perHit = Math.max(1, (int) (breakSpeed / hardness));
		int hits = (10 + perHit - 1) / perHit;
		// A tick per hit, with the wait between hits but none after the last (work() starts the next block at once)
		return (hits - 1) * ((int) (hardness / breakSpeed) + 1) + 1;
	}

	// ---------------------------------------------------------------- output

	public IItemHandler getOutputHandler() {
		return output;
	}

	void dropContents() {
		if (level == null || level.isClientSide)
			return;
		while (stored > 0) {
			int count = Math.min(stored, 64);
			stored -= count;
			Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
				productStack(count));
		}
	}

	int getComparatorOutput() {
		return stored <= 0 ? 0 : 1 + (int) (14f * stored / capacity());
	}

	private void onContentsChanged() {
		setChanged();
		syncQueued = true;
		if (level != null)
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/** Called by block movers once they've copied this block entity elsewhere, so nothing is dropped twice. */
	@Override
	public void clearContent() {
		stored = 0;
		destroyProgress = 0;
	}

	// ---------------------------------------------------------------- goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles." + product().langKey()).forGoggles(tooltip);
		lang().text(stored + " / " + capacity() + " ").style(ChatFormatting.GOLD)
			.add(lang().add(product().item().getDescription().copy()).style(ChatFormatting.GRAY))
			.forGoggles(tooltip, 1);
		int ticks = ticksPerCycle();
		if (powered)
			lang().translate("gui.goggles.cobble_generator.off").style(ChatFormatting.RED).forGoggles(tooltip, 1);
		else if (isFull())
			lang().translate("gui.goggles.cobble_generator.full").style(ChatFormatting.RED).forGoggles(tooltip, 1);
		else if (ticks == 0 || isOverStressed())
			lang().translate("gui.goggles.cobble_generator.idle").style(ChatFormatting.RED).forGoggles(tooltip, 1);
		else
			lang().text(String.format(Locale.ROOT, "%.1f", perCycle() * 1200f / ticks) + " ").style(ChatFormatting.AQUA)
				.add(lang().translate("gui.goggles.cobble_generator.per_minute").style(ChatFormatting.GRAY))
				.forGoggles(tooltip, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	// ---------------------------------------------------------------- persistence

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("Stored", stored);
		tag.putBoolean("Filled", filled);
		tag.putInt("Progress", destroyProgress);
		tag.putBoolean("Powered", powered);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		stored = tag.getInt("Stored");
		filled = tag.getBoolean("Filled");
		destroyProgress = tag.getInt("Progress");
		powered = tag.getBoolean("Powered");
	}

	/** The product out from any side; nothing goes in. */
	private class OutputHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return stored <= 0 ? ItemStack.EMPTY : productStack(Math.min(stored, 64));
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
				onContentsChanged();
			}
			return productStack(count);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return false;
		}
	}
}
