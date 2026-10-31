package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.tube.ClockworkInletBlock;
import com.comedymonkey.createaddons.content.tube.ClockworkInletBlockEntity;
import com.comedymonkey.createaddons.content.tube.ClockworkReceiverBlockEntity;
import com.comedymonkey.createaddons.content.tube.ClockworkTubeBlock;
import com.comedymonkey.createaddons.content.tube.ClockworkTubeBlockEntity;
import com.comedymonkey.createaddons.content.tube.TubeJoint;
import com.comedymonkey.createaddons.content.tube.TubeLoad;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * "Down the Tubes" (design/specs/clockwork-tubes.md D38): Inlets on a chest, over a depot and over the floor; tubes
 * branching into one; the Receiver turned by its cog, items gliding past the glass and out through the middle of the
 * cog; the Brass Inlet's filter; redstone on an Inlet; one Receiver to a network.
 */
public final class TubeScenes {
	private TubeScenes() {}

	/** Ticks an item takes across a tube in the scene. */
	private static final int PER_TUBE = 6;

	/** One stretch of a load's trip: the tube, and the faces it comes in and leaves by. */
	private record Leg(BlockPos tube, Direction from, Direction to) {}

	public static void downTheTubes(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("clockwork_tubes", "Down the Tubes");
		scene.configureBasePlate(0, 0, 9);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos leftInlet = util.grid().at(1, 2, 4), midInlet = util.grid().at(4, 2, 4), rightInlet = util.grid().at(7, 2, 4);
		BlockPos depot = util.grid().at(4, 1, 4), receiver = util.grid().at(4, 5, 4), hub = util.grid().at(4, 3, 4);
		BlockPos rightElbow = util.grid().at(7, 3, 4), second = util.grid().at(7, 4, 4), redstone = util.grid().at(8, 2, 4);

		// the Inlets
		scene.world().showSection(util.select().fromTo(1, 1, 4, 1, 2, 4), Direction.DOWN);
		scene.world().showSection(util.select().fromTo(4, 1, 4, 4, 2, 4), Direction.DOWN);
		scene.world().showSection(util.select().position(rightInlet), Direction.DOWN);
		scene.idle(15);
		scene.world().createItemOnBeltLike(depot, Direction.UP, new ItemStack(Items.IRON_INGOT));
		ElementLink<EntityElement> bone = scene.world().createItemEntity(util.vector().of(7.5, 1.1, 4.5), Vec3.ZERO, new ItemStack(Items.BONE));
		scene.overlay().showText(90)
			.text("A Clockwork Inlet takes items from what's in front of its mouth: a chest, a depot or a belt, or items lying on the floor")
			.pointAt(util.vector().blockSurface(leftInlet, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// the tubes
		scene.world().showSection(util.select().fromTo(1, 3, 4, 7, 3, 4), Direction.DOWN);
		scene.world().showSection(util.select().position(4, 4, 4), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("Clockwork Tubes join to them at their backs and carry what they take. They branch any way: up, down and sideways")
			.pointAt(util.vector().topOf(hub))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// the Receiver, turned by its cog
		Selection drive = util.select().fromTo(4, 1, 5, 4, 5, 5);
		scene.world().showSection(util.select().position(receiver).add(util.select().position(4, 6, 4)), Direction.DOWN);
		scene.world().showSection(drive, Direction.DOWN);
		scene.idle(15);
		scene.world().setKineticSpeed(drive, 64);
		scene.world().setKineticSpeed(util.select().position(receiver), -64);
		scene.overlay().showText(100)
			.text("One Clockwork Receiver pulls everything to itself. It turns by its cog: mesh a cogwheel with it. Faster means more items, and quicker")
			.pointAt(util.vector().blockSurface(receiver, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		// the Inlets take turns: each gulps, and its item glides up the glass and out through the middle of the cog
		scene.overlay().showText(110)
			.text("The Inlets take turns. Each item glides past the glass and out through the middle of the hollow cog, into the chest above, or drops out with nothing there")
			.pointAt(util.vector().centerOf(hub))
			.placeNearTarget()
			.attachKeyFrame();
		gulp(scene, leftInlet);
		glide(scene, new ItemStack(Items.COBBLESTONE), receiver,
			new Leg(util.grid().at(1, 3, 4), Direction.DOWN, Direction.EAST), new Leg(util.grid().at(2, 3, 4), Direction.WEST, Direction.EAST),
			new Leg(util.grid().at(3, 3, 4), Direction.WEST, Direction.EAST), new Leg(hub, Direction.WEST, Direction.UP),
			new Leg(util.grid().at(4, 4, 4), Direction.DOWN, Direction.UP));
		gulp(scene, midInlet);
		scene.world().removeItemsFromBelt(depot);
		glide(scene, new ItemStack(Items.IRON_INGOT), receiver, new Leg(hub, Direction.DOWN, Direction.UP),
			new Leg(util.grid().at(4, 4, 4), Direction.DOWN, Direction.UP));
		gulp(scene, rightInlet);
		scene.world().modifyEntity(bone, Entity::discard);
		glide(scene, new ItemStack(Items.BONE), receiver,
			new Leg(rightElbow, Direction.DOWN, Direction.WEST), new Leg(util.grid().at(6, 3, 4), Direction.EAST, Direction.WEST),
			new Leg(util.grid().at(5, 3, 4), Direction.EAST, Direction.WEST), new Leg(hub, Direction.EAST, Direction.UP),
			new Leg(util.grid().at(4, 4, 4), Direction.DOWN, Direction.UP));
		scene.idle(20);

		// the Brass Inlet's filter
		scene.world().setBlock(leftInlet, ModBlocks.BRASS_CLOCKWORK_INLET.get().defaultBlockState()
			.setValue(ClockworkInletBlock.FACING, Direction.DOWN), true);
		scene.idle(10);
		scene.overlay().showControls(util.vector().blockSurface(leftInlet, Direction.NORTH), Pointing.RIGHT, 60)
			.rightClick()
			.withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "filter"))));
		scene.overlay().showText(100)
			.text("A Brass Clockwork Inlet has a filter on its sides: it takes only what the filter passes, and up to a stack at a time")
			.pointAt(util.vector().blockSurface(leftInlet, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		// redstone switches an Inlet off
		scene.world().showSection(util.select().position(redstone), Direction.DOWN);
		scene.world().modifyBlock(rightInlet, s -> s.setValue(ClockworkInletBlock.POWERED, true), false);
		scene.idle(10);
		scene.overlay().showText(80)
			.text("Redstone switches an Inlet off")
			.pointAt(util.vector().topOf(redstone))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// one Receiver to a network
		scene.world().showSection(util.select().position(second), Direction.DOWN);
		scene.world().modifyBlock(rightElbow, s -> s.setValue(ClockworkTubeBlock.JOINTS.get(Direction.UP), TubeJoint.CAPPED), false);
		scene.idle(15);
		scene.overlay().showText(110)
			.text("One Receiver to a network: a tube never joins two, and stays capped instead. A Wrench on a tube's arm shuts that joint too, so networks can run side by side")
			.pointAt(util.vector().blockSurface(rightElbow, Direction.UP))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showText(90)
			.text("When the chest at the end is full, everything waits in the tubes, one item a tube. Nothing is lost")
			.pointAt(util.vector().topOf(util.grid().at(4, 6, 4)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
	}

	/** The Inlet gulps (as the server's block event does in game). */
	private static void gulp(CreateSceneBuilder scene, BlockPos inlet) {
		scene.world().modifyBlockEntity(inlet, ClockworkInletBlockEntity.class, be -> be.triggerEvent(ClockworkInletBlockEntity.GULP, 0));
		scene.idle(4);
	}

	/** A load glides through each tube in turn, then passes through the Receiver's cog. */
	private static void glide(CreateSceneBuilder scene, ItemStack stack, BlockPos receiver, Leg... legs) {
		for (Leg leg : legs) {
			for (int k = 0; k < PER_TUBE; k++) {
				float p = k / (float) PER_TUBE;
				scene.world().modifyBlockEntity(leg.tube(), ClockworkTubeBlockEntity.class,
					be -> be.showLoad(new TubeLoad(stack, leg.from(), leg.to(), p, 0, 0)));
				scene.idle(1);
			}
			scene.world().modifyBlockEntity(leg.tube(), ClockworkTubeBlockEntity.class, be -> be.showLoad(null));
		}
		// up through its throat and the middle of its cog, and into the chest
		for (int k = 0; k <= PER_TUBE; k++) {
			float p = k / (float) PER_TUBE;
			scene.world().modifyBlockEntity(receiver, ClockworkReceiverBlockEntity.class, be -> be.showThroat(stack, p));
			scene.idle(1);
		}
		scene.world().modifyBlockEntity(receiver, ClockworkReceiverBlockEntity.class, be -> be.showThroat(ItemStack.EMPTY, -1));
		scene.idle(6);
	}
}
