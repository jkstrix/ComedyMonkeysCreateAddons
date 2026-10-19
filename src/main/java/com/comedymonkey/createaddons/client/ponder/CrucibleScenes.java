package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.crucible.PressureCrucibleBlockEntity;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Ponder scene for the Pressure Crucible. As with the other scenes, the visible text comes from
 * {@code comedymonkeysaddons.ponder.pressure_crucible.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class CrucibleScenes {
	private CrucibleScenes() {}

	public static void crucible(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("pressure_crucible", "Pressing Dust into Emeralds and Netherite Scrap");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos burner = util.grid().at(2, 1, 2);
		BlockPos crucible = util.grid().at(2, 2, 2);
		Vec3 top = util.vector().topOf(crucible);
		Vec3 front = util.vector().blockSurface(crucible, Direction.SOUTH);
		Vec3 burnerFront = util.vector().blockSurface(burner, Direction.SOUTH);

		scene.world().showSection(util.select().position(burner), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(crucible), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().fromTo(0, 2, 2, 1, 2, 2), Direction.EAST);
		scene.idle(15);

		// text_1
		scene.overlay().showText(70)
			.text("The Pressure Crucible presses fine dust into gems and metal scrap")
			.pointAt(front)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		// text_2
		scene.overlay().showControls(top, Pointing.DOWN, 80)
			.withItem(new ItemStack(ModItems.EMERALD_DUST.get()));
		scene.overlay().showText(90)
			.text("Crushing Wheels and Millstones have a small chance to turn cobblestone or stone into Emerald Dust. Crushing netherrack can give Netherite Dust")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// text_3
		scene.world().setKineticSpeed(util.select().fromTo(0, 2, 2, 2, 2, 2), 64);
		scene.overlay().showText(80)
			.text("It needs rotational force, and a Blaze Burner underneath for heat")
			.pointAt(burnerFront)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// Emerald: heated burner
		scene.world().modifyBlock(burner, s -> s.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
		scene.overlay().showControls(top, Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.EMERALD_DUST.get(), 9));
		scene.idle(10);
		scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
			be -> be.setPonderState(new ItemStack(ModItems.EMERALD_DUST.get(), 9), ItemStack.EMPTY, true, 200));
		// text_4
		scene.overlay().showText(90)
			.text("Nine Emerald Dust over a heated Blaze Burner are pressed into an Emerald")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
			be -> be.setPonderState(ItemStack.EMPTY, new ItemStack(Items.EMERALD), false, 0));
		scene.idle(20);

		// Taking the results out
		scene.overlay().showControls(top, Pointing.DOWN, 40)
			.rightClick();
		scene.idle(10);
		scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
			be -> be.setPonderState(ItemStack.EMPTY, ItemStack.EMPTY, false, 0));
		// text_5
		scene.overlay().showText(100)
			.text("Right-click with an empty hand to take the results out. Funnels and other item transport can insert dust and extract results from any side")
			.pointAt(front)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		// Netherite: superheated burner
		scene.overlay().showControls(burnerFront, Pointing.RIGHT, 40)
			.rightClick()
			.withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "blaze_cake"))));
		scene.idle(10);
		scene.world().modifyBlock(burner, s -> s.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SEETHING), false);
		scene.world().setKineticSpeed(util.select().fromTo(0, 2, 2, 2, 2, 2), 128);
		scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
			be -> be.setPonderState(new ItemStack(ModItems.NETHERITE_DUST.get(), 9), ItemStack.EMPTY, true, 800));
		// text_6
		scene.overlay().showText(90)
			.text("Netherite Dust needs a superheated Blaze Burner, fed with Blaze Cakes, and takes much longer to press")
			.pointAt(burnerFront)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
			be -> be.setPonderState(ItemStack.EMPTY, new ItemStack(Items.NETHERITE_SCRAP), false, 0));
		// text_7
		scene.overlay().showText(90)
			.text("Nine Netherite Dust make one Netherite Scrap. Faster rotation presses faster, at a heavy stress cost")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// Quick jobs: sand into glass (heated), gravel into stone (no heat), one at a time
		scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
			be -> be.setPonderState(ItemStack.EMPTY, ItemStack.EMPTY, false, 0));
		scene.world().modifyBlock(burner, s -> s.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
		scene.overlay().showControls(top, Pointing.DOWN, 30)
			.rightClick()
			.withItem(new ItemStack(Items.SAND, 4));
		scene.idle(10);
		// text_8
		scene.overlay().showText(90)
			.text("It also makes quick work of everyday blocks: sand is pressed into glass over a heated Blaze Burner...")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		pressQuickly(scene, crucible, Items.SAND, Items.GLASS);
		scene.idle(50);

		scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
			be -> be.setPonderState(ItemStack.EMPTY, ItemStack.EMPTY, false, 0));
		scene.world().modifyBlock(burner, s -> s.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING), false);
		scene.overlay().showControls(top, Pointing.DOWN, 30)
			.rightClick()
			.withItem(new ItemStack(Items.GRAVEL, 4));
		scene.idle(10);
		// text_9
		scene.overlay().showText(90)
			.text("...and gravel is packed back into stone, with no heat needed at all. Both take only a moment each")
			.pointAt(burnerFront)
			.placeNearTarget()
			.attachKeyFrame();
		pressQuickly(scene, crucible, Items.GRAVEL, Items.STONE);
		scene.idle(50);
		scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
			be -> be.setPonderState(ItemStack.EMPTY, ItemStack.EMPTY, false, 0));

		// Netherrack supply: a Mechanical Mixer over a Basin on a heated Blaze Burner, in the back corner
		BlockPos mixerBurner = util.grid().at(4, 1, 0);
		BlockPos basin = util.grid().at(4, 2, 0);
		BlockPos mixer = util.grid().at(4, 4, 0);
		Vec3 basinSide = util.vector().blockSurface(basin, Direction.WEST);
		scene.world().showSection(util.select().fromTo(4, 1, 0, 4, 2, 0), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().fromTo(4, 4, 0, 4, 5, 0), Direction.DOWN);
		scene.world().modifyBlock(mixerBurner, s -> s.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
		scene.world().setKineticSpeed(util.select().fromTo(4, 4, 0, 4, 5, 0), 64);
		scene.idle(15);

		scene.overlay().showControls(util.vector().topOf(basin), Pointing.DOWN, 40)
			.withItem(new ItemStack(Items.COBBLESTONE));
		scene.idle(10);
		scene.overlay().showControls(util.vector().topOf(basin), Pointing.DOWN, 30)
			.withItem(new ItemStack(Items.LAVA_BUCKET));
		scene.world().modifyBlockEntity(basin, BasinBlockEntity.class, be -> {
			be.getInputInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 8));
			be.getTanks().getFirst().getPrimaryHandler().setFluid(new FluidStack(Fluids.LAVA, 400));
		});
		scene.idle(10);
		scene.world().modifyBlockEntity(mixer, MechanicalMixerBlockEntity.class, MechanicalMixerBlockEntity::startProcessingBasin);
		// text_10
		scene.overlay().showText(110)
			.text("For a steady supply of netherrack, mix cobblestone with lava in a Basin over a heated Blaze Burner. A cobblestone generator can then feed both dusts")
			.pointAt(basinSide)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(60);
		scene.world().modifyBlockEntity(basin, BasinBlockEntity.class, be -> {
			be.getInputInventory().setStackInSlot(0, ItemStack.EMPTY);
			be.getTanks().getFirst().getPrimaryHandler().setFluid(FluidStack.EMPTY);
			be.getOutputInventory().setStackInSlot(0, new ItemStack(Items.NETHERRACK, 8));
		});
		scene.idle(60);

		scene.markAsFinished();
	}

	/** Four one-for-one presses in quick succession, the results piling up in the bowl. */
	private static void pressQuickly(CreateSceneBuilder scene, BlockPos crucible, Item input, Item output) {
		for (int done = 0; done < 4; done++) {
			int pressed = done;
			// 40 work at 128 RPM (8 per tick) is five ticks per press, as in game
			scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
				be -> be.setPonderState(new ItemStack(input, 4 - pressed),
					pressed == 0 ? ItemStack.EMPTY : new ItemStack(output, pressed), true, 40));
			scene.idle(8);
		}
		scene.world().modifyBlockEntity(crucible, PressureCrucibleBlockEntity.class,
			be -> be.setPonderState(ItemStack.EMPTY, new ItemStack(output, 4), false, 0));
	}
}
