package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * A City Vault's contents and lock. It reads its district from the {@link CityRecord}: once the record says it opens
 * ({@link CityRecord#vaultOpen}), it unlocks for good and its iris opens (animated, if it happened while loaded). Its
 * loot is rolled the first time anyone opens it, so it's shared, first come first served (P23).
 */
public class CityVaultBlockEntity extends CityBlockEntity {
	public static final ResourceKey<LootTable> DISTRICT_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
		ComedyMonkeysCreateAddons.asResource("chests/clockwork_city_vault"));
	public static final ResourceKey<LootTable> HEART_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
		ComedyMonkeysCreateAddons.asResource("chests/clockwork_city_heart_vault"));
	public static final int SLOTS = 27;

	private final SimpleContainer container = new SimpleContainer(SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			CityVaultBlockEntity.this.setChanged();
		}

		@Override
		public boolean stillValid(Player player) {
			return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64;
		}
	};
	private final IItemHandler items = new InvWrapper(container);
	private boolean lootRolled;
	/** Game time its iris began to open while loaded (the renderer's animation); far in the past otherwise. */
	private long openedAt = SubstationBlockEntity.NEVER;

	public CityVaultBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.CITY_VAULT_BE.get(), pos, state);
	}

	public SimpleContainer container() {
		return container;
	}

	/** Its items, for funnels and arms: only once it's open. */
	@Nullable
	public IItemHandler items() {
		return isLocked() ? null : items;
	}

	public boolean isLocked() {
		return getBlockState().getValue(CityVaultBlock.LOCKED);
	}

	public boolean isHeart() {
		return getBlockState().getValue(CityVaultBlock.HEART);
	}

	public long openedAt() {
		return openedAt;
	}

	public boolean lootRolled() {
		return lootRolled;
	}

	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		CityRecord record = record();
		if (record == null || !isLocked() || !link.bound() || !(level instanceof ServerLevel server))
			return;
		if (record.vaultOpen(server, link.origin(), isHeart() ? District.HEART : link.district()))
			unlock(live);
	}

	/** Unlocks for good: its iris opens (animated when {@code live}). */
	public void unlock(boolean live) {
		if (level == null || !isLocked())
			return;
		openedAt = live ? level.getGameTime() : SubstationBlockEntity.NEVER;
		level.setBlock(worldPosition, getBlockState().setValue(CityVaultBlock.LOCKED, false), Block.UPDATE_ALL);
		invalidateCapabilities();
		sync();
	}

	/** A player opens it: its loot is rolled the first time. */
	void open(Player player) {
		if (!(player instanceof ServerPlayer sp) || isLocked())
			return;
		rollLoot(player);
		Component name = Component.translatable(getBlockState().getBlock().getDescriptionId());
		// its own screen: the GUI kit with the void-alloy band (D46, D59)
		sp.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CityVaultMenu(id, inventory, container), name), buf -> buf.writeVarInt(SLOTS / 9));
	}

	/** Fills it from its loot table, once (a district's, or the heart's with the Elytra). */
	public void rollLoot(@Nullable Player player) {
		if (lootRolled || !(level instanceof ServerLevel server))
			return;
		lootRolled = true;
		LootTable table = server.getServer().reloadableRegistries().getLootTable(isHeart() ? HEART_LOOT : DISTRICT_LOOT);
		LootParams.Builder params = new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition));
		if (player != null)
			params.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
		table.fill(container, params.create(LootContextParamSets.CHEST), server.getRandom().nextLong());
		setChanged();
	}

	// ---------- saving ----------

	@Override
	protected void writeState(CompoundTag tag, HolderLookup.Provider registries) {
		NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		for (int i = 0; i < SLOTS; i++)
			list.set(i, container.getItem(i));
		ContainerHelper.saveAllItems(tag, list, registries);
		tag.putBoolean("LootRolled", lootRolled);
	}

	@Override
	protected void readState(CompoundTag tag, HolderLookup.Provider registries) {
		NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, list, registries);
		for (int i = 0; i < SLOTS; i++)
			container.setItem(i, list.get(i));
		lootRolled = tag.getBoolean("LootRolled");
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		tag.putLong("OpenedAt", openedAt);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		openedAt = tag.contains("OpenedAt") ? tag.getLong("OpenedAt") : SubstationBlockEntity.NEVER;
	}
}
