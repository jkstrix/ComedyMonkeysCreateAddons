package com.comedymonkey.createaddons.compat.pipez;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.slf4j.Logger;

import com.comedymonkey.createaddons.config.CMConfig;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Lets Pipez fluid pipes (and other fluid transport) fuel the Portable Engine from Simulated (bundled with Create
 * Aeronautics) with lava and other fuel fluids. The engine already takes coal and other burnable items through
 * its own item capability, so Pipez item pipes work without help; it has no fluid input of its own.
 * <p>
 * Only registered when Pipez and Simulated are both installed. Simulated is not on the compile classpath, so the
 * engine's burn-time accessors are reached through method handles.
 */
public final class PipezEngineCompat {
	public static final String SIMULATED_ID = "simulated";
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ResourceLocation ENGINE_ID = ResourceLocation.fromNamespaceAndPath(SIMULATED_ID, "portable_engine");
	private static final String ENGINE_CLASS = "dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity";

	static MethodHandle getBurnTime;
	static MethodHandle setBurnTime;
	static MethodHandle isSuperHeated;
	static MethodHandle setSuperHeated;
	static MethodHandle isFuelInfinite;

	private PipezEngineCompat() {}

	@SuppressWarnings("unchecked")
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		var type = (BlockEntityType<BlockEntity>) BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ENGINE_ID);
		if (type == null || !bindEngine())
			return;

		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (engine, side) ->
			CMConfig.SERVER.pipezFeedsEngines() ? new EngineFuelTank(engine) : null);
	}

	private static boolean bindEngine() {
		try {
			Class<?> engine = Class.forName(ENGINE_CLASS);
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			getBurnTime = lookup.findVirtual(engine, "getCurrentBurnTime", MethodType.methodType(int.class));
			setBurnTime = lookup.findVirtual(engine, "setCurrentBurnTime", MethodType.methodType(void.class, int.class));
			isSuperHeated = lookup.findVirtual(engine, "isSuperHeated", MethodType.methodType(boolean.class));
			setSuperHeated = lookup.findVirtual(engine, "setSuperHeated", MethodType.methodType(void.class, boolean.class));
			isFuelInfinite = lookup.findVirtual(engine, "isCurrentFuelInfinite", MethodType.methodType(boolean.class));
			return true;
		} catch (ReflectiveOperationException e) {
			// A Simulated update renamed something: leave the engine alone rather than crash
			LOGGER.warn("Could not hook Simulated's Portable Engine for fluid fuel; engines won't take piped fluids", e);
			return false;
		}
	}
}
