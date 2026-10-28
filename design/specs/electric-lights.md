# Electric Lights: Systems Spec

Status: **ready for development (v1)**  ·  Related: [fluid-and-power-lines.md](fluid-and-power-lines.md) (the Power Lines and Line Posts these build on), brief (to come, `design/briefs/electric-lights.md`), quick read [electric-lights.page.html](electric-lights.page.html)

## Concept
"I want a series of blocks that are lights, street lamps (multiple styles), wall lights, ceiling lights (industrial and not), hanging lights, strip lights, edge lights and anything else you can think of. All powered by FE using our cables. They should be fairly cheap to make, Create styled but fit each scenario they are meant for. Also allow different colours by dyeing them with a right click (don't consume the dye, and breaking them removes the dye, and don't fill the recipe book with all the colours). Interesting, varied and useful. Street lights can use a similar build mechanic as the electricity poles." (user, 2026-10-28)

Clarified: **a family of cheap, Create-styled electric lights, one for each place you'd light (streets, walls, ceilings, hanging, strips, edges and more), fed by the mod's Power Lines, recoloured in place with a dye that isn't used up, and street lamps built up like the Line Post poles.**

## Decisions
| # | Area | Decision | Source |
|---|---|---|---|
| D1 | Scope | A family of lights: **street lamps (several styles), wall lights, ceiling lights (industrial and domestic), hanging lights, strip lights, edge lights**, plus other useful kinds | user, 2026-10-28 |
| D2 | Power | **FE, through the mod's Power Lines** (the `PowerGrid`) | user |
| D3 | Cost | **Fairly cheap** to make | user |
| D4 | Look | **Create-styled**, each light shaped for the place it's meant for | user |
| D5 | Dyeing | **Right-click a placed light with a dye** to colour it. **The dye isn't used up.** **Breaking the light takes the dye off** (it drops plain). **No colour recipes**, so the recipe book shows each light once | user |
| D6 | Street lamps | **Built like the Line Post poles** (stacked into a pole) | user |
| D7 | Mod rules | Mod id has no "create"; Carry On needs a BlockItem; works on Aeronautics physics objects (Sable restarts block entities from NBT) | existing mod |
| D8 | Art | Dark, muted Create palette; **glows muted, never near-white**; accents sparingly; every look previewed in Blockbench first; moving parts as Flywheel instances, glow and beams on renderers | existing mod (memories: art style, block restraint, Flywheel where possible) |
| D9 | Sound | Quiet, following the mod's sound staging ladder (`design/sound/staging.md`) | existing mod |
| D10 | Testing | Game tests for rules instead of manual checklists | existing mod |
| D11 | Power feed | **Most lights need their own feed**: a Power Line touching them. **Run-type lights chain power** along their run (Light Strips, Edge Lights and similar), so one feed lights the whole run | user R1 ("some lights can chain power … edge lighting etc., but mostly they require individual power sources") |
| D12 | Switching | Switching is done **on the circuit**, not on each light. A new **Light Switch** block you right-click; **street lamps are switched with redstone and a Breaker** on their feed line (a daylight sensor into a Breaker gives dusk-to-dawn) | user R1 ("make a light switch block for right clicking, also for street lamps we can use redstone and breaker") |
| D13 | Dye effect | **Glow colour only**: the lens, glow, halo and any beam take the dye's colour (muted to the house palette). Vanilla's light cast on the world stays white; **with Veil installed** (it comes with Sable and Create Aeronautics) each lit light also casts a Veil point light in its glow's colour, strengthened to read over the white, as far as its own light reaches, stopped by solid blocks (client config for both). Undyed is a warm incandescent tone; black casts a deep ultraviolet | user R1; coloured cast light, user 2026-10-28 ("really want the light to be coloured") |
| D14 | Street lamps | **Lamp Posts + heads**: a metal Lamp Post stacks into a pole like a Line Post. Right-click the top with a lamp head, or its sides to add arms with heads (up to 4). It joins the power network like a Line Post: fed by a cable at its foot, and spans can hang between lamp posts | user R1 |
| D15 | Light Switch | **Inline on the cable**: a small wall-plate switch a Power Line runs through, a hand-flipped Breaker made for walls. Off, everything fed through it goes dark. Two switches in series both have to be on | user R2 |
| D16 | Extras in v1 | **Floodlight, Festoon Lights, Warning Beacon, Floor Light, Indicator Lamp, Chandelier, Bollard Light**. The **Searchlight** is parked for later | user R2 |
| D17 | Power use | **Tiny draw**: 1 FE/t for a strip block or small light, 2–4 for wall and ceiling lights, 8 for street heads and high-bays, 32 for a Floodlight (config). One Lava Generator runs a few hundred lights. **Short of power, a light flickers briefly, then goes dark** | user R2 |
| D18 | Floodlight | **Paired to a target**: wrench the Floodlight, then click the block to light (up to 32 blocks, in clear sight). It turns to face it and puts full light on that spot, with a faint visible cone. A wall in the way stops the light there. Re-pair any time | user R3 |
| D19 | Hanging | **Lowered with cable**: place a Pendant or Chandelier under a powered ceiling; right-click it with a Power Line to lower the lamp one block, the gap filling with a hanging cable that carries the power (no collision). Up to 16 blocks. Breaking either part drops the lamp and gives the cable back | user R3 |
| D20 | Festoons | **Between posts and walls, powering only themselves**: hang between any two Lamp Posts, Line Posts or solid block faces (a small hook appears on a wall), up to 16 blocks, like a span. Fed from a powered post or a wall with a line in it. They **never join two networks**, so they're never a free power cable | user R3 |
| D21 | Lamp Post finishes | **Two: Cast Iron** (dark, fluted foot, scroll brackets on its arms, old-town) **and Steel** (plain round tube, modern). Any head on either; arms take the post's finish | user R3 |
| D22 | Brightness | **Wrench steps**: right-click a light with the wrench to step through **Full / Bright / Dim / Glow** (about 100 / 80 / 50 / 25% of the type's level, P1). Draw scales with it | user R4 |
| D23 | Recipes | **Craft a Light Assembly, then put it in a stonecutter to pick your light.** Cheap, and styles are easy to swap (a light goes back in the stonecutter to become another). **Lamp Posts are different and have their own recipes** | user R4 ("Craft a light assembly, then put it in a stone cutter to pick your light, cheap easy to swap out styles. Lamp posts are different and have their own recipes") |
| D24 | Dyeing runs, washing | **Dyeing a Strip, Edge Light or Festoon colours its whole connected run**; sneak to colour one block. **A water bucket washes any light plain** (the bucket isn't emptied) | user R4 |
| D25 | Defaults P1–P20 | **Approved as written** | user R4 |
| D26 | Bigger lights cost more | **Floodlight, Chandelier, Warning Beacon and High-Bay Lamp** are made at a crafting table from a Light Assembly plus a part, not in the stonecutter, and **leave the style swap**. Parts (proposed, P29): Floodlight + 2 iron sheets (the reflector); Chandelier + brass sheet; Warning Beacon + cogwheel (it turns); High-Bay + iron sheet | user R5 (chose "Bigger lights cost more" over the all-stonecutter P22) |
| D27 | Recipes P21, P24, P25 | **Approved** (P22 and P23 amended by D26) | user R5 |
| D28 | Wrench, beam, flicker P26–P28 | **Approved as written** | user R5 |

## Defaults (P1–P20 approved by the user, 2026-10-28)
| # | Area | Proposal |
|---|---|---|
| P1 | Light levels | At full brightness: Indicator Lamp 4; Light Strip and Edge Light 15 (10 in the first draft; raised after looks in game: runs are decoration as much as light, and their lit line takes the strong cast colour, not the muted lens); Festoon 10; Warning Beacon 10; Floor Light 12; Bollard 13; wall lights 14; ceiling lights, Pendant, Chandelier, street heads and Floodlight 15 |
| P2 | Draw (FE/t, full) | Indicator 1; Strip and Edge 1 per block; Festoon 1 per 2 blocks of string; Floor, Bollard and wall lights 2; Flush and Cage ceiling lights and Pendant 3; Warning Beacon 4; Chandelier 6; High-Bay and each street head 8; Floodlight 32. All in config |
| P3 | Flicker | Each light holds 2 s of power (covers the grid's once-a-second updates). When that runs dry it flickers for about a second with a faint buzz, then goes dark; power back, it flickers on. **Switched off** (Light Switch or Breaker) it just goes out, no flicker |
| P4 | Warm-up | The High-Bay and the Yard Light head warm up like sodium lamps: a dim amber glow rising to full over 4 s. Every other light is instant |
| P5 | Coming on together | Lights switched on at once (a Breaker or Switch) come on spread over up to a second, which reads as a ripple down a street and spreads the lighting updates |
| P6 | Placement | Wall lights: any wall face. Ceiling lights: underside of a block (the High-Bay on a short rod). Bollard: on a floor. Floor Light: a full, walkable block set flush in the floor. Indicator Lamp: any face. Warning Beacon: floor, wall or ceiling. Light Strip: any face, one per face, joining neighbours and turning corners like a Power Line. Edge Light: along any edge of a block, picked by where you click; several edges per block |
| P7 | Runs | Strips, Edge Lights and Festoons pass power only to their own run, whatever its colours, never to other lights or machines. A Power Line touching any piece of a run feeds the whole run |
| P8 | Lamp Posts | Stack into a pole of any height. The head takes one lamp on top (right-click with a lamp head) and up to 4 arms (right-click a side with a lamp head; the arm takes the post's finish). A small insulator bracket under the top ties spans, and **a pole is one junction per colour**, like a Line Post. A cable at the foot feeds it. The wrench takes a head or arm off and gives it back |
| P9 | Light Switch | A wall plate on any face. A Power Line enters one side and leaves another; same tier and colour only (it never joins tiers, unlike a Breaker). Right-click flips it with a soft click and a visible toggle. No redstone in (that's the Breaker's job) |
| P10 | Drops | Breaking or wrench-picking up always drops a plain light (D5). Carry On moves it as it is, colour and all. A lowered Pendant gives its cable back as Power Lines |
| P11 | Moving | On an Aeronautics ship, lights work from the ship's own network. On a moving Create contraption they keep their glow but light nothing and draw nothing, like Power Lines there. Pistons push them |
| P12 | Water and weather | Every light except the Chandelier is waterloggable and works underwater (the Floor Light doubles as a pool light). Lights never hurt anyone or start fires. Rain and lightning do nothing to them |
| P13 | Remote light | The Floodlight's spot and the Festoon's glow are invisible light blocks the lights place in air or water: one at the Floodlight's target, one every 4 blocks along a festoon. Removed when unpowered, broken, re-aimed, or the space is filled. They can't be picked, targeted or broken by players |
| P14 | Festoon cost | Hanging uses 1 Festoon Lights item per 4 blocks of string (rounded up), refunded when it comes down. Shears or the wrench take it down |
| P15 | Mobs | A lit light is a normal light source, so it stops spawns like a torch; a dark one doesn't |
| P16 | Goggles | Show: powered or not, draw, brightness, colour, and for a Floodlight its target |
| P17 | Ponder | Three scenes: lighting basics (a feed, a strip run, a Light Switch, dyeing), Lamp Posts (building a pole, arms, Breaker with a daylight sensor), Floodlight and Festoons |
| P18 | Names | Cast Iron Lamp Post, Steel Lamp Post, Lantern Head, Cobra Head, Globe Head, Yard Light Head, Bollard Light, Bulkhead Light, Wall Sconce, Gooseneck Lamp, High-Bay Lamp, Cage Lamp, Flush Ceiling Light, Pendant Lamp, Chandelier, Light Strip, Edge Light, Floor Light, Floodlight, Festoon Lights, Warning Beacon, Indicator Lamp, Light Switch |
| P19 | Sounds | Light Switch click; Warning Beacon's soft motor whirr while turning; faint buzz while flickering; quiet tick while warming up. All on the staging ladder, placeholder board first |
| P20 | JEI | An info page on every light explaining dyeing in place, since there are no colour recipes |

### Round 4 follow-ups (P21, P24–P28 approved 2026-10-28; P22–P23 amended by D26)
| # | Area | Proposal |
|---|---|---|
| P21 | Light Assembly | Shapeless: **glass + copper sheet + redstone dust → 2 Light Assemblies**. Early game: a press for the sheet, no Nether |
| P22 | Stonecutter yields | One assembly gives **1 light** of any full kind (every wall and ceiling light except the High-Bay, the Pendant, Bollard, Floor Light and the four street heads), **8** Light Strips, Edge Lights or Indicator Lamps, or **4** Festoon Lights (16 blocks of string, P14). The four bigger lights are crafted instead (D26) |
| P23 | Swapping styles | A light goes back in the stonecutter to become another **of the same yield group**, 1:1: full kinds with full kinds; Strip, Edge and Indicator with each other. Festoons and the four crafted lights don't swap. Groups stop a strip turning into a Sconce. The recipes use a tag per group as input, so there's one recipe per light, not one per pair (and the stonecutter has no recipe book) |
| P24 | Lamp Post recipes | **Cast Iron Lamp Post ×4**: 3 iron ingots + 1 coal or charcoal. **Steel Lamp Post ×4**: 3 iron sheets + 1 andesite alloy. The two finishes swap 1:1 in the stonecutter |
| P25 | Light Switch recipe | **Lever + copper sheet → 2 Light Switches** (it's a control, not a light, so it's not in the stonecutter) |
| P26 | Wrench | **Right-click** steps brightness (on a run, the whole run). **Sneak-right-click** picks the light up, or takes a head or arm off a Lamp Post (Create's usual pick-up; replaces P8's "the wrench takes a head off"). The **Floodlight** has no brightness steps: right-click starts aiming, then click the target (D18). Lights don't rotate with the wrench; placement sets which way they face |
| P27 | Floodlight beam | Clear sight is checked when pairing and again once a second; if a block now stands in the beam, the light lands on the face of that block instead, and returns when it's cleared |
| P28 | Flicker is visual | The flicker (P3) and warm-up (P4) are drawn by the renderer's glow; the light cast on the world changes **once** (to its level, or to dark), so a flickering street causes no lighting-update storm |

### Small facts (filled in while writing the report; approved 2026-10-28)
| # | Area | Proposal |
|---|---|---|
| P29 | Crafted lights' parts | Floodlight: assembly + 2 iron sheets. Chandelier: assembly + brass sheet. Warning Beacon: assembly + cogwheel. High-Bay: assembly + iron sheet (D26) |
| P30 | Toughness | Lights break fast with a pickaxe (hardness 1); the Floor Light is tougher (3) since it's walked on; Lamp Posts 2. Every light drops itself, plain |
| P31 | Who can change it | Anyone who can use the block can dye, wash, switch or step a light; claim and protection mods apply through the normal block-use events |

## Open Questions
None for v1. P29–P31 approved with the spec ("looks good", user 2026-10-28).

Parked for later versions:
- **Searchlight** (a big lamp with a visible beam, swept by a Create shaft) (D16).
- **Real coloured world light** through a coloured-lighting mod, if a usable one exists for NeoForge 1.21.1 (D13).
- **Two-way switching** (a stairwell switch at the top and bottom) (D15 chose plain inline switches).
- **Motion-sensor light** that comes on when someone walks near.
- Lamp heads on Line Post branches (D14 chose separate Lamp Posts).

## Catalogue (v1)
| Place | Light | For | Made |
|---|---|---|---|
| Street | **Cast Iron Lamp Post**, **Steel Lamp Post** (stack into poles) | The pole | Own recipes (P24) |
| Street | **Lantern Head** (old-town glass lantern), **Cobra Head** (modern road light), **Globe Head** (frosted ball), **Yard Light Head** (boxy sodium head, warms up) | Roads, squares, parks, depots, rail yards | Stonecutter ×1 |
| Ground | **Bollard Light** (knee-high post), **Floor Light** (flush, walkable, pool light) | Paths, gardens, platform edges, pools | Stonecutter ×1 |
| Wall | **Bulkhead Light** (caged oval), **Wall Sconce** (brass with a glass shade), **Gooseneck Lamp** (curved arm shining down) | Tunnels and factories, homes and halls, shop fronts and doorways | Stonecutter ×1 |
| Ceiling | **High-Bay Lamp** (big industrial dome, warms up), **Cage Lamp** (industrial), **Flush Ceiling Light** (low, domestic) | Factories and warehouses, workshops, homes | High-Bay crafted (D26); others stonecutter ×1 |
| Hanging | **Pendant Lamp** (enamel shade), **Chandelier** (brass, several arms) | Workshops and stairwells, halls | Pendant stonecutter ×1; Chandelier crafted |
| Runs | **Light Strip** (thin tube on a face), **Edge Light** (trim along a block edge), **Festoon Lights** (bulbs on a string between posts or walls) | Corridors, shelves, building outlines, steps, markets, gardens | Stonecutter ×8 / ×8 / ×4 |
| Special | **Floodlight** (paired to a far target), **Warning Beacon** (turns while lit), **Indicator Lamp** (tiny panel light) | Yards and farms, crossings and machines, control boards | Floodlight and Beacon crafted; Indicator stonecutter ×8 |
| Control | **Light Switch** (inline wall plate) | Switching a circuit by hand | Lever + copper sheet → 2 |
| Part | **Light Assembly** | Every light | Glass + copper sheet + redstone → 2 |

## Core Loop
Craft Light Assemblies → stonecut the light the place needs (or craft a bigger one) → run a Power Line to it, hidden in the wall if you like → dye it with a right-click, step its brightness with the wrench → put a Light Switch or a Breaker on the circuit → change your mind about the style later by putting it back in the stonecutter.

## Behaviour / Rules
1. **Power.** A light is lit when it has power and its circuit is switched on. It draws its kind's FE/t (P2) times its brightness step (D22). It takes power from any Power Line touching it, including one encased in the block it's mounted on (D11). A light holds 2 s of power (P3).
2. **Runs.** Light Strips and Edge Lights that touch form a run; a Festoon is one run. A run takes power where any piece touches a Power Line (or, for a Festoon, its powered post or wall) and shares it among its own pieces only. Runs never pass power to other lights, machines or networks (P7, D20).
3. **Switching** happens on the circuit (D12): a **Light Switch** (by hand, D15) or a **Breaker** (by redstone) on the feed line. Lights have no redstone input of their own. Dusk-to-dawn is a daylight sensor into a Breaker.
4. **Short of power**, a light flickers about a second, then goes dark; power back, it flickers on. Switched off, it just goes out (P3). Flicker and warm-up are visual only; the world light changes once (P28).
5. **Dye.** Right-click with a dye: the glow (lens, halo, beam) takes the colour, and the dye isn't used. On a run it colours the whole run; sneak for one block (D24). A water bucket washes it plain, and the bucket stays full. Breaking or wrench pick-up drops it plain (D5, P10).
6. **Brightness.** Wrench right-click steps Full → Bright → Dim → Glow → Full (100/80/50/25% of P1). On a run it steps the whole run. Not on the Floodlight (P26).
7. **Lamp Posts** stack into a pole; the head takes a lamp head on top and up to 4 arms (right-click a side with a head). A cable at the foot feeds the pole; a bracket under the top ties spans; a pole is one junction per colour, like a Line Post (D14, P8). Sneak-wrench takes a head or arm off. A lamp head can also stand on top of any block, fed by a Power Line touching it.
8. **Hanging.** Right-click a Pendant or Chandelier with a Power Line to lower it one block, up to 16; the hanging cable carries power and has no collision (D19).
9. **Floodlight.** Wrench it, then click a block up to 32 blocks away in clear sight: it turns to face it and places a light-15 spot there. A block put in the beam later moves the spot to that block's face until it's cleared (D18, P27, P13).
10. **Festoons** hang like spans between any two posts or solid faces, up to 16 blocks, 1 item per 4 blocks; their glow is light-10 spots every 4 blocks along the string (D20, P13, P14).

## Progression
- **Early game.** The Light Assembly needs glass, a copper sheet (Create's press) and redstone: the same moment as copper Power Lines and the first Lava Generator. No Nether.
- Lamp Posts need iron (Cast Iron) or iron sheets and andesite alloy (Steel), still early.
- The bigger lights need a second part: brass puts the Chandelier in the brass age; a cogwheel for the Warning Beacon; iron sheets for the Floodlight and High-Bay.
- Unlocks nothing: it's a builder's reward for having a power network, and a reason to run lines into houses and along streets.

## Balance
- **Against vanilla:** torches, lanterns, glowstone and sea lanterns are free to run. Electric lights earn their FE with what vanilla lacks: colour, four brightness steps, switching whole circuits, a shape for every place, hidden wiring, light at a distance (Floodlight) and light along a string (Festoons). The draw is tiny (D17): a small running cost, not a chore.
- **Cost:** cheap by design (D3). 2 assemblies per glass + sheet + redstone; strips 8 per assembly.
- **Exploits:**
  - *Free cable through runs:* runs and Festoons only power themselves (P7, D20).
  - *Dye duplication:* dye is never consumed and never dropped, so there's nothing to dupe (D5).
  - *Stonecutter value pumping:* swaps only within a yield group, 1:1 (P23); the crafted lights don't swap (D26).
  - *Festoon wire refund:* refunds exactly what was spent (P14).
  - *Floodlight griefing:* the light spot is invisible, can't be targeted and only goes into air or water, so it can't block or trap anyone (P13). It can light a neighbour's mob farm from 32 blocks away, but so can torches thrown over a wall.
  - *Lag:* lights don't tick; the grid delivers to them once a second like any device (fluid-and-power-lines D19). World light changes only on on/off and brightness steps (P28); switching many at once spreads over a second (P5).
- **Overpowered check:** a Floodlight lights a spot 32 blocks away for the price of a light, which mob-proofs at range. That's fine: one light-15 spot is one torch's worth, and it costs power.
- **Useless check:** every light does something vanilla can't (see above); the Indicator Lamp is the cheapest way to see a circuit's state on a control board.

## Create Integration
| System | Interaction | Notes |
|---|---|---|
| Power Lines, Breakers, Line Posts | The whole feed | Lights are ordinary FE devices on the `PowerGrid`; Breakers switch street circuits (D12) |
| Shafts and gears | None | Lights use FE; the shaft-swept Searchlight is parked |
| Cogwheel | Ingredient | The Warning Beacon's turning part (P29) |
| Stonecutter and Create's Mechanical Saw | Picks the light | The Saw runs stonecutting recipes, so a belt line can turn assemblies into strips automatically |
| Contraptions | Carried; glow kept, no world light, no draw | P11 |
| Aeronautics physics objects | Work from the ship's own network | P11; re-join on load like Power Lines |
| Trains | As contraptions | Station lighting is Lamp Posts, Floor Lights and Edge Lights on platforms |
| Deployers | Can place, dye, wash (water bucket) and step (wrench) lights | Natural Create behaviour; the dye and bucket stay in the Deployer |
| Logistics, vaults, funnels | None | Not storage |
| Redstone | Through Breakers only | D12; the Indicator Lamp shows a circuit's state |
| Goggles | Powered, draw, brightness, colour, Floodlight target | P16 |
| Copycats / encasing | Lines encased behind a light power it | D11 |
| Ponder | Three scenes | P17 |

## Technical Notes
- **Blocks:** one block per light kind (23 placeables, all with BlockItems), plus a Light Assembly item and an invisible **remote light** block (no item, no outline, no collision, light 15 or 10, removed by its owner). Lamp heads on arms live in the head post's block entity (kind, colour, brightness per side); the post block's light level is the brightest of its lamps.
- **Light level:** a `LEVEL` block state (0–15) drives the world light; it changes only on power on/off and brightness steps (P28). The glow, flicker and warm-up are a client renderer overlay (emissive, muted, coloured by the dye), with no ticking block entity on the server.
- **Block entity per light:** colour (dye or none), brightness step, a tiny energy buffer (2 s), switched state from the grid; the Floodlight's target; the Pendant's drop length; Festoon spans (like the posts' span list). Saved in NBT; synced on change only.
- **Energy:** an `IEnergyStorage` capability on every light (on the side touching a line, and on the mounting face for encased lines). Runs: the piece that touches a line receives and shares with its run through a cached run graph (rebuilt on change), like a tiny network.
- **Light Switch on the grid:** a Power Line piece with a switch, the same as a Breaker without the redstone input and without joining tiers (P9).
- **Recipes:** shapeless Light Assembly; one stonecutting recipe per light from the assembly, and one per light from its yield-group tag (`#comedymonkeysaddons:lights/full`, `#…/small`) for swapping; four shaped recipes for the crafted lights (P29); Lamp Posts and the Light Switch shaped. **No dye recipes** (D5).
- **Config:** FE/t per kind (P2), `floodlightRange` (32), `festoonReach` (16), `pendantMaxDrop` (16), `lightBufferSeconds` (2).
- **Data:** loot tables (drop plain), lang with subtitles, tags (lights, yield groups), JEI info pages (P20); advancements are optional, for the art pass to name.
- **Mod rules:** ids without "create"; Carry On moves lights with their colour (P10); Sable re-join on load.
- **Tests (game tests):** lit from a touching line; lit from an encased line behind it; dark without power; a strip run fed at one end lights to the far end; a run doesn't power a light or machine beyond it; the Light Switch cuts the circuit; a Breaker with redstone; dye not consumed, whole run, sneak for one; water bucket washes; breaking drops plain; brightness steps change level and draw; flicker then dark when the generator stops; Floodlight spot placed, moved by a block in the beam, removed when unpowered; Festoon spots placed and removed; Pendant lowered and dropped; Lamp Post heads and arms; stonecutter swaps stay in their group.

# ONE MINUTE UNDERSTANDING
Electric Lights is a set of 23 Create-style lights, one for each place you'd light: Lamp Posts with four street heads, wall lights, industrial and homely ceiling lights, hanging lamps, strips and edges that run along your build, festoons between buildings, a Floodlight that lights a spot far away, a turning Warning Beacon and a tiny Indicator Lamp. You craft a cheap Light Assembly and pick the light in a stonecutter. Each light runs off a Power Line touching it, even one hidden in the wall. Switch circuits with a Light Switch or a Breaker. Right-click a light with a dye to colour its glow (the dye isn't used), and step its brightness with the wrench.

# WHAT IT IS
A family of cheap electric lights fed by the mod's Power Lines, with a shape for every street, wall, ceiling and edge. Picked from one Light Assembly in a stonecutter, coloured in place with a dye that isn't used up, and switched on the circuit.

# WHY IT EXISTS
- Gives the power network a reason to reach streets and houses.
- Vanilla light has no colour, dimming or switching.
- Builders need a light shaped for each place.

# HOW PLAYERS USE IT
- Craft Light Assemblies; stonecut the light you need.
- Touch it with a Power Line, or hide the line behind it.
- Right-click with a dye to colour it; a water bucket washes it.
- Wrench to step brightness; Light Switch or Breaker to switch.
- Stack Lamp Posts, add heads and arms, feed at the foot.

# HOW IT FITS CREATE
- Fed by the mod's Power Lines, Breakers and posts.
- The Mechanical Saw stonecuts assemblies on a belt.
- Deployers place, dye, wash and step lights.
- Create palette: cast iron, steel, brass, copper, muted glows.
- Works on Aeronautics ships; inert on moving contraptions.

# IMPORTANT RULES
- A light needs a Power Line touching it.
- Strips, Edge Lights and Festoons share power along their own run only.
- Switch on the circuit: Light Switch by hand, Breaker by redstone.
- Dye colours the glow only; it's never used up.
- Breaking drops the light plain.
- Wrench steps brightness; sneak-wrench picks up.
- Short of power: flicker, then dark.
- Stonecutter swaps stay in their yield group.
- Floodlight, Chandelier, Warning Beacon and High-Bay are crafted, not stonecut.
- Runs and Festoons never carry power anywhere else.

# EDGE CASES
- **Generator runs dry?** Lights flicker for a second, then go dark.
- **Line hidden in the wall?** The light on it still gets power.
- **Block placed in a Floodlight's beam?** The spot lands on that block.
- **Block placed on a Festoon's light spot?** The spot is removed.
- **Lights on a moving contraption?** Glow kept, no world light, no draw.
- **Pendant broken 10 blocks down?** Lamp and cable drop back.
- **Dyed light picked up with Carry On?** It keeps its colour.
- **Hundreds of lights switched at once?** They come on over a second.
- **Chunk unloads?** Lights there draw nothing; they relight on load.
- **Strip run with mixed colours?** Still one run; colours stay per block.

# DEVELOPMENT CHECKLIST
All built by 2026-10-28 (commits 4cdb3d7 to cc70765), with game tests for each part.
- [x] Light Assembly item and recipe
- [x] 23 blocks with BlockItems: placement rules (P6), shapes, waterlogging (P12; the Chandelier is waterloggable too), hardness (P30)
- [x] Light block entity: colour, brightness, buffer, lit state; NBT and change-only sync
- [x] `LEVEL` block state for world light; one change per on/off or step (P28)
- [x] FE capability on lights (touching side and mounting face); grid delivery as ordinary devices
- [x] Runs: Light Strip and Edge Light run graphs; Festoon spans; power shared within the run only
- [x] Light Switch as a switched Power Line piece (P9)
- [x] Lamp Posts: two finishes, stacking roles, head on top, 4 arms in the BE, foot feed, span bracket, one junction per colour
- [x] Lamp heads standable on any block top
- [x] Pendant and Chandelier lowering with hanging cable (D19)
- [x] Floodlight pairing, beam check once a second, remote light block (D18, P27, P13)
- [x] Festoon hanging, wall hooks, light spots every 4 blocks, cost and refund (P14)
- [x] Warning Beacon turning part (drawn as parts: Flywheel when it's on)
- [x] Dyeing in place (not consumed, whole run, sneak for one), water-bucket wash, plain drops
- [x] Wrench: brightness steps, sneak pick-up, head and arm removal, Floodlight aiming (P26)
- [x] Renderer glow overlay: dye colour, flicker, warm-up (P3, P4), ripple on switch-on (P5)
- [x] Contraption and Aeronautics behaviour (P11: lights move with their blocks on Create contraptions; Aeronautics ships not yet tried in game)
- [x] Recipes: stonecutting per light, yield-group tag swaps, crafted big lights (P29), Lamp Posts, Light Switch
- [x] Tags, loot tables, lang and subtitles, JEI info pages (P20)
- [x] Config keys (Technical Notes)
- [x] Goggle readouts (P16); three Ponder scenes (P17)
- [x] Sounds on the staging ladder: switch click, beacon whirr, flicker buzz, warm-up tick (P19)
- [x] Game tests (Technical Notes)
- [x] Art: a brief and Blockbench previews for every light before code (D8)

# "IDIOT TEST"
- Lights run on FE from Power Lines.
- A line must touch the light.
- Lines hidden in walls still count.
- Strips and edges pass power along themselves.
- Craft an assembly, stonecut your light.
- Right-click a dye: glow changes, dye kept.
- Breaking drops it plain again.
- Wrench steps the brightness.
- Light Switch or Breaker switches circuits.
- Street lamps stack like power poles.

# COMPLETENESS SCORE
Gameplay: 100% · World Integration: 90% · Balance: 85% · Progression: 90% · Automation: 85% · Edge Cases: 85% · **Overall Design Readiness: 85%**

- Gameplay 100%: every decision and default is approved.
- World Integration 90%: how the glow looks with shaders, and the Floodlight cone in fog, need an in-game look.
- Balance 85%: the draw figures (P2) need a play test against a real town on one Lava Generator.
- Progression 90%: the Chandelier's brass gate makes it the only mid-game light; check that's wanted.
- Automation 85%: the Mechanical Saw making lights and Deployers dyeing them need game tests.
- Edge Cases 85%: the remote light block under Aeronautics ships and in flowing water needs a game test.

