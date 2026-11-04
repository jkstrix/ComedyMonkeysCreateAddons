# Design Brief: Trade Master

Character brief for the Trade Master: the Trade Bot, upgraded. **Everything not written here is exactly as in the [Trade Bot brief](trade-bot.md)**: geometry, palette, rig, motion, clips, sounds and builder notes. This brief lists only what changes and the rules that keep his clips identical.
Spec: [design/specs/trade-master.md](../specs/trade-master.md). Base model: [trade-bot.bbmodel](../models/trade-bot.bbmodel) from [trade-bot.gen.js](../models/trade-bot.gen.js).

## Hard Requirements
- "Lets model him up, remember, trade bot, + changes" (user, 2026-11-04)
- "the animations and noises should be the same" (user, 2026-11-04)
- "His look should also have a beard (as the helper bot has a moustache)" (user, 2026-11-04)
- Beard: **a brass whisker grille** (user R1, 2026-11-04)
- Upgraded metals: **netherite frame plates**: his dark-steel frame edges and lid plates become netherite trim; **his brass stays** (user R1)
- Extras: **a Stock Link aerial** (bulb instead of the brass bead), **a monocle**, **a ledger on his back** (user R1)
- "change his eye colour to make him a bit more distinct and give the netherite a more burgundy hue like netherite armor" (user, 2026-11-04); eyes: **muted violet** (user's pick)
- "The monocle should be smaller and should move for things like when he's startled or when he sees you, like a comical cartoonish monocle" (user, 2026-11-04)
- Everything in the Trade Bot brief's Hard Requirements still applies (1.5 blocks tall, one wheel, two arms, Wall-E face, Create colours, fluid tilting motion, his station and every clip)
- He is an upgraded Trade Bot: a new robot with an upgraded look (spec D16); bot-family parts from the Trade Bot (house rule)

## Asset Type
Two assets, as for Trade Bot:
1. **Trade Master**: character, 24 px tall (1.5 blocks), `modded_entity` (box UV), code-driven animation from the preview timings. **Trade Bot's rig, bone for bone, plus four new bones** (Rig Requirements).
2. **Trade Master Station**: the Trade Bot Station with his netherite trim (proposed below). The same geometry and rig.

Later, in their own briefs: the Trade Board, the Trade Ledger item and the Netherite Sheet item.

## Fantasy
The Trade Bot who made it: a bearded old clockwork merchant with a monocle and a ledger, who runs the whole shop from his network.

## Hero Element
**Still the binocular eye pods**, now framed by a **brass whisker-grille beard** below them. The pods carry the expressions; the beard makes him the senior, and the bot family's moustache-to-beard step reads at a glance (Helper Bot moustache → Trade Master beard). The monocle sits on the hero as its one accessory.

## Theme
Trade Bot's palette ([brief](trade-bot.md#theme), [visual language](../visual-language.md)), with one swap and a few additions:

| Material | Where | Colours |
|---|---|---|
| **Netherite trim**, taken from vanilla netherite armour (user, 2026-11-04: "look at the armor colours for inspiration, don't just go with burgundy") | **body frame edges** (the dark-steel frame around his andesite panels), **lid plates** and the station plinth frame; lid plates keep a lighter leading edge | armour's plum `#3f303b` (main), warm brown-black `#322727` (flecks), dusty plum `#51444e` (light), grey-lilac leading edge `#5d565d` |
| Dark blue-grey steel (kept) | eye pods, neck, upper arms, wheel parts that were steel | as Trade Bot |
| Bot brass (kept) | eye rims, waist band, shoulder cogs, key, drawer handle, claws, **beard, monocle and chain, ledger clasp** | `#6e5a2a` `#8f7638` `#b08f45`, `#c9a75a` highlights only |
| Copper (kept) | wheel fork, aerial rod | as Trade Bot |
| Ledger leather (new, small) | the ledger's cover | walnut from the generator `#3b2a1f` `#4e3626` `#5a3f2c` |
| Stock Link bulb (new glow) | aerial tip | Create's Stock Link red, muted; blinks briefly only (Builder Notes) |
| Glow | irises **muted violet** (user, 2026-11-04), so he's distinct from Trade Bot's amber and Helper Bot's cyan; his **station glow matches** (his light still moves into the station when he docks); trade lamp stays muted emerald | iris `#9c80c9` `#9276bf`, centre `#b89ee0`, catchlight `#e2d6f0`, unlit `#2e2638` `#362c42` |

Netherite and dark steel sit close in value; the netherite reads by its warm, slightly purple cast. Brass stays the warm accent, so he still reads as Trade Bot's family from across a room.

## Shape Language
As Trade Bot: **circles** dominate. The beard adds one **downward spade** under the round pods, the only pointed shape, and it points down (calm, not aggressive).

## Silhouette Description
Trade Bot's wine glass with binoculars, plus a beard: under the wide head, a brass spade hangs over the neck to the top of his chest, so the gap between head and body fills in. From the side, a small book shape on his back beside the key. The aerial ends in a glass bulb instead of a bead. Total height stays **24 px**.

## Primary Forms
Unchanged from Trade Bot (eye pods, body, wheel). The beard is the largest addition and sits at secondary scale. It must never compete with the eye pods.

## Secondary Forms (new)
- **Whisker-grille beard**: the Helper Bot's moustache grown into a beard. It hangs from the underside of the eye pods and the bridge, in front of the neck and the top of the chest:
  - top: a moustache-width bar (about 8 px across) tucked right under the lens rims;
  - below: a spade tapering 6 → 4 → 2 px to a point around **y14.5**, **above the chest lamp** (the lamp sits at x2.5, y≈13.5, so the narrow lower beard must stay inside x −2…2);
  - vertical dark slots make it read as a voice grille (`brassDark` slots between `brass` bars, as Helper Bot's grille);
  - depth about 1 px, sitting in front of the body face (z ≤ −4.25) so it never intersects the chassis.
- **Monocle** (smaller, user 2026-11-04): a small round brass ring (0.5 px bars, 4 px across) framing his **right iris**, in front of the rim and lid plate. A **short brass chain** (two 0.5 px links and a tiny fob) hangs from its bottom, just outside the beard's edge and inside the right arm.
- **Ledger**: a small book (about 3 × 4 × 1 px) strapped to his back, **on his left (−x) beside the key**: walnut cover, brass corner caps and clasp, one tiny emerald pixel inlay (unlit). A dark-steel strap holds it on. It must clear the station's post, post cog and winder (the winder arm swings in from his right, +x; the post cog sits on the post's −x side at x −2.25…−1.5).
- **Stock Link aerial**: the copper rod stays. The brass bead becomes a small **glass bulb in a brass cup**, after Create's Stock Link bulb: a dark glass bulb when unlit, and a separate lit cube for the blink. The tip stays at y24.

## Interesting Features
- The **beard follows the face**: it hangs from his head, so every nod, tilt and double-take carries it, and it swings a beat behind like a loose part (derived motion, no new clips).
- The **monocle chain** is his second loose part after the aerial.
- The **bulb blinks** when he reads his network (game-side light state, like his trade lamp; no clip).
- Everything else is Trade Bot's: the drawer, the key, the waist band, the catchlights, the emerald lamp.

## Personality
Trade Bot's personality, a few years on: *still eager, still nosy, now a little proud of himself*. The motion is identical; the beard, monocle and ledger make the same gestures read as a seasoned merchant's. No new behaviour.

## Face and Expressions
**Identical to Trade Bot** (same parts, same table). The beard, monocle and chain only follow the head and right eye, so every expression reads as before. Check in review that sad (Λ pods), cross (lid frown) and dizzy still read clearly with the beard below and the monocle on the right lens.

## Motion Set
**Identical to Trade Bot: every clip, every timing, every sound keyframe.** Locomotion, idle, mannerisms, the five fear stages, reactions, winding down and up, stairs, hops, item handling, trading, docking and the expression clips are all reused unchanged ([Trade Bot Motion Set](trade-bot.md#motion-set)).

The only motion added is **derived secondary motion on the new parts**, computed from the existing bones; nothing is keyframed by hand:
- **Beard**: a loose spring on the head; it lags the head by about +0.12 s and settles a little before the aerial. It hangs (gravity), so when his body leans in a roll or the head pitches, it counter-swings slightly toward vertical. When the neck sinks (doze, docking, hide), it squashes a little in Y as it meets his chest (scale toward 0.85).
- **Monocle chain**: a loose spring on the right eye; lags about +0.16 s; swings with tilts and settles last with the aerial.
- **Monocle pop** (the cartoon gag, user 2026-11-04): whenever he's surprised (his lid plates snap wide open: the startle, the double-take when he sees you, the wall bump, the jolt awake, the scared spot), the monocle **pops out of his eye**: it shoots out and up with a stretch, twirls a full turn in the air with its chain flailing, then drops back into the eye with a squash and a wobble (about 0.85 s). Triggered from each clip's own lid motion, so it fires wherever he's surprised; at most once per 1.2 s, never in the last 0.9 s of a clip (so clips still end at rest and chain).
- **Ledger**: rigid, carried by the body.
- **Bulb glow**: off in clips by default, so the clips stay identical; the game blinks it.

**Sounds:** the same Trade Bot sound set and cues (`trade-bot.cues.json`), with the same sound keyframes on every clip's Effects track.

## Rig Requirements
**Trade Bot's rig exactly** ([table](trade-bot.md#rig-requirements)): the same bone names, parents and pivots, so every clip plays unchanged. New bones:

| Bone | Parent | Pivot | Moves |
|---|---|---|---|
| `beard` | `head` | 0, 17, −4.75 (root under the bridge) | derived: lag swing (X/Z), squash (scale Y) |
| `monocle` | `eye_right` | on the right lens centre, front | rigid |
| `monocle_chain` | `monocle` | the ring's lower inner edge | derived: loose lag swing |
| `ledger` | `body` | on his back, left of the key | rigid |
| `aerial_bulb` → `aerial_bulb_glow` | `aerial` (replaces `aerial_bead`) | aerial tip | lit cube shown or hidden |

The station keeps Trade Bot's station rig.

## Trade Master Station
The Trade Bot Station's geometry and rig, with its **plinth frame in netherite trim** to match him, its brass parts kept and its glow unchanged. Nothing else changes (user approved, 2026-11-04).

## Fun Score (1-10)
**9**: everything that makes Trade Bot fun is kept, and the beard, monocle and ledger are a joke players get at a glance: the little salesman made it big. A beard that bobs a beat behind every double-take adds charm to clips they already love.

## Builder Notes
- **Build as a variant of `trade-bot.gen.js`, never a fork of its clips.** Write a `trade-master.gen.js` that reuses Trade Bot's geometry, rig, clips and sound cues, then applies the palette swap and adds the new parts and bones. Any later fix to a Trade Bot clip then reaches him for free. If the generator needs a small hook to allow this (export its builders, or take a variant option), add it without changing Trade Bot's output, and confirm by diffing the regenerated `trade-bot.bbmodel`.
- **Clips must stay identical.** Diff the bone keyframes of every clip against Trade Bot's: zero difference on shared bones.
- **Self-collision is the gate.** Extend the existing frame-by-frame check (`FK=1`) with the beard, monocle, chain and ledger boxes. Check the arms in every clip (above all `scared_hide` and `scared_cower` with the claws over the eyes, the waves, the folded arms in `dock_lights_out`, the lens polish) and the ledger against the station post, cog and winder in `dock_lights_out` and `action_comes_in`. **If a clip collides, reshape the new part (thinner, shorter, moved) and never the clip.** If it can't be solved that way, stop and ask the user.
- **Beard limits:** above the chest lamp, inside x −2…2 below y15.5, in front of the body face, under the lens rims without covering the lenses.
- **Height:** 24 px including the bulb.
- **Netherite swap scope:** body frame edges and lid plates only. Eye pods, neck and upper arms stay dark steel, so the face keeps Trade Bot's read.
- **Bulb colour (user approved, 2026-11-04): Create's Stock Link red, muted.** Create's bulb is lit `#ae0000`/`#970000` and unlit `#3b0000`/`#500101`; ours is a muted take on that (no near-white, small). It only **blinks briefly when he reads the network**, so players recognise the Stock Link without it reading as this mod's steady "fault" red.
- **Item:** like Trade Bot's (him on his station, 3D item renderer).
- **As built (2026-11-04):** `trade-master.gen.js` runs `trade-bot.gen.js --variant=trade-master.variant.js`. Trade Bot's own output is byte-identical apart from random ids, and all 46 clips match his on every shared track, sound keyframes included. Beard: odd-width rows 7-5-3-1 so the grille slots line up and mirror. It hangs plumb against tilt and never swings back into his chest. When his chin tucks or his head turns it concertinas up (squash to 0.3) before flaring forward (capped at 20° past plumb). Chain: hangs just outside the beard (x 4). Monocle pop: fires in 14 clips. Remaining contact, for the user's call: in three over-the-shoulder glances (head turned 47–55°: `belt_place`, `turn_around`, `dock_lights_out`), part of the beard dips up to 0.75 px into the top corner of his chest beyond what his own eye pods do, which already reach 1.24 px in Trade Bot's approved clips. Elsewhere it stays at 0.4 px or less. The arms graze the chain or monocle by 0.18 px at most in five clips.
- **Screenshots for review:** front, side and three-quarter at the same angles as Trade Bot's; the face sheet (`expression_sheet`); a frame of `scared_hide`, `dock_lights_out` (docked) and a wave; Trade Bot and Trade Master side by side.
