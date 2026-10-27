package com.comedymonkey.createaddons.client.helperbot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.tradebot.TradeBotGeometry;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * Helper Bot's model, clips and textures, exported from the Blockbench preview by
 * {@code design/models/helper-bot.gen.js --game} in Trade Bot's formats (so Trade Bot's loaders and posing read them).
 * One model is baked per resource reload and shared: each Helper Bot is posed just before he's drawn. With Flywheel
 * on, placed and riding bots are instances instead ({@link HelperBotVisual}, {@link HelperBotActorVisual}); this
 * model stays for Ponder, the shop screen, the item, and whenever Flywheel can't draw.
 */
public final class HelperBotModels {
	public static final String GEOMETRY = "/assets/comedymonkeysaddons/helper_bot/geometry.json";
	public static final String CLIPS = "/assets/comedymonkeysaddons/helper_bot/clips.json";
	public static final String ROOT = "helper_bot";
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(ComedyMonkeysCreateAddons.asResource("helper_bot"), "main");
	private static final ResourceLocation PLAIN = ComedyMonkeysCreateAddons.asResource("textures/entity/helper_bot/plain.png");
	private static final Map<DyeColor, ResourceLocation> DYED = new EnumMap<>(DyeColor.class);
	static {
		for (DyeColor dye : DyeColor.values())
			DYED.put(dye, ComedyMonkeysCreateAddons.asResource("textures/entity/helper_bot/" + dye.getName() + ".png"));
	}
	/** His render types, one per texture: each has a buffer of its own, so drawing him never ends anyone's batch. */
	static final List<RenderType> RENDER_TYPES;
	static {
		List<RenderType> types = new ArrayList<>();
		types.add(renderType(null));
		for (DyeColor dye : DyeColor.values())
			types.add(renderType(dye));
		RENDER_TYPES = List.copyOf(types);
	}

	/** A baked model: its root, and its bones to pose. */
	public record Baked(ModelPart root, HelperBotRig rig) {}

	/** Every bone and no cubes: what a Flywheel visual poses before copying the pose onto its instances. */
	@Nullable
	private static volatile LayerDefinition skeleton;

	private static int reloads;
	private static int builtFor = -1;
	@Nullable
	private static Baked baked;

	private HelperBotModels() {}

	public static TradeBotGeometry geometry() {
		return TradeBotGeometry.of(GEOMETRY);
	}

	public static ClipLibrary clips() {
		return ClipLibrary.of(CLIPS);
	}

	public static LayerDefinition layer() {
		return geometry().layer(ROOT);
	}

	static LayerDefinition skeletonLayer() {
		LayerDefinition layer = skeleton;
		if (layer == null)
			skeleton = layer = geometry().layer(cube -> false, ROOT);
		return layer;
	}

	/** His texture in this dye (each colour is its own texture, painted like the reviewed preview). */
	public static ResourceLocation texture(@Nullable DyeColor dye) {
		return dye == null ? PLAIN : DYED.get(dye);
	}

	static RenderType renderType(@Nullable DyeColor dye) {
		return RenderType.entityCutoutNoCull(texture(dye));
	}

	/** After a resource reload: bake again next time (the entity models are baked again). */
	public static void forget() {
		reloads++;
	}

	public static Baked baked() {
		if (baked == null || builtFor != reloads) {
			builtFor = reloads;
			ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(LAYER);
			baked = new Baked(root, new HelperBotRig(root));
		}
		return baked;
	}

	/**
	 * Draws him (already posed) standing in the block at the pose stack's origin, facing {@code facing}. His foot plate
	 * sits 2 px forward of the block's middle, toward what he faces (the brief's as-built notes).
	 */
	public static void draw(PoseStack ms, MultiBufferSource buffers, int light, int overlay, Direction facing, @Nullable DyeColor dye, Baked model) {
		ms.pushPose();
		ms.translate(0.5 + facing.getStepX() * 2 / 16f, 0, 0.5 + facing.getStepZ() * 2 / 16f);
		ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
		ms.scale(-1, -1, 1);
		ms.translate(0, -1.501, 0);
		// one buffer, glowing irises and all (see HelperBotRig.render)
		model.rig().render(ms, buffers.getBuffer(renderType(dye)), light, overlay);
		ms.popPose();
	}
}
