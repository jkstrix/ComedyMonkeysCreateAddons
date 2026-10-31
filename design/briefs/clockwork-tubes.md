# Design Brief: Clockwork Tubes

Four new blocks. Gameplay: [design/specs/clockwork-tubes.md](../specs/clockwork-tubes.md). Family: the bot-built machines (Trade Bot, Helper Bot, Clockwork Dynamo: [visual language](../visual-language.md), [Dynamo brief](clockwork-dynamo.md), [Trade Bot brief](trade-bot.md)). Neighbours by function: Create's Chute, Funnels and cogwheels, and the mod's Fluid Lines, which they must **not** look like.

## Hard Requirements

From the user:
- "I want to make three new blocks. These allow the user to suck items from one end of a chute to another." The Receiver (red), the Inlets (blue) and the tubes that join them.
- "The red block is powered by SU"; "the reciever should be powered by cog, the receives cog should be hollow to allow itms to pass through."
- "the blue blocks are connectors that can connect to blocks that contain items, be placed above depots or belts, or above the floor."
- "They can branch off in all directions."
- "There should be brass versions of the opening block with filters."
- "Inlets should be able to face any direction."
- "This should follow the clockwork theme of the trade bot and the dynamo."

From the spec:
- **Receiver** (D3, D3a, D11, D12): a **small-cogwheel-sized hollow cog** that meshes with Create's cogwheels (small cogs side by side, large on the diagonal). The cog's axis lies along the items' path; the hole is about **6 px**. The tube joins on one face; items leave through the cog on the **opposite face**. Six facings.
- **Inlet** (D4, D21, D22, D24, D27a): **six facings**, mouth at the front, tube at the back only. Two versions: plain (andesite, no filter) and **Brass** (Create filter slot). **Gulp**: the mouth flap twitches inward each time it takes something.
- **Tube** (D5, D14, D29, D31): branches in every direction; **glass windows** with items visibly sliding past; a wrench closes a joint; a refused join stays **capped**; waterloggable; no hiding.
- Bot-built family; **no copper** (copper means electricity, and nothing here is electric); no face on any block (D8, visual language).
- Dark, muted, Create-like; accents as trims; **previewed in Blockbench first** (standing rules).

## Asset Type

Four blocks, 16 × 16 × 16 px, `java_block`:
1. **Clockwork Receiver**: static body plus a **cog part** the renderer spins. Six facings.
2. **Clockwork Inlet**: static body plus a **flap part** (the gulp). Six facings.
3. **Brass Clockwork Inlet**: the same geometry and flap, a brass trim set and filter panels.
4. **Clockwork Tube**: multipart, like Create's Fluid Pipe: a straight model per axis, a hub, six arms and an end cap.

Items in the tubes and through the cog are drawn by the renderer; they aren't part of any model. New textures `clockwork_receiver_*`, `clockwork_inlet_*`, `brass_clockwork_inlet_*`, `clockwork_tube_*`, cell-relative UVs.

## Fantasy

The bots' own vacuum post: little glass tubes that the clockwork automatons ran through their workshop, with greedy hoods that gulp up whatever is left lying around, all drawn by one turning ring gear that pulls the whole system through its middle.

## Hero Element

**The hollow ring gear at the Receiver's waist.** It's a Create-profile cog with the middle cut out, turning between two housings, and items pop out of the Receiver right through it. It's the waist band of every bot, made into a working part. The second read is the **Inlet's gulp**: the hood's lid-plate flap snapping inward each time it takes something, the same steel lid plate that Trade Bot blinks with.

## Theme

The bots' materials, **andesite tier**: andesite panels in a dark-steel frame, stepped corners, rivet lines. **No brass on the plain blocks** (the house rule: each tier keeps its own trim metal; the andesite blackstone generator was caught wearing brass). Brass belongs to the Brass Inlet alone, where it marks the tier the way it does on Create's Brass Funnel.

