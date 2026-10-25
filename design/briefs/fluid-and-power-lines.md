# Design Brief: Fluid Lines and Power Lines

A family of new blocks and items. Gameplay is in `design/specs/fluid-and-power-lines.md` (D1–D29); the rough mock-up is `design/specs/fluid-and-power-lines.page.html`. This brief sets the look; where they differ, the spec wins on behaviour and this brief on appearance.

## Hard Requirements

From the user:
- Replace Pipez, which is "ugly by default", and the covers, which feel "like a work around". The new lines must **look good with nothing added**.
- "Look better", "more pleasing". **No item pipe**: "just a fluid pipe and wires".
- "Hang the wires from point to point."
- "Some way of hiding them when they are placed": **encase them in any block**, with goggles showing where they run (D6).
- Tiers: "Just copper and brass", then "**Wires and pipes base tier should be copper**": the tiers are **Copper and Brass**.
- "The wires and pipes want to be **relatively thin**, **wires should look like wires and not look like pipes, even the placed kind**, **pipes shouldn't feel ugly and blocky**."
- "Wires should be able to be **placed along walls and floors and look like they 'attach'**."
- "**Fluid intake should have a filter on it.**"
- "**Coloured lines**". Lines of different colours, undyed included, "**only join if wrenched**".
- "**Posts should be a collection of blocks so they can be different heights, and also have multiple 'branches' on top.**" They support **1 to 4 branches**.
- "**Poles should be wood and not have tiers.**" "**The horizontal lines on the pole need to go too.**"
- "**Coloured power lines should feel a little thicker, like they are a covered cable.**"
- "Power lines and fluid pipes should **just be their colour, no glowing**. **Undyed power lines and fluid pipes match their material**."
- Standing house rules: a dark, muted palette, mid-grey at most; accents used sparingly; everything **previewed in Blockbench before it goes into the mod**; moving parts drawn as Flywheel instances, like the other machines.

From the spec:
- **Fluid Lines:** two tiers, which never join each other. One fluid per line at a time. Lines store nothing, and the window shows the transfer as it happens (D3, D15, D16).
- **Power Lines:** face-attached, one wire per block face, joining round inside and outside corners (Technical Notes).
- **Fittings:**
  - one per block, on straight pieces only (D23);
  - the Intake goes on the side facing a tank;
  - the Breaker is for power only; the Intake, Valve and Filter are for fluid only; the Meter and Splitter work on both.
- **Line Posts:** wooden, with no tier; they go on any face. They stack into a pole of any height, and the top block (the head) takes up to 4 branches (D30, P4, P12).
- **Spans:** reach 16 or 32 blocks, sag gently and have no collision (D11).
- **Encased lines:** look exactly like the block they're encased in (copycat style). Goggles show them as an outline (D6, P9).

## Asset Type

Blocks and items, `java_block`, 16 px to a block. Everything is thin, so most of each block is empty space.
- **Fluid Line:** a multipart block with a centre piece plus an arm for each connected side.
- **Power Line:** a multiface block; each face carries its own wire, and the wires join at corners.
- **Line Post:** a stackable block that faces the surface it's placed on. It has three looks by position (foot, pole, head) and branch states on the head (N, E, S, W).
- **Fittings:** items. Their models are drawn on the line they're fitted to.

The flow window, the Meter needle, the Valve wheel, the Breaker lever, the Splitter dial and the spans are drawn by the renderer as Flywheel instances. Everything else is baked block model.

## Fantasy

The tidy services of a well-run Create workshop: slim pipes on regular couplings with the fluid moving along a sight window, and real cable clipped flat along the walls or strung between ceramic insulators. You can follow every run with your eye, read it with a gauge and shut it with a wheel. When you've finished, it disappears into the walls.

## Hero Element

There are two, one for each kind of line. Both show what the line is doing at that moment:
- **Fluid Lines: the sight window.** A 1 px slot runs along each pipe, and the real fluid sprite slides through it at the line's actual speed: slow on copper, quick on brass. It goes dark when nothing is flowing. The Meter's needle is its partner, swinging with the same flow.
- **Power Lines: the span.** A thin cable sags between ceramic insulators on slim poles. It's the most picturesque thing in the set and the reason to build outdoors.

Nothing else may compete with these two. The fittings are small, round and quiet, and they read as parts of the line rather than as boxes stuck on it.

## Theme

Create's material language, from the dark end of each range:

