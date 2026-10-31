# Clockwork Wastes: build progress

Spec: [clockwork-wastes.md](clockwork-wastes.md). Looks: [blocks brief](../briefs/clockwork-wastes-blocks.md), previews in `design/models/clockwork-wastes-*.gen.js`.

## Phase 1: foundation (done, 2026-10-29)

### Biome
- `content/wastes/EndBiomes.java`: the shared **End biome slot list** (`SLOTS`, one `Slot(biome, weight)` per End biome; biomes 2 and 3 add a line). `register()` hands each to TerraBlender's `EndBiomeRegistry.registerHighlandsBiome` and `registerMidlandsBiome` (never edge or island), and adds the END surface rule. Called once from `ModWastes.commonSetup()` inside `FMLCommonSetupEvent.enqueueWork` (TerraBlender reads the lists at `ServerAboutToStartEvent`).
- Surface rule (`EndBiomes.surfaceRules()`): in the Wastes, the top layer (`ON_FLOOR`) is Scorched End Stone; where `minecraft:surface` noise > 0.3 it's Rust-Stained, below -0.45 Littered. Everything under stays End Stone.
- P53 check: `EndBiomes.checkEnd(server)` on `ServerStartedEvent` logs one warning if the End's biome source isn't `TheEndBiomeSource` or doesn't list the Wastes. The highlands fallback for cities is phase 2's.
- Biome key `comedymonkeysaddons:clockwork_wastes` (`EndBiomes.CLOCKWORK_WASTES`). Data written before phase 1: the biome JSON, `is_end` tags, `has_structure/clockwork_city`.

### Config
- **Decision (spec silent on the mechanism):** the biome weight is in a new **COMMON** config, `CMConfig.COMMON` / `COMMON_SPEC` (`config/comedymonkeysaddons-common.toml`, section `clockworkWastes`, `biomeWeight`, default 1, 0 = off), not the Server section: TerraBlender takes its End slots once at startup and has no way to change or remove them, and server configs only load per world, after that. Registered in the mod constructor. Phase 2's city settings (spacing, fallback, wake charge...) belong in the **Server** section beside the Juggernaut's, as P26 says.

### Blocks and items (`registry/ModWastes.java`)
All registered through `ModBlocks.BLOCKS` / `ModItems.ITEMS` from one class (`ModWastes.init()` is called first in the mod constructor). Every block has a BlockItem (Carry On). All are `#minecraft:mineable/pickaxe`; none are in `#minecraft:enderman_holdable` (vanilla's tag doesn't include them; nothing to do for P51).

