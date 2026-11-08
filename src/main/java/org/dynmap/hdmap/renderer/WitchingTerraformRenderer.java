package org.dynmap.hdmap.renderer;

import java.lang.reflect.Field;
import cpw.mods.fml.common.Loader;
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
        } catch (ReflectiveOperationException | LinkageError failure) {
            Log.severe("Cannot read WitchingGadgets required-aspect colors", failure);
            return false;
        }
    }

    /** Read common-side Aspect fields; never reflect on a client-bearing Block hierarchy. */
    protected int[] sourceAspectColors(int id) throws ReflectiveOperationException {
        Class<?> aspectType = Class.forName("thaumcraft.api.aspects.Aspect");
        Field color = aspectType.getDeclaredField("color");
        color.setAccessible(true);
        String[] aspects = { "PLANT", "COLD", "FIRE", "TREE", "FIRE", "TAINT", "SLIME", "WATER", "WATER", "ELDRITCH", "HEAL" };
        int[] colors = new int[16];
        java.util.Arrays.fill(colors, 0xffffff);
        for (int meta = 3; meta <= 13; meta++) {
            Object aspect;
            if (meta == 7 && Loader.isModLoaded("ForbiddenMagic")) {
                aspect = Class.forName("fox.spiteful.forbidden.DarkAspects").getField("NETHER").get(null);
            } else {
                aspect = aspectType.getField(aspects[meta - 3]).get(null);
            }
            colors[meta] = color.getInt(aspect);
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
