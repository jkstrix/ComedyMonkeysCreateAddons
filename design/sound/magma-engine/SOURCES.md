# Sound sources: Magma Engines

The real sounds use recordings from the CC0 packs already in `design/sound/sources/` (read only). Everything else is synthesised by the sound designer skill's scripts.

| Pack | By | Licence | URL |
|---|---|---|---|
| Impact Sounds | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/impact-sounds |
| RPG Audio | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/rpg-audio |
| Interface Sounds | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/interface-sounds |
| BigSoundBank (`sources/bigsoundbank/`) | Joseph Sardin (bigsoundbank.com) | CC0 (public domain) | "Beer compressor #1" https://bigsoundbank.com/beer-compressor-1-s3091.html (the user's pick for the supercharger); "Electric hand mixer #6" https://bigsoundbank.com/electric-hand-mixer-6-s1757.html |

**Lava pop** is Minecraft's own `minecraft:block.lava.pop`, referenced from `sounds.json` as an event, so the game plays its own file and nothing is copied. `real/lava_pop.ogg` is a local preview made from the game assets for the soundboard; it's git-ignored and never exported.

Never add Minecraft's or other mods' sound files to the project.

## Recordings used per event

| Event | Variants | Recordings | Synth layers |
|---|---|---|---|
| `chug` | 4 | impact-sounds: impactSoft_heavy_000.ogg, impact-sounds: impactSoft_heavy_001.ogg, impact-sounds: impactSoft_heavy_002.ogg, impact-sounds: impactSoft_heavy_003.ogg | whoosh |
| `firebox` | 1 | — | hum, noise, rustle |
| `blower` | 1 | bigsoundbank: beer_compressor_1.ogg (4.2–8.0 s, pitched up 1.1, then the user's tweaks: pitch 1.67, high-pass 970 Hz, echo) | — |
| `start` | 2 | rpg-audio: metalLatch.ogg, impact-sounds: impactSoft_heavy_000.ogg, impact-sounds: impactSoft_heavy_001.ogg, impact-sounds: impactSoft_heavy_002.ogg, impact-sounds: impactSoft_heavy_003.ogg | whoosh, swell |
| `stop` | 2 | impact-sounds: impactSoft_heavy_000.ogg, impact-sounds: impactSoft_heavy_001.ogg, impact-sounds: impactSoft_heavy_002.ogg, impact-sounds: impactSoft_heavy_003.ogg, impact-sounds: impactMetal_heavy_001.ogg, impact-sounds: impactMetal_heavy_003.ogg, interface-sounds: tick_002.ogg | whoosh |
| `cool_tick` | 4 | interface-sounds: tick_001.ogg, interface-sounds: tick_002.ogg, interface-sounds: tick_004.ogg | — |
| `lava_pop` | vanilla | Minecraft `block.lava.pop` (referenced, not copied) | — |
