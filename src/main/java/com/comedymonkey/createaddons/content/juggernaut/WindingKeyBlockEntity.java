package com.comedymonkey.createaddons.content.juggernaut;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The ritual (D16-D19, D52, D54): a Winding Key fitted to the back of the centre of a T of four Brass Blocks, turned at a
 * Hand Crank's 32 RPM or more, winds over 20 s (twice as fast at 64 RPM); stopped or slowed, it slowly unwinds. Fully wound, the effigy and key are
 * gone, the shaft pops off, and the Clockwork Juggernaut stands in front of where the T stood (or kneels on the surface
 * above, if there's no room or sky). It never winds in the Nether, never past halfway with nowhere for him to stand, and
 * never wakes him in peaceful.
 */
public class WindingKeyBlockEntity extends KineticBlockEntity {
	private static final ResourceLocation BRASS_BLOCK = ResourceLocation.fromNamespaceAndPath("create", "brass_block");
	private static final ResourceLocation SHAFT = ResourceLocation.fromNamespaceAndPath("create", "shaft");
	/** Players this near hear of it on their action bar (D18). */
	private static final double TELL = 64;

	/** How wound, 0..1 (synced, for the runes). */
	private float wind;
	private float lastSent;
	/** Why it's stopped short, for the action bar (a lang key), or null. */
	@Nullable
	private String held;
	private boolean runesLit, ready, noRoom;
	private int nextTick;
	/** Game tests: turn as if at this speed, whatever the network says (negative: off). */
	public float testSpeed = -1;

	public WindingKeyBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WINDING_KEY.get(), pos, state);
	}

	public float wind() {
		return wind;
	}

	/** Game tests: set how wound it is. */
	public void setWind(float wind) {
		this.wind = wind;
	}

	private Direction facing() {
		return getBlockState().getValue(WindingKeyBlock.FACING);
	}

	/** The T's blocks, centre first, then its arms and its foot; null unless it's all there (D16). */
	@Nullable
	List<BlockPos> effigy() {
		Direction facing = facing();
		if (facing.getAxis().isVertical())
			return null;
		BlockPos centre = worldPosition.relative(facing.getOpposite());
		Direction side = facing.getClockWise();
		List<BlockPos> t = List.of(centre, centre.relative(side), centre.relative(side.getOpposite()), centre.below());
		Block brass = BuiltInRegistries.BLOCK.get(BRASS_BLOCK);
		for (BlockPos p : t)
			if (!level.getBlockState(p).is(brass))
				return null;
		return t;
	}

	private static boolean allowedIn(Level level) {
		return level.dimension() == Level.OVERWORLD || level.dimension() == Level.END; // D54
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		float rpm = testSpeed >= 0 ? testSpeed : isOverStressed() ? 0 : Math.abs(getSpeed());
		List<BlockPos> effigy = effigy();
		int minSpeed = CMConfig.Server.get(CMConfig.SERVER.ritualMinSpeed);
		boolean turning = rpm >= minSpeed && effigy != null;
		held = null;
		if (turning && !allowedIn(level)) {
			turning = false;
			held = "wrong_place"; // the key turns but never takes
		}
		// 20 s at a Hand Crank's speed; faster turning winds it faster, up to twice as fast; stopped, half the slow pace back
		float step = 1f / (CMConfig.Server.get(CMConfig.SERVER.ritualWindSeconds) * 20);
		wind = turning ? Math.min(1, wind + step * Math.min(2, rpm / minSpeed)) : Math.max(0, wind - step / 2);
		// D52: past halfway, it goes no further until there's somewhere for him to stand (looked for once a second)
		if (turning && wind >= 0.5f && (level.getGameTime() + worldPosition.hashCode()) % 20 == 0)
			noRoom = arrival(effigy) == null;
		if (turning && wind >= 0.5f && noRoom) {
			wind = Math.min(wind, 0.5f);
			held = "no_room";
		}
		if (wind >= 1 && level.getDifficulty() == Difficulty.PEACEFUL) {
			wind = 0.999f; // D19: it winds, but he never wakes
			held = "peaceful";
		}
		if (wind >= 1 && turning) {
			summon(effigy);
			return;
		}
		if (turning || held != null)
			effects(held != null);
		if (Math.abs(wind - lastSent) > 0.02f || wind == 0 && lastSent != 0) {
			lastSent = wind;
			sendData();
		}
	}

	@Nullable
	private JuggernautArrival.Spot arrival(List<BlockPos> effigy) {
		Set<BlockPos> ignore = new java.util.HashSet<>(effigy);
		ignore.add(worldPosition);
		ignore.add(worldPosition.relative(facing())); // the shaft into the key pops off
		return JuggernautArrival.find(level, effigy.get(3), facing().getOpposite(), ignore);
	}

	/** The ticking (faster and higher as it winds), the runes, the steam from the T's seams, and word to those near. */
	private void effects(boolean stalled) {
		ServerLevel server = (ServerLevel) level;
		Vec3 centre = Vec3.atCenterOf(worldPosition.relative(facing().getOpposite()));
		if (!stalled && --nextTick <= 0) {
			nextTick = Math.round(20 - 14 * wind);
			sound("ritual_tick", 0.9f + 0.4f * wind);
		}
		if (!runesLit && wind > 0.1f) {
			runesLit = true;
			sound("ritual_runes", 1);
		}
		if (!ready && wind > 0.9f) {
			ready = true;
			sound("ritual_ready", 1);
		}
		if (!stalled && level.random.nextFloat() < 0.05f + 0.15f * wind) {
			// steam escaping between the brass blocks, more as it nears full
			Direction side = facing().getClockWise();
			Vec3 seam = centre.add(Vec3.atLowerCornerOf(side.getNormal()).scale(level.random.nextBoolean() ? 0.5 : -0.5))
				.add(0, level.random.nextBoolean() ? 0.5 : -0.5, 0);
			server.sendParticles(ParticleTypes.CLOUD, seam.x, seam.y, seam.z, 1, 0.05, 0.05, 0.05, 0.02);
			if (level.random.nextFloat() < 0.15f)
				sound("ritual_steam", 0.9f + 0.2f * level.random.nextFloat());
		}
		if (level.getGameTime() % 40 == 0) {
			Component line = Component.translatable("comedymonkeysaddons.juggernaut.ritual." + (held != null ? held : "winding"))
				.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
			for (ServerPlayer p : server.players())
				if (p.distanceToSqr(centre) < TELL * TELL)
					p.displayClientMessage(line, true);
		}
	}

	/** Fully wound: the effigy and key go, the shaft pops off, and he's there. */
	private void summon(List<BlockPos> effigy) {
		ServerLevel server = (ServerLevel) level;
		JuggernautArrival.Spot spot = arrival(effigy);
		if (spot == null) {
			wind = 0.5f;
			return;
		}
		Direction front = facing().getOpposite();
		BlockPos shaft = worldPosition.relative(facing());
		Vec3 centre = Vec3.atCenterOf(effigy.get(0));
		for (BlockPos p : effigy)
			level.destroyBlock(p, false);
		if (level.getBlockState(shaft).is(BuiltInRegistries.BLOCK.get(SHAFT)))
			level.destroyBlock(shaft, true);
		server.sendParticles(ParticleTypes.CLOUD, centre.x, centre.y, centre.z, 40, 1, 1, 1, 0.08);
		ClockworkJuggernaut j = ModEntities.CLOCKWORK_JUGGERNAUT.get().create(server);
		if (j != null) {
			j.moveTo(spot.pos().x, spot.pos().y, spot.pos().z, front.toYRot(), 0);
			j.finalizeSpawn(server, server.getCurrentDifficultyAt(BlockPos.containing(spot.pos())), MobSpawnType.TRIGGERED, null);
			if (spot.surface()) {
				// summoned underground (D52): the effigy collapses into steam and he kneels on the surface above
				j.makeDormant();
				sound("ritual_collapse", 1);
				Component line = Component.translatable("comedymonkeysaddons.juggernaut.ritual.surface").withStyle(ChatFormatting.GRAY,
					ChatFormatting.ITALIC);
				for (ServerPlayer p : server.players())
					if (p.distanceToSqr(centre) < 32 * 32)
						p.displayClientMessage(line, true);
			} else
				sound("ritual_unfold", 1);
			server.addFreshEntity(j);
		}
		level.destroyBlock(worldPosition, false);
	}

	private void sound(String id, float pitch) {
		SoundEvent sound = ModSounds.juggernaut(id);
		if (sound != null)
			level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1, pitch);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		tooltip.add(Component.literal("    ").append(Component.translatable("comedymonkeysaddons.gui.goggles.winding_key",
			Math.round(wind * 100) + "%").withStyle(ChatFormatting.GRAY)));
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putFloat("Wind", wind);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		wind = tag.getFloat("Wind");
		runesLit = wind > 0.1f;
		ready = wind > 0.9f;
	}
}
