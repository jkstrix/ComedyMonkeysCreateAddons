package com.comedymonkey.createaddons.gametest;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.line.PowerGrid;
import com.comedymonkey.createaddons.content.line.PowerGridTesting;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.comedymonkey.createaddons.content.wastes.CityLink;
import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlock;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlockEntity;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictLightBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictStarReactorBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictStarReactorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock;
import com.comedymonkey.createaddons.content.wastes.ChargingAlcoveBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictCableBlock;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.ScarredPlateBlock;
import com.comedymonkey.createaddons.content.wastes.SeamedPlateBlock;
import com.comedymonkey.createaddons.content.wastes.VoidAlloyBond;
import com.comedymonkey.createaddons.content.wastes.SlidingDoorBlock;
import com.comedymonkey.createaddons.content.wastes.StarReactorBlock;
import com.comedymonkey.createaddons.content.wastes.StarReactorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.SubstationBlock;
import com.comedymonkey.createaddons.content.wastes.SubstationBlockEntity;
import com.comedymonkey.createaddons.content.wastes.ThreePartBlock;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Clockwork Wastes' powered city, phase 2 (design/specs/clockwork-wastes.md, P31, P55): a Substation wakes its
 * district at exactly the wake charge; locked blocks can't be broken by hand or blast until they open; vaults and doors
 * open when the record says so, including ones that load after the district woke; the heart vault opens on the
 * Custodian's death (or on Peaceful once four districts are awake) with its one Elytra; lamps light and go dark with the
 * district's feed; the Star Reactor runs flat out, keeps its burn and upgrade as an item, and burns 12 h overhauled; the
 * heart's reactor wakes the city and is salvaged for two Unknown Devices; a pylon breaks after six hits, only once the
 * Custodian is awake. Each test is its own city (its origin is its own corner), so they don't share a record.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesCityGameTests {
	private static final String COURSE = "trade_bot_course";

	private WastesCityGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(WastesCityGameTests.class);
	}

	// ---------------------------------------------------------------- helpers

	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(BlockPos.ZERO);
	}

	private static CityRecord record(GameTestHelper helper) {
		return CityRecord.of(helper.getLevel());
	}

	private static void bind(GameTestHelper helper, BlockPos pos, District district) {
		if (!CityLink.bind(helper.getLevel(), helper.absolutePos(pos), origin(helper), district))
			helper.fail("no city block to bind at " + pos);
	}

	/** A Substation (both halves) facing north at {@code pos}, bound to the test's city. */
	private static SubstationBlockEntity substation(GameTestHelper helper, BlockPos pos, District district) {
		BlockState lower = ModWastes.SUBSTATION.get().defaultBlockState();
		helper.setBlock(pos, lower);
		helper.setBlock(pos.above(), lower.setValue(SubstationBlock.HALF, DoubleBlockHalf.UPPER));
		bind(helper, pos, district);
		return (SubstationBlockEntity) helper.getBlockEntity(pos);
	}

	private static void door(GameTestHelper helper, BlockPos pos, boolean heart) {
		BlockState state = ModWastes.SLIDING_DOOR.get().defaultBlockState().setValue(SlidingDoorBlock.HEART, heart);
		Direction facing = Direction.NORTH;
		for (int c = 0; c < 2; c++)
			for (int r = 0; r < 3; r++)
				helper.setBlock(SlidingDoorBlock.part(pos, facing, c, r), state.setValue(SlidingDoorBlock.COL, c).setValue(SlidingDoorBlock.ROW, r));
	}

	private static void pylon(GameTestHelper helper, BlockPos pos) {
		BlockState state = ModWastes.CONDUIT_PYLON.get().defaultBlockState();
		helper.setBlock(pos, state);
		helper.setBlock(pos.above(), state.setValue(ThreePartBlock.PART, ThreePartBlock.Part.MIDDLE));
		helper.setBlock(pos.above(2), state.setValue(ThreePartBlock.PART, ThreePartBlock.Part.TOP));
	}

	private static Player player(GameTestHelper helper) {
		Player p = helper.makeMockPlayer(GameType.SURVIVAL);
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
		return p;
	}

	/** A netherite pickaxe gets nowhere on it (it can't be mined), and a blast beside it leaves it standing. */
	private static void assertLocked(GameTestHelper helper, BlockPos pos, Player player, String what) {
		BlockPos abs = helper.absolutePos(pos);
		BlockState state = helper.getBlockState(pos);
		if (state.getDestroyProgress(player, helper.getLevel(), abs) != 0)
			helper.fail(what + " can be mined while locked");
		if (state.getDestroySpeed(helper.getLevel(), abs) != -1)
			helper.fail(what + " doesn't read as unbreakable to contraptions");
	}

	private static void blast(GameTestHelper helper, BlockPos pos) {
		Vec3 at = Vec3.atCenterOf(helper.absolutePos(pos));
		helper.getLevel().explode(null, at.x, at.y, at.z, 4f, Level.ExplosionInteraction.TNT);
	}

	private static List<ItemEntity> drops(GameTestHelper helper, BlockPos pos, double reach) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(reach));
	}

	// ---------------------------------------------------------------- the Substation

	/** 999,999 FE leaves it dormant; the millionth wakes its district for good, unlocks it, and reads 15 on a comparator. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void substationWakesAtExactlyTheWakeCharge(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 2);
		SubstationBlockEntity be = substation(helper, at, District.FOUNDRY);
		IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(at), Direction.SOUTH);
		int wake = CMConfig.SERVER.wastesWakeCharge();
		helper.runAtTickTime(2, () -> {
			int took = 0;
			// a Battery's or a line's deliveries, in pieces
			for (int left = wake - 1; left > 0; ) {
				int got = energy.receiveEnergy(Math.min(left, 100_000), false);
				took += got;
				left -= got;
			}
			if (took != wake - 1)
				helper.fail("took " + took + ", not " + (wake - 1));
		});
		helper.runAtTickTime(30, () -> {
			if (be.isWoken() || record(helper).awake(origin(helper), District.FOUNDRY))
				helper.fail("woke a joule short of the wake charge");
			if (!helper.getBlockState(at).getValue(SubstationBlock.LOCKED))
				helper.fail("unlocked before waking");
			if (energy.receiveEnergy(5, false) != 1)
				helper.fail("took more than the last joule it needed");
		});
		helper.runAtTickTime(45, () -> {
			if (!be.isWoken() || !record(helper).awake(origin(helper), District.FOUNDRY))
				helper.fail("didn't wake at the wake charge");
			if (helper.getBlockState(at).getValue(SubstationBlock.LOCKED) || helper.getBlockState(at.above()).getValue(SubstationBlock.LOCKED))
				helper.fail("still locked after waking");
			if (be.comparator() != 15)
				helper.fail("the comparator reads " + be.comparator() + " woken");
			helper.succeed();
		});
	}

	/** Woken, it pays its upkeep from what it's given: fed while it's fed, unfed (lamps out) once it isn't. */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void substationUpkeepFeedsTheDistrict(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 2);
		SubstationBlockEntity be = substation(helper, at, District.WORKSHOPS);
		IEnergyStorage energy = be.energy();
		helper.runAtTickTime(2, () -> {
			for (int i = 0; i < 20 && !be.isWoken(); i++)
				energy.receiveEnergy(100_000, false);
		});
		// a second's upkeep each second for a while, then nothing
		for (int t = 25; t <= 85; t += 10)
			helper.runAtTickTime(t, () -> energy.receiveEnergy(CMConfig.SERVER.wastesUpkeep() * 20, false));
		helper.runAtTickTime(90, () -> {
			if (!record(helper).fed(origin(helper), District.WORKSHOPS, helper.getLevel().getGameTime()))
				helper.fail("not fed while its upkeep came in");
		});
		helper.runAtTickTime(150, () -> {
			if (record(helper).fed(origin(helper), District.WORKSHOPS, helper.getLevel().getGameTime()))
				helper.fail("still fed with no power coming in");
			if (!record(helper).awake(origin(helper), District.WORKSHOPS))
				helper.fail("went back to sleep unfed (it wakes for good)");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- locks

	/** A dormant district's vault, door and Substation, and a pylon, can't be mined or blown up. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void lockedBlocksCantBeBrokenOrBlown(GameTestHelper helper) {
		BlockPos vault = new BlockPos(3, 1, 3), sub = new BlockPos(8, 1, 3), door = new BlockPos(13, 1, 3), pylon = new BlockPos(18, 1, 3);
		helper.setBlock(vault, ModWastes.CITY_VAULT.get().defaultBlockState());
		bind(helper, vault, District.QUARTERS);
		substation(helper, sub, District.QUARTERS);
		door(helper, door, false);
		bind(helper, door, District.QUARTERS);
		pylon(helper, pylon);
		bind(helper, pylon, District.HEART);
		Player player = player(helper);
		helper.runAtTickTime(3, () -> {
			assertLocked(helper, vault, player, "the vault");
			assertLocked(helper, sub, player, "the Substation");
			assertLocked(helper, sub.above(), player, "the Substation's top");
			assertLocked(helper, door.above(), player, "the door");
			assertLocked(helper, pylon.above(2), player, "the pylon");
			for (BlockPos p : List.of(vault, sub, door, pylon))
				blast(helper, p.north());
		});
		helper.runAtTickTime(10, () -> {
			helper.assertBlockPresent(ModWastes.CITY_VAULT.get(), vault);
			helper.assertBlockPresent(ModWastes.SUBSTATION.get(), sub.above());
			for (int c = 0; c < 2; c++)
				for (int r = 0; r < 3; r++)
					helper.assertBlockPresent(ModWastes.SLIDING_DOOR.get(), SlidingDoorBlock.part(door, Direction.NORTH, c, r));
			helper.assertBlockPresent(ModWastes.CONDUIT_PYLON.get(), pylon.above(2));
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the vaults and doors

	/** Its district wakes: the iris opens, it's an ordinary block (minable), and it holds its loot with one Unknown Device. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void vaultOpensWhenItsDistrictWakes(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 1, 3);
		helper.setBlock(at, ModWastes.CITY_VAULT.get().defaultBlockState());
		bind(helper, at, District.RAIL_YARD);
		helper.runAtTickTime(3, () -> {
			if (!helper.getBlockState(at).getValue(CityVaultBlock.LOCKED))
				helper.fail("open before its district woke");
			record(helper).wake(helper.getLevel(), origin(helper), District.FOUNDRY, null);
		});
		helper.runAtTickTime(8, () -> {
			if (!helper.getBlockState(at).getValue(CityVaultBlock.LOCKED))
				helper.fail("another district's waking opened it");
			record(helper).wake(helper.getLevel(), origin(helper), District.RAIL_YARD, null);
		});
		helper.runAtTickTime(14, () -> {
			if (helper.getBlockState(at).getValue(CityVaultBlock.LOCKED))
				helper.fail("still locked after its district woke");
			if (helper.getBlockState(at).getDestroyProgress(player(helper), helper.getLevel(), helper.absolutePos(at)) <= 0)
				helper.fail("can't be mined once open");
			CityVaultBlockEntity be = (CityVaultBlockEntity) helper.getBlockEntity(at);
			be.rollLoot(null);
			int devices = 0;
			for (int i = 0; i < be.container().getContainerSize(); i++)
				if (be.container().getItem(i).is(ModWastes.UNKNOWN_DEVICE.get()))
					devices += be.container().getItem(i).getCount();
			if (devices != 1)
				helper.fail("a district vault holds " + devices + " Unknown Devices, not 1");
			helper.succeed();
		});
	}

	/**
	 * P48: a vault that loads after its district woke (its chunk was away) reads the record and opens, though it heard
	 * nothing: loaded here from saved data, as a chunk loads it.
	 */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void vaultLoadedAfterWakingCatchesUp(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 1, 3);
		record(helper).wake(helper.getLevel(), origin(helper), District.QUARTERS, null);
		BlockState state = ModWastes.CITY_VAULT.get().defaultBlockState();
		helper.setBlock(at, state);
		// as saved in a structure or an unloaded chunk: its link in its NBT, still locked
		CompoundTag tag = new CompoundTag();
		tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ModWastes.CITY_VAULT_BE.get()).toString());
		CityLink link = new CityLink();
		link.set(origin(helper), District.QUARTERS);
		link.write(tag);
		BlockPos abs = helper.absolutePos(at);
		BlockEntity loaded = BlockEntity.loadStatic(abs, state, tag, helper.getLevel().registryAccess());
		helper.getLevel().setBlockEntity(loaded);
		helper.runAtTickTime(10, () -> {
			if (helper.getBlockState(at).getValue(CityVaultBlock.LOCKED))
				helper.fail("loaded after its district woke, it stayed locked");
			helper.succeed();
		});
	}

	/** The heart vault stays shut with all four awake (the Custodian guards it), and opens on its death; one Elytra inside. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void heartVaultOpensOnTheCustodiansDeath(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 1, 3);
		helper.setBlock(at, ModWastes.CITY_VAULT.get().defaultBlockState().setValue(CityVaultBlock.HEART, true));
		bind(helper, at, District.HEART);
		helper.runAtTickTime(3, () -> record(helper).wakeAll(helper.getLevel(), origin(helper), null));
		helper.runAtTickTime(10, () -> {
			if (!helper.getBlockState(at).getValue(CityVaultBlock.LOCKED))
				helper.fail("the heart vault opened with the Custodian alive");
			record(helper).custodianDied(helper.getLevel(), origin(helper));
		});
		helper.runAtTickTime(16, () -> {
			if (helper.getBlockState(at).getValue(CityVaultBlock.LOCKED))
				helper.fail("the heart vault stayed locked after the Custodian died");
			CityVaultBlockEntity be = (CityVaultBlockEntity) helper.getBlockEntity(at);
			be.rollLoot(null);
			int elytra = 0, devices = 0;
			for (int i = 0; i < be.container().getContainerSize(); i++) {
				ItemStack s = be.container().getItem(i);
				if (s.is(Items.ELYTRA))
					elytra += s.getCount();
				if (s.is(ModWastes.UNKNOWN_DEVICE.get()))
					devices += s.getCount();
			}
			if (elytra != 1 || devices != 2)
				helper.fail("the heart vault holds " + elytra + " Elytra and " + devices + " Unknown Devices, not 1 and 2");
			helper.succeed();
		});
	}

	/** On Peaceful the Custodian never wakes: the heart vault opens once all four districts are awake (P41). */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void heartVaultOpensOnPeacefulAfterFourWakes(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 1, 3);
		CityRecord.PEACEFUL_FOR_TESTS.add(origin(helper));
		helper.setBlock(at, ModWastes.CITY_VAULT.get().defaultBlockState().setValue(CityVaultBlock.HEART, true));
		bind(helper, at, District.HEART);
		helper.runAtTickTime(3, () -> {
			for (District d : new District[] {District.FOUNDRY, District.WORKSHOPS, District.RAIL_YARD})
				record(helper).wake(helper.getLevel(), origin(helper), d, null);
		});
		helper.runAtTickTime(8, () -> {
			if (!helper.getBlockState(at).getValue(CityVaultBlock.LOCKED))
				helper.fail("opened with three districts awake");
			record(helper).wake(helper.getLevel(), origin(helper), District.QUARTERS, null);
		});
		helper.runAtTickTime(14, () -> {
			CityRecord.PEACEFUL_FOR_TESTS.remove(origin(helper));
			if (helper.getBlockState(at).getValue(CityVaultBlock.LOCKED))
				helper.fail("on Peaceful, the heart vault stayed locked with all four awake");
			helper.succeed();
		});
	}

	/** Its district wakes: every part opens at once (nothing stops anyone), and it can be broken; a heart door waits for all four. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void doorOpensWhenItsDistrictWakes(GameTestHelper helper) {
		BlockPos district = new BlockPos(3, 1, 3), heart = new BlockPos(10, 1, 3);
		door(helper, district, false);
		bind(helper, district, District.FOUNDRY);
		door(helper, heart, true);
		bind(helper, heart, District.HEART);
		helper.runAtTickTime(3, () -> record(helper).wake(helper.getLevel(), origin(helper), District.FOUNDRY, null));
		helper.runAtTickTime(10, () -> {
			for (int c = 0; c < 2; c++)
				for (int r = 0; r < 3; r++) {
					BlockPos p = SlidingDoorBlock.part(district, Direction.NORTH, c, r);
					BlockState s = helper.getBlockState(p);
					if (!s.getValue(SlidingDoorBlock.OPEN))
						helper.fail("part " + c + "," + r + " didn't open");
					if (!s.getCollisionShape(helper.getLevel(), helper.absolutePos(p)).isEmpty())
						helper.fail("an open door still stops things");
				}
			if (helper.getBlockState(district).getDestroyProgress(player(helper), helper.getLevel(), helper.absolutePos(district)) <= 0)
				helper.fail("an open door can't be broken");
			if (helper.getBlockState(heart).getValue(SlidingDoorBlock.OPEN))
				helper.fail("a heart door opened with one district awake");
			record(helper).wakeAll(helper.getLevel(), origin(helper), null);
		});
		helper.runAtTickTime(16, () -> {
			if (!helper.getBlockState(heart.above(2)).getValue(SlidingDoorBlock.OPEN))
				helper.fail("a heart door stayed shut with all four awake");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the lamps

	/** A woken, fed district's lamp lights (as bright as an Electric Light) and its cable's line; unfed, they go dark. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void lampsFollowTheirDistrictsFeed(GameTestHelper helper) {
		BlockPos foot = new BlockPos(3, 1, 3), cable = new BlockPos(5, 1, 3);
		BlockState post = ModWastes.DERELICT_STREET_LAMP.get().defaultBlockState();
		helper.setBlock(foot, post);
		helper.setBlock(foot.above(), post);
		helper.setBlock(foot.above(2), post);
		helper.setBlock(cable, ModWastes.DERELICT_CABLE.get().defaultBlockState());
		for (BlockPos p : List.of(foot, foot.above(), foot.above(2), cable))
			bind(helper, p, District.QUARTERS);
		BlockPos head = foot.above(2);
		helper.runAtTickTime(3, () -> {
			if (helper.getBlockState(head).getValue(DerelictStreetLampBlock.PART) != DerelictStreetLampBlock.Part.HEAD
				|| helper.getBlockState(foot).getValue(DerelictStreetLampBlock.PART) != DerelictStreetLampBlock.Part.FOOT)
				helper.fail("the post didn't stack into foot, pole and head");
			CityRecord r = record(helper);
			r.wake(helper.getLevel(), origin(helper), District.QUARTERS, helper.absolutePos(foot));
			r.setFed(helper.getLevel(), origin(helper), District.QUARTERS, true, helper.absolutePos(foot));
		});
		helper.runAtTickTime(40, () -> {
			if (!helper.getBlockState(head).getValue(BlockStateProperties.LIT) || helper.getBlockState(head).getLightEmission() != 15)
				helper.fail("the head didn't light at full brightness");
			if (helper.getBlockState(foot).getValue(BlockStateProperties.LIT))
				helper.fail("the foot lit");
			if (!helper.getBlockState(cable).getValue(BlockStateProperties.LIT))
				helper.fail("the cable's line didn't light");
			record(helper).setFed(helper.getLevel(), origin(helper), District.QUARTERS, false, null);
		});
		helper.runAtTickTime(50, () -> {
			if (((DerelictLightBlockEntity) helper.getBlockEntity(head)).phase() != DerelictLightBlockEntity.Phase.FADING)
				helper.fail("the head didn't flicker as it starved");
		});
		helper.runAtTickTime(80, () -> {
			if (helper.getBlockState(head).getValue(BlockStateProperties.LIT) || helper.getBlockState(cable).getValue(BlockStateProperties.LIT))
				helper.fail("still lit unfed");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the Star Reactor

	/** A star in (through a funnel's handler): 8,192 FE every tick into its store, a tick of burn each; redstone pauses both. */
	@GameTest(template = COURSE, timeoutTicks = 80)
	public static void starReactorRunsFlatOut(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 2, 3);
		helper.setBlock(at, ModWastes.STAR_REACTOR.get().defaultBlockState().setValue(StarReactorBlock.FACING, Direction.UP));
		long[] at0 = new long[3];
		helper.runAtTickTime(2, () -> {
			IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(at), Direction.NORTH);
			ItemStack left = items.insertItem(0, new ItemStack(Items.NETHER_STAR, 2), false);
			if (left.getCount() != 1)
				helper.fail("took " + (2 - left.getCount()) + " stars, not one");
		});
		helper.runAtTickTime(5, () -> {
			StarReactorBlockEntity be = (StarReactorBlockEntity) helper.getBlockEntity(at);
			at0[0] = be.energy().getEnergyStored();
			at0[1] = be.burn();
		});
		helper.runAtTickTime(25, () -> {
			StarReactorBlockEntity be = (StarReactorBlockEntity) helper.getBlockEntity(at);
			long made = be.energy().getEnergyStored() - at0[0], burned = at0[1] - be.burn();
			if (made != 20L * CMConfig.SERVER.starReactorOutput())
				helper.fail("made " + made + " FE in 20 ticks, not " + 20L * CMConfig.SERVER.starReactorOutput());
			if (burned != 20)
				helper.fail("burned " + burned + " ticks of star in 20");
			helper.setBlock(at.east(), Blocks.REDSTONE_BLOCK);
		});
		helper.runAtTickTime(30, () -> at0[2] = ((StarReactorBlockEntity) helper.getBlockEntity(at)).burn());
		helper.runAtTickTime(50, () -> {
			StarReactorBlockEntity be = (StarReactorBlockEntity) helper.getBlockEntity(at);
			if (be.burn() != at0[2] || !be.isPaused())
				helper.fail("redstone didn't pause the burn");
			helper.succeed();
		});
	}

	/** Broken mid-burn, it drops with its burn and upgrade; placed again, it carries on from there. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void starReactorKeepsItsBurnWhenBroken(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 2, 3), again = new BlockPos(8, 2, 3);
		helper.setBlock(at, ModWastes.STAR_REACTOR.get().defaultBlockState());
		long[] burn = new long[1];
		helper.runAtTickTime(2, () -> {
			StarReactorBlockEntity be = (StarReactorBlockEntity) helper.getBlockEntity(at);
			be.fitUpgrade();
			be.insertStar(null);
		});
		helper.runAtTickTime(12, () -> {
			burn[0] = ((StarReactorBlockEntity) helper.getBlockEntity(at)).burn();
			helper.getLevel().destroyBlock(helper.absolutePos(at), true);
		});
		helper.runAtTickTime(14, () -> {
			List<ItemEntity> drops = drops(helper, at, 1.5);
			if (drops.size() != 1)
				helper.fail("dropped " + drops.size() + " stacks");
			ItemStack stack = drops.get(0).getItem();
			long kept = stack.getOrDefault(ModWastes.STAR_BURN.get(), 0L);
			if (Math.abs(kept - burn[0]) > 2 || !stack.getOrDefault(ModWastes.REACTOR_OVERHAULED.get(), false))
				helper.fail("the item kept " + kept + " of " + burn[0] + " ticks, upgrade " + stack.get(ModWastes.REACTOR_OVERHAULED.get()));
			drops.forEach(ItemEntity::discard);
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			// clicked on the empty space itself (air is replaced where it's clicked)
			BlockPos there = helper.absolutePos(again);
			stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(there), Direction.UP, there, false)));
		});
		helper.runAtTickTime(16, () -> {
			if (!(helper.getBlockEntity(again) instanceof StarReactorBlockEntity be))
				throw new net.minecraft.gametest.framework.GameTestAssertException("not placed again");
			if (Math.abs(be.burn() - burn[0]) > 6 || !be.isUpgraded())
				helper.fail("placed again with " + be.burn() + " ticks (upgraded " + be.isUpgraded() + "), not ~" + burn[0]);
			helper.succeed();
		});
	}

	/** Overhauled, a star burns 12 hours (8 x 1.5); fitted mid-burn, the star in it stretches too. */
	@GameTest(template = COURSE, timeoutTicks = 30)
	public static void starReactorUpgradeGivesTwelveHours(GameTestHelper helper) {
		BlockPos plain = new BlockPos(3, 2, 3), upgraded = new BlockPos(6, 2, 3);
		helper.setBlock(plain, ModWastes.STAR_REACTOR.get().defaultBlockState());
		helper.setBlock(upgraded, ModWastes.STAR_REACTOR.get().defaultBlockState());
		helper.runAtTickTime(2, () -> {
			StarReactorBlockEntity a = (StarReactorBlockEntity) helper.getBlockEntity(plain), b = (StarReactorBlockEntity) helper.getBlockEntity(upgraded);
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			// the Unknown Device by hand, as a player (or a Deployer) uses it
			ItemStack device = new ItemStack(ModWastes.UNKNOWN_DEVICE.get(), 2);
			player.setItemInHand(InteractionHand.MAIN_HAND, device);
			BlockPos abs = helper.absolutePos(upgraded);
			helper.getBlockState(upgraded).useItemOn(device, helper.getLevel(), player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false));
			if (!b.isUpgraded() || device.getCount() != 1)
				helper.fail("the Unknown Device wasn't fitted (or not used up)");
			helper.getBlockState(upgraded).useItemOn(device, helper.getLevel(), player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false));
			if (device.getCount() != 1)
				helper.fail("a second Unknown Device went in");
			a.insertStar(null);
			b.insertStar(null);
			if (a.burn() != 8L * StarReactorBlockEntity.TICKS_PER_HOUR || b.burn() != 12L * StarReactorBlockEntity.TICKS_PER_HOUR)
				helper.fail("stars burn " + a.burn() + " and " + b.burn() + " ticks, not 8 h and 12 h");
			a.fitUpgrade();
			if (Math.abs(a.burn() - 12L * StarReactorBlockEntity.TICKS_PER_HOUR) > 2)
				helper.fail("fitted mid-burn, the star's " + a.burn() + " ticks didn't stretch to 12 h");
			// and it can be built: its Mechanical Crafter recipe loaded
			if (helper.getLevel().getRecipeManager().byKey(ComedyMonkeysCreateAddons.asResource("mechanical_crafting/star_reactor")).isEmpty())
				helper.fail("no Star Reactor recipe");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the heart's reactor

	/** A star in through any of its blocks wakes all four districts and burns for the city; a line can draw it; the wrench salvages it. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void derelictReactorWakesTheCityAndSalvages(GameTestHelper helper) {
		BlockPos controller = new BlockPos(6, 1, 6);
		BlockState state = ModWastes.DERELICT_STAR_REACTOR.get().defaultBlockState();
		for (int p = 0; p < 27; p++)
			helper.setBlock(DerelictStarReactorBlock.partAt(controller, p), state.setValue(DerelictStarReactorBlock.PART, p));
		bind(helper, controller, District.HEART);
		BlockPos port = DerelictStarReactorBlock.partAt(controller, 1);
		helper.runAtTickTime(3, () -> {
			IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(controller.above(2)), Direction.UP);
			if (items == null || !items.insertItem(0, new ItemStack(Items.NETHER_STAR), false).isEmpty())
				helper.fail("a star wouldn't go in from the top");
			CityRecord r = record(helper);
			if (!r.allAwake(origin(helper)) || !r.heartBurning(origin(helper), helper.getLevel().getGameTime()))
				helper.fail("the star didn't wake all four and burn for the heart");
			if (!r.fed(origin(helper), District.FOUNDRY, helper.getLevel().getGameTime()))
				helper.fail("the heart's star doesn't feed the districts' lamps");
		});
		helper.runAtTickTime(10, () -> {
			IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(port), Direction.NORTH);
			if (e == null || e.extractEnergy(1000, true) != 1000)
				helper.fail("a line at a port can't draw its spare power");
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setShiftKeyDown(true);
			ItemStack wrench = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench")));
			player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
			BlockPos abs = helper.absolutePos(port);
			wrench.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false)));
		});
		helper.runAtTickTime(14, () -> {
			if (!helper.getBlockState(controller).getValue(DerelictStarReactorBlock.SALVAGED) || !helper.getBlockState(controller.above(2)).getValue(DerelictStarReactorBlock.SALVAGED))
				helper.fail("not salvaged");
			int devices = drops(helper, controller, 3).stream().filter(e -> e.getItem().is(ModWastes.UNKNOWN_DEVICE.get())).mapToInt(e -> e.getItem().getCount()).sum();
			if (devices != 2)
				helper.fail("salvage gave " + devices + " Unknown Devices, not 2");
			if (record(helper).heartBurning(origin(helper), helper.getLevel().getGameTime()))
				helper.fail("the heart still burns from a salvaged reactor");
			if (!((DerelictStarReactorBlockEntity) helper.getBlockEntity(controller)).isSalvaged())
				helper.fail("its block entity doesn't know");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the pylons

	/**
	 * Hits do nothing before the Custodian wakes; after, six break it (five don't), by swing or arrow; re-lit, it needs
	 * the full six again (D43), and two swings inside 8 ticks count once.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void pylonBreaksAfterSixHitsOnceTheCustodianWakes(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 1, 3);
		pylon(helper, at);
		bind(helper, at, District.HEART);
		Player player = player(helper);
		ServerLevel level = helper.getLevel();
		Runnable swing = () -> helper.getBlockState(at.above(2)).attack(level, helper.absolutePos(at.above(2)), player);
		// (each a runnable of its own: the helper keeps one time per runnable)
		helper.runAtTickTime(3, () -> swing.run());
		helper.runAtTickTime(15, () -> swing.run());
		helper.runAtTickTime(25, () -> {
			ConduitPylonBlockEntity be = (ConduitPylonBlockEntity) helper.getBlockEntity(at);
			if (be.hits() != 0)
				helper.fail("harmed before the Custodian woke");
			record(helper).wakeCustodian(level, origin(helper));
		});
		int hits = ConduitPylonBlockEntity.maxHits();
		for (int i = 0; i < hits - 1; i++)
			helper.runAtTickTime(30 + i * 12, () -> swing.run());
		helper.runAtTickTime(30 + (hits - 1) * 12 - 6, () -> {
			if (!((ConduitPylonBlockEntity) helper.getBlockEntity(at)).isStanding())
				helper.fail("broke before " + hits + " hits");
		});
		// the last by arrow
		helper.runAtTickTime(30 + (hits - 1) * 12, () -> {
			Arrow arrow = new Arrow(EntityType.ARROW, level);
			arrow.setOwner(player);
			BlockPos abs = helper.absolutePos(at.above(2));
			BlockState s = helper.getBlockState(at.above(2));
			s.onProjectileHit(level, s, new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false), arrow);
		});
		helper.runAtTickTime(30 + hits * 12, () -> {
			ConduitPylonBlockEntity be = (ConduitPylonBlockEntity) helper.getBlockEntity(at);
			if (be.isStanding())
				helper.fail("still standing after " + be.hits() + " hits");
			helper.assertBlockPresent(ModWastes.CONDUIT_PYLON.get(), at.above(2));
			if (!be.relight() || !be.isStanding())
				helper.fail("didn't re-light");
			if (be.hits() != 0)
				helper.fail("re-lit with " + be.hits() + " hits already counted: it needs the full " + hits + " again");
		});
		// two swings 4 ticks apart: one hit (D43: at most one every 8 ticks)
		int relit = 30 + hits * 12;
		helper.runAtTickTime(relit + 10, () -> swing.run());
		helper.runAtTickTime(relit + 14, () -> swing.run());
		helper.runAtTickTime(relit + 16, () -> {
			if (((ConduitPylonBlockEntity) helper.getBlockEntity(at)).hits() != 1)
				helper.fail("two swings inside 8 ticks counted " + ((ConduitPylonBlockEntity) helper.getBlockEntity(at)).hits() + " times");
		});
		for (int i = 1; i < hits - 1; i++)
			helper.runAtTickTime(relit + 20 + i * 10, () -> swing.run());
		helper.runAtTickTime(relit + 20 + (hits - 1) * 10 - 3, () -> {
			if (!((ConduitPylonBlockEntity) helper.getBlockEntity(at)).isStanding())
				helper.fail("a re-lit pylon broke before " + hits + " hits");
		});
		helper.runAtTickTime(relit + 20 + (hits - 1) * 10, () -> swing.run());
		helper.runAtTickTime(relit + 20 + (hits - 1) * 10 + 2, () -> {
			if (((ConduitPylonBlockEntity) helper.getBlockEntity(at)).isStanding())
				helper.fail("a re-lit pylon still stands after " + hits + " hits");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the user's decisions of 2026-10-29 (D42-D48)

	/** A street lamp post of three blocks at foot, bound to a district; returns its head. */
	private static BlockPos lampPost(GameTestHelper helper, BlockPos foot, District district) {
		BlockState post = ModWastes.DERELICT_STREET_LAMP.get().defaultBlockState();
		for (int i = 0; i < 3; i++)
			helper.setBlock(foot.above(i), post);
		for (int i = 0; i < 3; i++)
			bind(helper, foot.above(i), district);
		return foot.above(2);
	}

	private static boolean lit(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(BlockStateProperties.LIT);
	}

	/**
	 * D42: locked, a vault, a Substation and a door are bedrock to Create (no drill breaks them, no contraption takes
	 * them); woken, they're ordinary blocks at their own hardness, which drills break and contraptions move.
	 */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void wokenBlocksAreOrdinaryForContraptions(GameTestHelper helper) {
		BlockPos vault = new BlockPos(3, 1, 3), sub = new BlockPos(8, 1, 3), door = new BlockPos(13, 1, 3);
		helper.setBlock(vault, ModWastes.CITY_VAULT.get().defaultBlockState());
		bind(helper, vault, District.RAIL_YARD);
		substation(helper, sub, District.RAIL_YARD);
		door(helper, door, false);
		bind(helper, door, District.RAIL_YARD);
		ServerLevel level = helper.getLevel();
		List<BlockPos> all = List.of(vault, sub, sub.above(), door, door.above(2));
		helper.runAtTickTime(3, () -> {
			for (BlockPos p : all) {
				BlockState s = helper.getBlockState(p);
				BlockPos abs = helper.absolutePos(p);
				if (BlockBreakingKineticBlockEntity.isBreakable(s, s.getDestroySpeed(level, abs)) || BlockMovementChecks.isMovementAllowed(s, level, abs))
					helper.fail("locked " + s.getBlock().getName().getString() + " at " + p + " is open to contraptions");
			}
			record(helper).wake(level, origin(helper), District.RAIL_YARD, helper.absolutePos(sub));
		});
		helper.runAtTickTime(20, () -> {
			java.util.Map<BlockPos, Float> hardness = java.util.Map.of(vault, CityVaultBlock.HARDNESS, sub, SubstationBlock.HARDNESS,
				sub.above(), SubstationBlock.HARDNESS, door, SlidingDoorBlock.HARDNESS, door.above(2), SlidingDoorBlock.HARDNESS);
			for (BlockPos p : all) {
				BlockState s = helper.getBlockState(p);
				BlockPos abs = helper.absolutePos(p);
				float h = s.getDestroySpeed(level, abs);
				if (h != hardness.get(p))
					helper.fail("woken " + s.getBlock().getName().getString() + " at " + p + " has hardness " + h + ", not " + hardness.get(p));
				if (!BlockBreakingKineticBlockEntity.isBreakable(s, h))
					helper.fail("a drill can't break the woken " + s.getBlock().getName().getString());
				if (!BlockMovementChecks.isMovementAllowed(s, level, abs))
					helper.fail("a contraption can't move the woken " + s.getBlock().getName().getString());
			}
			helper.succeed();
		});
	}

	/** D44: a star in the heart lights the whole city, a district whose Substation was broken included. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void heartStarLightsADistrictWhoseSubstationWasBroken(GameTestHelper helper) {
		BlockPos head = lampPost(helper, new BlockPos(3, 1, 3), District.FOUNDRY);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(3, () -> {
			CityRecord r = record(helper);
			r.wake(level, origin(helper), District.FOUNDRY, null);
			r.cut(level, origin(helper), District.FOUNDRY);
			if (r.fed(origin(helper), District.FOUNDRY, level.getGameTime()))
				helper.fail("a cut district is fed with no heart burning");
			r.burnHeart(level, origin(helper), level.getGameTime() + 10_000, null);
		});
		helper.runAtTickTime(40, () -> {
			if (!record(helper).fed(origin(helper), District.FOUNDRY, level.getGameTime()))
				helper.fail("the heart's star doesn't feed the cut district");
			if (!lit(helper, head))
				helper.fail("the cut district's lamp is dark with the heart burning");
			helper.succeed();
		});
	}

	/**
	 * D45: the lamps ripple on at the wake itself, with no upkeep paid yet; with none coming after, they flicker out once
	 * the first second's upkeep falls due.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void lampsRippleOnAtTheWake(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 2);
		SubstationBlockEntity be = substation(helper, at, District.WORKSHOPS);
		BlockPos head = lampPost(helper, new BlockPos(5, 1, 2), District.WORKSHOPS);
		helper.runAtTickTime(2, () -> {
			if (be.energy().receiveEnergy(CMConfig.SERVER.wastesWakeCharge(), false) != CMConfig.SERVER.wastesWakeCharge())
				helper.fail("didn't take the wake charge");
		});
		helper.runAtTickTime(14, () -> {
			if (!be.isWoken())
				helper.fail("didn't wake");
			if (!lit(helper, head))
				helper.fail("the lamp didn't light at the wake");
		});
		helper.runAtTickTime(90, () -> {
			if (lit(helper, head))
				helper.fail("still lit with no upkeep after the wake");
			if (!record(helper).awake(origin(helper), District.WORKSHOPS))
				helper.fail("went back to sleep");
			helper.succeed();
		});
	}

	/**
	 * D47: placed, its front faces the placer whichever of the six ways they look, its copper terminal (FACING) on the far
	 * side, whatever face was clicked.
	 */
	@GameTest(template = COURSE, timeoutTicks = 10)
	public static void starReactorFacesItsPlacer(GameTestHelper helper) {
		Player player = player(helper);
		ItemStack stack = new ItemStack(ModWastes.STAR_REACTOR.get());
		BlockPos abs = helper.absolutePos(new BlockPos(3, 2, 3));
		// look (yaw, pitch) and the terminal it gives, clicking the floor's top each time
		float[][] looks = { { 0, 0 }, { 90, 0 }, { 180, 0 }, { -90, 0 }, { 0, 90 }, { 0, -90 } };
		Direction[] terminal = { Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.DOWN, Direction.UP };
		for (int i = 0; i < looks.length; i++) {
			player.setYRot(looks[i][0]);
			player.setYHeadRot(looks[i][0]);
			player.setXRot(looks[i][1]);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs.below(), false);
			BlockState s = ModWastes.STAR_REACTOR.get().getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(player,
				InteractionHand.MAIN_HAND, stack, hit));
			if (s == null || s.getValue(StarReactorBlock.FACING) != terminal[i])
				helper.fail("looking " + java.util.Arrays.toString(looks[i]) + " put its terminal " + (s == null ? null : s.getValue(StarReactorBlock.FACING))
					+ ", not " + terminal[i] + " (front toward the placer)");
		}
		helper.succeed();
	}

	/**
	 * D54: the Custodian's overload surges every heart lamp, lit or dark, for about 3 s (a look: its light and phase stay
	 * as they were), and no district's.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void overloadSurgesTheHeartLamps(GameTestHelper helper) {
		BlockPos heart = lampPost(helper, new BlockPos(3, 1, 3), District.HEART), foundry = lampPost(helper, new BlockPos(6, 1, 3), District.FOUNDRY);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(3, () -> record(helper).surgeHeart(level, origin(helper)));
		helper.runAtTickTime(5, () -> {
			DerelictLightBlockEntity h = (DerelictLightBlockEntity) helper.getBlockEntity(heart), f = (DerelictLightBlockEntity) helper.getBlockEntity(foundry);
			if (!h.surging(level.getGameTime()))
				helper.fail("the dark heart lamp didn't surge");
			if (f.surging(level.getGameTime()))
				helper.fail("a district's lamp surged with the heart");
			if (lit(helper, heart) || h.phase() != DerelictLightBlockEntity.Phase.DARK)
				helper.fail("the surge changed the lamp's light");
		});
		helper.runAtTickTime(3 + DerelictLightBlockEntity.SURGE_TICKS + 2, () -> {
			if (((DerelictLightBlockEntity) helper.getBlockEntity(heart)).surging(level.getGameTime()))
				helper.fail("still surging after " + DerelictLightBlockEntity.SURGE_TICKS + " ticks");
			helper.succeed();
		});
	}

	/** A block placed as a player would, clicking {@code on}'s face {@code face} (looking along {@code look}). */
	private static BlockState placed(GameTestHelper helper, net.minecraft.world.level.block.Block block, BlockPos on, Direction face, float yaw, float pitch) {
		Player player = player(helper);
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setXRot(pitch);
		BlockPos abs = helper.absolutePos(on);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false);
		return block.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block), hit));
	}

	/**
	 * Derelict Cable on a wall and a ceiling (as the floor's, turned): placed on the face clicked; a wall run joins the
	 * cable above and below it on the same wall (not a floor cable beside it), runs level alone, falls with its wall, and
	 * lights with its district as the floor's does.
	 */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void cableRunsOnWallsAndCeilings(GameTestHelper helper) {
		BlockPos wall = new BlockPos(3, 1, 4), a = wall.north(), roof = new BlockPos(8, 4, 3);
		for (int y = 1; y <= 4; y++)
			helper.setBlock(wall.atY(y), Blocks.STONE);
		helper.setBlock(roof, Blocks.STONE);
		net.minecraft.world.level.block.Block cable = ModWastes.DERELICT_CABLE.get();
		// clicking the wall's north face from the north: it lies on the south side of its block, against the wall
		BlockState onWall = placed(helper, cable, wall, Direction.NORTH, 180, 0);
		if (onWall == null || onWall.getValue(DerelictCableBlock.SURFACE) != Direction.SOUTH)
			helper.fail("placed on a wall it lies on " + (onWall == null ? null : onWall.getValue(DerelictCableBlock.SURFACE)) + ", not against the wall");
		else if (!onWall.getValue(DerelictCableBlock.EAST) || !onWall.getValue(DerelictCableBlock.WEST) || onWall.getValue(DerelictCableBlock.UP))
			helper.fail("alone on a wall it should run level, east to west");
		BlockState under = placed(helper, cable, roof, Direction.DOWN, 0, -90);
		if (under == null || under.getValue(DerelictCableBlock.SURFACE) != Direction.UP)
			helper.fail("placed under a ceiling it lies on " + (under == null ? null : under.getValue(DerelictCableBlock.SURFACE)));
		helper.setBlock(a, onWall);
		helper.setBlock(a.above(), onWall);
		helper.setBlock(roof.below(), under);
		// a floor cable beside the wall run: not joined (it's on another side)
		helper.setBlock(a.east(), cable.defaultBlockState());
		for (BlockPos p : List.of(a, a.above(), roof.below()))
			bind(helper, p, District.QUARTERS);
		helper.runAtTickTime(3, () -> {
			BlockState s = helper.getBlockState(a);
			if (!s.getValue(DerelictCableBlock.UP) || !s.getValue(DerelictCableBlock.DOWN) || s.getValue(DerelictCableBlock.EAST))
				helper.fail("the wall run should join the cable above it and run on straight (up and down), not the floor cable: " + s);
			ServerLevel level = helper.getLevel();
			record(helper).wake(level, origin(helper), District.QUARTERS, null);
			record(helper).setFed(level, origin(helper), District.QUARTERS, true, null);
		});
		helper.runAtTickTime(40, () -> {
			for (BlockPos p : List.of(a, a.above(), roof.below()))
				if (!lit(helper, p))
					helper.fail("the cable at " + p + " didn't light with its district");
			helper.setBlock(wall.above(), Blocks.AIR);
		});
		helper.runAtTickTime(45, () -> {
			helper.assertBlockNotPresent(ModWastes.DERELICT_CABLE.get(), a.above());
			helper.succeed();
		});
	}

	/**
	 * D41: a leaning post: a block stacked on a leaning one leans with it, every slice has its own leaning model and a
	 * shape that follows it, and its head lights with its district as an upright one's does.
	 */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void leaningLampPostStacksAndLights(GameTestHelper helper) {
		BlockPos foot = new BlockPos(3, 1, 3);
		net.minecraft.world.level.block.Block lamp = ModWastes.DERELICT_STREET_LAMP.get();
		helper.setBlock(foot, lamp.defaultBlockState().setValue(DerelictStreetLampBlock.LEANING, true));
		for (int i = 1; i <= 2; i++) {
			BlockState s = placed(helper, lamp, foot.above(i - 1), Direction.UP, 0, 0);
			if (s == null || !s.getValue(DerelictStreetLampBlock.LEANING))
				helper.fail("a block stacked on a leaning post doesn't lean");
			helper.setBlock(foot.above(i), s);
		}
		for (int i = 0; i < 3; i++)
			bind(helper, foot.above(i), District.FOUNDRY);
		helper.runAtTickTime(3, () -> {
			for (int i = 0; i < 3; i++) {
				BlockState s = helper.getBlockState(foot.above(i));
				var shape = s.getShape(helper.getLevel(), helper.absolutePos(foot.above(i)));
				// (it follows the post as it leans out over the next block south, as the model does)
				if (shape.isEmpty() || shape.max(Direction.Axis.Z) > 2 || shape.min(Direction.Axis.Z) < 0)
					helper.fail("the leaning " + s.getValue(DerelictStreetLampBlock.PART) + "'s shape doesn't follow the post: " + (shape.isEmpty() ? "empty" : shape.bounds()));
			}
			ServerLevel level = helper.getLevel();
			record(helper).wake(level, origin(helper), District.FOUNDRY, null);
			record(helper).setFed(level, origin(helper), District.FOUNDRY, true, null);
		});
		helper.runAtTickTime(40, () -> {
			BlockState head = helper.getBlockState(foot.above(2));
			if (head.getValue(DerelictStreetLampBlock.PART) != DerelictStreetLampBlock.Part.HEAD || !lit(helper, foot.above(2)) || head.getLightEmission() != 15)
				helper.fail("the leaning post's head didn't light at 15");
			helper.succeed();
		});
	}

	/**
	 * The Seamed Void-Alloy Plate's seams and the Charging Alcove's contact plate light while their district is fed and go
	 * dark unfed; ones of no city (a player's) never light, and the plate still drops itself.
	 */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void seamsAndAlcoveLightWithTheirDistrict(GameTestHelper helper) {
		BlockPos plate = new BlockPos(3, 1, 3), loose = new BlockPos(5, 1, 3), alcove = new BlockPos(8, 1, 3);
		helper.setBlock(plate, ModWastes.SEAMED_VOID_ALLOY_PLATE.get().defaultBlockState());
		helper.setBlock(loose, ModWastes.SEAMED_VOID_ALLOY_PLATE.get().defaultBlockState());
		BlockState a = ModWastes.CHARGING_ALCOVE.get().defaultBlockState();
		helper.setBlock(alcove, a);
		helper.setBlock(alcove.above(), a.setValue(ThreePartBlock.PART, ThreePartBlock.Part.MIDDLE));
		helper.setBlock(alcove.above(2), a.setValue(ThreePartBlock.PART, ThreePartBlock.Part.TOP));
		List<BlockPos> city = List.of(plate, alcove, alcove.above(), alcove.above(2));
		for (BlockPos p : city)
			bind(helper, p, District.RAIL_YARD);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(3, () -> {
			record(helper).wake(level, origin(helper), District.RAIL_YARD, null);
			record(helper).setFed(level, origin(helper), District.RAIL_YARD, true, null);
		});
		helper.runAtTickTime(40, () -> {
			for (BlockPos p : city)
				if (!lit(helper, p))
					helper.fail(helper.getBlockState(p).getBlock().getName().getString() + " at " + p + " didn't light with its district");
			if (lit(helper, loose))
				helper.fail("a plate of no city lit");
			record(helper).setFed(level, origin(helper), District.RAIL_YARD, false, null);
		});
		helper.runAtTickTime(80, () -> {
			for (BlockPos p : city)
				if (lit(helper, p))
					helper.fail(helper.getBlockState(p).getBlock().getName().getString() + " at " + p + " still lit unfed");
			if (!(helper.getBlockState(plate).getBlock() instanceof SeamedPlateBlock) || !(helper.getBlockState(alcove).getBlock() instanceof ChargingAlcoveBlock))
				helper.fail("not the lit decor blocks");
			helper.succeed();
		});
	}

	/**
	 * D75: the building block set's glows light while their district is fed and go dark unfed (the Slotted Grille's pane,
	 * the Band's seam, a cracked Scarred Plate's crack); a chipped Scarred Plate never lights, nor a grille of no city.
	 */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void buildingBlocksLightWithTheirDistrict(GameTestHelper helper) {
		BlockPos grille = new BlockPos(2, 1, 3), band = new BlockPos(4, 1, 3), cracked = new BlockPos(6, 1, 3), chipped = new BlockPos(8, 1, 3),
			loose = new BlockPos(10, 1, 3);
		helper.setBlock(grille, ModWastes.SLOTTED_GRILLE.get().defaultBlockState());
		helper.setBlock(band, ModWastes.VOID_ALLOY_BAND.get().defaultBlockState());
		helper.setBlock(cracked, ModWastes.SCARRED_VOID_ALLOY_PLATE.get().defaultBlockState().setValue(ScarredPlateBlock.SCAR, ScarredPlateBlock.Scar.CRACKED));
		helper.setBlock(chipped, ModWastes.SCARRED_VOID_ALLOY_PLATE.get().defaultBlockState().setValue(ScarredPlateBlock.SCAR, ScarredPlateBlock.Scar.CHIPPED));
		helper.setBlock(loose, ModWastes.SLOTTED_GRILLE.get().defaultBlockState());
		List<BlockPos> lights = List.of(grille, band, cracked);
		for (BlockPos p : List.of(grille, band, cracked, chipped))
			bind(helper, p, District.QUARTERS);
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(3, () -> {
			record(helper).wake(level, origin(helper), District.QUARTERS, null);
			record(helper).setFed(level, origin(helper), District.QUARTERS, true, null);
		});
		helper.runAtTickTime(40, () -> {
			for (BlockPos p : lights)
				if (!lit(helper, p))
					helper.fail(helper.getBlockState(p).getBlock().getName().getString() + " at " + p + " didn't light with its district");
			if (lit(helper, chipped))
				helper.fail("a chipped Scarred Plate lit");
			if (lit(helper, loose))
				helper.fail("a grille of no city lit");
			if (helper.getBlockState(grille).getLightEmission() != 0)
				helper.fail("lit decor casts light (D63)");
			record(helper).setFed(level, origin(helper), District.QUARTERS, false, null);
		});
		helper.runAtTickTime(80, () -> {
			for (BlockPos p : lights)
				if (lit(helper, p))
					helper.fail(helper.getBlockState(p).getBlock().getName().getString() + " at " + p + " still lit unfed");
			helper.succeed();
		});
	}

	/**
	 * D75: a bonded void-alloy block takes the bond of where it stands, however it's placed: set any way (a command, the
	 * test's setBlock) it's put right; the city's builder puts it right before it sets it; and a 2 x 1 plate's halves
	 * stand side by side. The Plate, a double Slab, a Grille and a Scarred Plate alike.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void voidAlloyBondFollowsPosition(GameTestHelper helper) {
		List<BlockState> kinds = List.of(ModWastes.VOID_ALLOY_PLATE.get().defaultBlockState(),
			ModWastes.VOID_ALLOY_SLAB.get().defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, net.minecraft.world.level.block.state.properties.SlabType.DOUBLE),
			ModWastes.SLOTTED_GRILLE.get().defaultBlockState(), ModWastes.SCARRED_VOID_ALLOY_PLATE.get().defaultBlockState());
		for (int k = 0; k < kinds.size(); k++)
			for (int x = 1; x <= 6; x++)
				for (int y = 1; y <= 3; y++) {
					BlockPos p = new BlockPos(x, y, 2 + k * 2);
					// set with the wrong bond on purpose: every one is the same, wherever it is
					helper.setBlock(p, kinds.get(k).setValue(VoidAlloyBond.NS, VoidAlloyBond.Pair.RR).setValue(VoidAlloyBond.EW, VoidAlloyBond.Pair.RR));
				}
		ServerLevel level = helper.getLevel();
		helper.runAtTickTime(2, () -> {
			for (int k = 0; k < kinds.size(); k++)
				for (int x = 1; x <= 6; x++)
					for (int y = 1; y <= 3; y++) {
						BlockPos p = new BlockPos(x, y, 2 + k * 2), abs = helper.absolutePos(p);
						BlockState s = helper.getBlockState(p);
						if (s != VoidAlloyBond.at(s, abs))
							helper.fail(s.getBlock().getName().getString() + " at " + p + " kept a bond that isn't its position's: " + s);
						// its south face's left half has its right half east of it (the 2 x 1 plates are whole)
						if (s.getValue(VoidAlloyBond.NS).second() == 'l' && x < 6 && helper.getBlockState(p.east()).getValue(VoidAlloyBond.NS).second() != 'r')
							helper.fail("a 2 x 1 plate broken at " + p);
					}
			// the city's builder: a plan's plate goes in on its bond
			BlockPos at = helper.absolutePos(new BlockPos(9, 1, 2));
			new com.comedymonkey.createaddons.content.wastes.city.CityApplier.Live(level, origin(helper)).apply(List.of(
				new com.comedymonkey.createaddons.content.wastes.city.CityPlan.Op.Put(at, kinds.get(0), null,
					com.comedymonkey.createaddons.content.wastes.city.CityPlan.ANYWHERE)));
			if (level.getBlockState(at) != VoidAlloyBond.at(kinds.get(0), at))
				helper.fail("the city's builder didn't set the plate's bond");
			helper.succeed();
		});
	}

	/** A Lava Generator, three wires east along the floor, and a woken Substation bound to the Foundry beyond them. */
	private static SubstationBlockEntity linedSubstation(GameTestHelper helper, BlockPos gen, boolean fuelled) {
		if (fuelled)
			LineGameTests.generator(helper, gen);
		else
			helper.setBlock(gen, ModBlocks.LAVA_GENERATOR.get().defaultBlockState());
		for (int i = 1; i <= 3; i++)
			LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		SubstationBlockEntity be = substation(helper, gen.east(4), District.FOUNDRY);
		helper.runAtTickTime(2, () -> {
			LineGameTests.relink(helper, gen.east(1), gen.east(3));
			be.energy().receiveEnergy(CMConfig.SERVER.wastesWakeCharge(), false);
		});
		return be;
	}

	private static void away(GameTestHelper helper, int ticks, BlockPos... where) {
		for (BlockPos p : where)
			PowerGridTesting.away(helper.getLevel(), helper.absolutePos(p), ticks > 0, ticks);
	}

	private static boolean fed(GameTestHelper helper) {
		return record(helper).fed(origin(helper), District.FOUNDRY, helper.getLevel().getGameTime());
	}

	/**
	 * D48 (P24): a Substation that's away draws its upkeep through the lines on paper. With its line still running, the
	 * network sends its record what it needs and the district stays fed; its generator gone, it goes unfed.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void awaySubstationDrawsUpkeepFromARunningLine(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), sub = gen.east(4);
		linedSubstation(helper, gen, true);
		helper.runAtTickTime(60, () -> {
			if (!fed(helper))
				helper.fail("not fed by its line while running");
			if (PowerGrid.of(helper.getLevel()).consumer(helper.absolutePos(sub)) == null)
				helper.fail("no record to run on paper with");
			away(helper, 240, sub);
			// from unfed: only the lines can make it fed again while it's away
			record(helper).setFed(helper.getLevel(), origin(helper), District.FOUNDRY, false, null);
		});
		helper.runAtTickTime(120, () -> {
			if (!fed(helper))
				helper.fail("away, its upkeep didn't come through the running line");
			helper.setBlock(gen, Blocks.AIR);
		});
		helper.runAtTickTime(220, () -> {
			if (fed(helper))
				helper.fail("still fed away with its generator gone");
			away(helper, 0, sub);
			helper.succeed();
		});
	}

	/**
	 * D48 (P24): with the Substation, its line and its generator all away (nothing on the network running), its upkeep is
	 * drawn straight from the generator's record.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void awaySubstationDrawsUpkeepFromAGeneratorOnPaper(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), sub = gen.east(4);
		linedSubstation(helper, gen, true);
		helper.runAtTickTime(60, () -> {
			if (PowerGridTesting.store(helper.getLevel(), helper.absolutePos(gen)) < 0)
				helper.fail("the generator has no record");
			away(helper, 180, gen, gen.east(1), gen.east(2), gen.east(3), sub);
			record(helper).setFed(helper.getLevel(), origin(helper), District.FOUNDRY, false, null);
		});
		helper.runAtTickTime(140, () -> {
			if (!fed(helper))
				helper.fail("all away, its upkeep didn't come from the generator's record");
			away(helper, 0, gen, gen.east(1), gen.east(2), gen.east(3), sub);
			helper.succeed();
		});
	}

	/** D48: all away with a generator that has no fuel: the lines bring nothing, and the district goes unfed. */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void awaySubstationGoesUnfedWithNothingOnTheLine(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), sub = gen.east(4);
		linedSubstation(helper, gen, false);
		helper.runAtTickTime(60, () -> {
			away(helper, 180, gen, gen.east(1), gen.east(2), gen.east(3), sub);
			record(helper).setFed(helper.getLevel(), origin(helper), District.FOUNDRY, true, null);
		});
		helper.runAtTickTime(140, () -> {
			if (fed(helper))
				helper.fail("away with nothing on its line, it's still fed");
			away(helper, 0, gen, gen.east(1), gen.east(2), gen.east(3), sub);
			helper.succeed();
		});
	}
}
