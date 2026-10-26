# Fluid Lines and Power Lines: Systems Spec

Status: **ready for development (v1)**  ·  Related: brief (to come, `design/briefs/fluid-and-power-lines.md`), quick read [fluid-and-power-lines.page.html](fluid-and-power-lines.page.html)

## Concept
"I don't like Pipez, I think they are ugly by default and the covers we added while they fix them feels like a work around. Create new pipez, ones that feel a bit less cheaty, look better, have more interesting upgrades. They still shouldn't require power to work or a pump, but not item pipe, just a fluid pipe and wires, that are more pleasing, maybe you can also hang the wires from point to point and both should have some way of hiding them." (user, 2026-10-25)

Clarified: **pump-free, power-free fluid lines and FE power lines in copper and brass, whose limits you can see, upgraded with fittings that clip onto the line instead of menu slots, with power lines that also hang in spans between posts, and both disappear into any block you encase them in.**

## Decisions
| # | Area | Decision | Source |
|---|---|---|---|
| D1 | Scope | Fluid lines and power lines (FE) only. **No item pipes** | user, 2026-10-25 |
| D2 | No power, no pump | Lines move fluid and FE without rotation, FE or a pump | user |
| D3 | Fairness | **Visible, tiered flow**: each tier has a fixed throughput; fluid shows moving through a thin window slot along the pipe. **Nothing glows** (user, 2026-10-25). **Unlimited range, no loss** | user R1 |
| D4 | Upgrades | **Fittings clip onto the line** (physical, wrenchable, readable with goggles). No upgrade slots and no menus | user R1 |
| D5 | Power layout | **Both**: power lines laid along surfaces like pipes, and **hanging spans** between Line Posts | user R1 |
| D6 | Hiding | **Encase**: right-click a line with any full block to hide it inside that block (copycat-style), and it keeps working. **Engineer's Goggles show hidden lines** as a faint outline. A Line Post on an encased line's face makes a wall bushing, so a span can run into a hidden line | user R2 |
| D7 | Tiers | **Two: Copper and Brass**, for both lines (copper replaced copper as the base tier) | user R2, changed 2026-10-25 |
| D8 | Fluid intake | An **Intake** fitting on the side that faces the source draws fluid in; every other end only delivers. Flow always runs from the Intakes outwards (no one-way fitting needed) | user R2 |
| D9 | Pipez | **Covers retired**: placed covers keep working, but their recipes are removed and the cover items stay only for old worlds (hidden from the creative tab and JEI). **Pipez feeding stays** (Blaze Burners, engines). Ponder and tooltips point to the new lines | user R2 |
| D10 | Fittings v1 | **Intake** (fluid, **with a built-in filter**: set to one fluid it draws only that; empty, it draws anything), **Valve** (fluid: shuts on redstone, its wheel throttles flow in 10% steps), **Breaker** (power: the Valve's twin, with a visible lever, shuts on redstone), **Filter** (fluid: passes one fluid only, for splitting fluids partway along a line), **Meter** (both: dial shows live flow, comparator output follows load), **Splitter** (both: Even / Nearest first / Round robin for the ends it feeds; its arrow shows the mode) | user R3 |
| D11 | Spans | Line Posts go on any face. Spans reach **16 blocks (copper) / 32 (brass)**. You need a **clear line when hanging**; after that, blocks never cut it. They sag gently, have no collision, and **shears or the wrench** take them down | user R3 |
| D12 | Connections | Lines connect to **any block that holds fluid or FE** (Create tanks, basins, spouts, item drains, the Wellspring, engines, our generators and motor, the Trade Bot station, other mods). They **don't join Create's Fluid Pipes**. They work on **Aeronautics physics objects**. On a **moving Create contraption they stop** | user R3 |
| D13 | Cost | **Cheap runs, costly fittings**: lines come 8 per craft; each fitting costs a real Create part | user R3 |
| D14 | Power input | **Automatic**: power lines draw from anything that gives FE and deliver to anything that takes it. No Tap. **Generators always run flat out while fuelled**: the Lava Generator at full output, the Geothermal Turbine at the power level set on it (D31). What isn't used fills stores (the generators' own and any batteries on the network); the rest is lost | user R4; revised 2026-10-26 ("magma generators always produce full power"; surplus "Fills stores, rest lost") |
| D15 | Fluid rules | **One fluid per line at a time; lines store nothing.** What shows in the windows is the transfer as it happens. Breaking a line never spills or duplicates fluid | user R4 |
| D16 | Mixing tiers | **Tiers join only at poles and Breakers, one way: brass feeds copper, copper never feeds brass**, and a copper line takes at most its own rate from the brass. So brass trunk spans between poles power copper branches into each area. Wire runs of different tiers side by side still don't join. You can upgrade a line in place (right-click copper with brass). The wrench cuts any single joint (two colours never join: D29) | user R4; revised 2026-10-26 ("brass can power copper, copper cannot power brass and connections can only be made at poles/breakers with the copper only accepting the lower passthrough") |
| D17 | Throughput | Copper **100 mB/t** and **4,096 FE/t**; Brass **1,000 mB/t** and **65,536 FE/t** (config) | user R4 |
| D18 | Valves | Throttle in **10% steps** (wrench, or scroll while holding the wrench, like Create's value boxes) | user R4 |
| D19 | Unloaded chunks | Fluid Lines **skip the parts that aren't loaded**: ends in unloaded chunks get nothing; nothing is lost or queued. Power Lines carry on: **one big generator far out can power a whole base**, and **no extra chunk is ever loaded**. Nothing changes in an unloaded chunk, so the network's record (D32) holds for it; a generator whose chunk isn't running runs **on paper** from its record (output, store, fuel and what its Wellsprings bring in through Fluid Lines), flat out as it always does, and takes the record back on when its chunk runs again. Networks update **once a second at most**, a second's worth at a time, and only while a device on them is live; generators never wake a network | user R4; revised 2026-10-26 ("loaded on paper, with their fuel source loaded on paper"; "Extra chunks should never be loaded"; "We can assume a network device cannot change while a chunk is unloaded, don't overcomplicate"; "network processing should be incredibly low priority, updating once a second at most and only when a device on the network is active (generators are treated as passive)") |
| D20 | Sound | A very quiet trickle (fluid) or hum (power) while flowing, following the mod's sound staging ladder | user R4, existing mod rule |
| D21 | Teaching | Goggle readouts on lines, fittings and posts; **a Ponder scene each** for fluid lines, power lines and fittings | user R4 |
| D22 | Names | **Copper / Brass Fluid Line**, **Copper / Brass Power Line**, **Copper / Brass Line Post**, **Intake, Valve, Breaker, Filter, Meter, Splitter** | user R5 |
| D23 | Fitting on | Right-click a **straight** piece of line with a fitting: it **clips on, one per block**. The Intake clips onto the **side facing a tank or machine**. Breaking the block drops both; the **wrench removes just the fitting** **Except the Power Line Splitter (2026-10-26): it sits on a junction (a T or a cross) on a floor, wall or ceiling**, and a wrench opens its screen (sneak-wrench takes it off) | user R5; revised 2026-10-26 ("shouldn't it be at a threeway split? 1 input, 2 outputs?", "or 3 outputs", "placeable on floors, walls or ceilings", "it should have a gui") |
| D24 | Safety | Lines and spans **never hurt anyone**: no damage, no fire, no weather effects | user R5 |
| D25 | Moving posts | A span whose **two ends move apart** (piston, contraption, a ship flying off) **comes down and drops its wire**. Spans with both ends on the same ship or contraption stay up and keep working | user R5 |
| D26 | Mod rules | Mod id has no "create"; Carry On needs a BlockItem; works on Aeronautics physics objects (Sable restarts block entities from NBT, so every line re-joins its network in `initialize`/on load) | existing mod |
| D28 | Look | **Thin and round, never blocky.** Fluid lines are slim round pipes (about 3–4 px across) with thin ring couplings at the joints and smooth round fittings; no square collars or boxy bodies. Power lines **look like wires, not pipes, even when laid on a surface**: a thin insulated cable (about 1–2 px) held down by small clips, bending around edges and up posts. **Placed wires attach to the face they are placed on**, floors, walls and ceilings, lying flat against it and following it round inside and outside corners, so they look fixed to the surface rather than floating in the middle of the block; posts are slim poles with a ceramic insulator. Spans are the same thin cable | user, 2026-10-25 |
| D29 | Colours | Lines are **dyed as items, before they're placed** (16 dyes; craft 8 lines around a dye; re-dye the same way). A line is **just its colour**: a dyed line is the dye colour all over (a dyed power line becomes a slightly thicker **covered cable**), muted to the house palette; an undyed line is its tier material all over; couplings and clips stay a darker shade of the tier material. No glowing (user, 2026-10-25). **Two colours never join, plain counting as a colour**, and the wrench can't join them, so different lines run side by side; an insulator holds one colour of cable. A placed line can't be dyed or washed. A dyed line's name says its colour ("Red Copper Power Line"), and with Create: Dragons Plus its **Bulk Coloring** (a fan through dye fluid) dyes and re-dyes line items. The user (2026-10-26): "instead of dying laid pipes and cables, we have to dye them before we place them, with two colours never meeting. I want to be able to place two lines side by side without them connecting"; "you should also be able to wash them with bulk washing" | user, 2026-10-25; revised 2026-10-26 |
| D30 | Posts | **Posts are built from blocks.** Stack Line Posts to make a pole of **any height**; the top block is the **head**. The head can carry **1 to 4 branches** (a crossarm out to N, E, S or W, each with its own insulator) as well as its top insulator, so one pole can carry several spans. Branches are added and removed on the head (P12). **Poles are plain wood with no tier** (no bands at the joints); each insulator's cap shows the tier of the cable tied to it | user, 2026-10-25 |
| D31 | Turbine output | The Geothermal Turbine's **power level is set on it**, with a wrench screen in the Electric Motor's style; lava and water use scale with it. The Lava Generator has no setting | user 2026-10-26 ("geothermals can have their power level set (same interface style as electric motors)") |
| D33 | Screens | **Approved 2026-10-26** ("both screens are approved"): the Geothermal Turbine's power level (andesite band over its dark steel, one Power field in 10% steps, a read-out of what it makes, burns and is worth in Lava Generators; a wrench on a formed controller), and the Power Line Splitter's priorities (brass band, a row per branch: way, what's down it, First/Second/Last or *Power in*; a read-out of how it stands). Previews `design/gui/geothermal-turbine-preview.html`, `design/gui/splitter-preview.html` | user 2026-10-26 |
| D34 | Breaker joins tiers | A Breaker on a Power Line joins brass to copper along its run (brass feeds copper, only while it's on); the cable changes under its housing (its arm towards the copper is drawn as copper) | user 2026-10-26 (D16); previewed in the showcase's power_rules row |
| D32 | Power networks | **A network is a lasting record**, kept with the level: its generators (output, fuel use, fuel and the fuel sources feeding them) and its devices. It's made when a generator (or line) is placed, merged when lines join, split when they're cut, and only worked out again when it's changed. **A device is live while its chunk is running**; each tick the network sets its live devices' use against its generators' output, with no cable walking. Generators in chunks that aren't running carry on **on paper** from their record (D19) | user 2026-10-26 (the network schema; "when you add/subtract a device from the network we keep a tally"; "Devices are Live when they are loaded and ticking") |
| D27 | Art | Dark, muted Create palette, restrained accents; every look previewed in Blockbench first; moving parts (flow windows, dial needles, valve wheels) are Flywheel instanced, like the other machines | existing mod |

### Defaults (approved by the user, 2026-10-25)
| # | Area | Proposal |
|---|---|---|
| P1 | World fluids | The Intake draws **only from containers**, never from water or lava source blocks in the world. Create's pump and Hose Pulley stay the way to pull world fluid, so the pump still has a job |
| P2 | Self-feed | An Intake never delivers back into the block it draws from |
| P3 | Span wire | Hanging a span uses **1 Power Line per 4 blocks** of length (rounded up), refunded when it comes down. Right-click post A with Power Lines, then post B |
| P4 | Posts | Poles have no tier. **A pole is one junction per colour** (revised 2026-10-26: the user, "poles should only join cables of the same colour"): the cables on it of one colour join, same tier freely, brass feeding copper of its colour one way (D16), so lines of different colours can share poles. Each insulator takes the tier (and colour) of the first cable tied to it, up to 2 spans. A wire placed against the pole's foot joins the head's top insulator. Brass trunks run on any pole, no special block |
| P5 | Sharing | Without a Splitter, a Fluid Line shares **Even** between the ends that can take more. A Power Line shares evenly between its live devices, as a local setup would; if there isn't enough, each gets its share and may not run. A **Power Line Splitter** on a junction ranks its branches in its screen: **First, Second or Last** (default all Second, which is even sharing). When power is short, First branches get all they want, then Second, then Last; branches on one level share evenly. Its dial's light pointer points at the First branch (the fork mark when all are level). Splitters further down a branch rank within it (revised and approved 2026-10-26: "I like the multiple branch priority") |
| P6 | Filter set | Right-click the Filter, or the Intake, with a bucket or a fluid-holding item to set it; an empty hand clears it |
| P7 | Recipes | Copper Fluid Line ×8: 3 copper sheets + glass (the window). Brass Fluid Line ×8: 3 brass sheets + 2 copper sheets + glass. Copper Power Line ×8: 2 copper ingots + dried kelp. Brass Power Line ×8: brass sheet + copper sheet + 2 dried kelp. Line Posts ×4: 3 logs (any wood) + 2 terracotta + an iron nugget. Intake: fluid valve + iron sheet. Valve: fluid valve + copper sheet. Breaker: lever + electron tube. Filter: copper sheet + paper + iron bars. Meter: copper + electron tube + glass. Splitter: electron tube + brass sheet. Dye: 8 lines around a dye |
| P8 | Encasing | The encasing block is **kept**, not consumed, and given back by the wrench (sneak-wrench on an encased line). Light and sound are the block's; it occludes. Posts aren't encasable |
| P9 | Goggles x-ray | Hidden lines within **16 blocks** draw as a faint outline while you wear goggles (client-side, no extra sync) |
| P11 | Washing | Create's **bulk washing** (a fan through water) takes the dye off line items (revised 2026-10-26: placed lines aren't washed) |
| P12 | Branches | Right-click the side of a head with a Line Post to add a branch there (it's used up); sneak to place a separate post instead. The wrench takes a branch off and gives the post back. Branches only on the head, at most one per side |
| P13 | Weathering | Copper lines **don't oxidise** (no weathering, no waxing needed) |
| P10 | Pipez worlds | A one-time hint in the Pipez cover tooltip: "Retired: use Fluid Lines and Power Lines" |

## Open Questions
None for v1.

Parked for later versions:
- **Item lines** (explicitly out of v1, per D1).
- Bare (uninsulated) span looks.
- An adapter that couples into Create's Fluid Pipe networks (declined for v1: it would sidestep pumps).
- Tethered spans that stretch between a moving ship and the ground.

## Core Loop
Place a line from a source to the machines that need it → clip an Intake on the source side (fluid only) → watch it flow through the windows → read a Meter, add a Valve, Filter or Splitter when you want control → upgrade to brass when the flow is too thin → encase the run in your walls so the base looks finished, and put on goggles to find it again.

## Behaviour / Rules
1. **A line** is every connected piece of the same kind and tier. Pieces join neighbours of the same kind, tier and colour automatically (plain is a colour). The wrench toggles a joint: it cuts one, or joins it again; it never joins two colours. Tiers and kinds never join.
2. **Ends**: any face of a line that touches a block with a fluid handler (fluid lines) or an energy handler (power lines) is an end.
3. **Fluid**: each tick, every Intake on the line draws from its block, up to the line's throughput shared between Intakes, only its filter fluid if it has one, and only a fluid that some reachable end can accept (through Filters), and never back into its own block. Delivered at once to the ends; nothing stays in the line.
4. **One fluid**: if the line's ends could take two fluids, the first Intake in turn wins that tick; Intake filters pick what each source sends, and Filters split a line by fluid.
5. **Power**: each tick, the line pulls from every end that can give FE and pushes to every end that can take it, up to its throughput. A block that can both give and take (a battery) is treated as a source unless a Meter or Splitter on that branch says otherwise (implementation: prefer pure receivers first).
6. **Fittings** apply at their block: a Valve or Breaker caps or cuts what passes it; a Filter blocks every fluid but one; a Meter measures what passes and outputs a comparator signal (0–15 by fraction of line throughput); a Splitter sets how what passes it is shared between the ends beyond it.
7. **Spans** are joints between two insulators (on any poles) of the same tier. A pole is one connection, however many insulators it has. A span joins two lines into one. Clear line of sight is checked only when hanging.
8. **Encased** lines behave exactly like open ones; their fittings keep working and can still be wrenched (the wrench hits the hidden fitting first while you wear goggles).
9. **Unloaded** parts of a Fluid Line are skipped (D19): a line split by unloaded chunks behaves as separate lines until they load. A Power Line reaches past them to the generators on its far side, which run on paper from their snapshot; nothing far away is loaded (D19).
10. **Moving**: pistons and contraptions carry line blocks like any block; on a moving contraption a line does nothing; spans whose ends separate drop (D25).

## Progression
- **Copper tier: early game.** Needs copper sheets, the same moment as Create's first fluid pipes. Enough for a Wellspring into a few tanks, or four Lava Generators into a motor.
- **Brass tier: mid game.** Needs brass. Carries a Geothermal Turbine's output and fast Wellsprings.
- Fittings unlock with their Create parts: the Intake and Valve with the Fluid Valve, the Breaker, Meter and Splitter with Electron Tubes.

## Balance
- **Cost vs reward:** the lines are cheap; the Intake is what costs, since it replaces a pump. One Intake per source keeps a base honest without punishing long runs.
- **Pump-free is strong.** Kept in check by: tier throughput caps; Intakes only draw from containers (P1), so pumps and the Hose Pulley still own world fluid; no joining Create's pipe networks (D12).
- **Exploits:**
  - Duplication by breaking mid-transfer: impossible, nothing is stored (D15).
  - Loops (Intake feeding its own tank, or two tanks feeding each other at full rate): P2 blocks self-feed; tank-to-tank loops just move fluid and cost nothing but ticks. Lines compute at most once per tick.
  - FE duplication through batteries in a ring: power is moved, never created; a pass never delivers to the end it pulled from.
  - Griefing with spans: spans have no collision and never hurt (D24), so they can't trap.
  - Lag: networks are cached graphs, rebuilt only when a piece, fitting or neighbour changes; only Intakes and FE sources do work each tick.
- **Overpowered check:** it doesn't replace Create's pumps (world fluid, pipe networks feeding Spouts by pressure, contraption fluid). **Useless check:** it does what Create can't without stress: tidy, long, pump-free runs, power distribution and readable meters.

## Create Integration
| System | Interaction | Notes |
|---|---|---|
| Shafts and gears | None | No power needed by design (D2) |
| Fluid pipes and pumps | Don't join | Pumps keep world fluid and pressure networks (P1, D12) |
| Fluid tanks, vaults, basins, spouts, item drains | Ends | Any fluid handler |
| Belts, depots, funnels, chutes | None | No item lines (D1) |
| Contraptions | Carried as blocks; inert while moving | Spans to the world drop (D25) |
| Aeronautics physics objects | Work | Re-join on assembly (D26); spans within the ship stay |
| Trains | Carried on contraptions, inert while moving | As contraptions |
| Logistics and packages | None | Out of scope |
| Deployers and arms | Deployers can place lines and clip fittings (they use items) | Natural Create behaviour |
| Redstone and comparators | Valve and Breaker shut on signal; Meter gives comparator output | D10 |
| Display Links | Meter is a Display Link source (flow, fluid name) | Proposed with the Meter |
| Goggles | Throughput, flow now, fluid, ends; hidden-line outline | D6, D21 |
| Ponder | Three scenes | D21 |
| Copycats / casings | Encasing works like a copycat | D6 |

## Technical Notes
- **Blocks:** `copper_fluid_line`, `brass_fluid_line`, `copper_power_line`, `brass_power_line`, `line_post` (all with BlockItems, waterloggable lines and posts). Power lines are **face-attached** (a multiface block like glow lichen: one wire per face of a block, joining round inside and outside corners); fluid lines are centred pipes.
- **Fittings:** items, stored on the line block entity (fitting kind + face + settings), so a line block holds at most one fitting.
- **Encasing:** a block state on the line's block entity (like the Pipez cover attachment, which this replaces); the model swaps to the encasing block, outline and collision to a full block.
- **Networks:** a server-side graph per line kind and tier per level, cached, invalidated on change; spans are graph edges between posts. Saved: fittings, settings, encasing, span list on posts (endpoints and tier). Networks themselves aren't saved, they're rebuilt on load.
- **Sync:** line connections in block state; fittings, encasing and flow rate (rounded, for the window animation and Meter dial) in block entity updates, sent only on change and throttled.
- **Rendering:** thin, round models (D28): pipes built from stepped rings so they read as round at 3–4 px, wires as thin cables with clips (placed wires follow the surface; their model is not a pipe with a different texture). The fluid runs along a sight slot under a glass cover (its own translucent part); the Intake and Filter windows are glass too. A piece stands full of whatever fluid an Intake with something to draw can reach it with, even into a full tank (a pipe never shows air with fluid available); the fluid only slides along where it actually moved in the last half second. Past a shut Valve or a Filter set to another fluid, the slots show empty. A line **fills and drains gradually**, not instantly like Pipez: the fluid's front creeps out from the Intake (copper 8 ticks a block, brass 3), a tank only receives once the fluid reaches it, and when the supply runs dry, a Valve shuts or a Filter changes, it drains from the Intake's end outwards the same way. Pieces already full stay full when the network is worked out again, and the fill is saved with the block. (Display only: the line still stores nothing.) The fittings' parts (Valve wheel, Meter needle, Splitter dial, Breaker lever) are Flywheel instances (PartsVisual) with a renderer fallback; the flowing fluid and the spans are drawn by renderers (spans as sagging tubes).
- **Config:** `fluidLineCopperMbPerTick` (100), `fluidLineBrassMbPerTick` (1000), `powerLineCopperFePerTick` (4096), `powerLineBrassFePerTick` (65536), `spanReachCopper` (16), `spanReachBrass` (32), `spansPerPost` (4), `hiddenLineOutlineRange` (16).
- **Pipez:** remove the cover recipes and hide the items from JEI and the tab; keep their loading code. Keep `PipezBlazeBurnerCompat` and `PipezEngineCompat`.
- **Data:** recipes, loot tables (fittings and the encasing block drop with the line), tags (`c:` fluid and energy aren't needed; capabilities do it), lang keys with subtitles, advancements (optional: "Out of Sight", encase a line).
- **Power Lines into machines (user, 2026-10-25):** a cable meeting a machine's face runs up (or across) that face to its middle and plugs into a socket there; a machine that takes power on its bottom (an upright Electric Motor's base) is reached from below: raise it over a block of air and the floor cable under it rises straight up into a socket in the middle of its base (the piece under it can still branch to other machines; a ceiling cable drops into a machine's top the same way). Lava Generators whose side terminals touch even out their stored FE, so a cable on any one draws on the whole row.
- **Power Lines through a wall (user, 2026-10-25):** run a cable to a solid block on each side, then wrench one end on the side facing the block: it's drilled and the cable threaded through (wrench again to pull it out; breaking the block fills the hole). One solid block, never a machine or a line; same tier and colour (revised 2026-10-26: two colours never join); it joins two cable ends only (neither piece may branch at the hole, though each can run on left, right, up or down). One straight hole, the same spot both sides: at the floor edge if either side's cable comes along the floor (or another face), else in the middle of the wall. A dark surround rings the hole on the cable's free sides only: three where it goes straight in, two where it turns at the floor.
- **Unloaded chunks (tested 2026-10-25, far generators on paper 2026-10-26, `design/tests/line-chunk-test.js`):** a dedicated server over RCON, no player, a 128-block copper Fluid Line (Wellspring to a Create tank) beside a 128-block copper Power Line (Lava Generator to an Electric Motor), each case force-loading only some chunks. A network notes the chunks it runs through and the unloaded ones at its edges, and is worked out again when one of them goes or comes back (a chunk can stop being accessible without being unloaded from memory, so unload events alone missed it).

  | Case | Fluid into the tank | Power into the motor | Generator's lava |
  |---|---|---|---|
  | Everything loaded | +2,500 mB in 5 s | +102,400 FE | −100 mB |
  | Source unloaded | 0 | +102,400 FE (the generator's full output, on paper; its chunk stays unloaded) | (unloaded) |
  | Back from paper (10 s far out, then loaded) | | 163,840 FE taken far out | 7,970 → 7,709 mB (13 s flat out, as if it had been loaded) |
  | Middle unloaded | 0 | +102,400 FE (the generator, running, beyond the gap) | −100 mB |
  | Target unloaded | 0 | (the motor isn't running) | −100 mB (it runs flat out regardless) |
  | Everything loaded again (after 30 s to refill) | +8,000 mB (the Wellspring's backlog, to the tank's limit) | +102,400 FE | −100 mB |

  Fluid never crosses an unloaded chunk; power does, from generators beyond it running on paper (D19, D32). The level's `PowerGrid` keeps every piece of line, where lines meet each generator, and a record for each generator a line meets: its output, store, fuels, and what each Wellspring that fed it brought (as delivered, averaged). A network draws on a record, through the Breakers and Splitters of its line, whenever the generator's own chunk isn't running; when it runs again it takes on how far its record moved meanwhile. Generators run flat out regardless (D14): with nothing to power, the extra is lost, so the generator's lava still falls with the motor unloaded. A Wellspring's income counts on paper only while that Wellspring isn't running too; one that is runs for real, and the line between them stops at the unloaded chunk. Tested (2026-10-26, `design/tests/line-chunk-test.js`): with the Wellspring running and the generator away, the generator burned 260 mB over 13 s with nothing credited; with both away, its income about matched its burn (−33 mB). A part of a line that lost its supply drains, and refills from where the fluid stands once it's back (a block every 8 ticks on copper), so the far tank waits for it.
- **Tests:** network sharing rules (unit, `SharingTest`); 30 game tests in `LineGameTests` (Intake to tank, brass rate, tiers apart, Intake filter, no self-feed, even sharing, cut joint, Valve on redstone and throttled, Filter, empty Filter, slots past a shut Valve, full but still into a full tank (and a replaced piece fills again), filling and draining gradually, signal flags, through a drilled wall (floor to floor, the middle of a wall, not before drilling, never a branching end), Meter, Splitter nearest first, colours, encasing, wire to motor, Breaker, wire tiers apart, wire up a wall, span, posts into a pole); the unloaded-chunk server test above; Display Link sources read on a sign (25 mB/t, 25%).
- **Performance (RTX 5090, dev client):** 1,024 flowing copper Fluid Lines in view take the frame from 0.45 ms to 1.06 ms; 1,024 Power Lines add about 0.2 ms.
- **Known limits:** a line with a loop uses the shortest way only; a block's wires on opposite faces join electrically; encased lines don't block light; span offsets don't turn with a contraption; assembly onto a Sable physics object hasn't had a game test yet.

# ONE MINUTE UNDERSTANDING
Fluid Lines and Power Lines replace Pipez in this mod. They need no pump and no power. They come in copper and brass, each with a fixed throughput you can see: fluid moves through a window along each pipe. Fluid needs an **Intake** fitting at the source; power flows by itself. You upgrade a line by clipping visible fittings onto it: Valve, Breaker, Filter, Meter, Splitter. Power lines can also hang in spans between posts. Both disappear into any block you encase them in, and goggles show where they run.

# WHAT IT IS
Pump-free, power-free fluid and FE lines in two tiers. Their upgrades are physical fittings on the line, and they can be hidden inside any block. Power lines can also hang between posts.

# WHY IT EXISTS
- Pipez looks out of place, and the covers were a workaround.
- Hidden upgrade menus feel like cheating; visible fittings feel built.
- The mod's generators, motor and Wellspring need a good-looking way to connect.

# HOW PLAYERS USE IT
- Lay a line from a tank or generator to the machines; wires clip flat along floors and walls.
- Clip an Intake on the source side of a fluid line.
- Add a Valve, Filter, Meter or Splitter where you want control.
- Hang power spans between posts for long outdoor runs.
- Encase the run in your walls; goggles show it again.

# HOW IT FITS CREATE
- Copper with the first fluid pipes, brass after brass.
- Fittings are made from Create parts (fluid valve, electron tube).
- Pumps keep world fluid; lines only draw from containers.
- Works on Aeronautics ships; does nothing on moving contraptions.
- Goggles, Ponder, comparators and Display Links all read it.

# IMPORTANT RULES
- No pump, no power, no range limit, no loss.
- Copper 100 mB/t · 4,096 FE/t; brass 1,000 mB/t · 65,536 FE/t.
- Fluid only moves from an Intake; power moves by itself.
- One fluid per line at a time; lines store nothing.
- Copper and brass never join. Two colours never join (plain is one); lines are dyed before they're placed.
- One fitting per line block, on straight pieces.
- Spans: 16 / 32 blocks, clear line to hang, never cut by blocks after.
- Encased lines work exactly like open ones.
- Lines never hurt anyone.
- Never joins Create's own Fluid Pipes.

# EDGE CASES
- **Chunk unloads mid-line?** Those parts are skipped; nothing lost.
- **Break a line while flowing?** Nothing spills; nothing stored.
- **Intake on a tank that the line also feeds?** It never feeds itself.
- **Ship flies off with one span end?** The span drops its wire.
- **Both span ends on the ship?** It stays up and works.
- **Two fluids could flow?** Set each Intake's filter; Filters split the line.
- **Block placed through a span?** The span stays.
- **On a moving contraption?** The line pauses until it's placed again.
- **Old Pipez covers?** Keep working; no longer craftable.
- **Fitting on an encased line?** Still works; wrench it with goggles on.

# DEVELOPMENT CHECKLIST
- [x] Blocks: 2 fluid lines (connection states, waterlogging, voxel shapes), 2 face-attached power lines (per-face wires, corner joins), 2 Line Posts
- [x] Line block entity: fitting, face, settings, encasing, flow rate for rendering
- [x] Network graph per kind/tier: build, cache, invalidate, span edges, unloaded-chunk skipping
- [x] Fluid transfer: Intakes, Filters, Valves, Splitter modes, self-feed guard, one fluid per line
- [x] Power transfer: sources and receivers, Breakers, Splitter modes, no pull-and-push to the same end
- [x] Dyeing: 16 colours on all four lines, dyed as items before placing, two colours never joining (plain is one), bulk washing
- [x] Fittings: 6 items, clip-on and wrench removal; Valve and Splitter set by right-click (sneak to open the Valve) instead of scroll value boxes
- [x] Meter: dial, comparator output, Display Link sources (Meter Flow, Meter Load, Meter Load Bar)
- [x] Poles: one wooden, tierless block; stacking (foot, pole and head states), 1-4 branches on the head, insulators as separate connections with tier-coloured caps
- [x] Spans: hanging, line-of-sight check, reach, 4 per post, wire cost and refund, shears and wrench removal, drop when ends separate
- [x] Encasing: any full block, model swap, wrench returns the block, goggles outline of hidden lines
- [x] Rendering: models, glass-covered flow slots, fittings' moving parts as Flywheel instances with a BER fallback (flowing fluid and spans stay with renderers)
- [ ] Contraptions and Sable: turn and mirror with structures; re-join on load. Still to test: assembly onto a Sable physics object
- [x] Pipez: remove cover recipes, hide cover items, keep feeding compat
- [x] Config keys (Technical Notes)
- [x] Recipes, loot tables, lang, tooltips, advancements (Look, No Pump · Drinking Straw · Out of Sight · Live Wire · Strung Up · Long Distance Call)
- [ ] Sounds: clip-on, valve, lever, splitter, span hang and take-down use vanilla events; the quiet trickle and hum wait for the sound pass (placeholder board first)
- [x] Goggle readouts; Ponder: fluid lines, power lines, fittings; JEI info pages
- [x] Unit tests (network rules) and game tests (Intake to tank, filter, valve, power to motor, span); Sable still to do

# "IDIOT TEST"
- Pipes for fluid, lines for power. No items.
- No pump. No power. Just place it.
- Fluid needs an Intake at the source.
- Power flows by itself.
- Copper is slow, brass is fast.
- They never join each other.
- Upgrades are parts you clip on.
- Power can hang between posts.
- Encase in any block to hide.
- Goggles show where it's hidden.

# COMPLETENESS SCORE
Gameplay: 100% · World Integration: 90% · Balance: 85% · Progression: 90% · Automation: 90% · Edge Cases: 85% · **Overall Design Readiness: 85%**

- Gameplay 100%: every decision and default is approved.
- World Integration 90%: the battery source/receiver rule (Rule 5) should be checked against real battery mods.
- Balance 85%: recipes (P7) are proposals; throughput numbers need a play test against Wellspring upgrade rates.
- Progression 90%: confirm the brass tier isn't needed before a Geothermal Turbine.
- Automation 90%: Deployers placing fittings to be confirmed in game.
- Edge Cases 85%: two Intakes on different fluids into one line needs a game test.
