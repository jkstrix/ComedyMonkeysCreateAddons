package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.line.FluidLineBlock;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.line.FluidLineBlockEntity;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity;
import com.comedymonkey.createaddons.content.line.Fitting;
import com.comedymonkey.createaddons.content.line.LineBlockEntity;
import com.comedymonkey.createaddons.content.line.LinePostBlock;
import com.comedymonkey.createaddons.content.line.LinePostBlockEntity;
import com.comedymonkey.createaddons.content.line.LineSide;
import com.comedymonkey.createaddons.content.line.LineTier;
import com.comedymonkey.createaddons.content.line.PostRole;
import com.comedymonkey.createaddons.content.line.PowerLineBlock;
import com.comedymonkey.createaddons.content.line.PowerGridTesting;
import com.comedymonkey.createaddons.content.line.PowerLineBlockEntity;
import com.comedymonkey.createaddons.content.line.Span;
import com.comedymonkey.createaddons.content.line.SplitMode;
import com.comedymonkey.createaddons.content.line.WireLink;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Fluid Lines and Power Lines in a real world: tanks, generators and motors joined by lines (./gradlew runGameTestServer). */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LineGameTests {
	private static final String COURSE = "trade_bot_course";

	private LineGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(LineGameTests.class);
	}

	// ---------- setting up ----------

	static void tank(GameTestHelper helper, BlockPos pos, FluidStack fill) {
		helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "fluid_tank")).defaultBlockState());
		if (!fill.isEmpty())
			fluids(helper, pos).fill(fill, IFluidHandler.FluidAction.EXECUTE);
	}

	static IFluidHandler fluids(GameTestHelper helper, BlockPos pos) {
		IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
		if (handler == null)
			helper.fail("no fluid handler at " + pos);
		return handler;
	}

	static int amount(GameTestHelper helper, BlockPos pos) {
		return fluids(helper, pos).getFluidInTank(0).getAmount();
	}

	/** A run of line from {@code from} along {@code dir}, {@code length} blocks, joined up as a player would have placed it. */
	static void run(GameTestHelper helper, Block line, BlockPos from, Direction dir, int length) {
		for (int i = 0; i < length; i++)
			helper.setBlock(from.relative(dir, i), line.defaultBlockState());
		for (int i = 0; i < length; i++)
			join(helper, from.relative(dir, i));
	}

	static void join(GameTestHelper helper, BlockPos pos) {
		FluidLineBlock.refresh(helper.getLevel(), helper.absolutePos(pos));
	}

	static FluidLineBlockEntity line(GameTestHelper helper, BlockPos pos) {
		if (!(helper.getBlockEntity(pos) instanceof FluidLineBlockEntity be))
			throw new IllegalStateException("no line at " + pos);
		return be;
	}

	/** Tank A (full of fluid) at x 1, a line of {@code length} along +x with an Intake on A, and an empty tank B after it. */
	static BlockPos[] tankToTank(GameTestHelper helper, Block line, FluidStack fill, int length) {
		BlockPos a = new BlockPos(1, 1, 2), b = a.east(length + 1);
		tank(helper, a, fill);
		tank(helper, b, FluidStack.EMPTY);
		run(helper, line, a.east(), Direction.EAST, length);
		line(helper, a.east()).setIntake(Direction.WEST);
		return new BlockPos[] {a, b};
	}

	// ---------- Fluid Lines ----------

	/** An Intake on a copper line draws from tank A into tank B at no more than 100 mB a tick, losing nothing. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void copperLineMovesFluidBetweenTanks(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 8000), 4);
		helper.runAtTickTime(2, () -> {
			if (FluidLineBlock.side(helper.getBlockState(t[0].east()), Direction.WEST) != LineSide.INTAKE)
				helper.fail("the Intake isn't on the tank's side");
			if (FluidLineBlock.side(helper.getBlockState(t[1].west()), Direction.EAST) != LineSide.TANK)
				helper.fail("the line doesn't join tank B");
		});
		// the line fills first (copper: a block every 8 ticks), then delivers
		helper.runAtTickTime(50, () -> {
			int a = amount(helper, t[0]), b = amount(helper, t[1]);
			if (b <= 0)
				helper.fail("nothing reached tank B");
			if (b > 100 * 50)
				helper.fail("moved " + b + " mB in 50 ticks, over copper's 100 mB a tick");
			if (a + b != 8000)
				helper.fail("lost or made fluid: " + a + " + " + b);
			helper.succeed();
		});
	}

	/** Brass moves ten times as much. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void brassLineIsTenTimesFaster(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.BRASS_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 8000), 4);
		// once the line has filled (brass: a block every 3 ticks), before tank B does
		int[] start = {0};
		helper.runAtTickTime(13, () -> start[0] = amount(helper, t[1]));
		helper.runAtTickTime(16, () -> {
			int moved = amount(helper, t[1]) - start[0];
			if (moved <= 100 * 3)
				helper.fail("brass moved only " + moved + " mB in 3 ticks");
			helper.succeed();
		});
	}

	/** Copper and brass side by side never join. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void copperNeverJoinsBrass(GameTestHelper helper) {
		BlockPos copper = new BlockPos(2, 1, 2), brass = copper.east();
		helper.setBlock(copper, ModBlocks.COPPER_FLUID_LINE.get().defaultBlockState());
		helper.setBlock(brass, ModBlocks.BRASS_FLUID_LINE.get().defaultBlockState());
		join(helper, copper);
		join(helper, brass);
		helper.runAtTickTime(2, () -> {
			if (FluidLineBlock.side(helper.getBlockState(copper), Direction.EAST) != LineSide.NONE
				|| FluidLineBlock.side(helper.getBlockState(brass), Direction.WEST) != LineSide.NONE)
				helper.fail("copper joined brass");
			helper.succeed();
		});
	}

	/** An Intake filtered to water leaves lava in its tank; with the filter cleared the lava flows. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void intakeFilterHoldsBackOtherFluids(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.LAVA, 4000), 3);
		line(helper, t[0].east()).setFilter(new FluidStack(Fluids.WATER, 1));
		helper.runAtTickTime(20, () -> {
			if (amount(helper, t[1]) != 0)
				helper.fail("lava got past a water filter");
			line(helper, t[0].east()).setFilter(FluidStack.EMPTY);
		});
		helper.runAtTickTime(60, () -> {
			if (amount(helper, t[1]) <= 0)
				helper.fail("no lava with the filter cleared");
			helper.succeed();
		});
	}

	/** A line that loops back to its own tank moves nothing: an Intake never delivers into the block it draws from. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void intakeNeverFeedsItsOwnTank(GameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 2);
		tank(helper, a, new FluidStack(Fluids.WATER, 4000));
		BlockPos p1 = a.east(), p2 = p1.south(), p3 = a.south();
		for (BlockPos p : new BlockPos[] {p1, p2, p3})
			helper.setBlock(p, ModBlocks.COPPER_FLUID_LINE.get().defaultBlockState());
		for (BlockPos p : new BlockPos[] {p1, p2, p3})
			join(helper, p);
		line(helper, p1).setIntake(Direction.WEST);
		helper.runAtTickTime(40, () -> {
			if (amount(helper, a) != 4000)
				helper.fail("the tank went from 4000 to " + amount(helper, a));
			if (line(helper, p3).moving())
				helper.fail("the line shows a flow with nowhere to go");
			if (line(helper, p3).flowing().isEmpty())
				helper.fail("the line stands empty with water to fill it");
			helper.succeed();
		});
	}

	/** A T shares evenly: two tanks on its branches fill alike. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void lineSharesEvenlyBetweenTanks(GameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 3), tee = a.east(2), north = tee.north(2), south = tee.south(2);
		tank(helper, a, new FluidStack(Fluids.WATER, 8000));
		tank(helper, north, FluidStack.EMPTY);
		tank(helper, south, FluidStack.EMPTY);
		BlockPos[] lines = {a.east(), tee, tee.north(), tee.south()};
		for (BlockPos p : lines)
			helper.setBlock(p, ModBlocks.COPPER_FLUID_LINE.get().defaultBlockState());
		for (BlockPos p : lines)
			join(helper, p);
		line(helper, a.east()).setIntake(Direction.WEST);
		helper.runAtTickTime(30, () -> {
			int n = amount(helper, north), s = amount(helper, south);
			if (n <= 0 || s <= 0 || Math.abs(n - s) > 100)
				helper.fail("uneven: " + n + " north, " + s + " south");
			helper.succeed();
		});
	}

	/** A wrench cut in the line stops the flow; restoring the joint starts it again. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void cutJointStopsTheFlow(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 8000), 4);
		BlockPos mid = t[0].east(2);
		helper.runAtTickTime(2, () -> line(helper, mid).wrenchJoint(Direction.EAST));
		int[] held = {0};
		helper.runAtTickTime(5, () -> held[0] = amount(helper, t[1]));
		helper.runAtTickTime(25, () -> {
			if (amount(helper, t[1]) != held[0])
				helper.fail("fluid crossed a cut joint");
			if (FluidLineBlock.side(helper.getBlockState(mid.east()), Direction.WEST) != LineSide.NONE)
				helper.fail("the other side of the cut still joins");
			line(helper, mid).wrenchJoint(Direction.EAST);
		});
		helper.runAtTickTime(45, () -> {
			if (amount(helper, t[1]) <= held[0])
				helper.fail("no flow after the joint was restored");
			helper.succeed();
		});
	}
	// ---------- fittings on Fluid Lines ----------

	/** A Valve shut by redstone stops the flow; with the signal gone it flows again. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void valveShutsOnRedstone(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 8000), 3);
		BlockPos valve = t[0].east(2);
		line(helper, valve).setFitting(Fitting.VALVE, Direction.EAST);
		helper.setBlock(valve.north(), Blocks.REDSTONE_BLOCK);
		helper.runAtTickTime(20, () -> {
			if (amount(helper, t[1]) != 0)
				helper.fail("fluid passed a Valve shut by redstone");
			helper.setBlock(valve.north(), Blocks.AIR);
		});
		helper.runAtTickTime(40, () -> {
			if (amount(helper, t[1]) <= 0)
				helper.fail("no flow once the signal went");
			helper.succeed();
		});
	}

	/** A Valve turned to half open passes no more than half the line's rate. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void valveThrottlesTheFlow(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 8000), 3);
		BlockPos valve = t[0].east(2);
		line(helper, valve).setFitting(Fitting.VALVE, Direction.EAST);
		line(helper, valve).turnValve(-5);
		int[] at = {0};
		helper.runAtTickTime(10, () -> at[0] = amount(helper, t[1]));
		helper.runAtTickTime(30, () -> {
			int moved = amount(helper, t[1]) - at[0];
			if (moved <= 0 || moved > 50 * 20)
				helper.fail("half open moved " + moved + " mB in 20 ticks (no more than 1000)");
			helper.succeed();
		});
	}

	/** A Filter part-way lets only its fluid past. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void filterLetsOnlyItsFluidPast(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.LAVA, 4000), 3);
		BlockPos filter = t[0].east(2);
		line(helper, filter).setFitting(Fitting.FILTER, Direction.EAST);
		line(helper, filter).setFilter(new FluidStack(Fluids.WATER, 1));
		helper.runAtTickTime(30, () -> {
			if (amount(helper, t[1]) != 0)
				helper.fail("lava got past a water Filter");
			if (!line(helper, filter).holding())
				helper.fail("the Filter holding lava back hasn't raised its flag");
			helper.succeed();
		});
	}

	/** An empty Filter lets any fluid past. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void emptyFilterLetsAnythingPast(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.LAVA, 4000), 3);
		line(helper, t[0].east(2)).setFitting(Fitting.FILTER, Direction.EAST);
		helper.runAtTickTime(30, () -> {
			if (amount(helper, t[1]) <= 0)
				helper.fail("an empty Filter held the lava back");
			if (line(helper, t[0].east(2)).holding())
				helper.fail("an empty Filter raised its flag");
			helper.succeed();
		});
	}

	/** Only the pieces the fluid runs through show it: the branch behind a shut Valve shows an empty slot. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void slotsPastAShutValveShowEmpty(GameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 3), tee = a.east(2), north = tee.north(2), south = tee.south(2);
		tank(helper, a, new FluidStack(Fluids.WATER, 8000));
		tank(helper, north, FluidStack.EMPTY);
		tank(helper, south, FluidStack.EMPTY);
		BlockPos[] lines = {a.east(), tee, tee.north(), tee.south()};
		for (BlockPos p : lines)
			helper.setBlock(p, ModBlocks.COPPER_FLUID_LINE.get().defaultBlockState());
		for (BlockPos p : lines)
			join(helper, p);
		line(helper, a.east()).setIntake(Direction.WEST);
		line(helper, tee.south()).setFitting(Fitting.VALVE, Direction.SOUTH);
		helper.setBlock(tee.south().east(), Blocks.REDSTONE_BLOCK);
		helper.runAtTickTime(30, () -> {
			if (amount(helper, south) != 0)
				helper.fail("fluid passed the shut Valve");
			if (line(helper, tee.north()).flowing().isEmpty() || line(helper, tee).flowing().isEmpty())
				helper.fail("the open branch doesn't show its flow");
			if (!line(helper, tee.south()).flowing().isEmpty())
				helper.fail("the shut branch still shows fluid");
			if (!line(helper, tee.south()).holding())
				helper.fail("the shut Valve's flag isn't up");
			helper.succeed();
		});
	}

	/** A line into a full tank stands full but still, and a piece broken and put back fills again. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void lineIntoAFullTankStaysFull(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 8000), 4);
		fluids(helper, t[1]).fill(new FluidStack(Fluids.WATER, 64000), IFluidHandler.FluidAction.EXECUTE);
		BlockPos mid = t[0].east(2);
		// once it has filled (copper: a block every 8 ticks) and settled
		helper.runAtTickTime(50, () -> {
			if (line(helper, mid).flowing().isEmpty() || line(helper, mid).moving())
				helper.fail("a line into a full tank should stand full and still");
			helper.setBlock(mid, Blocks.AIR);
		});
		helper.runAtTickTime(55, () -> {
			helper.setBlock(mid, ModBlocks.COPPER_FLUID_LINE.get().defaultBlockState());
			join(helper, mid);
		});
		helper.runAtTickTime(75, () -> {
			if (line(helper, mid).flowing().isEmpty())
				helper.fail("the piece put back stayed empty");
			helper.succeed();
		});
	}

	/** A line fills from the Intake outwards, a block at a time, and once the source runs dry it drains the same way. */
	@GameTest(template = COURSE, timeoutTicks = 180)
	public static void lineFillsAndDrainsGradually(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 500), 6);
		BlockPos near = t[0].east(2), far = t[1].west();
		helper.runAtTickTime(14, () -> {
			if (line(helper, near).flowing().isEmpty())
				helper.fail("the near end hasn't filled");
			if (!line(helper, far).flowing().isEmpty() || amount(helper, t[1]) > 0)
				helper.fail("the far end filled (or delivered) before the fluid could reach it");
		});
		// the 500 mB run out a few ticks after the line reaches tank B (copper fills a block every 8 ticks); the line
		// starts draining once the tank has stood empty for 40 ticks
		helper.runAtTickTime(118, () -> {
			if (amount(helper, t[1]) != 500)
				helper.fail("tank B has " + amount(helper, t[1]) + " mB, not all 500");
			if (!line(helper, near).flowing().isEmpty())
				helper.fail("the near end is still full with the source dry");
			if (line(helper, far).flowing().isEmpty())
				helper.fail("the far end emptied at once, not draining along");
		});
		helper.runAtTickTime(165, () -> {
			if (!line(helper, far).flowing().isEmpty())
				helper.fail("the far end never drained");
			helper.succeed();
		});
	}

	/** A slow source (topped up 20 mB a tick, drained dry each tick) keeps the line full and delivering, not resetting. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void aSlowSourceKeepsTheLineGoing(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 100), 4);
		helper.onEachTick(() -> fluids(helper, t[0]).fill(new FluidStack(Fluids.WATER, 20), IFluidHandler.FluidAction.EXECUTE));
		int[] at = {0};
		helper.runAtTickTime(60, () -> at[0] = amount(helper, t[1]));
		helper.runAtTickTime(100, () -> {
			int moved = amount(helper, t[1]) - at[0];
			// 40 ticks at 20 mB a tick: about 800, unless the line keeps emptying and refilling
			if (moved < 600)
				helper.fail("only " + moved + " mB in 40 ticks from a source topped up 20 mB a tick");
			if (line(helper, t[0].east(2)).flowing().isEmpty())
				helper.fail("the line emptied though its source keeps being topped up");
			helper.succeed();
		});
	}

	/** A Meter reads the flow through it, and so does a comparator. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void meterReadsTheFlow(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 8000), 3);
		BlockPos meter = t[0].east(2);
		line(helper, meter).setFitting(Fitting.METER, Direction.EAST);
		helper.runAtTickTime(40, () -> {
			if (line(helper, meter).meterFlow() != 100)
				helper.fail("the Meter reads " + line(helper, meter).meterFlow() + " mB a tick, not copper's 100");
			if (line(helper, meter).comparatorSignal() != 15)
				helper.fail("comparator signal " + line(helper, meter).comparatorSignal());
			helper.succeed();
		});
	}

	/** A Splitter set to nearest first fills the near tank and leaves the far one. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void splitterNearestFirst(GameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 3), split = a.east(2), tee = a.east(3), near = tee.north(1), far = tee.south(4);
		tank(helper, a, new FluidStack(Fluids.WATER, 8000));
		tank(helper, near, FluidStack.EMPTY);
		tank(helper, far, FluidStack.EMPTY);
		List<BlockPos> lines = new ArrayList<>(List.of(a.east(), split, tee));
		for (int i = 1; i <= 3; i++)
			lines.add(tee.south(i));
		for (BlockPos p : lines)
			helper.setBlock(p, ModBlocks.COPPER_FLUID_LINE.get().defaultBlockState());
		for (BlockPos p : lines)
			join(helper, p);
		line(helper, a.east()).setIntake(Direction.WEST);
		line(helper, split).setFitting(Fitting.SPLITTER, Direction.EAST);
		line(helper, split).nextSplitMode();
		helper.runAtTickTime(25, () -> {
			if (line(helper, split).splitMode() != SplitMode.NEAREST)
				helper.fail("mode " + line(helper, split).splitMode());
			if (amount(helper, near) <= 0 || amount(helper, far) != 0)
				helper.fail("nearest first: " + amount(helper, near) + " near, " + amount(helper, far) + " far");
			helper.succeed();
		});
	}

	/** Two colours never join, not even wrenched, so no fluid crosses. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void twoColoursNeverJoin(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 8000), 4);
		// the last piece, beside tank B, red; the rest blue
		BlockPos red = t[0].east(4);
		line(helper, red).setDye(DyeColor.RED);
		for (int i = 1; i <= 3; i++)
			line(helper, t[0].east(i)).setDye(DyeColor.BLUE);
		helper.runAtTickTime(5, () -> {
			if (FluidLineBlock.side(helper.getBlockState(red), Direction.WEST) != LineSide.NONE)
				helper.fail("a red line joined a blue one");
			if (FluidLineBlock.side(helper.getBlockState(t[0].east(2)), Direction.EAST) != LineSide.LINE)
				helper.fail("blue lines stopped joining each other");
			// the wrench doesn't join them either
			line(helper, red).wrenchJoint(Direction.WEST);
		});
		helper.runAtTickTime(40, () -> {
			if (FluidLineBlock.side(helper.getBlockState(red), Direction.WEST) != LineSide.NONE)
				helper.fail("the wrench joined red to blue");
			if (amount(helper, t[1]) != 0)
				helper.fail("fluid crossed between colours");
			helper.succeed();
		});
	}

	/** A red Power Line laid beside plain ones doesn't join them: plain is a colour of its own. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void plainAndDyedNeverJoin(GameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 2);
		for (int i = 0; i < 4; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), a.east(i), Direction.DOWN);
		helper.runAtTickTime(2, () -> {
			if (helper.getBlockEntity(a.east(3)) instanceof PowerLineBlockEntity be)
				be.setDye(DyeColor.RED);
			relink(helper, a, a.east(3));
		});
		helper.runAtTickTime(10, () -> {
			if (!(helper.getBlockEntity(a.east(2)) instanceof PowerLineBlockEntity plain) || !(helper.getBlockEntity(a.east(3)) instanceof PowerLineBlockEntity red))
				throw new IllegalStateException("wires missing");
			if (plain.link(Direction.DOWN, Direction.EAST).connected() || red.link(Direction.DOWN, Direction.WEST).connected())
				helper.fail("a red wire joined a plain one");
			if (plain.dye() != null || red.dye() != DyeColor.RED)
				helper.fail("a colour changed: " + plain.dye() + " and " + red.dye());
			if (!(helper.getBlockEntity(a.east(1)) instanceof PowerLineBlockEntity next) || !next.link(Direction.DOWN, Direction.EAST).connected())
				helper.fail("plain wires stopped joining each other");
			helper.succeed();
		});
	}

	/**
	 * With Create: Dragons Plus, its Bulk Coloring (a fan through dye fluid) dyes every kind of line, plain or another
	 * colour, and a dyed line's name says its colour.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void bulkColoringDyesLines(GameTestHelper helper) {
		var red = com.simibubi.create.api.registry.CreateBuiltInRegistries.FAN_PROCESSING_TYPE.get(
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_red"));
		for (Block line : new Block[] {ModBlocks.COPPER_FLUID_LINE.get(), ModBlocks.BRASS_FLUID_LINE.get(), ModBlocks.COPPER_POWER_LINE.get(), ModBlocks.BRASS_POWER_LINE.get()}) {
			ItemStack plain = new ItemStack(line), blue = new ItemStack(line);
			blue.set(com.comedymonkey.createaddons.registry.ModDataComponents.LINE_DYE.get(), DyeColor.BLUE);
			if (!(blue.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t)
				|| !t.getKey().equals("item.comedymonkeysaddons.dyed_line"))
				helper.fail("a blue " + line.getName().getString() + " doesn't say its colour");
			// Dragons Plus isn't installed: nothing more to check
			if (red == null) {
				if (net.neoforged.fml.ModList.get().isLoaded("create_dragons_plus"))
					helper.fail("Dragons Plus is installed but its red Bulk Coloring wasn't found");
				continue;
			}
			for (ItemStack in : new ItemStack[] {plain, blue}) {
				List<ItemStack> out = red.canProcess(in, helper.getLevel()) ? red.process(in, helper.getLevel()) : null;
				if (out == null || out.isEmpty() || !out.get(0).is(line.asItem())
					|| out.get(0).get(com.comedymonkey.createaddons.registry.ModDataComponents.LINE_DYE.get()) != DyeColor.RED)
					helper.fail("Bulk Coloring in red dye didn't make a " + (in == plain ? "plain " : "blue ") + line.getName().getString() + " red: " + out);
			}
		}
		helper.succeed();
	}

	/** Bulk washing (a fan through water) takes the dye off every kind of line. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void bulkWashingTakesTheDyeOff(GameTestHelper helper) {
		net.minecraft.world.item.crafting.RecipeType<?> splashing = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.get(
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "splashing"));
		for (Block line : new Block[] {ModBlocks.COPPER_FLUID_LINE.get(), ModBlocks.BRASS_FLUID_LINE.get(), ModBlocks.COPPER_POWER_LINE.get(), ModBlocks.BRASS_POWER_LINE.get()}) {
			ItemStack dyed = new ItemStack(line);
			dyed.set(com.comedymonkey.createaddons.registry.ModDataComponents.LINE_DYE.get(), DyeColor.RED);
			boolean washed = false;
			for (var h : helper.getLevel().getRecipeManager().getRecipes()) {
				var r = h.value();
				if (r.getType() != splashing || r.getIngredients().isEmpty())
					continue;
				// a plain line passes a washing fan untouched
				if (r.getIngredients().get(0).test(new ItemStack(line)))
					helper.fail("bulk washing takes plain " + line.getName().getString() + " too");
				if (!r.getIngredients().get(0).test(dyed))
					continue;
				ItemStack out = r.getResultItem(helper.getLevel().registryAccess());
				washed = out.is(line.asItem()) && out.get(com.comedymonkey.createaddons.registry.ModDataComponents.LINE_DYE.get()) == null;
			}
			if (!washed)
				helper.fail("no bulk washing makes a dyed " + line.getName().getString() + " plain");
		}
		helper.succeed();
	}

	/** An encased line keeps working. */
	@GameTest(template = COURSE, timeoutTicks = 80)
	public static void encasedLineKeepsWorking(GameTestHelper helper) {
		BlockPos[] t = tankToTank(helper, ModBlocks.COPPER_FLUID_LINE.get(), new FluidStack(Fluids.WATER, 8000), 4);
		line(helper, t[0].east(2)).setEncasing(Blocks.STONE_BRICKS.defaultBlockState());
		helper.runAtTickTime(50, () -> {
			if (amount(helper, t[1]) <= 0)
				helper.fail("an encased line stopped the flow");
			helper.succeed();
		});
	}

	// ---------- Power Lines ----------

	/** One block of wire lying on {@code face}. */
	static void wire(GameTestHelper helper, Block block, BlockPos pos, Direction face) {
		helper.setBlock(pos, block.defaultBlockState().setValue(PowerLineBlock.FACES.get(face), true));
	}

	static void relink(GameTestHelper helper, BlockPos... where) {
		for (BlockPos p : where)
			PowerLineBlock.relinkAround(helper.getLevel(), helper.absolutePos(p));
	}

	static void generator(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.LAVA_GENERATOR.get().defaultBlockState());
		fluids(helper, pos).fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
	}

	/** A motor whose base (where power goes in) faces west. */
	static void motor(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
	}

	static int generated(GameTestHelper helper, BlockPos generator) {
		return helper.getBlockEntity(generator) instanceof LavaGeneratorBlockEntity g ? g.getEnergy().getEnergyStored() : -1;
	}

	static int stored(GameTestHelper helper, BlockPos motor) {
		return helper.getBlockEntity(motor) instanceof ElectricMotorBlockEntity m ? m.getEnergy().getEnergyStored() : -1;
	}

	/** A generator, three blocks of wire on the floor and a motor: the motor gets power. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void wireCarriesPowerToAMotor(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), m = gen.east(4);
		generator(helper, gen);
		motor(helper, m);
		for (int i = 1; i <= 3; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		helper.runAtTickTime(2, () -> relink(helper, gen.east(1), gen.east(3)));
		helper.runAtTickTime(10, () -> {
			if (!(helper.getBlockEntity(gen.east(1)) instanceof PowerLineBlockEntity w) || w.link(Direction.DOWN, Direction.WEST) != WireLink.TERMINAL)
				helper.fail("the wire doesn't meet the generator");
		});
		helper.runAtTickTime(60, () -> {
			if (stored(helper, m) <= 0)
				helper.fail("the motor got no power through the wire");
			helper.succeed();
		});
	}

	/** A floor wire either side of a wall: not joined until a wrench drills through, then the motor gets power. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void wireThroughADrilledWall(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), a = gen.east(), wall = a.east(), b = wall.east(), m = b.east();
		generator(helper, gen);
		motor(helper, m);
		helper.setBlock(wall, Blocks.STONE);
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), a, Direction.DOWN);
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), b, Direction.DOWN);
		helper.runAtTickTime(2, () -> relink(helper, a, b));
		helper.runAtTickTime(10, () -> {
			if (((PowerLineBlockEntity) helper.getBlockEntity(a)).link(Direction.DOWN, Direction.EAST) != WireLink.NONE)
				helper.fail("the wire went through the wall before it was drilled");
			((PowerLineBlockEntity) helper.getBlockEntity(a)).wrenchJoint(Direction.EAST);
		});
		helper.runAtTickTime(15, () -> {
			if (((PowerLineBlockEntity) helper.getBlockEntity(a)).link(Direction.DOWN, Direction.EAST) != WireLink.THROUGH
				|| ((PowerLineBlockEntity) helper.getBlockEntity(b)).link(Direction.DOWN, Direction.WEST) != WireLink.THROUGH)
				helper.fail("the drilled wall didn't thread both wires");
		});
		helper.runAtTickTime(80, () -> {
			if (stored(helper, m) <= 0)
				helper.fail("no power through the drilled wall");
			helper.succeed();
		});
	}

	/** Up the wall on one side, in through the middle of the upper block, out and down the far side to a motor. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void wireThroughTheMiddleOfAWall(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), low = gen.east(), wall = low.east(), up = low.above(), far = wall.east().above(), farLow = wall.east(), m = farLow.east();
		generator(helper, gen);
		motor(helper, m);
		helper.setBlock(wall, Blocks.STONE);
		helper.setBlock(wall.above(), Blocks.STONE);
		Block line = ModBlocks.COPPER_POWER_LINE.get();
		helper.setBlock(low, line.defaultBlockState().setValue(PowerLineBlock.FACES.get(Direction.DOWN), true).setValue(PowerLineBlock.FACES.get(Direction.EAST), true));
		wire(helper, line, up, Direction.EAST);
		wire(helper, line, far, Direction.WEST);
		helper.setBlock(farLow, line.defaultBlockState().setValue(PowerLineBlock.FACES.get(Direction.DOWN), true).setValue(PowerLineBlock.FACES.get(Direction.WEST), true));
		helper.runAtTickTime(2, () -> relink(helper, low, up, far, farLow));
		helper.runAtTickTime(8, () -> ((PowerLineBlockEntity) helper.getBlockEntity(up)).wrenchJoint(Direction.EAST));
		helper.runAtTickTime(14, () -> {
			if (WireLink.hole(((PowerLineBlockEntity) helper.getBlockEntity(up)).links(Direction.EAST)) != 1
				|| WireLink.hole(((PowerLineBlockEntity) helper.getBlockEntity(far)).links(Direction.WEST)) != 1)
				helper.fail("the wires on the wall weren't threaded through its middle");
		});
		helper.runAtTickTime(80, () -> {
			if (stored(helper, m) <= 0)
				helper.fail("no power through the middle of the wall");
			helper.succeed();
		});
	}

	/** A hole joins two cable ends: a piece that branches at the hole (left, right and up) isn't threaded. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void aHoleJoinsOnlyTwoEnds(GameTestHelper helper) {
		BlockPos a = new BlockPos(2, 2, 4), wall = a.east(), b = wall.east();
		for (int dz = -1; dz <= 1; dz++)
			for (int dy = 0; dy <= 1; dy++)
				helper.setBlock(wall.offset(0, dy, dz), Blocks.STONE);
		Block line = ModBlocks.COPPER_POWER_LINE.get();
		wire(helper, line, a, Direction.EAST);
		wire(helper, line, b, Direction.WEST);
		wire(helper, line, b.north(), Direction.WEST);
		wire(helper, line, b.south(), Direction.WEST);
		wire(helper, line, b.above(), Direction.WEST);
		helper.runAtTickTime(2, () -> relink(helper, a, b, b.north(), b.south(), b.above()));
		helper.runAtTickTime(5, () -> ((PowerLineBlockEntity) helper.getBlockEntity(a)).wrenchJoint(Direction.EAST));
		helper.runAtTickTime(12, () -> {
			if (WireLink.hole(((PowerLineBlockEntity) helper.getBlockEntity(a)).links(Direction.EAST)) != 0
				|| WireLink.hole(((PowerLineBlockEntity) helper.getBlockEntity(b)).links(Direction.WEST)) != 0)
				helper.fail("a branching piece was threaded through the wall");
			helper.succeed();
		});
	}

	/** A floor wire under a motor raised on a block of air rises straight up into its base. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void wireRisesIntoARaisedMotor(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), under = gen.east(2), m = under.above();
		generator(helper, gen);
		helper.setBlock(m, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.UP));
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(), Direction.DOWN);
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), under, Direction.DOWN);
		helper.runAtTickTime(2, () -> relink(helper, gen.east(), under));
		helper.runAtTickTime(10, () -> {
			if ((((PowerLineBlockEntity) helper.getBlockEntity(under)).links(Direction.DOWN) & WireLink.RISE) == 0)
				helper.fail("the wire under the motor doesn't rise into its base");
		});
		helper.runAtTickTime(70, () -> {
			if (stored(helper, m) <= 0)
				helper.fail("no power up into the raised motor");
			helper.succeed();
		});
	}

	/** Lava Generators whose terminals touch even out their stores. */
	@GameTest(template = COURSE, timeoutTicks = 80)
	public static void touchingGeneratorsShareTheirStores(GameTestHelper helper) {
		BlockPos a = new BlockPos(2, 1, 6), b = a.east();
		generator(helper, a);
		helper.setBlock(b, ModBlocks.LAVA_GENERATOR.get().defaultBlockState());
		helper.runAtTickTime(60, () -> {
			int ea = generated(helper, a), eb = generated(helper, b);
			if (eb <= 0 || Math.abs(ea - eb) > Math.max(ea, eb) / 4)
				helper.fail("the generators didn't share: " + ea + " and " + eb + " FE");
			helper.succeed();
		});
	}

	/** A Breaker switched off cuts the power. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void breakerCutsThePower(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 5), m = gen.east(4);
		generator(helper, gen);
		motor(helper, m);
		for (int i = 1; i <= 3; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		// switched off before the wire has carried anything
		if (helper.getBlockEntity(gen.east(2)) instanceof LineBlockEntity b) {
			b.setFitting(Fitting.BREAKER, Direction.EAST);
			b.flipLever();
		}
		helper.runAtTickTime(2, () -> relink(helper, gen.east(1), gen.east(3)));
		helper.runAtTickTime(60, () -> {
			if (stored(helper, m) > 0)
				helper.fail("power got past a Breaker switched off");
			helper.succeed();
		});
	}

	/** Copper and brass wire side by side don't join. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void copperWireNeverJoinsBrass(GameTestHelper helper) {
		BlockPos a = new BlockPos(2, 1, 8);
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), a, Direction.DOWN);
		wire(helper, ModBlocks.BRASS_POWER_LINE.get(), a.east(), Direction.DOWN);
		helper.runAtTickTime(3, () -> {
			relink(helper, a);
			if (helper.getBlockEntity(a) instanceof PowerLineBlockEntity w && w.link(Direction.DOWN, Direction.EAST).connected())
				helper.fail("copper wire joined brass");
			helper.succeed();
		});
	}

	/** A wire up a wall: the floor wire turns up the wall in the same block (an inside corner). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void wireTurnsUpAWall(GameTestHelper helper) {
		BlockPos p = new BlockPos(5, 1, 10);
		helper.setBlock(p.north(), Blocks.STONE);
		helper.setBlock(p, ModBlocks.COPPER_POWER_LINE.get().defaultBlockState().setValue(PowerLineBlock.FACES.get(Direction.DOWN), true)
			.setValue(PowerLineBlock.FACES.get(Direction.NORTH), true));
		helper.runAtTickTime(3, () -> {
			relink(helper, p);
			if (!(helper.getBlockEntity(p) instanceof PowerLineBlockEntity w) || w.link(Direction.DOWN, Direction.NORTH) != WireLink.INSIDE
				|| w.link(Direction.NORTH, Direction.DOWN) != WireLink.INSIDE)
				helper.fail("the floor wire doesn't turn up the wall");
			helper.succeed();
		});
	}

	/** Generator, wire, a Line Post, a span to a second post, wire, motor: the span carries the power. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void spanCarriesPower(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 14), w1 = gen.east(), postA = gen.east(2), postB = gen.east(8), w2 = postB.east(), m = postB.east(2);
		generator(helper, gen);
		motor(helper, m);
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), w1, Direction.DOWN);
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), w2, Direction.DOWN);
		helper.setBlock(postA, ModBlocks.LINE_POST.get().defaultBlockState());
		helper.setBlock(postB, ModBlocks.LINE_POST.get().defaultBlockState());
		helper.runAtTickTime(2, () -> {
			relink(helper, w1, w2);
			if (!(helper.getBlockEntity(postA) instanceof LinePostBlockEntity a) || !(helper.getBlockEntity(postB) instanceof LinePostBlockEntity b)) {
				helper.fail("no posts");
				return;
			}
			Span span = new Span(0, postB.subtract(postA), 0, LineTier.COPPER, null, true, 2);
			a.addSpan(span);
			b.addSpan(span.reversed());
			if (!(helper.getBlockEntity(w1) instanceof PowerLineBlockEntity w) || w.link(Direction.DOWN, Direction.EAST) != WireLink.POST)
				helper.fail("the wire doesn't run under the post's foot");
		});
		helper.runAtTickTime(80, () -> {
			if (stored(helper, m) <= 0)
				helper.fail("no power across the span");
			helper.succeed();
		});
	}

	/** Generator, wire of one tier, a pole, a span of another, wire, motor: whether power gets from the one to the other. */
	private static void acrossAPole(GameTestHelper helper, Block from, LineTier span, Block to, boolean expected, String fail) {
		acrossAPole(helper, from, span, to, null, null, expected, fail);
	}

	/** The same, with the wires in one colour and the span in another (null: plain). */
	private static void acrossAPole(GameTestHelper helper, Block from, LineTier span, Block to, @org.jetbrains.annotations.Nullable DyeColor wires,
		@org.jetbrains.annotations.Nullable DyeColor spanDye, boolean expected, String fail) {
		BlockPos gen = new BlockPos(1, 1, 14), w1 = gen.east(), postA = gen.east(2), postB = gen.east(8), w2 = postB.east(), m = postB.east(2);
		generator(helper, gen);
		motor(helper, m);
		wire(helper, from, w1, Direction.DOWN);
		wire(helper, to, w2, Direction.DOWN);
		helper.setBlock(postA, ModBlocks.LINE_POST.get().defaultBlockState());
		helper.setBlock(postB, ModBlocks.LINE_POST.get().defaultBlockState());
		helper.runAtTickTime(2, () -> {
			for (BlockPos w : new BlockPos[] {w1, w2})
				if (wires != null && helper.getBlockEntity(w) instanceof PowerLineBlockEntity be)
					be.setDye(wires);
			relink(helper, w1, w2);
			if (!(helper.getBlockEntity(postA) instanceof LinePostBlockEntity a) || !(helper.getBlockEntity(postB) instanceof LinePostBlockEntity b)) {
				helper.fail("no posts");
				return;
			}
			Span s = new Span(0, postB.subtract(postA), 0, span, spanDye, true, 2);
			a.addSpan(s);
			b.addSpan(s.reversed());
		});
		helper.runAtTickTime(80, () -> {
			if ((stored(helper, m) > 0) != expected)
				helper.fail(fail);
			helper.succeed();
		});
	}

	/** A red line, two pieces of it as a save from before colours left them (no colour, not read again yet): it still carries. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void oldSaveLinesStillJoin(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 18), m = gen.east(5);
		generator(helper, gen);
		motor(helper, m);
		for (int i = 1; i <= 4; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		helper.runAtTickTime(2, () -> {
			for (int i = 1; i <= 4; i++)
				if (helper.getBlockEntity(gen.east(i)) instanceof PowerLineBlockEntity be)
					be.setDye(DyeColor.RED);
			relink(helper, gen.east(1), gen.east(2), gen.east(3), gen.east(4));
		});
		helper.runAtTickTime(6, () -> {
			com.comedymonkey.createaddons.content.line.PowerGridTesting.asOldSave(helper.getLevel(), helper.absolutePos(gen.east(2)));
			com.comedymonkey.createaddons.content.line.PowerGridTesting.asOldSave(helper.getLevel(), helper.absolutePos(gen.east(3)));
			// from here only the line, through those pieces, can fill it
			if (helper.getBlockEntity(m) instanceof ElectricMotorBlockEntity motor)
				((com.comedymonkey.createaddons.content.electric.EnergyBuffer) motor.getEnergy()).setEnergy(0);
		});
		helper.runAtTickTime(90, () -> {
			if (stored(helper, m) <= 0)
				helper.fail("the line stopped at the pieces from an old save");
			helper.succeed();
		});
	}

	/** A whole pole joins: brass wire into a pole feeds a copper span out of it. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void brassFeedsCopperAtAPole(GameTestHelper helper) {
		acrossAPole(helper, ModBlocks.BRASS_POWER_LINE.get(), LineTier.COPPER, ModBlocks.COPPER_POWER_LINE.get(), true,
			"brass didn't feed copper at the pole");
	}

	/** A pole joins only cables of one colour: a red span between plain wires carries nothing. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void poleJoinsOnlyOneColour(GameTestHelper helper) {
		acrossAPole(helper, ModBlocks.COPPER_POWER_LINE.get(), LineTier.COPPER, ModBlocks.COPPER_POWER_LINE.get(), null, DyeColor.RED, false,
			"a pole joined plain wires to a red span");
	}

	/** Red wires and a red span join at the poles. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void poleJoinsItsOwnColour(GameTestHelper helper) {
		acrossAPole(helper, ModBlocks.COPPER_POWER_LINE.get(), LineTier.COPPER, ModBlocks.COPPER_POWER_LINE.get(), DyeColor.RED, DyeColor.RED, true,
			"red wires didn't join a red span at the poles");
	}

	/** Copper never feeds brass, even on one pole. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void copperNeverFeedsBrass(GameTestHelper helper) {
		acrossAPole(helper, ModBlocks.COPPER_POWER_LINE.get(), LineTier.BRASS, ModBlocks.BRASS_POWER_LINE.get(), false,
			"copper fed brass at the pole");
	}

	/** One generator, two motors on the same line: both get a share. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void powerIsShared(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), m1 = gen.east(4), m2 = m1.south();
		generator(helper, gen);
		motor(helper, m1);
		motor(helper, m2);
		for (int i = 1; i <= 3; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(3).south(), Direction.DOWN);
		helper.runAtTickTime(2, () -> relink(helper, gen.east(1), gen.east(3), gen.east(3).south()));
		helper.runAtTickTime(80, () -> {
			if (stored(helper, m1) <= 0 || stored(helper, m2) <= 0)
				helper.fail("a motor got no share: " + stored(helper, m1) + " and " + stored(helper, m2));
			helper.succeed();
		});
	}

	/** A Power Line Splitter goes on a junction, not a straight piece. */
	@GameTest(template = COURSE, timeoutTicks = 30)
	public static void splitterGoesOnAJunction(GameTestHelper helper) {
		BlockPos a = new BlockPos(2, 1, 2), t = a.east(), b = t.east(), c = t.south(), s1 = new BlockPos(2, 1, 8), s2 = s1.east();
		for (BlockPos p : new BlockPos[] {a, t, b, c, s1, s2})
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), p, Direction.DOWN);
		helper.runAtTickTime(2, () -> relink(helper, a, t, b, c, s1, s2));
		helper.runAtTickTime(5, () -> {
			PowerLineBlock line = (PowerLineBlock) ModBlocks.COPPER_POWER_LINE.get();
			if (line.sideFor(Fitting.SPLITTER, helper.getLevel(), helper.absolutePos(t), helper.getBlockState(t)) == null)
				helper.fail("a Splitter doesn't fit the junction");
			if (line.sideFor(Fitting.SPLITTER, helper.getLevel(), helper.absolutePos(s1), helper.getBlockState(s1)) != null)
				helper.fail("a Splitter fits a straight piece");
			helper.succeed();
		});
	}

	/** One generator, a Splitter on a T: the branch set First gets the power, the one set Last none while it's short. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void splitterServesTheFirstBranchFirst(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), t = gen.east(2), first = t.east(2), last = t.south(2);
		generator(helper, gen);
		helper.setBlock(first, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		for (BlockPos p : new BlockPos[] {gen.east(), t, t.east(), t.south()})
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), p, Direction.DOWN);
		// fitted in the same step as the wires join, so no update can share the power out before it's set
		helper.runAtTickTime(2, () -> {
			relink(helper, gen.east(), t, t.east(), t.south());
			if (!(helper.getBlockEntity(t) instanceof PowerLineBlockEntity s)) {
				helper.fail("no junction");
				return;
			}
			s.setFitting(Fitting.SPLITTER, Direction.EAST);
			byte[] levels = {1, 1, 1, 1, 1, 1};
			levels[Direction.EAST.ordinal()] = 0;
			levels[Direction.SOUTH.ordinal()] = 2;
			s.setPriorities(levels);
		});
		// the Last branch's motor only once the Splitter is set, so no update before it can share the power out evenly
		helper.runAtTickTime(4, () -> {
			helper.setBlock(last, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.SOUTH));
			relink(helper, t.south());
		});
		helper.runAtTickTime(90, () -> {
			if (stored(helper, first) <= 0)
				helper.fail("the First branch got no power");
			if (stored(helper, last) > 0)
				helper.fail("the Last branch got power while the First wanted more: " + stored(helper, last));
			helper.succeed();
		});
	}

	/** Generator, wire of one tier, a Breaker, wire of the other, motor: whether power gets through the Breaker. */
	private static void acrossABreaker(GameTestHelper helper, Block from, Block to, boolean expected, String fail) {
		BlockPos gen = new BlockPos(1, 1, 5), m = gen.east(4);
		generator(helper, gen);
		motor(helper, m);
		wire(helper, from, gen.east(1), Direction.DOWN);
		wire(helper, from, gen.east(2), Direction.DOWN);
		wire(helper, to, gen.east(3), Direction.DOWN);
		helper.runAtTickTime(2, () -> {
			relink(helper, gen.east(1), gen.east(2), gen.east(3));
			if (helper.getBlockEntity(gen.east(2)) instanceof LineBlockEntity b)
				b.setFitting(Fitting.BREAKER, Direction.EAST);
			// the other tier joins it only once that joint is wrenched
			if (helper.getBlockEntity(gen.east(3)) instanceof LineBlockEntity w)
				w.wrenchJoint(Direction.WEST);
		});
		helper.runAtTickTime(80, () -> {
			if ((stored(helper, m) > 0) != expected)
				helper.fail(fail);
			helper.succeed();
		});
	}

	/** A Breaker beside a wire of the other tier doesn't join it until that joint is wrenched. */
	@GameTest(template = COURSE, timeoutTicks = 30)
	public static void breakerJoinsTiersOnlyWhenWrenched(GameTestHelper helper) {
		BlockPos b = new BlockPos(2, 1, 8), c = b.east();
		wire(helper, ModBlocks.BRASS_POWER_LINE.get(), b.west(), Direction.DOWN);
		wire(helper, ModBlocks.BRASS_POWER_LINE.get(), b, Direction.DOWN);
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), c, Direction.DOWN);
		helper.runAtTickTime(2, () -> {
			relink(helper, b.west(), b, c);
			if (helper.getBlockEntity(b) instanceof LineBlockEntity be)
				be.setFitting(Fitting.BREAKER, Direction.EAST);
		});
		helper.runAtTickTime(5, () -> {
			if (helper.getBlockEntity(b) instanceof PowerLineBlockEntity w && w.link(Direction.DOWN, Direction.EAST).connected())
				helper.fail("the Breaker joined copper before the joint was wrenched");
			if (helper.getBlockEntity(c) instanceof LineBlockEntity w)
				w.wrenchJoint(Direction.WEST);
		});
		helper.runAtTickTime(8, () -> {
			if (!(helper.getBlockEntity(b) instanceof PowerLineBlockEntity w) || w.link(Direction.DOWN, Direction.EAST) != WireLink.LINE)
				helper.fail("the wrenched joint didn't join brass to copper at the Breaker");
			helper.succeed();
		});
	}

	/** One network meeting a generator at both its terminals, a motor at each end: counted once, both motors get an even share. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void generatorMetTwiceCountsOnce(GameTestHelper helper) {
		BlockPos gen = new BlockPos(3, 1, 2), west = gen.west(2), east = gen.east(2);
		generator(helper, gen);
		helper.setBlock(west, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.WEST));
		helper.setBlock(east, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		// one network: the two wires joined round the back of the generator
		BlockPos[] run = {gen.west(), gen.west().south(), gen.south(), gen.east().south(), gen.east()};
		for (BlockPos p : run)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), p, Direction.DOWN);
		helper.runAtTickTime(2, () -> relink(helper, run));
		helper.runAtTickTime(90, () -> {
			int a = stored(helper, west), b = stored(helper, east);
			if (a <= 0 || b <= 0 || Math.min(a, b) < Math.max(a, b) / 2)
				helper.fail("the two motors didn't share evenly: " + a + " and " + b);
			helper.succeed();
		});
	}

	/** Brass feeds copper through a Breaker. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void brassFeedsCopperThroughABreaker(GameTestHelper helper) {
		acrossABreaker(helper, ModBlocks.BRASS_POWER_LINE.get(), ModBlocks.COPPER_POWER_LINE.get(), true, "brass didn't feed copper through the Breaker");
	}

	/** Copper never feeds brass, even through a Breaker. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void copperNeverFeedsBrassThroughABreaker(GameTestHelper helper) {
		acrossABreaker(helper, ModBlocks.COPPER_POWER_LINE.get(), ModBlocks.BRASS_POWER_LINE.get(), false, "copper fed brass through the Breaker");
	}

	/** A full motor using less than the generator makes stays full, tick after tick (no once-a-second dips). */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void motorWithEnoughPowerStaysFull(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), m = gen.east(4);
		generator(helper, gen);
		motor(helper, m);
		for (int i = 1; i <= 3; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		helper.runAtTickTime(2, () -> {
			relink(helper, gen.east(1), gen.east(3));
			if (helper.getBlockEntity(m) instanceof ElectricMotorBlockEntity motor) {
				// half a generator's output, starting full
				motor.applySettings(64, ElectricMotorBlockEntity.STRESS_STEP * Math.max(1,
					(int) (CMConfig.SERVER.generatorFePerTick() / 2 / CMConfig.SERVER.motorFePerStress()) / ElectricMotorBlockEntity.STRESS_STEP), false);
				motor.getEnergy().receiveEnergy(motor.getEnergy().getMaxEnergyStored(), false);
			}
		});
		int[] lowest = {Integer.MAX_VALUE};
		for (int t = 60; t <= 140; t++)
			helper.runAtTickTime(t, () -> {
				if (helper.getBlockEntity(m) instanceof ElectricMotorBlockEntity motor)
					lowest[0] = Math.min(lowest[0], motor.getEnergy().getEnergyStored() * 100 / Math.max(1, motor.getEnergy().getMaxEnergyStored()));
			});
		helper.runAtTickTime(150, () -> {
			if (lowest[0] < 99)
				helper.fail("the motor's store dipped to " + lowest[0] + "% with more than enough power coming in");
			helper.succeed();
		});
	}

	/**
	 * A generator with a brass line on one side and a copper line on the other, a motor on each: one network through the
	 * generator, so all three show the same use, both motors' together, and both motors run.
	 */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void linesMeetingAGeneratorAreOneNetwork(GameTestHelper helper) {
		BlockPos gen = new BlockPos(5, 1, 2), east = gen.east(4), west = gen.west(4);
		generator(helper, gen);
		motor(helper, east);
		helper.setBlock(west, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.WEST));
		for (int i = 1; i <= 3; i++) {
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
			wire(helper, ModBlocks.BRASS_POWER_LINE.get(), gen.west(i), Direction.DOWN);
		}
		// each motor uses an eighth of what the generator makes, starting full (so it takes only what it uses)
		int stress = ElectricMotorBlockEntity.STRESS_STEP * Math.max(1,
			(int) (CMConfig.SERVER.generatorFePerTick() / 8 / CMConfig.SERVER.motorFePerStress()) / ElectricMotorBlockEntity.STRESS_STEP);
		helper.runAtTickTime(2, () -> {
			relink(helper, gen.east(1), gen.east(3), gen.west(1), gen.west(3));
			for (BlockPos m : new BlockPos[] {east, west})
				if (helper.getBlockEntity(m) instanceof ElectricMotorBlockEntity motor) {
					motor.applySettings(64, stress, false);
					motor.getEnergy().receiveEnergy(motor.getEnergy().getMaxEnergyStored(), false);
				}
		});
		helper.runAtTickTime(140, () -> {
			double both = 2 * ElectricMotorBlockEntity.usageFor(stress);
			for (BlockPos p : new BlockPos[] {gen, east, west}) {
				int uses = helper.getBlockEntity(p).saveWithoutMetadata(helper.getLevel().registryAccess()).getInt("NetUses");
				if (Math.abs(uses - both) > both / 10)
					helper.fail("the network reads " + uses + " FE a tick used at " + p.toShortString() + ", not both motors' " + (int) both);
			}
			// both started full: without power each would be down by over 17,000 FE by now
			for (BlockPos m : new BlockPos[] {east, west})
				if (helper.getBlockEntity(m) instanceof ElectricMotorBlockEntity motor
					&& motor.getEnergy().getEnergyStored() < motor.getEnergy().getMaxEnergyStored() - 40 * both)
					helper.fail("the motor at " + m.toShortString() + " isn't being kept full: " + motor.getEnergy().getEnergyStored());
			helper.succeed();
		});
	}

	/** A generator powering a motor that takes all it makes: its store stays level from tick to tick. */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void generatorStoreStaysLevel(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), m = gen.east(4);
		generator(helper, gen);
		motor(helper, m);
		for (int i = 1; i <= 3; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		helper.runAtTickTime(2, () -> relink(helper, gen.east(1), gen.east(3)));
		int[] range = {Integer.MAX_VALUE, Integer.MIN_VALUE};
		for (int t = 80; t <= 140; t++)
			helper.runAtTickTime(t, () -> {
				int e = generated(helper, gen);
				range[0] = Math.min(range[0], e);
				range[1] = Math.max(range[1], e);
			});
		helper.runAtTickTime(150, () -> {
			if (range[1] - range[0] > 3 * CMConfig.SERVER.generatorFePerTick())
				helper.fail("the generator's store swung from " + range[0] + " to " + range[1] + " FE");
			helper.succeed();
		});
	}

	/** A draw from a generator's record (on paper) marks the grid to be saved, and the record comes back from a save as it was. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void paperDrawsAreSaved(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), m = gen.east(4);
		generator(helper, gen);
		motor(helper, m);
		for (int i = 1; i <= 3; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		helper.runAtTickTime(2, () -> relink(helper, gen.east(1), gen.east(3)));
		helper.runAtTickTime(80, () -> {
			BlockPos key = helper.absolutePos(gen);
			if (PowerGridTesting.store(helper.getLevel(), key) < 0)
				helper.fail("the generator a line meets has no record");
			if (!PowerGridTesting.drawMarksDirty(helper.getLevel(), key))
				helper.fail("a draw from the record didn't mark the grid to be saved");
			double now = PowerGridTesting.store(helper.getLevel(), key), back = PowerGridTesting.storeAfterReload(helper.getLevel(), key);
			if (Math.abs(now - back) > 1)
				helper.fail("the record came back from a save at " + back + " FE, not " + now);
			helper.succeed();
		});
	}

	/** A generator keeps a record only while a line meets it: take the wire away and the record goes. */
	@GameTest(template = COURSE, timeoutTicks = 160)
	public static void recordGoesWithItsLine(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), m = gen.east(4);
		generator(helper, gen);
		motor(helper, m);
		for (int i = 1; i <= 3; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		helper.runAtTickTime(2, () -> relink(helper, gen.east(1), gen.east(3)));
		BlockPos key = helper.absolutePos(gen);
		helper.runAtTickTime(80, () -> {
			if (PowerGridTesting.store(helper.getLevel(), key) < 0)
				helper.fail("the generator a line meets has no record");
			helper.setBlock(gen.east(1), Blocks.AIR);
		});
		helper.runAtTickTime(140, () -> {
			if (PowerGridTesting.store(helper.getLevel(), key) >= 0)
				helper.fail("the generator kept its record with no line meeting it");
			helper.succeed();
		});
	}

	/** Two touching Lava Generators, a cable on one, both on paper: the line draws on both of them. */
	@GameTest(template = COURSE, timeoutTicks = 240)
	public static void touchingGeneratorsShareOnPaper(GameTestHelper helper) {
		BlockPos a = new BlockPos(1, 1, 2), b = a.east(), m = b.east(4);
		generator(helper, a);
		generator(helper, b);
		motor(helper, m);
		for (int i = 1; i <= 3; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), b.east(i), Direction.DOWN);
		helper.runAtTickTime(2, () -> {
			relink(helper, b.east(1), b.east(3));
			// at its lowest stress it uses next to nothing, so what it gains is what it's given
			if (helper.getBlockEntity(m) instanceof ElectricMotorBlockEntity motor)
				motor.applySettings(64, ElectricMotorBlockEntity.STRESS_STEP, false);
		});
		BlockPos ka = helper.absolutePos(a), kb = helper.absolutePos(b);
		int[] start = {0};
		// both stores empty, then both away: what the motor gets is what their records make
		helper.runAtTickTime(78, () -> {
			for (BlockPos g : new BlockPos[] {a, b})
				if (helper.getBlockEntity(g) instanceof LavaGeneratorBlockEntity be)
					be.getEnergy().setEnergy(0);
		});
		helper.runAtTickTime(80, () -> {
			if (PowerGridTesting.store(helper.getLevel(), ka) < 0)
				helper.fail("the generator in the row (no cable of its own) has no record");
			PowerGridTesting.away(helper.getLevel(), ka, true, 140);
			PowerGridTesting.away(helper.getLevel(), kb, true, 140);
			start[0] = stored(helper, m);
		});
		helper.runAtTickTime(200, () -> {
			PowerGridTesting.away(helper.getLevel(), ka, false, 0);
			PowerGridTesting.away(helper.getLevel(), kb, false, 0);
			int got = stored(helper, m) - start[0], one = CMConfig.SERVER.generatorFePerTick() * 120;
			// one generator's records make about 120 ticks' worth here (less the wait for the next plan); two make twice that
			if (got < one * 1.4)
				helper.fail("on paper the line only drew " + got + " FE from the pair: about one generator's worth (" + one + ")");
			helper.succeed();
		});
	}

	/** A copper line fed by two brass lines through two poles, one with nothing on it: the other gives copper all it wants. */
	@GameTest(template = COURSE, timeoutTicks = 80)
	public static void copperFedByTwoBrassLines(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 14), p1 = gen.east(2), t = gen.east(4), p2 = gen.east(6), m = t.south(2);
		generator(helper, gen);
		helper.setBlock(m, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.SOUTH));
		wire(helper, ModBlocks.BRASS_POWER_LINE.get(), gen.east(1), Direction.DOWN);
		// (posts need something solid under them; the course's floor isn't everywhere)
		helper.setBlock(p1.below(), Blocks.STONE);
		helper.setBlock(p2.below(), Blocks.STONE);
		helper.setBlock(p1, ModBlocks.LINE_POST.get().defaultBlockState());
		for (int i = 3; i <= 5; i++)
			wire(helper, ModBlocks.COPPER_POWER_LINE.get(), gen.east(i), Direction.DOWN);
		wire(helper, ModBlocks.COPPER_POWER_LINE.get(), t.south(), Direction.DOWN);
		helper.setBlock(p2, ModBlocks.LINE_POST.get().defaultBlockState());
		wire(helper, ModBlocks.BRASS_POWER_LINE.get(), p2.east(), Direction.DOWN);
		helper.runAtTickTime(2, () -> {
			if (helper.getBlockEntity(gen) instanceof LavaGeneratorBlockEntity be)
				be.getEnergy().setEnergy(100_000);
			relink(helper, gen.east(1), gen.east(3), t, gen.east(5), t.south(), p2.east());
		});
		// the most it moves into the motor in a tick, after the first update
		int[] last = {-1}, most = {0};
		for (int tick = 3; tick <= 30; tick++)
			helper.runAtTickTime(tick, () -> {
				int e = stored(helper, m);
				if (last[0] >= 0)
					most[0] = Math.max(most[0], e - last[0]);
				last[0] = e;
			});
		helper.runAtTickTime(32, () -> {
			int rate = CMConfig.SERVER.powerLineCopperFePerTick();
			if (most[0] < rate * 3 / 4) {
				StringBuilder why = new StringBuilder();
				for (BlockPos p : new BlockPos[] {gen.east(1), gen.east(3), t, gen.east(5), t.south(), p2.east()})
					if (helper.getBlockEntity(p) instanceof PowerLineBlockEntity w) {
						why.append(' ').append(p.getX()).append(':');
						for (Direction d : WireLink.across(Direction.DOWN))
							why.append(d.getName().charAt(0)).append('=').append(w.link(Direction.DOWN, d).name().charAt(0));
					}
				helper.fail("copper got at most " + most[0] + " FE a tick from the brass, not its " + rate  + "; generator " + generated(helper, gen)
					+ ", motor " + stored(helper, m) + ";" + why);
			}
			helper.succeed();
		});
	}

	/** A Lava Generator burns flat out while it has lava, even with nothing taking its power. */
	@GameTest(template = COURSE, timeoutTicks = 80)
	public static void lavaGeneratorAlwaysRuns(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2);
		generator(helper, gen);
		helper.runAtTickTime(60, () -> {
			if (amount(helper, gen) >= 8000)
				helper.fail("the generator didn't burn with nothing taking its power");
			helper.succeed();
		});
	}

	/** Posts stack: the bottom becomes the foot, the top the head; a branch adds an insulator. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void postsStackIntoAPole(GameTestHelper helper) {
		BlockPos foot = new BlockPos(16, 1, 14);
		helper.setBlock(foot.below(), Blocks.STONE);
		for (int i = 0; i < 3; i++)
			helper.setBlock(foot.above(i), ModBlocks.LINE_POST.get().defaultBlockState());
		helper.runAtTickTime(3, () -> {
			helper.assertBlockProperty(foot, LinePostBlock.ROLE, PostRole.FOOT);
			helper.assertBlockProperty(foot.above(), LinePostBlock.ROLE, PostRole.POLE);
			helper.assertBlockProperty(foot.above(2), LinePostBlock.ROLE, PostRole.HEAD);
			helper.setBlock(foot.above(2), helper.getBlockState(foot.above(2)).setValue(LinePostBlock.BRANCHES.get(Direction.EAST), true));
			if (!LinePostBlock.hasInsulator(helper.getBlockState(foot.above(2)), 2) || LinePostBlock.hasInsulator(helper.getBlockState(foot.above(2)), 1))
				helper.fail("the east branch's insulator isn't there (or another is)");
			helper.succeed();
		});
	}
}
