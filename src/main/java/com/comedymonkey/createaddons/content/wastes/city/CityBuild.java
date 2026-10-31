package com.comedymonkey.createaddons.content.wastes.city;

import com.comedymonkey.createaddons.content.wastes.CityRecord;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a whole Clockwork Wastes city into a live level at once (D66: the dev command {@code /wastes buildcity}, and the
 * game tests): optionally an End Stone island for it to stand on first, then its {@link CityPlan} through
 * {@link CityApplier.Live}, and its record. The same plan world generation builds a chunk at a time.
 * <p>
 * The test island ({@link #island}) is round, about 78 blocks across the middle, so the city's core fits on it but its
 * edges don't: the Rail Yard's far corner is bitten off (the city stands on its deck and trestles there, P3) and the
 * north and east boulevards run out over the void as bridges, which snap.
 */
public final class CityBuild {
	private CityBuild() {}

	public record Result(BlockPos origin, CityPlan plan, int ops) {}

	public static Result build(ServerLevel level, BlockPos origin, long seed, boolean withIsland) {
		return build(level, origin, seed, withIsland, CityLayout.BUILDING_KIT);
	}

	/** {@code kit}: the city with the building kit's buildings (D71: the swap, previewed before it's made). */
	public static Result build(ServerLevel level, BlockPos origin, long seed, boolean withIsland, boolean kit) {
		if (withIsland)
			island(level, origin);
		CityPlan plan = CityPlan.of(origin, seed, false, kit);
		// built again in the same place: it starts asleep, with only its new bots
		CityRecord.of(level).forget(origin);
		for (var bot : com.comedymonkey.createaddons.content.wastes.bot.WastesBots.loaded(level,
			com.comedymonkey.createaddons.content.wastes.bot.CityBot.class, origin, b -> true))
			bot.discard();
		CityRecord.of(level).add(origin);
		new CityApplier.Live(level, origin).apply(plan.ops());
		return new Result(origin, plan, plan.ops().size());
	}

	/**
	 * An outskirts-only site (P46: one whose island doesn't fit a city) into a live level at once: wrecks, the siege ring,
	 * the war-frames and the resistance's Elytra cache (D69), nothing bound and no record, as world generation would build
	 * it. {@code withIsland}: on the test island (so every wreck shows), else on the ground that's there.
	 */
	public static Result buildOutskirts(ServerLevel level, BlockPos origin, long seed, boolean withIsland) {
		if (withIsland)
			island(level, origin);
		CityPlan plan = CityPlan.of(origin, seed, true);
		new CityApplier.Live(level, origin).apply(plan.ops());
		return new Result(origin, plan, plan.ops().size());
	}

	/**
	 * A top-down picture of a built city (each column's top block's map colour, shaded by height, 3 px a block) for
	 * reviewing the layout without a client. Writes a PNG.
	 */
	public static void writeMap(ServerLevel level, BlockPos origin, java.nio.file.Path file) throws java.io.IOException {
		int r = CityLayout.OUT, scale = 3, n = 2 * r + 1;
		var img = new java.awt.image.BufferedImage(n * scale, n * scale, java.awt.image.BufferedImage.TYPE_INT_RGB);
		for (int dx = -r; dx <= r; dx++)
			for (int dz = -r; dz <= r; dz++) {
				int x = origin.getX() + dx, z = origin.getZ() + dz;
				int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) - 1;
				int rgb = 0x101018;
				if (top > level.getMinBuildHeight()) {
					BlockPos p = new BlockPos(x, top, z);
					int c = level.getBlockState(p).getMapColor(level, p).col;
					double shade = Math.max(0.45, Math.min(1.25, 0.8 + (top - origin.getY()) * 0.02));
					int red = (int) Math.min(255, (c >> 16 & 255) * shade), g = (int) Math.min(255, (c >> 8 & 255) * shade), b = (int) Math.min(255, (c & 255) * shade);
					rgb = red << 16 | g << 8 | b;
				}
				for (int i = 0; i < scale; i++)
					for (int j = 0; j < scale; j++)
						img.setRGB((dx + r) * scale + i, (dz + r) * scale + j, rgb);
			}
		file = file.toAbsolutePath();
		java.nio.file.Files.createDirectories(file.getParent());
		javax.imageio.ImageIO.write(img, "png", file.toFile());
	}

	/** The test island's radius at an angle (blocks). */
	static double radius(double angle) {
		return 80 + 6 * Math.sin(3 * angle + 0.7) + 4 * Math.cos(5 * angle);
	}

	/** Whether the test island has land under (dx, dz) from its middle. */
	public static boolean islandAt(int dx, int dz) {
		double d = Math.sqrt(dx * dx + dz * dz), a = Math.atan2(dz, dx);
		// the Rail Yard's far corner over the void (struts), the north and east boulevards' far ends too (bridges)
		if (dx > 44 && dz > 44 && dx + dz > 104)
			return false;
		if (Math.abs(dx) <= 6 && dz < -74 || Math.abs(dz) <= 6 && dx > 74)
			return false;
		return d <= radius(a);
	}

	/** An End Stone island under the city: a dome 3-17 blocks deep, its top at the city's floor. */
	public static void island(ServerLevel level, BlockPos origin) {
		BlockState stone = Blocks.END_STONE.defaultBlockState();
		int r = CityLayout.OUT;
		for (int dx = -r; dx <= r; dx++)
			for (int dz = -r; dz <= r; dz++) {
				if (!islandAt(dx, dz))
					continue;
				double d = Math.sqrt(dx * dx + dz * dz), f = Math.min(1, d / radius(Math.atan2(dz, dx)));
				int depth = 3 + (int) Math.round(14 * (1 - f * f));
				for (int y = 1; y <= depth; y++)
					level.setBlock(origin.offset(dx, -y, dz), stone, Block.UPDATE_CLIENTS);
			}
	}
}
