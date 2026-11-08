import java.util.*;
import java.lang.reflect.Proxy;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.hdmap.renderer.BakedMeshRenderer;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.utils.PatchDefinition;
import org.dynmap.utils.PatchDefinitionFactory;

public class BakedMeshBehaviorTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static void main(String[] args) {
        for(String resource:args) {
            BakedMeshRenderer renderer=new BakedMeshRenderer();
            Map<String,String> settings=new HashMap<>();settings.put("mesh",resource);
            check(renderer.initializeRenderer(new PatchDefinitionFactory(),1,0xffff,settings),"Baked model failed: "+resource);
            RenderPatch[] patches=renderer.getRenderPatchList(null);
            check(patches.length>0,"No model geometry");
            if(renderer.getTileEntityFieldsNeeded().length>0) {
                Set<RenderPatch[]> observed=Collections.newSetFromMap(new IdentityHashMap<RenderPatch[],Boolean>());
                for(int input=0;input<6;input++)for(int output=0;output<6;output++) {
                    final int in=input,out=output;
                    MapDataContext context=(MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),
                            new Class<?>[]{MapDataContext.class},(proxy,method,values)-> {
                                if(method.getName().equals("getBlockTileEntityField"))return values[0].equals("inputFace")?in:out;
                                throw new AssertionError("Unexpected context dependency "+method.getName());
                            });
                    RenderPatch[] pose=renderer.getRenderPatchList(context);
                    check(pose.length==patches.length,"Pose changed source triangle coverage");
                    check(pose==renderer.getRenderPatchList(context),"Equivalent state did not reuse immutable mesh");
                    observed.add(pose);
                }
                check(observed.size()>=6,"Source direction poses are not being selected");
                System.out.println(resource+": all36 input/output combinations and pose-cache identity passed");
            }
            double[] low={100,100,100},high={-100,-100,-100};
            for(RenderPatch patch:patches) {
                check(patch.getTextureIndex()>=0 && patch.getTextureIndex()<renderer.getMaximumTextureCount(),"Atlas tile out of bounds");
                PatchDefinition p=(PatchDefinition)patch;
                double[][] points={{p.x0,p.y0,p.z0},{p.xu,p.yu,p.zu},{p.xv,p.yv,p.zv}};
                for(double[] point:points)for(int i=0;i<3;i++){
                    check(!Double.isNaN(point[i]) && !Double.isInfinite(point[i]),"Non-finite baked geometry");
                    low[i]=Math.min(low[i],point[i]);high[i]=Math.max(high[i],point[i]);
                }
                for(double uv:p.explicitTexCoords)check(uv>=-1e-8 && uv<=1+1e-8,"UV escaped clipped atlas cell");
            }
            check(low[0]>=-1e-8 && high[0]<=1+1e-8 && low[2]>=-1e-8 && high[2]<=1+1e-8,"Model exceeds expected block footprint");
            check(low[1]>=-1e-8 && high[1]<=1.5+1e-8,"Model height out of expected bounds");
            System.out.println(resource+": "+patches.length+" atlas-clipped patches, bounds "+Arrays.toString(low)+".."+Arrays.toString(high));
        }
    }
}
