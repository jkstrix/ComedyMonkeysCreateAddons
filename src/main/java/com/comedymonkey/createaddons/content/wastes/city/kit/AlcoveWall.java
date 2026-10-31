package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.ScarredPlateBlock;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Type 6, the alcove wall: the honeycomb (Quarters, lot C; design brief). A dark wall of stacked Charging Alcoves, each tier
 * alternating alcove and Void-Alloy Pillar and offset by one from the tier below, so niches sit over piers; a Band at each
 * tier's head (lit), a void-alloy slab walkway 3 deep in front of each upper tier on a corbel of upside-down stairs with a
 * Void-Alloy Wall parapet, the top chamfered. They used ramps: a 2-wide slab ramp up the face in switchbacks (half a block a
 * block, one flight a tier; the first on a solid plate body, the second on wall legs with its parapet on a slab kerb), the
 * barricade across the main ramp, and the Lift at the wall's end (its 2 x 2 shaft framed by four pillars, a call plate at
 * each tier). It stands on a makers' plinth course (the old back-garden wall line), clamped. Trinkets in one alcove in five
 * (the plan's draw, D72), dead vines on about one in four, more on the upper tiers and at the outer end.
 * <p>
 * Local x along the wall (0..{@code l - 1}, the lift beyond it), z: the back wall 2, the alcoves 1, the walkway 0..-2, the
 * ramps -3..-7.
 */
public final class AlcoveWall {
	private AlcoveWall() {}

	public static final int TIER = 4;

	/** Whether an alcove gets dead vines: about 1 in 4, more up high and at the outer end (D72's "about 1 in 4"). */
	static boolean vines(int x, int t, int l, long seed) {
		int chance = 14 + 8 * t + (x >= l / 2 ? 8 : 0);
		return Kit.hash(x, t, 77, seed) % 100 < chance;
	}

	public static Model build(Ctx c, int l, int tiers) {
		Model md = new Model();
		Masonry m = c.m();
		int topY = TIER * tiers;
		// the old plinth course under it (body), its ledge in front, clamped at intervals
		for (int x = -1; x <= l; x++)
			for (int z = 0; z <= 2; z++)
				md.set(x, 0, z, m.plinth());
		for (int x = -1; x <= l; x++)
			md.set(x, 0, 3, Kit.stairs(m.plinthStairs(), Direction.NORTH, false));
		// the back wall and the honeycomb
		for (int t = 0; t < tiers; t++) {
			int yb = 1 + TIER * t;
			for (int x = 0; x < l; x++) {
				for (int y = yb; y <= yb + 2; y++)
					md.set(x, y, 2, Kit.plate());
				boolean alcove = (x + t) % 2 == 0 && x < l - 1;
				if (alcove) {
					int ax = x, ay = yb;
					boolean v = vines(x, t, l, c.seed());
					BlockState a = ModWastes.CHARGING_ALCOVE.get().defaultBlockState();
					md.special(Map.of(new BlockPos(ax, ay, 1), a, new BlockPos(ax, ay + 1, 1), a, new BlockPos(ax, ay + 2, 1), a),
						site -> site.alcove(ax, ay, 1, Direction.NORTH, v));
				} else
					for (int y = yb; y <= yb + 2; y++)
						md.set(x, y, 1, x == l - 1 ? Kit.plate() : Kit.pillar());
				md.set(x, yb + 3, 1, Kit.band());
				md.set(x, yb + 3, 2, Kit.band());
			}
			// the frame's ends: pillars full height
			for (int y = yb; y <= yb + 3; y++)
				for (int z = 1; z <= 2; z++) {
					md.set(-1, y, z, Kit.pillar());
					md.set(l, y, z, Kit.pillar());
				}
		}
		// the clamps on the ledge, in front of the bottom tier's pillars, biting the plinth
		for (int x = 1; x < l - 1; x += 4) {
			int px = (x + 0) % 2 == 1 ? x : x + 1;
			md.set(px, 1, 0, Kit.clampSaddle(Direction.EAST));
			md.set(px, 0, 0, m.spalled());
			md.mark(Model.Cause.CLAMP, px, 0, 0, 1);
		}
		// the top: a chamfer sloping to the front, plates behind
		for (int x = -1; x <= l; x++) {
			md.set(x, topY + 1, 2, Kit.plate());
			md.set(x, topY + 1, 1, Kit.vaStairs(Direction.SOUTH, false));
		}
		// the walkways: each upper tier's deck, z -2..0 (top slabs level with its floor), corbels under its front, a parapet
		for (int t = 1; t < tiers; t++) {
			int y = TIER * t;
			for (int x = 0; x <= l; x++) {
				for (int z = -2; z <= 0; z++)
					md.set(x, y, z, Kit.vaSlab(true));
				md.set(x, y - 1, -2, Kit.vaStairs(Direction.SOUTH, true));
				md.set(x, y + 1, -2, Kit.vaWall());
			}
		}
		// ---- the switchback ramp: flight 1 (ground to the first deck) east on lanes z -4..-3 on a solid body; a landing; flight
		// 2 west on lanes z -6..-5 on wall legs, to the upper landing and the second deck; its parapet on a kerb at z -7
		int xs = l - 12, rise1 = 2 * TIER + 2;
		for (int i = 0; i < rise1; i++) {
			int halves = i + 1, full = halves / 2;
			for (int z = -4; z <= -3; z++) {
				for (int y = 0; y < full; y++)
					md.set(xs + i, y, z, Kit.plate());
				if (halves % 2 == 1)
					md.set(xs + i, full, z, Kit.vaSlab(false));
			}
		}
		int deck1 = TIER; // the first deck's slab row (its top at TIER + 1)
		for (int x = xs + rise1; x <= xs + rise1 + 1; x++)
			for (int z = -6; z <= -3; z++)
				for (int y = 0; y <= deck1; y++)
					md.set(x, y, z, Kit.plate());
		for (int x = xs + rise1; x <= xs + rise1 + 1 && x <= l; x++)
			md.remove(x, deck1 + 1, -2);
		if (tiers > 2) {
			int deck2 = 2 * TIER;
			for (int j = 0; j < 2 * TIER; j++) {
				int x = xs + rise1 - 1 - j;
				int h = 2 * (deck1 + 1) + j + 1; // the surface in half blocks
				int cell = h / 2;
				boolean top = h % 2 == 0;
				int sy = top ? cell - 1 : cell;
				for (int z = -6; z <= -5; z++)
					md.set(x, sy, z, Kit.vaSlab(top));
				md.set(x, sy, -7, Kit.vaSlab(true));
				md.set(x, sy + 1, -7, Kit.vaWall());
				if (j % 3 == 1)
					for (int y = 0; y < sy; y++) {
						md.set(x, y, -6, Kit.vaWall());
						md.set(x, y, -5, Kit.vaWall());
					}
			}
			// the upper landing, x xs-1..xs, z -6..-3 at the second deck's level, on legs, its parapet; the deck's parapet opened
			for (int x = xs + rise1 - 2 * TIER - 2; x <= xs + rise1 - 2 * TIER - 1; x++) {
				for (int z = -6; z <= -3; z++)
					md.set(x, deck2, z, Kit.vaSlab(true));
				for (int y = 0; y < deck2; y++) {
					md.set(x, y, -6, Kit.vaWall());
					md.set(x, y, -5, Kit.vaWall());
				}
				md.set(x, deck2 + 1, -7, Kit.vaWall());
				md.set(x, deck2, -7, Kit.vaSlab(true));
				md.remove(x, deck2 + 1, -2);
			}
			md.set(xs + rise1 - 2 * TIER - 3, deck2 + 1, -6, Kit.vaWall());
			md.set(xs + rise1 - 2 * TIER - 3, deck2 + 1, -5, Kit.vaWall());
			md.set(xs + rise1 - 2 * TIER - 3, deck2 + 1, -4, Kit.vaWall());
			md.set(xs + rise1 - 2 * TIER - 3, deck2 + 1, -3, Kit.vaWall());
			md.set(xs + rise1 - 2 * TIER - 3, deck2, -6, Kit.vaSlab(true));
			md.set(xs + rise1 - 2 * TIER - 3, deck2, -5, Kit.vaSlab(true));
			md.set(xs + rise1 - 2 * TIER - 3, deck2, -4, Kit.vaSlab(true));
			md.set(xs + rise1 - 2 * TIER - 3, deck2, -3, Kit.vaSlab(true));
		}
		// the barricade across the main ramp (the war: welded scrap, a scarred plate, a heap)
		md.set(xs + 3, 2, -4, ModWastes.SCRAP_HEAP.get().defaultBlockState());
		md.set(xs + 3, 2, -3, Kit.scarred(ScarredPlateBlock.Scar.CHIPPED, Direction.WEST));
		md.set(xs + 3, 3, -3, ModWastes.SCRAP_HEAP.get().defaultBlockState());
		md.mark(Model.Cause.STRIKE, xs + 3, 2, -3, 1);
		// ---- the Lift at the wall's end: the 2 x 2 shaft x l+1..l+2, z -1..0, its floor at tier 1's, four framing pillars,
		// a call plate at each tier against the wall's last column
		int lx = l + 1;
		for (int x = lx; x <= lx + 1; x++)
			for (int z = -1; z <= 0; z++)
				md.set(x, 0, z, Kit.plate());
		for (int[] p : new int[][] { { lx - 1, -2 }, { lx + 2, -2 }, { lx - 1, 1 }, { lx + 2, 1 } })
			for (int y = 0; y <= topY + 1; y++)
				md.set(p[0], y, p[1], Kit.pillar());
		for (int z = -1; z <= 0; z++)
			md.set(lx + 2, topY + 1, z, Kit.pillar(Direction.Axis.Z));
		md.set(lx - 1, topY + 1, -1, Kit.pillar(Direction.Axis.Z));
		md.set(lx - 1, topY + 1, 0, Kit.pillar(Direction.Axis.Z));
		md.set(lx, topY + 1, -2, Kit.pillar(Direction.Axis.X));
		md.set(lx + 1, topY + 1, -2, Kit.pillar(Direction.Axis.X));
		md.set(lx, topY + 1, 1, Kit.pillar(Direction.Axis.X));
		md.set(lx + 1, topY + 1, 1, Kit.pillar(Direction.Axis.X));
		int head = topY + 2;
		// the shaft's head: a ring of plates on the frame, the Lift's drive in it
		for (int x = lx - 1; x <= lx + 2; x++)
			for (int z = -2; z <= 1; z++)
				if (x == lx - 1 || x == lx + 2 || z == -2 || z == 1)
					md.set(x, head, z, Kit.plate());
		md.special(Map.of(new BlockPos(lx, head, -1), ModWastes.LIFT.get().defaultBlockState()), site -> site.lift(lx, -1, head, Direction.WEST));
		for (int t = 0; t < tiers; t++) {
			int fy = 1 + TIER * t;
			md.special(Map.of(new BlockPos(l - 1, fy, 0), ModWastes.LIFT_CALL_PLATE.get().defaultBlockState()),
				site -> site.callPlate(l - 1, fy, 0, Direction.NORTH));
		}
		// worn: a chip on each outward face of the frame's end pillar
		if (c.wear().atLeast(Wear.WORN) && c.outward(Direction.EAST)) {
			md.set(l, 2, 2, Kit.scarred(ScarredPlateBlock.Scar.CHIPPED, Direction.EAST));
			md.scarredFaces.add(Direction.EAST);
		}
		return md;
	}

	/**
	 * The split alcove tower (damaged; world brief): a separate 6-wide, 3-tier honeycomb tower split down the middle, a
	 * 2-wide gap from top to bottom on a stepped slant, the two halves' top corners broken away in a V, rubble (plates,
	 * slabs) in the gap. On a plinth course like the wall's; vines on about one alcove in four.
	 */
	public static Model split(Ctx c) {
		Model md = new Model();
		Masonry m = c.m();
		int w = 6, tiers = 3, top = TIER * tiers;
		for (int x = -1; x <= w; x++)
			for (int z = 0; z <= 2; z++)
				md.set(x, 0, z, m.plinth());
		for (int t = 0; t < tiers; t++) {
			int yb = 1 + TIER * t;
			int gap = t < 2 ? 2 : 3; // the gap's left column: stepping over a block two tiers up (the slant)
			for (int x = 0; x < w; x++) {
				if (x == gap || x == gap + 1)
					continue;
				// the back: plate, its tier heads banded and the halves' edges pillared (it shows from behind and through the gap)
				boolean edge = x == 0 || x == w - 1 || x == gap - 1 || x == gap + 2;
				for (int y = yb; y <= yb + 3; y++)
					md.set(x, y, 2, y == yb + 3 ? Kit.band() : edge ? Kit.pillar() : Kit.plate());
				boolean alcove = (x + t) % 2 == 0;
				if (alcove) {
					int ax = x, ay = yb;
					boolean v = vines(x, t, w, c.seed() + 1);
					BlockState a = ModWastes.CHARGING_ALCOVE.get().defaultBlockState();
					md.special(Map.of(new BlockPos(ax, ay, 1), a, new BlockPos(ax, ay + 1, 1), a, new BlockPos(ax, ay + 2, 1), a),
						site -> site.alcove(ax, ay, 1, Direction.NORTH, v));
				} else
					for (int y = yb; y <= yb + 2; y++)
						md.set(x, y, 1, Kit.pillar());
				md.set(x, yb + 3, 1, Kit.band());
			}
		}
		for (int x = 0; x < w; x++) {
			if (x >= 2 && x <= 4)
				continue;
			md.set(x, top + 1, 2, Kit.plate());
			md.set(x, top + 1, 1, Kit.vaStairs(Direction.SOUTH, false));
		}
		// the V: the halves' top corners by the gap broken away (stepped), cracked plates on the break
		for (int[] v : new int[][] { { 2, top + 1 }, { 2, top }, { 2, top - 1 }, { 1, top + 1 }, { 5, top + 1 }, { 5, top } })
			for (int z = 1; z <= 2; z++)
				md.remove(v[0], v[1], z);
		for (int[] b : new int[][] { { 2, top - 2 }, { 5, top - 1 }, { 1, top } }) {
			if (md.get(b[0], b[1], 2) != null)
				md.set(b[0], b[1], 2, Kit.scarred(ScarredPlateBlock.Scar.CRACKED, Direction.SOUTH));
			md.mark(Model.Cause.BREAK, b[0], b[1], 2, 1);
		}
		md.strikes++;
		// the rubble in the gap: plates, slabs, a fallen band (lying)
		md.set(2, 1, 1, Kit.plate());
		md.set(3, 1, 0, Kit.vaSlab(false));
		md.set(2, 1, 0, Kit.band(Direction.Axis.X));
		md.set(3, 1, 2, Kit.vaSlab(false));
		md.set(3, 0, -1, Kit.plate());
		md.set(2, 0, -1, Kit.vaSlab(false));
		md.set(4, 0, -2, Kit.vaSlab(false));
		md.mark(Model.Cause.FALL, 3, 1, 1, 2);
		md.settle();
		return md;
	}
}
