# Design Brief: Geothermal Turbine

Redesign of an existing multiblock. What the code does today is in `design/dossiers/geothermal-turbine.md`. Coordinates are in the formed model's frame: north-facing, the core at 0–16, the 3×3×3 from −16 to 32, front face at z = −16, and −x is on the viewer's right.

## Hard Requirements

From the user:
- "Looks bad, blocky, just baaaad." The formed machine must not read as a box.
- Scope: "Redesign + small code". A new formed machine and new unformed blocks, a real turbine rotor that spins faster under load, and glows that only light while it runs. The structure, ports and rates stay the same.
- "Turbine only, own textures". The Lava Generator and Electric Motor don't change now; they can be redesigned to match later.
- Glows: muted amber for the heat and lava, with a single small cyan light for its electrical output.
- Show: a **spinning rotor**, the **energy buffer**, a **full / stalled** tell, and the **steam** stage.
- House style: dark muted Create palette, light areas mid-grey at most, crisp bevelled panels (not blotchy mottle), accents as trims. Previewed in Blockbench first.
- Nothing on `design/hands-off.md` changes.
- "Once the redesign is done let's [get] the sound designer on it": once the model is approved, it gets a sound pass (`character-sound-designer`, Stage 5), cued from the States below.

From the code:
- `ElectricRenderers.java:31-34` loads `block/geothermal_turbine`, `_glow`, `_gauge_lava` and `_gauge_water` by name. Those stay; new part models are added beside them.
- The formed machine is drawn by the controller from the middle block's origin, north-facing, and **must fit −16..32 on every axis** (the vanilla model limit; `ElectricRenderers.java:133-135`).
- Blockstates `geothermal_casing`, `geothermal_core` and `geothermal_controller` (16 variants, y-rotated by facing) keep their model names. Item models parent them.
- The controller sits in the front-middle cell (x0–16, y0–16, z−16–0); goggles and buckets work only there, so the control panel belongs on it.
- Every face of every casing and the controller is a lava, water and FE port; the look must not suggest only one inlet.
- Gauge fills scale in Y only from a fixed bottom (today y = −3) and are drawn full-bright.
- Everything renders cutout, lit by one light value sampled in front of the controller. No translucency.
- All three blocks are full cubes, formed or not.
- Particle anchors are in Java (`GeothermalControllerBlock.java:104-116`): steam at y 31.2 over x/z 3–13, vent flames at x −14.8 / 30.8, y 2.75 / 6.75 / 10.75. They move with the design (see code changes).

## Asset Type

Multiblock, 48 × 48 × 48 px formed, plus three 16 px unformed blocks. `java_block` models; the formed machine is one static model plus renderer-drawn parts (rotor, needles, lamps, glow, gauge fills).

## Family

The **electric family** (Lava Generator, Electric Motor, Geothermal Turbine), and the **heat family** (anything burning lava). The turbine becomes the reference for both: crisp blue-grey steel panels with andesite trim (the process-box panel style), muted amber heat, copper for windings and pipes, brass for bezels only, and one cyan light that means "making power". The motor and generator can take this language later; nothing of theirs changes now.

## Fantasy

A power station in a box: a lava firebox boils water in a riveted boiler drum, the steam roars through a turbine you can watch spinning behind its window, and a copper generator turns it into power, with a stack puffing steam on the roof.

## Hero Element

**The turbine rotor**: a long bladed rotor, three rows of blades on a shaft, seen through a wide window in the turbine housing across the top front. It spins about its length while the machine runs, faster under load, and coasts to a stop when it doesn't. It's the mechanic the name promises and the thing a player will stand and watch.

## Theme

| Part | Material | Colours |
|---|---|---|
| Plinth, firebox, frames | crisp dark blue-grey steel panels, andesite edge trim, bevels | `#2f343b` `#3c424a` `#4a5059`, trim `#6b6e6a` `#7c7f79` |
| Boiler drum | darker steel with riveted andesite bands | `#2f343b` `#363b42`, bands `#6b6e6a` |
| Turbine housing | steel, with a brass-rimmed window | steel as above, rim `#8f7638` `#b08f45` |
| Rotor blades | light steel blades on a dark shaft | `#4a5059` `#5f656e`, shaft `#2a2f35` |
| Generator | copper windings in a steel cage | `#7a4a2e` `#9c5f3a` `#b87a4e` |
| Pipes | copper with andesite flanges | as above |
| Firebox glow (running) | muted amber and warm orange behind a dark grate | `#d99a4a` `#c76a2a`, grate `#1f2327` |
| Output lamp (running) | the one cyan | `#6fb7c4` |
| Stall lamp (full) | dark red | `#8a2f22` |
| Lava gauge | own muted lava, not vanilla's near-white | `#c76a2a` `#d99a4a` `#8a2f22` |
| Water gauge | muted blue-grey water | `#3f5d6e` `#4f6f80` |
| Dials | andesite face, dark needle, brass bezel | `#6b6e6a`, `#1f2327`, `#8f7638` |

No weathered or exposed copper (the salmon pink and teal verdigris go), no rust stains, no near-white anywhere.

## Shape Language

**Circles over squares.** The machine is read through its round parts: the boiler drum, the turbine housing, the generator, the dials, the stack. The square plinth and firebox are the ground they sit on. Drums are 16-sided, built from boxes turned 0°, 22.5° and 45°.

## Silhouette Description

