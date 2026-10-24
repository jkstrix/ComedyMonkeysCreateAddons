# Sound sources: Geothermal Turbine

The turbine's real sounds use recordings from the CC0 packs already in `design/sound/sources/` (read only; that folder belongs to the Trade Bot's sound work and isn't changed). The start, the running whine and the power-down are local Stable Audio 3 generations (prompt, seed and licence in `sources/generated/SOURCES.md`). Everything else is synthesised by the sound designer skill's scripts.

| Pack | By | Licence | URL |
|---|---|---|---|
| Stable Audio 3 generations (`sources/generated/`) | generated locally (audio-studio) | Stability AI Community License (free mod, under US$1M revenue) | https://stability.ai/community-license-agreement |
| Impact Sounds | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/impact-sounds |
| RPG Audio | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/rpg-audio |
| Interface Sounds | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/interface-sounds |
| UI Audio | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/ui-audio |

Never add Minecraft's or other mods' sound files.

## Recordings used per event

| Event | Variants | Recordings | Synth layers |
|---|---|---|---|
| `turbine_whine` | 1 | generated: sa3_lava_generator_start_v2.wav (its tail, looped) | — |
| `steam_chuff` | 4 | impact-sounds: impactSoft_medium_000–003 | whoosh |
| `firebox_roar` | 1 | — | hum, noise, rustle |
| `relay_clack` | 3 | rpg-audio: metalLatch, metalClick | clink |
| `boiler_groan` | 3 | rpg-audio: creak1–3; impact-sounds: impactMetal_heavy_000, 002, 004 | — |
| `ignite` | 2 | rpg-audio: metalClick | whoosh, swell, rustle |
| `lamp_on` | 1 | ui-audio: switch12 | hum |
| `spin_up` | 1 | generated: sa3_lava_generator_start_v2.wav | — |
| `spin_down` | 1 | generated: sa3_generator_stop_v1.wav | — |
| `settle` | 2 | impact-sounds: impactMetal_heavy_001, 003; interface-sounds: tick_002 | — |
| `stall` | 1 | rpg-audio: metalLatch; impact-sounds: impactBell_heavy_003 | whoosh |
| `cool_tick` | 4 | interface-sounds: tick_001, 002, 004 | — |
| `form` | 1 | impact-sounds: impactPlate_heavy_000, 002, 004; impactBell_heavy_002 | whoosh |
| `unform` | 1 | impact-sounds: impactMetal_heavy_002, impactPlate_heavy_001 | servo |
