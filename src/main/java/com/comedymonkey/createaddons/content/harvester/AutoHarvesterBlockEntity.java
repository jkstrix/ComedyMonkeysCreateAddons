package com.comedymonkey.createaddons.content.harvester;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.FixedStress;
import com.comedymonkey.createaddons.content.MachineEnchantments;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Grows the planted crop or sapling for a fixed time, then runs one harvest: the head drops, sweeps the planter
 * with its reel (crops) or saw (trees), lifts and parks. The cut puts one harvest's drops
 * ({@link HarvesterPlants#harvest}) into a nine-slot buffer that automation empties from any side.
 * <p>
 * Speed doesn't matter: any rotation runs it at the same pace, and it always puts the same stress on the network
 * (its impact per RPM is the configured SU divided by the speed). Only the Bountiful enchantment, carried over from
 * the item it was placed from, makes it grow faster. It puts no stress on the network while empty, or while it
 * holds a harvest it has no room for.
 */
public class AutoHarvesterBlockEntity extends KineticBlockEntity implements Clearable, MenuProvider {
	/** Length of one harvest, start to parked again. */
	public static final int HARVEST_TICKS = 80;
	/** When in the harvest the plant is cut: the end of the sweep. */
	public static final int CUT_TICK = 52;
	private static final int OUTPUT_SLOTS = 9;
	private static final int SYNC_INTERVAL = 10;

	private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
		@Override
		protected void onContentsChanged(int slot) {
			onOutputChanged();
		}
	};
	private final IItemHandler automation = new AutomationHandler();
	private final IItemHandlerModifiable planter = new PlanterHandler();
	/** A harvest that didn't fit; nothing more is grown until it has gone into the buffer. */
	private final List<ItemStack> pending = new ArrayList<>();
	private ItemStack plant = ItemStack.EMPTY;
	private ItemEnchantments enchantments = ItemEnchantments.EMPTY;
	private int growth;
	/** Ticks into the current harvest, or -1 while growing. */
	private int harvestTick = -1;
	/** Grow time as the server worked it out (with Bountiful), so the client draws the same growth. */
	private int growTicks = 1;
	/** The client's copy of whether {@link #pending} holds anything. */
	private boolean blocked;
	private boolean syncQueued;

	public AutoHarvesterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.AUTO_HARVESTER.get(), pos, state);
	}

	// ---------------------------------------------------------------- planting

	public boolean hasPlant() {
		return !plant.isEmpty();
	}

	/** Ponder only: this planted, grown this far (0 to 1), not harvesting. */
	public void setPonderPlant(ItemStack stack, float grown) {
		plant = stack.copyWithCount(1);
		growTicks = computeGrowTicks();
		growth = Math.round(grown * growTicks);
		harvestTick = -1;
	}

	/** Ponder only: fully grown, and the head starts its sweep now. */
	public void startPonderHarvest() {
		growth = growTicks;
		harvestTick = 0;
	}

	/** What's planted (one item), or empty. */
	public ItemStack getPlant() {
		return plant;
	}

	public PlantKind kind() {
		return HarvesterPlants.kindOf(plant);
	}

	void plant(ItemStack stack) {
		plant = stack.copyWithCount(1);
		growth = 0;
		harvestTick = -1;
		level.playSound(null, worldPosition, kind() == PlantKind.TREE ? SoundEvents.GRASS_PLACE : SoundEvents.CROP_PLANTED,
			SoundSource.BLOCKS, 1, 1);
		plantChanged();
	}

	ItemStack unplant() {
		ItemStack out = plant;
		plant = ItemStack.EMPTY;
		growth = 0;
		harvestTick = -1;
		plantChanged();
		return out;
	}

	private void plantChanged() {
		growTicks = computeGrowTicks();
		BlockState state = getBlockState();
		PlantKind kind = kind();
		if (state.hasProperty(AutoHarvesterBlock.PLANT) && state.getValue(AutoHarvesterBlock.PLANT) != kind)
			level.setBlock(worldPosition, state.setValue(AutoHarvesterBlock.PLANT, kind), 3);
		setChanged();
		sendData();
	}

	/** The plant's grow time in ticks, sped up by Bountiful. */
	private int computeGrowTicks() {
		boolean loaded = CMConfig.SERVER_SPEC.isLoaded();
		var seconds = kind() == PlantKind.TREE ? CMConfig.SERVER.harvesterTreeSeconds : CMConfig.SERVER.harvesterCropSeconds;
		float base = 20 * (loaded ? seconds.get() : seconds.getDefault());
		if (kind() == PlantKind.CROP)
			base *= HarvesterPlants.growthFactor(plant);
		return Math.max(20, Math.round(base / MachineEnchantments.speedMultiplier(getBountifulLevel(),
			CMConfig.SERVER.harvesterBountifulBonus)));
	}

	public int getBountifulLevel() {
		return MachineEnchantments.levelIn(enchantments, MachineEnchantments.BOUNTIFUL);
	}

	// ---------------------------------------------------------------- running

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			if (isRunning())
				clientTick();
			return;
		}

		flushPending();
		keepStressFixed();
		if (isRunning())
			work();

		if (syncQueued && level.getGameTime() % SYNC_INTERVAL == 0) {
			syncQueued = false;
			sendData();
		}
	}

	private void work() {
		if (harvestTick < 0) {
			if (++growth >= growTicks) {
				growth = growTicks;
				harvestTick = 0;
				sendData();
			}
			return;
		}
		if (++harvestTick == CUT_TICK)
			harvest();
		if (harvestTick >= HARVEST_TICKS) {
			harvestTick = -1;
			setChanged();
			sendData();
		}
	}

	private void harvest() {
		growth = 0;
		if (level instanceof ServerLevel server)
			for (ItemStack drop : HarvesterPlants.harvest(plant, server, worldPosition)) {
				ItemStack left = ItemHandlerHelper.insertItemStacked(output, drop, false);
				if (!left.isEmpty())
					pending.add(left);
			}
		level.playSound(null, worldPosition, kind() == PlantKind.TREE ? SoundEvents.WOOD_BREAK : SoundEvents.CROP_BREAK,
			SoundSource.BLOCKS, 0.6f, 0.9f + level.random.nextFloat() * 0.2f);
		ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.HARVESTER_HARVEST, plant, 0);
		blocked = !pending.isEmpty();
		setChanged();
		sendData();
	}

	/** Moves a harvest that didn't fit into the buffer as room frees up. */
	private void flushPending() {
		if (pending.isEmpty())
			return;
		for (int i = 0; i < pending.size(); i++)
			pending.set(i, ItemHandlerHelper.insertItemStacked(output, pending.get(i), false));
		pending.removeIf(ItemStack::isEmpty);
		if (pending.isEmpty()) {
			blocked = false;
			sendData();
		}
	}

	/** The client runs the same clock, so the window shows the plant growing and the head moving in step. */
	private void clientTick() {
		if (harvestTick < 0) {
			growth = Math.min(growth + 1, growTicks);
			return;
		}
		if (harvestTick < HARVEST_TICKS - 1 && ++harvestTick == CUT_TICK) {
			cutParticles();
			growth = 0;
		}
	}

	/** Turning, not overstressed, with something planted and room for its harvest. */
	public boolean isRunning() {
		return getSpeed() != 0 && !isOverStressed() && !isIdle();
	}

	/** Nothing planted, or grown but with no room for the last harvest: no work and no load on the network. */
	public boolean isIdle() {
		return !hasPlant() || (blocked && harvestTick < 0);
	}

	// ---------------------------------------------------------------- stress: fixed SU at any speed

	@Override
	public float calculateStressApplied() {
		float impact = wantedImpact();
		lastStressApplied = impact;
		return impact;
	}

	/** Impact per RPM that comes to the configured SU at the current speed; none while idle. */
	private float wantedImpact() {
		return FixedStress.impact(this, stressSU(), isIdle());
	}

	private static float stressSU() {
		return CMConfig.SERVER_SPEC.isLoaded() ? CMConfig.SERVER.harvesterStress.get() : CMConfig.SERVER.harvesterStress.getDefault();
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (level != null && !level.isClientSide)
			keepStressFixed();
	}

	/** Keeps the network's copy of our impact right for the current speed (see {@link FixedStress#keep}). */
	private void keepStressFixed() {
		if (FixedStress.keep(this, wantedImpact()))
			sendData();
	}

	// ---------------------------------------------------------------- what the renderer shows

	/** How far into the harvest the head is, 0 to 1, or -1 while it's parked and the plant grows. */
	public float getHarvestProgress(float partialTicks) {
		if (harvestTick < 0)
			return -1;
		float tick = harvestTick + (isRunning() ? partialTicks : 0);
		return Math.min(1, tick / HARVEST_TICKS);
	}

	/** How grown the plant is, 0 to 1 (0 just after it's been cut). */
	public float getGrowth(float partialTicks) {
		if (!hasPlant())
			return 0;
		float grown = growth + (isRunning() && harvestTick < 0 ? partialTicks : 0);
		return Math.min(1, grown / Math.max(1, growTicks));
	}

	/** Bits of the plant flying where it was cut (client only). */
	private void cutParticles() {
		if (plant.isEmpty())
			return;
		Direction front = getBlockState().getValue(AutoHarvesterBlock.FACING);
		ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, plant);
		for (int i = 0; i < 10; i++) {
			// chamber coordinates are modelled with the front facing north; turn them to the actual front
			Vec3 local = new Vec3(0.3 + level.random.nextDouble() * 0.4, 0.22 + level.random.nextDouble() * 0.25,
				0.3 + level.random.nextDouble() * 0.4);
			Vec3 at = VecHelper.rotateCentered(local, -(front.toYRot() + 180), Direction.Axis.Y);
			level.addParticle(particle, worldPosition.getX() + at.x, worldPosition.getY() + at.y, worldPosition.getZ() + at.z,
				(level.random.nextDouble() - 0.5) * 0.08, 0.04, (level.random.nextDouble() - 0.5) * 0.08);
		}
	}

	// ---------------------------------------------------------------- the wrench screen

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new AutoHarvesterMenu(containerId, inventory, this);
	}

	/**
	 * The screen finds this block by its position, sent with every opening however it's opened (a spectator's
	 * right-click or another mod calling openMenu sends nothing else).
	 */
	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(worldPosition);
	}

	/** The planter as one slot for the screen: take the plant out, or put one in while it's empty. */
	public IItemHandlerModifiable getPlanter() {
		return planter;
	}

	public IItemHandlerModifiable getOutput() {
		return output;
	}

	/** One line on what it's doing, for the screen. */
	public Component getStatus() {
		if (!hasPlant())
			return lang().translate("gui.goggles.auto_harvester.empty").component();
		if (blocked && harvestTick < 0)
			return lang().translate("gui.goggles.auto_harvester.full").component();
		if (getSpeed() == 0 || isOverStressed())
			return lang().translate("gui.goggles.auto_harvester.idle").component();
		if (harvestTick >= 0)
			return lang().translate("gui.goggles.auto_harvester.harvesting").component();
		return lang().translate("gui.goggles.auto_harvester.growing", Math.round(100 * getGrowth(0))).component();
	}

	/** Seconds from one harvest to the next, growing and cutting. */
	public float getSecondsPerHarvest() {
		return (growTicks + HARVEST_TICKS) / 20f;
	}

	public boolean isWorking() {
		return hasPlant() && !(blocked && harvestTick < 0) && getSpeed() != 0 && !isOverStressed();
	}

	// ---------------------------------------------------------------- output

	public IItemHandler getAutomationHandler() {
		return automation;
	}

	public boolean hasOutput() {
		for (int i = 0; i < OUTPUT_SLOTS; i++)
			if (!output.getStackInSlot(i).isEmpty())
				return true;
		return false;
	}

	void giveOutputTo(Player player) {
		for (int i = 0; i < OUTPUT_SLOTS; i++) {
			ItemStack stack = output.extractItem(i, Integer.MAX_VALUE, false);
			if (!stack.isEmpty())
				player.getInventory().placeItemBackInInventory(stack);
		}
	}

	void dropContents() {
		if (level == null || level.isClientSide)
			return;
		List<ItemStack> all = new ArrayList<>(pending);
		for (int i = 0; i < OUTPUT_SLOTS; i++)
			all.add(output.getStackInSlot(i));
		all.add(plant);
		for (ItemStack stack : all)
			if (!stack.isEmpty())
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
		clearContent();
	}

	int getComparatorOutput() {
		return ItemHandlerHelper.calcRedstoneFromInventory(output);
	}

	private void onOutputChanged() {
		setChanged();  // which also updates the comparators
		syncQueued = true;
	}

	/** Called by block movers once they've copied this block entity elsewhere, so nothing is dropped twice. */
	@Override
	public void clearContent() {
		for (int i = 0; i < OUTPUT_SLOTS; i++)
			output.setStackInSlot(i, ItemStack.EMPTY);
		pending.clear();
		plant = ItemStack.EMPTY;
	}

	private int storedCount() {
		int count = 0;
		for (int i = 0; i < OUTPUT_SLOTS; i++)
			count += output.getStackInSlot(i).getCount();
		return count;
	}

	// ---------------------------------------------------------------- the item's enchantments

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		enchantments = input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		growTicks = computeGrowTicks();
	}

	/** So the block breaks back into the enchanted item (see its loot table's copy_components). */
	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!enchantments.isEmpty())
			components.set(DataComponents.ENCHANTMENTS, enchantments);
	}

	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		super.removeComponentsFromTag(tag);
		tag.remove("Enchantments");
	}

	// ---------------------------------------------------------------- goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.auto_harvester").forGoggles(tooltip);
		if (!hasPlant()) {
			lang().translate("gui.goggles.auto_harvester.empty").style(ChatFormatting.RED).forGoggles(tooltip, 1);
		} else {
			lang().add(plant.getHoverName().copy()).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
			if (blocked && harvestTick < 0)
				lang().translate("gui.goggles.auto_harvester.full").style(ChatFormatting.RED).forGoggles(tooltip, 1);
			else if (getSpeed() == 0 || isOverStressed())
				lang().translate("gui.goggles.auto_harvester.idle").style(ChatFormatting.RED).forGoggles(tooltip, 1);
			else if (harvestTick >= 0)
				lang().translate("gui.goggles.auto_harvester.harvesting").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
			else
				lang().translate("gui.goggles.auto_harvester.growing", Math.round(100 * getGrowth(0)))
					.style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
			lang().text(String.format(Locale.ROOT, "%.1f", getSecondsPerHarvest()) + " ").style(ChatFormatting.AQUA)
				.add(lang().translate("gui.goggles.auto_harvester.per_harvest").style(ChatFormatting.GRAY))
				.forGoggles(tooltip, 1);
		}
		int bountiful = getBountifulLevel();
		if (bountiful > 0)
			lang().translate("gui.goggles.auto_harvester.bountiful", Component.translatable("enchantment.level." + bountiful))
				.style(ChatFormatting.LIGHT_PURPLE).forGoggles(tooltip, 1);
		int stored = storedCount();
		if (stored > 0)
			lang().translate("gui.goggles.auto_harvester.stored", stored).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	// ---------------------------------------------------------------- persistence

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (!plant.isEmpty())
			tag.put("Plant", plant.save(registries));
		tag.putInt("Growth", growth);
		tag.putInt("HarvestTick", harvestTick);
		tag.putInt("GrowTicks", growTicks);
		tag.putBoolean("Blocked", blocked);
		tag.put("Output", output.serializeNBT(registries));
		if (!pending.isEmpty()) {
			ListTag list = new ListTag();
			for (ItemStack stack : pending)
				list.add(stack.save(registries));
			tag.put("Pending", list);
		}
		if (!enchantments.isEmpty())
			ItemEnchantments.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), enchantments)
				.ifSuccess(encoded -> tag.put("Enchantments", encoded));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		plant = ItemStack.parseOptional(registries, tag.getCompound("Plant"));
		growth = tag.getInt("Growth");
		harvestTick = tag.contains("HarvestTick") ? tag.getInt("HarvestTick") : -1;
		growTicks = Math.max(1, tag.getInt("GrowTicks"));
		output.deserializeNBT(registries, tag.getCompound("Output"));
		pending.clear();
		for (Tag entry : tag.getList("Pending", Tag.TAG_COMPOUND))
			ItemStack.parse(registries, entry).ifPresent(pending::add);
		blocked = clientPacket ? tag.getBoolean("Blocked") : !pending.isEmpty();
		enchantments = tag.contains("Enchantments")
			? ItemEnchantments.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Enchantments"))
				.result().orElse(ItemEnchantments.EMPTY)
			: ItemEnchantments.EMPTY;
		// The config may have changed while it was unloaded
		if (!clientPacket && CMConfig.SERVER_SPEC.isLoaded())
			growTicks = computeGrowTicks();
	}

	/** Hands out a copy, so shift-clicking the plant out still goes through {@link #unplant}. */
	private class PlanterHandler implements IItemHandlerModifiable {
		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return plant.copy();
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			if (level == null || level.isClientSide) {
				plant = stack.copyWithCount(Math.min(1, stack.getCount()));
				return;
			}
			if (stack.isEmpty())
				unplant();
			else if (ItemStack.isSameItemSameComponents(stack, plant))
				return;
			else
				plant(stack);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (hasPlant() || HarvesterPlants.kindOf(stack) == PlantKind.NONE)
				return stack;
			if (!simulate)
				setStackInSlot(slot, stack);
			return stack.copyWithCount(stack.getCount() - 1);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (amount <= 0 || !hasPlant())
				return ItemStack.EMPTY;
			ItemStack out = plant.copy();
			if (!simulate)
				setStackInSlot(slot, ItemStack.EMPTY);
			return out;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return HarvesterPlants.kindOf(stack) != PlantKind.NONE;
		}
	}

	/**
	 * Automation: the harvest comes out of slots 0-8 from any side; slot 9 is the planter, which takes a seed or
	 * sapling while it's empty (so a funnel or arm can plant it) and gives nothing back.
	 */
	private class AutomationHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return OUTPUT_SLOTS + 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return slot < OUTPUT_SLOTS ? output.getStackInSlot(slot) : plant;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (slot != OUTPUT_SLOTS || hasPlant() || HarvesterPlants.kindOf(stack) == PlantKind.NONE)
				return stack;
			if (!simulate)
				plant(stack);
			return stack.copyWithCount(stack.getCount() - 1);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return slot < OUTPUT_SLOTS ? output.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return slot < OUTPUT_SLOTS ? 64 : 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot == OUTPUT_SLOTS && !hasPlant() && HarvesterPlants.kindOf(stack) != PlantKind.NONE;
		}
	}
}
