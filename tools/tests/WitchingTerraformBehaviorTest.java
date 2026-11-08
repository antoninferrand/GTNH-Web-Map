import java.util.*;
import java.lang.reflect.Proxy;
import org.dynmap.hdmap.renderer.WitchingTerraformRenderer;
import org.dynmap.renderer.*;
import org.dynmap.utils.PatchDefinitionFactory;
public class WitchingTerraformBehaviorTest {
    static class Fixture extends WitchingTerraformRenderer {
        protected int[] sourceAspectColors(int id) {int[] colors=new int[16];for(int i=0;i<16;i++)colors[i]=0x010203+i;return colors;}
    }
    public static void main(String[] args) {
        for(String kind:new String[]{"focus","terraformer"}) {
            Fixture renderer=new Fixture();Map<String,String> parameters=new HashMap<>();
            parameters.put("mesh","models/baked/witchinggadgets-"+kind+".json");
            if(!renderer.initializeRenderer(new PatchDefinitionFactory(),123,65535,parameters))throw new AssertionError("model init");
            for(int metadata=0;metadata<16;metadata++) {
                final int meta=metadata;
                MapDataContext context=(MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),new Class<?>[]{MapDataContext.class},(proxy,method,values)->{
                    if(method.getName().equals("getBlockData")||method.getName().equals("getBlockDataAt"))return meta;
                    if(method.getName().equals("getBlockTypeIDAt"))return 123;
                    throw new AssertionError(method.getName());
                });
                CustomRendererData data=renderer.getRenderData(context);
                if(data.getCustomColorMultiplier(0,0)!=null)throw new AssertionError("Base got crystal tint");
                if(data.getCustomColorMultiplier(8,0).getColorMultiplier(context)!=0x010203+meta)throw new AssertionError("Crystal tint metadata");
                if(data.getCustomMesh()!=renderer.getRenderPatchList(context))throw new AssertionError("Color polluted geometry cache");
            }
        }
        System.out.println("WitchingGadgets all16 focus/neighbor tint states, isolated crystal colors and shared geometry cache passed");
    }
}
