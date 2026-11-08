package org.dynmap.forge;

import java.util.HashMap;
import java.util.Map;

/** Small, world-independent dependency rules for the two-by-three vending display. */
public final class VendingRenderUpdates {
    public static final int RADIUS = 2;
    private static final int[][] ADJACENT = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 } };

    private VendingRenderUpdates() {}

    public interface Blocks {
        /** Return air for unavailable coordinates; implementations must not load chunks. */
        int get(int x, int y, int z);
    }

    public interface Listener {
        void changed(int x, int y, int z);
    }

    public static boolean needsVolume(int casing, int changedBlock, int x, int y, int z, Blocks blocks) {
        if (casing <= 0) return false;
        if (changedBlock == casing) return true;
        for (int[] offset : ADJACENT) {
            if (blocks.get(x + offset[0], y + offset[1], z + offset[2]) == casing) return true;
        }
        return false;
    }

    public static final class Position {
        public final int x, y, z;
        public Position(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Position)) return false;
            Position p = (Position) other;
            return x == p.x && y == p.y && z == p.z;
        }
        @Override public int hashCode() { return (31 * x + y) * 31 + z; }
    }

    /** Per-world, detached values only; no tile entities, worlds or chunks are retained. */
    public static final class States {
        private Map<Position, Integer> previous = new HashMap<>();

        public void update(Map<Position, Integer> current, Listener listener) {
            for (Map.Entry<Position, Integer> entry : current.entrySet()) {
                if (!entry.getValue().equals(previous.get(entry.getKey()))) notify(listener, entry.getKey());
            }
            for (Position old : previous.keySet()) if (!current.containsKey(old)) notify(listener, old);
            previous = new HashMap<>(current);
        }

        private static void notify(Listener listener, Position p) { listener.changed(p.x, p.y, p.z); }
    }
}
