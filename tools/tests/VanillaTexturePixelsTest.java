import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.dynmap.*;
import org.dynmap.common.*;
import org.dynmap.common.DynmapListenerManager.EventType;
import org.dynmap.hdmap.*;
import org.dynmap.utils.MapChunkCache;

/** Load real bundled vanilla images through the production TexturePack constructor. */
public class VanillaTexturePixelsTest {
    static int assertions;
    static void check(boolean value, String reason) { assertions++; if (!value) throw new AssertionError(reason); }
    static class Server extends DynmapServerInterface {
        public void scheduleServerTask(Runnable r,long d) { throw new AssertionError("unexpected server work"); }
        public <T> Future<T> callSyncMethod(Callable<T> task) { throw new AssertionError("unexpected server work"); }
        public DynmapPlayer[] getOnlinePlayers(){return new DynmapPlayer[0];}
        public void reload(){throw new AssertionError("unexpected reload");}
        public DynmapPlayer getPlayer(String s){return null;} public DynmapPlayer getOfflinePlayer(String s){return null;}
        public Set<String> getIPBans(){return Collections.emptySet();} public String getServerName(){return "fixture";}
        public boolean isPlayerBanned(String s){return false;}public String stripChatColor(String s){return s;}
        public boolean requestEventNotification(EventType e){return false;}public boolean sendWebChatEvent(String a,String b,String c){throw new AssertionError("unexpected message");}
        public void broadcastMessage(String s){throw new AssertionError("unexpected message");}public String[] getBiomeIDs(){return new String[0];}
        public double getCacheHitRate(){return 0;} public void resetCacheStats(){} public DynmapWorld getWorldByName(String s){return null;}
        public Set<String> checkPlayerPermissions(String p,Set<String> s){return Collections.emptySet();} public boolean checkPlayerPermission(String p,String s){return false;}
        public MapChunkCache createMapChunkCache(DynmapWorld w,List<DynmapChunk> c,boolean a,boolean b,boolean d,boolean e){throw new AssertionError("unexpected world read");}
        public int getMaxPlayers(){return 0;}public int getCurrentPlayers(){return 0;}public int getBlockIDAt(String w,int x,int y,int z){throw new AssertionError("unexpected world read");}
        public int isSignAt(String w,int x,int y,int z){return 0;}public double getServerTPS(){return 20;}public String getServerIP(){return "127.0.0.1";}
    }
    static final String[] NAMES={"grass_top","stone","dirt"};
    static byte[] png(String name)throws Exception {
        try(InputStream in=VanillaTexturePixelsTest.class.getClassLoader().getResourceAsStream("texturepacks/standard/assets/minecraft/textures/blocks/"+name+".png")) {
            check(in!=null,"bundled vanilla image missing: "+name);ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);return bytes.toByteArray();
        }
    }
    static TextureResourceResolver.Lookup lookup(String pack,String path) {
        for(TextureResourceResolver.Lookup row:TexturePack.getLoadedResourceLookups().get(pack))if(row.resource.equals(path))return row;
        return null;
    }
    static void pixels(TexturePack pack)throws Exception {
        for(int i=0;i<NAMES.length;i++) {
            BufferedImage expected=ImageIO.read(new ByteArrayInputStream(png(NAMES[i])));
            int[] source=expected.getRGB(0,0,expected.getWidth(),expected.getHeight(),null,0,expected.getWidth());
            int[] actual=pack.getTileARGB(i);check(actual.length==source.length,"native vanilla resolution changed");
            for(int p=0;p<source.length;p++)check(actual[p]==source[p],"vanilla pixel changed: "+NAMES[i]+" pixel"+p);
        }
    }
    public static void main(String[] args)throws Exception {
        DynmapCore core=new DynmapCore();core.setServer(new Server());core.setDataFolder(new File(args[0]));
        Method reset=TexturePack.class.getDeclaredMethod("resetFiles",DynmapCore.class);reset.setAccessible(true);
        // Active resource-pack branch: known modern images retain every original pixel.
        reset.invoke(null,core);Files.createDirectories(Paths.get(args[0],"texturepacks","standard"));
        TexturePack rp=TexturePack.getTexturePack(core,"standard");check(rp!=null,"RP construction failed");pixels(rp);
        for(String name:NAMES) {
            check(lookup("standard","assets/minecraft/textures/blocks/"+name+".png").outcome==TextureResourceResolver.Outcome.ORIGINAL,"active RP image did not load");
            check(lookup("standard","textures/blocks/"+name+".png")==null,"inactive legacy variant was looked up");
        }
        check(TexturePack.getInactiveTerrainVariants().get("standard").contains("textures/blocks/stone.png"),"skipped legacy provenance missing");
        // An explicit support consumer restores the legacy image's real requirement.
        reset.invoke(null,core);HashMap<String,Integer> textures=new HashMap<>();
        int legacy=TexturePack.findOrAddDynamicTileFile("textures/blocks/stone.png",null,1,1,TexturePack.TileFileFormat.GRID,new String[0],null);
        textures.put("explicitLegacy",legacy);TexturePack.parseTextureIndex(textures,"0:explicitLegacy");
        int custom=TexturePack.findOrAddDynamicTileFile("fixture-required.png",null,1,1,TexturePack.TileFileFormat.GRID,new String[0],null);
        textures.put("custom",custom);TexturePack.parseTextureIndex(textures,"0:custom");
        rp=TexturePack.getTexturePack(core,"standard");pixels(rp);
        check(lookup("standard","textures/blocks/stone.png").outcome==TextureResourceResolver.Outcome.REQUIRED_MISSING,"explicit legacy requirement hidden");
        check(lookup("standard","fixture-required.png").outcome==TextureResourceResolver.Outcome.REQUIRED_MISSING,"custom null-owner requirement hidden");
        check(TexturePack.getResourceRole("standard","textures/blocks/stone.png").equals("TEXTURE"),"explicit legacy classified inactive");
        // Legacy pack branch: load actual old-format files, retaining their native pixels.
        reset.invoke(null,core);Path legacyDir=Paths.get(args[0],"texturepacks","legacy-fixture","textures","blocks");Files.createDirectories(legacyDir);
        for(String name:NAMES)Files.write(legacyDir.resolve(name+".png"),png(name));
        TexturePack old=TexturePack.getTexturePack(core,"legacy-fixture");check(old!=null,"legacy construction failed");pixels(old);
        for(String name:NAMES) {
            check(lookup("legacy-fixture","textures/blocks/"+name+".png").outcome==TextureResourceResolver.Outcome.ORIGINAL,"active legacy image did not load");
            check(lookup("legacy-fixture","assets/minecraft/textures/blocks/"+name+".png")==null,"inactive RP variant was looked up");
        }
        System.out.println("Vanilla TexturePack construction: "+assertions+" real pixel/format/explicit-consumer assertions passed");
    }
}
