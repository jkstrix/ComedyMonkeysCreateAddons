# Sound Design: Lava Generator

Brief: `design/briefs/lava-generator.md`. Model and clips: `design/models/electric.bbmodel` (`run`, `idle`, shared with the Electric Motor), timings from `electric.gen.js`: fires up at 1.0 s, the load rises to full by 3 s, holds to 5 s and eases to 6.5 s; the power stops at 7.5 s and the wheel coasts; the fire goes out at 9.5 s; the clip ends at 10.5 s. Cues: `lava-generator.cues.json`.

## Identity

The turbine's little brother: the same firebox-and-wheel idea in one block, so it shares the turbine's sound family at a smaller size. **Higher, lighter and busier**: a little wheel that whirs rather than whines, a firebox that crackles rather than roars, and lava that bloops behind the glass.

- **Mechanical identity**: a small bladed wheel on a shaft (a whir whose pitch follows speed, and so the load).
- **Material identity**: a steel firebox (soft thuds, cooling ticks), lava (bloops and sizzles), the brass-ringed window and copper terminals (light clicks).
- **Emotional identity**: eager and hard-working. Quick attacks, and a whir that rises when you ask more of it.

## Running all day

These run in a base 24/7, so every running sound is **background**: quiet, low, soft-edged and never hissy, and it carries only a short way (`attenuation_distance` 8 in `sounds.json`, half the default 16). Starting up and shutting down can be **loud**: they're rare and they tell you something changed. The wind-up rises full and then settles into the quiet running loop as it reaches speed.

## Signature sounds

1. **Wheel whir** (`whir`): the wheel's tone, a soft little cousin of the turbine's whine (900 Hz, no hiss). Its pitch follows rotor speed, so you can hear the load.
2. **Firebox burn** (`burn`): a faint warm bed with only the odd crackle, far quieter than the turbine's roar.
3. **Lava pop** (`lava_pop`): Minecraft's own lava pop, now and then, with the lava particles.

Every event carries one of them: ignition has the burn's crackle, spin-up and spin-down are the whir sliding, going out is the fire's last hiss.

## Personality

| Trait | Sound |
|---|---|
| Eager | quick ignition, a whir that rises fast |
| Hard-working | pitch follows load: busier when more power is taken |
| Small | fundamentals around 60–140 Hz, never the turbine's 55 Hz weight |
| Family | the same layer types as the turbine (servo whine, hum, rustle crackle, cooling ticks) |

## Events

### whir
**Purpose** · how fast the wheel turns, and so how much power is being taken
**Sound Layers** · mechanical: soft rotor whir (servo 900 Hz) · material: steel housing hum (130 Hz)
**Timing** · continuous loop
**Variations** · one loop; in game its pitch follows rotor speed (about 0.6 at a light load to 1.0 flat out) and its volume follows speed
**Implementation Notes** · tickable `SoundInstance` while the wheel turns, pitch and volume from the rotor speed (the new load-following speed); `BLOCKS`; background (about 0.12 volume flat out, less at light load), attenuation distance 8

### burn
**Purpose** · the firebox is burning lava
**Sound Layers** · low warm hum (60 Hz) · a faint dark noise body · the odd crackle
**Timing** · continuous loop
**Variations** · one loop; fades in and out with `lit`
**Implementation Notes** · tickable `SoundInstance` while `lit`; `BLOCKS`; background (about 0.1 volume), under the whir, attenuation distance 8

### lava_pop
**Purpose** · there's lava in the firebox; life behind the glass
**Sound Layers** · Minecraft's own lava pop (user's call: "use lava sound from minecraft's own library")
**Timing** · 0.13 s one-shot
**Variations** · vanilla's own, with a little pitch spread from the code
**Implementation Notes** · `sounds.json` entry `lava_generator.lava_pop` referencing `minecraft:block.lava.pop` as an event (nothing copied), with our subtitle and attenuation distance 8; client-side from `animateTick` with the lava particles out of the window, replacing today's `LAVA_AMBIENT`; `BLOCKS`; quiet (about 0.3 volume) and rare (about one every 10–20 s)

### ignite
**Purpose** · it caught: lava is burning and it's making power
**Sound Layers** · mechanical: a light click · fire: a rising whoosh · a warm swell · a few crackles
**Timing** · click 0 s → whoosh 0.05 s → swell 0.1 s → crackles 0.2 s; 0.7 s in all
**Variations** · 2 variants, pitch ±4%
**Implementation Notes** · when `lit` turns on; `BLOCKS`

### spin_up
**Purpose** · the wheel is winding up
**Sound Layers** · mechanical: whir rising from 220 to 900 Hz · a light ratchet as it catches
**Timing** · 1.8 s
**Variations** · one
**Implementation Notes** · in game the whir loop's pitch ramp does this job; the one-shot plays when `lit` turns on, under the loop fading in

### spin_down
**Purpose** · the load has gone or the fire is out: the wheel coasts
**Sound Layers** · mechanical: whir falling from 900 to 180 Hz · one tick as it stops
**Timing** · 1.8 s, the tick at 1.7 s
**Variations** · one
**Implementation Notes** · in game the whir loop's pitch and volume follow the coasting wheel; the one-shot plays when it drops to a stop

### fire_out
**Purpose** · the fire has gone out (no lava, or nothing taken for a while)
**Sound Layers** · mechanical: a light click · fire: a falling hiss · a soft steel thud
**Timing** · 0.85 s
**Variations** · 2 variants, pitch ±4%
**Implementation Notes** · when `lit` turns off; `BLOCKS`

### cool_tick
**Purpose** · the firebox is cooling down
**Sound Layers** · material: steel ticks, quieter each time
**Timing** · two ticks 0.27 s apart
**Variations** · 3 variants, pitch ±6%
**Implementation Notes** · a few times in the 10 s after going out, then never; `BLOCKS`; very quiet

## Clips

| Clip | Time (s) | Event |
|---|---|---|
| run | 1.0 | ignite, spin_up |
| run | 1.3, 2.8, 4.3, 5.8, 7.3, 8.8 | burn (the loop, sketched in 1.5 s pieces) |
| run | 2.6, 4.1, 5.6 | whir (the loop at speed) |
| run | 4.2, 8.6 | lava_pop |
| run | 7.5 | spin_down |
| run | 9.5 | fire_out |
| run | 10.2 | cool_tick |
| idle | 0.2 | cool_tick |
