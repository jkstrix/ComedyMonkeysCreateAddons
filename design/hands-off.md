# Hands off

Finished designs. The redesign skills (`redesign-to-model`, `voxel-redesign-director`) never survey, redesign or change anything listed here. Only the user adds to or removes from this list.

## Trade Bot
- Blocks and items: `trade_bot` (the winding station), the Trade Bot entity, the packed Trade Bot item.
- Code: `content/tradebot/`, `client/TradeBotStationRenderer.java` and any other Trade Bot client classes.
- Assets: everything with `trade_bot` in its path (`assets/comedymonkeysaddons/trade_bot/`, models, textures, sounds).
- Design: `design/briefs/trade-bot*.md`, `design/models/trade-bot*`, `design/specs/trade-bot*`, `design/plans/trade-bot.md`, `design/sound/`, `design/gui/trade*`.

## Auto Harvester
- Blocks: `auto_harvester`.
- Code: `content/harvester/`, `client/AutoHarvesterRenderer.java`, `client/RealPlants.java`.
- Assets: everything with `auto_harvester` in its path.
- Design: `design/briefs/auto-harvester.md`, `design/dossiers/auto-harvester*`, `design/models/auto-harvester*`.

## Animal Pens
- Blocks: `animal_pen`, `brass_animal_pen`.
- Code: `content/pen/`, `client/AnimalPenRenderer.java`.
- Assets: everything with `animal_pen` in its path (including `brass_animal_pen_*`).
- Design: `design/briefs/animal-pen.md`, `design/dossiers/animal-pen*`, `design/models/animal-pen*`.

## Geothermal Turbine
- Blocks: `geothermal_casing`, `geothermal_core`, `geothermal_controller` (the 3x3x3 multiblock).
- Code: `content/electric/Geothermal*`, the `Turbine` renderer in `client/ElectricRenderers.java`, `client/GeothermalTurbineSounds.java`, the turbine's capabilities in `ComedyMonkeysCreateAddons.java` and its scene in `client/ponder/ElectricScenes.java`.
- Assets: everything with `geothermal` in its path, and the `geothermal_turbine.*` sounds.
- Design: `design/briefs/geothermal-turbine.md`, `design/dossiers/geothermal-turbine*`, `design/models/geothermal-turbine*`, `design/sound/geothermal-turbine/`.
- The turbine uses only its own `geothermal_turbine_*` textures.

## Lava Generator
- Blocks: `lava_generator`.
- Code: `content/electric/LavaGenerator*`, the `Generator` renderer in `client/ElectricRenderers.java`, the generator half of `client/ElectricSounds.java`, its capabilities in `ComedyMonkeysCreateAddons.java` and its scene in `client/ponder/ElectricScenes.java`.
- Assets: everything with `lava_generator` in its path, and the `lava_generator.*` sounds.
- Design: `design/briefs/lava-generator.md`, `design/dossiers/lava-generator*`, `design/sound/lava-generator/`, and the generator in `design/models/electric*`.

## Electric Motor
- Blocks: `electric_motor`.
- Code: `content/electric/ElectricMotor*`, `client/ElectricMotorScreen.java`, `network/MotorSettingsPayload.java`, the `Motor` renderer in `client/ElectricRenderers.java`, the motor half of `client/ElectricSounds.java`, its capability in `ComedyMonkeysCreateAddons.java` and its scene in `client/ponder/ElectricScenes.java`.
- Assets: everything with `electric_motor` in its path, and the `electric_motor.*` sounds.
- Design: `design/briefs/electric-motor.md`, `design/dossiers/electric-motor*`, `design/sound/electric-motor/`, and the motor in `design/models/electric*`.

Shared by the electric family: `client/ElectricRenderers.java` (its `face`, `drawModel` and `registerModels` helpers), `EnergyBuffer` and `design/sound/export-block.js`.

Shared with future blocks: the Auto Harvester and the pens use `client/TrayItems.java`, and all three of these use the generator kit `design/models/process-box.lib.js`. Those may grow, but they must keep producing the same output for these blocks: after any change, regenerate their models and compare.

## Magma Engines
- Blocks: `magma_engine`, `supercharged_magma_engine`.
- Code: `content/engine/`, `client/MagmaEngineRenderer.java`, `client/MagmaEngineSounds.java`, their capability in `ComedyMonkeysCreateAddons.java` and their scene in `client/ponder/NewBlockScenes.java`.
- Assets: everything with `magma_engine` in its path (including `supercharged_magma_engine_*`), and the `magma_engine.*` sounds.
- Design: `design/briefs/magma-engine.md`, `design/briefs/supercharged-magma-engine.md`, `design/dossiers/magma-engine*`, `design/dossiers/supercharged-magma-engine.md`, `design/models/engines*`, `design/sound/magma-engine/`.

## Pressure Crucible
- Blocks: `pressure_crucible`.
- Code: `content/crucible/PressureCrucibleBlock.java`, `content/crucible/PressureCrucibleBlockEntity.java`, `client/PressureCrucibleRenderer.java`, `client/PressureCrucibleSounds.java`.
- Assets: everything with `pressure_crucible` in its path (models, textures, blockstate), and the `pressure_crucible.*` sounds.
- Design: `design/briefs/pressure-crucible.md`, `design/dossiers/pressure-crucible*`, `design/models/pressure-crucible*`, `design/sound/pressure-crucible/`.
- Its recipes, JEI category, Ponder scene and advancements are gameplay, not design, and aren't covered here.

Shared with future blocks: `design/sound/sources/generated/` holds the local Stable Audio 3 takes (the crucible's crunch, gears and fires); add new takes beside them, never change these.

## Upgraded Chests and Barrels
- Blocks: `upgraded_chest`, `upgraded_barrel` (all five tiers; single and double chests).
- Look only: the models and textures. Assets: `models/block/upgraded_chest_*.json`, `models/block/upgraded_barrel_*.json`, `textures/block/upgraded_chest_*.png`, `textures/block/upgraded_barrel_*.png`, and `client/UpgradedChestRenderer.java` (the lid).
- Design: `design/briefs/upgraded-chest.md`, `design/briefs/upgraded-barrel.md`, `design/dossiers/upgraded-chest*`, `design/models/upgraded-chest*`.
- Their storage behaviour (`content/chest/`, menus, screens, the upgrade items) is gameplay, not design, and isn't covered here.

## Cuckoo Clock
- Blocks: `cuckoo_clock`, `cuckoo_clock_weights`.
- Code: `content/clock/`, `client/CuckooClockRenderer.java`.
- Assets: `models/block/cuckoo_clock*.json`, `textures/block/cuckoo_clock*.png`, its blockstates, item model and sounds.
