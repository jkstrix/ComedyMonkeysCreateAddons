package com.comedymonkey.createaddons.content.wastes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The winding tower's props (D80-D84), as design/models/clockwork-wastes-props.export.js writes them, checked where only a
 * client or a world would otherwise find a fault: every piece has its blockstate (all four facings), its model and the
 * textures it names, its item model, a loot table that drops itself (D37), a name, and its mining tag (the spring a
 * pickaxe's, the rafters an axe's); its outline lies in its own block; and every piece is one the tower lays.
 */
class WindingTowerPiecesAssetsTest {
	private static final String NS = "comedymonkeysaddons";

	private static JsonObject json(String path) throws IOException {
		try (InputStream in = WindingTowerPiecesAssetsTest.class.getResourceAsStream(path)) {
			assertNotNull(in, "missing " + path);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static Set<String> tag(String tool) throws IOException {
		Set<String> out = new HashSet<>();
		// (an entry is an id, or {id, required})
		for (JsonElement e : json("/data/minecraft/tags/block/mineable/" + tool + ".json").getAsJsonArray("values"))
			out.add(e.isJsonObject() ? e.getAsJsonObject().get("id").getAsString() : e.getAsString());
		return out;
	}

	@Test
	void everyPieceHasItsAssetsAndData() throws IOException {
		JsonObject lang = json("/assets/" + NS + "/lang/en_us.json");
		Set<String> pickaxe = tag("pickaxe"), axe = tag("axe");
		for (int i = 0; i < WindingTowerPieces.PIECES.length; i++) {
			String id = WindingTowerPieces.PIECES[i].id();
			JsonObject variants = json("/assets/" + NS + "/blockstates/" + id + ".json").getAsJsonObject("variants");
			for (String facing : new String[] { "north", "east", "south", "west" })
				assertEquals(NS + ":block/wastes/" + id, variants.getAsJsonObject("facing=" + facing).get("model").getAsString(), id + " facing " + facing);
			JsonObject model = json("/assets/" + NS + "/models/block/wastes/" + id + ".json");
			assertTrue(model.getAsJsonArray("elements").size() > 0, id + " has no elements");
			for (var t : model.getAsJsonObject("textures").entrySet()) {
				String ref = t.getValue().getAsString();
				if (ref.startsWith(NS + ":"))
					assertNotNull(WindingTowerPiecesAssetsTest.class.getResourceAsStream("/assets/" + NS + "/textures/" + ref.substring(NS.length() + 1) + ".png"),
						id + ": no texture " + ref);
			}
			assertEquals(NS + ":block/wastes/" + id, json("/assets/" + NS + "/models/item/" + id + ".json").get("parent").getAsString(), id + "'s item");
			JsonObject loot = json("/data/" + NS + "/loot_table/blocks/" + id + ".json");
			assertEquals(NS + ":" + id, loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject()
				.get("name").getAsString(), id + " doesn't drop itself");
			assertTrue(lang.has("block." + NS + "." + id), id + " has no name");
			boolean spring = i < WindingTowerPieces.SPRING_PIECES;
			assertTrue((spring ? pickaxe : axe).contains(NS + ":" + id), id + " isn't in the " + (spring ? "pickaxe" : "axe") + " tag");
			double[] b = WindingTowerPieces.PIECES[i].box();
			assertTrue(b[0] >= 0 && b[1] >= 0 && b[2] >= 0 && b[3] <= 16 && b[4] <= 16 && b[5] <= 16 && b[0] < b[3] && b[1] < b[4] && b[2] < b[5],
				id + "'s outline isn't in its own block");
		}
	}

	/**
	 * D87: the kit's floating rule counts a multi-block prop standing in every cell its pieces reach. A spiral section's
	 * piece put so its reach touches the ground at one cell is held, and so is a second piece whose reach meets the first's
	 * (though the two pieces' own blocks are far apart); the same prop lifted clear of everything is held by nothing.
	 */
	@Test
	void aPropTouchingTheGroundAtOneCellStandsAndOneTouchingNothingFloats() {
		int a = index("sprung_mainspring_spiral_2_0"), b = index("sprung_mainspring_spiral_2_1");
		net.minecraft.world.level.block.state.BlockState sa = com.comedymonkey.createaddons.registry.ModWastes.WINDING_TOWER_PIECES.get(a).get().defaultBlockState(),
			sb = com.comedymonkey.createaddons.registry.ModWastes.WINDING_TOWER_PIECES.get(b).get().defaultBlockState();
		// each as the tower lays it (their reaches meet there); then lifted so the lower one's lowest reached cell is at y 0
		net.minecraft.core.BlockPos pa = layOf(a), pb = layOf(b);
		int low = PropPieceBlock.reach(pa, sa).stream().mapToInt(net.minecraft.core.BlockPos::getY).min().orElseThrow();
		net.minecraft.core.BlockPos ga = pa.below(low), gb = pb.below(low);
		java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> grounded = java.util.Map.of(ga, sa, gb, sb);
		var held = com.comedymonkey.createaddons.content.wastes.city.kit.ModelChecks.joined(grounded, p -> p.getY() <= 0);
		assertTrue(held.contains(ga) && held.contains(gb), "a prop on the ground at one cell holds all its pieces: " + held);
		assertTrue(ga.distManhattan(gb) > 1, "the two pieces' own blocks aren't neighbours (only their reaches join them)");
		java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState> lifted = java.util.Map.of(ga.above(20), sa, gb.above(20), sb);
		assertTrue(com.comedymonkey.createaddons.content.wastes.city.kit.ModelChecks.joined(lifted, p -> p.getY() <= 0).isEmpty(),
			"a prop touching nothing is held by nothing");
	}

	private static int index(String id) {
		for (int i = 0; i < WindingTowerPieces.PIECES.length; i++)
			if (WindingTowerPieces.PIECES[i].id().equals(id))
				return i;
		throw new AssertionError("no piece " + id);
	}

	private static net.minecraft.core.BlockPos layOf(int piece) {
		for (WindingTowerPieces.Lay l : WindingTowerPieces.LAYS)
			if (l.piece() == piece)
				return new net.minecraft.core.BlockPos(l.x(), l.y(), l.z());
		throw new AssertionError("piece " + piece + " isn't laid");
	}

	@Test
	void everyCellBelongsToALaying() {
		for (WindingTowerPieces.Lay l : WindingTowerPieces.LAYS)
			assertTrue(l.piece() >= 0 && l.piece() < WindingTowerPieces.PIECES.length, "a laying's piece " + l.piece());
		for (int[] cells : new int[][] { WindingTowerPieces.SPRING_CELLS, WindingTowerPieces.RAFTER_CELLS }) {
			assertEquals(0, cells.length % 4, "cells come in fours (x, y, z, laying)");
			for (int k = 3; k < cells.length; k += 4)
				assertTrue(cells[k] >= 0 && cells[k] < WindingTowerPieces.LAYS.length, "a cell's laying " + cells[k]);
		}
	}
}
