package org.dynmap.hdmap.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dynmap.renderer.CustomColorMultiplier;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.CustomRendererData;
import org.dynmap.renderer.CustomTextureMapper;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Insulated cable meshes and end layers, opt-in through cable support records. */
public final class GregTechCableRenderer {
    private static final int[] DIAMETERS = { 125, 250, 375, 500, 600, 625, 750, 875, 1000 };
    // CustomRenderer.addBox's array order is bottom, top, west, east, north, south.
    private static final int[] BOX_FACES = { 0, 1, 4, 5, 2, 3 };
    private final Map<Integer, RenderPatch[][]> meshes = new HashMap<Integer, RenderPatch[][]>();

    /** Build immutable mesh tables during the parent renderer's initialization. */
    public GregTechCableRenderer(RenderPatchFactory factory) {
        for (int diameter : DIAMETERS) {
            RenderPatch[][] table = new RenderPatch[64][];
            for (int mask = 0; mask < table.length; mask++) {
                table[mask] = buildMesh(factory, diameter, mask);
            }
            meshes.put(diameter, table);
        }
    }

    public CustomRendererData getRenderData(int diameter, int connections, int insulationTexture,
        int wireTexture, int endTexture, int insulationColor, int wireColor) {
        RenderPatch[][] table = meshes.get(diameter);
        if (table == null) {
            throw new IllegalArgumentException("Unsupported cable diameter: " + diameter);
        }
        int mask = connections & 63;
        return new CableData(table[mask], endFaces(mask), insulationTexture, wireTexture, endTexture,
            insulationColor, wireColor);
    }

    private static int endFaces(int connections) {
        // BaseMetaPipeEntity.getTextureUncovered treats isolated cables as open on
        // all sides, and a single connection as a straight cable with two ends.
        if (connections == 0) return 63;
        if (connections == 1 || connections == 2) return 3;
        if (connections == 4 || connections == 8) return 12;
        if (connections == 16 || connections == 32) return 48;
        return connections;
    }

    private static RenderPatch[] buildMesh(RenderPatchFactory factory, int diameter, int mask) {
        double min = (1 - diameter / 1000.0) / 2;
        double max = 1 - min;
        List<RenderPatch> patches = new ArrayList<RenderPatch>();
        int[] centerFaces = BOX_FACES.clone();
        if (diameter < 1000) {
            for (int i = 0; i < centerFaces.length; i++) {
                if ((mask & (1 << centerFaces[i])) != 0) centerFaces[i] = -1;
            }
        }
        CustomRenderer.addBox(factory, patches, min, max, min, max, min, max, centerFaces);
        if (diameter < 1000) {
            for (int direction = 0; direction < 6; direction++) {
                if ((mask & (1 << direction)) == 0) continue;
                int[] faces = BOX_FACES.clone();
                for (int i = 0; i < faces.length; i++) {
                    if (faces[i] == (direction ^ 1)) faces[i] = -1;
                }
                double xmin = min, xmax = max, ymin = min, ymax = max, zmin = min, zmax = max;
                switch (direction) {
                    case 0: ymin = 0; ymax = min; break;
                    case 1: ymin = max; ymax = 1; break;
                    case 2: zmin = 0; zmax = min; break;
                    case 3: zmin = max; zmax = 1; break;
                    case 4: xmin = 0; xmax = min; break;
                    case 5: xmin = max; xmax = 1; break;
                    default: throw new AssertionError(direction);
                }
                CustomRenderer.addBox(factory, patches, xmin, xmax, ymin, ymax, zmin, zmax, faces);
            }
        }
        return patches.toArray(new RenderPatch[patches.size()]);
    }

    private static final class CableData extends CustomRendererData implements CustomTextureMapper {
        private final int ends;
        private final int[] sideTextures;
        private final int[] endTextures;
        private final CustomColorMultiplier insulationColor;
        private final CustomColorMultiplier wireColor;

        CableData(RenderPatch[] mesh, int ends, int insulationTexture, int wireTexture, int endTexture,
            int insulationColor, int wireColor) {
            super(mesh, null, null);
            this.ends = ends;
            sideTextures = new int[] { insulationTexture };
            endTextures = new int[] { wireTexture, endTexture };
            this.insulationColor = new ConstantColor(insulationColor);
            this.wireColor = new ConstantColor(wireColor);
        }

        @Override
        public CustomTextureMapper getCustomTextureMapper() {
            return this;
        }

        @Override
        public int[] getTextureLayersForPatchId(int patchId) {
            if (patchId < 0 || patchId >= 6) return null;
            return (ends & (1 << patchId)) != 0 ? endTextures : sideTextures;
        }

        @Override
        public CustomColorMultiplier getCustomColorMultiplier(int patchId, int layer) {
            if (patchId < 0 || patchId >= 6 || layer < 0) return null;
            boolean end = (ends & (1 << patchId)) != 0;
            if (layer >= (end ? 2 : 1)) return null;
            return end && layer == 0 ? wireColor : insulationColor;
        }
    }

    private static final class ConstantColor extends CustomColorMultiplier {
        private final int color;

        ConstantColor(int color) {
            this.color = color;
        }

        @Override
        public int getColorMultiplier(MapDataContext context) {
            return color;
        }
    }
}
