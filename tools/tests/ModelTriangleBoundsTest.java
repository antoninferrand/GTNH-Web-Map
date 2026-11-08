import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;
import org.dynmap.renderer.RenderPatchFactory.SideVisible;
import org.dynmap.utils.PatchDefinition;
import org.dynmap.utils.PatchDefinitionFactory;

public class ModelTriangleBoundsTest {
    static int assertions;
    static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static RenderPatch triangle(RenderPatchFactory f,double[] p) {
        return f.getModelTriangleExplTexCoords(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8],p[9],p[10],p[11],p[12],p[13],p[14],SideVisible.TOP,7);
    }
    public static void main(String[] args)throws Throwable {
        PatchDefinitionFactory f=new PatchDefinitionFactory();
        double[] source={0,.2,-1.06773,.125,.25, 1,.2,-1.06773,.875,.25, 0,.9,1.046,.125,.75};
        for(double scale:new double[]{.03,1,16,256}) {
            double[] vertices=source.clone();for(int i=0;i<15;i+=5)for(int a=0;a<3;a++)vertices[i+a]*=scale;
            PatchDefinition patch=(PatchDefinition)triangle(f,vertices);check(patch!=null,"finite source model rejected at scale"+scale);
            check(patch.x0==vertices[0]&&patch.y0==vertices[1]&&patch.z0==vertices[2],"origin changed");
            check(patch.xu==vertices[5]&&patch.yu==vertices[6]&&patch.zu==vertices[7],"second vertex changed");
            check(patch.xv==vertices[10]&&patch.yv==vertices[11]&&patch.zv==vertices[12],"third vertex changed");
            check(Arrays.equals(patch.explicitTexCoords,new double[]{.125,.25,.875,.25,.125,.75}),"explicit UVs changed");
            check(patch.textureindex==7&&patch.sidevis==SideVisible.TOP&&patch.uplusvmax==1,"triangle attributes changed");
        }
        for(int coordinate=0;coordinate<15;coordinate++)for(double invalid:new double[]{Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY}) {
            double[] vertices=source.clone();vertices[coordinate]=invalid;check(triangle(f,vertices)==null,"nonfinite coordinate/UV accepted");
        }
        double[] collinear={0,0,0,0,0, 1,1,1,1,0, 2,2,2,0,1};check(triangle(f,collinear)==null,"degenerate triangle accepted");
        check(f.getModelTriangleExplTexCoords(0,0,0,0,0,1,0,0,1,0,0,1,0,0,1,null,0)==null,"null visibility accepted");
        check(f.getPatch(0,0,-1.06773,1,0,-1.06773,0,1,1.046,0,1,0,1,SideVisible.TOP,0)==null,"legacy block bounds relaxed");
        RenderPatch overhang=triangle(f,source);check(f.getRotatedPatch(overhang,0,0,0,7)==null,"legacy rotated bounds relaxed");
        double[] wrapped=source.clone();wrapped[3]=-0.25;wrapped[8]=1.25;
        check(((PatchDefinition)triangle(f,wrapped)).explicitTexCoords[0]==-.25,"legacy finite UV wrapping semantics changed");
        // Invoke the interface default on an old-style implementation that only handles the legacy triangle method.
        RenderPatch sentinel=f.getTriangleExplTexCoords(0,0,0,0,0,1,0,0,1,0,0,1,0,0,1,SideVisible.TOP,3);
        final int[] delegated={0};
        RenderPatchFactory legacy=(RenderPatchFactory)Proxy.newProxyInstance(RenderPatchFactory.class.getClassLoader(),new Class<?>[]{RenderPatchFactory.class},(proxy,method,values)->{
            if(method.getName().equals("getTriangleExplTexCoords")){delegated[0]++;return sentinel;}throw new AssertionError(method.getName());
        });
        Class<?>[] parameters=new Class<?>[17];Arrays.fill(parameters,0,15,double.class);parameters[15]=SideVisible.class;parameters[16]=int.class;
        MethodHandle defaultMethod=MethodHandles.lookup().findSpecial(RenderPatchFactory.class,"getModelTriangleExplTexCoords",MethodType.methodType(RenderPatch.class,parameters),RenderPatchFactory.class).bindTo(legacy);
        Object[] values=new Object[17];for(int i=0;i<15;i++)values[i]=source[i];values[15]=SideVisible.TOP;values[16]=7;
        check(defaultMethod.invokeWithArguments(values)==sentinel&&delegated[0]==1,"default implementation did not delegate to legacy API");
        System.out.println("Opt-in model triangle bounds: "+assertions+" production assertions passed");
    }
}
