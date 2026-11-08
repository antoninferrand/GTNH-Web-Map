#!/usr/bin/env python3
"""Exercise the opt-in model triangle API and unchanged legacy coordinate bounds."""
import argparse
from pathlib import Path
import subprocess
import tempfile
r = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--classes', type=Path, default=r / 'build/classes/java/main')
p.add_argument('--compile-classpath', type=Path, required=True)
p.add_argument('--runtime-classpath', type=Path, required=True)
a = p.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-model-bounds-') as out:
    subprocess.run(['javac', '--release', '8', '-cp', str(a.classes)+':'+a.compile_classpath.read_text().strip(), '-d', out,
                    str(r/'src/main/java/org/dynmap/renderer/RenderPatchFactory.java'),
                    str(r/'src/main/java/org/dynmap/utils/PatchDefinitionFactory.java'),
                    str(r/'tools/tests/ModelTriangleBoundsTest.java')], check=True)
    subprocess.run(['java','-cp',out+':'+str(a.classes)+':'+a.runtime_classpath.read_text().strip(),
                    'ModelTriangleBoundsTest'], check=True)
