#!/usr/bin/env python3
"""Check real bundled vanilla pixels through the production texture-pack loader and terrain path."""
import argparse
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--compile-classpath', type=Path, required=True)
parser.add_argument('--runtime-classpath', type=Path, required=True)
parser.add_argument('--classes', type=Path, default=root / 'build/classes/java/main')
args = parser.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-vanilla-pixels-') as out:
    subprocess.run(['javac', '--release', '8', '-cp', str(args.classes) + ':' + args.compile_classpath.read_text().strip(),
                    '-d', out, str(root / 'src/main/java/org/dynmap/hdmap/TexturePack.java'),
                    str(root / 'src/main/java/org/dynmap/hdmap/TexturePackLoader.java'),
                    str(root / 'tools/tests/VanillaTexturePixelsTest.java')], check=True)
    subprocess.run(['java', '-Djava.awt.headless=true', '-cp', out + ':' + str(root / 'src/main/resources') + ':'
                    + str(args.classes) + ':' + args.runtime_classpath.read_text().strip(),
                    'VanillaTexturePixelsTest', str(Path(out) / 'fixture')], check=True)
