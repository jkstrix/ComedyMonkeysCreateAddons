package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity;
import com.comedymonkey.createaddons.content.electric.GeothermalPartBlock;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scenes for the Lava Generator, the Electric Motor and the Geothermal Turbine. The visible text comes from
 * {@code comedymonkeysaddons.ponder.<scene>.*} in en_us.json, numbered in the order the texts appear below.
 */
public final class ElectricScenes {
	private ElectricScenes() {}

	public static void lavaGenerator(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("lava_generator", "Lava In, Power Out");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos gen = util.grid().at(2, 1, 2);
		scene.world().showSection(util.select().position(gen), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Lava Generator: a little lava-fired turbine that makes Forge Energy")
			.pointAt(util.vector().blockSurface(gen, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().fromTo(2, 1, 4, 2, 1, 3), Direction.NORTH);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("Lava goes in at the middle of its back, by pipe or by the bucket")
			.pointAt(util.vector().blockSurface(gen, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().modifyBlock(gen, s -> s.setValue(LavaGeneratorBlock.LIT, true), false);
		scene.overlay().showText(80)
			.text("It burns 20 mB a second, and its turbine wheel spins behind the glass while it does")
			.pointAt(util.vector().of(2.5, 1.72, 2.15))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.WEST);
		scene.idle(10);
		BlockPos motor = util.grid().at(3, 1, 2);
		scene.world().modifyBlock(motor, s -> s.setValue(ElectricMotorBlock.POWERED, true), false);
		scene.world().setKineticSpeed(util.select().fromTo(3, 1, 2, 4, 1, 2), 64);
		scene.overlay().showText(90)
			.text("Power comes out of the terminals in the middle of each side: 1,024 FE a tick, enough for an Electric Motor at 16,384 SU")
			.pointAt(util.vector().blockSurface(gen, Direction.EAST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(80)
			.text("Pipez energy pipes take its power anywhere. It only burns what's taken, so a light load saves lava")
			.pointAt(util.vector().blockSurface(gen, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.markAsFinished();
	}

	public static void electricMotor(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("electric_motor", "Plugging In");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos motor = util.grid().at(3, 1, 2);
		BlockPos gen = util.grid().at(2, 1, 2);
		Selection kinetics = util.select().fromTo(3, 1, 2, 4, 1, 2);
		scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Electric Motor turns Forge Energy into rotation, with up to 294,912 stress units: a full steam boiler's worth")
			.pointAt(util.vector().topOf(motor))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().fromTo(2, 1, 4, 2, 1, 2), Direction.DOWN);
		scene.idle(10);
		scene.world().modifyBlock(gen, s -> s.setValue(LavaGeneratorBlock.LIT, true), false);
		scene.world().modifyBlock(motor, s -> s.setValue(ElectricMotorBlock.POWERED, true), false);
		scene.world().setKineticSpeed(kinetics, 64);
		scene.overlay().showText(80)
			.text("Feed it power on any side: here, straight from a Lava Generator")
			.pointAt(util.vector().blockSurface(motor, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showControls(util.vector().topOf(motor), Pointing.DOWN, 60)
			.rightClick()
			.withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
		scene.overlay().showText(90)
			.text("Right-click it with a Wrench to set its speed, its stress and which way it turns")
			.pointAt(util.vector().topOf(motor))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().setKineticSpeed(kinetics, 128);
		scene.overlay().showText(90)
			.text("Speed snaps to Create's standard speeds. Stress snaps to what the generators make, so it's easy to match a motor to its supply")
			.pointAt(util.vector().topOf(motor))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(80)
			.text("The more stress it's set to give, the more power it draws: 1 FE a tick for every 16 SU")
			.pointAt(util.vector().blockSurface(motor, Direction.EAST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().modifyBlock(gen, s -> s.setValue(LavaGeneratorBlock.LIT, false), false);
		scene.world().modifyBlock(motor, s -> s.setValue(ElectricMotorBlock.POWERED, false), false);
		scene.world().setKineticSpeed(kinetics, 0);
		scene.overlay().showText(80)
			.text("Short of power, it stops, and starts again once it has half a second's worth stored")
			.pointAt(util.vector().topOf(motor))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.markAsFinished();
	}

	public static void geothermalTurbine(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("geothermal_turbine", "Full Steam Ahead");
		scene.configureBasePlate(0, 0, 7);
		scene.scaleSceneView(0.8f);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos core = util.grid().at(3, 2, 3);
		BlockPos controller = util.grid().at(3, 2, 2);
		Selection turbine = util.select().fromTo(2, 1, 4, 4, 3, 2);

		// Built up a layer at a time, the core first
		scene.world().showSection(util.select().fromTo(2, 1, 4, 4, 1, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Geothermal Turbine is a 3x3x3 machine. Start with a layer of Geothermal Casings...")
			.pointAt(util.vector().topOf(util.grid().at(3, 1, 3)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().position(core), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
			.text("...with the Turbine Core in the very middle...")
			.pointAt(util.vector().centerOf(core))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		scene.world().showSection(util.select().fromTo(2, 2, 4, 4, 2, 3).add(util.select().position(2, 2, 2))
			.add(util.select().position(4, 2, 2)), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().fromTo(2, 3, 4, 4, 3, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(70)
			.text("...and casings all round it")
			.pointAt(util.vector().topOf(util.grid().at(3, 3, 3)))
			.placeNearTarget();
		scene.idle(80);

		scene.world().showSection(util.select().position(controller), Direction.SOUTH);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Controller goes in the middle of the front, facing you")
			.pointAt(util.vector().blockSurface(controller, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// Formed: the casings disappear into the one machine the controller draws
		scene.world().modifyBlocks(turbine, s -> s.hasProperty(GeothermalPartBlock.FORMED) ? s.setValue(GeothermalPartBlock.FORMED, true) : s, false);
		scene.effects().indicateSuccess(controller);
		scene.overlay().showText(80)
			.text("Within a second it forms into one big machine")
			.pointAt(util.vector().blockSurface(controller, Direction.NORTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().showSection(util.select().fromTo(0, 1, 3, 1, 1, 3), Direction.EAST);
		scene.world().showSection(util.select().fromTo(3, 1, 6, 3, 1, 5), Direction.NORTH);
		scene.idle(15);
		scene.world().modifyBlockEntityNBT(util.select().position(controller), GeothermalControllerBlockEntity.class, nbt -> {
			nbt.put("Lava", fluid("minecraft:lava", 12000));
			nbt.put("Water", fluid("minecraft:water", 14000));
		});
		scene.overlay().showText(90)
			.text("Pipe lava and water into any casing. The gauges on the front show how much it has of each")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 1, 3), Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().modifyBlock(controller, s -> s.setValue(GeothermalControllerBlock.LIT, true), false);
		ParticleEmitter steam = scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.08, 0));
		ParticleEmitter flame = scene.effects().simpleParticleEmitter(ParticleTypes.FLAME, new Vec3(-0.01, 0.01, 0));
		scene.effects().emitParticles(util.vector().of(3.5, 3.97, 3.5), steam, 0.6f, 160);
		scene.effects().emitParticles(util.vector().of(1.98, 2.35, 3.6), flame, 0.4f, 160);
		scene.overlay().showText(90)
			.text("Lava boils the water to steam that turns the turbine: 46,080 FE a tick, ten times as much from each bucket of lava as a Lava Generator")
			.pointAt(util.vector().topOf(core).add(0, 1, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(5, 2, 3, 6, 2, 3), Direction.WEST);
		scene.idle(10);
		BlockPos motor = util.grid().at(5, 2, 3);
		scene.world().modifyBlock(motor, s -> s.setValue(ElectricMotorBlock.POWERED, true), false);
		scene.world().setKineticSpeed(util.select().fromTo(5, 2, 3, 6, 2, 3), 128);
		scene.overlay().showText(90)
			.text("It powers anything touching it, or energy pipes on any casing: enough for two and a half Electric Motors flat out")
			.pointAt(util.vector().blockSurface(motor, Direction.UP))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(80)
			.text("It uses 90 mB of lava and 250 mB of water a second: two fully upgraded Lava Wellsprings keep it going")
			.pointAt(util.vector().topOf(util.grid().at(0, 1, 3)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.markAsFinished();
	}

	private static CompoundTag fluid(String id, int amount) {
		CompoundTag inner = new CompoundTag();
		inner.putString("id", id);
		inner.putInt("amount", amount);
		CompoundTag tank = new CompoundTag();
		tank.put("Fluid", inner);
		return tank;
	}
}
