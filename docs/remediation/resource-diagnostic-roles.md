# Resource probes and vanilla terrain variants

The isolated fixture on candidate 7b797c4 resolved all 340 active vanilla resource-pack block images. Its 246 null-owner missing lookups were 224 legacy Minecraft 1.5 `textures/blocks/*` alternatives plus 22 optional color/animation features. The built WebMap artifact supplies the actual Minecraft 1.7 resource-pack images, so no Minecraft client download is needed. A rendered 128×128 fixture tile contained 5,120 opaque green grass pixels in 24 colors and 11,264 transparent pixels outside generated coverage.

Known constructor feature probes now use `TexturePackLoader.openOptionalTPResource` (single path and alternate-path overloads). This includes pack metadata, custom color properties, optional colormaps and custom animation images; CTM and normal texture resource calls retain their existing required behavior. A later probe cannot replace a required lookup or assign that path an optional role. A later actual declaration supersedes an earlier probe.

Vanilla registrations carry exact resource-pack/legacy provenance. Parsed texture consumers set an independent explicit-use flag. Only a built-in opposite-format variant with no explicit consumer gains the inactive role. A support mapping that explicitly consumes a legacy vanilla path continues to require it, even in resource-pack mode. Processor output reservation preserves the explicit-use flag as well as ordinary usage.

`TexturePack.getResourceRole(packName, path)` returns `INACTIVE_TERRAIN_VARIANT`, `OPTIONAL_PROBE`, `TEXTURE`, or `UNKNOWN_PACK`. Resource lookup outcomes remain available unchanged: the runtime audit should retain `rawOutcome`, classify an inactive variant's failed lookup separately, and exclude only that classified failure from its required-missing count. Optional probes use the existing `OPTIONAL_ABSENT` outcome. All missing active vanilla and custom null-owner textures retain `REQUIRED_MISSING`.

Validation: `python3 tools/tests/test_resource_roles.py` compiles the production loader, resolver and role methods. Its cases cover active/missing vanilla images, custom null-owner requirements, probe/declaration precedence in both orders, optional alternate streams, both vanilla terrain formats, explicit consumers of opposite-format files, and diagnostic role lookup. `python3 tools/tests/test_ae2_cable_registry.py` also passes with the explicit-consumer tracking.

After confirming the format split in cycle6, the decoder now skips inactive
vanilla alternatives before resource lookup, rather than opening both legacy
and resource-pack formats. `inactiveTerrainVariants` records exact skipped
paths per loaded pack. Active format images and any explicit custom consumer
remain required; the guard excludes explicitly consumed paths. This eliminates
false missing-image loads and their placeholder allocations. Legacy terrain
packs retain their own active branch. Raw attempted lookup outcomes remain
unmodified in the report; skipped paths are reported separately.
