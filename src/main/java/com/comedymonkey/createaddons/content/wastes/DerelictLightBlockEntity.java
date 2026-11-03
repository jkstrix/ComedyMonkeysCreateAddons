package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A Derelict lamp's or cable's light (design/specs/clockwork-wastes.md, D16, P25, P52): lit once its district wakes
 * (D96), by the {@link CityRecord}'s word. It never ticks: a change in its city schedules one tick. A district coming on
 * ripples out over about a second from its Substation (lamps nearer it first, as the Electric Lights' street does, P8);
 * a district going dark (its Substation broken, P50) flickers each lamp for a second, then dark. After loading it catches
 * up at once. A street lamp's foot and pole, and a snapped head, never light; they keep their city all the same, as any
 * part can become the head.
 * <p>
 * Asleep, two looks lead a player in (D96): a lamp head near a dormant Substation sputters as a player passes
 * ({@link #beckon}), and the heart's spire seams flash up to the crown when another district wakes ({@link #pulse}).
 */
public class DerelictLightBlockEntity extends CityBlockEntity {
	/** Ticks a flicker lasts, going out (as the Electric Lights'). */
	public static final int FLICKER_TICKS = 20;
	/** Ticks a heart lamp surges and flickers when the Custodian overloads (D54), before it settles. */
	public static final int SURGE_TICKS = 60;
	/** Ticks a dark lamp sputters when it beckons (D96): a going-out flicker. */
	public static final int BECKON_TICKS = 20;
	/** The spire's pulse (D96): it climbs from the roof to the crown in this many ticks, each seam lit for {@link #PULSE_TICKS}. */
	public static final int PULSE_CLIMB_TICKS = 40, PULSE_TICKS = 12;

	public enum Phase {
		DARK, LIT, FADING
	}

	/** Client hook: it went between dark and lit (its Flywheel visual is made again: only a lit lamp has one). */
	public static java.util.function.Consumer<DerelictLightBlockEntity> clientLit = be -> {};

	private Phase phase = Phase.DARK;
	private long flickerAt = SubstationBlockEntity.NEVER;
	/** The tick it comes on (the ripple's wait); MAX while not waiting. */
	private long onAt = Long.MAX_VALUE;
	/** When the heart last surged (the Custodian's overload, D54); NEVER if not. */
	private long surgeAt = SubstationBlockEntity.NEVER;
	/** When it last sputtered for a passing player (D96); NEVER if not. */
	private long beckonAt = SubstationBlockEntity.NEVER;
	/** The tick its spire pulse lights it (D96); MAX if none is coming. Not saved: a pulse is a moment's look. */
	private long pulseAt = Long.MAX_VALUE;

	public DerelictLightBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.DERELICT_LIGHT_BE.get(), pos, state);
	}

	public Phase phase() {
		return phase;
	}

	public long flickerAt() {
		return flickerAt;
	}

	public long surgeAt() {
		return surgeAt;
	}

	public long beckonAt() {
		return beckonAt;
	}

	/** It's surging now: the heart's overload (D54), for {@link #SURGE_TICKS} from {@link #surgeAt}. */
	public boolean surging(long now) {
		return now >= surgeAt && now - surgeAt < SURGE_TICKS;
	}

	/** It's sputtering for a passing player now, or about to (D96). */
	public boolean beckoning(long now) {
		return beckonAt != SubstationBlockEntity.NEVER && now < beckonAt + BECKON_TICKS;
	}

	/**
	 * The Custodian overloaded (P36, D54): a heart lamp, lit or dark, surges and flickers for about 3 s, then settles back
	 * as it was. A look only: its light and its phase don't change.
	 */
	@Override
	public void surged() {
		if (level == null || level.isClientSide || !lightable())
			return;
		surgeAt = level.getGameTime();
		sync();
	}

	/**
	 * D96: a dark lamp head near a dormant Substation sputters for a second, {@code delay} ticks from now, as a player
	 * passes (the Substation picks the lamps and their order). A look only.
	 */
	public void beckon(int delay) {
		if (level == null || level.isClientSide || !isLamp() || phase != Phase.DARK)
			return;
		beckonAt = level.getGameTime() + Math.max(0, delay);
		sync();
	}

	/**
	 * D96: another district woke: a dark spire seam flashes on for a moment as the pulse climbs past it, roof to crown
	 * over {@link #PULSE_CLIMB_TICKS}. Nothing else lights.
	 */
	public void pulse() {
		if (level == null || level.isClientSide || phase != Phase.DARK || wanted())
			return;
		int wait = spireWait(PULSE_CLIMB_TICKS);
		if (wait < 0)
			return;
		pulseAt = level.getGameTime() + wait;
		scheduleAfter(wait);
	}

	/** Its block can light at all (a street lamp's whole head, a wall lamp, a cable). */
	public boolean lightable() {
		return getBlockState().getBlock() instanceof DerelictLight light && light.lightable(getBlockState());
	}

	/** A lamp with a lens (a street lamp's head, a wall lamp), not a cable or a lit building block. */
	public boolean isLamp() {
		BlockState state = getBlockState();
		return state.getBlock() instanceof DerelictStreetLampBlock && state.getValue(DerelictStreetLampBlock.PART) == DerelictStreetLampBlock.Part.HEAD
			|| state.getBlock() instanceof DerelictWallLampBlock;
	}

	private boolean wanted() {
		CityRecord record = record();
		return level != null && record != null && link.bound() && lightable() && record.lit(link.origin(), link.district(), level.getGameTime());
	}

	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		if (level == null)
			return;
		long now = level.getGameTime();
		boolean want = wanted();
		// the spire's pulse: on as it passes, then dark again (unless the heart comes on meanwhile)
		if (pulseAt != Long.MAX_VALUE) {
			if (want)
				pulseAt = Long.MAX_VALUE;
			else if (now < pulseAt) {
				scheduleAfter((int) (pulseAt - now));
				return;
			} else if (now < pulseAt + PULSE_TICKS) {
				show(true);
				scheduleAfter((int) (pulseAt + PULSE_TICKS - now));
				return;
			} else
				pulseAt = Long.MAX_VALUE;
		}
		if (want) {
			if (phase == Phase.LIT)
				return;
			if (!live) {
				// catching up after loading, or the ripple's wait is over
				if (onAt == Long.MAX_VALUE || now >= onAt)
					light(true);
				else
					schedule((int) (onAt - now));
				return;
			}
			// the ripple: the farther from where the power came in, the later (about a second across a street)
			if (onAt == Long.MAX_VALUE) {
				int wait = from == null ? 1 + level.random.nextInt(20)
					: 1 + Math.min(18, (int) (Math.sqrt(from.distSqr(worldPosition)) / 3)) + level.random.nextInt(3);
				int climb = spireWait(SPIRE_CLIMB_TICKS);
				if (climb >= 0)
					wait = climb;
				onAt = now + wait;
			}
			if (now >= onAt)
				light(true);
			else
				scheduleAfter((int) (onAt - now));
			return;
		}
		onAt = Long.MAX_VALUE;
		switch (phase) {
			case LIT -> {
				if (!live) {
					light(false);
					return;
				}
				// its grid cut: a second's flicker, then dark
				phase = Phase.FADING;
				flickerAt = now;
				scheduleAfter(FLICKER_TICKS);
				sync();
			}
			case FADING -> {
				if (now >= flickerAt + FLICKER_TICKS)
					light(false);
				else
					scheduleAfter((int) (flickerAt + FLICKER_TICKS - now));
			}
			case DARK -> show(false);
		}
	}

	/** Ticks the heart's spire takes to light from its foot to its crown when the heart comes on (world brief: 4 s). */
	public static final int SPIRE_CLIMB_TICKS = 80;

	/**
	 * A heart light on the spire (above the drum's roof): the crown's seams at once, the spire's corner seams climbing from
	 * the bottom to the top over {@code climb} ticks (world brief, Interesting Features). -1 for any other light (it
	 * ripples as usual).
	 */
	private int spireWait(int climb) {
		BlockPos o = link.origin();
		if (o == null || link.district() != District.HEART)
			return -1;
		int dy = worldPosition.getY() - o.getY(), roof = com.comedymonkey.createaddons.content.wastes.city.CityLayout.SPIRE_ROOF,
			crown = com.comedymonkey.createaddons.content.wastes.city.CityLayout.CROWN_Y;
		if (dy < roof)
			return -1;
		if (dy >= crown)
			return 1;
		return 1 + (dy - roof) * climb / (crown - roof);
	}

	/** A later look at itself, as a change would (so it acts as a live change, not a catch-up). */
	private void scheduleAfter(int delay) {
		live = true;
		schedule(delay);
	}

	private void light(boolean on) {
		phase = on ? Phase.LIT : Phase.DARK;
		onAt = Long.MAX_VALUE;
		show(on);
		sync();
	}

	/** Lit or dark in its block state: the light it casts, the cable's lit line. */
	private void show(boolean lit) {
		if (level == null || isRemoved())
			return;
		BlockState state = level.getBlockState(worldPosition);
		if (state.getBlock() == getBlockState().getBlock() && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != lit)
			level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		tag.putInt("Phase", phase.ordinal());
		tag.putLong("FlickerAt", flickerAt);
		tag.putLong("SurgeAt", surgeAt);
		tag.putLong("BeckonAt", beckonAt);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		long now = level == null ? 0 : level.getGameTime();
		boolean wasShown = shown(now);
		phase = Phase.values()[Math.floorMod(tag.getInt("Phase"), Phase.values().length)];
		flickerAt = tag.contains("FlickerAt") ? tag.getLong("FlickerAt") : SubstationBlockEntity.NEVER;
		surgeAt = tag.contains("SurgeAt") ? tag.getLong("SurgeAt") : SubstationBlockEntity.NEVER;
		beckonAt = tag.contains("BeckonAt") ? tag.getLong("BeckonAt") : SubstationBlockEntity.NEVER;
		// a dark lamp has no visual: one is made for a surge or a sputter as for lighting
		if (level != null && level.isClientSide && wasShown != shown(now))
			clientLit.accept(this);
	}

	/** It has something to draw now or soon: lit, fading, surging or sputtering. */
	public boolean shown(long now) {
		return phase != Phase.DARK || surging(now) || beckoning(now);
	}

	/** A Derelict lamp or cable block: whether a block of it in this state lights at all. */
	public interface DerelictLight {
		boolean lightable(BlockState state);
	}
}
