package com.comedymonkey.createaddons.content.tradebot.board;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.Create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Trade Board (design/specs/trade-master.md D31, D34, D40, D41; brief design/briefs/trade-board.md): a thin panel
 * hung flat on one face of a container, like an item frame, tuned to a Trade Master's network. With no gauges on that
 * face it's an input he takes from; Factory Gauges placed on that face (up to four, one gauge block) sit in its
 * four places and say what he fills the container to. It takes no block space, so the gauge block shares its space.
 */
public class TradeBoardEntity extends HangingEntity {
	/** How deep it stands off the container's face (its rim), in blocks. */
	private static final double DEPTH = 1.25 / 16;

	/** The network it belongs to: a Trade Master's station, or any Create link, tuned to the item it was placed from. */
	@Nullable
	private UUID frequency;

	public TradeBoardEntity(EntityType<? extends TradeBoardEntity> type, Level level) {
		super(type, level);
	}

	public TradeBoardEntity(Level level, BlockPos pos, Direction facing, @Nullable UUID frequency) {
		super(ModEntities.TRADE_BOARD.get(), level, pos);
		this.frequency = frequency;
		setDirection(facing);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	// ---- where it hangs (any of the six faces, as an item frame)

	@Override
	protected void setDirection(Direction facing) {
		this.direction = facing;
		if (facing.getAxis().isHorizontal()) {
			setXRot(0);
			setYRot(facing.get2DDataValue() * 90);
		} else {
			setXRot(-90 * facing.getAxisDirection().getStep());
			setYRot(0);
		}
		xRotO = getXRot();
		yRotO = getYRot();
		recalculateBoundingBox();
	}

	/** The whole face it hangs on, {@link #DEPTH} deep. */
	@Override
	protected AABB calculateBoundingBox(BlockPos pos, Direction facing) {
		Vec3 centre = Vec3.atCenterOf(pos).relative(facing, -0.5 + DEPTH / 2);
		Direction.Axis axis = facing.getAxis();
		return AABB.ofSize(centre, axis == Direction.Axis.X ? DEPTH : 1, axis == Direction.Axis.Y ? DEPTH : 1, axis == Direction.Axis.Z ? DEPTH : 1);
	}

	/** The block it hangs on: the container. */
	public BlockPos containerPos() {
		return getPos().relative(direction.getOpposite());
	}

	/** The container's items, seen from the face it hangs on; null if there's no container there (or it's unloaded). */
	@Nullable
	public IItemHandler container() {
		BlockPos at = containerPos();
		if (!level().isLoaded(at))
			return null;
		return level().getCapability(Capabilities.ItemHandler.BLOCK, at, direction);
	}

	/**
	 * It stays as long as a container is behind it. Unlike an item frame it doesn't mind sharing its space: the gauge
	 * block on the same face is meant to be there.
	 */
	@Override
	public boolean survives() {
		return container() != null || !level().isLoaded(containerPos());
	}

	// ---- its network

	@Nullable
	public UUID frequency() {
		return frequency;
	}

	/** Players may break or tune it if they may use its network (Create's network lock, H5). */
	public boolean mayUse(Player player) {
		return frequency == null || Create.LOGISTICS.mayInteract(frequency, player);
	}

	// ---- breaking, dropping, saving

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && !mayUse(player)) {
			if (!level().isClientSide)
				player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_board.locked"), true);
			return false;
		}
		return super.hurt(source, amount);
	}

	@Override
	public void dropItem(@Nullable Entity breaker) {
		playSound(SoundEvents.ITEM_FRAME_BREAK, 1, 1);
		gameEvent(GameEvent.BLOCK_CHANGE, breaker);
		if (breaker instanceof Player player && player.getAbilities().instabuild)
			return;
		spawnAtLocation(asItem());
	}

	/** The board as an item, still tuned to its network. */
	public ItemStack asItem() {
		ItemStack stack = new ItemStack(ModItems.TRADE_BOARD.get());
		TradeBoardItem.tune(stack, frequency);
		return stack;
	}

	@Override
	public ItemStack getPickResult() {
		return asItem();
	}

	@Override
	public void playPlacementSound() {
		playSound(SoundEvents.ITEM_FRAME_PLACE, 1, 1);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putByte("Facing", (byte) direction.get3DDataValue());
		if (frequency != null)
			tag.putUUID("Freq", frequency);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		frequency = tag.hasUUID("Freq") ? tag.getUUID("Freq") : null;
		setDirection(Direction.from3DDataValue(tag.getByte("Facing")));
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
		return new ClientboundAddEntityPacket(this, direction.get3DDataValue(), getPos());
	}

	@Override
	public void recreateFromPacket(ClientboundAddEntityPacket packet) {
		super.recreateFromPacket(packet);
		setDirection(Direction.from3DDataValue(packet.getData()));
	}

	/**
	 * Clicks go through it while the player holds a Factory Gauge or another board, so gauges can be placed on the face
	 * it hangs on (the gauge block takes the space it hangs in). The gauges, standing proud of it, take their own clicks.
	 */
	@Override
	public boolean isPickable() {
		if (level().isClientSide && TradeBoards.placingOnBoards())
			return false;
		return super.isPickable();
	}

	// ---- the registry of boards by network

	@Override
	public void onAddedToLevel() {
		super.onAddedToLevel();
		if (!level().isClientSide)
			TradeBoards.add(this);
	}

	@Override
	public void onRemovedFromLevel() {
		super.onRemovedFromLevel();
		if (!level().isClientSide)
			TradeBoards.remove(this);
	}
}
