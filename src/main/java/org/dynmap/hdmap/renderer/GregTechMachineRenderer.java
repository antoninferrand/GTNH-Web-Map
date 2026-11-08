package org.dynmap.hdmap.renderer;


import gregtech.api.enums.Dyes;
import net.minecraftforge.common.util.ForgeDirection;
import org.dynmap.modsupport.GWM_Util;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.modsupport.gregtech.GregTechSupport;
import org.dynmap.renderer.CustomColorMultiplier;
import org.dynmap.renderer.CustomRenderer;
import org.dynmap.renderer.CustomRendererData;
import org.dynmap.renderer.CustomTextureMapper;
import org.dynmap.renderer.MapDataContext;
import org.dynmap.renderer.RenderPatch;
import org.dynmap.renderer.RenderPatchFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;

public class GregTechMachineRenderer extends PipeRendererBase {

    private static final int fullBlockPatchList[] = { 0, 1, 4, 5, 2, 3 };
    private RenderPatch[] fullBlock;
    private GregTechCableRenderer cableRenderer;
    private volatile VendingMachineRenderer vendingRenderer;
    RenderPatch[][] pipes125;
    RenderPatch[][] pipes250;
    RenderPatch[][] pipes375;
    RenderPatch[][] pipes500;
    RenderPatch[][] pipes600;
    RenderPatch[][] pipes625;
    RenderPatch[][] pipes750;
    RenderPatch[][] pipes875;

    static int[] dyeColors = new int[16];

    @Override
    public boolean initializeRenderer(RenderPatchFactory rpf, int blkid, int blockdatamask, Map<String, String> custparm) {
        if (!super.initializeRenderer(rpf, blkid, blockdatamask, custparm))
            return false;
        cableRenderer = new GregTechCableRenderer(rpf);

        pipes125 = generateSingleSize(rpf, 0.125 /2, 0, 0, 0);
        pipes250 = generateSingleSize(rpf, 0.250 /2, 0, 0, 0);
        pipes375 = generateSingleSize(rpf, 0.375 /2, 0, 0, 0);
        pipes500 = generateSingleSize(rpf, 0.500 /2, 0, 0, 0);
        pipes600 = generateSingleSize(rpf, 0.600 /2, 0, 0, 0);
        pipes625 = generateSingleSize(rpf, 0.625 /2, 0, 0, 0);
        pipes750 = generateSingleSize(rpf, 0.750 /2, 0, 0, 0);
        pipes875 = generateSingleSize(rpf, 0.875 /2, 0, 0, 0);

        ArrayList<RenderPatch> list = new ArrayList<RenderPatch>();
        CustomRenderer.addBox(rpf, list, 0, 1, 0, 1,0, 1, fullBlockPatchList);
        fullBlock = list.toArray(new RenderPatch[fullBlockPatchList.length]);

        for(Dyes d : Dyes.VALUES){
            short[] rgba = d.getRGBA();
            dyeColors[d.ordinal()] = (rgba[0] << 16) | (rgba[1] << 8) | rgba[2];
        }

        return true;
    }

