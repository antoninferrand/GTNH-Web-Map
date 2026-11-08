# Texture path corrections

Baseline: GTNH 2.9.0, GregTech 5.09.54.265. Each correction keeps the
existing texture ID, references, dimensions and processing unchanged. No Java
render behavior is changed.

## GT++ active texture filenames

Correct `adv_machine_matterfab_ACTIVE.png`, `machine_top_dieselmotor_ACTIVE.png`
and `machine_top_dieselmotor2_ACTIVE.png` to their lowercase `_active.png`
filenames in both GregTech and dreamcraft support definitions.

Evidence: the installed GregTech JAR asset inventory contains all three target
paths and none of the original paths. The matching GregTech source tag's
`gtPlusPlus/xmod/gregtech/common/blocks/textures/TexturesGtBlock.java` declares
these exact lowercase active paths (lines 163, 165 and 500). This resolves three
baseline unique resources used from two support files.

Validation: checked all targets against the installed asset inventory, kept
texture identifiers and animation dimensions unchanged, and ran `git diff
--check`. Live visual validation is pending fixture rendering.

## TecTech colored overlay filenames

Correct `OVERLAY_GREEN.png` and `OVERLAY_RED.png` to `overlay_green.png` and
`overlay_red.png` in the GregTech and dreamcraft support definitions. The
installed GregTech 5.09.54.265 JAR contains the lowercase files in the same
TecTech resource directory, and contains neither uppercase file. The existing
support IDs and all consumers remain unchanged. These are source-image path
corrections, not a substitution of similarly named GUI assets.

Validation: exact target paths exist in the installed JAR inventory; all four
changed declarations preserve IDs and texture parameters; `git diff --check`
passes. Machine state/geometry validation remains part of the fixture audit.

## OpenModularTurrets inventory expander

Use the packaged `ExpanderInvTierOne.png` image instead of the absent lowercase
`expanderInvTierOne.png`. The installed OpenModularTurrets 2.4.8 JAR contains
only the uppercase initial filename. The matching 2.4.8 source's
`BlockExpanderInvTierOne.registerBlockIcons` registers the lowercase name, so
this corrects a source/packaging mismatch rather than copying that mismatch
into Web Map. Preserve the block's registry name and existing texture ID.

Validation: replacement path verified in the installed JAR inventory and the
matching source checkout; `git diff --check` passes.

## Existing Ginkgo corrections

Fork commit `e0cb1f30` already corrected `gingkoBark`, `gingkoTrunk` and
`Gingko` planks to `ginkgoBark`, `ginkgoTrunk` and `Ginkgo`. All three corrected
paths exist in the installed BinnieCore asset inventory. No duplicate fix is
needed for those baseline resources.

## Mass solidifier casing names

Point the old `MACHINE_CASING_MS160` and `RADIATOR_MS160` texture IDs at
`MASS_SOLIDIFIER_CASING.png` and `MASS_SOLIDIFIER_RADIATOR_CASING.png`.
GregTech 5.09.54.265 `BlockCasings10.getIcon` explicitly selects those new
icons for metadata 13 and 14, while the corresponding casing texture indices
remain page 16, entries 61 and 62. `Textures.BlockIcons` declares both new
filenames and the installed JAR contains them. Correct the matching
dreamcraft declaration too, retaining support IDs and consumers.

Validation: both new assets exist and both original names are absent in the
installed asset inventory; the source block metadata/index correspondence was
checked; `git diff --check` passes. This corrects casing images, not the
separate missing machine-ID coverage.

## Deprecated BartWorks glass images

Keep the deprecated `BW_GlasBlocks` block's metadata mappings but update its
missing image sources. In the matching GregTech/BartWorks source,
`bartworks/common/loaders/ItemRegistry.java` declares `bw_deprecatedglass`
metadata 3 as `RhodiumPlatedPalladiumReinforcedBoronSilicateGlassBlock` and
metadata 14 as `InfinityReinforcedBoronSilicateGlassBlock`. Use those images
for the existing `LuVTierMaterial...` and `CosmicNeutronium...` texture IDs,
respectively. Preserve the connected-glass processor and patch selections.

Validation: verified metadata positions in the source registry array, both
new PNGs in the installed JAR inventory, and absence of both original PNGs.
`git diff --check` passes. Coverage for the newer `BW_TieredGlass` and
`BW_ExtraGlass` block registrations is a separate registry-audit concern.
