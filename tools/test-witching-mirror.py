#!/usr/bin/env python3
import pathlib,argparse,tempfile,subprocess
r=pathlib.Path(__file__).resolve().parents[1];p=argparse.ArgumentParser(description='Exercise actual mirror geometry and first-frame PNG pixel extraction')
p.add_argument('--atlas',type=pathlib.Path,required=True);p.add_argument('--compile-classpath',type=pathlib.Path,required=True);p.add_argument('--runtime-classpath',type=pathlib.Path,required=True);p.add_argument('--classes',type=pathlib.Path,required=True);a=p.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-mirror-')as out:
 stub=pathlib.Path(out)/'TexturePack.java';stub.write_text('''package org.dynmap.hdmap;import java.util.*;public class TexturePack {public Map<Integer,int[]> tiles=new HashMap<>();public void setTileARGB(int id,int[] pixels){tiles.put(id,pixels);}public static void scaleTerrainPNGSubImage(int source,int target,int[] in,int[] out){if(source!=target)throw new AssertionError("Golden requires actual native atlas dimensions");System.arraycopy(in,0,out,0,in.length);}}''')
 cp=str(a.classes)+':'+a.compile_classpath.read_text().strip()
 subprocess.run(['javac','--release','8','-cp',cp,'-d',out,str(stub)]+[str(r/f)for f in ['src/main/java/org/dynmap/hdmap/renderer/AtlasMesh.java','src/main/java/org/dynmap/hdmap/renderer/WitchingWallMirrorRenderer.java','src/main/java/org/dynmap/hdmap/textureprocessor/WitchingMirrorGlassTextureProcessor.java','tools/tests/WitchingMirrorBehaviorTest.java']],check=True)
 subprocess.run(['java','-cp',out+':'+str(r/'src/main/resources')+':'+str(a.classes)+':'+a.runtime_classpath.read_text().strip(),'WitchingMirrorBehaviorTest',str(a.atlas)],check=True)
