package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.comedymonkey.createaddons.content.VillagerSeats;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

/**
 * Bends a seated villager's legs forward with the angles a player's riding pose uses (see {@link VillagerSeats}).
 * One model draws every villager, and vanilla never sets the legs' roll, so it is put back for the rest.
 */
@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin {
	@Shadow
	@Final
	private ModelPart rightLeg;
	@Shadow
	@Final
	private ModelPart leftLeg;

	@Inject(method = "setupAnim", at = @At("TAIL"), require = 1)
	private void comedymonkeysaddons$sitInSeat(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
											   float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!VillagerSeats.sitting(entity)) {
			rightLeg.zRot = 0;
			leftLeg.zRot = 0;
			return;
		}
		// HumanoidModel's riding pose
		rightLeg.xRot = -1.4137167F;
		rightLeg.yRot = (float) (Math.PI / 10);
		rightLeg.zRot = 0.07853982F;
		leftLeg.xRot = -1.4137167F;
		leftLeg.yRot = (float) (-Math.PI / 10);
		leftLeg.zRot = -0.07853982F;
	}
}
