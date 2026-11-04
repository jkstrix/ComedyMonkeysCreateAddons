# Design Brief: Trade Board

The Trade Master's chest attachment (spec D31, D34, D37, D40, D41). Model: [trade-board.gen.js](../models/trade-board.gen.js) → `trade-board.bbmodel`; into the mod with [trade-board.export.js](../models/trade-board.export.js). Replaces the Trade Hatch: "Can we change it to a trade board, the hatch looks silly and isn't required" (user, 2026-11-04); the plain board approved: "much cleaner".

## Hard Requirements
- "Something smaller, something we could attach and it not be in the way. Thin like an item frame where the gauges go in the front" (user, 2026-11-04)
- It **hangs like an item frame** (an entity on the container's face, taking no block space); Create's Factory Gauges placed on that face sit on its front (user's pick, 2026-11-04)
- "We should be able to fill the whole face with gauges, same limit as you can put on a packager side" (user, 2026-11-04): four, one gauge block's worth
- A **plain flat board**: no grille, no flap, no bulb (user, 2026-11-04)
- On any item container (chest, barrel, item vault, the mod's upgraded chests) (H2); inputs are boards too (D40)
- The bots' parts (house rule); cheap: andesite casing + brass sheet + electron tube (H4, user)
- Previewed in Blockbench first

## Asset Type
A hanging entity's model (like an item frame), drawn by its renderer; 16×16 textures. It covers one whole face of its container, 1.25 px deep. It doesn't move.

## Fantasy
A mounting board off the Trade Master's own body, screwed to your chest for his gauges.

## Hero Element
The four gauge places, which the gauges fill.

## Theme
The bots' own parts (from `trade-bot.gen.js`):

| Part | Colours |
|---|---|
| Andesite board (his panels) | `#6b6e6a` `#7c7f79` `#8a8c86`; a pressed seam `#5b5e5a` between the four gauge places |
| Rim: his dark steel | `#2f343b` `#3c424a` `#4a5059`, top and left edges `#5a616b` (as his lid plates) |
| Bolts, the one accent | dark brass `#6e5a2a`, one in each corner |

The gauges bring their own brass rims, so the board stays andesite and steel.

## Shape Language
Squares: a framed panel in four, like his body panels.

## Silhouette Description
Flat on the container's face: a 0.75 px andesite board, a 1 px steel rim half a pixel proud round its edge (four bars that meet), and a painted seam in the 2 px between Create's four 6 × 6 px gauge panels.

## Primary Forms
The board and its rim.

## Secondary Forms
The seam between the gauge places; the corner bolts.

## Interesting Features
Empty, it's an input (he takes from that chest); with gauges, an output (he fills it to what they ask, D40). It reads the same either way: the gauges are the difference.

## Fun Score (1-10)
**7**: deliberately quiet, a mount for the gauges; the gauges and the Trade Master carry the fun. Accepted as a utility part (user: "much cleaner").

## Builder Notes
- Nothing of the rim in the gauges' 6 × 6 squares; the board (1.25 px) stays under their 2 px.
- Screenshots: on a barrel, bare, with two gauges and with four.
