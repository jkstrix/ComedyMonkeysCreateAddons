# Design Brief: Clockwork Jetpack

Design brief for the Clockwork Jetpack and the Netherite Clockwork Jetpack: worn model, its Winding Station, item icon, Curios slot icon, and the moving parts and preview clips. Gameplay is settled in [the spec](../specs/clockwork-jetpack.md) (D1–D45); this brief covers only the look and the motion.

## Hard Requirements
User calls at brief approval (2026-11-03): **stand-off rails** for the backpack fit; **wing blades of oiled canvas on brass ribs**. During modelling: "The wings want to read more davinci" (wooden ribs, stretched canvas, scalloped edge). Execution changes from the build: the wings stow between the plate and the drum, so folded they never hide the drum (the hero). After the first in-game test (2026-11-03): the rails looked silly behind a backpack, so **pack mode** replaced them ("the backpack merged with the pack looks better"); a backtank or Create Jetpack can no longer be worn with it (spec D23); and the drum **tucks in** against the plate once the wings are out ("the body should move inwards to fill that gap after the wings extend and move out before the wings retract").

User's words (2026-11-03), carried over verbatim:
- "a clockwork jetpack ... a similar pattern to the create jetpack mod but **instead of a back tank it uses the mainspring** and **instead of the elytra it uses a precision mechanism**"
- "we also need to make **a new curio slot** for it so **we can wear back packs and clockwork jetpacks at the same time**"
- "a flight mode, so it's like an elytra without rockets and a little bit faster, **wings should extend when this is active**"

From the spec and the mod's rules:
- **Worn** in the new Curios **Jetpack** slot, or in the chest slot without Curios (D18, D21). It must sit **over a chestplate** and **alongside a back-slot backpack** (Sophisticated Backpacks: a 10 × 13 × 6 px box with 2 px side and front pouches, so 14 px wide and 8 px deep on the back). It must also clear a worn Create backtank or Create Jetpack in the chest (D23).
- **Wound only at a Jetpack Winding Station** (user, 2026-11-03: "it should have a winding station like the trade bot (except running on SU)"); the jetpack has no block form (D12, D38–D45). It shows its wind while winding.
- **Wings unfold** for flight mode and **fold** on landing, in water or when riding (D14, D28, D29); visible to other players.
- **Thrust is not air**: the jetpack holds no air (D20). This brief decides the thrust look.
- Wind must be readable on the worn model without a duplicate dial (the user's "no duplicate dials" rule).
- **Also make:** the item icon, the Curios slot icon and the **Netherite Clockwork Jetpack** (D19).
- **Our own design only.** Create Jetpack's assets are All Rights Reserved.
- House rules: dark, muted palette; brass as trims; no hazard stripes; no glow unless it means something; crafted machines look new; preview everything in Blockbench first.

## Asset Type
**Item with moving parts** (wearable), plus the **Winding Station** block. Not a character: no face, no personality clips.
- **Worn model:** attached to the player's body bone, in player-model pixels (1 px = 1/16 block). Folded: 8 px wide (the torso's width), 14 px tall, 6 px deep including the key. Wings open: about 36 px across (2¼ blocks).
- **Winding Station:** a 1 × 1 block whose post and winder rise to about 1½ blocks (see its section below).
- **Item icon:** a hand-drawn flat 16 × 16 sprite.
- **Slot icon:** a 16 × 16 Curios empty-slot silhouette.

## Fantasy
**A wind-up toy for grown engineers:** the Juggernaut's own Mainspring, caged in a brass harness, with a winding key turning on your back as two little fans whirr you into the air and a pair of canvas fan-wings snaps open for the long flight home.

## Hero Element
**The winding key on the spring drum.** A round drum sits between your shoulder blades, its face looking backwards, with a two-loop brass key on its axle that **turns while the jetpack runs**. Anyone behind you reads "wind-up" instantly. It's the family's key: Trade Bot has a little one on his back, the Juggernaut a giant one, and this is the one you took from him.

The **folding fan-wings** are the reveal: hidden at rest, the showpiece in flight. They support the key and never compete with it on the ground.

## Theme
The jetpack is player-built, so it belongs to the **Aeronautics set** (aviator steel, mid brass, walnut accents), with the **Spring Engine's** coil porthole and the **family winding key** as its clockwork links.

