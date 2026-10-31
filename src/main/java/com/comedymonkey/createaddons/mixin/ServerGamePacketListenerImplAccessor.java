package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * A player riding a Clockwork Wastes lift's deck up is held up by an entity, not a block, so the server would count it
 * as floating and kick it after four seconds (Create's contraptions reset the same count for their riders).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {
	@Accessor("aboveGroundTickCount")
	void comedymonkeysaddons$setAboveGroundTickCount(int ticks);

	@Accessor("aboveGroundTickCount")
	int comedymonkeysaddons$getAboveGroundTickCount();
}
