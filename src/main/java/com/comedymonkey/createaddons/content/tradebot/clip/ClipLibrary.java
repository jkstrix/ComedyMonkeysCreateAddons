package com.comedymonkey.createaddons.content.tradebot.clip;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Trade Bot's clips, read once from the mod jar. Both sides use it: the server for timings, root movement and cue
 * times; the client to pose the model. They're read straight from the jar rather than through resource packs, so
 * server and client always agree. A clip's id (sent to clients) is its position in the file.
 */
public final class ClipLibrary {
	public static final String PATH = "/assets/comedymonkeysaddons/trade_bot/clips.json";

	private static volatile ClipLibrary instance;
	/** Other characters' clip files (Helper Bot's), by path. */
	private static final Map<String, ClipLibrary> OTHERS = new ConcurrentHashMap<>();

	private final List<Clip> clips;
	private final Map<String, Clip> byName = new LinkedHashMap<>();

	private ClipLibrary(List<Clip> clips) {
		this.clips = Collections.unmodifiableList(clips);
		for (Clip clip : clips)
			byName.put(clip.name(), clip);
	}

	public static ClipLibrary get() {
		ClipLibrary lib = instance;
		if (lib == null) {
			synchronized (ClipLibrary.class) {
				if (instance == null)
					instance = load();
				lib = instance;
			}
		}
		return lib;
	}

	/** The clips in another character's file, in the same format (read once). */
	public static ClipLibrary of(String path) {
		return PATH.equals(path) ? get() : OTHERS.computeIfAbsent(path, ClipLibrary::load);
	}

	public List<Clip> all() {
		return clips;
	}

	/** The clip with this name; throws if there's none, since names are fixed in code. */
	public Clip clip(String name) {
		Clip clip = byName.get(name);
		if (clip == null)
			throw new IllegalArgumentException("no clip " + name);
		return clip;
	}

	@Nullable
	public Clip find(String name) {
		return byName.get(name);
	}

	public int id(Clip clip) {
		return clips.indexOf(clip);
	}

	@Nullable
	public Clip byId(int id) {
		return id >= 0 && id < clips.size() ? clips.get(id) : null;
	}

	private static ClipLibrary load() {
		return load(PATH);
	}

	private static ClipLibrary load(String path) {
		try (InputStream in = ClipLibrary.class.getResourceAsStream(path)) {
			if (in == null)
				throw new IllegalStateException("missing " + path);
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return parse(JsonParser.parseReader(reader).getAsJsonObject());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static ClipLibrary parse(JsonObject json) {
		List<Clip> clips = new ArrayList<>();
		for (JsonElement e : json.getAsJsonArray("clips")) {
			JsonObject c = e.getAsJsonObject();
			Map<String, Map<String, Track>> tracks = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> bone : c.getAsJsonObject("tracks").entrySet()) {
				Map<String, Track> channels = new LinkedHashMap<>();
				for (Map.Entry<String, JsonElement> ch : bone.getValue().getAsJsonObject().entrySet()) {
					JsonObject t = ch.getValue().getAsJsonObject();
					channels.put(ch.getKey(), new Track(t.get("fps").getAsFloat(), "catmullrom".equals(t.get("interp").getAsString()),
						floats(t.getAsJsonArray("values"))));
				}
				tracks.put(bone.getKey(), channels);
			}
			Map<String, Map<String, StepTrack>> steps = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> bone : c.getAsJsonObject("steps").entrySet()) {
				Map<String, StepTrack> channels = new LinkedHashMap<>();
				for (Map.Entry<String, JsonElement> ch : bone.getValue().getAsJsonObject().entrySet()) {
					JsonArray keys = ch.getValue().getAsJsonArray();
					float[] times = new float[keys.size()], values = new float[keys.size() * 3];
					for (int k = 0; k < keys.size(); k++) {
						JsonArray key = keys.get(k).getAsJsonArray();
						times[k] = key.get(0).getAsFloat();
						for (int a = 0; a < 3; a++)
							values[k * 3 + a] = key.get(1 + a).getAsFloat();
					}
					channels.put(ch.getKey(), new StepTrack(times, values));
				}
				steps.put(bone.getKey(), channels);
			}
			List<Clip.Cue> cues = new ArrayList<>();
			for (JsonElement cue : c.getAsJsonArray("cues"))
				cues.add(new Clip.Cue(cue.getAsJsonArray().get(0).getAsFloat(), cue.getAsJsonArray().get(1).getAsString()));
			clips.add(new Clip(c.get("name").getAsString(), c.get("length").getAsFloat(),
				Clip.Mode.valueOf(c.get("mode").getAsString().toUpperCase(Locale.ROOT)),
				Clip.Root.valueOf(c.get("root").getAsString().toUpperCase(Locale.ROOT)), tracks, steps, List.copyOf(cues)));
		}
		return new ClipLibrary(clips);
	}

	private static float[] floats(JsonArray array) {
		float[] out = new float[array.size()];
		for (int i = 0; i < out.length; i++)
			out[i] = array.get(i).getAsFloat();
		return out;
	}
}
