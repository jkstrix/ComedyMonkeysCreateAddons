package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.comedymonkey.createaddons.compat.pipez.cover.PipeCoverEvents;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCoverShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A covered Pipez pipe takes its cover's shape, so a full cover is a solid block you can stand on and click anywhere.
 * Does nothing (and isn't applied) without Pipez.
 */
@Pseudo
@Mixin(targets = "de.maxhenkel.pipez.blocks.PipeBlock")
public abstract class PipeBlockMixin {
	@Inject(method = "getShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
		at = @At("HEAD"), cancellable = true, require = 0)
	private void comedymonkeysaddons$coverShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context,
												 CallbackInfoReturnable<VoxelShape> cir) {
		VoxelShape shape = PipeCoverShapes.shapeOrNull(state, level, pos);
		if (shape != null)
			cir.setReturnValue(shape);
	}

	@Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true, require = 0)
	private void comedymonkeysaddons$coverCollision(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context,
													 CallbackInfoReturnable<VoxelShape> cir) {
		VoxelShape shape = PipeCoverShapes.shapeOrNull(state, level, pos);
		if (shape != null)
			cir.setReturnValue(shape);
	}

	@Inject(method = "getInteractionShape", at = @At("HEAD"), cancellable = true, require = 0)
	private void comedymonkeysaddons$coverInteraction(BlockState state, BlockGetter level, BlockPos pos,
													   CallbackInfoReturnable<VoxelShape> cir) {
		VoxelShape shape = PipeCoverShapes.shapeOrNull(state, level, pos);
		if (shape != null)
			cir.setReturnValue(shape);
	}

	@Inject(method = "getBlockSupportShape", at = @At("HEAD"), cancellable = true, require = 0)
	private void comedymonkeysaddons$coverSupport(BlockState state, BlockGetter level, BlockPos pos,
												   CallbackInfoReturnable<VoxelShape> cir) {
		VoxelShape shape = PipeCoverShapes.shapeOrNull(state, level, pos);
		if (shape != null)
			cir.setReturnValue(shape);
	}

	/** A side starting or stopping extracting changes the snug cover's shape; tell clients to redraw it. */
	@Inject(method = "setExtracting", at = @At("TAIL"), require = 0)
	private void comedymonkeysaddons$extractingChanged(Level level, BlockPos pos, Direction side, boolean extracting,
														CallbackInfo ci) {
		PipeCoverEvents.resync(level, pos);
	}
}
