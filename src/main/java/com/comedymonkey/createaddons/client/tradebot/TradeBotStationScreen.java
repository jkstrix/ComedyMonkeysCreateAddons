package com.comedymonkey.createaddons.client.tradebot;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.GuiKit;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.TradeBotLinks;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.comedymonkey.createaddons.network.TradeBotStationPayload;

import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Trade Bot's station screen (D61, previewed in design/gui and approved): his status and charge, the containers he
 * takes from and delivers to (each with an ✕ to unlink), and buttons to pick containers in the world or call him home.
 * The background is drawn by design/gui/trade-bot-station.gui.js; the layout numbers below come from it.
 */
public class TradeBotStationScreen extends AbstractSimiScreen {
	private static final ResourceLocation BACKGROUND = ComedyMonkeysCreateAddons.asResource("textures/gui/trade_bot_station.png");
	private static final int W = 212, H = 182;
	private static final int STATUS_X = 6, STATUS_Y = 18, GAUGE_X = 148, GAUGE_Y = 18, GAUGE_W = 56;
	private static final int INPUTS_LABEL_Y = 36, OUTPUT_LABEL_Y = 126;
	private static final int[] ROW_Y = { 46, 65, 84, 103 };
	private static final int OUTPUT_Y = 136, ROW_X = 6, ROW_W = 200;
	private static final int BUTTON_Y = 160, BUTTON_W = 98, BUTTON_H = 16, SELECT_X = 6, HOME_X = 108;
	/** Sprites below the background on the sheet: the segmented gauge fill, and the unlink cross (normal, hovered). */
	private static final int SPRITES_V = H + 4, GAUGE_U = 0, CROSS_U = 60, CROSS_HOVER_U = 70;

	private final BlockPos pos;

	private TradeBotStationScreen(BlockPos pos) {
		super(Component.translatable("block." + ComedyMonkeysCreateAddons.MOD_ID + ".trade_bot"));
		this.pos = pos;
	}

	public static void open(BlockPos pos) {
		Minecraft.getInstance().setScreen(new TradeBotStationScreen(pos));
	}

	@Nullable
	private TradeBotStationBlockEntity station() {
		Level level = Minecraft.getInstance().level;
		return level != null && level.getBlockEntity(pos) instanceof TradeBotStationBlockEntity be ? be : null;
	}

	@Override
	protected void init() {
		setWindowSize(W, H);
		super.init();
	}

	@Override
	public void tick() {
		super.tick();
		if (station() == null)
			onClose(); // the station's gone (packed up, or out of range)
	}

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		TradeBotStationBlockEntity station = station();
		if (station == null)
			return;
		int x = guiLeft, y = guiTop;
		graphics.blit(BACKGROUND, x, y, 0, 0, W, H);
		graphics.drawString(font, title, x + (W - font.width(title)) / 2, y + 3, GuiKit.TITLE, false);

		// status and charge
		// status on the left, his charge as a number on the right of the same line, and the gauge beside it
		graphics.drawString(font, statusText(station), x + STATUS_X + 4, y + STATUS_Y + 3, GuiKit.TEXT, false);
		float charge = Mth.clamp((float) station.botCharge() / CMConfig.SERVER.tradeBotFullCharge(), 0, 1);
		String percent = Math.round(charge * 100) + "%";
		graphics.drawString(font, percent, x + GAUGE_X - 6 - font.width(percent), y + STATUS_Y + 3, GuiKit.LABEL, false);
		graphics.blit(BACKGROUND, x + GAUGE_X + 1, y + GAUGE_Y + 1, GAUGE_U, SPRITES_V, Math.round(GAUGE_W * charge), 12);

		// links
		List<BlockPos> inputs = station.inputPositions();
		graphics.drawString(font, tr("takes_from"), x + 8, y + INPUTS_LABEL_Y, GuiKit.LABEL, false);
		String count = inputs.size() + " / " + TradeBotStationBlockEntity.MAX_INPUTS;
		graphics.drawString(font, count, x + W - 8 - font.width(count), y + INPUTS_LABEL_Y, GuiKit.LABEL, false);
		for (int i = 0; i < ROW_Y.length; i++)
			row(graphics, station, i < inputs.size() ? inputs.get(i) : null, ROW_Y[i], mouseX, mouseY);
		graphics.drawString(font, tr("delivers_to"), x + 8, y + OUTPUT_LABEL_Y, GuiKit.LABEL, false);
		row(graphics, station, station.outputPosition(), OUTPUT_Y, mouseX, mouseY);

