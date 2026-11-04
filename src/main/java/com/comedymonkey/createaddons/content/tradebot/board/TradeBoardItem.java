package com.comedymonkey.createaddons.content.tradebot.board;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The Trade Board as an item (H3, D41). Tuned like Create's logistics items: right-click a Trade Master's station or any
 * Create link with it; right-click the air to clear it. Then right-click any face of a container to hang it there.
 */
public class TradeBoardItem extends Item {
	public TradeBoardItem(Properties properties) {
		super(properties);
	}

	/** The network this board is tuned to, or null. */
	@Nullable
	public static UUID frequency(ItemStack stack) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.hasUUID("Freq") ? tag.getUUID("Freq") : null;
	}

	/** Tunes the stack to {@code frequency}, or clears it (null). */
	public static void tune(ItemStack stack, @Nullable UUID frequency) {
		if (frequency == null) {
			stack.remove(DataComponents.CUSTOM_DATA);
			return;
		}
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		tag.putUUID("Freq", frequency);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return frequency(stack) != null;
	}

	/**
	 * Before the block's own click (so a chest doesn't just open, and his station doesn't open its screen): on a Trade
	 * Master's station or any Create link, take its network; on a container's face, hang there. Anything else is the
	 * block's as usual.
	 */
	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos clicked = context.getClickedPos();
		if (player == null)
			return InteractionResult.PASS;
		LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(level, clicked, LogisticallyLinkedBehaviour.TYPE);
		if (link != null && !player.isShiftKeyDown()) {
			if (level.isClientSide)
				return InteractionResult.SUCCESS;
			if (!link.mayInteractMessage(player))
				return InteractionResult.SUCCESS;
			tune(stack, link.freqId);
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_board.tuned"), true);
			return InteractionResult.SUCCESS;
		}
		if (level.getCapability(Capabilities.ItemHandler.BLOCK, clicked, context.getClickedFace()) == null)
			return InteractionResult.PASS;
		return hang(context, stack);
	}

	/** Clicked on something that isn't a container (and didn't use the click itself): it only hangs on containers. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getPlayer() != null && !context.getLevel().isClientSide)
			context.getPlayer().displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_board.no_container"), true);
		return InteractionResult.FAIL;
	}

	/** Hangs it on the clicked face of a container, if it's tuned and there's no board there already. */
	private InteractionResult hang(UseOnContext context, ItemStack stack) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos clicked = context.getClickedPos();
		Direction face = context.getClickedFace();
		UUID frequency = frequency(stack);
		if (frequency == null) {
			if (!level.isClientSide)
				player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_board.untuned"), true);
			return InteractionResult.FAIL;
		}
		BlockPos at = clicked.relative(face);
		if (TradeBoards.at(level, at, face) != null)
			return InteractionResult.FAIL;
		TradeBoardEntity board = new TradeBoardEntity(level, at, face, frequency);
		if (!board.survives() || !player.mayUseItemAt(at, face, stack))
			return InteractionResult.FAIL;
		if (!level.isClientSide) {
			board.playPlacementSound();
			level.gameEvent(player, GameEvent.ENTITY_PLACE, board.position());
			level.addFreshEntity(board);
		}
		stack.consume(1, player);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** Right-click the air with a tuned board: forget its network, as Create's logistics items do. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (frequency(stack) == null)
			return super.use(level, player, hand);
		if (level.isClientSide)
			level.playSound(player, player.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.75f, 1);
		else {
			tune(stack, null);
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_board.cleared"), true);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		boolean tuned = frequency(stack) != null;
		tooltip.add(Component.translatable("item.comedymonkeysaddons.trade_board." + (tuned ? "tuned" : "untuned"))
			.withStyle(tuned ? ChatFormatting.GOLD : ChatFormatting.GRAY));
	}
}
