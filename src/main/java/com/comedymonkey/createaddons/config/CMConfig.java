package com.comedymonkey.createaddons.config;

import org.apache.commons.lang3.tuple.Pair;

import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.wellspring.WellspringSource;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config specs for the mod. These are plain NeoForge specs, so they are editable from
 * Create's config menu (Mods > Create > Config > "Access configs of other mods"),
 * NeoForge's mod list config button, or the files in /config and /serverconfig.
 */
public final class CMConfig {
	public static final ModConfigSpec SERVER_SPEC;
	public static final Server SERVER;
	public static final ModConfigSpec CLIENT_SPEC;
	public static final Client CLIENT;

	static {
		Pair<Server, ModConfigSpec> server = new ModConfigSpec.Builder().configure(Server::new);
		SERVER = server.getLeft();
		SERVER_SPEC = server.getRight();

		Pair<Client, ModConfigSpec> client = new ModConfigSpec.Builder().configure(Client::new);
		CLIENT = client.getLeft();
		CLIENT_SPEC = client.getRight();
	}

	public static final int MAX_UPGRADE_SLOTS = 3;

	private CMConfig() {}

	public static class Server {
		public final ModConfigSpec.IntValue tankCapacity;
		public final ModConfigSpec.IntValue maxUpgrades;
		public final ModConfigSpec.IntValue capacityPerUpgrade;
		public final ModConfigSpec.IntValue maxCapacityUpgrades;

		public final ModConfigSpec.IntValue waterTier1;
		public final ModConfigSpec.IntValue waterTier2;
		public final ModConfigSpec.IntValue waterTier3;

		public final ModConfigSpec.IntValue lavaTier1;
		public final ModConfigSpec.IntValue lavaTier2;
		public final ModConfigSpec.IntValue lavaTier3;

		public final ModConfigSpec.IntValue honeyTier1;
		public final ModConfigSpec.IntValue honeyTier2;
		public final ModConfigSpec.IntValue honeyTier3;

		public final ModConfigSpec.IntValue chocolateTier1;
		public final ModConfigSpec.IntValue chocolateTier2;
		public final ModConfigSpec.IntValue chocolateTier3;

		public final ModConfigSpec.IntValue copperChestMultiplier;
		public final ModConfigSpec.IntValue ironChestMultiplier;
		public final ModConfigSpec.IntValue goldChestMultiplier;
		public final ModConfigSpec.IntValue netheriteChestMultiplier;

		public final ModConfigSpec.DoubleValue crucibleStressImpact;
		public final ModConfigSpec.DoubleValue andesiteCobbleGenStressImpact;
		public final ModConfigSpec.DoubleValue brassCobbleGenStressImpact;
		public final ModConfigSpec.IntValue harvesterStress;
		public final ModConfigSpec.IntValue harvesterCropSeconds;
		public final ModConfigSpec.IntValue harvesterTreeSeconds;
		public final ModConfigSpec.DoubleValue harvesterBountifulBonus;
		public final ModConfigSpec.IntValue harvesterTreeLogs;
		public final ModConfigSpec.IntValue harvesterLeafRolls;
		public final ModConfigSpec.IntValue penStress;
		public final ModConfigSpec.IntValue brassPenStress;
		public final ModConfigSpec.IntValue penCapacity;
		public final ModConfigSpec.IntValue brassPenCapacity;
		public final ModConfigSpec.IntValue penBreedSeconds;
		public final ModConfigSpec.DoubleValue penFruitfulBonus;
		public final ModConfigSpec.IntValue penGrowSeconds;
		public final ModConfigSpec.IntValue penWoolSeconds;
		public final ModConfigSpec.IntValue penEggSeconds;
		public final ModConfigSpec.IntValue penMilkPerMinute;
		public final ModConfigSpec.DoubleValue tanglerStressImpact;
		public final ModConfigSpec.DoubleValue infuserStressImpact;

