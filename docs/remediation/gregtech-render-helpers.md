# GregTech TESR helper base blocks

`gregtech.common.blocks.BlockRenderer` in GT5 5.09.54.265 intentionally registers
no block-atlas icon, returns `renderAsNormalBlock=false` and render type -1, and
has no collision geometry. `LoaderGTBlockFluid` registers this class for
`gt.wormholerenderer`, `gt.blackholerenderer`, and `gt.nanoforgerenderer`. Their
client appearances are bound separately in `GTClient` to `WormholeRenderer`,
`BlackholeRenderer`, and `NanoForgeRenderer` TESRs.

These three Web Map base blocks now use transparent empty face maps and the
existing empty-patch `InvisibleBlockRenderer`. The nonexistent
`iconsets/TRANSPARENT.png` declaration is removed; the source does not request
such a PNG. The obsolete `gt.dronerenderer` mapping is also removed: the exact
4,258-block runtime registry contains no such block, and current source does
not register the old `GregTechAPI.sDroneRender` field. Drone rendering moved to
its entity/center render path.

This is accurate **base-block invisibility**, not support for the three active
client TESR effects. Their animated/shader appearances, potentially far beyond
the helper block's own cell, remain an explicit visual limitation. No missing
required image is made optional or replaced with an unrelated blank PNG.

Validation: exact installed block registry confirms the three live helper
names and absence of the fourth; source icon/normal-render methods and TESR
bindings were checked. The renderer is the same zero-patch production class
already exercised by the GoodGenerator invisible-helper fixture. A later
full-pack render/audit must confirm no `TRANSPARENT.png` lookup remains.
