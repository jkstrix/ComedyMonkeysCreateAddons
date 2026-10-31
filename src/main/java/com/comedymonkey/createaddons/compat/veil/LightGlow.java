package com.comedymonkey.createaddons.compat.veil;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.joml.Vector3f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.LightRenderer;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.light.LightBlock;
import com.comedymonkey.createaddons.content.light.LightBlockEntity;
import com.comedymonkey.createaddons.content.light.LightColours;
import com.comedymonkey.createaddons.content.light.LightKind;
import com.mojang.blaze3d.vertex.PoseStack;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Coloured light from the Electric Lights, where Veil is installed (it comes with Sable and Create Aeronautics) and
 * Colorful Lighting isn't (that colours the light itself, from the block state: light/emitters.json), nor Iris (Veil's
 * light pass fails under it, with or without a shader pack: GL errors every frame and no light).
 * Vanilla's block light has no colour, so the light a lamp casts on the world stays white; with Veil, each lit lamp also
 * adds one of Veil's point lights at its lens, in its glow's colour (strengthened, so it reads over the white), as far
 * as its own light reaches. It flickers and warms up with the lens, frame by frame. Only for lights in the world the
 * player is in: Ponder's and contraptions' lamps keep just their lens.
 * <p>
 * Veil's lights read where things are from a copy of the scene's depth, which only works if its light buffer's depth
 * is the same kind as the screen's. Ponder (in every Create pack) gives the screen a stencil, so it's NeoForge's
 * depth-and-stencil, which Veil's own buffer isn't: the copy fails every frame and no light shows. A built-in pack,
 * always on and on top, gives Veil's light buffer the matching depth (resourcepacks/veil_light_depth).
 */
public final class LightGlow {
	/**
	 * How bright a dyed light's colour is; an undyed light's warm white is gentler, over vanilla's white. Faint, and short
	 * (REACH of its own light): Veil's lights add up, and a street with a lamp every few blocks washed out to white at 1.4
	 * and 0.6, reaching as far as each lamp's own light (2026-10-28).
	 */
	private static final float DYED = 0.5f, PLAIN = 0.2f, REACH = 0.55f;

	/** Lit lights in the player's world, and the Veil light each has while coloured light is on. */
	private static final Map<LightBlockEntity, Glow> GLOWS = new IdentityHashMap<>();
	/**
	 * Veil's light pass turns its albedo and normal buffers on only on frames where a light is in view, and off again on
	 * frames with none; each change deletes its framebuffers and swaps its shaders, and skips that frame's lights. Turning
	 * quickly by a lamp did that every few frames: hitches, and in a window a flashing screen. While any of our glows is
	 * lit, the buffers are held on under this name, so Veil's own on and off never changes them.
	 */
	private static final net.minecraft.resources.ResourceLocation HOLD = ComedyMonkeysCreateAddons.asResource("light_glow");
	private static boolean held;
	/** Where each light's lens sits in its block, by the way it faces (read from its lit lens model). */
	private static final Map<LightKind, Vec3[]> LENSES = new EnumMap<>(LightKind.class);

	private static final class Glow {
		LightRenderHandle<PointLightData> handle;
		int step = -1;
		float r = -1, g, b, brightness = -1;
		/** Where its light was last put, in the world (a ship's lamp moves with its ship). */
		double x = Double.NaN, y, z;
	}

	/** Set once the coloured light is installed: Veil's clears are only rerouted then (VeilRenderSystemMixin). */
	public static volatile boolean installed;
	private static final boolean SABLE = net.neoforged.fml.ModList.get().isLoaded("sable");

	private LightGlow() {}

	public static void install(IEventBus modBus) {
		installed = true;
		LightBlockEntity.clientLooks = LightGlow::changed;
		NeoForge.EVENT_BUS.addListener(LightGlow::frame);
		NeoForge.EVENT_BUS.addListener(LightGlow::unload);
		// lens positions are read again from the new models, and every glow put where its lens now is
		modBus.addListener((ModelEvent.BakingCompleted event) -> {
			LENSES.clear();
			GLOWS.values().forEach(g -> g.step = -1);
		});
		modBus.addListener(LightGlow::addPack);
	}

