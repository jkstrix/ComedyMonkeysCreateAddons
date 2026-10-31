# OBS-01: Helper Bot looks see-through where a chest is behind him

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Rendering and Flywheel |
| **Area** | Reported in play |
| **Confidence** | medium |

## Where to look

`src/main/java/com/comedymonkey/createaddons/client/helperbot/HelperBotInstances.java:84-90` (Flywheel material), `src/main/java/com/comedymonkey/createaddons/client/helperbot/HelperBotClient.java:431-447` (`registerRenderBuffers`, `endBatches` at `AFTER_BLOCK_ENTITIES`), `src/main/java/com/comedymonkey/createaddons/client/helperbot/HelperBotModels.java:92-94` (`entityCutoutNoCull`), `src/main/java/com/comedymonkey/createaddons/client/ComedyMonkeysCreateAddonsClient.java:65-66` (the two hooks are registered)

## What is wrong

Reported by the maintainer during this review: the whole bot blends with a chest standing behind him, so both the chest and the bot can be seen through each other, and only where the chest is behind him. That look means the bot's pixels are being alpha-blended over the chest instead of replacing it. Reading the code did not find the cause, so this is a confirmed observation with an unconfirmed cause. What was ruled out from the code and the jars: the Flywheel material is opaque with depth writes (`Transparency.OPAQUE`, `WriteMask.COLOR_DEPTH`, `DepthTest.LEQUAL` are the `SimpleMaterial` defaults and the bot only sets cutout, no backface culling, no mipmap, entity lighting); the Flywheel path draws after vanilla block entities (Flywheel injects at the `destroyProgress` profiler mark, after the chest sheet is flushed) with depth testing; all 18 bot textures have a binary alpha channel (every pixel is 0 or 255, measured), so no texel is translucent; the fallback renderer uses `entityCutoutNoCull`, which does not blend; the dev client has shaders disabled (`run/config/iris.properties` has `enableShaders=false`) and graphics on Fancy, so neither Iris nor the Fabulous transparency chain is involved there. Two things in the bot's render path are unusual and are the first suspects: the fallback renderer registers 17 private render buffers (`RegisterRenderBuffersEvent`) and flushes them itself at `RenderLevelStageEvent.AFTER_BLOCK_ENTITIES`, and with Flywheel on the bot is the only instanced object drawn with a non-atlas texture and `backfaceCulling(false)`.

## How to see it happen

Place a Helper Bot about one block in front of a chest (vanilla or Upgraded), look at him from the far side of the room so the chest is directly behind his body. Expected: he hides the chest. Seen: the chest shows through his whole body, and he shows through the chest.

## How to fix it

Diagnose first, then apply the fix for the branch that reproduces. Each step takes under a minute in the dev client.
  1. With the bot and chest set up, run `/flywheel backend off`. If the see-through disappears, the fault is in the Flywheel path; if it stays, it is in the fallback renderer path (go to step 3).
  2. Flywheel path: in `HelperBotInstances.build(...)`, temporarily add `.backfaceCulling(true)` and, separately, replace `.cutout(CutoutShaders.ONE_TENTH)` with the default (no cutout) to see which setting changes the look. Also try `/flywheel backend instancing` versus `indirect`: if only one backend shows it, report it to Flywheel with the material settings. If `backfaceCulling(false)` is the trigger, split the bot's model so only the parts that need two-sided faces (thin plates) use a second material with culling off, and keep culling on for the body.
  3. Fallback path: in `ComedyMonkeysCreateAddonsClient` comment out the two lines that register `HelperBotClient::registerRenderBuffers` and `HelperBotClient::endBatches`. The bot then draws through vanilla's shared `entityCutoutNoCull` batch and is flushed by `LevelRenderer` with the other block entities. If that fixes it, keep it that way: the private buffers only exist so the bot does not end another batch mid-draw, which is a micro-optimisation, and vanilla's own batching is what Iris and Sodium expect. If the bot then vanishes behind water or glass (the reason the hook was added), flush at `AFTER_ENTITIES` instead of `AFTER_BLOCK_ENTITIES`, which is before the `entityEffect`/outline post-process runs.
  4. If the report came from a play instance with shaders on: Flywheel turns itself off under a shaderpack (`Backends` checks `IrisCompat.isShaderPackInUse`), so step 3 applies, and in addition Iris routes batches it flushes itself through the shaderpack's entity program; a batch flushed from a `RenderLevelStageEvent` outside Iris's entity phase can be drawn with the pack's translucent blending. Removing the private buffers (step 3) is the fix there too.
  5. Whichever branch it was, add a screenshot test to `HelperBotRenderGameTests` (there is already an A/B Flywheel on/off screenshot runner in `client/dev/ShotRunner`) that renders a bot in front of a chest and compares the centre pixel to the bot's body colour, so the regression is caught.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Update: likely cause (Veil coloured light), not yet reproduced on screen

