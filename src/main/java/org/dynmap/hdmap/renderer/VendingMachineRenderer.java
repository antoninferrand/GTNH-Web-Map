package org.dynmap.hdmap.renderer;

import java.util.ArrayList;
import java.util.Map;

import net.minecraftforge.common.util.ForgeDirection;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.modsupport.GWM_Util;
import org.dynmap.renderer.CustomColorMultiplier;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.CustomRendererData;
import org.dynmap.renderer.CustomTextureMapper;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Saved-state equivalent of VendingMachine's casing CTM and client RenderOverlay. */
public class VendingMachineRenderer extends CustomRenderer {
    public static final int CONTROLLER = 2741;
    public static final int UPLINK = 2742;
    private static final String TEXTURE_MAP = "vendingmachine-casing";
    // U/right and V/up follow CustomRenderer's outward-facing cube patches.
    private static final int[] RIGHT = { 5, 5, 4, 5, 3, 2 };
    private static final int[] UP = { 3, 2, 1, 1, 1, 1 };
    private static final int[] DISPLAY_A = { -1, 0, -1, -1, 0 };
    private static final int[] DISPLAY_B = { -1, -1, 0, 1, 1 };
    private static final String[] FIELDS = { "mID", "mFacing", "mActive" };
    private RenderPatch[] mesh;
    private int casingBlock, machineBlock;

    @Override
    public boolean initializeRenderer(RenderPatchFactory factory, int block, int mask, Map<String, String> parameters) {
        casingBlock = block;
        String machines = parameters.get("machineblock");
        machineBlock = machines == null ? GWM_Util.blockNameToId("gregtech:gt.blockmachines") : Integer.parseInt(machines);
        ArrayList<RenderPatch> patches = new ArrayList<>();
        for (int face = 0; face < 6; face++) {
            for (int row = 0; row < 2; row++) {
                for (int column = 0; column < 2; column++) {
                    patches.add(quarter(factory, face, column * 0.5, (column + 1) * 0.5,
                            row * 0.5, (row + 1) * 0.5, face * 4 + row * 2 + column));
                }
            }
        }
        mesh = patches.toArray(new RenderPatch[0]);
        return super.initializeRenderer(factory, block, mask, parameters);
    }

    private static RenderPatch quarter(RenderPatchFactory factory, int face, double u0, double u1, double v0, double v1, int texture) {
        RenderPatchFactory.SideVisible visible = RenderPatchFactory.SideVisible.TOP;
        switch (face) {
            case 0: return factory.getPatch(0, 0, 0, 1, 0, 0, 0, 0, 1, u0, u1, v0, v1, visible, texture);
            case 1: return factory.getPatch(0, 1, 1, 1, 1, 1, 0, 1, 0, u0, u1, v0, v1, visible, texture);
            case 2: return factory.getPatch(1, 0, 0, 0, 0, 0, 1, 1, 0, u0, u1, v0, v1, visible, texture);
            case 3: return factory.getPatch(0, 0, 1, 1, 0, 1, 0, 1, 1, u0, u1, v0, v1, visible, texture);
            case 4: return factory.getPatch(0, 0, 0, 0, 0, 1, 0, 1, 0, u0, u1, v0, v1, visible, texture);
            default: return factory.getPatch(1, 0, 1, 1, 0, 0, 1, 1, 1, u0, u1, v0, v1, visible, texture);
        }
    }

    @Override
    public RenderPatch[] getRenderPatchList(MapDataContext context) {
        return mesh;
    }

    @Override
    public CustomRendererData getRenderData(MapDataContext context) {
        return getMachineRenderData(context, null, null);
    }

    /** Also decorates an ME uplink placed in any of the five display positions. */
    public CustomRendererData getMachineRenderData(MapDataContext context, CustomTextureMapper machineTextures, CustomColorMultiplier color) {
        return new CustomRendererData(mesh, color, new Textures(context, machineTextures));
    }

    @Override
    protected int getMaximumTextureCount() {
        return 24;
    }

    @Override
    public String[] getTileEntityFieldsNeeded() {
        return FIELDS;
    }

    private final class Textures implements CustomTextureMapper {
        private final int[][] layers = new int[24][];

        Textures(MapDataContext context, CustomTextureMapper machineTextures) {
            int ownId = number(context.getBlockTileEntityField("mID"), -1);
            int ownFacing = number(context.getBlockTileEntityField("mFacing"), -1);
            for (int face = 0; face < 6; face++) {
                int overlay = findDisplayOverlay(context, face);
                int[] original = machineTextures == null ? null : machineTextures.getTextureLayersForPatchId(face);
                int neighbors = neighborMask(context, face);
                for (int quarter = 0; quarter < 4; quarter++) {
                    ArrayList<Integer> result = new ArrayList<>();
                    if (ownId == CONTROLLER && face == ownFacing && original != null) {
                        for (int texture : original) if (texture >= 0) result.add(texture);
                    } else {
                        int compact = compactTile(neighbors, quarter);
                        int casing = TexturePack.getTextureIdFromTextureMap(TEXTURE_MAP, compact + 1);
                        if (casing < 0 && original != null && original.length > 0) casing = original[0];
                        if (casing < 0) casing = TexturePack.HDTextureMap.getMap(casingBlock, 0, 0).getIndexForFace(0);
                        result.add(casing);
                        // The first machine layer is its casing; keep its uplink overlay/glow above CTM.
                        if (original != null) for (int layer = 1; layer < original.length; layer++) if (original[layer] >= 0) result.add(original[layer]);
                    }
                    if (overlay >= 0) result.add(overlay);
                    int[] faceLayers = new int[result.size()];
                    for (int layer = 0; layer < faceLayers.length; layer++) faceLayers[layer] = result.get(layer);
                    layers[face * 4 + quarter] = faceLayers;
                }
            }
        }

