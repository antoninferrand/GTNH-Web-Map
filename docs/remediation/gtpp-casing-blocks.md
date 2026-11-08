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
source-proven machines listed in `legacy-controller-casing-source.csv` (13
records covering eight IDs across the two historical mapping files). This
preserves face-specific tree-farm/algae/chisel/duplicator casing designs and
avoids forcing their obsolete standalone PNG aliases. The power substation
uses casing-2 metadata 7 on its sides and metadata 8 on its front/back, matching
`MTEPowerSubStation.getTexture`. Its existing energy-mode back overlay remains
a separate parity concern; these records correct the underlying casing.

The shared renderer supports deferred `baseblock`/`basemeta`, with optional
`frontbaseblock`/`frontbasemeta` and `backbaseblock`/`backbasemeta`. It resolves
the effective face map at render time so resource-pack overrides remain in
effect. Focused production JVM fixtures pass 124 shared/coke assertions and
6,760 vending assertions, including all six front/back orientations.
