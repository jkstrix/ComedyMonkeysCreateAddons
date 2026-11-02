package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity;
import com.comedymonkey.createaddons.content.electric.GeothermalPartBlock;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlock;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
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
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
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
		scene.world().modifyBlock(gen, s -> s.setValue(LavaGeneratorBlock.FUELLED, true), false);
		scene.overlay().showText(80)
			.text("Lava goes in at the middle of its back, by pipe or by the bucket")
			.pointAt(util.vector().blockSurface(gen, Direction.SOUTH))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().modifyBlock(gen, s -> s.setValue(LavaGeneratorBlock.LIT, true), false);
		scene.overlay().showText(80)
			.text("It burns 20 mB a second, flat out whenever it has lava, and its turbine wheel spins behind the window")
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
			.text("Power Lines take its power anywhere, even from far off while its chunk isn't loaded. What its store can't hold is lost, so give it something to power")
			.pointAt(util.vector().blockSurface(gen, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.markAsFinished();
	}

	/** The Clockwork Dynamo (design/specs/dynamo.md D28): rotation in, power out at three quarters, and what that means. */
	public static void clockworkDynamo(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title("clockwork_dynamo", "Charging Up");
		scene.configureBasePlate(0, 0, 7);
		scene.showBasePlate();
		scene.idle(5);

		// the row stands a block up (the Water Wheel is two blocks across), the dynamo and motor on Andesite Casings
		BlockPos wheel = util.grid().at(1, 2, 2), clutch = util.grid().at(2, 2, 2), dynamo = util.grid().at(3, 2, 2),
			motor = util.grid().at(4, 2, 2);
		Selection drive = util.select().fromTo(1, 2, 2, 3, 2, 2), driven = util.select().fromTo(4, 2, 2, 5, 2, 2);
		scene.world().showSection(util.select().fromTo(1, 1, 2, 3, 2, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(80)
			.text("The Clockwork Dynamo turns rotation into Forge Energy: the Electric Motor, run backwards")
			.pointAt(util.vector().topOf(dynamo))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		scene.world().setKineticSpeed(drive, 8);
		scene.world().modifyBlock(dynamo, s -> s.setValue(com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlock.WORKING, true), false);
		scene.overlay().showText(90)
			.text("Its shaft goes in at the front. It takes the stress it's set to, at any speed, and its aerial springs up while it makes power")
			.pointAt(util.vector().blockSurface(dynamo, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showControls(util.vector().topOf(dynamo), Pointing.DOWN, 60)
			.rightClick()
			.withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
		scene.overlay().showText(90)
			.text("Right-click it with a Wrench to set how much it takes: from 256 SU, one water wheel, up to 294,912")
			.pointAt(util.vector().topOf(dynamo))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(90)
			.text("It makes 1 FE a tick for every 21⅓ SU: three quarters of what a motor would draw for the same stress")
			.pointAt(util.vector().topOf(dynamo))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().showSection(util.select().fromTo(4, 1, 2, 5, 2, 2), Direction.DOWN);
		scene.idle(15);
		scene.overlay().showText(90)
			.text("Power goes out through its base, opposite the shaft: into a Power Line, a battery or a machine")
			.pointAt(util.vector().blockSurface(dynamo, Direction.EAST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().modifyBlock(motor, s -> s.setValue(ElectricMotorBlock.POWERED, true), false);
		scene.world().setKineticSpeed(driven, 8);
		scene.idle(30);
		scene.world().modifyBlock(motor, s -> s.setValue(ElectricMotorBlock.POWERED, false), false);
		scene.world().setKineticSpeed(driven, 0);
		scene.overlay().showText(100)
			.text("A motor set to the same stress needs a third more than the dynamo makes, so it soon runs short: something is always lost on the way")
			.pointAt(util.vector().topOf(motor))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(110);

		scene.world().modifyBlock(clutch, s -> s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, true), false);
		scene.world().setKineticSpeed(util.select().position(dynamo), 0);
		scene.world().modifyBlock(dynamo, s -> s.setValue(com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlock.WORKING, false), false);
		scene.overlay().showText(90)
			.text("It has no redstone control: a Clutch or Gearshift on its shaft switches it off")
			.pointAt(util.vector().topOf(clutch))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(110)
			.text("Far from any player it keeps feeding its Power Lines, but only from water wheels and windmills: motors and engines count for nothing while it's away")
			.pointAt(util.vector().topOf(wheel))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(120);
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
		scene.world().modifyBlock(gen, s -> s.setValue(LavaGeneratorBlock.LIT, true).setValue(LavaGeneratorBlock.FUELLED, true), false);
		scene.world().modifyBlock(motor, s -> s.setValue(ElectricMotorBlock.POWERED, true), false);
		scene.world().setKineticSpeed(kinetics, 64);
		scene.overlay().showText(80)
			.text("Power goes in through its base, the flange opposite the shaft: here, straight from a Lava Generator")
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

		// the generator has run out of lava
		scene.world().modifyBlock(gen, s -> s.setValue(LavaGeneratorBlock.LIT, false).setValue(LavaGeneratorBlock.FUELLED, false), false);
		scene.world().modifyBlock(motor, s -> s.setValue(ElectricMotorBlock.POWERED, false), false);
		scene.world().setKineticSpeed(kinetics, 0);
		scene.overlay().showText(80)
			.text("Short of power, it stops, and starts again once it has a good reserve stored")
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

		// Everything connects through the port flanges under the base: pipes run in under the turbine from the Wellsprings
		scene.world().showSection(util.select().position(0, 1, 3).add(util.select().position(3, 1, 6)), Direction.DOWN);
		scene.idle(10);
		scene.world().setBlock(util.grid().at(0, 0, 3), pipe(Direction.UP, Direction.EAST), false);
		scene.world().setBlock(util.grid().at(1, 0, 3), pipe(Direction.WEST, Direction.EAST), false);
		scene.world().setBlock(util.grid().at(2, 0, 3), pipe(Direction.WEST, Direction.UP), false);
		scene.world().setBlock(util.grid().at(3, 0, 6), pipe(Direction.UP, Direction.NORTH), false);
		scene.world().setBlock(util.grid().at(3, 0, 5), pipe(Direction.SOUTH, Direction.NORTH), false);
		scene.world().setBlock(util.grid().at(3, 0, 4), pipe(Direction.SOUTH, Direction.UP), false);
		scene.idle(15);
		scene.world().modifyBlockEntityNBT(util.select().position(controller), GeothermalControllerBlockEntity.class, nbt -> {
			nbt.put("Lava", fluid("minecraft:lava", 12000));
			nbt.put("Water", fluid("minecraft:water", 14000));
		});
		scene.overlay().showText(90)
			.text("Lava, water and power connect only through the ports under its base: pipe lava and water in from below. The gauges on the front show how much it has of each")
			.pointAt(util.vector().topOf(util.grid().at(1, 0, 3)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.world().modifyBlock(controller, s -> s.setValue(GeothermalControllerBlock.LIT, true), false);
		ParticleEmitter steam = scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.08, 0));
		ParticleEmitter flame = scene.effects().simpleParticleEmitter(ParticleTypes.FLAME, new Vec3(-0.01, 0.01, 0));
		scene.effects().emitParticles(util.vector().of(3.5, 3.97, 3.5), steam, 0.6f, 160);
		// out of the firebox grates on its west side, low down
		scene.effects().emitParticles(util.vector().of(2.04, 1.6, 3.6), flame, 0.4f, 160);
		scene.overlay().showText(90)
			.text("Lava boils the water to steam that turns the turbine: 46,080 FE a tick, ten times as much from each bucket of lava as a Lava Generator")
			.pointAt(util.vector().topOf(core).add(0, 1, 0))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// A motor set into the floor under the base, its shaft running out to the east
		BlockPos motor = util.grid().at(4, 0, 3);
		scene.world().setBlock(motor, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState()
			.setValue(DirectionalKineticBlock.FACING, Direction.EAST).setValue(ElectricMotorBlock.POWERED, true), false);
		BlockState shaft = create("shaft").setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X);
		scene.world().setBlock(util.grid().at(5, 0, 3), shaft, false);
		scene.world().setBlock(util.grid().at(6, 0, 3), shaft, false);
		scene.world().setKineticSpeed(util.select().fromTo(4, 0, 3, 6, 0, 3), 128);
		scene.idle(10);
		scene.overlay().showText(90)
			.text("It powers anything under its base, or energy cables at the ports there: enough for two and a half Electric Motors flat out")
			.pointAt(util.vector().topOf(motor))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.overlay().showText(80)
			.text("At full power it uses 90 mB of lava and 250 mB of water a second: two fully upgraded Lava Wellsprings keep it going. Turn it down with a wrench on the controller to use less")
			.pointAt(util.vector().topOf(util.grid().at(0, 1, 3)))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);
		scene.markAsFinished();
	}

	/** One of Create's blocks by id (through the registry: Create's AllBlocks needs Registrate on the classpath). */
	private static BlockState create(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", id)).defaultBlockState();
	}

	/** A Create fluid pipe joined on just the given sides. */
	private static BlockState pipe(Direction... sides) {
		BlockState state = create("fluid_pipe");
		for (Direction side : Direction.values())
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), false);
		for (Direction side : sides)
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), true);
		return state;
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
