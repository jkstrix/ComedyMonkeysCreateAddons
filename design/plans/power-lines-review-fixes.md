# Power Lines: Review Findings and Fix Plan

Status: **fixes done 2026-10-26** (81 game tests pass; the server chunk test passes, with the new Wellspring cases) · see "What landed" at the end · Reviewed: the uncommitted Power Lines rework of 2026-10-26 (persistent networks, generators on paper, Splitter on junctions, Breaker joining tiers, the two new screens) · Spec: [fluid-and-power-lines.md](../specs/fluid-and-power-lines.md) (D14, D16, D19, D23, P4, P5, D31–D34)

Three independent reviewers covered the network core, the generators and their paper records, and the Splitter, Breaker and screens. Their findings are merged and de-duplicated here. Items marked **confirmed** were checked against the code.

A later review by Cursor (`Cursor-findings.md`) added two more: item 13b, and the spec paragraph under item 19.

`main` = `src/main/java/com/comedymonkey/createaddons/`, `res` = `src/main/resources/assets/comedymonkeysaddons/`.

## Decision needed

**Breakers in existing worlds (item 5).** Should brass and copper join at a Breaker automatically, or only when you wrench that joint?
- **Recommended: wrench to join**, as different colours already work. Nothing in an existing world merges on its own, and a joint never forms by accident.
- **Alternative:** keep the automatic join, and accept that some old builds will change when their chunks load.

The plan below assumes the recommendation.

## Order of work

1. **Phase 1, exploits and breakage:** items 1–6. Each one either creates power or fuel from nothing, or changes saved worlds.
2. **Phase 2, performance:** items 7–11.
3. **Phase 3, gameplay holes:** items 12–17, including 13b.
4. **Phase 4, rough edges and text:** items 18–20.
5. **Then:** run the game tests and the unloaded-chunk server test, do a fresh review of the fixes, and commit when the user approves.

---

## Phase 1: exploits and breakage

### 1. A generator back from paper can skip taking its record on (critical, confirmed)
- **Where:** `PowerGrid.back()` tests `p.at < now - 1`. `Paper.advance()`, which `draw()` calls, moves `at` up to the draw tick.
- **Problem:** the network can draw from the record in the tick just before the generator's chunk starts running. Then `at == now - 1` and `back()` thinks the generator was never away. The generator keeps its old fuel and store, and its next `note()` overwrites the record. Everything drawn on paper was free.
- **Fix:**
  - Keep the generator's own last tick in the record (`seenAt`, set only by `note()`), apart from the record's working time `at`.
  - `back()` tests `seenAt < now - 1`.
- **Test:** a game test where a network draws from a generator's record, then its chunk runs on the very next tick. Its fuel must drop by what was drawn.

### 2. Paper draws aren't saved (high, confirmed)
- **Where:** `Paper.draw()`, `Paper.advance()`, and the leftover handed back in `PowerLines.Network.tick()`. None of them call `setDirty()`.
- **Problem:** a world restart brings the record back to its last save, and its fuel is worked forward and burned again. The power drawn since the last save is handed out twice.
- **Fix:** mark the grid dirty whenever a record actually changes: on a real draw, on handing energy back, and in `advance()` when time moves on. Dirty only marks it for the next autosave, so this costs nothing per tick.
- **Test:** the server chunk test gets a "restart while on paper" case: draw far out, stop and start the server, then check the generator's fuel.

### 3. Chunks at the edge of the loaded area break the "nothing changes" rule (high)
These chunks are loaded but don't run. Fluid Lines, other mods' pipes, `share()` between touching generators, and capabilities all still act on blocks in them.
- **Where:**
  - `PowerLines.Network.tick()` decides "live or paper" from the target the cable meets (`level.shouldTickBlocksAt(t)`).
  - `resume()` overwrites the tanks and store outright.
  - `FluidLines.tick` only checks `isLoaded`.
- **Problems:**
  - **(a) Turbine across a chunk edge.** If the casing's chunk runs but the controller's doesn't, or the other way round, the network draws the live store and the record also refills, or the record is drawn and then overwritten. Power is created or duplicated.
  - **(b) Unlimited lava.** Pipe the lava out of a generator in a loaded-but-not-running chunk. The record keeps the full tank, and `resume()` refills it.
  - **(c) Lost input.** Lava, water or shared energy pushed into such a generator is wiped when it resumes.
