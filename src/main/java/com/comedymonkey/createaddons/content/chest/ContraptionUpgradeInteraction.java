package com.comedymonkey.createaddons.content.chest;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.network.ContraptionBlockSyncPayload;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Right-clicking a chest or barrel on a moving Create contraption with a chest upgrade: upgrades it right there,
 * with the same rules as on the ground (one upgrade per double chest, only to a higher tier, the old upgrade handed
 * back). Any other right-click is passed on to Create, which opens the storage as usual.
 * <p>
 * Create has no API for swapping a mounted block's storage, so the contraption's storage is saved to NBT, the
 * entry for this block replaced, and read back (which rebuilds it just as loading a world would).
 */
public class ContraptionUpgradeInteraction extends MovingInteractionBehaviour {
	@Override
	public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
										   AbstractContraptionEntity entity) {
		ItemStack held = player.getItemInHand(hand);
		Contraption contraption = entity.getContraption();
		if (!(held.getItem() instanceof ChestUpgradeItem upgradeItem))
			return contraption.getStorage().handlePlayerStorageInteraction(contraption, player, localPos);
		if (player.level().isClientSide)
			return true;

		StructureBlockInfo info = contraption.getBlocks().get(localPos);
		if (info == null)
			return false;
		BlockState state = info.state();
		ChestTier tier = upgradeItem.getTier();

		boolean chest = state.is(Blocks.CHEST) || state.is(ModBlocks.UPGRADED_CHEST.get());
		ChestTier current = state.getBlock() instanceof TieredStorageBlock tiered ? tiered.tierOf(state) : null;
		if (current != null && !tier.isAbove(current)) {
			player.displayClientMessage(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest.already_tier",
				chest ? current.chestName() : current.barrelName()).withStyle(ChatFormatting.GOLD), true);
			return true;
		}

		// Both halves of a double chest, the clicked one first (it keeps the one upgrade)
		List<BlockPos> parts = new ArrayList<>();
		parts.add(localPos);
		if (chest && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			BlockPos other = localPos.relative(ChestBlock.getConnectedDirection(state));
			StructureBlockInfo otherInfo = contraption.getBlocks().get(other);
			if (otherInfo != null && otherInfo.state().getBlock() == state.getBlock())
				parts.add(other);
		}

		CompoundTag storageTag = new CompoundTag();
		contraption.getStorage().write(storageTag, player.registryAccess(), false);
		ListTag storages = storageTag.getList("items", Tag.TAG_COMPOUND);
		var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);

		for (BlockPos part : parts) {
			StructureBlockInfo partInfo = contraption.getBlocks().get(part);
			BlockState partState = partInfo.state();
			MountedItemStorage old = contraption.getStorage().getAllItemStorages().get(part);
			NonNullList<ItemStack> items = NonNullList.withSize(tier.slots(), ItemStack.EMPTY);
			if (old != null)
				for (int slot = 0; slot < Math.min(old.getSlots(), items.size()); slot++)
					items.set(slot, old.getStackInSlot(slot).copy());

			// New block, and block entity data matching what an upgraded block saves
			BlockState upgraded;
			BlockEntityType<?> type;
			CompoundTag nbt = new CompoundTag();
			if (chest) {
				upgraded = ModBlocks.UPGRADED_CHEST.get().defaultBlockState()
					.setValue(UpgradedChestBlock.FACING, partState.getValue(ChestBlock.FACING))
					.setValue(UpgradedChestBlock.TYPE, partState.getValue(ChestBlock.TYPE))
					.setValue(UpgradedChestBlock.TIER, tier);
				type = ModBlockEntities.UPGRADED_CHEST.get();
				nbt.putBoolean("HoldsUpgrade", current != null ? holdsUpgrade(partInfo) : part.equals(localPos));
			} else {
				upgraded = ModBlocks.UPGRADED_BARREL.get().defaultBlockState()
					.setValue(UpgradedBarrelBlock.FACING, partState.getValue(BarrelBlock.FACING))
					.setValue(UpgradedBarrelBlock.TIER, tier);
				type = ModBlockEntities.UPGRADED_BARREL.get();
			}
			nbt.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).toString());
			nbt.put("Items", new ListTag());
			StructureBlockInfo newInfo = new StructureBlockInfo(part, upgraded, nbt);
			setContraptionBlockData(entity, part, newInfo);
			// Create only sends the new block state; send the block entity data too so clients rebuild it correctly
			PacketDistributor.sendToPlayersTrackingEntity(entity, new ContraptionBlockSyncPayload(entity.getId(), part, upgraded, nbt));

			// Swap this block's entry in the saved storage
			Tag encoded = MountedItemStorage.CODEC.encodeStart(ops, new UpgradedChestMountedStorage(tier, items))
				.resultOrPartial(LogUtils.getLogger()::error).orElse(null);
			if (encoded == null)
				continue;
			boolean replaced = false;
			for (int i = 0; i < storages.size(); i++) {
				CompoundTag entry = storages.getCompound(i);
				if (NbtUtils.readBlockPos(entry, "pos").map(part::equals).orElse(false)) {
					entry.put("storage", encoded);
					replaced = true;
				}
			}
			if (!replaced) {
				CompoundTag entry = new CompoundTag();
				entry.put("pos", NbtUtils.writeBlockPos(part));
				entry.put("storage", encoded);
				storages.add(entry);
			}
		}
		storageTag.put("items", storages);
		contraption.getStorage().read(storageTag, player.registryAccess(), false, contraption);

		// Pay for it, exactly as on the ground
		held.consume(1, player);
		if (current != null)
			player.getInventory().placeItemBackInInventory(new ItemStack(current.upgradeItem()));
		player.level().playSound(null, entity.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
		player.displayClientMessage(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest.upgraded",
			chest ? tier.chestName() : tier.barrelName(), tier.multiplier()).withStyle(ChatFormatting.GOLD), true);
		if (player instanceof ServerPlayer)
			ModAdvancements.award(player, chest ? ModAdvancements.CHEST_UPGRADE : ModAdvancements.BARREL_UPGRADE,
				new ItemStack(tier.upgradeItem()), tier.ordinal());
		return true;
	}

	private static boolean holdsUpgrade(StructureBlockInfo info) {
		CompoundTag nbt = info.nbt();
		return nbt == null || !nbt.contains("HoldsUpgrade") || nbt.getBoolean("HoldsUpgrade");
	}
}
