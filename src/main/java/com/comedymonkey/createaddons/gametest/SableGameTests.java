package com.comedymonkey.createaddons.gametest;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The machines on Sable physics objects (only registered when Sable is installed). */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SableGameTests {
	private static final String COURSE = "trade_bot_course";

	private SableGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(SableGameTests.class);
	}

	/**
	 * A Magma Engine, an Electric Motor and Create's Creative Motor, each turning two shafts, are each made into a
	 * physics object: all three keep turning their shafts there.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void generatorsKeepTurningOnAPhysicsObject(GameTestHelper helper) {
		BlockPos creative = new BlockPos(1, 1, 1);
		BlockPos engine = new BlockPos(1, 1, 3);
		BlockPos motor = new BlockPos(1, 1, 5);
		place(helper, creative, create("creative_motor").setValue(DirectionalKineticBlock.FACING, Direction.EAST));
		place(helper, engine, ModBlocks.MAGMA_ENGINE.get().defaultBlockState().setValue(MagmaEngineBlock.FACING, Direction.EAST));
		place(helper, motor, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		helper.runAtTickTime(2, () -> {
			IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(engine), null);
			if (tank == null)
				helper.fail("no fluid handler");
			tank.fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.EXECUTE);
			charge(helper, helper.absolutePos(motor));
		});
		helper.runAtTickTime(20, () -> {
			for (BlockPos pos : List.of(creative, engine, motor))
				assertTurning(helper, helper.absolutePos(pos.east(2)), "before assembly, " + pos);
		});
		BlockPos[] moved = new BlockPos[3];
		helper.runAtTickTime(21, () -> {
			moved[0] = assemble(helper, creative);
			moved[1] = assemble(helper, engine);
			moved[2] = assemble(helper, motor);
		});
		// an Electric Motor uses a lot of power: top it up on the physics object
		helper.runAtTickTime(30, () -> charge(helper, moved[2]));
		helper.runAtTickTime(60, () -> {
			assertTurning(helper, moved[0].east(2), "Creative Motor on a physics object");
			assertTurning(helper, moved[1].east(2), "Magma Engine on a physics object");
			assertTurning(helper, moved[2].east(2), "Electric Motor on a physics object");
			helper.succeed();
		});
	}

	/**
	 * The Star Reactor on an Aeronautics ship (P14, D23: its edge is fuel-free power anywhere, airships included): fuelled,
	 * then made a physics object, it keeps running there flat out (a tick of star burned and 8,192 FE made a tick), keeps
	 * its star, and takes another by its item handler.
	 */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void starReactorRunsOnAShip(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 2, 7);
		helper.setBlock(at, com.comedymonkey.createaddons.registry.ModWastes.STAR_REACTOR.get().defaultBlockState()
			.setValue(com.comedymonkey.createaddons.content.wastes.StarReactorBlock.FACING, Direction.UP));
		helper.runAtTickTime(2, () -> {
			var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(at), Direction.NORTH);
			if (items == null || !items.insertItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR), false).isEmpty())
				helper.fail("didn't take the star");
		});
		BlockPos[] moved = new BlockPos[1];
		long[] before = new long[2];
		helper.runAtTickTime(10, () -> moved[0] = com.comedymonkey.createaddons.compat.sable.SableAssembly.assemble(helper.getLevel(), helper.absolutePos(at),
			List.of(helper.absolutePos(at))));
		helper.runAtTickTime(20, () -> {
			if (!(helper.getLevel().getBlockEntity(moved[0]) instanceof com.comedymonkey.createaddons.content.wastes.StarReactorBlockEntity be)) {
				helper.fail("no Star Reactor on the physics object at " + moved[0]);
				return;
			}
			if (be.burn() <= 0)
				helper.fail("its star didn't come with it");
			// drained, so a full tick's output shows every tick
			be.energy().setEnergy(0);
			before[0] = be.burn();
		});
		helper.runAtTickTime(40, () -> {
			var be = (com.comedymonkey.createaddons.content.wastes.StarReactorBlockEntity) helper.getLevel().getBlockEntity(moved[0]);
			long burned = before[0] - be.burn(), made = be.energy().getEnergyStored();
			if (burned != 20)
				helper.fail("on the ship it burned " + burned + " ticks of star in 20");
			if (made != 20L * com.comedymonkey.createaddons.config.CMConfig.SERVER.starReactorOutput())
				helper.fail("on the ship it made " + made + " FE in 20 ticks");
			helper.succeed();
		});
	}

	/** The generator at {@code pos} with two shafts east of it. */
	private static void place(GameTestHelper helper, BlockPos pos, BlockState generator) {
		helper.setBlock(pos, generator);
		BlockState shaft = create("shaft").setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X);
		helper.setBlock(pos.east(), shaft);
		helper.setBlock(pos.east(2), shaft);
	}

	private static BlockState create(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", id)).defaultBlockState();
	}

	private static void charge(GameTestHelper helper, BlockPos absolute) {
		if (!(helper.getLevel().getBlockEntity(absolute) instanceof ElectricMotorBlockEntity be))
			helper.fail("no motor at " + absolute);
		else
			be.getEnergy().receiveEnergy(Integer.MAX_VALUE, false);
	}

	/** Makes the generator at {@code pos} and its shafts a physics object, as Simulated's Physics Assembler does; returns where the generator went. */
	private static BlockPos assemble(GameTestHelper helper, BlockPos pos) {
		BlockPos anchor = helper.absolutePos(pos);
		List<BlockPos> blocks = List.of(anchor, anchor.east(), anchor.east(2));
		return com.comedymonkey.createaddons.compat.sable.SableAssembly.assemble(helper.getLevel(), anchor, blocks);
	}

	private static void assertTurning(GameTestHelper helper, BlockPos absolute, String what) {
		if (!(helper.getLevel().getBlockEntity(absolute) instanceof KineticBlockEntity shaft))
			helper.fail(what + ": no shaft at " + absolute);
		else if (shaft.getSpeed() == 0)
			helper.fail(what + ": the far shaft isn't turning");
	}
}
