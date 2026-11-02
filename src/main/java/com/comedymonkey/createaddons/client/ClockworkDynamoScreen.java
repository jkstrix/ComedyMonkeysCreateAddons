package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity;
import com.comedymonkey.createaddons.network.DynamoSettingsPayload;
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
 * The Clockwork Dynamo's settings, opened with a wrench (design/gui/clockwork-dynamo.gui.js): only the stress it takes,
 * since it follows its shaft's speed and direction. Small steps for water wheels and windmills, then the motor's steps
 * (what the generators make, up to the motor's most). The read-out says what that makes.
 */
public class ClockworkDynamoScreen extends AbstractSimiScreen {
	private static final ResourceLocation BACKGROUND = ComedyMonkeysCreateAddons.asResource("textures/gui/clockwork_dynamo.png");
	private static final int W = 224, H = 80;
	private static final int TEXT = 0xD8D2C4, TEAL = 0x8FD6CC;

	private final BlockPos pos;
	private final int[] stresses;
	/** What it was set to when the screen opened: kept as an option, and nothing is sent unless it's changed. */
	private final int opened;
	private int stress;

	private ClockworkDynamoScreen(BlockPos pos, ClockworkDynamoBlockEntity dynamo) {
		super(Component.translatable("block." + ComedyMonkeysCreateAddons.MOD_ID + ".clockwork_dynamo"));
		this.pos = pos;
		this.opened = dynamo.getStressSetting();
		this.stresses = withSetting(stressSteps(), opened);
		this.stress = opened;
	}

	/** The steps, with its current setting among them even if it isn't one (the config's steps may have changed since). */
	static int[] withSetting(int[] steps, int setting) {
		TreeSet<Integer> all = new TreeSet<>();
		for (int s : steps)
			all.add(s);
		all.add(setting);
		return all.stream().mapToInt(Integer::intValue).toArray();
	}

	public static void open(BlockPos pos) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getBlockEntity(pos) instanceof ClockworkDynamoBlockEntity dynamo)
			mc.setScreen(new ClockworkDynamoScreen(pos, dynamo));
	}

	/** 256 to 2,048 SU (a water wheel to a windmill), then a quarter, half and whole Lava Generators, a turbine, and the most. */
	static int[] stressSteps() {
		int max = ClockworkDynamoBlockEntity.maxStress(), step = ClockworkDynamoBlockEntity.STRESS_STEP;
		double perStress = Math.max(1e-6, CMConfig.SERVER.motorFePerStress());
		double generator = CMConfig.SERVER.generatorFePerTick() / perStress, turbine = CMConfig.SERVER.turbineFePerTick() / perStress;
		TreeSet<Integer> steps = new TreeSet<>(List.of(256, 512, 1024, 2048));
		for (double n : new double[] {0.25, 0.5, 1, 2, 3, 4, 6, 8, 12})
			steps.add((int) Math.round(generator * n / step) * step);
		steps.add((int) Math.round(turbine / step) * step);
		steps.add(max / step * step);
		return steps.stream().filter(s -> s >= step && s <= max).mapToInt(Integer::intValue).toArray();
	}

	private static int indexOf(int[] options, int value) {
		for (int i = 0; i < options.length; i++)
			if (options[i] == value)
				return i;
		return 0;
	}

	private static MutableComponent tr(String key, Object... args) {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.clockwork_dynamo." + key, args);
	}

	@Override
	protected void init() {
		setWindowSize(W, H);
		super.init();
		int x = guiLeft, y = guiTop;

		Label stressLabel = new Label(x + 90, y + 30, Component.empty()).colored(TEXT).withShadow();
		List<Component> stressOptions = new ArrayList<>();
		for (int s : stresses)
			stressOptions.add(Component.literal(String.format(Locale.ROOT, "%,d SU", s)));
		SelectionScrollInput stressInput = new SelectionScrollInput(x + 84, y + 26, 129, 16);
		stressInput.forOptions(stressOptions).titled(tr("stress_title")).addHint(tr("scroll_hint"))
			.calling(i -> stress = stresses[i]).writingTo(stressLabel).setState(indexOf(stresses, stress));
		stressInput.onChanged();

		// the tick sits beside the read-out
		IconButton confirm = new IconButton(x + W - 30, y + 53, AllIcons.I_CONFIRM);
		confirm.withCallback(this::onClose);

		addRenderableWidgets(stressLabel, stressInput, confirm);
	}

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft, y = guiTop;
		graphics.blit(BACKGROUND, x, y, 0, 0, W, H);
		graphics.drawString(font, title, x + (W - font.width(title)) / 2, y + 3, GuiKit.TITLE, false);
		graphics.drawString(font, tr("stress_label"), x + 12, y + 30, TEXT, true);
		graphics.drawString(font, tr("making", String.format(Locale.ROOT, "%,.0f", ClockworkDynamoBlockEntity.outputFor(stress))),
			x + 16, y + 58, TEAL, true);
	}

	@Override
	public void onClose() {
		if (stress != opened)
			PacketDistributor.sendToServer(new DynamoSettingsPayload(pos, stress));
		super.onClose();
	}
}
