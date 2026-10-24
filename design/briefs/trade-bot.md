# Design Brief: Trade Bot

## Hard Requirements
- "a create mod style robot called trade bot"
- "He will have one wheel to move around"
- "be very animated with lots of character"
- "made in Blockbench" / "designed in block bench"
- "he will react when he bumps into walls"
- "[he will react when he] sees players"
- "will have little mannerisms that give him life and charm"
- "he should follow the create mod colours"
- "He should be 1 and a half block tall"
- "have two arms"
- "a charming Wall-E style face"
- "his motions must be fluid dynamic with lots of tilting and inertia"
- "He should have an idle station where he sits waiting for actions to do"
- "all his lights turn off there and his station glows"

## Asset Type
Two assets that are designed together:
1. **Trade Bot**: character (wandering robot mob) · 24 px tall (1.5 blocks), with the wheel on y=0 and the top of the head at 24 · `modded_entity` format (box UV, one 64x64 texture). The mod has no GeckoLib, so animation will be done in code from the preview timings.
2. **Trade Bot Station**: block (1x1 footprint) with moving parts and a glow, drawn by a block-entity renderer · `java_block` format, with the moving parts in their own groups.

## Fantasy
A cheerful little clockwork shopkeeper who unicycles around your base and rolls up to anyone with a deal.

## Hero Element
**Big binocular eye pods** in the Wall-E style: two chunky dark-steel pods, each with a brass rim, a dark glass lens, a muted amber iris and a single bright catchlight pixel. Each pod tilts on its own, so the eyes alone can show happy, worried, curious or cross.

## Theme
A Create clockwork machine that happens to have a soul. It should sit naturally beside the Cuckoo Clock, Create's mechanical crafters and deployers.

| Material | Where | Colours |
|---|---|---|
| Andesite casing | body panels | `#6b6e6a` `#7c7f79` `#8a8c86` (mid grey at most) |
| Dark blue-grey steel | body frame edges, eye pods, neck, lids, arms | `#2f343b` `#3c424a` `#4a5059` |
| Muted brass | eye rims, waist band, shoulder cogs, wind-up key, drawer handle, claws | `#8f7638` `#b08f45` (`#c9a75a` highlights only) |
| Copper | wheel fork, aerial | `#7a4a2e` `#9c5f3a` |
| Rubber tyre | wheel | `#26272a` `#323337` |
| Glow (small areas only) | irises: muted amber `#d99a4a` with an `#e8b56a` centre; "open for trade" lamp: muted emerald `#4f9a68` | |

There are no stripes or saturated surfaces. Brass, copper and glow are trims only.

## Shape Language
**Circles** dominate: a single round wheel, round lenses, a barrel-ish body with stepped, chamfered corners, cog shoulder joints and a looped wind-up key. Squares appear only in the steel frame that holds it together.

## Silhouette Description
Top-heavy and friendly. A wide binocular head (12 px) sits on a thin neck above a narrower rounded body (10 px), with a single wheel underneath. From the front it reads like a wine glass with binoculars on top. From the side, the body balances over the wheel axle, the head pokes slightly forward (curious) and the key sticks out at the back. The long arms hang to about wheel-hub height.

## Primary Forms
1. **Eye pod pair** (the hero): two 5x5x5 px pods, 2 px apart, joined by a 2 px bridge. Spans y17–22. Outer top corners are slightly chamfered, giving a soft, droopy-cute look.
2. **Body**: 10 w x 9 h x 8 d px, y7–16. Andesite panels in a dark-steel frame, with stepped corners so it reads round.
3. **Wheel**: 8 px diameter x 3 px wide, axle at y4. Dark rubber tyre, brass hub.

## Secondary Forms
- **Telescoping neck**: 2x2 px, y16–18. It slides up 2 px for surprise and down into the body for sleep.
- **Arms**: shoulder (cog), upper arm 2x4, forearm 2x4 and a two-prong brass claw, hanging from the body sides at y14.
- **Wheel fork**: copper plates on either side of the wheel, from the axle up into the body base.
- **Trade drawer**: a 6 x 2 px brass-handled drawer across the lower front of the body. It slides out to show his "wares" and is the trade mechanic made visible.
- **Wind-up key**: a brass looped key on his back that slowly turns. He's clockwork powered.

