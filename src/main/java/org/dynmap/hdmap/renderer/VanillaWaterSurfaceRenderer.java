package org.dynmap.hdmap.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Minecraft RenderBlocks liquid geometry, sampled entirely from the map snapshot. */
public class VanillaWaterSurfaceRenderer extends CustomRenderer {
    protected final byte[] materials = new byte[65536]; // 0 nonsolid,1 solid,2 water
    protected final boolean[] opaque = new boolean[65536], movement = new boolean[65536];
    private static final double EPSILON = 0.0010000000474974513D;

    @Override
    public boolean initializeRenderer(RenderPatchFactory factory, int id, int mask, Map<String,String> parameters) {
        initializeBlockKinds();
        return super.initializeRenderer(factory,id,mask,parameters);
    }

    protected void initializeBlockKinds() {
        for (Object value : net.minecraft.block.Block.blockRegistry) {
            net.minecraft.block.Block block = (net.minecraft.block.Block)value;
            int id = net.minecraft.block.Block.getIdFromBlock(block);
            net.minecraft.block.material.Material material = block.getMaterial();
            materials[id] = (byte)(material == net.minecraft.block.material.Material.water ? 2 : material.isSolid() ? 1 : 0);
            opaque[id] = block.isOpaqueCube(); movement[id] = material.blocksMovement();
        }
    }

    @Override
    public int getMaximumTextureCount() { return 2; }

    private int id(MapDataContext context,int x,int y,int z) { return context.getBlockTypeIDAt(x,y,z); }
    protected boolean water(MapDataContext context,int x,int y,int z) { return materials[id(context,x,y,z)] == 2; }
    private int decay(MapDataContext context,int x,int y,int z) {
        if(!water(context,x,y,z))return -1;
        int meta=context.getBlockDataAt(x,y,z);return meta>=8?0:meta;
    }

    /** Exact float accumulation and weighting from RenderBlocks.getLiquidHeight. */
    protected float height(MapDataContext context,int x,int y,int z) {
        int count=0;float sum=0;
        for(int corner=0;corner<4;corner++) {
            int sx=x-(corner&1),sz=z-(corner>>1&1);
            if(water(context,sx,y+1,sz))return 1;
            int kind=materials[id(context,sx,y,sz)];
            if(kind==2) {
                int meta=context.getBlockDataAt(sx,y,sz);
                float percent=((meta>=8?0:meta)+1)/9.0F;
                if(meta>=8||meta==0){sum+=percent*10;count+=10;}
                sum+=percent;count++;
            } else if(kind!=1){sum++;count++;}
        }
        return count==0?0:1-sum/count;
    }

    private double flow(MapDataContext context) {
        int center=decay(context,0,0,0);double x=0,z=0;
        for(int[] offset:new int[][]{{-1,0},{0,-1},{1,0},{0,1}}) {
            int next=decay(context,offset[0],0,offset[1]);
            if(next<0) {
                if(!movement[id(context,offset[0],0,offset[1])]) {
                    next=decay(context,offset[0],-1,offset[1]);
                    if(next>=0){x+=offset[0]*(next-(center-8));z+=offset[1]*(next-(center-8));}
                }
            } else {x+=offset[0]*(next-center);z+=offset[1]*(next-center);}
        }
        return x==0&&z==0?-1000:Math.atan2(z,x)-Math.PI/2;
    }

    @Override
    public RenderPatch[] getRenderPatchList(MapDataContext context) {
        RenderPatchFactory factory=context.getPatchFactory();List<RenderPatch> patches=new ArrayList<>();
        double[] h={height(context,0,0,0),height(context,0,0,1),height(context,1,0,1),height(context,1,0,0)};
        if(!water(context,0,1,0)) {
            double angle=(float)flow(context);double[][] uv={{0,1},{0,0},{1,0},{1,1}};int texture=0;
            if(angle>-999) {
                float sin=net.minecraft.util.MathHelper.sin((float)angle)*.25F,cos=net.minecraft.util.MathHelper.cos((float)angle)*.25F;
                uv=new double[][]{{.5-cos-sin,1-(.5-cos+sin)},{.5-cos+sin,1-(.5+cos+sin)},
                        {.5+cos+sin,1-(.5+cos-sin)},{.5+cos-sin,1-(.5-cos-sin)}};texture=1;
            }
            for(int i=0;i<4;i++)h[i]-=EPSILON;
            quad(factory,patches,new double[][]{{0,h[0],0,uv[0][0],uv[0][1]},{0,h[1],1,uv[1][0],uv[1][1]},
                    {1,h[2],1,uv[2][0],uv[2][1]},{1,h[3],0,uv[3][0],uv[3][1]}},texture,true);
        }
        if(!water(context,0,-1,0)&&!opaque[id(context,0,-1,0)])
            quad(factory,patches,new double[][]{{0,EPSILON,1,0,0},{0,EPSILON,0,0,1},{1,EPSILON,0,1,1},{1,EPSILON,1,1,0}},0,false);
        int[][] neighbors={{0,-1},{0,1},{-1,0},{1,0}};
        double[][] edges={{0,EPSILON,1,EPSILON,0,3},{1,1-EPSILON,0,1-EPSILON,2,1},
                {EPSILON,1,EPSILON,0,1,0},{1-EPSILON,0,1-EPSILON,1,3,2}};
        for(int side=0;side<4;side++) {
            int[] neighbor=neighbors[side];int neighborId=id(context,neighbor[0],0,neighbor[1]);
            if(materials[neighborId]==2||opaque[neighborId])continue;
            double[] edge=edges[side];double a=h[(int)edge[4]],b=h[(int)edge[5]];
            quad(factory,patches,new double[][]{{edge[0],a,edge[1],0,1-(1-a)*.5},{edge[2],b,edge[3],.5,1-(1-b)*.5},
                    {edge[2],0,edge[3],.5,.5},{edge[0],0,edge[1],0,.5}},1,true);
        }
        return patches.toArray(new RenderPatch[patches.size()]);
    }

    protected static void quad(RenderPatchFactory factory,List<RenderPatch> patches,double[][] q,int texture,boolean both) {
        for(int[] order:new int[][]{{0,1,2},{0,2,3}}) {
            double[] a=q[order[0]],b=q[order[1]],c=q[order[2]];
            RenderPatch patch=factory.getTriangleExplTexCoords(a[0],a[1],a[2],a[3],a[4],b[0],b[1],b[2],b[3],b[4],
                    c[0],c[1],c[2],c[3],c[4],both?RenderPatchFactory.SideVisible.BOTH:RenderPatchFactory.SideVisible.TOP,texture);
            if(patch!=null)patches.add(patch);
        }
    }
}
