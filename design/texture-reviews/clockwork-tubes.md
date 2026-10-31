# Texture Review: Clockwork Tubes

Family: Trade Bot (the parts it borrows), Helper Bot, Clockwork Dynamo (bot-built); Fluid Lines (the mod's other glass)  ·  Brief: design/briefs/clockwork-tubes.md  ·  Model: design/models/clockwork-tubes.bbmodel (generator `clockwork-tubes.gen.js`)  ·  Date: 2026-10-31

Screenshots judged from:
- Gameplay distance (about 6 blocks), the diagram running: `clockwork-tubes/gameplay-distance.png`
- Inventory size (about 48 px a block), Receiver and Brass Inlet: `clockwork-tubes/inventory-size.png`
- Block size: `../models/clockwork-tubes-review/diagram.png`, `inlets-plain-brass.png`, `facings-and-shapes.png`, `receiver-side-and-upside-down.png`
- Primary close-up: `../models/clockwork-tubes-review/receiver-ring-gear.png`, `flow-item-in-throat.png`
- Family side by side: `../models/clockwork-tubes-review/family-with-dynamo.png`
- Palettes sampled from the shipped `entity/trade_bot.png` (most-used: steel `#3c424a` `#4a5059` `#2f343b`, brass `#b08f45` 364 px over `#8f7638` 100 px, `#c9a75a` 81, dark brass `#6e5a2a` 197, andesite `#7c7f79` `#6b6e6a` `#8a8c86`, `#5a616b` lid edges) and the Fluid Lines generator's glass.

## Texture Contract

New asset; nothing in the mod yet. Textures will be `block/clockwork_tubes_*.png` (or split per block at export), 16 × 16, painted by the generator. Faces map 1:1 by position, except the glass panes and throat windows (explicit UVs, 1 texel a pixel, `_v`/`_h` by the pane's run). Everything cutout; no glow. Borrowed in the preview only: Create's cogwheel and depot, vanilla barrel, polished andesite and item textures (stand-ins, never exported).

## Design Intent

The bots' own vacuum post: glass tubes, gulping hoods, and one brass ring gear that pulls everything through its middle. It must read as built by the Trade Bot's makers, sharing his parts, colours and textures ("There's no elements from the trader bot on there, shared colours or textures", user). Themes: clockwork, friendly-industrial, logistics. Primary: the brass ring gear (Receiver); second read: the Inlet's lid-plate gulp; glass so the work is visible.

## Current Strengths

- **The Trade Bot reads at once.** His andesite mottle, his steel, his brass waist band (brass with dark brass every other pixel) on both Inlets, dark-brass bolts, his steel lid plate. In the family shot the Inlet and Receiver sit naturally beside the Dynamo.
- **The hero carries.** Brass ring gear between andesite housings is the strongest contrast on the Receiver at every distance, inventory size included.
- **Glass is calm.** Glints on the outward faces only; the tubes read as glass with steel rails, and items show through (iron ingot visible in the throat window mid-flow).
- **The tiers read** without labels: plain Inlet with a steel lip and socket, Brass Inlet with brass lip, socket and filter panels.
- **No face** from any facing; hubs carry one bolt per face.
- **Interiors** use the Dynamo's dark and floor shades (`#1f2327`/`#24282d`, `#16181b`/`#1a1d20`): one "inside" across the bot-built family.

## Current Weaknesses

1. **Three brasses on one family.** The waist band uses the Trade Bot's brass (mostly `#b08f45`, `#8f7638` under, `#c9a75a` highlights). The ring gear, Brass Inlet lip and socket use a darker mix (`#8f7638` 80%, `#b08f45` 20%). The filter-panel rims use the generators' mid brass (`#a3873f` / `#7e6832`), and the gear teeth's dark side mixes in `#7e6832`. On the Brass Inlet all three sit within a few pixels of each other: the band looks gilded, the lip a duller metal, the rim a third. It breaks "the same material looks the same" and dilutes the Trade Bot tie the user asked for.
2. **The tube glass is a second glass.** Ours is a green-teal (`#6f8487` edge, `#93a9ac` glint); the Fluid Lines' glass is a cool blue-grey (`#a9bac2` / `#c2cfd5` / `#d2dde2`). The two will run side by side in bases, and the shift in hue reads as two different glass makers.
3. **The filter rim is generic**, not the Trade Bot's. His rims (eye rims) are brass lit on the top-left with dark brass `#6e5a2a` on the lower edge; the filter rim uses the generators' `#a3873f` / `#7e6832` instead.

## Storytelling Evaluation

- **Who made it**: the bots' makers; the player crafts it from andesite parts. The textures say so through the Trade Bot's band, bolts, lid plate and rims.
- **Who uses it**: anyone gathering items; it runs unattended.
- **How old**: new. Factory-fresh is right; no wear has a reason (no heat, no exhaust, no hands on it).
- **How often / where**: constantly, beside farms, depots and storage. Items pass inside the glass, so nothing stains the outside.
- **How expensive**: andesite age with a brass upgrade. Brass on the plain blocks is now the bots' signature, not a tier marker (user's call); the Brass Inlet still shows more brass than the plain one.
- **Who repairs it**: nobody; no maintenance marks.

## Material Evaluation

| Material | Reads? | Against the family |
|---|---|---|
| Andesite | Yes | The Trade Bot's exact mottle (`#7c7f79` with `#6b6e6a` / `#8a8c86`) |
| Dark steel | Yes | The Trade Bot's shades; his `#5a616b` lid edge on the flap. Bot mottle on 1 px steps kept (user's call on the Dynamo) |
| Brass | Yes, but three mixes | Band matches the Trade Bot; gear, lips and socket darker; filter rims in the generators' mid brass (weaknesses 1, 3) |
| Dark brass `#6e5a2a` | Yes | The Trade Bot's bolts and band lines: matches |
| Glass | Yes | A teal hue unlike the Fluid Lines' blue-grey glass (weakness 2) |
| Interior dark | Yes | Same as the Dynamo's window |

