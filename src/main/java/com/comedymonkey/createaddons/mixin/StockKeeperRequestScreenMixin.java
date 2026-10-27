package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.comedymonkey.createaddons.content.helperbot.HelperBots;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;

import java.lang.ref.WeakReference;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Create's shop screen and a Helper Bot keeping shop (spec D35): it closes itself each tick it can't find its keeper (a
 * seated mob or a Blaze Burner beside the ticker), so a Helper Bot there counts too; and he's drawn in the keeper's spot.
 */
@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {
	@Shadow(remap = false)
	StockTickerBlockEntity blockEntity;
	@Shadow(remap = false)
	int windowHeight;
	@Shadow(remap = false)
	WeakReference<LivingEntity> stockKeeper;
	@Shadow(remap = false)
	WeakReference<BlazeBurnerBlockEntity> blaze;

	/** Draws the Helper Bot keeping shop where Create draws a Blaze Burner keeper, unless Create is drawing one there. */
	@Inject(method = "renderBg", at = @At("TAIL"), require = 1)
	private void comedymonkeysaddons$drawHelperBot(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
		if (blockEntity == null || blockEntity.getLevel() == null || Minecraft.getInstance().screen != (Object) this)
			return;
		LivingEntity keeper = stockKeeper.get();
		BlazeBurnerBlockEntity burner = blaze.get();
		if (keeper != null && keeper.isAlive() || burner != null && !burner.isRemoved()
			|| !HelperBots.keepsShopFor(blockEntity.getLevel(), blockEntity.getBlockPos()))
			return;
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
		com.comedymonkey.createaddons.client.helperbot.HelperBotClient.drawInShop(graphics, blockEntity.getBlockPos(), screen.getGuiLeft(),
			screen.getGuiTop(), windowHeight, partialTicks);
	}

	@WrapWithCondition(method = "containerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;closeContainer()V"), require = 1)
	private boolean comedymonkeysaddons$keepOpenForHelperBot(Player player) {
		return blockEntity == null || blockEntity.getLevel() == null || !HelperBots.keepsShopFor(blockEntity.getLevel(), blockEntity.getBlockPos());
	}
}
