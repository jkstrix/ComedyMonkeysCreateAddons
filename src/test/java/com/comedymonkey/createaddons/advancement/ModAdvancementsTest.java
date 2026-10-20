package com.comedymonkey.createaddons.advancement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

class ModAdvancementsTest {
	private static final String TRIGGER = ComedyMonkeysCreateAddons.MOD_ID + ":action";

	@Test
	void triggerIsRegistered() {
		assertEquals(ComedyMonkeysCreateAddons.asResource("action"),
			BuiltInRegistries.TRIGGER_TYPES.getKey(ModAdvancements.ACTION.get()));
		assertEquals(TRIGGER, ModAdvancements.ACTION.getId().toString());
	}

	@Test
	void awardingNobodyDoesNothing() {
		assertDoesNotThrow(() -> ModAdvancements.award(null, ModAdvancements.PEN_CULL));
		assertDoesNotThrow(() -> ModAdvancements.awardNearby(null, null, ModAdvancements.PEN_FULL, ItemStack.EMPTY, 1));
	}

	/** The action names the code fires: every public String constant. */
	private static Set<String> actions() throws IllegalAccessException {
		Set<String> actions = new TreeSet<>();
		for (Field field : ModAdvancements.class.getFields())
			if (field.getType() == String.class && Modifier.isStatic(field.getModifiers()))
				actions.add((String) field.get(null));
		return actions;
	}

	@Test
	void actionNamesAreDistinct() throws IllegalAccessException {
		int constants = 0;
		for (Field field : ModAdvancements.class.getFields())
			if (field.getType() == String.class && Modifier.isStatic(field.getModifiers()))
				constants++;
		assertEquals(constants, actions().size());
	}

	/**
	 * Every advancement on the mod's trigger reads with its codec and names an action the code fires (a typo would
	 * leave it unobtainable), and every action is used by some advancement.
	 */
	@Test
	void advancementsUseActionsTheCodeFires() throws IOException, IllegalAccessException {
		Path dir = ModResourceFiles.find("data", ComedyMonkeysCreateAddons.MOD_ID, "advancement");
		Assumptions.assumeTrue(dir != null && Files.isDirectory(dir), "advancement folder not reachable");
		RegistryOps<JsonElement> ops = VanillaRegistries.createLookup().createSerializationContext(JsonOps.INSTANCE);
		Set<String> known = actions();
		Set<String> used = new HashSet<>();
		List<Path> files;
		try (Stream<Path> list = Files.list(dir)) {
			files = list.filter(file -> file.getFileName().toString().endsWith(".json")).toList();
		}
		assertFalse(files.isEmpty());
		for (Path file : files) {
			JsonObject advancement = ModResourceFiles.readJson(file).getAsJsonObject();
			if (!advancement.has("criteria"))
				continue;
			for (Map.Entry<String, JsonElement> criterion : advancement.getAsJsonObject("criteria").entrySet()) {
				JsonObject body = criterion.getValue().getAsJsonObject();
				if (!TRIGGER.equals(body.get("trigger").getAsString()))
					continue;
				String where = file.getFileName() + " / " + criterion.getKey();
				var result = CMActionTrigger.Instance.CODEC.parse(ops, body.get("conditions"));
				assertTrue(result.result().isPresent(), where + ": " + result.error().map(Object::toString).orElse(""));
				String action = result.result().get().action();
				assertTrue(known.contains(action), where + " uses unknown action " + action);
				used.add(action);
			}
		}
		Set<String> unused = new TreeSet<>(known);
		unused.removeAll(used);
		assertTrue(unused.isEmpty(), "actions no advancement uses: " + unused);
	}
}
