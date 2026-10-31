package com.comedymonkey.createaddons.content.wastes;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.SaveMark;
import com.comedymonkey.createaddons.content.electric.EnergyBuffer;
import com.comedymonkey.createaddons.content.line.PowerGrid;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A Star Reactor's burn (D23, D29, D33, P14, P44). While it holds a star and no redstone signal reaches it, it makes
 * its full output every tick into its store (what doesn't fit is lost) and pushes it out of its terminal into whatever
 * takes it; each tick of running burns a tick of the star (8 hours a star, 12 overhauled). Redstone pauses both. It
 * never explodes. Broken or picked up, it keeps the burn left and its upgrade ({@link ModWastes#STAR_BURN},
 * {@link ModWastes#REACTOR_OVERHAULED} on the item).
 * <p>
 * It's a {@link PowerGrid.Generator}: while its chunk isn't running, a Power Line on it runs it on paper from its record
 * (the burn left is its one fuel, burning a tick a tick while it makes power). It needs nothing special on an
 * Aeronautics ship: it ticks there like any block (as the Lava Generator does).
 */
public class StarReactorBlockEntity extends BlockEntity implements IHaveGoggleInformation, PowerGrid.Generator {
	/** Real-time ticks in an hour of running: one pip a pip. */
	public static final int TICKS_PER_HOUR = 72_000;
	private static final int BUFFER = 1_000_000;

	private final EnergyBuffer energy = new EnergyBuffer(BUFFER, 0, BUFFER, () -> SaveMark.mark(this));
	private final IEnergyStorage energyOut = energy.extractOnly(() -> PowerGrid.givesHere(level, worldPosition));
	/** Ticks of running the star in it has left (0: none in it). */
	private long burn;
	private boolean paused;
	private boolean pausedKnown;
	@Nullable
	private BlockCapabilityCache<IEnergyStorage, @Nullable Direction> terminal;
	/** As last synced: whole hours left (the pips), and whether it's burning (the star turns, the ports glow). */
	private int hoursShown = -1;
	private boolean runningShown;

	/** Funnels, arms, chutes and packages put a star in here; nothing comes out. */
	private final IItemHandler items = new IItemHandler() {
		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (stack.isEmpty() || !isStar(stack) || !canTakeStar())
				return stack;
			if (!simulate) {
				insertStar(null);
				if (level != null)
					ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.WASTES_STAR_POWER, ItemStack.EMPTY, 0);
			}
			return stack.copyWithCount(stack.getCount() - 1);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return isStar(stack);
		}
	};

	public StarReactorBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.STAR_REACTOR_BE.get(), pos, state);
	}

	public static boolean isStar(ItemStack stack) {
		return stack.is(Items.NETHER_STAR);
	}

	public IItemHandler items() {
		return items;
	}

	public IEnergyStorage energyOut() {
		return energyOut;
	}

	public EnergyBuffer energy() {
		return energy;
	}

	public boolean isUpgraded() {
		return getBlockState().getValue(StarReactorBlock.UPGRADED);
	}

	public long burn() {
		return burn;
	}

	public boolean isPaused() {
		return paused;
	}

	/** Making power now (on the client: as last synced). */
	public boolean isRunning() {
		return level != null && level.isClientSide ? runningShown : burn > 0 && !paused;
	}

	/** Whole hours of star left, rounded up (the lit pips; on the client as last synced). */
	public int hoursLeft() {
		return level != null && level.isClientSide ? Math.max(0, hoursShown) : (int) ((burn + TICKS_PER_HOUR - 1) / TICKS_PER_HOUR);
	}

	/** Ticks a fresh star burns in it. */
	public long starTicks() {
		long plain = CMConfig.SERVER.starBurnTicks();
		return isUpgraded() ? Math.round(plain * CMConfig.SERVER.starUpgradeMultiplier()) : plain;
	}

	/** One star at a time (P14). */
	public boolean canTakeStar() {
		return burn <= 0;
	}

	/** A star goes in: a full star's burn. {@code by}: who put it in by hand (for the advancement). */
	public boolean insertStar(@Nullable Player by) {
		if (!canTakeStar() || level == null || level.isClientSide)
			return false;
		burn = starTicks();
		if (by != null)
			ModAdvancements.award(by, ModAdvancements.WASTES_STAR_POWER);
		show();
		SaveMark.mark(this);
		return true;
	}

	/** An Unknown Device fitted, once (D33, P44): each star burns half as long again, the one in it too. */
	public boolean fitUpgrade() {
		if (level == null || level.isClientSide || isUpgraded())
			return false;
		burn = Math.round(burn * CMConfig.SERVER.starUpgradeMultiplier());
		level.setBlock(worldPosition, getBlockState().setValue(StarReactorBlock.UPGRADED, true), Block.UPDATE_ALL);
		show();
		SaveMark.mark(this);
		return true;
	}

	void setPaused(boolean paused) {
		pausedKnown = true;
		if (this.paused != paused) {
			this.paused = paused;
			show();
		}
	}

	/** The comparator reads the star left (15 at a fresh star). */
	public int comparator() {
		if (burn <= 0)
			return 0;
		return (int) Math.max(1, Math.min(15, Math.ceil(burn * 15.0 / Math.max(1, starTicks()))));
	}

	/** Like vanilla's chest: turned by a wrench, its terminal looks again. */
	@Override
	public void setBlockState(BlockState state) {
		BlockState old = getBlockState();
		super.setBlockState(state);
		if (old.getValue(StarReactorBlock.FACING) != state.getValue(StarReactorBlock.FACING)) {
			invalidateCapabilities();
			terminal = null;
		}
	}

	void tick() {
		if (!(level instanceof ServerLevel server))
			return;
		// (game tests: marked away, it stands still as if its chunk weren't running)
		if (PowerGrid.away(level, worldPosition))
			return;
		PowerGrid.of(server).back(this, server);
		if (!pausedKnown)
			setPaused(level.hasNeighborSignal(worldPosition));
		if (burn > 0 && !paused) {
			burn--;
			energy.generate(CMConfig.SERVER.starReactorOutput());
			if (burn == 0)
				level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
		if (terminal == null)
			terminal = EnergyBuffer.cacheFor(server, worldPosition, getBlockState().getValue(StarReactorBlock.FACING));
		energy.pushTo(terminal, energy.getEnergyStored());
		show();
		if (burn % 1200 == 0)
			SaveMark.mark(this);
		PowerGrid.of(server).note(this, server);
	}

	/** Tells the client when its pips or its running change, and comparators when the star left steps. */
	private void show() {
		if (level == null || level.isClientSide)
			return;
		int hours = hoursLeft();
		boolean running = isRunning();
		if (hours != hoursShown || running != runningShown) {
			hoursShown = hours;
			runningShown = running;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
			setChanged();
		}
	}

	// ---------- its record, for running on paper ----------

	@Override
	public PowerGrid.Paper newPaper() {
		return new PowerGrid.Paper("star_reactor", 1);
	}

	@Override
	public void record(PowerGrid.Paper p, boolean refresh) {
		// paused, it makes nothing and burns nothing (a rate of 0 burns no fuel on paper)
		p.set(paused ? 0 : CMConfig.SERVER.starReactorOutput(), energy.getEnergyStored(), BUFFER);
		p.fuel(0).set(burn, (int) Math.min(Integer.MAX_VALUE, Math.max(burn, starTicks())), 1);
	}

	@Override
	public void resume(double storeChange, double[] fuelChange) {
		energy.setEnergy((int) Math.max(0, Math.min(BUFFER, energy.getEnergyStored() + Math.round(storeChange))));
		burn = Math.max(0, burn + Math.round(fuelChange[0]));
		show();
		SaveMark.mark(this);
	}

	// ---------- the item keeps its burn and upgrade ----------

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (burn > 0)
			components.set(ModWastes.STAR_BURN.get(), burn);
		if (isUpgraded())
			components.set(ModWastes.REACTOR_OVERHAULED.get(), true);
	}

	@Override
	protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
		super.applyImplicitComponents(input);
		burn = Math.max(0, input.getOrDefault(ModWastes.STAR_BURN.get(), 0L));
	}

	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		tag.remove("Burn");
	}

	// ---------- saving and syncing ----------

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putLong("Burn", burn);
		tag.putInt("Energy", energy.getEnergyStored());
		tag.putBoolean("Paused", paused);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		burn = tag.getLong("Burn");
		energy.setEnergy(tag.getInt("Energy"));
		paused = tag.getBoolean("Paused");
		if (tag.contains("Hours")) {
			hoursShown = tag.getInt("Hours");
			runningShown = tag.getBoolean("Running");
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Hours", hoursLeft());
		tag.putBoolean("Running", isRunning());
		tag.putLong("Burn", burn);
		tag.putBoolean("Paused", paused);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		hoursShown = tag.getInt("Hours");
		runningShown = tag.getBoolean("Running");
		burn = tag.getLong("Burn");
		paused = tag.getBoolean("Paused");
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
		handleUpdateTag(pkt.getTag(), registries);
	}

	// ---------- goggles ----------

	/** "7 h 42 min". */
	public static String duration(long ticks) {
		long minutes = Math.max(0, ticks) / 1200;
		return minutes >= 60 ? String.format(Locale.ROOT, "%d h %02d min", minutes / 60, minutes % 60) : String.format(Locale.ROOT, "%d min", minutes);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.star_reactor").forGoggles(tooltip);
		String state = burn <= 0 ? "no_star" : paused ? "paused" : "running";
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.star_reactor." + state,
				String.format(Locale.ROOT, "%,d", CMConfig.SERVER.starReactorOutput()))
			.style(state.equals("running") ? ChatFormatting.GREEN : ChatFormatting.RED).forGoggles(tooltip, 1);
		if (burn > 0)
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.star_reactor.left", duration(burn))
				.style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		if (isUpgraded())
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.star_reactor.overhauled",
					Math.round(CMConfig.SERVER.starBurnTicks() * CMConfig.SERVER.starUpgradeMultiplier() / TICKS_PER_HOUR))
				.style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.text(String.format(Locale.ROOT, "%,d / %,d FE", energy.getEnergyStored(), BUFFER)).style(ChatFormatting.AQUA)
			.forGoggles(tooltip, 1);
		return true;
	}
}
