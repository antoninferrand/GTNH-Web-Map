#!/usr/bin/env python3
"""Validate baked cuboids and, optionally, compare against actual Minecraft ModelBox."""
import argparse
import json
import pathlib
import runpy
import subprocess
import tempfile
root=pathlib.Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--minecraft-jar',type=pathlib.Path)
parser.add_argument('--runtime-classpath',type=pathlib.Path)
args=parser.parse_args()
baker=runpy.run_path(str(root/'tools/bake-techne-model.py'))
model='''class Example { public Example() {
 textureWidth=64; textureHeight=32;
 Base=new ModelRenderer(this,0,0); Base.setRotationPoint(-8,8,-8); Base.addBox(0,0,0,16,16,16);
 } }'''
baked=baker['bake'](model)
assert len(baked['parts'])==1 and len(baked['parts'][0]['triangles'])==12
for axis in range(3):
    values=[tri[i+axis] for tri in baked['parts'][0]['triangles'] for i in [0,5,10]]
    assert min(values)==0 and max(values)==1
for unsupported in [model.replace('Base.addBox(0,0,0,16,16,16);','Base.addBox(0,0,0,16,16,16); Base.addBox(0,0,0,1,1,1);'),
                    model.replace('Base.addBox(0,0,0,16,16,16);','Base.addChild(other); Base.addBox(0,0,0,16,16,16);'),
                    model.replace('16,16,16','size,16,16')]:
    try:baker['bake'](unsupported)
    except ValueError:pass
    else:raise AssertionError('Unsupported construct was guessed')
if args.minecraft_jar:
    with tempfile.TemporaryDirectory(prefix='gwm-modelbox-') as folder:
        subprocess.run(['javac','--release','8','-d',folder,str(root/'tools/tests/ModelBoxGolden.java')],check=True)
        cp=folder+':'+str(args.minecraft_jar)
        if args.runtime_classpath:cp+=':'+args.runtime_classpath.read_text().strip()
        expected=json.loads(subprocess.check_output(['java','-cp',cp,'ModelBoxGolden'],text=True))
        for mirror,actual in zip([False,True],expected):
            triangles=baker['cuboid']([1.25,2,-4,5,7,3],[7,9],[64,32],mirror)
            assert len(triangles)==len(actual)
            for a,b in zip(triangles,actual):
                assert all(abs(x-y)<1e-8 for x,y in zip(a,b)),(mirror,a,b)
        print('Exact Minecraft ModelBox positions/UVs match for ordinary and mirrored cuboids')
print('Static ModelRenderer baking, TESR transform and unsupported-source checks passed')
