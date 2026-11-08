#!/usr/bin/env python3
"""Check vending invalidation dependency/diff behavior without Minecraft or a world."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix='gwm-vending-updates-') as work:
    sources = [ROOT / 'src/main/java/org/dynmap/forge/VendingRenderUpdates.java', ROOT / 'tools/tests/VendingRenderUpdatesTest.java']
    subprocess.run(['javac', '--release', '8', '-proc:none', '-d', work, *map(str, sources)], check=True)
    subprocess.run(['java', '-cp', work, 'org.dynmap.forge.VendingRenderUpdatesTest'], check=True)
