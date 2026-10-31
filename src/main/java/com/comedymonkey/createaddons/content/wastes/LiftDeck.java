package com.comedymonkey.createaddons.content.wastes;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.mixin.ServerGamePacketListenerImplAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * A Lift's deck (design/specs/clockwork-wastes.md, P49, D40): the 2 x 2 void-alloy platform that rides the shaft. It's
 * an entity so it can be stood on anywhere along the shaft: solid underfoot (its own box, a block's eighth thick), its
 * guard rails on the two closed sides and its control post solid too ({@link Part}s). It never moves itself: every tick,
 * on the server and on each client alike, it goes where its drive ({@link LiftBlockEntity#deckY}) says, and carries
 * everything standing on it by the same step (the client carries its own player, as players move themselves; the
 * server carries mobs, items and the rest). On the server it also checks the step first: a block in its path, a rider
 * that would hit a block, or anything underneath it on the way down stops it where it is ("stop if anything is in the
 * way", P49). Its drive draws it (Flywheel parts); the deck itself has no renderer. Never saved: the drive makes a new
 * one when it loads.
 */
public class LiftDeck extends Entity {
	private static final EntityDataAccessor<BlockPos> DRIVE = SynchedEntityData.defineId(LiftDeck.class, EntityDataSerializers.BLOCK_POS);
	/** How close above (or how deep into) its top an entity's feet must be to stand on it. */
	private static final double ON_TOP_ABOVE = 0.05, ON_TOP_BELOW = 0.1;

	/** The guard rails (closed sides) and the control post (model px, deck coords: the preview's). */
	private static final double[][] PART_BOXES = { { 0, 2, 0, 2, 20, 32 }, { 30, 2, 0, 32, 20, 32 }, { 25, 2, 3, 29, 15, 7 } };
	private final Part[] parts = { new Part(this, false), new Part(this, false), new Part(this, true) };

	public LiftDeck(EntityType<?> type, Level level) {
		super(type, level);
		noPhysics = true;
		// the parts' ids follow the deck's (as the Ender Dragon's), so the client finds the post a player clicks
		setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
	}

	@Override
	public void setId(int id) {
		super.setId(id);
		for (int i = 0; i < parts.length; i++)
			parts[i].setId(id + i + 1);
	}

	void attach(LiftBlockEntity drive) {
		entityData.set(DRIVE, drive.getBlockPos());
	}

	public BlockPos drivePos() {
		return entityData.get(DRIVE);
	}

	@Nullable
	public LiftBlockEntity drive() {
		return level().getBlockEntity(drivePos()) instanceof LiftBlockEntity be ? be : null;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DRIVE, BlockPos.ZERO);
	}

	// ---------- what it is ----------

	@Override
	public boolean isMultipartEntity() {
		return true;
	}

	@Override
	public PartEntity<?>[] getParts() {
		return parts;
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean ignoreExplosion(net.minecraft.world.level.Explosion explosion) {
		return true;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	/** Its slab: the shaft's 2 x 2 at its position's height (its position is the slab's underside, at the shaft's middle). */
	@Override
	protected AABB makeBoundingBox() {
		return new AABB(getX() - 1, getY(), getZ() - 1, getX() + 1, getY() + LiftBlockEntity.DECK, getZ() + 1);
	}

	/** Its position comes from its drive on each side, never from the server's packets. */
	@Override
	public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}

	// ---------- riding ----------

	public double top() {
		return getY() + LiftBlockEntity.DECK;
	}

	/** Something stands on its top (its feet on it, over it). */
	public boolean carries(Entity e) {
		AABB b = e.getBoundingBox(), s = getBoundingBox();
		return b.minY <= top() + ON_TOP_ABOVE && b.minY >= top() - ON_TOP_BELOW
			&& b.maxX > s.minX && b.minX < s.maxX && b.maxZ > s.minZ && b.minZ < s.maxZ;
	}

	private boolean mine(Entity e) {
		return e == this || e instanceof PartEntity<?> p && p.getParent() == this;
	}

	@Override
	public void tick() {
		LiftBlockEntity drive = drive();
		if (drive == null || !level().isClientSide && !drive.owns(this)) {
			if (!level().isClientSide)
				discard();
			return;
		}
		double y = drive.deckY(level().getGameTime());
		double dy = y - getY();
		if (Math.abs(dy) > 1e-9 && !step(drive, dy))
			y = getY();
		setPos(getX(), y, getZ());
		placeParts(drive.facing());
	}

	/**
	 * One tick's step of {@code dy}: on the server, stopped (false) if anything's in the way; otherwise everything on it
	 * goes along. On a client its own player rides it (the server's word on everything else arrives as it always does,
	 * shifted along here so it doesn't trail the deck).
	 */
	private boolean step(LiftBlockEntity drive, double dy) {
		AABB slab = getBoundingBox();
		double oldTop = top(), newTop = oldTop + dy;
		AABB near = slab.inflate(0, 2.5, 0).expandTowards(0, dy, 0);
		List<Entity> moving = new ArrayList<>();
		List<Double> by = new ArrayList<>();
		boolean server = !level().isClientSide;
		for (Entity e : level().getEntities(this, near, e -> !mine(e) && !e.isSpectator() && !e.isPassenger() && !(e instanceof PartEntity<?>))) {
			AABB b = e.getBoundingBox();
			if (!(b.maxX > slab.minX && b.minX < slab.maxX && b.maxZ > slab.minZ && b.minZ < slab.maxZ))
				continue;
			if (carries(e)) {
				moving.add(e);
				by.add(newTop - b.minY);
			} else if (dy > 0 && b.minY < newTop && b.maxY > oldTop) {
				// in the way going up: lifted onto it
				moving.add(e);
				by.add(newTop - b.minY);
			} else if (dy < 0 && server && b.maxY > getY() + dy && b.minY < getY()) {
				// underneath it going down
				return false;
			}
		}
		if (server) {
			// its own path: the slab's sweep, through no block
			AABB sweep = dy > 0 ? new AABB(slab.minX, oldTop, slab.minZ, slab.maxX, newTop, slab.maxZ)
				: new AABB(slab.minX, slab.minY + dy, slab.minZ, slab.maxX, slab.minY, slab.maxZ);
			if (blocked(sweep.deflate(1e-4, 0, 1e-4)))
				return false;
			// riders going up must have room for their heads
			if (dy > 0)
				for (int i = 0; i < moving.size(); i++)
					if (blocked(moving.get(i).getBoundingBox().move(0, by.get(i), 0).deflate(1e-4)))
						return false;
		}
		for (int i = 0; i < moving.size(); i++) {
			Entity e = moving.get(i);
			double step = by.get(i);
			if (step < 0 && blocked(e.getBoundingBox().move(0, step, 0).deflate(1e-4)))
				continue; // half on a landing: it stays there
			carry(e, step);
		}
		return true;
	}

	private boolean blocked(AABB box) {
		for (VoxelShape s : level().getBlockCollisions(null, box))
			if (!s.isEmpty())
				return true;
		return false;
	}

	private void carry(Entity e, double step) {
		if (level().isClientSide) {
			if (e.isControlledByLocalInstance())
				e.setPos(e.getX(), e.getY() + step, e.getZ());
			else {
				// another's entity: the server's word follows; shift it and where it's heading, so it doesn't trail the deck
				double tx = e.lerpTargetX(), ty = e.lerpTargetY(), tz = e.lerpTargetZ();
				e.setPos(e.getX(), e.getY() + step, e.getZ());
				e.lerpTo(tx, ty + step, tz, e.lerpTargetYRot(), e.lerpTargetXRot(), 3);
			}
			return;
		}
		if (e instanceof ServerPlayer player) {
			// a player moves themselves (their client carries them)
			keepAloft(player);
			return;
		}
		e.setPos(e.getX(), e.getY() + step, e.getZ());
		e.resetFallDistance();
		if (e.getDeltaMovement().y < 0)
			e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
		e.setOnGround(true);
	}

	/** A player on a deck is held up: the server stops counting it as floating (it would kick it for flying). */
	public static void keepAloft(ServerPlayer player) {
		if (player.connection != null)
			((ServerGamePacketListenerImplAccessor) player.connection).comedymonkeysaddons$setAboveGroundTickCount(0);
	}

	/** Its guard rails and control post stand on it, turned with its drive. */
	private void placeParts(Direction facing) {
		AABB slab = getBoundingBox();
		for (int i = 0; i < parts.length; i++) {
			double[] b = PART_BOXES[i];
			double[] p = turn(b[0], b[2], facing), q = turn(b[3], b[5], facing);
			parts[i].place(new AABB(slab.minX + Math.min(p[0], q[0]) / 16, getY() + b[1] / 16, slab.minZ + Math.min(p[1], q[1]) / 16,
				slab.minX + Math.max(p[0], q[0]) / 16, getY() + b[4] / 16, slab.minZ + Math.max(p[1], q[1]) / 16));
		}
	}

	/** A deck point (px, x 0-32 and z 0-32 as the preview's) turned with its drive about the deck's middle. */
	static double[] turn(double x, double z, Direction facing) {
		return switch (facing) {
			case EAST -> new double[] { 32 - z, x };
			case SOUTH -> new double[] { 32 - x, 32 - z };
			case WEST -> new double[] { z, 32 - x };
			default -> new double[] { x, z };
		};
	}

	/** A player at its control post. */
	InteractionResult usePost(Player player) {
		if (level().isClientSide)
			return InteractionResult.SUCCESS;
		LiftBlockEntity drive = drive();
		if (drive != null)
			drive.usePost(player);
		return InteractionResult.CONSUME;
	}

	/** A guard rail or the control post: solid, and the post can be used. */
	public static final class Part extends PartEntity<LiftDeck> {
		private final boolean post;
		@Nullable
		private AABB placed;

		Part(LiftDeck deck, boolean post) {
			super(deck);
			this.post = post;
			noPhysics = true;
		}

		void place(AABB box) {
			placed = box;
			setPos(box.getCenter().x, box.minY, box.getCenter().z);
			xo = getX();
			yo = getY();
			zo = getZ();
		}

		@Override
		protected AABB makeBoundingBox() {
			return placed != null ? placed : super.makeBoundingBox();
		}

		@Override
		public boolean canBeCollidedWith() {
			return true;
		}

		@Override
		public boolean isPickable() {
			return post;
		}

		@Override
		public boolean hurt(DamageSource source, float amount) {
			return false;
		}

		@Override
		public InteractionResult interact(Player player, InteractionHand hand) {
			return post && hand == InteractionHand.MAIN_HAND ? getParent().usePost(player) : InteractionResult.PASS;
		}

		@Override
		public boolean is(Entity entity) {
			return this == entity || getParent() == entity;
		}

		@Override
		public boolean shouldBeSaved() {
			return false;
		}

		@Override
		protected void defineSynchedData(SynchedEntityData.Builder builder) {}

		@Override
		protected void readAdditionalSaveData(CompoundTag tag) {}

		@Override
		protected void addAdditionalSaveData(CompoundTag tag) {}
	}
}
