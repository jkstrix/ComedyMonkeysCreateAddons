package com.comedymonkey.createaddons.content.chest;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An upgraded barrel: like an upgraded chest (tiered stack size and slots, oversized stacks saved safely), but a
 * full block whose front shows open while someone is using it, like a vanilla barrel.
 */
public class UpgradedBarrelBlockEntity extends BaseContainerBlockEntity implements IHaveGoggleInformation, UpgradedStorage {
	private NonNullList<ItemStack> items;
	/** Set (not saved) once the upgrade has been taken out, just before the barrel turns back into a vanilla one. */
	private boolean upgradeTaken;
	private final UpgradedChestItemHandler itemHandler = new UpgradedChestItemHandler(this);
	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.BARREL_OPEN);
			setOpen(state, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			playSound(state, SoundEvents.BARREL_CLOSE);
			setOpen(state, false);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount) {
		}

		@Override
		protected boolean isOwnContainer(Player player) {
			return isOpenIn(player);
		}
	};

	@Override
	public boolean isOpenIn(Player player) {
		return player.containerMenu instanceof UpgradedChestView view && view.getChestContainer() == this;
	}

	public UpgradedBarrelBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.UPGRADED_BARREL.get(), pos, state);
		items = NonNullList.withSize(tierOf(state).slots(), ItemStack.EMPTY);
	}

	private static ChestTier tierOf(BlockState state) {
		return state.hasProperty(UpgradedBarrelBlock.TIER) ? state.getValue(UpgradedBarrelBlock.TIER) : ChestTier.COPPER;
	}

	@Override
	public ChestTier getTier() {
		return tierOf(getBlockState());
	}

	public UpgradedChestItemHandler getItemHandler() {
		return itemHandler;
	}

	boolean upgradeTaken() {
		return upgradeTaken;
	}

	void takeUpgrade() {
		upgradeTaken = true;
	}

	/** Diamond doubles the slots, so the inventory grows or (compacting, already checked to fit) shrinks with the tier. */
	@Override
	public void setBlockState(BlockState state) {
		super.setBlockState(state);
		int slots = tierOf(state).slots();
		if (items.size() != slots)
			items = ChestStacks.resize(items, slots);
	}

	// ---------------------------------------------------------------- opening

	void recheckOpen() {
		if (!remove && level != null)
			openersCounter.recheckOpeners(level, worldPosition, getBlockState());
	}

	private void setOpen(BlockState state, boolean open) {
		if (level != null && state.hasProperty(UpgradedBarrelBlock.OPEN))
			level.setBlock(worldPosition, state.setValue(UpgradedBarrelBlock.OPEN, open), Block.UPDATE_ALL);
	}

	private void playSound(BlockState state, SoundEvent sound) {
		if (level == null)
			return;
		Vec3i facing = state.hasProperty(UpgradedBarrelBlock.FACING)
			? state.getValue(UpgradedBarrelBlock.FACING).getNormal() : Direction.UP.getNormal();
		double x = worldPosition.getX() + 0.5 + facing.getX() / 2.0;
		double y = worldPosition.getY() + 0.5 + facing.getY() / 2.0;
		double z = worldPosition.getZ() + 0.5 + facing.getZ() / 2.0;
		level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.1f + 0.9f);
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

	@Override
	public void fill(List<ItemStack> contents) {
		items = ChestStacks.resize(contents, getTier().slots());
		setChanged();
	}

	@Override
	protected Component getDefaultName() {
		return getTier().barrelName();
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

	// ---------------------------------------------------------------- persistence

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("Items", ChestStacks.save(items, registries));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		ListTag saved = tag.getList("Items", Tag.TAG_COMPOUND);
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
		// Vanilla's name and lock code, but not its contents component
		super.collectImplicitComponents(components);
		components.set(DataComponents.CONTAINER, null);
	}

	// ---------------------------------------------------------------- goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).add(getTier().barrelName().copy()).forGoggles(tooltip);
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
