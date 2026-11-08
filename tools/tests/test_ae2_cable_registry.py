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


if __name__ == "__main__":
    unittest.main()
