package com.comedymonkey.createaddons.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.comedymonkey.createaddons.content.tradebot.board.TradeBoardEntity;
import com.comedymonkey.createaddons.content.tradebot.board.TradeBoards;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Factory Gauges on a Trade Board's face (design/specs/trade-master.md D31, D41): the gauge counts only the container
 * the board hangs on, and Create never orders anything for it; the Trade Master fills it. Gauges anywhere else stay
 * Create's. Whether a gauge is on a board is looked up once a second, not every tick.
 */
@Mixin(FactoryPanelBehaviour.class)
public abstract class FactoryPanelBehaviourMixin {
	@Unique
	private long comedymonkeysaddons$boardCheckedAt = Long.MIN_VALUE;
	@Unique
	@Nullable
	private TradeBoardEntity comedymonkeysaddons$board;

	/** On a board: the container behind it, as the gauge's stock. */
	@Inject(method = "getRelevantSummary", at = @At("HEAD"), cancellable = true, require = 1)
	private void comedymonkeysaddons$countTheBoardContainer(CallbackInfoReturnable<InventorySummary> cir) {
		TradeBoardEntity board = comedymonkeysaddons$board();
		if (board == null)
			return;
		InventorySummary summary = new InventorySummary();
		IItemHandler container = board.container();
		if (container != null)
			for (int slot = 0; slot < container.getSlots(); slot++)
				if (!container.getStackInSlot(slot).isEmpty())
					summary.add(container.getStackInSlot(slot));
		cir.setReturnValue(summary);
	}

	/** On a board: no orders to Create's network; the Trade Master trades for it instead. */
	@Inject(method = "tickRequests", at = @At("HEAD"), cancellable = true, require = 1)
	private void comedymonkeysaddons$noOrdersOnABoard(CallbackInfo ci) {
		if (comedymonkeysaddons$board() != null)
			ci.cancel();
	}

	/** The board this gauge sits in (its block's space, on the same face), server side; null if none. */
	@Unique
	@Nullable
	private TradeBoardEntity comedymonkeysaddons$board() {
		BlockEntityBehaviour self = (BlockEntityBehaviour) (Object) this;
		Level level = self.getWorld();
		if (level == null || level.isClientSide)
			return null;
		long now = level.getGameTime();
		TradeBoardEntity cached = comedymonkeysaddons$board;
		// (not now - checkedAt: from Long.MIN_VALUE that overflows, and it would never look)
		if (now >= comedymonkeysaddons$boardCheckedAt + 20 || cached != null && !cached.isAlive()) {
			comedymonkeysaddons$boardCheckedAt = now;
			BlockState state = self.blockEntity.getBlockState();
			comedymonkeysaddons$board = state.getBlock() instanceof FactoryPanelBlock
				? TradeBoards.at(level, self.getPos(), FactoryPanelBlock.connectedDirection(state)) : null;
		}
		return comedymonkeysaddons$board;
	}
}
