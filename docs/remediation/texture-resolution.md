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
