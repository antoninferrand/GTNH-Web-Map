#!/usr/bin/env python3
"""Reconcile every frozen missing-texture row with source and runtime evidence.

This is a report-only tool. Removed declarations and unused runtime declarations
remain review items unless independent source evidence explains their consumers.
Asset existence and observed loader outcomes do not prove Minecraft-client parity.
"""
import argparse
from collections import Counter, defaultdict
import csv
import hashlib
import json
from pathlib import Path
import re
import subprocess


def record(line):
    return dict(item.split('=', 1) for item in line.split(':', 1)[1].split(',') if '=' in item)


def declarations(text, source):
    owner = None
    result = []
    for number, raw in enumerate(text.splitlines(), 1):
        line = raw.strip()
        if line.startswith('modname:'):
            owner = line.split(':', 1)[1]
        elif line.startswith('texture:'):
            item = record(line)
            item.update(source=source, line=number, loader_mod=owner)
            result.append(item)
    return result


def read_csv(path):
    with path.open(newline='') as stream:
        return list(csv.DictReader(stream))


def evidence_index(directory, extra=()):
    """Read explicit source ledgers; Markdown matches are only review pointers."""
    result = defaultdict(list)
    specs = [
        ('optional-texture-evidence.csv', 'resource', 'optional'),
        ('material-fallback-evidence.csv', 'baseline_resource', 'material_fallback'),
        ('custom-material-path-evidence.csv', 'baseline_resource', 'source_correction'),
        ('legacy-texture-resolution-ledger.csv', 'baseline_resource', 'consumer_migration'),
    ]
    for filename, key, kind in specs:
        path = directory / filename
        if not path.exists():
            continue
        for row in read_csv(path):
            actual_kind = kind
            if kind == 'consumer_migration' and row.get('resolution_status') == 'retired_unregistered_consumers':
                actual_kind = 'retirement'
            result[(row.get('loader_mod'), row[key])].append(dict(row, kind=actual_kind, evidence_file=str(path)))
    for path in list(sorted(directory.glob('*-migration-evidence.csv'))) + list(extra):
        for row in read_csv(path):
            if not row.get('source') or not row.get('kind'):
                raise ValueError('Additional evidence needs nonempty source and kind: ' + str(path))
            result[(row.get('loader_mod') or None, row['baseline_resource'])].append(dict(row, evidence_file=str(path)))
    return result


def current_declarations(repo):
    return [item for path in sorted((repo / 'src/main/resources/renderdata').glob('*-texture.txt'))
            for item in declarations(path.read_text(), path.name)]


def old_declarations(repo, revision, filenames):
    result, absent = [], []
    for name in sorted(filenames):
        source = 'src/main/resources/renderdata/' + name
        process = subprocess.run(['git', '-C', str(repo), 'show', revision + ':' + source],
                                 capture_output=True, text=True)
        if process.returncode:
            absent.append(source)
        else:
            result.extend(declarations(process.stdout, name))
    return result, absent


def assets_from_index(path, repo):
    assets = set()
    if path:
        for archive in json.loads(path.read_text()):
            assets.update(archive['assets'])
    resource_root = repo / 'src/main/resources'
    assets.update(str(p.relative_to(resource_root)) for p in resource_root.rglob('*') if p.is_file())
    return assets


