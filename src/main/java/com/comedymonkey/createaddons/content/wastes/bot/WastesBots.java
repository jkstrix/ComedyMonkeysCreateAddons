package com.comedymonkey.createaddons.content.wastes.bot;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.SubstationBlock;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

/**
 * How the bots hear their city (P8): the {@link CityRecord} listener. A district waking stirs its loaded Scuttlers and
 * Sentries, each within 5 s; thrown from its breaker, its pack runs to the breaker (D97). The Custodian doesn't wait on the record: it rises when a player walks into its hall once
 * all four are awake (D96, {@link Custodian}). Bots that aren't loaded catch up from the record when they load
 * ({@link CityBot#catchUp}), so nothing here loads a chunk.
 */
public final class WastesBots implements CityRecord.Listener {
	public static final WastesBots LISTENER = new WastesBots();

	private WastesBots() {}

	/** Once, from the mod's setup. */
	public static void register() {
		CityRecord.listen(LISTENER);
	}

	/** The loaded bots of one city that match. */
	public static <T extends CityBot> List<T> loaded(ServerLevel level, Class<T> kind, BlockPos origin, Predicate<T> also) {
		List<T> out = new java.util.ArrayList<>();
		for (T b : level.getEntities(EntityTypeTest.forClass(kind), b -> origin.equals(b.cityOrigin()) && also.test(b)))
			out.add(b);
		return out;
	}

	/**
	 * D97: thrown from its breaker, the clunk calls the district's pack: its Scuttlers stir together and run to where the
	 * player stands to throw it; a Sentry that can see that spot unfolds a beat after the clunk, as the lamps ripple on. The
	 * rest stir within 5 s as before; woken with no breaker (the heart's star), every bot wakes where it lies.
	 */
	@Override
	public void districtWoke(ServerLevel level, BlockPos origin, District district, @Nullable BlockPos breaker) {
		BlockPos stand = breaker == null ? null : breakerStand(level, breaker);
		for (CityBot bot : loaded(level, CityBot.class, origin, b -> !(b instanceof Custodian) && b.district() == district)) {
			if (stand != null && bot instanceof Scuttler s && !s.isFightScuttler()) {
				s.callTo(stand);
				s.wakeIn(level.random.nextInt(Scuttler.CALL_SPREAD));
			} else if (stand != null && bot instanceof Sentry s && s.inSight(standEye(stand)))
				s.wakeIn(Sentry.BEAT);
			else
				bot.wakeIn(level.random.nextInt(CityBot.WAKE_SPREAD));
		}
	}

	/**
	 * Where a player stands to throw the breaker at {@code breaker}: the block in front of its Substation (either half), or
	 * {@code breaker} itself if no Substation stands there (the dev command's caller).
	 */
	public static BlockPos breakerStand(ServerLevel level, BlockPos breaker) {
		BlockState s = level.getBlockState(breaker);
		if (!(s.getBlock() instanceof SubstationBlock))
			return breaker;
		BlockPos lower = s.getValue(SubstationBlock.HALF) == DoubleBlockHalf.UPPER ? breaker.below() : breaker;
		return lower.relative(s.getValue(SubstationBlock.FACING));
	}

	/** The middle of a player standing in {@code stand} (where a Sentry aims at them). */
	public static Vec3 standEye(BlockPos stand) {
		return Vec3.atBottomCenterOf(stand).add(0, 0.9, 0);
	}

	// ---------------------------------------------------------------- placing them (the city's builder, phase 4; the dev setup)
	// Each takes a ServerLevelAccessor so the city's world generation can place them into the chunk it is generating
	// (a WorldGenRegion) as well as a ServerLevel (the dev commands).

	/** A dormant Scuttler at {@code at} (a block's bottom centre, say), of a city's district: it unrolls when that district wakes. */
	@Nullable
	public static Scuttler placeScuttler(ServerLevelAccessor level, Vec3 at, BlockPos origin, District district) {
		Scuttler s = ModWastes.SCUTTLER.get().create(level.getLevel());
		if (s == null)
			return null;
		s.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360, 0);
		s.bind(origin, district);
		s.makeDormant();
		level.addFreshEntity(s);
		return s;
	}

	/** A dormant Sentry on {@code face} of the block at {@code wall} (UP: on its top, a floor; a side: on that wall). */
	@Nullable
	public static Sentry placeSentry(ServerLevelAccessor level, BlockPos wall, Direction face, BlockPos origin, District district) {
		Sentry s = ModWastes.SENTRY.get().create(level.getLevel());
		if (s == null)
			return null;
		s.mount(wall, face);
		s.bind(origin, district);
		s.makeDormant();
		level.addFreshEntity(s);
		return s;
	}

	/**
	 * The Custodian kneeling on its dais (standing in the block {@code dais}, facing {@code yaw}), bound to the city's
	 * heart, with its four pylons (bottom blocks) and the heart vault it kneels to when it dies. Pylons and vault may be
	 * left out: it then looks round its hall for ones bound to its city's heart when it wakes.
	 */
	@Nullable
	public static Custodian placeCustodian(ServerLevelAccessor level, BlockPos dais, float yaw, BlockPos origin, List<BlockPos> pylons, @Nullable BlockPos vault) {
		Custodian c = ModWastes.CUSTODIAN.get().create(level.getLevel());
		if (c == null)
			return null;
		c.moveTo(dais.getX() + 0.5, dais.getY(), dais.getZ() + 0.5, yaw, 0);
		c.bind(origin, District.HEART);
		c.setDais(dais, yaw);
		c.setPylons(pylons);
		c.setVault(vault);
		c.makeDormant();
		level.addFreshEntity(c);
		return c;
	}
}
