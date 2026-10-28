package com.comedymonkey.createaddons.content.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * Every Electric Light has what the game needs (blockstate, body, glass and glow models, item model, loot, a recipe and
 * a name), its brightness steps and draws are as the spec says, and its outline turns the same way as its model.
 */
class LightBlockTest {
	private static final String NS = "comedymonkeysaddons";

	private static Path need(String... path) {
		Path p = ModResourceFiles.find(path);
		assertNotNull(p, "missing " + String.join("/", path));
		return p;
	}

	@Test
	void everyLightHasItsFiles() {
		JsonObject lang = ModResourceFiles.readJson(need("assets", NS, "lang", "en_us.json")).getAsJsonObject();
		for (LightKind k : LightKind.values()) {
			need("assets", NS, "blockstates", k.id() + ".json");
			for (String part : new String[] {"", "_glass", "_glow"})
				need("assets", NS, "models", "block", "light", k.id() + part + ".json");
			need("assets", NS, "models", "item", k.id() + ".json");
			need("data", NS, "loot_table", "blocks", k.id() + ".json");
			need("data", NS, "recipe", "light", k.id() + ".json");
			assertTrue(lang.has("block." + NS + "." + k.id()), k.id() + " has no name");
		}
		need("data", NS, "recipe", "light_assembly.json");
	}

	/** P1 light levels, and each step dimmer but never dark. */
	@Test
	void levelsFollowTheSteps() {
		assertEquals(4, LightKind.INDICATOR.level());
		assertEquals(14, LightKind.BULKHEAD.level());
		assertEquals(15, LightKind.HIGH_BAY.level());
		for (LightKind k : LightKind.values())
			for (int s = 1; s < LightKind.STEPS.length; s++) {
				assertTrue(k.level(s) <= k.level(s - 1), k + " step " + s + " is brighter than the one before");
				assertTrue(k.level(s) >= 1, k + " step " + s + " is dark");
			}
	}

	/** A wall light's outline sits against the wall behind it, whichever way it faces (as the blockstate turns its model). */
	@Test
	void wallOutlinesSitOnTheirWall() {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			AABB box = LightBlock.turned(LightKind.BULKHEAD, facing).bounds();
			Direction wall = facing.getOpposite();
			double back = switch (wall) {
				case NORTH -> box.minZ;
				case SOUTH -> 1 - box.maxZ;
				case WEST -> box.minX;
				default -> 1 - box.maxX;
			};
			assertEquals(0, back, 1e-6, "facing " + facing + ", the Bulkhead isn't against its wall");
		}
		// on a floor (facing up) the Indicator lies on the floor; on a ceiling (facing down) against the ceiling
		assertEquals(0, LightBlock.turned(LightKind.INDICATOR, Direction.UP).bounds().minY, 1e-6);
		assertEquals(1, LightBlock.turned(LightKind.INDICATOR, Direction.DOWN).bounds().maxY, 1e-6);
	}

	/** The blockstate turns each wall light's model by the same angles the outline (and the renderer) use. */
	@Test
	void blockstateTurnsMatchTheOutline() {
		JsonElement state = ModResourceFiles.readJson(need("assets", NS, "blockstates", "bulkhead_light.json"));
		for (JsonElement part : state.getAsJsonObject().getAsJsonArray("multipart")) {
			JsonObject p = part.getAsJsonObject();
			if (!p.has("when") || p.getAsJsonObject("when").has("light"))
				continue;
			String facing = p.getAsJsonObject("when").get("facing").getAsString();
			int y = p.getAsJsonObject("apply").has("y") ? p.getAsJsonObject("apply").get("y").getAsInt() : 0;
			int expected = switch (facing) {
				case "west" -> 90;
				case "north" -> 180;
				case "east" -> 270;
				default -> 0;
			};
			assertEquals(expected, y, "facing " + facing);
		}
	}
}
