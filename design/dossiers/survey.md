# Redesign Survey (2026-10-24)

Scores are 1–5. **Gap** is how far the look is from what the block does. **Presence** is how much players see it. **Fit** is how much art direction could add. The total is the sum of all three. Effort is listed but not scored.

| Rank | Block | What it does | Look today | Hidden states | Gap | Presence | Fit | Total | Effort |
|---|---|---|---|---|---|---|---|---|---|
| 1 | **Auto Harvester** (`auto_harvester`) | Sealed planter. A gantry head plunges, sweeps and cuts a crop or tree, over and over | Shared windowed box. The moving parts are stand-ins: the carriage is one casing cube, the reel is 3 flat planes, the saw is a flat square | Blocked/full (synced, never drawn), idle vs waiting, Bountiful | 4 | 4 | 5 | 13 | medium |
| 2 | **Pressure Crucible** (`pressure_crucible`) | Kinetic press over a Blaze Burner that turns 9 dust into gems and scrap | Brass-casing slabs, 4 pillars with 8 bright brass bands, an ember bowl that always glows, Create's press head borrowed | Heat needed vs present, too cold, no rotation, the press hit | 3 | 4 | 5 | 12 | medium |
| 3 | **Geothermal Turbine** (casing, core, controller) | 3x3x3 multiblock: lava boils water to steam that turns a turbine, 46,080 FE/t | Formed: one muted 109-element box with gauges and vanilla-lava glow strips. Unformed: 3 flat icon-like cubes. 72 faces are textured `#missing` and may show as seams | The turbine never turns. Steam, buffer, full and no-water states aren't shown | 4 | 3 | 5 | 12 | large |
| 4 | **Electric Motor** (`electric_motor`) | Turns FE into rotation at a set speed, stress and direction | Octagonal can, about 28% brass bands, teal glow strips. Only a stock shaft moves | Stress setting, energy, reverse and short of power. Only `powered` shows | 3 | 4 | 4 | 11 | medium |
| 5 | **Cobble Generators** (andesite, brass and wireless, 20 ids with products) | Shaft-driven cobble, basalt, blackstone or tuff maker | Good insides (pours, rising block). The casing is bright brass, with saturated icons on top, yellow vent stripes on blackstone and near-white vents on basalt | Stored amount. Powered looks the same as unpowered. Nothing moves to show drill pacing | 3 | 5 | 3 | 11 | small–medium |
| 6 | **Incinerator** (`incinerator`) | Destroys items and fluids, and heats a boiler above while lit | Plain textured cube. Near-white lit grille, a whole saturated orange top. No renderer | Items going in, burn intensity, its boiler-heater role | 4 | 3 | 3 | 10 | small–medium |
| 7 | **Lava Generator** (`lava_generator`) | Burns lava for 1,024 FE/t | Muted box with an octagonal intake. The rotor is tiny. The lava slot always looks full | Tank level, energy buffer, full with nothing taking power | 3 | 3 | 4 | 10 | small–medium |
| 8 | **Flight Instruments** (`flight_instruments`) | Cockpit desk with 4 live gauges | Good dark panel. Near-white needles and a saturated sky (Java constants). Three flat colour-strip buttons that do nothing | Gauges are already live. The buttons are dead | 3 | 3 | 4 | 10 | small–medium |
| 9 | **Stone Infuser** (`stone_infuser`) | Cobble + 9 nuggets make Create's decorative stones | Andesite frame, raw brass-block stand, near-white glass glints. The cobble turns while the new stone creeps up it | The nuggets, the infusing itself, which metal | 3 | 3 | 4 | 10 | medium |
| 10 | **String Tangler** (`string_tangler`) | 4 string + slime make a cobweb | Copper-casing frame and glass. Spinning string ribbons, static hub | Slime, the web growing, the cobweb output | 3 | 2 | 4 | 9 | medium |
| 11 | **Animal Pens** (normal and brass) | Keeps and breeds one animal and makes its drops | Shared box shell. Lively mini animals inside. The brass version only swaps the side texture | Milk level, eggs, breeding countdown, Fruitful | 3 | 3 | 3 | 9 | small–medium |
| 12 | **Wellspring** (`wellspring`) | Self-filling tank for water, lava, honey or chocolate | Copper-framed glass cube. The fluid level reads well. The copper is too bright and salmon | Generating (needs a sync change), which upgrades are fitted | 3 | 3 | 3 | 9 | small–medium |
| 13 | **Upgraded Chest** (5 tiers) | Tiered stack and slot upgrades. Netherite is blast-proof | Vanilla planks. Tiers shown only by trim colour, most of it off-style | Extra slots, blast-proof | 3 | 4 | 2 | 9 | medium |
| 14 | **Compact Gyroscope** | Keeps an Aeronautics ship level, with a redstone brake | Brass casing, glass, a mini flywheel spinning | Levelling, brake clamping | 3 | 2 | 4 | 9 | medium |
| 15 | **Supercharged Magma Engine** | Double-tier lava engine, 128 RPM | Same as the base engine plus brass trim. The fallback keeps the saturated yellow cog | Its double speed (it pulses like the base), tank level | 3 | 3 | 3 | 9 | small–medium |
| 16 | **Magma Engine** | Lava engine, 64 RPM | With Aeronautics loaded: Simulated's Portable Engine shape (All Rights Reserved; only our texture is ours), on style. Fallback without Aeronautics: saturated gold cog, near-white glow | Tank level | 2 | 4 | 3 | 9 | small |
| 17 | **Upgraded Barrel** (5 tiers) | As the chest, in barrel form | Vanilla barrel retexture with tier-coloured bands | Tier is colour only | 3 | 3 | 2 | 8 | small |
| 18 | **Torque Rotor** | Propeller that turns a ship | Andesite base, pitched blades in pale near-white stone, solid brass-block tips | Thrust is shown only by spin direction | 3 | 2 | 3 | 8 | small–medium |
| 19 | **Pipe Cover** (blank) | The undressed base of a Pipez cover | Already muted, mottled steel | None | 2 | 1 | 1 | 4 | small |

