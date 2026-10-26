package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity;
import com.comedymonkey.createaddons.network.TurbineSettingsPayload;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;

import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Geothermal Turbine's power level, opened with a wrench on a formed turbine's controller, in the Electric Motor
 * screen's style: scroll the power field in 10% steps of full output. The read-out says what that makes, what it burns,
 * and how many Lava Generators it's worth. It's sent when the screen closes, if it changed.
 */
public class GeothermalTurbineScreen extends AbstractSimiScreen {
	private static final ResourceLocation BACKGROUND = ComedyMonkeysCreateAddons.asResource("textures/gui/geothermal_turbine.png");
	private static final int W = 224, H = 116, STEPS = 10;
	private static final int TEXT = 0xD8D2C4, TEAL = 0x8FD6CC;

	private final BlockPos pos;
	/** Tenths of full output, 1 to 10, and as it was when opened. */
	private int tenths;
	private final int opened;

	private GeothermalTurbineScreen(BlockPos pos, GeothermalControllerBlockEntity turbine) {
		super(Component.translatable("block." + ComedyMonkeysCreateAddons.MOD_ID + ".geothermal_controller"));
		this.pos = pos;
		this.tenths = this.opened = turbine.outputTenths();
	}

	public static void open(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getBlockEntity(pos) instanceof GeothermalControllerBlockEntity turbine)
			mc.setScreen(new GeothermalTurbineScreen(pos, turbine));
	}

	private static int full() {
		return Math.max(0, CMConfig.SERVER.turbineFePerTick());
	}

	private static int fe(int tenths) {
		return (int) Math.round(full() * tenths / (double) STEPS);
	}

	private static String n(double v) {
		return v == Math.rint(v) ? String.format(Locale.ROOT, "%,.0f", v) : String.format(Locale.ROOT, "%,.1f", v);
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.geothermal_turbine." + key, args);
	}

	@Override
	protected void init() {
		setWindowSize(W, H);
		super.init();
		int x = guiLeft, y = guiTop;

		Label powerLabel = new Label(x + 90, y + 32, Component.empty()).colored(TEXT).withShadow();
		List<Component> options = new ArrayList<>();
		for (int t = 1; t <= STEPS; t++)
			options.add(tr("power_option", n(fe(t)), t * 10));
		SelectionScrollInput powerInput = new SelectionScrollInput(x + 84, y + 28, 129, 16);
		powerInput.forOptions(options).titled(tr("power_title"))
			.addHint(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.electric_motor.scroll_hint"))
			.calling(i -> tenths = i + 1).writingTo(powerLabel).setState(tenths - 1);
		powerInput.onChanged();

		IconButton confirm = new IconButton(x + W - 30, y + H - 30, AllIcons.I_CONFIRM);
		confirm.withCallback(this::onClose);

		addRenderableWidgets(powerLabel, powerInput, confirm);
	}

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft, y = guiTop;
		graphics.blit(BACKGROUND, x, y, 0, 0, W, H);
		graphics.drawString(font, title, x + (W - font.width(title)) / 2, y + 3, GuiKit.TITLE, false);
		graphics.drawString(font, tr("power_label"), x + 12, y + 32, TEXT, true);
		double share = tenths / (double) STEPS;
		graphics.drawString(font, tr("makes", n(fe(tenths))), x + 16, y + 61, TEAL, true);
		graphics.drawString(font, tr("burns", n(CMConfig.SERVER.turbineLavaPerSecond() * share), n(CMConfig.SERVER.turbineWaterPerSecond() * share)),
			x + 16, y + 72, TEXT, true);
		graphics.drawString(font, tr("worth", n(fe(tenths) / (double) Math.max(1, CMConfig.SERVER.generatorFePerTick()))), x + 16, y + 83, TEXT, true);
	}

	@Override
	public void onClose() {
		if (tenths != opened)
			PacketDistributor.sendToServer(new TurbineSettingsPayload(pos, tenths));
		super.onClose();
	}
}
