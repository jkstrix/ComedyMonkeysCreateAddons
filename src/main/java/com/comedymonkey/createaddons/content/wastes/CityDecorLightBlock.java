package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A building block of the city whose glow lights while its district is fed, as the Seamed Plate's seams do (D75: the
 * Slotted Grille's dim pane, the Band's seam, a cracked Scarred Plate's crack): a {@link DerelictLightBlockEntity} keeps
 * its district and sets {@link #LIT}, whose emissive part is in its block model. One a player places belongs to no city
 * and stays dark. It casts no light (D63: lit decor casts none). Subclasses add their own states to
 * {@link #createBlockStateDefinition} and start {@link #LIT} false.
 */
public abstract class CityDecorLightBlock extends Block implements EntityBlock, DerelictLightBlockEntity.DerelictLight {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	protected CityDecorLightBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public boolean lightable(BlockState state) {
		return true;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof DerelictLightBlockEntity be)
			be.scheduledTick();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DerelictLightBlockEntity(pos, state);
	}
}