- **Fix:**
  - **(a)** Decide live or paper by the generator's own block, its `generatorKey`: live while that chunk runs. A face in a different chunk only says where the line meets it.
  - **(b, c)** Make `resume()` apply only how far the record moved while it was away, not the record itself:
    - Each fuel and the store keep the value they had when the generator was last seen.
    - On resume, the block entity gets (paper now − paper when last seen) added to its current tanks and store, clamped to what they hold.
    - Anything else that changed the block meanwhile survives, and nothing is created.
- **Test:**
  - A game test with a generator in a loaded-but-not-running chunk, lava drained from it by a line, then resumed: no lava created.
  - A turbine with the controller and a port casing in different chunks.

### 4. A Lava Generator with a wire on each side counts twice (high, confirmed)
- **Where:** the `LAVA_GENERATOR` energy capability returns `getEnergy().extractOnly()`, which is a new wrapper object every call. `Network.tick()` de-duplicates by object identity (`seen.add(e)`).
- **Problem:** the network can't see that two wires meet one generator, so the generator's offer counts twice. The plan promises power that isn't in the pool, and takers later in the list get nothing. Even sharing and Splitter priority both break. Other mods' batteries that return a wrapper per side do the same.
- **Fix:**
  - De-duplicate machines by block: the target position, or `generatorKey` for this mod's generators. Don't use the capability object.
  - Also make `EnergyBuffer.extractOnly()` return one cached wrapper.
- **Test:** a game test with one generator, a wire off each terminal and two motors. Both motors get an even share, and the total doesn't go over the generator's output.

### 5. Breakers join tiers by themselves in existing worlds (medium-high)
- **Where:** `LineBlockEntity.breakerJoin()`, used by `joins()`, which every relink calls (including on chunk load).
- **Problem:**
  - Any Breaker whose run ends against a wire of the other tier now joins it, so lines kept apart merge.
  - The neighbour can stop being a straight piece, and its own fitting pops off (for example a Breaker on a north–south run next to a Meter on an east–west run).
- **Fix (recommended decision):**
  - A brass–copper joint at a Breaker forms only when that joint is wrenched: `Joint.JOINED` on both sides, as for different colours. The wrench on a joint already sets this.
  - The neighbour's straightness check ignores a cross-tier joint that isn't wrenched, so nothing pops off.
- **Test:**
  - A Breaker against the other tier doesn't join until the joint is wrenched, then does.
  - Change the existing Breaker test to wrench the joint.

### 6. The Breaker join is drawn wrong (medium-high, confirmed)
- **Where:** `PowerLineModel.getModelData()` sets a `cross` bit for any neighbour of the other tier, and `build()` draws the other tier's arm for any `LINE` link with that bit.
- **Problem:** the plain wire beside the Breaker also draws its arm in the other tier, so the cable reads brass, copper, brass, copper.
- **Fix:**
  - Set the bit only on the Breaker piece, and only along its run (`be.fitting() == BREAKER && d.getAxis() == fittingSide().getAxis()`).
  - Leave it out otherwise. This also stops pointless cache keys for unjoined side-by-side wires.
- **Check:** in game, and in the showcase's power_rules row.

---

## Phase 2: performance

### 7. Every generator writes its record every tick (medium-high)
- **Where:**
  - `PowerGrid.note()` is called every tick by each Lava Generator and turbine. It allocates a new `Paper`, `Fuel[]` and `Fuel` and calls `setDirty()`.
  - Every 20 ticks, on the same tick for all generators, `FluidLines.supplyTo()` copies `byPos.values()` into a new set (the turbine calls it twice).
- **Problem:**
  - The grid is always dirty, so every autosave rewrites the whole grid (every piece, link and end).
  - The once-a-second supply check copies every Fluid Line block in the dimension per generator, all in one tick. With 50 generators and 10,000 line blocks that's about 500,000 entries.
- **Fix:**
  - **Only keep records for generators a line meets.** `note()` returns at once unless the generator's key is in the faces map, so generators with no Power Line cost nothing.
  - **Update in place**, with no new objects per tick.
  - **Write once a second, not every tick,** spread out by a position hash. Also write when the chunk unloads (`onChunkUnloaded`), so a record is never more than a second old.
  - **Supply check:**
    - Spread over the second by a position hash.
    - Add a lookup in `FluidLines` from each end's target to its networks, kept up to date when networks are built and forgotten. `supplyTo` reads only the networks that reach the generator.
