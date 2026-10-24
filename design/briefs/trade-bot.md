# Design Brief: Trade Bot

Character brief for Trade Bot and his station: design, motion, rig, preview clips and the sound cue sheet.
Model: [design/models/trade-bot.bbmodel](../models/trade-bot.bbmodel) (rebuilt by [trade-bot.gen.js](../models/trade-bot.gen.js)). Every timing below is taken from that generator.

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
- "he also needs a bounce animation for going up and down stairs"
- "he should also hop in and out of his station" / "reverse hopping into it"
- "He also needs an opening, searching and taking things out of chests/barrels animations and animations for putting things on depots, belts or into chests/barrels"
- "He also needs a similar hop animation for slabs and any block he wants to climb on that is more than a quarter block tall, getting on and off"
- Depot: "He should put it in the same spot but then push it into the middle after a beat"
- Barrels "don't actually open or close" (no moving lid)
- Barrels: "the items should just appear in his hand and he should look confused", and he stands "back half a block"
- "this can be used for chests he can't actually look into (ones not on the floor)": "no knocking, some of these will be high or out of reach, keep it simple"
- "write all this into his character brief and add some times and animation descriptions for the sound designer"
- "he also needs to be able to take things from depots"
- "we also need start rolling, rolling, stop rolling animations and sounds"
- "the turn animation needs splitting to turn left and right and then we can transition from and to move with the turn in the middle"
- "we should also have a turn 180, where he does a little jump to spin around"
- "an afraid animation for when he sees a hostile mob, in game he will find a corner, and then try and make himself look small and shake until it's safe again. So animate each stage of that separately"
- Trading with villagers: "the villager must be sat on a seat for him to do it (that's going to be a game rule)"; "the villager maybe path blocked by their workstation and it needs to look like he's interacting even one full block away with an obstacle in between or not"

## Asset Type
Two assets, designed together:
1. **Trade Bot**: character (wandering robot mob) · 24 px tall (1.5 blocks), with the wheel on y=0 and the aerial bead at 24 · `modded_entity` format (box UV). The mod has no GeckoLib, so animation is done in code from the preview timings. The preview file uses one 256 px texture because it also holds the props. His in-game texture is generated from the same script without them.
2. **Trade Bot Station**: block (1x1 footprint) with moving parts and a glow, drawn by a block-entity renderer. For now it lives in the same preview file; it gets its own `java_block` model when it moves into the mod.

## Fantasy
A cheerful little clockwork shopkeeper who unicycles around your base and rolls up to anyone with a deal.

## Hero Element
**Big binocular eye pods** in the Wall-E style: two chunky dark-steel pods, each with a brass rim, a dark glass lens, a muted amber iris and a single bright catchlight pixel. Big steel **lid plates** sit across the top of each lens; they slide and tilt like eyebrows. Each pod tilts on its own, so the eyes alone can show happy, sad, curious or cross.

## Theme
A Create clockwork machine that happens to have a soul. It should sit naturally beside the Cuckoo Clock, Create's mechanical crafters and deployers.

| Material | Where | Colours |
|---|---|---|
| Andesite casing | body panels, forearms, station, props | `#6b6e6a` `#7c7f79` `#8a8c86` (mid grey at most) |
| Dark blue-grey steel | frame edges, eye pods, lids, neck, upper arms | `#2f343b` `#3c424a` `#4a5059` |
| Muted brass | eye rims, waist band, shoulder cogs, wind-up key, drawer handle, claws | `#8f7638` `#b08f45` (`#c9a75a` highlights only) |
| Copper | wheel fork, aerial | `#7a4a2e` `#9c5f3a` |
| Rubber tyre | wheel, cradle lining | `#26272a` `#323337` |
| Glow (small areas only) | irises: muted amber `#d99a4a` with an `#e8b56a` centre; "open for trade" lamp: muted emerald `#4f9a68` | |

There are no stripes or saturated surfaces. Brass, copper and glow are trims only.

## Shape Language
**Circles** dominate: a single round wheel, round lenses, a barrel-ish body with stepped corners, cog shoulder joints and a looped wind-up key. Squares appear only in the steel frame that holds him together.

## Silhouette Description
Top-heavy and friendly. A wide binocular head (14 px) sits on a thin telescoping neck above a narrower rounded body (10 px), with a single wheel underneath. From the front it reads like a wine glass with binoculars on top. From the side he balances over the wheel axle, with the head a touch forward and the key sticking out at the back. His long arms hang to about wheel-hub height.

## Primary Forms
1. **Eye pod pair** (the hero): two 6x5x5 px pods, 2 px apart, joined by a 2 px bridge, spanning y17–22. Each has a 4x3 lens inside a 1 px brass rim, and a 6x3 steel lid plate in front.
2. **Body**: 8x9x8 px chassis plus 1 px side panels (10 px wide), y7–16. Andesite panels in a dark-steel frame, with stepped corners so it reads round.
3. **Wheel**: 8 px diameter x 3 px wide, axle at y4. Dark rubber tyre with tread, brass hub caps with a cross.

## Secondary Forms
- **Telescoping neck**: 2x2 px with a brass collar. It slides up to 2 px for surprise and down into the body for sleep (the hidden part of the tube runs down to y12.5).
- **Arms**: a shoulder cog, a 2x5 steel upper arm, a 2x4 andesite forearm and a two-prong brass claw, hanging from the body sides at y14.
- **Wheel fork**: copper plates either side of the wheel, with brass axle nuts.
- **Trade drawer**: a 6x3 px drawer across the lower front with a brass handle. It slides out 3 px to show his "wares" (a little emerald, gold, copper and a paper slip painted inside).
- **Wind-up key**: a brass two-loop key on his back that always turns slowly. He's clockwork powered.

