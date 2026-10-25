package com.comedymonkey.createaddons.gametest;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.content.incinerator.IncineratorBlock;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** The machines' own rules, on the Trade Bot's empty course (run with ./gradlew runGameTestServer). */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineGameTests {
	private static final String COURSE = "trade_bot_course";

	private MachineGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(MachineGameTests.class);
	}

	/**
	 * The fire ramps up and down: nine things leave it cold, the tenth lights it, a thing a second keeps it lit, and once
	 * the feed stops it stays lit while it cools (no flicker) and is cold about ten seconds later. A bucket of water heats
	 * it right up.
	 */
	@GameTest(template = COURSE, timeoutTicks = 420)
	public static void incineratorRampsUpAndDown(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ModBlocks.INCINERATOR.get().defaultBlockState());
		helper.runAtTickTime(5, () -> feed(helper, pos, 9));
		helper.runAtTickTime(10, () -> helper.assertBlockProperty(pos, IncineratorBlock.LIT, false));
		helper.runAtTickTime(12, () -> feed(helper, pos, 1));
		helper.runAtTickTime(15, () -> helper.assertBlockProperty(pos, IncineratorBlock.LIT, true));
		for (int t = 30; t <= 130; t += 20) {
			int at = t;
			helper.runAtTickTime(at, () -> feed(helper, pos, 1));
		}
		// the feed stopped at 130: still lit while it cools, cold ten seconds or so later
		for (int t = 140; t <= 300; t += 5)
			helper.runAtTickTime(t, () -> helper.assertBlockProperty(pos, IncineratorBlock.LIT, true));
		helper.runAtTickTime(350, () -> {
			helper.assertBlockProperty(pos, IncineratorBlock.LIT, false);
			IFluidHandler fluids = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
			if (fluids == null)
				helper.fail("no fluid handler");
			fluids.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		});
		helper.runAtTickTime(355, () -> {
			helper.assertBlockProperty(pos, IncineratorBlock.LIT, true);
			helper.succeed();
		});
	}

	/**
	 * A Supercharged Magma Engine (20 mB/s) fed a millibucket every other tick, as one Lava Upgrade's Wellspring would:
	 * it runs in spells of a second or more, not on and off every tick or two (each time across its kinetic network).
	 */
	@GameTest(template = COURSE, timeoutTicks = 500)
	public static void starvedMagmaEngineRunsInSpells(GameTestHelper helper) {
		BlockPos pos = new BlockPos(3, 1, 3);
		helper.setBlock(pos, ModBlocks.SUPERCHARGED_MAGMA_ENGINE.get().defaultBlockState());
		int[] flips = {0};
		boolean[] lit = {false};
		for (int t = 1; t <= 400; t++) {
			int at = t;
			helper.runAtTickTime(at, () -> {
				if (at % 2 == 0) {
					IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), null);
					if (tank == null)
						helper.fail("no fluid handler");
					tank.fill(new FluidStack(Fluids.LAVA, 1), IFluidHandler.FluidAction.EXECUTE);
				}
				boolean now = helper.getBlockState(pos).getValue(MagmaEngineBlock.LIT);
				if (now != lit[0])
					flips[0]++;
				lit[0] = now;
			});
		}
		helper.runAtTickTime(401, () -> {
			if (flips[0] == 0)
				helper.fail("it never ran");
			if (flips[0] > 20)
				helper.fail("lit changed " + flips[0] + " times in 20 seconds");
			helper.succeed();
		});
	}

	/**
	 * The Lava Generator keeps what's on its terminals and only looks again when that changes: a motor put there
	 * later, taken away and put back, gets power each time.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void lavaGeneratorFindsANewNeighbour(GameTestHelper helper) {
		BlockPos generator = new BlockPos(3, 1, 3);
		BlockPos motor = generator.east();
		helper.setBlock(generator, ModBlocks.LAVA_GENERATOR.get().defaultBlockState());
		helper.runAtTickTime(2, () -> {
			IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(generator), null);
			if (tank == null)
				helper.fail("no fluid handler");
			tank.fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.EXECUTE);
		});
		// the generator has looked at its empty terminal by now
		helper.runAtTickTime(10, () -> placeMotor(helper, motor));
		helper.runAtTickTime(30, () -> assertPowered(helper, motor));
		helper.runAtTickTime(40, () -> helper.setBlock(motor, Blocks.AIR));
		helper.runAtTickTime(45, () -> placeMotor(helper, motor));
		helper.runAtTickTime(65, () -> {
			assertPowered(helper, motor);
			helper.succeed();
		});
	}

	private static void placeMotor(GameTestHelper helper, BlockPos pos) {
		// its base, where power goes in, against the generator's east terminal
		helper.setBlock(pos, ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, Direction.EAST));
	}

	private static void assertPowered(GameTestHelper helper, BlockPos pos) {
		if (!(helper.getBlockEntity(pos) instanceof ElectricMotorBlockEntity motor)) {
			helper.fail("no motor");
			return;
		}
		if (motor.getEnergy().getEnergyStored() <= 0)
			helper.fail("the motor got no power");
	}

	private static void feed(GameTestHelper helper, BlockPos pos, int count) {
		IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
		if (items == null)
			helper.fail("no item handler");
		items.insertItem(0, new ItemStack(Items.COBBLESTONE, count), false);
	}
}
