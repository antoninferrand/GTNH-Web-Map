import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.dynmap.hdmap.renderer.WitchingWallMirrorRenderer;
import org.dynmap.renderer.*;
import org.dynmap.utils.PatchDefinition;
import org.dynmap.utils.PatchDefinitionFactory;

/** Source pixel-grid frame, palette colors and saved active/dummy/facing state. */
public class WitchingWallMirrorBehaviorTest {
    static int assertions;
    static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static MapDataContext context(final int half,final int facing,final boolean active) {
        return (MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),new Class<?>[]{MapDataContext.class},(proxy,method,args)->{
            if(method.getName().equals("getBlockTileEntityField")) {
                if(args[0].equals("isDummy"))return half;
                if(args[0].equals("facing"))return facing;
                if(args[0].equals("isActive")){check(half==0,"dummy must obtain active state from lower block");return active;}
            }
            if(method.getName().equals("getBlockTileEntityFieldAt")) {
                check(half==1&&args[0].equals("isActive")&&args[1].equals(0)&&args[2].equals(-1)&&args[3].equals(0),"wrong saved-state neighbor");return active;
            }
            throw new AssertionError("unexpected world/context access "+method.getName());
        });
    }
    public static void main(String[] args)throws Exception {
        JSONObject source=(JSONObject)new JSONParser().parse(new InputStreamReader(WitchingWallMirrorBehaviorTest.class.getClassLoader().getResourceAsStream("models/baked/witchinggadgets-mirror-frame.json"),StandardCharsets.UTF_8));
        JSONArray shape=(JSONArray)source.get("shape"),palette=(JSONArray)source.get("colors");int occupied=0;
        for(Object row:shape)for(Object cell:(JSONArray)row)if(((Number)cell).intValue()>=0)occupied++;
        // Each nonempty source half-row pixel has a mirrored partner, each rendered as a six-face cuboid.
        int frameCount=occupied*12;
        WitchingWallMirrorRenderer renderer=new WitchingWallMirrorRenderer();
        check(renderer.initializeRenderer(new PatchDefinitionFactory(),123,65535,Collections.emptyMap()),"renderer init");
        check(renderer.getMaximumTextureCount()==32+palette.size(),"source glass atlas + frame palette slots");
        double[][] centers=new double[2][4];
        for(int half=0;half<2;half++)for(int facing=2;facing<=5;facing++)for(boolean active:new boolean[]{false,true}) {
            MapDataContext ctx=context(half,facing,active);CustomRendererData data=renderer.getRenderData(ctx);RenderPatch[] mesh=data.getCustomMesh();
            check(active?mesh.length==frameCount:mesh.length>frameCount,"active mirror hides only glass, retaining every source frame pixel");
            if(active)check(mesh==renderer.getRenderPatchList(ctx),"immutable frame cache not reused");
            double totalY=0;int framePatches=0,glassPatches=0;
            for(RenderPatch raw:mesh) {
                PatchDefinition patch=(PatchDefinition)raw;int texture=patch.getTextureIndex();
                check(texture>=0&&texture<renderer.getMaximumTextureCount(),"patch outside source atlas/palette slots");
                for(double coordinate:new double[]{patch.x0,patch.y0,patch.z0,patch.xu,patch.yu,patch.zu,patch.xv,patch.yv,patch.zv})
                    check(coordinate>=-1e-12&&coordinate<=1+1e-12,"frame/dummy patch escaped its owner cell");
                CustomColorMultiplier tint=data.getCustomColorMultiplier(texture,0);
                if(texture>=32) {
                    framePatches++;totalY+=patch.y0+patch.yu+patch.yv;
                    check(tint!=null&&tint.getColorMultiplier(ctx)==((Number)palette.get(texture-32)).intValue(),"source frame palette color changed");
                } else {
                    glassPatches++;check(tint==null,"glass received frame palette tint");
                    for(double uv:patch.explicitTexCoords)check(uv>=0&&uv<=1,"glass UV left source atlas cell");
                }
            }
            check(framePatches==frameCount,"lost/duplicated source frame cuboids");
            check(active?glassPatches==0:glassPatches>0,"active saved state does not select glass visibility");
            if(active)centers[half][facing-2]=totalY/(framePatches*3);
        }
        for(int facing=0;facing<4;facing++)check(Math.abs(centers[0][facing]+centers[1][facing]-1)<1e-12,"upper dummy frame is not vertical source mirror");
        System.out.println("Wall mirror: "+assertions+" source pixel/palette and all16 saved-state assertions passed");
    }
}
