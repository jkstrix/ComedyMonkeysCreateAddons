package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Conduit Pylon (design/specs/clockwork-wastes.md, P32, P33, P36): a narrow void-alloy obelisk three blocks tall with
 * a crown of six fins round a starlight core, one in each corner of the heart hall, shielding the Custodian. It can't
 * be mined, blown up or touched by contraptions; its coil breaks after six hits from players' weapons or projectiles
 * (one fin snaps a hit), and only once the Custodian is awake. The count and the API for the fight are on its
 * {@link ConduitPylonBlockEntity} (in its bottom block).
 */
public class ConduitPylonBlock extends ThreePartBlock implements EntityBlock {
	private static final double[][][] BOXES = {
		{ { 3, 0, 3, 13, 3.5, 13 }, { 5, 3.5, 5, 11, 16, 11 } },
		{ { 5.5, 0, 5.5, 10.5, 16, 10.5 } },
		{ { 6, 0, 6, 10, 8, 10 }, { 4, 8, 4, 12, 16, 12 } } };

	public ConduitPylonBlock(Properties properties) {
		super(properties, false, BOXES);
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return simpleCodec(ConduitPylonBlock::new);
	}

	@Nullable
	public static ConduitPylonBlockEntity pylonAt(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ConduitPylonBlock block))
			return null;
		return level.getBlockEntity(block.bottom(state, pos)) instanceof ConduitPylonBlockEntity be ? be : null;
	}

	/** A player's swing at any part of it: a hit on its coil (P33). */
	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		if (!level.isClientSide) {
			ConduitPylonBlockEntity be = pylonAt(level, pos);
			if (be != null)
				be.hit(player);
		}
	}

	/** An arrow, trident or any other projectile: a hit, as its shooter's. */
	@Override
	protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
		if (!level.isClientSide) {
			ConduitPylonBlockEntity be = pylonAt(level, hit.getBlockPos());
			if (be != null)
				be.hit(projectile.getOwner());
		}
	}

	/** Never mined (P33): only hits break its coil. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return 0;
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		return CityLocks.LOCKED_RESISTANCE;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof ConduitPylonBlockEntity be)
			be.scheduledTick();
	}

	/** A failing coil sparks (the brief: every second or so). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(PART) != Part.TOP || random.nextInt(3) != 0)
			return;
		ConduitPylonBlockEntity be = pylonAt(level, pos);
		if (be == null || !be.isStanding() || !be.isFailing())
			return;
		for (int i = 0; i < 3; i++)
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
				pos.getY() + 0.75 + random.nextDouble() * 0.2, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
				(random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.2);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == Part.BOTTOM ? new ConduitPylonBlockEntity(pos, state) : null;
	}
}
