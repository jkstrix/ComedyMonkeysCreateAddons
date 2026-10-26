# Battery and Industrial Battery: Sound Design

Brief: `design/briefs/battery.md` · spec: `design/specs/battery.md` · model: `design/models/battery.bbmodel` (clips `charging`, `giving`, `forming`, `lightning`) · levels: `design/sound/staging.md`.

The user (2026-10-26): "lets get the sounds done, keep them muted and simple, the smaller ones should probably be silent in use".

## Identity

A battery doesn't move, so its sound is the electricity itself, kept to the bare minimum: a substation's low mains hum when power is moving, the heavy clunk of a contactor when the big one switches in or out, and the crack of an arc when lightning comes down the rod.

- **The plain Battery is silent in use.** It places and breaks with the normal metal block sounds and makes nothing of its own: a bank of Batteries in a base never adds to the noise.
- **The Industrial Battery** has four events, all through the same three signature sounds.

## Signature sounds

1. **Mains hum**: a low, steady transformer hum (a fundamental near 100 Hz with a soft second harmonic), the sound of a substation. Only while power flows in or out.
2. **Contactor clunk**: a big switch closing or opening, a dull steel thunk with a short ring, no clatter.
3. **Arc crack**: a short electric crackle, sharp at the front and gone in a moment, over a low thump.

## Events

### industrial_battery.hum
**Purpose** · power is moving through the battery (charging or giving); silence means it's idle or full and nothing is flowing.
**Sound Layers** · mechanical: none · material: transformer core hum (100 Hz, a quieter 200 Hz) · personality: steady, calm.
**Timing** · a seamless loop; fades in over 0.5 s when flow starts and out over 1 s when it stops.
**Variations** · one loop; the code nudges its volume (0.7 to 1) and pitch (0.97 to 1.03) with how much is flowing, so a hard charge hums a touch louder and higher.
**Implementation Notes** · looping block sound from the bottom-front-middle block while the network moves power through it; category blocks; the running bed (B, −36 LUFS), range 12 (a multiblock). No hum from plain Batteries.

### industrial_battery.form
**Purpose** · the 3×3 column has just become an Industrial Battery.
**Sound Layers** · mechanical: contactor clunk · material: heavy steel thud with a short metallic ring · personality: a confident switch-in, then the hum swelling up.
**Timing** · clunk at 0 (0.2 s), ring 0.05–0.4 s, hum swelling 0.15–1.2 s (as the cyan ripple climbs the column).
**Variations** · one take; pitch ±3%.
**Implementation Notes** · one-shot at the centre of the tower when it forms (including re-forming smaller after a break); category blocks; forming a multiblock (about B + 12), range 16.

### industrial_battery.unform
**Purpose** · it has come apart (a block broken), back to plain Batteries.
**Sound Layers** · mechanical: contactor opening, a lighter clack · material: steel · personality: the hum falling away.
**Timing** · clack at 0 (0.15 s), hum falling 0.05–0.8 s.
**Variations** · one take; pitch ±3%.
**Implementation Notes** · one-shot at the tower's centre; category blocks; starting and stopping (B + 9), range 16. When it re-forms smaller straight away, only `form` plays.

### industrial_battery.strike
**Purpose** · lightning came down the rod and charged it (on top of the vanilla thunder).
**Sound Layers** · mechanical: none · material: arc crackle (short noise bursts) · personality: a sharp crack, then a low thump into the battery.
**Timing** · crack at 0 (0.25 s, three fast bursts), thump 0.02–0.3 s, a faint crackle tail to 0.6 s.
**Variations** · two takes; pitch ±5%.
**Implementation Notes** · one-shot at the rod when a strike is caught; category blocks; forming level (about B + 12), range 16.

## Clips

| Clip | Time (s) | Event |
|---|---|---|
| charging | 0 | hum (loop) |
| giving | 0 | hum (loop) |
| forming | 0.4 | form (as the ripple starts) |
| lightning | 0.3 | strike (as the arcs show) |
