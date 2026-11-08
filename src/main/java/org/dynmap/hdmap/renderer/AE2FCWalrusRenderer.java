package org.dynmap.hdmap.renderer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.dynmap.Log;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Exact source OBJ, saved scale and metadata transforms, including geometry outside the owner's cell. */
public class AE2FCWalrusRenderer extends CustomRenderer {
    private final List<AtlasMesh.Triangle> triangles=new ArrayList<>();
    private final Map<Integer,RenderPatch[]> defaults=new ConcurrentHashMap<>();
    private static final RenderPatch[] EMPTY=new RenderPatch[0];
    @Override public boolean initializeRenderer(RenderPatchFactory factory,int id,int mask,Map<String,String> parameters) {
        try(InputStream stream=getClass().getClassLoader().getResourceAsStream("models/baked/ae2fc-walrus.json")) {
            JSONObject model=(JSONObject)new JSONParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8));
            if(!Boolean.TRUE.equals(model.get("extendedCoordinates")))throw new IllegalArgumentException("Walrus requires explicit model coordinates");
            for(Object part:(JSONArray)model.get("parts"))for(Object raw:(JSONArray)((JSONObject)part).get("triangles")) {
                JSONArray list=(JSONArray)raw;double[] values=new double[15];for(int i=0;i<15;i++)values[i]=((Number)list.get(i)).doubleValue();
                triangles.addAll(AtlasMesh.split(values,5,2));
            }
            return true;
        }catch(Exception failure){Log.severe("Cannot load AE2FC walrus OBJ",failure);return false;}
    }
    @Override public int getMaximumTextureCount(){return 10;}
    @Override public String[] getTileEntityFieldsNeeded(){return new String[]{"scale"};}
    @Override public RenderPatch[] getRenderPatchList(MapDataContext context) {
        int facing=context.getBlockData();
        // Only these four metadata states are placed by BlockWalrus.onBlockPlacedBy.
        if(facing<2||facing>5)return EMPTY;
        Object saved=context.getBlockTileEntityField("scale");float scale=saved instanceof Number?((Number)saved).floatValue():1;
        if(Float.isNaN(scale)||Float.isInfinite(scale)){Log.warning("Invalid non-finite AE2FC walrus scale");return EMPTY;}
        if(scale==1){RenderPatch[] cached=defaults.get(facing);if(cached!=null)return cached;}
        double x=facing==4?1-scale*.655:facing==5?scale*.655:.5;
        double z=facing==2?1-scale*.655:facing==3?scale*.655:.5;
        int angle=facing==3?180:facing==4?90:facing==5?-90:0;
        double cos=Math.cos(Math.toRadians(angle)),sin=Math.sin(Math.toRadians(angle));
        List<RenderPatch> result=new ArrayList<>();RenderPatchFactory factory=context.getPatchFactory();
        for(AtlasMesh.Triangle triangle:triangles) {
            double[] p=triangle.vertices.clone();
            for(int i=0;i<15;i+=5){double sx=p[i]*scale,sz=p[i+2]*scale;p[i]=x+sx*cos+sz*sin;p[i+1]*=scale;p[i+2]=z-sx*sin+sz*cos;}
            RenderPatch patch=factory.getModelTriangleExplTexCoords(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7],p[8],p[9],p[10],p[11],p[12],p[13],p[14],RenderPatchFactory.SideVisible.TOP,triangle.texture);
            if(patch!=null)result.add(patch);
        }
        RenderPatch[] patches=result.toArray(new RenderPatch[result.size()]);
        if(scale==1){RenderPatch[] previous=defaults.putIfAbsent(facing,patches);if(previous!=null)return previous;}
        return patches;
    }
}
