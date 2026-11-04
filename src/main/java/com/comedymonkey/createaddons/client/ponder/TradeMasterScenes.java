package com.comedymonkey.createaddons.client.ponder;

import java.util.List;
import java.util.UUID;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.board.TradeBoardEntity;
import com.comedymonkey.createaddons.content.tradebot.trade.TradeSpot;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Trade Master's Ponder scenes (design/specs/trade-master.md P12): joining a network, and filling gauges. He plays
 * his real clips on his own clock, as Trade Bot does in his ({@link TradeBotScenes}, whose movement helpers these
 * share). The structures come from design/ponder/trade-master.ponder.js; the visible text from
 * {@code comedymonkeysaddons.ponder.trade_master*.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class TradeMasterScenes {
	/** One network for each scene's boards (only their look matters here). */
	private static final UUID NETWORK = UUID.fromString("7a3d6f1e-2b4c-4d8e-9f10-5c6b7a8d9e0f");

	private TradeMasterScenes() {}

	/** The Trade Master docked in his station at {@code station}. */
	private static ElementLink<EntityElement> dockedMaster(CreateSceneBuilder scene, BlockPos station, Direction facing) {
		return scene.world().createEntity(level -> {
			TradeBotEntity bot = ModEntities.TRADE_MASTER.get().create(level);
			bot.setPonder();
			bot.dockAt(station, facing);
			return bot;
		});
	}

	/** A Trade Board hung on {@code container}'s {@code face}. */
	private static void board(CreateSceneBuilder scene, BlockPos container, Direction face) {
		scene.world().createEntity(level -> new TradeBoardEntity(level, container.relative(face), face, NETWORK));
	}

	/** The gauge at {@code pos} asking for {@code amount} of {@code kind}. */
	private static void gauge(CreateSceneBuilder scene, BlockPos pos, ItemStack kind, int amount) {
		scene.world().modifyBlockEntity(pos, FactoryPanelBlockEntity.class, panel -> {
			panel.addPanel(PanelSlot.TOP_LEFT, NETWORK);
			FactoryPanelBehaviour gauge = panel.panels.get(PanelSlot.TOP_LEFT);
			gauge.setFilter(kind);
			gauge.upTo = true;
			gauge.count = amount;
		});
	}

	/** A grown villager of {@code profession} sat on the Create seat at {@code seat}, facing west to its workstation. */
	private static Vec3 seated(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos seat, VillagerProfession profession) {
		Vec3 at = util.vector().of(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5);
		scene.world().createEntity(level -> {
			SeatEntity chair = new SeatEntity(level);
			chair.setPos(at.x, at.y, at.z);
			level.addFreshEntity(chair);
			Villager villager = EntityType.VILLAGER.create(level);
			villager.setVillagerData(villager.getVillagerData().setProfession(profession));
			villager.setPos(at.x, at.y, at.z);
			villager.startRiding(chair, true);
			chair.positionRider(villager);
			villager.setYRot(90);
			villager.yBodyRot = villager.yHeadRot = 90;
			return villager;
		});
		return at;
	}

	// ---- 1. joining a network

	public static void network(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("trade_master", "The Trade Master and His Network");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos station = util.grid().at(3, 1, 5), link = util.grid().at(5, 1, 5);
		BlockPos input = util.grid().at(1, 1, 2), output = util.grid().at(4, 1, 2), gauge = util.grid().at(4, 2, 2);
		scene.world().showSection(util.select().position(link), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(link), Pointing.DOWN, 60).rightClick().withItem(new ItemStack(ModItems.TRADE_MASTER.get()));
		scene.overlay().showText(100)
			.text("The Trade Master is a Trade Bot upgraded on Mechanical Crafters, and he is a Stock Link himself. To join a network you already have, right-click one of its Stock Links with his item before placing him")
			.pointAt(util.vector().topOf(link))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.world().showSection(util.select().position(station), Direction.DOWN);
		ElementLink<EntityElement> master = dockedMaster(scene, station, Direction.NORTH);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("Placed, his station is a link on that network; placed as he comes, it starts a network of its own")
			.pointAt(util.vector().topOf(station).add(0, 0.6, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().position(input).add(util.select().position(output)), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showControls(util.vector().topOf(station), Pointing.DOWN, 50).rightClick().withItem(new ItemStack(ModItems.TRADE_BOARD.get()));
		scene.overlay().showText(90)
			.text("He works only through his network, by Trade Boards. Tune them the same way, on his station or any block on the network")
			.pointAt(util.vector().topOf(station))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(input), Pointing.DOWN, 40).rightClick().withItem(new ItemStack(ModItems.TRADE_BOARD.get()));
		scene.idle(15);
		board(scene, input, Direction.UP);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, input, new AABB(input), 120);
		scene.overlay().showText(100)
			.text("Hang a tuned board on any face of a container. With no gauges, the chest is an input: he takes from it, to sell or to spend")
			.pointAt(util.vector().topOf(input))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		board(scene, output, Direction.UP);
		scene.idle(10);
		scene.world().showSection(util.select().position(gauge), Direction.DOWN);
		gauge(scene, gauge, new ItemStack(Items.BOOKSHELF), 128);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, output, new AABB(output), 120);
		scene.overlay().showText(100)
			.text("Put Factory Gauges on a board's face, up to four, and the chest is an output: he keeps it filled to what they ask")
			.pointAt(util.vector().topOf(gauge))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showText(110)
			.text("A gauge on a board counts only the chest behind it, and Create never orders for it. He never takes anything from a chest below its gauges' amounts")
			.pointAt(util.vector().topOf(gauge))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showText(100)
			.text("Chests without a board don't exist for him: keep anything he mustn't sell there. He uses every board within 64 blocks of his station")
			.pointAt(util.vector().topOf(input))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(station), Pointing.DOWN, 50).rightClick();
		scene.overlay().showText(100)
			.text("Right-click his station for its screen: his network, the gauge he's working on, and every other gauge, with why he can't fill any he can't")
			.pointAt(util.vector().topOf(station).add(0, 0.6, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		TradeBotScenes.play(scene, master, "expr_happy");
		scene.idle(40);
		scene.markAsFinished();
	}

	// ---- 2. filling gauges

	public static void gauges(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("trade_master_gauges", "Filling Gauges");
		scene.configureBasePlate(0, 0, 9);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos station = util.grid().at(1, 1, 3), input = util.grid().at(1, 1, 5), output = util.grid().at(3, 1, 5), gauge = util.grid().at(3, 2, 5);
		scene.world().showSection(util.select().fromTo(1, 1, 3, 3, 2, 5), Direction.DOWN);
		ElementLink<EntityElement> master = dockedMaster(scene, station, Direction.EAST);
		board(scene, input, Direction.UP);
		board(scene, output, Direction.UP);
		gauge(scene, gauge, new ItemStack(Items.BOOKSHELF), 8);
		scene.idle(10);
		scene.world().showSection(util.select().fromTo(6, 1, 1, 7, 1, 4), Direction.DOWN);
		scene.idle(10);
		Vec3 farmer = seated(scene, util, util.grid().at(7, 1, 4), VillagerProfession.FARMER);
		Vec3 librarian = seated(scene, util, util.grid().at(7, 1, 1), VillagerProfession.LIBRARIAN);
		scene.idle(15);

		scene.overlay().showText(100)
			.text("A gauge on one of his boards is short: it wants 8 Bookshelves. Whenever it's day, he works on the emptiest short gauge first")
			.pointAt(util.vector().topOf(gauge))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		TradeBotScenes.playPhysical(scene, master, "action_comes_in");
		Vec3 out = util.vector().of(1.5 + 31 / 16.0, 1, 3.5);
		Vec3 atInput = util.vector().of(2.9, 1, 5.5);
		TradeBotScenes.roll(scene, master, out, List.of(new Vec3(atInput.x, 1, 4.2), atInput), Direction.WEST.toYRot());
		scene.overlay().showText(100)
			.text("First he fetches any his other board chests hold above their own gauges. Then, for the rest, he goes trading")
			.pointAt(util.vector().topOf(input))
			.placeNearTarget()
			.attachKeyFrame();
		TradeBotScenes.play(scene, master, "chest_take");
		scene.idle(110);

		Vec3 farmerSpot = farmer.add(-TradeSpot.SEAT_DISTANCE, 0, 0);
		TradeBotScenes.roll(scene, master, atInput, List.of(new Vec3(farmerSpot.x, 1, 5.5), farmerSpot), Direction.EAST.toYRot());
		TradeBotScenes.play(scene, master, "trade_villager");
		scene.overlay().showText(110)
			.text("Short of emeralds, he spends his chests' spare ones, then sells what his chests hold the most of to seated villagers. Never anything below a gauge")
			.pointAt(farmer.add(0, 1.2, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		Vec3 librarianSpot = librarian.add(-TradeSpot.SEAT_DISTANCE, 0, 0);
		TradeBotScenes.roll(scene, master, farmerSpot, List.of(librarianSpot), Direction.EAST.toYRot());
		TradeBotScenes.play(scene, master, "trade_villager");
		scene.overlay().showText(110)
			.text("Then he buys what the gauge wants, never above a villager's base price. A trade that needs a book as well takes one from his chests")
			.pointAt(librarian.add(0, 1.2, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		Vec3 atOutput = util.vector().of(3.5, 1, 3.9);
		TradeBotScenes.roll(scene, master, librarianSpot, List.of(new Vec3(librarianSpot.x, 1, 3.9), atOutput), Direction.SOUTH.toYRot());
		TradeBotScenes.play(scene, master, "chest_put");
		scene.overlay().showText(110)
			.text("He puts it all in the gauge's chest, the odd extra included, and takes whatever's left back where it came from")
			.pointAt(util.vector().topOf(gauge))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showText(110)
			.text("An Emerald gauge is filled by selling, and his spare emeralds go there. Nuggets of Experience go only to a chest with a Nugget of Experience gauge")
			.pointAt(util.vector().topOf(output))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showText(100)
			.text("Several Trade Masters can share a network: each gauge is filled by one at a time, and Create's gauges show what's on its way")
			.pointAt(atOutput.add(0, 1.4, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		TradeBotScenes.play(scene, master, "expr_happy");
		scene.idle(40);
		scene.markAsFinished();
	}
}
