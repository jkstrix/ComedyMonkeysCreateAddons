# Monster Cage: Sound Design

Stage 1 (placeholders on the board). Brief: `design/briefs/monster-cage.md`; spec: `design/specs/monster-cage-and-fish-tank.md`; clips: `design/models/monster-cage.gen.js`; cues: `monster-cage.cues.json` (written by `make-cues.js`). Levels follow `design/sound/staging.md` (running bed B = −36 LUFS).

## Identity

- **Mechanical**: a Create kennel: iron bars, a padlock and hasp, a chute dropping kibble into a steel bowl; on the Netherite tier, an electric containment field and a mechanical klaxon.
- **Emotional**: menace held in check, with a wink: grumbling monsters behind bars, and a dog bowl of Monster Chow.
- **Movement**: the horde is heard, not the cage: its own monsters' sounds, kept quiet.

## Signature sounds

1. **The stored monster's own voice** (`monster_grumble`): whatever is in the cage, played from its own ambient sound, quiet and rare (user: "the stored mob, but quieter so it's not annoying").
2. **Iron bars** (`bars_rattle`): a shove against heavy forged bars.
3. **Kibble in a steel bowl** (`chow_pour`): the joke of the name, heard every time chow goes in.
4. **The containment hum and klaxon** (Netherite): a steady electric hum, and an old mechanical "awooga" horn when it fails.

## Sources (the real-first ladder)

Rung 1 (Minecraft's own, referenced from `sounds.json` or played as vanilla events, never shipped): `monster_grumble` (the stored monster's ambient), `cull` (the pens' sweep), `trident_throw`, `drowning`, `breakout` (Wither spawn), `chow_eat`, `monster_belch`. Everything else gets real CC0 recordings in stage 2 (rung 2: the project's Kenney and BigSoundBank library; rung 3: new BigSoundBank or Freesound CC0 searches for iron bars, a padlock, kibble in a metal bowl, a letterbox flap, an electrical hum, sparks, a power-down and a klaxon). Synth or AI only if those come up empty, with the reason written here.

## Events

### monster_grumble
**Purpose** · there's a monster in there, and which kind · **Sound Layers** · the stored monster's own ambient sound, nothing added · **Timing** · its own length (about 1 s) · **Variations** · all of the monster's own ambient variants; pitch 0.95–1.05 · **Implementation Notes** · one-shot from the block entity at volume 0.35, at most every 10–30 s per cage (random), cages within 8 blocks share one cooldown; `BLOCKS`, 16 blocks; only while running · **Source** · rung 1: the stored monster's `getAmbientSound()`

### bars_rattle
**Purpose** · the bars are real and heavy · **Sound Layers** · iron clang, a rattle in the bars' fixings, a small high clink · **Timing** · clang 0, rattle 0.02–0.34, clink 0.06 · **Variations** · 3; pitch ±6% · **Implementation Notes** · one-shot when a new monster arrives and now and then while running (every 20–60 s); B + 3, 8 blocks · **Source** · rung 2/3: iron bars or jail cell recordings

### chow_pour
**Purpose** · chow went in; the bowl joke · **Sound Layers** · dry kibble rattle, the bowl's ring, a few kibble clinks · **Timing** · 0–0.8 s · **Variations** · 3 · **Implementation Notes** · one-shot when chow is inserted (by hand or automation, at most once a second); B + 4, 8 blocks · **Source** · rung 3: dry pet food poured into a metal bowl

### arrival
**Purpose** · the horde grew by one (and ate) · **Sound Layers** · a short chow crunch, a soft thud, a bars clang · **Timing** · crunch 0–0.5, thud 0.3, clang 0.34 · **Variations** · 3 · **Implementation Notes** · one-shot each time a monster joins; B + 2, 8 blocks · **Source** · rungs 2–3 (crunch, thud, iron)

### jar_in / jar_out
**Purpose** · a monster locked in, or taken out · **Sound Layers** · padlock click, hasp clank, a glassy jar ping (in); lock click, clink, a short air whoosh (out) · **Timing** · 0–0.4 s · **Variations** · 2 each · **Implementation Notes** · one-shot on use; B + 8, 16 blocks · **Source** · rung 3: padlock recordings; rung 2: Kenney metal and glass

### cull
**Purpose** · the cull landed · **Sound Layers** · the vanilla sweep, then `bars_rattle` · **Timing** · sweep 0, bars 0.08 · **Implementation Notes** · as the pens play the sweep · **Source** · rung 1 (sweep) + bars

### drop_tray
**Purpose** · drops are waiting · **Sound Layers** · a light metal clink, a small thud · **Timing** · 0–0.2 s · **Variations** · 4 · **Implementation Notes** · one-shot when an item lands in an empty tray slot; B, 8 blocks · **Source** · rung 2: Kenney `impactTin` / `impactMetal_light`

