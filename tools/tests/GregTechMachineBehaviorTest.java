package org.dynmap.hdmap.renderer;

import java.util.*;
import org.dynmap.modsupport.gregtech.GregTechSupport;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.renderer.*;

/** Exercises production mapping parsing and saved-state texture selection without a world. */
public class GregTechMachineBehaviorTest {
    private static final GregTechSupport SUPPORT = GregTechSupport.INSTANCE;
    private static final HashMap<String, Integer> FILES = new HashMap<String, Integer>();
    private static int assertions;

    private static HashMap<String, String> data(String... entries) {
        HashMap<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < entries.length; i += 2) result.put(entries[i], entries[i + 1]);
        return result;
    }

    private static void icon(String name, String... entries) {
        HashMap<String, String> record = data(entries);
        record.put("name", name);
        SUPPORT.processIconSet(record, FILES);
    }

    private static void mte(int id, String... entries) {
        HashMap<String, String> record = data(entries);
        record.put("id", String.valueOf(id));
        SUPPORT.processMte(record, FILES);
    }

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static void face(GregTechMachineRenderer renderer, Context context, int face, int... expected) {
        int[] actual = renderer.getRenderData(context).getCustomTextureMapper().getTextureLayersForPatchId(face);
        check(Arrays.equals(expected, actual), "face " + face + ": expected " + Arrays.toString(expected) + ", got " + Arrays.toString(actual));
    }

    public static void main(String[] args) throws Exception {
        GregTechMachineRenderer renderer = new GregTechMachineRenderer();
        icon("fixture-old", "bottom", "10", "top", "11", "side", "12", "front", "13", "output", "14");
        mte(64000, "basetex", "100", "iconset", "fixture-old");
        Context context = new Context(64000);
        context.fields.put("mFacing", (short) 2);
        context.fields.put("mMainFacing", 5);
        face(renderer, context, 0, 100, 10);
        face(renderer, context, 1, 100, 11);
        face(renderer, context, 2, 100, 14);
        face(renderer, context, 3, 100, 12);
        face(renderer, context, 4, 100, 12);
        face(renderer, context, 5, 100, 13);
        icon("fixture-layers", "front", "20", "front2", "23021", "front3", "22", "front4", "23", "output", "24", "output2", "25", "outputpaint0", "26", "outputpaint1", "27", "outputpaint16", "28");
        mte(64001, "basetex", "101", "iconset", "fixture-layers");
        context.fields.put("mID", 64001);
        face(renderer, context, 5, 101, 20, 23000021, 22, 23);
        face(renderer, context, 2, 101, 24, 25, 26);
        context.fields.put("mColor", (byte) 1);
        face(renderer, context, 2, 101, 24, 25, 27);
        context.fields.put("mColor", (byte) 16);
        face(renderer, context, 2, 101, 24, 25, 28);
        context.fields.put("mColor", 200);
        face(renderer, context, 2, 101, 24, 25);
        mte(64002, "basetex", "102", "iconset", "fixture-late", "activeset", "fixture-active");
        icon("fixture-late", "output", "30");
        icon("fixture-active", "output", "31", "output2", "23032");
        context.fields.put("mID", 64002);
        face(renderer, context, 2, 102, 30);
        context.fields.put("mActive", 1);
        face(renderer, context, 2, 102, 31, 23000032);
        context.fields.put("mActive", Boolean.FALSE);
        face(renderer, context, 2, 102, 30);
        mte(64003, "basetex", "103", "iconset", "fixture-old", "statefield", "fixtureMode", "state2basetex", "104", "state3basetex", "105", "state2iconset", "fixture-state", "casingstatefield", "fixtureCasing", "casingstate2basetex", "106");
        icon("fixture-state", "output", "33");
        context.fields.put("mID", 64003);
        face(renderer, context, 2, 103, 14);
        context.fields.put("fixtureMode", (byte) 2);
        face(renderer, context, 2, 104, 33);
        context.fields.put("fixtureMode", 3);
        face(renderer, context, 2, 105, 14);
        context.fields.put("fixtureCasing", 2);
        face(renderer, context, 2, 106, 14);
        check(Arrays.asList(renderer.getTileEntityFieldsNeeded()).containsAll(Arrays.asList("fixtureMode", "fixtureCasing")), "declared state NBT fields requested");
        mte(64004, "basetex", "107", "iconset", "fixture-old", "ishatch", "true");
        context.fields.put("mID", 64004);
        SUPPORT.hatchBaseTextures2[1] = new int[128];
        SUPPORT.hatchBaseTextures2[1][4] = 210;
        context.fields.put("texturePage", 1);
        context.fields.put("textureIndex", 4);
        face(renderer, context, 2, 210, 14);
        context.fields.put("texturePage", 200);
        face(renderer, context, 2, 107, 14);
        context.fields.put("texturePage", -1);
        face(renderer, context, 2, 107, 14);
        context.fields.remove("texturePage");
        context.fields.put("textureIndex", 132);
        face(renderer, context, 2, 210, 14);
        SUPPORT.hatchBaseTextures2[0] = new int[128];
        SUPPORT.hatchBaseTextures2[0][127] = 211;
        context.fields.remove("textureIndex");
        context.fields.put("mMachineBlock", (byte) -1);
        face(renderer, context, 2, 211, 14);
        mte(64005, "basetex", "108", "iconset", "fixture-old", "ishatch", "true", "inheritcasing", "false");
        context.fields.put("mID", 64005);
        face(renderer, context, 2, 108, 14);
        for (int facing = 0; facing < 6; facing++) {
            context.fields.put("mFacing", facing);
            face(renderer, context, facing, 108, 14);
        }
        context.fields.put("mFacing", 99);
        face(renderer, context, 2, 108, 14);
        context.fields.remove("mFacing");
        context.fields.remove("mMainFacing");
        face(renderer, context, 2, 108, 14);
        icon("fixture-down-extra", "output", "35", "downfacingextra", "4");
        mte(64006, "basetex", "109", "iconset", "fixture-down-extra");
        context.fields.put("mID", 64006);
        context.fields.put("mFacing", 0);
        face(renderer, context, 0, 109, 35);
        face(renderer, context, 4, 109, 35);
        face(renderer, context, 5, 109);
        context.fields.put("mFacing", 1);
        face(renderer, context, 4, 109);
        mte(64007, "basetex", "110", "iconset", "fixture-layers", "paint", "false");
        context.fields.put("mID", 64007);
        context.fields.put("mColor", 16);
        face(renderer, context, 1, 110, 24, 25, 26);
        java.lang.reflect.Method initialize = TexturePack.HDTextureMap.class.getDeclaredMethod("initializeTable");
        initialize.setAccessible(true);
        initialize.invoke(null);
        java.lang.reflect.Field registeredField = GregTechSupport.class.getDeclaredField("registeredHatchBaseBlocks");
        registeredField.setAccessible(true);
        int[][][] registered = (int[][][]) registeredField.get(SUPPORT);
        registered[2][7] = new int[] { 200, 3 };
        SUPPORT.hatchBaseTextures2[2] = new int[128];
        SUPPORT.hatchBaseTextures2[2][7] = 300;
        check(SUPPORT.getHatchBaseTexture(2, 7) == -1, "unmapped authoritative casing does not select obsolete explicit texture");
        new TexturePack.HDTextureMap(Collections.singletonList(200), 1 << 3, new int[] { 400 }, null,
                TexturePack.BlockTransparency.OPAQUE, false, 0xFFFFFF, null, null, true).addToTable();
        check(SUPPORT.getHatchBaseTexture(2, 7) == 400, "authoritative casing resolved after mapping loads");
        new TexturePack.HDTextureMap(Collections.singletonList(200), 1 << 3, new int[] { 401 }, null,
                TexturePack.BlockTransparency.OPAQUE, false, 0xFFFFFF, null, null, true).addToTable();
        check(SUPPORT.getHatchBaseTexture(2, 7) == 401, "authoritative casing follows effective mapping override");
        check(SUPPORT.getHatchBaseTexture(-1, 7) == -1 && SUPPORT.getHatchBaseTexture(128, 7) == -1, "authoritative casing bounds safe");
        System.out.println("GregTech saved-state rendering: " + assertions + " assertions passed");
    }

    private static class Context implements MapDataContext {
        final Map<String, Object> fields = new HashMap<String, Object>();
        Context(int id) { fields.put("mID", id); }
        public RenderPatchFactory getPatchFactory() { return null; }
        public int getBlockTypeID() { return 1000; }
        public int getBlockData() { return 0; }
        public Object getBlockTileEntityField(String name) { return fields.get(name); }
        public int getBlockTypeIDAt(int x, int y, int z) { return 0; }
        public int getBlockDataAt(int x, int y, int z) { return 0; }
        public Object getBlockTileEntityFieldAt(String name, int x, int y, int z) { return null; }
        public int getX() { return 0; }
        public int getY() { return 64; }
        public int getZ() { return 0; }
        public int getBlockDataFull() { return 0; }
    }
}
