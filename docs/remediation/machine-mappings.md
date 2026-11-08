# Missing machine mapping evidence

Target sources: GT5-Unofficial 5.09.54.265 and CropsNH 2.0.140. Registry IDs
were checked against the installed server's `gwm-gt-dump.txt`; texture paths
were checked against the installed JAR asset inventory. These data changes
require the shared renderer/resource follow-up fixes noted below before full
paint/glow parity is claimed. No live-server visual validation was performed.

## Compressed and quantum buses: 2743–2758

`LoaderMetaTileEntities.registerCompressedBus()` registers these as LuV through
UXV, tiers 6–13, despite the `.tier.00`–`.tier.03` names. Both compressed bus
classes inherit their texture behavior from normal input/output buses and
`MTEHatch`. Reuse the existing hatch input/output pipe overlays and dynamic
multi-block casing lookup, with the correct tier hull when unassembled.

All 16 IDs are newly mapped, with no duplicate MTE records. Referenced hulls
and pipe texture definitions already exist and their PNGs exist in the installed
GregTech JAR. Input bus paint stripes and optional indicator layers need the
shared multi-layer hatch renderer work; these records establish their correct
base appearance rather than claiming that missing feature is solved.

## Pattern providers: 2719–2723

`MTEHatchPatternProvider.getTexturesActive/Inactive()` applies the same
`OVERLAY_HATCH_PATTERN_PROVIDER` on the hatch facing in both states. The loader
registers IV through UHV, tiers 5–9. Define its icon using `output`, which the
map renderer orients with `mFacing` on all six directions; `front` would use
the unrelated `mMainFacing`. Preserve dynamic multi-block casing selection.

Validation: all five IDs are unique; the exact overlay PNG is present in the
installed GregTech asset inventory, and tiers reuse validated hull definitions.
