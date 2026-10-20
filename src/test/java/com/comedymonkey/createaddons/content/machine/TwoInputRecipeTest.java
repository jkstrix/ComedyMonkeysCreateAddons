package com.comedymonkey.createaddons.content.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModRecipes;
import com.comedymonkey.createaddons.testutil.ModRecipeFiles;
import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.network.connection.ConnectionType;

/** String Tangler and Stone Infuser recipes: the shipped files, both serializers' codecs, and matching. */
class TwoInputRecipeTest {
	private static final String MINIMAL = """
		{"ingredient": {"item": "minecraft:string"}, "extra": {"item": "minecraft:slime_ball"},
		 "result": {"id": "minecraft:cobweb"}}""";

	private static TwoInputRecipe.Serializer serializer(MachineKind kind) {
		return kind == MachineKind.TANGLING ? ModRecipes.TANGLING_SERIALIZER.get() : ModRecipes.INFUSING_SERIALIZER.get();
	}

	private static MapCodec<TwoInputRecipe> codec(MachineKind kind) {
		return serializer(kind).codec();
	}

	private static TwoInputRecipe parse(MachineKind kind, JsonObject json) {
		return codec(kind).codec().parse(ModRecipeFiles.jsonOps(), json).getOrThrow();
	}

	private static boolean parses(String json) {
		return codec(MachineKind.TANGLING).codec().parse(ModRecipeFiles.jsonOps(), ModRecipeFiles.json(json)).isSuccess();
	}

	private static JsonObject encode(TwoInputRecipe recipe) {
		return codec(recipe.kind()).codec().encodeStart(ModRecipeFiles.jsonOps(), recipe).getOrThrow().getAsJsonObject();
	}

	private static RecipeInput input(ItemStack... stacks) {
		return new RecipeInput() {
			@Override
			public ItemStack getItem(int index) {
				return index < stacks.length ? stacks[index] : ItemStack.EMPTY;
			}

			@Override
			public int size() {
				return stacks.length;
			}
		};
	}

	/** Cobweb from 4 string and 2 slime balls, with plain item ingredients (tags aren't bound in unit tests). */
	private static TwoInputRecipe cobweb() {
		return new TwoInputRecipe(MachineKind.TANGLING, Ingredient.of(Items.STRING), 4, Ingredient.of(Items.SLIME_BALL), 2,
			160, new ItemStack(Items.COBWEB));
	}

	private static List<?> items(Ingredient ingredient) {
		return List.of(ingredient.getItems()).stream().map(ItemStack::getItem).toList();
	}

	private static void assertSameRecipe(TwoInputRecipe expected, TwoInputRecipe actual) {
		assertEquals(expected.kind(), actual.kind());
		assertEquals(items(expected.ingredient()), items(actual.ingredient()));
		assertEquals(expected.count(), actual.count());
		assertEquals(items(expected.extra()), items(actual.extra()));
		assertEquals(expected.extraCount(), actual.extraCount());
		assertEquals(expected.processingTime(), actual.processingTime());
		assertTrue(ItemStack.matches(expected.result(), actual.result()), "result");
	}

	// ---------------------------------------------------------------- shipped recipes

	@Test
	void everyShippedRecipeLoadsAsItsMachinesKind() {
		for (MachineKind kind : MachineKind.values()) {
			Map<String, JsonObject> files = ModRecipeFiles.folder(kind.id());
			assertFalse(files.isEmpty(), kind.id());
			for (var entry : files.entrySet()) {
				String name = kind.id() + "/" + entry.getKey();
				var result = Recipe.CODEC.parse(ModRecipeFiles.jsonOps(), entry.getValue());
				assertTrue(result.isSuccess(), name + ": " + result.error().map(Object::toString).orElse(""));
				TwoInputRecipe recipe = assertInstanceOf(TwoInputRecipe.class, result.getOrThrow(), name);
				assertEquals(kind, recipe.kind(), name + " is in the wrong folder for its type");
				assertSame(kind.recipeType(), recipe.getType(), name);
				assertSame(serializer(kind), recipe.getSerializer(), name);
			}
		}
	}

