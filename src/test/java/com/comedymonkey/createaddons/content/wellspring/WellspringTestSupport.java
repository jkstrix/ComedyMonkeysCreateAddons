package com.comedymonkey.createaddons.content.wellspring;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** A Fluid Wellspring block entity with no level, and shortcuts for filling it and installing upgrades. */
final class WellspringTestSupport {
	static final HolderLookup.Provider REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

	private WellspringTestSupport() {}

	static BlockState state() {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("comedymonkeysaddons", "wellspring"))
			.defaultBlockState();
	}

	static WellspringBlockEntity create() {
		return new WellspringBlockEntity(BlockPos.ZERO, state());
	}

	/** Puts the upgrade straight into the slot, through the inventory. */
	static void install(WellspringBlockEntity be, int slot, WellspringUpgrade upgrade) {
		be.getUpgrades().setStackInSlot(slot, new ItemStack(upgrade.item()));
	}

	static int fill(WellspringBlockEntity be, FluidStack stack) {
		return be.getTank().fill(stack, IFluidHandler.FluidAction.EXECUTE);
	}
}
