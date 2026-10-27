package com.comedymonkey.createaddons.client.dev;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.slf4j.Logger;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Development only: renders the website's pictures with the game's own renderers. Runs only when the game is started
 * with {@code -Dcmaddons.renders=<repo root>} ({@code ./gradlew runRenders}); otherwise it does nothing at all.
 * <p>
 * It makes a fresh flat creative world, then for each scene in {@code tools/website/renders.json} flies over to its own
 * spot, builds it with commands, lets it run, and draws just the scene's blocks (and any entities in them) into a
 * transparent image: {@code docs/assets/renders/<id>.png}. Then it draws an icon for every item and fluid named
 * anywhere in {@code docs/data} (and every item of this mod) into {@code docs/assets/icons/<namespace>/<path>.png},
 * writes their names to {@code docs/data/names.json}, and quits.
 * <p>
 * Drawing: blocks through the block renderer, block entities and entities through their renderers, with Flywheel's
 * backend off so the renderers draw every part. The view is an orthographic three-quarter view; the camera's
 * rotation goes on the model-view matrix and its position on the pose, as the level renderer does, so lighting and
 * every renderer's idea of where the camera is come out as they do in the world.
 */
@EventBusSubscriber(modid = ComedyMonkeysCreateAddons.MOD_ID, value = Dist.CLIENT)
public final class RenderExporter {
	private static final Logger LOG = LogUtils.getLogger();
	private static final String ROOT = System.getProperty("cmaddons.renders");
	private static final String WORLD = "cmaddons-renders";
	private static final Pattern ID = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_/.-]+$");
	/** How far the camera stands back from a scene's middle: only matters to renderers that look at where it is. */
	private static final double CAMERA_DISTANCE = 24;

	private enum Phase { TITLE, CREATING, LOADING, SETTLING, SHOTS, ICONS, DONE }

	private static Phase phase = Phase.TITLE;
	private static int wait;
	private static JsonObject config;
	private static List<Shot> shots;
	private static int shot, shotStep;
	private static final List<String> report = new ArrayList<>();

	/** One scene: {@code settle} is how many ticks it runs after it's built, before it's drawn. */
	private record Shot(String id, BlockPos origin, List<String> setup, BlockPos min, BlockPos max, float yaw, float pitch, int settle,
		int pixelsPerBlock) {}

	private RenderExporter() {}

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		if (ROOT == null || phase == Phase.DONE)
			return;
		Minecraft mc = Minecraft.getInstance();
		try {
			tick(mc);
		} catch (Exception e) {
			LOG.error("Render export failed", e);
			report.add("FAILED: " + e);
			finish(mc);
		}
	}

	private static void tick(Minecraft mc) throws Exception {
		mc.options.pauseOnLostFocus = false;
		switch (phase) {
			case TITLE -> {
				// first launch asks about accessibility first; anything else that isn't the title screen, wait
				if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) {
					mc.options.onboardAccessibility = false;
					mc.options.save();
					mc.setScreen(new TitleScreen());
				}
				if (!(mc.screen instanceof TitleScreen))
					return;
				config = readJson(Path.of(ROOT, "tools/website/renders.json")).getAsJsonObject();
				shots = readShots(config);
				deleteWorld(mc);
				CreateWorldScreen.openFresh(mc, mc.screen);
				phase = Phase.CREATING;
			}
			case CREATING -> {
				if (!(mc.screen instanceof CreateWorldScreen screen))
					return;
				WorldCreationUiState ui = screen.getUiState();
				ui.setName(WORLD);
				ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				ui.setAllowCommands(true);
				ui.setGenerateStructures(false);
				ui.getNormalPresetList().stream()
					.filter(t -> t.preset() != null && t.preset().is(WorldPresets.FLAT))
					.findFirst().ifPresent(ui::setWorldType);
				var create = CreateWorldScreen.class.getDeclaredMethod("onCreate");
				create.setAccessible(true);
				create.invoke(screen);
				phase = Phase.LOADING;
			}
			case LOADING -> {
				if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null)
					return;
				for (String c : List.of("gamerule doDaylightCycle false", "time set 6000", "gamerule doWeatherCycle false", "weather clear",
					"gamerule doMobSpawning false", "gamerule randomTickSpeed 0", "gamemode spectator @a"))
					command(c);
				// Flywheel off: every renderer draws all of its parts itself
				ClientCommandHandler.runCommand("flywheel backend flywheel:off");
				mc.options.hideGui = true;
				wait = 40;
				phase = Phase.SETTLING;
			}
			case SETTLING -> {
				if (--wait <= 0) {
					phase = Phase.SHOTS;
					shot = 0;
					shotStep = 0;
				}
			}
			case SHOTS -> shotTick(mc);
			case ICONS -> {
				icons(mc);
				finish(mc);
			}
			default -> {}
		}
	}

	/** One scene at a time: fly there, build it, let it run, draw it. */
	private static void shotTick(Minecraft mc) throws IOException {
		if (shot >= shots.size()) {
			phase = Phase.ICONS;
			return;
		}
		Shot s = shots.get(shot);
		if (wait > 0) {
			wait--;
			return;
		}
		switch (shotStep) {
			case 0 -> {
				BlockPos o = s.origin();
				command("tp @a " + (o.getX() + 0.5) + " " + (o.getY() + 3) + " " + (o.getZ() + 10) + " 180 15");
				wait = 15;
				shotStep = 1;
			}
			case 1 -> {
				for (String c : s.setup()) {
					if (c.startsWith("!use "))
						use(s.origin(), c);
					else
						command("execute positioned " + s.origin().getX() + " " + s.origin().getY() + " " + s.origin().getZ() + " run " + c);
				}
				wait = s.settle();
				shotStep = 2;
			}
			default -> {
				Path out = Path.of(ROOT, "docs/assets/renders", s.id() + ".png");
				Files.createDirectories(out.getParent());
				NativeImage image = renderScene(mc, s);
				// a scene that drew nothing writes nothing, so the site falls back to the icon rather than a blank
				if (isEmpty(image)) {
					Files.deleteIfExists(out);
					report.add("EMPTY " + s.id() + ": nothing was drawn (check its commands in the log)");
				} else {
					image.writeToFile(out);
					report.add("render " + s.id() + " " + image.getWidth() + "x" + image.getHeight());
				}
				image.close();
				shot++;
				shotStep = 0;
			}
		}
	}

	// ---------------------------------------------------------------- scenes

	private static NativeImage renderScene(Minecraft mc, Shot s) {
		ClientLevel level = mc.level;
		BlockPos a = s.origin().offset(s.min()), b = s.origin().offset(s.max());
		AABB box = new AABB(Vec3.atLowerCornerOf(a), Vec3.atLowerCornerOf(b).add(1, 1, 1));
		List<Entity> entities = level.getEntities((Entity) null, box.inflate(0.25), e -> !(e instanceof Player));

		// the view: as the level renderer turns the world for a camera with this yaw and pitch
		Matrix4f view = new Matrix4f().rotateX(s.pitch() * Mth.DEG_TO_RAD).rotateY((s.yaw() + 180) * Mth.DEG_TO_RAD);
		Vec3 look = Vec3.directionFromRotation(s.pitch(), s.yaw());
		Vec3 camera = box.getCenter().subtract(look.scale(CAMERA_DISTANCE));

		// fit the scene's box (and its entities) with a margin for parts that hang outside their block
		float[] bounds = {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
		fit(bounds, view, box.inflate(0.4), camera);
		for (Entity e : entities)
			fit(bounds, view, e.getBoundingBox().inflate(0.2), camera);
		float w = bounds[1] - bounds[0], h = bounds[3] - bounds[2];
		int supersample = config.has("supersample") ? config.get("supersample").getAsInt() : 2;
		int maxSize = config.has("maxSize") ? config.get("maxSize").getAsInt() : 1600;
		float ppu = Math.min(s.pixelsPerBlock(), maxSize / Math.max(w, h)) * supersample;
		int width = Math.max(1, Math.round(w * ppu)), height = Math.max(1, Math.round(h * ppu));
		Matrix4f projection = new Matrix4f().setOrtho(bounds[0], bounds[1], bounds[2], bounds[3], -bounds[5] - 2, -bounds[4] + 2);

		TextureTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
		target.setClearColor(0, 0, 0, 0);
		target.clear(Minecraft.ON_OSX);
		target.bindWrite(true);
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.set(view);
		RenderSystem.applyModelViewMatrix();
		Lighting.setupLevel();
		FogRenderer.setupNoFog();

		float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		BlockRenderDispatcher blocks = mc.getBlockRenderer();
		PoseStack pose = new PoseStack();
		for (BlockPos pos : BlockPos.betweenClosed(a, b)) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir())
				continue;
			pose.pushPose();
			pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
			if (state.getRenderShape() == RenderShape.MODEL) {
				BakedModel model = blocks.getBlockModel(state);
				ModelData data = model.getModelData(level, pos, state, level.getModelData(pos));
				RandomSource random = RandomSource.create(state.getSeed(pos));
				for (RenderType type : model.getRenderTypes(state, random, data))
					blocks.renderBatched(state, pos, level, pose, buffers.getBuffer(type), true, RandomSource.create(state.getSeed(pos)), data, type);
			}
			BlockEntity be = level.getBlockEntity(pos);
			if (be != null)
				mc.getBlockEntityRenderDispatcher().render(be, partialTick, pose, buffers);
			pose.popPose();
		}
		var entityRenderer = mc.getEntityRenderDispatcher();
		entityRenderer.setRenderShadow(false);
		for (Entity e : entities) {
			double x = Mth.lerp(partialTick, e.xOld, e.getX()) - camera.x;
			double y = Mth.lerp(partialTick, e.yOld, e.getY()) - camera.y;
			double z = Mth.lerp(partialTick, e.zOld, e.getZ()) - camera.z;
			entityRenderer.render(e, x, y, z, e.getViewYRot(partialTick), partialTick, pose, buffers, entityRenderer.getPackedLightCoords(e, partialTick));
		}
		entityRenderer.setRenderShadow(true);
		buffers.endBatch();

		NativeImage image = read(target);
		modelView.popMatrix();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.restoreProjectionMatrix();
		target.destroyBuffers();
		mc.getMainRenderTarget().bindWrite(true);
		return finishImage(image, supersample, true);
	}

	/** Widens view-space bounds (x, y and depth) to take in a box. */
	private static void fit(float[] bounds, Matrix4f view, AABB box, Vec3 camera) {
		for (int i = 0; i < 8; i++) {
			Vector3f p = new Vector3f(
				(float) (((i & 1) == 0 ? box.minX : box.maxX) - camera.x),
				(float) (((i & 2) == 0 ? box.minY : box.maxY) - camera.y),
				(float) (((i & 4) == 0 ? box.minZ : box.maxZ) - camera.z));
			view.transformPosition(p);
			bounds[0] = Math.min(bounds[0], p.x);
			bounds[1] = Math.max(bounds[1], p.x);
			bounds[2] = Math.min(bounds[2], p.y);
			bounds[3] = Math.max(bounds[3], p.y);
			bounds[4] = Math.min(bounds[4], p.z);
			bounds[5] = Math.max(bounds[5], p.z);
		}
	}

	// ---------------------------------------------------------------- icons

	/** An icon for every item and fluid the site's data names, and every item of this mod, plus their names. */
	private static void icons(Minecraft mc) throws IOException {
		Set<String> ids = new LinkedHashSet<>();
		BuiltInRegistries.ITEM.keySet().stream()
			.filter(k -> k.getNamespace().equals(ComedyMonkeysCreateAddons.MOD_ID))
			.forEach(k -> ids.add(k.toString()));
		try (Stream<Path> files = Files.list(Path.of(ROOT, "docs/data"))) {
			for (Path f : files.filter(p -> p.toString().endsWith(".json") && !p.endsWith("names.json")).toList())
				collectIds(readJson(f), ids);
		}
		// nothing site.json lists as unreleased
		JsonObject site = readJson(Path.of(ROOT, "docs/data/site.json")).getAsJsonObject();
		if (site.has("unreleased"))
			site.getAsJsonArray("unreleased").forEach(u -> ids.remove(u.getAsString()));
		int size = config.has("iconSize") ? config.get("iconSize").getAsInt() : 128;
		int supersample = config.has("supersample") ? config.get("supersample").getAsInt() : 2;
		Path namesFile = Path.of(ROOT, "docs/data/names.json");
		Map<String, String> names = new TreeMap<>();
		if (Files.exists(namesFile))
			readJson(namesFile).getAsJsonObject().entrySet().forEach(e -> names.put(e.getKey(), e.getValue().getAsString()));

		int done = 0;
		for (String id : ids) {
			ResourceLocation rl = ResourceLocation.tryParse(id);
			if (rl == null)
				continue;
			ItemStack stack;
			String name;
			if (BuiltInRegistries.ITEM.containsKey(rl) && BuiltInRegistries.ITEM.get(rl) != Items.AIR) {
				stack = new ItemStack(BuiltInRegistries.ITEM.get(rl));
				name = stack.getHoverName().getString();
			} else if (BuiltInRegistries.FLUID.containsKey(rl) && BuiltInRegistries.FLUID.get(rl) != Fluids.EMPTY) {
				// a fluid is shown as its bucket, and named as the fluid
				Fluid fluid = BuiltInRegistries.FLUID.get(rl);
				stack = new ItemStack(fluid.getBucket());
				name = fluid.getFluidType().getDescription().getString();
				if (stack.isEmpty())
					continue;
			} else {
				continue;
			}
			NativeImage image = renderItem(mc, stack, size * supersample);
			Path out = Path.of(ROOT, "docs/assets/icons", rl.getNamespace(), rl.getPath() + ".png");
			Files.createDirectories(out.getParent());
			NativeImage finished = finishImage(image, supersample, false);
			finished.writeToFile(out);
			finished.close();
			names.put(id, name);
			done++;
		}
		Files.writeString(namesFile, new GsonBuilder().setPrettyPrinting().create().toJson(names) + "\n");
		report.add("icons " + done + " of " + ids.size() + " ids");
	}

	private static NativeImage renderItem(Minecraft mc, ItemStack stack, int pixels) {
		TextureTarget target = new TextureTarget(pixels, pixels, true, Minecraft.ON_OSX);
		target.setClearColor(0, 0, 0, 0);
		target.clear(Minecraft.ON_OSX);
		target.bindWrite(true);
		// the inventory's own projection, sized to one 16 x 16 slot
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 16, 16, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.translation(0, 0, -11000);
		RenderSystem.applyModelViewMatrix();
		GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
		graphics.renderItem(stack, 0, 0);
		graphics.flush();
		NativeImage image = read(target);
		modelView.popMatrix();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.restoreProjectionMatrix();
		target.destroyBuffers();
		mc.getMainRenderTarget().bindWrite(true);
		return image;
	}

	// ---------------------------------------------------------------- images

	private static boolean isEmpty(NativeImage image) {
		for (int y = 0; y < image.getHeight(); y++)
			for (int x = 0; x < image.getWidth(); x++)
				if (FastColor.ABGR32.alpha(image.getPixelRGBA(x, y)) > 2)
					return false;
		return true;
	}

	private static NativeImage read(TextureTarget target) {
		NativeImage image = new NativeImage(target.width, target.height, false);
		RenderSystem.bindTexture(target.getColorTextureId());
		image.downloadTexture(0, false);
		image.flipY();
		return image;
	}

	/**
	 * Shrinks a supersampled image and, for scenes, crops it to what was drawn with a little room around it. Colours
	 * drawn over the transparent clear come out premultiplied by their alpha (glass over nothing), so it averages
	 * premultiplied and divides back out at the end.
	 */
	private static NativeImage finishImage(NativeImage in, int factor, boolean crop) {
		int w = in.getWidth() / factor, h = in.getHeight() / factor;
		int[] r = new int[w * h], g = new int[w * h], bl = new int[w * h], al = new int[w * h];
		int minX = w, minY = h, maxX = -1, maxY = -1;
		int n = factor * factor;
		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++) {
				int sr = 0, sg = 0, sb = 0, sa = 0;
				for (int dy = 0; dy < factor; dy++)
					for (int dx = 0; dx < factor; dx++) {
						int c = in.getPixelRGBA(x * factor + dx, y * factor + dy);
						sr += FastColor.ABGR32.red(c);
						sg += FastColor.ABGR32.green(c);
						sb += FastColor.ABGR32.blue(c);
						sa += FastColor.ABGR32.alpha(c);
					}
				int i = y * w + x;
				r[i] = sr / n;
				g[i] = sg / n;
				bl[i] = sb / n;
				al[i] = sa / n;
				if (al[i] > 2) {
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
				}
			}
		in.close();
		if (!crop || maxX < 0) {
			minX = 0;
			minY = 0;
			maxX = w - 1;
			maxY = h - 1;
		} else {
			int pad = Math.round(Math.max(maxX - minX, maxY - minY) * 0.04f);
			minX = Math.max(0, minX - pad);
			minY = Math.max(0, minY - pad);
			maxX = Math.min(w - 1, maxX + pad);
			maxY = Math.min(h - 1, maxY + pad);
		}
		NativeImage out = new NativeImage(maxX - minX + 1, maxY - minY + 1, true);
		for (int y = minY; y <= maxY; y++)
			for (int x = minX; x <= maxX; x++) {
				int i = y * w + x, a = al[i];
				int c = a == 0 ? 0 : FastColor.ABGR32.color(a, Math.min(255, bl[i] * 255 / a), Math.min(255, g[i] * 255 / a), Math.min(255, r[i] * 255 / a));
				out.setPixelRGBA(x - minX, y - minY, c);
			}
		return out;
	}

	// ---------------------------------------------------------------- set-up

	private static List<Shot> readShots(JsonObject config) {
		JsonObject defaults = config.has("defaults") ? config.getAsJsonObject("defaults") : new JsonObject();
		int spacing = config.has("spacing") ? config.get("spacing").getAsInt() : 32;
		List<Shot> list = new ArrayList<>();
		JsonArray array = config.getAsJsonArray("shots");
		String only = System.getProperty("cmaddons.renders.only");
		for (int i = 0; i < array.size(); i++) {
			JsonObject j = array.get(i).getAsJsonObject();
			String id = j.get("id").getAsString();
			JsonArray region = j.has("region") ? j.getAsJsonArray("region") : null;
			BlockPos min = region == null ? BlockPos.ZERO : pos(region.get(0).getAsJsonArray());
			BlockPos max = region == null ? BlockPos.ZERO : pos(region.get(1).getAsJsonArray());
			// a scene can name its block once ("block", used as {block} in its commands) and list variants that swap
			// it for another: each variant is the same scene again, as <id>--<variant>
			Map<String, String> blocks = new java.util.LinkedHashMap<>();
			blocks.put(id, j.has("block") ? j.get("block").getAsString() : "");
			if (j.has("variants"))
				j.getAsJsonObject("variants").entrySet().forEach(v -> blocks.put(id + "--" + v.getKey(), v.getValue().getAsString()));
			for (var scene : blocks.entrySet()) {
				if (only != null && !scene.getKey().matches(only))
					continue;
				List<String> setup = new ArrayList<>();
				j.getAsJsonArray("setup").forEach(c -> setup.add(c.getAsString().replace("{block}", scene.getValue())));
				list.add(new Shot(scene.getKey(), new BlockPos(list.size() * spacing, 100, 0), setup, min, max,
					number(j, defaults, "yaw", 135), number(j, defaults, "pitch", 30), (int) number(j, defaults, "wait", 60),
					(int) number(j, defaults, "pixelsPerBlock", 400)));
			}
		}
		return list;
	}

	private static float number(JsonObject j, JsonObject defaults, String key, float fallback) {
		return j.has(key) ? j.get(key).getAsFloat() : defaults.has(key) ? defaults.get(key).getAsFloat() : fallback;
	}

	private static BlockPos pos(JsonArray a) {
		return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
	}

	private static void collectIds(JsonElement e, Set<String> ids) {
		if (e.isJsonObject())
			e.getAsJsonObject().entrySet().forEach(x -> collectIds(x.getValue(), ids));
		else if (e.isJsonArray())
			e.getAsJsonArray().forEach(x -> collectIds(x, ids));
		else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() && ID.matcher(e.getAsString()).matches())
			ids.add(e.getAsString());
	}

	private static JsonElement readJson(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader);
		}
	}

	private static void command(String command) {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		if (server != null)
			server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
	}

	/**
	 * {@code !use <item> <x> <y> <z> <face>}: a fake player uses the item on that face of that block (x y z as ~ offsets
	 * from the scene's origin), exactly as a player's click would, for things a command can't place properly: wires
	 * laid on a face, fittings clipped onto a line.
	 */
	private static void use(BlockPos origin, String step) {
		String[] p = step.split("\\s+");
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		if (server == null || p.length < 6)
			return;
		server.execute(() -> {
			try {
				net.minecraft.server.level.ServerLevel level = server.overworld();
				BlockPos pos = origin.offset(rel(p[2]), rel(p[3]), rel(p[4]));
				net.minecraft.core.Direction face = net.minecraft.core.Direction.byName(p[5]);
				var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
				ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(p[1])));
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
				Vec3 at = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
				player.moveTo(at.x + face.getStepX(), at.y + face.getStepY() - 1, at.z + face.getStepZ(), 0, 0);
				var hit = new net.minecraft.world.phys.BlockHitResult(at, face, pos, false);
				player.gameMode.useItemOn(player, level, stack, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
			} catch (RuntimeException e) {
				report.add("FAILED " + step + ": " + e);
			}
		});
	}

	private static int rel(String coordinate) {
		String n = coordinate.startsWith("~") ? coordinate.substring(1) : coordinate;
		return n.isEmpty() ? 0 : Integer.parseInt(n);
	}

	private static void deleteWorld(Minecraft mc) throws IOException {
		Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
		if (!Files.exists(dir))
			return;
		try (Stream<Path> walk = Files.walk(dir)) {
			for (Path p : walk.sorted(Comparator.reverseOrder()).toList())
				Files.delete(p);
		}
	}

	private static void finish(Minecraft mc) {
		phase = Phase.DONE;
		try {
			Files.writeString(Path.of(ROOT, "tools/website/renders-report.txt"), String.join("\n", report) + "\n");
		} catch (IOException e) {
			LOG.error("Couldn't write the render report", e);
		}
		LOG.info("Render export finished:\n{}", String.join("\n", report));
		if (!Boolean.getBoolean("cmaddons.renders.stay"))
			mc.stop();
	}
}