From the front (48 × 48): a low, wide firebox on a plinth across the bottom third, the control cabinet standing at its centre; behind and above it, the round end of the boiler drum; across the top, a long round turbine housing with a wide window full of blades; a short stack on the roof; the generator's copper drum on the viewer's left end. From the side: three stepped masses (cabinet, drum, housing) instead of one wall. It should read as "a machine with a turbine on top", never "a crate".

## Primary Forms

- **Plinth**: x −16..32, z −16..32, y −16..−13, full footprint (the structure's collision is a full cube).
- **Firebox**: x −14..30, z −10..30, y −13..2, a heavy steel box with grate windows on its east, west and back faces.
- **Boiler drum**: a 16-sided cylinder along X, axis at y 6, z 18, radius 11, from x −12 to 28, with stepped end caps to x −14 / 30.
- **Turbine housing**: a 16-sided cylinder along X, axis at y 20, z 2, radius 10, from x −8 to 24, sitting on two bearing pedestals at its ends.

## Secondary Forms

- **Control cabinet** on the controller cell: x −6..22, y −13..14, z −16..−10, with the panel facing front.
- **Generator**: a copper-wound drum on the turbine's east end (x 24..30), in a steel cage, carrying the cyan output lamp.
- **Steam pipe**: from the boiler drum's top up into the turbine housing.
- **Exhaust stack**: a short round stack on the housing top, x 5..11, z 4..12, rising to y 31 (where the steam spawns).
- **Bearing pedestals** under the housing ends, with the rotor shaft's ends showing.

## Interesting Features

- The window shows the rotor's blade rows turning, so the machine's speed is visible from across a room.
- The control panel holds four readouts in brass bezels: lava and water sight glasses, an energy buffer dial and the two lamps.
- A steam pressure dial sits on the boiler drum, between heat and turbine.
- Firebox grates glow amber only when it's running; off, they're dark iron.
- Copper pipe flanges on each side face read as ports, since every face is one.

## States

| State | What changes visually | Carried by |
|---|---|---|
| Unformed | Three blocks that read as machine parts: a panelled casing, a caged rotor (core), a control panel (controller) | blockstate models (new geometry) |
| Formed, idle | Grates dark, rotor still, lamps off, needles at rest | static model |
| Running | Grates glow amber; rotor spins, speed from output; cyan output lamp lit; steam dial up | **renderer, new**: rotor, glow only when lit, lamp, needle |
| Load | Rotor speed and steam needle follow the smoothed output (0 to 46,080 FE/t) | renderer, new |
| Energy buffer | Dial needle sweeps 0–270° with the buffer (0–4,000,000 FE) | renderer, new |
| Full / stalled | Dark-red stall lamp lit, rotor coasts to a stop, grates dark | renderer, new |
| No lava / no water | That sight glass empty; machine dark | existing gauges |

## Rig Requirements

Renderer-drawn part models, each modelled in the formed frame where the renderer draws it:
- `geothermal_turbine_rotor`: shaft along X, pivot at **(y 20, z 2)**, spins about X.
- `geothermal_turbine_needle_energy`: on the panel's energy dial, pivot at the dial centre, turns about Z.
- `geothermal_turbine_needle_steam`: on the boiler dial, pivot at its centre, turns about its face normal.
- `geothermal_turbine_lamp_output` (cyan) and `geothermal_turbine_lamp_stall` (dark red): drawn full-bright only in their state.
- `geothermal_turbine_glow`: the amber grate glow, drawn **only when running** (today it's drawn always and only changes brightness).
- `geothermal_turbine_gauge_lava` and `_gauge_water`: the fills, bottoms at the sight glasses' feet.

## Sound

See `design/sound/geothermal-turbine/geothermal-turbine.md` (signature sounds, events, cues).

## Fun Score (1-10)

9. The biggest machine in the mod finally looks like one: a player builds a cube of casings, it snaps into a power station, and they watch the turbine wind up as the lava goes in.

## Builder Notes

**Start from scratch.** Export to the existing names plus the new part models; new textures under new names (`geothermal_turbine_*`), so the Lava Generator and Electric Motor keep `brass_weathered` and `electric_glow`. Retire `turbine_skin_*` and `water_glow` once nothing uses them.

**Build big faces from block-sized pieces** so textures keep their 16 px density (a Java face's UV can't exceed one texture).

**Code changes the design needs** (small, for the implementation stage):
1. **Rotor**: a client-side angle on the controller block entity, advanced each tick by the smoothed output (about 30°/tick at full output), easing down when it stops. Drawn about (y 20, z 2) on X.
2. **Glow only when running**: draw `geothermal_turbine_glow` only while `lit`.
3. **Needles**: energy dial from the synced buffer; steam dial from the smoothed output.
4. **Lamps**: cyan while running; dark red when it's formed, has both fluids and isn't running (the "full" stop; derivable on the client from data it already has).
5. **Gauges**: if the sight glasses move, update the fill bottom (`ElectricRenderers.java:148, 150`).
6. **Particles**: vent flames out of the firebox grates and steam out of the stack (`GeothermalControllerBlock.java:104-116`).

**Must not lose**: the −16..32 envelope; the panel on the controller cell; readable from all four sides (it turns with the controller); a full-looking footprint so pipes meet something at each face.

**Pitfalls**: Java rotations are 22.5° steps on one axis, so drums are 16-gons from 0/±22.5/45° boxes. One light value lights the whole machine, so bake shading into the textures (darker undersides, lit tops). Cutout only: glass is clear plus a few grey glints.

**Screenshot for review**: front, three-quarter front and back, both sides, top; the rotor close up; idle vs running vs full; the three unformed blocks; each beside its "before" shot from `design/dossiers/geothermal-turbine/`.
