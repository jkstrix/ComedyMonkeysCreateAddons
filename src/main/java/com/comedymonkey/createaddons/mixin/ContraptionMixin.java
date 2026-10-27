package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.comedymonkey.createaddons.content.helperbot.HelperBotBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * A contraption that comes apart where one of its blocks can't go (bedrock, a barrier) drops that block without its
 * block entity, so a dyed Helper Bot came back plain. He's dropped from his saved data instead.
 */
@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionMixin {
	@WrapOperation(method = "addBlocksToWorld", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
	private void comedymonkeysaddons$dropHelperBotWithData(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity none,
														   Operation<Void> original, @Local StructureBlockInfo block, @Local(argsOnly = true) Level world) {
		BlockEntity saved = none;
		if (none == null && state.getBlock() instanceof HelperBotBlock bot && block.nbt() != null) {
			saved = bot.newBlockEntity(pos, state);
			if (saved != null)
				saved.loadWithComponents(block.nbt(), world.registryAccess());
		}
		original.call(state, level, pos, saved);
	}
}
