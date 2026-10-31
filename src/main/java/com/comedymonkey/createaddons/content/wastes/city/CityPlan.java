package com.comedymonkey.createaddons.content.wastes.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.wastes.AssemblyClampBlock;
import com.comedymonkey.createaddons.content.wastes.BrassPlaqueBlock;
import com.comedymonkey.createaddons.content.wastes.ChargingAlcoveBlock;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlock;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictCableBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictStarReactorBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictWallLampBlock;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.LiftBlock;
import com.comedymonkey.createaddons.content.wastes.LiftCallPlateBlock;
import com.comedymonkey.createaddons.content.wastes.SlidingDoorBlock;
import com.comedymonkey.createaddons.content.wastes.SubstationBlock;
import com.comedymonkey.createaddons.content.wastes.ThreePartBlock;
import com.comedymonkey.createaddons.content.wastes.WallVentBlock;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * A Clockwork Wastes city written down before it's built (design/specs/clockwork-wastes.md, D21, P3, P4, P32, D66): every
 * block, binding, span and bot as an {@link Op}, in the order they're built, keyed by the chunk each lands in. The plan
 * depends only on the city's origin and seed, never on the ground, so world generation can build it a chunk at a time
 * (the same plan every time) and the dev command can build all of it at once; what the ground does to it (cut into a
 * hill, filled down to a dip, decked and strutted over the void) is decided where it's built ({@link Op.Ground}).
 * {@link CityLayout} draws the city into it.
 */
public final class CityPlan {
	/** Everything the city does, in order. */
	public sealed interface Op {
		/** The block (or entity position) it lands in. */
		BlockPos at();

		/**
		 * A block, bound to {@code district} if it's a city block (null: ordinary). {@code need}: {@link #ANYWHERE}, only where
		 * the column has ground near the city's floor ({@link #ON_GROUND}: everything in the outskirts, so wrecks never float),
		 * or only where it has none ({@link #OVER_VOID}: a bridge's rails, a snapped bridge hanging).
		 */
		record Put(BlockPos at, BlockState state, @Nullable District district, int need) implements Op {}

		/**
		 * One column of the city's ground: its top at {@code y - 1} (Scorched End Stone); hills cut down to it, dips filled up to
		 * it; over the void (or far below) a void-alloy deck, on a trestle where {@code trestle}. Outskirts ({@code core} false)
		 * only scorch ground that's there.
		 */
		record Ground(BlockPos at, boolean core, boolean trestle, boolean deck) implements Op {}

		/** A sky-cable span kept by the insulator at {@code at}, to {@code other}; the bead runs it at {@code stage}. */
		record Span(BlockPos at, BlockPos other, int stage, boolean fromHere) implements Op {}

		/** A snapped span hanging from the insulator at {@code at}. */
		record Snapped(BlockPos at, float heading) implements Op {}

		/** An insulator's place in its district's chain. */
		record Hop(BlockPos at, int hop) implements Op {}

		/** A dormant Scuttler. */
		record Scuttler(BlockPos at, Vec3 pos, District district) implements Op {}

		/** A dormant Sentry on {@code face} of {@code wall} ({@code at}: the block it sits in). */
		record Sentry(BlockPos at, BlockPos wall, Direction face, District district) implements Op {}

		/** The Custodian kneeling on its dais. */
		record Custodian(BlockPos at, float yaw, List<BlockPos> pylons, BlockPos vault) implements Op {}

		/**
		 * A loot table for the container put at {@code at} (it rolls when it's first opened, as a vanilla structure's chest
		 * does); nothing if no container stands there (an outskirts {@link Put} over the void).
		 */
		record Loot(BlockPos at, ResourceKey<LootTable> table, long seed) implements Op {}
	}

	public static final int ANYWHERE = 0, ON_GROUND = 1, OVER_VOID = 2;

	public final BlockPos origin;
	public final long seed;
	public final boolean outskirtsOnly;
	/** The city's buildings come from the building kit (D71), not the old boxes: {@link CityLayout#BUILDING_KIT}. */
	boolean kit;
	private final List<Op> ops = new ArrayList<>();
	/** The cables, set last with their arms worked out from what's beside them (so chunk edges can't break a run). */
	private final Map<BlockPos, Op.Put> cables = new LinkedHashMap<>();
	/** What cables join (Substation feet, lamp feet, other cables), for working out their arms. */
	private final Map<BlockPos, BlockState> joins = new HashMap<>();
	private Map<Long, List<Op>> byChunk;
	/** The district blocks are bound to as they're put (null: none). */
	@Nullable
	District district;