| Material | Use | Colours |
|---|---|---|
| Copper | undyed copper pipes and wires, insulator caps on copper cable; couplings and clips a shade darker (`#7a3e2a`) | **Create's own copper** (sampled from its fluid tank): `#904931` `#9a5038` `#a75a40` `#b26247` `#c26b4c` `#c87456`, clean with no mottle. Our earlier darker copper was called "muddy" |
| Mid brass | undyed brass pipes and wires, insulator caps on brass cable, bezels; couplings and clips a shade darker (`#6b5828`) | the generators' mid brass `#7e6832` `#8f7638` `#a3873f`, no bright highlights (`#b08f45` read as "too bright" on the Aeronautics set) |
| Dark steel | fitting bodies, the Intake collar, the Breaker, splice sleeves | `#2f343b` `#3c424a` `#4a5059` |
| Ceramic | post insulators | bone grey `#7d786f` `#8f897e` `#9c968b` (never white) |
| Accents | Valve wheel, Meter needle, Breaker knob | dark red `#7a2a22`; the gauge face is `#8a8c86` with dark ticks |
| Goggles outline | hidden lines | cool cyan `#6fb7c4`, about 50% alpha |

**Brass pipes are a thin element**, so a whole brass pipe is fine here, as Create's brass tunnels are. They're still dark brass, never yellow. The light highlight colour appears on the top texel only.

**Dyes.** Take the 16 vanilla dye colours, desaturate them by about 40% and bring them down to mid value, so the brightest (white, yellow, lime) land at mid grey or below. White dye gives `#8a8c86`. **A line is just its colour**: a dyed pipe or wire (and its spans) is the dye colour all over, and an undyed one is its tier material all over. Only the **couplings and clips** stay a darker shade of the tier material, so you can always tell copper from brass. **Nothing on a line glows or is emissive**: no light, no pulse, no lit fluid.

It sits beside Create's own Fluid Pipes (8 px, copper) and should never be mistaken for them. Even the copper tier can't be mistaken for them: ours are half as thick, ringed and slotted. Next to the mod's Lava Generator and Electric Motor, a wire ends on a small terminal lug against the machine's face; those models don't change.

## Shape Language

**Circles.** Every part reads round: pipes, rings, hubs, the wheel, the gauge and the insulators. The roundness is built in voxels from plus-shaped cross sections, two crossed boxes, so a 4 px pipe reads as an octagon. No square collars, no boxes on the line and no hard corners anywhere except the encasing block. Wires take this further: they are only strands, with no box at all.

## Silhouette Description

- **A fluid run** is a thin, round line with a steady beat: one coupling ring per block, so you count blocks along it like beads. At a bend it turns through a round knuckle. Where it meets a tank, a flange sits flat against the tank's face. The slot along the top glints with the fluid. From 5 blocks away it reads as "a slim pipe", never "a row of blocks".
- **A wire** is a hairline that hugs its surface, dotted with a clip at every block. It folds into the corner where floor meets wall, climbs the wall, and folds over outside edges. It never floats in the middle of the block.
- **A post and span:** a slim upright with a stacked ceramic head (disc, neck, disc), and a cable curving down from it and up to the next one. It reads like a telegraph line in miniature.
- **In the inventory:** the pipe is a short diagonal length with two rings; the wire is a small coil of cable; the post stands upright.

## Primary Forms

**Fluid Line pipe**, in pixels:
- The body is 4 px across, centred on the block. Its cross-section is a 4×2 box crossed with a 2×4 box, filled to 4×4 with the corner texels darker, so it reads round.
- Each connected side gets an arm from the centre to the block edge.
- A straight piece through the block is one 16 px length with no centre lump.

**Power Line wire** (attached to a face):
- *Copper:* one round strand, 1 px wide and 1 px tall ("wires need to be thinner across the board"), lying directly on the face. Its underside is left out (it sits on the face), so nothing can z-fight.
- *Brass:* twin flex, two strands side by side, 1.5 px wide and 0.75 px tall, with a texture twist every 4 px.
- *Dyed (either tier):* a **covered cable**. A round jacket in the dye colour goes over the strand: 1.75 px across for copper, and a 2.25 × 1.75 px flattened oval over the brass twin. It has a soft highlight texel and no visible strands. Undyed wire is the bare strand in its material; dyeing is what covers it.
- From the centre of the face, the wire runs out to each connected edge, the way redstone dust does.