	private static void addPack(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.CLIENT_RESOURCES)
			event.addPackFinders(ComedyMonkeysCreateAddons.asResource("resourcepacks/veil_light_depth"), PackType.CLIENT_RESOURCES,
				Component.literal("Coloured light fix for Veil"), PackSource.BUILT_IN, true, Pack.Position.TOP);
	}

	/** A light was loaded or its looks changed: lit, it's followed; dark, it isn't. */
	private static void changed(LightBlockEntity be) {
		// (not Light Strips: a point light a block along a whole run would be far too many)
		if (be.getLevel() == null || be.getLevel() != Minecraft.getInstance().level
			|| be instanceof com.comedymonkey.createaddons.content.light.LightStripBlockEntity
			// (nor Festoon hooks: their light is their spots)
			|| be instanceof com.comedymonkey.createaddons.content.light.FestoonBlockEntity)
			return;
		if (!be.isRemoved() && be.phase() != LightBlockEntity.Phase.DARK)
			GLOWS.computeIfAbsent(be, b -> new Glow());
		else
			free(GLOWS.remove(be));
	}

	private static void frame(RenderFrameEvent.Pre event) {
		if (GLOWS.isEmpty()) {
			hold(false);
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		boolean on = CMConfig.CLIENT.lightColouredLight.get(), shadows = CMConfig.CLIENT.lightColouredLightShadows.get();
		float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		for (Iterator<Map.Entry<LightBlockEntity, Glow>> it = GLOWS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<LightBlockEntity, Glow> e = it.next();
			LightBlockEntity be = e.getKey();
			Glow glow = e.getValue();
			if (be.isRemoved() || be.getLevel() != mc.level || be.phase() == LightBlockEntity.Phase.DARK) {
				free(glow);
				it.remove();
				continue;
			}
			if (!on) {
				free(glow);
				continue;
			}
			update(be, glow, shadows, pt);
		}
		boolean lit = false;
		for (Glow glow : GLOWS.values())
			lit |= glow.handle != null && glow.handle.isValid();
		hold(lit);
	}

	/** Holds Veil's light buffers on (or lets them go), so lights coming into and out of view don't rebuild them. */
	private static void hold(boolean on) {
		if (on == held)
			return;
		held = on;
		if (on)
			VeilRenderSystem.renderer().enableBuffers(HOLD, foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType.ALBEDO,
				foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType.NORMAL);
		else
			VeilRenderSystem.renderer().disableBuffers(HOLD);
	}

	private static void update(LightBlockEntity be, Glow glow, boolean shadows, float pt) {
		if (glow.handle == null || !glow.handle.isValid()) {
			glow.handle = VeilRenderSystem.renderer().getLightRenderer().addLight(new PointLightData());
			glow.step = -1;
			glow.r = glow.brightness = -1;
		}
		PointLightData light = glow.handle.getLightData();
		boolean dirty = false;
		if (glow.step != be.step()) {
			// about half as far as its own light reaches at this step
			light.setRadius(be.kind().level(be.step()) * REACH + 1);
			glow.step = be.step();
			glow.x = Double.NaN;
			dirty = true;
		}
		// where its lens is (a Floodlight's light is where its beam lands: its spot, none while it has none; at its lens it
		// only lit up its own can): on a ship, out where the ship is now (its blocks are kept in a plot far away), every frame
		net.minecraft.core.BlockPos spot = be.kind() == LightKind.FLOODLIGHT ? be.spot() : null;
		Vec3 at = spot != null ? Vec3.atCenterOf(spot) : Vec3.atLowerCornerOf(be.getBlockPos()).add(lens(be));
		boolean placed = be.kind() != LightKind.FLOODLIGHT || spot != null;
		if (SABLE) {
			Vec3 out = com.comedymonkey.createaddons.compat.sable.client.SableSoundPositions.heardFrom(at.x, at.y, at.z);
			placed = placed && out != null;
			if (out != null)
				at = out;
		}
		if (at.x != glow.x || at.y != glow.y || at.z != glow.z) {
			light.setPosition(at.x, at.y, at.z);
			glow.x = at.x;
			glow.y = at.y;
			glow.z = at.z;
			dirty = true;
		}
		if (light.isOcclusionEnabled() != shadows) {
			light.setOcclusionEnabled(shadows);
			dirty = true;
		}
		long time = be.getLevel().getGameTime();
		float w = LightRenderer.warmth(be, time, pt);
		float[] c = LightColours.cast(be.dye(), LightRenderer.warming(be, time, pt));
		// a dye whose light isn't its lens's (black, green) still warms up from sodium red
		if (w < 1 && be.dye() != null) {
			float[] sodium = LightColours.cast(null, LightColours.SODIUM);
			float k = w * w * (3 - 2 * w);
			for (int i = 0; i < 3; i++)
				c[i] = sodium[i] + (c[i] - sodium[i]) * k;
		}
		if (c[0] != glow.r || c[1] != glow.g || c[2] != glow.b) {
			light.setColor(c[0], c[1], c[2]);
			glow.r = c[0];
			glow.g = c[1];
			glow.b = c[2];
			dirty = true;
		}
		// dark in a flicker's off frames (and on a ship that can't be placed); a sodium lamp brightens as it warms
		float brightness = !placed || !LightRenderer.lit(be, time, pt) ? 0 : (be.dye() == null ? PLAIN : DYED) * (0.25f + 0.75f * w);
		if (brightness != glow.brightness) {
			light.setBrightness(brightness);
			glow.brightness = brightness;
			dirty = true;
		}
		if (dirty)
			glow.handle.markDirty();
	}

	private static void free(Glow glow) {
		if (glow != null && glow.handle != null) {
			if (glow.handle.isValid())
				glow.handle.free();
			glow.handle = null;
		}
	}

	private static void unload(LevelEvent.Unload event) {
		if (!event.getLevel().isClientSide())
			return;
		GLOWS.values().forEach(LightGlow::free);
		GLOWS.clear();
	}

	/** The middle of its lit lens in its block, turned the way it faces. */
	private static Vec3 lens(LightBlockEntity be) {
		Direction facing = be.getBlockState().hasProperty(LightBlock.FACING) ? be.getBlockState().getValue(LightBlock.FACING) : Direction.UP;
		LightKind kind = be.kind();
		return LENSES.computeIfAbsent(kind, k -> {
			Vec3 built = lensAsBuilt(k);
			Vec3[] turned = new Vec3[Direction.values().length];
			for (Direction d : Direction.values()) {
				PoseStack ms = new PoseStack();
				LightRenderer.turn(ms, k, d);
				Vector3f p = ms.last().pose().transformPosition(new Vector3f((float) built.x, (float) built.y, (float) built.z));
				turned[d.ordinal()] = new Vec3(p.x(), p.y(), p.z());
			}
			return turned;
		})[facing.ordinal()];
	}

	/** The middle of the lens model's bounds, as built; the block's middle if it has none. */
	private static Vec3 lensAsBuilt(LightKind kind) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(LightRenderer.glow(kind));
		float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}, max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		RandomSource random = RandomSource.create(42);
		Direction[] sides = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
		for (Direction side : sides)
			for (BakedQuad quad : model.getQuads(null, side, random, ModelData.EMPTY, null)) {
				int[] v = quad.getVertices();
				for (int i = 0; i + IQuadTransformer.STRIDE <= v.length; i += IQuadTransformer.STRIDE)
					for (int a = 0; a < 3; a++) {
						float f = Float.intBitsToFloat(v[i + IQuadTransformer.POSITION + a]);
						min[a] = Math.min(min[a], f);
						max[a] = Math.max(max[a], f);
					}
			}
		if (min[0] > max[0])
			return new Vec3(0.5, 0.5, 0.5);
		return new Vec3((min[0] + max[0]) / 2, (min[1] + max[1]) / 2, (min[2] + max[2]) / 2);
	}
}
