package com.comedymonkey.createaddons.client.jetpack;

import java.time.LocalDate;
import java.time.Month;

import org.lwjgl.glfw.GLFW;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.jetpack.JetpackFlight;
import com.comedymonkey.createaddons.content.jetpack.JetpackState;
import com.comedymonkey.createaddons.content.jetpack.Jetpacks;
import com.comedymonkey.createaddons.network.JetpackInputPayload;
import com.comedymonkey.createaddons.network.JetpackTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.foundation.particle.AirParticleData;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Clockwork Jetpack on the client (jetpack spec D8, D14, D24): its keys (G engine, H hover, J wings, rebindable), the
 * controls it tells the server about, moving its own wearer (the server spends the wind), the thrust (Create's air
 * swirls under the fans, dust off the ground near it, snowflakes in December and embers at Halloween), and the wind
 * gauge, placed and scaled by the client config.
 */
public final class JetpackClient {
	private static final String CATEGORY = "key.categories." + ComedyMonkeysCreateAddons.MOD_ID + ".jetpack";
	public static final KeyMapping ENGINE = key("jetpack_engine", GLFW.GLFW_KEY_G);
	public static final KeyMapping HOVER = key("jetpack_hover", GLFW.GLFW_KEY_H);
	public static final KeyMapping WINGS = key("jetpack_wings", GLFW.GLFW_KEY_J);

	private static int sentKeys = -1, sentAge;
	/** Double-tapping Jump: whether it was down last tick, and when it last went down (client ticks). */
	private static boolean jumpWasDown;
	/** When the current press of Jump began, and when the last short tap of it was let go (client ticks; -1 none). */
	private static long pressStart = -1, tapReleased = -1;
	/** A tap of Jump (held no longer than TAP_TICKS, so not thrust or boost), then a second press within this many ticks
	 * of letting go, toggles flight mode. */
	private static final int DOUBLE_TAP_TICKS = 8, TAP_TICKS = 10;

	private JetpackClient() {}

	private static KeyMapping key(String name, int key) {
		return new KeyMapping("key." + ComedyMonkeysCreateAddons.MOD_ID + "." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, CATEGORY);
	}

	public static void registerKeys(RegisterKeyMappingsEvent event) {
		event.register(ENGINE);
		event.register(HOVER);
		event.register(WINGS);
	}

