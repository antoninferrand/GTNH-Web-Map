import java.util.*;import java.lang.reflect.Proxy;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;import java.io.File;
import org.dynmap.hdmap.TexturePack;import org.dynmap.hdmap.renderer.AE2FCWalrusRenderer;
import org.dynmap.hdmap.textureprocessor.WalrusAtlasTextureProcessor;import org.dynmap.renderer.*;import org.dynmap.utils.*;
public class WalrusBehaviorTest {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 public static void main(String[] args)throws Exception {
  BufferedImage image=ImageIO.read(new File(args[0]));int[] pixels=image.getRGB(0,0,70,32,null,0,70);TexturePack pack=new TexturePack();int[] ids=new int[10];for(int i=0;i<10;i++)ids[i]=i;
  new WalrusAtlasTextureProcessor().patchTextures(pack,ids,pixels,70,32,16);
  for(int t=0;t<10;t++)for(int y=0;y<16;y++)for(int x=0;x<16;x++){int sx=t%5*16+x,sy=t/5*16+y;check(pack.tiles.get(t)[y*16+x]==(sx<70?image.getRGB(sx,sy):0),"Native atlas pixel lost/changed");}
  int[] hd=new int[140*64];for(int i=0;i<hd.length;i++)hd[i]=0xff000000|i;
  TexturePack highDefinition=new TexturePack();new WalrusAtlasTextureProcessor().patchTextures(highDefinition,ids,hd,140,64,32);
  for(int t=0;t<10;t++)for(int y=0;y<32;y++)for(int x=0;x<32;x++){int sx=t%5*32+x,sy=t/5*32+y;check(highDefinition.tiles.get(t)[y*32+x]==(sx<140?hd[sy*140+sx]:0),"HD atlas pixel lost/changed");}
  AE2FCWalrusRenderer renderer=new AE2FCWalrusRenderer();PatchDefinitionFactory factory=new PatchDefinitionFactory();check(renderer.initializeRenderer(factory,1,65535,Collections.emptyMap()),"walrus init");
  int count=-1;
  for(float scale:new float[]{1,.03f,3.5f,256})for(int facing=2;facing<=5;facing++) {
   final float size=scale;final int side=facing;
   MapDataContext ctx=(MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),new Class<?>[]{MapDataContext.class},(p,m,v)->{
    if(m.getName().equals("getBlockData"))return side;if(m.getName().equals("getBlockTileEntityField"))return size;if(m.getName().equals("getPatchFactory"))return factory;throw new AssertionError(m.getName());});
   RenderPatch[] patches=renderer.getRenderPatchList(ctx);if(count<0)count=patches.length;check(patches.length==count,"Source geometry clipped/lost for scale/facing");
   if(scale==1)check(patches==renderer.getRenderPatchList(ctx),"Default scale cache");
   double[] lo={1e30,1e30,1e30},hi={-1e30,-1e30,-1e30};
   for(RenderPatch patch:patches){check(patch.getTextureIndex()>=0&&patch.getTextureIndex()<10,"Walrus atlas index");PatchDefinition p=(PatchDefinition)patch;
    for(double[] v:new double[][]{{p.x0,p.y0,p.z0},{p.xu,p.yu,p.zu},{p.xv,p.yv,p.zv}})for(int i=0;i<3;i++){lo[i]=Math.min(lo[i],v[i]);hi[i]=Math.max(hi[i],v[i]);}
    for(double uv:p.explicitTexCoords)check(uv>=-1e-8&&uv<=1+1e-8,"Padded UV out of bounds");
   }
   double horizontalLow=.5-.530718*size,horizontalHigh=.5+.530718*size;
   double lengthLow=(side==2||side==4)?1-2.06773*size:-.046007*size,lengthHigh=(side==2||side==4)?1+.046007*size:2.06773*size;
   double[] expectedLow={side<4?horizontalLow:lengthLow,-.028822*size,side<4?lengthLow:horizontalLow};
   double[] expectedHigh={side<4?horizontalHigh:lengthHigh,.929365*size,side<4?lengthHigh:horizontalHigh};
   for(int i=0;i<3;i++)check(Math.abs(lo[i]-expectedLow[i])<1e-5&&Math.abs(hi[i]-expectedHigh[i])<1e-5,"Source scale/facing transform mismatch");
  }
  System.out.println("Walrus all4 facings at scales.03,1,3.5,256 preserve all"+count+" clipped atlas triangles; every original70x32 pixel retained");
 }
}
