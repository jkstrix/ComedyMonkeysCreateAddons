package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlock;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlockEntity;
import com.comedymonkey.createaddons.content.machine.TwoInputMachineBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
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

/**
 * Ponder scenes for the String Tangler, the Stone Infuser and the Tuff Generators. The visible text comes from
 * {@code comedymonkeysaddons.ponder.<scene>.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class MachineScenes {
	private MachineScenes() {}

	private static Item create(String id) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", id));
	}

	private static void state(CreateSceneBuilder scene, BlockPos pos, ItemStack main, ItemStack extra, ItemStack output,
							  ItemStack making, int time) {
		scene.world().modifyBlockEntity(pos, TwoInputMachineBlockEntity.class,
			be -> be.setPonderState(main, extra, output, making, time));
	}

	/** Shared opening: the machine drops in, then its motor and the hoppers either side. */
	private static void setUp(CreateSceneBuilder scene, SceneBuildingUtil util) {
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);
		scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 1, 2), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(2, 2, 2), Direction.DOWN);
		scene.idle(15);
	}

	public static void tangler(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("string_tangler", "Webs on Demand");
		setUp(scene, util);
		BlockPos machine = util.grid().at(2, 2, 2);

		scene.overlay().showText(70)
			.text("The String Tangler twists string and slime into cobwebs")
			.pointAt(util.vector().blockSurface(machine, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		scene.world().showSection(util.select().position(2, 2, 1), Direction.SOUTH);
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().fromTo(2, 2, 1, 2, 2, 2), 64);
		scene.overlay().showText(70)
			.text("Give it rotation through the shaft hole in its back")
			.pointAt(util.vector().blockSurface(machine, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		scene.world().showSection(util.select().position(2, 3, 2), Direction.DOWN);
		scene.idle(10);
		scene.overlay().showControls(util.vector().topOf(util.grid().at(2, 3, 2)), Pointing.DOWN, 50)
			.withItem(new ItemStack(Items.STRING, 4));
		scene.overlay().showText(80)
			.text("Every cobweb takes four string and a slime ball. Hoppers, funnels and pipes can load both from any side")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 3, 2), Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		state(scene, machine, new ItemStack(Items.STRING, 12), new ItemStack(Items.SLIME_BALL, 3), ItemStack.EMPTY,
			new ItemStack(Items.COBWEB), 400);
		scene.overlay().showText(90)
			.text("Watch the strands spin, tangle and knot themselves up into a web")
			.pointAt(util.vector().blockSurface(machine, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		state(scene, machine, new ItemStack(Items.STRING, 8), new ItemStack(Items.SLIME_BALL, 2),
			new ItemStack(Items.COBWEB), ItemStack.EMPTY, 0);
		scene.overlay().showControls(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.UP), Pointing.DOWN, 50)
			.withItem(new ItemStack(Items.COBWEB));
		scene.overlay().showText(80)
			.text("Pull the cobwebs out from any side, or take them by hand. Spider-free, guaranteed")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.markAsFinished();
	}

	public static void infuser(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("stone_infuser", "Cobblestone with Ambition");
		setUp(scene, util);
		BlockPos machine = util.grid().at(2, 2, 2);

		scene.overlay().showText(80)
			.text("The Stone Infuser turns cobblestone into Create's ore-bearing stones")
			.pointAt(util.vector().blockSurface(machine, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().position(2, 2, 1), Direction.SOUTH);
		scene.world().showSection(util.select().position(2, 3, 2), Direction.DOWN);
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().fromTo(2, 2, 1, 2, 2, 2), 64);
		ItemStack zinc = new ItemStack(create("zinc_nugget"), 9);
		scene.overlay().showControls(util.vector().topOf(util.grid().at(2, 3, 2)), Pointing.DOWN, 50).withItem(zinc);
		state(scene, machine, new ItemStack(Items.COBBLESTONE, 8), zinc.copyWithCount(36), ItemStack.EMPTY,
			new ItemStack(create("asurine")), 160);
		scene.overlay().showText(90)
			.text("Power it through its back and feed it cobblestone and nine nuggets for each stone. Zinc nuggets make asurine...")
			.pointAt(util.vector().blockSurface(machine, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		Item[][] stones = {{Items.IRON_NUGGET, create("crimsite")}, {Items.GOLD_NUGGET, create("ochrum")},
			{create("copper_nugget"), create("veridium")}};
		scene.overlay().showText(110)
			.text("...iron nuggets make crimsite, gold makes ochrum and copper makes veridium")
			.pointAt(util.vector().blockSurface(machine, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		for (Item[] pair : stones) {
			ItemStack ore = new ItemStack(pair[0], 9);
			scene.overlay().showControls(util.vector().topOf(util.grid().at(2, 3, 2)), Pointing.DOWN, 30).withItem(ore);
			state(scene, machine, new ItemStack(Items.COBBLESTONE, 8), ore.copyWithCount(36), ItemStack.EMPTY,
				new ItemStack(pair[1]), 140);
			scene.idle(36);
		}
		scene.idle(10);

		state(scene, machine, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(create("veridium")), ItemStack.EMPTY, 0);
		scene.overlay().showText(90)
			.text("Crush the stone again and some nuggets come back, never as many as went in. Crushing tuff gives a few of all four")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}

	public static void tuff(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("tuff_generator", "When the Going Gets Tuff");
		scene.configureBasePlate(0, 0, 5);
		BlockPos gen = util.grid().at(2, 2, 2);
		scene.showBasePlate();
		scene.idle(5);
		scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 1, 2), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(gen), Direction.DOWN);
		scene.idle(15);
		scene.world().showSection(util.select().position(2, 2, 1), Direction.SOUTH);
		scene.idle(10);
		scene.world().setKineticSpeed(util.select().fromTo(2, 2, 1, 2, 2, 2), 64);
		scene.world().modifyBlockEntity(gen, CobbleGeneratorBlockEntity.class, be -> be.setPonderState(12, false));

		scene.overlay().showText(90)
			.text("The Tuff Generator works just like a Cobblestone Generator, but lava meets water over a bed of gravel, and out comes tuff")
			.pointAt(util.vector().blockSurface(gen, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.SOUTH), Pointing.DOWN, 40)
			.withItem(new ItemStack(Items.TUFF));
		scene.overlay().showText(90)
			.text("Power it from the back and collect from any side. Full, or switched off by redstone, it rests and uses no stress")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().setBlock(gen, ModBlocks.WIRELESS_TUFF_GENERATOR.get().defaultBlockState()
			.setValue(CobbleGeneratorBlock.FACING, Direction.SOUTH).setValue(CobbleGeneratorBlock.POWERED, false), true);
		scene.idle(5);
		scene.world().setKineticSpeed(util.select().fromTo(2, 2, 1, 2, 2, 2), 64);
		scene.world().modifyBlockEntity(gen, CobbleGeneratorBlockEntity.class, be -> be.setPonderState(12, false));
		scene.overlay().showText(90)
			.text("Craft it with a Transmitter to make it Wireless, and switch it off from anywhere on its frequency")
			.pointAt(util.vector().topOf(gen))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}

}
