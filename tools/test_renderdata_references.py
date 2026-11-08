from pathlib import Path
import unittest
from check_renderdata_references import errors_in


class ReferenceOrderTest(unittest.TestCase):
    def test_eager_references_must_follow_local_texture_declaration(self):
        before = "moddata:mod=gtmte,type=mte,id=1,wiretex=17000:wire\ntexture:id=wire,filename=wire.png\n"
        after = "texture:id=wire,filename=wire.png\nmoddata:mod=gtmte,type=mte,id=1,wiretex=17000:wire\n"
        self.assertEqual(len(errors_in(before, "fixture")), 1)
        self.assertEqual(errors_in(after, "fixture"), [])
        self.assertEqual(len(errors_in("moddata:mod=gtmte,type=iconset,name=x,front2=23000:other_file_only", "fixture")), 1)

    def test_named_iconsets_remain_late_bound(self):
        self.assertEqual(errors_in("moddata:mod=gtmte,type=mte,id=1,iconset=later,activeset=mod:later", "fixture"), [])

    def test_repository_support_references(self):
        root = Path(__file__).resolve().parents[1] / "src/main/resources/renderdata"
        errors = [error for path in root.glob("*-texture.txt")
                  for error in errors_in(path.read_text(), path.name)]
        self.assertEqual(errors, [])


if __name__ == "__main__":
    unittest.main()