	CityPlan(BlockPos origin, long seed, boolean outskirtsOnly) {
		this.origin = origin.immutable();
		this.seed = seed;
		this.outskirtsOnly = outskirtsOnly;
	}

	/** The whole plan of the city at {@code origin}. */
	public static CityPlan of(BlockPos origin, long seed, boolean outskirtsOnly) {
		return of(origin, seed, outskirtsOnly, CityLayout.BUILDING_KIT);
	}

	/**
	 * The plan with the city's buildings as the building kit's ({@code kit}, D71) or as the old boxes; {@link #of(BlockPos,
	 * long, boolean)} takes {@link CityLayout#BUILDING_KIT}.
	 */
	public static CityPlan of(BlockPos origin, long seed, boolean outskirtsOnly, boolean kit) {
		CityPlan plan = new CityPlan(origin, seed, outskirtsOnly);
		plan.kit = kit;
		CityLayout.draw(plan);
		plan.finish();
		return plan;
	}

	/**
	 * A plan drawn by {@code draw} alone (no city round it), its cables finished as a city's are: the building kit's
	 * catalogue ({@code /wastes buildcatalogue}, D71) is drawn this way. Never outskirts-only: everything in it is core.
	 */
	public static CityPlan drawn(BlockPos origin, long seed, java.util.function.Consumer<CityPlan> draw) {
		CityPlan plan = new CityPlan(origin, seed, false);
		draw.accept(plan);
		plan.district = null;
		plan.finish();
		return plan;
	}

	/** The district blocks put from now on are bound to (null: none). */
	public void bindTo(@Nullable District d) {
		district = d;
	}

	@Nullable
	public District boundTo() {
		return district;
	}

	/**
	 * The building kit's buildings in this plan (D71, design/briefs/clockwork-wastes-buildings.md), as each was put: what it
	 * is, its state, its world footprint and its local model, for the plan tests. Empty while the city keeps its boxes.
	 */
	public final List<Object> buildings = new ArrayList<>();

	public List<Op> ops() {
		return ops;
	}

	/** The ops landing in one chunk, in order. */
	public List<Op> opsIn(ChunkPos chunk) {
		if (byChunk == null) {
			Map<Long, List<Op>> map = new HashMap<>();
			for (Op op : ops)
				map.computeIfAbsent(ChunkPos.asLong(op.at().getX() >> 4, op.at().getZ() >> 4), k -> new ArrayList<>()).add(op);
			byChunk = map;
		}
		return byChunk.getOrDefault(chunk.toLong(), List.of());
	}

	public RandomSource random(int salt) {
		return RandomSource.create(seed * 31 + salt * 0x9E3779B97F4A7C15L + origin.asLong());
	}

	// ---------------------------------------------------------------- recording

	/** Relative to the city's origin (the middle of the heart, at its floor). */
	public BlockPos at(int x, int y, int z) {
		return origin.offset(x, y, z);
	}

	public void put(BlockPos p, BlockState s) {
		put(p, s, inCore(p) ? ANYWHERE : ON_GROUND);
	}

	/**
	 * In the city's core (levelled, decked over the void), not the outskirts. An outskirts-only site has no core: none of it
	 * is levelled or decked, so everything in it stands only on ground, however near the middle (P46).
	 */
	public boolean inCore(BlockPos p) {
		return !outskirtsOnly && Math.abs(p.getX() - origin.getX()) <= CityLayout.CORE && Math.abs(p.getZ() - origin.getZ()) <= CityLayout.CORE;
	}

	public void put(BlockPos p, BlockState s, int need) {
		ops.add(new Op.Put(p.immutable(), s, district, need));
		if (s.getBlock() instanceof SubstationBlock || s.getBlock() instanceof DerelictStreetLampBlock)
			joins.put(p.immutable(), s);
		else
			joins.remove(p);
	}

	public void put(int x, int y, int z, BlockState s) {
		put(at(x, y, z), s);
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
					put(x, y, z, s);
	}

	public void ground(int x, int z, boolean core, boolean trestle, boolean deck) {
		ops.add(new Op.Ground(at(x, 0, z), core, trestle, deck));
	}

