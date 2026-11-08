package org.dynmap.hdmap.renderer;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.minecraftforge.common.util.ForgeDirection;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import org.dynmap.hdmap.TexturePack;
import org.dynmap.modsupport.GWM_Util;
import org.dynmap.modsupport.gregtech.GregTechSupport;
import org.dynmap.renderer.*;
import org.dynmap.utils.*;

/** Actual mappings and saved neighbor data, compared to the client's compact CTM expansion. */
public class VendingMachineBehaviorTest {
    private static final int CASING = 1001, MACHINES = 1000;
    private static final String MAP = "vendingmachine-casing";
    private static final PatchDefinitionFactory FACTORY = new PatchDefinitionFactory();
    private static final GregTechSupport SUPPORT = GregTechSupport.INSTANCE;
    private static int assertions;

    private static void check(boolean value, String text) {
        assertions++;
        if (!value) throw new AssertionError(text);
    }

    private static HashMap<String, String> record(String line) {
        HashMap<String, String> result = new HashMap<String, String>();
        for (String token : line.substring(line.indexOf(':') + 1).split(",")) {
            String[] pair = token.split("=", 2);
            if (pair.length == 2) result.put(pair[0], pair[1]);
        }
        return result;
    }

    private static int texture(int key) {
        return TexturePack.getTextureIdFromTextureMap(MAP, key);
    }

    private static int[] layers(CustomRendererData data, int face, int quadrant) {
        return data.getCustomTextureMapper().getTextureLayersForPatchId(face * 4 + quadrant);
    }

