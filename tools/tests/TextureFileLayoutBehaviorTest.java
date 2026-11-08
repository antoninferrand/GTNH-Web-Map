import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.dynmap.ConfigurationNode;
import org.dynmap.DynmapCore;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.hdmap.TexturePack.TileFileFormat;
import org.dynmap.hdmap.textureprocessor.CustomTextureProcessor;

/** Same PNG can serve full-block and independent atlas consumers without changing vanilla lookup. */
public class TextureFileLayoutBehaviorTest {
    static int assertions;
    static void check(boolean v,String reason){assertions++;if(!v)throw new AssertionError(reason);}
    static class Processor extends CustomTextureProcessor {
        public int getTextureCount(){return 2;}
        public void patchTextures(TexturePack p,int[] ids,int[] pixels,int w,int h,int scale){throw new AssertionError("unused processor decoded");}
    }
    static int file(String name,int x,int y,TileFileFormat format,String...args){return TexturePack.findOrAddDynamicTileFile(name,null,x,y,format,args,null);}
    public static void main(String[] args)throws Exception {
        DynmapCore core=new DynmapCore();core.setMinecraftVersion("1.7.10");core.setServer(new VanillaTexturePixelsTest.Server());core.setDataFolder(new File(args[0]));
        Method reset=TexturePack.class.getDeclaredMethod("resetFiles",DynmapCore.class);reset.setAccessible(true);reset.invoke(null,core);
        Method table=Class.forName("org.dynmap.hdmap.TexturePack$HDTextureMap").getDeclaredMethod("initializeTable");table.setAccessible(true);table.invoke(null);
        Method load=TexturePack.class.getDeclaredMethod("loadTextureFile",InputStream.class,String.class,ConfigurationNode.class,DynmapCore.class,String.class);load.setAccessible(true);
        String path="assets/minecraft/textures/blocks/grass_top.png";
        StringBuilder definition=new StringBuilder("texture:id=full,filename="+path+"\nblock:id=210,data=0,allfaces=0:full\ntexture:id=quarters,filename="+path+",xcount=2,ycount=2\n");
        for(int tile=0;tile<4;tile++)definition.append("block:id=").append(211+tile).append(",data=0,allfaces=").append(tile).append(":quarters\n");
        load.invoke(null,new ByteArrayInputStream(definition.toString().getBytes(StandardCharsets.UTF_8)),"fixture-shared-png-layouts.txt",new ConfigurationNode(),core,"fixture");
        check(TexturePack.getTextureDefinitionErrors().isEmpty(),"same-PNG atlas consumer aborted parser");
        for(int id=210;id<=214;id++)check(!TexturePack.isBlankBlockMapping(id,0),"full/atlas block mapping missing");
        int full=file(path,1,1,TileFileFormat.GRID),quarters=file(path,2,2,TileFileFormat.GRID);
        check(full!=quarters,"different grid dimensions aliased");
        check(quarters==file(path,2,2,TileFileFormat.GRID),"same layout did not reuse file ID");
        int fullTile=TexturePack.HDTextureMap.getMap(210,0,0).getIndexForFace(0);
        check(TexturePack.findDynamicTile(path,0)==fullTile,"canonical filename lookup changed to later atlas");
        check(TexturePack.findDynamicTile(path,1)==-1,"canonical filename lookup ambiguously exposed atlas slots");
        check(file("fixture-format.png",1,1,TileFileFormat.GRID)!=file("fixture-format.png",1,1,TileFileFormat.BIOME),"different formats aliased");
        int custom=file("fixture-custom.png",1,1,TileFileFormat.CUSTOM,"tile0=0:0/4:4","tile1=4:0/4:4");
        check(custom==file("fixture-custom.png",1,1,TileFileFormat.CUSTOM,"tile1=4:0/4:4","tile0=0:0/4:4"),"custom layout argument order changed identity");
        check(custom!=file("fixture-custom.png",1,1,TileFileFormat.CUSTOM,"tile0=0:0/8:4","tile1=4:0/4:4"),"different custom source rectangles aliased");
        Processor a=new Processor(),b=new Processor();
        int pa=TexturePack.findOrAddDynamicTileFile("fixture-processor.png",null,1,1,TileFileFormat.CUSTOMPROC,new String[0],a);
        check(pa==TexturePack.findOrAddDynamicTileFile("fixture-processor.png",null,1,1,TileFileFormat.CUSTOMPROC,new String[0],a),"same processor instance/layout not reused");
        check(pa!=TexturePack.findOrAddDynamicTileFile("fixture-processor.png",null,1,1,TileFileFormat.CUSTOMPROC,new String[0],b),"independent processors aliased");
        // Reuse preserves explicit resource option updates without leaking to a different layout.
        String optionsPath="fixture-options.png";
        int options=file(optionsPath,1,1,TileFileFormat.GRID);
        check(options==file(optionsPath,1,1,TileFileFormat.GRID,"resourcemod=owner","fallback=alternate.png","optional=true"),"same-layout option update created unrelated IDs");
        int other=file(optionsPath,2,2,TileFileFormat.GRID);
        Field files=TexturePack.class.getDeclaredField("addonfiles");files.setAccessible(true);List<?> records=(List<?>)files.get(null);
        Object one=records.get(options),two=records.get(other);Class<?> type=one.getClass();
        for(String field:new String[]{"modname","fallbackFilename"}){Field f=type.getDeclaredField(field);f.setAccessible(true);check(f.get(one)!=null&&f.get(two)==null,"resource option leaked across layouts: "+field);}
        Field optional=type.getDeclaredField("optional");optional.setAccessible(true);check(optional.getBoolean(one)&&!optional.getBoolean(two),"optionality leaked across layouts");
        Files.createDirectories(Paths.get(args[0],"texturepacks","standard"));TexturePack pack=TexturePack.getTexturePack(core,"standard");check(pack!=null,"texture pack failed");
        BufferedImage png;try(InputStream in=TextureFileLayoutBehaviorTest.class.getClassLoader().getResourceAsStream("texturepacks/standard/"+path)){png=ImageIO.read(in);}
        check(png.getWidth()==16&&png.getHeight()==16,"source PNG golden dimensions changed");
        int[] source=png.getRGB(0,0,16,16,null,0,16),fullPixels=pack.getTileARGB(fullTile);
        for(int i=0;i<256;i++)check(fullPixels[i]==source[i],"full-image consumer lost native source pixel");
        for(int tile=0;tile<4;tile++) {
            int[] actual=pack.getTileARGB(TexturePack.HDTextureMap.getMap(211+tile,0,0).getIndexForFace(0));
            // Each source 8x8 quadrant scales by integer 2 into the native 16x16 tile.
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)check(actual[y*16+x]==source[(tile/2*8+y/2)*16+tile%2*8+x/2],"atlas quadrant source pixel mismatch");
        }
        reset.invoke(null,core);Field cacheField=TexturePack.class.getDeclaredField("addonfilesbylayout");cacheField.setAccessible(true);
        check(!((Map<?,?>)cacheField.get(null)).containsValue(one),"reset retained an old semantic entry");
        System.out.println("Dynamic texture layouts: "+assertions+" production parser/cache/source-pixel assertions passed");
    }
}
