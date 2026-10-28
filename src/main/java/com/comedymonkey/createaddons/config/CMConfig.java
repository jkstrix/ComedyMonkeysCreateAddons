package com.comedymonkey.createaddons.config;

import java.util.List;

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
		public final ModConfigSpec.IntValue clockStress;
		public final ModConfigSpec.IntValue clockRadius;
		public final ModConfigSpec.IntValue penStress;
		public final ModConfigSpec.IntValue brassPenStress;
		public final ModConfigSpec.IntValue penBreedSeconds;
		public final ModConfigSpec.DoubleValue penFruitfulBonus;
		public final ModConfigSpec.IntValue penGrowSeconds;
		public final ModConfigSpec.IntValue penWoolSeconds;
		public final ModConfigSpec.IntValue penEggSeconds;
		public final ModConfigSpec.IntValue penMilkPerMinute;
		public final ModConfigSpec.IntValue andesiteCageCapacity, brassCageCapacity, netheriteCageCapacity;
		public final ModConfigSpec.IntValue andesiteCageSeconds, brassCageSeconds, witherCageSeconds;
		public final ModConfigSpec.DoubleValue andesiteCageStress, brassCageStress, netheriteCageStress;
		public final ModConfigSpec.IntValue cageChowCommon, cageChowNether, cageChowRare, cageChowWither;
		public final ModConfigSpec.DoubleValue cageMonstrousBonus;
		public final ModConfigSpec.IntValue cageWaterPerMonster, floodedCageTank, brassFloodedCageTank;
		public final ModConfigSpec.IntValue cageFieldFe, cageFarmFe, cageFeBuffer;
		public final ModConfigSpec.IntValue cageWarningSeconds, cageEscapeSeconds, cageAlarmRange, cageRestoreHoldSeconds;
		public final ModConfigSpec.BooleanValue cageEscapes;
		public final ModConfigSpec.DoubleValue cageGrumbleVolume;
		public final ModConfigSpec.IntValue fishTankCapacity, brassFishTankCapacity, fishTankBreedSeconds, fishTankWaterPerBirth, fishTankWater, brassFishTankWater;
		public final ModConfigSpec.DoubleValue fishTankStress, brassFishTankStress;
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
		public final ModConfigSpec.IntValue batteryCapacity;
		public final ModConfigSpec.IntValue batteryFePerTick;
		public final ModConfigSpec.IntValue industrialBatteryCapacity;
		public final ModConfigSpec.IntValue lightningStrikeFe;
		public final ModConfigSpec.IntValue fluidLineCopperMbPerTick;
		public final ModConfigSpec.IntValue fluidLineBrassMbPerTick;
		public final ModConfigSpec.IntValue powerLineCopperFePerTick;
		public final ModConfigSpec.IntValue powerLineBrassFePerTick;
		public final ModConfigSpec.IntValue spanReachCopper;
		public final ModConfigSpec.IntValue spanReachBrass;

		public final ModConfigSpec.DoubleValue gyroStrength;
		public final ModConfigSpec.DoubleValue gyroDamping;
		public final ModConfigSpec.DoubleValue gyroIntegral;
		public final ModConfigSpec.DoubleValue gyroIntegralLimit;
		public final ModConfigSpec.DoubleValue gyroReferenceRpm;
		public final ModConfigSpec.DoubleValue gyroStressImpact;
		public final ModConfigSpec.DoubleValue torqueRotorThrust;
		public final ModConfigSpec.DoubleValue torqueRotorStressImpact;

		public final ModConfigSpec.BooleanValue villagersSitInSeats;

		public final ModConfigSpec.IntValue tradeBotRange;
		public final ModConfigSpec.IntValue tradeBotsPerPlayer;
		public final ModConfigSpec.IntValue tradeBotChargeFePerTick;
		public final ModConfigSpec.IntValue tradeBotStationBuffer;
		public final ModConfigSpec.IntValue tradeBotFullCharge;
		public final ModConfigSpec.IntValue tradeBotFePerBlock;
		public final ModConfigSpec.IntValue tradeBotFePerTrade;
		public final ModConfigSpec.IntValue tradeBotHostileRadius;
		public final ModConfigSpec.BooleanValue helperBotShopKeeper;
		public final ModConfigSpec.DoubleValue juggernautHealth;
		public final ModConfigSpec.DoubleValue juggernautExtraPlayerHealth;
		public final ModConfigSpec.ConfigValue<List<? extends Number>> juggernautWoundDamage;
		public final ModConfigSpec.ConfigValue<List<? extends Number>> juggernautWoundSeconds;
		public final ModConfigSpec.ConfigValue<List<? extends Number>> juggernautRewindSeconds;
		public final ModConfigSpec.IntValue ritualWindSeconds;
		public final ModConfigSpec.IntValue ritualMinSpeed;
		public final ModConfigSpec.DoubleValue windingKeyStressImpact;

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

			builder.comment("Monster Cages: hold one captured monster and bring in more of it, each eating Monster Chow. Faster",
					"shafts bring them sooner (and eat chow sooner); the stress is per RPM. The Netherite cage holds Withers on FE.")
				.push("monsterCage");
			andesiteCageCapacity = builder.comment("Monsters the Andesite (and Flooded Andesite) Monster Cage holds.")
				.translation("comedymonkeysaddons.configuration.andesiteCageCapacity").defineInRange("andesiteCapacity", 64, 1, 4096);
			brassCageCapacity = builder.comment("Monsters the Brass (and Flooded Brass) Monster Cage holds.")
				.translation("comedymonkeysaddons.configuration.brassCageCapacity").defineInRange("brassCapacity", 256, 1, 4096);
			netheriteCageCapacity = builder.comment("Withers the Netherite Monster Cage holds.")
				.translation("comedymonkeysaddons.configuration.netheriteCageCapacity").defineInRange("netheriteCapacity", 3, 1, 64);
			andesiteCageSeconds = builder.comment("Seconds per new monster in the Andesite cages at 64 RPM, times the monster's chow cost.")
				.translation("comedymonkeysaddons.configuration.andesiteCageSeconds").defineInRange("andesiteSeconds", 20, 1, 36000);
			brassCageSeconds = builder.comment("Seconds per new monster in the Brass cages at 64 RPM, times the monster's chow cost.")
				.translation("comedymonkeysaddons.configuration.brassCageSeconds").defineInRange("brassSeconds", 10, 1, 36000);
			witherCageSeconds = builder.comment("Seconds per new Wither in the Netherite cage at 256 RPM.")
				.translation("comedymonkeysaddons.configuration.witherCageSeconds").defineInRange("witherSeconds", 1200, 1, 360000);
			andesiteCageStress = builder.comment("Stress per RPM of the Andesite cages while they work (at 64 RPM, 256 SU).")
				.translation("comedymonkeysaddons.configuration.andesiteCageStress").defineInRange("andesiteStressPerRpm", 4.0, 0.0, 1024.0);
			brassCageStress = builder.comment("Stress per RPM of the Brass cages while they work.")
				.translation("comedymonkeysaddons.configuration.brassCageStress").defineInRange("brassStressPerRpm", 16.0, 0.0, 1024.0);
			netheriteCageStress = builder.comment("Stress per RPM of the Netherite cage while it works.")
				.translation("comedymonkeysaddons.configuration.netheriteCageStress").defineInRange("netheriteStressPerRpm", 16.0, 0.0, 1024.0);
			cageChowCommon = builder.comment("Monster Chow for each new common Overworld monster (tag monster_cage/andesite).")
				.translation("comedymonkeysaddons.configuration.cageChowCommon").defineInRange("chowCommon", 1, 0, 64);
			cageChowNether = builder.comment("Monster Chow for each new Nether, End, ocean or pillager-type monster, and modded ones.")
				.translation("comedymonkeysaddons.configuration.cageChowNether").defineInRange("chowNether", 2, 0, 64);
			cageChowRare = builder.comment("Monster Chow for each new rare monster (tag monster_cage/rare: evoker, ravager, shulker...).")
				.translation("comedymonkeysaddons.configuration.cageChowRare").defineInRange("chowRare", 4, 0, 64);
			cageChowWither = builder.comment("Monster Chow for each new Wither.")
				.translation("comedymonkeysaddons.configuration.cageChowWither").defineInRange("chowWither", 64, 0, 512);
			cageMonstrousBonus = builder.comment("Extra speed per level of Monstrous: 0.5 makes I 1.5x, II 2x, III 2.5x as fast.")
				.translation("comedymonkeysaddons.configuration.cageMonstrousBonus").defineInRange("monstrousBonusPerLevel", 0.5, 0.0, 10.0);
			cageWaterPerMonster = builder.comment("Water each new monster in a Flooded cage uses, in millibuckets.")
				.translation("comedymonkeysaddons.configuration.cageWaterPerMonster").defineInRange("waterPerMonster", 250, 0, 16000);
			floodedCageTank = builder.comment("Water the Flooded Andesite cage's tank holds, in millibuckets.")
				.translation("comedymonkeysaddons.configuration.floodedCageTank").defineInRange("floodedTank", 4000, 1000, 1000000);
			brassFloodedCageTank = builder.comment("Water the Flooded Brass cage's tank holds, in millibuckets.")
				.translation("comedymonkeysaddons.configuration.brassFloodedCageTank").defineInRange("brassFloodedTank", 16000, 1000, 1000000);
			cageFieldFe = builder.comment("FE per tick the Netherite cage's containment field takes whenever it holds a Wither, at any speed.")
				.translation("comedymonkeysaddons.configuration.cageFieldFe").defineInRange("fieldFePerTick", 2048, 0, 1000000);
			cageFarmFe = builder.comment("Extra FE per tick while it brings in Withers, at 256 RPM (less when slower).")
				.translation("comedymonkeysaddons.configuration.cageFarmFe").defineInRange("farmingFePerTick", 1024, 0, 1000000);
			cageFeBuffer = builder.comment("FE the Netherite cage stores.")
				.translation("comedymonkeysaddons.configuration.cageFeBuffer").defineInRange("feBuffer", 200000, 1000, 100000000);
			cageWarningSeconds = builder.comment("Seconds of warning before a Wither escapes from a cage whose field is down.")
				.translation("comedymonkeysaddons.configuration.cageWarningSeconds").defineInRange("warningSeconds", 300, 10, 36000);
			cageEscapeSeconds = builder.comment("Seconds between escapes while the field stays down (a game day).")
				.translation("comedymonkeysaddons.configuration.cageEscapeSeconds").defineInRange("escapeIntervalSeconds", 1200, 60, 360000);
			cageAlarmRange = builder.comment("Blocks a player must be within for the countdown to run and to get the warnings.")
				.translation("comedymonkeysaddons.configuration.cageAlarmRange").defineInRange("alarmRange", 128, 8, 1024);
			cageRestoreHoldSeconds = builder.comment("Seconds a restored field must hold before it calls off a countdown. Until then the countdown only stands still, and carries on if the field drops again.")
				.translation("comedymonkeysaddons.configuration.cageRestoreHoldSeconds").defineInRange("restoreHoldSeconds", 60, 0, 3600);
			cageEscapes = builder.comment("Whether Withers escape when the field is down. Off: the cage just stops.")
				.translation("comedymonkeysaddons.configuration.cageEscapes").define("escapes", true);
			cageGrumbleVolume = builder.comment("Volume of the caged monsters' own sounds, which they make now and then.")
				.translation("comedymonkeysaddons.configuration.cageGrumbleVolume").defineInRange("grumbleVolume", 0.35, 0.0, 1.0);
			builder.pop();

			builder.comment("Fish Tanks: breed one kind of fish, squid or axolotl, as the Animal Pens breed animals. Faster shafts",
					"breed faster (the times are at 64 RPM); each birth eats a Fish Food and uses water. Fruitful works on them.")
				.push("fishTank");
			fishTankCapacity = builder.comment("Creatures the Fish Tank holds.")
				.translation("comedymonkeysaddons.configuration.fishTankCapacity").defineInRange("capacity", 128, 1, 4096);
			brassFishTankCapacity = builder.comment("Creatures the Brass Fish Tank holds.")
				.translation("comedymonkeysaddons.configuration.brassFishTankCapacity").defineInRange("brassCapacity", 512, 1, 4096);
			fishTankBreedSeconds = builder.comment("Seconds between breedings at 64 RPM. (Axolotl young grow up in the Animal Pens' time.)")
				.translation("comedymonkeysaddons.configuration.fishTankBreedSeconds").defineInRange("secondsPerBreeding", 300, 1, 36000);
			fishTankWaterPerBirth = builder.comment("Water each birth uses, in millibuckets.")
				.translation("comedymonkeysaddons.configuration.fishTankWaterPerBirth").defineInRange("waterPerBirth", 250, 0, 16000);
			fishTankWater = builder.comment("Water the Fish Tank holds, in millibuckets.")
				.translation("comedymonkeysaddons.configuration.fishTankWater").defineInRange("tank", 4000, 1000, 1000000);
			brassFishTankWater = builder.comment("Water the Brass Fish Tank holds, in millibuckets.")
				.translation("comedymonkeysaddons.configuration.brassFishTankWater").defineInRange("brassTank", 16000, 1000, 1000000);
			fishTankStress = builder.comment("Stress per RPM of the Fish Tank while it works.")
				.translation("comedymonkeysaddons.configuration.fishTankStress").defineInRange("stressPerRpm", 4.0, 0.0, 1024.0);
			brassFishTankStress = builder.comment("Stress per RPM of the Brass Fish Tank while it works.")
				.translation("comedymonkeysaddons.configuration.brassFishTankStress").defineInRange("brassStressPerRpm", 16.0, 0.0, 1024.0);
			builder.pop();

			builder.comment("Cuckoo Clock: while it turns, villagers near it restock at 8:00 and 12:00 (game time), and it chimes",
					"and calls the hour. It uses the same stress at any speed.")
				.push("cuckooClock");
			clockStress = builder
				.comment("Stress it puts on the network, in SU, whatever the speed. 1024 needs a windmill of about 16 sails.")
				.translation("comedymonkeysaddons.configuration.clockStress")
				.defineInRange("stress", 1024, 0, 65536);
			clockRadius = builder
				.comment("How far from the clock, in blocks, villagers follow its restock times: measured across the ground, and",
					"as far again up and down. Big, so a clock on one wall reaches across a whole trading hall.")
				.translation("comedymonkeysaddons.configuration.clockRadius")
				.defineInRange("radius", 64, 1, 256);
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
			batteryCapacity = builder
				.comment("FE a Battery holds.")
				.translation("comedymonkeysaddons.configuration.batteryCapacity")
				.defineInRange("batteryCapacity", 10000000, 1000, 25000000);
			batteryFePerTick = builder
				.comment("FE a Battery takes or gives per tick; touching Batteries share one store, and move this much each.")
				.translation("comedymonkeysaddons.configuration.batteryFePerTick")
				.defineInRange("batteryFePerTick", 2048, 1, 1000000);
			industrialBatteryCapacity = builder
				.comment("FE each block of an Industrial Battery holds (a 3x3 tower, 3 to 9 high). It moves as much as its cables carry.",
					"At most 25,000,000, so a 9-high tower fits in what Forge Energy can count.")
				.translation("comedymonkeysaddons.configuration.industrialBatteryCapacity")
				.defineInRange("industrialBatteryCapacity", 20000000, 1000, 25000000);
			lightningStrikeFe = builder
				.comment("FE a lightning strike adds to a Battery or Industrial Battery with a Lightning Rod on top.")
				.translation("comedymonkeysaddons.configuration.lightningStrikeFe")
				.defineInRange("lightningStrikeFe", 250000, 0, 100000000);
			builder.pop();

			builder.comment("Fluid Lines: pump-free pipes. Fluid only moves from an Intake, shared by every Intake on a line.").push("fluidLines");
			fluidLineCopperMbPerTick = builder
				.comment("Fluid a Copper Fluid Line moves, in millibuckets per tick.")
				.translation("comedymonkeysaddons.configuration.fluidLineCopperMbPerTick")
				.defineInRange("copperMbPerTick", 100, 1, 100000);
			fluidLineBrassMbPerTick = builder
				.comment("Fluid a Brass Fluid Line moves, in millibuckets per tick.")
				.translation("comedymonkeysaddons.configuration.fluidLineBrassMbPerTick")
				.defineInRange("brassMbPerTick", 1000, 1, 100000);
			builder.pop();

			builder.comment("Power Lines: wires and hanging spans for FE. Power flows from anything that gives it to anything that takes it.").push("powerLines");
			powerLineCopperFePerTick = builder
				.comment("FE a Copper Power Line moves per tick.")
				.translation("comedymonkeysaddons.configuration.powerLineCopperFePerTick")
				.defineInRange("copperFePerTick", 4096, 1, 10000000);
			powerLineBrassFePerTick = builder
				.comment("FE a Brass Power Line moves per tick.")
				.translation("comedymonkeysaddons.configuration.powerLineBrassFePerTick")
				.defineInRange("brassFePerTick", 65536, 1, 10000000);
			spanReachCopper = builder
				.comment("The longest span of Copper Power Line between two Line Posts, in blocks.")
				.translation("comedymonkeysaddons.configuration.spanReachCopper")
				.defineInRange("spanReachCopper", 16, 2, 64);
			spanReachBrass = builder
				.comment("The longest span of Brass Power Line between two Line Posts, in blocks.")
				.translation("comedymonkeysaddons.configuration.spanReachBrass")
				.defineInRange("spanReachBrass", 32, 2, 64);
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

			builder.comment("Create Seats").push("seats");
			villagersSitInSeats = builder
				.comment("Villagers and wandering traders on a Create Seat sit down on it like a player does, legs bent, instead",
					"of standing on top of the cushion. Also on seats that are part of a moving contraption.")
				.translation("comedymonkeysaddons.configuration.villagersSitInSeats")
				.define("villagersSitInSeats", true);
			builder.pop();

			builder.comment("Trade Bot: the clockwork robot who sells your goods to seated villagers").push("tradeBot");
			tradeBotRange = builder
				.comment("How far from his station (in blocks) Trade Bot will go for containers and buyers.")
				.translation("comedymonkeysaddons.configuration.tradeBotRange")
				.defineInRange("tradeBotRange", 64, 8, 256);
			tradeBotsPerPlayer = builder
				.comment("How many Trade Bots each player can have placed at once.")
				.translation("comedymonkeysaddons.configuration.tradeBotsPerPlayer")
				.defineInRange("tradeBotsPerPlayer", 4, 1, 64);
			tradeBotChargeFePerTick = builder
				.comment("FE a tick his station puts into him while he's docked.")
				.translation("comedymonkeysaddons.configuration.tradeBotChargeFePerTick")
				.defineInRange("tradeBotChargeFePerTick", 64, 1, 100000);
			tradeBotStationBuffer = builder
				.comment("FE his station can store, fed from underneath.")
				.translation("comedymonkeysaddons.configuration.tradeBotStationBuffer")
				.defineInRange("tradeBotStationBuffer", 100000, 1000, 10000000);
			tradeBotFullCharge = builder
				.comment("FE he holds when fully charged. At the default 4 FE a block that's 25,000 blocks of rolling.")
				.translation("comedymonkeysaddons.configuration.tradeBotFullCharge")
				.defineInRange("tradeBotFullCharge", 100000, 1000, 10000000);
			tradeBotFePerBlock = builder
				.comment("FE he uses for every block he rolls.")
				.translation("comedymonkeysaddons.configuration.tradeBotFePerBlock")
				.defineInRange("tradeBotFePerBlock", 4, 0, 10000);
			tradeBotFePerTrade = builder
				.comment("FE he uses for every trade.")
				.translation("comedymonkeysaddons.configuration.tradeBotFePerTrade")
				.defineInRange("tradeBotFePerTrade", 50, 0, 100000);
			tradeBotHostileRadius = builder
				.comment("How close (in blocks) a monster he can see has to be before he gets scared and hides.")
				.translation("comedymonkeysaddons.configuration.tradeBotHostileRadius")
				.defineInRange("tradeBotHostileRadius", 12, 2, 32);
			builder.pop();

			builder.comment("Helper Bot: the robot who drives trains and keeps shop, like a Blaze Burner").push("helperBot");
			helperBotShopKeeper = builder
				.comment("Helper Bot can keep shop beside a Stock Ticker, as a Blaze Burner can. Turn off if a Create update breaks it.")
				.translation("comedymonkeysaddons.configuration.helperBotShopKeeper")
				.define("helperBotShopKeeper", true);
			builder.pop();

			builder.comment("Clockwork Juggernaut: the boss. Armoured while wound; he takes full damage only while he stops to",
					"rewind. Each stage (under 67% and 33% health) sheds plates and tightens his rhythm.").push("clockworkJuggernaut");
			juggernautHealth = builder.comment("His health with one player near when he wakes.")
				.translation("comedymonkeysaddons.configuration.juggernautHealth").defineInRange("health", 1000.0, 1.0, 1024.0);
			juggernautExtraPlayerHealth = builder.comment("Extra health for each other player within 32 blocks when he wakes (0.5 = +50%).")
				.translation("comedymonkeysaddons.configuration.juggernautExtraPlayerHealth").defineInRange("extraPlayerHealth", 0.5, 0.0, 10.0);
			juggernautWoundDamage = builder.comment("Share of damage he takes while wound, in stages 1, 2 and 3 (rewinding he takes it all).")
				.translation("comedymonkeysaddons.configuration.juggernautWoundDamage")
				.defineList("woundDamageTaken", List.of(0.10, 0.15, 0.20), () -> 0.1, o -> o instanceof Number n && n.doubleValue() >= 0 && n.doubleValue() <= 1);
			juggernautWoundSeconds = builder.comment("Seconds he stays wound in stages 1, 2 and 3, the last 4 of them running down.")
				.translation("comedymonkeysaddons.configuration.juggernautWoundSeconds")
				.defineList("woundSeconds", List.of(38, 32, 26), () -> 32, o -> o instanceof Number n && n.intValue() >= 6 && n.intValue() <= 600);
			juggernautRewindSeconds = builder.comment("Seconds he stands still rewinding, open to attack, in stages 1, 2 and 3.")
				.translation("comedymonkeysaddons.configuration.juggernautRewindSeconds")
				.defineList("rewindSeconds", List.of(6, 5, 4), () -> 5, o -> o instanceof Number n && n.intValue() >= 1 && n.intValue() <= 600);
			ritualWindSeconds = builder.comment("Seconds a Winding Key takes to wind at the slowest speed that winds it (a Hand Crank's); twice that speed or more", "winds it twice as fast. Stopped, it unwinds at half the slow pace.")
				.translation("comedymonkeysaddons.configuration.ritualWindSeconds").defineInRange("ritualWindSeconds", 20, 1, 600);
			ritualMinSpeed = builder.comment("The slowest a shaft can turn a Winding Key and still wind it, in RPM (a Hand Crank turns at 32).")
				.translation("comedymonkeysaddons.configuration.ritualMinSpeed").defineInRange("ritualMinSpeed", 32, 1, 256);
			windingKeyStressImpact = builder.comment("Stress a Winding Key puts on its network, per RPM. At a Hand Crank's 32 RPM the default of 4 needs 128 of its 256 stress units.")
				.translation("comedymonkeysaddons.configuration.windingKeyStressImpact").defineInRange("windingKeyStressImpact", 4.0, 0.0, 1024.0);
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

		/** A boolean setting, or its default before the server config has loaded. */
		public static boolean get(ModConfigSpec.BooleanValue value) {
			return SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
		}

		public double windingKeyStressImpact() {
			return SERVER_SPEC.isLoaded() ? windingKeyStressImpact.get() : windingKeyStressImpact.getDefault();
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

		/** A decimal setting, or its default before the server config has loaded. */
		public static double get(ModConfigSpec.DoubleValue value) {
			return SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
		}

		/** Entry {@code stage - 1} of a per-stage list setting (stages 1-3), or its default before the config loads. */
		public static double perStage(ModConfigSpec.ConfigValue<List<? extends Number>> value, int stage) {
			List<? extends Number> list = SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
			if (list.isEmpty())
				list = value.getDefault();
			return list.get(Math.min(Math.max(stage, 1), list.size()) - 1).doubleValue();
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

		public int batteryCapacity() {
			return get(batteryCapacity);
		}

		public int batteryFePerTick() {
			return get(batteryFePerTick);
		}

		public int industrialBatteryCapacity() {
			return get(industrialBatteryCapacity);
		}

		public int lightningStrikeFe() {
			return get(lightningStrikeFe);
		}

		public int fluidLineCopperMbPerTick() {
			return get(fluidLineCopperMbPerTick);
		}

		public int fluidLineBrassMbPerTick() {
			return get(fluidLineBrassMbPerTick);
		}

		public int powerLineCopperFePerTick() {
			return get(powerLineCopperFePerTick);
		}

		public int powerLineBrassFePerTick() {
			return get(powerLineBrassFePerTick);
		}

		public int spanReachCopper() {
			return get(spanReachCopper);
		}

		public int spanReachBrass() {
			return get(spanReachBrass);
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

		public boolean villagersSitInSeats() {
			return get(villagersSitInSeats);
		}

		public int tradeBotRange() {
			return get(tradeBotRange);
		}

		public int tradeBotsPerPlayer() {
			return get(tradeBotsPerPlayer);
		}

		public int tradeBotChargeFePerTick() {
			return get(tradeBotChargeFePerTick);
		}

		public int tradeBotStationBuffer() {
			return get(tradeBotStationBuffer);
		}

		public int tradeBotFullCharge() {
			return get(tradeBotFullCharge);
		}

		public int tradeBotFePerBlock() {
			return get(tradeBotFePerBlock);
		}

		public int tradeBotFePerTrade() {
			return get(tradeBotFePerTrade);
		}

		public int tradeBotHostileRadius() {
			return get(tradeBotHostileRadius);
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
		public final ModConfigSpec.IntValue helperBotVoiceCooldown;

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

			builder.comment("Helper Bot: the robot who drives trains and keeps shop").push("helperBot");
			helperBotVoiceCooldown = builder
				.comment("Seconds each Helper Bot waits between voice sounds (chirps, grumbles, sighs). Trains are loud enough.")
				.translation("comedymonkeysaddons.configuration.helperBotVoiceCooldown")
				.defineInRange("helperBotVoiceCooldown", 20, 0, 600);
			builder.pop();
		}
	}
}
