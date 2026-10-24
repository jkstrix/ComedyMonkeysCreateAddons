# Design Brief: Trade Bot Package

## Hard Requirements
- "We should design a new 'package' block for how it looks in the inventory (you still just put down a station with a bot on top)"
- "it should be a station with a folded up bot on it"
- "and be 3d": the inventory slot shows the 3D model, not a flat sprite. This replaces the flat 16 × 16 icon draft, which is dropped.
- It's only the item's look: in the inventory, in hand, on the ground and in item frames. Placing it still sets down the winding station with Trade Bot docked on it, unfolded as usual (design/specs/trade-bot.md D27).
- Create mod colours; the mod's dark, muted style. It's built from the existing Trade Bot and station model, with no new colours.
- Previewed in Blockbench before it goes into the mod.

## Asset Type
A static **pose** of the existing Trade Bot rig on his station: a new one-frame clip, `packed`, in design/models/trade-bot.bbmodel. In game it's drawn in 3D everywhere, including the inventory slot, by the item renderer (the same geometry and texture as the bot and station), so no new model or texture is needed.

## Fantasy
Trade Bot folded up for the journey like a camping chair, asleep on his own station, ready to be set down and spring open.

## Hero Element
**His eye pods resting on his body with the lids shut**, like a pair of binoculars put away. Even this small, the closed eyes say "he's in there, switched off", and they make placing him (when the lids open) feel like a wake-up.

## Theme
Exactly his and the station's materials: steel, andesite, muted brass and copper, rubber. **All lights are off**: no glowing irises, no station glow, because he's packed and unpowered. The brief's palette, nothing added.

## Shape Language
His circles, packed into a compact rounded block. It reads as one chunky object, a robot tucked onto a plinth, not a tall figure.

## Silhouette Description
About one block tall, and no taller than the station's winder post (15 px):
- the station plinth along the bottom;
- his body sitting low on it, wheel hidden;
- his folded head as a flat cap on top;
- arms tucked in at the sides.

The silhouette is a squat stack on a wide base, roughly a cube. In the inventory's three-quarter view the plinth, the closed eye pods and the brass key bow should all be visible.

## Primary Forms
- **The station:** as built, with the winder arm swung back to its rest position and the signal flag down.
- **His body:** dropped down onto his wheel. The chassis telescopes down over the wheel and forks until it sits just above the deck, with the wheel hidden inside the cradle.

## Secondary Forms
- **Neck:** fully retracted, so the head sits on the chassis top cap.
- **Head and eye pods:** sunk right down onto his chassis top with a slight sleepy droop (12° forward), lids fully shut. *(Changed while building, approved: tipping the pods forward 70° stood them up like bricks, because they're deeper than they are tall.)*
- **Arms:** upper arms straight down against the side panels, forearms folded up across his belly, claws closed. The drawer is shut.
- **Aerial:** folded back flat along the head, pointing backward.

## Interesting Features
- **The wind-up key on his back stays upright:** the one part that sticks out. It's what you'd turn to wake him, and it's what the player sees turn when he's placed and charges.
- **Nothing is hidden:** every part is the real part, just folded, so it matches his look in the world.

## Fun Score (1-10)
8. A folded robot with his eyes shut is instantly readable and a bit endearing, and it sets up the unfold when you place him.

## Builder Notes
- Build it as a **pose**, not new geometry: a one-frame `hold` clip named `packed` in trade-bot.gen.js, so the item renderer can play it like any other clip.
  - Lid positions at the shut value (−2).
  - Iris glow, lamp glow and station glow scaled to 0.
  - The held package hidden.
- **Check for clipping** with the generator's self-collision report (`ARMCLIP=1`). Folded arms against the side panels and pods against the chassis must not cut into each other; sub-pixel grazes are acceptable, as before.
- **Height target:** the top of his folded head at or below the post top (15 px above the block's bottom), so the item reads as a block.
- **Item display:** the GUI uses the standard block three-quarter view (30, 225, 0, scale about 0.625; adjust once the pose's real height is known). On the ground, in hand and in frames it's like a block.
- **Screenshot for review:** the inventory three-quarter view at 16 × 16 and enlarged, plus front and side views.
