package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.LinkedHashMap;
import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.ScarredPlateBlock;
import com.comedymonkey.createaddons.content.wastes.ShellBlock;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Type 11, the vault house (design brief): a squat, deep strongroom, chamfered all round at the top, its door in a deep
 * projecting portal: "a safe that grew a doorway". 9 x 9 (x, z 0..8) of void-alloy on a makers' plinth 11 x 11 whose ledge
 * shows all round, clamped by saddle brackets at the corners and the middle of the sides (the stood-on join). Pillar
 * corners, a Band at y 4 (lit), a grille vent high on each side wall, the portal (the Sliding Door set 2 deep in a frame
 * projecting 1: pillar jambs, a band head, a stair chamfer), a stair chamfer round the roof's edge with a slab field
 * inside; the wall lamp over the portal and cable on the front at head height. Inside: the City Vault in the back wall,
 * alcoves on the side walls, a slumped Worker.
 */
public final class VaultHouse {
	private VaultHouse() {}

	public static final int W = 9;
	/** The portal's door columns (x 3-4 of 9: a 2-wide door can't centre on an odd front; provisional, see the progress doc). */
	public static final int DOOR_X = 3;

	public static Model build(Ctx c) {
		Model md = new Model();
		Masonry m = c.m();
		// the old plinth, 11 x 11, its ledge 1 wide all round
		for (int x = -1; x <= W; x++)
			for (int z = -1; z <= W; z++)
				md.set(x, 0, z, m.plinth());
		// the walls y 1-5: pillar corners, plate, the band at y 4; the roof at y 5 (plates), the chamfer ring and slab field at y 6
		for (int y = 1; y <= 5; y++)
			for (int x = 0; x < W; x++)
				for (int z = 0; z < W; z++) {
					boolean ex = x == 0 || x == W - 1, ez = z == 0 || z == W - 1;
					if (y == 5) {
						md.set(x, y, z, Kit.plate());
						continue;
					}
					if (!ex && !ez)
						continue;
					md.set(x, y, z, y == 4 ? Kit.band() : ex && ez ? Kit.pillar() : Kit.plate());
				}
		Kit.chamfer(md, 0, 0, W - 1, W - 1, 6, Kit.vaSlab(false));
		// the grille vents high on the side walls (1 x 2 under the band), and one on the back
		for (int y = 2; y <= 3; y++) {
			md.set(0, y, W / 2, Kit.grille(Direction.WEST));
			md.set(W - 1, y, W / 2, Kit.grille(Direction.EAST));
			md.set(W / 2, y, W - 1, Kit.grille(Direction.SOUTH));
		}
		// the portal: the door 2 deep (in z 1), the front cut open in front of it, a frame projecting 1 (z -1): pillar jambs,
		// a band head at y 4 (the house's band runs into it), a stair chamfer over it at y 5
		for (int y = 1; y <= 3; y++)
			for (int x = DOOR_X; x <= DOOR_X + 1; x++)
				md.remove(x, y, 0);
		for (int y = 1; y <= 3; y++) {
			for (int x : new int[] { DOOR_X - 1, DOOR_X + 2 }) {
				md.set(x, y, -1, Kit.pillar());
				md.set(x, y, 1, Kit.plate());
			}
		}
		for (int x = DOOR_X - 1; x <= DOOR_X + 2; x++) {
			md.set(x, 4, -1, Kit.band());
			md.set(x, 5, -1, Kit.vaStairs(Direction.SOUTH, false));
		}
		md.set(DOOR_X - 1, 5, -1, Kit.vaStairs(Direction.EAST, false));
		md.set(DOOR_X + 2, 5, -1, Kit.vaStairs(Direction.SOUTH, false));
		Map<BlockPos, BlockState> door = new LinkedHashMap<>();
		for (int y = 1; y <= 3; y++)
			for (int x = DOOR_X; x <= DOOR_X + 1; x++)
				door.put(new BlockPos(x, y, 1), ModWastes.SLIDING_DOOR.get().defaultBlockState());
		md.special(door, site -> site.door(DOOR_X, 1, 1, Direction.NORTH, false));
		// the saddle clamps on the ledge at the corners and the middle of the sides (not the front's: the portal stands there)
		for (int[] cl : new int[][] { { -1, -1 }, { W, -1 }, { W, W }, { -1, W }, { -1, W / 2 }, { W, W / 2 }, { W / 2, W } }) {
			Direction along = cl[0] == -1 || cl[0] == W ? Direction.EAST : Direction.SOUTH;
			md.set(cl[0], 1, cl[1], Kit.clampSaddle(along));
			md.set(cl[0], 0, cl[1], m.spalled());
			md.mark(Model.Cause.CLAMP, cl[0], 0, cl[1], 1);
		}
		// the wall lamp over the portal, cable on the front at head height
		md.special(Map.of(new BlockPos(DOOR_X + 3, 4, -1), ModWastes.DERELICT_WALL_LAMP.get().defaultBlockState()),
			site -> site.wallLamp(DOOR_X + 3, 4, -1, Direction.NORTH));
		Map<BlockPos, BlockState> cable = new LinkedHashMap<>();
		for (int x = DOOR_X + 3; x < W; x++)
			cable.put(new BlockPos(x, 3, -1), ModWastes.DERELICT_CABLE.get().defaultBlockState());
		md.special(cable, site -> {
			for (BlockPos p : cable.keySet())
				site.cable(p.getX(), p.getY(), p.getZ(), Direction.SOUTH);
		});
		// inside: the vault in the back wall, alcoves on the side walls, a slumped Worker
		md.special(Map.of(new BlockPos(W / 2, 1, W - 2), ModWastes.CITY_VAULT.get().defaultBlockState()), site -> site.vault(W / 2, 1, W - 2, Direction.NORTH));
		for (int z : new int[] { 3, 6 }) {
			alcove(md, 1, z, Direction.EAST);
			alcove(md, W - 2, z, Direction.WEST);
		}
		md.set(3, 1, W - 3, ModWastes.WORKER_SHELL.get().defaultBlockState().setValue(ShellBlock.Worker.POSE, ShellBlock.WorkerPose.SLUMPED)
			.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		damage(md, c);
		return md;
	}

	static void alcove(Model md, int x, int z, Direction open) {
		BlockState a = ModWastes.CHARGING_ALCOVE.get().defaultBlockState();
		md.special(Map.of(new BlockPos(x, 1, z), a, new BlockPos(x, 2, z), a, new BlockPos(x, 3, z), a), site -> site.alcove(x, 1, z, open, false));
	}

	/**
	 * Worn: one chip on each outward face, heat tint above the outward vent. Damaged: a strike on the outward side wall
	 * (cracked plates round where it hit, heat tint above), the portal untouched (it held).
	 */
	static void damage(Model md, Ctx c) {
		if (!c.wear().atLeast(Wear.WORN))
			return;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (!c.outward(d))
				continue;
			int x = d == Direction.EAST ? W - 1 : d == Direction.WEST ? 0 : 6, z = d == Direction.SOUTH ? W - 1 : d == Direction.NORTH ? 0 : 2;
			md.set(x, 2, z, Kit.scarred(ScarredPlateBlock.Scar.CHIPPED, d));
			md.scarredFaces.add(d);
			if (d.getAxis() == Direction.Axis.X) {
				// heat tint above the vent on this side
				md.set(x, 5, W / 2, Kit.scarred(ScarredPlateBlock.Scar.HEAT, d));
				if (c.wear() == Wear.DAMAGED) {
					// the strike: cracked plates round the hit, its heat above
					md.strikes++;
					md.mark(Model.Cause.STRIKE, x, 2, 6, 2);
					md.set(x, 1, 6, Kit.scarred(ScarredPlateBlock.Scar.CRACKED, d));
					md.set(x, 2, 6, Kit.scarred(ScarredPlateBlock.Scar.CRACKED, d));
					md.set(x, 2, 7, Kit.scarred(ScarredPlateBlock.Scar.CRACKED, d));
					md.set(x, 3, 6, Kit.scarred(ScarredPlateBlock.Scar.HEAT, d));
				}
			}
		}
	}
}
