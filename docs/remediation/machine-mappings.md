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

## BartWorks acid generator: 12742 and existing family corrections

`ItemRegistry` registers IV generator 12742 at tier 5. It was missing entirely.
The same source confirms the existing LV/MV/HV/EV IDs 12793/12726/12727/12728
are tiers 1/2/3/4; several old records incorrectly used tier 1 for every tier.
`MTEAcidGenerator` uses acid-hazard side/back/bottom, chemical reactor front
images on its top (including active and glow), and voltage-specific
`OVERLAYS_ENERGY_OUT[tier+1]` on `mFacing`. New iconsets represent those exact
faces and fix active-output omission in older records. All required PNGs
exist in the installed GregTech JAR; the absent inactive top glow is source-declared optional. The mod JAR, which now includes BartWorks.

## BartWorks creative scanner: 12800

`ItemRegistry` instantiates the creative scanner at tier 14 (MAX). It inherits
`MTEScanner` textures, including the normal/active scanner process front and
separate glow images, plus pipe-output facing. Side/top/bottom scanner overlays
are source-optional and absent in the installed JAR. Preserve their optional
declarations so resource packs can still supply them. All four required front
images and the MAX hull images exist in the installed GregTech JAR.

## KubaTech electrode and detector hatches: 14203–14204

Both constructors use IV hulls. Electrode hatch inherits MTEHatchInputBus, so
its front uses PIPE_IN and source-ordered paint stripes. The detector uses
HEAT_SENSOR plus its glow only while active. Both retain generic dynamic
multiblock casing selection. All required PNGs, including the 16 stripes and
heat sensor glow, exist in the installed GregTech JAR.

Append to the existing KubaTech support file, reuse its loader registration,
and explicitly mark the GregTech resource owner. Local IV hull/iconset records
avoid depending on other renderdata files' load order.

### Acid generator duplicate-owner cleanup

The source audit also found legacy dreamcraft MTE records for four already
mapped acid generators. They could overwrite corrected GregTech records based
on file load order. Remove those duplicate MTE records; GregTech now owns the
whole family. Preserve unrelated legacy texture/icon definitions for separate
reachability review. This prevents a corrected record from silently reverting.

## Mass solidifier: 368

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings10:13`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_MASS_SOLIDIFIER`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## PCB bio chamber: 730

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings4:1`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_PURIFICATION_PLANT`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## PCB cooling tower: 731

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings8:12`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_PURIFICATION_PLANT`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Spinmatron: 1033

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings12:9`.
Front source: `assets/gregtech/textures/blocks/iconsets/TFFT`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Exo foundry controller: 14050

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.foundrycasings:0`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_EXOFOUNDRY`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

Exo foundry additional rendering remains separate from its controller cube:
`MTEExoFoundry.renderTESR()` loads `textures/model/foundry_ring.obj` and
compiles ring VBOs. The controller texture mapping does not reproduce that
world-spanning shader geometry; it must remain tracked as a visual parity item.

## Mega chemical reactor: 15515

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings8:0`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_MEGA_CHEMICAL_REACTOR`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Mega distillation tower: 15516

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings14:5`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_MEGA_DISTILLATION_TOWER`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Exothermic hearth: 15517

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings14:3`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_HEARTH`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Endothermic fridge: 15518

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings14:4`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_FRIDGE`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Mega oil cracker: 15569

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings14:5`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_MEGA_OIL_CRACKER`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Latex collector: 15752

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings8:0`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_MULTI_LATEX`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Ore washing plant: 15550

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `assets/miscutils/textures/blocks/TileEntities/MACHINE_CASING_STABLE_GRISIUM.png|assets/miscutils/textures/blocks/TileEntities/MACHINE_CASING_STABLE_GRISIUM_TOP.png`.
Front source: `assets/miscutils/textures/blocks/iconsets/controllerFaces/industrialWashPlant`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Industrial chemical bath: 15551

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `assets/miscutils/textures/blocks/TileEntities/MACHINE_CASING_STABLE_GRISIUM.png|assets/miscutils/textures/blocks/TileEntities/MACHINE_CASING_STABLE_GRISIUM_TOP.png`.
Front source: `assets/miscutils/textures/blocks/iconsets/controllerFaces/industrialWashPlant`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## Industrial forming press: 15552

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `assets/gregtech/textures/blocks/iconsets/MACHINE_CASING_STABLE_TITANIUM.png`.
Front source: `assets/miscutils/textures/blocks/iconsets/controllerFaces/industrialPlatePress`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

## name_remover: 12070

ULV NameRemover reuses disassembler textures. Source constructor registrations and texture methods were checked in
GT5 5.09.54.265. All required images exist; optional missing images match
explicit createOptional declarations. Animated frames use square source-image
strips. Special metadata requires the shared renderer contract.

## drawer_framer: 32050, 32051, 32052

LV/MV/HV DrawerFramer uses static side/top/bottom with active front/glow. Source constructor registrations and texture methods were checked in
GT5 5.09.54.265. All required images exist; optional missing images match
explicit createOptional declarations. Animated frames use square source-image
strips. Special metadata requires the shared renderer contract.

## modification_table: 12071

ModificationTable ignores facing/state/tier and uses bare side/top/bottom, with no paint. Source constructor registrations and texture methods were checked in
GT5 5.09.54.265. All required images exist; optional missing images match
explicit createOptional declarations. Animated frames use square source-image
strips. Special metadata requires the shared renderer contract.

## infinite_chest: 31010

InfiniteItemHolder inherits LV TieredChest and QCHEST facing overlay; DOWN-facing chest also puts QCHEST on WEST, via downfacingextra=4. Source constructor registrations and texture methods were checked in
GT5 5.09.54.265. All required images exist; optional missing images match
explicit createOptional declarations. Animated frames use square source-image
strips. Special metadata requires the shared renderer contract.

## Large neutralization engine: 31088

Source class texture method selects the declared casing and normal/active
front plus independent glow layer on `mFacing`. Casing source: `gt.blockcasings12:5`.
Front source: `assets/gregtech/textures/blocks/iconsets/OVERLAY_FRONT_LNE`. All required PNGs exist in the installed JAR
inventory; frame counts are taken from matching source PNG dimensions.
Base faces are preserved individually, rather than flattening top/bottom
casing textures. Absent source-optional glows remain optional for resource packs.

LNE source saves `structureTier` as a byte. Its casing selector uses Precise
Stationary for state 2, Ultimate Static for state 3 and Strengthened Inanimate
for all other values. Declare the exact state override instead of fixing all
structures to their tier-1 casing. Shared renderer clears the default base set
when applying a selected state base texture.