**Line Post** (shown on a floor; a stack points out of any face). One wooden block, no tiers, stacked into poles:
- **Foot** (the bottom block): an iron bracket plate 6 px across and 1.5 px tall, under the pole.
- **Pole** (every block): 3 × 3 px of walnut a step lighter than the house wood (`#4e3626` `#634532` `#6f4d36`), with vertical grain. **Plain the whole way up**: no bands or lines at the block joints.
- **Head** (the top block): the pole ends 10.5 px up under a ceramic insulator stack (5 px disc, 3 px neck, 5 px disc, 1.5 px each) with a 2 px terminal cap. The cap is in the **tier of the cable tied to it** (copper or brass), so the tier shows where the cable meets the pole.
- **Branches** (on the head, 1 to 4, one per side): a 2 × 2 px wooden crossarm from 8 to 10 px up, out to 7 px from the centre, end grain showing at its tip, with a 1 px iron brace under it at 45°. A smaller insulator (4 px and 2.4 px rings) stands on its tip with its own tier-coloured cap.
- A single post is foot, pole and head in one block.

**Span:** the wire's own strand, twin or covered cable, but **thinner than placed wire** (about 60% of it, at least 0.5 px: 0.6 px bare copper, 0.9 × 0.5 px brass twin, about 1 px covered), drawn as segmented quads along a catenary. It sags 1/24 of its length, at least ½ block. It's the wire's colour: its material, or its dye.

## Secondary Forms

**Pipes:**
- **Coupling ring:** 6 px across (plus-shaped) and 1 px long, at the block boundary on each connected side, so there's one ring between every two pieces. It's always a darker shade of the tier material, dyed or not.
- **Knuckle hub:** a 6 px rounded hub (three crossed boxes) at the centre whenever the piece isn't straight: bends, T-pieces, crosses and ends.
- **Flange:** an 8 px plus-shaped plate, 1 px thick, flat against a tank or machine at a connected end.
- **End cap:** a 5 px rounded cap on a dead end.
- **Sight slot:** a 1 px wide slot along the top of the pipe (or the outward side on vertical runs), between the rings and 12 px long per block. It's dark glass `#1f2226` when idle, and the fluid strip slides inside it when flowing.

**Wires:**
- **Clip:** a strap across the wire at the face centre, the wire's width plus 1 px, 1 px along and 0.5 px proud, in the tier material (copper grey or brass). The clip is the main way to read the tier at a glance.
- **Inside corner:** where floor meets wall, the wire bends up the wall face with a 1 px radius step. **Outside corner:** it folds over the edge the same way. Every corner shape is built from straight elements; Java elements can only turn in 22.5° steps.
- **Junction (T or cross):** a 3 px round splice dome, 1 px tall, in the clip material.
- **Terminal lug:** where a wire meets a block that takes or gives FE, a 2 px ring 1 px proud on that block's face, with the wire ending in it.
- **Post foot:** a wire that reaches a post runs under its foot plate.
- **Span tie:** at the insulator, the span ends in a small wrap, a 1 px loop round the top neck.

**Wrenched join between two colours** (D29; this shows that the join was made by hand):
- **Wires:** a 2 px dark-steel splice sleeve at the block boundary.
- **Pipes:** the coupling ring is split down the middle, half in one colour and half in the other.

## Interesting Features

**The fittings.** All of them are round, sit centred on a straight piece, and stay within 8 px across.
- **Intake** (fluid): the part that replaces a pump, in dark steel on a brass flange so it stands out on either tier.
  - An 8 px brass flange flat against the tank, then a 6 px dark-steel collar 4 px long.
  - The filter shows as a 2 × 3 px mesh window on the collar's side. It's tinted with the filter fluid's colour (static), or dark mesh when the filter is empty.
  - The Intake replaces the end flange on that side.
- **Valve** (fluid):
  - A 6 px round steel body, 5 px long.
  - A 1 px stem 3 px tall, with a 7 px dark-red hand wheel on it, 1 px thick with 3 spokes.
  - The wheel turns through ¾ of a turn from fully open to shut, in 10% clicks. A redstone shut spins it closed over 0.4 s.
- **Breaker** (power): a dark-steel switch body sitting on the wire, clipped to the face.
  - The body is 5 px round and 3 px tall.
  - The lever is 1 px wide and 4 px long, with a 1.5 px dark-red knob.
  - Lever forward is on and back is off. It snaps over in 0.15 s with a small overshoot.
