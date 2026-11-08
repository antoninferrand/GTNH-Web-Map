import csv
import tempfile
import unittest
from pathlib import Path

from render_audit import audit


class RenderAuditTest(unittest.TestCase):
    def test_migration_is_not_silently_counted_as_success_and_invalid_records_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "example-texture.txt").write_text(
                "modname:example\n"
                "texture:id=base,filename=assets/example/base.png\n"
                "texture:id=overlay,filename=assets/example/overlay.png,optional=true\n"
                "texture:id=material,filename=assets/example/material.png,fallback=assets/example/default.png\n"
                "moddata:mod=gtmte,type=mte,id=1,basetex=0:base\n"
                "moddata:mod=gtmte,type=mte,id=2,basetex=0:typo\n"
                "moddata:mod=gtmte,type=mte,id=3,iconset=unknown\n"
                "moddata:mod=gtmte,type=mte,id=5,baseblock=registered.casing,basemeta=2\n")
            with (root / "missing-textures.csv").open("w") as stream:
                writer = csv.writer(stream)
                writer.writerow(["loader_mod", "resource"])
                for name in ("base", "overlay", "material", "removed"):
                    writer.writerow(["example", "assets/example/%s.png" % name])
            with (root / "unsupported-gregtech.csv").open("w") as stream:
                writer = csv.writer(stream)
                writer.writerow(["id", "name", "class"])
                for number in range(1, 6):
                    writer.writerow([number, "machine", "Example"])
            result = audit(root, {"assets/example/base.png", "assets/example/default.png"}, root)
            self.assertEqual([row["status"] for row in result["textures"]],
                             ["asset_exists", "optional_absent", "fallback_exists", "declaration_removed_needs_review"])
            self.assertEqual([row["status"] for row in result["mtes"]],
                             ["declared_needs_runtime_validation", "invalid_mapping", "invalid_mapping", "unmapped", "declared_needs_runtime_validation"])


if __name__ == "__main__":
    unittest.main()
