package com.comedymonkey.createaddons.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlock;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity;
import com.comedymonkey.createaddons.content.cage.MonsterJarItem;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.comedymonkey.createaddons.content.tank.FishTankBlock;
import com.comedymonkey.createaddons.content.tank.FishTankBlockEntity;
import com.comedymonkey.createaddons.content.tank.TankRules;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Monster Cages' and Fish Tanks' performance fixes that need a world: the cached monster type through a drowning,
 * a broken cage's merged drops, the cage's own syncs leaving out the jar, the stress impact's slow fall, the trident
 * search's pause, the Netherite cage's FE not syncing every tick but saved, the countdown saved, the tank creature
 * cache, and a jar of nothing not taking display creature builds. On the Trade Bot's empty course (./gradlew
 * runGameTestServer).
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CagePerformanceGameTests {
	private static final String COURSE = "trade_bot_course";

	private CagePerformanceGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(CagePerformanceGameTests.class);
	}

	/** A zombie drowning into a drowned: the cage's remembered type follows the jar every tick, through the conversion. */
	@GameTest(template = COURSE, timeoutTicks = 800)
	public static void cachedTypeFollowsADrowning(GameTestHelper helper) {
		MonsterCageBlockEntity cage = cage(helper, new BlockPos(4, 1, 4), ModBlocks.FLOODED_MONSTER_CAGE.get());
		ItemStack zombie = jar(helper, EntityType.ZOMBIE);
		if (!cage.getCaptiveSlot().insertItem(0, zombie, false).isEmpty())
			helper.fail("the flooded cage didn't take the zombie");
		cage.getWaterHandler().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
		String[] mismatch = {null};
		helper.onEachTick(() -> {
			EntityType<?> held = cage.getMonsterType(), inJar = MonsterJarItem.typeOf(cage.getCaptive());
			if (held != inJar && mismatch[0] == null)
				mismatch[0] = "at tick " + helper.getTick() + " the cage held " + held + " for a jar of " + inJar;
		});
		helper.succeedWhen(() -> {
			if (mismatch[0] != null)
				helper.fail(mismatch[0]);
			if (cage.getMonsterType() != EntityType.DROWNED)
				helper.fail("still " + cage.getMonsterType() + " after " + helper.getTick() + " ticks");
		});
	}

	/**
	 * A full cage broken: the horde's hundreds of loot rolls come out as joined stacks. Dropping splits a stack into
	 * pieces of 10 to 30, so the flesh is in at most a tenth as many item entities as items (one roll each, before).
	 */
	@GameTest(template = COURSE)
	public static void brokenFullCageDropsMergedStacks(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 1, 4);
		MonsterCageBlockEntity cage = cage(helper, pos, ModBlocks.BRASS_MONSTER_CAGE.get());
		cage.setPonderHorde(jar(helper, EntityType.ZOMBIE), cage.getCapacity(), 0);
		int horde = cage.getCount();
		helper.destroyBlock(pos);
		List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
			new AABB(helper.absolutePos(pos)).inflate(2));
		int flesh = 0, fleshEntities = 0;
		for (ItemEntity drop : drops)
			if (drop.getItem().is(Items.ROTTEN_FLESH)) {
				flesh += drop.getItem().getCount();
				fleshEntities++;
			}
		if (horde < 60 || flesh < 30)
			helper.fail("a horde of " + horde + " dropped only " + flesh + " rotten flesh");
		if (fleshEntities > flesh / 10 + 6)
			helper.fail(flesh + " rotten flesh came out in " + fleshEntities + " item entities: not merged");
		if (drops.stream().noneMatch(drop -> MonsterJarItem.isFilled(drop.getItem())))
			helper.fail("the jar didn't drop");
		helper.succeed();
	}

	/**
	 * The jar counts as sent only when a sync of the cage's own broadcasts it: building the update packet (as another
	 * mod might, for one player) changes nothing. The sync after leaves the jar out; a while later it's sent again.
	 * Peeking at the packet here doesn't skew what the real broadcast sends.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void cageUpdatePacketLeavesOutTheJar(GameTestHelper helper) {
		MonsterCageBlockEntity cage = cage(helper, new BlockPos(4, 1, 4), ModBlocks.MONSTER_CAGE.get());
		cage.getCaptiveSlot().insertItem(0, jar(helper, EntityType.ZOMBIE), false);
		// the sync the jar went in with: it's in every packet built for that broadcast, however many are built
		for (int i = 0; i < 3; i++)
			if (!cage.getUpdatePacket().getTag().contains("Captive"))
				helper.fail("packet " + (i + 1) + " for the sync the jar went in with left it out");
		helper.runAfterDelay(5, () -> {
			// (unless a queued sync of its own has just gone out, which rightly leaves it out too)
			boolean ownSyncJustNow = helper.getLevel().getGameTime() - longField(cage, "ownSyncAt") <= 1;
			if (!ownSyncJustNow && !cage.getUpdatePacket().getTag().contains("Captive"))
				helper.fail("a packet built long after the cage's last sync left the jar out");
			cage.sendData();
			CompoundTag next = cage.getUpdatePacket().getTag();
			if (next.contains("Captive") || !next.getBoolean("SameCaptive"))
				helper.fail("the next sync of the cage's own sent the jar again: " + next.getAllKeys());
		});
		helper.runAfterDelay(120, () -> {
			cage.sendData();
			if (!cage.getUpdatePacket().getTag().contains("Captive"))
				helper.fail("a sync 100 ticks on didn't send the jar again");
			helper.succeed();
		});
	}

	/** A jar whose monster can't be made (a cow) is tried once: then it takes no more of the builds until the jar changes. */
	@GameTest(template = COURSE)
	public static void aJarThatMakesNothingStopsTakingBuilds(GameTestHelper helper) {
		MonsterCageBlockEntity cage = cage(helper, new BlockPos(4, 1, 4), ModBlocks.MONSTER_CAGE.get());
		cage.setPonderHorde(jar(helper, EntityType.COW), 4, 0);
		int[] asked = {0};
		BooleanSupplier budget = () -> {
			asked[0]++;
			return true;
		};
		buildShown(cage, 4, budget);
		buildShown(cage, 4, budget);
		buildShown(cage, 4, budget);
		if (asked[0] != 1 || !cage.getShown().isEmpty())
			helper.fail("a jar of nothing took " + asked[0] + " builds and showed " + cage.getShown().size());
		cage.setPonderHorde(jar(helper, EntityType.ZOMBIE), 4, 0);
		asked[0] = 0;
		buildShown(cage, 3, budget);
		if (asked[0] != 3 || cage.getShown().size() != 3)
			helper.fail("a new jar took " + asked[0] + " builds and showed " + cage.getShown().size());
		helper.succeed();
	}

	/**
	 * FE fed in from outside the cage's tick (a cable in a ticking chunk next to one that isn't) marks its chunk at once.
	 * (With its field up: off, it takes none.)
	 */
	@GameTest(template = COURSE)
	public static void feFedInMarksTheChunkAtOnce(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 1, 4);
		MonsterCageBlockEntity cage = cage(helper, pos, ModBlocks.NETHERITE_MONSTER_CAGE.get());
		helper.runAtTickTime(2, () -> {
			cage.setPonderField(true, -1);
			var chunk = helper.getLevel().getChunkAt(helper.absolutePos(pos));
			chunk.setUnsaved(false);
			cage.getEnergy().receiveEnergy(1000, false);
			if (!chunk.isUnsaved())
				helper.fail("1,000 FE fed in left the chunk unmarked: lost if it unloads");
			helper.succeed();
		});
	}

	/**
	 * With the field down the countdown to an escape starts (paused, nobody being near), and it's saved with the chunk
	 * as it changes, or a crash or an unload would put it back. The countdown running down goes through the same
	 * check; a player to run it would need a real connection (a mock one trips other mods' packets). Its own batch, so
	 * no other test marks the chunk.
	 */
	@GameTest(template = COURSE, batch = "cage_countdown")
	public static void theCountdownIsSavedAsItChanges(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 1, 4);
		MonsterCageBlockEntity cage = cage(helper, pos, ModBlocks.NETHERITE_MONSTER_CAGE.get());
		helper.runAtTickTime(3, () -> {
			ItemStack wither = ((MonsterJarItem) ModItems.NETHERITE_JAR.get()).fill(EntityType.WITHER.create(helper.getLevel()));
			cage.getCaptiveSlot().insertItem(0, wither, false);
			if (cage.getEscapeIn() >= 0)
				helper.fail("the countdown started before the cage ticked");
			// putting the jar in marked it: from here only the countdown can
			helper.getLevel().getChunkAt(helper.absolutePos(pos)).setUnsaved(false);
		});
		helper.runAtTickTime(6, () -> {
			if (cage.isFieldUp() || cage.getEscapeIn() < 0 || !cage.isPaused())
				helper.fail("no paused countdown: field " + cage.isFieldUp() + ", escape in " + cage.getEscapeIn() + ", paused " + cage.isPaused());
			if (!helper.getLevel().getChunkAt(helper.absolutePos(pos)).isUnsaved())
				helper.fail("the countdown started without marking the chunk for saving");
			helper.succeed();
		});
	}

	/**
	 * The tank's stress: on a motor it loads the network at once; with its water drained it wants nothing, but the
	 * network keeps the load for 20 ticks before it's taken off; water back, the load is back at once.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void stressRisesAtOnceAndFallsAfterTwentyTicks(GameTestHelper helper) {
		BlockPos at = new BlockPos(4, 1, 3);
		FishTankBlockEntity tank = stockedTank(helper, at);
		helper.runAtTickTime(2, () -> motor(helper, at.south(), 64));
		helper.runAtTickTime(20, () -> {
			if (held(tank) <= 0)
				helper.fail("the stocked tank put no load on the network (" + held(tank) + ")");
			tank.getWater().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
		});
		helper.runAtTickTime(30, () -> {
			if (held(tank) <= 0)
				helper.fail("the load came off 10 ticks after the water went");
		});
		helper.runAtTickTime(45, () -> {
			if (held(tank) != 0)
				helper.fail("the load was still on 25 ticks after the water went: " + held(tank));
			tank.getWaterHandler().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
		});
		helper.runAtTickTime(47, () -> {
			if (held(tank) <= 0)
				helper.fail("the load didn't come back at once with the water");
			helper.succeed();
		});
	}

	/** An armed drowned with nobody to throw at looks again every 11 ticks, not every tick. */
	@GameTest(template = COURSE)
	public static void tridentSearchPausesWithNobodyThere(GameTestHelper helper) {
		if (helper.getLevel().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL)
			helper.fail("the test world is on Peaceful, where drowned don't throw");
		MonsterCageBlockEntity cage = cage(helper, new BlockPos(4, 1, 4), ModBlocks.FLOODED_MONSTER_CAGE.get());
		cage.setPonderHorde(jar(helper, EntityType.DROWNED), 4, 2);
		int[] searches = {0};
		for (int tick = 2; tick < 32; tick++)
			helper.runAtTickTime(tick, () -> {
				if (field(cage, "throwCooldown") == 10)
					searches[0]++;
			});
		helper.runAtTickTime(32, () -> {
			if (searches[0] < 1 || searches[0] > 4)
				helper.fail("looked for a target " + searches[0] + " times in 30 ticks");
			if (!helper.getLevel().getEntitiesOfClass(ThrownTrident.class, new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(20)).isEmpty())
				helper.fail("a trident was thrown at nobody");
			helper.succeed();
		});
	}

	/**
	 * A Netherite cage's field draws FE every tick, but the cage isn't synced every tick for it (it was every 10): the
	 * goggles get the FE about once a second at most, and the chunk is still marked for saving. (The field is put up
	 * directly: the supply rules are CageFieldGameTests'.)
	 */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void netheriteFeDoesNotSyncEveryTick(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 1, 4);
		MonsterCageBlockEntity cage = cage(helper, pos, ModBlocks.NETHERITE_MONSTER_CAGE.get());
		ItemStack wither = ((MonsterJarItem) ModItems.NETHERITE_JAR.get()).fill(EntityType.WITHER.create(helper.getLevel()));
		if (!cage.getCaptiveSlot().insertItem(0, wither, false).isEmpty())
			helper.fail("the Netherite cage didn't take the Wither");
		// up and full, with nothing coming in: it runs down its store, its FE changing every tick
		cage.setPonderField(true, -1);
		cage.getEnergy().receiveEnergy(cage.getEnergyCapacity(), false);
		Set<Long> syncs = new HashSet<>();
		int[] energyAtStart = {0};
		helper.runAtTickTime(5, () -> {
			if (!cage.isFieldUp())
				helper.fail("the field didn't come up");
			energyAtStart[0] = cage.getEnergyStored();
		});
		for (int tick = 5; tick < 65; tick++)
			helper.runAtTickTime(tick, () -> syncs.add(longField(cage, "ownSyncAt")));
		helper.runAtTickTime(40, () -> helper.getLevel().getChunkAt(helper.absolutePos(pos)).setUnsaved(false));
		helper.runAtTickTime(65, () -> {
			if (cage.getEnergyStored() >= energyAtStart[0])
				helper.fail("the field took no FE");
			// the values seen include the sync before the window
			int synced = syncs.size() - 1;
			if (synced > 3)
				helper.fail("synced " + synced + " times in 60 ticks for the FE alone");
			if (synced < 1)
				helper.fail("the FE never reached clients in 60 ticks");
			if (!helper.getLevel().getChunkAt(helper.absolutePos(pos)).isUnsaved())
				helper.fail("the FE drawn wasn't marked for saving");
			helper.succeed();
		});
	}

	/**
	 * Whether a bottle holds a tank creature is worked out once per kind: the cache answers (a planted wrong answer is
	 * given back), and the blacklist (tadpoles are fish to the game) is still read each time.
	 */
	@GameTest(template = COURSE)
	public static void tankCreatureKindsAreCached(GameTestHelper helper) {
		Map<EntityType<?>, Boolean> cache = staticField(TankRules.class, "WATER_KINDS");
		if (!TankRules.livesInTank(bottled(helper, EntityType.COD), helper.getLevel()) || !Boolean.TRUE.equals(cache.get(EntityType.COD)))
			helper.fail("cod: not a tank creature, or not cached " + cache);
		if (TankRules.livesInTank(bottled(helper, EntityType.COW), helper.getLevel()) || !Boolean.FALSE.equals(cache.get(EntityType.COW)))
			helper.fail("cow: a tank creature, or not cached");
		if (TankRules.livesInTank(bottled(helper, EntityType.TADPOLE), helper.getLevel()))
			helper.fail("a tadpole lives in a tank");
		// the cache is what answers
		Boolean pig = cache.put(EntityType.PIG, true);
		try {
			if (!TankRules.livesInTank(bottled(helper, EntityType.PIG), helper.getLevel()))
				helper.fail("the cache wasn't asked");
		} finally {
			if (pig == null)
				cache.remove(EntityType.PIG);
			else
				cache.put(EntityType.PIG, pig);
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- helpers

	private static MonsterCageBlockEntity cage(GameTestHelper helper, BlockPos pos, Block block) {
		helper.setBlock(pos, block.defaultBlockState().setValue(MonsterCageBlock.FACING, Direction.NORTH));
		return helper.getBlockEntity(pos);
	}

	private static ItemStack jar(GameTestHelper helper, EntityType<?> type) {
		Entity monster = type.create(helper.getLevel());
		if (monster == null)
			throw new IllegalStateException("can't make " + type);
		return ((MonsterJarItem) ModItems.IRON_JAR.get()).fill(monster);
	}

	private static ItemStack bottled(GameTestHelper helper, EntityType<?> type) {
		Entity entity = type.create(helper.getLevel());
		if (entity == null)
			throw new IllegalStateException("can't make " + type);
		return BottledAnimalItem.of(entity);
	}

	/** Two cod with food and water, facing north (its shaft comes in from the south). */
	private static FishTankBlockEntity stockedTank(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.FISH_TANK.get().defaultBlockState().setValue(FishTankBlock.FACING, Direction.NORTH));
		FishTankBlockEntity tank = helper.getBlockEntity(pos);
		tank.setPonderSchool(bottled(helper, EntityType.COD), 2, 0, 0);
		tank.getAutomationHandler().insertItem(0, new ItemStack(ModItems.FISH_FOOD.get(), 16), false);
		tank.getWaterHandler().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
		return tank;
	}

	private static void motor(GameTestHelper helper, BlockPos pos, int rpm) {
		helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor")).defaultBlockState()
			.setValue(BlockStateProperties.FACING, Direction.NORTH));
		if (helper.getBlockEntity(pos) instanceof CreativeMotorBlockEntity motor)
			motor.generatedSpeed.setValue(rpm);
		else
			helper.fail("no creative motor");
	}

	/** The impact the tank's network holds for it (0 off a network). */
	private static float held(FishTankBlockEntity tank) {
		if (!tank.hasNetwork())
			return 0;
		Float held = tank.getOrCreateNetwork().members.get(tank);
		return held == null ? 0 : held;
	}

	/** The cage's package-private window builder, with a budget of the test's own. */
	private static void buildShown(MonsterCageBlockEntity cage, int want, BooleanSupplier budget) {
		try {
			Method method = MonsterCageBlockEntity.class.getDeclaredMethod("buildShown", int.class, BooleanSupplier.class);
			method.setAccessible(true);
			method.invoke(cage, want, budget);
		} catch (InvocationTargetException e) {
			throw e.getCause() instanceof RuntimeException runtime ? runtime : new IllegalStateException(e.getCause());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static int field(Object target, String name) {
		return ((Number) read(target, name)).intValue();
	}

	private static long longField(Object target, String name) {
		return ((Number) read(target, name)).longValue();
	}

	private static Object read(Object target, String name) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T staticField(Class<?> owner, String name) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			return (T) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
