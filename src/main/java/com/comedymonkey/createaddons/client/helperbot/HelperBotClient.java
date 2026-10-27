package com.comedymonkey.createaddons.client.helperbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.client.Hearing;
import com.comedymonkey.createaddons.client.helperbot.HelperBotAnimator.Mode;
import com.comedymonkey.createaddons.client.helperbot.HelperBotAnimator.Situation;
import com.comedymonkey.createaddons.compat.sable.SableCompat;
import com.comedymonkey.createaddons.compat.sable.client.SableSoundPositions;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlock;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity;
import com.comedymonkey.createaddons.content.helperbot.HelperBotMovement;
import com.comedymonkey.createaddons.content.helperbot.HelperBots;
import com.comedymonkey.createaddons.content.line.LineColors;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Helper Bot on the client: ticks his animator and plays his sounds, placed (from his block entity) and on contraptions
 * (from Create's movement context), and draws him in both.
 */
public final class HelperBotClient {
	/** Players this close get watched (and greeted); monsters this close scare him. */
	private static final double WATCH = 8, HOSTILE = 8;
	/** How far up a contraption is searched for a roof over him. */
	private static final int ROOF_SEARCH = 8;
	private static final EntityTypeTest<Entity, Mob> MOBS = EntityTypeTest.forClass(Mob.class);
	private static final Predicate<Mob> HOSTILE_MOB = mob -> mob instanceof Enemy && mob.isAlive();
	/** The monster search's result, reused (client tick only). */
	private static final List<Mob> FOUND = new ArrayList<>(1);
	private static final PoseStack CONTRAPTION_POSE = new PoseStack();

	/** His flat inventory icon's chest panels (tint 1): plain andesite, or his dye a little lighter, as icons are. */
	private static final int PLAIN_PANELS = 0xFF9A9C96;
	private static final float ICON_LIGHTEN = 1.3f;

	private HelperBotClient() {}

	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tint) -> iconTint(stack.get(ModDataComponents.HELPER_BOT_DYE.get()), tint), ModItems.HELPER_BOT.get());
	}

	/** The flat icon's tint for a layer: opaque (the item renderer takes alpha from it), and only on the panels. */
	static int iconTint(@Nullable DyeColor dye, int tint) {
		if (tint != 1)
			return -1;
		if (dye == null)
			return PLAIN_PANELS;
		int c = LineColors.dye(dye), out = 0xFF000000;
		for (int shift = 0; shift <= 16; shift += 8)
			out |= Math.min(255, Math.round((c >> shift & 0xFF) * ICON_LIGHTEN)) << shift;
		return out;
	}

	/** Per bot: his animator and the situation it last saw; also where his sounds play from (one sink, no lambda a tick). */
	static final class State implements Consumer<String> {
		final HelperBotAnimator animator = new HelperBotAnimator();
		final Situation situation = new Situation();
		@Nullable
		Vec3 lastPos;
		int scan;
		boolean scanned;
		/** On an Aeronautics ship: the way he faces in the world (looked up with the scan). */
		double shipFx = Double.NaN, shipFz = Double.NaN;
		/** Set by Ponder scenes: his situation is what the scene says, not what's around him. */
		@Nullable
		Mode forcedMode;
		boolean forcedSchedule, forcedManual;
		/** Placed: where monsters scare him from, and what his renderer is culled by (his block never moves). */
		@Nullable
		AABB hostileArea, renderBox;
		/** On a contraption: his colour (fixed when it was assembled), and his light, worked out once a tick. */
		@Nullable
		DyeColor dye;
		int light;
		long litAt = Long.MIN_VALUE;
		@Nullable
		Level soundLevel;
		double soundX, soundY, soundZ;

		@Override
		public void accept(String id) {
			if (soundLevel != null)
				play(soundLevel, soundX, soundY, soundZ, id);
		}
	}

	/** Ponder: has him act as if driving (with or without a schedule) or keeping shop, and optionally plays a clip now. */
	public static void ponder(HelperBotBlockEntity be, Mode mode, boolean schedule, @Nullable String clip) {
		State st = state(be);
		st.forcedMode = mode;
		st.forcedSchedule = schedule;
		st.forcedManual = clip != null && clip.startsWith("manual");
		st.situation.mode = mode;
		st.situation.hasSchedule = schedule;
		if (clip != null && be.getLevel() != null)
			st.animator.force(clip, AnimationTickHolder.getTicks(be.getLevel()), mode, schedule);
	}

	/** The server's Helper Bot shop setting; off when there's no server config to read (Ponder from the menus). */
	private static boolean shopEnabled() {
		try {
			return CMConfig.SERVER.helperBotShopKeeper.get();
		} catch (IllegalStateException e) {
			return false;
		}
	}

	static State state(HelperBotBlockEntity be) {
		if (be.clientState instanceof State s)
			return s;
		State s = new State();
		be.clientState = s;
		return s;
	}

	static State state(MovementContext context) {
		if (context.temporaryData instanceof State s)
			return s;
		State s = new State();
		context.temporaryData = s;
		s.dye = HelperBotBlockEntity.dyeOf(context.blockEntityData);
		// what the server last said, from the carriage's spawn data (a player who arrives late, or relogs)
		int f = context.data.getByte(HelperBotMovement.STATUS);
		s.situation.paused = (f & HelperBotMovement.PAUSED) != 0;
		s.situation.manual = (f & HelperBotMovement.MANUAL) != 0;
		s.situation.noPath = (f & HelperBotMovement.NO_PATH) != 0;
		s.situation.derailed = (f & HelperBotMovement.DERAILED) != 0;
		return s;
	}

	// ---------- placed ----------

	public static void tickPlaced(HelperBotBlockEntity be) {
		Level level = be.getLevel();
		if (level == null)
			return;
		State st = state(be);
		Situation s = st.situation;
		BlockPos pos = be.getBlockPos();
		Direction facing = be.getBlockState().getValue(HelperBotBlock.FACING);
		s.ducked = be.getBlockState().getValue(HelperBotBlock.DUCKED);
		if (st.forcedMode != null) {
			s.mode = st.forcedMode;
			s.hasSchedule = st.forcedSchedule;
			s.manual = st.forcedManual;
		} else
			s.mode = shopEnabled() && HelperBots.stockTicker(level, pos) != null ? Mode.SHOP : Mode.OFF;
		double ex = pos.getX() + 0.5, ey = pos.getY() + 1.2, ez = pos.getZ() + 0.5, fx = facing.getStepX(), fz = facing.getStepZ();
		// on an Aeronautics ship his block is kept in a plot far from the ship: he looks around from where the ship is,
		// facing the way it turns him
		boolean onShip = false;
		if (SableCompat.LOADED) {
			Vec3 at = SableSoundPositions.heardFrom(ex, ey, ez);
			if (at != null && at.distanceToSqr(ex, ey, ez) > 1e-6) {
				onShip = true;
				// which way the ship turns him changes slowly: looked up with the monster scan, not every tick
				if (st.scan <= 0 || Double.isNaN(st.shipFx)) {
					Vec3 ahead = SableSoundPositions.heardFrom(ex + fx, ey, ez + fz);
					if (ahead != null) {
						st.shipFx = ahead.x - at.x;
						st.shipFz = ahead.z - at.z;
					}
				}
				if (!Double.isNaN(st.shipFx)) {
					fx = st.shipFx;
					fz = st.shipFz;
				}
				ex = at.x;
				ey = at.y;
				ez = at.z;
			}
		}
		Player player = level.getNearestPlayer(ex, ey, ez, WATCH, EntitySelector.NO_SPECTATORS);
		s.playerYaw = player == null ? Float.NaN : yawTo(fx, fz, player.getX() - ex, player.getZ() - ez);
		// monsters and weather: a few times a second is plenty. The first look is at once; after that each bot looks
		// on his own beat (from where he stands), so a crowd of them doesn't all search for monsters in the same tick
		if (st.scan-- <= 0) {
			st.scan = st.scanned ? 10 : Math.floorMod(pos.hashCode(), 10);
			st.scanned = true;
			if (st.hostileArea == null || onShip)
				st.hostileArea = new AABB(ex - 0.5, ey - 1.2, ez - 0.5, ex + 0.5, ey - 0.2, ez + 0.5).inflate(HOSTILE);
			s.hostileNear = hostileIn(level, st.hostileArea);
			// rain where his head is: the block above his own (ducked or not, anything over it is a roof)
			s.raining = level.isRainingAt(BlockPos.containing(ex, ey - 0.2, ez));
			s.thundering = level.isThundering() && s.raining;
			s.night = level.isNight();
		}
		s.wave = be.waveRequested;
		be.waveRequested = false;
		s.order = be.orderPlaced;
		be.orderPlaced = false;
		st.soundLevel = level;
		st.soundX = pos.getX() + 0.5;
		st.soundY = pos.getY() + 0.5;
		st.soundZ = pos.getZ() + 0.5;
		st.animator.tick(s, AnimationTickHolder.getTicks(level), st);
	}

	/** His renderer's culling box: he's half a block taller than his block, and his arms and swivel reach past its sides. */
	public static AABB renderBox(HelperBotBlockEntity be) {
		State st = state(be);
		if (st.renderBox == null)
			st.renderBox = new AABB(be.getBlockPos()).expandTowards(0, 1, 0).inflate(0.5);
		return st.renderBox;
	}

	public static void renderPlaced(HelperBotBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		Level level = be.getLevel();
		if (level == null)
			return;
		State st = state(be);
		HelperBotModels.Baked model = HelperBotModels.baked();
		st.animator.pose(model.rig(), st.situation, AnimationTickHolder.getTicks(level) + partialTick, partialTick);
		HelperBotModels.draw(ms, buffers, light, overlay, be.getBlockState().getValue(HelperBotBlock.FACING), be.dye(), model);
	}

	// ---------- on a contraption ----------

	public static void tickOnContraption(MovementContext context) {
		State st = state(context);
		Situation s = st.situation;
		s.ducked = context.state.hasProperty(HelperBotBlock.DUCKED) && context.state.getValue(HelperBotBlock.DUCKED);
		s.wave = false;
		if (context.contraption.entity instanceof CarriageContraptionEntity cce) {
			s.mode = HelperBotMovement.inControl(context) ? Mode.DRIVING : Mode.PASSENGER;
			s.hasSchedule = cce.hasSchedule();
			Carriage carriage = cce.getCarriage();
			s.honk = carriage != null && carriage.train != null && carriage.train.honkTicks > 0;
		} else {
			s.mode = Mode.PASSENGER;
			s.hasSchedule = false;
			s.honk = false;
		}
		// his speed is how far the contraption moved since last tick, and whether that's the way he faces
		Vec3 pos = context.contraption.entity.position();
		Direction facing = context.state.hasProperty(HelperBotBlock.FACING) ? context.state.getValue(HelperBotBlock.FACING) : Direction.NORTH;
		Vec3 ahead = context.rotation != null ? context.rotation.apply(Vec3.atLowerCornerOf(facing.getNormal())) : Vec3.atLowerCornerOf(facing.getNormal());
		if (st.lastPos != null) {
			double dx = pos.x - st.lastPos.x, dy = pos.y - st.lastPos.y, dz = pos.z - st.lastPos.z;
			s.speed = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (s.speed > 0.01f) {
				s.along = along(dx, dy, dz, ahead);
				// facing across the train he's neither its front nor its back: keep what he was
				if (Math.abs(s.along) > 0.5f)
					s.forward = s.along > 0;
			}
		} else
			s.speed = 0;
		st.lastPos = pos;
		Vec3 at = context.position != null ? context.position : pos;
		// players to look at and wave to, monsters by the train and the weather, as placed
		Player player = context.world.getNearestPlayer(at.x, at.y + 0.7, at.z, WATCH, EntitySelector.NO_SPECTATORS);
		s.playerYaw = player == null ? Float.NaN : yawTo(ahead.x, ahead.z, player.getX() - at.x, player.getZ() - at.z);
		if (st.scan-- <= 0) {
			st.scan = 10;
			s.hostileNear = hostileIn(context.world, new AABB(BlockPos.containing(at)).inflate(HOSTILE));
			// the world's rain doesn't know the carriage's own roof
			s.raining = !roofed(context.contraption.getBlocks(), context.localPos) && context.world.isRainingAt(BlockPos.containing(at).above());
			s.thundering = context.world.isThundering() && s.raining;
		}
		st.soundLevel = context.world;
		st.soundX = at.x;
		st.soundY = at.y;
		st.soundZ = at.z;
		st.animator.tick(s, AnimationTickHolder.getTicks(context.world), st);
	}

	/** Without Flywheel: drawn by the contraption's renderer (with it, {@link HelperBotActorVisual} draws him). */
	public static void renderOnContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices,
										   MultiBufferSource buffers) {
		State st = state(context);
		int ticks = AnimationTickHolder.getTicks(context.world);
		float partialTick = AnimationTickHolder.getPartialTicks(context.world);
		HelperBotModels.Baked model = HelperBotModels.baked();
		st.animator.pose(model.rig(), st.situation, ticks + partialTick, partialTick);
		// lit by the world where he is, and by any lights on the contraption around him (worked out once a tick)
		if (st.litAt != ticks) {
			st.litAt = ticks;
			int inside = LevelRenderer.getLightColor(renderWorld, context.localPos);
			int outside = context.position == null ? inside : LevelRenderer.getLightColor(context.world, BlockPos.containing(context.position));
			st.light = LightTexture.pack(Math.max(LightTexture.block(inside), LightTexture.block(outside)),
				Math.max(LightTexture.sky(inside), LightTexture.sky(outside)));
		}
		// one pose stack for every bot (render thread only): the contraption's matrices, then his block
		PoseStack ms = CONTRAPTION_POSE;
		ms.last().pose().set(matrices.getModelViewProjection().last().pose());
		ms.last().normal().set(matrices.getModelViewProjection().last().normal());
		ms.translate(context.localPos.getX(), context.localPos.getY(), context.localPos.getZ());
		Direction facing = context.state.hasProperty(HelperBotBlock.FACING) ? context.state.getValue(HelperBotBlock.FACING) : Direction.NORTH;
		HelperBotModels.draw(ms, buffers, st.light, OverlayTexture.NO_OVERLAY, facing, st.dye, model);
	}

	/** On a contraption with Flywheel on: his instances, posed every frame ({@code HelperBotMovement.createVisual}). */
	public static ActorVisual actorVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext context) {
		return new HelperBotActorVisual(visualizationContext, simulationWorld, context);
	}

	/** The server's word on a driving bot's train ({@code HelperBotStatusPayload}): finds him among the contraption's actors. */
	public static void applyStatus(com.comedymonkey.createaddons.network.HelperBotStatusPayload payload) {
		net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
		if (level == null || !(level.getEntity(payload.entityId()) instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity entity)
			|| entity.getContraption() == null)
			return;
		for (var actor : entity.getContraption().getActors()) {
			if (!actor.left.pos().equals(payload.localPos()))
				continue;
			Situation s = state(actor.right).situation;
			int f = payload.flags();
			s.paused = (f & HelperBotMovement.PAUSED) != 0;
			s.manual = (f & HelperBotMovement.MANUAL) != 0;
			s.noPath = (f & HelperBotMovement.NO_PATH) != 0;
			s.derailed = (f & HelperBotMovement.DERAILED) != 0;
		}
	}

	/**
	 * Draws the Helper Bot keeping shop for this ticker in Create's shop screen, where it draws a Blaze Burner keeper,
	 * posed as he is in the world (so his nod shows when an order goes out).
	 */
	public static void drawInShop(net.minecraft.client.gui.GuiGraphics graphics, BlockPos tickerPos, int guiLeft, int guiTop, int windowHeight,
								  float partialTick) {
		Level level = net.minecraft.client.Minecraft.getInstance().level;
		if (level == null)
			return;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (!(level.getBlockEntity(tickerPos.relative(side)) instanceof HelperBotBlockEntity be))
				continue;
			State st = state(be);
			HelperBotModels.Baked model = HelperBotModels.baked();
			st.animator.pose(model.rig(), st.situation, AnimationTickHolder.getTicks(level) + partialTick, partialTick);
			PoseStack ms = graphics.pose();
			ms.pushPose();
			ms.translate(guiLeft - 35, guiTop + windowHeight - 43, 100);
			ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-22.5f));
			ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-45));
			ms.scale(36, -36, 36);
			ms.translate(-0.5, 0, -0.5);
			com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();
			HelperBotModels.draw(ms, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Direction.SOUTH, be.dye(), model);
			graphics.flush();
			com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
			ms.popPose();
			return;
		}
	}

	// ---------- shared ----------

	/**
	 * Degrees from his facing ({@code fx}, {@code fz}, any length) to the offset ({@code dx}, {@code dz}), + to his left
	 * (the way his swivel turns for + Blockbench y).
	 */
	static float yawTo(double fx, double fz, double dx, double dz) {
		// his left, looking along his facing: north (0, -1) has west (-1, 0) on its left
		double lx = fz, lz = -fx;
		return (float) Math.toDegrees(Math.atan2(dx * lx + dz * lz, dx * fx + dz * fz));
	}

	/** How much a bot facing {@code ahead} faces the move ({@code dx}, {@code dy}, {@code dz}): 1 ahead, -1 back, 0 across. */
	static float along(double dx, double dy, double dz, Vec3 ahead) {
		double length = Math.sqrt((dx * dx + dy * dy + dz * dz) * ahead.lengthSqr());
		return length < 1e-9 ? 1 : (float) ((dx * ahead.x + dy * ahead.y + dz * ahead.z) / length);
	}

	/** Whether a contraption has a block over him (a carriage roof): looked for a few blocks up. */
	static boolean roofed(Map<BlockPos, ?> blocks, BlockPos local) {
		for (int up = 1; up <= ROOF_SEARCH; up++)
			if (blocks.containsKey(local.above(up)))
				return true;
		return false;
	}

	/** Whether a monster is in the area: the first one found will do. */
	private static boolean hostileIn(Level level, AABB area) {
		level.getEntities(MOBS, area, HOSTILE_MOB, FOUND, 1);
		boolean found = !FOUND.isEmpty();
		FOUND.clear();
		return found;
	}

	private static void play(Level level, double x, double y, double z, String id) {
		SoundEvent sound = ModSounds.helperBot(id);
		// out of earshot (measured from where a ship really is): no sound channel taken
		if (sound != null && Hearing.inRange(sound, 1, x, y, z, Hearing.ONE_SHOT_MARGIN))
			level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, 1, 1, false);
	}

	// ---------- batching ----------

	/** His render types each get a buffer of their own (mod bus), so drawing him never ends another batch, nor his. */
	public static void registerRenderBuffers(RegisterRenderBuffersEvent event) {
		for (RenderType type : HelperBotModels.RENDER_TYPES)
			event.registerRenderBuffer(type);
	}

	/**
	 * Drawn once the block entities are: a mod's own buffer is otherwise only drawn at the very end of the level,
	 * after translucent terrain, and water or glass in front of him would hide him.
	 */
	public static void endBatches(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)
			return;
		MultiBufferSource.BufferSource buffers = net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
		for (RenderType type : HelperBotModels.RENDER_TYPES)
			buffers.endBatch(type);
	}
}
