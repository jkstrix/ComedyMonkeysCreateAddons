# Code Dossier: Pressure Crucible

What the code does today. Before shots: `design/dossiers/pressure-crucible/` (built by `tools/gen-before.js` from `tools/scene-crucible.json`; `open-front*` is the working side, `front*` and `back-3q` the shaft wall).

## Identity

- Block id `pressure_crucible`, `content/crucible/PressureCrucibleBlock.java` (a Create `HorizontalKineticBlock`), block entity `PressureCrucibleBlockEntity` (`KineticBlockEntity`), renderer `client/PressureCrucibleRenderer.java`, recipes `CrucibleRecipe` (type `ModRecipes.CRUCIBLE`, JEI `CrucibleCategory`), Ponder `client/ponder/CrucibleScenes.java`.
- Crafted from obsidian, a Mechanical Press, brass sheets, a cogwheel and a Basin (`recipe/pressure_crucible.json`).
- Family: heat and lava (survey), with the Magma Engines, Lava Generator, Geothermal Turbine and Incinerator.

## What It Does

1. Sits on a Blaze Burner and reads its heat the way a Basin does (`PressureCrucibleBlockEntity.java:160-163`).
2. Takes rotation through a shaft in its back wall (`HORIZONTAL_FACING` points at the back; `PressureCrucibleBlock.java:43-61`). Placed backing onto an adjacent shaft if there is one, otherwise facing away from the player so the open front faces them.
3. Holds one kind of input (only items some recipe can press) and one output (`:189-195`, `:41-46`).
4. Presses one batch at a time: `count` of the input becomes the result over `processingTime`, faster with speed like Create's Millstone (16 RPM is 1×, 256 RPM is 16×; `:105-108`). At 64 RPM an Emerald takes 2.5 s, Netherite Scrap 10 s, glass or stone half a second (README).
5. Recipes (`recipe/pressure_crucible/`): 9 Emerald Dust → Emerald (heated), 9 Netherite Dust → Netherite Scrap (superheated), 1 Sand → Glass (heated), 1 Gravel → Stone (no heat), 4 Quartz Dust → Nether Quartz (heated), 4 Nether Quartz → 2 Amethyst Shards (superheated), 4 Cobblestone → Obsidian (superheated), 64 Coal → Diamond (superheated, slow).
6. Each press: Create's press activation sound, item, flame (soul flame if superheated) and smoke particles, an advancement (`:110-131`).
7. Right-click with input loads it; empty hand takes results (or the input if nothing is pressed); funnels, hoppers and pipes insert and extract from any side (`ComedyMonkeysCreateAddons.java:148-150`).
8. Comparator output from the input count (`:230-233`).
9. Heavy stress: 12 SU per RPM by default (config `crucibleStressImpact`).

## What the Player Is Told

- Tooltip: "A _heavy press_ that squeezes _fine dust_ into _gems_ and _scrap_. Sits on a _Blaze Burner_ and takes a lot of _rotational force_" (`en_us.json:180`).
- Ponder "Pressing Dust into Emeralds and Netherite Scrap": it needs rotation and a Blaze Burner for heat; Netherite needs a superheated burner and takes much longer.
- Advancements: "Feel the Squeeze", "Hrrmmm-erald", "Under Pressure" ("Pushing down on me, pushing down on you").
- The promise: a heavy, hot hydraulic press that crushes dust into gems under enormous pressure.

## States

| State | Code | How the player tells today |
|---|---|---|
| Empty | `Status.EMPTY` | Empty bowl |
| Not enough input | `NOT_ENOUGH` | Goggles ("Needs 3 more"); 1–4 flat items in the bowl |
| No rotation / overstressed | `NO_ROTATION` | Shaft still; goggles |
| Burner not hot enough | `TOO_COLD` | **Goggles only**; the bowl's ember texture glows regardless |
| Output full | `OUTPUT_FULL` | Goggles only |
| Working | `WORKING`, `processing` | Plate lowers over exactly one item's time, then eases back up |
| Heat present (none, smouldering, kindled, seething) | the burner below | The burner itself; nothing on the crucible |
| Heat needed (none, heated, superheated) | the recipe | Goggles and JEI only |

## Moving and Changing Parts

- **Shaft stub** `block/pressure_crucible_shaft` (modelled pointing south, turned to the back and spun about the world axis; `PressureCrucibleRenderer.java:55-65`).
- **Press plate** `block/pressure_crucible_ram` (Create's `mechanical_press_head` texture, 8×8 px, 2 px thick), lowered from y 11 to y 4.5 by `getPlate` (`:67-72`): it follows the press down exactly, then eases back up (`PressureCrucibleBlockEntity.java:271-287`).
- **Pole** `block/pressure_crucible_rod` (Create's `mechanical_press_pole` texture), scaled from the plate up to the housing at y 13 (`:74-80`).
- **Contents**: the input drawn as 1–4 flat items and the output as one, lying in the bowl at y 4.05 (`:82-91`).

## Code Contract

- Blockstates `facing` (4) → `block/pressure_crucible` rotated by `y`, modelled with the back (the shaft) to the **south** and the open front to the north.
- Renderer parts: `pressure_crucible_ram`, `_rod`, `_shaft`, registered in `ComedyMonkeysCreateAddonsClient.java:111-113`.
- Renderer constants: plate underside from y 11 (top) to y 4.5 (bottom), plate 2 px thick, the housing it hangs from at y 13, contents at y 4.05 and 0.3 scale, the shaft x 6–10 / y 6–10 / z 12.5–16 at the back.
- Full-block shape, `noOcclusion`, not a redstone conductor; no light emission.
- Item model `models/item/pressure_crucible.json` (a copy of the block model).
- Borrowed from Create: `brass_casing`, `brass_gearbox`, `mechanical_press_head`, `mechanical_press_pole` and `axis` textures.

## Current Look

- A brass-casing box: a base slab, a top slab, four dark pillars with bright brass bands, a back wall with Create's brass gearbox face around the shaft, a shallow bowl in the middle with an ember texture, and Create's press head on its pole.
- What works: the open front shows the press and the contents; the plate's timing is exactly the press time.
- What doesn't: saturated bright brass everywhere (against the dark muted house style); the bowl glows whether or not anything is burning underneath, so the one rule players get wrong (is it hot enough?) isn't shown at all; the press is Create's head, small and borrowed, and doesn't read as heavy; nothing says "pressure".

## Function the Look Doesn't Show

- Whether the burner is hot enough for what's inside (heat present vs needed): goggles only.
- Output full: goggles only.
- How much input there is relative to a batch.

## Look the Code Doesn't Back Up

- The ember bowl glows even with no burner, or a cold one.

## History

- Ranked 2nd in the survey (score 12): "a heavier press of its own instead of Create's borrowed head, and a bowl that reacts to heat would show the one rule players get wrong".
