package com.comedymonkey.createaddons.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlock;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity;
import com.comedymonkey.createaddons.content.cage.MonsterJarItem;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Netherite cage's field on too little power (user, 2026-10-27: "if there isn't enough power going in the cage
 * should draw no power at all"). Off, it takes no FE; it starts only on a second of offers covering its cost, and comes
 * up once that FE arrives; up, it runs as ever on its store; and it never strobes. A half supply or a trickle leaves it
 * off, and the Wither gets out. Each test whose countdown must run brings a listener into the world and shortens the
 * warning, so has a batch of its own.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CageFieldGameTests {
	private static final String COURSE = "trade_bot_course";
	private static final BlockPos CAGE = new BlockPos(4, 1, 4);

	private CageFieldGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(CageFieldGameTests.class);
	}

	/** A supply pushing so much a tick, as cables do: simulated first, then for real what was accepted. */
	private static final class Supply {
		int perTick;
		long delivered;

		Supply(int perTick) {
			this.perTick = perTick;
		}

		void push(IEnergyStorage into) {
			if (perTick <= 0)
				return;
			int can = into.receiveEnergy(perTick, true);
			if (can > 0)
				delivered += into.receiveEnergy(can, false);
		}
	}

	/** What happened to the field over a test: flips, syncs and chat. */
	private static final class Watch {
		int flips;
		boolean up;
		final Set<Long> syncs = new HashSet<>();
		final List<Component> said = new ArrayList<>();

		void tick(MonsterCageBlockEntity cage) {
			if (cage.isFieldUp() != up) {
				up = cage.isFieldUp();
				flips++;
			}
			syncs.add(longField(cage, "ownSyncAt"));
		}

		/** How many announcements with this message (gui.monster_cage.{@code key}) were heard. */
		int said(String key) {
			String full = ComedyMonkeysCreateAddons.MOD_ID + ".gui.monster_cage." + key;
			int n = 0;
			for (Component c : said)
				if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals(full))
					n++;
			return n;
		}
	}

	private static int fieldCost() {
		return CMConfig.Server.get(CMConfig.SERVER.cageFieldFe);
	}

	/** A Netherite cage holding a Wither, its supply pushed every tick, and a watch on it. */
	private static MonsterCageBlockEntity cage(GameTestHelper helper, Supply supply, Watch watch) {
		helper.setBlock(CAGE, ModBlocks.NETHERITE_MONSTER_CAGE.get().defaultBlockState().setValue(MonsterCageBlock.FACING, Direction.NORTH));
		MonsterCageBlockEntity cage = helper.getBlockEntity(CAGE);
		ItemStack wither = ((MonsterJarItem) ModItems.NETHERITE_JAR.get()).fill(EntityType.WITHER.create(helper.getLevel()));
		if (!cage.getCaptiveSlot().insertItem(0, wither, false).isEmpty())
			helper.fail("the Netherite cage didn't take the Wither");
		helper.onEachTick(() -> {
			supply.push(cage.getEnergy());
			watch.tick(cage);
		});
		return cage;
	}

	@GameTest(template = COURSE, batch = "cage_field_full", timeoutTicks = 200)
	public static void aFullSupplyBringsTheFieldUpAndKeepsIt(GameTestHelper helper) {
		Supply supply = new Supply(fieldCost());
		Watch watch = new Watch();
		MonsterCageBlockEntity cage = cage(helper, supply, watch);
		// a second of offers, then a second of FE coming in
		int[] stood = {0};
		helper.runAtTickTime(60, () -> {
			if (!cage.isFieldUp())
				helper.fail("a full supply hadn't brought the field up in 3 s (" + supply.delivered + " FE in)");
			stood[0] = cage.getEscapeIn();
		});
		helper.runAtTickTime(160, () -> {
			if (!cage.isFieldUp() || watch.flips != 1)
				helper.fail("the field went up and down " + watch.flips + " times on a full supply");
			// the countdown started while it was off stands still while it holds (called off after a minute)
			if (cage.getEscapeIn() != stood[0])
				helper.fail("the countdown moved with the field up: " + stood[0] + " then " + cage.getEscapeIn());
			helper.succeed();
		});
	}

	/** Half the field's cost: off throughout, taking nothing, and the Wither breaks out after the warning, with no strobe. */
	@GameTest(template = COURSE, batch = "cage_field_half", timeoutTicks = 400)
	public static void aHalfSupplyLeavesItOffAndTheWitherEscapes(GameTestHelper helper) {
		staysOffAndTheWitherEscapes(helper, fieldCost() / 2);
	}

	/** A trickle of 1 FE a tick, which used to keep the field alive for ever. */
	@GameTest(template = COURSE, batch = "cage_field_trickle", timeoutTicks = 400)
	public static void aTrickleLeavesItOffAndTheWitherEscapes(GameTestHelper helper) {
		staysOffAndTheWitherEscapes(helper, 1);
	}

	private static void staysOffAndTheWitherEscapes(GameTestHelper helper, int perTick) {
		int warning = CMConfig.Server.get(CMConfig.SERVER.cageWarningSeconds);
		CMConfig.SERVER.cageWarningSeconds.set(10);
		Supply supply = new Supply(perTick);
		Watch watch = new Watch();
		FakePlayer listener = listener(helper, watch);
		MonsterCageBlockEntity cage;
		try {
			cage = cage(helper, supply, watch);
		} catch (RuntimeException e) {
			cleanUp(helper, listener, warning);
			throw e;
		}
		long[] ticks = {0}, escapedAt = {-1};
		helper.onEachTick(() -> {
			if (escapedAt[0] >= 0)
				return;
			ticks[0]++;
			List<WitherBoss> out = helper.getLevel().getEntitiesOfClass(WitherBoss.class, new AABB(helper.absolutePos(CAGE)).inflate(12));
			if (!out.isEmpty()) {
				// out: gone again before it can blow the course up
				out.forEach(Entity::discard);
				escapedAt[0] = ticks[0];
				cleanUp(helper, listener, warning);
			}
		});
		helper.succeedWhen(() -> {
			if (escapedAt[0] < 0)
				helper.fail("no Wither out yet: escape in " + cage.getEscapeIn() + ", paused " + cage.isPaused());
			if (supply.delivered != 0 || cage.getEnergyStored() != 0)
				helper.fail(perTick + " FE a tick: the cage took " + supply.delivered + " FE (stores " + cage.getEnergyStored() + ")");
			if (watch.flips != 0)
				helper.fail("the field flipped " + watch.flips + " times on " + perTick + " FE a tick");
			// one sync a second for the countdown, and a few for its start and the escape
			int synced = watch.syncs.size() - 1;
			if (synced > escapedAt[0] / 20 + 4)
				helper.fail("synced " + synced + " times in " + escapedAt[0] + " ticks");
			// the alarm going off and the Wither getting out; nothing restored
			if (watch.said.size() > 3)
				helper.fail("the chat had " + watch.said.size() + " announcements: " + watch.said);
		});
	}

	/**
	 * Cut off while running: it runs on its store, then goes off once. Back on a full supply, it comes up again after a
	 * second of offers and a second of FE coming in; the countdown it had started stands still (it's called off only
	 * once the field has held for a minute: see aRestoredSupplyCallsOffTheCountdownAfterTheHold).
	 */
	@GameTest(template = COURSE, batch = "cage_field_cut", timeoutTicks = 400)
	public static void aCutSupplyRunsTheStoreDownThenComesBack(GameTestHelper helper) {
		Supply supply = new Supply(2 * fieldCost());
		Watch watch = new Watch();
		MonsterCageBlockEntity cage = cage(helper, supply, watch);
		int[] storedAtCut = {0};
		long[] cutAt = {0}, downAt = {0}, backAt = {0};
		helper.succeedWhen(() -> {
			long now = helper.getTick();
			if (cutAt[0] == 0) {
				// up, with a store of 40 ticks or more
				if (!cage.isFieldUp() || cage.getEnergyStored() < 40 * fieldCost())
					helper.fail("not up with a store yet");
				storedAtCut[0] = cage.getEnergyStored();
				supply.perTick = 0;
				cutAt[0] = now;
				helper.fail("cut");
			}
			if (downAt[0] == 0) {
				if (cage.isFieldUp())
					helper.fail("still running on its store");
				downAt[0] = now;
				long ran = downAt[0] - cutAt[0], could = storedAtCut[0] / fieldCost();
				if (ran < could - 2 || ran > could + 2)
					helper.fail("ran " + ran + " ticks on a store good for " + could);
				if (watch.flips != 2)
					helper.fail("the field flipped " + watch.flips + " times going down once");
				supply.perTick = 2 * fieldCost();
				helper.fail("restored");
			}
			if (backAt[0] == 0) {
				if (!cage.isFieldUp())
					helper.fail("not back up");
				backAt[0] = now;
				long took = backAt[0] - downAt[0];
				if (took < 20 || took > 65)
					helper.fail("came back " + took + " ticks after the supply did (a second of offers, a second of FE)");
				if (cage.getEscapeIn() < 0)
					helper.fail("the countdown was called off at once, not after the hold");
				if (watch.flips != 3)
					helper.fail("the field flipped " + watch.flips + " times");
			}
		});
	}

	/** Off, simulated and real offers both get 0, and both are noted. */
	@GameTest(template = COURSE, batch = "cage_field_full")
	public static void offItRefusesEveryOfferAndNotesIt(GameTestHelper helper) {
		Supply none = new Supply(0);
		MonsterCageBlockEntity cage = cage(helper, none, new Watch());
		helper.runAtTickTime(3, () -> {
			if (cage.isFieldUp())
				helper.fail("the field is up with no supply");
			int simulated = cage.getEnergy().receiveEnergy(5000, true), real = cage.getEnergy().receiveEnergy(5000, false);
			if (simulated != 0 || real != 0)
				helper.fail("off, it took " + simulated + " simulated and " + real + " real");
			if (longField(cage, "offeredNow") != 10_000)
				helper.fail("the offers weren't noted: " + longField(cage, "offeredNow"));
			if (cage.getEnergyStored() != 0)
				helper.fail("it stored " + cage.getEnergyStored());
			helper.succeed();
		});
	}

	/**
	 * A battery charged by a weak generator (1,024 FE/t; it gives as an Industrial Battery does, simulating first, up to
	 * 2,048 a tick from what it holds): it can bring the field up in bursts, but they never call off the countdown, which
	 * stands still through each and carries on after; the Wither gets out once the countdown has run its 10 s.
	 */
	@GameTest(template = COURSE, batch = "cage_field_bursts", timeoutTicks = 3000)
	public static void burstsFromABatteryNeverCallOffTheCountdown(GameTestHelper helper) {
		int warning = CMConfig.Server.get(CMConfig.SERVER.cageWarningSeconds);
		CMConfig.SERVER.cageWarningSeconds.set(10);
		Watch watch = new Watch();
		FakePlayer listener = listener(helper, watch);
		Battery battery = new Battery(1_000_000, 100_000, 1024, 2 * fieldCost());
		MonsterCageBlockEntity cage;
		try {
			cage = cage(helper, new Supply(0), watch);
		} catch (RuntimeException e) {
			cleanUp(helper, listener, warning);
			throw e;
		}
		int[] lastEscape = {-1}, ran = {0}, held = {0};
		String[] wrong = {null};
		long[] escapedAt = {-1};
		helper.onEachTick(() -> {
			if (escapedAt[0] >= 0)
				return;
			// out: gone again before it can blow the course up (and the countdown's done)
			List<WitherBoss> out = helper.getLevel().getEntitiesOfClass(WitherBoss.class, new AABB(helper.absolutePos(CAGE)).inflate(12));
			if (!out.isEmpty()) {
				out.forEach(Entity::discard);
				escapedAt[0] = helper.getTick();
				cleanUp(helper, listener, warning);
				return;
			}
			battery.tick(cage.getEnergy());
			int escape = cage.getEscapeIn();
			if (cage.isFieldUp())
				held[0]++;
			// the escape itself empties the cage, which ends the countdown: only a burst before that counts
			if (lastEscape[0] >= 0 && wrong[0] == null && cage.hasMonster()) {
				if (escape < 0)
					wrong[0] = "the countdown was called off by a burst at " + lastEscape[0];
				else if (escape > lastEscape[0])
					wrong[0] = "the countdown went back from " + lastEscape[0] + " to " + escape;
				else if (escape < lastEscape[0])
					ran[0] += lastEscape[0] - escape;
			}
			lastEscape[0] = escape;
		});
		helper.succeedWhen(() -> {
			if (escapedAt[0] < 0)
				helper.fail("no Wither out yet: escape in " + cage.getEscapeIn() + ", up for " + held[0] + " ticks so far");
			if (wrong[0] != null)
				helper.fail(wrong[0]);
			if (watch.flips < 2)
				helper.fail("the battery never brought the field up: no bursts to test");
			// the countdown ran its 10 s, give or take a tick or two, however long the bursts held it
			if (Math.abs(ran[0] - 200) > 3)
				helper.fail("the countdown ran " + ran[0] + " ticks, not 200");
			if (watch.said("alarm_restored") > 0)
				helper.fail("a burst announced the containment restored");
		});
	}

	/**
	 * A full supply back mid-countdown: the field comes up and the countdown stands still for the hold (here 3 s), with
	 * one "holding" message; then it's called off with "restored". Not before.
	 */
	@GameTest(template = COURSE, batch = "cage_field_hold", timeoutTicks = 400)
	public static void aRestoredSupplyCallsOffTheCountdownAfterTheHold(GameTestHelper helper) {
		int warning = CMConfig.Server.get(CMConfig.SERVER.cageWarningSeconds), hold = CMConfig.Server.get(CMConfig.SERVER.cageRestoreHoldSeconds);
		CMConfig.SERVER.cageWarningSeconds.set(10);
		CMConfig.SERVER.cageRestoreHoldSeconds.set(3);
		Watch watch = new Watch();
		FakePlayer listener = listener(helper, watch);
		Supply supply = new Supply(0);
		MonsterCageBlockEntity cage;
		try {
			cage = cage(helper, supply, watch);
		} catch (RuntimeException e) {
			cleanUp(helper, listener, warning);
			CMConfig.SERVER.cageRestoreHoldSeconds.set(hold);
			throw e;
		}
		long[] upAt = {-1};
		int[] stood = {-1};
		helper.runAtTickTime(40, () -> {
			if (cage.getEscapeIn() < 0 || cage.isPaused())
				helper.fail("no countdown running");
			supply.perTick = 2 * fieldCost();
		});
		helper.onEachTick(() -> {
			if (upAt[0] < 0 && cage.isFieldUp()) {
				upAt[0] = helper.getTick();
				stood[0] = cage.getEscapeIn();
			}
		});
		helper.succeedWhen(() -> {
			if (upAt[0] < 0)
				helper.fail("not up yet");
			long since = helper.getTick() - upAt[0];
			if (since < 55) {
				if (cage.getEscapeIn() != stood[0])
					throw new IllegalStateException("the countdown moved during the hold: " + stood[0] + " then " + cage.getEscapeIn());
				helper.fail("holding");
			}
			if (since < 62 && cage.getEscapeIn() >= 0)
				helper.fail("not called off yet");
			try {
				if (cage.getEscapeIn() >= 0)
					helper.fail("the countdown wasn't called off after the hold");
				if (watch.said("alarm_holding") != 1 || watch.said("alarm_restored") != 1)
					helper.fail("holding said " + watch.said("alarm_holding") + " times, restored " + watch.said("alarm_restored"));
			} finally {
				cleanUp(helper, listener, warning);
				CMConfig.SERVER.cageRestoreHoldSeconds.set(hold);
			}
		});
	}

	/** The field dropping before the hold is out: the countdown carries on from where it stood, not from the start. */
	@GameTest(template = COURSE, batch = "cage_field_drop", timeoutTicks = 400)
	public static void aDropDuringTheHoldResumesTheCountdown(GameTestHelper helper) {
		int warning = CMConfig.Server.get(CMConfig.SERVER.cageWarningSeconds);
		CMConfig.SERVER.cageWarningSeconds.set(10);
		Watch watch = new Watch();
		FakePlayer listener = listener(helper, watch);
		Supply supply = new Supply(0);
		MonsterCageBlockEntity cage;
		try {
			cage = cage(helper, supply, watch);
		} catch (RuntimeException e) {
			cleanUp(helper, listener, warning);
			throw e;
		}
		long[] upAt = {-1}, downAt = {-1};
		int[] stood = {-1};
		helper.runAtTickTime(40, () -> supply.perTick = 2 * fieldCost());
		helper.onEachTick(() -> {
			if (upAt[0] < 0 && cage.isFieldUp()) {
				// cut at once: it runs a second or so on what its trial brought in, then drops
				upAt[0] = helper.getTick();
				stood[0] = cage.getEscapeIn();
				supply.perTick = 0;
			} else if (upAt[0] >= 0 && downAt[0] < 0 && !cage.isFieldUp()) {
				downAt[0] = helper.getTick();
			}
		});
		helper.succeedWhen(() -> {
			if (downAt[0] < 0 || helper.getTick() < downAt[0] + 20)
				helper.fail("not down a second yet");
			try {
				int now = cage.getEscapeIn();
				// a second after the drop: a second on from where it stood (give or take a tick), not back at the start
				if (now > stood[0] - 18 || now < stood[0] - 23)
					helper.fail("the countdown stood at " + stood[0] + " and is at " + now + " a second after the drop");
				if (watch.said("alarm_start") != 1 || watch.said("alarm_restored") != 0)
					helper.fail("started " + watch.said("alarm_start") + " times, restored " + watch.said("alarm_restored"));
			} finally {
				cleanUp(helper, listener, warning);
			}
		});
	}

	/**
	 * A generator alone that makes under half the field's cost (950 FE/t; it gives as a Lava Generator does, its whole
	 * store every tick, from a 100,000 FE store). A trial straight after it has emptied fails: it takes no more than the
	 * field's cost a tick, keeps what it took in the store (nothing drawn), and the next waits 10, 20, 40 and then 60 s.
	 * (After each wait its full store carries a trial, and the field holds a few seconds: a burst.) Not the Lava
	 * Generator's 1,024: exactly half, its trials pass or fail by the order things tick in.
	 */
	@GameTest(template = COURSE, batch = "cage_field_generator", timeoutTicks = 4000)
	public static void aWeakGeneratorsTrialsBackOff(GameTestHelper helper) {
		Generator generator = new Generator(100_000, 950);
		MonsterCageBlockEntity cage = cage(helper, new Supply(0), new Watch());
		int cost = fieldCost();
		int[] expected = {200, 400, 800, 1200};
		int[] failures = {0}, storedAtStart = {0}, storedAtFail = {-1};
		long[] deliveredAtStart = {0}, failedAt = {-1};
		boolean[] onTrial = {false};
		String[] wrong = {null};
		helper.onEachTick(() -> {
			if (failures[0] >= expected.length)
				return;
			generator.tick(cage.getEnergy());
			boolean trial = field(cage, "startingFor") >= 0;
			if (trial && !onTrial[0]) {
				// a trial starts: the wait since the last failure, and nothing drawn from the store meanwhile
				if (failedAt[0] >= 0) {
					long waited = helper.getTick() - failedAt[0];
					int want = expected[failures[0] - 1];
					if (wrong[0] == null && (waited < want || waited > want + 25))
						wrong[0] = "after failure " + failures[0] + " the next trial came " + waited + " ticks later, not " + want;
					if (wrong[0] == null && cage.getEnergyStored() < storedAtFail[0])
						wrong[0] = "the store went from " + storedAtFail[0] + " to " + cage.getEnergyStored() + " while off";
					failedAt[0] = -1;
				}
				storedAtStart[0] = cage.getEnergyStored();
				deliveredAtStart[0] = generator.delivered;
			} else if (!trial && onTrial[0] && !cage.isFieldUp()) {
				long took = generator.delivered - deliveredAtStart[0];
				if (wrong[0] == null && took > (long) SUPPLY_TICKS * cost)
					wrong[0] = "a failed trial took " + took + " FE, more than a second of the field's cost";
				if (wrong[0] == null && cage.getEnergyStored() != storedAtStart[0] + took)
					wrong[0] = "a failed trial's FE wasn't kept: store " + storedAtStart[0] + " + " + took + " came to " + cage.getEnergyStored();
				int wait = field(cage, "retryWait");
				if (wrong[0] == null && wait != expected[failures[0]])
					wrong[0] = "failure " + (failures[0] + 1) + " set a wait of " + wait + ", not " + expected[failures[0]];
				failures[0]++;
				failedAt[0] = helper.getTick();
				storedAtFail[0] = cage.getEnergyStored();
			}
			onTrial[0] = trial;
		});
		helper.succeedWhen(() -> {
			if (wrong[0] != null)
				throw new IllegalStateException(wrong[0]);
			if (failures[0] < expected.length)
				helper.fail(failures[0] + " failed trials so far");
		});
	}

	/** Power Lines (a Lava Generator, copper wire) bring an empty cage's field up (its cost lowered to the generator's). */
	@GameTest(template = COURSE, batch = "cage_field_lines", timeoutTicks = 300)
	public static void powerLinesStartAnEmptyCage(GameTestHelper helper) {
		int cost = fieldCost();
		CMConfig.SERVER.cageFieldFe.set(512);
		BlockPos gen = new BlockPos(1, 1, 4);
		Watch watch = new Watch();
		MonsterCageBlockEntity cage;
		try {
			LineGameTests.generator(helper, gen);
			for (int i = 1; i <= 2; i++)
				LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
			cage = cage(helper, new Supply(0), watch);
			helper.runAtTickTime(2, () -> LineGameTests.relink(helper, gen.east(1), gen.east(2)));
		} catch (RuntimeException e) {
			CMConfig.SERVER.cageFieldFe.set(cost);
			throw e;
		}
		long[] upAt = {-1};
		helper.onEachTick(() -> {
			if (upAt[0] < 0 && cage.isFieldUp())
				upAt[0] = helper.getTick();
		});
		helper.succeedWhen(() -> {
			if (upAt[0] < 0)
				helper.fail("the field isn't up on Power Lines (" + cage.getEnergyStored() + " FE stored)");
			if (helper.getTick() < upAt[0] + 60)
				helper.fail("up: watching it hold");
			try {
				if (!cage.isFieldUp() || watch.flips != 1)
					helper.fail("the field flipped " + watch.flips + " times on Power Lines");
			} finally {
				CMConfig.SERVER.cageFieldFe.set(cost);
			}
		});
	}

	// ---------------------------------------------------------------- helpers

	private static final int SUPPLY_TICKS = 20;

	/** An Industrial Battery, as it pushes: simulated first, up to its rate from what it holds; charged every tick. */
	private static final class Battery {
		final int capacity, charge, rate;
		int stored;

		Battery(int capacity, int stored, int charge, int rate) {
			this.capacity = capacity;
			this.stored = stored;
			this.charge = charge;
			this.rate = rate;
		}

		void tick(IEnergyStorage into) {
			stored = Math.min(capacity, stored + charge);
			int can = into.receiveEnergy(Math.min(stored, rate), true);
			if (can > 0)
				stored -= into.receiveEnergy(Math.min(can, stored), false);
		}
	}

	/** A generator, as the Lava Generator pushes: its whole store, for real, every tick; it makes so much a tick. */
	private static final class Generator {
		final int capacity, makes;
		int stored;
		long delivered;

		Generator(int capacity, int makes) {
			this.capacity = capacity;
			this.makes = makes;
		}

		void tick(IEnergyStorage into) {
			stored = Math.min(capacity, stored + makes);
			if (stored > 0) {
				int sent = into.receiveEnergy(stored, false);
				stored -= sent;
				delivered += sent;
			}
		}
	}

	private static int field(Object target, String name) {
		return (int) longField(target, name);
	}

	/** A player near the cage, so the countdown runs (it pauses with nobody near), noting what it's told. */
	private static FakePlayer listener(GameTestHelper helper, Watch watch) {
		FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "cage-listener")) {
			@Override
			public void sendSystemMessage(Component message) {
				watch.said.add(message);
			}

			@Override
			public void sendSystemMessage(Component message, boolean overlay) {
				if (!overlay)
					watch.said.add(message);
			}
		};
		Vec3 at = Vec3.atCenterOf(helper.absolutePos(CAGE.south(3)));
		player.moveTo(at.x, at.y, at.z);
		helper.getLevel().addNewPlayer(player);
		return player;
	}

	private static void cleanUp(GameTestHelper helper, FakePlayer listener, int warning) {
		CMConfig.SERVER.cageWarningSeconds.set(warning);
		if (!listener.isRemoved())
			helper.getLevel().removePlayerImmediately(listener, Entity.RemovalReason.DISCARDED);
	}

	private static long longField(Object target, String name) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return ((Number) field.get(target)).longValue();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
