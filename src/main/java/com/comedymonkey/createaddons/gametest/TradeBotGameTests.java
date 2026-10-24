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
import com.comedymonkey.createaddons.content.tradebot.TradeBotLinks;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
 * with a fourth booth; the trading courses put villagers on its seats.
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

	/**
	 * A raised block with the target diagonally across its corner, on open ground, so the route steps up and down
	 * diagonally: every hop must still be square-on, clearing the edge (user: "when he jumps diagonally down or up he
	 * needs to jump further to clear the edge").
	 */
	@GameTest(template = COURSE, timeoutTicks = 1600)
	public static void hopsSquareOnDiagonalSteps(GameTestHelper helper) {
		floor(helper, 0, 0, 0, 23, 23, FLOOR);
		floor(helper, 7, 1, 8, 16, 16, FLOOR);
		TradeBotEntity bot = station(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
		run(helper, bot, List.of(new BlockPos(10, 2, 11)), h -> {
			if (bot.motion.hops().isEmpty())
				h.fail("he never hopped");
			for (TradeBotMotion.Hop hop : bot.motion.hops())
				if (hop.from().getX() != hop.to().getX() && hop.from().getZ() != hop.to().getZ())
					h.fail("a diagonal hop from " + h.relativePos(hop.from()).toShortString() + " to " + h.relativePos(hop.to()).toShortString());
		});
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

	/**
	 * A whole job without trading yet: he takes from four kinds of input (a chest on the floor, a barrel, a chest up on
	 * a block, a Create depot), delivers only his earnings to the output chest (emeralds and Nuggets of Experience, as if
	 * he'd traded), and on the way home returns every unsold item to the input it came from (D48, D57, D62).
	 */
	@GameTest(template = COURSE, timeoutTicks = 6000)
	public static void deliversEarningsAndReturnsTheRest(GameTestHelper helper) {
		floor(helper, 0, 0, 0, 23, 23, FLOOR);
		TradeBotEntity bot = station(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
		TradeBotStationBlockEntity station = (TradeBotStationBlockEntity) helper.getBlockEntity(new BlockPos(3, 1, 2));
		BlockPos chest = new BlockPos(9, 1, 7), barrel = new BlockPos(13, 1, 7), high = new BlockPos(9, 2, 12), depot = new BlockPos(13, 1, 12);
		BlockPos output = new BlockPos(3, 1, 13);
		helper.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.WEST));
		helper.setBlock(barrel, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
		helper.setBlock(high.below(), FLOOR);
		helper.setBlock(high, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.WEST));
		helper.setBlock(depot, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "depot")).defaultBlockState());
		helper.setBlock(output, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH));
		fill(helper, chest, new ItemStack(Items.WHEAT, 32), new ItemStack(Items.IRON_INGOT, 10));
		fill(helper, barrel, new ItemStack(Items.BREAD, 16));
		fill(helper, high, new ItemStack(Items.EMERALD, 5));
		fill(helper, depot, new ItemStack(Items.APPLE, 1));
		for (BlockPos input : List.of(chest, barrel, high, depot))
			station.setRole(helper.absolutePos(input), TradeBotLinks.Role.TAKES);
		station.setRole(helper.absolutePos(output), TradeBotLinks.Role.DELIVERS);
		bot.addEarnings(70, 10); // as if he'd traded: 70 emeralds, and 10 XP (3 nuggets, 1 XP left over)
		TradeBotMotion.debug = true;
		helper.runAfterDelay(10, () -> {
			if (!bot.brain.fetchAndDeliver(stack -> true))
				helper.fail("he wouldn't start the job");
		});
		helper.succeedWhen(() -> {
			if (!bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME)
				helper.fail("not docked yet (" + bot.brain.task() + ", taken " + bot.brain.taken() + ", delivered " + bot.brain.delivered() + ")");
			if (count(helper, output, Items.EMERALD) != 70 || count(helper, output, nugget()) != 3 || count(helper, output) != 73)
				helper.fail("the output holds " + count(helper, output) + " items, not just 70 emeralds and 3 nuggets");
			if (bot.getXpBank() != 1)
				helper.fail(bot.getXpBank() + " XP banked, not 1");
			int[] expected = { 42, 16, 5, 1 };
			List<BlockPos> inputs = List.of(chest, barrel, high, depot);
			for (int i = 0; i < inputs.size(); i++)
				if (count(helper, inputs.get(i)) != expected[i])
					helper.fail(helper.getBlockState(inputs.get(i)).getBlock().getName().getString() + " has " + count(helper, inputs.get(i))
						+ " items back, not " + expected[i]);
			for (int slot = 0; slot < bot.getLoad().getSlots(); slot++)
				if (!bot.getLoad().getStackInSlot(slot).isEmpty())
					helper.fail("he still carries " + bot.getLoad().getStackInSlot(slot));
			if (helper.getBlockState(barrel).getValue(BarrelBlock.OPEN))
				helper.fail("the barrel was left open");
		});
	}

	/**
	 * Trading for real (D7, D17, D45, D57, D63): seated villagers in all four of the hall's booths, each buying one of
	 * the goods in his input chest. He goes by himself (D47), takes only what they'll buy (D22), trades with each in turn
	 * across its workstation, every trade he can pay for in one clip, delivers the emeralds and nuggets, brings
	 * back what's left over (D62), and docks. The cobblestone nobody buys never leaves the chest. Then, with not enough
	 * left for another trade, he stays home. (The goods set the number of trades: villagers at their workstations
	 * restock, as they do for players.)
	 */
	@GameTest(template = COURSE, timeoutTicks = 8000)
	public static void tradesRoundTheHall(GameTestHelper helper) {
		Hall hall = tradingHall(helper);
		helper.getLevel().setDayTime(1000);
		TradeBotEntity bot = station(helper, hall.station, Direction.SOUTH);
		BlockPos input = new BlockPos(7, 1, 1), output = new BlockPos(10, 1, 1);
		links(helper, hall.station, input, output);
		fill(helper, input, new ItemStack(Items.WHEAT, 40), new ItemStack(Items.PAPER, 30), new ItemStack(Items.COAL, 20),
			new ItemStack(Items.GLASS_PANE, 16), new ItemStack(Items.COBBLESTONE, 10));
		List<Villager> villagers = List.of(
			seated(helper, hall.seats.get(0), VillagerProfession.FARMER, offer(Items.WHEAT, 20, 16), offer(Items.CARROT, 22, 16)),
			seated(helper, hall.seats.get(1), VillagerProfession.LIBRARIAN, offer(Items.PAPER, 24, 16)),
			seated(helper, hall.seats.get(2), VillagerProfession.TOOLSMITH, offer(Items.COAL, 15, 16)),
			seated(helper, hall.seats.get(3), VillagerProfession.CARTOGRAPHER, offer(Items.GLASS_PANE, 11, 16)));
		int[] trades = { 2, 1, 1, 1 };
		TradeBotStationBlockEntity station = (TradeBotStationBlockEntity) helper.getBlockEntity(hall.station);
		TradeBotMotion.debug = true;
		long[] clipStart = { -1 };
		int[] clips = { 0 };
		helper.onEachTick(() -> {
			if (station.tradesToday() > 5)
				helper.fail("he made " + station.tradesToday() + " trades, more than his goods pay for");
			if (bot.clip().name().startsWith("trade_villager") && bot.clipStart() != clipStart[0]) {
				clipStart[0] = bot.clipStart();
				clips[0]++;
			}
		});
		helper.succeedWhen(() -> {
			if (station.tradesToday() < 5 || !bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME)
				helper.fail(station.tradesToday() + " of 5 trades made (" + bot.brain.task() + ")");
			if (count(helper, output, Items.EMERALD) != 5)
				helper.fail("the output holds " + count(helper, output, Items.EMERALD) + " emeralds, not 5");
			// each trade's XP is vanilla's 3-6, banked and delivered 3 a nugget (D57)
			int nuggets = count(helper, output, nugget());
			if (nuggets < 5 || nuggets > 10 || bot.getXpBank() >= TradeBotEntity.XP_PER_NUGGET)
				helper.fail(nuggets + " nuggets delivered and " + bot.getXpBank() + " XP kept from 5 trades");
			int[] left = { 0, 6, 5, 5, 10 };
			net.minecraft.world.item.Item[] goods = { Items.WHEAT, Items.PAPER, Items.COAL, Items.GLASS_PANE, Items.COBBLESTONE };
			for (int i = 0; i < goods.length; i++)
				if (count(helper, input, goods[i]) != left[i])
					helper.fail("the input has " + count(helper, input, goods[i]) + " " + goods[i] + " left, not " + left[i]);
			for (int i = 0; i < villagers.size(); i++) {
				Villager villager = villagers.get(i);
				if (bot.reputationWith(villager.getUUID()) != 2 * trades[i])
					helper.fail("his reputation with the " + villager.getVillagerData().getProfession() + " is " + bot.reputationWith(villager.getUUID()));
			}
			if (clips[0] != villagers.size())
				helper.fail(clips[0] + " trade clips for " + villagers.size() + " villagers: each visit's trades should be one clip");
			if (villagers.get(0).getVillagerXp() != 1 + 2 * 2)
				helper.fail("the farmer has " + villagers.get(0).getVillagerXp() + " XP, not 5");
		});
	}

	/**
	 * A villager stands up just as he starts the trade (D46): it's off, he shrugs, keeps everything, and on the way
	 * home puts the wheat back where it came from. No emerald, no use of the villager's stock.
	 */
	@GameTest(template = COURSE, timeoutTicks = 4000)
	public static void shrugsWhenTheVillagerStandsUp(GameTestHelper helper) {
		Hall hall = tradingHall(helper);
		helper.getLevel().setDayTime(1000);
		TradeBotEntity bot = station(helper, hall.station, Direction.SOUTH);
		BlockPos input = new BlockPos(7, 1, 1), output = new BlockPos(10, 1, 1);
		links(helper, hall.station, input, output);
		fill(helper, input, new ItemStack(Items.WHEAT, 64));
		Villager farmer = seated(helper, hall.seats.get(0), VillagerProfession.FARMER, offer(Items.WHEAT, 20, 3));
		boolean[] shrugged = { false };
		TradeBotMotion.debug = true;
		helper.onEachTick(() -> {
			if (farmer.isPassenger() && bot.clip().name().startsWith("trade_villager"))
				farmer.stopRiding();
			shrugged[0] |= bot.brain.task() == TradeBotBrain.Task.SHRUGGING;
		});
		helper.succeedWhen(() -> {
			if (!shrugged[0] || !bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME)
				helper.fail("not home after a shrug yet (" + bot.brain.task() + ", shrugged " + shrugged[0] + ")");
			if (count(helper, input, Items.WHEAT) != 64 || count(helper, output) != 0 || bot.getEmeralds() != 0)
				helper.fail("the input has " + count(helper, input, Items.WHEAT) + " wheat back, the output " + count(helper, output) + " items");
			if (farmer.getOffers().get(0).getUses() != 0)
				helper.fail("the farmer's stock was used");
		});
	}

	// ---- feeling alive (milestone 6)

	/**
	 * A zombie in the yard (D8): he spots it, flees, hides in a corner (two walls at least), cowers until it's gone,
	 * recovers, and carries on to his stop and home. (It stands still: monsters ignore him, D26.)
	 */
	@GameTest(template = COURSE, timeoutTicks = 3000)
	public static void hidesFromAZombieAndCarriesOn(GameTestHelper helper) {
		TradeBotEntity bot = yard(helper);
		net.minecraft.world.entity.monster.Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(10, 1, 9));
		// a helmet keeps it from burning in the sun (the time is shared by every course running at once)
		zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
		java.util.EnumSet<TradeBotBrain.Task> seen = java.util.EnumSet.noneOf(TradeBotBrain.Task.class);
		BlockPos[] hid = { null };
		helper.runAfterDelay(10, () -> bot.brain.goTo(helper.absolutePos(new BlockPos(3, 1, 14))));
		helper.onEachTick(() -> {
			seen.add(bot.brain.task());
			if (bot.brain.task() == TradeBotBrain.Task.SCARED_HIDE && hid[0] == null)
				hid[0] = bot.blockPosition();
			if (bot.brain.task() == TradeBotBrain.Task.SCARED_COWER && zombie.isAlive())
				zombie.discard(); // it wanders off; once it's been gone a while he recovers
		});
		helper.succeedWhen(() -> {
			for (TradeBotBrain.Task stage : List.of(TradeBotBrain.Task.SCARED_SPOT, TradeBotBrain.Task.SCARED_FLEE, TradeBotBrain.Task.SCARED_HIDE,
				TradeBotBrain.Task.SCARED_COWER, TradeBotBrain.Task.SCARED_RECOVER))
				if (!seen.contains(stage))
					helper.fail("never " + stage + " (" + bot.brain.task() + ")");
			int walls = 0;
			for (Direction side : Direction.Plane.HORIZONTAL)
				if (!helper.getLevel().getBlockState(hid[0].relative(side)).getCollisionShape(helper.getLevel(), hid[0].relative(side)).isEmpty())
					walls++;
			if (walls < 2)
				helper.fail("he hid in the open at " + helper.relativePos(hid[0]).toShortString());
			if (bot.brain.visited().size() != 1 || !bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME)
				helper.fail("didn't carry on to his stop and home (" + bot.brain.task() + ")");
		});
	}

	/** Pushed into water (D29): he winds down; out of it and dry, he winds himself up and carries on. */
	@GameTest(template = COURSE, timeoutTicks = 3000)
	public static void windsDownInWaterAndDriesOut(GameTestHelper helper) {
		TradeBotEntity bot = yard(helper);
		BlockPos pool = new BlockPos(8, 1, 8);
		for (Direction side : Direction.Plane.HORIZONTAL)
			helper.setBlock(pool.relative(side), WALL);
		helper.setBlock(pool, Blocks.WATER.defaultBlockState());
		boolean[] state = { false, false, false }; // pushed in, wound down, wound up
		helper.runAfterDelay(10, () -> bot.brain.goTo(helper.absolutePos(new BlockPos(3, 1, 14))));
		helper.onEachTick(() -> {
			if (!state[0] && bot.brain.task() == TradeBotBrain.Task.GOING && bot.motion.canHalt()) {
				state[0] = true;
				Vec3 in = Vec3.atBottomCenterOf(helper.absolutePos(pool));
				bot.teleportTo(in.x, in.y, in.z);
			}
			if (bot.isWoundDown() && !state[1]) {
				state[1] = true;
				if (bot.rundown() != TradeBotEntity.Rundown.WATER)
					helper.fail("wound down by " + bot.rundown() + ", not water");
				Vec3 out = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 1, 11)));
				helper.runAfterDelay(20, () -> bot.teleportTo(out.x, out.y, out.z)); // pulled out
			}
			state[2] |= state[1] && bot.brain.task() == TradeBotBrain.Task.WINDING_UP;
		});
		helper.succeedWhen(() -> {
			if (!state[1] || !state[2])
				helper.fail("not wound down and up again yet (" + bot.brain.task() + ")");
			if (!bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME)
				helper.fail("didn't carry on home (" + bot.brain.task() + ")");
		});
	}

	/**
	 * Hurt to nothing (D24): he winds down instead of dying and stays down, slumped, until his owner's wrench winds him
	 * up, back to full health; then he carries on.
	 */
	@GameTest(template = COURSE, timeoutTicks = 3000)
	public static void windsDownAtNoHealthAndTheWrenchWindsHimUp(GameTestHelper helper) {
		TradeBotEntity bot = yard(helper);
		long[] downAt = { -1 };
		helper.runAfterDelay(10, () -> bot.brain.goTo(helper.absolutePos(new BlockPos(3, 1, 14))));
		helper.onEachTick(() -> {
			if (downAt[0] < 0 && bot.brain.task() == TradeBotBrain.Task.GOING && bot.motion.canHalt()) {
				bot.hurt(helper.getLevel().damageSources().generic(), 100);
				if (!bot.isWoundDown() || bot.isRemoved() || bot.getHealth() <= 0)
					helper.fail("he didn't wind down (health " + bot.getHealth() + ", removed " + bot.isRemoved() + ")");
				downAt[0] = helper.getTick();
			}
			if (downAt[0] >= 0 && helper.getTick() == downAt[0] + 200) {
				if (!bot.isWoundDown() || bot.brain.task() != TradeBotBrain.Task.WOUND_DOWN)
					helper.fail("he didn't stay down until wound up");
				bot.windUp(true); // the owner's wrench
				if (bot.getHealth() != bot.getMaxHealth())
					helper.fail("wound up to " + bot.getHealth() + " health");
			}
		});
		helper.succeedWhen(() -> {
			if (downAt[0] < 0 || bot.isWoundDown() || !bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME)
				helper.fail("not wound up and home yet (" + bot.brain.task() + ")");
		});
	}

	/** Wound down in lava, or fallen out of the world (D59): put back in front of his station, wound down, not burning. */
	@GameTest(template = COURSE, timeoutTicks = 2000)
	public static void comesHomeFromLavaAndTheVoid(GameTestHelper helper) {
		floor(helper, 0, 0, 0, 23, 23, FLOOR);
		TradeBotEntity lava = station(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
		TradeBotEntity fallen = station(helper, new BlockPos(15, 1, 2), Direction.SOUTH);
		BlockPos pit = new BlockPos(8, 1, 12);
		for (Direction side : Direction.Plane.HORIZONTAL)
			helper.setBlock(pit.relative(side), WALL);
		helper.setBlock(pit, Blocks.LAVA.defaultBlockState());
		helper.runAfterDelay(10, () -> {
			lava.brain.goTo(helper.absolutePos(new BlockPos(3, 1, 14)));
			fallen.brain.goTo(helper.absolutePos(new BlockPos(15, 1, 14)));
		});
		boolean[] sent = { false, false };
		helper.onEachTick(() -> {
			if (!sent[0] && lava.brain.task() == TradeBotBrain.Task.GOING && lava.motion.canHalt()) {
				sent[0] = true;
				lava.setHealth(2);
				Vec3 in = Vec3.atBottomCenterOf(helper.absolutePos(pit));
				lava.teleportTo(in.x, in.y, in.z);
			}
			if (!sent[1] && fallen.brain.task() == TradeBotBrain.Task.GOING && fallen.motion.canHalt()) {
				sent[1] = true;
				fallen.teleportTo(fallen.getX(), helper.getLevel().getMinBuildHeight() - 70, fallen.getZ());
			}
		});
		helper.succeedWhen(() -> {
			for (TradeBotEntity bot : List.of(lava, fallen)) {
				BlockPos front = bot.getStation().relative(Direction.SOUTH);
				if (bot.isRemoved() || !bot.isWoundDown() || bot.isOnFire() || bot.isInLava()
					|| bot.position().distanceTo(Vec3.atBottomCenterOf(front)) > 1.5)
					helper.fail("not back at his station, wound down (at " + helper.relativePos(bot.blockPosition()).toShortString() + ", wound "
						+ bot.isWoundDown() + ", removed " + bot.isRemoved() + ")");
			}
		});
	}

	/**
	 * Run down asleep in his cradle and wound up there: he hops out like any morning (no floating on top of his
	 * station), then carries on, here straight back in, as he has nothing to do.
	 */
	@GameTest(template = COURSE, timeoutTicks = 2000)
	public static void windsUpInHisCradleAndHopsOut(GameTestHelper helper) {
		TradeBotEntity bot = yard(helper);
		java.util.List<TradeBotBrain.Task> after = new ArrayList<>();
		long[] downAt = { -1 };
		helper.onEachTick(() -> {
			if (downAt[0] < 0 && helper.getTick() == 20) {
				if (!bot.isDocked())
					helper.fail("not docked to start with");
				bot.hurt(helper.getLevel().damageSources().generic(), 100);
				downAt[0] = helper.getTick();
			}
			if (downAt[0] >= 0 && helper.getTick() == downAt[0] + 60)
				bot.windUp(true);
			if (downAt[0] >= 0 && (after.isEmpty() || after.get(after.size() - 1) != bot.brain.task()))
				after.add(bot.brain.task());
		});
		helper.succeedWhen(() -> {
			if (!after.contains(TradeBotBrain.Task.WINDING_UP) || !after.contains(TradeBotBrain.Task.LEAVING))
				helper.fail("didn't wind up and hop out: " + after);
			if (after.indexOf(TradeBotBrain.Task.LEAVING) < after.indexOf(TradeBotBrain.Task.WINDING_UP))
				helper.fail("hopped out before winding up: " + after);
			if (!bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME)
				helper.fail("not back in his cradle (" + after + ")");
		});
	}

	/**
	 * Someone standing by his station while he sleeps: he doesn't react asleep, or mid-hop; he greets them once he's
	 * out on the ground (D9). And only once.
	 */
	@GameTest(template = COURSE, timeoutTicks = 2400)
	public static void greetsSomeoneOnceHesOut(GameTestHelper helper) {
		TradeBotEntity bot = yard(helper);
		// a stand-in player, added straight to the world (joining one to the server trips other mods' join packets)
		net.neoforged.neoforge.common.util.FakePlayer visitor = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
			new com.mojang.authlib.GameProfile(UUID.randomUUID(), "visitor"));
		Vec3 by = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 1, 4)));
		visitor.moveTo(by.x, by.y, by.z, 0, 0);
		helper.getLevel().addNewPlayer(visitor);
		int[] greetings = { 0 };
		TradeBotBrain.Task[] last = { null };
		helper.runAfterDelay(60, () -> bot.brain.goTo(helper.absolutePos(new BlockPos(3, 1, 14))));
		helper.onEachTick(() -> {
			TradeBotBrain.Task task = bot.brain.task();
			if (task == TradeBotBrain.Task.REACTING && last[0] != TradeBotBrain.Task.REACTING) {
				greetings[0]++;
				if (bot.isDocked() || !bot.onGround() || !bot.clip().name().equals("sees_player"))
					helper.fail("reacted with " + bot.clip().name() + " docked " + bot.isDocked() + ", on the ground " + bot.onGround());
			}
			last[0] = task;
		});
		helper.succeedWhen(() -> {
			if (greetings[0] != 1 || bot.brain.visited().size() != 1 || !bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME)
				helper.fail(greetings[0] + " greetings, " + bot.brain.visited().size() + " stops (" + bot.brain.task() + ")");
			helper.getLevel().removePlayerImmediately(visitor, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
		});
	}

	/** An open yard walled all round (x 0-12, z 0-16), with his station at (3, 1, 2) facing south. */
	private static TradeBotEntity yard(GameTestHelper helper) {
		floor(helper, 0, 0, 0, 23, 23, FLOOR);
		solid(helper, 0, 1, 0, 12, 3, 0, WALL);
		solid(helper, 0, 1, 16, 12, 3, 16, WALL);
		solid(helper, 0, 1, 0, 0, 3, 16, WALL);
		solid(helper, 12, 1, 0, 12, 3, 16, WALL);
		TradeBotMotion.debug = true;
		return station(helper, new BlockPos(3, 1, 2), Direction.SOUTH);
	}

	/** A sell-to-villager offer: {@code count} goods for an emerald, {@code maxUses} times, 2 villager XP each. */
	private static MerchantOffer offer(net.minecraft.world.item.Item goods, int count, int maxUses) {
		return new MerchantOffer(new ItemCost(goods, count), java.util.Optional.empty(), new ItemStack(Items.EMERALD), 0, maxUses, 2, 0.05f, 0);
	}

	/** A grown villager of {@code profession} with just these offers, sat in the Create seat at {@code seat}. */
	private static Villager seated(GameTestHelper helper, BlockPos seat, VillagerProfession profession, MerchantOffer... offers) {
		Villager villager = helper.spawn(EntityType.VILLAGER, seat);
		villager.setVillagerData(villager.getVillagerData().setProfession(profession).setLevel(1));
		villager.setVillagerXp(1); // with some XP it keeps its profession away from a job site
		villager.overrideOffers(new MerchantOffers());
		for (MerchantOffer offer : offers)
			villager.getOffers().add(offer);
		SeatBlock.sitDown(helper.getLevel(), helper.absolutePos(seat), villager);
		return villager;
	}

	/** A chest at each of {@code input} and {@code output} in the yard, facing the station's side, linked to it. */
	private static void links(GameTestHelper helper, BlockPos station, BlockPos input, BlockPos output) {
		for (BlockPos chest : List.of(input, output))
			helper.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.SOUTH));
		TradeBotStationBlockEntity be = (TradeBotStationBlockEntity) helper.getBlockEntity(station);
		be.setRole(helper.absolutePos(input), TradeBotLinks.Role.TAKES);
		be.setRole(helper.absolutePos(output), TradeBotLinks.Role.DELIVERS);
	}

	private static void fill(GameTestHelper helper, BlockPos pos, ItemStack... stacks) {
		IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
		if (handler == null)
			helper.fail("no inventory at " + pos.toShortString());
		for (ItemStack stack : stacks)
			ItemHandlerHelper.insertItemStacked(handler, stack, false);
	}

	private static net.minecraft.world.item.Item nugget() {
		return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "experience_nugget"));
	}

	private static int count(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item item) {
		IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
		int total = 0;
		if (handler != null)
			for (int slot = 0; slot < handler.getSlots(); slot++)
				if (handler.getStackInSlot(slot).is(item))
					total += handler.getStackInSlot(slot).getCount();
		return total;
	}

	private static int count(GameTestHelper helper, BlockPos pos) {
		IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
		int total = 0;
		if (handler != null)
			for (int slot = 0; slot < handler.getSlots(); slot++)
				total += handler.getStackInSlot(slot).getCount();
		return total;
	}

	// ---- the trading hall

	record Hall(BlockPos station, List<BlockPos> spots, List<BlockPos> seats, BlockPos door, BlockPos gate) {}

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
		List<BlockPos> seats = List.of(new BlockPos(6, 2, 15), new BlockPos(11, 2, 15), new BlockPos(15, 2, 15), new BlockPos(21, 3, 15));
		return new Hall(new BlockPos(3, 1, 2), spots, seats, new BlockPos(3, 2, 8), new BlockPos(17, 2, 11));
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
