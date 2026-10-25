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
| D14 | Power input | **Automatic**: power lines draw from anything that gives FE and deliver to anything that takes it. No Tap. The Lava Generator still burns only what's drawn | user R4 |
| D15 | Fluid rules | **One fluid per line at a time; lines store nothing.** What shows in the windows is the transfer as it happens. Breaking a line never spills or duplicates fluid | user R4 |
| D16 | Mixing tiers | **Tiers don't join**: copper only joins copper, brass only brass, so lines can run side by side. You can upgrade a line in place (right-click copper with brass). The wrench cuts any single joint, and joins two different colours of the same tier (D29) | user R4 |
| D17 | Throughput | Copper **100 mB/t** and **4,096 FE/t**; Brass **1,000 mB/t** and **65,536 FE/t** (config) | user R4 |
| D18 | Valves | Throttle in **10% steps** (wrench, or scroll while holding the wrench, like Create's value boxes) | user R4 |
| D19 | Unloaded chunks | A line **skips the parts that aren't loaded**: ends in unloaded chunks get nothing that tick; nothing is lost or queued | user R4 |
| D20 | Sound | A very quiet trickle (fluid) or hum (power) while flowing, following the mod's sound staging ladder | user R4, existing mod rule |
| D21 | Teaching | Goggle readouts on lines, fittings and posts; **a Ponder scene each** for fluid lines, power lines and fittings | user R4 |
| D22 | Names | **Copper / Brass Fluid Line**, **Copper / Brass Power Line**, **Copper / Brass Line Post**, **Intake, Valve, Breaker, Filter, Meter, Splitter** | user R5 |
| D23 | Fitting on | Right-click a **straight** piece of line with a fitting: it **clips on, one per block**. The Intake clips onto the **side facing a tank or machine**. Breaking the block drops both; the **wrench removes just the fitting** | user R5 |
| D24 | Safety | Lines and spans **never hurt anyone**: no damage, no fire, no weather effects | user R5 |
| D25 | Moving posts | A span whose **two ends move apart** (piston, contraption, a ship flying off) **comes down and drops its wire**. Spans with both ends on the same ship or contraption stay up and keep working | user R5 |
| D26 | Mod rules | Mod id has no "create"; Carry On needs a BlockItem; works on Aeronautics physics objects (Sable restarts block entities from NBT, so every line re-joins its network in `initialize`/on load) | existing mod |
| D28 | Look | **Thin and round, never blocky.** Fluid lines are slim round pipes (about 3–4 px across) with thin ring couplings at the joints and smooth round fittings; no square collars or boxy bodies. Power lines **look like wires, not pipes, even when laid on a surface**: a thin insulated cable (about 1–2 px) held down by small clips, bending around edges and up posts. **Placed wires attach to the face they are placed on**, floors, walls and ceilings, lying flat against it and following it round inside and outside corners, so they look fixed to the surface rather than floating in the middle of the block; posts are slim poles with a ceramic insulator. Spans are the same thin cable | user, 2026-10-25 |
| D29 | Colours | Lines can be **dyed** (16 dyes; right-click with a dye, or craft 8 lines around a dye). A line is **just its colour**: a dyed line is the dye colour all over (a dyed power line becomes a slightly thicker **covered cable**), muted to the house palette; an undyed line is its tier material all over; couplings and clips stay a darker shade of the tier material. No glowing (user, 2026-10-25). Lines of the **same colour join by themselves**. Lines of **different colours, dyed or undyed, only join when you wrench the joint** between them | user, 2026-10-25 |
| D30 | Posts | **Posts are built from blocks.** Stack Line Posts to make a pole of **any height**; the top block is the **head**. The head can carry **1 to 4 branches** (a crossarm out to N, E, S or W, each with its own insulator) as well as its top insulator, so one pole can carry several spans. Branches are added and removed on the head (P12). **Poles are plain wood with no tier** (no bands at the joints); each insulator's cap shows the tier of the cable tied to it | user, 2026-10-25 |
| D27 | Art | Dark, muted Create palette, restrained accents; every look previewed in Blockbench first; moving parts (flow windows, dial needles, valve wheels) are Flywheel instanced, like the other machines | existing mod |

### Defaults (approved by the user, 2026-10-25)
| # | Area | Proposal |
|---|---|---|
| P1 | World fluids | The Intake draws **only from containers**, never from water or lava source blocks in the world. Create's pump and Hose Pulley stay the way to pull world fluid, so the pump still has a job |
| P2 | Self-feed | An Intake never delivers back into the block it draws from |
| P3 | Span wire | Hanging a span uses **1 Power Line per 4 blocks** of length (rounded up), refunded when it comes down. Right-click post A with Power Lines, then post B |
| P4 | Posts | Poles have no tier. **Each insulator is its own connection**, taking the tier (and colour) of the first cable tied to it, up to 2 spans; so one pole can carry copper and brass lines side by side without joining them. A wire placed against the pole's foot joins the head's top insulator |
| P5 | Splitter default | Without a Splitter, a line shares **Even** between the ends that can take more |
| P6 | Filter set | Right-click the Filter, or the Intake, with a bucket or a fluid-holding item to set it; an empty hand clears it |
| P7 | Recipes | Copper Fluid Line ×8: 3 copper sheets + glass (the window). Brass Fluid Line ×8: 3 brass sheets + 2 copper sheets + glass. Copper Power Line ×8: 2 copper ingots + dried kelp. Brass Power Line ×8: brass sheet + copper sheet + 2 dried kelp. Line Posts ×4: 3 logs (any wood) + 2 terracotta + an iron nugget. Intake: fluid valve + iron sheet. Valve: fluid valve + copper sheet. Breaker: lever + electron tube. Filter: copper sheet + paper + iron bars. Meter: copper + electron tube + glass. Splitter: electron tube + brass sheet. Dye: 8 lines around a dye |
| P8 | Encasing | The encasing block is **kept**, not consumed, and given back by the wrench (sneak-wrench on an encased line). Light and sound are the block's; it occludes. Posts aren't encasable |
| P9 | Goggles x-ray | Hidden lines within **16 blocks** draw as a faint outline while you wear goggles (client-side, no extra sync) |
| P11 | Washing | A water bucket washes the dye off |
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
1. **A line** is every connected piece of the same kind and tier. Pieces join neighbours of the same kind, tier and colour automatically. The wrench toggles a joint: it cuts one, or joins two neighbours of different colours (undyed counts as a colour). Tiers and kinds never join.
2. **Ends**: any face of a line that touches a block with a fluid handler (fluid lines) or an energy handler (power lines) is an end.
3. **Fluid**: each tick, every Intake on the line draws from its block, up to the line's throughput shared between Intakes, only its filter fluid if it has one, and only a fluid that some reachable end can accept (through Filters), and never back into its own block. Delivered at once to the ends; nothing stays in the line.
4. **One fluid**: if the line's ends could take two fluids, the first Intake in turn wins that tick; Intake filters pick what each source sends, and Filters split a line by fluid.
5. **Power**: each tick, the line pulls from every end that can give FE and pushes to every end that can take it, up to its throughput. A block that can both give and take (a battery) is treated as a source unless a Meter or Splitter on that branch says otherwise (implementation: prefer pure receivers first).
6. **Fittings** apply at their block: a Valve or Breaker caps or cuts what passes it; a Filter blocks every fluid but one; a Meter measures what passes and outputs a comparator signal (0–15 by fraction of line throughput); a Splitter sets how what passes it is shared between the ends beyond it.
7. **Spans** are joints between two insulators (on any poles) of the same tier. A pole is one connection, however many insulators it has. A span joins two lines into one. Clear line of sight is checked only when hanging.
8. **Encased** lines behave exactly like open ones; their fittings keep working and can still be wrenched (the wrench hits the hidden fitting first while you wear goggles).
9. **Unloaded** parts are skipped (D19). A line split by unloaded chunks behaves as separate lines until they load.
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
- **Power Lines through a wall (user, 2026-10-25):** run a cable to a solid block on each side, then wrench one end on the side facing the block: it's drilled and the cable threaded through (wrench again to pull it out; breaking the block fills the hole). One solid block, never a machine or a line; same tier, any colour; it joins two cable ends only (neither piece may branch at the hole, though each can run on left, right, up or down). One straight hole, the same spot both sides: at the floor edge if either side's cable comes along the floor (or another face), else in the middle of the wall. A dark surround rings the hole on the cable's free sides only: three where it goes straight in, two where it turns at the floor.
- **Unloaded chunks (tested 2026-10-25, `design/tests/line-chunk-test.js`):** a dedicated server over RCON, no player, a 128-block copper Fluid Line (Wellspring to a Create tank) beside a 128-block copper Power Line (Lava Generator to an Electric Motor), each case force-loading only some chunks. A network notes the chunks it runs through and the unloaded ones at its edges, and is worked out again when one of them goes or comes back (a chunk can stop being accessible without being unloaded from memory, so unload events alone missed it).

  | Case | Fluid into the tank | Power into the motor | Generator's lava |
  |---|---|---|---|
  | Everything loaded | +2,500 mB in 5 s | +92,160 FE | −100 mB |
  | Source unloaded | 0 | 0 | 0 |
  | Middle unloaded | 0 | 0 | 0 |
  | Target unloaded | 0 | 0 | 0 |
  | Everything loaded again (after 30 s to refill) | +8,000 mB (the Wellspring's backlog, to the tank's limit) | +92,160 FE | −100 mB |

  Nothing crosses an unloaded chunk, nothing is lost, and a generator with nowhere to send its power stops burning lava. A part of a line that lost its supply drains, and refills from where the fluid stands once it's back (a block every 8 ticks on copper), so the far tank waits for it.
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
- Copper and brass never join. Same colours join; different colours only when wrenched.
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
- [x] Dyeing: 16 colours on all four lines, same-colour joining, wrenched joins between colours (saved per joint), washing
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
