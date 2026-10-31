package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.content.wastes.KeyMarkedStoneBlock;
import com.comedymonkey.createaddons.content.wastes.ShellBlock;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Type 13, the outskirts siege works: the clockwork resistance's field works (design brief). From the air a zig-zag: short
 * straight segments turning by a stepped 45-degree joint, a salient pointing at the city with a strongpoint at its tip. All
 * makers' materials, never lit, no void-alloy: stacked Andesite Casing cover 2 high facing the city, walnut posts every 3
 * driven in as shoring (standing 1 above the wall), rubble fill of cobbled deepslate and Scorched End Stone, a walnut slab
 * firing step behind it; the strongpoint 3 high with a loophole at y 2; the Key-Marked Stone (the winding key scratched in
 * from outside, D73) one a segment on the outer face; Resistance Shells behind cover facing in. The war: soot fans and bolt
 * craters on the city side (the uprising fired out at them), a segment breached where a strike landed (a gap, a rubble cone
 * falling outward), casing knocked loose and lying behind the line.
 * <p>
 * Local: the city lies toward local north (-z, the front); the line runs along x; outside (+z) is behind the cover.
 */
public final class Siege {
	private Siege() {}

	/** One salient's line cells (x, z) in order: a straight, a stepped diagonal out toward the city, the tip, back, a straight. */
	public static List<int[]> line(int straight) {
		List<int[]> out = new ArrayList<>();
		int x = 0;
		for (int i = 0; i < straight; i++)
			out.add(new int[] { x++, 0 });
		for (int s = 1; s <= 3; s++) {
			out.add(new int[] { x, -s + 1 });
			out.add(new int[] { x, -s });
			x++;
		}
		for (int i = 0; i < 3; i++)
			out.add(new int[] { x++, -3 });
		for (int s = 3; s >= 1; s--) {
			out.add(new int[] { x, -s });
			out.add(new int[] { x, -s + 1 });
			x++;
		}
		for (int i = 0; i < straight; i++)
			out.add(new int[] { x++, 0 });
		return out;
	}

	public static Model build(Ctx c, int straight) {
		Model md = new Model();
		BlockState casing = CityPlan.block("create:andesite_casing", Blocks.POLISHED_ANDESITE);
		BlockState planks = Masonry.walnutPlanks();
		BlockState rubble = Masonry.rubble();
		BlockState scorched = ModWastes.SCORCHED_END_STONE.get().defaultBlockState();
		List<int[]> cells = line(straight);
		int tip0 = straight + 3, tip1 = tip0 + 2; // the tip's x
		int breach = cells.size() - straight / 2 - 1; // a breach in the outer straight
		int k = 0;
		for (int[] p : cells) {
			int x = p[0], z = p[1];
			boolean tip = z == -3 && x >= tip0 && x <= tip1;
			boolean breached = Math.abs(k - breach) <= 1 && z == 0;
			k++;
			if (breached)
				continue;
			int h = tip ? 3 : 2;
			for (int y = 0; y < h; y++) {
				BlockState s = (x + y) % 4 == 1 ? planks : (x * 3 + y) % 5 == 2 ? (x % 2 == 0 ? rubble : scorched) : casing;
				if (tip && y == 2 && x == tip0 + 1)
					continue; // the loophole
				md.set(x, y, z, s);
			}
			// the shoring posts every 3, behind the cover, standing one above it; the firing step between them
			if (x % 3 == 0 && !tip) {
				for (int y = 0; y <= h; y++)
					md.set(x, y, z + 1, Masonry.walnutFence());
			} else
				md.setIfEmpty(x, 0, z + 1, Kit.slab(Masonry.walnutSlab(), false));
		}
		// the strongpoint: the tip thickened behind, 3 high
		for (int x = tip0; x <= tip1; x++)
			for (int y = 0; y <= 2; y++)
				md.set(x, y, -2, y == 2 && x == tip0 + 1 ? Blocks.AIR.defaultBlockState() : casing);
		// the Key-Marked Stone on the outer face (looking away from the city), one a segment: in each straight and the tip
		for (int x : new int[] { 2, tip0 + 1, cells.get(cells.size() - 3)[0] }) {
			int z = x == tip0 + 1 ? -2 : 0;
			md.set(x, 1, z, ModWastes.KEY_MARKED_STONE.get().defaultBlockState().setValue(KeyMarkedStoneBlock.FACING, Direction.SOUTH));
		}
		// the resistance's dead behind cover, facing in (toward the city)
		md.set(4, 0, 2, shell(ShellBlock.ResistancePose.SLUMPED, Direction.NORTH));
		md.set(tip0 + 1, 0, 0, shell(ShellBlock.ResistancePose.SLUMPED, Direction.NORTH));
		md.set(cells.get(cells.size() - 2)[0], 0, 3, shell(ShellBlock.ResistancePose.FALLEN, Direction.NORTH));
		// the war: soot fans up the city face where bolts struck (the cover's own blocks sooted, a block either side and above)
		for (int[] st : new int[][] { { 5, 0 }, { tip0 + 2, -3 } }) {
			md.mark(Model.Cause.STRIKE, st[0], 0, st[1] - 1, 2);
			md.strikes++;
			for (int dx = -1; dx <= 1; dx++)
				for (int y = 0; y <= 2; y++) {
					BlockState s = md.get(st[0] + dx, y, st[1]);
					if (s != null && (dx == 0 || y >= 1) && !s.is(ModWastes.KEY_MARKED_STONE.get()))
						md.set(st[0] + dx, y, st[1], s.getBlock() == planks.getBlock() ? Masonry.charred().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X) : Masonry.sootPlain());
				}
			// the bolt crater in front of it on the city side
			md.set(st[0], -1, st[1] - 2, Masonry.sootPlain());
			md.set(st[0] - 1, -1, st[1] - 2, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
			md.set(st[0] + 1, -1, st[1] - 2, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
			md.set(st[0], -1, st[1] - 3, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		}
		// the breach: its scorch on the city side and its rubble cone falling outward (behind the line)
		int bx = cells.get(breach)[0];
		md.mark(Model.Cause.STRIKE, bx, 0, -1, 3);
		md.strikes++;
		for (int dx = -2; dx <= 2; dx += 4)
			for (int y = 0; y <= 1; y++)
				if (md.has(bx + dx, y, 0))
					md.set(bx + dx, y, 0, Masonry.sootPlain());
		md.set(bx, -1, -2, Masonry.sootPlain());
		md.set(bx - 1, 0, 2, casing);
		md.set(bx, 0, 2, casing);
		md.set(bx + 1, 0, 2, Kit.slab(Masonry.walnutSlab(), false));
		md.set(bx, 1, 2, Kit.slab(Masonry.walnutSlab(), false));
		md.set(bx, 0, 3, rubble);
		md.set(bx + 1, 0, 3, Kit.slab(Masonry.walnutSlab(), false));
		// casing knocked loose, lying behind the line
		md.set(tip0 - 2, 0, 1, casing);
		md.set(8, 0, 3, casing);
		return md;
	}

	static BlockState shell(ShellBlock.ResistancePose pose, Direction facing) {
		return ModWastes.RESISTANCE_SHELL.get().defaultBlockState().setValue(ShellBlock.Resistance.POSE, pose).setValue(HorizontalDirectionalBlock.FACING, facing);
	}
}
