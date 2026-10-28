# Electric Lights: Sound Design

Spec: `design/specs/electric-lights.md` (P19) · briefs: `design/briefs/electric-lights-{street,indoor,runs-and-specials}.md` · models: `design/models/electric-lights-{street,indoor,runs}.bbmodel` (generator `electric-lights.gen.js`) · levels: `design/sound/staging.md` · cues: `electric-lights.cues.json`.

The user (2026-10-28): "Lets do sound, keep them subtle".

## Identity

Lights are the quietest things in the mod. A lit street or factory makes **no running sound at all**: no hum from lamps, no buzz from strips. Sound only marks a **change you caused or should notice**: a switch flipped, a lamp starved of power, a sodium lamp striking, a beacon turning, a floodlight swinging round. Everything is small, close and soft-edged.

- **Mechanical identity**: small snap-action switches, a ballast that ticks as it warms, a little geared motor in the beacon, a yoke that swings on its pins.
- **Material identity**: bakelite and brass (the switch), steel housings (soft ticks, never clangs), glass (a faint tink).
- **Emotional identity**: calm and tidy. A light that works is silent; a light in trouble buzzes, briefly.

## Signature sounds

1. **Snap click**: the small, crisp snap of a light switch, with a soft body under it. On is a touch higher than off.
2. **Ballast tick**: a tiny dry tick of metal warming, the sound of a sodium lamp striking. Also the detent of the brightness step.
3. **Mains buzz**: a faint, rough 100 Hz buzz with tiny crackles, only while a light flickers.
4. **Gear whirr**: a soft small-motor whirr with a gear tick once a turn, only the Warning Beacon.

## Personality

| Trait | Sound |
|---|---|
| Calm | no running sound from any lamp |
| Tidy | short, dry one-shots with no tails |
| Honest | a flicker buzzes so you hear why the lights went out |
| Warm | the sodium warm-up ticks slow down as it settles |

## Events with new sounds

### lights.switch_on
**Purpose** · the circuit through this Light Switch is on.
**Sound Layers** · mechanical: snap-action toggle · material: bakelite and brass, a dry click over a small soft body · personality: crisp, small.
**Timing** · snap at 0 (15 ms), body 0.01–0.07 s; nothing after 0.1 s.
**Variations** · 2 takes; pitch ±4%, gain ±8%.
**Implementation Notes** · one-shot at the switch when it's flipped on; `BLOCKS`; starting/stopping extra layer level (B + 5, quieter than the Breaker's lever), range 8.
**Source** · real · BigSoundBank #0052 Electric switch #2, two takes; rung 3, CC0. (#0540 Old switch auditioned; the user preferred the modern clicks.)

### lights.switch_off
**Purpose** · the circuit is off.
**Sound Layers** · as `switch_on`, the snap a little lower and duller.
**Timing** · as `switch_on`.
**Variations** · 2 takes; pitch ±4%.
**Implementation Notes** · as `switch_on`.
**Source** · real · as `switch_on`, the off clicks of the same recordings.

### lights.flicker
**Purpose** · a light is short of power: it flickers for a second and goes dark (and the same when power comes back and it flickers on).
**Sound Layers** · material: a faint rough mains buzz · mechanical: tiny irregular contact ticks, in step with the renderer's 90 ms flicker frames.
**Timing** · buzz 0–1.0 s fading out; ticks at 0, 0.09, 0.2, 0.27, 0.45, 0.62, 0.85 s.
**Variations** · 3 takes with the ticks shuffled; pitch ±5%.
**Implementation Notes** · one-shot per light when its flicker starts, client side, **capped**: at most 3 flicker sounds at once within 16 blocks of the player, nearest first, so a whole street browning out is a soft crackle, not a roar. `BLOCKS`; running pulse level (B), range 8.
**Source** · real · BigSoundBank #2143/#2144 Flashing light bulb ticks over #2118 Neon or transformer (low-passed, 11 Hz tremolo); rung 2.

### lights.warm_up
**Purpose** · a High-Bay Lamp or Yard Light Head is striking: the 4 s warm-up from dark red to sodium amber.
**Sound Layers** · mechanical: ballast ticks, quick at first and slowing as it settles · material: a very faint low swell under them.
**Timing** · ticks at 0.15, 0.4, 0.75, 1.2, 1.8, 2.5, 3.3 s; swell 0–4 s.
**Variations** · 2 takes; pitch ±4%.
**Implementation Notes** · one-shot when the lamp starts warming; capped like `flicker` (3 at once), so a yard of lamps on one Breaker ticks softly together. `BLOCKS`; cooling-tick level (B − 8), range 8.
**Source** · real · Kenney `tick_004` pitched down and slowing, over #2118 low-passed to a swell; rung 2. No real ballast or cooling-tick recording found (searched cooling, quenching, thermal).

