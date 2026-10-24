# Design Brief: Pressure Crucible

Redesign of an existing block. What the code does today is in `design/dossiers/pressure-crucible.md`. Family: the heat-and-lava blocks (Magma Engines, Lava Generator, Geothermal Turbine).

## Hard Requirements

From the user:
- Scope: "Redesign + small code". New look and press parts; recipes, timing and behaviour unchanged.
- A **screw press**: the shaft turns gears that drive a big threaded screw, very Create, very dynamic.
- Show **heat from the burner** (the bowl glows to match the Blaze Burner underneath: dark when cold, amber when heated, soul-blue when superheated, nothing without a burner) and **press progress** (a pressure gauge whose needle climbs as each item is pressed and drops back after).
- **Square** where it's not turning: "round should be used for motors and engines, some things are good to be square". The frame is square; only the screw, the gears and the gauge are round.
- Keep the back shaft: four horizontal facings, the shaft through the back wall, the open front as the working side.
- The house style: dark muted Create palette, crisp bevelled panels, accents as trims, never large saturated yellow (today's bright brass casing goes). Previewed in Blockbench first.
- Nothing on `design/hands-off.md` changes; in shared files only the crucible's lines.

From the code:
- Blockstates load `block/pressure_crucible` for `facing` (4), modelled with the **back (shaft) to the south** and the open front to the north (`blockstates/pressure_crucible.json`).
- The shaft stub `block/pressure_crucible_shaft` at the back (x 6–10, y 6–10, z 12.5–16), spun about the world axis (`PressureCrucibleRenderer.java:55-65`).
- The press plate's underside travels from y 11 (top) to y 4.5 (bottom), 2 px thick, following `getPlate` (down over exactly one item's press time, then easing back up) (`:34-38`, `PressureCrucibleBlockEntity.java:271-287`). Keep these heights so the timing and the contents still fit.
- The contents lie flat at y 4.05, 0.3 scale, in the middle of the bowl (`:82-91`); press particles at y 0.35 of the block (`PressureCrucibleBlockEntity.java:125`).
- Heat is read from the block below the way a Basin reads it (`getHeatLevel`, `:160-163`): available on the client from block states, so the glow needs no sync.
- Progress `getProgress(partialTicks)` is already smooth on the client (`:258-269`).
- Full-block shape, `noOcclusion`, no light emission today; funnels and pipes from any side.

## Asset Type

Block, 16 × 16 × 16 px, `java_block`, horizontal facing. A static model plus renderer-drawn moving parts.

## Family

