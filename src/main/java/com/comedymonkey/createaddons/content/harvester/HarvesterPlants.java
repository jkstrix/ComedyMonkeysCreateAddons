package com.comedymonkey.createaddons.content.harvester;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * What can go in an Auto Harvester and what each harvest gives. Nothing is configured per plant: a crop gives what
 * its block would drop fully grown (so wheat seeds give wheat and the odd extra seed, sweet berries give berries,
 * melon seeds give a melon's slices); a sapling gives logs of its wood plus a few rolls of its leaves' drops (more
 * saplings, sticks, apples), and a nether fungus grows a huge fungus: stems, wart blocks and the odd shroomlight.
 * The planted item is never used up. Item tags {@code auto_harvester_plantable} and
 * {@code auto_harvester_blacklist} add or forbid plants.
 */
public final class HarvesterPlants {
	public static final TagKey<Item> PLANTABLE = ItemTags.create(ComedyMonkeysCreateAddons.asResource("auto_harvester_plantable"));
	public static final TagKey<Item> BLACKLIST = ItemTags.create(ComedyMonkeysCreateAddons.asResource("auto_harvester_blacklist"));

	/** StemBlock keeps the fruit it grows private. */
	@Nullable
	private static final Field STEM_FRUIT = findStemFruit();

	private HarvesterPlants() {}

	/** What kind of plant this item would be in the planter, or {@link PlantKind#NONE} if it can't be planted. */
	public static PlantKind kindOf(ItemStack stack) {
		if (stack.isEmpty() || stack.is(BLACKLIST) || !(stack.getItem() instanceof BlockItem item))
			return PlantKind.NONE;
		Block block = item.getBlock();
		if (stack.is(ItemTags.SAPLINGS) || block instanceof SaplingBlock || block instanceof FungusBlock)
			return woodOf(block, "_log") != null ? PlantKind.TREE : PlantKind.NONE;
		if (stack.is(PLANTABLE) || isGrownPlant(stack, block) || block instanceof SugarCaneBlock || block instanceof CactusBlock
			|| block instanceof BambooSaplingBlock || block instanceof BambooStalkBlock || block instanceof CocoaBlock
			|| block instanceof GrowingPlantBlock)
			return PlantKind.CROP;
		return PlantKind.NONE;
	}

	/**
	 * The small plants worth growing: crops, stems, berry bushes, nether wart, pitcher pods, flowers (small and two
	 * tall, vanilla's tags or any flower block, so modded ones count) and pink petals, and mushrooms (nether fungi grow
	 * huge fungi, so they're trees).
	 * Not grass, ferns, tall grass, dead bushes, roots, sprouts, seagrass or lily pads, which give nothing worth having.
	 */
	private static boolean isGrownPlant(ItemStack stack, Block block) {
		return block instanceof CropBlock || block instanceof StemBlock || block instanceof SweetBerryBushBlock
			|| block instanceof NetherWartBlock || block instanceof PitcherCropBlock
			|| stack.is(ItemTags.SMALL_FLOWERS) || stack.is(ItemTags.TALL_FLOWERS)
			|| block instanceof FlowerBlock || block instanceof TallFlowerBlock || block instanceof PinkPetalsBlock
			|| block instanceof MushroomBlock;
	}

	/**
	 * How long this plant takes to grow compared to wheat, from how vanilla grows it: the stages it goes through and
	 * the chance each random tick has of moving it on (a moist, well-placed crop advances about one tick in seven).
	 * Beetroot is a little slower than wheat, berries, cocoa, cane and cactus several times faster. Kept between
	 * 0.2 and 2 so nothing is instant or endless; trees all grow at the configured tree time.
	 */
	public static float growthFactor(ItemStack plant) {
		if (!(plant.getItem() instanceof BlockItem item))
			return 1;
		Block block = item.getBlock();
		// Random ticks to grow, against wheat's 7 stages x 7 ticks
		float ticks;
		if (block instanceof StemBlock)
			ticks = 7 * 7 + 7;
		else if (block instanceof BeetrootBlock)
			ticks = 3 * 7 * 3;
		else if (block instanceof CropBlock crop)
			ticks = crop.getMaxAge() * 7;
		else if (block instanceof SweetBerryBushBlock)
			ticks = 3 * 5;
		else if (block instanceof NetherWartBlock)
			ticks = 3 * 10;
		else if (block instanceof CocoaBlock)
			ticks = 2 * 5;
		else if (block instanceof SugarCaneBlock || block instanceof CactusBlock)
			ticks = 16;
		else if (block instanceof BambooSaplingBlock || block instanceof BambooStalkBlock)
			ticks = 6;
		else if (block instanceof GrowingPlantBlock)
			ticks = 7;
		else
			ticks = 49;
		return Math.clamp(ticks / 49f, 0.2f, 2f);
	}

	/** One harvest's worth of items from this plant. */
	public static List<ItemStack> harvest(ItemStack plant, ServerLevel level, BlockPos pos) {
		if (!(plant.getItem() instanceof BlockItem item))
			return List.of();
		Block block = item.getBlock();
		return switch (kindOf(plant)) {
			case TREE -> fell(block, level, pos);
			case CROP -> drops(fruitOf(block), level, pos);
			case NONE -> List.of();
		};
	}

	private static List<ItemStack> fell(Block sapling, ServerLevel level, BlockPos pos) {
		List<ItemStack> out = new ArrayList<>();
		Block log = woodOf(sapling, "_log");
		if (log == null)
			return out;
		int logs = CMConfig.SERVER.harvesterTreeLogs.get() + level.random.nextInt(3);
		if (logs > 0)
			out.add(new ItemStack(log, logs));
		Block leaves = woodOf(sapling, "_leaves");
		if (leaves != null)
			for (int i = CMConfig.SERVER.harvesterLeafRolls.get(); i > 0; i--) {
				out.addAll(drops(leaves.defaultBlockState(), level, pos));
				// A huge fungus has shroomlights dotted among its wart
				if (sapling instanceof FungusBlock && level.random.nextInt(3) == 0)
					out.add(new ItemStack(Items.SHROOMLIGHT));
			}
		return out;
	}

	/** The block's drops with no tool, as if it were broken by hand where the harvester stands. */
	private static List<ItemStack> drops(BlockState state, ServerLevel level, BlockPos pos) {
		return state.getDrops(new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
			.withParameter(LootContextParams.TOOL, ItemStack.EMPTY));
	}

	/** What is actually picked: a stem's melon or pumpkin, otherwise the plant itself, fully grown. */
	private static BlockState fruitOf(Block block) {
		if (block instanceof StemBlock && STEM_FRUIT != null) {
			try {
				@SuppressWarnings("unchecked")
				ResourceKey<Block> fruit = (ResourceKey<Block>) STEM_FRUIT.get(block);
				Block fruitBlock = BuiltInRegistries.BLOCK.get(fruit);
				if (fruitBlock != null && fruitBlock != Blocks.AIR)
					return fruitBlock.defaultBlockState();
			} catch (IllegalAccessException | ClassCastException ignored) {}
		}
		if (block instanceof CropBlock crop)
			return crop.getStateForAge(crop.getMaxAge());
		return mature(block.defaultBlockState());
	}

	/** Its "age" property at the top, for berries, nether wart, cocoa and the like. */
	private static BlockState mature(BlockState state) {
		for (Property<?> property : state.getProperties())
			if (property instanceof IntegerProperty age && property.getName().equals("age"))
				return state.setValue(age, age.getPossibleValues().stream().max(Integer::compare).orElse(0));
		return state;
	}

	/**
	 * The log or leaves of a sapling's wood, found by name in the sapling's own namespace (so modded trees work too):
	 * oak_sapling to oak_log, mangrove_propagule to mangrove_log, azaleas to oak logs and their own leaves, and a
	 * fungus to its stem and wart block (crimson's is the nether wart block).
	 */
	@Nullable
	public static Block woodOf(Block sapling, String suffix) {
		ResourceLocation id = BuiltInRegistries.BLOCK.getKey(sapling);
		String path = id.getPath();
		String wood;
		if (path.endsWith("_sapling"))
			wood = path.substring(0, path.length() - "_sapling".length());
		else if (path.endsWith("_propagule"))
			wood = path.substring(0, path.length() - "_propagule".length());
		else if (path.endsWith("_fungus")) {
			wood = path.substring(0, path.length() - "_fungus".length());
			if (suffix.equals("_leaves")) {
				ResourceLocation wart = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), wood + "_wart_block");
				if (BuiltInRegistries.BLOCK.containsKey(wart))
					return BuiltInRegistries.BLOCK.get(wart);
				return wood.equals("crimson") ? Blocks.NETHER_WART_BLOCK : null;
			}
		}
		else if (path.equals("azalea") || path.equals("flowering_azalea"))
			return suffix.equals("_log") ? Blocks.OAK_LOG : path.equals("azalea") ? Blocks.AZALEA_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES;
		else
			return null;
		ResourceLocation found = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), wood + suffix);
		if (!BuiltInRegistries.BLOCK.containsKey(found) && suffix.equals("_log"))
			// Some mods name the trunk of their trees "stem" or "wood"
			for (String other : new String[] {"_stem", "_wood"}) {
				ResourceLocation alt = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), wood + other);
				if (BuiltInRegistries.BLOCK.containsKey(alt))
					return BuiltInRegistries.BLOCK.get(alt);
			}
		return BuiltInRegistries.BLOCK.containsKey(found) ? BuiltInRegistries.BLOCK.get(found) : null;
	}

	@Nullable
	private static Field findStemFruit() {
		for (Field field : StemBlock.class.getDeclaredFields())
			if (field.getType() == ResourceKey.class && field.getName().equals("fruit")) {
				field.setAccessible(true);
				return field;
			}
		return null;
	}
}
