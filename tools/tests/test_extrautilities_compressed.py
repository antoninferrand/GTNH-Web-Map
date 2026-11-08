"""Pixel golden test against the installed ExtraUtilities class (JAR is not committed)."""
import os
import pathlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
JAR = pathlib.Path(os.environ.get('EXTRAUTILITIES_JAR', '/private/tmp/gwm-extrautilities.jar'))
SOURCES = {
 'net/minecraft/util/ResourceLocation.java': '''package net.minecraft.util;
 public class ResourceLocation { public final String domain,path;
  public ResourceLocation(String domain,String path){this.domain=domain;this.path=path;}
 }''',
 'net/minecraft/client/settings/GameSettings.java': '''package net.minecraft.client.settings;
 public class GameSettings { public int field_151442_I=0; public int field_151443_J=1; }''',
 'net/minecraft/client/Minecraft.java': '''package net.minecraft.client;
 public class Minecraft {
  private static final Minecraft INSTANCE=new Minecraft();
  public final net.minecraft.client.settings.GameSettings field_71474_y=new net.minecraft.client.settings.GameSettings();
  public static Minecraft func_71410_x(){return INSTANCE;}
 }''',
 'net/minecraft/client/resources/data/IMetadataSection.java': '''package net.minecraft.client.resources.data;
 public interface IMetadataSection {}''',
 'net/minecraft/client/resources/data/TextureMetadataSection.java': '''package net.minecraft.client.resources.data;
 public class TextureMetadataSection implements IMetadataSection {}''',
 'net/minecraft/client/resources/data/AnimationMetadataSection.java': '''package net.minecraft.client.resources.data;
 public class AnimationMetadataSection implements IMetadataSection {}''',
 'net/minecraft/client/resources/IResource.java': '''package net.minecraft.client.resources;
 import java.io.*;import net.minecraft.client.resources.data.*;
 public interface IResource {
  InputStream func_110527_b(); IMetadataSection func_110526_a(String name);
 }''',
 'net/minecraft/client/resources/IResourceManager.java': '''package net.minecraft.client.resources;
 import java.io.*;import net.minecraft.util.ResourceLocation;
 public interface IResourceManager { IResource func_110536_a(ResourceLocation value) throws IOException; }''',
 'net/minecraft/client/renderer/texture/TextureAtlasSprite.java': '''package net.minecraft.client.renderer.texture;
 import java.awt.image.BufferedImage;import java.util.*;
 import net.minecraft.client.resources.data.AnimationMetadataSection;
 public class TextureAtlasSprite {
  public List<int[][]> field_110976_a=new ArrayList<int[][]>();
  public int field_130223_c,field_130224_d;
  public TextureAtlasSprite(String name){}
  public void func_147964_a(BufferedImage[] images,AnimationMetadataSection animation,boolean anisotropic){
   BufferedImage image=images[0];field_130223_c=image.getWidth();field_130224_d=image.getHeight();
   int[] pixels=new int[field_130223_c*field_130224_d];
   image.getRGB(0,0,field_130223_c,field_130224_d,pixels,0,field_130223_c);
   field_110976_a.add(new int[][]{pixels});
  }
 }''',
 'org/dynmap/hdmap/TexturePack.java': '''package org.dynmap.hdmap;
 public class TexturePack {
  public int[] result;
  public static void scaleTerrainPNGSubImage(int width,int scale,int[] pixels,int[] out){
   if(width!=scale)throw new AssertionError("Golden harness uses native source resolution");
   System.arraycopy(pixels,0,out,0,out.length);
  }
  public void setTileARGB(int tile,int[] pixels){result=pixels;}
 }''',
 'CompressedGolden.java': '''import java.awt.image.BufferedImage;
 import java.io.*;import java.util.*;import javax.imageio.ImageIO;
 import net.minecraft.util.ResourceLocation;import net.minecraft.client.resources.*;
 import net.minecraft.client.resources.data.IMetadataSection;
 import com.rwtema.extrautils.texture.TextureComprBlock;
 import org.dynmap.hdmap.TexturePack;
 import org.dynmap.hdmap.textureprocessor.ExtraUtilitiesCompressedTextureProcessor;
 public class CompressedGolden {
  static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
  public static void main(String[] args) throws Exception {
   for(int size:new int[]{8,16,32}) {
    final BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
    final int[] original=new int[size*size];
    for(int y=0;y<size;y++)for(int x=0;x<size;x++){
     int p=0xff000000|((x*47+y*13)&255)<<16|((y*61+x*11)&255)<<8|((x*23+y*59)&255);
     image.setRGB(x,y,p);original[y*size+x]=p;
    }
    ByteArrayOutputStream encoded=new ByteArrayOutputStream();ImageIO.write(image,"PNG",encoded);
    final byte[] png=encoded.toByteArray();
    IResource base=new IResource(){
     public InputStream func_110527_b(){return new ByteArrayInputStream(png);}
     public IMetadataSection func_110526_a(String name){return null;}
    };
    for(int level=1;level<=8;level++){
     TextureComprBlock reference=new TextureComprBlock("compressed","cobblestone",level);
     final boolean premade=false;
     IResourceManager manager=new IResourceManager(){
      public IResource func_110536_a(ResourceLocation value) throws IOException {
       if(value.domain.equals("extrautils"))throw new IOException("No pre-generated PNG");return base;
      }
     };
     check(!reference.load(manager,new ResourceLocation("extrautils","compressed")),"Actual mod failed to generate");
     int[] expected=reference.field_110976_a.get(0)[0];
     ExtraUtilitiesCompressedTextureProcessor processor=new ExtraUtilitiesCompressedTextureProcessor();
     HashMap<String,String> config=new HashMap<>();config.put("compression",Integer.toString(level));
     processor.init(config);TexturePack map=new TexturePack();
     processor.patchTextures(map,new int[]{0},original,size,size,size);
     for(int i=0;i<expected.length;i++)check(expected[i]==map.result[i],"Pixel "+i+" at level "+level+", size "+size+" differs: "+Integer.toHexString(expected[i])+" != "+Integer.toHexString(map.result[i]));
     // Original premade resource takes the mod's early return, preserving all supplied pixels.
     TextureComprBlock premadeReference=new TextureComprBlock("compressed","cobblestone",level);
     premadeReference.load(new IResourceManager(){public IResource func_110536_a(ResourceLocation value){return base;}},new ResourceLocation("extrautils","compressed"));
     check(Arrays.equals(original,premadeReference.field_110976_a.get(0)[0]),"Premade source pixels changed");
    }
   }
   System.out.println("All8 compression levels match actual mod pixels at8/16/32 source resolution; originals preserved");
  }
 }''',
}


class CompressionGoldenTest(unittest.TestCase):
    @unittest.skipUnless(JAR.is_file(), 'set EXTRAUTILITIES_JAR to the installed ExtraUtilities 1.2.12 JAR')
    def test_all_levels_against_actual_mod(self):
        with tempfile.TemporaryDirectory(prefix='gwm-compressed-') as tmp:
            path = pathlib.Path(tmp)
            for name, source in SOURCES.items():
                target = path / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(source)
            for source in ['src/main/java/org/dynmap/hdmap/textureprocessor/CustomTextureProcessor.java',
                           'src/main/java/org/dynmap/hdmap/textureprocessor/ExtraUtilitiesCompressedTextureProcessor.java']:
                shutil.copyfile(ROOT / source, path / pathlib.Path(source).name)
            classes = path / 'classes'
            subprocess.run(['javac', '--release', '8', '-cp', str(JAR), '-d', str(classes)] +
                           [str(p) for p in path.rglob('*.java')], check=True)
            subprocess.run(['java', '-Djava.awt.headless=true', '-cp', str(classes) + os.pathsep + str(JAR),
                            'CompressedGolden'], check=True)


if __name__ == '__main__':
    unittest.main()
