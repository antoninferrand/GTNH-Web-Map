#!/usr/bin/env python3
"""Exercise wall-mirror source pixel geometry, palette and active/dummy/facing states."""
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
with tempfile.TemporaryDirectory(prefix='gwm-wall-mirror-') as out:
    subprocess.run(['javac','--release','8','-cp',str(a.classes)+':'+a.compile_classpath.read_text().strip(),'-d',out,
                    str(r/'src/main/java/org/dynmap/hdmap/renderer/AtlasMesh.java'),
                    str(r/'src/main/java/org/dynmap/hdmap/renderer/WitchingWallMirrorRenderer.java'),
                    str(r/'tools/tests/WitchingWallMirrorBehaviorTest.java')],check=True)
    subprocess.run(['java','-Djava.awt.headless=true','-cp',out+':'+str(r/'src/main/resources')+':'+str(a.classes)+':'+a.runtime_classpath.read_text().strip(),
                    'WitchingWallMirrorBehaviorTest'],check=True)
