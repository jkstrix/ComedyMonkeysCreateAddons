package com.comedymonkey.createaddons.content.electric;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Electric Motor: turns Forge Energy into rotation. Its speed, stress capacity and direction are set from its screen
 * (right-click with a wrench), and it draws more power the more stress it's set to provide. The shaft comes out of {@code facing}.
 */
public class ElectricMotorBlock extends DirectionalKineticBlock implements IBE<ElectricMotorBlockEntity> {
	/** Running: the coil glows. */
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(0.45f, 0.85f, 0.95f), 0.6f);

	public ElectricMotorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(POWERED));
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	/** A wrench opens the motor's settings (instead of turning it); sneaking with one still picks it up. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (context.getLevel().isClientSide)
			com.comedymonkey.createaddons.client.ElectricMotorScreen.open(context.getClickedPos());
		return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
	}

	/** Now and then a spark off the coil while it runs. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(POWERED) || random.nextInt(5) != 0)
			return;
		Vec3 at = Vec3.atCenterOf(pos).add((random.nextDouble() - 0.5) * 0.9, (random.nextDouble() - 0.5) * 0.9,
			(random.nextDouble() - 0.5) * 0.9);
		level.addParticle(SPARK, at.x, at.y, at.z, 0, 0, 0);
	}

	@Override
	public Class<ElectricMotorBlockEntity> getBlockEntityClass() {
		return ElectricMotorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ElectricMotorBlockEntity> getBlockEntityType() {
		return ModBlockEntities.ELECTRIC_MOTOR.get();
	}
}
