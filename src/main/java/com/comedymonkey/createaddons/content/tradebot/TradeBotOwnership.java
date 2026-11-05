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

	/**
	 * The owner of a Trade Bot a Schematicannon printed: nobody controls him (bar creative-mode operators), but anyone may
	 * pack him up, and whoever places him again owns him (user, 2026-11-05).
	 */
	public static final UUID NOBODY = new UUID(0L, 0L);

	public static boolean canControl(Player player, @Nullable UUID owner) {
		if (NOBODY.equals(owner))
			return player.isCreative() && player.hasPermissions(2);
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

	/** Who may pack a Trade Bot up: whoever controls him, and anyone at all when he's nobody's (or printed: {@link #NOBODY}). */
	public static boolean canPackUp(Player player, @Nullable UUID owner) {
		return owner == null || NOBODY.equals(owner) || canControl(player, owner);
	}
}
