package com.comedymonkey.createaddons.content.crucible;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModRecipes;
import com.comedymonkey.createaddons.testutil.ModRecipeFiles;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.network.connection.ConnectionType;

/** Pressure Crucible recipes: the shipped JSON files, the codecs, and matching. */
class CrucibleRecipeTest {
	private static final String MINIMAL = """
		{"type": "comedymonkeysaddons:pressure_crucible", "ingredient": {"item": "minecraft:sand"},
		 "result": {"id": "minecraft:glass"}}""";

	private static CrucibleRecipe parse(JsonObject json) {
		return CrucibleRecipe.CODEC.codec().parse(ModRecipeFiles.jsonOps(), json).getOrThrow();
	}

	private static boolean parses(String json) {
		return CrucibleRecipe.CODEC.codec().parse(ModRecipeFiles.jsonOps(), ModRecipeFiles.json(json)).isSuccess();
	}

	private static JsonObject encode(CrucibleRecipe recipe) {
		return CrucibleRecipe.CODEC.codec().encodeStart(ModRecipeFiles.jsonOps(), recipe).getOrThrow().getAsJsonObject();
	}

	/** The recipe's heat condition by its saved name. */
	static String heat(CrucibleRecipe recipe) {
		return recipe.heat().getSerializedName();
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
	}

	private static CrucibleRecipe shipped(String file) {
		return parse(ModRecipeFiles.file("pressure_crucible", file));
	}

	private static void assertSameRecipe(CrucibleRecipe expected, CrucibleRecipe actual) {
		assertEquals(List.of(expected.ingredient().getItems()).stream().map(ItemStack::getItem).toList(),
			List.of(actual.ingredient().getItems()).stream().map(ItemStack::getItem).toList());
		assertEquals(expected.count(), actual.count());
		assertEquals(heat(expected), heat(actual));
		assertEquals(expected.processingTime(), actual.processingTime());
		assertTrue(ItemStack.matches(expected.result(), actual.result()), "result");
	}

	// ---------------------------------------------------------------- shipped recipes

	@Test
	void everyShippedRecipeLoadsThroughTheRecipeCodec() {
		Map<String, JsonObject> files = ModRecipeFiles.folder("pressure_crucible");
		assertFalse(files.isEmpty());
		for (var entry : files.entrySet()) {
			var result = Recipe.CODEC.parse(ModRecipeFiles.jsonOps(), entry.getValue());
			assertTrue(result.isSuccess(), entry.getKey() + ": " + result.error().map(Object::toString).orElse(""));
			CrucibleRecipe recipe = assertInstanceOf(CrucibleRecipe.class, result.getOrThrow(), entry.getKey());
			assertSame(ModRecipes.CRUCIBLE.get(), recipe.getType(), entry.getKey());
		}
	}

	@Test
	void shippedRecipesArePressable() {
		for (var entry : ModRecipeFiles.folder("pressure_crucible").entrySet()) {
			String name = entry.getKey();
			CrucibleRecipe recipe = parse(entry.getValue());
			ItemStack[] inputs = recipe.ingredient().getItems();
			assertTrue(inputs.length > 0, name + " has no input items");
			// The input slot holds one stack, so more than a stack could never be loaded
			for (ItemStack input : inputs)
				assertTrue(recipe.count() <= input.getMaxStackSize(), name + " needs more than a stack");
			assertFalse(recipe.result().isEmpty(), name);
			assertTrue(recipe.result().getCount() <= recipe.result().getMaxStackSize(), name);
			assertTrue(recipe.processingTime() > 0, name);
		}
	}

	@Test
	void noTwoRecipesShareAnInput() {
		// The crucible takes the first recipe whose ingredient matches, so a second one would never run
		Map<Item, String> seen = new HashMap<>();
		for (var entry : ModRecipeFiles.folder("pressure_crucible").entrySet())
			for (ItemStack input : parse(entry.getValue()).ingredient().getItems()) {
				String other = seen.put(input.getItem(), entry.getKey());
				assertTrue(other == null, entry.getKey() + " and " + other + " both take " + input.getItem());
			}
	}

	@Test
	void emeraldRecipe() {
		CrucibleRecipe recipe = shipped("emerald.json");
		assertSame(item("comedymonkeysaddons:emerald_dust"), recipe.ingredient().getItems()[0].getItem());
		assertEquals(9, recipe.count());
		assertEquals("heated", heat(recipe));
		assertEquals(200, recipe.processingTime());
		assertSame(Items.EMERALD, recipe.result().getItem());
		assertEquals(1, recipe.result().getCount());
	}

	@Test
	void amethystRecipeMakesTwo() {
		CrucibleRecipe recipe = shipped("amethyst_shard.json");
		assertEquals("superheated", heat(recipe));
		assertEquals(4, recipe.count());
		assertSame(Items.AMETHYST_SHARD, recipe.result().getItem());
		assertEquals(2, recipe.result().getCount());
	}

	@Test
	void stoneRecipeNeedsNoHeat() {
		CrucibleRecipe recipe = shipped("stone.json");
		assertEquals("none", heat(recipe));
		assertEquals(1, recipe.count());
		assertEquals(40, recipe.processingTime());
	}

	// ---------------------------------------------------------------- codec

	@Test
	void defaults() {
		CrucibleRecipe recipe = parse(ModRecipeFiles.json(MINIMAL));
		assertEquals(9, recipe.count());
		assertEquals("none", heat(recipe));
		assertEquals(200, recipe.processingTime());
		assertSame(Items.GLASS, recipe.result().getItem());
	}

