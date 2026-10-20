package com.comedymonkey.createaddons.testutil;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;

/** The mod's recipe JSON files, and the ops to decode them with, for recipe codec tests. */
public final class ModRecipeFiles {
	public static final HolderLookup.Provider REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

	private ModRecipeFiles() {}

	public static DynamicOps<JsonElement> jsonOps() {
		return RegistryOps.create(JsonOps.INSTANCE, REGISTRIES);
	}

	/** Every .json file in data/comedymonkeysaddons/recipe/{folder}, by file name; skips the test if not found. */
	public static Map<String, JsonObject> folder(String folder) {
		Path dir = ModResourceFiles.find("data", "comedymonkeysaddons", "recipe", folder);
		assumeTrue(dir != null && Files.isDirectory(dir), "recipe folder " + folder + " not found");
		Map<String, JsonObject> files = new TreeMap<>();
		try (Stream<Path> list = Files.list(dir)) {
			list.filter(p -> p.getFileName().toString().endsWith(".json"))
				.forEach(p -> files.put(p.getFileName().toString(), ModResourceFiles.readJson(p).getAsJsonObject()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return files;
	}

	/** One recipe file, by its path under data/comedymonkeysaddons/recipe (e.g. "wellspring.json"); skips if not found. */
	public static JsonObject file(String... path) {
		String[] full = new String[path.length + 3];
		full[0] = "data";
		full[1] = "comedymonkeysaddons";
		full[2] = "recipe";
		System.arraycopy(path, 0, full, 3, path.length);
		Path file = ModResourceFiles.find(full);
		assumeTrue(file != null, "recipe " + String.join("/", path) + " not found");
		return ModResourceFiles.readJson(file).getAsJsonObject();
	}

	public static JsonObject json(String text) {
		return JsonParser.parseString(text).getAsJsonObject();
	}
}
