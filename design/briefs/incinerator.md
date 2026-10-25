# Design Brief: Incinerator

Redesign of an existing block. What the code does today is in `design/dossiers/incinerator.md`. Family: the heat and
lava machines (Pressure Crucible, Magma Engines, Lava Generator, Geothermal Turbine).

## Hard Requirements

From the user:
- Scope: **Redesign + small code**.
- Show it's **heating a boiler**: the top is a hot plate that glows only while it's lit.
- House style; previewed in Blockbench first; nothing on `design/hands-off.md` changes.

From the code (`design/dossiers/incinerator.md`):
- Models `incinerator` (cold) and `incinerator_lit` (lit, for 5 s after anything burns); horizontal facing, modelled
  front north; front faces the placer.
- A full cube (it occludes); **nothing above y 16** (a boiler tank, hopper or funnel sits on top); ports on all six
  faces, pipes at face centres.
- Flames spawn in front of the front face at 4-8.8 px up, within 3.2 px of centre: the firebox mouth sits there.
- Light 13 when lit; the lit faces get NeoForge full-bright where they glow.
- Only the Incinerator's lines in shared files (`ComedyMonkeysCreateAddons.java`, `NewBlockScenes.java`, `ModBlocks.java`).

## Family

The heat family's language: dark blue-grey riveted steel, andesite trim, **muted amber ember** (`#5a2a18` `#a4521f`
`#c06c2c` `#d08a3e`) behind dark grates, never near-white, never whole saturated faces.

## Fantasy

A squat steel furnace that eats anything: a heavy grated firebox mouth at the front, and on top a riveted hot plate
that glows when it's burning, ready for a boiler.

## Hero Element

**The firebox mouth**: a recessed, heavy cast grate in the front with the embers glowing through it when lit.

## Silhouette Description

A full steel cube with andesite corners, a deep grated mouth low on the front, plain riveted steel sides, a hot plate
top.

## Primary Forms

- Front: a recessed mouth (about x 4-12, y 3-10) behind a dark cast grate; cold, a dark ash bed; lit, muted amber
  embers glowing through the bars (full-bright).
- Sides and back: riveted steel panels with andesite trim (no vents).
- Top: a riveted steel hot plate with a raised rim, flush at y 16; cold, dark steel; lit, a muted amber glow in
  concentric coils across it (full-bright), so it reads as heating whatever sits on it.
- Bottom: riveted steel.

## States

| State | What changes | Carried by |
|---|---|---|
| Cold | dark grate, dark hot plate | `incinerator` model |
| Lit (5 s after a burn) | embers in the mouth, the hot plate glowing | `incinerator_lit` model (full-bright faces) |

## Code Changes

- None needed beyond the models (the recess stays inside the full cube, so it still occludes).

## Fun Score (1-10)

8. A furnace that clearly heats what sits on it.

## Builder Notes

- In the shared generator `design/models/wellspring-incinerator.gen.js`; preview cold and lit, alone and under a Create
  fluid tank.
- Before: `design/dossiers/incinerator/textures-before.png`.
