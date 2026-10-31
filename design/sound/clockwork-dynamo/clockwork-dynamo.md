# Sound Design: Clockwork Dynamo

Brief: [design/briefs/clockwork-dynamo.md](../../briefs/clockwork-dynamo.md) · Spec: [design/specs/dynamo.md](../../specs/dynamo.md) (D29) · Model clips: `start`, `working`, `boing`, `stop`, `idle` (`design/models/clockwork-dynamo.gen.js`) · Levels: [staging.md](../staging.md)

## Identity

A dynamo the bots built from their own parts. Eyes closed, a player should hear **a small, contented electrical machine**: a warm hum with the soft brush whisper of copper turning under carbon, and now and then the Trade Bot's own little aerial *boing*. Mechanical and handmade, never sci-fi; the only whimsy is the aerial, as in the look ("some whimsy, but not much").

- **Mechanical identity**: carbon brushes riding a copper commutator; the shaft's turn becomes a hum.
- **Emotional identity**: quietly busy, content. It doesn't chirp: it's a machine, not a bot.
- **Movement identity**: the aerial's spring, borrowed straight from the Trade Bot.

## Running all day

Dynamos run 24/7 beside water wheels and windmills, often in rows. The hum sits **10 dB under the staging bed** (about -46 LUFS, against B = -36; user, 2026-10-31: "much quieter so it doesn't drive the player mad"), 8 blocks, smooth and tonal, with no broadband hiss that would grate on repeat. It's noticed only up close. Start and stop can be heard across a room (B + 9 to 11, 16 blocks). The aerial sounds sit between.

## Signature sounds

1. **The output hum**: a warm electrical hum, one note per setting. **Its pitch follows output**: 0.8 at 256 SU rising on a log scale to 1.25 at 294,912 SU (`pitch = 0.8 + 0.45 × ln(setting / 256) / ln(1152)`), so a big boiler dynamo sings higher than a water-wheel one.
2. **Brush whisper**: carbon on copper, a soft, fine, *low-passed* whir under the hum, fluttering gently with the passing bars.
3. **The aerial boing**: the Trade Bot's `aerial_boing` (boing … boink … tink), the bot family's sound.
4. **Brush seat / lift**: a small dry contact click as the brushes take or drop the load: the dynamo's "on" and "off".

## Personality

| Trait | Sound |
|---|---|
| Content, steady | a smooth hum that never wavers, no clatter |
| Bot-built | the Trade Bot's aerial spring, brass-and-copper clicks |
| A little whimsical | the aerial gets the last word on start, stop and a new setting |

## Events

### hum
**Purpose** · it's making power, and how much (pitch).
**Sound Layers** · mechanical: a warm electrical hum (fundamental and a soft octave) · material: copper brush whisper, low-passed, fluttering at the bar rate.
**Timing** · a seamless 2 s loop.
**Variations** · one take; pitch set by the setting in code (formula above), no random spread (a row of identical dynamos should sound like one machine, not a chorus).
**Implementation Notes** · tickable `SoundInstance` while `working`, pitch from the setting, updated when it changes; `BLOCKS`; background (about 0.1 volume), attenuation distance 8.
**Source** · rung 2: the real transformer hum (BigSoundBank #2118, low-passed warm) and the antenna motor's run (#1263, pitched to 0.6, low-passed) as the brush whir; rung 4: a soft 100 Hz synth hum under them for warmth. The user picked it over the neon take (2026-10-31).

### start
**Purpose** · power is flowing.
**Sound Layers** · mechanical: brushes seat (a relay click) · motion: a motor's run-up into the hum · material: a faint brass tick from the band.
**Timing** · relay 0.05 s, tick 0.06 s, run-up from 0.25 s, hum swelling in from 0.25 s; 1 s.
**Variations** · 2 takes, ±3% pitch.
**Implementation Notes** · when `working` turns on; `BLOCKS`; staging B + 9 to 11, 16 blocks.
**Source** · rung 3: BigSoundBank #1498's relay click (the brushes seating) and its motor's run-up, pitched to 0.8; rung 2: `tick_004`, the transformer hum swelling in. The user picked this over the antenna-motor take (2026-10-31).

### stop
**Purpose** · the power's gone (shaft stopped or overstressed).
**Sound Layers** · motion: a spin-down whir falling away · mechanical: brushes lift (a softer contact click at the end).
**Timing** · whir 0–0.5 s falling, click 0.42 s.
**Variations** · 2 takes, ±3% pitch.
**Implementation Notes** · when `working` turns off; `BLOCKS`; staging B + 9 to 11, 16 blocks.
**Source** · rung 2: the transformer hum cutting out, the antenna motor's stop (#1263, pitched to 0.7) as the spin-down, Kenney `metalLatch` / `metalClick` lifting.

### aerial_up
**Purpose** · the aerial springs upright: the whimsy that says "on".
**Sound Layers** · mechanical: a spring boing on release, rising slightly, then a smaller boink on the overshoot and a tiny tink as it settles.
**Timing** · boing at release, boink at the overshoot (+0.2 s), tink +0.45 s.
**Variations** · 2 takes, ±5% pitch.
**Implementation Notes** · with `start` (0.05 s after); `BLOCKS`; staging B + 5 to 8, 16 blocks.
**Source** · rung 2: the Trade Bot's `aerial_boing` recipe, pitched up a touch.

### aerial_boing
**Purpose** · the output changed: the aerial wobbles.
**Sound Layers** · the Trade Bot's `aerial_boing`: boing, boink, tink, each quieter and with more silence.
**Timing** · 0.55 s.
**Variations** · 3 takes (the Trade Bot's).
**Implementation Notes** · when new settings are applied while working (with `adjust`, 0.02 s later); `BLOCKS`; staging B + 2 to 5, 8 blocks (the player is right there with the wrench).
**Source** · rung 2: the Trade Bot's approved `aerial_boing`, reused as is.

### aerial_fold
**Purpose** · the aerial sags flat: it's stopped.
**Sound Layers** · mechanical: a slow, falling, low boing as it sags · material: a tiny copper-on-andesite tap where it lands, and a softer bounce tap after.
**Timing** · sag 0–0.4 s, tap 0.42 s, bounce tap 0.6 s.
**Variations** · 2 takes, ±5% pitch.
**Implementation Notes** · with `stop` (0.1 s after); `BLOCKS`; staging B + 5 to 8, 16 blocks.
**Source** · rung 2: a slowed, lower Trade Bot boing; taps from the Kenney impact packs.

### adjust (proposal: the Electric Motor's settings click)
**Purpose** · the wrench screen's settings took.
**Sound Layers** · the Electric Motor's `adjust`: a short ratchet and a switch click.
**Timing** · 0.25 s.
**Variations** · the motor's 2 takes.
**Implementation Notes** · when `DynamoSettingsPayload` is applied, played at the dynamo for everyone nearby; `BLOCKS`. Not in spec D29: proposed so the two siblings click alike when wrenched.
**Source** · rung 2: the motor's approved `adjust`, reused as is.

## Clips

| Clip | Cues (s) | Why there |
|---|---|---|
| `start` | 0.50 `start` · 0.55 `aerial_up` · 1.00 `hum` | shaft and lamp on at 0.5; aerial released at 0.55 |
| `working` | 0.00 `hum` | the 2 s loop |
| `boing` | 0.00 `hum` · 0.38 `adjust` · 0.40 `aerial_boing` | the setting changes at 0.4 |
| `stop` | 0.50 `stop` · 0.60 `aerial_fold` | shaft stops dead at 0.5; aerial released at 0.6, lands about 1.0 |
| `idle` | — | silent |
