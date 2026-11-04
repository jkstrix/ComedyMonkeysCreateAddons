# Trade Master: Code Plan

Status: **M0–M3 done (2026-11-04); M4 next (the gauge planner and buying)** · Builds: [spec](../specs/trade-master.md) (40 decisions), [brief](../briefs/trade-master.md), [texture review](../texture-reviews/trade-master.md), model [trade-master.gen.js](../models/trade-master.gen.js) (a variant of `trade-bot.gen.js`) · On top of the built Trade Bot ([plan](trade-bot.md)) · Target: NeoForge 21.1 / MC 1.21.1, Create 6.0.10

`main` = `src/main/java/com/comedymonkey/createaddons/`, `res` = `src/main/resources/`, `test` = `src/test/java/com/comedymonkey/createaddons/`.

## The approach: one robot, two kinds

The Trade Master is Trade Bot with a different job (spec D16, D28, D39: same body, smarter brain, the same clips and sounds). So he is **the same classes with a kind**, not a fork:

| Piece | Shared with Trade Bot | Different for the Trade Master |
|---|---|---|
| `TradeBotEntity` | movement, hops, docking, fear, reactions, wind-down, charge, load, pouch, reputation, owner | entity type `trade_master`; a `BotKind` read from the type; frequency (his network) |
| `TradeBotBrain` | the task machine (leave, approach, work, wait, return, dock, every interrupt) | job planning: Trade Bot's link planner, or the Trade Master's **gauge planner** (`GaugePlanner`) |
| `TradeWork` | the trade clip, the stillThere checks, claims, bookkeeping | which offers and what pays: selling for emeralds (as now) or **buying** (D17, D32, P6) |
| `ContainerWork` | the take / put clips and the blind routine | the containers are **Trade Board chests** (D40) |
| Station block + BE | FE, charging, cradle, winder, comparator, pause, pack and unpack, wrench | block `trade_master`; a Create `LogisticallyLinkedBehaviour` (his frequency, so gauges and boards tune from him); the screen's contents |
| Item | `TradeBotItem` and `TradeBotData` | `trade_master` item; data adds his frequency |
| Renderer, model, clips | `TradeBotRenderer`, `TradeBotModel`, `TradeBotGeometry`, `ClipLibrary.of` | paths `trade_master/geometry.json`, `trade_master/clips.json`, `textures/entity/trade_master(_glow).png` |
| Sounds | every Trade Bot sound and cue | none (D39) |

New on their own: the **Trade Board** (block, BE, item), the **Trade Ledger** (and its incomplete item), the **Netherite Sheet**, the **gauge mixin**, the **upgrade recipe** that carries a Trade Bot's data into the Trade Master.

## Architecture in one picture

```
 trade-master.gen.js (--game) ──► res/assets/.../trade_master/*  (geometry, clips, textures; Trade Bot's cues)

 Trade Bot item ──5x5 + Trade Ledger + netherite──► Trade Master item ──places──► station (kind MASTER)
                                                                                    │ FE, cradle, frequency (a Create link)
                                                                                    ▼
                                                             TradeBotEntity (kind MASTER)
                                                               └─ TradeBotBrain ─► GaugePlanner
                                                                       │  reads: boards on his frequency, their gauges
                                                                       │  plans: fetch → (sell →) buy → deliver → tidy
                                                                       ▼
  Trade Board (on any container) ◄── Factory Gauges (FactoryPanelBehaviour mixin: count only the board's container,
                                      never ask Create's network, show his promises)
```

## Milestone 0: export pipeline

- `trade-bot.gen.js --variant=… --game` writes the variant's assets under its own slug. `trade-bot.game-export.js` takes the slug and id, and the new glowing bone (`aerial_bulb_glow`).
- Output: `res/assets/comedymonkeysaddons/trade_master/geometry.json`, `trade_master/clips.json` (with the beard, monocle, chain and bulb tracks), `textures/entity/trade_master.png` and `trade_master_glow.png`.
- Test: the exported clips match Trade Bot's on every shared track (the same check as the design review, as a data test).