| Material | Where | Colours |
|---|---|---|
| Andesite (the bots' panels) | housings, hood, collars, tube hubs, the ring gear | `#6b6e6a` `#73766f` `#7c7f79` `#8a8c86` (ceiling) |
| Dark blue-grey steel | frames, stepped corners, tube rails, sockets, lips, flap, bearing lips | `#2f343b` `#3c424a` `#4a5059` (the bots' three-shade mottle on frames) |
| Bot brass (Brass Inlet only) | waist band and rivets, mouth lip, filter-panel rims | `#6e5a2a` `#8f7638` `#b08f45` (`#c9a75a` highlights only) |
| Glass | tube windows, Receiver throat windows | clear pixels (cutout), a 1 px glint streak per pane, thin dark edge lines; pale glint `#cfe0e2`, edge `#9fb6b9` |
| Dark interior | mouths, sockets, throat | `#1d2024` → `#26292e`, fading into shadow like a chest's inside |

No stripes, no saturated surfaces, no glow anywhere. Rivets are dots a shade apart.

**Not Create's Chute or Funnel**: no iron-grey chute shell, no fabric flaps, no funnel brass collar on the plain blocks. **Not the Fluid Lines**: those are thin round 4 px pipes; Clockwork Tubes are chunkier, rounded-square glass tubes with collars.

## Shape Language

**Rounded squares** (stepped 1 px corners), the bots' body language, everywhere. The only circles are the ring gear and its hole. Tubes are octagonal-section rounded squares, never round pipes ("round is for motors and engines").

## Silhouette Description

- **Receiver**: a squat rounded box pinched at the waist by a cog, like a bobbin with a gear around its middle. A tube goes in one end, and a dark mouth opens at the other. From 5 blocks you see "box, gear ring, box", and the teeth stick out past the block like any Create cogwheel's.
- **Inlet**: a stepped hood or bell: narrow at the back where the tube plugs in, flaring to a wide square mouth at the front. It reads like a vacuum-cleaner nozzle. Brass version: the same hood with a brass band and lip.
- **Tube**: a glass rounded-square tube with steel corner rails and andesite collars at every block joint. At junctions, a small andesite hub box with arms out to each side.

## Primary Forms

Coordinates are in the model's own frame. **Receiver**: input at y = 0, output at y = 16, cog in the mid-plane (axis Y). **Inlet**: mouth at y = 0, tube at y = 16. Blockstate rotations give the six facings.

**Receiver**
- **Lower housing ("throat", input side)**: x/z 1–15, y 0–6.5. A rounded square (1 px stepped vertical edges and bottom edges), andesite panels inset 0.25 px in a 1 px dark-steel frame.
- **Ring gear (the cog part)**: y 6.5–9.5, **Create's small-cog profile exactly**: a 12 px disc (2–14, stepped to an octagon) and **eight teeth**, 3 × 3 px bars every 45°, reaching **−1 to 17** (1 px past the block, as Create's own). **Hollow**: a 6 px hole (5–11) with a 0.5 px dark-steel bearing lip round it. Andesite, like Create's andesite parts, with a slightly darker tooth face so the teeth read.
- **Upper housing ("crown", output side)**: x/z 1–15, y 9.5–16, the same rounded square and frame as the lower.
- **Sleeve**: a stationary dark-steel sleeve 6 × 6 through the cog's hole, joining the housings (the cog turns round it; the items pass inside it). Hidden in play; it keeps the inside from showing sky.

**Inlet**
- **Socket (back)**: y 15–16, a dark-steel collar 10 × 10 with an 8 × 8 opening: the same collar a tube ends in, so the tube visibly plugs in.
- **Neck**: y 11–15, a 10 × 10 rounded square, andesite in a steel frame.
- **Shoulder**: y 7–11, 12 × 12.
- **Hood mouth**: y 0–7, 14 × 14 (1–15), stepped. The mouth face (y = 0) has a 1 px dark-steel lip round a **10 × 10 opening** that fades into a dark interior.

**Tube**
- **Straight** (two opposite joints, nothing else): the core is 8 × 8 (4–12) with 1 px stepped corners. The **four chamfered corners are dark-steel rails**, and each face between them is a **glass pane** 6 px wide running the whole length between the collars. **Half-collars** at both ends: andesite, 10 × 10, 1 px long (y 0–1 and 15–16), so two blocks meeting make one 2 px collar at the joint.
- **Hub** (any other shape: elbows, T-joins, crosses, a lone tube): an andesite rounded-square box 10 × 10 × 10 (3–13) in a dark-steel frame, solid (no glass).
- **Arm** (one per joint, to the hub): the straight's section, 3 px long (13–16), with its glass and rails, ending in a half-collar at the block face.
- **End cap** (a closed or refused joint where a tube would otherwise point; a lone tube's ends): a dark-steel plate 9 × 9, 0.75 px, with a single centre bolt.

## Secondary Forms

- **Receiver throat windows**: one glass window on each side of the **lower housing** (4 total, one per face), 4 px wide (6–10) × 4 px tall (y 1.5–5.5), in a steel frame. Items are seen rising through the throat towards the cog, then they pop out of the mouth. Only one window per face, so no face can form.
- **Receiver mouth (output face, y = 16)**: a 1 px dark-steel lip round an 8 × 8 opening, with one rivet in each corner of the crown's top panel.
- **Receiver socket (input face, y = 0)**: the tube collar (as the Inlet's), 8 × 8 opening.
- **Rivet lines**: a short line of steel rivets along the bottom edge of each crown side (one every 2 px), the bots' rivet language, a shade apart.
- **Inlet waist band**: y 7–8, a 0.75 px proud band round the shoulder. **Plain**: andesite band with steel rivets. **Brass**: bot-brass band with brass rivets, plus a brass mouth lip. That's the tier read, like Andesite and Brass Funnels.
- **Inlet flap**: a 9 × 9 dark-steel lid plate across the mouth, hung from a hinge bar at the mouth's north edge (inside the lip at y 0.5–1). A small rivet line along the hinge. Shaped and shaded like **Trade Bot's lid plates**, so the gulp reads as his blink.
- **Brass Inlet filter panels**: on each of the hood's four sides, a flat 8 × 8 recessed panel (y 0.5–6.5, centred) with a thin brass rim, where Create's filter box and the filter item show (a sided value box: whichever side you look at). Plain Inlet: those panels are plain andesite, no rim.

## Interesting Features

- **Items go through the gear**: you watch them come up the glass, rise through the throat windows and pop out of the middle of a turning cog.
- **The gulp**: every Inlet blinks inward as it takes something, each on its own turn, so a busy network ripples with gulps (the round robin made visible).
- **Glass everywhere items travel**: a jam is visible at a glance, because the stuck items sit in the windows.
- **Bot parts**: stepped corners, andesite in steel frames, rivet lines and Trade Bot's lid plate. With no faces, it reads as the bots' plumbing, not as creatures.

## Motion (blocks with moving parts)

**Motion feel**: the network is steady clockwork: the cog turns evenly, items glide at constant speed. The one springy thing is the flap, a light lid on a stiff hinge that snaps and settles, never a linear tween.

| Part | Trigger | Motion | Time |
|---|---|---|---|
| **Ring gear** | Network turning | Rotates with the Receiver's speed and direction, **offset to mesh tooth-in-gap with neighbouring cogs** (Create's cog offset for its position) | continuous |
| **Inlet flap (gulp)** | Inlet takes a load | Anticipation: puffs **out** 5°; **snaps in** 50° (into the hood); swings back past closed by −8°; settles. Brass and plain the same | 0.3 s (0.05 / 0.08 / 0.1 / 0.07) |
| **Items in tubes** (renderer) | A load travels | Glides at network speed, **tumbling slowly** about its travel axis (a quarter turn a block). It rounds corners on a curve through hubs (hidden briefly inside the hub, then out into the next arm) | network speed |
| **Item through the cog** (renderer) | Load reaches the Receiver | Rises up the throat (visible in its windows), through the hole as the cog turns round it, and out of the mouth | network speed |
| **Item out of the mouth** (renderer) | No block in front | Pops out with a small hop away from the face, like Create's Chute | (item entity) |
| **Item into a block** | A chest or belt in front | Slides into the mouth and vanishes | — |

Nothing else moves: no idle sway, no lamp.

## Rig Requirements

- **receiver_cog**: one part, pivot at the block centre (8, 8, 8), rotating about the model's Y (the item axis). It holds the disc, the eight teeth and the bearing lip. The sleeve is static (in the body).
- **inlet_flap**: one part, pivot on the hinge line at the mouth's north edge, (8, 0.75, 2.5), rotating about X. The same part for both Inlets.
- Tube parts are static (multipart, joints from blockstate).
- Renderer-drawn: items (tubes, throat, mouth).

## Approved choices

Brief and all five choices below approved by the user, 2026-10-31 ("lets do it").

**Revised in the first build** (user, 2026-10-31: "There's no elements from the trader bot on there, shared colours or textures"); preview approved by the user, 2026-10-31 ("looks much better"). The model and generator (`design/models/clockwork-tubes.gen.js`) are the reference where they differ from the sections below:
1. **Trade Bot's own palette and parts** replace choice 1: his andesite mottle and steel; his **brass waist band** (brass, dark brass every other pixel) on both Inlets; the ring gear in **brass like his station's post cog**; the Receiver's mouth in a **brass rim like his eye rims**; **dark-brass bolts** (crown rivet line, corner bolts, one bolt in each tube hub panel); the flap is his **lid plate** (plain steel, lighter leading edge, no rivets). The plain blocks now carry brass, against the tier rule, at the user's request.
2. Brass Inlet vs plain: brass lip, brass socket ring and brass-rimmed filter panels (the band is brass on both).
3. Receiver housings are **12 px** (2–14), not 14, so the ring gear's disc and teeth stand clear of the body.
4. Inlet: a **proud lip ring** at the mouth (0.5–15.5, y 0–1) and the band proud (y 7–8), so the hood reads as a bell between two rings. Hollow to a ceiling at y 10.5 so the flap's 50° gulp clears; flap pivot (8, 1.25, 3.5).
5. Glass glints on the outward face of each pane only (two texels every 12); inward faces show just the lit edge.
6. End cap: a 6 × 6 steel disc with a bolt between the rails (a 9 × 9 plate would z-fight the rails).
7. Receiver mouth: an 8 px brass-rimmed opening over the 6 px bore.
11. **Brass Inlet, more brass** (user, 2026-10-31: "The brass inlet isn't brass enough and rectangle needs to match the filter size"; picked "Brass hood" and "4x4 square"): the hood's four sides are Trade Bot brass between the steel corners (its ledge too); each side has a **4 x 4 filter slot**, a dark steel recess the size of Create's filter box, in a 1 px dark-brass rim, centred 4 px up from the mouth (the filter box moves there). Shoulder and neck stay andesite. Preview: `design/models/clockwork-tubes-review/brass-inlet-v2-*.png`; approved by the user, 2026-10-31 ("brass inlet looks good").
9. **Hubs are open where a tube joins** (user, 2026-10-31: "The corner joints are blocked"): the hub keeps a framed panel with its bolt only on sides no tube joins; joined sides are open, its inside lined dark, and the arms run from the hub's edge (12 / 4) to the face, so a tube runs into the hub instead of ending at a wall.
10. **Texture review taken in full** (user, 2026-10-31; `design/texture-reviews/clockwork-tubes.md`): one brass (the Trade Bot's), glass in the Fluid Lines' blue-grey, the Dynamo's band repainted to match. **Sound**: Create's cog sound only (spec D37).
8. The preview diagram branches **out of its plane too** (user, 2026-10-31: "remember it can branch out not just on the same plane"): a fourth Inlet on its own barrel behind, its tube running back along z into the centre hub (five arms), with its own turn in `flow`.

1. **The ring gear is andesite, not brass.** Brass reads "clockwork", but the Receiver is andesite age, and the house rule keeps brass off andesite-tier blocks. Brass stays the Brass Inlet's mark.
2. **No status lamp.** The spec has none; moving items in the glass show the network working, and a jam shows as items sitting still. A lamp on a side would also make eye-pairs in some facings.
3. **The flap is Trade Bot's lid plate**, so the gulp is his blink: the family tie on the plain blocks.
4. **Brass Inlet filter on all four hood sides**, Create's "sided" filter box: whichever side you look at, like Create's own sided slots.
5. **The cog's teeth reach 1 px outside the block**, exactly like Create's cogwheel, so it meshes and reads as one. Everything else stays inside the 16 px cube.

## Fun Score (1-10)

**8.** Items gliding up glass and popping out of the middle of a turning gear is a small show every time, and a row of hoods gulping in turn gives a dull job some character, all without putting a face on anything.

## Builder Notes

- **Proportions**: Receiver housings 14 × 14 × 6.5 px with the cog in the 3 px gap; Inlet hood 14 → 12 → 10 px stepping back to an 8 px tube socket; tube core 8 × 8 with 10 × 10 collars. The Inlet socket, the Receiver socket and the tube's half-collar are **the same collar**, so every joint matches.
- **Must not lose**: the cog profile matches Create's small cogwheel (12 px disc, eight 3 × 3 teeth at 45°, −1 to 17, 3 px thick at y 6.5–9.5) so it meshes visually; the 6 px hole; **one** window per Receiver face; no brass on the plain blocks; no copper anywhere; cell-relative UVs; overlays ≥ 0.25 px apart (z-fight check).
- **Glass**: tube panes and throat windows are **cutout** glass (clear pixels with opaque glints and dark edge lines), so items drawn by the renderer show through. No translucent glass (memory: translucent glass as a part hides renderer-drawn contents).
- **Parts to export**: `block/clockwork_receiver` (body), `block/clockwork_receiver_cog` (part); `block/clockwork_inlet`, `block/brass_clockwork_inlet`, `block/clockwork_inlet_flap` (part); `block/clockwork_tube_straight`, `block/clockwork_tube_hub`, `block/clockwork_tube_arm`, `block/clockwork_tube_cap`.
- **Face check, all six facings**: the Receiver's throat window plus the crown's corner rivets; the Inlet's mouth plus anything on the hood sides; the tube hub. Two similar features side by side read as eyes (the Dynamo hit this three times).
- **Preview scene** (Blockbench): the user's diagram built in blocks. Three Inlets (on a chest, over a depot, over a floor one block up), tubes rising and branching left and right into one, the Receiver above outputting into a chest, and a Create small cogwheel meshed beside its ring gear (stand-in, not exported). Plus: an Inlet in all six facings; plain beside Brass; a tube showing straight, elbow, T, cross and a capped end. Item stand-ins only to check clearance in glass and through the hole; never exported.
- **Preview clips**: `cog_turn` (loop), `gulp`, `items_flow` (stand-ins gliding up a branch, through the hub, through the cog), `pop` (out of the mouth).
- **Screenshots for review**: the diagram scene (three-quarter, front); the Receiver close up (cog meshing with the stand-in, items in the throat windows); Inlet plain and brass side by side, front and three-quarter; the Inlet's six facings; the tube shapes; the Receiver on its side and upside down (no face?); beside the Clockwork Dynamo and Trade Bot Station for the family read.
- **Item icons**: 3D block models for Receiver and Inlets; the tube item as a horizontal straight tube.
- **Sound**: only Create's standard cog sound from the Receiver (spec D37, user 2026-10-31); everything else is silent, the gulp included.
