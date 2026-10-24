# Trade Bot: Code Plan (v1)

Status: **ready to start (M0)** · Builds: [spec](../specs/trade-bot.md) (59 decisions), [brief](../briefs/trade-bot.md), [sounds](../sound/trade-bot.md), model [trade-bot.gen.js](../models/trade-bot.gen.js) · Target: NeoForge 21.1 / MC 1.21.1, Create 6.0.10

`main` = `src/main/java/com/comedymonkey/createaddons/`, `res` = `src/main/resources/`, `test` = `src/test/java/com/comedymonkey/createaddons/`.

## What the mod already gives us, and what's new

Reused as is:

| Need | Existing piece |
|---|---|
| "Is this villager seated?" | `content/VillagerSeats.sitting(entity)`: world seats and contraption seats, wandering traders included |
| Villager restocks | Cuckoo Clock via `mixin/VillagerMixin` (`shouldRestock`); nothing to change |
| FE buffer | `content/electric/EnergyBuffer` (`generate`, `consume`, `setEnergy`, `pushTo`) |
| Registration | one `DeferredRegister` class per registry in `registry/`, wired explicitly in the `ComedyMonkeysCreateAddons` constructor (no `@EventBusSubscriber`) |
| Block entity base | Create `SmartBlockEntity` + `IBE`, `write/read(tag, registries, clientPacket)`, `sendData()` |
| Goggles, wrench | `IHaveGoggleInformation` + `LangBuilder`; `IWrenchable`, `content/Wrenches.isWrench` |
| Screens | `client/GuiKit` + `content/MenuLayout` (material band, dark body); Create `AbstractSimiScreen` for button-only screens (`ElectricMotorScreen`) |
| Network | record payloads in `network/`, registered in `registerPayloads`, server-side distance and permission checks |
| Config | `config/CMConfig` server spec, getters with default fallback, translation keys, `CMConfigTest` |
| Per-entity data on vanilla mobs | NeoForge attachments (`registry/ModAttachments`) |
| Ponder | `client/ponder/CMPonderPlugin`, scene classes, `res/assets/.../ponder/*.nbt` |
| Glow | `LightTexture.FULL_BRIGHT` on glow parts |

First of their kind in this mod (no convention yet; this plan sets it):
- an **entity type** (`ModEntities`), entity renderer, `LayerDefinition` model and synced entity data;
- **custom sounds** (`ModSounds`, `sounds.json`, subtitles, tickable loop sounds);
- a **custom data component** (`ModDataComponents`) for the combined item;
- **owner checks**, a **per-player limit** (`SavedData`) and **block-to-block links**;
- an **override of sneak-wrench pickup**.

## Architecture in one picture

```
 design/models/trade-bot.gen.js ──(--game)──► res/assets/.../trade_bot/*  (geometry, clips, textures)
 design/sound/real/*.ogg + cues.json ──────► res/assets/.../sounds/trade_bot/*, sounds.json, subtitles

 TradeBotItem ──places──► TradeBotStationBlock + BE ◄──FE from below
                                 │  links (4 inputs, 1 output), buffer, comparator, pause
                                 │  owns 1 bot (UUID)
                                 ▼
                         TradeBotEntity (PathfinderMob)
                           ├─ TradeBotBrain: state machine (server)
                           ├─ Motion: navigation + scripted moves (hops, dock) + turn clips
                           ├─ Load: 27 slots + emerald pouch + charge + reputation
                           └─ synced: clip id, clip start tick, flags (lights, held package)
                                 ▼ (client)
                         TradeBotRenderer ─ ClipPlayer (samples clip data, blends, root modes)
                                          ─ CuePlayer (one-shot sounds) + loop SoundInstances
```

The server only chooses *which clip and when*. All posing happens on the client from the same clip data the Blockbench preview uses, so the game looks exactly like the approved preview.

---

## Milestone 0: export pipeline (design → game assets)

The 42 clips hold about 58,000 baked keyframes, far too many to hand-write or compile into Java. The generator gets a game export mode that writes data files the client loads at startup.

**`node design/models/trade-bot.gen.js --game`** writes:
1. **`res/assets/comedymonkeysaddons/trade_bot/geometry.json`**: two bone trees, the bot (`trade_bot`) and the station (`station`), without the preview props. Each bone has its pivot, rotation and cubes (from, to, uv), all in Blockbench units. Cubes with their own rotation become small bones of their own, since `ModelPart` cubes can't rotate.
2. *(dropped)* The station is drawn entirely by its block-entity renderer from the same geometry file, rather than as a block model plus moving parts. It then shares the bot's model conversion, with no second set of UV rules for block-model faces. Its block model only names the particle texture.
3. *(dropped, see 2)*
4. **`res/assets/comedymonkeysaddons/trade_bot/clips.json`**: every clip, with:
   - `length`;
   - `mode` (`loop` / `once` / `hold`);
   - `fps`, either 20 or 30 (loops are linear at 30);
   - `root` (see root modes below);
   - per bone and channel: a flat float array of samples;
   - `steps`: on/off visibility tracks (glows, the held package);
   - `cues`: `[time, event]`.
   
   Prop bones are left out, and so are props' visibility steps.
