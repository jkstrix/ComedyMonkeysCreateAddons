package com.comedymonkey.createaddons.content.wastes.bot;

import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

/**
 * How the bots hear their city (P8, P9): the {@link CityRecord} listener. A district waking stirs its loaded Scuttlers
 * and Sentries, each within 5 s; a district's feed changing folds or unfolds its Sentries; the heart waking wakes the
 * Custodian. Bots that aren't loaded catch up from the record when they load ({@link CityBot#catchUp}), so nothing
 * here loads a chunk.
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

	@Override
	public void districtWoke(ServerLevel level, BlockPos origin, District district) {
		for (CityBot bot : loaded(level, CityBot.class, origin, b -> !(b instanceof Custodian) && b.district() == district))
			bot.wakeIn(level.random.nextInt(CityBot.WAKE_SPREAD));
	}

	@Override
	public void fedChanged(ServerLevel level, BlockPos origin, District district, boolean fed) {
		for (Sentry sentry : loaded(level, Sentry.class, origin, s -> s.district() == district))
			sentry.setFed(fed);
	}

	@Override
	public void heartWoke(ServerLevel level, BlockPos origin) {
		for (Custodian custodian : loaded(level, Custodian.class, origin, c -> true))
			custodian.heartWoke();
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
