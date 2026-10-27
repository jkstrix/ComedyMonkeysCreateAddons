# Design Brief: Fish Tanks (Fish Tank and Brass Fish Tank)

New blocks. What they do is in `design/specs/monster-cage-and-fish-tank.md` (Part 2). Family brief shared with `design/briefs/animal-pen.md` and `design/briefs/monster-cage.md`. Item at the end: Fish Food.

## Hard Requirements

From the user:
- "Must be a **sister block to the existing pens**, with **some flair**."
- Dark, muted Create palette; light areas mid-grey at most; accents as trims (house style).
- Previewed in Blockbench before anything goes into the mod.
- Each tier keeps its own trim metal; brass is the generators' warmer mid brass (`#7e6832` `#8f7638` `#a3873f`).

From the spec (reusing the Animal Pen's renderer and the Flooded cage's water):
- Real creatures (up to 4 entities: fish, squid, glow squid, axolotls), **swimming** in the water volume rather than walking.
- Shaft into the **back** only (the shared stub, 4 × 4 socket at (8, 8)). Items in and out on every face; water in on every face **but the back**.
- Water drawn by the renderer to the tank's level as its own **translucent** part; the level must read from outside.
- Food shown in the tank (0–3 Fish Food), produce waiting in a front catch (0–3 items via `TrayItems`).
- A tank of glow squid gives **light level 6** (a blockstate or block-entity light; the look should show where the light comes from).
- Full-cube shape; everything inside the block bounds.

## Asset Type

Block with renderer-drawn contents and one moving part. 16 × 16 × 16 px, `java_block`, window facing **north**. One geometry for both tiers (textures differ). Part model: `fish_tank_pump`.

## Family

The pens' box, frame and back socket, so it reads as their sibling. Its flair is **glass**: where the pen has one window and a sight glass on each side, the tank has **windows on the front and both sides**, a real aquarium you can look into from three directions, and the sides show the water level directly, so it doesn't need a separate sight glass.

## Fantasy

A Create engineer's aquarium: a riveted glass tank with fish drifting over the gravel, kelp swaying, an air pump on the back wall chugging in time with the shaft and sending bubbles up from an air stone, a little feeder on the lid sprinkling food, and a half-sunk Create cogwheel on the bottom as the tank ornament.

## Hero Element

**The water, framed on three sides**: a lit blue-green volume with real fish in it, visible from the front and both sides, with its surface line at the current level. Supporting it: the **sunken cogwheel ornament**, the joke that makes it ours.

## Theme

Create andesite and brass machine around a clean aquarium. It sits beside the Animal Pens and the Flooded Monster Cage (the same water, very different mood: calm, not menacing).

