package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.engine.MagmaEngineDisplaySources;
import com.comedymonkey.createaddons.content.wellspring.WellspringDisplaySources;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Create Display Link sources: what our blocks can show on Display Boards, Nixie Tubes and signs. */
public final class ModDisplaySources {
	public static final DeferredRegister<DisplaySource> SOURCES =
		DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, ComedyMonkeysCreateAddons.MOD_ID);

	public static final DeferredHolder<DisplaySource, MagmaEngineDisplaySources.Lava> MAGMA_ENGINE_LAVA =
		SOURCES.register("magma_engine_lava", MagmaEngineDisplaySources.Lava::new);
	public static final DeferredHolder<DisplaySource, MagmaEngineDisplaySources.Output> MAGMA_ENGINE_OUTPUT =
		SOURCES.register("magma_engine_output", MagmaEngineDisplaySources.Output::new);
	public static final DeferredHolder<DisplaySource, WellspringDisplaySources.Fluid> WELLSPRING_FLUID =
		SOURCES.register("wellspring_fluid", WellspringDisplaySources.Fluid::new);
	public static final DeferredHolder<DisplaySource, WellspringDisplaySources.FillPercent> WELLSPRING_FILL =
		SOURCES.register("wellspring_fill", WellspringDisplaySources.FillPercent::new);
	public static final DeferredHolder<DisplaySource, WellspringDisplaySources.FillBar> WELLSPRING_FILL_BAR =
		SOURCES.register("wellspring_fill_bar", WellspringDisplaySources.FillBar::new);
	public static final DeferredHolder<DisplaySource, WellspringDisplaySources.Generation> WELLSPRING_GENERATION =
		SOURCES.register("wellspring_generation", WellspringDisplaySources.Generation::new);

	/** From common setup: which blocks offer which sources. */
	public static void bindBlocks() {
		DisplaySource.BY_BLOCK.add(ModBlocks.MAGMA_ENGINE.get(), MAGMA_ENGINE_LAVA.get());
		DisplaySource.BY_BLOCK.add(ModBlocks.MAGMA_ENGINE.get(), MAGMA_ENGINE_OUTPUT.get());
		DisplaySource.BY_BLOCK.add(ModBlocks.WELLSPRING.get(), WELLSPRING_FLUID.get());
		DisplaySource.BY_BLOCK.add(ModBlocks.WELLSPRING.get(), WELLSPRING_FILL.get());
		DisplaySource.BY_BLOCK.add(ModBlocks.WELLSPRING.get(), WELLSPRING_FILL_BAR.get());
		DisplaySource.BY_BLOCK.add(ModBlocks.WELLSPRING.get(), WELLSPRING_GENERATION.get());
	}

	private ModDisplaySources() {}
}
