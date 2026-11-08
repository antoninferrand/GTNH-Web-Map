"""Compile production read-only Chisel reflection audit against registry fixtures."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]


def method(source, signature):
    start = source.index(signature)
    end = source.index('{', start) + 1
    depth = 1
    while depth:
        if source[end] == '{': depth += 1
        elif source[end] == '}': depth -= 1
        end += 1
    return source[start:end]


class ChiselVariantAuditTest(unittest.TestCase):
    def test_production_reflection(self):
        source = (ROOT / 'src/main/java/org/dynmap/forge/RenderAuditSubCommand.java').read_text()
        self.assertIn('report.put("chiselVariants", auditChiselVariants(registered));', source)
        methods = '\n'.join(method(source, signature) for signature in (
            'private static Map<String, Object> auditChiselVariants(', 'private static Field inheritedField('))
        sources = {
            'net/minecraft/block/Block.java': '''package net.minecraft.block; import java.util.*;
public class Block {public static final Map<Integer,Block> registry=new HashMap<>(); public static Block getBlockById(int id){return registry.get(id);}}''',
            'ChiselAuditHarness.java': '''import java.util.*;import java.lang.reflect.*;import net.minecraft.block.Block;
public class ChiselAuditHarness {
 %METHODS%
 static class Helper {private Object[] infoMap=new Object[16];Helper(int mask){for(int i=0;i<16;i++)if((mask&(1<<i))!=0)infoMap[i]=new Object();}}
 static class ParentBlock extends Block {private Helper carverHelper;ParentBlock(int mask){carverHelper=new Helper(mask);}}
 static class ChildBlock extends ParentBlock {ChildBlock(int mask){super(mask);}}
 static class NullHelper extends Block {private Object carverHelper;}
 static class UnknownHelper extends Block {private Object carverHelper=new Object();}
 static void check(boolean valid,String reason){if(!valid)throw new AssertionError(reason);}
 @SuppressWarnings("unchecked") public static void main(String[] args){
  Map<Integer,String> registry=new TreeMap<>();
  String[] names={"chisel:valid","chisel:empty","chisel:nohelper","chisel:nullhelper","chisel:unknownhelper","chisel:missing","other:ignored"};
  for(int i=0;i<names.length;i++)registry.put(i,names[i]);
  Block.registry.put(0,new ChildBlock(0x8005));Block.registry.put(1,new ChildBlock(0));Block.registry.put(2,new Block());Block.registry.put(3,new NullHelper());Block.registry.put(4,new UnknownHelper());Block.registry.put(6,new ChildBlock(1));
  Map<String,Object> result=auditChiselVariants(registry);check(result.size()==6,"only Chisel registry names audited");
  Map<String,Object> valid=(Map<String,Object>)result.get("chisel:valid");check(valid.get("status").equals("OK"),"inherited helper/private infoMap readable");check(valid.get("nonnullMetadataMask").equals(0x8005),"bit mask retains meta15");check(valid.get("nonnullMetadata").toString().equals("[0, 2, 15]"),"metadata list matches mask");check(valid.get("infoMapLength").equals(16),"infoMap shape reported");
  check(((Map<String,Object>)result.get("chisel:empty")).get("nonnullMetadataMask").equals(0),"empty helpers reported as mask0");
  check(((Map<String,Object>)result.get("chisel:nohelper")).get("status").equals("NO_HELPER"),"noncarvable marker");check(((Map<String,Object>)result.get("chisel:nullhelper")).get("status").equals("NULL_HELPER"),"nullhelper marker");check(((Map<String,Object>)result.get("chisel:unknownhelper")).get("status").equals("UNAVAILABLE"),"unsupported helper explicit failure");check(((Map<String,Object>)result.get("chisel:missing")).get("status").equals("UNAVAILABLE_BLOCK"),"missing block explicit failure");
  System.out.println("Chisel variation audit: 10 production assertions passed");
 }
}'''.replace('%METHODS%', methods)
        }
        with tempfile.TemporaryDirectory() as folder:
            root = pathlib.Path(folder)
            paths = []
            for name, text in sources.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text)
                paths.append(str(path))
            subprocess.run(['javac', '--release', '8', '-d', folder] + paths, check=True)
            subprocess.run(['java', '-cp', folder, 'ChiselAuditHarness'], check=True)


if __name__ == '__main__':
    unittest.main()