	@Test
	void shippedRecipesFitTheSlots() {
		for (MachineKind kind : MachineKind.values())
			for (var entry : ModRecipeFiles.folder(kind.id()).entrySet()) {
				String name = kind.id() + "/" + entry.getKey();
				TwoInputRecipe recipe = parse(kind, entry.getValue());
				assertTrue(recipe.count() >= 1 && recipe.count() <= 64, name);
				assertTrue(recipe.extraCount() >= 1 && recipe.extraCount() <= 64, name);
				assertTrue(recipe.processingTime() > 0, name);
				assertFalse(recipe.result().isEmpty(), name);
				assertTrue(recipe.result().getCount() <= recipe.result().getMaxStackSize(), name);
			}
	}

	@Test
	void noIngredientIsBothMainAndExtra() {
		// The machine sorts an item into the main slot if any recipe uses it as the main ingredient, so an ingredient
		// that is also some recipe's extra could never be loaded as the extra
		for (MachineKind kind : MachineKind.values()) {
			Set<String> mains = new HashSet<>(), extras = new HashSet<>();
			for (JsonObject json : ModRecipeFiles.folder(kind.id()).values()) {
				mains.add(json.get("ingredient").toString());
				extras.add(json.get("extra").toString());
			}
			mains.retainAll(extras);
			assertTrue(mains.isEmpty(), kind.id() + ": " + mains);
		}
	}

	@Test
	void cobwebRecipe() {
		TwoInputRecipe recipe = parse(MachineKind.TANGLING, ModRecipeFiles.file("tangling", "cobweb.json"));
		assertEquals(List.of(Items.STRING), items(recipe.ingredient()));
		assertEquals(4, recipe.count());
		assertEquals("c:slime_balls", encode(recipe).getAsJsonObject("extra").get("tag").getAsString());
		assertEquals(1, recipe.extraCount());
		assertEquals(160, recipe.processingTime());
		assertSame(Items.COBWEB, recipe.result().getItem());
	}

	@Test
	void infusingRecipesMakeTheStoneTheyAreNamedFor() {
		for (var entry : ModRecipeFiles.folder("infusing").entrySet()) {
			TwoInputRecipe recipe = parse(MachineKind.INFUSING, entry.getValue());
			String stone = entry.getKey().replace(".json", "");
			assertEquals("create:" + stone, BuiltInRegistries.ITEM.getKey(recipe.result().getItem()).toString());
			assertEquals(List.of(Items.COBBLESTONE), items(recipe.ingredient()), stone);
			assertEquals(1, recipe.count(), stone);
			assertEquals(9, recipe.extraCount(), "a crushed-ore's worth of nuggets for " + stone);
		}
	}

	// ---------------------------------------------------------------- codec

	@Test
	void defaults() {
		TwoInputRecipe recipe = parse(MachineKind.TANGLING, ModRecipeFiles.json(MINIMAL));
		assertEquals(1, recipe.count());
		assertEquals(1, recipe.extraCount());
		assertEquals(200, recipe.processingTime());
		assertEquals(MachineKind.TANGLING, recipe.kind());
	}

	@Test
	void eachSerializerStampsItsOwnKind() {
		assertEquals(MachineKind.INFUSING, parse(MachineKind.INFUSING, ModRecipeFiles.json(MINIMAL)).kind());
		assertEquals(MachineKind.TANGLING, parse(MachineKind.TANGLING, ModRecipeFiles.json(MINIMAL)).kind());
	}

	@Test
	void jsonRoundTrip() {
		for (MachineKind kind : MachineKind.values())
			for (JsonObject file : ModRecipeFiles.folder(kind.id()).values()) {
				JsonObject once = encode(parse(kind, file));
				assertEquals(once, encode(parse(kind, once)));
			}
		assertSameRecipe(cobweb(), parse(MachineKind.TANGLING, encode(cobweb())));
	}

	@Test
	void invalidRecipesAreRejected() {
		assertTrue(parses(MINIMAL));
		String[] bad = {
			"""
			{"ingredient": {"item": "minecraft:string"}, "extra": {"item": "minecraft:slime_ball"}, "count": 0,
			 "result": {"id": "minecraft:cobweb"}}""",
			"""
			{"ingredient": {"item": "minecraft:string"}, "extra": {"item": "minecraft:slime_ball"}, "extra_count": 65,
			 "result": {"id": "minecraft:cobweb"}}""",
			"""
			{"ingredient": {"item": "minecraft:string"}, "extra": {"item": "minecraft:slime_ball"}, "processing_time": -5,
			 "result": {"id": "minecraft:cobweb"}}""",
			"""
			{"ingredient": {"item": "minecraft:string"}, "result": {"id": "minecraft:cobweb"}}""",
			"""
			{"ingredient": {"item": "minecraft:string"}, "extra": [], "result": {"id": "minecraft:cobweb"}}""",
			"""
			{"ingredient": {"item": "minecraft:string"}, "extra": {"item": "minecraft:slime_ball"},
			 "result": {"id": "minecraft:air"}}""",
		};
		for (String json : bad)
			assertFalse(parses(json), json);
	}

