package com.comedymonkey.createaddons.client.tradebot;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tradebot.board.TradeBoardEntity;
import com.comedymonkey.createaddons.content.tradebot.master.GaugeNames;
import com.comedymonkey.createaddons.content.tradebot.master.TradeNetwork;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What goggles show on a Trade Board (design/specs/trade-master.md H8): the container it hangs on, and whether it's an
 * input or the gauges on its face, each with its count and what's on its way, as Create's gauges know them on the
 * client. {@code GoggleOverlayRendererMixin} draws it the way Create draws a block's.
 */
public final class TradeBoardGoggles {
	private TradeBoardGoggles() {}

	public static List<Component> tooltip(TradeBoardEntity board) {
		List<Component> tooltip = new ArrayList<>();
		Level level = board.level();
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.trade_board").forGoggles(tooltip);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.trade_board.on",
			level.getBlockState(board.containerPos()).getBlock().getName()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		List<FactoryPanelBehaviour> gauges = new ArrayList<>();
		BlockState state = level.getBlockState(board.getPos());
		if (state.getBlock() instanceof FactoryPanelBlock && FactoryPanelBlock.connectedDirection(state) == board.getDirection()
			&& level.getBlockEntity(board.getPos()) instanceof FactoryPanelBlockEntity panel)
			for (FactoryPanelBehaviour gauge : panel.panels.values())
				if (gauge.active && !gauge.getFilter().isEmpty() && gauge.getAmount() > 0)
					gauges.add(gauge);
		if (gauges.isEmpty()) {
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.trade_board.input").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			return tooltip;
		}
		for (FactoryPanelBehaviour gauge : gauges) {
			ItemStack kind = gauge.getFilter();
			int amount = TradeNetwork.amount(gauge.getAmount(), gauge.upTo, kind);
			int has = gauge.getLevelInStorage(), coming = gauge.getPromised();
			String key = coming > 0 ? "gui.goggles.trade_board.gauge_coming" : "gui.goggles.trade_board.gauge";
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate(key, GaugeNames.of(kind), has, amount, coming)
				.style(has >= amount ? ChatFormatting.GREEN : ChatFormatting.GOLD).forGoggles(tooltip, 1);
		}
		return tooltip;
	}
}