## Interesting Features
- An asymmetric copper **aerial with a bead** on top of the left eye pod. It wobbles and lags behind every movement and shows how he feels (droops when sad).
- Brass **waist band** under the drawer, with one rivet line.
- A tiny muted-emerald **"open" lamp** beside the drawer. It glows only when he's ready to trade, a quiet nod to villager emeralds.
- **Shoulder cogs**: small Create-style cogwheels visible at each shoulder joint. They turn as the arm swings.
- A hand-placed **catchlight** pixel in each lens. It is what makes the eyes look alive.

## Idle Station
**Fantasy:** his little home dock, a Create winding cradle where he parks, powers down and gets wound up while he waits for something to do.

**Role in the pair:** the station supports the bot and never competes with him. It is low, compact and quiet until he docks, and then it becomes the thing that glows.

**Form** (1 block footprint, kept low so he stays the star):
- **Plinth**: 14x14 px andesite casing base in a dark-steel frame, 4 px tall, with stepped corners matching his body.
- **Wheel cradle**: a rounded slot in the top centre, lined with dark rubber. His wheel drops 2 px into it, so he visibly *sits* rather than balances.
- **Winding post**: a 3x3 px dark-steel post at the back, rising to his key height (about y 14 above the plinth top), with a small Create cog on its side.
- **Winder arm**: a short brass arm on the post that swings forward to meet his back key and turns it while he's docked.
- **Signal flag**: a tiny brass flag on top of the post. It springs up when there's an action for him; it's folded down while he waits.

