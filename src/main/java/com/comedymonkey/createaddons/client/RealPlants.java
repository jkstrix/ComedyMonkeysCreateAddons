package com.comedymonkey.createaddons.client;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.harvester.HarvesterPlants;
import com.comedymonkey.createaddons.content.harvester.PlantKind;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The actual plant in an Auto Harvester's planter, drawn small with its own block models (and their usual tints),
 * so modded crops show up as themselves too. Crops show the growth stage matching how far they've grown; tall
 * crops (two blocks, like flax) get their top half; cane, cactus and bamboo stack up; saplings grow into a little
 * tree built from their wood's own log and leaves. Works in the planter's model space (front facing north).
 */
final class RealPlants {
	/** Soil level, in pixels. */
	private static final float SOIL_Y = 3;
	/** Size of a one-block plant: fits the planter, clears the parked head. */
	private static final float CROP_SCALE = 0.45f;
	/** Each block of a two-block plant, or of a stack of cane, cactus or bamboo. */
	private static final float TALL_SCALE = 0.3f;
	/** Each block of the little tree: 2 pixels. */
	private static final float TREE_BLOCK = 0.125f;
	/** Growth at which a sapling becomes a tree. */
	private static final float SAPLING_UNTIL = 0.35f;
	/** The little tree: two logs, a cross of leaves and one on top, in blocks from the trunk's base. */
	private static final int[][] TREE_LOGS = {{0, 0, 0}, {0, 1, 0}};
	private static final int[][] TREE_LEAVES = {{0, 2, 0}, {1, 2, 0}, {-1, 2, 0}, {0, 2, 1}, {0, 2, -1}, {0, 3, 0}};
	/** What's worked out once per plant, rather than every frame. Only used on the render thread. */
	private static final Map<Block, Plant> PLANTS = new IdentityHashMap<>();
	/** Whether each two-block crop's top half is really there ({@link #hasRealTop}), which depends on the models. */
	private static final Map<BlockState, Boolean> REAL_TOPS = new IdentityHashMap<>();

	/**
	 * A plant's tree parts (null unless it's a sapling with a wood to match) and its "age" property with that age's
	 * range (null if it has none).
	 */
	private record Plant(@Nullable Block log, @Nullable Block leaves, @Nullable IntegerProperty age, int minAge, int maxAge) {
		static Plant of(Block block) {
			Block log = HarvesterPlants.woodOf(block, "_log");
			Block leaves = HarvesterPlants.woodOf(block, "_leaves");
			IntegerProperty age = ageOf(block.defaultBlockState());
			int min = 0, max = 0;
			if (age != null) {
				min = Integer.MAX_VALUE;
				max = Integer.MIN_VALUE;
				for (int value : age.getPossibleValues()) {
					min = Math.min(min, value);
					max = Math.max(max, value);
				}
			}
			return new Plant(log, leaves, age, min, max);
		}
	}

	private RealPlants() {}

	/** Forgets what was worked out from the models, when they're reloaded. */
	static void forget() {
		REAL_TOPS.clear();
	}

	private static Plant plant(Block block) {
		return PLANTS.computeIfAbsent(block, Plant::of);
	}

	/** Draws the plant; false if it can't be drawn as blocks, so the generic model is used instead. */
	static boolean render(ItemStack plant, PlantKind kind, float growth, PoseStack ms, MultiBufferSource buffer, int light,
						  int overlay) {
		if (!(plant.getItem() instanceof BlockItem item))
			return false;
		Block block = item.getBlock();
		return kind == PlantKind.TREE ? tree(block, growth, ms, buffer, light, overlay)
			: crop(block, growth, ms, buffer, light, overlay);
	}