		public final ModConfigSpec.IntValue magmaEngineSpeed;
		public final ModConfigSpec.DoubleValue magmaEngineCapacity;
		public final ModConfigSpec.IntValue magmaEngineLavaPerSecond;
		public final ModConfigSpec.IntValue superchargedMagmaEngineSpeed;
		public final ModConfigSpec.DoubleValue superchargedMagmaEngineCapacity;
		public final ModConfigSpec.IntValue superchargedMagmaEngineLavaPerSecond;
		public final ModConfigSpec.IntValue motorMaxStress;
		public final ModConfigSpec.DoubleValue motorFePerStress;
		public final ModConfigSpec.IntValue generatorFePerTick;
		public final ModConfigSpec.IntValue generatorLavaPerSecond;
		public final ModConfigSpec.IntValue turbineFePerTick;
		public final ModConfigSpec.IntValue turbineLavaPerSecond;
		public final ModConfigSpec.IntValue turbineWaterPerSecond;

		public final ModConfigSpec.DoubleValue gyroStrength;
		public final ModConfigSpec.DoubleValue gyroDamping;
		public final ModConfigSpec.DoubleValue gyroIntegral;
		public final ModConfigSpec.DoubleValue gyroIntegralLimit;
		public final ModConfigSpec.DoubleValue gyroReferenceRpm;
		public final ModConfigSpec.DoubleValue gyroStressImpact;
		public final ModConfigSpec.DoubleValue torqueRotorThrust;
		public final ModConfigSpec.DoubleValue torqueRotorStressImpact;

		public final ModConfigSpec.BooleanValue pipezFeedsBlazeBurners;
		public final ModConfigSpec.BooleanValue blazeBurnerPipezOnly;
		public final ModConfigSpec.BooleanValue pipezFeedsEngines;

