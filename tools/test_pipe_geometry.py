import subprocess
import tempfile
import unittest
from pathlib import Path


class PipeGeometryTest(unittest.TestCase):
    def test_production_mesh_generator(self):
        root = Path(__file__).resolve().parents[1]
        sources = [str(path) for path in (root / "src/main/java/org/dynmap/renderer").glob("*.java")
                   if path.name != "CustomModSupportLoader.java"]
        sources.extend([str(root / "src/main/java/org/dynmap/hdmap/renderer/PipeRendererBase.java"),
                        str(root / "tools/java/PipeGeometryTest.java")])
        with tempfile.TemporaryDirectory() as output:
            subprocess.run(["javac", "-d", output] + sources, check=True, capture_output=True)
            subprocess.run(["java", "-cp", output, "PipeGeometryTest"], check=True)


if __name__ == "__main__":
    unittest.main()
