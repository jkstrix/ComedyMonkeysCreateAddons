package com.comedymonkey.createaddons.compat.sable.client;

import com.comedymonkey.createaddons.compat.sable.AeronauticsContent;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Ponder scenes for the Compact Gyroscope and Torque Rotor, with a little ship tilted and turned to show what they
 * do. Only registered when Sable is installed.
 */
public final class AeronauticsScenes {
	private AeronauticsScenes() {}

	public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		helper.forComponents(AeronauticsContent.COMPACT_GYROSCOPE.getId())
			.addStoryBoard("compact_gyroscope", AeronauticsScenes::gyroscope);
		helper.forComponents(AeronauticsContent.TORQUE_ROTOR.getId())
			.addStoryBoard("torque_rotor", AeronauticsScenes::rotor);
		helper.forComponents(AeronauticsContent.FLIGHT_INSTRUMENTS.getId())
			.addStoryBoard("flight_instruments", AeronauticsScenes::instruments);
	}

	public static void gyroscope(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("compact_gyroscope", "Keeping Things Level");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos gyro = util.grid().at(2, 2, 2);
		ElementLink<WorldSectionElement> ship =
			scene.world().showIndependentSection(util.select().fromTo(1, 1, 1, 3, 2, 3), Direction.DOWN);
		scene.world().configureCenterOfRotation(ship, util.vector().centerOf(2, 1, 2));
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Compact Gyroscope: a whole Gyroscope Stabilizer, squeezed into one framed glass block")
			.pointAt(util.vector().blockSurface(gyro, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		ElementLink<WorldSectionElement> motor =
			scene.world().showIndependentSection(util.select().position(2, 3, 2), Direction.DOWN);
		scene.world().configureCenterOfRotation(motor, util.vector().centerOf(2, 1, 2));
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().fromTo(2, 2, 2, 2, 3, 2), 256);
		scene.overlay().showText(80)
			.text("Spin it with a shaft from above or below. The faster it spins, the harder it works, up to 256 RPM")
			.pointAt(util.vector().topOf(util.grid().at(2, 3, 2)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// Something knocks the ship over...
		scene.world().rotateSection(ship, 0, 0, 20, 12);
		scene.world().rotateSection(motor, 0, 0, 20, 12);
		scene.idle(15);
		scene.overlay().showText(70)
			.text("On an Aeronautics ship, whenever something tips it over...")
			.pointAt(util.vector().blockSurface(gyro, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(50);
		// ...and it rights itself, overshooting a touch before settling
		scene.world().rotateSection(ship, 0, 0, -24, 30);
		scene.world().rotateSection(motor, 0, 0, -24, 30);
		scene.idle(32);
		scene.world().rotateSection(ship, 0, 0, 4, 20);
		scene.world().rotateSection(motor, 0, 0, 4, 20);
		scene.overlay().showText(80)
			.text("...it pushes back upright and calms the wobble until the ship sits level again")
			.pointAt(util.vector().blockSurface(gyro, Direction.SOUTH))
			.placeNearTarget();
		scene.idle(90);

		scene.overlay().showControls(util.vector().topOf(util.grid().at(2, 3, 2)), Pointing.DOWN, 60)
			.withItem(new ItemStack(AeronauticsContent.COMPACT_GYROSCOPE_ITEM.get()));
		scene.overlay().showText(90)
			.text("Craft it from a Flywheel, a Precision Mechanism, a shaft, Brass Casing and Framed Glass. Have Gyroscope Stabilizers? Its Gyroscope converts straight into one")
			.pointAt(util.vector().blockSurface(gyro, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void instruments(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("flight_instruments", "Know Where You Are");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);
		BlockPos panel = util.grid().at(2, 1, 2);
		scene.world().showSection(util.select().position(panel), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("Flight Instruments read the ship they're on: altitude, airspeed, attitude and heading, each on its own dial")
			.pointAt(util.vector().blockSurface(panel, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.overlay().showText(80)
			.text("Place it facing you in the cockpit: the way you face the panel is taken as the ship's forward")
			.pointAt(util.vector().topOf(panel))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.overlay().showText(90)
			.text("Engineer's Goggles show every reading, and a Display Link can put any of them on a Display Board or Nixie Tubes")
			.pointAt(util.vector().blockSurface(panel, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void rotor(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("torque_rotor", "Turn Without the Drift");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos rotor = util.grid().at(5, 3, 3);
		BlockPos motorPos = util.grid().at(5, 3, 2);
		ElementLink<WorldSectionElement> heli =
			scene.world().showIndependentSection(util.select().fromTo(1, 2, 2, 5, 3, 4), Direction.DOWN);
		// The cabin is the heavy end, so that's where it turns about
		scene.world().configureCenterOfRotation(heli, util.vector().of(2.5, 2.5, 3.5));
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Torque Rotor looks like a propeller, but it turns a ship instead of pushing it along")
			.pointAt(util.vector().blockSurface(rotor, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().setKineticSpeed(util.select().fromTo(5, 3, 2, 5, 3, 3), 128);
		scene.overlay().showText(80)
			.text("It mounts on the face you click, with the shaft going in through its base")
			.pointAt(util.vector().blockSurface(motorPos, Direction.UP))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showText(90)
			.text("Its blades push the way they face, swinging the ship round its centre of mass just as a propeller there would...")
			.pointAt(util.vector().blockSurface(rotor, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.world().rotateSection(heli, 0, 35, 0, 70);
		scene.idle(100);

		scene.overlay().showText(80)
			.text("...but without dragging the ship sideways. Just what a helicopter's tail needs")
			.pointAt(util.vector().centerOf(util.grid().at(2, 2, 3)))
			.placeNearTarget();
		scene.idle(90);

		scene.world().setKineticSpeed(util.select().fromTo(5, 3, 2, 5, 3, 3), -128);
		scene.world().rotateSection(heli, 0, -35, 0, 70);
		scene.overlay().showText(90)
			.text("Reverse the shaft to turn the other way. Mounted further from the middle, it turns harder")
			.pointAt(util.vector().blockSurface(rotor, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}
}
