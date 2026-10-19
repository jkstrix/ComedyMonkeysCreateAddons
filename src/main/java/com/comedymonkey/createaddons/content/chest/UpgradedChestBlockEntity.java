package com.comedymonkey.createaddons.content.chest;

import java.util.List;

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
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

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
			if (!(player.containerMenu instanceof UpgradedChestView view))
				return false;
			Container container = view.getChestContainer();
			return container == UpgradedChestBlockEntity.this
				|| container instanceof CompoundContainer compound && compound.contains(UpgradedChestBlockEntity.this);
		}
	};

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

	/** Diamond doubles the slots, so the inventory grows or (compacting, already checked to fit) shrinks with the tier. */
	@Override
	public void setBlockState(BlockState state) {
		super.setBlockState(state);
		int slots = tierOf(state).slots();
		if (items.size() != slots)
			items = ChestStacks.resize(items, slots);
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
		return new UpgradedChestMenu(containerId, inventory, this, getContainerSize() / 9);
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
	}

	/** Keeps oversized stacks out of item components, which cannot store more than 99 per stack. */
	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		if (getCustomName() != null)
			components.set(DataComponents.CUSTOM_NAME, getCustomName());
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
