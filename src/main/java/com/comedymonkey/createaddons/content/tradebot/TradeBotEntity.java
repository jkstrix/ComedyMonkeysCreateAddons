package com.comedymonkey.createaddons.content.tradebot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.Wrenches;
import com.comedymonkey.createaddons.content.tradebot.trade.BotPricing;
import com.comedymonkey.createaddons.content.tradebot.trade.VillagerClaims;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipClock;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipRoot;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.items.ItemStackHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.PathType;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotNavigation;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Trade Bot: a clockwork robot who sells your goods to seated villagers. See design/specs/trade-bot.md for what he
 * does and design/plans/trade-bot.md for how it's built.
 * <p>
 * The server picks which clip plays and when ({@link #playClip}); clients pose him from the same clip data the
 * Blockbench preview uses. In {@link Clip.Root#PHYSICAL} clips (hops, stairs, docking) the server moves him along the
 * clip's root path from where the clip started, and clients draw him on that path.
 */
public class TradeBotEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> CLIP = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CLIP_START = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.INT);
	/** Where a physical clip started: a block, plus the offset within it (floats alone are too coarse far from spawn). */
	private static final EntityDataAccessor<BlockPos> CLIP_ANCHOR_BLOCK = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.BLOCK_POS);
	private static final EntityDataAccessor<Vector3f> CLIP_ANCHOR = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Float> CLIP_ANCHOR_YAW = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.FLOAT);
	/** How fast the clip plays (turns play faster at his real rolling speed), and how far a physical clip's travel is
	 * stretched across the ground (so a hop lands exactly on the next block). */
	private static final EntityDataAccessor<Float> CLIP_SPEED = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> CLIP_SCALE = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.BYTE);
	private static final byte FLAG_CHARGING = 1, FLAG_WOUND_DOWN = 2;

	public static final String IDLE = "idle";
	public static final String DOCKED = "dock_lights_out";
	public static final String WIND_DOWN = "wind_down", REWIND = "rewind";
	/** Out of water this long (ticks), he's dry and winds himself back up (D29). */
	private static final int DRY_TICKS = 100;

	/** Why he's wound down (D24, D29): hurt to nothing (his owner's wrench winds him up) or soaked (he dries out). */
	public enum Rundown { DAMAGE, WATER }
	public static final int LOAD_SLOTS = 27;

	/** Client only: the cross-fade and sound cues. */
	public final ClipPlayback playback = new ClipPlayback();
	/** Client only: the clip last seen from the server (id and start tick), and when it started on the {@link ClipClock}. */
	private int seenClip = -1, seenClipStart;
	private double localClipStart;
	/** Client only: how far the station's winder has turned his key while charging, in degrees (60 a second). */
	private float windSpin, windSpinO;
	/** Client only: his wheel's turn from the ground he has actually covered (radians, 4 px radius). */
	private float wheelAngle, wheelAngleO;
	private static final float WHEEL_RADIUS = 4 / 16f;
	private static final float WIND_DEGREES_PER_TICK = 3;

	@Nullable
	private UUID owner;
	@Nullable
	private BlockPos station;
	private boolean docked;
	private int charge;
	/** The emerald pouch (D48), and the trade XP saved up for Nuggets of Experience (D57). */
	private long emeralds;
	private int xpBank;
	/** His own standing with each villager he's traded with (D34, D42). */
	private final Map<UUID, Integer> reputation = new HashMap<>();
	/** What he's carrying: a barrel's worth (D45). */
	private final ItemStackHandler load = new ItemStackHandler(LOAD_SLOTS);
	/** Set while he's packed into his item, so his removal isn't taken for him being lost. */
	private boolean packed;
	/** In a Ponder scene, whose world clock stands still: his clips run on his own ticks instead. */
	private boolean ponder;
	@Nullable
	private Rundown rundown;
	private int dryTicks;
	/** Containers, doors and gates he had open when he was unloaded (saved): shut once he's back. */
	private final List<BlockPos> leftOpen = new ArrayList<>();

	public final TradeBotMotion motion = new TradeBotMotion(this);
	public final TradeBotBrain brain = new TradeBotBrain(this);

	public TradeBotEntity(EntityType<? extends TradeBotEntity> type, Level level) {
		super(type, level);
		// he keeps out of water and lava, and away from fire (D29)
		setPathfindingMalus(PathType.WATER, -1);
		setPathfindingMalus(PathType.WATER_BORDER, 4);
		setPathfindingMalus(PathType.LAVA, -1);
		setPathfindingMalus(PathType.DAMAGE_FIRE, -1);
		setPathfindingMalus(PathType.DANGER_FIRE, 16);
		setPathfindingMalus(PathType.POWDER_SNOW, -1);
		setPathfindingMalus(PathType.DANGER_POWDER_SNOW, -1);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new TradeBotNavigation(this, level);
	}

	/** He hops off a block at most (D4). */
	@Override
	public int getMaxFallDistance() {
		return 1;
	}

	/** No ladders, vines or scaffolding (D18). */
	@Override
	public boolean onClimbable() {
		return false;
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		shutWhatHeLeftOpen();
		tickRundown();
		brain.tick();
		motion.tick();
	}

	// ---- winding down and up again (D24, D29, D59)

	/** Wound down: slumped and dark, doing nothing until he's wound up again. */
	public boolean isWoundDown() {
		return (entityData.get(FLAGS) & FLAG_WOUND_DOWN) != 0;
	}

	@Nullable
	public Rundown rundown() {
		return rundown;
	}

	/** Soaked out in the world: he winds down until he's out and dry. In lava or fallen out of the world: home he goes. */
	private void tickRundown() {
		if (rundown == null) {
			if (isInWater() && !docked)
				windDown(Rundown.WATER);
			return;
		}
		if (isInLava()) {
			rescue();
			return;
		}
		if (rundown == Rundown.WATER) {
			dryTicks = isInWater() ? 0 : dryTicks + 1;
			if (dryTicks >= DRY_TICKS)
				windUp(false);
		}
	}

	/** Runs down where he is (slumps, lights out), dropping whatever he was doing but keeping everything he carries. */
	public void windDown(Rundown why) {
		if (rundown == Rundown.DAMAGE && why == Rundown.WATER)
			return; // already down for good: drying out won't wind him up
		boolean already = rundown != null;
		rundown = why;
		dryTicks = 0;
		setFlag(FLAG_WOUND_DOWN, true);
		if (!already) {
			brain.windDown();
			playClip(WIND_DOWN);
			if (why == Rundown.WATER)
				awardOwner(com.comedymonkey.createaddons.advancement.ModAdvancements.TRADE_BOT_SOAKED, 0);
		}
	}

	/** Wound up again: by his owner's wrench (back to full health), or dried out. Then he carries on. */
	public void windUp(boolean wrench) {
		if (rundown == null)
			return;
		rundown = null;
		setFlag(FLAG_WOUND_DOWN, false);
		if (wrench)
			setHealth(getMaxHealth());
		playClip(REWIND);
		brain.windingUp();
	}

	/** Never dies (D24): at no health he winds down instead. Only the kill command removes him, dropping what he carries. */
	@Override
	public void die(DamageSource source) {
		if (source.is(DamageTypes.GENERIC_KILL)) {
			if (!isRemoved() && !dead)
				dropCarried();
			super.die(source);
			return;
		}
		setHealth(1);
		windDown(Rundown.DAMAGE);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		boolean hurt = super.hurt(source, amount);
		if (hurt && !level().isClientSide && rundown == null && source.getEntity() instanceof Player player)
			brain.hitBy(player);
		return hurt;
	}

	/** Clockwork doesn't drown: water winds him down instead. */
	@Override
	public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
		return false;
	}

	/** Fallen out of the world: back to his station, wound down (D59). */
	@Override
	protected void onBelowWorld() {
		if (!rescue())
			super.onBelowWorld();
	}

	/** Put back in front of his station, wound down, out of harm's way (D59); false if he has no station to go to. */
	private boolean rescue() {
		if (station == null || !(level().getBlockState(station).getBlock() instanceof TradeBotStationBlock))
			return false;
		Direction facing = level().getBlockState(station).getValue(TradeBotStationBlock.FACING);
		Vec3 at = TradeBotBrain.dockStart(station, facing);
		clearFire();
		fallDistance = 0;
		setDeltaMovement(Vec3.ZERO);
		teleportTo(at.x, at.y, at.z);
		motion.resetTravel(); // put back, not rolled: the jump costs nothing
		setYRot(facing.toYRot());
		if (rundown != Rundown.DAMAGE)
			windDown(Rundown.DAMAGE);
		playClipAtEnd(ClipLibrary.get().clip(WIND_DOWN));
		if (TradeBotMotion.debug)
			LogUtils.getLogger().info("[Trade Bot {}] rescued to his station at {}", getId(), station.toShortString());
		return true;
	}

	private void setFlag(byte flag, boolean on) {
		byte flags = entityData.get(FLAGS);
		byte now = (byte) (on ? flags | flag : flags & ~flag);
		if (now != flags)
			entityData.set(FLAGS, now);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20)
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			// he rolls over anything 4 px or lower; taller steps are hops (design/specs/trade-bot.md D4)
			.add(Attributes.STEP_HEIGHT, 0.25)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
			// routes as far as his range from the station (D28), with some to spare
			.add(Attributes.FOLLOW_RANGE, 96);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLIP, ClipLibrary.get().id(ClipLibrary.get().clip(IDLE)));
		builder.define(CLIP_START, 0);
		builder.define(CLIP_ANCHOR_BLOCK, BlockPos.ZERO);
		builder.define(CLIP_ANCHOR, new Vector3f());
		builder.define(CLIP_ANCHOR_YAW, 0f);
		builder.define(CLIP_SPEED, 1f);
		builder.define(CLIP_SCALE, 1f);
		builder.define(FLAGS, (byte) 0);
	}

	// ---- home, charge and what he carries

	/** Sets him up on a newly placed station, docked, with what his item carried. */
	public void setUp(UUID owner, BlockPos station, Direction facing, TradeBotData data) {
		this.owner = owner;
		this.station = station;
		applyData(data);
		dockAt(station, facing);
	}

	@Nullable
	public UUID getOwner() {
		return owner;
	}

	@Nullable
	public BlockPos getStation() {
		return station;
	}

	public boolean isDocked() {
		return docked;
	}

	/** Sits him in his station's cradle, lights out: the docking clip's last frame, held. */
	public void dockAt(BlockPos station, Direction facing) {
		Clip dock = ClipLibrary.get().clip(DOCKED);
		Vec3 end = TradeBotStationBlock.dockPoint(station, facing);
		float yaw = facing.toYRot();
		holdClipEnd(dock, end, yaw);
		motion.resetTravel(); // set in his cradle, not rolled there
		docked = true;
	}

	/** Back to his station, rolling home and docking (D27, D30, D38); false if he's run down and must be wound up first. */
	public boolean goHome() {
		return brain.goHome();
	}

	/** He's hopping out of the cradle. */
	void leaveDock() {
		docked = false;
	}

	/** His earnings: the emerald pouch (D48) and the trade XP saved up for Nuggets of Experience (D57). */
	public long getEmeralds() {
		return emeralds;
	}

	public int getXpBank() {
		return xpBank;
	}

	public void addEarnings(long emeralds, int xp) {
		this.emeralds = Math.max(0, this.emeralds + emeralds);
		this.xpBank = Math.max(0, this.xpBank + xp);
	}

	/** Something to deliver: emeralds, or XP for at least one nugget. */
	public boolean hasEarnings() {
		return emeralds > 0 || xpBank >= XP_PER_NUGGET;
	}

	/** His own reputation with a villager (D34, D42): 0 until they've traded. */
	public int reputationWith(UUID villager) {
		return reputation.getOrDefault(villager, 0);
	}

	/** One more trade with {@code villager}: his reputation with it grows, up to vanilla's cap. */
	public void tradedWith(UUID villager) {
		reputation.put(villager, BotPricing.raise(reputationWith(villager)));
	}

	/** A Create Nugget of Experience is worth 3 XP. */
	public static final int XP_PER_NUGGET = 3;

	/** What he's carrying (27 slots, D45). */
	public ItemStackHandler getLoad() {
		return load;
	}

	public int getCharge() {
		return charge;
	}

	public int chargeRoom() {
		return Math.max(0, CMConfig.SERVER.tradeBotFullCharge() - charge);
	}

	public void addCharge(int fe) {
		charge = Math.min(CMConfig.SERVER.tradeBotFullCharge(), Math.max(0, charge + fe));
	}

	/** The station is putting charge into him: his key and its winder keep turning. */
	public void setCharging(boolean charging) {
		setFlag(FLAG_CHARGING, charging);
	}

	public boolean isCharging() {
		return (entityData.get(FLAGS) & FLAG_CHARGING) != 0;
	}

	/** Client: degrees the winder has turned his key since he docked, between ticks. */
	public float windSpin(float partialTick) {
		return Mth.lerp(partialTick, windSpinO, windSpin);
	}

	/** Client: radians his wheel has rolled, between ticks. */
	public float wheelAngle(float partialTick) {
		return Mth.lerp(partialTick, wheelAngleO, wheelAngle);
	}

	/** What goes into his item when he's packed up, with his station's links. */
	public TradeBotData toData(List<BlockPos> inputs, Optional<BlockPos> output) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < load.getSlots(); i++)
			stacks.add(load.getStackInSlot(i).copy());
		return new TradeBotData(charge, emeralds, xpBank, reputation, ItemContainerContents.fromItems(stacks), inputs, output);
	}

	private void applyData(TradeBotData data) {
		charge = Math.min(data.charge(), CMConfig.SERVER.tradeBotFullCharge());
		emeralds = data.emeralds();
		xpBank = data.xpBank();
		reputation.clear();
		reputation.putAll(data.reputation());
		NonNullList<ItemStack> stacks = NonNullList.withSize(LOAD_SLOTS, ItemStack.EMPTY);
		data.load().copyInto(stacks);
		for (int i = 0; i < LOAD_SLOTS; i++)
			load.setStackInSlot(i, stacks.get(i));
	}

	/**
	 * Drops everything he carries where he is (removed some other way than packing): his load, his emeralds, and his
	 * banked XP as Nuggets of Experience, 3 XP each as he'd deliver them, with any XP left over as an orb.
	 */
	public void dropCarried() {
		if (!(level() instanceof ServerLevel server))
			return;
		for (int slot = 0; slot < load.getSlots(); slot++) {
			ItemStack stack = load.getStackInSlot(slot);
			if (!stack.isEmpty())
				spawnAtLocation(stack.copy());
			load.setStackInSlot(slot, ItemStack.EMPTY);
		}
		for (long left = emeralds; left > 0; left -= 64)
			spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.EMERALD, (int) Math.min(64, left)));
		net.minecraft.world.item.Item nugget = net.minecraft.core.registries.BuiltInRegistries.ITEM
			.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "experience_nugget"));
		for (int left = xpBank / XP_PER_NUGGET; left > 0; left -= 64)
			spawnAtLocation(new ItemStack(nugget, Math.min(64, left)));
		if (xpBank % XP_PER_NUGGET > 0)
			net.minecraft.world.entity.ExperienceOrb.award(server, position(), xpBank % XP_PER_NUGGET);
		emeralds = 0;
		xpBank = 0;
	}

	/** Removed because he was packed into his item. */
	public void packUp() {
		packed = true;
		discard();
	}

	// ---- clips

	/** Starts a clip now (server). A physical clip remembers where it started, so it can move him from there. */
	public void playClip(Clip clip) {
		playClip(clip, 1, 1);
	}

	/** Starts a clip at {@code speed} times its own pace; a physical one's ground travel is stretched by {@code scale}. */
	public void playClip(Clip clip, float speed, float scale) {
		entityData.set(CLIP, ClipLibrary.get().id(clip));
		entityData.set(CLIP_START, (int) now());
		entityData.set(CLIP_SPEED, speed);
		entityData.set(CLIP_SCALE, scale);
		if (clip.root() == Clip.Root.PHYSICAL) {
			setClipAnchor(position());
			entityData.set(CLIP_ANCHOR_YAW, getYRot());
		}
	}

	public void playClip(String name) {
		playClip(ClipLibrary.get().clip(name));
	}

	/** An advancement for his owner, if they're online (what he does earns them credit, wherever they are). */
	public void awardOwner(String action, int value) {
		if (owner != null && level() instanceof ServerLevel server) {
			net.minecraft.server.level.ServerPlayer player = server.getServer().getPlayerList().getPlayer(owner);
			if (player != null)
				com.comedymonkey.createaddons.advancement.ModAdvancements.award(player, action, ItemStack.EMPTY, value);
		}
	}

	/** The clock his clips run on: the world's, or in Ponder his own ticks. */
	public long now() {
		return ponder ? tickCount : level().getGameTime();
	}

	/** For Ponder scenes: his clips run on his own ticks (call before playing any). */
	public void setPonder() {
		ponder = true;
	}

	/** A held clip, already played to its end (wound down when he's loaded, or put back at his station). */
	public void playClipAtEnd(Clip clip) {
		entityData.set(CLIP, ClipLibrary.get().id(clip));
		entityData.set(CLIP_START, (int) (now() - Mth.ceil(clip.length() * 20) - 1));
		entityData.set(CLIP_SPEED, 1f);
		entityData.set(CLIP_SCALE, 1f);
	}

	/** Puts him at the end of a held physical clip, as if he'd just played it to {@code end}, facing {@code yaw}. */
	private void holdClipEnd(Clip clip, Vec3 end, float yaw) {
		float startYaw = yaw + ClipRoot.turned(clip, clip.length()); // he faces yaw after the clip's own turn
		Vec3 travelled = ClipRoot.place(Vec3.ZERO, startYaw, ClipRoot.travel(clip, clip.length(), new Vector3f()));
		entityData.set(CLIP, ClipLibrary.get().id(clip));
		entityData.set(CLIP_START, (int) (now() - Mth.ceil(clip.length() * 20) - 1));
		setClipAnchor(end.subtract(travelled));
		entityData.set(CLIP_ANCHOR_YAW, startYaw);
		entityData.set(CLIP_SPEED, 1f);
		entityData.set(CLIP_SCALE, 1f);
		setYRot(yaw);
		moveTo(end.x, end.y, end.z, yaw, 0);
	}

	public Clip clip() {
		Clip clip = ClipLibrary.get().byId(entityData.get(CLIP));
		return clip != null ? clip : ClipLibrary.get().clip(IDLE);
	}

	/** The game tick the current clip started on. */
	public long clipStart() {
		// the tick is synced as an int; rebuild the full value from the current time
		long now = now();
		return now - (int) (now - entityData.get(CLIP_START));
	}

	/** Seconds the current clip has played, at {@code partialTick} into the current tick. */
	public float clipElapsed(float partialTick) {
		return (float) ((clipNow(partialTick) - clipStartTime()) / 20) * clipSpeed();
	}

	/**
	 * The time his clips are played at, in ticks, {@code partialTick} into the current tick. In a client's world that's
	 * the steady {@link ClipClock}, not the world time, which jumps when the server corrects it.
	 */
	public double clipNow(float partialTick) {
		return clientClock() ? ClipClock.now(partialTick) : now() + partialTick;
	}

	/** When the current clip started, on the {@link #clipNow} clock. */
	public double clipStartTime() {
		if (!clientClock())
			return clipStart();
		int id = entityData.get(CLIP), start = entityData.get(CLIP_START);
		if (id != seenClip || start != seenClipStart) {
			// how far into it the server already is: kept when we first see him (he may be mid-clip) or when it's a
			// clip held at its end; a clip that changes while we watch starts now, as the world time is only a guess
			int lead = Math.max(0, (int) (level().getGameTime() - start));
			Clip clip = clip();
			if (seenClip != -1 && !clip.finished(lead / 20f * entityData.get(CLIP_SPEED)))
				lead = 0;
			seenClip = id;
			seenClipStart = start;
			localClipStart = ClipClock.now(0) - lead;
		}
		return localClipStart;
	}

	private boolean clientClock() {
		return level().isClientSide && !ponder;
	}

	public float clipSpeed() {
		return entityData.get(CLIP_SPEED);
	}

	/** Where a physical clip started, and which way he faced. */
	public Vec3 clipAnchor() {
		BlockPos block = entityData.get(CLIP_ANCHOR_BLOCK);
		Vector3f offset = entityData.get(CLIP_ANCHOR);
		// in doubles: an int plus a float is summed as a float, which loses the offset far from spawn
		return new Vec3(block.getX() + (double) offset.x, block.getY() + (double) offset.y, block.getZ() + (double) offset.z);
	}

	private void setClipAnchor(Vec3 at) {
		BlockPos block = BlockPos.containing(at);
		entityData.set(CLIP_ANCHOR_BLOCK, block);
		entityData.set(CLIP_ANCHOR, new Vector3f((float) (at.x - block.getX()), (float) (at.y - block.getY()), (float) (at.z - block.getZ())));
	}

	public float clipAnchorYaw() {
		return entityData.get(CLIP_ANCHOR_YAW);
	}

	/** The facing a clip's turn is measured from (a game clip's turn starts from wherever he faced). */
	public void setClipAnchorYaw(float yaw) {
		entityData.set(CLIP_ANCHOR_YAW, yaw);
	}

	/** His place and facing on the current physical clip's path, at {@code seconds} into it. */
	public Vec3 rootPosition(Clip clip, float seconds) {
		Vector3f travel = ClipRoot.travel(clip, seconds, new Vector3f());
		float scale = entityData.get(CLIP_SCALE);
		return ClipRoot.place(clipAnchor(), clipAnchorYaw(), travel.set(travel.x * scale, travel.y, travel.z * scale));
	}

	public float rootYaw(Clip clip, float seconds) {
		return ClipRoot.facing(clipAnchorYaw(), ClipRoot.turned(clip, seconds));
	}

	@Override
	public void tick() {
		super.tick();
		// he has no separate head or body turn: everything faces where he faces
		yBodyRot = getYRot();
		yHeadRot = getYRot();
		if (level().isClientSide) {
			playback.observe(clip(), clipStartTime(), clipSpeed(), clipNow(0));
			playback.tickCues(clipNow(0), this::playCue);
			wheelAngleO = wheelAngle;
			float r = getYRot() * Mth.DEG_TO_RAD;
			// xOld, not xo: Ponder's world only keeps xOld up to date
			double forward = (getX() - xOld) * -Mth.sin(r) + (getZ() - zOld) * Mth.cos(r);
			wheelAngle += (float) (forward / WHEEL_RADIUS);
			windSpinO = windSpin;
			if (!clip().name().equals(DOCKED))
				windSpin = windSpinO = 0;
			else if (isCharging())
				windSpin += WIND_DEGREES_PER_TICK;
			return;
		}
		Clip clip = clip();
		float elapsed = clipElapsed(0);
		docked &= clip.name().equals(DOCKED); // anything else he plays takes him out of the cradle
		boolean physical = clip.root() == Clip.Root.PHYSICAL;
		noPhysics = physical && !clip.finished(elapsed);
		setNoGravity(noPhysics);
		// on the path for the whole clip, including its last frame (and held there by a hold clip)
		if (physical) {
			float t = clip.localTime(elapsed);
			setDeltaMovement(Vec3.ZERO);
			Vec3 at = rootPosition(clip, t);
			setPos(at.x, at.y, at.z);
			setYRot(rootYaw(clip, t));
		}
		if (clip.mode() == Clip.Mode.ONCE && clip.finished(elapsed) && !motion.isMoving())
			playClip(IDLE);
	}

	private void playCue(String event) {
		SoundEvent sound = ModSounds.TRADE_BOT.containsKey(event) ? ModSounds.tradeBot(event) : null;
		if (sound != null)
			level().playLocalSound(getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 1, 1, false);
	}

	// ---- owner's wrench, saving, removal

	/** His owner's wrench sends him home (D27). */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!Wrenches.isWrench(player.getItemInHand(hand)))
			return super.mobInteract(player, hand);
		if (level().isClientSide)
			return InteractionResult.SUCCESS;
		if (!TradeBotOwnership.canControl(player, owner)) {
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.not_yours"), true);
			return InteractionResult.CONSUME;
		}
		if (rundown != null) {
			windUp(true);
			com.comedymonkey.createaddons.advancement.ModAdvancements.award(player, com.comedymonkey.createaddons.advancement.ModAdvancements.TRADE_BOT_WOUND_UP);
			return InteractionResult.CONSUME;
		}
		if (!docked && goHome()) {
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.heading_home"), true);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (owner != null)
			tag.putUUID("Owner", owner);
		if (station != null)
			tag.put("Station", NbtUtils.writeBlockPos(station));
		tag.putBoolean("Docked", docked);
		if (rundown != null)
			tag.putString("Rundown", rundown.name());
		TradeBotData.CODEC.encodeStart(registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
			toData(List.of(), Optional.empty())).ifSuccess(data -> tag.put("TradeBot", data));
		// what he has open: his errand isn't saved, so it's shut when he's next loaded
		List<BlockPos> open = new ArrayList<>(leftOpen);
		BlockPos container = brain.openContainer();
		if (container != null && !open.contains(container))
			open.add(container);
		for (BlockPos door : motion.openedDoors())
			if (!open.contains(door))
				open.add(door);
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		for (BlockPos pos : open)
			list.add(NbtUtils.writeBlockPos(pos));
		if (!list.isEmpty())
			tag.put("LeftOpen", list);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		station = NbtUtils.readBlockPos(tag, "Station").orElse(null);
		leftOpen.clear();
		for (net.minecraft.nbt.Tag pos : tag.getList("LeftOpen", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
			CompoundTag wrapped = new CompoundTag();
			wrapped.put("Pos", pos);
			NbtUtils.readBlockPos(wrapped, "Pos").ifPresent(leftOpen::add);
		}
		docked = tag.getBoolean("Docked");
		if (tag.contains("TradeBot"))
			TradeBotData.CODEC.parse(registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("TradeBot"))
				.ifSuccess(this::applyData);
		// held clips aren't saved: put him back in his cradle, slumped if he's run down, or let him stand
		if (docked && station != null && level().getBlockState(station).getBlock() instanceof TradeBotStationBlock)
			dockAt(station, level().getBlockState(station).getValue(TradeBotStationBlock.FACING));
		if (tag.contains("Rundown")) {
			rundown = Rundown.valueOf(tag.getString("Rundown"));
			setFlag(FLAG_WOUND_DOWN, true);
			brain.windDown();
			playClipAtEnd(ClipLibrary.get().clip(WIND_DOWN));
		}
	}

	/** Removed for good some other way than packing (a command): his station can then be packed on its own. */
	@Override
	public void remove(RemovalReason reason) {
		if (TradeBotMotion.debug && !level().isClientSide)
			LogUtils.getLogger().info("[Trade Bot {}] removed ({}) at {}, health {}", getId(), reason, blockPosition().toShortString(), getHealth(),
				new Throwable("removed here"));
		if (!level().isClientSide)
			VillagerClaims.releaseAll(getUUID());
		if (!packed && reason.shouldDestroy() && level() instanceof ServerLevel server)
			TradeBotRegistry.get(server.getServer()).markGone(getUUID());
		super.remove(reason);
	}

	/**
	 * Gone from the world for any reason (packed, killed, unloaded): he shuts the container and the doors he opened,
	 * as nothing he was doing is saved to finish it later.
	 */
	@Override
	public void onRemovedFromLevel() {
		if (!level().isClientSide) {
			// unloading (no removal reason yet): his own chunk is already past changing, so what can't be shut now is
			// saved with him and shut when he's back
			boolean gone = getRemovalReason() != null && getRemovalReason().shouldDestroy();
			BlockPos open = brain.openContainer();
			brain.abortWork();
			if (open != null && !gone && !level().isLoaded(open))
				remember(open);
			List<BlockPos> doors = motion.closeOpenedDoors();
			if (!gone)
				doors.forEach(this::remember);
		}
		super.onRemovedFromLevel();
	}

	private void remember(BlockPos pos) {
		if (!leftOpen.contains(pos))
			leftOpen.add(pos.immutable());
	}

	/** Back after being unloaded: shut the container he had open, and hand the doors back to be shut once he's clear. */
	private void shutWhatHeLeftOpen() {
		if (leftOpen.isEmpty())
			return;
		leftOpen.removeIf(pos -> {
			if (!level().isLoaded(pos))
				return false;
			if (TradeBotMotion.isDoor(level().getBlockState(pos)))
				motion.adoptOpenedDoor(pos);
			else
				com.comedymonkey.createaddons.content.tradebot.work.ContainerWork.shut(level(), pos);
			return true;
		});
	}

	/** He stays in his station's world: through a portal he'd never find his way home. */
	@Override
	public boolean canChangeDimensions(Level from, Level to) {
		return false;
	}

	// ---- life

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	protected BodyRotationControl createBodyControl() {
		// no lagging body turn: he faces where he rolls
		return new BodyRotationControl(this) {
			@Override
			public void clientTick() {}
		};
	}

	@Override
	@Nullable
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.tradeBot("bonk");
	}

	@Override
	@Nullable
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public boolean isPushable() {
		// nothing shoves him off a hop or out of his station mid-move
		return clip().root() != Clip.Root.PHYSICAL && super.isPushable();
	}
}
