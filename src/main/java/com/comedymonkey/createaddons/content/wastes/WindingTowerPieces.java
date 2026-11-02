package com.comedymonkey.createaddons.content.wastes;

/**
 * The winding tower's props (D80-D84), as design/models/clockwork-wastes-props.export.js writes them: generated, don't edit
 * (change design/models/clockwork-wastes-mainspring.gen.js and run the exporter). Each piece is a block whose model reaches
 * into the blocks round its own (its reach: the city counts a prop standing in every cell its reach covers, D87); the tower
 * lays them facing north in its own frame.
 */
public final class WindingTowerPieces {
	private WindingTowerPieces() {}

	/**
	 * A piece: its block id, its outline in its own block facing north (pixels: x0, y0, z0, x1, y1, z1), and its reach (the
	 * cells its model runs through, from its own block, facing north: dx, dy, dz triples).
	 */
	public record Piece(String id, double[] box, int[] reach) {}

	/** A laying: a piece (its index in {@link #PIECES}) at its own block (local x, y, z). */
	public record Lay(int piece, int x, int y, int z) {}

	/** The Sprung Mainspring's pieces, then the Fallen Rafters. */
	public static final Piece[] PIECES = {
		new Piece("sprung_mainspring_arbor_turn_upper", new double[] { 0, 0.75, 0, 16, 6, 16 },
			new int[] { -1, -1, 0, -1, -1, 1, 1, -1, 0, 1, -1, 1, -1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1 }),
		new Piece("sprung_mainspring_arbor_turn_lower", new double[] { 0, 7, 0, 15.75, 16, 16 },
			new int[] { -1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1, -1, 1, 0, -1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1 }),
		new Piece("sprung_mainspring_straight", new double[] { 0, 7, 0, 16, 9, 16 },
			new int[] { 0, 0, 0, 0, 0, 1 }),
		new Piece("sprung_mainspring_burst", new double[] { 1, 7, 0, 15, 13, 16 },
			new int[] { -1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1 }),
		new Piece("sprung_mainspring_peel_0_3", new double[] { 0, 0, 0, 16, 16, 16 },
			new int[] { 0, -2, 0, 0, -2, 1, 0, -1, 0, 0, -1, 1, 1, -1, 0, 1, -1, 1 }),
		new Piece("sprung_mainspring_peel_0_2", new double[] { 0, 0, 0, 16, 16, 16 },
			new int[] { -2, 0, 0, -2, 0, 1, -1, 0, 0, -1, 0, 1, -1, 1, 0, -1, 1, 1, 0, 1, 0, 0, 1, 1, 0, 2, 0, 0, 2, 1 }),
		new Piece("sprung_mainspring_peel_1_2", new double[] { 0, 0, 0, 16, 16, 16 },
			new int[] { 1, -2, 0, 1, -2, 1, 1, -1, 0, 1, -1, 1, 1, 0, 0, 1, 0, 1, 2, 0, 0, 2, 0, 1 }),
		new Piece("sprung_mainspring_peel_1_1", new double[] { 6, 0, 0, 13.75, 14.25, 16 },
			new int[] { 0, -2, 0, 0, -2, 1, -1, -1, 0, -1, -1, 1, 0, -1, 0, 0, -1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 2, 0, 1, 2, 1 }),
		new Piece("sprung_mainspring_peel_1_0", new double[] { 0, 0, 0, 16, 16, 16 },
			new int[] { -2, -1, 0, -2, -1, 1, -1, -1, 0, -1, -1, 1, -1, 0, 0, -1, 0, 1, -1, 1, 0, -1, 1, 1, 0, 1, 0, 0, 1, 1, -1, 2, 0, -1, 2, 1 }),
		new Piece("sprung_mainspring_spiral_3_0", new double[] { 0, 8.25, 0.75, 16, 15.75, 15.5 },
			new int[] { 0, -1, -1, 0, -1, 0, 0, -1, 1, 0, -1, 2, 1, -1, -1, 1, -1, 0, 1, -1, 1, 1, -1, 2, 0, 0, -2, 0, 0, -1, 0, 0, 0, 0, 0, 1, 1, 0, -2, 1, 0, -1, 1, 0, 0, 1, 0, 1, 0, 1, -2, 0, 1, -1, 0, 1, 0, 0, 1, 1, 1, 1, -2, 1, 1, -1, 1, 1, 0, 1, 1, 1, 0, 2, -1, 0, 2, 0, 0, 2, 1, 1, 2, -1, 1, 2, 0, 1, 2, 1 }),
		new Piece("sprung_mainspring_spiral_3_1", new double[] { 0, 0, 0, 16, 16, 16 },
			new int[] { 0, -2, 0, 0, -2, 1, 1, -2, 0, 1, -2, 1, 0, -1, 0, 0, -1, 1, 1, -1, 0, 1, -1, 1, 0, 1, -2, 0, 1, -1, 1, 1, -2, 1, 1, -1, 0, 2, -1, 1, 2, -1 }),
		new Piece("sprung_mainspring_spiral_2_0", new double[] { 0, 0, 0, 16, 16, 16 },
			new int[] { 0, -1, -1, 0, -1, 0, 0, -1, 1, 1, -1, -1, 1, -1, 0, 1, -1, 1, 0, 0, 1, 0, 0, 2, 1, 0, 1, 1, 0, 2, 0, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 2, 0, 0, 2, 1, 1, 2, 0, 1, 2, 1 }),
		new Piece("sprung_mainspring_spiral_2_1", new double[] { 0, 0, 6.75, 16, 14, 8.75 },
			new int[] { 0, -2, 0, 0, -2, 1, 1, -2, 0, 1, -2, 1, 0, -1, 0, 0, -1, 1, 1, -1, 0, 1, -1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 2, 1, 1, 0, 1, 1, 1, 1, 1, 2, 0, 2, 0, 1, 2, 0 }),
		new Piece("sprung_mainspring_spiral_3_2", new double[] { 0, 6, 0.25, 16, 8.75, 13.75 },
			new int[] { 0, -2, 0, 0, -2, 1, 1, -2, 0, 1, -2, 1, 0, -1, -1, 0, -1, 0, 0, -1, 1, 1, -1, -1, 1, -1, 0, 1, -1, 1, 0, 0, -2, 0, 0, -1, 0, 0, 0, 0, 0, 1, 0, 0, 2, 1, 0, -2, 1, 0, -1, 1, 0, 0, 1, 0, 1, 1, 0, 2 }),
		new Piece("sprung_mainspring_spiral_4_1", new double[] { 0, 0.25, 1, 16, 16, 9.75 },
			new int[] { 0, -2, 0, 1, -2, 0, 0, -1, 0, 0, -1, 1, 1, -1, 0, 1, -1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, -2, 0, 1, -1, 0, 1, 0, 0, 1, 1, 1, 1, -2, 1, 1, -1, 1, 1, 0, 1, 1, 1, 0, 2, 0, 1, 2, 0 }),
		new Piece("sprung_mainspring_spiral_4_0", new double[] { 0, 0.25, 0.25, 16, 16, 16 },
			new int[] { 0, -1, -2, 0, -1, -1, 0, -1, 0, 1, -1, -2, 1, -1, -1, 1, -1, 0, 0, 0, -2, 0, 0, -1, 0, 0, 0, 0, 0, 1, 1, 0, -2, 1, 0, -1, 1, 0, 0, 1, 0, 1, 0, 1, -1, 0, 1, 0, 0, 1, 1, 1, 1, -1, 1, 1, 0, 1, 1, 1, 0, 2, 0, 1, 2, 0 }),
		new Piece("sprung_mainspring_spiral_2_2", new double[] { 0, 0, 0, 16, 16, 16 },
			new int[] { 0, -2, 0, 1, -2, 0, 0, -1, 0, 0, -1, 1, 0, -1, 2, 1, -1, 0, 1, -1, 1, 1, -1, 2, 0, 0, 1, 1, 0, 1 }),
		new Piece("sprung_mainspring_spiral_4_2", new double[] { 0, 0, 0, 16, 16, 16 },
			new int[] { 0, -2, 0, 1, -2, 0, 0, -1, -1, 0, -1, 0, 1, -1, -1, 1, -1, 0, 0, 0, -2, 0, 0, -1, 1, 0, -2, 1, 0, -1 }),
		new Piece("sprung_mainspring_spiral_1_0", new double[] { 0, 0, 0, 16, 16, 16 },
			new int[] { 0, -1, -1, 0, -1, 0, 0, -1, 1, 1, -1, -1, 1, -1, 0, 1, -1, 1 }),
		new Piece("sprung_mainspring_fallen_0_0", new double[] { 0, 0, 0, 14, 9, 16 },
			new int[] { 0, -1, 0, 0, -1, 1, -1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1 }),
		new Piece("fallen_rafter_long", new double[] { 1.25, 1.75, 4, 15.25, 16, 12 },
			new int[] { 0, -1, 0, 1, -1, 0, -1, 0, 0, 0, 0, 0, 1, 0, 0, -1, 1, 0, 0, 1, 0 }),
		new Piece("fallen_rafter_short", new double[] { 0.25, 0.25, 4, 15.75, 10.5, 12 },
			new int[] { 0, -1, 0, 1, -1, 0, -1, 0, 0, 0, 0, 0, 1, 0, 0 }),
	};

