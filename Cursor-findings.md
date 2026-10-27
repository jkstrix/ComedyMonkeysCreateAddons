# Cursor findings

Reviewed: the uncommitted Power Lines rework, against [power-lines-review-fixes.md](design/plans/power-lines-review-fixes.md).

Issues already written up there are left out. That includes the cut that stays joined (item 16), paper draws not being saved (item 2), the per-tick record and `supplyTo` cost (item 7), leftover energy dumped into the first paper store (item 15), Wellspring income counted once per generator (item 13), and fittings saved by ordinal (item 20).

Two gaps are not in that plan.

## 1. Paper income is never taken out of the Wellspring (high)

- **Where:** `FluidLines.supplyTo()`, `PowerGrid.Paper.advance()`, `LavaGeneratorBlockEntity.resume()`, `GeothermalControllerBlockEntity.resume()`.
- **Not item 13.** Item 13 is the rate: every generator on the line is credited the Wellspring's whole output, ignoring the other ends, Valves, Filters and Splitters. Fixing that share still leaves this.
- **Problem:** the credited fluid is added to the generator's record and, on resume, written into its tank. The Wellspring's tank is not reduced by that amount. A Wellspring is a finite tank: it generates only while it has room, and a Fluid Line only moves what it pulls out. While the generator's chunk is not running, the line is not pulling, so the Wellspring keeps the fluid and the generator receives a copy of it. One generator, given a fair share of the real delivery, still comes back with lava or water the Wellspring never lost.
- **Spec:** D19 says the fuel source is on paper too, not only the generator.

## 2. The unloaded-chunk write-up contradicts the spec (medium)

- **Where:** `design/specs/fluid-and-power-lines.md`, the paragraph under the unloaded-chunk table (the table that was revised for generators on paper).
- **Problem:** that paragraph still says nothing is lost, and that a generator with nowhere to send its power stops burning. The table above it says the opposite: with the motor unloaded the generator's lava still falls, because it runs flat out. D14 says the same, and surplus energy is lost on purpose. The paragraph also describes the record as `FarGenerators`. The record in the code is `PowerGrid`.
- **Not item 19.** Item 19 is tooltips, JEI, ponders and goggles. It does not name this paragraph.
