package com.comedymonkey.createaddons.content.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * An Edge Light, placed on the edge nearest where you click (EdgeLightBlock.edgeFor): a new block of Edge Light there, or
 * another edge in one that's already there.
 */
public class EdgeLightItem extends BlockItem {
	public EdgeLightItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockPlaceContext ctx = updatePlacementContext(new BlockPlaceContext(context));
		if (ctx == null)
			return InteractionResult.FAIL;
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Direction[] edge = EdgeLightBlock.edgeFor(ctx);
		if (edge == null)
			return InteractionResult.FAIL;
		// another edge in a block of Edge Light that's there (placing the same block over itself would change nothing)
		if (level.getBlockEntity(pos) instanceof EdgeLightBlockEntity be) {
			if (EdgeLinks.has(be.edges(), edge[0], edge[1]))
				return InteractionResult.FAIL;
			if (!level.isClientSide) {
				Player player = ctx.getPlayer();
				// as a placement, so protection mods can refuse it
				if (player != null && net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,
					net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos), ctx.getClickedFace()))
					return InteractionResult.FAIL;
				be.addEdge(edge[0], edge[1]);
				level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE, pos);
				if (player == null || !player.getAbilities().instabuild)
					ctx.getItemInHand().shrink(1);
				SoundType sound = be.getBlockState().getSoundType(level, pos, player);
				level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		InteractionResult placed = place(ctx);
		if (placed.consumesAction() && !level.isClientSide && level.getBlockEntity(pos) instanceof EdgeLightBlockEntity be)
			be.addEdge(edge[0], edge[1]);
		return placed;
	}
}