	/** The Sprung Mainspring's pieces in {@link #PIECES} (the rest are the rafters). */
	public static final int SPRING_PIECES = 20;

	/** Where the tower lays them: the spring's layings, the ruined ring's fallen peel, then the rafters'. */
	public static final Lay[] LAYS = {
		new Lay(0, 5, 11, 3),
		new Lay(1, 5, 9, 3),
		new Lay(2, 4, 9, 3),
		new Lay(2, 3, 9, 3),
		new Lay(2, 2, 9, 3),
		new Lay(3, 0, 9, 3),
		new Lay(4, -3, 10, 3),
		new Lay(5, -3, 7, 3),
		new Lay(6, -6, 7, 3),
		new Lay(7, -6, 4, 3),
		new Lay(8, -6, 1, 3),
		new Lay(9, -8, 1, 10),
		new Lay(10, -8, 4, 10),
		new Lay(11, -8, 1, 7),
		new Lay(12, -8, 4, 7),
		new Lay(13, -8, 7, 10),
		new Lay(14, -8, 4, 13),
		new Lay(15, -8, 1, 13),
		new Lay(16, -8, 7, 7),
		new Lay(17, -8, 7, 13),
		new Lay(18, -8, 1, 4),
		new Lay(19, -8, 1, 3),
		new Lay(20, 2, 18, 3),
		new Lay(21, 2, 19, 7),
	};

