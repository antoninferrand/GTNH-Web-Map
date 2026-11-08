# GTNH 2.9.0 rendering remediation

The `baseline` directory freezes the 2026-11-08 server findings: 157 unsupported
MTE variants and 637 unresolved texture references. Backlog classifications are
initial hypotheses, not evidence that an entry has been fixed.

Run the offline audit against exact installed asset paths:

```sh
python3 tools/render_audit.py --jars /path/to/server/mods --output /tmp/render-audit.json
python3 -m unittest discover -s tools -p 'test_render_audit.py'
```

Alternatively supply `--assets-index inventory.json`, a JSON list of objects
with `jar` and `assets` fields. No proprietary mod assets are committed here.
`--fail-on-gaps` fails on unresolved baseline resources and missing/invalid
machine records. Removed declarations require review and never automatically
count as fixed. The report distinguishes statically declared mappings from
runtime and visual validation. It does not emulate Forge's version guards or
resource-pack resolution; those need the isolated pack fixture.

Individual fix notes record source evidence, affected baseline entries and
checks. Keep commits local; production deployment is outside this work.
