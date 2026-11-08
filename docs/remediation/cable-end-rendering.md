# Insulated cable cross-sections

GT5 5.09.54.265 `MTECable.getTexture()` uses `INSULATION_FULL` for side
faces, but connected/end faces layer the material's WIRE icon beneath a
diameter-specific insulation ring. The material RGB belongs to the wire;
insulation uses `Dyes.CABLE_INSULATION` (default `404040`) or its paint color.
Painting must not recolor the wire layer.

`BaseMetaPipeEntity.getTextureUncovered()` makes an isolated cable open on
all six faces and treats a single connection as open on both ends of that
axis. The new helper follows those source rules, not just the raw direction
mask. Its per-face patch indices match Forge direction ordinals.

`GregTechCableRenderer` prebuilds nine supported diameters times 64 masks at
initialization. Meshes form a center box plus disjoint connection arms,
omitting interior faces. This fixes end texture selection without changing
other pipe families or the common isolated-pipe geometry fix. Full-width
cables retain six block faces rather than creating zero-width arms.

Integration is opt-in: a cable record declares `wiretex`, `wirecolor` and
`endtex` in addition to its existing insulation `basetex` and `colormul`.
The shared machine renderer calls:

```java
cables.getRenderData(thickness, connections, insulationTexture, wireTexture,
    endTexture, insulationColor, wireColor);
```

Create the helper once with the renderer's `RenderPatchFactory`. Apply the
saved paint color to `insulationColor` before the call; pass the material's
unchanged `wireColor`. Returned `CustomRendererData` supplies distinct
per-layer color multipliers. Other cable records keep existing behavior
until their material and end textures have been verified.

Validation: `python3 tools/test_cable_rendering.py` compiles the production
helper and real renderer interfaces under Java 8 compatibility. Nine diameters
times all 64 masks pass 167,535 assertions covering surface bounds/area,
outward normals, absence of internal faces, isolated/single/multiple-end
selection, immutable mesh reuse and insulation-only paint modulation.
This is geometry/state validation, not a live screenshot comparison.

The isolated full-pack startup exposed an important loader-order constraint:
wire/end texture IDs are resolved while processing each MTE line. Declare
those textures before the cable MTE records; an unresolved forward reference
throws a format error and aborts the remainder of that support file. The cable
data regression check now enforces this ordering.
