#!/usr/bin/env python3
import argparse
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description="Exercise actual custom-model failure handling")
parser.add_argument("--compile-classpath", type=Path, required=True)
parser.add_argument("--runtime-classpath", type=Path, required=True)
args = parser.parse_args()
classes = str(root / "build/classes/java/main")
with tempfile.TemporaryDirectory(prefix="gwm-model-failure-") as out:
    subprocess.run(["javac", "--release", "8", "-cp", classes + ":" + args.compile_classpath.read_text().strip(),
                    "-d", out, str(root / "src/main/java/org/dynmap/hdmap/HDBlockModels.java"),
                    str(root / "tools/tests/RendererFailureBehaviorTest.java")], check=True)
    raise SystemExit(subprocess.run(["java", "-cp", out + ":" + classes + ":" + args.runtime_classpath.read_text().strip(),
                                    "RendererFailureBehaviorTest"]).returncode)
