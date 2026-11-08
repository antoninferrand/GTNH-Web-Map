# Final production validation

Production artifact `gtnh-web-map-ccc2d77.jar` passes the strict loaded-pack and
637-row baseline gate. Source commit:
`ccc2d771d19dfdcb094280da03782be2d8c085f9`. Artifact SHA-256:
`b8677d38e57ea2c80e996214287c1ff0a60466386de505ee740c44ca9085f803`.
The local release JAR checksum was independently checked against the deployment
provenance.

The actual fetched production audit reports 4,258 registered blocks and 3,412
mapped MTEs. Required missing resources, texture-definition/config errors,
failed custom models, unmapped MTEs, missing bases, and unresolved iconsets are
all **zero**. The Linux `Avaritiaddons.cfg` case correction is included.
`reconcile_baseline_textures.py --fail-on-gaps` exits 0 against this production
JSON and the current root definitions.

All 637 rows are accounted for: 270 source-optional absences, 277 independently
proven inactive consumers, 22 loaded fallbacks, 58 source-backed replacements,
and ten proven retirements/invisible bases; zero unexplained review gaps.
The production CSV is byte-identical to the preserved
[637-row ledger](runtime-fixture/final-baseline637-reconciliation.csv), SHA-256
`b2feb554d659df6bc9081093c439e38e02d4d18174964edf7a449173b025e3a4`.

[Production summary and provenance](runtime-fixture/production-validation-summary.json)
records the actual raw-audit SHA-256
`a0fb691b64773f78086758e4f8330abd92c0621bf66362b057aed29691d63e99`.
The fetched raw audit and full reconciliation remain in `/private/tmp/gwm-release/`.

This production evidence is separate from the unchanged
[isolated visual validation](final-validation.md) for artifact `af109fdb`.
The same documented client-effect, owner-cell, and restrictive pipe-design
limits remain. Loaded resource coverage does not imply every existing map PNG
has been refreshed. At handoff, the production radius-64 spawn refresh centered
at `(626,-200)` completed 36 flat tiles and 952 surface tiles in separate jobs. No additional restart is required by this report.
GitHub push was canceled by the user's latest instruction and was not performed.
