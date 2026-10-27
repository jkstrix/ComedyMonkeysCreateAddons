package com.comedymonkey.createaddons.gametest;

import java.util.Arrays;
import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlock;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity;
import com.comedymonkey.createaddons.content.helperbot.HelperBotMovement;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What Helper Bot's drawing reads from the server, in a real world (the drawing itself is client-only, so it's covered
 * by the unit tests under client/helperbot): his colour reaches the data Create captures when a contraption is
 * assembled (his riding visual and the Flywheel-off contraption drawing take it from there) and the update a client
 * receives (his placed visual follows it), and Create finds his movement behaviour, which keeps his block entity from
 * being drawn on contraptions so that only he draws himself.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HelperBotRenderGameTests {
	/** 24 x 12 x 24 of air (the Battery tests' course). */
	private static final String COURSE = "battery_course";

	private HelperBotRenderGameTests() {}

	/** Every colour and plain, placed from the item as a player does. */
	private static List<DyeColor> colours() {
		List<DyeColor> out = new java.util.ArrayList<>(Arrays.asList(DyeColor.values()));
		out.add(null);
		return out;
	}

	private static HelperBotBlockEntity place(GameTestHelper helper, BlockPos pos, DyeColor dye) {
		ItemStack stack = new ItemStack(ModItems.HELPER_BOT.get());
		if (dye != null)
			stack.set(ModDataComponents.HELPER_BOT_DYE.get(), dye);
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos abs = helper.absolutePos(pos);
		stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
		// (clicked air is replaced: he stands where he was aimed)
		helper.assertBlockPresent(ModBlocks.HELPER_BOT.get(), pos);
		if (!(helper.getBlockEntity(pos) instanceof HelperBotBlockEntity be))
			throw new GameTestAssertException("no Helper Bot block entity at " + pos);
		return be;
	}

	private static void same(DyeColor actual, DyeColor expected, String what) {
		if (actual != expected)
			throw new GameTestAssertException(what + ": expected " + expected + ", got " + actual);
	}

	/** The data Create keeps for him on a contraption carries his colour, as the client reads it. */
	@GameTest(template = COURSE)
	public static void colourReachesContraptionData(GameTestHelper helper) {
		int x = 1;
		for (DyeColor dye : colours()) {
			HelperBotBlockEntity be = place(helper, new BlockPos(x, 0, 2), dye);
			x++;
			// what Contraption.getBlockEntityNBT keeps: the block entity's full save, without its position
			CompoundTag captured = be.saveWithFullMetadata(helper.getLevel().registryAccess());
			captured.remove("x");
			captured.remove("y");
			captured.remove("z");
			same(HelperBotBlockEntity.dyeOf(captured), dye, "colour on a contraption");
		}
		helper.succeed();
	}

	/** The update a watching client gets carries his colour, and loading it changes the colour a client sees. */
	@GameTest(template = COURSE)
	public static void colourReachesClientUpdates(GameTestHelper helper) {
		BlockPos at = new BlockPos(3, 0, 3);
		HelperBotBlockEntity plain = place(helper, at, null);
		same(plain.dye(), null, "plain");
		for (DyeColor dye : DyeColor.values()) {
			// the server's bot in this colour, and what it tells clients
			HelperBotBlockEntity source = new HelperBotBlockEntity(plain.getBlockPos(), plain.getBlockState());
			CompoundTag nbt = new CompoundTag();
			nbt.putString(HelperBotBlockEntity.DYE, dye.getName());
			source.loadWithComponents(nbt, helper.getLevel().registryAccess());
			CompoundTag update = source.getUpdateTag(helper.getLevel().registryAccess());
			// a client's copy, loading the update: his visual compares this each frame
			HelperBotBlockEntity client = new HelperBotBlockEntity(plain.getBlockPos(), plain.getBlockState());
			client.loadWithComponents(update, helper.getLevel().registryAccess());
			same(client.dye(), dye, "colour after an update");
		}
		helper.succeed();
	}

	/** Create finds his movement behaviour for every facing and pose, and leaves the drawing on contraptions to it. */
	@GameTest(template = COURSE)
	public static void contraptionsLeaveHisDrawingToHim(GameTestHelper helper) {
		for (Direction facing : Direction.Plane.HORIZONTAL)
			for (boolean ducked : new boolean[] { false, true }) {
				var state = ModBlocks.HELPER_BOT.get().defaultBlockState().setValue(HelperBotBlock.FACING, facing)
					.setValue(HelperBotBlock.DUCKED, ducked);
				MovementBehaviour behaviour = MovementBehaviour.REGISTRY.get(state);
				if (!(behaviour instanceof HelperBotMovement))
					throw new GameTestAssertException("no Helper Bot movement behaviour for " + state + ": " + behaviour);
				if (!behaviour.disableBlockEntityRendering())
					throw new GameTestAssertException("the contraption would draw his block entity as well as him");
			}
		helper.succeed();
	}
}