		Server(ModConfigSpec.Builder builder) {
			builder.comment("Fluid Wellspring settings").push("fluidWellspring");

			tankCapacity = builder
				.comment("How much fluid a Fluid Wellspring can hold without Capacity Upgrades, in millibuckets.")
				.translation("comedymonkeysaddons.configuration.tankCapacity")
				.defineInRange("tankCapacity", 8000, 1000, 256000);

			maxUpgrades = builder
				.comment("How many generation upgrades (Water or Dripstone) can be installed in a single Fluid Wellspring.")
				.translation("comedymonkeysaddons.configuration.maxGenerationUpgrades")
				.defineInRange("maxGenerationUpgrades", MAX_UPGRADE_SLOTS, 1, MAX_UPGRADE_SLOTS);

			capacityPerUpgrade = builder
				.comment("Extra capacity, in millibuckets, added by each Capacity Upgrade.")
				.translation("comedymonkeysaddons.configuration.capacityPerUpgrade")
				.defineInRange("capacityPerUpgrade", 16000, 1000, 256000);

			maxCapacityUpgrades = builder
				.comment("How many Capacity Upgrades can be installed in a single Fluid Wellspring. 0 disables them.")
				.translation("comedymonkeysaddons.configuration.maxCapacityUpgrades")
				.defineInRange("maxCapacityUpgrades", MAX_UPGRADE_SLOTS, 0, MAX_UPGRADE_SLOTS);

			builder.comment("Water generated, in millibuckets per second, with 1, 2 or 3 Water Upgrades installed.")
				.push("waterGeneration");
			waterTier1 = rate(builder, "oneUpgrade", 100);
			waterTier2 = rate(builder, "twoUpgrades", 250);
			waterTier3 = rate(builder, "threeUpgrades", 500);
			builder.pop();

			builder.comment("Lava generated, in millibuckets per second, with 1, 2 or 3 Lava Upgrades installed.")
				.push("lavaGeneration");
			lavaTier1 = rate(builder, "oneUpgrade", 10);
			lavaTier2 = rate(builder, "twoUpgrades", 25);
			lavaTier3 = rate(builder, "threeUpgrades", 50);
			builder.pop();

			builder.comment("Create honey generated, in millibuckets per second, with 1, 2 or 3 Honey Upgrades installed.")
				.push("honeyGeneration");
			honeyTier1 = rate(builder, "oneUpgrade", 10);
			honeyTier2 = rate(builder, "twoUpgrades", 25);
			honeyTier3 = rate(builder, "threeUpgrades", 50);
			builder.pop();

			builder.comment("Create chocolate generated, in millibuckets per second, with 1, 2 or 3 Chocolate Upgrades installed.")
				.push("chocolateGeneration");
			chocolateTier1 = rate(builder, "oneUpgrade", 10);
			chocolateTier2 = rate(builder, "twoUpgrades", 25);
			chocolateTier3 = rate(builder, "threeUpgrades", 50);
			builder.pop();

			builder.pop();

			builder.comment("Stack size multipliers for upgraded chests. Unstackable items always stay at 1.",
					"Diamond chests use the Gold multiplier and get twice the slots instead (54 per chest, 108 as a double).",
					"Netherite chests have Diamond's slots and their own, bigger multiplier.")
				.push("chestUpgrades");
			copperChestMultiplier = multiplier(builder, "copperStackMultiplier", 2);
			ironChestMultiplier = multiplier(builder, "ironStackMultiplier", 4);
			goldChestMultiplier = multiplier(builder, "goldStackMultiplier", 8);
			netheriteChestMultiplier = multiplier(builder, "netheriteStackMultiplier", 16);
			builder.pop();

			builder.comment("Pressure Crucible settings. Its recipes (dust needed, heat, time) are data-driven:",
					"see data/comedymonkeysaddons/recipe/pressure_crucible/ in the mod jar.")
				.push("pressureCrucible");
			crucibleStressImpact = builder
				.comment("Stress impact per RPM. At 256 RPM the default of 12 needs 3072 stress units.")
				.translation("comedymonkeysaddons.configuration.crucibleStressImpact")
				.defineInRange("stressImpact", 12.0, 0.0, 1024.0);
			builder.pop();

			builder.comment("Cobblestone Generators. They make cobblestone exactly as fast as a Mechanical Drill at the same",
					"speed would mine it (10 ticks per block at 256 RPM); the Brass one makes 16 at a time.")
				.push("cobbleGenerators");
			andesiteCobbleGenStressImpact = builder
				.comment("Stress impact per RPM of the Andesite Cobblestone Generator (a Mechanical Drill is 4).")
				.translation("comedymonkeysaddons.configuration.andesiteCobbleGenStressImpact")
				.defineInRange("andesiteStressImpact", 4.0, 0.0, 1024.0);
			brassCobbleGenStressImpact = builder
				.comment("Stress impact per RPM of the Brass Cobblestone Generator.")
				.translation("comedymonkeysaddons.configuration.brassCobbleGenStressImpact")
				.defineInRange("brassStressImpact", 16.0, 0.0, 1024.0);
			builder.pop();

			builder.comment("Auto Harvester: grows a crop or sapling and harvests it. It uses the same stress at any speed and",
					"runs no faster for more of it; only the Bountiful enchantment speeds it up.")
				.push("autoHarvester");
			harvesterStress = builder
				.comment("Stress it puts on the network, in SU, whatever the speed.")
				.translation("comedymonkeysaddons.configuration.harvesterStress")
				.defineInRange("stress", 512, 0, 65536);
			harvesterCropSeconds = builder
				.comment("Seconds a crop takes to grow before each harvest, without Bountiful.")
				.translation("comedymonkeysaddons.configuration.harvesterCropSeconds")
				.defineInRange("cropSeconds", 40, 1, 3600);
			harvesterTreeSeconds = builder
				.comment("Seconds a tree takes to grow before it is felled, without Bountiful.")
				.translation("comedymonkeysaddons.configuration.harvesterTreeSeconds")
				.defineInRange("treeSeconds", 80, 1, 3600);
			harvesterBountifulBonus = builder
				.comment("Extra growing speed per level of Bountiful: 0.5 makes Bountiful I 1.5x, II 2x and III 2.5x as fast.")
				.translation("comedymonkeysaddons.configuration.harvesterBountifulBonus")
				.defineInRange("bountifulBonusPerLevel", 0.5, 0.0, 10.0);
			harvesterTreeLogs = builder
				.comment("Logs a felled tree gives at least (up to two more at random).")
				.translation("comedymonkeysaddons.configuration.harvesterTreeLogs")
				.defineInRange("treeLogs", 4, 0, 64);
			harvesterLeafRolls = builder
				.comment("Times a felled tree's leaves roll their drops (saplings, sticks, apples), as if broken by hand.")
				.translation("comedymonkeysaddons.configuration.harvesterLeafRolls")
				.defineInRange("leafRolls", 8, 0, 64);
			builder.pop();

			builder.comment("Animal Pens: hold one captured animal and breed it up. They use the same stress at any speed,",
					"and only breed, regrow wool, lay eggs and make milk while they turn.")
				.push("animalPen");
			penStress = builder
				.comment("Stress the Animal Pen puts on the network, in SU, whatever the speed.")
				.translation("comedymonkeysaddons.configuration.penStress")
				.defineInRange("stress", 256, 0, 65536);
			brassPenStress = builder
				.comment("Stress the Brass Animal Pen puts on the network, in SU, whatever the speed.")
				.translation("comedymonkeysaddons.configuration.brassPenStress")
				.defineInRange("brassStress", 1024, 0, 65536);
			penCapacity = builder
				.comment("Animals an Animal Pen holds (its trough holds food for half as many, enough to double them).")
				.translation("comedymonkeysaddons.configuration.penCapacity")
				.defineInRange("maxAnimals", 128, 1, 4096);
			brassPenCapacity = builder
				.comment("Animals a Brass Animal Pen holds (its trough holds food for half as many).")
				.translation("comedymonkeysaddons.configuration.brassPenCapacity")
				.defineInRange("brassMaxAnimals", 512, 1, 16384);
			penBreedSeconds = builder
				.comment("Seconds between breedings, as long as a vanilla animal's wait after breeding (300). Each time, every",
					"animal eats one of its food and has a baby, so the herd doubles: 1, 2, 4, 8, 16...")
				.translation("comedymonkeysaddons.configuration.penBreedSeconds")
				.defineInRange("secondsPerBreeding", 300, 1, 36000);
			penGrowSeconds = builder
				.comment("Seconds a baby takes to grow up, as in vanilla (1200, twenty minutes). Only adults breed, give drops,",
					"wool, eggs and milk.")
				.translation("comedymonkeysaddons.configuration.penGrowSeconds")
				.defineInRange("secondsToGrowUp", 1200, 1, 72000);
			penFruitfulBonus = builder
				.comment("Extra speed per level of Fruitful, for breeding and growing up: 0.5 makes I 1.5x, II 2x, III 2.5x as fast.")
				.translation("comedymonkeysaddons.configuration.penFruitfulBonus")
				.defineInRange("fruitfulBonusPerLevel", 0.5, 0.0, 10.0);
			penWoolSeconds = builder
				.comment("Seconds for sheared sheep to grow their wool back.")
				.translation("comedymonkeysaddons.configuration.penWoolSeconds")
				.defineInRange("woolSeconds", 60, 1, 3600);
			penEggSeconds = builder
				.comment("Average seconds between eggs (or armadillo scutes) from each animal; vanilla is 300 to 600.")
				.translation("comedymonkeysaddons.configuration.penEggSeconds")
				.defineInRange("eggSeconds", 450, 1, 36000);
			penMilkPerMinute = builder
				.comment("Milk each cow or goat gives a minute, in millibuckets.")
				.translation("comedymonkeysaddons.configuration.penMilkPerMinute")
				.defineInRange("milkPerMinute", 125, 0, 100000);
			builder.pop();

			builder.comment("String Tangler and Stone Infuser. Their recipes (ingredients, counts, times) are data-driven:",
					"see data/comedymonkeysaddons/recipe/tangling/ and infusing/ in the mod jar.")
				.push("machines");
			tanglerStressImpact = builder
				.comment("Stress impact per RPM of the String Tangler.")
				.translation("comedymonkeysaddons.configuration.tanglerStressImpact")
				.defineInRange("tanglerStressImpact", 4.0, 0.0, 1024.0);
			infuserStressImpact = builder
				.comment("Stress impact per RPM of the Stone Infuser.")
				.translation("comedymonkeysaddons.configuration.infuserStressImpact")
				.defineInRange("infuserStressImpact", 8.0, 0.0, 1024.0);
			builder.pop();

			builder.comment("Magma Engine: burns lava to drive a shaft. The defaults are four times a Portable Engine.")
				.push("magmaEngine");
			magmaEngineSpeed = builder
				.comment("Output speed in RPM.")
				.translation("comedymonkeysaddons.configuration.magmaEngineSpeed")
				.defineInRange("speed", 64, 1, 256);
			magmaEngineCapacity = builder
				.comment("Stress capacity per RPM. At the default 64 RPM, 128 gives 8192 stress units.")
				.translation("comedymonkeysaddons.configuration.magmaEngineCapacity")
				.defineInRange("capacityPerRpm", 128.0, 0.0, 16384.0);
			magmaEngineLavaPerSecond = builder
				.comment("Lava burned while running, in millibuckets per second.")
				.translation("comedymonkeysaddons.configuration.magmaEngineLavaPerSecond")
				.defineInRange("lavaPerSecond", 10, 0, 1000);
			builder.pop();

			builder.comment("Supercharged Magma Engine: twice a Magma Engine's speed and power, for twice the lava.")
				.push("superchargedMagmaEngine");
			superchargedMagmaEngineSpeed = builder
				.comment("Output speed in RPM.")
				.translation("comedymonkeysaddons.configuration.superchargedMagmaEngineSpeed")
				.defineInRange("speed", 128, 1, 256);
			superchargedMagmaEngineCapacity = builder
				.comment("Stress capacity per RPM. At the default 128 RPM, 128 gives 16384 stress units.")
				.translation("comedymonkeysaddons.configuration.superchargedMagmaEngineCapacity")
				.defineInRange("capacityPerRpm", 128.0, 0.0, 16384.0);
			superchargedMagmaEngineLavaPerSecond = builder
				.comment("Lava burned while running, in millibuckets per second.")
				.translation("comedymonkeysaddons.configuration.superchargedMagmaEngineLavaPerSecond")
				.defineInRange("lavaPerSecond", 20, 0, 1000);
			builder.pop();

			builder.comment("Electricity: the Lava Generator and Geothermal Turbine make Forge Energy (FE), the Electric Motor",
					"turns it into rotation. The defaults let one Lava Generator run a motor at 16,384 SU, and one turbine",
					"run two and a half motors flat out.").push("electricity");
			motorMaxStress = builder
				.comment("The most stress an Electric Motor can be set to give, in SU (a full superheated boiler is 294,912).")
				.translation("comedymonkeysaddons.configuration.motorMaxStress")
				.defineInRange("motorMaxStress", 294912, 256, 4194304);
			motorFePerStress = builder
				.comment("FE an Electric Motor draws per tick for each SU it's set to give.")
				.translation("comedymonkeysaddons.configuration.motorFePerStress")
				.defineInRange("motorFePerStress", 0.0625, 0.0, 64.0);
			generatorFePerTick = builder
				.comment("FE a Lava Generator makes per tick while it burns.")
				.translation("comedymonkeysaddons.configuration.generatorFePerTick")
				.defineInRange("generatorFePerTick", 1024, 0, 1000000);
			generatorLavaPerSecond = builder
				.comment("Lava a Lava Generator burns, in millibuckets per second.")
				.translation("comedymonkeysaddons.configuration.generatorLavaPerSecond")
				.defineInRange("generatorLavaPerSecond", 20, 0, 10000);
			turbineFePerTick = builder
				.comment("FE a Geothermal Turbine makes per tick while it runs.")
				.translation("comedymonkeysaddons.configuration.turbineFePerTick")
				.defineInRange("turbineFePerTick", 46080, 0, 10000000);
			turbineLavaPerSecond = builder
				.comment("Lava a Geothermal Turbine burns, in millibuckets per second (ten times as efficient as the Lava Generator).")
				.translation("comedymonkeysaddons.configuration.turbineLavaPerSecond")
				.defineInRange("turbineLavaPerSecond", 90, 0, 10000);
			turbineWaterPerSecond = builder
				.comment("Water a Geothermal Turbine boils off, in millibuckets per second.")
				.translation("comedymonkeysaddons.configuration.turbineWaterPerSecond")
				.defineInRange("turbineWaterPerSecond", 250, 0, 10000);
			builder.pop();

			builder.comment("Create Aeronautics blocks (only present when Sable is installed)").push("aeronautics");
			builder.comment("Compact Gyroscope: holds a ship level, like the Gyroscope Stabilizers' Gyroscope, whose",
					"defaults these match. Effectiveness grows with speed up to the reference RPM.")
				.push("compactGyroscope");
			gyroStrength = builder
				.comment("Restoring torque gain at the reference RPM, in Nm (peak torque is roughly this times sin(tilt)).")
				.translation("comedymonkeysaddons.configuration.gyroStrength")
				.defineInRange("strength", 16000.0, 0.0, 1.0E9);
			gyroDamping = builder
				.comment("Angular velocity damping at the reference RPM, in Nm·s/rad.")
				.translation("comedymonkeysaddons.configuration.gyroDamping")
				.defineInRange("damping", 4000.0, 0.0, 1.0E9);
			gyroIntegral = builder
				.comment("Integral gain at the reference RPM: removes the small lean left by off-centre weight.")
				.translation("comedymonkeysaddons.configuration.gyroIntegral")
				.defineInRange("integral", 6400.0, 0.0, 1.0E9);
			gyroIntegralLimit = builder
				.comment("Cap on the integral term, so a held or stuck ship recovers quickly once released.")
				.translation("comedymonkeysaddons.configuration.gyroIntegralLimit")
				.defineInRange("integralLimit", 0.5, 0.0, 100.0);
			gyroReferenceRpm = builder
				.comment("Speed at which it reaches full strength.")
				.translation("comedymonkeysaddons.configuration.gyroReferenceRpm")
				.defineInRange("referenceRpm", 256.0, 1.0, 4096.0);
			gyroStressImpact = builder
				.comment("Stress impact per RPM.")
				.translation("comedymonkeysaddons.configuration.gyroStressImpact")
				.defineInRange("stressImpact", 16.0, 0.0, 1024.0);
			builder.pop();
			builder.comment("Torque Rotor: turns a ship the way a propeller in its place would, without pushing it sideways.")
				.push("torqueRotor");
			torqueRotorThrust = builder
				.comment("Thrust per RPM of its blades. The default of 1 matches Create Aeronautics' Andesite Propeller: it turns",
					"the ship as hard as an Andesite Propeller at the same spot and speed would. Further from the ship's",
					"centre of mass turns harder.")
				.translation("comedymonkeysaddons.configuration.torqueRotorThrust")
				.defineInRange("thrust", 1.0, 0.0, 1000.0);
			torqueRotorStressImpact = builder
				.comment("Stress impact per RPM.")
				.translation("comedymonkeysaddons.configuration.torqueRotorStressImpact")
				.defineInRange("stressImpact", 4.0, 0.0, 1024.0);
			builder.pop();
			builder.pop();

			builder.comment("Compatibility with other mods").push("modCompatibility");

			pipezFeedsBlazeBurners = builder
				.comment("Let Pipez item pipes feed fuel into Create Blaze Burners. Only has an effect when Pipez is installed.")
				.translation("comedymonkeysaddons.configuration.pipezFeedsBlazeBurners")
				.define("pipezFeedsBlazeBurners", true);

			blazeBurnerPipezOnly = builder
				.comment("When false (default), any item-moving block (Pipez pipes, hoppers, funnels, other mods' pipes) can",
					"feed fuel into Blaze Burners. When true, only Pipez pipes can. Only has an effect when Pipez is installed.")
				.translation("comedymonkeysaddons.configuration.onlyPipezCanFeedBlazeBurners")
				.define("onlyPipezCanFeedBlazeBurners", false);

			pipezFeedsEngines = builder
				.comment("Let Pipez fluid pipes (and other fluid transport) fuel Portable Engines from Create Aeronautics with lava",
					"and other fuel fluids. They already take coal and other burnable items from item pipes.",
					"Only has an effect when Pipez and Create Aeronautics are installed.")
				.translation("comedymonkeysaddons.configuration.pipezFeedsEngines")
				.define("pipezFeedsEngines", true);

			builder.pop();
		}