    @Override
    public CustomRendererData getRenderData(MapDataContext mapDataCtx) {

        int version = 0;
        Object typeIdObj = mapDataCtx.getBlockTileEntityField("mID");

        if(typeIdObj instanceof Number) {
            int typeId = ((Number) typeIdObj).intValue();
            GregTechSupport.MetaTileEntityEntry entry = GregTechSupport.INSTANCE.getMTEEntre(typeId);
            int mColor = GWM_Util.objectToInt(mapDataCtx.getBlockTileEntityField("mColor"),0);

            if (entry != null) {
                if (entry.type == GregTechSupport.MteType.Cable && entry.wireTexture >= 0
                        && entry.endTexture >= 0 && cableRenderer != null) {
                    int insulationColor = entry.colorMul;
                    if (entry.paintable && mColor > 0 && mColor <= 16) insulationColor = dyeColors[mColor - 1];
                    int connections = GWM_Util.objectToInt(mapDataCtx.getBlockTileEntityField("mConnections"), 0);
                    return cableRenderer.getRenderData(entry.thickness, connections, entry.baseTexture,
                            entry.wireTexture, entry.endTexture, insulationColor, entry.wireColor);
                }
                if (entry.type == GregTechSupport.MteType.Cable || entry.type == GregTechSupport.MteType.Wire || entry.type == GregTechSupport.MteType.Frame || entry.type == GregTechSupport.MteType.Pipe) {
                    Object connections = mapDataCtx.getBlockTileEntityField("mConnections");
                    RenderPatch[][] patchSetToUse = pipes375;

                    BasicMachineTextureAndColor pipeTexturesAndColorsThing = new BasicMachineTextureAndColor(typeId, mapDataCtx, false, -1, -1, mColor);
                    int thickness = entry.thickness;

                    switch (thickness) {
                        case 125:
                            patchSetToUse = pipes125;
                            break;
                        case 250:
                            patchSetToUse = pipes250;
                            break;
                        case 375:
                            patchSetToUse = pipes375;
                            break;
                        case 500:
                            patchSetToUse = pipes500;
                            break;
                        case 600:
                            patchSetToUse = pipes600;
                            break;
                        case 625:
                            patchSetToUse = pipes625;
                            break;
                        case 750:
                            patchSetToUse = pipes750;
                            break;
                        case 875:
                            patchSetToUse = pipes875;
                            break;
                        case 1000:
                            return new CustomRendererData(fullBlock, pipeTexturesAndColorsThing, pipeTexturesAndColorsThing);
                        default:
                            patchSetToUse = pipes250;
                            break;

                    }
                    if (connections instanceof Number) {
                        // Six direction bits select the mesh. Saved connection
                        // flags must not index outside the 64 connection models.
                        version = ((Number) connections).intValue() & 0x3F;
                    }

                    return new CustomRendererData(patchSetToUse[version], pipeTexturesAndColorsThing, pipeTexturesAndColorsThing);
                } else {
                    Object tmpActive = mapDataCtx.getBlockTileEntityField("mActive");
                    Object tmpFacing = mapDataCtx.getBlockTileEntityField("mFacing");
                    Object tmpMainFacing = mapDataCtx.getBlockTileEntityField("mMainFacing");
                    boolean active = tmpActive instanceof Boolean ? (Boolean) tmpActive : number(tmpActive, 0) != 0;
                    int facing = validFacing(number(tmpFacing, ForgeDirection.NORTH.ordinal()), ForgeDirection.NORTH.ordinal());
                    int mainFacing = validFacing(number(tmpMainFacing, facing), facing);

                    BasicMachineTextureAndColor bmtac = new BasicMachineTextureAndColor(typeId, mapDataCtx, active, facing, mainFacing, mColor);
                    if (entry.vending) return getVendingRenderer(mapDataCtx).getMachineRenderData(mapDataCtx, bmtac, bmtac);
                    return new CustomRendererData(fullBlock, bmtac, bmtac);
                }
            }
        }
        return new CustomRendererData(fullBlock, null, null);
    }

    private VendingMachineRenderer getVendingRenderer(MapDataContext context) {
        if (vendingRenderer == null) {
            synchronized (this) {
                if (vendingRenderer == null) {
                    VendingMachineRenderer renderer = new VendingMachineRenderer();
                    java.util.HashMap<String, String> parameters = new java.util.HashMap<>();
                    parameters.put("machineblock", String.valueOf(context.getBlockTypeID()));
                    renderer.initializeRenderer(context.getPatchFactory(), GWM_Util.blockNameToId("vendingmachine:casing"), 1, parameters);
                    vendingRenderer = renderer;
                }
            }
        }
        return vendingRenderer;
    }

