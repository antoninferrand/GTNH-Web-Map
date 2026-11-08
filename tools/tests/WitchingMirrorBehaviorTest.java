import java.util.*;import java.lang.reflect.Proxy;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;import java.io.File;
import org.dynmap.hdmap.TexturePack;import org.dynmap.hdmap.renderer.WitchingWallMirrorRenderer;
import org.dynmap.hdmap.textureprocessor.WitchingMirrorGlassTextureProcessor;
import org.dynmap.renderer.*;import org.dynmap.utils.*;
public class WitchingMirrorBehaviorTest {
 public static void main(String[] args)throws Exception {
  BufferedImage image=ImageIO.read(new File(args[0]));int[] pixels=image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
  TexturePack pack=new TexturePack();int[] ids=new int[32];for(int i=0;i<32;i++)ids[i]=i;
  new WitchingMirrorGlassTextureProcessor().patchTextures(pack,ids,pixels,image.getWidth(),image.getHeight(),16);
  for(int tile=0;tile<32;tile++)for(int y=0;y<16;y++)for(int x=0;x<16;x++)
   if(pack.tiles.get(tile)[y*16+x]!=image.getRGB(tile%4*16+x,tile/4*16+y))throw new AssertionError("Source first-frame pixel loss");
  WitchingWallMirrorRenderer renderer=new WitchingWallMirrorRenderer();if(!renderer.initializeRenderer(new PatchDefinitionFactory(),1,65535,Collections.emptyMap()))throw new AssertionError("mirror init");
  for(int half=0;half<2;half++)for(int facing=2;facing<=5;facing++)for(int active=0;active<2;active++) {
   final int h=half,f=facing,a=active;
   MapDataContext ctx=(MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),new Class<?>[]{MapDataContext.class},(proxy,method,values)->{
    if(method.getName().equals("getBlockTileEntityFieldAt"))return (byte)a;
    if(method.getName().equals("getBlockTileEntityField"))return values[0].equals("facing")?f:values[0].equals("isDummy")?(byte)h:(byte)a;
    throw new AssertionError(method.getName());
   });
   CustomRendererData data=renderer.getRenderData(ctx);int pane=0;
   for(RenderPatch patch:data.getCustomMesh()) {
    int texture=patch.getTextureIndex();if(texture<32)pane++;
    if(texture<0||texture>=renderer.getMaximumTextureCount())throw new AssertionError("mirror texture bounds");
    if((data.getCustomColorMultiplier(texture,0)!=null)!=(texture>=32))throw new AssertionError("mirror isolated frame tint");
    PatchDefinition p=(PatchDefinition)patch;
    for(double u:new double[]{p.umin,p.umax})for(double vv:new double[]{p.vmin,p.vmax}) {
     if(u+vv>p.uplusvmax+1e-8)continue;
     double[] v={p.x0+u*(p.xu-p.x0)+vv*(p.xv-p.x0),p.y0+u*(p.yu-p.y0)+vv*(p.yv-p.y0),p.z0+u*(p.zu-p.z0)+vv*(p.zv-p.z0)};
     for(double coordinate:v)if(coordinate< -1e-8||coordinate>1+1e-8)throw new AssertionError("Mirror escaped real owner cell");
     double wall=f==2?1-v[2]:f==3?v[2]:f==4?1-v[0]:v[0];if(wall< -1e-8||wall>.0625+1e-8)throw new AssertionError("source mirror facing half="+h+" facing="+f+" v="+Arrays.toString(v));
    }
   }
   if((pane>0)!=(active==0))throw new AssertionError("Active mirror retained opaque glass");
  }
  System.out.println("All16 mirror half/facing/active states remain inside real helper cells;32 native glass cells match every actual source pixel");
 }
}
