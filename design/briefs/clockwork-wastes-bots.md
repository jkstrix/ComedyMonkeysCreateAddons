# Design Brief: Clockwork Wastes, Bots (group 3)

Group 3 of the Clockwork Wastes look ([spec](../specs/clockwork-wastes.md)): the **Scuttler**, the **Sentry** and **the Custodian**. They follow the look bible in [clockwork-wastes-world.md](clockwork-wastes-world.md). Their dead cousin, the **Worker** (whose shells lie everywhere), is designed in [clockwork-wastes-blocks.md](clockwork-wastes-blocks.md) §8 and sets the family face.

## Hard Requirements

From the user:
- "broken bots, technology is unknown" (2026-10-28).
- **Bots: their own family, "with eyes like ours":** built by the city's automatons from void-alloy, with lens eyes that echo Trade Bot's pods. As the pitch put it, "Scuttlers have one big lens, Sentries a ring of small ones, the Custodian a tall face of starlit seams" (user chose this, 2026-10-28).
- Lore: "A race of automatons whose makers were overthrown by their creations long before the cities were destroyed by war."
- The war: "automatons that found electricity and then turned on their masters, and the clockwork resistance: bots that helped their creators flee and fought back, eventually wiping each other out" (2026-10-28). **All three bots are the uprising:** electricity is what wakes them, so feeding the city power wakes its old soldiers. The resistance never moves (its dead are salvage blocks, in the blocks brief).
- Pale starlight glow.

From the spec:
- **Scuttler:** about half a block, 12 health, fast, **climbs walls, leaps**, 5 damage, groups of 3–5; drops 0–2 scrap (P18, P42). Wakes with its district (P8). The Custodian **calls 2 out of the walls** for each broken pylon (P35).
- **Sentry:** **fixed to a wall or floor**, 30 health, needs line of sight, **fires a bolt every 2 s** (7 damage, 24 blocks); **"unfold"**; **folds up and stops while its district is unfed** (P9, P19, P42).
- **The Custodian:** **about 3 blocks tall**; **wired to the heart's grid by four conduit pylons**; shielded and mending while any pylon stands; **at half health it overloads: faster, sparking, the heart's lamps surge**, and it re-lights one pylon; moves: **slam** (20, 2 blocks in front, **1 s wind-up**), **shockwave ring** (12, jump it, 8 blocks), **bolt volley** (3 bolts, 8 each), **chain arc** from overload (10, jumps to a second player); 400 health; boss bar; never breaks blocks; never leaves the hall; **kneels dormant on its dais** until the heart's doors open; resets by **walking back to its dais**; its death opens the heart vault (D30, D31, D35, P32–P40).
- Woken bots are a fixed set (D22); none spawn naturally (P6).

House and user rules:
- Dark, muted palette; glows muted, never near-white; accents as trims.
- Characters move with **spring-chain motion**, tilt and inertia, never stiff tweens (Trade Bot).
- **Self-collision-check every arm and every clip** (Helper Bot and Juggernaut: `*.collide.js`). Clipping fixes are **structural**: rotation-symmetric joints, elbows hinged at the front, roots lifted so feet stay on the ground.
- Tune damage against the user's gear (full Protection IV diamond within days).
- No Juggernaut motifs (runes, frost, knotwork, gold).
- Previewed in Blockbench first, with clips.

## Asset Type

Characters (hostile mob entities). 16 px = 1 block.
- **Scuttler:** about 8 px tall standing, 12 px long, 12 px across its legs.
- **Sentry:** 1 block (wall- or floor-mounted, an entity anchored to its block).
- **The Custodian:** 48 px tall (3 blocks), about 20 px across the shoulders; built at full size like the Juggernaut (pixels match the world).

## Fantasy

The uprising's last soldiers, asleep until someone brings back the electricity they rose up for. **Scuttlers** are its rats and its watchdogs: little machines that curl up in the scrap and come apart into a pack of legs and lenses when the lights come on. **Sentries** are its doormen: flowers of dark metal that open into a ring of eyes and don't miss. **The Custodian** is its priest-warden, built in the shape of the makers its people overthrew, tethered to the heart it guards and still keeping a vigil nobody asked it to keep.

## Hero Element

**The lens.** Every bot's face is a lens, or lenses, set in a ring: the family's "eyes like ours". Dormant, the lens is dark glass with one dull catchlight. Woken, it lights in starlight with a **single ember pupil** (`#b5452f`), and its **aperture** opens and narrows like a pupil (scale) to show interest, aim and anger. A player should learn to read every bot by its eyes: Trade Bot taught them how.

