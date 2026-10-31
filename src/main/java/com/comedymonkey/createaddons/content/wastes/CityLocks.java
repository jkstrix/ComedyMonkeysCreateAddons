package com.comedymonkey.createaddons.content.wastes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Locked city blocks (design/specs/clockwork-wastes.md, P21, P43, D42): doors, vaults, Substations and lifts until their
 * district wakes; the heart's reactor until it's salvaged; the pylons always. While locked a block has bedrock's
 * hardness (-1), so nothing but a player's hands can ever break one: Create's drills and saws pass them by and
 * contraptions can't carry them off (Create treats hardness -1 as immovable), nor can pistons. A {@link Lockable}
 * block's hardness is -1 only while its state is locked ({@code BlockStateBaseMixin}); woken, it's an ordinary block
 * that players and contraptions break and move (D42). The pylons and the heart's reactor keep -1 in their properties.
 * A player's breaking goes through {@link #progress}: none while locked, the block's ordinary hardness once open. Blasts
 * ask {@link #resistance}.
 */
public final class CityLocks {
	/** Like bedrock: no blast moves it. */
	public static final float LOCKED_RESISTANCE = 3_600_000f;

	private CityLocks() {}

	/** A city block whose lock is in its state: its hardness is bedrock's while {@link #locked}, its own once open (D42). */
	public interface Lockable {
		boolean locked(BlockState state);
	}

	/** How far a player's swing breaks it: not at all while locked, else as a block of {@code hardness} would. */
	public static float progress(BlockState state, Player player, BlockGetter level, BlockPos pos, boolean locked, float hardness) {
		if (locked)
			return 0;
		int scale = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, state, level, pos) ? 30 : 100;
		return player.getDigSpeed(state, pos) / hardness / scale;
	}

	/** Its blast resistance: bedrock's while locked. */
	public static float resistance(boolean locked, float resistance) {
		return locked ? LOCKED_RESISTANCE : resistance;
	}
}
