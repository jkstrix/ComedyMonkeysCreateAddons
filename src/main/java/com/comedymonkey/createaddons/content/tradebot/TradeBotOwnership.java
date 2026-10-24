package com.comedymonkey.createaddons.content.tradebot;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Who may control a Trade Bot (D23): his owner, players on the owner's scoreboard team, and creative-mode operators.
 * Team mods (FTB Teams and the like) can be added here later.
 */
public final class TradeBotOwnership {
	private TradeBotOwnership() {}

	public static boolean canControl(Player player, @Nullable UUID owner) {
		if (owner == null || player.getUUID().equals(owner))
			return true;
		if (player.isCreative() && player.hasPermissions(2))
			return true;
		PlayerTeam team = player.getTeam();
		if (team == null)
			return false;
		Player ownerPlayer = player.level().getPlayerByUUID(owner);
		return ownerPlayer != null && team.equals(ownerPlayer.getTeam());
	}
}
