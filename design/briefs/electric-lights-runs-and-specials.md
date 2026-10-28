# Design Brief: Electric Lights, Runs, Ground and Specials (group 3)

Group 3 of the Electric Lights set ([spec](../specs/electric-lights.md)): **Light Strip**, **Edge Light**, **Festoon Lights** (runs); **Bollard Light**, **Floor Light** (ground); **Floodlight**, **Warning Beacon**, **Indicator Lamp** (specials); and the **Light Switch**. Groups 1 and 2: [street](electric-lights-street.md), [indoor](electric-lights-indoor.md).

## Hard Requirements
- "Strip lights, edge lights and anything else you can think of", "Create styled but fit each scenario they are meant for", "fairly cheap to make", "interesting, varied and useful" (user, 2026-10-28).
- "Make a light switch block for right clicking" (user R1): **inline on the cable**, a small wall-plate switch a Power Line runs through (D15).
- Runs (D11, P6, P7): **Light Strip on any face, one per face, joining neighbours and turning corners like a Power Line**; **Edge Light along any edge of a block, picked by where you click, several edges per block**; one feed lights the whole run.
- **Festoon Lights hang between any two Lamp Posts, Line Posts or solid block faces (a small hook appears on a wall), up to 16 blocks, like a span** (D20); their light is invisible spots every 4 blocks (P13).
- **Floodlight: paired to a target; it turns to face it**, with a faint visible cone (D18).
- **Warning Beacon: a rotating light that spins while powered** (D16).
- **Floor Light: a full, walkable block set flush in the floor; waterloggable, doubles as a pool light** (P6, P12). **Bollard: on a floor.** **Indicator Lamp: any face.** **Warning Beacon: floor, wall or ceiling.**
- The Warning Beacon is crafted with a **cogwheel** ("it turns") (D26, P29).
- Dye colours **only the glow**, muted; **undyed is warm incandescent** (D13). Flicker on power loss is the renderer's (P3, P28).
- House rules as groups 1 and 2: dark, muted, glows never near-white, accents sparingly, preview in Blockbench first.

## Asset Type
Blocks (9). 16 px = 1 block. Strips and edges are thin face- or edge-attached pieces with connection states. The Festoon is a span drawn by the renderer plus a wall-hook block. The Floodlight and Warning Beacon have moving parts.

## Fantasy
The finishing touches of a lit Create base: lines of light along a corridor and around the roofline, bulbs strung over the market, glowing paving at the station edge, a floodlit yard, an amber beacon turning at the level crossing, and a panel of indicator lamps beside the switches that run it all.

## Hero Element
**Light as line and motion.** The runs draw lines of light across a build (strips, edges, festoon strings); the Warning Beacon's turning reflector and the Floodlight's aimed head are the two lights that move. Each special has one unmistakable part: the Floodlight's square lens on its yoke, the Beacon's amber dome, the Indicator's single glowing dot.

## Theme
Palette as groups 1 and 2, plus **channel aluminium** for strips and edges `#5f625f` `#6b6e6a` `#787b76` (mid grey at most) and **amber glass** for the undyed Warning Beacon (its undyed glow is the warm `#d99a4a`, a darker amber `#a8682c` where the dome is unlit). Festoon flex: dark `#2b2c2e`. Floor Light frame: dark steel; its sides read as Create casing when exposed.

## Shape Language
**Lines** for the runs (thin, continuous, no bulk). **Squares** for the Floodlight, Floor Light and Switch (equipment). **Circles** for the Beacon dome, the Bollard and the Indicator.

## Silhouette Description
- **Light Strip:** a thin lit line on a surface, turning corners: "a neon pencil line".
- **Edge Light:** an outline traced along a block's edges: "a lit picture frame on the building".
- **Festoon:** a sagging string with round bulbs hanging from it: "a necklace across the street".
- **Bollard:** a knee-high stub with a glowing band under a cap: "a lit fire hydrant without the arms".
- **Floor Light:** a glowing square set in the ground: "a lit paving stone".
- **Floodlight:** a square lamp on a U-yoke, tilted to its target: "a stage light".
- **Warning Beacon:** a squat base under an amber dome: "a siren light".
- **Indicator Lamp:** a dot in a ring: "a lit button".
- **Light Switch:** a small plate with a lever: "a light switch".

## Primary Forms
**Light Strip** (face-attached, like a Power Line): an aluminium **channel** 2 px wide, 1 px tall, laid along the block's middle; on top, a **frosted diffuser** 1 px wide that **glows**. It joins neighbours straight, in L and T junctions, and round inside and outside corners exactly as Power Line wires do. A run's open end gets a 1 px **end cap** in aluminium. One strip per face.

**Edge Light** (edge-attached): an **angle profile** that sits in the corner where two faces meet, 2 × 2 px in cross section along the full 16 px edge, aluminium, with its outer **chamfer** (a 1 px diagonal strip along the edge) **glowing**. Where three edges meet at a block corner, the pieces **mitre** into a small corner block. Open ends get end caps. Several edges per block.

**Festoon Lights**: the **string** is a dark flex, as thin as a span (about 0.6 px), sagging 1/12 of its length (more than a power span, as festoons droop). **Bulbs** every 2 blocks: a 1 px dark **socket** hanging 1 px under the string and a 2 px round **bulb** (glow) under it. **Wall hook** (block, on any solid face where the string ends): a 2 × 2 px iron plate with a 1 px **eye** ring the string ties to. At posts, the string ties to the insulator or bracket like a span. Item: a short coil of flex with three bulbs.

