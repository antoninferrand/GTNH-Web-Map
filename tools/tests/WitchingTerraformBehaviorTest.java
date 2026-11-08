import java.util.*;
import java.lang.reflect.Proxy;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.dynmap.hdmap.renderer.WitchingTerraformRenderer;
import org.dynmap.renderer.*;
import org.dynmap.utils.PatchDefinitionFactory;
public class WitchingTerraformBehaviorTest {
    static class Fixture extends WitchingTerraformRenderer {
        protected int[] sourceAspectColors(int id) {int[] colors=new int[16];for(int i=0;i<16;i++)colors[i]=0x010203+i;return colors;}
    }
    public static void main(String[] args) throws Exception {
        for(String kind:new String[]{"focus","terraformer"}) {
            Fixture renderer=new Fixture();Map<String,String> parameters=new HashMap<>();
            parameters.put("mesh","models/baked/witchinggadgets-"+kind+".json");
            if(!renderer.initializeRenderer(new PatchDefinitionFactory(),123,65535,parameters))throw new AssertionError("model init");
            JSONObject source=(JSONObject)new JSONParser().parse(new InputStreamReader(
                    WitchingTerraformBehaviorTest.class.getClassLoader().getResourceAsStream(parameters.get("mesh")),StandardCharsets.UTF_8));
            if(!Boolean.TRUE.equals(source.get("separatePartTextures")))throw new AssertionError("fixture requires source per-part texture contract");
            JSONArray sourceParts=(JSONArray)source.get("parts"),defaultAtlas=(JSONArray)source.get("atlas");
            int total=0;
            for(Object raw:sourceParts) {
                JSONObject part=(JSONObject)raw;JSONArray atlas=(JSONArray)part.get("atlas");if(atlas==null)atlas=defaultAtlas;
                total+=((Number)atlas.get(0)).intValue()*((Number)atlas.get(1)).intValue();
            }
            if(renderer.getMaximumTextureCount()!=total)throw new AssertionError("source native atlas slot count");
            for(int metadata=0;metadata<16;metadata++) {
                final int meta=metadata;
                MapDataContext context=(MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),new Class<?>[]{MapDataContext.class},(proxy,method,values)->{
                    if(method.getName().equals("getBlockData")||method.getName().equals("getBlockDataAt"))return meta;
                    if(method.getName().equals("getBlockTypeIDAt"))return 123;
                    throw new AssertionError(method.getName());
                });
                CustomRendererData data=renderer.getRenderData(context);
                int firstSlot=0;
                for(Object raw:sourceParts) {
                    JSONObject part=(JSONObject)raw;JSONArray atlas=(JSONArray)part.get("atlas");if(atlas==null)atlas=defaultAtlas;
                    int slots=((Number)atlas.get(0)).intValue()*((Number)atlas.get(1)).intValue();
                    String name=(String)part.get("name");boolean crystal=name.equals("focus_crystal_04")||name.equals("crystals_02");
                    for(int slot=firstSlot;slot<firstSlot+slots;slot++) {
                        CustomColorMultiplier tint=data.getCustomColorMultiplier(slot,0);
                        if(crystal?(tint==null||tint.getColorMultiplier(context)!=0x010203+meta):tint!=null)
                            throw new AssertionError("Source part tint mismatch "+name+" at native slot"+slot);
                    }
                    firstSlot+=slots;
                }
                if(data.getCustomMesh()!=renderer.getRenderPatchList(context))throw new AssertionError("Color polluted geometry cache");
            }
        }
        System.out.println("WitchingGadgets all16 focus/neighbor tint states across every source-declared native atlas cell, isolated crystal colors and shared geometry cache passed");
    }
}
