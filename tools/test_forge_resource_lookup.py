"""Exercise the production Forge resource lookup body with minimal loader fixtures.

Extracting the actual method avoids requiring a running Minecraft class loader;
only Forge's container/loader interfaces are replaced by deterministic test data.
"""
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


def production_method():
    text = (ROOT / "src/main/java/org/dynmap/forge/DynmapPlugin.java").read_text()
    start = text.index("public InputStream openResource(String modid, String rname)")
    brace = text.index("{", start)
    depth = 1
    end = brace + 1
    while depth:
        depth += (text[end] == "{") - (text[end] == "}")
        end += 1
    return text[start:end]


JAVA = r'''
import java.io.*;
import java.util.*;

public class ForgeResourceLookupTest {
    static final class FixtureMod { }
    static final class ModContainer {
        private final Object mod;
        int calls;
        ModContainer(Object mod) { this.mod = mod; }
        Object getMod() { calls++; return mod; }
    }
    static final class Loader {
        private static final Loader INSTANCE = new Loader();
        final Map<String, ModContainer> indexed = new HashMap<String, ModContainer>();
        final List<ModContainer> loaded = new ArrayList<ModContainer>();
        static Loader instance() { return INSTANCE; }
        Map<String, ModContainer> getIndexedModList() { return indexed; }
        List<ModContainer> getModList() { return loaded; }
    }
    static final class Server {
        METHOD_BODY
    }
    static int assertions;
    static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    static void found(InputStream stream, String message) throws IOException {
        check(stream != null, message);
        try (InputStream in = stream) {
            check(in.read() == 42 && in.read() == -1, message + " contents");
        }
    }
    public static void main(String[] args) throws Exception {
        Server server = new Server();
        Loader loader = Loader.instance();
        ModContainer indexed = new ModContainer(new FixtureMod());
        ModContainer fallback = new ModContainer(new FixtureMod());
        loader.indexed.put("valid-owner", indexed);
        loader.loaded.add(fallback);
        found(server.openResource("valid-owner", "fixture-resource.bin"), "registered owner lookup");
        check(indexed.calls == 1 && fallback.calls == 0, "registered owner returns before global fallback");
        found(server.openResource("unregistered-domain", "fixture-resource.bin"), "unknown owner uses loaded-mod search");
        check(fallback.calls == 1, "global fallback inspected when owner is absent");
        check(server.openResource("unregistered-domain", "missing-required.bin") == null, "required miss remains unresolved");
        found(server.openResource(null, "fixture-resource.bin"), "null owner global lookup");
        loader.indexed.put("bootstrap-owner", new ModContainer(null));
        found(server.openResource("bootstrap-owner", "fixture-resource.bin"), "null mod instance continues global search");
        loader.loaded.clear();
        check(server.openResource("unregistered-domain", "fixture-resource.bin") == null, "no registered fallback remains unresolved");
        check(server.openResource(null, "missing-required.bin") == null, "null-owner miss remains unresolved");
        System.out.println("Forge resource lookup: " + assertions + " assertions passed");
    }
}
'''


class ForgeResourceLookupTest(unittest.TestCase):
    def test_registered_and_absent_owners(self):
        with tempfile.TemporaryDirectory(prefix="gwm-forge-resource-test-") as temporary:
            work = Path(temporary)
            source = work / "ForgeResourceLookupTest.java"
            source.write_text(JAVA.replace("METHOD_BODY", production_method()))
            (work / "fixture-resource.bin").write_bytes(bytes([42]))
            subprocess.run(["javac", "--release", "8", "-d", str(work), str(source)], check=True)
            subprocess.run(["java", "-cp", str(work), "ForgeResourceLookupTest"], check=True)


if __name__ == "__main__":
    unittest.main()