	// ---------------------------------------------------------------- blocks by name (the makers' and Create's)

	private static final Map<String, Block> NAMED = new java.util.concurrent.ConcurrentHashMap<>();

	/** A block by id, or {@code fallback} if that mod isn't there. */
	public static BlockState block(String id, Block fallback) {
		Block b = NAMED.computeIfAbsent(id, k -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(k)).orElse(fallback));
		return b.defaultBlockState();
	}

	/** {@code state} with a property set by name, if it has one by that name and the value fits. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static BlockState with(BlockState state, String name, String value) {
		for (Property<?> p : state.getProperties())
			if (p.getName().equals(name)) {
				var v = p.getValue(value);
				if (v.isPresent())
					return state.setValue((Property) p, (Comparable) v.get());
			}
		return state;
	}

	// ---------------------------------------------------------------- the city's blocks

	public void cable(BlockPos p, Direction surface) {
		BlockState s = ModWastes.DERELICT_CABLE.get().defaultBlockState().setValue(DerelictCableBlock.SURFACE, surface);
		Op.Put op = new Op.Put(p.immutable(), s, district, inCore(p) ? ANYWHERE : ON_GROUND);
		cables.remove(p);
		cables.put(p.immutable(), op);
		joins.put(p.immutable(), s);
	}

	/** A street lamp post three tall: its crook toward {@code crook}; leaning (D41) and snapped as asked. */
	public void streetLamp(BlockPos foot, Direction crook, boolean leaning, boolean snapped) {
		BlockState post = ModWastes.DERELICT_STREET_LAMP.get().defaultBlockState().setValue(DerelictStreetLampBlock.FACING, crook)
			.setValue(DerelictStreetLampBlock.LEANING, leaning);
		put(foot, post.setValue(DerelictStreetLampBlock.PART, DerelictStreetLampBlock.Part.FOOT));
		put(foot.above(), post.setValue(DerelictStreetLampBlock.PART, DerelictStreetLampBlock.Part.POLE));
		put(foot.above(2), post.setValue(DerelictStreetLampBlock.PART, DerelictStreetLampBlock.Part.HEAD).setValue(DerelictStreetLampBlock.SNAPPED, snapped));
	}

	public void wallLamp(BlockPos p, Direction facing) {
		put(p, ModWastes.DERELICT_WALL_LAMP.get().defaultBlockState().setValue(DerelictWallLampBlock.FACING, facing));
	}

	public void substation(BlockPos lower, Direction facing) {
		BlockState s = ModWastes.SUBSTATION.get().defaultBlockState().setValue(SubstationBlock.FACING, facing);
		put(lower, s);
		put(lower.above(), s.setValue(SubstationBlock.HALF, DoubleBlockHalf.UPPER));
	}

	public void vault(BlockPos p, Direction facing, boolean heart) {
		put(p, ModWastes.CITY_VAULT.get().defaultBlockState().setValue(CityVaultBlock.FACING, facing).setValue(CityVaultBlock.HEART, heart));
	}

	/** A 2 x 3 sliding door, its controller at {@code controller}, the other column toward the facing's clockwise side. */
	public void door(BlockPos controller, Direction facing, boolean heart) {
		BlockState d = ModWastes.SLIDING_DOOR.get().defaultBlockState().setValue(SlidingDoorBlock.FACING, facing).setValue(SlidingDoorBlock.HEART, heart);
		for (int c = 0; c < 2; c++)
			for (int r = 0; r < 3; r++)
				put(SlidingDoorBlock.part(controller, facing, c, r), d.setValue(SlidingDoorBlock.COL, c).setValue(SlidingDoorBlock.ROW, r));
	}

	/** A door across the two blocks {@code a} and {@code b} (side by side), facing {@code facing}: its controller worked out. */
	public void doorAcross(BlockPos a, BlockPos b, Direction facing, boolean heart) {
		BlockPos controller = SlidingDoorBlock.part(a, facing, 1, 0).equals(b) ? a : b;
		door(controller, facing, heart);
	}

	public void threePart(Block block, BlockPos bottom, Direction facing, boolean lying) {
		BlockState s = block.defaultBlockState().setValue(ThreePartBlock.FACING, facing);
		for (int i = 0; i < 3; i++) {
			BlockPos p = lying ? bottom.relative(facing.getClockWise(), i) : bottom.above(i);
			put(p, s.setValue(ThreePartBlock.PART, ThreePartBlock.Part.values()[i]));
		}
	}

	/** A Charging Alcove with no vines (it may get a trinket: {@link #alcove(BlockPos, Direction, boolean)}). */
	public void alcove(BlockPos bottom, Direction open) {
		alcove(bottom, open, false);
	}

	/**
	 * Alcove trinkets (D72): one alcove in each {@code TRINKET_EVERY} the plan puts holds one, at a place in those five and
	 * of a kind drawn from the plan's seed (so the same plan always puts the same trinkets in the same alcoves).
	 */
	public static final int TRINKET_EVERY = 5;
	private static final int TRINKET_SALT = 7201;
	@Nullable
	private RandomSource trinkets;
	private int alcoves, trinketSlot;

	/** The trinket the next alcove put holds (NONE in four of each five). */
	private ChargingAlcoveBlock.Trinket nextTrinket() {
		if (trinkets == null)
			trinkets = random(TRINKET_SALT);
		int k = alcoves++ % TRINKET_EVERY;
		if (k == 0)
			trinketSlot = trinkets.nextInt(TRINKET_EVERY);
		return k == trinketSlot ? ChargingAlcoveBlock.Trinket.KINDS[trinkets.nextInt(ChargingAlcoveBlock.Trinket.KINDS.length)] : ChargingAlcoveBlock.Trinket.NONE;
	}

	/** A Charging Alcove three tall, opening toward {@code open}; dead vines on it if asked (D72), a trinket by the plan's draw. */
	public void alcove(BlockPos bottom, Direction open, boolean vines) {
		BlockState s = ModWastes.CHARGING_ALCOVE.get().defaultBlockState().setValue(ThreePartBlock.FACING, open)
			.setValue(ChargingAlcoveBlock.TRINKET, nextTrinket()).setValue(ChargingAlcoveBlock.VINES, vines);
		for (int i = 0; i < 3; i++)
			put(bottom.above(i), s.setValue(ThreePartBlock.PART, ThreePartBlock.Part.values()[i]));
	}

	/** An Assembly Clamp (D72) holding a Worker at {@code stage} (1-5), its front toward {@code facing}. */
	public void clamp(BlockPos p, Direction facing, int stage) {
		put(p, ModWastes.ASSEMBLY_CLAMP.get().defaultBlockState().setValue(AssemblyClampBlock.FACING, facing).setValue(AssemblyClampBlock.STAGE, stage));
	}

	/** A Brass Plaque (D72) on the wall behind {@code p}, its face toward {@code facing}; hanging from one bolt if {@code askew}. */
	public void plaque(BlockPos p, Direction facing, boolean askew) {
		put(p, ModWastes.BRASS_PLAQUE.get().defaultBlockState().setValue(BrassPlaqueBlock.FACING, facing).setValue(BrassPlaqueBlock.ASKEW, askew));
	}

	/**
	 * A mast riser (D72): Derelict Cable up the side of a girder column {@code height} tall, in the cells beside it from
	 * {@code foot} up, the girders toward {@code girderSide}; its foot joins the floor cable beside it (worked out last, with
	 * the other cables).
	 */
	public void riser(BlockPos foot, Direction girderSide, int height) {
		BlockState s = ModWastes.DERELICT_CABLE.get().defaultBlockState().setValue(DerelictCableBlock.SURFACE, girderSide);
		for (int y = 0; y < height; y++) {
			BlockPos p = foot.above(y).immutable();
			DerelictCableBlock.Girder part = y == 0 ? DerelictCableBlock.Girder.FOOT : y == height - 1 ? DerelictCableBlock.Girder.TOP : DerelictCableBlock.Girder.RUN;
			BlockState r = s.setValue(DerelictCableBlock.GIRDER, part);
			cables.remove(p);
			cables.put(p, new Op.Put(p, r, district, inCore(p) ? ANYWHERE : ON_GROUND));
			joins.put(p, r);
		}
	}

	public void statue(BlockPos bottom, Direction facing) {
		threePart(ModWastes.MAKER_STATUE.get(), bottom, facing, false);
	}

	public void toppledStatue(BlockPos foot, Direction facing) {
		threePart(ModWastes.TOPPLED_MAKER_STATUE.get(), foot, facing, true);
	}

	public void pylon(BlockPos bottom) {
		BlockState s = ModWastes.CONDUIT_PYLON.get().defaultBlockState();
		put(bottom, s.setValue(ThreePartBlock.PART, ThreePartBlock.Part.BOTTOM));
		put(bottom.above(), s.setValue(ThreePartBlock.PART, ThreePartBlock.Part.MIDDLE));
		put(bottom.above(2), s.setValue(ThreePartBlock.PART, ThreePartBlock.Part.TOP));
	}

	/** The heart's Derelict Star Reactor, 3 x 3 x 3 round {@code controller} (its bottom middle). */
	public void reactor(BlockPos controller) {
		BlockState s = ModWastes.DERELICT_STAR_REACTOR.get().defaultBlockState();
		for (int p = 0; p < 27; p++)
			put(DerelictStarReactorBlock.partAt(controller, p), s.setValue(DerelictStarReactorBlock.PART, p));
	}

	public void vent(BlockPos p, Direction into) {
		put(p, ModWastes.WALL_VENT.get().defaultBlockState().setValue(WallVentBlock.FACING, into));
	}

	public void insulator(BlockPos p, DerelictInsulatorBlock.Mount mount, Direction facing) {
		put(p, ModWastes.DERELICT_INSULATOR.get().defaultBlockState().setValue(DerelictInsulatorBlock.MOUNT, mount).setValue(DerelictInsulatorBlock.FACING, facing));
	}

	public void span(BlockPos owner, BlockPos other, int stage, boolean fromHere) {
		ops.add(new Op.Span(owner.immutable(), other.immutable(), stage, fromHere));
	}

	public void snapped(BlockPos owner, float heading) {
		ops.add(new Op.Snapped(owner.immutable(), heading));
	}

	public void hop(BlockPos ins, int hop) {
		ops.add(new Op.Hop(ins.immutable(), hop));
	}

	/** A Lift at the head of its shaft (never mirrored, D62: the shaft runs toward the facing's clockwise side and back). */
	public void lift(BlockPos head, Direction facing) {
		put(head, ModWastes.LIFT.get().defaultBlockState().setValue(LiftBlock.FACING, facing));
	}

	public void callPlate(BlockPos p, Direction facing) {
		put(p, ModWastes.LIFT_CALL_PLATE.get().defaultBlockState().setValue(LiftCallPlateBlock.FACING, facing));
	}

	public void pillar(BlockPos p, Direction.Axis axis) {
		put(p, ModWastes.VOID_ALLOY_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
	}

	public void scuttler(BlockPos block) {
		ops.add(new Op.Scuttler(block.immutable(), Vec3.atBottomCenterOf(block), district));
	}

	public void sentry(BlockPos wall, Direction face) {
		ops.add(new Op.Sentry(wall.relative(face).immutable(), wall.immutable(), face, district));
	}

	/** A loot table for the container just put at {@code p}. */
	public void loot(BlockPos p, ResourceKey<LootTable> table) {
		ops.add(new Op.Loot(p.immutable(), table, random(p.hashCode()).nextLong()));
	}

	public void custodian(BlockPos dais, float yaw, List<BlockPos> pylons, BlockPos vault) {
		ops.add(new Op.Custodian(dais.immutable(), yaw, List.copyOf(pylons), vault.immutable()));
	}

	// ---------------------------------------------------------------- finishing

	/** The cables last, each with its arms toward what it joins, as its block would work them out beside them. */
	private void finish() {
		DerelictCableBlock cable = ModWastes.DERELICT_CABLE.get();
		BlockGetter around = new BlockGetter() {
			@Override
			public BlockEntity getBlockEntity(BlockPos pos) {
				return null;
			}

			@Override
			public BlockState getBlockState(BlockPos pos) {
				return joins.getOrDefault(pos, Blocks.AIR.defaultBlockState());
			}

			@Override
			public FluidState getFluidState(BlockPos pos) {
				return Fluids.EMPTY.defaultFluidState();
			}

			@Override
			public int getHeight() {
				return 384;
			}

			@Override
			public int getMinBuildHeight() {
				return -64;
			}
		};
		for (Op.Put c : cables.values())
			ops.add(new Op.Put(c.at(), cable.connected(c.state(), around, c.at()), c.district(), c.need()));
		cables.clear();
		joins.clear();
	}
}