		// buttons
		button(graphics, tr("select"), SELECT_X, mouseX, mouseY);
		button(graphics, tr("call_home"), HOME_X, mouseX, mouseY);
	}

	private void row(GuiGraphics graphics, TradeBotStationBlockEntity station, @Nullable BlockPos target, int rowY, int mouseX, int mouseY) {
		int x = guiLeft + ROW_X, y = guiTop + rowY;
		if (target == null) {
			graphics.drawString(font, tr("empty"), x + 24, y + 5, 0x6E6B64, false);
			return;
		}
		Level level = Minecraft.getInstance().level;
		BlockState state = level.getBlockState(target);
		ItemStack icon = new ItemStack(state.getBlock().asItem());
		graphics.renderItem(icon, x + 4, y + 1);
		boolean alive = TradeBotLinks.canLink(level, target);
		Component name = alive ? state.getBlock().getName() : tr("missing");
		graphics.drawString(font, name, x + 24, y + 5, alive ? GuiKit.TEXT : GuiKit.BAD, false);
		String distance = String.format(Locale.ROOT, "%.0f m", Math.sqrt(target.distSqr(pos)));
		graphics.drawString(font, distance, x + ROW_W - 20 - font.width(distance), y + 5, GuiKit.LABEL, false);
		boolean hover = overCross(rowY, mouseX, mouseY);
		graphics.blit(BACKGROUND, x + ROW_W - 14, y + 5, hover ? CROSS_HOVER_U : CROSS_U, SPRITES_V, 8, 8);
		if (hover)
			graphics.renderTooltip(font, tr("unlink"), mouseX, mouseY);
	}

	private void button(GuiGraphics graphics, Component label, int bx, int mouseX, int mouseY) {
		int x = guiLeft + bx, y = guiTop + BUTTON_Y;
		if (mouseX >= x && mouseX < x + BUTTON_W && mouseY >= y && mouseY < y + BUTTON_H)
			graphics.fill(x + 1, y + 1, x + BUTTON_W - 1, y + BUTTON_H - 1, 0x22FFFFFF);
		graphics.drawString(font, label, x + (BUTTON_W - font.width(label)) / 2, y + 4, GuiKit.TEXT, true);
	}

	private boolean overCross(int rowY, double mouseX, double mouseY) {
		int x = guiLeft + ROW_X + ROW_W - 15, y = guiTop + rowY + 4;
		return mouseX >= x && mouseX < x + 10 && mouseY >= y && mouseY < y + 10;
	}

	private boolean overButton(int bx, double mouseX, double mouseY) {
		int x = guiLeft + bx, y = guiTop + BUTTON_Y;
		return mouseX >= x && mouseX < x + BUTTON_W && mouseY >= y && mouseY < y + BUTTON_H;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		TradeBotStationBlockEntity station = station();
		if (button == 0 && station != null) {
			List<BlockPos> inputs = station.inputPositions();
			for (int i = 0; i < inputs.size() && i < ROW_Y.length; i++)
				if (overCross(ROW_Y[i], mouseX, mouseY))
					return send(TradeBotStationPayload.Action.UNLINK, inputs.get(i));
			if (station.outputPosition() != null && overCross(OUTPUT_Y, mouseX, mouseY))
				return send(TradeBotStationPayload.Action.UNLINK, station.outputPosition());
			if (overButton(SELECT_X, mouseX, mouseY)) {
				onClose();
				TradeBotLinkPicking.start(pos);
				return true;
			}
			if (overButton(HOME_X, mouseX, mouseY)) {
				send(TradeBotStationPayload.Action.CALL_HOME, pos);
				onClose();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean send(TradeBotStationPayload.Action action, BlockPos target) {
		PacketDistributor.sendToServer(new TradeBotStationPayload(pos, action, target));
		Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
			net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1));
		return true;
	}

	private Component statusText(TradeBotStationBlockEntity station) {
		if (station.getBotId() == null)
			return tr("status.no_bot");
		if (station.isPaused())
			return tr("status.paused");
		if (station.isBotDocked())
			return tr(station.isCharging() ? "status.charging" : "status.docked");
		return tr("status.working");
	}

	private static Component tr(String key) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.trade_bot." + key);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
