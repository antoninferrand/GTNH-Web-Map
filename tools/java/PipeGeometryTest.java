import java.lang.reflect.Proxy;
import org.dynmap.hdmap.renderer.PipeRendererBase;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

public class PipeGeometryTest extends PipeRendererBase {
    private static class Patch implements RenderPatch {
        final double[] vertices;
        Patch(double[] vertices) { this.vertices = vertices; }
        public int getTextureIndex() { return 0; }
    }

    public static void main(String[] args) {
        RenderPatchFactory factory = (RenderPatchFactory) Proxy.newProxyInstance(
                PipeGeometryTest.class.getClassLoader(), new Class<?>[] { RenderPatchFactory.class },
                (proxy, method, values) -> {
                    if (!method.getName().equals("getPatch")) throw new AssertionError(method);
                    double[] p = new double[9];
                    for (int i = 0; i < p.length; i++) p[i] = ((Number) values[i]).doubleValue();
                    double ux = p[3] - p[0], uy = p[4] - p[1], uz = p[5] - p[2];
                    double vx = p[6] - p[0], vy = p[7] - p[1], vz = p[8] - p[2];
                    double area = Math.pow(uy * vz - uz * vy, 2)
                            + Math.pow(uz * vx - ux * vz, 2) + Math.pow(ux * vy - uy * vx, 2);
                    double umin = ((Number) values[9]).doubleValue();
                    double umax = ((Number) values[10]).doubleValue();
                    double vmin = ((Number) values[11]).doubleValue();
                    double vmax = ((Number) values[12]).doubleValue();
                    if (area == 0 || umin == umax || vmin == vmax) return null;
                    double[] corners = new double[12];
                    int offset = 0;
                    for (double u : new double[] { umin, umax }) {
                        for (double v : new double[] { vmin, vmax }) {
                            corners[offset++] = p[0] + u * ux + v * vx;
                            corners[offset++] = p[1] + u * uy + v * vy;
                            corners[offset++] = p[2] + u * uz + v * vz;
                        }
                    }
                    return new Patch(corners);
                });
        PipeGeometryTest renderer = new PipeGeometryTest();
        for (double diameter : new double[] { 0.125, 0.250, 0.375, 0.500, 0.600, 0.625, 0.750, 0.875 }) {
            double radius = diameter / 2;
            RenderPatch[][] meshes = renderer.generateSingleSize(factory, radius, 0, 0, 0);
            if (meshes.length != 64 || meshes[0].length != 6) throw new AssertionError("isolated cube");
            for (RenderPatch[] mesh : meshes) {
                if (mesh.length == 0) throw new AssertionError("empty connection mesh");
                for (RenderPatch patch : mesh) if (patch == null) throw new AssertionError("degenerate patch");
            }
            for (RenderPatch patch : meshes[0]) {
                for (double coordinate : ((Patch) patch).vertices) {
                    if (coordinate < 0.5 - radius - 1e-10 || coordinate > 0.5 + radius + 1e-10)
                        throw new AssertionError("isolated pipe exceeds its thickness");
                }
            }
            // Existing enlarged center fittings must retain their own dimensions.
            RenderPatch[][] fitting = renderer.generateSingleSize(factory, radius, 0.5, 0, 0);
            if (fitting[0].length != 6) throw new AssertionError("fitting changed");
        }
        System.out.println("Pipe geometry: eight diameters x 64 connection masks passed");
    }
}
