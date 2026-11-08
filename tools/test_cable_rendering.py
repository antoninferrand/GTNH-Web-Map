"""Compile and exercise the production cable renderer without Minecraft or Gradle."""
import subprocess
import tempfile
import unittest
from pathlib import Path


class CableRenderingTest(unittest.TestCase):
    def test_production_geometry_layers_and_paint(self):
        root = Path(__file__).resolve().parents[1]
        sources = [str(path) for path in (root / "src/main/java/org/dynmap/renderer").glob("*.java")
                   if path.name != "CustomModSupportLoader.java"]
        sources.extend([
            str(root / "src/main/java/org/dynmap/hdmap/renderer/GregTechCableRenderer.java"),
            str(root / "tools/java/GregTechCableRenderingTest.java"),
        ])
        with tempfile.TemporaryDirectory(prefix="gwm-cable-test-") as output:
            subprocess.run(["javac", "--release", "8", "-d", output] + sources, check=True)
            subprocess.run(["java", "-cp", output, "GregTechCableRenderingTest"], check=True)


if __name__ == "__main__":
    unittest.main()