## Recommended

1. **Auto Harvester**: the harvest motion already exists in the renderer. A real trolley on a rail, a reel with bats and tines, a toothed saw and a blocked/full tell would make it one of the mod's showpieces without new animation code.
2. **Pressure Crucible**: a heavier press of its own instead of Create's borrowed head, and a bowl that reacts to heat (cold, then amber, then superheated) would show the one rule players get wrong: whether it's hot enough.
3. **Geothermal Turbine**: it promises a steam turbine and nothing turns. A spinning turbine wheel and unformed parts that read as real machine parts would make the biggest machine in the mod feel like one. Also check the `#missing` faces.
4. **Cobble Generators**: the block players see most. The insides are good. Bringing the casing onto the house style, plus a part that moves with drill progress, changes 20 block ids at once.

## Families

- **Sealed process box**: Cobble Generators, Auto Harvester and Animal Pens use the same 9-element windowed shell, glass, floor, ceiling and back shaft. Redesign the shell once and give each its own interior.
- **Electric**: Lava Generator → Electric Motor → Geothermal Turbine share one texture kit (green-leaning steel, weathered brass, teal glows) and the octagonal-can motif. Restyle the kit once. The Wellspring feeds the turbine.
- **Heat and lava**: Pressure Crucible, Incinerator, the Magma Engines, the Lava Generator and the Geothermal Turbine all use lava or a Blaze Burner. They should share one muted-amber ember and firebox language.
- **Two-input machines**: String Tangler and Stone Infuser share a block class, renderer and back socket. Redesign them as a pair.
- **Tiered storage**: Upgraded Chest and Barrel share their tiers. Use one tier language, with fittings that grow in build per tier, not just in hue.
- **Ship hardware**: Compact Gyroscope, Torque Rotor and Flight Instruments are the Aeronautics set. They don't look alike today, and a shared casing would tie them together. None use Aeronautics' own models or textures.

## Left Out (already done)

Trade Bot and Cuckoo Clock, per `design/hands-off.md`.
