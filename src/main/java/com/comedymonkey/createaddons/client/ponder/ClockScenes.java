package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.clock.ClockTime;
import com.comedymonkey.createaddons.content.clock.CuckooClockBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scene for the Cuckoo Clock: it hangs on a planked wall facing the camera (north), with a shaft through the
 * wall from a motor behind it, and a fletcher at a fletching table in front. The clock runs its own time in Ponder
 * ({@link CuckooClockBlockEntity#setPonderTime}). The visible text comes from
 * {@code comedymonkeysaddons.ponder.cuckoo_clock.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class ClockScenes {
	private ClockScenes() {}

	private static ItemStack create(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", id)));
	}

	public static void cuckooClock(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("cuckoo_clock", "Keeping Village Time");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos clock = util.grid().at(3, 3, 3);
		Selection kinetics = util.select().fromTo(3, 3, 3, 3, 3, 5);
		// points on the clock, from its model: the dial, the day and night window, the balcony
		Vec3 dial = util.vector().of(3.5, 3.23, 3.42), window = util.vector().of(3.5, 3.5, 3.45);
		Vec3 balcony = util.vector().of(3.5, 2.85, 3.3);

		scene.world().showSection(util.select().fromTo(1, 1, 4, 5, 4, 4).substract(util.select().position(3, 3, 4)),
			Direction.DOWN);
		scene.idle(10);
		scene.world().modifyBlockEntity(clock, CuckooClockBlockEntity.class, be -> be.setPonderTime(3000, 0));
		scene.world().showSection(util.select().fromTo(3, 2, 3, 3, 3, 3), Direction.SOUTH);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Cuckoo Clock hangs on a wall, two blocks tall: the clock, with its pendulum and weights below")
			.pointAt(dial)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.rotateCameraY(150);
		scene.idle(20);
		scene.world().showSection(util.select().fromTo(3, 3, 4, 3, 3, 5), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(kinetics, 32);
		scene.world().modifyBlockEntity(clock, CuckooClockBlockEntity.class, be -> be.setPonderTime(3000, 2));
		scene.overlay().showText(90)
			.text("Its shaft goes into the middle of its back, usually through the wall. It always uses 1,024 SU by default, about a 16-sail windmill")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 3, 5), Direction.UP))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.rotateCameraY(-150);
		scene.idle(20);

		// a day's afternoon in a few seconds: the sun sinks behind the hump and the sky turns to dusk
		scene.world().modifyBlockEntity(clock, CuckooClockBlockEntity.class, be -> be.setPonderTime(8000, 50));
		scene.overlay().showText(100)
			.text("The hands keep the game's time, and the window shows the sun and moon crossing the sky")
			.pointAt(window)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.world().modifyBlockEntity(clock, CuckooClockBlockEntity.class, be -> be.setPonderTime(1700, 2));
		scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
		scene.idle(5);
		Vec3 fletcherAt = util.vector().of(2.4, 1, 1.5);
		scene.world().createEntity(level -> {
			Villager fletcher = EntityType.VILLAGER.create(level);
			fletcher.setVillagerData(fletcher.getVillagerData().setProfession(VillagerProfession.FLETCHER));
			fletcher.setPos(fletcherAt.x, fletcherAt.y, fletcherAt.z);
			fletcher.setYRot(90);
			fletcher.yBodyRot = fletcher.yHeadRot = 90;
			return fletcher;
		});
		scene.idle(15);
		scene.overlay().showControls(fletcherAt.add(0, 2.3, 0), Pointing.DOWN, 60)
			.withItem(new ItemStack(Items.EMERALD));
		scene.overlay().showText(110)
			.text("While it turns, villagers within 64 blocks (by default) restock at 8:00 and 12:00, on their next trip to their workstation")
			.pointAt(fletcherAt.add(0, 1.5, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		int calls = 8;
		scene.world().modifyBlockEntity(clock, CuckooClockBlockEntity.class, be -> {
			be.setPonderTime(ClockTime.MORNING, 1);
			be.startPonderShow(calls, Blocks.FLETCHING_TABLE.defaultBlockState());
		});
		scene.idle(30);
		scene.overlay().showText(110)
			.text("As the hour comes round it chimes, a carved villager rolls out with a workstation from nearby, and the cuckoo calls the hour")
			.pointAt(balcony)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(ClockTime.showLength(calls) - 20);

		scene.overlay().showControls(dial, Pointing.DOWN, 60)
			.withItem(create("goggles"));
		scene.overlay().showText(90)
			.text("Engineer's Goggles show the time, when the next restock is, and how many villagers it reaches")
			.pointAt(dial)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}
}
