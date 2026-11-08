#!/usr/bin/env python3
"""Offline support audit. Does not emulate Forge, version guards or render pixels."""
import argparse
import csv
import json
import re
import zipfile
from collections import Counter, defaultdict
from pathlib import Path


def properties(line):
    return dict(part.split("=", 1) for part in line.split(",") if "=" in part)


def definitions(directory):
    textures, machines, iconsets = [], defaultdict(list), set()
    for path in sorted(directory.glob("*-texture.txt")):
        mod = None
        for number, raw in enumerate(path.read_text().splitlines(), 1):
            line = raw.strip()
            if line.startswith("modname:"):
                mod = line.split(":", 1)[1]
            elif line.startswith("texture:"):
                data = properties(line.split(":", 1)[1])
                data.update(source=path.name, line=number, mod=mod)
                textures.append(data)
            elif line.startswith("moddata:"):
                data = properties(line.split(":", 1)[1])
                if data.get("mod") != "gtmte":
                    continue
                data.update(source=path.name, line=number, owner=mod)
                if data.get("type") == "mte":
                    machines[int(data["id"])].append(data)
                elif data.get("type") == "iconset":
                    iconsets.add(data["name"])
    return textures, machines, iconsets


def asset_index(index_path=None, jar_directory=None):
    paths = defaultdict(list)
    if index_path:
        for jar in json.loads(index_path.read_text()):
            for path in jar["assets"]:
                paths[path].append(jar["jar"])
    if jar_directory:
        for jar in sorted(jar_directory.glob("*.jar")):
            with zipfile.ZipFile(jar) as archive:
                for path in archive.namelist():
                    if path.startswith("assets/") and not path.endswith("/"):
                        paths[path].append(jar.name)
    return paths


def resource_status(texture, assets):
    path = texture.get("filename", "")
    if path in assets:
        return "asset_exists"
    if texture.get("fallback") in assets:
        return "fallback_exists"
    if texture.get("optional") == "true":
        return "optional_absent"
    return "required_missing"


def read_csv(path):
    with path.open(newline="") as stream:
        return list(csv.DictReader(stream))


def audit(renderdata, assets, baseline):
    textures, machines, iconsets = definitions(renderdata)
    by_path = defaultdict(list)
    by_file = defaultdict(set)
    for texture in textures:
        by_path[texture.get("filename")].append(texture)
        by_file[texture["source"]].add(texture.get("id"))

    texture_results = []
    for row in read_csv(baseline / "missing-textures.csv"):
        candidates = by_path.get(row["resource"], [])
        # A removed declaration needs explicit retirement/migration evidence.
        statuses = [resource_status(item, assets) for item in candidates]
        status = "declaration_removed_needs_review" if not statuses else (
            "required_missing" if "required_missing" in statuses else statuses[0])
        texture_results.append(dict(row, status=status,
                                    definitions=[dict(item, resolution=resource_status(item, assets))
                                                 for item in candidates]))

    machine_results = []
    for row in read_csv(baseline / "unsupported-gregtech.csv"):
        records = machines.get(int(row["id"]), [])
        issues = []
        for record in records:
            if not any(record.get(key) for key in ("basetex", "baseset")):
                issues.append("missing base texture or base set")
            for key in ("iconset", "activeset", "baseset"):
                if record.get(key) and record[key] not in iconsets:
                    issues.append("unknown %s %s" % (key, record[key]))
            for key in ("basetex", "basetex2"):
                value = record.get(key, "")
                if ":" in value and value.split(":", 1)[1] not in by_file[record["source"]]:
                    issues.append("unknown local texture %s" % value)
        status = "unmapped" if not records else "invalid_mapping" if issues else "declared_needs_runtime_validation"
        machine_results.append(dict(row, status=status, issues=issues, definitions=records))

    return {
        "limitations": [
            "Static source audit; does not evaluate installed mod/version guards or override order.",
            "Asset existence across JARs does not prove correct owner, UVs, geometry or state selection.",
            "Removed declarations require evidence; declared MTEs require runtime/visual validation.",
        ],
        "summary": {
            "baselineTextures": dict(Counter(item["status"] for item in texture_results)),
            "baselineMtes": dict(Counter(item["status"] for item in machine_results)),
            "duplicateMteIds": {str(key): value for key, value in machines.items() if len(value) > 1},
        },
        "textures": texture_results,
        "mtes": machine_results,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    root = Path(__file__).resolve().parents[1]
    parser.add_argument("--renderdata", type=Path, default=root / "src/main/resources/renderdata")
    parser.add_argument("--baseline", type=Path, default=root / "docs/remediation/baseline")
    parser.add_argument("--assets-index", type=Path,
                        help='JSON list of {"jar": filename, "assets": [exact resource paths]}')
    parser.add_argument("--jars", type=Path, help="Read assets from local installed mod JARs")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--fail-on-gaps", action="store_true")
    args = parser.parse_args()
    if not args.assets_index and not args.jars:
        parser.error("provide --assets-index or --jars to avoid an empty asset inventory")
    result = audit(args.renderdata, asset_index(args.assets_index, args.jars), args.baseline)
    if args.output:
        args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result["summary"], indent=2))
    gaps = {"unmapped", "invalid_mapping", "required_missing", "declaration_removed_needs_review"}
    return int(args.fail_on_gaps and any(row["status"] in gaps for row in result["textures"] + result["mtes"]))


if __name__ == "__main__":
    raise SystemExit(main())
