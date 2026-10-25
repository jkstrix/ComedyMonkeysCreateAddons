package com.comedymonkey.createaddons.client.tradebot;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Trade Bot's continuous sounds (design/sound/trade-bot.md), which clips don't cue: the rolling loop with its volume
 * and pitch following his real speed, the fast panicked roll while he flees, the tremble while he cowers, and the
 * station's ratchet clicks while it winds him. Each loop fades in and out rather than cutting.
 */
public final class TradeBotLoopSounds {
	/** A loop and how loud it should be for a bot right now (0 to 1). */
	private enum Loop {
		ROLL("roll_loop", bot -> !bot.clip().name().equals("scared_flee") && bot.clip().root() == Clip.Root.GAME
			? speed(bot) / TradeBotMotion.CRUISE : 0),
		ROLL_FAST("roll_fast_loop", bot -> bot.clip().name().equals("scared_flee") ? speed(bot) / (TradeBotMotion.CRUISE * 2) : 0),
		TREMBLE("tremble_loop", bot -> bot.clip().name().equals("scared_cower") ? 1 : 0);

		final String event;
		final ToDoubleFunction<TradeBotEntity> level;

		Loop(String event, ToDoubleFunction<TradeBotEntity> level) {
			this.event = event;
			this.level = level;
		}
	}

	/** The station's winder clicks this often (ticks) while it charges him. */
	private static final int RATCHET_TICKS = 16;
	/** How long a newly queued loop has to start playing before it counts as dropped (ticks). */
	private static final int START_GRACE_TICKS = 20;
	private static final Map<Integer, Map<Loop, LoopSound>> PLAYING = new HashMap<>();

	private TradeBotLoopSounds() {}

	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.isPaused()) {
			if (mc.level == null)
				PLAYING.clear();
			return;
		}
		// also dropped: ones the sound engine let go of without stopping them (F3+T, /stopsound, too many channels), once
		// they've had a moment to start, so that bot's loop starts again
		PLAYING.values().forEach(loops -> loops.values().removeIf(sound -> sound.isStopped()
			|| ++sound.age > START_GRACE_TICKS && !mc.getSoundManager().isActive(sound)));
		PLAYING.values().removeIf(Map::isEmpty);
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof TradeBotEntity bot) || bot.isRemoved())
				continue;
			for (Loop loop : Loop.values()) {
				if (loop.level.applyAsDouble(bot) <= 0.02)
					continue;
				Map<Loop, LoopSound> loops = PLAYING.computeIfAbsent(bot.getId(), id -> new HashMap<>());
				if (!loops.containsKey(loop)) {
					LoopSound sound = new LoopSound(bot, loop);
					loops.put(loop, sound);
					mc.getSoundManager().play(sound);
				}
			}
			if (bot.isCharging() && bot.tickCount % RATCHET_TICKS == 0)
				mc.level.playLocalSound(bot.getX(), bot.getY(), bot.getZ(), ModSounds.tradeBot("key_rattle"), SoundSource.NEUTRAL, 0.35f,
					0.95f + 0.1f * bot.getRandom().nextFloat(), false);
		}
	}

	/** Blocks a tick he's rolling (from where the client has him). */
	private static double speed(TradeBotEntity bot) {
		return Math.sqrt(Mth.square(bot.getX() - bot.xOld) + Mth.square(bot.getZ() - bot.zOld));
	}

	/** One loop on one bot: follows him, eases toward its level, and stops once it has faded out. */
	private static final class LoopSound extends AbstractTickableSoundInstance {
		private final TradeBotEntity bot;
		private final Loop loop;
		private float level;
		/** Client ticks since it was queued. */
		private int age;

		LoopSound(TradeBotEntity bot, Loop loop) {
			super(ModSounds.tradeBot(loop.event), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
			this.bot = bot;
			this.loop = loop;
			looping = true;
			delay = 0;
			volume = 0.01f;
			x = bot.getX();
			y = bot.getY();
			z = bot.getZ();
		}

		@Override
		public void tick() {
			if (bot.isRemoved()) {
				stop();
				return;
			}
			x = bot.getX();
			y = bot.getY();
			z = bot.getZ();
			float want = (float) Mth.clamp(loop.level.applyAsDouble(bot), 0, 1);
			level += (want - level) * 0.25f;
			if (want == 0 && level < 0.02f) {
				stop();
				return;
			}
			volume = Math.max(0.01f, level);
			pitch = loop == Loop.TREMBLE ? 1 : 0.85f + 0.3f * level; // a faster wheel sings a little higher
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}
}
