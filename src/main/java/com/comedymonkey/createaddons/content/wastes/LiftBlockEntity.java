package com.comedymonkey.createaddons.content.wastes;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * A Lift's drive (design/specs/clockwork-wastes.md, P49, D24, D40): it keeps where its deck is and where it's going,
 * and the deck ({@link LiftDeck}, an entity the drive spawns and owns, never saved itself) follows it, carrying whatever
 * stands on it. The motion is a pure function of the game time ({@link #deckY}), synced to clients, so the server, the
 * clients' decks (collision, riding) and the renderer agree to the tick without position packets.
 * <p>
 * The shaft is the 2 x 2 of air below the drive down to the first solid block: its floor is the bottom landing, and every
 * {@link LiftCallPlateBlock} within {@link #PLATE_REACH} blocks of the shaft marks a landing at its own level (the deck
 * stops with its underside level with that landing's floor, as in the preview). It moves only when called: by a player
 * standing on the deck using its control post (up to the next landing; sneaking, down), or by a player using a landing's
 * call plate. While it moves, anything in its way stops it where it is (the deck checks every tick).
 * <p>
 * Numbers the spec and preview don't give (waiting for the user): {@link #SPEED} (the preview's ride: about a block a
 * second), {@link #PLATE_REACH}, {@link #MAX_SHAFT}.
 */
public class LiftBlockEntity extends CityBlockEntity {
	/** Blocks a second (the preview's ride: 5 blocks in 5 s, eased in and out). */
	public static final double SPEED = 1.0;
	/** How far round the shaft a call plate can be and still call it (provisional). */
	public static final int PLATE_REACH = 4;
	/** How far down a shaft can run (provisional). */
	public static final int MAX_SHAFT = 64;
	/** The deck's own thickness, a block's eighth (the preview's 2 px). */
	public static final double DECK = 2 / 16.0;
	/** The last 0.4 s of a ride up: the clamps catch (the preview's small settle, 0.6 px down and back). */
	public static final int SETTLE_TICKS = 8;
	public static final double SETTLE_DIP = 0.6 / 16.0;

	/** Where the deck's underside is when it stands still (world y; NaN until the drive first looks down its shaft). */
	private double restY = Double.NaN;
	/** The ride under way: from, to (world y), started at (game time; -1: standing), lasting (ticks). */
	private double fromY, toY;
	private long startTick = -1;
	private int duration;
	/** The level of the shaft's floor's top: its bottom landing (synced, for the renderer's rails). */
	private int bottom = Integer.MIN_VALUE;
	@Nullable
	private LiftDeck deck;
	/** The call plate whose call the deck is answering (its lens lit until the deck arrives, D62), or null. */
	@Nullable
	private BlockPos caller;

	public LiftBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.LIFT_BE.get(), pos, state);
	}

	public Direction facing() {
		return getBlockState().getValue(LiftBlock.FACING);
	}

	/** Its district woke: it works, and its seams are lit (D24). */
	public boolean isAwake() {
		return !getBlockState().getValue(LiftBlock.LOCKED);
	}

	public boolean isMoving() {
		return startTick >= 0;
	}

	public int bottom() {
		return bottom == Integer.MIN_VALUE ? worldPosition.getY() - 1 : bottom;
	}

	/** Where the deck's ride ends (its rest when standing). */
	public double target() {
		return isMoving() ? toY : restY();
	}

	private double restY() {
		return Double.isNaN(restY) ? bottom() : restY;
	}

	/** The shaft's footprint, world x and z (min corner, max corner), two blocks each way. */
	public AABB shaft() {
		double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
		for (BlockPos p : LiftBlock.footprint(worldPosition, facing())) {
			minX = Math.min(minX, p.getX());
			minZ = Math.min(minZ, p.getZ());
		}
		return new AABB(minX, bottom(), minZ, minX + 2, worldPosition.getY(), minZ + 2);
	}

	/**
	 * Where the deck's underside is at {@code time} (game time, fractions allowed for drawing): eased in and out at about
	 * {@link #SPEED} blocks a second, with the clamps' small settle at the end of a ride up (the preview's ride clip).
	 */
	public double deckY(double time) {
		if (startTick < 0)
			return restY();
		double t = time - startTick;
		if (t <= 0)
			return fromY;
		if (t >= duration)
			return toY;
		double u = t / duration, s = u * u * (3 - 2 * u);
		double y = fromY + (toY - fromY) * s;
		if (toY > fromY && duration >= SETTLE_TICKS * 2 && t > duration - SETTLE_TICKS)
			y -= SETTLE_DIP * Math.sin((t - (duration - SETTLE_TICKS)) / SETTLE_TICKS * Math.PI);
		return y;
	}

	// ---------- the shaft and its landings ----------

	/**
	 * Looks down the shaft for its floor: the first level under the deck (under the drive, before it has one) where any of
	 * the shaft's four columns is solid. (So a block put in the shaft above the deck is in its way, not a new floor.)
	 */
	private int findBottom() {
		if (level == null)
			return worldPosition.getY() - 1;
		BlockPos[] cells = LiftBlock.footprint(worldPosition, facing());
		int from = Double.isNaN(restY) ? worldPosition.getY() - 1 : Math.min(worldPosition.getY() - 1, (int) Math.floor(deckY(level.getGameTime()) + 1e-3) - 1);
		for (int y = from; y > worldPosition.getY() - MAX_SHAFT && y > level.getMinBuildHeight(); y--)
			for (BlockPos c : cells) {
				BlockPos p = new BlockPos(c.getX(), y, c.getZ());
				if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty())
					return y + 1;
			}
		return Math.max(level.getMinBuildHeight(), worldPosition.getY() - MAX_SHAFT);
	}

	/** Its landings, lowest first: the shaft's floor and the level of each call plate near the shaft. */
	public List<Integer> landings() {
		TreeSet<Integer> out = new TreeSet<>();
		int b = findBottom();
		if (b != bottom) {
			bottom = b;
			sync();
		}
		out.add(b);
		if (level != null) {
			AABB s = shaft();
			int x0 = (int) s.minX - PLATE_REACH, x1 = (int) s.maxX - 1 + PLATE_REACH, z0 = (int) s.minZ - PLATE_REACH, z1 = (int) s.maxZ - 1 + PLATE_REACH;
			BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
			for (int y = b; y < worldPosition.getY(); y++)
				for (int x = x0; x <= x1; x++)
					for (int z = z0; z <= z1; z++)
						if (level.getBlockState(p.set(x, y, z)).getBlock() instanceof LiftCallPlateBlock)
							out.add(y);
		}
		return new ArrayList<>(out);
	}

	/** Whether a call plate at {@code plate} belongs to this lift's shaft (near it, between its floor and its head). */
	public boolean serves(BlockPos plate) {
		AABB s = shaft();
		return plate.getY() >= bottom() && plate.getY() < worldPosition.getY()
			&& plate.getX() >= s.minX - PLATE_REACH && plate.getX() < s.maxX + PLATE_REACH
			&& plate.getZ() >= s.minZ - PLATE_REACH && plate.getZ() < s.maxZ + PLATE_REACH;
	}

	/** The lift whose shaft a call plate at {@code plate} serves: the nearest drive above it within reach. */
	@Nullable
	public static LiftBlockEntity serving(Level level, BlockPos plate) {
		int r = PLATE_REACH + 2;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int y = plate.getY() + 1; y <= plate.getY() + MAX_SHAFT && y < level.getMaxBuildHeight(); y++)
			for (int x = -r; x <= r; x++)
				for (int z = -r; z <= r; z++)
					if (level.getBlockState(p.set(plate.getX() + x, y, plate.getZ() + z)).getBlock() instanceof LiftBlock
						&& level.getBlockEntity(p) instanceof LiftBlockEntity lift) {
						lift.landings();
						if (lift.serves(plate))
							return lift;
					}
		return null;
	}

	// ---------- calling it ----------

	/** Only a player's hand works it (not a Deployer: provisional, waiting for the user). */
	private static boolean canWork(@Nullable Player player) {
		return player != null && !(player instanceof FakePlayer) && !player.isSpectator();
	}

	/**
	 * A player standing on the deck uses its control post: up to the next landing (sneaking: down), or the other way when
	 * there's none that way. Nothing while it's dark or already moving.
	 */
	public boolean usePost(Player player) {
		if (!canWork(player) || level == null || level.isClientSide)
			return false;
		if (!isAwake()) {
			player.displayClientMessage(Component.translatable("comedymonkeysaddons.wastes.lift_dark"), true);
			return false;
		}
		if (isMoving() || deck == null || !deck.carries(player))
			return false;
		double here = deckY(level.getGameTime());
		Integer up = null, down = null;
		for (int l : landings()) {
			if (l > here + 1e-3 && up == null)
				up = l;
			if (l < here - 1e-3)
				down = l;
		}
		Integer to = player.isShiftKeyDown() ? (down != null ? down : up) : (up != null ? up : down);
		return to != null && rideTo(to);
	}

	/** A player at a landing's call plate calls the deck to that landing. */
	public boolean call(Player player, int landing) {
		return call(player, landing, null);
	}

	/**
	 * The same from the call plate at {@code plate}: if the deck sets off, that plate's lens lights while the call waits
	 * and goes dark again when the deck arrives (or halts on its way, D62).
	 */
	public boolean call(Player player, int landing, @Nullable BlockPos plate) {
		if (!canWork(player) || level == null || level.isClientSide)
			return false;
		if (!isAwake()) {
			player.displayClientMessage(Component.translatable("comedymonkeysaddons.wastes.lift_dark"), true);
			return false;
		}
		if (isMoving() || !rideTo(landing))
			return false;
		if (plate != null && level.getBlockEntity(plate) instanceof LiftCallPlateBlockEntity be) {
			caller = plate.immutable();
			be.setWaiting(true);
		}
		return true;
	}

	/** The call (if any) is answered or given up: its plate goes dark. */
	private void answerCall() {
		if (caller != null && level != null && level.getBlockEntity(caller) instanceof LiftCallPlateBlockEntity be)
			be.setWaiting(false);
		caller = null;
	}

	/** Starts a ride to {@code landing} (world y of the deck's underside). Nothing if it's moving or already there. */
	public boolean rideTo(double landing) {
		if (level == null || level.isClientSide || isMoving() || !isAwake())
			return false;
		double here = restY();
		if (Math.abs(here - landing) < 1e-3)
			return false;
		fromY = here;
		toY = landing;
		startTick = level.getGameTime();
		duration = Math.max(10, (int) Math.round(Math.abs(landing - here) / SPEED * 20));
		// sound: the drum's clunk as the clamps let go, its hum while it rides
		sync();
		return true;
	}

	/** The deck met something in its way: it stops where it is (the next call starts from there). */
	void stopAt(double y) {
		if (!isMoving())
			return;
		restY = y;
		startTick = -1;
		answerCall();
		// sound: the clamps' bite
		sync();
	}

	// ---------- ticking ----------

	void tick() {
		if (level == null || level.isClientSide)
			return;
		if (bottom == Integer.MIN_VALUE || Double.isNaN(restY)) {
			bottom = findBottom();
			if (Double.isNaN(restY))
				restY = bottom;
			sync();
		}
		if (isMoving() && level.getGameTime() >= startTick + duration) {
			restY = toY;
			startTick = -1;
			answerCall();
			// sound: the clamps catch
			sync();
		}
		if (deck == null || deck.isRemoved() || deck.level() != level) {
			deck = null;
			AABB s = shaft();
			BlockPos at = BlockPos.containing(s.getCenter().x, deckY(level.getGameTime()), s.getCenter().z);
			if (level.isLoaded(at) && level instanceof ServerLevel server) {
				LiftDeck d = new LiftDeck(ModWastes.LIFT_DECK.get(), level);
				d.attach(this);
				d.moveTo(s.getCenter().x, deckY(level.getGameTime()), s.getCenter().z);
				if (server.addFreshEntity(d))
					deck = d;
			}
		}
	}

	@Nullable
	public LiftDeck deck() {
		return deck;
	}

	/** The deck it owns now (a deck finds its drive; a deck the drive doesn't own goes). */
	boolean owns(LiftDeck d) {
		return deck == d;
	}

	private void dropDeck() {
		if (deck != null && !deck.level().isClientSide)
			deck.discard();
		deck = null;
	}

	/** Broken: its deck goes with it. */
	void removed() {
		dropDeck();
	}

	@Override
	public void setRemoved() {
		dropDeck();
		super.setRemoved();
	}

	@Override
	public void onChunkUnloaded() {
		dropDeck();
		super.onChunkUnloaded();
	}

	/** Its district woke (or had woken while it was unloaded): it works from now on. */
	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		CityRecord record = record();
		if (record == null || !link.bound() || level == null || isAwake())
			return;
		if (record.awake(link.origin(), link.district())) {
			level.setBlock(worldPosition, getBlockState().setValue(LiftBlock.LOCKED, false), Block.UPDATE_ALL);
			// sound: the drum stirs (with the district's clunk)
			sync();
		}
	}

	// ---------- saving and syncing ----------

	@Override
	protected void writeState(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
		if (caller != null)
			tag.putLong("Caller", caller.asLong());
	}

	@Override
	protected void readState(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
		caller = tag.contains("Caller") ? BlockPos.of(tag.getLong("Caller")) : null;
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		if (!Double.isNaN(restY))
			tag.putDouble("DeckY", restY);
		tag.putInt("Bottom", bottom);
		if (startTick >= 0) {
			tag.putDouble("FromY", fromY);
			tag.putDouble("ToY", toY);
			tag.putLong("Start", startTick);
			tag.putInt("Duration", duration);
		}
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		restY = tag.contains("DeckY") ? tag.getDouble("DeckY") : Double.NaN;
		bottom = tag.contains("Bottom") ? tag.getInt("Bottom") : Integer.MIN_VALUE;
		if (tag.contains("Start")) {
			fromY = tag.getDouble("FromY");
			toY = tag.getDouble("ToY");
			startTick = tag.getLong("Start");
			duration = Math.max(1, tag.getInt("Duration"));
		} else
			startTick = -1;
	}

}
