#!/usr/bin/env python3
"""Execute actual mesh rendering through PatchDefinitionFactory without a client/GL context."""
import argparse
import pathlib
import subprocess
import tempfile
root=pathlib.Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--compile-classpath',type=pathlib.Path,required=True)
parser.add_argument('--runtime-classpath',type=pathlib.Path,required=True)
parser.add_argument('--classes',type=pathlib.Path,default=root/'build/classes/java/main')
parser.add_argument('models',nargs='+')
args=parser.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-baked-') as out:
    subprocess.run(['javac','--release','8','-cp',str(args.classes)+':'+args.compile_classpath.read_text().strip(),'-d',out,
                    str(root/'src/main/java/org/dynmap/hdmap/renderer/AtlasMesh.java'),
                    str(root/'src/main/java/org/dynmap/hdmap/renderer/BakedMeshRenderer.java'),
                    str(root/'tools/tests/BakedMeshBehaviorTest.java')],check=True)
    subprocess.run(['java','-cp',out+':'+str(root/'src/main/resources')+':'+str(args.classes)+':'+args.runtime_classpath.read_text().strip(),
                    'BakedMeshBehaviorTest']+args.models,check=True)
