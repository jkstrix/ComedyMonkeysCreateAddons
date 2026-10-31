package com.comedymonkey.createaddons.content.wastes;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The void-alloy base surface (D75, option C): an ashlar bond of 1 x 1 and 2 x 1 plates in offset courses, picked by
 * position. Each side face of a bonded block shows a 1 x 1 plate ({@code s}) or the left or right half of a 2 x 1 plate
 * ({@code l}, {@code r}); along a course the plates run l r s l r, and each course is shifted (by 0, 3, 1, 4, 2 for the
 * courses y mod 5), exactly as design/models/clockwork-wastes-building-blocks.gen.js's {@code OPTS.C} lays them. A
 * block keeps its faces in {@link #NS} (north's then south's) and {@link #EW} (east's then west's), set from its position
 * when it's placed: by a player ({@code getStateForPlacement}), by the city's builder ({@link #at}) and on any other
 * placing in a live level (a command, a contraption, a piston: {@link #follow} from {@code onPlace}). Tops and bottoms
 * are always 1 x 1.
 */
public final class VoidAlloyBond {
	/** A pair of sides' plates: {@code s} a 1 x 1, {@code l} / {@code r} the left / right half of a 2 x 1, as the viewer facing it sees it. */
	public enum Pair implements StringRepresentable {
		SS, SL, SR, LS, LL, LR, RS, RL, RR;

		public char first() {
			return Character.toLowerCase(name().charAt(0));
		}

		public char second() {
			return Character.toLowerCase(name().charAt(1));
		}

		public static Pair of(char first, char second) {
			return valueOf(("" + first + second).toUpperCase(Locale.ROOT));
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** The north face's plate, then the south face's. */
	public static final EnumProperty<Pair> NS = EnumProperty.create("bond_ns", Pair.class);
	/** The east face's plate, then the west face's. */
	public static final EnumProperty<Pair> EW = EnumProperty.create("bond_ew", Pair.class);

	/** A course: 2 x 1, 1 x 1, 2 x 1 (five blocks), repeating. */
	private static final char[] COURSE = { 'l', 'r', 's', 'l', 'r' };
	/** Each course's shift along the face, by y mod 5. */
	private static final int[] SHIFT = { 0, 3, 1, 4, 2 };

	private VoidAlloyBond() {}

	/** The plate on a side of the block at {@code pos}: 's', 'l' or 'r' ('s' up and down). */
	public static char plate(BlockPos pos, Direction side) {
		// a face's left-to-right coordinate as the viewer facing it sees it (its texture's u runs the same way)
		int across;
		switch (side) {
			case SOUTH -> across = pos.getX();
			case NORTH -> across = -pos.getX();
			case WEST -> across = pos.getZ();
			case EAST -> across = -pos.getZ();
			default -> {
				return 's';
			}
		}
		return COURSE[Math.floorMod(across + SHIFT[Math.floorMod(pos.getY(), 5)], 5)];
	}

	/** The state with its bond for this position (unchanged if it has no bond). */
	public static BlockState at(BlockState state, BlockPos pos) {
		if (!state.hasProperty(NS) || !state.hasProperty(EW))
			return state;
		return state.setValue(NS, Pair.of(plate(pos, Direction.NORTH), plate(pos, Direction.SOUTH)))
			.setValue(EW, Pair.of(plate(pos, Direction.EAST), plate(pos, Direction.WEST)));
	}

	/** Puts a block just placed in a live level onto its position's bond, if it isn't (from its {@code onPlace}). */
	public static void follow(Level level, BlockPos pos, BlockState state) {
		if (level.isClientSide)
			return;
		BlockState want = at(state, pos);
		if (want != state && level.getBlockState(pos) == state)
			level.setBlock(pos, want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
	}
}
