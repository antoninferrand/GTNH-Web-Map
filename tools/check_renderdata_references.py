#!/usr/bin/env python3
"""Check eager texture references in file-local declaration order.

Named iconsets remain late-bound. Historical unknown block aliases are listed
explicitly; version/registry selection still requires the effective runtime audit.
"""
from pathlib import Path
import argparse
import json
import re


LEGACY_UNKNOWN = {tuple(row) for row in json.loads(
    Path(__file__).with_name("legacy-unresolved-block-textures.json").read_text())}

def errors_in(text, source):
    known = set()
    errors = []
    for number, line in enumerate(text.splitlines(), 1):
        line = re.sub(r"^\[[^]]+\]", "", line)
        if line.startswith("texture:"):
            properties = dict(token.split("=", 1) for token in line[8:].split(",") if "=" in token)
            if "id" in properties:
                known.add(properties["id"])
        elif line.startswith(("moddata:", "block:")):
            fields = dict(re.findall(r"(?:^\w+:|,)([^=,]+)=([^,]+)", line))
            for key, value in fields.items():
                reference = re.fullmatch(r"-?\d+:([^,]+)", value)
                if reference and reference[1] not in known:
                    legacy = (source, fields.get("id", ""), key, reference[1])
                    if line.startswith("block:") and legacy in LEGACY_UNKNOWN:
                        continue
                    errors.append(f"{source}:{number}: {key} references undeclared local texture {reference[1]}")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", nargs="?", type=Path,
                        default=Path(__file__).resolve().parents[1] / "src/main/resources/renderdata")
    args = parser.parse_args()
    errors = [error for path in sorted(args.directory.glob("*-texture.txt"))
              for error in errors_in(path.read_text(), path.name)]
    for error in errors:
        print(error)
    print(f"Eager support references: {len(errors)} ordering/name errors")
    return bool(errors)


if __name__ == "__main__":
    raise SystemExit(main())
