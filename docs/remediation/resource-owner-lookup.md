# Unknown resource owner lookup

The isolated full-pack fixture booted successfully, but its first stdtexture
radius render failed in `ForgeServer.openResource`: a declared resource owner
was not in Forge's indexed mod list, so `mc.getMod()` dereferenced null.
Resource domains and historical fallback owners need not be registered mod IDs.

Treat an unknown owner like an owner without a mod instance, then continue the
existing loaded-mod resource search. Required misses still return null to the
texture resolver and remain reported; this change does not mark them optional.

`python3 tools/test_forge_resource_lookup.py` extracts and compiles the real
production lookup method with tiny loader/container fixtures. Thirteen
assertions cover valid-owner short circuit, absent owner fallback, null owner,
bootstrap container without mod instance and unresolved required resources.
