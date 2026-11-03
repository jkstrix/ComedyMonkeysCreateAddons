package com.comedymonkey.createaddons.client.wastes;

import com.comedymonkey.createaddons.content.wastes.SubstationBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** D96: looking at a dormant Substation says, on the action bar, what to do with it ("Hold to throw the breaker"). */
public final class BreakerHint {
	private BreakerHint() {}

	/** A few times a second, while the crosshair is on a dormant (locked) Substation in reach. */
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.isPaused() || mc.player.isSpectator() || mc.player.tickCount % 5 != 0)
			return;
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
			return;
		BlockState state = mc.level.getBlockState(hit.getBlockPos());
		if (state.getBlock() instanceof SubstationBlock && state.getValue(SubstationBlock.LOCKED))
			mc.player.displayClientMessage(Component.translatable("comedymonkeysaddons.wastes.breaker_hint"), true);
	}
}
