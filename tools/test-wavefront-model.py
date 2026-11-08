#!/usr/bin/env python3
"""Exercise production OBJ parsing and real patch geometry without a client/GL context."""
import argparse
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--compile-classpath", type=Path, required=True)
parser.add_argument("--runtime-classpath", type=Path, required=True)
parser.add_argument("--model", type=Path, help="Optional installed/source ice_cream_machine.obj fixture")
args = parser.parse_args()
classes = str(ROOT / "build/classes/java/main")
with tempfile.TemporaryDirectory(prefix="gwm-obj-test-") as work:
    subprocess.run(["javac", "--release", "8", "-cp", classes + ":" + args.compile_classpath.read_text().strip(),
                    "-d", work, str(ROOT / "src/main/java/org/dynmap/hdmap/renderer/WavefrontModel.java"),
                    str(ROOT / "tools/tests/WavefrontModelBehaviorTest.java")], check=True)
    command = ["java", "-cp", work + ":" + classes + ":" + args.runtime_classpath.read_text().strip(),
               "WavefrontModelBehaviorTest"]
    if args.model:
        command.append(str(args.model))
    subprocess.run(command, check=True)
