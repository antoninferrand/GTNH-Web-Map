#!/usr/bin/env python3
"""Create and verify a cold-block scene ONLY in the disposable localhost Fixture.

Never targets a configurable server address. --fixture must resolve to the exact
/private/tmp/gwm-fixture directory created by the separate fixture preparation.
Commands are appended to its supervisor console file only with --enqueue.
"""
import argparse,csv,hashlib,json,re,time
import io,struct,gzip,zlib,pathlib
class Reader:
 def __init__(self,b):self.f=io.BytesIO(b)
 def num(self,fmt):return struct.unpack('>'+fmt,self.f.read(struct.calcsize('>'+fmt)))[0]
 def string(self):return self.f.read(self.num('H')).decode('utf8')
 def tag(self,t):
  if t in range(1,7):return self.num({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
  if t==7:return self.f.read(self.num('i'))
  if t==8:return self.string()
  if t==9:
   elem,n=self.num('B'),self.num('i');return [self.tag(elem) for _ in range(n)]
  if t==10:
   d={}
   while True:
    child=self.num('B')
    if not child:return d
    name=self.string();d[name]=self.tag(child)
  if t in [11,12]:return [self.num('i' if t==11 else 'q') for _ in range(self.num('i'))]
  raise ValueError(t)
 def root(self):
  t=self.num('B');self.string();return self.tag(t)
def nbt(b):return Reader(b).root()
def chunk(world,cx,cz):
 p=pathlib.Path(world)/'region'/('r.%d.%d.mca'%(cx//32,cz//32))
 with p.open('rb') as f:
  f.seek(4*((cx%32)+32*(cz%32)));location=int.from_bytes(f.read(4),'big');offset,count=location>>8,location&255
  if not offset:return None
  f.seek(offset*4096);length=int.from_bytes(f.read(4),'big');kind=f.read(1)[0];b=f.read(length-1)
  return nbt(zlib.decompress(b) if kind==2 else gzip.decompress(b) if kind==1 else b)

FIXTURE=pathlib.Path('/private/tmp/gwm-fixture')

def guard(path):
 root=path.resolve()
 if root != FIXTURE: raise ValueError('Only /private/tmp/gwm-fixture is permitted')
 props=dict(line.split('=',1) for line in (root/'server.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
 for key,value in {'server-ip':'127.0.0.1','server-port':'25566','level-name':'Fixture','level-type':'FLAT'}.items():
  if props.get(key)!=value: raise ValueError('Fixture guard failed: '+key)
 if not (root/'copy-manifest.json').is_file() or not (root/'README-fixture.md').is_file(): raise ValueError('Missing isolated fixture provenance')
 level=nbt(gzip.decompress((root/'Fixture/level.dat').read_bytes()))['Data']
 if level['LevelName']!='Fixture' or level['generatorName']!='flat':raise ValueError('Unexpected world')
 if json.loads((root/'supervisor-status.json').read_text())['status']!='running':raise ValueError('Fixture supervisor is not running')
 return root,level

def write_json(path,data):path.write_text(json.dumps(data,indent=2)+'\n')
def append(root,commands):
 console=(root/'console.log').read_text(errors='replace')
 if not re.search(r'Done \(.*\)!',console) or 'Stopping server' in console:raise ValueError('Fixture has not completed startup or is stopping')
 with (root/'console-commands.txt').open('a') as f:f.write('\n'.join(commands)+'\n')

def main():
 parser=argparse.ArgumentParser(description=__doc__)
 parser.add_argument('action',choices=['prepare','verify','render'])
 parser.add_argument('--fixture',type=pathlib.Path,required=True)
 parser.add_argument('--map',choices=['flat','surface'],default='flat',help='Render one map at a time; wait for its completion before the next')
 parser.add_argument('--enqueue',action='store_true',help='Append prepared local fixture commands; omitted by default')
 args=parser.parse_args();root,level=guard(args.fixture);scene=root/'visual-scene';scene.mkdir(exist_ok=True)
 manifest_path=scene/'manifest.json'
 if args.action=='prepare':
  repo=pathlib.Path(__file__).resolve().parents[2]
  baseline=repo/'docs/remediation/baseline/unsupported-gregtech.csv'
  audit=root/'dynmap/render-audit.json';registry={r['id']:r for r in json.loads(audit.read_text())['mtes']}
  rows=list(csv.DictReader(baseline.open()));assert len(rows)==157
  entries=[];commands=[];saved_chunks={}
  # Align a 13-column 3-block grid about the actual saved spawn.
  x0=(level['SpawnX']//3)*3-18;z0=(level['SpawnZ']//3)*3-18
  def add(mid,x,y,z,role):
   r=registry[mid];entry={'mteId':mid,'name':r['name'],'class':r['class'],'blockMetadata':r['blockMetadata'],'x':x,'y':y,'z':z,'role':role}
   nbt='{mID:%d,mFacing:2s,mMainFacing:2,mColor:0b,mWorks:1b,mConnections:0b}'%mid
   # All processing disabled, no active flag, energy, items or formed structures.
   entry['command']='setblock %d %d %d gregtech:gt.blockmachines %d replace %s'%(x,y,z,r['blockMetadata'],nbt)
   entries.append(entry)
   key=(x//16,z//16)
   if key not in saved_chunks:saved_chunks[key]=chunk(root/'Fixture',*key)
   c=saved_chunks[key];existing=next((t for t in c['Level']['TileEntities'] if [t.get(k) for k in ['x','y','z']]==[x,y,z]),None) if c else None
   entry['alreadySavedCold']=existing is not None and existing.get('mID')==mid and existing.get('mWorks')==1 and not existing.get('mStoredEnergy') and not existing.get('mActive')
   if not entry['alreadySavedCold']:commands.extend(['setblock %d %d %d minecraft:stone 0 replace'%(x,y-1,z),entry['command']])
  for index,row in enumerate(rows):add(int(row['id']),x0+3*(index%13),80,z0+3*(index//13),'baseline157')
  for index,mid in enumerate(range(5589,5599)):add(mid,x0+3*index,80,z0+42,'tin-pipe-example')
  # Incomplete, inactive 3x3 visual panel; no energy/storage multiblock supplied.
  vx,vz=x0+33,z0+45
  add(2741,vx,80,vz,'vending-cluster-controller');add(2742,vx+1,80,vz,'vending-cluster-uplink')
  casings=[]
  for dx in [-1,0,1]:
   for dy in [-1,0,1]:
    if dy==0 and dx in [0,1]:continue
    casing={'x':vx+dx,'y':80+dy,'z':vz,'registryName':'vendingmachine:casing','metadata':0};casings.append(casing)
    commands.append('setblock {x} {y} {z} vendingmachine:casing 0 replace'.format(**casing))
  commands.append('save-all')
  manifest={'schemaVersion':1,'world':'Fixture','artifact':json.loads((root/'artifact.json').read_text()),'spawn':{k:level[k] for k in ['SpawnX','SpawnY','SpawnZ']},'center':[x0+18,80,z0+21],'bounds':{'min':[x0,79,z0],'max':[x0+36,81,z0+45]},'spacing':3,'baselineSha256':hashlib.sha256(baseline.read_bytes()).hexdigest(),'auditSha256':hashlib.sha256(audit.read_bytes()).hexdigest(),'entries':entries,'casings':casings,'commands':commands,'coldNBT':'mWorks=1; no energy/items/mActive; north-facing; paint0; no pipe connections'}
  write_json(manifest_path,manifest);(scene/'commands.txt').write_text('\n'.join(commands)+'\n')
  if args.enqueue:
   write_json(scene/'placement-run.json',{'startedAt':time.time(),'consoleOffset':(root/'console.log').stat().st_size,'commands':len(commands)})
   append(root,commands)
  print('Prepared',len(entries),'MTE placements,',len(casings),'casings;',len(commands),'commands; enqueued=',args.enqueue)
 elif args.action=='verify':
  manifest=json.loads(manifest_path.read_text());cache={};results=[]
  for entry in manifest['entries']:
   x,y,z=entry['x'],entry['y'],entry['z'];key=(x//16,z//16)
   if key not in cache:cache[key]=chunk(root/'Fixture',*key)
   c=cache[key];te=next((t for t in c['Level']['TileEntities'] if [t.get(k) for k in ['x','y','z']]==[x,y,z]),None) if c else None
   result={'mteId':entry['mteId'],'x':x,'y':y,'z':z,'role':entry['role'],'tileEntityPresent':te is not None}
   if te:
    result.update({k:te.get(k) for k in ['id','mID','mFacing','mColor','mWorks','mConnections','mStoredEnergy','mActive']})
    section=next((s for s in c['Level']['Sections'] if s['Y']==y//16),None)
    if section:
     index=((y%16)*16+(z%16))*16+(x%16);data=section['Data'][index//2];result['savedBlockMetadata']=(data>>(4*(index%2)))&15
   result['matches']=te is not None and te.get('mID')==entry['mteId'] and result.get('savedBlockMetadata')==entry['blockMetadata']
   results.append(result)
  run=json.loads((scene/'placement-run.json').read_text());console=(root/'console.log').read_bytes()[run['consoleOffset']:].decode(errors='replace')
  (scene/'placement-console.log').write_text(console)
  errors=[line for line in console.splitlines() if re.search(r'ERROR|Exception|CRITICAL|FATAL|tagError|outOfWorld|Could not|noChange',line,re.I)]
  report={'artifact':manifest['artifact'],'checkedAt':time.time(),'requested':len(results),'matches':sum(r['matches'] for r in results),'coldDisabled':sum(r.get('mWorks')==1 for r in results),'active':sum(bool(r.get('mActive')) for r in results),'storedEnergyNonzero':sum(bool(r.get('mStoredEnergy')) for r in results),'consoleErrors':errors,'results':results}
  write_json(scene/'placement-verification.json',report);print(json.dumps({k:v for k,v in report.items() if k!='results'},indent=2))
 elif args.action=='render':
  m=json.loads(manifest_path.read_text());x,y,z=m['center'];commands=['dynmap pause all','dmap mapset Fixture:flat shader:stdtexture perspective:iso_S_90_hires','dmap mapset Fixture:surface shader:stdtexture perspective:iso_SE_60_hires','dynmap pause none','dynmap radiusrender Fixture %d %d 64 %s'%(x,z,args.map)]
  (scene/('render-commands-'+args.map+'.txt')).write_text('\n'.join(commands)+'\n')
  if args.enqueue:
   write_json(scene/('render-run-'+args.map+'.json'),{'startedAt':time.time(),'consoleOffset':(root/'console.log').stat().st_size,'commands':commands})
   append(root,commands)
  print('Prepared guarded '+args.map+' radius64 render; enqueued=',args.enqueue)

if __name__=='__main__':main()