	private static boolean crop(Block block, float growth, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		// Cane, cactus and bamboo: one block, then two as it grows
		if (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooStalkBlock
			|| block instanceof BambooSaplingBlock) {
			BlockState stalk = block instanceof BambooSaplingBlock ? Blocks.BAMBOO.defaultBlockState() : block.defaultBlockState();
			if (block instanceof BambooSaplingBlock && growth < SAPLING_UNTIL)
				return single(block.defaultBlockState(), CROP_SCALE, ms, buffer, light, overlay);
			if (!drawable(stalk))
				return false;
			int blocks = growth < 0.5f ? 1 : 2;
			float scale = TALL_SCALE * (0.7f + 0.3f * growth);
			for (int i = 0; i < blocks; i++)
				block(stalk, 0, i, 0, scale, ms, buffer, light, overlay);
			return true;
		}

		BlockState state = staged(block.defaultBlockState(), block, growth);
		if (!drawable(state))
			return false;
		// Plants with no growth stages (flowers, mushrooms, sunflowers) grow in size instead
		float size = block instanceof CropBlock || plant(block).age() != null ? 1 : 0.3f + 0.7f * growth;
		// Two-block plants, like Supplementaries' flax: the top half on the bottom one, both smaller
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			block(state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), 0, 0, 0, TALL_SCALE * size,
				ms, buffer, light, overlay);
			BlockState upper = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
			if (drawable(upper) && hasRealTop(upper))
				block(upper, 0, 1, 0, TALL_SCALE * size, ms, buffer, light, overlay);
			return true;
		}
		return single(state, CROP_SCALE * size, ms, buffer, light, overlay);
	}

	private static boolean tree(Block sapling, float growth, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (growth < SAPLING_UNTIL)
			return single(sapling.defaultBlockState(), CROP_SCALE * (0.6f + 0.4f * growth / SAPLING_UNTIL), ms, buffer,
				light, overlay);
		Plant plant = plant(sapling);
		Block log = plant.log(), leaves = plant.leaves();
		if (log == null || leaves == null || !drawable(log.defaultBlockState()) || !drawable(leaves.defaultBlockState()))
			return false;
		// The whole tree grows from about two thirds to full size
		float scale = TREE_BLOCK * (0.65f + 0.35f * (growth - SAPLING_UNTIL) / (1 - SAPLING_UNTIL));
		for (int[] at : TREE_LOGS)
			block(log.defaultBlockState(), at[0], at[1], at[2], scale, ms, buffer, light, overlay);
		for (int[] at : TREE_LEAVES)
			block(leaves.defaultBlockState(), at[0], at[1], at[2], scale, ms, buffer, light, overlay);
		return true;
	}

	/** The crop's growth stage for how far it has grown: its age, or any "age" property, scaled to its range. */
	private static BlockState staged(BlockState state, Block block, float growth) {
		if (block instanceof CropBlock crop)
			return crop.getStateForAge(Math.round(growth * crop.getMaxAge()));
		Plant plant = plant(block);
		IntegerProperty age = plant.age();
		if (age == null)
			return state;
		int min = plant.minAge(), max = plant.maxAge();
		return state.setValue(age, Mth.clamp(min + Math.round(growth * (max - min)), min, max));
	}

	@Nullable
	private static IntegerProperty ageOf(BlockState state) {
		for (Property<?> property : state.getProperties())
			if (property instanceof IntegerProperty age && property.getName().equals("age"))
				return age;
		return null;
	}

	/**
	 * Whether a two-block crop really has its top half at this age. Crops that only grow tall later (flax from age 4)
	 * give the top half of the younger ages a placeholder model, the same one a seedling's top would have; a top that
	 * looks like the age-0 one isn't there yet. Plants without an age (sunflowers and the like) always have one.
	 */
	private static boolean hasRealTop(BlockState upper) {
		return REAL_TOPS.computeIfAbsent(upper, RealPlants::lookHasRealTop);
	}

	private static boolean lookHasRealTop(BlockState upper) {
		Plant plant = plant(upper.getBlock());
		IntegerProperty age = plant.age();
		if (age == null)
			return true;
		int youngest = plant.minAge();
		if (upper.getValue(age) == youngest)
			return false;
		var models = Minecraft.getInstance().getBlockRenderer();
		BakedModel model = models.getBlockModel(upper);
		BakedModel seedling = models.getBlockModel(upper.setValue(age, youngest));
		if (model == Minecraft.getInstance().getModelManager().getMissingModel())
			return false;
		return !looksSame(model, seedling);
	}

	private static boolean looksSame(BakedModel a, BakedModel b) {
		if (a == b)
			return true;
		RandomSource random = RandomSource.create(42);
		List<BakedQuad> quadsA = a.getQuads(null, null, random, ModelData.EMPTY, null);
		random = RandomSource.create(42);
		List<BakedQuad> quadsB = b.getQuads(null, null, random, ModelData.EMPTY, null);
		if (quadsA.size() != quadsB.size() || a.getParticleIcon(ModelData.EMPTY) != b.getParticleIcon(ModelData.EMPTY))
			return false;
		for (int i = 0; i < quadsA.size(); i++)
			if (quadsA.get(i).getSprite() != quadsB.get(i).getSprite())
				return false;
		return true;
	}

	private static boolean drawable(BlockState state) {
		return state.getRenderShape() == RenderShape.MODEL;
	}

	private static boolean single(BlockState state, float scale, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (!drawable(state))
			return false;
		block(state, 0, 0, 0, scale, ms, buffer, light, overlay);
		return true;
	}

	/** One block at {@code scale}, centred on the planter, {@code (x, y, z)} blocks from the middle of the soil. */
	private static void block(BlockState state, int x, int y, int z, float scale, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		ms.pushPose();
		ms.translate(0.5 + (x - 0.5) * scale, SOIL_Y / 16 + y * scale, 0.5 + (z - 0.5) * scale);
		ms.scale(scale, scale, scale);
		Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ms, buffer, light, overlay, ModelData.EMPTY, null);
		ms.popPose();
	}
}
