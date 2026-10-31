package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Type 7, the assembly gantry (Workshops, lot C; design brief, D73: void-alloy): a long portal frame over a row of five jigs,
 * each body more complete than the last. Legs of paired Void-Alloy Pillars at each end (front and back), braced by
 * Void-Alloy Wall cross-ties at third heights; the beam two horizontal pillars side by side with a Band course along its
 * front (lit); chamfered stair caps over each leg; a hoist trolley (a void-alloy box with a grille) on the beam, its chain
 * down to a lens held over clamp 5. The five Assembly Clamps (D72), stages 5 to 1 from the heart end, nothing added round
 * them. The assembly floor of Void-Alloy Plate laid over the makers' paving, its edge a slab step down, a Seamed Plate groove
 * from each jig's foot to the line that runs to the legs (lit).
 */
public final class Gantry {
	private Gantry() {}

	public static final int L = 25, D = 6, BEAM = 8;

	public static Model build(Ctx c) {
		Model md = new Model();
		int c0 = D / 2 - 1;
		// the floor: plates over x 2..L-3 at y 0 (the paving round it), a slab step at its edges, the seamed grooves
		for (int x = 1; x <= L - 2; x++)
			for (int z = -1; z <= D; z++) {
				boolean edge = x == 1 || x == L - 2 || z == -1 || z == D;
				md.set(x, 0, z, edge ? Kit.vaSlab(false) : z == D - 2 ? Kit.seamed() : Kit.plate());
			}
		// the jigs: stage 5 at the heart end (x 4) to stage 1 at the outer end (x 20), each with its groove to the line
		for (int i = 0; i < 5; i++) {
			int x = 4 + 4 * i, stage = 5 - i;
			// damaged: clamp 1, the nearest the buckled leg, knocked askew off its mark (a clamp can't lie down: progress doc)
			boolean knocked = stage == 1 && c.wear() == Wear.DAMAGED;
			int cx = knocked ? x - 1 : x, cz = knocked ? c0 + 2 : c0;
			Direction f = knocked ? Direction.EAST : Direction.NORTH;
			md.special(Map.of(new BlockPos(cx, 1, cz), ModWastes.ASSEMBLY_CLAMP.get().defaultBlockState()), site -> site.clamp(cx, 1, cz, f, stage));
			if (knocked)
				md.mark(Model.Cause.FALL, x, 1, c0, 1);
			for (int z = c0 + 1; z < D - 2; z++)
				md.set(x, 0, z, Kit.seamed());
		}
		// the legs: paired pillars (x 0-1 and x L-2..L-1) front and back, cross-ties at thirds, the cross beams and caps
		for (int lx : new int[] { 0, L - 2 })
			leg(md, lx);
		// the beam: two pillars along x side by side (z c0, c0 + 1), the band course along its front (z c0 - 1), lit
		for (int x = 0; x < L; x++) {
			md.set(x, BEAM, c0, Kit.pillar(Direction.Axis.X));
			md.set(x, BEAM, c0 + 1, Kit.pillar(Direction.Axis.X));
			if (x >= 2 && x <= L - 3)
				md.set(x, BEAM, c0 - 1, Kit.band(Direction.Axis.X));
		}
		// the hoist trolley over clamp 5, its chain down to the lens held over the body
		int tx = 4;
		md.set(tx, BEAM - 1, c0, Kit.plate());
		md.set(tx + 1, BEAM - 1, c0, Kit.grille(Direction.NORTH));
		for (int y = 4; y <= BEAM - 2; y++)
			md.set(tx, y, c0, Blocks.CHAIN.defaultBlockState());
		md.set(tx, 3, c0, Blocks.TINTED_GLASS.defaultBlockState());
		damage(md, c);
		return md;
	}

	/** A leg pair at x lx..lx+1: pillars front and back to the beam, wall cross-ties at thirds between them, the cap. */
	static void leg(Model md, int lx) {
		for (int x = lx; x <= lx + 1; x++) {
			for (int y = 0; y <= BEAM; y++) {
				md.set(x, y, 0, y == BEAM ? Kit.pillar(Direction.Axis.Z) : Kit.pillar());
				md.set(x, y, D - 1, y == BEAM ? Kit.pillar(Direction.Axis.Z) : Kit.pillar());
			}
			for (int z = 1; z < D - 1; z++) {
				md.set(x, BEAM, z, Kit.pillar(Direction.Axis.Z));
				for (int y : new int[] { BEAM / 3, 2 * BEAM / 3 })
					md.set(x, y, z, Kit.vaWall());
			}
		}
		Kit.chamfer(md, lx, 0, lx + 1, D - 1, BEAM + 1, null);
	}

	/**
	 * Worn: two bolt craters in the floor (sunk, sooted at the bottom; scarred plates show their scar only on a side, so the
	 * floor's rings are sunk slabs). Damaged: the outer leg buckled, the beam's outer end down to the ground on a stepped
	 * slope, the hoist slid to its bottom, clamp 1 (the nearest) knocked askew.
	 */
	static void damage(Model md, Ctx c) {
		if (!c.wear().atLeast(Wear.WORN))
			return;
		int c0 = D / 2 - 1;
		for (int[] cr : new int[][] { { 10, 1 }, { 18, D - 2 } }) {
			md.remove(cr[0], 0, cr[1]);
			md.set(cr[0], -1, cr[1], Masonry.sootPlain());
			for (int dx = -1; dx <= 1; dx++)
				for (int dz = -1; dz <= 1; dz++)
					if ((dx != 0 || dz != 0) && md.get(cr[0] + dx, 0, cr[1] + dz) != null && md.get(cr[0] + dx, 0, cr[1] + dz).is(ModWastes.VOID_ALLOY_PLATE.get()))
						md.set(cr[0] + dx, 0, cr[1] + dz, Kit.vaSlab(false));
			md.mark(Model.Cause.STRIKE, cr[0], 0, cr[1], 2);
			md.strikes++;
		}
		if (c.wear() != Wear.DAMAGED)
			return;
		// the outer leg buckled to a stub; the beam's outer end down on a stepped slope from x 14
		for (int x = L - 2; x <= L - 1; x++)
			for (int z = -1; z <= D; z++)
				for (int y = 3; y <= BEAM + 1; y++)
					md.remove(x, y, z);
		int xk = 14;
		for (int x = xk + 1; x < L; x++)
			for (int z = c0 - 1; z <= c0 + 1; z++) {
				BlockState s = md.get(x, BEAM, z);
				if (s == null)
					continue;
				md.remove(x, BEAM, z);
				int y = Math.max(1, BEAM - (x - xk));
				md.set(x, y, z, s);
			}
		md.mark(Model.Cause.BREAK, xk, BEAM, c0, 2);
		// the hoist slid to the bottom of the slope
		md.remove(4, BEAM - 1, c0);
		md.remove(5, BEAM - 1, c0);
		for (int y = 3; y <= BEAM - 2; y++)
			md.remove(4, y, c0);
		md.set(L - 3, 1, c0 + 2, Kit.plate());
		md.set(L - 4, 1, c0 + 2, Kit.grille(Direction.NORTH));
		md.mark(Model.Cause.FALL, L - 3, 1, c0, 2);
		md.settle();
	}
}
