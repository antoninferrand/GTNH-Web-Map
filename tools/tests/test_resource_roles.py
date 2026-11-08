"""Compile production resource loader and terrain-role methods with tiny server fixtures."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]


def extract(source, signature):
    start = source.index(signature)
    end = source.index('{', start) + 1
    depth = 1
    while depth:
        if source[end] == '{': depth += 1
        elif source[end] == '}': depth -= 1
        end += 1
    return source[start:end]


class ResourceRolesTest(unittest.TestCase):
    def test_required_resources_and_optional_probes(self):
        pack = (ROOT / 'src/main/java/org/dynmap/hdmap/TexturePack.java').read_text()
        self.assertIn('if (isInactiveTerrainVariant(dtf, is_rp))', pack)
        self.assertIn('dtf.terrainVariant = 1;', pack)
        self.assertIn('dtf.terrainVariant = 2;', pack)
        self.assertIn('f.explicitlyUsed = true;', pack)
        self.assertIn('file.explicitlyUsed = wasExplicitlyUsed;', pack)
        constructor = pack[pack.index('private TexturePack(DynmapCore core, String tpname)'):]
        constructor = constructor[:constructor.index('private void copySubimageFromImage(')]
        self.assertNotIn('tpl.openTPResource(', constructor)
        self.assertIn('tpl.openModTPResource(dtf.filename', constructor)
        methods = '\n'.join(extract(pack, signature) for signature in [
            'public static String getResourceRole(', 'private static boolean isInactiveTerrainVariant('])
        stubs = {
            'org/dynmap/Log.java': 'package org.dynmap; public class Log { public static void severe(String s) {} public static void info(String s) {} public static void warning(String s) {} }',
            'org/dynmap/common/DynmapServerInterface.java': '''package org.dynmap.common;
import java.io.*; import java.util.*;
public class DynmapServerInterface {
 public final Set<String> resources=new HashSet<String>();
 public InputStream openResource(String mod,String path) {return resources.contains(path)?new ByteArrayInputStream(new byte[]{1}):null;}
 public File getModContainerFile(String mod) {return null;}
}''',
            'org/dynmap/DynmapCore.java': '''package org.dynmap; public class DynmapCore {
 public final org.dynmap.common.DynmapServerInterface server=new org.dynmap.common.DynmapServerInterface();
 public org.dynmap.common.DynmapServerInterface getServer() {return server;}
}''',
            'org/dynmap/hdmap/TexturePack.java': '''package org.dynmap.hdmap;
import java.util.*;
public class TexturePack {
 static final Object packlock=new Object(); static Map<String,TexturePack> packs=new HashMap<>();
 Set<String> inactiveTerrainVariants=new HashSet<>(), optionalResourceProbes=new HashSet<>();
 static class DynamicTileFile {boolean explicitlyUsed;int terrainVariant;}
 public static boolean inactive(int variant,boolean explicit,boolean rp) {DynamicTileFile f=new DynamicTileFile();f.terrainVariant=variant;f.explicitlyUsed=explicit;return isInactiveTerrainVariant(f,rp);}
 public static void fixture() {TexturePack p=new TexturePack();p.inactiveTerrainVariants.add("textures/blocks/stone.png");p.optionalResourceProbes.add("color.properties");packs.put("standard",p);}
 %METHODS%
}'''.replace('%METHODS%', methods),
            'RoleHarness.java': '''import org.dynmap.*;import org.dynmap.hdmap.*;import java.io.*;import java.util.*;
public class RoleHarness {
 static void check(boolean v,String s) {if(!v)throw new AssertionError(s);}
 static TextureResourceResolver.Outcome outcome(TexturePackLoader loader,String path) {for(TextureResourceResolver.Lookup l:loader.getResourceLookups())if(l.resource.equals(path))return l.outcome;throw new AssertionError(path);}
 public static void main(String[] args) {
  DynmapCore core=new DynmapCore();TexturePackLoader loader=new TexturePackLoader(new File(args[0]),core);
  loader.openOptionalTPResource("color.properties");check(outcome(loader,"color.properties")==TextureResourceResolver.Outcome.OPTIONAL_ABSENT,"optional pack feature");
  check(loader.getOptionalProbeResources().contains("color.properties"),"probe role retained");
  loader.openTPResource("custom-required.png");check(outcome(loader,"custom-required.png")==TextureResourceResolver.Outcome.REQUIRED_MISSING,"null-owner custom required image retained");
  loader.openTPResource("assets/minecraft/textures/blocks/stone.png");check(outcome(loader,"assets/minecraft/textures/blocks/stone.png")==TextureResourceResolver.Outcome.REQUIRED_MISSING,"active vanilla missing remains required");
  loader.openOptionalTPResource("custom-required.png");check(outcome(loader,"custom-required.png")==TextureResourceResolver.Outcome.REQUIRED_MISSING,"later probe cannot downgrade required image");
  check(!loader.getOptionalProbeResources().contains("custom-required.png"),"required path cannot gain probe role");
  loader.openModTPResource("color.properties",null);check(outcome(loader,"color.properties")==TextureResourceResolver.Outcome.REQUIRED_MISSING,"later real declaration overrides earlier probe");
  check(!loader.getOptionalProbeResources().contains("color.properties"),"real declaration clears probe role");
  core.server.resources.add("alternate.png");InputStream alt=loader.openOptionalTPResource("absent.png","alternate.png");check(alt!=null,"optional alternate stream retained");loader.closeResource(alt);
  check(outcome(loader,"alternate.png")==TextureResourceResolver.Outcome.ORIGINAL,"present optional images loaded normally");
  check(TexturePack.inactive(2,false,true),"legacy branch inactive in RP");check(!TexturePack.inactive(1,false,true),"RP branch active");
  check(TexturePack.inactive(1,false,false),"RP branch inactive in legacy pack");check(!TexturePack.inactive(2,false,false),"legacy branch active");
  check(!TexturePack.inactive(2,true,true),"explicitly consumed legacy image remains required in RP");check(!TexturePack.inactive(1,true,false),"explicitly consumed RP image remains required in legacy pack");
  check(!TexturePack.inactive(0,false,true),"custom null-owner image never a vanilla variant");
  TexturePack.fixture();check(TexturePack.getResourceRole("standard","textures/blocks/stone.png").equals("INACTIVE_TERRAIN_VARIANT"),"exact inactive role");
  check(TexturePack.getResourceRole("standard","color.properties").equals("OPTIONAL_PROBE"),"exact optional role");
  check(TexturePack.getResourceRole("standard","custom-required.png").equals("TEXTURE"),"ordinary textures retain role");
  check(TexturePack.getResourceRole("absent","x").equals("UNKNOWN_PACK"),"unloaded pack explicit");
  loader.close();System.out.println("Resource roles: 21 production assertions passed");
 }
}'''
        }
        with tempfile.TemporaryDirectory() as temporary:
            root = pathlib.Path(temporary)
            sources = []
            for name, source in stubs.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(source)
                sources.append(str(path))
            sources += [str(ROOT / 'src/main/java/org/dynmap/hdmap' / name) for name in ('TexturePackLoader.java', 'TextureResourceResolver.java')]
            subprocess.run(['javac', '--release', '8', '-d', str(root)] + sources, check=True)
            subprocess.run(['java', '-cp', str(root), 'RoleHarness', str(root)], check=True)


if __name__ == '__main__':
    unittest.main()
