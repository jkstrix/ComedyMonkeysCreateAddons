package com.comedymonkey.createaddons.content.wastes;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Assembly Clamp (D72): the uprising's unlit void-alloy jig on the Workshops' assembly line, holding a Worker at one of
 * five {@link #STAGE}s of being built (1 the armature, 2 the chest cage, 3 the core and upper arms, 4 plated with an empty
 * lens socket, 5 whole with its lens in; the copper harness in 1-3). Salvaged like a Worker Shell ({@link SalvageBlock}:
 * a Wrench for the full salvage, anything else one scrap). Its model is baked to quads as the shells' are
 * (models/block/wastes/assembly_clamp_<stage>.obj: stages 4-5 stand 33.5 px tall, past a block model's reach), so it
 * needs no renderer; it collides as its own block only.
 */
public class AssemblyClampBlock extends SalvageBlock {
	public static final IntegerProperty STAGE = IntegerProperty.create("stage", 1, 5);
	/**
	 * The stage the item places (provisional, for the user's word: progress doc, "D72"): the whole Worker, which its flat icon
	 * shows. The city sets each clamp's own stage.
	 */
	public static final int ITEM_STAGE = 5;

	public AssemblyClampBlock(Properties properties) {
		// the jig's footplate and uprights (x 1-15, z 4-12 facing north), one block tall
		super(properties, new double[][] { { 1, 0, 4, 15, 16, 12 } });
		registerDefaultState(defaultBlockState().setValue(STAGE, ITEM_STAGE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(STAGE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(STAGE, ITEM_STAGE);
	}
}
