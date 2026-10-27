package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity;
import com.comedymonkey.createaddons.content.cage.MonsterJarItem;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.comedymonkey.createaddons.content.tank.FishTankBlock;
import com.comedymonkey.createaddons.content.tank.FishTankBlockEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for the Monster Cages ("Monster Cages", "Flooded Monster Cages", "Containment") and the Fish Tanks, each
 * on a 5 x 5 plate with its window towards the camera (north) and a motor behind it, as the Animal Pen's. The Ponder
 * world doesn't run them, so each step sets what's inside. The visible text comes from
 * {@code comedymonkeysaddons.ponder.<scene>.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class CageScenes {
	private CageScenes() {}

	private static ItemStack wrench() {
		return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench")));
	}

	// ---------------------------------------------------------------- Monster Cages

	public static void monsterCage(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("monster_cage", "Monster Cages");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos cage = util.grid().at(2, 1, 2);
		BlockPos motor = util.grid().at(2, 1, 3);
		Selection kinetics = util.select().fromTo(2, 1, 2, 2, 1, 3);
		scene.world().showSection(util.select().position(cage), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Monster Cage keeps one jarred monster, and brings in more of the same kind")
			.pointAt(util.vector().blockSurface(cage, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		Vec3 zombieAt = util.vector().of(3.8, 1, 0.9);
		ElementLink<EntityElement> zombie = scene.world().createEntity(level -> standing(EntityType.ZOMBIE.create(level), zombieAt));
		scene.idle(15);
		scene.overlay().showControls(zombieAt.add(0, 2.2, 0), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.IRON_JAR.get()));
		scene.overlay().showText(70)
			.text("Catch a monster in an Iron Jar, at any health")
			.pointAt(zombieAt.add(0, 1.2, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(45);
		scene.world().modifyEntity(zombie, Entity::discard);
		scene.idle(40);

		scene.overlay().showControls(util.vector().topOf(cage), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.IRON_JAR.get()));
		scene.idle(10);
		horde(scene, cage, EntityType.ZOMBIE, false, 1, 0);
		scene.overlay().showText(70)
			.text("Then use the full jar on the cage to put the monster in")
			.pointAt(util.vector().topOf(cage))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		scene.overlay().showControls(util.vector().topOf(cage), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.MONSTER_CHOW.get()));
		scene.idle(10);
		supplies(scene, cage, new ItemStack(ModItems.MONSTER_CHOW.get(), 32), 0);
		scene.overlay().showText(90)
			.text("Feed it Monster Chow, mixed from Wheat Flour, Cinder Flour and Bone Meal, by hand or with a funnel")
			.pointAt(util.vector().of(2.5, 1.25, 2.6))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().position(motor), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(kinetics, 32);
		horde(scene, cage, EntityType.ZOMBIE, false, 3, 0);
		scene.overlay().showText(100)
			.text("With a shaft turning in its back, another arrives every 20 seconds at 64 RPM, sooner the faster it turns. Each one eats chow")
			.pointAt(util.vector().blockSurface(cage, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(70);
		horde(scene, cage, EntityType.ZOMBIE, false, 8, 0);
		scene.idle(40);

		scene.overlay().showControls(util.vector().blockSurface(cage, Direction.NORTH), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(Items.IRON_SWORD));
		scene.idle(10);
		horde(scene, cage, EntityType.ZOMBIE, false, 4, 0);
		Vec3 front = util.vector().of(2.5, 1.5, 1.8);
		scene.world().createItemEntity(front, util.vector().of(0.05, 0.2, -0.1), new ItemStack(Items.ROTTEN_FLESH, 4));
		scene.overlay().showText(110)
			.text("A sword culls half of them for their real drops, and Looting and Fire Aspect count. One is always left")
			.pointAt(util.vector().blockSurface(cage, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.overlay().showText(90)
			.text("Deployers can cull with a sword and catch with jars too, and funnels take the drops out")
			.pointAt(util.vector().blockSurface(cage, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(cage), Pointing.DOWN, 60)
			.rightClick()
			.withItem(wrench());
		scene.overlay().showText(90)
			.text("A Wrench opens its screen. Taking the jar out there empties the cage, and the rest go into its output")
			.pointAt(util.vector().topOf(cage))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(100)
			.text("The Brass Monster Cage holds 256 and also takes Nether, End and rare monsters. Monstrous, from a Librarian, makes either faster")
			.pointAt(util.vector().blockSurface(cage, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		scene.markAsFinished();
	}

	public static void floodedCage(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("flooded_monster_cage", "Flooded Monster Cages");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos cage = util.grid().at(2, 1, 2);
		BlockPos motor = util.grid().at(2, 1, 3);
		Selection kinetics = util.select().fromTo(2, 1, 2, 2, 1, 3);
		scene.world().showSection(util.select().position(cage), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("A Flooded Monster Cage holds drowned, and the Flooded Brass one guardians and elder guardians too")
			.pointAt(util.vector().blockSurface(cage, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		supplies(scene, cage, new ItemStack(ModItems.MONSTER_CHOW.get(), 32), 4000);
		scene.overlay().showText(90)
			.text("Pipe water in on any side but the back. Each new monster uses 250 mB of it")
			.pointAt(util.vector().blockSurface(cage, Direction.EAST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(cage), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.IRON_JAR.get()));
		scene.idle(10);
		horde(scene, cage, EntityType.ZOMBIE, false, 1, 0);
		scene.overlay().showText(80)
			.text("A zombie put in one drowns, and becomes a drowned")
			.pointAt(util.vector().blockSurface(cage, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(60);
		horde(scene, cage, EntityType.DROWNED, false, 1, 0);
		scene.idle(30);

		scene.world().showSection(util.select().position(motor), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(kinetics, 32);
		horde(scene, cage, EntityType.DROWNED, false, 6, 1);
		scene.overlay().showText(110)
			.text("Now and then a drowned comes armed, and throws its trident out of the front at you. Cull the armed ones first, or stand behind it")
			.pointAt(util.vector().blockSurface(cage, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);
		scene.markAsFinished();
	}

	public static void containment(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("netherite_monster_cage", "Containment");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos cage = util.grid().at(2, 1, 2);
		BlockPos motor = util.grid().at(2, 1, 3);
		BlockPos battery = util.grid().at(1, 1, 2);
		Selection kinetics = util.select().fromTo(2, 1, 2, 2, 1, 3);
		scene.world().showSection(util.select().position(cage), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Netherite Monster Cage holds Withers, caught in a Netherite Jar")
			.pointAt(util.vector().blockSurface(cage, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showControls(util.vector().topOf(cage), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.NETHERITE_JAR.get()));
		scene.idle(10);
		horde(scene, cage, EntityType.WITHER, true, 1, 0);
		scene.world().showSection(util.select().position(battery), Direction.EAST);
		scene.idle(10);
		field(scene, cage, true, 0);
		scene.overlay().showText(110)
			.text("It needs FE on any side but the back: 2,048 FE/t holds the Withers in a containment field, and more brings new ones in")
			.pointAt(util.vector().blockSurface(battery, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);

		scene.world().showSection(util.select().position(motor), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(kinetics, 32);
		supplies(scene, cage, new ItemStack(ModItems.MONSTER_CHOW.get(), 64), 0);
		horde(scene, cage, EntityType.WITHER, true, 2, 0);
		scene.overlay().showText(100)
			.text("With the shaft turning, new Withers arrive, each one eating 64 Monster Chow. Cull them for Nether Stars")
			.pointAt(util.vector().blockSurface(cage, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		field(scene, cage, false, 6000);
		scene.overlay().showText(120)
			.text("If the power fails, everyone nearby gets 5 minutes of warning. Then one Wither breaks out, and another every game day until the power is back")
			.pointAt(util.vector().blockSurface(cage, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(130);

		field(scene, cage, true, 0);
		scene.overlay().showText(100)
			.text("It's blast-proof, only its owner can open or break it, and never while the field is down")
			.pointAt(util.vector().topOf(cage))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		scene.markAsFinished();
	}

	// ---------------------------------------------------------------- Fish Tanks

	public static void fishTank(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("fish_tank", "Fish Tanks");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos tank = util.grid().at(2, 1, 2);
		BlockPos motor = util.grid().at(2, 1, 3);
		Selection kinetics = util.select().fromTo(2, 1, 2, 2, 1, 3);
		scene.world().showSection(util.select().position(tank), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Fish Tank keeps one fish, squid or axolotl, and breeds a whole school from it")
			.pointAt(util.vector().blockSurface(tank, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		Vec3 codAt = util.vector().of(3.8, 1, 0.9);
		ElementLink<EntityElement> cod = scene.world().createEntity(level -> standing(EntityType.COD.create(level), codAt));
		scene.idle(15);
		scene.overlay().showControls(codAt.add(0, 0.8, 0), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.CAPTURE_BOTTLE.get()));
		scene.overlay().showText(70)
			.text("Catch one with a Capture Bottle, then use the bottle on the tank")
			.pointAt(codAt.add(0, 0.3, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(45);
		scene.world().modifyEntity(cod, Entity::discard);
		scene.idle(20);
		school(scene, tank, EntityType.COD, 1, 0, 0);
		scene.idle(30);

		scene.world().modifyBlockEntity(tank, FishTankBlockEntity.class,
			be -> be.setPonderSupplies(new ItemStack(ModItems.FISH_FOOD.get(), 16), 4000));
		scene.overlay().showText(100)
			.text("Pipe water in on any side but the back, or pour in water buckets, and feed it Fish Food, mixed from Wheat Flour, Dried Kelp and Bone Meal")
			.pointAt(util.vector().blockSurface(tank, Direction.EAST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.world().showSection(util.select().position(motor), Direction.NORTH);
		scene.idle(10);
		scene.world().setKineticSpeed(kinetics, 32);
		school(scene, tank, EntityType.COD, 2, 0, 0);
		scene.overlay().showText(110)
			.text("With a shaft turning in its back, every five minutes at 64 RPM each adult eats one Fish Food and has young, using 250 mB of water. It holds up to 128")
			.pointAt(util.vector().blockSurface(tank, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(70);
		school(scene, tank, EntityType.COD, 8, 0, 0);
		scene.idle(50);

		scene.overlay().showControls(util.vector().blockSurface(tank, Direction.NORTH), Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(Items.IRON_SWORD));
		scene.idle(10);
		school(scene, tank, EntityType.COD, 4, 0, 0);
		Vec3 front = util.vector().of(2.5, 1.5, 1.8);
		scene.world().createItemEntity(front, util.vector().of(0.05, 0.2, -0.1), new ItemStack(Items.COD, 4));
		scene.overlay().showText(100)
			.text("A sword culls half for their drops, and a water bucket scoops one out alive. One is always left. Deployers can do both")
			.pointAt(util.vector().blockSurface(tank, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		school(scene, tank, EntityType.AXOLOTL, 4, 2, 1);
		scene.overlay().showText(100)
			.text("Axolotls' young grow up in twenty minutes, and one in 1,200 is born blue. A water bucket scoops the blue ones first")
			.pointAt(util.vector().blockSurface(tank, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		school(scene, tank, EntityType.GLOW_SQUID, 3, 0, 0);
		scene.world().modifyBlock(tank, state -> state.setValue(FishTankBlock.GLOWING, true), false);
		scene.overlay().showText(80)
			.text("A tank of glow squid gives off light")
			.pointAt(util.vector().blockSurface(tank, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showControls(util.vector().topOf(tank), Pointing.DOWN, 60)
			.rightClick()
			.withItem(wrench());
		scene.overlay().showText(100)
			.text("A Wrench opens its screen. The Brass Fish Tank holds 512, and Fruitful, from a Librarian, makes either faster")
			.pointAt(util.vector().topOf(tank))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);
		scene.markAsFinished();
	}

	// ---------------------------------------------------------------- helpers

	/** An entity standing at {@code at}, turned towards the camera. */
	private static Entity standing(Entity entity, Vec3 at) {
		entity.setPos(at.x, at.y, at.z);
		entity.setYRot(160);
		if (entity instanceof net.minecraft.world.entity.LivingEntity living)
			living.yBodyRot = living.yHeadRot = 160;
		return entity;
	}

	/** Sets the cage's monsters (made in the scene's own world, as jars are). */
	private static void horde(CreateSceneBuilder scene, BlockPos cage, EntityType<?> type, boolean netherite, int count, int armed) {
		scene.world().modifyBlockEntity(cage, MonsterCageBlockEntity.class, be -> {
			Level level = be.getLevel();
			Entity monster = level == null ? null : type.create(level);
			if (monster != null)
				be.setPonderHorde(((MonsterJarItem) (netherite ? ModItems.NETHERITE_JAR : ModItems.IRON_JAR).get()).fill(monster), count, armed);
		});
	}

	private static void supplies(CreateSceneBuilder scene, BlockPos pos, ItemStack food, int water) {
		scene.world().modifyBlockEntity(pos, MonsterCageBlockEntity.class, be -> be.setPonderSupplies(food, water));
	}

	private static void field(CreateSceneBuilder scene, BlockPos cage, boolean up, int ticksToEscape) {
		scene.world().modifyBlockEntity(cage, MonsterCageBlockEntity.class, be -> be.setPonderField(up, ticksToEscape));
	}

	/** Sets the tank's school (made in the scene's own world, as Bottled Animals are). */
	private static void school(CreateSceneBuilder scene, BlockPos tank, EntityType<?> type, int adults, int young, int blue) {
		scene.world().modifyBlockEntity(tank, FishTankBlockEntity.class, be -> {
			Level level = be.getLevel();
			Entity creature = level == null ? null : type.create(level);
			if (creature != null)
				be.setPonderSchool(BottledAnimalItem.of(creature), adults, young, blue);
		});
	}
}
