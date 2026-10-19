package com.comedymonkey.createaddons.compat.pipez;

import com.comedymonkey.createaddons.config.CMConfig;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Lets Pipez item pipes feed fuel into Create's Blaze Burners, which normally only take fuel by hand, deployer
 * or mechanical arm. Only registered when Pipez is installed, and needs no Pipez classes to compile.
 * <p>
 * Only an <b>item</b> capability is added. Create: Liquid Fuel adds a <b>fluid</b> capability to the same block
 * entity, and fuel inserted here still goes through Create's own {@code tryInsert}/{@code tryUpdateFuel}, which
 * Liquid Fuel hooks, so the two mods do not step on each other.
 */
public final class PipezBlazeBurnerCompat {
	public static final String PIPEZ_ID = "pipez";
	private static final String PIPEZ_PACKAGE = "de.maxhenkel.pipez.";
	private static final ResourceLocation BLAZE_BURNER_ID = ResourceLocation.fromNamespaceAndPath("create", "blaze_heater");
	/** Pipez looks capabilities up a handful of frames above us; this is a generous bound. */
	private static final int CALLER_SEARCH_DEPTH = 48;
	private static final StackWalker WALKER = StackWalker.getInstance();

	private PipezBlazeBurnerCompat() {}

	@SuppressWarnings("unchecked")
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		// Looked up by id rather than AllBlockEntityTypes.HEATER, to avoid a compile dependency on Registrate
		var type = (BlockEntityType<BlazeBurnerBlockEntity>) BuiltInRegistries.BLOCK_ENTITY_TYPE.get(BLAZE_BURNER_ID);
		if (type == null)
			return;

		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (burner, side) -> {
			if (!CMConfig.SERVER.pipezFeedsBlazeBurners())
				return null;
			if (CMConfig.SERVER.blazeBurnerPipezOnly() && !calledFromPipez())
				return null;
			return new BlazeBurnerFuelHandler(burner);
		});
	}

	/**
	 * Capability lookups do not say who is asking, so check whether a Pipez pipe is on the call stack. This keeps
	 * hoppers, funnels and other mods' pipes from feeding burners unless the config allows it.
	 */
	private static boolean calledFromPipez() {
		return WALKER.walk(frames -> frames.limit(CALLER_SEARCH_DEPTH)
			.anyMatch(frame -> frame.getClassName().startsWith(PIPEZ_PACKAGE)));
	}
}
