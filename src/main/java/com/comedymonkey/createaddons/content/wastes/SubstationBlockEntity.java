package com.comedymonkey.createaddons.content.wastes;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.line.PowerGrid;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * A Substation's charge and upkeep (D24, P7, P8, P52). Dormant, everything it takes counts toward its wake charge
 * (1,000,000 FE by default); the moment the total reaches it, it wakes its district for good in the {@link CityRecord}
 * (the lever slams, the action bar tells players near). Woken, it keeps a store of two seconds' upkeep (500 FE a tick by
 * default) and, once a second, spends a second's worth: while it can, its district is fed and the lamps stay lit; short,
 * they flicker and go dark (the record tells them), until it's fed again. Its logic runs once a second (P52), on the
 * lower half; the upper half only shows the goggles.
 * <p>
 * Away (its chunk unloaded, P24, D48), it draws its upkeep through the Power Lines on paper: while it runs it keeps a
 * consumer record in the {@link PowerGrid} ({@link PowerGrid#noteConsumer}) and tells the {@link CityRecord} where it
 * stands, and the record pays its upkeep once a second from the lines ({@code PowerLines.drawAway}). Back, it takes on
 * whatever the network sent it meanwhile.
 */
public class SubstationBlockEntity extends CityBlockEntity implements IHaveGoggleInformation {
	/** Column steps (P7: fills in ten). */
	public static final int STEPS = 10;
	/** Game time it woke while loaded (the lever's slam); far in the past if it didn't or long ago. */
	public static final long NEVER = Long.MIN_VALUE / 2;

	private long charge;
	private boolean woken;
	private double store;
	private boolean fed;
	/** The wake charge was reached: it wakes on its next tick. */
	private boolean wakeDue;
	private long wokeAt = NEVER;
	/** Game time its first upkeep falls due: a second after it woke here (the wake itself lights the lamps, D45). */
	private long upkeepFrom = NEVER;
	private int fillShown;

	private final IEnergyStorage energy = new IEnergyStorage() {
		@Override
		public int receiveEnergy(int max, boolean simulate) {
			return receive(max, simulate);
		}

		@Override
		public int extractEnergy(int max, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return woken ? (int) store : (int) Math.min(Integer.MAX_VALUE, charge);
		}

		@Override
		public int getMaxEnergyStored() {
			return woken ? (int) capacity() : CMConfig.SERVER.wastesWakeCharge();
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return !isUpper();
		}
	};

	public SubstationBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.SUBSTATION_BE.get(), pos, state);
	}

	public IEnergyStorage energy() {
		return energy;
	}

	private boolean isUpper() {
		return getBlockState().getValue(SubstationBlock.HALF) == DoubleBlockHalf.UPPER;
	}

	/** The lower half, which does the work (this one, or the one below). */
	@Nullable
	private SubstationBlockEntity lower() {
		if (!isUpper())
			return this;
		return level != null && level.getBlockEntity(worldPosition.below()) instanceof SubstationBlockEntity be && !be.isUpper() ? be : null;
	}

	public long charge() {
		return charge;
	}

	public boolean isWoken() {
		return woken;
	}

	public boolean isFed() {
		return fed;
	}

	public long wokeAt() {
		return wokeAt;
	}

	/** The column's fill, 0 to {@link #STEPS} (as last synced, on the client). */
	public int fill() {
		return fillShown;
	}

	private static double upkeepPerSecond() {
		return CMConfig.SERVER.wastesUpkeep() * 20.0;
	}

	/** Two seconds of upkeep, covering the network's once-a-second deliveries. */
	private static double capacity() {
		return Math.max(1, upkeepPerSecond() * 2);
	}

	private int receive(int max, boolean simulate) {
		if (level == null || level.isClientSide || isUpper() || max <= 0)
			return 0;
		if (!woken) {
			long need = CMConfig.SERVER.wastesWakeCharge() - charge;
			int took = (int) Math.max(0, Math.min(max, need));
			if (!simulate && took > 0) {
				charge += took;
				setChanged();
				if (charge >= CMConfig.SERVER.wastesWakeCharge())
					wakeDue = true;
			}
			return took;
		}
		int took = (int) Math.max(0, Math.min(max, Math.floor(capacity() - store)));
		if (!simulate && took > 0) {
			store += took;
			setChanged();
		}
		return took;
	}

	/** Every tick on the server's lower half; it works once a second (P52), or at once when it's due to wake. */
	void tick() {
		// (away: a game test's stand-in for an unloaded chunk; the city's record pays its upkeep on paper meanwhile)
		if (level == null || level.isClientSide || PowerGrid.away(level, worldPosition))
			return;
		long now = level.getGameTime();
		if (!wakeDue && Math.floorMod(now + worldPosition.hashCode(), 20) != 0)
			return;
		second(now);
	}

	private void second(long now) {
		if (!woken && charge >= CMConfig.SERVER.wastesWakeCharge())
			wake(now, true);
		wakeDue = false;
		if (woken && now >= upkeepFrom) {
			PowerGrid grid = level instanceof ServerLevel server ? PowerGrid.of(server) : null;
			// back from away: what the lines sent its record meanwhile
			if (grid != null)
				store = Math.min(capacity(), store + grid.consumerBack(worldPosition));
			// a second's upkeep from its store: fed while it can pay, else unfed until it can again
			double need = upkeepPerSecond();
			boolean nowFed = store + 1e-6 >= need;
			if (nowFed)
				store -= need;
			if (nowFed != fed) {
				fed = nowFed;
				sync();
			}
			CityRecord record = record();
			if (record != null && link.bound() && level instanceof ServerLevel server) {
				record.setFed(server, link.origin(), link.district(), fed, worldPosition);
				record.substationAt(link.origin(), link.district(), worldPosition);
			}
			// its record for running on paper while away (only while a line meets it)
			if (grid != null)
				grid.noteConsumer(worldPosition, CMConfig.SERVER.wastesUpkeep(), (long) capacity());
			setChanged();
		}
		showFill();
	}

	/**
	 * Wakes: for good, its district with it. {@code live}: it happened here and now (the lever slams); false when it
	 * only catches up with the record (a heart star woke the city while it was unloaded).
	 */
	private void wake(long now, boolean live) {
		if (woken)
			return;
		woken = true;
		charge = Math.max(charge, CMConfig.SERVER.wastesWakeCharge());
		wokeAt = live ? now : NEVER;
		unlock();
		if (level instanceof ServerLevel server) {
			CityRecord record = record();
			if (live) {
				// D45: the lamps ripple on at the wake, with the lever's clunk; its upkeep keeps them lit from a second on
				fed = true;
				upkeepFrom = now + 20;
			}
			if (record != null && link.bound()) {
				if (live)
					record.setFed(server, link.origin(), link.district(), true, worldPosition);
				record.wake(server, link.origin(), link.district(), worldPosition);
			} else if (live) {
				// a Substation of no city still wakes itself, with the same word to players near
				Component text = Component.translatable("comedymonkeysaddons.wastes.wakes", link.district().displayName());
				for (ServerPlayer p : server.players())
					if (p.blockPosition().distSqr(worldPosition) <= 64 * 64)
						p.displayClientMessage(text, true);
			}
		}
		showFill();
		sync();
	}

	/** Both halves become ordinary blocks. */
	private void unlock() {
		if (level == null)
			return;
		BlockState state = getBlockState();
		if (state.getValue(SubstationBlock.LOCKED))
			level.setBlock(worldPosition, state.setValue(SubstationBlock.LOCKED, false), Block.UPDATE_ALL);
		BlockState up = level.getBlockState(worldPosition.above());
		if (up.is(state.getBlock()) && up.getValue(SubstationBlock.LOCKED))
			level.setBlock(worldPosition.above(), up.setValue(SubstationBlock.LOCKED, false), Block.UPDATE_ALL);
	}

	private void showFill() {
		int fill = woken ? STEPS : (int) Math.min(STEPS, charge * STEPS / Math.max(1, CMConfig.SERVER.wastesWakeCharge()));
		if (fill != fillShown) {
			fillShown = fill;
			sync();
			if (level != null)
				level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	/** The comparator reads the wake charge: 15 once woken. */
	public int comparator() {
		if (woken)
			return 15;
		long wake = Math.max(1, CMConfig.SERVER.wastesWakeCharge());
		return charge <= 0 ? 0 : (int) Math.max(1, Math.min(15, charge * 15 / wake));
	}

	/** Its district woke some other way (the heart's star) or was already awake when it loaded: it wakes to match. */
	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		if (isUpper() || level == null)
			return;
		CityRecord record = record();
		if (record == null || !link.bound())
			return;
		if (record.awake(link.origin(), link.district()))
			wake(level.getGameTime(), live);
		else if (woken && level instanceof ServerLevel server)
			// woken while its city had no record yet (bound after waking): the record learns it now
			record.wake(server, link.origin(), link.district(), worldPosition);
	}

	/** Broken (P50): a woken one's district stays awake, its own feed gone for good. */
	void removed() {
		if (!(level instanceof ServerLevel server))
			return;
		PowerGrid.of(server).forgetConsumer(worldPosition);
		if (woken && link.bound())
			CityRecord.of(server).cut(server, link.origin(), link.district());
	}

	// ---------- saving ----------

	@Override
	protected void writeState(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putLong("Charge", charge);
		tag.putDouble("Store", store);
	}

	@Override
	protected void readState(CompoundTag tag, HolderLookup.Provider registries) {
		charge = tag.getLong("Charge");
		store = tag.getDouble("Store");
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		tag.putBoolean("Woken", woken);
		tag.putBoolean("Fed", fed);
		tag.putLong("WokeAt", wokeAt);
		tag.putInt("Fill", fillShown);
		tag.putLong("ChargeShown", charge);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		woken = tag.getBoolean("Woken");
		fed = tag.getBoolean("Fed");
		wokeAt = tag.contains("WokeAt") ? tag.getLong("WokeAt") : NEVER;
		fillShown = tag.getInt("Fill");
		if (level != null && level.isClientSide)
			charge = tag.getLong("ChargeShown");
	}

	// ---------- goggles ----------

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
		SubstationBlockEntity be = lower();
		if (be == null)
			return false;
		LangBuilder lang = new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
		lang.translate("gui.goggles.substation").forGoggles(tooltip);
		if (!be.woken) {
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.substation.charge",
					String.format(Locale.ROOT, "%,d", be.charge), String.format(Locale.ROOT, "%,d", CMConfig.SERVER.wastesWakeCharge()))
				.style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.substation.dormant").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			return true;
		}
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate(be.fed ? "gui.goggles.substation.fed" : "gui.goggles.substation.unfed")
			.style(be.fed ? ChatFormatting.GOLD : ChatFormatting.RED).forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.substation.draw",
				String.format(Locale.ROOT, "%,d", CMConfig.SERVER.wastesUpkeep()))
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		return true;
	}
}
