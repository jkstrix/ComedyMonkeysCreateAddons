# Design Brief: Clockwork Wastes Overgrowth (the Twisted Battlefields' first plants)

Status: **approved** (D92, 2026-11-02) with every recommendation, except three changes now written in: Shiftcaps grow **freely wherever growth is heavy**, there are **two tree species now** (Crookwood and the Shieldcrown, species 12), and the Charging Alcove's vines become **none / dead / living Knotvine** (living only where growth is heavy). A Blockbench preview comes next, and nothing is registered until the user approves that preview.

This set is the first flora of the third End biome, the **Twisted Battlefields**. In the Clockwork Wastes only that biome's edge has crept into the dead cities. Every species here gets reused in biome 3 later, so each section ends with a note on how it grows there. It follows [visual-language.md](../visual-language.md), the look bible in [clockwork-wastes-world.md](clockwork-wastes-world.md) and the rings in [clockwork-wastes-buildings.md](clockwork-wastes-buildings.md). The approved looks of every existing block and prop stay as they are.

## Hard Requirements

From the user (D91, 2026-11-02):
- Twisted Battlefields (biome 3): "thick, twisting plants of rich blues, glowing fungi that shift hue from blue to purple, trees that grow tall and are indigo with pale blue leaves and others, water plants, creatures that camouflage and fit the colour scheme".
- In the Wastes: "this will be just the vines and creepers and bushes and maybe one tree or two".
- The kinds the user chose: **hanging vines and creepers; moss and lichen; small plants in cracks; roots bursting through.** Bushes and "one tree or two" also come from the user's words above.
- Spread: "a mix of heavily overgrown and not, spreading seemingly at random, with most of the lighter patches being towards the heart".
- Treat these plants as the start of biome 3's flora. Whatever is designed now gets reused there.
- Brief first, then a Blockbench preview, before anything is registered (D91).

Lore (`the-three-peoples.md`, XII and XIV):
- "Out on the far islands, where the siege armies had camped ... Roots found the cracks in fallen war-frames. Trees grew up through shields and round the barrels of silent guns, and grew crooked doing it ... The Resistance's last camps became the Twisted Battlefields: a war buried under a garden that never quite grew straight."
- "On the islands where the siege armies camped, the Twisted Battlefields lie green and crooked over the wreckage of the war."

Standing rules (user, memories, visual language):
- Dark and muted. Light areas are mid grey at most (the andesite ceiling `#8a8c86`, about 139). Glows stay muted and never near-white, in small areas only.
- Accents are trims. Real-object detail is welcome when every part is a real thing, never noise (the Cuckoo Clock).
- One glow per meaning. Pale violet starlight belongs to the uprising, cyan to bots and electric blocks, frost blue to the Norse makers ("never anything in the Wastes"). None of them may be reused for plants.
- Wastes decor drops itself as a building block (spec D37).
- Every look gets previewed in Blockbench first, including tweaks. Any choice the user hasn't agreed is a question in this brief, not a decision. Nothing gets cut or deferred silently.
- No blocks or items whose id contains "create" (Carry On). Every carryable block needs a BlockItem.

## Asset Type

