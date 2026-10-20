package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlock;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlockEntity;
import com.comedymonkey.createaddons.content.harvester.PlantKind;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlockEntity;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.comedymonkey.createaddons.registry.ModItems;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for the Auto Harvester and the Animal Pens, their windows towards the camera (north) and the motor
 * behind them. The visible text comes from
 * {@code comedymonkeysaddons.ponder.<scene>.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class FarmScenes {
	private FarmScenes() {}

	private static ItemStack wrench() {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench")));
	}

	public static void harvester(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("auto_harvester", "A Farm in a Box");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos harvester = util.grid().at(2, 1, 2);
		BlockPos motor = util.grid().at(2, 1, 3);
		Selection kinetics = util.select().fromTo(2, 1, 2, 2, 1, 3);
		scene.world().showSection(util.select().position(harvester), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Auto Harvester grows a crop or a sapling behind its glass, and harvests it again and again")
			.pointAt(util.vector().blockSurface(harvester, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showControls(util.vector().topOf(harvester), Pointing.DOWN, 50)
			.rightClick()
			.withItem(new ItemStack(Items.WHEAT_SEEDS));
		scene.idle(10);
		scene.world().modifyBlock(harvester, s -> s.setValue(AutoHarvesterBlock.PLANT, PlantKind.CROP), false);
		scene.world().modifyBlockEntity(harvester, AutoHarvesterBlockEntity.class,
			be -> be.setPonderPlant(new ItemStack(Items.WHEAT_SEEDS), 0.25f));
		scene.overlay().showText(80)
			.text("Use a seed, crop or sapling on it to plant it. The plant itself is never used up")
			.pointAt(util.vector().topOf(harvester))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().position(motor), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(kinetics, 32);
		scene.overlay().showText(90)
			.text("Turn a shaft in its back. It always uses 512 SU, and more speed won't make it any faster")
			.pointAt(util.vector().blockSurface(harvester, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().modifyBlockEntity(harvester, AutoHarvesterBlockEntity.class,
			be -> be.setPonderPlant(new ItemStack(Items.WHEAT_SEEDS), 0.95f));
		scene.idle(20);
		scene.world().modifyBlockEntity(harvester, AutoHarvesterBlockEntity.class, AutoHarvesterBlockEntity::startPonderHarvest);
		scene.overlay().showText(90)
			.text("Once the plant has grown, the reel sweeps across it and the harvest goes inside")
			.pointAt(util.vector().of(2.5, 1.4, 2.1))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(80)
			.text("Hoppers, chutes, funnels and pipes take the harvest out of any side")
			.pointAt(util.vector().blockSurface(harvester, Direction.EAST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().modifyBlock(harvester, s -> s.setValue(AutoHarvesterBlock.PLANT, PlantKind.TREE), false);
		scene.world().modifyBlockEntity(harvester, AutoHarvesterBlockEntity.class,
			be -> be.setPonderPlant(new ItemStack(Items.OAK_SAPLING), 0.95f));
		scene.idle(20);
		scene.world().modifyBlockEntity(harvester, AutoHarvesterBlockEntity.class, AutoHarvesterBlockEntity::startPonderHarvest);
		scene.overlay().showText(90)
			.text("Saplings grow into trees that a saw fells: their logs, and a few saplings, sticks and apples")
			.pointAt(util.vector().of(2.5, 1.4, 2.1))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(harvester), Pointing.DOWN, 60)
			.rightClick()
			.withItem(new ItemStack(Items.ENCHANTED_BOOK));
		scene.overlay().showText(90)
			.text("Faster-growing plants harvest more often. To speed it up, enchant it with Bountiful, from a Librarian")
			.pointAt(util.vector().topOf(harvester))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(harvester), Pointing.DOWN, 60)
			.rightClick()
			.withItem(wrench());
		scene.overlay().showText(80)
			.text("A Wrench opens its screen: the plant, the harvest and how far it has grown")
			.pointAt(util.vector().topOf(harvester))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.markAsFinished();
	}

	public static void animalPen(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("animal_pen", "Animal Husbandry");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos pen = util.grid().at(2, 1, 2);
		BlockPos motor = util.grid().at(2, 1, 3);
		Selection kinetics = util.select().fromTo(2, 1, 2, 2, 1, 3);
		scene.world().showSection(util.select().position(pen), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Animal Pen keeps one captured animal, and breeds a whole herd from it")
			.pointAt(util.vector().blockSurface(pen, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		Vec3 cowAt = util.vector().of(3.8, 1, 0.9);
		ElementLink<EntityElement> cow = scene.world().createEntity(level -> {
			Cow animal = EntityType.COW.create(level);
			animal.setPos(cowAt.x, cowAt.y, cowAt.z);
			animal.setYRot(160);
			animal.yBodyRot = animal.yHeadRot = 160;
			return animal;
		});
		scene.idle(15);
		scene.overlay().showControls(cowAt.add(0, 1.6, 0), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.CAPTURE_BOTTLE.get()));
		scene.overlay().showText(70)
			.text("Catch one by using a Capture Bottle on it")
			.pointAt(cowAt.add(0, 1, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(45);
		scene.world().modifyEntity(cow, Entity::discard);
		scene.idle(40);

		scene.overlay().showControls(util.vector().topOf(pen), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.BOTTLED_ANIMAL.get()));
		scene.idle(10);
		herd(scene, pen, 1, 0);
		scene.overlay().showText(70)
			.text("Then use the Bottled Animal on the pen to move it in")
			.pointAt(util.vector().topOf(pen))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		scene.overlay().showControls(util.vector().topOf(pen), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(Items.WHEAT));
		scene.idle(10);
		scene.world().modifyBlockEntity(pen, AnimalPenBlockEntity.class, be -> be.setPonderFood(new ItemStack(Items.WHEAT, 32)));
		scene.overlay().showText(80)
			.text("Put its breeding food in the trough, by hand or with a funnel")
			.pointAt(util.vector().of(2.5, 1.25, 2.75))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().position(motor), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(kinetics, 32);
		herd(scene, pen, 1, 1);
		scene.overlay().showText(100)
			.text("With a shaft turning in its back, every five minutes each adult eats one food and has a baby")
			.pointAt(util.vector().blockSurface(pen, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		herd(scene, pen, 2, 2);
		scene.overlay().showText(100)
			.text("Babies grow up in twenty minutes, as in vanilla, and then breed too. It holds up to 128 animals")
			.pointAt(util.vector().blockSurface(pen, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(60);
		herd(scene, pen, 4, 4);
		scene.idle(50);

		scene.overlay().showControls(util.vector().blockSurface(pen, Direction.NORTH), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(Items.IRON_SWORD));
		scene.idle(10);
		herd(scene, pen, 2, 4);
		Vec3 front = util.vector().of(2.5, 1.5, 1.8);
		scene.world().createItemEntity(front, util.vector().of(0.05, 0.2, -0.1), new ItemStack(Items.BEEF, 3));
		scene.world().createItemEntity(front, util.vector().of(-0.05, 0.2, -0.1), new ItemStack(Items.LEATHER, 2));
		scene.overlay().showText(110)
			.text("A sword takes half the adults with their drops, and Looting counts. One is always left, and the young are left to grow up")
			.pointAt(util.vector().blockSurface(pen, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showControls(util.vector().topOf(pen), Pointing.DOWN, 50)
			.rightClick()
			.withItem(new ItemStack(Items.BUCKET));
		scene.overlay().showText(100)
			.text("Shears take every adult sheep's wool, a bucket milks cows and goats, and chickens lay eggs by themselves")
			.pointAt(util.vector().topOf(pen))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.overlay().showText(90)
			.text("Deployers can use swords, shears and buckets on it too, for a farm that runs itself")
			.pointAt(util.vector().blockSurface(pen, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(pen), Pointing.DOWN, 60)
			.rightClick()
			.withItem(wrench());
		scene.overlay().showText(90)
			.text("A Wrench opens its screen. Taking the bottle out there empties the pen, and the rest go into its output")
			.pointAt(util.vector().topOf(pen))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(90)
			.text("Fruitful, from a Librarian, makes it breed and grow faster. The Brass Animal Pen holds 512")
			.pointAt(util.vector().blockSurface(pen, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}

	/** Sets the pen's herd of cows (made in the scene's own world, as Bottled Animals are). */
	private static void herd(CreateSceneBuilder scene, BlockPos pen, int adults, int young) {
		scene.world().modifyBlockEntity(pen, AnimalPenBlockEntity.class, be -> {
			Level level = be.getLevel();
			Cow cow = level == null ? null : EntityType.COW.create(level);
			if (cow != null)
				be.setPonderHerd(BottledAnimalItem.of(cow), adults, young);
		});
	}
}
