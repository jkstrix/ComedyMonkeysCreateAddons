package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.line.FluidLineBlockEntity;
import com.comedymonkey.createaddons.content.line.PowerLineBlockEntity;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Ponder scenes for the Fluid Lines, their fittings and the Power Lines. The Ponder world doesn't run the lines'
 * networks, so the scenes switch the flow on themselves. The visible text comes from
 * {@code comedymonkeysaddons.ponder.<scene>.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class LineScenes {
	private LineScenes() {}

	/** Shows the fluid running along the lines' slots, out towards the ends (east, or the sides given). */
	private static CompoundTag flowing(CompoundTag nbt, String fluid, int out) {
		CompoundTag f = new CompoundTag();
		f.putString("id", fluid);
		f.putInt("amount", 1);
		nbt.put("Flowing", f);
		nbt.putBoolean("Moving", true);
		nbt.putInt("Downstream", out);
		return nbt;
	}

	private static final int EAST = 1 << 5, NORTH_SOUTH = (1 << 2) | (1 << 3);

	public static void fluidLines(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("fluid_lines", "Pump-Free Pipes");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos intake = util.grid().at(2, 1, 2), mid = util.grid().at(3, 1, 2);
		scene.world().showSection(util.select().fromTo(1, 1, 2, 5, 1, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("Fluid Lines move fluid with no pump and no power")
			.pointAt(util.vector().topOf(mid))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showControls(util.vector().topOf(intake), Pointing.DOWN, 60).rightClick().withItem(new ItemStack(ModItems.INTAKE.get()));
		scene.overlay().showText(90)
			.text("Only an Intake moves fluid: fit it on the side of the line that faces a tank, and it draws that tank's fluid in")
			.pointAt(util.vector().blockSurface(intake, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().modifyBlockEntityNBT(util.select().fromTo(2, 1, 2, 4, 1, 2), FluidLineBlockEntity.class, nbt -> flowing(nbt, "minecraft:water", EAST));
		scene.overlay().showText(100)
			.text("The fluid slides along the slot on top as it moves. Copper carries 100 mB a tick, brass 1,000; nothing stays in the line")
			.pointAt(util.vector().topOf(mid))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(intake), Pointing.DOWN, 60).rightClick().withItem(new ItemStack(Items.WATER_BUCKET));
		scene.world().modifyBlockEntityNBT(util.select().position(intake), FluidLineBlockEntity.class, nbt -> {
			CompoundTag f = new CompoundTag();
			f.putString("id", "minecraft:water");
			f.putInt("amount", 1);
			nbt.put("Filter", f);
		});
		scene.overlay().showText(90)
			.text("Right-click the line with a bucket to set the Intake's filter: then it draws only that fluid")
			.pointAt(util.vector().topOf(intake))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(2, 1, 4, 3, 1, 4), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("Copper and brass never join, and nor do two colours (plain is one), so lines can run side by side. Dye lines before you place them, eight around a dye")
			.pointAt(util.vector().topOf(util.grid().at(2, 1, 4)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(mid), Pointing.DOWN, 60).rightClick().withItem(new ItemStack(Blocks.STONE_BRICKS));
		scene.world().modifyBlockEntityNBT(util.select().position(mid), FluidLineBlockEntity.class,
			nbt -> nbt.put("Encasing", NbtUtils.writeBlockState(Blocks.STONE_BRICKS.defaultBlockState())), true);
		scene.overlay().showText(100)
			.text("Right-click a line with any full block to encase it. It keeps working, and Engineer's Goggles show where it runs")
			.pointAt(util.vector().topOf(mid))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		scene.markAsFinished();
	}

	public static void fittings(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("line_fittings", "Fittings");
		scene.configureBasePlate(0, 0, 9);
		scene.showBasePlate();
		scene.idle(5);
		scene.world().showSection(util.select().fromTo(1, 1, 1, 8, 1, 3), Direction.DOWN);
		scene.world().modifyBlockEntityNBT(util.select().fromTo(2, 1, 2, 6, 1, 2), FluidLineBlockEntity.class, nbt -> flowing(nbt, "minecraft:water", EAST));
		scene.world().modifyBlockEntityNBT(util.select().position(7, 1, 2), FluidLineBlockEntity.class, nbt -> flowing(nbt, "minecraft:water", NORTH_SOUTH));
		scene.idle(15);
		scene.overlay().showText(80)
			.text("Fittings clip round a straight piece of line, one to a block. The wrench takes them off again")
			.pointAt(util.vector().topOf(util.grid().at(4, 1, 2)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		BlockPos valve = util.grid().at(3, 1, 2), filter = util.grid().at(4, 1, 2), meter = util.grid().at(5, 1, 2), splitter = util.grid().at(6, 1, 2);
		scene.overlay().showControls(util.vector().topOf(valve), Pointing.DOWN, 60).rightClick();
		scene.world().modifyBlockEntityNBT(util.select().position(valve), FluidLineBlockEntity.class, nbt -> nbt.putInt("ValveOpen", 5));
		scene.overlay().showText(90)
			.text("A Valve shuts on a redstone signal. Right-click to turn its wheel down a tenth at a time; sneak to open it")
			.pointAt(util.vector().topOf(valve))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().modifyBlockEntityNBT(util.select().position(filter), FluidLineBlockEntity.class, nbt -> {
			CompoundTag f = new CompoundTag();
			f.putString("id", "minecraft:water");
			f.putInt("amount", 1);
			nbt.put("Filter", f);
		});
		scene.overlay().showText(90)
			.text("A Filter lets only its own fluid past: set it with a bucket. Good for sorting fluids part-way along a line")
			.pointAt(util.vector().topOf(filter))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().modifyBlockEntityNBT(util.select().position(meter), FluidLineBlockEntity.class, nbt -> {
			nbt.putInt("MeterFlow", 50);
			nbt.putFloat("MeterLevel", 0.5f);
		});
		scene.overlay().showText(90)
			.text("A Meter's needle shows the flow through it. Comparators read it, and goggles give the figure")
			.pointAt(util.vector().topOf(meter))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(splitter), Pointing.DOWN, 60).rightClick();
		scene.world().modifyBlockEntityNBT(util.select().position(splitter), FluidLineBlockEntity.class, nbt -> nbt.putInt("SplitMode", 1));
		scene.overlay().showText(100)
			.text("On a Fluid Line, a Splitter decides how the ends beyond it share: evenly, nearest first, or one at a time in turn. Right-click to switch")
			.pointAt(util.vector().topOf(splitter))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		scene.markAsFinished();
	}

	public static void powerLines(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("power_lines", "Power Lines");
		scene.configureBasePlate(0, 0, 11);
		// eleven blocks across: smaller, so the generator and the motor at its ends stay clear of the controls
		scene.scaleSceneView(0.6f);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos wire = util.grid().at(2, 1, 2), breaker = util.grid().at(3, 1, 2), postA = util.grid().at(4, 1, 2), motor = util.grid().at(10, 1, 2);
		scene.world().showSection(util.select().fromTo(1, 1, 2, 3, 1, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("Power Lines are thin cable for FE. They lie flat on whatever floor, wall or ceiling you place them against, and fold round corners")
			.pointAt(util.vector().topOf(wire))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(80)
			.text("Power flows by itself, from anything that gives it to anything that takes it: no Intake needed")
			.pointAt(util.vector().blockSurface(util.grid().at(1, 1, 2), Direction.EAST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().position(postA), Direction.DOWN);
		scene.world().showSection(util.select().fromTo(8, 1, 2, 10, 1, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(postA), Pointing.DOWN, 60).rightClick().withItem(new ItemStack(ModItems.COPPER_POWER_LINE.get()));
		scene.overlay().showText(110)
			.text("Line Posts carry it over distance: use Power Lines on one post's insulator, then another's, to hang a span. One Power Line for every four blocks, up to 16 for copper and 32 for brass")
			.pointAt(util.vector().topOf(postA))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showText(90)
			.text("Posts stack into a pole of any height. Use a Line Post on the side of the top one to add a branch with its own insulator")
			.pointAt(util.vector().topOf(postA))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(100)
			.text("A pole joins the cables tied to it that share a colour, so lines of different colours can share poles. Brass feeds copper of its colour there, never the other way")
			.pointAt(util.vector().topOf(postA))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(breaker), Pointing.DOWN, 60).rightClick();
		scene.world().modifyBlockEntityNBT(util.select().position(breaker), PowerLineBlockEntity.class, nbt -> nbt.putBoolean("LeverOff", true));
		scene.world().modifyBlock(motor, s -> s.setValue(ElectricMotorBlock.POWERED, false), false);
		scene.overlay().showText(90)
			.text("A Breaker cuts the power, with its lever or a redstone signal. Wrench a brass wire's joint with a copper one of the same colour at a Breaker, and brass feeds copper through it")
			.pointAt(util.vector().topOf(breaker))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(90)
			.text("Dye Power Lines before you place them, eight around a dye: a dyed line is a slightly thicker covered cable. Two colours never join (plain is one), so cables can run side by side")
			.pointAt(util.vector().topOf(util.grid().at(9, 1, 2)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}

	private static ItemStack create(String item) {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", item)));
	}

	public static void powerNetworks(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("power_networks", "Power Networks");
		scene.configureBasePlate(0, 0, 9);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos gen = util.grid().at(4, 1, 2), splitter = util.grid().at(6, 1, 2), drilled = util.grid().at(6, 1, 3);
		scene.world().showSection(util.select().fromTo(1, 1, 2, 8, 1, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(100)
			.text("Everything a Power Line reaches is one network. Lines on a generator's sides are one network too, brass and copper alike: it powers them all")
			.pointAt(util.vector().topOf(gen))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(gen), Pointing.DOWN, 60).withItem(create("goggles"));
		scene.overlay().showText(90)
			.text("Engineer's Goggles on a generator, a machine or a wire show the whole network: what its generators make, and what its machines use")
			.pointAt(util.vector().topOf(gen))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(80)
			.text("Short of power, every machine gets an even share, as if they were wired up by hand")
			.pointAt(util.vector().topOf(util.grid().at(8, 1, 2)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().fromTo(5, 1, 3, 7, 1, 6), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showControls(util.vector().topOf(splitter), Pointing.DOWN, 60).rightClick().withItem(create("wrench"));
		scene.overlay().showText(100)
			.text("A Power Splitter goes on a junction of three or four ways, on a floor, wall or ceiling. Wrench it to open its screen")
			.pointAt(util.vector().topOf(splitter))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showText(100)
			.text("Set each branch First, Second or Last: when power runs short, the First branches are served before the rest. Its dial points at the First branch")
			.pointAt(util.vector().topOf(splitter))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showControls(util.vector().topOf(drilled), Pointing.DOWN, 60).rightClick().withItem(create("wrench"));
		scene.overlay().showText(100)
			.text("To take a cable through a wall, run it up to a solid block on each side, in the same tier and colour, and wrench one end: it drills through and joins the two")
			.pointAt(util.vector().topOf(drilled))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showText(100)
			.text("A network keeps going with nobody nearby: far generators run on paper, their chunks never load, and their power still arrives")
			.pointAt(util.vector().topOf(gen))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		scene.markAsFinished();
	}
}
