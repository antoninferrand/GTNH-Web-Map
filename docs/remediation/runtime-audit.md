# Effective runtime rendering audit

Run `gwm renderaudit` from an operator console after map initialization. It writes
`dynmap/render-audit.json` with exact runtime mod versions, registered blocks,
blank metadata candidates, registered MTEs and their loaded support, and effective
resource-resolution outcomes for each loaded pack. It never places blocks,
changes rendering settings, reloads the map or restarts a server.

Blank metadata is a candidate list: many registered blocks intentionally use
only some metadata values. Explicit invisible texture records count as defined.
MTE hasBase is a structural check, not proof of correct geometry/state/UVs.
Required failures, optional absence and successful fallback are separate counts.

The command preserves immutable lookup snapshots from decoded packs before
closing their loaders; no streams or global decoder state are retained. If no
pack has loaded yet, resourceCoverage is NOT_LOADED_RENDER_FIRST and zero missing
resources is not evidence of coverage. Render a small fixture area and repeat.

The production Java build validates linkage. Full-pack fixture execution is the
integration check for registry order, dynamic resolution and saved-state behavior.

Schema2 includes the missing-base and unresolved-iconset counts for registered
MTEs, including deferred registered-block bases. Resource rows preserve both
`rawOutcome` and the effective `outcome`, plus a precise resource `role`. Only
missing inactive vanilla terrain-format alternatives receive
`INACTIVE_TERRAIN_VARIANT`; custom required images stay required even without a
mod owner. Optional feature probes are recorded separately. The raw results
remain available for reviewing this classification.

The audit also retains `textureDefinitionErrors` with source file and line.
A texture-file parser can stop before later mappings while reporting zero
missing loaded images; cycle6 exposed this with a checker atlas dimension
collision in Blood Magic. The summary must have zero definition errors as
well as zero failed custom renderers before resource closure is accepted.
Parser diagnostics remain severe in the server log; the audit does not hide
or automatically repair malformed definitions.
