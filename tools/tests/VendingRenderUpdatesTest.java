package org.dynmap.forge;
import java.util.*;

public class VendingRenderUpdatesTest {
    private static int assertions;
    static void check(boolean value, String text) { assertions++; if (!value) throw new AssertionError(text); }
    static String key(int x, int y, int z) { return x + ":" + y + ":" + z; }
    public static void main(String[] args) {
        final Set<String> casings = new HashSet<String>();
        final int[] reads = { 0 };
        VendingRenderUpdates.Blocks blocks = new VendingRenderUpdates.Blocks() {
            public int get(int x, int y, int z) { reads[0]++; return casings.contains(key(x, y, z)) ? 1001 : 0; }
        };
        check(!VendingRenderUpdates.needsVolume(0, 0, 0, 64, 0, blocks) && reads[0] == 0, "disabled mod causes no neighbor reads");
        check(VendingRenderUpdates.needsVolume(1001, 1001, 0, 64, 0, blocks) && reads[0] == 0, "changed casing needs no neighbor scan");
        check(!VendingRenderUpdates.needsVolume(1001, 0, 0, 64, 0, blocks) && reads[0] == 6, "unrelated update scans only six cardinal neighbors");
        for (int[] offset : new int[][] { {-1,0,0}, {1,0,0}, {0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1} }) {
            casings.clear(); casings.add(key(offset[0], 64 + offset[1], offset[2]));
            check(VendingRenderUpdates.needsVolume(1001, 0, 0, 64, 0, blocks), "removed/replaced controller detected beside surviving casing");
        }
        final List<String> changes = new ArrayList<String>();
        VendingRenderUpdates.Listener listener = new VendingRenderUpdates.Listener() {
            public void changed(int x, int y, int z) { changes.add(key(x, y, z)); }
        };
        VendingRenderUpdates.States states = new VendingRenderUpdates.States();
        Map<VendingRenderUpdates.Position, Integer> current = new HashMap<VendingRenderUpdates.Position, Integer>();
        VendingRenderUpdates.Position position = new VendingRenderUpdates.Position(15, 64, -16);
        current.put(position, (2741 << 4) | 2);
        states.update(current, listener);
        check(changes.equals(Collections.singletonList("15:64:-16")), "first sighting invalidates restored map data");
        changes.clear(); states.update(current, listener); check(changes.isEmpty(), "unchanged state produces no work");
        current.put(position, (2741 << 4) | 2 | 8);
        states.update(current, listener); check(changes.size() == 1, "activation invalidates display");
        changes.clear(); current.put(position, (2741 << 4) | 3 | 8);
        states.update(current, listener); check(changes.size() == 1, "facing change invalidates old and new orientation area");
        changes.clear(); current.put(position, (2742 << 4) | 3 | 8);
        states.update(current, listener); check(changes.size() == 1, "controller replaced by uplink invalidates");
        changes.clear(); current.clear(); states.update(current, listener);
        check(changes.size() == 1, "removal or unload clears stale display");
        changes.clear(); states.update(current, listener); check(changes.isEmpty(), "removed location does not stay retained");
        current.put(position, (2741 << 4) | 2); states.update(current, listener); changes.clear();
        current.clear(); check(changes.isEmpty(), "caller mutations cannot mutate cached snapshot");
        states.update(current, listener); check(changes.size() == 1, "detached snapshot still detects caller removal");
        // Any display cell is at most two blocks from any other cell of its 2x3 front.
        for (int ax = -1; ax <= 0; ax++) for (int ay = -1; ay <= 1; ay++) {
            for (int bx = -1; bx <= 0; bx++) for (int by = -1; by <= 1; by++) {
                check(Math.abs(ax - bx) <= VendingRenderUpdates.RADIUS && Math.abs(ay - by) <= VendingRenderUpdates.RADIUS, "invalidation volume covers all display dependents");
            }
        }
        System.out.println("Vending update dependencies: " + assertions + " assertions passed");
    }
}
