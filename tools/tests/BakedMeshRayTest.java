import java.util.*;import java.lang.reflect.Proxy;
import org.dynmap.hdmap.renderer.BakedMeshRenderer;import org.dynmap.hdmap.renderer.WitchingWallMirrorRenderer;
import org.dynmap.renderer.*;import org.dynmap.utils.*;
/** Exact determinant/barycentric acceptance from IsoHDPerspective.handlePatches. */
public class BakedMeshRayTest {
 static double dot(double[]a,double[]b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
 static double[] cross(double[]a,double[]b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
 static boolean hit(PatchDefinition p,double[] origin,double[] direction) {
  double[] u={p.xu-p.x0,p.yu-p.y0,p.zu-p.z0},v={p.xv-p.x0,p.yv-p.y0,p.zv-p.z0};
  double[] dCrossV=cross(direction,v);double det=dot(u,dCrossV);
  switch(p.sidevis){case TOP:if(det<.000001)return false;break;case BOTTOM:if(det>-.000001)return false;break;default:if(det>-.000001&&det<.000001)return false;}
  double inv=1/det;double[] offset={origin[0]-p.x0,origin[1]-p.y0,origin[2]-p.z0};double pu=inv*dot(offset,dCrossV);
  if(pu<=p.umin||pu>=p.umax)return false;
  double[] sCrossU=cross(offset,u);double pv=inv*dot(direction,sCrossU);
  if(pv<=p.vmin||pv>=p.vmax||pu+pv>=p.uplusvmax)return false;
  double t=inv*dot(v,sCrossU);if(t<=.000001)return false;
  for(int i=0;i<3;i++){double coordinate=origin[i]+direction[i]*t;if(coordinate<0||coordinate>1)return false;}
  return true;
 }
 public static void main(String[] args) {
  for(String resource:args) {
   CustomRenderer renderer=resource.equals("mirror")?new WitchingWallMirrorRenderer():new BakedMeshRenderer();Map<String,String> parameters=new HashMap<>();parameters.put("mesh",resource);
   if(!renderer.initializeRenderer(new PatchDefinitionFactory(),1,65535,parameters))throw new AssertionError("Model init");
   MapDataContext context=(MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),new Class<?>[]{MapDataContext.class},(p,m,v)->{
    if(m.getName().equals("getBlockData"))return 1;
    if(m.getName().equals("getBlockTileEntityFieldAt"))return (byte)0;
    if(m.getName().equals("getBlockTileEntityField")){String name=(String)v[0];if(name.equals("isDummy")||name.equals("isActive"))return (byte)0;if(name.equals("Items"))return Collections.emptyList();return name.equals("outputFace")?3:2;}
    throw new AssertionError(m.getName());});
   RenderPatch[] patches=renderer.getRenderPatchList(context);int hits=0;
   for(int sx=0;sx<24;sx++)for(int sz=0;sz<24;sz++) {
    double x=(sx+.37)/24,z=(sz+.61)/24;
    for(double[] ray:new double[][]{{x+1.5,3,z+1.5,-.5,-1,-.5},{3,x,z+1.5,-1,0,-.5},{x+1.5,z,3,-.5,0,-1}}) {
     double[] origin={ray[0],ray[1],ray[2]},direction={ray[3],ray[4],ray[5]};
     for(RenderPatch patch:patches)if(hit((PatchDefinition)patch,origin,direction)){hits++;break;}
    }
   }
   if(hits==0)throw new AssertionError("No source model triangles visible through actual Iso acceptance: "+resource);
   System.out.println(resource+": "+hits+" owner-cell ray hits using actual Iso determinant/UV bounds");
  }
 }
}
