package com.comedymonkey.createaddons.content.chest;

import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Upgrade GUI for an upgraded chest or barrel (wrench, or sneak + right-click with an upgrade): just the upgrade slot, holding
 * the chest's one upgrade (a double chest shares one). Upgrades go in by right-clicking the chest with them; here they can be swapped (with one on the cursor) or taken out, which
 * turns the block back into a vanilla chest. Clicking or shift-clicking the slot moves the upgrade to the player's
 * inventory. Anything that would leave stacks or slots that no longer fit is refused, and the server tells the client
 * which tiers still fit so the slot never pretends otherwise.
 */
/* Just the upgrade slot: no quick moves, and Inventory Profiles Next's buttons have nothing to do here. */
@IPNIgnore
public class ChestUpgradeMenu extends AbstractContainerMenu {
	public static final int UPGRADE_X = 80;
	public static final int UPGRADE_Y = 24;
	/** In {@link #fittingTiers()}: a plain vanilla chest (or barrel) would do. Each tier has bit {@code ordinal()}. */
	public static final int VANILLA_BIT = 1 << ChestTier.values().length;

	private final Level level;
	private final Player player;
	private final BlockPos pos;
	private final int halves;
	private final SimpleContainer upgradeContainer = new SimpleContainer(1);
	/** Client copy of {@link #fittingTiers()}. */
	private int syncedFits;

	public ChestUpgradeMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, extraData.readBlockPos());
	}

	public ChestUpgradeMenu(int containerId, Inventory inventory, BlockPos pos) {
		super(ModMenus.CHEST_UPGRADE.get(), containerId);
		this.level = inventory.player.level();
		this.player = inventory.player;
		this.pos = pos;

		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof TieredStorageBlock block) {
			halves = block.parts(level, pos, state).size();
			upgradeContainer.setItem(0, new ItemStack(block.tierOf(state).upgradeItem()));
		} else {
			halves = 1;
		}
		addSlot(new UpgradeSlot());

		addDataSlot(new DataSlot() {
			@Override
			public int get() {
				return fittingTiers();
			}

			@Override
			public void set(int value) {
				syncedFits = value;
			}
		});
	}

	/** 1 for a single chest, 2 for a double (which still holds just one upgrade). */
	public int getHalves() {
		return halves;
	}

	/** Current tier (null if the block is no longer upgraded). */
	@Nullable
	public ChestTier getTier() {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof TieredStorageBlock block ? block.tierOf(state) : null;
	}

	/**
	 * Which tiers every part's contents would fit, as bits ({@code 1 << tier.ordinal()}), plus {@link #VANILLA_BIT} if
	 * a plain chest would. Checked tier by tier: the config can give a higher tier a smaller stack size. Synced.
	 */
	public int fittingTiers() {
		if (level.isClientSide)
			return syncedFits;
		int fits = ChestUpgrades.fitsEachPart(level, pos, 1, ChestStacks.SLOTS) ? VANILLA_BIT : 0;
		for (ChestTier tier : ChestTier.values())
			if (ChestUpgrades.fitsEachPart(level, pos, tier.multiplier(), tier.slots()))
				fits |= 1 << tier.ordinal();
		return fits;
	}

	public boolean canRemoveUpgrade() {
		return (fittingTiers() & VANILLA_BIT) != 0;
	}

	public boolean tierFits(ChestTier tier) {
		return (fittingTiers() & 1 << tier.ordinal()) != 0;
	}

	private boolean isValidReplacement(ItemStack stack) {
		return stack.getItem() instanceof ChestUpgradeItem upgrade
			&& tierFits(upgrade.getTier());
	}

	/**
	 * Puts the block's current upgrade back in the slot. Someone else's upgrade screen (or a right-click with an
	 * upgrade) may have changed the tier since, and the slot must never hand out an upgrade the block no longer has.
	 */
	private void refreshUpgrade() {
		if (level.isClientSide)
			return;
		BlockState state = level.getBlockState(pos);
		ItemStack current = state.getBlock() instanceof TieredStorageBlock block
			? new ItemStack(block.tierOf(state).upgradeItem()) : ItemStack.EMPTY;
		if (!ItemStack.isSameItem(current, upgradeContainer.getItem(0)))
			upgradeContainer.setItem(0, current);
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		refreshUpgrade();
		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public void broadcastChanges() {
		refreshUpgrade();
		super.broadcastChanges();
	}

	private void onUpgradeChanged() {
		if (level.isClientSide || !(level.getBlockState(pos).getBlock() instanceof TieredStorageBlock block))
			return;
		ItemStack stack = upgradeContainer.getItem(0);
		if (stack.isEmpty())
			block.revertToVanilla(level, pos);
		else if (stack.getItem() instanceof ChestUpgradeItem upgrade) {
			block.setTier(level, pos, upgrade.getTier());
			ModAdvancements.award(player, block instanceof UpgradedBarrelBlock
				? ModAdvancements.BARREL_UPGRADE : ModAdvancements.CHEST_UPGRADE, stack, upgrade.getTier().ordinal());
		}
	}

	/** Shift-click: straight into the player's inventory. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (index != 0 || !slot.hasItem() || !slot.mayPickup(player))
			return ItemStack.EMPTY;
		ItemStack taken = slot.remove(slot.getItem().getCount());
		slot.setChanged();
		player.getInventory().placeItemBackInInventory(taken);
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return level.getBlockState(pos).getBlock() instanceof TieredStorageBlock && player.canInteractWithBlock(pos, 4.0);
	}

	public class UpgradeSlot extends Slot {
		UpgradeSlot() {
			super(upgradeContainer, 0, UPGRADE_X, UPGRADE_Y);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return 1;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return isValidReplacement(stack);
		}

		/**
		 * Taking the upgrades out needs every stack and slot to fit a normal chest. Swapping is also allowed when the
		 * player is holding a replacement that still fits (vanilla checks this before a swap).
		 */
		@Override
		public boolean mayPickup(Player player) {
			return canRemoveUpgrade() || isValidReplacement(getCarried());
		}

		@Override
		public void setChanged() {
			super.setChanged();
			onUpgradeChanged();
		}
	}
}