def reconcile(baseline, current, original, runtime, evidence, assets, markdown=()):
    by_path, current_by_id, old_by_path = defaultdict(list), defaultdict(list), defaultdict(list)
    for item in current:
        by_path[item.get('filename')].append(item)
        current_by_id[(item['source'], item.get('id'))].append(item)
    for item in original:
        old_by_path[item.get('filename')].append(item)
    lookups, inactive = defaultdict(list), defaultdict(list)
    for pack, items in runtime.get('resourcePacks', {}).items():
        for item in items:
            lookups[item['resource']].append(dict(item, pack=pack))
    for item in runtime.get('inactiveResourceDeclarations', []):
        inactive[item['resource']].append(item)
    results = []
    for row in baseline:
        resource, owner = row['resource'], row['loader_mod']
        source_name = owner + '-texture.txt'
        # File-qualified IDs avoid cross-mod ID collisions. No basename guessing.
        old = [d for d in old_by_path[resource] if d['source'] == source_name]
        same = [d for d in by_path[resource] if d['source'] == source_name]
        migrated = [d for previous in old for d in current_by_id[(previous['source'], previous.get('id'))]
                    if d.get('filename') != resource]
        proofs = evidence.get((owner, resource), []) + evidence.get((None, resource), [])
        # An explicit evidence table may supply a replacement after IDs were retired.
        replacements = {d['filename'] for d in migrated}
        for proof in proofs:
            if proof.get('resource') and proof['resource'] != resource:
                replacements.add(proof['resource'])
            if proof.get('replacement_resource'):
                replacements.add(proof['replacement_resource'])
        target_paths = {resource} | replacements
        expected_owners = {owner}
        for declaration in same + migrated + [d for path in replacements for d in by_path[path]]:
            expected_owners.add(declaration.get('resourcemod') or declaration.get('loader_mod'))
        def owner_matches(item):
            return item.get('mod') is None or item['mod'] in expected_owners
        attempts = [d for p in sorted(target_paths) for d in lookups[p] if owner_matches(d)]
        dormant = [d for p in sorted(target_paths) for d in inactive[p] if owner_matches(d)]
        skipped = [dict(resource=path, pack=pack, reason='inactive_terrain_variant')
                   for pack, paths in runtime.get('inactiveTerrainVariants', {}).items()
                   for path in paths if path in target_paths]
        outcomes = sorted({d['outcome'] for d in attempts})
        kinds = {p['kind'] for p in proofs}
        source_verified = bool(proofs)
        retirement = bool(kinds & {'retirement', 'invisible_base'})
        missing = any(d['outcome'] == 'REQUIRED_MISSING' for d in attempts)
        loaded_original = [d for d in attempts if d['outcome'] == 'ORIGINAL']
        loaded_fallback = [d for d in attempts if d['outcome'] == 'FALLBACK']
        optional = any(d['outcome'] == 'OPTIONAL_ABSENT' for d in attempts)
        inactive_variant = any(d['outcome'] == 'INACTIVE_TERRAIN_VARIANT' for d in attempts)
        if missing:
            status = 'required_missing'
        elif any(d['resource'] == resource for d in loaded_original):
            status = 'loaded_original'
        elif loaded_original:
            status = 'replacement_loaded_source_verified' if source_verified else 'replacement_loaded_needs_source_review'
        elif loaded_fallback:
            status = 'fallback_loaded_source_verified' if source_verified else 'fallback_loaded_needs_source_review'
        elif optional:
            status = 'optional_absent_source_verified' if 'optional' in kinds else 'optional_absent_needs_source_review'
        elif inactive_variant or skipped:
            status = 'inactive_terrain_variant'
        elif retirement and not same:
            status = 'retired_source_verified'
        elif dormant:
            status = 'inactive_declaration_source_verified' if source_verified else 'inactive_declaration_needs_source_review'
        elif not same and not migrated:
            status = 'declaration_removed_source_verified' if retirement else 'declaration_removed_needs_review'
        elif migrated:
            status = 'replacement_not_runtime_observed'
        elif source_verified:
            status = 'declared_source_verified_not_runtime_observed'
        else:
            status = 'declared_not_runtime_observed'
        pointers = []
        for path, text in markdown:
            # Common filenames such as 0.png are not useful source attribution.
            basename = Path(resource).name
            if resource in text or (len(basename) >= 12 and basename in text):
                pointers.append(str(path))
        results.append(dict(row, status=status, declarationStatus='retained' if same else 'migrated' if migrated else 'removed_or_prebaseline_correction',
                            baselineDeclarations=old, currentDeclarations=same, replacementDeclarations=migrated,
                            replacementResources=sorted(replacements), runtimeOutcomes=outcomes,
                            runtimeLookups=attempts, inactiveRuntimeDeclarations=dormant, skippedRuntimeVariants=skipped,
                            sourceEvidence=proofs, reviewDocumentPointers=pointers,
                            originalAssetExists=resource in assets,
                            replacementAssets={p: p in assets for p in sorted(replacements)}))
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--runtime', type=Path, required=True, help='Saved /gwm renderaudit JSON after rendering')
    parser.add_argument('--assets-index', type=Path)
    parser.add_argument('--baseline-revision', default='e0cb1f30')
    parser.add_argument('--evidence', type=Path, action='append', default=[])
    parser.add_argument('--output', type=Path, required=True, help='JSON report; adjacent CSV and summary are also written')
    parser.add_argument('--fail-on-gaps', action='store_true')
    args = parser.parse_args()
    baseline_path = args.repo / 'docs/remediation/baseline/missing-textures.csv'
    baseline = read_csv(baseline_path)
    if len(baseline) != 637 or len({(r['loader_mod'], r['resource']) for r in baseline}) != 637:
        raise ValueError('Expected exactly 637 unique frozen baseline rows')
    runtime = json.loads(args.runtime.read_text())
    if 'schemaVersion' not in runtime or 'resourcePacks' not in runtime:
        raise ValueError('Expected raw /gwm renderaudit JSON with schemaVersion/resourcePacks, not a derived report')
    evidence_dir = args.repo / 'docs/remediation'
    current = current_declarations(args.repo)
    original, absent = old_declarations(args.repo, args.baseline_revision, {r['loader_mod'] + '-texture.txt' for r in baseline})
    markdown = [(p, p.read_text()) for p in sorted(evidence_dir.glob('*.md'))]
    rows = reconcile(baseline, current, original, runtime, evidence_index(evidence_dir, args.evidence),
                     assets_from_index(args.assets_index, args.repo), markdown)
    verified = {'loaded_original', 'replacement_loaded_source_verified', 'fallback_loaded_source_verified',
                'optional_absent_source_verified', 'inactive_terrain_variant', 'retired_source_verified',
                'inactive_declaration_source_verified', 'declaration_removed_source_verified'}
    source_head = subprocess.check_output(['git', '-C', str(args.repo), 'rev-parse', 'HEAD'], text=True).strip()
    summary = dict(baselineRows=len(rows), sourceHead=source_head, runtimeDynmapVersion=runtime.get('versions', {}).get('Dynmap'),
                   evidenceKindCounts=dict(sorted(Counter(kind for row in rows for kind in {p['kind'] for p in row['sourceEvidence']}).items())), statusCounts=dict(sorted(Counter(r['status'] for r in rows).items())),
                   verifiedAssetOrConsumerOutcomes=sum(r['status'] in verified for r in rows),
                   needsReviewOrRuntime=sum(r['status'] not in verified for r in rows),
                   declarationCounts=dict(sorted(Counter(r['declarationStatus'] for r in rows).items())),
                   runtimeSchemaVersion=runtime.get('schemaVersion'), resourceCoverage=runtime.get('resourceCoverage'),
                   runtimeSummary=runtime.get('summary'), baselineRevision=args.baseline_revision,
                   baselineSha256=hashlib.sha256(baseline_path.read_bytes()).hexdigest(),
                   runtimeSha256=hashlib.sha256(args.runtime.read_bytes()).hexdigest(), missingBaselineSourceFiles=absent)
    report = dict(summary=summary, limitations=[
        'Exactly one row per frozen baseline pair; source documents are review pointers unless an explicit evidence ledger supplies a contract.',
        'Unused runtime declarations require independent consumer evidence; removal is not automatically a fix.',
        'Any observed effective REQUIRED_MISSING remains a gap even if another lookup succeeds.',
        'Loader results cover the loaded packs only; cold renders do not prove active-state or Minecraft-client visual parity.',
        'Static asset existence alone is never promoted to runtime validation.',
        'Schema versions before2 lack resource roles/raw outcomes and may lack owner fields; rerun with schema2 for final counts.'
    ], textures=rows)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    args.output.with_suffix('.summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    with args.output.with_suffix('.csv').open('w', newline='') as stream:
        fields = ['loader_mod', 'resource', 'status', 'declarationStatus', 'originalAssetExists', 'replacementResources', 'runtimeOutcomes', 'sourceEvidence', 'reviewDocumentPointers']
        writer = csv.DictWriter(stream, fieldnames=fields, lineterminator='\n')
        writer.writeheader()
        for row in rows:
            writer.writerow({key: json.dumps(row[key], separators=(',', ':')) if isinstance(row[key], (list, dict)) else row[key] for key in fields})
    print(json.dumps(summary, indent=2))
    return int(args.fail_on_gaps and summary['needsReviewOrRuntime'] > 0)


if __name__ == '__main__':
    raise SystemExit(main())
