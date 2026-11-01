package com.comedymonkey.createaddons.content.juggernaut.ruin;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModStructures;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Builds the hall from the ruin's own blocks ({@link RuinBlocks}): every block of its template that has a ruin copy is
 * placed as the copy, its shape and facing kept, except the floor (the template's bottom layer), which is the ground.
 */
public final class RuinBlocksProcessor extends StructureProcessor {
	public static final RuinBlocksProcessor INSTANCE = new RuinBlocksProcessor();
	public static final MapCodec<RuinBlocksProcessor> CODEC = MapCodec.unit(INSTANCE);

	private RuinBlocksProcessor() {}

	@Nullable
	@Override
	@SuppressWarnings("deprecation") // override-only API: overriding it is the intended use
	public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
															 StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current, StructurePlaceSettings settings) {
		if (original.pos().getY() == 0)
			return current;
		return new StructureTemplate.StructureBlockInfo(current.pos(), RuinBlocks.ruin(current.state()), current.nbt());
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return ModStructures.RUIN_BLOCKS_PROCESSOR.get();
	}
}