## Minecraft Readability Evaluation

- **Primary**: the ring gear (brass on andesite and steel): strongest contrast, reads at inventory size and at 6 blocks. The Inlet's flap is seen only from below, as intended; from the side the brass band carries the Inlet.
- **Secondary**: housings, hood rings, tube rails and collars, hubs: clear value steps (andesite against steel).
- **Tertiary**: crown bolts, hub bolts, band lines: present but never louder than the gear.
- **Texel density**: 1 texel a pixel on every face, panes and windows included.
- **Clutter**: none; no gradients, no zoom-only detail. The band's every-other-pixel line is the Trade Bot's own and reads as a knurled band at block size.

## Cohesion Evaluation

- **Shared materials**: andesite, steel and dark brass now match the Trade Bot exactly; brass doesn't yet (weakness 1).
- **Manufacturing language**: stepped corners, framed andesite panels, riveted brass band, lid plate: the bots' kit, read as one maker with the Dynamo.
- **Colour logic** (visual language): no copper (nothing electric), no glow, brass as trims and the hero gear. Brass on andesite-tier blocks is new: the doc says each tier keeps its own trim metal, and bot-built blocks now override that (proposed addition below).
- **Age**: all new, like the Dynamo and the bots.
- **Against the Dynamo**: its waist band is the generators' mid brass (`#8f7638` with `#a3873f` rivets), a calmer, different pattern from the Trade Bot band the Tubes now wear. Side by side the two "bot bands" differ (retexture candidate).

## Recommended Texture Changes

