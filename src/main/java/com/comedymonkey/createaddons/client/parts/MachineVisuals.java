package com.comedymonkey.createaddons.client.parts;

import java.util.function.Supplier;

import com.comedymonkey.createaddons.client.AnimalPenRenderer;
import com.comedymonkey.createaddons.client.AutoHarvesterRenderer;
import com.comedymonkey.createaddons.client.CobbleGeneratorRenderer;
import com.comedymonkey.createaddons.client.CuckooClockRenderer;
import com.comedymonkey.createaddons.client.ElectricRenderers;
import com.comedymonkey.createaddons.client.FluidLineRenderer;
import com.comedymonkey.createaddons.client.MachineRenderers;
import com.comedymonkey.createaddons.client.MagmaEngineRenderer;
import com.comedymonkey.createaddons.client.PressureCrucibleRenderer;
import com.comedymonkey.createaddons.client.WellspringRenderer;
import com.comedymonkey.createaddons.client.line.PowerLineRenderer;
import com.comedymonkey.createaddons.compat.sable.SableCompat;
import com.comedymonkey.createaddons.content.line.Fitting;
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
		visual(ModBlockEntities.WINDING_KEY, com.comedymonkey.createaddons.client.juggernaut.WindingKeyRenderer::parts, true);
		visual(ModBlockEntities.SPRING_ENGINE, com.comedymonkey.createaddons.client.juggernaut.SpringEngineRenderer::parts, true);
		visual(ModBlockEntities.LAVA_GENERATOR, ElectricRenderers.Generator::parts, true);
		visual(ModBlockEntities.GEOTHERMAL_CONTROLLER, ElectricRenderers.Turbine::parts, true);
		// a Battery's tubes and lamps; formed, only the master draws (a 9-high tower's column and arcs, 11 blocks up)
		SimpleBlockEntityVisualizer.builder(ModBlockEntities.BATTERY.get())
			.factory((context, be, partialTick) -> new PartsVisual<>(context, be, partialTick,
				com.comedymonkey.createaddons.client.BatteryRenderer::parts, b -> !b.getBlockState().getValue(
					com.comedymonkey.createaddons.content.electric.BatteryBlock.FORMED) || b.isMaster()).reaching(11))
			.skipVanillaRender(be -> true)
			.apply();
		visual(ModBlockEntities.PRESSURE_CRUCIBLE, PressureCrucibleRenderer::parts, false);
		visual(ModBlockEntities.STRING_TANGLER, MachineRenderers.Tangler::parts, false);
		visual(ModBlockEntities.STONE_INFUSER, MachineRenderers.Infuser::parts, false);
		visual(ModBlockEntities.AUTO_HARVESTER, AutoHarvesterRenderer::parts, false);
		visual(ModBlockEntities.WELLSPRING, WellspringRenderer::parts, false);
		visual(ModBlockEntities.ANIMAL_PEN, AnimalPenRenderer::parts, false);
		// the cages' and tanks' water and the Netherite shield stay with their renderers (translucent)
		visual(ModBlockEntities.MONSTER_CAGE, com.comedymonkey.createaddons.client.MonsterCageRenderer::parts, false);
		visual(ModBlockEntities.FISH_TANK, com.comedymonkey.createaddons.client.FishTankRenderer::parts, false);
		visual(ModBlockEntities.CUCKOO_CLOCK, CuckooClockRenderer::parts, false);
		// the lines' fittings; a Fluid Line's fluid stays with its renderer, a Power Line has nothing else
		// (most pieces carry no fitting: their visuals do nothing a frame)
		// (most pieces carry no fitting: theirs is an idle visual, made again when a fitting goes on)
		visualWhen(ModBlockEntities.FLUID_LINE, FluidLineRenderer::parts, false, be -> be.fitting() != Fitting.NONE && be.encasing() == null);
		visualWhen(ModBlockEntities.POWER_LINE, PowerLineRenderer::parts, true, be -> be.fitting() != Fitting.NONE && be.encasing() == null);
		// an Electric Light's lens, only while it's lit (dark, its glass is in the block model)
		// (the Floodlight's renderer still runs, for its cone)
		visualWhen(ModBlockEntities.LIGHT, com.comedymonkey.createaddons.client.LightRenderer::parts,
			be -> be.kind() != com.comedymonkey.createaddons.content.light.LightKind.FLOODLIGHT, com.comedymonkey.createaddons.client.LightRenderer::active);
		// an Upgraded Chest's lid, only while it's open (closed, it's part of the block model)
		visualWhen(ModBlockEntities.UPGRADED_CHEST, com.comedymonkey.createaddons.client.UpgradedChestRenderer::parts, true,
			com.comedymonkey.createaddons.client.UpgradedChestRenderer::open);
		// Trade Bot's winding station
		SimpleBlockEntityVisualizer.builder(ModBlockEntities.TRADE_BOT_STATION.get())
			.factory(com.comedymonkey.createaddons.client.tradebot.TradeBotStationVisual::new)
			.skipVanillaRender(be -> true)
			.apply();
		// Helper Bot, posed every frame (his renderer stays for Ponder, the shop screen, and when Flywheel can't draw)
		SimpleBlockEntityVisualizer.builder(ModBlockEntities.HELPER_BOT.get())
			.factory(com.comedymonkey.createaddons.client.helperbot.HelperBotVisual::new)
			.skipVanillaRender(be -> true)
			.apply();
		// the spans between Line Posts: laid out once, as instances
		SimpleBlockEntityVisualizer.builder(ModBlockEntities.LINE_POST.get())
			.factory(com.comedymonkey.createaddons.client.line.LinePostVisual::new)
			.skipVanillaRender(be -> true)
			.apply();
		// the Compact Gyroscope and Torque Rotor only exist with Sable
		if (SableCompat.LOADED)
			AeronauticsRenderers.registerVisuals();
	}

	/**
	 * A visual that's only needed some of the time: its parts while {@code active} (a line's fitting, a chest's open
	 * lid), else an idle visual that does nothing a frame. The client makes it again when that changes.
	 */
	public static <T extends BlockEntity> void visualWhen(Supplier<BlockEntityType<T>> type, PartsVisual.Parts<T> parts, boolean partsOnly,
															java.util.function.Predicate<T> active) {
		visualWhen(type, parts, be -> partsOnly, active);
	}

	/** As above, {@code partsOnly} asked of each block entity (one whose renderer draws something more keeps it). */
	public static <T extends BlockEntity> void visualWhen(Supplier<BlockEntityType<T>> type, PartsVisual.Parts<T> parts,
															java.util.function.Predicate<T> partsOnly, java.util.function.Predicate<T> active) {
		SimpleBlockEntityVisualizer.builder(type.get())
			.factory((context, be, partialTick) -> active.test(be) ? new PartsVisual<>(context, be, partialTick, parts, active)
				: new IdleVisual<>(context, be, partialTick))
			.skipVanillaRender(partsOnly::test)
			.apply();
	}

	/** A block entity with nothing for Flywheel to draw (a plain line). */
	private static final class IdleVisual<T extends BlockEntity> extends dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual<T> {
		IdleVisual(dev.engine_room.flywheel.api.visualization.VisualizationContext context, T be, float partialTick) {
			super(context, be, partialTick);
		}

		@Override
		public void updateLight(float partialTick) {}

		@Override
		public void collectCrumblingInstances(java.util.function.Consumer<dev.engine_room.flywheel.api.instance.Instance> consumer) {}

		@Override
		protected void _delete() {}
	}

	/** {@code partsOnly}: the renderer draws nothing else, so it needn't run while the visual draws. */
	public static <T extends BlockEntity> void visual(Supplier<BlockEntityType<T>> type, PartsVisual.Parts<T> parts,
													   boolean partsOnly) {
		visual(type, parts, partsOnly, be -> true);
	}

	/** {@code active}: whether it has parts to show now (checked every frame before anything else). */
	public static <T extends BlockEntity> void visual(Supplier<BlockEntityType<T>> type, PartsVisual.Parts<T> parts,
													   boolean partsOnly, java.util.function.Predicate<T> active) {
		SimpleBlockEntityVisualizer.builder(type.get())
			.factory((context, be, partialTick) -> new PartsVisual<>(context, be, partialTick, parts, active))
			.skipVanillaRender(be -> partsOnly)
			.apply();
	}
}
