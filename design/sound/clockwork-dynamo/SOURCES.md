# Sound sources: Clockwork Dynamo

Every recording used is **CC0** (BigSoundBank by Joseph Sardin; Kenney packs). Nothing from Minecraft, other mods or Epidemic Sound, and nothing AI-generated. Springs (boings), the warm hum under the transformer and the ratchet are synthesised by the sound designer skill's scripts.

## By rung (real-first ladder)

- **Rung 1, Minecraft's own:** none fits. Searched: the beacon and conduit ambients (recognisably those blocks), minecart rolling, piston.
- **Rung 2, the project library:**
  - BigSoundBank #2118 [Neon or transformer](https://bigsoundbank.com/neon-or-transformer-s2118.html) as cut for the lights (`cuts/loop_transformer.wav`, `cuts/loop_neon.wav`)
  - BigSoundBank #1263 [Car antenna, exit](https://bigsoundbank.com/car-antenna-exit-s1263.html), its small motor's start, run and stop (`cuts/lt_antenna_start.wav`, `lt_antenna_run.wav`, `lt_antenna_stop.wav`), taken lower and darker than the lights use them
  - Kenney RPG Audio `metalLatch`, `metalClick`; Interface Sounds `tick_004`; Impact Sounds `impactMetal_light_00x`; UI Audio `switch12`, `switch13`
  - The Trade Bot's approved `aerial_boing` recipe and the Electric Motor's approved `adjust` recipe, reused
- **Rung 3, new CC0 recording:** BigSoundBank #1498 [Electric shutter, opening](https://bigsoundbank.com/electric-shutter-opening-s1498.html) (`bigsoundbank/1498.ogg`), its relay click and run-up (0–0.8 s), for `start`. Searched with no result: "dynamo", "electric motor", "small motor", "whirring", "turbine" (no dynamo recording exists on BigSoundBank).
- **Rung 4, synth:** the springs (boings) in the aerial sounds, as approved on the Trade Bot (no recording of a tiny aerial spring); a soft 100 Hz hum under the transformer for warmth; the motor's ratchet.

## Recordings used per event

| Event | Variants | Recordings | Synth layers |
|---|---|---|---|
| `hum` | 1 | loop_transformer, lt_antenna_run | hum |
| `start` | 2 | 1498 (shutter relay and run-up), tick_004, loop_transformer | — |

Auditioned and not picked (2026-10-31): a neon take of the hum (`loop_neon`), and an antenna-motor take of the start (`metalLatch`, `metalClick`, `lt_antenna_start`).
| `stop` | 2 | loop_transformer, lt_antenna_stop, metalLatch, metalClick | — |
| `aerial_up` | 2 | impactMetal_light_000–004 | boing |
| `aerial_boing` | 3 | impactMetal_light_000–004 (the Trade Bot's) | boing |
| `aerial_fold` | 2 | impactMetal_light_000–004 | boing |
| `adjust` | 2 | switch12, switch13 (the motor's) | ratchet |
