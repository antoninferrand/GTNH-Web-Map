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

## Extrusion hatches: 31785–31788

`MTEHatchExtrusion` inherits `MTEHatchInputBus` without overriding texture
methods. Registry names confirm tiers 5–8. Reuse its existing input pipe
overlay, tier hulls and multi-block casing behavior. As for compressed input
buses, paint/indicator layers depend on shared renderer support.

Validation: the four IDs were absent and are now uniquely defined; they use
the already validated existing input overlay and hull assets.

## Follow-up: input bus paint stripes

Add a source-ordered `outputpaint1`–`outputpaint16` texture selection to the new
compressed input and extrusion records. Unpainted `OVERLAY_PIPE_COLOR_NONE` is
optional and absent in the installed JAR, so index 0 adds no layer. Every one
of the sixteen real stripe PNGs exists. This uses the shared renderer's new
painted output-layer contract and resolves the paint limitation recorded above.
Client-configurable ITEM_IN/OUT_SIGN indicators follow the map's existing
non-indicator default and are not added unconditionally.

## CropsNH crop managers: 28001–28012

`MTECropManager.getTexture()` ignores facing and active state: both vertical
faces use the BOXES overlay and all four horizontal faces use CROP. Its
`getTextureSet()` supplies tier-colored MACHINE_CASINGS. Register all tiers
1–12 with a local hull namespace and explicit `resourcemod=gregtech` for hull
assets, since CropsNH's renderdata has a separate file-local texture table.
This requires the shared resource-owner metadata change.

The existing cropsnh file receives its own GregTech support loader registration
(the loader registry is file-local). All new asset paths were checked against
installed JAR entries and all 12 IDs against the runtime registry.

## CropsNH seed_generator: 28013–28024

The matching CropsNH 2.0.140 MTE constructor supplies the normal/active
seed_generator front with a separate glow layer and optional scanner side/top/bottom
layers to MTEBasicMachine. Preserve process front (`mMainFacing`) and pipe
output (`mFacing`), tiers 1–12, and normal/active texture sets.
Use `front2`/face2 glow layers (modifier 23000) under the shared layered-icon
contract. Scanner side/top/bottom images are absent in the installed GT JAR
and source-declared optional; retain optional declarations for resource packs.

All four front PNGs exist in the installed CropsNH JAR. Animated strips are
read as square frames using their source PNG dimensions. New MTE IDs are
unique, local texture IDs resolve, and resource owners are explicit.

## CropsNH crop_breeder: 28025–28036

The matching CropsNH 2.0.140 MTE constructor supplies the normal/active
crop_breeder front with a separate glow layer and optional scanner side/top/bottom
layers to MTEBasicMachine. Preserve process front (`mMainFacing`) and pipe
output (`mFacing`), tiers 1–12, and normal/active texture sets.
Use `front2`/face2 glow layers (modifier 23000) under the shared layered-icon
contract. Scanner side/top/bottom images are absent in the installed GT JAR
and source-declared optional; retain optional declarations for resource packs.

All four front PNGs exist in the installed CropsNH JAR. Animated strips are
read as square frames using their source PNG dimensions. New MTE IDs are
unique, local texture IDs resolve, and resource owners are explicit.

## CropsNH gene_extractor: 28037–28045

The matching CropsNH 2.0.140 MTE constructor supplies the normal/active
gene_extractor front with a separate glow layer and optional scanner side/top/bottom
layers to MTEBasicMachine. Preserve process front (`mMainFacing`) and pipe
output (`mFacing`), tiers 4–12, and normal/active texture sets.
Use `front2`/face2 glow layers (modifier 23000) under the shared layered-icon
contract. Scanner side/top/bottom images are absent in the installed GT JAR
and source-declared optional; retain optional declarations for resource packs.

All four front PNGs exist in the installed CropsNH JAR. Animated strips are
read as square frames using their source PNG dimensions. New MTE IDs are
unique, local texture IDs resolve, and resource owners are explicit.

## CropsNH crop_synthesizer: 28046–28054

The matching CropsNH 2.0.140 MTE constructor supplies the normal/active
crop_synthesizer front with a separate glow layer and optional scanner side/top/bottom
layers to MTEBasicMachine. Preserve process front (`mMainFacing`) and pipe
output (`mFacing`), tiers 4–12, and normal/active texture sets.
Use `front2`/face2 glow layers (modifier 23000) under the shared layered-icon
contract. Scanner side/top/bottom images are absent in the installed GT JAR
and source-declared optional; retain optional declarations for resource packs.

All four front PNGs exist in the installed CropsNH JAR. Animated strips are
read as square frames using their source PNG dimensions. New MTE IDs are
unique, local texture IDs resolve, and resource owners are explicit.

## CropsNH industrial farm: 28055

`MTEIndustrialFarm.getTexture()` uses `createTextureWithCasing()` with assembly
line normal/active and glow overlays on `mFacing`. `getCasingTexture()` uses
CropsNH casing page 20 index 0 (`brickedAgriculturalCasing`). Add the controller
and register that casing page for surrounding generic hatches. All four overlay
assets and the casing PNG exist in the installed pack; MTE ID is unique.
