package com.comedymonkey.createaddons.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.comedymonkey.createaddons.compat.pipez.cover.PipeCoverGoggles;
import com.comedymonkey.createaddons.registry.ModAttachments;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Two things for Pipez Covers on Pipez's pipe block entity:
 * <ul>
 * <li>Pipez drops a pipe's block entity once it has nothing to keep (no extracting sides, upgrades or settings). A
 * cover lives on that block entity, so a covered pipe always has a reason to keep it.</li>
 * <li>Under a cover you can't see what kind of pipe it is, so goggles say (only on covered pipes).</li>
 * </ul>
 */
@Pseudo
@Mixin(targets = "de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity")
public abstract class PipeTileEntityMixin implements IHaveGoggleInformation {
	@Inject(method = "hasReasonToStay", at = @At("HEAD"), cancellable = true, require = 0)
	private void comedymonkeysaddons$keepForCover(CallbackInfoReturnable<Boolean> cir) {
		if (((BlockEntity) (Object) this).hasData(ModAttachments.PIPE_COVER))
			cir.setReturnValue(true);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return PipeCoverGoggles.addToTooltip((BlockEntity) (Object) this, tooltip);
	}
}
