package com.comedymonkey.createaddons.client.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.backend.BackendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Development only: A/B screenshots of the same view with Flywheel off (the renderers draw every part) and on, with
 * both of its backends (the machines' visuals draw them), to check the instanced parts look exactly like the drawn
 * ones. Write a label into {@code cmca-shots.txt} in the game folder; it switches the backend, waits for the world to
 * settle, saves {@code screenshots/cmca/<label>-<backend>.png} and appends the frame rate to {@code cmca-shots.fps},
 * then puts the backend and the settings it changed back and writes {@code cmca-shots.done}.
 */
public final class ShotRunner {
	private static final String[] BACKENDS = {"flywheel:off", "flywheel:instancing", "flywheel:indirect"};
	private static final int SETTLE_TICKS = 60;

	private static String label;
	private static int step = -1, wait, ticks;
	/** A label starting "gui-": one shot with the HUD showing (the goggles' overlay), on the backend as it is. */
	private static boolean gui;
	/** What it changes, to put back afterwards. */
	private static String backendBefore;
	private static boolean vsyncBefore, hideGuiBefore;
	private static int frameLimitBefore;

	private ShotRunner() {}

	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (FMLEnvironment.production || mc.level == null || mc.player == null)
			return;
		Path request = mc.gameDirectory.toPath().resolve("cmca-shots.txt");
		if (step < 0) {
			// its own count: game time stands still while the world is frozen for the shots
			if (++ticks % 10 != 0 || !Files.exists(request))
				return;
			try {
				label = Files.readString(request).trim();
				Files.delete(request);
			} catch (IOException e) {
				return;
			}
			step = 0;
			wait = 0;
			gui = label.startsWith("gui-");
			backendBefore = Backend.REGISTRY.getIdOrThrow(BackendManager.currentBackend()).toString();
			vsyncBefore = mc.options.enableVsync().get();
			frameLimitBefore = mc.options.framerateLimit().get();
			hideGuiBefore = mc.options.hideGui;
		}
		if (wait == 0) {
			if (!gui)
				ClientCommandHandler.runCommand("flywheel backend " + BACKENDS[step]);
			mc.options.hideGui = !gui; // no chat, hand or hotbar in the shots (unless the HUD is what's being looked at)
			// uncapped, so the frame rates measure the renderer, not the limit
			setVsync(mc, false);
			setFrameLimit(mc, 260);
		}
		if (++wait < SETTLE_TICKS)
			return;
		String name = gui ? "cmca/" + label + ".png" : "cmca/" + label + "-" + BACKENDS[step].substring("flywheel:".length()) + ".png";
		Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
		try {
			Files.writeString(mc.gameDirectory.toPath().resolve("cmca-shots.fps"), label + " " + BACKENDS[step] + " " + mc.getFps() + " fps\n",
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException ignored) {
		}
		wait = 0;
		if (++step == BACKENDS.length || gui) {
			step = -1;
			ClientCommandHandler.runCommand("flywheel backend " + backendBefore);
			mc.options.hideGui = hideGuiBefore;
			setVsync(mc, vsyncBefore);
			setFrameLimit(mc, frameLimitBefore);
			try {
				Files.writeString(mc.gameDirectory.toPath().resolve("cmca-shots.done"), label);
			} catch (IOException ignored) {
			}
		}
	}

	private static void setVsync(Minecraft mc, boolean on) {
		mc.options.enableVsync().set(on);
		mc.getWindow().updateVsync(on);
	}

	private static void setFrameLimit(Minecraft mc, int limit) {
		mc.options.framerateLimit().set(limit);
		mc.getWindow().setFramerateLimit(limit);
	}
}