| id | class | notes |
|---|---|---|
| `scorched_end_stone`, `rusted_scorched_end_stone`, `littered_scorched_end_stone` | `Block` | End Stone properties; random y-rotation variants |
| `void_alloy_plate`, `seamed_void_alloy_plate` | `Block` | full cubes; the seamed plate's lit seams are a separate model for phase 2 (`block/wastes/seamed_void_alloy_plate_seams`) |
| `void_alloy_pillar` | `RotatedPillarBlock` | stepped chamfer, seam strips dark |
| `charging_alcove`, `maker_statue` | `ThreePartBlock` (standing) | 1 x 3, `facing` + `part` (bottom/middle/top), placed and broken like a door, drops once (bottom part's loot). Alcove's lit contact plate: `block/wastes/charging_alcove_contact_<part>` for phase 2 |
| `toppled_maker_statue` | `ThreePartBlock` (lying) | 3 x 1, parts run toward `facing.getClockWise()` (east for north) |
| `scrap_heap` | `ScrapHeapBlock` | half height, random y rotation |
| `broken_press`, `broken_mixer`, `broken_deployer`, `broken_mechanical_arm`, `broken_crushing_wheel` | `SalvageBlock` | horizontal `facing`, `IWrenchable` |
| `worker_shell` (pose: slumped/fallen/frozen), `resistance_shell` (pose: slumped/fallen/reaching) | `ShellBlock.Worker`, `ShellBlock.Resistance` | `facing` + `pose`; the city sets the pose |
| items `clockwork_scrap`, `unknown_device` (RARE, stacks to 16) | `Item` | Create "Hold Shift" summaries |

Creative tab: `ModWastes.addToTab` (called from `ModCreativeTabs` after the Juggernaut egg). Lang: names, tooltip summaries, biome name, config names.

### Salvage (D26, D32, P16, P43)
- `content/wastes/Salvage.java`: `salvage(level, pos, player, tool)` is the one path: rolls `loot_table/salvage/<id>`, plays the break effect, removes the block without its normal drop, pops the stacks out where it stood.
- Player: Create's Wrench calls `SalvageBlock.onSneakWrenched` (sneak-click). A plain wrench click rotates it (IWrenchable default).
- Deployer: Create's `DeployerHandler` calls the block's `useItemOn` with a `DeployerFakePlayer` first; `SalvageBlock.useItemOn` salvages when the clicker is a `DeployerFakePlayer` holding a `WrenchItem`.
- Anything else (pickaxe, drill, saw, blast): `loot_table/blocks/<id>` = 1 Clockwork Scrap (no explosion condition, so blasts give it too). Needs the right tool by hand.
- Salvage tables: machines 1-3 Create parts (weighted per machine), a 5-12% Precision Mechanism, 1-2 scrap; shells 2-3 scrap + 25% Electron Tube. Tag `comedymonkeysaddons:salvage` lists the salvage blocks.
- Crushing: `recipe/crushing/clockwork_scrap.json`: 3 iron nuggets, 2 Create copper nuggets, 1 brass nugget + 50% one more, 10% zinc nugget.

### Models (exporter)
- `design/models/clockwork-wastes.export.js` writes models, blockstates, item models, item icons, only the used textures (`textures/block/wastes/<key>.png`; the folder is the prefix), loot tables, the pickaxe tag, the salvage tag and the crushing recipe. **Re-run:** `node design/models/clockwork-wastes.export.js` (from the repo root or `design/models`). It exits 1 if any cross-texture faces are closer than 0.2 px (z-fight check), and throws on any export rule broken (UV off the texture, zero-width UV, bounds outside -16..32, rotations other than 0/±22.5/±45 on one axis). **It regenerates the loot tables and recipe**: change their numbers in the exporter, not the JSON.
- `clockwork-wastes-salvage.gen.js` now exports its builders and only writes its `.bbmodel` when run directly; `clockwork-wastes-materials.gen.js` also exports `pillar`.
- Exact rebuilds of preview-only constructs: the Crushing Wheel's lying half (a 90 degree turn) and the toppled statue (90 degree turns) are done by swapping axes, each face keeping its UV with the face rotation that keeps the picture the right way round (`quarter()`); the Mechanical Arm's claws' two rotations (own + lean, both about z) are composed into one (`compose()`): one claw becomes unrotated, the other a single 45 degree turn.
- Three-long blocks are cut into one model per part (`slice()`). Cut cubes take their UVs per block cell (the preview stretched the 46 px alcove back over one texture; in game each block shows the tiling texture at 16 px a block like everything else). Quarter-turned cubes keep the preview's UVs, cut in step.
- **Shells are OBJ models** (`models/block/wastes/<shell>_<pose>.obj`, one shared `wastes_shells.mtl`, NeoForge's `neoforge:obj` loader): the posed bones turn at angles a block model can't hold (-38, 95, -170...), so the exporter poses the rig exactly as Blockbench does (bone rest rotations right-handed, Euler order ZYX = x first, checked against Blockbench 5.2.2's `NodePreviewController`) and bakes every face to a quad with its own UV and face rotation. Static, chunk-baked, no block entity or renderer. Item model = the slumped pose with a fitted GUI view.
- Toppled statue: rests on the ground (the preview floated it 1.5 px); its head is set past the shoulders in the third block (the preview had it a block further on, in a fourth), rolled 22.5 degrees (preview 25).
- Flat icons (house rule for tall blocks): `charging_alcove`, `maker_statue`, hand-drawn in the exporter (`ICONS`), copies in `design/texture-reviews/clockwork-wastes-icons/`. **Not previewed with the user yet.** The toppled statue's item is its 3D model fitted to the slot.

### Tests
- Game tests `gametest/WastesGameTests.java`: sneak-wrench salvage of a machine and a shell (full loot, block gone), Deployer (DeployerFakePlayer + Wrench through `useItemOn`), plain wrench click turns and salvages nothing, pickaxe and drill/blast give exactly one scrap, decor drops itself and a three-long block drops once from its top, the toppled statue's run, the biome in TerraBlender's highlands and midlands (not island) at the configured weight and `checkEnd` true on the test server.
- Unit test `src/test/.../content/wastes/WastesAssetsTest.java`: every blockstate model and texture exists, the OBJ shells are whole (quads, UVs on the texture, materials resolve to model textures), names, tooltips, loot tables.

### Known looks to check in game (no client run in phase 1)
- The approved preview's poses carry over exactly, including what it hid: the slumped Worker's shins and the slumped Resistance's legs fold back and dip up to ~6 px below its floor (the preview's floor block hid it), and the slumped Worker's arms reach about 8 px past its back (through the wall it leans on in the preview). Check in game; a pose fix belongs in `POSES` in the salvage generator, then re-export.
- Shells collide as one block (their models lie across two blocks when fallen, stand two tall when frozen). A filler block can come later if the city needs it.
- The Broken Deployer's hand lies 16 px in front of it, in the next block; the Crushing Wheel's fallen half reaches 4.5 px into the block behind.

## Phase 2 models (exporter)

`clockwork-wastes.export.js` now also writes the nine powered city blocks from `clockwork-wastes-city.gen.js` and `clockwork-wastes-reactors.gen.js` (both now export their builders and only write their `.bbmodel` when run directly; their previews are unchanged apart from Blockbench's random uuids). Same house checks and z-fight check; it still exits 0. Each block's still parts are its block model; what moves or glows is a **part model** (`models/block/wastes/<name>.json`, loaded standalone, posed by the renderer; the exporter prints the list, 31 of them). Glows are `shade:false` cutout parts; glass is in its own `minecraft:translucent` models.

| id | block models (blockstate) | parts |
|---|---|---|
| `substation` | `substation_lower/_upper` (`facing`, `half`) | `substation_lever` (turns about x through (16,20,8): 0 shut, -130 open), `_seams`, `_sigil`, `_flash`, `_column` (scaled in y about y 3.5) |
| `city_vault` | `city_vault` (`facing`) | `city_vault_blade` (= blade 5, z 2.25), `_blade_seam`, `_pip` |
| `sliding_door` | `sliding_door_<col>_<row>` (`facing`, `col` 0-1, `row` 0-2): the jamb cut per cell | `sliding_door_leaf_l/_r`, `_seam`, `_chain`, `_jamb_glow`, all in the controller's coords 16 px down |
| `derelict_street_lamp` | `_foot`, `_pole`, `_head`, `_head_snapped` (`facing`, `part`, `snapped`) | `derelict_street_lamp_glow` (head coords) |
| `derelict_wall_lamp` | `derelict_wall_lamp` (`facing`) | `derelict_wall_lamp_glow` |
| `derelict_cable` | multipart: `derelict_cable_arm` per side (east 0, south 90, west 180, north 270), `derelict_cable_knot` at corners, `derelict_cable_arm_glow` per side when `lit` (emissive in the block model: `neoforge_data` light 15, no renderer) | none |
| `conduit_pylon` | `conduit_pylon_bottom/_middle/_top` (`facing`, `part`): shaft, collar, groove, dark seams | top-block coords (y -32): `_crown` (slumps about x through (8,8,5)), `_fin`, `_fin_stub` (fin k: 60k+30 about y), `_core`, `_core_cracked`, `_core_dark`, `_core_blaze`; middle coords (y -16): `_seams`; bottom: `_groove_glow` |
| `derelict_star_reactor` | multipart on `part` (0-26 = x + 3z + 9y) and `salvaged`: part 4 `derelict_star_reactor` (ring base, grooves turned 45), part 13 `_glass` / `_glass_cracked` (translucent, its own coords), the rest `_empty` | `_arm` (y -16; arm k 90k about y, salvaged -35 about x through (8,-10,-10)), `_arm_seam` (y -16), `_core` (y -16, translucent glow), `_grooves_glow` |
| `star_reactor` | multipart on `facing` (terminal = model south; south none, west y90, north y180, east y270, up x90, down x270) and `upgraded`: `star_reactor`, `star_reactor_glass` (translucent), `star_reactor_pips8` / `_pips12` | `star_reactor_orbit`, `star_reactor_pip` (one lit pip at (8,16.5,8)), `star_reactor_ports_lit` (translucent glow) |

Rebuild decisions (the previews couldn't export as drawn):
- Textures both new generators paint under names the salvage set already uses are kept apart, so phase 1's pictures don't change: the city's `copper` and `lens_ring` export as `terminal_copper` and `lamp_lens_ring`, the reactors' `brass` and `copper` as `reactor_brass` and `reactor_copper`.
- **Substation:** the frame's lit seams lay on the dark ones (coplanar, a z-fight whenever lit): now 0.25 px in front.
- **Conduit Pylon:** each preview state had either dark or lit seams; here the dark ones are in the block model, so the lit ones sit 0.25 px further out over them.
- **Sliding Door:** the preview's lit jamb grooves were 0.25 px inside the jamb's faces (hidden behind them); now 0.25 px proud of them.
- **Star Reactor orbit ring:** segments turned -67.5, -90, -112.5... are square across, so each is the same turned by its angle less a quarter turn: all fall in 0/±22.5/45.
- **Derelict Cable:** only the floor run; the arm is the east half of the preview's (strands x 8-16, the clip at 11), so a straight run is exactly the preview's. A knot clip (x 7-9, z 6.5-9.5, 1.5 high) covers where two arms' strands cross at a corner; the hairlines meet under it, so it needs no glow of its own. Spans and wall/ceiling runs aren't built.
- **Street lamp:** no leaning variant (a block model can't lean a three-block post as one); the foot is the unchipped one.
- **Items:** flat icons (exporter `ICONS`, drawn as rectangles, copies in `design/texture-reviews/clockwork-wastes-icons/`) for the Substation, Sliding Door, Derelict Street Lamp and Conduit Pylon, **not yet previewed with the user**. The City Vault's item is an **OBJ** of the closed vault (`city_vault_item.obj`: its blades at 60k degrees can't be a block model's), baked as the shells are. The Wall Lamp and Cable (a straight run) are their models fitted to the slot; the Derelict Star Reactor's is its base, four arms (quarter turns, their own tilts turned with them) and dark glass dropped 8 px and fitted; the Star Reactor's is its block model.
- Data from the exporter: loot tables for all nine (scrap per the spec, once per multiblock; the pylon's empty; the Star Reactor keeps `star_burn` and `reactor_overhauled`), `salvage/derelict_star_reactor` (2 Unknown Devices), and all nine in the pickaxe tag. `WastesAssetsTest.cityBlockstatesPartsAndTexturesExist` checks the blockstates (variants and multipart), items, parts, textures and loot.

## Phase 2: the powered city blocks (done, 2026-10-29)

All in `content/wastes/` (same package as phase 1), registered in `registry/ModWastes.java` (blocks, BlockItems, block entity types, the two data components, capabilities, events). Renderers in `client/wastes/WastesRenderers.java`. Every block has a BlockItem; the locked and fixed ones (Substation, City Vault, Sliding Door, Conduit Pylon, Derelict Star Reactor) are in Carry On's `carryon:block_blacklist` tag.

### The city record (P48) and its API
- `CityRecord` (level `SavedData`, `comedymonkeysaddons_clockwork_cities`): per city origin, each of the four districts `awake` (for good), `fed` (its Substation meets upkeep), `cut` (its Substation was broken after waking), `heartUntil` (game time the heart's star burns to), `custodianAwake`, `custodianDead`. `District` enum: FOUNDRY, WORKSHOPS, RAIL_YARD, QUARTERS, HEART.
- Reading: `CityRecord.of(level)`, `awake(origin, d)` (HEART = all four), `allAwake`, `fed(origin, d, now)` (awake and (own feed and not cut, or the heart burning); HEART = burning or all four fed), `heartBurning`, `heartUntil`, `custodianAwake`, `custodianDead`, `cut`, `vaultOpen(level, origin, d)`, `doorOpen(origin, d)`, `cities()`.
- Changing: `add(origin)` (**phase 3: call when a city is placed**), `wake(level, origin, d, from)` (action bar "The Foundry wakes…" to players within 64 of `from`, "Lights On"; the fourth fires `heartWoke` and "City of Lights" within 160), `wakeAll`, `setFed`, `cut`, `burnHeart(level, origin, until, from)`, `wakeCustodian`, `custodianReset`, `custodianDied` (any death, D35). Once a second (`onLevelTick`) a heart star that burned out puts the lamps out even with the heart unloaded.
- **Listeners (phase 4):** `CityRecord.listen(Listener)`: `districtWoke` (wake its bots within 5 s), `fedChanged(level, origin, district, fed)` (Sentries fold while unfed, P9; the heart is reported as HEART), `heartWoke` (wake the Custodian: call `wakeCustodian`), `custodianDied`.
- **Binding (phase 3):** city blocks keep their city in BE NBT (`CityOrigin`, `District`; `CityLink`). After placing a district piece call `CityLink.bindAll(level, box, origin, district)` (or `bind` per block); a template can also carry the NBT. Heart blocks (heart vault, heart doors, pylons, the Derelict Star Reactor) bind to `District.HEART`. An unbound block reads as a district that never wakes (a creative-placed vault stays locked; creative can still break it).
- How blocks hear: `CityBlockEntity` (base of every city block entity) joins its city's loaded blocks on load and catches up with the record on its first scheduled tick (so a district that woke while it was unloaded opens/lights it as soon as it's back: no chunk loading); later changes reach it at once (`cityChanged(from)`, "live": animated, lamps ripple from `from`). Non-ticking blocks act on one scheduled tick.
- Game tests only: `CityRecord.PEACEFUL_FOR_TESTS` (cities that follow the Peaceful heart-vault rule without making the server Peaceful).

### Blocks
| id | class (BE) | what it does |
|---|---|---|
| `substation` | `SubstationBlock` (`SubstationBlockEntity`) | 1 x 2 (`half`, `facing`, `locked`), placed and broken like a door. FE in on any side of its lower half (`Capabilities.EnergyStorage.BLOCK`, receive only; a Battery beside it pushes in, Power Lines deliver). Dormant: every FE counts toward the wake charge (config 1,000,000); at exactly the charge it wakes its district for good (record), unlocks both halves, its lever slams. Woken: a store of 2 s upkeep; once a second (P52, the only work it does) it pays a second's upkeep (config 500 FE/t): fed or unfed to the record. Column 0-10 steps with the charge; comparator = charge (15 woken); goggles: charge, then fed/unfed and draw. Broken after waking: scrap, district stays awake, `cut` for good (P50). |
| `city_vault` | `CityVaultBlock` (`CityVaultBlockEntity`) | `facing`, `locked`, `heart`. 27 slots, loot rolled on first open (shared, P23): `chests/clockwork_city_vault` (exactly 1 Unknown Device; brass, zinc, Electron Tubes, Precision Mechanism, brass sheet, scrap; 35% an enchanted book) or `chests/clockwork_city_heart_vault` (exactly 1 Elytra, 2 Unknown Devices, brass, mechanisms, tubes, diamonds, emeralds, a golden apple, scrap, a level-30-39 book). Opens: district's when awake; heart's on the Custodian's death, or Peaceful with all four awake. Locked: action bar "The iris is sealed…". Uses the Upgraded Chest's screen (copper band; see below). Items capability only once open. |
| `sliding_door` | `SlidingDoorBlock` (`SlidingDoorBlockEntity` on the controller) | 2 x 3 (`col` 0-1 toward the facing's clockwise side, `row` 0-2), `open`, `heart`. Opens for good when its district wakes (heart doors: all four): every part open at once with no collision (so it can never close on anyone), then the seam lights and the leaves slide into the walls (0.5-1.5 s). Open, it's breakable (2 scrap from the controller). |
| `derelict_street_lamp` | `DerelictStreetLampBlock` (`DerelictLightBlockEntity`) | stacks: each block takes `part` foot/pole/head from its neighbours; `facing` (the crook), `snapped` (never lights), `lit`. The head lights at 15 while its district is fed. |
| `derelict_wall_lamp` | `DerelictWallLampBlock` (same BE) | `facing` away from the wall clicked, `lit` (15). |
| `derelict_cable` | `DerelictCableBlock` (same BE) | floor only, `north/east/south/west` arms toward cable, a Substation's foot or a lamp post's foot (one join runs straight on; alone, east-west), `lit` (the emissive line in its block model; no light cast, no renderer). 1 scrap in 4. |
| `conduit_pylon` | `ConduitPylonBlock` (a `ThreePartBlock`; `ConduitPylonBlockEntity` in the bottom) | never mined or blasted; a player's swing (`attack`) or a player's projectile (`onProjectileHit`) is a hit, at most one per 8 ticks, never a fake player or a mob, and only while its city's Custodian is awake. 6 hits (config) break it (fin k snaps at hit k+1). |
| `derelict_star_reactor` | `DerelictStarReactorBlock` (`DerelictStarReactorBlockEntity` at part 4; `DerelictReactorPortBlockEntity` at parts 1, 3, 5, 7) | 3 x 3 x 3, `part` 0-26 = x + 3z + 9y, `salvaged`. A Nether Star in by hand, Deployer (`useItemOn`) or item handler on any of its 27 blocks (funnels, arms, chutes, packages): `wakeAll` + `burnHeart` for the star's 8 h. While burning it makes the reactor output into a store; a Power Line on a port draws it (`PowerGrid.Generator`, ports record as the controller like the turbine's casings; on paper while unloaded). Sneak-wrench (or a Deployer with a Wrench): `salvage/derelict_star_reactor` (2 Unknown Devices), arms fall open, salvaged on every part, no reactor ever again; only then can it be broken (3 scrap). |
| `star_reactor` | `StarReactorBlock` (`StarReactorBlockEntity`) | any of six facings (`facing` = its copper terminal, toward the block it was placed against), `upgraded`. One star at a time by hand, Deployer or item handler (any side). Flat out `reactorOutput` (8,192) FE/t into a 1,000,000 store, pushed out of the terminal each tick; FE capability on the terminal face only. Each running tick burns a tick: `burnHours` (8) x 72,000; overhauled x `upgradeMultiplier` (1.5 = 12 h) by right-click or Deployer with an Unknown Device (once; a star already in stretches too). Redstone pauses output and burn. Comparator = star left. Goggles: output/paused/no star, star left ("7 h 42 min"), "Overhauled: 12 h a star", store. Item keeps `comedymonkeysaddons:star_burn` (long) and `reactor_overhauled` (copy_components). A `PowerGrid.Generator` on paper while unloaded (the burn is its one fuel, 1 a tick; paused = rate 0, burns nothing). Never explodes. Aeronautics: nothing special needed, it ticks on a Sable ship like the Lava Generator (which has no special handling either); no Sable test written. |

Locks (`CityLocks`): locked blocks have bedrock's hardness (-1) and blast resistance, so Create's drills, saws and contraptions and pistons pass them by; a player's breaking goes through `getDestroyProgress` (none while locked, the block's own hardness once open) and blasts through `getExplosionResistance`. ~~The hardness stays -1 after unlocking~~ **Superseded by D42** (see "The user's decisions D39-D58" below): woken vaults, doors, Substations (and lifts) are ordinary blocks at their own hardness.

### Looks (renderers)
`WastesRenderers`: each block's `parts` method poses its solid moving parts and opaque glows (Flywheel instances via `MachineVisuals.visual` / `visualWhen`; the renderer draws them when Flywheel can't). Translucent glows (the reactors' cores, the Star Reactor's lit ports) and the Nether Stars (the vanilla item, full-bright) stay on the BERs. Motion from the approved clips: lever slam (0.12 s, bounce, settle) with the contact flash at 0.08-0.3 s and the seams/sigil from 0.1 s; column scaled by fill/10, dim when woken and unfed; vault seams spiral on (blade k at k x 0.08 s) then the blades swing -40 and fold (0.5-1.5 s), the centre pip pulses on a 4 s beat while locked; door seam and chain lit 0-0.5 s, leaves slide 16 px each (0.5-1.5 s) then hide, the jamb glow lit after; lamp lens full-bright, the Electric Lights' flicker for a second when starving (a lamp's visual exists only while it's lit); pylon fins snap in order with the newest tumbling off (0.55 s), core dimmer per hit, cracked from the first, flickering when failing, dark when broken, blazing for 0.6 s when re-lit, crown slumps -20 (jolt) when broken and jolts back when re-lit, failing coils throw spark particles; derelict reactor grooves, arm seams and the inner sphere (4 s pulse) lit while burning, star turning 45 deg/s, arms fall -35 when salvaged; Star Reactor pips (one out per hour burned, first first), orbit ring tilted 25 and turning once per 6 s while running, ports lit and the star turning once per 8 s.

### Config (Server, section `clockworkWastes`)
`wakeCharge` 1,000,000, `upkeep` 500, `reactorOutput` 8192, `burnHours` 8, `upgradeMultiplier` 1.5, `pylonHits` 6.

### Data
Lang (names per P29, Create summaries for all nine, districts, action bar, goggles, advancements, config), advancements `wastes_lights_on` (parent root), `wastes_city_of_lights` (challenge, parent Lights On), `wastes_star_power` (goal, parent root; by hand, Deployer, funnel or the heart's reactor), trigger `comedymonkeysaddons:action` with constants in `ModAdvancements`; `recipe/mechanical_crafting/star_reactor.json` (S U S / U C U / S P S: 3 Unknown Devices, Brass Casing, Precision Mechanism, 4 scrap); the two chest loot tables (hand-written; the block loot is the exporter's).

### Dev tool
`/wastes testsetup` (op, dev only like `/lights testsetup`): a test district beside you (Substation, cable, three lamp posts with the middle head snapped, a wall lamp, both vaults, both doors, a pylon, the heart's reactor, a Star Reactor), all bound to a city at your feet. `/wastes wake <district|all>`, `/wastes heart <minutes>` (the heart burns: every lamp on, then off with a flicker), `/wastes custodian wake|die|reset` act on the nearest city.

### Tests
- Game tests `gametest/WastesCityGameTests.java` (14): Substation wakes at exactly the wake charge (999,999 leaves it dormant, the last 1 wakes it, unlocks both halves, comparator 15); upkeep keeps the district fed and it goes unfed without power (staying awake); locked vault/Substation (both halves)/door/pylon can't be mined (progress 0, hardness -1 for contraptions) and survive a TNT-sized blast; a vault opens when its own district wakes (not another), becomes minable, and holds exactly 1 Unknown Device; a vault loaded from saved data after its district woke catches up and opens (P48); the heart vault stays shut with four awake and opens on the Custodian's death with 1 Elytra and 2 Devices; on Peaceful it opens after the fourth wake; doors open (no collision, breakable) when their district wakes, a heart door only with all four; a lamp post stacks foot/pole/head, its head lights at 15 and the cable's line lights when fed, the head flickers then goes dark unfed; Star Reactor makes exactly 20 x 8,192 FE and burns 20 ticks in 20, redstone pauses the burn; it drops with its burn and upgrade and placed again carries on; overhauled stars burn 12 h (8 h plain, a mid-burn fit stretches), the recipe loads; the heart's reactor takes a star through its top block, wakes all four, burns the heart and feeds the districts, a line at a port can draw it, sneak-wrench salvage gives exactly 2 Devices and ends the burn; a pylon ignores hits before the Custodian wakes, stands after 5 hits and breaks on the 6th (an arrow), and re-lights. All 262 game tests pass.
- Unit test `WastesAssetsTest`: the city blocks' blockstates, parts, textures, loot (the exporter's fork added these) and their names, tooltips, district and goggle text, advancements, chest loot and recipe.

### Decisions where the spec was silent
- The wake is checked once a second, but a Substation that reaches the charge wakes on its next tick (not up to a second later).
- A heart star feeds every district's lamps, even a `cut` one (its Substation broken): "dark for good" means its own feed is gone; the heart is the whole city's grid (P10).
- ~~Lamps of a woken district light only once it's fed~~ **Superseded by D45:** the lamps ripple on at the wake itself; upkeep keeps them lit afterwards.
- Ripple (P8): each lamp waits 1 + distance from where the power came in / 3 ticks (at most 19) + 0-2 at random, so a street lights outward from its Substation over about a second.
- ~~An unloaded Substation keeps its last fed/unfed word~~ **Superseded by D48:** it draws its upkeep through the Power Lines on paper.
- ~~A re-lit pylon breaks after 2 more hits~~ **Superseded by D43:** a re-lit pylon needs the full 6 again. Only players' hits count (a stray skeleton arrow doesn't); Deployers and other fake players never.
- ~~Placing a Star Reactor: its terminal faces the block it was placed against~~ **Superseded by D47:** its front faces the placer (six ways), the terminal on the far side.
- ~~The vault's screen is the Upgraded Chest's (copper material band, 3 rows)~~ **Superseded by D46/D59:** its own screen with the void-alloy band (see "The approved previews in the mod").
- Breaking a multiblock anywhere drops once, from its controller (Substation's lower half, the door's controller, the reactor's centre).
- Player-placed (creative) Derelict Star Reactor places all 27 blocks; it's fixed (unbreakable) until salvaged.

### Deferred / not built (as of phase 2; see "The user's decisions D39-D58" for what's since been built)
- ~~Lifts~~, ~~leaning street lamps~~, ~~cable on walls and ceilings~~, ~~the Seamed Plate's and Alcove's glow~~, ~~a Sable test for the Star Reactor~~: built (D39-D41).
- **Spans between towers and the wake bead: still not built**, waiting on the user's choices (below).
- Pylon arcs to the Custodian: built in phase 3 (the Custodian's renderer).
- Ponder scenes "Waking a district" and "The Star Reactor" (P27, P56).
- Sounds: the clunk, lamps rippling, the reactor hum (last, on the staging ladder).

### API for phase 4 (bots)
- `CityRecord.listen(...)`: `districtWoke` (stir that district's Sentries and Scuttlers within 5 s), `fedChanged` (Sentries fold while their district is unfed), `heartWoke` (wake the Custodian, then `record.wakeCustodian(level, origin)` so pylons can be harmed), `custodianDied`.
- Pylons: `ConduitPylonBlock.pylonAt(level, pos)` → `ConduitPylonBlockEntity`: `isStanding()`, `hit(entity)`, `relight()` (overload, P36), `reset()` (fight reset re-lights all, P39), `arcPoint()`, `isFailing()`. The fight finds its four pylons itself (e.g. the city's builder records them, or a scan of the heart hall). Death: `record.custodianDied(level, origin)` on any death opens the heart vault; a reset: `record.custodianReset`.
- Peaceful: bots never wake (P22); `vaultOpen` already handles the heart vault on Peaceful.

## Phase 3: the bots (built 2026-10-29; choices below wait for the user)

All in `content/wastes/bot/`, registered in `registry/ModWastes.java` (entity types through `ModEntities.ENTITY_TYPES`, eggs through `ModItems.ITEMS`, attributes by `ModWastes.registerAttributes` on the mod bus, the record listener and the griefing rule in `registerEvents`). Client in `client/wastes/bot/`, registered in `ComedyMonkeysCreateAddonsClient` (renderers, three layer definitions).

| id | class | what it is |
|---|---|---|
| `scuttler` | `Scuttler` | 0.75 x 0.5 (the brief's 12 px across its legs, 8 tall), 12 health, 5 a bite, speed 0.35, spider climbing (`WallClimberNavigation`, synced `CLIMBING`), `LeapAtTargetGoal` (0.4) playing `leap`, melee, targets players (`NearestAttackableTargetGoal`, needs sight), 5 XP |
| `sentry` | `Sentry` | 1 x 1, 30 health, anchored to a block face (`mount(wall, face)`: UP = floor, a side = that wall; snapped to its block every tick; no move, push, knockback or fall), its own targeting (nearest fightable player within 24 in line of sight from its head, not behind its own wall), fires a `starlight_bolt` (7) every 40 ticks at the fire clip's 0.7 s, 10 XP |
| `custodian` | `Custodian` | 1.4 x 3, 400 health, knockback 1, fire-immune type, speed 0.18 (+30% overloaded), boss bar (yellow = P20's brass, within 32 blocks, while awake), 100 XP |
| `starlight_bolt` | `StarlightBolt` | a `ThrowableProjectile`, no gravity, 1.5 blocks a tick, gone after 24 blocks; hurts the first living thing that isn't a city bot (`mobProjectile`), never touches a block |
| `scuttler_spawn_egg`, `sentry_spawn_egg`, `custodian_spawn_egg` | `DeferredSpawnEggItem` | in the creative tab after the Wastes blocks |

### Shared (`CityBot`)
- Every bot keeps a `CityLink` (origin + district, saved as the blocks do: `CityOrigin`, `District`), a synced `DORMANT` flag, and plays clips the Juggernaut's way (synced `CLIP`, `CLIP_START`, `CLIP_SPEED`, `CLIP_OFFSET`; speed 0 holds a clip still: the dormant poses are the first frame of `wake` / `unfold` / `waking`).
- Dormant = `isImmobile()`: vanilla skips all AI. Persistent, `removeWhenFarAway` false, `shouldDespawnInPeaceful` false (they stay, asleep). No spawn placements or biome spawns anywhere: they never spawn naturally (P6). Mob griefing off (`CityBot.onMobGriefing`).
- Waking (P8): `WastesBots` is a `CityRecord.Listener`. `districtWoke` gives every loaded Scuttler/Sentry of that city and district `wakeIn(random 0-99 ticks)`; `fedChanged` folds/unfolds that district's Sentries; `heartWoke` wakes that city's Custodians. A bot that wasn't loaded catches up on its first tick (`catchUp`: district awake -> wakes within 5 s; a Sentry reads `fed`). No chunk is ever loaded.
- Peaceful (P22, P41): `wake()` does nothing on Peaceful (or for a city in `CityRecord.PEACEFUL_FOR_TESTS`).
- Glow: the exporter copies every pixel of each glowing cube's box-UV net (names `*_lit`, `*_glow` and anything under a `*_glow` bone, `wrist_seam_*`, `pectoral*`, `studs_*`, `spring_seam`, `petal_N_seam`; `bore_glow`) into `<bot>_glow.png`, drawn full bright by `GlowModel` (the Juggernaut's layer). Lenses go dark by their clips' scale keys.

### Scuttler
- Dormant: `wake` held at 0 (curled, lens dark). Wakes: plays `wake` (stands still 44 ticks while it unrolls). Woken: `walk` while moving, `idle` standing, `leap` when the leap goal fires, `sees_player` (a 12-tick freeze) the first time it targets a player, `wipe_lens` now and then standing with no target (every 4-8 s, the brief's mannerism timing).
- Fight Scuttlers (P35): `makeFightScuttler(custodian)`, saved (`Fight`, `Custodian`); `dropAllDeathLoot` skipped, so no items and no XP; `crumble()` (smoke, discarded) when the Custodian dies or resets.
- Loot `loot_table/entities/scuttler.json`: 0-2 Clockwork Scrap (uniform), on any death as vanilla mobs' common drops; XP only for a player's kill (vanilla).

### Sentry
- Dormant: `unfold` held at 0 (the closed bud). Wakes: `unfold`, then `watching` with no target. Target: `fire` held at 0 between shots (open, lenses lit), `fire` played for each shot. Unfed (P9): `fold`, `FOLDED` synced, immobile (no aim, no fire); fed again: `unfold`.
- Aim: the server turns synced `AIM_YAW` / `AIM_PITCH` (its own frame, from `Sentry.frame(face)` = `Direction.getRotation()`) at 6 degrees a tick (120/s), pitch clamped +-60; fires when within 10 degrees. Clients follow with an underdamped spring (`springAim`, about 2.5 Hz: a few degrees past, then settled) added onto the `yaw` and `pitch` bones over the clip.
- Renderer: turned by `face.getRotation()` about its block's centre, so the mount lies on the wall; no death flip.

### The Custodian
- Phases: DORMANT (kneeling, `waking` at 0) -> WAKING (`waking`, 4.5 s) -> BOWING (`bow` to the nearest player) -> FIGHT / MOVE -> RETURNING / KNEELING (reset) or DYING.
- Waking: `heartWoke` (listener) or `catchUp` (the heart woke while it was unloaded and it never has woken). On waking: toughness 1 + 0.5 per extra fightable player within 32 (every hit divided by it, as the Juggernaut), `record.wakeCustodian` (pylons harmable), and it finds its pylons and the heart vault if the builder didn't set them (block entities within 16 blocks of its dais bound to its city's heart).
- Shield (P34): shielded while any of its pylons `isStanding()` (polled every tick, four lookups); a player's hit then does nothing but ripple the shell (synced `SHIELD_HIT`); it mends 2 a second per standing pylon. Nothing but a player (their projectile, their pet) hurts it at all; /kill and the void do (`BYPASSES_INVULNERABILITY`). Falls, fire, drowning, freezing, cramming, walls, non-player explosions: immune.
- A pylon broken (P35): the pylon now remembers its last hitter (`ConduitPylonBlockEntity.lastHitter()`, not saved) and awards **Short Circuit** to a real player who breaks it. The Custodian targets the breaker, plays `wrath` when it's free, and calls 2 fight Scuttlers (8 a fight) at the broken pylon's foot. The last one down: sparks and `shield_down`.
- Moves (P37), damage at the clip's times: `slam` 20 at 1.15 s to players within 2 blocks of its front, 60 degrees either side; `shockwave` 12 at 0.8 s to players on the ground within 8 (+ half its width), thrown up and out; `bolt_volley` 3 bolts of 8 at 0.9, 1.1, 1.3 s from its right palm; `chain_arc` (overloaded only) 10 at 0.8 s to the nearest player, then 10 to one more within 6 of them (spark lines). All only hit fightable players; never a block.
- Overload (P36): once, the first time a hit leaves it at or under half health: `OVERLOADED` synced, +30% walk (modifier `comedymonkeysaddons:custodian_overload`), clips at 1.3x, sparks every other tick, one broken pylon `relight()`ed, `overload` clip.
- Leash (P38): it never walks to a point more than 15 blocks from its dais (targets beyond are walked toward up to the leash).
- Reset (P39): no fightable player within 48 for 600 ticks, or every player who fought it dead (seen `isDeadOrDying`) -> RETURNING (walks to the dais) -> KNEELING (`kneel`; pylon k `reset()` at 0.6 + 0.5k s) -> full health, fight Scuttlers crumble, `record.custodianReset`, dormant. The vault stays locked.
- Death (D35, P40): a player's last hit leaves it at 1 health and plays `death` (turned so the heart vault is behind it: the clip turns it round to kneel to it), Scuttlers crumble; at the clip's end it dies with that hit's source. `die()` (any death, /kill too) calls `record.custodianDied` (the heart vault opens) and gives **Custodian Down** to fightable players within 32 when a player killed it. `dropAllDeathLoot` only for a player's kill (loot and XP); the player's credit is kept through the 6.5 s clip. Loot `loot_table/entities/custodian.json`: 1 Unknown Device (`killed_by_player`).
- Renderer: no death flip; while awake and shielded, a translucent starlight shell (`RenderType.lightning`, ellipsoid 2 x 3.5 blocks, rippling for 0.5 s after a hit) and a jagged arc from each standing pylon's `arcPoint()` to one of its four back sockets (rest-pose positions, `Custodian.socket(k)`), re-jagged every other tick.

### New clips (`clockwork-wastes-bots.gen.js`, `custodianFightClips()`)
`walk` (1.6 s loop), `shockwave` (2.4 s), `bolt_volley` (2.6 s), `chain_arc` (2.2 s), `overload` (2.8 s), `wrath` (2.0 s), `shield_down` (2.6 s), `kneel` (3.0 s, hold), each from the brief's Motion Set; written into `clockwork-wastes-custodian.bbmodel` (**not yet previewed with the user**). Self-collision: all eight clean. Leftovers in the approved clips, unchanged: Scuttler `leap` (lens in its housing 1.4 px, knuckles under the plates 0.4), `wipe_lens` (shin through the housing 0.5); Custodian `slam` (robe plate 2 against the left forearm and shin 0.5) and `death` (the same plate, 0.5).

### Game export
`node design/models/clockwork-wastes-bots.game-export.js [scuttler|sentry|custodian|all]` writes `textures/entity/<bot>.png` and `_glow.png`, `<bot>/geometry.json`, `<bot>/clips.json` (`idle`, `walk`, `watching` loop; the previews' hold clips hold; the rest play once). Sound cues come from `design/sound/clockwork-wastes-bots/<bot>.cues.json` once it exists. Re-run it after the generator.

### Dev tool
`/wastes testheart`: a 25 x 25 heart hall round you (void-alloy floor and walls, a doorway south), the Custodian kneeling on a 3 x 3 dais facing the doorway, a pylon in each corner, the heart vault behind it; outside the doorway a Foundry Substation, four dormant Scuttlers on Scrap Heaps, a floor Sentry and a wall Sentry. `/wastes custodian wake|die|reset` now act on the loaded Custodian too (wake: also re-wakes one kneeling after a reset).

### Sounds (hook points, none yet)
Comments `// sound: ...` mark each: the Scuttler's unroll, chitter on seeing a player, leap, crumble; the Sentry's unfold, fold, lenses turning, the gathering bore, the bolt and its hit; the Custodian's waking, each move's wind-up and impact, the shield's ring and crack, the overload howl, vent bursts, kneeling, death. Clip cues play through `CityBot.tick` once a cue file exists.

### For phase 4 (the city structure): placing the bots
- Call `CityRecord.add(origin)` first, then per district piece: `WastesBots.placeScuttler(level, Vec3 at, origin, district)` (3-5 a pack, curled in scrap heaps and alcoves), `WastesBots.placeSentry(level, BlockPos wall, Direction face, origin, district)` (UP for a floor, a side for a wall; 2-4 a district), and in the heart hall `WastesBots.placeCustodian(level, BlockPos dais, float yaw, origin, List<BlockPos> pylons, @Nullable BlockPos vault)` (pylons = the bottom blocks; face it away from the vault). All come out dormant and bound; bots of a district that's already awake wake within 5 s of loading.
- From a template instead: an entity with `{Dormant:1b, CityOrigin:{X,Y,Z}, District:"foundry"}` (Sentry also `Face:"up"`, `Anchor:<long>`; Custodian `Dais`, `DaisYaw`, `Pylons:[longs]`, `Vault`) works the same, but the origin has to be rewritten to the placed city's.
- Entity ids: `comedymonkeysaddons:scuttler`, `sentry`, `custodian`, `starlight_bolt`. P52: about 20 bots a district.

### Tests
`gametest/WastesBotGameTests.java` (11): district bots wake within 5 s of their district (not another's), an unfed district's Sentry folds; a Scuttler loaded after its district woke wakes; on Peaceful nothing wakes (Scuttler, Sentry, the Custodian when the heart wakes); a Sentry unfed doesn't fire at a player in sight, fed fires (2+ bolts in 9 s), unfed again stops and never leaves its mount; the Custodian takes 0 with pylons standing (also with all four cracked and with one left), mends 8 a second with four, takes a full 10 with all four down, and calls exactly 8 fight Scuttlers for four pylons; at half health it overloads, exactly one pylon re-lights, broken again and hit lower no second re-light; reset re-lights all four, mends fully, its Scuttlers crumble, the record says asleep and the heart vault stays locked; /kill opens the heart vault and drops nothing; a player's kill plays the death clip (vault still shut 3 s in), then drops exactly 1 Unknown Device and experience; a fight Scuttler killed by a player drops no items or XP and the other crumbles with the Custodian; no bot can grief. Unit test `src/test/.../content/wastes/bot/WastesBotAssetsTest.java` (5): every clip the code plays is exported with the right mode, every move's hit times fall inside its clip (the volley's 0.2 s apart), the bones the code poses exist, textures and glows exist, names, eggs, advancements and loot exist. Game tests: all 273 pass; unit tests: all 934 pass.

### Choices needing the user's agreement
**Answered by the user's D51-D58 (2026-10-29) and built: see "The user's decisions D39-D58" below** (1 = D51, 2 = D52, 3 = D53, 4 = D54, 5 and 6 = D55 (another agent's clip and tether pass), 7-12 = D57, 13 = D57 vents (another agent's), 15 and 12 (stuck) = D56, 16 and 17 = D58). Kept for the record:

The user's rule (2026-10-29): nothing the spec and briefs don't say is decided without them. Where code had to run it holds a provisional value (marked **in code**); where a behaviour could be left out it was (**not built**).

1. **Re-waking after a reset (not built; matters most).** The heart wakes once, so after a reset the Custodian kneels for good and the heart vault can never open (only `/wastes custodian wake` stirs it). Options: a player within ~16 blocks in sight of the dais wakes it (waking + bow, like the Juggernaut's stirring); a player entering the hall; a hit. Recommend: a player within 16 in sight.
2. **Dormant bots can be hurt (not built either way).** Asleep, they take damage as any mob and a hit doesn't wake them, so a district's bots can be killed before it's woken. Options: untouchable until their district wakes; a player's hit wakes that bot; as now. Recommend: untouchable while dormant (keeps "lighting the city has a cost", D11).
3. **Loot numbers the spec doesn't give (not built).** The Sentry drops only its 10 XP (no scrap count, no Electron Tube chance given); the Custodian drops 1 Unknown Device and 100 XP but no scrap (no count given). Recommend: Sentry 1-2 scrap + 15% Electron Tube (Tube on a player's kill), Custodian 4-8 scrap on a player's kill.
4. **The heart's lamps surging on overload (P36; not built).** Recommend: the heart's lamps flicker bright for ~3 s through a new record event.
5. **Physical cables (brief: copper catenaries from its back sockets to the pylons; not built).** Only P32's arcs while shielded are drawn. Recommend: build the cables after a Blockbench look preview.
6. **Expressions and clips the brief describes that no rig clip exists for (not built):** the wrath's ember lenses (needs a texture swap), the Custodian's four mannerisms and shield-hit "plate rattle"; the Scuttler's other five mannerisms, climbing pose, flipped-on-its-back hit reaction, packmate grief, "bursts out of a wall vent tumbling", and its death (vanilla tip-over now); the Sentry's sweep/listen mannerisms, hit flinch and death (it just dies now). Recommend: a Blockbench clip pass for these, previewed first.
7. **New Custodian clips' timings where the brief gives none (in code; not yet previewed with the user):** walk 1.6 s a cycle; overload 2.8 s; wrath 2.0 s; shield_down 2.6 s; kneel 3.0 s (pylons re-light at 0.6 + 0.5k s); volley's raise 0.4 s before the studs' 0.5 s; slam lands at 1.15 s, shockwave stamp at 0.8 s, chain arc at 0.8 s. Recommend: preview in Blockbench, then tune.
8. **Speeds (in code):** Scuttler 0.35 ("fast"); Custodian walk 0.18 (+30% overloaded); bolt 1.5 blocks a tick. Recommend: keep, tune in play.
9. **The Custodian's move choice (in code):** slam when the target is within 2 blocks of its front (60 degrees either side); shockwave when the target is on the ground within 8; volley when in sight within 24; chain arc (overloaded) one time in three when in sight within 24; 1.5 s between moves; turns 8 degrees a tick; targets the nearest player within 32. Recommend: keep, tune.
10. **Hit shapes (in code):** the shockwave is instant (everyone on the ground within 8 at the stamp, as the Juggernaut's stomp) rather than a ring running outward; the chain arc's jump also does 10; moves hit players only (not pets or other mobs); bolts hit any living thing except the city's bots. Recommend: keep (or an outward-running ring for timed jumps).
11. **Overload details (in code):** the first hit that leaves it at or under half triggers it (the hit isn't stopped at half); a random broken pylon re-lights; it interrupts the current move (a pylon's wrath and shield-down wait for the move to end). Recommend: keep.
12. **A reset is a new fight (in code):** the 8-Scuttler count and the overload come back after a reset. While walking home it follows the shield rules (unshielded if the pylons are down until the kneel re-lights them); a blocked path is retried, never teleported. Recommend: keep; add a teleport home if stuck 10 s.
13. **Fight Scuttlers' entrance (in code):** "out of the walls": the bot doesn't know the hall's walls, so they appear (with smoke) 1.2 blocks round the broken pylon's foot. Recommend: phase 4 marks wall vents and they come from the nearest.
14. **Without a city (spawn eggs; in code):** a Scuttler is awake; a Sentry is awake, mounted on the floor, and runs as fed; a Custodian kneels and wakes at once, unshielded (no pylons). A Custodian with no pylons can be hurt even kneeling. Recommend: keep (dev only); kneeling untouchable.
15. **Peaceful after waking (not built):** awake bots stay awake (vanilla makes mob damage 0 on Peaceful). Recommend: they go dormant again, as the Juggernaut does.
16. **Sizes (in code, from the brief's pixel sizes):** Scuttler 0.75 x 0.5, Sentry 1 x 1, Custodian 1.4 x 3. Recommend: keep.
17. **Small ones (in code):** Sentry fires when its aim is within 10 degrees; glowing seams (the Sentry's petals, the Custodian's wrists and sigil, the Scuttler's spring seam) stay lit while dormant (only the lenses go dark); shell and arcs hidden while it kneels; boss bar yellow (P20's brass), plain; Scuttler scrap drops on any death like vanilla common drops (XP only for a player); "every fighter dead" = every player who hit it or it targeted seen dead; advancements Short Circuit (parent City of Lights, task, pylon icon, the breaker gets it) and Custodian Down (parent Short Circuit, challenge, Unknown Device icon, fightable players within 32 at a player's kill) and their descriptions; egg colours; toughness +50% as a constant, not a config. Recommend: keep.

## The user's decisions D39-D58 (built 2026-10-29)

The user's decisions of 2026-10-29 (spec, D39-D58) override earlier choices. Everything they settle is built; what they don't is under "Choices needing the user's agreement" at the end of this section (nothing there was decided silently: each is either not built or built provisionally, as marked).

### Locks after waking (D42)
- `CityLocks.Lockable` (`locked(state)`): a block's hardness reads as bedrock's (-1) only while its state is locked, through `mixin/BlockStateBaseMixin` (`BlockStateBase.getDestroySpeed`; block hardness is one number per block in vanilla). The Substation, City Vault, Sliding Door (and the Lift and call plate) are Lockable, registered with their own hardness (Substation 5, vault 6, door 6; `ModWastes.lockable(...)`) and no `PushReaction.BLOCK`: locked, Create's drills, saws, contraptions and pistons pass them by; woken, drills break them and contraptions move them. Blasts still ask `CityLocks.resistance`. Pylons and the heart's reactor keep -1 in their properties. Carry On still blacklists them (choice below).

### Pylons (D43)
- `relight()` sets hits to 0: a re-lit pylon needs the full 6 again (`RELIT_HITS` is gone). It keeps the failing look (fins snapped, core flickering) until it breaks again. At most one hit counts per 8 ticks (unchanged, now tested).

### The heart, lamp timing and the overload (D44, D45, D54)
- D44 was already so (`CityRecord.fed`: a burning heart feeds every awake district, `cut` or not); now tested.
- D45: a Substation that wakes here tells the record it's fed before the wake (`setFed` then `wake`), so the lamps ripple on from it at the wake, with the lever; its first upkeep falls due a second later (`upkeepFrom`), and with none coming they flicker out then.
- D54: `CityRecord.surgeHeart(level, origin)` (the Custodian calls it at the overload) tells every loaded heart member `surged()` (`CityLink.Member`, default nothing). `DerelictLightBlockEntity.surged()` stamps `surgeAt` (synced, not saved): for `SURGE_TICKS` (60, about 3 s) the lamp, lit or dark, is drawn with a hard flicker settling as it goes (`WastesRenderers.surgeOn`), then as it was. A look only: light level and phase don't change.

### Star Reactor (D47) and its ship test
- Placed, its `FACING` (the copper terminal) is `getNearestLookingDirection()`: its front faces the placer, any of six ways; nothing else changed (models, capability side).
- `SableGameTests.starReactorRunsOnAShip`: fuelled, made a Sable physics object with `SableAssembly`, it keeps its star and burns exactly 20 ticks and makes exactly 20 x 8,192 FE in 20 ticks there.

### Unloaded upkeep on paper (D48, P24)
The consumer side of the Power Lines' paper records:
- `PowerGrid.ConsumerPaper` (saved in the grid's records, `Consumers`): energy the network sent it while away, its cap and draw a tick. `noteConsumer(pos, perTick, cap)` (a running Substation, once a second; kept only while a line meets it), `consumerBack(pos)` (back: takes what's there, once), `forgetConsumer(pos)` (broken), `consumer(pos)`.
- `PowerLines`: a running network that meets an away consumer with a record adds it as a taker (its room), delivering into the record. `PowerLines.drawAway(device, need)`: a second's worth, from the record, topped up (when no network meeting it has run in the last second) straight from the records of the away generators (and their rows) and paper stores the lines reach, through Breakers and Light Switches only while on; returns `need` and spends it if all there, else 0 (what came stays toward the next). Terminal wires a game test marks away are skipped (test hook; `PowerGrid.AWAY` is empty in play).
- `CityRecord`: each woken Substation says where it stands (`substationAt`, saved; cleared when it's cut); once a second `onLevelTick` pays the upkeep of every awake, uncut district whose Substation isn't running through `drawAway` and sets it fed or unfed. The Substation takes on what its record holds when it runs again. Nothing is ever loaded.

### Leaning street lamps (D41)
- `DerelictStreetLampBlock.LEANING`: the post leans 22.5 degrees away from its crook about its foot (the preview's `lamp_leaning`). A block stacked on a leaning one leans with it; only the city (or a debug stick) makes a foot lean. Shapes follow the shaft out as it leans (the head's slice is all over the next block south, so its outline is there).
- Exporter: `derelict_street_lamp_{foot,pole,head,head_snapped}_leaning`, every part turned 22.5 about x through the post's foot (`compose`, so the snapped crook's -45 and the lean make one -22.5 turn); the foot is the chipped one, as the preview's; the glow part `derelict_street_lamp_glow_leaning` (the renderer picks it). Blockstate: `leaning` on every variant.

### Derelict Cable on walls and ceilings
- `DerelictCableBlock.SURFACE` (the side of its block it lies on: down floor, up ceiling, a wall's side; placed on the face clicked) and `up`/`down` arms. Arms run along its side toward cable on the same side (on a floor also Substation and lamp feet, as before); one join runs straight on; alone it runs level. It falls if its surface goes. Lights with its district as before.
- Exporter: a multipart over every surface and arm, the floor's arm/knot/line models turned by the blockstate (found by search: a blockstate turns x before y, so `derelict_cable_arm_south` and `derelict_cable_arm_glow_south`, the arm quarter-turned, give the arms up and down a wall).

### Seamed Void-Alloy Plate and Charging Alcove (lit with their district)
- `SeamedPlateBlock` and `ChargingAlcoveBlock` (a `ThreePartBlock`) have `lit` and a `DerelictLightBlockEntity` (the type's block list now includes them), so they light, ripple and flicker out with their district like the cable; `CityLink.bindAll` binds them. Their lit models (phase 1's `seamed_void_alloy_plate_seams`, `charging_alcove_contact_<part>`) are now emissive and drawn by multipart blockstates when `lit`. They cast no light; a player's (unbound) stays dark; they still drop themselves.

### Dev tool
`/wastes testsetup` now also builds a leaning post, cable up the wall's east face and a Charging Alcove (all Foundry). `/wastes testlift` builds a lift shaft (below).

### Bots (D51-D53, D56, D58; built in an isolated worktree and merged)
D51-D58 (2026-10-29): bots asleep are untouchable (only /kill and the void), and so is the Custodian kneeling, asleep or at the end of a reset; a hit never wakes them. After a reset the Custodian wakes again (rise, bow) for a fightable player within 16 blocks in its sight (`REWAKE_RANGE`), if its heart is awake and it isn't dead. Peaceful sends woken bots to sleep (`CityBot.sleepForPeace`, saved `PeaceSlept`): Scuttlers and Sentries go dormant and wake within 5 s when the difficulty rises; fight Scuttlers crumble; the Custodian resets (walks home, kneels, pylons re-lit, mended) and then wakes by the D51 rule. Walking home, a Custodian that gets no nearer for 10 s (`STUCK_TICKS`) appears on its dais in smoke. The shockwave is now a starlight ring running out 8 blocks along the floor from the clip's stamp at 0.8 s, at `RING_SPEED` 0.5 blocks a tick (provisional); its crest hits each player standing on that floor once, for 12, and misses anyone in the air; the renderer draws the crest in `#b8b0d8` and a trailing `#7f76a8` band. At the overload, `CityRecord.surgeHeart` makes the heart's lamps surge. Loot: Sentry 1-2 scrap and a 15% Electron Tube, Custodian 4-8 scrap besides its Unknown Device, both on a player's kill only (`loot_table/entities/sentry.json`, `custodian.json`). Checked: yellow boss bar, hitboxes, 10 degree aim, seams lit asleep, vault on the death clip's end (at once on /kill), toughness fixed. New APIs: `CityBot.STAND_INS_FOR_TESTS`, `sleepForPeace()`/`wakeAfterPeace()`/`sleptForPeace()`, `Custodian.REWAKE_RANGE`, `STUCK_TICKS`, `RING_SPEED`, `ringRadius(age)`, `ringEnd()`, `ringAge(partial)`, `ringCentre()`, test hooks `testShockwave()`, `bossBarColor()`; `Sentry.AIMED` public.

### Lifts (P49, D40; built in an isolated worktree and merged)
- Mechanism: a custom moving-platform entity, `LiftDeck` (`lift_deck`, never saved; the drive spawns and owns it), not a Create contraption: Create's collider leaves server-side players to their client (rider carrying untestable, a custom ContraptionType per deck). The drive block entity holds the motion as a function of game time and syncs it, so server, client deck and renderer agree with no position packets. Deck slab, guard rails and control post are solid (rails and post are NeoForge `PartEntity`s with exact boxes; the post can be clicked). The client carries its own player; the server carries mobs, items and mock players; a rider's floating counter is reset as Create does (`mixin/ServerGamePacketListenerImplAccessor`). Each server tick it checks for a block in its path, a rider's head that would hit a block, or anything under it going down, and stops where it is. Flywheel draws everything through the drive's parts visual (`client/wastes/LiftRenderers`).
- Blocks: `lift` (`LiftBlock` + `LiftBlockEntity`): the drive at the head of a 2 x 2 shaft, in one corner cell; the shaft runs toward its facing's clockwise side and away from its front, open sides front and back. `lift_call_plate` (`LiftCallPlateBlock` + BE): a wall block marking a landing at its level, within 4 blocks of the shaft (`PLATE_REACH`). The shaft's floor is the first solid level under the deck; the deck stops with its underside level with each landing's floor. Both Lockable and dark until their district wakes (D24), then ordinary blocks (D42) with seams, lenses and pips lit. A player on the deck uses the post: up to the next landing, sneak: down. A player at a plate calls the deck to that level. About 1 block/s eased, a 0.6 px clamp settle over the last 0.4 s of a ride up.
- APIs: `LiftBlockEntity` `deckY(time)`, `rideTo(y)`, `usePost(player)`, `call(player, level)`, `landings()`, `serving(level, platePos)`, `shaft()`, `bottom()`, `isAwake()`, `isMoving()`, `target()`, `deck()`; constants `SPEED`, `PLATE_REACH`, `MAX_SHAFT` (64), `DECK`, `SETTLE_*`. `LiftDeck.carries(e)`, `top()`, `keepAloft(ServerPlayer)`. `LiftBlock.footprint(pos, facing)`. City placement: the Lift at the head of the shaft, call plates at the landings, then `CityLink.bind` both; the deck spawns itself; a template can carry `DeckY`.
- Exporter: a marked "the lifts" section; everything cut per block cell and split west/east (and north/south for the head) within the model bounds: `lift_deck_w/e`, `lift_deck_glow_w/e`, `lift_head_nw/ne/sw/se`, `lift_guides_w/e`, `lift_guide_seams_w/e`, `lift_call_plate_lens`; textures `lift_sigil`, `lift_sigil_lit`; loot (Lift 2-3 scrap, plate 1: `LIFT_SCRAP`). `clockwork-wastes-lift.gen.js` exports its builders and writes its .bbmodel only when run directly (identical apart from uuids). Rebuild notes: the call plate's back is on the wall and its sigil faces out (the preview had it facing the wall); the deck texture tiles at 16 px a block (the preview stretched it); the preview's 4 px pit and collar are dropped (in game the deck rests on the floor, its beams hidden in the floor block).
- Dev tool: `/wastes testlift`: a 13-tall shaft with landings at 0/5/10 and plates on a wall, the Rail Yard of a city at your feet.
- Not seen in a client yet (riding, clicking the post).

### Tests
- Game tests (`WastesCityGameTests`, 14 new): woken vault/Substation/door are ordinary to drills and contraptions, locked ones not (D42); a heart star lights a district whose Substation was broken (D44); lamps ripple on at the wake and flicker out with no upkeep (D45); the Star Reactor faces its placer for all six looks (D47); the overload surges heart lamps only, 3 s, light unchanged (D54); an away Substation is fed through a running line and goes unfed when its generator is gone, is fed from a generator's record with the whole line away, and goes unfed with nothing on the line (D48, 3 tests); cable on walls and ceilings (placement, joins, level alone, falls with its wall, lights); a leaning post stacks, has shapes and lights (D41); seams and alcove light and go dark with their district, an unbound plate never lights; the pylon test also checks a re-lit pylon needs all 6 again and two swings inside 8 ticks count once (D43). `SableGameTests.starReactorRunsOnAShip`. Bots: 8 new (`dormantBotsAreUntouchable`, `aKneelingCustodianIsUntouchable`, `custodianWakesAgainForAPlayerInSight`, `peacefulSendsWokenBotsToSleep`, `aStuckCustodianAppearsOnItsDais`, `theShockwaveRingHitsAsItPasses`, `sentryLootOnlyForAPlayersKill`, `theAgreedDetails`; `aPlayersKillDropsItsLoot` checks 4-8 scrap). Lifts (`WastesLiftGameTests`, 5): still while dormant and locked; carries a player and a pig up to the plate's landing and back, the plate calls it; stops under a block in the shaft without the rider's head going in; stops above a pig underneath; the floating-counter hook. **All 298 game tests pass.**
- Unit tests: `WastesAssetsTest.cableOnEverySideAndLeaningLampsHaveModels` (each surface and arm draws exactly one arm and one line, none into the surface; one street-lamp variant for every state; the new models' textures), multipart blockstates read by `everyBlockstateModelAndTextureExists`, `liftModelsNamesLootAndTags`; bots `botLootIsAsAgreed`, `shockwaveRingRunsOutWithinItsClip`. **All 938 unit tests pass.**

### Choices needing the user's agreement
Nothing below was decided silently: **not built** means it waits for the user; **built provisionally** means code had to hold some value to run, and it's one named constant or rule to change.

City and power:
1. **Sky-cable spans and the wake bead (not built).** The preview hangs sagging copper-patina spans from the Substation's tray insulators to "the tower's", with a snapped span hanging off; nothing in the mod is a tower or mast a span can tie to, and the spec doesn't say how spans are placed, whether they break or drop, or which spans a district's bead runs along. Options: (a) a new Derelict Insulator block for towers and masts (designed and previewed in Blockbench first) holding spans as a Line Post head does, drawn with the Line Posts' span renderer and the derelict cable texture, the Substation's tray as the district end; the city links them (`CityRecord`/builder API); players can't make them; a broken anchor drops its spans as scrap; the snapped variant a span state hanging from one end. (b) spans as pure looks stored in the city record, drawn between fixed points, unbreakable. (c) only Substation-to-heart spans. Recommend (a). Bead: once spans exist, a 3 px starlight bead runs along each span leaving the woken district's Substation over ~2 s, starting at the lever's slam (the preview's `pulse`).
2. **Carry On and woken blocks (built: unchanged).** D42 makes woken vaults, doors, Substations and lifts ordinary blocks, but Carry On's tag blacklists them in every state (Carry On can't be told per state without a hook). Options: keep blacklisted; let woken ones be carried (a Carry On pickup hook). Recommend keep (a carried vault takes its city link and loot with it).
3. **An away Substation with no line (built: goes unfed).** Fed only by a Battery beside it or a carried one, it now goes unfed once its chunk unloads (no line reaches it on paper); before, it kept its last word. Recommend as built (D48: upkeep comes through the lines).
4. **Paper draw limits (built).** On paper the upkeep is taken from whatever generators and stores the lines reach until it has a second's worth, ignoring line tier rates and Splitter priorities; Breakers and Light Switches pass only while on. Recommend keep (500 FE/t is far below a copper line's rate).
5. **Dark heart lamps surge too (built).** D54 says every heart lamp: a dark one flickers 3 s and settles dark again; light levels never change. Options: lit lamps only; also cast light during the surge. Recommend as built.
6. **Leaning posts (built).** The chipped foot (as the preview's leaning post); a snapped head can lean too (its crook and the lean make one turn); the head's slice lies wholly over the next block south (its own block looks empty, its outline is out there); only the city or a debug stick makes a foot lean, and blocks stacked on it follow. Recommend keep; say if players should be able to place a leaning post (e.g. sneak-place).
7. **Lit decor (built).** The Seamed Plate and Alcove cast no light (as the cable), each is now a block entity (one per plate the city places; P52), and a player's stays dark. Recommend keep.
8. **Wall and ceiling cable joins (built).** A run joins cable on the same side only (no floor-to-wall corner piece), joins Substations and lamp feet only on a floor, and runs level when alone. Recommend keep; a corner piece would need a look previewed first.
9. **A re-lit pylon's look (built).** It keeps the failing look (no fins, flickering core) for all six hits D43 now asks for. Option: fins grow back. Recommend keep.
10. **Star Reactor placement (built).** Front toward the placer, terminal behind, no sneak-to-reverse (the Magma Engine has one). Recommend keep.

Bots:
11. **Ring speed (built provisionally, `Custodian.RING_SPEED` 0.5 blocks/tick).** Spec, brief and clip give none: 0.5 (out in 0.8 s, done at 1.6 s of the 2.4 s clip), 0.35 (ends near the clip's end), 0.75 (snappier). Recommend 0.5, tuned in play.
12. **Ring look and reach (built).** Crest 0.14 blocks wide with a 0.22 lip, 0.7 blocks of edge tone behind, fading over the last 2 blocks; passes through walls; hits only on the Custodian's own floor (+-1.5 blocks); a rider is hit when their mount is on the ground. Alternative: stop at walls. Recommend keep, look checked in game.
13. **The Custodian after Peaceful (built).** District bots wake within 5 s when the difficulty rises; the Custodian wakes by D51 (a player within 16 in sight), not at once. Alternative: at once if a player is within 32. Recommend as built.
14. **"Kneeling" for D52 (built).** Untouchable during the ~3 s kneel at the end of a reset as well as asleep. Recommend as built.
15. **Fight Scuttlers on Peaceful (built):** they crumble rather than sleep. Recommend keep.
16. **Stuck rule (built):** progress = 0.25 blocks nearer home; 10 s without it, it appears on the dais; applies to any walk home. Recommend keep.

Lifts (all built provisionally as described above):
17. **Mechanism:** custom deck entity (built) vs a Create contraption (every part collides as blocks, but heavy custom contraption code and players can't be test-carried). Recommend keep, then try the ride in a client.
18. **What a lift is made of:** one Lift block at the shaft head drawing head, rails and deck, plus call plates; the rails aren't blocks, and the frame and clamps reach 2-3 px into the columns beside the closed sides (a city shaft needs its walls one block out there). Alternatives: rail blocks; a 2 x 2 head multiblock. Recommend keep.
19. **Landings:** the shaft floor plus each call plate within 4 blocks (`PLATE_REACH`). Alternative: links set by the city builder. Recommend keep, plates beside the shaft doors.
20. **Control post with 3+ landings:** use = next up, sneak-use = next down (two landings: either goes to the other). Alternatives: cycle; a menu. Recommend keep.
21. **Blocked:** it halts where it is and waits for a new call (alternative: resume once clear); stopped by blocks in its path, a rider's head hitting a block, anything under it going down; riders half on a landing are left there. Recommend keep.
22. **Calls while moving** are ignored (alternative: queue). Recommend keep.
23. **Who can work it:** players only (not Deployers, fake players or spectators). Recommend keep for now.
24. **(Answered by D62, built: dark until pressed, lit while its call waits.)** **Call plate lens:** lit whenever its district is awake (the preview's awake clip); the ride clip instead shows the upper plate dark until pressed, lit while the call is pending. Ask which reading.
25. **Works without upkeep:** awake = works, whether fed or not (D24 groups lifts with doors and vaults); glows don't dim unfed. Recommend keep.
26. **Settle** only at the end of rides up, as the clip (alternative: every stop). Recommend keep.
27. **Drops:** Lift 2-3 scrap, call plate 1 (`LIFT_SCRAP` in the exporter). Recommend keep.
28. **Names and text:** "Lift", "Lift Call Plate", "Lift Deck", tooltips, the action bar "The lift is dark. Its district still sleeps." (P29 doesn't name lifts.)
29. **Item models:** the Lift item shows the deck (3D, fitted), the plate its model. Alternative: flat icons with a texture review (D49). Recommend: ask.
30. **Limits:** shaft at most 64 deep (`MAX_SHAFT`); Carry On blacklists both for good (as item 2).
31. **Mirroring:** the shaft always extends toward the facing's clockwise side, so a mirrored structure would put it on the wrong side; the city builder should not mirror lifts. Recommend keep.

## The approved previews in the mod (D59-D62; built 2026-10-29)

D59 approved the bots' clip pass, the Custodian's tether cables, the wall vent and the City Vault screen (option A); D60 set the look rules; D62 the call plate. All built here; what they leave open is under "Choices needing the user's agreement" at the end of this section (each is built provisionally as one named constant or rule, or not built, as marked).

### Bot clip pass (D59, D60)
- **Export:** `node design/models/clockwork-wastes-bots.gen.js all` (the approved clips, unchanged), then `node design/models/clockwork-wastes-bots.game-export.js all`. The exporter now loops `run`, `climb` and the Sentry's `aim` besides `idle`, `walk`, `watching` (hold clips hold, the rest play once), and writes `textures/entity/custodian_tether.png` (48 x 16: the cable, void alloy, the bead, taken from the heart-hall preview's own textures). Glow split checked: the new hidden layers are glow (`lens_*_ember_lit`, `lens_*_blaze_lit`, the Sentry's `lens_k_dim_lit` and `lens_k_pupil_lit`; all under the 'cube ends `_lit`' rule), the Scuttler's `lens_crack_dark` is not (a dark crack painted over the lit lens). Counts: Scuttler 20 clips, Sentry 12, Custodian 19.
- **`CityBot`:** `playIfFree(name, speed)`, `clipLocalTime()`; `deathClip()`: a bot with one plays it as it dies (no topple: the renderer's flip is 0 for every bot) and is removed, with vanilla's puff, when the clip ends (Scuttler 2.2 s, Sentry 3.2 s); a dying bot's city tick is skipped (nothing wakes or sleeps it mid-death).
- **Scuttler** (`Scuttler`): walks at `WALK_SPEED` = 0.3/0.45 (the walk clip's tripod is 0.3 s a cycle, so it plays at 2/3: 0.45 s, D60) when moving without a target, `run` (0.3 s) after a player; `climb` while pressed to a wall (synced `CLIMB_WALL`: the wall most nearly ahead; the renderer turns it square to that wall, which is then 6 px in front of its centre, as the clip assumes); `hostile` (held) standing with a target; `sees_player` on a new target (as before); `flipped` on any hit it survives, still for `FLIP_TICKS` (27, until it's righted at 1.35 s); `grief` when a packmate dies within `GRIEF_RANGE` (8): it turns so the clip's 40 degree head turn lands on the body (`GRIEF_LOOK`), still 0.55 s, facing held for the clip; `death` (lens cracks, legs curl); `vent_burst` from `burstFromVent()` (still `VENT_TICKS` = the clip's 2.4 s). Mannerisms every 4-8 s standing idle with no target: `wipe_lens`, `sniff`, `chitter`, `stretch`, `pick_scrap` (an electric spark at its front left foot at 1.0 s), and `huddle` when another idle woken Scuttler is within `HUDDLE_RANGE` (1.5): both turn to face the same way side by side, it plays `huddle_right`, the packmate `huddle_left`, each facing held for the clip. `curious` is exported but not played (choice 1).
- **Sentry** (`Sentry`): `target` when it takes a new target (the petals snap, the lenses swivel 5 degrees); it may fire from 0.3 s (`SNAPPED`); `aim` (loop) between shots instead of the held fire frame; `fire` as before; `hit` on a hit it survives unless mid-shot or unfolding; `death`; watching with a mannerism every 6-10 s (`barrel_check`, `petal_twitch`, `blink`, `listen`), started only as the watching sweep passes centre (0 or 3 s of its 6 s loop) so the yaw ring never jumps. In `target` and `unfold` the code's aim replaces the clip's own 30 degree yaw-ring turn (which previewed turning to a target); elsewhere the aim adds on as before.
- **Custodian** (`Custodian`): a pylon broken: `wrath` (head kept on the breaker), then `call_scuttlers` (new `Move.CALL`, its hit at 0.8 s: the vents burst), then `shield_down` if it was the last; a pylon broken during a wrath adds to the call (one wrath at a time); every pylon broken before the call is called for in it. Unshielded, a hit in the fight plays `hit` (its walk takes over again after `HIT_HOLD` 0.5 s; a move replaces it at once). Lulls: standing in the fight with no move to make (no target, or in reach waiting) for 6-10 s (`MANNERISM_MIN`/`SPREAD`): one of `straighten_plate`, `mourn` (needs a broken pylon: a random one), `count`, `glance_vault` (needs the vault), never the same twice running; a move or a walk interrupts it. Synced for the client: `DAIS`, `DAIS_YAW` (the cables), `LOOK_ENTITY` (the breaker), `LOOK_BLOCK` (the mourned pylon, the vault).
- **The code aims the head (D60, `BotModel.look`):** in `wrath` the head follows the breaker's eyes (snapped in over 0.08 s, held to 1.3 s, eased out by 1.7 s), up to `WRATH_YAW` 75 / `WRATH_PITCH` 40 degrees; in `mourn` and `glance_vault` the clip's turn about the spine (waist, chest, neck, head; read from the clip: its largest summed yaw) is scaled so it ends on the pylon's crown or the vault, mirrored when that's on its right, up to `LOOK_STRETCH` 1.5 x the clip's own turn.
- Sounds: `// sound:` hooks at every new clip start.

### Tether cables (D59, D60; `client/wastes/bot/CustodianTethers`)
From the heart-hall preview's own formulas (in blocks): each cable leaves its socket 1.5 px behind the plug's back face, where the posed model has that socket this frame (a layer reads it while the model's pose is on the stack: `BotRenderer.CustodianRenderer.probe`); upper sockets to the pylons behind its dais, lower to those in front, by side (`sockets(c, pylons)`, from the synced dais and its yaw); each ends at the crown plate's corner toward the dais, 40.75 px up and 4.6 px in (the sleeve), 22 segments, 2 x 2 px, the worn copper rows on the underside. Shapes: hanging (the preview's Bezier, swaying 1.6 px over 6 s); slack (back over the robe, down onto the 3 x 3 dais behind it, over its edge, along the floor, up the pylon's corner). Phases: asleep slack; waking: lifted with the preview's spring from 1.9 s of `waking`; fight, walking home: hanging; reset kneel: back to slack from 0.4 s over 1.5 s; dying: hanging and dark, unplugging at 5 s and dropping (0.45 s, a small bounce), the socket end lying on the dais. Beads: two per cable, 3 s pylon to socket, staggered as the preview, full bright, while shielded (from 3.2 s of waking; gone at 0.3 s of a reset kneel; none dying). Plugs (3.5 px void alloy at the socket end, turned with it) and sleeves (4 x 3.5 x 3.5 and a 1 px collar, turned to the dais) are drawn with them (neither is in the rig). The arcs now run to each pylon's own cable socket as posed. It's drawn while any of its pylons is in view (`shouldRender`).

### Wall vent (D57, D59, D60)
- **Block** `wall_vent` (`WallVentBlock`, `WallVentBlockEntity`, a `CityBlockEntity`): horizontal `facing` (into the hall), `state` sealed / live / open. Bound to its city's HEART; sealed until the heart wakes (all four), then live; `burst()` (live only) sets it open for good and stamps `burstAt` (synced) for the look. Provisional (choice 7): locked (bedrock hardness, contraption-proof) until spent, then hardness 5, 1 Clockwork Scrap; Carry On blacklisted; pickaxe; full-cube shape; `noOcclusion`.
- **Exporter** (`clockwork-wastes.export.js`, section "the wall vents"; checks kept, no z-fights): block model = the body and the dark contact; the hatch (louvres turned 35 degrees: a block model can't) is an OBJ part `wall_vent_hatch` baked from a rig (each louvre a bone); parts `wall_vent_seam` (lit, edge tone), `wall_vent_flash` (core tone), `wall_vent_contact` (dim), `wall_vent_glint` (the dull lens, block-lit); item = an OBJ of the shut vent. Rebuild notes: the lit contact 0.25 px in front of the dark one (the preview had them coplanar); hatch seams 0.25 / 0.5 / 0.75 px proud (dark / lit / flash; the preview's 0.05 / 0.3 / 0.4 fought). `wastes_shells.mtl` is now written after every OBJ (it missed the vent's `vent_cavity`).
- **Looks** (`WastesRenderers.vent`, Flywheel visual): sealed: hatch shut, seam dark; live: seam lit, contact dim, the glint (blinks out at 1.6-1.75 and 3.1-3.2 s of a 4 s loop, the live clip); burst (2.6 s from `burstAt`): seam flares to the flash 0.1-0.3 s, hatch rattles from 0.15 s (harder from 0.3), blows flat 0.45-0.55 s and bounces twice (`hatchAngle`), contact and seam out at 0.45, glint flickers out by 0.36; open: hatch flat on the floor in front, dark.
- **The call:** at `call_scuttlers` 0.8 s, for each pylon called for, the two live vents of its city's heart nearest that pylon within its hall (16 blocks, loaded chunks) burst, one Scuttler each; each appears `VENT_DELAY` (6 ticks: the preview starts its `vent_burst` 0.3 s into the vent's burst) later at 1.25 blocks (`VENT_LANDING`) out from the vent's face, facing out, playing `vent_burst`, after the breaker. Fewer live vents than needed: the rest appear round the pylon's foot in smoke as before (provisional, choice 2). Calls still count toward 8 a fight.
- **`/wastes testheart`:** eight vents, two per pylon, in the walls either side of its corner two blocks in from it (the hall preview's places), bound to the heart.

### City Vault screen (D46, D59 option A)
`CityVaultMenu` (an `UpgradedChestMenu` with its own menu type, `ModWastes.CITY_VAULT_MENU`; `UpgradedChestMenu` gained a protected constructor taking the type) and `client/wastes/CityVaultScreen` (the kit's 3-row chest panel and inventory panel, then the band texture `textures/gui/city_vault_band.png` (174 x 12, untinted) over the panel's band, the title in #b8b0d8). The band comes from the approved generator: `node design/gui/city-vault.gui.js --mod`. The vault opens it with one int (its rows).

### Lift call plate (D62)
`LiftCallPlateBlockEntity.isCalling()` (synced `Calling`): set when a press starts a ride (`LiftBlockEntity.call(player, landing, plate)`; the lift saves the calling plate as `Caller`), cleared when the deck arrives or halts. The lens part draws only while calling (`LiftRenderers`, its visual made again on change through `LiftCallPlateBlockEntity.clientChanged`). Supersedes choice 24 above.

### Tests
- Unit (`WastesBotAssetsTest`): every clip the code plays (now 19 / 12 / 19) with the modes the code expects; mannerism lists are once-clips; `clipTimingsMatchTheCode` (0.45 s walk, flip and vent timings, `VENT_DELAY` = 0.3 s); the new hidden layers and the spine bones exist; `lensLayersGlowAndTheCrackDoesnt`; `tetherTextureExists`. (`WastesAssetsTest`): `wallVentModelsNamesLootAndTags` (every facing and state, parts, OBJs and their materials, loot, lang, pickaxe, Carry On), `cityVaultBandExists` (174 x 12, the lit seam along its foot). **All 943 unit tests pass.**
- Game tests: `aWallVentGoesLiveWithTheHeartAndBurstsOnce` (sealed and unbreakable asleep, live with the heart, one burst, open for good, ordinary when spent, an unbound one stays sealed); `calledScuttlersBurstOutOfTheNearestVents` (nothing before the wrath has played; then exactly the two vents by the broken pylon, one Scuttler 1.25 blocks out of each playing `vent_burst`; all four pylons: all eight vents once, eight Scuttlers); `callPlateLensLightsWhileItsCallWaits` (dark awake, lit while called, dark on arrival, pressing at the deck's landing calls nothing). The shield and reset tests wait for the call (wrath, then the call) before counting Scuttlers. **All 301 game tests pass.**

### Choices needing the user's agreement
1. **The Scuttler's `curious` (not built).** The brief gives its face (aperture wide, lid up, head tilted 20) but no moment for it, so nothing plays it. Options: (a) a player it can't fight (creative, spectator) within ~8 blocks: it watches them, curious; (b) a beat before `sees_player` the first time it notices anyone; (c) a dropped item or a noise nearby. Recommend (a).
2. **No live vent left (built provisionally: the old smoke at the pylon's foot).** Each vent bursts once and stays open (D60), so after a reset (a new fight, 8 Scuttlers again, D57) every vent is spent; a broken or unloaded vent leaves fewer too. Options: (a) the vents re-seal (shut, live, a new Scuttler) when the Custodian kneels at a reset; (b) keep the smoke at the pylon's foot for whatever no vent can give; (c) no Scuttlers once the vents are spent. Recommend (a) for a reset, (b) for a vent that's gone.
3. **The order when a pylon breaks (built provisionally).** Wrath first (2 s, head on the breaker), then `call_scuttlers` (vents burst at its 0.8 s), so Scuttlers now arrive about 3 s after the break (they came at once before); a pylon broken during a wrath doesn't start another; every pylon broken before the call is called for in it. Options: call first, then wrath; the call alone. Recommend as built.
4. **A Scuttler hit is a stun (built provisionally: `FLIP_TICKS`).** Every hit it survives flips it onto its back, still and harmless until it's righted (1.35 s), so hitting faster than that keeps it there. Options: flip at most once every ~4 s (other hits just knock it back); flip only on crits or knockback; as built. Recommend once every 4 s.
5. **Hits from the left (built: the one clip).** The Sentry's and the Custodian's `hit` clips recoil on their right; a hit from either side plays them. Option: mirrored left-side clips (a short clip pass, previewed). Recommend the mirrored clips.
6. **Small Scuttler numbers (built provisionally):** packmates within 8 blocks grieve (`GRIEF_RANGE`; the brief: "nearby"); two idle Scuttlers within 1.5 blocks may huddle (`HUDDLE_RANGE`), turning to stand side by side facing the same way; a called Scuttler stays still for its whole 2.4 s `vent_burst` (its hitbox is already at its landing while its model is still in the vent for the first half second). Recommend keep.
7. **The wall vent as a block (built provisionally):** named "Wall Vent"; unspent it's locked (unbreakable, contraption-proof, so a fight can't be cheesed by breaking the vents first), spent it breaks like a Void-Alloy Plate into 1 Clockwork Scrap; Carry On can't take it; full-block collision; a player-placed one stays sealed for good; its item is the shut vent in 3D (not a flat icon). Recommend keep.
8. **The Custodian's lulls (built provisionally: `MANNERISM_MIN`/`SPREAD`).** The brief gives no interval for its mannerisms: one every 6-10 s (the Sentry's), when it has stood in the fight with no move to make for that long (no player in range, or waiting in reach); mourn looks at a random broken pylon. Recommend keep.
9. **How far the code turns its head (D60; built provisionally):** in wrath the head follows the breaker up to 75 degrees either way and 40 up or down (`WRATH_YAW`, `WRATH_PITCH`); a mourn or a glance turns its spine up to 1.5 x the clip's own turn (mourn ~96, glance ~144 degrees: `LOOK_STRETCH`), then looks as far as it can. Recommend keep.
10. **The Sentry's first shot (built provisionally: `SNAPPED`).** It may fire once its petals have snapped open (0.3 s into `target`) rather than after the whole 1.6 s clip; in `target` and `unfold` its aim replaces the clip's own 30 degree turn. Recommend keep.
11. **Beads on a broken pylon's cable (built literally).** While it's shielded, beads crawl along all four cables, a broken pylon's too. Option: only the standing pylons' cables (as the arcs). Recommend only standing pylons'.
12. **The cables after its death (built: they go with it).** They unplug and drop at 5 s of the death clip; when the clip ends (6.5 s) it's removed and the fallen cables and the sleeves go with it. Option: leave them lying in the hall (would need a block, previewed). Recommend as built.
13. **Cable fit to the hall (built; for the city phase):** the slack lies over a 3 x 3, one-block dais (the preview's; the city's heart hall should build that); the cable ends at the crown plate's corner (the preview's sleeve), 3.25 px below the arcs' point; a broken pylon's crown slumps ~2 px away from its sleeve. Recommend keep, and the city builds the preview's dais.
14. **Call plate (D62; built):** only the plate that started the ride lights; a ride sent by the post lights none; a deck halted on its way puts the plate out (the call is given up). Recommend keep.

## D67 follow-ups: bots, vents, tethers (built 2026-10-29)

D67 settled choices 1, 2, 4, 5 and 11 above. Built here except the left-side hit clips, which are previewed only (the user approves them first). What it leaves open is under "Choices needing the user's agreement" at the end of this section.

### Scuttler `curious` (choice 1)
`Scuttler`: standing idle with no target, a player it can't fight within `CURIOUS_RANGE` (8) and in its sight (creative or spectating, a real player, not a Deployer's) makes it curious: it plays `curious` and turns to face them, following them as they move, for `CURIOUS_TICKS` (60, 3 s: the clip springs into its pose and holds it); then `idle`, and not curious again for `CURIOUS_COOLDOWN` (400, 20 s). A target, a run, a climb or a hit ends it at once (the cooldown starts then too). Provisional: the two lengths and the sight rule (choice 1 below). `CityBot.realPlayer(p)` is the new shared check.

### Vents re-seal at a reset (choice 2)
`WallVentBlockEntity.reseal()`: an open vent goes back to sealed (`burstAt` cleared, so no burst look) and at once live again while its heart is awake, so it can burst in the next fight. `Custodian.resealVents()` re-seals every spent vent of its city's heart in its hall (loaded chunks), called in `finishReset` (the end of the reset's kneel, beside the Scuttlers crumbling; a Peaceful reset ends there too). The smoke at the pylon's foot is now only for Scuttlers no live vent can give: a vent broken (spent vents can be mined) or unloaded. A vent unloaded at the reset stays open until a later reset finds it loaded (choice 2 below).

### Scuttler hit-stun (choice 4)
`Scuttler.FLIP_EVERY` (80, 4 s): a hit flips it only if 4 s have passed since its last flip (flip to flip); hits inside the window still do their damage and vanilla's knockback, with no clip change (a hit while it's on its back no longer restarts the flip). `lastFlip()` for the tests.

### Tether beads (choice 11)
`CustodianTethers.beads(phase, clip, t, shielded, pylonStanding)`: the old rule (shielded; from 3.2 s of waking; the first 0.3 s of a reset's kneel; never dying) and now only on a standing pylon's cable, read from the pylon block entity on the client as the arcs do.

### Left-side hits (choice 5): previewed, not in the game
- **Clips** (`clockwork-wastes-bots.gen.js`): `mirrored(T, twin)` mirrors a clip left for right: each bone's tracks go to its twin (`*_left` <-> `*_right`; the Sentry's petals 0-3, 1-2 and lenses 2-6, 3-5; the Custodian's robe plates 1-4, 2-3, 5-8, 6-7 and sockets 1-2, 3-4), turns about Y and Z and moves along X change sign; the timing, keys and springs are the same functions. New `hit_left` for the Sentry (13 clips) and the Custodian (20). Rigs checked mirror-symmetric under the twin maps (the Sentry's pupil bones sit half a pixel off-centre in the rig, but its hit clips only slide them in and out, so the mirror is exact). Every other clip is unchanged (checked keyframe by keyframe against the previous bbmodels).
- **Self-collision check:** `node design/models/clockwork-wastes-bots.gen.js sentry` and `... custodian`: every clip, `hit_left` included, "no clipping".
- **Preview:** both bbmodels reopened in Blockbench, left in the Animate tab on `hit_left` (the Custodian's tab in front, at 0.25 s; the Sentry's at 0.2 s). Peak frames: the Sentry at 0.2 s, the Custodian at 0.25 s.
- **Not exported, not wired:** don't run the game export for these until the user approves. In code, `CityBot.LEFT_HIT_CLIPS` (false) is the flag; `hitClip(source, at, forward, up)` works out the struck side (`struckSide`: the hit's source position against the part's right, forward x up; dead ahead or behind counts as right), notes it (`lastStruck()`), and plays `hit_left` for a left hit only while the flag is on, otherwise `hit`. The Sentry's side is its head's (yaw ring turn in its mount's plane: `headFacing()`, `headUp()`, so wall Sentries work too); the Custodian's is its body's (`yBodyRot`). Both now treat either hit clip as a hit (`playingHit()`: the Sentry's shot waits, the Custodian's walk holds off `HIT_HOLD`). **To turn on after approval:** run the game export, then set `LEFT_HIT_CLIPS = true` (a unit test fails if the flag and the exported clips disagree).

### Tests
- Unit: `WastesBotAssetsTest.flipWindowAndCuriosityMatchD67` (4 s window, righted inside it, `curious` holds and is posed within the curiosity, ~8 blocks, cooldown longer than the curiosity; `curious` added to the clips the code plays), `hitSideIsToldFromTheFacing` (sides for south, west and a wall Sentry's frame; dead ahead; the clip choice with the flag off and on; the flag on exactly when both bots' exported clips have `hit_left`); `CustodianTethersTest` (beads only on a standing pylon's cable, and the old timing rule). **All 948 unit tests pass.**
- Game (`WastesBotGameTests`): `aScuttlerIsCuriousAboutAPlayerItCantFight` (own batch; a creative and a spectating stand-in 5 blocks off: curious, facing them, no target; back to idle after 3 s with the cooldown running; not again within it; one 10 blocks off never), `aHitFlipsAScuttlerAtMostOnceEvery4s` (flipped; a hit on its back and one 2 s on: hurt, not flipped; 4 s on: flipped), `aHitIsToldFromItsLeftOrItsRight` (a floor Sentry faces south, right to the west; hits from each side of the Sentry and the fighting Custodian are told apart and play `hit` while the flag is off), `spentVentsResealWhenTheCustodianResets` (the burst vents re-seal at the reset: live, no burst look, locked; in the next fight the re-sealed vent bursts again, and with the other vents broken the second Scuttler comes in smoke at the pylon's foot). **All 305 game tests passed** (301 + these 4). A later run had 311 with the city work's new `WastesWorldGameTests`, in progress at the same time; two of those failed (`elytraParity`, `devCityBindsAndWakesPerDistrict`), all of these passed.

### Choices needing the user's agreement
1. **Curiosity's timing and reach (built provisionally: `CURIOUS_TICKS`, `CURIOUS_COOLDOWN`).** Curious for 3 s, then not again for 20 s per Scuttler; it turns its whole body to face the player and follows them with it; it needs sight of them (not through walls); only from standing idle (not mid-mannerism, walking or climbing). Options: shorter/longer (e.g. 2 s / 10 s, or 5 s / 30 s); once per player arriving (until they've gone 8+ blocks away) instead of a timed cooldown; no sight needed. Recommend as built.
2. **A vent unloaded at the reset (built: stays open).** Only loaded vents re-seal (the Custodian looks in its hall's loaded chunks; in practice the hall is loaded while it kneels home, as a player has just been there). Option: the city record notes the reset and a vent loading later re-seals itself (a change to `CityRecord`, the city agent's file). Recommend as built.
3. **The re-seal has no look of its own (built: the hatch is simply shut again).** At the end of the reset's kneel the open vents snap shut, seam lit. Options: a re-seal animation (the hatch swinging up and latching, the seam relighting; a preview first); as built. Recommend a short preview if the snap reads badly in game.
4. **Hits inside the 4 s window (built: damage and knockback, no clip).** Options: play a smaller flinch (no clip exists yet; a clip pass); as built. Recommend as built.
5. **The side of a hit (built behind `LEFT_HIT_CLIPS`, off).** Told from where the damage came from (the projectile, or the attacker) against the Sentry's head and the Custodian's body; dead ahead or behind plays the right-side clip. Recommend as built, turned on once the user approves `hit_left`.

## The city (D61, D64-D67; built 2026-10-29, for review in game before world generation)

D66: the city is reviewed in game, not as Blockbench vignettes. `/wastes buildcity [seed]` builds the whole of it beside you; it goes into world generation only after the user approves it (the switch below is off).

### How it's built: a plan, then the world
- `content/wastes/city/CityPlan`: the city written down before it's built, as ordered `Op`s (`Put` a block bound to a district or not, `Ground` a column, `Span` / `Snapped` / `Hop` for the sky-cables, `Scuttler` / `Sentry` / `Custodian`). It depends only on the origin (the heart's middle, at its floor) and a seed, never on the ground, so world generation builds it a chunk at a time (`opsIn(chunk)`) and the dev command all at once, and they get the same city. Cables are worked out last from the plan itself (their arms join across chunk edges). Each `Put` says where it may go: anywhere (the core), only on ground (everything in the outskirts, so wrecks never float), only over the void (bridge rails, a snapped bridge's hanging end).
- `CityLayout` draws it (below). `CityLayout.districtAt(dx, dz)` is the rule every block is bound by: the heart for |x|, |z| up to 22, else its quadrant.
- `CityApplier` builds ops into a world and reads the ground as it goes: the core (|x|, |z| up to 70) is levelled to the heart's floor (a dip up to `FILL_DEPTH` 6 filled with End Stone, a hill cut down, the top Scorched End Stone in its three variants); with no land that near, a void-alloy deck, a trestle (a void-alloy pillar column) every 8 blocks reaching down to land or `TRESTLE_REACH` 32 into the void (P3). The outskirts are levelled only where land is within 8 below or 12 above. Bars, panes, fences and girders join up with their neighbours after. `CityApplier.Live` (a loaded level: binds through `CityLink.bind`) and `CityApplier.Gen` (a generating chunk: the link goes straight into the block entity's NBT and it catches up on load, P48; bots go into the chunk through the `WastesBots` placement API).
- `CityBuild.build(level, origin, seed, island)`: the dev command's and the tests' whole-city build; `CityBuild.island` lays a test island (round, about 80 across the middle, the Rail Yard's far corner and the north and east boulevards' far ends bitten off so the struts and snapping bridges show); `CityBuild.writeMap` writes a top-down PNG for reviewing the layout (the dev-city game test writes `run-gametest/wastes-city-map.png`).
- `WastesBots.placeScuttler/placeSentry/placeCustodian` now take a `ServerLevelAccessor` (a `ServerLevel` as before, or the generating chunk).

### The layout (every choice here is listed for the user below)
- **Heart** (|x|, |z| up to 22): the makers' End Stone brick drum, an octagon round the 31 x 31 hall (flats at 16, corners cut at |x| + |z| = 28), 14 high, its top course gapped; eight copper-patina dome ribs (Weathered Cut Copper) rising from the wall top toward the spire, three snapped short. The hall's floor is Void-Alloy Plate with conduit grooves (Seamed Plate, lit with the heart) running diagonally from each pylon to the dais. The dais is the preview's 3 x 3, one block high, its middle seamed (D67). Pylons at (+-11, +-11); the eight wall vents at (+-16, +-9) and (+-9, +-16), facing in (the heart-hall preview's places). The Custodian kneels on the dais facing north, toward the main heart door; the Derelict Star Reactor stands between them (centre (0, -8)) under the spire's shaft, its four legs round it; the heart vault is in the south wall in a tall void-alloy surround with two Maker Statues facing the wall beside it. Heart doors (Sliding Door, heart) north, east and west, none south (the vault's wall). Heart lamps: eight wall lamps in the hall, eight posts round the plaza. The frozen shift: two Worker Shells `frozen`, a Broken Mechanical Arm over a Create Depot.
- **Spire:** over the reactor (x 0, z -8), on its four legs to the roof (14), a hollow 5 x 5 shaft to 40 then 3 x 3 to the crown at 58, its corners Seamed Plate (lit with the heart: they climb, below), slots every 6 blocks. The crown: a seamed cap and four arms of Void-Alloy Pillar reaching 8 blocks north, east, south and west, a hanging Derelict Insulator under each tip.
- **Boulevards:** 7 wide (|x| or |z| up to 3) from the plaza to the city's edge, paving with scorched gutters, lamps on each side every 8 blocks. Beyond the core they run on as bridges (decks over the void, iron-bar rails); the north and east ones snap off 82 blocks out, a void-alloy end and a chain hanging below.
- **Districts:** Foundry north-west, Workshops north-east, Rail Yard south-east, Quarters south-west. Each in its own frame (u out along x, v out along z, from 4 blocks off the axes): two streets 5 wide (v 30-34 along u, u 30-34 along v) of the makers' paving, lamps every 7 blocks alternating sides, crooks over the street, the outer ones leaning (D41) and snapped more; Derelict Cable along each street one block in, joining the lamp feet. Lots: the Substation yard (u, v 19-29, the corner nearest the heart), lot N1 (sky tower, a small shed), lot E1 (the vault house), lot A (u 0-29, v 35-66: two sheds, scrap heaps, the standing statue), lot B (u 35-66, v 0-29) and lot C (u, v 35-66) for the set piece.
- **Substation yard:** iron-bar fence with a gate to street A; the Substation facing the gate behind one hazard band (Yellow and Black Terracotta alternating: the only warning colours in the city); its cable out of the gate to the street's; a transformer bank (Seamed Plate fins, lit, with iron bars between, a plate on top); a makers' control hut (Andesite Casing, a walnut-slab roof, a Broken Press inside); the lattice mast (five Create Metal Girders) with a top-mounted insulator.
- **Sky-cables (D61, D64):** each district's chain: the Substation's tray -> the mast's insulator -> a wall insulator near the top of the sky tower (u 12-14, v 23-25, 22 high, slotted, corner pillars, a seamed course every 7) -> the crown insulator over the heart (north arm the Foundry, east the Workshops, south the Rail Yard, west the Quarters). A snapped span hangs from a second insulator lower on each tower, and from a top insulator on a void-alloy stub in each district's outer corner.
- **Vault house** (u 20-28, v 4-12): void-alloy walls 5 high with a seamed course, a locked district Sliding Door toward the boulevard, the City Vault facing it from the back wall, a Sentry on the wall above it, four Charging Alcoves along the side walls, a slumped Worker, a wall lamp over the door, Derelict Cable along the front wall at head height.
- **Lot A:** two makers' sheds in the district's material (Foundry Bricks, Workshops Andesite Casing, Rail Yard and Quarters Stone Bricks), walnut-slab roofs with holes (more toward the edge), the second behind a locked district Sliding Door with two alcoves; broken machines inside; Worker Shells slumped against the walls; six Scrap Heaps where the Scuttlers sleep; the district's one standing Maker Statue facing away from the heart; a small shed with a press near the tower.
- **Set pieces:** Foundry: a brick furnace shed with Blast Furnaces and Cauldrons (crucibles), a press and a mixer, a scorch fan of Polished Blackstone Bricks up its outer wall, two chimneys (2 x 2, 16 high) and a third lying across street A; the casting floor: rows of void-alloy moulds with a limb-shaped groove, stacked moulds, the frozen pour (a fallen Worker in its mould, Tinted Glass spill), a girder crane with a cauldron ladle on chains, tipped. Workshops: two long sheds of walnut benches with Broken Mechanical Arms and Deployers on them; the assembly gantry (girders) over five clamps, each body more complete (a pillar spine; + chest plate; + shoulders; + a seamed core; a whole Worker `frozen`), a fallen roof and two bolt craters. Rail Yard: track (rails on gravel and walnut sleepers), a raised platform under a canopy with three charging cradles, the last train (two Railway Casing carriages and a Brass Casing locomotive, doors open to the platform), a Resistance Shell `reaching` on the first carriage's step, uprising Workers on the platform behind it, a void-alloy signal mast with its wall lamp; a turntable, a signal box, a carriage on its side and torn-up track in a crater. Quarters: four makers' terraces, their small doorways bricked up; the alcove wall (three tiers of Charging Alcoves, walkways with railings), a ramp (half-block steps) up to the first walkway with a barricade across it, a **Lift** at the wall's end with a call plate at each tier; the vigil (eight alcoves round a Toppled Maker Statue, a slumped Worker before each, facing it); an alcove tower split down the middle.
- **Outskirts:** barricades (iron bars, plates, scrap heaps) across both streets' ends at each district's edge; a snapped tower stub with an insulator and a leaning, snapped post at each district's edge; 160 scattered wrecks (scrap heaps, broken machines, stubs, fallen Workers) thinning outward; the two war-frames locked together north-west of the city (the uprising's: void-alloy pillars, its big dark lens; the resistance's: Andesite Casing and Brass, a split brass drum with its spring out in iron bars, a winding key, two brown-glass eyes in brass); the siege lines at 84 blocks (a broken ring of casing and walnut, Resistance Shells behind it facing in); the toppled siege winch (south-east).
- **Bots (P4):** per district 2-4 Sentries (in order: the vault house wall over the vault, the yard's floor, the tower's street side, shed 2's wall) and 3-5 Scuttlers curled in lot A's scrap heaps, all dormant through the `WastesBots` API; the Custodian kneeling on the dais, given its four pylons and the vault.

### The Derelict Insulator and its spans (D61, D64)
- Block `derelict_insulator` (`DerelictInsulatorBlock`): `mount` top / wall / hanging by the face it's placed on (a wall mount faces away from its wall), `facing`, `lit` (its neck ring, an emissive part in its block model, drawn by the multipart when lit). Ties at the block's middle, 12 / 13.5 / 4.4 px up (the exporter's). The Substation's tray tie is (11, 34, 8) px in its north-facing model, turned with its facing (`DerelictInsulatorBlockEntity.substationTie`).
- `DerelictInsulatorBlockEntity` (a `CityBlockEntity`): spans it keeps (`addSpan(other, stage, beadFromHere)`, `addSnapped(heading)`), its `hop` (spans from the Substation). The city links them; players can't. Lit while its district is fed. When its district wakes while it's loaded, it stamps `wakeAt` (synced): each span's bead runs in its 2 s turn (`BEAD_TICKS` 40 x its stage) and the line lights behind it; the ring lights when the bead reaches its tie (hop x 2 s). Broken, its spans drop as scrap (`SCRAP_PER_BLOCKS` 4 blocks of span a scrap, a snapped span 1) and so do spans held by insulators within 4 chunks that tied to it; the Substation does the same when it's broken (`anchorRemoved`).
- `client/wastes/SkyCableRenderer` draws the spans: the preview's 1.2 x 0.8 px strand pair (`span_side` / `span_top`, the worn copper textures on the two segments at each tie), a sag of `SAG` 0.075 of its length, the lit line (`ring_lit`, 0.3 px, full bright) in its gap, the bead (3 px, `flash`); the snapped span is the preview's polyline (`SNAP_PTS`), bare copper at the break, swaying 2.5 degrees over 4 s.
- Exporter (`clockwork-wastes.export.js`, section "the Derelict Insulator"): `derelict_insulator_{top,wall,hanging}` and their `_ring` overlays, a multipart blockstate, the item (the top mount in 3D, provisional), the span textures, loot (1 scrap), pickaxe tag. Rebuild note: the preview's ring stood 0.1 px proud of its neck (a z-fight); now 0.25. `clockwork-wastes-insulator.gen.js` exports its builders and writes its preview only when run directly (unchanged apart from uuids).
- Lift flat icons (D65): the approved rows are in the exporter's `ICONS`; `models/item/lift.json` and `lift_call_plate.json` are flat icons now.
- The spire's seams (world brief): when the heart comes on, a heart light above the drum's roof waits by its height (`DerelictLightBlockEntity.SPIRE_CLIMB_TICKS` 80): the crown's at once, the corner seams climbing to it over 4 s.

### World generation (written, switched OFF)
- **The switch:** `clockworkWastes.cities` in the server config (`comedymonkeysaddons-server.toml`; `CMConfig.SERVER.clockworkCities`), **false** by default. While false, `ClockworkCityStructure.findGenerationPoint` places nothing. To turn it on for a world: set `cities = true` in that world's `serverconfig/comedymonkeysaddons-server.toml`; to ship it on, change the default in `CMConfig` (one word).
- Structure type `comedymonkeysaddons:clockwork_city` (`ClockworkCityStructure`, registered in `ModStructures` with the piece `ClockworkCityPiece`: the whole city as one piece, built a chunk at a time from its cached plan; the heart's chunk records the city through `CityRecord.addLater`, drained on the next server tick). Structures `clockwork_city` (biomes `#comedymonkeysaddons:has_structure/clockwork_city`) and `clockwork_city_highlands` (`minecraft:end_highlands`, places only with `highlandsFallback` on: P53), both `surface_structures`. Structure set `clockwork_city`: random spread, spacing and separation as below, salt 1478302851, exclusion zone 6 chunks against `minecraft:end_cities`.
- Island fit (P46, `ClockworkCityStructure.site`): the heart needs ground; one sample per chunk of the 9 x 9 footprint, End Stone at the heart's floor in at least `islandFit` (0.5) of them, else outskirts only (no record), with their own Elytra since D69 (see "D69: city systems").
- Config: `cities` (false), `islandFit` (0.5), `highlandsFallback` (false). The spacing stays in the data pack, as the Juggernaut ruins' does.

### Tests
- Unit `content/wastes/city/CityWorldgenDataTest` (5): the structure set's spacing, separation and exclusion; both structures; the Wastes in the city tag and not End cities'; every column in exactly one district; the test island holds at least half the footprint but not the struts' corner or the bridges. `WastesAssetsTest.derelictInsulatorModelsAndSpanTextures`, and the lift items are flat icons.
- Game `WastesWorldGameTests` (6): `everyPlacedBlockIsInItsDistrict` (three seeds: every city block and bot bound to the district it stands in, crown insulators to their own; no city block unbound; per district 1 Substation, 1 vault, 2-4 Sentries, 3-5 Scuttlers; the heart 4 pylons, 8 vents, 1 vault, 1 Custodian); `devCityBindsAndWakesPerDistrict` (own batch: builds the city far off with its chunks forced; every city block bound to the city and its district, 12 spans tied at both ends, 8 vents, 3 heart doors; all asleep; the Foundry woken by its Substation's wake charge and kept fed: its mast's ring lit by 2.5 s, its crown's still dark then and lit by 6.5 s; its bots stir, vault and doors open, the others' don't; the other three one by one; the Quarters' lift unlocked with 3+ landings; four awake opens the heart doors and wakes the Custodian); `theCitySetIsAsTheSpecSays`; `citiesKeepSixChunksFromEndCities` (no allowed city chunk within 6 of an End-city start; the zone really turns some away); `islandFitSampling` (200 sites); `elytraParity`.
- Results (the test seed): island fit 63 of 200 Wastes sites fit, 118 outskirts only, 19 no ground under the heart (mean footprint on land 0.44); the exclusion zone keeps about 41% of placements off End cities; Elytra parity 62 cities against 50 displaced ships at spacing 24 (the table at each spacing is in choice 1). **All 953 unit tests and all 311 game tests pass.** The exporter still exits 0 with no z-fights.

### Choices needing the user's agreement
Nothing below was decided silently: each is built provisionally as one named constant or rule, or not built, as marked.

World generation and the city's systems:
1. **Decided (D69): spacing 32, option (b); built, see "D69: city systems".** Was: **Spacing (built provisionally: 24, `CityLayout.SPACING`, worldgen/structure_set/clockwork_city.json).** P45 starts at 32 and says to tune it until the Wastes give at least as many Elytra as the End-city ships they displace (D18). On the test seed, over +-20,000 blocks: 99 End cities would have started in the Wastes, 50 of them with ships; cities that would place (allowed by the 6-chunk exclusion, on an island that fits) give 33 Elytra at spacing 32, 46 at 30, 38 at 28, 46 at 26, 62 at 24 (separation 12 throughout). The exclusion zone turns away about 41% of city placements (3,666 allowed, 2,575 kept off over +-20,000), and island fit passes 61 of 200 Wastes sites (110 outskirts only, 29 no ground under the heart). Options: (a) spacing 24 (as built: parity with margin); (b) keep 32 and give outskirts-only sites their own Elytra (say a resistance cache: they flew the makers out on wings); (c) keep 32 and lower `islandFit`; (d) keep 32 and shrink the exclusion. Recommend (a); the test checks one seed only.
2. **The switch (built):** `clockworkWastes.cities` in the server config, off. Turned on per world in its server config, or for everyone by changing its default.
3. **Spacing in the data pack, not the config (built).** P26 and the tech notes put city spacing in the config; it's in the structure set JSON, as the Juggernaut ruins' is (a config would need a custom placement type). Recommend keep.
4. **Highlands fallback (built):** a second structure in the same set, `clockwork_city_highlands` (`minecraft:end_highlands`), placing only with `highlandsFallback` on; the Wastes tag unchanged. Recommend keep.
5. **Ground rules (built: `CityApplier.FILL_DEPTH` 6, `OUTSKIRT_BELOW` 8, `OUTSKIRT_ABOVE` 12, `TRESTLE_REACH` 32):** the core's dips up to 6 blocks are filled with End Stone and its hills cut down; deeper or void, a void-alloy deck with a trestle every 8 blocks; outskirts wrecks only on land within 8 below / 12 above the floor. Recommend keep, checked in game.
6. **Kept (D69); outskirts-only sites now also hold an Elytra, see "D69: city systems".** **Outskirts-only sites (built):** wrecks, the siege ring at 30 blocks and the war-frames round the spot, nothing bound, no record. Recommend keep.
7. **Crown insulators' district (built):** bound to the district whose sky-cable they hold (so their ring lights with it), though they stand over the heart. Option: bind them to the heart (lit only with all four fed). Recommend as built.
8. **The bead (built):** runs only for a wake seen while loaded and fed (a district woken unfed, or loaded later, just lights); upkeep returning later lights the spans without a bead. Recommend keep.
9. **Spans coming down (built: `SCRAP_PER_BLOCKS` 4):** a span drops a scrap per 4 blocks of its length (at least 1), a snapped span 1, where the broken anchor stood; insulators look 4 chunks round for spans tied to it. Recommend keep.
10. **Span sag (built: `SkyCableRenderer.SAG` 0.075 of the length, at least 0.25 blocks)** from the preview's 6-9 px on 5-8 block spans. Recommend keep, checked in game.
11. **Decided (D69): blacklisted on Carry On; built.** The item and loot are still as built and still need the user's word. **The insulator's item, loot and Carry On (built):** its item is the top mount in 3D (option: a flat icon), it breaks into 1 scrap, and it is **not** on Carry On's blacklist. Recommend keep the item and loot, and blacklist it (it's city-linked, as the lift and vent are).
12. **The spire climb (built: `DerelictLightBlockEntity.SPIRE_CLIMB_TICKS` 80):** when the heart comes on, heart lights above the drum's roof wait by height (the crown at once, the corner seams climbing over 4 s); the rest of the heart ripples as before. Recommend keep.
13. **Rebuilding (built):** `/wastes buildcity` at the same origin starts the record over and removes that city's loaded bots first. Recommend keep (dev only).

The layout (the briefs give the parts, not where they go; all built, each for checking while walking round):
14. **Quadrants:** Foundry north-west, Workshops north-east, Rail Yard south-east, Quarters south-west.
15. **The drum's size:** the brief says 21 across but the hall inside it is 31 x 31 (P32), so the drum is an octagon about 35 across (flats at 16-17, corners cut), 14 high, inside a heart of 45 x 45 (the brief's "about 41").
16. **The spire's place:** over the reactor (the brief: "under the spire's shaft"), so 8 blocks north of the hall's middle, on four legs round the reactor up to the roof; 5 x 5 to 40, 3 x 3 to the crown at 58.
17. **The hall's orientation:** the Custodian faces north toward the main heart door, the reactor between them, the vault in the south wall behind the dais; heart doors north, east and west (none south).
18. **The crown's arms:** on the axes (north, east, south, west), so each district's cable runs to the arm a quarter turn from it: north the Foundry, east the Workshops, south the Rail Yard, west the Quarters (diagonal arms would be stair-stepped blocks).
19. **Conduit grooves:** diagonal lines of Seamed Plate in the floor from each pylon to the dais.
20. **Districts as L-shaped quadrants** 4 blocks off the axes (larger than the brief's 45 x 45 square), with 7-wide boulevards on the axes; two 5-wide streets each; lamps every 7 blocks (the brief: 6-8) alternating sides; boulevard lamps every 8.
21. **The Substation yard:** where its parts go; the hazard band as Yellow and Black Terracotta; five girders in the mast.
22. **The sky-cable chain:** Substation -> yard mast -> one sky tower (22 high) beside the yard -> the crown: three spans a district; snapped spans from a lower insulator on each tower and from a stub in each district's outer corner.
23. **Doors:** two locked district doors a district (the vault house and one shed), three heart doors.
24. **Makers' materials:** Foundry Bricks, Workshops Andesite Casing, Rail Yard and Quarters Stone Bricks; walnut as Dark Oak; soot as Polished Blackstone; the dark glass spill as Tinted Glass; the train as Railway Casing and Brass Casing with vanilla rails.
25. **Lifts:** one, in the Quarters' alcove wall (P4 doesn't list lifts per district; the brief puts them in the Quarters). Options: also one in the heart ("a lift halfway up its shaft", the brief's frozen shift) or one a district.
26. **Bots' places:** Sentries in order at the vault house (over the vault), the yard's floor, the sky tower, shed 2's wall; Scuttlers in lot A's scrap heaps.
27. **Outskirts:** barricades at the street ends; the war-frames north-west; the siege ring at 84 blocks; the siege winch south-east; the north and east bridges snap 82 blocks out.

Needs new models (**28-31 answered by D72 and built: see "D72: the city props"**):
28. **Half-built bodies** for the Workshops' assembly line: the first four clamps hold block stand-ins (a pillar spine, plates), the fifth a `frozen` Worker. The brief: "each more complete than the last; the last one has its lens in".
29. **Alcove trinkets** (a cog, a winding key, a toy cart, a faded flower in one alcove in five): the alcove has no trinket state or model.
30. **Brass makers' plaques** on old buildings, **dead vines** in Quarters alcoves, **cable climbing the lattice masts** (Derelict Cable needs a full face; girders have none): not built.
31. **The `frozen` pose outside the heart:** the brief keeps it to the heart, but the assembly line's last body uses it (it's the only upright pose). Option: a new "clamped" pose.

## D69: city systems (built 2026-10-29)

D69 settles city choices 1, 6 and 11 and keeps the rest of choices 3-5 and 7-13 as built. The brief items still standing in as plain blocks (choices 28-30) are being modelled in Blockbench separately and aren't part of this.

### Spacing 32
- `CityLayout.SPACING` 32 and `worldgen/structure_set/clockwork_city.json` `spacing` 32. Separation 12, salt and the 6-chunk exclusion against `minecraft:end_cities` are unchanged.

### Outskirts-only sites hold their own Elytra (provisional: choices below)
- A site that fails island fit (P46) still gets the outskirts only: no city, no record, nothing bound, no bots. It now also gets **the resistance's cache** (`CityLayout.resistanceCache`) in its middle (`CityLayout.RESISTANCE_CACHE`, offset 0, 0, 0). That's the heart's column, which `ClockworkCityStructure.site` has already made sure has ground, so the cache always places.
- The cache is a vanilla Barrel (facing up, closed) with the loot table `comedymonkeysaddons:chests/clockwork_resistance_cache` (`CityLayout.RESISTANCE_CACHE_LOOT`), which holds exactly `OUTSKIRTS_ELYTRA` (1) Elytra and nothing else. Its loot rolls when it's first opened, as a vanilla chest's does (new `CityPlan.Op.Loot`, applied by `CityApplier` to any `RandomizableContainerBlockEntity`). Behind it, to the west, is a short stretch of the siege line's cover (Andesite Casing, Dark Oak Planks, Andesite Casing, one more casing on top). East of it a Resistance Shell sits `slumped`, back to the barrel, facing east. All existing blocks; nothing is lit.
- A full city never gets the cache: its one Elytra is the heart vault's.
- **Fix found on the way:** `CityPlan.inCore` counted an outskirts-only site's middle (|x|, |z| up to 70) as core, so its wrecks there could be put "anywhere" and float over the void, since that site has no levelled core or decks. An outskirts-only site now has no core, so all of it stands only on ground, as choice 6 already said.
- Dev tool: `/wastes buildoutskirts [seed]` builds an outskirts-only site, with its middle where you stand, on the test island. It puts you 12 blocks south of the cache, looking at it (`CityBuild.buildOutskirts`).

### How it fits the vault's Elytra rules
- D19, P11, P23: one Elytra a city, in the heart vault behind the last locked door, shared and first come first served. Unchanged: a site that fits gets a city with its heart-vault Elytra and no cache.
- D31, P38, P40, P41: the heart vault opens on the Custodian's death (on Peaceful, once four districts wake), with no grab-and-run. Unchanged: these rules are about a city's vault. An outskirts-only site has no city, vault, Custodian or record, so there's nothing to grab and run from.
- Each site gives exactly one Elytra either way: a city's from its vault, an outskirts-only site's from its cache. That matches one Elytra per End-city ship (D18, P45).
- Story tension, flagged below: D36 calls the heart vault's Elytra "the last pair of wings". The cache is only ever where no heart stands.

### Carry On
- `derelict_insulator` is on `data/carryon/tags/block/block_blacklist.json` beside the Substation, vault, doors, pylons, reactor, lift, call plate and vent. That list is kept by hand, and no exporter writes it.

### Tests
- Unit `CityWorldgenDataTest` (6): spacing 32 in `CityLayout` and the data pack; new `theResistanceCacheHoldsOneElytra` checks the cache's table holds exactly one Elytra and nothing else. `WastesAssetsTest.derelictInsulatorModelsAndSpanTextures` now checks the Carry On blacklist too.
- Game `WastesWorldGameTests` (7):
  - New `outskirtsSitesHoldOneElytra` (own batch). Its plans on three seeds have exactly one cache (a barrel at the middle, on ground, with the table), no bound or city block, no bot and nothing "anywhere". A city's plan has no cache. It then builds a site a chunk at a time through `CityApplier.Gen`, as world generation does, high over the void with land under the middle column only. The barrel stands and rolls exactly 1 Elytra and nothing else, no other block floats, and there's no record.
  - The End tests (`citiesKeepSixChunksFromEndCities`, `islandFitSampling`, `elytraParity`) now run on the game-test world's End (seed 0) and the same End rebuilt for `OTHER_SEEDS` 1, 42, 8675309 and -20241029. Each seed gets a fresh TerraBlender End biome source, the End's noise with that seed, and its structure state; they're sampled, not generated.
  - Parity counts a city's Elytra and an outskirts-only site's, and must reach the ships on every seed. The spacing table is gone, now that 32 is decided.
- **Elytra parity at spacing 32** over +-20,000 blocks (Elytra = cities + outskirts-only sites, against the ships of End cities displaced by the Wastes):

| Seed | Cities | Outskirts-only | Elytra | End cities displaced | Ships | Elytra / ships |
|---|---|---|---|---|---|---|
| 0 (game-test world) | 33 | 55 | 88 | 99 | 50 | 1.76 |
| 1 | 36 | 86 | 122 | 128 | 68 | 1.79 |
| 42 | 33 | 69 | 102 | 115 | 53 | 1.92 |
| 8675309 | 41 | 71 | 112 | 151 | 93 | 1.20 |
| -20241029 | 37 | 70 | 107 | 132 | 77 | 1.39 |

  Cities alone would fall short on every seed (33-41 against 50-93).
- **Island fit** (200 Wastes sites each; fit / outskirts only with an Elytra / no ground under the heart / mean footprint on land): seed 0 61 / 110 / 29 / 0.44; seed 1 55 / 119 / 26 / 0.43; seed 42 74 / 105 / 21 / 0.44; seed 8675309 60 / 118 / 22 / 0.42; seed -20241029 56 / 115 / 29 / 0.41.
- **Exclusion** (placements allowed / kept off End cities over +-20,000): seed 0 3,666 / 2,575; seed 1 3,628 / 2,613; seed 42 3,548 / 2,693; seed 8675309 3,597 / 2,644; seed -20241029 3,652 / 2,589. That's about 41-43% kept off, and no allowed city is within 6 chunks of an End-city start on any seed.
- **All 954 unit tests and all 312 game tests pass** (`./gradlew compileJava test runGameTestServer`).

### Choices needing the user's agreement
Each is built provisionally as named, for the user's word.
1. **Where the outskirts Elytra sits (built: `CityLayout.RESISTANCE_CACHE`, the site's middle).** It's the only column world generation is sure has ground, so the Elytra always places and parity is exact. Options:
   - (a) the middle, as built;
   - (b) by the war-frames, where it fits the story better ("the resistance flew the makers out on wings") but may land over the void, so some sites lose their Elytra and the parity margin shrinks;
   - (c) in the siege ring, with the same risk.
   
   Recommend (a).
2. **What holds it (built: a vanilla Barrel with a loot table).** It rolls on first open as vanilla chests do, uses the makers' wood, and fits "a supply cache". Options:
   - (a) Barrel;
   - (b) Chest (reads as "loot" from further off);
   - (c) an Item Frame showing the Elytra on the resistance's cover, as an End ship shows its own. Very visible, but it's an entity and is easily knocked off.
   
   Recommend (a).
3. **How many a site (built: `OUTSKIRTS_ELYTRA` 1).** One a site, like a city's vault and an End ship. With it, parity holds at 1.2-1.9x the ships on five seeds. Recommend 1.
4. **Anything else in the cache (built: nothing).** Options: (a) the Elytra alone; (b) a little resistance filler (brass, Clockwork Scrap, a Mainspring). Recommend (a), so it reads as the wings and nothing more.
5. **Its look (built: barrel, a short cover wall of casing and walnut, one slumped Resistance Shell, unlit).** It shows only as a small, quiet vignette in the middle of the site. Nothing points to it, keeping the brief's "the resistance never glows". Options: (a) as built; (b) plainer (the barrel alone); (c) bolder (a winding-key mark, or more shells round it; a key on a nail would need a new model). Recommend (a), checked in game with `/wastes buildoutskirts`.
6. **Unguarded (built).** An End ship's Elytra has one Shulker by it, but the cache has no guard: an outskirts-only site has no record or district, so city bots there would never wake. Options: (a) unguarded, as built; (b) a few plain hostile mobs (vanilla spawner or Endermites); (c) bind cache bots to a mini-record. Recommend (a): the trip across the void is the cost, as it is for a ship.
7. **The story (not written anywhere in game).** D36 calls the heart vault's Elytra "the last pair of wings". The cache is only ever at a site with no heart, so it's the pair the resistance never came back for. Recommend keeping D36's line and treating the cache as the wings of the resistance's last forward post. Option: soften D36 to "the last pair in the city".
8. **Fix: outskirts-only sites have no core (built).** Wrecks within 70 blocks of an outskirts-only site's middle could float over the void. Now they stand only on ground, as choice 6 of "The city" said. Recommend keep.
9. **`/wastes buildoutskirts [seed]` (built, dev only).** For reviewing an outskirts-only site and its cache in game (D66), on the test island. Recommend keep.
10. **Parity seeds (built: `WastesWorldGameTests.OTHER_SEEDS` 1, 42, 8675309, -20241029, plus the world's 0).** Over five seeds the island-fit and parity tests take about a minute each, and the whole game-test run is about 3.5 minutes. Recommend keep.

## D72: the city props (built 2026-10-29)

D72 approved the city props as previewed (`design/models/clockwork-wastes-city-props.gen.js`, its `.bbmodel`, screenshots in `design/texture-reviews/clockwork-wastes-city-props/`). All built here; nothing approved was reshaped. What D72 leaves open is built provisionally (one named constant or rule each) and listed under "Choices needing the user's agreement" at the end of this section.

### Exporter (`clockwork-wastes.export.js`, section "the city props", at the end so the generators whose textures the props share are loaded first)
Every check kept (bounds -16..32, one-axis turns of 0/±22.5/±45, no zero-width UVs, no faceless elements, the z-fight check, the `#121118` icon outline); it still exits 0 with **no z-fights**. Re-run: `node design/models/clockwork-wastes.export.js`.
- **Assembly Clamp:** `assembly_clamp_<1-5>.obj` + `.json` (NeoForge OBJ loader, baked from `assemblyStage(n)` as the shells are: stages 4-5 stand 33 and 33.5 px, past a block model's 32). Stage 1's open chest jaws (turned 22.5 about y) are baked as bones. Blockstate `facing` x `stage` (20 variants, north as drawn). Bounds (px): x 1-15, z 4-12, y 0-30 (stages 1-3), 0-33 (4), 0-33.5 (5). Item: a flat icon (`ICONS.assembly_clamp`, stage 5, the stage the item places). Loot: `blocks/assembly_clamp` 1 scrap; `salvage/assembly_clamp` the shells' (2-3 scrap, 25% Electron Tube); in the `salvage` tag and the pickaxe tag.
- **Charging Alcove:** the base is now `alcoveBase()` + `shelf()` (the corner shelf in place of the thin ledge and the cart that stood on it). New parts in its multipart: `charging_alcove_trinket_{cog,key,cart,flower}` (middle block; the framed flower, the key standing) on `trinket`, `charging_alcove_vines_{middle,top}` on `vines` (the cutout cards as previewed). The alcove block moved from the decor section to here.
- **Brass Plaque:** `brass_plaque`, `brass_plaque_askew` (the preview's `plaqueAskew`: hanging from its top-west bolt, the other two gone), blockstate `facing` x `askew` (north as drawn: the wall south of it). Item: its square model fitted to the slot. Loot: drops itself.
- **Mast riser:** `derelict_cable_riser_{foot,run,top}` and their emissive `_glow` lines, the preview's `riser(5)` moved 16 px into the cable's cell (the girder south of it): the foot is the riser guard and lip (to 19.75 px) and its block of strands plus the preview's last ground run into the guard (`groundRun` from the block's middle to z 17.25); a run is one block of strands with its clamp (the preview's clamp 6 px up each block); the top is the block under the insulator with the last clamp, the pothead and the jumper up to the tie knob (reaching 11.7 px into the block above and 8.5 px into the girder's). `derelict_cable.json` gains `girder` on every old part (`none`) and the riser's parts (surface = the girder's side: south 0, west 90, north 180, east 270); the foot's floor arms toward the floor cable it joins and a knot where one meets its run at a corner (the knot lies across the run).
- **Export notes (no approved shape changed):** cubes a block or more tall (the clamp's uprights, 28 px; the riser guard, 19 px) are cut at the block lines so each block's piece takes its UVs from its own cell and tiles at 16 px a block as everything else in game (the preview stretched one texture over them); every other cube keeps the preview's UVs.
- **Icon review:** `design/texture-reviews/clockwork-wastes-icons/assembly-clamp-icon.js` writes `assembly-clamp-icon-review.png` (8x, beside the approved city and lift icons) and `assembly-clamp-icon-sizes.png` (1x/2x/3x on an inventory slot's grey, beside the alcove's and statue's). Not yet seen by the user.

### Blocks (`registry/ModWastes.java`)
| id | class | notes |
|---|---|---|
| `assembly_clamp` | `AssemblyClampBlock` (a `SalvageBlock`) | `facing`, `stage` 1-5; collides as one block (x 1-15, z 4-12 facing north); salvage like the shells (sneak-wrench / Deployer with a Wrench: the salvage table; anything else one scrap); shell properties; item places `ITEM_STAGE` (5); Create tooltip; **Carry On blacklisted** |
| `brass_plaque` | `BrassPlaqueBlock` | `facing` (its face; placed facing out of the side clicked), `askew`; thin outline (square x 2-14, y 3-12; askew x 0-13.5, y 0-12.25; z 14.25-16 facing north); needs no wall; drops itself; brass map colour, metal sound, hardness 1.5, pickaxe |
| `charging_alcove` | `ChargingAlcoveBlock` | new `trinket` (none, cog, key, cart, flower) and `vines` on all three parts, set by the city; a player's has neither |
| `derelict_cable` | `DerelictCableBlock` | new `girder` (none, foot, run, top). A riser stands beside an upright Create Metal Girder (`uprightGirder`), its `surface` toward it; it falls when its girder goes. Its part follows what's round it (`riserPart`: top where the girder column ends, foot with no riser under it, else run), so a player's cable clicked onto an upright girder's side climbs it too. A riser joins nothing but its foot's floor arms (floor cable beside the foot joins it and it joins them); its line lights with its district as any cable's (same block entity) |

Lang: "Assembly Clamp" (+ a Create summary), "Brass Plaque".

### The city (`CityPlan`, `CityLayout`)
- **Assembly line (Workshops, lot C):** the five stand-ins and the `frozen` Worker are gone; five Assembly Clamps at stages 1-5 stand where they stood (u 43 + 4i, v 52), facing -v as the Worker did. The frozen pose is in the heart only again.
- **Trinkets:** `CityPlan.alcove(...)` gives one alcove in each `TRINKET_EVERY` (5) it puts, in plan order, a trinket: the place in the five and the kind (cog, key, cart, flower, evenly) drawn from the plan's seed (its own stream, salt 7201). Every alcove in the city counts (vault houses, sheds, the platform's cradles, the Quarters' wall, vigil and split tower).
- **Vines:** about one in `VINES_ONE_IN` (4) of the Quarters' alcove wall and split tower (51 alcoves a city), from the plan's seed (own stream, salt 7202, so the district's draws and its bots are unchanged).
- **Lattice masts (`CityLayout.mast`):** `MAST_HEIGHT` (5) girders, a top insulator, and the riser up the face `riserFace(spans)` (the face that keeps the jumper on the far side from the spans, as in the preview). **The yard mast** (the tray span kept) gets its riser on its +v face in the Workshops and Quarters and its -v face in the Foundry and Rail Yard; the yard's ground cable runs along the foot's row from the gate cable (v 22) or from the Substation's back (v 20).
- **Corner masts (`CityLayout.cornerMast`, its own function):** each district's lattice mast at `CORNER_MAST` (u 35, v 29: the street-corner block of lot B by the crossing), its riser on the +v (street) or +u face (whichever keeps the jumper off its span), its foot joined to the street cable (v 31) directly or through one floor cable; a span from its insulator to the yard mast's (about 12 blocks), on the bead's path (`CORNER_MAST_ON_BEAD_PATH`: stage 1, ring at hop 2, the bead running out to it as it runs up to the tower). A city now has 16 spans.
- **Plaques (`CityLayout.doorPlaque`, the one placement helper):** `shed()` puts one beside every shed door (the wall block left of the doorway, `PLAQUE_Y` 1 up, facing out), so the terraces (their doorways bricked up after) get theirs; askew where the wall is out past `PLAQUE_ASKEW_FROM` (50) in its district's frame.
- **Bindings:** risers (cable block entities) and the corner masts' insulators bind to their district as every city block does; the clamps and plaques have no block entity.
- **The building kit takes these over:** the buildings are being redone as a kit (D71, `design/briefs/clockwork-wastes-buildings.md`). Plaques go through `CityLayout.doorPlaque` and corner masts through `CityLayout.cornerMast` (masts through `CityLayout.mast`) so the kit can call them for its own doors, terraces and corners; the current sheds call them until then.

### Tests
- Unit (`WastesAssetsTest`, 4 new): `assemblyClampModelsIconAndData` (each stage's OBJ whole; 4-5 past 32 px, 1-3 not; every facing and stage; the flat icon 16 x 16 with its outline; salvage tag and table, Carry On, pickaxe), `alcoveShelfTrinketsAndVines` (every facing, part, trinket, vines and lit state draws exactly its models, turned; the shelf in, the ledge and cart out), `brassPlaqueModelsItemAndLoot`, `mastRiserModels` (every girder side, part and lit state draws exactly its riser part and line, turned, and no run models; the foot's floor arm, line and corner knot; the models reach the girder's cell, the top's jumper the block above, each run has its clamp); the clamp and plaque in `BLOCKS`/`SALVAGE` (names, tooltips, loot); the cable test now sets `girder=none`.
- Game: `WastesGameTests.wrenchSalvagesAssemblyClamp` (every stage: 2-3 scrap and at most a tube; a pickaxe one scrap), `brassPlaqueDropsItself`, `cableClimbsAGirder` (a player's cable on an upright girder's side climbs it foot/run/top, its foot and the floor cable join, the girder's going drops its riser and the next is the foot); `WastesWorldGameTests.cityPropsArePlacedAndBound` (three seeds: clamps 1-5 in the Workshops and no frozen Worker outside the heart; exactly one trinket in each five alcoves, more than one kind; whole alcoves; vines only on the wall and split tower, 4-25 of 51; two masts a district, every riser beside an upright girder, foot/run/top by height, its own top insulator, bound, each foot joined to ground cable; the corner span to the yard mast at stage 1 / hop 2; every plaque on a wall, 1 up, askew exactly past 50); `devCityBindsAndWakesPerDistrict` now counts 16 spans and 40 risers standing by their girders, dark asleep, and the Foundry's risers lit 6.5 s after its wake while the others stay dark.
- **All 958 unit tests and all 316 game tests pass** (`./gradlew compileJava test runGameTestServer`); the exporter exits 0 with no z-fights.

### Fix found on the way
- The outskirts' 160 scattered wrecks are placed on a ring from radius 71, which reaches into the core square's corners (|x|, |z| up to 70): one landed on the assembly line's first clamp (it had sat on the old stand-in too), others on the districts' outer lots. A wreck that falls inside the core is now drawn as before (so every other wreck stays where it was) but not put. Outskirts-only sites are unchanged.

### Choices needing the user's agreement
Nothing below was decided silently: each is built provisionally as one named constant or rule.

The corner masts and the bead:
1. **Is a corner mast on the bead's path? (built: yes, `CityLayout.CORNER_MAST_ON_BEAD_PATH`).** Options: (a) a branch from the yard mast: the bead runs out to the corner mast at the same time as it runs up to the tower (stage 1, its ring lights at 4 s), as built; (b) after the tower (stage 2, ring at 6 s with the crown); (c) off the path: its span and ring simply light at the wake, no bead. Recommend (a): the whole district's web visibly comes alive from the yard.
2. **Where it is and what its span ties to (built: `CORNER_MAST` u 35, v 29; span to the yard mast's insulator).** The street-corner block of lot B by the crossing, 5 girders like the yard's; its span (about 12 blocks) goes to the yard mast, the nearest sky-web node that isn't behind the tower. Options: span to the tower's wall insulator (longer, crosses the yard); a different corner (the outer end of a street). Recommend as built; the building kit can move it (`cornerMast`).
3. **Which girder face a riser climbs (built: `riserFace`).** The face that keeps the jumper on the far side from its spans, as the preview had it. The riser is handed, so in the mirrored districts the yard riser is on the mast's back (Foundry, Rail Yard) and the corner riser on its +u face (Foundry, Rail Yard). Options: always the front, with a mirrored riser model (a new look, previewed first). Recommend as built.
4. **The yard's ground cable (built).** In game it's whole cable blocks (arms, clips, knots) from the Substation's gate cable or its back to the riser's foot, not the preview's custom 6 px run with cleats; only the last run into the riser guard is the preview's. Recommend keep.

The Assembly Clamp:
5. **What the item places, and its icon (built: `AssemblyClampBlock.ITEM_STAGE` 5, the icon shows stage 5).** A stage-3 icon (cage, core, copper lead) read as a cluttered frame at 16 px; the whole Worker with its lens ring reads. Options: stage 5 (built); stage 3 or 1 with an icon to match; five items, one a stage. Recommend stage 5. The icon needs your review (`assembly-clamp-icon-review.png`).
6. **Salvage (built: the shells' table at every stage: 2-3 scrap, 25% Electron Tube).** Options: as built; scaled by stage (stage 1 a scrap or two, stage 5 the shell's). Recommend as built.
7. **Carry On (built: blacklisted, as asked).** The brief said "like the shells", but the Worker and Resistance Shells are **not** on Carry On's blacklist (its Wastes entries are all city-linked or locked blocks), so the clamp is now the one salvage block players can't carry. Options: (a) keep the clamp blacklisted and add the shells; (b) take the clamp off, as the shells; (c) leave it as built (inconsistent). Recommend (b): neither is city-linked, and they're salvage decor alike; but (a) if carried shells were never wanted.

The Brass Plaque:
8. **Carryable (built: not on the blacklist).** It's plain decor that drops itself. Recommend keep carryable.
9. **"Askew in the outer districts" (built: `PLAQUE_ASKEW_FROM` 50: askew where its wall is 50+ blocks out in its district's frame).** Today that's only the Quarters' two outer terraces. Options: a share at random (say 1 in 3) everywhere; every plaque past the streets (u or v over 34); left to the building kit (askew on its war-damaged buildings). Recommend the kit's war-damaged versions; until then as built.
10. **Placement details (built):** one plaque beside every shed door, on the wall block to the doorway's left (-u), `PLAQUE_Y` 1 up (its plate 1.2-1.75 blocks up: "about 1.5"); the terraces get theirs the same way. It needs no wall to stay up; a player's faces out of the side clicked (a floor or ceiling click faces the player). Its item is its 3D model fitted to the slot (not a flat icon). Hardness 1.5, metal sound, pickaxe, no tooltip. Recommend keep.

The Charging Alcove:
11. **Trinkets (built: `CityPlan.TRINKET_EVERY` 5: exactly one in each five alcoves the plan puts, in plan order).** Kinds evenly (cog, key, cart, framed flower). Every alcove counts, the vigil's and the platform's cradles too. Options: an independent 1-in-5 chance per alcove (about one in five); only some kinds in some places (the cart only in the Quarters). Recommend as built.
12. **Players and the states (built: city only).** A player's alcove has no trinket or vines, and breaking an alcove gives back a plain one (the trinket and vines are lost). Options: sneak-use to cycle the trinket in creative; keep the state on the item. Recommend as built (decor dressing, like leaning posts).
13. **Vines (built: `CityLayout.VINES_ONE_IN` 4, random per alcove from the plan's seed).** Recommend keep.
14. **The alcove's flat icon still shows the old ledge with the toy cart** (phase 1's icon). Options: keep; redraw it with the corner shelf (and no cart), reviewed first. Recommend redraw, previewed.

The riser:
15. **Players can make risers (built).** Derelict Cable clicked onto an upright Create Metal Girder's side becomes a riser and works out its part as it goes up; it falls with its girder. Options: city-only (a player's click there fails, as before). Recommend keep: it's how a player would repair a mast.
16. **Its outline (built):** the thin slab on its girder side of its own cell (its model is almost all in the girder's cell); no collision, as all cable. Recommend keep.

Found on the way (not changed, a look the user approved):
17. **The Derelict Cable's corner knot z-fights.** The knot (2 x 3 px) is exactly as wide as the strand pair, so at every floor (and wall) corner the strands running along its long side share its faces (1.5 px² each side): the exporter's knot check only tried the east arm. The riser foot's knot lies across its own run, so the run is clean, but a side arm has the same fight. Options: widen the knot to 2.5 px (x 6.75-9.25), previewed first; leave it. Recommend widen.
18. **Wrecks kept out of the core (built, see "Fix found on the way").** Options: as built; let them land on the districts' outer lots again but never on a prop. Recommend as built.

## D75: the base surface and the building block set (built 2026-10-29)

D75 approved the building block set as previewed (`design/models/clockwork-wastes-building-blocks.gen.js`, its `.bbmodel`, screenshots in `design/texture-reviews/clockwork-wastes-building-blocks/`) and option C, the ashlar bond, as the void-alloy base surface everywhere. All of it is built here. No approved shape changed: every existing model was checked against its old export, texture names aside (142 block models and the OBJ models identical). Where D75 is silent, the build is provisional and named, and it's listed under "Choices needing the user's agreement" at the end of this section.

### The base surface, everywhere (`clockwork-wastes-materials.gen.js`)
- The diagonal sheen `va_tile` is gone from the mod. In its place:
  - `va_plain`: the crisp plain plate, the preview's `vB_plain`.
  - `va_bond_s/l/r`: the bond's 1 x 1 plate and the two halves of its 2 x 1 plate, the preview's `vC_*`.
  - `va_field`: the plain field with no plate edges, the preview's `vb_field`.
  All four are painted in the materials sheet with the grain and frame moved there, so they're pixel for pixel the preview's.
- `vaTile` is now the plain plate, so every texture painted on it is too: `va_seamed`, `va_chipped`, `va_cracked`, `chain_dark` and `cog`. The chipped and cracked faces are now exactly the preview's `vb_chipped` and `vb_cracked`.
- Every Wastes generator's `'va_tile'` became `'va_plain'`: city, city props, heart hall, insulator, lift, reactors, salvage and vent. This covers the Charging Alcove, Assembly Clamp, City Vault, Sliding Door, Conduit Pylon, Derelict Star Reactor, riser foot, lift deck and guides, and Wall Vent.
- Two blocks don't take `va_plain`, following the preview:
  - The **Pillar**'s chamfered faces take `va_field`.
  - The **Void-Alloy Plate** takes the bond.
- All the previews were regenerated: materials, city, city props, heart hall, insulator, lift, reactors, salvage, vent and building blocks. In the building-blocks preview, row C now shows the existing family on the new base, and its alcove is on `va_plain` like the mod's (it was `vb_field`; see choice 1).
- **Before and after sheet:** everything is in `design/texture-reviews/clockwork-wastes-base-swap/`.
  - `base-swap.gen.js` draws the mod's own models twice: once with the old textures (kept in `before-textures/`), once with the new.
  - For the shaped and complex models it adds a third copy on the frameless `va_field` (choice 1).
  - The scene is `base-swap.bbmodel`, open in Blockbench as the tab "base-swap", with clips `dormant` and `awake`.
  - Shots in `shots/` are at gameplay distance, eye height, from the fronts' side. They're tiled by `sheet.js` into `base-swap-sheet-dormant.png` and `base-swap-sheet-awake.png`: before, after, field from the left.
  - **Not yet seen by the user.**

### The bond in game (`VoidAlloyBond`)
- A bonded block keeps the plate on each side in two states:
  - `bond_ns`: the north face's plate, then the south face's.
  - `bond_ew`: the east face's, then the west face's.
  Each letter is s, l or r, giving 9 values each. Tops and bottoms are always 1 x 1.
- `VoidAlloyBond.plate(pos, side)` is the preview's `OPTS.C`: courses run l r s l r, shifted 0, 3, 1, 4, 2 by y mod 5, read left to right as the viewer facing the face sees it.
- The bond is set from position in three ways:
  - A player's placing sets it (`getStateForPlacement`).
  - The city's builder sets it before it sets the block (`CityApplier.put` and the deck plates in `ground`), which covers world generation too.
  - In a live level, anything else that places one (a command, a contraption, a piston) is put right in `onPlace` by `follow`.
- The exporter writes each bonded face as its own one-face model, chosen per face by a multipart. The Plate, the Void-Alloy Slab's double and the Scarred Plate's other faces share the same 18 face models; the Grille's faces have their own.
- Blocks with the bond: Void-Alloy Plate (now `VoidAlloyPlateBlock`), Void-Alloy Slab (for its double), Slotted Grille, Scarred Void-Alloy Plate.
- Plates already standing in a world from before keep the default bond (1 x 1 on every face) until they're placed again (choice 5).

### Exporter (`clockwork-wastes.export.js`, section "the building block set", after the city props)
- Every check is kept:
  - bounds -16..32
  - one-axis turns of 0, ±22.5 or ±45
  - no zero-width or off-texture UVs
  - no faceless elements
  - the z-fight check
  The building blocks' shapes are z-checked too: stairs, wall, grille lit, band lit and joined on every side, clamp wall and saddle, each scar with its glow, stained wall.
- It still exits 0 with **no z-fights**.
- A wall's tall side meeting its post's top on the block's top plane is vanilla's own template (both are culled by the block above, the only time a side is tall), so the wall check runs with low sides.
- No icon outline check is needed: the new blocks' items are their 3D models.
- The new building blocks take ambient occlusion like the vanilla blocks they're built beside (`blockModel`'s new `ao` option). The city's machines and props keep theirs off.
- It refuses to run if the building-blocks generator repaints any texture another block already uses.

### Blocks (`registry/ModWastes.java`, `buildingBlocks()`, after the Pillar in the creative tab)
| id | class | notes |
|---|---|---|
| `void_alloy_stairs`, `void_alloy_slab`, `void_alloy_wall` | `StairBlock`, `VoidAlloySlabBlock`, `WallBlock` | vanilla behaviour, states and turns (uvlock); `va_plain` on every face; the wall post's top `va_post_top`; the slab's double is the bonded Plate |
| `slotted_grille` | `SlottedGrilleBlock` (a `CityDecorLightBlock`) | `axis` with six values: x and z upright; `x_lying`, `z_lying`, `up_x`, `up_z` lying for fallen tops (the lying ones use a second model given an exact quarter turn about z, since a blockstate can only turn about x then y); bond; `lit` draws the dim pane; not a full occluder; a player's is upright by look, or lying when sneaking (`LYING_WHEN_SNEAKING`) |
| `void_alloy_band` | `VoidAlloyBandBlock` (lit) | `axis` (ring axis: y upright, x or z lying); six join states, one per side (only those round its axis count: a neighbour whose face against it is sturdy); flush fills; `lit` draws the edge-tone seam; upright unless sneaking (`LYING_WHEN_SNEAKING`, along the clicked face's axis) |
| `clamp_bracket` | `ClampBracketBlock` | `mount` (wall, saddle) and `facing`. Wall: on the clicked face. Saddle: a top click, jaws along the look. Outline is the strap, jaws and boss, or the plate and boss. Needs no support. Never lit |
| `scarred_void_alloy_plate` | `ScarredPlateBlock` (lit) | `facing` (the scar toward the placer), `scar` (chipped, cracked, heat), bond on its other faces; only a cracked one lights (dim); the item places `ITEM_SCAR` (chipped) and a Create Wrench cycles the scar |
| `stained_end_stone_bricks`, `cracked_…`, `…_brick_stairs`, `_slab`, `_wall` | `Block`, `StairBlock`, `SlabBlock`, `WallBlock` | three weighted variants (`seb_0/1/2`, 50/25/25 as the preview's `sebVariant`) as weighted models; `seb_top` on up faces; End Stone Bricks' strength |
| `key_marked_stone` | `KeyMarkedStoneBlock` | `facing`: the key's face, toward the placer; the other faces Scorched End Stone |
| `broken_makers_window` | `BrokenMakersWindowBlock` | the preview's pane (2 px thick, in the middle of its block); `axis` x or z; three weighted variants (`win_a/b/c`); cutout; glass sound; joins nothing |
| `patina_rib` | `Block` | `rib_patina` all round; copper sound |

- The lit ones (Grille, Band, Scarred Plate) are in `DERELICT_LIGHT_BE`'s blocks. They bind to their district like every city block (`CityApplier` binds any `CityLink.Member` placed with a district) and light only while it's fed. They cast no light (D63), and a player's stays dark.
- Every block drops itself (a double slab drops two) and is mined with a pickaxe.
- The stairs, slabs and walls are in vanilla's `#stairs`, `#slabs` and `#walls` tags, for blocks and items. A wall joins only walls in `#walls`.
- Lang names: Void-Alloy Stairs, Slab and Wall; Slotted Grille; Void-Alloy Band; Clamp Bracket; Scarred Void-Alloy Plate; Stained End Stone Bricks; Cracked Stained End Stone Bricks; Stained End Stone Brick Stairs, Slab and Wall; Key-Marked Stone; Broken Makers' Window; Patina Rib.
- No recipes (choice 11).
- None is on Carry On's blacklist, as with the Seamed Plate.
- The city doesn't place the new blocks yet. The building kit's catalogue (D71) is next.

### Tests
- **Unit** (`BuildingBlockSetAssetsTest`, 5 tests):
  - Every new block has its blockstate, models (with parents), textures, item, name, self-drop loot, pickaxe tag and shape tags.
  - Every bond value (81 pairs) draws exactly one plate on each face: the Plate, the double Slab, and the Scarred Plate (its scarred face the scar, turned, and the glow only when cracked and lit).
  - Every grille orientation draws its body, six bonded faces turned alike, and its pane when lit. Each band axis draws a fill only on a joined side round its axis.
  - No Wastes model uses `va_tile`, and its PNG is gone.
  - The bond's courses are whole (a left half always has its right half beside it on every face) and match the preview's (`lrslr`, then `lrlrs`).
- **Game** (`WastesCityGameTests`, 2 tests):
  - `buildingBlocksLightWithTheirDistrict`: the grille, band and cracked plate light with their fed district and go dark unfed; a chipped plate and a grille of no city never light; no light is cast.
  - `voidAlloyBondFollowsPosition`: plates, double slabs, grilles and scarred plates set with the wrong bond on purpose are put right, every one; 2 x 1 plates are whole; the city's builder places a plate on its bond.
- **All 963 unit tests and all 318 game tests pass** (`./gradlew compileJava test runGameTestServer`). The exporter exits 0 with no z-fights.

### Previews for D74 (not in the mod)
- **The Derelict Cable's corner knot at 2.5 px:**
  - Built by `design/models/clockwork-wastes-cable-knot.gen.js` into `.bbmodel`, open as the Blockbench tab "clockwork-wastes-cable-knot".
  - Shots in `design/texture-reviews/clockwork-wastes-cable-knot/`, before left and after right; the `20-pair-*` images are the close-ups.
  - Before: 20 knot z-fights, at every L, T, cross, wall corner and riser-foot side arm. After: none.
- **The Charging Alcove's flat icon, redrawn with the empty corner shelf on the plain plate:**
  - Drawn by `design/texture-reviews/clockwork-wastes-icons/charging-alcove-icon.js`.
  - It writes `charging-alcove-icon-review.png` (8x, beside the approved icons), `charging-alcove-icon-sizes.png` (16, 32, 64 px), and `charging_alcove_new.png`, plus `charging_alcove_new_seams.png` as an option.
  - In Blockbench: `charging-alcove-icon.bbmodel`, tab "charging-alcove-icon".

### Choices needing the user's agreement
Nothing below was decided silently: each is built as one named constant or rule.

The base swap:
1. **Shaped and complex models: framed plate or frameless field (built: `va_plain`, the framed plate, as asked; `vaTile` in the materials sheet).** On the city's machines and props the frame draws a lit and dark line at every block line inside one model: across the Charging Alcove and Sliding Door, up the Pylon and the lift guides. The preview's own alcove used the frameless `va_field`. The sheet's third column shows every complex model on the field. Options:
   - (a) framed everywhere, as built
   - (b) the field for the city's machines and props, the framed plate for stairs, slabs and walls only (as the preview did)
   - (c) the field everywhere but the Plate's bond

   Recommend (b): a cast machine reads as one piece, and the frame belongs where a block meets a block.
2. **The small parts' `va` texture still has the diagonal three-band sheen inside its facet frame:** clamps, bezels, door panels, vault blades, the knot. D75 named only `va_tile`, so `va` was kept, as the preview kept it on the Clamp Bracket. Options: keep; repaint its field with the new grain, keeping its frame (previewed first). Recommend the repaint, for one surface.
3. **Seamed Plate (built: its seams on the plain plate, no bond).** Options: a bond under the seams (the seams would cross plate joints). Recommend as built.
4. **Pillar (built: the plain field, as the preview drew it).** Recommend keep.
5. **Plates already in a world** keep the 1 x 1 bond until placed again. A rebuilt city has the bond. Options: leave; put them right on load (a random tick or a chunk fixer). Recommend leave: only test worlds have them.

The new blocks:
6. **Grille and Band orientations and placing (built: `SlottedGrilleBlock.LYING_WHEN_SNEAKING`, `VoidAlloyBandBlock.LYING_WHEN_SNEAKING`).**
   - The grille's `axis` has six values. The band's `axis` is its ring's axis.
   - Upright by the player's look; lying when sneaking (the grille looks out of the clicked face, the band lies along it).
   - Lying, the whole model turns with its textures, as the preview's fallen top did.

   Options: lying states for the city only; a wrench to tip them over. Recommend as built.
7. **The Scarred Plate's scar for players (built: `ScarredPlateBlock.ITEM_SCAR` chipped; a Create Wrench cycles chipped, cracked, heat).** Options: three items; a random scar when placed. Recommend as built.
8. **The Band joins any block whose face against it is whole** (the preview's "solid neighbour"), not only bands and plates. Recommend keep.
9. **The Clamp Bracket needs no support** (as the Brass Plaque). A top click makes a saddle, a side click a wall strap, an underside click a strap facing the player. Recommend keep.
10. **The Broken Makers' Window:**
    - a 2 px pane in the middle of its block, as the preview drew it, joining nothing (not a glass pane)
    - three weighted breakages at random
    - glass sound, hardness 1.5, pickaxe
    - drops itself, where vanilla glass drops nothing without Silk Touch

    Options: join like iron bars; drop nothing. Recommend as built.
11. **Recipes (built: none; D37's default keeps void-alloy finite per city).** Should players be able to craft the set, and from what? Options:
    - (a) none
    - (b) Stonecutter recipes from the Void-Alloy Plate for its stairs, slab, wall, band, grille and clamp (shapes from salvaged plates, still finite)
    - (c) Stained End Stone Bricks from vanilla End Stone Bricks (eight round a brown dye, or smelted), with stonecutter shapes; the Patina Rib from copper; the window from iron bars and glass

    Recommend (b) and (c), with the Key-Marked Stone and Scarred Plates city-only.
12. **Variant weights (built: stained bricks 50/25/25 as the preview's `sebVariant`; window breakages even).** In game they're vanilla's weighted random models, not the preview's position hash: the same mix, a different pattern. Recommend keep.
13. **Ambient occlusion (built: on for the new building blocks, off for the city's machines as before).** Recommend keep.
14. **Carry On (built: none of the new blocks blacklisted, as the Seamed Plate).** Recommend keep.

Found on the way (the knot preview's agent; not changed):
15. **Derelict Cable wall arms are drawn on the wrong half.**
    - The exporter's `derelict_cable_arm_south` is turned `y 90`, so it runs north, but `turnFor` treats it as running south.
    - On every wall, the `up` and `down` arms swap: a wall corner or a run's end points the wrong way, with a gap.
    - Screenshots `90-*` and `91-*` in `clockwork-wastes-cable-knot/`.
    - Options: fix (y 270) with a game test for a wall corner; leave it. Recommend fix.
16. **The knot to 2.5 px.** Recommend widen.
17. **The alcove icon's details (provisional):**
    - the shelf drawn on the icon's right, as the family draws east
    - no plate seams on the icon
    - the contact strip moved one pixel left
    - shoulder rests left off

## D76: block set follow-ups (built 2026-10-29)

D76 settled D75's choices 1, 2, 11, 15, 16 and 17 and D72's 14 and 17. All of it is built here except the `va` repaint, which is previewed and waits for the user. No approved shape changed. Where D76 is silent, the build is provisional and named, and it's listed under "Choices needing the user's agreement" at the end of this section.

### The frameless field on machines and props (D75 choice 1, option b)
- `clockwork-wastes-materials.gen.js` has one switch, `VA_BODY = 'va_field'`. Every machine and prop body that was `va_plain` now uses it, in the city, city props, lift, reactors, salvage and vent generators, and in the exporter's particle textures. That covers the Charging Alcove (body, shelf top, trinkets, vines, contacts), City Vault (and its item OBJ), Sliding Door (blocks, leaves, seam, chain, jamb glow), Conduit Pylon, Derelict Star Reactor (body, glass, arm), Lift deck and guides, Wall Vent (and its item OBJ), Assembly Clamp uprights (stages 1-5) and the riser foot's guard.
- `va_plain`, the framed plate, stays only on the Void-Alloy Stairs, Slab and Wall. The bond stays on full plates.
- Preview-only full blocks (the heart hall's floor and walls, the vent scene's wall, the alcove wall course, the city and insulator previews' pillar stand-ins) are unchanged.
- **Checked:** against a snapshot of the mod's assets taken before D76, the 56 changed model files (JSON, OBJ, MTL) are identical once `va_plain` is read as `va_field`. No geometry, UV or other texture changed. All previews were regenerated: materials, city, city props, lift, reactors, salvage, vent, insulator, heart hall, building blocks and cable knot.

### The small parts' `va`, repainted (D75 choice 2): previewed, NOT in the mod
- `vaGrain` in the materials sheet is the new texture. It keeps the frame pixel for pixel: a 1 px `#34304a` lit edge on the top and left and a `#16151c` edge on the bottom and right, with the lit edge winning at the two mixed corners. Its field is the house grain: `#232130`, `#272534` and `#2b2939`, mostly the middle shade, seed 541. There is no diagonal sheen.
- It's switched by `VA_GRAIN` (false), or by `WASTES_VA_GRAIN=1` in the environment. While it's off, the mod's `va.png` is unchanged, and so are the two textures painted on `va`: the Substation's `sub_sigil` and the lift call plate's `lift_sigil`.
- **Once approved:** set `VA_GRAIN = true` and run the exporter. That changes `va.png`, `sub_sigil.png` and `lift_sigil.png`, and nothing else.
- **Preview** (in `design/texture-reviews/clockwork-wastes-va-parts/`):
  - `va-parts.gen.js` paints both versions with the generators' own painters. It checks that the sheen painter matches the mod's files exactly, and that the frame is unchanged.
  - It writes `va.preview.png` (the new `va`), `after-textures/` and `before-textures/`.
  - `va-texture-before-after.png` shows `va`, `sub_sigil` and `lift_sigil` at 16x: the sheen on top, the grain below.
  - The scene is `va-parts.bbmodel`. It draws the mod's own models twice, before (left) and after (right): Assembly Clamp stages 5 and 2, City Vault with its six blades closed, Sliding Door, Clamp Bracket (wall strap and saddle), Derelict Cable floor corner and riser foot, Charging Alcove, Lift and call plate, Substation, Conduit Pylon, Derelict Star Reactor, Wall Vent, Insulator and street lamp.
  - `shots.js` opens it in Blockbench as the tab **"va-parts"** (clips `dormant` and `awake`). It takes gameplay-distance shots and dormant close-ups into `shots/` and `shots/close/`.
  - `sheet.js` tiles them into `va-parts-sheet-dormant.png`, `va-parts-sheet-awake.png` and `va-parts-sheet-close.png`.
  - **Not yet seen by the user.**

### The Derelict Cable
- **Wall arms (D75 choice 15):** `derelict_cable_arm_south` and its glow are now turned `y 270` (east to south). At `y 90` they ran north, so on every wall the up and down arms swapped. This matches the knot agent's screenshots: `90-*`/`91-*` show the `y 90` gap, and `14-*` shows the `y 270` corner whole. Only those two models changed.
- **Knot (D74, D75 choice 16):** `cableKnot()` in the exporter is now 2.5 px wide (x 6.75-9.25), as previewed. The floor cable and the riser foot share it.
  - The z-fight check now covers the knot against every arm: east, west, north, south, and all four at a cross, each with its lit line. It also covers the riser foot with each floor arm it can take (north, east, west).
  - The old 2 px knot fails this check with 12 fights; the new knot passes with none.
- The knot preview generator's notes now say it's in the mod.

### The Charging Alcove's icon (D72 choice 14, D75 choice 17)
- `ICONS.charging_alcove` is the approved plain version from `charging-alcove-icon.js`: rows, palette, and the field's grain through a new per-pixel `paint` hook on the exporter's icons. It matches `charging_alcove_new.png` pixel for pixel.
- The exporter also writes the review folder's `charging_alcove.png`, so phase 1's icon is kept as `charging_alcove_old.png`, and the icon script now reads its "before" from there.

### Recipes (D75 choice 11; written by the exporter, section "the building block set", into `data/comedymonkeysaddons/recipe/wastes/`)
JEI shows them as it shows any vanilla crafting, stonecutting or smelting recipe. Shapes and counts are provisional.

| Makes | How | From |
|---|---|---|
| Void-Alloy Stairs ×1, Slab ×2, Wall ×1, Band ×1, Slotted Grille ×1, Clamp Bracket ×1 | Stonecutter | 1 Void-Alloy Plate |
| Stained End Stone Bricks ×8 | crafting `###/#D#/###` | 8 End Stone Bricks, 1 brown dye (`c:dyes/brown`) |
| Stained End Stone Brick Stairs ×4 | crafting (vanilla stairs) | 6 Stained End Stone Bricks |
| Stained End Stone Brick Slab ×6 | crafting `###` | 3 Stained End Stone Bricks |
| Stained End Stone Brick Wall ×6 | crafting `###/###` | 6 Stained End Stone Bricks |
| the same stairs ×1, slab ×2, wall ×1 | Stonecutter | 1 Stained End Stone Bricks |
| Cracked Stained End Stone Bricks | smelting (0.1 xp, 200 ticks) | Stained End Stone Bricks |
| Patina Rib ×2 | crafting, two stacked (as the Quartz Pillar) | any copper block: plain, exposed, weathered or oxidized, waxed or not |
| Broken Makers' Window ×4 | crafting `BGB/BGB` | 4 Iron Bars, 2 glass (`c:glass_blocks/colorless`) |

Key-Marked Stone and the Scarred Plates have no recipe, nor do the Plate, Pillar and Seamed Plate (D37: void-alloy stays finite per city).

### Tests
- **Unit:**
  - `BuildingBlockSetAssetsTest.machinesAndPropsTakeTheFrameslessField`: no model the machines and props draw (blockstates and renderer parts) uses `va_plain`, and each body uses `va_field`. The stairs, slab and wall models use `va_plain` and never the field.
  - `WastesAssetsTest.cableKnotClearsEveryArm`: the knot is x 6.75-9.25. At every floor corner, T and cross (9 states), no drawn face of it lies within 0.2 px of a drawn face of any arm or lit line drawn with it, each turned as the blockstate turns it.
- **Game (`WastesGameTests`):**
  - `cableWallArmsPointTheirWay`: a player-placed run along a wall turns a corner up it. Each block joins the right sides, and the arm models its blockstate draws, turned as vanilla's BlockModelRotation turns them, point those ways. Then every surface with each single arm, dark and lit.
  - `buildingBlockRecipes`: through the server's recipe manager, every recipe above makes what it should. Each grid matches exactly one recipe, so nothing clashes with vanilla's or Create's. The plate and the stained bricks cut into exactly their shapes. Nothing makes Key-Marked Stone or a Scarred Plate.
- **All 965 unit tests and all 320 game tests pass** (`./gradlew compileJava test runGameTestServer`). The exporter exits 0 with no z-fights.

### Choices needing the user's agreement
Nothing below was decided silently: each is built as one named constant or rule.

1. **The `va` repaint (preview, not in the mod).** See the sheets in `clockwork-wastes-va-parts/` and the Blockbench tab "va-parts". Options:
   - (a) approve: set `VA_GRAIN = true` and export (va, sub_sigil and lift_sigil change)
   - (b) keep the sheen
   - (c) the grain at another weight (more of the light shade, say), previewed again

   Recommend (a): the clamps, bezels, door panels and vault blades then read as the same cast metal as the bodies they sit on.
2. **What counts as a machine or prop (built: `VA_BODY` on the bodies listed above).** The Wall Vent and the riser foot's guard are included. Both are set into or against walls, so the field means a vent's jambs show no frame where they meet the plates beside them. Options: the framed plate on the vent only. Recommend as built.
3. **The Clamp Bracket's count (built: one a plate).** Options: two a plate, as it's a small strap. Recommend one, since void-alloy is finite.
4. **Cracked Stained End Stone Bricks by smelting (built).** D76 names only the bricks and their shapes. Options: no recipe (city-only, as the Scarred Plates). Recommend smelting, as vanilla cracks its bricks.
5. **The Patina Rib (built: two copper blocks stacked, any weathering, waxed or not, gives two).** A 2 × 2 of copper blocks is vanilla's cut copper, and three ingots in a column is the Lightning Rod, so both clash. Options:
   - only weathered or oxidized copper, for the patina
   - also a Stonecutter recipe, a copper block giving one or four (as cut copper)
   - copper ingots, in a shape nothing else uses

   Recommend as built.
6. **The Broken Makers' Window (built: `BGB/BGB`, 4 bars and 2 glass give 4).** Options: glass panes in place of glass; fewer windows a craft. Recommend as built.
7. **Recipe unlocks.** As every other recipe in the mod, these have no recipe-book advancement. JEI lists them all, and the vanilla recipe book shows them once crafted. Recommend keep, for consistency.

Found on the way (not changed):

8. **The bots' bodies still paint their own diagonal sheen.** `clockwork-wastes-bots.gen.js`'s `vaPaint` uses three bands by `(i + j)` on the Scuttler, Sentry and others; these are entity textures, outside `va`. Options: repaint them on the grain, previewed first; leave. Recommend a preview, if "no diagonal sheen anywhere" is meant to include the bots.
9. **The Worker and Resistance Shells (salvage OBJs) use `va` too**, so approving the repaint changes them. The sheet doesn't show them (they're baked OBJs). Recommend no separate preview: they take the same texture as the parts shown.

## Deferred to later phases
- **Phase 2 leftovers:** Ponder.
- **City structure:** built (see "The city"); in world generation once the user approves it.
- **Bots:** built (phase 3); see its "Choices needing the user's agreement".
- **Sounds** (last, on the staging ladder). Salvage uses the block's break sound for now.
- JEI info pages for the Unknown Device and Clockwork Scrap (P27).

## Catalogue agent — paused 2026-10-29

The building kit (D71, D73, D75) and `/wastes buildcatalogue`, paused at the user's power-down. Nothing committed.

### Done
- **Kit layer** (`content/wastes/city/kit/`): `Site` (a local frame in a `CityPlan`, any turn or mirror; blocks turned by facing/axis/side/hinge, stairs, bands and pipes re-joined by `CityApplier`), `Model` (a building drawn locally, its marks' causes, `settle()`), `Kit` (faces, plinth and step, piers, windows and sills, corbels, slab pitch, chamfer, soot fan, strike, rubble cone), `Masonry` (one stone per district, shared roof/window/soot blocks), `Wear` (intact to 22, worn 23-45, damaged 46-70, ruined beyond), `ModelChecks` (anti-monoblock share 65%, excluded blocks, marks near causes, held up).
- **Builders, every type in the states the brief sets:** `Shed` (makers' shed, all four states; also the control hut, signal box, lot A's and N1's sheds; walnut door, lined cut, or lined cut with the district Sliding Door), `FurnaceShed` (worn/damaged with scorch fan and toppled chimney/ruined; chimney, blast furnace, bustle pipe, launder), `Terrace` (intact/worn/damaged front-less end house/ruined headstones), `Workshop` (sawtooth; intact/worn/damaged collapsed teeth/ruined), `Tower` (slotted 3x3 and 5x5 on old plinth: intact/worn chips/damaged snapped with fallen top/ruined stub; through-roof; sky tower with T-head, intact/worn only), `VaultHouse` (worn/damaged), `AlcoveWall` (honeycomb, switchback slab ramps, barricade, lift, trinkets and vines; the split alcove tower), `Gantry` (intact/worn craters/damaged buckled leg), `Yard` (Substation yard, transformer and arc scorch, tray, lattice mast with riser, hut; `cornerMast` mock crossroads), `Drum` (octagon drum with buttresses, windows, portals, plaques, Patina Rib dome ribs, shingles; the spire with collar, grille runs, bands, crown, needle), `Siege` (zig-zag salient, strongpoint, key stones, shells, breach, soot, craters), `Canopy` (Rail Yard platform canopy).
- **`/wastes buildcatalogue`** (`Catalogue`, `CatalogueBuild`, `WastesTestSetup`): 37 exhibits in five rows going north from the player, fronts south, a sign on each pad, on its own End Stone island, bound to the catalogue's own city record (`/wastes wake all` then `/wastes heart 60` lights it); writes camera marks to `wastes-catalogue.json` in the game folder.
- **The swap, ready but not made:** `CityLayout.BUILDING_KIT = false` is the one switch; `KitCity` draws every box's building in its lot (yard, sky tower, vault house, lot A sheds, set pieces, corner masts, heart drum and spire, siege salients); `CityLayout` branches on `p.kit`. `/wastes buildcity <seed> kit` previews it without flipping the switch. `CityPlan` gained `drawn`, `bindTo`, `buildings`, `of(..., kit)`. `CityApplier` now joins stairs, bands and pipes after placing.
- **Tests** (`gametest/WastesKitGameTests`): `everyKitBuildingKeepsTheGrammar`, `damageFollowsTheRings`, `catalogueExhibitsStandApart`, `theSwapFitsTheLots`, `catalogueBuildsAndBinds`. Last full run (`./gradlew compileJava test runGameTestServer`, then `compileJava runGameTestServer` after one test fix): **965 unit tests and all 325 game tests pass.** After that only the `buildcity kit` option was added; `compileJava` passed.
- Dev tool: `ShotRunner` gained a `solo-` single-shot mode, releases the mouse during shots and sizes a small window up for them.

### Half-done
- **Screenshots:** none in `design/texture-reviews/clockwork-wastes-buildings/` yet. In `run/screenshots/cmca/` from the current catalogue (built at 9000, 100, 3000 in the End of the "Wastes Catalogue" save): the six dormant join close-ups (`solo-dormant-join-*`). Earlier review passes (dormant three-quarters and close views) were taken on older code and deleted. The driver is `C:\Users\bones\AppData\Local\Temp\claude\e--Repos-Flight-Goggles-Compat\a5290aa5-3ebf-4819-bc66-776e11887ecd\scratchpad\shoot.js` (contact sheets: `sheet.ps1` beside it); scratchpad files may not survive.
- **Visual review not finished:** first passes showed the sheds, furnace, terrace, workshop, drum and towers reading with relief; void-alloy reads near-black in End light dormant (lit shots not yet taken). Not yet reviewed closely: awake glows, street-level shots, the swapped city from a distance.
- Brief shot 5 (a lit tower beside an Electric Lights lamp) not taken.

### Choices needing the user's agreement (provisional as built)
1. Ridge roll: Patina Rib (full block) instead of the brief's weathered cut copper slab, which read mint (`Masonry.ridge`).
2. Anti-monoblock share: 65% of a face's seen blocks (`ModelChecks.MAX_SHARE`); lattice masts and the siege line exempt.
3. Lintel band as a full dressing-stone course (`Masonry.band`: polished cut scoria / tuff, cut deepslate) rather than a slab in the wall plane.
4. Furnace hall 16 long with three mouths (brief 17-19 / four mouths doesn't fit lot B); tap hole at y 2 (brief y 0) so the launder stands on legs; the toppled chimney 4 blocks plus cap, across street B (toward the heart).
5. Terrace 6 deep (even ridge) not 7; eaves on the corbels so the 1:1 roof tops out at 10-11.
6. Long workshop 9 wide (two fit lot B beside the 3x3 corner mast).
7. Vault house door at x 3-4 of 9 (can't centre); no ground step round its plinth (it would reach the street).
8. Drum: two windows per axis flat (a portal or the heart vault takes the middle); diagonal flats stepped with the courses but no windows; west heart door one block south of the old one.
9. Line shaft hung on chains from the tie beams (Create brackets can't be placed as blocks).
10. Gantry: the hoist over clamp 5 (brief says both "halfway" and "over clamp 5"); the lens is Tinted Glass; clamp 1 "knocked over" shown turned and shoved (a clamp can't lie down). Floor craters are sunk slabs and soot (a scarred plate's scar only shows on a side).
11. Split tower: no "fallen alcove lying on its back" (needs a new lying alcove look, previewed first).
12. Canopy posts: polished blackstone wall (options: metal girder, walnut fence).
13. Siege works in the city: four salients a side of the ring at 84; the old winch, barricades kept.
14. Small sheds (hut, signal box, N1 shed) have no corbel course, eaves 1 over.
15. Sky tower T-head as twin pillars at z 1 and 3 with a band web; its sky-cable insulator hanging under the heart-side end.
16. Swap props moved for the kit's walls: shed 2's alcoves (u 26), its slumped shell inside (u 19), the N1 press and shell, two Sentries' walls.

### To resume
1. Relaunch `./gradlew runClientWorld "-Pworld=Wastes Catalogue"`; the catalogue at 9000, 100, 3000 (End) is current, or rebuild at a fresh x (`/wastes buildcatalogue` in the End, spectator).
2. Take the set: dormant and awake (`wastes wake all`, `wastes heart 600`), per exhibit street + three-quarter (use `tp ... facing <centre>`; `CatalogueBuild` now writes it), five row distance shots, the join close-ups, and a kit-city distance set (`/wastes buildcity 0 kit`).
3. Review every shot; fix flat boxes, one-block areas, noise or colours; rerun the kit tests.
4. Copy the shots into `design/texture-reviews/clockwork-wastes-buildings/` with a contact sheet.
5. Run `./gradlew compileJava test runGameTestServer`, then report.

## Main session — paused 2026-10-29

- **Branch:** `clockwork-wastes`, nothing committed yet. Whether to make a first commit (only Wastes files) is still to be asked.
- **Landed this session (D67–D78):** the block set (D75), the field on machines plus the `va` repaint (D76/D77), and bot grain version A (D78, entity textures exported). Carry On: the Assembly Clamp is carryable again (D74).
- **Still to run:** `./gradlew test` after the bot texture export (the catalogue agent's last full run was 965 unit and 325 game tests, all passing; it came before or around the export).
- **Next, in order:**
  1. Finish the catalogue screenshots (see the catalogue agent's pause section above).
  2. Bring the 11 provisional kit choices to the user.
  3. On approval, flip `CityLayout.BUILDING_KIT` (D71).
  4. In-game walk via `/wastes buildcity`.
  5. Ponder, JEI, codex page, adversarial review, then sounds last.
