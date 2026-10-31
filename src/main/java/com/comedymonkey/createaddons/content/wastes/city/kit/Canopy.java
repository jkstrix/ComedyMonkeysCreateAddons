package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Type 14 (D73: in the kit), the Rail Yard's platform canopy: the makers' shed roof (a 1:2 slate pitch, its copper ridge,
 * walnut barges) on cast-iron posts every 6 along both edges of a raised platform, walnut tie beams across at the posts.
 * The platform (x 0..l-1, z 0..3): the Rail Yard's engineering brick with a tuff coping on its track edge (z 0), its charging
 * cradles (Charging Alcoves where passengers waited) against the back. In front, the track (rails on gravel, walnut
 * sleepers). Its signal box is a small {@link Shed}.
 */
public final class Canopy {
	private Canopy() {}

	public static final int D = 4, EAVE = 4;

	/** The cast-iron post: polished blackstone wall (dark, round-shouldered; provisional, see the progress doc). */
	static BlockState post() {
		return Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState();
	}

	public static Model build(Ctx c, int l) {
		return build(c, l, true);
	}

	/** {@code track}: lay a short track in front (the catalogue); the city's yard lays its own under the train. */
	public static Model build(Ctx c, int l, boolean track) {
		Model md = new Model();
		Masonry m = c.m();
		// the platform, raised a block: engineering brick, the coping along its edge, a step at its ends
		for (int x = 0; x < l; x++)
			for (int z = 0; z < D; z++)
				md.set(x, 0, z, z == 0 ? m.band() : m.wall());
		for (int z = 0; z < D; z++) {
			md.set(-1, 0, z, Kit.stairs(m.plinthStairs(), Direction.EAST, false));
			md.set(l, 0, z, Kit.stairs(m.plinthStairs(), Direction.WEST, false));
		}
		// the track in front: walnut sleepers, gravel, rails
		BlockState rail = CityPlan.with(Blocks.RAIL.defaultBlockState(), "shape", "east_west");
		for (int x = -2; x <= l + 1 && track; x++) {
			md.set(x, -1, -2, Blocks.GRAVEL.defaultBlockState());
			md.set(x, -1, -1, x % 2 == 0 ? Masonry.walnutPlanks() : Blocks.GRAVEL.defaultBlockState());
			md.set(x, -1, -3, x % 2 == 0 ? Masonry.walnutPlanks() : Blocks.GRAVEL.defaultBlockState());
			md.set(x, 0, -2, rail);
		}
		// the posts every 6 along both edges, walnut tie beams across at the posts
		for (int x = 1; x < l; x += 6) {
			for (int y = 1; y <= EAVE - 1; y++) {
				md.set(x, y, 0, post());
				md.set(x, y, D - 1, post());
			}
			for (int z = 0; z < D; z++)
				md.set(x, EAVE, z, Kit.slab(Masonry.walnutSlab(), false));
		}
		// the roof: eaves 1 out over each edge, a 1:2 pitch, its copper ridge roll, the barges walnut
		int zE = -1, zF = D;
		int hMax = 0;
		for (int z = zE; z <= zF; z++)
			hMax = Math.max(hMax, Kit.pitchHalf(z, zE, zF, EAVE));
		for (int x = -1; x <= l; x++)
			for (int z = zE; z <= zF; z++) {
				int h = Kit.pitchHalf(z, zE, zF, EAVE);
				boolean barge = x == -1 || x == l;
				Kit.pitchSlab(md, x, z, h, barge ? Masonry.walnutSlab() : Masonry.slateSlab());
				if (h == hMax && !barge)
					md.set(x, h % 2 == 1 ? h / 2 + 1 : h / 2, z, h % 2 == 1 ? Masonry.ridge() : Masonry.ridge());
				// the beams' ends carry the roof (a slab under the eave at each post)
			}
		for (int x = 1; x < l; x += 6)
			for (int z = 0; z < D; z++)
				md.setIfEmpty(x, EAVE + 1, z, Kit.slab(Masonry.walnutSlab(), false));
		// the charging cradles against the back, where passengers waited
		for (int x = 4; x < l - 1; x += 6) {
			int ax = x;
			BlockState a = ModWastes.CHARGING_ALCOVE.get().defaultBlockState();
			md.special(Map.of(new BlockPos(ax, 1, D - 1), a, new BlockPos(ax, 2, D - 1), a, new BlockPos(ax, 3, D - 1), a),
				site -> site.alcove(ax, 1, D - 1, Direction.NORTH, false));
		}
		if (c.wear().atLeast(Wear.DAMAGED)) {
			// the outer end's roof holed: slates gone over three, a rafter left
			for (int x = l - 5; x <= l - 3; x++)
				for (int z = zE; z <= 1; z++)
					md.remove(x, Kit.pitchHalf(z, zE, zF, EAVE) / 2, z);
			md.set(l - 4, 1, 1, Kit.slab(Masonry.slateSlab(), false));
			md.mark(Model.Cause.STRIKE, l - 4, EAVE + 1, 0, 2);
			md.strikes++;
		}
		return md;
	}
}
