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

## Cuckoo Clock
- Blocks: `cuckoo_clock`, `cuckoo_clock_weights`.
- Code: `content/clock/`, `client/CuckooClockRenderer.java`.
- Assets: `models/block/cuckoo_clock*.json`, `textures/block/cuckoo_clock*.png`, its blockstates, item model and sounds.
