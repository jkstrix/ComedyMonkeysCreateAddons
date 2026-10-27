package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.comedymonkey.createaddons.content.helperbot.HelperBots;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Helper Bot keeps shop (spec D35): Create only counts a seated mob or a Blaze Burner beside a Stock Ticker as its
 * keeper. Everything that needs a keeper (the ticker, table cloths, the shop's menu) asks this one method.
 */
@Mixin(StockTickerBlockEntity.class)
public abstract class StockTickerBlockEntityMixin {
	@Inject(method = "isKeeperPresent", at = @At("RETURN"), cancellable = true, require = 1)
	private void comedymonkeysaddons$helperBotKeepsShop(CallbackInfoReturnable<Boolean> cir) {
		BlockEntity self = (BlockEntity) (Object) this;
		if (!cir.getReturnValueZ() && self.getLevel() != null && HelperBots.keepsShopFor(self.getLevel(), self.getBlockPos()))
			cir.setReturnValue(true);
	}

	/** A player's order went out from this ticker: a Helper Bot keeping shop here nods. */
	@Inject(method = "broadcastPackageRequest", at = @At("RETURN"), require = 1)
	private void comedymonkeysaddons$helperBotNods(RequestType type, PackageOrderWithCrafts order, IdentifiedInventory ignoredHandler, String address,
		CallbackInfoReturnable<Boolean> cir) {
		BlockEntity self = (BlockEntity) (Object) this;
		if (type == RequestType.PLAYER && cir.getReturnValueZ() && self.getLevel() != null && !self.getLevel().isClientSide)
			HelperBots.orderPlacedAt(self.getLevel(), self.getBlockPos());
	}
}
