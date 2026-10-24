# Sound Design: Geothermal Turbine

Brief: `design/briefs/geothermal-turbine.md`. Model and clips: `design/models/geothermal-turbine.bbmodel` (`run`, `idle`), timings from `geothermal-turbine.gen.js` (`RUN`: ignition 1.0 s, full speed 3.5 s, stall 6.5 s, end 9.0 s). Cues: `geothermal-turbine.cues.json`.

## Identity

A power station in a box. It should sound **big, steady and proud**: the biggest machine in the mod, built by hand from iron, brass and copper. Its sound is about **weight and rhythm**, not sci-fi hum: a turbine that winds up with effort, a firebox that roars, steam that chuffs like a heartbeat, and a brass relay that clacks when something changes.

- **Mechanical identity**: a bladed rotor on a shaft (whine that rises with speed), a riveted boiler (groans under pressure), relays and gauges on the panel (clacks).
- **Material identity**: dark iron (low thuds, groans), brass (the relay and the stall ping), copper (the generator's lamp buzz), steam (chuffs and the relief hiss).
- **Emotional identity**: proud and dependable. Confident attacks, steady rhythm; when it stalls it sighs rather than alarms.

## Signature sounds

1. **Turbine whine** (`turbine_whine`): the rotor's tone. Its pitch follows speed, so a player can hear how hard it's working.
2. **Steam chuff** (`steam_chuff`): the stack's heartbeat while it runs.
3. **Firebox roar** (`firebox_roar`): low rumble with crackle, the heat.
4. **Relay clack** (`relay_clack`): brass relay on the panel, whenever the state changes.
5. **Boiler groan** (`boiler_groan`): the drum flexing under pressure, now and then.

Almost every event carries one of these: ignition has the relay and the roar, the stall has the relay and steam.

## Personality

| Trait | Sound |
|---|---|
| Powerful | low fundamentals (55–95 Hz) under everything |
| Steady | regular chuffs, even whine, no jitter |
| Proud | confident attacks on the relay and ignition |
| Dependable | the stall is a relief-valve sigh and a soft brass ping, not an alarm |

## Events

### turbine_whine
**Purpose** · how fast it's spinning, and so how much power it's making
**Sound Layers** · mechanical: rotor whine (the bladed wheel) · material: iron housing hum · steam: thin hiss bed
**Timing** · continuous loop
**Variations** · one loop; in game its pitch follows rotor speed (0.6 at a crawl to 1.0 flat out) and its volume follows speed too
**Implementation Notes** · tickable `SoundInstance` on the controller while formed and spinning, pitch and volume from the rotor speed; `BLOCKS`; quiet (0.3 volume flat out, halved after play-testing), default attenuation

### steam_chuff
**Purpose** · it's running; the machine's heartbeat
**Sound Layers** · steam: short falling whoosh · material: soft low bump of the valve
**Timing** · 0.35 s one-shot
**Variations** · 4 variants, pitch ±6%, volume ±10%; interval shortens with load (about 1.2 s idle-load to 0.6 s flat out)
**Implementation Notes** · client-side, from the controller while `lit`, timed with the steam particles at the stack; `BLOCKS`

### firebox_roar
**Purpose** · the firebox is burning lava
**Sound Layers** · low roar (55 Hz) · filtered noise body · sparse crackles
**Timing** · continuous loop
**Variations** · one loop; volume follows `lit` with a short fade in and out
**Implementation Notes** · tickable `SoundInstance` while `lit`; `BLOCKS`; 0.25 volume (halved after play-testing), quieter than the whine. Replaces today's vanilla `LAVA_AMBIENT` pops (keep the occasional lava pop as part of it)

### relay_clack
**Purpose** · a state changed on the panel
**Sound Layers** · mechanical: relay armature · material: brass clink
**Timing** · 0.1 s one-shot
**Variations** · 3 variants, pitch ±4%
**Implementation Notes** · on `lit` changing, and on the stall lamp changing; `BLOCKS`

### boiler_groan
**Purpose** · there's pressure in the drum; life between the big events
**Sound Layers** · iron creak · low knock at the end
**Timing** · 0.55 s creak, then the knock, 0.8 s total
**Variations** · 3 variants, pitch ±8%; random, at most once every 8–15 s while running
**Implementation Notes** · random client one-shot while `lit`; `BLOCKS`; low volume

### ignite
**Purpose** · it fired up: lava and water are both in
**Sound Layers** · relay click (anticipation) · low whoomph as the firebox catches (impact) · warm swell · crackle tail (settle)
**Timing** · click 0 · whoomph 0.08–0.8 · swell 0.15–1.05 · crackle 0.3–1.0
**Variations** · 2 variants
**Implementation Notes** · when `lit` turns on; `BLOCKS`

### lamp_on
**Purpose** · the cyan output lamp lit: it's making power
**Sound Layers** · relay clack · small copper buzz
**Timing** · 0.45 s
**Variations** · 1
**Implementation Notes** · with `ignite`, offset 20 ms

### spin_up
**Purpose** · the rotor winds up from rest
**Sound Layers** · rising whine 160 → 880 Hz over 2.6 s (effort) · iron hum swelling in · bearing ratchet fading out as it gets going
**Timing** · 2.6 s, matching the clip's ease to full speed (1.0 → 3.8 s)
**Variations** · 1 (the in-game loop's pitch ramp does this; this one-shot is for the preview and the first start)
**Implementation Notes** · in game, the `turbine_whine` loop with its pitch ramped by rotor speed does the job; keep this for when it starts from a dead stop

### spin_down
**Purpose** · the rotor coasts down
**Sound Layers** · falling whine 880 → 120 Hz · fading hum
**Timing** · 2.2 s, matching the clip's coast (6.5 → 8.7 s)
**Variations** · 1
**Implementation Notes** · as `spin_up`: the loop's pitch falling with speed, this one-shot for the preview

### settle
**Purpose** · the rotor has stopped
**Sound Layers** · low thud of the shaft settling · one tick
**Timing** · 0.2 s
**Variations** · 2
**Implementation Notes** · when the rotor speed reaches zero

### stall
**Purpose** · it stopped because nothing is taking the power (buffer full)
**Sound Layers** · relay clack (the red lamp) · soft low brass ping · relief-valve steam hiss (the sigh)
**Timing** · clack 0 · ping 0.05–0.65 · hiss 0.25–1.55
**Variations** · 1, so it's recognisable
**Implementation Notes** · when the stall lamp lights; `BLOCKS`; not loud, it isn't an error

### cool_tick
**Purpose** · it ran recently and is cooling down
**Sound Layers** · two small metal ticks
**Timing** · 0.4 s
**Variations** · 4, pitch ±10%; random every 2–5 s for about a minute after it stops
**Implementation Notes** · client one-shot; very quiet

### form
**Purpose** · the 3×3×3 snapped into a turbine
**Sound Layers** · three heavy thuds as it locks together (impact cascade) · a big brass clang · a steam sigh
**Timing** · thuds 0, 0.14, 0.28 · clang 0.45 · sigh 0.7–1.3
**Variations** · 1
**Implementation Notes** · on `setFormed(true)`, at the middle block; `BLOCKS`; with the "Full Steam Ahead" advancement it's the reward moment

### unform
**Purpose** · a part was broken and it came apart
**Sound Layers** · clang · falling servo groan · thud
**Timing** · 0.6 s
**Variations** · 1
**Implementation Notes** · on unforming; `BLOCKS`

## Clips

| Clip | Time (s) | Event |
|---|---|---|
| run | 1.0 | relay_clack, ignite, spin_up |
| run | 1.02 | lamp_on |
| run | 1.5, 3.0, 4.5 | firebox_roar (the loop, in 1.5 s sections) |
| run | 2.2, 3.0, 3.6, 4.2, 4.8, 5.4, 6.0 | steam_chuff (quickening as the load builds) |
| run | 3.5, 5.0 | turbine_whine (the loop at full speed) |
| run | 4.7 | boiler_groan |
| run | 6.5 | stall, spin_down |
| run | 8.7 | settle |
| idle | 0.2 | cool_tick |
| (game only) | — | form, unform |

## In Minecraft

Events register as `comedymonkeysaddons:block.geothermal_turbine.<id>`, category `BLOCKS`. The two loops (`turbine_whine`, `firebox_roar`) are tickable sound instances on the controller, following rotor speed and `lit`; everything else is one-shot. Subtitles as in the cue file.
