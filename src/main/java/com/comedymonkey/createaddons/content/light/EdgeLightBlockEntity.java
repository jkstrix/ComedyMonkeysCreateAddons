package com.comedymonkey.createaddons.content.light;

import java.util.List;

import com.comedymonkey.createaddons.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block of Edge Light: the edges it has trim on (EdgeLinks), and a piece of a run of edges joined to the same edges of
 * the blocks along. Its edges are kept here, not in its block state (24 of them would be far too many states); its model
 * reads them, so the client is told whenever they change.
 */
public class EdgeLightBlockEntity extends RunLightBlockEntity {
	private int edges;

	public EdgeLightBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.EDGE_LIGHT.get(), pos, state);
	}

	public int edges() {
		return edges;
	}

	@Override
	protected List<BlockPos> joined(BlockPos p, BlockState state) {
		return EdgeLinks.joined(level, p, EdgeLinks.edgesAt(level, p));
	}

	/** Trim on another edge: the run it's in, and the wires round the block it's fixed to, are worked out again. */
	public void addEdge(Direction face, Direction side) {
		setEdges(edges | EdgeLinks.bit(face, side));
	}

	/** The edges lying against face come off (their block went): how many there were. */
	public int removeEdgesOn(Direction face) {
		int before = edges, after = edges;
		for (Direction side : com.comedymonkey.createaddons.content.line.WireLink.across(face))
			after &= ~EdgeLinks.bit(face, side);
		setEdges(after);
		return Integer.bitCount(before) - Integer.bitCount(after);
	}

	private void setEdges(int next) {
		if (next == edges || level == null)
			return;
		edges = next;
		RunLightBlockEntity.runsChanged();
		if (!level.isClientSide) {
			setChanged();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
			LightMounts.relinkBehind(level, getBlockState(), worldPosition);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("Edges", edges);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		edges = tag.getInt("Edges");
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		tag.putInt("Edges", edges);
		return tag;
	}

	/** On the client: new edges are drawn (the model reads them), and the blocks round about join to them afresh. */
	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		super.handleUpdateTag(tag, registries);
		int next = tag.getInt("Edges");
		if (next != edges) {
			edges = next;
			RunLightBlockEntity.runsChanged();
			requestModelDataUpdate();
			if (level != null)
				level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
		}
	}
}
