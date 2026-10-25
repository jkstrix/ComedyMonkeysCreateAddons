# Code Dossier: Flight Instruments

What the code does today. The Flight Instruments are one of the three "ship hardware" blocks (with the Compact Gyroscope and Torque Rotor) that exist only when Sable / Create Aeronautics is installed. "Shared with the ship hardware set" lists what the three have in common.

Paths: `J/` = `src/main/java/com/comedymonkey/createaddons/`, `S/` = `J/compat/sable/`, `A/` = `src/main/resources/assets/comedymonkeysaddons/`, `D/` = `src/main/resources/data/comedymonkeysaddons/`. Short names: `FIB` = `S/FlightInstrumentsBlock.java`, `FIBE` = `S/FlightInstrumentsBlockEntity.java`, `FIR` = `S/client/FlightInstrumentsRenderer.java`, `FDS` = `S/FlightDisplaySources.java`, `AC` = `S/AeronauticsContent.java`, `AS` = `S/client/AeronauticsScenes.java`. Model elements are cited by index (`#n`) and the line their `"from"` is on. Panel coordinates are in sixteenths of the panel: u to the right, v down from the top edge, as the renderer uses them (`FIR:32-33`).

## Identity

- Registry id `flight_instruments`. Single block, not kinetic, 1x1x1 (a sloping desk, 11 px deep).
- Block `FIB` (`BaseEntityBlock`, `IWrenchable`, `FIB:33`), registered `AC:51-58`. Block entity `FIBE` (plain `BlockEntity` + `IHaveGoggleInformation`, `FIBE:25`), type registered `AC:63-65`. Plain `BlockItem` `AC:60`.
- Renderer `FIR` (`SafeBlockEntityRenderer`), registered `S/client/AeronauticsRenderers.java:47`. No part models: every moving thing is Java-drawn quads. No menu, no ticker.
- Five Create Display Link sources, one per reading (`AC:68-77`, bound to the block `AC:105-106`; class `FDS`).
- Only exists with Sable: `SableCompat.LOADED` (`S/SableCompat.java:10-11`) gates registration (`J/ComedyMonkeysCreateAddons.java:73-74`), the display sources (`:271`), the renderer (`J/client/ComedyMonkeysCreateAddonsClient.java:102-103`), Ponder (`J/client/ponder/CMPonderPlugin.java:91-92`) and the creative tab (`J/registry/ModCreativeTabs.java:73-74`). Recipe and advancement have a `mod_loaded: sable` condition; pickaxe tag entry `required: false` (`src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json:32-35`).
- Family (survey): "Ship hardware" (`design/dossiers/survey.md:41`).

## What It Does

1. **Placing**: it faces the player who places it (`FACING` = horizontal look direction reversed, `FIB:74-76`); four horizontal facings only (`FIB:35`). The way the pilot faces the panel is the ship's forward: forward = −facing (`FIBE:52-54`; test `FlightInstrumentsBlockEntityTest.java:89-98`).
2. **Reading the ship** (`FIBE:50-67`): if the block is on a Sable sub-level (an Aeronautics ship) it takes the ship's orientation, turns forward and right by it, and reads:
   - **Altitude**: the world Y of the block's centre, projected out of the ship (`:62`, `:66`). It's world height, not height above ground.
   - **Airspeed**: the length of the ship's velocity at the block's centre, in blocks (m) per second (`:63`, `:66`), including any vertical speed and spin.
   - **Climb**: the vertical part of that velocity (`:66`).
   - **Heading**: compass bearing of forward, north 0°, east 90° (`:74-77`), with one of 8 compass points (`:26`, `:31-38`).
   - **Pitch**: asin of forward's height, ±90° (`:64`). **Roll**: asin of −right's height, ±90° (`:65`).
3. **Off a ship** it still reads: altitude = the block's own centre Y, airspeed 0, climb 0, heading = the panel's forward, pitch and roll 0, flagged "not on a ship" (`FIBE:56-57`; test `:77-86`).
4. **Shows it**: the renderer works the readings out again every frame on the client (`FIR:48`) and draws them on four dials (see Moving Parts). Nothing is stored or synced: the block entity has no saved data and no tick.
5. **Goggles**: altitude, airspeed, climb, heading and pitch/roll lines, plus "Not on a ship" off a ship (`FIBE:80-91`).
6. **Display Links**: altitude "N m", airspeed "N.N m/s", climb "+N.N m/s", heading "045° NE", attitude "P +N° R +N°" (`FDS:15-24`), each a single line that can be labelled (`:40-42`), refreshed every 20 ticks (`:46-48`). Tests pin the formats (`src/test/java/com/comedymonkey/createaddons/compat/sable/FlightDisplaySourcesTest.java`).
7. **No other interaction**: no right-click action, no redstone, no power, no inventory. Wrench is Create's default (`IWrenchable`, not overridden).

