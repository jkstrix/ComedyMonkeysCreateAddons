package com.comedymonkey.createaddons.gametest;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.CutDeepslateDiagonalBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.MossCarpetBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.SpringcurlBlock;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Wastes' new blocks in a world: Knotvine is climbed only where it covers a side; the diagonal course gives the water
 * table on a lower half or as the first course, plain when stacked on a diagonal (choice 39 as revised); bone meal is only
 * spent on a plant that can grow or has room nearby; the moss carpet's drapes catch up with the block beside the one below.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesBlockGameTests {
	private static final String COURSE = "trade_bot_course";

	private WastesBlockGameTests() {}

	private static void check(boolean ok, String message) {
		if (!ok)
			throw new GameTestAssertException(message);
	}

	private static BlockState cover(Block block, Direction... faces) {
		BlockState s = block.defaultBlockState();
		for (Direction d : faces)
			s = s.setValue(MultifaceBlock.getFaceProperty(d), true);
		return s;
	}

	/** Whether a body standing in {@code rel} climbs, by the block's own rule and by an armour stand there. */
	private static boolean climbs(GameTestHelper helper, BlockPos rel) {
		ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, rel);
		BlockPos abs = helper.absolutePos(rel);
		boolean ladder = helper.getLevel().getBlockState(abs).isLadder(helper.getLevel(), abs, stand), on = stand.onClimbable();
		stand.discard();
		check(ladder == on, rel + ": the block's rule and the stand disagree");
		return on;
	}

	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void knotvineIsClimbedOnlyOnASide(GameTestHelper helper) {
		Block knotvine = ModWastes.KNOTVINE.get();
		// on a wall's south side, on a floor, under a ceiling, on a floor and a wall at once; Scale Lichen on a side
		helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
		helper.setBlock(new BlockPos(2, 2, 3), cover(knotvine, Direction.NORTH));
		helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
		helper.setBlock(new BlockPos(6, 2, 6), cover(knotvine, Direction.DOWN));
		helper.setBlock(new BlockPos(10, 4, 10), Blocks.STONE);
		helper.setBlock(new BlockPos(10, 3, 10), cover(knotvine, Direction.UP));
		helper.setBlock(new BlockPos(14, 1, 14), Blocks.STONE);
		helper.setBlock(new BlockPos(15, 2, 14), Blocks.STONE);
		helper.setBlock(new BlockPos(14, 2, 14), cover(knotvine, Direction.DOWN, Direction.EAST));
		helper.setBlock(new BlockPos(18, 2, 18), Blocks.STONE);
		helper.setBlock(new BlockPos(18, 2, 19), cover(ModWastes.SCALE_LICHEN.get(), Direction.NORTH));
		check(climbs(helper, new BlockPos(2, 2, 3)), "Knotvine on a side isn't climbable");
		check(!climbs(helper, new BlockPos(6, 2, 6)), "Knotvine on a floor is climbable");
		check(!climbs(helper, new BlockPos(10, 3, 10)), "Knotvine under a ceiling is climbable");
		check(climbs(helper, new BlockPos(14, 2, 14)), "Knotvine on a floor and a side isn't climbable");
		check(!climbs(helper, new BlockPos(18, 2, 19)), "Scale Lichen is climbable");
		helper.succeed();
	}

	/** The course a diagonal takes from a click on {@code rel}'s {@code face}, {@code y} up the clicked block. */
	private static CutDeepslateDiagonalBlock.Course place(GameTestHelper helper, BlockPos rel, Direction face, double y) {
		var player = helper.makeMockPlayer(GameType.CREATIVE);
		BlockPos at = helper.absolutePos(rel);
		var hit = new BlockHitResult(new Vec3(at.getX() + 0.5, at.getY() + y, at.getZ() + 0.5), face, at, false);
		var ctx = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(ModWastes.CUT_DEEPSLATE_BRICK_DIAGONAL.get()), hit);
		return ModWastes.CUT_DEEPSLATE_BRICK_DIAGONAL.get().getStateForPlacement(ctx).getValue(CutDeepslateDiagonalBlock.COURSE);
	}

	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void waterTableIsTheFirstCourseOnly(GameTestHelper helper) {
		var water = CutDeepslateDiagonalBlock.Course.WATER_TABLE;
		var course = CutDeepslateDiagonalBlock.Course.COURSE;
		BlockPos at = new BlockPos(3, 2, 3);
		helper.setBlock(at, Blocks.STONE);
		check(place(helper, at, Direction.UP, 1.0) == water, "on top of a plain block doesn't give the water table");
		helper.setBlock(at, ModWastes.CUT_DEEPSLATE_BRICK_DIAGONAL.get());
		check(place(helper, at, Direction.UP, 1.0) == course, "stacked on a diagonal course doesn't give the plain course");
		helper.setBlock(at, ModWastes.STAINED_END_STONE_BRICK_DIAGONAL.get());
		check(place(helper, at, Direction.UP, 1.0) == course, "stacked on a brick diagonal doesn't give the plain course");
		// a side's lower half still gives it, whatever stands below
		helper.setBlock(at.north().below(), ModWastes.CUT_DEEPSLATE_BRICK_DIAGONAL.get());
		check(place(helper, at, Direction.NORTH, 0.25) == water, "a side's lower half over a diagonal doesn't give the water table");
		check(place(helper, at, Direction.NORTH, 0.75) == course, "a side's upper half doesn't give the course");
		helper.succeed();
	}

	/** Whether bone meal used on {@code rel} is spent (and so taken). */
	private static boolean bonemeal(GameTestHelper helper, BlockPos rel) {
		ItemStack stack = new ItemStack(Items.BONE_MEAL);
		boolean used = BoneMealItem.applyBonemeal(stack, helper.getLevel(), helper.absolutePos(rel), helper.makeMockPlayer(GameType.SURVIVAL));
		check(used == stack.isEmpty(), rel + ": bone meal used but not spent, or spent but not used");
		return used;
	}

	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void boneMealIsOnlySpentWhereAPlantCanGrowOrSpread(GameTestHelper helper) {
		// a plant on a lone pillar: nowhere within bone meal's reach could a young one stand
		BlockPos plant = new BlockPos(5, 4, 5);
		for (BlockPos p : BlockPos.betweenClosed(plant.offset(-3, -2, -3), plant.offset(3, 2, 3)))
			helper.setBlock(p, Blocks.AIR);
		helper.setBlock(plant.below(), Blocks.STONE);
		helper.setBlock(plant, ModWastes.VIGIL_BELL.get());
		check(!bonemeal(helper, plant), "bone meal was spent on a Vigil Bell with no room to spread");
		helper.setBlock(plant, ModWastes.SPRINGCURL.get().defaultBlockState().setValue(SpringcurlBlock.GROWN, true));
		check(!bonemeal(helper, plant), "bone meal was spent on a grown Springcurl with no room to spread");
		helper.setBlock(plant, ModWastes.SPRINGCURL.get().defaultBlockState().setValue(SpringcurlBlock.GROWN, false));
		check(bonemeal(helper, plant), "bone meal wasn't spent on a young Springcurl");
		check(helper.getBlockState(plant).getValue(SpringcurlBlock.GROWN), "bone meal didn't grow the Springcurl");
		// a spot beside it to stand on
		helper.setBlock(plant, ModWastes.VIGIL_BELL.get());
		helper.setBlock(plant.east().below(), Blocks.STONE);
		check(bonemeal(helper, plant), "bone meal wasn't spent on a Vigil Bell with room beside it");
		helper.succeed();
	}

	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void mossCarpetsDrapesCatchUpWithTheBlockBesideTheOneBelow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos carpet = new BlockPos(4, 3, 4), under = carpet.below();
		for (BlockPos p : BlockPos.betweenClosed(under.offset(-1, 0, -1), carpet.offset(1, 0, 1)))
			helper.setBlock(p, Blocks.AIR);
		helper.setBlock(under, Blocks.STONE);
		BlockPos abs = helper.absolutePos(carpet);
		helper.setBlock(carpet, MossCarpetBlock.draped(ModWastes.DUSK_MOSS_CARPET.get().defaultBlockState(), level, abs));
		for (Direction d : Direction.Plane.HORIZONTAL)
			check(helper.getBlockState(carpet).getValue(MossCarpetBlock.DRAPES.get(d)), "no drape down the open " + d + " side");
		// a block set beside the one below sends the carpet no update; its random tick works the drapes out again
		helper.setBlock(under.east(), Blocks.STONE);
		helper.getBlockState(carpet).randomTick(level, abs, level.random);
		check(!helper.getBlockState(carpet).getValue(MossCarpetBlock.DRAPES.get(Direction.EAST)), "the drape still hangs into the block beside");
		check(helper.getBlockState(carpet).getValue(MossCarpetBlock.DRAPES.get(Direction.WEST)), "the open west drape went");
		helper.setBlock(under.east(), Blocks.AIR);
		helper.getBlockState(carpet).randomTick(level, abs, level.random);
		check(helper.getBlockState(carpet).getValue(MossCarpetBlock.DRAPES.get(Direction.EAST)), "the drape didn't come back once the side was open");
		helper.succeed();
	}
}
