# Explicit texture resource policies

Texture records accept `resourcemod=<owner>`, `fallback=<asset path>` and
`fallbackmod=<owner>`. The fallback owner defaults to the original owner.
Resolution searches the complete resource-pack/mod/bundled stack for the original
path before searching that stack for the declared fallback. Required resources
without an original or fallback still produce missing-resource warnings.

`TexturePackLoader.getResourceLookups()` exposes an immutable snapshot containing
requested resource/owner, resolved resource/owner and outcome. Lookup metadata
contains no streams, stays local to its loader and is released when the loader
closes. Streams remain owned by the decoding caller.

`python3 tools/test-texture-loader.py` compiles the production loader/resolver for
Java 8 with small server stubs and exercises directory/ZIP packs, original versus
fallback priority, explicit owning JARs, required failures, optional lookup
semantics and stream/diagnostic lifecycle. It does not replace the full mod build
or in-game validation.

## Optional layers

`optional=true` on a texture record means that absence is intentional according
to the mod's renderer contract. Resource-pack/mod lookup still runs first. If no
image exists, its dynamic texture tiles are explicitly transparent and custom
texture processors are skipped. Required textures remain required by default.
The loader diagnoses absence as `OPTIONAL_ABSENT`, distinct from a failed
required lookup.

Source evidence at GregTech 5.09.54.265:

- `MTEBasicMachineWithRecipe` constructs side, front, top and bottom overlays
  (including active variants) with `Textures.BlockIcons.customOptional`.
- `MTEBoxinator`, `MTEPotionBrewer` and `MTEReplicator` explicitly construct
  their suffixed overlay names with `customOptional` as well (16 baseline paths).
- `GTCustomOptionalBlockIconContainer.run()` uses `InvisibleIcon` when the
  declared texture is absent, leaving the casing layer intact.
- `Textures.BlockIcons` declares the 48 baseline iconset names via
  `createOptional`; `GTOptionalBlockIconContainer.run()` has the same absence
  semantics.

Only the 270 baseline resource paths covered by these contracts are annotated
in this change. Every duplicate texture declaration for the same path receives
the same property; this avoids depending on which alias registers first.
