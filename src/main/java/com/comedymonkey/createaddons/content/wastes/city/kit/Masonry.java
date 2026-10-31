package com.comedymonkey.createaddons.content.wastes.city.kit;

import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The makers' masonry, one stone per district so districts read apart from the air (design/briefs/clockwork-wastes-buildings.md,
 * "Palette by role"; D73): the wall field (~70%), its close second (gable triangles and panels between windows), the piers,
 * the dressings (sills, corbels, lintels, arches), the plinth and its step, and the stone shown where a face spalled (Create's
 * palette stones have no cracked variants). {@code band} is the lintel band's full block (the dressing stone in the wall
 * plane, so the band reads as a course; provisional, see the progress doc, "The building kit").
 */
public record Masonry(String name, BlockState wall, BlockState second, BlockState pier, BlockState dressStairs, BlockState dressSlab,
	BlockState band, BlockState plinth, BlockState plinthStairs, BlockState plinthSlab, BlockState spalled, BlockState parapet) {

	static BlockState c(String id, net.minecraft.world.level.block.Block fallback) {
		return CityPlan.block("create:" + id, fallback);
	}

	/** Foundry: firebrick, darkened by a century of smoke (cut scoria bricks, 64). */
	public static Masonry foundry() {
		return new Masonry("foundry", c("cut_scoria_bricks", Blocks.BRICKS), c("small_scoria_bricks", Blocks.BRICKS),
			CityPlan.with(c("scoria_pillar", Blocks.BRICKS), "axis", "y"), c("cut_scoria_brick_stairs", Blocks.BRICK_STAIRS),
			c("cut_scoria_brick_slab", Blocks.BRICK_SLAB), c("polished_cut_scoria", Blocks.BRICKS), c("polished_cut_scorchia", Blocks.POLISHED_BLACKSTONE),
			c("polished_cut_scorchia_stairs", Blocks.POLISHED_BLACKSTONE_STAIRS), c("polished_cut_scorchia_slab", Blocks.POLISHED_BLACKSTONE_SLAB),
			c("scoria", Blocks.BRICKS), c("cut_scoria_brick_wall", Blocks.BRICK_WALL));
	}

	/** Workshops: Create's andesite world a step darker (cut tuff bricks, 112) with andesite casing piers. */
	public static Masonry workshops() {
		return new Masonry("workshops", c("cut_tuff_bricks", Blocks.TUFF_BRICKS), c("small_tuff_bricks", Blocks.TUFF_BRICKS),
			c("andesite_casing", Blocks.POLISHED_ANDESITE), c("polished_cut_tuff_stairs", Blocks.POLISHED_TUFF_STAIRS),
			c("polished_cut_tuff_slab", Blocks.POLISHED_TUFF_SLAB), c("polished_cut_tuff", Blocks.POLISHED_TUFF), c("cut_deepslate_bricks", Blocks.DEEPSLATE_BRICKS),
			c("cut_deepslate_brick_stairs", Blocks.DEEPSLATE_BRICK_STAIRS), c("cut_deepslate_brick_slab", Blocks.DEEPSLATE_BRICK_SLAB),
			Blocks.TUFF.defaultBlockState(), c("cut_tuff_brick_wall", Blocks.TUFF_BRICK_WALL));
	}

	/** Rail Yard: blue engineering brick (cut deepslate bricks, 84), lighter tuff dressings for contrast. */
	public static Masonry railYard() {
		return new Masonry("rail_yard", c("cut_deepslate_bricks", Blocks.DEEPSLATE_BRICKS), c("small_deepslate_bricks", Blocks.DEEPSLATE_TILES),
			CityPlan.with(c("deepslate_pillar", Blocks.DEEPSLATE), "axis", "y"), c("polished_cut_tuff_stairs", Blocks.POLISHED_TUFF_STAIRS),
			c("polished_cut_tuff_slab", Blocks.POLISHED_TUFF_SLAB), c("polished_cut_tuff", Blocks.POLISHED_TUFF), c("polished_cut_scorchia", Blocks.POLISHED_BLACKSTONE),
			c("polished_cut_scorchia_stairs", Blocks.POLISHED_BLACKSTONE_STAIRS), c("polished_cut_scorchia_slab", Blocks.POLISHED_BLACKSTONE_SLAB),
			Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), c("cut_deepslate_brick_wall", Blocks.DEEPSLATE_BRICK_WALL));
	}

	/** Quarters and the heart drum: the island's own stone (Stained End Stone Bricks, D73, D75), cut deepslate dressings. */
	public static Masonry quarters() {
		BlockState seb = ModWastes.STAINED_END_STONE_BRICKS.get().defaultBlockState();
		return new Masonry("quarters", seb, seb, CityPlan.with(c("deepslate_pillar", Blocks.DEEPSLATE), "axis", "y"),
			c("cut_deepslate_brick_stairs", Blocks.DEEPSLATE_BRICK_STAIRS), c("cut_deepslate_brick_slab", Blocks.DEEPSLATE_BRICK_SLAB),
			c("cut_deepslate_bricks", Blocks.DEEPSLATE_BRICKS), c("cut_deepslate_bricks", Blocks.DEEPSLATE_BRICKS),
			c("cut_deepslate_brick_stairs", Blocks.DEEPSLATE_BRICK_STAIRS), c("cut_deepslate_brick_slab", Blocks.DEEPSLATE_BRICK_SLAB),
			ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get().defaultBlockState(), ModWastes.STAINED_END_STONE_BRICK_WALL.get().defaultBlockState());
	}

	/** Each district's stone (the heart's is the Quarters'). */
	public static Masonry of(District d) {
		return switch (d) {
			case FOUNDRY -> foundry();
			case WORKSHOPS -> workshops();
			case RAIL_YARD -> railYard();
			default -> quarters();
		};
	}

	// ---------------------------------------------------------------- shared by every makers' building (one town)

	/** Slate roofs (deepslate tiles, 55). */
	public static BlockState slateSlab() {
		return Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState();
	}

	public static BlockState slateStairs() {
		return Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState();
	}

	/** Where a slab roof crosses a wall line: the roof's two halves as one block (the wall plate, hidden from outside). */
	public static BlockState slateFull() {
		return Blocks.DEEPSLATE_TILES.defaultBlockState();
	}

	/**
	 * The copper ridge roll (patina from age; no rain): the Patina Rib (D75's muted patina) in place of the brief's vanilla
	 * weathered cut copper slab, which read mint against the slate (provisional, for the user's word: progress doc).
	 */
	public static BlockState ridge() {
		return ModWastes.PATINA_RIB.get().defaultBlockState();
	}

	/** Walnut: barges, rafters, trusses, floors, benches. */
	public static BlockState walnutSlab() {
		return Blocks.DARK_OAK_SLAB.defaultBlockState();
	}

	public static BlockState walnutStairs() {
		return Blocks.DARK_OAK_STAIRS.defaultBlockState();
	}

	public static BlockState walnutFence() {
		return Blocks.DARK_OAK_FENCE.defaultBlockState();
	}

	public static BlockState walnutPlanks() {
		return Blocks.DARK_OAK_PLANKS.defaultBlockState();
	}

	/** A charred rafter stub (fire). */
	public static BlockState charred() {
		return Blocks.DARK_OAK_LOG.defaultBlockState();
	}

	/** The makers' windows: dark, dusty, mostly broken (D75: the Broken Makers' Window, not Create's weathered iron window). */
	public static BlockState window() {
		return ModWastes.BROKEN_MAKERS_WINDOW.get().defaultBlockState();
	}

	/** Railings, fences and grilles on makers' work (Create's andesite bars, 107; never Iron Bars, 138). */
	public static BlockState bars() {
		return c("andesite_bars", Blocks.IRON_BARS);
	}

	/** Steel straps and hoops. */
	public static BlockState hoop() {
		return c("weathered_iron_block", Blocks.IRON_BLOCK);
	}

	public static BlockState industrialIron() {
		return c("industrial_iron_block", Blocks.IRON_BLOCK);
	}

	/** Soot: a fan's inner band, its dressings and its pier faces. */
	public static BlockState soot() {
		return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
	}

	public static BlockState sootCracked() {
		return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
	}

	public static BlockState sootStairs() {
		return Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState();
	}

	public static BlockState sootSlab() {
		return Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState();
	}

	public static BlockState sootPlain() {
		return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
	}

	/** The terraces' hasty rubble infill (D73: the makers bricked up their own doors). */
	public static BlockState rubble() {
		return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
	}

	public static boolean isSoot(BlockState s) {
		return s.is(Blocks.POLISHED_BLACKSTONE_BRICKS) || s.is(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS) || s.is(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS)
			|| s.is(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB) || s.is(Blocks.POLISHED_BLACKSTONE);
	}

	/** A cracked or spalled stone of any district (the mark a cause leaves). */
	public static boolean isSpalled(BlockState s) {
		return s.is(Blocks.TUFF) || s.is(Blocks.CRACKED_DEEPSLATE_BRICKS) || s.is(ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get())
			|| CityPlan.block("create:scoria", Blocks.AIR).getBlock() == s.getBlock() && !s.isAir();
	}
}