### bubbles_loop (Flooded)
**Purpose** · there's water in there · **Sound Layers** · a soft bed of bubbles and a few pops · **Timing** · loop · **Implementation Notes** · looping sound instance while running with water; B − 3, 8 blocks · **Source** · rung 3: an aquarium air stone (shared with the Fish Tank), or rung 1 (`ambient/underwater/additions/bubbles`) as a candidate

### hatch_open / trident_throw / hatch_shut (Flooded)
**Purpose** · a trident is coming at you · **Sound Layers** · a spring creak and latch click; the vanilla trident throw; a flap clank with a small spring bounce · **Timing** · open 0, throw 0.12 after, shut 0.85 after · **Implementation Notes** · one-shots with each throw; open and shut B + 6, 16 blocks · **Source** · rung 3 (letterbox flap, spring), rung 1 (throw)

### drowning (Flooded)
**Purpose** · the zombie turned · **Implementation Notes** · vanilla `entity.zombie.converted_to_drowned`, once · **Source** · rung 1

### field_hum (Netherite)
**Purpose** · the field is up · **Sound Layers** · a 60 Hz electrical hum with a faint high whine · **Timing** · 2 s loop · **Implementation Notes** · looping instance while the field is up; B, 8 blocks · **Source** · rung 4, by the user's choice: their own Stable Audio 3 take (seed 501191784), raw and cleaned, picked over the CC0 transformer and neon hums that were on the board (`sources/generated/SOURCES.md`)

### field_cutout (Netherite)
**Purpose** · the field is failing · **Sound Layers** · the same hum, dropping to a faint buzz and surging back in the shield's flicker pattern · **Timing** · 3 s, from the countdown's start · **Implementation Notes** · in game this is the `field_hum` loop's volume following the shield's flicker (5% while out, a short surge on return), so the sound and the shield always agree; the file previews it on the board and in the clip; sparks (`field_flicker`) land on each drop-out · **Source** · rung 4, made from the user's hum take

### field_up / field_flicker / field_down (Netherite)
**Purpose** · power came, wavers, or went · **Sound Layers** · contactor click and a rising swell (up); a spark crackle with a hum dip (flicker); a click, a falling whine and a spark (down) · **Timing** · up 1.2 s, flicker 0.15 s, down 1.4 s · **Variations** · flicker 4 · **Implementation Notes** · up and down B + 10, 16 blocks; flicker on each drop-out during a countdown, B + 4 · **Source** · rung 3: power-on / power-down and spark recordings; rung 2 (the Battery's approved arc) as a candidate

### klaxon (Netherite)
**Purpose** · get back here and fix the power · **Sound Layers** · an old mechanical horn ("awooga") · **Timing** · 0.8 s a blast; through the 5-minute countdown in bursts that speed up: three blasts at the start, three every 30 s, every 10 s in the last minute, continuously (every 1.6 s) in the last 10 s · **Implementation Notes** · loud on purpose: B + 12, 32 blocks, only while a player is within 128 blocks (silent while the countdown is stopped) · **Source** · rung 3: a real klaxon horn

### breakout (Netherite)
**Purpose** · it's out · **Sound Layers** · the vanilla Wither spawn, plus a cage-burst clang · **Implementation Notes** · vanilla spawn as the game plays it; our clang B + 12 · **Source** · rung 1 + rung 3 (heavy metal crash)

### jar_catch / chow_eat / monster_belch / chow_breath (items)
**Purpose** · catching; eating Monster Chow and its side effects · **Sound Layers** · a whoosh into the jar, the clamp snapping shut, a glass clink; vanilla eating; a random monster's ambient from the player (25%); a breathy puff (5%) · **Implementation Notes** · `PLAYERS`, 16 blocks; the belch is heard by others · **Source** · rung 3 (clamp), rung 1 (eat, belch), puff rung 3 (breath recordings)

## Clips

| Clip | Time (s) | Event |
|---|---|---|
| life | 0, 2, 4, 6 | field_hum (loop) |
| life | 0, 3, 6 | bubbles_loop |
| life | 1.2, 5.6 | monster_grumble |
| life | 2, 4, 6 | drop_tray |
| life | 4.8 | bars_rattle |
| trident_hatch | 0.3 / 0.42 / 1.15 | hatch_open / trident_throw / hatch_shut |
| containment | 0 | field_hum |
| containment | 2 | field_cutout |
| containment | 2, 3.6, 5.2 | klaxon |
| containment | 2, 2.3, 2.8, 3.35, 3.9, 4.25, 4.7 | field_flicker |
| containment | 5 | field_down |
