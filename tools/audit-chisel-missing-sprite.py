#!/usr/bin/env python3
"""Check every current missing-sprite consumer against initialized Chisel variant masks."""
import argparse,csv,json,pathlib,re
r=pathlib.Path(__file__).resolve().parents[1];p=argparse.ArgumentParser(description=__doc__);p.add_argument('audit',type=pathlib.Path);p.add_argument('--chisel-source',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args();audit=json.loads(a.audit.read_text());variants=audit['chiselVariants'];registered={b['registryName']for b in audit['blocks']};records=[]
# Source-specific metadata remapping for stair variants must be included, not assumed direct.
stairs=(a.chisel_source/'src/main/java/team/chisel/block/BlockCarvableStairs.java').read_text();assert 'blockMeta + metadata / 8' in stairs
conversion=(a.chisel_source/'src/main/java/team/chisel/BlockNameConversion.java').read_text();assert 'namingConversion.put("wroughtironblock", "castironblock")' in conversion
for line_no,line in enumerate((r/'src/main/resources/renderdata/chisel-texture.txt').read_text().splitlines(),1):
    if not(line.startswith('block:')and':minecraft/missingno' in line):continue
    metas=re.findall(r'(?:^|,)data=([^,]+)',line);metas=list(range(16))if not metas or '*'in metas else list(map(int,metas))
    for name in re.findall(r'(?:^block:|,)id=%([^,]+)',line):
        key='chisel:'+name;row=variants.get(key);effective=metas;status='CLIENT_MISSING_SPRITE'
        if row is None:
            assert key=='chisel:wroughtironblock' and key not in registered and 'chisel:castironblock' in registered,(key,'unexpected absent helper')
            status='INACTIVE_RENAMED_REGISTRY_ALIAS';mask=0
        else:
            assert row['status']=='OK',(key,row)
            mask=row['nonnullMetadataMask']
            if row['class']=='team.chisel.block.BlockCarvableConcreteStairs':
                effective=[2*int(name.rsplit('.',1)[1])+m//8 for m in metas]
            assert all(not(mask&(1<<m))for m in effective),(key,metas,effective,mask,'valid source variant was replaced by checker')
        records.append({'registryName':key,'definitionLine':line_no,'metadata':';'.join(map(str,metas)),'helperMetadata':';'.join(map(str,effective)),'nonnullMask':mask,'status':status})
a.output.parent.mkdir(parents=True,exist_ok=True)
with a.output.open('w')as f:
 w=csv.DictWriter(f,fieldnames=list(records[0]));w.writeheader();w.writerows(records)
print(sum(len(x['metadata'].split(';'))for x in records if x['status']=='CLIENT_MISSING_SPRITE'),'registered metadata consumers intentionally return missingno;',sum(x['status']!='CLIENT_MISSING_SPRITE'for x in records),'inactive renamed alias; no valid source variant hidden')
