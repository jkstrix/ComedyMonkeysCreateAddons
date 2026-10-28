# Design Brief: Electric Lights, Street Lamps (group 1)

Group 1 of the Electric Lights set ([spec](../specs/electric-lights.md)): the **Cast Iron Lamp Post**, the **Steel Lamp Post**, and the four lamp heads that go on them, **Lantern Head**, **Cobra Head**, **Globe Head** and **Yard Light Head**. Later groups: wall and ceiling, hanging, runs, specials.

## Hard Requirements
- "Street lamps (multiple styles)", "Create styled but fit each scenario they are meant for", "fairly cheap to make", "interesting, varied and useful" (user, 2026-10-28).
- "Street lights can use a similar build mechanic as the electricity poles" (user): Lamp Posts **stack into a pole of any height** like the Line Post, with **foot, pole and head** looks by position, and a single post is foot and head in one block (spec D6, D14, P8).
- **Two finishes: Cast Iron** (dark, fluted foot, scroll brackets on its arms, old-town) **and Steel** (plain round tube, modern). **Any head on either; arms take the post's finish** (spec D21).
- The head takes **one lamp on top** and **up to 4 arms**, one per side, each with a lamp head at its tip (P8).
- **A small insulator bracket under the top ties spans**, like a Line Post's insulator (P8).
- A lamp head can also **stand on top of any block** (spec Rule 7).
- "Allow different colours by dyeing them": the dye colours **only the glow** (lens, halo, beam), **muted to the house palette**; **undyed is a warm incandescent tone** (D13). The light cast on the world stays white.
- The **Yard Light Head warms up like a sodium lamp**: a dim amber glow rising to full over 4 s (P4). Lights also **flicker** when power runs short (P3). Both are drawn by the renderer's glow, not the model (P28).
- House rules: dark, muted Create palette, mid grey at most; **glows muted, never near-white**; accents sparingly; every look **previewed in Blockbench before it goes into the mod**; solid parts as Flywheel instances, glow on the renderer (D8).

