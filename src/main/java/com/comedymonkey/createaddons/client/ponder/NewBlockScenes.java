package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlock;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlockEntity;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.content.incinerator.IncineratorBlock;
import com.comedymonkey.createaddons.registry.ModBlocks;
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
 * Ponder scenes for the Basalt and Blackstone Generators, the Magma Engine and the Incinerator. The visible text comes
 * from {@code comedymonkeysaddons.ponder.<scene>.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class NewBlockScenes {
	private NewBlockScenes() {}

	public static void stoneGenerators(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("stone_generators", "Lava, Meet Ice");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos basalt = util.grid().at(1, 2, 2);
		BlockPos blackstone = util.grid().at(3, 2, 2);
		scene.world().showSection(util.select().fromTo(1, 1, 2, 3, 1, 3), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(basalt), Direction.DOWN);
		scene.idle(15);

		scene.overlay().showText(80)
			.text("Basalt and Blackstone Generators work just like the Cobblestone ones, only here lava meets a block of blue ice")
			.pointAt(util.vector().blockSurface(basalt, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().position(1, 2, 1), Direction.SOUTH);
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().fromTo(1, 2, 1, 1, 2, 2), 64);
		running(scene, basalt);
		scene.overlay().showText(90)
			.text("The Basalt Generator wears ice-blue trim and is crafted with Soul Soil. Basalt is soft, so at full speed it comes twice as fast")
			.pointAt(util.vector().blockSurface(basalt, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().position(blackstone), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(3, 2, 1), Direction.SOUTH);
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().fromTo(3, 2, 1, 3, 2, 2), 64);
		running(scene, blackstone);
		scene.overlay().showText(90)
			.text("The Blackstone Generator is all black and gold, and is crafted with a block of Blackstone")
			.pointAt(util.vector().blockSurface(blackstone, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().blockSurface(util.grid().at(3, 1, 3), Direction.UP), Pointing.DOWN, 50)
			.withItem(new ItemStack(Items.BLACKSTONE));
		scene.overlay().showText(80)
			.text("Power goes in the back and the stone comes out through hoppers, chutes, funnels or pipes. Full or switched off by redstone, they rest")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		swap(scene, util, basalt, ModBlocks.BRASS_BASALT_GENERATOR.get());
		swap(scene, util, blackstone, ModBlocks.BRASS_BLACKSTONE_GENERATOR.get());
		scene.overlay().showText(90)
			.text("Brass versions make sixteen at a time, and a Transmitter makes any of them Wireless, just like the cobblestone kind")
			.pointAt(util.vector().topOf(blackstone))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}

	private static void running(CreateSceneBuilder scene, BlockPos gen) {
		scene.world().modifyBlockEntity(gen, CobbleGeneratorBlockEntity.class, be -> be.setPonderState(12, false));
	}

	private static void swap(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos gen, Block block) {
		scene.world().setBlock(gen, block.defaultBlockState().setValue(CobbleGeneratorBlock.FACING, Direction.SOUTH), true);
		scene.idle(5);
		scene.world().setKineticSpeed(util.select().fromTo(gen.north(), gen), 64);
		running(scene, gen);
		scene.idle(5);
	}

	public static void magmaEngine(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("magma_engine", "Lava-Powered Muscle");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos engine = util.grid().at(2, 1, 2);
		BlockPos wellspring = util.grid().at(0, 1, 2);
		scene.world().showSection(util.select().position(engine), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Magma Engine: four Portable Engines' worth of muscle, in a single block")
			.pointAt(util.vector().topOf(engine))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().fromTo(0, 1, 2, 1, 1, 2), Direction.EAST);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("Pipe lava into its back or its bottom, or pour it in by the bucket. It holds four buckets")
			.pointAt(util.vector().blockSurface(engine, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.WEST);
		scene.idle(10);
		scene.world().modifyBlock(engine, s -> s.setValue(MagmaEngineBlock.LIT, true), false);
		scene.world().setKineticSpeed(util.select().fromTo(2, 1, 2, 4, 1, 2), 64);
		scene.overlay().showText(90)
			.text("Burning 10 mB of lava a second, it turns its shaft at 64 RPM with a whopping 8,192 stress units to spare")
			.pointAt(util.vector().blockSurface(engine, Direction.EAST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(80)
			.text("Hook it up to a Fluid Wellspring with Lava Upgrades and it runs forever. Lava in, power out, not a lump of coal in sight")
			.pointAt(util.vector().topOf(wellspring))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().modifyBlock(engine, s -> s.setValue(MagmaEngineBlock.LIT, false), false);
		scene.world().setKineticSpeed(util.select().fromTo(2, 1, 2, 4, 1, 2), 0);
		scene.overlay().showText(70)
			.text("Run it dry and it stops. Engineer's Goggles show how much lava is left")
			.pointAt(util.vector().topOf(engine))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);
		scene.markAsFinished();
	}

	public static void incinerator(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("incinerator", "Gone, Reduced to Atoms");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos fed = util.grid().at(1, 1, 2);
		BlockPos hopper = util.grid().at(1, 2, 2);
		BlockPos heater = util.grid().at(3, 1, 2);
		scene.world().showSection(util.select().position(fed), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
			.text("The Incinerator gets rid of anything you give it. No questions asked")
			.pointAt(util.vector().blockSurface(fed, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		scene.world().showSection(util.select().position(hopper), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showControls(util.vector().topOf(hopper), Pointing.DOWN, 40)
			.withItem(new ItemStack(Items.ROTTEN_FLESH));
		scene.idle(10);
		scene.world().modifyBlock(fed, s -> s.setValue(IncineratorBlock.LIT, true), false);
		scene.overlay().showText(80)
			.text("Hoppers, chutes, funnels and arms can push items in from any side. It never fills up, so it never backs up")
			.pointAt(util.vector().blockSurface(hopper, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().position(0, 1, 2), Direction.EAST);
		scene.idle(10);
		scene.overlay().showText(70)
			.text("Pipes work too: any fluid piped in simply vanishes")
			.pointAt(util.vector().centerOf(util.grid().at(0, 1, 2)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		scene.overlay().showText(70)
			.text("It stays lit for a few seconds after the last thing burns...")
			.pointAt(util.vector().blockSurface(fed, Direction.SOUTH))
			.placeNearTarget();
		scene.idle(60);
		scene.world().modifyBlock(fed, s -> s.setValue(IncineratorBlock.LIT, false), false);
		scene.idle(20);

		scene.world().showSection(util.select().position(heater), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().fromTo(3, 2, 2, 4, 2, 2), Direction.DOWN);
		scene.idle(15);
		scene.world().modifyBlock(heater, s -> s.setValue(IncineratorBlock.LIT, true), false);
		scene.overlay().showText(90)
			.text("...and while it's lit, it heats a Steam Engine boiler above it just like a lit Blaze Burner. Your rubbish, now a power source")
			.pointAt(util.vector().blockSurface(heater, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}
}
