# Trade Bot: Sound Design

Audio identity for Trade Bot and his station. The character comes from [the brief](../briefs/trade-bot.md); the cue times come from the animation clips in [trade-bot.gen.js](../models/trade-bot.gen.js).
Cue file: [trade-bot.cues.json](trade-bot.cues.json) · Real-sound recipes: [trade-bot.real-recipes.js](trade-bot.real-recipes.js) · Real sounds: [real/](real/) (mono 44.1 kHz OGG, with variants) · Sources and licences: [sources/SOURCES.md](sources/SOURCES.md) (all CC0) · Placeholder sketches: [placeholder/](placeholder/) (approved for timing; never ship them).
**Status:** real sounds approved (2026-10-24) and added to the Blockbench clips: 303 sound keyframes across 41 clips (the scared routine and the turns approved and added the same day), generated from the cue file by [trade-bot.gen.js](../models/trade-bot.gen.js). In Blockbench, play any clip in the Animate tab to hear them.
Listen: serve this folder with `node ~/.claude/skills/character-sound-designer/scripts/serve.js design/sound 8765` and open http://localhost:8765/trade-bot-soundboard.html. Switch between real and placeholder, try the variants, and use ⚙ to tweak a sound live; **Save tweaks** writes `trade-bot.tweaks.json`, which the next real build applies.

## Identity

