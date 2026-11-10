package com.comedymonkey.createaddons.gametest;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Bulk washing ages copper a stage a pass, to oxidized and no further; wax keeps it. */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CopperWashingGameTests {
	private static final String COURSE = "trade_bot_course";

	private CopperWashingGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(CopperWashingGameTests.class);
	}

	private static Item item(String id) {
		Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
		if (item == Items.AIR)
			throw new IllegalStateException("no item " + id);
		return item;
	}

	/** What one pass of a washing fan makes of a stack, or null if it passes untouched. */
	private static ItemStack washOnce(GameTestHelper helper, ItemStack stack) {
		if (!AllFanProcessingTypes.SPLASHING.canProcess(stack, helper.getLevel()))
			return null;
		List<ItemStack> out = AllFanProcessingTypes.SPLASHING.process(stack, helper.getLevel());
		if (out == null || out.size() != 1)
			helper.fail("washing " + stack + " made " + out);
		return out.get(0);
	}

	/** Each stage of each kind, vanilla's and Create's, goes one stage on, and the last stays. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void washingAgesCopperAStageAPass(GameTestHelper helper) {
		String[][] chains = {
			{"minecraft:copper_block", "minecraft:exposed_copper", "minecraft:weathered_copper", "minecraft:oxidized_copper"},
			{"minecraft:cut_copper", "minecraft:exposed_cut_copper", "minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper"},
			{"minecraft:cut_copper_stairs", "minecraft:exposed_cut_copper_stairs", "minecraft:weathered_cut_copper_stairs", "minecraft:oxidized_cut_copper_stairs"},
			{"minecraft:cut_copper_slab", "minecraft:exposed_cut_copper_slab", "minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab"},
			{"minecraft:copper_door", "minecraft:exposed_copper_door", "minecraft:weathered_copper_door", "minecraft:oxidized_copper_door"},
			{"minecraft:copper_trapdoor", "minecraft:exposed_copper_trapdoor", "minecraft:weathered_copper_trapdoor", "minecraft:oxidized_copper_trapdoor"},
			{"minecraft:copper_bulb", "minecraft:exposed_copper_bulb", "minecraft:weathered_copper_bulb", "minecraft:oxidized_copper_bulb"},
			{"minecraft:copper_grate", "minecraft:exposed_copper_grate", "minecraft:weathered_copper_grate", "minecraft:oxidized_copper_grate"},
			{"minecraft:chiseled_copper", "minecraft:exposed_chiseled_copper", "minecraft:weathered_chiseled_copper", "minecraft:oxidized_chiseled_copper"},
			{"create:copper_shingles", "create:exposed_copper_shingles", "create:weathered_copper_shingles", "create:oxidized_copper_shingles"},
			{"create:copper_shingle_stairs", "create:exposed_copper_shingle_stairs", "create:weathered_copper_shingle_stairs", "create:oxidized_copper_shingle_stairs"},
			{"create:copper_shingle_slab", "create:exposed_copper_shingle_slab", "create:weathered_copper_shingle_slab", "create:oxidized_copper_shingle_slab"},
			{"create:copper_tiles", "create:exposed_copper_tiles", "create:weathered_copper_tiles", "create:oxidized_copper_tiles"},
			{"create:copper_tile_stairs", "create:exposed_copper_tile_stairs", "create:weathered_copper_tile_stairs", "create:oxidized_copper_tile_stairs"},
			{"create:copper_tile_slab", "create:exposed_copper_tile_slab", "create:weathered_copper_tile_slab", "create:oxidized_copper_tile_slab"},
		};
		for (String[] chain : chains) {
			for (int i = 0; i + 1 < chain.length; i++) {
				ItemStack out = washOnce(helper, new ItemStack(item(chain[i]), 64));
				if (out == null || !out.is(item(chain[i + 1])) || out.getCount() != 64)
					helper.fail("washing 64 " + chain[i] + " made " + out + ", not 64 " + chain[i + 1]);
			}
			if (washOnce(helper, new ItemStack(item(chain[chain.length - 1]))) != null)
				helper.fail("washing " + chain[chain.length - 1] + " changed it: it's as oxidized as it goes");
		}
		helper.succeed();
	}

	/** The stages recipe viewers show (on Create's Bulk Washing page): vanilla's and Create's, and nothing waxed or fully oxidized. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void recipeViewersShowEveryStage(GameTestHelper helper) {
		var stages = com.comedymonkey.createaddons.content.CopperWashing.stages();
		String[][] expected = {{"minecraft:copper_block", "minecraft:exposed_copper"}, {"minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab"},
			{"create:copper_shingles", "create:exposed_copper_shingles"}, {"create:exposed_copper_tile_stairs", "create:weathered_copper_tile_stairs"}};
		for (String[] pair : expected)
			if (stages.stream().noneMatch(s -> s.getKey() == item(pair[0]) && s.getValue() == item(pair[1])))
				helper.fail("recipe viewers don't show washing " + pair[0] + " into " + pair[1]);
		for (var s : stages) {
			String id = BuiltInRegistries.ITEM.getKey(s.getKey()).getPath();
			if (id.contains("waxed") || id.startsWith("oxidized"))
				helper.fail("recipe viewers show washing " + id + ", which washing leaves as it is");
		}
		helper.succeed();
	}

	/** Waxed copper never weathers: the wax keeps it as it is. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void waxKeepsCopperFromWashing(GameTestHelper helper) {
		for (String id : new String[] {"minecraft:waxed_copper_block", "minecraft:waxed_exposed_copper", "minecraft:waxed_cut_copper_stairs",
			"minecraft:waxed_copper_door", "create:waxed_copper_shingles", "create:waxed_exposed_copper_tiles"})
			if (washOnce(helper, new ItemStack(item(id))) != null)
				helper.fail("washing " + id + " changed it, through the wax");
		helper.succeed();
	}

	/** Washing recipes still work: a red Fluid Line still washes plain. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void washingRecipesStillWork(GameTestHelper helper) {
		var dye = com.comedymonkey.createaddons.registry.ModDataComponents.LINE_DYE.get();
		ItemStack red = new ItemStack(com.comedymonkey.createaddons.registry.ModBlocks.COPPER_FLUID_LINE.get(), 8);
		red.set(dye, net.minecraft.world.item.DyeColor.RED);
		ItemStack out = washOnce(helper, red);
		if (out == null || !out.is(red.getItem()) || out.get(dye) != null)
			helper.fail("a red Fluid Line no longer washes plain: " + out);
		helper.succeed();
	}

	/** A dropped stack held in a washing fan's stream as long as it likes (Create's own processing): what it ends as. */
	private static ItemStack throughAFan(GameTestHelper helper, ItemStack stack) {
		ItemEntity entity = new ItemEntity(helper.getLevel(), 0, 0, 0, stack);
		for (int tick = 0; tick < 20000; tick++)
			if (FanProcessing.canProcess(entity, AllFanProcessingTypes.SPLASHING))
				FanProcessing.applyProcessing(entity, AllFanProcessingTypes.SPLASHING);
		return entity.getItem();
	}

	/**
	 * One pass through a washing fan's stream, however long it stays there, ages copper one stage, as with any washing;
	 * picked up and dropped in again it goes on a stage, to oxidized and no further. It keeps its count and name.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void aFanPassAgesCopperOneStage(GameTestHelper helper) {
		ItemStack stack = new ItemStack(Items.COPPER_BLOCK, 5);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Roof"));
		Item[] stages = {Items.EXPOSED_COPPER, Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER, Items.OXIDIZED_COPPER};
		for (Item expected : stages) {
			ItemStack before = stack;
			stack = throughAFan(helper, stack.copy());
			if (!stack.is(expected) || stack.getCount() != 5)
				helper.fail("a pass through a fan made " + before + " into " + stack + ", not 5 " + expected);
			if (!Component.literal("Roof").equals(stack.get(DataComponents.CUSTOM_NAME)))
				helper.fail("washing lost the copper's name: " + stack.get(DataComponents.CUSTOM_NAME));
		}
		helper.succeed();
	}
}
