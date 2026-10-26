package com.comedymonkey.createaddons.content.electric;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.SaveMark;
import com.comedymonkey.createaddons.content.line.LineBlock;
import com.comedymonkey.createaddons.content.line.PowerLines;
import com.comedymonkey.createaddons.content.line.Span;
import com.comedymonkey.createaddons.content.line.SpanHolder;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * A Battery's charge, and its part in a bank or an Industrial Battery. Its charge is its share of its group's store
 * ({@link BatteryStore}); while the store lives that share is worked out from it, so it's always exact when saved,
 * dropped or carried off. A plain Battery pushes into the machines on its connectors (a motor beside it runs from it, as
 * from a generator); Power Lines and other mods' cables draw and fill it themselves.
 * <p>
 * Formed, it knows its tower: the bottom-front-middle block (the master, which keeps the store, takes the cables and
 * draws the moving parts), the tower's height and the way its front faces. Each block shows its own tile of the tower.
 */
public class BatteryBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, SpanHolder, SpanHolder.Part,
	com.comedymonkey.createaddons.content.line.PowerGrid.Store {
	/** Which tile of the tower a formed block shows (the tile models' index, by height, layer and place, turned to its front). */
	public record Tile(int height, int layer, int ix, int iz, Direction facing) {}

	public static final ModelProperty<Tile> TILE = new ModelProperty<>();

	/** Its share, kept for when it isn't in a live store (and saved). */
	int charge;
	@Nullable
	BatteryStore store;
	/** Formed: the tower's master, height and front. */
	@Nullable
	BlockPos master;
	int height;
	Direction towerFacing = Direction.NORTH;
	/** Where it stood when it formed: put down anywhere else (moved by a contraption), its tower isn't there. */
	@Nullable
	BlockPos formedAt;
	/** A master's spans of Power Line, from its four insulators (front-left, front-right, back-left, back-right) to posts. */
	private final List<Span> spans = new ArrayList<>();
	/** Server: what's on each side, for pushing into machines. */
	@SuppressWarnings("unchecked")
	private final BlockCapabilityCache<IEnergyStorage, Direction>[] sides = new BlockCapabilityCache[4];
	/** What's shown, as the client last heard it: how full, which way it flows, the figures for the goggles. */
	float shownFill;
	byte shownFlow;
	long shownEnergy, shownCapacity;
	float shownIn, shownOut;
	int shownSize = 1;
	private float syncedFill = -1;
	private byte syncedFlow;
	/** Client: ticks since a lightning strike, from the top block it struck (-1 none), for the arcs and the flash. */
	public int strikeAge = -1, strikeAt;
	/** Client: ticks since the tower formed (-1 none), for the ripple up its column. */
	public int formAge = -1;
	/** Client: the tower's level column as drawn, easing toward the charge (this tick and the last). */
	private float drawn = -1, drawnLast;

	/** What machines and cables see: the store, whatever it is at the moment (nothing while it can't be worked out). */
	private final IEnergyStorage view = new com.comedymonkey.createaddons.content.line.PowerGrid.RefundingStorage() {
		/** Not while this block isn't running (its chunk loaded but still): its store may be on paper then. */
		@Nullable
		private BatteryStore live() {
			return com.comedymonkey.createaddons.content.line.PowerGrid.givesHere(level, worldPosition) ? store() : null;
		}

		@Override
		public int receiveEnergy(int max, boolean simulate) {
			BatteryStore s = live();
			return s == null ? 0 : s.receiveEnergy(max, simulate);
		}

		@Override
		public int extractEnergy(int max, boolean simulate) {
			BatteryStore s = live();
			return s == null ? 0 : s.extractEnergy(max, simulate);
		}

		@Override
		public int refund(int fe) {
			BatteryStore s = store();
			return s == null ? 0 : s.refund(fe);
		}

		@Override
		public int getEnergyStored() {
			BatteryStore s = store();
			return s == null ? (int) Math.min(Integer.MAX_VALUE, shownEnergy) : s.getEnergyStored();
		}

		@Override
		public int getMaxEnergyStored() {
			BatteryStore s = store();
			return s == null ? (int) Math.min(Integer.MAX_VALUE, shownCapacity) : s.getMaxEnergyStored();
		}

		@Override
		public boolean canExtract() {
			return true;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	};

	public BatteryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BATTERY.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	/** The energy capability: a plain Battery's on its connectors; an Industrial Battery's only at its master, for the network. */
	@Nullable
	public IEnergyStorage energy(@Nullable Direction side) {
		BlockState state = getBlockState();
		if (state.getValue(BatteryBlock.FORMED))
			return isMaster() && (side == null || side == Direction.UP) ? view : null;
		return side == null || BatteryBlock.isConnector(state, side) ? view : null;
	}

	/** Formed and plain have different connectors: what's round it looks again. */
	@Override
	public void setBlockState(BlockState state) {
		BlockState old = getBlockState();
		super.setBlockState(state);
		if (old.hasProperty(BatteryBlock.FORMED) && state.hasProperty(BatteryBlock.FORMED)
			&& old.getValue(BatteryBlock.FORMED) != state.getValue(BatteryBlock.FORMED))
			invalidateCapabilities();
	}

	// ---------- the tower ----------

	public boolean isFormed() {
		return master != null && getBlockState().getValue(BatteryBlock.FORMED);
	}

	public boolean isMaster() {
		return isFormed() && worldPosition.equals(master);
	}

	@Nullable
	public BlockPos master() {
		return master;
	}

	public int height() {
		return height;
	}

	public Direction towerFacing() {
		return towerFacing;
	}

	/** This block's tile, if it's part of a tower. */
	@Nullable
	public Tile tile() {
		if (master == null || height < BatteryTowers.MIN)
			return null;
		BlockPos rel = worldPosition.subtract(BatteryTowers.center(master, towerFacing));
		Direction right = towerFacing.getClockWise(), back = towerFacing.getOpposite();
		int ix = 1 + rel.getX() * right.getStepX() + rel.getZ() * right.getStepZ();
		int iz = 1 + rel.getX() * back.getStepX() + rel.getZ() * back.getStepZ();
		int layer = rel.getY();
		if (ix < 0 || ix > 2 || iz < 0 || iz > 2 || layer < 0 || layer >= height)
			return null;
		return new Tile(height, layer, ix, iz, towerFacing);
	}

	@Override
	public ModelData getModelData() {
		Tile t = getBlockState().getValue(BatteryBlock.FORMED) ? tile() : null;
		return t == null ? ModelData.EMPTY : ModelData.builder().with(TILE, t).build();
	}

	/** Its tower's master, if it's loaded and still its tower. */
	@Nullable
	BatteryBlockEntity masterEntity() {
		if (master == null || level == null || !level.isLoaded(master))
			return null;
		return level.getBlockEntity(master) instanceof BatteryBlockEntity m && m.isMaster() && master.equals(m.master) ? m : null;
	}

	/** Made part of a tower (or, with a null master, plain again). */
	void setTower(@Nullable BlockPos master, int height, Direction facing) {
		this.master = master == null ? null : master.immutable();
		this.height = master == null ? 0 : height;
		this.towerFacing = facing;
		this.formedAt = master == null ? null : worldPosition.immutable();
		SaveMark.mark(this);
	}

	// ---------- its insulators ----------

	/** An insulator's top, above the tower's top layer, and its distance in from the tower's corners (px). */
	private static final float INSULATOR_TOP = 15.55f - 1, INSULATOR_IN = 5.5f;

	@Override
	public List<Span> spans() {
		return spans;
	}

	@Override
	public boolean holdsSpans() {
		return isMaster();
	}

	@Override
	public boolean isBattery() {
		return true;
	}

	@Override
	public BlockPos holderPos() {
		return worldPosition;
	}

	@Override
	public boolean hasInsulator(int insulator) {
		return isMaster() && insulator >= 0 && insulator < 4;
	}

	/** Insulator 0 to 3: on the top corners, front-left, front-right, back-left, back-right as the tower faces north. */
	@Override
	public Vec3 tie(int insulator) {
		BlockPos c = BatteryTowers.center(worldPosition, towerFacing);
		float dx = ((insulator & 1) == 0 ? INSULATOR_IN : 48 - INSULATOR_IN) - 24, dz = ((insulator & 2) == 0 ? INSULATOR_IN : 48 - INSULATOR_IN) - 24;
		Direction right = towerFacing.getClockWise(), back = towerFacing.getOpposite();
		return new Vec3(c.getX() + 0.5 + (right.getStepX() * dx + back.getStepX() * dz) / 16,
			c.getY() + (height * 16 + INSULATOR_TOP) / 16, c.getZ() + 0.5 + (right.getStepZ() * dx + back.getStepZ() * dz) / 16);
	}

	@Override
	public int nearestInsulator(Vec3 at) {
		int best = 0;
		for (int i = 1; i < 4; i++)
			if (tie(i).distanceToSqr(at) < tie(best).distanceToSqr(at))
				best = i;
		return best;
	}

	@Override
	public void addSpan(Span span) {
		spans.add(span);
		spansChanged();
	}

	@Override
	public boolean removeSpan(Span span) {
		boolean held = spans.remove(span);
		if (held)
			spansChanged();
		return held;
	}

	/** Its tower's master, which holds the spans. */
	@Override
	@Nullable
	public SpanHolder spanHolder() {
		if (!getBlockState().getValue(BatteryBlock.FORMED))
			return null;
		if (isMaster())
			return this;
		if (master == null || level == null || !level.isLoaded(master))
			return null;
		return level.getBlockEntity(master) instanceof BatteryBlockEntity m && m.isMaster() ? m : null;
	}

	/** Shears: every span on an insulator comes down (here and at the post); returns the Power Lines they hung. */
	public List<net.minecraft.world.item.ItemStack> takeDown(int insulator) {
		List<net.minecraft.world.item.ItemStack> refund = new ArrayList<>();
		for (Span sp : spansAt(insulator)) {
			spans.remove(sp);
			BlockPos farPos = worldPosition.offset(sp.offset());
			SpanHolder far = level != null && level.isLoaded(farPos) ? SpanHolder.at(level, farPos) : null;
			if (far != null && far.removeSpan(sp.reversed()) || sp.owner())
				refund.add(com.comedymonkey.createaddons.content.line.LinePostBlockEntity.wireFor(sp));
		}
		spansChanged();
		return refund;
	}

	/** Spans whose post is gone or no longer holds them come down (the post's end pays for a span, so nothing is dropped here). */
	void dropBrokenSpans() {
		if (level == null || level.isClientSide || spans.isEmpty())
			return;
		boolean changed = false;
		for (Span sp : new ArrayList<>(spans)) {
			BlockPos far = worldPosition.offset(sp.offset());
			if (!level.isLoaded(far))
				continue;
			SpanHolder h = SpanHolder.at(level, far);
			if (h == null || !h.spans().contains(sp.reversed())) {
				spans.remove(sp);
				changed = true;
			}
		}
		if (changed)
			spansChanged();
	}

	/** Put down from elsewhere (a contraption, Carry On) as a plain Battery: the spans it carried belong to no tower here. */
	void forgetSpans() {
		if (!spans.isEmpty()) {
			spans.clear();
			spansChanged();
		}
	}

	/** Its spans, taken off without touching the far ends (it's coming apart: they go back on if it re-forms here). */
	List<Span> takeSpans() {
		List<Span> out = new ArrayList<>(spans);
		if (!spans.isEmpty()) {
			spans.clear();
			spansChanged();
		}
		return out;
	}

	void giveSpans(List<Span> back) {
		spans.addAll(back);
		spansChanged();
	}

	private void spansChanged() {
		SaveMark.mark(this);
		if (level instanceof ServerLevel server) {
			sendData();
			PowerLines.of(server).changed(worldPosition);
		}
	}

	/** Client: its tower or spans changed, or it came into view (the posts its spans hang to lay them out again); set by the client setup. */
	public static java.util.function.Consumer<BatteryBlockEntity> clientTowerChanged = be -> {};

	// ---------- the store ----------

	/** The store it's part of now, worked out if it has to be (server only; null while part of its group isn't loaded). */
	@Nullable
	public BatteryStore store() {
		if (level == null || level.isClientSide || isRemoved())
			return null;
		// (game tests: marked away, its store is on paper)
		if (com.comedymonkey.createaddons.content.line.PowerGrid.away(level, worldPosition))
			return null;
		if (store != null && store.valid())
			return store;
		store = null;
		if (getBlockState().getValue(BatteryBlock.FORMED)) {
			if (!isMaster()) {
				BatteryBlockEntity m = masterEntity();
				return m == null ? null : m.store();
			}
			return store = buildTower();
		}
		return store = buildBank();
	}

	/** The store it's in, if there is one live, without working one out. */
	@Nullable
	private BatteryStore liveStore() {
		if (store != null && store.valid())
			return store;
		if (getBlockState().getValue(BatteryBlock.FORMED) && !isMaster()) {
			BatteryBlockEntity m = masterEntity();
			if (m != null && m.store != null && m.store.valid() && m.store.contains(this))
				return m.store;
		}
		return null;
	}

	/** Its store's key on the network: a bank's first Battery, or a tower's master. */
	@Override
	@Nullable
	public BlockPos storeKey() {
		BatteryStore s = store();
		return s == null ? null : s.tower ? master : s.members.get(0).worldPosition;
	}

	/** Where lines meet its store: every Battery of a bank; a tower only at its master. */
	@Override
	public java.util.Collection<BlockPos> storeBlocks() {
		BatteryStore s = store();
		if (s == null)
			return List.of(worldPosition);
		if (s.tower)
			return List.of(master);
		List<BlockPos> out = new ArrayList<>(s.members.size());
		for (BatteryBlockEntity be : s.members)
			out.add(be.worldPosition);
		return out;
	}

	/** Its share of the store now (its own charge while it isn't in one). */
	public int share() {
		BatteryStore s = liveStore();
		return s == null ? charge : s.shareOf(this);
	}

	/** Sets what it holds (game tests): its group keeps their shares, and it joins them again with this. */
	public void setCharge(int fe) {
		dissolveStore();
		charge = Math.max(0, fe);
		SaveMark.mark(this);
	}

	/** Its group changed: everyone in it keeps their share, and the store is worked out again when next needed. */
	void dissolveStore() {
		BatteryStore s = liveStore();
		if (s != null)
			s.dissolve();
		store = null;
	}

	/** Every plain Battery joined to this one through their connectors (loaded ones only: it never loads a chunk). */
	private BatteryStore buildBank() {
		List<BatteryBlockEntity> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(worldPosition);
		seen.add(worldPosition);
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			if (!(level.getBlockEntity(p) instanceof BatteryBlockEntity be) || be.isRemoved() || !BatteryBlock.isPlain(be.getBlockState()))
				continue;
			found.add(be);
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos q = p.relative(d);
				if (seen.add(q) && level.isLoaded(q) && BatteryBlock.isPlain(level.getBlockState(q)))
					queue.add(q);
			}
		}
		return assemble(found, false, (long) found.size() * CMConfig.SERVER.batteryCapacity(),
			(long) found.size() * CMConfig.SERVER.batteryFePerTick());
	}

	/** The whole tower, if all of it is loaded. */
	@Nullable
	private BatteryStore buildTower() {
		List<BatteryBlockEntity> found = new ArrayList<>();
		for (BlockPos p : BatteryTowers.blocks(master, height, towerFacing)) {
			if (!level.isLoaded(p) || com.comedymonkey.createaddons.content.line.PowerGrid.away(level, p))
				return null;
			if (!(level.getBlockEntity(p) instanceof BatteryBlockEntity be) || be.isRemoved() || !be.isFormed() || !master.equals(be.master)
				|| be.formedAt != null && !p.equals(be.formedAt)) {
				// all of it here, and something's wrong (a block gone without its tower knowing): worked out again
				level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
				return null;
			}
			found.add(be);
		}
		// as much as its cables carry: no rate of its own
		return assemble(found, true, (long) found.size() * CMConfig.SERVER.industrialBatteryCapacity(), Long.MAX_VALUE / 64);
	}

	private BatteryStore assemble(List<BatteryBlockEntity> found, boolean tower, long capacity, long perTick) {
		// any store one of them was still in gives them their shares first, so the new one starts from what they hold
		for (BatteryBlockEntity be : found)
			if (be != this && be.store != null && be.store.valid())
				be.store.dissolve();
		found.sort((a, b) -> a.worldPosition.compareTo(b.worldPosition));
		BatteryStore s = new BatteryStore(found, tower, capacity, perTick);
		for (BatteryBlockEntity be : found)
			be.store = s;
		// back from being away: what its record did meanwhile
		if (level instanceof ServerLevel server) {
			List<BlockPos> blocks = new ArrayList<>(found.size());
			for (BatteryBlockEntity be : found)
				blocks.add(be.worldPosition);
			long delta = com.comedymonkey.createaddons.content.line.PowerGrid.of(server).storeBack(blocks);
			if (delta != 0)
				s.resume(delta);
			// the networks meeting it are worked out again, so lines meeting any of it are one network
			PowerLines.of(server).storeChanged(blocks);
		}
		return s;
	}

	/** Joining any Batteries beside it: their banks are worked out again, with it. */
	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && level.isClientSide && master != null)
			clientTowerChanged.accept(this);
		// a formed block checks its tower is still here (part of it may have gone while this chunk was unloaded)
		if (level instanceof ServerLevel && BatteryBlock.isFormed(getBlockState()))
			level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
		if (level instanceof ServerLevel server && master != null && worldPosition.equals(master)) {
			PowerLines.of(server).loaded(worldPosition);
			// a tick later, once the posts have loaded too
			server.getServer().execute(this::dropBrokenSpans);
		}
		if (level == null || level.isClientSide || !BatteryBlock.isPlain(getBlockState()))
			return;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos q = worldPosition.relative(d);
			if (level.isLoaded(q) && level.getBlockEntity(q) instanceof BatteryBlockEntity be && BatteryBlock.isPlain(be.getBlockState()))
				be.dissolveStore();
		}
	}

	/**
	 * Gone from the world (not unloaded): broken, or taken by a contraption or Carry On, which take the block entity
	 * before the block, so the block can't do it. Its tower comes apart, nothing re-forming (the rest may be going too);
	 * after a break the block has done it already.
	 */
	@Override
	public void remove() {
		super.remove();
		if (level instanceof ServerLevel server)
			BatteryTowers.removed(server, worldPosition, getBlockState(), this, true);
	}

	/** Broken or unloaded: what it was part of goes on without it (it keeps its share). */
	@Override
	public void invalidate() {
		super.invalidate();
		if (level != null && !level.isClientSide)
			dissolveStore();
	}

	/** A lightning strike on a rod on top: as much as fits, past its rate; arcs and a crackle either way. */
	public void struck(BlockPos top) {
		BatteryStore s = store();
		if (s == null || !(level instanceof ServerLevel server))
			return;
		s.add(CMConfig.SERVER.lightningStrikeFe());
		BatteryBlockEntity shows = isFormed() ? masterEntity() : this;
		if (shows == null)
			return;
		int at = 4;
		Tile t = tile();
		if (t != null)
			at = t.ix() + t.iz() * 3;
		server.blockEvent(shows.worldPosition, shows.getBlockState().getBlock(), BatteryTowers.EVENT_STRIKE, at);
		BatteryTowers.play(server, BatteryTowers.Sound.STRIKE, top.above());
	}

	@Override
	public boolean triggerEvent(int id, int param) {
		if (id == BatteryTowers.EVENT_STRIKE) {
			strikeAge = 0;
			strikeAt = param;
			return true;
		}
		if (id == BatteryTowers.EVENT_FORM) {
			formAge = 0;
			return true;
		}
		return super.triggerEvent(id, param);
	}

	// ---------- ticking ----------

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			if (strikeAge >= 0 && ++strikeAge > 40)
				strikeAge = -1;
			if (formAge >= 0 && ++formAge > 60)
				formAge = -1;
			drawnLast = drawn < 0 ? shownFill : drawn;
			drawn = drawn < 0 ? shownFill : drawn + (shownFill - drawn) * 0.15f;
			clientTicker.accept(this);
			return;
		}
		// (game tests: marked away, it stands still as if its chunk weren't running, and its store goes on paper)
		if (com.comedymonkey.createaddons.content.line.PowerGrid.away(level, worldPosition)) {
			dissolveStore();
			return;
		}
		BlockState state = getBlockState();
		// formed, only the master does anything
		if (state.getValue(BatteryBlock.FORMED) && !isMaster())
			return;
		BatteryStore s = store();
		if (s == null)
			return;
		long now = level.getGameTime();
		// the first of its Batteries to run this tick looks after it: its chunk stopped and started again without
		// unloading (it was away, so it's worked out again and takes on what its record did meanwhile), and its record
		boolean first = s.ran != now;
		if (first && s.ran != Long.MIN_VALUE && now - s.ran > 1) {
			s.dissolve();
			s = store();
			if (s == null)
				return;
		}
		s.ran = now;
		s.roll(now);
		// its record, for when all of it is away: from whichever of its Batteries runs first each tick
		if (first) {
			BlockPos key = storeKey();
			BatteryStore noted = s;
			if (key != null && level instanceof ServerLevel server)
				com.comedymonkey.createaddons.content.line.PowerGrid.of(server).noteStore(key, () -> {
						List<BlockPos> blocks = new ArrayList<>(noted.members.size());
						for (BatteryBlockEntity be : noted.members)
							blocks.add(be.worldPosition);
						return blocks;
					}, s.energy(), s.capacity(), s.perTick(), s.tower, Math.floorMod(key.hashCode() + now, 20) == 0);
		}
		if (!state.getValue(BatteryBlock.FORMED))
			push(s);
		show(s, now);
	}

	/** Into the machines on its connectors that only take (not Batteries, generators, lines or other mods' cables). */
	private void push(BatteryStore s) {
		if (s.energy() <= 0 || !(level instanceof ServerLevel server))
			return;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			int i = d.get2DDataValue();
			BlockPos next = worldPosition.relative(d);
			BlockState there = level.getBlockState(next);
			if (there.isAir() || there.getBlock() instanceof BatteryBlock || there.getBlock() instanceof LineBlock)
				continue;
			if (sides[i] == null)
				sides[i] = EnergyBuffer.cacheFor(server, worldPosition, d);
			IEnergyStorage target = sides[i].getCapability();
			if (target == null || !target.canReceive() || target.canExtract())
				continue;
			int can = target.receiveEnergy(s.extractEnergy(Integer.MAX_VALUE, true), true);
			if (can <= 0)
				continue;
			int taken = s.extractEnergy(can, false);
			int sent = target.receiveEnergy(taken, false);
			if (sent < taken)
				s.refund(taken - sent);
		}
	}

	/** Tells the client how full it is and which way it's flowing, when that changes (or now and then while it flows). */
	private void show(BatteryStore s, long now) {
		float in = s.inRate(), out = s.outRate();
		byte flow = (byte) (in - out > 0.5f ? 1 : out - in > 0.5f ? -1 : 0);
		float fill = s.fill();
		shownFill = fill;
		shownFlow = flow;
		shownEnergy = s.energy();
		shownCapacity = s.capacity();
		shownIn = in;
		shownOut = out;
		shownSize = s.members.size();
		if (now % 10 != 0)
			return;
		// a tower's master shows a smooth column; a Battery's tubes go in eighths (and the goggles' figures, now and then)
		boolean changed = s.tower ? Math.abs(fill - syncedFill) >= 1 / 256f || flow != 0 && now % 40 == 0
			: Math.round(fill * 8) != Math.round(syncedFill * 8) || flow != 0 && now % 100 == 0;
		if (syncedFill < 0 || changed || flow != syncedFlow) {
			syncedFill = fill;
			syncedFlow = flow;
			sendData();
		}
	}

	/** Client: run every client tick (the tower's hum); set by the client setup. */
	public static java.util.function.Consumer<BatteryBlockEntity> clientTicker = be -> {};

	// ---------- goggles ----------

	/** The figures to show: its own, or for a block of a tower, its master's. */
	private BatteryBlockEntity shown() {
		if (getBlockState().getValue(BatteryBlock.FORMED) && !isMaster() && master != null && level != null
			&& level.getBlockEntity(master) instanceof BatteryBlockEntity m)
			return m;
		return this;
	}

	public float shownFill() {
		return shownFill;
	}

	public byte shownFlow() {
		return shownFlow;
	}

	/** Client: the level column's height, 0 to 1, between ticks. */
	public float drawnFill(float partialTicks) {
		return drawn < 0 ? shownFill : net.minecraft.util.Mth.lerp(partialTicks, drawnLast, drawn);
	}

	public float shownRate() {
		return Math.abs(shownIn - shownOut);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		BatteryBlockEntity m = shown();
		boolean formed = m.getBlockState().getValue(BatteryBlock.FORMED);
		LangBuilder lang = new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
		lang.translate(formed ? "gui.goggles.industrial_battery" : "gui.goggles.battery").forGoggles(tooltip);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.text(String.format(Locale.ROOT, "%,d / %,d FE", m.shownEnergy, m.shownCapacity)).style(ChatFormatting.AQUA)
			.forGoggles(tooltip, 1);
		if (!formed && m.shownSize > 1)
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.battery.bank", m.shownSize)
				.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		float rate = m.shownRate();
		String key = m.shownFlow > 0 ? "charging" : m.shownFlow < 0 ? "giving" : "idle";
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.translate("gui.goggles.battery." + key, String.format(Locale.ROOT, "%,.0f", rate))
			.style(m.shownFlow > 0 ? ChatFormatting.GREEN : m.shownFlow < 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);
		if (m.shownFlow != 0 && rate > 0) {
			long left = m.shownFlow > 0 ? Math.max(0, m.shownCapacity - m.shownEnergy) : m.shownEnergy;
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
				.translate(m.shownFlow > 0 ? "gui.goggles.battery.full_in" : "gui.goggles.battery.empty_in", time((long) (left / rate / 20)))
				.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		}
		return true;
	}

	/** Seconds as 45s, 3m 20s or 2h 05m. */
	static String time(long seconds) {
		if (seconds < 60)
			return seconds + "s";
		if (seconds < 3600)
			return String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60);
		return String.format(Locale.ROOT, "%dh %02dm", seconds / 3600, seconds / 60 % 60);
	}

	// ---------- saving ----------

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putInt("Charge", share());
		if (master != null) {
			tag.putLong("Master", master.asLong());
			tag.putInt("Height", height);
			tag.putString("TowerFacing", towerFacing.getSerializedName());
			if (formedAt != null)
				tag.putLong("FormedAt", formedAt.asLong());
		}
		if (!spans.isEmpty()) {
			ListTag list = new ListTag();
			for (Span sp : spans)
				list.add(sp.save());
			tag.put("Spans", list);
		}
		if (clientPacket) {
			tag.putFloat("Fill", shownFill);
			tag.putByte("Flow", shownFlow);
			tag.putLong("Energy", shownEnergy);
			tag.putLong("Capacity", shownCapacity);
			tag.putFloat("In", shownIn);
			tag.putFloat("Out", shownOut);
			tag.putInt("Size", shownSize);
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		Tile beforeTile = clientPacket ? tile() : null;
		charge = tag.getInt("Charge");
		if (tag.contains("Master")) {
			master = BlockPos.of(tag.getLong("Master"));
			height = tag.getInt("Height");
			Direction f = Direction.byName(tag.getString("TowerFacing"));
			towerFacing = f == null || f.getAxis().isVertical() ? Direction.NORTH : f;
			formedAt = tag.contains("FormedAt") ? BlockPos.of(tag.getLong("FormedAt")) : null;
		} else {
			master = null;
			height = 0;
			formedAt = null;
		}
		List<Span> before = clientPacket ? new ArrayList<>(spans) : List.of();
		spans.clear();
		for (Tag t : tag.getList("Spans", Tag.TAG_COMPOUND))
			spans.add(Span.load((CompoundTag) t));
		// what it shows: sent to the client (and written into Ponder's structures)
		if (tag.contains("Fill")) {
			shownFill = tag.getFloat("Fill");
			shownFlow = tag.getByte("Flow");
			shownEnergy = tag.getLong("Energy");
			shownCapacity = tag.getLong("Capacity");
			shownIn = tag.getFloat("In");
			shownOut = tag.getFloat("Out");
			shownSize = Math.max(1, tag.getInt("Size"));
		}
		if (!clientPacket)
			return;
		// a new tile: the block is drawn again with it (and the spans to a master that moved, or came or went, too)
		boolean newTile = !java.util.Objects.equals(beforeTile, tile());
		if (level != null && newTile) {
			requestModelDataUpdate();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 0);
		}
		if (level != null && (newTile && (before.size() > 0 || !spans.isEmpty()) || !before.equals(spans)))
			clientTowerChanged.accept(this);
	}

	/** As an item it keeps its share. */
	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		int share = share();
		if (share > 0)
			components.set(ModDataComponents.BATTERY_CHARGE.get(), share);
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		charge = Math.max(0, input.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), 0));
	}

	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		super.removeComponentsFromTag(tag);
		tag.remove("Charge");
	}
}