The heat-and-lava set: crisp blue-grey steel with andesite trim, brass only on bezels and bolts, muted amber heat, and here soul-blue for superheated (Create's superheated burner colour, muted). The crucible is the family's **heavy tool**: square and solid like a machine-shop press, where the engines and generators are round.

## Fantasy

A cast-iron screw press over a fire: the shaft turns a big gear on top, the screw winds the platen down onto the dust, the gauge needle climbs, and *crunch*: a gem.

## Hero Element

**The screw and its drive**: a big threaded screw standing in the middle of the frame, turning with the shaft, topped by a large spur gear on the crosshead that a small pinion at the back turns. Both gears spin visibly from above and the side. The heavy platen rides down the screw onto the dust in the bowl as each item is pressed.

## Theme

Steel `#2f343b` `#3c424a` `#4a5059`, andesite trim `#6b6e6a` `#7c7f79`, polished steel for the screw threads `#5f656e` `#7a8089`, dark aged brass `#8f7638` `#b08f45` on the gauge bezel and a few bolts only. Heat: amber `#c76a2a` `#d99a4a` when heated, muted soul-blue `#4f86b0` `#6fa7cf` when superheated, dark when cold. No bright brass casing, no saturated yellow, no blotchy mottle.

## Shape Language

A square frame: a heavy bed, four square corner posts, a square crosshead, a back gearbox. Round only where things turn: the screw, the gears, the gauge.

## Silhouette Description

From the front: a heavy bed with a square bowl (die) in it, two corner posts, the crosshead across the top with the gauge on its face, the screw coming down through the middle to the platen, and the big gear on top. From the side: the same frame open between the posts, the gearbox standing up the back wall to the pinion on top. From above: the big gear and the small pinion meshing on the crosshead.

## Primary Forms

- **Bed**: x 0–16, y 0–3, with a square die bowl in the middle (floor at y 4 for the contents at 4.05), andesite trim.
- **Corner posts**: four square steel posts, 2.5 px, y 3–12.
- **Crosshead**: a square steel slab across the top, y 12–14.
- **Back gearbox**: a steel housing up the middle of the back wall (where the shaft goes in), y 3–14, carrying the drive up to the pinion.

## Secondary Forms

- **Screw**: a threaded square rod about 3 px across at the centre, from the platen up through the crosshead, turning with the shaft.
- **Big gear**: a spur gear about 9 px across on top of the crosshead, on the screw.
- **Pinion**: a small gear about 4 px across at the back of the crosshead, meshing with the big gear, turning twice as fast the other way.
- **Platen**: a heavy square plate (about 9 × 9, 2 px thick) with a nut boss round the screw, riding down it.
- **Pressure gauge**: a round dial with a dark brass bezel on the crosshead's front, its needle climbing with progress.
- **Heat glow**: slots round the bowl and under the die that glow with the burner's heat.

## Interesting Features

- Everything that turns is driven from the shaft: stop the rotation and the gears and screw stop.
- The bowl tells you at a glance whether the fire under it is right: dark, amber or soul-blue.
- The gauge and the platen both show how far along the current item is.

## States

| State | What changes | Carried by |
|---|---|---|
| No burner or cold | Heat slots dark | renderer (no glow part) |
| Smouldering | Faint dark-red glow | renderer glow part |
| Heated (kindled) | Amber glow | renderer glow part, full-bright |
| Superheated (seething) | Soul-blue glow | renderer glow part, full-bright |
| Turning | Gears and screw spin with the shaft | renderer |
| Pressing | Platen rides down the screw over one item's time; gauge needle climbs; both ease back after | renderer (`getPlate`, `getProgress`) |
| Contents | Flat items in the bowl (as today) | renderer |

## Rig Requirements

Renderer parts, modelled with the back to the south:
- `pressure_crucible_screw`: the screw and the big gear, spinning about Y through (8, 8).
- `pressure_crucible_pinion`: spinning about Y through its own axis at the back, twice as fast the other way.
- `pressure_crucible_ram`: the platen (replacing Create's press head), moved down by `getPlate`.
- `pressure_crucible_needle`: the gauge needle, turned about the dial's centre by `getProgress`.
- `pressure_crucible_heat_smouldering`, `_heat_kindled`, `_heat_seething`: the glow, one drawn by the burner's heat.
- `pressure_crucible_shaft` stays; `pressure_crucible_rod` goes (the platen rides the screw instead).

## Sound

A sound pass can follow once the model is approved: a gear whirr while turning, a creaking screw, and a deep crunch when an item is pressed (replacing Create's press sound), quiet while running.

## Fun Score (1-10)

9. A proper screw press with spinning gears, a needle that climbs and a fire you can read: it finally looks heavy enough to make diamonds.

## Builder Notes

**Start from scratch** on the family kit (`process-box.lib.js`), in its own generator `pressure-crucible.gen.js` (the electric and engine generators are hands-off; copy helpers rather than importing them). Export the static model, the renderer parts and a full item model, with new `pressure_crucible_*` textures; the borrowed Create brass casing, gearbox and press head textures go.

**Code changes** (small): the renderer draws the screw, gears, platen, needle and heat glow from the existing state; `pressure_crucible_rod` and its scaling go; press particles from the die's height. Nothing else changes.

**Must not lose**: the back shaft, the open front, the plate heights and timing, the contents' position, the full-block shape, no z-fighting (overlays 0.25 px apart), cell-relative UVs, no exposed missing faces.

**Screenshot for review**: front, three-quarter front and back, side, top; cold, heated and superheated; the platen at the top and bottom of its travel; with the Magma Engines for the family read; beside the before shots.
