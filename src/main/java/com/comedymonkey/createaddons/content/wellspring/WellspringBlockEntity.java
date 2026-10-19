package com.comedymonkey.createaddons.content.wellspring;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class WellspringBlockEntity extends BlockEntity implements IHaveGoggleInformation, MenuProvider, Clearable {
	private static final int SYNC_INTERVAL = 10;

	private final WellspringTank tank;
	private final WellspringUpgradeInventory upgrades;

	/** Accumulates mB/s every tick; every 20 units is one millibucket generated. */
	private int generationProgress;
	private boolean syncQueued;
	private int syncCooldown;

	// Client-side smoothing of the rendered fluid level
	private float renderedFill;
	private float prevRenderedFill;
	private boolean renderInitialised;

	public WellspringBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WELLSPRING.get(), pos, state);
		tank = new WellspringTank(CMConfig.SERVER.tankCapacity(0));
		upgrades = new WellspringUpgradeInventory(this);
	}

	// ---------------------------------------------------------------- ticking

	void serverTick() {
		// Picks up config changes as well as installed/removed Capacity Upgrades
		int capacity = CMConfig.SERVER.tankCapacity(getCapacityUpgradeCount());
		if (tank.getCapacity() != capacity) {
			tank.setCapacity(capacity);
			if (tank.getFluidAmount() > capacity)
				tank.getFluid().setAmount(capacity);
			queueSync();
		}

		int rate = getGenerationRate();
		if (rate > 0 && tank.getFluidAmount() < tank.getCapacity()) {
			generationProgress += rate;
			int generated = generationProgress / 20;
			if (generated > 0) {
				generationProgress -= generated * 20;
				tank.fill(new FluidStack(getGenerationSource().fluid(), generated), IFluidHandler.FluidAction.EXECUTE);
			}
		} else {
			generationProgress = 0;
		}

		if (syncCooldown > 0)
			syncCooldown--;
		if (syncQueued && syncCooldown == 0)
			sendData();
	}

	void clientTick() {
		float target = getFillFraction();
		if (!renderInitialised) {
			renderedFill = prevRenderedFill = target;
			renderInitialised = true;
			return;
		}
		prevRenderedFill = renderedFill;
		renderedFill += (target - renderedFill) * 0.25f;
		if (Math.abs(target - renderedFill) < 0.0005f)
			renderedFill = target;
	}

	public float getRenderedFill(float partialTicks) {
		// Before the first client tick (e.g. a freshly built Ponder scene) show the real level
		if (!renderInitialised)
			return getFillFraction();
		return Mth.lerp(partialTicks, prevRenderedFill, renderedFill);
	}

	public float getFillFraction() {
		return tank.getCapacity() <= 0 ? 0 : Mth.clamp((float) tank.getFluidAmount() / tank.getCapacity(), 0, 1);
	}

	/** Whole-number fill percentage. Only reads 100 when completely full, and never 0 while holding any fluid. */
	public int getFillPercent() {
		if (tank.isEmpty())
			return 0;
		if (tank.getFluidAmount() >= tank.getCapacity())
			return 100;
		return Mth.clamp((int) (getFillFraction() * 100), 1, 99);
	}

	// ---------------------------------------------------------------- upgrades

	public WellspringUpgradeInventory getUpgrades() {
		return upgrades;
	}

	public int getGenerationUpgradeCount() {
		int count = 0;
		for (int i = 0; i < WellspringUpgradeInventory.GENERATION_SLOTS; i++)
			if (!upgrades.getStackInSlot(i).isEmpty())
				count++;
		return count;
	}

	public int getCapacityUpgradeCount() {
		int count = 0;
		for (int i = WellspringUpgradeInventory.FIRST_CAPACITY_SLOT; i < upgrades.getSlots(); i++)
			if (!upgrades.getStackInSlot(i).isEmpty())
				count++;
		return count;
	}

	/** The fluid the installed generation upgrades produce, or NONE. */
	public WellspringSource getGenerationSource() {
		for (int i = 0; i < WellspringUpgradeInventory.GENERATION_SLOTS; i++)
			if (upgrades.getStackInSlot(i).getItem() instanceof WellspringUpgradeItem item)
				return item.getUpgrade().source();
		return WellspringSource.NONE;
	}

	int getGenerationProgress() {
		return generationProgress;
	}

	void setGenerationProgress(int progress) {
		generationProgress = progress;
		setChanged();
	}

	public int getGenerationRate() {
		return CMConfig.SERVER.generationRate(getGenerationSource(), getGenerationUpgradeCount());
	}

	boolean canRemoveCapacityUpgrade() {
		return tank.getFluidAmount() <= CMConfig.SERVER.tankCapacity(getCapacityUpgradeCount() - 1);
	}

	/** Right-click with an upgrade: drop it into the first slot that takes it, or explain why not. */
	void tryInstallUpgrade(WellspringUpgradeItem item, ItemStack stack, Player player) {
		WellspringUpgrade upgrade = item.getUpgrade();

		if (upgrade == WellspringUpgrade.CAPACITY) {
			int max = CMConfig.SERVER.maxCapacityUpgrades();
			if (!upgrades.insertOne(stack)) {
				message(player, "wellspring.capacity_upgrades_full", max);
				return;
			}
			installed(stack, player);
			message(player, "wellspring.capacity_upgrade_installed", getCapacityUpgradeCount(), max,
				CMConfig.SERVER.tankCapacity(getCapacityUpgradeCount()));
			return;
		}

		WellspringSource current = getGenerationSource();
		if (current != WellspringSource.NONE && current != upgrade.source()) {
			message(player, "wellspring.wrong_upgrade_type");
			return;
		}
		if (!tank.isEmpty() && !upgrade.source().matches(tank.getFluid())) {
			message(player, "wellspring.contains_other_fluid");
			return;
		}
		int max = CMConfig.SERVER.maxUpgrades();
		if (!upgrades.insertOne(stack)) {
			message(player, "wellspring.upgrades_full", max);
			return;
		}
		installed(stack, player);
		message(player, "wellspring.upgrade_installed", getGenerationUpgradeCount(), max);
	}

	private void installed(ItemStack stack, Player player) {
		stack.consume(1, player);
		level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
	}

	void dropUpgrades() {
		if (level == null || level.isClientSide)
			return;
		for (int i = 0; i < upgrades.getSlots(); i++) {
			ItemStack stack = upgrades.getStackInSlot(i);
			if (!stack.isEmpty())
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
		}
		// Quietly: the block is being broken, so nothing may react by updating (and so re-placing) it
		upgrades.clearQuietly();
		setChanged();
	}

	void onUpgradesChanged() {
		onContentsChanged();
		if (level == null || level.isClientSide)
			return;
		// Advancements: one award per kind of upgrade fitted, with how many of it there now are
		int generation = getGenerationUpgradeCount();
		WellspringUpgrade generator = WellspringUpgrade.forSource(getGenerationSource());
		if (generation > 0 && generator != null)
			ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.WELLSPRING_UPGRADE,
				new ItemStack(generator.item()), generation);
		int capacity = getCapacityUpgradeCount();
		if (capacity > 0)
			ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.WELLSPRING_UPGRADE,
				new ItemStack(WellspringUpgrade.CAPACITY.item()), capacity);
	}

	/**
	 * Called by block movers once they have copied this block entity's data elsewhere: Sable (Create Aeronautics
	 * physics objects) when assembling or disassembling, and vanilla structure tools. Empties the original quietly
	 * so nothing is dropped or duplicated, and touches neither the block state nor the network.
	 */
	@Override
	public void clearContent() {
		upgrades.clearQuietly();
		tank.setFluid(FluidStack.EMPTY);
		generationProgress = 0;
	}

	private static void message(Player player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + "." + key, args)
			.withStyle(ChatFormatting.GOLD), true);
	}

	// ---------------------------------------------------------------- GUI

	@Override
	public Component getDisplayName() {
		return Component.translatable("block." + ComedyMonkeysCreateAddons.MOD_ID + ".wellspring");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new WellspringMenu(containerId, inventory, this);
	}

	// ---------------------------------------------------------------- fluid

	public FluidTank getTank() {
		return tank;
	}

	/** Water or lava only, and only the fluid matching any installed generation upgrades. */
	private boolean isFluidAllowed(FluidStack stack) {
		WellspringSource source = getGenerationSource();
		if (source != WellspringSource.NONE)
			return source.matches(stack);
		return WellspringSource.of(stack) != WellspringSource.NONE;
	}

	private void onContentsChanged() {
		setChanged();
		if (level == null || level.isClientSide)
			return;
		updateBlockState();
		queueSync();
	}

	private void updateBlockState() {
		// Only while this Wellspring is really in the world: setting the state of a block that is being broken or
		// replaced would put it straight back
		if (isRemoved() || !(level.getBlockState(worldPosition).getBlock() instanceof WellspringBlock))
			return;
		WellspringSource source = getGenerationSource();
		WellspringSource displayed = source != WellspringSource.NONE ? source : WellspringSource.of(tank.getFluid());
		int lamps = Math.min(getGenerationUpgradeCount(), 3);

		BlockState state = getBlockState();
		if (state.getValue(WellspringBlock.SOURCE) != displayed || state.getValue(WellspringBlock.UPGRADES) != lamps)
			level.setBlock(worldPosition, state
				.setValue(WellspringBlock.SOURCE, displayed)
				.setValue(WellspringBlock.UPGRADES, lamps), 3);
	}

	private void queueSync() {
		syncQueued = true;
	}

	private void sendData() {
		syncQueued = false;
		syncCooldown = SYNC_INTERVAL;
		BlockState state = getBlockState();
		level.sendBlockUpdated(worldPosition, state, state, 3);
		level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
	}

	// ---------------------------------------------------------------- goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.wellspring").forGoggles(tooltip);

		// Upgrades
		WellspringSource source = getGenerationSource();
		int generation = getGenerationUpgradeCount();
		int capacityUpgrades = getCapacityUpgradeCount();
		if (generation + capacityUpgrades == 0) {
			lang().translate("gui.goggles.no_upgrades").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		} else {
			lang().translate("gui.goggles.upgrades").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			WellspringUpgrade generator = WellspringUpgrade.forSource(source);
			if (generation > 0 && generator != null)
				upgradeLine(tooltip, generation, CMConfig.SERVER.maxUpgrades(),
					generator.getSerializedName());
			if (capacityUpgrades > 0)
				upgradeLine(tooltip, capacityUpgrades, CMConfig.SERVER.maxCapacityUpgrades(), "capacity");
		}

		// Generation
		if (generation > 0) {
			lang().translate("gui.goggles.generating").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			if (tank.getFluidAmount() >= tank.getCapacity())
				lang().translate("gui.goggles.full").style(ChatFormatting.RED).forGoggles(tooltip, 2);
			else
				lang().text(getGenerationRate() + " mB/s").style(ChatFormatting.AQUA).forGoggles(tooltip, 2);
		}

		// Fill level, as a percentage
		lang().translate("gui.goggles.fill").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		if (tank.isEmpty()) {
			lang().translate("gui.goggles.empty")
				.style(ChatFormatting.DARK_GRAY)
				.add(lang().text(" (" + mb(tank.getCapacity()) + " mB)").style(ChatFormatting.DARK_GRAY))
				.forGoggles(tooltip, 2);
		} else {
			lang().add(tank.getFluid().getHoverName().copy()).style(ChatFormatting.GRAY).forGoggles(tooltip, 2);
			lang().text(getFillPercent() + "%")
				.style(ChatFormatting.GOLD)
				.add(lang().text(" (" + mb(tank.getFluidAmount()) + " / " + mb(tank.getCapacity()) + " mB)")
					.style(ChatFormatting.DARK_GRAY))
				.forGoggles(tooltip, 2);
		}
		return true;
	}

	private static void upgradeLine(List<Component> tooltip, int count, int max, String type) {
		lang().text(count + " / " + max + " ")
			.style(ChatFormatting.GOLD)
			.add(lang().translate("gui.goggles.upgrade_type." + type).style(ChatFormatting.DARK_GRAY))
			.forGoggles(tooltip, 2);
	}

	public static String mb(int amount) {
		return String.format(Locale.ROOT, "%,d", amount);
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	// ---------------------------------------------------------------- persistence & sync

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		writeShared(tag, registries);
		tag.putInt("GenerationProgress", generationProgress);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("UpgradeSlots", Tag.TAG_COMPOUND))
			upgrades.deserializeNBT(registries, tag.getCompound("UpgradeSlots"));
		else if (tag.contains("Upgrades", Tag.TAG_LIST))
			loadLegacyUpgrades(tag);
		// Capacity first, so a full tank is not clipped to the base capacity when read back
		if (tag.contains("Capacity"))
			tank.setCapacity(tag.getInt("Capacity"));
		tank.readFromNBT(registries, tag.getCompound("Tank"));
		generationProgress = tag.getInt("GenerationProgress");
	}

	/** Worlds from before the upgrade GUI stored upgrades as a list of names. */
	private void loadLegacyUpgrades(CompoundTag tag) {
		int generationSlot = 0;
		int capacitySlot = WellspringUpgradeInventory.FIRST_CAPACITY_SLOT;
		for (Tag entry : tag.getList("Upgrades", Tag.TAG_STRING)) {
			WellspringUpgrade upgrade = WellspringUpgrade.byName(entry.getAsString());
			if (upgrade == null)
				continue;
			ItemStack stack = new ItemStack(upgrade.item());
			if (upgrade.isGeneration() && generationSlot < WellspringUpgradeInventory.GENERATION_SLOTS)
				upgrades.setStackInSlot(generationSlot++, stack);
			else if (!upgrade.isGeneration() && capacitySlot < upgrades.getSlots())
				upgrades.setStackInSlot(capacitySlot++, stack);
		}
	}

	private void writeShared(CompoundTag tag, HolderLookup.Provider registries) {
		tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
		tag.putInt("Capacity", tank.getCapacity());
		tag.put("UpgradeSlots", upgrades.serializeNBT(registries));
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		writeShared(tag, registries);
		return tag;
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	private class WellspringTank extends FluidTank {
		WellspringTank(int capacity) {
			super(capacity, WellspringBlockEntity.this::isFluidAllowed);
		}

		@Override
		protected void onContentsChanged() {
			WellspringBlockEntity.this.onContentsChanged();
		}
	}
}
