package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.clock.ClockTime;
import com.comedymonkey.createaddons.content.clock.CuckooClockBlock;
import com.comedymonkey.createaddons.content.clock.CuckooClockBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws a Cuckoo Clock's moving parts: the shaft end in the back and the cogs behind the side portholes, turning with
 * the network; the hands, the sun and moon disc and the sky behind it, set by the time; the swinging pendulum; and the show, with its
 * doors, the cuckoo and the carved villager with its little workstation. All of it is modelled with the front facing
 * north (see the parts' models); the pixel numbers below are in that model space, where the top block is y 0..16.
 */
public class CuckooClockRenderer extends SafeBlockEntityRenderer<CuckooClockBlockEntity> {
	public static final ModelResourceLocation HOUR_HAND = model("cuckoo_clock_hour_hand");
	public static final ModelResourceLocation MINUTE_HAND = model("cuckoo_clock_minute_hand");
	public static final ModelResourceLocation SKY_DISC = model("cuckoo_clock_sky_disc");
	public static final ModelResourceLocation DOOR_LEFT = model("cuckoo_clock_door_left");
	public static final ModelResourceLocation DOOR_RIGHT = model("cuckoo_clock_door_right");
	public static final ModelResourceLocation LOW_DOOR_LEFT = model("cuckoo_clock_low_door_left");
	public static final ModelResourceLocation LOW_DOOR_RIGHT = model("cuckoo_clock_low_door_right");
	public static final ModelResourceLocation BIRD = model("cuckoo_clock_bird");
	public static final ModelResourceLocation VILLAGER = model("cuckoo_clock_villager");
	public static final ModelResourceLocation PENDULUM = model("cuckoo_clock_pendulum");
	public static final ModelResourceLocation COG = model("cuckoo_clock_cog");

	/** Pivots: the dial, the sun and moon disc, the pendulum (x, y, z). */
	private static final float DIAL_Y = 3.6f, DISC_Y = 7, PENDULUM_Y = -3.5f, PENDULUM_Z = 8.75f;
	/** Door hinges: the cuckoo's at z 7.15, the villager's at z 7.2, both at x 6.5 and 9.5. */
	private static final float HINGE_LEFT = 6.5f, HINGE_RIGHT = 9.5f, BIRD_HINGE_Z = 7.15f, LOW_HINGE_Z = 7.2f;
	private static final float DOOR_DEGREES = 100;
	/** How far back the bird and the villager sit when inside. */
	private static final float BIRD_IN = 2.6f, VILLAGER_IN = 3.5f;
	/** Where the workstation stands on the balcony, and its size. */
	private static final float STATION_X = 8, STATION_Y = -3, STATION_Z = 4.2f, STATION_SIZE = 1.5f;
	/** The east porthole's cog is the west one moved across the case. */
	private static final float COG_SPAN = 9.55f;
	private static final float PENDULUM_DEGREES = 14;
	/** The sky strip behind the window, and the rectangle it fills (just behind the sun and moon disc). */
	private static final ResourceLocation SKY_TEXTURE = ComedyMonkeysCreateAddons.asResource("block/cuckoo_clock_sky");
	private static final float SKY_X0 = 4.75f, SKY_X1 = 11.25f, SKY_Y0 = 6.9f, SKY_Y1 = 10.1f, SKY_Z = 7.9f;
	private static final int SKY_COLUMNS = 12;
	/** The strip's layout (as the texture is drawn): 128 rows, 24 a unit of sky value, value 2.5 at the top. */
	private static final float SKY_ROWS = 128, SKY_ROWS_PER_UNIT = 24, SKY_TOP = 2.5f;
	/** How far the sun's height moves the sky, how much the sky changes from the window's bottom to its top, and the sag. */
	private static final float SKY_SUN = 0.5f, SKY_SPAN = 0.8f, SKY_SAG = 0.45f;

	public CuckooClockRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {HOUR_HAND, MINUTE_HAND, SKY_DISC, DOOR_LEFT, DOOR_RIGHT,
			LOW_DOOR_LEFT, LOW_DOOR_RIGHT, BIRD, VILLAGER, PENDULUM, COG})
			event.register(m);
	}

	@Override
	protected void renderSafe(CuckooClockBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(CuckooClockBlock.FACING))
			return;
		Direction front = state.getValue(CuckooClockBlock.FACING);
		CobbleGeneratorRenderer.renderShaft(be, state, front.getOpposite(), ms, buffer, light, overlay);

		VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);

		// the cogs turn with the shaft
		float cog = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), front.getAxis());
		for (int side = 0; side < 2; side++) {
			ms.pushPose();
			ms.translate(side * COG_SPAN / 16, 0, 0);
			turn(ms, Axis.XP, cog * Mth.RAD_TO_DEG, 3.225f, 4, 12);
			draw(ms, vc, state, COG, light);
			ms.popPose();
		}

		// hands, clockwise from 12 as seen from the front; the disc has noon's sun at the top and turns once a day
		float time = be.getShownTime(partialTicks);
		float hours = (Mth.positiveModulo(time, ClockTime.TICKS_PER_DAY) / 1000f + 6) % 12;
		drawTurned(ms, vc, state, HOUR_HAND, light, Axis.ZP, hours * 30, 8, DIAL_Y, 0);
		drawTurned(ms, vc, state, MINUTE_HAND, light, Axis.ZP, Mth.positiveModulo(time, 1000) / 1000f * 360, 8, DIAL_Y, 0);
		float day = Mth.positiveModulo(time - ClockTime.NOON, ClockTime.TICKS_PER_DAY) / ClockTime.TICKS_PER_DAY;
		renderSky(ms, vc, time, light);
		drawTurned(ms, vc, state, SKY_DISC, light, Axis.ZP, day * 360, 8, DISC_Y, 0);

		float swing = 0;
		if (be.isRunning())
			swing = PENDULUM_DEGREES * Mth.sin(be.getSwingTicks(partialTicks) * Mth.TWO_PI / CuckooClockBlockEntity.SWING_TICKS);
		drawTurned(ms, vc, state, PENDULUM, light, Axis.ZP, swing, 8, PENDULUM_Y, PENDULUM_Z);

		renderShow(be, partialTicks, ms, buffer, vc, state, light, overlay);
		ms.popPose();
	}

	/**
	 * The sky behind the sun and moon disc, which stays put while the disc turns: a slice of the sky strip (one
	 * gradient from day at the top to night at the bottom) that slides with the sun's height. By day the window shows
	 * the sky getting bluer upwards, by night darker upwards, and around sunrise and sunset the dusk glow on the
	 * horizon. The slice is drawn as thin columns, each sampled a little higher towards the middle, so the bands sag in
	 * the middle of the window and rise to the humps at either side.
	 */
	private static void renderSky(PoseStack ms, VertexConsumer vc, float time, int light) {
		TextureAtlasSprite sky = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SKY_TEXTURE);
		// the sun's height: 0 at 6:00 and 18:00, 1 at noon, -1 at midnight
		float sun = Mth.sin(Mth.positiveModulo(time, ClockTime.TICKS_PER_DAY) / ClockTime.TICKS_PER_DAY * Mth.TWO_PI);
		// how the sky changes upwards: bluer by day (from the dusk glow at the horizon around sunrise and sunset), the
		// same all over for a moment in twilight, then darker upwards by night
		float upwards = Mth.clamp((sun + 0.12f) / 0.25f, -1, 1) * SKY_SPAN;
		PoseStack.Pose pose = ms.last();
		for (int i = 0; i < SKY_COLUMNS; i++) {
			float x0 = SKY_X0 + (SKY_X1 - SKY_X0) * i / SKY_COLUMNS, x1 = SKY_X0 + (SKY_X1 - SKY_X0) * (i + 1) / SKY_COLUMNS;
			float sag0 = sag(x0), sag1 = sag(x1);
			// seen from the front the model's x runs right to left, so u runs the other way
			float u0 = sky.getU(1 - (float) i / SKY_COLUMNS), u1 = sky.getU(1 - (float) (i + 1) / SKY_COLUMNS);
			float top0 = skyV(sky, SKY_SUN * sun + upwards * (1 + sag0)), top1 = skyV(sky, SKY_SUN * sun + upwards * (1 + sag1));
			float bottom0 = skyV(sky, SKY_SUN * sun + upwards * sag0), bottom1 = skyV(sky, SKY_SUN * sun + upwards * sag1);
			// counter-clockwise as seen from the front (where x runs right to left), or it's culled as a back face
			skyVertex(vc, pose, x0, SKY_Y1, u0, top0, light);
			skyVertex(vc, pose, x1, SKY_Y1, u1, top1, light);
			skyVertex(vc, pose, x1, SKY_Y0, u1, bottom1, light);
			skyVertex(vc, pose, x0, SKY_Y0, u0, bottom0, light);
		}
	}

	/** How much higher in the sky a column is sampled: most in the middle of the window, none at its sides. */
	private static float sag(float x) {
		float across = (x - 8) / ((SKY_X1 - SKY_X0) / 2);
		return SKY_SAG * (1 - across * across);
	}

	/** The strip's v for a sky value; row r of the strip holds the value SKY_TOP - (r + 0.5) / SKY_ROWS_PER_UNIT. */
	private static float skyV(TextureAtlasSprite sky, float value) {
		float row = (SKY_TOP - value) * SKY_ROWS_PER_UNIT;
		return sky.getV(Mth.clamp(row / SKY_ROWS, 0, 1));
	}

	private static void skyVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
		vc.addVertex(pose, x / 16, y / 16, SKY_Z / 16).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light).setNormal(pose, 0, 0, -1);
	}

	/** The doors, the cuckoo and the villager: closed and inside unless a show is on. */
	private static void renderShow(CuckooClockBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
								   VertexConsumer vc, BlockState state, int light, int overlay) {
		float t = be.getShowTime(partialTicks);
		int calls = be.getShowCalls();
		float birdDoors = t < 0 ? 0 : ClockTime.birdDoors(t, calls), birdOut = t < 0 ? 0 : ClockTime.birdOut(t, calls);
		float lowDoors = t < 0 ? 0 : ClockTime.villagerDoors(t, calls), villagerOut = t < 0 ? 0 : ClockTime.villagerOut(t, calls);

		// the leaves swing out towards the front: the left one (reaching +x from its hinge) about +y, the right about -y
		drawTurned(ms, vc, state, DOOR_LEFT, light, Axis.YP, birdDoors * DOOR_DEGREES, HINGE_LEFT, 0, BIRD_HINGE_Z);
		drawTurned(ms, vc, state, DOOR_RIGHT, light, Axis.YP, -birdDoors * DOOR_DEGREES, HINGE_RIGHT, 0, BIRD_HINGE_Z);
		drawTurned(ms, vc, state, LOW_DOOR_LEFT, light, Axis.YP, lowDoors * DOOR_DEGREES, HINGE_LEFT, 0, LOW_HINGE_Z);
		drawTurned(ms, vc, state, LOW_DOOR_RIGHT, light, Axis.YP, -lowDoors * DOOR_DEGREES, HINGE_RIGHT, 0, LOW_HINGE_Z);

		if (birdOut > 0) {
			// it leans forward and tips its head down a little as it calls
			float call = ClockTime.birdCall(t, calls);
			ms.pushPose();
			ms.translate(0, 0, (1 - birdOut) * BIRD_IN / 16 - call * 0.4f / 16);
			turn(ms, Axis.XP, -call * 12, 8, 11.5f, 6.4f);
			draw(ms, vc, state, BIRD, light);
			ms.popPose();
		}

		if (villagerOut > 0) {
			ms.pushPose();
			ms.translate(0, 0, (1 - villagerOut) * VILLAGER_IN / 16);
			draw(ms, vc, state, VILLAGER, light);
			BlockState station = be.getWorkstation();
			if (station != null) {
				if (station.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
					station = station.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
				ms.pushPose();
				ms.translate((STATION_X - STATION_SIZE / 2) / 16, STATION_Y / 16, (STATION_Z - STATION_SIZE / 2) / 16);
				ms.scale(STATION_SIZE / 16, STATION_SIZE / 16, STATION_SIZE / 16);
				Minecraft.getInstance().getBlockRenderer().renderSingleBlock(station, ms, buffer, light, overlay,
					ModelData.EMPTY, null);
				ms.popPose();
			}
			ms.popPose();
		}
	}

	/** Turns the pose by {@code degrees} about {@code axis} through the model-space pixel point (x, y, z). */
	private static void turn(PoseStack ms, Axis axis, float degrees, float x, float y, float z) {
		ms.translate(x / 16, y / 16, z / 16);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-x / 16, -y / 16, -z / 16);
	}

	private static void drawTurned(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation location,
								   int light, Axis axis, float degrees, float x, float y, float z) {
		ms.pushPose();
		turn(ms, axis, degrees, x, y, z);
		draw(ms, vc, state, location, light);
		ms.popPose();
	}

	private static void draw(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation location, int light) {
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), vc, state,
			Minecraft.getInstance().getModelManager().getModel(location), 1, 1, 1, light, OverlayTexture.NO_OVERLAY,
			ModelData.EMPTY, RenderType.cutout());
	}
}
