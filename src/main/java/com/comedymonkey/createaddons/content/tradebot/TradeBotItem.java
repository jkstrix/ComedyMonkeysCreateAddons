package com.comedymonkey.createaddons.content.tradebot;

import java.util.List;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModSounds;

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
		if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative()
			&& TradeBotRegistry.get(serverPlayer.server).count(serverPlayer.getUUID()) >= CMConfig.SERVER.tradeBotsPerPlayer()) {
			serverPlayer.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.limit",
				CMConfig.SERVER.tradeBotsPerPlayer()), true);
			return InteractionResult.FAIL;
		}
		TradeBotData data = context.getItemInHand().getOrDefault(ModDataComponents.TRADE_BOT_DATA.get(), TradeBotData.EMPTY);
		Component name = context.getItemInHand().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
		BlockPos pos = context.getClickedPos();
		InteractionResult result = super.place(context);
		if (!result.consumesAction() || !(context.getLevel() instanceof ServerLevel level) || player == null)
			return result;
		if (!(level.getBlockEntity(pos) instanceof TradeBotStationBlockEntity station))
			return result;
		TradeBotEntity bot = ModEntities.TRADE_BOT.get().create(level);
		if (bot == null)
			return result;
		bot.setUp(player.getUUID(), pos, station.facing(), data);
		if (name != null)
			bot.setCustomName(name);
		level.addFreshEntity(bot);
		station.claim(player.getUUID(), bot.getUUID(), data);
		TradeBotRegistry.get(level.getServer()).add(player.getUUID(), bot.getUUID(), new TradeBotRegistry.Home(level.dimension(), pos));
		level.playSound(null, pos, ModSounds.tradeBot("winder_engage"), SoundSource.BLOCKS, 1, 1);
		return result;
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
