# Design Brief: Electric Lights, Wall, Ceiling and Hanging (group 2)

Group 2 of the Electric Lights set ([spec](../specs/electric-lights.md)): **Bulkhead Light**, **Wall Sconce**, **Gooseneck Lamp** (wall); **High-Bay Lamp**, **Cage Lamp**, **Flush Ceiling Light** (ceiling); **Pendant Lamp**, **Chandelier** (hanging). Group 1 (street lamps) is [electric-lights-street.md](electric-lights-street.md); group 3 is [electric-lights-runs-and-specials.md](electric-lights-runs-and-specials.md).

## Hard Requirements
- "Wall lights, ceiling lights (industrial and not), hanging lights", "Create styled but fit each scenario they are meant for", "fairly cheap to make", "interesting, varied and useful" (user, 2026-10-28).
- Places, from the spec catalogue: **Bulkhead** (caged oval, industrial: tunnels, factories), **Wall Sconce** (brass with a glass shade: homes, halls), **Gooseneck Lamp** (curved arm shining down: signs, doorways); **High-Bay Lamp** (big industrial dome, warehouses), **Cage Lamp** (industrial), **Flush Ceiling Light** (low, domestic); **Pendant Lamp** (enamel shade), **Chandelier** (brass, several arms).
- Placement (P6): wall lights on **any wall face**; ceiling lights on **the underside of a block**, the **High-Bay on a short rod**.
- Hanging (D19): **right-click with a Power Line to lower the lamp one block, up to 16; the gap fills with a hanging cable** that carries the power, with no collision.
- **The High-Bay warms up like a sodium lamp**, dim amber to full over 4 s (P4). Lights flicker when power runs short (P3). Both are drawn by the renderer's glow (P28).
- Dye colours **only the glow**, muted; **undyed is warm incandescent** (D13). Every light except the Chandelier is **waterloggable** (P12).
- House rules: dark, muted Create palette, mid grey at most; glows muted, never near-white; accents sparingly; preview in Blockbench first; solid parts baked or Flywheel, glow on the renderer (D8).

## Asset Type
Blocks (8). 16 px = 1 block. Each fits inside its block except the Gooseneck (reaches 11 px off the wall, still inside) and the Chandelier (14 px across). The hanging cable is a second block (a cable segment) repeated per block of drop.

## Fantasy
Lighting a Create build from the inside: bulkheads down a tunnel, a row of big domes over the factory floor, a warm sconce by the cottage door, a pendant over the workbench, and a brass chandelier in the station hall.

## Hero Element
**The lit glass in a shape that says where it belongs.** Industrial lights (Bulkhead, High-Bay, Cage) are dark steel with cages and reflectors; domestic ones (Sconce, Flush, Pendant, Chandelier) have brass and frosted glass. In each, the glow is the brightest thing, and the hardware around it explains how it's held and protected.

## Theme
Same palette as group 1: **cast iron** `#26292d` `#2f3338` `#3a3f45`, **dark steel** `#2f343b` `#3c424a` `#4a5059`, **muted brass** `#7e6832` `#8f7638` `#a3873f`, **reflector aluminium** (inside shades only) `#6b6e6a` `#7c7f79` `#8a8c86`, **unlit glass** smoky `#3a3d3f`, **glow undyed** `#d99a4a` core and `#c47f3c` edge, High-Bay warm-up `#8a2f22` → `#d9883a`, dyed glows as group 1. Hanging cable: dark covered flex `#2b2c2e` `#353638`, whatever the Power Line used (it's the lamp's flex).

Industrial lights sit beside Create's andesite and train casings; domestic ones beside dark walnut, the Cuckoo Clock and brass trims.

## Shape Language
**Circles for domestic, squares for industrial.** Sconce, Flush, Pendant and Chandelier are round (stepped discs, bells and domes); Bulkhead and Cage are rounded rectangles and cylinders behind straight bars; the High-Bay is a big round dome on square hardware.

## Silhouette Description
- **Bulkhead:** a flat rounded-rectangle puck on the wall with a cross of bars over it: "a ship's porthole with a guard".
- **Wall Sconce:** a small plate, a short curl, an upturned bell: "a tulip on a wall".
- **Gooseneck:** a curved tube arching out and over, ending in a dish facing down: "a reading lamp for a shop sign".
- **High-Bay:** a rod, a finned box, a wide bell: "an upside-down bowl the size of the block".
- **Cage Lamp:** a glass cylinder inside four bars from a round plate: "a jam jar in a cage".
- **Flush Ceiling Light:** a thin disc tight to the ceiling: "a lit coin".
- **Pendant:** a cone on a long thin flex: "a dunce cap hanging on a thread".
- **Chandelier:** a hub with a ring of arms, each with a small bulb: "a brass spider of lights".

## Primary Forms
**Bulkhead Light** (wall): a dark steel body, 8 px wide × 6 px tall × 3 px deep with corners stepped off (rounded rectangle), a 1 px rim. The lens is 6 × 4 px of glow, recessed ½ px. Over it a **guard**: two vertical 1 px bars and one horizontal, in cast iron, standing ½ px proud. Four screw pixels at the rim's corners.

