#!/usr/bin/env python3
"""Import an OBJ group with the production parser, retaining source UVs/winding."""
import argparse,pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--compile-classpath',type=pathlib.Path,required=True)
p.add_argument('--classes',type=pathlib.Path,required=True)
p.add_argument('source',type=pathlib.Path);p.add_argument('group')
p.add_argument('--offset',default='0,0,0')
a=p.parse_args();cp=str(a.classes)+':'+a.compile_classpath.read_text().strip()
with tempfile.TemporaryDirectory(prefix='gwm-obj-bake-') as out:
    subprocess.run(['javac','--release','8','-cp',cp,'-d',out,
                    str(root/'src/main/java/org/dynmap/hdmap/renderer/AtlasMesh.java'),
                    str(root/'src/main/java/org/dynmap/hdmap/renderer/WavefrontModel.java'),
                    str(root/'tools/tests/WavefrontBake.java'),str(root/'tools/tests/WavefrontGroupsTest.java')],check=True,stdout=subprocess.DEVNULL)
    subprocess.run(['java','-cp',out+':'+cp,'WavefrontGroupsTest'],check=True,stdout=subprocess.DEVNULL)
    subprocess.run(['java','-cp',out+':'+cp,'WavefrontBake',str(a.source),a.group]+a.offset.split(','),check=True)
