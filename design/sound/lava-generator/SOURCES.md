# Sound sources: Lava Generator

The real sounds use recordings from the CC0 packs already in `design/sound/sources/` (read only). Everything else is synthesised by the sound designer skill's scripts.

| Pack | By | Licence | URL |
|---|---|---|---|
| Impact Sounds | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/impact-sounds |
| RPG Audio | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/rpg-audio |
| Interface Sounds | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/interface-sounds |
| UI Audio | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/ui-audio |

**Lava pop** is Minecraft's own `minecraft:block.lava.pop`, referenced from `sounds.json` as an event (`"type": "event"`), so the game plays its own file and nothing is copied. `real/lava_pop.ogg` is a local preview made from the game assets for the soundboard; it's git-ignored and never exported.

Never add Minecraft's or other mods' sound files to the project.

## Recordings used per event

| Event | Variants | Recordings | Synth layers |
|---|---|---|---|
| `whir` | 1 | — | hum, servo |
| `burn` | 1 | — | hum, noise, rustle |
| `ignite` | 2 | rpg-audio: metalClick.ogg | whoosh, swell, rustle |
| `spin_up` | 1 | rpg-audio: metalPot1.ogg | servo, hum |
| `spin_down` | 1 | interface-sounds: tick_002.ogg | servo, hum |
| `fire_out` | 2 | rpg-audio: metalClick.ogg, impact-sounds: impactMetal_medium_001.ogg, impact-sounds: impactMetal_medium_003.ogg | whoosh |
| `cool_tick` | 4 | interface-sounds: tick_001.ogg, interface-sounds: tick_002.ogg, interface-sounds: tick_004.ogg | — |
| `lava_pop` | vanilla | Minecraft `block.lava.pop` (referenced, not copied) | — |