## Theme

Void-alloy bodies (`#16151c` `#1d1c25` `#25232f`, facet edges `#34304a`), starlight seams and lenses (`#3a3552` / `#7f76a8` / `#b8b0d8`), the ember pupil (`#9c3a2a` / `#b5452f`) on woken hostiles only. Makers' brass appears on them only as **scavenged** parts: a brass collar, a copper cable wrap, a citizen tag. They're automatons who've been patching themselves for ages. War damage: chipped facets, a cracked lens on some Scuttlers, scorch on the Custodian's lower robe.

They sit beside **Trade Bot and Helper Bot** (whose pods their lenses echo: same ring-and-lens build, same catchlight) and must look clearly **unlike the Juggernaut** (no runes, no stone, no Norse armour). Where the Juggernaut is heavy, wide and hunched, these are **thin, tall or low, and precise**.

## Shape Language

- **Scuttler:** **circles** on **triangles**: a round lens on a low, wedge-shaped body, six spiky legs. Quick and nasty.
- **Sentry:** **triangles** (petals and the bud's point) round a **circle** (the eye ring). Danger, folded into a flower.
- **The Custodian:** **tall rectangles** with chamfered tops (the city's language) and a **vertical stack of circles** for a face. Formal, upright, ceremonial; the robe's hanging plates make a long tapering column.

## Silhouette Description

- **Scuttler:** a low wedge with one big lens at the front and a ring of six thin legs: "a tick with a camera for a face." Curled dormant: a fist-sized round lump of plates, legs tucked, reading as scrap until it opens.
- **Sentry, folded:** a closed pointed pod on a mount: "a bud". **Open:** four triangular petals peeled back round a ring of six little lenses and a barrel: "a flower with eyes".
- **The Custodian:** a tall narrow robed figure with high square shoulders, long arms ending in heavy forearms, a tall chamfered head with three lenses stacked vertically, and **four cables trailing from its back** to the corners of the hall: "a robed priest on puppet strings." Kneeling: a bowed hooded shape on the dais, cables slack.
- **From 5 blocks:** the Scuttler reads by its leg-spread and single glowing eye; the Sentry by its open petals; the Custodian by its height, the stacked lenses and the four cables.

## Primary Forms

### Scuttler
1. **Lens head** (the hero): a round housing at the front of the body (5 × 5 × 3 px) holding **one big lens** (4 px across) in a void-alloy ring, a near cousin of Trade Bot's eye pod. A 1 px **lid plate** above it slides and tilts (expressions).
2. **Body:** a low wedge (8 × 4 × 6 px), deeper at the back, chamfered, with two overlapping **back plates** that open like beetle wings for the leap (they're its springs: a coiled starlight seam shows under them).
3. **Legs:** six thin two-segment legs (upper 1 × 4 px, lower 1 × 5 px, pointed tips), three per side, splayed wide.

### Sentry
1. **Mount:** a square void-alloy base plate (10 × 10 × 2 px) fixed to the wall or floor, with a short neck on a **yaw ring** and a **pitch knuckle**.
2. **Bud / petals** (the hero, with the eye ring): four triangular petals (each 8 × 10 × 1 px, chamfered, a starlight seam up the middle) closed into a point when folded, peeling back 120° when open.
3. **Eye ring:** six small lenses (2 px each) in a ring round a **barrel** (3 px bore, 5 px long) at the head's centre. Each lens can light, dim and narrow on its own.

### The Custodian
1. **Head** (the hero): a tall chamfered void-alloy housing (7 × 10 × 7 px) like a mitre or a hooded cowl seen front-on, with **three lenses stacked vertically** down its face: a big middle lens (4 px), smaller ones above and below (3 px). The face plate around them is plain, deliberately like the **blank plates the automatons bolted over the makers' statues**.
2. **Body and robe:** a narrow chest (12 × 12 × 7 px) with high square **shoulder blocks** (rotation-symmetric drums at the joints, under square covers), and from the hips down a **robe of hanging plates**: eight tall chamfered plates (each about 4 × 18 × 1 px) in two overlapping rings, split at front and back so the legs move freely between them. Each plate is its own bone, swinging with motion.
3. **Arms:** long thin upper arms, then heavy **forearm hammers** (6 × 9 × 6 px, chamfered, a starlight seam round the wrist), ending in three-fingered hands. The slam lands with the forearms.
4. **Tether spine:** four **sockets** down its back, each with a heavy cable (renderer catenary, makers' copper with void-alloy plugs, the Derelict Cable's look) running to a pylon. Shielded, the cables carry a crawling starlight pulse toward it.

## Secondary Forms

- **Scuttler:** a scavenged **brass collar** behind the lens on about one in three (variation); a cracked lens on a few (one dark crack line through the light).
- **Sentry:** a makers'-era **brass maintenance plate** on the mount (screwed, scratched), the only warm colour on it.
- **The Custodian:** a **citizen tag** that's not a citizen number but a broken-chain sigil, worn on its chest like a pectoral; a **scorched hem** on the robe plates (war); four small **thumb-studs** on its palms that light for the bolt volley (it fires from its hands).

## Interesting Features

- **Scuttlers sleep as scrap.** Dormant ones are curled into round lumps of plates sitting in scrap heaps and alcoves, lenses dark. When a district wakes they **unroll** (legs out one by one, lens lights, pupil snaps red).
- **The Sentry's ring of eyes all turn at once.** When it spots you, six lenses swivel to you together: the most unsettling beat in the city.
- **The Custodian's shield is a cage of light.** While shielded, a translucent starlight shell (renderer) wraps it, rippling at each hit like a soap film, the four cable pulses feeding it.
- **The Custodian bows before it fights.** It's formal: a short bow to the first player through the door, then it rises.
- **When it dies, it kneels to the vault.** It turns to the heart vault, kneels and bows its head; the lenses go out from top to bottom; the vault clunks open behind it.

## Personality

- **Scuttler:** a nosy, nervy scavenger that's brave in a pack. *Twitchy, curious, spiteful.*
- **Sentry:** a doorman who takes the job very seriously. *Watchful, deliberate, cold.*
- **The Custodian:** a priest-warden keeping a vigil for a city that's gone, sure it's still on duty. *Solemn, formal, relentless.*

## Face and Expressions

**Scuttler** (lens aperture, lid plate, body tilt):
| Expression | Lens | Lid | Body |
|---|---|---|---|
| Dormant | dark, dull catchlight | closed | curled ball |
| Neutral (woken) | lit, aperture mid | level | low, legs wide |
| Curious | aperture wide (scale 1.2) | raised | head tilt 20° |
| Hostile | aperture narrow (0.7), ember pupil | slanted down at the inside | crouched, back plates lifted |
| Stunned | lens flickers | half shut | on its back, legs paddling |
| Dying | cracks, goes out | slams shut | legs curl in |

**Sentry** (six lenses together or one at a time, petals):
| Expression | Lenses | Petals |
|---|---|---|
| Folded / unfed | dark | closed |
| Watching | dim, a slow ripple round the ring (one brightens in turn) | open 100° |
| Target | all six lit, all turn to the target, ember pupils | full open 120° |
| Aiming | lenses narrow (0.7), the bore gathers light | twitch tight |
| Hurt | the hit-side lenses blink out for 0.3 s | flinch to 70° and reopen |

**The Custodian** (three lenses, head tilt, posture):
| Expression | Lenses | Head and body |
|---|---|---|
| Dormant | dark | kneeling, head bowed |
| Waking | light one at a time, top to bottom | head lifts, then rises |
| Formal (the bow) | all mid | a 25° bow from the waist |
| Fighting | all lit, ember pupils, middle lens wide | upright, measured |
| Wrath (pylon broken) | apertures snap narrow, top and bottom go ember entirely | head snaps to the breaker, leans 10° forward |
| Shield down | flicker, then steady | looks at its own hands for 0.6 s |
| Overload | blaze to core tone, flicker | arches back, plates flare |
| Death | go out top to bottom | kneels to the vault, bows |

## Motion Set

**Motion feel (all three).** Spring chains: every part lags and settles after the one carrying it. The **Scuttler** is light and fast: its legs move in snappy tripods (three down, three up), the body bobs and banks into turns, and the lens is the last thing to settle, always sweeping. The **Sentry** is heavy and precise: the yaw ring turns smoothly with a slight overshoot, the petals are springy (a little flutter after each open). **The Custodian** is slow and deliberate: weight goes into every step, the robe plates swing and settle a beat after its legs, the head leads every turn, and the four cables sway behind it. Heavy: forearms, robe hem. Loose: robe plates, cables, Scuttler legs' tips.

### Scuttler
- **Locomotion:** tripod gait, quick (a full cycle in 0.3 s at run); the body banks 10° into turns and pitches forward when accelerating. **Climbing:** it turns to face up the wall and clings, legs splayed wider. **Leap:** back plates lift (0.3 s coil, body squashes 0.8), spring out (body stretches 1.2), legs tuck mid-air, land splayed with a 0.15 s settle.
- **Idle loop:** a constant small jitter of the legs' tips, a slow lens sweep, the body breathing (±0.3 px).
- **Mannerisms** (random, every 4–8 s):
  1. wipes its lens with a front leg;
  2. sniffs the ground: lens dips, aperture widens, then snaps back up;
  3. chitters: the whole body buzzes for 0.4 s;
  4. stretches its legs one at a time, front to back;
  5. huddles: if two are close, they lean together, lenses touching;
  6. picks at scrap: a leg pokes the ground and flicks a spark.
- **Reactions:**
  - **sees a player:** freezes (0.2 s hold), lens snaps to them and widens, pupil turns ember, crouches, then the pack moves together;
  - **gets hit:** flips onto its back, legs paddle (0.5 s), rights itself with a hop;
  - **a packmate dies:** every nearby Scuttler turns its lens to the body for 0.3 s (a beat of grief), then attacks harder (no stat change; it's a look);
  - **called out by the Custodian:** bursts out of a wall vent, tumbling, rights itself.
- **Rest:** curled ball (dormant); **waking:** legs unfold one by one (0.8 s), lens lights, pupil snaps red.

### Sentry
- **Locomotion:** none (anchored). Turning: yaw up to 120°/s with a 5° overshoot and settle; pitch ±60°.
- **Idle loop (woken, no target):** a slow sweep left-right (6 s), lenses rippling round the ring, petals breathing ±3°.
- **Mannerisms** (every 6–10 s): spins its barrel a quarter-turn and back (a self-check); one petal twitches; all lenses blink in sequence; it tilts to "listen" (pitch down 10°, holds 1 s).
- **Reactions:**
  - **target seen:** petals snap open (0.25 s), all lenses swivel to it together, ember pupils;
  - **firing:** lenses narrow, the bore gathers starlight (0.6 s, a pinpoint growing), the shot (a short starlight bolt), recoil (head kicks back 2 px, petals flutter);
  - **hit:** the hit side flinches, those lenses blink out for 0.3 s;
  - **district unfed:** a slow fold (2 s), petals closing like a flower at dusk, lenses going out one by one;
  - **destroyed:** petals splay open and droop, lenses go out in a spiral, the barrel sags.

### The Custodian
- **Locomotion:** a measured walk (no running); the robe plates swing a beat behind each step; the head leads turns, then the shoulders, then the hem. Speed rises 30% in overload with a slight forward lean.
- **Idle loop (in the fight, between moves):** stands upright, hands open at its sides, shoulders rising and falling slowly (a pump, not a breath), cables swaying.
- **Mannerisms** (in lulls, never twice in a row):
  1. straightens a robe plate with two fingers;
  2. turns its head to look at a broken pylon (a beat of mourning);
  3. taps fingertips on its palm, counting;
  4. turns its lenses to the vault behind it, then back.
- **Attacks** (each with anticipation, action, overshoot and settle):
  - **Slam:** raises both forearms over its head (1 s, lenses narrowing), brings them down together 2 blocks in front (impact frame, dust and a starlight crack on the floor), holds 0.3 s bent over, then straightens.
  - **Shockwave ring:** plants its feet, robe plates flare out (0.6 s), stamps; a starlight ring runs out 8 blocks along the floor (jump it); the plates fall back.
  - **Bolt volley:** raises one hand palm out, the thumb-studs light (0.5 s), three bolts in quick succession (0.2 s apart), the hand kicks back with each.
  - **Chain arc** (overload only): spreads its arms, lightning crawls over its robe (0.4 s), an arc leaps to the nearest player and jumps once.
  - **Calls Scuttlers** (on a pylon breaking): turns to the breaker, raises a hand; wall vents burst with 2 Scuttlers.
- **Reactions:**
  - **pylon broken:** head snaps to the breaker, lenses narrow and go ember top and bottom (wrath), a step toward them;
  - **shield down:** staggers back one step, looks at its hands (0.6 s hold), then the lenses steady and it advances;
  - **overload:** arches back, plates fan out, seams blaze, a long electric howl (no voice), one broken pylon jolts upright; the heart's lamps surge and flicker;
  - **hit while shielded:** the shell ripples, it doesn't flinch;
  - **hit unshielded:** a small recoil of the struck side, a plate rattles.
- **Rest, waking, reset and death:**
  - **dormant:** kneeling on the dais, head bowed, hands on knees, cables slack on the floor;
  - **waking** (the heart's doors open): lenses light top to bottom (1.5 s), head lifts, it rises (2 s), the cables lift off the floor and tighten, then **the bow** to the nearest player (1 s), then it fights;
  - **reset:** walks back to the dais, turns, kneels, bows its head, cables slacken; pylons re-light one by one;
  - **death:** stops, turns to the heart vault, kneels slowly (2 s), bows; lenses go out top to bottom; the robe plates drop one by one with a clatter (no items); the cables fall; the vault's iris spirals open.

## Rig Requirements

**Scuttler** (root at the body's centre, y 4):
- `body` → `head` (pivot at the body's front, y 5) → `lens` (scale for aperture), `lid` (pivot at its back edge);
- `plate_left`, `plate_right` (pivot at the body's spine, rotate up 40° for the leap);
- `leg_l1`–`leg_l3`, `leg_r1`–`leg_r3` (pivot at the body's side, rotation-symmetric hip knuckles) → `shin_*` (pivot at the knee).
- `curled` is a pose (legs folded under, plates closed, head tucked), not a separate model.

**Sentry** (root at the mount's centre, on its face):
- `mount` → `yaw` (yaw ring, pivot at the mount's centre) → `pitch` (knuckle, pivot 3 px out from the mount) → `head`;
- `petal_1`–`petal_4` (each pivots at its base hinge on the head's rim);
- `lens_1`–`lens_6` (each own glow part, scale for aperture); `barrel` (rolls on its axis); `bore_glow`.

**The Custodian** (root at the hips, y 22; lift the root to keep feet planted, as the Juggernaut):
- `waist` (rotation-symmetric drum) → `chest` → `neck` → `head` → `lens_top`, `lens_mid`, `lens_bottom` (each glow, aperture scale);
- `shoulder_left` / `_right` (drums centred on the arm pivots, under square covers) → `arm_*` → `forearm_*` (elbow hinged at the **front** of the arm) → `hand_*` → `finger_*_1`–`_3`, `thumbstuds_*` (glow);
- `leg_left` / `_right` (round hip) → `shin_*` (round knee) → `foot_*` (round ankle);
- `robe_1`–`robe_8` (each pivots at the hip ring; swing ±25°, flare out 40° for the shockwave and overload);
- `pectoral` (the sigil, glow);
- `socket_1`–`socket_4` on the back (cable anchor points for the renderer's catenaries);
- shield shell and arcs on the renderer.
- **Collision:** a `custodian.collide.js` checks every clip, part against part: robe plates against legs in the walk, forearms against the head in the slam, plates against each other in the flare.

## Fun Score (1-10)

**9.** Three bots with three clear jobs and three ways of using the same face: the Scuttler's single snapping eye, the Sentry's ring of eyes turning together, the Custodian's stacked lenses going out as it kneels to the vault. The Custodian is the city's whole story in one figure: built in its makers' shape, still bowing at its post.

## Builder Notes

- **Sizes:** Scuttler lens 4 px in a 5 px ring, body 8 × 4 × 6, legs 1 px thick; Sentry petals 8 × 10 × 1, lenses 2 px, bore 3 px; Custodian 48 px tall, head 7 × 10 × 7, lenses 4 / 3 / 3 px, forearms 6 × 9 × 6, robe plates 4 × 18 × 1.
- **Lenses on the pixel grid** with a hand-placed catchlight pixel in each, as Trade Bot. That catchlight is what makes them alive.
- **The ember pupil is one or two pixels,** only when hostile; never a red-lit face.
- **Don't make the Custodian a Juggernaut:** thin, tall and upright where he is wide, hunched and heavy. No stone, no runes, no shields.
- **Robe plates are the clipping risk.** Split front and back, plates as separate bones, hips rotation-symmetric; run the collide check on walk, slam, shockwave and kneel.
- **Scuttler legs at 1 px** can vanish at distance: give the shins a 1 px darker underside and the tips a 1 px lighter point, and check from 10 blocks.
- **Clips to make in Blockbench for review:**
  - Scuttler: walk, run, climb, leap, the six mannerisms, sees-player, flipped, curled, unroll, death;
  - Sentry: fold / unfold, sweep, target snap, fire, hit, unfed fold, death;
  - Custodian: dormant, wake and bow, walk, slam, shockwave, volley, chain arc, the four mannerisms, pylon-broken wrath, shield down, overload, reset kneel, death kneel.
- **Screenshot for review:** each bot front, side and three-quarter; each face's expression row; the Custodian beside the Juggernaut and a player for scale; a Scuttler curled in a scrap heap (can you spot it?).