- **Filter** (fluid): a 6 px steel canister, 6 px long, with a mesh band round the middle showing the filter fluid's colour. It reads as a strainer.
- **Meter:**
  - *On a pipe:* a 5 px housing ring, a 2 px stem and a 6 px round gauge with a 1 px brass bezel. The face is mid grey with 5 dark ticks.
  - *On a wire:* the same gauge, lying on the face with its dial facing out, clipped over the wire.
  - The needle is dark red. It sweeps 180° with the flow, eases toward its reading and overshoots slightly (spring-damped), so it looks like a real gauge.
- **Splitter:**
  - *On a pipe:* a 6 px hub with a 4 px dial disc on top.
  - *On a wire:* a low 5 px round body with the same disc.
  - An engraved mark on the disc shows the mode: a fork for Even, one arrow for Nearest first, a curled arrow for Round robin. The disc clicks round 120° between modes.
- **Encased:** nothing to model. It's the encasing block. With goggles on, the line inside draws as a thin cyan outline (1 px lines, 50% alpha, a quiet dash, never glowing white), along with any hidden fittings.
- **Items:**
  - Fluid Line: a short diagonal length of pipe with two couplings, in the tier material.
  - Power Line: a 3-loop coil of cable in the line's colour (its material, or its dye), with one clip-coloured tie.
  - Line Post: upright, as placed.
  - Fittings: their model on a short pipe or wire stub.

## Rig Requirements

These are the renderer's parts (Flywheel instances), each with its own model file and pivot:
- **flow_strip**: the fluid sprite in the sight slot. UV-scrolled along the pipe axis, with speed proportional to the current flow. Hidden when idle. Tinted by the fluid and lit like the pipe, never full-bright (lava in the slot doesn't glow).
- **valve_wheel**: pivots on the stem axis at the stem top.
- **breaker_lever**: pivots at its base inside the body, across the wire axis, ±35°.
- **meter_needle**: pivots at the gauge centre, normal to the dial face, over a 180° sweep.
- **splitter_dial**: pivots at the disc centre, normal to the disc, in 120° steps.
- **span**: generated geometry, not a model file. A strip of quads between the two terminal caps, 12 segments per 16 blocks.

The rest (pipes, rings, hubs, flanges, wires, clips, posts and the static bodies of the fittings) is baked into the block model.

## Fun Score (1-10)

**8.** The thin ringed pipes with the fluid moving along them, and cable strung between ceramic posts, are things players will stop to look at. Clicking a Valve wheel and watching a gauge needle swing gives each run a small toy. Dyes and encasing let a base look planned rather than plumbed.

## Builder Notes

- **Build a showcase in one Blockbench project** (`design/models/fluid-and-power-lines.bbmodel`), laid out like the mock-up so everything can be reviewed together:
  - copper and brass pipe pieces: straight, bend, T, flanged end and capped end;
  - an Intake on a tank with its filter window set to lava and to water;
  - wire on a floor: straight, inside corner up a wall, outside corner over an edge, a junction and a terminal lug on a machine face;
  - poles with 1, 2, 3 and 4 branches at different heights, and a single post, with spans between their insulators;
  - all six fittings;
  - a row of dye samples, and one wrenched two-colour join of each kind;
  - an encased stretch with the goggles outline.
- **Keep it thin.** The pipe is 4 px, the wire 1.5 to 2 px, and every fitting at most 8 px across. If something starts to look blocky, make it smaller and rounder; don't add detail.
- **Wires never become pipes.** No square cross-section bigger than 2 px. Every wire touches its face, and no wire runs through the middle of a block.
- Use fractional pixels only where the brief says (0.5 px proud, the 1 px strand). Face-attached parts leave out their underside instead of floating above the face; any overlay sits at least 0.25 px in front of what's behind it (the house z-fight rule).
- **Corners** use 22.5° steps or stepped straight pieces. Don't rely on arbitrary rotations.
- **Texture:** gentle mottling of a few close shades, with one light texel along the top of each round part. No stripes, no hazard marks, and no bright fills.
- **Animated parts:** pivots as listed above. Drive each one in a preview clip (flow speeds for copper and brass, the wheel closing, the lever snapping, the needle settling and the splitter clicking) so the motion is judged before any code.
- **Screenshot for review:**
  - a three-quarter view of the whole showcase;
  - close-ups of a pipe bend with its rings, a wire wall corner, a branched head with its span ties, and each fitting;
  - the inventory icons at 16 px;
  - the showcase at about 5 blocks' distance, to check that the thinness and the ring and clip beats read.
- **Hands-off check:** this set touches nothing on `design/hands-off.md`. Terminal lugs and flanges sit on other blocks' faces but are part of the line's model.
