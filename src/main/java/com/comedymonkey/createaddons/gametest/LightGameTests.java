package com.comedymonkey.createaddons.gametest;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.line.Fitting;
import com.comedymonkey.createaddons.content.line.LineBlockEntity;
import com.comedymonkey.createaddons.content.light.LightBlock;
import com.comedymonkey.createaddons.content.light.LightBlockEntity;
import com.comedymonkey.createaddons.content.light.LightKind;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Electric Lights (design/specs/electric-lights.md): fed by a Power Line, dark without power, switched off at once by a
 * Breaker, flickering out when they starve, dyed without using the dye, washed, stepped by the wrench, and always
 * dropped plain.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LightGameTests {
	private static final String COURSE = "trade_bot_course";

	private LightGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(LightGameTests.class);
	}

	private static void light(GameTestHelper helper, LightKind kind, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.LIGHTS.get(kind).get().defaultBlockState());
	}

	private static LightBlockEntity be(GameTestHelper helper, BlockPos pos) {
		if (helper.getBlockEntity(pos) instanceof LightBlockEntity be)
			return be;
		throw new net.minecraft.gametest.framework.GameTestAssertException("no light at " + pos);
	}

	private static int level(GameTestHelper helper, BlockPos pos) {
		BlockState s = helper.getBlockState(pos);
		return s.hasProperty(LightBlock.LIGHT) ? s.getLightEmission(helper.getLevel(), helper.absolutePos(pos)) : -1;
	}

	/** A generator, three blocks of floor wire, and a Bollard at the end: returns the Bollard's place. */
	private static BlockPos fedBollard(GameTestHelper helper, BlockPos gen, boolean withGenerator) {
		if (withGenerator)
			LineGameTests.generator(helper, gen);
		for (int i = 1; i <= 3; i++)
			LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		BlockPos lamp = gen.east(4);
		light(helper, LightKind.BOLLARD, lamp);
		helper.runAtTickTime(2, () -> LineGameTests.relink(helper, gen.east(1), gen.east(3)));
		return lamp;
	}

	private static ItemStack use(GameTestHelper helper, BlockPos pos, ItemStack stack) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos abs = helper.absolutePos(pos);
		helper.getBlockState(pos).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
		return player.getItemInHand(InteractionHand.MAIN_HAND);
	}

	/** A Bollard at the end of a wire from a generator lights up, at its full light level. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void lightComesOnFromAWire(GameTestHelper helper) {
		BlockPos lamp = fedBollard(helper, new BlockPos(1, 1, 2), true);
		helper.runAtTickTime(100, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the Bollard didn't light from the wire");
			if (level(helper, lamp) != LightKind.BOLLARD.level())
				helper.fail("lit at light level " + level(helper, lamp) + ", not " + LightKind.BOLLARD.level());
			helper.succeed();
		});
	}

	/** The same Bollard and wire with no generator stays dark. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void lightStaysDarkWithoutPower(GameTestHelper helper) {
		BlockPos lamp = fedBollard(helper, new BlockPos(1, 1, 2), false);
		helper.runAtTickTime(80, () -> {
			if (level(helper, lamp) != 0 || be(helper, lamp).phase() != LightBlockEntity.Phase.DARK)
				helper.fail("the Bollard lit with no generator");
			helper.succeed();
		});
	}

	/** A Breaker switched off puts it out at once: dark well before its 2 s store could run out, with no flicker. */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void breakerSwitchesItOffAtOnce(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 5);
		BlockPos lamp = fedBollard(helper, gen, true);
		helper.runAtTickTime(4, () -> {
			if (helper.getBlockEntity(gen.east(2)) instanceof LineBlockEntity b)
				b.setFitting(Fitting.BREAKER, Direction.EAST);
		});
		helper.runAtTickTime(100, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the Bollard didn't light through the Breaker");
			if (helper.getBlockEntity(gen.east(2)) instanceof LineBlockEntity b)
				b.flipLever();
		});
		boolean[] flickered = {false};
		for (int t = 101; t <= 130; t++)
			helper.runAtTickTime(t, () -> flickered[0] |= be(helper, lamp).phase() == LightBlockEntity.Phase.FADING);
		helper.runAtTickTime(130, () -> {
			if (level(helper, lamp) != 0)
				helper.fail("still lit 1.5 s after its Breaker was switched off (it should go out at once)");
			if (flickered[0])
				helper.fail("it flickered when it was switched off (only a starved light flickers)");
			helper.succeed();
		});
	}

	/** A Light Switch on the feed line: off puts the light out at once (no flicker); on again, it comes back. */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void lightSwitchTurnsItOffAndOn(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 5);
		BlockPos lamp = fedBollard(helper, gen, true);
		helper.runAtTickTime(4, () -> {
			if (helper.getBlockEntity(gen.east(2)) instanceof LineBlockEntity b)
				b.setFitting(Fitting.SWITCH, Direction.EAST);
		});
		helper.runAtTickTime(100, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the Bollard didn't light through the Light Switch");
			if (helper.getBlockEntity(gen.east(2)) instanceof LineBlockEntity b)
				b.flipLever();
		});
		boolean[] flickered = {false};
		for (int t = 101; t <= 130; t++)
			helper.runAtTickTime(t, () -> flickered[0] |= be(helper, lamp).phase() == LightBlockEntity.Phase.FADING);
		helper.runAtTickTime(130, () -> {
			if (level(helper, lamp) != 0)
				helper.fail("still lit 1.5 s after its Light Switch was switched off");
			if (flickered[0])
				helper.fail("it flickered when it was switched off");
			if (helper.getBlockEntity(gen.east(2)) instanceof LineBlockEntity b)
				b.flipLever();
		});
		helper.runAtTickTime(230, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("it didn't come back on when the Light Switch was switched on again");
			helper.succeed();
		});
	}

	/** Redstone doesn't work a Light Switch (that's the Breaker's job): a signal beside it leaves the light lit. */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void redstoneDoesntWorkALightSwitch(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 5);
		BlockPos lamp = fedBollard(helper, gen, true);
		helper.runAtTickTime(4, () -> {
			if (helper.getBlockEntity(gen.east(2)) instanceof LineBlockEntity b)
				b.setFitting(Fitting.SWITCH, Direction.EAST);
			helper.setBlock(gen.east(2).south(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
		});
		helper.runAtTickTime(150, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("a redstone signal switched the Light Switch off");
			helper.succeed();
		});
	}

	/** A Light Switch never joins brass to copper, wrench or not (only a Breaker does). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void lightSwitchNeverJoinsTiers(GameTestHelper helper) {
		BlockPos a = new BlockPos(2, 1, 3);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), a, Direction.DOWN);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), a.east(), Direction.DOWN);
		LineGameTests.wire(helper, ModBlocks.BRASS_POWER_LINE.get(), a.east(2), Direction.DOWN);
		helper.runAtTickTime(2, () -> {
			LineGameTests.relink(helper, a, a.east(), a.east(2));
			if (!(helper.getBlockEntity(a.east()) instanceof LineBlockEntity sw) || !(helper.getBlockEntity(a.east(2)) instanceof LineBlockEntity brass))
				throw new net.minecraft.gametest.framework.GameTestAssertException("no lines");
			sw.setFitting(Fitting.SWITCH, Direction.EAST);
			if (LineBlockEntity.joins(sw, Direction.EAST, brass) || LineBlockEntity.joins(brass, Direction.WEST, sw))
				helper.fail("a Light Switch joined copper to brass");
			helper.succeed();
		});
	}

	/** Starved (power stops coming, its circuit still has a source), it stays lit for its store, flickers, then goes dark. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void starvedLightFlickersOut(GameTestHelper helper) {
		BlockPos lamp = new BlockPos(3, 1, 3);
		light(helper, LightKind.BOLLARD, lamp);
		helper.runAtTickTime(2, () -> be(helper, lamp).energy().receiveEnergy(1_000_000, false));
		helper.runAtTickTime(30, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("didn't light on the power it was given");
		});
		boolean[] flickered = {false};
		for (int t = 31; t <= 110; t++)
			helper.runAtTickTime(t, () -> flickered[0] |= be(helper, lamp).phase() == LightBlockEntity.Phase.FADING);
		helper.runAtTickTime(110, () -> {
			if (!flickered[0])
				helper.fail("it went dark without flickering");
			if (level(helper, lamp) != 0 || be(helper, lamp).phase() != LightBlockEntity.Phase.DARK)
				helper.fail("still lit with nothing coming in");
			helper.succeed();
		});
	}

	/**
	 * A light between a live line and a dead one (a brass wire on its other side, joined to nothing that gives power)
	 * stays lit: the dead line doesn't switch it off.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void deadLineBesideItDoesntSwitchItOff(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 11);
		BlockPos lamp = fedBollard(helper, gen, true);
		for (int i = 1; i <= 2; i++)
			LineGameTests.wire(helper, ModBlocks.BRASS_POWER_LINE.get(), lamp.east(i), Direction.DOWN);
		helper.runAtTickTime(3, () -> LineGameTests.relink(helper, lamp.east(1), lamp.east(2)));
		boolean[] wentDark = {false};
		for (int t = 100; t <= 240; t++)
			helper.runAtTickTime(t, () -> wentDark[0] |= level(helper, lamp) == 0);
		helper.runAtTickTime(240, () -> {
			if (wentDark[0])
				helper.fail("the dead brass line beside it switched it off");
			helper.succeed();
		});
	}

	/** After starving, a trickle doesn't bring it back: it waits for a full store (no brownout cycling). */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void starvedLightWaitsForAFullStore(GameTestHelper helper) {
		BlockPos lamp = new BlockPos(3, 1, 3);
		light(helper, LightKind.BOLLARD, lamp);
		helper.runAtTickTime(2, () -> be(helper, lamp).energy().receiveEnergy(1_000_000, false));
		// lit by ~22, empty by ~62, dark by ~82
		helper.runAtTickTime(100, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.DARK)
				helper.fail("didn't starve");
			be(helper, lamp).energy().receiveEnergy(10, false);
		});
		helper.runAtTickTime(130, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.DARK)
				helper.fail("a trickle of 10 FE brought a starved light back");
			be(helper, lamp).energy().receiveEnergy(1_000_000, false);
		});
		helper.runAtTickTime(155, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("a full store didn't bring it back");
			helper.succeed();
		});
	}

	/** A dye colours it without being used up; a water bucket washes it plain and stays full. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void dyeIsKeptAndWaterWashes(GameTestHelper helper) {
		BlockPos lamp = new BlockPos(3, 1, 3);
		light(helper, LightKind.GLOBE_HEAD, lamp);
		helper.runAtTickTime(2, () -> {
			ItemStack dye = use(helper, lamp, new ItemStack(Items.RED_DYE));
			if (be(helper, lamp).dye() != DyeColor.RED)
				helper.fail("the dye didn't colour it");
			if (dye.getCount() != 1 || !dye.is(Items.RED_DYE))
				helper.fail("the dye was used up");
			ItemStack bucket = use(helper, lamp, new ItemStack(Items.WATER_BUCKET));
			if (be(helper, lamp).dye() != null)
				helper.fail("the water bucket didn't wash it plain");
			if (!bucket.is(Items.WATER_BUCKET))
				helper.fail("the water bucket was emptied");
			helper.succeed();
		});
	}

	/** A dyed light breaks to a plain item. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void breakingDropsItPlain(GameTestHelper helper) {
		BlockPos lamp = new BlockPos(3, 1, 3);
		light(helper, LightKind.GLOBE_HEAD, lamp);
		helper.runAtTickTime(2, () -> {
			be(helper, lamp).setDye(DyeColor.BLUE);
			helper.getLevel().destroyBlock(helper.absolutePos(lamp), true);
		});
		helper.runAtTickTime(5, () -> {
			helper.assertItemEntityPresent(ModItems.LIGHTS.get(LightKind.GLOBE_HEAD).get(), lamp, 2);
			for (var e : helper.getEntities(net.minecraft.world.entity.EntityType.ITEM, lamp, 2))
				if (!e.getItem().getComponentsPatch().isEmpty())
					helper.fail("the dropped light carries data: " + e.getItem().getComponentsPatch());
			helper.succeed();
		});
	}

	/** The wrench's steps: Bright then Dim lower its light level and its draw. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void brightnessStepsDimIt(GameTestHelper helper) {
		BlockPos lamp = fedBollard(helper, new BlockPos(1, 1, 8), true);
		helper.runAtTickTime(100, () -> {
			LightBlockEntity be = be(helper, lamp);
			double full = be.draw();
			be.stepBrightness();
			be.stepBrightness();
			if (level(helper, lamp) != LightKind.BOLLARD.level(2))
				helper.fail("Dim gave light level " + level(helper, lamp) + ", not " + LightKind.BOLLARD.level(2));
			if (Math.abs(be.draw() - full * LightKind.STEPS[2]) > 1e-9)
				helper.fail("Dim draws " + be.draw() + " FE/t, not half of " + full);
			helper.succeed();
		});
	}

	/** A wall light only stays up with a wall behind it: placed on a block's side, it falls when the block goes. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void wallLightNeedsItsWall(GameTestHelper helper) {
		BlockPos wall = new BlockPos(3, 1, 3), lamp = wall.south();
		helper.setBlock(wall, net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.BULKHEAD).get().defaultBlockState().setValue(LightBlock.FACING, Direction.SOUTH));
		helper.runAtTickTime(2, () -> helper.setBlock(wall, net.minecraft.world.level.block.Blocks.AIR));
		helper.runAtTickTime(5, () -> {
			if (helper.getBlockState(lamp).getBlock() instanceof LightBlock)
				helper.fail("the Bulkhead stayed up with no wall behind it");
			helper.succeed();
		});
	}
}
