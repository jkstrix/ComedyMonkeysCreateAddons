# Design Brief: Animal Pens (Animal Pen and Brass Animal Pen)

Redesign of an existing block. What the code does today is in `design/dossiers/animal-pen.md`. Family brief shared with `design/briefs/auto-harvester.md`.

## Hard Requirements

From the user:
- Scope: "Redesign + small code". New look; small renderer changes are allowed where the design needs them; the shaft and the block's behaviour stay as they are.
- "Own textures": the pens get their own copies of every texture they share today. The Cobble Generators, Auto Harvester (until its own redesign lands) and Stone Infuser must not change.
- Show the **milk level** on the block.
- Show **eggs and drops** waiting in the output.
- Fix what the user dislikes: the "bright brass pen", the "near-white glass glints", the "dark, empty chamber".
- Dark, muted Create palette; light areas mid-grey at most; accents as trims, no hazard stripes (house style).
- Previewed in Blockbench before anything goes into the mod.
- Nothing on `design/hands-off.md` changes (the Trade Bot and the Cuckoo Clock).

From the code:
- Blockstates `animal_pen.json` and `brass_animal_pen.json` load one static model each; the item models parent them. Both tiers keep **identical geometry** (the renderer draws both the same way).
- Modelled with the window facing **north**, shaft at the back (south) (`AnimalPenRenderer.java:45-48`).
- **Floor surface at exactly y 2.5**: animal feet stand there (`AnimalPenRenderer.java:27`).
- Animals: up to 4 real entities, scale `min(0.26, 0.4/size)`, tallest 6.4 px; feet wander x3.5–12.5, z2.5–9.5 today (`PenAnimal.java:15`). Leave headroom to y 10–11.
- Trough food: up to 3 items at (5.5 / 8 / 10.5, 3.1, 12) at scale 0.3 (`AnimalPenRenderer.java:31-32`). The trough stays centred on z 12, spanning about x3–13, bed just under y 3.1.
- Back shaft stub (`cobble_generator_shaft`, frozen because the Cuckoo Clock uses it) at x6–10, y6–10, z12.5–16. The back needs a 4 × 4 socket centred at (8, 8).
- Shaft into the **back** only; items in and out, milk out and the comparator on **every** face. The look must not suggest only one face is a port.
- Full-cube shape, cutout render type, no light level.

## Asset Type

Block (static model) with renderer-drawn contents. 16 × 16 × 16 px, `java_block`. Two tiers from one geometry with different textures.

## Family

The **sealed process box**, shared with the Auto Harvester (see its brief): the same frame, glass, top, bottom and back construction; the lighter panelled chamber; muted glass; and **the front tray**, where what's waiting to be collected sits on show. In the pen that's eggs, wool and drops.

The pen's own interior is a **stall**: walnut boarding, a fence rail, straw, a trough at the back fed by a hay chute.

## Fantasy

A little stable in a box: real animals pottering about on straw, a hay chute topping up the trough at the back, eggs and wool turning up in the tray at the front, and a milk gauge on the side filling up for the pipes.

## Hero Element

**The animals themselves**, framed like a diorama. Everything else is staging that makes them read: a warmer, lighter stall behind them, the trough they walk to, and the tray of produce in front of them. The second read, from outside, is the **milk sight glass** on each side.

## Theme

Create andesite (base) and brass (brass tier), with farm wood inside.

| Part | Base pen | Brass pen |
|---|---|---|
| Frame, outside walls | dark steel `#2f343b` `#3c424a` `#4a5059`, andesite trim `#6b6e6a` `#7c7f79` | dark steel, **muted brass** trim `#8f7638` `#b08f45` (highlight `#c9a75a` single pixels only) |
| Top | Create andesite casing (borrowed) | own top: dark walnut with a muted brass frame, replacing Create's brighter brass casing |
| Chamber | walnut boarding `#3b2a1f` `#4e3626` `#634532` below a fence rail, dark steel above `#3a3f46` `#454b53` | same |
| Straw | muted ochre straw `#6e5f3a` `#8a7a4a` `#9c8a52` | same |
| Trough, tray | walnut with andesite lips | walnut with muted brass lips |
| Milk sight glass | copper frame `#7a4a2e` `#9c5f3a`; dark glass `#2a2f33`; milk muted cream `#a39d8b` `#b9b29f`, never white | same |
| Glass | clear with 4–6 cool grey glints `#6f7f86` `#83939a` | same |

Gentle mottling on every surface. The two tiers differ only in trim metal and the top.

## Shape Language

Squares: a sturdy stall, boards, rails and boxes. The animals bring the curves.

