package org.dynmap.hdmap.textureprocessor;

import java.util.HashMap;
import org.dynmap.hdmap.TexturePack;

/** The client-side TextureComprBlock effect, derived from the installed ExtraUtilities 1.2.12 bytecode. */
public class ExtraUtilitiesCompressedTextureProcessor extends CustomTextureProcessor {
    private int level;

    @Override
    public boolean init(HashMap<String, String> data) {
        level = Integer.parseInt(data.get("compression"));
        if (level < 1 || level > 8) {
            throw new IllegalArgumentException("Compression level must be between 1 and 8");
        }
        return true;
    }

    @Override
    public int getTextureCount() {
        return 1;
    }

    @Override
    public void patchTextures(TexturePack texturePack, int[] tiles, int[] argb, int width, int height, int scale) {
        // TextureAtlasSprite supplies square animation frames to the original effect.
        // Web Map uses the first such frame for its static map texture.
        int[] pixels = new int[width * width];
        float amount = level / 8.5F;
        float contrast = 1.0F - amount;
        float border = 1.0F - (2 + level) / 32.0F;
        for (int i = 0; i < pixels.length; i++) {
            int inverted = -argb[i];
            float red = ((inverted >> 16) & 255) / 255.0F;
            float green = ((inverted >> 8) & 255) / 255.0F;
            float blue = (inverted & 255) / 255.0F;
            float x = 2.0F * (i % width) / (width - 1) - 1.0F;
            float y = 2.0F * (i / width) / (width - 1) - 1.0F;
            float edge = Math.max(Math.abs(x), Math.abs(y));
            edge = Math.max(edge, (float) Math.sqrt(x * x + y * y) / 1.4F);
            float strength = 1.0F - edge + 1.0F - amount;
            float rim = 1.0F;
            if (edge > border) {
                rim = 0.7F + 0.1F * (edge - border) / (1.0F - border);
            }
            strength *= rim * rim;
            strength = Math.max(0.0F, Math.min(1.0F, strength));
            red = 1.0F - (1.0F - red) * contrast * strength;
            green = 1.0F - (1.0F - green) * contrast * strength;
            blue = 1.0F - (1.0F - blue) * contrast * strength;
            // The original effect deliberately negates packed RGB rather than inverting bits.
            pixels[i] = -(((int) (red * 255.0F) << 16) | ((int) (green * 255.0F) << 8)
                    | (int) (blue * 255.0F));
        }
        int[] scaled = new int[scale * scale];
        TexturePack.scaleTerrainPNGSubImage(width, scale, pixels, scaled);
        texturePack.setTileARGB(tiles[0], scaled);
    }
}
