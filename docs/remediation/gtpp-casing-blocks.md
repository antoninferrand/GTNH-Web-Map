# GT++ casing blocks and source-backed faces

The dedicated fixture showed all metadata blank for the six registered GT++
casing blocks. `miscutils-texture.txt` had hatch-base declarations referencing
these blocks, but no block texture definitions. Their registered casing-page
sources therefore correctly identified blocks whose Web Map face maps were
still missing.

The new definitions follow GT5 5.09.54.265
`GregtechMetaCasingBlocks`, `GregtechMetaCasingBlocks2` through `6` `getIcon`
methods. Every face is recorded in `gtpp-casing-source.csv`: 96 metadata cases
(71 concrete designs and 25 explicitly diagnostic/unused cases whose getter
returns the missing icon), with separate top/bottom versus side images where
the source distinguishes them. The supplied diagnostic checker represents
those intentional rendering-error getters; it does not conceal an unknown
required asset.

All 73 texture declarations were checked against the installed JAR asset
inventory or the existing bundled diagnostic checker. Resource ownership is
explicitly `gregtech`, including `assets/miscutils` images now packaged in the
merged GregTech JAR. Animated image dimensions were read from source PNGs so
frame zero is sampled without stretching the whole vertical strip.

The maps are declared before the existing hatch-base registration records,
allowing both hatch inheritance and deferred controller block sources to use
the effective new block faces. Force-field glass preserves transparency. This
change adds actual block appearance rather than replacing obsolete aliases with
unrelated textures or dropping required-image warnings.

Controller records now reference the same registered casing blocks for the
source-proven machines listed in `legacy-controller-casing-source.csv` (47
records covering 29 IDs across the two historical mapping files). This
preserves face-specific tree-farm/algae/chisel/duplicator casing designs and
avoids forcing their obsolete standalone PNG aliases. The power substation
uses casing-2 metadata 7 on its sides and metadata 8 on its front/back, matching
`MTEPowerSubStation.getTexture`. Its energy-mode back overlay is handled by the separate saved-state correction
described below.

The shared renderer supports deferred `baseblock`/`basemeta`, with optional
`frontbaseblock`/`frontbasemeta` and `backbaseblock`/`backbasemeta`. It resolves
the effective face map at render time so resource-pack overrides remain in
effect. Focused production JVM fixtures pass 124 shared/coke assertions and
6,760 vending assertions, including all six front/back orientations.

The separate BlackGate correction follows `BlockWitherProof.getIcon`, which
returns `Textures.BlockIcons.CONTAINMENT_FRAME`; the removed `blockFrameGt.png`
reference was the block's only consumer. Its source tint remains `202020`, and
its translucent render pass is now represented by `SEMITRANSPARENT`. The current
containment-frame asset already has an existing declaration in this file.

The additional source ledger includes all active controller consumers of the
legacy chrono/metro casing aliases. Legacy flotation ID 31028 deliberately uses
the registered Inconel casing selected by its actual getter; current ID 15560
uses the explicit flotation block metadata 9, as its source intentionally avoids
the misleading casing registry slot. That block face map has been added using
the installed `MACHINE_CASING_FLOTATION` image. The production JVM fixtures
remain green (124 shared/coke assertions and 6,760 vending assertions).

Seven additional baseline paths belonged exclusively to 22 obsolete iconsets:
legacy structural tree-farm, GT++ crop-harvester, and GT4 redstone panel/display
sets. The structural/display-strength sets had no MTE consumers anywhere in
renderdata. The others were referenced only by IDs 31111–31118, 31800, 31801, and
31805, all absent from the exact installed 3,412-MTE registry dump. Those eleven
unregistered MTE records, 44 duplicate iconset declarations, and seven texture
records are retired together; `retired-unregistered-iconsets.csv` preserves each
removed record. An all-renderdata consumer scan verified that no registered MTE
references the retired names. This is installed-version retirement, rather than
turning a missing required image into a transparent optional layer.

Substation back-side overlay parity follows saved `mIsOutputtingPower`: input
and output select `OVERLAY_ENERGY_IN_MULTI_2A_IV` and
`OVERLAY_ENERGY_OUT_MULTI_2A_IV`. Both maximum voltage methods return 32768, so
`getInputTier`/`getOutputTier` select the IV entries in the source arrays. The
boolean is explicitly saved and loaded by `MTEPowerSubStation`; absent state
uses the source default input mode. The generic iconset `back` through `back4`
layers apply opposite `mFacing` on all six orientations while the disassembler
front continues following active state. Resource declarations retain their
source `createOptional` semantics, and both actual PNGs exist in the installed
JAR. Fixtures cover absent/byte/Boolean mode, both active states, all facings,
and layered back glow (257 shared/coke/substation assertions; 6,760 vending).

Deferred block faces convert Minecraft outward side ordinals to the ray-entered
`BlockStep` indexing used by `HDTextureMap`: `{1,4,2,5,0,3}`. This conversion
applies to controller bases, their front/back sources, and registered hatch
casings. The production fixture now invokes the actual private
`TexturePack.loadTextureFile` parser with six differently labelled `face0`
through `face5` images, then checks every controller orientation and hatch
selection. The original synthetic array fixture would not have caught this
convention mismatch; the corrected parser-based checks pass all 257 assertions.

These fixed controller casing textures are also explicitly `paint=false`:
their traced `getTexture`/`getCasingTexture` implementations do not consume the
requested paint color. Their source block images retain their own design and
the shared renderer no longer multiplies the entire fixed casing and overlay
by saved `mColor`. This uses the existing tested per-record opt-out.
