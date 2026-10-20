package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlock;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scene shared by all four Cobblestone Generators. The visible text comes from
 * {@code comedymonkeysaddons.ponder.cobble_generator.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class CobbleGeneratorScenes {
	private CobbleGeneratorScenes() {}

	public static void generator(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("cobble_generator", "Cobblestone, on Tap");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos gen = util.grid().at(2, 2, 2);
		BlockPos motor = util.grid().at(2, 2, 3);
		BlockPos hopper = util.grid().at(2, 1, 2);
		BlockPos lever = util.grid().at(1, 2, 2);
		BlockPos link = util.grid().at(4, 1, 0);
		BlockPos linkLever = util.grid().at(3, 1, 0);
		Selection kinetics = util.select().fromTo(2, 2, 3, 2, 2, 2);
		Vec3 front = util.vector().blockSurface(gen, Direction.NORTH);
		Vec3 back = util.vector().blockSurface(gen, Direction.SOUTH);
		Vec3 top = util.vector().topOf(gen);

		// Hopper and chest first, so the generator has something to sit on
		scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 1, 2), Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(util.select().position(gen), Direction.DOWN);
		scene.idle(15);

		// text_1
		scene.overlay().showText(70)
			.text("Meet the Cobblestone Generator: an entire cobblestone farm, squeezed into one block")
			.pointAt(front)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		// text_2: power from the back
		scene.world().showSection(util.select().position(motor), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(kinetics, 64);
		scene.overlay().showText(80)
			.text("Power it through the shaft hole in the back. Only the back! It's picky like that")
			.pointAt(back)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// text_3: it works
		scene.overlay().showText(90)
			.text("Inside, water meets lava and out pops cobblestone, exactly as fast as a Mechanical Drill could mine it")
			.pointAt(front)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// text_4: needs collecting
		scene.overlay().showControls(util.vector().blockSurface(hopper, Direction.NORTH), Pointing.DOWN, 40)
			.withItem(new ItemStack(Items.COBBLESTONE));
		scene.overlay().showText(80)
			.text("It won't just hand it over, though. Hoppers, chutes, funnels or pipes have to come and get it")
			.pointAt(util.vector().blockSurface(hopper, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// text_5: full means idle, and no stress
		state(scene, gen, 64, false);
		scene.overlay().showText(80)
			.text("When it's full it simply stops, and stops using stress too. How considerate")
			.pointAt(front)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		state(scene, gen, 12, false);
		scene.idle(20);

		// text_6: redstone off switch
		scene.world().showSection(util.select().position(lever), Direction.EAST);
		scene.idle(10);
		scene.world().modifyBlock(lever, s -> s.setValue(LeverBlock.POWERED, true), false);
		state(scene, gen, 12, true);
		scene.overlay().showText(70)
			.text("A redstone signal switches it off as well")
			.pointAt(util.vector().centerOf(lever))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);
		scene.world().modifyBlock(lever, s -> s.setValue(LeverBlock.POWERED, false), false);
		state(scene, gen, 12, false);
		scene.idle(15);
		scene.world().hideSection(util.select().position(lever), Direction.WEST);
		scene.idle(10);

		// text_7: brass
		swap(scene, gen, ModBlocks.BRASS_COBBLE_GENERATOR.get().defaultBlockState());
		scene.world().setKineticSpeed(kinetics, 64);
		scene.overlay().showText(80)
			.text("The Brass Cobblestone Generator makes sixteen at a time. Sixteen!")
			.pointAt(front)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// text_8: wireless
		swap(scene, gen, ModBlocks.WIRELESS_BRASS_COBBLE_GENERATOR.get().defaultBlockState());
		scene.world().setKineticSpeed(kinetics, 64);
		scene.world().modifyBlockEntityNBT(util.select().position(gen), CobbleGeneratorBlockEntity.class, nbt -> {
			nbt.put("FrequencyFirst", item("minecraft:diamond"));
			nbt.put("FrequencyLast", item("minecraft:emerald"));
		});
		scene.overlay().showControls(top, Pointing.DOWN, 50)
			.withItem(new ItemStack(Items.DIAMOND));
		scene.overlay().showText(90)
			.text("Craft either one with a Transmitter to go Wireless. Set a frequency on top, just like a Redstone Link...")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(3, 1, 0, 4, 1, 0), Direction.DOWN);
		scene.idle(15);
		scene.world().modifyBlock(linkLever, s -> s.setValue(LeverBlock.POWERED, true), false);
		scene.world().modifyBlock(link, s -> s.setValue(CobbleGeneratorBlock.POWERED, true), false);
		scene.world().modifyBlock(gen, s -> s.setValue(CobbleGeneratorBlock.POWERED, true), false);
		state(scene, gen, 12, true);
		// text_9
		scene.overlay().showText(90)
			.text("...and switch it off from anywhere. Rocks, now with Wi-Fi")
			.pointAt(util.vector().topOf(link))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}

	private static void state(CreateSceneBuilder scene, BlockPos gen, int stored, boolean powered) {
		scene.world().modifyBlockEntity(gen, CobbleGeneratorBlockEntity.class, be -> be.setPonderState(stored, powered));
	}

	private static void swap(CreateSceneBuilder scene, BlockPos gen, BlockState state) {
		scene.world().setBlock(gen, state.setValue(CobbleGeneratorBlock.FACING, Direction.NORTH), true);
		scene.idle(5);
		state(scene, gen, 12, false);
	}

	private static CompoundTag item(String id) {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", id);
		tag.putInt("count", 1);
		return tag;
	}
}
