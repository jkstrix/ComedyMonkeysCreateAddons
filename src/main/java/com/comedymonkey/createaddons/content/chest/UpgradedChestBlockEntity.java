package com.comedymonkey.createaddons.content.chest;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

/**
 * One half (or all) of an upgraded chest. Slot count and stack limit come from the tier in the block state; items are
 * saved with separate counts so stacks above 99 survive. Being a {@link Container} it is also
 * {@link net.minecraft.world.Clearable}, which Sable uses when moving it into or out of a physics object.
 * <p>
 * While nobody has it open the block model includes the lid, so the chest looks complete everywhere (Carry On, Ponder,
 * contraptions). Opening sets the block's {@code open} state, which hides the static lid and lets
 * {@code UpgradedChestRenderer} animate it; the state is cleared once the closing animation has finished.
 */
public class UpgradedChestBlockEntity extends BaseContainerBlockEntity implements IHaveGoggleInformation, UpgradedStorage {
	private static final int LID_EVENT = 1;
	/** Vanilla's lid closes over 10 ticks; leave a little slack before showing the static lid again. */
	static final int LID_CLOSE_TICKS = 12;

	private NonNullList<ItemStack> items;
	/**
	 * Whether this half holds the chest's upgrade item. A double chest has one upgrade between its two halves; it
	 * moves to the other half if this one is broken, and a half that ends up alone without it reverts to a vanilla
	 * chest. Single chests always hold it.
	 */
	private boolean holdsUpgrade = true;
	private final UpgradedChestItemHandler itemHandler = new UpgradedChestItemHandler(this);
	/** What automation was last given (both halves of a double chest as one), and the other half in it. */
	@Nullable
	private IItemHandler automation;
	@Nullable
	private UpgradedChestBlockEntity automationPartner;
	/** Each half's slots when the combined handler was made. */
	private int automationSlots, automationPartnerSlots;
	private final ChestLidController lidController = new ChestLidController();
	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(level, pos, state, SoundEvents.CHEST_OPEN);
			setOpenState(level, pos, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(level, pos, state, SoundEvents.CHEST_CLOSE);
			// Keep the animated lid until it has swung shut, then put the static one back
			level.scheduleTick(pos, state.getBlock(), LID_CLOSE_TICKS);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount) {
			level.blockEvent(pos, state.getBlock(), LID_EVENT, openCount);
		}

