package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.electric.BatteryBlock;
import com.comedymonkey.createaddons.content.electric.BatteryBlockEntity;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Battery and the Industrial Battery (design/specs/battery.md). The structures (design/ponder/battery.ponder.js)
 * carry what each battery shows; the tower's blocks know the tower they form, and the scene switches them over.
 */
public final class BatteryScenes {
	private BatteryScenes() {}

	public static void battery(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("battery", "Storing Power");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos one = util.grid().at(3, 1, 1), end = util.grid().at(3, 1, 4), motor = util.grid().at(4, 1, 4);
		scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 1, 1), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(100)
			.text("A Battery stores Forge Energy. What its line's generators make beyond what's used fills it, and when they fall short it gives it back")
			.pointAt(util.vector().topOf(one))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showText(90)
			.text("Its tubes light up as it fills. The left lamp shows it charging, the right one giving")
			.pointAt(util.vector().blockSurface(one, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(1, 1, 4, 3, 1, 4), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(100)
			.text("Its connectors are its sides, front and back. Batteries that touch there are one bank, with one store, moving 2,048 FE a tick each")
			.pointAt(util.vector().topOf(util.grid().at(2, 1, 4)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.world().showSection(util.select().position(motor), Direction.WEST);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("A machine on a connector runs from it, as from a generator")
			.pointAt(util.vector().topOf(motor))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showControls(util.vector().topOf(end), Pointing.DOWN, 60).withItem(new ItemStack(ModItems.BATTERY.get()));
		scene.overlay().showText(80)
			.text("Broken or picked up, a Battery keeps its charge")
			.pointAt(util.vector().topOf(end))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.markAsFinished();
	}

	public static void industrialBattery(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("industrial_battery", "The Industrial Battery");
		scene.configureBasePlate(0, 0, 11);
		scene.scaleSceneView(0.8f);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos master = util.grid().at(2, 1, 1), top = util.grid().at(2, 3, 2), rod = util.grid().at(2, 4, 2);
		for (int y = 1; y <= 3; y++) {
			scene.world().showSection(util.select().fromTo(1, y, 1, 3, y, 3), Direction.DOWN);
			scene.idle(8);
		}
		scene.idle(10);
		scene.overlay().showText(100)
			.text("Fill a 3x3 footprint, 3 to 9 high, with Batteries and they become one Industrial Battery: 20 million FE a block")
			.pointAt(util.vector().topOf(top))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(50);
		scene.world().modifyBlocks(util.select().fromTo(1, 1, 1, 3, 3, 3), s -> s.setValue(BatteryBlock.FORMED, true)
			.setValue(BatteryBlock.LEFT, false).setValue(BatteryBlock.RIGHT, false).setValue(BatteryBlock.BACK, false), false);
		scene.world().modifyBlockEntity(master, BatteryBlockEntity.class, be -> be.formAge = 0);
		scene.idle(70);

		scene.overlay().showText(90)
			.text("It faces the way of the Battery that completed it. Its level column rises and falls with its charge")
			.pointAt(util.vector().blockSurface(master, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(8, 1, 2, 10, 1, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(util.grid().at(3, 3, 1)), Pointing.DOWN, 60)
			.withItem(new ItemStack(ModItems.COPPER_POWER_LINE.get()));
		scene.overlay().showText(110)
			.text("It connects only through the four insulators on its top corners: hang Power Lines from them to Line Posts, like a pole. Wires and machines beside it don't connect")
			.pointAt(util.vector().topOf(util.grid().at(3, 3, 1)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.world().showSection(util.select().position(rod), Direction.DOWN);
		scene.idle(15);
		scene.world().modifyBlockEntity(master, BatteryBlockEntity.class, be -> {
			be.strikeAge = 0;
			be.strikeAt = 4;
		});
		scene.overlay().showText(90)
			.text("A Lightning Rod on top catches storms: each strike adds 250,000 FE")
			.pointAt(util.vector().centerOf(rod))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(50);
		scene.world().modifyBlockEntity(master, BatteryBlockEntity.class, be -> {
			be.strikeAge = 0;
			be.strikeAt = 4;
		});
		scene.idle(50);

		scene.overlay().showControls(util.vector().topOf(util.grid().at(1, 3, 3)), Pointing.DOWN, 50).leftClick().withItem(new ItemStack(Items.IRON_PICKAXE));
		scene.overlay().showText(110)
			.text("Break a block and the tallest run of 3 or more full layers left forms again. The rest are plain Batteries, each with its share: charge is never lost")
			.pointAt(util.vector().topOf(util.grid().at(1, 3, 3)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showText(90)
			.text("Far from any player, a Battery or an Industrial Battery keeps charging and giving on paper, and never loads a chunk")
			.pointAt(util.vector().topOf(top))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}
}
