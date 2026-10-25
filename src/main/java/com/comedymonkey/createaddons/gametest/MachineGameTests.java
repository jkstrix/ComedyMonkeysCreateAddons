package com.comedymonkey.createaddons.gametest;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.incinerator.IncineratorBlock;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

	private static void feed(GameTestHelper helper, BlockPos pos, int count) {
		IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
		if (items == null)
			helper.fail("no item handler");
		items.insertItem(0, new ItemStack(Items.COBBLESTONE, count), false);
	}
}
