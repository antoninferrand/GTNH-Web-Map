package org.dynmap.hdmap.renderer;

import java.io.IOException;
import java.util.Map;
import gregtech.api.GregTechAPI;
import org.dynmap.Log;
import org.dynmap.modsupport.GWM_Util;
import org.dynmap.modsupport.ModelAtlasSupport;
import org.dynmap.modsupport.gregtech.GregTechSupport;
import org.dynmap.renderer.CustomRendererData;
import org.dynmap.renderer.CustomTextureMapper;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

/** Adds source OBJ appearances while retaining the shared machine/state renderer. */
public class GregTechModelRenderer extends GregTechMachineRenderer {
    private RenderPatch[][] iceCream;

    @Override
    public boolean initializeRenderer(RenderPatchFactory factory, int block, int mask, Map<String, String> parameters) {
        if (!super.initializeRenderer(factory, block, mask, parameters)) return false;
        try {
            WavefrontModel model = WavefrontModel.read(GregTechAPI.class.getResourceAsStream(
                    "/assets/gregtech/textures/model/ice_cream_machine.obj"));
            iceCream = new RenderPatch[4][];
            for (int turn = 0; turn < 4; turn++) {
                // MTEIceCreamMachine.renderTESR translates before rotating.
                iceCream[turn] = model.patches(factory, turn * 90, 0.5, 1.0, 0.5, 0, 4, 4);
            }
        } catch (IOException invalidModel) {
            // Keep unrelated machines usable; expose a real model-loading error.
            Log.severe("Unable to load GregTech ice cream machine OBJ: " + invalidModel.getMessage());
        }
        return true;
    }

    @Override
    public CustomRendererData getRenderData(MapDataContext context) {
        int id = GWM_Util.objectToInt(context.getBlockTileEntityField("mID"), -1);
        if (id == 20000 && iceCream != null) {
            GregTechSupport.MetaTileEntityEntry entry = GregTechSupport.INSTANCE.getMTEEntre(id);
            CustomTextureMapper atlas = ModelAtlasSupport.getMapper("gregtech:icecream");
            if (entry != null && atlas != null) {
                int facing = GWM_Util.objectToInt(context.getBlockTileEntityField("mMainFacing"), 6);
                // Source: NORTH=90, EAST=0, SOUTH=270, otherwise=180.
                int turn = facing == 2 ? 1 : facing == 5 ? 0 : facing == 3 ? 3 : 2;
                return new CustomRendererData(iceCream[turn], null, atlas);
            }
        }
        return super.getRenderData(context);
    }
}
