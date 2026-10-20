package com.comedymonkey.createaddons.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Bountiful and Fruitful: finding their level on an item, the speed-up they give, and their data files. */
class MachineEnchantmentsTest {
	private static final HolderOwner<Enchantment> OWNER = new HolderOwner<>() {};

	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static Holder<Enchantment> holder(ResourceKey<Enchantment> key) {
		return Holder.Reference.createStandAlone(OWNER, key);
	}

	private static ItemEnchantments with(Object... holdersAndLevels) {
		ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		for (int i = 0; i < holdersAndLevels.length; i += 2) {
			@SuppressWarnings("unchecked")
			Holder<Enchantment> holder = (Holder<Enchantment>) holdersAndLevels[i];
			mutable.set(holder, (Integer) holdersAndLevels[i + 1]);
		}
		return mutable.toImmutable();
	}

	// ---------------------------------------------------------------- keys

	@Test
	void keysAreInTheModsNamespace() {
		assertEquals(ComedyMonkeysCreateAddons.asResource("bountiful"), MachineEnchantments.BOUNTIFUL.location());
		assertEquals(ComedyMonkeysCreateAddons.asResource("fruitful"), MachineEnchantments.FRUITFUL.location());
		assertTrue(MachineEnchantments.BOUNTIFUL.isFor(Registries.ENCHANTMENT));
	}

	// ---------------------------------------------------------------- levelIn

	@Test
	void noEnchantmentsIsLevelZero() {
		assertEquals(0, MachineEnchantments.levelIn(ItemEnchantments.EMPTY, MachineEnchantments.BOUNTIFUL));
	}

	@Test
	void findsTheLevelOfTheRightEnchantment() {
		ItemEnchantments enchantments = with(holder(MachineEnchantments.BOUNTIFUL), 2);
		assertEquals(2, MachineEnchantments.levelIn(enchantments, MachineEnchantments.BOUNTIFUL));
		assertEquals(0, MachineEnchantments.levelIn(enchantments, MachineEnchantments.FRUITFUL));
	}

	@Test
	void picksItOutAmongOthers() {
		var vanilla = VanillaRegistries.createLookup().lookupOrThrow(Registries.ENCHANTMENT);
		ItemEnchantments enchantments = with(
			vanilla.getOrThrow(Enchantments.UNBREAKING), 3,
			holder(MachineEnchantments.FRUITFUL), 1,
			vanilla.getOrThrow(Enchantments.MENDING), 1,
			holder(MachineEnchantments.BOUNTIFUL), 3);
		assertEquals(3, MachineEnchantments.levelIn(enchantments, MachineEnchantments.BOUNTIFUL));
		assertEquals(1, MachineEnchantments.levelIn(enchantments, MachineEnchantments.FRUITFUL));
	}

	@Test
	void anEnchantmentWithTheSamePathElsewhereDoesNotCount() {
		ResourceKey<Enchantment> impostor = ResourceKey.create(Registries.ENCHANTMENT,
			net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("othermod", "bountiful"));
		ItemEnchantments enchantments = with(holder(impostor), 3);
		assertEquals(0, MachineEnchantments.levelIn(enchantments, MachineEnchantments.BOUNTIFUL));
	}

	// ---------------------------------------------------------------- speedMultiplier

	@Test
	void defaultBonusIsHalfPerLevel() {
		assertEquals(1f, MachineEnchantments.speedMultiplier(0, CMConfig.SERVER.harvesterBountifulBonus), 1e-6f);
		assertEquals(1.5f, MachineEnchantments.speedMultiplier(1, CMConfig.SERVER.harvesterBountifulBonus), 1e-6f);
		assertEquals(2f, MachineEnchantments.speedMultiplier(2, CMConfig.SERVER.harvesterBountifulBonus), 1e-6f);
		assertEquals(2.5f, MachineEnchantments.speedMultiplier(3, CMConfig.SERVER.harvesterBountifulBonus), 1e-6f);
	}

	@Test
	void negativeLevelsNeverSlowItDown() {
		assertEquals(1f, MachineEnchantments.speedMultiplier(-3, CMConfig.SERVER.harvesterBountifulBonus), 1e-6f);
	}

	@Test
	void bonusFollowsTheConfig() {
		TestConfig.set(CMConfig.SERVER.harvesterBountifulBonus, 1.0);
		assertEquals(3f, MachineEnchantments.speedMultiplier(2, CMConfig.SERVER.harvesterBountifulBonus), 1e-6f);
		TestConfig.set(CMConfig.SERVER.harvesterBountifulBonus, 0.0);
		assertEquals(1f, MachineEnchantments.speedMultiplier(3, CMConfig.SERVER.harvesterBountifulBonus), 1e-6f);
	}

	// ---------------------------------------------------------------- data files agree with the code

	private static JsonObject json(String... path) {
		Path file = ModResourceFiles.find(path);
		assumeTrue(file != null, "resource not reachable: " + String.join("/", path));
		JsonElement element = ModResourceFiles.readJson(file);
		return element.getAsJsonObject();
	}

	@Test
	void enchantmentsGoUpToTheMaxLevel() {
		for (String name : new String[] {"bountiful", "fruitful"}) {
			JsonObject enchantment = json("data", ComedyMonkeysCreateAddons.MOD_ID, "enchantment", name + ".json");
			assertEquals(MachineEnchantments.MAX_LEVEL, enchantment.get("max_level").getAsInt(), name);
			assertEquals("#" + ComedyMonkeysCreateAddons.MOD_ID + ":enchantable/" + name,
				enchantment.get("supported_items").getAsString(), name);
		}
	}

	@Test
	void bountifulGoesOnTheAutoHarvester() {
		JsonObject tag = json("data", ComedyMonkeysCreateAddons.MOD_ID, "tags", "item", "enchantable", "bountiful.json");
		boolean found = false;
		for (JsonElement value : tag.getAsJsonArray("values"))
			found |= value.getAsString().equals(ComedyMonkeysCreateAddons.MOD_ID + ":auto_harvester");
		assertTrue(found);
	}

	@Test
	void harvesterKeepsItsEnchantmentsWhenBroken() {
		// The block entity hands them back through collectImplicitComponents; the loot table has to copy them
		JsonObject table = json("data", ComedyMonkeysCreateAddons.MOD_ID, "loot_table", "blocks", "auto_harvester.json");
		String text = table.toString();
		assertTrue(text.contains("minecraft:copy_components"), text);
		assertTrue(text.contains("minecraft:enchantments"), text);
	}

	@Test
	void booksAreTradeable() {
		JsonObject tag = json("data", "minecraft", "tags", "enchantment", "tradeable.json");
		String values = tag.getAsJsonArray("values").toString();
		assertNotNull(values);
		assertTrue(values.contains(MachineEnchantments.BOUNTIFUL.location().toString()), values);
		assertTrue(values.contains(MachineEnchantments.FRUITFUL.location().toString()), values);
	}
}
