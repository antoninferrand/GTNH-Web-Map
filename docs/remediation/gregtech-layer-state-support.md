# GregTech layered and saved-state textures

Shared machine support now accepts up to four layers per face (`front`,
`front2` through `front4`, likewise bottom/top/side/output). Existing one-layer
records retain their behavior. `outputpaint0` through `outputpaint16` append the
matching stored `mColor` stripe to the output face. Precolored unmodified images
remain unmodified; tint/glow modifiers are preserved in each texture index.

Named iconsets are resolved lazily, allowing MTE declarations to precede their
iconset in the same file or another mod-scoped support file.

MTE records may declare `statefield=<saved field>`,
`stateNbasetex=<texture>`, `stateNiconset=<iconset>`, and
`stateNactiveset=<iconset>`. A separate `casingstatefield` with
`casingstateNbasetex` supports an independent casing tier. Missing/unrecognized
state values retain the normal declared texture. These fields are automatically
included in the renderer's requested chunk NBT; server/client live objects are
not accessed by rendering threads.

`inheritcasing=false` retains a hatch's fixed declared casing rather than
borrowing a multiblock casing. `paint=false` disables stored paint selection for
fixed-color machines. `downfacingextra=<direction ordinal>` on an iconset covers
the inherited tiered-chest behavior that draws its output texture on WEST in
addition to DOWN when the stored front faces down.

The renderer accepts numeric saved field types, defaults invalid/missing facing
safely, bounds-checks casing pages/indexes and neighboring metadata, and supports
both the current separate integer casing page/index and legacy signed index.
A packed integer index can be decoded when no separate page is present.

`tools/test-gregtech-renderer.py` compiles the actual production support parser
and renderer against the mod compile/runtime classpaths and checks original
face mappings, layer order/glow, paint indexes, late binding, active variants,
state/casing overrides and NBT requests, all facings, current/legacy/invalid
casing indexes, fixed casings and the down-facing chest exception. It requires
existing compiled dependency classes; it does not boot or modify a world.
