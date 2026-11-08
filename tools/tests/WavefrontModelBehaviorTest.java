import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.dynmap.hdmap.renderer.WavefrontModel;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.utils.PatchDefinition;
import org.dynmap.utils.PatchDefinitionFactory;

public class WavefrontModelBehaviorTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static WavefrontModel parse(String data) throws IOException {
        return WavefrontModel.read(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws Exception {
        String quad = "v 0 0 0\nv 0.25 0 0\nv 0.25 0.25 0\nv 0 0.25 0\n"
                + "vt 0 0\nvt 1 0\nvt 1 1\nvt 0 1\nf -4/-4 -3/-3 -2/-2 -1/-1\n";
        WavefrontModel model = parse(quad);
        check(model.triangleCount() == 2, "quad fan triangulation");
        PatchDefinitionFactory factory = new PatchDefinitionFactory();
        for (int yaw : new int[] { 0, 90, 180, 270 }) {
            RenderPatch[] patches = model.patches(factory, yaw, 0.5, 1, 0.5, 7);
            check(patches.length == 2, "valid rotated triangles");
            for (RenderPatch patch : patches) {
                check(patch.getTextureIndex() == 7, "atlas texture preserved");
                PatchDefinition p = (PatchDefinition) patch;
                check(Math.abs(p.x0 - 0.5) < 1e-9 && Math.abs(p.y0 - 1) < 1e-9
                        && Math.abs(p.z0 - 0.5) < 1e-9, "rotate first, then source translation");
            }
        }
        for (String invalid : new String[] { "", "v NaN 0 0", "v 0 0 0\nf 1 1 1",
                "v 0 0 0\nvt 0 0\nf 0/1 1/1 1/1", "v 0 0 0\nvt 0 0\nf 2/1 1/1 1/1" }) {
            try { parse(invalid); throw new AssertionError("bad OBJ accepted"); }
            catch (IOException expected) { }
        }
        if (args.length > 0) {
            WavefrontModel actual = WavefrontModel.read(new FileInputStream(args[0]));
            check(actual.triangleCount() > 20, "actual machine geometry");
            for (int yaw : new int[] { 0, 90, 180, 270 }) {
                RenderPatch[] patches = actual.patches(factory, yaw, 0.5, 1, 0.5, 0);
                check(patches.length == actual.triangleCount(), "actual model has no invalid or discarded patches");
            }
            System.out.println("Actual ice cream model: " + actual.triangleCount() + " UV triangles x four facings passed");
        }
        System.out.println("OBJ parser, negative indices, UVs, transforms and malformed-input checks passed");
    }
}
