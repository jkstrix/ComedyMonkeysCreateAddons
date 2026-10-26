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

	/**
	 * A label "ponder-&lt;item&gt;-&lt;scene&gt;-&lt;keyframe&gt;" (an item of this mod; scenes from 0, keyframes from 1, 0 the start): that item's Ponder,
	 * at that scene's keyframe once its text is up, as {@code screenshots/cmca/<label>.png}.
	 */
	private static boolean ponder;
	private static int ponderScene, ponderKeyframe;
	@javax.annotation.Nullable
	private static net.createmod.ponder.foundation.ui.PonderUI ponderUi;

	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (FMLEnvironment.production || mc.level == null || mc.player == null)
			return;
		Path request = mc.gameDirectory.toPath().resolve("cmca-shots.txt");
		if (ponder) {
			ponderTick(mc);
			return;
		}
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
			if (label.startsWith("ponder-")) {
				openPonder(mc);
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
			Files.writeString(mc.gameDirectory.toPath().resolve("cmca-shots.fps"), label + " " + (gui ? backendBefore : BACKENDS[step]) + " " + mc.getFps() + " fps\n",
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

	private static void openPonder(Minecraft mc) {
		try {
			String[] p = label.substring("ponder-".length()).split("-");
			ponderScene = Integer.parseInt(p[1]);
			ponderKeyframe = Integer.parseInt(p[2]);
			ponderUi = net.createmod.ponder.foundation.ui.PonderUI.of(new net.minecraft.world.item.ItemStack(
				net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("comedymonkeysaddons", p[0]))));
			mc.setScreen(ponderUi);
			ponder = true;
			wait = 0;
		} catch (RuntimeException e) {
			ponderFailed(mc, "the label isn't ponder-<item>-<scene>-<keyframe>: " + e);
		}
	}

	/** Through the Ponder: to the scene (scrolled to, as the arrows do), then the keyframe, then the shot once its text is up. */
	private static void ponderTick(Minecraft mc) {
		var ui = ponderUi;
		wait++;
		try {
			if (ui == null || mc.screen != ui) {
				ponderFailed(mc, "the Ponder screen closed");
				return;
			}
			if (wait == 10 && ponderScene > 0) {
				java.lang.reflect.Method scroll = net.createmod.ponder.foundation.ui.PonderUI.class.getDeclaredMethod("scroll", boolean.class);
				scroll.setAccessible(true);
				for (int i = 0; i < ponderScene; i++)
					scroll.invoke(ui, true);
			}
			// the scroll eases over to the next scene: then the keyframe
			if (wait == 40) {
				java.lang.reflect.Field index = net.createmod.ponder.foundation.ui.PonderUI.class.getDeclaredField("index");
				index.setAccessible(true);
				if (index.getInt(ui) != ponderScene) {
					ponderFailed(mc, "it has no scene " + ponderScene);
					return;
				}
				var scene = ui.getActiveScene();
				if (ponderKeyframe < 0 || ponderKeyframe > scene.getKeyframeCount()) {
					ponderFailed(mc, "its scene has no keyframe " + ponderKeyframe);
					return;
				}
				ui.seekToTime(ponderKeyframe == 0 ? 0 : scene.getKeyframeTime(ponderKeyframe - 1));
			}
			if (wait == 80) {
				Screenshot.grab(mc.gameDirectory, "cmca/" + label + ".png", mc.getMainRenderTarget(), message -> {});
				Files.writeString(mc.gameDirectory.toPath().resolve("cmca-shots.done"), label);
				mc.setScreen(null);
				ponder = false;
			}
		} catch (ReflectiveOperationException | IOException | RuntimeException e) {
			ponderFailed(mc, e.toString());
		}
	}

	/** A Ponder shot that couldn't be taken: said in the log and in the done file, and the screen closed. */
	private static void ponderFailed(Minecraft mc, String why) {
		ponder = false;
		com.mojang.logging.LogUtils.getLogger().warn("Ponder shot {} failed: {}", label, why);
		if (mc.screen == ponderUi)
			mc.setScreen(null);
		try {
			Files.writeString(mc.gameDirectory.toPath().resolve("cmca-shots.done"), "FAILED " + label + ": " + why);
		} catch (IOException ignored) {
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
