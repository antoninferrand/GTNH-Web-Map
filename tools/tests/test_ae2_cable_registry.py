"""Exercise the production AE2 loader before texture resolution with a part registry."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
STUBS = {
    "org/dynmap/DynmapCore.java": "package org.dynmap; public class DynmapCore {}",
    "org/dynmap/Log.java": "package org.dynmap; public class Log { public static int warnings; public static void warning(String s) { warnings++; } }",
    "org/dynmap/modsupport/GWM_Util.java": "package org.dynmap.modsupport; public class GWM_Util { public static void initialize(org.dynmap.DynmapCore core) {} }",
    "net/minecraft/item/ItemStack.java": "package net.minecraft.item; public class ItemStack { public final int damage; public ItemStack(Object item,int n,int d) { damage=d; } }",
    "appeng/items/parts/PartType.java": "package appeng.items.parts; public enum PartType { InvalidType, Cable }",
    "appeng/items/parts/ItemMultiPart.java": """package appeng.items.parts;
import java.util.*; import net.minecraft.item.ItemStack;
public class ItemMultiPart {
 public static ItemMultiPart instance; public static boolean unavailable;
 public final Set<Integer> registered=new HashSet<Integer>();
 public PartType getTypeByStack(ItemStack s) {
  if(unavailable) throw new NoSuchMethodError("optional API changed");
  return registered.contains(s.damage) ? PartType.Cable : PartType.InvalidType;
 }
}""",
    "org/dynmap/modsupport/appliedenergistics2/AE2Support.java": """package org.dynmap.modsupport.appliedenergistics2;
import java.util.HashMap;
public class AE2Support {
 public static final Object[] cableTypes=new Object[32768];
 public static int calls; public static boolean fail;
 public static void addCableType(HashMap<String,Integer> files,int damage,String a,String b,String c,int size) {
  if(fail) throw new IllegalArgumentException("registered required resource missing");
  calls++;
 }
}""",
    "CableRegistryHarness.java": """import java.util.*;