| Material | Shades | Used for |
|---|---|---|
| Dark blue-grey steel | `#2f343b` `#3c424a` `#4a5059`; near-flat panels (`#3c424a` with a rare `#40464e`) | Back plate, drum shell, fan ducts |
| Mid brass (no highlights) | `#7e6832` `#8f7638` `#a3873f` | Key, drum bands, precision-mechanism hubs, wing ribs and tips, gear train, buckles |
| Oiled canvas (mid-grey ceiling) | `#5f5747` `#6f6552` `#7d725c` | Wing blades: canvas stretched on brass ribs, like an ornithopter (user, brief approval 2026-11-03) |
| Walnut (a step lighter, so it doesn't go black in game) | `#4e3626` `#634532` `#6f4d36` | Harness cheeks, the Aeronautics accent |
| Spring steel (as the Spring Engine's coil) | `#8a949c` `#6d7780` `#4a525a` on `#16181b` | The Mainspring seen through the porthole |
| Glass (cutout) | edge `#6f7f87`, glint `#a9bac2` | The porthole ring |
| Leather / carbon | `#3b2a1f` `#46321f`; `#26272a` `#323337` | Harness straps; fan rotor hubs |

**Netherite version:** the steel parts become netherite (`#2e2a2b` `#3b3536` `#4a4344`, a faint warm-violet cast, gently mottled, the Netherite Backtank's language). **Every brass part becomes netherite metal** (`#433d40` `#4d4649` `#5a5255`), rims darkest (`#2a2526`): the key, hubs, ducts, bands, ferrules, knuckles, gears, pawl, buckles and rails (user, 2026-11-03: "darker rims for netherite version, paint out the brass with the netherite colour"). The canvas becomes **soot-dark crimson-dyed canvas**, muted (`#3f2226` `#4a2a2e` `#57333a`), as a Nether nod. Same shape, nothing added.

No glow anywhere: it's clockwork. Nothing is lit.

## Shape Language
**Circles lead**, as they do for motors and engines (the user's "round is for motors and engines" rule): the drum, the porthole, the key's loops, the round fan ducts, the precision-mechanism hubs. **Triangles** come out only with the wings: a fanned wing is a wedge, and speed is the point of flight mode. The back plate and harness are quiet squares that hold it all.

## Silhouette Description
- **From behind, folded:** a round drum with a two-loop key sticking out of its middle, sitting on a narrow back plate. Two short round ducts hang under it, and the folded wings make a closed **V** over the drum's lower edge. It reads as a **wind-up toy key on a backpack**.
- **From the side:** a shallow stack against the back (plate, drum, key) with the key's bow standing proud. The ducts point straight down past the belt.
- **Wings open:** a wide, swept, slightly downturned fan on each side, like a hand fan or a bat's wing. Five canvas blades per side, spread from a brass hub at each shoulder blade. It reads as **"gliding"** from across a valley.
- **Inventory:** the drum and key in the middle, a duct under each side, the folded wing edges peeking out.

## Primary Forms
1. **Back plate (8 × 12 × 1 px)**, dark steel with a crisp bevel, strapped to the torso. It carries everything.
2. **Spring drum (8 px across, 3 px deep)**, stepped-round, set on the upper back: centre 8 px up from the bottom of the torso, so its top sits about at the shoulder line. Two brass bands round its rim.
3. **Winding key**: a 1.5 × 1.5 px square stem, 2 px long, out of the drum's centre; a **two-loop bow**, 7 px across and 3 px tall, 1 px thick, turning in the plane of the back. Its shape is the Juggernaut's key in miniature.
4. **Twin fan ducts (3 × 3 px, 4 px tall, riveted brass**: user, modelling 2026-11-03, "the downward pipes should be brass and more textured, less long"**)**, under the drum's lower corners at x ±1…±4, ending at the bottom of the plate. They stay inside the torso's width so a swinging arm never hits them.

## Secondary Forms
- **Coil porthole**: a glass ring in the drum's face round the key's arbor (inner radius 1.5, outer 3.5 px). The **Mainspring** shows through it in the Spring Engine's coil style: **full is a tight mass round the arbor; empty is a loose ring at the wall**. The renderer picks one of 4 coil states by wind, and that's the gauge (no dial).
- **Precision-mechanism hubs** (the user's "instead of the elytra it uses a precision mechanism"): one at each top corner of the back plate (x ±4, 2 px below the top). Each is a round brass hub, 3 px across, with three tiny cogs on its face, styled after Create's Precision Mechanism item. The wing blades pivot from them.
- **Fan-wings, da Vinci style** (user, modelling 2026-11-03: "The wings want to read more davinci"): 5 finger-ribs a side, like Leonardo's ornithopter and bat-wing studies. A heavier **walnut leading spar** (13 px, brass ferrule at the tip), then four walnut ribs, each shorter than the last (12, 11, 10, 9 px), radiating from the precision-mechanism joint. Oiled canvas is **stretched between the ribs**, its trailing edge **scalloped** where it sags between the rib tips, with cord lashings dotted along each rib. Folded, the ribs close together like a bat's fingers and stow between the back plate and the drum. **Jointed** (user, 2026-11-03: "needs to have a greater wingspan"; chose a jointed elbow and a 3-block span): every rib has a brass knuckle halfway, like Leonardo's articulated wings. Spar 20.5 px (10.5 + 10), then 19.5, 18.5, 17 and 15.5 px; the inner halves are 10 px. Open, the wings span **48 px (3 blocks)** and fan from +5° to −31°. Folded, each outer half swings back up beside its inner half, and the whole wing still stows behind the drum. Unfolding is two-stage: the fan spreads, then the elbows straighten.
- **Gear train**: one small brass cog on each side between the drum and its duct (2 px), turning with the fans. It shows the spring drives the fans.
- **Fan rotors**: a 4-blade rotor (carbon hub, steel blades) inside each duct's mouth, visible from below and the side.
- **Click pawl**: a small brass pawl on the drum rim at 2 o'clock, over the brass band. It ticks while winding.
- **Harness**: two leather shoulder straps and a belt strap with brass buckles, from the plate over the shoulders and round the waist, sitting 0.25 px proud of a chestplate.
- **Pack mode** (the backpack answer, replacing the stand-off rails): with a backpack on (Curios' back slot, or a backpack in the chest), the jetpack is built into it. The drum stands 1 px further out, through the pack's face, seated in a stepped dark-steel flange (one pixel round the drum's steps, four brass rivets) that hides the cut; the ducts drop 2.5 px so their mouths hang out under the pack; the folded wings stow inside the pack and unfold out of its sides. No tuck in pack mode (the gap is inside the pack).
- **Drum tuck:** once the wings are fully out, the drum and all it carries (glass, key, pawl, gears, ducts, rotors) slide 3.5 px in against the plate, filling the gap the wings left; before they fold, it slides back out. The inner edge of each canvas panel is trimmed so the open wings clear the tucked drum.

## Interesting Features
- **The key tells you the state.** Still: off. Steady turn: hovering. Brisk turn: thrusting. Slowing, then stopped with a last shiver: run down. Winding on the block: it turns the other way, fast, with the pawl clattering.
- **The coil porthole is the fuel gauge**, the same language as the Spring Engine. A player who knows one reads the other.
- **The wings open like a hand fan.** The blades cascade out one after another, overshoot and settle. Folding, they snap shut in reverse order.
- **The fans honestly push you up.** You can see two rotors blur inside the ducts. **Thrust look:** the rotors blur, with Create's own encased-fan **air-current streaks** (the thin lines an Encased Fan blows) coming down from each duct mouth, and a kick of **ground dust** within 3 blocks of the ground. No smoke, no puffs, no flame.
- **Wearing it with a backpack looks engineered**: the jetpack is built into the pack, its drum through the pack's face.
- **The Winding Station is the jetpack's home.** Hang it on the post, or back into it wearing it: the winder arm swings in, fits over the key and turns it, and you watch the coil tighten in the porthole.
- **Netherite:** dark netherite plates and trim all over, and crimson-dyed canvas wings.

## Rig Requirements
Every moving part is its own bone. Pivots are in player-model pixels relative to the body bone's origin (neck, 0, 0, 0; +z is backwards).

| Bone | Child of | Pivot | Moves |
|---|---|---|---|
| `root` | player body | 0, 0, 0 | Static. Offsets 0.25 px out from a chestplate's back face |
| `body` | `root` | back face of torso + chestplate | Static (pack mode: everything but the flange and the harness 1 px out) |
| `frame` | `body` | (0, 6, 0) | Static: plate, boss (and the flange in pack mode) |
| `drum` | `frame` | (8, 12, 8.5) | Slides −3.5 px in z (the tuck) once the wings are out: drum shell, glass, key, pawl, coils, gears, ducts, rotors |
| `key` | `frame` | drum centre | Rotates about z (the drum axis) |
| `pawl` | `frame` | pawl hinge on the rim | Small rotation, ±10°, a click per tooth |
| `coil_0…coil_3` | `frame` | drum centre | Visibility only: the renderer shows one coil state by wind |
| `gear_L`, `gear_R` | `frame` | each cog's centre | Rotates about z with the fans |
| `rotor_L`, `rotor_R` | `frame` | each duct's centre axis | Spins about y |
| `hub_L`, `hub_R` | `frame` | hub centres, x ±4 | Rotates about z as the wing opens (the mechanism turning) |
| `blade_L1…L5`, `blade_R1…R5` | their hub | hub centre | Each rotates about z on its own angle (the fan spread), plus a shared small x tilt for trim |

**Wing angles** (in the plane of the back; 0° points straight out sideways, negative is down):
- **Folded:** every blade at −105°, so they point down and slightly inward over the drum's lower edge and form the V.
- **Open:** blades at **+12°, −4°, −20°, −36°, −52°**, from the leading blade to the trailing one. Each pair overlaps by about 1 px at the hub.

## Motion
Machine motion: crisp, geared and springy, never floaty. The key and gears move in **ticks** (a stepped rotation, like a clock's escapement); the rotors spin smoothly; the wings move with **overshoot and settle** like a real snapping fan.

Preview clips (Blockbench):

| Clip | Length | What happens |
|---|---|---|
| `idle` | loop | All still. The coil sits at its wind state. |
| `thrust` | loop, 1 s | Key ticks round once per second (12 steps), rotors blur, gears turn, the pawl rests. |
| `hover` | loop, 2 s | Same, at half speed (hover uses half the wind). |
| `wings_unfold` | 0.5 s | Hubs turn 90°. Blades cascade out, each 0.05 s after the last (L1 first), overshoot 8° and settle. A 0.05 s anticipation first: the blades tuck 5° tighter. Once they're out (0.9 s), the drum tucks in against the plate. |
| `flight` | loop, 1.5 s | Wings open, the key turning steadily. Each trailing blade trembles ±1.5°, out of phase, like air on a fan. Rotors spin slowly. |
| `wings_fold` | 0.35 s | Reverse cascade (L5 first), no overshoot; the last blade lands with a 2° bounce. The drum slides back out first (0.2 s). |
| `run_down` | 2 s | Key and rotors slow to a stop over 1.5 s; one last half-tick shiver; the pawl drops with a click. |
| `winding` (block) | loop, 1 s | Key turns backwards, fast; the pawl clicks every 30°; the coil steps from `coil_0` to `coil_3` over the clip. |
| (pack mode) | — | Shown on the left Steve: the jetpack built into a Sophisticated Backpack; it unfolds in `wings_unfold` and flies in `flight`. |

## Fun Score (1-10)
**9.** A wind-up key turning on your back is instantly charming and readable, the fan-wings snapping open are a moment players will show friends, and the coil porthole ties it to the Spring Engine without a single dial.

## Builder Notes
- **Proportions (worn, folded):** plate 8 × 12 × 1; drum 8 Ø × 3; key stem 2 long; key bow 7 × 3 × 1; ducts 3 × 3 × 6; hubs 3 Ø; blades 14 × 2.5 × 0.5. Total depth with the key: 6 px (rails collapsed), 14 px (extended).
- **Clearance (must check with a clipping test, as the Juggernaut's `collide.js`):**
  - head turned and pitched ±60°: the back of the head is at z +4, and the key and drum must stay behind it;
  - arms swinging ±45° (walking, sprinting): ducts and folded blades stay inside |x| ≤ 4;
  - a chestplate (armour adds 1 px all round);
  - a Sophisticated Backpack (its worn model's boxes) with the jetpack built into it (pack mode);
  - the fall-flying pose with the wings open.
- **Stand-in props for the preview, never exported:** a Steve body, a chestplate shell and a 14 × 13 × 8 backpack box.
- **Don't lose:** the key in the middle of the drum, turning in the back's plane (it's the hero); the porthole coil states; the five-blade fan, not a solid wing.
- **Pitfalls:**
  - 0.5 px blades z-fight when stacked folded, so stagger them by 0.1 px along z;
  - circles at 8 px need stepped corners on the pixel grid (no hypot blur), and the 3 px ducts are stepped too;
  - brass stays mid brass (no `#c9a75a`), since "too bright" was the Aeronautics call;
  - canvas stays at or under `#7d725c` (mid grey at most: "the light areas are too light"); walnut a step lighter than the house walnut so it doesn't go black in game.
- **No block form** for the jetpack itself (D12): it is wound at its Winding Station.
- **Item icon:** a flat, hand-drawn 16 × 16 back view (drum, key, ducts, folded-wing edges), lighter than the block textures, like the Mainspring icon.
- **Slot icon:** a 16 × 16 light-grey outline in the style of vanilla's empty armour-slot icons: the drum circle, the key bow and two duct stubs. It must read at 50% in the Curios screen.
- **Netherite:** the same model with a texture swap only.
- **Screenshot for review:**
  - back, side and ¾ views folded, on a Steve with a chestplate;
  - the same with the backpack stand-in (pack mode);
  - wings open from the back and in the fall-flying pose;
  - the four coil states close up;
  - the block on a shaft;
  - the item and slot icons at 1× and 4×;
  - the Netherite version beside the plain one.

## Winding Station (block)
**A coat stand for a flying machine:** a winding post the jetpack hangs on, with a winder arm like the Trade Bot station's that swings in and turns the key. Running on rotation, it belongs to the same aviator workshop as the jetpack (Aeronautics steel, mid brass, walnut).

- **Hard requirements** (spec D38–D45):
  - one jetpack, hung on it or backed into while worn;
  - a shaft from below;
  - wind rate by RPM;
  - no light;
  - it faces the player when placed;
  - funnels and arms can load and unload it.
- **One winding height for both uses.** On a standing player the key sits about **20 px** above the floor, and on the station the jetpack hangs so that its key sits there too. So the post rises above the block, to about 24 px. The collision box stays one block; only the post and arm stand proud.
- **Plinth** (y 0–4): a dark steel base, 14 × 14, with a crisp bevel. Underneath is a brass shaft socket (like Create's backtank); on top, a brass bevel-gear housing at the back of the base turns the drive up the post.
- **Post:** a walnut upright at the back, 3 × 3, from the plinth to y ≈ 24. It has a slot window down its front where a thin steel drive shaft turns (you see the rotation travel up), and brass collars at the top and bottom.
- **Hanger:** a brass yoke at the top of the post. The jetpack hangs by its harness straps, plate against the post, drum and key facing out the station's front, key centre at y ≈ 20.
- **Winder arm** (the Trade Bot echo): a brass crank arm on a pivot at the post's top. At rest it lies folded up against the post. When winding, it swings down and out to the front and its square socket fits over the key's hub; then it turns with the drive. For a backed-in player it reaches just past the station's front face, to where their key is.
- **Click pawl:** on the arm's pivot, clicking as it winds (the jetpack's own pawl clicks too).
- **Empty station:** the arm folded up, the yoke bare, the drive shaft still turning in its slot if the shaft turns. You can see it's live.
- **Restraint:** brass only on the yoke, arm, collars and socket. No glow and no stripes; the jetpack's porthole is the gauge.
- **Netherite jetpack on it:** no change to the station.
- **Rig:**
  - `arm` pivots at the post top (swings about x, down and out);
  - `socket` spins about z at the arm's end;
  - `drive` spins about y in the post's slot;
  - `pawl` is a small rotation;
  - `hung` holds the jetpack, hidden when empty.
- **Clips:**
  - `station_hang`: put the jetpack on; the arm swings in with overshoot; winding; full stop;
  - `station_back_in`: a Steve backs in and sneaks; the arm reaches his key and winds; he steps away and the arm swings back up;
  - `station_idle`: empty, the drive turning.
- **Fun score: 8.** It's the jetpack's home and a clear Trade Bot echo; the arm reaching out to a player's back is the moment.