5. **Textures** `res/assets/comedymonkeysaddons/textures/entity/trade_bot.png` and `trade_bot_glow.png` (glowing pixels only, drawn full bright): the bot and the station re-packed together into one 128×128 texture, without the props.

**Done (M0):** `node design/models/trade-bot.gen.js --game` (35 bones, 69 cubes, 42 clips in 651 KB) and `node design/sound/export-game.js` (80 events, 207 files). In code: `content/tradebot/clip/` (`ClipLibrary`, `Clip`, `Track`, `StepTrack`, read from the jar by both sides) and `registry/ModSounds` (registered from our sounds.json). Tests: `ClipLibraryTest` (every clip, root modes, bones exist, loops wrap, locomotion seams) and `ModSoundsTest` (every cue has a sound, every sound has its files and a subtitle). The seam test found a 0.03 px eyelid step at the end of `start_rolling`; fixed in the generator.

**Root modes** (per clip, set in the generator):

| Mode | Clips | What happens in game |
|---|---|---|
| `physical` | `hop_on/off_slab`, `hop_on/off_block`, `stairs_up/down`, `dock_lights_out`, `action_comes_in` | The `trade_bot` bone's travel and yaw are extracted into a root curve. The server moves the entity along it tick by tick, and the client draws the root at zero (entity interpolation carries it). He really ends up on the block. |
| `game` | `rolling`, `start_rolling`, `stop_rolling`, `turn_left`, `turn_right`, `turn_around` | The root is ignored except the turn's **yaw progress curve** and the 180's **hop height**: the server uses those to ease the entity's own yaw, and adds the hop to the drawn offset. |
| `visual` | everything else (`wall_bump` recoil, `sees_player`, `trade_villager` spin, expressions) | The root channel stays in the pose: small, decorative moves that don't change where he is. |

**Sounds:** `node design/sound/export-game.js` (new):
- copies `real/*.ogg` to `res/assets/comedymonkeysaddons/sounds/trade_bot/` (83 events, 277 files, 2.9 MB);
- writes `res/assets/comedymonkeysaddons/sounds.json` (event `trade_bot.<id>` with all variants, `subtitle: subtitles.comedymonkeysaddons.trade_bot.<id>`);
- merges the subtitle lines into `en_us.json`.

The `villager_*` stand-ins are skipped, because the game uses vanilla `VILLAGER_YES/NO/TRADE`.

**Coordinate conventions:** Blockbench is Y-up and Minecraft's `ModelPart` space is Y-down and mirrored. These conversions are done in one place, in `TradeBotGeometry` and `ClipData`:
- pivot `y → 24 − y`;
- cube `y` flipped;
- signs on `xRot`/`yRot`/position channels.

They are **calibrated against Blockbench's own "Export Java Entity (Mojang mappings)"** for a 3-bone test model and pinned with unit tests, not guessed.

