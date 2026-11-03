package com.comedymonkey.createaddons.content.jetpack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.mixin.ServerGamePacketListenerImplAccessor;
import com.comedymonkey.createaddons.network.JetpackInputPayload;
import com.comedymonkey.createaddons.network.JetpackStatePayload;
import com.comedymonkey.createaddons.network.JetpackTogglePayload;
import com.comedymonkey.createaddons.registry.ModAttachments;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * How the Clockwork Jetpack flies (jetpack spec D4-D17, D23, D27-D29). Like Create Jetpack, the wearer's client moves
 * them and the server spends the wind: thrust (Jump up, Sneak down, the movement keys) at 450 s a wind, hover (H) at
 * 900 s, holding its height when no key is held (hover descend 0), and the wings (J): vanilla's Elytra glide, untouched
 * and free, with Jump held boosting it at 900 s a wind. Out of wind, the wings glide on as a plain Elytra; the jetpack
 * modes simply stop. Nothing runs in Creative's own flight, while riding, or in vanilla fall flying from a
 * chest Elytra (D23: one flyer at a time, the Clockwork Jetpack first only while its engine is on).
 */
public final class JetpackFlight {
	/** An Elytra's steady shallow glide, blocks a tick (about 8 blocks a second); boosting flies a little faster (D16). */
	public static final double GLIDE_SPEED = 0.4;
	/** How hard the fans push along the look while under boosting speed. */
	private static final double FLIGHT_THRUST = 0.06;
	/** Toggle keys are taken at most this often (ticks), so a client can't flood its neighbours with sounds and states. */
	private static final int TOGGLE_TICKS = 4;
	private static final net.minecraft.resources.ResourceLocation SWIM_ID = ComedyMonkeysCreateAddons.asResource("clockwork_jetpack_swim");

	/** Server: what each player is holding, from {@link JetpackInputPayload}. */
	private static final Map<UUID, Integer> INPUT = new HashMap<>();
	/** Server: the fraction of a tick of wind used but not yet written to the jetpack (always under 1). */
	private static final Map<UUID, Double> SPENT = new HashMap<>();
	/** Server: whether each player was flying on wind last tick (for the run-down sound and the boost's start). */
	private static final Map<UUID, Integer> WAS = new HashMap<>();
	/** Server: when each player last toggled something (game time). */
	private static final Map<UUID, Long> LAST_TOGGLE = new HashMap<>();

	private static final int WAS_RUNNING = 1, WAS_BOOSTING = 2;

	private JetpackFlight() {}

	public static JetpackState state(Player player) {
		return player.getData(ModAttachments.JETPACK);
	}

	public static void setInput(ServerPlayer player, int keys) {
		INPUT.put(player.getUUID(), keys);
	}

	/** The wings hold the Elytra pose while they're out, wherever the jetpack is worn (LivingEntityFallFlyingMixin). */
	public static boolean keepsWings(LivingEntity entity) {
		return entity instanceof Player player && state(player).wings() && canSpreadWings(player) && !Jetpacks.worn(player).isEmpty();
	}

	/**
	 * Where the wings can open, and stay open: in the air, not in water, not riding, not in Creative flight (D28, D29),
	 * not levitating (as an Elytra), and not in lava unless it's the Netherite one (D19).
	 */
	public static boolean canSpreadWings(Player player) {
		return !player.onGround() && !player.isInWater() && !player.isPassenger() && !player.getAbilities().flying && !player.isSpectator()
			&& !player.hasEffect(MobEffects.LEVITATION) && works(player, Jetpacks.worn(player));
	}

	/** In lava only the Netherite jetpack keeps working; the plain one's thrust, hover, boost and wings cut out (D19). */
	public static boolean works(Player player, ItemStack jetpack) {
		return !player.isInLava() || jetpack.is(ModItems.NETHERITE_CLOCKWORK_JETPACK.get());
	}

	/** Whether the wearer has wind to use (Creative never uses any, D27). */
	public static boolean hasWind(Player player, ItemStack jetpack) {
		if (player.isCreative())
			return true;
		double spent = player.level().isClientSide ? 0 : SPENT.getOrDefault(player.getUUID(), 0.0);
		return Jetpacks.wind(jetpack) - spent > 0;
	}

	// ---- server

