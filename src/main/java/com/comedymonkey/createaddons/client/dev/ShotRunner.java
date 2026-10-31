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
 * then puts the backend and the settings it changed back and writes {@code cmca-shots.done}. Commands written into
 * {@code cmca-commands.txt}, one a line, are run as the player first (to set the time or the view).
 */
public final class ShotRunner {
	private static final String[] BACKENDS = {"flywheel:off", "flywheel:instancing", "flywheel:indirect"};
	private static final int SETTLE_TICKS = 60;

	private static String label;
	private static int step = -1, wait, ticks;
	/** A label starting "gui-": one shot with the HUD showing (the goggles' overlay), on the backend as it is. */
	private static boolean gui;
	/** A label starting "burst-": as "gui-", but {@link #BURST} frames in a row (one a tick), to catch flicker. */
	private static boolean burst;
	private static final int BURST = 16;
	private static int burstFrame;
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

	/**
	 * Whether the world named by -Dcmaddons.openWorld has been opened (once the title screen is up: runClientWorld); a
	 * name with no save makes a new flat creative world by that name.
	 */
	private static boolean worldOpened;

	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		String world = System.getProperty("cmaddons.openWorld");
		if (world != null && !worldOpened && !FMLEnvironment.production && mc.getOverlay() == null
			&& mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
			// from the title screen, not quick play: Inventory Profiles Next needs its screens set up first
			worldOpened = true;
			if (mc.getLevelSource().levelExists(world)) {
				mc.createWorldOpenFlows().openWorld(world, () -> mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
				return;
			}
			// no such save: a new flat creative world with cheats, by that name (for test streets and courses)
			var settings = new net.minecraft.world.level.LevelSettings(world, net.minecraft.world.level.GameType.CREATIVE, false,
				net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(),
				net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
			mc.createWorldOpenFlows().createFreshLevel(world, settings, new net.minecraft.world.level.levelgen.WorldOptions(0, false, false),
				registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
					.getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),
				new net.minecraft.client.gui.screens.TitleScreen());
			return;
		}
		if (FMLEnvironment.production || mc.level == null || mc.player == null)
			return;
		Path request = mc.gameDirectory.toPath().resolve("cmca-shots.txt");
		// commands to run as the player first (time, teleport, which way to look), one a line: cmca-commands.txt
		Path commands = mc.gameDirectory.toPath().resolve("cmca-commands.txt");
		if (step < 0 && !ponder && Files.exists(commands)) {
			try {
				// read and deleted before they're run, so a file that can't be deleted isn't run every tick
				java.util.List<String> lines = Files.readAllLines(commands);
				Files.delete(commands);
				for (String line : lines)
					if (!line.isBlank())
						mc.player.connection.sendCommand(line.strip().replaceFirst("^/", ""));
			} catch (IOException e) {
				return;
			}
		}
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
			burst = label.startsWith("burst-");
			burstFrame = 0;
			gui = burst || label.startsWith("gui-");
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
		if (burst) {
			Screenshot.grab(mc.gameDirectory, String.format("cmca/%s-%02d.png", label, burstFrame), mc.getMainRenderTarget(), message -> {});
			if (++burstFrame < BURST) {
				wait = SETTLE_TICKS - 1;
				return;
			}
		}
		String name = gui ? "cmca/" + label + ".png" : "cmca/" + label + "-" + BACKENDS[step].substring("flywheel:".length()) + ".png";
		if (!burst)
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
