package com.comedymonkey.createaddons.content.juggernaut.ruin;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.juggernaut.ClockworkJuggernaut;
import com.comedymonkey.createaddons.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * The hall at the middle of a Juggernaut Ruin (structure/juggernaut_ruin{,_end}.nbt): broken walls round a dais where a
 * dormant Juggernaut kneels facing the doorway. Its floor sits in the ground (in the End three blocks deeper, half-sunk),
 * and wherever the ground falls away under it the footings go down to meet it, so it never stands on air.
 */
public class RuinHall extends TemplateStructurePiece {
	private static final BlockPos PIVOT = new BlockPos(7, 0, 7);
	private final boolean end;

	public RuinHall(StructureTemplateManager templates, BlockPos centre, Rotation rotation, boolean end) {
		super(ModStructures.RUIN_HALL.get(), 0, templates, template(end), template(end).toString(), settings(rotation),
			centre.offset(-PIVOT.getX(), end ? -4 : -1, -PIVOT.getZ()));
		this.end = end;
	}

	public RuinHall(StructureTemplateManager templates, CompoundTag tag) {
		super(ModStructures.RUIN_HALL.get(), tag, templates, id -> settings(Rotation.valueOf(tag.getString("Rot"))));
		this.end = tag.getBoolean("End");
	}

	private static ResourceLocation template(boolean end) {
		return ComedyMonkeysCreateAddons.asResource(end ? "juggernaut_ruin_end" : "juggernaut_ruin");
	}

	private static StructurePlaceSettings settings(Rotation rotation) {
		return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(PIVOT);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", placeSettings.getRotation().name());
		tag.putBoolean("End", end);
	}

	@Override
	protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
							BoundingBox chunkBox, ChunkPos chunk, BlockPos pivot) {
		super.postProcess(level, structures, generator, random, chunkBox, chunk, pivot);
		// on a hillside, the slope above the hall opened up: nothing left over its walls but sky
		BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
		for (int x = Math.max(boundingBox.minX() + 1, chunkBox.minX()); x <= Math.min(boundingBox.maxX() - 1, chunkBox.maxX()); x++)
			for (int z = Math.max(boundingBox.minZ() + 1, chunkBox.minZ()); z <= Math.min(boundingBox.maxZ() - 1, chunkBox.maxZ()); z++)
				for (int y = templatePosition.getY() + 10; y <= templatePosition.getY() + 32; y++) {
					BlockState above = level.getBlockState(q.set(x, y, z));
					if (!above.isAir() && !above.is(net.minecraft.tags.BlockTags.LEAVES) && !above.is(net.minecraft.tags.BlockTags.LOGS)
						&& !above.is(ClockworkJuggernaut.RUIN_BLOCKS) && !above.is(ClockworkJuggernaut.BREAKS_OUT) && !above.is(Blocks.PURPUR_PILLAR))
						level.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
				}
		// footings: under every floor block, down to the ground
		BlockState footing = end ? Blocks.END_STONE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
		int floor = templatePosition.getY();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = Math.max(boundingBox.minX(), chunkBox.minX()); x <= Math.min(boundingBox.maxX(), chunkBox.maxX()); x++)
			for (int z = Math.max(boundingBox.minZ(), chunkBox.minZ()); z <= Math.min(boundingBox.maxZ(), chunkBox.maxZ()); z++) {
				if (level.getBlockState(p.set(x, floor, z)).isAir())
					continue;
				for (int y = floor - 1; y > floor - 12 && y > level.getMinBuildHeight(); y--) {
					BlockState below = level.getBlockState(p.set(x, y, z));
					if (!below.isAir() && below.getFluidState().isEmpty() && !below.canBeReplaced())
						break;
					level.setBlock(p, footing, 2);
				}
			}
	}
}
