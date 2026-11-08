#!/usr/bin/env python3
"""Bake static ModelRenderer cuboids using Minecraft 1.7.10 ModelBox/TexturedQuad UVs.

Input source is not vendored. Reject unsupported constructs instead of guessing geometry.
"""
import argparse
import hashlib
import json
import math
import pathlib
import re
import struct

NUMBER = r'[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?[fFdD]?'


def numbers(value):
    result = []
    for token in value.split(','):
        token = token.strip()
        if not re.fullmatch(NUMBER, token):
            raise ValueError('Expected a literal number, got ' + token)
        result.append(float(token.rstrip('fFdD')))
    return result


def rotate(point, angles):
    x, y, z = point
    ax, ay, az = angles
    y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
    x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
    x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
    return x, y, z


def cuboid(box, uv, atlas, mirror=False):
    x, y, z, w, h, d = box[:6]
    inflate = box[6] if len(box) > 6 else 0
    x0, y0, z0 = x - inflate, y - inflate, z - inflate
    x1, y1, z1 = x + w + inflate, y + h + inflate, z + d + inflate
    if mirror:
        x0, x1 = x1, x0
    vertices = [(x0,y0,z0),(x1,y0,z0),(x1,y1,z0),(x0,y1,z0),
                (x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)]
    u, v = uv
    # Exact ModelBox quad order and TexturedQuad corner UV assignments.
    faces = [((5,1,2,6),(u+d+w,v+d,u+d+w+d,v+d+h)),
             ((0,4,7,3),(u,v+d,u+d,v+d+h)),
             ((5,4,0,1),(u+d,v,u+d+w,v+d)),
             ((2,3,7,6),(u+d+w,v+d,u+d+w+w,v)),
             ((1,0,3,2),(u+d,v+d,u+d+w,v+d+h)),
             ((4,5,6,7),(u+d+w+d,v+d,u+d+w+d+w,v+d+h))]
    result = []
    for indexes, bounds in faces:
        u0,v0,u1,v1 = bounds
        tex = [(u1/atlas[0],1-v0/atlas[1]),(u0/atlas[0],1-v0/atlas[1]),
               (u0/atlas[0],1-v1/atlas[1]),(u1/atlas[0],1-v1/atlas[1])]
        quad = [list(vertices[index]) + list(uvpoint) for index, uvpoint in zip(indexes,tex)]
        if mirror:
            quad.reverse()
        for indexes in [(0,1,2),(0,2,3)]:
            a,b,c = [quad[i] for i in indexes]
            ab=[b[i]-a[i] for i in range(3)];ac=[c[i]-a[i] for i in range(3)]
            cross=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
            if sum(x*x for x in cross) > 1e-18:
                result.append(sum([quad[i] for i in indexes],[]))
    return result


def bake(source, offset=(.5,1.5,.5), scale=(1,-1,-1)):
    # The supported exports have a no-argument constructor and independent parts.
    source = re.sub(r'//[^\n]*|/\*.*?\*/','',source,flags=re.S)
    cls=re.search(r'class\s+(\w+)',source)[1]
    start=re.search(r'public\s+'+cls+r'\s*\(\s*\)\s*\{',source).end()
    depth=1;end=start
    while depth:
        depth += (source[end]=='{')-(source[end]=='}');end+=1
    constructor=source[start:end-1]
    if '.addChild(' in constructor:
        raise ValueError('Child transforms need explicit baking support')
    width=re.search(r'(?:this\.)?textureWidth\s*=\s*('+NUMBER+r')',constructor)
    height=re.search(r'(?:this\.)?textureHeight\s*=\s*('+NUMBER+r')',constructor)
    atlas=(int(numbers(width[1])[0]) if width else 64,int(numbers(height[1])[0]) if height else 32)
    parts=[]
    declarations=list(re.finditer(r'(?:this\.)?(\w+)\s*=\s*new ModelRenderer\(this,\s*([^)]*)\);',constructor))
    for i,m in enumerate(declarations):
        name=m[1];body=constructor[m.end():declarations[i+1].start() if i+1<len(declarations) else len(constructor)]
        boxes=list(re.finditer(r'(?:this\.)?'+name+r'\.addBox\(([^)]*)\)',body))
        if len(boxes)>1:
            raise ValueError('Multiple cuboids need explicit baking support for '+name)
        box=boxes[0] if boxes else None
        if not box:
            raise ValueError('No literal cuboid for '+name)
        box_values=numbers(box[1]);uv=numbers(m[2])
        if len(uv)!=2 or len(box_values) not in [6,7]:
            raise ValueError('Unsupported cuboid constructor for '+name)
        before=body[:box.start()]
        mirrored=bool(re.search(re.escape(name)+r'\.mirror\s*=\s*true',before))
        pivot=re.search(re.escape(name)+r'\.setRotationPoint\(([^)]*)\)',body)
        pivot=numbers(pivot[1]) if pivot else [0,0,0]
        rotation=re.search(r'setRotation\(\s*(?:this\.)?'+name+r'\s*,([^)]*)\)',body)
        rotation=numbers(rotation[1]) if rotation else [0,0,0]
        if len(rotation)!=3 or len(pivot)!=3:
            raise ValueError('Invalid part transform for '+name)
        triangles=cuboid(box_values,uv,atlas,mirrored)
        for tri in triangles:
            for index in [0,5,10]:
                point=rotate(tri[index:index+3],rotation)
                tri[index:index+3]=[offset[j]+scale[j]*(point[j]+pivot[j])/16 for j in range(3)]
        parts.append({'name':name,'triangles':triangles})
    if not parts:
        raise ValueError('No ModelRenderer parts found')
    return {'atlas':[atlas[0]//16,atlas[1]//16],'parts':parts}


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source',type=pathlib.Path)
    parser.add_argument('output',type=pathlib.Path)
    parser.add_argument('--offset',default='.5,1.5,.5')
    parser.add_argument('--scale',default='1,-1,-1')
    parser.add_argument('--source-reference',help='Versioned source URL recorded with its SHA-256')
    parser.add_argument('--atlas-image',type=pathlib.Path,help='Actual PNG dimensions for higher-resolution atlases')
    args=parser.parse_args()
    model=bake(args.source.read_text(),numbers(args.offset),numbers(args.scale))
    if args.atlas_image:
        raw=args.atlas_image.read_bytes()
        if raw[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Expected a PNG atlas')
        width,height=struct.unpack('>II',raw[16:24])
        if width%16 or height%16:raise ValueError('Atlas must have whole16-pixel cells')
        model['atlas']=[width//16,height//16]
    if args.source_reference:
        model['source']={'url':args.source_reference,'sha256':hashlib.sha256(args.source.read_bytes()).hexdigest()}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(model,separators=(',',':'))+'\n')
    print(len(model['parts']),'parts;',sum(len(p['triangles']) for p in model['parts']),'triangles;',model['atlas'],'atlas')
