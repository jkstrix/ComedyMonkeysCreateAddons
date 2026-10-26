package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.network.SplitterInfoPayload;
import com.comedymonkey.createaddons.network.SplitterSettingsPayload;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;

import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A Power Line Splitter's priorities, opened with a wrench: one row per branch of its junction (which way it goes, what's
 * down it) with its priority scrolled between First, Second and Last. When power is short the First branches get all
 * they want, then the Second, then the Last; branches on one level share evenly. A branch with only generators down it
 * shows "Power in". The read-out says how the line stood at its last update, and in what order it's served. Sent when the
 * screen closes.
 */
public class SplitterScreen extends AbstractSimiScreen {
	private static final int W = 224, ROW = 27, TOP = 20;
	private static final int TEXT = 0xD8D2C4, DIM = 0x8A8478, TEAL = 0x8FD6CC, FIRST = 0xE0C27A;

	private final SplitterInfoPayload info;
	private final byte[] priorities = {1, 1, 1, 1, 1, 1};
	private final int h;
	private final ResourceLocation background;

	private SplitterScreen(SplitterInfoPayload info) {
		super(Component.translatable("item." + ComedyMonkeysCreateAddons.MOD_ID + ".splitter"));
		this.info = info;
		for (SplitterInfoPayload.Row r : info.rows())
			priorities[r.dir().ordinal()] = (byte) r.priority();
		int rows = Math.max(3, Math.min(4, info.rows().size()));
		this.h = TOP + rows * ROW + 3 + 24 + 8;
		this.background = ComedyMonkeysCreateAddons.asResource("textures/gui/splitter_" + rows + ".png");
	}

	public static void open(SplitterInfoPayload info) {
		if (Minecraft.getInstance().level != null)
			Minecraft.getInstance().setScreen(new SplitterScreen(info));
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.splitter." + key, args);
	}

	private static boolean powerIn(SplitterInfoPayload.Row r) {
		return r.devices() == 0 && r.generators() > 0;
	}

	@Override
	protected void init() {
		setWindowSize(W, h);
		super.init();
		int x = guiLeft, y = guiTop;
		List<Component> options = List.of(tr("first"), tr("second"), tr("last"));
		for (int i = 0; i < info.rows().size() && i < 4; i++) {
			SplitterInfoPayload.Row r = info.rows().get(i);
			if (powerIn(r))
				continue;
			int rowY = y + TOP + i * ROW;
			Label label = new Label(x + 146, rowY + 8, Component.empty()).colored(TEXT).withShadow();
			SelectionScrollInput input = new SelectionScrollInput(x + 140, rowY + 4, 74, 16);
			int dir = r.dir().ordinal();
			input.forOptions(options).titled(tr("priority_title"))
				.addHint(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.electric_motor.scroll_hint"))
				.calling(k -> {
					priorities[dir] = (byte) (int) k;
					label.colored(k == 0 ? FIRST : TEXT);
				})
				.writingTo(label).setState(priorities[dir]);
			input.onChanged();
			addRenderableWidgets(label, input);
		}
		IconButton confirm = new IconButton(x + W - 30, y + h - 30, AllIcons.I_CONFIRM);
		confirm.withCallback(this::onClose);
		addRenderableWidget(confirm);
	}

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft, y = guiTop;
		graphics.blit(background, x, y, 0, 0, W, h);
		graphics.drawString(font, title, x + (W - font.width(title)) / 2, y + 3, GuiKit.TITLE, false);
		for (int i = 0; i < info.rows().size() && i < 4; i++) {
			SplitterInfoPayload.Row r = info.rows().get(i);
			int rowY = y + TOP + i * ROW;
			graphics.drawString(font, tr("dir_" + r.dir().getName()), x + 12, rowY + 3, TEXT, true);
			graphics.drawString(font, contents(r), x + 12, rowY + 13, DIM, true);
			if (powerIn(r))
				graphics.drawString(font, tr("power_in"), x + 150, rowY + 8, DIM, true);
		}
		int ry = y + TOP + Math.max(3, Math.min(4, info.rows().size())) * ROW + 3;
		if (!info.known()) {
			graphics.drawString(font, tr("pending"), x + 12, ry + 4, TEAL, true);
		} else if (info.wanted() <= 0) {
			graphics.drawString(font, tr("idle"), x + 12, ry + 4, TEAL, true);
		} else if (info.has() >= info.wanted()) {
			graphics.drawString(font, tr("enough"), x + 12, ry + 4, TEAL, true);
			graphics.drawString(font, tr("using", n(info.took()), n(info.has())), x + 12, ry + 14, TEXT, true);
		} else {
			graphics.drawString(font, tr("short", n(info.has()), n(info.wanted())), x + 12, ry + 4, TEAL, true);
			// kept clear of the confirm button: just who's first, if the whole order won't fit
			Component order = order(true);
			graphics.drawString(font, font.width(order) <= W - 46 ? order : order(false), x + 12, ry + 14, TEXT, true);
		}
	}

	private static String n(int v) {
		return String.format(Locale.ROOT, "%,d", v);
	}

	/** "Electric Motor", "Electric Motor ×2", "3 machines", "Nothing". */
	private static Component contents(SplitterInfoPayload.Row r) {
		if (r.devices() > 0)
			return count(r.devices(), r.deviceName(), "machine");
		if (r.generators() > 0)
			return count(r.generators(), r.generatorName(), "generator");
		return tr("nothing");
	}

	private static Component count(int n, @Nullable Component name, String unnamed) {
		if (name == null)
			return n == 1 ? tr(unnamed + "_one") : tr(unnamed + "_many", n);
		return n == 1 ? name : tr("many", name, n);
	}

	/** "East first, then South", "North and East first" ({@code all}: every level, else only the first), "Shared evenly". */
	private Component order(boolean all) {
		List<List<Component>> levels = new ArrayList<>();
		for (int level = 0; level <= 2; level++) {
			List<Component> names = new ArrayList<>();
			for (SplitterInfoPayload.Row r : info.rows())
				if (!powerIn(r) && priorities[r.dir().ordinal()] == level)
					names.add(tr("dir_" + r.dir().getName()));
			if (!names.isEmpty())
				levels.add(names);
		}
		if (levels.size() <= 1)
			return tr("even");
		Component out = tr("order_first", joined(levels.get(0)));
		for (int i = 1; all && i < levels.size(); i++)
			out = tr("order_then", out, joined(levels.get(i)));
		return out;
	}

	private static Component joined(List<Component> names) {
		Component out = names.get(0);
		for (int i = 1; i < names.size(); i++)
			out = tr("and", out, names.get(i));
		return out;
	}

	@Override
	public void onClose() {
		// a branch with only generators down it has nothing to rank: back to the default, so it never stays First unseen
		for (SplitterInfoPayload.Row r : info.rows())
			if (powerIn(r))
				priorities[r.dir().ordinal()] = 1;
		boolean changed = false;
		for (SplitterInfoPayload.Row r : info.rows())
			changed |= priorities[r.dir().ordinal()] != r.priority();
		if (changed)
			PacketDistributor.sendToServer(new SplitterSettingsPayload(info.pos(), priorities.clone()));
		super.onClose();
	}
}
