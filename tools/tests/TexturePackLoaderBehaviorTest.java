import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.dynmap.DynmapCore;
import org.dynmap.Log;
import org.dynmap.common.DynmapServerInterface;
import org.dynmap.hdmap.TexturePackLoader;
import org.dynmap.hdmap.TextureResourceResolver;

public class TexturePackLoaderBehaviorTest {
    private static final String ORIGINAL = "assets/gregtech/textures/blocks/materialicons/DULL/pipeSide.png";
    private static final String FALLBACK = "assets/gregtech/textures/blocks/materialicons/NONE/pipeSide.png";
    private static int assertions;

    static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    static String read(TexturePackLoader loader, InputStream stream) throws IOException {
        if (stream == null) return null;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[64];
            int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return new String(bytes.toByteArray(), "UTF-8");
        } finally {
            loader.closeResource(stream);
        }
    }

    static void file(Path root, String name, String value) throws IOException {
        Path file = root.resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, value.getBytes("UTF-8"));
    }

    static void zip(Path target, Map<String, String> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes("UTF-8"));
                zip.closeEntry();
            }
        }
    }

    static TexturePackLoader loader(Path pack, DynmapServerInterface server) {
        return new TexturePackLoader(pack.toFile(), new DynmapCore(server));
    }

    static TextureResourceResolver.Lookup last(TexturePackLoader loader) {
        List<TextureResourceResolver.Lookup> records = new ArrayList<TextureResourceResolver.Lookup>(loader.getResourceLookups());
        return records.get(records.size() - 1);
    }

    public static void main(String[] args) throws Exception {
        Path work = Paths.get(args[0]);
        Path pack = Files.createDirectories(work.resolve("pack"));
        Path mod = Files.createDirectories(work.resolve("mod"));
        DynmapServerInterface server = new DynmapServerInterface();
        server.containers.put("gregtech", mod.toFile());
        file(mod, ORIGINAL, "material-original");
        file(mod, FALLBACK, "material-fallback");
        file(pack, ORIGINAL, "pack-original");
        TexturePackLoader loader = loader(pack, server);
        check("pack-original".equals(read(loader, loader.openModTPResource(ORIGINAL, "gregtech", FALLBACK, null, false))), "pack original overrides mod original");
        Files.delete(pack.resolve(ORIGINAL));
        file(pack, FALLBACK, "pack-fallback");
        check("material-original".equals(read(loader, loader.openModTPResource(ORIGINAL, "gregtech", FALLBACK, null, false))), "existing material original wins over fallback even in pack");
        Files.delete(mod.resolve(ORIGINAL));
        check("pack-fallback".equals(read(loader, loader.openModTPResource(ORIGINAL, "gregtech", FALLBACK, null, false))), "pack overrides fallback image");
        check(last(loader).outcome == TextureResourceResolver.Outcome.FALLBACK, "fallback diagnostic");
        check(FALLBACK.equals(last(loader).resolvedResource), "resolved path diagnostic");
        check("gregtech".equals(last(loader).resolvedMod), "default fallback owner");
        check(Log.warnings.isEmpty(), "successful fallback does not produce a missing-resource warning");
        Files.delete(pack.resolve(FALLBACK));
        check("material-fallback".equals(read(loader, loader.openModTPResource(ORIGINAL, "gregtech", FALLBACK, null, false))), "mod fallback available");
        check(loader.openModTPResource(ORIGINAL, "gregtech") == null, "required resource gets no undeclared fallback");
        check(Log.warnings.size() == 1 && Log.warnings.get(0).contains(ORIGINAL), "required miss still warns");
        check(last(loader).outcome == TextureResourceResolver.Outcome.REQUIRED_MISSING, "required miss diagnostic");
        Log.warnings.clear();
        check(loader.openModTPResource("optional.png", "gregtech", null, null, true) == null, "optional miss is absent");
        check(last(loader).outcome == TextureResourceResolver.Outcome.OPTIONAL_ABSENT, "optional diagnostic distinct from required miss");
        check(Log.warnings.isEmpty(), "optional absence does not warn");
        file(pack, "optional.png", "pack-optional");
        check("pack-optional".equals(read(loader, loader.openModTPResource("optional.png", "gregtech", null, null, true))), "pack can supply optional texture");
        check(last(loader).outcome == TextureResourceResolver.Outcome.ORIGINAL, "supplied optional resource classified original");
        Path otherJar = work.resolve("other.jar");
        zip(otherJar, Collections.singletonMap("other-fallback.png", "other-owner"));
        server.containers.put("other", otherJar.toFile());
        check("other-owner".equals(read(loader, loader.openModTPResource("missing.png", "gregtech", "other-fallback.png", "other", false))), "explicit fallback owner jar");
        check("other".equals(last(loader).resolvedMod), "fallback owner diagnostic");
        check("other-owner".equals(read(loader, loader.openModTPResource("other-fallback.png", "other"))), "explicit original owner jar");
        final boolean[] closed = { false };
        server.resources.put("stream.png", new ByteArrayInputStream(new byte[] { 1 }) {
            public void close() throws IOException { closed[0] = true; super.close(); }
        });
        InputStream stream = loader.openModTPResource("stream.png", "gregtech");
        check(!closed[0], "lookup does not close stream before decoding");
        loader.closeResource(stream);
        check(closed[0], "caller can close resolved stream");
        Collection<TextureResourceResolver.Lookup> snapshot = loader.getResourceLookups();
        boolean immutable = false;
        try { snapshot.clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "lookup snapshot immutable");
        loader.close();
        check(loader.getResourceLookups().isEmpty(), "close releases diagnostics");
        check(!snapshot.isEmpty(), "previous snapshot independent of close");
        Path packed = work.resolve("pack.zip");
        zip(packed, Collections.singletonMap(ORIGINAL, "zip-pack"));
        loader = loader(packed, server);
        check("zip-pack".equals(read(loader, loader.openModTPResource(ORIGINAL, "gregtech", FALLBACK, null, false))), "ZIP resource pack original override");
        loader.close();
        System.out.println("Texture loader behavior: " + assertions + " assertions passed");
    }
}