## Interesting Features
- An asymmetric copper **aerial with a brass bead** on top of the left eye pod. It's the loosest part of him: it lags every movement, droops when he's sad and bends into a question mark when he's confused.
- A brass **waist band** with a rivet line.
- A tiny emerald **"open for trade" lamp** on his chest. It's off by default and lights only when he's offering a trade.
- **Shoulder cogs**: small Create-style cogwheels at each shoulder that turn as the arm swings.
- A hand-placed **catchlight** pixel in each iris. It's what makes the eyes look alive.
- He carries **one item at a time** in his right claw, shown as a small Create-style cardboard package with a tape band.

## Idle Station
**Fantasy:** his little home dock, a Create winding cradle where he parks, powers down and gets wound up while he waits for something to do.

**Role in the pair:** the station supports the bot and never competes with him. It's low, compact and quiet until he docks, and then it becomes the thing that glows.

**Form** (1 block footprint, kept low so he stays the star):
- **Plinth**: 14x14 px andesite casing base in a dark-steel frame, 4 px tall, with stepped corners matching his body.
- **Wheel cradle**: a slot in the top centre, lined with dark rubber. His wheel drops 2 px into it, so he visibly *sits* rather than balances.
- **Winding post**: a 3x2 px dark-steel post at the back, rising just to his key height, with a small Create cog on its side.
- **Winder arm**: a short brass arm on the post that swings in to meet his back key and turns it while he's docked.
- **Signal flag**: a tiny brass flag on top of the post. It lies folded while he waits and springs up when there's something for him to do.

**Glow** (muted, small areas only):
- A **glow ring** around the cradle and a **lamp window** in the front of the plinth, in the same amber as his irises, so his light visibly *moves* into the station.
- While he's docked the glow **breathes slowly** (60%–100% over 4 s) in time with the winder. (In-game renderer effect; the Blockbench preview only shows on and off.)
- An empty station shows only a faint ember in the lamp window.

## Personality
*Eager, nosy, a bit clumsy*: a born salesman who never stops trying, gets distracted by anything new, and is crushed for exactly one second when you walk away. He doesn't quite understand how the world works (see the barrels), but he tries his best.

## Face and Expressions
Moving parts: the eye pods (each rolls and pitches on its own), the lid plates (slide up and down, tilt like brows), the irises (spin, scale, switch off), the head (pitch, yaw, roll), the neck (telescopes) and the aerial.

| Expression | How it reads |
|---|---|
| Neutral | Pods level, lids covering the top third of the lens, irises normal |
| Happy | Pods tilt outer-edges-down ~10° and lift, lids up, irises 1.2x, aerial perks up |
| Curious | Head roll 15°, one pod higher than the other, irises dilate (1.25x) |
| Startled | Neck up 2 px, lids fully open, irises shrink (0.7x), aerial springs back |
| Sad | Pods tilt ~12° outer-edges-down (a droopy Λ; a V read as cross, so the user flipped it), lids half, head down, aerial droops |
| Cross | Lids lowered and tilted into a frown, pods pitch forward, head low |
| Confused | Pods tilted to one side, one lid down and one up, irises shrink then widen, aerial bent into a "?", head tilted at whatever he's puzzling over |
| Dizzy | Irises spin (the catchlight orbits), pods wobble out of phase |
| Sleepy | Lids sink shut, head nods, neck sinks, irises switch off |

## Motion Set

**Motion feel: fluid, tilting, full of inertia.** This applies to everything below.
- **Nothing moves linearly and nothing stops dead.** Every movement eases in, overshoots and settles with one or two decaying wobbles.
- **He is a stack of springs.** The body follows the wheel, the head follows the body, and the eye pods, aerial, claws and drawer follow whatever carries them. Each link lags a beat (head +0.05 s, eyes +0.09 s, aerial +0.14 s) and settles later than the one below, so one input (start, stop, turn, bump) rolls up through him like a wave. In game: spring-damper smoothing per bone, with targets from velocity and acceleration.
- **Tilt is his main way of moving.** He leans into acceleration (about 10°), rocks back on braking (about 12°), banks into turns (up to ~17°) and rides the swing back out. The head counter-tilts slightly to keep the eyes level.
- **Weight:** the head is heavy and lags most on pitch, the arms are light and swing freely, and the aerial is loosest and settles last.

**Locomotion clips for the game** (`start_rolling` → `rolling` → `stop_rolling`, with `turn_left`, `turn_right` and `turn_around` dropped in between rolling loops; the game moves the entity)
- **Start rolling** (1.3 s): a small lean-back wind-up and crouch, then he lunges ~12° into the acceleration as the wheel spins up. His head and aerial lag back, his arms swing back and his lids widen eagerly, then everything settles into the cruise pose.
- **Rolling** (3 s seamless loop): the cruise pose (6° forward lean, head counter-tilted 2°), a 0.3 px bob twice a second, a slow ±1.5° sway, arms swinging gently, the aerial bobbing a beat behind, a blink, and the wheel turning exactly once per loop (120°/s).
- **Stop rolling** (1.6 s): the wheel brakes to a stop in 0.5 s. He rocks back ~12°, his head and aerial whip forward, his arms swing forward, and it all wobbles to rest.
- **Turn left / turn right** (3 s each, from rolling back into rolling): a 90° arc while he keeps rolling. His neck and head look into the bend first (~0.35 s ahead), then he eases off a little and banks ~15° into the turn. His aerial and outer arm swing wide, and the inner arm tucks slightly away from his chassis. Coming out, he powers back up, swings past upright and wobbles back into the cruise pose.
- **Turn around** (3 s, from rolling back into rolling): he brakes and glances over his shoulder the way he'll spin, then crouches, hops 5 px and spins 180° in the air with his arms flung out and his wheel whirring freely. He lands with a squash, his head and aerial whip down, then he powers off the other way.
- The last frame of each clip equals the first frame of the next (checked: 0.00 difference), so they chain cleanly, and the rolling loop wraps onto itself the same way. The turns start and end on the rolling loop's first frame. In game the entity's own movement does the travelling; the arcs in the preview clips are only there to show the motion.

