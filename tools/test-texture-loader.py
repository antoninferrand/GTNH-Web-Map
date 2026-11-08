#!/usr/bin/env python3
"""Compile the real texture loader with tiny server stubs; no Minecraft/Gradle needed."""
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
STUBS = {
    'org/dynmap/DynmapCore.java': '''package org.dynmap;
import org.dynmap.common.DynmapServerInterface;
public class DynmapCore {
 private final DynmapServerInterface server;
 public DynmapCore(DynmapServerInterface server) { this.server = server; }
 public DynmapServerInterface getServer() { return server; }
}''',
    'org/dynmap/Log.java': '''package org.dynmap;
import java.util.*;
public class Log {
 public static final List<String> warnings = new ArrayList<String>();
 public static void info(String text) { }
 public static void severe(String text) { throw new AssertionError(text); }
 public static void warning(String text) { warnings.add(text); }
}''',
    'org/dynmap/common/DynmapServerInterface.java': '''package org.dynmap.common;
import java.io.*;
import java.util.*;
public class DynmapServerInterface {
 public final Map<String, File> containers = new HashMap<String, File>();
 public final Map<String, InputStream> resources = new HashMap<String, InputStream>();
 public InputStream openResource(String mod, String name) { return resources.get(name); }
 public File getModContainerFile(String mod) { return containers.get(mod); }
}''',
}


def main():
    java_home = os.environ.get('JAVA_HOME')
    javac = str(Path(java_home) / 'bin/javac') if java_home else 'javac'
    java = str(Path(java_home) / 'bin/java') if java_home else 'java'
    with tempfile.TemporaryDirectory(prefix='gwm-texture-loader-') as directory:
        work = Path(directory)
        sources = []
        for name, text in STUBS.items():
            path = work / 'stubs' / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
            sources.append(str(path))
        sources.extend(str(ROOT / 'src/main/java/org/dynmap/hdmap' / name) for name in ['TextureResourceResolver.java', 'TexturePackLoader.java'])
        sources.append(str(ROOT / 'tools/tests/TexturePackLoaderBehaviorTest.java'))
        classes = work / 'classes'
        classes.mkdir()
        subprocess.run([javac, '--release', '8', '-d', str(classes), *sources], check=True)
        subprocess.run([java, '-cp', str(classes), 'TexturePackLoaderBehaviorTest', str(work)], check=True)


if __name__ == '__main__':
    main()
