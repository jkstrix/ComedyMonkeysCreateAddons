package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.CityLink;
import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlock;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlockEntity;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.ThreePartBlock;
import com.comedymonkey.createaddons.content.wastes.WallVentBlock;
import com.comedymonkey.createaddons.content.wastes.WallVentBlockEntity;
import com.comedymonkey.createaddons.content.wastes.bot.CityBot;
import com.comedymonkey.createaddons.content.wastes.bot.Custodian;
import com.comedymonkey.createaddons.content.wastes.bot.Scuttler;
import com.comedymonkey.createaddons.content.wastes.bot.Sentry;
import com.comedymonkey.createaddons.content.wastes.bot.WastesBots;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Clockwork Wastes' bots, phase 3 (design/specs/clockwork-wastes.md, P8, P9, P22, P31-P41): dormant bots wake when
 * their district does (and not another's), and catch up if they load after it woke; on Peaceful they never wake; a
 * Sentry folds and stops firing while its district is unfed and fires again when it's fed; the Custodian takes no damage
 * while a pylon stands and mends 2 a second for each, takes it once all four are down, calls its Scuttlers, overloads
 * once at half health re-lighting exactly one pylon, resets with every pylon re-lit and the vault still locked, opens
 * the heart vault on any death (/kill too) but drops its loot only for a player's kill; fight Scuttlers drop nothing.
 * Each test is its own city (its origin is its own corner), so they don't share a record.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesBotGameTests {
	private static final String COURSE = "trade_bot_course";

	private WastesBotGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(WastesBotGameTests.class);
	}

	// ---------------------------------------------------------------- helpers

	/** The course cleared above its floor: nothing in a Sentry's sight or the Custodian's hall. */
	private static void clear(GameTestHelper helper) {
		for (BlockPos p : BlockPos.betweenClosed(0, 1, 0, 23, 7, 23))
			helper.setBlock(p, net.minecraft.world.level.block.Blocks.AIR);
	}

	private static BlockPos origin(GameTestHelper helper) {
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		CityRecord.of(helper.getLevel()).add(o);
		return o;
	}

	private static CityRecord record(GameTestHelper helper) {
		return CityRecord.of(helper.getLevel());
	}

	private static Scuttler scuttler(GameTestHelper helper, BlockPos pos, District district) {
		return WastesBots.placeScuttler(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(pos)), origin(helper), district);
	}

	/** A Sentry on the floor in {@code pos}. */
	private static Sentry sentry(GameTestHelper helper, BlockPos pos, District district) {
		return WastesBots.placeSentry(helper.getLevel(), helper.absolutePos(pos.below()), Direction.UP, origin(helper), district);
	}

	private static BlockPos pylon(GameTestHelper helper, BlockPos pos) {
		BlockState state = ModWastes.CONDUIT_PYLON.get().defaultBlockState();
		helper.setBlock(pos, state);
		helper.setBlock(pos.above(), state.setValue(ThreePartBlock.PART, ThreePartBlock.Part.MIDDLE));
		helper.setBlock(pos.above(2), state.setValue(ThreePartBlock.PART, ThreePartBlock.Part.TOP));
		CityLink.bind(helper.getLevel(), helper.absolutePos(pos), origin(helper), District.HEART);
		return helper.absolutePos(pos);
	}

	private static final BlockPos[] CORNERS = { new BlockPos(3, 1, 3), new BlockPos(19, 1, 3), new BlockPos(3, 1, 19), new BlockPos(19, 1, 19) };
	private static final BlockPos DAIS = new BlockPos(11, 1, 11), VAULT = new BlockPos(11, 1, 5);

	/** The heart: four pylons in the corners, the heart vault, and the Custodian kneeling between them. */
	private static Custodian hall(GameTestHelper helper, boolean pylons) {
		clear(helper);
		List<BlockPos> list = new ArrayList<>();
		if (pylons)
			for (BlockPos c : CORNERS)
				list.add(pylon(helper, c));
		helper.setBlock(VAULT, ModWastes.CITY_VAULT.get().defaultBlockState().setValue(CityVaultBlock.HEART, true));
		CityLink.bind(helper.getLevel(), helper.absolutePos(VAULT), origin(helper), District.HEART);
		return WastesBots.placeCustodian(helper.getLevel(), helper.absolutePos(DAIS), 0, origin(helper), list, helper.absolutePos(VAULT));
	}

	private static Player player(GameTestHelper helper) {
		return helper.makeMockPlayer(GameType.SURVIVAL);
	}

	/** Hits every pylon in the hall once a round, 10 ticks apart, from {@code start}, for {@code rounds} rounds. */
	private static void hitPylons(GameTestHelper helper, Player player, int start, int rounds) {
		for (int r = 0; r < rounds; r++)
			helper.runAtTickTime(start + r * 10, () -> {
				for (BlockPos c : CORNERS) {
					ConduitPylonBlockEntity be = (ConduitPylonBlockEntity) helper.getBlockEntity(c);
					be.hit(player);
				}
			});
	}

	private static int standing(GameTestHelper helper) {
		int n = 0;
		for (BlockPos c : CORNERS)
			if (((ConduitPylonBlockEntity) helper.getBlockEntity(c)).isStanding())
				n++;
		return n;
	}

	private static float hit(Custodian c, Player player, float amount) {
		float before = c.getHealth();
		c.invulnerableTime = 0;
		c.hurt(c.damageSources().playerAttack(player), amount);
		return before - c.getHealth();
	}

	private static List<? extends Scuttler> fightScuttlers(GameTestHelper helper, Custodian c) {
		return helper.getLevel().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Scuttler.class),
			s -> s.isFightScuttler() && c.getUUID().equals(s.custodian()));
	}

	// ---------------------------------------------------------------- waking (P8, P22, P48)

	/** A district waking stirs its own bots within 5 s (each at its own moment), not another district's. */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void dormantBotsWakeWhenTheirDistrictWakes(GameTestHelper helper) {
		Scuttler foundry = scuttler(helper, new BlockPos(4, 1, 4), District.FOUNDRY);
		Scuttler workshops = scuttler(helper, new BlockPos(8, 1, 4), District.WORKSHOPS);
		Sentry sentry = sentry(helper, new BlockPos(12, 1, 4), District.FOUNDRY);
		helper.runAtTickTime(2, () -> {
			helper.assertTrue(foundry.dormant() && workshops.dormant() && sentry.dormant(), "bots should start asleep");
			record(helper).wake(helper.getLevel(), origin(helper), District.FOUNDRY, null);
			long now = helper.getLevel().getGameTime();
			helper.assertTrue(foundry.wakeAt() >= now && foundry.wakeAt() < now + 100, "the Foundry's Scuttler should stir within 5 s");
			helper.assertTrue(sentry.wakeAt() >= now && sentry.wakeAt() < now + 100, "the Foundry's Sentry should stir within 5 s");
			helper.assertTrue(workshops.wakeAt() < 0, "the Workshops' Scuttler shouldn't stir");
		});
		helper.runAtTickTime(110, () -> {
			helper.assertFalse(foundry.dormant(), "the Foundry's Scuttler is still asleep after 5 s");
			helper.assertFalse(sentry.dormant(), "the Foundry's Sentry is still asleep after 5 s");
			helper.assertTrue(workshops.dormant(), "the Workshops' Scuttler woke with the Foundry");
			// woken but unfed (no Substation pays its upkeep): the Sentry folds at once (P9)
			helper.assertTrue(sentry.folded(), "an unfed district's Sentry should be folded");
			helper.succeed();
		});
	}

	/** A Scuttler that loads after its district woke wakes within 5 s (as the city's blocks catch up, P48). */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void aBotLoadedAfterItsDistrictWokeCatchesUp(GameTestHelper helper) {
		Scuttler first = scuttler(helper, new BlockPos(4, 1, 4), District.RAIL_YARD);
		CompoundTag saved = new CompoundTag();
		helper.runAtTickTime(2, () -> {
			first.saveWithoutId(saved);
			first.discard();
			record(helper).wake(helper.getLevel(), origin(helper), District.RAIL_YARD, null);
		});
		Scuttler[] loaded = new Scuttler[1];
		helper.runAtTickTime(5, () -> {
			Scuttler s = ModWastes.SCUTTLER.get().create(helper.getLevel());
			s.load(saved);
			s.setUUID(java.util.UUID.randomUUID());
			helper.getLevel().addFreshEntity(s);
			helper.assertTrue(s.dormant(), "it should load asleep");
			loaded[0] = s;
		});
		helper.runAtTickTime(110, () -> {
			helper.assertFalse(loaded[0].dormant(), "loaded after its district woke, it should have woken");
			helper.succeed();
		});
	}

	/** P22, P41: on Peaceful no bot wakes, not even the Custodian when the heart does. */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void onPeacefulBotsNeverWake(GameTestHelper helper) {
		BlockPos o = origin(helper);
		CityRecord.PEACEFUL_FOR_TESTS.add(o);
		Scuttler scuttler = scuttler(helper, new BlockPos(4, 1, 4), District.QUARTERS);
		Sentry sentry = sentry(helper, new BlockPos(6, 1, 4), District.QUARTERS);
		Custodian custodian = hall(helper, false);
		helper.runAtTickTime(2, () -> record(helper).wakeAll(helper.getLevel(), o, null));
		helper.runAtTickTime(120, () -> {
			try {
				helper.assertTrue(scuttler.dormant(), "a Scuttler woke on Peaceful");
				helper.assertTrue(sentry.dormant(), "a Sentry woke on Peaceful");
				helper.assertTrue(custodian.dormant() && !custodian.hasWoken(), "the Custodian woke on Peaceful");
				helper.assertFalse(record(helper).custodianAwake(o), "the record says the Custodian woke on Peaceful");
			} finally {
				CityRecord.PEACEFUL_FOR_TESTS.remove(o);
			}
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the Sentry (P9, P19)

	/** Unfed it folds and doesn't fire at a player in plain sight; fed it opens and fires; unfed again it stops. */
	@GameTest(template = COURSE, timeoutTicks = 460)
	public static void sentryFoldsWhileUnfedAndFiresWhenFed(GameTestHelper helper) {
		clear(helper);
		Sentry sentry = sentry(helper, new BlockPos(5, 1, 5), District.FOUNDRY);
		Player target = player(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 14)));
		target.setPos(at.x, at.y, at.z);
		helper.runAtTickTime(2, () -> {
			record(helper).wake(helper.getLevel(), origin(helper), District.FOUNDRY, null);
			sentry.wake(); // now, not within 5 s
		});
		helper.runAtTickTime(10, () -> sentry.setTarget(target));
		helper.runAtTickTime(120, () -> {
			helper.assertTrue(sentry.folded(), "the district is unfed: it should be folded");
			helper.assertTrue(sentry.shots() == 0, "it fired " + sentry.shots() + " bolts folded");
			record(helper).setFed(helper.getLevel(), origin(helper), District.FOUNDRY, true, null);
			helper.assertFalse(sentry.folded(), "fed, it should open");
		});
		helper.runAtTickTime(125, () -> sentry.setTarget(target));
		int[] firedFed = new int[1];
		helper.runAtTickTime(300, () -> {
			helper.assertTrue(sentry.shots() >= 2, "fed, with a player in sight, it fired " + sentry.shots() + " bolts in 9 s");
			record(helper).setFed(helper.getLevel(), origin(helper), District.FOUNDRY, false, null);
			helper.assertTrue(sentry.folded(), "unfed again, it should fold");
			firedFed[0] = sentry.shots();
		});
		helper.runAtTickTime(440, () -> {
			helper.assertTrue(sentry.shots() == firedFed[0], "it kept firing folded");
			helper.assertTrue(sentry.position().equals(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 5)))), "it moved off its mount");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the Custodian (P33-P40)

	/** Shielded it takes nothing and mends 2 a second for each pylon; all four down, the shield drops and hits land. */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void custodianIsShieldedWhileAPylonStands(GameTestHelper helper) {
		Custodian c = hall(helper, true);
		Player player = player(helper);
		helper.runAtTickTime(1, c::forceFight);
		helper.runAtTickTime(4, () -> {
			helper.assertTrue(c.shielded(), "four pylons stand: it should be shielded");
			helper.assertTrue(hit(c, player, 50) == 0, "a hit landed through the shield");
			c.setHealth(300);
		});
		// 30 ticks: one or two of its once-a-second mends, 8 each
		helper.runAtTickTime(34, () -> {
			float mended = c.getHealth() - 300;
			helper.assertTrue(Math.abs(mended - 8) < 0.01f || Math.abs(mended - 16) < 0.01f, "four pylons should mend 8 a second; mended " + mended + " in 1.5 s");
		});
		hitPylons(helper, player, 40, ConduitPylonBlockEntity.maxHits() - 1);
		helper.runAtTickTime(40 + (ConduitPylonBlockEntity.maxHits() - 1) * 10 + 5, () -> {
			helper.assertTrue(standing(helper) == 4, "a pylon broke early");
			helper.assertTrue(hit(c, player, 50) == 0, "a hit landed with all four pylons cracked but standing");
			// three go down: still shielded by the fourth
			for (int k = 0; k < 3; k++)
				((ConduitPylonBlockEntity) helper.getBlockEntity(CORNERS[k])).hit(player);
		});
		helper.runAtTickTime(40 + ConduitPylonBlockEntity.maxHits() * 10, () -> {
			helper.assertTrue(standing(helper) == 1, "three pylons should be down, " + standing(helper) + " stand");
			helper.assertTrue(c.shielded() && hit(c, player, 50) == 0, "one pylon still stands: no damage");
			((ConduitPylonBlockEntity) helper.getBlockEntity(CORNERS[3])).hit(player);
		});
		helper.runAtTickTime(40 + ConduitPylonBlockEntity.maxHits() * 10 + 5, () -> {
			helper.assertFalse(c.shielded(), "all four down: the shield should drop");
			c.setHealth(380);
			float dealt = hit(c, player, 10);
			helper.assertTrue(Math.abs(dealt - 10) < 0.01f, "unshielded, a 10 hit took " + dealt);
		});
		// P35: two Scuttlers for each broken pylon, eight in all, called after its wrath (2 s) at its call's 0.8 s
		helper.runAtTickTime(40 + ConduitPylonBlockEntity.maxHits() * 10 + 70, () -> {
			helper.assertTrue(c.scuttlersCalled() == 8, "it called " + c.scuttlersCalled() + " Scuttlers for four pylons");
			helper.assertTrue(fightScuttlers(helper, c).size() == 8, "eight fight Scuttlers should be out");
			helper.succeed();
		});
	}

	/** At half health it overloads once: exactly one pylon re-lights; broken again and hit lower, no second re-light. */
	@GameTest(template = COURSE, timeoutTicks = 240)
	public static void overloadRelightsExactlyOnePylonOnce(GameTestHelper helper) {
		Custodian c = hall(helper, true);
		Player player = player(helper);
		helper.runAtTickTime(1, c::forceFight);
		hitPylons(helper, player, 5, ConduitPylonBlockEntity.maxHits());
		helper.runAtTickTime(5 + ConduitPylonBlockEntity.maxHits() * 10, () -> {
			helper.assertTrue(standing(helper) == 0 && !c.shielded(), "the pylons should all be down");
			c.setHealth(210);
			hit(c, player, 20);
			helper.assertTrue(c.overloaded(), "it should overload at half health");
			helper.assertTrue(standing(helper) == 1, "the overload should re-light exactly one pylon, " + standing(helper) + " stand");
		});
		helper.runAtTickTime(5 + ConduitPylonBlockEntity.maxHits() * 10 + 2, () -> helper.assertTrue(c.shielded(), "re-lit, the shield is back"));
		// the re-lit one needs the full six hits again (D43)
		hitPylons(helper, player, 5 + ConduitPylonBlockEntity.maxHits() * 10 + 10, ConduitPylonBlockEntity.maxHits());
		helper.runAtTickTime(5 + ConduitPylonBlockEntity.maxHits() * 10 + 10 + ConduitPylonBlockEntity.maxHits() * 10 + 5, () -> {
			helper.assertTrue(standing(helper) == 0 && !c.shielded(), "the re-lit pylon should be down again");
			c.setHealth(150);
			hit(c, player, 10);
			helper.assertTrue(standing(helper) == 0, "it overloaded twice: a pylon re-lit again");
			helper.succeed();
		});
	}

	/** Reset (P39): it kneels on its dais, every pylon re-lights, it mends fully, its Scuttlers crumble, the vault stays shut. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void resetRelightsEveryPylonAndKeepsTheVaultLocked(GameTestHelper helper) {
		Custodian c = hall(helper, true);
		Player player = player(helper);
		BlockPos o = origin(helper);
		helper.runAtTickTime(1, c::forceFight);
		hitPylons(helper, player, 5, ConduitPylonBlockEntity.maxHits());
		helper.runAtTickTime(5 + ConduitPylonBlockEntity.maxHits() * 10 + 65, () -> {
			helper.assertTrue(standing(helper) == 0, "the pylons should all be down");
			helper.assertTrue(fightScuttlers(helper, c).size() == 8, "its Scuttlers should be out");
			c.setHealth(150);
			c.beginReset();
		});
		helper.runAtTickTime(5 + ConduitPylonBlockEntity.maxHits() * 10 + 165, () -> {
			helper.assertTrue(c.dormant(), "it should be kneeling asleep again, is " + c.phase());
			helper.assertTrue(standing(helper) == 4, "every pylon should re-light, " + standing(helper) + " stand");
			helper.assertTrue(c.getHealth() == c.getMaxHealth(), "it should mend fully, has " + c.getHealth());
			helper.assertTrue(fightScuttlers(helper, c).isEmpty(), "its Scuttlers should crumble");
			helper.assertFalse(record(helper).custodianAwake(o), "the record should say it's asleep");
			helper.assertFalse(record(helper).custodianDead(o), "the record says it died");
			helper.assertFalse(record(helper).vaultOpen(helper.getLevel(), o, District.HEART), "the heart vault opened on a reset");
			helper.assertTrue(helper.getBlockState(VAULT).getValue(CityVaultBlock.LOCKED), "the heart vault unlocked on a reset");
			helper.succeed();
		});
	}

	/** D35: any death, /kill too, opens the heart vault; /kill drops nothing (P40). */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void itsDeathOpensTheHeartVaultEvenByKill(GameTestHelper helper) {
		Custodian c = hall(helper, true);
		BlockPos o = origin(helper);
		helper.runAtTickTime(1, c::forceFight);
		helper.runAtTickTime(5, () -> {
			helper.assertFalse(record(helper).vaultOpen(helper.getLevel(), o, District.HEART), "the heart vault is open with it alive");
			c.kill();
		});
		helper.runAtTickTime(20, () -> {
			helper.assertTrue(c.isRemoved(), "/kill should remove it");
			helper.assertTrue(record(helper).custodianDead(o), "its death isn't in the record");
			helper.assertTrue(record(helper).vaultOpen(helper.getLevel(), o, District.HEART), "the heart vault should open on its death");
			helper.assertFalse(helper.getBlockState(VAULT).getValue(CityVaultBlock.LOCKED), "the heart vault is still locked");
			AABB around = new AABB(helper.absolutePos(DAIS)).inflate(6);
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "/kill dropped items");
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, around).isEmpty(), "/kill dropped experience");
			helper.succeed();
		});
	}

	/** A player's kill: its death clip plays out, then one Unknown Device and its experience drop (P40). */
	@GameTest(template = COURSE, timeoutTicks = 220)
	public static void aPlayersKillDropsItsLoot(GameTestHelper helper) {
		Custodian c = hall(helper, false);
		Player player = player(helper);
		BlockPos o = origin(helper);
		helper.runAtTickTime(1, c::forceFight);
		helper.runAtTickTime(5, () -> {
			helper.assertFalse(c.shielded(), "no pylons: it shouldn't be shielded");
			hit(c, player, 10000);
			helper.assertTrue(c.isAlive() && c.phase() == Custodian.Phase.DYING, "it should be playing its death, is " + c.phase());
		});
		helper.runAtTickTime(60, () -> helper.assertFalse(record(helper).custodianDead(o), "the vault opened before its death clip ended"));
		helper.runAtTickTime(200, () -> {
			helper.assertTrue(c.isRemoved(), "it should be gone after its death clip");
			helper.assertTrue(record(helper).custodianDead(o), "its death isn't in the record");
			AABB around = new AABB(helper.absolutePos(DAIS)).inflate(6);
			int devices = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(ModWastes.UNKNOWN_DEVICE.get()))
				.stream().mapToInt(e -> e.getItem().getCount()).sum();
			helper.assertTrue(devices == 1, "a player's kill should drop one Unknown Device, dropped " + devices);
			// D53: and 4-8 scrap
			int scrap = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(ModWastes.CLOCKWORK_SCRAP.get()))
				.stream().mapToInt(e -> e.getItem().getCount()).sum();
			helper.assertTrue(scrap >= 4 && scrap <= 8, "a player's kill should drop 4-8 scrap, dropped " + scrap);
			helper.assertFalse(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, around).isEmpty(), "a player's kill should drop experience");
			helper.succeed();
		});
	}

	/** P35: a fight Scuttler killed by a player drops nothing; the rest crumble when the Custodian dies. */
	@GameTest(template = COURSE, timeoutTicks = 80)
	public static void fightScuttlersDropNothingAndCrumble(GameTestHelper helper) {
		Custodian c = hall(helper, false);
		Player player = player(helper);
		List<Scuttler> fight = new ArrayList<>();
		helper.runAtTickTime(1, () -> {
			c.forceFight();
			for (int i = 0; i < 2; i++) {
				Scuttler s = helper.spawn(ModWastes.SCUTTLER.get(), new BlockPos(6 + i * 4, 1, 16));
				s.makeFightScuttler(c);
				fight.add(s);
			}
			// and an ordinary one, for comparison: it drops by its loot table
			helper.spawn(ModWastes.SCUTTLER.get(), new BlockPos(16, 1, 16));
		});
		helper.runAtTickTime(5, () -> {
			Scuttler s = fight.get(0);
			s.hurt(s.damageSources().playerAttack(player), 100);
			helper.assertTrue(s.isDeadOrDying(), "the fight Scuttler should be dead");
		});
		helper.runAtTickTime(30, () -> {
			AABB around = new AABB(helper.absolutePos(new BlockPos(6, 1, 16))).inflate(2);
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "a fight Scuttler dropped items");
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, around).isEmpty(), "a fight Scuttler dropped experience");
			c.kill();
		});
		helper.runAtTickTime(40, () -> {
			helper.assertTrue(fight.get(1).isRemoved(), "the other fight Scuttler should crumble with the Custodian");
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(10, 1, 16))).inflate(2)).isEmpty(),
				"a crumbled Scuttler dropped items");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the wall vents (D57, D59, D60)

	/** Two wall vents by each corner pylon, in the walls either side of it (the heart-hall preview's), facing into the hall. */
	private static final BlockPos[][] VENTS = {
		{ new BlockPos(1, 1, 5), new BlockPos(5, 1, 1) }, { new BlockPos(21, 1, 5), new BlockPos(17, 1, 1) },
		{ new BlockPos(1, 1, 17), new BlockPos(5, 1, 21) }, { new BlockPos(21, 1, 17), new BlockPos(17, 1, 21) } };

	private static Direction ventFacing(BlockPos v) {
		return v.getX() == 1 ? Direction.EAST : v.getX() == 21 ? Direction.WEST : v.getZ() == 1 ? Direction.SOUTH : Direction.NORTH;
	}

	/** The eight vents, bound to the hall's city's heart. */
	private static void vents(GameTestHelper helper) {
		for (BlockPos[] pair : VENTS)
			for (BlockPos v : pair) {
				helper.setBlock(v, ModWastes.WALL_VENT.get().defaultBlockState().setValue(WallVentBlock.FACING, ventFacing(v)));
				CityLink.bind(helper.getLevel(), helper.absolutePos(v), origin(helper), District.HEART);
			}
	}

	private static WallVentBlockEntity vent(GameTestHelper helper, BlockPos v) {
		return (WallVentBlockEntity) helper.getBlockEntity(v);
	}

	/** Where a Scuttler out of the vent at {@code v} lands: 1.25 blocks out from its face. */
	private static Vec3 landing(GameTestHelper helper, BlockPos v) {
		Direction f = ventFacing(v);
		return Vec3.atBottomCenterOf(helper.absolutePos(v)).add(f.getStepX() * (0.5 + Custodian.VENT_LANDING), 0, f.getStepZ() * (0.5 + Custodian.VENT_LANDING));
	}

	/** Breaks one pylon with six counted hits (one every 10 ticks) from {@code start}. */
	private static void breakPylon(GameTestHelper helper, Player player, BlockPos pylon, int start) {
		for (int r = 0; r < ConduitPylonBlockEntity.maxHits(); r++)
			helper.runAtTickTime(start + r * 10, () -> ((ConduitPylonBlockEntity) helper.getBlockEntity(pylon)).hit(player));
	}

	/**
	 * A vent is sealed while the heart sleeps and live once all four districts wake; it bursts once, is left open for good,
	 * and never bursts again (D57, D60). One bound to no city stays sealed.
	 */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aWallVentGoesLiveWithTheHeartAndBurstsOnce(GameTestHelper helper) {
		clear(helper);
		vents(helper);
		BlockPos loose = new BlockPos(11, 1, 11);
		helper.setBlock(loose, ModWastes.WALL_VENT.get().defaultBlockState());
		BlockPos v = VENTS[0][0];
		helper.runAtTickTime(3, () -> {
			helper.assertTrue(vent(helper, v).ventState() == WallVentBlock.State.SEALED, "a vent should be sealed while the heart sleeps");
			helper.assertFalse(vent(helper, v).burst(), "a sealed vent burst");
			helper.assertTrue(helper.getBlockState(v).getDestroySpeed(helper.getLevel(), helper.absolutePos(v)) == -1, "an unspent vent can be broken");
			record(helper).wakeAll(helper.getLevel(), origin(helper), null);
		});
		helper.runAtTickTime(8, () -> {
			for (BlockPos[] pair : VENTS)
				for (BlockPos p : pair)
					helper.assertTrue(vent(helper, p).isLive(), "the heart woke: the vent at " + p + " should be live");
			helper.assertTrue(vent(helper, loose).ventState() == WallVentBlock.State.SEALED, "a vent of no city went live");
			helper.assertTrue(vent(helper, v).burst(), "a live vent didn't burst");
			helper.assertTrue(vent(helper, v).isOpen() && !vent(helper, v).isLive(), "a burst vent should be open, spent");
			helper.assertFalse(vent(helper, v).burst(), "a vent burst twice");
			helper.assertTrue(helper.getBlockState(v).getDestroySpeed(helper.getLevel(), helper.absolutePos(v)) == WallVentBlock.HARDNESS,
				"a spent vent should be an ordinary block");
		});
		helper.runAtTickTime(14, () -> {
			// a reload or a chunk coming back: still open, still spent
			vent(helper, v).catchUp();
		});
		helper.runAtTickTime(20, () -> {
			helper.assertTrue(vent(helper, v).isOpen(), "a spent vent closed again");
			helper.succeed();
		});
	}

	/**
	 * The Custodian's called Scuttlers come out of the two live vents nearest the broken pylon, one from each, landing 1.25
	 * blocks out from the vent's face and tumbling out (vent_burst); the vents stay open. With all four pylons down, all
	 * eight vents have burst once each, and eight Scuttlers came out of them.
	 */
	@GameTest(template = COURSE, timeoutTicks = 480)
	public static void calledScuttlersBurstOutOfTheNearestVents(GameTestHelper helper) {
		Custodian c = hall(helper, true);
		vents(helper);
		Player player = player(helper);
		helper.runAtTickTime(2, () -> record(helper).wakeAll(helper.getLevel(), origin(helper), null));
		helper.runAtTickTime(6, c::forceFight);
		breakPylon(helper, player, CORNERS[0], 10);
		int broke = 10 + (ConduitPylonBlockEntity.maxHits() - 1) * 10;
		// the wrath (2 s), the call (its vents burst at 0.8 s), the Scuttlers out 0.3 s after that
		helper.runAtTickTime(broke + 20, () -> {
			helper.assertTrue(c.scuttlersCalled() == 0, "it called before its wrath had played");
			helper.assertTrue(vent(helper, VENTS[0][0]).isLive() && vent(helper, VENTS[0][1]).isLive(), "a vent burst before the call");
		});
		helper.runAtTickTime(broke + 75, () -> {
			helper.assertTrue(c.scuttlersCalled() == 2, "it called " + c.scuttlersCalled() + " for one pylon");
			for (BlockPos v : VENTS[0])
				helper.assertTrue(vent(helper, v).isOpen(), "the vent at " + v + " beside the broken pylon should have burst");
			for (int k = 1; k < 4; k++)
				for (BlockPos v : VENTS[k])
					helper.assertTrue(vent(helper, v).isLive(), "the vent at " + v + " by a standing pylon burst");
			List<? extends Scuttler> out = fightScuttlers(helper, c);
			helper.assertTrue(out.size() == 2, out.size() + " Scuttlers came out for one pylon");
			for (BlockPos v : VENTS[0]) {
				Vec3 at = landing(helper, v);
				// (across the floor: the course's floor there is a block lower, so it drops)
				helper.assertTrue(out.stream().anyMatch(s -> Math.hypot(s.getX() - at.x, s.getZ() - at.z) < 0.3),
					"no Scuttler landed 1.25 blocks out of the vent at " + v + " (" + at + "); they're at " + out.stream().map(s -> s.position().toString()).toList());
			}
			for (Scuttler s : out)
				helper.assertTrue(s.clip().name().equals("vent_burst"), "a called Scuttler should tumble out of its vent, plays " + s.clip().name());
		});
		// the other three, one after another
		for (int k = 1; k < 4; k++)
			breakPylon(helper, player, CORNERS[k], broke + 80 + (k - 1) * 70);
		helper.runAtTickTime(broke + 80 + 2 * 70 + (ConduitPylonBlockEntity.maxHits() - 1) * 10 + 100, () -> {
			helper.assertTrue(c.scuttlersCalled() == 8, "it called " + c.scuttlersCalled() + " for four pylons");
			helper.assertTrue(fightScuttlers(helper, c).size() == 8, fightScuttlers(helper, c).size() + " fight Scuttlers out, not 8");
			// each vent burst once and is left open; none can burst again
			for (BlockPos[] pair : VENTS)
				for (BlockPos v : pair) {
					helper.assertTrue(vent(helper, v).isOpen(), "the vent at " + v + " didn't burst");
					helper.assertFalse(vent(helper, v).burst(), "the vent at " + v + " burst twice");
				}
			helper.succeed();
		});
	}

	/**
	 * D67: when the Custodian resets, every spent vent shuts again with a new Scuttler inside, live, and bursts again in
	 * the next fight; a vent that's gone leaves its Scuttler to appear at the pylon's foot in smoke.
	 */
	@GameTest(template = COURSE, timeoutTicks = 700)
	public static void spentVentsResealWhenTheCustodianResets(GameTestHelper helper) {
		Custodian c = hall(helper, true);
		vents(helper);
		Player player = player(helper);
		helper.runAtTickTime(2, () -> record(helper).wakeAll(helper.getLevel(), origin(helper), null));
		helper.runAtTickTime(6, c::forceFight);
		breakPylon(helper, player, CORNERS[0], 10);
		int broke = 10 + (ConduitPylonBlockEntity.maxHits() - 1) * 10;
		helper.runAtTickTime(broke + 75, () -> {
			for (BlockPos v : VENTS[0])
				helper.assertTrue(vent(helper, v).isOpen(), "the vent at " + v + " should have burst");
			c.beginReset();
		});
		int reset = broke + 75 + 165;
		helper.runAtTickTime(reset, () -> {
			helper.assertTrue(c.dormant(), "it should be kneeling asleep again, is " + c.phase());
			for (BlockPos[] pair : VENTS)
				for (BlockPos v : pair) {
					helper.assertTrue(vent(helper, v).isLive(), "after the reset the vent at " + v + " should be shut and live, is " + vent(helper, v).ventState());
					helper.assertTrue(vent(helper, v).burstAt() == com.comedymonkey.createaddons.content.wastes.SubstationBlockEntity.NEVER,
						"a re-sealed vent still shows its burst");
					helper.assertTrue(helper.getBlockState(v).getDestroySpeed(helper.getLevel(), helper.absolutePos(v)) == -1, "a re-sealed vent can be broken");
				}
			// the next fight: every vent but one gone (broken), so the second Scuttler comes in smoke at the pylon's foot
			for (BlockPos[] pair : VENTS)
				for (BlockPos v : pair)
					if (!v.equals(VENTS[0][0]))
						helper.setBlock(v, net.minecraft.world.level.block.Blocks.AIR);
			c.forceFight();
		});
		breakPylon(helper, player, CORNERS[0], reset + 5);
		int broke2 = reset + 5 + (ConduitPylonBlockEntity.maxHits() - 1) * 10;
		helper.runAtTickTime(broke2 + 75, () -> {
			helper.assertTrue(c.scuttlersCalled() == 2, "it called " + c.scuttlersCalled() + " for one pylon in the new fight");
			helper.assertTrue(vent(helper, VENTS[0][0]).isOpen(), "the re-sealed vent should burst again");
			List<? extends Scuttler> out = fightScuttlers(helper, c);
			helper.assertTrue(out.size() == 2, out.size() + " Scuttlers came out for one pylon");
			Vec3 at = landing(helper, VENTS[0][0]), foot = Vec3.atBottomCenterOf(helper.absolutePos(CORNERS[0]));
			helper.assertTrue(out.stream().anyMatch(s -> Math.hypot(s.getX() - at.x, s.getZ() - at.z) < 0.3 && s.clip().name().equals("vent_burst")),
				"no Scuttler came out of the re-sealed vent; they're at " + out.stream().map(s -> s.position().toString()).toList());
			helper.assertTrue(out.stream().anyMatch(s -> Math.hypot(s.getX() - foot.x, s.getZ() - foot.z) < 1.6),
				"the vent that's gone should leave a Scuttler at the pylon's foot; they're at " + out.stream().map(s -> s.position().toString()).toList());
			helper.succeed();
		});
	}

	/** Its moves never change a block, and mob griefing is off for every bot (P37). */
	@GameTest(template = COURSE)
	public static void botsCantGrief(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Custodian c = hall(helper, false);
		Scuttler s = scuttler(helper, new BlockPos(4, 1, 4), District.FOUNDRY);
		helper.assertFalse(net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, c), "the Custodian can grief");
		helper.assertFalse(net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, s), "a Scuttler can grief");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the user's decisions of 2026-10-29 (D51-D58)

	/** A fake player put straight into the world (so the bots see it among the level's players), counted as a real one; it notes the hits it takes. */
	private static final class StandIn extends FakePlayer {
		final List<Float> hits = new ArrayList<>();

		StandIn(ServerLevel level, String name) {
			super(level, new GameProfile(UUID.randomUUID(), name));
		}

		@Override
		public boolean hurt(DamageSource source, float amount) {
			hits.add(amount);
			return true;
		}
	}

	private static StandIn standIn(GameTestHelper helper, BlockPos pos, String name) {
		StandIn p = new StandIn(helper.getLevel(), name);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(pos));
		p.moveTo(at.x, at.y, at.z, 0, 0);
		p.setOnGround(true);
		CityBot.STAND_INS_FOR_TESTS.add(p.getUUID());
		helper.getLevel().addNewPlayer(p);
		return p;
	}

	private static void leave(GameTestHelper helper, StandIn... players) {
		for (StandIn p : players) {
			if (p == null)
				continue;
			CityBot.STAND_INS_FOR_TESTS.remove(p.getUUID());
			if (!p.isRemoved())
				helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
		}
	}

	/** D52: asleep, a Scuttler, a Sentry and the kneeling Custodian take nothing and don't wake; /kill still ends one. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void dormantBotsAreUntouchable(GameTestHelper helper) {
		Custodian c = hall(helper, false);
		Scuttler scuttler = scuttler(helper, new BlockPos(4, 1, 16), District.FOUNDRY);
		Sentry sentry = sentry(helper, new BlockPos(8, 1, 16), District.FOUNDRY);
		Player player = player(helper);
		helper.runAtTickTime(2, () -> {
			for (CityBot bot : List.<CityBot>of(scuttler, sentry, c)) {
				float before = bot.getHealth();
				bot.invulnerableTime = 0;
				bot.hurt(bot.damageSources().playerAttack(player), 100);
				bot.invulnerableTime = 0;
				bot.hurt(bot.damageSources().arrow(null, player), 100);
				helper.assertTrue(bot.getHealth() == before, bot.getType().getDescriptionId() + " was hurt asleep");
				helper.assertTrue(bot.dormant() && bot.wakeAt() < 0, bot.getType().getDescriptionId() + " woke from a hit");
			}
			helper.assertFalse(c.hasWoken(), "a hit woke the Custodian");
			scuttler.kill();
			helper.assertTrue(scuttler.isDeadOrDying() || scuttler.isRemoved(), "/kill should still end a sleeping bot");
		});
		helper.runAtTickTime(40, helper::succeed);
	}

	/** D52: kneeling home after a reset it's untouchable too, pylons or not. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aKneelingCustodianIsUntouchable(GameTestHelper helper) {
		Custodian c = hall(helper, false);
		Player player = player(helper);
		helper.runAtTickTime(1, c::forceFight);
		helper.runAtTickTime(3, () -> {
			helper.assertTrue(hit(c, player, 10) > 0, "fighting with no pylons, a hit should land");
			c.beginReset();
		});
		helper.runAtTickTime(10, () -> {
			helper.assertTrue(c.phase() == Custodian.Phase.KNEELING, "on its dais it should kneel, is " + c.phase());
			helper.assertTrue(hit(c, player, 10) == 0, "a hit landed on it kneeling");
			helper.succeed();
		});
	}

	/**
	 * D51: kneeling after a reset, it stays asleep for a player too far off or out of sight, and wakes (rising, then
	 * bowing) for one within 16 blocks in its sight.
	 */
	@GameTest(template = COURSE, timeoutTicks = 220, batch = "wastes_custodian_rewake")
	public static void custodianWakesAgainForAPlayerInSight(GameTestHelper helper) {
		Custodian c = hall(helper, true);
		BlockPos o = origin(helper);
		StandIn[] who = new StandIn[2];
		helper.runAtTickTime(1, () -> record(helper).wakeAll(helper.getLevel(), o, null));
		helper.runAtTickTime(3, () -> {
			helper.assertFalse(c.dormant(), "the heart woke: it should wake");
			c.beginReset();
		});
		helper.runAtTickTime(80, () -> {
			helper.assertTrue(c.dormant() && c.hasWoken(), "reset, it should be kneeling asleep, is " + c.phase());
			who[0] = standIn(helper, new BlockPos(11, 1, -8), "far"); // 19 blocks off
		});
		helper.runAtTickTime(110, () -> {
			helper.assertTrue(c.dormant(), "a player 19 blocks off woke it");
			leave(helper, who[0]);
			for (BlockPos p : BlockPos.betweenClosed(6, 1, 15, 16, 6, 15))
				helper.setBlock(p, net.minecraft.world.level.block.Blocks.STONE);
			who[1] = standIn(helper, new BlockPos(11, 1, 19), "hidden"); // 8 blocks, behind a wall
		});
		helper.runAtTickTime(140, () -> {
			helper.assertTrue(c.dormant(), "a player out of its sight woke it");
			for (BlockPos p : BlockPos.betweenClosed(6, 1, 15, 16, 6, 15))
				helper.setBlock(p, net.minecraft.world.level.block.Blocks.AIR);
		});
		helper.runAtTickTime(170, () -> {
			try {
				helper.assertFalse(c.dormant(), "a player 8 blocks off in plain sight should wake it");
				helper.assertTrue(c.phase() == Custodian.Phase.WAKING || c.phase() == Custodian.Phase.BOWING, "it should rise and bow, is " + c.phase());
				helper.assertTrue(record(helper).custodianAwake(o), "the record should say it's awake (pylons harmable)");
			} finally {
				leave(helper, who[1]);
			}
			helper.succeed();
		});
	}

	/**
	 * D56: the world turning Peaceful sends woken bots to sleep (the Custodian walks home and kneels as in a reset, its
	 * Scuttlers crumble); the district's bots wake again within 5 s once the difficulty rises.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void peacefulSendsWokenBotsToSleep(GameTestHelper helper) {
		Custodian c = hall(helper, true);
		BlockPos o = origin(helper);
		Scuttler scuttler = scuttler(helper, new BlockPos(4, 1, 16), District.WORKSHOPS);
		Sentry sentry = sentry(helper, new BlockPos(8, 1, 16), District.WORKSHOPS);
		Scuttler[] fight = new Scuttler[1];
		helper.runAtTickTime(2, () -> {
			record(helper).wake(helper.getLevel(), o, District.WORKSHOPS, null);
			scuttler.wake();
			sentry.wake();
			c.forceFight();
			fight[0] = helper.spawn(ModWastes.SCUTTLER.get(), new BlockPos(16, 1, 16));
			fight[0].makeFightScuttler(c);
		});
		helper.runAtTickTime(5, () -> {
			helper.assertFalse(scuttler.dormant() || sentry.dormant() || c.dormant(), "they should all be awake");
			CityRecord.PEACEFUL_FOR_TESTS.add(o);
		});
		helper.runAtTickTime(8, () -> {
			helper.assertTrue(scuttler.dormant() && scuttler.sleptForPeace(), "Peaceful: the Scuttler should sleep");
			helper.assertTrue(sentry.dormant() && sentry.sleptForPeace(), "Peaceful: the Sentry should sleep");
			helper.assertTrue(fight[0].isRemoved(), "Peaceful: a fight Scuttler should crumble");
			helper.assertTrue(c.phase() == Custodian.Phase.RETURNING || c.phase() == Custodian.Phase.KNEELING, "Peaceful: it should go home, is " + c.phase());
		});
		helper.runAtTickTime(100, () -> {
			try {
				helper.assertTrue(c.dormant(), "Peaceful: it should kneel asleep, is " + c.phase());
				helper.assertTrue(standing(helper) == 4 && c.getHealth() == c.getMaxHealth(), "Peaceful: it should reset (pylons, health)");
				helper.assertFalse(record(helper).custodianAwake(o), "the record still says it's awake");
				helper.assertTrue(scuttler.dormant() && sentry.dormant(), "they woke on Peaceful");
			} finally {
				CityRecord.PEACEFUL_FOR_TESTS.remove(o);
			}
		});
		helper.runAtTickTime(210, () -> {
			helper.assertFalse(scuttler.dormant(), "the difficulty rose: the Scuttler should wake within 5 s");
			helper.assertFalse(sentry.dormant(), "the difficulty rose: the Sentry should wake within 5 s");
			helper.assertTrue(c.dormant(), "with nobody near, the Custodian should stay kneeling");
			helper.succeed();
		});
	}

	/** D56: walking home with its way blocked, after 10 s getting no nearer it appears on its dais and kneels. */
	@GameTest(template = COURSE, timeoutTicks = 420)
	public static void aStuckCustodianAppearsOnItsDais(GameTestHelper helper) {
		Custodian c = hall(helper, false);
		Vec3 dais = Vec3.atBottomCenterOf(helper.absolutePos(DAIS));
		helper.runAtTickTime(1, () -> {
			// a ring of wall round its dais, too high to step
			for (BlockPos p : BlockPos.betweenClosed(9, 1, 9, 13, 4, 13))
				if (p.getX() == 9 || p.getX() == 13 || p.getZ() == 9 || p.getZ() == 13)
					helper.setBlock(p, net.minecraft.world.level.block.Blocks.STONE);
			c.forceFight();
			Vec3 away = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 16)));
			c.moveTo(away.x, away.y, away.z);
		});
		helper.runAtTickTime(3, c::beginReset);
		helper.runAtTickTime(120, () -> {
			helper.assertTrue(c.phase() == Custodian.Phase.RETURNING, "walled out, it should still be trying, is " + c.phase());
			helper.assertTrue(c.position().distanceTo(dais) > 2, "it reached its dais through a wall");
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getTick() > 120, "too soon");
			helper.assertTrue(c.position().distanceTo(dais) < 0.7, "it should appear on its dais, is " + c.position().distanceTo(dais) + " off");
			helper.assertTrue(c.phase() == Custodian.Phase.KNEELING || c.dormant(), "on its dais it should kneel, is " + c.phase());
		});
	}

	/**
	 * D56: the shockwave is a ring running out 8 blocks from the clip's stamp: a player on the ground 5 blocks off is hit
	 * once, as the crest reaches them (not before); one in the air as it passes isn't; one 11 blocks off isn't.
	 */
	@GameTest(template = COURSE, timeoutTicks = 80, batch = "wastes_custodian_ring")
	public static void theShockwaveRingHitsAsItPasses(GameTestHelper helper) {
		Custodian c = hall(helper, false);
		StandIn ground = standIn(helper, new BlockPos(16, 1, 11), "ground");
		StandIn jumper = standIn(helper, new BlockPos(6, 1, 11), "jumper");
		StandIn far = standIn(helper, new BlockPos(11, 1, 22), "far");
		jumper.setOnGround(false);
		helper.runAtTickTime(1, c::forceFight);
		helper.runAtTickTime(2, c::testShockwave);
		helper.runAtTickTime(14, () -> helper.assertTrue(c.ringAge(0) < 0, "the ring ran before the stamp (0.8 s)"));
		helper.runAtTickTime(22, () -> {
			helper.assertTrue(c.ringAge(0) >= 0, "the ring should be running out after the stamp");
			helper.assertTrue(ground.hits.isEmpty(), "the player 5 blocks off was hit before the ring reached them");
		});
		helper.runAtTickTime(45, () -> {
			try {
				helper.assertTrue(c.ringAge(0) < 0, "the ring should be spent after 8 blocks");
				helper.assertTrue(ground.hits.equals(List.of(Custodian.SHOCKWAVE_DAMAGE)), "the player on the ground should take one hit of 12, took " + ground.hits);
				helper.assertTrue(jumper.hits.isEmpty(), "the player in the air was hit: " + jumper.hits);
				helper.assertTrue(far.hits.isEmpty(), "the player 11 blocks off was hit: " + far.hits);
			} finally {
				leave(helper, ground, jumper, far);
			}
			helper.succeed();
		});
	}

	/** D53: a Sentry killed by a player drops 1-2 scrap (and now and then an Electron Tube); killed any other way, nothing. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void sentryLootOnlyForAPlayersKill(GameTestHelper helper) {
		clear(helper);
		Sentry killed = sentry(helper, new BlockPos(5, 1, 5), District.QUARTERS);
		Sentry other = sentry(helper, new BlockPos(17, 1, 17), District.QUARTERS);
		Player player = player(helper);
		helper.runAtTickTime(2, () -> {
			record(helper).wake(helper.getLevel(), origin(helper), District.QUARTERS, null);
			killed.wake();
			other.wake();
		});
		helper.runAtTickTime(5, () -> {
			killed.hurt(killed.damageSources().playerAttack(player), 100);
			other.hurt(other.damageSources().generic(), 100);
			helper.assertTrue(killed.isDeadOrDying() && other.isDeadOrDying(), "both Sentries should be dead");
		});
		helper.runAtTickTime(30, () -> {
			List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(5, 1, 5))).inflate(3));
			int scrap = drops.stream().filter(e -> e.getItem().is(ModWastes.CLOCKWORK_SCRAP.get())).mapToInt(e -> e.getItem().getCount()).sum();
			helper.assertTrue(scrap >= 1 && scrap <= 2, "a player's kill should drop 1-2 scrap, dropped " + scrap);
			helper.assertTrue(drops.stream().allMatch(e -> e.getItem().is(ModWastes.CLOCKWORK_SCRAP.get())
				|| net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString().equals("create:electron_tube")),
				"it dropped something besides scrap and an Electron Tube");
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(17, 1, 17))).inflate(3)).isEmpty(),
				"a Sentry not killed by a player dropped items");
			helper.succeed();
		});
	}

	/** D58: hitboxes, the boss bar's yellow, the Sentry's 10 degree aim. */
	@GameTest(template = COURSE)
	public static void theAgreedDetails(GameTestHelper helper) {
		helper.assertTrue(ModWastes.SCUTTLER.get().getWidth() == 0.75f && ModWastes.SCUTTLER.get().getHeight() == 0.5f, "Scuttler hitbox");
		helper.assertTrue(ModWastes.SENTRY.get().getWidth() == 1f && ModWastes.SENTRY.get().getHeight() == 1f, "Sentry hitbox");
		helper.assertTrue(ModWastes.CUSTODIAN.get().getWidth() == 1.4f && ModWastes.CUSTODIAN.get().getHeight() == 3f, "Custodian hitbox");
		helper.assertTrue(Sentry.AIMED == 10, "the Sentry should fire within 10 degrees");
		Custodian c = hall(helper, false);
		helper.assertTrue(c.bossBarColor() == net.minecraft.world.BossEvent.BossBarColor.YELLOW, "the boss bar should be yellow");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the user's decisions of 2026-10-29 (D67)

	/** A stand-in player in creative or spectating: one a bot can't fight. */
	private static final class Onlooker extends FakePlayer {
		private final boolean spectating;

		Onlooker(ServerLevel level, boolean spectating) {
			super(level, new GameProfile(UUID.randomUUID(), spectating ? "spectator" : "creative"));
			this.spectating = spectating;
		}

		@Override
		public boolean isCreative() {
			return !spectating;
		}

		@Override
		public boolean isSpectator() {
			return spectating;
		}
	}

	private static Onlooker onlooker(GameTestHelper helper, BlockPos pos, boolean spectating) {
		Onlooker p = new Onlooker(helper.getLevel(), spectating);
		// the game mode itself too: its abilities (a creative player is invulnerable, so never a mob's enemy)
		p.setGameMode(spectating ? GameType.SPECTATOR : GameType.CREATIVE);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(pos));
		p.moveTo(at.x, at.y, at.z, 0, 0);
		p.setOnGround(true);
		CityBot.STAND_INS_FOR_TESTS.add(p.getUUID());
		helper.getLevel().addNewPlayer(p);
		return p;
	}

	private static void leave(GameTestHelper helper, Onlooker... players) {
		for (Onlooker p : players) {
			CityBot.STAND_INS_FOR_TESTS.remove(p.getUUID());
			if (!p.isRemoved())
				helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
		}
	}

	/**
	 * D67: a player a Scuttler can't fight (creative, spectating) within about 8 blocks, in its sight, makes it curious: it
	 * plays curious facing them, then goes back to idle and isn't curious again until its cooldown has run; one farther
	 * off never is.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200, batch = "wastes_scuttler_curious")
	public static void aScuttlerIsCuriousAboutAPlayerItCantFight(GameTestHelper helper) {
		clear(helper);
		Scuttler near = helper.spawn(ModWastes.SCUTTLER.get(), new BlockPos(4, 1, 4));
		Scuttler spectated = helper.spawn(ModWastes.SCUTTLER.get(), new BlockPos(19, 1, 19));
		Scuttler far = helper.spawn(ModWastes.SCUTTLER.get(), new BlockPos(4, 1, 19));
		Onlooker[] who = new Onlooker[2];
		helper.runAtTickTime(1, () -> {
			who[0] = onlooker(helper, new BlockPos(4, 1, 9), false);
			who[1] = onlooker(helper, new BlockPos(19, 1, 14), true);
		});
		helper.runAtTickTime(10, () -> {
			try {
				helper.assertTrue(near.curious() && near.clip().name().equals("curious"), "a creative player 5 blocks off: it should be curious, plays " + near.clip().name());
				helper.assertTrue(spectated.curious(), "a spectator 5 blocks off: it should be curious, plays " + spectated.clip().name());
				helper.assertFalse(far.curious(), "a Scuttler 10 blocks from the nearest onlooker got curious");
				helper.assertTrue(near.getTarget() == null, "it took a creative player for a target");
				// it faces them (they're due south of it: yaw 0)
				helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(near.getYRot())) < 5, "it should face the player, faces " + near.getYRot());
			} catch (RuntimeException e) {
				leave(helper, who);
				throw e;
			}
		});
		helper.runAtTickTime(10 + Scuttler.CURIOUS_TICKS + 5, () -> {
			try {
				helper.assertFalse(near.curious(), "it should have gone back to idle");
				helper.assertTrue(near.clip().name().equals("idle"), "after curious it should idle, plays " + near.clip().name());
				helper.assertTrue(near.curiousCooldown() > 0, "it should be cooling down");
			} catch (RuntimeException e) {
				leave(helper, who);
				throw e;
			}
		});
		helper.runAtTickTime(10 + Scuttler.CURIOUS_TICKS + 80, () -> {
			try {
				helper.assertFalse(near.curious(), "it was curious again within its cooldown (the player never left)");
				helper.assertFalse(far.curious(), "the far one got curious");
			} finally {
				leave(helper, who);
			}
			helper.succeed();
		});
	}

	/** D67: a hit flips a Scuttler at most once every 4 s; the hits between still hurt it. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void aHitFlipsAScuttlerAtMostOnceEvery4s(GameTestHelper helper) {
		clear(helper);
		Scuttler s = helper.spawn(ModWastes.SCUTTLER.get(), new BlockPos(11, 1, 11));
		long[] first = new long[1];
		float[] health = new float[1];
		helper.runAtTickTime(5, () -> {
			s.invulnerableTime = 0;
			s.hurt(s.damageSources().generic(), 1);
			helper.assertTrue(s.clip().name().equals("flipped") && s.busy(), "the first hit should flip it, plays " + s.clip().name());
			first[0] = s.lastFlip();
			helper.assertTrue(first[0] == helper.getLevel().getGameTime(), "the flip wasn't noted");
		});
		helper.runAtTickTime(15, () -> {
			// still on its back: hit again, it stays as it was (no new flip), and takes the damage
			health[0] = s.getHealth();
			s.invulnerableTime = 0;
			s.hurt(s.damageSources().generic(), 1);
			helper.assertTrue(s.lastFlip() == first[0], "a hit while it was on its back flipped it again");
			helper.assertTrue(s.getHealth() < health[0], "a hit inside the window should still hurt it");
		});
		helper.runAtTickTime(45, () -> {
			// righted, still inside the 4 s: no flip, still hurt
			health[0] = s.getHealth();
			s.invulnerableTime = 0;
			s.hurt(s.damageSources().generic(), 1);
			helper.assertTrue(s.lastFlip() == first[0], "a hit 2 s after the flip flipped it again");
			helper.assertFalse(s.clip().name().equals("flipped") && s.clipElapsed() < 0.1f, "it started its flip again 2 s after the last");
			helper.assertTrue(s.getHealth() < health[0], "a hit inside the window should still hurt it");
		});
		helper.runAtTickTime(5 + Scuttler.FLIP_EVERY + 1, () -> {
			s.invulnerableTime = 0;
			s.hurt(s.damageSources().generic(), 1);
			helper.assertTrue(s.lastFlip() == helper.getLevel().getGameTime(), "4 s on, a hit should flip it again");
			helper.assertTrue(s.clip().name().equals("flipped"), "4 s on it should be flipped, plays " + s.clip().name());
			helper.succeed();
		});
	}

	/**
	 * D67: a hit is told from its left or its right by the bot's facing (the Sentry's head as its yaw ring has turned it;
	 * the Custodian's body); until the left-side clips are approved and exported every hit plays the right-side clip.
	 */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aHitIsToldFromItsLeftOrItsRight(GameTestHelper helper) {
		Custodian c = hall(helper, false);
		Sentry sentry = helper.spawn(ModWastes.SENTRY.get(), new BlockPos(5, 1, 18));
		Player player = player(helper);
		helper.runAtTickTime(2, c::forceFight);
		helper.runAtTickTime(6, () -> {
			// a floor Sentry's head faces south at rest, so its right is west
			Vec3 facing = sentry.headFacing(), right = facing.cross(sentry.headUp());
			helper.assertTrue(facing.distanceTo(new Vec3(0, 0, 1)) < 1e-4 && right.distanceTo(new Vec3(-1, 0, 0)) < 1e-4,
				"a floor Sentry should face south with its right to the west: " + facing + ", " + right);
			for (CityBot.Side side : CityBot.Side.values()) {
				Vec3 from = sentry.headPos().add(right.scale(side == CityBot.Side.RIGHT ? 3 : -3));
				player.setPos(from.x, from.y, from.z);
				sentry.invulnerableTime = 0;
				sentry.hurt(sentry.damageSources().playerAttack(player), 1);
				helper.assertTrue(sentry.lastStruck() == side, "a hit from its " + side + " was told as " + sentry.lastStruck());
				helper.assertTrue(sentry.clip().name().equals(CityBot.hitClip(side, CityBot.LEFT_HIT_CLIPS)), "the Sentry plays " + sentry.clip().name());
				helper.assertTrue(CityBot.LEFT_HIT_CLIPS || sentry.clip().name().equals("hit"), "until the left clips are in, every hit plays hit");
			}
			// the Custodian, unshielded in its fight, by its body's facing
			helper.assertTrue(c.phase() == Custodian.Phase.FIGHT, "it should be fighting, is " + c.phase());
			Vec3 cRight = CityBot.facing(c.yBodyRot).cross(new Vec3(0, 1, 0));
			for (CityBot.Side side : CityBot.Side.values()) {
				Vec3 from = c.position().add(cRight.scale(side == CityBot.Side.RIGHT ? 3 : -3));
				player.setPos(from.x, from.y, from.z);
				hit(c, player, 1);
				helper.assertTrue(c.lastStruck() == side, "a hit from the Custodian's " + side + " was told as " + c.lastStruck());
				helper.assertTrue(c.clip().name().equals(CityBot.hitClip(side, CityBot.LEFT_HIT_CLIPS)), "the Custodian plays " + c.clip().name());
			}
			helper.succeed();
		});
	}
}