- **Test:** a timing check on the dedicated server with the lag datapack and many generators: tick time before and after.

### 8. Rebuilds break "once a second at most" (medium)
- **Where:**
  - A new `Network` starts with `lastTick = -1` and updates as soon as any of its terminals comes up.
  - `setPriorities()` always rebuilds.
  - `SplitterSettingsPayload` can be sent any number of times.
  - A Breaker that changes state forgets the network on every update.
- **Problems:**
  - Placing a wire every few ticks moves about 5× the rate, and rebuilds the network each time.
  - Closing a Splitter screen rebuilds even when nothing changed.
  - A Breaker on a fast clock means the network rebuilds every update and never moves power.
- **Fix:**
  - **Network timing:** the level remembers the last update tick of each network by one of its nodes, and a rebuilt network carries it over. It never updates sooner than `INTERVAL` after the last one.
  - **Priorities:** `setPriorities()` does nothing when the levels haven't changed, and the screen only sends when something changed.
  - **Breakers:** an open or closed Breaker joins or cuts circuits in the update itself, from the switches recorded at build, rather than by rebuilding.
- **Test:**
  - A game test placing wires on a running network every 4 ticks: the motor gets no more than the line's rate.
  - A Breaker on a 2-tick clock still lets power through while it's closed.

### 9. Splitter ranking is costly (medium)
- **Where:** `Network.rankSplitters()`, `behind()`, `keyOf()`.
- **Problem:**
  - Each Splitter keeps a map of nearly every piece in the network, for the network's whole life, though it's only needed while building.
  - Each branch runs its own walk through the network, so the cost is Splitters × branches × network size on every change.
  - On a ring, every branch reaches everything and gets the lowest level, so priorities silently do nothing.
- **Fix:**
  - Work the keys out in one pass while building, then drop the maps; only the devices' and feeds' keys are kept.
  - Walk each branch once from the Splitter, stopping at other Splitters. Each of those starts its own walk, so their levels nest.
  - Total work is about one walk of the network.
  - On a ring (a branch reaches back to its Splitter), give that branch's pieces the better level and show "Loops back" on that row in the screen, so the player can see why.
- **Test:**
  - Nested Splitters (a Last branch with a First sub-branch).
  - A ring, where the screen says the branch loops back.

### 10. Each change re-reads 33 blocks, and neighbouring changes aren't merged (low)
- **Where:** `PowerLines.readChanges()`.
- **Problem:** a schematic or contraption placing a run of N wires re-reads each piece up to about 27 times, each with unlinking and relinking.
- **Fix:** collect the set of positions to re-read first (the changed position, its 26 neighbours and the 6 two away), then read each once.

### 11. Two reads can load a chunk (medium, confirmed for the pole foot)
- **Where:**
  - In `PowerLines.read()`, `level.getBlockState(foot.relative(d))` at a pole's foot.
  - In `PowerLines.splitter()`, `getBlockState(g.generatorKey())`.
- **Fix:** check `level.isLoaded(...)` first in both places. For the Splitter screen, use the recorded name, or "generator", when the controller isn't loaded.

---

## Phase 3: gameplay holes

### 12. Touching Lava Generators don't share on paper (medium)
- **Where:** `PowerGrid.paperAt(face)` gives only the generator the cable touches. `share()` only runs live.
- **Problem:** a far row of four generators with one cable gives a quarter of its power.
- **Fix:**
  - While live, each generator records the other generators its terminals touch (its row) in its record.
  - On paper, the network treats the row as one source: their stores are summed and each is drawn in proportion.
  - On resume, each takes its own share.

### 13. Wellspring income is over-counted (medium-high)
- **Where:** `FluidLines.supplyTo()`.
- **Problem:**
  - Every generator on a Fluid Line is credited the Wellspring's whole rate, ignoring the other ends, Valves, Filters and Splitters.
  - One Wellspring feeding four generators keeps all four running forever on paper, though loaded they'd share it.
- **Fix:** credit each generator with what the line actually delivered to it. While live, the Fluid Lines already move fluid to each end, so `supplyTo` reads the delivery to that end over the last seconds (a running average kept per end) instead of the Wellsprings' rates. A shut Valve, a Filter set to another fluid or a full tank then counts as it does in the world.

