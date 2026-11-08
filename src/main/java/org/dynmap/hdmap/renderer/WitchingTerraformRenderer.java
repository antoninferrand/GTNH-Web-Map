package org.dynmap.hdmap.renderer;

import java.lang.reflect.Method;
import java.util.Map;
import org.dynmap.Log;
import org.dynmap.renderer.CustomColorMultiplier;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatchFactory;

/** Source OBJ groups with the same required-aspect tint as WitchingGadgets' TESRs. */
public class WitchingTerraformRenderer extends BakedMeshRenderer {
    private int blockId;
    private int[] aspectColors;

    @Override
    public boolean initializeRenderer(RenderPatchFactory factory, int id, int mask, Map<String, String> parameters) {
        if (!super.initializeRenderer(factory, id, mask, parameters)) return false;
        blockId = id;
        try {
            aspectColors = sourceAspectColors(id);
            return true;
        } catch (ReflectiveOperationException failure) {
            Log.severe("Cannot read WitchingGadgets required-aspect colors", failure);
            return false;
        }
    }

    /** Evaluate the installed block's pure metadata API once, including optional-mod aspect choices. */
    protected int[] sourceAspectColors(int id) throws ReflectiveOperationException {
        Object block = net.minecraft.block.Block.getBlockById(id);
        Method required = block.getClass().getMethod("requiredAspect", int.class);
        int[] colors = new int[16];
        for (int meta = 0; meta < colors.length; meta++) {
            Object aspect = required.invoke(block, meta);
            colors[meta] = aspect == null ? 0xffffff : ((Number) aspect.getClass().getMethod("getColor").invoke(aspect)).intValue();
        }
        return colors;
    }

    @Override
    protected CustomColorMultiplier getPartColor(String name, MapDataContext context) {
        int meta;
        if (name.equals("focus_crystal_04")) {
            meta = context.getBlockData();
        } else if (name.equals("crystals_02") && context.getBlockTypeIDAt(0, 1, 0) == blockId) {
            meta = context.getBlockDataAt(0, 1, 0);
        } else return null;
        final int color = meta >= 0 && meta < aspectColors.length ? aspectColors[meta] : 0xffffff;
        return new CustomColorMultiplier() {
            @Override
            public int getColorMultiplier(MapDataContext ignored) {
                return color;
            }
        };
    }
}
