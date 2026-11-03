package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.jetpack.JetpackWindingStationBlockEntity;
import com.comedymonkey.createaddons.content.jetpack.JetpackWindingStationBlockEntity.Boom;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * The Jetpack Winding Station (jetpack spec D45): turned from a shaft below, it winds a Clockwork Jetpack hung on it, or
 * the one on the back of a player who backs into it; the faster the shaft, the faster it winds; its comparator and
 * funnels read and take a full one; and with an empty hand it swaps the jetpack on your back with the station.
 */
public final class JetpackScenes {
	private JetpackScenes() {}

	public static void windingStation(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("jetpack_winding_station", "Winding a Clockwork Jetpack");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(10);

		BlockPos motor = util.grid().at(2, 1, 3), station = util.grid().at(2, 2, 3), front = util.grid().at(2, 1, 2);
		scene.world().showSection(util.select().position(motor), Direction.DOWN);
		scene.world().showSection(util.select().position(station), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("The Jetpack Winding Station is where a Clockwork Jetpack is wound. It turns from a shaft below")
			.pointAt(util.vector().topOf(station))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// hang one on: the boom reaches out, lowers and turns the key; the coil tightens in the porthole
		scene.overlay().showControls(util.vector().blockSurface(station, Direction.NORTH), Pointing.DOWN, 40)
			.rightClick().withItem(new ItemStack(ModItems.CLOCKWORK_JETPACK.get()));
		scene.idle(10);
		scene.world().modifyBlockEntity(station, JetpackWindingStationBlockEntity.class, be -> {
			be.hang(new ItemStack(ModItems.CLOCKWORK_JETPACK.get()));
			be.showWinding(Boom.PARKED, 0);
		});
		scene.idle(20);
		scene.world().setKineticSpeed(util.select().fromTo(motor, station), 64);
		scene.world().modifyBlockEntity(station, JetpackWindingStationBlockEntity.class, be -> be.showWinding(Boom.HUNG, 0));
		scene.overlay().showText(100)
			.text("Hang a jetpack on it. Its boom reaches out, fits its socket over the key and winds it, as fast as a Copper Backtank fills")
			.pointAt(util.vector().blockSurface(station, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		for (int i = 1; i <= 10; i++) {
			float fill = i / 20f;
			scene.world().modifyBlockEntity(station, JetpackWindingStationBlockEntity.class, be -> be.showWinding(Boom.HUNG, fill));
			scene.idle(6);
		}
		// the shaft's speed sets the rate
		scene.world().setKineticSpeed(util.select().fromTo(motor, station), 256);
		scene.effects().rotationSpeedIndicator(motor);
		scene.overlay().showText(90)
			.text("The faster the shaft turns, the faster it winds: about nine seconds at 256 RPM, about ten minutes on a Hand Crank")
			.pointAt(util.vector().blockSurface(motor, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		for (int i = 11; i <= 20; i++) {
			float fill = i / 20f;
			scene.world().modifyBlockEntity(station, JetpackWindingStationBlockEntity.class, be -> be.showWinding(Boom.HUNG, fill));
			scene.idle(3);
		}
		scene.idle(60);
		scene.world().modifyBlockEntity(station, JetpackWindingStationBlockEntity.class, be -> be.showWinding(Boom.PARKED, 1));
		scene.overlay().showText(90)
			.text("Full, the boom parks. A comparator reads how wound it is, and funnels or arms can take it off once it's full")
			.pointAt(util.vector().topOf(station))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// or back in wearing one
		scene.world().modifyBlockEntity(station, JetpackWindingStationBlockEntity.class, be -> {
			be.takeDown();
			be.showWinding(Boom.PARKED, 0);
		});
		scene.idle(10);
		scene.overlay().showText(110)
			.text("Or back into it wearing one: stand on the block in front with your back to it and sneak. It winds the jetpack on your back until you step away")
			.pointAt(util.vector().topOf(front))
			.placeNearTarget()
			.attachKeyFrame();
		scene.world().modifyBlockEntity(station, JetpackWindingStationBlockEntity.class, be -> be.showWinding(Boom.WORN, 0));
		scene.idle(120);
		scene.world().modifyBlockEntity(station, JetpackWindingStationBlockEntity.class, be -> be.showWinding(Boom.PARKED, 0));
		scene.idle(20);

		// or swap it with an empty hand
		scene.overlay().showControls(util.vector().blockSurface(station, Direction.NORTH), Pointing.DOWN, 40).rightClick();
		scene.idle(10);
		scene.world().modifyBlockEntity(station, JetpackWindingStationBlockEntity.class, be -> {
			be.hang(new ItemStack(ModItems.CLOCKWORK_JETPACK.get()));
			be.showWinding(Boom.PARKED, 0);
		});
		scene.overlay().showText(100)
			.text("With an empty hand, right-click it while wearing a jetpack to take yours off and hang it here. Right-click again to put it back on")
			.pointAt(util.vector().blockSurface(station, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		scene.markAsFinished();
	}
}
