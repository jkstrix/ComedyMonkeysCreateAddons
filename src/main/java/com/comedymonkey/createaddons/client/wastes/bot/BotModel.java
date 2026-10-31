package com.comedymonkey.createaddons.client.wastes.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.tradebot.TradeBotGeometry;
import com.comedymonkey.createaddons.client.tradebot.TradeBotModel;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;
import com.comedymonkey.createaddons.content.tradebot.clip.Track;
import com.comedymonkey.createaddons.content.wastes.bot.CityBot;
import com.comedymonkey.createaddons.content.wastes.bot.Custodian;
import com.comedymonkey.createaddons.content.wastes.bot.Sentry;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * A Clockwork Wastes bot posed from its clips exactly as the Blockbench preview plays them (through Trade Bot's
 * posing, as the Juggernaut is). The Sentry's aim is laid over its clip on the yaw ring and pitch knuckle (in its target
 * and unfold clips the aim replaces the ring's own turn, which only previewed it turning to a target). The Custodian's
 * head is turned by the code to what it looks at (D60): the breaker in wrath, a broken pylon in a mourn, the heart vault
 * in a glance.
 */
public class BotModel<T extends CityBot> extends HierarchicalModel<T> {
	/** One bot's exported rig: its geometry (design/models/clockwork-wastes-bots.game-export.js), its root bone, its layer. */
	public record Rig(String id) {
		public String geometryPath() {
			return "/assets/comedymonkeysaddons/" + id + "/geometry.json";
		}

		public TradeBotGeometry geometry() {
			return TradeBotGeometry.of(geometryPath());
		}

		public ModelLayerLocation layer() {
			return new ModelLayerLocation(ComedyMonkeysCreateAddons.asResource(id), "main");
		}

		public LayerDefinition layerDefinition() {
			return geometry().layer(id);
		}
	}

	public static final Rig SCUTTLER = new Rig("scuttler"), SENTRY = new Rig("sentry"), CUSTODIAN = new Rig("custodian");

	/** The Sentry's clips whose yaw ring turn the aim replaces. */
	private static final List<String> AIM_REPLACES = List.of("target", "unfold");
	/** The Custodian's bones that turn its look about its spine, waist up to head. */
	private static final String[] SPINE = { "waist", "chest", "neck", "head" };
	/** Provisional: how much further than its clip a mourn or a glance may turn it to reach what it looks at (x the clip's turn). */
	static final float LOOK_STRETCH = 1.5f;
	/** Provisional: in wrath, its head follows the breaker up to this far either way, and up or down. */
	static final float WRATH_YAW = 75, WRATH_PITCH = 40;
	/** Its look turns: each clip's largest turn about its spine (Blockbench degrees, + to its left), read from the clip. */
	private static final Map<String, Float> TURNS = new ConcurrentHashMap<>();

	private final ModelPart root;
	private final Map<String, ModelPart> parts;
	/** The Custodian's four sockets: the parts from the root down to each (to place them as posed). */
	@Nullable
	private final List<List<ModelPart>> sockets;

	public BotModel(Rig rig, ModelPart root) {
		this.root = root;
		this.parts = rig.geometry().parts(root, rig.id());
		if (rig.equals(CUSTODIAN)) {
			List<List<ModelPart>> chains = new ArrayList<>();
			for (int k = 1; k <= 4; k++) {
				List<String> names = path(rig.geometry(), rig.id(), "socket_" + k);
				List<ModelPart> chain = new ArrayList<>();
				chain.add(root);
				if (names != null)
					for (String n : names)
						chain.add(parts.get(n));
				chains.add(names == null ? null : chain);
			}
			sockets = chains;
		} else
			sockets = null;
	}

	/** The bones from {@code from} down to {@code to}, or null if it isn't under it. */
	@Nullable
	private static List<String> path(TradeBotGeometry geometry, String from, String to) {
		if (from.equals(to))
			return new ArrayList<>(List.of(from));
		for (String child : geometry.children(from)) {
			List<String> rest = path(geometry, child, to);
			if (rest != null) {
				rest.add(0, from);
				return rest;
			}
		}
		return null;
	}

	@Override
	public ModelPart root() {
		return root;
	}

	public Map<String, ModelPart> parts() {
		return parts;
	}

	/** The parts from the model's root down to the Custodian's socket {@code k} (0-3), or null. */
	@Nullable
	public List<ModelPart> socketChain(int k) {
		return sockets == null ? null : sockets.get(k);
	}