**Tests** (`test/content/tradebot/ClipDataTest`, `SoundsJsonTest`):
- every clip parses; lengths and modes match the brief's clip index;
- every channel's bone exists in the geometry;
- loop clips' first frame equals their last;
- the locomotion seams are exact (`start_rolling` end = `rolling` start = each turn's start and end);
- every cue event exists in `sounds.json` and has a subtitle in `en_us.json`;
- every `sounds.json` file exists.

## Milestone 1: he exists and moves like the preview

**Registry (new classes):**
- `registry/ModEntities`: `DeferredRegister<EntityType<?>>`, `TRADE_BOT` (`MobCategory.MISC`, size 0.75 × 1.5, client tracking range 10, update interval 2). `EntityAttributeCreationEvent`: `MAX_HEALTH 20`, `MOVEMENT_SPEED`, `STEP_HEIGHT 0.25` (he rolls over 4 px; anything taller is a hop), `KNOCKBACK_RESISTANCE 0.6`.
- `registry/ModSounds`: one `DeferredHolder<SoundEvent>` per event, created by looping over a generated `TradeBotSounds` id list. It's generated by `export-game.js` so the ids can't drift from `sounds.json`.
- Both are wired in the main constructor after `ModItems`.

**`content/tradebot/TradeBotEntity extends PathfinderMob`:**
- **Synced data:** `CLIP` (int), `CLIP_START` (int game tick), `FLAGS` (byte: lights on, docked, package in claw, wet, wound down), `CHARGE_BAND` (byte 0–10 for visuals and goggles).
- **Rules:**
  - `requiresCustomPersistence` returns true, so he never despawns.
  - `canBeLeashed` returns false.
  - He's only pushable when not docked and not in a scripted move.
  - `isInvulnerableTo` returns true for drowning and suffocation spam.
  - He's never removed by death (milestone 6).
- `playClip(Clip, boolean restartIfSame)`: the only way server code animates him. It sets `CLIP` and `CLIP_START`.

**Client (`client/tradebot/`):**
- `TradeBotGeometry`: builds a `LayerDefinition` from `geometry.json`. It's registered in `ComedyMonkeysCreateAddonsClient.registerLayers` as `ModelLayerLocation(asResource("trade_bot"), "main")`.
- `TradeBotModel extends HierarchicalModel<TradeBotEntity>`. `setupAnim` calls `ClipPlayer.apply(...)`.
- `ClipLibrary`: a client reload listener (`RegisterClientReloadListenersEvent`) that loads `clips.json` into `Clip` objects (float arrays plus a sampler).
- `ClipPlayer`:
  - samples the current clip at `(gameTime − CLIP_START + partialTick) / 20` s;
  - writes rotations, offsets and scales onto `ModelPart`s (`xRot…`, `x…`, `xScale…`) after `resetPose()`;
  - applies the root mode;
  - **cross-fades 0.15 s** whenever `CLIP` changes (per channel lerp from the last sampled pose). The designed seams need no fade, and it's harmless there.
  - Client-side per-entity state (previous clip, fade start, last cue time) lives in a small `ClientState` object on the entity, so it never syncs.
- **Procedural overrides** in `game` mode:
  - the **wheel** angle comes from the distance actually travelled (radius 4 px), so it never slides;
  - the **wind-up key** turns at 60°/s while he has charge and slows to a stop as charge runs out.
- `TradeBotRenderer extends MobRenderer` (shadow 0.35) with:
  - `TradeBotGlowLayer`: `RenderType.eyes(trade_bot_glow.png)`, drawn only while `lights on`, and scaled by the glow bones' step tracks, so lights pass to the station exactly as in the preview;
  - the package in his claw, drawn by the model's `held_item` bone, as designed.
- **Dev command `/tradebot clip <name>`** (ops only; registered in `RegisterCommandsEvent`): plays any clip on the nearest bot. It's for comparing with Blockbench.

**Done (M1):** `registry/ModEntities`, `content/tradebot/TradeBotEntity` (synced clip, start tick and physical-clip anchor), `TradeBotCommands`, `clip/ClipRoot` (root travel and turns, shared by server and client), `clip/ClipPlayback` (fade and cues), and `client/tradebot/TradeBotGeometry`, `TradeBotModel`, `TradeBotRenderer` (with the glow layer). No spawn egg: `/summon comedymonkeysaddons:trade_bot` does the job. Checked in game by the user ("he looked awesome"); `ClipRootTest` pins the hop distances and turn directions.

**Acceptance:**
- Summon him (`/summon comedymonkeysaddons:trade_bot`).
- `/tradebot clip` each of the 42 clips.
- Put three poses side by side with Blockbench screenshots: `turn_left` at 1.3 s, `chest_take` at the reach, `scared_cower`.
- Launch the client for review (standing rule: build, headless check, `runClient`).

## Milestone 2: station, item, power, ownership

**Blocks and items:**
- `content/tradebot/TradeBotStationBlock` (implements `IBE`, `IWrenchable`):
  - horizontal facing;
  - `destroyTime −1` and `explosionResistance 3,600,000` (D51);
  - `noOcclusion`, `lightLevel` from a `GLOW` state (0 or 7, muted);
  - `hasAnalogOutputSignal`.
- The registry id is **`trade_bot`** for the block and the item. One id keeps `ModBlocksTest.everyBlockHasItsItem` and Carry On happy, and the item *is* "Trade Bot on his station" (D27).
- `TradeBotItem extends BlockItem`:
  - Placing it places the station facing the player and spawns the bot docked on it.
  - Both are restored from the `TRADE_BOT_DATA` component.
  - Placing is refused, with an action-bar message, past the per-player limit.
- `registry/ModDataComponents` (new): `TRADE_BOT_DATA`, a record with:
  - owner UUID;
  - charge;
  - station links (relative positions + dimension);
  - reputation map;
  - emerald pouch count and banked trade XP;
  - load (27 stacks, normally empty);
  - optional custom name.
  
  It has a `Codec` and `StreamCodec`, plus a round-trip unit test.

**`TradeBotStationBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation`:**
- **Energy:**
  - `EnergyBuffer(stationBuffer, chargeFePerTick·4, 0)`, exposed **only on `Direction.DOWN`** in `registerCapabilities` (D16);
  - moves `chargeFePerTick` (64) into the docked bot each tick (D21);
  - sync is throttled like the motor's (every 10 ticks, on a >1% change).
- **Links:** `List<BlockPos> inputs` (max 4), `BlockPos output`. Validated on use: the block still exposes `Capabilities.ItemHandler.BLOCK` or is a Create belt or depot, and is within range. A dead link is shown red in the screen and skipped (D33).
- **Bot:** `UUID bot`, plus the last known state for goggles and comparator (task, trips left, trades today). Before reaching for the entity, it checks `level.isLoaded(botPos)`.
- **Comparator** (D38, D58): 0 with no bot, 5 docked/idle, 10 working, 15 out of charge.
- **Redstone pause:** `hasNeighborSignal`, so the bot is recalled and stays docked.
- **Goggles** (D39), per line:
  - charge: station FE / buffer and bot % with an estimate of trips left;
  - task ("Charging", "Trading with Toolsmith", "Waiting: output full"…);
  - links (4/4 inputs, output set);
  - trades today.

**Renderer:** `client/tradebot/TradeBotStationRenderer` (`SafeBlockEntityRenderer`) bakes the `station` tree of `geometry.json` the same way as the bot.
- While the bot is docking or leaving, it samples the station bones from **the bot's current clip** (`dock_lights_out`, `action_comes_in`), so the winder and flag stay in step.
- While he's docked and charging, the winder head turns procedurally, and the glow ring is drawn `FULL_BRIGHT`, matching the muted amber of his irises.

**Wrench and ownership (D23, D27, D30):**
- `content/tradebot/TradeBotOwnership`: `canControl(player, ownerUuid)` returns true for the owner, the owner's vanilla scoreboard team, or creative ops. Team-mod support (FTB Teams and similar) is left for later.
- **Wrench on the bot** (`mobInteract`, owner only): he goes home (`RETURN`).
- **Sneak-wrench on the station** (`onSneakWrenched`, overriding Create's default pickup):
  - not the owner: refused with a message;
  - bot away: recalled, with "Trade Bot is heading home"; the player wrenches again once he's docked;
  - bot docked and loaded: the bot is removed (`RemovalReason.DISCARDED`), then the station, and **one** `trade_bot` item with `TRADE_BOT_DATA` goes into the player's inventory, or drops.
- **Per-player limit:** `content/tradebot/TradeBotRegistry extends SavedData` (overworld data storage). It maps owner → bot UUIDs, with each bot's station position and dimension.
  - It counts against `botsPerPlayer` (4).
  - It also records bots removed by commands, so a station whose bot is gone for good can still be packed, and nothing can be duplicated.
- **Carry On:** add `comedymonkeysaddons:trade_bot` to `res/data/carryon/tags/block/block_blacklist.json`. Create `res/data/carryon/tags/entity_type/entity_blacklist.json` with the bot. Carrying either half would split the pair.

**Done (M2):** `TradeBotStationBlock` (block id `trade_bot`) and `TradeBotStationBlockEntity` (FE from below only, charging, links storage, pause, comparator, goggles, packing), `TradeBotItem` (places station + docked bot, per-player limit), `TradeBotData` + `ModDataComponents`, `TradeBotRegistry` (SavedData), `TradeBotOwnership`, the station renderer (whole station from `geometry.json`, posed from the bot's clip, winder turning while charging) and the item renderer. The item looks like the station with him folded up on it, in 3D everywhere (the `packed` clip; design/briefs/trade-bot-package.md, approved). Until milestone 3, "go home" puts him straight back in his cradle.

**Tests:**
- BE energy (receives only from below; charges the docked bot at 64 FE/t);
- save/load round trip;
- comparator values;
- owner checks;
- the item component codec;
- the per-player limit;
- `ModBlockEntitiesTest` picks the new BE up automatically.

## Milestone 3: getting around

**Pathfinding (`content/tradebot/move/`):**
- `TradeBotNavigation extends GroundPathNavigation` with `TradeBotNodeEvaluator extends WalkNodeEvaluator`:
  - **forbids** ladders, scaffolding, vines, bubble columns, water and lava (malus −1) (D18, D29);
  - **allows** steps up to 1 block (as jump nodes) and drops of up to 1 block;
  - **doors:** `canOpenDoors` and `canPassDoors`. Closed fence gates and trapdoors that block the way are treated as wooden doors (D28).
  - Only loaded chunks are searched; a target in an unloaded chunk is skipped this trip (D55).
- `DoorHandler`: opens a door, gate or trapdoor ahead and closes it behind him, with vanilla sounds. Iron doors stay shut, as for villagers.
- **Range:** every target must be within `range` (64) of the station, and the station's own chunk must be loaded for him to leave. If it unloads while he's out, he parks in place until it loads again.

**`TradeBotMotion`** runs the path:

| Path situation | What plays | How the entity moves |
|---|---|---|
| Setting off / stopping | `start_rolling` → `rolling` loop / `stop_rolling` | Speed ramps over the clip; the rolling loop's rate follows speed |
| Corner of 60–120° | `turn_left` / `turn_right`, time-scaled to the turn's real length | Yaw eased by the clip's yaw curve |
| Reversal (>150°) | `turn_around` | Yaw follows the clip's curve and the hop is visual; the entity brakes and re-accelerates as in the clip |
| Rise ≤ 4 px | none (step) | Normal stepping (`STEP_HEIGHT 0.25`) |
| Stairs step | `stairs_up` / `stairs_down` | Scripted move along the root curve |
| Rise to a slab (8 px) or a block (16 px) | `hop_on_slab` / `hop_on_block` | **Scripted move**: he first rolls to the clip's start offset from the edge, then the server sets `noPhysics` and follows the extracted root curve tick by tick, landing exactly on the block |
| Drop of a slab or block | `hop_off_slab` / `hop_off_block` | Same |

A scripted move that's interrupted (chunk unload, damage) snaps him to the nearest safe end of the curve, saved in NBT.

**Charge:**
- `4 FE` per block actually moved (from position deltas) and `50 FE` per trade (D49).
- A planned trip is refused unless `charge ≥ roundTripFE(path) + fePerTrade` (reserve rule).
- He re-plans home when the charge left equals the cost home.

**Home rules** (a `HomeRules` predicate, checked every 20 ticks): night (`!level.isDay()`), thunder (`isThundering`), redstone pause, low charge, nothing to sell (D52). He leaves with `action_comes_in` (physical root: hop out forwards) and docks with `dock_lights_out` (reverse in, reverse hop, lights pass to the station).

**Game tests (headless courses):** `gametest/TradeBotGameTests`, run with `./gradlew runGameTestServer` (or `/test runall` in a dev world). Each course is built in code on an empty 24 × 8 × 24 template:
- a straight run;
- slab and block hops;
- stairs;
- doors and gates (shut again behind him; an already-open door stays open);
- an L-shaped corridor;
- **the trading hall**: a yard, a slab step, a door, three booths (workstation counter + Create seat) along an aisle with a dog-leg, a gate and stairs to a gallery booth.

Each passes when he has reached every stop in order and docked again. Milestone 5 adds villagers to the seats. Findings that only the courses caught:
- a stuck check that always fired;
- held clips pinning him in place;
- waypoints behind him at the start;
- float precision in the synced clip start;
- a door that flickered shut before he was through;
- a hitbox too wide for doorways (now 0.6, like a villager).

**Tests:**
- the node evaluator on a test level: ladders and water rejected, door accepted;
- the turn classifier (angle → clip);
- the reserve rule maths;
- root-curve extraction (hop clips end at +16 px up and −18 px forward, as designed).

## Milestone 4: jobs and item handling

**Station screen** (`client/tradebot/TradeBotStationScreen`, an `AbstractSimiScreen` in the GUI kit style). The layout gets **mocked as an image for approval before it's coded** (standing rule). It shows:
- the bot's status line and charge bar;
- four input rows and an output row, each with the block's icon, name, distance and a ✕ to unlink;
- buttons **Add input**, **Set output** and **Call home**.

It has no item slots, so no quick-move controls apply.

**Linking (D19):** `network/TradeBotLinkPayload` (server-bound; `pos`, `mode`).
- It starts a 30 s **linking session** for that player (a server-side map, cleared on logout and server stop).
- The next right-click on a block (`PlayerInteractEvent.RightClickBlock`, cancelled) is validated:
  - it's an item container, belt or depot;
  - it's within range;
  - it isn't already linked;
  - the player is still the owner.
- Then it's linked, with a chime and an action-bar line. Sneak-click cancels.
- Linked blocks are outlined client-side while the screen is open or the player holds a wrench near the station, using Create's `Outliner`.

**Container interactions** (`content/tradebot/work/`): one `ContainerKind` per target, picked when he arrives:

| Kind | Detected by | Clips | Real block effect |
|---|---|---|---|
| Chest on the floor | `ChestBlock`, top within reach | `chest_take` / `chest_put` | lid opens and closes (`level.blockEvent(pos, block, 1, n)`) |
| Barrel | `BarrelBlock` | `barrel_take` / `barrel_put` (stands half a block back; items "poof") | `OPEN` state on and off |
| High or unreachable chest | top above his reach, or blocked | `chest_take_high` / `chest_put_high` | lid opens and closes by itself |
| Depot | Create `DepotBlock` | `depot_take` / `depot_place` (place to the side, push to the middle after a beat) | via the item handler |
| Belt (output only) | Create belt | `belt_place` | `DirectBeltInputBehaviour.handleInsertion` |
| Anything else with an item handler (vaults, modded) | the capability | blind routine (barrel clips) | none |

Each item move happens **on the clip's release frame**, read from the clip cues (for example the first `grip`), never at clip start. If the move fails (the container was emptied meanwhile), he plays `expr_curious` and moves on.

**Load and delivery (D22, D45, D47, D48, D57):**
- `ItemStackHandler load` (27 slots), `long emeralds` and `int xpBank`.
- Loading takes only items some seated buyer in range wants (from the trade plan), round-robin across the inputs.
- Delivery puts the emeralds into the output (as 64-stacks), followed by `xpBank / 3` **Create Nuggets of Experience** (`create:experience_nugget`, 3 XP each; the remainder stays banked). If the output is full, he waits beside it with `expr_sad` and retries every 5 s.
- Unsold items go back to their own input, or to any input, and if all are full they stay in his load.

**Tests:**
- the container-kind classifier;
- load planning (only wanted items, round-robin);
- delivery partials;
- link validation;
- the payload round trip.

## Milestone 5: trading

**Finding buyers** (`content/tradebot/trade/`):
- `BuyerFinder`: `AbstractVillager`s within range that are `VillagerSeats.sitting`, not babies, not trading with a player, and in loaded chunks.
- `TradePlan` matches their **sell-to-villager** offers (result = emeralds, `costA`/`costB` = goods) against his load.
- Visits are nearest-first, re-planned after each buyer (D49).

**Claims (D54):** `VillagerClaims` (a static map of villager UUID → bot UUID + expiry tick, cleared on server stop, like `CuckooClocks.RUNNING`). First come, first served; a bot skips claimed villagers.

**Standing spot:** a reachable block within 1 block (gap) of the villager's seat, facing it; a workstation or obstacle in between is fine (D7). He plays `trade_villager_open` when the gap is open and `trade_villager` otherwise.

**One trade per animation (D35):** on the clip's trade beat, `TradeExecutor`:
1. re-checks that the villager is still seated, not trading with a player, and the offer is in stock;
2. works out his price with `BotPricing.cost(offer, reputation)`: base cost + demand − `floor(reputation × priceMultiplier)`, min 1;
3. takes the goods from his load, adds the emeralds to the pouch, and banks the XP vanilla would have dropped as an orb (3–6, +5 on a level-up, only when `offer.shouldRewardExp()`) into `xpBank` (D57);
4. calls the new `TradeBotCustomer.comedymonkeysaddons$botTrade(offer)` on the villager.

That last method is **added to the existing `VillagerMixin`**. It does exactly what vanilla's `rewardTradeXp` does for the villager (uses, villager XP, level-up flag), using `@Shadow` like the clock code, **without** calling `notifyTrade`. Vanilla's `notifyTrade` posts NeoForge's `TradeWithVillagerEvent` with a null player when no player is trading, which other mods' listeners may not expect. No orb is spawned; the XP goes to his bank instead (D57). The mixin method returns the XP amount so the bot can bank it.

**Aborts (D46):**
- The villager stands up, dies or gets a player customer, or a contraption seat moves out of reach: he shrugs, keeps everything and retries later. There's a `shrug` sound but no shrug clip yet. It gets one in Blockbench in M6 (preview first); until then he plays `expr_curious` with the `shrug` sound.
- A player opening the villager always wins.

**Reputation (D34, D42):** a `Map<UUID, Integer>` on the bot: +2 per trade, capped at 25 (vanilla's trading-gossip cap), never negative, no decay. It's saved in his NBT and carried in `TRADE_BOT_DATA` when packed.

**Tests (pure, no level):**
- `BotPricing` (discounts, minimum 1, demand);
- offer matching (sell-only, `costB`);
- claims expiry;
- the reputation cap.

`MixinConfigTest` gets a check that `Villager` still has the shadowed fields (`villagerXp`, `increaseProfessionLevelOnUpdate`, `updateMerchantTimer`).

**Done (M5).** As built, in `content/tradebot/trade/`: `Buyers`, `BotPricing`, `TradeDemand`, `TradeSpot`, `TradeWork`, `VillagerClaims`, `TradeBotCustomer` (the mixin interface). What differs from the plan, or wasn't in it:
- **The spot** is two blocks out from the seat, 39 px from its centre, where the clip's preview villager sits, so the package lands in its arms. It prefers a side across a workstation, then the nearest.
- **Loading** takes only what the trip's buyers will take, in **whole lots of what's there**: for each offer, as many lots at its current price as its trades left and the goods in his inputs and load can pay for, less what he already carries. (Restocks raise prices through vanilla's demand: 64 sticks against two lots of 35 is one lot, not 64 with 29 carried back. Course: `takesWholeLotsAfterARestock`.) Items left over after trading go home with the D62 returns.
- **Leaving by himself:** docked, he checks every 10 s (D47). He goes only if some buyer's price can be paid **in full** from his inputs and load, or he has earnings to deliver. A run that sells nothing rests a minute before the next check, and a villager he couldn't reach is left alone for 2 minutes. This stops him shuttling 4 wheat to a farmer who wants 20.
- **Trades that open trades:** when his buyers run out, and again before he heads home, he looks at what they want now. A villager that levelled up mid-trip, buying something new his inputs hold, gets another round of fetching and trading on the same trip, at most 3 extra rounds; he doesn't dock and set straight back out. A villager who called a trade off waits for another trip (D46). Course: `sellsWhatALevelUpOpensOnTheSameTrip` (a fletcher's sticks, then its apprentice flint trade).
- **Grouped trades (D63):** one clip per villager visit. On its trade beat he makes every trade he can pay for, while his charge still covers a trade and the way home. The lobbed package and the emerald now draw in game: the export lifts them out of the preview-only villager prop into a `trade_props` root beside the bot, and they show only in clips that animate them.
- **Aborts:** before the trade beat (4.0 s) he checks every tick that the villager is alive, seated, in place and not trading with a player. If not, he plays `expr_curious` with the `shrug` sound and moves to the next buyer.
- **Restocks:** villagers seated at their workstations restock, as they do for players.
- **Goggles** show trades today, counted by the station and reset each morning. `/tradebot trade` starts a run now.
- **Courses:**
  - `tradesRoundTheHall`: four seated villagers, five trades in four clips. Only the goods he can sell leave the chest, and the leftovers come back.
  - `shrugsWhenTheVillagerStandsUp`: everything comes back and no stock is used.

## Milestone 6: feeling alive

**Scared routine (D8, D26):**
- `HostileWatch` (every 10 ticks): any `Monster` within 12 blocks with line of sight.
- `CornerFinder`: a search of reachable spots within 8 blocks; the score is walls around the spot + distance from the mob.
- The stages are `scared_spot` → `scared_flee` (fast roll, `roll_fast_loop`) → `scared_hide` → `scared_cower` (`tremble_loop`) until no hostile has been seen for 5 s → `scared_recover`. Then he resumes his job.
- Monsters don't target him: no goal targets his class. Explosions and stray arrows hurt as normal.

**Reactions** (a `ReactionScheduler` with cooldowns):
- `wall_bump` when the navigation is stuck against a block;
- `sees_player` when a player comes within 6 blocks after not being seen for 60 s;
- `expr_happy` after a good trade run;
- `expr_cross` when the owner hits him;
- mannerisms (`count_stock`, `expr_curious`, `expr_doze_off` at night) while docked or idle.

**Damage (D24, D29):**
- At 0 HP he **winds down** instead of dying: `die()` is overridden, his health is kept at 0.5, and the state is set to `WOUND_DOWN` (lights off, slumped).
- A wrench right-click from the owner re-winds him (the `key_rattle` sound) to full HP.
- In water he winds down until he's out and dry (5 s).
- If he's wound down in lava or falls below the world's minimum height, he's returned to his station, still wound down (D59). This is handled in `tick` and in an `outOfWorld` override, and goes through the same safe teleport as the snap-to-end recovery.

**Sounds:**
- `CuePlayer` (client) fires one-shot cues as the clip time crosses them (`level.playLocalSound` at the bot, `SoundSource.NEUTRAL`). It de-duplicates across frames and blends, and skips loop events.
- **Voice cooldown:** a voice cue is skipped if one played less than 4 s ago (D43).
- Loops are `AbstractTickableSoundInstance`s:
  - `roll_loop`, with volume and pitch following speed;
  - `roll_fast_loop` while fleeing;
  - `tremble_loop` while cowering;
  - the station's winding loop while it's charging him.

**Tests:** corner scoring on a fixture grid, the reaction cooldowns and the cue crossing logic (pure).

**Done (M6).** As built (`content/tradebot/react/`, the brain, the motion, `client/tradebot/TradeBotLoopSounds`). What differs from the plan, or wasn't in it:
- **New clips,** previewed and approved in Blockbench first:
  - `shrug`: called-off trades use it now, not `expr_curious`;
  - `wind_down`: holds its last frame;
  - `rewind`: starts exactly on `wind_down`'s last frame, which a test checks.
  
  They reuse approved sounds. `sees_player` is now a game-root clip: the brain turns him to face the player instead of the clip.
- **Interruptions:**
  - Reactions and frights only interrupt travelling and waiting, never a container or a trade clip, so the item bookkeeping stays exact.
  - Running down interrupts anything. After any interruption he resumes his job, his trip, or goes home.
  - Nothing interrupts a hop; `halt()` waits for the landing.
- **Scared:**
  - Checked every half second while he's out, for monsters within `tradeBotHostileRadius` that he can see.
  - `CornerFinder` searches spots within 8 steps: two walls or more score 3 blocks of distance per wall; otherwise it's distance from the mob.
  - Fleeing runs at twice cruise speed, with `scared_flee` instead of the rolling clips and no turn clips.
  - He recovers 5 s after the last sighting and stays calm for 5 s after that.
- **Reactions** (`Reactions` cooldowns):
  - `sees_player` for a player within 6 blocks, in sight, who has been away a minute;
  - `expr_cross` when a player hits him;
  - `expr_happy` once at the end of a run that made trades;
  - `wall_bump` inside the motion, before a stuck replan, at most every 10 s.

  Idle mannerisms aren't in: he's only idle docked, where he sleeps with his lights out.
- **Running down:**
  - At 0 HP `die()` winds him down instead (health 1); only `/kill` removes him.
  - In water: winds down, dries out 5 s after he's out, then winds himself up. He can't drown (`canDrownInFluidType`).
  - Lava or below the world: put in front of his station, wound down. The owner's wrench winds him up to full health.
  - Saved across reloads. The station's goggles say "Wound down", and the comparator gives 15 (as out of charge: he needs his owner).
- **Sounds:**
  - The voice cooldown is between clips: within a clip every voice cue plays, but a new clip's voice waits 4 s after another clip spoke. In looping clips the voice plays every other time round.
  - Loops: `roll_loop` (volume and pitch follow his real speed), `roll_fast_loop` while fleeing, `tremble_loop` while cowering. Each fades in and out.
  - The station's winder clicks `key_rattle` every 0.8 s while charging him.
- **Courses:** `hidesFromAZombieAndCarriesOn`, `windsDownInWaterAndDriesOut`, `windsDownAtNoHealthAndTheWrenchWindsHimUp`, `comesHomeFromLavaAndTheVoid`.

## Milestone 7: teaching and release

**Release:** route logging (`TradeBotMotion.debug`) is off again by default; `/tradebot debug` turns it on.

- **Ponder** (`client/ponder/TradeBotScenes`, three storyboards in `ponder/trade_bot.nbt`):
  1. placing him and powering the station from below;
  2. linking inputs and output;
  3. a trade with a seated villager.
  
  Scenes play clips through a ponder-only `setPonderClip` on the entity (like the clock's `setPonderTime`).
- **FE Coil** (D56): `ModItems.FE_COIL` (`fe_coil`), `recipe/fe_coil.json` (shaped: 4 copper sheets (`c:plates/copper`) around 1 redstone dust, proposed). Its look is designed and **previewed in Blockbench first**, matching the mod's other item icons. It goes in the creative tab next to the electric machines, with a Create tooltip.
- **Recipe** (D44): `recipe/mechanical_crafting/trade_bot.json`, 5×5, with the FE Coil as its FE part.
- **Lang:**
  - block and item names;
  - the Create "Hold Shift" tooltip (`registerCreateTooltip`);
  - goggles lines, screen strings and messages;
  - config names;
  - 80 subtitles;
  - Ponder text.
- **Config** (`CMConfig`, a new `tradeBot` section, server): `tradeBotRange` 64, `tradeBotsPerPlayer` 4, `tradeBotChargeFePerTick` 64, `tradeBotStationBuffer` 100,000, `tradeBotFullCharge` 100,000, `tradeBotFePerBlock` 4, `tradeBotFePerTrade` 50, `tradeBotHostileRadius` 12. `CMConfigTest` covers them automatically; add the comment-promise test (a full charge covers at least 1,000 blocks of travel at defaults).
- **Creative tab, README section, version bump**, and a release build.

**Done (M7, before release):**
- **FE Coil:** the icon is drawn by `design/items/fe-coil.gen.js` (a bobbin: copper windings, steel flanges, a redstone core, brass leads), previewed and approved. Shaped recipe: 4 copper sheets around redstone.
- **Trade Bot's recipe:** a 5x5 mechanical crafting grid, approved: `STPTS / SPEPS / ATBTA / SGFGS / AGPGA`.
- **Ponder:** three scenes (station and power, linking, trading), structures from `design/ponder/trade-bot.ponder.js`. In them he plays his real clips on his own clock (`setPonder`), because Ponder's world clock stands still; his wheel and rolling sound use `xOld`, which Ponder does keep up to date.
- **Advancements:** ten, from `trade_bot` (after `fe_coil`). His owner gets the credit wherever they are; the wrench one goes to whoever winds him up.
- **README section**; version **2.0.0**.

## Order of work and size

| Milestone | Rough size | Playable result |
|---|---|---|
| 0 Export pipeline | small–medium | assets in the mod, data tests green |
| 1 Entity + clips | medium | spawn him and play every clip in game |
| 2 Station, item, power, owner | medium | place and pack him; he charges |
| 3 Movement | **large** (the riskiest part) | he rolls, turns, hops and docks |
| 4 Jobs and items | medium–large | he fetches and delivers |
| 5 Trading | medium | v1 loop complete |
| 6 Life | medium | scared routine, reactions and sounds |
| 7 Ponder, recipe, release | small–medium | shippable |

Each milestone ends with the unit tests, a headless check and `runClient` for you to try.

## Risks

1. **Hops and stairs against real terrain** (M3). The clips assume exact distances (the slab hop starts 4.5 px back and lands 18 px forward). The fix is the "roll to the start offset first" step plus the snap-to-end recovery. Budget time for odd blocks: carpets, snow layers, half-open trapdoors.
2. **Coordinate conversion** (M0/M1). It's mitigated by calibrating against Blockbench's own Java export and by the side-by-side pose check.
3. **The villager mixin** (M5) depends on private vanilla fields. That's the same pattern and risk as the Cuckoo Clock, covered by `MixinConfigTest`.
4. **Performance:** at most 4 bots per player. Pathing runs only on state changes and every 20 ticks while travelling. Clip sampling is client-side array lookups.

## Decided since the first draft

- **FE Coil:** a new component from copper and redstone (D56; recipe proposed above, look previewed first).
- **Trade XP:** delivered to the output as Create Nuggets of Experience, no orbs (D57).
- **Comparator:** 0 / 5 / 10 / 15 (D58).
- **Lava and void:** returned to his station, wound down (D59).

No open questions remain for v1.
