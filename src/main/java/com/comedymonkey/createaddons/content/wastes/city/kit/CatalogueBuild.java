package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.city.CityApplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code /wastes buildcatalogue} (D71): the building kit's catalogue built beside you on a flat End Stone island of its own,
 * every exhibit on its pad with a sign naming it, bound to the catalogue's own city record (wake its glows with
 * {@code /wastes wake all}, then {@code /wastes heart <minutes>} to feed them). Writes the fixed camera marks for the
 * screenshot set ({@code wastes-catalogue.json} in the game folder): street level, three-quarter, the row from a distance,
 * and the close-ups of the joins, so every shot can be retaken identically after a change.
 */
public final class CatalogueBuild {
	private CatalogueBuild() {}

	/** A camera mark: where the player stands (feet) and looks (yaw, pitch). */
	public record Shot(String label, double x, double y, double z, float yaw, float pitch, double[] facing) {
		Shot(String label, double x, double y, double z, float yaw, float pitch) {
			this(label, x, y, z, yaw, pitch, null);
		}

		/** The teleport: turned by yaw and pitch, or looking straight at a point (the game works out the turn). */
		public String tp() {
			if (facing != null)
				return String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f facing %.2f %.2f %.2f", x, y, z, facing[0], facing[1], facing[2]);
			return String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch);
		}
	}

	public static Catalogue.Built build(ServerLevel level, BlockPos o, long seed) {
		Catalogue.Built b = Catalogue.plan(o, seed);
		island(level, o, b);
		CityRecord record = CityRecord.of(level);
		record.forget(b.recordOrigin());
		record.add(b.recordOrigin());
		new CityApplier.Live(level, b.recordOrigin()).apply(b.plan().ops());
		for (Catalogue.Placed p : b.placed())
			sign(level, new BlockPos(p.min().getX(), o.getY(), p.max().getZ() + 2), p.label());
		try {
			writeMarks(level.getServer().getServerDirectory().resolve("wastes-catalogue.json"), b);
		} catch (IOException e) {
			com.mojang.logging.LogUtils.getLogger().warn("couldn't write the catalogue's camera marks", e);
		}
		return b;
	}

	/** A flat End Stone island under the whole catalogue and its camera side: 4 deep, its top at the floor. */
	static void island(ServerLevel level, BlockPos o, Catalogue.Built b) {
		BlockState stone = Blocks.END_STONE.defaultBlockState();
		for (int x = b.minX() - 4; x <= b.maxX() + 4; x++)
			for (int z = b.minZ() - 4; z <= b.maxZ() + 30; z++)
				for (int y = 1; y <= 4; y++)
					level.setBlock(new BlockPos(x, o.getY() - y, z), stone, Block.UPDATE_CLIENTS);
	}

	/** A dark oak sign on the pad's front corner, facing the camera side, with the exhibit's name. */
	static void sign(ServerLevel level, BlockPos at, String label) {
		level.setBlock(at, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0), Block.UPDATE_ALL);
		if (!(level.getBlockEntity(at) instanceof SignBlockEntity be))
			return;
		List<String> lines = wrap(label, 15, 4);
		SignText text = new SignText();
		for (int i = 0; i < lines.size(); i++)
			text = text.setMessage(i, Component.literal(lines.get(i)));
		be.setText(text, true);
		be.setChanged();
		level.sendBlockUpdated(at, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
	}

	static List<String> wrap(String s, int width, int max) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String w : s.split(" ")) {
			if (line.length() > 0 && line.length() + 1 + w.length() > width) {
				out.add(line.toString());
				line.setLength(0);
			}
			if (line.length() > 0)
				line.append(' ');
			line.append(w);
		}
		if (line.length() > 0)
			out.add(line.toString());
		return out.size() > max ? out.subList(0, max) : out;
	}

	/** Each exhibit's street-level and three-quarter marks (the brief's screenshot set 1 and 2). */
	public static List<Shot> shots(Catalogue.Placed p, int floor) {
		double cx = (p.min().getX() + p.max().getX()) / 2.0 + 0.5, cz = (p.min().getZ() + p.max().getZ()) / 2.0 + 0.5;
		int h = p.max().getY() - floor + 1;
		int width = p.max().getX() - p.min().getX() + 1, depth = p.max().getZ() - p.min().getZ() + 1;
		List<Shot> out = new ArrayList<>();
		// street level: eye height 1.6, 10 blocks from the main (south) facade, looking slightly up
		out.add(new Shot(p.id() + "-street", cx, floor, p.max().getZ() + 11.5, 180, -8));
		// three-quarter: 45 degrees in plan from the south-east, about 25 down, at 2.5 x its height (and wide enough for its length)
		double d = Math.max(Math.max(2.5 * h, 1.15 * Math.max(width, depth)), 20);
		double rad = Math.toRadians(25), horiz = d * Math.cos(rad);
		double cy = floor + h * 0.35;
		double tx = cx + horiz / Math.sqrt(2), tz = cz + horiz / Math.sqrt(2), ty = cy + d * Math.sin(rad) - 1.62;
		out.add(new Shot(p.id() + "-threequarter", tx, ty, tz, 135, 25, new double[] { cx, cy, cz }));
		return out;
	}

	/** Each row from the distance (as from the next island): 200 blocks south of its middle, at the floor, looking north. */
	public static List<Shot> rowShots(Catalogue.Built b, int floor) {
		List<Shot> out = new ArrayList<>();
		for (int row = 0; row < 5; row++) {
			int r = row;
			int minX = b.placed().stream().filter(p -> p.row() == r).mapToInt(p -> p.min().getX()).min().orElse(0);
			int maxX = b.placed().stream().filter(p -> p.row() == r).mapToInt(p -> p.max().getX()).max().orElse(0);
			int front = b.placed().stream().filter(p -> p.row() == r).mapToInt(p -> p.max().getZ()).max().orElse(0);
			out.add(new Shot("row" + (row + 1) + "-distance", (minX + maxX) / 2.0, floor + 2, front + 200, 180, 0));
		}
		return out;
	}

	static void writeMarks(Path file, Catalogue.Built b) throws IOException {
		int floor = b.recordOrigin().getY();
		StringBuilder j = new StringBuilder("{\n  \"record\": [" + b.recordOrigin().getX() + ", " + floor + ", " + b.recordOrigin().getZ() + "],\n  \"exhibits\": [\n");
		for (int i = 0; i < b.placed().size(); i++) {
			Catalogue.Placed p = b.placed().get(i);
			j.append("    {\"id\": \"").append(p.id()).append("\", \"label\": \"").append(p.label().replace("\"", "'")).append("\", \"row\": ").append(p.row() + 1)
				.append(", \"wear\": \"").append(p.wear().name().toLowerCase(Locale.ROOT)).append("\", \"min\": [").append(p.min().getX()).append(", ")
				.append(p.min().getY()).append(", ").append(p.min().getZ()).append("], \"max\": [").append(p.max().getX()).append(", ").append(p.max().getY())
				.append(", ").append(p.max().getZ()).append("], \"shots\": [");
			List<Shot> s = shots(p, floor);
			for (int k = 0; k < s.size(); k++)
				j.append(k > 0 ? ", " : "").append("{\"label\": \"").append(s.get(k).label()).append("\", \"tp\": \"").append(s.get(k).tp()).append("\"}");
			j.append("]}").append(i < b.placed().size() - 1 ? "," : "").append("\n");
		}
		j.append("  ],\n  \"rows\": [");
		List<Shot> rows = rowShots(b, floor);
		for (int k = 0; k < rows.size(); k++)
			j.append(k > 0 ? ", " : "").append("{\"label\": \"").append(rows.get(k).label()).append("\", \"tp\": \"").append(rows.get(k).tp()).append("\"}");
		j.append("]\n}\n");
		Files.writeString(file.toAbsolutePath(), j.toString(), StandardCharsets.UTF_8);
	}
}
