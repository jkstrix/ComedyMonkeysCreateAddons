package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.juggernaut.ClockworkJuggernaut;
import com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlockEntity;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** The Clockwork Juggernaut's ritual (juggernaut_ritual.nbt): the effigy, the Winding Key, winding it, and his waking. */
public final class JuggernautScenes {
	private JuggernautScenes() {}

	public static void ritual(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("juggernaut_ritual", "Waking the Clockwork Juggernaut");
		scene.configureBasePlate(0, 0, 9);
		scene.scaleSceneView(0.6f);
		scene.showBasePlate();
		scene.idle(10);

		BlockPos foot = util.grid().at(4, 1, 6), centre = foot.above();
		BlockPos key = util.grid().at(4, 2, 7), crank = util.grid().at(4, 2, 8);
		Selection effigy = util.select().position(foot).add(util.select().fromTo(3, 2, 6, 5, 2, 6));
		scene.world().showSection(effigy, Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("Four Brass Blocks in a T, standing up, make the effigy")
			.pointAt(util.vector().blockSurface(centre, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().position(key), Direction.NORTH);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("Fit a Winding Key to the back of the T's middle block")
			.pointAt(util.vector().topOf(key))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// wound by hand: the key turns, and its ring of runes lights once it has some wind in it
		scene.world().showSection(util.select().position(crank), Direction.NORTH);
		scene.idle(10);
		Selection turning = util.select().position(key).add(util.select().position(crank));
		scene.world().setKineticSpeed(turning, 32);
		scene.overlay().showControls(util.vector().topOf(crank), Pointing.DOWN, 40).rightClick();
		scene.overlay().showText(100)
			.text("Wind it with a Hand Crank for 20 seconds, or twice as fast with a shaft at 64 RPM or more. Its runes light as it winds")
			.pointAt(util.vector().topOf(key))
			.placeNearTarget()
			.attachKeyFrame();
		for (int i = 1; i <= 25; i++) {
			float wind = i * 0.02f;
			scene.world().modifyBlockEntity(key, WindingKeyBlockEntity.class, be -> be.setWind(wind));
			scene.idle(4);
		}

		// let go: it slowly unwinds
		scene.world().setKineticSpeed(turning, 0);
		scene.overlay().showText(70)
			.text("Let go before it's done and it slowly unwinds")
			.pointAt(util.vector().topOf(key))
			.placeNearTarget()
			.attachKeyFrame();
		for (int i = 1; i <= 10; i++) {
			float wind = 0.5f - i * 0.01f;
			scene.world().modifyBlockEntity(key, WindingKeyBlockEntity.class, be -> be.setWind(wind));
			scene.idle(7);
		}

		// wound in full: the effigy and the key are gone, the crank pops off, and he stands in front of where it stood
		scene.world().setKineticSpeed(turning, 32);
		for (int i = 1; i <= 15; i++) {
			float wind = 0.4f + i * 0.04f;
			scene.world().modifyBlockEntity(key, WindingKeyBlockEntity.class, be -> be.setWind(Math.min(1, wind)));
			scene.idle(3);
		}
		scene.world().setKineticSpeed(turning, 0);
		for (BlockPos p : new BlockPos[] { crank, key, centre, centre.west(), centre.east(), foot })
			scene.world().destroyBlock(p);
		ElementLink<EntityElement> juggernaut = scene.world().createEntity(level -> {
			ClockworkJuggernaut j = ModEntities.CLOCKWORK_JUGGERNAUT.get().create(level);
			j.setPos(4.5, 1, 3.5);
			j.setYRot(180); // facing the camera, the way the T faced
			j.yBodyRot = j.yHeadRot = 180;
			j.playClip("wake_ritual");
			return j;
		});
		scene.overlay().showText(90)
			.text("Wound in full, the effigy is gone and the Clockwork Juggernaut stands in front of where it stood")
			.pointAt(util.vector().of(4.5, 6, 3.5))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(65);
		scene.world().modifyEntity(juggernaut, e -> ((ClockworkJuggernaut) e).playClip("idle"));
		scene.idle(35);

		scene.overlay().showText(100)
			.text("He needs open sky and room to stand. Summoned under a roof, he rises on the surface above, kneeling until someone comes near")
			.pointAt(util.vector().of(4.5, 7, 3.5))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		scene.overlay().showText(80)
			.text("The key never takes in the Nether, and in Peaceful he never wakes")
			.pointAt(util.vector().of(4.5, 7, 3.5))
			.placeNearTarget();
		scene.idle(90);
	}
}
