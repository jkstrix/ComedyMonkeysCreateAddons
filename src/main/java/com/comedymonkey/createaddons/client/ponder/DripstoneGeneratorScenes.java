package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.dripstone.PointedDripstoneGeneratorBlock;
import com.comedymonkey.createaddons.content.dripstone.PointedDripstoneGeneratorBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * "Drip, Drip, Drop": the Pointed Dripstone Generator, on the Tuff Generator's schematic (a generator on a plate with a
 * shaft into its back), the generator swapped in. Seeding, water, rotation and the knock, collecting, and the Wireless one.
 */
public final class DripstoneGeneratorScenes {
	private DripstoneGeneratorScenes() {}

	public static void generator(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("pointed_dripstone_generator", "Drip, Drip, Drop");
		scene.configureBasePlate(0, 0, 5);
		BlockPos gen = util.grid().at(2, 2, 2);
		swap(scene, gen, ModBlocks.POINTED_DRIPSTONE_GENERATOR.get());
		scene.world().modifyBlockEntity(gen, PointedDripstoneGeneratorBlockEntity.class, be -> be.setPonderState(0, 0, 0));
		scene.showBasePlate();
		scene.idle(5);
		scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 1, 2), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(gen), Direction.DOWN);
		scene.idle(15);

		scene.overlay().showText(80)
			.text("The Pointed Dripstone Generator is a little dripstone cave in a stone generator's box")
			.pointAt(util.vector().blockSurface(gen, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showControls(util.vector().blockSurface(gen, Direction.NORTH), Pointing.DOWN, 40)
			.rightClick().withItem(new ItemStack(Items.POINTED_DRIPSTONE, 4));
		scene.idle(10);
		scene.world().modifyBlockEntity(gen, PointedDripstoneGeneratorBlockEntity.class, be -> be.setPonderState(0, 4, 0));
		scene.overlay().showText(90)
			.text("Right-click it with four Pointed Dripstone to seed it. They stay inside, and grow the tips")
			.pointAt(util.vector().blockSurface(gen, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().blockSurface(gen, Direction.WEST), Pointing.LEFT, 40)
			.withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "fluid_pipe"))));
		scene.world().modifyBlockEntity(gen, PointedDripstoneGeneratorBlockEntity.class, be -> be.setPonderState(0, 4, 1000));
		scene.overlay().showText(90)
			.text("Pipe water into any side but the window and the back. Each Pointed Dripstone takes 100 mB")
			.pointAt(util.vector().blockSurface(gen, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().position(2, 2, 3), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().fromTo(2, 2, 3, 2, 2, 2), 256);
		scene.overlay().showText(110)
			.text("Turn a shaft in its back. The four grow their tips together, then a hammer goes round knocking each one off into its store")
			.pointAt(util.vector().blockSurface(gen, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);
		scene.overlay().showText(90)
			.text("Four at a time: one every 5 seconds at 256 RPM. Slower rotation grows them more slowly")
			.pointAt(util.vector().blockSurface(gen, Direction.NORTH))
			.placeNearTarget();
		// the tips take 12 s to grow at 256 RPM; stay for the hammer's round of knocks (8 s)
		scene.idle(260);

		scene.overlay().showControls(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.NORTH), Pointing.DOWN, 40)
			.withItem(new ItemStack(Items.POINTED_DRIPSTONE));
		scene.overlay().showText(100)
			.text("Collect from any side, or by hand in its Wrench screen. Full, switched off, unseeded or dry, it rests and uses no stress")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(gen), Pointing.DOWN, 40).rightClick().withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
		scene.idle(50);

		swap(scene, gen, ModBlocks.WIRELESS_POINTED_DRIPSTONE_GENERATOR.get());
		scene.idle(5);
		scene.world().setKineticSpeed(util.select().fromTo(2, 2, 3, 2, 2, 2), 256);
		scene.world().modifyBlockEntity(gen, PointedDripstoneGeneratorBlockEntity.class, be -> be.setPonderState(12, 4, 1000));
		scene.overlay().showText(90)
			.text("Craft it with a Transmitter to make it Wireless, and switch it off from anywhere on its frequency")
			.pointAt(util.vector().topOf(gen))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}

	private static void swap(CreateSceneBuilder scene, BlockPos gen, Block block) {
		scene.world().setBlock(gen, block.defaultBlockState().setValue(PointedDripstoneGeneratorBlock.FACING, Direction.NORTH)
			.setValue(PointedDripstoneGeneratorBlock.POWERED, false), false);
	}
}
