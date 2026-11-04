package com.comedymonkey.createaddons.client.tradebot;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.GuiKit;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.comedymonkey.createaddons.content.tradebot.master.GaugeNames;
import com.comedymonkey.createaddons.content.tradebot.master.MasterStatus;
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
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Trade Master's station screen (design/specs/trade-master.md P2, P3; previewed in
 * design/gui/trade-master-station-preview.html and approved): his status and charge as Trade Bot's, the board chests he
 * can see, the gauge he's working on, and every other gauge on his network in a list that scrolls (can't fill and why,
 * waiting or another Trade Master's, full), and Call home. No container links: he works only through his network. The
 * background is drawn by design/gui/trade-master-station.gui.js; the layout numbers below come from it.
 */
public class TradeMasterStationScreen extends AbstractSimiScreen {
	private static final ResourceLocation BACKGROUND = ComedyMonkeysCreateAddons.asResource("textures/gui/trade_master_station.png");
	private static final int W = 212, H = 230;
	private static final int STATUS_X = 6, STATUS_Y = 18, GAUGE_X = 148, GAUGE_Y = 18, GAUGE_W = 56;
	private static final int NETWORK_LABEL_Y = 36, NETWORK_Y = 46, WORKING_LABEL_Y = 68, WORKING_Y = 78, GAUGES_LABEL_Y = 100;
	private static final int ROW_X = 6, ROW_W = 200;
	/** The other gauges: rows of 18 inside the list's recess, and its scroll track. */
	private static final int LIST_Y = 111, ROW_H = 18, ROWS = 5, TRACK_X = 201, TRACK_W = 4, TRACK_H = 90;
	private static final int BUTTON_X = 6, BUTTON_Y = 208, BUTTON_W = 200, BUTTON_H = 16;
	/** The segmented charge gauge fill, below the background on the sheet. */
	private static final int SPRITES_V = H + 4, GAUGE_U = 0;
	private static final int GOOD = 0x8FBF6A, WARN = 0xD9A25A, DIM = 0x6E6B64;
	private static final int THUMB = 0xFF4E4A44, THUMB_TOP = 0xFF615C55, THUMB_EDGE = 0xFF2A2622;

	private final BlockPos pos;
	/** The first of the other gauges shown. */
	private int scroll;

	private TradeMasterStationScreen(BlockPos pos) {
		super(Component.translatable("block." + ComedyMonkeysCreateAddons.MOD_ID + ".trade_master"));
		this.pos = pos;
	}

