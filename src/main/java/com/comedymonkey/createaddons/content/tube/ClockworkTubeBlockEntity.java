package com.comedymonkey.createaddons.content.tube;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Clockwork Tube's one load (if it has one), the joints shut on it with a wrench, and why any capped joint is capped.
 * Tubes don't tick: their network's Receiver moves the loads, and the client glides each one on from its last update.
 */
public class ClockworkTubeBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation,
	com.simibubi.create.api.contraption.transformable.TransformableBlockEntity {
	@Nullable
	private TubeLoad load;
	/** Joints shut with a wrench, one bit a side ({@link Direction#get3DDataValue}). */
	private int shut;
	/** Why each capped joint is capped (synced for the goggles). */
	private final Map<Direction, TubeJoints.Refusal> refusals = new EnumMap<>(Direction.class);
	/** Server: the network version its capped joints were last looked at for. */
	private int checked = -1;

	public ClockworkTubeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CLOCKWORK_TUBE.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Nullable
	public TubeLoad getLoad() {
		return load;
	}

	/** A load moved in (or out, with null): saved and sent to the client. */
	public void setLoad(@Nullable TubeLoad load) {
		this.load = load;
		setChanged();
		sendData();
	}

	/**
	 * Server: its load moved on into {@code next}. Saved but not sent: the client clears it itself when the next tube's
	 * update arrives (one packet a hop, not two), unless the two are in different chunks (a player may see only one).
	 */
	public void loadMovedOn(BlockPos next) {
		load = null;
		setChanged();
		if ((next.getX() >> 4) != (worldPosition.getX() >> 4) || (next.getZ() >> 4) != (worldPosition.getZ() >> 4))
			sendData();
	}

	/** Server: its load stopped at the far face to wait. Saved but not sent: the client stops it there by itself. */
	public void loadWaiting() {
		setChanged();
	}

	/** Client only (Ponder): shows a load without telling anyone. */
	public void showLoad(@Nullable TubeLoad load) {
		this.load = load;
	}

	/** The load changed where it is without moving tube (waiting at the face, sped up): sent so the client follows. */
	public void loadChanged() {
		setChanged();
		sendData();
	}

	public boolean isShut(Direction side) {
		return (shut & (1 << side.get3DDataValue())) != 0;
	}

	void setShut(Direction side, boolean value) {
		int bit = 1 << side.get3DDataValue();
		int next = value ? shut | bit : shut & ~bit;
		if (next != shut) {
			shut = next;
			setChanged();
		}
	}

	/** One side's reason, as worked out when that side changed. */
	void setRefusal(Direction side, TubeJoints.Refusal why) {
		if (why == null || why == TubeJoints.Refusal.NONE ? refusals.remove(side) != null : refusals.put(side, why) != why) {
			setChanged();
			sendData();
		}
	}

	/** Its chunk loaded: Receivers read their networks again (a stretch they couldn't see may be here now). */
	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide)
			TubeJoints.changed(level, worldPosition);
	}

	/**
	 * About once a second, if anything changed since: a joint capped for a reason that may have gone (a Receiver broken,
	 * the network shortened), or capped with no reason known yet (just placed), is worked out again.
	 */
	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide || Math.floorMod(worldPosition.hashCode() + level.getGameTime(), 20) != 0)
			return;
		int version = TubeJoints.version(level);
		if (version == checked)
			return;
		checked = version;
		BlockState state = getBlockState();
		boolean recheck = false;
		for (Direction side : Direction.values())
			if (state.getValue(ClockworkTubeBlock.JOINTS.get(side)) == TubeJoint.CAPPED && refusals.get(side) != TubeJoints.Refusal.SHUT)
				recheck = true;
		if (!recheck)
			return;
		BlockState next = ClockworkTubeBlock.rejoin(state, level, worldPosition);
		if (next != state)
			level.setBlock(worldPosition, next, 3);
	}

	/** Turned or mirrored with a contraption or schematic: its shut joints, reasons and load turn with it. */
	@Override
	public void transform(net.minecraft.world.level.block.entity.BlockEntity be, com.simibubi.create.content.contraptions.StructureTransform transform) {
		java.util.function.UnaryOperator<Direction> turn = d -> transform.rotateFacing(transform.mirrorFacing(d));
		int turned = 0;
		for (Direction side : Direction.values())
			if (isShut(side))
				turned |= 1 << turn.apply(side).get3DDataValue();
		shut = turned;
		Map<Direction, TubeJoints.Refusal> was = new EnumMap<>(refusals);
		refusals.clear();
		was.forEach((side, r) -> refusals.put(turn.apply(side), r));
		if (load != null) {
			load.from = turn.apply(load.from);
			load.to = turn.apply(load.to);
		}
		setChanged();
	}

	void setRefusals(Map<Direction, TubeJoints.Refusal> why) {
		Map<Direction, TubeJoints.Refusal> next = new EnumMap<>(Direction.class);
		why.forEach((side, r) -> {
			if (r != TubeJoints.Refusal.NONE)
				next.put(side, r);
		});
		if (!next.equals(refusals)) {
			refusals.clear();
			refusals.putAll(next);
			setChanged();
			sendData();
		}
	}

	// ---------- goggles and saving ----------

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.clockwork_tube").forGoggles(tooltip);
		if (load != null)
			lang().translate("gui.goggles.clockwork_tube.carrying", load.stack.getHoverName(), load.stack.getCount())
				.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		for (Map.Entry<Direction, TubeJoints.Refusal> e : refusals.entrySet())
			lang().translate("gui.goggles.clockwork_tube.capped." + e.getValue().name().toLowerCase(java.util.Locale.ROOT),
					Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".direction." + e.getKey().getSerializedName()))
				.style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (load != null)
			tag.put("Load", load.write(registries));
		if (!clientPacket && shut != 0)
			tag.putByte("Shut", (byte) shut);
		int r = 0;
		for (Map.Entry<Direction, TubeJoints.Refusal> e : refusals.entrySet())
			r |= e.getValue().ordinal() << (e.getKey().get3DDataValue() * 2);
		if (r != 0)
			tag.putInt("Refusals", r);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		TubeLoad was = load;
		load = tag.contains("Load") ? TubeLoad.read(tag.getCompound("Load"), registries) : null;
		// a load came in: the tube it came from has passed it on (its own update isn't sent: loadMovedOn)
		if (clientPacket && load != null && level != null && (was == null || was.since != load.since)
			&& level.getBlockEntity(worldPosition.relative(load.from)) instanceof ClockworkTubeBlockEntity behind
			&& behind.load != null && behind.load.to == load.from.getOpposite()
			// (the one at its far face: a load that only just came into it, in the same tick, stays)
			&& behind.load.progressAt(level.getGameTime()) > 0.5f)
			behind.load = null;
		if (!clientPacket)
			shut = tag.getByte("Shut");
		refusals.clear();
		int r = tag.getInt("Refusals");
		for (Direction side : Direction.values()) {
			int v = (r >> (side.get3DDataValue() * 2)) & 3;
			if (v != 0)
				refusals.put(side, TubeJoints.Refusal.values()[v]);
		}
	}
}
