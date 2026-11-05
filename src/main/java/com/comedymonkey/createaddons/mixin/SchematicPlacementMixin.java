package com.comedymonkey.createaddons.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.comedymonkey.createaddons.content.line.PrintedRelinks;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Schematicannon sets its blocks down without telling their neighbours, so a Power Line printed before the machine,
 * light or wall it meets never linked to it (a lamp left dark, a generator not feeding its wire). Each block it prints
 * has the wires round it work their links out again, as a player's block would (at the end of the tick: PrintedRelinks).
 */
@Mixin(value = BlockHelper.class, remap = false)
public abstract class SchematicPlacementMixin {
	@Inject(method = "placeSchematicBlock", at = @At("RETURN"))
	private static void comedymonkeysaddons$relinkWires(Level level, BlockState state, BlockPos target, ItemStack stack,
														@Nullable CompoundTag data, CallbackInfo ci) {
		PrintedRelinks.printed(level, target);
	}
}
