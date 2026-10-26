package com.comedymonkey.createaddons.gametest;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.electric.BatteryBlock;
import com.comedymonkey.createaddons.content.electric.BatteryBlockEntity;
import com.comedymonkey.createaddons.content.line.LinePostBlockEntity;
import com.comedymonkey.createaddons.content.line.LineTier;
import com.comedymonkey.createaddons.content.line.Span;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Batteries and Industrial Batteries in a real world: banks, forming, coming apart, the network (./gradlew runGameTestServer). */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BatteryGameTests {
	/** 24 x 12 x 24 of air: room for a tower 9 high and more. */
	private static final String COURSE = "battery_course";

	private BatteryGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(BatteryGameTests.class);
	}

	// ---------- setting up ----------

	static void battery(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.BATTERY.get().defaultBlockState());
	}

	/** Batteries filling a box from its west-north-bottom corner, each holding {@code each}. */
	static void batteries(GameTestHelper helper, BlockPos corner, int w, int h, int d, int each) {
		for (int y = 0; y < h; y++)
			for (int z = 0; z < d; z++)
				for (int x = 0; x < w; x++) {
					BlockPos p = corner.offset(x, y, z);
					battery(helper, p);
					if (each > 0)
						be(helper, p).setCharge(each);
				}
	}

	static BatteryBlockEntity be(GameTestHelper helper, BlockPos pos) {
		if (!(helper.getBlockEntity(pos) instanceof BatteryBlockEntity be))
			throw new net.minecraft.gametest.framework.GameTestAssertException("no Battery at " + pos);
		return be;
	}

	static boolean formed(GameTestHelper helper, BlockPos pos) {
		BlockState s = helper.getBlockState(pos);
		return s.getBlock() instanceof BatteryBlock && s.getValue(BatteryBlock.FORMED);
	}

	/** Every Battery's share in a box, added up. */
	static long total(GameTestHelper helper, BlockPos corner, int w, int h, int d) {
		long sum = 0;
		for (int y = 0; y < h; y++)
			for (int z = 0; z < d; z++)
				for (int x = 0; x < w; x++)
					if (helper.getLevel().getBlockEntity(helper.absolutePos(corner.offset(x, y, z))) instanceof BatteryBlockEntity be)
						sum += be.share();
		return sum;
	}

	static IEnergyStorage energy(GameTestHelper helper, BlockPos pos, Direction side) {
		IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), side);
		if (e == null)
			helper.fail("no energy at " + pos + " on " + side);
		return e;
	}

	/** A Line Post standing on a block of stone. */
	static void post(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos.below(), Blocks.STONE);
		helper.setBlock(pos, ModBlocks.LINE_POST.get().defaultBlockState());
	}

	/** The master a tower on this footprint (west-north corner) facing north has: the middle of its front row. */
	static BlockPos master(BlockPos corner) {
		return corner.east();
	}

	// ---------- banks ----------

	/** Three Batteries side by side are one bank: one store, three Batteries' room, three Batteries' rate; one on top isn't in it. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void bankSharesOneStore(GameTestHelper helper) {
		BlockPos a = new BlockPos(2, 1, 2);
		batteries(helper, a, 3, 1, 1, 0);
		battery(helper, a.above());
		helper.runAtTickTime(2, () -> be(helper, a.east()).setCharge(300_000));
		helper.runAtTickTime(4, () -> {
			int cap = CMConfig.SERVER.batteryCapacity(), rate = CMConfig.SERVER.batteryFePerTick();
			IEnergyStorage end = energy(helper, a, Direction.WEST);
			if (end.getEnergyStored() != 300_000 || end.getMaxEnergyStored() != 3 * cap)
				helper.fail("the bank reads " + end.getEnergyStored() + " / " + end.getMaxEnergyStored());
			for (int i = 0; i < 3; i++)
				if (be(helper, a.east(i)).share() != 100_000)
					helper.fail("Battery " + i + " holds " + be(helper, a.east(i)).share() + ", not its third");
			if (energy(helper, a.above(), Direction.WEST).getMaxEnergyStored() != cap)
				helper.fail("the one on top joined the bank");
			// a tick's worth out, at three Batteries' rate, and no more that tick
			int got = end.extractEnergy(Integer.MAX_VALUE, false), more = end.extractEnergy(Integer.MAX_VALUE, false);
			if (got != 3 * rate || more != 0)
				helper.fail("took " + got + " then " + more + " in a tick, not " + 3 * rate + " then none");
			if (helper.getBlockState(a).getValue(BatteryBlock.RIGHT) == helper.getBlockState(a).getValue(BatteryBlock.LEFT))
				helper.fail("the end Battery shows a bus bar on both sides, or neither");
			helper.succeed();
		});
	}

	/** A Battery's charge goes with it as an item. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void itemKeepsCharge(GameTestHelper helper) {
		BlockPos p = new BlockPos(2, 1, 2);
		battery(helper, p);
		helper.runAtTickTime(2, () -> {
			be(helper, p).setCharge(123_456);
			List<ItemStack> drops = Block.getDrops(helper.getBlockState(p), helper.getLevel(), helper.absolutePos(p), helper.getBlockEntity(p));
			if (drops.size() != 1 || drops.get(0).getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), 0) != 123_456)
				helper.fail("dropped " + drops);
			helper.succeed();
		});
	}

	/** Shift-wrenched, a Battery goes into the player's inventory with its charge. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void wrenchPicksUpWithCharge(GameTestHelper helper) {
		BlockPos p = new BlockPos(2, 1, 2);
		battery(helper, p);
		helper.runAtTickTime(2, () -> {
			be(helper, p).setCharge(123_456);
			net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
			BlockPos abs = helper.absolutePos(p);
			net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs),
				Direction.UP, abs, false);
			((BatteryBlock) ModBlocks.BATTERY.get()).onSneakWrenched(helper.getBlockState(p),
				new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
			if (!helper.getBlockState(p).isAir())
				helper.fail("the Battery is still there");
			int got = -1;
			for (ItemStack s : player.getInventory().items)
				if (s.is(ModBlocks.BATTERY.get().asItem()))
					got = s.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), 0);
			if (got != 123_456)
				helper.fail("picked up with " + got + " FE, not 123,456");
			helper.succeed();
		});
	}

	/** On top of a tower, one Battery placed fills the whole layer (nine from the stack, like a vault) and it grows. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void placingOnATowerFillsTheLayer(GameTestHelper helper) {
		BlockPos c = new BlockPos(2, 1, 2), top = c.offset(1, 2, 1);
		batteries(helper, c, 3, 3, 3, 10_000);
		ItemStack stack = new ItemStack(ModBlocks.BATTERY.get().asItem(), 9);
		stack.set(ModDataComponents.BATTERY_CHARGE.get(), 1_000);
		helper.runAtTickTime(5, () -> {
			net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
			BlockPos abs = helper.absolutePos(top);
			net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs).add(0, 0.5, 0),
				Direction.UP, abs, false);
			((net.minecraft.world.item.BlockItem) stack.getItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(player,
				net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit));
		});
		helper.runAtTickTime(9, () -> {
			if (!stack.isEmpty())
				helper.fail(stack.getCount() + " Batteries left in the stack, not none");
			if (be(helper, master(c)).height() != 4)
				helper.fail("the tower is " + be(helper, master(c)).height() + " high, not 4");
			if (energy(helper, master(c), Direction.UP).getEnergyStored() != 27 * 10_000 + 9 * 1_000)
				helper.fail("the tower holds " + energy(helper, master(c), Direction.UP).getEnergyStored());
			helper.succeed();
		});
	}

	/** A Battery beside a motor's base runs it, as a generator would. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void batteryRunsAMotorBesideIt(GameTestHelper helper) {
		BlockPos b = new BlockPos(2, 1, 2), m = b.east();
		battery(helper, b);
		LineGameTests.motor(helper, m);
		helper.runAtTickTime(2, () -> be(helper, b).setCharge(200_000));
		helper.runAtTickTime(20, () -> {
			if (LineGameTests.stored(helper, m) <= 0)
				helper.fail("the motor got nothing from the Battery beside it");
			helper.succeed();
		});
	}

	/** On a line, a Battery fills from a generator's spare, and gives to a motor when there's no generator. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void lineChargesAndDrawsABattery(GameTestHelper helper) {
		BlockPos gen = new BlockPos(1, 1, 2), w1 = gen.east(), b1 = gen.east(2);
		BlockPos b2 = new BlockPos(1, 1, 8), w2 = b2.east(), m = b2.east(2);
		LineGameTests.generator(helper, gen);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), w1, Direction.DOWN);
		battery(helper, b1);
		battery(helper, b2);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), w2, Direction.DOWN);
		LineGameTests.motor(helper, m);
		helper.runAtTickTime(2, () -> {
			be(helper, b2).setCharge(400_000);
			LineGameTests.relink(helper, w1, w2);
		});
		helper.runAtTickTime(120, () -> {
			if (be(helper, b1).share() <= 0)
				helper.fail("the generator's spare didn't charge the Battery");
			if (LineGameTests.stored(helper, m) <= 0 || be(helper, b2).share() >= 400_000)
				helper.fail("the Battery didn't run the motor on its line");
			helper.succeed();
		});
	}

	// ---------- forming ----------

	/** 27 Batteries in a 3x3x3 form an Industrial Battery: every block formed, one master, the charge summed, 27M room. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void formsAtThreeHigh(GameTestHelper helper) {
		BlockPos c = new BlockPos(2, 1, 2);
		batteries(helper, c, 3, 3, 3, 10_000);
		helper.runAtTickTime(5, () -> {
			for (int y = 0; y < 3; y++)
				for (int z = 0; z < 3; z++)
					for (int x = 0; x < 3; x++)
						if (!formed(helper, c.offset(x, y, z)))
							helper.fail("not formed at " + c.offset(x, y, z));
			BatteryBlockEntity m = be(helper, master(c));
			if (!m.isMaster() || m.height() != 3)
				helper.fail("the master isn't the middle of the front row, or the height isn't 3");
			IEnergyStorage e = energy(helper, master(c), Direction.UP);
			if (e.getEnergyStored() != 270_000 || e.getMaxEnergyStored() != 27 * CMConfig.SERVER.industrialBatteryCapacity())
				helper.fail("the tower reads " + e.getEnergyStored() + " / " + e.getMaxEnergyStored());
			// wires and machines beside it don't connect
			if (helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(c), Direction.WEST) != null)
				helper.fail("a side of the tower offers energy");
			helper.succeed();
		});
	}

	/** Ten high: the bottom nine form, the top layer stays plain. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void tenHighFormsNine(GameTestHelper helper) {
		BlockPos c = new BlockPos(2, 1, 2);
		batteries(helper, c, 3, 10, 3, 0);
		helper.runAtTickTime(5, () -> {
			if (be(helper, master(c)).height() != 9)
				helper.fail("the tower is " + be(helper, master(c)).height() + " high, not 9");
			if (!formed(helper, c.offset(1, 8, 1)) || formed(helper, c.offset(1, 9, 1)))
				helper.fail("the ninth layer isn't formed, or the tenth is");
			helper.succeed();
		});
	}

	/** Breaking a block of a 4-high tower's top layer: it re-forms 3 high, the rest plain, and only the broken block's share leaves. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void breakingTheTopLayerReformsShorter(GameTestHelper helper) {
		BlockPos c = new BlockPos(2, 1, 2);
		batteries(helper, c, 3, 4, 3, 40_000);
		helper.runAtTickTime(5, () -> {
			if (be(helper, master(c)).height() != 4)
				helper.fail("didn't form 4 high");
			helper.setBlock(c.offset(2, 3, 2), Blocks.AIR);
		});
		helper.runAtTickTime(8, () -> {
			if (be(helper, master(c)).height() != 3 || !formed(helper, c.offset(1, 2, 1)) || formed(helper, c.offset(1, 3, 1)))
				helper.fail("didn't re-form 3 high with the top layer plain");
			long left = total(helper, c, 3, 4, 3);
			if (left != 35 * 40_000L)
				helper.fail("left " + left + ", not 35 Batteries' worth");
			helper.succeed();
		});
	}

	/** Breaking the middle of a 9-high tower: the two runs left tie at 4, and the lower one re-forms. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void breakingTheMiddleReformsTheLowerRun(GameTestHelper helper) {
		BlockPos c = new BlockPos(2, 1, 2);
		batteries(helper, c, 3, 9, 3, 0);
		helper.runAtTickTime(5, () -> helper.setBlock(c.offset(1, 4, 1), Blocks.AIR));
		helper.runAtTickTime(8, () -> {
			if (be(helper, master(c)).height() != 4 || !formed(helper, c.offset(0, 3, 0)) || formed(helper, c.offset(0, 5, 0)))
				helper.fail("the lower four didn't re-form, or the upper four did");
			helper.succeed();
		});
	}

	/** A 3x6 footprint is two towers side by side, separate; a 4x4 forms once, at its lowest corner. */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void biggerFootprints(GameTestHelper helper) {
		BlockPos wide = new BlockPos(1, 1, 1), square = new BlockPos(12, 1, 1);
		batteries(helper, wide, 3, 3, 6, 0);
		batteries(helper, square, 4, 3, 4, 0);
		helper.runAtTickTime(5, () -> {
			if (!be(helper, master(wide)).isMaster() || !be(helper, master(wide.south(3))).isMaster())
				helper.fail("the 3x6 isn't two towers");
			if (!be(helper, master(square)).isMaster() || formed(helper, square.offset(3, 0, 3)) || formed(helper, square.offset(3, 0, 0)))
				helper.fail("the 4x4 didn't form once, at its lowest corner");
			helper.succeed();
		});
	}

	// ---------- on the network ----------

	/** A tower's span to a Line Post, a wire from the post's foot, a motor: the tower runs the motor. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void towerFeedsAPoleLine(GameTestHelper helper) {
		BlockPos c = new BlockPos(1, 1, 2), post = new BlockPos(9, 1, 3), w = post.east(), m = post.east(2);
		batteries(helper, c, 3, 3, 3, 100_000);
		post(helper, post);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), w, Direction.DOWN);
		LineGameTests.motor(helper, m);
		helper.runAtTickTime(5, () -> {
			LineGameTests.relink(helper, w);
			BatteryBlockEntity tower = be(helper, master(c));
			if (!(helper.getBlockEntity(post) instanceof LinePostBlockEntity p)) {
				helper.fail("no post");
				return;
			}
			Span s = new Span(0, post.subtract(master(c)), 0, LineTier.COPPER, null, false, 3);
			tower.addSpan(s);
			p.addSpan(s.reversed());
		});
		helper.runAtTickTime(120, () -> {
			if (LineGameTests.stored(helper, m) <= 0)
				helper.fail("the tower didn't run the motor through the pole");
			helper.succeed();
		});
	}

	/** One tower, a red cable to one post and a plain one to another, a motor on each: its one store runs both. */
	@GameTest(template = COURSE, timeoutTicks = 140)
	public static void towerServesEveryColour(GameTestHelper helper) {
		BlockPos c = new BlockPos(1, 1, 2), postA = new BlockPos(9, 1, 3), postB = new BlockPos(9, 1, 8);
		batteries(helper, c, 3, 3, 3, 100_000);
		post(helper, postA);
		post(helper, postB);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), postA.east(), Direction.DOWN);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), postB.east(), Direction.DOWN);
		LineGameTests.motor(helper, postA.east(2));
		LineGameTests.motor(helper, postB.east(2));
		helper.runAtTickTime(5, () -> {
			if (helper.getBlockEntity(postA.east()) instanceof com.comedymonkey.createaddons.content.line.PowerLineBlockEntity w)
				w.setDye(net.minecraft.world.item.DyeColor.RED);
			LineGameTests.relink(helper, postA.east(), postB.east());
			BatteryBlockEntity tower = be(helper, master(c));
			Span red = new Span(0, postA.subtract(master(c)), 0, LineTier.COPPER, net.minecraft.world.item.DyeColor.RED, false, 3);
			Span plain = new Span(1, postB.subtract(master(c)), 0, LineTier.COPPER, null, false, 3);
			tower.addSpan(red);
			tower.addSpan(plain);
			((LinePostBlockEntity) helper.getBlockEntity(postA)).addSpan(red.reversed());
			((LinePostBlockEntity) helper.getBlockEntity(postB)).addSpan(plain.reversed());
		});
		helper.runAtTickTime(120, () -> {
			if (LineGameTests.stored(helper, postA.east(2)) <= 0)
				helper.fail("the motor on the red line got nothing from the tower");
			if (LineGameTests.stored(helper, postB.east(2)) <= 0)
				helper.fail("the motor on the plain line got nothing from the tower");
			helper.succeed();
		});
	}

	/**
	 * A tower whose every block is away runs on paper: an empty motor put on its line meanwhile is powered from its
	 * record, and back, the tower takes on what the record gave out (no more, no less).
	 */
	@GameTest(template = COURSE, timeoutTicks = 240)
	public static void towerRunsOnPaper(GameTestHelper helper) {
		BlockPos c = new BlockPos(1, 1, 2), post = new BlockPos(9, 1, 3), w = post.east(), m = post.east(2);
		batteries(helper, c, 3, 3, 3, 100_000);
		post(helper, post);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), w, Direction.DOWN);
		LineGameTests.motor(helper, m);
		BlockPos key = helper.absolutePos(master(c));
		double[] before = new double[1];
		helper.runAtTickTime(5, () -> {
			LineGameTests.relink(helper, w);
			Span s = new Span(0, post.subtract(master(c)), 0, LineTier.COPPER, null, false, 3);
			be(helper, master(c)).addSpan(s);
			((LinePostBlockEntity) helper.getBlockEntity(post)).addSpan(s.reversed());
		});
		helper.runAtTickTime(80, () -> {
			before[0] = com.comedymonkey.createaddons.content.line.PowerGridTesting.storeRecord(helper.getLevel(), key);
			if (before[0] <= 0)
				helper.fail("the tower kept no record (" + before[0] + ")");
			if (com.comedymonkey.createaddons.content.line.PowerGridTesting.storeRecordAfterReload(helper.getLevel(), key) != before[0])
				helper.fail("the record didn't survive a save");
			for (int y = 0; y < 3; y++)
				for (int z = 0; z < 3; z++)
					for (int x = 0; x < 3; x++)
						com.comedymonkey.createaddons.content.line.PowerGridTesting.away(helper.getLevel(), helper.absolutePos(c.offset(x, y, z)), true, 200);
			// a fresh, empty motor: only the record can fill it
			helper.setBlock(m, Blocks.AIR);
			LineGameTests.motor(helper, m);
			LineGameTests.relink(helper, w);
		});
		double[] onPaper = new double[1];
		helper.runAtTickTime(160, () -> {
			onPaper[0] = com.comedymonkey.createaddons.content.line.PowerGridTesting.storeRecord(helper.getLevel(), key);
			if (LineGameTests.stored(helper, m) <= 0)
				helper.fail("the motor got nothing from the tower on paper");
			if (onPaper[0] >= before[0])
				helper.fail("the record didn't go down (" + before[0] + " to " + onPaper[0] + ")");
			for (int y = 0; y < 3; y++)
				for (int z = 0; z < 3; z++)
					for (int x = 0; x < 3; x++)
						com.comedymonkey.createaddons.content.line.PowerGridTesting.away(helper.getLevel(), helper.absolutePos(c.offset(x, y, z)), false, 0);
		});
		helper.runAtTickTime(163, () -> {
			long live = energy(helper, master(c), Direction.UP).getEnergyStored();
			// back: what it held before going away, less what the record gave out (and what it gave live since)
			if (live > onPaper[0] + 1 || live < onPaper[0] - 3 * 81_920)
				helper.fail("back, the tower holds " + live + ", not what its record came to (" + onPaper[0] + ")");
			helper.succeed();
		});
	}

	/**
	 * A tower's cables stay up when it re-forms: on the same master (its top layer broken), or on the one above (its
	 * bottom layer broken), the post's end moved over with them.
	 */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void spansFollowTheTower(GameTestHelper helper) {
		BlockPos c = new BlockPos(1, 1, 2), post = new BlockPos(9, 1, 3);
		batteries(helper, c, 3, 5, 3, 0);
		post(helper, post);
		helper.runAtTickTime(5, () -> {
			Span s = new Span(0, post.subtract(master(c)), 0, LineTier.COPPER, null, false, 3);
			be(helper, master(c)).addSpan(s);
			((LinePostBlockEntity) helper.getBlockEntity(post)).addSpan(s.reversed());
			// the top layer: it re-forms 4 high on the same master
			helper.setBlock(c.offset(0, 4, 0), Blocks.AIR);
		});
		helper.runAtTickTime(8, () -> {
			if (be(helper, master(c)).spans().size() != 1 || ((LinePostBlockEntity) helper.getBlockEntity(post)).spans().size() != 1)
				helper.fail("the span came down when the tower re-formed on the same master");
			// the bottom layer: what re-forms stands higher, on the master above
			helper.setBlock(c.offset(0, 0, 2), Blocks.AIR);
		});
		helper.runAtTickTime(11, () -> {
			BatteryBlockEntity up = be(helper, master(c).above());
			if (!up.isMaster())
				helper.fail("the upper run didn't re-form");
			LinePostBlockEntity p = (LinePostBlockEntity) helper.getBlockEntity(post);
			if (up.spans().size() != 1 || p.spans().size() != 1 || !p.spans().get(0).equals(up.spans().get(0).reversed()))
				helper.fail("the span didn't move to the new master (tower " + up.spans() + ", post " + p.spans() + ")");
			helper.succeed();
		});
	}

	/** Taken by a contraption (its block entity first, then the block): the rest of the tower is plain, nothing lost. */
	@GameTest(template = COURSE, timeoutTicks = 30)
	public static void contraptionTakesATowerBlock(GameTestHelper helper) {
		BlockPos c = new BlockPos(2, 1, 2), taken = c.offset(2, 1, 2);
		batteries(helper, c, 3, 3, 3, 10_000);
		helper.runAtTickTime(5, () -> {
			BlockPos abs = helper.absolutePos(taken);
			helper.getLevel().removeBlockEntity(abs);
			helper.getLevel().setBlock(abs, Blocks.AIR.defaultBlockState(), 2 | 16 | 64);
		});
		helper.runAtTickTime(8, () -> {
			for (int y = 0; y < 3; y++)
				for (int z = 0; z < 3; z++)
					for (int x = 0; x < 3; x++)
						if (formed(helper, c.offset(x, y, z)))
							helper.fail("still formed at " + c.offset(x, y, z));
			long left = total(helper, c, 3, 3, 3);
			if (left != 26 * 10_000L)
				helper.fail("left " + left + ", not 26 Batteries' worth");
			helper.succeed();
		});
	}

	/** Two towers stacked on one footprint: breaking the lower one leaves the upper one as it is. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void breakingOneStackedTowerLeavesTheOther(GameTestHelper helper) {
		BlockPos c = new BlockPos(2, 1, 2);
		batteries(helper, c, 3, 12, 3, 0);
		helper.runAtTickTime(5, () -> {
			if (be(helper, master(c).above(9)).height() != 3)
				helper.fail("12 high isn't 9 and 3");
			helper.setBlock(c.offset(1, 5, 1), Blocks.AIR);
		});
		helper.runAtTickTime(8, () -> {
			if (!be(helper, master(c).above(9)).isMaster() || be(helper, master(c).above(9)).height() != 3)
				helper.fail("the upper tower was taken apart");
			if (be(helper, master(c)).height() != 5 || formed(helper, c.offset(0, 6, 0)))
				helper.fail("the lower tower didn't re-form 5 high with the layers above the break plain");
			helper.succeed();
		});
	}

	/** Filling a layer never walls anyone in: with a cow standing in it (a player is the same), only the one Battery goes down. */
	@GameTest(template = COURSE, timeoutTicks = 30)
	public static void fillingALayerWallsNobodyIn(GameTestHelper helper) {
		BlockPos c = new BlockPos(2, 1, 2), top = c.offset(1, 2, 1), corner = c.offset(0, 3, 0);
		batteries(helper, c, 3, 3, 3, 0);
		ItemStack stack = new ItemStack(ModBlocks.BATTERY.get().asItem(), 9);
		helper.runAtTickTime(5, () -> {
			helper.spawn(net.minecraft.world.entity.EntityType.COW, corner);
			net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
			BlockPos abs = helper.absolutePos(top);
			net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs).add(0, 0.5, 0),
				Direction.UP, abs, false);
			((net.minecraft.world.item.BlockItem) stack.getItem()).place(new net.minecraft.world.item.context.BlockPlaceContext(player,
				net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit));
		});
		helper.runAtTickTime(8, () -> {
			if (!helper.getBlockState(corner).isAir())
				helper.fail("a Battery was put where the cow stands");
			if (stack.getCount() != 8)
				helper.fail(stack.getCount() + " left, not 8: the layer was filled round the cow");
			helper.succeed();
		});
	}

	/** A tower with one block away (its master running): it can't run live, so it runs on paper meanwhile. */
	@GameTest(template = COURSE, timeoutTicks = 240)
	public static void partlyAwayTowerRunsOnPaper(GameTestHelper helper) {
		BlockPos c = new BlockPos(1, 1, 2), post = new BlockPos(9, 1, 3), w = post.east(), m = post.east(2);
		batteries(helper, c, 3, 3, 3, 100_000);
		post(helper, post);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), w, Direction.DOWN);
		LineGameTests.motor(helper, m);
		BlockPos key = helper.absolutePos(master(c)), far = helper.absolutePos(c.offset(2, 2, 2));
		helper.runAtTickTime(5, () -> {
			LineGameTests.relink(helper, w);
			Span s = new Span(0, post.subtract(master(c)), 0, LineTier.COPPER, null, false, 3);
			be(helper, master(c)).addSpan(s);
			((LinePostBlockEntity) helper.getBlockEntity(post)).addSpan(s.reversed());
		});
		double[] before = new double[1];
		helper.runAtTickTime(80, () -> {
			before[0] = com.comedymonkey.createaddons.content.line.PowerGridTesting.storeRecord(helper.getLevel(), key);
			if (before[0] <= 0)
				helper.fail("the tower kept no record");
			com.comedymonkey.createaddons.content.line.PowerGridTesting.away(helper.getLevel(), far, true, 200);
			helper.setBlock(m, Blocks.AIR);
			LineGameTests.motor(helper, m);
			LineGameTests.relink(helper, w);
		});
		helper.runAtTickTime(160, () -> {
			if (LineGameTests.stored(helper, m) <= 0)
				helper.fail("the motor got nothing from the part-away tower");
			if (com.comedymonkey.createaddons.content.line.PowerGridTesting.storeRecord(helper.getLevel(), key) >= before[0])
				helper.fail("the record didn't go down");
			helper.succeed();
		});
	}

	/** A strike on a rod on top adds its charge, to a Battery or a tower, up to full. */
	@GameTest(template = COURSE, timeoutTicks = 30)
	public static void lightningCharges(GameTestHelper helper) {
		BlockPos one = new BlockPos(1, 1, 1), full = new BlockPos(5, 1, 1), c = new BlockPos(10, 1, 2);
		battery(helper, one);
		battery(helper, full);
		batteries(helper, c, 3, 3, 3, 0);
		helper.runAtTickTime(5, () -> {
			int strike = CMConfig.SERVER.lightningStrikeFe(), cap = CMConfig.SERVER.batteryCapacity();
			be(helper, full).setCharge(cap);
			be(helper, one).struck(helper.absolutePos(one));
			be(helper, full).struck(helper.absolutePos(full));
			be(helper, c.offset(1, 2, 1)).struck(helper.absolutePos(c.offset(1, 2, 1)));
			if (be(helper, one).share() != Math.min(cap, strike) || be(helper, full).share() != cap)
				helper.fail("a Battery holds " + be(helper, one).share() + ", a full one " + be(helper, full).share());
			if (energy(helper, master(c), Direction.UP).getEnergyStored() != strike)
				helper.fail("the tower holds " + energy(helper, master(c), Direction.UP).getEnergyStored());
			helper.succeed();
		});
	}
}
