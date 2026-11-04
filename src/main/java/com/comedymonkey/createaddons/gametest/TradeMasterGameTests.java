package com.comedymonkey.createaddons.gametest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tradebot.BotKind;
import com.comedymonkey.createaddons.content.tradebot.TradeBotData;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.TradeBotItem;
import com.comedymonkey.createaddons.content.tradebot.TradeBotRegistry;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.content.tradebot.board.TradeBoardEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

	/** A tuned Trade Master item places a station on its network (P1); Trade Bot's station has none. Packed, it goes back in. */
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
		stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(top, Direction.UP, helper.absolutePos(floor), false)));
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
