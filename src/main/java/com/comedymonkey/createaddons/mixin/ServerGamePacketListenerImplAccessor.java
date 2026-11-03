package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * The Clockwork Jetpack keeps its wearer up without Creative flight, which the server would otherwise take for cheating
 * ("Flying is not enabled on this server") after a few seconds in the air: while it thrusts or hovers, that count is reset.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {
	@Accessor("aboveGroundTickCount")
	void setAboveGroundTickCount(int ticks);
}
