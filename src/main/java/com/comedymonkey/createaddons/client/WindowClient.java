package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.content.ClientView;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The windowed machines' (the Monster Cages', the Fish Tanks') client side: how far the camera is from each, which of
 * their worlds count their display creatures' builds, and the per-tick and per-frame counts behind both.
 */
final class WindowClient {
	private WindowClient() {}

	static void install() {
		ClientView.distanceSqr = be -> {
			if (be.getLevel() != Minecraft.getInstance().level)
				return 0;
			BlockPos pos = be.getBlockPos();
			return Hearing.distanceSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		};
		ClientView.counted = level -> level == Minecraft.getInstance().level;
		NeoForge.EVENT_BUS.addListener(WindowClient::onClientTick);
		NeoForge.EVENT_BUS.addListener(NearestLoops::onClientTick);
		NeoForge.EVENT_BUS.addListener(CameraView::onFrame);
		NeoForge.EVENT_BUS.addListener(CameraView::onFrameEnd);
	}

	/** Every client tick, paused or not, the builds start again. */
	private static void onClientTick(ClientTickEvent.Pre event) {
		ClientView.BUILDS.newTick();
	}
}
