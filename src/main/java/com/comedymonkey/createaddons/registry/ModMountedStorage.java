package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.ContraptionUpgradeInteraction;
import com.comedymonkey.createaddons.content.chest.UpgradedChestMountedStorageType;
import com.comedymonkey.createaddons.content.wellspring.WellspringMountedStorageType;
import com.comedymonkey.createaddons.content.wellspring.WellspringMovementBehaviour;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Create contraption storage types (how blocks behave while mounted on a moving contraption). */
public final class ModMountedStorage {
	public static final DeferredRegister<MountedItemStorageType<?>> ITEM_STORAGE_TYPES =
		DeferredRegister.create(CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, ComedyMonkeysCreateAddons.MOD_ID);

	public static final DeferredHolder<MountedItemStorageType<?>, UpgradedChestMountedStorageType> UPGRADED_CHEST =
		ITEM_STORAGE_TYPES.register("upgraded_chest", UpgradedChestMountedStorageType::new);

	public static final DeferredRegister<MountedFluidStorageType<?>> FLUID_STORAGE_TYPES =
		DeferredRegister.create(CreateRegistries.MOUNTED_FLUID_STORAGE_TYPE, ComedyMonkeysCreateAddons.MOD_ID);

	public static final DeferredHolder<MountedFluidStorageType<?>, WellspringMountedStorageType> WELLSPRING =
		FLUID_STORAGE_TYPES.register("wellspring", WellspringMountedStorageType::new);

	private ModMountedStorage() {}

	/** Links blocks to their storage types. Call once registries are populated (common setup). */
	public static void bindBlocks() {
		MountedItemStorageType.REGISTRY.register(ModBlocks.UPGRADED_CHEST.get(), UPGRADED_CHEST.get());
		MountedItemStorageType.REGISTRY.register(ModBlocks.UPGRADED_BARREL.get(), UPGRADED_CHEST.get());
		// Chest upgrades can be used on chests and barrels riding contraptions (other clicks still open them)
		var upgradeOnContraption = new ContraptionUpgradeInteraction();
		MovingInteractionBehaviour.REGISTRY.register(Blocks.CHEST, upgradeOnContraption);
		MovingInteractionBehaviour.REGISTRY.register(Blocks.BARREL, upgradeOnContraption);
		MovingInteractionBehaviour.REGISTRY.register(ModBlocks.UPGRADED_CHEST.get(), upgradeOnContraption);
		MovingInteractionBehaviour.REGISTRY.register(ModBlocks.UPGRADED_BARREL.get(), upgradeOnContraption);
		// Wellsprings carry their tank on contraptions and keep generating while they move
		MountedFluidStorageType.REGISTRY.register(ModBlocks.WELLSPRING.get(), WELLSPRING.get());
		MovementBehaviour.REGISTRY.register(ModBlocks.WELLSPRING.get(), new WellspringMovementBehaviour());
	}
}
