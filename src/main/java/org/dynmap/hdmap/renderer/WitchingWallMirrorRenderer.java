package org.dynmap.hdmap.renderer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.dynmap.Log;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.CustomRendererData;
import org.dynmap.renderer.CustomColorMultiplier;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Source pixel frame split across its real lower and dummy upper blocks. */
public class WitchingWallMirrorRenderer extends CustomRenderer {
    private final RenderPatch[][][] frames=new RenderPatch[2][4][];
    private final RenderPatch[][][] glass=new RenderPatch[2][4][];
    private int[] colors;
    @Override public boolean initializeRenderer(RenderPatchFactory factory,int id,int mask,Map<String,String> parameters) {
        try(InputStream stream=getClass().getClassLoader().getResourceAsStream("models/baked/witchinggadgets-mirror-frame.json")) {
            JSONObject source=(JSONObject)new JSONParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8));
            JSONArray palette=(JSONArray)source.get("colors"),shape=(JSONArray)source.get("shape");
            colors=new int[palette.size()];for(int i=0;i<colors.length;i++)colors[i]=((Number)palette.get(i)).intValue();
            for(int half=0;half<2;half++) {
                List<RenderPatch> frame=new ArrayList<>();
                for(int y=0;y<16;y++)for(int z=0;z<8;z++) {
                    int color=((Number)((JSONArray)shape.get(y)).get(z)).intValue();if(color<0)continue;
                    for(int mirroredZ:new int[]{z,15-z})addBox(factory,frame,0,.0625,y/16.0,(y+1)/16.0,mirroredZ/16.0,(mirroredZ+1)/16.0,new int[]{32+color,32+color,32+color,32+color,32+color,32+color});
                }
                // Top half is the source's vertically mirrored lower half.
                if(half==1) {
                    List<RenderPatch> flipped=new ArrayList<>();
                    for(RenderPatch patch:frame)flipped.add(factory.getRotatedPatch(patch,180,0,0,patch.getTextureIndex()));
                    frame=flipped;
                }
                double low=half*.5,high=low+.5;
                double[][] q={{.0005,0,0,1,low},{.0005,1,0,1,high},{.0005,1,1,0,high},{.0005,0,1,0,low}};
                List<RenderPatch> pane=new ArrayList<>();
                for(int[] order:new int[][]{{0,1,2},{0,2,3}}) {
                    double[] triangle=new double[15];for(int i=0;i<3;i++)System.arraycopy(q[order[i]],0,triangle,i*5,5);
                    for(AtlasMesh.Triangle tile:AtlasMesh.split(triangle,4,8)) {
                        double[] v=tile.vertices;
                        RenderPatch patch=factory.getTriangleExplTexCoords(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],v[10],v[11],v[12],v[13],v[14],RenderPatchFactory.SideVisible.TOP,tile.texture);
                        if(patch!=null)pane.add(patch);
                    }
                }
                for(int facing=0;facing<4;facing++) {
                    frames[half][facing]=getRotatedSet(factory,frame.toArray(new RenderPatch[frame.size()]),0,-facing*90,0);
                    glass[half][facing]=getRotatedSet(factory,pane.toArray(new RenderPatch[pane.size()]),0,-facing*90,0);
                }
            }
            return true;
        }catch(Exception failure){Log.severe("Cannot build WitchingGadgets wall mirror",failure);return false;}
    }
    @Override public int getMaximumTextureCount(){return 32+colors.length;}
    @Override public String[] getTileEntityFieldsNeeded(){return new String[]{"isDummy","isActive","facing"};}
    private static boolean flag(Object value){return Boolean.TRUE.equals(value)||value instanceof Number&&((Number)value).intValue()!=0;}
    @Override public RenderPatch[] getRenderPatchList(MapDataContext context) {
        int half=flag(context.getBlockTileEntityField("isDummy"))?1:0;
        Object facingValue=context.getBlockTileEntityField("facing");int facing=facingValue instanceof Number?((Number)facingValue).intValue():5;
        int rotation=facing==2?1:facing==3?3:facing==4?2:0;
        Object active=half==0?context.getBlockTileEntityField("isActive"):context.getBlockTileEntityFieldAt("isActive",0,-1,0);
        RenderPatch[] frame=frames[half][rotation];if(flag(active))return frame;
        RenderPatch[] pane=glass[half][rotation],result=Arrays.copyOf(frame,frame.length+pane.length);System.arraycopy(pane,0,result,frame.length,pane.length);return result;
    }
    @Override public CustomRendererData getRenderData(MapDataContext context) {
        return new CustomRendererData(getRenderPatchList(context),null,null) {
            @Override public CustomColorMultiplier getCustomColorMultiplier(int patchId,int layer) {
                final int index=patchId-32;if(index<0||index>=colors.length)return null;
                return new CustomColorMultiplier(){@Override public int getColorMultiplier(MapDataContext ignored){return colors[index];}};
            }
        };
    }
}