## Silhouette Description

A cube. Straight on: a steel frame around a warm, boarded stall, lighter than today's black hole; a fence rail across the walls at about y 6; a trough at the back with a hay chute above it; a low tray along the bottom edge in front. From the side: a tall, narrow copper-framed sight glass with a small tap at its foot. At 5 blocks it reads as "box with animals in it", and from the side "it holds milk".

## Primary Forms

- The shell: 16 px cube, 12 × 12 window on the north face in a 2-px steel frame.
- The stall interior: boarded walls, straw floor whose top is y 2.5.
- The trough: a walnut trough at the back, x3–13, centred on z 12, bed under y 3.1.
- The front tray: x3–13, z1.2–3.8, bed at y 2.5 (level with the straw).

## Secondary Forms

- **Hay chute**: a short wooden chute on the back wall above the trough, which also swallows the shaft stub's tip (x6–10, y6–10, z12.5–14), so the stub reads as driving the feeder.
- **Fence rail**: a 0.5 px walnut rail along the inside of both side walls and the back, about y 6, plus posts in the boarding texture.
- **Milk sight glass** on both side faces: a copper frame around a 2 × 10 px recessed glass slot (about y 3–13), with a small brass tap at its foot, recessed so nothing sticks out of the block.

## Interesting Features

- The tray fills with what the herd makes: eggs from chickens, wool from sheep, drops after the sword.
- The milk column rises in the side glass and stays dark for animals that don't make milk.
- "Food in at the back, produce out at the front": the stall tells its own story.
- The back loses Create's 4-way gearbox texture for a single centred shaft socket.
- The brass pen is told apart by muted brass trims and its walnut-and-brass top, not by yellow bands round every edge.

## States

| State | What changes visually | Carried by |
|---|---|---|
| Empty | Empty stall (any leftover food in the trough as today) | existing |
| Running vs stopped | Animals walk vs stand; shaft turns vs stops | existing |
| Fed | 0–3 food items in the trough | existing |
| **Produce waiting** (`Output`) | 0–3 items in the front tray: 1 when there's anything, 2 from a third full, 3 from two thirds full | **renderer, new** (same helper as the harvester) |
| **Milk level** | Cream column in both side sight glasses, height = milk / capacity | **renderer, new** |
| Sheared, babies, animal type | as today | existing |
| Tier | Andesite vs muted brass trims and top | textures |

## Rig Requirements

No moving parts in the block model. New renderer-drawn parts:
- **Milk column**: a quad (or thin box) in each side slot, scaled in Y from the slot bottom (about y 3) by milk / capacity, drawn just inside the recessed glass. Needs the slot positions in the model to be exact.
- **Tray items**: up to 3 items lying in the tray, tilted about 30° towards the window.

## Fun Score (1-10)

8. The animals were already the charm; now they live somewhere that looks like a stable, and the pen shows its produce and its milk, so the player can read it without goggles.

## Builder Notes

**Start from scratch** on today's coordinates (the shell numbers are in the dossier). Export to `animal_pen.json` and `brass_animal_pen.json`; new textures under new names (`animal_pen_frame`, `_chamber`, `_boards`, `_straw`, `_glass`, `_back`, `_bottom`, `_side` with the sight-glass frame, `_tray`, `_trough`, and brass variants of the frame, side, bottom, top, tray lip).

**Code changes the design needs** (small, for the implementation stage):
1. **Wander bounds**: feet z min 2.5 → 4.5 (`PenAnimal.java:15`), so animals stay behind the tray. It also stops adult heads reaching through the glass, which the dossier estimates happens today. The model's clearance check added z max 9.5 → 8.8, so a big animal's head stays out of the hay chute (approved with the model).
2. **Tray items**: the shared tray helper from the harvester, fed from the pen's output buffer.
3. **Milk column**: draw the fill in both side slots from the synced milk level and the tier's capacity.

**Must not lose**: the floor at y 2.5, the trough at z 12, headroom over the animals, the window size (a smaller window hides animals at the edges), the stub socket.

**Pitfalls**: animal heads and tails reach past the wander rectangle (estimated 3.6 px ahead, 3.1 px behind), so keep the boarding and fence rail thin against the side walls (inner face x 2 / 14). Don't put anything solid in the stall's open volume. The milk column must sit in front of the slot's dark backing to avoid z-fighting, and inside the block bounds.

**Screenshot for review**: straight on and three-quarter for both tiers, a side view of the sight glass at empty / half / full, the tray at 0–3 items, a cow and a chicken stand-in at the wander edges to check clearance, the item models, and each next to its "before" shot from `design/dossiers/animal-pen/`.
