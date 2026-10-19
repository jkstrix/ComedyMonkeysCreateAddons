package com.comedymonkey.createaddons.content.crucible;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModRecipes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PressureCrucibleBlockEntity extends KineticBlockEntity implements Clearable {
	public static final int INPUT = 0;
	public static final int OUTPUT = 1;
	private static final int SYNC_INTERVAL = 4;

	private final ItemStackHandler inventory = new ItemStackHandler(2) {
		@Override
		protected void onContentsChanged(int slot) {
			onInventoryChanged();
		}
	};
	private final IItemHandler automation = new AutomationHandler();

	/** Work left on the current press, in "16 RPM ticks". */
	private float timer;
	private int maxTime;
	private boolean processing;
	private boolean syncQueued;
	/** Client only: press plate travel, 0 at the top and 1 at the bottom, with last tick's value for smoothing. */
	private float plate;
	private float prevPlate;
	@Nullable
	private RecipeHolder<CrucibleRecipe> cachedRecipe;

	public PressureCrucibleBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PRESSURE_CRUCIBLE.get(), pos, state);
	}

	// ---------------------------------------------------------------- processing

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;

		if (level.isClientSide) {
			// Mirror the server's countdown so goggles and the plate stay smooth between syncs
			if (processing && getSpeed() != 0 && timer > 0)
				timer = Math.max(0, timer - processingSpeed());
			animatePlate();
			return;
		}

		CrucibleRecipe recipe = getRecipe();
		if (recipe == null || getStatus(recipe) != Status.WORKING) {
			if (processing) {
				processing = false;
				timer = 0;
				syncQueued = true;
			}
		} else {
			if (!processing) {
				processing = true;
				maxTime = recipe.processingTime();
				timer = maxTime;
				syncQueued = true;
			}
			timer -= processingSpeed();
			if (timer <= 0)
				press(recipe);
		}

		if (syncQueued && level.getGameTime() % SYNC_INTERVAL == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** Same speed scaling as Create's Millstone: 16 RPM is 1x, 256 RPM is 16x. */
	public float processingSpeed() {
		return Mth.clamp(Math.abs(getSpeed()) / 16f, 1, 512);
	}

	private void press(CrucibleRecipe recipe) {
		inventory.extractItem(INPUT, recipe.count(), false);
		ItemStack result = recipe.result().copy();
		ItemStack output = inventory.getStackInSlot(OUTPUT);
		if (output.isEmpty())
			inventory.setStackInSlot(OUTPUT, result);
		else
			output.grow(result.getCount());
		onInventoryChanged();
		processing = false;
		timer = 0;

		AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(level, worldPosition, 0.6f, 0.7f);
		ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.CRUCIBLE_PRESS, result, result.getCount());
		if (level instanceof ServerLevel server) {
			double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.35, z = worldPosition.getZ() + 0.5;
			server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, result), x, y, z, 12, 0.2, 0.05, 0.2, 0.08);
			server.sendParticles(recipe.heat() == HeatCondition.SUPERHEATED ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
				x, y, z, 6, 0.25, 0.05, 0.25, 0.01);
			server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, 3, 0.2, 0.05, 0.2, 0.01);
		}
	}

	public enum Status {
		EMPTY, NOT_ENOUGH, NO_ROTATION, TOO_COLD, OUTPUT_FULL, WORKING
	}

	public Status getStatus(@Nullable CrucibleRecipe recipe) {
		ItemStack input = inventory.getStackInSlot(INPUT);
		if (recipe == null || input.isEmpty())
			return Status.EMPTY;
		if (input.getCount() < recipe.count())
			return Status.NOT_ENOUGH;
		if (getSpeed() == 0 || isOverStressed())
			return Status.NO_ROTATION;
		if (!recipe.heat().testBlazeBurner(getHeatLevel()))
			return Status.TOO_COLD;
		if (!outputFits(recipe.result()))
			return Status.OUTPUT_FULL;
		return Status.WORKING;
	}

	private boolean outputFits(ItemStack result) {
		ItemStack output = inventory.getStackInSlot(OUTPUT);
		if (output.isEmpty())
			return true;
		return ItemStack.isSameItemSameComponents(output, result)
			&& output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), inventory.getSlotLimit(OUTPUT));
	}

	/** Heat from the block below, read the same way a Basin reads it. */
	public HeatLevel getHeatLevel() {
		return level == null ? HeatLevel.NONE : BasinBlockEntity.getHeatLevelOf(level.getBlockState(worldPosition.below()));
	}

	/** The recipe for whatever is in the input slot, ignoring how much of it there is. */
	@Nullable
	public CrucibleRecipe getRecipe() {
		ItemStack input = inventory.getStackInSlot(INPUT);
		if (input.isEmpty() || level == null)
			return null;
		if (cachedRecipe != null && cachedRecipe.value().ingredient().test(input))
			return cachedRecipe.value();
		cachedRecipe = findRecipe(input);
		return cachedRecipe == null ? null : cachedRecipe.value();
	}

	@Nullable
	private RecipeHolder<CrucibleRecipe> findRecipe(ItemStack stack) {
		if (level == null)
			return null;
		for (RecipeHolder<CrucibleRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUCIBLE.get()))
			if (holder.value().ingredient().test(stack))
				return holder;
		return null;
	}

	/** Only dust some recipe can press, and only one kind at a time. */
	public boolean isValidInput(ItemStack stack) {
		if (stack.isEmpty() || findRecipe(stack) == null)
			return false;
		ItemStack current = inventory.getStackInSlot(INPUT);
		return current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack);
	}

	// ---------------------------------------------------------------- inventory

	public ItemStackHandler getInventory() {
		return inventory;
	}

	/** What hoppers, funnels and pipes see: dust goes in, results come out. */
	public IItemHandler getAutomationHandler() {
		return automation;
	}

	/** Empty-hand right-click: results first, then the dust if nothing has been pressed yet. */
	boolean takeContents(Player player) {
		int slot = !inventory.getStackInSlot(OUTPUT).isEmpty() ? OUTPUT : INPUT;
		ItemStack stack = inventory.getStackInSlot(slot);
		if (stack.isEmpty())
			return false;
		inventory.setStackInSlot(slot, ItemStack.EMPTY);
		player.getInventory().placeItemBackInInventory(stack);
		return true;
	}

	void dropContents() {
		if (level == null || level.isClientSide)
			return;
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (!stack.isEmpty())
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
			inventory.setStackInSlot(slot, ItemStack.EMPTY);
		}
	}

	int getComparatorOutput() {
		ItemStack input = inventory.getStackInSlot(INPUT);
		return input.isEmpty() ? 0 : 1 + (int) (14f * input.getCount() / input.getMaxStackSize());
	}

	private void onInventoryChanged() {
		setChanged();
		syncQueued = true;
	}

	/**
	 * Called by block movers once they have copied this block entity's data elsewhere (Aeronautics physics objects
	 * assembling or disassembling, structure tools). Empties the original so nothing is dropped twice.
	 */
	@Override
	public void clearContent() {
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			inventory.setStackInSlot(slot, ItemStack.EMPTY);
		processing = false;
		timer = 0;
	}

	// ---------------------------------------------------------------- rendering & Ponder

	public boolean isProcessing() {
		return processing;
	}

	/** Progress of the current press, 0 to 1. */
	public float getProgress() {
		return getProgress(0);
	}

	/** Progress including the part of the current tick already rendered, for a smooth ram. */
	public float getProgress(float partialTicks) {
		if (!processing || maxTime <= 0)
			return 0;
		float left = getSpeed() != 0 ? timer - partialTicks * processingSpeed() : timer;
		return Mth.clamp(1 - left / maxTime, 0, 1);
	}

	/**
	 * The plate follows the press down exactly, then eases back up after each item (quick at first, slowing as it
	 * nears the top). With items pressed back to back it meets the next press partway up and follows it down again.
	 */
	private void animatePlate() {
		prevPlate = plate;
		float target = getProgress();
		if (target >= plate)
			plate = target;
		else
			plate = Math.max(target, plate - Math.max(0.03f, plate * 0.3f));
	}

	/** Plate travel for rendering: 0 at the top, 1 resting on the contents. */
	public float getPlate(float partialTicks) {
		return Mth.lerp(partialTicks, prevPlate, plate);
	}

	/** Ponder only: show the given contents, and press them over {@code time} (in 16 RPM ticks) if working. */
	public void setPonderState(ItemStack input, ItemStack output, boolean working, int time) {
		inventory.setStackInSlot(INPUT, input);
		inventory.setStackInSlot(OUTPUT, output);
		processing = working;
		maxTime = time;
		timer = working ? maxTime : 0;
	}

	// ---------------------------------------------------------------- goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.crucible").forGoggles(tooltip);

		ItemStack input = inventory.getStackInSlot(INPUT);
		ItemStack output = inventory.getStackInSlot(OUTPUT);
		CrucibleRecipe recipe = getRecipe();

		if (input.isEmpty()) {
			lang().translate("gui.goggles.crucible.empty").style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);
		} else {
			lang().text(input.getCount() + " x ").style(ChatFormatting.GOLD)
				.add(lang().add(input.getHoverName().copy()).style(ChatFormatting.GRAY))
				.forGoggles(tooltip, 1);
		}

		if (recipe != null) {
			lang().translate("gui.goggles.crucible.needs").style(ChatFormatting.GRAY)
				.add(lang().text(" " + recipe.count() + " x ").style(ChatFormatting.GOLD))
				.add(lang().add(recipe.ingredient().getItems().length > 0
					? recipe.ingredient().getItems()[0].getHoverName().copy() : Component.empty()).style(ChatFormatting.GRAY))
				.forGoggles(tooltip, 1);
			if (recipe.heat() != HeatCondition.NONE)
				lang().translate("gui.goggles.crucible.heat").style(ChatFormatting.GRAY)
					.add(lang().text(" ").add(lang().translate("gui.heat." + recipe.heat().getSerializedName()))
						.color(recipe.heat().getColor()))
					.forGoggles(tooltip, 1);

			Status status = getStatus(recipe);
			if (status == Status.WORKING || processing) {
				lang().translate("gui.goggles.crucible.pressing").style(ChatFormatting.GRAY)
					.add(lang().text(" " + (int) (getProgress() * 100) + "%").style(ChatFormatting.AQUA))
					.forGoggles(tooltip, 1);
			} else if (status != Status.EMPTY) {
				lang().translate("gui.goggles.crucible.status." + status.name().toLowerCase(java.util.Locale.ROOT),
						recipe.count() - input.getCount())
					.style(ChatFormatting.RED)
					.forGoggles(tooltip, 1);
			}
		}

		if (!output.isEmpty()) {
			lang().translate("gui.goggles.crucible.output").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			lang().text(output.getCount() + " x ").style(ChatFormatting.GOLD)
				.add(lang().add(output.getHoverName().copy()).style(ChatFormatting.GRAY))
				.forGoggles(tooltip, 2);
		}

		// Create's stress impact section
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	// ---------------------------------------------------------------- persistence & sync

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("Inventory", inventory.serializeNBT(registries));
		tag.putFloat("Timer", timer);
		tag.putInt("MaxTime", maxTime);
		tag.putBoolean("Processing", processing);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
		timer = tag.getFloat("Timer");
		maxTime = tag.getInt("MaxTime");
		processing = tag.getBoolean("Processing");
	}

	private class AutomationHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return 2;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return inventory.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (slot != INPUT || !isValidInput(stack))
				return stack;
			return inventory.insertItem(INPUT, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (slot != OUTPUT)
				return ItemStack.EMPTY;
			return inventory.extractItem(OUTPUT, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return inventory.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot == INPUT && isValidInput(stack);
		}
	}
}
