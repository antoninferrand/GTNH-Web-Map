import java.util.*;
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