**Locomotion: unicycle balancing**
- The wheel spins to match ground speed.
- At a standstill, a constant tiny balance wobble (see idle).
- Small bumps (4 px or less): he just rolls over with a 0.5 px body bob and an aerial lag.

**Idle loop** (3 s, seamless)
- Balance sway ±2° roll and ±1.5° pitch, out of phase, with a 0.25 px bob.
- The wind-up key turns at a steady 60°/s.
- He blinks every 3–6 s in game (2.2 s in the preview loop); the lids drop and lift in 0.2 s.
- The aerial and arms sway a beat behind the body.

**Mannerisms** (one at random every 6–15 s while idle)
1. **Count the stock** (3 s, preview `count_stock`): the drawer slides out, he peers down and nods twice, and the drawer snaps shut.
2. **Doze off** (6.8 s, preview `expr_doze_off`): his lids sink, his head nods off twice, his eyes flicker dark, then a jolt awake and a look round to check nobody saw.
3. **Look around** (2 s): the eyes lead and the head follows: left, hold, right, hold, back.
4. **Polish the lens** (2.5 s): the right claw rubs the right lens in two circles, then a double blink.
5. **Hum a tune** (2 s): little rhythmic bounces with the arms swinging in time and the aerial boinging.
6. **Flick the aerial** (1.5 s): his left claw flicks the aerial, it boings, and he watches it settle.

Mannerisms 3–6 have no preview clip yet; the expression clips cover their faces.

**Afraid of hostile mobs** (five separate stages that chain seamlessly; the game picks the corner and moves him)
1. **Spot** (`scared_spot`, 1.1 s): a double-take toward the mob, then a startle: neck up, lids wide, irises shrink to pinpricks, arms flinch up and a little hop back. He crouches and spins his wheel up to flee.
2. **Flee** (`scared_flee`, 1.5 s loop): panicked rolling toward a corner. A deep 14° forward lean, the wheel at double cruise speed, arms flung up and out in a panicked V beside his head (flailing outward, never into it), the aerial streaming back, eyes wide, and a quick glance back over his shoulder each loop.
3. **Hide** (`scared_hide`, 1.3 s): he brakes hard in the corner and makes himself small. His neck retracts fully, his body crouches, his forearms rise in front of his chest and his claws cover his eyes from below, his lids squeeze down, his irises shrink and his aerial lies flat.
4. **Cower** (`scared_cower`, 2 s loop): a fast tremble (9–13 Hz) through the body, head, claws and aerial. Halfway through, one eye peeks out between his claws, looks around nervously and hides again.
5. **Recover** (`scared_recover`, 2.6 s): when it's safe he peeks cautiously, raises his head slowly and looks left and right. A big relieved sigh, a whole-body shake-it-off, and back to normal.
- The arm poses are solved clear of his own head and body, with a frame-by-frame self-collision check (arms against the eye pods, rims, eyelid plates, bridge, chassis, side panels and drawer).
- The game's state machine runs spot → flee (while moving) → hide (on reaching the corner) → cower (while a hostile is near) → recover (once it's safe). Each clip ends exactly on the next one's first frame.

**Reactions**
- **Bumps into a wall** (preview `wall_bump`): thrown back 20°, neck squashed, arms flung up, dizzy, a head shake, a cross glare at the wall, then he reverses.
- **Sees a player** (preview `sees_player`): notice, look away, double-take with the neck shooting up, a happy wave, he turns to face them and rolls closer, the drawer opens and the trade lamp lights.
- **Player walks away** (game-side): watches them go, plays `expr_sad` with a small slow wave, then perks back up.
- **Trade done** (game-side): `expr_happy` bounces plus a 360° spin on the wheel; the drawer closes.
- **Gets hit** (game-side): flinch with the lids shut, then `expr_cross` and he rolls back a block.
- **A trade falls through** (`shrug`, 2.2 s): a puzzled head tilt with the eyes tipped in and lids half down; then a shrug, elbows out and claws turned up, with a little lift of the body; then he settles.