## Milestone 1: he exists and moves like the preview

- `ModEntities.TRADE_MASTER` (the same `TradeBotEntity` class, a second type); `BotKind` from the type.
- The renderer, model and geometry are taken per kind; the clip library per kind (`ClipLibrary.of`).
- `/tradebot` debug commands work on him (spawn, play a clip), so every clip can be checked in game.
- Result: spawn him, play every clip; the beard swings, the monocle pops, the bulb lit cube stays off.

## Milestone 2: station, item, power, ownership, the upgrade

- Block `trade_master` (station, kind MASTER), BE with a `LogisticallyLinkedBehaviour`: the frequency is taken when he's tuned (P1: right-click a Stock Link with the item before placing) and kept in his data; a locked network admits only players allowed in.
- `TradeBotData` gains `frequency` (optional); packing and unpacking carry it.
- Per-player cap: `tradeMastersPerPlayer` (2, D29), counted separately from Trade Bots.
- Items: **Netherite Sheet** (pressing recipe, `c:plates/netherite` tag), **Trade Ledger** and its incomplete item (sequenced assembly, D27), and the **upgrade recipe**: a mechanical-crafting recipe type of our own (Create's, plus copying the packed Trade Bot's `trade_bot` data component onto the result: owner, reputation, charge, P10).
- **Art first** (brief H9, D38, "preview in Blockbench first"): the station item look, the Netherite Sheet and the Trade Ledger icons.

## Milestone 3: the Trade Board and the gauges

- **Moved here from M2:** his network frequency (a Create `LogisticallyLinkedBehaviour` on his station, tuned with the item before placing, P1; kept in `TradeBotData` when packed), since the boards and gauges are what use it.

