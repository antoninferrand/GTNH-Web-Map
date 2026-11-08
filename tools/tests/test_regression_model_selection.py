"""Custom renderer model schemas must not be inferred from the presence of an atlas key."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('renderer_regressions', ROOT / 'tools/run-renderer-regressions.py')
RUNNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RUNNER)


class RegressionModelSelectionTest(unittest.TestCase):
    def test_declared_baked_consumers_and_source_subclasses_only(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            renderer = root / 'src/main/java/org/dynmap/hdmap/renderer'
            data = root / 'src/main/resources/renderdata'
            models = root / 'src/main/resources/models/baked'
            for path in (renderer, data, models): path.mkdir(parents=True)
            (renderer / 'Tinted.java').write_text('package org.dynmap.hdmap.renderer; public class Tinted extends BakedMeshRenderer {}')
            (renderer / 'Child.java').write_text('package org.dynmap.hdmap.renderer; public class Child extends Tinted {}')
            for name in ('actual', 'child', 'walrus', 'orphan'):
                (models / (name + '.json')).write_text('{"atlas":[1,1]}')
            (data / 'fixture-models.txt').write_text(
                'customblock:id=1,class=org.dynmap.hdmap.renderer.BakedMeshRenderer,mesh=models/baked/actual.json\n'
                'customblock:id=2,class=org.dynmap.hdmap.renderer.Child,mesh=models/baked/child.json\n'
                'customblock:id=3,class=org.dynmap.hdmap.renderer.AE2FCWalrusRenderer,mesh=models/baked/walrus.json\n')
            self.assertEqual(RUNNER.declared_baked_models(root), ['models/baked/actual.json', 'models/baked/child.json'])


if __name__ == '__main__': unittest.main()