		private static ModConfigSpec.IntValue rate(ModConfigSpec.Builder builder, String name, int def) {
			return builder
				.comment("Millibuckets generated per second.")
				.translation("comedymonkeysaddons.configuration." + name)
				.defineInRange(name, def, 0, 100000);
		}

		private static ModConfigSpec.IntValue multiplier(ModConfigSpec.Builder builder, String name, int def) {
			return builder
				.comment("Stack size multiplier. With 64-stack items, 2 = 128 per slot, 16 = 1024 per slot.")
				.translation("comedymonkeysaddons.configuration." + name)
				.defineInRange(name, def, 1, 64);
		}

		/** Stack size multiplier for a chest of the given tier. */
		public int chestMultiplier(ChestTier tier) {
			return get(switch (tier) {
				case COPPER -> copperChestMultiplier;
				case IRON -> ironChestMultiplier;
				// Diamond keeps Gold's stack size so the counts still fit in a slot; it doubles the slots instead
				case GOLD, DIAMOND -> goldChestMultiplier;
				case NETHERITE -> netheriteChestMultiplier;
			});
		}

		/** Generation rate in mB/s for the given source and installed upgrade count. */
		public int generationRate(WellspringSource source, int upgrades) {
			if (!SERVER_SPEC.isLoaded() || upgrades <= 0)
				return 0;
			int tier = Math.min(upgrades, maxUpgrades.get());
			return switch (source) {
				case WATER -> switch (tier) {
					case 1 -> waterTier1.get();
					case 2 -> waterTier2.get();
					default -> waterTier3.get();
				};
				case LAVA -> switch (tier) {
					case 1 -> lavaTier1.get();
					case 2 -> lavaTier2.get();
					default -> lavaTier3.get();
				};
				case HONEY -> switch (tier) {
					case 1 -> honeyTier1.get();
					case 2 -> honeyTier2.get();
					default -> honeyTier3.get();
				};
				case CHOCOLATE -> switch (tier) {
					case 1 -> chocolateTier1.get();
					case 2 -> chocolateTier2.get();
					default -> chocolateTier3.get();
				};
				case NONE -> 0;
			};
		}

