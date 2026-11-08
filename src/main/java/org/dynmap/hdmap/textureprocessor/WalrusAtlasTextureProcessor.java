package org.dynmap.hdmap.textureprocessor;
import org.dynmap.hdmap.TexturePack;
/** Pads the source70x32 atlas on the right to80x32; every original pixel is retained. */
public class WalrusAtlasTextureProcessor extends CustomTextureProcessor {
    @Override public int getTextureCount(){return 10;}
    @Override public void patchTextures(TexturePack pack,int[] tiles,int[] pixels,int width,int height,int scale) {
        if(width%70!=0||height!=width/70*32)throw new IllegalArgumentException("Unexpected AE2FC walrus atlas dimensions");
        int cell=16*(width/70);
        for(int row=0;row<2;row++)for(int col=0;col<5;col++) {
            int[] source=new int[cell*cell],target=new int[scale*scale];
            int copied=Math.max(0,Math.min(cell,width-col*cell));
            for(int y=0;y<cell;y++)System.arraycopy(pixels,(row*cell+y)*width+col*cell,source,y*cell,copied);
            TexturePack.scaleTerrainPNGSubImage(cell,scale,source,target);pack.setTileARGB(tiles[row*5+col],target);
        }
    }
}
