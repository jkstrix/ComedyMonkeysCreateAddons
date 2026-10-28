package com.comedymonkey.createaddons.content.line;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * A Power Line block's own state (see {@link LineBlockEntity}) and the shape of its wires: for each face a wire lies on,
 * how it runs off towards each edge (on along the next block, up the next face, over the edge, into a machine, under a
 * Line Post), worked out on the server whenever anything round it changes and sent to clients for the model.
 */
public class PowerLineBlockEntity extends LineBlockEntity implements NetworkReading.Shown {
	/**
	 * Per face (by {@link Direction#ordinal()}), the packed {@link WireLink}s with the {@link WireLink#BASE} and
	 * {@link WireLink#RISE} bits and the hole field (see {@link WireLink}).
	 */
	private final int[] links = new int[6];
	/** A Splitter's priority for each branch, by direction: 0 first, 1 second (the default), 2 last. */
	private final byte[] priority = {1, 1, 1, 1, 1, 1};
	@Nullable
	private VoxelShape shape;

	public PowerLineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.POWER_LINE.get(), pos, state);
	}

	@Override
	public LineTier tier() {
		return getBlockState().getBlock() instanceof PowerLineBlock line ? line.tier() : LineTier.COPPER;
	}

	@Override
	public int rate() {
		return tier().powerPerTick();
	}

	public int links(Direction face) {
		return links[face.ordinal()];
	}

	public WireLink link(Direction face, Direction d) {
		return WireLink.get(links[face.ordinal()], face, d);
	}

	/** Works every wire's links out again from the world round it; sends them on if they changed. */
	public void relink() {
		if (level == null || level.isClientSide)
			return;
		int[] next = new int[6];
		BlockState state = getBlockState();
		for (Direction face : Direction.values()) {
			if (!PowerLineBlock.hasFace(state, face))
				continue;
			int packed = 0;
			for (Direction d : WireLink.across(face))
				packed = WireLink.with(packed, face, d, linkFor(level, state, face, d));
			BlockPos support = worldPosition.relative(face);
			if (!level.isLoaded(support))
				packed |= links[face.ordinal()] & (WireLink.BASE | WireLink.BEHIND);
			else if (!(level.getBlockState(support).getBlock() instanceof LineBlock)
				&& level.getCapability(Capabilities.EnergyStorage.BLOCK, support, face.getOpposite()) != null)
				packed |= WireLink.BASE;
			// lights fixed to the block it lies on, on its other faces: fed through the block
			else if (!com.comedymonkey.createaddons.content.light.LightMounts.fixedAround(level, support, worldPosition).isEmpty())
				packed |= WireLink.BEHIND;
			// a machine across the block (over a floor wire, under a ceiling one) that takes power on the face towards it;
			// not from a wall wire (it would catch whatever stood across from a passing cable), nor when a wire lies on it already
			BlockPos across = worldPosition.relative(face.getOpposite());
			if (face.getAxis() == Direction.Axis.Y && !PowerLineBlock.hasFace(state, face.getOpposite())) {
				if (!level.isLoaded(across))
					packed |= links[face.ordinal()] & WireLink.RISE;
				else if (!(level.getBlockState(across).getBlock() instanceof LineBlock)
					&& level.getCapability(Capabilities.EnergyStorage.BLOCK, across, face) != null)
					packed |= WireLink.RISE;
			}
			next[face.ordinal()] = packed;
		}
		for (Direction d : Direction.values())
			through(level, state, d, next);
		if (!Arrays.equals(next, links)) {
			int[] before = links.clone();
			System.arraycopy(next, 0, links, 0, 6);
			// threaded through a wall, or no longer: the wire behind works its end out again, so the two always agree
			for (Direction d : Direction.values()) {
				BlockPos far = worldPosition.relative(d, 2);
				if (threaded(before, d) != threaded(links, d) && level.isLoaded(far) && level.getBlockEntity(far) instanceof PowerLineBlockEntity o)
					o.relink();
			}
			shape = null;
			changed(false);
			rerenderOnClients();
			// no longer a straight run (something joined it on the side), or for a Splitter a junction: the fitting comes off
			if (fitting() != Fitting.NONE && state.getBlock() instanceof PowerLineBlock wire && wire.sideFor(fitting(), level, worldPosition, state) == null)
				removeFitting(null);
		}
	}

	/**
	 * The way through the solid block on side d, if it's been drilled (both ends wrenched) to a wire of the same tier and colour
	 * behind it. One straight hole, at the same spot on both sides: at the edge where a floor (or other) wire on either
	 * side meets the wall, or in the middle of the wall when both wires lie on it. A floor wire runs into the hole
	 * ({@link WireLink#THROUGH}); a wire lying on the wall goes into it where it lies, or runs along to it at the edge.
	 */
	private void through(Level level, BlockState state, Direction d, int[] next) {
		BlockPos wall = worldPosition.relative(d), far = wall.relative(d);
		// beside an unloaded chunk: keep what it was
		if (!level.isLoaded(wall) || !level.isLoaded(far)) {
			for (Direction g : WireLink.across(d))
				if (link(g, d) == WireLink.THROUGH)
					next[g.ordinal()] = WireLink.with(next[g.ordinal()], g, d, WireLink.THROUGH);
			next[d.ordinal()] |= WireLink.holeBits(links[d.ordinal()]);
			return;
		}
		if (joint(d) != Joint.DRILLED || !PowerLineBlock.drillable(level, wall, d) || !(level.getBlockEntity(far) instanceof PowerLineBlockEntity o)
			|| o.tier() != tier() || o.dye() != dye() || o.joint(d.getOpposite()) != Joint.DRILLED)
			return;
		BlockState there = o.getBlockState();
		boolean onWall = PowerLineBlock.hasFace(state, d), onWallThere = PowerLineBlock.hasFace(there, d.getOpposite());
		// the edge: a floor wire on either side, met by a floor wire or a wire on the wall on the other (floor to floor first)
		Direction edge = null;
		boolean both = false;
		for (Direction g : WireLink.across(d)) {
			boolean here = PowerLineBlock.hasFace(state, g), opposite = PowerLineBlock.hasFace(there, g);
			if ((here || opposite) && (here || onWall) && (opposite || onWallThere) && (edge == null || (!both && here && opposite))) {
				edge = g;
				both = here && opposite;
			}
		}
		if (edge == null && !(onWall && onWallThere))
			return;
		// a hole joins two cable ends, one each side: neither piece may branch there
		if (!cableEnd(next, d) || !cableEnd(o.links, d.getOpposite()))
			return;
		if (onWall)
			next[d.ordinal()] = WireLink.withHole(next[d.ordinal()], d, edge);
		else
			next[edge.ordinal()] = WireLink.with(next[edge.ordinal()], edge, d, WireLink.THROUGH);
	}

	/**
	 * The wire in this block is the end of a cable, all a drilled hole joins: at most one way out of the block besides the
	 * way through the wall on side {@code toward} (runs between its own faces don't count; another hole, a machine or a
	 * post does).
	 */
	private static boolean cableEnd(int[] links, Direction toward) {
		int runs = 0;
		for (Direction f : Direction.values()) {
			int p = links[f.ordinal()];
			if ((p & WireLink.BASE) != 0)
				runs++;
			if ((p & WireLink.RISE) != 0)
				runs++;
			if ((p & WireLink.BEHIND) != 0)
				runs++;
			if (WireLink.hole(p) != 0 && f != toward)
				runs++;
			for (Direction e : WireLink.across(f)) {
				WireLink l = WireLink.get(p, f, e);
				if (l == WireLink.LINE || l == WireLink.OUTSIDE || l == WireLink.TERMINAL || l == WireLink.LAMP || l == WireLink.POST || (l == WireLink.THROUGH && e != toward))
					runs++;
			}
		}
		return runs <= 1;
	}

	/** Threaded through the wall on side d: a wire running into it, or a hole in the wall a wire lies on. */
	private static boolean threaded(int[] links, Direction d) {
		if (WireLink.hole(links[d.ordinal()]) != 0)
			return true;
		for (Direction g : WireLink.across(d))
			if (WireLink.get(links[g.ordinal()], g, d) == WireLink.THROUGH)
				return true;
		return false;
	}

	/**
	 * The wrench on side d, against a solid block with a wire of the same tier and colour behind it: drills through and threads the
	 * cable, or pulls it back out. Elsewhere, the joint as usual.
	 */
	@Override
	public void wrenchJoint(Direction d) {
		if (level == null)
			return;
		BlockPos wall = worldPosition.relative(d);
		if (!level.isLoaded(wall.relative(d)) || !PowerLineBlock.drillable(level, wall, d) || !(level.getBlockEntity(wall.relative(d)) instanceof PowerLineBlockEntity o)
			|| o.tier() != tier() || o.dye() != dye()) {
			super.wrenchJoint(d);
			return;
		}
		Joint next = joint(d) == Joint.DRILLED && o.joint(d.getOpposite()) == Joint.DRILLED ? Joint.AUTO : Joint.DRILLED;
		setJoint(d, next);
		o.setJoint(d.getOpposite(), next);
		// both ends marked: now each can find the other
		relink();
		o.relink();
		level.playSound(null, worldPosition, level.getBlockState(wall).getSoundType(level, wall, null).getHitSound(), SoundSource.BLOCKS, 1f, 0.8f);
		level.playSound(null, worldPosition, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.7f, next == Joint.DRILLED ? 1.2f : 0.8f);
	}

	private WireLink linkFor(Level level, BlockState state, Direction face, Direction d) {
		if (isCut(d))
			return WireLink.NONE;
		// a wire up the next face of this same block
		if (PowerLineBlock.hasFace(state, d))
			return WireLink.INSIDE;
		BlockPos next = worldPosition.relative(d);
		// beside an unloaded chunk: keep what it was (reading it would load it)
		if (!level.isLoaded(next) || !level.isLoaded(next.relative(face)))
			return link(face, d);
		BlockState there = level.getBlockState(next);
		if (there.getBlock() instanceof PowerLineBlock && PowerLineBlock.hasFace(there, face))
			return level.getBlockEntity(next) instanceof PowerLineBlockEntity o && joins(this, d, o) ? WireLink.LINE : WireLink.NONE;
		if (there.getBlock() instanceof LinePostBlock && there.getValue(LinePostBlock.FACING) == face.getOpposite()
			&& there.getValue(LinePostBlock.ROLE).isFoot())
			return WireLink.POST;
		// a light standing on the same surface: flat under its foot; a machine: up its face to its socket
		if (!(there.getBlock() instanceof LineBlock) && level.getCapability(Capabilities.EnergyStorage.BLOCK, next, d.getOpposite()) != null)
			return com.comedymonkey.createaddons.content.light.LightMounts.fixedTo(level, there, next, next.relative(face)) ? WireLink.LAMP : WireLink.TERMINAL;
		// over the edge: the next block is open, and a wire runs down the far side of the block this one lies on
		if (!there.isCollisionShapeFullBlock(level, next)) {
			BlockPos round = next.relative(face);
			BlockState below = level.getBlockState(round);
			if (below.getBlock() instanceof PowerLineBlock && PowerLineBlock.hasFace(below, d.getOpposite())
				&& level.getBlockEntity(round) instanceof PowerLineBlockEntity o && o.tier() == tier()
				&& o.dye() == dye())
				return WireLink.OUTSIDE;
		}
		return WireLink.NONE;
	}

	/** The wires' outline: a thin strip along each run, so they can be clicked. */
	public VoxelShape wireShape() {
		if (shape == null)
			shape = makeShape();
		return shape;
	}

	private VoxelShape makeShape() {
		VoxelShape out = Shapes.empty();
		BlockState state = getBlockState();
		for (Direction face : Direction.values()) {
			if (!PowerLineBlock.hasFace(state, face))
				continue;
			out = Shapes.or(out, PowerLineBlock.plate(face, null));
			Direction hole = WireLink.holeEdge(links[face.ordinal()], face);
			for (Direction d : WireLink.across(face))
				if (link(face, d).connected() || d == hole) {
					out = Shapes.or(out, PowerLineBlock.plate(face, d));
					// up the machine's face to its socket: a wire lying on that face, running from this one
					if (link(face, d) == WireLink.TERMINAL)
						out = Shapes.or(out, PowerLineBlock.plate(d, face));
				}
			if ((links[face.ordinal()] & WireLink.RISE) != 0)
				out = Shapes.or(out, net.minecraft.world.level.block.Block.box(7, 0, 7, 9, 16, 9));
		}
		return out.optimize();
	}

	@Override
	protected void refreshState() {
		relink();
		if (level != null)
			for (Direction d : Direction.values()) {
				if (level.isLoaded(worldPosition.relative(d)) && level.getBlockEntity(worldPosition.relative(d)) instanceof PowerLineBlockEntity o)
					o.relink();
				// and the one behind a wall it's threaded through
				if (level.isLoaded(worldPosition.relative(d, 2)) && level.getBlockEntity(worldPosition.relative(d, 2)) instanceof PowerLineBlockEntity o)
					o.relink();
			}
	}

	// ---------- a Splitter's priorities ----------

	/** The ways the wire runs off its face (a junction's branches), in order round it. */
	public List<Direction> branches() {
		Direction face = face();
		List<Direction> out = new java.util.ArrayList<>();
		if (face != null)
			for (Direction d : WireLink.across(face))
				if (link(face, d).connected())
					out.add(d);
		return out;
	}

	/** The face it lies on, if only one. */
	@Nullable
	public Direction face() {
		Direction face = null;
		for (Direction d : Direction.values())
			if (PowerLineBlock.hasFace(getBlockState(), d)) {
				if (face != null)
					return null;
				face = d;
			}
		return face;
	}

	/** A Splitter's priority for the branch that way: 0 first, 1 second (the default), 2 last. */
	public int priority(Direction d) {
		return priority[d.ordinal()];
	}

	public byte[] priorities() {
		return priority.clone();
	}

	/** From its screen, checked on the server: only a real change rebuilds the network. */
	public void setPriorities(byte[] levels) {
		byte[] next = priority.clone();
		for (int i = 0; i < 6 && i < levels.length; i++)
			next[i] = (byte) Math.clamp(levels[i], 0, 2);
		if (Arrays.equals(next, priority))
			return;
		System.arraycopy(next, 0, priority, 0, 6);
		changed(false);
	}

	/** The branch served first, for the dial's pointer: only when exactly one branch comes first (else null). */
	@Nullable
	public Direction firstBranch() {
		Direction first = null;
		int ties = 0;
		for (Direction d : branches()) {
			if (first == null || priority(d) < priority(first)) {
				first = d;
				ties = 1;
			} else if (priority(d) == priority(first)) {
				ties++;
			}
		}
		return ties == 1 ? first : null;
	}

	@Override
	protected Component splitterLine() {
		Direction first = firstBranch();
		if (first != null)
			return Component.translatable(key("splitter_first"),
				Component.translatable("comedymonkeysaddons.gui.splitter.dir_" + first.getName()));
		List<Direction> b = branches();
		for (Direction d : b)
			if (priority(d) != priority(b.get(0)))
				return Component.translatable(key("splitter_ranked"));
		return Component.translatable(key("splitter_even"));
	}

	@Override
	public void setFitting(Fitting f, Direction side) {
		Arrays.fill(priority, (byte) 1);
		super.setFitting(f, side);
	}

	/** A Splitter from before they went on junctions, on a straight piece: it comes off (given back) when it loads. */
	private void dropStraightSplitter() {
		if (fitting() == Fitting.SPLITTER && level != null && getBlockState().getBlock() instanceof PowerLineBlock wire
			&& wire.junctionSide(level, worldPosition, getBlockState()) == null)
			removeFitting(null);
	}

	@Override
	protected void networkChanged(ServerLevel level) {
		PowerLines.of(level).changed(worldPosition);
		if (hasTerminal())
			PowerLines.of(level).addTerminal(worldPosition);
	}

	/** Loaded: the grid already has it unless it's new to it (it can't have changed while it was unloaded). */
	@Override
	protected void networkLoaded(ServerLevel level) {
		PowerLines.of(level).loaded(worldPosition);
		if (hasTerminal())
			PowerLines.of(level).addTerminal(worldPosition);
	}

	/** Unloading changes nothing: the grid keeps it as it was. */
	@Override
	protected void networkUnloaded(ServerLevel level) {}

	/** The lights it feeds through its block changed: the network works its devices out again. */
	public void devicesChanged() {
		changed(false);
	}

	/** Some wire here meets a machine or lies on one: where the network's work starts. */
	public boolean hasTerminal() {
		for (Direction face : Direction.values()) {
			if ((links[face.ordinal()] & (WireLink.BASE | WireLink.RISE | WireLink.BEHIND)) != 0)
				return true;
			for (Direction d : WireLink.across(face))
				if (link(face, d) == WireLink.TERMINAL || link(face, d) == WireLink.LAMP)
					return true;
		}
		return false;
	}

	private void rerenderOnClients() {
		sync();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide) {
			relink();
			dropStraightSplitter();
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		int[] old = links.clone();
		super.loadAdditional(tag, registries);
		int[] read = tag.getIntArray("Links");
		Arrays.fill(links, 0);
		System.arraycopy(read, 0, links, 0, Math.min(read.length, 6));
		reading.read(tag);
		Arrays.fill(priority, (byte) 1);
		byte[] levels = tag.getByteArray("Priority");
		for (int i = 0; i < 6 && i < levels.length; i++)
			priority[i] = (byte) Math.clamp(levels[i], 0, 2);
		shape = null;
		// (the base class told the network before these were read)
		if (level instanceof ServerLevel server && hasTerminal())
			PowerLines.of(server).addTerminal(worldPosition);
		if (level != null && level.isClientSide && !Arrays.equals(old, links)) {
			requestModelDataUpdate();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putIntArray("Links", links);
		tag.putByteArray("Priority", priority);
	}

	@Override
	protected void writeClient(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putIntArray("Links", links);
		tag.putByteArray("Priority", priority);
		reading.write(tag);
	}

	@Override
	protected void addLineTooltip(List<Component> tooltip) {
		line(tooltip, Component.translatable(key("power_line_capacity"), rate()));
		reading.addTo(tooltip, level, 1);
	}

	/** Its network's figures, set on the wires that meet a machine. */
	private final NetworkReading reading = new NetworkReading();

	@Override
	public void networkReading(int makes, int uses) {
		if (level != null && reading.set(makes, uses, level.getGameTime()))
			sync();
	}

	@Override
	protected String meterUnitKey() {
		return "meter_power";
	}
}
