#!/usr/bin/env python3
import pathlib
import subprocess
import tempfile
root = pathlib.Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix='gwm-atlas-') as folder:
    subprocess.run(['javac', '--release', '8', '-d', folder,
                    str(root / 'src/main/java/org/dynmap/hdmap/renderer/AtlasMesh.java'),
                    str(root / 'tools/tests/AtlasMeshBehaviorTest.java')], check=True)
    subprocess.run(['java', '-cp', folder, 'AtlasMeshBehaviorTest'], check=True)
