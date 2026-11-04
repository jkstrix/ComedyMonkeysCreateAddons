package com.comedymonkey.createaddons.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.comedymonkey.createaddons.client.tradebot.TradeBoardGoggles;
import com.comedymonkey.createaddons.content.tradebot.board.TradeBoardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Goggles on a Trade Board (design/specs/trade-master.md H8): Create's overlay only reads blocks, and a board is an
 * entity hung on one, so looking at one with goggles on draws its lines here the way Create draws a block's: the same
 * box, colours, offset, icon and fade-in. Anything else is Create's as ever.
 */
@Mixin(GoggleOverlayRenderer.class)
public abstract class GoggleOverlayRendererMixin {
	@Unique
	private static final ResourceLocation GOGGLES = ResourceLocation.fromNamespaceAndPath("create", "goggles");

	@Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
	private static void comedymonkeysaddons$tradeBoard(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.player == null || mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR
			|| !(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof TradeBoardEntity board)
			|| !GogglesItem.isWearingGoggles(mc.player))
			return;
		ci.cancel();
		if (!board.getPos().equals(GoggleOverlayRenderer.lastHovered))
			GoggleOverlayRenderer.hoverTicks = 0;
		GoggleOverlayRenderer.hoverTicks++;
		GoggleOverlayRenderer.lastHovered = board.getPos();
		List<Component> tooltip = TradeBoardGoggles.tooltip(board);

		int textWidth = 0;
		for (Component line : tooltip)
			textWidth = Math.max(textWidth, mc.font.width(line));
		int tooltipHeight = 8 + (tooltip.size() > 1 ? 2 + (tooltip.size() - 1) * 10 : 0);
		int width = graphics.guiWidth(), height = graphics.guiHeight();
		CClient cfg = AllConfigs.client();
		int x = Math.min(width / 2 + cfg.overlayOffsetX.get(), width - textWidth - 20);
		int y = Math.min(height / 2 + cfg.overlayOffsetY.get(), height - tooltipHeight - 20);
		float fade = Mth.clamp((GoggleOverlayRenderer.hoverTicks + delta.getGameTimeDeltaPartialTick(false)) / 24f, 0, 1);
		boolean custom = cfg.overlayCustomColor.get();
		Color background = custom ? new Color(cfg.overlayBackgroundColor.get()) : BoxElement.COLOR_VANILLA_BACKGROUND.scaleAlpha(.75f);
		Color top = custom ? new Color(cfg.overlayBorderColorTop.get()) : BoxElement.COLOR_VANILLA_BORDER.getFirst().copy();
		Color bottom = custom ? new Color(cfg.overlayBorderColorBot.get()) : BoxElement.COLOR_VANILLA_BORDER.getSecond().copy();

		PoseStack pose = graphics.pose();
		pose.pushPose();
		if (fade < 1) {
			pose.translate(Math.pow(1 - fade, 3) * Math.signum(cfg.overlayOffsetX.get() + .5f) * 8, 0, 0);
			background.scaleAlpha(fade);
			top.scaleAlpha(fade);
			bottom.scaleAlpha(fade);
		}
		GuiGameElement.of(new ItemStack(BuiltInRegistries.ITEM.get(GOGGLES))).at(x + 10, y - 16, 450).render(graphics);
		RemovedGuiUtils.drawHoveringText(graphics, tooltip, x, y, width, height, -1, background.getRGB(), top.getRGB(), bottom.getRGB(), mc.font);
		pose.popPose();
	}
}
