import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.dynmap.ConfigurationNode;
import org.dynmap.DynmapCore;
import org.dynmap.hdmap.TexturePack;

/** Exercise the actual support-file parser, including its abort boundary and diagnostic reset. */
public class TextureDefinitionDiagnosticsTest {
    static int assertions;
    static Method load;
    static DynmapCore core;
    static void check(boolean valid,String reason){assertions++;if(!valid)throw new AssertionError(reason);}
    static void parse(String name,String text)throws Exception {
        load.invoke(null,new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),name,new ConfigurationNode(),core,"fixture");
    }
    static final String SINGLE="texture:id=single,filename=fixtures/single.png\n";
    static String block(int id,int tile){return "block:id="+id+",data=0,allfaces="+tile+":single\n";}
    public static void main(String[] args)throws Exception {
        core=new DynmapCore();core.setMinecraftVersion("1.7.10");
        Method reset=TexturePack.class.getDeclaredMethod("resetFiles",DynmapCore.class);reset.setAccessible(true);
        load=TexturePack.class.getDeclaredMethod("loadTextureFile",java.io.InputStream.class,String.class,ConfigurationNode.class,DynmapCore.class,String.class);load.setAccessible(true);
        Method table=Class.forName("org.dynmap.hdmap.TexturePack$HDTextureMap").getDeclaredMethod("initializeTable");table.setAccessible(true);table.invoke(null);
        reset.invoke(null,core);check(TexturePack.getTextureDefinitionErrors().isEmpty(),"clean parser starts with errors");
        check(TexturePack.isBlankBlockMapping(202,0),"later block fixture not initially blank");
        parse("fixture-invalid-texture.txt",SINGLE+block(201,1)+block(202,0));
        List<String> errors=TexturePack.getTextureDefinitionErrors();check(errors.size()==1,"invalid tile must record one error");
        check(errors.get(0).contains("fixture-invalid-texture.txt:2:"),"diagnostic must identify exact support file and line");
        check(errors.get(0).contains("Invalid index 1"),"diagnostic must retain invalid tile reason");
        check(TexturePack.isBlankBlockMapping(201,0),"invalid block unexpectedly installed");
        check(TexturePack.isBlankBlockMapping(202,0),"following valid block must remain blank after parser abort");
        try{errors.add("bad");throw new AssertionError("mutable diagnostic snapshot");}catch(UnsupportedOperationException expected){assertions++;}
        reset.invoke(null,core);check(TexturePack.getTextureDefinitionErrors().isEmpty(),"resetFiles did not clear old errors");
        check(errors.size()==1,"reset mutated an existing audit snapshot");
        parse("fixture-valid-texture.txt",SINGLE+block(203,0)+block(204,0));
        check(!TexturePack.isBlankBlockMapping(203,0)&&!TexturePack.isBlankBlockMapping(204,0),"valid later support file did not install mappings");
        check(TexturePack.getTextureDefinitionErrors().isEmpty(),"valid file reported an error");
        parse("fixture-disabled-texture.txt","enabled:false\n"+SINGLE+block(205,1));
        check(TexturePack.isBlankBlockMapping(205,0),"disabled file unexpectedly installed mappings");
        check(TexturePack.getTextureDefinitionErrors().isEmpty(),"intentional disabled-file guard counted as error");
        parse("fixture-version-texture.txt","version:9.9\n"+SINGLE+block(206,1));
        check(TexturePack.isBlankBlockMapping(206,0),"version guard unexpectedly installed mapping");
        check(TexturePack.getTextureDefinitionErrors().isEmpty(),"intentional file-version guard counted as error");
        parse("fixture-inline-version-texture.txt",SINGLE+"[9.9]"+block(207,1)+block(208,0));
        check(TexturePack.isBlankBlockMapping(207,0)&&!TexturePack.isBlankBlockMapping(208,0),"inline version guard should skip only its line");
        check(TexturePack.getTextureDefinitionErrors().isEmpty(),"intentional inline-version guard counted as error");
        System.out.println("Texture definition diagnostics: "+assertions+" production parser/abort/reset/guard assertions passed");
    }
}
