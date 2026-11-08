package org.dynmap.hdmap.renderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** CPU-only OBJ geometry with explicit atlas UVs; no Minecraft client or GL linkage. */
public final class WavefrontModel {
    private final List<double[]> triangles;

    private WavefrontModel(List<double[]> triangles) {
        this.triangles = triangles;
    }

    public static WavefrontModel read(InputStream stream) throws IOException {
        return read(stream, null);
    }

    /** Read selected OBJ object/group names; null selects every face. */
    public static WavefrontModel read(InputStream stream, Set<String> groups) throws IOException {
        if (stream == null) throw new IOException("Missing OBJ model resource");
        List<double[]> vertices = new ArrayList<>(), uv = new ArrayList<>(), triangles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int number = 0;
            boolean selected = groups == null;
            while ((line = reader.readLine()) != null) {
                number++;
                line = line.split("#", 2)[0].trim();
                if (line.isEmpty()) continue;
                String[] words = line.split("\\s+");
                try {
                    if (words[0].equals("o") || words[0].equals("g")) {
                        selected = groups == null;
                        if (groups != null) for (int i = 1; i < words.length; i++)
                            if (groups.contains(words[i])) selected = true;
                    } else if (words[0].equals("v")) {
                        vertices.add(new double[] { finite(words[1]), finite(words[2]), finite(words[3]) });
                    } else if (words[0].equals("vt")) {
                        uv.add(new double[] { finite(words[1]), finite(words[2]) });
                    } else if (words[0].equals("f") && selected) {
                        if (words.length < 4) throw new IllegalArgumentException("Face needs three vertices");
                        double[][] face = new double[words.length - 1][];
                        for (int i = 1; i < words.length; i++) {
                            String[] refs = words[i].split("/", -1);
                            if (refs.length < 2 || refs[1].isEmpty())
                                throw new IllegalArgumentException("Atlas model requires explicit UV coordinates");
                            double[] point = vertices.get(index(refs[0], vertices.size()));
                            double[] tex = uv.get(index(refs[1], uv.size()));
                            face[i - 1] = new double[] { point[0], point[1], point[2], tex[0], tex[1] };
                        }
                        // The supported models use convex planar faces. Preserve
                        // OBJ winding when triangulating those faces as a fan.
                        for (int i = 1; i < face.length - 1; i++) {
                            double[] triangle = new double[15];
                            System.arraycopy(face[0], 0, triangle, 0, 5);
                            System.arraycopy(face[i], 0, triangle, 5, 5);
                            System.arraycopy(face[i + 1], 0, triangle, 10, 5);
                            if (hasArea(triangle)) triangles.add(triangle);
                        }
                    }
                } catch (IllegalArgumentException | IndexOutOfBoundsException badModel) {
                    throw new IOException("Invalid OBJ data at line " + number + ": " + line, badModel);
                }
            }
        }
        if (triangles.isEmpty()) throw new IOException("OBJ model contains no textured faces");
        return new WavefrontModel(triangles);
    }

    private static double finite(String value) {
        double parsed = Double.parseDouble(value);
        if (Double.isNaN(parsed) || Double.isInfinite(parsed))
            throw new IllegalArgumentException("Non-finite coordinate");
        return parsed;
    }

    private static boolean hasArea(double[] p) {
        double ux = p[5] - p[0], uy = p[6] - p[1], uz = p[7] - p[2];
        double vx = p[10] - p[0], vy = p[11] - p[1], vz = p[12] - p[2];
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        return nx * nx + ny * ny + nz * nz > 1e-20;
    }

    private static int index(String value, int size) {
        int index = Integer.parseInt(value);
        if (index == 0) throw new IllegalArgumentException("OBJ indices start at one");
        return index > 0 ? index - 1 : size + index;
    }

    /** Deep copy for offline source baking; mutations cannot affect the loaded model. */
    public List<double[]> triangles() {
        List<double[]> copy = new ArrayList<>();
        for (double[] triangle : triangles) copy.add(triangle.clone());
        return Collections.unmodifiableList(copy);
    }

    public int triangleCount() {
        return triangles.size();
    }

    public RenderPatch[] patches(RenderPatchFactory factory, int yaw, double x, double y, double z, int texture) {
        return patches(factory, yaw, x, y, z, texture, 1, 1);
    }

    public RenderPatch[] patches(RenderPatchFactory factory, int yaw, double x, double y, double z,
                                 int texture, int columns, int rows) {
        List<RenderPatch> patches = new ArrayList<>();
        double radians = Math.toRadians(yaw), cos = Math.cos(radians), sin = Math.sin(radians);
        for (double[] source : triangles) {
          for (AtlasMesh.Triangle cell : AtlasMesh.split(source, columns, rows)) {
            double[] p = cell.vertices.clone();
            for (int offset = 0; offset < 15; offset += 5) {
                p[offset] = x + cell.vertices[offset] * cos + cell.vertices[offset + 2] * sin;
                p[offset + 1] = y + cell.vertices[offset + 1];
                p[offset + 2] = z - cell.vertices[offset] * sin + cell.vertices[offset + 2] * cos;
            }
            RenderPatch patch = factory.getTriangleExplTexCoords(
                    p[0], p[1], p[2], p[3], p[4],
                    p[5], p[6], p[7], p[8], p[9],
                    p[10], p[11], p[12], p[13], p[14],
                    RenderPatchFactory.SideVisible.TOP, texture + cell.texture);
            if (patch != null) patches.add(patch);
          }
        }
        return patches.toArray(new RenderPatch[patches.size()]);
    }
}
