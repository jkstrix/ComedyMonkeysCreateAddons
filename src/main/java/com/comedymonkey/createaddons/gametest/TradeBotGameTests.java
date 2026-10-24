package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.TradeBotBrain;
import com.comedymonkey.createaddons.content.tradebot.TradeBotData;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.TradeBotItem;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Trade Bot's test courses. Each one builds a small world in code (on an empty 24 x 8 x 24 template), places his
 * station, sends him round, and passes once he has reached every stop and docked again. Run them all headless with
 * {@code ./gradlew runGameTestServer}, or watch one in game with {@code /test run comedymonkeysaddons:<name>}.
 * <p>
 * The trading hall is the main course: a yard with his station, a slab step up to the hall, a door, three booths (a
 * workstation counter with a Create seat behind it) along an aisle with a dog-leg, a gate and stairs up to a gallery
 * with a fourth booth. Milestone 5 puts villagers on the seats.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TradeBotGameTests {
	private static final String COURSE = "trade_bot_course";
	private static final BlockState FLOOR = Blocks.STONE.defaultBlockState();
	private static final BlockState WALL = Blocks.STONE_BRICKS.defaultBlockState();

	private TradeBotGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(TradeBotGameTests.class);
	}

	// ---- the courses

	/** Out of the station, a straight run, and back in. */
	@GameTest(template = COURSE, timeoutTicks = 1200)
	public static void rollsOutAndDocks(GameTestHelper helper) {
		floor(helper, 0, 0, 0, 23, 23, FLOOR);
		TradeBotEntity bot = station(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
		run(helper, bot, List.of(new BlockPos(3, 1, 12)), h -> {});
	}

	/** A walled lane with a slab and a block to hop up onto, and off again on the way home. */
	@GameTest(template = COURSE, timeoutTicks = 1600)
	public static void hopsSlabsAndBlocks(GameTestHelper helper) {
		lane(helper, 3, 2, 16);
		helper.setBlock(new BlockPos(3, 1, 7), Blocks.STONE_SLAB.defaultBlockState());
		for (int z = 9; z <= 13; z++)
			helper.setBlock(new BlockPos(3, 1, z), FLOOR);
		TradeBotEntity bot = station(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
		run(helper, bot, List.of(new BlockPos(3, 2, 11)), h -> {});
	}

	/** Up a stair onto a raised floor, and back down it. */
	@GameTest(template = COURSE, timeoutTicks = 1600)
	public static void climbsStairs(GameTestHelper helper) {
		lane(helper, 3, 2, 16);
		helper.setBlock(new BlockPos(3, 1, 8), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
		for (int z = 9; z <= 13; z++)
			helper.setBlock(new BlockPos(3, 1, z), FLOOR);
		TradeBotEntity bot = station(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
		run(helper, bot, List.of(new BlockPos(3, 2, 11)), h -> {});
	}

	/** Through a closed door and a closed gate, both shut again behind him (D60), and an open door left open. */
	@GameTest(template = COURSE, timeoutTicks = 1600)
	public static void opensAndClosesDoorsAndGates(GameTestHelper helper) {
		lane(helper, 3, 2, 18);
		door(helper, new BlockPos(3, 1, 6), Direction.SOUTH, false);
		helper.setBlock(new BlockPos(3, 1, 9), Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH));
		door(helper, new BlockPos(3, 1, 12), Direction.SOUTH, true);
		TradeBotEntity bot = station(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
		run(helper, bot, List.of(new BlockPos(3, 1, 15)), h -> {
			assertDoor(h, new BlockPos(3, 1, 6), false, "the door he opened");
			if (h.getBlockState(new BlockPos(3, 1, 9)).getValue(FenceGateBlock.OPEN))
				h.fail("the gate he opened was left open");
			assertDoor(h, new BlockPos(3, 1, 12), true, "the door that was already open");
		});
	}

	/** An L-shaped corridor: a sharp corner each way. */
	@GameTest(template = COURSE, timeoutTicks = 1600)
	public static void turnsCorners(GameTestHelper helper) {
		solid(helper, 0, 1, 0, 23, 3, 23, WALL);
		floor(helper, 0, 0, 0, 23, 23, FLOOR);
		carve(helper, 3, 1, 2, 3, 2, 10);
		carve(helper, 3, 1, 10, 14, 2, 10);
		TradeBotEntity bot = station(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
		run(helper, bot, List.of(new BlockPos(14, 1, 10)), h -> {});
	}

	/** The main course: round a trading hall's booths and gallery, and home. */
	@GameTest(template = COURSE, timeoutTicks = 4800)
	public static void tradingHallTour(GameTestHelper helper) {
		Hall hall = tradingHall(helper);
		TradeBotEntity bot = station(helper, hall.station, Direction.SOUTH);
		run(helper, bot, hall.spots, h -> {
			assertDoor(h, hall.door, false, "the hall door");
			if (h.getBlockState(hall.gate).getValue(FenceGateBlock.OPEN))
				h.fail("the gallery gate was left open");
		});
	}

	// ---- the trading hall

	record Hall(BlockPos station, List<BlockPos> spots, BlockPos door, BlockPos gate) {}

	/**
	 * <pre>
	 * z 0-5   yard (walk level 1), station at (3, 1, 2) facing the hall
	 * z 6     slab step (level 1.5)
	 * z 7     hall floor (level 2), in front of the hall wall
	 * z 8     hall wall with an oak door at x 3
	 * z 9-13  aisle; a short wall at x 13, z 11-13 makes a dog-leg between booths B and C
	 * z 14    counters (workstations) at x 6, 11, 15; z 15 Create seats behind them; z 16 back wall
	 * x 17    fence across the aisle with a gate at z 11; x 19 railing with oak stairs at z 11
	 * x 20-21 gallery (level 3) with booth D: counter (21, 3, 14), seat (21, 3, 15)
	 * </pre>
	 * The trading spots are the aisle blocks in front of each counter: one block from the villager, across his
	 * workstation, as Trade Bot will trade (D7).
	 */
	static Hall tradingHall(GameTestHelper helper) {
		floor(helper, 0, 0, 0, 23, 23, FLOOR);
		for (int x = 1; x <= 21; x++)
			helper.setBlock(new BlockPos(x, 1, 6), Blocks.STONE_SLAB.defaultBlockState());
		floor(helper, 1, 1, 7, 21, 16, FLOOR);
		solid(helper, 0, 1, 6, 0, 4, 16, WALL);
		solid(helper, 22, 1, 6, 22, 4, 16, WALL);
		solid(helper, 1, 2, 8, 21, 4, 8, WALL);
		solid(helper, 1, 2, 16, 21, 4, 16, WALL);
		carve(helper, 3, 2, 8, 3, 3, 8);
		door(helper, new BlockPos(3, 2, 8), Direction.SOUTH, false);
		solid(helper, 13, 2, 11, 13, 3, 13, WALL);
		Map<Integer, Block> counters = Map.of(6, Blocks.COMPOSTER, 11, Blocks.LECTERN, 15, Blocks.SMITHING_TABLE);
		counters.forEach((x, block) -> booth(helper, new BlockPos(x, 2, 14), block));
		for (int z = 9; z <= 13; z++) {
			if (z != 11) {
				helper.setBlock(new BlockPos(17, 2, z), Blocks.OAK_FENCE.defaultBlockState());
				helper.setBlock(new BlockPos(19, 2, z), Blocks.OAK_FENCE.defaultBlockState());
			}
		}
		helper.setBlock(new BlockPos(17, 2, 11), Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST));
		helper.setBlock(new BlockPos(19, 2, 11), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
		floor(helper, 20, 2, 9, 21, 15, FLOOR);
		booth(helper, new BlockPos(21, 3, 14), Blocks.CARTOGRAPHY_TABLE);
		List<BlockPos> spots = List.of(new BlockPos(6, 2, 13), new BlockPos(11, 2, 13), new BlockPos(15, 2, 13), new BlockPos(21, 3, 13));
		return new Hall(new BlockPos(3, 1, 2), spots, new BlockPos(3, 2, 8), new BlockPos(17, 2, 11));
	}

	/** A villager's booth: a workstation counter, and a Create seat behind it for the villager. */
	private static void booth(GameTestHelper helper, BlockPos counter, Block workstation) {
		helper.setBlock(counter, workstation.defaultBlockState());
		Block seat = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "red_seat"));
		helper.setBlock(counter.south(), seat.defaultBlockState());
	}

	// ---- running a course

	/** Sends him round {@code stops}; passes once he has stood on each, in order, and docked again. */
	static void run(GameTestHelper helper, TradeBotEntity bot, List<BlockPos> stops, Consumer<GameTestHelper> finalChecks) {
		TradeBotMotion.debug = true;
		List<BlockPos> absolute = new ArrayList<>();
		for (BlockPos stop : stops)
			absolute.add(helper.absolutePos(stop));
		helper.runAfterDelay(10, () -> {
			if (!bot.brain.tour(absolute))
				helper.fail("he couldn't leave his station");
		});
		helper.onEachTick(() -> {
			if (bot.isRemoved())
				helper.fail("he was removed");
			if (bot.brain.task() == TradeBotBrain.Task.STRANDED)
				helper.fail("he lost his station");
			if (bot.motion.failed())
				helper.fail(String.format(Locale.ROOT, "he gave up a route at %s, %d of %d stops reached",
					helper.relativePos(bot.blockPosition()).toShortString(), bot.brain.visited().size(), stops.size()));
		});
		helper.succeedWhen(() -> {
			if (bot.brain.visited().size() < absolute.size())
				helper.fail(bot.brain.visited().size() + " of " + absolute.size() + " stops reached");
			if (!bot.brain.visited().equals(absolute))
				helper.fail("stops reached out of order: " + bot.brain.visited());
			if (!bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME)
				helper.fail("not docked yet (" + bot.brain.task() + ")");
			finalChecks.accept(helper);
		});
	}

	/** A station facing {@code facing} with a fully charged bot docked in it. */
	static TradeBotEntity station(GameTestHelper helper, BlockPos pos, Direction facing) {
		helper.setBlock(pos, ModBlocks.TRADE_BOT.get().defaultBlockState().setValue(TradeBotStationBlock.FACING, facing));
		TradeBotData charged = new TradeBotData(CMConfig.SERVER.tradeBotFullCharge(), 0, 0, Map.of(), ItemContainerContents.EMPTY, List.of(),
			java.util.Optional.empty());
		TradeBotEntity bot = TradeBotItem.populate(helper.getLevel(), helper.absolutePos(pos), UUID.randomUUID(), charged, null);
		if (bot == null)
			helper.fail("no bot in the station");
		return bot;
	}

	// ---- building

	/** A one-block-wide lane along z at {@code x}, walled both sides, from z0 to z1. */
	private static void lane(GameTestHelper helper, int x, int z0, int z1) {
		floor(helper, 0, 0, 0, 23, 23, FLOOR);
		solid(helper, x - 1, 1, z0, x - 1, 3, z1, WALL);
		solid(helper, x + 1, 1, z0, x + 1, 3, z1, WALL);
	}

	private static void floor(GameTestHelper helper, int x0, int y, int z0, int x1, int z1, BlockState state) {
		solid(helper, x0, y, z0, x1, y, z1, state);
	}

	private static void solid(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		for (int x = x0; x <= x1; x++)
			for (int y = y0; y <= y1; y++)
				for (int z = z0; z <= z1; z++)
					helper.setBlock(new BlockPos(x, y, z), state);
	}

	private static void carve(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1) {
		solid(helper, x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
	}

	private static void door(GameTestHelper helper, BlockPos lower, Direction facing, boolean open) {
		BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, open);
		helper.setBlock(lower, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(lower.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	private static void assertDoor(GameTestHelper helper, BlockPos lower, boolean open, String what) {
		if (helper.getBlockState(lower).getValue(DoorBlock.OPEN) != open)
			helper.fail(what + " is " + (open ? "shut" : "still open"));
	}
}