## What the Player Is Told

- Name "Flight Instruments" (`A/lang/en_us.json:621`).
- Tooltip text exists: "A sloping cockpit desk for _Aeronautics ships_ with four live gauges: an _altimeter_, _airspeed_ indicator, _artificial horizon_ and _heading_ indicator." / "When Placed: Faces you. The way you face the panel counts as the ship's _forward_, for heading and pitch." / "With Goggles or a Display Link: … _altitude_, _airspeed_, _climb_, _heading_ or _attitude_ on a display." (`en_us.json:622-626`). But only the gyroscope and rotor items are registered for Create tooltips (`J/ComedyMonkeysCreateAddons.java:272-273`), so this text may never show (worth checking in game).
- Goggles: "Flight Instruments:", then gray labels with aqua values: "Altitude N.N m", "Airspeed N.N m/s", "Climb +N.N m/s", "Heading 045° NE", "Pitch / Roll +N° / +N°", and dark grey "Not on a ship" (`FIBE:80-96`, `en_us.json:627-633`).
- Display Link source names: Altitude, Airspeed, Climb Rate, Heading, Pitch and Roll (`en_us.json:637-641`).
- Ponder "Know Where You Are" (`AS:97-125`, `en_us.json:649-652`): the panel alone on a base plate at (2,1,2), facing south (`A/ponder/flight_instruments.nbt`); "read the ship they're on: altitude, airspeed, attitude and heading, each on its own dial" (south face); "Place it facing you in the cockpit: the way you face the panel is taken as the ship's forward" (top); "Engineer's Goggles show every reading, and a Display Link can put any of them on a Display Board or Nixie Tubes". No ship, nothing moves.
- README: "a sloping cockpit console with four live dials: an altimeter (long hand per 100 m, short per 1000 m), airspeed (0 to 60 m/s), an artificial horizon that tilts with roll and drops with pitch, and heading" (`README.md:259`). (`README.md:31` lists Sable as needed only for the gyroscope and rotor.)
- Recipe: shaped `ICI / KPK / IEI` = 4 Iron Sheets, a **Compass** (top), 2 **Clocks** (sides), a Precision Mechanism (centre), an **Electron Tube** (bottom) (`D/recipe/flight_instruments.json:10-31`).
- Advancement "Instrument Rated": "Now you know exactly how lost you are" (`en_us.json:642-643`; parent the gyroscope's, `D/advancement/flight_instruments.json:8`).
- The promise: a real aircraft instrument panel with four live dials.

## States

| State | Source | How the player can tell today |
|---|---|---|
| `facing` north/east/south/west | blockstate (`FIB:35`), toward the placer (`FIB:74-76`); `rotate`/`mirror` follow it (`FIB:79-86`) | The panel faces that way |
| On a ship / not | derived each frame (`FIBE:55-57`), not synced | Not on the model: off a ship the dials still show a level horizon, zero airspeed, the block's height and a heading. Goggles only ("Not on a ship") |
| Altitude | live (`FIBE:62`) | Altimeter hands |
| Airspeed 0–60 m/s | live (`FIBE:63`) | Airspeed needle; pegs at 60 (`FIR:67`) |
| Climb rate | live (`FIBE:66`) | **Not drawn**: goggles and Display Link only |
| Heading | live (`FIBE:74-77`) | Little plane on the heading dial |
| Pitch | live (`FIBE:64`) | Horizon drops as the nose rises; pegs at about ±36° (`FIR:147`) |
| Roll | live (`FIBE:65`) | Horizon tilts, full circle |
| Day / night | block light | The dials take the world's light (`FIR:104`): they go dark at night, with no glow |

## Moving and Changing Parts

All drawn by `FIR` as flat two-sided quads (`FIR:96-107`) in the cutout layer, coloured by vertex colour over the centre texel of vanilla `block/white_concrete` (`FIR:31`, `:49-50`, `:85-86`), at the block's light, layered 0.015 px apart just in front of the panel face (`FIR:91-94`). Placement: rotated to the facing about the block centre (`FIR:52-55`), then leant back 22.5° about X at the panel's foot (8, 2, 8) (`FIR:57-59`). Panel units map u → x = 16 − u (as you face it, u runs to your right) and v → height along the panel, its full length 14 / cos 22.5° = 15.15 px (`FIR:36`, `:91-94`).

| Part | Model file | Driven by | Axis / pivot / range | Source |
|---|---|---|---|---|
| Altimeter long hand | none (Java quads), `NEEDLE` `0xF0F0F0` | altitude | Pivot gauge 0 at panel (4, 4), top left. One turn per 100 m, clockwise from straight up (`(alt mod 100)/100 × 360`). Length 0.85 × 2.75 = 2.34 units, base width 0.2, tapering to 30% | `FIR:64`, `:123-128` |
| Altimeter short hand | Java, `SHORT` `0xF0B040` (amber) | altitude | Same pivot; one turn per 1000 m; length 0.55 × 2.75, width 0.3. World heights −64 to 320 keep it within about 337° to 115° | `FIR:65` |
| Needle hubs | Java, `0x2A2A2A` | — | A square of half-size 1.4 × width over each pivot | `FIR:129-132` |
| Airspeed needle | Java, `NEEDLE` | airspeed | Pivot gauge 1 at (12, 4), top right. −150° (lower left) at 0 m/s to +150° (lower right) at 60 m/s, 5° per m/s, clamped | `FIR:67` |
| Attitude ball, sky | Java, `SKY` `0x5AA6E8` | — | A 28-sided disc, radius 2.75 units, at gauge 2 (4, 12), bottom left | `FIR:136-143` |
| Attitude ball, ground | Java, `GROUND` `0x8A5A2E` | roll, pitch | The disc clipped by the horizon. The horizon turns against the roll (full circle) and moves down by pitch/40 × radius (nose up shows more sky), capped at 0.9 of the radius | `FIR:145-162` |
| Horizon line | Java, `LINE` `0xF0F0F0` | roll, pitch | 0.24 units thick across the ball's chord at the horizon | `FIR:164-171` |
| Aircraft symbol | Java, wings `0x101010`, centre `0xF0F0F0` | fixed | Two bars 0.55 to 1.9 units each side of centre, 0.3 thick; a 0.7-unit centre square | `FIR:173-176` |
| Heading plane | Java, `NEEDLE` | heading | At gauge 3 (12, 12), bottom right; nose points at the heading clockwise from straight up (north up). Fuselage nose +1.3 to tail −1.1 units, 0.36 wide; wings span ±1.1 at +0.2; tailplane ±0.5 at −0.9 | `FIR:71`, `:184-202` |

No smoothing: readings are taken fresh each frame from Sable's client pose and `partialTicks` is unused (`FIR:42-48`). Nothing in the static model moves; the dial faces, numerals and the airspeed arc are painted in the panel texture.

## Code Contract

**Files a new look must keep (or change the code with it)**
- Blockstate `A/blockstates/flight_instruments.json`: `facing=north` unrotated, east y90, south y180, west y270, all `block/flight_instruments` (`:3-18`). So the model is built **panel facing north**, the pilot standing on the north side.
- Block model `A/models/block/flight_instruments.json`: parent `minecraft:block/block`, no render type set (solid), AO on; textures `body`, `side`, `panel`, `buttons`, particle `body` (`:2-9`), all ours.
- Textures (ours, used only by this model): `flight_instruments_body.png`, `flight_instruments_side.png`, `flight_instruments_buttons.png` (16x16) and `flight_instruments_panel.png` (128x128; the renderer's comment still says 64 px, `FIR:32`, but it works in sixteenths so any resolution fits).
- Item model `A/models/item/flight_instruments.json`: just `parent: block/flight_instruments`. No item renderer, so the item shows the painted dials with no hands and a black attitude dial.

**Geometry the renderer assumes (the tightest constraint)**
- The panel's front plane: through (y 2, z 8) in model space, leant back 22.5° about X at (8, 2, 8), 16 px wide and 15.15 px long, its top edge at y 16 (`FIR:57-59`, `:36`; model #18 `:887` is `[0,2,8]`–`[16,17.153,9]` rotated x 22.5 at origin `[8,2,8]`). The Java quads sit 0.015–0.06 px in front of that plane, so any new model must put the panel face exactly there, or the hands float or sink into it. Moving the panel means changing `FIR:57-59` and `:36` together.
- Gauge layout: four dials centred at panel (4, 4), (12, 4), (4, 12), (12, 12) (`FIR:33`), radius 2.75 units (`FIR:34`; 22 px on the 128 px texture): top left altimeter, top right airspeed, bottom left attitude, bottom right heading (`FIR:63-71`). The painted dial faces must match these centres and that radius. The attitude dial must stay a plain disc of that radius: the sky ball is drawn over it.
- Readable dial marks must agree with the code's scales: altimeter 10 m per digit (0–9) on the long hand; airspeed −150° = 0 to +150° = 60 m/s; heading north up, clockwise.
- Shape (Java, per facing, `FIB:37-56`): a full-width lip y 0–2 from 5 px behind the front to the back, then 2 px steps from y 2 to 16 starting 8 + ⌊(y − 2)·tan 22.5°⌋ px back (8, 8, 9, 10, 11, 12, 12). The front 5 px of the block are open. The outline sits up to about 1.8 px proud of the panel near the top. A new silhouette means editing `SHAPES`.
- Facing: horizontal only, faces the placer. Heading and pitch are measured from that facing, so the "front" in the model must be where the pilot stands.
- Properties: map colour metal, strength 2.5/6.0, metal sound, needs a pickaxe, `noOcclusion` (`AC:52-58`); unlike the other two it doesn't turn off redstone conduction, suffocation or view blocking. No light level.
- No ports, no shaft, no redstone, no value boxes, no filter slots. Display Links attach to the block itself.
- Ponder: one panel at (2,1,2) facing south; arrows at its south face and top (`AS:103-121`). Survives a new model.

**Borrowed**
- Vanilla `block/white_concrete` as a flat colour sprite for the quads (`FIR:31`). Nothing from Create, Aeronautics or Simulated is in the model. Aeronautics and Simulated assets are All Rights Reserved; nothing of theirs may be copied.

**Colours that are Java constants** (`FIR:37`, `:132`, `:174-176`): sky `0x5AA6E8`, ground `0x8A5A2E`, horizon and long needles and heading plane `0xF0F0F0`, altimeter short hand `0xF0B040`, needle hubs `0x2A2A2A`, aircraft wings `0x101010`, aircraft centre `0xF0F0F0`. A palette change to the moving parts is a code change, not a texture change.

### Shared with the ship hardware set

| Shared piece | Where | What depends on it |
|---|---|---|
| Registration and Sable gate | `AC:27-107`, `S/SableCompat.java` | Name-only |
| Renderer registration | `S/client/AeronauticsRenderers.java:44-48` registers all three; `FIR` is its own file and shares no helpers with the gyroscope and rotor renderers | `FIR` can be rewritten alone |
| Ponder scenes file | `AS:24-31` (all three); the instruments are in no Ponder tag (`CMPonderPlugin.java:119-122` tags only the other two) | Shared file, separate methods |
| Advancement tree | child of the gyroscope's (`D/advancement/flight_instruments.json:8`) | Icon is the item |
| Recipe language | Precision Mechanism in all three; iron sheets here and in the rotor | Materials a shared casing could draw on |
| Textures | none shared: the instruments use only their own `flight_instruments_*` textures; the gyroscope and rotor share Create's brass casing | A shared set casing would be new |

The instruments don't draw `CobbleGeneratorRenderer.SHAFT` or anything else frozen; nothing here is on `design/hands-off.md`.

## Current Look

Before sheet: `design/dossiers/aeronautics-set/flight-instruments-textures-before.png`. Top row: `flight_instruments_body`, `flight_instruments_side`, `flight_instruments_buttons` (each scaled to 128) and `flight_instruments_panel` (scaled to 256). Bottom row, the Java colour constants as swatches: sky `5AA6E8`, ground `8A5A2E`, line/needle `F0F0F0`, short hand `F0B040`, hub `2A2A2A`, aircraft wings `101010`.

Geometry (19 elements, `flight_instruments.json`):
- The lip, 16 x 2 x 11 at the bottom, z 5–16 (#0 `:12`).
- Fourteen 1 px slabs y 2–16, each 0.414 px further back (front edges z 9.01 to 14.40), giving the side profile its slope; only their sides, back and the last one's top are drawn (#1–14 `:84-656`).
- Three 2 x 0.6 x 1.5 px buttons on the lip in front of the panel, at x 3–5, 7–9 and 11–13, z 5.75–7.25 (#15–17 `:710-828`), sampling red, amber and green strips of the buttons texture.
- The panel, a 16 x 15.15 x 1 slab leant back 22.5° (#18 `:887`), its front textured with the panel, its edges with the body.

Textures (measured): body and side are dark blue-grey steel, `#565a63` with `#393c43`/`#474b53`/`#676c75` mottling and `#7f848d` edge lights; the side has a small three-slot vent at lower right. Panel: four riveted steel bezels round black dial faces `#131418`, numerals and ticks near-white `#e8e8e8`; the airspeed dial has a saturated green-yellow-red arc and "20", "40", "M/S"; the heading dial is marked N, 3, 6, 9; the attitude dial is blank black. Buttons: red `#7a3029`/`#b0544a`, amber `#86652a`/`#c09a48`, green `#41693a`/`#6c9a52`, each a flat stripe.

What works:
- The dark steel body and panel are close to the house palette (`#2f343b`–`#4a5059` steel, a little lighter), with gentle mottling.
- The 2x2 riveted bezels read clearly as an instrument panel; the four dials are the hero and they're live.
- The sloping desk silhouette says "cockpit console" at a glance.

What doesn't (against `create-house-style.md`):
- Near-white everywhere on the moving parts and the dials: `#e8e8e8` numerals, `0xF0F0F0` needles, horizon line, plane and aircraft centre. The style says light areas stay mid grey and glows are never near-white.
- The sky `0x5AA6E8` is a saturated bright blue and the short hand `0xF0B040` a bright yellow, both Java constants; the airspeed arc is fully saturated traffic-light colours.
- The three buttons are flat colour strips with no shape, an accent cluster with no meaning.
- No Create material at all (no brass, no andesite, no shafts or cogs): it reads as a generic sci-fi panel rather than Create flight equipment, and shares nothing with the gyroscope and rotor (`survey.md:41`).
- The dials don't glow, so at night or in a dark cockpit the panel goes black.
- The lip and the stepped body are plain; the empty front 5 px make it a shallow desk with nothing on the ledge but the buttons.
- The item shows dead dials.

Survey: rank 8, total 10 (Gap 3, Presence 3, Fit 4, effort small–medium): "Good dark panel. Near-white needles and a saturated sky (Java constants). Three flat colour-strip buttons that do nothing"; "Gauges are already live. The buttons are dead" (`survey.md:14`).

## Function the Look Doesn't Show

- **Climb rate**: computed, shown in goggles and on Display Links, but there's no vertical speed dial.
- **On a ship or not**: off a ship the panel looks exactly like a parked ship's: level horizon, zero airspeed, height and heading all shown.
- **Display Link output**: five sources, but nothing on the block hints that it can feed a display (no plug, socket or terminal).
- **Which way is the ship's forward**: only implied by where the pilot stands; nothing marks it, and the heading plane only matches it once you know the rule.
- **Exact numbers**: the dials give rough readings; values are goggles-only.
- **Altitude beyond one turn**: above 1000 m the short hand wraps; there's no thousands indicator (rarely matters at world heights).

## Look the Code Doesn't Back Up

- **The three buttons**: red, amber and green controls on the lip that do nothing. The block has no use action, no redstone and no state for them (`FIB` has no interaction methods).
- **The airspeed arc's colour bands**: green, yellow and red imply safe, caution and danger speeds; nothing in the code reacts to speed, and the needle simply stops at 60.
- **The heading dial's 3, 6, 9**: on a real heading card these mean 030°, 060°, 090°; here they sit at east, south and west (90°, 180°, 270°), more like a clock face, while the goggles and Display Link give degrees and compass points.
- **The side vent and the electron tube in the recipe**: they suggest powered electronics that need cooling; it needs no power.
- **A cockpit desk of switches**: the shape and buttons promise controls; it's read-only.

## History

- `6a38571` Initial Checkin: block, block entity, display sources, renderer, model, textures, recipe, Ponder; no later commits to these files.
- Tests: `src/test/java/com/comedymonkey/createaddons/compat/sable/FlightInstrumentsBlockEntityTest.java` (compass points, heading maths, off-ship readings, forward = into the panel) and `FlightDisplaySourcesTest.java` (display formats).
- Survey rank 8 (`survey.md:14`); "Ship hardware" family note (`survey.md:41`).
- No earlier brief, spec or plan in `design/briefs`, `design/specs` or `design/plans`.
