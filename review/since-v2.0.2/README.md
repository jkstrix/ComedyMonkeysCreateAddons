# Review of everything since v2.0.2

Scope: the 59 commits between tag `v2.0.2` and `e00d819` (2.5.0 work: Fluid and Power Lines, Batteries, Monster Cages and Fish Tanks, Helper Bot, Clockwork Juggernaut, Electric Lights, Clockwork Dynamo and Tubes, and the Flywheel instancing pass). About 50,000 new lines of Java in 289 files.

How it was done: a lint compile of main and test sources with every javac warning on, then eight read-only reviewers, one per area, each told to confirm every claim against the code and the Create 6.0.10 / Flywheel 1.0.6 / NeoForge 21.1.248 jars before writing it down. Each finding below has its own file in [bugs/](bugs/) with a junior-developer-level fix.

Totals: 4 high, 10 medium, 33 low (47 findings). Compiler warnings: 172 in total, grouped into the ten WARN entries.

## Status after the fixes (2026-10-31)

All findings were worked on the same day. Every fix has a test where its behaviour could be tested. Four fresh reviewers then attacked the fixes; their nine findings were fixed too. Final run: javac -Xlint:all reports no warnings (it is now on in build.gradle), 999 unit tests and 303 game tests pass. The fixes were committed as f3ea3da; OBS-01 and the vanilla chest visual came after.

