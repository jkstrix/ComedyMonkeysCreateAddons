# Design Brief: Clockwork Wastes, the Building Kit (group 1b)

The first step of D71 ([spec](../specs/clockwork-wastes.md)): a kit the city's buildings are built from, so that no building in the city is a one-material box again. It follows the look bible in [clockwork-wastes-world.md](clockwork-wastes-world.md) (three layers, palette, motifs, rings of damage) and the shared rules in [visual-language.md](../visual-language.md). It uses the blocks in [clockwork-wastes-blocks.md](clockwork-wastes-blocks.md) and the props approved in D72 (`design/models/clockwork-wastes-city-props.gen.js`) without changing their looks.

Contents: Hard Requirements, what the in-game walk showed, the shared grammar (palette, relief, mixing, damage), the 13 building types, the void-alloy building block set, the catalogue, and the choices that need the user's agreement. Every design call the briefs don't already make is listed at the end; nothing here is final until the user agrees.

## Hard Requirements

From the user:
- **The verdict (first walk, D71):** "Buildings are bland monoblocks, no texturing or detail."
- **D71:** "an art-director brief per building type (makers' shed, furnace shed, terrace, long workshop, slotted tower, alcove wall, gantry, Substation yard, heart drum, and their war-damaged versions) with real relief and reasoned wear; a void-alloy building block set (stairs, slabs, walls, slotted grille panel, trim or cornice, a clamp bracket onto old stonework) previewed in Blockbench first; each building built as an in-game catalogue (`/wastes buildcatalogue`), shown to the user as in-game screenshots (street level and distance) before they walk it; approved buildings then replace the boxes in the city."
- **D72 (approved props, looks unchanged):** the Brass Plaque "beside makers' shed doors and on terraces about 1.5 up, askew in the outer districts"; the Charging Alcove's `trinket` state (none, cog, key, cart, framed flower) "in 1 alcove in 5, kind at random"; its `vines` state "about 1 in 4 in the Quarters' alcove wall and split tower"; the Assembly Clamp, stages 1–5 (stage 5 replaces the `frozen` Worker on the line); the mast riser, a `girder` mount of Derelict Cable; and "**corner masts**: each district also gets a lattice mast with a riser at a street corner, as the world brief says".
- **D66:** the city is reviewed in game, not as Blockbench vignettes. **D39:** everything ships; nothing deferred.

