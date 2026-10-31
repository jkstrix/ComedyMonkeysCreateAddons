package com.comedymonkey.createaddons.client.wastes;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.MachineVisuals;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlock;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlockEntity;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlock;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictLightBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictStarReactorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictWallLampBlock;
import com.comedymonkey.createaddons.content.wastes.SlidingDoorBlock;
import com.comedymonkey.createaddons.content.wastes.SlidingDoorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.StarReactorBlock;
import com.comedymonkey.createaddons.content.wastes.StarReactorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.SubstationBlock;
import com.comedymonkey.createaddons.content.wastes.SubstationBlockEntity;
import com.comedymonkey.createaddons.content.wastes.WallVentBlock;
import com.comedymonkey.createaddons.content.wastes.WallVentBlockEntity;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The Clockwork Wastes' powered city blocks, drawn (design/briefs/clockwork-wastes-blocks.md, Rig Requirements; motion
 * from the approved clips in clockwork-wastes-city.gen.js and clockwork-wastes-reactors.gen.js). Each block's moving
 * solid parts and opaque glows are posed in one {@code parts} method, which Flywheel draws as instances (PartsVisual)
 * or the renderer draws itself; glows go full-bright, tinted down when dim. Translucent glass glows and the Nether Stars
 * stay with the renderers. Part models are exported by design/models/clockwork-wastes.export.js, built in their block's
 * model space facing north (some shifted down a block or two to stay within a block model's bounds; noted where drawn).
 */
public final class WastesRenderers {
	private WastesRenderers() {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/wastes/" + name));
	}

	// Substation
	static final ModelResourceLocation SUB_LEVER = model("substation_lever"), SUB_SEAMS = model("substation_seams"),
		SUB_SIGIL = model("substation_sigil"), SUB_FLASH = model("substation_flash"), SUB_COLUMN = model("substation_column");
	// City Vault
	static final ModelResourceLocation VAULT_BLADE = model("city_vault_blade"), VAULT_SEAM = model("city_vault_blade_seam"),
		VAULT_PIP = model("city_vault_pip");
	// Sliding Door (shifted down 16 px)
	static final ModelResourceLocation DOOR_LEAF_L = model("sliding_door_leaf_l"), DOOR_LEAF_R = model("sliding_door_leaf_r"),
		DOOR_SEAM = model("sliding_door_seam"), DOOR_CHAIN = model("sliding_door_chain"), DOOR_JAMB = model("sliding_door_jamb_glow");
	// lamps
	static final ModelResourceLocation STREET_GLOW = model("derelict_street_lamp_glow"), WALL_GLOW = model("derelict_wall_lamp_glow"),
		STREET_GLOW_LEANING = model("derelict_street_lamp_glow_leaning");
	// Conduit Pylon (crown, fins and cores in the top block's coords; seams in the middle's)
	static final ModelResourceLocation PYLON_CROWN = model("conduit_pylon_crown"), PYLON_FIN = model("conduit_pylon_fin"),
		PYLON_STUB = model("conduit_pylon_fin_stub"), PYLON_CORE = model("conduit_pylon_core"), PYLON_CORE_CRACKED = model("conduit_pylon_core_cracked"),
		PYLON_CORE_DARK = model("conduit_pylon_core_dark"), PYLON_CORE_BLAZE = model("conduit_pylon_core_blaze"),
		PYLON_SEAMS = model("conduit_pylon_seams"), PYLON_GROOVE = model("conduit_pylon_groove_glow");
	// Derelict Star Reactor (arms, their seams and the core shifted down 16 px)
	static final ModelResourceLocation DSR_ARM = model("derelict_star_reactor_arm"), DSR_ARM_SEAM = model("derelict_star_reactor_arm_seam"),
		DSR_CORE = model("derelict_star_reactor_core"), DSR_GROOVES = model("derelict_star_reactor_grooves_glow");
	// Star Reactor
	static final ModelResourceLocation SR_ORBIT = model("star_reactor_orbit"), SR_PIP = model("star_reactor_pip"),
		SR_PORTS = model("star_reactor_ports_lit");
	// Wall vent (the hatch an OBJ: its louvres are turned 35 degrees)
	static final ModelResourceLocation VENT_HATCH = model("wall_vent_hatch"), VENT_SEAM = model("wall_vent_seam"),
		VENT_FLASH = model("wall_vent_flash"), VENT_CONTACT = model("wall_vent_contact"), VENT_GLINT = model("wall_vent_glint");

	/** Dim starlight: a woken district's glows while it's unfed. */
	static final int DIM = 0x737373;
	private static final ItemStack STAR = new ItemStack(Items.NETHER_STAR);

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {SUB_LEVER, SUB_SEAMS, SUB_SIGIL, SUB_FLASH, SUB_COLUMN, VAULT_BLADE, VAULT_SEAM,
			VAULT_PIP, DOOR_LEAF_L, DOOR_LEAF_R, DOOR_SEAM, DOOR_CHAIN, DOOR_JAMB, STREET_GLOW, STREET_GLOW_LEANING, WALL_GLOW, PYLON_CROWN, PYLON_FIN, PYLON_STUB,
			PYLON_CORE, PYLON_CORE_CRACKED, PYLON_CORE_DARK, PYLON_CORE_BLAZE, PYLON_SEAMS, PYLON_GROOVE, DSR_ARM, DSR_ARM_SEAM, DSR_CORE,
			DSR_GROOVES, SR_ORBIT, SR_PIP, SR_PORTS, VENT_HATCH, VENT_SEAM, VENT_FLASH, VENT_CONTACT, VENT_GLINT})
			event.register(m);
	}

	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModWastes.SUBSTATION_BE.get(), c -> new Parts<>(WastesRenderers::substation));
		event.registerBlockEntityRenderer(ModWastes.CITY_VAULT_BE.get(), c -> new Parts<>(WastesRenderers::vault));
		event.registerBlockEntityRenderer(ModWastes.SLIDING_DOOR_BE.get(), c -> new Parts<>(WastesRenderers::door));
		event.registerBlockEntityRenderer(ModWastes.DERELICT_LIGHT_BE.get(), c -> new Parts<>(WastesRenderers::lamp));
		event.registerBlockEntityRenderer(ModWastes.CONDUIT_PYLON_BE.get(), c -> new Parts<>(WastesRenderers::pylon));
		event.registerBlockEntityRenderer(ModWastes.DERELICT_STAR_REACTOR_BE.get(), c -> new DerelictReactorRenderer());
		event.registerBlockEntityRenderer(ModWastes.STAR_REACTOR_BE.get(), c -> new StarReactorRenderer());
		event.registerBlockEntityRenderer(ModWastes.WALL_VENT_BE.get(), c -> new Parts<>(WastesRenderers::vent));
		// the sky-cables (D61, D64): spans, the lit line and the wake bead
		event.registerBlockEntityRenderer(ModWastes.DERELICT_INSULATOR_BE.get(), SkyCableRenderer::new);
	}

	/** At client setup: Flywheel draws the parts (the reactors' renderers still draw their stars and glass). */
	public static void registerVisuals() {
		MachineVisuals.visual(ModWastes.SUBSTATION_BE, WastesRenderers::substation, true);
		MachineVisuals.visual(ModWastes.CITY_VAULT_BE, WastesRenderers::vault, true);
		MachineVisuals.visual(ModWastes.SLIDING_DOOR_BE, WastesRenderers::door, true);
		// a lamp's lens only while it's lit (dark, its glass is in the block model); a cable's line is in its model
		MachineVisuals.visualWhen(ModWastes.DERELICT_LIGHT_BE, WastesRenderers::lamp, true, WastesRenderers::lampActive);
		MachineVisuals.visual(ModWastes.CONDUIT_PYLON_BE, WastesRenderers::pylon, true);
		MachineVisuals.visual(ModWastes.DERELICT_STAR_REACTOR_BE, WastesRenderers::derelictReactor, false);
		MachineVisuals.visual(ModWastes.STAR_REACTOR_BE, WastesRenderers::starReactor, false);
		MachineVisuals.visual(ModWastes.WALL_VENT_BE, WastesRenderers::vent, true);
	}

	/** A renderer that draws nothing but its parts (Flywheel draws them instead whenever it can). */
	static final class Parts<T extends BlockEntity> extends SafeBlockEntityRenderer<T> {
		private final com.comedymonkey.createaddons.client.parts.PartsVisual.Parts<T> parts;

		Parts(com.comedymonkey.createaddons.client.parts.PartsVisual.Parts<T> parts) {
			this.parts = parts;
		}

		@Override
		protected void renderSafe(T be, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
			parts.draw(be, pt, ms, PartSink.forRenderer(be, buffer, overlay), light);
		}

		@Override
		public boolean shouldRenderOffScreen(T be) {
			return true;
		}

		@Override
		public AABB getRenderBoundingBox(T be) {
			return new AABB(be.getBlockPos()).inflate(2).expandTowards(0, 2, 0);
		}

		@Override
		public int getViewDistance() {
			return 128;
		}
	}

	// ---------------------------------------------------------------- helpers

	/** Seconds since a game time, with the part tick (worked out whole first: game time is too large for a float). */
	static float since(BlockEntity be, long at, float pt) {
		return be.getLevel() == null ? 1e6f : ((be.getLevel().getGameTime() - at) + pt) / 20f;
	}

	static float seconds(BlockEntity be, float pt) {
		return be.getLevel() == null ? 0 : ((be.getLevel().getGameTime() % 24_000L) + pt) / 20f;
	}

	/** Turned to a horizontal facing about the block's middle, as the blockstates turn the models (north: as built). */
	static void face(PoseStack ms, Direction facing) {
		float y = switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
		ms.translate(0.5, 0, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-y));
		ms.translate(-0.5, 0, -0.5);
	}

	/** Turned about the axis through a point (in pixels). */
	static void turn(PoseStack ms, com.mojang.math.Axis axis, float degrees, float px, float py, float pz) {
		ms.translate(px / 16f, py / 16f, pz / 16f);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-px / 16f, -py / 16f, -pz / 16f);
	}

	static void px(PoseStack ms, float x, float y, float z) {
		ms.translate(x / 16f, y / 16f, z / 16f);
	}

	static float smooth(float t, float t0, float t1) {
		if (t <= t0)
			return 0;
		if (t >= t1)
			return 1;
		float u = (t - t0) / (t1 - t0);
		return u * u * (3 - 2 * u);
	}

	/** A damped jolt to 1 (the clips' jolt): there by d seconds, overshooting and settling. */
	static float jolt(float t, float d) {
		if (t <= 0)
			return 0;
		float u = t / d;
		return u < 1 ? u * u : 1 + 0.18f * (float) Math.exp(-(u - 1) * 1.4f) * Mth.sin((u - 1) * 2.6f);
	}

	static int grey(float f) {
		int c = Mth.clamp((int) (f * 255), 0, 255);
		return c << 16 | c << 8 | c;
	}

	/** A flicker's frame is on (the Electric Lights' flicker: fewer frames on as it goes out). */
	static boolean flickerOn(BlockPos pos, float ticks, boolean comingOn) {
		if (ticks < 0 || ticks >= 20)
			return comingOn;
		int frame = (int) (ticks / 1.8f);
		float chance = comingOn ? 0.35f + frame * 0.06f : 0.65f - frame * 0.06f;
		long h = pos.asLong() * 0x9E3779B97F4A7C15L + frame * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h & 0xFFFFFF) / (float) 0x1000000 < chance;
	}

	// ---------------------------------------------------------------- Substation

	static final float LEVER_OPEN = -130;

	/** The slam (the clip's): down in 0.12 s, a hard bounce off the jaw and a short settle; 0 open, 1 shut. */
	static float slam(float t) {
		if (t <= 0)
			return 0;
		float u = t / 0.12f;
		return u < 1 ? u * u : 1 - 0.12f * (float) Math.exp(-(u - 1) * 1.6f) * Math.abs(Mth.sin((u - 1) * 2.2f));
	}

	public static void substation(SubstationBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		if (state.getValue(SubstationBlock.HALF) != DoubleBlockHalf.LOWER)
			return;
		ms.pushPose();
		face(ms, state.getValue(SubstationBlock.FACING));
		boolean woken = be.isWoken();
		float t = woken && be.wokeAt() != SubstationBlockEntity.NEVER ? since(be, be.wokeAt(), pt) : 1e6f;
		// the knife switch: thrown open while dormant; it slams shut by itself as the district wakes
		ms.pushPose();
		turn(ms, Axis.XP, woken ? LEVER_OPEN * (1 - slam(t)) : LEVER_OPEN, 16, 20, 8);
		sink.part(SUB_LEVER, ms, light);
		ms.popPose();
		if (t >= 0.08f && t <= 0.3f)
			sink.part(SUB_FLASH, ms, LightTexture.FULL_BRIGHT);
		// the column rises with the charge in ten steps; woken and unfed it's dim, the sigil's lens dark
		int fill = be.fill();
		boolean bright = !woken || be.isFed();
		if (fill > 0) {
			ms.pushPose();
			px(ms, 0, 3.5f, 0);
			ms.scale(1, fill / (float) SubstationBlockEntity.STEPS, 1);
			px(ms, 0, -3.5f, 0);
			sink.part(SUB_COLUMN, ms, LightTexture.FULL_BRIGHT, RenderType.cutout(), bright ? 0xFFFFFF : DIM);
			ms.popPose();
		}
		if (woken && t >= 0.1f) {
			sink.part(SUB_SEAMS, ms, LightTexture.FULL_BRIGHT, RenderType.cutout(), bright ? 0xFFFFFF : DIM);
			if (be.isFed())
				sink.part(SUB_SIGIL, ms, LightTexture.FULL_BRIGHT);
		}
		ms.popPose();
	}

	// ---------------------------------------------------------------- City Vault

	public static void vault(CityVaultBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		boolean locked = state.getValue(CityVaultBlock.LOCKED);
		float t = locked ? -1 : be.openedAt() == SubstationBlockEntity.NEVER ? 1e6f : since(be, be.openedAt(), pt);
		// the light reaches the edges before the blades move: seams spiral on (0.5 s), then the iris turns open (1 s)
		float open = locked ? 0 : smooth(t, 0.5f, 1.5f);
		ms.pushPose();
		face(ms, state.getValue(CityVaultBlock.FACING));
		for (int k = 0; k < 6; k++) {
			ms.pushPose();
			px(ms, 0, 0, (5 - k) * 0.75f);
			turn(ms, Axis.ZP, 60 * k, 8, 8, 0);
			// each blade swings out about its hinge on the rim and folds toward it
			px(ms, 14, 8, 0);
			ms.mulPose(Axis.ZP.rotationDegrees(-40 * open));
			ms.scale(1 - 0.65f * open, 1 - 0.4f * open, 1);
			px(ms, -14, -8, 0);
			sink.part(VAULT_BLADE, ms, light);
			if (!locked && t >= k * 0.08f)
				sink.part(VAULT_SEAM, ms, LightTexture.FULL_BRIGHT);
			ms.popPose();
		}
		// locked, one pale pixel at the iris centre pulses slowly: it's waiting
		if (locked) {
			float pulse = 0.35f + 0.65f * (0.5f + 0.5f * Mth.sin(seconds(be, pt) * Mth.TWO_PI / 4));
			sink.part(VAULT_PIP, ms, LightTexture.FULL_BRIGHT, RenderType.cutout(), grey(pulse));
		} else if (t < 0.55f)
			sink.part(VAULT_PIP, ms, LightTexture.FULL_BRIGHT);
		ms.popPose();
	}

	// ---------------------------------------------------------------- Sliding Door

	public static void door(SlidingDoorBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		boolean open = state.getValue(SlidingDoorBlock.OPEN);
		float t = !open ? -1 : be.openedAt() == SubstationBlockEntity.NEVER ? 1e6f : since(be, be.openedAt(), pt);
		ms.pushPose();
		face(ms, state.getValue(SlidingDoorBlock.FACING));
		// the parts are built a block down: up into place
		ms.translate(0, 1, 0);
		float from = SlidingDoorBlockEntity.SLIDE_FROM / 20f, to = SlidingDoorBlockEntity.SLIDE_TO / 20f;
		if (t < to) {
			// shut, or sliding into the walls (then they're in the walls, out of sight)
			float slide = 16 * smooth(t, from, to);
			ms.pushPose();
			px(ms, -slide, 0, 0);
			sink.part(DOOR_LEAF_L, ms, light);
			ms.popPose();
			ms.pushPose();
			px(ms, slide, 0, 0);
			sink.part(DOOR_LEAF_R, ms, light);
			ms.popPose();
		}
		// the seam and the broken chain light before the leaves part
		if (open && t >= 0 && t < from) {
			sink.part(DOOR_SEAM, ms, LightTexture.FULL_BRIGHT);
			sink.part(DOOR_CHAIN, ms, LightTexture.FULL_BRIGHT);
		}
		if (open && t >= to)
			sink.part(DOOR_JAMB, ms, LightTexture.FULL_BRIGHT);
		ms.popPose();
	}

	// ---------------------------------------------------------------- Wall vent (D57, D59, D60)

	/**
	 * The hatch's turn about its hinge (the vent preview's burst clip), degrees, {@code t} seconds into the burst: still,
	 * then rattling (something kicking inside, harder from 0.3 s), then blown: slammed flat onto the floor in 0.1 s,
	 * bouncing twice, flat (-90) for good.
	 */
	static float hatchAngle(float t) {
		if (t < WallVentBlockEntity.RATTLE_FROM)
			return 0;
		if (t < WallVentBlockEntity.BLOW)
			return 2.5f * Mth.sin((t - WallVentBlockEntity.RATTLE_FROM) * 70) * (t > WallVentBlockEntity.RATTLE_HARDER ? 1.6f : 1);
		float u = (t - WallVentBlockEntity.BLOW) / (WallVentBlockEntity.SLAMMED - WallVentBlockEntity.BLOW);
		if (u < 1)
			return -90 * u * u;
		float b = t - WallVentBlockEntity.SLAMMED;
		return -90 + 14 * (float) Math.exp(-7 * b) * Math.abs(Mth.sin(b * 16));
	}

	/**
	 * A wall vent: sealed, its hatch shut and seam dark; live, the seam lit in the edge tone, the charging contact dim at
	 * the back and a dull lens glint between the louvres, now and then blinking out (the live clip's 4 s loop); bursting,
	 * the vent preview's sequence (the seam flares to the core tone, the glint flickers, the hatch rattles and blows flat,
	 * the light goes out); open, the hatch lies flat on the floor in front, everything dark.
	 */
	public static void vent(WallVentBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		WallVentBlock.State st = state.getValue(WallVentBlock.STATE);
		float t = st != WallVentBlock.State.OPEN || be.burstAt() == SubstationBlockEntity.NEVER ? 1e6f : since(be, be.burstAt(), pt);
		boolean bursting = st == WallVentBlock.State.OPEN && t < WallVentBlockEntity.BURST_LENGTH;
		boolean live = st == WallVentBlock.State.LIVE;
		ms.pushPose();
		face(ms, state.getValue(WallVentBlock.FACING));
		ms.pushPose();
		turn(ms, Axis.XP, st != WallVentBlock.State.OPEN ? 0 : bursting ? hatchAngle(t) : -90, 8, 0, 0);
		sink.part(VENT_HATCH, ms, light);
		if (live || bursting && (t < WallVentBlockEntity.FLASH_FROM || t >= WallVentBlockEntity.FLASH_TO && t < WallVentBlockEntity.BLOW))
			sink.part(VENT_SEAM, ms, LightTexture.FULL_BRIGHT);
		if (bursting && t >= WallVentBlockEntity.FLASH_FROM && t < WallVentBlockEntity.FLASH_TO)
			sink.part(VENT_FLASH, ms, LightTexture.FULL_BRIGHT);
		ms.popPose();
		if (live || bursting && t < WallVentBlockEntity.BLOW)
			sink.part(VENT_CONTACT, ms, LightTexture.FULL_BRIGHT);
		// the glint: the live clip blinks it out at 1.6-1.75 s and 3.1-3.2 s of 4; the burst flickers it, then it's gone
		float loop = seconds(be, pt) % 4;
		boolean glint = live ? !(loop >= 1.6f && loop < 1.75f || loop >= 3.1f && loop < 3.2f)
			: bursting && (t < 0.2f || t >= 0.26f && t < 0.36f);
		if (glint)
			sink.part(VENT_GLINT, ms, light);
		ms.popPose();
	}

	// ---------------------------------------------------------------- Derelict lamps

	static boolean lampActive(DerelictLightBlockEntity be) {
		BlockState state = be.getBlockState();
		boolean lamp = state.getBlock() instanceof DerelictStreetLampBlock && state.getValue(DerelictStreetLampBlock.PART) == DerelictStreetLampBlock.Part.HEAD
			|| state.getBlock() instanceof DerelictWallLampBlock;
		return lamp && (be.phase() != DerelictLightBlockEntity.Phase.DARK || be.getLevel() != null && be.surging(be.getLevel().getGameTime()));
	}

	/** A surge's frame is on (D54): hard flicker at first, settling over its 3 s (more frames on as it goes). */
	static boolean surgeOn(BlockPos pos, float ticks) {
		int frame = (int) (ticks / 1.5f);
		float chance = 0.4f + 0.6f * Mth.clamp(ticks / DerelictLightBlockEntity.SURGE_TICKS, 0, 1);
		long h = pos.asLong() * 0x9E3779B97F4A7C15L + frame * 0xD6E8FEB86659FD93L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h & 0xFFFFFF) / (float) 0x1000000 < chance;
	}

	public static void lamp(DerelictLightBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		if (!lampActive(be) || !be.lightable())
			return;
		BlockState state = be.getBlockState();
		// the heart surging at the Custodian's overload (D54): about 3 s of flicker, lit or dark, then as it was
		if (be.getLevel() != null && be.surging(be.getLevel().getGameTime())) {
			if (!surgeOn(be.getBlockPos(), since(be, be.surgeAt(), pt) * 20))
				return;
		} else if (be.phase() == DerelictLightBlockEntity.Phase.DARK)
			return;
		// starving: a second's flicker, then dark
		else if (be.phase() == DerelictLightBlockEntity.Phase.FADING
			&& !flickerOn(be.getBlockPos(), since(be, be.flickerAt(), pt) * 20, false))
			return;
		ms.pushPose();
		boolean wall = state.getBlock() instanceof DerelictWallLampBlock;
		face(ms, state.getValue(wall ? DerelictWallLampBlock.FACING : DerelictStreetLampBlock.FACING));
		sink.part(wall ? WALL_GLOW : state.getValue(DerelictStreetLampBlock.LEANING) ? STREET_GLOW_LEANING : STREET_GLOW, ms, LightTexture.FULL_BRIGHT);
		ms.popPose();
	}

	// ---------------------------------------------------------------- Conduit Pylon

	public static void pylon(ConduitPylonBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		boolean standing = be.isStanding();
		ms.pushPose();
		face(ms, state.getValue(ConduitPylonBlock.FACING));
		if (standing) {
			sink.part(PYLON_GROOVE, ms, LightTexture.FULL_BRIGHT);
			ms.pushPose();
			ms.translate(0, 1, 0);
			sink.part(PYLON_SEAMS, ms, LightTexture.FULL_BRIGHT);
			ms.popPose();
		}
		// the crown, in the top block: broken, it slumps forward; re-lit, it jolts back upright
		ms.translate(0, 2, 0);
		float slump = 0;
		if (!standing)
			slump = be.brokenAt() == SubstationBlockEntity.NEVER ? 1 : jolt(since(be, be.brokenAt(), pt), 0.35f);
		else if (be.isRelit() && be.relitAt() != SubstationBlockEntity.NEVER)
			slump = 1 - jolt(since(be, be.relitAt(), pt), 0.12f);
		turn(ms, Axis.XP, -20 * Mth.clamp(slump, -0.5f, 1.5f), 8, 8, 5);
		sink.part(PYLON_CROWN, ms, light);
		// fins snap in order, one a hit; the newest tumbles off
		int left = be.finsLeft(), gone = 6 - left;
		float sinceHit = since(be, be.lastHitAt(), pt);
		for (int k = 0; k < 6; k++) {
			ms.pushPose();
			turn(ms, Axis.YP, 60 * k + 30, 8, 0, 8);
			sink.part(k < gone ? PYLON_STUB : PYLON_FIN, ms, light);
			if (k == gone - 1 && sinceHit < 0.55f && be.lastHitAt() != SubstationBlockEntity.NEVER) {
				float u = sinceHit;
				px(ms, 0, -Math.min(40, 30 * u * u), -3 * Math.min(u, 0.6f));
				turn(ms, Axis.XP, -Math.min(150, 260 * u), 8, 12, 8);
				sink.part(PYLON_FIN, ms, light);
			}
			ms.popPose();
		}
		// the core: dimmer with each hit, cracked from the first, flickering when failing; dark when broken; blazing as it's re-lit
		ms.pushPose();
		if (!standing)
			sink.part(PYLON_CORE_DARK, ms, light);
		else {
			float sinceRelit = be.isRelit() && be.relitAt() != SubstationBlockEntity.NEVER ? since(be, be.relitAt(), pt) : 1e6f;
			if (sinceRelit < 0.6f) {
				float b = 1 + 0.6f * (float) Math.exp(-sinceRelit * 5);
				turnScale(ms, b, 8, 12, 8);
				sink.part(PYLON_CORE_BLAZE, ms, LightTexture.FULL_BRIGHT);
			} else {
				boolean on = !be.isFailing() || ((int) (seconds(be, pt) * 9)) % 4 != 0;
				float dim = Math.max(0.35f, 1 - 0.08f * Math.round(be.hits() * 6f / ConduitPylonBlockEntity.maxHits()));
				if (on)
					sink.part(be.hits() == 0 && !be.isRelit() ? PYLON_CORE : PYLON_CORE_CRACKED, ms, LightTexture.FULL_BRIGHT, RenderType.cutout(), grey(dim));
				else
					sink.part(PYLON_CORE_DARK, ms, light);
			}
		}
		ms.popPose();
		ms.popPose();
	}

	static void turnScale(PoseStack ms, float s, float px, float py, float pz) {
		px(ms, px, py, pz);
		ms.scale(s, s, s);
		px(ms, -px, -py, -pz);
	}

	// ---------------------------------------------------------------- Derelict Star Reactor

	public static void derelictReactor(DerelictStarReactorBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		boolean salvaged = be.isSalvaged(), burning = be.isBurning() && !salvaged;
		float fall = !salvaged ? 0 : be.salvagedAt() == SubstationBlockEntity.NEVER ? 1 : jolt(since(be, be.salvagedAt(), pt) - 0.3f, 0.5f);
		if (burning)
			sink.part(DSR_GROOVES, ms, LightTexture.FULL_BRIGHT);
		for (int k = 0; k < 4; k++) {
			ms.pushPose();
			// the arms are built a block down, on the north side; each turned a quarter round the centre column
			ms.translate(0, 1, 0);
			turn(ms, Axis.YP, 90 * k, 8, 0, 8);
			if (burning)
				sink.part(DSR_ARM_SEAM, ms, LightTexture.FULL_BRIGHT);
			// salvaged, the arms split and fall open from their feet
			turn(ms, Axis.XP, -35 * fall, 8, -10, -10);
			sink.part(DSR_ARM, ms, light);
			ms.popPose();
		}
	}

	static final class DerelictReactorRenderer extends SafeBlockEntityRenderer<DerelictStarReactorBlockEntity> {
		@Override
		protected void renderSafe(DerelictStarReactorBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
			derelictReactor(be, pt, ms, PartSink.forRenderer(be, buffer, overlay), light);
			if (!be.isBurning() || be.isSalvaged())
				return;
			float s = seconds(be, pt);
			// the light inside the glass, pulsing on a 4 s beat; the star turning slowly in it
			ms.pushPose();
			ms.translate(0, 1, 0);
			turnScale(ms, 1 + 0.015f * Mth.sin(s * Mth.PI / 2), 8, 14, 8);
			PartSink.screen(buffer, be.getBlockState(), overlay).part(DSR_CORE, ms, LightTexture.FULL_BRIGHT, RenderType.translucent());
			ms.popPose();
			star(ms, buffer, be, 8, 30, 8, 10, -s * 45);
		}

		@Override
		public boolean shouldRenderOffScreen(DerelictStarReactorBlockEntity be) {
			return true;
		}

		@Override
		public AABB getRenderBoundingBox(DerelictStarReactorBlockEntity be) {
			return new AABB(be.getBlockPos()).inflate(1.5).expandTowards(0, 2, 0);
		}

		@Override
		public int getViewDistance() {
			return 128;
		}
	}

	/** A Nether Star as items are drawn, full-bright, centred at (x, y, z) px, {@code size} px across, turned about y. */
	static void star(PoseStack ms, MultiBufferSource buffer, BlockEntity be, float x, float y, float z, float size, float yaw) {
		ms.pushPose();
		px(ms, x, y, z);
		ms.mulPose(Axis.YP.rotationDegrees(yaw));
		ms.scale(size / 16f, size / 16f, size / 16f);
		Minecraft.getInstance().getItemRenderer().renderStatic(STAR, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
			ms, buffer, be.getLevel(), (int) be.getBlockPos().asLong());
		ms.popPose();
	}

	// ---------------------------------------------------------------- Star Reactor

	/** Turned as its blockstate turns its model (built with its terminal south): x first, then y, about the middle. */
	static void reactorFacing(PoseStack ms, Direction facing) {
		int x = facing == Direction.UP ? 90 : facing == Direction.DOWN ? 270 : 0;
		int y = switch (facing) {
			case WEST -> 90;
			case NORTH -> 180;
			case EAST -> 270;
			default -> 0;
		};
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-y));
		ms.mulPose(Axis.XP.rotationDegrees(-x));
		ms.translate(-0.5, -0.5, -0.5);
	}

	/** The pip ring's places (as the exporter's): n round the top face's centre. */
	static float[] pip(int i, int n) {
		double a = (double) i / n * 2 * Math.PI - Math.PI / 2, r = n == 8 ? 4.5 : 5;
		return new float[] {Math.round((8 + Math.cos(a) * r) * 2) / 2f, Math.round((8 + Math.sin(a) * r) * 2) / 2f};
	}

	public static void starReactor(StarReactorBlockEntity be, float pt, PoseStack ms, PartSink sink, int light) {
		BlockState state = be.getBlockState();
		boolean upgraded = state.getValue(StarReactorBlock.UPGRADED);
		ms.pushPose();
		reactorFacing(ms, state.getValue(StarReactorBlock.FACING));
		// the burn readout: a pip goes out for each hour burned (the first one first)
		int n = upgraded ? 12 : 8, lit = Math.min(n, be.hoursLeft());
		for (int i = n - lit; i < n; i++) {
			float[] p = pip(i, n);
			ms.pushPose();
			px(ms, p[0] - 8, 0, p[1] - 8);
			sink.part(SR_PIP, ms, LightTexture.FULL_BRIGHT);
			ms.popPose();
		}
		// overhauled: the void-alloy orbit ring round the core, tilted, turning once every 6 s
		if (upgraded) {
			ms.pushPose();
			turn(ms, Axis.XP, 25, 8, 8, 8);
			turn(ms, Axis.YP, be.isRunning() ? -seconds(be, pt) * 60 : 0, 8, 8, 8);
			sink.part(SR_ORBIT, ms, light);
			ms.popPose();
		}
		ms.popPose();
	}

	static final class StarReactorRenderer extends SafeBlockEntityRenderer<StarReactorBlockEntity> {
		@Override
		protected void renderSafe(StarReactorBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
			starReactor(be, pt, ms, PartSink.forRenderer(be, buffer, overlay), light);
			if (be.hoursLeft() <= 0)
				return;
			ms.pushPose();
			reactorFacing(ms, be.getBlockState().getValue(StarReactorBlock.FACING));
			if (be.isRunning())
				PartSink.screen(buffer, be.getBlockState(), overlay).part(SR_PORTS, ms, LightTexture.FULL_BRIGHT, RenderType.translucent());
			// the star turns once every 8 s while it burns
			star(ms, buffer, be, 8, 8, 8, 6, be.isRunning() ? -seconds(be, pt) * 45 : 0);
			ms.popPose();
		}
	}
}