	@Override
	public void setupAnim(T bot, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		root.getAllParts().forEach(ModelPart::resetPose);
		double now = bot.clipNow(ageInTicks - bot.tickCount);
		ClipPlayback playback = bot.playback;
		playback.observe(bot.clip(), bot.clipStartTime(), bot.clipSpeed(), now);
		Clip clip = playback.clip();
		if (clip == null)
			return;
		Clip old = playback.fadeFrom(now);
		float t = clip.localTime(playback.elapsed(now));
		TradeBotModel.pose(parts, clip, t, old, old == null ? 0 : old.localTime(playback.fadeFromElapsed(now)), playback.fade(now));
		float partialTick = ageInTicks - bot.tickCount;
		if (bot instanceof Sentry sentry) {
			// its aim, on top of the clip (Blockbench's frame: ry turns the front to -X, rx tips it up; see TradeBotGeometry)
			sentry.springAim(now);
			ModelPart yaw = parts.get("yaw"), pitch = parts.get("pitch");
			if (AIM_REPLACES.contains(clip.name())) {
				if (yaw != null)
					yaw.yRot = 0;
				if (pitch != null)
					pitch.xRot = 0;
			}
			if (yaw != null)
				yaw.yRot += -sentry.shownYaw * Mth.DEG_TO_RAD;
			if (pitch != null)
				pitch.xRot += -sentry.shownPitch * Mth.DEG_TO_RAD;
		}
		if (bot instanceof Custodian custodian)
			look(custodian, clip, t, partialTick);
	}

	/**
	 * D60: the code aims the Custodian's head. In wrath its head follows the breaker (from the snap at the clip's start
	 * until it settles back); in a mourn and a glance at the vault the clip's turn about its spine is scaled so it ends
	 * looking at the pylon or the vault, wherever they are (mirrored when they're on its right).
	 */
	private void look(Custodian c, Clip clip, float t, float partialTick) {
		String name = clip.name();
		boolean wrath = name.equals("wrath"), turn = name.equals("mourn") || name.equals("glance_vault");
		if (!wrath && !turn)
			return;
		Vec3 at;
		if (wrath) {
			Entity e = c.lookEntity();
			at = e == null ? null : e.getEyePosition(partialTick);
		} else {
			BlockPos b = c.lookBlock();
			// a pylon: its crown; the vault: its iris
			at = b == null ? null : name.equals("mourn") ? Vec3.atBottomCenterOf(b).add(0, 2.75, 0) : Vec3.atCenterOf(b);
		}
		if (at == null)
			return;
		Vec3 eye = c.getPosition(partialTick).add(0, 2.6, 0), d = at.subtract(eye);
		float body = Mth.rotLerp(partialTick, c.yBodyRotO, c.yBodyRot);
		float bearing = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90;
		// Blockbench's turn to its left is +ry; the world's to its right is +yaw
		float want = -Mth.wrapDegrees(bearing - body);
		if (wrath) {
			ModelPart head = parts.get("head");
			if (head == null)
				return;
			float w = t < 0.08f ? t / 0.08f : t < 1.3f ? 1 : Math.max(0, 1 - (t - 1.3f) / 0.4f);
			float up = (float) (Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
			head.yRot += -Mth.clamp(want, -WRATH_YAW, WRATH_YAW) * w * Mth.DEG_TO_RAD;
			head.xRot += -Mth.clamp(up, -WRATH_PITCH, WRATH_PITCH) * w * Mth.DEG_TO_RAD;
			return;
		}
		float peak = TURNS.computeIfAbsent(name, n -> peakTurn(clip));
		if (Math.abs(peak) < 1)
			return;
		float scale = Mth.clamp(want, -Math.abs(peak) * LOOK_STRETCH, Math.abs(peak) * LOOK_STRETCH) / peak;
		for (String bone : SPINE) {
			ModelPart p = parts.get(bone);
			if (p != null)
				p.yRot *= scale;
		}
	}

	/** A clip's largest turn about the Custodian's spine (the sum of its spine bones' yaw), in Blockbench degrees. */
	private static float peakTurn(Clip clip) {
		Vector3f v = new Vector3f();
		float peak = 0;
		for (float t = 0; t <= clip.length(); t += 0.05f) {
			float sum = 0;
			for (String bone : SPINE) {
				Track track = clip.track(bone, "rotation");
				if (track != null)
					sum += track.sample(t, v).y;
			}
			if (Math.abs(sum) > Math.abs(peak))
				peak = sum;
		}
		return peak;
	}
}
