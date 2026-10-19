package com.comedymonkey.createaddons.content.wellspring;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

/** Keeps a Fluid Wellspring generating while it rides on a Create contraption (see {@link WellspringMountedStorage}). */
public class WellspringMovementBehaviour implements MovementBehaviour {
	@Override
	public void tick(MovementContext context) {
		if (!context.world.isClientSide && context.getFluidStorage() instanceof WellspringMountedStorage storage)
			storage.generate();
	}

	/** Generation isn't something the contraption's controls switch off. */
	@Override
	public boolean mustTickWhileDisabled() {
		return true;
	}
}
