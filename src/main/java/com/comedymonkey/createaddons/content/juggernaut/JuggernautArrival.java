package com.comedymonkey.createaddons.content.juggernaut;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Where a summoned Juggernaut appears (D52): always under open sky, with a clear space 7 wide and 8 high and ground
 * under all of him. In front of the effigy if it has that (facing the way the T faces), otherwise at the nearest such
 * spot above it, up to the surface and up to 16 blocks out, where he kneels dormant.
 */
public final class JuggernautArrival {
	/** Half his footprint, his headroom, and how far out from the effigy a surface spot is looked for. */
	static final int HALF = 3, HEIGHT = 8, SEARCH = 16;

	private JuggernautArrival() {}

	/** Where he stands, and whether that's away on the surface (kneeling) rather than in the effigy's place. */
	public record Spot(Vec3 pos, boolean surface) {}

	/**
	 * {@code base} is the T's foot (the block his feet replace), {@code front} the way the T faces; {@code ignore} are the
	 * effigy and key blocks, which go when he comes.
	 */
	@Nullable
	public static Spot find(Level level, BlockPos base, Direction front, Set<BlockPos> ignore) {
		// in front of the T, his back to where it stood: his shoulders clear its key and the shaft behind it
		BlockPos here = base.relative(front, HALF);
		if (fits(level, here, ignore))
			return new Spot(Vec3.atBottomCenterOf(here), false);
		for (int r = 0; r <= SEARCH; r++)
			for (int dx = -r; dx <= r; dx++)
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r)
						continue;
					int x = base.getX() + dx, z = base.getZ() + dz;
					if (!level.hasChunkAt(new BlockPos(x, base.getY(), z)))
						continue; // never loads a chunk to look
					int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
					BlockPos at = new BlockPos(x, y, z);
					if (y >= base.getY() && fits(level, at, ignore))
						return new Spot(Vec3.atBottomCenterOf(at), true);
				}
		return null;
	}

	/** Open sky, a clear 7 x 8 x 7 space standing at {@code feet}, and ground under every corner within a block. */
	static boolean fits(Level level, BlockPos feet, Set<BlockPos> ignore) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dx = -HALF; dx <= HALF; dx++)
			for (int dz = -HALF; dz <= HALF; dz++) {
				for (int dy = 0; dy < HEIGHT; dy++) {
					p.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
					if (!ignore.contains(p) && !level.getBlockState(p).getCollisionShape(level, p).isEmpty())
						return false;
					if (!ignore.contains(p) && !level.getFluidState(p).isEmpty())
						return false;
				}
				if (!level.canSeeSky(p.set(feet.getX() + dx, feet.getY() + HEIGHT, feet.getZ() + dz)))
					return false;
			}
		for (int[] c : new int[][] { { -HALF, -HALF }, { HALF, -HALF }, { -HALF, HALF }, { HALF, HALF }, { 0, 0 } }) {
			boolean ground = false;
			for (int dy = 1; dy <= 2 && !ground; dy++) {
				p.set(feet.getX() + c[0], feet.getY() - dy, feet.getZ() + c[1]);
				ground = !ignore.contains(p) && !level.getBlockState(p).getCollisionShape(level, p).isEmpty();
			}
			if (!ground)
				return false;
		}
		return true;
	}
}
