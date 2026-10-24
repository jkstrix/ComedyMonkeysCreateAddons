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

The Auto Harvester and the pens share `client/TrayItems.java` and the generator kit `design/models/process-box.lib.js` with future blocks in their family. Those may grow, but they must keep producing the same output for these two: after any change, regenerate both models and compare.

## Cuckoo Clock
- Blocks: `cuckoo_clock`, `cuckoo_clock_weights`.
- Code: `content/clock/`, `client/CuckooClockRenderer.java`.
- Assets: `models/block/cuckoo_clock*.json`, `textures/block/cuckoo_clock*.png`, its blockstates, item model and sounds.
