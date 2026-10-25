package com.comedymonkey.createaddons.content.tradebot.work;

import com.comedymonkey.createaddons.content.chest.UpgradedBarrelBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.logistics.depot.DepotBlock;

import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How Trade Bot handles each kind of container (design/briefs/trade-bot.md, D5, D6, D36): which clip he plays, how far
 * from its face he stands (from the Blockbench preview's props, in pixels), and when, in seconds into the clip, the lid
 * opens, the items move and the lid closes (from the clips' sound cues). "Blind" containers, ones he can't look into
 * (barrels, high chests, vaults and modded containers), open by themselves and the items just appear or vanish.
 */
public enum ContainerKind {
	/** A chest at his level: he opens the lid, rummages, finds it, and shuts it. */
	CHEST("chest_take", "chest_put", 8, 0.5f, 3.45f, 2.0f, 4.45f, 3.15f, true),
	/** A chest one block up: it opens by itself, the item appears in his claw, he looks confused. */
	CHEST_HIGH("chest_take_high", "chest_put_high", 16, 0.5f, 1.3f, 1.18f, 3.7f, 3.7f, true),
	/** A barrel: he stands half a block back, it opens by itself, the item appears. */
	BARREL("barrel_take", "barrel_put", 18, 0.5f, 1.3f, 1.18f, 3.7f, 3.7f, true),
	/** Anything else that holds items (vaults, modded containers): the barrel routine, with nothing to open. */
	BLIND("barrel_take", "barrel_put", 18, -1, 1.3f, 1.18f, -1, -1, false),
	/** A Create depot: picked up with a grip, or set down and nudged into the middle. */
	DEPOT("depot_take", "depot_place", 8, -1, 2.05f, 1.05f, -1, -1, false),
	/** A Create belt: deliveries only. */
	BELT(null, "belt_place", 8, -1, -1, 1.0f, -1, -1, false);

	public final String takeClip, putClip;
	/** Pixels from the container's face to his centre. */
	public final int standoff;
	public final float openAt, takeAt, putAt, closeAfterTake, closeAfterPut;
	public final boolean opens;

	ContainerKind(String takeClip, String putClip, int standoff, float openAt, float takeAt, float putAt, float closeAfterTake,
		float closeAfterPut, boolean opens) {
		this.takeClip = takeClip;
		this.putClip = putClip;
		this.standoff = standoff;
		this.openAt = openAt;
		this.takeAt = takeAt;
		this.putAt = putAt;
		this.closeAfterTake = closeAfterTake;
		this.closeAfterPut = closeAfterPut;
		this.opens = opens;
	}

	/** The kind for a container, standing at its level ({@code high}: it's a block above his floor). */
	public static ContainerKind of(BlockState state, boolean high) {
		if (state.getBlock() instanceof BeltBlock)
			return BELT;
		if (state.getBlock() instanceof DepotBlock)
			return DEPOT;
		// upgraded chests and barrels open like the vanilla ones
		if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof UpgradedChestBlock)
			return high ? CHEST_HIGH : CHEST;
		if (state.getBlock() instanceof BarrelBlock || state.getBlock() instanceof UpgradedBarrelBlock)
			return BARREL;
		return BLIND;
	}

	public String clip(boolean taking) {
		return taking ? takeClip : putClip;
	}

	public float transferAt(boolean taking) {
		return taking ? takeAt : putAt;
	}

	public float closeAt(boolean taking) {
		return taking ? closeAfterTake : closeAfterPut;
	}
}
