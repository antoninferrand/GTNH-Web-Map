package org.dynmap.hdmap.renderer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.dynmap.Log;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/** Server-safe, source-baked textured geometry with native atlas detail. */
public class BakedMeshRenderer extends CustomRenderer {
    private static final class Part {
        final List<AtlasMesh.Triangle> triangles = new ArrayList<>();
        final Map<Integer, double[]> rotations = new HashMap<>();
        String field;
        double[] pivot = { 0, 0, 0 }, fallback = { 0, 0, 0 };
    }

    private final List<Part> parts = new ArrayList<>();
    private final Map<String, RenderPatch[]> poses = new ConcurrentHashMap<>();
    private RenderPatchFactory factory;
    private String[] fields;
    private int textureCount;

    @Override
    public boolean initializeRenderer(RenderPatchFactory factory, int id, int mask, Map<String, String> parameters) {
        if (!super.initializeRenderer(factory, id, mask, parameters)) return false;
        this.factory = factory;
        parts.clear(); poses.clear();
        String resource = parameters.get("mesh");
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalArgumentException("Missing baked model " + resource);
            JSONObject model = (JSONObject) new JSONParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            JSONArray atlas = (JSONArray) model.get("atlas");
            int columns = ((Number) atlas.get(0)).intValue(), rows = ((Number) atlas.get(1)).intValue();
            textureCount = columns * rows;
            Set<String> requested = new LinkedHashSet<>();
            for (Object partObject : (JSONArray) model.get("parts")) {
                JSONObject data = (JSONObject) partObject;
                Part part = new Part();
                JSONObject pose = (JSONObject) data.get("pose");
                if (pose != null) {
                    part.field = (String) pose.get("field");
                    if (part.field == null || part.field.isEmpty()) throw new IllegalArgumentException("Missing pose field");
                    requested.add(part.field);
                    part.pivot = vector((JSONArray) data.get("pivot"));
                    part.fallback = vector((JSONArray) pose.get("default"));
                    JSONObject rotations = (JSONObject) pose.get("rotations");
                    for (Object key : rotations.keySet()) {
                        part.rotations.put(Integer.valueOf((String) key), vector((JSONArray) rotations.get(key)));
                    }
                }
                for (Object triangleObject : (JSONArray) data.get("triangles")) {
                    JSONArray values = (JSONArray) triangleObject;
                    if (values.size() != 15) throw new IllegalArgumentException("Invalid baked triangle");
                    double[] source = new double[15];
                    for (int i = 0; i < 15; i++) source[i] = ((Number) values.get(i)).doubleValue();
                    part.triangles.addAll(AtlasMesh.split(source, columns, rows));
                }
                parts.add(part);
            }
            fields = requested.toArray(new String[requested.size()]);
            return true;
        } catch (Exception invalid) {
            Log.severe("Unable to load baked model " + resource, invalid);
            return false;
        }
    }

    private static double[] vector(JSONArray data) {
        if (data == null || data.size() != 3) throw new IllegalArgumentException("Expected three pose coordinates");
        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            result[i] = ((Number) data.get(i)).doubleValue();
            if (Double.isNaN(result[i]) || Double.isInfinite(result[i])) throw new IllegalArgumentException("Invalid pose coordinate");
        }
        return result;
    }

    @Override
    public int getMaximumTextureCount() {
        return textureCount;
    }

    @Override
    public String[] getTileEntityFieldsNeeded() {
        return fields;
    }

    @Override
    public RenderPatch[] getRenderPatchList(MapDataContext context) {
        StringBuilder key = new StringBuilder();
        List<double[]> selected = new ArrayList<>();
        for (Part part : parts) {
            Object raw = part.field == null || context == null ? null : context.getBlockTileEntityField(part.field);
            int value = raw instanceof Number ? ((Number) raw).intValue() : 0;
            double[] rotation = part.rotations.get(value);
            key.append(rotation == null ? "d" : Integer.toString(value)).append(',');
            selected.add(rotation == null ? part.fallback : rotation);
        }
        String poseKey = key.toString();
        RenderPatch[] cached = poses.get(poseKey);
        if (cached != null) return cached;
        List<RenderPatch> result = new ArrayList<>();
        for (int index = 0; index < parts.size(); index++) {
            Part part = parts.get(index);
            double[] rotation = selected.get(index);
            for (AtlasMesh.Triangle triangle : part.triangles) {
                double[] p = triangle.vertices.clone();
                for (int i = 0; i < 15; i += 5) rotate(p, i, part.pivot, rotation);
                RenderPatch patch = factory.getTriangleExplTexCoords(
                        p[0], p[1], p[2], p[3], p[4],
                        p[5], p[6], p[7], p[8], p[9],
                        p[10], p[11], p[12], p[13], p[14],
                        RenderPatchFactory.SideVisible.TOP, triangle.texture);
                if (patch != null) result.add(patch);
            }
        }
        RenderPatch[] built = result.toArray(new RenderPatch[result.size()]);
        RenderPatch[] previous = poses.putIfAbsent(poseKey, built);
        return previous == null ? built : previous;
    }

    private static void rotate(double[] p, int index, double[] pivot, double[] angles) {
        double x = p[index] - pivot[0], y = p[index + 1] - pivot[1], z = p[index + 2] - pivot[2];
        double a = Math.toRadians(angles[0]);
        double nextY = y * Math.cos(a) - z * Math.sin(a);
        z = y * Math.sin(a) + z * Math.cos(a); y = nextY;
        a = Math.toRadians(angles[1]);
        double nextX = x * Math.cos(a) + z * Math.sin(a);
        z = -x * Math.sin(a) + z * Math.cos(a); x = nextX;
        a = Math.toRadians(angles[2]);
        p[index] = pivot[0] + x * Math.cos(a) - y * Math.sin(a);
        p[index + 1] = pivot[1] + x * Math.sin(a) + y * Math.cos(a);
        p[index + 2] = pivot[2] + z;
    }
}
