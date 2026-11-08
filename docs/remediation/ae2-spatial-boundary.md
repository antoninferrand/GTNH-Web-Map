# AE2 invisible spatial boundary

AE2 rv3-beta-1095-GTNH `BlockMatrixFrame.getRenderer()` explicitly returns
`RenderNull`. Its spatial boundary is intentionally invisible, although it has
collision behavior. The previous mapping incorrectly required the nonexistent
`BlockMatrixFrame.png` and attempted a textured cube.

Map its faces to the existing blank texture index(-1) and mark the block
transparent. TexturePack parses -1 as TILEINDEX_BLANK and readColor returns
transparent for negative indices. The historical image declaration can remain
unused for compatibility: no block or support recipe now references it.

Validation: matching installed-version source and production blank-index path
reviewed; the complete Java build passes. Visual runtime fixture still required.
