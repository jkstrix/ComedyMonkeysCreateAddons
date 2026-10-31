package com.comedymonkey.createaddons.content.wastes.city.kit;

/**
 * A building's state by its ring of damage (design/briefs/clockwork-wastes-buildings.md, "Wear and war damage"; D73: intact
 * to 22, worn 23-45, damaged 46-70, ruined beyond). The ring is the distance from the city's middle, the larger of |x| and
 * |z|, of the building's outer end (the end farthest from the heart: damage starts there).
 */
public enum Wear {
	/** The heart: wear only (soot above chimneys, patina, plaques straight). No war marks. */
	INTACT,
	/** The districts' inner half: one strike a building at most, on its outward face; slates slipped; one chip an outward void-alloy face. */
	WORN,
	/** The districts' outer half: strikes, a roof hole or fallen bay at the outer end, broken windows, towers snapped, plaques askew. */
	DAMAGED,
	/** The outskirts: stubs, outer walls fallen, rubble cones, only the plinth and corner piers standing. */
	RUINED;

	/** The rings' outer edges (D73). */
	public static final int INTACT_TO = 22, WORN_TO = 45, DAMAGED_TO = 70;

	/** The state for a building whose outer end is {@code ring} from the city's middle. */
	public static Wear forRing(int ring) {
		ring = Math.abs(ring);
		return ring <= INTACT_TO ? INTACT : ring <= WORN_TO ? WORN : ring <= DAMAGED_TO ? DAMAGED : RUINED;
	}

	/** The ring of (dx, dz) from the city's middle: the larger of |dx| and |dz|. */
	public static int ring(int dx, int dz) {
		return Math.max(Math.abs(dx), Math.abs(dz));
	}

	public boolean atLeast(Wear w) {
		return ordinal() >= w.ordinal();
	}

	/** No worse than {@code most} (a building the brief keeps whole: the sky tower holds a live span, the yard is near the heart). */
	public Wear atMost(Wear most) {
		return ordinal() > most.ordinal() ? most : this;
	}
}
