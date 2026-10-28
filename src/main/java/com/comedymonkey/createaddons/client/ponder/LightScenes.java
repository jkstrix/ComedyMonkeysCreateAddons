package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.light.LightBlock;
import com.comedymonkey.createaddons.content.light.LightBlockEntity;
import com.comedymonkey.createaddons.content.light.LightDye;
import com.comedymonkey.createaddons.content.light.LightKind;
import com.comedymonkey.createaddons.content.line.PowerLineBlockEntity;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Electric Lights' Ponder scenes (design/specs/electric-lights.md, P17): the basics (a feed, a Light Switch, a run,
 * dyeing, brightness), Lamp Posts, and the Floodlight with Festoon Lights. Their structures are written by
 * design/ponder/lights.ponder.js, every light dark; the Ponder world doesn't run lights, so the scenes light them.
 */
public final class LightScenes {
	private LightScenes() {}

	private static ItemStack wrench() {
		return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "wrench")));
	}

	/** Lights (or darkens) every light in the selection: its light level in its block, its lens from its block entity. */
	private static void lit(CreateSceneBuilder scene, Selection where, boolean on, int step) {
		scene.world().modifyBlocks(where, s -> s.hasProperty(LightBlock.LIGHT) ? s.setValue(LightBlock.LIGHT, on ? step + 1 : 0) : s, false);
		scene.world().modifyBlockEntityNBT(where, LightBlockEntity.class, nbt -> {
			nbt.putInt("Phase", on ? 1 : 0);
			nbt.putInt("Step", step);
		});
	}

	private static void dye(CreateSceneBuilder scene, Selection where, DyeColor dye) {
		scene.world().modifyBlocks(where, s -> s.hasProperty(LightBlock.COLOR) ? s.setValue(LightBlock.COLOR, LightDye.of(dye)) : s, false);
	}

	public static void basics(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("lights", "Electric Lights");
		scene.configureBasePlate(0, 0, 8);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos bollard = util.grid().at(6, 1, 2), lightSwitch = util.grid().at(3, 1, 2);
		Selection strips = util.select().fromTo(2, 1, 4, 6, 1, 4), lights = strips.add(util.select().position(bollard));
		scene.world().showSection(util.select().fromTo(1, 1, 2, 5, 1, 2), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(bollard), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("Electric Lights run on FE from a Power Line touching them, on the floor, the wall or the ceiling beside them")
			.pointAt(util.vector().topOf(bollard))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(40);
		lit(scene, util.select().position(bollard), true, 0);
		scene.idle(60);

		scene.overlay().showText(90)
			.text("A line on any other face of the block a light is fixed to feeds it too, so the cable can hide behind the wall or above the ceiling")
			.pointAt(util.vector().topOf(bollard))
			.placeNearTarget();
		scene.idle(100);

		scene.world().showSection(util.select().position(4, 1, 3), Direction.DOWN);
		scene.world().showSection(strips, Direction.DOWN);
		scene.idle(15);
		lit(scene, strips, true, 0);
		scene.overlay().showText(100)
			.text("Light Strips, Edge Lights and Festoons that join are one run: a line touching any piece lights all of it, and a run powers nothing else")
			.pointAt(util.vector().topOf(util.grid().at(5, 1, 4)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(lightSwitch), Pointing.DOWN, 40).rightClick();
		scene.world().modifyBlockEntityNBT(util.select().position(lightSwitch), PowerLineBlockEntity.class, nbt -> nbt.putBoolean("LeverOff", true));
		lit(scene, lights, false, 0);
		scene.overlay().showText(80)
			.text("A Light Switch on the line turns off every light beyond it, at once")
			.pointAt(util.vector().topOf(lightSwitch))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.world().modifyBlockEntityNBT(util.select().position(lightSwitch), PowerLineBlockEntity.class, nbt -> nbt.putBoolean("LeverOff", false));
		lit(scene, lights, true, 0);
		scene.idle(20);

		scene.overlay().showControls(util.vector().topOf(bollard), Pointing.DOWN, 50).rightClick().withItem(new ItemStack(Items.BLUE_DYE));
		dye(scene, util.select().position(bollard), DyeColor.BLUE);
		scene.idle(20);
		scene.overlay().showControls(util.vector().topOf(util.grid().at(3, 1, 4)), Pointing.DOWN, 50).rightClick().withItem(new ItemStack(Items.LIME_DYE));
		dye(scene, strips, DyeColor.LIME);
		scene.overlay().showText(100)
			.text("Right-click a light with a dye to colour its glow; the dye isn't used. A run takes it all along (sneak for one block), and a water bucket washes it")
			.pointAt(util.vector().topOf(util.grid().at(3, 1, 4)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(bollard), Pointing.DOWN, 50).rightClick().withItem(wrench());
		lit(scene, util.select().position(bollard), true, 2);
		scene.overlay().showText(80)
			.text("The Wrench steps its brightness: Full, Bright, Dim, Glow. It draws less as it dims")
			.pointAt(util.vector().topOf(bollard))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.markAsFinished();
	}

	public static void lampPosts(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("lamp_posts", "Lamp Posts");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos foot = util.grid().at(3, 1, 2), head = util.grid().at(3, 3, 2), top = head.above();
		Selection heads = util.select().position(top).add(util.select().position(head.west())).add(util.select().position(head.east()));
		for (int y = 1; y <= 3; y++) {
			scene.world().showSection(util.select().position(foot.atY(y)), Direction.DOWN);
			scene.idle(8);
		}
		scene.overlay().showText(80)
			.text("Stack Lamp Posts into a pole of any height, in Cast Iron or Steel")
			.pointAt(util.vector().blockSurface(head, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().position(top), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showControls(util.vector().blockSurface(head, Direction.EAST), Pointing.RIGHT, 50).rightClick()
			.withItem(new ItemStack(ModItems.LIGHTS.get(LightKind.GLOBE_HEAD).get()));
		scene.world().showSection(util.select().position(head.west()).add(util.select().position(head.east())), Direction.DOWN);
		scene.overlay().showText(100)
			.text("Put a lamp head on top, and click a side of the top post with a head to hang it on an arm: up to four")
			.pointAt(util.vector().topOf(top))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.world().showSection(util.select().fromTo(1, 1, 2, 2, 1, 2), Direction.DOWN);
		scene.idle(15);
		lit(scene, heads, true, 0);
		scene.overlay().showText(90)
			.text("Run a Power Line to the foot of the pole and every lamp on it lights. Tie Power Line spans to the insulator under the top")
			.pointAt(util.vector().topOf(util.grid().at(2, 1, 2)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(90)
			.text("Put a Breaker in the line with a Daylight Detector beside it, and the street comes on at dusk")
			.pointAt(util.vector().topOf(util.grid().at(2, 1, 2)))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void floodlightFestoons(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("floodlight_festoons", "Floodlights and Festoon Lights");
		scene.configureBasePlate(0, 0, 11);
		scene.scaleSceneView(0.7f);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos flood = util.grid().at(3, 1, 3), target = util.grid().at(9, 1, 5);
		scene.world().showSection(util.select().fromTo(1, 1, 3, 3, 1, 3), Direction.DOWN);
		scene.world().showSection(util.select().position(target), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("A Floodlight lights a spot far off. Wrench it, then click the block to light: up to 32 blocks away, in clear sight")
			.pointAt(util.vector().topOf(flood))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(50);
		scene.overlay().showControls(util.vector().topOf(flood), Pointing.DOWN, 30).rightClick().withItem(wrench());
		scene.idle(35);
		scene.overlay().showControls(util.vector().blockSurface(target, Direction.WEST), Pointing.LEFT, 30).rightClick().withItem(wrench());
		scene.world().modifyBlockEntityNBT(util.select().position(flood), LightBlockEntity.class, nbt -> nbt.putLong("Aim", target.asLong()));
		lit(scene, util.select().position(flood), true, 0);
		scene.idle(50);
		scene.overlay().showText(100)
			.text("It swings round and lights the block's face, with a faint beam. Put a block in the way and the light lands on that instead")
			.pointAt(util.vector().blockSurface(target, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		Selection festoon = util.select().position(3, 3, 1).add(util.select().position(9, 3, 1));
		scene.world().showSection(util.select().fromTo(2, 1, 1, 2, 3, 1).add(util.select().position(2, 1, 2)), Direction.DOWN);
		scene.world().showSection(util.select().fromTo(10, 1, 1, 10, 3, 1), Direction.DOWN);
		scene.idle(15);
		scene.world().showSection(festoon, Direction.DOWN);
		scene.overlay().showText(110)
			.text("Festoon Lights hang between two posts or wall faces up to 16 blocks apart: click one end, then the other. Each covers 4 blocks of string")
			.pointAt(util.vector().blockSurface(util.grid().at(9, 3, 1), Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(70);
		lit(scene, festoon, true, 0);
		scene.idle(50);
		scene.overlay().showText(100)
			.text("Fed at either end, by a powered post or a wall with a line on it, a festoon lights only itself: it never carries power on")
			.pointAt(util.vector().topOf(util.grid().at(2, 1, 2)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showControls(util.vector().blockSurface(util.grid().at(9, 3, 1), Direction.WEST), Pointing.LEFT, 50).rightClick()
			.withItem(new ItemStack(Items.ORANGE_DYE));
		dye(scene, festoon, DyeColor.ORANGE);
		scene.overlay().showText(90)
			.text("Dye a hook to colour the whole string. Shears take it down, giving the Festoon Lights back")
			.pointAt(util.vector().blockSurface(util.grid().at(9, 3, 1), Direction.WEST))
			.placeNearTarget();
		scene.idle(100);
		scene.markAsFinished();
	}
}
