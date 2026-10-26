package com.comedymonkey.createaddons.client.tradebot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Trade Bot's and his station's models, built from {@code trade_bot/geometry.json} (exported from the Blockbench
 * preview). This is the one place Blockbench's frame becomes Minecraft's model frame: Blockbench is Y up with him
 * facing -Z and his right at +X; a model is Y down with its feet at y = 24 and the renderer flips X and Y. Turning
 * the Blockbench model half a turn about Z lines the two up without mirroring him: (x, y, z) becomes (-x, 24 - y, z),
 * rotations (rx, ry, rz) become (-rx, -ry, rz), and box-UV faces land where the generator painted them.
 */
public final class TradeBotGeometry {
	public static final String PATH = "/assets/comedymonkeysaddons/trade_bot/geometry.json";
	public static final ModelLayerLocation BOT_LAYER = new ModelLayerLocation(ComedyMonkeysCreateAddons.asResource("trade_bot"), "main");
	public static final ModelLayerLocation STATION_LAYER = new ModelLayerLocation(ComedyMonkeysCreateAddons.asResource("trade_bot"), "station");
	public static final ModelLayerLocation STATION_GLOW_LAYER = new ModelLayerLocation(ComedyMonkeysCreateAddons.asResource("trade_bot"), "station_glow");

	public record Bone(String name, @Nullable String parent, float[] pivot, float[] rotation, List<Cube> cubes) {}

	public record Cube(float[] from, float[] to, int u, int v) {}

	private static volatile TradeBotGeometry instance;

	private final int textureWidth, textureHeight;
	private final List<Bone> bones;

	private TradeBotGeometry(int textureWidth, int textureHeight, List<Bone> bones) {
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.bones = bones;
	}

	public static TradeBotGeometry get() {
		TradeBotGeometry geometry = instance;
		if (geometry == null) {
			synchronized (TradeBotGeometry.class) {
				if (instance == null)
					instance = load();
				geometry = instance;
			}
		}
		return geometry;
	}

	/** The lobbed package and the emerald paid back in a trade: drawn with him, but only while a clip shows them. */
	public static final String PROPS = "trade_props";
	/** His model: the bot, and the trade props beside him (in the same frame, so they fly free of his own moves). */
	public static final String[] BOT_ROOTS = { "trade_bot", PROPS };

	public static LayerDefinition botLayer() {
		return get().layer(BOT_ROOTS);
	}

	public static LayerDefinition stationLayer() {
		return get().layer("station");
	}

	/** Only the station's cubes with any glow on them (its Flywheel visual draws them at full brightness). */
	public static LayerDefinition stationGlowLayer() {
		return get().layer(GlowModel.glowing(get()), "station");
	}

	/** A model of these root bones and everything under them. */
	public LayerDefinition layer(String... roots) {
		return layer(cube -> true, roots);
	}

	/** The same, with only the cubes {@code keep} lets in (every bone stays, so it poses the same). */
	public LayerDefinition layer(Predicate<Cube> keep, String... roots) {
		MeshDefinition mesh = new MeshDefinition();
		Map<String, PartDefinition> parts = new HashMap<>();
		Map<String, Bone> byName = new HashMap<>();
		for (Bone bone : bones)
			byName.put(bone.name(), bone);
		for (Bone bone : bones) {
			boolean isRoot = List.of(roots).contains(bone.name());
			if (!isRoot && !isUnder(bone, roots, byName))
				continue;
			PartDefinition parent = isRoot ? mesh.getRoot() : parts.get(bone.parent());
			float[] p = bone.pivot();
			float[] parentPivot = isRoot ? null : byName.get(bone.parent()).pivot();
			CubeListBuilder cubes = CubeListBuilder.create();
			for (Cube cube : bone.cubes())
				if (keep.test(cube))
						cubes.texOffs(cube.u(), cube.v()).addBox(p[0] - cube.to()[0], p[1] - cube.to()[1], cube.from()[2] - p[2],
						cube.to()[0] - cube.from()[0], cube.to()[1] - cube.from()[1], cube.to()[2] - cube.from()[2]);
			PartPose pose = PartPose.offsetAndRotation(
				parentPivot == null ? -p[0] : -(p[0] - parentPivot[0]),
				parentPivot == null ? 24 - p[1] : -(p[1] - parentPivot[1]),
				parentPivot == null ? p[2] : p[2] - parentPivot[2],
				-bone.rotation()[0] * Mth.DEG_TO_RAD, -bone.rotation()[1] * Mth.DEG_TO_RAD, bone.rotation()[2] * Mth.DEG_TO_RAD);
			parts.put(bone.name(), parent.addOrReplaceChild(bone.name(), cubes, pose));
		}
		return LayerDefinition.create(mesh, textureWidth, textureHeight);
	}

	/** The texture's size the cubes' UVs are laid out on. */
	public int textureWidth() {
		return textureWidth;
	}

	public int textureHeight() {
		return textureHeight;
	}

	/** Every bone of a baked layer by name, for posing. */
	public Map<String, ModelPart> parts(ModelPart bakedRoot, String... roots) {
		Map<String, ModelPart> parts = new LinkedHashMap<>();
		Map<String, Bone> byName = new HashMap<>();
		for (Bone bone : bones)
			byName.put(bone.name(), bone);
		for (Bone bone : bones) {
			if (List.of(roots).contains(bone.name()))
				parts.put(bone.name(), bakedRoot.getChild(bone.name()));
			else if (isUnder(bone, roots, byName))
				parts.put(bone.name(), parts.get(bone.parent()).getChild(bone.name()));
		}
		return parts;
	}

	/** The bones directly under {@code root}. */
	public List<String> children(String root) {
		return bones.stream().filter(bone -> root.equals(bone.parent())).map(Bone::name).toList();
	}

	private static boolean isUnder(Bone bone, String[] roots, Map<String, Bone> byName) {
		for (String p = bone.parent(); p != null; p = byName.get(p).parent())
			if (List.of(roots).contains(p))
				return true;
		return false;
	}

	private static TradeBotGeometry load() {
		try (InputStream in = TradeBotGeometry.class.getResourceAsStream(PATH)) {
			if (in == null)
				throw new IllegalStateException("missing " + PATH);
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				JsonArray texture = json.getAsJsonArray("texture");
				List<Bone> bones = new ArrayList<>();
				for (JsonElement e : json.getAsJsonArray("bones")) {
					JsonObject b = e.getAsJsonObject();
					List<Cube> cubes = new ArrayList<>();
					for (JsonElement c : b.getAsJsonArray("cubes")) {
						JsonObject cube = c.getAsJsonObject();
						JsonArray uv = cube.getAsJsonArray("uv");
						cubes.add(new Cube(floats(cube.getAsJsonArray("from")), floats(cube.getAsJsonArray("to")), uv.get(0).getAsInt(), uv.get(1).getAsInt()));
					}
					bones.add(new Bone(b.get("name").getAsString(), b.get("parent").isJsonNull() ? null : b.get("parent").getAsString(),
						floats(b.getAsJsonArray("pivot")), floats(b.getAsJsonArray("rotation")), cubes));
				}
				return new TradeBotGeometry(texture.get(0).getAsInt(), texture.get(1).getAsInt(), bones);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static float[] floats(JsonArray array) {
		float[] out = new float[array.size()];
		for (int i = 0; i < out.length; i++)
			out[i] = array.get(i).getAsFloat();
		return out;
	}
}
