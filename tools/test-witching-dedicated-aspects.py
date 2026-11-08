#!/usr/bin/env python3
"""Reject client IIconRegister linkage and execute production aspect color lookup."""
import pathlib,argparse,tempfile,subprocess
r=pathlib.Path(__file__).resolve().parents[1];p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--compile-classpath',type=pathlib.Path,required=True);p.add_argument('--runtime-classpath',type=pathlib.Path,required=True);p.add_argument('--classes',type=pathlib.Path,required=True);a=p.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-dedicated-aspect-')as out:
 root=pathlib.Path(out);files={
 'thaumcraft/api/aspects/Aspect.java':'package thaumcraft.api.aspects;public class Aspect {int color;Aspect(int n){color=n;}'+''.join(f'public static final Aspect {name}=new Aspect({i+1});'for i,name in enumerate(['PLANT','COLD','FIRE','TREE','TAINT','SLIME','WATER','ELDRITCH','HEAL']))+'}',
 'cpw/mods/fml/common/Loader.java':'package cpw.mods.fml.common;public class Loader {public static boolean isModLoaded(String name){return false;}}',
 'DedicatedAspectTest.java':'''import org.dynmap.hdmap.renderer.WitchingTerraformRenderer;
 public class DedicatedAspectTest extends WitchingTerraformRenderer {
  public static void main(String[] args)throws Exception {
   try{Class.forName("net.minecraft.block.Block").getMethod("requiredAspect",int.class);throw new AssertionError("Client linkage was available");}catch(NoClassDefFoundError expected){}
   int[] actual=new DedicatedAspectTest().sourceAspectColors(1);int[] expected={0xffffff,0xffffff,0xffffff,1,2,3,4,3,5,6,7,7,8,9,0xffffff,0xffffff};
   if(!java.util.Arrays.equals(actual,expected))throw new AssertionError("Source aspect metadata table");
   System.out.println("Unavailable IIconRegister reproduces old Block reflection failure; production common-side Aspect lookup passes all16 metadata states");
  }
 }''',
 'net/minecraft/block/Block.java':'package net.minecraft.block;public class Block {public Object requiredAspect(int meta){return null;}public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister client){}public static Block getBlockById(int id){return new Block();}}',
 'DedicatedBarrier.java':'''import java.net.*;import java.io.*;public class DedicatedBarrier {public static void main(String[] args)throws Exception {String[] paths=System.getProperty("java.class.path").split(File.pathSeparator);URL[] urls=new URL[paths.length];for(int i=0;i<paths.length;i++)urls[i]=new File(paths[i]).toURI().toURL();try(URLClassLoader loader=new URLClassLoader(urls,null){protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{if(n.equals("net.minecraft.client.renderer.texture.IIconRegister"))throw new ClassNotFoundException("Dedicated side unavailable");return super.loadClass(n,resolve);}}){loader.loadClass("DedicatedAspectTest").getMethod("main",String[].class).invoke(null,(Object)new String[0]);}}}'''}
 for name,source in files.items():
  target=root/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(source)
 cp=str(a.classes)+':'+a.compile_classpath.read_text().strip()
 subprocess.run(['javac','--release','8','-cp',cp,'-d',out]+[str(root/name)for name in files]+[str(r/f)for f in ['src/main/java/org/dynmap/hdmap/renderer/AtlasMesh.java','src/main/java/org/dynmap/hdmap/renderer/BakedMeshRenderer.java','src/main/java/org/dynmap/hdmap/renderer/WitchingTerraformRenderer.java']],check=True)
 subprocess.run(['java','-cp',out+':'+str(a.classes)+':'+a.runtime_classpath.read_text().strip(),'DedicatedBarrier'],check=True)