	/** The standing spring's layings in {@link #LAYS}, then the fallen peel's (laid in the ruined ring instead of the peel). */
	public static final int SPRING_LAYS = 21, FALLEN_LAYS = 1;

	/** The cells the spring's strip passes through (local x, y, z, and the laying's index in {@link #LAYS}), less the arbor's. */
	public static final int[] SPRING_CELLS = {
		4, 10, 3, 0, 4, 10, 4, 0, 6, 10, 3, 0, 6, 10, 4, 0,
		4, 11, 3, 0, 4, 11, 4, 0, 5, 11, 3, 0, 5, 11, 4, 0,
		6, 11, 3, 0, 6, 11, 4, 0, 4, 9, 3, 1, 4, 9, 4, 1,
		5, 9, 3, 1, 5, 9, 4, 1, 6, 9, 3, 1, 6, 9, 4, 1,
		3, 9, 3, 3, 3, 9, 4, 3, 2, 9, 3, 4, 2, 9, 4, 4,
		-1, 9, 3, 5, -1, 9, 4, 5, 0, 9, 3, 5, 0, 9, 4, 5,
		1, 9, 3, 5, 1, 9, 4, 5, -3, 8, 3, 6, -3, 8, 4, 6,
		-3, 9, 3, 6, -3, 9, 4, 6, -2, 9, 3, 6, -2, 9, 4, 6,
		-5, 7, 3, 7, -5, 7, 4, 7, -4, 7, 3, 7, -4, 7, 4, 7,
		-4, 8, 3, 7, -4, 8, 4, 7, -5, 5, 3, 8, -5, 5, 4, 8,
		-5, 6, 3, 8, -5, 6, 4, 8, -6, 2, 3, 9, -6, 2, 4, 9,
		-7, 3, 3, 9, -7, 3, 4, 9, -6, 3, 3, 9, -6, 3, 4, 9,
		-6, 4, 3, 9, -6, 4, 4, 9, -6, 5, 3, 9, -6, 5, 4, 9,
		-8, 0, 3, 10, -8, 0, 4, 10, -7, 0, 3, 10, -7, 0, 4, 10,
		-7, 1, 3, 10, -7, 1, 4, 10, -7, 2, 3, 10, -7, 2, 4, 10,
		-8, 0, 9, 11, -8, 0, 10, 11, -8, 0, 11, 11, -8, 0, 12, 11,
		-7, 0, 9, 11, -7, 0, 10, 11, -7, 0, 11, 11, -7, 0, 12, 11,
		-8, 1, 8, 11, -8, 1, 9, 11, -8, 1, 10, 11, -8, 1, 11, 11,
		-7, 1, 8, 11, -7, 1, 9, 11, -7, 1, 10, 11, -7, 1, 11, 11,
		-8, 2, 8, 11, -8, 2, 9, 11, -8, 2, 10, 11, -8, 2, 11, 11,
		-7, 2, 8, 11, -7, 2, 9, 11, -7, 2, 10, 11, -7, 2, 11, 11,
		-8, 3, 9, 11, -8, 3, 10, 11, -8, 3, 11, 11, -7, 3, 9, 11,
		-7, 3, 10, 11, -7, 3, 11, 11, -8, 5, 8, 12, -8, 5, 9, 12,
		-7, 5, 8, 12, -7, 5, 9, 12, -8, 6, 9, 12, -7, 6, 9, 12,
		-8, 0, 6, 13, -8, 0, 7, 13, -8, 0, 8, 13, -7, 0, 6, 13,
		-7, 0, 7, 13, -7, 0, 8, 13, -8, 2, 7, 13, -7, 2, 7, 13,
		-8, 3, 7, 13, -8, 3, 8, 13, -7, 3, 7, 13, -7, 3, 8, 13,
		-8, 4, 7, 14, -8, 4, 8, 14, -7, 4, 7, 14, -7, 4, 8, 14,
		-8, 5, 7, 14, -7, 5, 7, 14, -8, 6, 7, 14, -7, 6, 7, 14,
		-8, 5, 10, 15, -8, 5, 11, 15, -7, 5, 10, 15, -7, 5, 11, 15,
		-8, 6, 10, 15, -8, 6, 11, 15, -7, 6, 10, 15, -7, 6, 11, 15,
		-8, 7, 8, 15, -8, 7, 9, 15, -8, 7, 10, 15, -8, 7, 11, 15,
		-8, 7, 12, 15, -7, 7, 8, 15, -7, 7, 9, 15, -7, 7, 10, 15,
		-7, 7, 11, 15, -7, 7, 12, 15, -8, 2, 13, 16, -7, 2, 13, 16,
		-8, 3, 13, 16, -8, 3, 14, 16, -7, 3, 13, 16, -7, 3, 14, 16,
		-8, 4, 13, 16, -8, 4, 14, 16, -7, 4, 13, 16, -7, 4, 14, 16,
		-8, 5, 12, 16, -8, 5, 13, 16, -8, 5, 14, 16, -7, 5, 12, 16,
		-7, 5, 13, 16, -7, 5, 14, 16, -8, 6, 13, 16, -7, 6, 13, 16,
		-8, 0, 13, 17, -7, 0, 13, 17, -8, 1, 12, 17, -8, 1, 13, 17,
		-8, 1, 14, 17, -7, 1, 12, 17, -7, 1, 13, 17, -7, 1, 14, 17,
		-8, 2, 12, 17, -8, 2, 14, 17, -7, 2, 12, 17, -7, 2, 14, 17,
		-8, 6, 8, 18, -7, 6, 8, 18, -8, 6, 12, 19, -7, 6, 12, 19,
		-8, 0, 5, 20, -7, 0, 5, 20, -9, 1, 3, 21, -9, 1, 4, 21,
		-8, 1, 3, 21, -8, 1, 4, 21,
	};

	/** The cells the rafters pass through (local x, y, z, laying index), less the wall's. */
	public static final int[] RAFTER_CELLS = {
		2, 17, 3, 22, 3, 17, 3, 22, 2, 18, 3, 22, 3, 18, 3, 22,
		2, 19, 3, 22, 2, 18, 7, 23, 3, 18, 7, 23, 2, 19, 7, 23,
		3, 19, 7, 23,
	};
}