        @Override
        public int[] getTextureLayersForPatchId(int patch) {
            return patch >= 0 && patch < layers.length ? layers[patch] : null;
        }
    }

    private int findDisplayOverlay(MapDataContext context, int face) {
        if (face < 2) return -1; // VendingMachine forbids vertical controller facing.
        ForgeDirection right = ForgeDirection.getOrientation(RIGHT[face]);
        for (int part = 0; part < DISPLAY_A.length; part++) {
            int x = -DISPLAY_A[part] * right.offsetX;
            int y = DISPLAY_B[part]; // StructureLib's B increases downwards.
            int z = -DISPLAY_A[part] * right.offsetZ;
            if (context.getBlockTypeIDAt(x, y, z) != machineBlock) continue;
            if (number(field(context, "mID", x, y, z), -1) != CONTROLLER) continue;
            if (number(field(context, "mFacing", x, y, z), -1) != face) continue;
            // The client clears its RenderOverlay whenever the structure is inactive.
            if (number(field(context, "mActive", x, y, z), 0) == 0) continue;
            return TexturePack.getTextureIdFromTextureMap(TEXTURE_MAP, 20 + part);
        }
        return -1;
    }

    private int neighborMask(MapDataContext context, int face) {
        ForgeDirection right = ForgeDirection.getOrientation(RIGHT[face]);
        ForgeDirection up = ForgeDirection.getOrientation(UP[face]);
        int[] horizontal = { -1, -1, 0, 1, 1, 1, 0, -1 };
        int[] vertical = { 0, -1, -1, -1, 0, 1, 1, 1 };
        int mask = 0;
        for (int neighbor = 0; neighbor < 8; neighbor++) {
            if (isCasingTexture(context, face,
                    horizontal[neighbor] * right.offsetX + vertical[neighbor] * up.offsetX,
                    horizontal[neighbor] * right.offsetY + vertical[neighbor] * up.offsetY,
                    horizontal[neighbor] * right.offsetZ + vertical[neighbor] * up.offsetZ)) mask |= 1 << neighbor;
        }
        return mask;
    }

    static int compactTile(int neighbors, int quarter) {
        boolean horizontal = (neighbors & (1 << ((quarter & 1) == 0 ? 0 : 4))) != 0;
        boolean vertical = (neighbors & (1 << ((quarter & 2) == 0 ? 2 : 6))) != 0;
        int[] diagonal = { 1, 3, 7, 5 };
        boolean corner = (neighbors & (1 << diagonal[quarter])) != 0;
        // compact_expanded tile 41 uses center pixels for both left quadrants,
        // including its absent lower-left diagonal. Match that client expansion.
        if (quarter == 0 && (neighbors & 0xD7) == 0xC5) return 1;
        return compactTile(horizontal, vertical, corner);
    }

    /** compact_expanded: 0 isolated, 1 center, 2 vertical, 3 horizontal, 4 inner corner. */
    static int compactTile(boolean horizontal, boolean vertical, boolean diagonal) {
        if (horizontal && vertical) return diagonal ? 1 : 4;
        if (horizontal) return 3;
        return vertical ? 2 : 0;
    }

    private boolean isCasingTexture(MapDataContext context, int face, int x, int y, int z) {
        int block = context.getBlockTypeIDAt(x, y, z);
        if (casingBlock > 0 && block == casingBlock) return context.getBlockDataAt(x, y, z) == 0;
        if (machineBlock <= 0 || block != machineBlock) return false;
        int type = number(field(context, "mID", x, y, z), -1);
        // BlockMachines.getIcon(IBlockAccess, ...) exposes ICasingTextureProvider's
        // casing icon for connection tests even on the controller's front face.
        return type == UPLINK || type == CONTROLLER;
    }

    private static Object field(MapDataContext context, String name, int x, int y, int z) {
        return x == 0 && y == 0 && z == 0 ? context.getBlockTileEntityField(name) : context.getBlockTileEntityFieldAt(name, x, y, z);
    }

    private static int number(Object value, int fallback) {
        if (value instanceof Boolean) return (Boolean) value ? 1 : 0;
        return value instanceof Number ? ((Number) value).intValue() : GWM_Util.objectToInt(value, fallback);
    }
}
