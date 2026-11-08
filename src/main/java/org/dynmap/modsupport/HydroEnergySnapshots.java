package org.dynmap.modsupport;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.dynmap.*;
import org.dynmap.forge.ForgeWorld;

/** Copies optional HydroEnergy global state on the server thread; renderers never read HEServer. */
public final class HydroEnergySnapshots {
    public static final class Dam {
        public final float level;
        public final int dimension;
        private final int[] bounds;
        public final String world;
        Dam(NBTTagCompound tag,String world) {
            this.world=world;
            level=tag.getFloat("walv");dimension=tag.getInteger("dimI");
            bounds=new int[]{tag.getInteger("limW"),tag.getInteger("limD"),tag.getInteger("limN"),
                    tag.getInteger("limE"),tag.getInteger("limU"),tag.getInteger("limS")};
        }
        @Override public boolean equals(Object other) {
            if(!(other instanceof Dam))return false;Dam dam=(Dam)other;
            return Float.floatToIntBits(level)==Float.floatToIntBits(dam.level)&&dimension==dam.dimension&&Objects.equals(world,dam.world)&&Arrays.equals(bounds,dam.bounds);
        }
        @Override public int hashCode(){return Float.floatToIntBits(level)*31+dimension;}
    }
    private static final Set<Integer> ids=new CopyOnWriteArraySet<>();
    private static volatile Map<Integer,Dam> snapshot=Collections.emptyMap();
    private static boolean registered;
    private HydroEnergySnapshots(){}

    public static Dam get(int id,String world){Dam dam=snapshot.get(id);return dam!=null&&world!=null&&world.equals(dam.world)?dam:null;}

    public static synchronized void register(int id) throws ReflectiveOperationException {
        ids.add(id);
        if(registered)return;
        Hook hook=new Hook();
        FMLCommonHandler.instance().bus().register(hook);
        DynmapCommonAPIListener.register(hook);
        registered=true;
    }

    /** One complete publication, before invalidating any tiles which will consume it. */
    static void publish(Map<Integer,Dam> next,DynmapCommonAPI api) {
        Map<Integer,Dam> previous=snapshot;
        snapshot=Collections.unmodifiableMap(new HashMap<>(next));
        if(api==null)return;
        for(Integer id:ids) {
            Dam before=previous.get(id),after=next.get(id);
            if(Objects.equals(before,after))continue;
            invalidate(before,api);invalidate(after,api);
        }
    }

    private static void invalidate(Dam dam,DynmapCommonAPI api) {
        if(dam==null)return;
        WorldServer world=DimensionManager.getWorld(dam.dimension);
        if(world==null)return;
        int[] b=dam.bounds;
        if(b[0]>b[3]||b[1]>b[4]||b[2]>b[5])return;
        api.triggerRenderOfVolume(ForgeWorld.getWorldName(world),b[0],b[1],b[2],b[3],b[4],b[5]);
    }

    public static final class Hook extends DynmapCommonAPIListener {
        private final Field server=Class.forName("com.sinthoras.hydroenergy.server.HEServer").getField("instance");
        private DynmapCommonAPI api;
        private int ticks;
        private boolean warned;
        Hook() throws ReflectiveOperationException {}
        @Override public void apiEnabled(DynmapCommonAPI api){this.api=api;}
        @Override public void apiDisabled(DynmapCommonAPI api){this.api=null;snapshot=Collections.emptyMap();}
        @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
            if(event.phase!=TickEvent.Phase.END||ticks++%20!=0)return;
            try {
                Object instance=server.get(null);
                if(instance==null){publish(Collections.<Integer,Dam>emptyMap(),api);return;}
                // Call through the Minecraft base type so reobfuscation remaps writeToNBT correctly.
                NBTTagCompound serialized=new NBTTagCompound();
                ((WorldSavedData)instance).writeToNBT(serialized);
                Map<Integer,Dam> next=new HashMap<>();
                for(Integer id:ids)if(serialized.hasKey("dam"+id)) {
                    NBTTagCompound tag=serialized.getCompoundTag("dam"+id);
                    WorldServer world=DimensionManager.getWorld(tag.getInteger("dimI"));
                    Dam dam=new Dam(tag,world==null?null:ForgeWorld.getWorldName(world));
                    if(Float.isNaN(dam.level)||Float.isInfinite(dam.level))continue;
                    next.put(id,dam);
                }
                publish(next,api);warned=false;
            } catch(Exception failure) {
                if(!warned){Log.severe("Cannot snapshot HydroEnergy dam levels",failure);warned=true;}
            }
        }
    }
}
