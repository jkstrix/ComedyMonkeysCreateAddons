# Design Brief: Trade Master items (Netherite Sheet, Trade Ledger)

Items for the Trade Master's recipe (spec D27, D38). Model: [trade-master-items.gen.js](../models/trade-master-items.gen.js) → `trade-master-items.bbmodel`.

## Hard Requirements
- Netherite Sheet: "pressed netherite"; one netherite ingot in a Mechanical Press makes one (spec D38)
- Trade Ledger: a sequenced assembly of book → precision mechanism → block of emerald → electron tube → press (D27); it has an incomplete form for the assembly (Create's convention)
- Item models are 3D, shown at the standard angle, like Create's items (user, 2026-11-03), **except the Netherite Sheet**: "sheet wants to look like the iron, brass etc. that are already in the game ... they only have an icon"; "they come from an ingot, so should this" (user, 2026-11-04)
- Previewed in Blockbench before they go in the mod (user)
- Netherite colours: blocks and ingot-made items use the netherite **block** ramp (visual language, 2026-11-04)

## Asset Type
Items. Block-model geometry (Java, rotations in 22.5° steps), 16×16 textures, display transforms with the inventory view fitted to the slot.

## Fantasy
- **Netherite Sheet:** one more of Create's sheets: a netherite ingot through the press. A flat icon, Create's sheet shape (as the iron, brass and golden sheets) in the vanilla netherite ingot's colours.
- **Trade Ledger:** the Trade Master's own account book, the one strapped to his back: walnut and brass, an emerald set in a brass cog, an electron tube for a bookmark.

## Hero Element
- Sheet: the family shape; you know it's a sheet before you read the name.
- Ledger: the emerald in its brass cog on the cover.

## Theme
| Part | Colours |
|---|---|
| Netherite (the ingot's shades, on Create's brass-sheet pixels) | `#241e1f` `#31292a` `#3c3232` `#4c4143` `#4d494d` `#5a575a` `#656163` `#737173`, sparkle `#8c888a` |
| Walnut (house) | `#4e3626` `#634532` `#6f4d36` |
| Bot brass | `#6e5a2a` `#8f7638` `#b08f45`, `#c9a75a` highlights only |
| Paper | `#8a8170`, rules `#7a7262` |
| Emerald | `#4f9a68`, glint `#6fb184` |
| Electron tube | Create-like: a muted rose bulb `#a8656d`/`#c2848b` on a brass base |

## Shape Language
Squares: a plate and a book. The cog emblem and the gem are the one round accent.

## Silhouette Description
- Sheet: Create's sheet silhouette (the iron, brass and golden sheets' pixels). Family strip: [netherite-sheet-family.png](../models/netherite-sheet-family.png).
- Ledger: a closed book lying flat, 11 × 13 px, 4 px thick; the spine on the left, the clasp over the right edge, the tube's bulb peeking out of the top.

## Primary Forms
Sheet: the icon. Ledger: the book block (covers and pages).

## Secondary Forms
Ledger: the spine with two brass bands, the clasp, the brass corner caps.

## Interesting Features
- The ledger's cover emblem is its recipe made visible: the precision mechanism's brass cog, the emerald, the tube.
- The incomplete ledger: the plain book with a loose brass cog lying on it, which is the first step of the assembly done.

## Fun Score (1-10)
**8**: small items, but each tells its story (fresh off the press; the merchant's ledger) and matches the model it belongs to.

## Builder Notes
- Real geometry, no flat sprites; one texel per pixel; mottled 2–3 shades, no flat fill.
- The ledger matches the one on the Trade Master's back (walnut, brass, emerald, paper edge).
- Screenshots: each item at the standard inventory angle (large and at slot size), side by side.
