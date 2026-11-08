#!/usr/bin/env python3
"""Run the actual IsoHDPerspective patch intersection path with oversized geometry."""
import argparse
import pathlib
import subprocess
import tempfile

root = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--compile-classpath', type=pathlib.Path, required=True)
parser.add_argument('--runtime-classpath', type=pathlib.Path, required=True)
parser.add_argument('--classes', type=pathlib.Path, default=root / 'build/classes/java/main')
args = parser.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-patch-capacity-') as out:
    subprocess.run(['javac', '--release', '8', '-cp', str(args.classes) + ':' + args.compile_classpath.read_text().strip(),
                    '-d', out, str(root / 'src/main/java/org/dynmap/hdmap/IsoHDPerspective.java'),
                    str(root / 'tools/tests/PatchIntersectionCapacityTest.java')], check=True)
    subprocess.run(['java', '-Djava.awt.headless=true', '-cp', out + ':' + str(args.classes) + ':'
                    + args.runtime_classpath.read_text().strip(), 'PatchIntersectionCapacityTest'], check=True)
