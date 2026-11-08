# Independent layouts of one source image

The real full-pack parser stopped in WitchingGadgets support when a new 2×2 pump atlas reused `assets/thaumcraft/textures/blocks/alchemyblock.png`, already declared as a 1×1 block image. The old filename/processor cache returned the one-tile entry, so tile index 1 aborted the rest of that support file.

Dynamic file decoding now has its own identity: filename, X/Y grid dimensions, format, custom tile-coordinate definitions, processor instance and processor output count. Equal layouts reuse their entry. Different grids, formats, custom rectangles or processor instances have separate tile indices. Custom layout argument order does not affect identity. The semantic cache is cleared on reset.

The separate filename-only lookup retains the first nonprocessor declaration. Vanilla resource-pack images and `findDynamicTile` therefore keep their canonical full-image meaning when a later atlas shares a filename. Filename-only lookup is also marked as an explicit consumer for inactive-format classification. Existing explicit owner/fallback/optional update behavior is preserved for same-layout reuse; this fix does not redesign resource ownership policy. Those options remain independent between different layouts.

`tools/test-texture-file-layouts.py` compiles the production parser and image loader. A real bundled grass PNG is declared as both 1×1 and 2×2; all five block mappings parse successfully, full-image pixels stay exact, and each source quadrant produces the expected integer-scaled pixels. The fixture also verifies same-layout identity, canonical filename lookup, grid/format/custom-coordinate/processor separation, custom argument order, resource-option isolation and reset. All 1,302 assertions pass. The existing complete vanilla RP/legacy pixel fixture also passes all 2,343 assertions.

The portable regression runner includes this as its 25th harness. Full-pack startup must still verify the original Thaumcraft/WitchingGadgets resource declarations together.