### lights.beacon_whirr
**Purpose** · the Warning Beacon is turning.
**Sound Layers** · mechanical: a soft small geared motor and one gear tick per turn (the brass cog) · material: muffled under the glass dome.
**Timing** · a seamless 2 s loop (two turns at one turn a second); ticks at 0 and 1.0 s.
**Variations** · one loop; the code nudges pitch ±3% per beacon so a row doesn't phase.
**Implementation Notes** · looping block sound while lit; `BLOCKS`; second-layer running level (B − 4), range 8.
**Source** · real · BigSoundBank #1263 Car antenna, exit: its steady small motor, pitched down and muffled (rung 3); Kenney `tick_004` once a turn (rung 2). BigSoundBank 'gyrophare' results are sirens, not motors.

### lights.floodlight_aim
**Purpose** · the Floodlight swings round to its new target after pairing.
**Sound Layers** · mechanical: a short smooth servo sweep · material: a faint pin creak, then a small steel settle tick.
**Timing** · sweep 0–0.5 s (the turn eases over half a second), creak 0–0.4 s, settle tick at 0.5 s.
**Variations** · 2 takes; pitch ±5%.
**Implementation Notes** · one-shot when a pairing finishes; `BLOCKS`; starting/stopping extra layer level (B + 5), range 12.
**Source** · real · #1263 the antenna motor's start and stop clunk; rung 3.

### lights.brightness_step
**Purpose** · the wrench stepped the light's brightness.
**Sound Layers** · mechanical: a small detent click · material: a faint glass tink.
**Timing** · click at 0 (20 ms), tink 0.01–0.12 s.
**Variations** · one take, **pitched by step** in code: Full 1.2, Bright 1.1, Dim 1.0, Glow 0.9, so you can hear where you are without looking.
**Implementation Notes** · one-shot on each step (on a run, once at the clicked block, not per piece); `BLOCKS`; B + 3, range 8. Plays alongside Create's own wrench sound.
**Source** · real · Kenney UI Audio `switch24` and Interface Sounds `tick_002`; rung 2.

## Vanilla sounds (no new files)

Following the Power Lines, which use quiet vanilla events for every hand action:

| Action | Sound | Volume, pitch |
|---|---|---|
| Dye a light (or a run) | `item.dye.use` (the sign-dyeing sound) | 0.7, 1.1 |
| Wash with a water bucket | `item.bottle.empty` | 0.5, 1.2 |
| Lower a Pendant or Chandelier one block | `block.chain.place` | 0.6, 1.3 |
| Add a lamp head or arm to a Lamp Post | `block.lantern.place` | 0.8, 1.0 |
| Take a head or arm off (sneak-wrench) | `block.lantern.break` | 0.7, 1.1 |
| Hang Festoon Lights | `block.chain.place` (as power spans) | 0.8, 0.9 |
| Take Festoon Lights down | `entity.sheep.shear` (as power spans) | 0.8, 1.2 |
| Lights switched by a Breaker | the Breaker's own lever click; lights are silent | |

**Block sound types:** lamps (heads, wall, ceiling, Pendant, Bollard, Floodlight, Warning Beacon) `SoundType.LANTERN`; Lamp Posts `SoundType.METAL`; Chandelier and wall hook `SoundType.CHAIN`; Light Strip, Edge Light, Indicator Lamp and Light Switch `SoundType.COPPER` at 0.8 volume; Floor Light `SoundType.METAL`.

## Clips

Preview clips with these sounds on their Effects tracks (built by `electric-lights.gen.js`, which refuses to build if a cued sound has no real file): `warm_up` in `electric-lights-indoor.bbmodel` (the High-Bay from dark red to amber); the rest in `electric-lights-runs.bbmodel` (the Light Switch with the Indicators following it, the Beacon rotor, the Floodlight swinging to a new target, two Bollards flickering out, a Floor Light stepping through its brightness). Approved real sounds 2026-10-28 (modern switch clicks only).

| Clip | Length | Time (s) | Event |
|---|---|---|---|
| switch | 2.0 | 0.3 / 1.4 | switch_on / switch_off |
| flicker_out | 1.5 | 0 | flicker |
| warm_up | 4.5 | 0 | warm_up |
| beacon | 2.0 | 0 | beacon_whirr (loop) |
| floodlight_aim | 1.0 | 0.1 | floodlight_aim |
| brightness | 2.0 | 0.2 / 0.7 / 1.2 / 1.7 | brightness_step (×4) |