    @Override
    public RenderPatch[] getRenderPatchList(MapDataContext mapDataCtx) {
        return super.getRenderPatchList(mapDataCtx);
    }
    class BasicMachineTextureAndColor extends CustomColorMultiplier implements CustomTextureMapper
    {
        int[][] texturesToUse = new int[6][1];
        int colorMul = 0xFFFFFF;
        GregTechSupport.IconSet baseSet;
        int baseTex, baseTex2;
        int[] baseFaces;
        public BasicMachineTextureAndColor(int typeId, MapDataContext mapDataCtx, boolean active, int outputFacing, int mainFacing, int mColor){
            GregTechSupport.MetaTileEntityEntry entry = GregTechSupport.INSTANCE.getMTEEntre(typeId);

            if(entry != null){
                colorMul = entry.colorMul;
                int front = mainFacing;
                baseSet = entry.baseTextureSet;
                int state = entry.stateField == null ? Integer.MIN_VALUE : number(mapDataCtx.getBlockTileEntityField(entry.stateField), Integer.MIN_VALUE);
                GregTechSupport.IconSet iconSet = entry.getIconSet(active, state);
                baseTex = entry.baseTexture;
                baseTex2 = entry.baseTexture2;
                if (entry.baseBlock > 0) {
                    baseFaces = new int[6];
                    TexturePack.HDTextureMap block = TexturePack.HDTextureMap.getMap(entry.baseBlock, entry.baseMetadata, 0);
                    for (int face = 0; face < 6; face++) baseFaces[face] = block.getIndexForFace(entry.baseFace >= 0 && entry.baseFace < 6 ? entry.baseFace : face);
                    if (outputFacing >= 0 && outputFacing < 6) {
                        if (entry.frontBaseBlock > 0 && entry.frontBaseMetadata >= 0 && entry.frontBaseMetadata < 16)
                            baseFaces[outputFacing] = TexturePack.HDTextureMap.getMap(entry.frontBaseBlock, entry.frontBaseMetadata, 0).getIndexForFace(outputFacing);
                        int back = ForgeDirection.getOrientation(outputFacing).getOpposite().ordinal();
                        if (entry.backBaseBlock > 0 && entry.backBaseMetadata >= 0 && entry.backBaseMetadata < 16)
                            baseFaces[back] = TexturePack.HDTextureMap.getMap(entry.backBaseBlock, entry.backBaseMetadata, 0).getIndexForFace(back);
                    }
                    baseSet = null;
                }
                Integer stateBase = entry.stateBaseTextures.get(state);
                if (stateBase != null) {
                    baseTex = stateBase;
                    baseSet = null;
                    baseFaces = null;
                }
                if (entry.casingStateField != null) {
                    Integer casing = entry.casingStateBaseTextures.get(number(mapDataCtx.getBlockTileEntityField(entry.casingStateField), Integer.MIN_VALUE));
                    if (casing != null) {
                        baseTex = casing;
                        baseSet = null;
                        baseFaces = null;
                    }
                }

                if (entry.isHatch && entry.inheritCasing) {
                    int[] casing = getHatchBaseTextures(mapDataCtx);
                    if (casing != null) {
                        baseFaces = casing;
                        baseTex = casing[0];
                        baseSet = null;
                    } else if (mapDataCtx.getBlockTileEntityField("textureIndex") == null) {
                        // Current hatches explicitly persist their casing choice (0/0
                        // means no multi-block casing). Only old NBT needs inference.
                        tryStealBaseTexFromNeighbor(mapDataCtx, outputFacing);
                    }
                }

                if(entry.paintable && mColor > 0 && mColor <= 16){
                    colorMul = dyeColors[mColor -1];
                }

                for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                    ArrayList<Integer> layers = new ArrayList<>();
                    if (baseFaces != null) {
                        if (baseFaces[dir.ordinal()] >= 0) layers.add(baseFaces[dir.ordinal()]);
                    } else if (baseSet != null) {
                        if (dir == ForgeDirection.DOWN) addLayers(layers, baseSet.bottomLayers, baseSet.bottom);
                        else if (dir == ForgeDirection.UP) addLayers(layers, baseSet.topLayers, baseSet.top);
                        else addLayers(layers, baseSet.sideLayers, baseSet.side);
                    } else {
                        if (baseTex >= 0) layers.add(baseTex);
                        if (baseTex2 >= 0) layers.add(baseTex2);
                    }
                    if (iconSet != null) {
                        int paintColor = entry.paintable ? mColor : 0;
                        int paint = paintColor >= 0 && paintColor <= 16 ? iconSet.outputPaint[paintColor] : -1;
                        if ((outputFacing == dir.ordinal() || (outputFacing == ForgeDirection.DOWN.ordinal() && iconSet.downFacingExtra == dir.ordinal())) && (iconSet.output >= 0 || iconSet.outputLayers.length > 0 || paint >= 0)) {
                            addLayers(layers, iconSet.outputLayers, iconSet.output);
                            if (paint >= 0) layers.add(paint);
                        } else if (dir.ordinal() == (outputFacing ^ 1) && (iconSet.back >= 0 || iconSet.backLayers.length > 0)) addLayers(layers, iconSet.backLayers, iconSet.back);
                        else if (dir == ForgeDirection.DOWN) addLayers(layers, iconSet.bottomLayers, iconSet.bottom);
                        else if (dir == ForgeDirection.UP) addLayers(layers, iconSet.topLayers, iconSet.top);
                        else if (front == dir.ordinal() && (iconSet.front >= 0 || iconSet.frontLayers.length > 0)) addLayers(layers, iconSet.frontLayers, iconSet.front);
                        else addLayers(layers, iconSet.sideLayers, iconSet.side);
                    }
                    texturesToUse[dir.ordinal()] = layers.isEmpty() ? new int[] { -1 } : toArray(layers);
                }
            } else {
                for (int i = 0; i < 6; i++)
                    texturesToUse[i] = new int[]{ -1 };
            }
        }

