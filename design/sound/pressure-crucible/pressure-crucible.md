# Sound Design: Pressure Crucible

Brief: `design/briefs/pressure-crucible.md`. Model and clips: `design/models/pressure-crucible.bbmodel` (`heated`: an Emerald every 2.5 s at 64 RPM; `superheated`: Netherite, shortened to every 5 s for the preview; `smouldering` and `cold` at rest), timings from `pressure-crucible.gen.js`. Cues: `pressure-crucible.cues.json`, written by `make-cues.js`.

## Identity

A cast-iron screw press over a fire. It should sound **heavy, slow and satisfying**: gears grinding quietly round, the screw creaking as it winds the platen down, and a deep, gritty *crunch* when the dust gives way and becomes a gem. The fire under it tells you how hot it is.

- **Mechanical identity**: meshing gears (the whirr), a big screw in a nut (the creak), the platen landing on the die (the crunch).
- **Material identity**: cast iron and steel (low thuds, groans), crushed dust (the gritty crumble), fire from the Blaze Burner (crackle, or a cool soul hiss when superheated).
- **Emotional identity**: patient and powerful. It takes its time, then delivers.

## Running all day

It runs in a base 24/7, so the turning and fire sounds are **background**: quiet, low, carrying only 8 blocks. The crunch is the payoff and can be a little clearer, but it repeats (glass and stone take half a second each at 64 RPM), so it stays moderate. Everything starts quieter than feels right, as the user asked of the Magma Engines.

## Signature sounds

1. **Crunch** (`crunch`): a deep thud with a gritty crumble and a hard clack as the platen lands: the item is pressed.
2. **Creak** (`creak`): the screw groaning as the platen starts down.
3. **Whirr** (`whirr`): meshing gear teeth, quiet, while the shaft turns.
4. **Fire** (`fire`, `fire_soul`): the burner under the bowl: a warm crackle when heated, a cooler soul hiss when superheated.

## Personality

| Trait | Sound |
|---|---|
| Heavy | low thuds and groans, nothing bright |
| Patient | a slow creak through the press, no rush |
| Satisfying | a crunch with grit in it at the end of every press |
| Honest about heat | the fire you hear is the fire it has |

## Events

### whirr
**Purpose** · it's turning
**Sound Layers** · mechanical: a soft gear whine (300 Hz) and teeth ticking past · a low hum
**Timing** · continuous loop
**Variations** · one loop; pitch follows the shaft speed (about 0.8 at 16 RPM to 1.3 at 256 RPM)
**Implementation Notes** · tickable `SoundInstance` while the shaft turns; `BLOCKS`; background, attenuation distance 8

### creak
**Purpose** · a press has started: the screw is winding the platen down
**Sound Layers** · material: an iron creak (140 Hz) · mechanical: a slow ratchet of the threads
**Timing** · 0.7 s, as the platen leaves the top
**Variations** · 3 variants, pitch ±6%; skipped when presses are under a second apart (glass, stone, high speed) so it doesn't chatter
**Implementation Notes** · client-side when `processing` turns on; `BLOCKS`; quiet, attenuation distance 8

### crunch
**Purpose** · an item has been pressed
**Sound Layers** · mechanical: a hard clack of the platen on the die · material: a deep iron thud · the dust crumbling
**Timing** · clack 0 s, thud 0.01 s, crumble 0.02–0.27 s
**Variations** · 4 variants, pitch ±6%; a little lower for the slow presses (Netherite, Diamond)
**Implementation Notes** · replaces Create's press activation sound in `press()`, played by the server so everyone nearby hears it; `BLOCKS`; moderate

### fire
**Purpose** · the burner underneath is lit (heated, or faintly when smouldering)
**Sound Layers** · a low warm hum (60 Hz) · a faint dark noise body · the odd crackle
**Timing** · continuous loop
**Variations** · one loop; quieter when smouldering, none when cold or with no burner
**Implementation Notes** · tickable `SoundInstance` following the heat below (read on the client, like the glow); `BLOCKS`; background, attenuation distance 8

### fire_soul
**Purpose** · the burner is superheated
**Sound Layers** · a cooler hum (90 Hz) · a soft high hiss · the odd crackle
**Timing** · continuous loop
**Variations** · one loop, instead of `fire` when superheated
**Implementation Notes** · as `fire`

## Clips

| Clip | Time (s) | Event |
|---|---|---|
| heated | 0, 1.5, 3.0, 4.5, 6.0 | whirr (the loop, sketched in 1.5 s pieces) |
| heated | 0.1, 1.6, 3.1, 4.6, 6.1 | fire |
| heated | 0.05, 2.55, 5.05 | creak |
| heated | 2.5, 5.0 | crunch |
| superheated | as `heated` with presses every 5 s and `fire_soul` | whirr, fire_soul, creak, crunch |
| smouldering | 0 | fire |
