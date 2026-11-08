import java.util.HashMap;
import org.dynmap.hdmap.HDBlockModels;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;

public class RendererFailureBehaviorTest {
    public static class RequestedFields extends CustomRenderer {
        public RenderPatch[] getRenderPatchList(MapDataContext context) { return new RenderPatch[0]; }
        public String[] getTileEntityFieldsNeeded() { return new String[] { "fixtureField" }; }
    }

    public static void main(String[] args) {
        new HDBlockModels.CustomBlockModel(62000, 1, RequestedFields.class.getName(),
                new HashMap<String, String>(), "fixture");
        if (HDBlockModels.getTileEntityFieldsNeeded(62000, 0) == null)
            throw new AssertionError("fixture requesting fields was not registered");
        HDBlockModels.CustomBlockModel failed = new HDBlockModels.CustomBlockModel(62000, 1,
                "fixture.MissingCustomRenderer", new HashMap<String, String>(), "fixture");
        if (failed.getTextureCount() != 0) throw new AssertionError("failed model texture count");
        if (failed.getMeshForBlock(null).getCustomMesh().length != 0) throw new AssertionError("failed mesh");
        if (HDBlockModels.getTileEntityFieldsNeeded(62000, 0) != null)
            throw new AssertionError("stale requested-fields flag must not dereference a missing renderer");
        if (!HDBlockModels.getFailedCustomModels().containsKey(62000))
            throw new AssertionError("failed custom model must stay visible to the audit");
        System.out.println("Failed custom renderer keeps initialization/chunk-field checks usable");
    }
}
