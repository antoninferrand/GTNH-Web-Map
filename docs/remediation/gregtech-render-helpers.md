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

## TecTech helper bases

`tectech.thing.block.BlockEOHRender` and `BlockForgeOfGods` have the same
explicit source contract: `registerBlockIcons` does nothing (commented as
TESR-only), `renderAsNormalBlock=false`, render type -1, no collision bounding
box, and no drops. Their registered names are `Eye of Harmony Renderer` and
`ForgeOfGodsRenderBlock`. TecTech `ClientProxy` binds their separate
`EOHTileEntitySR` and `RenderForgeOfGods` effects. Their Web Map base definitions
now use the same empty face map/zero-patch renderer; the last synthetic
`TRANSPARENT.png` declaration is removed from TecTech support.

This does not implement the active Eye of Harmony or Forge of Gods animation,
shader, skybox, or geometry far outside the helper block's owner cell. Those
remain explicitly unsupported client effects. The source supplies no required
block-atlas PNG to repair here. Candidate7's resource audit exposed this final
consumer after the other GregTech/GoodGenerator helper records were corrected;
a subsequent artifact must confirm zero required lookup failures.
