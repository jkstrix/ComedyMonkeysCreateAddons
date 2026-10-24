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
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
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
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
	private static final EntityDataAccessor<Vector3f> CLIP_ANCHOR = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Float> CLIP_ANCHOR_YAW = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.BYTE);
	private static final byte FLAG_CHARGING = 1;

	public static final String IDLE = "idle";
	public static final String DOCKED = "dock_lights_out";
	public static final int LOAD_SLOTS = 27;

	/** Client only: the cross-fade and sound cues. */
	public final ClipPlayback playback = new ClipPlayback();
	/** Client only: how far the station's winder has turned his key while charging, in degrees (60 a second). */
	private float windSpin, windSpinO;
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

	public TradeBotEntity(EntityType<? extends TradeBotEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20)
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			// he rolls over anything 4 px or lower; taller steps are hops (design/specs/trade-bot.md D4)
			.add(Attributes.STEP_HEIGHT, 0.25)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLIP, ClipLibrary.get().id(ClipLibrary.get().clip(IDLE)));
		builder.define(CLIP_START, 0);
		builder.define(CLIP_ANCHOR, new Vector3f());
		builder.define(CLIP_ANCHOR_YAW, 0f);
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
		docked = true;
	}

	/** Back to his station (D27, D30, D38). Walking home comes with movement (milestone 3); for now he's put back. */
	public void goHome() {
		if (station != null && level().getBlockState(station).getBlock() instanceof TradeBotStationBlock)
			dockAt(station, level().getBlockState(station).getValue(TradeBotStationBlock.FACING));
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
		byte flags = entityData.get(FLAGS);
		byte now = (byte) (charging ? flags | FLAG_CHARGING : flags & ~FLAG_CHARGING);
		if (now != flags)
			entityData.set(FLAGS, now);
	}

	public boolean isCharging() {
		return (entityData.get(FLAGS) & FLAG_CHARGING) != 0;
	}

	/** Client: degrees the winder has turned his key since he docked, between ticks. */
	public float windSpin(float partialTick) {
		return Mth.lerp(partialTick, windSpinO, windSpin);
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

	/** Removed because he was packed into his item. */
	public void packUp() {
		packed = true;
		discard();
	}

	// ---- clips

	/** Starts a clip now (server). A physical clip remembers where it started, so it can move him from there. */
	public void playClip(Clip clip) {
		entityData.set(CLIP, ClipLibrary.get().id(clip));
		entityData.set(CLIP_START, (int) level().getGameTime());
		if (clip.root() == Clip.Root.PHYSICAL) {
			entityData.set(CLIP_ANCHOR, position().toVector3f());
			entityData.set(CLIP_ANCHOR_YAW, getYRot());
		}
	}

	public void playClip(String name) {
		playClip(ClipLibrary.get().clip(name));
	}

	/** Puts him at the end of a held physical clip, as if he'd just played it to {@code end}, facing {@code yaw}. */
	private void holdClipEnd(Clip clip, Vec3 end, float yaw) {
		float startYaw = yaw + ClipRoot.turned(clip, clip.length()); // he faces yaw after the clip's own turn
		Vec3 travelled = ClipRoot.place(Vec3.ZERO, startYaw, ClipRoot.travel(clip, clip.length(), new Vector3f()));
		entityData.set(CLIP, ClipLibrary.get().id(clip));
		entityData.set(CLIP_START, (int) (level().getGameTime() - Mth.ceil(clip.length() * 20) - 1));
		entityData.set(CLIP_ANCHOR, end.subtract(travelled).toVector3f());
		entityData.set(CLIP_ANCHOR_YAW, startYaw);
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
		long now = level().getGameTime();
		return now - (int) (now - entityData.get(CLIP_START));
	}

	/** Seconds the current clip has played, at {@code partialTick} into the current tick. */
	public float clipElapsed(float partialTick) {
		return (float) ((level().getGameTime() - clipStart() + partialTick) / 20);
	}

	/** Where a physical clip started, and which way he faced. */
	public Vec3 clipAnchor() {
		return new Vec3(entityData.get(CLIP_ANCHOR));
	}

	public float clipAnchorYaw() {
		return entityData.get(CLIP_ANCHOR_YAW);
	}

	/** His place and facing on the current physical clip's path, at {@code seconds} into it. */
	public Vec3 rootPosition(Clip clip, float seconds) {
		return ClipRoot.place(clipAnchor(), clipAnchorYaw(), ClipRoot.travel(clip, seconds, new Vector3f()));
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
			playback.observe(clip(), clipStart(), level().getGameTime());
			playback.tickCues(level().getGameTime(), this::playCue);
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
		if (clip.mode() == Clip.Mode.ONCE && clip.finished(elapsed))
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
		if (!docked) {
			goHome();
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
		TradeBotData.CODEC.encodeStart(registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
			toData(List.of(), Optional.empty())).ifSuccess(data -> tag.put("TradeBot", data));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		station = NbtUtils.readBlockPos(tag, "Station").orElse(null);
		docked = tag.getBoolean("Docked");
		if (tag.contains("TradeBot"))
			TradeBotData.CODEC.parse(registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("TradeBot"))
				.ifSuccess(this::applyData);
		// held clips aren't saved: put him back in his cradle, or let him stand
		if (docked && station != null && level().getBlockState(station).getBlock() instanceof TradeBotStationBlock)
			dockAt(station, level().getBlockState(station).getValue(TradeBotStationBlock.FACING));
	}

	/** Removed for good some other way than packing (a command): his station can then be packed on its own. */
	@Override
	public void remove(RemovalReason reason) {
		if (!packed && reason.shouldDestroy() && level() instanceof ServerLevel server)
			TradeBotRegistry.get(server.getServer()).markGone(getUUID());
		super.remove(reason);
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
