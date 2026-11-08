#!/usr/bin/env python3
"""Compare liquid heights against the actual Minecraft source method and exercise real patches."""
import argparse,pathlib,subprocess,tempfile,zipfile
r=pathlib.Path(__file__).resolve().parents[1];p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--minecraft-sources',type=pathlib.Path,required=True);p.add_argument('--compile-classpath',type=pathlib.Path,required=True);p.add_argument('--runtime-classpath',type=pathlib.Path,required=True);p.add_argument('--classes',type=pathlib.Path,required=True);a=p.parse_args()
with zipfile.ZipFile(a.minecraft_sources) as z:
    src=z.read('net/minecraft/client/renderer/RenderBlocks.java').decode();start=src.index('public float getLiquidHeight(');end=src.index('\n    public ',start+10);method=src[start:end].replace('this.blockAccess','blockAccess')
java='''import java.util.*;import java.lang.reflect.Proxy;
import org.dynmap.hdmap.renderer.VanillaWaterSurfaceRenderer;import org.dynmap.renderer.*;import org.dynmap.utils.*;
public class WaterSurfaceBehaviorTest {
 static class Material {int id;Material(int i){id=i;}boolean isSolid(){return id==2;}}
 static final Material AIR=new Material(0),WATER=new Material(1),SOLID=new Material(2);
 static class Block {int id;Block(int i){id=i;}Material getMaterial(){return id==1?WATER:id==2?SOLID:AIR;}}
 static class BlockLiquid {static float getLiquidHeightPercent(int metadata){if(metadata>=8)metadata=0;return (metadata+1)/9.0F;}}
 static class Snapshot {int[][][] ids=new int[5][5][5],metas=new int[5][5][5];Block getBlock(int x,int y,int z){return new Block(ids[x+2][y+2][z+2]);}int getBlockMetadata(int x,int y,int z){return metas[x+2][y+2][z+2];}}
 Snapshot blockAccess;
 METHOD
 static class Fixture extends VanillaWaterSurfaceRenderer {
  protected void initializeBlockKinds(){materials[1]=2;materials[2]=1;opaque[2]=true;movement[2]=true;}
  float sample(MapDataContext ctx,int x,int z){return height(ctx,x,0,z);}
 }
 static MapDataContext context(Snapshot state){return (MapDataContext)Proxy.newProxyInstance(MapDataContext.class.getClassLoader(),new Class<?>[]{MapDataContext.class},(p,m,v)->{
  if(m.getName().equals("getPatchFactory"))return new PatchDefinitionFactory();
  if(m.getName().equals("getBlockTypeIDAt"))return state.getBlock((Integer)v[0],(Integer)v[1],(Integer)v[2]).id;
  if(m.getName().equals("getBlockDataAt"))return state.getBlockMetadata((Integer)v[0],(Integer)v[1],(Integer)v[2]);
  throw new AssertionError(m.getName());});}
 public static void main(String[] args) {
  Fixture f=new Fixture();f.initializeRenderer(new PatchDefinitionFactory(),1,65535,Collections.emptyMap());
  WaterSurfaceBehaviorTest golden=new WaterSurfaceBehaviorTest();Random random=new Random(719);
  for(int run=0;run<1000;run++) {
   Snapshot state=new Snapshot();for(int x=0;x<5;x++)for(int y=0;y<5;y++)for(int z=0;z<5;z++){state.ids[x][y][z]=random.nextInt(3);state.metas[x][y][z]=random.nextInt(16);}
   state.ids[2][2][2]=1;golden.blockAccess=state;MapDataContext ctx=context(state);
   for(int x=0;x<=1;x++)for(int z=0;z<=1;z++){
    float expected=golden.getLiquidHeight(x,0,z,WATER),actual=f.sample(ctx,x,z);
    if(Float.floatToIntBits(expected)!=Float.floatToIntBits(actual))throw new AssertionError("Actual Minecraft liquid height mismatch");
   }
   for(RenderPatch patch:f.getRenderPatchList(ctx)) {
    if(patch.getTextureIndex()<0||patch.getTextureIndex()>1)throw new AssertionError("Water atlas index");
    for(double uv:((PatchDefinition)patch).explicitTexCoords)if(uv<0||uv>1)throw new AssertionError("Water UV bounds");
   }
  }
  Snapshot covered=new Snapshot();covered.ids[2][2][2]=1;covered.ids[2][3][2]=1;
  if(f.sample(context(covered),0,0)!=1)throw new AssertionError("Water above should fill surface");
  System.out.println("4000 corner heights match actual RenderBlocks source bit-for-bit;1000 randomized patch/UV cases passed");
 }
}'''.replace('METHOD',method)
cp=str(a.classes)+':'+a.compile_classpath.read_text().strip()
with tempfile.TemporaryDirectory(prefix='gwm-water-') as out:
    source=pathlib.Path(out)/'WaterSurfaceBehaviorTest.java';source.write_text(java)
    subprocess.run(['javac','--release','8','-cp',cp,'-d',out,str(r/'src/main/java/org/dynmap/hdmap/renderer/VanillaWaterSurfaceRenderer.java'),str(source)],check=True)
    subprocess.run(['java','-cp',out+':'+str(a.classes)+':'+a.runtime_classpath.read_text().strip(),'WaterSurfaceBehaviorTest'],check=True)
