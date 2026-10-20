package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;
import com.comedymonkey.createaddons.network.MotorSettingsPayload;
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
 * The Electric Motor's settings, opened with a wrench, in Create's own style of screen: scroll the speed and stress
 * fields, click the arrow to reverse. Speed snaps to Create's doubling speeds (1 to 256 RPM); stress snaps to what the
 * generators can feed (a quarter, a half, one, two... Lava Generators, up to one Geothermal Turbine), so it's easy to
 * match a motor to its supply. The read-out says what that costs.
 */
public class ElectricMotorScreen extends AbstractSimiScreen {
	private static final ResourceLocation BACKGROUND = ComedyMonkeysCreateAddons.asResource("textures/gui/electric_motor.png");
	private static final int W = 224, H = 132;
	private static final int[] SPEEDS = {1, 2, 4, 8, 16, 32, 64, 128, 256};
	private static final int TEXT = 0xD8D2C4, BRASS = 0xCFB164, TEAL = 0x8FD6CC;

	private final BlockPos pos;
	private final int[] stresses;
	private int speed;
	private int stress;
	private boolean reversed;
	private IconButton direction;

	private ElectricMotorScreen(BlockPos pos, ElectricMotorBlockEntity motor) {
		super(Component.translatable("block." + ComedyMonkeysCreateAddons.MOD_ID + ".electric_motor"));
		this.pos = pos;
		this.stresses = stressSteps();
		this.speed = nearest(SPEEDS, motor.getSpeedSetting());
		this.stress = nearest(stresses, motor.getStressSetting());
		this.reversed = motor.isReversed();
	}

	public static void open(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getBlockEntity(pos) instanceof ElectricMotorBlockEntity motor)
			mc.setScreen(new ElectricMotorScreen(pos, motor));
	}

	/** One Lava Generator's worth of stress, from the config (the motor draws FE per SU). */
	private static double generatorStress() {
		return CMConfig.SERVER.generatorFePerTick() / Math.max(1e-6, CMConfig.SERVER.motorFePerStress());
	}

	private static double turbineStress() {
		return CMConfig.SERVER.turbineFePerTick() / Math.max(1e-6, CMConfig.SERVER.motorFePerStress());
	}

	/** Quarter and half a generator, whole generators, a turbine, and the motor's most. */
	private static int[] stressSteps() {
		int max = ElectricMotorBlockEntity.maxStress(), step = ElectricMotorBlockEntity.STRESS_STEP;
		TreeSet<Integer> steps = new TreeSet<>();
		for (double n : new double[] {0.25, 0.5, 1, 2, 3, 4, 6, 8, 12})
			steps.add((int) Math.round(generatorStress() * n / step) * step);
		steps.add((int) Math.round(turbineStress() / step) * step);
		steps.add(max / step * step);
		return steps.stream().filter(s -> s >= step && s <= max).mapToInt(Integer::intValue).toArray();
	}

	private static int nearest(int[] options, int value) {
		int best = options[0];
		for (int option : options)
			if (Math.abs(option - value) < Math.abs(best - value))
				best = option;
		return best;
	}

	private static int indexOf(int[] options, int value) {
		for (int i = 0; i < options.length; i++)
			if (options[i] == value)
				return i;
		return 0;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.electric_motor." + key, args);
	}

	@Override
	protected void init() {
		setWindowSize(W, H);
		super.init();
		int x = guiLeft, y = guiTop;

		Label speedLabel = new Label(x + 90, y + 32, Component.empty()).colored(TEXT).withShadow();
		List<Component> speedOptions = new ArrayList<>();
		for (int s : SPEEDS)
			speedOptions.add(Component.literal(s + " RPM"));
		SelectionScrollInput speedInput = new SelectionScrollInput(x + 84, y + 28, 129, 16);
		speedInput.forOptions(speedOptions).titled(tr("speed_title")).addHint(tr("scroll_hint"))
			.calling(i -> speed = SPEEDS[i]).writingTo(speedLabel).setState(indexOf(SPEEDS, speed));
		speedInput.onChanged();

		Label stressLabel = new Label(x + 90, y + 54, Component.empty()).colored(TEXT).withShadow();
		List<Component> stressOptions = new ArrayList<>();
		for (int s : stresses)
			stressOptions.add(Component.literal(String.format(Locale.ROOT, "%,d SU", s)));
		SelectionScrollInput stressInput = new SelectionScrollInput(x + 84, y + 50, 129, 16);
		stressInput.forOptions(stressOptions).titled(tr("stress_title")).addHint(tr("scroll_hint"))
			.calling(i -> stress = stresses[i]).writingTo(stressLabel).setState(indexOf(stresses, stress));
		stressInput.onChanged();

		direction = new IconButton(x + 86, y + 74, icon());
		direction.withCallback(() -> {
			reversed = !reversed;
			direction.setIcon(icon());
			direction.setToolTip(directionName());
		});
		direction.setToolTip(directionName());

		IconButton confirm = new IconButton(x + W - 30, y + H - 30, AllIcons.I_CONFIRM);
		confirm.withCallback(this::onClose);

		addRenderableWidgets(speedLabel, speedInput, stressLabel, stressInput, direction, confirm);
	}

	private AllIcons icon() {
		return reversed ? AllIcons.I_ROTATE_CCW : AllIcons.I_REFRESH;
	}

	private Component directionName() {
		return tr(reversed ? "counter_clockwise" : "clockwise");
	}

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft, y = guiTop;
		graphics.blit(BACKGROUND, x, y, 0, 0, W, H);
		graphics.drawCenteredString(font, title, x + W / 2, y + 8, BRASS);
		graphics.drawString(font, tr("speed_label"), x + 12, y + 32, TEXT, true);
		graphics.drawString(font, tr("stress_label"), x + 12, y + 54, TEXT, true);
		graphics.drawString(font, tr("direction_label"), x + 12, y + 78, TEXT, true);
		graphics.drawString(font, directionName(), x + 110, y + 78, TEXT, true);
		graphics.drawString(font, tr("usage", String.format(Locale.ROOT, "%,.0f", ElectricMotorBlockEntity.usageFor(stress))),
			x + 16, y + 103, TEAL, true);
		graphics.drawString(font, supplyNeeded(), x + 16, y + 113, TEXT, true);
	}

	/** "1 Lava Generator", "1/2 a Lava Generator", "1 Geothermal Turbine"... */
	private Component supplyNeeded() {
		int turbine = (int) Math.round(turbineStress() / ElectricMotorBlockEntity.STRESS_STEP) * ElectricMotorBlockEntity.STRESS_STEP;
		if (stress == turbine)
			return tr("supply_turbine");
		double generators = stress / generatorStress();
		if (Math.abs(generators - 0.25) < 0.01)
			return tr("supply_quarter");
		if (Math.abs(generators - 0.5) < 0.01)
			return tr("supply_half");
		if (stress > turbine)
			return tr("supply_more", String.format(Locale.ROOT, "%.1f", stress / turbineStress()));
		long whole = Math.round(generators);
		String count = Math.abs(generators - whole) < 0.01 ? String.valueOf(whole) : String.format(Locale.ROOT, "%.1f", generators);
		if (generators >= 4)
			return tr("supply_many_turbine", count, Math.round(100 * stress / turbineStress()));
		return tr(whole == 1 ? "supply_one" : "supply_many", count);
	}

	@Override
	public void onClose() {
		PacketDistributor.sendToServer(new MotorSettingsPayload(pos, speed, stress, reversed));
		super.onClose();
	}
}
