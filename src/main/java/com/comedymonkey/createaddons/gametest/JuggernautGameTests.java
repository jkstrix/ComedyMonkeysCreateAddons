package com.comedymonkey.createaddons.gametest;

import java.util.List;
import java.util.Set;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.juggernaut.ClockworkJuggernaut;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlock;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlockEntity;
import com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlock;
import com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlockEntity;
import com.comedymonkey.createaddons.content.juggernaut.ruin.RuinBlocks;
import com.comedymonkey.createaddons.content.juggernaut.ruin.RuinBlocksProcessor;
import com.comedymonkey.createaddons.content.juggernaut.ruin.RuinField;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Clockwork Juggernaut's fight rules in a real world (./gradlew runGameTestServer): what hurts him and how much
 * (D14, D23, D48, D50, D51), his stages (D43), his death and drop (D30), that he never changes a block (D2), and that
 * every clip and sound the code names was exported.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JuggernautGameTests {
	/** 24 x 12 x 24 of air on a floor (the Battery tests' course): room for him. */
	private static final String COURSE = "battery_course";
	private static final BlockPos MIDDLE = new BlockPos(12, 2, 12);

	private JuggernautGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(JuggernautGameTests.class);
	}

	/** A Juggernaut at full health, wound, facing south (+Z), without waking him through finalizeSpawn. */
	private static ClockworkJuggernaut spawn(GameTestHelper helper) {
		ClockworkJuggernaut j = helper.spawn(ModEntities.CLOCKWORK_JUGGERNAUT.get(), MIDDLE);
		j.setYRot(0);
		j.setHealth(j.getMaxHealth());
		j.forcePhase(ClockworkJuggernaut.Phase.WOUND);
		return j;
	}

	/** A survival player standing {@code dz} blocks in front of him (negative: behind). */
	private static Player player(GameTestHelper helper, ClockworkJuggernaut j, double dz) {
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		p.setPos(j.getX(), j.getY(), j.getZ() + dz);
		return p;
	}

	private static float hit(ClockworkJuggernaut j, DamageSource source, float amount) {
		float before = j.getHealth();
		j.invulnerableTime = 0;
		j.hurt(source, amount);
		return before - j.getHealth();
	}

	private static void near(GameTestHelper helper, float actual, float expected, String what) {
		helper.assertTrue(Math.abs(actual - expected) < 0.01f, what + ": expected " + expected + ", got " + actual);
	}

	/** Wound he takes 10% (stage 1); rewinding, all of it, and half as much again on the key from behind (D14, D23). */
	@GameTest(template = COURSE)
	public static void woundHeShrugsItOffRewindingHeTakesItAll(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		Player front = player(helper, j, 6), behind = player(helper, j, -6);
		near(helper, hit(j, j.damageSources().playerAttack(front), 20), 2, "wound, from the front");
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		near(helper, hit(j, j.damageSources().playerAttack(front), 20), 20, "rewinding, from the front");
		near(helper, hit(j, j.damageSources().playerAttack(behind), 20), 30, "rewinding, on the key from behind");
		helper.succeed();
	}

	/** Arrows glance off him while he's wound, and land while he's open (D23). */
	@GameTest(template = COURSE)
	public static void arrowsGlanceOffUntilHeRewinds(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		Player archer = player(helper, j, 10);
		Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
		arrow.setOwner(archer);
		arrow.setPos(archer.position());
		near(helper, hit(j, j.damageSources().arrow(arrow, archer), 10), 0, "an arrow while wound");
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		near(helper, hit(j, j.damageSources().arrow(arrow, archer), 10), 10, "an arrow while rewinding");
		helper.succeed();
	}

	/** Nothing but players (and their arrows and pets) can hurt him: not falls, fire, lava, drowning, walls or Create (D50, D51). */
	@GameTest(template = COURSE)
	public static void onlyPlayersCanHurtHim(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND); // even wide open
		var sources = j.damageSources();
		var deployer = FakePlayerFactory.getMinecraft(helper.getLevel());
		List<DamageSource> harmless = List.of(sources.fall(), sources.onFire(), sources.lava(), sources.drown(), sources.inWall(), sources.generic(),
			sources.cactus(), sources.playerAttack(deployer), sources.mobAttack(helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2))));
		for (DamageSource source : harmless)
			near(helper, hit(j, source, 50), 0, source.getMsgId());
		helper.assertTrue(j.fireImmune(), "he should be fire immune");
		helper.succeed();
	}

	/** Three hits on the key in one rewind jam it, once: 3 s more open (D48). */
	@GameTest(template = COURSE)
	public static void threeKeyHitsJamTheKeyOnce(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		Player behind = player(helper, j, -6);
		for (int i = 0; i < 2; i++)
			hit(j, j.damageSources().playerAttack(behind), 1);
		helper.assertFalse(j.jammed(), "two hits shouldn't jam it");
		hit(j, j.damageSources().playerAttack(behind), 1);
		helper.assertTrue(j.jammed(), "the third hit on the key should jam it");
		helper.assertTrue(j.clip().name().equals("key_jam"), "he should play the jam, plays " + j.clip().name());
		helper.succeed();
	}

	/** Circling him faster than he turns doesn't keep him from attacking: he stomps the ring round him. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void circlingHimDoesntStopHisAttacks(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(16, 2, 12));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		double[] angle = { 0 };
		helper.onEachTick(() -> {
			angle[0] += Math.toRadians(14); // faster than his 9° a tick
			dummy.teleportTo(j.getX() + Math.cos(angle[0]) * 4.5, j.getY(), j.getZ() + Math.sin(angle[0]) * 4.5);
		});
		// not just one lucky stomp: he keeps attacking a target that keeps circling (stomps, whirls round to swing or grab)
		helper.succeedWhen(() -> helper.assertTrue(j.movesStarted() >= 3,
			"he should keep attacking a target circling him, has started " + j.movesStarted() + " moves"));
	}

	/** D14: three players make him twice as tough (every hit halved), the same as twice the health past vanilla's cap. */
	@GameTest(template = COURSE)
	public static void morePlayersMakeHimTougher(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.scaleForPlayers(3);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		near(helper, hit(j, j.damageSources().playerAttack(player(helper, j, 6)), 20), 10, "a hit with three players about");
		helper.succeed();
	}

	/** In the air he can't turn: he leaves the ground facing where he'll land and holds that heading all the way down. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void heCantTurnInTheAir(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		Pig pig = helper.spawn(EntityType.PIG, new BlockPos(18, 0, 20));
		pig.setInvulnerable(true);
		pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 10));
		j.forceLeap(pig);
		float[] yaw = { Float.NaN };
		helper.onEachTick(() -> {
			if (!j.isNoGravity())
				return;
			if (Float.isNaN(yaw[0])) {
				yaw[0] = j.getYRot();
				helper.assertTrue(j.inFront(pig, 25), "he should take off facing where he'll land");
			} else
				helper.assertTrue(Math.abs(Mth.wrapDegrees(j.getYRot() - yaw[0])) < 0.01f, "he turned in the air");
		});
		helper.succeedWhen(() -> helper.assertTrue(!Float.isNaN(yaw[0]) && !j.isNoGravity(), "he should have leapt and landed"));
	}

	/** D61: woken in his ruin, he bursts up through the beam over his head, leaving the stone beside it, and comes down fighting. */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void heBreaksOutOfHisRuin(GameTestHelper helper) {
		for (int x = 8; x <= 16; x++)
			helper.setBlock(new BlockPos(x, 9, 5), Blocks.DARK_OAK_LOG.defaultBlockState().setValue(
				net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
		helper.setBlock(new BlockPos(12, 5, 1), Blocks.STONE); // and a block of wall beside his way up, which he leaves be
		ClockworkJuggernaut j = helper.spawn(ModEntities.CLOCKWORK_JUGGERNAUT.get(), new BlockPos(12, 0, 5));
		j.setYRot(0);
		j.wakeFromRuin();
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(12, 9, 5));
			helper.assertBlockPresent(Blocks.STONE, new BlockPos(12, 5, 1));
			// the test arena's barrier ceiling (about 12 up) is no way over: he comes back down where he knelt, roof gone
			helper.assertTrue(!j.isNoGravity() && j.phase() == ClockworkJuggernaut.Phase.WOUND && j.getY() < helper.absolutePos(BlockPos.ZERO).getY() + 1, "he should be back on his feet, fighting");
		});
	}

	/** The real ruin's hall: its Juggernaut kneels there, dormant, marked to break out when he wakes (D31, D61). */
	@GameTest(template = COURSE)
	public static void theRuinsJuggernautKneelsReadyToBreakOut(GameTestHelper helper) {
		var template = helper.getLevel().getStructureManager().getOrCreate(ComedyMonkeysCreateAddons.asResource("juggernaut_ruin"));
		BlockPos at = helper.absolutePos(new BlockPos(4, 0, 4));
		template.placeInWorld(helper.getLevel(), at, at, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
			helper.getLevel().getRandom(), 2);
		List<ClockworkJuggernaut> found = juggernauts(helper);
		helper.assertTrue(found.size() == 1, "the hall should hold one Juggernaut, holds " + found.size());
		helper.assertTrue(found.get(0).phase() == ClockworkJuggernaut.Phase.DORMANT, "he should kneel dormant, is " + found.get(0).phase());
		helper.assertTrue(found.get(0).fromRuin(), "he should break out when he wakes");
		helper.succeed();
	}

	/** D61 in the real hall, hemmed in by its walls: a hit wakes him and he still bursts up through the beam over his head. */
	@GameTest(template = COURSE, timeoutTicks = 240)
	public static void theRuinsJuggernautBurstsThroughItsRoof(GameTestHelper helper) {
		var template = helper.getLevel().getStructureManager().getOrCreate(ComedyMonkeysCreateAddons.asResource("juggernaut_ruin"));
		BlockPos at = helper.absolutePos(new BlockPos(4, 0, 4));
		template.placeInWorld(helper.getLevel(), at, at, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
			helper.getLevel().getRandom(), 2);
		BlockPos beam = new BlockPos(11, 10, 9); // the hall's front beam, over his head
		helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.DARK_OAK_LOG, beam);
		ClockworkJuggernaut j = juggernauts(helper).get(0);
		Player waker = player(helper, j, 8);
		j.hurt(j.damageSources().playerAttack(waker), 1);
		helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.AIR, beam));
	}

	/** D62: in his own ruin, its walls don't stop him: he walks through a wall of its brick in his way, and it crumbles. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void heWalksThroughHisRuinsWalls(GameTestHelper helper) {
		var template = helper.getLevel().getStructureManager().getOrCreate(ComedyMonkeysCreateAddons.asResource("juggernaut_ruin"));
		BlockPos at = helper.absolutePos(new BlockPos(4, 0, 4));
		template.placeInWorld(helper.getLevel(), at, at, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
			helper.getLevel().getRandom(), 2);
		// a wall of the ruin's brick across the hall, between him and the doorway, and a stone pillar beside it he leaves be
		for (int x = 6; x <= 16; x++)
			for (int y = 2; y <= 5; y++)
				helper.setBlock(new BlockPos(x, y, 7), Blocks.DEEPSLATE_BRICKS);
		helper.setBlock(new BlockPos(6, 6, 7), Blocks.STONE);
		ClockworkJuggernaut j = juggernauts(helper).get(0);
		j.forcePhase(ClockworkJuggernaut.Phase.WOUND);
		// placed by hand, the hall is no structure in the world: its box stands in for the ruin's pieces
		j.testRuinPieces(template.getBoundingBox(new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), at));
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(11, 1, 1));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(11, 3, 7));
			helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 6, 7));
		});
	}

	/** D2: one that wasn't found in a ruin (summoned, an egg) never crumbles anything, ruin brick included. */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void aSummonedOneNeverCrumblesBrick(GameTestHelper helper) {
		for (int x = 6; x <= 18; x++)
			for (int y = 0; y <= 3; y++)
				helper.setBlock(new BlockPos(x, y, 17), Blocks.DEEPSLATE_BRICKS);
		ClockworkJuggernaut j = spawn(helper);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 0, 21));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.runAfterDelay(140, () -> {
			for (int x = 6; x <= 18; x++)
				for (int y = 0; y <= 3; y++)
					helper.assertBlockPresent(Blocks.DEEPSLATE_BRICKS, new BlockPos(x, y, 17));
			helper.succeed();
		});
	}

	/**
	 * JUG-01, D62: one found in a ruin still leaves a player's wall of its very brick standing, when it's no part of the
	 * ruin's pieces (here there's no ruin structure at all): he only crumbles what the ruin built.
	 */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void aRuinOneLeavesAPlayersWallStanding(GameTestHelper helper) {
		for (int x = 6; x <= 18; x++)
			for (int y = 2; y <= 5; y++)
				helper.setBlock(new BlockPos(x, y, 17), Blocks.DEEPSLATE_BRICKS);
		ClockworkJuggernaut j = spawn(helper);
		j.setRuinHome(j.blockPosition());
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 2, 21));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.runAfterDelay(140, () -> {
			for (int x = 6; x <= 18; x++)
				for (int y = 2; y <= 5; y++)
					helper.assertBlockPresent(Blocks.DEEPSLATE_BRICKS, new BlockPos(x, y, 17));
			helper.succeed();
		});
	}

	/** The real ruin check (JUG-01): the field builds on scattered spots of its own, none in the hall's square or past its edge. */
	@GameTest(template = COURSE)
	public static void theRuinFieldKnowsWhereItBuilt(GameTestHelper helper) {
		BlockPos centre = new BlockPos(1000, 64, 1000);
		RuinField field = new RuinField(centre, 40, 12345L, false);
		int built = 0, spots = 0;
		for (int x = -40; x <= 40; x++)
			for (int z = -40; z <= 40; z++) {
				spots++;
				if (field.height(centre.getX() + x, centre.getZ() + z) > 0) {
					built++;
					helper.assertTrue(Math.max(Math.abs(x), Math.abs(z)) > 8, "the hall's square is the hall's, not the field's: " + x + ", " + z);
					helper.assertTrue(x * x + z * z <= 40 * 40, "nothing past the field's edge: " + x + ", " + z);
				}
			}
		helper.assertTrue(built > 0 && built < spots / 4, "the field should build on its own scattered spots, builds on " + built + " of " + spots);
		helper.succeed();
	}

	/**
	 * MOB-R03, R04: the field claims a block only as part of one of its columns, standing on the ground no higher than
	 * that column was built, however far above or below the hall: a player's block of the same brick on top of a wall,
	 * floating over it or standing apart is theirs.
	 */
	@GameTest(template = COURSE)
	public static void theRuinClaimsOnlyItsOwnColumns(GameTestHelper helper) {
		BlockPos ground = helper.absolutePos(new BlockPos(12, 1, 12));
		// a field laid, its hall 60 blocks below, so one of its columns (3 or more high, on bare ground) stands on this spot
		RuinField field = fieldWithColumnAt(helper, ground, false, 3);
		int top = field.height(ground.getX(), ground.getZ());
		helper.setBlock(new BlockPos(12, 1, 12), Blocks.STONE);
		for (int h = 0; h < top; h++)
			helper.setBlock(new BlockPos(12, 2 + h, 12), h % 2 == 0 ? Blocks.DEEPSLATE_BRICKS : Blocks.COBBLED_DEEPSLATE);
		helper.setBlock(new BlockPos(12, 2 + top, 12), Blocks.DEEPSLATE_BRICKS); // a player's, on top of the wall
		helper.setBlock(new BlockPos(12, 4 + top, 12), Blocks.DEEPSLATE_BRICKS); // and one floating over it
		for (int h = 0; h < top; h++)
			helper.assertTrue(field.built(helper.getLevel(), helper.absolutePos(new BlockPos(12, 2 + h, 12))), "the wall's own block " + h + " up");
		helper.assertFalse(field.built(helper.getLevel(), helper.absolutePos(new BlockPos(12, 2 + top, 12))), "a block on top of the wall");
		helper.assertFalse(field.built(helper.getLevel(), helper.absolutePos(new BlockPos(12, 4 + top, 12))), "a block floating over it");
		// a wall knocked out from under is no longer standing, so no longer the ruin's: why he brings a column down whole
		helper.setBlock(new BlockPos(12, 2, 12), Blocks.AIR);
		helper.assertFalse(field.built(helper.getLevel(), helper.absolutePos(new BlockPos(12, 4, 12))), "what's left floating over a gap");
		// a player's block standing apart, on a spot the field built nothing on
		for (int x = 1; x < 23; x++) {
			BlockPos spot = helper.absolutePos(new BlockPos(x, 2, 3));
			if (field.height(spot.getX(), spot.getZ()) > 0)
				continue;
			helper.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 2, 3), Blocks.DEEPSLATE_BRICKS);
			helper.assertFalse(field.built(helper.getLevel(), spot), "a block where the field built nothing");
			break;
		}
		helper.succeed();
	}

	/**
	 * A field whose column (at least {@code minTop} high, not on its paving) stands at {@code spot}'s x and z, its hall 60
	 * blocks below. A field's columns move with it, but whether it paved a spot is hashed from the spot's world position,
	 * which every candidate column shares and which changes with wherever the test lands: so a spot may be paved under
	 * all of one seed's columns. The next seed hashes it afresh (32 seeds all paving it: about one in 10^16).
	 */
	private static RuinField fieldWithColumnAt(GameTestHelper helper, BlockPos spot, boolean end, int minTop) {
		for (long seed = 777L; seed < 777L + 32; seed++) {
			RuinField probe = new RuinField(BlockPos.ZERO, 40, seed, end);
			for (int dx = -40; dx <= 40; dx++)
				for (int dz = -40; dz <= 40; dz++) {
					if (probe.height(dx, dz) < minTop)
						continue;
					RuinField f = new RuinField(new BlockPos(spot.getX() - dx, spot.getY() - 60, spot.getZ() - dz), 40, seed, end);
					if (f.height(spot.getX(), spot.getZ()) >= minTop && !f.paved(spot.getX(), spot.getZ()))
						return f;
				}
		}
		throw new net.minecraft.gametest.framework.GameTestAssertException("no column to try");
	}

	/**
	 * The column tests above stand a column on a spot wherever the test lands: the search finds one at any world position
	 * (it used to fail now and then, at spots its one seed had paved under every column).
	 */
	@GameTest(template = COURSE)
	public static void aRuinColumnCanBeFoundAnywhere(GameTestHelper helper) {
		java.util.Random r = new java.util.Random(4242L);
		for (int i = 0; i < 64; i++) {
			BlockPos spot = new BlockPos(r.nextInt(2_000_000) - 1_000_000, 2, r.nextInt(2_000_000) - 1_000_000);
			fieldWithColumnAt(helper, spot, i % 2 == 0, 3);
		}
		helper.succeed();
	}

	/**
	 * The user's "he gets stuck on his ruins": in a ruin built before it had its own blocks, he walks into one of its
	 * field's walls and the whole of it above his feet comes down, not just its bottom block (which left the rest
	 * floating, no longer standing on the ground, so no longer the ruin's, and in his way for good).
	 */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void heBringsDownAWholeFieldWall(GameTestHelper helper) {
		RuinField field = fieldWithColumnAt(helper, helper.absolutePos(new BlockPos(12, 2, 17)), false, 4);
		int top = field.height(helper.absolutePos(new BlockPos(12, 2, 17)).getX(), helper.absolutePos(new BlockPos(12, 2, 17)).getZ());
		helper.setBlock(new BlockPos(12, 1, 17), Blocks.STONE);
		for (int h = 0; h < top; h++)
			helper.setBlock(new BlockPos(12, 2 + h, 17), h % 2 == 0 ? Blocks.DEEPSLATE_BRICKS : Blocks.COBBLED_DEEPSLATE);
		ClockworkJuggernaut j = spawn(helper);
		j.setRuinHome(j.blockPosition());
		j.testRuinField(field);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 2, 22));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.succeedWhen(() -> {
			for (int h = 1; h < top; h++)
				helper.assertBlockPresent(Blocks.AIR, new BlockPos(12, 2 + h, 17));
		});
	}

	/**
	 * A ruin's own blocks (RuinBlocks) crumble before any Juggernaut, summoned or found, wherever they are; its brass and
	 * its chest never do (the user: "he shouldn't be able to destroy the chests or brass").
	 */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void anyJuggernautCrumblesARuinsOwnBlocksButNotItsBrass(GameTestHelper helper) {
		// up to the course's ceiling, so he can't leap over it: through it is the only way
		for (int x = 0; x <= 23; x++)
			for (int y = 0; y <= 11; y++)
				helper.setBlock(new BlockPos(x, y, 17), (y % 2 == 0 ? RuinBlocks.DEEPSLATE_BRICKS : RuinBlocks.END_STONE).get());
		// in the wall a step up from his feet (he stands at y 0 here), where he'd crumble them if he crumbled anything
		BlockState brass = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "brass_block")).defaultBlockState();
		helper.setBlock(new BlockPos(10, 1, 17), brass);
		helper.setBlock(new BlockPos(14, 1, 17), Blocks.CHEST);
		ClockworkJuggernaut j = spawn(helper);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 2, 22));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.succeedWhen(() -> {
			for (int y = 1; y <= 6; y++)
				helper.assertBlockPresent(Blocks.AIR, new BlockPos(12, y, 17));
			helper.assertBlockState(new BlockPos(10, 1, 17), s -> s.is(brass.getBlock()), () -> "the brass should stand");
			helper.assertBlockPresent(Blocks.CHEST, new BlockPos(14, 1, 17));
		});
	}

	/** The hall is built of the ruin's own blocks, but for its floor (the ground), its chest and its brass. */
	@GameTest(template = COURSE)
	public static void theHallIsBuiltOfTheRuinsOwnBlocks(GameTestHelper helper) {
		var template = helper.getLevel().getStructureManager().getOrCreate(ComedyMonkeysCreateAddons.asResource("juggernaut_ruin"));
		BlockPos at = helper.absolutePos(new BlockPos(4, 0, 4));
		var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().addProcessor(RuinBlocksProcessor.INSTANCE);
		template.placeInWorld(helper.getLevel(), at, at, settings, helper.getLevel().getRandom(), 2);
		int own = 0, chests = 0, brass = 0;
		for (BlockPos p : BlockPos.betweenClosed(at, at.offset(template.getSize()).offset(-1, -1, -1))) {
			BlockState s = helper.getLevel().getBlockState(p);
			if (p.getY() == at.getY())
				helper.assertFalse(s.is(ClockworkJuggernaut.CRUMBLES), "the floor is the ground, not his to crumble: " + p);
			else {
				if (s.is(ClockworkJuggernaut.CRUMBLES))
					own++;
				helper.assertFalse(RuinBlocks.ruin(s) != s, "a vanilla block the ruin has its own of, above the floor: " + s + " at " + p);
			}
			chests += s.is(Blocks.CHEST) ? 1 : 0;
			brass += BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().equals("brass_block") ? 1 : 0;
		}
		helper.assertTrue(own > 200, "the hall should be built of the ruin's own blocks, has " + own);
		helper.assertTrue(chests == 1 && brass == 1, "its chest and brass stay as they are: " + chests + " chests, " + brass + " brass");
		helper.succeed();
	}

	/** Each of the ruin's own blocks is its vanilla block to a player: named as it, dropping it, crumbled by him. */
	@GameTest(template = COURSE)
	public static void theRuinsOwnBlocksAreTheirVanillaBlocks(GameTestHelper helper) {
		int n = 0;
		for (var entry : ModBlocks.BLOCKS.getEntries())
			if (entry.get() instanceof RuinBlocks.Ruin ruin) {
				n++;
				net.minecraft.world.level.block.Block copy = entry.get(), original = ruin.original();
				helper.assertTrue(copy.getLootTable().equals(original.getLootTable()), entry.getId() + " should drop as " + original);
				helper.assertTrue(copy.getDescriptionId().equals(original.getDescriptionId()), entry.getId() + " should be named as " + original);
				helper.assertTrue(copy.defaultBlockState().is(ClockworkJuggernaut.CRUMBLES), entry.getId() + " should crumble before him");
				helper.assertTrue(RuinBlocks.ruin(original.defaultBlockState()).is(copy), "the ruin should build " + original + " as " + entry.getId());
			}
		helper.assertTrue(n == 14, "14 ruin blocks, found " + n);
		helper.succeed();
	}

	/** In an End ruin built before it had its own blocks, a column's end stone is the ruin's; the island under it isn't. */
	@GameTest(template = COURSE)
	public static void anEndColumnStandsOnTheIsland(GameTestHelper helper) {
		BlockPos spot = helper.absolutePos(new BlockPos(12, 2, 12));
		RuinField field = fieldWithColumnAt(helper, spot, true, 3);
		int top = field.height(spot.getX(), spot.getZ());
		helper.setBlock(new BlockPos(12, 1, 12), Blocks.END_STONE);
		for (int h = 0; h <= top; h++)
			helper.setBlock(new BlockPos(12, 2 + h, 12), h % 2 == 0 ? Blocks.END_STONE : Blocks.END_STONE_BRICKS);
		for (int h = 0; h < top; h++)
			helper.assertTrue(field.builtOn(helper.getLevel(), spot.above(h), spot.getY()), "the column's block " + h + " up");
		helper.assertFalse(field.builtOn(helper.getLevel(), spot.above(top), spot.getY()), "end stone on top of the column");
		helper.assertFalse(field.builtOn(helper.getLevel(), spot.below(), spot.getY()), "the island under it");
		helper.succeed();
	}

	/**
	 * The user's "he shouldn't be able to be pinned or trapped": walled in with them out of reach, after a while going
	 * nowhere he comes out of steam beside them.
	 */
	@GameTest(template = COURSE, timeoutTicks = 360)
	public static void aTrappedOneComesToThem(GameTestHelper helper) {
		for (int i = 8; i <= 16; i++)
			for (int y = 1; y <= 11; y++) {
				helper.setBlock(new BlockPos(i, y, 8), Blocks.OBSIDIAN);
				helper.setBlock(new BlockPos(i, y, 16), Blocks.OBSIDIAN);
				helper.setBlock(new BlockPos(8, y, i), Blocks.OBSIDIAN);
				helper.setBlock(new BlockPos(16, y, i), Blocks.OBSIDIAN);
			}
		ClockworkJuggernaut j = spawn(helper);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(3, 2, 3));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getTick() >= ClockworkJuggernaut.TRAPPED_TICKS, "not before he's been trapped a while");
			BlockPos at = helper.relativePos(j.blockPosition());
			helper.assertTrue(at.getX() < 8 || at.getZ() < 8, "he should be out of the box, is at " + at);
			helper.assertTrue(j.distanceTo(dummy) < 13, "he should be beside them, is " + j.distanceTo(dummy) + " off");
		});
	}

	/** The user's "he ends up floating if he is on a hill": held up by one corner on a ledge, he slides off and comes down. */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void heSlidesOffALedgeHeCaughtACornerOn(GameTestHelper helper) {
		for (int x = 9; x <= 10; x++)
			for (int z = 9; z <= 10; z++)
				for (int y = 0; y <= 2; y++)
					helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
		ClockworkJuggernaut j = spawn(helper);
		BlockPos at = helper.absolutePos(new BlockPos(12, 3, 12));
		j.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		helper.succeedWhen(() -> helper.assertTrue(j.onGround() && j.getY() < at.getY() - 2.5,
			"he should come down off the ledge, is at " + helper.relativeVec(j.position())));
	}

	/** Half on a step he could walk up, he stands: the slide is only for being held up over thin air. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void heStandsAStepHalfUnderHim(GameTestHelper helper) {
		for (int x = 0; x <= 12; x++)
			for (int z = 0; z <= 23; z++)
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		ClockworkJuggernaut j = spawn(helper);
		BlockPos at = helper.absolutePos(new BlockPos(12, 1, 12));
		j.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		helper.runAfterDelay(100, () -> {
			helper.assertTrue(Math.abs(j.getX() - (at.getX() + 0.5)) < 0.1 && j.getY() >= at.getY() - 0.01,
				"he should stand where he was, is at " + helper.relativeVec(j.position()));
			helper.succeed();
		});
	}

	/**
	 * Snow terraces falling south (+Z) a block a block from a 6-high plateau to the floor, or with {@code cliff} the
	 * plateau running on to z = 12 and dropping straight to the floor.
	 */
	private static void terraces(GameTestHelper helper, boolean cliff) {
		for (int z = 0; z <= 23; z++) {
			int h = cliff ? (z <= 12 ? 6 : 0) : Mth.clamp(14 - z, 0, 6);
			for (int x = 0; x <= 23; x++)
				for (int y = 0; y < h; y++)
					helper.setBlock(new BlockPos(x, y, z), Blocks.SNOW_BLOCK);
		}
	}

	/** Him, still, centred at {@code (12.5, y, z)} facing south. */
	private static ClockworkJuggernaut standAt(GameTestHelper helper, double y, double z) {
		ClockworkJuggernaut j = spawn(helper);
		BlockPos at = helper.absolutePos(new BlockPos(12, 0, 0));
		j.moveTo(at.getX() + 0.5, at.getY() + y, at.getZ() + z, 0, 0);
		return j;
	}

	/**
	 * The user's "the juggernaut is getting stuck on this terrain" (snowy terraces): stood on the top step of a slope
	 * that falls a block a block under his wide footprint, the ground past his edge is a step down from the ground
	 * under it, not a cliff from his feet; a real cliff there still stops him (D56).
	 */
	@GameTest(template = COURSE)
	public static void aSlopeIsGroundButACliffIsnt(GameTestHelper helper) {
		terraces(helper, false);
		ClockworkJuggernaut j = standAt(helper, 6, 9.5);
		helper.assertTrue(j.safeAhead(new Vec3(0, 0, 1), 1), "a block down the slope should be walkable");
		helper.assertTrue(j.safeAhead(new Vec3(0, 0, 1), 6), "six blocks down the slope should be walkable");
		ClockworkJuggernaut up = standAt(helper, 0, 20.5);
		helper.assertTrue(up.safeAhead(new Vec3(0, 0, -1), 6), "six blocks up the slope should be walkable");
		j.discard();
		up.discard();
		for (int z = 0; z <= 23; z++)
			for (int x = 0; x <= 23; x++)
				for (int y = 0; y < 6; y++)
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
		terraces(helper, true);
		ClockworkJuggernaut edge = standAt(helper, 6, 9.5);
		helper.assertFalse(edge.safeAhead(new Vec3(0, 0, 1), 1), "a 6-block drop past his edge should stop him");
		ClockworkJuggernaut foot = standAt(helper, 0, 16.5);
		helper.assertFalse(foot.safeAhead(new Vec3(0, 0, -1), 1), "a 6-block wall past his edge should stop him");
		helper.succeed();
	}

	/** Down terraced snow to them, and (below) up it to them: he gets there, as on flat ground. */
	@GameTest(template = COURSE, timeoutTicks = 400)
	public static void heWalksDownTerracedSnow(GameTestHelper helper) {
		terraces(helper, false);
		ClockworkJuggernaut j = standAt(helper, 6, 4.5);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 0, 21));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.succeedWhen(() -> helper.assertTrue(j.horizontalDistance(dummy) < 7,
			"he should have come within 7 blocks, is " + String.format("%.1f", j.horizontalDistance(dummy)) + " away at "
				+ helper.relativeVec(j.position()) + ", " + j.clip().name()));
	}

	@GameTest(template = COURSE, timeoutTicks = 400)
	public static void heWalksUpTerracedSnow(GameTestHelper helper) {
		terraces(helper, false);
		ClockworkJuggernaut j = standAt(helper, 0, 20.5);
		j.setYRot(180);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 6, 2));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.succeedWhen(() -> helper.assertTrue(j.horizontalDistance(dummy) < 7,
			"he should have come within 7 blocks, is " + String.format("%.1f", j.horizontalDistance(dummy)) + " away at "
				+ helper.relativeVec(j.position()) + ", " + j.clip().name()));
	}

	/**
	 * The user's "he locks on one player and it makes it easy to cheese him": whoever's hurting him (from behind most of
	 * all) is who he goes after next; with nobody hurting him, who it is varies.
	 */
	@GameTest(template = COURSE)
	public static void heGoesAfterWhoeverIsHurtingHim(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		Player front = player(helper, j, 6), behind = player(helper, j, -6);
		List<Player> both = List.of(front, behind);
		int frontFirst = 0;
		for (int i = 0; i < 200; i++)
			frontFirst += j.nextTargetOf(both) == front ? 1 : 0;
		helper.assertTrue(frontFirst > 20 && frontFirst < 180, "with nobody hurting him, either should do: front picked " + frontFirst + "/200");
		for (int i = 0; i < 3; i++)
			hit(j, j.damageSources().playerAttack(behind), 10);
		for (int i = 0; i < 50; i++)
			helper.assertTrue(j.nextTargetOf(both) == behind, "the one hammering his back should draw him");
		helper.succeed();
	}

	/** The user's "be a bit more random in his attacks": now and then a move from the next distance band over. */
	@GameTest(template = COURSE)
	public static void hisMovesSpillOverTheirDistanceBands(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		// 7 blocks off, in front: the middle band (charge, stomp), with the close band's swing and grab still able to land
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 2, 19));
		java.util.Map<String, Integer> seen = new java.util.HashMap<>();
		for (int i = 0; i < 400; i++)
			seen.merge(String.valueOf(j.pickFor(dummy)), 1, Integer::sum);
		helper.assertTrue(seen.getOrDefault("CHARGE", 0) > 100, "mostly the middle band's moves: " + seen);
		helper.assertTrue(seen.getOrDefault("SWING", 0) + seen.getOrDefault("GRAB", 0) > 10, "now and then a close one: " + seen);
		helper.succeed();
	}

	/** MOB-R01: saved in the moment between his death and his going, he loads dead: the Mainspring, loot and XP come once. */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void aSaveAfterHisDeathPaysOutOnce(GameTestHelper helper) {
		listenForDrops();
		ClockworkJuggernaut j = spawn(helper);
		j.forceStage(3);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		hit(j, j.damageSources().playerAttack(player(helper, j, 6)), 10000);
		ClockworkJuggernaut[] loaded = new ClockworkJuggernaut[1];
		long[] at = { 0 };
		helper.onEachTick(() -> {
			if (loaded[0] != null || !j.isDeadOrDying() || j.isRemoved())
				return;
			// died this tick and not gone yet: the save the world might make now
			CompoundTag tag = new CompoundTag();
			j.saveWithoutId(tag);
			j.discard();
			loaded[0] = ModEntities.CLOCKWORK_JUGGERNAUT.get().create(helper.getLevel());
			loaded[0].load(tag);
			helper.getLevel().addFreshEntity(loaded[0]);
			at[0] = helper.getTick();
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(loaded[0] != null && helper.getTick() > at[0] + 110, "waiting out a second death clip");
			helper.assertTrue(loaded[0].isRemoved(), "he should stay dead and go");
			List<ItemEntity> springs = helper.getLevel().getEntitiesOfClass(ItemEntity.class, loaded[0].getBoundingBox().inflate(6),
				e -> e.getItem().is(ModItems.MAINSPRING.get()));
			helper.assertTrue(springs.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1, "exactly one Mainspring should drop");
			int xp = XP_DROPS.getOrDefault(j.getUUID(), 0);
			helper.assertTrue(xp == 1, "his XP should drop once, dropped " + xp + " times");
		});
	}

	/** Times each Juggernaut's XP has dropped. */
	private static final java.util.Map<java.util.UUID, Integer> XP_DROPS = new java.util.concurrent.ConcurrentHashMap<>();
	private static boolean listeningForDrops;

	private static synchronized void listenForDrops() {
		if (listeningForDrops)
			return;
		listeningForDrops = true;
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent.class, e -> {
			if (e.getEntity() instanceof ClockworkJuggernaut j)
				XP_DROPS.merge(j.getUUID(), 1, Integer::sum);
		});
	}

	/** MOB-R02: a shove that comes as speed (a train running him over, a slime block's bounce) doesn't carry him off either. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void aThrowDoesntMoveHim(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		double[] at = new double[2];
		helper.runAfterDelay(2, j::wakeFromRuin); // getting up: no step of his stops him dead, as standing wound does
		helper.runAfterDelay(6, () -> {
			at[0] = j.getX();
			at[1] = j.getZ();
			j.setDeltaMovement(3, 0, 0); // what Create's train collision hands what it runs over
		});
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(j.phase() == ClockworkJuggernaut.Phase.WAKING, "he should still be getting up, is " + j.phase());
			helper.assertTrue(Math.abs(j.getX() - at[0]) < 1e-3 && Math.abs(j.getZ() - at[1]) < 1e-3,
				"he should be where he stood, is " + String.format("%.2f", j.getX() - at[0]) + " over");
			helper.succeed();
		});
	}

	/** WARN-05: the ruin stands on natural ground, never on water, a tree, plants, thin snow or air. */
	@GameTest(template = COURSE)
	public static void theRuinStandsOnNaturalGround(GameTestHelper helper) {
		List<net.minecraft.world.level.block.Block> ground = List.of(Blocks.STONE, Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SAND, Blocks.GRAVEL,
			Blocks.DEEPSLATE, Blocks.END_STONE, Blocks.SNOW_BLOCK, Blocks.MOSS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.TERRACOTTA);
		List<net.minecraft.world.level.block.Block> not = List.of(Blocks.WATER, Blocks.OAK_LEAVES, Blocks.OAK_LOG, Blocks.SHORT_GRASS, Blocks.SNOW,
			Blocks.AIR, Blocks.POPPY);
		int x = 1;
		for (var block : ground) {
			BlockPos p = new BlockPos(x++, 2, 2);
			helper.setBlock(p, block);
			helper.assertTrue(com.comedymonkey.createaddons.content.juggernaut.ruin.RuinField.ground(helper.getLevel(), helper.absolutePos(p)),
				"the ruin should stand on " + block);
		}
		x = 1;
		for (var block : not) {
			BlockPos p = new BlockPos(x++, 2, 5);
			helper.setBlock(p, block);
			helper.assertFalse(com.comedymonkey.createaddons.content.juggernaut.ruin.RuinField.ground(helper.getLevel(), helper.absolutePos(p)),
				"the ruin shouldn't stand on " + block);
		}
		helper.succeed();
	}

	/** JUG-04, D20: a shove from outside (a contraption sets his position) is undone the next step while he stands. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void aShoveDoesntMoveHim(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		double[] at = new double[2];
		helper.runAfterDelay(10, () -> {
			at[0] = j.getX();
			at[1] = j.getZ();
			j.setPos(j.getX() + 1, j.getY(), j.getZ()); // what Create's ContraptionCollider does
		});
		helper.runAfterDelay(11, () -> {
			helper.assertTrue(Math.abs(j.getX() - at[0]) < 1e-3 && Math.abs(j.getZ() - at[1]) < 1e-3,
				"he should be back where he stood, is " + String.format("%.2f", j.getX() - at[0]) + " over");
			helper.succeed();
		});
	}

	/** JUG-04: holding his ground never holds back his own moves: a charge still carries him on. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aChargeStillMovesHim(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 2, 22));
		dummy.setInvulnerable(true);
		double[] from = { Double.NaN };
		helper.runAfterDelay(5, () -> {
			from[0] = j.getZ();
			j.forceCharge(dummy);
		});
		helper.runAfterDelay(45, () -> {
			helper.assertTrue(j.getZ() - from[0] > 3, "the charge should carry him on, moved " + String.format("%.2f", j.getZ() - from[0]));
			helper.succeed();
		});
	}

	/** A player who struck him, with their death recorded by what the kill credits them with. */
	private static Player killer(GameTestHelper helper, ClockworkJuggernaut j, List<String> seen) {
		Player p = new Player(helper.getLevel(), BlockPos.ZERO, 0, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-killer")) {
			@Override
			public boolean isSpectator() {
				return false;
			}

			@Override
			public boolean isCreative() {
				return false;
			}

			@Override
			public boolean killedEntity(ServerLevel level, net.minecraft.world.entity.LivingEntity entity) {
				if (entity == j)
					seen.add("blow"); // the death's source was this player's blow
				return super.killedEntity(level, entity);
			}

			@Override
			public void awardKillScore(net.minecraft.world.entity.Entity killed, int score, DamageSource source) {
				if (killed == j)
					seen.add("credit"); // what fires player_killed_entity ("Run Down") for a real player
				super.awardKillScore(killed, score, source);
			}
		};
		p.setPos(j.getX(), j.getY(), j.getZ() + 6);
		return p;
	}

	/** JUG-03, JUG-07: he dies of the player's own killing blow at the end of his clip, and the kill is credited to them. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void heDiesOfTheKillingBlow(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.forceStage(3);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		List<String> seen = new java.util.concurrent.CopyOnWriteArrayList<>();
		hit(j, j.damageSources().playerAttack(killer(helper, j, seen)), 10000);
		helper.succeedWhen(() -> {
			helper.assertTrue(j.isRemoved(), "he should be gone after his death clip");
			helper.assertTrue(seen.contains("blow"), "he should die of the player's blow, not a generic one");
			helper.assertTrue(seen.contains("credit"), "the player should get the kill");
		});
	}

	/** JUG-03: saved in his death clip (a quit, a server stop) and loaded again, he still drops the Mainspring. */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void aReloadInHisDeathStillDropsTheMainspring(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.forceStage(3);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		hit(j, j.damageSources().playerAttack(player(helper, j, 6)), 10000);
		helper.assertTrue(j.phase() == ClockworkJuggernaut.Phase.DYING, "he should be dying, is " + j.phase());
		ClockworkJuggernaut[] loaded = new ClockworkJuggernaut[1];
		helper.runAfterDelay(20, () -> {
			CompoundTag tag = new CompoundTag();
			j.saveWithoutId(tag);
			j.discard();
			loaded[0] = ModEntities.CLOCKWORK_JUGGERNAUT.get().create(helper.getLevel());
			loaded[0].load(tag);
			helper.getLevel().addFreshEntity(loaded[0]);
			helper.assertTrue(loaded[0].phase() == ClockworkJuggernaut.Phase.DYING, "he should load still dying, is " + loaded[0].phase());
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(loaded[0] != null && loaded[0].isRemoved(), "he should finish dying after the reload");
			List<ItemEntity> springs = helper.getLevel().getEntitiesOfClass(ItemEntity.class, loaded[0].getBoundingBox().inflate(4),
				e -> e.getItem().is(ModItems.MAINSPRING.get()));
			helper.assertTrue(springs.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1, "the Mainspring should still drop");
		});
	}

	/** JUG-06, D27: villagers, livestock and pets that never touched him are no fair game; whatever hits him is. */
	@GameTest(template = COURSE)
	public static void heSparesBystanders(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		Player someone = player(helper, j, 8);
		net.minecraft.world.entity.npc.Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(14, 2, 14));
		Pig pig = helper.spawn(EntityType.PIG, new BlockPos(10, 2, 14));
		net.minecraft.world.entity.animal.Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(14, 2, 10));
		wolf.tame(someone);
		net.minecraft.world.entity.animal.IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(10, 2, 10));
		helper.assertTrue(j.fairGame(someone), "a survival player is fair game");
		for (var bystander : List.of(villager, pig, wolf, golem))
			helper.assertFalse(j.fairGame(bystander), bystander.getType() + " never touched him: no fair game");
		j.hurt(j.damageSources().mobAttack(wolf), 4);
		j.hurt(j.damageSources().mobAttack(golem), 4);
		helper.assertTrue(j.fairGame(wolf), "a pet that bit him is fair game");
		helper.assertTrue(j.fairGame(golem), "a golem that hit him is fair game");
		helper.assertFalse(j.fairGame(villager), "the villager still isn't");
		helper.succeed();
	}

	/** JUG-06: his stomp (the leap's landing) leaves a villager standing beside his target unhurt. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void hisLandingSparesAVillager(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		Pig pig = helper.spawn(EntityType.PIG, new BlockPos(12, 0, 20));
		pig.setInvulnerable(true);
		pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 10));
		net.minecraft.world.entity.npc.Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(14, 0, 20));
		villager.setNoAi(true);
		j.forceLeap(pig);
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.getTicksFrozen() > 0, "the landing should come down on the pig");
			helper.assertTrue(villager.getHealth() == villager.getMaxHealth() && villager.getTicksFrozen() == 0,
				"the villager by it should be untouched, health " + villager.getHealth());
		});
	}

	/** JUG-06: his cog flies through a villager in its way and hits only what he may hit. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void hisCogPassesThroughBystanders(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		net.minecraft.world.entity.npc.Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(4, 2, 4));
		villager.setNoAi(true);
		Pig target = helper.spawn(EntityType.PIG, new BlockPos(20, 2, 4));
		target.setNoAi(true);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND); // standing still, so only the cogs reach them
		j.setTarget(target);
		for (net.minecraft.world.entity.LivingEntity at : List.of(villager, target)) {
			com.comedymonkey.createaddons.content.juggernaut.ThrownCog cog = new com.comedymonkey.createaddons.content.juggernaut.ThrownCog(
				ModEntities.THROWN_COG.get(), helper.getLevel());
			cog.setOwner(j);
			cog.setPos(at.getX(), at.getY(0.5), at.getZ() - 3);
			cog.shoot(0, 0, 1, 1f, 0);
			cog.setNoGravity(true);
			helper.getLevel().addFreshEntity(cog);
		}
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(villager.getHealth() == villager.getMaxHealth(), "the cog should fly through the villager, health " + villager.getHealth());
			helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the cog should hit his target");
			helper.succeed();
		});
	}

	/** JUG-07 / WARN-01: the ritual's waking goes through NeoForge's spawn hook, and everyone by gets "Wound Up". */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void theRitualIsWoundUp(GameTestHelper helper) {
		listenForSpawns();
		BlockPos foot = new BlockPos(12, 0, 14);
		WindingKeyBlockEntity key = effigy(helper, foot, true);
		key.testSpeed = 64;
		key.setWind(0.95f);
		BlockPos keyAt = helper.absolutePos(foot.above().south());
		boolean[] awarded = { false };
		com.comedymonkey.createaddons.advancement.ModAdvancements.NearbyAward listener = (level, pos, action, range) -> {
			if (level == helper.getLevel() && pos.equals(keyAt)
				&& action.equals(com.comedymonkey.createaddons.advancement.ModAdvancements.JUGGERNAUT_SUMMONED) && range >= 32)
				awarded[0] = true;
		};
		com.comedymonkey.createaddons.advancement.ModAdvancements.TEST_LISTENERS.add(listener);
		helper.succeedWhen(() -> {
			List<ClockworkJuggernaut> found = juggernauts(helper);
			helper.assertTrue(found.size() == 1, "he should have woken, found " + found.size());
			helper.assertTrue(SPAWN_HOOKED.contains(found.get(0).getUUID()), "his waking should go through FinalizeSpawnEvent");
			helper.assertTrue(awarded[0], "\"Wound Up\" should be awarded near the key");
			com.comedymonkey.createaddons.advancement.ModAdvancements.TEST_LISTENERS.remove(listener);
		});
	}

	private static final Set<java.util.UUID> SPAWN_HOOKED = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private static boolean listening;

	private static synchronized void listenForSpawns() {
		if (listening)
			return;
		listening = true;
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent.class, e -> {
			if (e.getEntity() instanceof ClockworkJuggernaut j && e.getSpawnType() == net.minecraft.world.entity.MobSpawnType.TRIGGERED)
				SPAWN_HOOKED.add(j.getUUID());
		});
	}

	/** JUG-07: a Spring Engine wound full gets everyone by "Tightly Wound", once. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void aFullSpringEngineIsTightlyWound(GameTestHelper helper) {
		BlockPos out = new BlockPos(12, 0, 10);
		SpringEngineBlockEntity engine = springEngine(helper, out);
		engine.setStored(SpringEngineBlockEntity.capacity() - 1024L * 5);
		BlockPos at = helper.absolutePos(out);
		int[] awarded = { 0 };
		com.comedymonkey.createaddons.advancement.ModAdvancements.NearbyAward listener = (level, pos, action, range) -> {
			if (level == helper.getLevel() && pos.equals(at) && action.equals(com.comedymonkey.createaddons.advancement.ModAdvancements.SPRING_ENGINE_FULL))
				awarded[0]++;
		};
		com.comedymonkey.createaddons.advancement.ModAdvancements.TEST_LISTENERS.add(listener);
		helper.setBlock(out.south(2), creativeMotor(Direction.NORTH));
		helper.runAfterDelay(80, () -> {
			com.comedymonkey.createaddons.advancement.ModAdvancements.TEST_LISTENERS.remove(listener);
			helper.assertTrue(engine.stored() == SpringEngineBlockEntity.capacity(), "it should be wound full, holds " + engine.stored());
			helper.assertTrue(awarded[0] == 1, "\"Tightly Wound\" should be awarded once as it fills, was " + awarded[0]);
			helper.succeed();
		});
	}

	/** Saved while he gets up out of his ruin, he loads without the break-out pending: a later waking elsewhere breaks nothing. */
	@GameTest(template = COURSE)
	public static void aReloadWhileHeWakesClearsTheBreakOut(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.wakeFromRuin();
		CompoundTag tag = new CompoundTag();
		j.saveWithoutId(tag);
		ClockworkJuggernaut loaded = ModEntities.CLOCKWORK_JUGGERNAUT.get().create(helper.getLevel());
		loaded.load(tag);
		helper.assertFalse(loaded.fromRuin(), "a reload while he wakes should leave no break-out pending");
		helper.succeed();
	}

	/** He never leaps through a wall: with no clear arc to them, he straightens up instead. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void heNeverLeapsThroughAWall(GameTestHelper helper) {
		for (int x = 0; x < 24; x++)
			for (int y = 1; y < 12; y++)
				helper.setBlock(new BlockPos(x, y, 17), Blocks.GLASS);
		ClockworkJuggernaut j = spawn(helper);
		Pig pig = helper.spawn(EntityType.PIG, new BlockPos(12, 2, 21));
		pig.setInvulnerable(true);
		j.forceLeap(pig);
		helper.onEachTick(() -> helper.assertTrue(j.getZ() < helper.absolutePos(new BlockPos(0, 0, 17)).getZ(),
			"he went through the wall"));
		helper.runAfterDelay(80, () -> {
			helper.assertFalse(j.isNoGravity(), "he shouldn't be left floating");
			helper.succeed();
		});
	}

	/** At 20% in his last stage he goes into overdrive: out of his rewind at once, and every hit (arrows too) lands in full. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void underTwentyPercentHeFightsInOverdrive(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.forceStage(3);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		j.setHealth(j.getMaxHealth() * 0.22f);
		Player front = player(helper, j, 6);
		hit(j, j.damageSources().playerAttack(front), j.getMaxHealth() * 0.04f);
		helper.assertTrue(j.overdrive(), "he should be in overdrive under 20%");
		helper.assertTrue(j.phase() == ClockworkJuggernaut.Phase.REWIND_END, "overdrive should end his rewind, is " + j.phase());
		j.forcePhase(ClockworkJuggernaut.Phase.WOUND);
		near(helper, hit(j, j.damageSources().playerAttack(front), 10), 10, "a hit in overdrive, wound");
		Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
		arrow.setOwner(front);
		near(helper, hit(j, j.damageSources().arrow(arrow, front), 5), 5, "an arrow in overdrive");
		helper.succeed();
	}

	/** He killed the last player near him: he kneels, untouchable, until his rest is over or the first hit wakes him. */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void afterAWipeHeSleepsUntilHit(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		Player victim = player(helper, j, 4);
		j.awardKillScore(victim, 0, j.damageSources().mobAttack(j)); // what a player's death calls on its killer
		helper.succeedWhen(() -> {
			helper.assertTrue(j.resting(), "he should be resting after the wipe, is " + j.phase());
			j.setHealth(j.getHealth() - 50); // room to show a hit landing
			near(helper, hit(j, j.damageSources().playerAttack(victim), 30), 0, "the hit that wakes him");
			helper.assertTrue(j.phase() == ClockworkJuggernaut.Phase.WAKING, "the first hit should wake him, is " + j.phase());
		});
	}

	/** His spring leap comes down on them and holds them still for a moment, frost on them (the landing freeze). */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void theLeapFreezesWhoItLandsOn(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		Pig pig = helper.spawn(EntityType.PIG, new BlockPos(12, 0, 20)); // held where it stands (a wandering pig is a moving target), still on its feet
		pig.setInvulnerable(true);
		pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 10));
		j.forceLeap(pig);
		helper.succeedWhen(() -> {
			helper.assertTrue(pig.getTicksFrozen() > 0, "the landing should frost the pig over and hold it; he plays " + j.clip().name() + ", " + String.format("%.1f", j.horizontalDistance(pig)) + " away, y " + String.format("%.1f", j.getY()) + ", pig on ground " + pig.onGround() + ", phase " + j.phase());
			helper.assertTrue(j.horizontalDistance(pig) < 3, "he should have come down on the pig");
		});
	}

	/** D15: no Capture Bottle, jar or cage takes him (he's a boss), so there's no Mainspring farm; Carry On can't lift him. */
	@GameTest(template = COURSE)
	public static void noBottleOrCageTakesHim(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		helper.assertTrue(j.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES), "he should be tagged a boss");
		helper.assertFalse(com.comedymonkey.createaddons.content.cage.CageRules.isCageable(j), "no cage or jar should take him");
		helper.assertFalse(com.comedymonkey.createaddons.content.pen.AnimalCapture.canCapture(null, j), "the Capture Bottle shouldn't take him");
		helper.assertTrue(j.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath("carryon", "entity_blacklist"))), "Carry On should leave him be");
		helper.succeed();
	}

	/** Snowballs and other zero-damage hits on the key don't count toward a jam. */
	@GameTest(template = COURSE)
	public static void snowballsDontJamTheKey(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		Player behind = player(helper, j, -6);
		for (int i = 0; i < 5; i++)
			hit(j, j.damageSources().playerAttack(behind), 0);
		helper.assertFalse(j.jammed(), "zero-damage hits shouldn't jam the key");
		helper.succeed();
	}

	/** /kill (and the void) end him, but only a player's kill gets the Mainspring. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void noMainspringWithoutAPlayer(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.kill();
		helper.succeedWhen(() -> {
			helper.assertTrue(j.isRemoved(), "/kill should remove him");
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, j.getBoundingBox().inflate(4)).isEmpty(),
				"nothing should drop without a player");
		});
	}

	/** Saved mid-leap (no gravity for the arc), he comes back on his feet, not floating. */
	@GameTest(template = COURSE)
	public static void aReloadMidLeapDoesntLeaveHimFloating(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.setNoGravity(true);
		CompoundTag tag = new CompoundTag();
		j.saveWithoutId(tag);
		ClockworkJuggernaut loaded = ModEntities.CLOCKWORK_JUGGERNAUT.get().create(helper.getLevel());
		loaded.load(tag);
		helper.assertFalse(loaded.isNoGravity(), "he should fall again after a reload");
		helper.succeed();
	}

	/** D2: mob griefing is off for him, so farmland and turtle eggs survive his feet. */
	@GameTest(template = COURSE)
	public static void heCantGrief(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		helper.assertFalse(EventHooks.canEntityGrief(helper.getLevel(), j), "he shouldn't be able to grief");
		helper.succeed();
	}

	/** One huge hit can't skip a stage: he stops at two thirds, sheds his plates, and goes on in stage 2 (D43). */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void heShedsHisPlatesAtTwoThirds(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		hit(j, j.damageSources().playerAttack(player(helper, j, 6)), 1000);
		near(helper, j.getHealth(), j.getMaxHealth() * 2 / 3f, "health after a huge hit in stage 1");
		helper.assertTrue(j.phase() == ClockworkJuggernaut.Phase.SHEDDING, "he should be shedding, is " + j.phase());
		near(helper, hit(j, j.damageSources().playerAttack(player(helper, j, 6)), 50), 0, "a hit while he sheds");
		helper.succeedWhen(() -> {
			helper.assertTrue(j.stage() == 2, "stage should be 2, is " + j.stage());
			helper.assertTrue(j.phase() == ClockworkJuggernaut.Phase.WOUND, "he should fight on wound, is " + j.phase());
		});
	}

	/** His last hit plays his death, and the Mainspring comes out at the end, always one (D30). */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void heDiesSlowlyAndDropsTheMainspring(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		j.forceStage(3);
		j.forcePhase(ClockworkJuggernaut.Phase.REWIND);
		Player killer = player(helper, j, 6);
		hit(j, j.damageSources().playerAttack(killer), 10000);
		helper.assertTrue(j.isAlive() && j.phase() == ClockworkJuggernaut.Phase.DYING, "he should be playing his death, is " + j.phase());
		helper.runAfterDelay(40, () -> helper.assertTrue(j.isAlive(), "he should still be winding down 2 s in"));
		helper.succeedWhen(() -> {
			helper.assertTrue(j.isRemoved(), "he should be gone after his death clip");
			List<ItemEntity> springs = helper.getLevel().getEntitiesOfClass(ItemEntity.class, j.getBoundingBox().inflate(4),
				e -> e.getItem().is(ModItems.MAINSPRING.get()));
			helper.assertTrue(springs.stream().mapToInt(e -> e.getItem().getCount()).sum() == 1, "exactly one Mainspring should drop");
		});
	}

	/** D2: twenty seconds of fighting (charges, stomps, swings, grabs) and not one block around him has changed. */
	@GameTest(template = COURSE, timeoutTicks = 500)
	public static void heNeverChangesABlock(GameTestHelper helper) {
		// a ring of glass, flowers and grass around the arena: anything he broke or trampled would show
		for (int x = 1; x < 23; x++)
			for (int z = 1; z < 23; z++)
				if (x == 1 || z == 1 || x == 22 || z == 22)
					helper.setBlock(new BlockPos(x, 2, z), (x + z) % 2 == 0 ? Blocks.GLASS : Blocks.POPPY);
		for (int x = 3; x < 21; x += 4)
			helper.setBlock(new BlockPos(x, 2, 4), Blocks.SHORT_GRASS);
		List<BlockState> before = snapshot(helper);
		ClockworkJuggernaut j = spawn(helper);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 2, 3));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.runAfterDelay(400, () -> {
			helper.assertTrue(snapshot(helper).equals(before), "a block in the arena changed while he fought");
			helper.succeed();
		});
	}

	/** He doesn't stand there: given a target 10 blocks off, he walks or charges in to swinging range. */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void heClosesOnHisTarget(GameTestHelper helper) {
		ClockworkJuggernaut j = spawn(helper);
		ArmorStand dummy = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 2, 22));
		dummy.setInvulnerable(true);
		j.setTarget(dummy);
		helper.succeedWhen(() -> helper.assertTrue(j.horizontalDistance(dummy) < 7,
			"he should have come within 7 blocks, is " + String.format("%.1f", j.horizontalDistance(dummy)) + " away, " + j.phase() + ", " + j.clip().name()));
	}

	// ---- the ritual (D16-D19, D52)

	/** A T of Brass Blocks (its foot at {@code foot}, facing north) with a Winding Key on its back; returns the key. */
	private static WindingKeyBlockEntity effigy(GameTestHelper helper, BlockPos foot, boolean whole) {
		BlockState brass = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "brass_block")).defaultBlockState();
		BlockPos centre = foot.above();
		helper.setBlock(foot, brass);
		helper.setBlock(centre, brass);
		helper.setBlock(centre.west(), brass);
		if (whole)
			helper.setBlock(centre.east(), brass);
		BlockPos key = centre.south();
		helper.setBlock(key, ModBlocks.WINDING_KEY.get().defaultBlockState().setValue(WindingKeyBlock.FACING, Direction.SOUTH));
		return helper.getBlockEntity(key);
	}

	private static List<ClockworkJuggernaut> juggernauts(GameTestHelper helper) {
		return helper.getLevel().getEntitiesOfClass(ClockworkJuggernaut.class, helper.getBounds().inflate(0, 10, 0));
	}

	/** Wound in full, the effigy and key are gone and he stands in front of where the T stood, facing its way (north). */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void theRitualWakesHim(GameTestHelper helper) {
		BlockPos foot = new BlockPos(12, 0, 14);
		WindingKeyBlockEntity key = effigy(helper, foot, true);
		key.testSpeed = 64;
		key.setWind(0.95f);
		helper.succeedWhen(() -> {
			List<ClockworkJuggernaut> found = juggernauts(helper);
			helper.assertTrue(found.size() == 1, "he should have woken, found " + found.size());
			ClockworkJuggernaut j = found.get(0);
			helper.assertBlockNotPresent(ModBlocks.WINDING_KEY.get(), foot.above().south());
			helper.assertBlockPresent(Blocks.AIR, foot.above());
			helper.assertTrue(j.phase() != ClockworkJuggernaut.Phase.DORMANT, "in the effigy's place he wakes at once, is " + j.phase());
			helper.assertTrue(j.getZ() < helper.absolutePos(foot).getZ(), "he should stand in front (north) of the T");
		});
	}

	/** D18: a Hand Crank (32 RPM) winds it in 20 s, a quarter in 5 s; a shaft at 64 RPM winds twice as fast. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void aHandCrankWindsItInTwentySeconds(GameTestHelper helper) {
		WindingKeyBlockEntity byHand = effigy(helper, new BlockPos(6, 0, 14), true);
		WindingKeyBlockEntity byMotor = effigy(helper, new BlockPos(18, 0, 14), true);
		byHand.testSpeed = 32;
		byMotor.testSpeed = 64;
		helper.runAfterDelay(100, () -> {
			near(helper, byHand.wind(), 0.25f, "wound by hand for 5 s");
			near(helper, byMotor.wind(), 0.5f, "wound at 64 RPM for 5 s");
			helper.succeed();
		});
	}

	/** Stopped, the key slowly unwinds (D18). */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aStoppedKeyUnwinds(GameTestHelper helper) {
		WindingKeyBlockEntity key = effigy(helper, new BlockPos(12, 0, 14), true);
		key.testSpeed = 0;
		key.setWind(0.5f);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(key.wind() < 0.5f && key.wind() > 0.4f, "it should unwind slowly, is at " + key.wind());
			helper.succeed();
		});
	}

	/** Without the whole T, the key turns but never winds (D16). */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aBrokenEffigyNeverWinds(GameTestHelper helper) {
		WindingKeyBlockEntity key = effigy(helper, new BlockPos(12, 0, 14), false);
		key.testSpeed = 64;
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(key.wind() == 0, "it shouldn't wind without the whole T, is at " + key.wind());
			helper.succeed();
		});
	}

	/** With a roof over the effigy, he appears on top of it instead, kneeling, and wakes when someone comes (D52). */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void summonedUnderARoofHeKneelsOnTop(GameTestHelper helper) {
		for (int x = 1; x < 23; x++)
			for (int z = 1; z < 23; z++)
				helper.setBlock(new BlockPos(x, 5, z), Blocks.STONE);
		WindingKeyBlockEntity key = effigy(helper, new BlockPos(12, 0, 14), true);
		key.testSpeed = 64;
		key.setWind(0.95f);
		helper.succeedWhen(() -> {
			List<ClockworkJuggernaut> found = juggernauts(helper);
			helper.assertTrue(found.size() == 1, "he should have come, found " + found.size());
			ClockworkJuggernaut j = found.get(0);
			helper.assertTrue(j.getY() >= helper.absolutePos(new BlockPos(0, 6, 0)).getY(), "he should be on the roof, at y " + j.getY());
			helper.assertTrue(j.phase() == ClockworkJuggernaut.Phase.DORMANT, "on the surface he kneels, is " + j.phase());
		});
	}

	// ---- the Spring Engine (D13, D34-D38)

	/** A Spring Engine with its output end at {@code out}, giving north; its winding end is the block south of it. */
	private static SpringEngineBlockEntity springEngine(GameTestHelper helper, BlockPos out) {
		BlockState state = ModBlocks.SPRING_ENGINE.get().defaultBlockState().setValue(SpringEngineBlock.HORIZONTAL_FACING, Direction.NORTH);
		helper.setBlock(out, state.setValue(SpringEngineBlock.PART, SpringEngineBlock.Part.OUTPUT));
		helper.setBlock(out.south(), state.setValue(SpringEngineBlock.PART, SpringEngineBlock.Part.WINDING));
		return helper.getBlockEntity(out);
	}

	private static BlockState creativeMotor(Direction facing) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor")).defaultBlockState()
			.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, facing);
	}

	/** D13: full, it holds an hour at 16,384 SU; picked up, it keeps every bit of its wind (D38). */
	@GameTest(template = COURSE)
	public static void theSpringEngineKeepsItsWind(GameTestHelper helper) {
		helper.assertTrue(SpringEngineBlockEntity.capacity() == 3600L * 16_384 * 20, "full should be an hour at 16,384 SU, is " + SpringEngineBlockEntity.capacity());
		BlockPos out = new BlockPos(12, 0, 10);
		SpringEngineBlockEntity engine = springEngine(helper, out);
		engine.setStored(SpringEngineBlockEntity.capacity() / 2 + 7);
		List<net.minecraft.world.item.ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(helper.getBlockState(out), helper.getLevel(),
			helper.absolutePos(out), engine);
		helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.SPRING_ENGINE.get()), "it should drop one Spring Engine, drops " + drops);
		helper.assertTrue(drops.get(0).getOrDefault(com.comedymonkey.createaddons.registry.ModDataComponents.SPRING_WIND.get(), 0L)
			== SpringEngineBlockEntity.capacity() / 2 + 7, "the item should keep its wind exactly");
		helper.succeed();
	}

	/** Broken from its winding end, the engine still drops once, wind and all, and the output end goes with it. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void breakingTheWindingEndDropsTheEngineOnce(GameTestHelper helper) {
		BlockPos out = new BlockPos(12, 0, 10), back = out.south();
		SpringEngineBlockEntity engine = springEngine(helper, out);
		engine.setStored(12_345);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		BlockState state = helper.getBlockState(back);
		state.getBlock().playerWillDestroy(helper.getLevel(), helper.absolutePos(back), state, player);
		helper.getLevel().destroyBlock(helper.absolutePos(back), false);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, out);
			List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(out)).inflate(3),
				e -> e.getItem().is(ModItems.SPRING_ENGINE.get()));
			helper.assertTrue(items.size() == 1, "one Spring Engine should drop, " + items.size() + " did");
			helper.assertTrue(items.get(0).getItem().getOrDefault(com.comedymonkey.createaddons.registry.ModDataComponents.SPRING_WIND.get(), 0L) == 12_345,
				"it should keep its wind");
		});
	}

	/** D35: a motor turning its winding end stores exactly the stress it's set to take (1,024 SU) every tick, lossless. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void aMotorWindsTheSpringEngine(GameTestHelper helper) {
		BlockPos out = new BlockPos(12, 0, 10);
		SpringEngineBlockEntity engine = springEngine(helper, out);
		helper.setBlock(out.south(2), creativeMotor(Direction.NORTH));
		helper.runAfterDelay(100, () -> {
			long stored = engine.stored();
			helper.assertTrue(stored > 0 && stored % 1024 == 0 && stored <= 1024L * 100, "it should store 1,024 SU a tick it turns, holds " + stored);
			helper.assertTrue(stored >= 1024L * 60, "it should have wound most of those 5 s, holds " + stored);
			helper.succeed();
		});
	}

	/** D36/D37: wound, it turns its shaft; nothing drawing on it, it loses nothing; a redstone signal stops it. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void theSpringEngineGivesOnlyWhatIsUsed(GameTestHelper helper) {
		BlockPos out = new BlockPos(12, 0, 10);
		SpringEngineBlockEntity engine = springEngine(helper, out);
		engine.setStored(1_000_000);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(engine.getSpeed() != 0, "wound, it should turn its shaft");
			helper.assertTrue(engine.stored() == 1_000_000, "with nothing drawing on it, it should lose nothing, holds " + engine.stored());
			helper.assertTrue(engine.comparatorLevel() > 0, "a comparator should read some wind");
			helper.setBlock(out.east(), Blocks.REDSTONE_BLOCK);
		});
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(engine.getSpeed() == 0, "a redstone signal should stop it");
			helper.succeed();
		});
	}

	private static List<BlockState> snapshot(GameTestHelper helper) {
		List<BlockState> states = new java.util.ArrayList<>();
		for (int x = 0; x < 24; x++)
			for (int y = 0; y < 8; y++)
				for (int z = 0; z < 24; z++)
					states.add(helper.getBlockState(new BlockPos(x, y, z)));
		return states;
	}

	/** Every clip the code plays was exported, and every sound a clip cues is registered. */
	@GameTest(template = COURSE)
	public static void everyClipAndCueExists(GameTestHelper helper) {
		Set<String> used = Set.of("idle", "walk", "scan", "knuckles", "sigh", "charge", "swing", "stomp", "stomp_double", "grab_throw",
			"grab_blocked", "leap", "steam_vent", "gear_spin_start", "gear_spin", "gear_spin_end", "cog_throw", "run_down", "rewind",
			"rewind_end", "feint", "key_jam", "shed_plates_2", "shed_plates_3", "hit_wound", "hit_open", "lightning", "dormant", "wake",
			"wake_ritual", "death", "salute");
		for (String name : used)
			helper.assertTrue(ClockworkJuggernaut.clips().find(name) != null, "no clip " + name);
		for (Clip clip : ClockworkJuggernaut.clips().all())
			for (Clip.Cue cue : clip.cues())
				helper.assertTrue(ModSounds.juggernaut(cue.event()) != null, clip.name() + " cues an unregistered sound " + cue.event());
		for (String id : List.of("tick", "tock", "plate_ring", "glow_on", "cog_fly", "cog_hit"))
			helper.assertTrue(ModSounds.juggernaut(id) != null, "the code plays an unregistered sound " + id);
		helper.succeed();
	}
}
