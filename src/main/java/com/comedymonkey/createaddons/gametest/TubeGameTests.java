package com.comedymonkey.createaddons.gametest;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tube.ClockworkInletBlock;
import com.comedymonkey.createaddons.content.tube.ClockworkReceiverBlock;
import com.comedymonkey.createaddons.content.tube.ClockworkTubeBlock;
import com.comedymonkey.createaddons.content.tube.ClockworkTubeBlockEntity;
import com.comedymonkey.createaddons.content.tube.TubeJoint;
import com.comedymonkey.createaddons.content.tube.TubeLoad;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Clockwork Tubes in a real world (design/specs/clockwork-tubes.md). The standard network: a Creative Motor (facing up)
 * turns a cogwheel meshed with a Receiver (output up, into a chest); its tube runs east two blocks to an Inlet hanging
 * mouth-down over a source chest.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TubeGameTests {
	private static final String COURSE = "trade_bot_course";

	private static final BlockPos MOTOR = new BlockPos(3, 3, 4), COG = new BlockPos(3, 4, 4), RECEIVER = new BlockPos(4, 4, 4),
		OUT = new BlockPos(4, 5, 4), INLET = new BlockPos(6, 2, 4), SOURCE = new BlockPos(6, 1, 4);
	private static final BlockPos[] TUBES = {new BlockPos(4, 3, 4), new BlockPos(5, 3, 4), new BlockPos(6, 3, 4)};

	private TubeGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(TubeGameTests.class);
	}

	// ---------- building ----------

	private static Block create(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", id));
	}

	/** Places tubes, then lets each work out its joints (twice, so neighbours placed after it are seen). */
	static void tubes(GameTestHelper helper, BlockPos... at) {
		for (BlockPos p : at)
			helper.setBlock(p, ModBlocks.CLOCKWORK_TUBE.get().defaultBlockState());
		for (int pass = 0; pass < 2; pass++)
			for (BlockPos p : at) {
				BlockPos abs = helper.absolutePos(p);
				helper.getLevel().setBlock(abs, Block.updateFromNeighbourShapes(helper.getLevel().getBlockState(abs), helper.getLevel(), abs), 3);
			}
	}

	static void inlet(GameTestHelper helper, BlockPos at, Direction mouth, boolean brass) {
		helper.setBlock(at, (brass ? ModBlocks.BRASS_CLOCKWORK_INLET : ModBlocks.CLOCKWORK_INLET).get().defaultBlockState()
			.setValue(ClockworkInletBlock.FACING, mouth));
	}

	static void receiver(GameTestHelper helper, BlockPos at, Direction out) {
		helper.setBlock(at, ModBlocks.CLOCKWORK_RECEIVER.get().defaultBlockState().setValue(ClockworkReceiverBlock.FACING, out));
	}

	/** A Creative Motor facing up into a cogwheel, at {@code rpm} from the second tick. */
	static void drive(GameTestHelper helper, int rpm) {
		helper.setBlock(MOTOR, create("creative_motor").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
		helper.setBlock(COG, create("cogwheel").defaultBlockState());
		helper.runAtTickTime(2, () -> {
			if (helper.getBlockEntity(MOTOR) instanceof CreativeMotorBlockEntity motor)
				motor.generatedSpeed.setValue(rpm);
			else
				helper.fail("no creative motor");
		});
	}

	/** The standard network, without its power, its output chest or its source. */
	static void network(GameTestHelper helper, boolean brass) {
		receiver(helper, RECEIVER, Direction.UP);
		inlet(helper, INLET, Direction.DOWN, brass);
		tubes(helper, TUBES);
	}

	static Container chest(GameTestHelper helper, BlockPos at, ItemStack... contents) {
		helper.setBlock(at, Blocks.CHEST);
		Container chest = (Container) helper.getBlockEntity(at);
		for (int i = 0; i < contents.length; i++)
			chest.setItem(i, contents[i]);
		return chest;
	}

	static int count(Container chest, Item item) {
		int n = 0;
		for (int i = 0; i < chest.getContainerSize(); i++)
			if (chest.getItem(i).is(item))
				n += chest.getItem(i).getCount();
		return n;
	}

	static int lying(GameTestHelper helper, Item item, AABB area) {
		int n = 0;
		for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(item)))
			n += e.getItem().getCount();
		return n;
	}

	static ClockworkTubeBlockEntity tube(GameTestHelper helper, BlockPos at) {
		if (!(helper.getBlockEntity(at) instanceof ClockworkTubeBlockEntity t))
			throw new net.minecraft.gametest.framework.GameTestAssertException("no tube at " + at);
		return t;
	}

	static TubeJoint joint(GameTestHelper helper, BlockPos at, Direction side) {
		BlockState s = helper.getBlockState(at);
		return s.getValue(ClockworkTubeBlock.JOINTS.get(side));
	}

	// ---------- tests ----------

	/** Items go from the source chest, up the Inlet, along the tubes, through the Receiver and into its chest. */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void itemsTravelFromTheInletToTheChest(GameTestHelper helper) {
		network(helper, false);
		Container out = chest(helper, OUT), source = chest(helper, SOURCE, new ItemStack(Items.COBBLESTONE, 10));
		drive(helper, 256);
		helper.succeedWhen(() -> {
			helper.assertTrue(count(out, Items.COBBLESTONE) == 10, "expected 10 cobblestone in the chest, got " + count(out, Items.COBBLESTONE));
			helper.assertTrue(count(source, Items.COBBLESTONE) == 0, "the source should be empty");
		});
	}

	/** A short run: an Inlet right against the Receiver's input, no tubes, still delivers (D39). */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void anInletRightAgainstTheReceiverDelivers(GameTestHelper helper) {
		receiver(helper, RECEIVER, Direction.UP);
		inlet(helper, RECEIVER.below(), Direction.DOWN, false);
		Container out = chest(helper, OUT), source = chest(helper, RECEIVER.below(2), new ItemStack(Items.COBBLESTONE, 6));
		drive(helper, 256);
		helper.succeedWhen(() -> {
			helper.assertTrue(count(out, Items.COBBLESTONE) == 6, "expected 6 cobblestone in the chest, got " + count(out, Items.COBBLESTONE));
			helper.assertTrue(count(source, Items.COBBLESTONE) == 0, "the source should be empty");
		});
	}

	/** The joints: the Receiver's tube joins it, the tubes join each other, the end tube joins the Inlet's back. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void tubesJoinTheReceiverEachOtherAndTheInlet(GameTestHelper helper) {
		network(helper, false);
		helper.runAtTickTime(2, () -> {
			helper.assertTrue(joint(helper, TUBES[0], Direction.UP) == TubeJoint.OPEN, "joined to the Receiver");
			helper.assertTrue(joint(helper, TUBES[0], Direction.EAST) == TubeJoint.OPEN, "joined to the next tube");
			helper.assertTrue(joint(helper, TUBES[2], Direction.DOWN) == TubeJoint.OPEN, "joined to the Inlet's back");
			helper.assertTrue(joint(helper, TUBES[2], Direction.UP) == TubeJoint.NONE, "nothing above");
			helper.succeed();
		});
	}

	/** A tube broken with a load in it drops exactly that load, once (D20). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void aBrokenTubeDropsItsLoadOnce(GameTestHelper helper) {
		network(helper, false);
		helper.runAtTickTime(2, () -> {
			tube(helper, TUBES[1]).setLoad(new TubeLoad(new ItemStack(Items.DIAMOND, 3), Direction.EAST, Direction.WEST, 0.5f, 0, 0));
			helper.destroyBlock(TUBES[1]);
		});
		helper.runAtTickTime(4, () -> {
			AABB around = new AABB(helper.absolutePos(TUBES[1])).inflate(3);
			int dropped = lying(helper, Items.DIAMOND, around);
			helper.assertTrue(dropped == 3, "expected the 3 diamonds dropped once, got " + dropped);
			helper.succeed();
		});
	}

	/** Stopped (no power), a load stays where it is: nothing lost, nothing moves. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aStoppedNetworkKeepsItsLoads(GameTestHelper helper) {
		network(helper, false);
		Container out = chest(helper, OUT);
		helper.runAtTickTime(2, () -> tube(helper, TUBES[1]).setLoad(new TubeLoad(new ItemStack(Items.DIAMOND, 2), Direction.EAST, Direction.WEST, 0.5f,
			helper.getLevel().getGameTime(), 0)));
		helper.runAtTickTime(50, () -> {
			TubeLoad load = tube(helper, TUBES[1]).getLoad();
			helper.assertTrue(load != null && load.stack.is(Items.DIAMOND) && load.stack.getCount() == 2, "the load should still be there");
			helper.assertTrue(count(out, Items.DIAMOND) == 0, "nothing delivered without power");
			helper.succeed();
		});
	}

	/**
	 * Two Receivers' tubes meet: exactly one joint on the way from one to the other stays capped (a tube's, or a
	 * Receiver's, whichever the order of joining reaches last), and every other opens (D16).
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void twoReceiversNeverJoin(GameTestHelper helper) {
		receiver(helper, new BlockPos(2, 2, 2), Direction.UP);
		receiver(helper, new BlockPos(7, 2, 2), Direction.UP);
		BlockPos[] line = new BlockPos[6];
		for (int i = 0; i < line.length; i++)
			line[i] = new BlockPos(2 + i, 1, 2);
		tubes(helper, line);
		helper.runAtTickTime(2, () -> {
			int capped = 0;
			for (BlockPos end : new BlockPos[] {line[0], line[line.length - 1]})
				if (joint(helper, end, Direction.UP) == TubeJoint.CAPPED)
					capped++;
			for (int i = 0; i + 1 < line.length; i++) {
				TubeJoint a = joint(helper, line[i], Direction.EAST), b = joint(helper, line[i + 1], Direction.WEST);
				helper.assertTrue(a == b, "both sides of a joint must agree at " + line[i]);
				if (a == TubeJoint.CAPPED)
					capped++;
			}
			helper.assertTrue(capped == 1, "expected exactly one capped joint between the two Receivers, got " + capped);
			helper.succeed();
		});
	}

	/** The second Receiver is broken: the joint capped against it opens again by itself (within a few seconds). */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void aCapOpensOnceItsReasonHasGone(GameTestHelper helper) {
		BlockPos second = new BlockPos(7, 2, 2);
		receiver(helper, new BlockPos(2, 2, 2), Direction.UP);
		receiver(helper, second, Direction.UP);
		BlockPos[] line = new BlockPos[6];
		for (int i = 0; i < line.length; i++)
			line[i] = new BlockPos(2 + i, 1, 2);
		tubes(helper, line);
		helper.runAtTickTime(5, () -> helper.destroyBlock(second));
		helper.succeedWhen(() -> {
			for (BlockPos p : line)
				for (Direction side : Direction.values())
					helper.assertTrue(joint(helper, p, side) != TubeJoint.CAPPED, "still capped at " + p + " " + side);
			for (int i = 0; i + 1 < line.length; i++)
				helper.assertTrue(joint(helper, line[i], Direction.EAST) == TubeJoint.OPEN, "not joined at " + line[i]);
		});
	}

	/**
	 * Two branches backed up behind a full chest: once it empties, both get turns at the join, the load that has waited
	 * longest first (D26), never one branch only.
	 */
	@GameTest(template = COURSE, timeoutTicks = 400)
	public static void branchesTakeTurnsAtAJoin(GameTestHelper helper) {
		network(helper, false);
		BlockPos[] south = {new BlockPos(4, 3, 5), new BlockPos(4, 3, 6)};
		inlet(helper, new BlockPos(4, 2, 6), Direction.DOWN, false);
		tubes(helper, TUBES[0], south[0], south[1]);
		ItemStack[] full = new ItemStack[27];
		for (int i = 0; i < full.length; i++)
			full[i] = new ItemStack(Items.DIRT, 64);
		Container out = chest(helper, OUT, full);
		chest(helper, SOURCE, new ItemStack(Items.COBBLESTONE, 30));
		chest(helper, new BlockPos(4, 1, 6), new ItemStack(Items.GRAVEL, 30));
		drive(helper, 256);
		// everything backs up, then the chest empties
		helper.runAtTickTime(150, out::clearContent);
		helper.succeedWhen(() -> {
			int cobble = count(out, Items.COBBLESTONE), gravel = count(out, Items.GRAVEL);
			helper.assertTrue(cobble + gravel >= 12, "waiting for twelve");
			helper.assertTrue(cobble >= 4 && gravel >= 4, "both branches should have had turns: " + cobble + " cobblestone, " + gravel + " gravel");
		});
	}

	/** The Receiver's chest is full: loads queue back, one a tube, and the Inlet stops taking (D11, D26). */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void aFullChestBacksTheTubesUp(GameTestHelper helper) {
		network(helper, false);
		ItemStack[] full = new ItemStack[27];
		for (int i = 0; i < full.length; i++)
			full[i] = new ItemStack(Items.DIRT, 64);
		Container out = chest(helper, OUT, full), source = chest(helper, SOURCE, new ItemStack(Items.COBBLESTONE, 20));
		drive(helper, 256);
		helper.runAtTickTime(180, () -> {
			helper.assertTrue(count(out, Items.COBBLESTONE) == 0, "the full chest took nothing");
			// one load a tube, and one in the Receiver's throat
			int left = 20 - TUBES.length - 1;
			helper.assertTrue(count(source, Items.COBBLESTONE) == left,
				"expected one load a tube and one in the throat taken (" + left + " left), got " + count(source, Items.COBBLESTONE));
			for (BlockPos t : TUBES)
				helper.assertTrue(tube(helper, t).getLoad() != null, "every tube should hold a waiting load");
			helper.succeed();
		});
	}

	/** A Brass Inlet's filter: only iron comes, a stack at a time (D21, D22). */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void theBrassInletsFilterTakesOnlyItsItem(GameTestHelper helper) {
		network(helper, true);
		Container out = chest(helper, OUT), source = chest(helper, SOURCE, new ItemStack(Items.DIRT, 64), new ItemStack(Items.IRON_INGOT, 64));
		helper.runAtTickTime(1, () -> {
			FilteringBehaviour filter = BlockEntityBehaviour.get(helper.getBlockEntity(INLET), FilteringBehaviour.TYPE);
			if (filter == null)
				helper.fail("the Brass Inlet has no filter");
			else
				filter.setFilter(new ItemStack(Items.IRON_INGOT));
		});
		drive(helper, 256);
		helper.succeedWhen(() -> {
			helper.assertTrue(count(out, Items.IRON_INGOT) == 64, "expected all 64 iron, got " + count(out, Items.IRON_INGOT));
			helper.assertTrue(count(out, Items.DIRT) == 0, "the filter let dirt through");
			helper.assertTrue(count(source, Items.DIRT) == 64, "the dirt should stay");
		});
	}

	/** A plain Inlet takes one item a load, so a stack in the tubes never shows up from it. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aPlainInletTakesOneItemALoad(GameTestHelper helper) {
		network(helper, false);
		ItemStack[] full = new ItemStack[27];
		for (int i = 0; i < full.length; i++)
			full[i] = new ItemStack(Items.DIRT, 64);
		chest(helper, OUT, full);
		chest(helper, SOURCE, new ItemStack(Items.COBBLESTONE, 64));
		drive(helper, 256);
		helper.runAtTickTime(50, () -> {
			for (BlockPos t : TUBES) {
				TubeLoad load = tube(helper, t).getLoad();
				helper.assertTrue(load == null || load.stack.getCount() == 1, "a plain Inlet's load is one item");
			}
			helper.succeed();
		});
	}

	/** Redstone on an Inlet switches it off (D19). */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void redstoneSwitchesAnInletOff(GameTestHelper helper) {
		network(helper, false);
		Container out = chest(helper, OUT), source = chest(helper, SOURCE, new ItemStack(Items.COBBLESTONE, 10));
		helper.setBlock(INLET.east(), Blocks.REDSTONE_BLOCK);
		drive(helper, 256);
		helper.runAtTickTime(100, () -> {
			helper.assertBlockProperty(INLET, ClockworkInletBlock.POWERED, true);
			helper.assertTrue(count(source, Items.COBBLESTONE) == 10, "a powered Inlet takes nothing");
			helper.assertTrue(count(out, Items.COBBLESTONE) == 0, "nothing delivered");
			helper.succeed();
		});
	}

	/** An Inlet over the floor sweeps items lying in the block space under its mouth (D15). */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void anInletOverTheFloorTakesWhatsLyingThere(GameTestHelper helper) {
		network(helper, false);
		Container out = chest(helper, OUT);
		// the course is empty: a floor for the bones to lie on
		helper.setBlock(SOURCE, Blocks.AIR);
		helper.setBlock(SOURCE.below(), Blocks.STONE);
		helper.runAtTickTime(2, () -> {
			BlockPos at = helper.absolutePos(SOURCE);
			ItemEntity bones = new ItemEntity(helper.getLevel(), at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, new ItemStack(Items.BONE, 5));
			bones.setDeltaMovement(0, 0, 0);
			helper.getLevel().addFreshEntity(bones);
		});
		drive(helper, 256);
		helper.succeedWhen(() -> helper.assertTrue(count(out, Items.BONE) == 5, "expected the 5 bones, got " + count(out, Items.BONE)));
	}

	/** Two Inlets take turns (D26): both have sent some when ten have arrived. */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void inletsTakeTurns(GameTestHelper helper) {
		network(helper, false);
		BlockPos branch = new BlockPos(4, 3, 5), second = new BlockPos(4, 2, 5), secondSource = new BlockPos(4, 1, 5);
		inlet(helper, second, Direction.DOWN, false);
		tubes(helper, TUBES[0], branch);
		Container out = chest(helper, OUT);
		chest(helper, SOURCE, new ItemStack(Items.COBBLESTONE, 30));
		chest(helper, secondSource, new ItemStack(Items.DIRT, 30));
		drive(helper, 256);
		helper.succeedWhen(() -> {
			int cobble = count(out, Items.COBBLESTONE), dirt = count(out, Items.DIRT);
			helper.assertTrue(cobble + dirt >= 10, "waiting for ten");
			helper.assertTrue(cobble >= 3 && dirt >= 3, "both Inlets should have had turns: " + cobble + " cobblestone, " + dirt + " dirt");
		});
	}

	/** Nothing in front of the Receiver: its items drop out (D11). */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void itemsDropOutWithNothingInFront(GameTestHelper helper) {
		network(helper, false);
		chest(helper, SOURCE, new ItemStack(Items.COBBLESTONE, 5));
		drive(helper, 256);
		helper.succeedWhen(() -> {
			AABB above = new AABB(helper.absolutePos(OUT)).inflate(2, 3, 2);
			int dropped = lying(helper, Items.COBBLESTONE, above);
			helper.assertTrue(dropped == 5, "expected the 5 cobblestone dropped out, got " + dropped);
		});
	}

	/** A tube that would make a Receiver's network longer than the most stays capped (D30). */
	@GameTest(template = COURSE, timeoutTicks = 20, batch = "clockwork_tube_length")
	public static void aNetworkPastItsLengthCapsOff(GameTestHelper helper) {
		int max = CMConfig.Server.get(CMConfig.SERVER.clockworkTubeMaxLength);
		CMConfig.SERVER.clockworkTubeMaxLength.set(2);
		try {
			receiver(helper, RECEIVER, Direction.UP);
			// laid one at a time from the Receiver, as a player builds
			for (BlockPos t : TUBES)
				tubes(helper, t);
			List<TubeJoint> joints = List.of(joint(helper, TUBES[0], Direction.EAST), joint(helper, TUBES[1], Direction.EAST));
			helper.assertTrue(joints.get(0) == TubeJoint.OPEN, "the first two tubes join");
			helper.assertTrue(joints.get(1) == TubeJoint.CAPPED, "the third tube is one too many and caps off");
			helper.succeed();
		} finally {
			CMConfig.SERVER.clockworkTubeMaxLength.set(max);
		}
	}
}
