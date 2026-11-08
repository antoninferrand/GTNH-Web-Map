# Reconciling the 637 frozen missing-texture rows

`tools/reconcile_baseline_textures.py` produces exactly one JSON/CSV row per
unique `(loader_mod, resource)` in the frozen 637-row baseline. It joins current
renderdata, file-qualified historical texture IDs, saved `/gwm renderaudit`
lookups, inactive declarations, installed assets, and explicit source evidence.
It is read-only with respect to the server/world.

```text
python3 tools/reconcile_baseline_textures.py \
  --repo /private/tmp/gtnh-web-map-work \
  --runtime /private/tmp/gwm-fixture/cycle6-render-audit.json \
  --assets-index /private/tmp/gtnh-map-assets.json \
  --output /private/tmp/gwm-fixture/baseline637-reconciliation.json
```

The output includes adjacent `.csv` and `.summary.json` files, source/runtime
versions and SHA-256 provenance. Use the **raw** audit JSON, not the derived
fixture summary. Final counts must use a fresh audit from the matching loaded
candidate; newer source declarations cannot establish that an older running
artifact rendered a replacement successfully.

Explicit source ledgers include optional-overlay contracts, material fallback
contracts, custom-material case corrections, registered casing migrations, and
`*-migration-evidence.csv`. Additional evidence files can be passed through
`--evidence`; they require `loader_mod` (optional for a shared contract),
`baseline_resource`, `kind`, and a nonempty `source`. An exact
`replacement_resource` is supplied for renderer/path migrations that retire
historical IDs. Markdown filename matches are **review pointers only** and do
not close rows automatically.

The new reachability ledger records all 207 baseline IC2 crop stage resources
individually, with their current exact owner/name definition and source
registry gate. Matching CropsNH migration switch cases are cited where they
exist. Force has no established historical owner or migration case; its record
asserts only the installed ownerless name gate, never Force→Carrot or a guessed
image equivalence. The 34 AE2 ultra-dense resources cite the missing registered
part metadata and preallocation used-flag fix. The remaining AE2 matrix frame
cites its source-defined invisible base. These 242 rows require both source
consumer evidence and runtime inactivity; unused declarations alone remain
review gaps.

The generated-image ledger records the sixteen ExtraUtilities compressed PNGs
with their bytecode-derived processor and golden pixel tests. It also records
GoodGenerator's invisible helper base: no required TRANSPARENT PNG exists in
that source; active client TESR effects remain a visual limitation.

Classification rules deliberately keep evidence boundaries visible:

- Effective required lookup failures remain gaps even if another matching
  lookup succeeds. A failure from a different resource loader is kept separate
  from a source-proven retired consumer; the audit's global summary still
  exposes all required failures.
- Removed declarations remain `declaration_removed_needs_review` unless an
  explicit retirement/invisible-base contract or a source-backed observed
  replacement explains their consumers.
- Optional absence requires the explicit optional source contract. Fallback
  and replacement loading also need source evidence before being counted as
  verified outcomes.
- `inactiveTerrainVariants` are pre-lookup format skips, not successful or
  failed loads. They are reported separately from lookup `rawOutcome`, `role`
  and effective `outcome` retained from schema 2.
- Asset existence never substitutes for an observed runtime load. Runtime
  inactivity needs independent consumer/registry evidence.

`python3 tools/test_baseline_reconciliation.py` exercises fourteen production
policy cases, including unexplained removal, false document matches, required
failures, optional/fallback contracts, inactive gates, per-file texture IDs,
loader identity, and format skips without fabricated lookups.

Intermediate cycle-6 reconciliation with the then-current root definitions and
explicit ledgers accounted for 586 source-backed asset/consumer outcomes and
51 remaining review/runtime rows. This is an intermediate checkpoint, not a
final claim that those 586 appearances match the Minecraft client. The final
candidate report must be regenerated after the remaining migrations and fixes
are loaded and rendered.

`--fail-on-gaps` additionally requires the global runtime gate: zero required
lookup failures, texture-definition errors, failed custom models, unmapped MTEs,
missing MTE bases, and unresolved iconsets. Missing summary fields are reported
as unverified checks rather than assumed zero. Thus all 637 baseline rows can
be explained while an additional non-baseline resource/parser error still
correctly prevents a clean final validation result.
