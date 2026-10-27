# Design Brief: Monster Cages (Andesite, Brass, Flooded Andesite, Flooded Brass, Netherite)

New blocks. What they do is in `design/specs/monster-cage-and-fish-tank.md` (Part 1). Family brief shared with `design/briefs/animal-pen.md` and `design/briefs/auto-harvester.md` (the sealed process box). Items at the end: Iron Jar, Netherite Jar, Monster Chow.

## Hard Requirements

From the user:
- "Must be a **sister block to the existing pens**, with **some flair**."
- Dark, muted Create palette; light areas mid-grey at most; accents as trims; at most one small hazard accent (house style, `art-style-dark-muted`, `block-design-restraint`).
- Previewed in Blockbench before anything goes into the mod (`preview-in-blockbench-first`).
- Each tier keeps its own trim metal (a product accent never replaces it).
- Brass is the warmer mid brass that landed on the generators (`#7e6832` `#8f7638` `#a3873f`), with brighter brass only on rivets and small fittings.

From the spec (the code contract to come; it reuses the Animal Pen's renderer):
- Five blocks: Andesite, Brass, Flooded Andesite, Flooded Brass, Netherite. A cage shows **real monsters** (up to 4 entities; the Netherite one a single Wither), scaled as the pens scale animals (`min(0.26, 0.4/size)`, tallest 6.4 px).
- Shaft into the **back** only (the shared `cobble_generator_shaft` stub, frozen; 4 × 4 socket centred at (8, 8)). Items in and out on **every** face; Flooded cages take water and the Netherite cage FE on every face **but the back**. Nothing may suggest only one face is a port.
- Floor surface at **y 2.5** (monster feet); wander area as the pens' (feet x 3.5–12.5, z 4.5–8.8); headroom to y 10–11.
- **Front tray** for drops (the family motif): x 3–13, z 1.2–3.8, bed at y 2.5, 0–3 items drawn by the shared `TrayItems` helper.
- **Feeding at the back**: Monster Chow shown like the pens' trough food (up to 3 items, at about z 12).
- Flooded cages: water drawn by the renderer to the tank's level, as its own **translucent** part; a **water sight glass on both sides** like the pens' milk glass.
- Flooded cages throw tridents **out of the front**; Netherite has a **klaxon** and a **containment field** that can be up, flickering (short of FE) or down, and an escape clock.
- "Netherite one should have a **blue glowing shield around it** when it's powered with FE", and "the shield when on should have some **animation** to it" (user, 2026-10-27).
- "Remove the **glass** from the front of the **non-water-filled** ones" (user, 2026-10-27): the dry cages and the Netherite cage are open behind their bars; only the Flooded cages have glass.
- "Bottles should be modelled off the **original jars** as a starting point" (user, 2026-10-27): the Iron and Netherite Jars are 3D item models built from the Capture Bottle's (`models/item/capture_bottle.json`).
- Full-cube shape; cutout for the shell. Everything recessed inside the block bounds.

## Asset Type

Blocks with renderer-drawn contents and a few moving parts. 16 × 16 × 16 px, `java_block`, window facing **north** as modelled. Two geometries (dry, Flooded) shared by the Andesite and Brass tiers through textures, plus one Netherite geometry. Part models: `monster_cage_hatch` (Flooded), `netherite_monster_cage_beacon`, `netherite_monster_cage_shield`.

## Family

The **sealed process box** again: the pens' frame, 12 × 12 front window in a 2 px frame, glass with a few cool grey glints, a front tray, a back socket for the stub, a sight glass on the sides where there's a liquid. A player who knows the pen should see a pen's sibling at once.

What makes it a **cage** and not a stall, in four moves:
1. **Bars** behind the glass.
2. A **stone cell** inside instead of boarding and straw.
3. A **pet bowl** of Monster Chow under a chute at the back instead of a hay trough.
4. A **grated top** instead of the walnut lid.

## Fantasy

A Create-built monster kennel: a barred, padlocked cell with zombies shuffling about behind the glass, a chute dropping Monster Chow into a big dented steel dog bowl, and their drops piling up in the tray in front. The Netherite one is a containment unit wrapped in a humming blue shield, with a klaxon on the roof and one very angry Wither.

## Hero Element

**The bars, with the monsters behind them.** A row of heavy iron bars and a latched crossbar across the window turns "box with a mob in it" into "cage" from 5 blocks away, while the monsters stay readable between the bars. The second read, close up, is the **dog bowl**: a big steel pet bowl full of chow, which tells the joke of the name.

## Theme

Create andesite and brass machine, dark end of the range, with a dungeon cell inside. It sits beside the Animal Pens and the String Tangler (which eats its string).

| Part | Andesite | Brass | Netherite |
|---|---|---|---|
| Frame, outside walls | dark steel `#2f343b` `#3c424a` `#4a5059`, andesite trim `#6b6e6a` `#7c7f79` | dark steel, mid brass trim `#7e6832` `#8f7638` `#a3873f` | netherite plate `#31292a` `#443a3b` `#4d4345`, edge `#5d5052`; obsidian inlay `#1b1424` `#2e1f3d` |
| Bars and crossbar | forged iron `#3c424a` `#5f656e`, one-pixel highlight `#6f757d` | same iron, brass caps on the crossbar ends | blackened netherite bars `#2a2324` `#443a3b` |
| Top | grated steel lid (a 1 px grid over darkness) in an andesite frame | same grate in a brass frame | netherite plate with a recessed beacon under a cage guard |
| Cell walls | dark stone brick `#33363a` `#3d4045` `#474a4f`, mortar `#2a2c30` | same | obsidian-and-blackstone cell `#1f1a22` `#2b2430` `#352d3a` |
| Floor | worn flagstones with a centre drain grate `#3a3c40` `#45484c` | same | same, darker |
| Chow bowl, chute | dented steel bowl `#4a5059` `#5f656e` rim `#6f757d`, chow `#5a4630` `#6e5a3a` `#4f5a34`; steel chute | same, brass rim band | not used (the Wither is fed through a sealed hopper port on the back wall) |
| Tray | iron tray with an andesite lip | iron tray with a brass lip | netherite tray |
| Padlock and hasp | dark iron lock `#3c424a`, brass keyhole plate `#8f7638` (small) | brass lock body `#8f7638` | — (field instead) |
| Water (Flooded) | muted deep blue-green `#26444f` `#2f5563` at about 55% alpha; never bright cyan | same | — |
| Shield (Netherite) | — | — | glowing blue `#2f5f9a` `#4a86c4`, bright edges and pattern lines `#6fb7c4` (never near-white), at 15–40% alpha |
| Klaxon (Netherite) | — | — | dark red lens `#5a221c`, lit `#a3412f` `#c4553c` |
| Glass | clear with 4–6 cool grey glints `#6f7f86` `#83939a` | same | same |

Big panels crisp with clean bevels (the pens' finish, not three-shade mottle, which was "muddy"); the stone cell gets gentle per-brick variation.

## Shape Language

Squares and verticals: a cell, bars, a grate, a lock. Circles only in the **bowl** and, on the Netherite cage, the **field coils** and **beacon**. The Netherite cage's shield is the one soft, glowing form in the family. The monsters bring the chaos.

## Silhouette Description

A cube, like the pens. Straight on: the pens' steel frame around a darker stone cell, with **three heavy vertical bars and a crossbar** across the window, monsters between the bars, a steel bowl glinting at the back, a tray of drops along the bottom. A **padlocked hasp** on the right-hand frame post (the "door" side). From the side: Andesite and Brass show a small barred **air vent** in the middle of each side; Flooded ones show the pens' tall **sight glass**, filled blue-green; Netherite shows a **power terminal** at the centre. From above: a grate, not wood. At 5 blocks: "a cage with something angry in it".

## Primary Forms

- **Shell**: the pens' 16 px cube, 12 × 12 window on the north face in a 2 px frame.
- **Bars**: three vertical bars, 1 × 1 px, at x 4.5, 7.5 and 10.5 (each 1 px wide, so gaps of 2 px with the frame), running y 2–14, plus one **crossbar** 1 px tall at about y 9.5, set in the plane between the glass and the tray (keep ≥ 0.25 px from both). On the Netherite cage the bars are blackened and there's no crossbar (the field is the lock).
- **Cell**: stone-brick walls on the inside faces (inner faces at x 2 / 14, as the pens' boarding), flagstone floor with its top at y 2.5 and a small drain grate in the middle.
- **Tray**: the family tray at x 3–13, z 1.2–3.8, in iron, under the bars.

## Secondary Forms

- **Chow chute and bowl** (dry and Flooded): a square steel chute down the back wall that swallows the shaft stub's tip (x 6–10, y 6–10, z 12.5–14), ending above a **dented steel pet bowl** centred on z 12 (about 6 px across, 1.5 px tall, rim at y ~4). Chow items lie in the bowl at the pens' trough positions (5.5 / 8 / 10.5, 3.1, 12) — move the bowl so they sit inside its rim.
- **Padlock and hasp**: on the front frame's right post, a 2 × 3 px hasp plate with a 2 × 2 px padlock hanging off it, recessed so nothing passes the block face. One small brass keyhole plate is the andesite cage's only brass.
- **Grated top**: the lid is a steel grate (1 px bars every 2 px over a dark gap texture, so it reads as open without being see-through), in the tier's frame.
- **Side vents** (dry): a 4 × 4 px barred vent at the centre of each side face, recessed 0.5 px, so every side reads as a port.
- **Shackle chains**: two short chains hanging from the back corners of the cell ceiling (each 0.5 px wide links, about 3 px long), the cell's one piece of set dressing. Keep them in the corners, out of the wander area.

### Flooded cages (Andesite and Brass)

- Same cell, bars and lock; the cell walls turn to **wet tiles** (darker, with a faint green tide line at the tank's middle) and a few strands of **kelp** grow from the back corners.
- **Water sight glass** on both sides: the pens' milk glass exactly (copper frame `#7a4a2e` `#9c5f3a`, 2 × 10 px slot, brass tap at its foot), filled blue-green by the renderer.
- **Trident hatch**: a 4 × 2 px hinged flap in the window, at the top of the middle bay between the bars (about x 6–10, y 12–14). It's the hole the tridents come through. It swings open (hinge along its top edge) for a few ticks at each throw. The glass has a matching notch, so the hatch is the one place the cell is open to the world: a nice detail for a cage of monsters that throw things.
- **Bubbles**: a small **air stone** on the floor at the back left, from which the renderer releases a few bubble particles while it runs.

### Netherite cage

- **No bowl.** The Wither is fed by a **sealed hopper port** on the back wall around the stub: a squat round port with a blast door (closed), at the same place as the chute.
- **Field coils**: four short copper coil stacks (Create's own copper palette, clean, not darkened: `#9a5038` `#a75a40` `#b26247`) in the four inner corners of the window frame, each with a small **blue** emitter tip. They're what makes the shield.
- **The shield** (part model `netherite_monster_cage_shield`, drawn by the renderer): while the field is up, **animated** (an animated texture, 12 frames at 2 ticks with an `.mcmeta`: the cell grid drifts up a pixel a frame, the glow pulses, and a brighter shimmer band sweeps up each face once a loop), a **glowing blue box hugging the whole block about 1 px outside every face**, translucent, with soft rounded-looking edges (a bright rim where its faces meet) and a faint hexagonal cell pattern that drifts slowly across it and pulses gently with the hum. It is the hero of this tier: from across the room a powered Netherite cage is a softly glowing blue cube.
  - **Short of FE** (the countdown): it flickers, dropping out and snapping back irregularly, with cracks of brighter blue running across it.
  - **Field down**: hidden; the coil tips go dark.
  - Faces of the shield against a solid neighbouring block aren't drawn, so a cage built into a wall doesn't glow through the wall.
  - Unlike the rest of the cage, the shield is drawn **full-bright** (it glows in the dark). It gives no block light.
- **Beacon** (part model `netherite_monster_cage_beacon`): set into the top plate under a 1 px cage guard, a dark red lens with a reflector that spins about Y during a countdown or escape and sits still otherwise; dark when all is well.
- **Power terminals**: a copper terminal at the centre (8, 8) of each side face (house rule: ports at face centres), with a short insulated collar.
- **The one hazard accent**: a single yellow-and-black stripe band (2 px) along the bottom of the front frame, under the window. Nothing else is striped.
- **Owner plate**: a small blank brass nameplate on the top-left of the front frame (it reads as "registered"); text is not drawn.

## Interesting Features

- The monsters pace behind real bars; drops pile up in the tray in front, just out of their reach.
- The dog bowl full of chow makes "Monster Chow" land as a joke without any text.
- A Flooded cage's drowned stalk about in blue-green water, bubbles rising from the air stone, and the little hatch at the top flaps open when one throws a trident at you.
- The Netherite cage hums inside a glowing blue shield; the copper coils glow at their tips, and when the power fails the shield flickers and cracks, the beacon spins and the klaxon goes.
- Sides tell you which cage it is: a vent (dry), a water glass (Flooded), a power terminal (Netherite).

## States

| State | What changes visually | Carried by |
|---|---|---|
| Empty | Empty cell; any leftover chow in the bowl | renderer (as the pens) |
| Running vs stopped | Monsters walk vs stand; shaft turns vs stops | renderer, existing |
| Chow | 0–3 chow items in the bowl | renderer (pens' trough logic) |
| Drops waiting | 0–3 items in the tray | `TrayItems` |
| Water level (Flooded) | Water volume and side glasses at level / capacity; the monsters keep walking on the floor (drowned and guardians don't need to swim up) | renderer |
| Armed drowned (Flooded) | the hatch flaps open for 4–6 ticks per throw | renderer + part model |
| Field up / flickering / down (Netherite) | shield glows and drifts / flickers and cracks / hidden; coil tips lit / flickering / dark | renderer + part model |
| Alarm (Netherite) | beacon lit and spinning, red light on the frame edges | renderer + part model |
| Tier | andesite vs brass trims, lock, grate frame | textures |

## Rig Requirements

No bones; part models placed where the renderer expects them (north-facing):
- `monster_cage_hatch`: modelled closed in the window notch; rotates about X along its **top edge** (hinge at y 14, z ≈ glass plane), 0 → 70° open.
- `netherite_monster_cage_beacon`: the reflector inside the lens, rotating about **Y** through the beacon's centre (x 8, z 8).
- `netherite_monster_cage_shield`: a box from −1 to 17 px on every axis (1 px outside the block), each face its own quad so the renderer can skip faces against solid neighbours; the hex pattern scrolls in UV and the alpha pulses. Drawn translucent and full-bright, after the cage.
- Renderer-only: water volume (translucent box x 2–14, z 1–14 from y 2.5 up to the level, capped at y 13.5), side water columns (as the milk column), bubbles.

## Fun Score (1-10)

9. It's the pen everyone knows, turned into a monster kennel at a glance (bars, padlock, dog bowl), and each tier adds a real reason to stop and watch: drowned in a flooded cell throwing tridents through a hatch, and a Wither glaring out of a glowing blue shield while the beacon waits to spin.

## Builder Notes

**Start from the pens' generator** (`design/models/animal-pen.gen.js` and the shared kit `process-box.lib.js`): same shell coordinates, same tray, same stub socket, same glass. New textures under new names (`monster_cage_frame`, `_cell`, `_floor`, `_bars`, `_grate`, `_bowl`, `_vent`, `_lock`, flooded `_tiles`, `_kelp`, netherite `_plate`, `_coil`, `_shield`, `_beacon`, brass variants of frame, grate, lock and tray lip). Export: `monster_cage.json`, `brass_monster_cage.json`, `flooded_monster_cage.json`, `brass_flooded_monster_cage.json`, `netherite_monster_cage.json`, plus the three part models.

**Must not lose**: the pens' shell and frame proportions (the sister read); readable monsters between the bars (keep exactly three bars); the floor at y 2.5; headroom; the stub socket; the tray in front of the bars.

**Pitfalls**:
- Bars between glass and tray need ≥ 0.25 px clearance from both (z-fight check).
- Water and the shield are **translucent**: own parts or renderer quads, never painted into cutout textures. The item model shows the cage dry and empty (Flooded: water drawn as an opaque tinted panel in the item model only).
- The shield is drawn outside the block bounds: check it doesn't z-fight with a neighbouring block's face (1 px off is enough) and that the block entity's render bounds include it, or it pops out at the screen edges.
- Big monsters (ghast, ravager, elder guardian, the Wither) are scaled down to 6.4 px tall; check in preview that a ghast's tentacles and a Wither's heads clear the bars and the crossbar at the wander edges.
- The grate top must not be see-through (a dark backing under it), or the top face shows the cell's insides in odd ways.
- Copper on the Netherite coils is Create's clean copper, not darkened (the Wellspring lesson).
- No light level; the beacon and coil glow are textures, not block light.

**Screenshot for review**: all five straight on and three-quarter in a row; the dry cage with zombie and creeper stand-ins at the wander edges; a Flooded cage at empty / half / full water with a drowned stand-in, and the hatch closed and open; the Netherite cage with a Wither stand-in, shield up / a flicker frame / down, in daylight and in the dark, against a wall (face culled), beacon lit; side views of the vent, the water glass and the terminal; the top grate; the bowl with 0–3 chow; the item models; and one shot beside the Animal Pen to check the sister read.

---

# Items: Iron Jar, Netherite Jar, Monster Chow

16 × 16 item sprites, flat (generated), in the house palette.

- **Iron Jar** and **Netherite Jar** are **3D item models built from the Capture Bottle** (glass body x4-12 y0-9, neck, lid; its `capture_bottle_glass` texture is reused): squatter and wider (glass x3.5-12.5, y0-8), with two metal bands round the body, a clamp lid with a knob, a wire bail over the lid and a clamp lever on the side. Iron: dark iron bands and lid. Netherite: netherite bands and lid and a **blue rim** at the top of the glass (the shield's colour); fireproof. A filled jar shows its monster inside, as the Bottled Animal does.
- **Monster Chow**: a dented tin can with its lid peeled back, full of lumpy brown-and-sickly-green kibble, one bone-shaped biscuit sticking out. Muted: tin `#5f656e` `#7c7f79`, chow `#5a4630` `#6e5a3a` `#4f5a34`. It should look edible only to a zombie.

**Fun Score**: 8. The tin of chow is a little gag in the inventory; the jars read as "stronger than a bottle" at a glance.
