package com.comedymonkey.createaddons.content.light;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The Floodlight (design/specs/electric-lights.md, D18, P13, P27): wrench it, then click the block to light, up to 32 blocks
 * away (config) in clear sight. While it's lit it puts a spot of light (LightSpotBlock) in the air at that block's face
 * toward it, or at the face of whatever now stands in the beam, looked at again each time the light wakes (at least once a
 * second); the spot goes when it's dark, broken or aimed elsewhere.
 */
public final class Floodlights {
	private static final String KEY = ComedyMonkeysCreateAddons.MOD_ID + ".floodlight.";
	/** How long a wrenched Floodlight waits for its target (ticks). */
	private static final int AIM_TICKS = 600;

	private record Aiming(ResourceKey<Level> dimension, BlockPos light, long from, long until) {}

	/** Who's aiming which Floodlight, on each side (an integrated server shares these statics with its client). */
	private static final Map<UUID, Aiming> SERVER = new HashMap<>(), CLIENT = new HashMap<>();

	private Floodlights() {}

	public static double range() {
		return CMConfig.SERVER.floodlightRange();
	}

	private static Map<UUID, Aiming> aiming(Level level) {
		return level.isClientSide ? CLIENT : SERVER;
	}

	/** The wrench on a Floodlight: its next click on a block (with the wrench) is its target. */
	public static void startAiming(Level level, Player player, BlockPos light) {
		long now = level.getGameTime();
		aiming(level).put(player.getUUID(), new Aiming(level.dimension(), light.immutable(), now, now + AIM_TICKS));
		if (!level.isClientSide)
			player.displayClientMessage(Component.translatable(KEY + "aiming"), true);
	}

	/** A player's click while aiming: on the Floodlight again it stops; on any other block, that's the target. */
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		Level level = event.getLevel();
		if (event.getHand() != InteractionHand.MAIN_HAND)
			return;
		Map<UUID, Aiming> map = aiming(level);
		Aiming a = map.get(player.getUUID());
		if (a == null)
			return;
		map.remove(player.getUUID());
		if (!a.dimension().equals(level.dimension()) || level.getGameTime() > a.until()
			|| !(player.getMainHandItem().getItem() instanceof com.simibubi.create.content.equipment.wrench.WrenchItem))
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		if (event.getPos().equals(a.light())) {
			// (a held click repeats: only a fresh one stops it)
			if (level.getGameTime() - a.from() < 10) {
				map.put(player.getUUID(), a);
				return;
			}
			if (!level.isClientSide)
				player.displayClientMessage(Component.translatable(KEY + "stopped"), true);
			return;
		}
		if (level.isClientSide || !level.isLoaded(a.light()) || !(level.getBlockEntity(a.light()) instanceof LightBlockEntity be)
			|| be.kind() != LightKind.FLOODLIGHT)
			return;
		Result result = aim(level, be, event.getPos(), event.getFace());
		player.displayClientMessage(Component.translatable(KEY + result.name().toLowerCase(java.util.Locale.ROOT), (int) range()), true);
	}

	public enum Result {
		AIMED,
		TOO_FAR,
		BLOCKED
	}

	/** Aims the Floodlight at target (the face turned most toward it), if it's in range and in clear sight. */
	public static Result aim(Level level, LightBlockEntity be, BlockPos target) {
		return aim(level, be, target, null);
	}

	/**
	 * Aims the Floodlight at a face of target, if it's in range and it can see that face: the face clicked, else the one
	 * turned most toward it. (A face, not the block's middle: a block set in a wall, seen at a slant, is reached past its
	 * neighbours' faces only at its own.)
	 */
	public static Result aim(Level level, LightBlockEntity be, BlockPos target, @Nullable Direction clicked) {
		Vec3 from = lens(be);
		if (from.distanceTo(Vec3.atCenterOf(target)) > range())
			return Result.TOO_FAR;
		if (!loadedBetween(level, be.getBlockPos(), target))
			return Result.BLOCKED;
		Direction face = clicked != null && sees(level, be, target, clicked) ? clicked : null;
		double best = 0;
		if (face == null)
			for (Direction d : Direction.values()) {
				double toward = normal(d).dot(from.subtract(facePoint(target, d)).normalize());
				if (toward > best && sees(level, be, target, d)) {
					best = toward;
					face = d;
				}
			}
		if (face == null)
			return Result.BLOCKED;
		be.setAim(target, face);
		level.playSound(null, be.getBlockPos(), ModSounds.light("floodlight_aim"), SoundSource.BLOCKS, 1f, 0.95f + level.random.nextFloat() * 0.1f);
		return Result.AIMED;
	}

	/** Where its beam starts: just out of its own block, the way it faces. */
	static Vec3 lens(LightBlockEntity be) {
		Direction facing = be.getBlockState().getValue(LightBlock.FACING);
		return Vec3.atCenterOf(be.getBlockPos()).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.55));
	}

	/** Every chunk between a and b is loaded (corners in order: the game's own check assumes it). */
	private static boolean loadedBetween(Level level, BlockPos a, BlockPos b) {
		return level.hasChunksAt(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
			Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
	}

	/** A world closed: nobody's aiming any more. */
	public static void forget(boolean client) {
		(client ? CLIENT : SERVER).clear();
	}

	private static Vec3 normal(Direction d) {
		return Vec3.atLowerCornerOf(d.getNormal());
	}

	/** The middle of target's face, just out in front of it. */
	private static Vec3 facePoint(BlockPos target, Direction face) {
		return Vec3.atCenterOf(target).add(normal(face).scale(0.51));
	}

	/** The light sees that face of target: turned toward it, nothing between (nor in front of the face). */
	private static boolean sees(Level level, LightBlockEntity be, BlockPos target, Direction face) {
		Vec3 point = facePoint(target, face);
		return normal(face).dot(lens(be).subtract(point)) > 0 && clip(level, be, point).getType() == HitResult.Type.MISS;
	}

	private static BlockHitResult clip(Level level, LightBlockEntity be, Vec3 to) {
		return level.clip(new ClipContext(lens(be), to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
	}

	/** Lit: its spot where its beam lands now (moved if a block came into the beam, or went); dark: none. */
	static void beam(LightBlockEntity be, boolean lit) {
		Level level = be.getLevel();
		if (level == null || level.isClientSide)
			return;
		BlockPos want = lit ? landing(level, be) : null;
		BlockPos had = be.spot();
		if (had != null && !had.equals(want))
			LightSpotBlock.remove(level, had, be.getBlockPos());
		if (want != null && !LightSpotBlock.place(level, want, be.getBlockPos(), be.kind().level(be.step()), be.dye()))
			want = null;
		be.setSpot(want);
	}

	/**
	 * The air (or water) its beam lands in: in front of its target's face, or of the first block in the way to it. Null when
	 * it isn't aimed, or the beam's way isn't all loaded (it never loads a chunk to look).
	 */
	static @Nullable BlockPos landing(Level level, LightBlockEntity be) {
		BlockPos target = be.aim();
		// (and still in reach: a light carried off by a contraption, or the reach made shorter)
		if (target == null || !loadedBetween(level, be.getBlockPos(), target) || lens(be).distanceTo(Vec3.atCenterOf(target)) > range() + 1)
			return null;
		Direction face = be.aimFace();
		BlockHitResult hit = clip(level, be, face != null ? facePoint(target, face) : Vec3.atCenterOf(target));
		BlockPos at = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().relative(hit.getDirection())
			: face != null ? target.relative(face) : target;
		return at.equals(be.getBlockPos()) ? null : at;
	}
}