### 13b. A Wellspring in a running chunk isn't drawn down for a generator on paper (medium-high; from Cursor-findings.md #1)
- **Where:** `FluidLines.supplyTo()` (the income a record is credited), `PowerGrid.Paper.advance()`, and the generators' `resume()`. `FluidLines.tick` skips ends in chunks that aren't running.
- **When it's fine:** if the Wellspring's chunk isn't running either, crediting the generator its income while the Wellspring stands still comes to the same as both running (the Wellspring makes it, the generator burns it, the Wellspring's tank stays put). Nothing is duplicated.
- **Problem:** if the Wellspring's chunk is running, nothing takes the generator's share out of it.
  - The Wellspring fills its own tank (up to a tank's worth), or the line gives the generator's share to its other live ends.
  - Meanwhile the generator is also credited that fuel on paper. Fuel is duplicated, a tankful or more per trip.
- **Fix:** a generator on paper becomes a **paper end** of its Fluid Line.
  - **Wellspring running:** the line delivers to that end as usual, up to the line's rate and its share. The fluid is really taken from the Wellspring and added to the generator's record, up to the record's tank space. The record's paper income is zero in this case.
  - **Wellspring not running:** the credited income stands, as now (item 13's fair share).
  - Either way, a full record stops taking, as a full tank would.
- **Test:** the server chunk test gets a case with the Wellspring force-loaded and the generator's chunk unloaded. The Wellspring's output goes to the record, the generator comes back with what the Wellspring gave, and the Wellspring isn't left a full tank ahead.

### 14. Brass feeding copper misses some cases (low-medium)
- **Where:** `Network.plan()`.
- **Problems:**
  - A copper line fed by two brass lines asks each for half. If one has nothing, copper gets only half even when the other has spare.
  - Brass surplus never charges batteries on the copper side.
- **Fix:**
  - Two passes: ask each brass parent for the need, take what each can give from its spare, and ask the others for the rest.
  - Include copper battery room in what copper asks for, after its devices.

### 15. Leftover energy is dumped into the first paper record with no cap (low)
- **Where:** the leftover loop at the end of `Network.tick()`.
- **Problem:** leftovers from batteries, live generators or brass go into whichever paper record comes first, possibly past its cap.
- **Fix:**
  - Draw only what's given. Simulate each taker's acceptance before drawing, so there's nothing left over.
  - Return any rounding remainder to the sources it came from, in proportion, capped at each one's room.

### 16. A span or joint cut while the far end is unloaded still carries power (low-medium)
- **Where:** `PowerGrid.neighbours()` joins two pieces if either one links to the other.
- **Problem:**
  - Taking a span down from a near pole leaves the far pole's record linking back, so power still flows until the far pole loads.
  - The same happens for a joint wrenched cut at a chunk edge.
- **Fix:**
  - **Spans:** when a span comes down, also remove the far pole's link to it in the grid directly (the far pole's piece is in the grid even while unloaded). `dropBrokenSpans` still tidies the block when that chunk loads.
  - **Wire joints:** the same, removing the link on both pieces.

### 17. Turbine setting edge cases (medium for the crash)
- **Where:** `GeothermalControllerBlockEntity.outputSetting()`, `write()`, `GeothermalTurbineScreen`.
- **Problems:**
  - **Crash:** `turbineFePerTick = 0`, which the config range allows, makes `Math.clamp(x, 1, 0)` throw on every turbine tick and save.
  - **Saved clamped:** raising the config maximum leaves existing turbines at the old one.
  - **Rounding:** opening and closing the screen rounds an off-step value.
- **Fix:**
  - Save the setting as a share of full (tenths, 1 to 10, default 10) instead of FE. Work out FE from the current config, guarding 0.
  - The screen sends only when changed.

---

## Phase 4: rough edges and text

### 18. Splitter rough edges (low)
- **Flaky test:** `splitterServesTheFirstBranchFirst` is likely flaky. An update can run before the Splitter is fitted. Fit it in the same step as the relink.
- **Old Splitters on straight pieces** stay until something nearby changes, then pop off. On load, drop the Splitter off a Power Line piece that isn't a junction (one check in `onLoad`), giving the item back.
- **A "Power in" branch keeps a stale priority.** Treat Power in branches as level 1 when ranking. Ties at First point at none; show the pointer only when exactly one branch is first, or point at the first one round the junction and say "first equal" in the screen.
- **Screen text:**
  - Keep the order line within the read-out (wrap to two lines or shorten to "N first").
  - Fix the plurals: use the item's plural via its own lang key, fall back to "×N".
  - Fix "1 machines": use singular keys.
- **Read-out after a change** says "Nothing is drawing power". Carry the last figures over to the rebuilt network (with item 8's timing), or say "Working it out…" until the first update.
- **"Wanted" is buffer room, not real draw.** Show what the devices took at the last update instead.
- **Encased Splitter:** the empty hand says "use a wrench", and the wrench then removes it. Treat an encased Splitter like any encased line (wrench actions go to the encasing first), and say so.
- **Joints at a Splitter:** a wrench on a Splitter can no longer cut or join joints at that block. Sneak-wrench removes the Splitter first, then joints work as usual. Mention this in the tooltip.

### 19. Out-of-date text (medium)
- **Power Line tooltips** (`en_us.json`, the copper and brass Power Line summaries): "never joins Brass/Copper Power Lines". Change to: joins the other tier only at a pole or a wrenched Breaker, brass feeding copper.
- **JEI Splitter text:** still "Fit it to a straight Fluid or Power Line…". Describe both.
- **Breaker tooltip, JEI text and Power Lines ponder (text_5):** add the tier join.
- **Goggles on a Power Splitter** (`LineBlockEntity` tooltip) shows `splitter_<splitMode>`. Show the First branch, or "Shares evenly".
- **Lava Generator ponder:** text_3 says the more power is taken, the faster it turns; text_5 mentions Pipez and "only burns what's taken". Rewrite for "always runs flat out while it has lava".
- **Unreachable goggles lines:** "Full: nothing is taking the power" for both generators. Replace with "Store full: the extra is lost".
- **Turbine stall:** `isStalled()`, the stall sound (`GeothermalTurbineSounds`), the stall lamp (`ElectricRenderers`) and the "Turbine stalls: power full" subtitle can no longer happen. Remove them, or tie them to "store full" (decide with the user; sound changes go through the sound workflow).
- **Turbine tooltip and ponder text_9** quote 90/250 mB/s. Say "at full power", and add the power level.
- **The spec's unloaded-chunk write-up** (`design/specs/fluid-and-power-lines.md`, the paragraph under the unloaded-chunk table; from Cursor-findings.md #2):
  - It still says nothing is lost and that a generator with nowhere to send its power stops burning lava. The table above it, D14 and D19 now say the opposite: it runs flat out, and surplus is lost.
  - It also calls the record `FarGenerators` instead of `PowerGrid`.
  - Rewrite it to match D14, D19 and D32.
- **Spec and memory:** update for each fix as it lands.

### 20. Records and saves (low)
- **Records aren't checked against the block they're applied to.** A Lava Generator's 1-fuel record under a turbine's key throws `ArrayIndexOutOfBoundsException`. Store the generator kind and fuel count in each record, and drop it on a mismatch.
- **Records are never pruned.** When a block that should be a generator loads without one, drop its record. Also drop any piece whose block, on load, isn't what it says.
- **Fittings are saved by ordinal.** Save them by name.

---

## Checked and fine
- **Graph:** `put` and `linkedFrom` stay symmetric, and pole head moves are handled.
- **Concurrency:** a network update can't modify what it's iterating; `changed()` only queues.
- **Clean-up:** `BY_LEVEL` is cleared on server stop.
- **Chunk loading:** `BlockCapabilityCache` doesn't load chunks.
- **Removal paths:** `forget()` on break, Carry On, contraptions and explosions; all go through `onRemove`.
- **Payloads:** distance and permission checks, and server-side clamping.
- **Dedicated server:** the client-screen references load lazily, the same pattern as the Electric Motor.
- **Rendering:** reading neighbour block entities in the wire model is safe on the meshing thread, and the Splitter pointer turns correctly on walls and ceilings. The Flywheel visual redraws every frame.
- **Motor:** the 2-second buffer and old saves are fine.
- **Paper maths:** `Paper.advance()` handles fuels running dry, income over use, rate 0 and long gaps.

---

## What landed (2026-10-26)

All items are fixed, as planned, with these notes:
- **Decisions taken:**
  - **Item 5:** brass joins copper at a Breaker only once you wrench the wire next to it. A wrench on the Breaker's own block still takes the Breaker off.
  - **Item 19:** the turbine's stall lamp and sound now mean "running with its store full: the extra is lost". So do the "Store full" goggles lines for both generators.
- **Item 9:** priorities follow one walk out from the generators, each piece taking the Splitter levels of the way it was reached. On a ring, a branch's level is the one of the nearest way round. The planned "Loops back" note in the screen wasn't added.
- **Item 13:** income is what the Fluid Lines actually delivered to the generator, from each source, averaged over a few seconds. It isn't saved, so it builds up again within seconds of the generator running.
- **Tested since:**
  - **Item 2:** `paperDrawsAreSaved` checks a draw marks the save dirty and the record survives a save and load.
  - **Item 12:** `touchingGeneratorsShareOnPaper`. It also showed a gap: a generator in a row with no cable of its own never got a record. Fixed.
  - **Item 14:** `copperFedByTwoBrassLines`.
  - **Item 6:** checked in game on a dev client, with screenshots.
- **New game tests:**
  - `breakerJoinsTiersOnlyWhenWrenched`
  - `generatorMetTwiceCountsOnce`
  - The Breaker tier tests now wrench their joint.
  - The Splitter priority test sets the priorities in the same step as the relink.
- **New server test cases:** "Wellspring running" and "Wellspring away too".

## Afterwards (2026-10-26)

- **Readings strobed** (the user, in game): a generator's store dipped and climbed back each second, and motors swung by their use.
  - **Cause:** the network moved each second's power in one lump.
  - **Fix:** the plan is still made once a second, but it's handed out a twentieth each tick, with no planning or network walking per tick. A machine's share counts what it used in the last second, so with enough power it sits full. A generator is drawn only for what's actually taken.
  - **Tests:** `motorWithEnoughPowerStaysFull` and `generatorStoreStaysLevel`.
- **Flywheel:**
  - A part is only sent to the GPU again when it moved, changed light or was shown or hidden. This covers every machine.
  - A line with no fitting gets an idle visual that costs nothing per frame; it's made again when a fitting goes on or comes off.
  - Spans are Flywheel instances, laid out once and relit only on light changes (`LinePostVisual`). Checked against the renderer: pixel for pixel the same, and drawn by Flywheel (a temporary magenta tint showed only with Flywheel on).
- **Game tests:** 86 pass.

## Second review (2026-10-26)

Three fresh reviewers (network core and flow; generators, feeds and screens; Flywheel and rendering). Fixed:

**Records and the network**
- **Critical:** a record whose generator no longer refreshed it was taken back on every tick. A generator now takes its record on once and the record starts again from there. A record no line needs (no face, not in a row with one) is dropped. Test: `recordGoesWithItsLine`.
- A second's plan expires if it isn't renewed. Networks update from any terminal wire in a loaded chunk; devices are checked one by one.
- A paper source stops being drawn once its generator runs again, and a live one once its own chunk stops running.
- Generators on paper don't give their lava or power to other pipes and cables, so edge chunks can't spend the same twice. The Lava Generator's energy wrapper is made once.
- **Paper income:**
  - Only Wellsprings count; a Wellspring that's gone stops counting.
  - Records of generators away are worked forward once a second, so a Wellspring that comes back with the generator still counted for the time away.
- **Faces:**
  - A loaded block that isn't a generator drops its face, fixing a broken turbine casing replaced by a machine.
  - Faces whose line is gone are dropped.
- **Network behaviour:**
  - A Breaker beside a pole switches into the pole.
  - Shortfalls are shared max-min (evenly, none past its want).
  - Machines' use carries across a rebuild.
- **Upkeep:**
  - Generator records are their own saved data, so the grid isn't rewritten each autosave.
  - Terminals are checked in 20 buckets.
  - A Fluid Line ending at a chunk edge no longer loads the chunk past it.
  - Splitter changes always apply.
  - The test hook is cleared on server stop.

**Rendering**
- **Station:**
  - Its visual keeps its own playback of Trade Bot's clip. It ran on Flywheel's workers and raced his renderer.
  - It updates on ships and contraptions.
  - Its glow is drawn with a polygon offset.
- **Spans:** not relaid when unchanged, and dropped when the far post unloads.
- **Chest:** the extra rebuild hook is gone; Flywheel already rebuilds a visual when its block state changes.
- **Text:** the Ponder texts in code match the lang, and the stall subtitle now reads "store full".

**Not changed, by choice**
- Rounding of under 1 FE a tick per feed.
- The idle visuals for plain lines and closed chests. Returning no visual relies on Flywheel accepting null, which its API doesn't promise.

**Tests:** 87 game tests pass, and the unloaded-chunk server test passes ("Wellspring away too" −20 mB over 13 s).
