# Fish Tank: Sound Design

Stage 1 (placeholders on the board). Brief: `design/briefs/fish-tank.md`; clips: `design/models/fish-tank.gen.js`; cues: `fish-tank.cues.json` (written by `../monster-cage/make-cues.js`). Levels follow `design/sound/staging.md`.

## Identity

- **Mechanical**: a small air pump on the back wall, chugging with the shaft; an air stone; a hinged feeder in the lid.
- **Emotional**: calm, the opposite of the cage: the block you put in your living room.
- **Movement**: the fish themselves stay silent, as in vanilla; the water and the pump carry it.

## Signature sounds

1. **The aquarium pump** (`pump_loop`): a soft electric diaphragm hum with a slow chug.
2. **Bubbles** (`bubbles_loop`): the air stone, shared with the Flooded Monster Cage.
3. **Fish Food sprinkle** (`feeder_sprinkle`): the lid hatch clicks, flakes patter on the water.

## Sources (the real-first ladder)

Rung 1 (Minecraft's own, never shipped): `fish_birth` (bubble pop), `net_drop` (fish splash), `scoop` (bucket fill fish). Rung 3 in stage 2: a real aquarium air pump, an air stone bubbling, fish flakes sprinkled on water, a small plastic or tin hatch.

## Events

### pump_loop
**Purpose** · the tank is running · **Sound Layers** · diaphragm hum, a soft motor whir, a slow chug per stroke · **Timing** · loop · **Implementation Notes** · looping instance while the shaft turns, pitch following speed a little (0.9–1.1); **B − 10 (−46 LUFS)**, 8 blocks: the user asked for it at least half as loud (2026-10-27), since a tank runs all day in a living room · **Source** · rung 3: an aquarium air pump recording

### bubbles_loop
**Purpose** · there's air going in · **Sound Layers** · bubble bed and pops · **Timing** · loop · **Implementation Notes** · while running; B − 3, 8 blocks · **Source** · rung 3: air stone; rung 1 candidate (`ambient/underwater/additions/bubbles`)

### feeder_sprinkle / lid_feed
**Purpose** · food went in · **Sound Layers** · hatch click; flakes pattering, two tiny plops · **Timing** · 0–0.5 s · **Implementation Notes** · one-shot when Fish Food is inserted, once a second at most; B + 4, 8 blocks · **Source** · rung 3: flakes or rice sprinkled on water; small hatch

### fish_birth / net_drop / scoop
**Purpose** · a new fish; a catch is waiting; a fish scooped up · **Implementation Notes** · vanilla `block.bubble_column.bubble_pop` (quiet, 0.5), `entity.fish.swim`, `item.bucket.fill_fish` · **Source** · rung 1

## Clips

| Clip | Time (s) | Event |
|---|---|---|
| life | 0, 2, 4, 6 | pump_loop |
| life | 0, 3, 6 | bubbles_loop |
| life | 0, 4 | feeder_sprinkle |
| life | 2, 4, 6 | net_drop |
| pump | 0 | pump_loop |