		/** Total capacity with the given number of Capacity Upgrades installed. */
		public int tankCapacity(int capacityUpgrades) {
			int upgrades = Math.min(capacityUpgrades, maxCapacityUpgrades());
			return get(tankCapacity) + upgrades * get(capacityPerUpgrade);
		}

		public int maxUpgrades() {
			return get(maxUpgrades);
		}

		public int maxCapacityUpgrades() {
			return get(maxCapacityUpgrades);
		}

		/** An int setting, or its default before the server config has loaded. */
		public static int get(ModConfigSpec.IntValue value) {
			return SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
		}

		private static boolean get(ModConfigSpec.BooleanValue value) {
			return SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
		}

		public double crucibleStressImpact() {
			return SERVER_SPEC.isLoaded() ? crucibleStressImpact.get() : crucibleStressImpact.getDefault();
		}

		public double andesiteCobbleGenStressImpact() {
			return SERVER_SPEC.isLoaded() ? andesiteCobbleGenStressImpact.get() : andesiteCobbleGenStressImpact.getDefault();
		}

		public double brassCobbleGenStressImpact() {
			return SERVER_SPEC.isLoaded() ? brassCobbleGenStressImpact.get() : brassCobbleGenStressImpact.getDefault();
		}

		private static double get(ModConfigSpec.DoubleValue value) {
			return SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
		}

