package com.comedymonkey.createaddons.content.line;

import java.util.List;

import com.comedymonkey.createaddons.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A Fluid Line block's own state (see {@link LineBlockEntity}), and for the sight slot the fluid the line is moving
 * right now and which way it runs through this block. The fluid itself never stays in the line: {@link FluidLines}
 * moves it from the Intakes straight into the tanks.
 */
public class FluidLineBlockEntity extends LineBlockEntity {
	/**
	 * What the sight slot shows: the fluid the piece stands full of (empty when none can reach it), whether it's moving,
	 * and the sides it flows out of.
	 */
	private FluidStack flowing = FluidStack.EMPTY;
	private boolean moving;
	private int downstream;
	/** Whether the fluid is creeping into this block or running out of it, and since which game tick. */
	public static final int FULL = 0, FILLING = 1, DRAINING = 2;
	private int mode;
	private long since;
	/** Client only: how far the fluid has slid along the slot, in pixels, and the time it was last moved on. */
	private float slid, slidAt = Float.NaN;

	public FluidLineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUID_LINE.get(), pos, state);
	}

	@Override
	public LineTier tier() {
		return getBlockState().getBlock() instanceof FluidLineBlock line ? line.tier() : LineTier.COPPER;
	}

	@Override
	public int rate() {
		return tier().fluidPerTick();
	}

	/** What the piece stands full of (moving or not). */
	public FluidStack flowing() {
		return flowing;
	}

	/** The fluid in it is moving (it slides along the slot), rather than standing still. */
	public boolean moving() {
		return moving;
	}

	/**
	 * How far the fluid has come through this block, 0 to 1: while filling, how far its front has crept in from the
	 * upstream side; while draining, how far its tail has crept out (what's left is the downstream part). Full: 1.
	 */
	public float progress(float time) {
		if (mode == FULL)
			return 1;
		return net.minecraft.util.Mth.clamp((time - since) / tier().drainTicksPerBlock(), 0, 1);
	}

	public boolean draining() {
		return mode == DRAINING;
	}

	/**
	 * Client only: how far the fluid has slid along the slot, in pixels, at {@code time} (ticks). It only moves on while
	 * the fluid is moving, from where it stopped, so starting and stopping never makes it jump.
	 */
	public float slid(float time) {
		if (!Float.isNaN(slidAt) && moving)
			slid = (slid + net.minecraft.util.Mth.clamp(time - slidAt, 0, 20) * tier().slotSpeed() / 20f) % 16f;
		slidAt = time;
		return slid;
	}

	/** The fluid flows out of this block through side d (towards the tanks), rather than in. */
	public boolean flowsOut(Direction d) {
		return (downstream & (1 << d.ordinal())) != 0;
	}

	/** Fits an Intake on side d (the side facing the tank it will draw from). */
	public void setIntake(Direction d) {
		setFitting(Fitting.INTAKE, d);
	}

	@Override
	protected void refreshState() {
		if (level != null)
			FluidLineBlock.refresh(level, worldPosition);
	}

	@Override
	protected void networkChanged(ServerLevel level) {
		FluidLines lines = FluidLines.of(level);
		lines.changed(worldPosition);
		// showing fluid from the save, or from a network now gone: kept only if a network has it after its next tick
		if (!flowing.isEmpty())
			lines.recheck(worldPosition);
		if (fitting() == Fitting.INTAKE)
			lines.addIntake(worldPosition);
	}

	/** Set by the line's network as the piece fills, empties, starts or stops moving, or changes way; drawn by the renderer. */
	void setFlow(FluidStack fluid, int out, boolean moving, int mode, long since) {
		FluidStack next = fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1);
		moving &= !next.isEmpty();
		if (FluidStack.isSameFluidSameComponents(next, flowing) && next.isEmpty() == flowing.isEmpty() && out == downstream && moving == this.moving
			&& mode == this.mode && since == this.since)
			return;
		flowing = next;
		downstream = out;
		this.moving = moving;
		this.mode = mode;
		this.since = since;
		sync();
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		// saved too, so a line that was full is still full when the world loads (no slow refill from empty)
		flowing = tag.contains("Flowing") ? FluidStack.parseOptional(registries, tag.getCompound("Flowing")) : FluidStack.EMPTY;
		downstream = tag.getInt("Downstream");
		moving = tag.getBoolean("Moving");
		mode = tag.getInt("FillMode");
		since = tag.getLong("FillSince");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		writeClient(tag, registries);
	}

	@Override
	protected void writeClient(CompoundTag tag, HolderLookup.Provider registries) {
		if (!flowing.isEmpty()) {
			tag.put("Flowing", flowing.save(registries));
			tag.putInt("Downstream", downstream);
			tag.putBoolean("Moving", moving);
			if (mode != FULL) {
				tag.putInt("FillMode", mode);
				tag.putLong("FillSince", since);
			}
		}
	}

	@Override
	protected void addLineTooltip(List<Component> tooltip) {
		line(tooltip, Component.translatable(key("fluid_line_capacity"), rate()));
		line(tooltip, flowing.isEmpty() ? Component.translatable(key("fluid_line_still"))
			: Component.translatable(key(moving ? "fluid_line_moving" : "fluid_line_full"), flowing.getHoverName()));
	}

	@Override
	protected String meterUnitKey() {
		return "meter_fluid";
	}
}
