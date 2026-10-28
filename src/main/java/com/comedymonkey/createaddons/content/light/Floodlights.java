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

	private record Aiming(ResourceKey<Level> dimension, BlockPos light, long until) {}

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
		aiming(level).put(player.getUUID(), new Aiming(level.dimension(), light.immutable(), level.getGameTime() + AIM_TICKS));
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
		if (level.isClientSide)
			return;
		if (event.getPos().equals(a.light())) {
			player.displayClientMessage(Component.translatable(KEY + "stopped"), true);
			return;
		}
		if (!(level.getBlockEntity(a.light()) instanceof LightBlockEntity be) || be.kind() != LightKind.FLOODLIGHT)
			return;
		Result result = aim(level, be, event.getPos());
		player.displayClientMessage(Component.translatable(KEY + result.name().toLowerCase(java.util.Locale.ROOT), (int) range()), true);
	}

	public enum Result {
		AIMED,
		TOO_FAR,
		BLOCKED
	}

	/** Aims the Floodlight at target, if it's in range and in clear sight. */
	public static Result aim(Level level, LightBlockEntity be, BlockPos target) {
		Vec3 to = Vec3.atCenterOf(target);
		if (lens(be).distanceTo(to) > range())
			return Result.TOO_FAR;
		BlockHitResult hit = clip(level, be, to);
		if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(target))
			return Result.BLOCKED;
		be.setAim(target);
		level.playSound(null, be.getBlockPos(), ModSounds.light("floodlight_aim"), SoundSource.BLOCKS, 1f, 0.95f + level.random.nextFloat() * 0.1f);
		return Result.AIMED;
	}

	/** Where its beam starts: just out of its own block, the way it faces. */
	static Vec3 lens(LightBlockEntity be) {
		Direction facing = be.getBlockState().getValue(LightBlock.FACING);
		return Vec3.atCenterOf(be.getBlockPos()).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.55));
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
	 * The air (or water) its beam lands in: in front of the first block in its way, or its target if that's gone. Null when
	 * it isn't aimed, or the beam's way isn't all loaded (it never loads a chunk to look).
	 */
	static @Nullable BlockPos landing(Level level, LightBlockEntity be) {
		BlockPos target = be.aim();
		if (target == null || !level.hasChunksAt(be.getBlockPos(), target))
			return null;
		BlockHitResult hit = clip(level, be, Vec3.atCenterOf(target));
		BlockPos at = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().relative(hit.getDirection()) : target;
		return at.equals(be.getBlockPos()) ? null : at;
	}
}