A **block set**: 19 plant blocks in 12 species, plus two tree features. 16 px = 1 block. Static block models with no moving parts and no block entities (the one exception would be an optional renderer touch on cable spans, question G7). Forms: multi-face covers like glow lichen, a hanging column like weeping vines, a carpet and a full block, cross/plant models with random XZ offset, small modelled props (roots, bush, fungi), and a log-and-leaves tree that worldgen places. Item icons are flat sprites, like vanilla plants. The exceptions are the moss block, the log, the leaves and the root pieces, which get 3D block items. Minecraft 1.21.1 / NeoForge, so `MultifaceBlock` (glow lichen's class) is there and the pale moss carpet's hanging sides are not: our drape is our own model.

## Fantasy

The war ended at the siege lines, and the garden started there. Seeds from the Resistance's last camps have crept in from the outskirts towards the dead heart, so that thick blue ropes of vine now hang from the makers' eaves, coiled ferns push up through the paving, and somewhere at the city's edge a crooked indigo tree has grown straight up through two war-frames that died holding each other.

## Hero Element

**The coil.** Every species grows in a twist or a spiral, as if the ground still remembers the springs the Resistance ran on. Rope vines wind two strands round each other. Ferns come up as tight fiddlehead coils, like little mainsprings. Rushes corkscrew. The bush is a ball of coiled thorny stems. The tree's trunk climbs in a corkscrew, and its bark grooves spiral. A player should be able to say "the plants are all twisted", and that twist is how biome 3's flora will be recognised too.

The set's single best moment is the **Crookwood in the grapple**: one tree per city growing up between the two locked war-frames in the outskirts, between the Resistance frame's hand and the uprising frame's cracked lens, with its pale blue crown over both. It's the lore's sentence, built. Its partner is the **Shieldcrown**: a braided trunk holding a flat round crown over a burnt-out roof, like a shield raised over the hole ("trees grew up through shields").

## Theme

### Where the growth comes from (the story rule)

The siege ring is the Resistance's old line, and it's where the Battlefields' edge reached the city. Growth is heaviest out there and thins towards the heart. That is why the lighter patches are mostly towards the heart. It isn't decoration on a dead city; it's the war's last camp still advancing, slowly.

### Plants grip the makers' work and slide off void-alloy (proposed rule, question C4)

The garden takes back what the makers built: End brick, slate, walnut, andesite casing, cast iron, the siege works. It can't root in the uprising's cast alloy. Moss and lichen never sit on void-alloy, and Knotvine climbs a void-alloy face only where it's scarred (a crack gives it a hold). Ropes may hang **past** alloy from makers' work above, never from it. The result: the uprising's towers and the spire stay clean dark silhouettes standing out of a greening makers' town, which keeps the three layers readable and adds a fourth (the garden) that clearly belongs to the makers' side of the story.

### Palette

All colours are baked into the textures, with **no biome tint**, so the exact palette holds in the Wastes and in biome 3. Brightness is Rec. 709 luma on sRGB, as the building kit measured it.

**How "rich blues" and "dark and muted" live together:** the blues are **saturated in hue but low in value**. The foliage tops out at about 85 brightness, darker than the aged andesite, and the darkest tones are near navy. Richness comes from saturation and from sitting next to the Wastes' warm brick and rust-brown fog, its complementary colour, not from brightness. The only pale things are lichen and the tree's leaves, and both stop at the andesite ceiling (about 139). The glow lives only in the fungi, in small areas, as the city's starlight does.

| Ramp | Shades (dark → highlight) | Hue | Brightness | Used for |
|---|---|---|---|---|
| **Battlefield leaf** | `#141c2e` `#1c2a45` `#24365a` `#2e4670`, lit edge `#3b5886` | about 217 | 28 → 85 | Knotvine and Twinerope leaves, Bramblecoil, Springcurl fronds |
| **Rope stem** | `#1a1e33` `#252b48` `#323a5e`, highlight `#424c75` | about 230 | 30 → 77 | Vine strands, bush stems, fungus stalks |
| **Crookwood bark** (indigo) | shadow `#1b1d3a`, body `#272c55`, ridge `#333a6b`, lit `#424a86` | about 233 | 30 → 76 | The tree's logs; Breakroot a step darker (`#181a30` `#22254a` `#2e3260`, lit `#3b4078`) |
| **Crookwood leaf** (pale blue) | `#3e4e68` `#55688a` `#6b7fa0`, top `#7a8dab` | about 217, desaturated | 77 → 139 | The tree's leaves: the palest plant in the set, never past the andesite ceiling |
| **Shieldcrown bark** (braided, ash blue) | `#1f232e` `#2b3140` `#384053`, lit strand `#4a5469` | about 222, greyed | 34 → 82 | The second tree's braided trunk and branches |
| **Shieldcrown leaf** (deep blue) | underside `#121a36`, `#18234a` `#213060` `#2a3d78`, rim `#3a5296`, veins `#2e4670` | about 225 | 25 → 82 | The flat crown: the darkest, richest blue in the set, against the Crookwood's pale |
| **Dusk moss** | `#141e2b` `#1e2b3d` `#26364c`, tips `#30445e` | about 215 | 29 → 66 | Moss block and carpet |
| **Scale lichen** | centre `#3a414c`, `#4a5462` `#5d6878` `#6f7b8b`, rim `#7e8898` | about 215, greyed | 64 → 135 | Lichen rosettes; bush thorns; the rope's seed pod |
| **Vigil Bell** | inside `#1a2244`, `#2c3f7a` `#3a5296`, rim `#4d66ad` | about 224 | 35 → 102 | The flower's petals |
| **Rush tip** | `#5a6b85` | about 217 | 105 | Corkscrew Rush's dry tips |
| **Shiftcap glow, blue** | dim `#2b4180`, edge `#4f6bb5`, core `#8095d8` | about 224 | 65 / 106 / 149 | Young caps' gills; blue spots |
| **Shiftcap glow, plum** | dim `#3f2848`, edge `#8a4fa0`, core `#b483c8` | about 283 | 47 / 97 / 146 | Old caps' rims; plum spots |
| **Shiftcap cap** (unlit body) | `#1a1a33` `#24264a`, rim shade `#2e2a52` | 235 → 250 | 26 → 45 | Cap tops (dark, so the glow sits underneath) |

### Against the approved colour meanings

| Approved colour | Hue | How the overgrowth stays clear of it |
|---|---|---|
| Pale violet starlight (`#3a3552` `#7f76a8` `#b8b0d8`), the uprising | about 250, greyed (saturation about 0.3) | **The fungus glow never uses the in-between violet.** Blue to purple passes right through starlight's hue, so the Shiftcap shift is drawn with **two ramps only**, blue (224) and plum (283), side by side and across ages, never blended through 240–265. Both ramps are more saturated (about 0.5) than starlight. Plant glow is **round spots and gills**; starlight is **straight hairline seams and lenses**. Plants glow whether or not a district is fed, so they can't be mistaken for power. |
| Cyan glow (`#4f8f99` `#6fb7c4`), bots and electric blocks | about 188 | No plant colour sits between 185 and 205. |
| Frost blue (`#4f8fae` `#7ab8d0`), Norse runes, "never anything in the Wastes" | about 199 | Same band, avoided. The foliage is ultramarine (215–224), darker and matte. |
| Void-alloy (`#16151c` … `#2a2838`) | about 248, almost grey | The indigo bark is clearly **blue** (233) and about twice as saturated, with organic spiral grooves instead of plates. It is never laid against void-alloy without a makers' block between them (the grip rule). |
| Ember (`#9c3a2a`), hostile | about 8 | Nowhere near. |
| Patina (`#4f6b5e` `#5b7a6c`), aged copper | about 150 | The moss stays blue (215), never green, so it doesn't read as patina or vanilla moss. |
| Dead vines (`#4f5236` dull olive, D72) | olive | The old, dead growth. The living blue growth is its opposite (question C3). |

**Proposed addition to `visual-language.md`** (only with the user's approval): *Battlefield blue (matte): growth from the Twisted Battlefields; never on made things. Shiftcap glow (blue `#4f6bb5`, plum `#8a4fa0`): living light, the only glow that's alive; always on; never a seam.*

**The rust-brown fog** (`#4a3a2e`) pulls far blues towards a dark brown-grey. That's wanted: the city reads brown from the next island and turns blue as you walk into it. Check it in game, because Blockbench can't show fog.

### What it sits beside

Stained End Stone Brick (about 125, warm), slate roofs and the patina ridge roll, void-alloy (left bare), the Derelict lamps and cables, the Brass Plaque, broken machines, Worker and Resistance Shells, the siege works, the Sprung Mainspring and the Fallen Rafter. The plants must read as **living and cool** against all of that warm, dead material.

## Shape Language

**Circles and spirals** dominate: coils, curls, corkscrews, rosettes, round caps and bells. The plants are organic and friendly, the "gentle" side of the lore. **Thick** means strands of 2 px or more, never 1 px wisps. This also follows the visual language's rule that thin lines read as perforation, and 1 px strands vanish under mipmapping. Against the uprising's tall chamfered rectangles and the makers' square boxes, the overgrowth is the only thing in the city that curves.

## Silhouette Description

- **A heavy patch from 20 blocks:** a makers' wall half-drowned in a dark blue sheet, with thick ropes hanging in curtains from the eaves, a lumpy bush at its foot and pale freckles of lichen above. "The building is wearing a coat."
- **A light patch:** the same wall nearly bare: pale lichen spots on the brick, one short rope at a corner, a coiled fern in the gutter. "Something's starting."
- **The Crookwood:** a corkscrew trunk with drooping pale crowns. "A twisted willow made of dusk." It's the one pale shape in the city's low skyline, so it must be visible from the outskirts' approach.
- **The Shieldcrown:** a tall straight braided stem forking near the top into a **flat round dark disc**. "A raised shield on a braided spear." Where the Crookwood droops and spirals, the Shieldcrown stands straight and spreads flat; the two can't be mistaken even as black shapes.
- **Twinerope from a snapped bridge:** ropes hanging off the makers' line shaft into the void, a fringe under the broken deck. That's a silhouette against the End sky.
- **Inventory:** each plant is its own recognisable flat sprite: the coil, the bell, the corkscrew, the cap.

## Primary Forms

The shapes that carry the look at street and skyline distance (each species is detailed in **The Species** below):
1. **Twinerope curtains** hanging from eaves, corbel tables, roof holes, hatches, line shafts and the crown of a tree: the vertical lines that make a district read "overgrown" from far away.
2. **Knotvine sheets** climbing walls from the base and creeping across roofs and floors: big dark blue areas.
3. **The trees**: one Crookwood and one Shieldcrown per city, the tallest living things.
4. **Breakroot arches** heaving the paving and bursting from wall bases.

## Secondary Forms

- **Dusk moss** cushions on up-faces (wall tops, sills, plinth steps, flat roofs, rubble) and as full moss ground in the outskirts.
- **Scale lichen** rosettes on walls, gables, statues and plinths: the light patches' main growth.
- **Bramblecoil** bushes in corners and wall feet, in roofless rooms and round broken machines.
- **Crack plants** (Springcurl, Corkscrew Rush, Vigil Bell) in gutters, paving seams and rubble.
- **Shiftcaps** (if the user wants them in the Wastes): clusters in dark corners, shelves on the sides of wrecks.

**Details:** seed pods on rope tips, thorns on the bramble, fiddleheads still coiled, a ring of dark spores at the centre of a lichen rosette, bark grooves that spiral, root hairs where a root dives back under the stone.

## Interesting Features

- **The springs grow back.** The Springcurl fern's coils are drawn exactly like the Wastes' spring steel (the dark gap between turns is real, never painted on), so a fern beside a burst Resistance mainspring reads as the same shape, alive.
- **Bells bowed like the lamps.** The Vigil Bell hangs its flower on a crook, exactly like the Derelict Street Lamp's hooded lens. They grow most at the feet of dead lamp posts, bowed the same way. When the district wakes, the lamp lights over its flower.
- **Ropes are routes.** Knotvine and Twinerope are climbable (question G5), so a heavy patch is a way up onto a roof, a vault house or an alcove tier. The overgrowth becomes the city's ladders.
- **The Crookwood in the grapple.** See Hero Element.
- **A shield over the hole.** In a damaged district, the Shieldcrown grows up inside a makers' shed whose roof burned through, and its flat crown spreads just above the broken roof line like a lid put back on. Standing inside, you look up at a dark blue ceiling with a lit rim where the roof used to be.
- **The heart is almost clean.** Only lichen, moss and a fern or two reach it. The frozen shift stays frozen, and the spire stays a clean needle (void-alloy rule).
- **Shiftcaps age from blue to plum.** Young caps glow blue at the gills, and old caps carry plum at the rim. A colony shows the whole shift at once, and in biome 3 that becomes a forest of it.
- **Brushing a Shiftcap releases spores** (question G9): a few slow blue and plum motes drift up and fade, living specks among the dead brass dust.
- **Old vines and new** (question C3): the Quarters' dead olive vines (D72) were the city's own garden, dead with it. Beside them, the living blue Knotvine is coming in from outside.

## Rig Requirements

None. Every model is static, and the glows are separate `shade:false` parts (house rule) so a renderer could pulse them later. Nothing is animated in Blockbench except an optional preview of the Shiftcap age states side by side.

## The Species

Each entry gives: form, size, palette, glow, distance read, where it grows in the Wastes and how biome 3 grows it.

### 1. Knotvine (the creeper)
- **Form:** multi-face cover (`MultifaceBlock`, as glow lichen): any of the six faces, so it climbs walls, creeps across floors and roofs and hangs under ceilings. Cards 0.8 px off the face (vanilla vine's offset), cutout.
- **Look:** two or three **thick rope stems** (2 px, drawn as a two-strand twist: a dark and a lit pixel pair alternating along the diagonal every 2 px) meandering diagonally across the cell, with **broad lobed leaves** (3–4 px, three lobes) in pairs at each node, and a tendril curl (3 px spiral) at one or two leaf tips. About 45–55% coverage, so the wall shows through a single block and adjacent cells read as a mat. The stems leave each edge at fixed points (x 4–5 and 11–12 on top and bottom, y 3–4 and 10–11 on the sides), so neighbouring cells join into continuous ropes. Three weighted texture variants.
- **Palette:** Battlefield leaf with rope-stem strands.
- **Glow:** none.
- **Distance:** a dark blue veil with diagonal ropy structure over warm brick: the heavy patches' main colour mass.
- **Biome 3:** the ground and trunk cover of the Battlefields.

### 2. Twinerope (the hanging vine)
- **Form:** a hanging column like weeping vines: `twinerope` (body) and `twinerope_tip`. It hangs under any sturdy down-face, is 1–6 blocks long in the Wastes, and is climbable.
- **Look, body:** a real **3D helix**: two strands, each 2 × 2 px in cross-section, winding round each other one full turn per block (four 2 × 2 × 4 px steps per strand, a quarter-turn apart, so a column of segments joins seamlessly). Small leaf cards (4 × 3 px, cross) sprout at two nodes per block. It reads thick and twisted from every angle, never as a flat card.
- **Look, tip:** the two strands unwind, one ending in a **curled hook** (a 5 px fiddle-curl card) and the other in a **seed pod** (a 2 × 3 × 2 px teardrop in scale-lichen `#5d6878`/`#6f7b8b`, the pod's one paler accent).
- **Palette:** rope stem, Battlefield leaf, lichen pod.
- **Glow:** none.
- **Distance:** dark vertical lines and curtains: the strongest sign of "overgrown" from far away.
- **Biome 3:** curtains from the indigo trees and from wrecks.

### 3. Dusk Moss and Dusk Moss Carpet
- **Form:** a full block (like the moss block) and a carpet (1 px). The carpet gets a **drape** on any open side: a ragged 3–4 px fringe hanging down that side face, 0.25 px off it (our own model, as 1.21.1 has no pale moss carpet), set by placement and neighbour updates like a wall connection.
- **Look:** cushions: rounded tufts 3–5 px with dark gaps between (`#141e2b`), a lit top-left on each tuft, and a few **spore stalks** (1 × 2 px, pale `#5d6878` tip) per block. Top and side textures differ (the side shows the tufts' profile and a root-dark base).
- **Palette:** Dusk moss.
- **Glow:** none.
- **Distance:** dark blue cushions softening every up-edge; the carpet's drape draws a dark line under sills and parapets.
- **Biome 3:** ground cover.

### 4. Scale Lichen
- **Form:** multi-face cover (`MultifaceBlock`), a card **0.25 px** off the face (the house z-fight minimum; glow lichen's 0.1 px is too close). Not climbable.
- **Look:** **rosettes** 3–7 px across: a dark spore centre (`#3a414c`), concentric rings and a scalloped pale rim, 2–4 per cell, some overlapping, about 30–40% coverage. Four texture variants and random rotations by face, so walls don't pattern.
- **Palette:** Scale lichen: the palest growth on walls.
- **Glow:** none.
- **Distance:** pale cool freckles on warm brick, the first thing on a wall and the light patches' signature. Its rosettes are clearly cooler than the brick even though they're close in brightness (about 120–135 against 125), and the dark centres make them read as rings.
- **Biome 3:** on rock and on wrecks.

### 5. Springcurl (crack fern)
- **Form:** cross/plant model with random XZ offset, placed on any sturdy up-face (question G4). A `size` state: `young` (7 px tall, three fiddleheads still coiled) and `grown` (12 px, three fronds unrolled into arcs with leaflets, two coils still tight).
- **Look:** the **fiddlehead coils** are the hero: a 4 × 4 px spiral of 2 px-wide frond with the dark gap between turns showing through (cutout), the same shape as the Wastes' spring steel coils. Unrolled fronds are arcs with paired 2 px leaflets.
- **Palette:** Battlefield leaf; coil highlight `#3b5886`; gaps `#141c2e`.
- **Glow:** none.
- **Distance:** small dark coils in the gutters; up close, "little springs".
- **Biome 3:** a tall form, 2 blocks.

### 6. Corkscrew Rush (crack tuft)
- **Form:** cross model with random XZ offset, 8–13 px tall.
- **Look:** six to eight stems, each 2 px wide, drawn as a **corkscrew** (kinking left and right every 2 px with a lit pixel on each outer bend, so each stem reads as a spiral), the tips fading to dry `#5a6b85`. It's a real plant (corkscrew rush), so it never reads as a grass recolour.
- **Palette:** rope stem shading into the rush tip.
- **Glow:** none.
- **Distance:** a wiry tuft. The commonest plant in the streets and rubble, the most "in the cracks".
- **Biome 3:** at the water's edge (with the water plants).

### 7. Vigil Bell (flower)
- **Form:** cross model with random XZ offset, 9–11 px tall.
- **Look:** a stem that rises and bows over in a **crook**, like the Derelict Street Lamp's arm, with one to three drooping **bells** (3 × 4 px, flared rim, dark inside) hanging from it. One leaf rosette at the foot.
- **Palette:** rope stem; Vigil Bell petals, rim `#4d66ad`.
- **Glow:** none.
- **Distance:** small bowed blue flowers, most of all at the feet of lamp posts.
- **Biome 3:** meadows of them.

### 8. Bramblecoil (bush)
- **Form:** a modelled bush with no full collision, in two sizes (`size` state): `small` (12 × 9 × 12 px) and `large` (16 × 15 × 16 px, its leaves overhanging by 1 px). Like the azalea: an outer leaf mass of cutout cubes plus a cross-card core showing **coiled thorny stems** inside.
- **Look:** a dense rounded mass of dark blue leaves, the coiled stems (2 px) visible where the leaves gap, with pale **thorns** (single `#6f7b8b` pixels) along the coils. Optional **sloe berries** (2 × 2 px, `#2a2d55` with a `#5a6390` bloom pixel), question G10.
- **Palette:** Battlefield leaf, rope stem, lichen thorns.
- **Glow:** none.
- **Distance:** lumpy dark mounds at wall feet and corners. They break up the city's straight lines.
- **Biome 3:** hedges of it round the old camps.

### 9. Shiftcap and Shiftcap Shelf (glowing fungi; grow freely wherever growth is heavy, D92)
- **Form:** `shiftcap`: a plant model of two to four mushrooms on **crooked stalks** (2 px, bent once or twice), caps 3–6 px across, conical to flat, set on a sturdy up-face. `shiftcap_shelf`: a bracket fungus on a side face (facing), two or three shelves 6–10 px wide and 2 px thick, stacked off-centre. Both have an `age` state, `young` / `mature` / `old`.
- **Look and glow:** cap tops dark (`#1a1a33`). The **glow is underneath and on the rim**, as separate `shade:false` parts 0.25 px under the cap. Young: gills in blue (edge tone, with the core tone at a single pixel per cap). Mature: blue gills plus two or three plum rim spots. Old: plum gills, blue only at the stalk top. No in-between violet ever (see the Theme table).
- **Light level:** low, about 3 (question G3), with an optional muted blue Colorful Lighting emitter.
- **Distance:** small, cool, low points of light in dark corners and on wrecks. They grow **freely in every heavy patch** (D92): many clusters and shelves, wherever the heavy growth is, in whichever ring. Each cap's glow stays small, so even a crowded corner reads as specks under dark caps, never a lit field.
- **Biome 3:** "glowing fungi that shift hue from blue to purple" at full scale: big caps, whole colonies, the biome's light.

### 10. Breakroot (roots bursting through)
Roots of the Crookwood family, in the darker root bark. Three modelled pieces, each turned four ways:
- **Breakroot Knuckle** (1 × 1): a root (5 px thick, twisted with spiral grooves) that **arches up out of the ground and dives back**, its top at 9–11 px, root hairs (1 px cards) at both dive points. It sits on a top face where the paving was pushed aside. Beside it the city lays **heaved paving**: the next paving block raised as a slab or stair, or cracked, from blocks that already exist.
- **Breakroot Run** (1 × 1, axis): a thick root lying along the ground (6 px), joining end to end with its neighbours, so roots can cross a street or follow a wall foot. A wall-climbing variant on a side face.
- **Breakroot Burst** (1 × 1, up to 22 px tall, facing up or sideways): **three roots splaying out of a crack**, one tall and curling at its tip, with two or three **broken stone chunks** (2–3 px cubes in a neutral scorched-stone texture) tossed aside. Placed on a wall's side, it's a root bursting out of the wall base. The bricks round it are cracked (a root counts as a cause, so this follows the "cracks only at a cause" rule).
- **Palette:** root bark (`#181a30` … `#3b4078`); End dust (`#878063`) on the top face along the upper edge, as on every other up-face in the city.
- **Glow:** none.
- **Distance:** dark humps and claws in the street and at wall feet: the city's floor being lifted.
- **Biome 3:** huge root arches round wrecks.

### 11. Crookwood (the tree)
- **Blocks:** `crookwood_log` (axis) and `crookwood_leaves`. They drop themselves; planks, sapling and other wood blocks wait for biome 3 (D92).
- **Log look:** indigo bark with **spiral grooves**: the groove runs diagonally across each face and lines up across stacked blocks, so a straight column still reads twisted; it does so even when the trunk's offsets carry it round. End grain in rings with a pale `#424a86` heart ring.
- **Leaves look:** pale blue, `cutout_mipped`: clustered lobes with dark gaps, drooping. The bottom row of a crown uses a `hanging` state whose texture fringes downwards.
- **Tree shape (worldgen feature) in the Wastes:** 9–14 blocks tall, a 1 × 1 trunk that **corkscrews** (steps one block round a centre every three or four blocks, so it climbs in a slow spiral), two or three short branches near the top, each ending in a **drooping crown** (a 5 × 5 to 7 × 7 mass, with leaves hanging two or three blocks down at the rim like a willow), and two or three Twinerope strands from the crown. Breakroots spread from its foot.
- **Palette:** Crookwood bark and Crookwood leaf.
- **Glow:** none.
- **Distance:** the only pale shape in the outskirts' low skyline: a twisted dark trunk under drooping pale blue.
- **Where in the city:** one per city, in the grapple (the hero).
- **Biome 3:** "trees that grow tall": a big 2 × 2 form 20–30 blocks tall, with other species alongside.

### 12. Shieldcrown (the second tree)
The "and others" in "trees that grow tall and are indigo with pale blue leaves and others". It is the Crookwood's opposite in every read: straight where that one twists round, flat where that one droops, dark-crowned where that one is pale.
- **Blocks:** `shieldcrown_log` (axis) and `shieldcrown_leaves`. They drop themselves; no planks or sapling until biome 3.
- **Log look:** a **braided** trunk: three strands (each about 5 px wide on the face) crossing over one another on a diagonal, with dark gaps between them, so a column reads as a rope of three stems twisted together (the coil motif as a braid). The strands meet themselves across stacked blocks. End grain shows three rings, one per strand.
- **Leaves look:** deep blue, `cutout_mipped`: small overlapping round leaves (2–3 px) with vein lines. The disc's **rim** blocks use a lit edge texture (`#3a5296` along the top edge), so the crown's outline reads from below; the underside is the darkest tone.
- **Tree shape (worldgen feature) in the Wastes:** 13–17 blocks tall. A **straight 1 × 1 braided stem**, flared at its foot by three or four Breakroot Runs. At about 70% of the height it **forks into four branches** that step out diagonally (two or three log blocks each) and carry a **flat round crown**: a disc 9–11 blocks across and 2 blocks thick, the top layer two blocks narrower so the edge is softly lensed, with a flat top and no hanging leaves. One or two Twinerope strands may hang from the rim.
- **Palette:** Shieldcrown bark and Shieldcrown leaf.
- **Glow:** none.
- **Distance:** a flat dark disc on a straight stem, the only horizontal shape over the city's roofs. "A raised shield."
- **Where in the city:** one per city, growing up through a makers' shed or long workshop whose roof burned through, in a damaged-ring district (the fire mark gives the reason for the hole), its crown spreading 1–3 blocks above the broken roof line. If no such building is in the damaged ring, in an open lot in the ruined ring.
- **Biome 3:** groves of them 25–35 tall with 2 × 2 and 3 × 3 braided trunks, their crowns overlapping into a dark canopy over the old camps.

**Not in the Wastes:** water plants (the Wastes has no open water) and creatures. Both are biome 3's.

## Placement in the Cities

### Density tiers

| Tier | What it holds |
|---|---|
| **Bare** | Nothing. Bareness also has reasons: scorch fans, craters, void-alloy, the heart's arena, door paths |
| **Light** | Lichen freckles on walls and plinths; a crack plant every few blocks of gutter; moss on some sills and wall tops; at most one short Twinerope (1–2) at a corner |
| **Medium** | Light plus: Knotvine climbing 2–4 blocks up from wall bases and down from eaves; Twinerope 1–3 long along eaves; a Bramblecoil at a wall foot; Vigil Bells under lamps; moss carpet along wall feet; the odd Breakroot Knuckle |
| **Heavy** | Knotvine over whole wall sections up to the eaves and across roofs; Twinerope curtains 3–6 long; moss carpet across the street with drapes; roots heaving the paving and bursting from wall bases; a Bramblecoil in every corner; **Shiftcaps freely** (clusters on the ground and in corners, shelves on walls and wrecks, D92); a tree if this is a tree site |

### The spread: patchy, seemingly random, lighter towards the heart

Two inputs decide the tier for each spot:
1. **Patch noise:** low-frequency noise (patches about 8–20 blocks across, seeded from the city plan's own stream so the districts' existing draws don't change) with a finer noise wobbling its edges. Patches can be bare, light, medium or heavy, so heavy and bare sit side by side anywhere.
2. **Ring cap and weight**, using the approved damage rings:

| Ring (building kit) | Most common | Allowed | Notes |
|---|---|---|---|
| **Intact**, the heart (to 22) | Bare, Light | Light at most (one Medium patch per city as an exception, question P1) | Lichen and moss on the drum, a fern or two at the doors. The spire and its crown stay clean. **None** in the Custodian arena, on the dais or in the conduit grooves |
| **Worn** (23–45) | Light | up to Medium | Knotvine starting up the outer ends of buildings |
| **Damaged** (46–70) | Medium | up to Heavy | Heavy patches common at buildings' outer ends; roof holes full of bramble and rope; Shiftcaps in every heavy patch; the Shieldcrown through a burnt-out roof |
| **Ruined**, outskirts and siege ring (71+) | Heavy | all | The source. Roots through the siege works' walnut, Knotvine over the andesite casing cover, moss on everything, Shiftcaps on the wrecks, the Crookwood in the grapple. Scorch fans and craters stay bare |
| **Beyond the siege ring** (the D91 natural edge) | Light, thinning | Light | A fringe of moss, rush and bramble 10–16 blocks onto the natural island, then nothing (question P2) |

**Growth follows ruin within a patch.** Within a building, growth starts at the outer end, just as damage does: rubble cones, holed roofs, cracked and spalled blocks and fallen bays get it first. So a building's overgrowth is never even, and it matches its damage.

### Surfaces

| Surface | What grows there |
|---|---|
| **Makers' walls** (End brick, tuff, deepslate, scoria, andesite casing) | Lichen first, then Knotvine climbing from the base or from a rubble cone; Breakroot Burst at the foot (with cracked bricks round it) in heavy patches |
| **Eaves, corbel tables, sills, hatches, roof holes** | Twinerope hanging from their undersides; moss carpet on sills and wall tops, its drape over the edge |
| **Roofs** (slate stairs, the patina ridge roll) | Knotvine creeping over gables and up the slopes from a hole; moss on flat parts (parapets, flat decks, gutters). No carpets on stairs (they float) |
| **Streets and paving** | Crack plants in the gutters and paving seams; moss carpet in heavy patches; Breakroot Knuckles and Runs with heaved paving |
| **Lamps** | Vigil Bells at the post's foot; Knotvine may climb a **leaning** or snapped post; nothing ever in front of a lens (one block clear below and in front of the head) |
| **Derelict cables (on walls)** | Knotvine grows either side but never in a cable's cell (one block per cell). Spans between insulators are renderer curves, not blocks: question G7 |
| **Broken machines and shells** | Shiftcap Shelves on their sides (heavy patches), moss on sturdy tops, a Bramblecoil at the foot, a Springcurl in the Broken Mixer's basin where the model leaves a sturdy face. Never over a wreck's salvage read |
| **Maker statues** | Knotvine and lichen on the robes and plinth; the **blank face plate stays visible** (it's void-alloy, so the grip rule holds it bare anyway) |
| **The spire and the heart drum** | The drum (makers' End brick) takes lichen and moss in Light only; the spire (void-alloy) stays clean |
| **Void-alloy towers, alcove walls, vault houses** | Bare (grip rule), except Knotvine on scarred plates in damaged rings and Twinerope hanging past them from makers' work above |
| **Bridges and line shafts** (D79) | Twinerope hanging from the makers' line shaft and its hangers under the bridges, **never from the alloy deck**; longest (up to 6) at the snapped ends, dangling into the void |
| **Siege works and the grapple** | Breakroots through the walnut cover and firing step; Knotvine over the andesite casing; moss across the firing step; the Crookwood growing up between the two war-frames, its roots round both |
| **Quarters' alcoves** | The `vines` state becomes none / dead / living (D92). Alcoves chosen for vines (D72's 1 in 4) get **living Knotvine** where the growth is heavy and D72's dead vines elsewhere; the living cards follow the dead ones' layout (down the back and cheek, over the front, one strand hanging) in Knotvine's palette and rope-twist stems |

**Never:** in front of Sliding Door leaves or their slide path, on vault fronts, on the Substation's front, column or lever, over insulators, in the train's doorway or on the reaching Resistance shell, on Key-Marked Stone's scratched key (it must stay readable), in the heart arena.

## Gameplay Questions

These are the user's choices. Each has a recommendation, and none of them has been decided.

- **G1 Drops (D37: decor drops itself).** (a) Thin plants (Knotvine, Twinerope, lichen, moss carpet, Springcurl, Corkscrew Rush) drop themselves only with shears or Silk Touch, as vanilla vines and ferns do, and nothing otherwise; the moss block, flowers, Bramblecoil, Shiftcaps, Breakroots and logs drop themselves by hand or with the right tool, and leaves need shears. (b) Everything drops itself with any tool. **Recommend (a):** it's vanilla's convention, and builders still get everything.
- **G2 Spreading and growing.** (a) Static: no random ticks, so the city never changes and costs nothing on a server. (b) Grows like vanilla: vines spread, ropes lengthen, Shiftcaps spread in the dark. (c) Bone meal only: Twinerope lengthens, moss spreads to nearby stone like vanilla moss, ferns and flowers multiply, and Shiftcaps age. **Recommend (c):** players can farm the plants for building, and the ruins stay as built.
- **G3 Light level** of the Shiftcaps: (a) 0, visual glow only; (b) 3 for clusters and 2 for shelves; (c) 6. Coloured light: a muted blue Colorful Lighting emitter, or none. **Recommend (b) with the blue emitter:** a small pool of cool light that never lights a street. Note: it slightly reduces mob spawning in those corners.
- **G4 What crack plants stand on.** (a) Any sturdy up-face (they grow in cracks); (b) a tag of End stone, scorched End Stone, moss and the city's stones. **Recommend (a):** simpler for builders and true to "plants in cracks".
- **G5 Climbable.** Knotvine on walls and Twinerope climbable like vanilla vines? **Recommend yes:** the overgrowth becomes routes up the city.
- **G6 Carry On.** **Recommend:** blacklist all thin plants (vines, ropes, lichen, carpets, crack plants, fungi), as you can't sensibly carry them; leave the moss block, logs, leaves, bush and Breakroot pieces carryable, each with its BlockItem.
- **G7 Overgrown sky-cable spans.** (a) Spans stay clean; (b) the renderer draws Knotvine wrapped round some spans with one to three Twinerope strands hanging off them, on spans in damaged and ruined rings and on snapped spans, never on lit beads' paths. **Recommend (a)** to keep the web readable when it lights, with (b) as a nice extra if wanted.
- **G8 Players getting the plants.** Beyond drops: flower pots for Springcurl, Vigil Bell, Corkscrew Rush and Shiftcaps; blue dye from the Vigil Bell; the composter for all of them; flammability as vanilla plants. **Recommend all four** (vanilla consistency).
- **G9 Spores.** Brushing through a Shiftcap releases a few slow blue and plum motes (a particle, no effect), and a rare ambient mote drifts off old caps. **Recommend yes** (cheap, and the only life that moves in the city).
- **G10 Bramblecoil.** (a) Berries (food) and thorns that slow and prick like a sweet berry bush; (b) slows only, no berries; (c) plain decor. **Recommend (b) for the Wastes**, leaving berries and creatures that eat them to biome 3's spec.
- **G11 Names.** Knotvine, Twinerope, Dusk Moss, Scale Lichen, Springcurl, Corkscrew Rush, Vigil Bell, Bramblecoil, Shiftcap, Breakroot, Crookwood, Shieldcrown (added with D92). None uses "creeper" (it clashes with the mob). **Recommend as listed**, open to the user's own.

## Fun Score (1-10)

**8.** The first pass scored 6: a good palette on plants that just sat there. The rework gave every species a reason to be looked at (the coils echo the Resistance's springs, the bells bow like the lamps, the ropes are climbable routes, the fungi age through the hue shift and puff spores), and gave the set one hero moment, the Crookwood rising out of the grapple. That turns "the city has plants now" into "the war's last camp is taking the city back". D92's changes add to it: the Shieldcrown's shield over a burnt roof is a second scene worth walking to, and free-growing Shiftcaps make the heavy patches glitter. It stays at 8, not 9, because it is still mostly still life, by design, next to a city whose drama is its lights.

## Builder Notes

**Preview in Blockbench first** (a generator in the house style, e.g. `design/models/clockwork-wastes-overgrowth.gen.js`, using the city's real block models and textures from the existing generators):
1. **Swatch board:** every species in a row on small plinths of Stained End Stone Brick, both sizes and states (Springcurl young and grown, Bramblecoil small and large, Shiftcap young, mature and old, lit and on a dark background), with palette chips for every ramp and the approved colours they must stay clear of (starlight, cyan, frost, void-alloy, patina, dead olive) beside them.
2. **Wall study:** a stretch of the makers' terrace front (plinth, piers, sills, corbel table, slate eaves, a hatch, a Brass Plaque, a Derelict Wall Lamp) **side by side at Light and Heavy**, with a void-alloy slotted tower section and a clamp beside it to show the grip rule.
3. **Street corner study:** paving and scorched gutter, an upright and a leaning street lamp (one lit), a Derelict Cable on the wall, a Broken Press and a Resistance Shell, a Breakroot heaving the paving, at Light and Heavy.
4. **The grapple vignette:** the Crookwood rising between the two locked war-frames (from the city's block layout), its crown over both.
4b. **The Shieldcrown vignette:** the second tree up through a burnt-out makers' shed, its disc just above the broken roof, with both trees' silhouettes side by side as black shapes.
5. **A lit check:** a Shiftcap colony next to a lit starlight lamp and a lit Seamed Plate, to prove the plum and blue don't read as starlight.
6. In game afterwards (ShotRunner): the same spots under the rust-brown fog at 10, 30 and 60 blocks, since Blockbench can't show fog.

**Must not lose:** strands 2 px or thicker; the twist on every species; values under the andesite ceiling; glow only on fungi, in small areas; no in-between violet; void-alloy left bare.

**Pitfalls:**
- **Cutout, never translucent.** Plants and lichen are `cutout`, leaves and moss `cutout_mipped`. No semi-transparent pixels in any texture (anti-aliased edges become hard jaggies or holes). Twinerope's helix is opaque cubes.
- **Mipmaps eat thin lines.** 1 px stems vanish at distance under `cutout_mipped`, which is another reason for 2 px strands. Check at 30 blocks.
- **No tint.** No `tintindex` anywhere, so the colours never shift with the biome's foliage colour (the Wastes has none).
- **Z-fighting:** vine cards at 0.8 px, lichen at 0.25 px (not glow lichen's 0.1), carpet drapes at 0.25 px off the side, glow parts 0.25 px under the caps. Two faces of a multi-face block meeting in a corner must not overlap: inset each card's edge where it meets the other. Run `zfight-check.js` on the export.
- **One block per cell.** Knotvine, lichen, a cable or a rope can't share a cell. The city has to choose per cell, and Knotvine must not try to fill a cable's cell.
- **Random offset** (crack plants, flowers) moves models up to 3 px in x and z, so keep their geometry within the middle 10 px or it pokes into walls.
- **Ambient occlusion off** on cross models (`ambientocclusion: false`), as vanilla plants, or they go black at the base.
- **Edge joins:** Knotvine stems must leave each edge at the fixed points, and the bark groove must meet itself across stacked logs. Check both on a 3 × 3 wall and a 4-high column.
- **Carpets on stairs and slabs** float. The city places moss carpet only on full or bottom-slab up-faces.
- **Sturdy faces:** fungi, crack plants and shelves need a sturdy face. Broken machine and shell models may not offer one, so place beside them instead.
- **Glow parts** are separate `shade:false` elements with `neoforge_data` full-bright, never a baked lit texture (house rule).
- **Item icons:** flat sprites drawn a touch lighter than the block textures (the Cuckoo Clock lesson), and the coil, bell, corkscrew and cap must each read at 16 × 16.

**Screenshots for review:** the swatch board front and three-quarter; the wall and street studies at Light and Heavy; the grapple vignette from the approach; the lit check; each crack plant at 1:1 and from 5 blocks; the item icons at 16 × 16 and 32 × 32.

## Decisions (D94, user, 2026-11-02): the preview approved

The Blockbench preview (`design/models/clockwork-wastes-overgrowth.bbmodel`, `design/texture-reviews/clockwork-wastes-overgrowth/`) was approved as built, with the build's changes:
- **Shiftcap caps** `#1d2238 #283050 #36406a` and **Breakroot** `#171b2e #212845 #2c3558 #3a4672` (the palette above read as void-alloy and as purple).
- The Shiftcaps' **lit lip** round each cap's lower edge and **lit spots** on top, so they show from above; the **stepped Shiftcap Shelf** with a dotted lit lip (a straight lit line read as a seam).
- The Crookwood's hanging state as **see-through strands** on crossed cards (a mid strand and an end), not full leaf blocks.
- **Bramblecoil** built from crossed sprig cards (thorny stems ending in coils) round a small leaf core.
- The **Breakroot shapes as built** (the knuckle a low arch, the run kinked, the burst on the ground) **plus a sideways burst** out of a wall's foot (built after the approval; `08b-swatch-burst-wall.png`).
- **Two-tone Twinerope** (a lit strand and a dark one).
- **One Crookwood** (the grapple) **and one Shieldcrown** (a burnt-out shed in the damaged ring) a city.
- **Shiftcaps also grow in medium patches**, so they reach the worn ring; none near the heart (the heart stays light).

## Decisions (D92, user, 2026-11-02)

Every recommendation below was approved, except: **C1** became (c), Shiftcaps grow freely wherever growth is heavy; **C2** became two tree species now (Crookwood and the Shieldcrown, species 12), one of each per city, logs and leaves only; **C3** is (c) as recommended (none / dead / living Knotvine, living only where growth is heavy). The list below is kept as asked, for the record.

- **C1 Fungi in the Wastes?** You named glowing fungi for the Battlefields, but the Wastes list is vines, creepers, bushes and a tree or two. (a) None in the Wastes; (b) a few, only in heavy patches in the damaged and ruined rings (a cluster or a shelf per patch, on wrecks and in dark corners), as a first glimpse of biome 3; (c) freely in every medium and heavy patch. **Recommend (b).**
- **C2 "One tree or two".** As species or as trees per city? (a) One species (Crookwood), one tree per city, in the grapple; (b) one species, two trees per city, the grapple plus one through a roofless shed in a damaged district; (c) two species now. Also whether players get its wood: (i) logs and leaves drop themselves only (decor, no planks or sapling until biome 3); (ii) a full wood set (planks, stairs, slabs, sapling) now. **Recommend (b) with (i):** one species reads as "the" tree of the coming biome, two per city keeps it special, and the wood set belongs with biome 3's trees.
- **C3 The Charging Alcove's dead vines (D72).** (a) Keep them as they are; the living growth stays out of the alcoves. (b) Replace them with living Knotvine. (c) Make the alcove's `vines` state none / dead / living: living in alcoves inside a heavy patch, dead elsewhere, so old dead growth and new living growth sit side by side. **Recommend (c),** or (a) if no new alcove art is wanted. The alcove's pressed-flower trinket stays as approved; it can't be a Vigil Bell, because the Battlefields only grew after the war.
- **C4 The grip rule** (plants grip makers' work and slide off void-alloy, except Knotvine on scarred plates). **Recommend yes.** It keeps the spire and towers as clean silhouettes and the layers readable. It also supersedes the building kit's rule 5 ("no moss, no vines on makers' walls") and its "nothing grows in the End" note for the overgrowth only; vanilla mossy blocks stay excluded.
- **C5 Colour.** The palette above (deep ultramarine kept dark, pale blue only on the tree's leaves and the lichen, both under the andesite ceiling, glow only on fungi in two ramps that skip starlight's violet), plus the two proposed rows for `visual-language.md`. **Recommend as proposed.**
- **C6 Colour shift.** How the Shiftcap "shifts": (a) by age (young blue, mature both, old plum) and within each cap; (b) a synced animated texture (every cap in the world changes in step, so it reads mechanical); (c) per-block timing through a renderer (costly across many plants). **Recommend (a).**
- **P1 The heart:** Light only, or Light with one Medium patch per city? **Recommend one Medium patch,** near a heart door (not the arena), so the heart isn't uniformly clean.
- **P2 Beyond the city:** (a) a 10–16 block fringe onto the natural island, then bare; (b) the city only; (c) also scattered over the Wastes biome's land away from cities. **Recommend (a).** The Wastes' land stays scorched, and the plants belong to the old siege lines.
- **P3 A prevailing side:** should growth lean towards one side of each city (towards where the Battlefields lie)? **Recommend no.** You asked for "seemingly at random", and the rings already give the inward creep.
- **Gameplay G1–G11** above.
