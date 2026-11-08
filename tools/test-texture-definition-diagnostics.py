#!/usr/bin/env python3
"""Exercise production support-file abort diagnostics and deliberate version guards."""
import argparse
from pathlib import Path
import subprocess
import tempfile
r = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--production-source-root', type=Path, default=r)
p.add_argument('--classes', type=Path, default=r / 'build/classes/java/main')
p.add_argument('--compile-classpath', type=Path, required=True)
p.add_argument('--runtime-classpath', type=Path, required=True)
a = p.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-definition-diagnostics-') as out:
    subprocess.run(['javac','--release','8','-cp',str(a.classes)+':'+a.compile_classpath.read_text().strip(),'-d',out,
                    str(a.production_source_root/'src/main/java/org/dynmap/hdmap/TexturePack.java'),
                    str(a.production_source_root/'src/main/java/org/dynmap/hdmap/TexturePackLoader.java'),
                    str(r/'tools/tests/TextureDefinitionDiagnosticsTest.java')],check=True)
    subprocess.run(['java','-Djava.awt.headless=true','-cp',out+':'+str(a.classes)+':'+a.runtime_classpath.read_text().strip(),
                    'TextureDefinitionDiagnosticsTest'],check=True)