		@Override
		protected boolean isOwnContainer(Player player) {
			return isOpenIn(player);
		}
	};

	@Override
	public boolean isOpenIn(Player player) {
		if (!(player.containerMenu instanceof UpgradedChestView view))
			return false;
		Container container = view.getChestContainer();
		return container == this || container instanceof CompoundContainer compound && compound.contains(this);
	}

	public UpgradedChestBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.UPGRADED_CHEST.get(), pos, state);
		items = NonNullList.withSize(tierOf(state).slots(), ItemStack.EMPTY);
	}

	private static ChestTier tierOf(BlockState state) {
		return state.hasProperty(UpgradedChestBlock.TIER) ? state.getValue(UpgradedChestBlock.TIER) : ChestTier.COPPER;
	}

	@Override
	public ChestTier getTier() {
		return tierOf(getBlockState());
	}

	@Override
	public int multiplier() {
		return getTier().multiplier();
	}

	/**
	 * What automation (hoppers, funnels, pipes, Trade Bot) sees: both halves of a double chest as one, in the menu's
	 * order (the RIGHT half first), like a vanilla double chest; a single chest is just its own slots.
	 */
	public IItemHandler getAutomationHandler() {
		if (level == null)
			return itemHandler;
		// Asked on every look by hoppers and the like, so the double chest's handler is kept until the pairing changes
		IItemHandler kept = automation;
		UpgradedChestBlockEntity partner = automationPartner;
		// Still right: a single chest's own slots, or both halves with the other half still there at the same size (a
		// combined handler counts the halves' slots when it's made, and /data can change one without a new state)
		if (kept != null && (partner == null
			|| !partner.isRemoved() && partner.items.size() == automationPartnerSlots && items.size() == automationSlots))
			return kept;
		automation = null;
		automationPartner = null;
		ChestType type = getBlockState().getValue(UpgradedChestBlock.TYPE);
		List<BlockPos> parts = ChestUpgrades.halves(level, worldPosition, getBlockState());
		if (parts.size() == 2 && level.getBlockEntity(parts.get(1)) instanceof UpgradedChestBlockEntity other) {
			automation = type == ChestType.RIGHT ? new CombinedInvWrapper(itemHandler, other.itemHandler)
				: new CombinedInvWrapper(other.itemHandler, itemHandler);
			automationPartner = other;
			automationSlots = items.size();
			automationPartnerSlots = other.items.size();
			return automation;
		}
		// A half whose other half isn't there (yet: contraptions put chests down a block at a time) isn't kept
		if (type == ChestType.SINGLE)
			automation = itemHandler;
		return itemHandler;
	}

	/** Drops the kept automation handler: the pairing, or the other half's size, has changed. */
	void forgetAutomationHandler() {
		automation = null;
		automationPartner = null;
	}

	public UpgradedChestItemHandler getItemHandler() {
		return itemHandler;
	}

	public boolean holdsUpgrade() {
		return holdsUpgrade;
	}

	void setHoldsUpgrade(boolean holdsUpgrade) {
		this.holdsUpgrade = holdsUpgrade;
		setChanged();
	}

	/**
	 * Diamond doubles the slots, so the inventory grows or (compacting, already checked to fit) shrinks with the tier.
	 * Joining or splitting a double chest, turning it, or a new tier (a new number of slots), changes what automation sees, so its
	 * cached item handlers are dropped: a combined handler works out its slot ranges once, when it's made.
	 */
	@Override
	public void setBlockState(BlockState state) {
		BlockState old = getBlockState();
		boolean repaired = old.getValue(UpgradedChestBlock.TYPE) != state.getValue(UpgradedChestBlock.TYPE)
			|| old.getValue(UpgradedChestBlock.TIER) != state.getValue(UpgradedChestBlock.TIER)
			|| old.getValue(UpgradedChestBlock.FACING) != state.getValue(UpgradedChestBlock.FACING);
		super.setBlockState(state);
		int slots = tierOf(state).slots();
		if (items.size() != slots)
			items = ChestStacks.resize(items, slots);
		if (repaired) {
			// Both halves' kept handlers counted this half's slots, in this pairing
			UpgradedChestBlockEntity was = automationPartner;
			forgetAutomationHandler();
			if (was != null)
				was.forgetAutomationHandler();
			if (level != null) {
				if (state.getValue(UpgradedChestBlock.TYPE) != ChestType.SINGLE) {
					BlockPos other = worldPosition.relative(ChestBlock.getConnectedDirection(state));
					if (level.isLoaded(other) && level.getBlockEntity(other) instanceof UpgradedChestBlockEntity partner)
						partner.forgetAutomationHandler();
				}
				level.invalidateCapabilities(worldPosition);
			}
		}
	}

	// ---------------------------------------------------------------- lid

	void clientTick() {
		lidController.tickLid();
	}

	/** 0 = closed, 1 = fully open. */
	public float getOpenness(float partialTicks) {
		return lidController.getOpenness(partialTicks);
	}

	@Override
	public boolean triggerEvent(int id, int type) {
		if (id == LID_EVENT) {
			lidController.shouldBeOpen(type > 0);
			return true;
		}
		return super.triggerEvent(id, type);
	}

	/**
	 * Opens or closes the lid for something that isn't a player (Trade Bot rummaging): the animated lid swings, and once
	 * shut the static lid comes back. A player looking in keeps it open.
	 */
	public void swingLid(boolean open) {
		if (level == null || remove || (!open && openersCounter.getOpenerCount() > 0))
			return;
		BlockState state = getBlockState();
		if (open)
			setOpenState(level, worldPosition, true);
		else
			level.scheduleTick(worldPosition, state.getBlock(), LID_CLOSE_TICKS);
		level.blockEvent(worldPosition, state.getBlock(), LID_EVENT, open ? 1 : 0);
	}

	/** Scheduled tick: drop viewers that walked away, and restore the static lid once nobody is looking. */
	void recheckOpen() {
		if (remove || level == null)
			return;
		openersCounter.recheckOpeners(level, worldPosition, getBlockState());
		if (openersCounter.getOpenerCount() == 0)
			setOpenState(level, worldPosition, false);
	}

	private static void setOpenState(Level level, BlockPos pos, boolean open) {
		BlockState state = level.getBlockState(pos);
		if (state.hasProperty(UpgradedChestBlock.OPEN) && state.getValue(UpgradedChestBlock.OPEN) != open)
			level.setBlock(pos, state.setValue(UpgradedChestBlock.OPEN, open), Block.UPDATE_CLIENTS);
	}

	private static void playSound(Level level, BlockPos pos, BlockState state, SoundEvent sound) {
		// A double chest sounds once, from its left half
		if (state.hasProperty(UpgradedChestBlock.TYPE) && state.getValue(UpgradedChestBlock.TYPE) == ChestType.RIGHT)
			return;
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.1f + 0.9f);
	}

	// ---------------------------------------------------------------- container

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public int getMaxStackSize() {
		return 64 * multiplier();
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return ChestStacks.limit(stack, multiplier());
	}

	@Override
	public NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = ChestStacks.resize(items, getTier().slots());
	}

	/** Used when converting from a vanilla chest or coming back off a contraption. */
	@Override
	public void fill(List<ItemStack> contents) {
		items = ChestStacks.resize(contents, getTier().slots());
		setChanged();
	}

	@Override
	protected Component getDefaultName() {
		return getTier().chestName();
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new UpgradedChestMenu(containerId, inventory, this, getContainerSize() / 9, getTier());
	}

	/**
	 * Opened the vanilla way (another mod calling openMenu on this block entity): send the screen what it needs, as
	 * {@link ChestUpgrades#openChestMenu} does, rather than leaving the client nothing to read.
	 */
	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(multiplier());
		buffer.writeVarInt(getTier().ordinal());
		buffer.writeVarInt(getContainerSize() / 9);
	}

	@Override
	public void startOpen(Player player) {
		if (!remove && !player.isSpectator() && level != null)
			openersCounter.incrementOpeners(player, level, worldPosition, getBlockState());
	}

	@Override
	public void stopOpen(Player player) {
		if (!remove && !player.isSpectator() && level != null)
			openersCounter.decrementOpeners(player, level, worldPosition, getBlockState());
	}

	// ---------------------------------------------------------------- persistence

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("Items", ChestStacks.save(items, registries));
		tag.putBoolean("HoldsUpgrade", holdsUpgrade);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		holdsUpgrade = !tag.contains("HoldsUpgrade") || tag.getBoolean("HoldsUpgrade");
		ListTag saved = tag.getList("Items", Tag.TAG_COMPOUND);
		// Big enough for every saved slot, in case the data arrives before the matching block state
		int size = getTier().slots();
		for (Tag entry : saved)
			if ((((CompoundTag) entry).getByte("Slot") & 255) >= size)
				size = ChestStacks.SLOTS * 2;
		items = NonNullList.withSize(size, ItemStack.EMPTY);
		ChestStacks.load(items, saved, registries);
		forgetAutomationHandler();
	}

	/** Keeps oversized stacks out of item components, which cannot store more than 99 per stack. */
	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		// Vanilla's name and lock code, but not its contents component
		super.collectImplicitComponents(components);
		components.set(DataComponents.CONTAINER, null);
	}

	// ---------------------------------------------------------------- goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).add(getTier().chestName().copy()).forGoggles(tooltip);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.chest_stack_size")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
			.text("x" + multiplier() + " ")
			.style(ChatFormatting.GOLD)
			.add(new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
				.translate("gui.goggles.chest_per_slot", 64 * multiplier())
				.style(ChatFormatting.DARK_GRAY))
			.forGoggles(tooltip, 2);
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.chest_slots", getContainerSize())
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);
		return true;
	}
}
