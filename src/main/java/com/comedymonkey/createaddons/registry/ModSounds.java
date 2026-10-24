package com.comedymonkey.createaddons.registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own sounds. Trade Bot's are registered from the {@code trade_bot.*} entries of our sounds.json, which
 * {@code design/sound/export-game.js} writes from the approved sound design, so the two can never drift apart.
 */
public final class ModSounds {
	public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
		DeferredRegister.create(Registries.SOUND_EVENT, ComedyMonkeysCreateAddons.MOD_ID);

	private static final String SOUNDS_JSON = "/assets/" + ComedyMonkeysCreateAddons.MOD_ID + "/sounds.json";
	private static final String TRADE_BOT_PREFIX = "trade_bot.";

	/** Trade Bot's sounds by event id (the cue file's id, e.g. "roll_loop"). */
	public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> TRADE_BOT = registerTradeBot();
	/**
	 * The Geothermal Turbine's sounds by event id, from the {@code geothermal_turbine.*} entries that
	 * {@code design/sound/geothermal-turbine/export-game.js} writes.
	 */
	public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> TURBINE = registerPrefixed("geothermal_turbine.");
	/** The Lava Generator's and the Electric Motor's sounds, which {@code design/sound/export-block.js} writes. */
	public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> GENERATOR = registerPrefixed("lava_generator.");
	public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> MOTOR = registerPrefixed("electric_motor.");

	private ModSounds() {}

	/** One of the Lava Generator's sounds; throws for an unknown id. */
	public static SoundEvent generator(String id) {
		return get(GENERATOR, id, "Lava Generator");
	}

	/** One of the Electric Motor's sounds; throws for an unknown id. */
	public static SoundEvent motor(String id) {
		return get(MOTOR, id, "Electric Motor");
	}

	private static SoundEvent get(Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds, String id, String block) {
		DeferredHolder<SoundEvent, SoundEvent> holder = sounds.get(id);
		if (holder == null)
			throw new IllegalArgumentException("no " + block + " sound " + id);
		return holder.get();
	}

	/** One of the Geothermal Turbine's sounds; throws for an unknown id. */
	public static SoundEvent turbine(String id) {
		DeferredHolder<SoundEvent, SoundEvent> holder = TURBINE.get(id);
		if (holder == null)
			throw new IllegalArgumentException("no Geothermal Turbine sound " + id);
		return holder.get();
	}

	private static Map<String, DeferredHolder<SoundEvent, SoundEvent>> registerPrefixed(String prefix) {
		Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds = new LinkedHashMap<>();
		try (InputStream in = ModSounds.class.getResourceAsStream(SOUNDS_JSON)) {
			if (in == null)
				throw new IllegalStateException("missing " + SOUNDS_JSON);
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				for (String key : JsonParser.parseReader(reader).getAsJsonObject().keySet())
					if (key.startsWith(prefix))
						sounds.put(key.substring(prefix.length()), SOUND_EVENTS.register(key,
							() -> SoundEvent.createVariableRangeEvent(ComedyMonkeysCreateAddons.asResource(key))));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return Collections.unmodifiableMap(sounds);
	}

	/** One of Trade Bot's sounds; throws for an unknown id, since ids are fixed by the sound design. */
	public static SoundEvent tradeBot(String id) {
		DeferredHolder<SoundEvent, SoundEvent> holder = TRADE_BOT.get(id);
		if (holder == null)
			throw new IllegalArgumentException("no Trade Bot sound " + id);
		return holder.get();
	}

	private static Map<String, DeferredHolder<SoundEvent, SoundEvent>> registerTradeBot() {
		Map<String, DeferredHolder<SoundEvent, SoundEvent>> sounds = new LinkedHashMap<>();
		try (InputStream in = ModSounds.class.getResourceAsStream(SOUNDS_JSON)) {
			if (in == null)
				throw new IllegalStateException("missing " + SOUNDS_JSON);
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				for (String key : JsonParser.parseReader(reader).getAsJsonObject().keySet()) {
					if (!key.startsWith(TRADE_BOT_PREFIX))
						continue;
					sounds.put(key.substring(TRADE_BOT_PREFIX.length()), SOUND_EVENTS.register(key,
						() -> SoundEvent.createVariableRangeEvent(ComedyMonkeysCreateAddons.asResource(key))));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return Collections.unmodifiableMap(sounds);
	}
}
