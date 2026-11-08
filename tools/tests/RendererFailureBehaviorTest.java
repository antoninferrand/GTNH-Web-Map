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

    public static class MissingClientDependency extends RequestedFields {
        @Override
        public boolean initializeRenderer(org.dynmap.renderer.RenderPatchFactory factory, int id, int mask,
                java.util.Map<String, String> parameters) {
            throw new NoClassDefFoundError("net/minecraft/client/renderer/texture/IIconRegister");
        }
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
        HDBlockModels.CustomBlockModel linkage = new HDBlockModels.CustomBlockModel(62001, 1,
                MissingClientDependency.class.getName(), new HashMap<String, String>(), "fixture");
        if (linkage.getTextureCount() != 0 || linkage.getMeshForBlock(null).getCustomMesh().length != 0)
            throw new AssertionError("Linkage failure must retain an explicit failed empty model");
        if (!HDBlockModels.getFailedCustomModels().containsKey(62001))
            throw new AssertionError("Client dependency failure must remain visible in the audit");
        System.out.println("Failed custom renderer and missing client dependency preserve initialization and audit evidence");
    }
}
