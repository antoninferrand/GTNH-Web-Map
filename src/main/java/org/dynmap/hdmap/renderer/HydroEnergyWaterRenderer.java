package org.dynmap.hdmap.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dynmap.Log;
import org.dynmap.modsupport.HydroEnergySnapshots;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Flat dam water surface with a height from the main-thread HydroEnergy snapshot. */
public class HydroEnergyWaterRenderer extends VanillaWaterSurfaceRenderer {
    private int waterId;
    private static final RenderPatch[] EMPTY=new RenderPatch[0];
    private static final double OFFSET=.0010000000474974513;
    @Override
    public boolean initializeRenderer(RenderPatchFactory factory,int id,int mask,Map<String,String> parameters) {
        if(!super.initializeRenderer(factory,id,mask,parameters))return false;
        try {
            waterId=Integer.parseInt(parameters.get("waterid"));
            HydroEnergySnapshots.register(waterId);return true;
        } catch(Exception failure){Log.severe("Cannot initialize HydroEnergy water snapshot",failure);return false;}
    }
    protected Float waterLevel(MapDataContext context) {
        HydroEnergySnapshots.Dam dam=HydroEnergySnapshots.get(waterId,context.getWorldName());
        return dam==null?null:dam.level;
    }
    @Override
    public RenderPatch[] getRenderPatchList(MapDataContext context) {
        Float level=waterLevel(context);if(level==null)return EMPTY;
        double height=Math.max(0,Math.min(1,level-context.getY()));
        if(height<=0)return EMPTY;
        RenderPatchFactory factory=context.getPatchFactory();List<RenderPatch> patches=new ArrayList<>();
        int current=context.getBlockTypeID();
        if(level<=context.getY()+1) {
            height-=OFFSET;
            quad(factory,patches,new double[][]{{0,height,0,0,1},{0,height,1,0,0},{1,height,1,1,0},{1,height,0,1,1}},0,true);
        }
        int below=context.getBlockTypeIDAt(0,-1,0);
        if(below!=current&&!opaque[below])quad(factory,patches,new double[][]{{0,OFFSET,1,0,0},{0,OFFSET,0,0,1},{1,OFFSET,0,1,1},{1,OFFSET,1,1,0}},0,false);
        int[][] offsets={{0,-1},{0,1},{-1,0},{1,0}};
        double[][] edges={{0,OFFSET,1,OFFSET},{1,1-OFFSET,0,1-OFFSET},{OFFSET,1,OFFSET,0},{1-OFFSET,0,1-OFFSET,1}};
        for(int side=0;side<4;side++) {
            int neighbor=context.getBlockTypeIDAt(offsets[side][0],0,offsets[side][1]);
            if(neighbor==current||opaque[neighbor])continue;
            double[] e=edges[side];
            quad(factory,patches,new double[][]{{e[0],height,e[1],0,1-(1-height)*.5},{e[2],height,e[3],.5,1-(1-height)*.5},
                    {e[2],0,e[3],.5,.5},{e[0],0,e[1],0,.5}},1,true);
        }
        return patches.toArray(new RenderPatch[patches.size()]);
    }
}
