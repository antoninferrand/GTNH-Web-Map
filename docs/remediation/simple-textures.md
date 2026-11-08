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