	@Test
	void defaultsAreLeftOutWhenSaved() {
		JsonObject json = encode(parse(ModRecipeFiles.json(MINIMAL)));
		assertFalse(json.has("count"));
		assertFalse(json.has("heat_requirement"));
		assertFalse(json.has("processing_time"));
	}

	@Test
	void jsonRoundTrip() {
		for (JsonObject file : ModRecipeFiles.folder("pressure_crucible").values()) {
			CrucibleRecipe recipe = parse(file);
			assertSameRecipe(recipe, parse(encode(recipe)));
		}
	}

	@Test
	void invalidRecipesAreRejected() {
		assertTrue(parses(MINIMAL));
		List<String> bad = new ArrayList<>();
		bad.add("""
			{"ingredient": {"item": "minecraft:sand"}, "count": 0, "result": {"id": "minecraft:glass"}}""");
		bad.add("""
			{"ingredient": {"item": "minecraft:sand"}, "count": 65, "result": {"id": "minecraft:glass"}}""");
		bad.add("""
			{"ingredient": {"item": "minecraft:sand"}, "processing_time": 0, "result": {"id": "minecraft:glass"}}""");
		bad.add("""
			{"ingredient": {"item": "minecraft:sand"}, "heat_requirement": "lukewarm", "result": {"id": "minecraft:glass"}}""");
		bad.add("""
			{"ingredient": {"item": "minecraft:sand"}, "result": {"id": "minecraft:air"}}""");
		bad.add("""
			{"ingredient": {"item": "minecraft:sand"}}""");
		bad.add("""
			{"result": {"id": "minecraft:glass"}}""");
		bad.add("""
			{"ingredient": [], "result": {"id": "minecraft:glass"}}""");
		bad.add("""
			{"ingredient": {"item": "minecraft:no_such_item"}, "result": {"id": "minecraft:glass"}}""");
		for (String json : bad)
			assertFalse(parses(json), json);
	}

	@Test
	void streamCodecRoundTrip() {
		for (var entry : ModRecipeFiles.folder("pressure_crucible").entrySet()) {
			CrucibleRecipe recipe = parse(entry.getValue());
			RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), ConnectionType.NEOFORGE);
			CrucibleRecipe.STREAM_CODEC.encode(buf, recipe);
			CrucibleRecipe decoded = CrucibleRecipe.STREAM_CODEC.decode(buf);
			assertEquals(0, buf.readableBytes(), entry.getKey() + ": bytes left over");
			assertSameRecipe(recipe, decoded);
		}
	}

	@Test
	void syncsThroughTheRecipeStreamCodec() {
		CrucibleRecipe recipe = shipped("netherite_scrap.json");
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), ConnectionType.NEOFORGE);
		Recipe.STREAM_CODEC.encode(buf, recipe);
		assertSameRecipe(recipe, assertInstanceOf(CrucibleRecipe.class, Recipe.STREAM_CODEC.decode(buf)));
	}

	// ---------------------------------------------------------------- matching

	@Test
	void matchesNeedsTheWholeCount() {
		CrucibleRecipe recipe = shipped("emerald.json");
		Item dust = item("comedymonkeysaddons:emerald_dust");
		assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(dust, 8)), null));
		assertTrue(recipe.matches(new SingleRecipeInput(new ItemStack(dust, 9)), null));
		assertTrue(recipe.matches(new SingleRecipeInput(new ItemStack(dust, 64)), null));
	}

	@Test
	void matchesOnlyItsIngredient() {
		CrucibleRecipe recipe = shipped("emerald.json");
		assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(Items.EMERALD, 64)), null));
		assertFalse(recipe.matches(new SingleRecipeInput(ItemStack.EMPTY), null));
	}

	@Test
	void assembleHandsOutACopy() {
		CrucibleRecipe recipe = shipped("amethyst_shard.json");
		ItemStack made = recipe.assemble(new SingleRecipeInput(new ItemStack(Items.QUARTZ, 4)), ModRecipeFiles.REGISTRIES);
		assertNotSame(recipe.result(), made);
		made.shrink(2);
		assertEquals(2, recipe.result().getCount(), "the recipe's own result must not change");
		assertEquals(2, recipe.getResultItem(ModRecipeFiles.REGISTRIES).getCount());
	}

	@Test
	void recipeProperties() {
		CrucibleRecipe recipe = shipped("glass.json");
		assertTrue(recipe.isSpecial(), "kept out of the recipe book");
		assertTrue(recipe.canCraftInDimensions(1, 1));
		assertEquals(1, recipe.getIngredients().size());
		assertSame(ModRecipes.CRUCIBLE_SERIALIZER.get(), recipe.getSerializer());
		assertSame(ModRecipes.CRUCIBLE.get(), recipe.getType());
	}

	@Test
	void heatNamesAreTranslated() {
		Path lang = ModResourceFiles.find("assets", "comedymonkeysaddons", "lang", "en_us.json");
		assumeTrue(lang != null, "en_us.json not found");
		JsonElement json = ModResourceFiles.readJson(lang);
		for (JsonObject file : ModRecipeFiles.folder("pressure_crucible").values()) {
			String key = "comedymonkeysaddons.gui.heat." + heat(parse(file));
			assertTrue(json.getAsJsonObject().has(key), key);
		}
	}
}
