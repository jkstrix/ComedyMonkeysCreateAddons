package com.comedymonkey.createaddons.content.juggernaut.ruin;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The Juggernaut Ruins' own copies of the blocks they're built from (the user's "tag the ruin's blocks"): each looks,
 * sounds and mines like its vanilla block, is named as it, drops it and picks as it, but is in
 * {@code #comedymonkeysaddons:juggernaut_crumbles}, so any Juggernaut that comes up against it crumbles it. A player's
 * wall of the same brick is vanilla brick and stands. The hall's floor and the field's paving stay vanilla: they're the
 * ground. They have no items: nobody ever holds one.
 */
public final class RuinBlocks {
	/** A ruin block: the vanilla block it copies. */
	public interface Ruin {
		Block original();
	}

	public static final DeferredBlock<Block> DEEPSLATE_BRICKS = plain("deepslate_bricks", () -> Blocks.DEEPSLATE_BRICKS);
	public static final DeferredBlock<Block> CRACKED_DEEPSLATE_BRICKS = plain("cracked_deepslate_bricks", () -> Blocks.CRACKED_DEEPSLATE_BRICKS);
	public static final DeferredBlock<Block> DEEPSLATE_TILES = plain("deepslate_tiles", () -> Blocks.DEEPSLATE_TILES);
	public static final DeferredBlock<Block> COBBLED_DEEPSLATE = plain("cobbled_deepslate", () -> Blocks.COBBLED_DEEPSLATE);
	public static final DeferredBlock<Block> POLISHED_DEEPSLATE = plain("polished_deepslate", () -> Blocks.POLISHED_DEEPSLATE);
	public static final DeferredBlock<Block> MOSSY_COBBLESTONE = plain("mossy_cobblestone", () -> Blocks.MOSSY_COBBLESTONE);
	public static final DeferredBlock<Block> END_STONE = plain("end_stone", () -> Blocks.END_STONE);
	public static final DeferredBlock<Block> END_STONE_BRICKS = plain("end_stone_bricks", () -> Blocks.END_STONE_BRICKS);
	public static final DeferredBlock<Block> PURPUR_BLOCK = plain("purpur_block", () -> Blocks.PURPUR_BLOCK);
	public static final DeferredBlock<SlabBlock> DEEPSLATE_TILE_SLAB = copy("deepslate_tile_slab", () -> Blocks.DEEPSLATE_TILE_SLAB, Slab::new);
	public static final DeferredBlock<SlabBlock> END_STONE_BRICK_SLAB = copy("end_stone_brick_slab", () -> Blocks.END_STONE_BRICK_SLAB, Slab::new);
	public static final DeferredBlock<RotatedPillarBlock> PURPUR_PILLAR = copy("purpur_pillar", () -> Blocks.PURPUR_PILLAR, Pillar::new);
	public static final DeferredBlock<ChainBlock> CHAIN = copy("chain", () -> Blocks.CHAIN, Chain::new);
	public static final DeferredBlock<ColoredFallingBlock> GRAVEL = copy("gravel", () -> Blocks.GRAVEL, Gravel::new);

	/** Built on first use, from worldgen threads at once: volatile, so none sees it half-filled. */
	private static volatile Map<Block, Block> copies;

	private RuinBlocks() {}

	/** Loads the class, so its blocks are registered with the mod's. */
	public static void register() {}

	private static DeferredBlock<Block> plain(String name, Supplier<Block> original) {
		return copy(name, original, Plain::new);
	}

	private static <B extends Block> DeferredBlock<B> copy(String name, Supplier<Block> original,
														   java.util.function.BiFunction<Block, BlockBehaviour.Properties, B> make) {
		return ModBlocks.BLOCKS.register("ruin_" + name, () -> {
			Block vanilla = original.get();
			return make.apply(vanilla, BlockBehaviour.Properties.ofFullCopy(vanilla).lootFrom(original));
		});
	}

	/** The ruin's copy of {@code state} (its shape and facing kept), or {@code state} itself if it has none. */
	public static BlockState ruin(BlockState state) {
		if (copies == null) {
			Map<Block, Block> map = new IdentityHashMap<>();
			for (DeferredBlock<?> b : java.util.List.of(DEEPSLATE_BRICKS, CRACKED_DEEPSLATE_BRICKS, DEEPSLATE_TILES, COBBLED_DEEPSLATE,
				POLISHED_DEEPSLATE, MOSSY_COBBLESTONE, END_STONE, END_STONE_BRICKS, PURPUR_BLOCK, DEEPSLATE_TILE_SLAB, END_STONE_BRICK_SLAB,
				PURPUR_PILLAR, CHAIN, GRAVEL))
				map.put(((Ruin) b.get()).original(), b.get());
			copies = map;
		}
		Block copy = copies.get(state.getBlock());
		return copy == null ? state : copy.withPropertiesOf(state);
	}

	/** Named as the vanilla block, and pick-block gives the vanilla block. */
	private static String name(Block original) {
		return original.getDescriptionId();
	}

	private static ItemStack pick(Block original) {
		return new ItemStack(original);
	}

	static final class Plain extends Block implements Ruin {
		private final Block original;

		Plain(Block original, Properties properties) {
			super(properties);
			this.original = original;
		}

		@Override
		public Block original() {
			return original;
		}

		@Override
		public String getDescriptionId() {
			return name(original);
		}

		@Override
		@SuppressWarnings("deprecation") // NeoForge's pick-block asks this one
		public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
			return pick(original);
		}
	}

	static final class Slab extends SlabBlock implements Ruin {
		private final Block original;

		Slab(Block original, Properties properties) {
			super(properties);
			this.original = original;
		}

		@Override
		public Block original() {
			return original;
		}

		@Override
		public String getDescriptionId() {
			return name(original);
		}

		@Override
		@SuppressWarnings("deprecation") // NeoForge's pick-block asks this one
		public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
			return pick(original);
		}
	}

	static final class Pillar extends RotatedPillarBlock implements Ruin {
		private final Block original;

		Pillar(Block original, Properties properties) {
			super(properties);
			this.original = original;
		}

		@Override
		public Block original() {
			return original;
		}

		@Override
		public String getDescriptionId() {
			return name(original);
		}

		@Override
		@SuppressWarnings("deprecation") // NeoForge's pick-block asks this one
		public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
			return pick(original);
		}
	}

	static final class Chain extends ChainBlock implements Ruin {
		private final Block original;

		Chain(Block original, Properties properties) {
			super(properties);
			this.original = original;
		}

		@Override
		public Block original() {
			return original;
		}

		@Override
		public String getDescriptionId() {
			return name(original);
		}

		@Override
		@SuppressWarnings("deprecation") // NeoForge's pick-block asks this one
		public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
			return pick(original);
		}
	}

	/** Gravel falls as gravel does, and lands as the ruin's. */
	static final class Gravel extends ColoredFallingBlock implements Ruin {
		private final Block original;

		Gravel(Block original, Properties properties) {
			super(new ColorRGBA(-8356741), properties);
			this.original = original;
		}

		/** Broken as it lands (on a torch, a slab), it drops gravel, as gravel does: it has no item of its own. */
		@Override
		public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity entity) {
			if (entity.dropItem && level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS))
				entity.spawnAtLocation(original);
		}

		@Override
		public Block original() {
			return original;
		}

		@Override
		public String getDescriptionId() {
			return name(original);
		}

		@Override
		@SuppressWarnings("deprecation") // NeoForge's pick-block asks this one
		public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
			return pick(original);
		}
	}
}