	public static void toggle(ServerPlayer player, JetpackTogglePayload.Toggle toggle) {
		if (Jetpacks.worn(player).isEmpty())
			return;
		long now = player.level().getGameTime();
		if (now - LAST_TOGGLE.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) < TOGGLE_TICKS)
			return;
		LAST_TOGGLE.put(player.getUUID(), now);
		JetpackState state = state(player);
		switch (toggle) {
			case ENGINE -> {
				state = state.withEngine(!state.engine());
				sound(player, state.engine() ? "engine_on" : "engine_off");
			}
			case HOVER -> {
				state = state.withHover(!state.hover());
				sound(player, state.hover() ? "hover_on" : "hover_off");
			}
			case WINGS -> {
				if (state.wings()) {
					state = state.withWings(false);
					player.stopFallFlying();
					sound(player, "wings_fold");
				} else if (canSpreadWings(player)) {
					state = state.withWings(true);
					player.startFallFlying();
					sound(player, "wings_unfold");
				}
			}
		}
		set(player, state);
	}

	/** Sets a player's toggles and tells them and everyone who can see them. */
	public static void set(ServerPlayer player, JetpackState state) {
		if (state.equals(state(player)))
			return;
		player.setData(ModAttachments.JETPACK, state);
		send(player);
	}

	public static void send(ServerPlayer player) {
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new JetpackStatePayload(player.getId(), state(player)));
	}

	/** On PlayerTickEvent.Post, both sides: the server spends wind and keeps the flight legal; the client moves its own player. */
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player)
			serverTick(player);
	}

	/** A wearer's server tick (public for the game tests, whose mock players don't tick on their own). */
	public static void serverTick(ServerPlayer player) {
		UUID id = player.getUUID();
		ItemStack jetpack = Jetpacks.worn(player);
		JetpackState state = state(player);
		if (jetpack.isEmpty()) {
			if (state.wings())
				set(player, state.withWings(false));
			swim(player, false);
			SPENT.remove(id);
			WAS.remove(id);
			return;
		}
		// the wings fold on landing, in water and when riding (D14, D28, D29)
		if (state.wings() && !canSpreadWings(player)) {
			state = state.withWings(false);
			set(player, state);
			sound(player, "wings_fold");
		}
		int keys = INPUT.getOrDefault(id, 0);
		boolean up = (keys & JetpackInputPayload.UP) != 0, down = (keys & JetpackInputPayload.DOWN) != 0 && !player.onGround(),
			moving = (keys & JetpackInputPayload.MOVING) != 0;
		boolean wind = hasWind(player, jetpack) && works(player, jetpack);
		// one flyer at a time (D23): a chest Elytra glides as normal, and gives way only while the jetpack is thrusting or hovering
		if (!state.wings() && state.engine() && wind && (up || down || state.hover()) && player.isFallFlying()
			&& player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)
			player.stopFallFlying();
		double use = 0;
		boolean running = false, boosting = false;
		if (state.wings()) {
			if (!player.isFallFlying())
				player.startFallFlying();
			else if (player.getFallFlyingTicks() % 10 == 0)
				player.gameEvent(GameEvent.ELYTRA_GLIDE);
			// gliding is an Elytra's and costs nothing; only the boost runs on wind, and only with the engine on
			if (state.engine() && wind && player.isFallFlying() && up) {
				boosting = running = true;
				use = Jetpacks.flightRate();
			}
		} else if (state.engine() && !player.getAbilities().flying && !player.isFallFlying() && !player.isPassenger() && wind) {
			if (up || down || (state.hover() && !player.onGround())) {
				use = up || down || moving ? 1 : Jetpacks.hoverRate();
				running = true;
				// the height limit holds on the server too: climbing past it, nothing excuses the flight
				if (!(aboveLimit(player) && player.getY() > player.yo + 1e-3)) {
					player.fallDistance = 0;
					if (player.connection instanceof ServerGamePacketListenerImplAccessor connection)
						connection.setAboveGroundTickCount(0);
				}
			}
		}
		// swimming with the engine on is faster (Create Jetpack's swim modifier); it runs on wind too
		boolean swimming = state.engine() && wind && player.isInWater() && !player.isPassenger() && !player.isSpectator();
		swim(player, swimming);
		if (swimming && moving && use == 0) {
			use = 1;
			running = true;
		}

		int was = WAS.getOrDefault(id, 0);
		if (boosting && (was & WAS_BOOSTING) == 0)
			sound(player, "boost");
		WAS.put(id, (running ? WAS_RUNNING : 0) | (boosting ? WAS_BOOSTING : 0));
		if (use > 0 && !player.isCreative())
			spend(player, jetpack, use);
	}

	/**
	 * Spends wind: whole ticks go straight onto the jetpack, only the fraction waits, so taking it off, dying or swapping
	 * it never leaves unwritten wind behind.
	 */
	private static void spend(ServerPlayer player, ItemStack jetpack, double use) {
		UUID id = player.getUUID();
		double spent = SPENT.getOrDefault(id, 0.0) + use;
		int wind = Jetpacks.wind(jetpack);
		if (spent < 1) {
			SPENT.put(id, spent);
			return;
		}
		int whole = (int) Math.min(wind, Math.floor(spent));
		float before = Jetpacks.fill(jetpack);
		Jetpacks.setWind(jetpack, wind - whole);
		SPENT.put(id, spent - whole);
		float after = Jetpacks.fill(jetpack);
		if (Jetpacks.wind(jetpack) <= 0) {
			SPENT.remove(id);
			sound(player, "run_down");
		} else if (before > 0.1f && after <= 0.1f)
			sound(player, "wind_low"); // once, as it crosses 10% (sound doc: never repeated)
	}

	/** Writes the wind spent but not yet written back to the jetpack the player wears, before it's taken off. */
	public static void flush(ServerPlayer player) {
		ItemStack jetpack = Jetpacks.worn(player);
		Double spent = SPENT.remove(player.getUUID());
		if (spent != null && !jetpack.isEmpty())
			Jetpacks.setWind(jetpack, Jetpacks.wind(jetpack) - (int) Math.ceil(spent));
	}

	/** Swimming with the engine on: Create Jetpack's swim modifier on the swim speed attribute. */
	private static void swim(Player player, boolean on) {
		AttributeInstance swim = player.getAttribute(NeoForgeMod.SWIM_SPEED);
		if (swim == null)
			return;
		if (on && !swim.hasModifier(SWIM_ID))
			swim.addTransientModifier(new AttributeModifier(SWIM_ID,
				CMConfig.Server.get(CMConfig.SERVER.jetpackSwimModifier) - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		else if (!on && swim.hasModifier(SWIM_ID))
			swim.removeModifier(SWIM_ID);
	}

	static void sound(Player player, String id) {
		SoundEvent sound = ModSounds.jetpack(id);
		if (sound != null)
			player.level().playSound(null, player.getX(), player.getY() + 1, player.getZ(), sound, SoundSource.PLAYERS, 1, 1);
	}

	/**
	 * A backtank (or Create's jetpack) put in the chest while a Clockwork Jetpack is worn comes straight off again, into
	 * the inventory (D23): the two are never worn together. Equipment changes are seen once a tick, after the click.
	 */
	public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (event.getSlot() != EquipmentSlot.CHEST || !(event.getEntity() instanceof ServerPlayer player) || !Jetpacks.isBacktank(event.getTo())
			|| Jetpacks.worn(player).isEmpty())
			return;
		ItemStack backtank = event.getTo().copy();
		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		player.getInventory().placeItemBackInInventory(backtank);
		player.displayClientMessage(Component.translatable("comedymonkeysaddons.jetpack.no_backtank"), true);
	}

	/** Whoever comes into view: a player's toggles, or an armour stand's or mob's set by command (its wings shown open). */
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (!(event.getEntity() instanceof ServerPlayer tracker))
			return;
		if (event.getTarget() instanceof ServerPlayer target)
			PacketDistributor.sendToPlayer(tracker, new JetpackStatePayload(target.getId(), state(target)));
		else if (event.getTarget() instanceof LivingEntity living && living.hasData(ModAttachments.JETPACK))
			PacketDistributor.sendToPlayer(tracker, new JetpackStatePayload(living.getId(), living.getData(ModAttachments.JETPACK)));
	}

	public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			send(player);
	}

	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			// a new body: the wings start folded
			player.setData(ModAttachments.JETPACK, state(player).withWings(false));
			send(player);
		}
	}

	public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			send(player);
	}

	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID id = event.getEntity().getUUID();
		INPUT.remove(id);
		WAS.remove(id);
		LAST_TOGGLE.remove(id);
		// what's spent but not written: write it now, so logging out can't save wind
		if (event.getEntity() instanceof ServerPlayer player) {
			ItemStack jetpack = Jetpacks.worn(player);
			Double spent = SPENT.remove(id);
			if (spent != null && !jetpack.isEmpty())
				Jetpacks.setWind(jetpack, Jetpacks.wind(jetpack) - (int) Math.ceil(spent));
		}
	}

	public static void clear() {
		INPUT.clear();
		SPENT.clear();
		WAS.clear();
		LAST_TOGGLE.clear();
	}

	// ---- the wearer's client moves them (called for the local player only)

	/**
	 * Moves the wearer for one tick, after vanilla's own movement: thrust towards the vertical speed (or, hovering with no
	 * key held, the hover descend speed: 0 holds height exactly), the movement keys pushing at the horizontal speed; or,
	 * with the wings out, the boost while Jump is held (without it, vanilla's glide is left alone). Returns whether the
	 * jetpack was working.
	 */
	public static boolean move(Player player, ItemStack jetpack, int keys, float strafe, float forward) {
		JetpackState state = state(player);
		if (!hasWind(player, jetpack) || !works(player, jetpack))
			return false;
		boolean up = (keys & JetpackInputPayload.UP) != 0, down = (keys & JetpackInputPayload.DOWN) != 0 && !player.onGround();
		if (state.wings()) {
			if (!player.isFallFlying() || !up || !state.engine())
				return false;
			boost(player);
			return true;
		}
		if (!state.engine() || player.getAbilities().flying || player.isFallFlying() || player.isPassenger())
			return false;
		boolean hovering = state.hover() && !player.onGround();
		if (!up && !down && !hovering)
			return false;
		double vertical = CMConfig.Server.get(CMConfig.SERVER.jetpackVerticalSpeed);
		double target = up ? vertical : down ? -vertical : CMConfig.Server.get(CMConfig.SERVER.jetpackHoverDescend);
		if (up && aboveLimit(player))
			target = Math.min(target, 0);
		double acceleration = CMConfig.Server.get(CMConfig.SERVER.jetpackAcceleration);
		Vec3 motion = player.getDeltaMovement();
		double vy = motion.y < target ? Math.min(target, motion.y + acceleration) : Math.max(target, motion.y - acceleration);
		player.setDeltaMovement(motion.x, vy, motion.z);
		if (strafe != 0 || forward != 0)
			player.moveRelative(1, new Vec3(strafe, 0, forward).normalize().scale(CMConfig.Server.get(CMConfig.SERVER.jetpackHorizontalSpeed)));
		player.fallDistance = 0;
		return true;
	}

	/** Boosting speed in flight mode, blocks a tick: an Elytra's shallow glide by the speed modifier and the elytra boost. */
	public static double boostSpeed() {
		return GLIDE_SPEED * CMConfig.Server.get(CMConfig.SERVER.jetpackFlightSpeedModifier) * CMConfig.Server.get(CMConfig.SERVER.jetpackElytraBoost);
	}

	/**
	 * The boost on top of vanilla's Elytra glide while Jump is held (D16): the fans push along the look up to boosting
	 * speed; looking level holds height, looking up climbs, looking down dives as an Elytra does.
	 */
	private static void boost(Player player) {
		Vec3 look = player.getLookAngle();
		Vec3 v = player.getDeltaMovement();
		double cruise = boostSpeed();
		if (v.length() < cruise)
			v = v.add(look.scale(FLIGHT_THRUST));
		if (Math.abs(look.y) < 0.2)
			v = new Vec3(v.x, Mth.lerp(0.5, v.y, look.y * cruise), v.z); // level: hold height
		else if (look.y > 0)
			v = new Vec3(v.x, Math.max(v.y, look.y * cruise), v.z); // up: a powered climb
		player.setDeltaMovement(v);
	}

	/** Create Jetpack's heightAboveGroundLimit: no thrust upwards once that high over the ground (-1: no limit). */
	private static boolean aboveLimit(Player player) {
		int limit = CMConfig.Server.get(CMConfig.SERVER.jetpackHeightAboveGroundLimit);
		if (limit < 0)
			return false;
		BlockPos pos = player.blockPosition();
		for (int d = 0; d <= limit; d++)
			if (!player.level().getBlockState(pos.below(d)).isAir())
				return false;
		return true;
	}
}
