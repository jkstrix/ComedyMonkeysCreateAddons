package com.comedymonkey.createaddons.content.chest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModMountedStorage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;

/**
 * An upgraded chest riding on a Create contraption. Keeps its multiplied stack limit for anything that deposits into
 * the contraption (drills, harvesters, portable storage interfaces...), saves oversized stacks safely, and opens the
 * upgraded chest screen when a player right-clicks it on the moving contraption.
 */
public class UpgradedChestMountedStorage extends MountedItemStorage {
	private record Entry(int slot, ItemStack item, int count) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("slot").forGetter(Entry::slot),
			ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(Entry::item),
			Codec.INT.fieldOf("count").forGetter(Entry::count)
		).apply(i, Entry::new));
	}

	public static final MapCodec<UpgradedChestMountedStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		ChestTier.CODEC.fieldOf("tier").forGetter(s -> s.tier),
		Entry.CODEC.listOf().fieldOf("items").forGetter(UpgradedChestMountedStorage::entries)
	).apply(i, UpgradedChestMountedStorage::fromEntries));

	private final ChestTier tier;
	private final NonNullList<ItemStack> items;

	UpgradedChestMountedStorage(ChestTier tier, NonNullList<ItemStack> items) {
		super(ModMountedStorage.UPGRADED_CHEST.get());
		this.tier = tier;
		this.items = items;
	}

	static UpgradedChestMountedStorage of(UpgradedStorage chest) {
		return new UpgradedChestMountedStorage(chest.getTier(), ChestStacks.copy(chest.getItems()));
	}

	private static UpgradedChestMountedStorage fromEntries(ChestTier tier, List<Entry> entries) {
		NonNullList<ItemStack> items = NonNullList.withSize(tier.slots(), ItemStack.EMPTY);
		for (Entry entry : entries)
			if (entry.slot() >= 0 && entry.slot() < items.size())
				items.set(entry.slot(), entry.item().copyWithCount(Math.max(1, entry.count())));
		return new UpgradedChestMountedStorage(tier, items);
	}

	private List<Entry> entries() {
		List<Entry> entries = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			if (!stack.isEmpty())
				entries.add(new Entry(i, stack.copyWithCount(1), stack.getCount()));
		}
		return entries;
	}

	private int multiplier() {
		return tier.multiplier();
	}

	/** Back on the ground: hand the contents to the re-placed chest. */
	@Override
	public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		if (be instanceof UpgradedStorage storage)
			storage.fill(ChestStacks.copy(items));
	}

	// ---------------------------------------------------------------- item handler

	@Override
	public int getSlots() {
		return items.size();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return items.get(slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		items.set(slot, stack);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		return ChestStacks.insert(items, slot, stack, simulate, multiplier());
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ChestStacks.extract(items, slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64 * multiplier();
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}

	// ---------------------------------------------------------------- opening on a moving contraption

	/**
	 * Same as Create's default, except it opens the upgraded chest screen (which needs the stack multiplier sent
	 * along) instead of Create's generic one, which would cap stacks at 64.
	 */
	@Override
	public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureBlockInfo info) {
		ServerLevel level = player.serverLevel();
		Vec3 localCenter = Vec3.atCenterOf(info.pos());
		Predicate<Player> stillValid = p -> contraption.entity.isAlive()
			&& p.distanceToSqr(contraption.entity.toGlobalVector(localCenter, 0)) < 8 * 8;
		BlockState state = info.state();
		boolean barrel = state.getBlock() instanceof UpgradedBarrelBlock;
		Container mine = new MountedView(stillValid, () -> playSound(level, contraption, localCenter, false, barrel));
		Container container = mine;
		Component name = barrel ? tier.barrelName() : tier.chestName();

		// Half of a double chest: open both halves together, like a placed double chest
		if (state.hasProperty(UpgradedChestBlock.TYPE) && state.getValue(UpgradedChestBlock.TYPE) != ChestType.SINGLE) {
			BlockPos otherPos = info.pos().relative(ChestBlock.getConnectedDirection(state));
			MountedItemStorage other = contraption.getStorage().getMountedItems().storages.get(otherPos);
			if (other instanceof UpgradedChestMountedStorage otherChest && otherChest.tier == tier) {
				Container theirs = otherChest.new MountedView(stillValid, () -> {});
				boolean mineFirst = state.getValue(UpgradedChestBlock.TYPE) == ChestType.RIGHT;
				container = new BigCompoundContainer(mineFirst ? mine : theirs, mineFirst ? theirs : mine, this::multiplier);
				name = Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest.large", tier.chestName());
			}
		}

		ChestUpgrades.openChestMenu(player, container, name, multiplier(), tier);
		playSound(level, contraption, localCenter, true, barrel);
		return true;
	}

	private static void playSound(ServerLevel level, Contraption contraption, Vec3 localCenter, boolean open,
								  boolean barrel) {
		Vec3 pos = contraption.entity.toGlobalVector(localCenter, 0);
		var sound = barrel ? (open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE)
			: (open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE);
		level.playSound(null, BlockPos.containing(pos), sound, SoundSource.BLOCKS, 0.75f, 1f);
	}

	/** Container view of this storage for the chest screen. */
	class MountedView implements Container {
		private final Predicate<Player> stillValid;
		private final Runnable onClose;

		MountedView(Predicate<Player> stillValid, Runnable onClose) {
			this.stillValid = stillValid;
			this.onClose = onClose;
		}

		/** The storage this view shows. */
		UpgradedChestMountedStorage storage() {
			return UpgradedChestMountedStorage.this;
		}

		@Override
		public int getContainerSize() {
			return items.size();
		}

		@Override
		public boolean isEmpty() {
			return items.stream().allMatch(ItemStack::isEmpty);
		}

		@Override
		public ItemStack getItem(int slot) {
			return items.get(slot);
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			return ContainerHelper.removeItem(items, slot, amount);
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return ContainerHelper.takeItem(items, slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			items.set(slot, stack);
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
		public void setChanged() {}

		@Override
		public boolean stillValid(Player player) {
			return stillValid.test(player);
		}

		@Override
		public void stopOpen(Player player) {
			onClose.run();
		}

		@Override
		public void clearContent() {
			items.clear();
		}
	}
}
