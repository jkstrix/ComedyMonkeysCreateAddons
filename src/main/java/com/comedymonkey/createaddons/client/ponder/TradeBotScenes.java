package com.comedymonkey.createaddons.client.ponder;

import java.util.List;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion;
import com.comedymonkey.createaddons.content.tradebot.trade.TradeSpot;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Trade Bot's Ponder scenes (D40): his station and its power, linking his containers, and trading. He plays his real
 * clips, on his own clock ({@link TradeBotEntity#setPonder}), and is moved by the scene where the server would move
 * him. The structures come from design/ponder/trade-bot.ponder.js; the visible text from
 * {@code comedymonkeysaddons.ponder.trade_bot*.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class TradeBotScenes {
	private TradeBotScenes() {}

	private static ItemStack create(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", id)));
	}

	/** Trade Bot docked in his station at {@code station}. */
	private static ElementLink<EntityElement> dockedBot(CreateSceneBuilder scene, BlockPos station, Direction facing) {
		return scene.world().createEntity(level -> {
			TradeBotEntity bot = ModEntities.TRADE_BOT.get().create(level);
			bot.setPonder();
			bot.dockAt(station, facing);
			return bot;
		});
	}

	private static void play(CreateSceneBuilder scene, ElementLink<EntityElement> bot, String clip) {
		scene.world().modifyEntity(bot, e -> ((TradeBotEntity) e).playClip(clip));
	}

	/** Lets a physical clip (hopping out of the station) finish, then puts him where it ended, as the server would. */
	private static void playPhysical(CreateSceneBuilder scene, ElementLink<EntityElement> bot, String name) {
		play(scene, bot, name);
		Clip clip = ClipLibrary.get().clip(name);
		scene.idle((int) Math.ceil(clip.length() * 20));
		scene.world().modifyEntity(bot, e -> {
			TradeBotEntity tradeBot = (TradeBotEntity) e;
			Vec3 end = tradeBot.rootPosition(clip, clip.length());
			float yaw = tradeBot.rootYaw(clip, clip.length());
			tradeBot.playClip(TradeBotEntity.IDLE);
			tradeBot.setPos(end.x, end.y, end.z);
			tradeBot.setYRot(yaw);
		});
	}

	/** Rolls him through {@code points} at his cruising speed, turning toward each, and stops facing {@code yaw}. */
	private static void roll(CreateSceneBuilder scene, ElementLink<EntityElement> bot, Vec3 from, List<Vec3> points, float yaw) {
		play(scene, bot, "start_rolling");
		Vec3 at = from;
		boolean rolling = false;
		int ticks = 0;
		for (Vec3 point : points) {
			while (at.distanceTo(point) > 1e-3) {
				Vec3 step = point.subtract(at);
				double length = step.length();
				Vec3 next = length <= TradeBotMotion.CRUISE ? point : at.add(step.scale(TradeBotMotion.CRUISE / length));
				float facing = TradeBotMotion.yawTo(at, point);
				Vec3 to = next;
				scene.world().modifyEntity(bot, e -> {
					e.setPos(to.x, to.y, to.z);
					e.setYRot(facing);
				});
				scene.idle(1);
				at = next;
				if (!rolling && ++ticks >= 26) { // start_rolling is 1.3 s
					play(scene, bot, "rolling");
					rolling = true;
				}
			}
		}
		play(scene, bot, "stop_rolling");
		scene.world().modifyEntity(bot, e -> e.setYRot(yaw));
		scene.idle(32);
	}

	// ---- 1. his station and its power

	public static void station(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("trade_bot", "Trade Bot and His Winding Station");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos station = util.grid().at(2, 1, 2);
		scene.world().showSection(util.select().position(station), Direction.DOWN);
		ElementLink<EntityElement> bot = dockedBot(scene, station, Direction.NORTH);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("Trade Bot comes docked in his winding station: place them as one. He's yours, and only you and your team can give him jobs")
			.pointAt(util.vector().topOf(station).add(0, 0.6, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(90)
			.text("Feed Forge Energy in from underneath, through a cable or a generator that outputs upward. The station stores 100,000 FE")
			.pointAt(util.vector().blockSurface(station, Direction.DOWN))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().modifyEntity(bot, e -> ((TradeBotEntity) e).setCharging(true));
		scene.overlay().showText(90)
			.text("While he's docked it winds him up, 64 FE a tick. A full charge of 100,000 FE carries him around 25,000 blocks")
			.pointAt(util.vector().topOf(station).add(0, 0.9, 0.3))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		BlockPos lever = util.grid().at(1, 1, 2), comparator = util.grid().at(3, 1, 2), wire = util.grid().at(4, 1, 2);
		scene.world().showSection(util.select().position(lever), Direction.DOWN);
		scene.idle(10);
		scene.world().modifyBlock(lever, s -> s.setValue(LeverBlock.POWERED, true), false);
		scene.overlay().showText(70)
			.text("A redstone signal keeps him at home")
			.pointAt(util.vector().centerOf(lever))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);
		scene.world().modifyBlock(lever, s -> s.setValue(LeverBlock.POWERED, false), false);
		scene.idle(10);

		scene.world().showSection(util.select().fromTo(comparator, wire), Direction.DOWN);
		scene.idle(10);
		scene.world().modifyBlock(comparator, s -> s.setValue(ComparatorBlock.POWERED, true), false);
		scene.world().modifyBlock(wire, s -> s.setValue(RedStoneWireBlock.POWER, 5), false);
		scene.overlay().showText(110)
			.text("A comparator reads 5 while he's home, 10 while he's out working, and 15 when he needs you: low on charge, or run down")
			.pointAt(util.vector().centerOf(comparator))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showControls(util.vector().topOf(station), Pointing.DOWN, 60)
			.withItem(create("goggles"));
		scene.overlay().showText(90)
			.text("Engineer's Goggles on the station show his charge, what he's doing, his containers and today's trades")
			.pointAt(util.vector().topOf(station))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().blockSurface(station, Direction.NORTH), Pointing.DOWN, 60)
			.rightClick()
			.whileSneaking()
			.withItem(create("wrench"));
		scene.overlay().showText(100)
			.text("Sneak-click the station with a Wrench to pack him back into one item, jobs and all. Nothing else can break it")
			.pointAt(util.vector().blockSurface(station, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		scene.markAsFinished();
	}

	// ---- 2. linking his containers

	public static void linking(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("trade_bot_linking", "Giving Trade Bot His Containers");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos station = util.grid().at(3, 1, 5);
		BlockPos left = util.grid().at(1, 1, 2), middle = util.grid().at(3, 1, 1), barrel = util.grid().at(5, 1, 2);
		scene.world().showSection(util.select().position(station), Direction.DOWN);
		dockedBot(scene, station, Direction.NORTH);
		scene.idle(10);
		scene.world().showSection(util.select().position(left).add(util.select().position(middle)).add(util.select().position(barrel)), Direction.DOWN);
		scene.idle(15);

		scene.overlay().showControls(util.vector().topOf(station), Pointing.DOWN, 60).rightClick();
		scene.overlay().showText(100)
			.text("Right-click his station for its screen: his charge, his containers, and buttons to Select containers or Call him home")
			.pointAt(util.vector().topOf(station))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showText(100)
			.text("Select containers, then right-click containers out in the world. The first click makes one an input, that he takes goods from")
			.pointAt(util.vector().topOf(left))
			.placeNearTarget()
			.attachKeyFrame();
		for (BlockPos input : List.of(left, middle, barrel)) {
			scene.overlay().showControls(util.vector().topOf(input), Pointing.DOWN, 20).rightClick();
			scene.idle(15);
			scene.overlay().chaseBoundingBoxOutline(PonderPalette.INPUT, input, new AABB(input), 300);
			scene.idle(15);
		}
		scene.idle(30);

		scene.overlay().showControls(util.vector().topOf(barrel), Pointing.DOWN, 20).rightClick();
		scene.idle(15);
		scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, barrel, new AABB(barrel), 240);
		scene.overlay().showText(90)
			.text("Click one again to make it his output, if he hasn't got one yet")
			.pointAt(util.vector().topOf(barrel))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(110)
			.text("He takes up to 4 inputs and 1 output, within 64 blocks. Left-click one to unlink it; sneak, or click his station, when you're done. A belt can only be an output")
			.pointAt(util.vector().topOf(middle))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showText(110)
			.text("His output only ever gets his earnings: emeralds, and Nuggets of Experience for the trades' XP. Whatever doesn't sell goes back where it came from")
			.pointAt(util.vector().topOf(barrel))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);
		scene.markAsFinished();
	}

	// ---- 3. trading

	public static void trading(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("trade_bot_trading", "Trading with Villagers");
		scene.configureBasePlate(0, 0, 9);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos station = util.grid().at(1, 1, 4), seat = util.grid().at(7, 1, 1), table = util.grid().at(6, 1, 1);
		scene.world().showSection(util.select().fromTo(1, 1, 4, 3, 1, 6), Direction.DOWN);
		ElementLink<EntityElement> bot = dockedBot(scene, station, Direction.EAST);
		scene.idle(10);
		scene.world().showSection(util.select().fromTo(6, 1, 1, 7, 1, 1), Direction.DOWN);
		scene.idle(10);
		Vec3 seatAt = util.vector().of(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5);
		scene.world().createEntity(level -> {
			SeatEntity chair = new SeatEntity(level);
			chair.setPos(seatAt.x, seatAt.y, seatAt.z);
			level.addFreshEntity(chair);
			Villager fletcher = EntityType.VILLAGER.create(level);
			fletcher.setVillagerData(fletcher.getVillagerData().setProfession(VillagerProfession.FLETCHER));
			fletcher.setPos(seatAt.x, seatAt.y, seatAt.z);
			fletcher.startRiding(chair, true);
			chair.positionRider(fletcher);
			fletcher.setYRot(90);
			fletcher.yBodyRot = fletcher.yHeadRot = 90;
			return fletcher;
		});
		scene.idle(15);
		scene.overlay().showText(100)
			.text("He sells to villagers sitting on Create Seats, and to wandering traders: any that buy what's in his inputs, within 64 blocks of his station")
			.pointAt(seatAt.add(0, 1.2, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showText(90)
			.text("When there's a sale to make he sets off by himself, by day, taking only what they'll buy")
			.pointAt(util.vector().topOf(station).add(0, 0.6, 0))
			.placeNearTarget()
			.attachKeyFrame();
		playPhysical(scene, bot, "action_comes_in");

		// he trades from two blocks out, across the workstation, where his clip puts the package in its arms
		Vec3 spot = seatAt.add(-TradeSpot.SEAT_DISTANCE, 0, 0);
		Vec3 out = util.vector().of(1.5 + 31 / 16.0, 1, 4.5);
		roll(scene, bot, out, List.of(new Vec3(spot.x, 1, 4.5), spot), Direction.EAST.toYRot());

		scene.overlay().showText(80)
			.text("He trades from two blocks away, across the villager's workstation")
			.pointAt(util.vector().topOf(table))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		play(scene, bot, "trade_villager");
		scene.idle(30);
		scene.overlay().showText(110)
			.text("In one go he makes every trade he can pay for, at the villager's real prices and stock. Each trade makes him a better price next time")
			.pointAt(seatAt.add(0, 1.2, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showText(100)
			.text("Villagers earn XP and level up as usual, and any new trades that opens up he sells on the same trip")
			.pointAt(seatAt.add(0, 1.2, 0))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(100)
			.text("Then he delivers his emeralds, and Nuggets of Experience for the trades' XP, puts back what didn't sell, and docks")
			.pointAt(util.vector().topOf(util.grid().at(3, 1, 6)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		play(scene, bot, TradeBotEntity.WIND_DOWN);
		scene.idle(20);
		scene.overlay().showText(100)
			.text("Hostile mobs frighten him, and with no health left he winds down. He never dies: wind him back up with a Wrench")
			.pointAt(spot.add(0, 1.2, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(70);
		scene.overlay().showControls(spot.add(0, 1.6, 0), Pointing.DOWN, 40)
			.rightClick()
			.withItem(create("wrench"));
		scene.idle(20);
		play(scene, bot, TradeBotEntity.REWIND);
		scene.idle(70);
		scene.markAsFinished();
	}
}
