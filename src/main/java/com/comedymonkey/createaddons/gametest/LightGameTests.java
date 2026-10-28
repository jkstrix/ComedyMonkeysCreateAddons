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

	// ---------- fed through the block, and flat to the foot ----------

	/** A Bulkhead on the south face of a stone block lights from a wire lying on the block's north face (hidden behind it). */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void lightFedThroughItsWall(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 1), wire = gen.east(), wall = wire.south(), lamp = wall.south();
		LineGameTests.generator(helper, gen);
		helper.setBlock(wall, net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.BULKHEAD).get().defaultBlockState().setValue(LightBlock.FACING, Direction.SOUTH));
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), wire, Direction.SOUTH);
		helper.runAtTickTime(2, () -> LineGameTests.relink(helper, wire));
		helper.runAtTickTime(120, () -> {
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the Bulkhead didn't light from the wire behind its wall");
			helper.succeed();
		});
	}

	/** A Light Strip on top of a block lights from a wire on the block's underside (above the ceiling below). */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void stripFedThroughItsFloor(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), wire = gen.east(), floor = wire.above(), top = floor.above();
		LineGameTests.generator(helper, gen);
		helper.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(top, ModBlocks.LIGHT_STRIP.get().defaultBlockState()
			.setValue(com.comedymonkey.createaddons.content.light.LightStripBlock.FACES.get(Direction.DOWN), true));
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), wire, Direction.UP);
		helper.runAtTickTime(2, () -> LineGameTests.relink(helper, wire));
		helper.runAtTickTime(120, () -> {
			if (strip(helper, top).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the strip didn't light from the wire under its floor");
			helper.succeed();
		});
	}

	/** A floor wire meets a Bollard on the same floor flat (under its foot); it still climbs a machine's face to its socket. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void wireMeetsALightFlat(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2);
		BlockPos lamp = fedBollard(helper, gen, true);
		helper.runAtTickTime(4, () -> {
			if (!(helper.getBlockEntity(lamp.west()) instanceof com.comedymonkey.createaddons.content.line.PowerLineBlockEntity w)
				|| w.link(Direction.DOWN, Direction.EAST) != com.comedymonkey.createaddons.content.line.WireLink.LAMP)
				helper.fail("the wire doesn't meet the Bollard flat");
			if (!(helper.getBlockEntity(gen.east()) instanceof com.comedymonkey.createaddons.content.line.PowerLineBlockEntity g)
				|| g.link(Direction.DOWN, Direction.WEST) != com.comedymonkey.createaddons.content.line.WireLink.TERMINAL)
				helper.fail("the wire no longer climbs to the generator's socket");
			helper.succeed();
		});
	}

	// ---------- Light Strips ----------

	/** A run of {@code length} floor strips along +x from {@code from}. */
	private static void strips(GameTestHelper helper, BlockPos from, int length) {
		for (int i = 0; i < length; i++)
			helper.setBlock(from.east(i), ModBlocks.LIGHT_STRIP.get().defaultBlockState()
				.setValue(com.comedymonkey.createaddons.content.light.LightStripBlock.FACES.get(Direction.DOWN), true));
	}

	/** A generator, two blocks of wire, then a run of five floor strips: only the first touches the wire. Returns the run's start. */
	private static BlockPos fedRun(GameTestHelper helper, BlockPos gen) {
		LineGameTests.generator(helper, gen);
		for (int i = 1; i <= 2; i++)
			LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		strips(helper, gen.east(3), 5);
		helper.runAtTickTime(2, () -> LineGameTests.relink(helper, gen.east(1), gen.east(2)));
		return gen.east(3);
	}

	private static com.comedymonkey.createaddons.content.light.LightStripBlockEntity strip(GameTestHelper helper, BlockPos pos) {
		if (helper.getBlockEntity(pos) instanceof com.comedymonkey.createaddons.content.light.LightStripBlockEntity be)
			return be;
		throw new net.minecraft.gametest.framework.GameTestAssertException("no strip at " + pos);
	}

	/** A Power Line touching the first strip of a run lights the whole run, to its far end, at a strip's light level. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void stripRunLightsFromOneFeed(GameTestHelper helper) {
		BlockPos start = fedRun(helper, new BlockPos(0, 1, 2));
		helper.runAtTickTime(120, () -> {
			for (int i = 0; i < 5; i++)
				if (strip(helper, start.east(i)).phase() != LightBlockEntity.Phase.LIT)
					helper.fail("strip " + i + " of the run isn't lit");
			if (helper.getBlockState(start.east(4)).getLightEmission(helper.getLevel(), helper.absolutePos(start.east(4)))
				!= com.comedymonkey.createaddons.content.light.LightStripBlock.LEVEL)
				helper.fail("the run's far end doesn't cast a strip's light");
			helper.succeed();
		});
	}

	/** A run never passes power on (P7): a Bollard beside its far end, touching no line, stays dark. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void stripRunPowersNothingElse(GameTestHelper helper) {
		BlockPos start = fedRun(helper, new BlockPos(0, 1, 2));
		BlockPos lamp = start.east(5);
		light(helper, LightKind.BOLLARD, lamp);
		helper.runAtTickTime(120, () -> {
			if (strip(helper, start.east(4)).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the run didn't light");
			if (be(helper, lamp).phase() != LightBlockEntity.Phase.DARK)
				helper.fail("the run powered a Bollard beside it");
			helper.succeed();
		});
	}

	/** A dye colours the whole run (D24) and isn't used; sneaking, only the strip clicked; water washes the whole run. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void dyeColoursTheWholeRun(GameTestHelper helper) {
		BlockPos start = new BlockPos(1, 1, 2);
		strips(helper, start, 4);
		helper.runAtTickTime(2, () -> {
			ItemStack dye = use(helper, start.east(1), new ItemStack(Items.BLUE_DYE));
			if (dye.getCount() != 1)
				helper.fail("the dye was used up");
			for (int i = 0; i < 4; i++)
				if (strip(helper, start.east(i)).dye() != DyeColor.BLUE)
					helper.fail("strip " + i + " wasn't coloured with the run");
			Player sneaker = helper.makeMockPlayer(GameType.SURVIVAL);
			sneaker.setShiftKeyDown(true);
			sneaker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE));
			BlockPos abs = helper.absolutePos(start.east(3));
			helper.getBlockState(start.east(3)).useItemOn(sneaker.getMainHandItem(), helper.getLevel(), sneaker, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
			if (strip(helper, start.east(3)).dye() != DyeColor.RED || strip(helper, start.east(2)).dye() != DyeColor.BLUE)
				helper.fail("sneaking didn't colour just the one strip");
			use(helper, start, new ItemStack(Items.WATER_BUCKET));
			for (int i = 0; i < 4; i++)
				if (strip(helper, start.east(i)).dye() != null)
					helper.fail("strip " + i + " wasn't washed with the run");
			helper.succeed();
		});
	}

	/**
	 * A generator, six blocks of wire with a Light Switch part-way, and a strip run beside the whole wire: fed on both
	 * sides of the switch. Returns the run's start (beside the first wire).
	 */
	private static BlockPos runBesideASwitch(GameTestHelper helper, BlockPos gen) {
		LineGameTests.generator(helper, gen);
		for (int i = 1; i <= 6; i++)
			LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		strips(helper, gen.east().south(), 6);
		helper.runAtTickTime(2, () -> {
			LineGameTests.relink(helper, gen.east(1), gen.east(2), gen.east(3), gen.east(4), gen.east(5), gen.east(6));
			if (helper.getBlockEntity(gen.east(3)) instanceof LineBlockEntity b)
				b.setFitting(Fitting.SWITCH, Direction.EAST);
		});
		return gen.east().south();
	}

	/**
	 * A run fed on both sides of a Light Switch is one lamp: switched off, the far side's word (no power) doesn't put it out
	 * while the near side still reaches the generator. Not one piece goes dark, or blinks, for two seconds after.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void stripRunFedAcrossASwitchStaysLit(GameTestHelper helper) {
		BlockPos start = runBesideASwitch(helper, new BlockPos(0, 1, 1));
		helper.runAtTickTime(100, () -> {
			for (int i = 0; i < 6; i++)
				if (strip(helper, start.east(i)).phase() != LightBlockEntity.Phase.LIT)
					helper.fail("strip " + i + " didn't light");
			if (helper.getBlockEntity(new BlockPos(3, 1, 1)) instanceof LineBlockEntity b)
				b.flipLever();
		});
		for (int t = 101; t <= 150; t++)
			helper.runAtTickTime(t, () -> {
				for (int i = 0; i < 6; i++)
					if (strip(helper, start.east(i)).phase() == LightBlockEntity.Phase.DARK)
						helper.fail("strip " + i + " went dark when a switch cut one of the run's feeds");
			});
		helper.runAtTickTime(150, helper::succeed);
	}

	/**
	 * A run touching the line in many places is counted once: each piece the line feeds it through says it has only its
	 * share of the run's room, so the network doesn't draw for the run once per piece.
	 */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void stripRunTouchingTheLineIsCountedOnce(GameTestHelper helper) {
		BlockPos start = runBesideASwitch(helper, new BlockPos(0, 1, 1));
		helper.runAtTickTime(120, () -> {
			long room = 0, said = 0;
			for (int i = 0; i < 6; i++) {
				room += strip(helper, start.east(i)).energy().getMaxEnergyStored();
				said += strip(helper, start.east(i)).runEnergy().getMaxEnergyStored();
			}
			// six feeds, each saying a sixth of the run (give or take rounding)
			if (said > room + 6)
				helper.fail("the run's feeds say " + said + " FE of room between them, more than the run's " + room);
			helper.succeed();
		});
	}

	/** The wrench steps the whole run's brightness together (P26). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void wrenchStepsTheWholeRun(GameTestHelper helper) {
		BlockPos start = new BlockPos(1, 1, 2);
		strips(helper, start, 3);
		helper.runAtTickTime(2, () -> {
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
			BlockPos abs = helper.absolutePos(start.east(1));
			((com.comedymonkey.createaddons.content.light.LightStripBlock) ModBlocks.LIGHT_STRIP.get()).onWrenched(helper.getBlockState(start.east(1)),
				new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
			for (int i = 0; i < 3; i++)
				if (strip(helper, start.east(i)).step() != 1)
					helper.fail("strip " + i + " didn't step with the run");
			helper.succeed();
		});
	}

	// ---------- Edge Lights ----------

	private static com.comedymonkey.createaddons.content.light.EdgeLightBlockEntity edge(GameTestHelper helper, BlockPos pos) {
		if (helper.getBlockEntity(pos) instanceof com.comedymonkey.createaddons.content.light.EdgeLightBlockEntity be)
			return be;
		throw new net.minecraft.gametest.framework.GameTestAssertException("no Edge Light at " + pos);
	}

	/** A run of Edge Light along the floor's north edge, five blocks along +x, fed at its west end: lit to its east end. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void edgeRunLightsFromOneFeed(GameTestHelper helper) {
		BlockPos gen = new BlockPos(0, 1, 2);
		LineGameTests.generator(helper, gen);
		for (int i = 1; i <= 2; i++)
			LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		for (int i = 3; i <= 7; i++) {
			// (on stone: trim drops off a floor that isn't sturdy, as the test course's isn't)
			helper.setBlock(gen.east(i).below(), net.minecraft.world.level.block.Blocks.STONE);
			helper.setBlock(gen.east(i), ModBlocks.EDGE_LIGHT.get());
			edge(helper, gen.east(i)).addEdge(Direction.DOWN, Direction.NORTH);
		}
		helper.runAtTickTime(2, () -> LineGameTests.relink(helper, gen.east(1), gen.east(2)));
		helper.runAtTickTime(120, () -> {
			for (int i = 3; i <= 7; i++)
				if (edge(helper, gen.east(i)).phase() != LightBlockEntity.Phase.LIT)
					helper.fail("edge " + i + " of the run isn't lit");
			helper.succeed();
		});
	}

	/**
	 * Clicking a floor near its north edge lays trim on that edge; clicking near its east edge adds that edge to the same
	 * block. Broken, it drops one Edge Light for each edge.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void edgeLightGoesOnTheEdgeClicked(GameTestHelper helper) {
		BlockPos floor = new BlockPos(2, 1, 2), cell = floor.above();
		helper.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE);
		helper.runAtTickTime(1, () -> {
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EDGE_LIGHT.get(), 4));
			BlockPos abs = helper.absolutePos(floor);
			Vec3 north = new Vec3(abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 0.1), east = new Vec3(abs.getX() + 0.9, abs.getY() + 1, abs.getZ() + 0.5);
			net.minecraft.world.InteractionResult first = ModItems.EDGE_LIGHT.get().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(north, Direction.UP, abs, false)));
			if (!(helper.getLevel().getBlockEntity(helper.absolutePos(cell)) instanceof com.comedymonkey.createaddons.content.light.EdgeLightBlockEntity))
				helper.fail("the first click placed nothing (" + first + ", floor " + helper.getBlockState(floor) + ", cell " + helper.getBlockState(cell) + ")");
			ModItems.EDGE_LIGHT.get().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(east, Direction.UP, abs, false)));
			int edges = edge(helper, cell).edges();
			if (!com.comedymonkey.createaddons.content.light.EdgeLinks.has(edges, Direction.DOWN, Direction.NORTH))
				helper.fail("no trim on the north edge clicked");
			if (!com.comedymonkey.createaddons.content.light.EdgeLinks.has(edges, Direction.DOWN, Direction.EAST))
				helper.fail("the east edge clicked wasn't added to the same block");
			if (Integer.bitCount(edges) != 2)
				helper.fail("the block has " + Integer.bitCount(edges) + " edges, not 2");
			if (player.getMainHandItem().getCount() != 2)
				helper.fail("placing two edges used " + (4 - player.getMainHandItem().getCount()) + " Edge Lights");
			int dropped = net.minecraft.world.level.block.Block.getDrops(helper.getBlockState(cell), helper.getLevel(), helper.absolutePos(cell),
				helper.getBlockEntity(cell)).stream().mapToInt(ItemStack::getCount).sum();
			if (dropped != 2)
				helper.fail("broken, it dropped " + dropped + ", not 2");
			helper.succeed();
		});
	}

	/** A block of Edge Light with two edges on its floor drops both when the floor goes (the last edges too). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void edgeLightDropsAllItsEdgesWhenItsFloorGoes(GameTestHelper helper) {
		BlockPos floor = new BlockPos(2, 1, 2), cell = floor.above();
		helper.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(cell, ModBlocks.EDGE_LIGHT.get());
		edge(helper, cell).addEdge(Direction.DOWN, Direction.NORTH);
		edge(helper, cell).addEdge(Direction.DOWN, Direction.EAST);
		helper.runAtTickTime(2, () -> helper.setBlock(floor, net.minecraft.world.level.block.Blocks.AIR));
		helper.runAtTickTime(6, () -> {
			if (helper.getBlockState(cell).is(ModBlocks.EDGE_LIGHT.get()))
				helper.fail("the Edge Light stayed without its floor");
			int dropped = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(helper.absolutePos(cell)).inflate(2), e -> e.getItem().is(ModItems.EDGE_LIGHT.get()))
				.stream().mapToInt(e -> e.getItem().getCount()).sum();
			if (dropped != 2)
				helper.fail("it dropped " + dropped + " Edge Lights for its 2 edges");
			helper.succeed();
		});
	}

	/**
	 * In the corner of a floor and a wall the trim is one edge however it's clicked (on the wall, along its foot): clicking
	 * the floor near the wall and the wall near the floor give the same edge, placed once.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void edgeLightInACornerIsOneEdge(GameTestHelper helper) {
		BlockPos floor = new BlockPos(2, 1, 2), cell = floor.above(), wall = cell.north();
		helper.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(wall, net.minecraft.world.level.block.Blocks.STONE);
		helper.runAtTickTime(1, () -> {
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EDGE_LIGHT.get(), 4));
			BlockPos f = helper.absolutePos(floor), w = helper.absolutePos(wall);
			// the floor's top, near the wall; then the wall's south face, near the floor
			ModItems.EDGE_LIGHT.get().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(new Vec3(f.getX() + 0.5, f.getY() + 1, f.getZ() + 0.05), Direction.UP, f, false)));
			ModItems.EDGE_LIGHT.get().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(new Vec3(w.getX() + 0.5, w.getY() + 0.05, w.getZ() + 1), Direction.SOUTH, w, false)));
			int edges = edge(helper, cell).edges();
			if (Integer.bitCount(edges) != 1)
				helper.fail("the corner has " + Integer.bitCount(edges) + " edges, not 1");
			if (!com.comedymonkey.createaddons.content.light.EdgeLinks.has(edges, Direction.NORTH, Direction.DOWN))
				helper.fail("the corner's trim isn't on the wall, along its foot");
			if (player.getMainHandItem().getCount() != 3)
				helper.fail("the second click used an Edge Light");
			helper.succeed();
		});
	}

	// ---------- Lamp Posts ----------

	/** A Lamp Post pole of {@code height} at {@code foot}, upright; returns its head. */
	private static BlockPos lampPost(GameTestHelper helper, BlockPos foot, int height) {
		// (on stone: the test course's floor isn't sturdy enough to stand on)
		helper.setBlock(foot.below(), net.minecraft.world.level.block.Blocks.STONE);
		for (int i = 0; i < height; i++)
			helper.setBlock(foot.above(i), ModBlocks.CAST_IRON_LAMP_POST.get());
		return foot.above(height - 1);
	}

	/**
	 * A three-high Cast Iron Lamp Post fed by a wire at its foot lights a Lantern Head on top and a Cobra Head on an arm on
	 * its north side.
	 */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void lampPostFeedsItsHeads(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 4), foot = gen.east(3);
		LineGameTests.generator(helper, gen);
		for (int i = 1; i <= 2; i++)
			LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		BlockPos head = lampPost(helper, foot, 3);
		helper.runAtTickTime(3, () -> {
			helper.setBlock(head.above(), ModBlocks.LIGHTS.get(LightKind.LANTERN_HEAD).get());
			helper.setBlock(head.north(), ModBlocks.LIGHTS.get(LightKind.COBRA_HEAD).get().defaultBlockState()
				.setValue(com.comedymonkey.createaddons.content.light.LampHeadBlock.ARM, com.comedymonkey.createaddons.content.light.ArmFinish.IRON)
				.setValue(LightBlock.FACING, Direction.NORTH));
			LineGameTests.relink(helper, gen.east(1), gen.east(2));
		});
		helper.runAtTickTime(6, () -> {
			if (!helper.getBlockState(head).getValue(com.comedymonkey.createaddons.content.line.LinePostBlock.BRANCHES.get(Direction.NORTH)))
				helper.fail("the post didn't grow an arm for the head on its north side");
		});
		helper.runAtTickTime(150, () -> {
			if (be(helper, head.above()).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the Lantern Head on top of the pole isn't lit");
			if (be(helper, head.north()).phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the Cobra Head on the arm isn't lit");
			helper.succeed();
		});
	}

	/** Taking a head off its arm takes the arm off the post with it. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void armGoesWithItsHead(GameTestHelper helper) {
		BlockPos head = lampPost(helper, new BlockPos(2, 1, 2), 2);
		helper.runAtTickTime(2, () -> helper.setBlock(head.east(), ModBlocks.LIGHTS.get(LightKind.GLOBE_HEAD).get().defaultBlockState()
			.setValue(com.comedymonkey.createaddons.content.light.LampHeadBlock.ARM, com.comedymonkey.createaddons.content.light.ArmFinish.IRON)
			.setValue(LightBlock.FACING, Direction.EAST)));
		helper.runAtTickTime(4, () -> helper.setBlock(head.east(), net.minecraft.world.level.block.Blocks.AIR));
		helper.runAtTickTime(6, () -> {
			if (helper.getBlockState(head).getValue(com.comedymonkey.createaddons.content.line.LinePostBlock.BRANCHES.get(Direction.EAST)))
				helper.fail("the arm stayed after its head was taken off");
			helper.succeed();
		});
	}

	/** A Lamp Post stacked on a Line Post doesn't join its pole: each is a pole of its own kind. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void lampPostDoesntJoinALinePost(GameTestHelper helper) {
		BlockPos line = new BlockPos(2, 1, 2);
		helper.setBlock(line.below(), net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(line, ModBlocks.LINE_POST.get());
		helper.setBlock(line.above(), ModBlocks.CAST_IRON_LAMP_POST.get());
		helper.runAtTickTime(4, () -> {
			var role = com.comedymonkey.createaddons.content.line.LinePostBlock.ROLE;
			if (helper.getBlockState(line).getValue(role) != com.comedymonkey.createaddons.content.line.PostRole.SINGLE)
				helper.fail("the Line Post took the Lamp Post above it as its pole (" + helper.getBlockState(line).getValue(role) + ")");
			if (helper.getBlockState(line.above()).getValue(role) != com.comedymonkey.createaddons.content.line.PostRole.SINGLE)
				helper.fail("the Lamp Post took the Line Post below it as its pole");
			helper.succeed();
		});
	}

	// ---------- hanging lamps ----------

	/** Lowers the hanging lamp at {@code lamp} a block, as a player clicking it with a Copper Power Line; returns the stack. */
	private static ItemStack lower(GameTestHelper helper, BlockPos lamp, ItemStack wire) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, wire);
		BlockPos at = helper.absolutePos(lamp);
		helper.getBlockState(lamp).useItemOn(wire, helper.getLevel(), player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false));
		return wire;
	}

	/**
	 * A Pendant Lamp under a ceiling, clicked twice with a Power Line, hangs two blocks lower on cable (the rose at the top),
	 * keeps its dye, and uses a wire a block; it can't go through the floor.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void pendantLowersOnCable(GameTestHelper helper) {
		BlockPos ceiling = new BlockPos(3, 4, 3), lamp = ceiling.below();
		helper.setBlock(ceiling, net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.PENDANT).get());
		// (the course floor isn't solid: a block for it to stop at)
		helper.setBlock(lamp.below(3), net.minecraft.world.level.block.Blocks.STONE);
		var wire = new ItemStack(ModItems.COPPER_POWER_LINE.get(), 4);
		helper.runAtTickTime(2, () -> {
			be(helper, lamp).setDye(DyeColor.BLUE);
			lower(helper, lamp, wire);
			lower(helper, lamp.below(), wire);
			lower(helper, lamp.below(2), wire);
		});
		helper.runAtTickTime(4, () -> {
			var hb = com.comedymonkey.createaddons.content.light.HangingCableBlock.class;
			var rose = com.comedymonkey.createaddons.content.light.HangingCableBlock.ROSE;
			BlockState top = helper.getBlockState(lamp), mid = helper.getBlockState(lamp.below()), foot = helper.getBlockState(lamp.below(2));
			if (!hb.isInstance(top.getBlock()) || top.getValue(rose) != com.comedymonkey.createaddons.content.light.HangingCableBlock.Rose.STEEL)
				helper.fail("the top of the cable isn't cable with a steel rose: " + top);
			if (!hb.isInstance(mid.getBlock()) || mid.getValue(rose) != com.comedymonkey.createaddons.content.light.HangingCableBlock.Rose.NONE)
				helper.fail("the cable's second block isn't plain cable: " + mid);
			if (!foot.is(ModBlocks.LIGHTS.get(LightKind.PENDANT).get()) || !foot.getValue(com.comedymonkey.createaddons.content.light.HangingLampBlock.HUNG))
				helper.fail("the lamp isn't hung two blocks down: " + foot);
			if (be(helper, lamp.below(2)).dye() != DyeColor.BLUE)
				helper.fail("the lamp lost its dye on the way down");
			if (wire.getCount() != 2)
				helper.fail("lowering two blocks left " + wire.getCount() + " of 4 wires, not 2");
			helper.succeed();
		});
	}

	/** A Pendant Lamp lowered a block lights from a wire on its ceiling block's side (fed down its cable). */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void hungLampFedThroughItsCeiling(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 4, 1), w = gen.east(), ceiling = w.south(), lamp = ceiling.below();
		LineGameTests.generator(helper, gen);
		helper.setBlock(ceiling, net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.PENDANT).get());
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), w, Direction.SOUTH);
		helper.runAtTickTime(2, () -> LineGameTests.relink(helper, w));
		helper.runAtTickTime(4, () -> lower(helper, lamp, new ItemStack(ModItems.COPPER_POWER_LINE.get())));
		helper.runAtTickTime(140, () -> {
			if (!(helper.getBlockEntity(lamp.below()) instanceof LightBlockEntity be) || be.phase() != LightBlockEntity.Phase.LIT)
				helper.fail("the hung lamp didn't light from the wire beside its ceiling");
			helper.succeed();
		});
	}

	/** Breaking the top of a Chandelier's cable brings it all down: the lamp drops, each block of cable gives its wire back. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void hangingLampComesDownWithItsCable(GameTestHelper helper) {
		BlockPos ceiling = new BlockPos(3, 4, 3), lamp = ceiling.below();
		helper.setBlock(ceiling, net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(lamp, ModBlocks.LIGHTS.get(LightKind.CHANDELIER).get());
		var wire = new ItemStack(ModItems.COPPER_POWER_LINE.get(), 2);
		helper.runAtTickTime(2, () -> {
			lower(helper, lamp, wire);
			lower(helper, lamp.below(), wire);
		});
		helper.runAtTickTime(4, () -> helper.getLevel().destroyBlock(helper.absolutePos(lamp), true));
		helper.runAtTickTime(7, () -> {
			for (int i = 0; i < 3; i++)
				if (!helper.getBlockState(lamp.below(i)).isAir())
					helper.fail("something was left hanging " + i + " below the ceiling: " + helper.getBlockState(lamp.below(i)));
			int wires = 0, lamps = 0;
			for (var e : helper.getEntities(net.minecraft.world.entity.EntityType.ITEM, lamp, 4)) {
				if (e.getItem().is(ModItems.COPPER_POWER_LINE.get()))
					wires += e.getItem().getCount();
				if (e.getItem().is(ModItems.LIGHTS.get(LightKind.CHANDELIER).get()))
					lamps += e.getItem().getCount();
			}
			if (wires != 2 || lamps != 1)
				helper.fail("it came down as " + wires + " wires and " + lamps + " chandeliers, not 2 and 1");
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