		public int magmaEngineSpeed() {
			return get(magmaEngineSpeed);
		}

		public double tanglerStressImpact() {
			return get(tanglerStressImpact);
		}

		public double infuserStressImpact() {
			return get(infuserStressImpact);
		}

		public double magmaEngineCapacity() {
			return get(magmaEngineCapacity);
		}

		public int magmaEngineLavaPerSecond() {
			return get(magmaEngineLavaPerSecond);
		}

		public int superchargedMagmaEngineSpeed() {
			return get(superchargedMagmaEngineSpeed);
		}

		public double superchargedMagmaEngineCapacity() {
			return get(superchargedMagmaEngineCapacity);
		}

		public int superchargedMagmaEngineLavaPerSecond() {
			return get(superchargedMagmaEngineLavaPerSecond);
		}

		public int motorMaxStress() {
			return get(motorMaxStress);
		}

		public double motorFePerStress() {
			return get(motorFePerStress);
		}

		public int generatorFePerTick() {
			return get(generatorFePerTick);
		}

		public int generatorLavaPerSecond() {
			return get(generatorLavaPerSecond);
		}

		public int turbineFePerTick() {
			return get(turbineFePerTick);
		}

		public int turbineLavaPerSecond() {
			return get(turbineLavaPerSecond);
		}

