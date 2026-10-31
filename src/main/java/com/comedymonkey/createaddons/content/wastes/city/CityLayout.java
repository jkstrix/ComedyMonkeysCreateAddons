package com.comedymonkey.createaddons.content.wastes.city;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock.Mount;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.LiftBlock;
import com.comedymonkey.createaddons.content.wastes.ShellBlock;
import com.comedymonkey.createaddons.content.wastes.ShellBlock.ResistancePose;
import com.comedymonkey.createaddons.content.wastes.ShellBlock.WorkerPose;
import com.comedymonkey.createaddons.content.wastes.city.kit.KitCity;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Clockwork Wastes city's layout (design/briefs/clockwork-wastes-world.md, Primary Forms; spec P4, P32, D21, D61, D64,
 * D67), drawn into a {@link CityPlan} relative to its origin: the middle of the heart, at its floor.
 * <ul>
 * <li>The heart ({@link #HEART}: |x|, |z| up to 22): the makers' End Stone brick drum (an octagon round the 31 x 31 hall,
 * 14 high, its copper dome ribs left bare and broken), the hall's floor with conduit grooves from each pylon to the
 * preview's 3 x 3, one-block dais (D67), the four conduit pylons at (+-11, +-11), the eight wall vents two by each pylon
 * (the heart-hall preview's places), the Derelict Star Reactor under the spire's shaft, the heart vault in the south wall
 * flanked by two maker statues facing the wall, three heart doors (north, east, west), the Custodian kneeling, facing the
 * north door. The spire (5 x 5, then 3 x 3, its corners seamed) stands on four legs round the reactor and rises about 60
 * blocks to a crown of four arms reaching 8 blocks, each holding a hanging insulator.</li>
 * <li>Four boulevards (|x| or |z| up to 3) from the heart to the city's edge, and four districts, one a quadrant
 * ({@link #districtAt}): Foundry north-west, Workshops north-east, Rail Yard south-east, Quarters south-west. Each in its
 * own frame ({@link Q}: u out along x, v out along z, from 4 blocks off the axes): two streets (v 30-34 and u 30-34) with
 * lamps every 7 blocks, alternating sides, on cable; the Substation yard at the corner nearest the heart (D61: its
 * lattice mast of Create Metal Girders and the sky-cable tower beside it); the vault house; makers' sheds; the district's
 * set piece; its Sentries and Scuttlers, dormant.</li>
 * <li>The outskirts (to {@link #OUT}): stubs, fallen lamp posts, scrap heaps, barricades at the street ends, the
 * resistance's siege lines and their dead, the two war-frames locked together, bridges snapping off into the void.</li>
 * </ul>
 * Every choice the briefs don't pin down is listed in the progress doc (design/specs/clockwork-wastes.progress.md, "The
 * city").
 */
public final class CityLayout {
	/** The heart: |x| and |z| up to this. */
	public static final int HEART = 22;
	/** The city's core (districts, streets): |x| and |z| up to this; the outskirts beyond. */
	public static final int CORE = 70;
	/** The outskirts reach this far. */
	public static final int OUT = 92;
	/** Where the spire stands (over the reactor): x 0, z this. */
	public static final int SPIRE_Z = -8;
	/** The spire's crown, this high above the floor. */
	public static final int CROWN_Y = 58;
	/**
	 * The city structure set's spacing in chunks (worldgen/structure_set/clockwork_city.json; separation 12): 32 (P45, D69).
	 * Elytra parity with the End-city ships the Wastes displace is made up by the outskirts-only sites' own Elytra
	 * ({@link #resistanceCache}, D69; WastesWorldGameTests.elytraParity).
	 */
	public static final int SPACING = 32;
	/**
	 * Where an outskirts-only site's Elytra cache stands (D69): the site's middle, the one column world generation has
	 * made sure has ground (the heart's, {@link ClockworkCityStructure#site}), so it always places. Provisional (the
	 * user's agreement): the progress doc's "D69" choices.
	 */
	public static final BlockPos RESISTANCE_CACHE = BlockPos.ZERO;
	/** Elytra in each outskirts-only site's cache (D69): one, as a city's heart vault and an End ship hold one. */
	public static final int OUTSKIRTS_ELYTRA = 1;
	/** The cache's loot table (D69): {@link #OUTSKIRTS_ELYTRA} Elytra and nothing else. */
	public static final ResourceKey<LootTable> RESISTANCE_CACHE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
		ComedyMonkeysCreateAddons.asResource("chests/clockwork_resistance_cache"));
	/** The drum's roof line, where the spire's shaft rises out of it. */
	public static final int SPIRE_ROOF = 14;
	/** The heart vault, in the south wall. */
	public static final BlockPos VAULT = new BlockPos(0, 0, 16);
	/** The pylons' bottom blocks. */
	public static final int PYLON = 11;

	private CityLayout() {}

	/**
	 * The city's buildings from the building kit (D71: "approved buildings then replace the boxes in the city"), or the old
	 * one-material boxes. OFF until the user approves the catalogue ({@code /wastes buildcatalogue}); the swap is this one
	 * word ({@code kit.KitCity} draws every building the boxes stood for, in the same lots).
	 */
	public static final boolean BUILDING_KIT = false;

	/** The district a block at (dx, dz) from the origin belongs to: the heart in the middle, else its quadrant. */
	public static District districtAt(int dx, int dz) {
		if (Math.abs(dx) <= HEART && Math.abs(dz) <= HEART)
			return District.HEART;
		if (dz < 0)
			return dx < 0 ? District.FOUNDRY : District.WORKSHOPS;
		return dx < 0 ? District.QUARTERS : District.RAIL_YARD;
	}

	/** Each district's quadrant (x sign, z sign). */
	static int[] quadrant(District d) {
		return switch (d) {
			case FOUNDRY -> new int[] { -1, -1 };
			case WORKSHOPS -> new int[] { 1, -1 };
			case RAIL_YARD -> new int[] { 1, 1 };
			case QUARTERS -> new int[] { -1, 1 };
			case HEART -> new int[] { 0, 0 };
		};
	}

	// ---------------------------------------------------------------- materials

	static BlockState va() {
		return ModWastes.VOID_ALLOY_PLATE.get().defaultBlockState();
	}

	static BlockState seam() {
		return ModWastes.SEAMED_VOID_ALLOY_PLATE.get().defaultBlockState();
	}

	static BlockState casing() {
		return CityPlan.block("create:andesite_casing", Blocks.POLISHED_ANDESITE);
	}

	static BlockState brass() {
		return CityPlan.block("create:brass_block", Blocks.CUT_COPPER);
	}

	static BlockState brassCasing() {
		return CityPlan.block("create:brass_casing", Blocks.CUT_COPPER);
	}

	static BlockState railCasing() {
		return CityPlan.block("create:railway_casing", Blocks.POLISHED_DEEPSLATE);
	}

	static BlockState girder() {
		return CityPlan.with(CityPlan.block("create:metal_girder", Blocks.IRON_BARS), "axis", "y");
	}

	static BlockState girderAlong(Direction.Axis axis) {
		return CityPlan.with(CityPlan.block("create:metal_girder", Blocks.IRON_BARS), "axis", axis.getName());
	}

	static BlockState slab(Block b) {
		return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
	}

	static BlockState topSlab(Block b) {
		return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
	}

	static BlockState scrapHeap(RandomSource r) {
		return ModWastes.SCRAP_HEAP.get().defaultBlockState();
	}

	static BlockState worker(WorkerPose pose, Direction facing) {
		return ModWastes.WORKER_SHELL.get().defaultBlockState().setValue(ShellBlock.Worker.POSE, pose).setValue(HorizontalDirectionalBlock.FACING, facing);
	}

	static BlockState resistance(ResistancePose pose, Direction facing) {
		return ModWastes.RESISTANCE_SHELL.get().defaultBlockState().setValue(ShellBlock.Resistance.POSE, pose).setValue(HorizontalDirectionalBlock.FACING, facing);
	}

	static BlockState machine(Block b, Direction facing) {
		return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
	}

	/** The city's ground, by a hash: mostly plain scorched, some rust-stained, some littered. */
	static BlockState scorched(int x, int z) {
		int h = hash(x, z, 7) % 10;
		return (h < 2 ? ModWastes.RUSTED_SCORCHED_END_STONE : h < 3 ? ModWastes.LITTERED_SCORCHED_END_STONE : ModWastes.SCORCHED_END_STONE).get().defaultBlockState();
	}

	/** The makers' worn paving. */
	static BlockState paving(int x, int z) {
		int h = hash(x, z, 11) % 20;
		return (h < 13 ? Blocks.POLISHED_ANDESITE : h < 16 ? Blocks.STONE_BRICKS : h < 19 ? Blocks.CRACKED_STONE_BRICKS : Blocks.ANDESITE).defaultBlockState();
	}

	static int hash(int x, int z, int salt) {
		int h = x * 73856093 ^ z * 19349663 ^ salt * 83492791;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return h & 0x7fffffff;
	}

	// ---------------------------------------------------------------- the whole city

	static void draw(CityPlan p) {
		ground(p);
		if (!p.outskirtsOnly) {
			p.district = District.HEART;
			heart(p);
			boulevards(p);
			for (District d : District.DISTRICTS) {
				int[] q = quadrant(d);
				p.district = d;
				district(new Q(p, d, q[0], q[1]));
			}
		}
		p.district = null;
		outskirts(p);
	}

	/** The ground: the core levelled to the heart's floor (decked and strutted over the void); the outskirts levelled where land is near. */
	private static void ground(CityPlan p) {
		for (int x = -OUT; x <= OUT; x++)
			for (int z = -OUT; z <= OUT; z++) {
				int ax = Math.abs(x), az = Math.abs(z);
				boolean core = ax <= CORE && az <= CORE;
				if (p.outskirtsOnly) {
					if (ax * ax + az * az <= OUT * OUT)
						p.ground(x, z, false, false, false);
					continue;
				}
				if (core) {
					p.ground(x, z, true, Math.floorMod(x, 8) == 0 && Math.floorMod(z, 8) == 0, true);
					continue;
				}
				// the boulevards run on as bridges over the void; two snap off (P3, the world brief)
				boolean bridge = ax <= 2 || az <= 2;
				int far = Math.max(ax, az);
				boolean snaps = far > 82 && (ax <= 2 && z < 0 || az <= 2 && x > 0);
				p.ground(x, z, false, bridge && !snaps && far % 8 == 0 && (ax <= 2 && ax % 4 == 2 || az <= 2 && az % 4 == 2), bridge && (!snaps));
				// a bridge's rails where it crosses the void; a snapped one hanging down off its end on its cables
				if (bridge && !snaps && (ax == 2 && az > 2 || az == 2 && ax > 2))
					p.put(p.at(x, 0, z), Blocks.IRON_BARS.defaultBlockState(), CityPlan.OVER_VOID);
				if (bridge && snaps && far <= 85)
					p.put(p.at(x, -1 - (far - 82) * 2, z), (far == 85 ? Blocks.CHAIN : ModWastes.VOID_ALLOY_PLATE.get()).defaultBlockState(), CityPlan.OVER_VOID);
			}
	}

	// ================================================================ the heart

	private static boolean inHall(int x, int z) {
		return Math.abs(x) <= 15 && Math.abs(z) <= 15 && Math.abs(x) + Math.abs(z) <= 27;
	}

	private static boolean inDrum(int x, int z) {
		return Math.abs(x) <= 17 && Math.abs(z) <= 17 && Math.abs(x) + Math.abs(z) <= 30;
	}

	private static boolean inSpire(int x, int z, int half) {
		return Math.abs(x) <= half && Math.abs(z - SPIRE_Z) <= half;
	}

	private static void heart(CityPlan p) {
		boolean kit = p.kit;
		// the kit draws the drum in Stained End Stone Brick (D73: vanilla End Stone Bricks are excluded); its plaza bands too
		BlockState brick = kit ? ModWastes.STAINED_END_STONE_BRICKS.get().defaultBlockState() : Blocks.END_STONE_BRICKS.defaultBlockState();
		// the plaza round the drum: the makers' paving, End Stone brick bands
		for (int x = -HEART; x <= HEART; x++)
			for (int z = -HEART; z <= HEART; z++)
				if (!inDrum(x, z))
					p.put(x, -1, z, (Math.abs(x) == 20 || Math.abs(z) == 20) ? brick : paving(x, z));
		// the hall's floor: void-alloy plate; the drum's wall, 14 high, End Stone brick (the makers' layer)
		for (int x = -17; x <= 17; x++)
			for (int z = -17; z <= 17; z++) {
				if (inHall(x, z))
					p.put(x, -1, z, va());
				else if (inDrum(x, z)) {
					p.put(x, -1, z, brick);
					for (int y = 0; y < 14 && !kit; y++)
						p.put(x, y, z, y == 13 && hash(x, z, 3) % 4 == 0 ? Blocks.AIR.defaultBlockState() : brick);
				}
			}
		// the kit's drum and spire (D71): before the hall's pieces, which go into the places it leaves for them
		if (kit)
			KitCity.heart(p, SPIRE_Z);
		// the conduit grooves: seamed plate in the floor from each pylon to the dais (lit, starlight runs down them)
		for (int sx = -1; sx <= 1; sx += 2)
			for (int sz = -1; sz <= 1; sz += 2)
				for (int k = 2; k <= 10; k++) {
					p.put(sx * k, -1, sz * k, seam());
					p.put(sx * k, -1, sz * (k - 1), seam());
				}
		// the dais: the preview's 3 x 3, one block high, its middle seamed (D67)
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				p.put(x, 0, z, x == 0 && z == 0 ? seam() : va());
		// the four conduit pylons
		List<BlockPos> pylons = new ArrayList<>();
		for (int[] c : new int[][] { { -PYLON, -PYLON }, { PYLON, -PYLON }, { -PYLON, PYLON }, { PYLON, PYLON } }) {
			p.pylon(p.at(c[0], 0, c[1]));
			pylons.add(p.at(c[0], 0, c[1]));
		}
		// eight wall vents, two by each pylon, in the walls either side of its corner, facing in (the heart-hall preview)
		for (int sx = -1; sx <= 1; sx += 2)
			for (int sz = -1; sz <= 1; sz += 2) {
				p.vent(p.at(sx * 16, 0, sz * (PYLON - 2)), sx > 0 ? Direction.WEST : Direction.EAST);
				p.vent(p.at(sx * (PYLON - 2), 0, sz * 16), sz > 0 ? Direction.NORTH : Direction.SOUTH);
			}
		// the heart doors: north (facing the Custodian), east and west; the outer course of the wall cleared behind each
		if (!kit) {
			p.doorAcross(p.at(-1, 0, -16), p.at(0, 0, -16), Direction.NORTH, true);
			p.doorAcross(p.at(16, 0, -1), p.at(16, 0, 0), Direction.EAST, true);
			p.doorAcross(p.at(-16, 0, 0), p.at(-16, 0, -1), Direction.WEST, true);
		}
		for (int y = 0; y < 3 && !kit; y++)
			for (int i = -1; i <= 0; i++) {
				p.put(i, y, -17, Blocks.AIR.defaultBlockState());
				p.put(17, y, i, Blocks.AIR.defaultBlockState());
				p.put(-17, y, i, Blocks.AIR.defaultBlockState());
			}
		// the heart vault in the south wall, in a tall chamfered void-alloy surround, two statues facing the wall beside it
		for (int y = 0; y < 5; y++) {
			p.pillar(p.at(-1, y, 16), Direction.Axis.Y);
			p.pillar(p.at(1, y, 16), Direction.Axis.Y);
		}
		p.put(0, 1, 16, va());
		p.put(0, 2, 16, seam());
		p.put(0, 3, 16, va());
		p.put(0, 4, 16, va());
		p.pillar(p.at(0, 5, 16), Direction.Axis.X);
		p.vault(p.at(VAULT.getX(), VAULT.getY(), VAULT.getZ()), Direction.NORTH, true);
		p.statue(p.at(-3, 0, 15), Direction.SOUTH);
		p.statue(p.at(3, 0, 15), Direction.SOUTH);
		// the spire's legs round the reactor, the reactor under its shaft, facing the dais
		for (int[] c : new int[][] { { -2, SPIRE_Z - 2 }, { 2, SPIRE_Z - 2 }, { -2, SPIRE_Z + 2 }, { 2, SPIRE_Z + 2 } })
			for (int y = 0; y < 14; y++)
				p.pillar(p.at(c[0], y, c[1]), Direction.Axis.Y);
		p.reactor(p.at(0, 0, SPIRE_Z));
		if (!kit) {
			spire(p);
			dome(p);
		}
		// heart lamps: on the hall's walls (they surge at the overload, D54) and round the plaza
		for (int[] w : new int[][] { { -15, 4, -4, 1 }, { -15, 4, 4, 1 }, { 15, 4, -4, -1 }, { 15, 4, 4, -1 } })
			p.wallLamp(p.at(w[0], w[1], w[2]), w[3] > 0 ? Direction.EAST : Direction.WEST);
		for (int[] w : new int[][] { { -5, 4, -15 }, { 5, 4, -15 }, { -6, 4, 15 }, { 6, 4, 15 } })
			p.wallLamp(p.at(w[0], w[1], w[2]), w[2] < 0 ? Direction.SOUTH : Direction.NORTH);
		for (int[] c : new int[][] { { -19, -19 }, { 19, -19 }, { -19, 19 }, { 19, 19 }, { -19, 8 }, { 19, 8 }, { -8, 19 }, { 8, 19 } })
			p.streetLamp(p.at(c[0], 0, c[1]), Math.abs(c[0]) == 19 && Math.abs(c[1]) != 19 ? (c[0] < 0 ? Direction.WEST : Direction.EAST)
				: c[1] < 0 ? Direction.NORTH : Direction.SOUTH, false, false);
		// the frozen shift (world brief): shells stopped mid-step, a Mechanical Arm frozen over a depot
		p.put(-7, 0, 9, worker(WorkerPose.FROZEN, Direction.EAST));
		p.put(8, 0, 10, worker(WorkerPose.FROZEN, Direction.WEST));
		p.put(-12, 0, -3, machine(ModWastes.BROKEN_MECHANICAL_ARM.get(), Direction.EAST));
		p.put(-11, 0, -3, CityPlan.block("create:depot", Blocks.SMOOTH_STONE_SLAB));
		// the Custodian kneeling on its dais, facing the north door; its pylons and the vault behind it
		p.custodian(p.at(0, 1, 0), 180, pylons, p.at(VAULT.getX(), VAULT.getY(), VAULT.getZ()));
	}

	/** The spire: 5 x 5 from the roof to 40, then 3 x 3 to the crown; corners seamed; the crown's four arms and their insulators. */
	private static void spire(CityPlan p) {
		for (int y = 14; y < CROWN_Y; y++) {
			int half = y < 40 ? 2 : 1;
			for (int x = -half; x <= half; x++)
				for (int dz = -half; dz <= half; dz++) {
					boolean edge = Math.abs(x) == half || Math.abs(dz) == half;
					if (!edge)
						continue;
					boolean corner = Math.abs(x) == half && Math.abs(dz) == half;
					p.put(x, y, SPIRE_Z + dz, corner ? seam() : (y % 6 == 3 && x == 0 || y % 6 == 3 && dz == 0) ? Blocks.AIR.defaultBlockState() : va());
				}
		}
		// the crown: a cap, and four arms reaching 8 blocks out (north, east, south, west), a hanging insulator under each tip
		p.fill(-1, CROWN_Y, SPIRE_Z - 1, 1, CROWN_Y, SPIRE_Z + 1, seam());
		p.put(0, CROWN_Y + 1, SPIRE_Z, va());
		for (Direction d : Direction.Plane.HORIZONTAL) {
			for (int k = 2; k <= 9; k++)
				p.pillar(p.at(d.getStepX() * k, CROWN_Y, SPIRE_Z + d.getStepZ() * k), d.getAxis());
		}
	}

	/** Where the crown insulator of a district's sky-cable hangs (under the tip of its arm). */
	public static BlockPos crownInsulator(District d) {
		Direction arm = crownArm(d);
		return new BlockPos(arm.getStepX() * 9, CROWN_Y - 1, SPIRE_Z + arm.getStepZ() * 9);
	}

	/** Which crown arm holds a district's cable: north the Foundry, east the Workshops, south the Rail Yard, west the Quarters. */
	static Direction crownArm(District d) {
		return switch (d) {
			case FOUNDRY -> Direction.NORTH;
			case WORKSHOPS -> Direction.EAST;
			case RAIL_YARD -> Direction.SOUTH;
			default -> Direction.WEST;
		};
	}

	/** The broken dome: eight copper-patina ribs from the wall's top toward the spire, some snapped short. */
	private static void dome(CityPlan p) {
		BlockState rib = Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
		for (int k = 0; k < 8; k++) {
			double a = Math.PI / 4 * k;
			int reach = k % 3 == 1 ? 8 : 14;
			for (int s = 0; s <= reach; s++) {
				double r = 16 - s;
				int x = (int) Math.round(Math.sin(a) * r), z = (int) Math.round(-Math.cos(a) * r);
				int y = 13 + (int) Math.round(7 * Math.sin(Math.PI / 2 * s / 14.0));
				if (inSpire(x, z, 2) || !inDrum(x, z))
					continue;
				p.put(x, y, z, rib);
			}
		}
	}

	// ================================================================ the boulevards

	private static void boulevards(CityPlan p) {
		for (int a = HEART + 1; a <= CORE; a++)
			for (int w = -3; w <= 3; w++) {
				p.put(w, -1, a, Math.abs(w) == 3 ? scorched(w, a) : paving(w, a));
				p.put(w, -1, -a, Math.abs(w) == 3 ? scorched(w, -a) : paving(w, -a));
				p.put(a, -1, w, Math.abs(w) == 3 ? scorched(a, w) : paving(a, w));
				p.put(-a, -1, w, Math.abs(w) == 3 ? scorched(-a, w) : paving(-a, w));
			}
	}

	// ================================================================ a district

	/**
	 * A district's frame: u runs out along x, v out along z, from 4 blocks off the axes (u 0 is |x| 4); a world direction
	 * for +u and +v. Blocks are placed in the world through it, so directions come out right on both sides.
	 */
	record Q(CityPlan p, District d, int sx, int sz) {
		BlockPos at(int u, int y, int v) {
			return p.at(sx * (4 + u), y, sz * (4 + v));
		}

		Direction U() {
			return sx > 0 ? Direction.EAST : Direction.WEST;
		}

		Direction V() {
			return sz > 0 ? Direction.SOUTH : Direction.NORTH;
		}

		void put(int u, int y, int v, BlockState s) {
			p.put(at(u, y, v), s);
		}

		void fill(int u0, int y0, int v0, int u1, int y1, int v1, BlockState s) {
			for (int u = Math.min(u0, u1); u <= Math.max(u0, u1); u++)
				for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
					for (int v = Math.min(v0, v1); v <= Math.max(v0, v1); v++)
						put(u, y, v, s);
		}

		/** Hollow walls of a box (no floor, no roof). */
		void walls(int u0, int v0, int u1, int v1, int h, BlockState s) {
			for (int u = u0; u <= u1; u++)
				for (int v = v0; v <= v1; v++)
					if (u == u0 || u == u1 || v == v0 || v == v1)
						for (int y = 0; y < h; y++)
							put(u, y, v, s);
		}

		void air(int u0, int y0, int v0, int u1, int y1, int v1) {
			fill(u0, y0, v0, u1, y1, v1, Blocks.AIR.defaultBlockState());
		}
	}

	/** The parts every district has, then its own set piece. */
	private static void district(Q q) {
		RandomSource r = q.p().random(q.d().ordinal() + 1);
		streets(q, r);
		KitCity.Frame kf = new KitCity.Frame(q.p(), q.d(), q.sx(), q.sz());
		boolean kit = q.p().kit;
		if (kit) {
			BlockPos[] chain = KitCity.yard(kf);
			skyCables(q, chain, KitCity.skyTower(kf));
			KitCity.vaultHouse(kf);
			KitCity.lotA(kf);
			lotAProps(q, r, true);
			setPiece(q, r, kf);
			KitCity.cornerMast(kf, chain[1], CORNER_MAST_ON_BEAD_PATH);
			bots(q, r);
			return;
		}
		BlockPos[] chain = yard(q, r);
		skyTower(q, chain);
		vaultHouse(q, r);
		lotA(q, r);
		switch (q.d()) {
			case FOUNDRY -> foundry(q, r);
			case WORKSHOPS -> workshops(q, r);
			case RAIL_YARD -> railYard(q, r);
			case QUARTERS -> quarters(q, r);
			default -> {}
		}
		cornerMast(q, chain[1]);
		bots(q, r);
	}

	/** The district's set piece with the kit's buildings (D71): the kit draws the buildings, the set piece the rest round them. */
	private static void setPiece(Q q, RandomSource r, KitCity.Frame kf) {
		switch (q.d()) {
			case FOUNDRY -> {
				KitCity.furnaceShed(kf);
				foundry(q, r);
			}
			case WORKSHOPS -> {
				KitCity.workshops(kf);
				KitCity.gantry(kf);
				workshops(q, r);
			}
			case RAIL_YARD -> {
				KitCity.railYard(kf);
				railYard(q, r);
			}
			case QUARTERS -> {
				KitCity.quarters(kf);
				quarters(q, r);
			}
			default -> {}
		}
	}

	/** Two streets of the makers' paving (v 30-34 along u, u 30-34 along v), lamps every 7 blocks alternating sides, cable feeding them. */
	private static void streets(Q q, RandomSource r) {
		for (int a = 0; a <= 66; a++)
			for (int w = 30; w <= 34; w++) {
				q.put(a, -1, w, w == 30 || w == 34 ? scorched(a * 7 + w, q.d().ordinal()) : paving(q.sx() * a, q.sz() * w));
				q.put(w, -1, a, w == 30 || w == 34 ? scorched(a * 5 + w, q.d().ordinal() + 9) : paving(q.sx() * w, q.sz() * a));
			}
		// lamps along street A (along u) and B (along v): the crook over the street; the outer ones lean and snap more (the war)
		for (int k = 0; 2 + 7 * k <= 64; k++) {
			int a = 2 + 7 * k;
			if (a >= 28 && a <= 36)
				continue;
			boolean near = k % 2 == 0;
			boolean outer = a > 50;
			boolean leaning = k % 5 == 3 || outer && r.nextInt(3) == 0;
			boolean snapped = k % 6 == 4 || outer && r.nextInt(2) == 0;
			q.p().streetLamp(q.at(a, 0, near ? 30 : 34), near ? q.V() : q.V().getOpposite(), leaning, snapped);
			boolean leaningB = (k + 2) % 5 == 3 || outer && r.nextInt(3) == 0;
			boolean snappedB = (k + 3) % 6 == 4 || outer && r.nextInt(2) == 0;
			q.p().streetLamp(q.at(near ? 34 : 30, 0, a), near ? q.U().getOpposite() : q.U(), leaningB, snappedB);
		}
		// the boulevard's lamps on this district's side (u -1 and v -1), crooks over the boulevard
		for (int a = 24; a <= 64; a += 8) {
			q.p().streetLamp(q.at(-1, 0, a), q.U().getOpposite(), a % 24 == 16, a > 56);
			q.p().streetLamp(q.at(a, 0, -1), q.V().getOpposite(), a % 24 == 8, a > 56);
		}
		// cable along each street's lamp side (one in from the edge), joining the lamp feet
		for (int a = 0; a <= 66; a++) {
			q.p().cable(q.at(a, 0, 31), Direction.DOWN);
			q.p().cable(q.at(31, 0, a), Direction.DOWN);
		}
	}

	/**
	 * The Substation yard at the district's corner nearest the heart (u, v 19-29): the Substation behind a hazard band, its
	 * cable to the street, a transformer bank of void-alloy fins and iron bars, a makers' control hut, the lattice mast
	 * (four Create Metal Girders and a top insulator) that carries the first sky-cable span, an iron-bar fence. The yard is
	 * the one place with warning colours. Returns the chain so far: the Substation, the mast's insulator.
	 */
	private static BlockPos[] yard(Q q, RandomSource r) {
		// floor and fence
		for (int u = 19; u <= 29; u++)
			for (int v = 19; v <= 29; v++) {
				q.put(u, -1, v, hash(u, v, 21) % 5 == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState());
				boolean edge = u == 19 || u == 29 || v == 19 || v == 29;
				boolean gate = v == 29 && u >= 21 && u <= 23;
				if (edge && !gate)
					q.fill(u, 0, v, u, 1, v, Blocks.IRON_BARS.defaultBlockState());
			}
		// one hazard band across the yard in front of the Substation, muted safety yellow and black
		for (int u = 20; u <= 28; u++)
			q.put(u, -1, 23, (u % 2 == 0 ? Blocks.YELLOW_TERRACOTTA : Blocks.BLACK_TERRACOTTA).defaultBlockState());
		// the Substation, facing the gate
		BlockPos sub = q.at(22, 0, 21);
		q.p().substation(sub, q.V());
		// its cable out of the gate to the street's cable
		for (int v = 22; v <= 30; v++)
			if (v != 23)
				q.p().cable(q.at(22, 0, v), Direction.DOWN);
		q.p().cable(q.at(22, 0, 23), Direction.DOWN);
		// the transformer bank: void-alloy fins (seamed, lit along their edges) with iron bars between
		for (int u = 24; u <= 28; u++)
			for (int y = 0; y < 3; y++)
				q.put(u, y, 24, u % 2 == 0 ? seam() : Blocks.IRON_BARS.defaultBlockState());
		q.fill(24, 3, 24, 28, 3, 24, va());
		// the makers' control hut, its door on the side toward the Substation
		q.walls(25, 26, 28, 28, 3, casing());
		q.air(25, 0, 27, 25, 1, 27);
		q.fill(25, 3, 26, 28, 3, 28, slab(Blocks.DARK_OAK_SLAB));
		q.put(27, 0, 27, machine(ModWastes.BROKEN_PRESS.get(), q.V().getOpposite()));
		// the lattice mast: Create Metal Girders, the top insulator on them, the riser up its side (D72) on the side that keeps
		// its jumper away from the spans, which leave toward the Substation and the tower (-u)
		BlockPos mastFoot = q.at(26, 0, 21);
		Direction face = riserFace(q.U().getOpposite());
		BlockPos mastIns = mast(q, mastFoot, face);
		// the yard's ground cable to the riser's foot: along the foot's row from the Substation's gate cable (v 22) or from
		// the Substation's back (v 20)
		BlockPos riserFoot = mastFoot.relative(face);
		int footV = q.sz() * (riserFoot.getZ() - q.p().origin.getZ()) - 4;
		for (int u = footV == 22 ? 23 : 22; u <= 25; u++)
			q.p().cable(q.at(u, 0, footV), Direction.DOWN);
		q.put(28, 0, 20, scrapHeap(r));
		return new BlockPos[] { sub, mastIns };
	}

	/** Every lattice mast's height in girders (the yard's five, D61; the corner masts the same). */
	public static final int MAST_HEIGHT = 5;

	/**
	 * A lattice mast (D61, D72): {@link #MAST_HEIGHT} Create Metal Girders from {@code foot}, a top-mounted insulator on them,
	 * and a riser of Derelict Cable up their {@code face} side (in the cells beside the girders, its foot on the ground).
	 * Returns the insulator.
	 */
	static BlockPos mast(Q q, BlockPos foot, Direction face) {
		for (int y = 0; y < MAST_HEIGHT; y++)
			q.p().put(foot.above(y), girder());
		BlockPos ins = foot.above(MAST_HEIGHT);
		q.p().insulator(ins, Mount.TOP, q.V());
		q.p().riser(foot.relative(face), face.getOpposite(), MAST_HEIGHT);
		return ins;
	}

	/**
	 * The girder face a riser goes up so its jumper (which bends round to the girder's edge toward the face's clockwise
	 * side: the east edge for a riser on the north face, as in the preview) stays on the far side from the spans leaving
	 * the mast toward {@code spans}, as in the approved preview.
	 */
	static Direction riserFace(Direction spans) {
		return spans.getClockWise();
	}

	/**
	 * Where each district's corner mast stands (D72; frame u, v): the street-corner block of lot B by the streets' crossing
	 * (provisional, for the user's word: progress doc, "D72").
	 */
	public static final int[] CORNER_MAST = { 35, 29 };
	/**
	 * The corner mast's span is on the wake bead's path (provisional, D72): the bead runs out to it from the yard mast at
	 * the same time as up to the tower (stage 1, its ring at hop 2). Off it, the span and ring just light at the wake.
	 */
	public static final boolean CORNER_MAST_ON_BEAD_PATH = true;

	/**
	 * The district's corner mast (D72; world brief: cable rising up lattice masts at the corners): a lattice mast at the
	 * street corner whose riser carries the street cable up to the sky-web, a span from its insulator to the yard mast's.
	 * Its riser goes up the mast's +v (street) face or +u face, whichever keeps the jumper off the span; its foot joins the
	 * street cable (v 31) through the floor cable between them. The building kit (design/briefs/clockwork-wastes-buildings.md)
	 * will place these through here.
	 */
	static void cornerMast(Q q, BlockPos yardIns) {
		BlockPos foot = q.at(CORNER_MAST[0], 0, CORNER_MAST[1]);
		Direction face = riserFace(q.U().getOpposite());
		if (face != q.U() && face != q.V())
			face = riserFace(q.V().getOpposite());
		BlockPos ins = mast(q, foot, face);
		// the floor cable from the riser's foot along +v to the street's cable
		BlockPos riserFoot = foot.relative(face);
		int u = q.sx() * (riserFoot.getX() - q.p().origin.getX()) - 4, v = q.sz() * (riserFoot.getZ() - q.p().origin.getZ()) - 4;
		for (int w = v + 1; w < 31; w++)
			q.p().cable(q.at(u, 0, w), Direction.DOWN);
		// its span to the yard mast, the bead coming out to it from there (or not: CORNER_MAST_ON_BEAD_PATH)
		q.p().span(ins, yardIns, CORNER_MAST_ON_BEAD_PATH ? 1 : -1, false);
		q.p().hop(ins, CORNER_MAST_ON_BEAD_PATH ? 2 : 0);
	}

	/**
	 * The sky-cable tower beside the yard (u 12-14, v 23-25, 22 tall, slotted, its top snapped), its wall insulator facing
	 * the heart; and the chain's spans: Substation tray to the mast (stage 0), mast to the tower (1), tower to the crown
	 * insulator under its arm (2). Each span is kept by its upper end; the bead runs from the Substation upward (D64). A
	 * snapped span hangs from a second insulator lower on the tower.
	 */
	private static void skyTower(Q q, BlockPos[] chain) {
		int h = 22;
		for (int y = 0; y < h; y++)
			for (int u = 12; u <= 14; u++)
				for (int v = 23; v <= 25; v++) {
					boolean corner = (u == 12 || u == 14) && (v == 23 || v == 25);
					boolean mid = u == 13 && v == 24;
					boolean slot = !corner && !mid && y % 5 >= 1 && y % 5 <= 3 && (u == 13 || v == 24);
					if (mid)
						continue;
					if (corner)
						q.p().pillar(q.at(u, y, v), Direction.Axis.Y);
					else
						q.put(u, y, v, slot ? Blocks.AIR.defaultBlockState() : y % 7 == 3 ? seam() : va());
				}
		q.fill(12, h, 23, 14, h, 25, va());
		q.put(13, h + 1, 24, va());
		// the wall insulator near the top, on the face toward the heart (its wall is the tower, behind it)
		BlockPos towerIns = q.at(13, h - 3, 22);
		q.p().insulator(towerIns, Mount.WALL, q.V().getOpposite());
		skyCables(q, chain, towerIns);
		// a snapped span from a lower insulator, hanging out toward the district's edge
		BlockPos low = q.at(15, 9, 24);
		q.p().insulator(low, Mount.WALL, q.U());
		q.p().snapped(low, yaw(q.U()));
	}

	/**
	 * The district's chain of sky-cable spans (D61, D64): the crown's insulator for it, then the Substation's tray to the
	 * mast (stage 0), the mast to the tower's insulator (1), the tower's to the crown (2), each kept by its upper end.
	 */
	private static void skyCables(Q q, BlockPos[] chain, BlockPos towerIns) {
		// the crown's insulator for this district: on the spire, but its district's (it holds that district's cable)
		BlockPos crown = q.p().at(crownInsulator(q.d()).getX(), crownInsulator(q.d()).getY(), crownInsulator(q.d()).getZ());
		q.p().insulator(crown, Mount.HANGING, Direction.NORTH);
		// the chain: the bead leaves the Substation and hops up to the crown, 2 s a span
		q.p().span(chain[1], chain[0], 0, false);
		q.p().span(towerIns, chain[1], 1, false);
		q.p().span(crown, towerIns, 2, false);
		q.p().hop(chain[1], 1);
		q.p().hop(towerIns, 2);
		q.p().hop(crown, 3);
	}

	/** An entity-style yaw (0 south, 90 west) for a direction. */
	static float yaw(Direction d) {
		return d.toYRot();
	}

	/**
	 * The vault house (u 20-28, v 4-12): void-alloy walls 5 high with a seamed course, its locked Sliding Door toward the
	 * boulevard, the City Vault facing the door from the back wall, a Sentry on the wall above it, charging alcoves along the
	 * side walls, a wall lamp over the door and cable along the front wall at head height.
	 */
	private static void vaultHouse(Q q, RandomSource r) {
		for (int u = 20; u <= 28; u++)
			for (int v = 4; v <= 12; v++) {
				q.put(u, -1, v, va());
				boolean wall = u == 20 || u == 28 || v == 4 || v == 12;
				for (int y = 0; y < 5; y++)
					q.put(u, y, v, wall ? (y == 3 ? seam() : va()) : Blocks.AIR.defaultBlockState());
				q.put(u, 5, v, va());
			}
		q.p().doorAcross(q.at(23, 0, 4), q.at(24, 0, 4), q.V().getOpposite(), false);
		q.p().vault(q.at(24, 0, 11), q.V().getOpposite(), false);
		q.p().wallLamp(q.at(22, 3, 3), q.V().getOpposite());
		for (int u : new int[] { 20, 21, 25, 26, 27, 28 })
			q.p().cable(q.at(u, 2, 3), q.V());
		for (int v : new int[] { 6, 9 }) {
			q.p().alcove(q.at(21, 0, v), q.U());
			q.p().alcove(q.at(27, 0, v), q.U().getOpposite());
		}
		q.put(22, 0, 10, worker(WorkerPose.SLUMPED, q.V().getOpposite()));
	}

	/**
	 * Lot A (u 0-29, v 35-66), the makers' layer: two sheds in the district's material, one behind a locked Sliding Door,
	 * broken machines inside, shells against the walls, a pile of scrap heaps where the Scuttlers sleep, the district's one
	 * standing maker statue facing away from the heart, a small shed with a lone press near the tower.
	 */
	private static void lotA(Q q, RandomSource r) {
		BlockState wall = material(q.d());
		// shed 1, open doorway (automaton scale, 2 x 3); shed 2 behind a locked door; the small shed near the tower (lot N1)
		shed(q, 2, 37, 14, 50, 6, wall, r, false);
		shed(q, 17, 40, 28, 60, 7, wall, r, true);
		shed(q, 2, 21, 9, 28, 4, wall, r, false);
		lotAProps(q, r, false);
	}

	/**
	 * Lot A's machines, shells, scrap heaps and statue, in the boxes or the kit's sheds (in the kit's: the shell that leaned
	 * on shed 2's outer wall sits inside it against that wall, and the small shed's press and shell move clear of the sky
	 * tower's plinth).
	 */
	private static void lotAProps(Q q, RandomSource r, boolean kit) {
		q.put(5, 0, 44, machine(ModWastes.BROKEN_MIXER.get(), q.U()));
		q.put(10, 0, 41, machine(ModWastes.BROKEN_PRESS.get(), q.V()));
		q.put(11, 0, 47, machine(ModWastes.BROKEN_CRUSHING_WHEEL.get(), q.U().getOpposite()));
		q.put(3, 0, 39, worker(WorkerPose.SLUMPED, q.U()));
		q.put(20, 0, 50, machine(ModWastes.BROKEN_DEPLOYER.get(), q.V()));
		q.put(25, 0, 55, machine(ModWastes.BROKEN_MECHANICAL_ARM.get(), q.U().getOpposite()));
		q.p().alcove(q.at(kit ? 26 : 27, 0, 45), q.U().getOpposite());
		q.p().alcove(q.at(kit ? 26 : 27, 0, 48), q.U().getOpposite());
		if (kit)
			q.put(19, 0, 52, worker(WorkerPose.SLUMPED, q.U()));
		else
			q.put(16, 0, 52, worker(WorkerPose.SLUMPED, q.U().getOpposite()));
		// the scrap heaps the Scuttlers curl in
		for (int[] c : new int[][] { { 6, 55 }, { 7, 56 }, { 9, 55 }, { 8, 58 }, { 10, 57 }, { 5, 57 } })
			q.put(c[0], 0, c[1], scrapHeap(r));
		q.put(7, 0, 54, machine(ModWastes.BROKEN_CRUSHING_WHEEL.get(), q.V()));
		// the standing statue, facing away from the heart
		q.p().statue(q.at(12, 0, 63), q.V());
		// the small shed's press, and a shell by it
		q.put(kit ? 4 : 5, 0, kit ? 23 : 25, machine(ModWastes.BROKEN_PRESS.get(), q.U()));
		q.put(kit ? 8 : 10, 0, 22, worker(WorkerPose.SLUMPED, q.U()));
	}

	/** Each district's makers' walls: Foundry brick, Workshops andesite casing, Rail Yard stone brick, Quarters stone brick. */
	static BlockState material(District d) {
		return switch (d) {
			case FOUNDRY -> Blocks.BRICKS.defaultBlockState();
			case WORKSHOPS -> casing();
			default -> Blocks.STONE_BRICKS.defaultBlockState();
		};
	}

	/**
	 * A makers' shed: walls h high, windows, a roof of dark walnut slabs with holes (more toward the city's edge), a doorway
	 * (2 x 3) in its heart-side short wall, or a locked district Sliding Door there.
	 */
	private static void shed(Q q, int u0, int v0, int u1, int v1, int h, BlockState wall, RandomSource r, boolean locked) {
		for (int u = u0; u <= u1; u++)
			for (int v = v0; v <= v1; v++) {
				boolean edge = u == u0 || u == u1 || v == v0 || v == v1;
				for (int y = 0; y < h; y++) {
					boolean window = edge && y == 2 && (u + v) % 4 == 0 && !(u == u0 || u == u1) == (v == v0 || v == v1) == false;
					q.put(u, y, v, edge ? (window ? Blocks.IRON_BARS.defaultBlockState() : wall) : Blocks.AIR.defaultBlockState());
				}
				boolean hole = r.nextInt(100) < 12 + Math.max(u, v);
				q.put(u, h, v, hole ? Blocks.AIR.defaultBlockState() : slab(Blocks.DARK_OAK_SLAB));
			}
		int mid = (u0 + u1) / 2;
		q.air(mid, 0, v0, mid + 1, 2, v0);
		if (locked)
			q.p().doorAcross(q.at(mid, 0, v0), q.at(mid + 1, 0, v0), q.V().getOpposite(), false);
		// the makers' plaque beside its door (D72)
		doorPlaque(q, mid - 1, v0, q.V().getOpposite());
	}

	/** Plaques this far out (u or v, in a district's frame) and beyond hang askew (D72; provisional: progress doc, "D72"). */
	public static final int PLAQUE_ASKEW_FROM = 50;
	/** A plaque's height up its wall, in blocks: the brief's "about 1.5 up" (its plate is 1.2-1.75 blocks up in this block). */
	public static final int PLAQUE_Y = 1;

	/**
	 * A Brass Plaque (D72) on a makers' wall beside a door or on a terrace: on the wall block at (u, {@link #PLAQUE_Y}, v),
	 * in the block in front of it toward {@code out}, askew in the district's outer part ({@link #PLAQUE_ASKEW_FROM}). The
	 * one place plaques are put, so the building kit (design/briefs/clockwork-wastes-buildings.md) places them through here.
	 */
	static void doorPlaque(Q q, int u, int v, Direction out) {
		BlockPos wall = q.at(u, PLAQUE_Y, v);
		q.p().plaque(wall.relative(out), out, Math.max(u, v) >= PLAQUE_ASKEW_FROM);
	}

	// ---------------------------------------------------------------- the bots

	/**
	 * The district's dormant bots (P4): 2-4 Sentries (the vault house wall over the vault, the yard's floor, the sky tower's
	 * street side, shed 2's wall) and one pack of 3-5 Scuttlers curled in the scrap heaps.
	 */
	private static void bots(Q q, RandomSource r) {
		int sentries = 2 + r.nextInt(3), scuttlers = 3 + r.nextInt(3);
		boolean kit = q.p().kit;
		// in the kit's buildings the vault house stands a course higher (its plinth) and the sky tower is 5 x 5
		BlockPos[] walls = { q.at(24, kit ? 3 : 2, 12), q.at(25, -1, 22), q.at(14, 3, kit ? 26 : 25), q.at(kit ? 18 : 17, 3, 47) };
		Direction[] faces = { q.V().getOpposite(), Direction.UP, q.V(), q.U().getOpposite() };
		for (int i = 0; i < sentries; i++)
			q.p().sentry(walls[i], faces[i]);
		int[][] dens = { { 6, 54 }, { 8, 54 }, { 10, 56 }, { 6, 58 }, { 9, 59 } };
		for (int i = 0; i < scuttlers; i++)
			q.p().scuttler(q.at(dens[i][0], 0, dens[i][1]));
	}

	// ================================================================ the set pieces (lot B: u 35-66, v 0-29; lot C: u, v 35-66)

	/**
	 * The Foundry: brick furnace sheds with a cold blast furnace and crucibles, two chimneys and a third toppled across
	 * street A, a scorch fan up a furnace wall; the casting floor (void-alloy moulds shaped like limbs and heads, stacked; a
	 * frozen pour with a half-cast body in its mould and the dark glass-like spill; a girder crane with its ladle tipped).
	 */
	private static void foundry(Q q, RandomSource r) {
		if (!q.p().kit)
			furnaceBoxes(q, r);
		castingFloor(q);
	}

	/** The boxes' furnace shed, chimneys and the chimney fallen across street A (the kit's {@link KitCity#furnaceShed} replaces them). */
	private static void furnaceBoxes(Q q, RandomSource r) {
		BlockState brick = Blocks.BRICKS.defaultBlockState();
		// the furnace shed (lot B)
		shed(q, 38, 4, 56, 18, 7, brick, r, false);
		for (int u = 40; u <= 54; u += 3) {
			q.put(u, 0, 15, Blocks.BLAST_FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, q.V().getOpposite()));
			q.put(u, 0, 8, Blocks.CAULDRON.defaultBlockState());
		}
		q.put(46, 0, 11, machine(ModWastes.BROKEN_PRESS.get(), q.V()));
		q.put(50, 0, 11, machine(ModWastes.BROKEN_MIXER.get(), q.V()));
		// the scorch fan up its outer wall
		for (int y = 0; y < 6; y++)
			for (int du = -y / 2 - 1; du <= y / 2 + 1; du++)
				if (hash(du, y, 5) % 3 != 0)
					q.put(47 + du, y, 18, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		// chimneys, 2 x 2, 16 high; a third toppled across street A
		for (int[] c : new int[][] { { 58, 6 }, { 58, 14 } })
			q.fill(c[0], 0, c[1], c[0] + 1, 15, c[1] + 1, brick);
		for (int v = 26; v <= 38; v++)
			if (v % 5 != 2)
				q.fill(45, 0, v, 46, 1, v, brick);
	}

	/** The Foundry's casting floor (lot C): moulds in rows, stacked moulds, the frozen pour, the crane and its ladle. */
	private static void castingFloor(Q q) {
		// the casting floor (lot C): moulds in rows, stacked moulds, the frozen pour
		for (int u = 40; u <= 64; u++)
			for (int v = 40; v <= 64; v++)
				q.put(u, -1, v, va());
		for (int u = 41; u <= 55; u += 7)
			for (int v = 42; v <= 50; v += 4) {
				q.fill(u, 0, v, u + 4, 0, v + 2, va());
				// the limb's shape left in it: a long groove, a round head
				q.air(u + 1, 0, v + 1, u + 3, 0, v + 1);
			}
		for (int[] s : new int[][] { { 62, 42 }, { 62, 46 }, { 61, 51 } })
			q.fill(s[0], 0, s[1], s[0] + 1, 1 + (s[0] + s[1]) % 2, s[1] + 2, va());
		// the frozen pour: a body half cast in its mould, the spill round it
		q.fill(46, 0, 56, 52, 0, 60, va());
		q.air(47, 0, 57, 51, 0, 59);
		q.put(49, 0, 58, worker(WorkerPose.FALLEN, q.U()));
		for (int[] g : new int[][] { { 47, 57 }, { 48, 57 }, { 51, 59 }, { 47, 59 }, { 52, 61 }, { 53, 61 }, { 53, 62 } })
			q.put(g[0], g[1] > 60 ? -1 : 0, g[1], Blocks.TINTED_GLASS.defaultBlockState());
		// the crane: two girder legs, a girder beam, the ladle hanging tipped on chains over the pour
		for (int y = 0; y < 8; y++) {
			q.put(44, y, 58, girder());
			q.put(55, y, 58, girder());
		}
		for (int u = 44; u <= 55; u++)
			q.put(u, 8, 58, girderAlong(q.U().getAxis()));
		q.fill(50, 6, 58, 50, 7, 58, Blocks.CHAIN.defaultBlockState());
		q.put(50, 5, 58, Blocks.CAULDRON.defaultBlockState());
		q.put(51, 4, 58, Blocks.TINTED_GLASS.defaultBlockState());
		// a resistance shell at the edge, one Worker fallen by the moulds
		q.put(63, 0, 63, resistance(ResistancePose.FALLEN, q.U().getOpposite()));
		q.put(57, 0, 52, worker(WorkerPose.SLUMPED, q.V().getOpposite()));
	}

	/**
	 * The Workshops: long andesite sheds with walnut benches and broken Mechanical Arms and Deployers on them; the assembly
	 * gantry (girders) over a row of five clamps, each body more complete than the last (the last a whole Worker); a shed
	 * roof fallen onto the line and bolt craters in the floor.
	 */
	private static void workshops(Q q, RandomSource r) {
		if (q.p().kit) {
			// the kit's long sheds (their benches, machines and line shafts inside) and its gantry over the five clamps
			q.put(62, 0, 64, resistance(ResistancePose.SLUMPED, q.U().getOpposite()));
			return;
		}
		BlockState wall = casing();
		shed(q, 37, 3, 64, 12, 6, wall, r, false);
		shed(q, 37, 17, 64, 26, 6, wall, r, false);
		for (int u = 40; u <= 61; u += 3) {
			q.put(u, 0, 7, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			q.put(u, 1, 7, (u / 3 % 2 == 0 ? ModWastes.BROKEN_MECHANICAL_ARM : ModWastes.BROKEN_DEPLOYER).get().defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, q.V()));
			q.put(u, 0, 21, slab(Blocks.DARK_OAK_SLAB));
			q.put(u + 1, 0, 21, slab(Blocks.DARK_OAK_SLAB));
		}
		// the assembly line (lot C): the gantry
		for (int y = 0; y < 7; y++) {
			q.put(39, y, 52, girder());
			q.put(63, y, 52, girder());
		}
		for (int u = 39; u <= 63; u++)
			q.put(u, 7, 52, girderAlong(q.U().getAxis()));
		// five Assembly Clamps (D72), each body more complete than the last: the armature; + the chest cage; + the core and
		// upper arms; plated with an empty lens socket; whole with its lens in (stage 5 stands where the frozen Worker stood)
		for (int i = 0; i < 5; i++)
			q.p().clamp(q.at(43 + 4 * i, 0, 52), q.V().getOpposite(), i + 1);
		// the fallen roof across the line's far end, and bolt craters
		for (int u = 56; u <= 64; u++)
			q.put(u, 0, 57 + (u % 3), slab(Blocks.DARK_OAK_SLAB));
		for (int[] c : new int[][] { { 47, 61 }, { 58, 44 } })
			for (int du = -1; du <= 1; du++)
				for (int dv = -1; dv <= 1; dv++)
					q.put(c[0] + du, -1, c[1] + dv, du == 0 && dv == 0 ? Blocks.AIR.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState());
		q.put(62, 0, 64, resistance(ResistancePose.SLUMPED, q.U().getOpposite()));
	}

	/**
	 * The Rail Yard: track along the platform, the platform under a canopy, charging cradles on it, a void-alloy signal mast
	 * with its lamp; the last train halted at the platform, doors open, a Resistance Shell on the carriage step reaching back
	 * down, uprising shells on the platform behind it; a turntable, a signal box, a carriage derailed on its side, track torn
	 * up in a crater.
	 */
	private static void railYard(Q q, RandomSource r) {
		BlockState rail = Blocks.RAIL.defaultBlockState();
		// lot C: track at v 48 along u, the train on it, the platform v 50-53 under its canopy
		for (int u = 35; u <= 66; u++) {
			q.put(u, -1, 47, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			q.put(u, -1, 48, Blocks.GRAVEL.defaultBlockState());
			q.put(u, -1, 49, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			q.put(u, 0, 48, CityPlan.with(rail, "shape", q.U().getAxis() == Direction.Axis.X ? "east_west" : "north_south"));
		}
		boolean kit = q.p().kit;
		// (the kit's canopy holds its own platform, posts and cradles)
		for (int u = 36; u <= 64 && !kit; u++)
			for (int v = 50; v <= 53; v++) {
				q.put(u, 0, v, Blocks.POLISHED_ANDESITE.defaultBlockState());
				q.put(u, 4, v, v == 50 || v == 53 ? slab(Blocks.DARK_OAK_SLAB) : casing());
			}
		for (int u = 38; u <= 62 && !kit; u += 6)
			for (int y = 1; y <= 3; y++)
				q.put(u, y, 53, Blocks.DARK_OAK_FENCE.defaultBlockState());
		// charging cradles on the platform, where passengers waited
		for (int u : kit ? new int[0] : new int[] { 41, 47, 59 })
			q.p().alcove(q.at(u, 1, 53), q.V().getOpposite());
		// the last train: two carriages and a locomotive on the track (3 wide, 3 tall), doors open toward the platform
		for (int car = 0; car < 3; car++) {
			int u0 = 38 + car * 9, u1 = u0 + 7;
			for (int u = u0; u <= u1; u++)
				for (int v = 47; v <= 49; v++)
					for (int y = 1; y <= 3; y++) {
						boolean shell = u == u0 || u == u1 || v == 47 || v == 49 || y == 1 || y == 3;
						boolean window = y == 2 && (v == 47 || v == 49) && u % 2 == 0 && u != u0 && u != u1;
						q.put(u, y, v, !shell ? Blocks.AIR.defaultBlockState()
							: window ? Blocks.GLASS_PANE.defaultBlockState() : car == 2 ? brassCasing() : railCasing());
					}
			// the open door toward the platform
			q.air(u0 + 3, 2, 49, u0 + 4, 2, 49);
			q.put(u0 + 3, 1, 49, slab(Blocks.DARK_OAK_SLAB));
		}
		// the resistance shell on the first carriage's step, reaching back down to the platform; uprising shells behind it
		q.put(41, 1, 49, Blocks.AIR.defaultBlockState());
		q.put(41, 1, 49, resistance(ResistancePose.REACHING, q.V()));
		q.put(44, 1, 51, worker(WorkerPose.FALLEN, q.U()));
		q.put(52, 1, 52, worker(WorkerPose.FALLEN, q.U().getOpposite()));
		q.put(56, 1, 51, worker(WorkerPose.SLUMPED, q.V()));
		// a signal mast of void-alloy with its lamp
		for (int y = 0; y < 6; y++)
			q.p().pillar(q.at(37, y, 45), Direction.Axis.Y);
		q.p().wallLamp(q.at(37, 4, 46), q.V());
		// lot B: the turntable (a ring of smooth stone with track across), the signal box, a derailed carriage, torn track
		for (int du = -5; du <= 5; du++)
			for (int dv = -5; dv <= 5; dv++) {
				int d2 = du * du + dv * dv;
				if (d2 <= 25)
					q.put(48 + du, -1, 14 + dv, d2 >= 16 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState());
				if (d2 <= 16 && dv == 0)
					q.put(48 + du, 0, 14, CityPlan.with(rail, "shape", q.U().getAxis() == Direction.Axis.X ? "east_west" : "north_south"));
			}
		if (!kit)
			shed(q, 38, 3, 42, 7, 4, Blocks.STONE_BRICKS.defaultBlockState(), r, false);
		q.put(40, 0, 5, machine(ModWastes.BROKEN_PRESS.get(), q.U()));
		// the derailed carriage on its side (lying across the yard)
		for (int u = 55; u <= 62; u++)
			for (int v = 20; v <= 23; v++)
				for (int y = 0; y <= 2; y++) {
					boolean shell = u == 55 || u == 62 || v == 20 || v == 23 || y == 0 || y == 2;
					if (shell)
						q.put(u, y, v, y == 1 && (v == 20 || v == 23) && u % 2 == 1 ? Blocks.GLASS_PANE.defaultBlockState() : railCasing());
				}
		for (int u = 55; u <= 64; u++)
			if (u % 4 != 1)
				q.put(u, 0, 26, CityPlan.with(rail, "shape", q.U().getAxis() == Direction.Axis.X ? "east_west" : "north_south"));
		for (int du = -2; du <= 2; du++)
			q.put(60 + du, -1, 26, du == 0 ? Blocks.AIR.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState());
		q.put(64, 0, 64, resistance(ResistancePose.FALLEN, q.V().getOpposite()));
	}

	/**
	 * The Quarters: the makers' terraces with their small doorways bricked up (lot B); the alcove wall, charging alcoves three
	 * tiers high reached by a ramp (barricaded) and a Lift with a call plate at each tier (lot C); the vigil, alcoves round a
	 * toppled maker statue with every alcove's shell turned to look at it; one alcove tower split down the middle.
	 */
	/** Dead vines (D72) on about one alcove in this many in the Quarters' alcove wall and split tower. */
	public static final int VINES_ONE_IN = 4;
	private static final int VINES_SALT = 7202;

	private static void quarters(Q q, RandomSource r) {
		BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
		boolean kit = q.p().kit;
		// the makers' terraces (lot B): a row of small houses, their human-sized doorways bricked up (the kit's: KitCity.quarters)
		for (int i = 0; i < 4 && !kit; i++) {
			int u0 = 38 + i * 7;
			shed(q, u0, 6, u0 + 5, 12, 4, brick, r, false);
			q.fill(u0 + 2, 0, 6, u0 + 3, 2, 6, brick);
			q.put(u0 + 2, 0, 6, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
			q.put(u0 + 2, 1, 6, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
		}
		// dead vines (D72) on about one alcove in VINES_ONE_IN of the alcove wall and the split tower, drawn from their own
		// stream of the plan's seed (so the district's own draws, its bots among them, stay as they were)
		RandomSource vines = q.p().random(VINES_SALT);
		if (!kit) {
		// the alcove wall (lot C): along u 38-52 at v 40, open toward +v, tiers at 0, 4, 8; its back wall at v 39
		int[] tiers = { 0, 4, 8 };
		for (int u = 38; u <= 53; u++) {
			for (int y = 0; y <= 11; y++)
				q.put(u, y, 39, va());
			for (int t : tiers) {
				if (u <= 52)
					q.p().alcove(q.at(u, t, 40), q.V(), vines.nextInt(VINES_ONE_IN) == 0);
				else
					q.fill(u, t, 40, u, t + 2, 40, va());
				q.put(u, t + 3, 40, t == 8 ? va() : seam());
				// the walkway in front of each upper tier
				if (t > 0) {
					q.put(u, t - 1, 41, va());
					q.put(u, t - 1, 42, va());
					// a railing along its edge, open where the ramp comes up to the first walkway
					if (!(t == 4 && u <= 39))
						q.put(u, t, 43, Blocks.IRON_BARS.defaultBlockState());
				}
			}
		}
		// the ramp up to the first walkway (they used ramps, not stairs), and a barricade across it
		// (in half blocks: 8 at the walkway, a half block lower each block out along v)
		for (int i = 1; i <= 8; i++) {
			int v = 42 + i, halves = 8 - i, full = halves / 2;
			for (int u = 38; u <= 39; u++) {
				if (full > 0)
					q.fill(u, 0, v, u, full - 1, v, brick);
				if (halves % 2 == 1)
					q.put(u, full, v, slab(Blocks.STONE_BRICK_SLAB));
			}
		}
		q.put(38, 2, 46, Blocks.IRON_BARS.defaultBlockState());
		q.put(39, 2, 46, scrapHeap(r));
		// the Lift at the wall's end: a 2 x 2 shaft (u 54-55, v 41-42) open toward the walkway, a call plate on the end wall
		// at each tier (D62: landings = the shaft floor and the call plates within 4)
		liftShaft(q, 54, 41, 12, q.U().getOpposite());
		for (int t : tiers)
			q.p().callPlate(q.at(53, t, 41), q.V());
		}
		// the vigil (lot C): alcoves round a toppled statue, each with a slumped Worker turned to look at it
		BlockPos centre = q.at(47, 0, 56);
		q.p().toppledStatue(q.at(46, 0, 56), Direction.NORTH);
		for (int k = 0; k < 8; k++) {
			double a = Math.PI / 4 * k;
			int du = (int) Math.round(Math.sin(a) * 7), dv = (int) Math.round(Math.cos(a) * 7);
			BlockPos al = q.at(47 + du, 0, 56 + dv);
			// an alcove opening toward the statue, the shell in front of it facing the statue
			Direction worldIn = Direction.getNearest(centre.getX() - al.getX(), 0, centre.getZ() - al.getZ());
			q.p().alcove(al, worldIn);
			q.p().put(al.relative(worldIn), worker(WorkerPose.SLUMPED, worldIn));
		}
		if (!kit) {
		// one alcove tower split down the middle (lot C's far corner)
		for (int y = 0; y <= 9; y++)
			for (int u = 59; u <= 64; u++)
				q.put(u, y, 63, u == 61 || u == 62 ? (y == 0 ? scrapHeap(r) : Blocks.AIR.defaultBlockState()) : va());
		for (int y = 0; y < 9; y += 3)
			for (int u : new int[] { 60, 63 })
				q.p().alcove(q.at(u, y, 62), q.V().getOpposite(), vines.nextInt(VINES_ONE_IN) == 0);
		q.put(61, 0, 61, scrapHeap(r));
		q.put(62, 0, 60, scrapHeap(r));
		}
		q.put(64, 0, 58, resistance(ResistancePose.SLUMPED, q.U().getOpposite()));
	}

	/**
	 * A Lift: its 2 x 2 shaft with its south-west... corner cell at (u, v) in the frame's terms, open toward {@code open}
	 * (and away from it), its head {@code head} blocks up. The Lift block decides where the shaft runs (never mirrored,
	 * D62), so its head goes in whichever corner makes the shaft cover exactly these four columns.
	 */
	private static void liftShaft(Q q, int u, int v, int head, Direction open) {
		List<BlockPos> cells = List.of(q.at(u, 0, v), q.at(u + 1, 0, v), q.at(u, 0, v + 1), q.at(u + 1, 0, v + 1));
		for (BlockPos c : cells) {
			BlockPos[] fp = LiftBlock.footprint(c, open);
			boolean all = true;
			for (BlockPos f : fp)
				all &= cells.contains(f);
			if (all) {
				for (BlockPos f : fp)
					for (int y = 0; y < head; y++)
						q.p().put(f.above(y), Blocks.AIR.defaultBlockState());
				q.p().lift(c.above(head), open);
				return;
			}
		}
		throw new IllegalStateException("no corner for the lift's shaft");
	}

	// ================================================================ the outskirts

	/**
	 * The rings of damage (world brief), outward from the districts: snapped towers and fallen lamp posts, then scrap heaps,
	 * barricades and the two war-frames locked together, then stubs; in the outermost band the resistance's siege lines,
	 * their dead behind cover facing in, a toppled siege winch. Everything here only stands where there's ground
	 * ({@link CityApplier}).
	 */
	private static void outskirts(CityPlan p) {
		RandomSource r = p.random(99);
		boolean city = !p.outskirtsOnly;
		// barricades across the streets' ends at each district's edge (welded scrap, the winding key scratched on from outside)
		if (city)
			for (District d : District.DISTRICTS) {
				int[] q = quadrant(d);
				Q f = new Q(p, d, q[0], q[1]);
				p.district = d;
				for (int w = 30; w <= 34; w++) {
					f.put(64, 0, w, w % 2 == 0 ? Blocks.IRON_BARS.defaultBlockState() : scrapHeap(r));
					f.put(64, 1, w, w == 32 ? va() : Blocks.IRON_BARS.defaultBlockState());
					f.put(w, 0, 64, w % 2 == 1 ? Blocks.IRON_BARS.defaultBlockState() : scrapHeap(r));
					f.put(w, 1, 64, w == 32 ? va() : Blocks.IRON_BARS.defaultBlockState());
				}
				// snapped towers and fallen lamp posts just beyond the district (the first ring)
				f.put(68, 0, 12, va());
				f.p().pillar(f.at(68, 1, 12), Direction.Axis.Y);
				f.p().streetLamp(f.at(67, 0, 50), f.U(), true, true);
				f.p().streetLamp(f.at(50, 0, 67), f.V(), true, true);
				// a tower stub with an insulator still holding a snapped span (the web, D61)
				for (int y = 0; y < 7; y++)
					f.p().pillar(f.at(66, y, 66), Direction.Axis.Y);
				BlockPos ins = f.at(66, 7, 66);
				f.p().insulator(ins, Mount.TOP, f.U());
				f.p().snapped(ins, yaw(f.V()));
				p.district = null;
			}
		int inner = city ? CORE + 1 : 8;
		// scattered scrap heaps, stubs and wrecks, thinning outward
		for (int i = 0; i < (city ? 160 : 70); i++) {
			double a = r.nextDouble() * Math.PI * 2, rad = inner + Math.pow(r.nextDouble(), 1.6) * (OUT - inner - 2);
			int x = (int) Math.round(Math.cos(a) * rad), z = (int) Math.round(Math.sin(a) * rad);
			if (Math.abs(x) <= 3 || Math.abs(z) <= 3)
				continue;
			// (the ring's radius reaches into the core square's corners: a wreck there would land on a district's set piece,
			// as one did on the assembly line's first clamp, so it's drawn as before but not put; D72)
			boolean put = !(city && Math.abs(x) <= CORE && Math.abs(z) <= CORE);
			int kind = r.nextInt(10);
			if (kind < 5) {
				BlockState heap = scrapHeap(r);
				if (put)
					p.put(x, 0, z, heap);
			} else if (kind < 7) {
				BlockState wreck = machine(ModWastes.salvage().get(r.nextInt(5)).get(), Direction.from2DDataValue(r.nextInt(4)));
				if (put)
					p.put(x, 0, z, wreck);
			} else if (kind < 9) {
				int h = 1 + r.nextInt(3);
				for (int y = 0; y < h && put; y++)
					p.pillar(p.at(x, y, z), Direction.Axis.Y);
			} else {
				BlockState fallen = worker(WorkerPose.FALLEN, Direction.from2DDataValue(r.nextInt(4)));
				if (put)
					p.put(x, 0, z, fallen);
			}
		}
		// an outskirts-only site's own Elytra (D69): the resistance's cache where they got furthest
		if (!city)
			resistanceCache(p);
		// the two war-frames locked in a grapple (north-west of the city)
		warFrames(p, city ? -88 : -20, city ? -40 : -6);
		// the siege lines: low walls of andesite casing and walnut in an arc facing the city, resistance shells behind them
		int ring = city ? 84 : 30;
		if (p.kit)
			KitCity.siege(p, ring);
		for (int k = 0; k < 64 && !p.kit; k++) {
			double a = Math.PI * 2 * k / 64;
			int x = (int) Math.round(Math.cos(a) * ring), z = (int) Math.round(Math.sin(a) * ring);
			if (Math.abs(x) <= 3 || Math.abs(z) <= 3 || k % 8 == 7)
				continue;
			p.put(x, 0, z, k % 3 == 0 ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : casing());
			if (k % 4 == 1)
				p.put(x, 1, z, casing());
			if (k % 6 == 2) {
				int bx = (int) Math.round(Math.cos(a) * (ring + 1)), bz = (int) Math.round(Math.sin(a) * (ring + 1));
				Direction in = Direction.getNearest(-Math.cos(a), 0, -Math.sin(a));
				p.put(bx, 0, bz, resistance(k % 12 == 2 ? ResistancePose.SLUMPED : ResistancePose.FALLEN, in));
			}
		}
		// the toppled siege winch: a brass mainspring drum on its wheeled frame, its spring sprung out in a loose coil
		int wx = city ? 60 : 16, wz = city ? 86 : 22;
		p.fill(wx, 0, wz, wx + 1, 0, wz + 3, casing());
		p.fill(wx - 1, 1, wz, wx + 2, 2, wz + 3, brass());
		p.fill(wx, 1, wz + 1, wx + 1, 2, wz + 2, Blocks.AIR.defaultBlockState());
		for (int s = 0; s < 14; s++) {
			double a = s * 0.7, rr = 1 + s * 0.35;
			p.put(wx + 4 + (int) Math.round(Math.cos(a) * rr), 0, wz + 2 + (int) Math.round(Math.sin(a) * rr), Blocks.IRON_BARS.defaultBlockState());
		}
	}

	/**
	 * An outskirts-only site's Elytra (D69: the city that didn't fit still gives its wings, so the Wastes keep Elytra parity
	 * with the End-city ships they displace, P45, D18): the resistance's cache in the site's middle ({@link #RESISTANCE_CACHE},
	 * the column world generation made sure has ground), where they got furthest. A plain barrel holding one Elytra
	 * ({@link #RESISTANCE_CACHE_LOOT}) behind a short stretch of their siege-line cover (andesite casing and walnut), one of
	 * their dead slumped against it, its back to the barrel, never lit. Existing blocks only; provisional, for the user.
	 */
	private static void resistanceCache(CityPlan p) {
		BlockPos c = RESISTANCE_CACHE;
		BlockPos barrel = p.at(c.getX(), c.getY(), c.getZ());
		p.put(barrel, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
		p.loot(barrel, RESISTANCE_CACHE_LOOT);
		p.put(c.getX() - 1, c.getY(), c.getZ() - 1, casing());
		p.put(c.getX() - 1, c.getY(), c.getZ(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
		p.put(c.getX() - 1, c.getY(), c.getZ() + 1, casing());
		p.put(c.getX() - 1, c.getY() + 1, c.getZ(), casing());
		p.put(c.getX() + 1, c.getY(), c.getZ(), resistance(ResistancePose.SLUMPED, Direction.EAST));
	}

	/**
	 * The two war-frames locked together where they fell (world brief): the uprising's tall, thin void-alloy frame with its
	 * big dark lens, and the resistance's stocky andesite and brass frame, its mainspring drum split and the spring out, a
	 * door-sized winding key, two glass eyes in brass rims; its hand on the other's lens.
	 */
	private static void warFrames(CityPlan p, int x, int z) {
		// the uprising frame lying along x: legs, a long torso, the head with its lens
		for (int i = 0; i < 11; i++)
			p.pillar(p.at(x + i, 0, z), Direction.Axis.X);
		p.pillar(p.at(x + 2, 0, z - 1), Direction.Axis.X);
		p.pillar(p.at(x + 3, 0, z - 1), Direction.Axis.X);
		p.fill(x + 5, 1, z, x + 8, 1, z, va());
		p.fill(x + 11, 0, z - 1, x + 12, 1, z + 1, va());
		p.put(x + 12, 1, z, Blocks.TINTED_GLASS.defaultBlockState());
		// the resistance frame across it, stocky, its hand on the lens
		p.fill(x + 9, 0, z + 2, x + 11, 2, z + 5, casing());
		p.fill(x + 9, 3, z + 3, x + 11, 3, z + 4, brassCasing());
		p.put(x + 12, 2, z + 1, brass());
		p.put(x + 12, 1, z + 1, casing());
		// its head: two glass eyes in brass
		p.fill(x + 9, 0, z + 6, x + 11, 2, z + 7, casing());
		p.put(x + 9, 1, z + 8, Blocks.BROWN_STAINED_GLASS.defaultBlockState());
		p.put(x + 11, 1, z + 8, Blocks.BROWN_STAINED_GLASS.defaultBlockState());
		p.put(x + 10, 1, z + 8, brass());
		// the split drum on its back and the spring burst out, the winding key the size of a door
		p.fill(x + 7, 0, z + 3, x + 8, 2, z + 4, brass());
		for (int s = 0; s < 10; s++)
			p.put(x + 6 - s / 2, 0, z + 4 + (s % 3), Blocks.IRON_BARS.defaultBlockState());
		p.fill(x + 7, 3, z + 3, x + 7, 5, z + 3, brass());
		p.fill(x + 7, 6, z + 2, x + 7, 6, z + 4, brass());
	}
}