| Part | Fish Tank | Brass Fish Tank |
|---|---|---|
| Frame, corner posts | dark steel `#2f343b` `#3c424a` `#4a5059`, andesite trim `#6b6e6a` `#7c7f79`, rivets along the posts | dark steel, mid brass trim, brighter brass rivets only |
| Glass | clear with a few cool glints `#6f7f86` `#83939a` | same |
| Lid | walnut lid (the pens' top, one step lighter as landed: `#4e3626` `#634532` `#6f4d36`) with a small steel **feeder hopper** at the front-left | walnut lid with a brass-rimmed feeder |
| Back wall (inside) | dark slate tiles `#2e3a40` `#36444b` | same |
| Gravel floor | muted river gravel `#4a4740` `#5a564c` `#6b665a` (never above mid grey) | same |
| Plants | kelp `#2f4a2a` `#3e5e33` `#4f7340`, seagrass tufts | same |
| Ornament | Create's own cogwheel, half sunk | the same |
| Air pump | steel body, a copper air pipe (Create's copper `#9a5038` `#a75a40` `#b26247`) down to a grey air stone | brass pump cap |
| Water | muted blue-green `#2a5a66` `#336a77` at about 50% alpha; never bright cyan | same |
| Catch net | walnut rim with a dark net mesh `#2a2c30` | brass rim |

## Shape Language

Squares for the tank (frame, posts, lid), **soft verticals** for the life inside (kelp, bubbles), and **one circle**: the sunken cogwheel.

## Silhouette Description

A cube with **glass on three sides**: a steel frame with corner posts, a walnut lid, and through the glass a blue-green column of water with a visible surface line, a gravel bed, kelp at the back corners and a cogwheel half-buried in front of them. From the side you see straight through to the fish; from the back, a solid steel wall with the shaft socket and the pump's copper pipe. At 5 blocks: "an aquarium", and the water line tells you how full it is.

## Primary Forms

- **Shell**: 16 px cube; front, left and right faces each a 12 × 12 window in the pens' 2 px frame; the back solid; the lid walnut.
- **Gravel bed**: top at y 2.5 (the family floor), a slight slope higher at the back (y 2.5 → 3.5) so the cog and kelp sit up.
- **Water**: renderer volume x 2–14, z 2–14, from the gravel to level × capacity, capped at y 13.5.
- **Catch net**: replaces the tray in the same place (x 3–13, z 1.2–3.8): a shallow walnut-rimmed net basket hanging just inside the front glass at y 2.5–4, holding 0–3 drops.

## Secondary Forms

- **Air pump** on the back wall around the shaft stub (x 6–10, y 6–10, z 12.5–14): a small steel piston pump that swallows the stub's tip, with a copper pipe running down the back-left corner to an **air stone** on the gravel. The pump's **piston** (part model) bobs up and down with the shaft angle: rotation visibly does something.
- **Feeder**: a small steel hopper on the lid's front-left (recessed, flush with the top), with a chute through the lid. Fish Food items (0–3) appear sinking just under the surface below it, at the pens' three trough positions moved up to just under the water line.
- **Kelp**: two strands in the back corners, 6–9 px tall, and a few seagrass tufts. Static in the model.
- **Sunken cogwheel**: **Create's own small cogwheel** (`create:block/cogwheel_shaftless`, referenced at runtime, not copied), drawn by the renderer at a third of its size (about 6 px across), lying tipped in the gravel with one side sunk in, front-right of centre (centre (5.2, 2.9, 7.2), turned −28° / 20° / −12° about X / Y / Z). Both tiers use it. (User, 2026-10-27: the first hand-built cog "looked like a saw".)
- **Glow**: when the tank holds glow squid, a soft teal-green wash on the back tiles (renderer tint), so the light level 6 has a visible source.

## Interesting Features

- It's the only block in the family you can look into from the side: fish drift past the side glass as you walk by.
- The pump chugs with the shaft and bubbles rise from the air stone; stop the shaft and the bubbles stop.
- Food sprinkles in from the lid feeder and sinks.
- The sunken cogwheel (brass in the Brass tank) is the Create-joke ornament every aquarium needs.
- A tank of glow squid glows; a tank with a blue axolotl is something to show off.
- Catches wait in a little net at the front, not a tray: the same family motif, in the tank's own language.

## States

| State | What changes visually | Carried by |
|---|---|---|
| Empty | Water (if any), plants and cog; no creatures | renderer |
| Water level | Volume height and surface line at level / capacity; creatures swim only below the surface | renderer |
| Running vs stopped | creatures swim vs hover; pump bobs vs stops; bubbles vs none | renderer + part model |
| Food | 0–3 Fish Food sinking under the feeder | renderer (pens' trough logic, new positions) |
| Catches waiting | 0–3 items in the net | `TrayItems` (new positions) |
| Glow squid | teal-green wash on the back tiles; light level 6 | renderer + blockstate light |
| Tier | andesite vs brass trims, feeder rim, cog, net rim | textures |

## Rig Requirements

- Create's `cogwheel_shaftless` model, drawn as the ornament (see Secondary Forms).
- `fish_tank_pump`: the piston rod and head, modelled at the bottom of its stroke; the renderer translates it **up 0–1.5 px in Y** with the sine of the shaft angle.
- Renderer-only: water volume and surface; creatures on swim paths inside x 3–13, z 4.5–12, y (gravel + 1) to (surface − 1); bubbles from the air stone (x ≈ 3.5, z ≈ 12.5) rising to the surface; food items.
- **Code change** the design needs: a swim mover for tank creatures (the pens' `PenAnimal` walks on a floor; tank creatures need 3D paths with gentle turns, and squid a slow pulse). Axolotl young swim like adults.

## Fun Score (1-10)

9. An aquarium you can peer into from three sides, with a pump that visibly runs off the shaft, food that sinks, bubbles, glowing squid and a sunken cogwheel: it's the block players will put in their living room, not their farm.

## Builder Notes

**Start from the pens' generator and the process-box kit**; copy the frame for the two extra side windows. New textures under new names (`fish_tank_frame`, `_glass`, `_lid`, `_tiles`, `_gravel`, `_kelp`, `_cog`, `_pump`, `_net`, and brass variants). Export `fish_tank.json`, `brass_fish_tank.json` and `fish_tank_pump.json`.

**Must not lose**: the pens' frame proportions (sister read), glass on three faces, the back socket, the floor at y 2.5, the catch at the front.

**Pitfalls**:
- Three glass faces mean the cell is seen from the sides: the back wall and lid underside must be finished, and the pump and pipe must look right from both sides.
- Water is **translucent** and drawn by the renderer; glass stays cutout with opaque glints. The item model shows the tank half full, with an opaque tinted panel for the water and no creatures (the item can't draw entities).
- Glass side faces next to another tank: glass-to-glass faces should not cull the water (check two tanks side by side).
- Keep kelp and the cog out of the swim volume, or fish clip through them.

**Screenshot for review**: both tiers straight on, three-quarter and from the side; water at empty / half / full; the pump at the top and bottom of its stroke; food 0–3; the net with 0–3 items; a glow squid tank's wash; two tanks side by side; and one beside the Animal Pen and a Flooded Monster Cage to check the family read.

---

# Item: Fish Food

A 16 × 16 flat item: a small round paper-and-tin flake tub with a lid tipped open and coloured flakes (muted kelp green `#4f7340`, ochre `#8a7a4a`, rust `#9c5f3a`) spilling over the rim. **Fun Score**: 7, a plain, friendly icon that reads as "fish food" in any language.
