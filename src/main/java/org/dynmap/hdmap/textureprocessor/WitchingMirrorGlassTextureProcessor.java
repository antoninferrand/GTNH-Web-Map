package org.dynmap.hdmap.textureprocessor;

import org.dynmap.hdmap.TexturePack;

/** Native atlas cells from the inactive first frame used by TileRenderWallMirror. */
public class WitchingMirrorGlassTextureProcessor extends CustomTextureProcessor {
    @Override public int getTextureCount(){return 32;}
    @Override public void patchTextures(TexturePack pack,int[] tiles,int[] pixels,int width,int height,int scale) {
        int cell=width/128;
        if(width%128!=0||height!=cell*16)throw new IllegalArgumentException("Unexpected WitchingGadgets mirror atlas dimensions");
        for(int row=0;row<8;row++)for(int col=0;col<4;col++) {
            int[] source=new int[cell*cell],target=new int[scale*scale];
            for(int y=0;y<cell;y++)System.arraycopy(pixels,(row*cell+y)*width+col*cell,source,y*cell,cell);
            TexturePack.scaleTerrainPNGSubImage(cell,scale,source,target);
            pack.setTileARGB(tiles[row*4+col],target);
        }
    }
}
