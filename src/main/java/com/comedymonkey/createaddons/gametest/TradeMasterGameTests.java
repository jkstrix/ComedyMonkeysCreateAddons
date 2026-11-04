package com.comedymonkey.createaddons.gametest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.BotKind;
import com.comedymonkey.createaddons.content.tradebot.TradeBotBrain;
import com.comedymonkey.createaddons.content.tradebot.TradeBotData;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.TradeBotItem;
import com.comedymonkey.createaddons.content.tradebot.TradeBotRegistry;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.comedymonkey.createaddons.content.tradebot.board.TradeBoardEntity;
import com.comedymonkey.createaddons.content.tradebot.master.GaugeNames;
import com.comedymonkey.createaddons.content.tradebot.master.MasterStatus;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Trade Master's item, station and upgrade (design/plans/trade-master.md, M2), and his Trade Boards (M3). Run headless with
 * {@code ./gradlew runGameTestServer}.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TradeMasterGameTests {
	private static final String COURSE = "trade_bot_course";
	/** The upgrade's grid (spec D27), as in its recipe file. */
	private static final String[] PATTERN = { "NSKSN", "SPLPS", "GETEG", "SPCPS", "NSSSN" };

	private TradeMasterGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(TradeMasterGameTests.class);
	}

	/**
	 * A Trade Bot on Create's crafter grid, with the rest of the recipe round him, becomes the Trade Master, keeping his
	 * charge, earnings, banked XP, reputation and name, and leaving his container links behind (D16, D40, P10). It goes
	 * through Create's own crafter code, matching included.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void upgradeKeepsTheTradeBotsData(GameTestHelper helper) {
		UUID villager = UUID.randomUUID();
		ItemStack bot = new ItemStack(ModItems.TRADE_BOT.get());
		bot.set(ModDataComponents.TRADE_BOT_DATA.get(), new TradeBotData(1234, 7, 5, Map.of(villager, 12), ItemContainerContents.EMPTY,
			List.of(new BlockPos(1, 2, 3)), Optional.of(new BlockPos(4, 5, 6))));
		bot.set(DataComponents.CUSTOM_NAME, Component.literal("Gerald"));
		ItemStack out = RecipeGridHandler.tryToApplyRecipe(helper.getLevel(), grid(helper, bot, true));
		if (out == null || !out.is(ModItems.TRADE_MASTER.get()))
			throw new IllegalStateException("the grid didn't make a Trade Master: " + out);
		TradeBotData data = out.get(ModDataComponents.TRADE_BOT_DATA.get());
		if (data == null)
			throw new IllegalStateException("the Trade Master carries no data");
		helper.assertValueEqual(data.charge(), 1234, "charge");
		helper.assertValueEqual(data.emeralds(), 7L, "emeralds");
		helper.assertValueEqual(data.xpBank(), 5, "banked XP");
		helper.assertValueEqual(data.reputation().get(villager), 12, "reputation");
		helper.assertTrue(data.inputs().isEmpty() && data.output().isEmpty(), "his container links stay behind");
		helper.assertValueEqual(out.getHoverName().getString(), "Gerald", "his name");
		// without the Trade Ledger, it's no recipe
		helper.assertTrue(RecipeGridHandler.tryToApplyRecipe(helper.getLevel(), grid(helper, bot, false)) == null, "made without the ledger");
		helper.succeed();
	}

	/** His station spawns a Trade Master, who counts toward his own limit (D29), not Trade Bot's. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void hisStationSpawnsHimAndCountsHimApart(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 1, 4);
		helper.setBlock(pos, ModBlocks.TRADE_MASTER.get().defaultBlockState());
		UUID owner = UUID.randomUUID();
		TradeBotEntity bot = TradeBotItem.populate(helper.getLevel(), helper.absolutePos(pos), owner, TradeBotData.EMPTY, null);
		if (bot == null)
			throw new IllegalStateException("no bot in the station");
		helper.assertTrue(bot.getType() == ModEntities.TRADE_MASTER.get() && bot.kind() == BotKind.TRADE_MASTER, "the station's bot is a " + bot.getType());
		if (!(helper.getBlockEntity(pos) instanceof TradeBotStationBlockEntity station) || station.kind() != BotKind.TRADE_MASTER)
			throw new IllegalStateException("not a Trade Master station");
		TradeBotRegistry registry = TradeBotRegistry.get(helper.getLevel().getServer());
		helper.assertValueEqual(registry.count(owner, BotKind.TRADE_MASTER), 1, "Trade Masters counted");
		helper.assertValueEqual(registry.count(owner, BotKind.TRADE_BOT), 0, "Trade Bots counted");
		helper.assertTrue(ModItems.TRADE_MASTER.get().kind() == BotKind.TRADE_MASTER && ModItems.TRADE_BOT.get().kind() == BotKind.TRADE_BOT, "item kinds");
		helper.succeed();
	}

	/**
	 * A tuned Trade Master item places a station on its network (P1); an untuned one is a Stock Link of its own and
	 * starts a network (user, 2026-11-04: "he is the stock link"); Trade Bot's station has none. Packed, it goes back in.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void hisStationKeepsItsNetwork(GameTestHelper helper) {
		UUID network = UUID.randomUUID();
		ItemStack stack = new ItemStack(ModItems.TRADE_MASTER.get());
		LogisticallyLinkedBlockItem.assignFrequency(stack, helper.makeMockPlayer(GameType.CREATIVE), network);
		BlockPos floor = new BlockPos(6, 1, 6);
		helper.setBlock(floor, Blocks.STONE);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		Vec3 top = Vec3.atCenterOf(helper.absolutePos(floor)).add(0, 0.5, 0);
		BlockHitResult onTop = new BlockHitResult(top, Direction.UP, helper.absolutePos(floor), false);
		// untuned, he's a Stock Link of his own: his station starts its own network
		BlockPos ownFloor = new BlockPos(9, 1, 6);
		helper.setBlock(ownFloor, Blocks.STONE);
		ItemStack untuned = new ItemStack(ModItems.TRADE_MASTER.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, untuned);
		untuned.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(ownFloor)).add(0, 0.5, 0), Direction.UP, helper.absolutePos(ownFloor), false)));
		if (!(helper.getBlockEntity(ownFloor.above()) instanceof TradeBotStationBlockEntity own) || own.frequency() == null || own.frequency().equals(network))
			throw new IllegalStateException("untuned, his station should start a network of its own");
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, onTop));
		if (!(helper.getBlockEntity(floor.above()) instanceof TradeBotStationBlockEntity station))
			throw new IllegalStateException("no station placed");
		helper.assertValueEqual(station.frequency(), network, "his station's network");
		ItemStack packed = new ItemStack(ModItems.TRADE_MASTER.get());
		TradeBotItem.keepFrequency(packed, network, station.getType());
		helper.assertValueEqual(LogisticallyLinkedBlockItem.networkFromStack(packed), network, "the packed item's network");
		helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.TRADE_BOT.get().defaultBlockState());
		if (!(helper.getBlockEntity(new BlockPos(2, 2, 2)) instanceof TradeBotStationBlockEntity tradeBots) || tradeBots.frequency() != null)
			throw new IllegalStateException("Trade Bot's station is on a network");
		helper.succeed();
	}

	/**
	 * A Factory Gauge on a Trade Board's face counts the chest behind it (D41), not its network; without the board it's
	 * Create's gauge again and counts its (empty) network.
	 */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aGaugeOnABoardCountsItsChest(GameTestHelper helper) {
		BlockPos chest = chestWith(helper, new ItemStack(Items.BOOKSHELF, 10));
		FactoryPanelBehaviour gauge = gauge(helper, chest.north(), Direction.NORTH, PanelSlot.TOP_LEFT);
		gauge.setFilter(new ItemStack(Items.BOOKSHELF));
		helper.assertValueEqual(gauge.getLevelInStorage(), 0, "the gauge's count with no board");
		board(helper, chest.north(), Direction.NORTH);
		helper.succeedWhen(() -> helper.assertValueEqual(gauge.getLevelInStorage(), 10, "the gauge's count on a board"));
	}

	/** Create orders nothing for a gauge on a board (D31): its request timer never starts, though it's short and linked. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void createOrdersNothingForAGaugeOnABoard(GameTestHelper helper) {
		BlockPos chest = chestWith(helper, ItemStack.EMPTY);
		board(helper, chest.north(), Direction.NORTH);
		FactoryPanelBehaviour gauge = gauge(helper, chest.north(), Direction.NORTH, PanelSlot.TOP_LEFT);
		gauge.setFilter(new ItemStack(Items.BOOKSHELF));
		gauge.count = 1;
		// linked from the gauge beside it, as for a crafting chain, so Create would time a request
		FactoryPanelBehaviour from = gauge(helper, chest.north(), Direction.NORTH, PanelSlot.TOP_RIGHT);
		gauge.targetedBy.put(from.getPanelPosition(), new FactoryPanelConnection(from.getPanelPosition(), 1));
		helper.runAfterDelay(10, () -> {
			helper.assertValueEqual(requestTimer(helper, gauge), 0, "the gauge's request timer");
			helper.succeed();
		});
	}

	/**
	 * The board shares its space with the gauge and stays (D41); swap the chest for stone (solid, so an item frame would
	 * stay) and it drops: it hangs on containers only.
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void aBoardStaysByItsGaugeButOnlyOnAContainer(GameTestHelper helper) {
		BlockPos chest = chestWith(helper, ItemStack.EMPTY);
		gauge(helper, chest.north(), Direction.NORTH, PanelSlot.TOP_LEFT);
		TradeBoardEntity board = board(helper, chest.north(), Direction.NORTH);
		// a hanging thing checks its support every 100 ticks
		helper.runAfterDelay(110, () -> {
			helper.assertTrue(board.isAlive(), "the board fell off beside its gauge");
			helper.setBlock(chest, Blocks.STONE);
			helper.succeedWhen(() -> {
				helper.assertTrue(!board.isAlive(), "the board stayed on stone");
				helper.assertItemEntityPresent(ModItems.TRADE_BOARD.get());
			});
		});
	}

	/**
	 * The whole loop (spec "Filling a gauge", M4): a gauge on one board chest wants 8 bookshelves; his input board chest
	 * holds 3, with 100 wheat and 10 cobblestone. He fetches the 3 he owns (D35), sells the wheat to the farmer for 5
	 * emeralds (D22), buys 5 bookshelves from the librarian at base price (D32, D17), delivers all 8 and docks. The
	 * cobblestone nobody buys never leaves; his trade XP stays banked, as no chest has a Nugget gauge (D30).
	 */
	@GameTest(template = COURSE, timeoutTicks = 8000, batch = "trade_master_fills_a_gauge")
	public static void fillsAGaugeByFetchingSellingAndBuying(GameTestHelper helper) {
		// his owner's advancements: "Trade Master" for filling a gauge, "Supply Chain" for selling and buying for it in one trip
		java.util.Set<String> earned = java.util.concurrent.ConcurrentHashMap.newKeySet();
		ModAdvancements.Award listener = (player, action, value) -> earned.add(action);
		ModAdvancements.TEST_AWARDS.add(listener);
		helper.onEachTick(() -> {
			if (helper.getTick() >= 7990)
				ModAdvancements.TEST_AWARDS.remove(listener);
		});
		TradeBotGameTests.Hall hall = TradeBotGameTests.tradingHall(helper);
		helper.getLevel().setDayTime(1000);
		UUID network = UUID.randomUUID();
		TradeBotEntity bot = masterStation(helper, hall.station(), network);
		BlockPos input = new BlockPos(7, 1, 1), books = new BlockPos(10, 1, 1);
		boardChest(helper, input, network);
		boardChest(helper, books, network);
		TradeBotGameTests.fill(helper, input, new ItemStack(Items.BOOKSHELF, 3), new ItemStack(Items.WHEAT, 64), new ItemStack(Items.WHEAT, 36),
			new ItemStack(Items.COBBLESTONE, 10));
		gaugeOnTop(helper, books, PanelSlot.TOP_LEFT, new ItemStack(Items.BOOKSHELF), 8);
		TradeBotGameTests.seated(helper, hall.seats().get(0), VillagerProfession.FARMER, TradeBotGameTests.offer(Items.WHEAT, 20, 16));
		TradeBotGameTests.seated(helper, hall.seats().get(1), VillagerProfession.LIBRARIAN,
			new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.BOOKSHELF), 0, 12, 1, 0.05f, 0));
		helper.succeedWhen(() -> {
			if (!bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME || bot.brain.errand() == null)
				helper.fail("not home from a trip yet (" + bot.brain.task() + ")");
			helper.assertValueEqual(TradeBotGameTests.count(helper, books, Items.BOOKSHELF), 8, "bookshelves in the gauge's chest");
			helper.assertValueEqual(TradeBotGameTests.count(helper, input, Items.BOOKSHELF), 0, "bookshelves left in the input");
			helper.assertValueEqual(TradeBotGameTests.count(helper, input, Items.WHEAT), 0, "wheat left in the input");
			helper.assertValueEqual(TradeBotGameTests.count(helper, input, Items.COBBLESTONE), 10, "cobblestone, which nobody buys");
			helper.assertValueEqual(bot.getEmeralds(), 0L, "emeralds left in his pouch");
			helper.assertTrue(bot.getXpBank() > 0, "his trade XP is banked");
			helper.assertTrue(earned.contains(ModAdvancements.TRADE_MASTER_FILL) && earned.contains(ModAdvancements.TRADE_MASTER_SUPPLY_CHAIN),
				"his owner's advancements: " + earned);
			helper.assertFalse(earned.contains(ModAdvancements.TRADE_MASTER_BOOK), "no book, no Book Club");
			ModAdvancements.TEST_AWARDS.remove(listener);
		});
	}

	/**
	 * An emerald gauge is filled by selling (D22), but never from below a gauge (D23, D36): the only spare wheat is the
	 * input's 20, as another chest's 40 sit at its own wheat gauge. So one lot, one emerald, and that chest untouched.
	 */
	@GameTest(template = COURSE, timeoutTicks = 6000, batch = "trade_master_keeps_gauged_stock")
	public static void anEmeraldGaugeIsFilledFromSpareGoodsOnly(GameTestHelper helper) {
		TradeBotGameTests.Hall hall = TradeBotGameTests.tradingHall(helper);
		helper.getLevel().setDayTime(1000);
		UUID network = UUID.randomUUID();
		TradeBotEntity bot = masterStation(helper, hall.station(), network);
		BlockPos input = new BlockPos(7, 1, 1), wheat = new BlockPos(10, 1, 1), emeralds = new BlockPos(13, 1, 1);
		for (BlockPos chest : List.of(input, wheat, emeralds))
			boardChest(helper, chest, network);
		TradeBotGameTests.fill(helper, input, new ItemStack(Items.WHEAT, 20));
		TradeBotGameTests.fill(helper, wheat, new ItemStack(Items.WHEAT, 40));
		TradeBotGameTests.fill(helper, emeralds, new ItemStack(Items.EMERALD, 1));
		gaugeOnTop(helper, wheat, PanelSlot.TOP_LEFT, new ItemStack(Items.WHEAT), 40);
		gaugeOnTop(helper, emeralds, PanelSlot.TOP_LEFT, new ItemStack(Items.EMERALD), 3);
		TradeBotGameTests.seated(helper, hall.seats().get(0), VillagerProfession.FARMER, TradeBotGameTests.offer(Items.WHEAT, 20, 16));
		helper.succeedWhen(() -> {
			if (!bot.isDocked() || bot.brain.task() != TradeBotBrain.Task.HOME || bot.brain.errand() == null)
				helper.fail("not home from a trip yet (" + bot.brain.task() + ")");
			helper.assertValueEqual(TradeBotGameTests.count(helper, emeralds, Items.EMERALD), 2, "emeralds in the emerald gauge's chest");
			helper.assertValueEqual(TradeBotGameTests.count(helper, input, Items.WHEAT), 0, "the input's spare wheat");
			helper.assertValueEqual(TradeBotGameTests.count(helper, wheat, Items.WHEAT), 40, "wheat held at its gauge");
			// his station's list (P2, P3): the emerald gauge still short and waiting, above the full wheat gauge
			List<MasterStatus.Line> others = bot.brain.masterStatus().others();
			helper.assertValueEqual(others.size(), 2, "gauges in his station's list");
			helper.assertTrue(others.get(0).kind().is(Items.EMERALD) && others.get(0).state() == MasterStatus.State.WAITING
				&& others.get(0).level() == 2, "the emerald gauge first, waiting at 2: " + others.get(0));
			helper.assertTrue(others.get(1).kind().is(Items.WHEAT) && others.get(1).state() == MasterStatus.State.FULL, "the wheat gauge full, last");
		});
	}

	/**
	 * The user's play-test (2026-11-04): an emerald gauge "always a stack" on a board on one chest's side, sticks in
	 * another boarded chest, and a seated fletcher with vanilla's own trades. He fetches the sticks, sells them to the
	 * fletcher, and puts the emeralds in the gauge's chest, though he's stopped on his way to the chest and on his way to
	 * deliver (the user stood by him, and he stopped to greet them: the errand used to skip the chest he was heading for).
	 */
	@GameTest(template = COURSE, timeoutTicks = 6000, batch = "trade_master_sells_sticks_for_an_emerald_gauge")
	public static void sellsSticksForAnEmeraldGauge(GameTestHelper helper) {
		TradeBotGameTests.Hall hall = TradeBotGameTests.tradingHall(helper);
		helper.getLevel().setDayTime(1000);
		UUID network = UUID.randomUUID();
		TradeBotEntity bot = masterStation(helper, hall.station(), network);
		BlockPos sticks = new BlockPos(7, 1, 1), emeralds = new BlockPos(10, 1, 1);
		for (BlockPos chest : List.of(sticks, emeralds)) {
			helper.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
			helper.getLevel().addFreshEntity(new TradeBoardEntity(helper.getLevel(), helper.absolutePos(chest.east()), Direction.EAST, network));
		}
		TradeBotGameTests.fill(helper, sticks, new ItemStack(Items.STICK, 64), new ItemStack(Items.STICK, 64));
		Block gaugeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:factory_gauge"));
		helper.setBlock(emeralds.east(), gaugeBlock.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.WALL)
			.setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
		if (!(helper.getBlockEntity(emeralds.east()) instanceof FactoryPanelBlockEntity panel))
			throw new IllegalStateException("no gauge placed");
		panel.addPanel(PanelSlot.TOP_LEFT, network);
		FactoryPanelBehaviour gauge = panel.panels.get(PanelSlot.TOP_LEFT);
		gauge.setFilter(new ItemStack(Items.EMERALD));
		gauge.upTo = false; // in stacks: "always a stack of emeralds"
		gauge.count = 1;
		net.minecraft.world.entity.npc.Villager fletcher = helper.spawn(net.minecraft.world.entity.EntityType.VILLAGER, hall.seats().get(0));
		fletcher.setVillagerData(fletcher.getVillagerData().setProfession(VillagerProfession.FLETCHER).setLevel(1));
		fletcher.setVillagerXp(1);
		// vanilla's novice fletcher trades, the stick one included (vanilla picks two of these three at random)
		fletcher.getOffers().clear();
		fletcher.getOffers().add(new MerchantOffer(new ItemCost(Items.STICK, 32), Optional.empty(), new ItemStack(Items.EMERALD), 0, 16, 2, 0.05f, 0));
		fletcher.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.ARROW, 16), 0, 12, 1, 0.05f, 0));
		fletcher.getOffers().add(new MerchantOffer(new ItemCost(Items.GRAVEL, 10), Optional.of(new ItemCost(Items.EMERALD, 1)), new ItemStack(Items.FLINT, 10),
			0, 12, 1, 0.05f, 0));
		com.simibubi.create.content.contraptions.actors.seat.SeatBlock.sitDown(helper.getLevel(), helper.absolutePos(hall.seats().get(0)), fletcher);
		com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.debug = true;
		Player passerBy = helper.makeMockPlayer(GameType.SURVIVAL);
		boolean[] stopped = new boolean[2];
		// it must be his first trip that delivers: the old errand came home empty, and the next trip (unbothered) hid it
		Object[] first = new Object[1];
		helper.onEachTick(() -> {
			var errand = bot.brain.errand();
			if (first[0] == null && errand != null)
				first[0] = errand;
			if (errand == null || bot.brain.task() != TradeBotBrain.Task.APPROACHING)
				return;
			int on = errand.stage() == com.comedymonkey.createaddons.content.tradebot.master.MasterErrand.Stage.TAKE ? 0
				: errand.stage() == com.comedymonkey.createaddons.content.tradebot.master.MasterErrand.Stage.TIDY && errand.promised() > 0 ? 1 : -1;
			if (on >= 0 && !stopped[on] && bot.motion.isMoving()) {
				bot.brain.hitBy(passerBy); // a cross look, as at a hit: it stops him on the way, then he carries on
				stopped[on] = bot.brain.task() == TradeBotBrain.Task.REACTING; // only once it did stop him (else next tick)
			}
		});
		helper.succeedWhen(() -> {
			int got = TradeBotGameTests.count(helper, emeralds, Items.EMERALD);
			if (got == 0 || !bot.isDocked())
				helper.fail("no emeralds in the gauge's chest yet (" + bot.brain.task() + ", problems " + bot.brain.problems()
					+ ", fletcher sells " + fletcher.getOffers().stream().map(o -> o.getCostA().getCount() + " " + o.getCostA().getItem() + " -> "
						+ o.getResult().getItem()).toList() + ")");
			helper.assertTrue(stopped[0] && stopped[1], "he was stopped on the way to the chest and to deliver: " + java.util.Arrays.toString(stopped));
			helper.assertTrue(bot.brain.errand() == first[0], "his first trip came home without the emeralds");
			com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.debug = false;
		});
	}

	/**
	 * His station is a Stock Link: right-clicking it with a Factory Gauge tunes the gauge to his network, as on any Create
	 * link, rather than opening his screen (user, 2026-11-04: "Need to be able to tune a gauge by right click the bot
	 * station"). Through the game's own click handling, block first.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void aGaugeTunesOnHisStation(GameTestHelper helper) {
		UUID network = UUID.randomUUID();
		BlockPos pos = new BlockPos(4, 1, 4);
		masterStation(helper, pos, network);
		net.minecraft.server.level.ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
			new com.mojang.authlib.GameProfile(UUID.randomUUID(), "gauge_tuner"));
		ItemStack gauge = item("create:factory_gauge");
		player.setItemInHand(InteractionHand.MAIN_HAND, gauge);
		BlockPos at = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, helper.getLevel(), gauge, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
		helper.assertValueEqual(LogisticallyLinkedBlockItem.networkFromStack(gauge), network, "the gauge's network");
		helper.succeed();
	}

	/** His screen and goggles name an enchanted book by its enchantment, as that's what its gauge counts (P7). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void anEnchantedBookIsNamedByItsEnchantment(GameTestHelper helper) {
		var mending = helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
			.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING);
		ItemStack book = net.minecraft.world.item.EnchantedBookItem.createForEnchantment(new net.minecraft.world.item.enchantment.EnchantmentInstance(mending, 1));
		helper.assertValueEqual(GaugeNames.of(book).getString(), "Mending", "the enchanted book's name");
		helper.assertValueEqual(GaugeNames.of(new ItemStack(Items.BOOKSHELF)).getString(), "Bookshelf", "anything else's name");
		helper.succeed();
	}

	/** A charged Trade Master in his station at {@code pos}, facing the hall, his station on {@code network}. */
	private static TradeBotEntity masterStation(GameTestHelper helper, BlockPos pos, UUID network) {
		helper.setBlock(pos, ModBlocks.TRADE_MASTER.get().defaultBlockState().setValue(TradeBotStationBlock.FACING, Direction.SOUTH));
		TradeBotData charged = new TradeBotData(CMConfig.SERVER.tradeBotFullCharge(), 0, 0, Map.of(), ItemContainerContents.EMPTY, List.of(), Optional.empty());
		TradeBotEntity bot = TradeBotItem.populate(helper.getLevel(), helper.absolutePos(pos), UUID.randomUUID(), charged, null);
		if (bot == null || !(helper.getBlockEntity(pos) instanceof TradeBotStationBlockEntity station))
			throw new IllegalStateException("no Trade Master in his station");
		LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(station, LogisticallyLinkedBehaviour.TYPE);
		link.freqId = network;
		return bot;
	}

	/** A chest at {@code pos} with a Trade Board on its top, tuned to {@code network}. */
	private static void boardChest(GameTestHelper helper, BlockPos pos, UUID network) {
		helper.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
		helper.getLevel().addFreshEntity(new TradeBoardEntity(helper.getLevel(), helper.absolutePos(pos.above()), Direction.UP, network));
	}

	/** A gauge on a chest's top (on its board), asking for {@code amount} items of {@code kind}. */
	private static void gaugeOnTop(GameTestHelper helper, BlockPos chest, PanelSlot slot, ItemStack kind, int amount) {
		BlockPos at = chest.above();
		if (!(helper.getLevel().getBlockEntity(helper.absolutePos(at)) instanceof FactoryPanelBlockEntity)) {
			Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:factory_gauge"));
			helper.setBlock(at, block.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
				.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		}
		if (!(helper.getBlockEntity(at) instanceof FactoryPanelBlockEntity panel))
			throw new IllegalStateException("no gauge placed");
		panel.addPanel(slot, null);
		FactoryPanelBehaviour gauge = panel.panels.get(slot);
		gauge.setFilter(kind);
		gauge.upTo = true;
		gauge.count = amount;
	}

	private static BlockPos chestWith(GameTestHelper helper, ItemStack stack) {
		BlockPos chest = new BlockPos(4, 2, 4);
		helper.setBlock(chest, Blocks.CHEST);
		if (!stack.isEmpty() && helper.getBlockEntity(chest) instanceof ChestBlockEntity be)
			be.setItem(0, stack);
		return chest;
	}

	/** A Factory Gauge at {@code pos}, hung on the face behind it, facing {@code facing}, with {@code slot}'s gauge in it. */
	private static FactoryPanelBehaviour gauge(GameTestHelper helper, BlockPos pos, Direction facing, PanelSlot slot) {
		// (the helper's getBlockEntity throws on an empty space)
		if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof FactoryPanelBlockEntity)) {
			Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("create:factory_gauge"));
			helper.setBlock(pos, block.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.WALL)
				.setValue(HorizontalDirectionalBlock.FACING, facing));
		}
		if (!(helper.getBlockEntity(pos) instanceof FactoryPanelBlockEntity panel))
			throw new IllegalStateException("no gauge placed");
		panel.addPanel(slot, null);
		return panel.panels.get(slot);
	}

	private static TradeBoardEntity board(GameTestHelper helper, BlockPos pos, Direction facing) {
		TradeBoardEntity board = new TradeBoardEntity(helper.getLevel(), helper.absolutePos(pos), facing, UUID.randomUUID());
		helper.getLevel().addFreshEntity(board);
		return board;
	}

	private static int requestTimer(GameTestHelper helper, FactoryPanelBehaviour gauge) {
		try {
			Field timer = FactoryPanelBehaviour.class.getDeclaredField("timer");
			timer.setAccessible(true);
			return timer.getInt(gauge);
		} catch (ReflectiveOperationException e) {
			helper.fail("Create's gauge changed: " + e);
			return -1;
		}
	}

	/** The crafter grid for the upgrade, filled as the pattern says; {@code ledger} false leaves the Trade Ledger out. */
	private static RecipeGridHandler.GroupedItems grid(GameTestHelper helper, ItemStack bot, boolean ledger) {
		Map<Character, ItemStack> key = new HashMap<>();
		key.put('T', bot);
		key.put('L', ledger ? new ItemStack(ModItems.TRADE_LEDGER.get()) : ItemStack.EMPTY);
		key.put('K', item("create:stock_link"));
		key.put('G', item("create:factory_gauge"));
		key.put('P', item("create:precision_mechanism"));
		key.put('E', item("minecraft:emerald_block"));
		key.put('C', item("create:brass_casing"));
		key.put('N', new ItemStack(ModItems.NETHERITE_SHEET.get()));
		key.put('S', item("create:brass_sheet"));
		RecipeGridHandler.GroupedItems items = new RecipeGridHandler.GroupedItems();
		try {
			// the grid is Create's own: y goes up, so the pattern's top row is the highest
			Field field = RecipeGridHandler.GroupedItems.class.getDeclaredField("grid");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<Pair<Integer, Integer>, ItemStack> grid = (Map<Pair<Integer, Integer>, ItemStack>) field.get(items);
			for (int y = 0; y < PATTERN.length; y++)
				for (int x = 0; x < PATTERN[y].length(); x++) {
					ItemStack stack = key.get(PATTERN[y].charAt(x));
					if (!stack.isEmpty())
						grid.put(Pair.of(x, PATTERN.length - 1 - y), stack.copy());
				}
		} catch (ReflectiveOperationException e) {
			helper.fail("Create's crafter grid changed: " + e);
		}
		return items;
	}

	private static ItemStack item(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
	}
}
