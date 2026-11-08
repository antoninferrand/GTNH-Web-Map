package org.dynmap.hdmap.renderer;

import java.util.ArrayList;
import java.util.List;

/** Clips textured geometry to atlas cells, retaining native pixel detail in ordinary GRID textures. */
public final class AtlasMesh {
    private static final double EPSILON = 1.0E-10;

    private AtlasMesh() {}

    public static final class Triangle {
        /** Three xyz/uv vertices. V uses RenderPatchFactory's bottom-origin convention. */
        public final double[] vertices;
        /** Row-major GRID index, with rows beginning at the top of the PNG. */
        public final int texture;

        private Triangle(double[] vertices, int texture) {
            this.vertices = vertices;
            this.texture = texture;
        }
    }

    /**
     * A source triangle has fifteen values: x,y,z,u,v for each vertex. UVs may
     * repeat outside 0..1 as with OBJ repeat wrapping. Texture cells use local UVs.
     */
    public static List<Triangle> split(double[] source, int columns, int rows) {
        if (source.length != 15 || columns < 1 || rows < 1) {
            throw new IllegalArgumentException("Expected an xyz/uv triangle and a positive atlas grid");
        }
        double minU = Double.POSITIVE_INFINITY, minV = minU;
        double maxU = Double.NEGATIVE_INFINITY, maxV = maxU;
        for (int i = 0; i < source.length; i++) {
            if (Double.isNaN(source[i]) || Double.isInfinite(source[i])) {
                throw new IllegalArgumentException("Non-finite mesh coordinate");
            }
        }
        List<double[]> triangle = new ArrayList<>();
        for (int i = 0; i < 15; i += 5) {
            double[] vertex = new double[5];
            System.arraycopy(source, i, vertex, 0, 5);
            triangle.add(vertex);
            minU = Math.min(minU, vertex[3]); maxU = Math.max(maxU, vertex[3]);
            minV = Math.min(minV, vertex[4]); maxV = Math.max(maxV, vertex[4]);
        }
        int u0 = (int) Math.floor(minU * columns), v0 = (int) Math.floor(minV * rows);
        int u1 = Math.max(u0, (int) Math.ceil(maxU * columns) - 1);
        int v1 = Math.max(v0, (int) Math.ceil(maxV * rows) - 1);
        if ((long) (u1 - u0 + 1) * (v1 - v0 + 1) > 65536L) {
            throw new IllegalArgumentException("Mesh UV span exceeds atlas clipping limit");
        }
        List<Triangle> result = new ArrayList<>();
        for (int v = v0; v <= v1; v++) {
            for (int u = u0; u <= u1; u++) {
                List<double[]> polygon = clip(triangle, 3, (double) u / columns, true);
                polygon = clip(polygon, 3, (double) (u + 1) / columns, false);
                polygon = clip(polygon, 4, (double) v / rows, true);
                polygon = clip(polygon, 4, (double) (v + 1) / rows, false);
                for (int i = 1; i + 1 < polygon.size(); i++) {
                    double[] a = polygon.get(0), b = polygon.get(i), c = polygon.get(i + 1);
                    if (areaSquared(a, b, c) < EPSILON * EPSILON) continue;
                    double[] data = new double[15];
                    double[][] vertices = { a, b, c };
                    for (int j = 0; j < 3; j++) {
                        System.arraycopy(vertices[j], 0, data, 5 * j, 3);
                        data[5 * j + 3] = clamp(vertices[j][3] * columns - u);
                        data[5 * j + 4] = clamp(vertices[j][4] * rows - v);
                    }
                    int texture = Math.floorMod(u, columns) + columns * (rows - 1 - Math.floorMod(v, rows));
                    result.add(new Triangle(data, texture));
                }
            }
        }
        return result;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static List<double[]> clip(List<double[]> source, int axis, double boundary, boolean above) {
        List<double[]> output = new ArrayList<>();
        if (source.isEmpty()) return output;
        double[] previous = source.get(source.size() - 1);
        boolean previousInside = above ? previous[axis] >= boundary - EPSILON : previous[axis] <= boundary + EPSILON;
        for (double[] current : source) {
            boolean inside = above ? current[axis] >= boundary - EPSILON : current[axis] <= boundary + EPSILON;
            if (inside != previousInside) {
                double t = (boundary - previous[axis]) / (current[axis] - previous[axis]);
                double[] intersection = new double[5];
                for (int i = 0; i < 5; i++) intersection[i] = previous[i] + t * (current[i] - previous[i]);
                intersection[axis] = boundary;
                output.add(intersection);
            }
            if (inside) output.add(current);
            previous = current;
            previousInside = inside;
        }
        return output;
    }

    private static double areaSquared(double[] a, double[] b, double[] c) {
        double x = (b[1] - a[1]) * (c[2] - a[2]) - (b[2] - a[2]) * (c[1] - a[1]);
        double y = (b[2] - a[2]) * (c[0] - a[0]) - (b[0] - a[0]) * (c[2] - a[2]);
        double z = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
        return x * x + y * y + z * z;
    }
}
