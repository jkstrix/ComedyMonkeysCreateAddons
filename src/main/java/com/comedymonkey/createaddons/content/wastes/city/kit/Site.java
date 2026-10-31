package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.LiftBlock;
import com.comedymonkey.createaddons.content.wastes.SlottedGrilleBlock;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Where a kit building stands: a local frame in a {@link CityPlan}. Local x runs along the building (its outer end, the end
 * farthest from the heart, at the high x: damage starts there), local z runs back from its front (the front face is the
 * z = 0 wall, looking local north, -z), y up from the floor (y 0 stands on the ground at y -1). {@code a} and {@code b} are
 * the world directions of local +x and +z: any of the eight turns and mirrors, so a building drawn once fits a district's
 * mirrored frame or the catalogue's row alike. Blocks are turned into the world by their {@code facing}, {@code axis}, side
 * and hinge properties; stairs, bars, walls, fences, pipes and bands then join up with what's round them in the world
 * ({@code CityApplier}).
 * <p>
 * {@code heart} is the world position of the city's middle (or the catalogue's stand-in for it): which faces are outward
 * and which way fallen tops lie are worked out from it.
 */
public final class Site {
	public final CityPlan plan;
	public final BlockPos origin;
	public final Direction a, b;
	public final BlockPos heart;
	/** The district its city blocks are bound to (null: none). */
	@Nullable
	public final District district;

	public Site(CityPlan plan, BlockPos origin, Direction a, Direction b, BlockPos heart, @Nullable District district) {
		if (a.getAxis() == b.getAxis() || !a.getAxis().isHorizontal() || !b.getAxis().isHorizontal())
			throw new IllegalArgumentException("a site's axes must be two different horizontal axes: " + a + ", " + b);
		this.plan = plan;
		this.origin = origin.immutable();
		this.a = a;
		this.b = b;
		this.heart = heart.immutable();
		this.district = district;
	}

	/**
	 * A site inside this one: its origin at local (x, 0, z), its local +x and +z along this site's local {@code la} and
	 * {@code lb} (any turn or mirror), the same heart and district.
	 */
	public Site sub(int x, int z, Direction la, Direction lb) {
		return new Site(plan, at(x, 0, z), w(la), w(lb), heart, district);
	}

	/** The same frame bound to another district (the heart's spire on a district's catalogue row, say). */
	public Site boundTo(@Nullable District d) {
		return new Site(plan, origin, a, b, heart, d);
	}

	/** Its frame turned or mirrored the other way (local x and z swapped would be a different building; this keeps x along). */
	public boolean mirrored() {
		return b != a.getClockWise();
	}

	/** A quarter turn from the local frame (x and z swap axes in the world). */
	public boolean quarter() {
		return a.getAxis() == Direction.Axis.Z;
	}

	public BlockPos at(int x, int y, int z) {
		return origin.offset(a.getStepX() * x + b.getStepX() * z, y, a.getStepZ() * x + b.getStepZ() * z);
	}

	/** A local direction in the world (local east is +x, south is +z). */
	public Direction w(Direction local) {
		return switch (local) {
			case EAST -> a;
			case WEST -> a.getOpposite();
			case SOUTH -> b;
			case NORTH -> b.getOpposite();
			default -> local;
		};
	}

	/** A world direction in the local frame. */
	public Direction local(Direction world) {
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (w(d) == world)
				return d;
		return world;
	}

	/** Whether the local face {@code side} looks away from the heart (an outward face: the war came from outside). */
	public boolean outward(Direction side) {
		Direction d = w(side);
		BlockPos mid = at(0, 0, 0);
		int dx = mid.getX() - heart.getX(), dz = mid.getZ() - heart.getZ();
		return d.getStepX() * dx + d.getStepZ() * dz > 0;
	}

	/** The local horizontal direction toward the heart along x (fallen tops lie this way; the outer end is the other). */
	public Direction towardHeart() {
		return outward(Direction.EAST) ? Direction.WEST : Direction.EAST;
	}

	private static final Map<Direction, String> SIDE = Map.of(Direction.NORTH, "north", Direction.EAST, "east", Direction.SOUTH, "south", Direction.WEST, "west");