**Winding down and being wound up** (D24, D29: at 0 HP or soaked he never dies, he runs down)
- **Winds down** (`wind_down`, 3.4 s, holds its last frame): the key slows to a stop. Four escapement ticks come further and further apart (0.3, 0.9, 1.7, 2.6 s), and each drops him a bit further into a slump: body forward and to one side, head drooping and tilted, aerial flopped, lids half shut, eyes tipped sad, arms limp. The irises shrink, flicker and go dark. The last tick lands with a small, tired bounce.
- **Re-wound** (`rewind`, 3.0 s, starts exactly on `wind_down`'s last frame): two cranks of the key, each with a shudder; the eyes flicker back on; he jolts upright and overshoots (lids wide, irises big, neck up, aerial whipping back, a tiny hop); then he settles into his idle and blinks.

**Stairs** (one bounce per 8 px step)
- **Up** (`stairs_up`): he rolls in and stops short, crouches (body squashes 1 px, arms swing back, head dips), then pops *straight up first* and only moves forward once the wheel clears the step. The wheel spins fast in the air, the arms swing forward and the lids open wide ("hup!"). He lands with a squash, a head nod and an aerial whip, and springs back with one overshoot.
- **Down** (`stairs_down`): he rolls off each edge without stopping, tips forward with his arms flicked out and his eyes wide ("whee"). The landing is a double bounce (1.5 px, then 0.5 px).

**Climbing on and off blocks** (anything over a quarter block)
- 4 px or less: roll over. Up to 8 px: the slab hop. Taller, up to a full block: the block hop.
- **Hop on** (`hop_on_slab`, `hop_on_block`): the stair crouch-pop-land scaled by height. Up first, forward once above the top. A full block gets a deeper crouch, a bigger arm swing, a higher arc and a heavier landing, then a tiny proud wiggle on top.
- **Hop off** (`hop_off_slab`, `hop_off_block`): roll to the edge, a little hop out *past the edge first*, then the drop. A slab gives the stair double bounce. A full block gives an "oof": a deep squash, a 2 px rebound and two blinks to shake it off.
- None of these clip the block (checked frame by frame in the generator).

**Handling items** (one package at a time, in the right claw; the left claw helps)
- **Take from a chest** (`chest_take`): hooks the lid with his left claw and heaves it open, then peeks in and rummages with both claws. He flings a cobblestone over his shoulder without looking. **Found it!** He pops up with wide, then happy eyes, holds the package up high, shuts the lid and tucks it into a carry pose.
- **Put into a chest** (`chest_put`): opens the lid, lowers the package in, pats it, shuts the lid and dusts off his claws.
- **Take from a depot** (`depot_take`): the package sits in the middle of the depot, out of reach. He rolls in 2 px, leans right over, puts his claw on top of it and drags it toward himself to the near edge. Then he lets go, grabs it properly, lifts it into his carry pose with a happy little "got it" bob, and rolls back.
- **Put onto a depot** (`depot_place`): sets it down near the front edge and lets go. A beat ("hmm, not quite"), then he pushes it into the middle of the depot and leans back to admire it.
- **Put onto a belt** (`belt_place`): lays it on the belt, watches it ride away and waves it goodbye.
- **"Blind" containers** (`barrel_take`, `barrel_put`, `chest_take_high`, `chest_put_high`): barrels just sit there (no lid; the top only switches between closed and open, like the vanilla textures), and chests up high or out of reach can't be seen into. He stands half a block back and doesn't touch them. The container opens by itself and he looks at it. Then **poof**: the package pops into his claw from nowhere, or swells and vanishes from his outstretched claw. He's baffled: he looks at his claw, at the container, back at his claw, then shrugs. The container shuts by itself and he gives it a startled glance.
- The hand-over happens on the exact frame his claw opens: the world item spawns at the claw's real position (solved in the generator).

**Trading with villagers** (`trade_villager`, `trade_villager_open`; ~7.2 s)
- **Game rules:** the villager must be **sitting on a seat** (in this mod villagers sit on seats). He trades from where he stands, up to **one full block** from the villager, with or without an obstacle in between (often the villager's own workstation, which can block the path). He never has to reach the villager. Everything happens through gestures and throws, so it reads the same at any distance up to a block.
- **The scene:**
  1. He spots the villager (a lens focus), lights his trade lamp, waves hi, and his drawer slides out to show the wares.
  2. He takes the package out of the drawer and holds it up high, above any workstation, to present it with a brass ping.
  3. Haggling: he chatters with head tilts. The villager shakes its head ("no"), he gives a quick pleading tilt, and the villager nods.
  4. He winds up, arm back and body leaning back, then lobs the package in a high arc that clears a full block. It lands in the villager's crossed arms, and the villager nods.
  5. The villager tosses an emerald back in an arc, and it drops right into his open drawer: *ding*.
  6. The drawer snaps shut. He does the trade-done celebration (a 360° spin, arms up and a hop with a happy trill), his lamp goes off, and he waves goodbye.
- **Throw arcs:** both throws peak well above the top of a full block (about 12 px above the start), so nothing ever passes through a workstation.
- **The villager side** (in game): only the head nod and shake are animated (through the existing villager model mixin). The package vanishes into the villager's inventory a beat after landing, and the emerald spawns at the villager's hands.

**Station: docking, waiting and leaving** (his rest state; he goes home when there's nothing to do, and at night)
- **Docking** (`dock_lights_out`): reverses up to the station glancing over his shoulder, glances back again, crouches and **reverse-hops** up into the cradle, landing with a heavy squash and two rebounds. He relaxes back against the post and folds his arms across his belly.
- **Lights out → station on** (the signature moment): his chest lamp clicks off, each iris flickers off in turn and his lids sink, while the station's ring and lamp window flicker on. The winder arm swings in and starts winding his key.
- **Waiting**: the winder turns his key and the station glow breathes. Rare tiny mannerisms (game-side): one iris peeks on and off, a claw twitches in a dream, or a sleepy shuffle in the cradle.
- **An action comes in** (`action_comes_in`): the flag springs up with a wobble and the station glow drains as his irises snap on (startled) and his lamp lights. The winder swings away, and he hops out forwards and rolls off.
- **At night** he stays docked with his lids fully shut and no peeks.

**Expressions as clips** (`expr_happy`, `expr_curious`, `expr_startled`, `expr_sad`, `expr_cross`, `expr_wave`, `expr_doze_off`): each one springs in, holds with the idle still running, and springs out, with the face parts arriving in a staggered chain. The game blends these on top of whatever else he's doing.

## Rig Requirements
Pivots are in model pixels as built in Blockbench: x is his right, y is up, and **his front faces −Z**. The origin is the ground under the axle.

| Bone | Parent | Pivot | Moves |
|---|---|---|---|
| `trade_bot` (root) | — | 0, 0, 0 | whole-model translate and turn (hops, rolls) |
| `wheel` | root | 0, 4, 0 (axle) | rotate X (spin) |
| `body` | root | 0, 4, 0 (axle) | lean, bank, bob: rotates around the axle like a unicycle |
| `drawer` | body | 0, 9.5, −4 | translate Z (out −3 px) |
| `lamp` → `lamp_glow` | body | 2.5, 13.5, −4 | lit cube shown or hidden (on/off) |
| `key` | body | 0, 12, 4.5 (back) | rotate Z, always turning |
| `arm_left` / `arm_right` | body | ∓6 / 6, 14, 0 (shoulder) | rotate X (swing), Z (raise sideways) |
| `forearm_left` / `forearm_right` | arm | ±6, 10, 0 (elbow) | rotate X, plus Y and Z for the folded and wave poses |
| `claw_left` / `claw_right` | forearm | ±6, 6, 0 (wrist) | rotate (flick, wave, fidget) |
| `held_item` | claw_right | 6, 1.5, 0 | the carried package; scale 0 when empty, and pops in or out |
| `neck` | body | 0, 16, 0 | translate Y (−1.5 to +2) |
| `head` | neck | 0, 18, 0 | pitch, yaw, roll; scale for the landing squash |
| `eye_left` / `eye_right` | head | ∓1 / 1, 19.5, −0.5 (inner edge at the bridge) | roll (tilt), pitch |
| `lid_left` / `lid_right` | eye | ∓4 / 4, 21.5, −4.5 | translate Y (+1 wide open, 0 neutral, −2 shut), rotate Z (brow) |
| `iris_left` / `iris_right` | eye | ∓4 / 4, 19.5, −3 | rotate Z (spin), scale (dilate) |
| `iris_glow_left` / `iris_glow_right` | iris | same | lit cube shown or hidden (on/off) |
| `aerial` | eye_left | −5.5, 22, 1 | rotate X/Z (lag, droop, "?") |

The lights (irises, lamp) are separate lit cubes over dark ones, so the renderer can draw them full-bright, dim them or hide them.

**Station rig** (block-entity renderer):

| Bone | Pivot | Moves |
|---|---|---|
| `station_base` | — | static: plinth, cradle, post |
| `winder_arm` | hinge on the post, at key height | rotate Y (swings in and out; rests swung out) |
| `winder_head` | on the key axis | rotate Z (turns the key) |
| `post_cog` | cog centre on the post side | rotate (turns with the winder) |
| `signal_flag` | hinge on top of the post | rotate Z (rests folded, springs up) |
| `station_glow` | — | glow ring and lamp window, shown or hidden |

**Preview props** (Blockbench only, not part of the mod): stairs, slab, full block, chest, barrel (closed top swaps out to show the dark hollow), depot, belt, and a chest on a block. Each appears only in its own clips; the others are parked out of view.

## Preview Clips (Blockbench, Animate tab)

| Clip | Length | Loop | What it shows |
|---|---|---|---|
| `idle` | 3.0 s | loop | balance sway, key, blink |
| `turn_left` / `turn_right` | 3.0 s | once | from rolling: look into the bend, bank into a 90° arc, swing back out into rolling |
| `turn_around` | 3.0 s | once | from rolling: brake, crouch, hop and spin 180° in the air, land, power off into rolling |
| `scared_spot` / `scared_flee` / `scared_hide` / `scared_cower` / `scared_recover` | 1.1 / 1.5 / 1.3 / 2.0 / 2.6 s | once / loop / once / loop / once | afraid of a hostile mob: spot, flee to a corner, hide small, shake, recover |
| `start_rolling` / `rolling` / `stop_rolling` | 1.3 / 3.0 / 1.6 s | once / loop / once | the game locomotion set, in place, chaining seamlessly |
| `wall_bump` | 2.8 s | once | hit, dizzy, head shake, glare, reverse |
| `count_stock` | 3.0 s | once | drawer out, look, nods, drawer slam |
| `sees_player` | 3.6 s | once | double-take, wave, turn, roll closer, open for trade |
| `dock_lights_out` | 6.0 s | hold | reverse in, reverse hop, lights pass to the station, winder |
| `action_comes_in` | 3.2 s | hold | flag, lights back on, hop out, roll off |
| `stairs_up` / `stairs_down` | 2.4 s each | once | two steps |
| `hop_on_slab` / `hop_off_slab` | 2.6 s each | once | 8 px |
| `hop_on_block` / `hop_off_block` | 2.9 / 2.75 s | once | 16 px |
| `chest_take` / `chest_put` | 6.6 / 5.6 s | hold | floor chest |
| `barrel_take` / `barrel_put` | 4.6 s each | hold | blind: poof and confusion |
| `chest_take_high` / `chest_put_high` | 4.6 s each | hold | blind: chest out of reach |
| `depot_place` | 5.6 s | hold | place, beat, push to centre, admire |
| `depot_take` | 3.8 s | hold | lean over, drag it to the edge, grab, carry |
| `belt_place` | 5.2 s | hold | place, watch it go, wave |
| `trade_villager` / `trade_villager_open` | 7.2 s each | hold | trade with a seated villager a block away, over a workstation / with nothing between |
| `expr_happy`, `expr_curious`, `expr_startled`, `expr_sad`, `expr_cross`, `expr_wave`, `expr_doze_off` | 3.3, 3.8, 3.0, 4.4, 3.6, 3.4, 6.8 s | once | smooth expression clips |
| `expression_sheet` | 8.0 s | once | the poses held one per second, for review |

## Sound Design Cue Sheet

**Status:** the real sounds are approved and in the Blockbench clips (each clip's Effects track; 303 sound keyframes across 41 clips). The full sound design (signature sounds, layered events, variations and implementation notes) lives in [design/sound/trade-bot.md](../sound/trade-bot.md). The cues are in [trade-bot.cues.json](../sound/trade-bot.cues.json), and you can listen on the [soundboard](../sound/trade-bot-soundboard.html). The sheet below is the original per-clip brief for the sound designer.

### His sound
- **Voice:** no words. Small, warm, musical **chirps, warbles and boops** with a clockwork-music-box flavour, close to Wall-E or a friendly R2 but quieter and rounder. Rising pitch for questions and curiosity, falling for disappointment, a quick bright trill for joy. Keep it sparse: he's a character, not a radio.
- **Body:** a soft **gyroscopic hum and rubber rolling** for the wheel, its pitch following speed. Tiny **servo whirs** for neck, arms and head turns (short and quiet, never constant). A faint **clockwork tick** from the wind-up key, about 2 ticks a second while it turns.
- **Materials:** brass and steel. Clinks, taps, gentle clunks and spring *boings* for the aerial. Landings are a rubbery **"bdoop"** plus a small metal rattle; heavier for bigger drops.
- **Lights:** a soft **click** when a light goes off, a tiny rising **"bwip"** when it comes on. The station glow has a very low warm **hum** while lit.
- **Mix:** everything small and close, quieter than a villager. Nothing harsh, no electronic glitch sounds. He's clockwork, not digital.

Times are seconds from the start of each clip. *Voice* cues are optional character chirps.

### start_rolling / rolling / stop_rolling
| Clip | Time | Event | Sound idea |
|---|---|---|---|
| start_rolling | 0.1 | wheel spins up | rubber rumble rising (`roll_start`) |
| start_rolling | 0.3 | aerial lags back | aerial boing |
| rolling | loop | cruising | seamless rubber rumble (`roll_loop`), quieter than the start |
| stop_rolling | 0.02 | brakes | rumble slowing, rubber scrub, soft settle (`roll_stop`) |
| stop_rolling | 0.15 | aerial whips forward | aerial boing |

### idle (3.0 s loop)
| Time | Event | Sound idea |
|---|---|---|
| 0.0–3.0 | balance sway; key turning | faint key ticking (about 2/s), very soft gyro hum |
| 2.2–2.4 | blink | tiny shutter tick (very quiet) |

### turn_left / turn_right (3.0 s, played over the rolling loop's sound)
| Time | Event | Sound idea |
|---|---|---|
| 0.15–0.5 | neck and head turn into the bend | small servo (head turn) |
| 0.5–2.0 | banks through the 90° arc | soft rubber scrub as the tyre carves the bend |
| 1.9–2.5 | powers out, swings back past upright | aerial boing as it whips back |
| 1.7 | blink | shutter tick |

### turn_around (3.0 s, played over the rolling loop's sound)
| Time | Event | Sound idea |
|---|---|---|
| 0.05–0.6 | brakes | rubber scrub, rumble slowing |
| 0.45–0.7 | glances over his shoulder | small servo |
| 0.6–0.78 | crouches, springs off | spring compress, then the hop launch |
| 0.8–1.2 | spins in the air, wheel whirring | swish of the spin; free-spinning wheel whir; *voice: small "wheee" chirp* |
| 1.22 | lands | heavy rubbery thump plus a metal settle (lighter than a block hop) |
| 1.3 | aerial whips down | aerial boing |
| 1.4–2.3 | powers off the other way | rubber rumble rising (`roll_start`) |

### wall_bump (2.8 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.0–0.5 | rolling forward | roll |
| 0.5 | **hits the wall** | metallic *bonk*, a clang on the brass, a spring twang |
| 0.5–0.8 | bounces up 1 px, arms fly up | rattle, landing "bdoop" |
| 1.0–2.0 | dizzy: irises spin twice, pods wobble | descending wobbly warble, birdie-chirp swirl, clockwork whirr |
| 2.05–2.5 | shakes head fast | quick servo zip-zip-zip |
| 2.35–2.8 | cross glare at the wall | *voice: grumpy low "hmph" buzz* |
| 2.45–2.8 | reverses away | roll |

### count_stock (3.0 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.15 | drawer slides out | wooden-metal drawer slide, little rattle of wares |
| 0.35–0.7 | peers down | soft neck servo |
| 1.0, 1.4 | two nods | *voice: two satisfied "mm-hm" boops* |
| 1.9 | blink | tick |
| 2.35–2.4 | drawer snaps shut | crisp drawer clack, aerial boing |

### sees_player (3.6 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.05 | notices, head snaps toward you | quick servo; *voice: curious rising "hm?"* |
| 0.5 | looks away | — |
| 0.68 | **double-take**: neck shoots up, lids wide | spring "boing-thwip", startled chirp |
| 1.1 | turns happy | *voice: bright rising trill* |
| 1.0–1.5 | turns to face you | wheel scuff |
| 1.5–2.35 | waves three times | light clink per wave |
| 2.4–3.2 | rolls closer | roll |
| 3.0 | drawer slides out, trade lamp lights | drawer slide, light "bwip" (emerald), little shop-bell *ding* |

### dock_lights_out (6.0 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.0–1.4 | reverses up to the station, glancing back | reverse roll; maybe a soft "beep-beep" reversing chirp |
| 1.4 | stops | rubber squeak |
| 1.5–1.78 | glance back, crouch | servo; spring compress |
| 1.78 | **reverse hop** launch | spring "boing", wheel whirr in the air |
| 2.2 | lands in the cradle; rebounds at ~2.25 and ~2.4 | heavy rubbery "bdoomp" into the cradle, two smaller bumps, metal settle |
| 2.5–3.1 | arms fold | soft servo |
| 2.55 | chest lamp off | click |
| 2.75–2.95 | left iris flickers off-on-off | tiny electric flicker ticks |
| 2.95–3.2 | right iris flickers off-on-off | flicker ticks; *voice: sleepy descending "mmm"* |
| 2.9–3.1 | station glow flickers on | warm "fwoom" swell into a low hum |
| 2.7–3.5 | lids sink | slow shutter slide |
| 3.3 | winder arm swings in and engages the key | brass swing, "clunk-click" of engagement |
| 3.9 → | winder turns his key | slow ratchet clicks (wind-up), post cog ticking |

### action_comes_in (3.2 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.05 | **signal flag springs up** and wobbles | brass "ding!" plus a spring wobble |
| 0.4 | station glow drains | hum fades with a soft "fwmp" |
| 0.42, 0.46 | his irises snap on (left, right) | two quick "bwip" on-sounds |
| 0.45 | startled: neck up | boing; *voice: "!" chirp* |
| 0.55 | chest lamp on | bwip |
| 0.6 | winder swings away | brass swing, unclick |
| 0.95–1.15 | crouch | spring compress |
| 1.15 | hops out | boing |
| 1.55 | lands on the ground | bdoop |
| 1.55 → | rolls off | roll |

### stairs_up (2.4 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.0–0.45 | rolls up to the step | roll |
| 0.45–0.6 | crouch | spring compress |
| 0.6 | hop 1 | boing; *voice: "hup!"* |
| 0.94 | lands on step 1 | bdoop, metal rattle |
| 1.11–1.26 | crouch | spring compress |
| 1.26 | hop 2 | boing (pitched up a step) |
| 1.6 | lands on step 2 | bdoop; aerial boing follow-through |

### stairs_down (2.4 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.0 → | rolling | roll |
| 0.54–0.74 | drops off step 2 | quick whoosh; *voice: "whee" chirp* |
| 0.74, ~0.92 | lands, double bounce | bdoop, small bdip |
| 1.15–1.35 | drops off step 1 | whoosh |
| 1.35, ~1.53 | lands, double bounce | bdoop, bdip |

### hop_on_slab / hop_on_block (2.6 / 2.9 s)
| Slab | Block | Event | Sound idea |
|---|---|---|---|
| 0.0–0.5 | 0.0–0.5 | rolls up | roll |
| 0.47–0.62 | 0.52–0.78 | crouch (deeper for a block) | spring compress (longer for a block) |
| 0.62 | 0.78 | launch | boing (bigger for a block); *voice: "hup" / effortful "hnnf-hup!"* |
| 0.62–0.98 | 0.78–1.28 | airborne, wheel spinning | wheel whirr |
| 0.98 | 1.28 | lands on top | bdoop (heavier for a block) |
| 1.43–2.13 | 1.73–2.43 | proud wiggle | *voice: pleased little trill* |

### hop_off_slab / hop_off_block (2.6 / 2.75 s)
| Slab | Block | Event | Sound idea |
|---|---|---|---|
| 0.0–0.55 | 0.0–0.55 | rolls to the edge | roll |
| 0.55 | 0.55 | small hop out | light boing |
| 0.55–0.93 | 0.55–1.05 | falls | whoosh (longer for a block); *voice: "whee" / "whoa"* |
| 0.93 | 1.05 | lands | bdoop / big **"oof" thud** and rattle |
| 0.93–1.26 | 1.05–1.38 | rebounds | two smaller bumps |
| — | 1.35, 1.6 | blinks it off | two shutter ticks; *voice: dazed "brr"* |
| 1.4 → | 1.55 → | rolls on | roll |

### chest_take (6.6 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.35 | hooks the lid | small claw clink on wood |
| 0.55 | lid swings open, overshoots | vanilla-style chest creak open |
| 1.3 | peeks in | *voice: curious "ooh?"* |
| 1.55–3.35 | rummaging, both claws | rustling, clinks, wood knocks, item shuffles |
| 2.4 | flings a cobblestone over his shoulder | whoosh |
| 3.1 | cobblestone lands behind him | stone clack and skitter (to 3.4) |
| 3.45 | **found it!** | *voice: triumphant rising trill*, small sparkle |
| 3.57–4.25 | holds it up high | — |
| 4.3–4.45 | shuts the lid | chest close thud |
| 4.7 | carry pose | soft servo |

### chest_put (5.6 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.35, 0.55 | hooks lid, lid opens | clink, chest creak open |
| 1.3 | leans in | servo |
| 2.0 | lets go of the package | soft cardboard thump inside |
| 2.15–2.75 | pats it | three soft pats |
| 3.0–3.15 | shuts the lid | chest close thud, small body jolt |
| 4.0–4.9 | dusts off claws | brushing, little metal rubs; *voice: satisfied "mm!"* |

### barrel_take / chest_take_high (4.6 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.5 | container opens by itself (barrel top swaps open, or high chest lid swings open); he perks up | barrel open (vanilla) or chest creak; *voice: hopeful "oh?"* |
| 1.3 | **poof**: package pops into his claw | soft magical "pop" with a tiny sparkle (cartoony, not epic) |
| 1.32 | double-take at his claw | startled chirp; servo snap |
| 1.65 | lifts the claw to inspect it | servo |
| 2.3 | looks at the container | *voice: puzzled rising "hmm?"* |
| 2.65 | back at his claw | *voice: "huh"* |
| 2.85–3.3 | shrugs | shoulder-cog clink, a tiny "mm-mm" |
| 3.7 | container shuts by itself; he flinches | barrel close / chest close thud; little startled chirp |
| 3.7–4.3 | settles | — |

### barrel_put / chest_put_high (4.6 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.5 | container opens by itself | barrel open / chest creak |
| 0.7 | holds the package out | servo; *voice: offering "here you go" boop* |
| 1.2–1.3 | package swells... | rising "fwee" |
| 1.3 | **poof**: package vanishes | soft "pop" |
| 1.5–2.2 | fidgets with the empty claw | small claw clicks |
| 2.3, 2.65 | looks at the container, then his claw | *voice: puzzled "hmm?" then "huh"* |
| 2.85–3.3 | shrugs | clink |
| 3.7 | container shuts by itself; he flinches | barrel close / chest close; startled chirp |

### depot_place (5.6 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.3–0.45 | leans and reaches | servo |
| 1.05 | sets the package down | cardboard on steel plate, soft clunk |
| 1.35 | head tilt: "hmm, not quite" | *voice: thoughtful "hmm"* |
| 2.0 | claw back to the package | — |
| 2.3–2.9 | pushes it into the middle | cardboard scrape across metal, short |
| 3.5 | leans back to admire it | *voice: pleased little trill* |

### depot_take (3.8 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.0–0.3 | rolls in 2 px, leans right over | short roll, servo creak |
| 0.3–0.8 | reaches out and puts his claw on top of the package | servo; claw tap on cardboard |
| 0.9–1.45 | drags it toward himself to the near edge | cardboard scraping across the steel plate |
| 1.5–1.65 | lets go, repositions | small clink |
| 2.05 | grabs it properly | cardboard squeeze / grip clink |
| 2.2 | lifts into carry pose | servo |
| 2.3–2.6 | happy "got it" bob | *voice: pleased boop* |
| 2.35 → | rolls back 2 px | short roll |

### belt_place (5.2 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.45 | reaches down | servo |
| 1.0 | sets it on the belt | cardboard on rubber, soft |
| 1.3 → | package rides away | (belt sound is the belt's own) |
| 1.2–1.5 | head turns to follow it | servo |
| 3.1–4.4 | waves goodbye | clinks per wave; *voice: "bye-bye" two-note boop* |

### expression clips
| Clip | Time | Event | Sound idea |
|---|---|---|---|
| `expr_happy` | 0.2 | springs into a happy face | *voice: bright trill* |
| | 0.4, 0.68 | two little hops | two small boings, bdips on landing |
| | 0.95–2.4 | happy wiggle | soft rhythmic squeaks |
| `expr_curious` | 0.25 | tilt and lean in | *voice: rising "hm?"* |
| | 0.9–2.8 | scanning, irises pulsing | tiny lens-focus clicks |
| `expr_startled` | 0.15 | snap, jolt back, small hop | boing, startled chirp |
| | 0.2–1.5 | aerial trembles | fast spring buzz |
| `expr_sad` | 0.3 | slow droop | *voice: falling "aww"* |
| | 1.1–2.3 | sigh (rise, then long sink) | mechanical sigh: slow whir down, soft hiss |
| `expr_cross` | 0.2 | lids flatten | shutter click |
| | 1.0–1.5 | puff and huffy head shake | *voice: grumpy buzz*, quick servo shakes |
| `expr_wave` | 0.2 | arm springs up, lamp on | servo, bwip |
| | 0.55–2.3 | three waves | clinks; *voice: "hi!" boop* |
| `shrug` | 0.15 | puzzled tilt | *voice: "hmm?"* (`puzzled_hmm`) |
| | 0.55 | shrug | shoulder-cog clink, a tiny "mm-mm" (`shrug`) |
| | 1.3 | settles | servo |
| `wind_down` | 0.3, 0.9, 1.7, 2.6 | slowing escapement ticks, a sag at each | `clock_tick`, further apart each time |
| | 1.2 | the body gives | servo |
| | 2.3–2.5 | eyes flicker and go dark | `flicker`, `light_off` |
| | 2.8 | the last of the spring | mechanical sigh (`sigh`) |
| `rewind` | 0.2, 0.65 | two key cranks | `key_rattle` ×2 |
| | 1.1–1.25 | eyes flicker on | `flicker`, `light_on` |
| | 1.3 | jolts upright, small hop | `startled_chirp`, `aerial_boing`, `land_soft` at 1.55 |
| | 2.2 | blink, settled | `blink`, `pleased_boop` at 2.4 |
| `expr_doze_off` | 0.4 | lids start sinking | slow shutter |
| | 1.2, 2.0 | two nod-and-catch dips | small servo jerk each |
| | 2.2–2.36 | eyes flicker off | flicker ticks, click |
| | 2.4–4.4 | sleepy breathing | slow soft whir in and out; *voice: tiny snore-buzz* |
| | 4.5 | **jolts awake** (hop, lids wide, eyes on) | boing, "bwip-bwip", startled chirp |
| | 4.9–6.4 | looks around | quick servos; *voice: sheepish "...hm"* |

### trade_villager / trade_villager_open (7.2 s)
| Time | Event | Sound idea |
|---|---|---|
| 0.05 | spots the villager | lens focus click |
| 0.3 | trade lamp on | bwip |
| 0.4–0.95 | waves hi | *voice: hi boop*, wave clinks |
| 0.95 | drawer slides out | drawer slide and rattle |
| 1.15–1.45 | takes the package from the drawer | servo, grip clink |
| 1.6–1.75 | holds it up high | servo, **brass ping** (signature) |
| 1.95–2.4 | haggles, head tilting | *voice: rapid haggling chatter* |
| 2.2–2.7 | villager shakes its head | vanilla villager "no" |
| 2.45 | pleading tilt | *voice: pleading chirp* |
| 2.8–3.25 | villager nods | vanilla villager "yes" |
| 3.0–3.22 | winds up | servo, spring compress |
| 3.38–3.97 | lobs the package over | whoosh |
| 3.97 | lands in the villager's arms | soft cardboard thump; vanilla villager "trade" |
| 4.45–5.0 | emerald tossed back | whoosh |
| 5.0 | **emerald lands in the drawer** | bright clinks, coin rattle, brass ping |
| 5.15 | drawer snaps shut | drawer clack |
| 5.3–5.8 | spin and hop | gyro whir and whoosh; *voice: merchant trill*; landing bdoop |
| 6.4 | lamp off | click |
| 6.45–7.0 | waves goodbye | *voice: bye boop*, wave clinks |

### Station (game-side ambience)
| Event | Sound idea |
|---|---|
| Glow on, while docked | very low warm hum, breathing gently with the glow (4 s cycle) |
| Winding | slow ratchet clicks in time with the key |
| Empty station | silent (an ember, no hum) |

## Fun Score (1-10)
**9**: the eyes and the unicycle wobble make him look alive even when he isn't doing anything. The drawer, the wind-up key, the dizzy wall bump and the baffled barrel moments give players things to stop and watch. The light passing between him and his station turns "going home" into a small ritual.

## Builder Notes
- **The eye pods and lid plates are the hero.** Never shrink them, keep the head wider than the body, and hand-place the lens pixels (dark glass, amber iris, one catchlight).
- Total height is 24 px including the aerial bead.
- He must look balanced over the axle in side view.
- Body roundness comes from stepped corners, not rotated cubes.
- Arms must clear the body when they swing, fold or wave, and must never pass through the eye pods.
- The palette stays muted. The lamp and irises are the only glow, and they stay small.
- Pivots sit at the real joints (the neck base for the head, the bridge for the eye pods, the aerial base), because lag and overshoot rotate around them.
- **Station:** lower and quieter than the bot, no taller than his key when docked. His wheel sits in the cradle and his key meets the winder head.
- **Everything is generated** by `design/models/trade-bot.gen.js`: geometry, texture, rig and all clips. Change the script and re-run it rather than hand-editing the model. `FK=1` prints claw hand-over positions and a frame-by-frame clearance check (wheel, body corners, drawer handle, key) against the climb props.