	public static void registerGui(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.HOTBAR, ComedyMonkeysCreateAddons.asResource("clockwork_jetpack"), (graphics, delta) -> drawGauge(graphics));
	}

	/** Each client tick: the keys, the controls held, and the thrust. */
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null)
			return;
		ItemStack jetpack = Jetpacks.worn(player);
		while (ENGINE.consumeClick())
			if (!jetpack.isEmpty())
				PacketDistributor.sendToServer(new JetpackTogglePayload(JetpackTogglePayload.Toggle.ENGINE));
		while (HOVER.consumeClick())
			if (!jetpack.isEmpty())
				PacketDistributor.sendToServer(new JetpackTogglePayload(JetpackTogglePayload.Toggle.HOVER));
		while (WINGS.consumeClick())
			if (!jetpack.isEmpty())
				PacketDistributor.sendToServer(new JetpackTogglePayload(JetpackTogglePayload.Toggle.WINGS));
		doubleTap(player, jetpack);

		int keys = jetpack.isEmpty() ? 0 : keys(player);
		// on change, and again now and then, in case one was lost
		if (keys != sentKeys || (keys != 0 && ++sentAge > 40)) {
			PacketDistributor.sendToServer(new JetpackInputPayload(keys));
			sentKeys = keys;
			sentAge = 0;
		}
		if (!mc.isPaused())
			for (Player wearer : mc.level.players())
				if (wearer.distanceToSqr(player) < 48 * 48)
					thrust(wearer);
	}

	/**
	 * Double-tapping Jump toggles flight mode, as J does: on the ground (the first tap jumps), mid-jump or hovering, the
	 * wings spread (the server checks they can); in flight they fold. Holding Jump to thrust or boost is never a tap. Not
	 * where a double tap already means something: Creative's (or any) flight you're allowed.
	 */
	private static void doubleTap(LocalPlayer player, ItemStack jetpack) {
		boolean down = Minecraft.getInstance().options.keyJump.isDown();
		boolean pressed = down && !jumpWasDown, released = !down && jumpWasDown;
		jumpWasDown = down;
		long now = player.tickCount;
		if (released) {
			// a short tap: the second one is timed from letting go (the first press often lifts you for a few ticks)
			tapReleased = pressStart >= 0 && now >= pressStart && now - pressStart <= TAP_TICKS ? now : -1;
			return;
		}
		if (!pressed)
			return;
		pressStart = now;
		boolean twice = tapReleased >= 0 && now >= tapReleased && now - tapReleased <= DOUBLE_TAP_TICKS;
		tapReleased = -1;
		if (!twice || jetpack.isEmpty() || !CMConfig.CLIENT.jetpackDoubleTapWings.get() || player.mayFly())
			return;
		boolean open = player.getData(com.comedymonkey.createaddons.registry.ModAttachments.JETPACK).wings();
		if (!open && !JetpackFlight.canSpreadWings(player))
			return;
		pressStart = -1; // the tap that toggled doesn't start another double tap
		PacketDistributor.sendToServer(new JetpackTogglePayload(JetpackTogglePayload.Toggle.WINGS));
	}

	private static int keys(LocalPlayer player) {
		Minecraft mc = Minecraft.getInstance();
		int keys = 0;
		if (mc.options.keyJump.isDown())
			keys |= JetpackInputPayload.UP;
		if (mc.options.keyShift.isDown())
			keys |= JetpackInputPayload.DOWN;
		if (player.input.leftImpulse != 0 || player.input.forwardImpulse != 0)
			keys |= JetpackInputPayload.MOVING;
		return keys;
	}

	/** After vanilla's movement each tick: the local wearer's thrust, hover or flight. */
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof LocalPlayer player) || player != Minecraft.getInstance().player)
			return;
		ItemStack jetpack = Jetpacks.worn(player);
		if (!jetpack.isEmpty())
			JetpackFlight.move(player, jetpack, keys(player), player.xxa, player.zza);
	}

	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		JetpackWorn.clear();
		sentKeys = -1;
		pressStart = tapReleased = -1;
		jumpWasDown = false;
	}

	/** The thrust under a wearer's fans while it lifts them: Create's air swirls, and dust off the ground below. */
	private static void thrust(Player wearer) {
		ItemStack jetpack = Jetpacks.worn(wearer);
		if (jetpack.isEmpty() || wearer.onGround() || wearer.isPassenger() || wearer.isSpectator() || Jetpacks.wind(jetpack) <= 0)
			return;
		JetpackState state = wearer.getData(com.comedymonkey.createaddons.registry.ModAttachments.JETPACK);
		boolean flying = state.wings() && wearer.isFallFlying();
		if (!state.engine() || !JetpackFlight.works(wearer, jetpack) || (!flying && (wearer.isFallFlying() || wearer.getAbilities().flying)))
			return;
		// winged, only the boost blows: the wearer's own client knows Jump; others are boosting if they hold its speed and height
		if (flying && (wearer == Minecraft.getInstance().player ? !Minecraft.getInstance().options.keyJump.isDown()
			: wearer.getDeltaMovement().length() < 0.9 * JetpackFlight.boostSpeed() || wearer.getDeltaMovement().y < -0.1))
			return;
		// the wearer's own client knows its controls; for others, rising or holding still in the air means it's working
		if (!flying && wearer != Minecraft.getInstance().player && wearer.getDeltaMovement().y < -0.15)
			return;
		if (!flying && wearer == Minecraft.getInstance().player && keys((LocalPlayer) wearer) == 0 && !state.hover())
			return;
		float yaw = wearer.yBodyRot * Mth.DEG_TO_RAD;
		Vec3 behind = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw)), right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
		double height = wearer.isCrouching() ? 0.55 : 0.75;
		ParticleOptions particle = seasonal();
		for (int side = -1; side <= 1; side += 2) {
			Vec3 mouth = wearer.position().add(behind.scale(0.58)).add(right.scale(side * 2.5 / 16)).add(0, height, 0);
			wearer.level().addParticle(particle, mouth.x, mouth.y, mouth.z, 0, -0.15, 0);
		}
		if (wearer.tickCount % 4 == 0)
			for (int d = 1; d <= 3; d++) {
				BlockPos below = wearer.blockPosition().below(d);
				BlockState ground = wearer.level().getBlockState(below);
				if (!ground.isAir()) {
					wearer.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), wearer.getX() + (wearer.getRandom().nextDouble() - 0.5),
						below.getY() + 1.05, wearer.getZ() + (wearer.getRandom().nextDouble() - 0.5), 0, 0.05, 0);
					break;
				}
			}
	}

	private static ParticleOptions seasonal() {
		if (CMConfig.CLIENT_SPEC.isLoaded() && CMConfig.CLIENT.jetpackSeasonalEffects.get()) {
			LocalDate today = LocalDate.now();
			if (today.getMonth() == Month.DECEMBER)
				return ParticleTypes.SNOWFLAKE;
			if ((today.getMonth() == Month.OCTOBER && today.getDayOfMonth() >= 24) || (today.getMonth() == Month.NOVEMBER && today.getDayOfMonth() == 1))
				return ParticleTypes.SMALL_FLAME;
		}
		return new AirParticleData(1, 0.05f);
	}

	/** The wind gauge: the jetpack, a bar of its wind, and what's switched on. */
	private static void drawGauge(GuiGraphics graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || !CMConfig.CLIENT_SPEC.isLoaded() || !CMConfig.CLIENT.jetpackOverlay.get())
			return;
		ItemStack jetpack = Jetpacks.worn(mc.player);
		if (jetpack.isEmpty())
			return;
		JetpackState state = mc.player.getData(com.comedymonkey.createaddons.registry.ModAttachments.JETPACK);
		float scale = (float) (double) CMConfig.CLIENT.jetpackOverlayScale.get();
		int width = 64, height = 20;
		int x = CMConfig.CLIENT.jetpackOverlayX.get(), y = CMConfig.CLIENT.jetpackOverlayY.get();
		float px = x >= 0 ? x : graphics.guiWidth() + x - width * scale, py = y >= 0 ? y : graphics.guiHeight() + y - height * scale;
		graphics.pose().pushPose();
		graphics.pose().translate(px, py, 0);
		graphics.pose().scale(scale, scale, 1);
		graphics.renderItem(jetpack, 0, 0);
		int fill = Math.round(44 * Jetpacks.fill(jetpack));
		graphics.fill(18, 5, 62, 9, 0xFF1C1E21);
		graphics.fill(18, 5, 18 + fill, 9, Jetpacks.fill(jetpack) <= 0.1f ? 0xFFA3712A : 0xFF8A949C);
		String key = !state.engine() ? "engine_off" : state.wings() ? "wings" : state.hover() ? "hover" : null;
		if (key != null)
			graphics.drawString(mc.font, Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".jetpack.hud." + key), 18, 11, 0xFFB0B0A8, true);
		graphics.pose().popPose();
	}
}