	/** A block state drawn in the local frame, turned into the world. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BlockState orient(BlockState s) {
		BlockState out = s;
		for (Property<?> p : s.getProperties()) {
			if (p instanceof DirectionProperty dp) {
				Direction d = s.getValue(dp);
				if (d.getAxis().isHorizontal() && dp.getPossibleValues().contains(w(d)))
					out = out.setValue(dp, w(d));
			} else if (p.getValueClass() == Direction.Axis.class && quarter()) {
				Direction.Axis ax = (Direction.Axis) s.getValue(p);
				if (ax != Direction.Axis.Y) {
					Direction.Axis to = ax == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
					if (((Property) p).getPossibleValues().contains(to))
						out = out.setValue((Property) p, to);
				}
			} else if (p == SlottedGrilleBlock.AXIS && quarter()) {
				out = out.setValue(SlottedGrilleBlock.AXIS, s.getValue(SlottedGrilleBlock.AXIS).turned());
			} else if (p == DoorBlock.HINGE && mirrored()) {
				out = out.setValue(DoorBlock.HINGE, s.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
			}
		}
		// side properties (bars, fences, walls, pipes, band joins): each side's value goes to its turned side
		for (Direction d : Direction.Plane.HORIZONTAL) {
			Property<?> from = prop(s, SIDE.get(d));
			Property<?> to = prop(s, SIDE.get(w(d)));
			if (from != null && to != null && from.getValueClass() == to.getValueClass())
				out = out.setValue((Property) to, (Comparable) s.getValue(from));
		}
		return out;
	}

	@Nullable
	private static Property<?> prop(BlockState s, String name) {
		for (Property<?> p : s.getProperties())
			if (p.getName().equals(name))
				return p;
		return null;
	}

	// ---------------------------------------------------------------- putting

	public void put(int x, int y, int z, BlockState local) {
		District before = plan.boundTo();
		plan.bindTo(district);
		plan.put(at(x, y, z), orient(local));
		plan.bindTo(before);
	}

	/** Runs {@code r} with the plan binding to this site's district. */
	public void bound(Runnable r) {
		District before = plan.boundTo();
		plan.bindTo(district);
		r.run();
		plan.bindTo(before);
	}

	// ---------------------------------------------------------------- the city's multi-block pieces, placed in world terms

	/** A 2 x 3 sliding door across local (x0, z) and (x0 + 1, z), its face toward local {@code facing}. */
	public void door(int x0, int y, int z, Direction facing, boolean heartDoor) {
		bound(() -> plan.doorAcross(at(x0, y, z), at(x0 + 1, y, z), w(facing), heartDoor));
	}

	/** A sliding door across two local cells (any pair side by side), facing {@code facing}. */
	public void doorAcross(int x0, int z0, int x1, int z1, Direction facing, boolean heartDoor) {
		bound(() -> plan.doorAcross(at(x0, 0, z0), at(x1, 0, z1), w(facing), heartDoor));
	}

	public void alcove(int x, int y, int z, Direction open, boolean vines) {
		bound(() -> plan.alcove(at(x, y, z), w(open), vines));
	}

	public void plaque(int x, int y, int z, Direction facing, boolean askew) {
		bound(() -> plan.plaque(at(x, y, z), w(facing), askew));
	}

	public void cable(int x, int y, int z, Direction surface) {
		bound(() -> plan.cable(at(x, y, z), w(surface)));
	}

	public BlockPos insulator(int x, int y, int z, DerelictInsulatorBlock.Mount mount, Direction facing) {
		BlockPos p = at(x, y, z);
		bound(() -> plan.insulator(p, mount, w(facing)));
		return p;
	}

	public void riser(int x, int y, int z, Direction girderSide, int height) {
		bound(() -> plan.riser(at(x, y, z), w(girderSide), height));
	}

	public void vault(int x, int y, int z, Direction facing) {
		bound(() -> plan.vault(at(x, y, z), w(facing), false));
	}

	public void callPlate(int x, int y, int z, Direction facing) {
		bound(() -> plan.callPlate(at(x, y, z), w(facing)));
	}

	public void wallLamp(int x, int y, int z, Direction facing) {
		bound(() -> plan.wallLamp(at(x, y, z), w(facing)));
	}

	public void clamp(int x, int y, int z, Direction facing, int stage) {
		bound(() -> plan.clamp(at(x, y, z), w(facing), stage));
	}

	/**
	 * A Lift over the 2 x 2 shaft whose local cells are (x, z) to (x + 1, z + 1), its head {@code head} up, open toward local
	 * {@code open} and away from it; the Lift picks where its shaft runs (never mirrored, D62), so its head goes in whichever
	 * corner covers exactly these four columns. Returns its head, or null if no corner fits.
	 */
	@Nullable
	public BlockPos lift(int x, int z, int head, Direction open) {
		List<BlockPos> cells = List.of(at(x, 0, z), at(x + 1, 0, z), at(x, 0, z + 1), at(x + 1, 0, z + 1));
		Direction wo = w(open);
		for (BlockPos c : cells) {
			boolean all = true;
			for (BlockPos f : LiftBlock.footprint(c, wo))
				all &= cells.contains(f);
			if (all) {
				BlockPos h = c.above(head);
				bound(() -> plan.lift(h, wo));
				return h;
			}
		}
		return null;
	}
}