	public static void open(BlockPos pos) {
		Minecraft.getInstance().setScreen(new TradeMasterStationScreen(pos));
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
		MasterStatus status = station.masterStatus();
		int x = guiLeft, y = guiTop;
		graphics.blit(BACKGROUND, x, y, 0, 0, W, H);
		graphics.drawString(font, title, x + (W - font.width(title)) / 2, y + 3, GuiKit.TITLE_LIGHT, false);

		// status on the left, his charge as a number on the right of the same line, and the gauge beside it
		graphics.drawString(font, statusText(station, status), x + STATUS_X + 4, y + STATUS_Y + 3, GuiKit.TEXT, false);
		float charge = Mth.clamp((float) station.botCharge() / CMConfig.SERVER.tradeBotFullCharge(), 0, 1);
		String percent = Math.round(charge * 100) + "%";
		graphics.drawString(font, percent, x + GAUGE_X - 6 - font.width(percent), y + STATUS_Y + 3, GuiKit.LABEL, false);
		graphics.blit(BACKGROUND, x + GAUGE_X + 1, y + GAUGE_Y + 1, GAUGE_U, SPRITES_V, Math.round(GAUGE_W * charge), 12);

		// his network
		graphics.drawString(font, tr("network"), x + 8, y + NETWORK_LABEL_Y, GuiKit.LABEL, false);
		if (status.boards() == 0)
			graphics.drawString(font, tr("no_boards"), x + ROW_X + 4, y + NETWORK_Y + 5, WARN, false);
		else {
			graphics.drawString(font, tr("connected"), x + ROW_X + 4, y + NETWORK_Y + 5, GOOD, false);
			Component seen = Component.translatable(key("board_chests"), status.boards());
			graphics.drawString(font, seen, x + ROW_X + ROW_W - 6 - font.width(seen), y + NETWORK_Y + 5, GuiKit.LABEL, false);
		}

		// the gauge he's filling
		graphics.drawString(font, tr("working_on"), x + 8, y + WORKING_LABEL_Y, GuiKit.LABEL, false);
		if (status.working().isEmpty())
			graphics.drawString(font, tr("nothing"), x + ROW_X + 24, y + WORKING_Y + 5, DIM, false);
		else {
			String fill = status.level() + " / " + status.amount();
			Component amounts = status.coming() > 0 ? Component.translatable(key("coming"), fill, status.coming()) : Component.literal(fill);
			row(graphics, status.working(), WORKING_Y, ROW_W, amounts, GOOD, mouseX, mouseY);
		}

		// every other gauge, scrolling
		List<MasterStatus.Line> others = status.others();
		graphics.drawString(font, tr("other_gauges"), x + 8, y + GAUGES_LABEL_Y, GuiKit.LABEL, false);
		if (!others.isEmpty()) {
			String count = Integer.toString(others.size());
			graphics.drawString(font, count, x + W - 8 - font.width(count), y + GAUGES_LABEL_Y, GuiKit.LABEL, false);
		}
		scroll = Mth.clamp(scroll, 0, Math.max(0, others.size() - ROWS));
		if (others.isEmpty())
			graphics.drawString(font, tr("none"), x + ROW_X + 24, y + LIST_Y + 5, DIM, false);
		for (int i = 0; i < ROWS && scroll + i < others.size(); i++) {
			MasterStatus.Line line = others.get(scroll + i);
			Component right = switch (line.state()) {
				case CANT -> tr("reason." + (line.reason() == null ? "nobody_sells" : line.reason().name().toLowerCase(Locale.ROOT)));
				case OTHER_MASTER -> tr("another_master");
				default -> Component.literal(line.level() + " / " + line.amount());
			};
			int colour = switch (line.state()) {
				case CANT -> WARN;
				case WAITING -> GuiKit.LABEL;
				default -> DIM;
			};
			row(graphics, line.kind(), LIST_Y - 1 + i * ROW_H, ROW_W - TRACK_W - 2, right, colour, mouseX, mouseY);
		}
		if (others.size() > ROWS) {
			// the thumb: its share of the track as the rows shown are of the list
			int thumbH = Math.max(8, TRACK_H * ROWS / others.size());
			int thumbY = y + LIST_Y + (TRACK_H - thumbH) * scroll / (others.size() - ROWS);
			int tx = x + TRACK_X;
			graphics.fill(tx, thumbY, tx + TRACK_W, thumbY + thumbH, THUMB_EDGE);
			graphics.fill(tx, thumbY, tx + TRACK_W - 1, thumbY + thumbH - 1, THUMB_TOP);
			graphics.fill(tx + 1, thumbY + 1, tx + TRACK_W - 1, thumbY + thumbH - 1, THUMB);
		}

		// Call home
		int bx = x + BUTTON_X, by = y + BUTTON_Y;
		if (overButton(mouseX, mouseY))
			graphics.fill(bx + 1, by + 1, bx + BUTTON_W - 1, by + BUTTON_H - 1, 0x22FFFFFF);
		Component home = Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.trade_bot.call_home");
		graphics.drawString(font, home, bx + (BUTTON_W - font.width(home)) / 2, by + 4, GuiKit.TEXT, true);
	}

	/**
	 * A gauge's row: its icon and name (an enchanted book by its enchantments), cut to leave room for what's on the
	 * right; hovering it shows the item's full tooltip.
	 */
	private void row(GuiGraphics graphics, ItemStack kind, int rowY, int width, Component right, int colour, int mouseX, int mouseY) {
		int x = guiLeft + ROW_X, y = guiTop + rowY;
		graphics.renderItem(kind, x + 4, y + 1);
		int rightW = font.width(right);
		graphics.drawString(font, right, x + width - 6 - rightW, y + 5, colour, false);
		String name = GaugeNames.of(kind).getString();
		int room = width - 6 - rightW - 24 - 4;
		String shown = font.width(name) > room ? font.plainSubstrByWidth(name, Math.max(0, room - font.width("…"))) + "…" : name;
		graphics.drawString(font, shown, x + 24, y + 5, GuiKit.TEXT, false);
		if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_H)
			graphics.renderTooltip(font, kind, mouseX, mouseY);
	}

	private Component statusText(TradeBotStationBlockEntity station, MasterStatus status) {
		if (station.getBotId() == null)
			return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.goggles.trade_bot.status.no_master");
		if (station.isPaused())
			return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.trade_bot.status.paused");
		if (station.isBotDocked())
			return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.trade_bot.status." + (station.isCharging() ? "charging" : "docked"));
		if (status.activity() != null)
			return tr("activity." + status.activity().name().toLowerCase(Locale.ROOT));
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.trade_bot.status.working");
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			scroll -= (int) Math.signum(scrollY);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private boolean overButton(double mouseX, double mouseY) {
		int x = guiLeft + BUTTON_X, y = guiTop + BUTTON_Y;
		return mouseX >= x && mouseX < x + BUTTON_W && mouseY >= y && mouseY < y + BUTTON_H;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && station() != null && overButton(mouseX, mouseY)) {
			PacketDistributor.sendToServer(new TradeBotStationPayload(pos, TradeBotStationPayload.Action.CALL_HOME, pos));
			Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
				net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1));
			onClose();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private static String key(String name) {
		return ComedyMonkeysCreateAddons.MOD_ID + ".gui.trade_master." + name;
	}

	private static Component tr(String name) {
		return Component.translatable(key(name));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