**Glow** (the station's light, muted, small areas only):
- A **glow ring** lining the wheel cradle and a **lamp window** in the front of the plinth, in muted amber `#d99a4a`, the same colour as his irises. His light visibly *moves* into the station.
- While he's docked, the glow **breathes slowly** (fades between 60% and 100% over 4 s), in time with the winder arm turning his key.
- When the station is empty, the glow is off apart from a faint ember in the lamp window, so it reads as "home" from across the room.

## Personality
*Eager, nosy, a bit clumsy*: a born salesman who never stops trying, gets distracted by anything new, and is crushed for exactly one second when you walk away.

## Face and Expressions
Moving parts: the eye pods (each one rolls and pitches on its own), the top lids (one per pod), the irises (spin and scale), the head (pitch, yaw, roll), the neck (extend) and the aerial.

| Expression | How it reads |
|---|---|
| Neutral | Pods level, lids up 30%, irises normal |
| Happy | Pods tilt **outward** ~10° (outer edges down) and lift a little, irises slightly larger, aerial perks up |
| Curious | Head roll 15°, one pod raised more than the other, irises dilate (scale 1.2) |
| Startled | Neck extends +2 px, lids snap fully open, irises shrink (0.7), aerial springs back |
| Worried/sad | Pods tilt **inward** ~12° (inner edges down), lids half, head pitches down, aerial droops |
| Cross | Lids half closed and flat, pods pitch forward, head low |
| Dizzy | Irises spin (the catchlight orbits), pods wobble out of phase |
| Sleepy | Lids slowly close, head sinks as the neck retracts |

## Motion Set

**Motion feel: fluid, tilting, full of inertia.** This applies to every item below.
- **Nothing moves linearly and nothing stops dead.** Every movement eases in, overshoots and settles with one or two decaying wobbles.
- **He is a stack of springs.** The body follows the wheel, the head follows the body, and the eye pods, aerial, claws and drawer follow whatever carries them. Each link lags a little and settles slightly later than the one below it, so a single input (start, stop, turn, bump) rolls up the whole stack like a wave. In game this is done with spring-damper smoothing per bone (the target comes from velocity and acceleration). The Blockbench previews fake it with staggered, overshooting keyframes.
- **Tilt is his main way of moving.** He leans into acceleration, rocks back on braking, banks into turns (roll toward the inside of the turn, up to ~15° on sharp turns) and rides the swing back out. The head counter-tilts slightly to keep the eyes level, as a real balancing creature would. Expect gentle tilt in every frame, even idle.
- **Weight:** the head is heavy (big eyes) and lags most on pitch. The arms are light and swing freely. The aerial is the loosest part and settles last.
- **Speed drives wobble:** faster moves and harder stops mean bigger lean and longer settling. Any jolt (a hop, a bump, the double-take) also sets off the spring chain.

**Locomotion: unicycle balancing**
- The wheel spins to match ground speed.
- The body pivots on the axle: it leans forward ~10° into acceleration, then rocks back ~8° on braking with one overshoot wobble (0.4 s).
- Arms swing slightly out for balance when turning.
- Bump in the ground → the body bobs 0.5 px and the aerial lags 2 frames.
- Standing still → a constant tiny balance wobble (see idle).

**Idle loop** (3 s, seamless)
- Balance sway ±2° (roll) and ±1.5° (pitch), out of phase.
- Body bob of 0.25 px.
- The wind-up key turns slowly (one turn every 4 s).
- He blinks at random every 3–6 s (lids down/up in 0.15 s, with a double blink sometimes).
- The aerial sways a beat behind the body.

**Mannerisms** (one at random every 6–15 s while idle)
1. **Look around** (2 s): the eyes lead and the head follows, left, hold, right, hold, back.
2. **Polish the lens** (2.5 s): the right claw comes up and rubs the right lens in two small circles, then he blinks twice.
3. **Count the stock** (3 s): the drawer slides out, he peers down with the neck pitched forward, nods twice, and the drawer snaps shut.
4. **Hum a tune** (2 s): little rhythmic bounces on the wheel while his arms swing in time and the aerial boings.
5. **Doze off** (5 s): lids sink, his head droops and the key slows down. Then he jolts awake (a small hop, lids wide) and looks around to check nobody saw.
6. **Flick the aerial** (1.5 s): his left claw taps the aerial, it boings, and he watches it settle.

**Reactions**
- **Bumps into a wall** (~2.5 s): the impact tips the body back 20° and squashes the neck (head scale y 0.85), the wheel bounces 1 px and both arms fly up. He overshoots forward 6°, then settles. Next he goes **dizzy** for 1 s, then shakes his head (yaw ±15° three times, fast). He gives the wall a **cross** look for 0.4 s, reverses and turns away.
- **Sees a player** (first sighting within ~8 blocks, ~2.5 s): he stops, and his eyes snap toward the player. He holds for 0.2 s, then does a **double-take**: looks away, snaps back, neck extends +2 px, lids wide, irises shrink then dilate. He turns **happy**, waves one arm three times, and rolls closer; the drawer slides out and the "open" lamp lights. While the player stays near, his head tracks them.
- **Player walks away** (1.5 s): he watches them go, turns **sad**, gives a small slow wave with the aerial drooping, then perks back up.
- **Trade done** (1.5 s): a happy spin on the wheel (360°), both arms up and a little hop. The drawer closes.
- **Gets hit** (1.5 s): he flinches back with his eyes squeezed shut (lids closed, pods pitch in), then glares **cross** and rolls back 1 block.

**Station: docking, waiting and leaving** (his rest state; he goes home when there's nothing to do, and at night)
- **Docking** (~3 s): he rolls up, does a three-point turn, glances back over his shoulder (the head leads) and reverses in. The wheel drops into the cradle with a heavy settle: the body dips, bounces once and the aerial wobbles last. He stops balancing; his body relaxes back 3° against the post and his arms fold in front.
- **Lights out → station on** (1.2 s, the signature moment): his lamp clicks off first. His irises dim one after the other with a little flicker, while the lids sink to half closed. As his lights fade, the station's cradle ring and lamp window **fade up** in the same amber, so the light passes from him into the dock. The winder arm swings in and engages his key.
- **Waiting** (docked, lights off): the winder arm slowly turns his key, the station glow breathes in time with it, and he settles 0.25 px each "breath". He stays still apart from rare small mannerisms (one every 10–25 s):
  1. **Peek**: one iris flickers on dimly, the eye pod tilts to look around, then it fades off again.
  2. **Dream twitch**: a claw twitches, and the aerial gives a tiny boing.
  3. **Settle**: a sleepy shuffle, with the body rocking side to side once in the cradle.
- **An action comes in** (~2 s): the signal flag springs up with a brass *ding* and wobbles. The station glow drains while his irises **snap on** (startled: neck up, lids wide) and his lamp lights. The winder arm swings away, he hops out of the cradle (anticipation dip, a pop up, landing with a forward lean) and rolls off with the inertia chain in full swing.
- **At night** he stays docked, lids fully closed, with no peeks.

## Rig Requirements
Pivots are given in model pixels (x right, y up, z forward; origin at the ground under the axle).

| Bone | Parent | Pivot | Moves |
|---|---|---|---|
| `root` | — | 0, 0, 0 | whole-model translate (hops) |
| `wheel` | root | 0, 4, 0 (axle) | rotate X (spin) |
| `body` | root | 0, 4, 0 (axle) | lean, sway, bob (rotates around the axle like a unicycle) |
| `fork` | body | 0, 4, 0 | (static, follows body) |
| `drawer` | body | 0, 9, 4 | translate Z (out 2 px) |
| `lamp` | body | — | texture/visibility state only |
| `key` | body | 0, 12, -4 (back face) | rotate Z (around the back normal) |
| `arm_left` / `arm_right` | body | ±5.5, 14, 0 (shoulder cog centre) | rotate X/Z |
| `forearm_left` / `forearm_right` | arm | ±5.5, 10, 0 (elbow) | rotate X |
| `claw_left` / `claw_right` | forearm | ±5.5, 6, 0 (wrist) | rotate (grip/wave) |
| `neck` | body | 0, 16, 0 | translate Y (−2 to +2) |
| `head` | neck | 0, 18, 0 | pitch, yaw, roll |
| `eye_left` / `eye_right` | head | ±1, 19.5, 0 (inner edge of each pod, at the bridge) | roll (tilt), pitch |
| `lid_left` / `lid_right` | eye | top-back edge of the pod (y 22, z −2.5) | rotate X (open/close) |
| `iris_left` / `iris_right` | eye | centre of the lens, front face | rotate Z (spin), scale (dilate) |
| `aerial` | eye_left | base on top of the left pod | rotate X/Z (lag, droop) |

The bot's lights (`iris_left`, `iris_right`, `lamp`) need on, dim and off states. Keep them as separate cubes with their own texture areas, so the renderer can draw them full-bright when on and darken them when off.

**Station rig** (block-entity renderer):

| Bone | Pivot | Moves |
|---|---|---|
| `station_base` | — | static: plinth, cradle, post |
| `winder_arm` | hinge on the post, at bot-key height | rotate Y (swing in/out) |
| `winder_head` | end of the arm, on the key axis | rotate Z (turns the key) |
| `post_cog` | cog centre on the side of the post | rotate (turns with the winder) |
| `signal_flag` | hinge on top of the post | rotate X (fold down / spring up) |
| `station_glow` | — | the cradle ring and lamp window as separate cubes for glow brightness |

## Fun Score (1-10)
**9**: the eyes and the unicycle wobble make him look alive even when he isn't doing anything. The drawer, the wind-up key and the dizzy wall bump give players moments they'll stop to watch. The light passing between him and his station turns "going home" into a small ritual.

## Builder Notes
- **The eye pods are the hero.** Never shrink them below 5x5 px each, and keep the head wider than the body. Hand-place the lens pixels: dark glass, amber iris ring, one catchlight. Don't use a computed circle.
- Total height of 24 px includes the aerial bead. The top of the pods is at 22, with the aerial to 24.
- He must look balanced over the axle in side view. The head may sit 1 px forward of the axle for a "leaning in" look, no more.
- Body roundness comes from stepped corners (an inset 1 px corner column), not from rotated cubes. Keep it a clean barrel.
- Arms must clear the body when they swing and when raised overhead (the wave and wall-bump poses).
- The palette stays muted. The lamp and irises are the only glow, and they stay small.
- Screenshots for review: front, three-quarter, side, back (key), a face close-up, and the poses **neutral, happy, startled (neck up), sad, dizzy, wave, wall-bump recoil and dozing**.
- Preview animations: the idle loop, "count the stock", the wall-bump reaction and a **roll-start-turn-stop** clip that shows the inertia chain (lean in, bank into the turn, brake, wobble settle with the head and aerial lagging). Use overshoot and offset timing on every bone; no linear interpolation between holds.
- **Station:** it must stay lower and quieter than the bot. No part of it should be taller than the bot's key height when he's docked, and its only glow is the amber ring and lamp window. Build the bot and station so his wheel axle lines up with the cradle when docked, and his key lines up with the winder head. Screenshot them together: docked and lit (station glowing, bot dark), and empty.
- Extra preview animation: **docking → lights out → station glow up**, and the reverse **action comes in** clip.
- Rig consequence of the inertia brief: pivots must sit at the real joints (the neck base for the head, the bridge for the eye pods, the aerial base), because lag and overshoot rotate around them. A misplaced pivot will make the wobble look like sliding.
