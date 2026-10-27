package com.comedymonkey.createaddons.client;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.cage.CageMonster;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlock;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Draws a Monster Cage's insides (design/briefs/monster-cage.md): its monsters pacing behind the bars at the Animal Pens'
 * scale, chow in the dog bowl, drops in the tray, and the shaft end. Flooded cages add the water (a translucent volume to
 * the tank's level), the water glass on each side and the trident hatch flapping open; the Netherite cage its coil tips,
 * the beacon (spinning red through a countdown) and the blue shield round the whole block, which drifts and pulses
 * (an animated texture) while the field is up, flickers through a countdown, and is gone when the field is down. The
 * cage is modelled with its front facing north; positions are in its model pixels.
 */
public class MonsterCageRenderer extends SafeBlockEntityRenderer<MonsterCageBlockEntity> {
	public static final ModelResourceLocation HATCH = part("block/monster_cage_hatch");
	public static final ModelResourceLocation WATER = part("block/flooded_monster_cage_water");
	public static final ModelResourceLocation WATER_COLUMN = part("block/flooded_monster_cage_water_column");
	public static final ModelResourceLocation REFLECTOR = part("block/netherite_monster_cage_reflector");
	public static final ModelResourceLocation LENS = part("block/netherite_monster_cage_lens");
	public static final ModelResourceLocation LENS_LIT = part("block/netherite_monster_cage_lens_lit");
	public static final ModelResourceLocation TIPS_LIT = part("block/netherite_monster_cage_tips_lit");
	public static final ModelResourceLocation TIPS_DARK = part("block/netherite_monster_cage_tips_dark");
	private static final ModelResourceLocation[] SHIELD = new ModelResourceLocation[6];

	static {
		for (Direction d : Direction.values())
			SHIELD[d.get3DDataValue()] = part("block/netherite_monster_cage_shield_" + d.getName());
	}

	private static final float FLOOR_Y = 2.5f, TRAY_Y = 2.75f, WATER_TOP = 13.5f, GAUGE_Y = 3, GAUGE_H = 10;
	private static final float MAX_SCALE = 0.26f, MAX_SIZE = 0.4f;
	private static final float[] CHOW_X = {6.6f, 8, 9.4f};
	private static final float CHOW_Y = 3.1f, CHOW_Z = 11.6f;
	private static final float HATCH_Y = 14, HATCH_Z = 0.5f;

	public MonsterCageRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation part(String path) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource(path));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation model : List.of(HATCH, WATER, WATER_COLUMN, REFLECTOR, LENS, LENS_LIT, TIPS_LIT, TIPS_DARK))
			event.register(model);
		for (ModelResourceLocation model : SHIELD)
			event.register(model);
	}

	@Override
	public AABB getRenderBoundingBox(MonsterCageBlockEntity be) {
		// the shield sits a pixel outside the block
		return new AABB(be.getBlockPos()).inflate(0.1);
	}

	@Override
	protected void renderSafe(MonsterCageBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(MonsterCageBlock.FACING))
			return;
		Direction front = state.getValue(MonsterCageBlock.FACING);
		CameraView camera = CameraView.of(be, ms);
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
		// Translucent things stay here: drawn by Flywheel, they'd hide the monsters behind them. They go last, into the
		// sorted translucent sheet the items use: the plain translucent type writes depth before the items are drawn.
		PartSink sink = PartSink.screen(buffer, state, overlay);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		if (!camera.behindGlass(front)) {
			renderChow(be, ms, buffer, light, overlay);
			renderMonsters(be, partialTicks, ms, buffer, light, camera);
			TrayItems.render(be.getOutput(), TRAY_Y, be.getLevel(), ms, buffer, light, overlay);
		}
		if (be.isFlooded())
			water(be, ms, sink, light);
		ms.popPose();
		if (be.isNetherite())
			shield(be, ms, sink);
	}

	/**
	 * The part models (for this renderer, or Flywheel's visual): the shaft end; a flooded cage's water gauge and trident
	 * hatch; the Netherite cage's coil tips, beacon lens and reflector.
	 */
	public static void parts(MonsterCageBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(MonsterCageBlock.FACING))
			return;
		Direction front = state.getValue(MonsterCageBlock.FACING);
		CobbleGeneratorRenderer.shaft(be, front.getOpposite(), ms, sink, light);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		if (be.isFlooded())
			flooded(be, partialTicks, ms, sink, light);
		if (be.isNetherite())
			netherite(be, partialTicks, ms, sink, light);
		ms.popPose();
	}

	// ---------------------------------------------------------------- Flooded

	private static float waterLevel(MonsterCageBlockEntity be) {
		FluidTank water = be.getWater();
		return Math.min(1, water.getFluidAmount() / (float) water.getCapacity());
	}

	/** The water in the cage, to the tank's level. */
	private static void water(MonsterCageBlockEntity be, PoseStack ms, PartSink sink, int light) {
		float level = waterLevel(be);
		if (level <= 0)
			return;
		ms.pushPose();
		ms.translate(0, FLOOR_Y / 16, 0);
		ms.scale(1, (WATER_TOP - FLOOR_Y) * level, 1);
		sink.part(WATER, ms, light, Sheets.translucentCullBlockSheet());
		ms.popPose();
	}

	private static void flooded(MonsterCageBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		float level = waterLevel(be);
		if (level > 0) {
			ms.pushPose();
			ms.translate(0, GAUGE_Y / 16, 0);
			ms.scale(1, GAUGE_H * level, 1);
			sink.part(WATER_COLUMN, ms, light);
			ms.popPose();
		}
		// the hatch: flaps out on its top edge as a trident goes, then falls shut with a little bounce
		float t = be.ticksSinceHatch() + partialTicks;
		float angle = t < 0 || t > 20 ? 0 : t < 4 ? -70 * t / 4 : t < 10 ? -70
			: t < 17 ? -70 * (1 - (t - 10) / 7) : 8 * Mth.sin((t - 17) * 2.5f) * (20 - t) / 3;
		ms.pushPose();
		ms.translate(8 / 16f, HATCH_Y / 16, HATCH_Z / 16);
		ms.mulPose(Axis.XP.rotationDegrees(angle));
		ms.translate(-8 / 16f, -HATCH_Y / 16, -HATCH_Z / 16);
		sink.part(HATCH, ms, light);
		ms.popPose();
	}

	// ---------------------------------------------------------------- Netherite

	/** Whether the shield shows this tick: always with the field up, flickering through a countdown, else never. */
	public static boolean shieldOn(MonsterCageBlockEntity be, long gameTime) {
		if (!be.hasMonster())
			return false;
		if (be.isFieldUp())
			return true;
		if (!be.isWarning() || be.isPaused())
			return false;
		// irregular drop-outs, a few ticks long, different for each cage
		long slot = gameTime / 3 + be.getBlockPos().asLong() * 31;
		return Mth.murmurHash3Mixer((int) (slot ^ (slot >>> 32))) % 100 >= 45;
	}

	private static void netherite(MonsterCageBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		Level level = be.getLevel();
		long time = level == null ? 0 : level.getGameTime();
		boolean on = shieldOn(be, time);
		sink.part(on ? TIPS_LIT : TIPS_DARK, ms, on ? LightTexture.FULL_BRIGHT : light);
		// the beacon: dark until a countdown, then lit in pulses with its reflector spinning
		boolean alarm = be.isWarning() && !be.isPaused();
		sink.part(alarm && (time / 8) % 2 == 0 ? LENS_LIT : LENS, ms, alarm ? LightTexture.FULL_BRIGHT : light);
		ms.pushPose();
		ms.translate(0.5, 0, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(alarm ? (time + partialTicks) * 27 % 360 : 0));
		ms.translate(-0.5, 0, -0.5);
		sink.part(REFLECTOR, ms, light);
		ms.popPose();
	}

	/** The shield round the block, full-bright and translucent; faces against solid blocks aren't drawn. */
	private static void shield(MonsterCageBlockEntity be, PoseStack ms, PartSink sink) {
		Level level = be.getLevel();
		if (level == null || !shieldOn(be, level.getGameTime()))
			return;
		BlockPos pos = be.getBlockPos();
		for (Direction d : Direction.values()) {
			BlockPos next = pos.relative(d);
			if (level.getBlockState(next).isSolidRender(level, next))
				continue;
			sink.part(SHIELD[d.get3DDataValue()], ms, LightTexture.FULL_BRIGHT, Sheets.translucentCullBlockSheet());
		}
	}

	// ---------------------------------------------------------------- the bowl and the monsters

	private static void renderChow(MonsterCageBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		ItemStack chow = be.getBowlChow();
		if (chow.isEmpty() || be.isNetherite())
			return;
		int count = be.getChowCount();
		int shown = count >= 32 ? 3 : count >= 8 ? 2 : 1;
		var items = Minecraft.getInstance().getItemRenderer();
		for (int i = 0; i < shown; i++) {
			ms.pushPose();
			ms.translate(CHOW_X[i] / 16, CHOW_Y / 16, CHOW_Z / 16);
			ms.mulPose(Axis.XP.rotationDegrees(90));
			ms.mulPose(Axis.ZP.rotationDegrees(i * 70 - 30));
			ms.scale(0.2f, 0.2f, 0.2f);
			items.renderStatic(chow, ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), i);
			ms.popPose();
		}
	}

	private static void renderMonsters(MonsterCageBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
									   int light, CameraView camera) {
		List<CageMonster> shown = be.getShown();
		if (shown.isEmpty())
			return;
		// far off, just one; past 32 blocks, none (as the pens)
		boolean known = camera.known();
		double distanceSqr = known ? camera.distanceSqr() : 0;
		if (distanceSqr > 32 * 32)
			return;
		boolean fewer = distanceSqr > 16 * 16;
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		dispatcher.setRenderShadow(false);
		try {
			for (int i = 0; i < shown.size(); i++) {
				if (fewer && i > 0)
					break;
				CageMonster monster = shown.get(i);
				Entity entity = monster.entity();
				float scale = Math.min(MAX_SCALE, MAX_SIZE / Math.max(entity.getBbHeight(), entity.getBbWidth()));
				ms.pushPose();
				ms.translate(monster.x(partialTicks) / 16, (FLOOR_Y + monster.y(partialTicks)) / 16, monster.z(partialTicks) / 16);
				ms.scale(scale, scale, scale);
				dispatcher.render(entity, 0, 0, 0, monster.yaw(partialTicks), partialTicks, ms, buffer, light);
				ms.popPose();
			}
		} finally {
			dispatcher.setRenderShadow(true);
		}
	}
}
