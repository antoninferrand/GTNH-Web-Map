#!/usr/bin/env python3
import argparse
import pathlib
import subprocess
import tempfile
root=pathlib.Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--compile-classpath',type=pathlib.Path,required=True)
parser.add_argument('--runtime-classpath',type=pathlib.Path,required=True)
parser.add_argument('--classes',type=pathlib.Path,default=root/'build/classes/java/main')
args=parser.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-mimic-') as folder:
    subprocess.run(['javac','--release','8','-cp',str(args.classes)+':'+args.compile_classpath.read_text().strip(),'-d',folder,
                    str(root/'src/main/java/org/dynmap/hdmap/renderer/InventoryMimicRenderer.java'),
                    str(root/'tools/tests/InventoryMimicBehaviorTest.java')],check=True)
    subprocess.run(['java','-cp',folder+':'+str(args.classes)+':'+args.runtime_classpath.read_text().strip(),'InventoryMimicBehaviorTest'],check=True)