**Bollard Light** (floor): a dark steel post 5 × 5 px (corners stepped for a round read), 10 px tall, on a 7 × 7 × 1 px **base flange** with two bolts. Near the top, a **louvred light band**: three 1 px horizontal **louvres** with 1 px gaps of **glow** behind them, all round. A 6 px **domed cap** above.

**Floor Light** (full block, flush): the top face is a **frosted glow panel** 10 × 10 px in a 3 px dark steel **frame**, with a 1 px **anti-slip cross** (two thin dark lines) over the glass and four screw pixels at the frame's corners. The sides read as Create **andesite casing** (they show only at an edge or a pool wall). Waterlogged, it's a pool light.

**Floodlight** (floor, wall or ceiling): a 6 × 6 × 1 px **mount plate**; a **turntable** 4 px round on it; a **U-yoke**, 1 px arms 9 px apart, rising 6 px, with muted brass **pivot knobs** outside; the **lamp head** between them on pins: a **round can** 8 px across and 6 px deep in dark steel (revised in the build, 2026-10-28: the square body read as a TV), a reflector-aluminium **bezel**, a 6 px round **lens** (glow), a 1 px **visor** over the front, three **cooling rings** round the back half and a small back box. The faint visible cone is a renderer effect.

**Warning Beacon** (floor, wall or ceiling): a dark steel **base** 6 × 6 × 2 px; an **amber glass dome** 5 px across, 5 px tall (stepped: 5, 5, 4, 3 px), with a 1 px cast-iron **guard** of two crossed bars over it; inside the dome, visible through the glass, a **reflector**: a 1 × 3 × 3 px mirror plate beside a 1 px bulb, on a spindle with a tiny **brass cog** (3 px) at its foot, the part the cogwheel recipe points to. The dome's **glow** brightens on the side the reflector faces (the renderer sweeps it).

**Indicator Lamp** (any face): a 4 × 4 px dark steel **bezel**, 1 px deep, with a 1 px muted brass **ring**, and a 2 × 2 px **domed lens** standing 1 px proud, **glowing**. Tiny on purpose; a row of them reads as a control panel.

**Light Switch** (any face): a **plate** 6 × 8 px, 1 px deep, in andesite casing grey (`#6b6e6a`), with two screw pixels; a **toggle** 1 × 3 px with a 1 px muted brass tip, standing out of a 2 × 2 px **boss**, tilted up for on and down for off. Where the Power Line enters and leaves, a 2 × 1 px **gland** on the plate's edge (one per connected side).

## Secondary Forms
- Strips' and edges' aluminium channels and end caps, the Festoon's sockets and hook, the Floodlight's fins and visor, the Bollard's louvres, the Floor Light's screws: each a real part.
- The Beacon's visible cog inside the dome links its look to its recipe.
- The Switch's glands show where the circuit passes through.

## Interesting Features
- **Lines of light that turn corners.** A strip down a corridor, up a wall and across the ceiling, all one run, fed once.
- **Outlines.** Edge Lights trace a building's roofline or a platform edge in light.
- **A turning beacon.** The only light that moves on its own; the reflector sweeps the dome, and the brass cog turns.
- **An aimed Floodlight** that visibly swings to its target when paired.
- **A switch you can see flip.** The toggle snaps up and down.

## Rig Requirements
- **Floodlight:** `yoke` turns about the mount's normal (pivot at the turntable centre); `head` tilts about the yoke pins (pivot at the pin axis, 5 px above the plate). The renderer sets both from the target; turns ease over about half a second when re-paired.
- **Warning Beacon:** `rotor` (reflector, bulb, spindle and cog) turns about the dome's axis (pivot at its centre, on the base's top), about one turn per second while lit. Flywheel instance.
- **Light Switch:** `toggle` rotates about the boss centre, ±30°, snapping over 2 ticks.
- **Glow parts** for every light (diffusers, chamfers, bulbs, band, panel, lens, dome, dot), so the renderer can draw them emissive, dye-tinted, dimmed and flickered.
- **Connection states:** Light Strip and Edge Light pieces as part models per connection, like Power Line wires; the Festoon string and bulbs on the renderer, as spans are.

## Fun Score (1-10)
**8.** Lines of light that turn corners and trace outlines make whole buildings glow, festoons make a market feel alive, and the turning beacon and swinging floodlight are small mechanical shows. The switch gives the player a satisfying flick.

## Builder Notes
- **Thinness matters:** strips and edges must stay 1–2 px so a lit corridor reads as lines, not pipes; check them beside a Power Line wire (about 1–2 px) so they read as relatives.
- **Pitfalls:** don't make the Beacon dome saturated orange; it's muted amber glass, darker where unlit. Don't let the Floor Light's frame dominate; the panel is the point. Indicator stays 4 px; resist making it bigger. The Floodlight is the biggest special, but keep its head 8 px, not a full block.
- **Screenshot for review:** a night scene with a corridor lit by a Light Strip turning up a wall and across the ceiling; a building outlined in Edge Lights; a Festoon between a Lamp Post and a wall hook; a path of Bollards; a station platform edge of Floor Lights (and one in a pool); a Floodlight on a wall aimed down at a yard; a Warning Beacon at a level crossing (three frames of its turn); a control board with a Light Switch (on and off) and a row of Indicator Lamps in dye colours. Plus close-ups of each, lit and unlit, and strip and edge corner joins.
