#!/usr/bin/env python3
"""Execute the exact WitchingGadgets TESR and installed Thaumcraft bellows with CPU/GL boundary doubles."""
import argparse,pathlib,subprocess,tempfile,json,runpy,hashlib,zipfile,struct
r=pathlib.Path(__file__).resolve().parents[1];p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--source',type=pathlib.Path,required=True);p.add_argument('--thaumcraft',type=pathlib.Path,required=True);p.add_argument('--classpath',type=pathlib.Path,required=True);p.add_argument('output',type=pathlib.Path);a=p.parse_args()
stubs={
'net/minecraft/client/model/ModelBase.java':'''package net.minecraft.client.model;public class ModelBase{public int field_78090_t=64,field_78089_u=32;}''',
'net/minecraft/client/model/ModelRenderer.java':'''package net.minecraft.client.model;import capture.Capture;public class ModelRenderer{
 public boolean field_78809_i;public float field_78795_f,field_78796_g,field_78808_h;int u,v,w,h;float[] box,pivot={0,0,0};boolean mirror;
 public ModelRenderer(ModelBase m,int u,int v){this.u=u;this.v=v;w=m.field_78090_t;h=m.field_78089_u;}
 public ModelRenderer func_78789_a(float x,float y,float z,int w,int h,int d){box=new float[]{x,y,z,w,h,d};mirror=field_78809_i;return this;}
 public ModelRenderer func_78787_b(int w,int h){this.w=w;this.h=h;return this;}
 public void func_78793_a(float x,float y,float z){pivot=new float[]{x,y,z};}
 public void func_78785_a(float scale){Capture.box(box,new int[]{u,v},new int[]{w,h},pivot,new float[]{field_78795_f,field_78796_g,field_78808_h},mirror,scale);}
}''',
'net/minecraft/tileentity/TileEntity.java':'''package net.minecraft.tileentity;public class TileEntity{public net.minecraft.world.World func_145831_w(){return null;}}''',
'net/minecraft/world/World.java':'''package net.minecraft.world;public class World{}''',
'net/minecraft/client/renderer/tileentity/TileEntitySpecialRenderer.java':'''package net.minecraft.client.renderer.tileentity;public class TileEntitySpecialRenderer{public void renderTileEntityAt(net.minecraft.tileentity.TileEntity t,double x,double y,double z,float f){}}''',
'net/minecraft/entity/player/EntityPlayer.java':'''package net.minecraft.entity.player;public class EntityPlayer{public int field_70173_aa=0;}''',
'net/minecraft/client/entity/EntityClientPlayerMP.java':'''package net.minecraft.client.entity;public class EntityClientPlayerMP extends net.minecraft.entity.player.EntityPlayer{}''',
'net/minecraft/client/Minecraft.java':'''package net.minecraft.client;public class Minecraft{public net.minecraft.client.entity.EntityClientPlayerMP field_71439_g=new net.minecraft.client.entity.EntityClientPlayerMP();static Minecraft m=new Minecraft();public static Minecraft func_71410_x(){return m;}}''',
'cpw/mods/fml/client/FMLClientHandler.java':'''package cpw.mods.fml.client;public class FMLClientHandler{public static FMLClientHandler instance(){return new FMLClientHandler();}public net.minecraft.client.Minecraft getClient(){return net.minecraft.client.Minecraft.func_71410_x();}}''',
'net/minecraft/util/MathHelper.java':'''package net.minecraft.util;public class MathHelper{public static float func_76126_a(float f){return (float)Math.sin(f);}}''',
'net/minecraftforge/common/util/ForgeDirection.java':'''package net.minecraftforge.common.util;public enum ForgeDirection{DOWN,UP,NORTH,SOUTH,WEST,EAST,UNKNOWN}''',
'witchinggadgets/common/blocks/tiles/TileEntityEssentiaPump.java':'''package witchinggadgets.common.blocks.tiles;public class TileEntityEssentiaPump extends net.minecraft.tileentity.TileEntity{public net.minecraftforge.common.util.ForgeDirection facing=net.minecraftforge.common.util.ForgeDirection.NORTH;}''',
'thaumcraft/common/tiles/TileBellows.java':'''package thaumcraft.common.tiles;public class TileBellows extends net.minecraft.tileentity.TileEntity{public byte orientation;public float inflation;}''',
'witchinggadgets/client/ClientUtilities.java':'''package witchinggadgets.client;public class ClientUtilities{public static void bindTexture(String s){capture.Capture.texture=s;}}''',
'thaumcraft/client/lib/UtilsFX.java':'''package thaumcraft.client.lib;public class UtilsFX{public static void bindTexture(String s){capture.Capture.texture="thaumcraft:"+s;}}''',
'net/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher.java':'''package net.minecraft.client.renderer.tileentity;public class TileEntityRendererDispatcher{public static TileEntityRendererDispatcher instance=new TileEntityRendererDispatcher();public void renderTileEntityAt(net.minecraft.tileentity.TileEntity tile,double x,double y,double z,float f){new thaumcraft.client.renderers.tile.TileBellowsRenderer().renderEntityAt((thaumcraft.common.tiles.TileBellows)tile,x,y,z,f);}}''',
'net/minecraft/client/renderer/Tessellator.java':'''package net.minecraft.client.renderer;public class Tessellator{public static Tessellator instance=new Tessellator();public void startDrawing(int mode){capture.Capture.begin(mode);}public void startDrawingQuads(){startDrawing(7);}public void setNormal(float x,float y,float z){}public void addVertexWithUV(double x,double y,double z,double u,double v){capture.Capture.vertex(x,y,z,u,v);}public int draw(){capture.Capture.end();return 0;}}''',
'org/lwjgl/opengl/GL11.java':'''package org.lwjgl.opengl;public class GL11{
 public static void glPushMatrix(){capture.Capture.push();}public static void glPopMatrix(){capture.Capture.pop();}
 public static void glTranslatef(float x,float y,float z){glTranslated(x,y,z);}public static void glTranslated(double x,double y,double z){capture.Capture.translate(x,y,z);}
 public static void glScalef(float x,float y,float z){glScaled(x,y,z);}public static void glScaled(double x,double y,double z){capture.Capture.scale(x,y,z);}
 public static void glRotatef(float angle,float x,float y,float z){capture.Capture.rotate(angle,x,y,z);}
 public static void glEnable(int i){}public static void glDisable(int i){}public static void glBlendFunc(int a,int b){}public static void glColor4f(float a,float b,float c,float d){}
}''',
}
with tempfile.TemporaryDirectory(prefix='gwm-pump-source-') as out:
 root=pathlib.Path(out)
 for file,source in stubs.items():
  target=root/file;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(source)
 capture=r/'tools/fixtures/PumpCapture.java';target=root/'capture/Capture.java';target.parent.mkdir(parents=True,exist_ok=True);target.write_text(capture.read_text())
 cp=str(a.thaumcraft)+':'+a.classpath.read_text().strip()
 subprocess.run(['javac','--release','8','-cp',cp,'-d',out]+[str(root/f)for f in stubs]+[str(target),str(a.source)],check=True)
 raw=json.loads(subprocess.check_output(['java','-cp',out+':'+cp,'capture.Capture'],text=True))
 baker=runpy.run_path(str(r/'tools/bake-techne-model.py'));parts={}
 for triangle in raw['triangles']:parts.setdefault(triangle['texture'],[]).append(triangle['vertices'])
 for box in raw['boxes']:
  for tri in baker['cuboid'](box['box'],box['uv'],box['atlas'],box['mirror']):
   for i in [0,5,10]:
    point=baker['rotate'](tri[i:i+3],box['rotation']);point=[(point[j]+box['pivot'][j])*box['scale']for j in range(3)]+[1]
    tri[i:i+3]=[sum(box['matrix'][j*4+k]*point[k]for k in range(4))for j in range(3)]
   parts.setdefault(box['texture'],[]).append(tri)
 # Each source texture gets its own16pixel atlas range; UVs are normalized to its actual dimensions.
 # Grid8x4 matches bellows128x64; other images are subdivided more finely without losing pixels.
 grids={}
 with zipfile.ZipFile(a.thaumcraft)as jar:
  for name in parts:
   width,height=struct.unpack('>II',jar.read('assets/'+name.replace(':','/'))[16:24]);assert width%16==height%16==0
   grids[name]=[width//16,height//16]
 model={'atlas':[1,1],'separatePartTextures':True,'parts':[{'name':name,'atlas':grids[name],'triangles':tri}for name,tri in parts.items()],
        'source':{'url':'https://github.com/GTNewHorizons/WitchingGadgets/blob/1.8.53-GTNH/src/main/java/witchinggadgets/client/render/TileRenderEssentiaPump.java','sha256':hashlib.sha256(a.source.read_bytes()).hexdigest(),'thaumcraftJarSha256':hashlib.sha256(a.thaumcraft.read_bytes()).hexdigest(),'animationSampleTicks':0}}
 # Geometry yaw follows the saved Forge direction; BakedMeshRenderer's direct rotations match OpenGL.
 for part in model['parts']:part['pivot']=[.5,.5,.5];part['pose']={'field':'facing','default':[0,0,0],'rotations':{'2':[0,0,0],'3':[0,180,0],'4':[0,90,0],'5':[0,270,0]}}
 a.output.write_text(json.dumps(model,separators=(',',':'))+'\n')
 print([(p['name'],len(p['triangles']))for p in model['parts']])