- **Mechanical:** a clockwork shopkeeper. Escapement ticks, a wind-up key ratchet, small servos, a gyro-balanced rubber wheel, springs everywhere.
- **Emotional:** eager, nosy and a bit clumsy, warm and never harsh. A wordless music-box voice: rising for curiosity, falling for disappointment, quick bright trills for joy.
- **Movement:** rubbery and springy. Every hop is *compress → boing → bdoop → tink*. Landings get heavier with height, and his aerial always has the last word.
- **Materials:** brass (warm pings, the voice's tines), steel (sharper clicks), copper (soft clinks), andesite and wood (knocks and clacks), rubber (muted impacts).
- **Style:** Pixar clockwork, not factory. Small and close, quieter than a villager. No synth lasers or AI bleeps.

## Signature sounds
These recur so you can recognise him from any single cue:
1. **`brass_ping`**: his merchant "ding", heard when he opens for trade and when he finds things.
2. **`clock_tick`**: the escapement in his chest, always faintly there while he idles.
3. **`focus_click`**: the binocular lenses refocusing: curiosity, noticing, deciding.
4. **`key_rattle`**: the wind-up key being wound at his station.
5. **`merchant_trill`**: his happy rising voice.

## Personality → sound
| Trait | Sound |
|---|---|
| Eager | quick attacks, rising pitch: trills, hop chirps, `servo_snap` head turns |
| Nosy | `focus_click`, `curious_chirp`, little lens ticks while he looks |
| Clumsy | rattles after landings, delayed aerial boings, the cobblestone flung without looking |
| Warm | brass and music-box tones, soft rubber impacts, nothing sharp |

## Events

Common rules: one-shots play from the entity's animation clock at the cue time (see the cue table). Category `NEUTRAL`, default attenuation (16 blocks). Small pitch randomisation (±6%) and volume randomisation (±10%) unless noted. Voice cues get a 3 s cooldown outside scripted clips.

### Signature sounds

### clock_tick
**Purpose** · he's clockwork and running; a heartbeat while idle.
**Sound Layers** · mechanical: escapement tick · material: steel/brass click.
**Timing** · 30 ms tick, every 0.5 s while idle (2/s).
**Variations** · 4 variants, alternating tick-tock pitch (−3% / +3%); no volume randomisation.
**Implementation Notes** · a quiet loop driven by the key rotation; only within ~6 blocks and only while idle or docked.

### focus_click
**Purpose** · "I see something": the lenses refocus.
**Sound Layers** · mechanical: tiny servo zip · material: steel click and tick · personality: nosy.
**Timing** · click 0 s, servo 0.01–0.09 s, settle tick 0.08 s.
**Variations** · 3 variants, ±4% pitch.
**Implementation Notes** · whenever his head snaps to a new target; also in the curious expression.

### brass_ping
**Purpose** · the shopkeeper's bell: "open for business" or "found it".
**Sound Layers** · material: warm brass ping with bell partials, plus a high clink · personality: proud.
**Timing** · attack at once, 0.7 s ring-out.
**Variations** · 2 variants, ±2% pitch only (it should stay recognisable).
**Implementation Notes** · when the trade lamp lights; layered inside `found_it`.

### key_rattle
**Purpose** · he's being wound up: cosy, safe, home.
**Sound Layers** · mechanical: ratchet clicks (10/s) · material: brass clink tail.
**Timing** · 0.7 s of ratchet, a clink at the end of each wind.
**Variations** · 3 variants, rate ±10%.
**Implementation Notes** · a loop while docked and winding (tickable sound instance at low volume), synced to the winder head turning.

### merchant_trill
**Purpose** · pure joy and pride.
**Sound Layers** · personality: three rising chirp notes with music-box tines · material: brass ping tail.
**Timing** · notes at 0, 0.07 and 0.13 s; ping at 0.22 s.
**Variations** · 3 melodic variants (different note steps); no pitch randomisation, so the melody stays intact.
**Implementation Notes** · on seeing a player, a completed trade, or a proud landing. 5 s cooldown.

### Movement

### roll_start
**Purpose** · he sets off.
**Sound Layers** · material: rubber rolling on the surface, a soft low rumble whose tread thump speeds up (2→6 per second) as he gets going, with a barely-audible crunch of grit under the tyre · mechanical: a quiet motor underneath.
**Timing** · 0.75 s, easing in over 0.3 s.
**Variations** · 3 variants.
**Implementation Notes** · on starting to move; while moving, a quiet rolling loop with pitch following speed takes over.

### roll_loop
**Purpose** · cruising along: the steady sound of him rolling.
**Sound Layers** · material: rubber rolling on the surface, a soft low rumble with a steady tread thump (6 per second) and a barely-audible crunch of grit.
**Timing** · a seamless 3 s loop (the seam is crossfaded).
**Variations** · 2 loop variants; pitch follows speed in game (about ±15%).
**Implementation Notes** · a tickable looping sound instance while moving: volume and pitch follow speed, and it fades out on stopping, where `roll_stop` takes over. It's quieter than `roll_start`, so long rolls aren't tiring.

### roll_stop
**Purpose** · brakes, with momentum.
**Sound Layers** · material: the rolling rumble slowing (tread thump 6→1.5 per second), a rubber scrub as the tyre grips and slips · settle: a soft thump.
**Timing** · rumble 0–0.45 s, scrub 0.03–0.27 s, settle thump at 0.26 s.
**Variations** · 3 variants; the squeak is louder the faster he was going.
**Implementation Notes** · when deceleration crosses a threshold.

### servo_short / servo_snap
**Purpose** · arm and head moves; a snap is a fast eager turn.
**Sound Layers** · mechanical: a small motor whir with a pitch ramp; the snap adds an end click.
**Timing** · short 0.18 s; snap 0.08 s.
**Variations** · 4 variants each, ±8% pitch.
**Implementation Notes** · use sparingly: only big deliberate moves, never on idle sway.

### hop_launch / hop_launch_big / hop_small
**Purpose** · the hop: anticipation, then spring.
**Sound Layers** · anticipation: a falling servo (spring compress) · mechanical: a spring boing with a decaying wobble · personality: "hup!" chirp (the big one is an effortful "hnnf-hup").
**Timing** · compress 0.12 s (big: 0.22 s), boing at the launch frame; cue 0.12 s (big: 0.22 s) before take-off.
**Variations** · 3 variants; the big one is about 20% lower pitched.
**Implementation Notes** · cue at the start of the crouch. `hop_small` is the happy bounce in place.

### land_soft
**Purpose** · a springy landing: boing, boink, tink.
**Sound Layers** · material: rubber bump ("bdoop") · mechanical: a smaller boink (overshoot) · settle: a faint tink.
**Timing** · bump 0 s, boink 0.03 s, tink 0.15 s, each quieter and further apart.
**Variations** · 4 variants, ±6% pitch.
**Implementation Notes** · on landing (stairs, slabs, small hops).

### land_heavy
**Purpose** · the "oof": a full-block drop or the dock landing.
**Sound Layers** · impact: a low thud and heavy rubber bump · material: a rattle of loose parts · overshoot: a spring boing · settle: a faint tink.
**Timing** · thud 0 s, rattle 0.03–0.28 s, boing 0.05 s, tink 0.3 s.
**Variations** · 3 variants.
**Implementation Notes** · drops over 8 px, and docking.

### drop_whoosh
**Purpose** · falling off an edge: "whee!".
**Sound Layers** · movement: a short air whoosh · personality: a rising "whee" chirp.
**Timing** · 0.25 s.
**Variations** · 3 variants; the chirp only on about half of the drops, so it doesn't become a jingle.
**Implementation Notes** · when he leaves the edge.

### aerial_boing
**Purpose** · the aerial wobble: his secondary motion made audible.
**Sound Layers** · mechanical: a spring boing, then a smaller boink, then a tink.
**Timing** · 0, 0.28 and 0.5 s, each quieter.
**Variations** · 3 variants.
**Implementation Notes** · after big stops, startles and jolts; at most once a second.

### blink
**Purpose** · a sign of life: tiny lid shutters.
**Sound Layers** · material: a steel click down and a tick up.
**Timing** · 0 and 0.12 s, very quiet.
**Variations** · 2 variants.
**Implementation Notes** · only audible within ~4 blocks.

### Voice and reactions

### bonk
**Purpose** · hits a wall: comic impact.
**Sound Layers** · impact: a brass clang · mechanical: a spring twang with a long wobble · material: a rubber bump.
**Timing** · 0 s clang and bump; twang from 0.02 s, ringing 0.6 s.
**Variations** · 2 variants.
**Implementation Notes** · on horizontal collision above walking speed. The strongest one-shot he has; everything else stays under it.

### dizzy
**Purpose** · "seeing stars".
**Sound Layers** · personality: a descending wobbly warble · mechanical: a clockwork whirr (fast ratchet).
**Timing** · 0.67 s of warble, whirr over 0.05–0.85 s.
**Variations** · 2 variants.
**Implementation Notes** · after `bonk`, while his irises spin.

### head_shake
**Purpose** · shaking it off.
**Sound Layers** · mechanical: three servo zips, each quieter.
**Timing** · 0, 0.14 and 0.28 s.
**Variations** · 2 variants.
**Implementation Notes** · after dizzy; also in the cross expression.

### grumble
**Purpose** · cross: "hmph".
**Sound Layers** · personality: a low falling buzz-chirp · material: a muffled rumble.
**Timing** · 0.3 s.
**Variations** · 3 variants.
**Implementation Notes** · glaring at a wall, or when hit.

### double_take
**Purpose** · "wait, what?!", the big notice moment.
**Sound Layers** · mechanical: a click and a spring "boing-thwip" (his neck shooting up) · personality: a startled rising chirp.
**Timing** · click 0 s, boing 0.01 s, chirp 0.05 s.
**Variations** · 2 variants.
**Implementation Notes** · on first noticing a player.

### curious_chirp / puzzled_hmm / huh
**Purpose** · curiosity ("hm?"), puzzlement ("hmm?" with a lens tick), bafflement ("huh").
**Sound Layers** · personality: chirps (rising, rise-and-fall, falling) · mechanical: a focus tick.
**Timing** · 0.15–0.27 s.
**Variations** · 3 variants each.
**Implementation Notes** · the blind-container routine uses `puzzled_hmm` then `huh`.

### startled_chirp
**Purpose** · a little jump of surprise.
**Sound Layers** · personality: a fast upward chirp · mechanical: a short spring boing.
**Timing** · 0.08 s chirp, 0.25 s boing.
**Variations** · 3 variants.
**Implementation Notes** · lights snapping on, poofs, doze-jolts, containers shutting on their own.

### shrug
**Purpose** · "how does this even work?".
**Sound Layers** · material: two shoulder-cog clinks · personality: a small "mm-mm".
**Timing** · clinks at 0 and 0.05 s, voice at 0.08 s.
**Variations** · 2 variants.
**Implementation Notes** · end of the blind-container routine.

### sad_aww / sigh
**Purpose** · disappointment, then the mechanical sigh.
**Sound Layers** · personality: a slow falling tone · mechanical: a slow creak; the sigh is a winding-down servo with an air hiss.
**Timing** · aww 0.38 s; sigh 0.9 s.
**Variations** · 2 variants each.
**Implementation Notes** · when a player walks away; 10 s cooldown, so it stays touching rather than needy.

### snore / sleepy_mmm
**Purpose** · drowsiness.
**Sound Layers** · personality: a tiny low buzz-snore; a sleepy falling "mmm".
**Timing** · 0.4 s each.
**Variations** · 3 variants.
**Implementation Notes** · dozing and docking. The snore only plays at night when docked, every 3–5 s, very quietly.

### pleased_boop / hi_boop / bye_boop
**Purpose** · small social sounds: satisfied, hello, goodbye.
**Sound Layers** · personality: two-note chirps (up-up, up-down, down-down).
**Timing** · 0.16–0.22 s.
**Variations** · 3 variants each.
**Implementation Notes** · counting stock, greeting, waving goodbye to belt items, proud wiggles.

### wave_clink
**Purpose** · a claw wave.
**Sound Layers** · material: a copper clink · mechanical: a light servo.
**Timing** · 0.12 s, one per wave stroke.
**Variations** · 3 variants, ±8% pitch, so a run of waves sounds alive.
**Implementation Notes** · also the claw hooking a chest lid.

### found_it
**Purpose** · the triumph of finding the item.
**Sound Layers** · personality: three rising notes · material: a brass ping (signature) · a small sparkle.
**Timing** · notes 0–0.26 s, ping 0.03 s, sparkle 0.2 s.
**Variations** · 2 variants.
**Implementation Notes** · when he pulls an item out of a chest.

### Lights and station

### light_off / light_on / flicker
**Purpose** · his lights changing: off is a click and a falling "bwip", on is a rising "bwip", flicker is unsteady ticks.
**Sound Layers** · mechanical: a switch click or tick · personality: a short chirp glide.
**Timing** · 0.06–0.1 s.
**Variations** · 2 variants each.
**Implementation Notes** · the iris and lamp state changes; flickers on the dock handover.

### glow_on / glow_off
**Purpose** · the station taking his light: a warm swell into a low hum.
**Sound Layers** · a rising warm tone and a 110 Hz hum (`BLOCKS` category).
**Timing** · swell 0.8 s, hum from 0.5 s; off is a 0.5 s falling swell.
**Variations** · 1 each.
**Implementation Notes** · the hum continues as a quiet loop while docked, breathing with the glow (4 s cycle).

### winder_engage / winder_release
**Purpose** · the brass arm meeting or leaving his key.
**Sound Layers** · material: a brass clink and wooden clack · mechanical: a latch tick; the release adds a swing whir.
**Timing** · 0.12–0.25 s.
**Variations** · 2 variants.
**Implementation Notes** · station block-entity sounds (`BLOCKS`).

### flag_ding
**Purpose** · "there's work!": the call to action.
**Sound Layers** · material: a bright brass ding · mechanical: a flag-spring wobble.
**Timing** · 0.9 s ring-out.
**Variations** · 1 (it's a recognisable bell).
**Implementation Notes** · when a task arrives; audible to 16 blocks.

### Items and containers

### drawer_open / drawer_close
**Purpose** · his shop drawer.
**Sound Layers** · material: a wood-on-metal slide, a rattle of wares, a clack; the close adds a jingle.
**Timing** · open 0.2 s and a stop clack; close 0.1 s.
**Variations** · 3 variants each.
**Implementation Notes** · counting stock, offering trades.

### chest_open / chest_close / barrel_open / barrel_close
**Purpose** · the containers' own sounds (use vanilla ones in game if preferred).
**Sound Layers** · material: a wood creak and clack; a thud on close.
**Timing** · 0.25–0.5 s.
**Variations** · vanilla variants.
**Implementation Notes** · play the vanilla chest and barrel open/close sounds at the container; these are stand-ins for the preview.

### rummage
**Purpose** · digging through a chest.
**Sound Layers** · a rustle of items · material: occasional clinks and a wood knock.
**Timing** · 1.8 s.
**Variations** · 3 variants (random burst pattern).
**Implementation Notes** · while rummaging.

### toss_whoosh / cobble_clack
**Purpose** · the cobblestone flung over his shoulder, and where it lands.
**Sound Layers** · air whoosh; stone clacks and a skitter.
**Timing** · whoosh 0.25 s; clack, a bounce at 0.12 s, skitter to 0.35 s.
**Variations** · 2 variants each.
**Implementation Notes** · flavour in the chest search.

### set_on_metal / set_soft / pat / push_scrape / grip / claw_fidget
**Purpose** · item handling: setting down on a depot or belt, patting, sliding, grabbing, fidgeting.
**Sound Layers** · material: cardboard on steel or rubber, soft thuds, a scrape, a claw clink and clicks.
**Timing** · 0.05–0.5 s (pat is three taps 0.2 s apart; scrape 0.5 s).
**Variations** · 3 variants each.
**Implementation Notes** · at the release and grab frames; the scrape plays for as long as the push or drag lasts.

### dust_off
**Purpose** · a satisfied "job done".
**Sound Layers** · material: two metal rubs and a small clink.
**Timing** · 0.7 s.
**Variations** · 2 variants.
**Implementation Notes** · after closing a chest.

### poof_in / poof_out
**Purpose** · the item appearing in his claw, or vanishing from it, at blind containers.
**Sound Layers** · a soft cartoon pop and a small sparkle; out adds a little rising swell before the pop.
**Timing** · in: pop at 0 s, sparkle at 0.03 s. Out: swell 0–0.12 s, pop at 0.12 s.
**Variations** · 2 variants each.
**Implementation Notes** · should feel slightly magical, but small.

### Trading

### haggle_chatter / plead_chirp
**Purpose** · the haggle: eager rapid-fire bargaining, then a pleading "pleeease".
**Sound Layers** · personality: five quick up-down chirps with a lens tick (nosy); the plead is a rising then held two-note chirp.
**Timing** · chatter 0.35 s; plead 0.28 s.
**Variations** · 4 chatter melodies, 2 pleads; no pitch randomisation on the plead.
**Implementation Notes** · while the villager is 'considering'; answered by the villager's no or yes.

### emerald_catch
**Purpose** · the payoff: payment lands in his till.
**Sound Layers** · material: two bright emerald clinks and a coin-rattle rustle · signature: a brass ping.
**Timing** · clink 0 s, ping 0.02 s, second clink 0.06 s.
**Variations** · 3 variants (clink pitch ±5%); the ping stays fixed.
**Implementation Notes** · on the emerald landing in the drawer. The most satisfying sound in the trade, so keep it crisp.

### spin_whir
**Purpose** · the trade-done celebration spin.
**Sound Layers** · mechanical: a rising gyro whir · movement: an air whoosh.
**Timing** · 0.45 s.
**Variations** · 2 variants.
**Implementation Notes** · paired with `merchant_trill`, and a `land_soft` for the hop.

### turn_scrub
**Purpose** · he's carving a bend, not sliding: the tyre grips and works through the turn.
**Sound Layers** · movement: the rolling rumble leaning harder on the tyre · material: a soft rubber grip-and-slip under 1.2 kHz.
**Timing** · 1.3 s: swells in as he banks (0.35 s), a light scrub through the middle of the arc, eases out as he straightens.
**Variations** · 3 variants, small pitch spread.
**Implementation Notes** · one-shot at the start of a turn, over the rolling loop; quiet (below `roll_start`). Used by `turn_left` and `turn_right`.

### wheel_freespin
**Purpose** · his wheel keeps spinning while he's in the air on the turn-around hop.
**Sound Layers** · mechanical: a hub whir that rises as the load comes off and falls as it spins down · air: a faint whoosh.
**Timing** · 0.45 s, rise 0.2 s then fall.
**Variations** · 2 variants.
**Implementation Notes** · one-shot with the take-off in `turn_around`; sits under `spin_whir`.

### wheee
**Purpose** · he enjoys the turn-around hop: a quick, happy "wheee".
**Sound Layers** · personality: a rising-then-falling glide on the soft voice.
**Timing** · 0.36 s.
**Variations** · 3 variants.
**Implementation Notes** · optional voice cue in `turn_around`; skip it if a voice cue played in the last few seconds (voice cooldown), so frequent turn-arounds don't nag.

### villager_no / villager_yes / villager_trade
**Purpose** · the villager's side of the haggle.
**Sound Layers** · placeholders only.
**Timing** · 0.2–0.32 s.
**Variations** · vanilla.
**Implementation Notes** · in game play vanilla `entity.villager.no`, `entity.villager.yes` and `entity.villager.trade` from the villager, together with the head shake and nod.

### Scared of hostile mobs

### scared_yelp
**Purpose** · the startle when he spots a hostile mob: instantly readable fear.
**Sound Layers** · personality: a fast two-note "eep!" (eager, so it rises) · mechanical: a spring twang as his neck shoots up · material: a steel click.
**Timing** · click and yelp at 0 s, twang from 0.02 s, about 0.35 s in all.
**Variations** · 3 variants, ±5% pitch.
**Implementation Notes** · on entering the scared state; 10 s cooldown, so a mob stepping in and out of view doesn't spam it.

### panic_chatter
**Purpose** · frantic babbling while he flees.
**Sound Layers** · personality: seven rapid high up-down chirps (nervous, eager).
**Timing** · 0.39 s, once per flee loop.
**Variations** · 4 melodies, so consecutive loops differ.
**Implementation Notes** · cued in the flee loop. In game play it every 1–2 loops at random, not every loop.

### roll_fast_loop
**Purpose** · fleeing at speed: his rubber roll, faster and higher.
**Sound Layers** · material: rubber rumble with tread thumps at 12 per second (double cruise), the same barely-audible crunch.
**Timing** · a seamless 1.5 s loop.
**Variations** · 2 loop variants; pitch follows speed.
**Implementation Notes** · replaces `roll_loop` while fleeing.

### hard_brake
**Purpose** · the panic stop in the corner.
**Sound Layers** · material: the fast rumble slowing sharply, a longer rubber scrub than `roll_stop`, and a settle bump.
**Timing** · rumble 0–0.5 s, scrub 0.02–0.37 s, bump at 0.3 s.
**Variations** · 3 variants.
**Implementation Notes** · at the start of the hide stage.

### hide_tuck
**Purpose** · making himself small.
**Sound Layers** · mechanical: a falling servo as the neck slides in · material: two small claw clinks as they cover his face, and a soft crouch thump.
**Timing** · servo 0–0.25 s, clinks at 0.2 and 0.24 s, thump at 0.3 s (cued at 0.38 s in `scared_hide`, so the clinks land as the claws reach his face at 0.6 s).
**Variations** · 2 variants.
**Implementation Notes** · in the hide stage.

### tremble_loop
**Purpose** · shaking with fear: rusty metal plates chattering together.
**Sound Layers** · material: about 11 short, damped knocks a second from plate, tin and metal recordings, with uneven timing and pitch, cut short so they knock rather than ring (rusty, not shiny), and a slightly gritty mid edge · mechanical: a faint corroded grind underneath.
**Timing** · a seamless 2 s loop, quiet.
**Variations** · 2 loop variants.
**Implementation Notes** · a looping sound instance while cowering; fade it out on recovering.

### whimper
**Purpose** · a tiny scared "eee…".
**Sound Layers** · personality: a high, slowly falling voice note.
**Timing** · 0.4 s.
**Variations** · 3 variants.
**Implementation Notes** · at most one every 3–6 s while hiding or cowering.

### peek_click
**Purpose** · a nervous peek out from behind his claws.
**Sound Layers** · mechanical: a quiet lens focus tick · personality: a tiny questioning "…?".
**Timing** · 0.2 s.
**Variations** · 2 variants.
**Implementation Notes** · during the peeks in the cower loop and at the start of recovering.

### relief_sigh
**Purpose** · "phew, it's gone".
**Sound Layers** · mechanical: a long winding-down servo · air: a soft exhale · personality: a falling relieved note.
**Timing** · about 1 s.
**Variations** · 2 variants.
**Implementation Notes** · in the recover stage.

### shake_off
**Purpose** · shaking the fear off.
**Sound Layers** · material: a quick rattle of loose parts and a clink · mechanical: an aerial boing.
**Timing** · 0.45 s.
**Variations** · 3 variants.
**Implementation Notes** · in the recover stage, before he's back to normal.

## Cue table
Seconds into each clip (the same data as `trade-bot.cues.json`):

| Clip | Cues (time event) |
|---|---|
| `idle` | 0.00 `clock_tick` · 0.50 `clock_tick` · 1.00 `clock_tick` · 1.50 `clock_tick` · 2.00 `clock_tick` · 2.20 `blink` · 2.50 `clock_tick` |
| `wall_bump` | 0.00 `roll_start` · 0.50 `bonk` · 0.80 `land_soft` · 1.00 `dizzy` · 2.05 `head_shake` · 2.40 `grumble` · 2.45 `roll_start` |
| `count_stock` | 0.15 `drawer_open` · 0.35 `servo_short` · 1.00 `pleased_boop` · 1.40 `focus_click` · 1.90 `blink` · 2.35 `drawer_close` · 2.40 `aerial_boing` |
| `sees_player` | 0.05 `servo_snap` · 0.10 `curious_chirp` · 0.50 `servo_snap` · 0.68 `double_take` · 1.00 `servo_short` · 1.10 `merchant_trill` · 1.58 `wave_clink` · 1.91 `wave_clink` · 2.24 `wave_clink` · 2.40 `roll_start` · 3.00 `drawer_open` · 3.00 `light_on` · 3.05 `brass_ping` |
| `dock_lights_out` | 0.00 `roll_start` · 0.05 `servo_short` · 1.40 `roll_stop` · 1.45 `servo_short` · 1.66 `hop_launch` · 2.20 `land_heavy` · 2.50 `servo_short` · 2.55 `light_off` · 2.75 `flicker` · 2.90 `glow_on` · 2.95 `flicker` · 3.05 `sleepy_mmm` · 3.60 `winder_engage` · 3.90 `key_rattle` · 4.70 `key_rattle` · 5.50 `key_rattle` |
| `action_comes_in` | 0.05 `flag_ding` · 0.40 `glow_off` · 0.42 `light_on` · 0.45 `startled_chirp` · 0.55 `light_on` · 0.60 `winder_release` · 1.03 `hop_launch` · 1.55 `land_soft` · 1.60 `roll_start` |
| `stairs_up` | 0.00 `roll_start` · 0.48 `hop_launch` · 0.94 `land_soft` · 1.14 `hop_launch` · 1.60 `land_soft` · 1.70 `aerial_boing` |
| `stairs_down` | 0.00 `roll_start` · 0.54 `drop_whoosh` · 0.74 `land_soft` · 1.15 `drop_whoosh` · 1.35 `land_soft` |
| `hop_on_slab` | 0.00 `roll_start` · 0.50 `hop_launch` · 0.98 `land_soft` · 1.45 `pleased_boop` |
| `hop_on_block` | 0.00 `roll_start` · 0.56 `hop_launch_big` · 1.28 `land_heavy` · 1.75 `merchant_trill` |
| `hop_off_slab` | 0.00 `roll_start` · 0.55 `drop_whoosh` · 0.93 `land_soft` · 1.43 `roll_start` |
| `hop_off_block` | 0.00 `roll_start` · 0.55 `drop_whoosh` · 1.05 `land_heavy` · 1.35 `blink` · 1.60 `blink` · 1.55 `roll_start` |
| `chest_take` | 0.35 `wave_clink` · 0.50 `chest_open` · 1.30 `curious_chirp` · 1.55 `rummage` · 2.40 `toss_whoosh` · 3.10 `cobble_clack` · 3.45 `found_it` · 4.25 `servo_short` · 4.45 `chest_close` · 4.70 `servo_short` |
| `chest_put` | 0.35 `wave_clink` · 0.50 `chest_open` · 1.30 `servo_short` · 2.00 `set_soft` · 2.20 `pat` · 3.15 `chest_close` · 4.00 `dust_off` · 4.50 `pleased_boop` |
| `barrel_take` | 0.50 `barrel_open` · 0.55 `curious_chirp` · 1.30 `poof_in` · 1.32 `startled_chirp` · 1.65 `servo_short` · 2.30 `puzzled_hmm` · 2.65 `huh` · 2.85 `shrug` · 3.70 `barrel_close` · 3.72 `startled_chirp` |
| `barrel_put` | 0.50 `barrel_open` · 0.70 `servo_short` · 1.18 `poof_out` · 1.50 `claw_fidget` · 2.30 `puzzled_hmm` · 2.65 `huh` · 2.85 `shrug` · 3.70 `barrel_close` · 3.72 `startled_chirp` |
| `chest_take_high` | 0.50 `chest_open` · 0.55 `curious_chirp` · 1.30 `poof_in` · 1.32 `startled_chirp` · 1.65 `servo_short` · 2.30 `puzzled_hmm` · 2.65 `huh` · 2.85 `shrug` · 3.70 `chest_close` · 3.72 `startled_chirp` |
| `chest_put_high` | 0.50 `chest_open` · 0.70 `servo_short` · 1.18 `poof_out` · 1.50 `claw_fidget` · 2.30 `puzzled_hmm` · 2.65 `huh` · 2.85 `shrug` · 3.70 `chest_close` · 3.72 `startled_chirp` |
| `depot_place` | 0.30 `servo_short` · 1.05 `set_on_metal` · 1.35 `puzzled_hmm` · 2.40 `push_scrape` · 3.50 `pleased_boop` |
| `depot_take` | 0.00 `roll_start` · 0.30 `servo_short` · 0.75 `wave_clink` · 0.95 `push_scrape` · 2.05 `grip` · 2.20 `servo_short` · 2.35 `pleased_boop` · 2.40 `roll_start` |
| `belt_place` | 0.45 `servo_short` · 1.00 `set_soft` · 1.20 `servo_short` · 3.25 `wave_clink` · 3.30 `bye_boop` · 3.65 `wave_clink` · 4.05 `wave_clink` |
| `expr_happy` | 0.20 `merchant_trill` · 0.40 `hop_small` · 0.68 `hop_small` · 1.00 `aerial_boing` |
| `expr_curious` | 0.25 `curious_chirp` · 0.90 `focus_click` · 1.50 `focus_click` · 2.10 `focus_click` |
| `expr_startled` | 0.15 `startled_chirp` · 0.20 `aerial_boing` |
| `expr_sad` | 0.30 `sad_aww` · 1.10 `sigh` |
| `expr_cross` | 0.20 `focus_click` · 1.00 `grumble` · 1.10 `head_shake` |
| `expr_wave` | 0.20 `servo_short` · 0.40 `light_on` · 0.50 `hi_boop` · 0.65 `wave_clink` · 1.04 `wave_clink` · 1.42 `wave_clink` |
| `expr_doze_off` | 0.40 `sleepy_mmm` · 1.20 `servo_short` · 2.00 `servo_short` · 2.20 `flicker` · 2.36 `light_off` · 2.60 `snore` · 3.60 `snore` · 4.50 `startled_chirp` · 4.50 `aerial_boing` · 4.52 `light_on` · 4.90 `servo_snap` · 5.60 `servo_snap` · 6.10 `huh` |
| `trade_villager` | 0.05 `focus_click` · 0.30 `light_on` · 0.40 `hi_boop` · 0.60 `wave_clink` · 0.95 `drawer_open` · 1.15 `servo_short` · 1.45 `grip` · 1.60 `servo_short` · 1.75 `brass_ping` · 1.95 `haggle_chatter` · 2.20 `villager_no` · 2.45 `plead_chirp` · 2.80 `villager_yes` · 2.90 `pleased_boop` · 3.00 `servo_short` · 3.38 `toss_whoosh` · 3.97 `set_soft` · 4.00 `villager_trade` · 4.45 `toss_whoosh` · 5.00 `emerald_catch` · 5.15 `drawer_close` · 5.30 `spin_whir` · 5.35 `merchant_trill` · 5.80 `land_soft` · 6.40 `light_off` · 6.45 `bye_boop` · 6.55 `wave_clink` · 6.90 `wave_clink` |
| `trade_villager_open` | 0.05 `focus_click` · 0.30 `light_on` · 0.40 `hi_boop` · 0.60 `wave_clink` · 0.95 `drawer_open` · 1.15 `servo_short` · 1.45 `grip` · 1.60 `servo_short` · 1.75 `brass_ping` · 1.95 `haggle_chatter` · 2.20 `villager_no` · 2.45 `plead_chirp` · 2.80 `villager_yes` · 2.90 `pleased_boop` · 3.00 `servo_short` · 3.38 `toss_whoosh` · 3.97 `set_soft` · 4.00 `villager_trade` · 4.45 `toss_whoosh` · 5.00 `emerald_catch` · 5.15 `drawer_close` · 5.30 `spin_whir` · 5.35 `merchant_trill` · 5.80 `land_soft` · 6.40 `light_off` · 6.45 `bye_boop` · 6.55 `wave_clink` · 6.90 `wave_clink` |
| `start_rolling` | 0.10 `roll_start` · 0.30 `aerial_boing` |
| `rolling` | 0.00 `roll_loop` |
| `stop_rolling` | 0.02 `roll_stop` · 0.15 `aerial_boing` |
| `scared_spot` | 0.05 `focus_click` · 0.22 `servo_snap` · 0.28 `scared_yelp` · 0.50 `land_soft` · 0.70 `servo_short` · 0.75 `roll_start` |
| `scared_flee` | 0.00 `roll_fast_loop` · 0.10 `panic_chatter` · 0.65 `servo_snap` |
| `scared_hide` | 0.00 `hard_brake` · 0.10 `aerial_boing` · 0.38 `hide_tuck` · 0.75 `whimper` |
| `scared_cower` | 0.00 `tremble_loop` · 0.85 `peek_click` · 1.50 `whimper` |
| `scared_recover` | 0.15 `peek_click` · 0.20 `servo_short` · 0.55 `servo_short` · 0.80 `curious_chirp` · 0.95 `servo_short` · 1.20 `relief_sigh` · 1.70 `shake_off` · 2.20 `pleased_boop` |
| `turn_left` | 0.00 `roll_loop` · 0.15 `servo_short` · 0.55 `turn_scrub` · 1.70 `blink` · 2.05 `aerial_boing` |
| `turn_right` | 0.00 `roll_loop` · 0.15 `servo_short` · 0.55 `turn_scrub` · 1.70 `blink` · 2.05 `aerial_boing` |
| `turn_around` | 0.05 `roll_stop` · 0.45 `servo_short` · 0.62 `hop_launch` · 0.80 `spin_whir` · 0.82 `wheel_freespin` · 0.88 `wheee` · 1.22 `land_soft` · 1.30 `aerial_boing` · 1.45 `roll_start` · 1.70 `blink` |
