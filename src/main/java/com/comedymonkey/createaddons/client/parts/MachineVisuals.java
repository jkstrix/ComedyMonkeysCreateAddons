package com.comedymonkey.createaddons.client.parts;

import java.util.function.Supplier;

import com.comedymonkey.createaddons.client.AnimalPenRenderer;
import com.comedymonkey.createaddons.client.AutoHarvesterRenderer;
import com.comedymonkey.createaddons.client.CobbleGeneratorRenderer;
import com.comedymonkey.createaddons.client.CuckooClockRenderer;
import com.comedymonkey.createaddons.client.ElectricRenderers;
import com.comedymonkey.createaddons.client.MachineRenderers;
import com.comedymonkey.createaddons.client.MagmaEngineRenderer;
import com.comedymonkey.createaddons.client.PressureCrucibleRenderer;
import com.comedymonkey.createaddons.client.WellspringRenderer;
import com.comedymonkey.createaddons.compat.sable.SableCompat;
import com.comedymonkey.createaddons.compat.sable.client.AeronauticsRenderers;
import com.comedymonkey.createaddons.registry.ModBlockEntities;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The machines whose model parts Flywheel draws (see {@link PartsVisual}). A renderer that draws nothing but part
 * models is skipped altogether while its visual draws; the others keep drawing what isn't a part model (fluids, items,
 * plants), with their parts left to the visual.
 */
public final class MachineVisuals {
	private MachineVisuals() {}

	/** At client setup, once the block entity types exist. */
	public static void register() {
		visual(ModBlockEntities.COBBLE_GENERATOR, CobbleGeneratorRenderer::parts, false);
		visual(ModBlockEntities.MAGMA_ENGINE, MagmaEngineRenderer::parts, true);
		visual(ModBlockEntities.ELECTRIC_MOTOR, ElectricRenderers.Motor::parts, true);
		visual(ModBlockEntities.LAVA_GENERATOR, ElectricRenderers.Generator::parts, true);
		visual(ModBlockEntities.GEOTHERMAL_CONTROLLER, ElectricRenderers.Turbine::parts, true);
		visual(ModBlockEntities.PRESSURE_CRUCIBLE, PressureCrucibleRenderer::parts, false);
		visual(ModBlockEntities.STRING_TANGLER, MachineRenderers.Tangler::parts, false);
		visual(ModBlockEntities.STONE_INFUSER, MachineRenderers.Infuser::parts, false);
		visual(ModBlockEntities.AUTO_HARVESTER, AutoHarvesterRenderer::parts, false);
		visual(ModBlockEntities.WELLSPRING, WellspringRenderer::parts, false);
		visual(ModBlockEntities.ANIMAL_PEN, AnimalPenRenderer::parts, false);
		visual(ModBlockEntities.CUCKOO_CLOCK, CuckooClockRenderer::parts, false);
		// the Compact Gyroscope and Torque Rotor only exist with Sable
		if (SableCompat.LOADED)
			AeronauticsRenderers.registerVisuals();
	}

	/** {@code partsOnly}: the renderer draws nothing else, so it needn't run while the visual draws. */
	public static <T extends BlockEntity> void visual(Supplier<BlockEntityType<T>> type, PartsVisual.Parts<T> parts,
													   boolean partsOnly) {
		SimpleBlockEntityVisualizer.builder(type.get())
			.factory((context, be, partialTick) -> new PartsVisual<>(context, be, partialTick, parts))
			.skipVanillaRender(be -> partsOnly)
			.apply();
	}
}
