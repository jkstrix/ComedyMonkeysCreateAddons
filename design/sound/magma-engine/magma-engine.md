# Sound Design: Magma Engine and Supercharged Magma Engine

Briefs: `design/briefs/magma-engine.md`, `design/briefs/supercharged-magma-engine.md`. Model and clips: `design/models/engines.bbmodel` (`run` is the Magma Engine, `run_super` the Supercharged one, `idle` both cold), timings from `engines.gen.js`: lit from 1.0 s to 6.5 s, when the tank runs dry and the engine stops at once; the crank turns at 64 RPM (384° a second), the Supercharged at 128 RPM. Cues: `magma-engine.cues.json`, written by `make-cues.js` from those timings (each piston chugs at the top of its stroke, half a turn apart).

## Identity

A hot rod V-twin running on lava. It should sound **mechanical, punchy and alive**: a steady chug-chug from the two pistons, a low lava firebox under it, and on the Supercharged one a thin supercharger whine on top. Hand-built Create machinery, not a car engine recording.

- **Mechanical identity**: two pistons on a crank (the chug, twice a turn, so its rate *is* the speed), a flywheel (the even rhythm), a belt-driven blower (the whine).
- **Material identity**: a dark iron drum (low thumps), copper ports and exhaust stubs (a soft puff with each chug), lava (the firebox bed and Minecraft's own lava pop).
- **Emotional identity**: eager and cheeky. It catches with a cough and a few quick chugs; when the lava runs out it sputters twice and clunks to a stop.

## Running all day

After play-testing in game the user asked for every engine sound at half volume ("the sounds need to be made 50% quieter, but other than that they work perfectly"): every real recipe's level is halved, and the vanilla lava pop plays at 0.2.

These run in a base 24/7, so every running sound is **background**: quiet, low, soft-edged and never hissy, and it carries only a short way (`attenuation_distance` 8 in `sounds.json`). The chug is the quietest thing that still tells you it's running. Starting and stopping can be **loud**: they're rare and they tell you something changed.

## Signature sounds

1. **Chug** (`chug`): a short low thump with a soft exhaust puff, once per piston stroke, timed from the crank angle. Its rate is the engine's speed: about 2 a second on the Magma Engine, 4 on the Supercharged.
2. **Firebox** (`firebox`): a faint warm lava bed while it burns.
3. **Supercharger whine** (`blower`): the Supercharged engine only, a thin soft whine above the chugs.
4. **Lava pop** (`lava_pop`): Minecraft's own lava pop, now and then, as on the Lava Generator.

## Personality

| Trait | Sound |
|---|---|
| Eager | it catches fast: a clack, a whoomph and quickening chugs |
| Punchy | a firm low thump on every stroke, no wobble |
| Cheeky | it sputters out with two coughs rather than just going quiet |
| Supercharged | the same chug twice as fast, a touch higher, with the whine on top |

## Events

### chug
**Purpose** · it's running, and how fast
**Sound Layers** · mechanical: a low piston thump (85 → 50 Hz) · material: a soft copper exhaust puff · a tiny valve tick
**Timing** · 0.15 s one-shot, at the top of each piston's stroke
**Variations** · 4 variants, pitch ±5%, volume ±10%; the Supercharged engine plays them a little higher (pitch about 1.1)
**Implementation Notes** · client-side from the block entity's client tick, whenever the crank angle passes 0° or 180° (so it speeds up and stops with the shaft); `BLOCKS`; background (quiet), attenuation distance 8. The smoke puff from that cylinder's exhaust spawns on the same tick

### firebox
**Purpose** · there's lava burning
**Sound Layers** · low warm hum (55 Hz) · a faint dark noise body · the odd crackle
**Timing** · continuous loop
**Variations** · one loop; fades in and out with `lit`
**Implementation Notes** · tickable `SoundInstance` while `lit`; `BLOCKS`; background, attenuation distance 8

### blower
**Purpose** · the supercharger is spinning
**Sound Layers** · mechanical: a soft rotor whine (1150 Hz) over a low hum · a thin air hiss
**Timing** · continuous loop
**Variations** · one loop; its volume follows the shaft speed
**Implementation Notes** · Supercharged engine only; tickable `SoundInstance` while the shaft turns; `BLOCKS`; background, attenuation distance 8

### lava_pop
**Purpose** · life in the lava tank
**Sound Layers** · Minecraft's own lava pop
**Timing** · 0.13 s one-shot
**Variations** · vanilla's own, a little pitch spread from the code
**Implementation Notes** · `sounds.json` entry `magma_engine.lava_pop` referencing `minecraft:block.lava.pop` as an event (nothing copied); rare (about one every 10–20 s) while lit, replacing today's vanilla `LAVA_AMBIENT` call; quiet

### start
**Purpose** · the lava has caught and it's running
**Sound Layers** · mechanical: a latch clack · fire: a rising whoomph · the first three chugs, quickening · a warm swell
**Timing** · clack 0 s → whoomph 0.05 s → chugs 0.3, 0.55, 0.74 s; about 0.9 s in all, then the running chugs take over
**Variations** · 2 variants, pitch ±4%
**Implementation Notes** · when `lit` turns on; `BLOCKS`; loud

### stop
**Purpose** · it's run out of lava and stopped
**Sound Layers** · two coughs (a thump and a puff each, the second weaker) · a heavy clunk as it stops · one tick
**Timing** · cough 0 s, cough 0.38 s, clunk 0.8 s, tick 1.0 s
**Variations** · 2 variants, pitch ±4%
**Implementation Notes** · when `lit` turns off; `BLOCKS`; loud

### cool_tick
**Purpose** · the drum is cooling down
**Sound Layers** · iron ticks, quieter each time
**Timing** · two ticks 0.3 s apart
**Variations** · 3 variants, pitch ±6%
**Implementation Notes** · a few times in the 10 s after it stops; `BLOCKS`; very quiet

## Clips

| Clip | Time (s) | Event |
|---|---|---|
| run | 1.0 | start |
| run | 1.2, 2.7, 4.2, 5.7 | firebox (the loop, sketched in 1.5 s pieces) |
| run | every 0.47 s from 1.47 to 6.16 | chug (each piston at the top of its stroke, 64 RPM) |
| run | 3.6 | lava_pop |
| run | 6.5 | stop |
| run | 7.4 | cool_tick |
| run_super | as `run`, with chugs every 0.23 s from 1.23 (128 RPM) and the blower from 1.3 | start, firebox, blower, chug, lava_pop, stop, cool_tick |
| idle | 0.2 | cool_tick |
