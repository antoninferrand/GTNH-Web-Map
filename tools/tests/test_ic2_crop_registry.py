"""Execute the crop loader against registry/texture-boundary doubles without Minecraft."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
STUBS = {
    'org/dynmap/DynmapCore.java': '''package org.dynmap;
public class DynmapCore {
 public final boolean ic2; public DynmapCore(boolean value) { ic2=value; }
 public DynmapCore getServer() { return this; }
 public boolean isModLoaded(String name) { return ic2; }
}''',
    'org/dynmap/Log.java': '''package org.dynmap;
public class Log {
 public static int warnings;
 public static void info(String value) {}
 public static void warning(String value) { warnings++; }
 public static void warning(String value, Throwable cause) { warnings++; }
}''',
    'org/dynmap/hdmap/TexturePack.java': '''package org.dynmap.hdmap;
import java.util.HashMap;
public class TexturePack {
 public static int parsed; public static boolean fail;
 public static int parseTextureIndex(HashMap<String,Integer> map, String value) {
  parsed++; if (fail) throw new IllegalArgumentException("Missing required image");
  return 1;
 }
}''',
    'org/dynmap/modsupport/GWM_Util.java': '''package org.dynmap.modsupport;
public class GWM_Util {
 public static int objectToInt(Object value, int fallback) {
  return value == null ? fallback : Integer.parseInt(value.toString());
 }
}''',
    'org/dynmap/modsupport/ic2/CropsSupport.java': '''package org.dynmap.modsupport.ic2;
public class CropsSupport {
 public static int registered;
 public static void registerCrop(String name, int[] images) { registered++; }
}''',
    'ic2/api/crops/CropCard.java': '''package ic2.api.crops;
public class CropCard {
 private final String owner, name;
 public CropCard(String owner, String name) { this.owner=owner; this.name=name; }
 public String name() { return name; }
 public String owner() { return owner; }
}''',
    'ic2/api/crops/Crops.java': '''package ic2.api.crops;
import java.util.Collection;
public abstract class Crops {
 public static Crops instance;
 public abstract Collection<CropCard> getCrops();
}''',
    'CropRegistryHarness.java': '''import java.util.*;
import java.nio.file.*;
import org.dynmap.*;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.modsupport.loaders.IC2CropsSupportLoader;
import ic2.api.crops.*;
public class CropRegistryHarness {
 static HashMap<String,String> data(String owner, String name) {
  HashMap<String,String> d=new HashMap<>(); d.put("crop",name); d.put("steps","2");
  if(owner!=null)d.put("owner",owner); d.put("step0","image-a"); d.put("step1","image-b");
  return d;
 }
 static void check(boolean condition, String why) { if(!condition) throw new AssertionError(why); }
 public static void main(String[] args) throws Exception {
  Crops.instance=new Crops() { public Collection<CropCard> getCrops() {
   return Arrays.asList(new CropCard("IC2","wheat"),new CropCard("another-mod","Argentia"));
  }};
  IC2CropsSupportLoader l=new IC2CropsSupportLoader();
  l.initializeModSupport(new DynmapCore(true),new HashMap<String,Integer>());
  l.processData("",data("gregtech","Argentia"));
  check(TexturePack.parsed==0,"Same display name from another owner must not load legacy assets");
  l.processData("",data(null,"absent"));
  check(TexturePack.parsed==0,"Absent crop must be filtered before texture parsing");
  check(l.getSkippedDefinitionCount()==2 && l.getSkippedStageReferenceCount()==4,"Audit counters");
  l.processData("",data(null,"Argentia"));
  check(TexturePack.parsed==2,"Owner-less legacy records conservatively match any exact registered name");
  l.processData("",data("IC2","wheat"));
  check(TexturePack.parsed==4,"Registered required crop remains loaded");
  TexturePack.fail=true;
  try { l.processData("",data("IC2","wheat")); throw new AssertionError("Missing required asset was hidden"); }
  catch(IllegalArgumentException expected) {}
  TexturePack.fail=false;
  Crops.instance=null;
  l.initializeModSupport(new DynmapCore(true),new HashMap<String,Integer>());
  int before=TexturePack.parsed;
  l.processData("",data("gregtech","Argentia"));
  check(TexturePack.parsed==before+2 && Log.warnings==1,"Unavailable registry must warn and retain definitions");
  check(l.getSkippedDefinitionCount()==0,"Reinitialization must reset counters");
  Crops.instance=new Crops() { public Collection<CropCard> getCrops() { return Collections.emptyList(); }};
  l.initializeModSupport(new DynmapCore(true),new HashMap<String,Integer>());
  l.processData("",data("IC2","wheat"));
  check(l.getSkippedDefinitionCount()==1,"An initialized empty registry has no reachable crop recipes");
  // Run the actual 55 GT and two GT++ support records against an IC2-only registry.
  Crops.instance=new Crops() { public Collection<CropCard> getCrops() {
   return Arrays.asList(new CropCard("IC2","wheat"));
  }};
  l.initializeModSupport(new DynmapCore(true),new HashMap<String,Integer>());
  before=TexturePack.parsed;
  for(String filename:args) for(String line:Files.readAllLines(Paths.get(filename))) {
   if(!line.startsWith("moddata:mod=ic2crops,"))continue;
   HashMap<String,String> d=new HashMap<>();
   for(String token:line.substring(8).split(",")) { String[] pair=token.split("=",2); if(pair.length==2)d.put(pair[0],pair[1]); }
   l.processData(line,d);
  }
  check(l.getSkippedDefinitionCount()==57,"All original GT/GT++ legacy definitions must be gated");
  check(TexturePack.parsed==before,"Legacy recipes must not activate absent old stage PNGs");
  System.out.println("57 obsolete definitions gated; "+l.getSkippedStageReferenceCount()+" stage references left inactive");
 }
}''',
    'WithoutIc2Harness.java': '''import java.util.HashMap;
import org.dynmap.DynmapCore;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.modsupport.loaders.IC2CropsSupportLoader;
public class WithoutIc2Harness {
 public static void main(String[] args) {
  IC2CropsSupportLoader l=new IC2CropsSupportLoader();
  l.initializeModSupport(new DynmapCore(false),new HashMap<String,Integer>());
  HashMap<String,String> d=new HashMap<>();d.put("crop","legacy");d.put("step0","required");
  l.processData("",d);
  if(TexturePack.parsed!=1) throw new AssertionError("Missing optional mod changed legacy support behavior");
 }
}''',
}


class CropRegistryTest(unittest.TestCase):
    def test_registry_reachability_before_texture_parsing(self):
        with tempfile.TemporaryDirectory(prefix='gwm-crop-registry-') as tmp:
            folder = pathlib.Path(tmp)
            sources = folder / 'sources'
            classes = folder / 'classes'
            for name, source in STUBS.items():
                path = sources / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(source)
            for name in ['org/dynmap/renderer/CustomModSupportLoader.java',
                         'org/dynmap/modsupport/loaders/IC2CropsSupportLoader.java']:
                path = sources / name
                path.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(ROOT / 'src/main/java' / name, path)
            subprocess.run(['javac', '--release', '8', '-d', str(classes)] +
                           [str(p) for p in sources.rglob('*.java')], check=True)
            subprocess.run(['java', '-cp', str(classes), 'CropRegistryHarness'] +
                           [str(ROOT / 'src/main/resources/renderdata' / name) for name in
                            ['gregtech-texture.txt', 'miscutils-texture.txt']], check=True)
            # Verify real optional-mod isolation with no IC2 classes on the classpath.
            without_ic2 = folder / 'without-ic2'
            for source in classes.rglob('*.class'):
                relative = source.relative_to(classes)
                if relative.parts[0] == 'ic2' or relative.name.startswith('CropRegistryHarness'):
                    continue
                target = without_ic2 / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source, target)
            subprocess.run(['java', '-cp', str(without_ic2), 'WithoutIc2Harness'], check=True)


if __name__ == '__main__':
    unittest.main()
