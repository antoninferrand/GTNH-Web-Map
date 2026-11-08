"""Guard immediate texture-ID resolution in opt-in cable support records."""
from pathlib import Path
import unittest


class CableResourceOrderTest(unittest.TestCase):
    def test_cable_texture_ids_exist_before_mte_records(self):
        root = Path(__file__).resolve().parents[2]
        count = 0
        for path in (root / "src/main/resources/renderdata").glob("*-texture.txt"):
            declared = set()
            for number, line in enumerate(path.read_text().splitlines(), 1):
                if not line.startswith(("texture:", "moddata:mod=gtmte,type=mte,")):
                    continue
                fields = dict(part.split("=", 1) for part in line.split(":", 1)[1].split(",") if "=" in part)
                if line.startswith("texture:"):
                    declared.add(fields["id"])
                    continue
                for key in ("wiretex", "endtex"):
                    if key in fields and ":" in fields[key]:
                        texture = fields[key].split(":", 1)[1]
                        self.assertIn(texture, declared, f"{path.name}:{number} unresolved {key} {texture}")
                        count += 1
        self.assertGreater(count, 0, "fixture must cover real opt-in cable records")


if __name__ == "__main__":
    unittest.main()
