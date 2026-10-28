package com.comedymonkey.createaddons.gametest;

import java.util.List;
import java.util.Set;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.juggernaut.ClockworkJuggernaut;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlock;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlockEntity;
import com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlock;
import com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlockEntity;
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
		var deployer = FakePlayerFactory.getMinecraft((ServerLevel) helper.getLevel());
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
