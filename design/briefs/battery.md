# Design Brief: Battery and Industrial Battery

Gameplay is decided in `design/specs/battery.md` (quick read: https://claude.ai/artifact/E9dzpPAxLxBbCFHvM6FStM). This brief is the look and motion only.

## Hard Requirements

From the user (2026-10-26):
- "This is a battery block."
- "You can connect them together (like you can do magma generators), they have connectors on the side in the same way, but also ones front and back."
- "If you connect 3x3x3 you get a bigger industrial battery multiblock that can go up to 9 high."
- "Bigger multiblock battery has connectors like the poles that can only be connected to poles."
- "Make it interesting and dynamic and fun."
- Chosen features: **charge you can see** (cells on the front light as it fills, a flow light for charging or draining, a big level column on the Industrial Battery that rises and falls); **lightning** charges it through a Lightning Rod on top (a crackle and arcs over it); a broken Battery **keeps its charge** as an item.
- Four insulators on the Industrial Battery, one on each top corner, like a pole head; cables only to Line Posts.

Standing rules:
- Dark, muted, Create-like; accents are trims, glows small; mid grey at most for light areas; preview in Blockbench before anything goes into the mod.
- **Ports at the middle of the face, (8, 8)**: Power Lines run up a face to its middle and into a socket there.
- Copper is **Create's own copper**; brass is the generators' **mid brass**, no bright highlights; ceramic is bone grey, never white.
- Electric family lights: **cyan means power**, amber means full or warm, dark red means off. Running sounds are quiet background.
- Round shapes are for motors and engines; boxes and cabinets stay square.

## Asset Type

Block (16 × 16 × 16 px), plus a multiblock of tiled blocks (48 × 48 px footprint, 48 to 144 px tall). `java_block` models; fills, pulses, lamps and arcs are renderer or Flywheel parts drawn over the static models. Placed facing the player (four horizontal facings): the cells and lamps are on the front. The Industrial Battery faces the way of the Battery that completed it; its level column is on that face. Breaking a block re-forms the tallest run left, so the tiles must suit any height from 3 to 9: a station can be 3, 4, 5, 6, 7, 8 or 9 high (the user), and the preview shows all seven.

## Fantasy

**Battery:** a sturdy steel accumulator cabinet, two glass cell tubes on its face filling with cyan light as it charges, like watching a tank fill.
**Industrial Battery:** a substation in your base: a tall steel tower of cells with a great glowing level column up its front, ribbed ceramic bushings on its corners carrying the grid, and a lightning rod that turns storms into stored power.

## Hero Element

**The charge light.** On the Battery, the twin cell tubes on its front; on the Industrial Battery, one full-height level column up the middle of its front. Both fill bottom-up in cyan as the charge rises, and pulses move up through them while charging and down while giving power. A player reads "how full" from across the room, and "which way" from the motion.

## Theme

| Material | Use | Colours |
|---|---|---|
| Dark blue-grey steel | cabinet, frame, tower | `#2f343b` `#3c424a` `#4a5059` (crisp bevelled panels, flat fields, no mottle) |
| Andesite trim | plinth edge, cap edge, collars | `#6b6e6a` `#7c7f79` |
| Create copper | terminal posts, bus bars, the Industrial Battery's bus strips | `#904931` `#9a5038` `#a75a40` `#b26247` `#c26b4c` |
| Mid brass | tube bezels and the level column's tick marks | `#7e6832` `#8f7638` `#a3873f` |
| Ceramic | the Industrial Battery's bushings | bone grey `#7d786f` `#8f897e` `#9c968b` |
| Charge | fill light in the tubes and column | cyan `#6fb7c4`, dim `#3f7d84` near the bottom of a fill, dark glass `#1f2a2e` when empty |
| Lamps | charging ▲, giving ▼ | cyan `#6fb7c4`, amber `#d99a4a`; dark red `#8a2f22` ember when empty |

Sits beside: the **Lava Generator** (its side terminal plates and copper posts are the Battery's connectors), the **Line Posts** (the bushings are their big industrial cousins, the same ceramic and tier caps), the **Geothermal Turbine** (the same steel and trim, the same "one cyan light means power").

## Shape Language

**Squares.** A battery is a store: heavy, stable, square. Roundness only where a real part is round: the glass cell tubes and the bushings' ribbed discs.

## Silhouette Description

**Battery:** a solid square cabinet on a slightly wider plinth, a capped top with a copper strike plate in the middle, a copper terminal plate standing proud at the middle of each of the four sides, and two glowing vertical slots on the front. Reads as "a little power cabinet", and in a row as a bank.

**Industrial Battery:** a tall square tower, 3 wide, on a heavy plinth band, with vertical ribbed fins down its sides and a glowing column up its front middle; four tall ribbed bushings stand on the top corners like chess rooks, cables sweeping away from them to the poles. Reads as "substation", a skyline piece.

## Primary Forms

**Battery (16 px):**
- **Plinth:** x 0–16, z 0–16, y 0–2, andesite-trimmed steel.
- **Cabinet:** x 1–15, z 1–15, y 2–14, crisp steel panels inset 0.5 px within a frame.
- **Cap:** x 0.5–15.5, z 0.5–15.5, y 14–16, with a 5 × 5 copper strike plate in the middle (where a Lightning Rod sits).

**Industrial Battery (per layer, 48 × 48):**
- **Plinth band** (bottom layer): a heavy base, 3 px tall, a step wider, with andesite edge.
- **Tower:** the 3 × 3 cabinet faces, panels joined (no seams between the blocks of one face, only between layers, a thin dark seam every 16 px).
- **Crown** (top layer): a cap band with a low parapet, and the four bushings.

## Secondary Forms

**Battery:**
- **Twin cell tubes** (front): two glass tubes at x 3–5.5 and x 10.5–13, y 3.5–12.5, each in a mid-brass bezel; the cyan fill rises inside them.
- **Front connector** at (8, 8): a dark insulator plate (5 × 5) with two copper terminal posts and andesite collars, the Lava Generator's terminal, between the tubes.
- **Flow lamps** above the front connector: a small cyan ▲ (charging) and amber ▼ (giving), 2 px each, side by side at y 11–13.
- **Side and back connectors** at (8, 8): the same terminal plate on each face.
- **Bus bars** (state): a face joined to another Battery shows a copper bus bar across the gap instead of its terminal posts, a flat copper strap meeting its neighbour's halfway, so a bank reads as joined.

**Industrial Battery: the stack (revised 2026-10-26).** The user: "it's more the repeating pattern, I don't like the vents but as they get bigger, maybe different styled blocks can appear in the stack layers"; chose anchored modules. Layers are modules with a job each, at fixed places, so a taller tower gains new kinds instead of repeating one:
- **Base** (the bottom layer): the heavy plinth, and a bolted access hatch on each front side tile.
- **Control** (the second layer, from 4 high): a knife switch (a copper blade closed into its jaws on a ceramic base, with a walnut handle) under a warning sign; the other front side tile is a cell bay. No meter: the user, "not a fan of extra dials that add nothing" (the column already shows the charge).
- **Junction** (from 6 high, the middle layer: layer ⌈H/2⌉): a copper bus band across the front, bolted, joining the two corner bus strips; the level column runs behind a gap in it.
- **Cells** (every other layer): plain panels, the quiet filler.
- **Crown** (the top layer): the cap, lip, bushings and copper strike plate.
- No vents, no section belts.

**Industrial Battery: substation dressing (revised 2026-10-26).** The user: "by more detail I meant like bits that stick in or out, warning colours, things that look industrial like a sub station in a power grid, be creative with the tall one, doesn't have to match the little one". So the tower gets real substation hardware in relief, and warning colours (muted safety yellow `#b39234` `#8f7428`, black `#25282c`, earth green `#4f7a3c`) overriding the house rule of at most one hazard accent:
- **Radiator banks** standing 6 px off both sides: vertical fins between header pipes, on brackets, one bank per run of cell layers (the transformer's radiators).
- **A caged ladder** up the back, from the ground to a **railed catwalk** under the crown: grated platform, yellow handrails, hazard-striped toe boards.
- **A conservator tank** on legs across the back of the crown, behind the bushings.
- **Recessed cell bays** on the front's cell layers (1.5 px in), each with a pull handle. No extra lamps or dials (they'd only repeat the column).
- **Conduits** up the two back corners, clamped every block, from gland boxes at the base to junction boxes at the top.
- **A busbar chamber** standing out of each side on the junction layer.
- **A green-and-yellow earth strap** down one front corner to a ground clamp.
- **Warning colours**: a hazard-striped plinth band, and lightning warning signs on the base's sides, at the ladder foot and above the knife switch.

**Industrial Battery:**
- **Level column** up the front middle block column: a glass column 6 px wide inset in a dark channel, brass tick marks on its frame at every block height, the cyan fill rising through all 3 to 9 layers.
- **Cooling fins** down the two sides and back: vertical steel ribs, 1 px proud, 2 px apart, the real radiator fins of a substation; they carry the silhouette at a distance.
- **Copper bus strips:** two vertical copper straps running up the front corners to the crown, showing where the bushings' power goes.
- **Bushings** (four, on the top corners): each a ribbed ceramic stack (four discs, 5 px, 4 px, 4 px, 3 px, with 1.5 px necks), about 9 px tall, on an andesite collar, with a tier-coloured terminal cap where the span ties on, as on the Line Posts.
- **Strike plate** in the crown's middle: a copper plate where a Lightning Rod stands (the user: "area for rod should be copper"; the Battery's too).

## Interesting Features

- **A battery you read like a fuel tank**: the fill height is the charge, in eight steps on the Battery and smoothly up the Industrial Battery's column.
- **Motion tells the direction**: soft light pulses travel up the tubes while charging, down while giving; still when idle. The ▲ and ▼ lamps say it too, for anyone who doesn't watch.
- **Joined banks look joined**: copper bus bars bridge the faces of touching Batteries.
- **Forming is an event**: when the last Battery completes the 3×3, a ripple of cyan runs up the column from the bottom, the fins catch it, and the bushings click into place.
- **Lightning**: a strike on the rod throws two or three short cyan arcs from the rod to the bushings (or over the Battery's cap), the column flashes and jumps, with a sharp crackle.
- **The item shows its charge**: the inventory icon's tubes are filled to the item's stored charge (four steps), plus the usual bar.

## States

| State | What shows | Carried by |
|---|---|---|
| Empty | Tubes and column dark glass, a dark red ember at their bottoms, lamps off | fill part at 0, ember |
| Charging | Fill rising, pulses travelling up, ▲ lamp cyan | fill, pulse, lamp |
| Giving | Fill falling, pulses travelling down, ▼ lamp amber | fill, pulse, lamp |
| Idle | Fill still, lamps off | fill |
| Full | Tubes or column full and steady, no pulses | fill |
| Joined face | Bus bar instead of terminal posts | blockstate per side |
| Forming | Cyan ripple up the column, 1 s | renderer, once |
| Lightning | Arcs from the rod, column flash, 0.5 s | renderer, once |
| Formed, unloaded far away | (nothing visible: it isn't drawn) | — |

## Rig Requirements

Moving or changing parts, for the renderer or Flywheel:
- **Battery:** `tube_fill_left`, `tube_fill_right`: scale Y from the tube's bottom (y 3.5), in eight steps; `pulse`: a brighter band that slides along each tube; `lamp_in`, `lamp_out`: on/off; `ember`: on when empty; per-side `bus_bar`: a static part shown by blockstate.
- **Industrial Battery:** `level_fill`: one per layer of the column, filled bottom to top (each layer a block's height, scaled in Y); `pulse` along the whole column; `arc`: renderer-drawn segments from the rod to each bushing cap; bushings and fins are static.

## Fun Score (1-10)

**8.** It shows its charge and its direction at a glance, banks visibly join, forming is a moment, the tall tower with its bushings makes a skyline, and lightning turns storms into an event.

## Builder Notes

- **Ports at (8, 8)** on all four connector faces; the front connector sits between the tubes. None on top or bottom (the top is the strike plate).
- **Industrial Battery models:** too tall for one model (vanilla's −16..32 limit), so build it as **tiles by position**: corner, edge and front-middle pieces for the bottom, middle and top layers (the front middle column carries the level channel; the crown pieces carry the bushings on the corners). Each tile is a 16 px block model; the fill, pulse and arcs are drawn by a renderer from the bottom-front-middle block.
- **Bushings** stand inside the corner blocks of the top layer (they may rise above y 16 by up to 10 px, inside the −16..32 limit).
- **No z-fighting**: fills sit 0.25 px behind their glass; bezels go round, not behind; the bus bar meets its neighbour's at the block edge.
- **Glows full-bright but muted**: never near-white; the cyan is the family's.
- Keep the panels crisp: flat fields with bevels, no mottle ("muddy").
- **Screenshot for review:** a single Battery front, side and three-quarter at empty, half and full; a bank of three in a row (bus bars); the Industrial Battery at 3 and 9 high, front and three-quarter, half full, with a span to a Line Post; the inventory icon at empty and full.
- **Preview clips:** `charging` (fill rising, pulses up, ▲ lamp), `giving` (pulses down, ▼ lamp), `forming` (the ripple), `lightning` (arcs and flash).

## Sound

See `design/sound/battery/battery.md` (approved 2026-10-26: "sounds good"). The plain Battery is silent in use; the Industrial Battery has a quiet hum while power flows, a contactor clunk forming, a clack coming apart and an arc crack for lightning.
