package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.wellspring.WellspringBlock;
import com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity;
import com.comedymonkey.createaddons.content.wellspring.WellspringSource;
import com.comedymonkey.createaddons.registry.ModItems;

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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Ponder scenes for the Fluid Wellspring. The text passed to {@code text(...)} is only the in-code default; what
 * players see comes from the {@code comedymonkeysaddons.ponder.wellspring.*} keys in en_us.json, numbered
 * {@code text_1}, {@code text_2}... in the order the texts appear below. Keep the two in step.
 */
public final class WellspringScenes {
	private WellspringScenes() {}

	public static void wellspring(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("wellspring", "Generating Water and Lava with the Fluid Wellspring");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos wellspring = util.grid().at(2, 1, 2);
		Vec3 top = util.vector().topOf(wellspring);
		Vec3 westFace = util.vector().blockSurface(wellspring, Direction.WEST);
		Vec3 northFace = util.vector().blockSurface(wellspring, Direction.NORTH);

		scene.world().showSection(util.select().position(wellspring), Direction.DOWN);
		scene.idle(15);

		// text_1
		scene.overlay().showText(70)
			.text("The Fluid Wellspring is a tank that holds water or lava")
			.pointAt(westFace)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		// Install one generation upgrade
		scene.overlay().showControls(top, Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.WATER_UPGRADE.get()));
		scene.idle(10);
		scene.world().modifyBlock(wellspring, state -> state
			.setValue(WellspringBlock.SOURCE, WellspringSource.WATER)
			.setValue(WellspringBlock.UPGRADES, 1), false);
		// text_2
		scene.overlay().showText(90)
			.text("Right-click it with a Water Upgrade or a Lava Upgrade to make it generate water or lava")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		fill(scene, wellspring, 6, 300, 10);
		scene.idle(40);

		// Up to three of one kind
		scene.overlay().showControls(top, Pointing.DOWN, 30)
			.rightClick()
			.withItem(new ItemStack(ModItems.WATER_UPGRADE.get(), 2));
		scene.idle(10);
		scene.world().modifyBlock(wellspring, state -> state.setValue(WellspringBlock.UPGRADES, 3), false);
		// text_3
		scene.overlay().showText(90)
			.text("Up to three upgrades of one kind can be installed. Each one plugs into a socket on the lid, and each one speeds up generation")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		fill(scene, wellspring, 6, 900, 6);
		scene.idle(50);

		// The windows show the level
		scene.overlay().showOutline(PonderPalette.WHITE, "window", util.select().position(wellspring), 80);
		// text_4
		scene.overlay().showText(80)
			.text("The glass windows show the fill level. The ticks on the corner posts mark 25%%, 50%% and 75%%")
			.pointAt(northFace)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// Capacity Upgrades
		scene.overlay().showControls(top, Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.CAPACITY_UPGRADE.get()));
		scene.idle(10);
		scene.world().modifyBlockEntity(wellspring, WellspringBlockEntity.class,
			be -> be.getTank().setCapacity(be.getTank().getCapacity() + 16000));
		// text_5
		scene.overlay().showText(90)
			.text("Capacity Upgrades have their own slots and make the tank larger")
			.pointAt(westFace)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// Upgrade GUI
		scene.overlay().showControls(top, Pointing.DOWN, 50)
			.rightClick()
			.withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
		scene.idle(10);
		// text_6
		scene.overlay().showText(80)
			.text("Right-click with a Wrench, or sneak and right-click, to open the upgrade screen where upgrades can be added or removed")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// Getting fluid out
		scene.overlay().showControls(util.vector().blockSurface(wellspring, Direction.EAST), Pointing.RIGHT, 40)
			.rightClick()
			.withItem(new ItemStack(Items.BUCKET));
		scene.idle(10);
		// text_7
		scene.overlay().showText(90)
			.text("Pumps and pipes can draw fluid out from any side, or fill a bucket by hand")
			.pointAt(util.vector().blockSurface(wellspring, Direction.EAST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// Goggles
		var goggles = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "goggles"));
		scene.overlay().showControls(top, Pointing.DOWN, 60)
			.withItem(new ItemStack(goggles));
		scene.idle(10);
		// text_8
		scene.overlay().showText(80)
			.text("Wearing Engineer's Goggles shows the upgrades, the generation rate and the fill level as a percentage")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.markAsFinished();
	}

	/** Tops the tank up in steps so the level visibly rises. */
	private static void fill(SceneBuilder scene, BlockPos pos, int steps, int amountPerStep, int ticksPerStep) {
		for (int i = 0; i < steps; i++) {
			scene.world().modifyBlockEntity(pos, WellspringBlockEntity.class, be -> be.getTank()
				.fill(new FluidStack(Fluids.WATER, amountPerStep), IFluidHandler.FluidAction.EXECUTE));
			scene.idle(ticksPerStep);
		}
	}
}
