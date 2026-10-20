package com.comedymonkey.createaddons.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Finding, on the client, the block entity a menu's screen is for. */
public final class ScreenBlockEntities {
	private ScreenBlockEntities() {}

	/**
	 * The block entity at {@code pos}, or, if it's already gone (the block was broken just as the screen opened), an
	 * empty stand-in so the screen can open without crashing; the server closes it straight away anyway.
	 */
	public static <T extends BlockEntity> T find(Inventory inventory, BlockPos pos, BlockEntityType<T> type, Block block) {
		Level level = inventory.player.level();
		T found = level.getBlockEntity(pos, type).orElse(null);
		if (found != null)
			return found;
		T standIn = type.create(pos, block.defaultBlockState());
		if (standIn == null)
			throw new IllegalStateException("Couldn't make a " + type + " for a screen at " + pos);
		standIn.setLevel(level);
		return standIn;
	}
}
