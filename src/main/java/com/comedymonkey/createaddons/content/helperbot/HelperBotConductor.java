package com.comedymonkey.createaddons.content.helperbot;

import com.simibubi.create.api.behaviour.interaction.ConductorBlockInteractionBehavior;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Makes Helper Bot a train conductor through Create's own API, the same one the Blaze Burner uses (D7–D9): directly
 * behind Train Controls facing him he drives, and schedules are handed over, taken back and resumed exactly as Create
 * does it for a Blaze Burner.
 */
public class HelperBotConductor extends ConductorBlockInteractionBehavior {
	@Override
	public boolean isValidConductor(BlockState state) {
		return state.getBlock() instanceof HelperBotBlock;
	}
}