    private static void loadMappings(Path root) throws Exception {
        Method initialize = TexturePack.HDTextureMap.class.getDeclaredMethod("initializeTable");
        initialize.setAccessible(true);
        initialize.invoke(null);
        Field ids = GWM_Util.class.getDeclaredField("blockIdMap");
        ids.setAccessible(true);
        Map<String, Integer> names = new HashMap<String, Integer>();
        names.put("vendingmachine:casing", CASING);
        names.put("gregtech:gt.blockmachines", MACHINES);
        ids.set(null, names);
        HashMap<String, Integer> files = new HashMap<String, Integer>();
        Method add = TexturePack.class.getDeclaredMethod("addTextureByKey", String.class, int.class, int.class);
        add.setAccessible(true);
        for (String line : Files.readAllLines(root.resolve("src/main/resources/renderdata/vendingmachine-texture.txt"))) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            HashMap<String, String> data = record(line);
            if (line.startsWith("texture:")) {
                int x = Integer.parseInt(data.containsKey("xcount") ? data.get("xcount") : "1");
                int y = Integer.parseInt(data.containsKey("ycount") ? data.get("ycount") : "1");
                files.put(data.get("id"), TexturePack.findOrAddDynamicTileFile(data.get("filename"), "vendingmachine", x, y,
                        TexturePack.TileFileFormat.GRID, new String[0], null));
            } else if (line.startsWith("moddata:") && "iconset".equals(data.get("type"))) SUPPORT.processIconSet(data, files);
            else if (line.startsWith("moddata:") && "mte".equals(data.get("type"))) SUPPORT.processMte(data, files);
            else if (line.startsWith("addtotexturemap:")) {
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    if (entry.getKey().startsWith("key:")) add.invoke(null, data.get("mapid"), Integer.parseInt(entry.getKey().substring(4)), TexturePack.parseTextureIndex(files, entry.getValue()));
                }
            }
        }
        int base = TexturePack.parseTextureIndex(files, "0:vendingmachine/casing");
        new TexturePack.HDTextureMap(Collections.singletonList(CASING), 1, new int[] { base, base, base, base, base, base }, null,
                TexturePack.BlockTransparency.OPAQUE, false, 0xFFFFFF, null, null, true).addToTable();
    }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]);
        loadMappings(root);
        VendingMachineRenderer casing = new VendingMachineRenderer();
        check(casing.initializeRenderer(FACTORY, CASING, 1, Collections.singletonMap("machineblock", String.valueOf(MACHINES))), "casing initializes");
        Scene empty = new Scene(CASING);
        RenderPatch[] mesh = casing.getRenderPatchList(empty);
        check(mesh.length == 24, "cube split into 24 immutable quarter faces");
        for (int patch = 0; patch < mesh.length; patch++) {
            check(mesh[patch] != null && mesh[patch].getTextureIndex() == patch, "quarter patch index");
            PatchDefinition p = (PatchDefinition) mesh[patch];
            check(p.umax - p.umin == .5 && p.vmax - p.vmin == .5, "quarter retains full-face UV scale");
        }
        List<String> reference = Files.readAllLines(root.resolve("tools/tests/vending-compact-reference.csv"));
        int[] h = { -1, -1, 0, 1, 1, 1, 0, -1 };
        int[] v = { 0, -1, -1, -1, 0, 1, 1, 1 };
        for (int line = 1; line < reference.size(); line++) {
            String[] fields = reference.get(line).split(",");
            int mask = Integer.parseInt(fields[0]);
            for (int face = 0; face < 6; face++) {
                Scene scene = new Scene(CASING);
                PatchDefinition p = (PatchDefinition) mesh[face * 4];
                for (int neighbor = 0; neighbor < 8; neighbor++) if ((mask & (1 << neighbor)) != 0) {
                    int x = (int) Math.round(h[neighbor] * p.u.x + v[neighbor] * p.v.x);
                    int y = (int) Math.round(h[neighbor] * p.u.y + v[neighbor] * p.v.y);
                    int z = (int) Math.round(h[neighbor] * p.u.z + v[neighbor] * p.v.z);
                    scene.put(x, y, z, CASING, null);
                }
                CustomRendererData data = casing.getRenderData(scene);
                for (int quarter = 0; quarter < 4; quarter++) {
                    int expected = texture(1 + Integer.parseInt(fields[quarter + 2]));
                    int[] actual = layers(data, face, quarter);
                    check(Arrays.equals(actual, new int[] { expected }), "client CTM reference mask=" + mask + " face=" + face + " quarter=" + quarter);
                }
            }
        }
        GregTechMachineRenderer machine = new GregTechMachineRenderer();
        int[] localA = { -1, 0, -1, -1, 0 }, localB = { -1, -1, 0, 1, 1 };
        for (int facing = 2; facing < 6; facing++) {
            for (int part = 0; part < 5; part++) {
                int[] world = new int[3];
                ExtendedFacing.of(ForgeDirection.getOrientation(facing)).getWorldOffset(new int[] { localA[part], localB[part], 0 }, world);
                for (boolean uplink : new boolean[] { false, true }) {
                    Scene scene = new Scene(uplink ? MACHINES : CASING);
                    if (uplink) {
                        scene.fields.put("mID", VendingMachineRenderer.UPLINK);
                        scene.fields.put("mFacing", (short) facing);
                    }
                    Map<String, Object> controller = new HashMap<String, Object>();
                    controller.put("mID", VendingMachineRenderer.CONTROLLER);
                    controller.put("mFacing", (short) facing);
                    controller.put("mActive", (byte) 1);
                    scene.put(-world[0], -world[1], -world[2], MACHINES, controller);
                    CustomRendererData data = uplink ? machine.getRenderData(scene) : casing.getRenderData(scene);
                    for (int quarter = 0; quarter < 4; quarter++) {
                        int[] actual = layers(data, facing, quarter);
                        check(actual[actual.length - 1] == texture(20 + part), "StructureLib display position facing=" + facing + " part=" + part + " uplink=" + uplink);
                        check((texture(20 + part) >= 23000000) == (part < 4), "four emissive parts and one ordinary part");
                    }
                    controller.put("mActive", (byte) 0);
                    data = uplink ? machine.getRenderData(scene) : casing.getRenderData(scene);
                    for (int quarter = 0; quarter < 4; quarter++) {
                        int[] actual = layers(data, facing, quarter);
                        check(actual[actual.length - 1] != texture(20 + part), "inactive controller clears neighboring display");
                    }
                    controller.put("mActive", true);
                    controller.put("mFacing", (short) 0);
                    data = uplink ? machine.getRenderData(scene) : casing.getRenderData(scene);
                    check(layers(data, facing, 0)[layers(data, facing, 0).length - 1] != texture(20 + part), "invalid vertical facing cannot draw display");
                    scene.blocks.clear();
                    data = uplink ? machine.getRenderData(scene) : casing.getRenderData(scene);
                    check(layers(data, facing, 0)[layers(data, facing, 0).length - 1] != texture(20 + part), "removed controller clears display");
                }
            }
        }
        // connect=tile can join casing sides on the controller and on the optional uplink.
        for (int id : new int[] { VendingMachineRenderer.CONTROLLER, VendingMachineRenderer.UPLINK }) {
            Scene scene = new Scene(CASING);
            Map<String, Object> other = new HashMap<String, Object>();
            other.put("mID", id);
            other.put("mFacing", (short) 3);
            scene.put(1, 0, 0, MACHINES, other); // left of the NORTH face
            check(layers(casing.getRenderData(scene), 2, 0)[0] == texture(4), "neighboring machine casing side joins CTM");
            if (id == VendingMachineRenderer.CONTROLLER) {
                other.put("mFacing", (short) 2);
                check(layers(casing.getRenderData(scene), 2, 0)[0] == texture(4), "BlockMachines connection icon remains casing even on controller front");
            }
        }
        check(Arrays.asList(casing.getTileEntityFieldsNeeded()).containsAll(Arrays.asList("mID", "mFacing", "mActive")), "neighbor state fields requested");
        System.out.println("Vending saved-state rendering: " + assertions + " assertions passed");
    }

    private static final class Cell {
        int block;
        Map<String, Object> fields;
        Cell(int block, Map<String, Object> fields) { this.block = block; this.fields = fields; }
    }

    private static final class Scene implements MapDataContext {
        int ownBlock;
        final Map<String, Object> fields = new HashMap<String, Object>();
        final Map<String, Cell> blocks = new HashMap<String, Cell>();
        Scene(int ownBlock) { this.ownBlock = ownBlock; }
        void put(int x, int y, int z, int block, Map<String, Object> data) { blocks.put(x + ":" + y + ":" + z, new Cell(block, data)); }
        Cell cell(int x, int y, int z) { return blocks.get(x + ":" + y + ":" + z); }
        public RenderPatchFactory getPatchFactory() { return FACTORY; }
        public int getBlockTypeID() { return ownBlock; }
        public int getBlockData() { return 0; }
        public Object getBlockTileEntityField(String name) { return fields.get(name); }
        public int getBlockTypeIDAt(int x, int y, int z) { if (x == 0 && y == 0 && z == 0) return ownBlock; Cell c = cell(x, y, z); return c == null ? 0 : c.block; }
        public int getBlockDataAt(int x, int y, int z) { return 0; }
        public Object getBlockTileEntityFieldAt(String name, int x, int y, int z) { Cell c = cell(x, y, z); return c == null || c.fields == null ? null : c.fields.get(name); }
        public int getX() { return 0; }
        public int getY() { return 64; }
        public int getZ() { return 0; }
        public int getBlockDataFull() { return 0; }
    }
}