| ID | Status | What changed |
|---|---|---|
| CAGE-01 | fixed | Shift-click on the jar goes through the cage's own extract, which refuses during lock-down. The wrench screen still opens in lock-down (maintainer's choice). |
| CAGE-02 | fixed | The Netherite Monster Cage is in create:non_breakable: drills and saws can't break it (maintainer's choice). |
| CAGE-03 | fixed | A jar only releases or enters a cage with a monster its kind can hold. |
| CAGE-04 | won't fix | Accepted by the maintainer: chow from bone drops is fine. |
| CAGE-05 | fixed | Maintainer's rule: a non-blue captive's tank can roll one blue at a time (shown in the window, scooped first once grown); a blue captive breeds all blue; old saves with a blue captive load all blue. |
| CAGE-06 | fixed | No allocation in the per-tick player check while a field is down. |
| CORE-01 | fixed | A placed Battery's charge is clamped to the larger of the two configured capacities; the component codecs clamp to 0..25,000,000. |
| ELEC-01 | fixed | Battery and paper stores answer simulate for this tick only; Power Lines plan a second through their own planReceive/planExtract. |
| ELEC-02 | fixed | A displaced or stale formed block only resets itself; unform matches master, height, facing and formedAt. |
| ELEC-03 | fixed | Carry On pickup re-forms the rest of the tower and keeps its cables; a contraption still doesn't. Also fixed: a fresh empty block entity was being created at the removed block. Pending entries are cleared on server stop. |
| ELEC-04 | fixed | A partly loaded tower checks its four corner chunks first and allocates nothing. |
| ELEC-05 | fixed | Generators compare paper kinds without building a record. |
| FLY-01 | fixed | The chest's visual is remade on the client whenever it opens or shuts (no per-frame cost for closed chests). |
| FLY-02 | fixed | Festoon strings are a Flywheel visual laid out once (FestoonVisual), with geometry shared with the fallback renderer (FestoonGeometry); the fallback no longer allocates per frame; the crack overlay still shows while mining. |
| FLY-03 | fixed | A capped tube re-walks only when a change happened within reach of it. |
| FLY-04 | fixed | The Receiver caches its tubes' block entities and the tubes behind each; a full output retries every 4 ticks (maintainer's choice). |
| FLY-05 | fixed (harvester) | The harvester's generic plant is a Flywheel part when real plants can't draw it. The clock's workstation stays on the renderer (maintainer's choice). |
| FLY-06 | fixed | No Vec3 per frame in PartsVisual; Fluid Line Intakes get the idle visual. |
| HBOT-01 | fixed | Status is sent only when it changes; late joiners get it from the carriage's spawn data. |
| HBOT-02 | fixed | Render types come from the prebuilt table. |
| HBOT-03 | fixed | Pick-block keeps the bot's name as well as his colour. |
| JUG-01 | fixed | He crumbles only blocks the ruin itself built (its pieces, and the field's own columns, vines and moss, checked against what generation laid). Still no drops (maintainer's choice). A ruin placed with /place structure no longer crumbles. |
| JUG-02 | fixed | Fill, comparator, tooltip and renderer are clamped, so an over-capacity engine can't crash clients. |
| JUG-03 | fixed | The player kill and the killing blow survive a save during the death clip; the kill pays out exactly once even across a save between death and removal. If the killer is offline the loot still drops with no credit (maintainer's choice). |
| JUG-04 | fixed | While standing still he snaps back from outside shoves, by position and by velocity (maintainer's choice). |
| JUG-05 | fixed | The leap aim is recomputed every 4 ticks of the coil; the take-off check stays fresh. |
| JUG-06 | fixed | Cogs and blows only hit survival players, his target, and whatever has hit him this fight. |
| JUG-07 | fixed | Wound Up, Run Down and Tightly Wound added; who earns them, icons, frames and text confirmed by the maintainer. |
| LIGHT-01 | fixed | Runs keep their piece list until the run changes; the change counter is per level and server-only. |
| LIGHT-03 | fixed | Every festoon hook, new or already there, goes through the block-place event (except the hook the player clicked); a refusal hangs nothing and costs nothing. |
| LINE-01 | fixed | Each Wellspring that feeds a far generator keeps its own paper record (stock, rate, fluid). Generators on paper burn that stock, shared by income, and the real tank is drained by the same amount when the Wellspring loads again. A follow-up after review also debits the record when a Fluid Line drains the Wellspring while it is loaded but not ticking. |
| LINE-02 | fixed | Splitter priorities turn with contraptions and schematics; an encasing keeps its axis too. |
| LINE-03 | fixed | Fluid networks check their chunks once a tick and only walk their pieces when the display can change. |
| LINE-04 | fixed | Dynamo paper rates use an index of who last saw each source and a per-tick cache; no SavedData lookups per call. |
| LINE-05 | fixed | A post or battery tower left behind drops a span whose partner was carried off, at once; only the owner end refunds the wire. |
| LINE-06 | fixed | A turbine casing stops pointing at a controller that was carried off, unless the controller's chunk is unloaded (on paper). |
| OBS-01 | fixed | Cause: Veil's coloured light lights each pixel from its albedo and normal buffers, but Flywheel's shader never wrote them and Flywheel drew with only the plain screen bound, so what was behind an instanced part showed through it. Fix, only where the coloured light runs through Veil: Flywheel's common fragment shader is given Veil's albedo (location 1) and view-space normal (location 2) outputs as it loads (VeilFlywheelOutputs, FlywheelSourceFileMixin), and Veil's buffers are bound around Flywheel's draw (LightGlow, LevelRendererVeilFlywheelMixin). If Flywheel's shader ever changes, the patch skips itself and logs a warning. Checked in the maintainer's own scene: the ghost of the grass and chest behind the Helper Bot is gone and the frame matches the Flywheel-off picture. Vanilla chests now also draw through Flywheel (VanillaChestVisual), at the maintainer's request. |
| WARN-01 | fixed | The boss is spawned through EventHooks.finalizeMobSpawn. |
| WARN-02 | fixed | stack.getEnchantmentLevel everywhere. |
| WARN-03 | fixed (suppressed) | The six pick-block overrides keep the 3-argument method with a stated suppression: Create's Display Link screen still calls it directly, so moving to NeoForge's 5-argument hook broke its icons (caught in review). |
| WARN-04 | fixed | Every form of the game's chunk check on a LevelReader is deprecated, including the one the finding suggested, so all call sites go through one helper, content/Loaded, which holds the single suppression. |
| WARN-05 | fixed | The ruin's ground test uses isFaceSturdy; dirt paths, farmland and bottom slabs no longer count as ground. |
| WARN-06 | fixed | Monster Chow uses the supplier form. |
| WARN-07 | fixed | Raw generic arrays carry a stated suppression; 14 redundant casts removed. |
| WARN-08 | fixed | 57 classes nothing extends are now final; the two subclassed constructors carry a stated suppression. |
| WARN-09 | fixed | Override-only methods and vanilla's mirror pattern carry a stated suppression; build.gradle now compiles with -Xlint:all and the tree has no warnings. |
| WARN-10 | fixed | Atlas constant, sound lookup and test helpers replaced; the item-sprite and JEI legacy calls carry a stated suppression. |

## By severity

| ID | Title | Area | Kind |
|---|---|---|---|
| **HIGH** | | | |
| [CAGE-01](bugs/CAGE-01-shift-click-dupes-the-jar-in-a-locked-down-netherite-cage.md) | Shift-click dupes the jar in a locked-down Netherite cage | Monster Cage & Fish Tank | exploit |
| [ELEC-01](bugs/ELEC-01-battery-simulate-answers-lie-20x-cables-destroy-energy.md) | Battery simulate answers lie 20x, cables destroy energy | Battery, Dynamo & generators | broken |
| [FLY-01](bugs/FLY-01-upgraded-chest-lid-vanishes-while-open-flywheel-on.md) | Upgraded Chest lid vanishes while open (Flywheel on) | Flywheel & Clockwork Tubes | flywheel |
| [LINE-01](bugs/LINE-01-paper-fuel-income-is-never-bounded-by-the-wellspring-free-po.md) | Paper fuel income is never bounded by the Wellspring: free power from stock | Fluid & Power Lines | exploit |
| **MEDIUM** | | | |
| [CAGE-02](bugs/CAGE-02-create-drills-break-a-locked-down-foreign-netherite-cage.md) | Create drills break a locked-down / foreign Netherite cage | Monster Cage & Fish Tank | exploit |
| [ELEC-02](bugs/ELEC-02-a-stale-formed-block-tears-down-a-live-tower-sharing-its-mas.md) | A stale formed block tears down a live tower sharing its master | Battery, Dynamo & generators | broken |
| [FLY-02](bugs/FLY-02-festoon-strings-rebuilt-on-the-cpu-every-frame.md) | Festoon strings rebuilt on the CPU every frame | Flywheel & Clockwork Tubes | flywheel |
| [FLY-03](bugs/FLY-03-capped-tubes-re-walk-the-whole-network-on-any-change-anywher.md) | Capped tubes re-walk the whole network on any change anywhere | Flywheel & Clockwork Tubes | performance |
| [JUG-01](bugs/JUG-01-ruin-juggernaut-destroys-players-blocks-near-his-hall-no-dro.md) | Ruin Juggernaut destroys players' blocks near his hall, no drops | Clockwork Juggernaut | exploit |
| [JUG-02](bugs/JUG-02-spring-engine-over-capacity-crashes-every-client-that-render.md) | Spring Engine over capacity crashes every client that renders it | Clockwork Juggernaut | broken |
| [JUG-03](bugs/JUG-03-mainspring-lost-if-the-world-saves-during-his-death-animatio.md) | Mainspring lost if the world saves during his death animation | Clockwork Juggernaut | broken |
| [LIGHT-01](bugs/LIGHT-01-run-lights-rebuild-their-piece-list-every-tick-and-every-run.md) | Run lights rebuild their piece list every tick, and every run re-walks on any change anywhere | Electric Lights | performance |
| [LINE-02](bugs/LINE-02-splitter-priorities-dont-turn-with-contraptions-or-schematic.md) | Splitter priorities don't turn with contraptions or schematics | Fluid & Power Lines | broken |
| [OBS-01](bugs/OBS-01-helper-bot-looks-see-through-where-a-chest-is-behind-him.md) | Helper Bot looks see-through where a chest is behind him | Reported in play | flywheel |
| **LOW** | | | |
| [CAGE-03](bugs/CAGE-03-jar-release-never-checks-the-jar-is-the-right-kind-for-its-m.md) | Jar release never checks the jar is the right kind for its monster | Monster Cage & Fish Tank | exploit |
| [CAGE-04](bugs/CAGE-04-bone-dropping-cages-pay-for-their-own-monster-chow-contradic.md) | Bone-dropping cages pay for their own Monster Chow (contradicts D9) | Monster Cage & Fish Tank | exploit |
| [CAGE-05](bugs/CAGE-05-blue-axolotl-tank-scooped-colour-disagrees-with-the-window-a.md) | Blue-axolotl tank: scooped colour disagrees with the window and the blue count | Monster Cage & Fish Tank | broken |
| [CAGE-06](bugs/CAGE-06-per-tick-allocation-while-a-netherite-field-is-down.md) | Per-tick allocation while a Netherite field is down | Monster Cage & Fish Tank | performance |
| [CORE-01](bugs/CORE-01-battery-item-charge-is-not-capped-at-capacity.md) | Battery item charge is not capped at capacity | Registry, payloads & data | exploit |
| [ELEC-03](bugs/ELEC-03-carry-on-pickup-of-one-tower-block-unforms-the-whole-tower-n.md) | Carry On pickup of one tower block unforms the whole tower, no re-form | Battery, Dynamo & generators | broken |
| [ELEC-04](bugs/ELEC-04-partly-loaded-tower-rebuilds-81-blockpos-every-tick.md) | Partly loaded tower rebuilds 81 BlockPos every tick | Battery, Dynamo & generators | performance |
| [ELEC-05](bugs/ELEC-05-generators-allocate-a-paper-record-every-tick-just-to-compar.md) | Generators allocate a Paper record every tick just to compare kinds | Battery, Dynamo & generators | performance |
| [FLY-04](bugs/FLY-04-receiver-scans-every-tube-every-tick-loads-or-not.md) | Receiver scans every tube every tick, loads or not | Flywheel & Clockwork Tubes | performance |
| [FLY-05](bugs/FLY-05-generic-plant-parts-and-the-clocks-workstation-bypass-flywhe.md) | Generic plant parts and the clock's workstation bypass Flywheel | Flywheel & Clockwork Tubes | flywheel |
| [FLY-06](bugs/FLY-06-partsvisual-allocates-and-re-tests-per-frame-where-it-neednt.md) | PartsVisual allocates and re-tests per frame where it needn't | Flywheel & Clockwork Tubes | performance |
| [HBOT-01](bugs/HBOT-01-unchanged-conductor-status-is-re-sent-every-5-s.md) | Unchanged conductor status is re-sent every 5 s | Helper Bot | performance |
| [HBOT-02](bugs/HBOT-02-ber-draw-path-looks-up-the-render-type-through-utilmemoize-e.md) | BER draw path looks up the render type through `Util.memoize` every frame | Helper Bot | performance |
| [HBOT-03](bugs/HBOT-03-pick-block-keeps-the-dye-but-drops-the-anvil-name.md) | Pick-block keeps the dye but drops the anvil name | Helper Bot | broken |
| [JUG-04](bugs/JUG-04-create-contraptions-shove-him-d20-d56.md) | Create contraptions shove him (D20, D56) | Clockwork Juggernaut | broken |
| [JUG-05](bugs/JUG-05-leap-aim-recomputed-every-tick-of-the-coil.md) | Leap aim recomputed every tick of the coil | Clockwork Juggernaut | performance |
| [JUG-06](bugs/JUG-06-cog-and-melee-hit-bystanders-that-never-attacked-him.md) | Cog and melee hit bystanders that never attacked him | Clockwork Juggernaut | exploit |
| [JUG-07](bugs/JUG-07-the-three-juggernaut-advancements-from-the-spec-do-not-exist.md) | The three Juggernaut advancements from the spec do not exist | Clockwork Juggernaut | broken |
| [LIGHT-03](bugs/LIGHT-03-hanging-a-festoon-places-the-far-hook-with-no-block-place-ev.md) | Hanging a festoon places the far hook with no block-place event (protection bypass) | Electric Lights | exploit |
| [LINE-03](bugs/LINE-03-fluid-networks-walk-every-piece-every-tick-to-refresh-the-si.md) | Fluid networks walk every piece every tick to refresh the sight slots | Fluid & Power Lines | performance |
| [LINE-04](bugs/LINE-04-a-clockwork-dynamo-on-paper-scans-every-record-for-every-fre.md) | A Clockwork Dynamo on paper scans every record for every free source, every tick | Fluid & Power Lines | performance |
| [LINE-05](bugs/LINE-05-a-span-whose-partner-post-is-carried-off-stays-on-the-post-l.md) | A span whose partner post is carried off stays on the post left behind | Fluid & Power Lines | broken |
| [LINE-06](bugs/LINE-06-a-turbine-casing-keeps-pointing-at-a-controller-a-contraptio.md) | A turbine casing keeps pointing at a controller a contraption carried away | Fluid & Power Lines | performance |
| [WARN-01](bugs/WARN-01-boss-spawned-with-override-only-finalizespawn.md) | Boss spawned with override-only finalizeSpawn | Compiler warnings | broken |
| [WARN-02](bugs/WARN-02-looting-fire-aspect-read-with-deprecated-enchantmenthelper.md) | Looting / Fire Aspect read with deprecated EnchantmentHelper | Compiler warnings | broken |
| [WARN-03](bugs/WARN-03-pick-block-overrides-use-the-old-getcloneitemstack.md) | Pick-block overrides use the old getCloneItemStack | Compiler warnings | broken |
| [WARN-04](bugs/WARN-04-chunk-loaded-checks-use-the-deprecated-haschunkatblockpos.md) | Chunk-loaded checks use the deprecated hasChunkAt(BlockPos) | Compiler warnings | performance |
| [WARN-05](bugs/WARN-05-ruin-ground-test-uses-deprecated-blockstateissolid.md) | Ruin ground test uses deprecated BlockState.isSolid() | Compiler warnings | broken |
| [WARN-06](bugs/WARN-06-monster-chow-food-effect-built-with-a-shared-mobeffectinstan.md) | Monster Chow food effect built with a shared MobEffectInstance | Compiler warnings | broken |
| [WARN-07](bugs/WARN-07-raw-types-and-redundant-casts.md) | Raw types and redundant casts | Compiler warnings | broken |
| [WARN-08](bugs/WARN-08-this-escapes-in-constructors-65-sites.md) | 'this' escapes in constructors (65 sites) | Compiler warnings | broken |
| [WARN-09](bugs/WARN-09-override-only-deprecations-that-should-be-silenced-not-chang.md) | Override-only deprecations that should be silenced, not changed | Compiler warnings | broken |
| [WARN-10](bugs/WARN-10-minor-client-and-test-api-deprecations.md) | Minor client and test API deprecations | Compiler warnings | broken |

## By area

### Monster Cage & Fish Tank

- [CAGE-01](bugs/CAGE-01-shift-click-dupes-the-jar-in-a-locked-down-netherite-cage.md) (high, exploit): Shift-click dupes the jar in a locked-down Netherite cage
- [CAGE-02](bugs/CAGE-02-create-drills-break-a-locked-down-foreign-netherite-cage.md) (medium, exploit): Create drills break a locked-down / foreign Netherite cage
- [CAGE-03](bugs/CAGE-03-jar-release-never-checks-the-jar-is-the-right-kind-for-its-m.md) (low, exploit): Jar release never checks the jar is the right kind for its monster
- [CAGE-04](bugs/CAGE-04-bone-dropping-cages-pay-for-their-own-monster-chow-contradic.md) (low, exploit): Bone-dropping cages pay for their own Monster Chow (contradicts D9)
- [CAGE-05](bugs/CAGE-05-blue-axolotl-tank-scooped-colour-disagrees-with-the-window-a.md) (low, broken): Blue-axolotl tank: scooped colour disagrees with the window and the blue count
- [CAGE-06](bugs/CAGE-06-per-tick-allocation-while-a-netherite-field-is-down.md) (low, performance): Per-tick allocation while a Netherite field is down

### Battery, Dynamo & generators

- [ELEC-01](bugs/ELEC-01-battery-simulate-answers-lie-20x-cables-destroy-energy.md) (high, broken): Battery simulate answers lie 20x, cables destroy energy
- [ELEC-02](bugs/ELEC-02-a-stale-formed-block-tears-down-a-live-tower-sharing-its-mas.md) (medium, broken): A stale formed block tears down a live tower sharing its master
- [ELEC-03](bugs/ELEC-03-carry-on-pickup-of-one-tower-block-unforms-the-whole-tower-n.md) (low, broken): Carry On pickup of one tower block unforms the whole tower, no re-form
- [ELEC-04](bugs/ELEC-04-partly-loaded-tower-rebuilds-81-blockpos-every-tick.md) (low, performance): Partly loaded tower rebuilds 81 BlockPos every tick
- [ELEC-05](bugs/ELEC-05-generators-allocate-a-paper-record-every-tick-just-to-compar.md) (low, performance): Generators allocate a Paper record every tick just to compare kinds

### Flywheel & Clockwork Tubes

- [FLY-01](bugs/FLY-01-upgraded-chest-lid-vanishes-while-open-flywheel-on.md) (high, flywheel): Upgraded Chest lid vanishes while open (Flywheel on)
- [FLY-02](bugs/FLY-02-festoon-strings-rebuilt-on-the-cpu-every-frame.md) (medium, flywheel): Festoon strings rebuilt on the CPU every frame
- [FLY-03](bugs/FLY-03-capped-tubes-re-walk-the-whole-network-on-any-change-anywher.md) (medium, performance): Capped tubes re-walk the whole network on any change anywhere
- [FLY-04](bugs/FLY-04-receiver-scans-every-tube-every-tick-loads-or-not.md) (low, performance): Receiver scans every tube every tick, loads or not
- [FLY-05](bugs/FLY-05-generic-plant-parts-and-the-clocks-workstation-bypass-flywhe.md) (low, flywheel): Generic plant parts and the clock's workstation bypass Flywheel
- [FLY-06](bugs/FLY-06-partsvisual-allocates-and-re-tests-per-frame-where-it-neednt.md) (low, performance): PartsVisual allocates and re-tests per frame where it needn't

### Fluid & Power Lines

- [LINE-01](bugs/LINE-01-paper-fuel-income-is-never-bounded-by-the-wellspring-free-po.md) (high, exploit): Paper fuel income is never bounded by the Wellspring: free power from stock
- [LINE-02](bugs/LINE-02-splitter-priorities-dont-turn-with-contraptions-or-schematic.md) (medium, broken): Splitter priorities don't turn with contraptions or schematics
- [LINE-03](bugs/LINE-03-fluid-networks-walk-every-piece-every-tick-to-refresh-the-si.md) (low, performance): Fluid networks walk every piece every tick to refresh the sight slots
- [LINE-04](bugs/LINE-04-a-clockwork-dynamo-on-paper-scans-every-record-for-every-fre.md) (low, performance): A Clockwork Dynamo on paper scans every record for every free source, every tick
- [LINE-05](bugs/LINE-05-a-span-whose-partner-post-is-carried-off-stays-on-the-post-l.md) (low, broken): A span whose partner post is carried off stays on the post left behind
- [LINE-06](bugs/LINE-06-a-turbine-casing-keeps-pointing-at-a-controller-a-contraptio.md) (low, performance): A turbine casing keeps pointing at a controller a contraption carried away

### Clockwork Juggernaut

- [JUG-01](bugs/JUG-01-ruin-juggernaut-destroys-players-blocks-near-his-hall-no-dro.md) (medium, exploit): Ruin Juggernaut destroys players' blocks near his hall, no drops
- [JUG-02](bugs/JUG-02-spring-engine-over-capacity-crashes-every-client-that-render.md) (medium, broken): Spring Engine over capacity crashes every client that renders it
- [JUG-03](bugs/JUG-03-mainspring-lost-if-the-world-saves-during-his-death-animatio.md) (medium, broken): Mainspring lost if the world saves during his death animation
- [JUG-04](bugs/JUG-04-create-contraptions-shove-him-d20-d56.md) (low, broken): Create contraptions shove him (D20, D56)
- [JUG-05](bugs/JUG-05-leap-aim-recomputed-every-tick-of-the-coil.md) (low, performance): Leap aim recomputed every tick of the coil
- [JUG-06](bugs/JUG-06-cog-and-melee-hit-bystanders-that-never-attacked-him.md) (low, exploit): Cog and melee hit bystanders that never attacked him
- [JUG-07](bugs/JUG-07-the-three-juggernaut-advancements-from-the-spec-do-not-exist.md) (low, broken): The three Juggernaut advancements from the spec do not exist

### Electric Lights

- [LIGHT-01](bugs/LIGHT-01-run-lights-rebuild-their-piece-list-every-tick-and-every-run.md) (medium, performance): Run lights rebuild their piece list every tick, and every run re-walks on any change anywhere
- [LIGHT-03](bugs/LIGHT-03-hanging-a-festoon-places-the-far-hook-with-no-block-place-ev.md) (low, exploit): Hanging a festoon places the far hook with no block-place event (protection bypass)

### Reported in play

- [OBS-01](bugs/OBS-01-helper-bot-looks-see-through-where-a-chest-is-behind-him.md) (medium, flywheel): Helper Bot looks see-through where a chest is behind him

### Registry, payloads & data

- [CORE-01](bugs/CORE-01-battery-item-charge-is-not-capped-at-capacity.md) (low, exploit): Battery item charge is not capped at capacity

### Helper Bot

- [HBOT-01](bugs/HBOT-01-unchanged-conductor-status-is-re-sent-every-5-s.md) (low, performance): Unchanged conductor status is re-sent every 5 s
- [HBOT-02](bugs/HBOT-02-ber-draw-path-looks-up-the-render-type-through-utilmemoize-e.md) (low, performance): BER draw path looks up the render type through `Util.memoize` every frame
- [HBOT-03](bugs/HBOT-03-pick-block-keeps-the-dye-but-drops-the-anvil-name.md) (low, broken): Pick-block keeps the dye but drops the anvil name

### Compiler warnings

- [WARN-01](bugs/WARN-01-boss-spawned-with-override-only-finalizespawn.md) (low, broken): Boss spawned with override-only finalizeSpawn
- [WARN-02](bugs/WARN-02-looting-fire-aspect-read-with-deprecated-enchantmenthelper.md) (low, broken): Looting / Fire Aspect read with deprecated EnchantmentHelper
- [WARN-03](bugs/WARN-03-pick-block-overrides-use-the-old-getcloneitemstack.md) (low, broken): Pick-block overrides use the old getCloneItemStack
- [WARN-04](bugs/WARN-04-chunk-loaded-checks-use-the-deprecated-haschunkatblockpos.md) (low, performance): Chunk-loaded checks use the deprecated hasChunkAt(BlockPos)
- [WARN-05](bugs/WARN-05-ruin-ground-test-uses-deprecated-blockstateissolid.md) (low, broken): Ruin ground test uses deprecated BlockState.isSolid()
- [WARN-06](bugs/WARN-06-monster-chow-food-effect-built-with-a-shared-mobeffectinstan.md) (low, broken): Monster Chow food effect built with a shared MobEffectInstance
- [WARN-07](bugs/WARN-07-raw-types-and-redundant-casts.md) (low, broken): Raw types and redundant casts
- [WARN-08](bugs/WARN-08-this-escapes-in-constructors-65-sites.md) (low, broken): 'this' escapes in constructors (65 sites)
- [WARN-09](bugs/WARN-09-override-only-deprecations-that-should-be-silenced-not-chang.md) (low, broken): Override-only deprecations that should be silenced, not changed
- [WARN-10](bugs/WARN-10-minor-client-and-test-api-deprecations.md) (low, broken): Minor client and test API deprecations

## Suggested order of work

1. The four high findings: free power from stock (LINE-01), cables destroying energy into Batteries (ELEC-01), the jar dupe in a locked-down cage (CAGE-01), and the Upgraded Chest lid that vanishes with Flywheel on (FLY-01).
2. The crash and loss bugs among the mediums: Spring Engine over capacity crashing clients (JUG-02), the Mainspring lost on save during the death clip (JUG-03), and the ruin Juggernaut deleting players' blocks (JUG-01).
3. The two medium performance items that scale with base size: run lights (LIGHT-01) and festoons on Flywheel (FLY-02), then the tube re-walk (FLY-03).
4. OBS-01 needs a repro in the dev client before a fix; its file has a five-step ladder.
5. Everything low, with WARN-09 first so the build is quiet enough to see new warnings.

## Things the reviewers checked and found sound

Each findings file ends with a "Checked and OK" list; the highlights: all five new network payloads are validated server side (distance, permission, block entity type, clamped values); no energy, item or fluid duplication was found in Batteries, tubes, cages, pens, lines or spans; all mixin targets exist in the declared dependency versions and the optional-mod mixins are guarded; no client class is reachable from common code on a dedicated server; every new block has a blockstate, lang entry, loot table and item model; every sound id has a file; static caches are cleared on world unload; the Flywheel visual/renderer split is correct for every machine except the chest and the items in FLY-05.
