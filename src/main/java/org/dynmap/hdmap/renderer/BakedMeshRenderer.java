package org.dynmap.hdmap.renderer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private RenderPatch[] mesh;
    private int textureCount;

    @Override
    public boolean initializeRenderer(RenderPatchFactory factory, int id, int mask, Map<String, String> parameters) {
        if (!super.initializeRenderer(factory, id, mask, parameters)) return false;
        String resource = parameters.get("mesh");
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalArgumentException("Missing baked model " + resource);
            JSONObject model = (JSONObject) new JSONParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            JSONArray atlas = (JSONArray) model.get("atlas");
            int columns = ((Number) atlas.get(0)).intValue(), rows = ((Number) atlas.get(1)).intValue();
            textureCount = columns * rows;
            List<RenderPatch> result = new ArrayList<>();
            for (Object partObject : (JSONArray) model.get("parts")) {
                JSONObject part = (JSONObject) partObject;
                for (Object triangleObject : (JSONArray) part.get("triangles")) {
                    JSONArray values = (JSONArray) triangleObject;
                    if (values.size() != 15) throw new IllegalArgumentException("Invalid baked triangle");
                    double[] source = new double[15];
                    for (int i = 0; i < 15; i++) source[i] = ((Number) values.get(i)).doubleValue();
                    for (AtlasMesh.Triangle triangle : AtlasMesh.split(source, columns, rows)) {
                        double[] p = triangle.vertices;
                        RenderPatch patch = factory.getTriangleExplTexCoords(
                                p[0], p[1], p[2], p[3], p[4],
                                p[5], p[6], p[7], p[8], p[9],
                                p[10], p[11], p[12], p[13], p[14],
                                RenderPatchFactory.SideVisible.TOP, triangle.texture);
                        if (patch != null) result.add(patch);
                    }
                }
            }
            mesh = result.toArray(new RenderPatch[result.size()]);
            return true;
        } catch (Exception invalid) {
            Log.severe("Unable to load baked model " + resource, invalid);
            return false;
        }
    }

    @Override
    public int getMaximumTextureCount() {
        return textureCount;
    }

    @Override
    public RenderPatch[] getRenderPatchList(MapDataContext context) {
        return mesh;
    }
}
