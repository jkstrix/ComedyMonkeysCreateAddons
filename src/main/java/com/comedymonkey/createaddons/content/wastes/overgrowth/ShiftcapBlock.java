package com.comedymonkey.createaddons.content.wastes.overgrowth;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Shiftcap (D92, D94): a cluster of glowing fungi whose glow shifts from blue to plum as they age (young blue, mature blue
 * gills under a plum lip, old plum), never through the uprising's violet. Light 3; bone meal ages it, an old one sets a
 * young one nearby. Brushing through it puffs a few spores (G9).
 */
public final class ShiftcapBlock extends OvergrowthPlantBlock {
	public static final MapCodec<ShiftcapBlock> CODEC = simpleCodec(ShiftcapBlock::new);
	public static final EnumProperty<Age> AGE = EnumProperty.create("age", Age.class);
	/** The spores: the glow ramps' edge tones (blue #4f6bb5, plum #8a4fa0). */
	static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0x4f / 255f, 0x6b / 255f, 0xb5 / 255f), 0.6f),
		PLUM = new DustParticleOptions(new Vector3f(0x8a / 255f, 0x4f / 255f, 0xa0 / 255f), 0.6f);

	public ShiftcapBlock(Properties properties) {
		super(properties, Block.box(2, 0, 2, 14, 12, 14));
		registerDefaultState(defaultBlockState().setValue(AGE, Age.YOUNG));
	}

	@Override
	protected MapCodec<ShiftcapBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	@Nullable
	protected BlockState grown(BlockState state) {
		Age a = state.getValue(AGE);
		return a == Age.OLD ? null : state.setValue(AGE, Age.values()[a.ordinal() + 1]);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide && (entity.xOld != entity.getX() || entity.zOld != entity.getZ()) && level.random.nextInt(3) == 0)
			spores(state, level, pos, level.random, 2);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(AGE) == Age.OLD && random.nextInt(40) == 0)
			spores(state, level, pos, random, 1);
	}

	/** A few motes drifting up off the caps, blue or plum by its age. */
	static void spores(BlockState state, Level level, BlockPos pos, RandomSource random, int n) {
		Age a = state.getValue(AGE);
		for (int i = 0; i < n; i++) {
			boolean plum = a == Age.OLD || a == Age.MATURE && random.nextBoolean();
			level.addParticle(plum ? PLUM : BLUE, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.4 + random.nextDouble() * 0.4,
				pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
		}
	}

	/** How far its glow has shifted. */
	public enum Age implements StringRepresentable {
		YOUNG, MATURE, OLD;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
