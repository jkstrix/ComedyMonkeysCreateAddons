package com.comedymonkey.createaddons.testutil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Loads the mod's config with its default values, so code that reads
 * {@code CMConfig.SERVER.x.get()} works in unit tests. A test can change a value
 * with {@link #set}; {@link #reset} (e.g. from {@code @AfterEach}) puts every default back.
 */
public final class TestConfig {
	private static boolean loaded;

	private TestConfig() {}

	/** Loads the server and client configs with their defaults, once. */
	public static synchronized void loadDefaults() {
		if (!loaded) {
			reset();
		}
	}

	/** Reloads every config with its default values. */
	public static synchronized void reset() {
		ConfigTracker.INSTANCE.loadDefaultServerConfigs();
		try {
			ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.CLIENT, Files.createTempDirectory("cm-test-config"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		loaded = true;
	}

	/** Changes one config value in memory, until the next {@link #reset}. */
	public static <T> void set(ModConfigSpec.ConfigValue<T> value, T newValue) {
		loadDefaults();
		value.set(newValue);
		value.clearCache();
	}
}
