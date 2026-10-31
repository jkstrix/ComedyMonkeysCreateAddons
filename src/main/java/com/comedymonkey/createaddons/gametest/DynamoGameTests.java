package com.comedymonkey.createaddons.gametest;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlock;
import com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Clockwork Dynamo in a real world (design/specs/dynamo.md): the power it makes off a shaft, out of its base only,
 * nothing when overstressed, and which of its shaft's power counts on paper. A Creative Motor (16 RPM, more stress
 * than anything needs, and in the free sources' tag) turns it.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DynamoGameTests {
	private static final String COURSE = "trade_bot_course";

	private DynamoGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(DynamoGameTests.class);
	}

	/** A Creative Motor at (1,1,3) facing east turns a dynamo at (2,1,3), whose shaft faces west back at it. */
	private static void creativeMotorAndDynamo(GameTestHelper helper, BlockPos motor, BlockPos dynamo) {
		BlockState creative = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor")).defaultBlockState()
			.setValue(BlockStateProperties.FACING, Direction.EAST);
		helper.setBlock(motor, creative);
		helper.setBlock(dynamo, ModBlocks.CLOCKWORK_DYNAMO.get().defaultBlockState().setValue(ClockworkDynamoBlock.FACING, Direction.WEST));
	}

	private static ClockworkDynamoBlockEntity dynamo(GameTestHelper helper, BlockPos pos) {
		if (!(helper.getBlockEntity(pos) instanceof ClockworkDynamoBlockEntity d))
			throw new net.minecraft.gametest.framework.GameTestAssertException("no dynamo at " + pos);
		return d;
	}

	/** Turning at its default 256 SU it makes 12 FE a tick: its store fills by that, and its lamp lights. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void dynamoMakesPowerFromItsShaft(GameTestHelper helper) {
		BlockPos motor = new BlockPos(1, 1, 3), pos = new BlockPos(2, 1, 3);
		creativeMotorAndDynamo(helper, motor, pos);
		helper.runAtTickTime(10, () -> dynamo(helper, pos).getEnergy().setEnergy(0));
		helper.runAtTickTime(50, () -> {
			ClockworkDynamoBlockEntity d = dynamo(helper, pos);
			helper.assertBlockProperty(pos, ClockworkDynamoBlock.WORKING, true);
			int stored = d.getEnergy().getEnergyStored();
			// 40 ticks at 12 FE a tick, give or take a tick either side
			if (stored < 12 * 39 || stored > 12 * 41)
				helper.fail("expected about " + 12 * 40 + " FE after 40 ticks, got " + stored);
			helper.succeed();
		});
	}

	/** Power goes out through its base only: no other face gives any, and nothing can be pushed in anywhere. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void dynamoGivesThroughItsBaseOnly(GameTestHelper helper) {
		BlockPos motor = new BlockPos(1, 1, 3), pos = new BlockPos(2, 1, 3);
		creativeMotorAndDynamo(helper, motor, pos);
		helper.runAtTickTime(30, () -> {
			for (Direction side : Direction.values()) {
				IEnergyStorage e = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), side);
				boolean base = side == Direction.EAST;
				if (base != (e != null))
					helper.fail((base ? "no power out of the base" : "power out of the " + side + " face"));
				if (e != null && (e.canReceive() || e.receiveEnergy(100, true) != 0))
					helper.fail("power can be pushed into a dynamo");
				if (e != null && e.extractEnergy(10, true) <= 0)
					helper.fail("nothing to take from the base");
			}
			helper.succeed();
		});
	}

	/** Its base against an Electric Motor's base: the motor gets the power. */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void dynamoPowersAMotorAgainstItsBase(GameTestHelper helper) {
		BlockPos motor = new BlockPos(1, 1, 3), pos = new BlockPos(2, 1, 3), electric = new BlockPos(3, 1, 3);
		creativeMotorAndDynamo(helper, motor, pos);
		helper.setBlock(electric, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		helper.runAtTickTime(40, () -> {
			if (!(helper.getBlockEntity(electric) instanceof ElectricMotorBlockEntity m) || m.getEnergy().getEnergyStored() <= 0)
				helper.fail("the motor got no power from the dynamo");
			helper.succeed();
		});
	}

	/** Set to more than the shaft has (the Creative Motor gives 262,144 SU at 16 RPM): overstressed, it makes nothing. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void overstressedDynamoMakesNothing(GameTestHelper helper) {
		BlockPos motor = new BlockPos(1, 1, 3), pos = new BlockPos(2, 1, 3);
		creativeMotorAndDynamo(helper, motor, pos);
		helper.runAtTickTime(10, () -> dynamo(helper, pos).applySettings(294912));
		helper.runAtTickTime(30, () -> dynamo(helper, pos).getEnergy().setEnergy(0));
		helper.runAtTickTime(60, () -> {
			ClockworkDynamoBlockEntity d = dynamo(helper, pos);
			if (!d.isOverStressed())
				helper.fail("not overstressed at 294,912 SU on a Creative Motor");
			helper.assertBlockProperty(pos, ClockworkDynamoBlock.WORKING, false);
			if (d.getEnergy().getEnergyStored() != 0)
				helper.fail("made " + d.getEnergy().getEnergyStored() + " FE while overstressed");
			helper.succeed();
		});
	}

	/**
	 * On a Power Line its network shows what it makes, whatever turns it: here an Electric Motor run off a Lava Generator,
	 * none of which counts on paper (the bug this found: the network read its paper figure, 0, while it ran).
	 */
	@GameTest(template = COURSE, timeoutTicks = 260)
	public static void networkShowsWhatAMotorDrivenDynamoMakes(GameTestHelper helper) {
		BlockPos generator = new BlockPos(1, 1, 3), electric = new BlockPos(2, 1, 3), pos = new BlockPos(3, 1, 3), load = new BlockPos(6, 1, 3);
		helper.setBlock(generator, ModBlocks.LAVA_GENERATOR.get().defaultBlockState());
		helper.setBlock(electric, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		helper.setBlock(pos, ModBlocks.CLOCKWORK_DYNAMO.get().defaultBlockState().setValue(ClockworkDynamoBlock.FACING, Direction.WEST));
		// two blocks of wire on the floor from the dynamo's base (east) to a second motor's base (west)
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), pos.east(), Direction.DOWN);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), pos.east(2), Direction.DOWN);
		LineGameTests.motor(helper, load);
		helper.runAtTickTime(2, () -> {
			IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(generator), null);
			if (tank == null)
				helper.fail("no fluid handler");
			tank.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
			if (helper.getBlockEntity(electric) instanceof ElectricMotorBlockEntity m)
				m.applySettings(64, 1024, false);
			LineGameTests.relink(helper, pos.east(), pos.east(2));
		});
		helper.runAtTickTime(240, () -> {
			ClockworkDynamoBlockEntity d = dynamo(helper, pos);
			if (!d.isWorking())
				helper.fail("the dynamo isn't working");
			if (d.getFreeShare() != 0)
				helper.fail("a motor's power counted as free");
			int makes = d.networkMakes();
			if (makes < (int) d.outputPerTick())
				helper.fail("its network says it makes " + makes + " FE a tick, not its " + (int) d.outputPerTick());
			helper.succeed();
		});
	}

	/** A dynamo turned by the Creative Motor, at 16,384 SU (768 FE a tick), with a wire from its base to a motor. */
	private static void dynamoOnALine(GameTestHelper helper, BlockPos motor, BlockPos pos) {
		creativeMotorAndDynamo(helper, motor, pos);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), pos.east(), Direction.DOWN);
		LineGameTests.wire(helper, ModBlocks.COPPER_POWER_LINE.get(), pos.east(2), Direction.DOWN);
		LineGameTests.motor(helper, pos.east(3));
		helper.runAtTickTime(2, () -> {
			dynamo(helper, pos).applySettings(16384);
			LineGameTests.relink(helper, pos.east(), pos.east(2));
		});
	}

	/** Away with its free source away too (nothing else can use it), its record makes its full output (D12). */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void onPaperWithItsFreeSourceAway(GameTestHelper helper) {
		BlockPos motor = new BlockPos(1, 1, 3), pos = new BlockPos(2, 1, 3);
		dynamoOnALine(helper, motor, pos);
		helper.runAtTickTime(60, () -> {
			com.comedymonkey.createaddons.content.line.PowerGridTesting.away(helper.getLevel(), helper.absolutePos(pos), true, 100);
			com.comedymonkey.createaddons.content.line.PowerGridTesting.away(helper.getLevel(), helper.absolutePos(motor), true, 100);
		});
		helper.runAtTickTime(70, () -> {
			double away = com.comedymonkey.createaddons.content.line.PowerGridTesting.makingAway(helper.getLevel(), helper.absolutePos(pos));
			if (away < 767)
				helper.fail("on paper with its free source away it should make 768 FE a tick, not " + away);
			helper.succeed();
		});
	}

	/**
	 * Away while its free source keeps running (it could be turning something else for real): the source doesn't count,
	 * so a far dynamo can't double up on wheels that are running near the player.
	 */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void onPaperWithItsFreeSourceRunning(GameTestHelper helper) {
		BlockPos motor = new BlockPos(1, 1, 3), pos = new BlockPos(2, 1, 3);
		dynamoOnALine(helper, motor, pos);
		helper.runAtTickTime(60, () -> com.comedymonkey.createaddons.content.line.PowerGridTesting.away(helper.getLevel(), helper.absolutePos(pos), true, 100));
		helper.runAtTickTime(70, () -> {
			double away = com.comedymonkey.createaddons.content.line.PowerGridTesting.makingAway(helper.getLevel(), helper.absolutePos(pos));
			if (away < 0)
				helper.fail("no record for the dynamo");
			if (away != 0)
				helper.fail("its running free source counted on paper: " + away + " FE a tick");
			helper.succeed();
		});
	}

	/** A dynamo on a line replaced in place by a Lava Generator (a /setblock, a schematic): its record goes, nothing crashes. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void replacedByAnotherGenerator(GameTestHelper helper) {
		BlockPos motor = new BlockPos(1, 1, 3), pos = new BlockPos(2, 1, 3);
		dynamoOnALine(helper, motor, pos);
		helper.runAtTickTime(40, () -> helper.setBlock(pos, ModBlocks.LAVA_GENERATOR.get().defaultBlockState()));
		helper.runAtTickTime(80, () -> {
			if (!(helper.getBlockEntity(pos) instanceof com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity))
				helper.fail("the Lava Generator isn't there");
			helper.succeed();
		});
	}

	/** Turned by a free source (the Creative Motor is in the tag), all its power counts on paper. */
	@GameTest(template = COURSE, timeoutTicks = 80)
	public static void freeSourceCountsInFull(GameTestHelper helper) {
		BlockPos motor = new BlockPos(1, 1, 3), pos = new BlockPos(2, 1, 3);
		creativeMotorAndDynamo(helper, motor, pos);
		helper.runAtTickTime(50, () -> {
			float share = dynamo(helper, pos).getFreeShare();
			if (Math.abs(share - 1) > 1e-4)
				helper.fail("a Creative Motor's share should be 1, got " + share);
			helper.succeed();
		});
	}

	/**
	 * Turned by an Electric Motor (run off a Lava Generator), none of its power counts on paper: away, a motor draws
	 * nothing, so it would be power for free (D12).
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void motorDrivenDynamoCountsNothing(GameTestHelper helper) {
		BlockPos generator = new BlockPos(1, 1, 3), electric = new BlockPos(2, 1, 3), pos = new BlockPos(3, 1, 3);
		helper.setBlock(generator, ModBlocks.LAVA_GENERATOR.get().defaultBlockState());
		// its base (west) against the generator's east terminal, its shaft out of the east face into the dynamo's (set low, so
		// it has its starting reserve within a couple of seconds)
		helper.setBlock(electric, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
		helper.setBlock(pos, ModBlocks.CLOCKWORK_DYNAMO.get().defaultBlockState().setValue(ClockworkDynamoBlock.FACING, Direction.WEST));
		helper.runAtTickTime(2, () -> {
			IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(generator), null);
			if (tank == null)
				helper.fail("no fluid handler");
			tank.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
			if (helper.getBlockEntity(electric) instanceof ElectricMotorBlockEntity m)
				m.applySettings(64, 1024, false);
		});
		helper.runAtTickTime(150, () -> {
			if (!(helper.getBlockEntity(electric) instanceof ElectricMotorBlockEntity m) || !m.isRunning())
				helper.fail("the motor isn't running");
			ClockworkDynamoBlockEntity d = dynamo(helper, pos);
			if (!d.isWorking())
				helper.fail("the dynamo isn't working");
			if (d.getFreeShare() != 0)
				helper.fail("a motor's power counted as free: " + d.getFreeShare());
			helper.succeed();
		});
	}
}
