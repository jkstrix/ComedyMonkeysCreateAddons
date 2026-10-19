package com.comedymonkey.createaddons.compat.pipez.cover;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.network.PipeCoverPayload;
import com.comedymonkey.createaddons.registry.ModAttachments;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;

import de.maxhenkel.pipez.blocks.PipeBlock;
import de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Fitting and dressing Pipez Covers, like Create's copycats:
 * <ul>
 * <li>right-click a pipe with a cover to fit it (or swap it for the other kind);</li>
 * <li>right-click a covered pipe with a full block to dress the cover in it (one is used, and the last one back);</li>
 * <li>sneak and right-click with a wrench to take the block off, then the cover;</li>
 * <li>breaking the pipe drops the cover and its block.</li>
 * </ul>
 * Only registered when Pipez is installed.
 */
public final class PipeCoverEvents {
	private static final TagKey<Item> WRENCHES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

	private PipeCoverEvents() {}

	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		if (!(level.getBlockState(pos).getBlock() instanceof PipeBlock pipeBlock))
			return;
		Player player = event.getEntity();
		ItemStack held = event.getItemStack();
		// A plain run of pipe has no block entity until it's configured (or covered)
		PipeCover cover = PipeCoverShapes.cover(level, pos);
		Runnable action = null;

		if (held.getItem() instanceof PipeCoverItem item) {
			if (cover == null || cover.kind() != item.kind())
				action = () -> {
					// Swapping kinds keeps the block and hands back the other cover
					if (cover != null)
						give(player, new ItemStack(itemFor(cover.kind())));
					if (!(level.getBlockEntity(pos) instanceof PipeTileEntity))
						pipeBlock.setHasData(level, pos, true);
					if (!(level.getBlockEntity(pos) instanceof PipeTileEntity pipe))
						return;
					set(pipe, new PipeCover(item.kind(), cover == null ? Blocks.AIR.defaultBlockState() : cover.material()));
					consume(player, held);
				};
		} else if (cover != null && player.isShiftKeyDown() && held.is(WRENCHES)) {
			PipeTileEntity pipe = (PipeTileEntity) level.getBlockEntity(pos);
			action = () -> {
				if (cover.hasMaterial()) {
					give(player, new ItemStack(cover.material().getBlock()));
					set(pipe, cover.withMaterial(Blocks.AIR.defaultBlockState()));
				} else {
					give(player, new ItemStack(itemFor(cover.kind())));
					set(pipe, null);
					// Back to a plain pipe if nothing else needs the block entity
					if (!pipe.hasReasonToStay())
						pipeBlock.setHasData(level, pos, false);
				}
			};
		} else if (cover != null && held.getItem() instanceof BlockItem blockItem) {
			PipeTileEntity pipe = (PipeTileEntity) level.getBlockEntity(pos);
			BlockState material = materialFor(blockItem.getBlock(), level, pos);
			if (material != null && material != cover.material())
				action = () -> {
					if (cover.hasMaterial())
						give(player, new ItemStack(cover.material().getBlock()));
					set(pipe, cover.withMaterial(material));
					consume(player, held);
				};
		}

		if (action == null)
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
		if (level.isClientSide)
			return;
		action.run();
		level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.8f, 1.1f);
	}

	/** A block that can dress a cover: a plain full block, like Create's copycats take. */
	@Nullable
	private static BlockState materialFor(Block block, Level level, BlockPos pos) {
		if (block instanceof EntityBlock || block instanceof PipeBlock || block == ModBlocks.PIPE_COVER_BLANK.get())
			return null;
		BlockState state = block.defaultBlockState();
		if (state.getRenderShape() != RenderShape.MODEL || !state.isCollisionShapeFullBlock(level, pos))
			return null;
		return state;
	}

	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (!(event.getLevel() instanceof Level level) || !(level.getBlockEntity(event.getPos()) instanceof PipeTileEntity pipe)
			|| !pipe.hasData(ModAttachments.PIPE_COVER) || event.getPlayer().isCreative())
			return;
		PipeCover cover = pipe.getData(ModAttachments.PIPE_COVER);
		Block.popResource(level, event.getPos(), new ItemStack(itemFor(cover.kind())));
		if (cover.hasMaterial())
			Block.popResource(level, event.getPos(), new ItemStack(cover.material().getBlock()));
	}

	/** Covers in a chunk go to a player along with it. */
	public static void onChunkSent(ChunkWatchEvent.Sent event) {
		for (BlockEntity be : event.getChunk().getBlockEntities().values())
			if (be instanceof PipeTileEntity && be.hasData(ModAttachments.PIPE_COVER))
				PacketDistributor.sendToPlayer(event.getPlayer(),
					new PipeCoverPayload(be.getBlockPos(), Optional.of(be.getData(ModAttachments.PIPE_COVER))));
	}

	/** Sends the pipe's cover again, e.g. when it starts or stops extracting on a side and the snug cover changes shape. */
	public static void resync(Level level, BlockPos pos) {
		if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof PipeTileEntity pipe
			&& pipe.hasData(ModAttachments.PIPE_COVER))
			send(server, pos, pipe.getData(ModAttachments.PIPE_COVER));
	}

	private static void set(PipeTileEntity pipe, @Nullable PipeCover cover) {
		if (cover == null)
			pipe.removeData(ModAttachments.PIPE_COVER);
		else
			pipe.setData(ModAttachments.PIPE_COVER, cover);
		pipe.setChanged();
		if (pipe.getLevel() instanceof ServerLevel server)
			send(server, pipe.getBlockPos(), cover);
	}

	/**
	 * Sent a tick later: a newly made block entity reaches clients with the block change at the end of this tick, and
	 * the cover has to arrive after it.
	 */
	private static void send(ServerLevel level, BlockPos pos, @Nullable PipeCover cover) {
		PipeCoverPayload payload = new PipeCoverPayload(pos.immutable(), Optional.ofNullable(cover));
		var server = level.getServer();
		server.tell(new TickTask(server.getTickCount() + 1,
			() -> PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(payload.pos()), payload)));
	}

	private static Item itemFor(PipeCover.Kind kind) {
		return kind == PipeCover.Kind.FULL ? ModItems.FULL_PIPEZ_COVER.get() : ModItems.SNUG_PIPEZ_COVER.get();
	}

	private static void consume(Player player, ItemStack held) {
		if (!player.getAbilities().instabuild)
			held.shrink(1);
	}

	private static void give(Player player, ItemStack stack) {
		if (!player.getAbilities().instabuild && !player.getInventory().add(stack))
			player.drop(stack, false);
	}
}