1. **One brass, the Trade Bot's, everywhere on these blocks.** Ring gear, Brass Inlet lip and socket: from `#8f7638` 80% / `#b08f45` 20% → the Trade Bot's base pair in his proportion, `#b08f45` ~70% / `#8f7638` ~30%, **without** `#c9a75a` on these big areas (his highlight stays on the band only, where it's his own). Gear teeth sides: `#6e5a2a` / `#7e6832` → `#6e5a2a` / `#8f7638`. Why: weakness 1; one material, one look. Risk: brighter than the generators' brass, which the house notes say can read too bright on blocks; the preview will show it beside the Dynamo before anything is kept.
2. **Filter rims as the Trade Bot's eye rims**: `#a3873f` top-left / `#7e6832` bottom-right → `#b08f45` top-left / `#6e5a2a` bottom-right. Why: weakness 3.
3. **Tube glass in the Fluid Lines' hue**, still cutout (items must show through): glint `#93a9ac` → `#a9bac2`, edge `#6f8487` → `#6f7f87`. Why: weakness 2; one glass in the mod.

No shape problems for the modeller. (The Inlet's slightly stacked hood is a shape choice already offered to the user, not a texture issue.)

## Recommended Retexture Candidates

Now that this asset exists, are there earlier assets that should be revisited for visual consistency?

| Asset | Reason for review | What has changed | Proposed update |
|---|---|---|---|
| Clockwork Dynamo | Its waist band is the generators' mid brass (`#8f7638` field, `#a3873f` rivets every 3 px); the Clockwork Tubes wear the Trade Bot's own band (`#b08f45` with `#6e5a2a` every other pixel) | The user asked bot-built blocks to share the Trade Bot's textures; the bot-built family now has two different "bot bands" | Repaint the Dynamo's band (and its brass trims: lamp rim, collar, vent frames) with the Trade Bot band and brass, as change 1 here. Same texture names and UVs; preview beside the Tubes first |

Helper Bot: none; it already uses the Trade Bot's palette. Fluid Lines: none; change 3 brings the Tubes to their glass.

**Proposed visual-language additions** (written into `design/visual-language.md` only on your approval):
- *Makers, the bots*: "Bot-built blocks borrow the Trade Bot's actual parts and painters: his waist band (brass with dark brass every other pixel), dark-brass bolts, eye-rim brass rims, steel lid plates, his station's brass cog. This holds on any tier: for bot-built blocks it outranks 'each tier keeps its own trim metal'." (user, 2026-10-31)
- *Materials, Bot brass*: extend "Used for" to "bot-built blocks' bands, rims, cogs and lips"; big areas use `#b08f45` / `#8f7638` only, `#c9a75a` stays on bands and small trims.
- *Materials, new row, Glass (cutout)*: edge `#6f7f87`, glint `#a9bac2`, glints on outward faces only; "item tubes and windows, where the renderer draws items behind it". The Fluid Lines' translucent glass stays as it is.

## Art Director Verdict

**Approved with changes.** The Clockwork Tubes now read as the Trade Bot's makers' work and strengthen the bot-built family; the ring gear is a clear hero at every distance. Before the feature counts as finished: unify the brass (changes 1 and 2), match the mod's glass (change 3), and decide on the Clockwork Dynamo's band so the bot-built family wears one band.

## Follow-up

User, 2026-10-31: took all four.
- **Change 1, one brass**: done in `clockwork-tubes.gen.js` (ring gear, lips, socket: `#b08f45` over `#8f7638`, no `#c9a75a`; teeth sides `#6e5a2a`/`#8f7638`). **Change 2, filter rims**: `#b08f45` lit, `#6e5a2a` lower edges. **Change 3, one glass**: edge `#6f7f87`, glint `#a9bac2`. Z-fight and export checks still pass.
- **Clockwork Dynamo**: band repainted with the Trade Bot band (dark brass every other pixel along its length, his brass mottle), brass trims (lamp rim, collar, band corners) and vent frames to `#b08f45` over `#8f7638`, in `clockwork-dynamo.gen.js`; same texture names and UVs. Previewed beside the Tubes (`clockwork-tubes/after-family.png`, `after-dynamo.png`). **Not yet in the mod**: the shipped `block/clockwork_dynamo_*.png` change when the textures are exported (with the Tubes going into the mod, or on request).
- **Visual language**: the three additions written into `design/visual-language.md` (bot-built blocks borrow the Trade Bot's parts on any tier; where bot brass goes; cutout glass).
- Also since the review: hubs open where a tube joins (user: "The corner joints are blocked"); the preview branches out of its plane; sound is Create's cog sound only (spec D37).
