package com.comedymonkey.createaddons.client.juggernaut;

import java.util.List;
import java.util.Map;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.tradebot.TradeBotGeometry;
import com.comedymonkey.createaddons.client.tradebot.TradeBotModel;
import com.comedymonkey.createaddons.content.juggernaut.ClockworkJuggernaut;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/**
 * The Clockwork Juggernaut posed from his clips, exactly as the Blockbench preview plays them (through Trade Bot's
 * posing), with the plates each stage shed left off afterwards.
 */
public class JuggernautModel extends HierarchicalModel<ClockworkJuggernaut> {
	public static final String GEOMETRY = "/assets/comedymonkeysaddons/clockwork_juggernaut/geometry.json";
	public static final String ROOT = "juggernaut";
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ComedyMonkeysCreateAddons.asResource("clockwork_juggernaut"), "main");
	/** The plates stage 2 and stage 3 shed (D43), as in the shed_plates clips (clockwork-juggernaut.clips.js). */
	private static final List<String> STAGE_2_PLATES = List.of("chest_plate", "shield_right", "shield_left");
	private static final List<String> STAGE_3_PLATES = List.of("helm", "forearm_guard_right", "forearm_guard_left", "shin_guard_right",
		"shin_guard_left");

	private final ModelPart root;
	private final Map<String, ModelPart> parts;

	public JuggernautModel(ModelPart root) {
		this.root = root;
		this.parts = geometry().parts(root, ROOT);
	}

	public static TradeBotGeometry geometry() {
		return TradeBotGeometry.of(GEOMETRY);
	}

	public static LayerDefinition layer() {
		return geometry().layer(ROOT);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	public Map<String, ModelPart> parts() {
		return parts;
	}

	@Override
	public void setupAnim(ClockworkJuggernaut juggernaut, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
		float headPitch) {
		root.getAllParts().forEach(ModelPart::resetPose);
		double now = juggernaut.clipNow(ageInTicks - juggernaut.tickCount);
		ClipPlayback playback = juggernaut.playback;
		playback.observe(juggernaut.clip(), juggernaut.clipStartTime(), juggernaut.clipSpeed(), now);
		Clip clip = playback.clip();
		if (clip == null)
			return;
		Clip old = playback.fadeFrom(now);
		TradeBotModel.pose(parts, clip, clip.localTime(playback.elapsed(now)), old, old == null ? 0 : old.localTime(playback.fadeFromElapsed(now)),
			playback.fade(now));
		// once shed, plates stay gone (the shed clip itself flings them away)
		int stage = juggernaut.stage();
		boolean shedding = clip.name().startsWith("shed_plates_");
		hide(STAGE_2_PLATES, stage >= 3 || stage == 2 && !(shedding && clip.name().endsWith("2")));
		hide(STAGE_3_PLATES, stage >= 3 && !(shedding && clip.name().endsWith("3")));
		// stirring (D32): his slit lights while he decides to wake
		ModelPart slit = parts.get("slit_glow");
		if (slit != null && juggernaut.phase() == ClockworkJuggernaut.Phase.STIRRING)
			slit.xScale = slit.yScale = slit.zScale = 1;
	}

	/** Every Juggernaut shares this model, and resetPose leaves visibility alone: set it both ways every frame. */
	private void hide(List<String> bones, boolean hidden) {
		for (String bone : bones) {
			ModelPart part = parts.get(bone);
			if (part != null)
				part.visible = !hidden;
		}
	}
}
