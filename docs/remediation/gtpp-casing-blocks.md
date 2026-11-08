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