From the world brief (quoted):
- The three layers: "Every structure and block belongs to one of three layers. A player should be able to tell which by looking." Makers' era: "Familiar. 'I know what that is.' Create, abandoned." The uprising: "Void-alloy with hairline starlight seams; no rivets, no bolts, no wood: cast and fitted, never fastened ... Smooth, faceted, tall, built for bodies that don't sit." The war: "Violent, then still. Heaviest at the edge, lightest at the heart."
- "**Automatons build over, never with.** Void-alloy is clamped onto old stonework, threaded through old windows, stood on old foundations. It never mixes into a makers' wall as random blocks."
- "**The machines' scale wins.** Automaton doorways are 2 wide × 3 tall; ceilings are high; stairs are rare (they used ramps and lifts)." Makers survive "in what's left of their scale: a few human-sized doorways, bricked up."
- "**Tall, narrow rectangles with chamfered tops** dominate the automatons' layer ... **The makers' layer is squarer and lower**: Create-style sheds, boxes and pitched roofs. The war breaks both into **jagged diagonals**: snapped towers, leaning posts, splayed scrap."
- The district table (Foundry: brick furnace sheds, a cold blast furnace, chimneys, one chimney toppled across the street, a scorch fan up a furnace wall; Workshops: long sheds, assembly gantries, a shed roof collapsed onto the line, bolt craters; Rail Yard: track, signal box, platform canopies; Quarters: makers' terraces with small doorways bricked up, alcove walls stacked 3–5 high reached by ramps and lifts, one alcove tower split down the middle, barricades across the main ramp).
- "**Rings of damage** like the Juggernaut ruins ... reversed: from the districts outward, collapse grows." "Intact core, broken edge: collapse at the outskirts, the heart nearly whole and frozen mid-shift."
- "**Streets:** ... Derelict cable runs along the walls at head height on brackets, rising up lattice masts at the corners." "**Brass makers' plaques** on old buildings, worn smooth: the only writing, and it's unreadable."
- Skyline: "a ragged ring of broken towers round a taller, intact centre spire, with a web of cables slung from the ring to the spire. 'A spider web strung between broken teeth, with a needle in the middle.'"
- "**Warning colours only in Substation yards.**" "**The resistance never glows.**" "The Juggernaut's motifs are banned here: runes, frost, knotwork, gold."

Standing art rules (user): dark and muted, light areas mid grey at most, glows muted and never near-white; accents are trims; ordinary blocks restrained, big industrial structures get real relief hardware; real-object detail welcome when every part is a real component (the Cuckoo Clock); round only for motors and engines; every look previewed in Blockbench before it goes into the mod; no choices without the user's agreement.

### What the walk showed, and where it comes from in the code

`CityLayout` builds every building from one routine per kind, and each routine fills a box with one block:
- `shed()`: four walls of one block (`material(d)`: Bricks, Andesite Casing or Stone Bricks), a window of Iron Bars wherever `(u + v) % 4 == 0` on row 2, and a **flat** roof of Dark Oak slabs with holes rolled at random (`r.nextInt(100) < 12 + max(u, v)`). No plinth, no piers, no cornice, no eaves, no pitch: a lidded box. The terraces, furnace shed, workshops, signal box and control hut are all this box.
- `vaultHouse()` and `skyTower()`: plain void-alloy boxes with one seamed course; the tower's "slots" are air, so they read as holes, not windows.
- The heart drum: a 14-high octagon of vanilla End Stone Bricks (lum 215, the brightest surface in the city, against the house rule and the world brief's own stained-brick palette), its top course gapped at random.
- Damage everywhere is a random roll (roof holes, gaps), so it reads as noise, not as something that happened.
- Layer slips: the alcove wall's ramp is Stone Brick slabs and its railings Iron Bars (makers' materials on the uprising's work); the terraces use Mossy Stone Bricks (nothing grows in the End).

The kit fixes all of this with four things: **relief in depth** (every wall has at least three planes), **a vernacular per layer** (the same rules on every building of a layer), **mixing by reason** and **damage by story**.

## Asset Type

World structure kit: 13 building types, each in up to four states (intact, worn, damaged, ruined), built in code from vanilla, Create 6.0.10 and mod blocks (16 px = 1 block; all sizes below are in blocks). Plus **7 new void-alloy building blocks** (previewed in Blockbench later) and, if the user agrees, a small makers' masonry set (choice 1). No Blockbench geometry for the buildings themselves: they're reviewed in game through `/wastes buildcatalogue` (D66, D71).

## Fantasy

Walk down a street and read three lifetimes off one facade: the makers' heavy brick sheds with their slate roofs and copper ridges, built at human height; the doorway someone cut twice as tall and lined with black metal; and the bolt hole above it, scorched, with the roof slumped in behind. Every building tells you who built it, who took it, and what hit it, without a word.

## Hero Element

**The join.** The place on every building where void-alloy grips, cuts into or rises out of the makers' stonework: the clamp biting a cracked brick, the human doorway cut up to 2 × 3 and lined, the slotted tower punching up through a slate roof. It's the uprising's story in one detail, and it's on every building type. Everything else (piers, roofs, bands of light, damage) supports it.

## Theme

### Palette by role (measured)

Values are the mean texture luminance (0–255; mid grey is 128), measured from the Create 6.0.10 and Minecraft 1.21.1 textures. Mid grey at most applies to every large surface.

**Makers' masonry (one main stone per district, so districts read apart from the air):**

| District | Wall field (main, ~70%) | Second (close, ~20%) | Piers and dressings | Plinth | Why |
|---|---|---|---|---|---|
| **Foundry** | `create:cut_scoria_bricks` (64, warm dark red-brown) | `create:small_scoria_bricks` (65) | `create:scoria_pillar` (58); lintels and corbels `create:cut_scoria_brick_stairs` / `_slab` | `create:polished_cut_scorchia` (51) | Firebrick, darkened by a century of smoke. Replaces vanilla Bricks (`#976253`, a saturated orange-red) |
| **Workshops** | `create:cut_tuff_bricks` (112, neutral grey) | `create:small_tuff_bricks` (112) | `create:andesite_casing` piers (Create's own post; 95); dressings `create:polished_cut_tuff_stairs` / `_slab` | `create:cut_deepslate_bricks` (84) | Create's andesite world, a step darker. Cut andesite itself is 142, over the limit |
| **Rail Yard** | `create:cut_deepslate_bricks` (84, blue-grey) | `create:small_deepslate_bricks` (85) | `create:deepslate_pillar` (88); dressings `create:polished_cut_tuff_stairs` / `_slab` (lighter, for contrast) | `create:polished_cut_scorchia` (51) | Blue engineering brick, the real railway brick of viaducts and sheds |
| **Quarters, heart drum** | Stained End Stone Bricks (choice 1; bricks `#827d5f` `#8d876a`, mortar `#6c6750`, about 125) | Cracked Stained End Stone Bricks (only at causes) | `create:deepslate_pillar`; dressings `create:cut_deepslate_brick_stairs` / `_slab` | `create:cut_deepslate_bricks` | The makers' homes and their great hall, built from the island's own stone (world brief palette). Vanilla End Stone Bricks are 215 |

**Shared by every makers' building (one town):**

| Part | Blocks | Value |
|---|---|---|
| Roof field | `minecraft:deepslate_tile_slab` / `_stairs` (slate) | 55 |
| Ridge roll | `minecraft:weathered_cut_copper_slab` (the copper trim; patina from age) | 130, a 2-block-wide line only |
| Barge (gable edge) and rafters | `minecraft:dark_oak_slab` / `_stairs` / `_fence` (walnut) | 48 |
| Windows | `create:weathered_iron_window` (connects into tall windows; aged iron glazing) | 73 |
| Makers' doors (human, 1 × 2) | `minecraft:dark_oak_door` | 56 |
| Railings, fences, grilles on makers' work | `create:andesite_bars` | 107 (replaces Iron Bars, 138) |
| Steel straps and hoops | `create:weathered_iron_block`, `create:industrial_iron_block` | 78, 80 |
| Pipes | `create:fluid_pipe` (Create's copper; approved as is) | trim only |
| Soot | `minecraft:polished_blackstone_bricks`, `cracked_polished_blackstone_bricks`, `polished_blackstone` | 45, 40, 51 |
| Plaque | the mod's Brass Plaque (D72) | prop |

**The uprising:** Void-Alloy Plate (`va_tile`), Seamed Void-Alloy Plate, Void-Alloy Pillar, and the new set below (stairs, slab, wall, grille, band, clamp bracket, scarred plate). About 29 lum: they are the darkest things in the city on purpose. Starlight per the visual language (dim `#3a3552`, edge `#7f76a8`, core `#b8b0d8` at single pixels).

**Excluded, and why:** Create limestone (about 185) and calcite (white); vanilla End Stone Bricks (215), smooth stone (159); Create framed glass (199, pale cyan); Brass Block (161) and Brass Bars (180) as anything bigger than a trim; Iron Bars on anything new; any mossy block (no rain, nothing grows in the End; the only plant life is D72's dead vines in Quarters alcoves). Create's palette stones have no cracked variants: a "cracked" Create brick is shown as its natural stone where the face spalled (`create:scoria`, 59; `minecraft:tuff`, 108), or vanilla `cracked_deepslate_bricks` (65) for deepslate.

### Shape language

- **Makers: squares, low.** Rectangular plans, even widths, 1:2 slab pitches (about 27°, "squarer and lower"), heavy plinths, piers on a 4-block rhythm. Human scale shows in doors (1 × 2), windows (1 × 2) and plaques at 1.5 blocks.
- **The uprising: tall narrow rectangles, chamfered tops.** Height at least 4 × width on towers; openings 1 × 3 or 2 × 3; every top steps in with void-alloy stairs; bands of light every 6–7 blocks. Circles only in lenses (lamps, alcove contacts).
- **The war: jagged diagonals.** Breaks run diagonally across a facade (a stepped line, 1 block per block), towers snap on a slant, beams come down on a slope. Never a clean horizontal cut.

## Silhouette Description

- **A makers' street:** a low terrace of slate pitches and copper ridge lines at 9–12 blocks, broken by chimneys (Foundry), saw-teeth (Workshops) and a raised monitor roof (furnace shed).
- **Over it, the uprising:** dark slotted towers at 18–30 blocks, each ringed with thin bands, standing on old plinths or punching through old roofs; the alcove wall's honeycomb; the sky tower's T-head holding its cable.
- **Lit:** the towers' rings, the honeycomb's contacts and the grille slots glow as horizontal dashes; the cables draw the web; the spire's corner seams climb to the crown. A district lit reads as stacked rings of light under a web, round a needle.
- **From the next island:** the ring of towers stays the "broken teeth" because the outer towers are snapped on a slant; the spire stays the needle because it ends in a thin post tip, not a block.

## Secondary Forms (the shared grammar)

### Relief: every wall has at least three planes

The single biggest fix. A wall is never one flat plane of one block. For makers' walls, outward from the wall plane:
1. **Plinth:** one course of the plinth stone in the wall plane, and a **ground step** of the plinth stone's stairs 1 proud along its foot (right way up, step outward). Heavy buildings (drum, furnace shed) get two courses and a stair water table on top.
2. **Piers:** the dressing pillar, **1 proud**, every 4 blocks (pier + 3-block bay), at every corner, and either side of every door. Piers run from the plinth to the corbel course.
3. **Window bays:** one window per bay, 1 × 2, centred, with a **sill** (upside-down dressing stair, 1 proud) under it and the **lintel band** (dressing slab, in the wall plane, through the piers) above.
4. **Corbel course:** a row of upside-down dressing stairs, 1 proud, at the top of the wall. On the drum and furnace shed, a corbel table: corbels on alternate blocks with slabs between.
5. **Eaves:** the roof's first course overhangs the corbels by 1, and the gables by 1 (barge in walnut slabs, 1 proud of the gable).
6. **Roof:** 1:2 slab pitch (bottom slab, top slab, step up), slate, a 2-wide ridge (even spans so doors and ridges centre together) with a weathered copper ridge roll. Inside, a **truss** at every pier: a walnut slab tie beam and a walnut fence king post. They're what a roof hole shows.

For void-alloy walls: wall plane in Void-Alloy Plate, **corners** in Void-Alloy Pillar (chamfered), a **Void-Alloy Band** course every 6–7 blocks, openings as **Slotted Grille** runs (1 × 3) in deep reveals, and the top **chamfered** in void-alloy stairs sloping in, capped with slabs. Portals (sliding doors) project 1 block from the wall in a frame of pillars, a band head and a chamfer.

### How void-alloy joins makers' work (the hero, five ways only)

| Join | What it is | Where |
|---|---|---|
| **Clamp** | A Clamp Bracket gripping a makers' face or wall top; the stone under its jaws is the cracked/spalled variant (the bite) | Insulators and cable on gables and party walls; tower feet on old plinths; the furnace launder on the furnace hoops; the spire collar on the dome ribs |
| **Lined cut** | A makers' opening cut bigger (to 2 × 3), its cut edges lined with void-alloy (pillar jambs, a pillar head), 1–2 spalled stones where the cut broke the bond | Doorways of sheds the uprising used; the heart doors in the drum |
| **Threaded** | Derelict Cable entering one old window and leaving by another, or running through a cut in a lintel | Terraces, sheds, the control hut |
| **Stood on** | A void-alloy building standing on a makers' plinth whose ledge (1 wider all round) still shows, clamped at intervals | Vault house, slotted towers, sky tower, alcove wall |
| **Rising through** | A void-alloy tower standing inside a makers' building and breaking out through its roof; the slates gone in a 1-block ring round it, the cut rafters clamped to it | The "through-roof" slotted tower |

Never: void-alloy blocks inside a makers' wall field, void-alloy replacing makers' bricks one for one, makers' stone or iron in void-alloy work.

### Block-mixing rules

1. **One main block per surface (60–80%).** At most 3 others, all close to it in value (within about 15 lum) and hue: the district's small-brick variant, its cracked or spalled stone, and soot where there's fire.
2. **Each variant has a fixed place or a cause, never a dice roll on an open wall:**
   - small-brick variant: **gable triangles** and the panels between windows (lighter, non-bearing infill), never at random;
   - cracked/spalled: within 1 block of a cause (a clamp's jaws, a lined cut's edge, a strike, the top of a broken wall, the stones a fallen block hit);
   - soot: in a fan above its source (a window or door fire vented from, a chimney top, a furnace mouth, a strike, arcing contacts), densest at the source;
   - weathered/oxidised copper: patina on all old copper (age; no rain), bare copper only where something rubs.
3. **Gradients, not noise.** Soot and cracks fade by distance from the cause in deterministic bands: the inner band all the dark variant, the middle band about half (alternate blocks by a position hash), the fringe a quarter. A hash may only break up a band's edge; it never places a mark outside a band.
4. **Void-alloy isn't mixed.** Plate, seamed plate and band are placed by design (courses, corners, grooves). Scarred plates are placed only by the damage rules: **one mark per outward face, none at the heart**.
5. **No moss, no vines on makers' walls, no grass.** Dead vines live only in Quarters alcoves (D72).
6. **Dust:** our own blocks carry the End-dust crust on top edges in their textures (blocks brief). Vanilla and Create blocks get none; never fake dust with pale blocks.

### Wear and war damage: the rings and the reasons

**State by ring** (distance from the city's middle, the larger of |x| and |z|; choice 10):

| State | Where | What it allows |
|---|---|---|
| **Intact** | the heart (to 22) | Wear only: soot above chimneys, patina, plaques straight. The frozen shift. No war marks |
| **Worn** | the districts' inner half (23–45) | One strike per building at most, on its outward face; a few slates slipped at the outer eaves; one chip per outward void-alloy face |
| **Damaged** | the districts' outer half (46–70) | Strikes, a roof hole or a fallen bay at the outer end, broken windows on the outward face, towers snapped on a slant, plaques askew |
| **Ruined** | the outskirts (71+) | Stubs, the outer walls fallen, rubble cones, only the plinth and corner piers standing |

**Within a building, damage starts at its outer end** (the end or corner farthest from the heart) and grows toward it. A building is never evenly damaged.

**The marks, each with its reason:**
- **Strike** (a war bolt or shell from outside): a hole 1–3 blocks across in the outward face, a ring of cracked/spalled stone round it, a soot fan above it (heat rises), the knocked-out blocks inside on the floor (it came from outside). On void-alloy: a chipped or cracked scarred plate, heat tint above.
- **Fire:** soot fans up the outer face above windows and doors where it vented; the windows in the fan are gone; the roof above is holed (it burned through) with charred rafters (walnut fences, a few `minecraft:dark_oak_log` stubs).
- **Roof hole:** slates gone over 2–4 blocks; the rafters and a truss (walnut) still there, one rafter broken and hanging; fallen slates (slate slabs) on the floor under it.
- **Collapse:** the outer gable's top falls outward first, then the roof over the last bay slumps in (missing, with a broken truss hanging), then the outer wall comes down to its sills. A **rubble cone** at the foot on the side it fell, made of that wall's own blocks with its slabs and stairs (about a third of the missing volume; the rest crumbled), never mixed with another building's stone. The break line on what's standing is a stepped diagonal.
- **Snapped tower:** a diagonal break across the shaft (stepped, 1 per block), scarred plates along the break, the fallen top lying in the street in the direction it fell (away from the blow: toward the heart), its band and grilles still recognisable.
- **Soot above chimneys:** every chimney's top 2–3 courses are soot blocks, ragged at the lower edge (smoke, not war). Applies intact.
- **Arc scorch:** a soot fan above arcing contacts (the yard's transformer bushings); heat tint on void-alloy above them.

## Primary Forms: the 13 building types

Sketch key (1 character = 1 block): `B` plinth, `b` ground step (1 proud), `P` pier (1 proud), `W` wall field, `w` small-brick infill, `G` window, `s` sill (1 proud), `L` lintel band, `c` corbel (1 proud), `/` `\` roof pitch, `~` ridge roll, `K` Brass Plaque, `x` cracked/spalled, `v` void-alloy pillar (lining), `p` void-alloy plate, `|` void-alloy pillar (corner), `=` Void-Alloy Band (lit), `g` Slotted Grille, `^` void-alloy stairs (chamfer), `-` void-alloy slab, `#` soot, `.` air.

### 1. Makers' shed (pitched roof)

The town's common building, in all four districts (lot A's two sheds, lot N1's small shed, the Rail Yard's signal box and the yard's control hut are sizes of it).
- **Footprint** 8–12 wide (even, so the ridge and a centred door line up) × 9–21 long; the yard hut and signal box at 4–6 × 4–6. **Height:** walls 5–6 to the eaves; ridge at 8–10. Small sizes: walls 3–4, ridge 5–6.
- **Silhouette:** a long low box under a shallow slate pitch with a copper ridge line; piers make a ribbed side; sheds 15+ long carry a **ridge vent** (a 2-wide, 1-high louvred box on the ridge of every other bay: dark oak trapdoors as louvres, a slate slab lid).
- **Palette:** the district's masonry row (Theme), the shared roof, windows and doors. Proportions on a long side: wall field 55%, piers 20%, windows 10%, plinth 10%, dressings 5%.
- **Relief:** the full grammar: plinth + ground step, piers every 4, windows 1 × 2 with sills, lintel band, corbel course, eaves 1 over, barge 1 proud, trusses inside.
- **Doors:** the makers' doorway was 1 × 2 in the gable. Where the uprising used the shed, it's a **lined cut**: 2 × 3, jambs and head in Void-Alloy Pillar, 1–2 spalled stones beside it. Sheds they didn't use keep their walnut door (1 × 2) shut.
- **Plaque (D72):** on the gable, beside the door, on the first wall block after the door's pier or jamb, at 1.5 blocks (the block at y 1), facing out. Askew (hanging from one bolt) in the damaged and ruined states.
- **The uprising on it:** the lined cut; on the gable apex, a Clamp Bracket (saddle) holding a Derelict Insulator where a district span passes; Derelict Cable threaded in through one side window and out through the next; charging alcoves along the inside of the long walls (lot A's shed 2). The **through-roof** slotted tower (type 5) uses a large shed as its host.
- **States:** intact (patina, sooted chimney if any); worn (a strike on the outward gable, slates slipped at the outer eaves); damaged (roof hole over the outer bay showing its truss, outward windows gone, fire soot above them, plaque askew); ruined (outer gable and last bay down into a rubble cone, the far end standing to its corbels).

```
Long side, 13 long (worn):                 Gable, 10 wide, with a lined cut (cols -1..10):

y8   ~~~~~~~~~~~~~~~   ridge roll          y9        ~~          ridge roll, 2 wide
y7  /////////////////  slate courses       y8      /ww\          w: small-brick gable infill
y6  /////////////////  eaves, 1 over       y7    /wwwwww\        / \: slate + walnut barge, 1 proud
y5   ccccccccccccc     corbels, 1 proud    y6  /wwwwwwwwww\      eaves, 1 past the gable
y4   PLLLPLLLPLLLP     lintel band         y5   PccccccccP
y3   PWGWPWGWPWGWP                         y4   PLLLLLLLLP
y2   PWGWPWGWPWGWP                         y3   PWxvvvvWWP       v: void-alloy head over the cut
y1   PWsWPWsWPWsWP     sills, 1 proud      y2   PWWv..vxWP       x: spalled where the cut broke the bond
y0   BBBBBBBBBBBBB     plinth + step       y1   PWKv..vWWP       K: Brass Plaque, 1.5 up
                                           y0   BBBv..vBBB
```

### 2. Brick furnace shed, with chimney and blast furnace (Foundry, lot B)

- **Footprint:** the hall 17–19 × 13–15; the blast furnace 5 × 5 at one end; the chimney 3 × 3 at the other (base 5 × 5). **Height:** walls 7–8; a **monitor roof** (a raised clerestory along the ridge, 5–6 wide, 2 high, louvred sides) to 13–14; the blast furnace 16–18; the chimney 20–22.
- **Silhouette:** a heavy hall with a stepped monitor roof, a tall square stack at one end and a stepped-in furnace stack at the other: the Foundry's skyline reads as industrial teeth even before the war.
- **Palette:** Foundry row. Walls cut scoria bricks 65%, small scoria bricks 15% (spandrels, gable), scoria pillar piers 10%, scorchia plinth and arches 10%. Roof slate; monitor louvres dark oak trapdoors; hoops `create:weathered_iron_block`.
- **Relief:** walls 2 thick. Bays are **arched furnace mouths** on the long side facing the casting floor: openings 2 wide × 3 tall with a block arch over each (upside-down scorchia stairs either side of a slab key, the arch 1 proud), recessed 1 into the wall; piers between them; a corbel table under the eaves; two-course plinth with a stair water table. Inside: hearths along the walls (vanilla Blast Furnaces set into a scoria bench under small arches), cauldrons as crucibles, the broken press and mixer.
- **Blast furnace (real object):** a square stack of cut scoria bricks, 5 × 5 to y 6, stepped in by a stair course (the batter) to 3 × 3, banded every 3 blocks by a hoop course of weathered iron; at its foot a **bustle pipe** (a ring of `create:fluid_pipe` at y 3 round the stack, standing 1 off it on clamps of weathered iron) with four short downcomers into the base (the tuyeres); a tap hole (a 1 × 1 opening at y 0 on the casting floor side) under a soot fan; a charging platform at the top (a walnut slab deck on weathered-iron brackets) reached by a Create andesite ladder. Square, because it's a vessel, not an engine.
- **Chimney:** 5 × 5 plinth 2 high, 3 × 3 shaft, one stair set-off at a third of its height, hoop courses every 5, a **corbelled cap** (a course of upside-down stairs 1 proud, then a band of polished cut scorchia), a 1 × 1 flue (soot inside). Top 3 courses soot (smoke). An andesite ladder up one face.
- **Plaque:** beside the main door (in the gable facing the street), 1.5 up.
- **The uprising on it: the launder.** From the tap hole to the casting floor's moulds, a void-alloy pouring channel (Void-Alloy Slabs on Void-Alloy Wall legs every 3, sloping down in half-block steps) clamped to the furnace's bottom hoop by two Clamp Brackets. They used the makers' furnace to cast their own bodies; the launder is the join. Its first blocks are heat-tinted scarred plates (heat).
- **States:** worn (soot over the tap hole and chimney top only); damaged: the **scorch fan up a furnace wall** (world brief): its source the outer arched mouth, full soot 2 blocks round it, half to 4, a quarter to 6, up the wall and onto the monitor; monitor louvres burnt out over that bay; the **second chimney toppled across street A** (world brief): the stump broken on a stepped diagonal 5–8 high, the fallen shaft lying in segments with 1-block gaps where it broke on landing, its corbelled cap lying at the far end, the stones it hit cracked; ruined (catalogue only): the hall's outer end down, the furnace stack standing.

```
Long side toward the casting floor, 19 long (damaged), chimney at left, blast furnace at right:

y21  .ccc.                                      chimney cap: corbels 1 proud
y19  .###.                                      soot (#) in the top three courses
y16  .WWW.                          .WWW.       blast furnace top: charging deck on brackets
y15  .???.                          .???.       ?: hoop courses (weathered iron) every 3-5
 ..   WWW                            WWW
y13  .WWW.     ..ddddddddd..        .WWW.       d: monitor louvres (dark oak trapdoors), 2 high
y11  .WWW.   ////ddddddddd////      .WWW.
y10  .WWW. /////////////////////    .WWW.
y9   .WWW.  cCcCcCcCcCcCcCcCcC      ?????       corbel table
y8   .WWW.  PLLLPLLLPLLLP#LLLP      .WWW.
y7   .WWW.  PW^^WP^^WP^^WP#^^#P     ^WWW^       stair batter: 5 x 5 steps in to 3 x 3
y6   .???.  PW..WP..WP..WP#..#P     ?????       ^^ block arch over each furnace mouth, 1 proud
y4   .WWW.  PW..WP..WP..WP#..#P    oWWWWWo      o: bustle pipe on clamps, 1 off the stack
y3   .WWW.  PW..WP..WP..WP#..#P     WWWWW       # : the scorch fan from the outer mouth
y1   BBBBB  BBBBBBBBBBBBBBBBBBB     WWWWW
y0   BBBBB  bbbbbbbbbbbbbbbbbbb     WW.WW-->    tap hole and the void-alloy launder to the moulds
```

### 3. Makers' terrace with bricked-up doorways (Quarters, lot B)

- **Footprint:** a row of 4–6 houses, each 5 wide (party walls shared) × 7 deep; the row 21–29 long. **Height:** two storeys at human scale (3 blocks floor to floor), walls 7 to the eaves; 1:1 roof (houses pitch steeper than sheds) to 10–11; a party-wall chimney stack (1 × 2, rising 2 above the ridge) between each pair.
- **Silhouette:** a row of small, steep roofs with chimney stacks marching along the ridge: the most human-scaled thing in the city, which is the point. Against the alcove wall behind it, it looks like a toy.
- **Palette:** Quarters row (stained End brick 70%, deepslate pillar at the party walls and ends 15%, dressings and plinth 15%); slate roof; walnut doors and window frames; soot on chimney tops.
- **Relief:** party walls show as piers on the front (1 proud) dividing the houses; each house a **doorstep** (one plinth stair), a door 1 × 2 and a ground-floor window 1 × 2 with a sill; upstairs two 1 × 1 windows under the lintel band; corbel course; eaves 1 over. Inside (seen through the ruined end): walnut slab floors at the storey line, a **fireplace** on each party wall at both storeys (a scoria-brick hearth with a soot flue), a walnut stair.
- **Bricked-up doorways:** every front door and ground-floor window infilled flush with **hasty rubble**: `minecraft:cobbled_deepslate` with a spalled brick or two, no coursing, no sill, a mismatched lintel. Who bricked them is choice 5 (recommended: the makers, barricading themselves in during the uprising before the resistance flew them out).
- **Plaque (D72):** one per house, beside its (bricked) front door, 1.5 up. Askew on the outer houses in the damaged state.
- **The uprising on it:** Derelict Cable threaded through the upper windows along the row (in one house, out the next); a Clamp Bracket on each party-wall pier at the eaves holding the cable off the wall. Nothing else: they had no use for the houses.
- **States:** worn (a strike on the outer end house, soot from one upper window); damaged: **the outer end house's front wall collapsed into the street** (rubble cone), exposing its floor line, both fireplaces stacked on the party wall, the stair; the neighbour's roof holed; ruined (catalogue only): only the party walls and chimney stacks standing, like a row of headstones.

```
Front, 3 of the row (intact), houses 5 wide, party-wall piers P:

y11     .##...##...##.          chimney stacks on the party walls, sooted tops
y10    //~~\\//~~\\//~~\\        1:1 slate, ridge roll
y8   //////////////////////     eaves 1 over
y7    ccccccccccccccccccc
y6    PWGWGPWGWGPWGWGPWGWP      upper windows 1 x 1
y5    PLLLLPLLLLPLLLLPLLLP      lintel band (storey line)
y4    PWWWWPWWWWPWWWWPWWWP
y3    PRGWKPRGWKPRGWKPRGWP      R: bricked-up door (rubble infill), G: bricked window; K: plaque
y2    PRsWWPRsWWPRsWWPRsWP
y1    BRBBBBRBBBBRBBBBRBBB      doorstep stair 1 proud at each R
```

### 4. Long workshop shed (Workshops, lot B)

- **Footprint** 9–12 wide × 24–30 long (two, side by side, as built). **Height:** walls 6; a **sawtooth (north-light) roof** to 10: teeth across the long axis every 4 blocks, each a 1:1 slate slope rising 3 to a vertical glazed face 3 high (`create:weathered_iron_window`, connected), the glazing facing the heart.
- **Silhouette:** a saw blade along the skyline: the Workshops' signature, unmistakable from the next island.
- **Palette:** Workshops row: cut tuff bricks 60%, andesite casing piers 20% (the teeth's trusses bear on them), glazing 10%, plinth and dressings 10%.
- **Relief:** piers every 4 (one under each tooth's valley), tall side windows 1 × 3 per bay, sills, lintel band, corbels. At the gable facing the street, a **loading door** 4 wide × 4 tall (the one makers' opening big enough that the uprising never cut it), with a walnut lintel beam and `create:train_trapdoor` panels as a half-raised roller door.
- **Inside (real objects, seen through the door and holes):** walnut benches with Broken Mechanical Arms and Deployers (as built); an overhead **line shaft**: Create Shafts along each truss line on `create:metal_bracket`s, with a cogwheel over each bench, the makers' rotation town's power; walnut trusses.
- **Plaque:** beside the loading door, 1.5 up.
- **The uprising on it:** a lined cut (2 × 3) in the long side opening onto the gantry lot; charging alcoves against the inner piers at the far end; cable threaded through the clerestory glazing.
- **States:** worn (outward glazing broken in one tooth); damaged: **"a shed roof collapsed onto the line"** (world brief): the last two teeth at the outer end down on the benches (slate slabs and a broken truss across them, glazing gone, the line shaft fallen in two lengths), rubble on the floor not outside (it fell in); ruined (catalogue only): the outer half's walls to sill height.

```
Long side, 28 long (damaged: the outer end at right):

y9   G///G///G///G///G///.....         G: glazed tooth face, 3 high; / slate slope
y8   G///G///G///G///G//.......
y7   G///G///G///G///G/....x...         the last two teeth down
y6   ccccccccccccccccccccc..xcc
y5   PLLLPLLLPLLLPLLLPLLLPLLLPLLL
y4   PWGWPWGWPWGWPWGWPWGWPW.WPW.W       windows 1 x 3; outward ones gone
y3   PWGWPWGWPWGWPWGWPWGWPW.WPW.W
y2   PWGWPWGWPWGWPWGWPWGWPW.WPW.W
y1   PWsWPWsWPWsWPWsWPWsWPWsWPWsW
y0   BBBBBBBBBBBBBBBBBBBBBBBBBBBB
```

### 5. Void-alloy slotted tower

The uprising's commonest building and the skyline's "teeth".
- **Footprint** 3 × 3 to 5 × 5. **Height** 14–30 (at least 4 × the width). Two placings: **on old foundations** (it stands on a makers' plinth fragment one wider all round, clamped) and **through a roof** (it stands inside a big makers' shed and breaks out of its roof: slates gone in a 1-block ring, cut rafters clamped to it).
- **Silhouette:** a tall dark shaft, ringed with thin bands, a stepped chamfer and a small cap at the top.
- **Palette:** Void-Alloy Plate 60%, Pillar corners 20%, Slotted Grille 10%, Band 7%, stairs and slab at the crown 3%.
- **Relief:** chamfered corners (pillars); one grille run (1 × 3) centred on each face, repeated every 7 blocks, alternating faces on 3 × 3 towers; a Band course every 7 (between grille runs); at the top a course of stairs sloping in all round, then a slab cap; on 5 × 5, a second set-off at two-thirds height (a stair course) to 3 × 3.
- **Lit:** bands (edge tone) and grille slots (dim), when its district is fed.
- **States:** intact/worn (one chip per outward face); damaged: **snapped** on a stepped diagonal at 55–75% of its height, scarred (cracked) plates along the break, the top lying in the street toward the heart, heat tint above the outward grilles; ruined: a stub 3–7 high (as the outskirts' stubs), a snapped insulator where one was.

```
On old foundations, 5 wide, 26 high (intact):     Snapped (damaged), side view:

y26    .-.      cap                                 y17   |..
y25   .^^^.     chamfer, stairs sloping in          y16   |p..
y24   |ppp|                                         y15   |pp.        the break: a stepped
y21   |===|     band (lit)                          y14   |===|       diagonal, cracked plates
y20   |pgp|                                         ..
y19   |pgp|     grille run, 1 x 3                   y0    |pgp|
y18   |pgp|
 ..                                                 street:  p p|p=pg|p   the fallen top, toward
y7    |===|                                                              the heart
y1    |ppp|
y0   xBBBBBx    the old plinth ledge, clamped; x: the bite
     (| = corner pillar; a 5-wide face is pillar, three plates, pillar)
```

### 6. Alcove wall: the honeycomb (Quarters, lot C)

- **Footprint** 14–20 long × 5–6 deep (back wall, alcove, 2-deep walkway, parapet). **Height:** 3–5 tiers at 4 blocks a tier (alcove 3, band 1) = 12–20, plus a chamfered top.
- **Silhouette:** a dark wall of stacked niches, staggered like a honeycomb, with a zig-zag ramp across its face and a lift tower at one end.
- **Honeycomb:** each tier alternates Charging Alcove and Void-Alloy Pillar (alcove, pillar, alcove...), and each tier is offset by one block from the one below, so niches sit over piers: the honeycomb the world brief names (the current build is one unbroken strip).
- **Palette:** alcoves 45%, pillars 20%, plate (back wall, deck fronts) 20%, slabs (decks, ramp) 10%, walls (parapets) and bands 5%.
- **Relief:** each tier's walkway is a Void-Alloy Slab deck 2 deep with a corbel of upside-down void-alloy stairs under its front edge; parapets of Void-Alloy Wall (replacing Iron Bars); a Band at each tier's head; the top chamfered.
- **Ramps (they used ramps):** a 2-wide **Void-Alloy Slab ramp** (a half block per block) up the face in switchbacks, one flight a tier, parapet on its open side: the zig-zag is the wall's strongest diagonal. The barricade across the main ramp stays (world brief).
- **Lift:** the existing Lift at the wall's end (D40, D62), its 2 × 2 shaft framed by four Void-Alloy Pillars, open to each walkway, call plates at each tier.
- **Stood on:** its first tier sits on a makers' plinth course (the old back-garden wall line of the terraces), clamped.
- **Trinkets and vines (D72):** the alcove wall is the city's main host for both: trinkets in 1 alcove in 5 (kind at random), vines in about 1 in 4, more on the upper tiers and the outer end (where nobody came back to clear them).
- **Lit:** alcove contacts (existing), bands (edge), nothing else.
- **States:** worn (the barricade, a chip per outward face); damaged: **the split alcove tower** (world brief): a separate 6-wide, 3-tier honeycomb tower split down the middle, a 2-wide gap from top to bottom on a stepped slant, the two halves' top corners broken away in a V, rubble (plates, slabs, a fallen alcove lying on its back) in the gap; ruined: catalogue only.

```
Face, 15 long, 3 tiers (intact), ramp from left:

y12   ^^^^^^^^^^^^^^^    chamfer
y11   ===============    band (lit)
y8-10 |A|A|A|A|A|A|A|    tier 3: alcove (A) / pillar
y7    -------- ccccc     deck (slab) with corbels; ramp flight 3
y4-6  A|A|A|A|A|A|A|A    tier 2: offset by one
y3    ---------------
y0-2  |A|A|A|A|A|A|A|    tier 1
     xBBBBBBBBBBBBBBBx   old plinth course, clamped
      //-/-/ switchback ramp, 2 wide, half a block per block, Void-Alloy Wall parapet
```

### 7. Assembly gantry (Workshops, lot C)

- **Footprint** 24–26 × 5–7. **Height** 8–10.
- **Silhouette:** a long portal frame (two A-frame legs and a beam) over a row of five jigs, each body more complete than the last, with a hoist trolley stopped halfway along.
- **Construction (automatons' layer, choice 7):** legs of paired Void-Alloy Pillars, braced by Void-Alloy Wall cross-ties at third heights; the beam two horizontal pillars side by side with a Band course along their face (lit); chamfered stair caps over each leg. A **hoist trolley** (a 2 × 1 void-alloy box with a grille) on the beam, its chain (`minecraft:chain`) down to a lens held over clamp 5.
- **The jigs:** the five approved **Assembly Clamps** (D72), stages 1–5 in order under the beam, stage 5 in place of the `frozen` Worker. The gantry adds nothing to them: no extra brackets or uprights round the jigs.
- **Floor:** an assembly floor of Void-Alloy Plate laid over the makers' paving (its edge a slab step down; the old paving shows round it), a Seamed Plate groove from each jig's foot to the beam leg (lit).
- **States:** worn (two **bolt craters** in the floor, as built, now with scarred plates round them and soot fans); damaged: the **outer leg buckled**: the beam's outer end down to the ground on a stepped slope, its hoist slid to the bottom, clamp 1 (nearest) knocked over; ruined: catalogue only.

```
Elevation, 25 long (intact):

y9   ^^.....................^^       chamfer caps over the legs
y8   ||=====================||       beam: two pillars, band along the face (lit)
y7   |\.........T...........\|       T: hoist trolley; '\' wall cross-ties
y6   ||.........:...........||       chain
y4   |\.........o...........\|       o: lens held over clamp 5
y0   ||..1...2...3...4...5..||       Assembly Clamps, stages 1-5
    --pppppppppppppppppppppppp--     assembly floor over the old paving
```

### 8. Substation yard and its lattice mast, and the corner masts

The district's industrial set piece; the only place with warning colours (Industrial Battery lesson: real hardware, layered modules, one hazard band).
- **Footprint** 11 × 11 (u, v 19–29, as laid out). **Height:** the mast 10–12; everything else under 5.
- **Silhouette:** a fenced compound of equipment on pads, one lattice mast rising out of it with cables leaving its head.
- **Ground:** `minecraft:gravel` yard (real substations stand on gravel) with `create:polished_cut_deepslate` pads under each piece of equipment, slab-edged.
- **Fence:** a plinth wall of polished cut deepslate (1 high) with `create:andesite_bars` on it (2 high), andesite casing gate posts; the gate to street A open. Makers' work: the uprising took over the makers' yard.
- **Substation:** on its pad, facing the gate, behind the **one hazard band** (as built: yellow and black terracotta, the only warning colours in the city).
- **Transformer (real hardware):** a void-alloy tank (plate, 3 long × 2 high × 2 deep) on its pad; **radiator banks** of Slotted Grille down both long sides (lit dim when fed); three **Derelict Insulators** (top mount) on its lid as bushings, with short cable to the tray; an **arc scorch**: a soot fan on the yard wall behind the bushings, heat tint on the lid (visual language: "a scorch fan above arcing contacts").
- **Cable tray:** Void-Alloy Slabs (top half) on Void-Alloy Wall posts every 3, at head height, carrying Derelict Cable from the Substation to the transformer and to the mast's foot.
- **Lattice mast:** choice 11. Recommended: four `create:metal_girder` legs on a 3 × 3 square (corners), horizontal girder rings every 3 blocks, 10–12 high, a cross-arm (horizontal girder, 5 long) at the top carrying the top-mounted insulator(s) (D64's collar bites a girder); an **anti-climb guard** of andesite bars round the legs at y 2–3; each leg on a deepslate pad. The approved **mast riser** (D72: Derelict Cable `girder` mount, foot, run, top, pothead and jumper) climbs one leg's outer face. The yard's tray span is kept (D72).
- **Control hut:** a makers' shed at its smallest (4 × 3 to 5 × 4, walls 3, slab pitch), its window toward the Substation, a Broken Press inside (as built), cable threaded in through the window, a plaque by its door.
- **States:** the yard is near the heart: intact or worn only (one fence panel down, the arc scorch). Lit: transformer radiators (dim), bushings' rings, the riser and spans with the district.

**Corner masts (D72, world brief "rising up lattice masts at the corners"):** each district's street cables cross at its crossroads (streets A and B meet at u, v 30–34). The corner mast stands on the **crossroads' outer corner, at u 35, v 35** (the pavement corner of lot C, clear of the carriageway, the lamps and every set piece's footprint). It's the same lattice mast as the yard's (same build, 10–12 high, 3 × 3), on a deepslate pad, its riser climbing the leg that faces the crossroads, fed by the street cables (both streets' cable lines turn into its foot). At its top, one insulator; where its span goes is choice 11 (recommended: a span to the sky tower's lower insulator, so the corner joins the web). So each district has two masts: the yard's (the sky-cable chain) and the corner's (the streets').

```
Yard plan, 11 x 11 (u 19-29 across, v 19-29 down; the gate at v 29):

     19         29
v19  =#=#=#=#=#=     = fence: deepslate plinth + andesite bars
     #.ddd...MMM#    M: lattice mast (3 x 3 legs at corners), riser on a leg
     #.dSd...MMM#    S: Substation on its pad (d), hazard band (H) in front
     #.ddd...MMM#
v23  #HHHHHHHHH.#    the one hazard band
     #.....tTTTt#    T: transformer tank, t: grille radiators, bushings on top
     #.....tTTTt#
     #..........#    ~ cable tray on posts (head height) from S to T and M
     #......hhhh#    h: control hut
     #......hhhh#
v29  ==#..g..#===    g: gate to street A
```

### 9. The heart drum and the hall's exterior

- **Footprint:** the octagon round the 31 × 31 hall (flats at 16–17, corners cut, as built; about 35 across). **Height:** 14 to the parapet; the copper dome ribs rising to about 21 at the spire collar.
- **Silhouette:** a heavy buttressed rotunda with bare copper ribs arching in to the spire, like the ribs of a lantern with its glass gone.
- **Palette:** stained End brick 60% (choice 1), deepslate pillars and dressings 20%, deepslate plinth 10%, windows and copper 10%.
- **Relief:** two-course plinth with a stair water table; **eight buttresses**, one at each corner of the octagon, 2 wide and 2 proud, with stair set-offs at y 5 and y 10 (they carry the dome ribs: each rib lands on a buttress top, so the structure explains itself); on each flat, three tall makers' windows (2 × 4, round-headed with a block arch of stairs) in **recessed panels** 1 deep in the 2-thick wall; a lintel band at y 9; a corbel table at y 12; a parapet of stained End brick wall blocks at y 13.
- **Dome:** the eight ribs (weathered cut copper, 1 wide × 2 deep, patina from age) from each buttress top to the spire collar; three snapped short (as built). At the dome's foot, between ribs, 1–2 rows of **weathered copper shingles** still in place: the rest of the dome's skin is gone because the uprising stripped it to draw their cable (Derelict Cable is "copper drawn from the makers' pipes").
- **The uprising on it:** the three heart doors are **lined cuts** with projecting **portals** (a void-alloy frame 1 proud of the drum: pillar jambs, a band head, a stair chamfer above, broken-chain sigil on the door itself); the spire's collar clamps the ribs' heads (Clamp Brackets, saddle). Inside, void-alloy pilasters clamped to the hall wall carry the wall lamps and frame the vents.
- **Plaque:** one beside each heart door, on the first brick past the portal, 1.5 up (choice 13).
- **State:** intact (the heart is "nearly whole"). The parapet is gapped only where a snapped rib fell and broke it (three places), never at random. Soot none; dust on the water table. The frozen shift lives inside.

```
One flat between two buttresses (intact):

y13    PP:W:W:W:W:W:W:W:PP     parapet (stained End brick wall blocks)
y12    PPcWcWcWcWcWcWcWcPP     corbel table
y11    PP.LLLLLLLLLLLLL.PP
y9     PPLLLLLLLLLLLLLLLPP     lintel band
y8     PP|^^|..|^^|..|^^|PP    window heads: stair arches
y4-7   PP|GG|..|GG|..|GG|PP    windows 2 x 4 in panels recessed 1
y2     PPWWWWWWWWWWWWWWWPP
y1     BBBBBBBBBBBBBBBBBBB     water table (stairs) over two plinth courses
y0     BBBBBBBBBBBBBBBBBBB
       PP = buttress, 2 wide, 2 proud, set-offs at y5 and y10
```

### 10. The spire

- **Footprint** 5 × 5 to y 40, 3 × 3 to the crown at 58 (as built). **Height** about 64 with the needle.
- **Silhouette:** the needle: a dark shaft with corner seams, a stepped waist at 40, four crown arms, and a thin tip.
- **Relief:** a **collar** where it rises out of the dome (a ring of Void-Alloy Band and plate one block wider than the shaft, clamped to the rib heads); Seamed Plate corners (they climb, approved); faces of plate with a **grille run** every 6 blocks on each face (in place of today's air slots) and a **Band** ring between runs; at y 40 a stair course sloping in to the 3 × 3; the crown: a seamed cap and the four arms (horizontal pillars, as built), each braced at its root by two upside-down void-alloy stairs (a strut), a hanging insulator under each tip; above the cap the **needle tip**: Void-Alloy Wall posts stacked 4–6 high (thinner than a block, so the top reads sharp from the next island).
- **Lit:** corner seams climb (approved); bands light as the climb passes them; grilles dim; the crown's insulators with their districts.
- **State:** intact. No marks.

### 11. Vault house

- **Footprint** 9 × 9 (as laid out) on a makers' plinth 11 × 11. **Height** 6–7 with the chamfer.
- **Silhouette:** a squat, deep strongroom, chamfered all round at the top, its door in a deep projecting portal. "A safe that grew a doorway."
- **Palette:** plate 60%, pillars 15%, band 8%, grille 5%, stairs and slabs 12%; the old plinth in the district's plinth stone.
- **Relief:** pillar corners; a Band at y 4 (lit); one grille vent high on each side wall (y 4–5... under the band at y 3, 1 × 2); the **portal**: the Sliding Door (2 × 3) set back in a frame projecting 1 from the front (pillar jambs, band head, stair chamfer), so the door sits 2 deep in shadow; the roof's edge a stair chamfer sloping in, a slab field inside it; the wall lamp over the portal and the cable on clamps at head height (as built).
- **Stood on:** the old plinth ledge shows all round (1 wide), clamped by a saddle Clamp Bracket at each corner and the middle of each side.
- **Inside:** the vault in the back wall, alcoves on the side walls (trinkets 1 in 5, D72), the slumped Worker.
- **States:** worn: one chip per outward face, heat tint above the outward vent; damaged (if a vault house lands in the outer ring): a strike on the outward side wall (cracked plates, heat tint), the portal untouched (it held).

```
Front (intact), 9 wide on the 11-wide old plinth (the grille vents are on the side walls, not shown):

y6     .^^^^^^^.       roof chamfer, stairs sloping in
y5     |ppppppp|
y4     |=======|       band (lit), full width
y3     |pv^^^vp|       portal head: chamfer stairs over a band, the frame 1 proud
y0-2   |pv[D]vp|       Sliding Door 2 x 3 ([D]), set 2 deep between pillar jambs (v)
     xBBBBBBBBBBBx     old plinth ledge, clamped at the corners and mid-sides; x: the bite
```

### 12. Sky tower

- **Footprint** 5 × 5 (choice 12; today 3 × 3) at u 11–15, v 22–26, on an old plinth. **Height** 24–28 to the arm.
- **Silhouette:** a tall slotted tower with a **T-head**: a cross-arm (two horizontal pillars) at the top reaching 2 blocks toward the heart and 1 back, the sky-cable's wall insulator under its heart-side end. It's the one tooth in each district that holds its cable, so it must read as a cable tower.
- **Build:** the slotted tower (type 5) on old foundations; bands every 7; the lower insulator with its snapped span (as built) on a clamp on the outward face.
- **Lit:** bands, grilles, its insulators' rings with the district.
- **State:** intact or worn: its top never snapped (it holds a live span). Chips on outward faces.

### 13. Outskirts siege works (the clockwork resistance)

- **Footprint:** the siege ring at 84 (30 on outskirts-only sites), as built, in **segments** 5–9 long with gaps. **Height** 2 (cover), 3 at strongpoints.
- **Silhouette:** from the air, a **zig-zag** (a line of short straight segments turning by a stepped 45° at each joint, salients pointing at the city): the war's jagged diagonal drawn round the city.
- **Palette (makers' materials, never lit, no void-alloy):** stacked `create:andesite_casing` 50%, walnut (`minecraft:dark_oak_planks`, `dark_oak_fence` posts every 3 driven in as shoring) 25%, rubble fill of `minecraft:cobbled_deepslate` and scorched End Stone 20%, brass 5% (only on the siege winch).
- **Relief:** the cover wall 2 high faces the city; behind it (outside) a 1-block firing step of walnut slabs; at each salient a strongpoint 3 high with a gap at y 2 (a loophole); posts every 3 standing 1 above the wall.
- **The winding key mark:** scratched into the cover from outside (choice 9: a Key-Marked Stone block), one per segment, on the outer face.
- **The resistance:** their shells behind cover facing in (as built); the siege winch (as built, its frame in walnut and casing).
- **War marks (with reasons):** soot fans and bolt craters on the **city side** of the cover (the uprising fired out at them); segments breached where a strike landed (a gap with a rubble cone falling outward); casing blocks knocked loose and lying behind the line.
- **The uprising's barricades** at the street ends are their counterpart (welded scrap across the streets, scorched on the outer face); they're not in this kit's list and keep their current build unless the user wants them in (choice 14).

```
Plan, one salient (the city is up):

          #x#
         #...#          '#' cover wall (casing + walnut, posts every 3)
        #.....#         a stepped 45-degree joint at each turn
  ######.......######   x: strongpoint with loophole
  ssssss  s  s  ssssss  s: walnut firing step behind the cover (outside)
     R     R      R     R: Resistance Shells behind cover, facing in
```

## Interesting Features

- **Plaques at a makers' eye height.** The only writing in the city sits at 1.5 blocks, under the gaze of bodies 2–3 blocks tall. Players who notice will feel how small the makers were.
- **Doors cut twice as tall.** Every shed the uprising used has its little door cut up to 2 × 3 and lined in void-alloy, with the broken bricks still at the edge.
- **The furnace and the launder.** The uprising poured their own bodies from the makers' blast furnace; the void-alloy channel clamped to its hoops tells you so.
- **A house with its front off.** In the Quarters, the outer terrace house's front lies in the street, showing two fireplaces on the party wall and the stair to an upper floor nobody uses.
- **The dome they skinned.** The heart's copper ribs are bare because the uprising stripped the dome for cable; a few rows of old shingles cling at the bottom.
- **The honeycomb at night.** A lit alcove wall is a staggered grid of small lights with a zig-zag ramp across it.
- **The saw blade and the teeth.** From the next island each district has its own outline: chimneys, saw-teeth, the platform canopy, the honeycomb; and the towers ring them, snapped on a slant toward the edge.

## Fun Score (1-10)

**9.** Every building type now carries one readable story beat (cut door, launder, front-less house, skinned dome, honeycomb, snapped tower) on top of real structure, so a street rewards a close look and the skyline reads at a distance; it holds back on colour and puts its richness into relief and cause, which is what the Cuckoo Clock and the Battery taught.

## Builder Notes

- **Order of work:** (1) the user's answers to the choices; (2) the new block set and (if agreed) the masonry set, previewed in Blockbench once the decor preview session is done; (3) a kit layer in code (plinth, pier run, window bay, corbel, eaves, slab-pitch roof, truss, lined cut, clamp, band, chamfer, grille run, rubble cone, soot fan, strike) so every building is built from the same pieces; (4) `/wastes buildcatalogue`; (5) the screenshots; (6) after approval, the buildings replace the boxes in `CityLayout` without moving the lots.
- **Footprints stay inside the current lot rectangles.** Piers, ground steps and eaves add 1 block round a building; size each building so its eaves stay at least 1 clear of the street paving. The chimney grows from 2 × 2 to 3 × 3 (base 5 × 5), the sky tower from 3 × 3 to 5 × 5; both fit where they are.
- **Even spans** for makers' buildings so 2-wide doors, 2-wide ridges and gables centre together.
- **Stairs are not steps.** Void-alloy stairs are only chamfers, corbels, set-offs and struts; nothing in the uprising's layer is a walkable flight. Their slopes are slab ramps.
- **Things previews hide, check in game:** stair shapes at corners (outer and inner corners must form); `create:weathered_iron_window` connecting vertically only within a run (break it with a slab or band between runs); Create brackets need a shaft; Create fluid pipes connect to neighbours (keep the bustle pipe clear of other pipe-connecting blocks); andesite bars join with neighbours like Iron Bars (the applier already joins bars and panes after placing).
- **Tests over checks (memory):** add plan tests in the style of `WastesWorldGameTests`: no void-alloy block inside a makers' wall field (only clamps, lined cuts and on-top or through placings); every cracked/spalled or soot block within its band of a recorded cause; every scarred plate on an outward face and none in the heart; no excluded block (Iron Bars on new work, mossy blocks, vanilla End Stone Bricks, limestone, framed glass) in a building; every district has its corner mast at u, v 35.
- **Luminance check:** the catalogue shots go side by side with the material sheet; nothing in a wall field may read lighter than the paving.

## The Void-Alloy Building Block Set

Seven blocks. D71 asked for the first six; the seventh (Scarred Plate) is added because the rings of damage can't otherwise mark void-alloy without random missing blocks (choice 8). All drop themselves (D37), have proper item models, look right in a player's build, and use the existing textures from `clockwork-wastes-materials.gen.js` unless marked new. None is mirrored in its texture; the sheen of `va_tile` runs on across neighbours, so models use `uvlock` / world-aligned UVs where vanilla shapes allow.

| # | Block | Shape | Texture rule | Lights with its district? | Why the city needs it |
|---|---|---|---|---|---|
| 1 | **Void-Alloy Stairs** | Vanilla stairs | `va_tile` on every face (no facet frame: it joins plate walls as one cast surface) | No | Every chamfered top (towers, vault house, alcove wall, gantry legs), the spire's waist, corbels under decks and crown arms. The shape language's "chamfered tops" can't be built without it. Never a walking flight |
| 2 | **Void-Alloy Slab** | Vanilla slab (bottom, top, double) | `va_tile`; double = Plate | No | Ramps (they used ramps: half a block a block), walkway decks, cable trays, caps, the launder. Replaces the Stone Brick slab ramp (a layer slip) |
| 3 | **Void-Alloy Wall** | Vanilla wall (post, low and tall sides) | `va_tile` on sides; `va` (facet frame) on the post's top | No | Parapets on walkways, ramps and bridges (replacing makers' Iron Bars on uprising work), gantry cross-ties, tray posts, the launder's legs, the spire's needle tip (a post thinner than a block) |
| 4 | **Slotted Grille** | Full block, `axis` x or z. The two axis faces each carry **three tall slots** (2 px wide, 12 px tall, at x 3–4, 7–8, 11–12; ends chamfered by 1 px steps) cut right through (cutout); the slot reveals are inner faces; the other two faces plain | Field `va_tile`; slot reveals `VA.lo` with a `VA.hi` 1 px lip at the top of each slot (light catching the chamfer) | **Yes, dim tone:** a separate `shade:false` part on the slots' back edges, `#3a3552`, no edge or core tone (choice 8) | The uprising's window. Today's air "slots" read as holes; a grille reads as intended, tall-narrow-chamfered, and lit it turns towers into stacked dashes of light. Also vault-house vents, transformer radiators, the hoist trolley |
| 5 | **Void-Alloy Band** (the trim/cornice) | Full block. On all four side faces a raised band: the top 3 px and bottom 3 px are set back 1 px (two 1 px chamfer steps), leaving a 10 px band; one horizontal 1 px **seam** along the band's middle (y 7) across the full width, so bands join round corners into one ring | `va_tile` field and band; the chamfer steps in `va` edge tones (`VA.hi` on the upper step, `VA.lo` on the lower); seam `VA.seam` (`#2e2a40`) | **Yes, edge tone** (`seam_strip_lit`), the Seamed Plate's `lit` multipart pattern; no core tone (it's a secondary light) | Horizontal articulation for every void-alloy building: rings every 6–7 blocks on towers and the spire, heads of tiers, the gantry beam, the vault house's course. Seamed Plate's three-panel pattern repeats per block and reads busy as a band |
| 6 | **Clamp Bracket** | Not a full block; `mount` wall or top, `facing`. **Wall:** a strap 6 px wide × 16 tall × 2 deep against the face, two jaws (10 wide × 3 tall × 4 deep, at top and bottom) each with a 1 px lip turned back to the wall, a boss (4 × 4 × 3) in the middle as the load socket. **Top (saddle):** a 4 px top plate across the block with two jaws reaching 4 px down over the top of the block below (the model may extend under its own block; the collision box stays inside it) | `va` (small part: full facet frame); one `seam_strip_dark` line down the strap | **No, never:** it's structural, not electrical | The hero join: what "clamped onto old stonework" means in blocks. Holds insulators, cable, the launder, the spire collar, tower feet and plinth ledges. Rule: the stone under its jaws is the cracked/spalled variant |
| 7 | **Scarred Void-Alloy Plate** | Full block, `facing` (the scarred face) + `scar`: chipped, cracked, heat | `va_chipped` and `va_cracked` (on the sheet); **new** `va_heat`: heat tint `#3a2e24` with streaks `#4a3a2e` rising from the bottom edge, fading out by y 6 (visual language: soot is the same value as the metal, so fire shows as heat tint). The other faces `va_tile` | Cracked only: the seam-coloured pixels beside the crack glow **dim** when fed (the blocks brief's "a dim seam glow beneath"); chipped and heat never (choice 8) | Damage on the uprising's buildings by the rules (one mark per outward face, none at the heart) without random missing blocks |

Not needed (checked): a corner or chamfer block (the Pillar already has stepped chamfers), seamed stairs or slabs (the Band carries the light), a void-alloy door frame (pillars, band and stairs build the portals), a void-alloy railing pane (the Wall does it). Existing blocks used as they are: Void-Alloy Plate, Seamed Plate, Pillar (also laid horizontally as beams and arms), Charging Alcove (with D72's states), Derelict Insulator, Derelict Cable (with D72's girder mount), Brass Plaque, Assembly Clamp.

**Makers' masonry set (choice 1, not void-alloy):** Stained End Stone Bricks, Cracked Stained End Stone Bricks, Stained End Stone Brick Stairs, Slab and Wall, from the sheet's `endbrick` texture (mortar `#6c6750`, bricks `#827d5f` `#8d876a`) with a cracked variant painted to match. Never lit. **Key-Marked Stone (choice 9):** the resistance's winding key scratched into a cover stone, on one face.

All are previewed in Blockbench (a block-set sheet: each block alone, then in a short wall corner with plate and pillars, dark and lit) before they go in.

## The Catalogue: `/wastes buildcatalogue`

Builds every type beside the player on the test ground, each on its own pad of Scorched End Stone, footprints 10 blocks apart, in rows 40 apart running east, the camera side south. Nothing is bound to a district; a `lit` switch on the command lights every exhibit's glows (or binds the catalogue to a fake fed district) so dark and lit shots can be taken. Each exhibit gets fixed camera marks for its three shots so they can be retaken identically after changes (the ShotRunner).

| Row | Exhibits (left to right) |
|---|---|
| **1. Makers' sheds** | Shed in each district's masonry, intact (4); the Workshops shed worn, damaged, ruined (3); the yard control hut and the Rail Yard signal box (2) |
| **2. Makers' set pieces** | Furnace shed with chimney and blast furnace: worn, damaged (toppled chimney, scorch fan), ruined (3). Terrace of 4: intact, damaged (front-less end house), ruined (3). Long workshop: intact, damaged (collapsed teeth), ruined (3) |
| **3. The uprising** | Slotted tower 3 × 3 on old foundations: intact, damaged (snapped, its top in the street), ruined stub (3). Slotted tower 5 × 5 through a shed roof (1). Sky tower with its T-head and spans to a dummy mast (1). Vault house: worn, damaged (2). Alcove wall, 3 tiers with ramp, lift, trinkets and vines (1), and the split alcove tower (1). Assembly gantry: intact with its five clamps, damaged (buckled leg) (2) |
| **4. Yard and heart** | Substation yard with the lattice mast and riser (1); a corner mast at a mock crossroads with its street cables (1); a quarter of the heart drum (two flats, three buttresses, one heart portal, two ribs, one snapped) (1); the spire full height on a drum stub (1) |
| **5. Outskirts** | A siege-works salient with its strongpoint, shells, a breach and its scorch on the city side (1); a ruined shed and a ruined tower stub as they'd lie in the outskirts (2) |

**Screenshots to take (for the user before the walk):**
1. **Street level**, every exhibit: eye height 1.6, 8–12 blocks from the main facade, looking slightly up; dark, and lit where it has lights.
2. **Three-quarter**, every exhibit: 45° in plan, about 25° down, at 2.5 × its height; dark and lit.
3. **Close-ups** of the joins: a clamp's bite, a lined cut, the launder on the hoop, a plaque (straight and askew), the honeycomb with a trinket and vines.
4. **From the next island** (the full city, after the kit replaces the boxes): 250–400 blocks off, at the city's floor height, dark; then one, two and four districts lit (the world brief's skyline set). Plus one high shot (Elytra height, 120 up, 200 off) to check the zig-zag siege line and district outlines.
5. One **side-by-side** of a lit void-alloy tower beside an Electric Lights street lamp (starlight must stay muted next to amber).

## Choices Needing the User's Agreement

Each has a recommendation; nothing here is built until agreed.

1. **Makers' masonry block set.** Vanilla End Stone Bricks are lum 215, the city's brightest surface; the world brief's palette wants them "tinted down". (a) Add Stained End Stone Bricks + cracked, stairs, slab, wall (5 blocks, the sheet's `endbrick`); (b) keep vanilla End Stone Bricks; (c) use Create tuff for the drum and Quarters, no End brick. **Recommend (a).**
2. **District masonry:** Foundry scoria, Workshops tuff with andesite-casing piers, Rail Yard deepslate (engineering brick), Quarters and heart stained End brick, replacing Bricks / Andesite Casing / Stone Bricks. **Recommend as proposed** (vanilla Bricks are a saturated orange-red; cut andesite is over mid grey).
3. **Roofs:** (a) slate (deepslate tiles) with a weathered copper ridge roll and walnut barge; (b) exposed copper shingles going to patina; (c) walnut slabs as built. **Recommend (a)**: copper as a trim line, not whole roofs.
4. **Excluded blocks:** no limestone, calcite, framed glass, smooth stone, Brass Block surfaces, mossy blocks; Iron Bars replaced by Create andesite bars on makers' work and the Void-Alloy Wall on uprising work. **Recommend yes.**
5. **Who bricked the terraces' doors:** (a) the makers, barricading themselves in during the uprising (hasty cobbled-deepslate rubble, no coursing) before the resistance flew them out; (b) the uprising, sealing them neatly in the same brick (bends "never with"). **Recommend (a).**
6. **Small story details** (each yes or no): the shed doorways cut up to 2 × 3 and lined in void-alloy; the furnace's void-alloy launder to the casting floor; the dome's skin stripped for cable copper with a few shingle rows left; open roof hatches on two terraces (where the makers left on wings). **Recommend yes to all.**
7. **The gantry's material:** (a) void-alloy (the district table puts gantries in the automatons' layer); (b) Create girders as built. **Recommend (a).**
8. **New blocks beyond D71's list, and what lights:** add the Scarred Void-Alloy Plate (with a new `va_heat` texture); grille slots glow dim when fed; the Band's seam glows edge tone; a cracked scar glows dim; clamps, stairs, slabs and walls never light. **Recommend all as listed.**
9. **Key-Marked Stone** (the resistance's winding key scratched into cover stones, a world-brief motif with no block yet): (a) add it, one per siege segment; (b) leave the motif out. **Recommend (a)** (D39: no deferrals).
10. **Damage rings:** intact to 22, worn 23–45, damaged 46–70, ruined beyond; damage within a building starts at its outer end; fallen tops and chimneys lie toward the heart (away from the blow). **Recommend as proposed.**
11. **Masts:** (a) both the yard's and the new corner masts as 3 × 3 four-leg girder lattices with rings and a cross-arm (today the yard's is a single stack of five girders); (b) single girder poles. Corner mast at u, v 35 in each district, its top span to the sky tower's lower insulator (lit with the district, no bead), or (c) a snapped span instead. **Recommend (a), at u, v 35, with the span to the sky tower.**
12. **Sizes that grow:** the sky tower to 5 × 5 with a T-head; the chimneys to 3 × 3 on a 5 × 5 base; the vault house on an 11 × 11 old plinth. All inside their lots. **Recommend yes.**
13. **Plaques beyond D72's sheds and terraces:** also by the furnace shed's and long workshop's doors, the control hut's, the signal box's, and beside each heart door on the drum. **Recommend yes** (they're all makers' buildings).
14. **Not in D71's list:** the Rail Yard's platform canopy (the shed's roof on cast-iron posts) and signal box (a small shed), and the uprising's street-end barricades. (a) Bring the canopy and signal box into the kit, keep the barricades as built; (b) all three into the kit; (c) none. **Recommend (a).**
15. **Ramps replace the alcove wall's straight stone ramp** with void-alloy slab switchbacks (one flight a tier), keeping the barricade across the main ramp. **Recommend yes.**
