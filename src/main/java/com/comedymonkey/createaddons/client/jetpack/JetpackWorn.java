package com.comedymonkey.createaddons.client.jetpack;

import java.util.HashMap;
import java.util.Map;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.compat.curios.JetpackCurios;
import com.comedymonkey.createaddons.content.jetpack.JetpackState;
import com.comedymonkey.createaddons.content.jetpack.JetpackWindingStationBlockEntity;
import com.comedymonkey.createaddons.content.jetpack.Jetpacks;
import com.comedymonkey.createaddons.registry.ModAttachments;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws a Clockwork Jetpack on its wearer's back (jetpack brief): from the Curios Jetpack slot (JetpackCurioRenderer)
 * or the chest (JetpackLayer). Its plate sits a quarter pixel off a chestplate (or the shirt), its harness hugging it;
 * with a backpack on (Curios' back slot, or a backpack worn in the chest), it's built into the pack (pack mode).
 * The wings unfold and fold from the wearer's synced toggle, the key ticks round while it runs, and while a Winding
 * Station winds it the key turns with the station's socket.
 */
public final class JetpackWorn {
	private static final class Anim {
		float open, lastOpen, tuck, lastTuck;
		/** The key's turn in 30-degree ticks, and the fans' in degrees, wound on while it runs and slowing in the run-down. */
		float keyTicks, lastKeyTicks, fans, lastFans;
		/** Ticks since the spring ran out mid-air (the run-down clip), or -1. */
		int runDown = -1;
		boolean wasRunning;
		long tick = -1;
	}

	/** While it runs, the key ticks this many times a tick (6 a second) and the fans turn this many degrees a tick. */
	private static final float KEY_RATE = 6 / 20f, FAN_RATE = 72;
	/** The run-down: the key and fans slow to a stop over 1.5 s; the pawl drops just after (run_down clip). */
	private static final int RUN_DOWN = 30, PAWL_DROP = 32;
	/** Below this much wind the key ticks at half speed, so anyone can see it's nearly out (D17). */
	private static final float LOW = 0.1f;

	private static final Map<Integer, Anim> ANIMS = new HashMap<>();

	private JetpackWorn() {}

	public static void render(LivingEntity entity, ItemStack jetpack, HumanoidModel<?> model, PoseStack ms, MultiBufferSource buffers, int light,
							  float partialTicks) {
		JetpackState state = entity.getData(ModAttachments.JETPACK);
		boolean pack = wearsBackpack(entity);
		boolean running = state.engine() && (!entity.onGround() || entity.isFallFlying()) && Jetpacks.wind(jetpack) > 0;
		Anim anim = advance(entity, state, pack, running, Jetpacks.wind(jetpack) <= 0, Jetpacks.fill(jetpack) < LOW);
		float open = Mth.lerp(partialTicks, anim.lastOpen, anim.open), tuck = Mth.lerp(partialTicks, anim.lastTuck, anim.tuck);
		// each tick of the key a quick flick, then a rest
		float ticks = Mth.lerp(partialTicks, anim.lastKeyTicks, anim.keyTicks);
		float key = -30 * (Mth.floor(ticks) + smooth(Math.min(1, Mth.frac(ticks) * 3)));
		float pawl = anim.runDown >= PAWL_DROP ? -10 * Math.min(1, (anim.runDown - PAWL_DROP + partialTicks) / 3) : 0;
		Float wound = windingKey(entity);
		if (wound != null) {
			key = wound;
			pawl = pawlClick(wound);
		}
		float fans = Mth.lerp(partialTicks, anim.lastFans, anim.fans);
		boolean chestplate = entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem && !Jetpacks.isJetpack(entity.getItemBySlot(EquipmentSlot.CHEST));

		ms.pushPose();
		model.body.translateAndRotate(ms);
		// the entity model's space is upside down and mirrored; back to the jetpack's own block space, its plate on the back
		ms.scale(-1, -1, 1);
		ms.translate(-8 / 16f, -16 / 16f, ((chestplate ? 3.25f : 2.25f) - 4) / 16f);
		JetpackModel.Variant variant = jetpack.is(ModItems.NETHERITE_CLOCKWORK_JETPACK.get()) ? JetpackModel.Variant.NETHERITE : JetpackModel.Variant.CLOCKWORK;
		JetpackModel.draw(ms, sink(buffers), light, Sheets.cutoutBlockSheet(), variant,
			new JetpackModel.Pose(Jetpacks.fill(jetpack), key, pawl, fans, fans / 3, open, tuck, chestplate ? JetpackModel.Harness.CHESTPLATE : JetpackModel.Harness.SHIRT, pack));
		ms.popPose();
	}

	/** Part models drawn on an entity: the block atlas, lit as an entity is. */
	static PartSink sink(MultiBufferSource buffers) {
		return (model, ms, light, type, rgb) -> Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(),
			buffers.getBuffer(type), null, Minecraft.getInstance().getModelManager().getModel(model), 1, 1, 1, light, OverlayTexture.NO_OVERLAY,
			ModelData.EMPTY, RenderType.cutout());
	}

	/**
	 * Once a tick per wearer: the wings toward their toggle, open over a second, then the drum tucks in (0.3 s); folding,
	 * the drum comes out first (0.2 s), then the wings fold (0.6 s). Built into a backpack, the drum stays put.
	 */
	private static Anim advance(LivingEntity entity, JetpackState state, boolean pack, boolean running, boolean empty, boolean low) {
		Anim anim = ANIMS.computeIfAbsent(entity.getId(), id -> new Anim());
		long now = entity.level().getGameTime();
		if (anim.tick == now)
			return anim;
		// first seen, or not drawn for a while (out of view, another level, a reused id): snap to the toggle; otherwise
		// catch up every tick missed (low frame rates), at most a second's worth
		boolean first = anim.tick < 0 || now - anim.tick > 20 || now < anim.tick;
		long steps = first ? 1 : now - anim.tick;
		anim.tick = now;
		for (long i = 0; i < steps; i++) {
			step(anim, state, pack);
			spin(anim, running, empty, low);
		}
		if (first) {
			anim.open = anim.lastOpen = state.wings() ? 1 : 0;
			anim.tuck = anim.lastTuck = state.wings() && !pack ? 1 : 0;
		}
		return anim;
	}

	/**
	 * The key and fans: wound on while it runs, the key at half speed when the wind's low; when the spring runs out
	 * mid-run, they slow to a stop (run_down clip).
	 */
	private static void spin(Anim anim, boolean running, boolean empty, boolean low) {
		anim.lastKeyTicks = anim.keyTicks;
		anim.lastFans = anim.fans;
		if (running)
			anim.runDown = -1;
		else if (anim.wasRunning && empty)
			anim.runDown = 0;
		anim.wasRunning = running;
		float speed = running ? 1 : anim.runDown >= 0 && anim.runDown < RUN_DOWN ? 1 - smooth(anim.runDown / (float) RUN_DOWN) : 0;
		anim.keyTicks += KEY_RATE * speed * (low && running ? 0.5f : 1);
		anim.fans += FAN_RATE * speed;
		if (anim.runDown >= 0 && anim.runDown < PAWL_DROP + 3)
			anim.runDown++;
		// keep the numbers small; whole turns look the same
		if (anim.keyTicks >= 12 && anim.lastKeyTicks >= 12) {
			anim.keyTicks -= 12;
			anim.lastKeyTicks -= 12;
		}
		if (anim.fans >= 1080 && anim.lastFans >= 1080) {
			anim.fans -= 1080;
			anim.lastFans -= 1080;
		}
	}

	/** While a station winds it, the pawl clicks over each of the key's 30-degree teeth. */
	static float pawlClick(float keyDegrees) {
		return 10 * Math.max(0, 1 - Mth.frac(Math.abs(keyDegrees) / 30) * 4);
	}

	private static float smooth(float t) {
		return t * t * (3 - 2 * t);
	}

	private static void step(Anim anim, JetpackState state, boolean pack) {
		anim.lastOpen = anim.open;
		anim.lastTuck = anim.tuck;
		if (state.wings()) {
			anim.open = Math.min(1, anim.open + 1 / 20f);
			anim.tuck = anim.open >= 1 && !pack ? Math.min(1, anim.tuck + 1 / 6f) : Math.max(0, anim.tuck - 1 / 4f);
		} else {
			anim.tuck = Math.max(0, anim.tuck - 1 / 4f);
			if (anim.lastTuck <= 0)
				anim.open = Math.max(0, anim.open - 1 / 12f);
		}
	}

	/** A backpack on too: something in Curios' back slot, or a backpack (Sophisticated Backpacks' and the like) in the chest. */
	private static boolean wearsBackpack(LivingEntity entity) {
		if (BuiltInRegistries.ITEM.getKey(entity.getItemBySlot(EquipmentSlot.CHEST).getItem()).getPath().endsWith("backpack"))
			return true;
		return ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID) && JetpackCurios.wearsOnBack(entity);
	}

	/** The key's turn while a Winding Station winds this wearer's jetpack, or null if none is. */
	private static Float windingKey(LivingEntity entity) {
		BlockPos pos = JetpackWindingStationBlockEntity.WINDING.get(entity.getId());
		if (pos == null)
			return null;
		if (entity.level().getBlockEntity(pos) instanceof JetpackWindingStationBlockEntity be && be.boom() == JetpackWindingStationBlockEntity.Boom.WORN
			&& be.wearerId() == entity.getId())
			return JetpackWindingStationRenderer.socketDegrees(be);
		JetpackWindingStationBlockEntity.WINDING.remove(entity.getId());
		return null;
	}

	/** On leaving a level: forget everyone's animations. */
	public static void clear() {
		ANIMS.clear();
		JetpackWindingStationBlockEntity.WINDING.clear();
	}
}
