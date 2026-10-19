package com.comedymonkey.createaddons.content.chest;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Game events for upgraded chests. Registered on the NeoForge event bus. */
public final class ChestEvents {
	private static final String CARRY_ON_PACKAGE = "tschipp.carryon.";
	private static final StackWalker WALKER = StackWalker.getInstance();

	private ChestEvents() {}

	/**
	 * Sneak + right-click the side of a single upgraded chest with a chest in hand to extend it into a double chest.
	 * Handled before the chest item's own placement (which, while sneaking, would just place a separate chest).
	 */
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack stack = event.getItemStack();
		if (!event.getEntity().isShiftKeyDown() || !stack.is(Items.CHEST) || event.getFace() == null)
			return;
		if (ChestUpgrades.extendToDouble(event.getLevel(), event.getPos(), event.getFace(), event.getEntity(), stack)) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
		}
	}

	/**
	 * Carry On picking up one half of an upgraded double chest. The chest has one upgrade, so after the split one
	 * half stays upgraded and the other becomes a vanilla chest. The upgrade goes to whichever half needs it; if
	 * neither half would fit in a vanilla chest the pickup is refused. (Carry On asks permission through a break
	 * event before taking the block, and saves the block entity again afterwards, so the change is carried along.)
	 */
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (!(event.getLevel() instanceof Level level) || level.isClientSide)
			return;
		BlockPos pos = event.getPos();
		BlockState state = event.getState();
		if (!(state.getBlock() instanceof UpgradedChestBlock))
			return;
		List<BlockPos> parts = ChestUpgrades.halves(level, pos, state);
		if (parts.size() != 2 || !calledFromCarryOn())
			return;
		if (!(level.getBlockEntity(pos) instanceof UpgradedChestBlockEntity carried)
			|| !(level.getBlockEntity(parts.get(1)) instanceof UpgradedChestBlockEntity remaining))
			return;

		boolean carriedFits = ChestStacks.fits(carried.getItems(), 1, ChestStacks.SLOTS);
		boolean remainingFits = ChestStacks.fits(remaining.getItems(), 1, ChestStacks.SLOTS);
		if (!carriedFits && !remainingFits) {
			event.setCanceled(true);
			event.getPlayer().displayClientMessage(Component.translatable(
				ComedyMonkeysCreateAddons.MOD_ID + ".chest.cannot_split").withStyle(ChatFormatting.GOLD), true);
			return;
		}
		// The half that wouldn't fit a vanilla chest keeps the upgrade; if both fit, it stays with the half left behind
		boolean remainingKeeps = !remainingFits || carriedFits;
		remaining.setHoldsUpgrade(remainingKeeps);
		carried.setHoldsUpgrade(!remainingKeeps);
	}

	private static boolean calledFromCarryOn() {
		return WALKER.walk(frames -> frames.limit(64).anyMatch(f -> f.getClassName().startsWith(CARRY_ON_PACKAGE)));
	}
}
