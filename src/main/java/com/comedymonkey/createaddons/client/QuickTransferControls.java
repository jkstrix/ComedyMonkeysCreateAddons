package com.comedymonkey.createaddons.client;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.QuickTransfer.Action;
import com.comedymonkey.createaddons.content.QuickTransferMenu;
import com.comedymonkey.createaddons.network.QuickTransferPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Quick moves on our screens with a player inventory (storage and machine output): four small buttons at the right of
 * the inventory panel's label (put everything in, put in what matches, take everything out, take out what matches),
 * the mouse wheel over a slot moving one item, and shift-dragging, which shift-clicks every slot the mouse passes
 * over. The block's panel is always above the inventory, so scrolling towards the other panel sends an item there and
 * scrolling away pulls a matching one back.
 */
public final class QuickTransferControls {
	private static final int SIZE = 10;
	private static final int SPRITE_V = 76, SPRITE_HOVER_V = 86;
	private static final Action[] BUTTONS = {Action.ALL_IN, Action.MATCHING_IN, Action.ALL_OUT, Action.MATCHING_OUT};
	/** Where each button's sprite is in the widgets texture. */
	private static final int[] SPRITE_U = {0, 10, 20, 30};

	/** Slots already shift-clicked in the current shift-drag. */
	private static final Set<Integer> DRAGGED = new HashSet<>();

	private QuickTransferControls() {}

	public static void onScreenInit(ScreenEvent.Init.Post event) {
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof QuickTransferMenu quick)
			|| quick.firstPlayerSlot() >= screen.getMenu().slots.size())
			return;
		// The inventory panel, found from its first slot
		Slot first = screen.getMenu().slots.get(quick.firstPlayerSlot());
		int panelLeft = screen.getGuiLeft() + first.x - 8;
		int panelTop = screen.getGuiTop() + first.y - MenuLayout.INVENTORY_ROWS_Y;
		int right = panelLeft + MenuLayout.INVENTORY_WIDTH - 7;
		int y = panelTop + 3;
		int containerId = screen.getMenu().containerId;
		// Machines that only hand out their output get just the two take buttons
		int from = quick.allowsQuickTransferIn() ? 0 : 2;
		for (int i = from; i < BUTTONS.length; i++) {
			Action action = BUTTONS[i];
			int u = SPRITE_U[i];
			Button button = new Button(right - (BUTTONS.length - i) * (SIZE + 1), y, SIZE, SIZE, Component.empty(),
				b -> PacketDistributor.sendToServer(new QuickTransferPayload(containerId, action, -1)), narration -> narration.get()) {
				@Override
				protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
					graphics.blit(GuiKit.WIDGETS, getX(), getY(), u, isHoveredOrFocused() ? SPRITE_HOVER_V : SPRITE_V, SIZE, SIZE);
				}
			};
			String key = ComedyMonkeysCreateAddons.MOD_ID + ".gui.quick_transfer." + action.name().toLowerCase(Locale.ROOT);
			button.setTooltip(Tooltip.create(Component.translatable(key).append("\n")
				.append(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.quick_transfer.wheel")
					.withStyle(ChatFormatting.GRAY))));
			event.addListener(button);
		}
	}

	/** A shift-click starts a shift-drag; vanilla moves the slot under the mouse itself. */
	public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
		DRAGGED.clear();
		if (event.getButton() == 0 && Screen.hasShiftDown() && event.getScreen() instanceof AbstractContainerScreen<?> screen
			&& screen.getMenu() instanceof QuickTransferMenu && screen.getSlotUnderMouse() != null)
			DRAGGED.add(screen.getSlotUnderMouse().index);
	}

	/** Shift held while dragging: shift-click each slot the mouse reaches, once, either side of the screen. */
	public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
		if (event.getMouseButton() != 0 || !Screen.hasShiftDown()
			|| !(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof QuickTransferMenu)
			|| !screen.getMenu().getCarried().isEmpty())
			return;
		Slot slot = screen.getSlotUnderMouse();
		Minecraft minecraft = Minecraft.getInstance();
		if (slot == null || !slot.hasItem() || minecraft.gameMode == null || !DRAGGED.add(slot.index))
			return;
		minecraft.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slot.index, 0, ClickType.QUICK_MOVE,
			minecraft.player);
	}

	public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
		DRAGGED.clear();
	}

	public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof QuickTransferMenu quick)
			|| event.getScrollDeltaY() == 0)
			return;
		Slot slot = screen.getSlotUnderMouse();
		if (slot == null)
			return;
		boolean blockSide = slot.index < quick.firstPlayerSlot();
		boolean towardsInventory = event.getScrollDeltaY() < 0;
		Action action = blockSide == towardsInventory ? Action.ONE_AWAY : Action.ONE_HERE;
		// Take-only machines: the wheel can't put anything in (the server refuses it too)
		if (!quick.allowsQuickTransferIn() && (action == Action.ONE_AWAY) != blockSide)
			return;
		PacketDistributor.sendToServer(new QuickTransferPayload(screen.getMenu().containerId, action, slot.index));
		event.setCanceled(true);
	}
}
