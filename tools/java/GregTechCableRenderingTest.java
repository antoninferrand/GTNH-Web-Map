import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.dynmap.hdmap.renderer.GregTechCableRenderer;
import org.dynmap.renderer.CustomRendererData;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Exercises the production cable mesh, endpoint texture and per-layer color logic. */
public class GregTechCableRenderingTest {
    private static int assertions;
    private static final double EPS = 1e-8;

    private static class Patch implements RenderPatch {
        final int face;
        final double[][] corners;
        final double[] normal;
        final double area;

        Patch(int face, double[][] corners, double[] normal, double area) {
            this.face = face;
            this.corners = corners;
            this.normal = normal;
            this.area = area;
        }

        public int getTextureIndex() { return face; }
    }

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static boolean between(double x, double min, double max) {
        return x >= min && x <= max;
    }

    private static boolean inside(double[] point, int mask, double min, double max) {
        double x = point[0], y = point[1], z = point[2];
        boolean cx = between(x, min, max), cy = between(y, min, max), cz = between(z, min, max);
        if (cx && cy && cz) return true;
        if (cx && cz && ((mask & 1) != 0 && between(y, 0, min) || (mask & 2) != 0 && between(y, max, 1))) return true;
        if (cx && cy && ((mask & 4) != 0 && between(z, 0, min) || (mask & 8) != 0 && between(z, max, 1))) return true;
        return cy && cz && ((mask & 16) != 0 && between(x, 0, min) || (mask & 32) != 0 && between(x, max, 1));
    }

    private static boolean expectedEnd(int mask, int direction) {
        if (mask == 0) return true;
        // A lone connection exposes a cap on both ends of that axis, including
        // the center cube's opposite face. Multiple connections use their mask.
        if (Integer.bitCount(mask) == 1) {
            int connected = Integer.numberOfTrailingZeros(mask);
            return direction / 2 == connected / 2;
        }
        return (mask & (1 << direction)) != 0;
    }

    private static RenderPatchFactory factory() {
        return (RenderPatchFactory) Proxy.newProxyInstance(
            GregTechCableRenderingTest.class.getClassLoader(), new Class<?>[] { RenderPatchFactory.class },
            (proxy, method, values) -> {
                if (!method.getName().equals("getPatch") || values.length != 15) throw new AssertionError(method);
                double[] origin = new double[3], u = new double[3], v = new double[3];
                for (int i = 0; i < 3; i++) {
                    origin[i] = ((Number) values[i]).doubleValue();
                    u[i] = ((Number) values[3 + i]).doubleValue() - origin[i];
                    v[i] = ((Number) values[6 + i]).doubleValue() - origin[i];
                }
                double[] normal = { u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0] };
                double crossArea = Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
                double umin = ((Number) values[9]).doubleValue(), umax = ((Number) values[10]).doubleValue();
                double vmin = ((Number) values[11]).doubleValue(), vmax = ((Number) values[12]).doubleValue();
                check(crossArea > 0 && umax > umin && vmax > vmin, "non-degenerate production patch");
                for (int i = 0; i < 3; i++) normal[i] /= crossArea;
                double[][] corners = new double[4][3];
                int n = 0;
                for (double a : new double[] { umin, umax }) {
                    for (double b : new double[] { vmin, vmax }) {
                        for (int i = 0; i < 3; i++) corners[n][i] = origin[i] + a * u[i] + b * v[i];
                        n++;
                    }
                }
                return new Patch(((Number) values[14]).intValue(), corners, normal, crossArea * (umax - umin) * (vmax - vmin));
            });
    }

    public static void main(String[] args) {
        GregTechCableRenderer renderer = new GregTechCableRenderer(factory());
        for (int diameter : new int[] { 125, 250, 375, 500, 600, 625, 750, 875, 1000 }) {
            double width = diameter / 1000.0, min = (1 - width) / 2, max = 1 - min;
            for (int mask = 0; mask < 64; mask++) {
                CustomRendererData plain = renderer.getRenderData(diameter, mask, 101, 202, 303, 0x404040, 0x323232);
                CustomRendererData painted = renderer.getRenderData(diameter, mask, 101, 202, 303, 0xF05555, 0x323232);
                check(plain.getCustomMesh() == painted.getCustomMesh(), "immutable cached mesh reused");
                check(plain.getCustomMesh().length == (diameter == 1000 ? 6 : 6 + 4 * Integer.bitCount(mask)), "union surface patch count");
                double area = 0;
                for (RenderPatch raw : plain.getCustomMesh()) {
                    Patch patch = (Patch) raw;
                    int axis = patch.face / 2 == 0 ? 1 : patch.face / 2 == 1 ? 2 : 0;
                    check(Math.abs(patch.normal[axis] - (patch.face % 2 == 0 ? -1 : 1)) < EPS, "face ID matches outward normal");
                    double[] center = new double[3];
                    for (double[] corner : patch.corners) {
                        for (int i = 0; i < 3; i++) {
                            check(corner[i] >= -EPS && corner[i] <= 1 + EPS, "cable stays within its block");
                            center[i] += corner[i] / 4;
                        }
                    }
                    double[] inward = center.clone(), outward = center.clone();
                    for (int i = 0; i < 3; i++) {
                        inward[i] -= patch.normal[i] * EPS;
                        outward[i] += patch.normal[i] * EPS;
                    }
                    check(inside(inward, mask, min, max), "surface borders cable interior");
                    check(!inside(outward, mask, min, max), "no hidden internal surfaces");
                    area += patch.area;
                }
                double expectedArea = 6 * width * width + 4 * Integer.bitCount(mask) * min * width;
                check(Math.abs(area - expectedArea) < EPS, "union surface area has no overlap or missing faces");
                for (int face = 0; face < 6; face++) {
                    boolean end = expectedEnd(mask, face);
                    int[] textures = plain.getCustomTextureMapper().getTextureLayersForPatchId(face);
                    check(Arrays.equals(textures, end ? new int[] { 202, 303 } : new int[] { 101 }), "wire and insulation end layers");
                    for (int layer = 0; layer < textures.length; layer++) {
                        int color = plain.getCustomColorMultiplier(face, layer).getColorMultiplier(null);
                        int paintedColor = painted.getCustomColorMultiplier(face, layer).getColorMultiplier(null);
                        if (end && layer == 0) {
                            check(color == 0x323232 && paintedColor == 0x323232, "paint must not recolor exposed wire");
                        } else {
                            check(color == 0x404040 && paintedColor == 0xF05555, "paint applies to insulation only");
                        }
                    }
                    check(plain.getCustomColorMultiplier(face, textures.length) == null, "invalid layer safely ignored");
                }
            }
        }
        CustomRendererData masked = renderer.getRenderData(250, 0xC1, 101, 202, 303, 1, 2);
        CustomRendererData clean = renderer.getRenderData(250, 1, 101, 202, 303, 1, 2);
        check(masked.getCustomMesh() == clean.getCustomMesh(), "non-direction connection bits masked");
        check(clean.getCustomTextureMapper().getTextureLayersForPatchId(6) == null, "invalid patch safely ignored");
        check(clean.getCustomColorMultiplier(-1, 0) == null, "invalid patch color safely ignored");
        System.out.println("Cable rendering: nine diameters x 64 masks; " + assertions + " assertions passed");
    }
}