import org.dynmap.*; import org.dynmap.modsupport.loaders.AppliedEnergisticsModSupportLoader;
import org.dynmap.modsupport.appliedenergistics2.AE2Support;
import appeng.items.parts.*;
public class CableRegistryHarness {
 static HashMap<String,String> data(int damage) {
  HashMap<String,String> d=new HashMap<String,String>();
  d.put("damage",Integer.toString(damage)); d.put("size","5");
  d.put("tex","0:image"); d.put("tex2","1:image"); d.put("tex3","2:image"); return d;
 }
 static void check(boolean valid,String reason) { if(!valid) throw new AssertionError(reason); }
 public static void main(String[] args) {
  AppliedEnergisticsModSupportLoader loader=new AppliedEnergisticsModSupportLoader();
  loader.initializeModSupport(new DynmapCore(),new HashMap<String,Integer>());
  ItemMultiPart.instance=new ItemMultiPart(); ItemMultiPart.instance.registered.add(0);
  loader.processData("",data(0)); check(AE2Support.calls==1,"registered cable retained");
  loader.processData("",data(541)); check(AE2Support.calls==1,"absent cable never resolves images");
  ItemMultiPart.instance.registered.add(541);
  loader.processData("",data(541)); check(AE2Support.calls==2,"old registered ultra-dense cable retained");
  AE2Support.fail=true;
  try { loader.processData("",data(0)); throw new AssertionError("required texture failure swallowed"); }
  catch(IllegalArgumentException expected) {}
  AE2Support.fail=false; ItemMultiPart.instance=null;
  loader.processData("",data(541)); check(AE2Support.calls==3 && Log.warnings==1,"unavailable registry fails open");
  loader.processData("",data(541)); check(Log.warnings==1,"registry warning only once");
  loader.processData("",data(-1)); loader.processData("",data(32768));
  check(AE2Support.calls==4,"invalid metadata never resolves images");
  ItemMultiPart.instance=new ItemMultiPart(); ItemMultiPart.unavailable=true;
  loader.processData("",data(541)); check(AE2Support.calls==5,"optional API linkage failure retains textures");
  System.out.println("AE2 registry reachability and required failures passed");
 }
}""",
}


def production_texture_methods():
    """Compile the actual allocation/parsing methods, avoiding a mirrored implementation."""
    source = (ROOT / "src/main/java/org/dynmap/hdmap/TexturePack.java").read_text()
    assert "reserveProcessorTiles(filetoidx, fid, id, num);" in source
    methods = []
    for signature in [
        "private static void reserveProcessorTiles(",
        "public static int findOrAddDynamicTile(",
        "public static int parseTextureIndex(HashMap<String,Integer> filetoidx, String val)",
        "private static int parseTextureIndex(HashMap<String,Integer> filetoidx, int srctxtid, String val)",
    ]:
        start = source.index(signature)
        opening = source.index("{", start)
        depth = 1
        end = opening + 1
        while depth:
            if source[end] == "{": depth += 1
            elif source[end] == "}": depth -= 1
            end += 1
        methods.append(source[start:end])
    return "\n".join(methods)


PROCESSOR_TEXTURE_STUB = """package org.dynmap.hdmap;
import java.util.*; import org.dynmap.modsupport.GWM_Util;
public class TexturePack {
 static final int TXTID_TERRAINPNG=-1, TXTID_INVALID=-2;
 static final int COLORMOD_MULT_FILE=1000, COLORMOD_MULT_INTERNAL=1000000;
 static int next_dynamic_tile=100;
 static class DynamicTileFile {
  boolean used, explicitlyUsed; int[] tile_to_dyntile={-1,-1,-1}; String filename="legacy-cable"; String modname="ae2";
 }
 static ArrayList<DynamicTileFile> addonfiles=new ArrayList<DynamicTileFile>();
 public static HashMap<String,Integer> declarations=new HashMap<String,Integer>();
 public static void declare() {
  addonfiles.add(new DynamicTileFile()); declarations.put("image",0);
  reserveProcessorTiles(declarations,0,"image",3);
 }
 public static boolean used() { return addonfiles.get(0).used; }
 public static int[] reserved() { return addonfiles.get(0).tile_to_dyntile; }
 public static void reserveAgain() { reserveProcessorTiles(declarations,0,"image",3); }
 %METHODS%
}
"""

PROCESSOR_HARNESS = """import java.util.*;
import org.dynmap.*; import org.dynmap.hdmap.TexturePack;
import org.dynmap.modsupport.loaders.AppliedEnergisticsModSupportLoader;
import appeng.items.parts.*;
public class ProcessorReachabilityHarness {
 static void check(boolean valid,String reason) { if(!valid) throw new AssertionError(reason); }
 static HashMap<String,String> cable(int metadata) {
  HashMap<String,String> d=new HashMap<String,String>();
  d.put("damage",Integer.toString(metadata));d.put("size","5");
  d.put("tex","0:image");d.put("tex2","1:image");d.put("tex3","2:image");return d;
 }
 public static void main(String[] args) {
  TexturePack.declare();check(!TexturePack.used(),"processor reservation does not activate legacy resources");
  int[] ids=TexturePack.reserved();check(ids[0]>=100 && ids[1]==ids[0]+1 && ids[2]==ids[1]+1,"outputs stay contiguous");
  AppliedEnergisticsModSupportLoader loader=new AppliedEnergisticsModSupportLoader();
  loader.initializeModSupport(new DynmapCore(),TexturePack.declarations);
  ItemMultiPart.instance=new ItemMultiPart();
  loader.processData("",cable(541));check(!TexturePack.used(),"absent cable remains inactive after real registry gate");
  ItemMultiPart.instance.registered.add(541);
  loader.processData("",cable(541));check(TexturePack.used(),"registered legacy cable activates required images");
  TexturePack.reserveAgain();check(TexturePack.used(),"repeated reservation preserves prior real use");
  check(TexturePack.parseTextureIndex(TexturePack.declarations,"2:image")==ids[2],"reserved IDs stable");
  check(TexturePack.parseTextureIndex(TexturePack.declarations,"23000:image")==ids[0]+23000000,"modifiers preserved");
  System.out.println("Production processor allocation and AE2 registry integration passed");
 }
}
"""


class CableRegistryTest(unittest.TestCase):
    def test_production_loader(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = pathlib.Path(temporary)
            sources = []
            for name, source in STUBS.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(source)
                sources.append(str(path))
            sources += [str(ROOT / "src/main/java/org/dynmap/renderer/CustomModSupportLoader.java"),
                        str(ROOT / "src/main/java/org/dynmap/modsupport/loaders/AppliedEnergisticsModSupportLoader.java")]
            subprocess.run(["javac", "--release", "8", "-d", str(root)] + sources, check=True)
            subprocess.run(["java", "-cp", str(root), "CableRegistryHarness"], check=True)


    def test_processor_declaration_and_registry_gate(self):
        stubs = dict(STUBS)
        stubs.pop("CableRegistryHarness.java")
        stubs["org/dynmap/modsupport/GWM_Util.java"] = (
            "package org.dynmap.modsupport; public class GWM_Util { "
            "public static void initialize(org.dynmap.DynmapCore core) {} "
            "public static void tryRegisterTextureByName(String name,int id) {} }")
        stubs["org/dynmap/hdmap/TexturePack.java"] = PROCESSOR_TEXTURE_STUB.replace(
            "%METHODS%", production_texture_methods())
        stubs["org/dynmap/modsupport/appliedenergistics2/AE2Support.java"] = (
            "package org.dynmap.modsupport.appliedenergistics2; import java.util.*; "
            "import org.dynmap.hdmap.TexturePack; public class AE2Support { "
            "public static Object[] cableTypes=new Object[32768]; "
            "public static void addCableType(HashMap<String,Integer> map,int damage,String a,String b,String c,int size) { "
            "TexturePack.parseTextureIndex(map,a);TexturePack.parseTextureIndex(map,b);TexturePack.parseTextureIndex(map,c); } }")
        stubs["ProcessorReachabilityHarness.java"] = PROCESSOR_HARNESS
        with tempfile.TemporaryDirectory() as temporary:
            root = pathlib.Path(temporary)
            sources = []
            for name, source in stubs.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(source)
                sources.append(str(path))
            sources += [str(ROOT / "src/main/java/org/dynmap/renderer/CustomModSupportLoader.java"),
                        str(ROOT / "src/main/java/org/dynmap/modsupport/loaders/AppliedEnergisticsModSupportLoader.java")]
            subprocess.run(["javac", "--release", "8", "-d", str(root)] + sources, check=True)
            subprocess.run(["java", "-cp", str(root), "ProcessorReachabilityHarness"], check=True)


if __name__ == "__main__":
    unittest.main()