The maintainer added detail: what showed through the chest and the bot was **tall grass** behind them, it was **dark**, and a **coloured Electric Light** was nearby. That points away from the bot's own rendering and at Veil's coloured light, which `compat/veil/LightGlow` turns on when Veil is installed (it comes with Sable) and Iris and Colorful Lighting are not.

How Veil lights a pixel (from `assets/veil/pinwheel/shaders/program/light/point.fsh` in the Veil 4.3.2 jar bundled with Sable 2.0.6):

```glsl
vec4 albedoColor = texture(AlbedoSampler, screenUv);
if (albedoColor.a == 0) discard;
float depth = texture(DepthSampler, screenUv).r;
...
fragColor = vec4(albedoColor.rgb * diffuseColor * (1.0 - reflectivity) + diffuseColor * reflectivity, 1.0);
```

The light is added on top of the frame, using the surface colour from Veil's own **albedo buffer** and the position from the depth buffer. Only things drawn through shaders Veil has patched write into that albedo buffer. Terrain (tall grass included) does. Anything drawn through a shader Veil doesn't patch (Flywheel's instance shaders, which draw the bot with Flywheel on, and possibly some block entity renderers) writes depth and colour to the screen but leaves the albedo buffer holding whatever was behind it. Veil then adds "tall grass × light colour" on top of the bot's and the chest's pixels. In the dark, where that added light is most of what you see, the grass pattern looks like it shows through both of them. Where the sky is behind, the albedo is empty and Veil discards the pixel, which is why it only happens with something behind.

What this predicts, for checking against the screenshots:
- It happens only with Veil coloured light on (`lightColouredLight = true` in `comedymonkeysaddons-client.toml`) and only within reach of a lit Electric Light. Turning that option off makes it go away.
- It affects every Flywheel-instanced thing of this mod (machines' moving parts, posts, the bot), and Create's own instanced parts, not just the bot. It is most visible on dark objects with a textured, brighter surface behind them.
- `/flywheel backend off` may make the bot itself correct (the fallback renderer uses vanilla's `entityCutoutNoCull` shader, which Veil may patch) while the chest's behaviour depends on how it is drawn.

Possible fixes, to choose once the screenshots confirm it:
1. Have the light pass ignore pixels whose albedo is stale. The mod already ships a built-in resource pack over Veil's light setup (`resourcepacks/veil_light_depth`); it could also override `point.fsh` to skip pixels where the albedo buffer's depth (if Veil keeps one) doesn't match the screen depth. Needs checking what Veil's dynamic buffers expose.
2. Make Flywheel's instance shaders write Veil's albedo and normal buffers. Veil has an API for injecting its dynamic-buffer outputs into shaders; whether it can reach Flywheel's compiled pipeline needs research.
3. If neither is practical, limit the coloured light so it can't cause this, for example draw it only onto terrain-backed pixels whose depth matches (same as 1), or document it as a known Veil limitation.

The repro attempt in the dev client (flat world, `/lights testsetup` street at midnight, bot, chest and tall grass beside a dyed lamp) didn't show it clearly because the lamps next to the scene were dim small lights. A brighter dyed lamp (a Floodlight, High Bay or Lantern Head) right beside the scene should.

## Status (2026-10-31)

**open.** Likely Veil's coloured light lighting Flywheel-drawn pixels with the albedo of what is behind them; waiting for the maintainer's screenshots before choosing a fix.