## Asset Type
Blocks (6). 16 px = 1 block. A typical street lamp is foot + 2 pole + head + a lamp on top: 5 blocks tall. Lamp heads on top are their own block standing on the head post; arms and the heads on them belong to the head post (drawn as part models, see Rig Requirements). Models may reach outside their block (arms out to 14 px from the pole's centre).

## Fantasy
The street furniture of a Create town: dark iron posts with lanterns along the old lanes, slim steel poles with cobra heads over the rail yard road, a ring of globes over the plaza, all switching on at dusk in a ripple of warm light.

## Hero Element
**The lit head.** Each head is a distinct, readable lamp shape with a glowing lens and a soft halo, held up on a dark post so the light is the brightest thing in the scene. The post is the stand; the head is the star. Between heads, the four silhouettes must never be confused.

## Theme
Sits beside the wooden **Line Posts** and their spans (same stacking logic, same ceramic insulators), Create's trains and stations, and the mod's dark steel machines.

| Material | Use | Colours |
|---|---|---|
| Cast iron | Cast Iron post, its arms, the Lantern's frame | near-black blue-grey `#26292d` `#2f3338` `#3a3f45`, gently mottled; edges catch `#454b52` |
| Galvanised steel | Steel post and its arms | mid-dark grey `#565c62` `#62686e` `#6e7479` (never lighter) |
| Dark steel | Cobra and Yard Light housings, bolts, the hatch | `#2f343b` `#3c424a` `#4a5059` |
| Muted brass | small trims only: the Lantern's finial, the Globe's collar ring, hatch hinge | `#7e6832` `#8f7638` `#a3873f` |
| Ceramic | the span insulator | bone grey `#7d786f` `#8f897e` `#9c968b` (as the Line Post) |
| Glass, unlit | lenses when dark | smoky `#3a3d3f` with a faint pane line `#4a4e50` |
| Glow, undyed | lit lenses | warm incandescent `#d99a4a` core, `#c47f3c` edges; halo the same at low alpha |
| Glow, Yard Light | sodium warm-up | starts `#8a2f22` (dark red), rises to `#d9883a` (sodium amber) over 4 s |
| Glow, dyed | lit lenses | the dye, muted and lifted for a lens, the quick-read's set: white `#d6d3c8`, orange `#d38a4c`, magenta `#b979ad`, light blue `#7fa9bf`, yellow `#d2b964`, lime `#9cba66`, pink `#cf97a5`, gray `#8c8f91`, light gray `#b0afa8`, cyan `#5fa6a6`, purple `#9270b4`, blue `#6e7cbd`, brown `#a7845f`, green `#86a058`, red `#c0665a`, black `#5c5c62` |

## Shape Language
**Posts: squares (Cast Iron) and circles (Steel).** Cast iron is stepped and square-shouldered, with heavy mouldings at the foot, like a Victorian casting. Steel is a plain round tube (plus-shaped cross section) with nothing but a base plate and bolts.
**Heads: one strong shape each.** Lantern = a square box with a pyramid roof (squares + a triangle). Cobra = a long, flat teardrop leaning out (a triangle in profile). Globe = a sphere (circle). Yard Light = a flat rectangular box tilted down (square). The four shapes are the variety; their materials stay quiet.

## Silhouette Description
- **Cast Iron + Lantern on top:** a heavy stepped foot, a straight dark shaft, a collar, and a square lantern with a pointed roof and finial: "a chess pawn on a stick, with a lit lantern for a head".
- **Steel + Cobra Head on an arm:** a thin straight pole, an arm sweeping up and out, and a long flat head leaning over the road: "a question mark tipped on its side".
- **Cast Iron + 4 arms of Globes:** a cross of scrolled brackets each holding up a ball: "a candelabra".
- **Steel + 2 Yard Lights:** a T with a flat box at each end tilted down: "a stadium mast in miniature".
- **From 5 blocks:** each head reads by outline alone: square with a roof, long flat wedge, ball, flat tilted box.
- **Inventory:** posts as a single post (foot + head in one) with no lamp; each head on its own, three-quarter view, lens lit.

## Primary Forms
**Cast Iron Lamp Post** (one block per segment, faces out of the surface it stands on like the Line Post):
- **Foot:** a stepped plinth, 8 × 8 px at the ground, 2 px tall, stepping to 6 × 6 px for 3 px, then a 1 px moulding ring at 5 × 5 px, then the shaft. The 6 × 6 section has **fluting**: four vertical grooves per side, 1 px wide, a shade darker. On the side facing the placer, a small **access hatch** (3 × 4 px, inset ½ px, a brass hinge pixel and a keyhole).
- **Pole:** a 4 × 4 px shaft, plain, with faint vertical fluting in the texture only (no geometry, no bands at block joints, as the Line Post).
- **Head:** the shaft rises to 12.5 px, then a **collar** (6 × 6 px, 2 px tall) at 12.5–14.5 px with a 5 × 5 px cap plate on top at 14.5–16 px, flush with the block top so a top lamp stands on it as on any block. The **arm sockets** are on the collar's four sides.
- **Single post:** the foot's plinth with the head's collar on a short shaft.

**Steel Lamp Post:**
- **Foot:** a square **base plate** 8 × 8 × 1 px with four bolt heads (1 px, dark steel) at the corners, the tube rising from it, and a flush **access panel** (2 × 4 px, its outline a line of darker pixels, two screw pixels) low on the placer's side.
- **Pole:** a 3 px tube built as two crossed boxes (3 × 2 and 2 × 3) for a round read, plain.
- **Head:** the tube rises to 13 px and ends in a **cap** (4 × 4 px, 1 px tall); arm sockets are a 1 px **clamp band** (4 × 4 px) at 11–12 px.
- **Single post:** base plate, short tube, cap.

**Arms** (on a head, 1 to 4, one per side, in the post's finish):
- **Cast Iron arm: a scroll bracket.** A 2 × 2 px bar runs out level from the collar to 14.5 px from the centre (12 in the first build crowded the plaza globes); under it a **scroll** of stepped 1 px cubes curls from the shaft (3 px below the bar) up and out to meet the bar halfway, ending in a small curl at the shaft. A **hook** at the tip: a 1 px drop with a ring, from which a Lantern hangs; other heads sit on a 3 × 3 px **cup** at the tip instead.
- **Steel arm: a sweep.** A 2 px tube leaves the clamp band level, then rises at a gentle slope (about 1 px up per 4 px out, stepped) to 13 px out, ending in a short horizontal **spigot** (2 px) that the head slips onto.

**Lamp heads** (each has two mountings: **on top**, standing on a post or any block, and **at an arm tip**):
- **Lantern Head:** a square lantern, 6 × 6 px body, 7 px tall, four glass panes (5 × 5 px glow each) in a 1 px cast-iron frame, a pyramid roof (stepped 7 → 5 → 3 → 1 px), a 1 px brass finial ball, and a 1 px vent ring under the roof. **On top:** stands on a 4 × 4 px tapered base. **At an arm:** hangs from the hook on a 1 px ring above its finial.
- **Cobra Head:** a dark steel housing, 5 px wide, 12 px long, 3 px deep at the back tapering to 2 px at the nose, its top domed (a 1 px step either side), its **lens underneath only** (a 3 × 8 px glow panel recessed ½ px). A 1 px **ridge** runs along its back. **At an arm:** slips onto the spigot, level. **On top:** its own short **gooseneck** (a 2 px tube rising 4 px and curving out 6 px) carries it off-centre over the street.
- **Globe Head:** a frosted ball, 8 px across (a stepped sphere: 4, 6, 8, 8, 6, 4 px rings), **all of it glow**, on a 1 px **brass collar ring** (4 × 4 px) and a 2 px neck. **On top** and **at an arm:** the same, sitting upright (on the cup at an arm tip).
- **Yard Light Head:** a flat dark steel box, 10 × 7 px, 3 px deep, **tilted 22.5° down** from its mounting, its **lens on the underside** (8 × 5 px, glow), three 1 px **cooling fins** along its top, and a small **ballast box** (3 × 3 × 2 px) behind it on the yoke. **At an arm:** on a short yoke (a 1 px U-bracket) at the tip. **On top:** on a yoke on a 2 px stub, facing the way it was placed.

**Span bracket** (every head post, one): a 1 px iron bracket out from the shaft, low on the head block (y 3–4, moved in the build so it clears a north arm's scroll), on the post's own side (opposite its hatch), with a **small ceramic insulator** standing on it (the Line Post branch insulator: 4 px and 2.4 px rings, a tier-coloured cap). Spans tie there.

## Secondary Forms
- The cast-iron foot's mouldings and the hatch say "cast and serviced". The steel foot's bolts and flush panel say "bolted down by the council".
- The Lantern's vent ring and finial; the Cobra's ridge; the Yard Light's fins and ballast box. Each is a real part that explains how the lamp works (venting heat, housing the gear).
- The scroll on cast-iron arms is the only ornament in the set, and it's structure (it braces the arm).

## Interesting Features
- **Distinct heads by outline.** A player can stand at the end of a street and tell which lamps are which without colour.
- **Hanging vs standing lanterns.** A Lantern hangs from a cast-iron arm's hook but stands upright on a post top, as real lanterns do.
- **The Yard Light's warm-up.** At switch-on it glows dark red and climbs to sodium amber over 4 s. A rail yard lighting up is a small show.
- **Dye only lights the glass.** A blue-dyed globe is a dark iron post with a cool blue ball; the post never changes.
- **Real service detail.** Access hatches, bolts and the span insulator say these are part of the town's power network.

## Rig Requirements
No animation. What the code needs as separate parts:
- **Glow parts** (one per head, per mounting): the lens or glass faces only, as their own model, so the renderer can draw them emissive, tinted by the dye, faded by brightness and flickered (P3, P4, P28). The Globe's glow part is the whole ball; the Lantern's is its four panes; the Cobra's and Yard Light's are their underside lenses. The halo is a renderer quad, not geometry.
- **Arm parts:** each finish's arm as a part model built pointing north from the pole's centre, rotated by the renderer per side (Flywheel parts).
- **Arm-tip head parts:** each head's arm mounting as a part model positioned for the north arm tip (so it rotates with the arm).
- **Baked block models:** the posts' foot, pole, head and single looks for both finishes (facing variants like the Line Post); the four heads' on-top mounting.
- **Pivot:** arm parts pivot at the pole's centre axis (8, y, 8); the Yard Light's tilt is baked into its model, about the yoke pin.

## Fun Score (1-10)
**8.** Four lamp shapes on two post styles with 1–4 arms give a lot of real-world variety from a few parts, and a street of them switching on in a warm ripple, a yard warming up through red to amber, and dyed globes over a plaza are moments players will stop to look at.

## Builder Notes
- **Proportions to keep:** posts are 4 px (cast iron) or 3 px round (steel), so they read heavier than the 3 px wooden Line Post; heads 6–12 px; arms reach 12–13 px from the centre. Don't thicken the posts past 4 px, or a street turns into a fence.
- **Pitfalls:** don't let the glass go lighter than the glow palette; unlit glass must read as dark smoky glass, not grey plastic. No hazard stripes, no saturated yellow. Don't add bands at block joints of the pole. Keep the scroll bracket to stepped 1 px cubes; don't over-ornament it.
- **Glow vs model:** in Blockbench, show lit lenses with the undyed warm colour on separate glow groups (named `glow_*`) so the screenshot reads as lit; in game they come from the renderer.
- **Screenshot for review:** a dusk scene with
  1. Cast Iron, 4 blocks, Lantern on top;
  2. Cast Iron with 2 arms of hanging Lanterns;
  3. Steel, 5 blocks, 2 arms of Cobra Heads (a double road light), plus one Steel with a Cobra on top on its gooseneck;
  4. Cast Iron with 4 arms of Globes and a Globe on top (the plaza lamp);
  5. Steel with 2 arms of Yard Lights, one shown mid warm-up (dark red);
  6. single posts of both finishes with nothing on top (finial and cap);
  7. a row of Globes on top of plain blocks in five dye colours;
  8. one span tied to a lamp post's insulator and running to a wooden Line Post, for scale against the existing set.
  Plus close-ups of each head (lit and unlit), each foot, and each arm.