        private int[] getHatchBaseTextures(MapDataContext context) {
            Object indexValue = context.getBlockTileEntityField("textureIndex");
            boolean legacy = indexValue == null;
            if (legacy) indexValue = context.getBlockTileEntityField("mMachineBlock");
            if (indexValue == null) return null;
            Object pageValue = context.getBlockTileEntityField(legacy ? "mTexturePage" : "texturePage");
            int page = number(pageValue, 0);
            int index = number(indexValue, -1);
            if (legacy && index < 0) index += 128;
            if (!legacy && pageValue == null && index >= 128) {
                page = index >>> 7;
                index &= 127;
            }
            if (!legacy && page == 0 && index == 0) return null;
            int[] faces = new int[6];
            boolean any = false;
            for (int face = 0; face < 6; face++) {
                faces[face] = GregTechSupport.INSTANCE.getHatchBaseTexture(page, index, face);
                any |= faces[face] >= 0;
            }
            return any ? faces : null;
        }

        private void tryStealBaseTexFromNeighbor(MapDataContext mapDataCtx, int outputFacing) {
            ForgeDirection output = ForgeDirection.getOrientation(outputFacing);
            for (ForgeDirection searchDir : ForgeDirection.VALID_DIRECTIONS){
                if(searchDir == output)
                    continue;

                int blockAt = mapDataCtx.getBlockTypeIDAt(searchDir.offsetX, searchDir.offsetY, searchDir.offsetZ);
                if(blockAt <= 1 || blockAt >= 65536)
                    continue;

                if(GregTechSupport.INSTANCE.validHatchBaseBlocks[blockAt] != null){
                    int blockDataAt = mapDataCtx.getBlockDataAt(searchDir.offsetX, searchDir.offsetY, searchDir.offsetZ);
                    int[] baseTextures = GregTechSupport.INSTANCE.validHatchBaseBlocks[blockAt];
                    if (blockDataAt < 0 || blockDataAt >= baseTextures.length) continue;
                    int tex = baseTextures[blockDataAt];
                    if(tex > 64) {
                        baseSet = null;
                        baseFaces = null;
                        baseTex = tex;
                        break;
                    }
                }
                else if(blockAt == mapDataCtx.getBlockTypeID()){ // Another hatch or GT block
                    Object objId = mapDataCtx.getBlockTileEntityFieldAt("mID",searchDir.offsetX, searchDir.offsetY, searchDir.offsetZ);
                    if(objId instanceof Number){
                        int otherId = ((Number)objId).intValue();
                        if(otherId >= 0 && otherId < 65536){
                            GregTechSupport.MetaTileEntityEntry otherMtee = GregTechSupport.INSTANCE.getMTEEntre(otherId);
                            if(otherMtee !=null && otherMtee.baseTexture != -1 && otherMtee.type == GregTechSupport.MteType.Controller){
                                baseSet = null;
                                baseFaces = null;
                                baseTex = otherMtee.baseTexture;
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public int getColorMultiplier(MapDataContext mapDataCtx) {
            return colorMul;
        }

        @Override
        public int[] getTextureLayersForPatchId(int patchId) {
            return texturesToUse[patchId];
        }
    }


    private static int number(Object value, int fallback) {
        return value instanceof Boolean ? ((Boolean) value ? 1 : 0) : value instanceof Number ? ((Number) value).intValue() : GWM_Util.objectToInt(value, fallback);
    }

    private static int validFacing(int direction, int fallback) {
        return direction >= 0 && direction < 6 ? direction : fallback;
    }

    private static void addLayers(ArrayList<Integer> target, int[] layers, int primary) {
        if (layers.length == 0) {
            if (primary >= 0) target.add(primary);
        } else {
            for (int texture : layers) if (texture >= 0) target.add(texture);
        }
    }

    private static int[] toArray(ArrayList<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }

    static String[] nbtFieldsNeeded = {"id", "mID", "mColor", "mConnections", "mFacing", "mActive", "mMainFacing", "mMachineBlock", "mTexturePage", "texturePage", "textureIndex"};

    private String[] lastStateFields;
    private String[] allFields = nbtFieldsNeeded;

    @Override
    public String[] getTileEntityFieldsNeeded() {
        String[] stateFields = GregTechSupport.INSTANCE.getStateFieldsNeeded();
        if (stateFields != lastStateFields) {
            String[] fields = Arrays.copyOf(nbtFieldsNeeded, nbtFieldsNeeded.length + stateFields.length);
            System.arraycopy(stateFields, 0, fields, nbtFieldsNeeded.length, stateFields.length);
            allFields = fields;
            lastStateFields = stateFields;
        }
        return allFields;
    }
}
