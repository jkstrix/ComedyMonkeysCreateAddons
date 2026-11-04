package com.comedymonkey.createaddons.content.tradebot;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * Trade Bot on his winding station, as one item (D27). Placing it sets the station down with him docked in it,
 * owned by whoever placed it (D23), with whatever he carried when he was packed up.
 */
public class TradeBotItem extends BlockItem {
	public TradeBotItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		Player player = context.getPlayer();
		BotKind kind = kind();
		if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative()
			&& TradeBotRegistry.get(serverPlayer.server).count(serverPlayer.getUUID(), kind) >= kind.perPlayer()) {
			serverPlayer.displayClientMessage(Component.translatable("message.comedymonkeysaddons." + kind.id() + ".limit", kind.perPlayer()), true);
			return InteractionResult.FAIL;
		}
		TradeBotData data = context.getItemInHand().getOrDefault(ModDataComponents.TRADE_BOT_DATA.get(), TradeBotData.EMPTY);
		Component name = context.getItemInHand().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
		BlockPos pos = context.getClickedPos();
		InteractionResult result = super.place(context);
		if (!result.consumesAction() || !(context.getLevel() instanceof ServerLevel level) || player == null)
			return result;
		if (populate(level, pos, player.getUUID(), data, name) != null)
			level.playSound(null, pos, ModSounds.tradeBot("winder_engage"), SoundSource.BLOCKS, 1, 1);
		return result;
	}

	/**
	 * The Trade Master's item is tuned like Create's logistics items (design/specs/trade-master.md P1): right-click any
	 * Create link (a Stock Link, a gauge, another Trade Master's station) to take its network; placed, his station keeps
	 * it. Untuned, he starts a network of his own.
	 */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (kind() != BotKind.TRADE_MASTER || player == null || player.isShiftKeyDown())
			return super.useOn(context);
		LogisticallyLinkedBehaviour link = BlockEntityBehaviour.get(context.getLevel(), context.getClickedPos(), LogisticallyLinkedBehaviour.TYPE);
		if (link == null)
			return super.useOn(context);
		if (context.getLevel().isClientSide)
			return InteractionResult.SUCCESS;
		if (link.mayInteractMessage(player))
			LogisticallyLinkedBlockItem.assignFrequency(context.getItemInHand(), player, link.freqId);
		return InteractionResult.SUCCESS;
	}

	/** Packed up: his network goes into the item, as Create keeps a tuned logistics block's. */
	public static void keepFrequency(ItemStack stack, java.util.UUID frequency, net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putUUID("Freq", frequency);
		net.minecraft.world.level.block.entity.BlockEntity.addEntityType(tag, type);
		stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return kind() == BotKind.TRADE_MASTER && LogisticallyLinkedBlockItem.isTuned(stack) || super.isFoil(stack);
	}

	/** Trade Bot or the Trade Master, by the station this item places. */
	public BotKind kind() {
		return getBlock() instanceof TradeBotStationBlock station ? station.kind() : BotKind.TRADE_BOT;
	}

	/** Puts a bot, docked, into a just-placed station at {@code pos}, owned by {@code owner}. Also used by the game tests. */
	@Nullable
	public static TradeBotEntity populate(ServerLevel level, BlockPos pos, UUID owner, TradeBotData data, @Nullable Component name) {
		if (!(level.getBlockEntity(pos) instanceof TradeBotStationBlockEntity station))
			return null;
		TradeBotEntity bot = station.kind().entityType().create(level);
		if (bot == null)
			return null;
		bot.setUp(owner, pos, station.facing(), data);
		if (name != null)
			bot.setCustomName(name);
		level.addFreshEntity(bot);
		station.claim(owner, bot.getUUID(), data);
		TradeBotRegistry.get(level.getServer()).add(owner, bot.getUUID(), new TradeBotRegistry.Home(level.dimension(), pos, station.kind()));
		return bot;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TradeBotData data = stack.get(ModDataComponents.TRADE_BOT_DATA.get());
		if (data == null)
			return;
		int full = CMConfig.SERVER.tradeBotFullCharge();
		tooltip.add(Component.translatable("item.comedymonkeysaddons.trade_bot.charge", Math.round(100f * data.charge() / full))
			.withStyle(ChatFormatting.GRAY));
		if (data.emeralds() > 0)
			tooltip.add(Component.translatable("item.comedymonkeysaddons.trade_bot.emeralds", data.emeralds()).withStyle(ChatFormatting.GRAY));
	}
}