**Wall Sconce** (wall): a brass **backplate** 3 × 5 px, ½ px deep, rounded ends; a 1 px brass **arm** from its middle curling down, out and up (stepped), 4 px out; a **shade** like an upturned bell (rings 3, 4, 5 px, 4 px tall, open at the top), frosted glass that **glows all over**, with a 1 px brass **rim** at its lip. Light goes up the wall.

**Gooseneck Lamp** (wall): a 4 × 4 px dark steel **wall plate**; a 1.5 px tube leaving it level, curving up and over in a stepped arc (rising 4 px, reaching 9 px out) and down into a **dish shade** 7 px across (rings 7, 5, 3 px, 3 px tall), dark steel outside, reflector aluminium inside, with a **glow disc** 3 px across under its centre.

**High-Bay Lamp** (ceiling): a 2 px **rod** 3 px long from a 4 px ceiling plate; a **driver box** 5 × 5 × 3 px with three 1 px fins on each side; then the **dome**, a bell 12 px across at the mouth (rings 6, 9, 11, 12 px over 6 px of height), dark steel outside with a 1 px rolled **rim**, reflector aluminium inside, and a 4 px **glow** lamp at its centre with the inside of the bell lit at the glow's edge colour. The biggest, brightest light in the set.

**Cage Lamp** (ceiling): a 6 px round dark steel **base plate** 1 px deep; a **glass cylinder** 4 × 4 px (plus-shaped, round read), 5 px tall, **glowing**; four 1 px cast-iron **cage bars** from the plate down the sides, meeting a 1 px **bottom ring** and a small cap.

**Flush Ceiling Light** (ceiling): a round dish 10 px across (stepped disc), 1.5 px deep, its underside **frosted glow** 8 px across, held by a 1 px **brass rim** ring.

**Pendant Lamp** (hanging): a 3 px dark steel **ceiling rose** ½ px deep; a 1 px **flex**; a **shade**: a cone in dark steel (rings 2, 4, 6, 7 px over 4 px), reflector aluminium inside, with a 3 px **bulb glow** at its mouth showing below the rim. Lowered: the rose stays on the ceiling, each block of drop is a 1 px flex segment, the shade at the bottom block.

**Chandelier** (hanging): a 3 px brass **ceiling rose**; a 1 px **flex**; a brass **stem** 2 px across, 6 px tall, with a 3 px **knop** (ball) halfway and a small **drop finial** under the hub; a **hub ring** 4 px across; **eight arms** (four on the axes, four at 45°), 1 px brass, curving out and up 6 px, each ending in a 2 px **cup** holding a **candle bulb** (1 × 2 px glow). 14 px across. Lowered like the Pendant. Not waterloggable.

**Hanging cable segment** (shared by Pendant and Chandelier): a 1 px dark flex down the block's centre, no collision, no outline beyond a thin pick box.

## Secondary Forms
- The Bulkhead's guard and screws, the High-Bay's fins and rod, the Cage's bars: protection and heat, the language of industrial light.
- The Sconce's brass curl and rim, the Chandelier's knop and finial, the Pendant's rose: domestic care, sparing brass.
- Reflector aluminium only inside shades, so a lit lamp shows a bright inner surface and a dark outside.

## Interesting Features
- **Up, down, out.** The Sconce lights up the wall, the Gooseneck and Pendant down, the Bulkhead out. Different jobs, visible in the shape.
- **The High-Bay's warm-up** fills a factory with red that climbs to amber.
- **Pendants at different drops** over a long table or stairwell, each lowered by hand, a block at a time.
- **The Chandelier's ring of eight small bulbs**: the only multi-bulb light, made for halls and stations.

## Rig Requirements
No animation. Parts the code needs:
- **Glow part per light**, as its own model (lens, bell, cylinder, disc, bulbs), so the renderer can draw it emissive, dye-tinted, dimmed and flickered. The High-Bay's glow part includes its lit inner bell rim.
- **Hanging cable segment** as a separate block model.
- Everything else baked, with facing variants (wall lights by wall side; ceiling lights fixed down; Pendant and Chandelier always hang down).

## Fun Score (1-10)
**8.** Eight lights with clearly different jobs and silhouettes; the big High-Bay warming up and pendants lowered to any height are the moments, and the Chandelier gives grand rooms a centrepiece.

## Builder Notes
- **Keep sizes honest:** the Flush light is the thinnest thing in the set (1.5 px); the High-Bay the widest (12 px). Wall lights stay within 3 px of the wall except the Gooseneck.
- **Pitfalls:** reflector aluminium only inside shades, never on the outside. Brass stays trims on everything except the Chandelier, where it's the material but still muted (no `#c9a75a` highlights over large areas). Cage bars 1 px, not 2. Don't make the Bulkhead's guard a grid; two vertical bars and one cross bar.
- **Screenshot for review:** a dim interior scene: a tunnel wall with three Bulkheads; a factory bay with a row of High-Bays (one mid warm-up) and a Cage Lamp; a room with a Wall Sconce, a Flush light and a Gooseneck over a door; a table with three Pendants at drops of 1, 2 and 3; a hall with a Chandelier lowered 2 blocks; a row of Flush lights in five dye colours. Plus close-ups of each, lit and unlit.