- **Art first**: the Trade Board model (a plain flat board on any container face, H9, D41), designed and previewed before code.
- A hanging entity (D41: like an item frame, on any of a container's six faces, sharing its space with the gauge block), its item, tuned like Create's logistics items (H3), the network lock (H5), cheap recipe (H4). Goggles (H8) come with M4's station screen and goggles.
- A per-level registry of boards by frequency (rebuilt as they load; no saved data needed).
- **Gauge mixin** on `FactoryPanelBehaviour`: when a gauge stands on a Trade Board's face, `getRelevantSummary` returns the board's container and `tickRequests` does nothing (Create never orders for a board gauge). Gauges elsewhere stay Create's. `getPromised` (his promises) comes with M4, which makes them.

## Milestone 4: the gauge planner and buying

- `GaugePlanner`: the unmet board gauges in range (D19, D24, D31), the emptiest first (P4), claimed per gauge (P8); "nobody sells this" and "out of range" reasons (P3).
- Steps (spec Behaviour): fetch owned stock first (D35), pay with emeralds above any emerald gauge (D36), sell the most plentiful item when short (D22, P5), buy at no more than base price (D32), two-item trades (P6), deliver into the gauge's chest with overshoot (D30), tidy leftovers, Nuggets of Experience to a Nugget gauge (D30).
- `TradeWork` buying: offer choice by predicate, payment from his load (cost A and B), result into his load.
- The station screen: network, gauges in work, can't-fill list, charge, Call home (P2). Goggles (P11); redstone: full signal takes him off the network (P9).

## Milestone 5: life, teaching, release

- Advancement "Trade Master" (P14). Ponder: "Joining a network", "Filling gauges" (P12). JEI shows the recipes for free (Create's categories).
- Config: `tradeMastersPerPlayer`; his range, charge and drain reuse Trade Bot's (D28).
- Tests: unit (reserve maths, the base-price rule, gauge selection, leftover routing, sell choice) and game tests (a full loop: board gauge → fetch → sell → buy → deliver), each proven to fail first.
- **His own subtitles:** his clips play Trade Bot's sound events, whose subtitles say "Trade Bot …". Give him his own events on the same OGG files, as the Helper Bot did (`ModSounds`, `sounds.json`, lang).
- Lang, README, website (Codex page), release.

## Progress
- **M0 (2026-11-04):** `trade-bot.game-export.js` takes the character's id and extra glowing bones; `trade-master.gen.js --game` writes `trade_master/geometry.json`, `trade_master/clips.json` and `textures/entity/trade_master(_glow).png`. Re-exporting Trade Bot changes nothing (byte-identical). `TradeMasterClipsTest` checks the same clips in the same order, every Trade Bot track unchanged, only his own parts adding tracks, and the pop (proven to fail on a planted fault).
- **M1 (2026-11-04):** `BotKind`; `ModEntities.TRADE_MASTER` (the same `TradeBotEntity` class); `TradeBotRenderer`, `TradeBotModel` and `TradeBotGeometry` per kind; the client poses him from his own clips (`BotKind.own`), the server keeps Trade Bot's. In game (`/summon comedymonkeysaddons:trade_master`, `/tradebot clip …`): he renders with beard, monocle, netherite and violet eyes; the monocle pops on `expr_startled` (seen by the user).

## Order of work and size

| Milestone | Size | Playable result |
|---|---|---|
| 0 Export | small | his assets in the mod |
| 1 Entity + clips | small–medium | spawn him, play every clip |
| 2 Station, item, upgrade | medium (+ art) | upgrade a Trade Bot, place and pack him, he charges |
| 3 Board + gauges | medium–large (+ art) | gauges on boards count their chest |
| 4 Planner + buying | **large** | the whole loop |
| 5 Life, teaching, release | medium | shippable |

Each milestone ends with the unit and game tests, a headless check and `runClient` for you to try.

## Risks

1. **Refactoring the brain** (M4): the job planner moves behind an interface while Trade Bot keeps working. Covered by `TradeBotGameTests` before and after.
2. **The gauge mixin** (M3) targets private Create methods (`getRelevantSummary`, `tickRequests`; `getPromised` in M4); covered by `MixinConfigTest` and game tests.
3. **The upgrade recipe** (M2): Create's mechanical crafting result has no data hook; a recipe type of our own wraps it.
4. **Server load**: gauge checks only while docked, a cached board registry, 2 per player.
- **M2 (2026-11-04):** the station block `trade_master` (`TradeBotStationBlock` with a `BotKind`; the same block-entity type for both), the item `trade_master` (spawns his entity type; his own limit, `tradeMastersPerPlayer` 2, counted by kind in `TradeBotRegistry`); the station, its Flywheel visual and the item drawn from his geometry and textures (netherite plinth, violet glow). Items: Netherite Sheet (a flat icon like Create's sheets, `c:plates/netherite`, pressing), Trade Ledger and its incomplete form (3D models from `trade-master-items.gen.js` via `trade-master-items.export.js`, sequenced assembly). The upgrade: `TradeMasterUpgradeRecipe`, Create's mechanical crafting with the Trade Bot's data carried over (links dropped). Game tests `TradeMasterGameTests` (the upgrade through Create's crafter code; his station spawns him and counts him apart), proven to fail on a planted fault; all 343 game tests pass.
- **M3 (2026-11-04):** the Trade Board (the thin hatch with a flap and a bulb was dropped for a plain board: "the hatch looks silly and isn't required"; the board approved, "much cleaner"): `TradeBoardEntity` (hangs on any face of an item container, stays while a container is behind it, its network lock), `TradeBoardItem` (tuned on any Create link, cleared in the air), `TradeBoards` (loaded boards by network; the board a gauge stands on), `TradeBoardRenderer`; model and textures from `trade-board.gen.js` via `trade-board.export.js`; recipe (electron tube, andesite casing, brass sheet). `FactoryPanelBehaviourMixin`: a gauge on a board counts its chest and Create orders nothing for it. His station is a Create link (`LogisticallyLinkedBehaviour`, Trade Master only), his item tunes on any link and his station keeps the network when packed. Game tests: a gauge on a board counts its chest, Create orders nothing for it, the board stays by its gauge but only on a container, his station keeps its network; each proven to fail on a planted fault (which also found the gauge's board lookup never ran: an overflow from `Long.MIN_VALUE`). All 347 game tests pass.
