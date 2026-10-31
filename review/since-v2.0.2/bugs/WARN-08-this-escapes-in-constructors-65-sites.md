# WARN-08: 'this' escapes in constructors (65 sites)

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Compiler warnings |
| **Confidence** | high |

## Where to look

57 Block/BlockEntity/Menu/entity constructors (`registerDefaultState`, `addSlot`, navigation setup); client: `client/parts/PartsVisual.java:84`, `client/line/LinePostVisual.java:51,96`, `client/helperbot/HelperBotVisual.java:52`, `client/helperbot/HelperBotActorVisual.java:37`, `client/tradebot/TradeBotStationVisual.java:59`, `client/tradebot/TradeBotRenderer.java:28`, `client/juggernaut/JuggernautRenderer.java:27`

## What is wrong

Java 21's `this-escape` lint fires when a constructor calls an overridable method before subclasses finish constructing. For blocks and menus this is the normal vanilla pattern and harmless. The client visuals are worth a look: `PartsVisual`'s constructor calls `computePackedLight()` and `LinePostVisual`'s calls `layOut()`, both overridable, and `PartsVisual` is subclassed in the mod.

## How to see it happen

A subclass of `PartsVisual` overrides `computePackedLight()` using a field set in its own constructor. The superclass constructor calls it first, the field is still 0/null, and the visual starts with wrong light or an NPE.

## How to fix it

(a) For the visuals and renderers, make the methods the constructor calls `private` or `final` (`layOut()`, the light helper); that removes the warning and the risk. (b) For the vanilla-pattern blocks and menus, mark the class `final` where nothing extends it, or add `@SuppressWarnings("this-escape")` to the constructor.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** 57 classes nothing extends are now final; the two subclassed constructors carry a stated suppression.