	@Test
	void streamCodecRoundTrip() {
		for (MachineKind kind : MachineKind.values()) {
			TwoInputRecipe recipe = new TwoInputRecipe(kind, Ingredient.of(Items.COBBLESTONE, Items.STONE), 3,
				Ingredient.of(Items.IRON_NUGGET), 9, 77, new ItemStack(Items.DIAMOND, 2));
			RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), ConnectionType.NEOFORGE);
			serializer(kind).streamCodec().encode(buf, recipe);
			TwoInputRecipe decoded = serializer(kind).streamCodec().decode(buf);
			assertEquals(0, buf.readableBytes(), "bytes left over");
			assertSameRecipe(recipe, decoded);
		}
	}

	@Test
	void syncsThroughTheRecipeStreamCodecKeepingItsKind() {
		for (MachineKind kind : MachineKind.values()) {
			TwoInputRecipe recipe = new TwoInputRecipe(kind, Ingredient.of(Items.STRING), 4, Ingredient.of(Items.SLIME_BALL),
				1, 160, new ItemStack(Items.COBWEB));
			RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), ConnectionType.NEOFORGE);
			Recipe.STREAM_CODEC.encode(buf, recipe);
			TwoInputRecipe decoded = assertInstanceOf(TwoInputRecipe.class, Recipe.STREAM_CODEC.decode(buf));
			assertSameRecipe(recipe, decoded);
		}
	}

	// ---------------------------------------------------------------- matching

	@Test
	void matchesWithEnoughOfBoth() {
		TwoInputRecipe recipe = cobweb();
		assertTrue(recipe.matches(input(new ItemStack(Items.STRING, 4), new ItemStack(Items.SLIME_BALL, 2)), null));
		assertTrue(recipe.matches(input(new ItemStack(Items.STRING, 64), new ItemStack(Items.SLIME_BALL, 64)), null));
	}

	@Test
	void doesNotMatchWithTooFew() {
		TwoInputRecipe recipe = cobweb();
		assertFalse(recipe.matches(input(new ItemStack(Items.STRING, 3), new ItemStack(Items.SLIME_BALL, 2)), null));
		assertFalse(recipe.matches(input(new ItemStack(Items.STRING, 4), new ItemStack(Items.SLIME_BALL, 1)), null));
	}

	@Test
	void slotsAreNotInterchangeable() {
		TwoInputRecipe recipe = cobweb();
		assertFalse(recipe.matches(input(new ItemStack(Items.SLIME_BALL, 64), new ItemStack(Items.STRING, 64)), null));
		assertFalse(recipe.matches(input(new ItemStack(Items.STRING, 64), ItemStack.EMPTY), null));
		assertFalse(recipe.matches(input(new ItemStack(Items.STRING, 64)), null), "needs two slots");
		assertFalse(recipe.matches(input(), null));
	}

	@Test
	void assembleHandsOutACopy() {
		TwoInputRecipe recipe = cobweb();
		ItemStack made = recipe.assemble(input(new ItemStack(Items.STRING, 4), new ItemStack(Items.SLIME_BALL, 2)),
			ModRecipeFiles.REGISTRIES);
		assertNotSame(recipe.result(), made);
		made.grow(10);
		assertEquals(1, recipe.result().getCount());
		assertEquals(1, recipe.getResultItem(ModRecipeFiles.REGISTRIES).getCount());
	}

	@Test
	void recipeProperties() {
		TwoInputRecipe recipe = cobweb();
		assertTrue(recipe.isSpecial(), "kept out of the recipe book");
		assertTrue(recipe.canCraftInDimensions(1, 1));
		assertEquals(2, recipe.getIngredients().size());
		assertSame(recipe.ingredient(), recipe.getIngredients().get(0), "main ingredient first");
		assertSame(recipe.extra(), recipe.getIngredients().get(1));
	}
}