		public int turbineWaterPerSecond() {
			return get(turbineWaterPerSecond);
		}

		public double gyroStrength() {
			return get(gyroStrength);
		}

		public double gyroDamping() {
			return get(gyroDamping);
		}

		public double gyroIntegral() {
			return get(gyroIntegral);
		}

		public double gyroIntegralLimit() {
			return get(gyroIntegralLimit);
		}

		public double gyroReferenceRpm() {
			return get(gyroReferenceRpm);
		}

		public double gyroStressImpact() {
			return get(gyroStressImpact);
		}

		public double torqueRotorThrust() {
			return get(torqueRotorThrust);
		}

		public double torqueRotorStressImpact() {
			return get(torqueRotorStressImpact);
		}

		public boolean pipezFeedsBlazeBurners() {
			return get(pipezFeedsBlazeBurners);
		}

		public boolean blazeBurnerPipezOnly() {
			return get(blazeBurnerPipezOnly);
		}

		public boolean pipezFeedsEngines() {
			return get(pipezFeedsEngines);
		}
	}

	public static class Client {
		public final ModConfigSpec.BooleanValue renderCurioGoggles;
		public final ModConfigSpec.BooleanValue renderOverHelmet;

		Client(ModConfigSpec.Builder builder) {
			builder.comment("Aviator's Goggles worn in the Curios head slot").push("aviatorsGoggles");

			renderCurioGoggles = builder
				.comment("Render Aviator's Goggles on the player's head when worn in a Curios slot.")
				.translation("comedymonkeysaddons.configuration.showGogglesInCuriosSlot")
				.define("showGogglesInCuriosSlot", true);

			renderOverHelmet = builder
				.comment("Also render them when a helmet is worn (they are drawn slightly larger, on top of the helmet).")
				.translation("comedymonkeysaddons.configuration.showGogglesOverHelmet")
				.define("showGogglesOverHelmet", true);

			builder.pop();
		}
	}
}
