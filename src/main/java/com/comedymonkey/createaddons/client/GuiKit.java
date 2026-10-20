package com.comedymonkey.createaddons.client;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The look every screen shares (see {@link MenuLayout} for where things go): text colours, the shared sprites, the
 * title on the header band, and the band tinted to a chest tier's colour.
 */
public final class GuiKit {
	/** Title on a header band. */
	public static final int TITLE = 0x2B2724;
	/** Title on a dark band (Netherite). */
	public static final int TITLE_LIGHT = 0xE6DEE0;
	/** Text on the dark body. */
	public static final int TEXT = 0xD6D8D4;
	public static final int LABEL = 0xA8ACA8;
	public static final int BAD = 0xD07060;
	/** The "Inventory" label on the inventory panel. */
	public static final int INVENTORY_TEXT = 0x33352F;

	/** Shared sprites: padlock, Sort button, Wellspring glass. */
	public static final ResourceLocation WIDGETS = ComedyMonkeysCreateAddons.asResource("textures/gui/widgets.png");
	public static final int LOCK_U = 0, LOCK_V = 0;
	public static final int SORT_U = 16, SORT_HOVER_U = 32, SORT_V = 0, SORT_W = 16, SORT_H = 12;
	public static final int GLASS_U = 0, GLASS_V = 16;

	/** Counts from here up are too wide for a slot at full size. */
	public static final int BIG_COUNT = 100;
	private static final float BIG_COUNT_SCALE = 0.75f;

	private GuiKit() {}

	/** A count for a slot: as it is up to 999, then shortened and rounded down, so 4,096 is "4.0k" and 12,800 "12k". */
	public static String shortCount(int count) {
		if (count < 1000)
			return String.valueOf(count);
		if (count < 10_000)
			return count / 1000 + "." + count / 100 % 10 + "k";
		return count / 1000 + "k";
	}

	/** Draws a big stack's count three-quarter size in the slot's bottom right, where vanilla draws counts. */
	public static void drawBigCount(GuiGraphics graphics, Font font, int x, int y, int count) {
		String text = shortCount(count);
		graphics.pose().pushPose();
		graphics.pose().translate(x + 17 - font.width(text) * BIG_COUNT_SCALE, y + 16 - 7 * BIG_COUNT_SCALE, 200);
		graphics.pose().scale(BIG_COUNT_SCALE, BIG_COUNT_SCALE, 1);
		graphics.drawString(font, text, 0, 0, 0xFFFFFF, true);
		graphics.pose().popPose();
	}

	/** A tooltip line with a shortened count's exact number ("4,096 items"), or null if it isn't shortened. */
	@Nullable
	public static Component exactCount(ItemStack stack) {
		return stack.getCount() < 1000 ? null : Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.item_count",
			String.format(Locale.ROOT, "%,d", stack.getCount())).withStyle(ChatFormatting.GRAY);
	}

	/** Draws a title centred on the header band of a panel {@code width} wide (in the screen's own coordinates). */
	public static void title(GuiGraphics graphics, Font font, Component title, int width, int colour) {
		graphics.drawString(font, title, (width - font.width(title)) / 2, 3, colour, false);
	}

	/** Where the "Inventory" label goes, for a machine panel of the given size. */
	public static int inventoryLabelX(int machineWidth) {
		return MenuLayout.inventoryLeft(machineWidth) + 8;
	}

	public static int inventoryLabelY(int machineHeight) {
		return MenuLayout.inventoryTop(machineHeight) + MenuLayout.INVENTORY_LABEL_Y;
	}

	/** A chest tier's band colour, multiplied over the texture's grey band. */
	public static int tierColour(ChestTier tier) {
		return switch (tier) {
			case COPPER -> 0xC4764E;
			case IRON -> 0xB0B2B2;
			case GOLD -> 0xD6B046;
			case DIAMOND -> 0x68B8B2;
			case NETHERITE -> 0x807478;
		};
	}

	public static int titleColour(ChestTier tier) {
		return tier == ChestTier.NETHERITE ? TITLE_LIGHT : TITLE;
	}

	/**
	 * Draws the band of a panel at (x, y) again, tinted with the tier's colour (the texture's band is grey). Must
	 * come right after the panel itself is drawn.
	 */
	public static void tintBand(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int textureWidth,
								int textureHeight, ChestTier tier) {
		int colour = tierColour(tier);
		RenderSystem.setShaderColor((colour >> 16 & 0xFF) / 255f, (colour >> 8 & 0xFF) / 255f, (colour & 0xFF) / 255f, 1);
		graphics.blit(texture, x + 1, y + 1, 1, 1, width - 2, MenuLayout.HEADER - 2, textureWidth, textureHeight);
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
}
