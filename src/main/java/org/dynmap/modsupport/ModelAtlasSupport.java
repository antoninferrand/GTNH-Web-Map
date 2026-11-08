package org.dynmap.modsupport;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.dynmap.renderer.CustomTextureMapper;

/** File-local GRID indices become immutable global model atlas mappers at load time. */
public final class ModelAtlasSupport {
    private static final Map<String, CustomTextureMapper> ATLASES = new ConcurrentHashMap<>();

    private ModelAtlasSupport() {}

    public static void register(String name, int[] cells) {
        final int[][] layers = new int[cells.length][];
        for (int i = 0; i < cells.length; i++) layers[i] = new int[] { cells[i] };
        ATLASES.put(name, new CustomTextureMapper() {
            @Override
            public int[] getTextureLayersForPatchId(int patchId) {
                return patchId >= 0 && patchId < layers.length ? layers[patchId] : null;
            }
        });
    }

    public static CustomTextureMapper getMapper(String name) {
        return ATLASES.get(name);
    }
}
