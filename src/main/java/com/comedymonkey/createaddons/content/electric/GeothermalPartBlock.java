package com.comedymonkey.createaddons.content.electric;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A Geothermal Casing or the Turbine Core. Formed into a turbine, it's drawn as part of the whole machine by the
 * controller, so it hides its own model. Casings keep a note of their controller, so pipes and energy pipes on any
 * casing reach it.
 */
public class GeothermalPartBlock extends Block implements EntityBlock {
	public static final BooleanProperty FORMED = BooleanProperty.create("formed");
	private final boolean casing;

	public GeothermalPartBlock(Properties properties, boolean casing) {
		super(properties);
		this.casing = casing;
		registerDefaultState(defaultBlockState().setValue(FORMED, false));
	}

	public boolean isCasing() {
		return casing;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return casing ? new GeothermalCasingBlockEntity(pos, state) : null;
	}

	/** Losing a part breaks the turbine; its controller notices on its next check. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && state.getValue(FORMED) && level.getBlockEntity(pos) instanceof GeothermalCasingBlockEntity part
			&& part.getController() != null && level.getBlockEntity(part.getController()) instanceof GeothermalControllerBlockEntity controller)
			controller.queueCheck();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** For the core, which has no block entity: its controller re-checks every second anyway. */
	public static boolean isPart(BlockState state, boolean casing) {
		return state.getBlock() instanceof GeothermalPartBlock part && part.casing == casing;
	}

	static net.minecraft.world.level.block.entity.BlockEntityType<?> casingType() {
		return ModBlockEntities.GEOTHERMAL_CASING.get();
	}
}
