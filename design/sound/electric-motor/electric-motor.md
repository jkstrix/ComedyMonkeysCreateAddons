# Sound Design: Electric Motor

Brief: `design/briefs/electric-motor.md`. Model and clips: `design/models/electric.bbmodel` (`run`, `idle`, shared with the Lava Generator), timings from `electric.gen.js`: power arrives at 2.0 s and the shaft spins; it runs short at 8.0 s and stops; the clip ends at 10.5 s. Cues: `electric-motor.cues.json`.

## Identity

A sturdy flange-mounted motor: copper windings in a ribbed steel can. It should sound **electric but hand-built**, a Create machine rather than sci-fi: a warm winding hum with a bearing whine on top, a brass contactor that clacks in and out, and a little crackle now and then from the coil.

- **Mechanical identity**: a spinning armature on bearings (a whine whose pitch follows the set speed), a contactor that pulls in when power arrives.
- **Material identity**: copper windings (the warm hum), the steel can (body), the brass contactor (the clack and clink).
- **Emotional identity**: dependable and strong. A confident clack on, a smooth run, an honest wind-down when it runs short.

## Running all day

These run in a base 24/7, so every running sound is **background**: quiet, low, soft-edged and never hissy, and it carries only a short way (`attenuation_distance` 8 in `sounds.json`, half the default 16). Starting up and shutting down can be **loud**: they're rare and they tell you something changed. The wind-up rises full and then settles into the quiet running loop as it reaches speed.

## Signature sounds

1. **Winding hum** (`hum`): a soft warm 100 Hz hum with just a hint of whine (520 Hz) on top. In game the whine's pitch follows the speed setting.
2. **Contactor clack**: a brass clack and clink, the first layer of `start` and `stop`.
3. **Coil spark** (`spark`): a barely-there crackle, rare, with the spark particles.

## Personality

| Trait | Sound |
|---|---|
| Dependable | an even hum, no wobble |
| Strong | a solid low hum under the whine |
| Confident | sharp contactor attack on starting |
| Honest | it winds down audibly when short of power, so you know why it stopped |

## Events

### hum
**Purpose** · it's running, and how fast
**Sound Layers** · material: copper winding hum (100 Hz) · mechanical: a faint armature and bearing whine (520 Hz)
**Timing** · continuous loop
**Variations** · one loop; in game the pitch follows the speed setting (about 0.7 at 16 RPM to 1.2 at 256 RPM) and the volume rises a little with the stress setting
**Implementation Notes** · tickable `SoundInstance` while `powered`; `BLOCKS`; background (about 0.1 volume), attenuation distance 8, so a row of motors doesn't drone

### spark
**Purpose** · there's real power in the coil
**Sound Layers** · electric: a soft dry click and a very short muted burst · a tiny tick
**Timing** · 0.08 s one-shot
**Variations** · 4 variants, pitch ±10%
**Implementation Notes** · client-side from `animateTick`, only on a few of the spark particles (about one in ten); `BLOCKS`; very quiet, attenuation distance 8

### start
**Purpose** · power has arrived and it's running
**Sound Layers** · mechanical: contactor clack and brass clink · whine rising from 120 to 520 Hz · the hum swelling in
**Timing** · clack 0 s → wind-up 0.05–0.95 s; the hum loop takes over
**Variations** · 2 variants, pitch ±4%
**Implementation Notes** · when `powered` turns on; `BLOCKS`

### stop
**Purpose** · it's short of power (or the power was cut) and has stopped
**Sound Layers** · mechanical: contactor drop-out clack · whine falling from 520 to 90 Hz · a soft thud as it stops
**Timing** · clack 0 s → wind-down 0.03–1.2 s → thud 1.15 s
**Variations** · 2 variants, pitch ±4%
**Implementation Notes** · when `powered` turns off; `BLOCKS`

### adjust
**Purpose** · the settings changed (speed, stress or direction from its screen)
**Sound Layers** · mechanical: a short ratchet of detent clicks · a brass clink
**Timing** · 0.4 s
**Variations** · 2 variants, pitch ±5%
**Implementation Notes** · when new settings are applied (`MotorSettingsPayload`), played at the motor for everyone nearby; `BLOCKS`

## Clips

| Clip | Time (s) | Event |
|---|---|---|
| run | 2.0 | start |
| run | 2.6, 4.1, 5.6, 6.9 | hum (the loop, sketched in 1.5 s pieces) |
| run | 4.6 | spark |
| run | 8.0 | stop |
| idle | 0.4 | adjust |
