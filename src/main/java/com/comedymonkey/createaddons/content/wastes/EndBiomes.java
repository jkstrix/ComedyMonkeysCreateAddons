package com.comedymonkey.createaddons.content.wastes;

import java.util.List;
import java.util.function.IntSupplier;

import org.slf4j.Logger;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.SurfaceRules;
import terrablender.api.EndBiomeRegistry;
import terrablender.api.SurfaceRuleManager;

/**
 * The mod's End biomes and the TerraBlender slots they take (design/specs/clockwork-wastes.md, P1, D28, D34): the
 * Clockwork Wastes now, End biomes 2 and 3 later, each one entry in {@link #SLOTS}. Every biome here goes in the outer
 * End's highlands and midlands, never the island slot (bugged on 1.21.1, TerraBlender #179) and never the central
 * island (TerraBlender keeps the first ~1,024 blocks vanilla). Vanilla weighs 10 in each slot, so a weight of 1 makes
 * about 1 in 11 of that land the biome.
 */
public final class EndBiomes {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final ResourceKey<Biome> CLOCKWORK_WASTES = ResourceKey.create(Registries.BIOME,
		ComedyMonkeysCreateAddons.asResource("clockwork_wastes"));

	/** An End biome and its weight in the highlands and midlands (read once, at startup). */
	public record Slot(ResourceKey<Biome> biome, IntSupplier weight) {}

	/** Biomes 2 and 3 slot in beside the Wastes. */
	public static final List<Slot> SLOTS = List.of(
		new Slot(CLOCKWORK_WASTES, CMConfig.COMMON::clockworkWastesWeight));

	private static boolean registered;

	private EndBiomes() {}

	/** Hands every slot to TerraBlender, with the surface rules. Common setup, on the main thread; only once. */
	public static void register() {
		if (registered)
			return;
		registered = true;
		for (Slot slot : SLOTS) {
			int weight = slot.weight().getAsInt();
			if (weight <= 0)
				continue;
			EndBiomeRegistry.registerHighlandsBiome(slot.biome(), weight);
			EndBiomeRegistry.registerMidlandsBiome(slot.biome(), weight);
		}
		SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.END, ComedyMonkeysCreateAddons.MOD_ID, surfaceRules());
	}

	/**
	 * The Wastes' ground: its top layer Scorched End Stone, with rust-stained and littered patches where the surface
	 * noise runs high or low; the rest of the island stays End Stone (the End's own rule).
	 */
	public static SurfaceRules.RuleSource surfaceRules() {
		SurfaceRules.RuleSource scorched = SurfaceRules.state(ModWastes.SCORCHED_END_STONE.get().defaultBlockState());
		SurfaceRules.RuleSource rusted = SurfaceRules.state(ModWastes.RUSTED_SCORCHED_END_STONE.get().defaultBlockState());
		SurfaceRules.RuleSource littered = SurfaceRules.state(ModWastes.LITTERED_SCORCHED_END_STONE.get().defaultBlockState());
		var noise = net.minecraft.world.level.levelgen.Noises.SURFACE;
		SurfaceRules.RuleSource top = SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.noiseCondition(noise, 0.3), rusted),
			SurfaceRules.ifTrue(SurfaceRules.noiseCondition(noise, -10.0, -0.45), littered),
			scorched);
		return SurfaceRules.ifTrue(SurfaceRules.isBiome(CLOCKWORK_WASTES), SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, top));
	}

	/**
	 * P53: if another mod or a data pack replaces the End's biome source, TerraBlender can't place the Wastes there.
	 * Logs one warning at server start; the city's highlands fallback is phase 2's.
	 * @return true when the End can hold the Wastes
	 */
	public static boolean checkEnd(MinecraftServer server) {
		ServerLevel end = server.getLevel(Level.END);
		if (end == null)
			return true;
		var source = end.getChunkSource().getGenerator().getBiomeSource();
		boolean ok = source instanceof TheEndBiomeSource && source.possibleBiomes().stream().anyMatch(b -> b.is(CLOCKWORK_WASTES));
		if (!ok && SLOTS.stream().anyMatch(s -> s.weight().getAsInt() > 0))
			LOGGER.warn("The End's biomes come from {}, not TerraBlender's End: the Clockwork Wastes can't appear in this world"
				+ " (another End mod or a data pack has replaced them).", source.getClass().getName());
		return ok;
	}
}
