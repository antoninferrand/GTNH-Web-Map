#!/usr/bin/env python3
import pathlib,argparse,tempfile,subprocess
r=pathlib.Path(__file__).resolve().parents[1];p=argparse.ArgumentParser(description='Trace real source model triangles with IsoHDPerspective acceptance rules')
p.add_argument('--compile-classpath',type=pathlib.Path,required=True);p.add_argument('--runtime-classpath',type=pathlib.Path,required=True);p.add_argument('--classes',type=pathlib.Path,required=True);p.add_argument('models',nargs='+');a=p.parse_args()
with tempfile.TemporaryDirectory(prefix='gwm-model-rays-')as out:
 cp=str(a.classes)+':'+a.compile_classpath.read_text().strip()
 subprocess.run(['javac','--release','8','-cp',cp,'-d',out]+[str(r/f)for f in ['src/main/java/org/dynmap/hdmap/renderer/AtlasMesh.java','src/main/java/org/dynmap/hdmap/renderer/BakedMeshRenderer.java','src/main/java/org/dynmap/hdmap/renderer/WitchingWallMirrorRenderer.java','tools/tests/BakedMeshRayTest.java']],check=True)
 subprocess.run(['java','-cp',out+':'+str(r/'src/main/resources')+':'+str(a.classes)+':'+a.runtime_classpath.read_text().strip(),'BakedMeshRayTest']+a.models,check=True)
