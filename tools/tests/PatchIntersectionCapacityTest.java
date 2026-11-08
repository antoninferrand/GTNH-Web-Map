import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import org.dynmap.ConfigurationNode;
import org.dynmap.hdmap.HDBlockModels;
import org.dynmap.hdmap.HDPerspectiveState;
import org.dynmap.hdmap.HDShaderState;
import org.dynmap.hdmap.IsoHDPerspective;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory.SideVisible;
import org.dynmap.utils.BlockStep;
import org.dynmap.utils.MapIterator;
import org.dynmap.utils.PatchDefinitionFactory;
import org.dynmap.utils.Vector3D;

public class PatchIntersectionCapacityTest {
    private static int assertions;
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
    private static Object state(IsoHDPerspective perspective, Class<?> type) throws Exception {
        MapIterator iterator = (MapIterator) Proxy.newProxyInstance(MapIterator.class.getClassLoader(),
                new Class<?>[] {MapIterator.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getWorldHeight")) return 256;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
        Constructor<?> constructor = type.getDeclaredConstructor(IsoHDPerspective.class, MapIterator.class,
                boolean.class, int.class);
        constructor.setAccessible(true);
        Object result = constructor.newInstance(perspective, iterator, false, 0);
        field(type, "top").set(result, new Vector3D(.25, .25, -1));
        field(type, "direction").set(result, new Vector3D(0, 0, 1));
        field(type, "laststep").set(result, BlockStep.Y_MINUS);
        return result;
    }
    private static int[] trace(Method handler, Object state, RenderPatch[] patches, int stopAfter) throws Exception {
        final int[] hits = {0};
        HDShaderState shader = (HDShaderState) Proxy.newProxyInstance(HDShaderState.class.getClassLoader(),
                new Class<?>[] {HDShaderState.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("processBlock")) return null;
                    HDPerspectiveState ray = (HDPerspectiveState) args[0];
                    check(ray.getTextureIndex() == hits[0] % 16, "patch order/texture index changed");
                    check(Math.abs(ray.getPatchU() - .25) < 1e-12, "intercept U changed");
                    check(Math.abs(ray.getPatchV() - .25) < 1e-12, "intercept V changed");
                    check(ray.getLastBlockStep() == BlockStep.Z_PLUS, "intercept normal changed");
                    hits[0]++;
                    return stopAfter > 0 && hits[0] == stopAfter;
                });
        boolean[] done = {false};
        boolean completed = (Boolean) handler.invoke(state, patches, new HDShaderState[] {shader}, done);
        check(completed == (stopAfter > 0), "shader completion result changed");
        check(done[0] == completed, "shader completion flag changed");
        check(field(state.getClass(), "laststep").get(state) == BlockStep.Y_MINUS, "previous step not restored");
        check(field(state.getClass(), "cur_patch").getInt(state) == -1, "current patch not cleared");
        return hits;
    }
    public static void main(String[] args) throws Exception {
        // Keep the global texture-slot estimate tiny while geometry grows independently.
        Field maximum = field(HDBlockModels.class, "max_patches");
        int previous = maximum.getInt(null);
        maximum.setInt(null, 16);
        try {
            ConfigurationNode config = new ConfigurationNode();
            config.put("name", "capacity-test");
            IsoHDPerspective perspective = new IsoHDPerspective(null, config);
            Class<?> stateType = Class.forName("org.dynmap.hdmap.IsoHDPerspective$OurPerspectiveState");
            Object first = state(perspective, stateType), second = state(perspective, stateType);
            Method handler = stateType.getDeclaredMethod("handlePatches", RenderPatch[].class,
                    HDShaderState[].class, boolean[].class);
            handler.setAccessible(true);
            PatchDefinitionFactory factory = new PatchDefinitionFactory();
            RenderPatch[] patches = new RenderPatch[2048];
            for (int i = 0; i < patches.length; i++) {
                // All 2,048 patches intersect the same ray; only 16 texture slots.
                patches[i] = factory.getPatch(0, 0, .5, 1, 0, .5, 0, 1, .5,
                        0, 1, 0, 1, SideVisible.BOTH, i % 16);
            }
            check(trace(handler, first, patches, 0)[0] == patches.length, "not every coincident patch reached shader");
            String[] buffers = {"patch_t", "patch_u", "patch_v", "patch_step", "patch_id"};
            Object[] retained = new Object[buffers.length];
            for (int i = 0; i < buffers.length; i++) {
                retained[i] = field(stateType, buffers[i]).get(first);
                check(java.lang.reflect.Array.getLength(retained[i]) >= patches.length, "parallel buffer not grown: " + buffers[i]);
                check(java.lang.reflect.Array.getLength(field(stateType, buffers[i]).get(second)) == 16,
                        "another perspective state's buffer changed: " + buffers[i]);
            }
            check(trace(handler, first, Arrays.copyOf(patches, 64), 3)[0] == 3, "early shader completion changed");
            for (int i = 0; i < buffers.length; i++)
                check(retained[i] == field(stateType, buffers[i]).get(first), "smaller mesh unnecessarily reallocates");
            RenderPatch[] larger = Arrays.copyOf(patches, 3072);
            for (int i = patches.length; i < larger.length; i++) larger[i] = patches[i % patches.length];
            check(trace(handler, first, larger, 0)[0] == larger.length, "second growth lost intersections");
            for (String buffer : buffers)
                check(java.lang.reflect.Array.getLength(field(stateType, buffer).get(first)) >= larger.length,
                        "second growth missed buffer: " + buffer);
            check(trace(handler, first, new RenderPatch[0], 0)[0] == 0, "empty mesh changed");
        } finally {
            maximum.setInt(null, previous);
        }
        System.out.println("Patch intersections: " + assertions + " production assertions passed");
    }
}
