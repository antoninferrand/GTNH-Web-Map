#!/usr/bin/env python3
"""Extract the source-defined colored pixel frame, without copying mod PNGs."""
import argparse,pathlib,re,json,hashlib
p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=pathlib.Path);p.add_argument('output',type=pathlib.Path);a=p.parse_args();text=a.source.read_text()
colors={name:list(map(int,rgb.split(',')))for name,rgb in re.findall(r'static int\[\] (\w+) = \{([^}]+)\}',text)}
start=text.index('public static int[][][] shape =');end=text.index(';',start);shape=re.sub(r'/\*.*?\*/','',text[start:end],flags=re.S);rows=[re.findall(r'\w+',s)for s in re.findall(r'\{([^{}]+)\}',shape)]
assert len(rows)==16 and all(len(r)==8 for r in rows)
names=[k for k in colors if k!='n'];data={'shape':[[-1 if name=='n'else names.index(name)for name in row]for row in rows],
    'colors':[sum(round(v*255/256)<<(16-8*i)for i,v in enumerate(colors[name]))for name in names],
    'source':{'url':'https://github.com/GTNewHorizons/WitchingGadgets/blob/1.8.53-GTNH/src/main/java/witchinggadgets/client/render/TileRenderWallMirror.java','sha256':hashlib.sha256(a.source.read_bytes()).hexdigest()}}
a.output.write_text(json.dumps(data,separators=(',',':'))+'\n')
