package com.comedymonkey.createaddons.client.helperbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.world.item.DyeColor;

/**
 * The contract his one-pass drawing rests on (HelperBotRig.render, and the Flywheel visuals' single material): the
 * glow texture's lit texels are exactly the faces of his two iris glow cubes, and every dye's texture (and plain) has
 * those very texels, so drawing the irises from his own texture at full brightness looks as the glow pass did.
 * Repainting his textures without keeping the irises equal to glow.png fails here.
 */
class HelperBotTexturesTest {
	private static final String TEXTURES = "/assets/comedymonkeysaddons/textures/entity/helper_bot/";

	private record Texel(int x, int y) implements Comparable<Texel> {
		@Override
		public int compareTo(Texel o) {
			return x != o.x ? Integer.compare(x, o.x) : Integer.compare(y, o.y);
		}
	}

	@Test
	void glowTexelsAreExactlyTheIrisGlowFaces() throws IOException {
		BufferedImage glow = image("glow.png");
		Set<Texel> lit = new TreeSet<>();
		for (int x = 0; x < glow.getWidth(); x++)
			for (int y = 0; y < glow.getHeight(); y++)
				if (alpha(glow, x, y) != 0) {
					lit.add(new Texel(x, y));
					// fully opaque: the old glow pass covered the lit copy of these cubes completely
					assertEquals(255, alpha(glow, x, y), "glow texel " + x + "," + y + " is see-through");
				}
		assertEquals(32, lit.size());

		JsonObject geometry = json(HelperBotModels.GEOMETRY);
		JsonArray size = geometry.getAsJsonArray("texture");
		float sx = glow.getWidth() / size.get(0).getAsFloat(), sy = glow.getHeight() / size.get(1).getAsFloat();
		Set<Texel> irisFaces = new TreeSet<>();
		List<String> glowing = new ArrayList<>();
		for (JsonElement b : geometry.getAsJsonArray("bones")) {
			JsonObject bone = b.getAsJsonObject();
			for (JsonElement c : bone.getAsJsonArray("cubes")) {
				Set<Texel> faces = faces(c.getAsJsonObject(), sx, sy);
				if (faces.stream().anyMatch(lit::contains))
					glowing.add(bone.get("name").getAsString());
				if (bone.get("name").getAsString().startsWith("iris_glow_"))
					irisFaces.addAll(faces);
			}
		}
		// only the two bones the drawing treats as glowing have glow on them, and their faces are all lit
		assertEquals(List.of("iris_glow_right", "iris_glow_left"), glowing);
		assertEquals(Set.of("iris_glow_left", "iris_glow_right"),
			Set.of(HelperBotRig.name(HelperBotRig.Bones.GLOW_LEFT), HelperBotRig.name(HelperBotRig.Bones.GLOW_RIGHT)));
		assertEquals(irisFaces, lit);
	}

	@Test
	void everyTextureHasTheGlowTexelsExactly() throws IOException {
		BufferedImage glow = image("glow.png");
		List<String> files = new ArrayList<>();
		files.add("plain.png");
		for (DyeColor dye : DyeColor.values())
			files.add(dye.getName() + ".png");
		assertEquals(17, files.size());
		Set<String> checked = new HashSet<>();
		for (String file : files) {
			BufferedImage texture = image(file);
			assertEquals(glow.getWidth(), texture.getWidth(), file);
			assertEquals(glow.getHeight(), texture.getHeight(), file);
			for (int x = 0; x < glow.getWidth(); x++)
				for (int y = 0; y < glow.getHeight(); y++)
					if (alpha(glow, x, y) != 0)
						assertEquals(Integer.toHexString(glow.getRGB(x, y)), Integer.toHexString(texture.getRGB(x, y)),
							file + " at " + x + "," + y + " differs from glow.png: his irises would change colour");
			checked.add(file);
		}
		assertEquals(17, checked.size());
		// and those are the textures his drawing asks for
		assertTrue(HelperBotModels.texture(null).getPath().endsWith("helper_bot/plain.png"));
		for (DyeColor dye : DyeColor.values())
			assertTrue(HelperBotModels.texture(dye).getPath().endsWith("helper_bot/" + dye.getName() + ".png"), dye.getName());
	}

	/** The texels a box-UV cube's six faces cover (Minecraft's layout). */
	private static Set<Texel> faces(JsonObject cube, float sx, float sy) {
		JsonArray from = cube.getAsJsonArray("from"), to = cube.getAsJsonArray("to"), uv = cube.getAsJsonArray("uv");
		float dx = to.get(0).getAsFloat() - from.get(0).getAsFloat(), dy = to.get(1).getAsFloat() - from.get(1).getAsFloat();
		float dz = to.get(2).getAsFloat() - from.get(2).getAsFloat(), u = uv.get(0).getAsFloat(), v = uv.get(1).getAsFloat();
		float[][] rects = { { u + dz, v, dx, dz }, { u + dz + dx, v, dx, dz }, { u, v + dz, dz, dy }, { u + dz, v + dz, dx, dy },
			{ u + dz + dx, v + dz, dz, dy }, { u + 2 * dz + dx, v + dz, dx, dy } };
		Set<Texel> out = new HashSet<>();
		for (float[] r : rects)
			for (int x = (int) (r[0] * sx); x < (int) ((r[0] + r[2]) * sx); x++)
				for (int y = (int) (r[1] * sy); y < (int) ((r[1] + r[3]) * sy); y++)
					out.add(new Texel(x, y));
		return out;
	}

	private static int alpha(BufferedImage image, int x, int y) {
		return image.getRGB(x, y) >>> 24;
	}

	private static BufferedImage image(String file) throws IOException {
		try (InputStream in = HelperBotTexturesTest.class.getResourceAsStream(TEXTURES + file)) {
			assertNotNull(in, file);
			return ImageIO.read(in);
		}
	}

	private static JsonObject json(String path) throws IOException {
		try (InputStream in = HelperBotTexturesTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
