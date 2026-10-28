# Design Brief: Clockwork Wastes, Blocks and Items (group 2)

Group 2 of the Clockwork Wastes look ([spec](../specs/clockwork-wastes.md)). Everything here follows the look bible in [clockwork-wastes-world.md](clockwork-wastes-world.md): its three layers (makers' era, automatons' era, war), palette and motifs. Group 3 is the bots ([clockwork-wastes-bots.md](clockwork-wastes-bots.md)).

Assets, in build order:
1. **Substation**
2. **City Vault** (district and heart)
3. **Sliding Door** (the locked doors)
4. **Derelict Street Lamp**, **Derelict Wall Lamp**, **Derelict Cable** (surface and spans)
5. **Conduit Pylon**
6. **Derelict Star Reactor** (the heart's)
7. **Star Reactor** (crafted) and its upgrade
8. **Salvage blocks:** Broken Press, Broken Mixer, Broken Deployer, Broken Mechanical Arm, Broken Crushing Wheel; and **Worker Shells** (dead citizens: slumped, fallen, frozen mid-step)
9. **Decor set** (new, see the note at the end): Scorched End Stone, Void-Alloy Plate / Pillar / Seamed Plate, Charging Alcove, Maker Statue (standing, toppled), Scrap Heap
10. **Items:** Unknown Device, Clockwork Scrap

## Hard Requirements

From the spec:
- **Substation:** takes FE from anything; wakes its district at 1,000,000 FE; goggles show the charge, then fed / draw; comparator reads the charge; short of upkeep, its lamps flicker and go dark (D24, P7). When it wakes: lamps ripple on, doors and vault unlock "with a heavy clunk", lifts start (P8).
- **City Vault:** locked until its district wakes; unbreakable and blast-proof while locked (P21); the heart vault opens on the Custodian's death (D31, D35).
- **Locked doors** slide open into the wall when woken and never close on anyone (P49).
- **Derelict lamps and cable:** their own old blocks of unknown make; light like Electric Lights while fed; not dyeable, no brightness steps; break into scrap (D16, P25). Glow is **pale starlight** (user).
- **Conduit pylons:** 3 blocks tall with a glowing coil on top; the coil breaks after 6 hits, sparking and dimming as it weakens; broken, it slumps and stops arcing; cables arc from each pylon to the Custodian's back while it's shielded; one re-lights at the Custodian's overload (P32, P33, P36).
- **Derelict Star Reactor:** in the heart, under the spire; a Nether Star in it lights the whole city; can't be moved; wrench-salvaged it gives 2 Unknown Devices (P10, P13).
- **Star Reactor:** made from 3 Unknown Devices, a Brass Casing, a Precision Mechanism and 4 scrap; one Nether Star at a time; flat out 8,192 FE/t, 8 h a star; comparator, redstone pause, goggles; works on airships; keeps its burn when broken (D23, D29, P13, P14). **Upgrade:** an Unknown Device fitted shows as **a visible part on the reactor** (P44).
- **Salvage:** broken Create machines and bot shells, wrench for full salvage, pickaxe for scrap (D26, P16).
- **Items:** Unknown Device (rare intact tech), Clockwork Scrap (salvage material).

From the user and the house:
- Pale starlight glow; lore of automatons who overthrew their makers; bots with "eyes like ours" (2026-10-28).
- The war: "automatons that found electricity and then turned on their masters, and the clockwork resistance: bots that helped their creators flee and fought back, eventually wiping each other out" (2026-10-28). The two sides are set out in the world brief.
- Dark, muted; mid grey at most; glows muted; accents as trims; restraint on ordinary blocks, real relief hardware on big industrial ones; no decoration that duplicates a readout; round only for motors and engines; machines placeable in **all six directions** where it makes sense; ports at **face centres (8, 8)**; Create's copper palette; previewed in Blockbench first.
- Glows are their own `shade:false` parts so the renderer can light, dim, flicker and pulse them (the Electric Lights' pattern). Solid moving parts are Flywheel instances; glow, arcs and beams on the renderer.

## Asset Type

Blocks (about 20 models, many with state variants) and 2 items. 16 px = 1 block. Multi-block models (Substation 1 × 2, Sliding Door 2 × 3, Conduit Pylon 1 × 3, Derelict Star Reactor 3 × 3 × 3, Charging Alcove 1 × 3, Maker Statue 1 × 3) are a controller block plus filler blocks, like a door. Item icons for tall blocks are hand-drawn flat 16 × 16 sprites, lighter than the block textures (the Cuckoo Clock lesson).

## Fantasy

The furniture of a machine city: the cabinets that fed it, the vaults that kept its secrets, the lamps that lit its streets and the cores that powered it, all made by hands that weren't hands. Some of it is the makers' old Create kit, kept running; the rest is the automatons' own, in a dark alloy with light inside it. Players should want one of everything, and be a little uneasy about taking it.

## Hero Element

**Starlight seams in void-alloy.** Across every automaton-made block, light lives in hairline seams cut into dark, faceted metal: dark when the district is dead, a slow pale glow when it's fed, a bright pulse when it wakes. That one visual tells the player "this is theirs, and it's alive". Each block below has its own hero inside that language, named in its section.

## Theme

Palette and layers are in the world brief. In short: **void-alloy** `#16151c` `#1d1c25` `#25232f` (edges `#34304a`), **starlight** `#3a3552` / `#7f76a8` / `#b8b0d8`, and the makers' Create materials aged a step darker (andesite `#5e615d`–`#767873`, dark steel, mid brass `#7e6832`–`#a3873f`, Create copper with patina `#4f6b5e`). Rust and soot for wear.

Which layer each asset belongs to:

| Asset | Layer | Why |
|---|---|---|
| Substation | The uprising's core in a makers'-era cabinet | They gutted the makers' clockwork control cabinet and put electricity in it |
| City Vault, Sliding Door, Conduit Pylon, Derelict Star Reactor, Charging Alcove, Void-Alloy set | The uprising | Their own work |
| Derelict Street / Wall Lamp | Makers' cast-iron oil-lamp posts with the uprising's starlight heads | Retrofitted: the old lanterns pulled off, new light put in |
| Derelict Cable | Copper drawn from the makers' pipes, uprising insulators | The makers had no wires; the uprising made them from what was there |
| Star Reactor (crafted) | Player-made: Create Brass Casing round an automaton core | The recipe is literally both |
| Broken machines | Makers (Create) | "Broken Create machines" (user) |
| Worker Shells | The uprising | The dead citizens |
| Resistance Shells | The clockwork resistance | The other side's dead: makers' materials, never lit |
| Maker Statue | Makers, defaced by automatons | The story |

## Shape Language

Automaton blocks: **tall chamfered rectangles** (clipped corners, slots, upright panels) with **circles only for lenses, irises and cores**. Makers' parts stay **square** and Create-like. The war shows as **bends, snaps and leans**: lamp posts at an angle, a cabinet door hanging, a cog with teeth missing. Only the Star Reactors are round, because they are engines (house rule).

## Silhouette Description

- **Substation:** a tall makers'-era steel cabinet (1 × 2) with a void-alloy **charge column** set into its face and a big **breaker lever** on its side. "A grandfather clock that runs on lightning."
- **City Vault:** a squat, deep armoured block with a round **iris door** on its face. "A camera lens set in a safe."
- **Sliding Door:** two tall void-alloy leaves meeting at a chamfered seam, broken-chain sigil across the join. "A lift door built for giants."
- **Derelict Street Lamp:** a cast-iron post (often leaning) with a void-alloy **lens head** like a hooded eye on a crook. "A shepherd's crook with an eye."
- **Conduit Pylon:** a narrow 3-tall void-alloy obelisk with a **finned coil crown** and a starlight core in it. "A lightning rod made of knives."
- **Derelict Star Reactor:** a 3 × 3 × 3 cradle of four curved void-alloy arms round a floating star in smoky glass. "A hand holding a star."
- **Star Reactor:** a Create brass casing cube with a smoked-glass sphere set into it, the star turning inside. "A brass lantern holding a star."
- **Worker Shell:** a slumped tall thin body with a single dark lens, knees up. Reads at once as "dead robot".
- **Resistance Shell:** a stocky square body slumped forward, a round brass drum on its back with a key sticking out, two glass eyes. "A wind-up toy that ran down."
- **Inventory:** each at three-quarter view; tall blocks as flat hand-drawn icons.

## Primary Forms

### 1. Substation (1 × 2, faces the placer, horizontal facings)
The district's power heart, and the industrial set piece's focus.
- **Cabinet:** a makers'-era dark steel cabinet, 14 × 30 × 12 px, on a 1 px plinth. Bevelled panels (the process-box panel look: near-flat field, crisp bevels). Andesite inset panels on the sides. Rust runs from the top corners.
- **Charge column** (the hero): the cabinet's front centre holds a **void-alloy frame** 6 × 22 px, set into the steel as an obvious later addition (clamped on with four void-alloy brackets that bite into the steel). Inside, a **smoked glass tube** 3 × 19 px where **starlight rises** with the wake charge: 0–100% fills from the bottom in 10 steps (`charge` state 0–10). It's the only readout, so there are no dials (house rule: no duplicate readouts).
- **Breaker lever** (right side): a big knife switch (the uprising's, bolted onto the makers' cabinet) on a brass pivot boss, handle 2 × 9 px, **open** (up, 50°) while dormant. On waking it **slams shut** by itself (down, the "heavy clunk"), with a starlight flash across the contacts.
- **Lens-in-cog sigil** plate above the column (void-alloy, 6 × 6 px), its lens lit once woken.
- **Ports:** FE from the **back face centre** and the **bottom** (house rule: face centres). A small copper terminal block on the back.
- **Cable tray** on top: two insulators where the district's Derelict Cable leaves for the sky-web.
- **Woken and fed:** column full, seams on the frame lit (edge tone), sigil lens lit. **Woken and unfed:** column dim (dim tone), sigil lens dark, lever still shut. The look never goes back to "dormant".

### 2. City Vault (1 × 1, horizontal facings; heart variant in a bigger frame)
- **Body:** a deep void-alloy block, chamfered on all vertical edges (2 px), its face recessed 2 px.
- **Iris door** (the hero): a round door (12 px across) of **six overlapping void-alloy blades**, like a camera aperture, closed to a point. A thin starlight seam traces each blade edge. A **broken-chain sigil** is etched across the closed iris.
- **Locked:** seams dark, a single pale pixel at the iris centre pulses slowly (it's waiting).
- **Unlocking:** the seams light in a spiral from the centre out (0.5 s), the blades **rotate open** (1 s), the clunk; the open iris shows a dark interior (the vanilla-chest lesson: insides dark with depth) with a void-alloy shelf and loot glints.
- **Heart variant:** same block, framed in the structure by a tall chamfered surround with two maker statues (world brief), so the block itself stays one model.

### 3. Sliding Door (2 wide × 3 tall, controller bottom-left)
- **Two leaves**, each 16 × 48 × 4 px, void-alloy with tall chamfered panels (three per leaf, stepped 1 px), meeting at a **stepped seam** (the leaves interlock with a 2 px tooth, so it reads sealed).
- **Lock:** a broken-chain sigil spans the seam at chest height (for a bot: about 2.2 blocks up); its seam is dark when locked.
- **Opening:** the seam lights (edge tone) for 0.5 s, then the leaves slide sideways into the wall (each 16 px, 1 s, ease-out), leaving a clean chamfered frame. It never closes again (P49).
- **Frame:** a 2 px void-alloy jamb all round with a lit groove when open.

### 4. Derelict lamps and cable (makers' cast iron, automaton light)
- **Derelict Street Lamp:** a cast-iron post (our Cast Iron Lamp Post's family: fluted, square, `#26292d`–`#3a3f45`, but older: rust streaks, a chipped plinth, one flute broken). At the top, a **crook arm** curls forward (makers' scrollwork, 1 px iron), and from it hangs the automatons' **lens head**: a void-alloy hood (6 × 4 × 6 px, chamfered) over a **round lens** (4 px) pointing down at the street, like a hooded eye. **States:** `upright`, `leaning` (the whole post 12° off, toward the street), `snapped` (top block only: the crook bent down, lens cracked and never lighting). Stacks into a pole like the Lamp Posts (foot, pole, head).
- **Derelict Wall Lamp:** a makers' cast-iron bracket (scrolled, 1 px iron) holding the same hooded lens, pointing down and out. On any wall.
- **Lit:** the lens glows starlight (edge tone ring, core-tone centre 2 × 2 px); a faint halo (renderer). **Flicker** when power runs short, as Electric Lights.
- **Derelict Cable:** copper drawn from the makers' pipes (Create copper, mostly patina'd: `#4f6b5e` with copper showing on bends), held by **void-alloy insulators** (small chamfered stacks, 2 × 3 px). Laid on surfaces like our Power Lines (flat, clipped to the face), and in **spans** between masts and towers (sagging; some spans snapped and hanging). **Lit:** a fine starlight line runs inside a hairline gap along the cable's top; the wake **pulse** is a 3-px bright bead that runs along it (renderer).

### 5. Conduit Pylon (1 × 3)
- **Shaft:** a narrow void-alloy obelisk, 6 × 6 px at the foot tapering to 4 × 4 at 40 px, chamfered, with a seam up each corner (lit while live).
- **Coil crown** (the hero), top 8 px: **six blade-like fins** (2 × 8 × 1 px) fanning out round a **starlight core** (3 × 3 × 3 px) in a smoky glass cage. Arcs leap from the fins to the Custodian (renderer, a jagged starlight bolt, 2–3 segments).
- **Damage states** (from 6 hits; P33): `intact`, `cracked` (hits 2–3: one fin snapped off, a crack across the core glass, the core dimmer), `failing` (hits 4–5: two fins gone, the core flickering, sparks every second), `broken` (6: the crown slumps forward 20°, the core dark, the seams dark). `relit` at the overload: the broken crown pulls itself upright with a jolt and the core blazes (brief core-tone flash), then settles at `failing`'s look until broken again.
- **Foot:** a square void-alloy collar bolted into the floor groove, its groove lit when live.

### 6. Derelict Star Reactor (3 × 3 × 3, fixed)
- **Ring base:** a raised round platform (48 px across, 6 px tall) of void-alloy with conduit grooves running out to the floor.
- **Four cradle arms** (the hero): tall curved void-alloy arms (each 4 px thick, rising 34 px and curling inward at the top like fingers) holding the centre without touching it.
- **The star:** a smoky glass sphere (16 px, stepped to read round) held in the air between the fingertips. **Dormant:** dark glass, faint dim-tone sparkle deep inside. **Fuelled:** a Nether Star slowly turning inside (the item rendered by the renderer, as Create shows items), the glass glowing edge tone, the arms' inner seams lit, and a slow pulse (4 s) in the core tone.
- **Salvaged:** if wrench-salvaged (P13), the arms split and fall open, the sphere cracked and empty (a `salvaged` state; the structure keeps it as a ruin).

### 7. Star Reactor (1 × 1, any of six facings; crafted)
- **Body:** a **Create Brass Casing** cube (its frame and panels in our mid brass and dark steel, house brass not bright) with the four side panels each cut by a **round port** (8 px) of smoked glass.
- **Core** (the hero): inside, a void-alloy cage holding a **star sphere** (8 px), seen through all four ports. Fuelled: a Nether Star turns inside (renderer item, slow, 1 turn per 8 s), the glass glows edge tone, a slow pulse.
- **Burn readout:** a **ring of 8 starlight pips** round the top face's centre: one goes out for each hour burned (from 8 lit to 0). No other dials.
- **Ports:** FE out from the **back face centre** (a copper terminal); funnels and arms insert at the **top face** (a brass hatch).
- **Upgrade** (an Unknown Device fitted, P44): a **void-alloy orbit ring** (12 px across, 1 px thick) appears round the core, visible through the ports, turning slowly at a tilt (a gyroscope); the pips become 12 (top ring of 12). That's the visible part P44 asks for.
- **Unfuelled:** dark glass, pips dark, the cage visible as a dim silhouette.

### 8. Salvage blocks
**Broken machines** (makers' layer: Create machines, broken). Each is our own model in the shape of the Create machine, **broken in one readable way**, never copied from Create's files (reference Create's geometry through a parent model at runtime if we want its exact shape; check Create's LICENSE before reusing textures):
- **Broken Press:** the press head dropped and jammed crooked in its frame, one side panel missing to show a snapped shaft.
- **Broken Mixer:** the whisk bent 30° and wedged in a cracked basin.
- **Broken Deployer:** the arm extended and snapped at the elbow, its hand hanging by a cable.
- **Broken Mechanical Arm:** frozen mid-reach, the claw open over nothing, its base cracked and leaning.
- **Broken Crushing Wheel:** a cog-toothed wheel split in two, one half fallen on its side.
All in aged andesite casing, rust streaks, End dust settled on top faces (a pale `#878063` crust along top edges). Wrench salvage pops them apart (a short "come apart" burst of parts, particles).

**Worker Shells** (automatons' layer: the dead citizens): the city's common automaton, so this is where its look is set.
- **The Worker:** about 2 blocks tall standing (32 px), thin and long-limbed: a narrow void-alloy torso (6 × 10 × 4 px) on a spine, long arms with two-fingered clamps, thin digitigrade legs. The **head** is a tall chamfered housing (5 × 6 × 5 px) with **one big round lens** (4 px) set in a ring: the "eyes like ours" family face, a Trade Bot pod's cousin. Dead: the lens is dark glass with one dull catchlight pixel.
- **Shell poses** (each a block): `slumped` (sitting against a wall, knees up, head down; 1 × 1 footprint, 1 tall), `fallen` (face down, one arm reaching; 1 × 2 footprint), `frozen` (standing mid-step, heart only; 1 × 2 tall). Some wear a small makers'-era brass tag (a citizen number, unreadable).

**Resistance Shells** (the clockwork resistance: the other side's dead). Same salvage rules as Worker Shells (wrench for full salvage, pickaxe for scrap; a look variant, not new gameplay). This is where the resistance's look is set, and it must sit beside the Worker as its opposite.
- **The resistance bot:** about 1.5 blocks tall standing (24 px), stocky and square where the Worker is tall and thin. A **torso of andesite casing** (8 × 8 × 6 px) with riveted brass corner straps and field patches (a mismatched plate bolted on, walnut slats on one). Short thick arms with three-fingered brass hands; short legs.
- **Mainspring drum** (its hero): a round brass drum (6 px across, 3 px deep) on its back, with a **winding key** (butterfly bow, 4 × 3 px, brass) standing out of the drum's centre. Round, because it's the engine (house rule). On some, the drum is split and a **blued spring** has burst out in a loose coil.
- **Head:** square, Create-built (5 × 4 × 5 px), with **two round glass eyes** (2 px each) in brass rims, set wide: binocular, like Trade Bot and Helper Bot, so players read "one of ours". Warm dark glass, a single dull catchlight. **Never lit, in any state.**
- **The winding key** stamped on one shoulder plate.
- **Poses:** `slumped` (sat forward over its knees, head down, as if it ran down mid-thought; 1 × 1), `fallen` (on its side behind cover, one arm over its head; 1 × 2), `reaching` (Rail Yard only: on the carriage step, one hand reaching back down; 1 × 2 tall).
- **Wear:** dents, rust runs, soot on the side that faced the city. No void-alloy anywhere on it.

### 9. Decor set
- **Scorched End Stone:** End Stone's shape in the scorched palette, with two variants: `rust-stained` and `littered` (a few pixels of bent wire and cog teeth on top).
- **Void-Alloy Plate** (full block, faceted), **Void-Alloy Pillar** (chamfered, axis-placed), **Seamed Void-Alloy Plate** (a hairline seam pattern of tall chamfered panels; the seams light with their district, as the lamps do).
- **Charging Alcove** (1 × 3): an upright void-alloy cradle shaped round a standing Worker's outline, with a contact plate at the back (lit when its district is fed). Optional **trinket** state: a tiny object on a little ledge (cog, key, toy cart, faded flower).
- **Maker Statue** (1 × 3): a tall robed figure in End Stone brick and aged brass (makers' style), hands folded, the face chiselled flat with a **blank void-alloy plate** bolted over it. Variants: `standing`, `toppled` (lying in pieces, 3 × 1 footprint, head off).
- **Scrap Heap:** a low mound block (half height) of bent iron, chain and cog teeth: the outskirts' filler.

### 10. Items
- **Unknown Device** (hero item): a palm-sized **sealed void-alloy puck** (stepped octagon), a **broken-chain seam** of starlight running across its face, and a small lens on one edge. It looks like a part from something bigger, and nobody knows which way up it goes. Icon: three-quarter view, lighter than block textures, the seam the brightest thing on it, with a 1-px violet glint.
- **Clockwork Scrap:** a twisted bundle: a bent brass cog (two teeth missing), a curl of patina'd copper wire and one sliver of void-alloy. Icon reads as "junk you want", warm brass the dominant colour, so it stands apart from the Unknown Device in a full inventory.

## Secondary Forms

- **Brackets that bite:** wherever void-alloy is fitted to makers' work (Substation, lamp heads, cable insulators), it's held by void-alloy clamps that grip the old metal. These are the visible join between the two layers.
- **Wear by layer:** makers' steel rusts and dents; void-alloy doesn't rust but **chips** (a small bright-edged notch) and **cracks** (a dark line with a dim seam glow beneath).
- **Settled dust:** on every top face in the city, a faint End-dust crust along the top edges (as the Juggernaut's frost lesson: clusters at edges only, never speckle).

## Interesting Features

- **The Substation's lever slams itself shut** when the district wakes. Nobody touches it. It's the clunk.
- **The vault's iris** spirals open from its centre; the light gets there before the blades move.
- **Lamps hang like eyes.** Every lamp head is a hooded lens looking down at the street. When a district wakes, the lamps "open their eyes" in a ripple.
- **Pylons snap fin by fin.** Six hits, visibly counted down: a player can read how close they are.
- **The crafted Star Reactor is both layers in one block:** a Create brass casing (the player's world) holding the automatons' core. The upgrade ring turns it into a little gyroscope.
- **Trinkets in alcoves:** the city's quietest storytelling. One alcove in the Quarters holds a toy cart, the same shape as a makers' child's toy.

## Rig Requirements

Moving parts (Flywheel instances for solid parts; renderer for glows, arcs, beads):
- **Substation:** `lever` (pivot at the brass boss, right side, x 15, y 20, z 8; 50° open → 0° shut); `column_glow` (10 fill states, own part); `sigil_glow`; `seams_glow`.
- **City Vault:** `blade_1`–`blade_6` (each pivots at its outer hinge on the iris rim, 60° each, opening together); `seam_glow` (spiral reveal: 6 segments lit in order); `centre_pip`.
- **Sliding Door:** `leaf_left`, `leaf_right` (translate ±16 px on x); `seam_glow`, `jamb_glow`.
- **Lamps:** `lens_glow` (own `shade:false` part, renderer flicker and ripple delay); `snapped` is a separate model, no glow.
- **Derelict Cable:** `line_glow` (span and surface), `pulse` (renderer bead along the curve).
- **Conduit Pylon:** `crown` (pivot at the shaft top, front edge; 20° slump in `broken`, jolt back on `relit`); `fin_1`–`fin_6` (each hideable, snap with a small drop and tumble); `core_glow`; `seams_glow`; arc emitter point at the core centre.
- **Derelict Star Reactor:** `arm_1`–`arm_4` (salvaged: rotate out 35° from their base pivots); `sphere` (static); `star` (renderer item, slow turn); `core_glow` (4 s pulse).
- **Star Reactor:** `star` (renderer item, 1 turn per 8 s); `orbit_ring` (upgrade only, tilted 25°, turns once per 6 s); `core_glow`; `pip_1`–`pip_12` (own glow parts).
- **Broken machines:** none (static); the salvage burst is particles.

## Fun Score (1-10)

**9.** Every block has one thing that moves or lights in a way you'd show a friend: the lever that slams itself, the iris that spirals open, lamps that open like eyes, pylons counted down fin by fin, a star turning inside brass. Restraint holds: ordinary blocks are one material with one seam, and only the Substation yard gets industrial hardware.

## Builder Notes

- **Sizes:** Substation cabinet 14 × 30 × 12 px; vault iris 12 px; door leaves 16 × 48 × 4 px; lamp lens 4 px, hood 6 × 4 × 6; pylon 6 → 4 px shaft, crown 8 px; Derelict Star Reactor 48 px wide; Star Reactor 16 px casing with 8 px ports and an 8 px star; Worker standing 32 px, head 5 × 6 × 5 with a 4 px lens.
- **Round on the pixel grid:** lenses, ports and the iris must sit on the grid (the Wellspring lesson: a 2 × 2 hole in a 4 × 4 collar with corners off; circles drawn with hypot at 16 px blur into crosses). Hand-place the iris blades.
- **Void-alloy facets:** 1 px lighter top-left edge, darker bottom-right, faint violet gradient on big faces, **no mottle**.
- **Starlight discipline:** seams in the edge tone `#7f76a8`; the core tone `#b8b0d8` only at seam crossings, lens centres and the star's pulse peak. Render one lit street beside our Electric Lights to check it stays muted.
- **No duplicate readouts:** the Substation has only its column; the Star Reactor only its pips.
- **Glows are separate parts** (`shade:false`), so the renderer can light, pulse, flicker and ripple them; never bake a lit and an unlit texture.
- **Check in game what previews hide:** overlays closer than 0.25 px z-fight; no UV clamping outside 0–16; the glass parts are translucent multipart entries (the Battery's `battery_glass` pattern); run `zfight-check.js` and the export checks.
- **Decor drops (spec D37, user 2026-10-28):** the void-alloy blocks, Charging Alcove, Maker Statue, Scrap Heap and Scorched End Stone drop themselves as building blocks, so each needs a proper item model and must look good placed in a player's build, not only in ruins.
- **Screenshot for review:** the material sheet; each block front and three-quarter, dark and lit; the Substation waking (column fill, lever slam); the vault iris opening as a 5-frame strip; the pylon's five states side by side; both reactors fuelled; a Worker Shell in each pose; a Resistance Shell in each pose, beside a Worker, dark and with a lit lamp over both; the two item icons at 16 × 16 and 32 × 32.
