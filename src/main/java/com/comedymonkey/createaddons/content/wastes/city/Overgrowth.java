package com.comedymonkey.createaddons.content.wastes.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.wastes.ChargingAlcoveBlock;
import com.comedymonkey.createaddons.content.wastes.PropPieceBlock;
import com.comedymonkey.createaddons.content.wastes.SlidingDoorBlock;
import com.comedymonkey.createaddons.content.wastes.ShellBlock;
import com.comedymonkey.createaddons.content.wastes.ThreePartBlock;
import com.comedymonkey.createaddons.content.wastes.city.kit.KitCity;
import com.comedymonkey.createaddons.content.wastes.city.kit.Wear;
import com.comedymonkey.createaddons.content.wastes.overgrowth.BramblecoilBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.BreakrootBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.BreakrootBurstBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.CrookwoodLeavesBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.MossCarpetBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.ShieldcrownLeavesBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.ShiftcapBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.SpringcurlBlock;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The Twisted Battlefields' edge, grown into a city (D91, D92, D94; design/briefs/clockwork-wastes-overgrowth.md): drawn
 * into the plan after everything else, from the plan alone (never the world), so every chunk builds the same overgrowth
 * whichever order they come in.
 * <ul>
 * <li>Density: four tiers from patch noise, so heavy and bare lie side by side, the weight growing outward (the war's last
 * camp at the siege ring is where it came in) and capped by the wear rings: the heart light (one medium patch by a heart
 * door), the worn ring medium, the damaged ring and outskirts heavy; a light fringe 10-16 blocks onto the island.</li>
 * <li>Where: Knotvine and Scale Lichen on the makers' walls (the core only: a wall's plant hangs on the next column),
 * Twinerope under overhangs, moss on sills and tops, crack plants, bushes, roots and Shiftcaps on the ground (put on the
 * column's ground wherever it ends up, {@link CityPlan.Op.Surface}); Shiftcaps in medium and heavy patches only.</li>
 * <li>The grip rule: plants hold to the makers' work and never to void-alloy, except Knotvine on a scarred plate. The
 * never list: nothing at doors, vaults, Substations, lamps' heads, insulators, pylons, lifts, vents, the reaching shell,
 * inside the heart's drum or on Key-Marked Stone; nothing in a prop's strip (every cell its model runs through), a Scuttler's
 * den or a Sentry's mount.</li>
 * <li>One Crookwood in the grapple; one Shieldcrown up through a shed's burnt-out roof in the damaged ring. The trees keep
 * the never list too, and grow only where nothing else is (the burnt roof aside).</li>
 * </ul>
 */
public final class Overgrowth {
	/** The density tiers (the brief's Placement table). */
	public enum Tier {
		BARE, LIGHT, MEDIUM, HEAVY
	}

	private static final int SALT = 9301;
	/** The heart's one medium patch reaches this far from its door. */
	public static final int HEART_PATCH = 4;

	private final CityPlan p;
	private final SimplexNoise patch, fine;
	/** Every block the plan puts, as it ends up (the last put at each place), in the plan's order. */
	private final Map<BlockPos, BlockState> world = new LinkedHashMap<>();
	/** Places nothing grows (the never list, the props' reach, the bots' places) and places a plant has taken. */
	private final Set<BlockPos> clear = new HashSet<>(), taken = new HashSet<>();
	/** Multi-face covers being built up, a cell at a time, in the order they're grown. */
	private final Map<BlockPos, BlockState> covers = new LinkedHashMap<>();
	@Nullable
	private BlockPos heartDoor;
	private final CityEdge edge;

	/** The overgrowth over the plan's ops before it ({@link CityPlan#overgrowthFrom}), its never list worked out. */
	private Overgrowth(CityPlan p) {
		this.p = p;
		RandomSource r = p.random(SALT);
		patch = new SimplexNoise(r);
		fine = new SimplexNoise(r);
		edge = new CityEdge(p);
		List<CityPlan.Op> ops = p.ops();
		for (int i = 0; i < Math.min(ops.size(), p.overgrowthFrom); i++)
			switch (ops.get(i)) {
				case CityPlan.Op.Put put -> world.put(put.at(), put.state());
				// a Scuttler's den and a Sentry's mount stay bare
				case CityPlan.Op.Scuttler s -> {
					clear.add(s.at());
					clear.add(s.at().above());
				}
				case CityPlan.Op.Sentry s -> clear.add(s.at());
				default -> {}
			}
		// a prop stands in every cell its model runs through (D87): nothing grows in its strip
		for (var e : world.entrySet())
			if (e.getValue().getBlock() instanceof PropPieceBlock)
				for (BlockPos q : PropPieceBlock.reach(e.getKey(), e.getValue()))
					if (!q.equals(e.getKey()))
						clear.add(q);
		neverList();
	}

	/** Grows the overgrowth into a finished plan. */
	static void grow(CityPlan p) {
		p.overgrowthFrom = p.ops().size();
		new Overgrowth(p).draw();
	}

	/** The overgrowth's density tiers over a city (dx, dz from its middle). */
	@FunctionalInterface
	public interface Tiers {
		Tier at(int dx, int dz);
	}

	/** The overgrowth's density tiers for the plan's city, from its seed and its heart's door (for the tests and the city's map). */
	public static Tiers tiers(CityPlan p) {
		return new Overgrowth(p)::tier;
	}

	/** Where the heart's medium patch is (dx, dz of the heart's door from the middle), or null: none. */
	@Nullable
	public static int[] heartPatchAt(CityPlan p) {
		BlockPos d = new Overgrowth(p).heartDoor;
		return d == null ? null : new int[] { d.getX() - p.origin.getX(), d.getZ() - p.origin.getZ() };
	}

	// ---------------------------------------------------------------- the density

	/** The tier at (dx, dz) from the city's middle. */
	Tier tier(int dx, int dz) {
		int ring = Wear.ring(dx, dz);
		double across = ring > CityLayout.CORE ? edge.across(dx, dz) : -1;
		if (across > 0) {
			// the fringe onto the island: light, thinning out across the band and its fan
			double left = 1 - across / (1 + CityEdge.FAN);
			return left > 0 && noise(dx, dz) < left * 0.8 ? Tier.LIGHT : Tier.BARE;
		}
		double u = noise(dx, dz) + 0.35 * Math.clamp((ring - 10) / 80.0, 0, 1);
		Tier t = u < 0.47 ? Tier.BARE : u < 0.6 ? Tier.LIGHT : u < 0.72 ? Tier.MEDIUM : Tier.HEAVY;
		// the heart's one medium patch is always there: medium near its door, its edge broken by the noise
		if (ring <= Wear.INTACT_TO && heartPatch(dx, dz)
			&& Math.max(Math.abs(dx - heartDoor.getX() + p.origin.getX()), Math.abs(dz - heartDoor.getZ() + p.origin.getZ())) <= HEART_PATCH - 1)
			return Tier.MEDIUM;
		Tier cap = ring <= Wear.INTACT_TO ? (heartPatch(dx, dz) ? Tier.MEDIUM : Tier.LIGHT) : ring <= Wear.WORN_TO ? Tier.MEDIUM : Tier.HEAVY;
		return t.ordinal() > cap.ordinal() ? cap : t;
	}

	/** Patch noise, 0-1: broad patches with their edges broken up. */
	private double noise(int dx, int dz) {
		double n = 0.75 * patch.getValue(dx / 22.0, dz / 22.0) + 0.25 * fine.getValue(dx / 5.0, dz / 5.0);
		return Math.clamp((n + 1) / 2, 0, 1);
	}

	private boolean heartPatch(int dx, int dz) {
		if (heartDoor == null)
			return false;
		int hx = heartDoor.getX() - p.origin.getX(), hz = heartDoor.getZ() - p.origin.getZ();
		return Math.abs(dx - hx) <= HEART_PATCH && Math.abs(dz - hz) <= HEART_PATCH && !inDrum(dx, dz);
	}

	private Tier tier(BlockPos w) {
		return tier(w.getX() - p.origin.getX(), w.getZ() - p.origin.getZ());
	}

	/** A draw for one place and purpose, 0-1, from the plan's seed. */
	private double roll(BlockPos w, int salt) {
		return (CityLayout.hash(w.getX() * 31 + w.getY(), w.getZ() - w.getY() * 17, SALT + salt) % 10000) / 10000.0;
	}

	// ---------------------------------------------------------------- what plants may hold to

	private static final Map<Block, String> IDS = new ConcurrentHashMap<>();
	private static final Map<BlockState, Boolean> GRIPS = new ConcurrentHashMap<>();

	private static String id(BlockState s) {
		return IDS.computeIfAbsent(s.getBlock(), b -> BuiltInRegistries.BLOCK.getKey(b).getPath());
	}

	/** The uprising's work and the city's machines: nothing roots in them (the grip rule), except Knotvine on a scarred plate. */
	private static final String[] NO_GRIP = { "void_alloy", "seamed", "slotted_grille", "scarred", "clamp_bracket", "derelict", "substation", "city_vault",
		"sliding_door", "conduit_pylon", "lift", "wall_vent", "charging_alcove", "star_reactor", "maker_statue", "key_marked", "broken_", "shell",
		"assembly_clamp", "glass", "window", "blackstone", "brass_plaque", "girder", "insulator" };

	/** The makers' work a plant can hold to: a full block of theirs (soot and scorch fans stay bare: their blackstone). */
	static boolean grips(BlockState s) {
		return GRIPS.computeIfAbsent(s, Overgrowth::gripsOnce);
	}

	private static boolean gripsOnce(BlockState s) {
		if (s.isAir() || !Block.isShapeFullBlock(s.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)))
			return false;
		String id = id(s);
		for (String no : NO_GRIP)
			if (id.contains(no))
				return false;
		return !(s.getBlock() instanceof LeavesBlock) && !(s.getBlock() instanceof MultifaceBlock);
	}

	/** Knotvine's one exception: a cracked or chipped scarred plate gives it a hold. */
	static boolean scarred(BlockState s) {
		return s.is(ModWastes.SCARRED_VOID_ALLOY_PLATE.get());
	}

	private boolean empty(BlockPos w) {
		return !world.containsKey(w) && !taken.contains(w) && !clear.contains(w) && !inHall(w.getX() - p.origin.getX(), w.getZ() - p.origin.getZ());
	}

	/** The heart's hall, the Custodian's arena: nothing grows in it (an outskirts-only site has no heart). */
	private boolean inHall(int dx, int dz) {
		return !p.outskirtsOnly && Math.abs(dx) <= 15 && Math.abs(dz) <= 15 && Math.abs(dx) + Math.abs(dz) <= 27;
	}

	private boolean inDrum(int dx, int dz) {
		return !p.outskirtsOnly && Math.abs(dx) <= 18 && Math.abs(dz) <= 18 && Math.abs(dx) + Math.abs(dz) <= 31;
	}

	/** The never list (the brief's Placement section). */
	private void neverList() {
		for (var e : world.entrySet()) {
			BlockPos w = e.getKey();
			BlockState s = e.getValue();
			String id = id(s);
			if (s.getBlock() instanceof SlidingDoorBlock) {
				// the leaves' path and two blocks either side of the door
				Direction f = s.getValue(SlidingDoorBlock.FACING);
				for (int k = -2; k <= 2; k++)
					for (Direction side : new Direction[] { f.getClockWise(), f.getCounterClockWise() })
						for (int j = 0; j <= 2; j++)
							clear.add(w.relative(f, k).relative(side, j));
			} else if (id.contains("street_lamp") && s.hasProperty(com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock.PART)
				&& s.getValue(com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock.PART) == com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock.Part.HEAD) {
				// the head and the street below its lens
				for (BlockPos q : BlockPos.betweenClosed(w.offset(-2, -3, -2), w.offset(2, 1, 2)))
					clear.add(q.immutable());
			} else if (id.contains("wall_lamp")) {
				for (BlockPos q : BlockPos.betweenClosed(w.offset(-1, -3, -1), w.offset(1, 1, 1)))
					clear.add(q.immutable());
			} else if (id.contains("city_vault") || id.contains("substation") || id.contains("insulator") || id.contains("conduit_pylon")
				|| id.contains("star_reactor") || id.equals("lift") || id.contains("lift_call") || id.contains("wall_vent")
				|| s.getBlock() instanceof ShellBlock.Resistance && s.getValue(ShellBlock.Resistance.POSE) == ShellBlock.ResistancePose.REACHING) {
				for (BlockPos q : BlockPos.betweenClosed(w.offset(-1, -1, -1), w.offset(1, 1, 1)))
					clear.add(q.immutable());
				for (Direction d : Direction.values())
					clear.add(w.relative(d, 2));
			}
			if (s.getBlock() instanceof SlidingDoorBlock && s.getValue(SlidingDoorBlock.HEART) && heartDoor == null)
				heartDoor = w;
		}
	}

	// ---------------------------------------------------------------- drawing it

	private void draw() {
		// the trees first: the shed's roof they break through is gone before anything grows on it
		trees();
		walls();
		overhangs();
		tops();
		ground();
		alcoves();
		for (var e : covers.entrySet())
			p.put(e.getKey(), e.getValue());
	}

	private boolean core(BlockPos w) {
		return p.inCore(w);
	}

	/** Knotvine, Scale Lichen and the Shiftcap Shelves on the outward faces of the makers' walls (the core's). */
	private void walls() {
		for (var e : List.copyOf(world.entrySet())) {
			BlockPos wall = e.getKey();
			BlockState s = e.getValue();
			boolean grip = grips(s), scar = scarred(s);
			if (!grip && !scar || !core(wall))
				continue;
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos c = wall.relative(d);
				int y = c.getY() - p.origin.getY();
				// (below the floor is the ground's, not a face)
				if (y < 0 || !empty(c) && !covers.containsKey(c) || !outside(c, d))
					continue;
				Tier t = tier(c);
				double r = roll(c, d.ordinal());
				Block plant = null;
				switch (t) {
					case LIGHT -> plant = scar ? null : r < 0.16 && y <= 8 ? ModWastes.SCALE_LICHEN.get() : r > 0.96 && y <= 1 ? ModWastes.KNOTVINE.get() : null;
					case MEDIUM -> {
						int h = 2 + (int) (roll(c.atY(0), 7) * 3);
						if (!scar && y <= 1 && r > 0.97)
							shelf(c, d, 0);
						else
							plant = y <= h && r < 0.75 ? ModWastes.KNOTVINE.get() : !scar && r < 0.3 && y <= 10 ? ModWastes.SCALE_LICHEN.get() : null;
					}
					case HEAVY -> {
						int h = 3 + (int) (roll(c.atY(0), 8) * 10);
						if (!scar && y <= 2 && r > 0.93)
							shelf(c, d, 1);
						else
							plant = y <= h && r < 0.92 ? ModWastes.KNOTVINE.get() : !scar && r < 0.45 ? ModWastes.SCALE_LICHEN.get() : null;
					}
					default -> {}
				}
				if (plant == ModWastes.KNOTVINE.get() || plant == ModWastes.SCALE_LICHEN.get() && !scar)
					cover(c, plant, d.getOpposite());
			}
		}
	}

	/** Open toward {@code d} for a few blocks: an outside face, not a room's. */
	private boolean outside(BlockPos c, Direction d) {
		for (int k = 1; k <= 3; k++)
			if (world.containsKey(c.relative(d, k)))
				return false;
		return true;
	}

	/** A cover's face on {@code toward} at {@code c} (two kinds never share a cell: the first there keeps it). */
	private void cover(BlockPos c, Block kind, Direction toward) {
		BlockState have = covers.get(c);
		if (have != null && !have.is(kind))
			return;
		if (have == null) {
			if (!empty(c))
				return;
			have = kind.defaultBlockState();
			taken.add(c);
		}
		covers.put(c, have.setValue(MultifaceBlock.getFaceProperty(toward), true));
	}

	private void shelf(BlockPos c, Direction out, int older) {
		if (!empty(c) || nearHeart(c))
			return;
		ShiftcapBlock.Age age = ShiftcapBlock.Age.values()[Math.min(2, (int) (roll(c, 21) * 3) + older)];
		put(c, ModWastes.SHIFTCAP_SHELF.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, out)
			.setValue(ShiftcapBlock.AGE, age));
	}

	private void put(BlockPos c, BlockState s) {
		taken.add(c);
		p.put(c, s);
	}

	/** Twinerope under the makers' overhangs: lintels, corbels, eaves, roof holes. In the same column as what it hangs from. */
	private void overhangs() {
		for (var e : List.copyOf(world.entrySet())) {
			BlockPos c = e.getKey().below();
			BlockState over = e.getValue();
			// from the makers' work or a tree's crown, never the uprising's
			if (!grips(over) && !(over.getBlock() instanceof LeavesBlock) || !over.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Direction.DOWN)
				|| !empty(c) || !empty(c.below())
				|| c.getY() - p.origin.getY() < 2 || inDrum(c.getX() - p.origin.getX(), c.getZ() - p.origin.getZ()))
				continue;
			Tier t = tier(c);
			double r = roll(c, 31);
			int len = switch (t) {
				case LIGHT -> r < 0.02 ? 1 : 0;
				case MEDIUM -> r < 0.2 ? 1 + (int) (roll(c, 32) * 3) : 0;
				case HEAVY -> r < 0.5 ? 2 + (int) (roll(c, 32) * 5) : 0;
				default -> 0;
			};
			List<BlockPos> rope = new ArrayList<>();
			for (BlockPos q = c; rope.size() < len && empty(q) && q.getY() - p.origin.getY() >= 1; q = q.below())
				rope.add(q);
			for (int i = 0; i < rope.size(); i++)
				put(rope.get(i), (i == rope.size() - 1 ? ModWastes.TWINEROPE.get() : ModWastes.TWINEROPE_PLANT.get()).defaultBlockState());
		}
	}

	/** Moss on the makers' sills, wall tops and flat roofs (draping over open edges), Knotvine creeping over some. */
	private void tops() {
		for (var e : List.copyOf(world.entrySet())) {
			BlockPos b = e.getKey(), c = b.above();
			if (!grips(e.getValue()) || !empty(c) || b.getY() - p.origin.getY() < 0)
				continue;
			boolean open = true;
			for (int k = 1; k <= 3 && open; k++)
				open = !world.containsKey(c.above(k));
			if (!open)
				continue;
			Tier t = tier(c);
			double r = roll(c, 41);
			double moss = switch (t) {
				case LIGHT -> 0.1;
				case MEDIUM -> 0.3;
				case HEAVY -> 0.65;
				default -> 0;
			};
			if (r < moss)
				put(c, carpet(c));
			else if (t == Tier.HEAVY && r < moss + 0.12)
				cover(c, ModWastes.KNOTVINE.get(), Direction.DOWN);
			else if (t != Tier.BARE && r > 0.97)
				cover(c, ModWastes.SCALE_LICHEN.get(), Direction.DOWN);
		}
	}

	/** A moss carpet at {@code c}, draped down the open sides of what it lies on. */
	private BlockState carpet(BlockPos c) {
		BlockState s = ModWastes.DUSK_MOSS_CARPET.get().defaultBlockState();
		BlockPos under = c.below();
		boolean onBlock = world.containsKey(under);
		for (var d : MossCarpetBlock.DRAPES.entrySet()) {
			BlockPos beside = under.relative(d.getKey());
			BlockState b = world.get(beside);
			boolean covered = b != null && b.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, d.getKey().getOpposite());
			s = s.setValue(d.getValue(), onBlock && !covered && !world.containsKey(c.relative(d.getKey())));
		}
		return s;
	}

	/** The ground's plants: crack plants, bushes, roots, Shiftcaps, moss; bells at the lamps' feet; the fringe onto the island. */
	private void ground() {
		int reach = CityEdge.REACH;
		Set<BlockPos> lampFeet = new HashSet<>(), wallFoot = new HashSet<>();
		Map<BlockPos, Direction> walls = new HashMap<>();
		for (var e : world.entrySet()) {
			BlockPos w = e.getKey();
			if (w.getY() != p.origin.getY())
				continue;
			if (e.getValue().getBlock() instanceof com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock)
				for (Direction d : Direction.Plane.HORIZONTAL)
					lampFeet.add(w.relative(d));
			if (grips(e.getValue()))
				for (Direction d : Direction.Plane.HORIZONTAL) {
					wallFoot.add(w.relative(d));
					walls.putIfAbsent(w.relative(d), d);
				}
		}
		for (int dx = -reach; dx <= reach; dx++)
			for (int dz = -reach; dz <= reach; dz++) {
				BlockPos c = p.at(dx, 0, dz);
				if (!empty(c) || inDrum(dx, dz))
					continue;
				// the floor under it: the plan's paving or the column's ground; never the uprising's floor or a soot fan
				BlockState floor = world.get(c.below());
				if (floor != null && !grips(floor))
					continue;
				Tier t = tier(dx, dz);
				if (t == Tier.BARE)
					continue;
				double r = roll(c, 51);
				BlockState plant = null;
				boolean floorOnly = false, cracks = false;
				if (lampFeet.contains(c))
					plant = r < (t == Tier.LIGHT ? 0.25 : 0.6) ? ModWastes.VIGIL_BELL.get().defaultBlockState() : null;
				else if (wallFoot.contains(c) && t != Tier.LIGHT) {
					boolean heavy = t == Tier.HEAVY;
					floorOnly = true;
					if (heavy && r < 0.03 && p.inCore(c)) {
						plant = burst(c, walls.get(c));
						cracks = plant != null && cracks(c, walls.get(c));
					}
					else if (r < (heavy ? 0.14 : 0.06))
						plant = ModWastes.BRAMBLECOIL.get().defaultBlockState().setValue(BramblecoilBlock.LARGE, heavy && r < 0.07);
					else if (r < (heavy ? 0.22 : 0.09))
						plant = shiftcap(c);
					else if (r < (heavy ? 0.5 : 0.24))
						plant = carpet(c);
				} else
					plant = switch (t) {
						case LIGHT -> r < 0.015 ? rush() : r < 0.023 ? springcurl(c) : r < 0.03 ? carpet(c) : null;
						case MEDIUM -> r < 0.03 ? rush() : r < 0.05 ? springcurl(c) : r < 0.12 && noise(dx * 3, dz * 3) > 0.55 ? carpet(c)
							: r < 0.13 ? shiftcap(c) : r < 0.135 ? root(c, false) : r < 0.14 ? ModWastes.VIGIL_BELL.get().defaultBlockState() : null;
						case HEAVY -> r < 0.3 && noise(dx * 3, dz * 3) > 0.5 ? carpet(c) : r < 0.33 ? rush() : r < 0.35 ? springcurl(c)
							: r < 0.37 ? shiftcap(c) : r < 0.38 ? root(c, false) : r < 0.39 ? root(c, true)
							: r < 0.41 ? ModWastes.KNOTVINE.get().defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true) : null;
						default -> null;
					};
				if (plant != null) {
					taken.add(c);
					p.surface(dx, dz, plant, floorOnly, cracks);
				}
			}
	}

	private static BlockState rush() {
		return ModWastes.CORKSCREW_RUSH.get().defaultBlockState();
	}

	private BlockState springcurl(BlockPos c) {
		return ModWastes.SPRINGCURL.get().defaultBlockState().setValue(SpringcurlBlock.GROWN, roll(c, 52) < 0.6);
	}

	/** In the heart's ring (no Shiftcaps there, D94; an outskirts-only site has no heart). */
	private boolean nearHeart(BlockPos c) {
		return !p.outskirtsOnly && Wear.ring(c.getX() - p.origin.getX(), c.getZ() - p.origin.getZ()) <= Wear.INTACT_TO;
	}

	@Nullable
	private BlockState shiftcap(BlockPos c) {
		if (nearHeart(c))
			return null;
		ShiftcapBlock.Age[] ages = ShiftcapBlock.Age.values();
		return ModWastes.SHIFTCAP.get().defaultBlockState().setValue(ShiftcapBlock.AGE, ages[(int) (roll(c, 53) * ages.length)]);
	}

	private BlockState root(BlockPos c, boolean run) {
		return (run ? ModWastes.BREAKROOT_RUN : ModWastes.BREAKROOT_KNUCKLE).get().defaultBlockState()
			.setValue(BreakrootBlock.AXIS, roll(c, 54) < 0.5 ? Direction.Axis.X : Direction.Axis.Z);
	}

	/** A Breakroot Burst out of a wall's foot (the wall toward {@code fromWall}'s opposite). */
	@Nullable
	private BlockState burst(BlockPos c, @Nullable Direction fromWall) {
		return fromWall == null ? null : ModWastes.BREAKROOT_BURST.get().defaultBlockState().setValue(BreakrootBurstBlock.FACING, fromWall);
	}

	/**
	 * Whether a burst at {@code c} cracks the brick it bursts from: a Stained End Stone Brick in the same chunk (the applier
	 * cracks it only if the burst grows, and reads nothing past the chunk it builds).
	 */
	private boolean cracks(BlockPos c, Direction fromWall) {
		BlockPos wall = c.relative(fromWall.getOpposite());
		BlockState w = world.get(wall);
		return w != null && w.is(ModWastes.STAINED_END_STONE_BRICKS.get()) && wall.getX() >> 4 == c.getX() >> 4 && wall.getZ() >> 4 == c.getZ() >> 4;
	}

	// ---------------------------------------------------------------- the trees

	private void trees() {
		int[] g = CityLayout.grapple(!p.outskirtsOnly);
		crookwood(g[0] + 13, g[1] + 1);
		// the likeliest shed it fits up through, or standing in the open
		for (BlockPos shed : burntSheds())
			if (shieldcrown(shed, roofTop(shed)))
				return;
		shieldcrown(p.at(p.outskirtsOnly ? -14 : 78, 0, p.outskirtsOnly ? 16 : 20), 0);
	}

	/** A tree's wood, where nothing else is (the plan's blocks, the never list, a prop's reach). */
	private void log(Block log, int x, int y, int z, Direction.Axis axis) {
		BlockPos w = p.at(x, y, z);
		if (!empty(w))
			return;
		taken.add(w);
		world.put(w, log.defaultBlockState());
		p.put(w, log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
	}

	/** A tree's leaf, where nothing else is (false: not put). */
	private boolean leaf(BlockPos w, BlockState s) {
		if (!empty(w))
			return false;
		taken.add(w);
		world.put(w, s);
		p.put(w, s.setValue(LeavesBlock.PERSISTENT, true));
		return true;
	}

	/**
	 * The Crookwood in the grapple (D92): a corkscrew trunk from between the resistance frame's hand and the uprising frame's
	 * lens (up three, a step west, up three, a step south, up three, a step east, up), branches at the top, four drooping pale
	 * crowns with curtains of strands, roots round its foot. As previewed (clockwork-wastes-overgrowth.gen.js).
	 */
	private void crookwood(int bx, int bz) {
		Block log = ModWastes.CROOKWOOD_LOG.get();
		int[][] trunk = { { 0, 0, 0, 1 }, { 0, 1, 0, 1 }, { 0, 2, 0, 1 }, { 0, 3, 0, 1 }, { -1, 3, 0, 0 }, { -1, 4, 0, 1 }, { -1, 5, 0, 1 }, { -1, 6, 0, 1 },
			{ -1, 6, 1, 2 }, { -1, 7, 1, 1 }, { -1, 8, 1, 1 }, { -1, 9, 1, 1 }, { 0, 9, 1, 0 }, { 0, 10, 1, 1 }, { 0, 11, 1, 1 }, { 0, 12, 1, 1 }, { 0, 13, 1, 1 },
			{ -1, 11, 1, 0 }, { -2, 11, 1, 0 }, { -3, 11, 1, 0 }, { -4, 11, 1, 0 }, { 1, 12, 1, 0 }, { 2, 12, 1, 0 }, { 3, 12, 1, 0 },
			{ 0, 12, 0, 2 }, { 0, 12, -1, 2 }, { 0, 12, -2, 2 } };
		Direction.Axis[] axes = { Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z };
		for (int[] t : trunk)
			log(log, bx + t[0], t[1], bz + t[2], axes[t[3]]);
		BlockState leaves = ModWastes.CROOKWOOD_LEAVES.get().defaultBlockState();
		double[][] crowns = { { -5, 12, 1, 2.6, 4 }, { 4, 13, 1, 2.3, 4 }, { 0, 13, -3, 2.2, 3 }, { 0, 14, 1, 1.8, 2 } };
		for (double[] cr : crowns) {
			int cx = bx + (int) cr[0], cy = (int) cr[1], cz = bz + (int) cr[2], drop = (int) cr[4];
			double r = cr[3];
			int span = (int) Math.ceil(r);
			for (int dy = 0; dy <= 1; dy++)
				for (int dx = -span; dx <= span; dx++)
					for (int dz = -span; dz <= span; dz++) {
						double rr = dy == 1 ? r - 1.4 : r;
						if (dx * dx + dz * dz <= rr * rr + 0.3)
							leaf(p.at(cx + dx, cy + dy, cz + dz), leaves);
					}
			// the willow's curtain: strands hanging from the crown's rim, a gap here and there
			for (int dx = -span; dx <= span; dx++)
				for (int dz = -span; dz <= span; dz++) {
					int d2 = dx * dx + dz * dz;
					if (d2 > r * r + 0.3 || d2 < (r - 1.3) * (r - 1.3) || roll(p.at(cx + dx, 0, cz + dz), 61) < 0.25)
						continue;
					int len = 1 + (int) (roll(p.at(cx + dx, 1, cz + dz), 62) * drop);
					// only under a leaf of the crown, each length under the last (a strand hangs from leaves, nothing else)
					if (!world.getOrDefault(p.at(cx + dx, cy, cz + dz), leaves).is(leaves.getBlock()) || !world.containsKey(p.at(cx + dx, cy, cz + dz)))
						continue;
					for (int i = 1; i <= len; i++) {
						BlockPos w = p.at(cx + dx, cy - i, cz + dz);
						// the strand's end where the next length down is taken
						boolean end = i == len || taken.contains(w.below()) || world.containsKey(w.below());
						if (!leaf(w, leaves.setValue(CrookwoodLeavesBlock.HANGING, end ? CrookwoodLeavesBlock.Hanging.END : CrookwoodLeavesBlock.Hanging.MID)) || end)
							break;
					}
				}
		}
		// roots round its foot (on the ground, wherever it is), moss over the frames' casing
		// (dx, dz from its foot; 0 a run, 1 a knuckle, 2 a burst; along x 0 or z 1)
		int[][] roots = { { 1, 0, 0, 1 }, { 1, 1, 0, 1 }, { 0, 2, 1, 1 }, { 1, -1, 2, 0 }, { 0, -2, 0, 0 }, { -3, 0, 1, 0 } };
		for (int[] rt : roots) {
			int x = bx + rt[0], z = bz + rt[1];
			BlockPos c = p.at(x, 0, z);
			if (!empty(c))
				continue;
			taken.add(c);
			BlockState s = rt[2] == 2 ? ModWastes.BREAKROOT_BURST.get().defaultBlockState()
				: (rt[2] == 0 ? ModWastes.BREAKROOT_RUN : ModWastes.BREAKROOT_KNUCKLE).get().defaultBlockState().setValue(BreakrootBlock.AXIS, rt[3] == 0 ? Direction.Axis.X : Direction.Axis.Z);
			p.surface(x, z, s, false);
		}
	}

	/** A makers' shed or workshop in the damaged ring for the Shieldcrown: their middle columns (at the floor), the likeliest first. */
	private List<BlockPos> burntSheds() {
		List<BlockPos> out = new ArrayList<>();
		List<Integer> scores = new ArrayList<>();
		for (Object o : p.buildings)
			if (o instanceof KitCity.Placement pl && (pl.what().startsWith("shed") || pl.what().equals("small shed") || pl.what().startsWith("long workshop"))) {
				int[] b = pl.model().bounds();
				BlockPos mid = pl.site().at((b[0] + b[3]) / 2, 0, (b[2] + b[5]) / 2);
				int ring = Wear.ring(mid.getX() - p.origin.getX(), mid.getZ() - p.origin.getZ());
				if (ring <= Wear.WORN_TO || ring > Wear.DAMAGED_TO)
					continue;
				// (a stable order: the plan's, among equals)
				int score = Math.abs(ring - 58), i = 0;
				while (i < scores.size() && scores.get(i) <= score)
					i++;
				scores.add(i, score);
				out.add(i, mid);
			}
		return out;
	}

	/** The highest block the plan puts over a shed's middle (its roof), from the floor. */
	private int roofTop(BlockPos mid) {
		int top = 0;
		for (int dx = -2; dx <= 2; dx++)
			for (int dz = -2; dz <= 2; dz++)
				for (int y = 1; y < 24; y++)
					if (world.containsKey(mid.offset(dx, y, dz)))
						top = Math.max(top, y);
		return top;
	}

	/** In the burnt-out roof over a shed's middle (what the Shieldcrown breaks through: never a never-list block). */
	private boolean burnt(BlockPos mid, int roof, BlockPos w) {
		int y = w.getY() - mid.getY();
		return roof > 0 && Math.abs(w.getX() - mid.getX()) <= 2 && Math.abs(w.getZ() - mid.getZ()) <= 2 && y >= 2 && y <= roof && world.containsKey(w)
			&& !clear.contains(w) && !taken.contains(w);
	}

	/**
	 * The Shieldcrown (D92): up through the burnt-out roof of the shed at {@code mid} (the roof over its middle 5 x 5 gone), a
	 * straight braided stem, four branches stepping out at about 70% of it, a flat round crown 11 across and 2 thick 1-3 blocks
	 * over the broken roof line. {@code roof}: the shed's roof top (0: none, standing in the open). Only where its stem and
	 * branches are clear of everything but that roof (false: not grown).
	 */
	private boolean shieldcrown(BlockPos mid, int roof) {
		int h = Math.max(11, roof + 1);
		int bx = mid.getX() - p.origin.getX(), bz = mid.getZ() - p.origin.getZ();
		List<BlockPos> wood = new ArrayList<>();
		for (int y = 0; y <= h; y++)
			wood.add(p.at(bx, y, bz));
		for (Direction d : Direction.Plane.HORIZONTAL)
			for (int i = 1; i <= 3; i++)
				wood.add(p.at(bx + d.getStepX() * i, h - 3 + i, bz + d.getStepZ() * i));
		for (BlockPos w : wood)
			if (!empty(w) && !burnt(mid, roof, w))
				return false;
		if (roof > 0)
			for (int dx = -2; dx <= 2; dx++)
				for (int dz = -2; dz <= 2; dz++)
					for (int y = 2; y <= roof; y++) {
						BlockPos w = mid.offset(dx, y, dz);
						if (burnt(mid, roof, w)) {
							world.remove(w);
							p.put(w, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
						}
					}
		Block log = ModWastes.SHIELDCROWN_LOG.get();
		for (BlockPos w : wood)
			log(log, w.getX() - p.origin.getX(), w.getY() - p.origin.getY(), w.getZ() - p.origin.getZ(), Direction.Axis.Y);
		BlockState leaves = ModWastes.SHIELDCROWN_LEAVES.get().defaultBlockState();
		double[][] layers = { { h + 1, 5.3 }, { h + 2, 4.3 } };
		for (double[] l : layers)
			for (int dx = -6; dx <= 6; dx++)
				for (int dz = -6; dz <= 6; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= l[1])
						leaf(p.at(bx + dx, (int) l[0], bz + dz), leaves.setValue(ShieldcrownLeavesBlock.RIM, d > l[1] - 1.1));
				}
		return true;
	}

	// ---------------------------------------------------------------- the alcoves' vines

	/** The Charging Alcoves' dead vines turn to living Knotvine where the overgrowth is heavy (D92). */
	private void alcoves() {
		Set<BlockPos> living = new HashSet<>();
		for (var e : world.entrySet())
			if (e.getValue().getBlock() instanceof ChargingAlcoveBlock && e.getValue().getValue(ThreePartBlock.PART) == ThreePartBlock.Part.BOTTOM
				&& e.getValue().getValue(ChargingAlcoveBlock.VINES) == ChargingAlcoveBlock.Vines.DEAD && tier(e.getKey()) == Tier.HEAVY)
				for (int k = 0; k < 3; k++)
					living.add(e.getKey().above(k));
		if (!living.isEmpty())
			p.restate(put -> living.contains(put.at()) && put.state().getBlock() instanceof ChargingAlcoveBlock,
				s -> s.setValue(ChargingAlcoveBlock.VINES, ChargingAlcoveBlock.Vines.LIVING));
	}
}
