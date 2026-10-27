package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.client.helperbot.HelperBotAnimator.Mode;
import com.comedymonkey.createaddons.client.helperbot.HelperBotClient;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Helper Bot's Ponder scenes (spec D26): driving a train, and keeping shop. In Ponder he's a placed block, so the scenes
 * tell him what he's doing ({@link HelperBotClient#ponder}) and he plays his real clips. The structures come from
 * design/ponder/helper-bot.ponder.js; the visible text from {@code comedymonkeysaddons.ponder.helper_bot_*.*} in
 * en_us.json, numbered in the order the texts appear below.
 */
public final class HelperBotScenes {
	private HelperBotScenes() {}

	private static ItemStack create(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", id)));
	}

	private static void act(CreateSceneBuilder scene, BlockPos bot, Mode mode, boolean schedule, String clip) {
		scene.world().modifyBlockEntity(bot, HelperBotBlockEntity.class, be -> HelperBotClient.ponder(be, mode, schedule, clip));
	}

	// ---- 1. driving a train

	public static void driving(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("helper_bot_driving", "Helper Bot Driving a Train");
		scene.configureBasePlate(0, 0, 9);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos station = util.grid().at(7, 1, 3), controls = util.grid().at(7, 4, 2), bot = util.grid().at(6, 4, 2);
		scene.world().showSection(util.select().fromTo(0, 1, 2, 8, 1, 3), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().fromTo(1, 2, 2, 7, 3, 2), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(controls), Direction.DOWN);
		scene.idle(15);

		scene.world().showSection(util.select().position(bot), Direction.DOWN);
		act(scene, bot, Mode.DRIVING, false, "no_schedule");
		scene.overlay().showText(90)
			.text("Helper Bot drives trains just like a Blaze Burner: place him directly behind the Train Controls, with the controls facing him")
			.pointAt(util.vector().topOf(bot).add(0, 0.5, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showOutline(PonderPalette.GREEN, "station", util.select().position(station), 60);
		scene.overlay().showText(70)
			.text("Assemble the train at a station as usual")
			.pointAt(util.vector().topOf(station))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		scene.overlay().showControls(util.vector().topOf(bot).add(0, 0.5, 0), Pointing.DOWN, 50)
			.rightClick()
			.withItem(create("schedule"));
		act(scene, bot, Mode.DRIVING, true, "schedule_given");
		scene.overlay().showText(80)
			.text("Then use a Schedule on him. On goes his cap, and he takes charge")
			.pointAt(util.vector().topOf(bot).add(0, 0.5, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		act(scene, bot, Mode.DRIVING, true, "depart");
		scene.overlay().showText(100)
			.text("He works the levers himself: he toots his whistle as the train leaves, brakes into stations and checks his watch while it waits")
			.pointAt(util.vector().topOf(controls))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(70);
		act(scene, bot, Mode.DRIVING, true, "station_wait");
		scene.idle(40);

		scene.overlay().showControls(util.vector().topOf(bot).add(0, 0.5, 0), Pointing.DOWN, 50).rightClick();
		act(scene, bot, Mode.DRIVING, false, "schedule_taken");
		scene.overlay().showText(90)
			.text("An empty hand takes the schedule back. If you drive the train yourself, he waits with his arms folded until you hand it back to him")
			.pointAt(util.vector().topOf(bot).add(0, 0.5, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(60);
		act(scene, bot, Mode.DRIVING, true, "manual_folded");
		scene.idle(50);

		scene.overlay().showText(90)
			.text("One Helper Bot drives each way: put another behind controls at the other end for a two-way train. Any others just ride along")
			.pointAt(util.vector().topOf(bot))
			.placeNearTarget()
			.attachKeyFrame();
		act(scene, bot, Mode.PASSENGER, false, "passenger");
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(bot).add(0, 0.5, 0), Pointing.DOWN, 50).withItem(new ItemStack(Items.RED_DYE));
		scene.overlay().showText(90)
			.text("Craft him with one dye to paint him. Bulk washing makes him plain again")
			.pointAt(util.vector().topOf(bot).add(0, 0.5, 0))
			.placeNearTarget()
			.attachKeyFrame();
		act(scene, bot, Mode.OFF, false, "wave");
		scene.idle(100);
		scene.markAsFinished();
	}

	// ---- 2. keeping shop

	public static void shop(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("helper_bot_shop", "Helper Bot Keeping Shop");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos ticker = util.grid().at(3, 1, 2), bot = util.grid().at(4, 1, 2);
		scene.world().showSection(util.select().fromTo(1, 1, 2, 1, 2, 3), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(ticker), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showText(70)
			.text("A Stock Ticker sells what its logistics network holds, but it needs someone to keep shop")
			.pointAt(util.vector().topOf(ticker))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		scene.world().showSection(util.select().position(bot), Direction.DOWN);
		act(scene, bot, Mode.SHOP, false, "shop_greet");
		scene.overlay().showText(90)
			.text("Put a Helper Bot beside it, on the same level, and he keeps shop for it like a Blaze Burner would")
			.pointAt(util.vector().topOf(bot).add(0, 0.5, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().blockSurface(bot, Direction.NORTH), Pointing.DOWN, 50).rightClick();
		scene.overlay().showText(80)
			.text("Use him to see the stock and place an order, as you would a Blaze Burner keeping shop")
			.pointAt(util.vector().blockSurface(bot, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		act(scene, bot, Mode.SHOP, false, "shop_order_nod");
		scene.overlay().showText(70)
			.text("He nods for every order that goes out")
			.pointAt(util.vector().topOf(bot).add(0, 0.5, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);
		scene.markAsFinished();
	}
}
