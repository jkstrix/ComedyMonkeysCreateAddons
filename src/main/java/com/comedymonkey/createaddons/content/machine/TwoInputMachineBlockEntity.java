package com.comedymonkey.createaddons.content.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Two ingredients in, one result out, turning as fast as its shaft (Create Millstone speeds: 16 RPM is 1x, 256 RPM
 * is 16x). The main ingredient goes in slot 0, the extra one in slot 1; which slot an item takes is decided by the
 * machine's recipes, so packs can add their own.
 */
public class TwoInputMachineBlockEntity extends KineticBlockEntity implements Clearable {
	public static final int MAIN = 0;
	public static final int EXTRA = 1;
	public static final int OUTPUT = 2;
	private static final int SYNC_INTERVAL = 4;

	private final MachineKind kind;
	private final ItemStackHandler inventory = new ItemStackHandler(3) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
			syncQueued = true;
		}
	};
	private final IItemHandler automation = new AutomationHandler();

	/** Work left on the current item, in "16 RPM ticks". */
	private float timer;
	private int maxTime;
	private boolean processing;
	private boolean syncQueued;
	/** What's being made, for the renderer and goggles. */
	private ItemStack making = ItemStack.EMPTY;
	@Nullable
	private RecipeHolder<TwoInputRecipe> cached;

	public TwoInputMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineKind kind) {
		super(type, pos, state);
		this.kind = kind;
	}

	public MachineKind getKind() {
		return kind;
	}

	// ---------------------------------------------------------------- processing

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			if (processing && getSpeed() != 0 && timer > 0)
				timer = Math.max(0, timer - processingSpeed());
			if (processing && kind == MachineKind.INFUSING)
				sprinkleDust();
			return;
		}

		TwoInputRecipe recipe = getRecipe();
		if (recipe == null || !canWork(recipe)) {
			if (processing) {
				processing = false;
				timer = 0;
				making = ItemStack.EMPTY;
				syncQueued = true;
			}
		} else {
			if (!processing) {
				processing = true;
				maxTime = recipe.processingTime();
				timer = maxTime;
				making = recipe.result().copy();
				syncQueued = true;
			}
			timer -= processingSpeed();
			if (timer <= 0)
				finish(recipe);
		}

		if (syncQueued && level.getGameTime() % SYNC_INTERVAL == 0) {
			syncQueued = false;
			sendData();
		}
	}

	/** The infuser's feeder spout, in model pixels (window north): the dust falls from here onto the stone. */
	private static final double SPOUT_X = 6, SPOUT_Y = 9.9, SPOUT_Z = 7.6;

	/**
	 * Client only: while the Stone Infuser works, a fine dust of the metal falls from the feeder's spout onto the
	 * turning stone, in the colour of the stone it's making.
	 */
	private void sprinkleDust() {
		if (level == null || getSpeed() == 0 || level.random.nextInt(3) != 0)
			return;
		Direction front = getBlockState().getValue(TwoInputMachineBlock.FACING);
		Vec3 local = new Vec3(SPOUT_X / 16 + (level.random.nextDouble() - 0.5) * 0.04, SPOUT_Y / 16,
			SPOUT_Z / 16 + (level.random.nextDouble() - 0.5) * 0.04);
		Vec3 at = VecHelper.rotateCentered(local, -(front.toYRot() + 180), Direction.Axis.Y);
		level.addParticle(new DustParticleOptions(dustColour(), 0.35f), worldPosition.getX() + at.x,
			worldPosition.getY() + at.y, worldPosition.getZ() + at.z, 0, -0.02, 0);
	}

	/** Asurine blue, crimsite red, ochrum gold, veridium green (muted), from the stone being made; grey otherwise. */
	private Vector3f dustColour() {
		String id = BuiltInRegistries.ITEM.getKey(making.getItem()).getPath();
		int rgb = id.contains("asurine") ? 0x4F7C96 : id.contains("crimsite") ? 0x8A3A30 : id.contains("ochrum") ? 0xA3873F
			: id.contains("veridium") ? 0x4E7A5A : 0x8A8C86;
		return new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
	}

	public float processingSpeed() {
		return Mth.clamp(Math.abs(getSpeed()) / 16f, 1, 512);
	}

	private boolean canWork(TwoInputRecipe recipe) {
		return getSpeed() != 0 && !isOverStressed()
			&& inventory.getStackInSlot(MAIN).getCount() >= recipe.count()
			&& inventory.getStackInSlot(EXTRA).getCount() >= recipe.extraCount()
			&& outputFits(recipe.result());
	}

	private void finish(TwoInputRecipe recipe) {
		inventory.extractItem(MAIN, recipe.count(), false);
		inventory.extractItem(EXTRA, recipe.extraCount(), false);
		ItemStack result = recipe.result().copy();
		ItemStack output = inventory.getStackInSlot(OUTPUT);
		if (output.isEmpty())
			inventory.setStackInSlot(OUTPUT, result);
		else
			output.grow(result.getCount());
		processing = false;
		timer = 0;
		making = ItemStack.EMPTY;
		syncQueued = true;

		if (kind == MachineKind.TANGLING)
			level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.WOOL_PLACE,
				net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 0.8f);
		else
			AllSoundEvents.MECHANICAL_PRESS_ACTIVATION.playOnServer(level, worldPosition, 0.4f, 1.2f);
		if (level instanceof ServerLevel server)
			server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, result), worldPosition.getX() + 0.5,
				worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.04);
	}

	private boolean outputFits(ItemStack result) {
		ItemStack output = inventory.getStackInSlot(OUTPUT);
		if (output.isEmpty())
			return true;
		return ItemStack.isSameItemSameComponents(output, result)
			&& output.getCount() + result.getCount() <= output.getMaxStackSize();
	}

	private RecipeInput input() {
		ItemStack main = inventory.getStackInSlot(MAIN);
		ItemStack extra = inventory.getStackInSlot(EXTRA);
		return new RecipeInput() {
			@Override
			public ItemStack getItem(int index) {
				return index == 0 ? main : index == 1 ? extra : ItemStack.EMPTY;
			}

			@Override
			public int size() {
				return 2;
			}
		};
	}

	/** The recipe for the loaded ingredients, ignoring how much of each there is. */
	@Nullable
	public TwoInputRecipe getRecipe() {
		ItemStack main = inventory.getStackInSlot(MAIN);
		ItemStack extra = inventory.getStackInSlot(EXTRA);
		if (main.isEmpty() || extra.isEmpty() || level == null)
			return null;
		// The cached recipe still counts if it matches and is still the loaded one (a /reload replaces them all)
		if (cached != null && cached.value().ingredient().test(main) && cached.value().extra().test(extra)
			&& level.getRecipeManager().byKey(cached.id()).orElse(null) == cached)
			return cached.value();
		cached = null;
		for (RecipeHolder<TwoInputRecipe> holder : recipes())
			if (holder.value().ingredient().test(main) && holder.value().extra().test(extra)) {
				cached = holder;
				break;
			}
		return cached == null ? null : cached.value();
	}

	private List<RecipeHolder<TwoInputRecipe>> recipes() {
		return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(kind.recipeType());
	}

	/** Which input slot an item goes in: the main slot if it's some recipe's main ingredient, else the extra slot. */
	public int inputSlotFor(ItemStack stack) {
		for (RecipeHolder<TwoInputRecipe> holder : recipes())
			if (holder.value().ingredient().test(stack))
				return MAIN;
		return EXTRA;
	}

	/** Only ingredients some recipe uses, and only one kind in each slot at a time. */
	public boolean acceptsInput(ItemStack stack) {
		if (stack.isEmpty())
			return false;
		boolean main = false, extra = false;
		for (RecipeHolder<TwoInputRecipe> holder : recipes()) {
			main |= holder.value().ingredient().test(stack);
			extra |= holder.value().extra().test(stack);
		}
		if (!main && !extra)
			return false;
		ItemStack current = inventory.getStackInSlot(main ? MAIN : EXTRA);
		return current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack);
	}

	// ---------------------------------------------------------------- inventory

	public IItemHandler getAutomationHandler() {
		return automation;
	}

	public ItemStack getStack(int slot) {
		return inventory.getStackInSlot(slot);
	}

	/** Empty-hand right-click: results first, then the ingredients. */
	boolean takeContents(Player player) {
		for (int slot : new int[] {OUTPUT, EXTRA, MAIN}) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (!stack.isEmpty()) {
				inventory.setStackInSlot(slot, ItemStack.EMPTY);
				player.getInventory().placeItemBackInInventory(stack);
				return true;
			}
		}
		return false;
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
		ItemStack output = inventory.getStackInSlot(OUTPUT);
		return output.isEmpty() ? 0 : 1 + (int) (14f * output.getCount() / output.getMaxStackSize());
	}

	/** Called by block movers once they've copied this block entity elsewhere, so nothing is dropped twice. */
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

	/** What the current item will be, or empty. */
	public ItemStack getMaking() {
		return making;
	}

	/** Progress of the current item, 0 to 1, including the part of this tick already rendered. */
	public float getProgress(float partialTicks) {
		if (!processing || maxTime <= 0)
			return 0;
		float left = getSpeed() != 0 ? timer - partialTicks * processingSpeed() : timer;
		return Mth.clamp(1 - left / maxTime, 0, 1);
	}

	/** Ponder only: show the given contents, working on {@code making} over {@code time} 16 RPM ticks. */
	public void setPonderState(ItemStack main, ItemStack extra, ItemStack output, ItemStack making, int time) {
		inventory.setStackInSlot(MAIN, main);
		inventory.setStackInSlot(EXTRA, extra);
		inventory.setStackInSlot(OUTPUT, output);
		this.making = making;
		processing = !making.isEmpty();
		maxTime = time;
		timer = processing ? time : 0;
	}

	// ---------------------------------------------------------------- goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles." + kind.id()).forGoggles(tooltip);
		boolean empty = true;
		for (int slot : new int[] {MAIN, EXTRA}) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;
			empty = false;
			lang().text(stack.getCount() + " x ").style(ChatFormatting.GOLD)
				.add(lang().add(stack.getHoverName().copy()).style(ChatFormatting.GRAY)).forGoggles(tooltip, 1);
		}
		if (empty)
			lang().translate("gui.goggles.machine.empty").style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1);

		TwoInputRecipe recipe = getRecipe();
		if (processing && !making.isEmpty()) {
			lang().translate("gui.goggles.machine.making").style(ChatFormatting.GRAY)
				.add(lang().text(" ").add(lang().add(making.getHoverName().copy())).style(ChatFormatting.AQUA))
				.add(lang().text(" " + (int) (getProgress(0) * 100) + "%").style(ChatFormatting.AQUA))
				.forGoggles(tooltip, 1);
		} else if (recipe != null) {
			String status = getSpeed() == 0 || isOverStressed() ? "no_rotation"
				: !outputFits(recipe.result()) ? "output_full" : "needs_more";
			lang().translate("gui.goggles.machine." + status).style(ChatFormatting.RED).forGoggles(tooltip, 1);
		} else if (!inventory.getStackInSlot(MAIN).isEmpty() || !inventory.getStackInSlot(EXTRA).isEmpty()) {
			lang().translate("gui.goggles.machine.needs_other").style(ChatFormatting.RED).forGoggles(tooltip, 1);
		}

		ItemStack output = inventory.getStackInSlot(OUTPUT);
		if (!output.isEmpty()) {
			lang().translate("gui.goggles.machine.output").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			lang().text(output.getCount() + " x ").style(ChatFormatting.GOLD)
				.add(lang().add(output.getHoverName().copy()).style(ChatFormatting.GRAY)).forGoggles(tooltip, 2);
		}
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
		if (!making.isEmpty())
			tag.put("Making", making.save(registries));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
		timer = tag.getFloat("Timer");
		maxTime = tag.getInt("MaxTime");
		processing = tag.getBoolean("Processing");
		making = tag.contains("Making") ? ItemStack.parseOptional(registries, tag.getCompound("Making")) : ItemStack.EMPTY;
	}

	/** Both ingredients in from any side (sorted into their slots), the result out. */
	private class AutomationHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return 3;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return inventory.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (slot == OUTPUT || !acceptsInput(stack))
				return stack;
			return inventory.insertItem(inputSlotFor(stack), stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return slot == OUTPUT ? inventory.extractItem(OUTPUT, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot != OUTPUT && acceptsInput(stack) && inputSlotFor(stack) == slot;
		}
	}

}
